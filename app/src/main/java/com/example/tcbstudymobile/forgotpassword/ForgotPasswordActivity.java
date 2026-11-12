package com.example.tcbstudymobile.forgotpassword;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.OtpSendRequest;
import com.example.tcbstudymobile.model.OtpSendResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.network.ErrorParser;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText edtPhoneForgot;
    private Button btnContinueForgot;
    private TextView txtForgotMessage;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        edtPhoneForgot = findViewById(R.id.edtPhoneForgot);
        btnContinueForgot = findViewById(R.id.btnContinueForgot);
        txtForgotMessage = findViewById(R.id.txtForgotMessage);
        apiService = ApiClient.getClient().create(ApiService.class);

        btnContinueForgot.setOnClickListener(v -> requestResetOtp());
    }

    private void requestResetOtp() {
        String phone = edtPhoneForgot.getText().toString().trim();

        if (phone.isEmpty()) {
            txtForgotMessage.setText("Vui lòng nhập số điện thoại");
            return;
        }

        if (!phone.matches("^(03|05|07|08|09)[0-9]{8}$")) {
            txtForgotMessage.setText("Số điện thoại không hợp lệ (10 chữ số)");
            return;
        }

        btnContinueForgot.setEnabled(false);
        txtForgotMessage.setText("Đang gửi mã OTP xác thực...");

        OtpSendRequest request = new OtpSendRequest(phone, "RESET_PASSWORD");
        apiService.sendOtp(request).enqueue(new Callback<OtpSendResponse>() {
            @Override
            public void onResponse(Call<OtpSendResponse> call, Response<OtpSendResponse> response) {
                btnContinueForgot.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    txtForgotMessage.setText("");
                    Intent intent = new Intent(ForgotPasswordActivity.this, ForgotOtpActivity.class);
                    intent.putExtra("phone", phone);
                    startActivity(intent);
                } else {
                    txtForgotMessage.setText(
                            ErrorParser.parse(response, "Không gửi được mã OTP. Vui lòng thử lại.")
                    );
                }
            }

            @Override
            public void onFailure(Call<OtpSendResponse> call, Throwable t) {
                btnContinueForgot.setEnabled(true);
                txtForgotMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }
}
