"use strict";

require("dotenv").config();

const PORT = Number(process.env.PORT) || 3000;
const NODE_ENV = process.env.NODE_ENV || "development";
const JWT_SECRET = (process.env.JWT_SECRET || "").trim();
const ADMIN_KEY = (process.env.ADMIN_KEY || "").trim();

// Cấm tuyệt đối fallback về khóa bí mật mặc định (Zero Hardcoded Secrets)
if (!JWT_SECRET || JWT_SECRET.length < 16 || JWT_SECRET === "banklite_secret_key") {
  console.error(
    "[FATAL SECURITY] JWT_SECRET chưa được cấu hình hoặc quá yếu trong file .env (tối thiểu 16 ký tự)."
  );
  process.exit(1);
}

if (!ADMIN_KEY || ADMIN_KEY.length < 16 || ADMIN_KEY === "tcbstudy_admin_demo") {
  console.error(
    "[FATAL SECURITY] ADMIN_KEY chưa được cấu hình hoặc quá yếu trong file .env (tối thiểu 16 ký tự)."
  );
  process.exit(1);
}

module.exports = {
  PORT,
  NODE_ENV,
  JWT_SECRET,
  ADMIN_KEY,
  EXPOSE_DEMO_OTP: process.env.EXPOSE_DEMO_OTP === "true",
  TRUST_PROXY: process.env.TRUST_PROXY === "true",
  ALLOW_LEGACY_BILL_PAYMENT: process.env.ALLOW_LEGACY_BILL_PAYMENT === "true",

  // Hạn mức & chính sách bảo mật nghiệp vụ
  TRANSFER_SINGLE_LIMIT: 50_000_000,
  TRANSFER_DAILY_LIMIT: 100_000_000,
  LOGIN_MAX_FAIL: 5,
  LOGIN_LOCK_MINUTES: 5,
  OTP_EXPIRE_MINUTES: 5,
  OTP_MAX_ATTEMPTS: 5,
};
