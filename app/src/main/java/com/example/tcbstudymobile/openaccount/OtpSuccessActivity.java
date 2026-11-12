package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class OtpSuccessActivity extends AppCompatActivity {

    private Button btnNextAfterOtp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_otp_success);

        btnNextAfterOtp = findViewById(R.id.btnNextAfterOtp);

        btnNextAfterOtp.setOnClickListener(v -> {
            Intent intent = new Intent(OtpSuccessActivity.this, IdentityMethodActivity.class);
            startActivity(intent);
        });
    }
}