package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class PasswordCreateActivity extends AppCompatActivity {

    private TextView txtBackPassword, txtPasswordMessage;
    private EditText edtPasswordOpen, edtConfirmPasswordOpen;
    private Button btnContinuePassword;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_create);

        txtBackPassword = findViewById(R.id.txtBackPassword);
        txtPasswordMessage = findViewById(R.id.txtPasswordMessage);
        edtPasswordOpen = findViewById(R.id.edtPasswordOpen);
        edtConfirmPasswordOpen = findViewById(R.id.edtConfirmPasswordOpen);
        btnContinuePassword = findViewById(R.id.btnContinuePassword);

        openAccountManager = new OpenAccountManager(this);

        txtBackPassword.setOnClickListener(v -> finish());

        btnContinuePassword.setOnClickListener(v -> validateAndContinue());
    }

    private void validateAndContinue() {
        String password = edtPasswordOpen.getText().toString();
        String confirmPassword = edtConfirmPasswordOpen.getText().toString();
        String username = openAccountManager.getUsername();

        if (password.isEmpty()) {
            txtPasswordMessage.setText("Vui lòng nhập mật khẩu");
            return;
        }

        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!]).{6,20}$")) {
            txtPasswordMessage.setText("Mật khẩu phải có 6–20 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
            return;
        }

        if (password.contains(" ")) {
            txtPasswordMessage.setText("Mật khẩu không được chứa dấu cách");
            return;
        }

        if (password.equalsIgnoreCase(username)) {
            txtPasswordMessage.setText("Mật khẩu không được trùng tên đăng nhập");
            return;
        }

        if (!password.equals(confirmPassword)) {
            txtPasswordMessage.setText("Mật khẩu nhập lại không khớp");
            return;
        }

        openAccountManager.setPassword(password);

        Intent intent = new Intent(PasswordCreateActivity.this, AgreementActivity.class);
        startActivity(intent);
    }
}