# Kiến Trúc Bảo Mật & Toàn Vẹn Giao Dịch Tài Chính (Security & Ledger Architecture)

Tài liệu này mô tả chi tiết mô hình mối đe dọa (**Threat Model**), các cơ chế phòng thủ chiều sâu (**Defense-in-Depth**) và kiến trúc chống chi tiêu kép (**Anti-Double-Spending**) được triển khai trong hệ thống **TCB Digital Banking Simulation** (Android Client + BankLite API).

---

## 1. Các Bất Biến Tài Chính Cốt Lõi (Core Financial Invariants)

Trong hệ thống ngân hàng lõi (Core Banking), tính đúng đắn của sổ cái (**Ledger Integrity**) là bất biến quan trọng nhất. Backend [`banklite-api/src/db.js`](banklite-api/src/db.js) và [`banklite-api/server.js`](banklite-api/server.js) cưỡng chế 4 bất biến ở tầng cơ sở dữ liệu:

1. **Chống Race Condition / Double-Spending (Serialized Write Lock):**
   - Toàn bộ giao dịch ghi biến động số dư (`transfer`, `bill payment`, `admin deposit`) đi qua hàng đợi tuần tự `runTransaction()` kết hợp lệnh khóa ghi tức thời `BEGIN IMMEDIATE TRANSACTION` trên **SQLite WAL (Write-Ahead Logging)**.
   - Câu lệnh trừ tiền thực hiện kiểm tra số dư nguyên tử ngay tại thời điểm ghi:
     ```sql
     UPDATE users
     SET balance = balance - ?
     WHERE id = ? AND balance >= ?
     ```
   - Nếu `this.changes === 0`, giao dịch lập tức `ROLLBACK`, triệt tiêu hoàn toàn kịch bản gửi đồng thời nhiều request song song để rút quá số dư khả dụng (**TOCTOU — Time-of-Check to Time-of-Use**).
2. **Chống Gửi Trùng Giao Dịch (Idempotency Key):**
   - Mỗi yêu cầu chuyển khoản và thanh toán hóa đơn hỗ trợ khóa `idempotencyKey` gắn định danh duy nhất trên bảng `transactions` (`UNIQUE(user_id, idempotency_key)`), đảm bảo mạng chập chờn hoặc retry từ phía Mobile không bao giờ trừ tiền hai lần.
3. **Truy Vết Sổ Cái Bất Biến (`balance_after` Snapshot):**
   - Mọi bản ghi trong `transactions` lưu trữ ảnh chụp số dư ngay sau giao dịch (`balance_after`), mã tham chiếu (`reference_code`) và liên kết đối ứng hai chiều giữa người chuyển (`transfer_out`) và người nhận (`transfer_in`).
4. **Phân Quyền Tối Thiểu Cho Nghiệp Vụ Nạp Tiền (RBAC Guard):**
   - Endpoint `/api/transactions/income` bị khóa sau middleware `requireAdmin` (yêu cầu header `x-admin-key` khớp hằng thời gian với `ADMIN_KEY`), ngăn chặn tuyệt đối người dùng thông thường tự cộng tiền vào tài khoản.

---

## 2. Ma Trận Đối Chiếu OWASP API Security Top 10 (2023)

| Mã OWASP | Hạng mục rủi ro | Cơ chế phòng thủ thực tế trong `banklite-api` | File triển khai |
| :--- | :--- | :--- | :--- |
| **API1:2023** | Broken Object Level Authorization (BOLA) | Mọi truy vấn tài khoản, sổ cái, thông báo và giao dịch đều lấy `req.user.id` từ phiên JWT đã xác thực thay vì tin tưởng `userId` từ body/query. | [`banklite-api/server.js`](banklite-api/server.js) |
| **API2:2023** | Broken Authentication | Mật khẩu băm `bcryptjs` (salt rounds = 10); JWT ký bằng khóa 256-bit bắt buộc từ biến môi trường; đối chiếu phiên **Stateful Session** qua bảng `user_sessions` lưu `SHA-256(token)`. | [`banklite-api/src/config.js`](banklite-api/src/config.js), [`banklite-api/server.js`](banklite-api/server.js) |
| **API3:2023** | Broken Object Property Level Authorization | API tra cứu người nhận (`/api/account/lookup/:accountNumber`) chỉ trả về tên chủ tài khoản, số tài khoản, ngân hàng và số điện thoại đã che (`098****321`), không lộ số dư hay CCCD. | [`banklite-api/server.js`](banklite-api/server.js) |
| **API4:2023** | Unrestricted Resource Consumption | Giới hạn kích thước JSON body (`express.json({ limit: "64kb" })`); Rate Limiter có giới hạn bộ nhớ (`MAX_ENTRIES = 10,000`) kèm bộ dọn rác định kỳ chống tấn công cạn kiệt RAM. | [`banklite-api/src/security.js`](banklite-api/src/security.js) |
| **API5:2023** | Broken Function Level Authorization | Tách biệt rõ quyền người dùng (`authMiddleware`) và quyền quản trị nạp quỹ (`requireAdmin` dùng `crypto.timingSafeEqual`). | [`banklite-api/src/security.js`](banklite-api/src/security.js), [`banklite-api/server.js`](banklite-api/server.js) |
| **API6:2023** | Unrestricted Access to Sensitive Business Flows | Chuyển tiền, thanh toán hóa đơn và đặt lại mật khẩu bắt buộc luồng **2 bước xác thực OTP**: sinh mã bằng CSPRNG (`crypto.randomInt`), hủy mã ngay sau **5 lần nhập sai** hoặc sau khi tiêu thụ (`consumed = 1`), và thu hồi toàn bộ phiên đăng nhập cũ khi đổi mật khẩu. | [`banklite-api/src/security.js`](banklite-api/src/security.js), [`banklite-api/server.js`](banklite-api/server.js) |
| **API8:2023** | Security Misconfiguration | Fail-fast khi khởi động nếu thiếu `JWT_SECRET` hoặc `ADMIN_KEY` (< 32 ký tự); mặc định `EXPOSE_DEMO_OTP=false` (không bao giờ trả OTP trong HTTP JSON); gắn đầy đủ Security Headers (`X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Cache-Control: no-store`). | [`banklite-api/src/config.js`](banklite-api/src/config.js), [`banklite-api/src/security.js`](banklite-api/src/security.js) |

---

## 3. Ma Trận Đối Chiếu OWASP MASVS v2 (Android Client)

| Nhóm kiểm soát MASVS | Rủi ro trên thiết bị di động | Giải pháp kỹ thuật trên Android Client | File triển khai |
| :--- | :--- | :--- | :--- |
| **MASVS-STORAGE-1** | Lộ JWT Token hoặc PII (CCCD, SĐT) trong file XML `SharedPreferences` dạng rõ khi máy bị root hoặc trích xuất backup | Xây dựng lớp [`SecureStorageHelper`](app/src/main/java/com/example/tcbstudymobile/storage/SecureStorageHelper.java) sinh khóa `AES-256` phần cứng trong **AndroidKeyStore** (`AES/GCM/NoPadding`, IV 12 bytes ngẫu nhiên, Auth Tag 128-bit) để mã hóa toàn bộ token phiên và hồ sơ nháp eKYC. | [`SecureStorageHelper.java`](app/src/main/java/com/example/tcbstudymobile/storage/SecureStorageHelper.java), [`SessionManager.java`](app/src/main/java/com/example/tcbstudymobile/storage/SessionManager.java) |
| **MASVS-STORAGE-2** | Lưu mật khẩu nháp xuống bộ nhớ Flash trong quá trình đăng ký nhiều bước (eKYC) | Mật khẩu khởi tạo trong luồng mở tài khoản chỉ giữ trên biến `private static transient String draftPassword` (Volatile RAM) và xóa sạch ngay sau khi hoàn tất đăng ký. | [`OpenAccountManager.java`](app/src/main/java/com/example/tcbstudymobile/openaccount/OpenAccountManager.java) |
| **MASVS-NETWORK-1** | Nghe lén lưu lượng HTTP dạng rõ (Cleartext MITM) trên mạng công cộng | Cấu hình `cleartextTrafficPermitted="false"` toàn cục; chỉ cho phép ngoại lệ cục bộ phục vụ kiểm thử máy ảo Android (`10.0.2.2`, `127.0.0.1`, `localhost`). | [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml) |
| **MASVS-PLATFORM-1** | Khai thác Activity nội bộ từ ứng dụng bên thứ ba hoặc trích xuất dữ liệu qua `adb backup` | Đặt `android:allowBackup="false"` và khóa `android:exported="false"` cho toàn bộ các màn hình nghiệp vụ nhạy cảm (OTP, Đổi mật khẩu, Chuyển khoản, Hóa đơn); chỉ `MainActivity` (`LAUNCHER`) được export. | [`AndroidManifest.xml`](app/src/main/AndroidManifest.xml) |
| **MASVS-AUTH-2** | Đăng xuất giả phía Client (Token vẫn còn hiệu lực trên Server) | Luồng đăng xuất gọi `POST /api/auth/logout` để xóa phiên trên bảng `user_sessions` phía Backend đồng thời xóa khóa phiên mã hóa trên thiết bị. | [`HomeActivity.java`](app/src/main/java/com/example/tcbstudymobile/home/HomeActivity.java) |

---

## 4. Kiểm Chứng Bảo Mật Tự Động (Automated Security Regression Suite)

Toàn bộ các cơ chế phòng thủ trên được kiểm chứng tự động qua bộ test tích hợp độc lập [`banklite-api/tests/security_e2e.test.js`](banklite-api/tests/security_e2e.test.js) (khởi chạy tiến trình API thật trên cổng cô lập với cơ sở dữ liệu tạm thời và tự dọn sạch sau khi chạy):

```bash
cd banklite-api
npm test
```

9 kịch bản kiểm định bắt buộc trong CI:
1. Kiểm tra HTTP Security Headers & đảm bảo **Zero OTP Leakage** trong phản hồi JSON.
2. Kiểm tra khóa chống vét cạn OTP (**5-Attempt Lockout Destruction**).
3. Kiểm thử luồng mở tài khoản số đẹp eKYC toàn trình (Phone -> OTP -> Identity -> Registration).
4. Kiểm tra chặn leo thang đặc quyền (`403 Forbidden`) trên API nạp tiền quản trị.
5. Kiểm tra ẩn danh thông tin cá nhân (**PII Phone Masking**) khi tra cứu số tài khoản người nhận.
6. Kiểm thử chuyển khoản nội bộ 2 bước xác thực OTP & đối chiếu biến động số dư nguyên tử.
7. Kiểm thử tra cứu công nợ hóa đơn & thanh toán xác thực OTP.
8. Kiểm thử phân trang sổ cái & tính nhất quán của trường `balanceAfter`.
9. Kiểm thử chống phát lại mã OTP (**Anti-Replay**) và thu hồi toàn bộ phiên đăng nhập cũ khi đặt lại mật khẩu.
