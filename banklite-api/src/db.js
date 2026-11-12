"use strict";

const path = require("path");
const sqlite3 = require("sqlite3").verbose();

const DB_PATH = process.env.DB_PATH
  ? path.resolve(process.env.DB_PATH)
  : path.join(__dirname, "..", "banklite.db");
console.log("SQLite DB file:", DB_PATH);

const db = new sqlite3.Database(DB_PATH, (err) => {
  if (err) {
    console.error("Database connection error:", err.message);
    process.exit(1);
  }
  console.log("Connected to SQLite database.");
});

function addColIfMissing(sql) {
  db.run(sql, (err) => {
    if (err && !err.message.includes("duplicate column name")) {
      console.error("Migration:", err.message);
    }
  });
}

db.serialize(() => {
  db.run("PRAGMA journal_mode = WAL", (err) => {
    if (err) console.error("PRAGMA WAL:", err.message);
  });
  db.run("PRAGMA foreign_keys = ON", (err) => {
    if (err) console.error("PRAGMA FK:", err.message);
  });

  // ── users ──
  db.run(
    `CREATE TABLE IF NOT EXISTS users (
      id               INTEGER PRIMARY KEY AUTOINCREMENT,
      full_name        TEXT    NOT NULL,
      phone            TEXT    NOT NULL UNIQUE,
      email            TEXT    DEFAULT '',
      password_hash    TEXT    NOT NULL,
      balance          REAL    DEFAULT 0,
      username         TEXT,
      accountNumber    TEXT,
      accountType      TEXT    DEFAULT 'Tài khoản thường',
      accountFee       REAL    DEFAULT 0,
      cccdNumber       TEXT    DEFAULT '',
      job              TEXT    DEFAULT '',
      status           TEXT    DEFAULT 'ACTIVE',
      ekycStatus       TEXT    DEFAULT 'PENDING',
      loginFailCount   INTEGER DEFAULT 0,
      lockedUntil      TEXT,
      created_at       TEXT    DEFAULT CURRENT_TIMESTAMP
    )`,
    (err) => {
      if (err) console.error("CREATE users:", err.message);
    }
  );

  addColIfMissing("ALTER TABLE users ADD COLUMN username       TEXT");
  addColIfMissing("ALTER TABLE users ADD COLUMN accountNumber  TEXT");
  addColIfMissing("ALTER TABLE users ADD COLUMN accountType    TEXT");
  addColIfMissing("ALTER TABLE users ADD COLUMN accountFee     REAL DEFAULT 0");
  addColIfMissing("ALTER TABLE users ADD COLUMN cccdNumber     TEXT DEFAULT ''");
  addColIfMissing("ALTER TABLE users ADD COLUMN job            TEXT DEFAULT ''");
  addColIfMissing("ALTER TABLE users ADD COLUMN status         TEXT DEFAULT 'ACTIVE'");
  addColIfMissing("ALTER TABLE users ADD COLUMN ekycStatus     TEXT DEFAULT 'PENDING'");
  addColIfMissing("ALTER TABLE users ADD COLUMN loginFailCount INTEGER DEFAULT 0");
  addColIfMissing("ALTER TABLE users ADD COLUMN lockedUntil    TEXT");

  db.run(
    `CREATE UNIQUE INDEX IF NOT EXISTS idx_users_accountNumber
     ON users (accountNumber) WHERE accountNumber IS NOT NULL AND accountNumber <> ''`,
    (err) => {
      if (err) console.error("idx accountNumber:", err.message);
    }
  );

  db.run(
    `CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username
     ON users (username) WHERE username IS NOT NULL AND username <> ''`,
    (err) => {
      if (err) console.error("idx username:", err.message);
    }
  );

  // ── transactions ──
  db.run(
    `CREATE TABLE IF NOT EXISTS transactions (
      id                   INTEGER PRIMARY KEY AUTOINCREMENT,
      reference_code       TEXT    NOT NULL UNIQUE,
      user_id              INTEGER NOT NULL,
      type                 TEXT    NOT NULL,
      amount               REAL    NOT NULL,
      fee                  REAL    DEFAULT 0,
      status               TEXT    DEFAULT 'SUCCESS',
      content              TEXT    DEFAULT '',
      counterparty_name    TEXT    DEFAULT '',
      counterparty_account TEXT    DEFAULT '',
      balance_after        REAL,
      idempotency_key      TEXT,
      created_at           TEXT    DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id)
    )`,
    (err) => {
      if (err) console.error("CREATE transactions:", err.message);
    }
  );

  addColIfMissing("ALTER TABLE transactions ADD COLUMN reference_code       TEXT");
  addColIfMissing("ALTER TABLE transactions ADD COLUMN fee                  REAL DEFAULT 0");
  addColIfMissing("ALTER TABLE transactions ADD COLUMN counterparty_name    TEXT DEFAULT ''");
  addColIfMissing("ALTER TABLE transactions ADD COLUMN counterparty_account TEXT DEFAULT ''");
  addColIfMissing("ALTER TABLE transactions ADD COLUMN balance_after        REAL");
  addColIfMissing("ALTER TABLE transactions ADD COLUMN idempotency_key      TEXT");

  db.run(
    `CREATE UNIQUE INDEX IF NOT EXISTS idx_tx_reference ON transactions (reference_code)
     WHERE reference_code IS NOT NULL`,
    (err) => {
      if (err) console.error("idx reference_code:", err.message);
    }
  );

  // ── otp_codes (Nâng cấp: thêm attempts & consumed chống brute-force và replay) ──
  db.run(
    `CREATE TABLE IF NOT EXISTS otp_codes (
      id             INTEGER PRIMARY KEY AUTOINCREMENT,
      phone          TEXT    NOT NULL,
      code           TEXT    NOT NULL,
      purpose        TEXT    NOT NULL,
      reference_code TEXT    DEFAULT '',
      verified       INTEGER DEFAULT 0,
      consumed       INTEGER DEFAULT 0,
      attempts       INTEGER DEFAULT 0,
      expires_at     TEXT    NOT NULL,
      created_at     TEXT    DEFAULT CURRENT_TIMESTAMP
    )`,
    (err) => {
      if (err) console.error("CREATE otp_codes:", err.message);
    }
  );

  addColIfMissing("ALTER TABLE otp_codes ADD COLUMN reference_code TEXT DEFAULT ''");
  addColIfMissing("ALTER TABLE otp_codes ADD COLUMN consumed       INTEGER DEFAULT 0");
  addColIfMissing("ALTER TABLE otp_codes ADD COLUMN attempts       INTEGER DEFAULT 0");

  // ── sessions ──
  db.run(
    `CREATE TABLE IF NOT EXISTS sessions (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id    INTEGER NOT NULL,
      token_hash TEXT    NOT NULL,
      expires_at TEXT    NOT NULL,
      created_at TEXT    DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (user_id) REFERENCES users(id)
    )`,
    (err) => {
      if (err) console.error("CREATE sessions:", err.message);
    }
  );

  // ── ekyc_verifications ──
  db.run(
    `CREATE TABLE IF NOT EXISTS ekyc_verifications (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      phone         TEXT    NOT NULL,
      cccd_number   TEXT    NOT NULL,
      full_name     TEXT    DEFAULT '',
      cccd_verified INTEGER DEFAULT 0,
      face_verified INTEGER DEFAULT 0,
      face_score    REAL    DEFAULT 0,
      ekyc_status   TEXT    DEFAULT 'PENDING',
      expires_at    TEXT    NOT NULL,
      created_at    TEXT    DEFAULT CURRENT_TIMESTAMP
    )`,
    (err) => {
      if (err) console.error("CREATE ekyc_verifications:", err.message);
    }
  );

  // ── transfer_requests ──
  db.run(
    `CREATE TABLE IF NOT EXISTS transfer_requests (
      id               INTEGER PRIMARY KEY AUTOINCREMENT,
      reference_code   TEXT    NOT NULL UNIQUE,
      sender_id        INTEGER NOT NULL,
      receiver_id      INTEGER NOT NULL,
      amount           REAL    NOT NULL,
      fee              REAL    DEFAULT 0,
      content          TEXT    DEFAULT '',
      status           TEXT    DEFAULT 'PENDING',
      otp_verified     INTEGER DEFAULT 0,
      idempotency_key  TEXT,
      expires_at       TEXT    NOT NULL,
      created_at       TEXT    DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (sender_id)   REFERENCES users(id),
      FOREIGN KEY (receiver_id) REFERENCES users(id)
    )`,
    (err) => {
      if (err) console.error("CREATE transfer_requests:", err.message);
    }
  );

  // ── bill_services, bill_providers, bills ──
  db.run(
    `CREATE TABLE IF NOT EXISTS bill_services (
      code TEXT PRIMARY KEY,
      name TEXT NOT NULL
    )`,
    (err) => {
      if (err) console.error("CREATE bill_services:", err.message);
    }
  );

  db.run(
    `CREATE TABLE IF NOT EXISTS bill_providers (
      code         TEXT PRIMARY KEY,
      name         TEXT NOT NULL,
      service_code TEXT NOT NULL,
      FOREIGN KEY (service_code) REFERENCES bill_services(code)
    )`,
    (err) => {
      if (err) console.error("CREATE bill_providers:", err.message);
    }
  );

  db.run(
    `CREATE TABLE IF NOT EXISTS bills (
      id            INTEGER PRIMARY KEY AUTOINCREMENT,
      service_code  TEXT    NOT NULL,
      provider_code TEXT    NOT NULL,
      customer_code TEXT    NOT NULL,
      customer_name TEXT    DEFAULT '',
      bill_period   TEXT    DEFAULT '',
      amount        REAL    NOT NULL,
      due_date      TEXT,
      status        TEXT    DEFAULT 'UNPAID',
      paid_at       TEXT,
      paid_by       INTEGER,
      created_at    TEXT    DEFAULT CURRENT_TIMESTAMP,
      FOREIGN KEY (service_code)  REFERENCES bill_services(code),
      FOREIGN KEY (provider_code) REFERENCES bill_providers(code)
    )`,
    (err) => {
      if (err) console.error("CREATE bills:", err.message);
    }
  );

  // ── audit_logs ──
  db.run(
    `CREATE TABLE IF NOT EXISTS audit_logs (
      id         INTEGER PRIMARY KEY AUTOINCREMENT,
      user_id    INTEGER,
      action     TEXT    NOT NULL,
      detail     TEXT    DEFAULT '',
      ip         TEXT    DEFAULT '',
      created_at TEXT    DEFAULT CURRENT_TIMESTAMP
    )`,
    (err) => {
      if (err) console.error("CREATE audit_logs:", err.message);
    }
  );
});

function dbRun(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.run(sql, params, function (err) {
      if (err) return reject(err);
      resolve({ lastID: this.lastID, changes: this.changes });
    });
  });
}

function dbGet(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.get(sql, params, (err, row) => {
      if (err) return reject(err);
      resolve(row);
    });
  });
}

function dbAll(sql, params = []) {
  return new Promise((resolve, reject) => {
    db.all(sql, params, (err, rows) => {
      if (err) return reject(err);
      resolve(rows);
    });
  });
}

let txQueue = Promise.resolve();

function runTransaction(asyncFn) {
  const task = txQueue.then(async () => {
    await dbRun("BEGIN IMMEDIATE TRANSACTION");
    try {
      const result = await asyncFn();
      await dbRun("COMMIT");
      return result;
    } catch (err) {
      try {
        await dbRun("ROLLBACK");
      } catch (_) {}
      throw err;
    }
  });
  txQueue = task.catch(() => {});
  return task;
}

function audit(userId, action, detail = "", ip = "") {
  dbRun(
    "INSERT INTO audit_logs (user_id, action, detail, ip) VALUES (?, ?, ?, ?)",
    [userId || null, action, String(detail).slice(0, 500), ip]
  ).catch((e) => console.error("Audit log error:", e.message));
}

async function seedDemoData() {
  try {
    const services = [
      ["ELECTRICITY", "Điện"],
      ["WATER", "Nước"],
      ["INTERNET", "Internet"],
      ["PHONE", "Điện thoại"],
      ["TV", "Truyền hình"],
    ];
    for (const [code, name] of services) {
      await dbRun("INSERT OR IGNORE INTO bill_services (code, name) VALUES (?, ?)", [code, name]);
    }

    const providers = [
      ["EVN_HN", "EVN Hà Nội", "ELECTRICITY"],
      ["EVN_HCM", "EVN TP.HCM", "ELECTRICITY"],
      ["HANOIWATER", "Nước Hà Nội", "WATER"],
      ["HCMWATER", "Nước TP.HCM", "WATER"],
      ["VNPT", "VNPT", "INTERNET"],
      ["VIETTEL", "Viettel", "INTERNET"],
      ["FPT", "FPT Telecom", "INTERNET"],
      ["VINAPHONE", "VinaPhone", "PHONE"],
      ["MOBIFONE", "MobiFone", "PHONE"],
      ["VIETTEL_TV", "Viettel TV", "TV"],
      ["SCTV", "SCTV", "TV"],
    ];
    for (const [code, name, svc] of providers) {
      await dbRun(
        "INSERT OR IGNORE INTO bill_providers (code, name, service_code) VALUES (?, ?, ?)",
        [code, name, svc]
      );
    }

    const billCount = await dbGet("SELECT COUNT(*) AS c FROM bills");
    if (billCount.c === 0) {
      const sampleBills = [
        ["ELECTRICITY", "EVN_HN", "EVN001", "Nguyen Van A", "2026-05", 285000, "2026-06-15"],
        ["ELECTRICITY", "EVN_HN", "EVN002", "Tran Thi B", "2026-05", 320000, "2026-06-15"],
        ["ELECTRICITY", "EVN_HCM", "EVNHCM01", "Le Van C", "2026-05", 410000, "2026-06-15"],
        ["WATER", "HANOIWATER", "HNW001", "Nguyen Van A", "2026-05", 98000, "2026-06-20"],
        ["WATER", "HCMWATER", "HCMW01", "Pham Thi D", "2026-05", 115000, "2026-06-20"],
        ["INTERNET", "FPT", "FPT001", "Nguyen Van A", "2026-05", 265000, "2026-06-25"],
        ["INTERNET", "VIETTEL", "VTL001", "Tran Thi B", "2026-05", 199000, "2026-06-25"],
        ["INTERNET", "VNPT", "VNPT001", "Hoang Van E", "2026-05", 220000, "2026-06-25"],
        ["PHONE", "MOBIFONE", "MB001", "Demo User", "2026-05", 50000, "2026-06-30"],
        ["PHONE", "VINAPHONE", "VNP001", "Demo User", "2026-05", 75000, "2026-06-30"],
        ["TV", "SCTV", "SCTV001", "Demo User", "2026-05", 155000, "2026-06-30"],
        ["TV", "VIETTEL_TV", "VTV001", "Demo User", "2026-05", 120000, "2026-06-30"],
      ];
      for (const [sc, pc, cc, cn, bp, amt, dd] of sampleBills) {
        await dbRun(
          `INSERT INTO bills (service_code, provider_code, customer_code, customer_name, bill_period, amount, due_date)
           VALUES (?, ?, ?, ?, ?, ?, ?)`,
          [sc, pc, cc, cn, bp, Number(amt), dd]
        );
      }
      console.log("Demo bills seeded.");
    }
  } catch (err) {
    console.error("Seed error:", err.message);
  }
}

function closeDb(cb) {
  db.close(cb);
}

module.exports = {
  db,
  dbRun,
  dbGet,
  dbAll,
  runTransaction,
  audit,
  seedDemoData,
  closeDb,
};
