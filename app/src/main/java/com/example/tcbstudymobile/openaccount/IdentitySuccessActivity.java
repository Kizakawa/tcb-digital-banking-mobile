package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class IdentitySuccessActivity extends AppCompatActivity {

    private Button btnNextAfterIdentity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_success);

        btnNextAfterIdentity = findViewById(R.id.btnNextAfterIdentity);

        btnNextAfterIdentity.setOnClickListener(v -> {
            Intent intent = new Intent(IdentitySuccessActivity.this, EmailInputActivity.class);
            startActivity(intent);
        });
    }
}