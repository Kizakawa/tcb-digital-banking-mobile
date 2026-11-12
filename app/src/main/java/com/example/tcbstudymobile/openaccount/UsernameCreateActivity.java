package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class UsernameCreateActivity extends AppCompatActivity {

    private TextView txtBackUsername, txtUsernameMessage;
    private EditText edtUsername;
    private Button btnContinueUsername;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username_create);

        txtBackUsername = findViewById(R.id.txtBackUsername);
        txtUsernameMessage = findViewById(R.id.txtUsernameMessage);
        edtUsername = findViewById(R.id.edtUsername);
        btnContinueUsername = findViewById(R.id.btnContinueUsername);

        openAccountManager = new OpenAccountManager(this);

        txtBackUsername.setOnClickListener(v -> finish());

        btnContinueUsername.setOnClickListener(v -> validateAndContinue());
    }

    private void validateAndContinue() {
        String username = edtUsername.getText().toString().trim();

        if (username.isEmpty()) {
            txtUsernameMessage.setText("Vui lòng nhập tên đăng nhập");
            return;
        }

        if (!username.matches("^[A-Za-z0-9]{6,16}$")) {
            txtUsernameMessage.setText("Tên đăng nhập phải gồm 6–16 ký tự, chỉ bao gồm chữ và số");
            return;
        }

        openAccountManager.setUsername(username);

        Intent intent = new Intent(UsernameCreateActivity.this, PasswordCreateActivity.class);
        startActivity(intent);
    }
}