package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.CheckExistResponse;
import com.example.tcbstudymobile.model.OtpSendRequest;
import com.example.tcbstudymobile.model.OtpSendResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PhoneInputActivity extends AppCompatActivity {

    private TextView txtBackPhone, txtPhoneMessage;
    private EditText edtOpenPhone;
    private Button btnContinuePhone;

    private OpenAccountManager openAccountManager;
    private ApiService apiService;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable phoneCheckRunnable;

    private boolean isPhoneAvailable = false;
    private String lastCheckedPhone = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_input);

        txtBackPhone = findViewById(R.id.txtBackPhone);
        txtPhoneMessage = findViewById(R.id.txtPhoneMessage);
        edtOpenPhone = findViewById(R.id.edtOpenPhone);
        btnContinuePhone = findViewById(R.id.btnContinuePhone);

        openAccountManager = new OpenAccountManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        txtBackPhone.setOnClickListener(v -> finish());

        edtOpenPhone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handlePhoneTyping(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnContinuePhone.setOnClickListener(v -> validateAndContinue());
    }

    private void handlePhoneTyping(String phone) {
        isPhoneAvailable = false;
        lastCheckedPhone = "";

        if (phoneCheckRunnable != null) {
            handler.removeCallbacks(phoneCheckRunnable);
        }

        if (phone.isEmpty()) {
            txtPhoneMessage.setText("");
            return;
        }

        if (phone.length() < 10) {
            txtPhoneMessage.setText("");
            return;
        }

        if (!isValidVietnamPhone(phone)) {
            txtPhoneMessage.setText("Số điện thoại không hợp lệ");
            return;
        }

        txtPhoneMessage.setText("Đang kiểm tra số điện thoại...");

        phoneCheckRunnable = () -> checkPhoneExists(phone, false);
        handler.postDelayed(phoneCheckRunnable, 500);
    }

    private void validateAndContinue() {
        String phone = edtOpenPhone.getText().toString().trim();

        if (phone.isEmpty()) {
            txtPhoneMessage.setText("Vui lòng nhập số điện thoại");
            return;
        }

        if (!isValidVietnamPhone(phone)) {
            txtPhoneMessage.setText("Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam gồm 10 số.");
            return;
        }

        if (phone.equals(lastCheckedPhone) && isPhoneAvailable) {
            sendRegisterOtp(phone);
            return;
        }

        checkPhoneExists(phone, true);
    }

    private boolean isValidVietnamPhone(String phone) {
        return phone.matches("^(03|05|07|08|09)[0-9]{8}$");
    }

    private void checkPhoneExists(String phone, boolean continueIfAvailable) {
        btnContinuePhone.setEnabled(false);
        txtPhoneMessage.setText("Đang kiểm tra số điện thoại...");

        apiService.checkPhone(phone).enqueue(new Callback<CheckExistResponse>() {
            @Override
            public void onResponse(Call<CheckExistResponse> call, Response<CheckExistResponse> response) {
                btnContinuePhone.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    CheckExistResponse checkResponse = response.body();
                    lastCheckedPhone = phone;

                    if (checkResponse.isExists()) {
                        isPhoneAvailable = false;
                        txtPhoneMessage.setText("Số điện thoại đã được đăng ký. Vui lòng nhập số khác.");
                    } else {
                        isPhoneAvailable = true;
                        txtPhoneMessage.setText("Số điện thoại có thể sử dụng.");

                        if (continueIfAvailable) {
                            sendRegisterOtp(phone);
                        }
                    }
                } else {
                    isPhoneAvailable = false;
                    txtPhoneMessage.setText("Không kiểm tra được số điện thoại. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(Call<CheckExistResponse> call, Throwable t) {
                btnContinuePhone.setEnabled(true);
                isPhoneAvailable = false;
                txtPhoneMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void sendRegisterOtp(String phone) {
        btnContinuePhone.setEnabled(false);
        txtPhoneMessage.setText("Đang gửi OTP...");

        OtpSendRequest request = new OtpSendRequest(phone, "REGISTER");

        apiService.sendOtp(request).enqueue(new Callback<OtpSendResponse>() {
            @Override
            public void onResponse(Call<OtpSendResponse> call, Response<OtpSendResponse> response) {
                btnContinuePhone.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    OtpSendResponse otpResponse = response.body();

                    if ("success".equals(otpResponse.getStatus())) {
                        openAccountManager.setPhone(phone);

                        /*
                         * Reset trạng thái cũ để tránh lấy nhầm dữ liệu lần đăng ký trước.
                         */
                        openAccountManager.setOtpVerified(false);
                        openAccountManager.setCccdVerified(false);
                        openAccountManager.setFaceVerified(false);

                        /*
                         * API demo trả otpCode để phục vụ kiểm thử.
                         * Khi làm thật thì không trả OTP về response.
                         */
                        openAccountManager.setLastRegisterOtpCode(otpResponse.getOtpCode());

                        Intent intent = new Intent(PhoneInputActivity.this, OtpVerifyActivity.class);
                        startActivity(intent);
                    } else {
                        txtPhoneMessage.setText(otpResponse.getMessage());
                    }
                } else {
                    txtPhoneMessage.setText("Gửi OTP thất bại. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(Call<OtpSendResponse> call, Throwable t) {
                btnContinuePhone.setEnabled(true);
                txtPhoneMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }
}