package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class CccdGuideActivity extends AppCompatActivity {

    private TextView txtBackCccdGuide;
    private Button btnCaptureCccd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cccd_guide);

        txtBackCccdGuide = findViewById(R.id.txtBackCccdGuide);
        btnCaptureCccd = findViewById(R.id.btnCaptureCccd);

        txtBackCccdGuide.setOnClickListener(v -> finish());

        // Vì chưa tích hợp camera/OCR thật, nút này sẽ mở màn nhập dữ liệu CCCD mô phỏng
        btnCaptureCccd.setOnClickListener(v -> {
            Intent intent = new Intent(CccdGuideActivity.this, MockCccdInputActivity.class);
            startActivity(intent);
        });
    }
}