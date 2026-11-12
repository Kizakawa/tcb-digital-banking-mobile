package com.example.tcbstudymobile.forgotpassword;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.auth.MainActivity;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.ResetPasswordRequest;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText edtNewPassword;
    private EditText edtConfirmPassword;
    private Button btnResetPassword;
    private TextView txtResetPasswordMessage;

    private ApiService apiService;

    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        phone = getIntent().getStringExtra("phone");

        apiService = ApiClient.getClient().create(ApiService.class);

        edtNewPassword = findViewById(R.id.edtNewPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);
        btnResetPassword = findViewById(R.id.btnResetPassword);
        txtResetPasswordMessage = findViewById(R.id.txtResetPasswordMessage);

        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {

        String newPassword = edtNewPassword.getText().toString().trim();
        String confirmPassword = edtConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty()) {
            txtResetPasswordMessage.setText("Vui lòng nhập mật khẩu mới");
            return;
        }

        if (newPassword.length() < 6) {
            txtResetPasswordMessage.setText("Mật khẩu tối thiểu 6 ký tự");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            txtResetPasswordMessage.setText("Mật khẩu xác nhận không khớp");
            return;
        }

        btnResetPassword.setEnabled(false);

        ResetPasswordRequest request =
                new ResetPasswordRequest(phone, newPassword);

        apiService.resetPassword(request)
                .enqueue(new Callback<BasicResponse>() {

                    @Override
                    public void onResponse(
                            Call<BasicResponse> call,
                            Response<BasicResponse> response
                    ) {

                        btnResetPassword.setEnabled(true);

                        if (response.isSuccessful()
                                && response.body() != null) {

                            BasicResponse res = response.body();

                            if ("success".equals(res.getStatus())) {

                                Intent intent = new Intent(
                                        ResetPasswordActivity.this,
                                        MainActivity.class
                                );

                                intent.setFlags(
                                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                );

                                startActivity(intent);

                                finish();

                            } else {
                                txtResetPasswordMessage
                                        .setText(res.getMessage());
                            }

                        } else {
                            txtResetPasswordMessage
                                    .setText("Không đổi được mật khẩu");
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<BasicResponse> call,
                            Throwable t
                    ) {

                        btnResetPassword.setEnabled(true);

                        txtResetPasswordMessage
                                .setText("Lỗi: " + t.getMessage());
                    }
                });
    }
}