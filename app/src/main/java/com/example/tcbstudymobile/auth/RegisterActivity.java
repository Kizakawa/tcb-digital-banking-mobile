package com.example.tcbstudymobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.RegisterRequest;
import com.example.tcbstudymobile.model.RegisterResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText edtFullName, edtRegisterPhone, edtEmail, edtRegisterPassword;
    private Button btnRegister;
    private TextView txtRegisterMessage, txtBackToLogin;

    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        edtFullName = findViewById(R.id.edtFullName);
        edtRegisterPhone = findViewById(R.id.edtRegisterPhone);
        edtEmail = findViewById(R.id.edtEmail);
        edtRegisterPassword = findViewById(R.id.edtRegisterPassword);
        btnRegister = findViewById(R.id.btnRegister);
        txtRegisterMessage = findViewById(R.id.txtRegisterMessage);
        txtBackToLogin = findViewById(R.id.txtBackToLogin);

        apiService = ApiClient.getClient().create(ApiService.class);

        btnRegister.setOnClickListener(v -> register());

        txtBackToLogin.setOnClickListener(v -> {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void register() {
        String fullName = edtFullName.getText().toString().trim();
        String phone = edtRegisterPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String password = edtRegisterPassword.getText().toString().trim();

        if (fullName.isEmpty()) {
            txtRegisterMessage.setText("Vui lòng nhập họ tên");
            return;
        }

        if (phone.isEmpty()) {
            txtRegisterMessage.setText("Vui lòng nhập số điện thoại");
            return;
        }

        if (phone.length() < 9 || phone.length() > 11) {
            txtRegisterMessage.setText("Số điện thoại không hợp lệ");
            return;
        }

        if (password.isEmpty()) {
            txtRegisterMessage.setText("Vui lòng nhập mật khẩu");
            return;
        }

        if (password.length() < 6) {
            txtRegisterMessage.setText("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        btnRegister.setEnabled(false);
        txtRegisterMessage.setText("Đang đăng ký...");

        RegisterRequest request = new RegisterRequest(fullName, phone, email, password);

        apiService.register(request).enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call, Response<RegisterResponse> response) {
                btnRegister.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    RegisterResponse registerResponse = response.body();

                    if ("success".equals(registerResponse.getStatus())) {
                        txtRegisterMessage.setText("Đăng ký thành công. Đang quay lại đăng nhập...");

                        txtRegisterMessage.postDelayed(() -> {
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }, 1200);
                    } else {
                        txtRegisterMessage.setText(registerResponse.getMessage());
                    }
                } else {
                    txtRegisterMessage.setText("Đăng ký thất bại. Số điện thoại có thể đã tồn tại.");
                }
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable t) {
                btnRegister.setEnabled(true);
                txtRegisterMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }
}