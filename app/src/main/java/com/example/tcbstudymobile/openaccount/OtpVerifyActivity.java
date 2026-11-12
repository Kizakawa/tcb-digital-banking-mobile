package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.OtpVerifyRequest;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpVerifyActivity extends AppCompatActivity {

    private TextView txtBackOtp, txtOtpTitle, txtOtpTimer, txtOtpMessage;
    private EditText edtOtpCode;
    private Button btnVerifyOtp;

    private OpenAccountManager openAccountManager;
    private ApiService apiService;

    private CountDownTimer countDownTimer;
    private boolean otpExpired = false;

    private static final long OTP_TIME_MS = 90 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_verify);

        txtBackOtp = findViewById(R.id.txtBackOtp);
        txtOtpTitle = findViewById(R.id.txtOtpTitle);
        txtOtpTimer = findViewById(R.id.txtOtpTimer);
        txtOtpMessage = findViewById(R.id.txtOtpMessage);
        edtOtpCode = findViewById(R.id.edtOtpCode);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);

        openAccountManager = new OpenAccountManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        String phone = openAccountManager.getPhone();
        txtOtpTitle.setText("Vui lòng nhập mã xác thực được gửi tới số điện thoại " + maskPhone(phone));

        String demoOtp = openAccountManager.getLastRegisterOtpCode();
        if (demoOtp != null && !demoOtp.trim().isEmpty()) {
            txtOtpMessage.setText("OTP demo từ server: " + demoOtp);
            txtOtpMessage.setTextColor(0xFF777777);
        } else {
            txtOtpMessage.setText("");
        }

        txtBackOtp.setOnClickListener(v -> finish());

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());

        startOtpTimer();
    }

    private void startOtpTimer() {
        otpExpired = false;

        countDownTimer = new CountDownTimer(OTP_TIME_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long second = millisUntilFinished / 1000;
                txtOtpTimer.setText("Mã OTP sẽ hết hiệu lực sau " + second + " giây");
            }

            @Override
            public void onFinish() {
                otpExpired = true;
                txtOtpTimer.setText("Mã OTP đã hết hiệu lực");
            }
        };

        countDownTimer.start();
    }

    private void verifyOtp() {
        String phone = openAccountManager.getPhone();
        String otp = edtOtpCode.getText().toString().trim();

        if (phone == null || phone.trim().isEmpty()) {
            txtOtpMessage.setText("Thiếu số điện thoại. Vui lòng quay lại bước trước.");
            txtOtpMessage.setTextColor(0xFFD71920);
            return;
        }

        if (otp.isEmpty()) {
            txtOtpMessage.setText("Vui lòng nhập mã OTP");
            txtOtpMessage.setTextColor(0xFFD71920);
            return;
        }

        if (!otp.matches("^[0-9]{6}$")) {
            txtOtpMessage.setText("Mã OTP phải gồm đúng 6 chữ số");
            txtOtpMessage.setTextColor(0xFFD71920);
            return;
        }

        if (otpExpired) {
            txtOtpMessage.setText("Mã OTP đã hết hiệu lực. Vui lòng quay lại nhập lại số điện thoại.");
            txtOtpMessage.setTextColor(0xFFD71920);
            return;
        }

        btnVerifyOtp.setEnabled(false);
        txtOtpMessage.setText("Đang xác thực OTP...");
        txtOtpMessage.setTextColor(0xFF555555);

        OtpVerifyRequest request = new OtpVerifyRequest(phone, otp, "REGISTER");

        apiService.verifyOtp(request).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                btnVerifyOtp.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    BasicResponse basicResponse = response.body();

                    if ("success".equals(basicResponse.getStatus())) {
                        /*
                         * OTP chỉ dùng để xác minh số điện thoại tại bước này.
                         * Sau khi verify thành công, lưu trạng thái đã xác thực.
                         * Không để timer OTP ảnh hưởng bước register cuối.
                         */
                        openAccountManager.setOtpVerified(true);

                        if (countDownTimer != null) {
                            countDownTimer.cancel();
                        }

                        txtOtpTimer.setText("Số điện thoại đã được xác thực");
                        txtOtpMessage.setTextColor(0xFF008000);
                        txtOtpMessage.setText("Xác thực OTP thành công");

                        Intent intent = new Intent(OtpVerifyActivity.this, OtpSuccessActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        txtOtpMessage.setText(basicResponse.getMessage());
                        txtOtpMessage.setTextColor(0xFFD71920);
                    }
                } else {
                    txtOtpMessage.setText("OTP không đúng, đã hết hạn hoặc đã sử dụng");
                    txtOtpMessage.setTextColor(0xFFD71920);
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                btnVerifyOtp.setEnabled(true);
                txtOtpMessage.setText("Không kết nối được server: " + t.getMessage());
                txtOtpMessage.setTextColor(0xFFD71920);
            }
        });
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "******";
        }

        return "******" + phone.substring(phone.length() - 4);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}