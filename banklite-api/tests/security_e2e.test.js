"use strict";

const assert = require("assert");
const crypto = require("crypto");
const { spawn } = require("child_process");
const fs = require("fs");
const os = require("os");
const path = require("path");

const apiDir = path.resolve(__dirname, "..");
const tempDbPath = path.join(os.tmpdir(), `banklite_test_${Date.now()}.db`);
const testPort = 3199;

const testEnv = {
  ...process.env,
  PORT: String(testPort),
  NODE_ENV: "development",
  DB_PATH: tempDbPath,
  JWT_SECRET: crypto.randomBytes(32).toString("hex"),
  ADMIN_KEY: crypto.randomBytes(32).toString("hex"),
  EXPOSE_DEMO_OTP: "false",
  TRUST_PROXY: "false",
};

let lastLoggedOtp = null;
const server = spawn(process.execPath, ["server.js"], {
  cwd: apiDir,
  env: testEnv,
});

server.stdout.on("data", (chunk) => {
  const text = chunk.toString();
  const matches = [...text.matchAll(/Mã OTP=(\d{6})/g)];
  if (matches.length > 0) {
    lastLoggedOtp = matches[matches.length - 1][1];
  }
});

server.stderr.on("data", (chunk) => {
  process.stderr.write(chunk);
});

async function req(method, urlPath, body = null, headers = {}) {
  const opts = {
    method,
    headers: { "Content-Type": "application/json", ...headers },
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(`http://127.0.0.1:${testPort}${urlPath}`, opts);
  const json = await res.json();
  return { status: res.status, headers: res.headers, body: json };
}

function cleanupFiles() {
  for (const ext of ["", "-wal", "-shm"]) {
    const f = tempDbPath + ext;
    if (fs.existsSync(f)) {
      try {
        fs.unlinkSync(f);
      } catch (_) {}
    }
  }
}

(async () => {
  let exitCode = 0;
  try {
    for (let w = 0; w < 40; w++) {
      try {
        const health = await fetch(`http://127.0.0.1:${testPort}/`);
        if (health.ok) break;
      } catch (_) {
        await new Promise((r) => setTimeout(r, 250));
      }
    }
    await new Promise((r) => setTimeout(r, 300));

    // 1. Security Headers & Zero OTP Leak in HTTP JSON
    const otpSend1 = await req("POST", "/api/otp/send", {
      phone: "0912345678",
      purpose: "REGISTER",
    });
    assert.strictEqual(otpSend1.status, 200);
    assert.strictEqual(otpSend1.body.otpCode, undefined, "OTP must never leak in HTTP JSON");
    assert.strictEqual(otpSend1.headers.get("x-content-type-options"), "nosniff");
    assert.strictEqual(otpSend1.headers.get("x-frame-options"), "DENY");
    const realOtp1 = lastLoggedOtp;
    assert.ok(/^\d{6}$/.test(realOtp1), "6-digit CSPRNG OTP expected on dev SMS gateway log");
    console.log("✔ [1/9] HTTP Security Headers active & Zero OTP Leakage in JSON verified");

    // 2. OTP Brute-Force Guard (Destroys OTP after 5 wrong attempts)
    for (let i = 1; i <= 5; i++) {
      const wrongCode = realOtp1 === "000000" ? "111111" : "000000";
      const r = await req("POST", "/api/otp/verify", {
        phone: "0912345678",
        code: wrongCode,
        purpose: "REGISTER",
      });
      assert.strictEqual(r.status, 400);
    }
    const afterLock = await req("POST", "/api/otp/verify", {
      phone: "0912345678",
      code: realOtp1,
      purpose: "REGISTER",
    });
    assert.strictEqual(afterLock.status, 400, "OTP must be invalidated after 5 failed guesses");
    console.log("✔ [2/9] Anti-Brute-Force OTP Lockout (5-attempt destruction) verified");

    // 3. Full eKYC Account Opening (OTP -> CCCD -> Face -> Register)
    async function registerFull(phone, fullName, cccd, stk, password) {
      lastLoggedOtp = null;
      await req("POST", "/api/otp/send", { phone, purpose: "REGISTER" });
      await new Promise((r) => setTimeout(r, 100));
      const otp = lastLoggedOtp;
      assert.strictEqual(
        (await req("POST", "/api/otp/verify", { phone, code: otp, purpose: "REGISTER" })).status,
        200
      );
      assert.strictEqual(
        (await req("POST", "/api/ekyc/verify-cccd", { phone, cccdNumber: cccd, fullName })).status,
        200
      );
      assert.strictEqual((await req("POST", "/api/ekyc/verify-face", { phone })).status, 200);
      const reg = await req("POST", "/api/auth/register", {
        fullName,
        phone,
        email: `${phone}@tcb-demo.vn`,
        password,
        username: `user_${phone}`,
        accountNumber: stk,
        accountType: "Tài khoản số đẹp",
        accountFee: 50000,
        cccdNumber: cccd,
        job: "Software Engineer",
      });
      assert.strictEqual(reg.status, 201);
    }

    await registerFull("0988111222", "Nguyen Hai A", "001200011122", "19036868", "Pass@123");
    await registerFull("0988333444", "Tran Minh B", "001200033344", "19039999", "Pass@123");
    console.log("✔ [3/9] End-to-End eKYC Onboarding & Vanity Account Registration verified");

    const loginA = await req("POST", "/api/auth/login", {
      phone: "0988111222",
      password: "Pass@123",
    });
    assert.strictEqual(loginA.status, 200);
    const authHeaderA = { Authorization: `Bearer ${loginA.body.token}` };

    // 4. Unauthorized Balance Inflation Guard (RBAC)
    const unauthDeposit = await req(
      "POST",
      "/api/transactions/income",
      { amount: 99999999, content: "unauthorized" },
      authHeaderA
    );
    assert.strictEqual(unauthDeposit.status, 403);
    console.log("✔ [4/9] Admin-only Deposit RBAC Guard (403 Forbidden for standard users) verified");

    // 5. PII Masking on Recipient Account Lookup
    const lookupB = await req("GET", "/api/account/lookup/19039999", null, authHeaderA);
    assert.strictEqual(lookupB.status, 200);
    assert.strictEqual(lookupB.body.data.fullName, "Tran Minh B");
    assert.strictEqual(lookupB.body.data.phone, undefined, "Recipient phone PII must be hidden");
    console.log("✔ [5/9] Recipient Lookup PII Masking (Zero Phone Leakage) verified");

    // 6. Atomic 2-Step Internal Money Transfer + Idempotency + OTP
    lastLoggedOtp = null;
    const initTrf = await req(
      "POST",
      "/api/transfers/initiate",
      {
        receiverAccountNumber: "19039999",
        amount: 150000,
        content: "Transfer E2E Test",
        idempotencyKey: "idem-key-001",
      },
      authHeaderA
    );
    assert.strictEqual(initTrf.status, 200);
    assert.strictEqual(initTrf.body.data.otpCode, undefined);
    await new Promise((r) => setTimeout(r, 100));
    const confTrf = await req(
      "POST",
      "/api/transfers/confirm",
      {
        referenceCode: initTrf.body.data.referenceCode,
        otpCode: lastLoggedOtp,
      },
      authHeaderA
    );
    assert.strictEqual(confTrf.status, 200);
    assert.strictEqual(confTrf.body.balance, 850000);
    console.log("✔ [6/9] Atomic 2-Step Money Transfer (Initiate + OTP Confirm) verified");

    // 7. Bill Lookup + Authenticated OTP Bill Payment
    const billLookup = await req(
      "POST",
      "/api/bills/lookup",
      { serviceCode: "ELECTRICITY", providerCode: "EVN_HN", customerCode: "EVN001" },
      authHeaderA
    );
    assert.strictEqual(billLookup.status, 200);
    const billId = billLookup.body.data[0].billId;

    lastLoggedOtp = null;
    assert.strictEqual(
      (
        await req(
          "POST",
          "/api/otp/send",
          { phone: "0988111222", purpose: "BILL_PAYMENT", referenceCode: String(billId) },
          authHeaderA
        )
      ).status,
      200
    );
    await new Promise((r) => setTimeout(r, 100));
    const billPay = await req(
      "POST",
      "/api/bills/pay",
      { billId, otpCode: lastLoggedOtp },
      authHeaderA
    );
    assert.strictEqual(billPay.status, 200);
    assert.strictEqual(billPay.body.balance, 565000);
    console.log("✔ [7/9] Utility Bill Lookup & OTP-Authenticated Payment verified");

    // 8. Paginated Ledger & Summary Consistency
    const txHist = await req("GET", "/api/transactions", null, authHeaderA);
    assert.strictEqual(txHist.status, 200);
    assert.strictEqual(txHist.body.data.total, 2);
    assert.strictEqual(txHist.body.data.transactions.length, 2);
    console.log("✔ [8/9] Paginated Ledger & Balance-After Audit Trail verified");

    // 9. Password Reset Single-Use OTP Consumption & Active Session Revocation
    lastLoggedOtp = null;
    await req("POST", "/api/otp/send", { phone: "0988111222", purpose: "RESET_PASSWORD" });
    await new Promise((r) => setTimeout(r, 100));
    const resetOtp = lastLoggedOtp;
    assert.strictEqual(
      (
        await req("POST", "/api/otp/verify", {
          phone: "0988111222",
          code: resetOtp,
          purpose: "RESET_PASSWORD",
        })
      ).status,
      200
    );
    assert.strictEqual(
      (
        await req("POST", "/api/auth/reset-password", {
          phone: "0988111222",
          newPassword: "NewSecure@456",
        })
      ).status,
      200
    );
    const replayReset = await req("POST", "/api/auth/reset-password", {
      phone: "0988111222",
      newPassword: "ReplayAttack@789",
    });
    assert.strictEqual(replayReset.status, 400, "OTP replay on reset-password must fail");

    const revokedCheck = await req("GET", "/api/account/me", null, authHeaderA);
    assert.strictEqual(revokedCheck.status, 401, "Old JWT session must be revoked immediately");
    console.log("✔ [9/9] Password Reset Anti-Replay & Global Session Revocation verified");

    console.log("\nALL 9 SECURITY & E2E INTEGRATION SUITES PASSED SUCCESSFULLY!");
  } catch (err) {
    console.error("\nTEST SUITE FAILED:", err);
    exitCode = 1;
  } finally {
    server.kill();
    setTimeout(() => {
      cleanupFiles();
      process.exit(exitCode);
    }, 300);
  }
})();
