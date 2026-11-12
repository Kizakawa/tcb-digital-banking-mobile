package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.home.HomeActivity;
import com.example.tcbstudymobile.model.LoginRequest;
import com.example.tcbstudymobile.model.LoginResponse;
import com.example.tcbstudymobile.model.RegisterRequest;
import com.example.tcbstudymobile.model.RegisterResponse;
import com.example.tcbstudymobile.model.User;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.storage.SessionManager;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AgreementActivity extends AppCompatActivity {

    private TextView txtBackAgreement, txtAgreementContent, txtAgreementMessage;
    private Button btnAgreeOpenAccount;

    private OpenAccountManager openAccountManager;
    private SessionManager sessionManager;
    private ApiService apiService;

    private String registerPhone;
    private String registerPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agreement);

        txtBackAgreement = findViewById(R.id.txtBackAgreement);
        txtAgreementContent = findViewById(R.id.txtAgreementContent);
        txtAgreementMessage = findViewById(R.id.txtAgreementMessage);
        btnAgreeOpenAccount = findViewById(R.id.btnAgreeOpenAccount);

        openAccountManager = new OpenAccountManager(this);
        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        txtBackAgreement.setOnClickListener(v -> finish());

        showSummary();

        btnAgreeOpenAccount.setOnClickListener(v -> createAccount());
    }

    private void showSummary() {
        txtAgreementContent.setText(
                "Họ tên: " + safeText(openAccountManager.getFullName()) +
                        "\nSố điện thoại: " + safeText(openAccountManager.getPhone()) +
                        "\nEmail: " + getDisplayEmail() +
                        "\nSố CCCD: " + safeText(openAccountManager.getCccdNumber()) +
                        "\nNghề nghiệp: " + safeText(openAccountManager.getJob()) +
                        "\nLoại tài khoản: " + safeText(openAccountManager.getAccountType()) +
                        "\nSố tài khoản: " + safeText(openAccountManager.getAccountNumber()) +
                        "\nPhí số tài khoản: " + formatMoney(openAccountManager.getAccountFee()) +
                        "\nTên đăng nhập: " + maskUsername(openAccountManager.getUsername()) +
                        "\n\nTrạng thái xác thực:" +
                        "\nOTP: " + (openAccountManager.isOtpVerified() ? "Đã xác thực" : "Chưa xác thực") +
                        "\neKYC CCCD: " + (openAccountManager.isCccdVerified() ? "Đã xác thực" : "Chưa xác thực") +
                        "\nKhuôn mặt: " + (openAccountManager.isFaceVerified() ? "Đã xác thực" : "Chưa xác thực") +
                        "\n\nTôi xác nhận các thông tin trên là chính xác và đồng ý với điều khoản sử dụng dịch vụ ngân hàng điện tử của Techcombank Study."
        );
    }

    private String getDisplayEmail() {
        String email = openAccountManager.getEmail();

        if (email == null || email.trim().isEmpty()) {
            return "Không cung cấp";
        }

        return email;
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private String maskUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "-";
        }

        String value = username.trim();

        if (value.length() <= 4) {
            return "****";
        }

        return "******" + value.substring(value.length() - 4);
    }

    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VND";
    }

    private void createAccount() {
        String fullName = openAccountManager.getFullName();
        String phone = openAccountManager.getPhone();
        String email = openAccountManager.getEmail();
        String password = openAccountManager.getPassword();
        String cccdNumber = openAccountManager.getCccdNumber();
        String username = openAccountManager.getUsername();
        String accountNumber = openAccountManager.getAccountNumber();
        String accountType = openAccountManager.getAccountType();
        String job = openAccountManager.getJob();

        txtAgreementMessage.setText("");

        if (phone == null || phone.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu số điện thoại đăng ký");
            return;
        }

        if (!phone.matches("^(03|05|07|08|09)[0-9]{8}$")) {
            txtAgreementMessage.setText("Số điện thoại không hợp lệ");
            return;
        }

        if (password == null || password.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu mật khẩu đăng ký");
            return;
        }

        if (password.length() < 6) {
            txtAgreementMessage.setText("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu họ tên khách hàng");
            return;
        }

        if (cccdNumber == null || !cccdNumber.matches("^[0-9]{12}$")) {
            txtAgreementMessage.setText("Thiếu CCCD hợp lệ. CCCD phải gồm đúng 12 chữ số");
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu email");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            txtAgreementMessage.setText("Email không hợp lệ");
            return;
        }

        if (username == null || username.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu tên đăng nhập");
            return;
        }

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu số tài khoản");
            return;
        }

        if (!accountNumber.matches("^[0-9]{6,14}$")) {
            txtAgreementMessage.setText("Số tài khoản phải gồm 6 đến 14 chữ số");
            return;
        }

        if (accountType == null || accountType.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu loại tài khoản");
            return;
        }

        if (job == null || job.trim().isEmpty()) {
            txtAgreementMessage.setText("Thiếu nghề nghiệp");
            return;
        }

        if (!openAccountManager.isOtpVerified()) {
            txtAgreementMessage.setText("Chưa xác thực OTP đăng ký");
            return;
        }

        if (!openAccountManager.isCccdVerified()) {
            txtAgreementMessage.setText("Chưa xác thực CCCD eKYC");
            return;
        }

        if (!openAccountManager.isFaceVerified()) {
            txtAgreementMessage.setText("Chưa xác thực khuôn mặt");
            return;
        }

        registerPhone = phone;
        registerPassword = password;

        btnAgreeOpenAccount.setEnabled(false);
        txtAgreementMessage.setText("Đang tạo tài khoản...");

        RegisterRequest request = new RegisterRequest(
                fullName.trim(),
                phone.trim(),
                email.trim(),
                password,
                username.trim(),
                accountNumber.trim(),
                accountType.trim(),
                openAccountManager.getAccountFee(),
                cccdNumber.trim(),
                job.trim()
        );

        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();

                    if ("success".equals(registerResponse.getStatus())) {
                        txtAgreementMessage.setText("Tạo tài khoản thành công. Đang đăng nhập...");
                        loginAfterRegister();
                    } else {
                        btnAgreeOpenAccount.setEnabled(true);
                        txtAgreementMessage.setText(normalizeBackendMessage(registerResponse.getMessage()));
                    }
                } else {
                    btnAgreeOpenAccount.setEnabled(true);
                    String errorMessage = extractErrorMessage(response);
                    txtAgreementMessage.setText(normalizeBackendMessage(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnAgreeOpenAccount.setEnabled(true);
                txtAgreementMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void loginAfterRegister() {
        LoginRequest loginRequest = new LoginRequest(registerPhone, registerPassword);

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnAgreeOpenAccount.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if ("success".equals(loginResponse.getStatus())) {
                        User user = loginResponse.getUser();

                        if (loginResponse.getToken() == null || loginResponse.getToken().trim().isEmpty()) {
                            txtAgreementMessage.setText("Tạo tài khoản thành công nhưng server không trả token đăng nhập.");
                            return;
                        }

                        if (user == null) {
                            txtAgreementMessage.setText("Tạo tài khoản thành công nhưng server không trả thông tin người dùng.");
                            return;
                        }

                        sessionManager.saveLoginSession(
                                loginResponse.getToken(),
                                user.getFullName(),
                                user.getPhone(),
                                user.getBalance()
                        );

                        openAccountManager.clearAll();

                        Intent intent = new Intent(AgreementActivity.this, HomeActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        txtAgreementMessage.setText(normalizeBackendMessage(loginResponse.getMessage()));
                    }
                } else {
                    String errorMessage = extractErrorMessage(response);
                    txtAgreementMessage.setText("Tạo tài khoản thành công nhưng đăng nhập tự động thất bại: " + normalizeBackendMessage(errorMessage));
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnAgreeOpenAccount.setEnabled(true);
                txtAgreementMessage.setText("Tạo tài khoản thành công nhưng không kết nối được để đăng nhập: " + t.getMessage());
            }
        });
    }

    private String extractErrorMessage(Response<?> response) {
        try {
            if (response == null) {
                return "Không nhận được phản hồi từ server";
            }

            if (response.errorBody() != null) {
                String raw = response.errorBody().string();

                if (raw != null && !raw.trim().isEmpty()) {
                    try {
                        JSONObject jsonObject = new JSONObject(raw);

                        String message = jsonObject.optString("message", "");
                        if (message != null && !message.trim().isEmpty()) {
                            return message;
                        }

                        String error = jsonObject.optString("error", "");
                        if (error != null && !error.trim().isEmpty()) {
                            return error;
                        }

                        return raw;
                    } catch (Exception jsonException) {
                        return raw;
                    }
                }
            }

            return "Yêu cầu thất bại. HTTP code: " + response.code();
        } catch (Exception e) {
            return "Không đọc được thông báo lỗi từ server";
        }
    }

    private String normalizeBackendMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Thao tác thất bại. Vui lòng thử lại.";
        }

        String msg = message.trim();

        if (msg.equalsIgnoreCase("Phone number already exists")) {
            return "Số điện thoại đã được đăng ký.";
        }

        if (msg.equalsIgnoreCase("Username already exists")) {
            return "Tên đăng nhập đã tồn tại. Vui lòng chọn tên đăng nhập khác.";
        }

        if (msg.equalsIgnoreCase("Account number already exists")) {
            return "Số tài khoản đã tồn tại. Vui lòng chọn số tài khoản khác.";
        }

        if (msg.equalsIgnoreCase("CCCD already registered")
                || msg.equalsIgnoreCase("CCCD number already exists")
                || msg.equalsIgnoreCase("CCCD already exists")) {
            return "Số CCCD đã được đăng ký. Vui lòng dùng CCCD khác.";
        }

        if (msg.equalsIgnoreCase("OTP is required")
                || msg.equalsIgnoreCase("OTP not verified")
                || msg.equalsIgnoreCase("OTP verification required")) {
            return "OTP chưa được xác thực hoặc đã hết hiệu lực. Vui lòng thực hiện lại bước OTP.";
        }

        if (msg.equalsIgnoreCase("eKYC verification required")
                || msg.equalsIgnoreCase("eKYC not verified")
                || msg.equalsIgnoreCase("CCCD and face verification are required")) {
            return "Thông tin eKYC chưa được xác thực đầy đủ. Vui lòng xác minh lại CCCD và khuôn mặt.";
        }

        if (msg.equalsIgnoreCase("Invalid CCCD number")
                || msg.equalsIgnoreCase("CCCD must be 12 digits")) {
            return "Số CCCD không hợp lệ. CCCD phải gồm đúng 12 chữ số.";
        }

        if (msg.equalsIgnoreCase("Invalid phone number")) {
            return "Số điện thoại không hợp lệ.";
        }

        if (msg.equalsIgnoreCase("Password must be at least 6 characters")) {
            return "Mật khẩu phải có ít nhất 6 ký tự.";
        }

        if (msg.equalsIgnoreCase("Account number must be 6-14 digits")) {
            return "Số tài khoản phải gồm 6 đến 14 chữ số.";
        }

        return msg;
    }
}