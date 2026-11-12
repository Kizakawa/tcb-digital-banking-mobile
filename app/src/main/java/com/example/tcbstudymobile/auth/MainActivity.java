package com.example.tcbstudymobile.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.forgotpassword.ForgotPasswordActivity;
import com.example.tcbstudymobile.home.HomeActivity;
import com.example.tcbstudymobile.model.LoginRequest;
import com.example.tcbstudymobile.model.LoginResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.storage.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.example.tcbstudymobile.openaccount.OpenAccountEntryActivity;

public class MainActivity extends AppCompatActivity {

    private EditText edtPhone, edtPassword;
    private Button btnLogin;
    private TextView txtMessage, txtGoToRegister, txtForgotPassword;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtPhone = findViewById(R.id.edtPhone);
        edtPassword = findViewById(R.id.edtPassword);
        btnLogin = findViewById(R.id.btnLogin);
        txtMessage = findViewById(R.id.txtMessage);
        txtGoToRegister = findViewById(R.id.txtGoToRegister);
        txtForgotPassword = findViewById(R.id.txtForgotPassword);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        btnLogin.setOnClickListener(v -> login());

        txtGoToRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, OpenAccountEntryActivity.class);
            startActivity(intent);
        });

        txtForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(
                    MainActivity.this,
                    ForgotPasswordActivity.class
            );
            startActivity(intent);
        });
    }

    private void login() {
        String phone = edtPhone.getText().toString().trim();
        String password = edtPassword.getText().toString().trim();

        if (phone.isEmpty()) {
            txtMessage.setText("Vui lòng nhập số điện thoại");
            return;
        }

        if (password.isEmpty()) {
            txtMessage.setText("Vui lòng nhập mật khẩu");
            return;
        }

        btnLogin.setEnabled(false);
        txtMessage.setText("Đang đăng nhập...");

        LoginRequest request = new LoginRequest(phone, password);

        apiService.login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if ("success".equals(loginResponse.getStatus()) && loginResponse.getUser() != null) {
                        sessionManager.saveLoginSession(
                                loginResponse.getToken(),
                                loginResponse.getUser().getFullName(),
                                loginResponse.getUser().getPhone(),
                                loginResponse.getUser().getBalance()
                        );

                        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        txtMessage.setText(loginResponse.getMessage());
                    }
                } else {
                    txtMessage.setText("Sai số điện thoại hoặc mật khẩu");
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                txtMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }
}