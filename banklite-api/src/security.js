"use strict";

const crypto = require("crypto");
const jwt = require("jsonwebtoken");
const config = require("./config");
const { dbRun, dbGet } = require("./db");

// ================= SECURITY HEADERS =================

function securityHeadersMiddleware(_req, res, next) {
  res.setHeader("X-Content-Type-Options", "nosniff");
  res.setHeader("X-Frame-Options", "DENY");
  res.setHeader("X-XSS-Protection", "0");
  res.setHeader("Referrer-Policy", "no-referrer");
  res.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
  next();
}

// ================= BOUNDED IN-MEMORY RATE LIMIT =================

const rateLimitMap = new Map();
const MAX_RATE_LIMIT_KEYS = 10_000;

// Tự động dọn dẹp các mục hết hạn mỗi 2 phút, không giữ event loop khi tắt server
setInterval(() => {
  const now = Date.now();
  for (const [key, entry] of rateLimitMap.entries()) {
    if (now > entry.resetAt) {
      rateLimitMap.delete(key);
    }
  }
}, 2 * 60 * 1000).unref();

function checkRateLimit(key, maxRequests, windowMs) {
  const now = Date.now();
  const entry = rateLimitMap.get(key);
  if (!entry || now > entry.resetAt) {
    if (rateLimitMap.size >= MAX_RATE_LIMIT_KEYS) {
      const oldestKey = rateLimitMap.keys().next().value;
      if (oldestKey) rateLimitMap.delete(oldestKey);
    }
    rateLimitMap.set(key, { count: 1, resetAt: now + windowMs });
    return true;
  }
  if (entry.count >= maxRequests) return false;
  entry.count++;
  return true;
}

// ================= CRYPTO & TIMING-SAFE HELPERS =================

function timingSafeEqualStr(input, expected) {
  const hashA = crypto.createHash("sha256").update(String(input || "")).digest();
  const hashB = crypto.createHash("sha256").update(String(expected || "")).digest();
  return crypto.timingSafeEqual(hashA, hashB);
}

function generateRef(prefix = "TXN") {
  const date = new Date().toISOString().slice(0, 10).replace(/-/g, "");
  return `${prefix}${date}-${crypto.randomBytes(3).toString("hex").toUpperCase()}`;
}

function generateOTP() {
  return String(crypto.randomInt(100000, 1000000));
}

async function generateUniqueAccountNumber(maxRetries = 10) {
  for (let i = 0; i < maxRetries; i++) {
    const num = "1903" + String(crypto.randomInt(10000000, 100000000));
    const exists = await dbGet("SELECT id FROM users WHERE accountNumber = ?", [num]);
    if (!exists) return num;
  }
  throw new Error("Cannot generate unique account number");
}

function getIP(req) {
  if (config.TRUST_PROXY && req.headers["x-forwarded-for"]) {
    return String(req.headers["x-forwarded-for"]).split(",")[0].trim();
  }
  return (req.socket?.remoteAddress || req.ip || "").toString();
}

// ================= OTP LIFECYCLE (ANTI-BRUTEFORCE & SINGLE-USE) =================

async function issueOTP(phone, purpose, referenceCode = "") {
  const code = generateOTP();
  const expiresAt = new Date(Date.now() + config.OTP_EXPIRE_MINUTES * 60 * 1000).toISOString();

  // Hủy toàn bộ OTP cũ cùng phone + purpose + referenceCode chưa tiêu thụ
  await dbRun(
    "UPDATE otp_codes SET consumed = 1 WHERE phone = ? AND purpose = ? AND reference_code = ? AND consumed = 0",
    [phone, purpose, referenceCode]
  );

  await dbRun(
    `INSERT INTO otp_codes (phone, code, purpose, reference_code, verified, consumed, attempts, expires_at)
     VALUES (?, ?, ?, ?, 0, 0, 0, ?)`,
    [phone, code, purpose, referenceCode, expiresAt]
  );

  if (config.NODE_ENV !== "production") {
    console.log(
      `[SMS OTP GATEWAY] SĐT=${phone} | Mục đích=${purpose} | Ref=${referenceCode || "-"} | Mã OTP=${code}`
    );
  }
  return { code, expiresAt };
}

/**
 * Kiểm tra mã OTP an toàn:
 * - So sánh constant-time (timingSafeEqualStr).
 * - Đếm số lần nhập sai (attempts). Nếu sai >= OTP_MAX_ATTEMPTS (5 lần) -> tự động hủy mã (consumed = 1).
 * - Nếu markVerified = true -> đánh dấu verified = 1 (cho bước REGISTER / RESET_PASSWORD).
 */
async function verifyOTPWithAttemptGuard(phone, purpose, inputCode, referenceCode = "", markVerified = false) {
  const row = await dbGet(
    `SELECT id, code, attempts, verified
     FROM otp_codes
     WHERE phone = ? AND purpose = ? AND reference_code = ?
       AND consumed = 0 AND julianday(expires_at) > julianday('now')
     ORDER BY id DESC LIMIT 1`,
    [phone, purpose, referenceCode]
  );

  if (!row) {
    return {
      ok: false,
      message: "OTP không tồn tại, đã hết hạn hoặc đã được sử dụng",
    };
  }

  if ((row.attempts || 0) >= config.OTP_MAX_ATTEMPTS) {
    await dbRun("UPDATE otp_codes SET consumed = 1 WHERE id = ?", [row.id]);
    return {
      ok: false,
      message: "Mã OTP đã bị hủy do nhập sai quá số lần quy định. Vui lòng gửi lại OTP mới.",
    };
  }

  if (!timingSafeEqualStr(inputCode, row.code)) {
    const nextAttempts = (row.attempts || 0) + 1;
    if (nextAttempts >= config.OTP_MAX_ATTEMPTS) {
      await dbRun("UPDATE otp_codes SET attempts = ?, consumed = 1 WHERE id = ?", [nextAttempts, row.id]);
      return {
        ok: false,
        message: `Sai OTP quá ${config.OTP_MAX_ATTEMPTS} lần. Mã OTP đã bị hủy, vui lòng gửi lại mã mới.`,
      };
    }
    await dbRun("UPDATE otp_codes SET attempts = ? WHERE id = ?", [nextAttempts, row.id]);
    return {
      ok: false,
      message: `Mã OTP không đúng. Còn ${config.OTP_MAX_ATTEMPTS - nextAttempts} lần thử.`,
    };
  }

  if (markVerified) {
    await dbRun("UPDATE otp_codes SET verified = 1 WHERE id = ?", [row.id]);
  }

  return { ok: true, row };
}

// ================= AUTH MIDDLEWARES =================

async function authenticateToken(req, res, next) {
  const authHeader = req.headers["authorization"];
  if (!authHeader) {
    return res.status(401).json({ status: "error", message: "Authorization header is required" });
  }

  const token = (authHeader.split(" ")[1] || "").trim();
  if (!token) {
    return res.status(401).json({ status: "error", message: "Token is required" });
  }

  let decoded;
  try {
    decoded = jwt.verify(token, config.JWT_SECRET);
  } catch (_) {
    return res.status(403).json({ status: "error", message: "Invalid or expired token" });
  }

  const tokenHash = crypto.createHash("sha256").update(token).digest("hex");
  try {
    const session = await dbGet(
      `SELECT id FROM sessions
       WHERE user_id = ? AND token_hash = ? AND julianday(expires_at) > julianday('now')`,
      [decoded.id, tokenHash]
    );
    if (!session) {
      return res.status(401).json({ status: "error", message: "Session expired or logged out" });
    }
  } catch (dbErr) {
    console.error("Session check error:", dbErr.message);
    return res.status(500).json({ status: "error", message: "Cannot verify session" });
  }

  req.user = decoded;
  req.tokenHash = tokenHash;
  next();
}

async function getAuthenticatedUserFromRequest(req) {
  const authHeader = req.headers["authorization"];
  if (!authHeader) {
    throw Object.assign(new Error("Authorization header is required"), { statusCode: 401 });
  }

  const token = (authHeader.split(" ")[1] || "").trim();
  if (!token) {
    throw Object.assign(new Error("Token is required"), { statusCode: 401 });
  }

  let decoded;
  try {
    decoded = jwt.verify(token, config.JWT_SECRET);
  } catch (_) {
    throw Object.assign(new Error("Invalid or expired token"), { statusCode: 403 });
  }

  const tokenHash = crypto.createHash("sha256").update(token).digest("hex");
  const session = await dbGet(
    `SELECT id FROM sessions
     WHERE user_id = ? AND token_hash = ? AND julianday(expires_at) > julianday('now')`,
    [decoded.id, tokenHash]
  );
  if (!session) {
    throw Object.assign(new Error("Session expired or logged out"), { statusCode: 401 });
  }

  const user = await dbGet(
    "SELECT id, phone, status, lockedUntil FROM users WHERE id = ?",
    [decoded.id]
  );
  if (!user) throw Object.assign(new Error("User not found"), { statusCode: 404 });
  if (user.status !== "ACTIVE") {
    throw Object.assign(new Error("Account is not active"), { statusCode: 403 });
  }
  if (user.lockedUntil && new Date(user.lockedUntil) > new Date()) {
    throw Object.assign(
      new Error("Account temporarily locked due to too many failed login attempts"),
      { statusCode: 403 }
    );
  }

  return user;
}

async function requireActiveAccount(req, res, next) {
  try {
    const user = await dbGet("SELECT status, lockedUntil FROM users WHERE id = ?", [req.user.id]);
    if (!user || user.status !== "ACTIVE") {
      return res.status(403).json({ status: "error", message: "Account is not active" });
    }
    if (user.lockedUntil && new Date(user.lockedUntil) > new Date()) {
      return res.status(403).json({
        status: "error",
        message: "Account temporarily locked due to too many failed login attempts",
      });
    }
    next();
  } catch (_err) {
    return res.status(500).json({ status: "error", message: "Cannot verify account status" });
  }
}

function requireAdmin(req, res, next) {
  const providedKey = req.headers["x-admin-key"];
  if (!providedKey || !timingSafeEqualStr(providedKey, config.ADMIN_KEY)) {
    return res.status(403).json({ status: "error", message: "Forbidden: Admin privileges required" });
  }
  next();
}

module.exports = {
  securityHeadersMiddleware,
  checkRateLimit,
  timingSafeEqualStr,
  generateRef,
  generateOTP,
  generateUniqueAccountNumber,
  getIP,
  issueOTP,
  verifyOTPWithAttemptGuard,
  authenticateToken,
  getAuthenticatedUserFromRequest,
  requireActiveAccount,
  requireAdmin,
};
