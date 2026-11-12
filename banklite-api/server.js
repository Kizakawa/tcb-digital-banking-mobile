"use strict";

// =============================================================
// server.js — TCB Study Mobile Banking Backend (Security-Hardened)
// Node.js / Express / SQLite (Modular Architecture)
// =============================================================

const express = require("express");
const cors = require("cors");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const crypto = require("crypto");

const config = require("./src/config");
const { dbRun, dbGet, dbAll, runTransaction, audit, seedDemoData, closeDb } = require("./src/db");
const {
  securityHeadersMiddleware,
  checkRateLimit,
  generateRef,
  generateUniqueAccountNumber,
  getIP,
  issueOTP,
  verifyOTPWithAttemptGuard,
  authenticateToken,
  getAuthenticatedUserFromRequest,
  requireActiveAccount,
  requireAdmin,
} = require("./src/security");

const app = express();
app.disable("x-powered-by");

if (config.TRUST_PROXY) {
  app.set("trust proxy", 1);
}

app.use(securityHeadersMiddleware);
app.use(cors());
app.use(express.json({ limit: "64kb" }));

app.use((err, _req, res, next) => {
  if (err instanceof SyntaxError && err.status === 400 && "body" in err) {
    return res.status(400).json({ status: "error", message: "Invalid JSON body" });
  }
  next(err);
});

setTimeout(seedDemoData, 100);

// ================= FORMATTERS =================

function maskSensitive(val) {
  const s = String(val || "").trim();
  if (s.length <= 4) return "****";
  return "*".repeat(s.length - 4) + s.slice(-4);
}

function formatUser(u) {
  return {
    id: u.id,
    fullName: u.full_name,
    phone: u.phone,
    email: u.email || "",
    balance: u.balance,
    username: u.username || "",
    accountNumber: u.accountNumber || "",
    accountType: u.accountType || "",
    accountFee: u.accountFee || 0,
    cccdNumber: maskSensitive(u.cccdNumber),
    job: u.job || "",
    status: u.status || "ACTIVE",
    ekycStatus: u.ekycStatus || "PENDING",
    createdAt: u.created_at,
  };
}

function formatTx(t) {
  return {
    id: t.id,
    referenceCode: t.reference_code || "",
    type: t.type,
    amount: t.amount,
    fee: t.fee || 0,
    status: t.status,
    content: t.content || "",
    counterpartyName: t.counterparty_name || "",
    counterpartyAccount: t.counterparty_account || "",
    balanceAfter: t.balance_after,
    createdAt: t.created_at,
  };
}

// ================= ROOT =================

app.get("/", (_req, res) =>
  res.json({ status: "success", message: "BankLite API (Hardened) is running" })
);

// =============================================================
// UC_001 — ĐĂNG KÝ MỞ TÀI KHOẢN & OTP & EKYC
// =============================================================

app.get("/api/auth/check-phone/:phone", async (req, res) => {
  try {
    const phone = String(req.params.phone || "").trim();
    if (!phone) {
      return res.status(400).json({ status: "error", exists: false, message: "Phone is required" });
    }
    if (!checkRateLimit(`check_phone:${getIP(req)}`, 30, 60 * 1000)) {
      return res.status(429).json({ status: "error", exists: false, message: "Quá nhiều yêu cầu kiểm tra" });
    }
    const user = await dbGet("SELECT id FROM users WHERE phone = ?", [phone]);
    return res.json({
      status: "success",
      exists: !!user,
      message: user ? "Số điện thoại đã được đăng ký" : "Số điện thoại có thể sử dụng",
    });
  } catch (err) {
    console.error("check-phone:", err.message);
    return res.status(500).json({ status: "error", exists: false, message: "Cannot check phone" });
  }
});

app.get("/api/account/check-number/:accountNumber", async (req, res) => {
  try {
    const acc = String(req.params.accountNumber || "").trim();
    if (!acc) {
      return res.status(400).json({ status: "error", exists: false, message: "Account number is required" });
    }
    if (!checkRateLimit(`check_acc:${getIP(req)}`, 40, 60 * 1000)) {
      return res.status(429).json({ status: "error", exists: false, message: "Quá nhiều yêu cầu kiểm tra" });
    }
    const user = await dbGet("SELECT id FROM users WHERE accountNumber = ?", [acc]);
    return res.json({
      status: "success",
      exists: !!user,
      message: user ? "Số tài khoản đã tồn tại" : "Số tài khoản có thể sử dụng",
    });
  } catch (err) {
    console.error("check-number:", err.message);
    return res.status(500).json({ status: "error", exists: false, message: "Cannot check account number" });
  }
});

// POST /api/otp/send
app.post("/api/otp/send", async (req, res) => {
  try {
    const phone = String(req.body.phone || "").trim();
    const purpose = String(req.body.purpose || "").trim().toUpperCase();
    const referenceCode = String(req.body.referenceCode || "").trim();

    if (!phone || !purpose) {
      return res.status(400).json({ status: "error", message: "phone và purpose là bắt buộc" });
    }

    if (!/^[0-9]{9,11}$/.test(phone)) {
      return res.status(400).json({ status: "error", message: "Số điện thoại không hợp lệ" });
    }

    const validPurposes = ["REGISTER", "RESET_PASSWORD", "TRANSFER", "BILL_PAYMENT"];
    if (!validPurposes.includes(purpose)) {
      return res.status(400).json({ status: "error", message: "purpose không hợp lệ" });
    }

    if (purpose === "TRANSFER" || purpose === "BILL_PAYMENT") {
      let authedUser;
      try {
        authedUser = await getAuthenticatedUserFromRequest(req);
      } catch (authErr) {
        return res.status(authErr.statusCode || 401).json({
          status: "error",
          message: authErr.message,
        });
      }

      if (authedUser.phone !== phone) {
        return res.status(403).json({
          status: "error",
          message: "Không thể gửi OTP giao dịch cho số điện thoại của người khác",
        });
      }

      if (purpose === "TRANSFER" && referenceCode) {
        const transferReq = await dbGet(
          `SELECT id FROM transfer_requests
           WHERE reference_code = ? AND sender_id = ? AND status = 'PENDING'
             AND julianday(expires_at) > julianday('now')`,
          [referenceCode, authedUser.id]
        );
        if (!transferReq) {
          return res.status(400).json({
            status: "error",
            message: "Yêu cầu chuyển tiền không tồn tại, không thuộc về bạn, đã hết hạn hoặc không còn PENDING",
          });
        }
      }

      if (purpose === "BILL_PAYMENT" && referenceCode) {
        const billId = parseInt(referenceCode, 10);
        if (!billId || Number.isNaN(billId)) {
          return res.status(400).json({ status: "error", message: "referenceCode (billId) không hợp lệ" });
        }
        const bill = await dbGet("SELECT id, status FROM bills WHERE id = ?", [billId]);
        if (!bill) {
          return res.status(404).json({ status: "error", message: "Hóa đơn không tồn tại" });
        }
        if (bill.status === "PAID") {
          return res.status(400).json({ status: "error", message: "Hóa đơn đã được thanh toán, không thể gửi OTP" });
        }
      }
    }

    if (!checkRateLimit(`otp_send:${phone}`, 5, 10 * 60 * 1000)) {
      return res.status(429).json({ status: "error", message: "Gửi OTP quá nhiều lần. Vui lòng thử lại sau." });
    }

    // Với RESET_PASSWORD: không tạo OTP rác và không lộ thông tin tài khoản nếu SĐT chưa đăng ký
    if (purpose === "RESET_PASSWORD") {
      const existingUser = await dbGet("SELECT id FROM users WHERE phone = ?", [phone]);
      if (!existingUser) {
        return res.json({
          status: "success",
          message: "Nếu số điện thoại đã đăng ký, mã OTP đã được gửi.",
          expiresIn: config.OTP_EXPIRE_MINUTES * 60,
        });
      }
    }

    const { code } = await issueOTP(phone, purpose, referenceCode);
    audit(null, "OTP_SENT", `phone=${phone} purpose=${purpose} ref=${referenceCode}`, getIP(req));

    const payload = {
      status: "success",
      message: `Mã OTP đã được gửi tới số điện thoại ${maskSensitive(phone)}`,
      expiresIn: config.OTP_EXPIRE_MINUTES * 60,
    };

    // VÁ LỖ HỔNG #1: Tuyệt đối không trả otpCode trong HTTP JSON trừ khi bật EXPOSE_DEMO_OTP=true
    if (config.EXPOSE_DEMO_OTP) {
      payload.otpCode = code;
    }

    return res.json(payload);
  } catch (err) {
    console.error("otp/send:", err.message);
    return res.status(500).json({ status: "error", message: "Không thể gửi OTP" });
  }
});

// POST /api/otp/verify
app.post("/api/otp/verify", async (req, res) => {
  try {
    const phone = String(req.body.phone || "").trim();
    const code = String(req.body.code || "").trim();
    const purpose = String(req.body.purpose || "").trim().toUpperCase();

    if (!phone || !code || !purpose) {
      return res.status(400).json({ status: "error", message: "phone, code, purpose là bắt buộc" });
    }

    if (!["REGISTER", "RESET_PASSWORD"].includes(purpose)) {
      return res.status(400).json({ status: "error", message: "purpose không hỗ trợ xác thực độc lập" });
    }

    // VÁ LỖ HỔNG #3: Rate-limit + Đếm số lần sai tối đa 5 lần
    if (!checkRateLimit(`otp_verify:${phone}`, 10, 10 * 60 * 1000)) {
      return res.status(429).json({ status: "error", message: "Thử xác thực OTP quá nhiều lần. Vui lòng thử lại sau." });
    }

    const verification = await verifyOTPWithAttemptGuard(phone, purpose, code, "", true);
    if (!verification.ok) {
      audit(null, "OTP_VERIFY_FAILED", `phone=${phone} purpose=${purpose}`, getIP(req));
      return res.status(400).json({ status: "error", message: verification.message });
    }

    audit(null, "OTP_VERIFIED", `phone=${phone} purpose=${purpose}`, getIP(req));
    return res.json({ status: "success", message: "Xác thực OTP thành công" });
  } catch (err) {
    console.error("otp/verify:", err.message);
    return res.status(500).json({ status: "error", message: "Không thể xác thực OTP" });
  }
});

// POST /api/ekyc/verify-cccd
app.post("/api/ekyc/verify-cccd", async (req, res) => {
  try {
    const phone = String(req.body.phone || "").trim();
    const cccdNumber = String(req.body.cccdNumber || "").trim();
    const fullName = String(req.body.fullName || "").trim();

    if (!phone || !cccdNumber) {
      return res.status(400).json({ status: "error", message: "phone và cccdNumber là bắt buộc" });
    }

    if (!/^[0-9]{9}$|^[0-9]{12}$/.test(cccdNumber)) {
      return res.status(400).json({ status: "error", message: "Số CCCD không hợp lệ (phải 9 hoặc 12 chữ số)" });
    }

    const existingUser = await dbGet(
      "SELECT id FROM users WHERE cccdNumber = ? AND phone <> ?",
      [cccdNumber, phone]
    );
    if (existingUser) {
      return res.status(409).json({ status: "error", message: "Số CCCD đã được đăng ký với tài khoản khác" });
    }

    const expiresAt = new Date(Date.now() + 30 * 60 * 1000).toISOString();
    const existing = await dbGet("SELECT id FROM ekyc_verifications WHERE phone = ?", [phone]);
    if (existing) {
      await dbRun(
        `UPDATE ekyc_verifications
         SET cccd_number = ?, full_name = ?, cccd_verified = 1, face_verified = 0,
             ekyc_status = 'PENDING', expires_at = ?
         WHERE phone = ?`,
        [cccdNumber, fullName, expiresAt, phone]
      );
    } else {
      await dbRun(
        `INSERT INTO ekyc_verifications (phone, cccd_number, full_name, cccd_verified, ekyc_status, expires_at)
         VALUES (?, ?, ?, 1, 'PENDING', ?)`,
        [phone, cccdNumber, fullName, expiresAt]
      );
    }

    audit(null, "EKYC_CCCD_VERIFIED", `phone=${phone} cccd=${maskSensitive(cccdNumber)}`, getIP(req));

    return res.json({
      status: "success",
      message: "Xác minh CCCD thành công",
      data: { cccdNumber: maskSensitive(cccdNumber), fullName, verifiedAt: new Date().toISOString() },
    });
  } catch (err) {
    console.error("ekyc/verify-cccd:", err.message);
    return res.status(500).json({ status: "error", message: "Không thể xác minh CCCD" });
  }
});

// POST /api/ekyc/verify-face
app.post("/api/ekyc/verify-face", async (req, res) => {
  try {
    const phone = String(req.body.phone || "").trim();
    if (!phone) {
      return res.status(400).json({ status: "error", message: "phone là bắt buộc" });
    }

    const ekyc = await dbGet(
      "SELECT id, cccd_verified, expires_at FROM ekyc_verifications WHERE phone = ?",
      [phone]
    );
    if (!ekyc || !ekyc.cccd_verified) {
      return res.status(400).json({ status: "error", message: "Vui lòng xác minh CCCD trước" });
    }

    if (new Date() > new Date(ekyc.expires_at)) {
      return res.status(400).json({ status: "error", message: "Phiên eKYC đã hết hạn. Vui lòng xác minh CCCD lại." });
    }

    const score = parseFloat((0.85 + (crypto.randomInt(0, 1400) / 10000)).toFixed(4));

    await dbRun(
      `UPDATE ekyc_verifications
       SET face_verified = 1, face_score = ?, ekyc_status = 'VERIFIED'
       WHERE phone = ?`,
      [score, phone]
    );

    audit(null, "EKYC_FACE_VERIFIED", `phone=${phone} score=${score}`, getIP(req));

    return res.json({
      status: "success",
      message: "Xác minh khuôn mặt thành công",
      data: { ekycStatus: "VERIFIED", faceScore: score, verifiedAt: new Date().toISOString() },
    });
  } catch (err) {
    console.error("ekyc/verify-face:", err.message);
    return res.status(500).json({ status: "error", message: "Không thể xác minh khuôn mặt" });
  }
});

// POST /api/auth/register
app.post("/api/auth/register", async (req, res) => {
  try {
    const body = req.body || {};

    const fullName = String(body.fullName || "").trim();
    const phone = String(body.phone || "").trim();
    const password = String(body.password || "");
    const email = String(body.email || "").trim();
    const cccdNumber = String(body.cccdNumber || "").trim();
    const job = String(body.job || "").trim();
    const accountType = String(body.accountType || "").trim() || "Tài khoản thường";
    const rawAccountFee = Number(body.accountFee);
    const accountFee = Number.isFinite(rawAccountFee) && rawAccountFee >= 0 ? rawAccountFee : 0;
    const rawUsername = String(body.username || "").trim();
    const rawAccNum = String(body.accountNumber || "").trim();

    if (!fullName || !phone || !password) {
      return res.status(400).json({ status: "error", message: "Full name, phone and password are required" });
    }
    if (!/^[0-9]{9,11}$/.test(phone)) {
      return res.status(400).json({ status: "error", message: "Invalid phone number" });
    }
    if (password.length < 6) {
      return res.status(400).json({ status: "error", message: "Password must be at least 6 characters" });
    }
    if (!cccdNumber || !/^[0-9]{9}$|^[0-9]{12}$/.test(cccdNumber)) {
      return res.status(400).json({ status: "error", message: "CCCD phải 9 hoặc 12 chữ số" });
    }
    if (rawUsername && !/^[a-zA-Z0-9_.]{3,30}$/.test(rawUsername)) {
      return res.status(400).json({ status: "error", message: "Username must be 3-30 chars, alphanumeric/dot/underscore" });
    }
    if (rawAccNum && !/^[0-9]{6,14}$/.test(rawAccNum)) {
      return res.status(400).json({ status: "error", message: "Account number must be 6-14 digits" });
    }

    const otpRow = await dbGet(
      `SELECT id FROM otp_codes
       WHERE phone = ? AND purpose = 'REGISTER' AND reference_code = ''
         AND verified = 1 AND consumed = 0
         AND julianday(expires_at) > julianday('now')
       ORDER BY id DESC LIMIT 1`,
      [phone]
    );
    if (!otpRow) {
      return res.status(400).json({ status: "error", message: "Chưa xác thực OTP đăng ký hoặc OTP đã hết hạn" });
    }

    const ekyc = await dbGet(
      `SELECT cccd_number, cccd_verified, face_verified, ekyc_status, expires_at
       FROM ekyc_verifications WHERE phone = ?`,
      [phone]
    );
    if (!ekyc || !ekyc.cccd_verified) {
      return res.status(400).json({ status: "error", message: "Chưa xác minh CCCD. Vui lòng gọi /api/ekyc/verify-cccd trước." });
    }
    if (!ekyc.face_verified || ekyc.ekyc_status !== "VERIFIED") {
      return res.status(400).json({ status: "error", message: "Chưa xác minh khuôn mặt eKYC." });
    }
    if (new Date() > new Date(ekyc.expires_at)) {
      return res.status(400).json({ status: "error", message: "Phiên eKYC đã hết hạn. Vui lòng xác minh lại." });
    }
    if (ekyc.cccd_number !== cccdNumber) {
      return res.status(400).json({ status: "error", message: "Số CCCD không khớp với thông tin đã xác minh" });
    }

    const finalUsername = rawUsername || phone;
    const finalAccNum = rawAccNum || (await generateUniqueAccountNumber());
    const passwordHash = await bcrypt.hash(password, 10);

    let newUserId;
    try {
      const { lastID } = await runTransaction(async () => {
        const existing = await dbGet(
          `SELECT id, phone, accountNumber, username, cccdNumber FROM users
           WHERE phone = ? OR accountNumber = ?
              OR (username = ? AND username IS NOT NULL AND username <> '')
              OR (cccdNumber = ? AND cccdNumber IS NOT NULL AND cccdNumber <> '')`,
          [phone, finalAccNum, finalUsername, cccdNumber]
        );
        if (existing) {
          if (existing.phone === phone)
            throw Object.assign(new Error("Phone number already exists"), { statusCode: 409 });
          if (existing.accountNumber === finalAccNum)
            throw Object.assign(new Error("Account number already exists"), { statusCode: 409 });
          if (existing.username === finalUsername)
            throw Object.assign(new Error("Username already exists"), { statusCode: 409 });
          if (existing.cccdNumber === cccdNumber)
            throw Object.assign(new Error("Số CCCD đã đăng ký với tài khoản khác"), { statusCode: 409 });
        }

        // Tiêu hủy OTP & phiên eKYC tạm sau khi tạo tài khoản thành công
        await dbRun("UPDATE otp_codes SET consumed = 1 WHERE id = ?", [otpRow.id]);
        await dbRun("DELETE FROM ekyc_verifications WHERE phone = ?", [phone]);

        return dbRun(
          `INSERT INTO users
             (full_name, phone, email, password_hash, balance, username,
              accountNumber, accountType, accountFee, cccdNumber, job, status, ekycStatus)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', 'VERIFIED')`,
          [
            fullName,
            phone,
            email,
            passwordHash,
            1_000_000,
            finalUsername,
            finalAccNum,
            accountType,
            accountFee,
            cccdNumber,
            job,
          ]
        );
      });
      newUserId = lastID;
    } catch (txErr) {
      if (txErr.statusCode === 409) {
        return res.status(409).json({ status: "error", message: txErr.message });
      }
      throw txErr;
    }

    audit(newUserId, "REGISTER", `phone=${phone} accountNumber=${finalAccNum}`, getIP(req));

    return res.status(201).json({
      status: "success",
      message: "Đăng ký tài khoản thành công",
      userId: newUserId,
      accountNumber: finalAccNum,
    });
  } catch (err) {
    console.error("register:", err.message);
    return res.status(500).json({ status: "error", message: "Register failed" });
  }
});

// =============================================================
// UC_002 — ĐĂNG NHẬP, ĐĂNG XUẤT & ĐẶT LẠI MẬT KHẨU
// =============================================================

app.post("/api/auth/login", async (req, res) => {
  try {
    const phone = String(req.body.phone || "").trim();
    const password = String(req.body.password || "");

    if (!phone || !password) {
      return res.status(400).json({ status: "error", message: "Phone and password are required" });
    }

    if (!checkRateLimit(`login:${phone}`, 10, 15 * 60 * 1000)) {
      return res.status(429).json({ status: "error", message: "Đăng nhập quá nhiều lần. Thử lại sau." });
    }

    const user = await dbGet("SELECT * FROM users WHERE phone = ?", [phone]);
    if (!user) {
      audit(null, "LOGIN_FAILED", `phone=${phone} reason=not_found`, getIP(req));
      return res.status(401).json({ status: "error", message: "Invalid phone or password" });
    }

    if (user.lockedUntil && new Date(user.lockedUntil) > new Date()) {
      const unlockAt = new Date(user.lockedUntil).toLocaleTimeString("vi-VN");
      audit(user.id, "LOGIN_FAILED", `reason=locked until ${user.lockedUntil}`, getIP(req));
      return res.status(403).json({
        status: "error",
        message: `Tài khoản bị khóa tạm thời. Thử lại sau ${unlockAt}`,
      });
    }

    if (user.status !== "ACTIVE") {
      audit(user.id, "LOGIN_FAILED", `reason=status=${user.status}`, getIP(req));
      return res.status(403).json({ status: "error", message: "Tài khoản không hoạt động" });
    }

    const isMatch = await bcrypt.compare(password, user.password_hash);
    if (!isMatch) {
      const failCount = (user.loginFailCount || 0) + 1;
      if (failCount >= config.LOGIN_MAX_FAIL) {
        const lockUntil = new Date(Date.now() + config.LOGIN_LOCK_MINUTES * 60 * 1000).toISOString();
        await dbRun("UPDATE users SET loginFailCount = ?, lockedUntil = ? WHERE id = ?", [
          failCount,
          lockUntil,
          user.id,
        ]);
        audit(user.id, "LOGIN_FAILED", `reason=wrong_password fail=${failCount} locked`, getIP(req));
        return res.status(403).json({
          status: "error",
          message: `Sai mật khẩu quá ${config.LOGIN_MAX_FAIL} lần. Tài khoản bị khóa ${config.LOGIN_LOCK_MINUTES} phút.`,
        });
      }
      await dbRun("UPDATE users SET loginFailCount = ? WHERE id = ?", [failCount, user.id]);
      audit(user.id, "LOGIN_FAILED", `reason=wrong_password fail=${failCount}`, getIP(req));
      return res.status(401).json({
        status: "error",
        message: `Sai mật khẩu. Còn ${config.LOGIN_MAX_FAIL - failCount} lần thử.`,
      });
    }

    await dbRun("UPDATE users SET loginFailCount = 0, lockedUntil = NULL WHERE id = ?", [user.id]);

    const token = jwt.sign(
      { id: user.id, phone: user.phone, jti: crypto.randomBytes(8).toString("hex") },
      config.JWT_SECRET,
      { expiresIn: "1d" }
    );
    const expiresAt = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
    const tokenHash = crypto.createHash("sha256").update(token).digest("hex");

    await dbRun("INSERT INTO sessions (user_id, token_hash, expires_at) VALUES (?, ?, ?)", [
      user.id,
      tokenHash,
      expiresAt,
    ]);

    audit(user.id, "LOGIN_SUCCESS", `phone=${phone}`, getIP(req));

    return res.json({
      status: "success",
      message: "Login successfully",
      token,
      user: formatUser(user),
    });
  } catch (err) {
    console.error("login:", err.message);
    return res.status(500).json({ status: "error", message: "Login failed" });
  }
});

app.post("/api/auth/logout", authenticateToken, async (req, res) => {
  try {
    await dbRun("DELETE FROM sessions WHERE user_id = ? AND token_hash = ?", [
      req.user.id,
      req.tokenHash,
    ]);
    audit(req.user.id, "LOGOUT", "", getIP(req));
    return res.json({ status: "success", message: "Đăng xuất thành công" });
  } catch (err) {
    console.error("logout:", err.message);
    return res.status(500).json({ status: "error", message: "Logout failed" });
  }
});

app.get("/api/auth/me", authenticateToken, async (req, res) => {
  try {
    const user = await dbGet(
      `SELECT id, full_name, phone, email, balance, username, accountNumber,
              accountType, accountFee, cccdNumber, job, status, ekycStatus, created_at
       FROM users WHERE id = ?`,
      [req.user.id]
    );
    if (!user) return res.status(404).json({ status: "error", message: "User not found" });
    return res.json({ status: "success", data: formatUser(user) });
  } catch (err) {
    console.error("auth/me:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get account" });
  }
});

// POST /api/auth/reset-password
// VÁ LỖ HỔNG #4: Tiêu hủy OTP ngay khi đổi mật khẩu (chống replay) + Thu hồi toàn bộ session cũ
app.post("/api/auth/reset-password", async (req, res) => {
  try {
    const phone = String(req.body.phone || "").trim();
    const newPassword = String(req.body.newPassword || "");

    if (!phone || !newPassword) {
      return res.status(400).json({ status: "error", message: "Thiếu dữ liệu" });
    }
    if (newPassword.length < 6) {
      return res.status(400).json({ status: "error", message: "Mật khẩu mới phải có ít nhất 6 ký tự" });
    }

    const otpRow = await dbGet(
      `SELECT id FROM otp_codes
       WHERE phone = ? AND purpose = 'RESET_PASSWORD' AND reference_code = ''
         AND verified = 1 AND consumed = 0
         AND julianday(expires_at) > julianday('now')
       ORDER BY id DESC LIMIT 1`,
      [phone]
    );
    if (!otpRow) {
      return res.status(400).json({
        status: "error",
        message: "Chưa xác thực OTP đặt lại mật khẩu hoặc OTP đã hết hiệu lực",
      });
    }

    const user = await dbGet("SELECT id FROM users WHERE phone = ?", [phone]);
    if (!user) {
      return res.status(404).json({ status: "error", message: "Số điện thoại chưa đăng ký" });
    }

    const newHash = await bcrypt.hash(newPassword, 10);

    const { changes } = await runTransaction(async () => {
      // Đánh dấu consumed = 1 để không thể dùng lại OTP này lần thứ 2
      const otpConsume = await dbRun(
        "UPDATE otp_codes SET consumed = 1 WHERE id = ? AND consumed = 0",
        [otpRow.id]
      );
      if (otpConsume.changes === 0) {
        throw Object.assign(new Error("OTP đã được sử dụng"), { statusCode: 400 });
      }

      // Thu hồi toàn bộ phiên đăng nhập cũ của tài khoản
      await dbRun("DELETE FROM sessions WHERE user_id = ?", [user.id]);

      return dbRun(
        "UPDATE users SET password_hash = ?, loginFailCount = 0, lockedUntil = NULL WHERE id = ?",
        [newHash, user.id]
      );
    });

    if (changes === 0) {
      return res.status(500).json({ status: "error", message: "Không cập nhật được mật khẩu" });
    }

    audit(user.id, "RESET_PASSWORD_AND_REVOKE_SESSIONS", `phone=${phone}`, getIP(req));
    return res.json({ status: "success", message: "Đổi mật khẩu thành công. Vui lòng đăng nhập lại." });
  } catch (err) {
    if (err.statusCode === 400) {
      return res.status(400).json({ status: "error", message: err.message });
    }
    console.error("reset-password:", err.message);
    return res.status(500).json({ status: "error", message: "Lỗi server" });
  }
});

// =============================================================
// UC_003 — TRUY VẤN TÀI KHOẢN & LỊCH SỬ GIAO DỊCH
// =============================================================

app.get("/api/account/me", authenticateToken, async (req, res) => {
  try {
    const user = await dbGet(
      `SELECT id, full_name, phone, email, balance, username, accountNumber,
              accountType, accountFee, cccdNumber, job, status, ekycStatus, created_at
       FROM users WHERE id = ?`,
      [req.user.id]
    );
    if (!user) return res.status(404).json({ status: "error", message: "User not found" });
    return res.json({ status: "success", data: formatUser(user) });
  } catch (err) {
    console.error("account/me:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get account information" });
  }
});

// VÁ LỖ HỔNG #6: Không trả số điện thoại cá nhân (phone) khi tra cứu STK người nhận
app.get("/api/account/lookup/:accountNumber", authenticateToken, async (req, res) => {
  try {
    const acc = String(req.params.accountNumber || "").trim();
    if (!acc) {
      return res.status(400).json({ status: "error", message: "Account number is required" });
    }

    if (!checkRateLimit(`lookup:${req.user.id}`, 30, 60 * 1000)) {
      return res.status(429).json({ status: "error", message: "Tra cứu quá nhanh, vui lòng thử lại sau." });
    }

    const receiver = await dbGet(
      "SELECT id, full_name, accountNumber, accountType FROM users WHERE accountNumber = ?",
      [acc]
    );
    if (!receiver) {
      return res.status(404).json({ status: "error", message: "Receiver account not found" });
    }
    if (receiver.id === req.user.id) {
      return res.status(400).json({ status: "error", message: "Cannot transfer to yourself" });
    }

    return res.json({
      status: "success",
      message: "Receiver found",
      data: {
        id: receiver.id,
        fullName: receiver.full_name,
        accountNumber: receiver.accountNumber,
        accountType: receiver.accountType || "",
      },
    });
  } catch (err) {
    console.error("lookup:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot lookup receiver" });
  }
});

app.get("/api/transactions", authenticateToken, async (req, res) => {
  try {
    const { type, status, fromDate, toDate } = req.query;
    const page = Math.max(1, parseInt(req.query.page || "1", 10));
    const limit = Math.min(100, Math.max(1, parseInt(req.query.limit || "20", 10)));
    const offset = (page - 1) * limit;

    const conditions = ["user_id = ?"];
    const params = [req.user.id];

    if (type) {
      conditions.push("type = ?");
      params.push(String(type).toUpperCase());
    }
    if (status) {
      conditions.push("status = ?");
      params.push(String(status).toUpperCase());
    }
    if (fromDate) {
      conditions.push("created_at >= ?");
      params.push(String(fromDate));
    }
    if (toDate) {
      conditions.push("created_at <= ?");
      params.push(String(toDate) + " 23:59:59");
    }

    const where = conditions.join(" AND ");
    const total = await dbGet(`SELECT COUNT(*) AS c FROM transactions WHERE ${where}`, params);
    const rows = await dbAll(
      `SELECT * FROM transactions WHERE ${where} ORDER BY created_at DESC LIMIT ? OFFSET ?`,
      [...params, limit, offset]
    );

    return res.json({
      status: "success",
      data: { total: total.c, page, limit, transactions: rows.map(formatTx) },
    });
  } catch (err) {
    console.error("transactions:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get transactions" });
  }
});

app.get("/api/transactions/summary", authenticateToken, async (req, res) => {
  try {
    const summary = await dbGet(
      `SELECT
         COUNT(*) AS transactionCount,
         SUM(CASE WHEN type IN ('INCOME','TRANSFER_IN') THEN amount ELSE 0 END) AS totalMoneyIn,
         SUM(CASE WHEN type IN ('EXPENSE','TRANSFER_OUT','BILL_PAYMENT') THEN amount ELSE 0 END) AS totalMoneyOut,
         SUM(CASE WHEN type = 'INCOME' THEN amount ELSE 0 END) AS totalIncome,
         SUM(CASE WHEN type = 'TRANSFER_IN' THEN amount ELSE 0 END) AS totalTransferIn,
         SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS totalExpense,
         SUM(CASE WHEN type = 'TRANSFER_OUT' THEN amount ELSE 0 END) AS totalTransferOut,
         SUM(CASE WHEN type = 'BILL_PAYMENT' THEN amount ELSE 0 END) AS totalBillPayment
       FROM transactions WHERE user_id = ?`,
      [req.user.id]
    );
    const user = await dbGet("SELECT balance FROM users WHERE id = ?", [req.user.id]);
    if (!user) return res.status(404).json({ status: "error", message: "User not found" });

    return res.json({
      status: "success",
      data: {
        balance: user.balance,
        transactionCount: summary.transactionCount || 0,
        totalMoneyIn: summary.totalMoneyIn || 0,
        totalMoneyOut: summary.totalMoneyOut || 0,
        totalIncome: summary.totalIncome || 0,
        totalTransferIn: summary.totalTransferIn || 0,
        totalExpense: summary.totalExpense || 0,
        totalTransferOut: summary.totalTransferOut || 0,
        totalBillPayment: summary.totalBillPayment || 0,
      },
    });
  } catch (err) {
    console.error("summary:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get transaction summary" });
  }
});

app.get("/api/transactions/:id", authenticateToken, async (req, res) => {
  try {
    const txId = parseInt(req.params.id, 10);
    if (!txId) {
      return res.status(400).json({ status: "error", message: "Invalid transaction id" });
    }
    const tx = await dbGet("SELECT * FROM transactions WHERE id = ? AND user_id = ?", [
      txId,
      req.user.id,
    ]);
    if (!tx) return res.status(404).json({ status: "error", message: "Transaction not found" });
    return res.json({ status: "success", data: formatTx(tx) });
  } catch (err) {
    console.error("transaction/:id:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get transaction" });
  }
});

// =============================================================
// UC_004 — CHUYỂN TIỀN NỘI BỘ (2 BƯỚC INITIATE + CONFIRM OTP)
// =============================================================

app.post("/api/transfers/initiate", authenticateToken, requireActiveAccount, async (req, res) => {
  try {
    const receiverAccountNumber = String(req.body.receiverAccountNumber || "").trim();
    const money = Number(req.body.amount);
    const content = String(req.body.content || "").trim();
    const idempotencyKey = String(req.body.idempotencyKey || "").trim();

    if (!receiverAccountNumber) {
      return res.status(400).json({ status: "error", message: "Receiver account number is required" });
    }
    if (!Number.isFinite(money) || money <= 0) {
      return res.status(400).json({ status: "error", message: "Amount must be greater than 0" });
    }
    if (money > config.TRANSFER_SINGLE_LIMIT) {
      return res.status(400).json({
        status: "error",
        message: `Hạn mức mỗi lần chuyển tối đa ${config.TRANSFER_SINGLE_LIMIT.toLocaleString()} VND`,
      });
    }

    if (idempotencyKey) {
      const existing = await dbGet(
        "SELECT * FROM transfer_requests WHERE sender_id = ? AND idempotency_key = ? AND status = 'PENDING'",
        [req.user.id, idempotencyKey]
      );
      if (existing) {
        return res.json({
          status: "success",
          message: "Yêu cầu chuyển tiền đã tồn tại (idempotent)",
          data: { referenceCode: existing.reference_code, status: existing.status },
        });
      }
    }

    const sender = await dbGet("SELECT * FROM users WHERE id = ?", [req.user.id]);
    if (!sender) return res.status(404).json({ status: "error", message: "Sender not found" });
    if (money > sender.balance) {
      return res.status(400).json({ status: "error", message: "Insufficient balance" });
    }

    const today = new Date().toISOString().slice(0, 10);
    const dailyRow = await dbGet(
      `SELECT SUM(amount) AS dailyTotal FROM transactions
       WHERE user_id = ? AND type = 'TRANSFER_OUT' AND status = 'SUCCESS' AND created_at >= ?`,
      [req.user.id, today]
    );
    const dailyUsed = dailyRow.dailyTotal || 0;
    if (dailyUsed + money > config.TRANSFER_DAILY_LIMIT) {
      return res.status(400).json({
        status: "error",
        message: `Hạn mức ngày tối đa ${config.TRANSFER_DAILY_LIMIT.toLocaleString()} VND (đã dùng ${dailyUsed.toLocaleString()} VND)`,
      });
    }

    const receiver = await dbGet("SELECT * FROM users WHERE accountNumber = ?", [receiverAccountNumber]);
    if (!receiver) {
      return res.status(404).json({ status: "error", message: "Receiver account not found" });
    }
    if (receiver.id === sender.id) {
      return res.status(400).json({ status: "error", message: "Cannot transfer to yourself" });
    }

    const refCode = generateRef("TRF");
    const expiresAt = new Date(Date.now() + 10 * 60 * 1000).toISOString();

    await dbRun(
      `INSERT INTO transfer_requests
         (reference_code, sender_id, receiver_id, amount, fee, content, status, idempotency_key, expires_at)
       VALUES (?, ?, ?, ?, ?, ?, 'PENDING', ?, ?)`,
      [refCode, sender.id, receiver.id, money, 0, content, idempotencyKey || null, expiresAt]
    );

    const { code: otpCode } = await issueOTP(sender.phone, "TRANSFER", refCode);

    audit(
      sender.id,
      "TRANSFER_INITIATED",
      `ref=${refCode} to=${receiver.accountNumber} amount=${money}`,
      getIP(req)
    );

    const data = {
      referenceCode: refCode,
      receiverName: receiver.full_name,
      receiverAccount: receiver.accountNumber,
      amount: money,
      fee: 0,
      expiresAt,
    };
    if (config.EXPOSE_DEMO_OTP) {
      data.otpCode = otpCode;
    }

    return res.json({
      status: "success",
      message: "Yêu cầu chuyển tiền đã khởi tạo. Vui lòng nhập mã OTP để xác nhận.",
      data,
    });
  } catch (err) {
    console.error("transfers/initiate:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot initiate transfer" });
  }
});

app.post("/api/transfers/confirm", authenticateToken, requireActiveAccount, async (req, res) => {
  try {
    const referenceCode = String(req.body.referenceCode || "").trim();
    const otpCode = String(req.body.otpCode || "").trim();

    if (!referenceCode || !otpCode) {
      return res.status(400).json({ status: "error", message: "referenceCode và otpCode là bắt buộc" });
    }

    const request = await dbGet(
      "SELECT * FROM transfer_requests WHERE reference_code = ? AND sender_id = ?",
      [referenceCode, req.user.id]
    );
    if (!request) {
      return res.status(404).json({ status: "error", message: "Không tìm thấy yêu cầu chuyển tiền" });
    }
    if (request.status !== "PENDING") {
      return res.status(400).json({ status: "error", message: `Giao dịch đã ở trạng thái ${request.status}` });
    }
    if (new Date() > new Date(request.expires_at)) {
      return res.status(400).json({ status: "error", message: "Yêu cầu chuyển tiền đã hết hạn" });
    }

    const sender = await dbGet("SELECT * FROM users WHERE id = ?", [req.user.id]);

    const otpCheck = await verifyOTPWithAttemptGuard(sender.phone, "TRANSFER", otpCode, referenceCode, false);
    if (!otpCheck.ok) {
      return res.status(400).json({ status: "error", message: otpCheck.message });
    }
    const otpRow = otpCheck.row;

    const receiver = await dbGet("SELECT * FROM users WHERE id = ?", [request.receiver_id]);
    if (!receiver) {
      return res.status(404).json({ status: "error", message: "Tài khoản nhận không tồn tại" });
    }

    const money = request.amount;

    try {
      const result = await runTransaction(async () => {
        const today = new Date().toISOString().slice(0, 10);
        const dailyRow = await dbGet(
          `SELECT SUM(amount) AS dailyTotal FROM transactions
           WHERE user_id = ? AND type = 'TRANSFER_OUT' AND status = 'SUCCESS' AND created_at >= ?`,
          [sender.id, today]
        );
        const dailyUsed = dailyRow.dailyTotal || 0;
        if (dailyUsed + money > config.TRANSFER_DAILY_LIMIT) {
          throw Object.assign(
            new Error(
              `Hạn mức ngày tối đa ${config.TRANSFER_DAILY_LIMIT.toLocaleString()} VND (đã dùng ${dailyUsed.toLocaleString()} VND).`
            ),
            { statusCode: 400, dailyLimitExceeded: true }
          );
        }

        const consumeRes = await dbRun(
          "UPDATE otp_codes SET verified = 1, consumed = 1 WHERE id = ? AND consumed = 0",
          [otpRow.id]
        );
        if (consumeRes.changes === 0) {
          throw Object.assign(new Error("Mã OTP đã được sử dụng"), { statusCode: 400 });
        }

        const { changes } = await dbRun(
          "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?",
          [money, sender.id, money]
        );
        if (changes === 0) {
          throw Object.assign(new Error("Insufficient balance"), { statusCode: 400 });
        }

        await dbRun("UPDATE users SET balance = balance + ? WHERE id = ?", [money, receiver.id]);

        const senderBalance = (await dbGet("SELECT balance FROM users WHERE id = ?", [sender.id])).balance;
        const receiverBalance = (await dbGet("SELECT balance FROM users WHERE id = ?", [receiver.id])).balance;

        const refOut = generateRef("OUT");
        const refIn = generateRef("IN");
        const desc = request.content || `Chuyển tiền tới ${receiver.full_name}`;

        const { lastID: txOutId } = await dbRun(
          `INSERT INTO transactions
             (reference_code, user_id, type, amount, fee, status, content,
              counterparty_name, counterparty_account, balance_after)
           VALUES (?, ?, 'TRANSFER_OUT', ?, 0, 'SUCCESS', ?, ?, ?, ?)`,
          [refOut, sender.id, money, desc, receiver.full_name, receiver.accountNumber, senderBalance]
        );

        await dbRun(
          `INSERT INTO transactions
             (reference_code, user_id, type, amount, fee, status, content,
              counterparty_name, counterparty_account, balance_after)
           VALUES (?, ?, 'TRANSFER_IN', ?, 0, 'SUCCESS', ?, ?, ?, ?)`,
          [
            refIn,
            receiver.id,
            money,
            request.content || `Nhận tiền từ ${sender.full_name}`,
            sender.full_name,
            sender.accountNumber || sender.phone,
            receiverBalance,
          ]
        );

        await dbRun(
          "UPDATE transfer_requests SET status = 'SUCCESS', otp_verified = 1 WHERE reference_code = ?",
          [referenceCode]
        );

        return { transactionId: txOutId, balance: senderBalance };
      });

      audit(
        sender.id,
        "TRANSFER_SUCCESS",
        `ref=${referenceCode} to=${receiver.accountNumber} amount=${money}`,
        getIP(req)
      );

      return res.json({
        status: "success",
        message: "Chuyển tiền thành công",
        transactionId: result.transactionId,
        balance: result.balance,
        receiver: {
          fullName: receiver.full_name,
          accountNumber: receiver.accountNumber,
        },
      });
    } catch (txErr) {
      if (!txErr.dailyLimitExceeded) {
        await dbRun("UPDATE transfer_requests SET status = 'FAILED' WHERE reference_code = ?", [
          referenceCode,
        ]);
        audit(sender.id, "TRANSFER_FAILED", `ref=${referenceCode} reason=${txErr.message}`, getIP(req));
      } else {
        audit(sender.id, "TRANSFER_DAILY_LIMIT_EXCEEDED", `ref=${referenceCode} amount=${money}`, getIP(req));
      }
      if (txErr.statusCode === 400) {
        return res.status(400).json({ status: "error", message: txErr.message });
      }
      throw txErr;
    }
  } catch (err) {
    console.error("transfers/confirm:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot confirm transfer" });
  }
});

// POST /api/transactions/transfer (Alias có bắt buộc OTP + chống brute-force)
app.post("/api/transactions/transfer", authenticateToken, requireActiveAccount, async (req, res) => {
  try {
    const receiverAccountNumber = String(req.body.receiverAccountNumber || "").trim();
    const money = Number(req.body.amount);
    const content = String(req.body.content || "").trim();
    const otpCode = String(req.body.otpCode || "").trim();

    if (!otpCode) {
      return res.status(400).json({
        status: "error",
        message: "OTP là bắt buộc. Vui lòng gọi /api/transfers/initiate hoặc /api/otp/send trước.",
      });
    }
    if (!receiverAccountNumber) {
      return res.status(400).json({ status: "error", message: "Receiver account number is required" });
    }
    if (!Number.isFinite(money) || money <= 0) {
      return res.status(400).json({ status: "error", message: "Amount must be greater than 0" });
    }
    if (money > config.TRANSFER_SINGLE_LIMIT) {
      return res.status(400).json({
        status: "error",
        message: `Hạn mức mỗi lần chuyển tối đa ${config.TRANSFER_SINGLE_LIMIT.toLocaleString()} VND`,
      });
    }

    const sender = await dbGet("SELECT * FROM users WHERE id = ?", [req.user.id]);
    if (!sender) return res.status(404).json({ status: "error", message: "Sender not found" });
    if (money > sender.balance) {
      return res.status(400).json({ status: "error", message: "Insufficient balance" });
    }

    const receiver = await dbGet("SELECT * FROM users WHERE accountNumber = ?", [receiverAccountNumber]);
    if (!receiver) {
      return res.status(404).json({ status: "error", message: "Receiver account not found" });
    }
    if (receiver.id === sender.id) {
      return res.status(400).json({ status: "error", message: "Cannot transfer to yourself" });
    }

    const otpCheck = await verifyOTPWithAttemptGuard(sender.phone, "TRANSFER", otpCode, "", false);
    if (!otpCheck.ok) {
      return res.status(400).json({ status: "error", message: otpCheck.message });
    }
    const otpRow = otpCheck.row;

    const today = new Date().toISOString().slice(0, 10);

    const result = await runTransaction(async () => {
      const dailyRowTx = await dbGet(
        `SELECT SUM(amount) AS dailyTotal FROM transactions
         WHERE user_id = ? AND type = 'TRANSFER_OUT' AND status = 'SUCCESS' AND created_at >= ?`,
        [sender.id, today]
      );
      const dailyUsed = dailyRowTx.dailyTotal || 0;
      if (dailyUsed + money > config.TRANSFER_DAILY_LIMIT) {
        throw Object.assign(
          new Error(
            `Hạn mức ngày tối đa ${config.TRANSFER_DAILY_LIMIT.toLocaleString()} VND (đã dùng ${dailyUsed.toLocaleString()} VND)`
          ),
          { statusCode: 400 }
        );
      }

      const consumeRes = await dbRun(
        "UPDATE otp_codes SET verified = 1, consumed = 1 WHERE id = ? AND consumed = 0",
        [otpRow.id]
      );
      if (consumeRes.changes === 0) {
        throw Object.assign(new Error("Mã OTP đã được sử dụng"), { statusCode: 400 });
      }

      const { changes } = await dbRun(
        "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?",
        [money, sender.id, money]
      );
      if (changes === 0) {
        throw Object.assign(new Error("Insufficient balance"), { statusCode: 400 });
      }

      await dbRun("UPDATE users SET balance = balance + ? WHERE id = ?", [money, receiver.id]);

      const senderBal = (await dbGet("SELECT balance FROM users WHERE id = ?", [sender.id])).balance;
      const receiverBal = (await dbGet("SELECT balance FROM users WHERE id = ?", [receiver.id])).balance;

      const { lastID: txOutId } = await dbRun(
        `INSERT INTO transactions
           (reference_code, user_id, type, amount, fee, status, content,
            counterparty_name, counterparty_account, balance_after)
         VALUES (?, ?, 'TRANSFER_OUT', ?, 0, 'SUCCESS', ?, ?, ?, ?)`,
        [
          generateRef("OUT"),
          sender.id,
          money,
          content || `Chuyển tiền tới ${receiver.full_name}`,
          receiver.full_name,
          receiver.accountNumber,
          senderBal,
        ]
      );

      await dbRun(
        `INSERT INTO transactions
           (reference_code, user_id, type, amount, fee, status, content,
            counterparty_name, counterparty_account, balance_after)
         VALUES (?, ?, 'TRANSFER_IN', ?, 0, 'SUCCESS', ?, ?, ?, ?)`,
        [
          generateRef("IN"),
          receiver.id,
          money,
          content || `Nhận tiền từ ${sender.full_name}`,
          sender.full_name,
          sender.accountNumber || sender.phone,
          receiverBal,
        ]
      );

      return { transactionId: txOutId, balance: senderBal };
    });

    audit(sender.id, "TRANSFER_SUCCESS", `(alias+otp) to=${receiver.accountNumber} amount=${money}`, getIP(req));

    return res.json({
      status: "success",
      message: "Transfer successfully",
      transactionId: result.transactionId,
      balance: result.balance,
      receiver: {
        fullName: receiver.full_name,
        accountNumber: receiver.accountNumber,
      },
    });
  } catch (err) {
    if (err.statusCode === 400) {
      return res.status(400).json({ status: "error", message: err.message });
    }
    console.error("transactions/transfer:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot process transfer" });
  }
});

// VÁ LỖ HỔNG #2: Chặn người dùng thường tự nạp tiền ảo — Bắt buộc quyền Admin (x-admin-key)
app.post("/api/transactions/income", requireAdmin, async (req, res) => {
  const targetUserId = Number(req.body.userId);
  const targetAccountNumber = String(req.body.accountNumber || "").trim();
  const money = Number(req.body.amount);
  const content = String(req.body.content || "Nạp tiền tại quầy (Admin)").trim();

  if (!Number.isFinite(money) || money <= 0) {
    return res.status(400).json({ status: "error", message: "Amount must be greater than 0" });
  }

  try {
    const user = targetUserId
      ? await dbGet("SELECT id FROM users WHERE id = ?", [targetUserId])
      : await dbGet("SELECT id FROM users WHERE accountNumber = ?", [targetAccountNumber]);

    if (!user) {
      return res.status(404).json({ status: "error", message: "Target user account not found" });
    }

    const result = await runTransaction(async () => {
      await dbRun("UPDATE users SET balance = balance + ? WHERE id = ?", [money, user.id]);
      const bal = (await dbGet("SELECT balance FROM users WHERE id = ?", [user.id])).balance;
      const { lastID } = await dbRun(
        `INSERT INTO transactions (reference_code, user_id, type, amount, fee, status, content, balance_after)
         VALUES (?, ?, 'INCOME', ?, 0, 'SUCCESS', ?, ?)`,
        [generateRef("INC"), user.id, money, content, bal]
      );
      return { transactionId: lastID, balance: bal };
    });

    audit(user.id, "ADMIN_DEPOSIT", `amount=${money}`, getIP(req));
    return res.json({
      status: "success",
      message: "Admin deposit added successfully",
      transactionId: result.transactionId,
      balance: result.balance,
    });
  } catch (err) {
    console.error("income:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot process deposit" });
  }
});

// =============================================================
// UC_005 — THANH TOÁN & QUẢN LÝ HÓA ĐƠN
// =============================================================

app.get("/api/bills/services", async (_req, res) => {
  try {
    const rows = await dbAll("SELECT code, name FROM bill_services ORDER BY name", []);
    return res.json({ status: "success", data: rows });
  } catch (err) {
    console.error("bills/services:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get services" });
  }
});

app.get("/api/bills/providers", async (req, res) => {
  try {
    const serviceCode = String(req.query.serviceCode || "").trim().toUpperCase();
    const rows = serviceCode
      ? await dbAll(
          "SELECT code, name, service_code AS serviceCode FROM bill_providers WHERE service_code = ? ORDER BY name",
          [serviceCode]
        )
      : await dbAll(
          "SELECT code, name, service_code AS serviceCode FROM bill_providers ORDER BY service_code, name",
          []
        );
    return res.json({ status: "success", data: rows });
  } catch (err) {
    console.error("bills/providers:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get providers" });
  }
});

app.post("/api/bills/lookup", authenticateToken, async (req, res) => {
  try {
    const serviceCode = String(req.body.serviceCode || "").trim().toUpperCase();
    const providerCode = String(req.body.providerCode || "").trim().toUpperCase();
    const customerCode = String(req.body.customerCode || "").trim().toUpperCase();

    if (!serviceCode || !providerCode || !customerCode) {
      return res.status(400).json({
        status: "error",
        message: "serviceCode, providerCode, customerCode là bắt buộc",
      });
    }

    const bills = await dbAll(
      `SELECT b.id AS billId, s.name AS serviceName, p.name AS providerName,
              b.customer_code AS customerCode, b.customer_name AS customerName,
              b.bill_period AS billPeriod, b.amount, b.due_date AS dueDate, b.status
       FROM bills b
       JOIN bill_services  s ON s.code = b.service_code
       JOIN bill_providers p ON p.code = b.provider_code
       WHERE b.service_code = ? AND b.provider_code = ? AND UPPER(b.customer_code) = ?
       ORDER BY b.due_date DESC`,
      [serviceCode, providerCode, customerCode]
    );

    audit(
      req.user.id,
      "BILL_LOOKUP",
      `svc=${serviceCode} pvd=${providerCode} cust=${customerCode}`,
      getIP(req)
    );

    return res.json({ status: "success", data: bills });
  } catch (err) {
    console.error("bills/lookup:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot lookup bill" });
  }
});

app.post("/api/bills/pay", authenticateToken, requireActiveAccount, async (req, res) => {
  try {
    const billId = req.body.billId ? parseInt(req.body.billId, 10) : null;
    const otpCode = String(req.body.otpCode || "").trim();

    if (billId) {
      if (!otpCode) {
        return res.status(400).json({ status: "error", message: "OTP is required for bill payment" });
      }

      const bill = await dbGet(
        `SELECT b.*, s.name AS serviceName, p.name AS providerName
         FROM bills b
         JOIN bill_services  s ON s.code = b.service_code
         JOIN bill_providers p ON p.code = b.provider_code
         WHERE b.id = ?`,
        [billId]
      );
      if (!bill) {
        return res.status(404).json({ status: "error", message: "Hóa đơn không tồn tại" });
      }
      if (bill.status === "PAID") {
        return res.status(400).json({ status: "error", message: "Hóa đơn đã được thanh toán" });
      }

      const userRow = await dbGet("SELECT phone FROM users WHERE id = ?", [req.user.id]);
      const otpCheck = await verifyOTPWithAttemptGuard(
        userRow.phone,
        "BILL_PAYMENT",
        otpCode,
        String(billId),
        false
      );
      if (!otpCheck.ok) {
        return res.status(400).json({ status: "error", message: otpCheck.message });
      }
      const otpRow = otpCheck.row;

      const money = bill.amount;

      const result = await runTransaction(async () => {
        const consumeRes = await dbRun(
          "UPDATE otp_codes SET verified = 1, consumed = 1 WHERE id = ? AND consumed = 0",
          [otpRow.id]
        );
        if (consumeRes.changes === 0) {
          throw Object.assign(new Error("Mã OTP đã được sử dụng"), { statusCode: 400 });
        }

        const { changes: billChanges } = await dbRun(
          "UPDATE bills SET status = 'PAID', paid_at = datetime('now'), paid_by = ? WHERE id = ? AND status = 'UNPAID'",
          [req.user.id, billId]
        );
        if (billChanges === 0) {
          throw Object.assign(new Error("Hóa đơn đã được thanh toán hoặc không còn hợp lệ"), {
            statusCode: 400,
          });
        }

        const { changes: balChanges } = await dbRun(
          "UPDATE users SET balance = balance - ? WHERE id = ? AND balance >= ?",
          [money, req.user.id, money]
        );
        if (balChanges === 0) {
          throw Object.assign(new Error("Insufficient balance"), { statusCode: 400 });
        }

        const bal = (await dbGet("SELECT balance FROM users WHERE id = ?", [req.user.id])).balance;
        const txContent = `Thanh toán ${bill.serviceName} - ${bill.providerName} - Mã KH: ${bill.customer_code}`;
        const { lastID } = await dbRun(
          `INSERT INTO transactions
             (reference_code, user_id, type, amount, fee, status, content,
              counterparty_name, counterparty_account, balance_after)
           VALUES (?, ?, 'BILL_PAYMENT', ?, 0, 'SUCCESS', ?, ?, ?, ?)`,
          [generateRef("BILL"), req.user.id, money, txContent, bill.providerName, bill.customer_code, bal]
        );
        return { transactionId: lastID, balance: bal };
      });

      audit(req.user.id, "BILL_PAYMENT_SUCCESS", `billId=${billId} amount=${money}`, getIP(req));

      return res.json({
        status: "success",
        message: "Thanh toán hóa đơn thành công",
        transactionId: result.transactionId,
        balance: result.balance,
        receipt: {
          billId,
          serviceName: bill.serviceName,
          providerName: bill.providerName,
          customerCode: bill.customer_code,
          customerName: bill.customer_name,
          billPeriod: bill.bill_period,
          amount: money,
          paidAt: new Date().toISOString(),
        },
      });
    }

    return res.status(400).json({
      status: "error",
      message: "Vui lòng tra cứu hóa đơn (/api/bills/lookup) để lấy billId và xác thực bằng mã OTP.",
    });
  } catch (err) {
    if (err.statusCode === 400) {
      audit(req.user.id || null, "BILL_PAYMENT_FAILED", err.message, getIP(req));
      return res.status(400).json({ status: "error", message: err.message });
    }
    console.error("bills/pay:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot process bill payment" });
  }
});

app.get("/api/bills/history", authenticateToken, async (req, res) => {
  try {
    const rows = await dbAll(
      `SELECT t.id, t.reference_code AS referenceCode, t.amount, t.content,
              t.balance_after AS balanceAfter, t.created_at AS createdAt
       FROM transactions t
       WHERE t.user_id = ? AND t.type = 'BILL_PAYMENT'
       ORDER BY t.created_at DESC`,
      [req.user.id]
    );
    return res.json({ status: "success", data: rows });
  } catch (err) {
    console.error("bills/history:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get bill history" });
  }
});

// =============================================================
// ADMIN API
// =============================================================

app.get("/api/admin/users", requireAdmin, async (_req, res) => {
  try {
    const rows = await dbAll(
      `SELECT id, full_name AS fullName, phone, email, balance,
              username, accountNumber, accountType, accountFee,
              cccdNumber, job, status, ekycStatus, created_at AS createdAt
       FROM users ORDER BY id DESC`,
      []
    );
    const sanitized = rows.map((r) => ({ ...r, cccdNumber: maskSensitive(r.cccdNumber) }));
    return res.json({ status: "success", total: sanitized.length, data: sanitized });
  } catch (err) {
    console.error("admin/users:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get users" });
  }
});

app.get("/api/admin/audit-logs", requireAdmin, async (req, res) => {
  try {
    const limit = Math.min(500, parseInt(req.query.limit || "100", 10));
    const rows = await dbAll("SELECT * FROM audit_logs ORDER BY id DESC LIMIT ?", [limit]);
    return res.json({ status: "success", total: rows.length, data: rows });
  } catch (err) {
    console.error("admin/audit-logs:", err.message);
    return res.status(500).json({ status: "error", message: "Cannot get audit logs" });
  }
});

app.post("/api/admin/seed-demo-data", requireAdmin, async (_req, res) => {
  try {
    const demoPassword = process.env.DEMO_SEED_PASSWORD || crypto.randomBytes(8).toString("hex") + "Aa1!";
    const demoUsers = [
      {
        fullName: "Nguyen Van A",
        phone: "0901111111",
        password: demoPassword,
        accountNumber: "190312345678",
        cccdNumber: "001099012345",
        job: "Kỹ sư phần mềm",
      },
      {
        fullName: "Tran Thi B",
        phone: "0902222222",
        password: demoPassword,
        accountNumber: "190387654321",
        cccdNumber: "001099054321",
        job: "Kế toán",
      },
    ];

    const results = [];
    for (const u of demoUsers) {
      const exists = await dbGet("SELECT id FROM users WHERE phone = ?", [u.phone]);
      if (exists) {
        results.push({ phone: u.phone, status: "already exists" });
        continue;
      }

      const hash = await bcrypt.hash(u.password, 10);
      await dbRun(
        `INSERT INTO users
           (full_name, phone, password_hash, balance, username, accountNumber,
            accountType, cccdNumber, job, status, ekycStatus)
         VALUES (?, ?, ?, 5000000, ?, ?, 'Tài khoản thường', ?, ?, 'ACTIVE', 'VERIFIED')`,
        [u.fullName, u.phone, hash, u.phone, u.accountNumber, u.cccdNumber, u.job]
      );
      results.push({ phone: u.phone, accountNumber: u.accountNumber, status: "created" });
    }

    await seedDemoData();
    return res.json({ status: "success", message: "Seed demo data xong", data: results });
  } catch (err) {
    console.error("admin/seed:", err.message);
    return res.status(500).json({ status: "error", message: "Seed failed" });
  }
});

app.post("/api/admin/reset-demo-data", requireAdmin, async (_req, res) => {
  try {
    await runTransaction(async () => {
      await dbRun("DELETE FROM audit_logs");
      await dbRun("DELETE FROM sessions");
      await dbRun("DELETE FROM otp_codes");
      await dbRun("DELETE FROM ekyc_verifications");
      await dbRun("DELETE FROM transfer_requests");
      await dbRun("DELETE FROM transactions");
      await dbRun("DELETE FROM bills WHERE status = 'PAID'");
      await dbRun("DELETE FROM users");
    });
    await seedDemoData();
    return res.json({
      status: "success",
      message: "Reset xong. Gọi /api/admin/seed-demo-data để tạo user mẫu.",
    });
  } catch (err) {
    console.error("admin/reset:", err.message);
    return res.status(500).json({ status: "error", message: "Reset failed" });
  }
});

app.use((_req, res) => {
  res.status(404).json({ status: "error", message: "API endpoint not found" });
});

app.use((err, _req, res, _next) => {
  console.error("Unhandled error:", err.message);
  res.status(500).json({ status: "error", message: "Internal server error" });
});

const server = app.listen(config.PORT, "0.0.0.0", () => {
  console.log(`BankLite API (Hardened) is running at http://localhost:${config.PORT}`);
});

function shutdown() {
  console.log("\nShutting down server...");
  server.close(() => {
    closeDb((err) => {
      if (err) console.error("DB close error:", err.message);
      process.exit(0);
    });
  });
}
process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
