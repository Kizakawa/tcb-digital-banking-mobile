package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class IdentityMethodActivity extends AppCompatActivity {

    private TextView txtBackIdentityMethod;
    private LinearLayout cardScanCccd, cardConnectVneid;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identity_method);

        txtBackIdentityMethod = findViewById(R.id.txtBackIdentityMethod);
        cardScanCccd = findViewById(R.id.cardScanCccd);
        cardConnectVneid = findViewById(R.id.cardConnectVneid);

        openAccountManager = new OpenAccountManager(this);

        txtBackIdentityMethod.setOnClickListener(v -> finish());

        cardScanCccd.setOnClickListener(v -> {
            openAccountManager.setIdentityMethod("CCCD_CHIP");
            Intent intent = new Intent(IdentityMethodActivity.this, CccdGuideActivity.class);
            startActivity(intent);
        });

        cardConnectVneid.setOnClickListener(v -> {
            openAccountManager.setIdentityMethod("VNEID");
            Intent intent = new Intent(IdentityMethodActivity.this, VneidLinkActivity.class);
            startActivity(intent);
        });
    }
}