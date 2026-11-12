package com.example.tcbstudymobile.forgotpassword;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.OtpVerifyRequest;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.network.ErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotOtpActivity extends AppCompatActivity {

    private EditText edtOtpForgot;
    private Button btnVerifyOtpForgot;
    private TextView txtOtpForgotMessage;

    private ApiService apiService;
    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_otp);

        phone = getIntent().getStringExtra("phone");
        apiService = ApiClient.getClient().create(ApiService.class);

        edtOtpForgot = findViewById(R.id.edtOtpForgot);
        btnVerifyOtpForgot = findViewById(R.id.btnVerifyOtpForgot);
        txtOtpForgotMessage = findViewById(R.id.txtOtpForgotMessage);

        btnVerifyOtpForgot.setOnClickListener(v -> verifyResetOtp());
    }

    private void verifyResetOtp() {
        String otp = edtOtpForgot.getText().toString().trim();

        if (phone == null || phone.trim().isEmpty()) {
            txtOtpForgotMessage.setText("Thiếu số điện thoại. Vui lòng quay lại bước trước.");
            return;
        }

        if (!otp.matches("^[0-9]{6}$")) {
            txtOtpForgotMessage.setText("Mã OTP phải gồm đúng 6 chữ số");
            return;
        }

        btnVerifyOtpForgot.setEnabled(false);
        txtOtpForgotMessage.setText("Đang xác thực mã OTP...");

        OtpVerifyRequest request = new OtpVerifyRequest(phone, otp, "RESET_PASSWORD");
        apiService.verifyOtp(request).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                btnVerifyOtpForgot.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    Intent intent = new Intent(ForgotOtpActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("phone", phone);
                    startActivity(intent);
                    finish();
                } else {
                    txtOtpForgotMessage.setText(
                            ErrorParser.parse(response, "Mã OTP không đúng hoặc đã hết hạn")
                    );
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                btnVerifyOtpForgot.setEnabled(true);
                txtOtpForgotMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }
}
