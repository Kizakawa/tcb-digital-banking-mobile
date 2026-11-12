package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class VneidLinkActivity extends AppCompatActivity {

    private TextView txtBackVneid;
    private Button btnAgreeVneid, btnCancelVneid;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vneid_link);

        txtBackVneid = findViewById(R.id.txtBackVneid);
        btnAgreeVneid = findViewById(R.id.btnAgreeVneid);
        btnCancelVneid = findViewById(R.id.btnCancelVneid);

        openAccountManager = new OpenAccountManager(this);

        txtBackVneid.setOnClickListener(v -> finish());

        btnCancelVneid.setOnClickListener(v -> finish());

        btnAgreeVneid.setOnClickListener(v -> {
            openAccountManager.setVneidLinked(true);

            String method = openAccountManager.getIdentityMethod();

            if ("VNEID".equals(method)) {
                // Nếu người dùng chọn VNeID từ đầu, sau bước liên kết VNeID mô phỏng,
                // vẫn cần nhập dữ liệu định danh mô phỏng để backend verify eKYC.
                Intent intent = new Intent(VneidLinkActivity.this, MockCccdInputActivity.class);
                startActivity(intent);
            } else {
                // Nếu đi theo luồng quét CCCD, VNeID chỉ là bước liên kết bổ sung sau face.
                Intent intent = new Intent(VneidLinkActivity.this, IdentitySuccessActivity.class);
                startActivity(intent);
            }
        });
    }
}