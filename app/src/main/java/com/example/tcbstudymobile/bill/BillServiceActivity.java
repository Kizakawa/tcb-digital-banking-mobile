package com.example.tcbstudymobile.bill;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class BillServiceActivity extends AppCompatActivity {

    private TextView btnElectricBill, btnWaterBill, btnPhoneTopup, btnPostpaidMobile,
            btnInternetBill, btnTvBill, btnApartmentFee, btnInsuranceFee;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_service);

        btnElectricBill = findViewById(R.id.btnElectricBill);
        btnWaterBill = findViewById(R.id.btnWaterBill);
        btnPhoneTopup = findViewById(R.id.btnPhoneTopup);
        btnPostpaidMobile = findViewById(R.id.btnPostpaidMobile);
        btnInternetBill = findViewById(R.id.btnInternetBill);
        btnTvBill = findViewById(R.id.btnTvBill);
        btnApartmentFee = findViewById(R.id.btnApartmentFee);
        btnInsuranceFee = findViewById(R.id.btnInsuranceFee);

        btnElectricBill.setOnClickListener(v -> openProvider("ELECTRICITY", "hóa đơn điện"));
        btnWaterBill.setOnClickListener(v -> openProvider("WATER", "hóa đơn nước"));
        btnPhoneTopup.setOnClickListener(v -> openProvider("PHONE", "nạp tiền điện thoại"));
        btnPostpaidMobile.setOnClickListener(v -> openProvider("PHONE", "cước di động trả sau"));
        btnInternetBill.setOnClickListener(v -> openProvider("INTERNET", "hóa đơn internet"));
        btnTvBill.setOnClickListener(v -> openProvider("TV", "truyền hình"));
        btnApartmentFee.setOnClickListener(v -> openProvider("ELECTRICITY", "phí chung cư"));
        btnInsuranceFee.setOnClickListener(v -> openProvider("PHONE", "phí bảo hiểm"));
    }

    private void openProvider(String serviceCode, String serviceName) {
        Intent intent = new Intent(BillServiceActivity.this, BillProviderActivity.class);
        intent.putExtra("serviceCode", serviceCode);
        intent.putExtra("serviceName", serviceName);
        startActivity(intent);
    }
}
