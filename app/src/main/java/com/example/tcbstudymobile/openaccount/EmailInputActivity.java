package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class EmailInputActivity extends AppCompatActivity {

    private TextView txtBackEmail, txtEmailMessage, txtVerifiedFullName, txtVerifiedCccd;
    private EditText edtOpenEmail;
    private Button btnContinueEmail;

    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_input);

        txtBackEmail = findViewById(R.id.txtBackEmail);
        txtEmailMessage = findViewById(R.id.txtEmailMessage);
        txtVerifiedFullName = findViewById(R.id.txtVerifiedFullName);
        txtVerifiedCccd = findViewById(R.id.txtVerifiedCccd);

        edtOpenEmail = findViewById(R.id.edtOpenEmail);
        btnContinueEmail = findViewById(R.id.btnContinueEmail);

        openAccountManager = new OpenAccountManager(this);

        showVerifiedIdentity();
        fillOldEmailIfExists();

        txtBackEmail.setOnClickListener(v -> finish());

        btnContinueEmail.setOnClickListener(v -> validateAndContinue());
    }

    private void showVerifiedIdentity() {
        String fullName = openAccountManager.getFullName();
        String cccdNumber = openAccountManager.getCccdNumber();

        if (fullName == null || fullName.trim().isEmpty()
                || cccdNumber == null || cccdNumber.trim().isEmpty()
                || !openAccountManager.isCccdVerified()
                || !openAccountManager.isFaceVerified()) {

            txtEmailMessage.setText("Thông tin định danh chưa được xác minh. Vui lòng quay lại bước xác minh danh tính.");
            btnContinueEmail.setEnabled(false);
            return;
        }

        txtVerifiedFullName.setText(fullName);
        txtVerifiedCccd.setText(cccdNumber);
    }

    private void fillOldEmailIfExists() {
        String oldEmail = openAccountManager.getEmail();

        if (oldEmail != null && !oldEmail.trim().isEmpty()) {
            edtOpenEmail.setText(oldEmail);
        }
    }

    private void validateAndContinue() {
        String email = edtOpenEmail.getText().toString().trim();

        if (!openAccountManager.isCccdVerified()) {
            txtEmailMessage.setText("Chưa xác minh CCCD eKYC");
            return;
        }

        if (!openAccountManager.isFaceVerified()) {
            txtEmailMessage.setText("Chưa xác minh khuôn mặt");
            return;
        }

        if (email.isEmpty()) {
            txtEmailMessage.setText("Vui lòng nhập email");
            return;
        }

        if (!isValidEmail(email)) {
            txtEmailMessage.setText("Email không hợp lệ");
            return;
        }

        openAccountManager.setEmail(email);

        Intent intent = new Intent(EmailInputActivity.this, JobSelectActivity.class);
        startActivity(intent);
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}