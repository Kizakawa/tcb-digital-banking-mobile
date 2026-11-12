package com.example.tcbstudymobile.bill;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class BillProviderActivity extends AppCompatActivity {

    private String serviceCode, serviceName;
    private TextView txtCloseProvider, txtProviderTitle, txtProviderSubtitle;
    private LinearLayout layoutProviderList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_provider);

        serviceCode = getIntent().getStringExtra("serviceCode");
        serviceName = getIntent().getStringExtra("serviceName");
        if (serviceCode == null) serviceCode = "ELECTRICITY";
        if (serviceName == null) serviceName = "hóa đơn";

        txtCloseProvider = findViewById(R.id.txtCloseProvider);
        txtProviderTitle = findViewById(R.id.txtProviderTitle);
        txtProviderSubtitle = findViewById(R.id.txtProviderSubtitle);
        layoutProviderList = findViewById(R.id.layoutProviderList);

        txtProviderTitle.setText("Thanh toán " + serviceName);
        txtProviderSubtitle.setText("cho nhà cung cấp");

        txtCloseProvider.setOnClickListener(v -> finish());

        loadProviders();
    }

    private void loadProviders() {
        switch (serviceCode) {
            case "INTERNET":
                addProviderRow("FPT", "FPT Telecom", "FPT001");
                addProviderRow("VIETTEL", "Viettel Internet", "VTL001");
                addProviderRow("VNPT", "VNPT Internet", "VNPT001");
                break;
            case "WATER":
                addProviderRow("HANOIWATER", "Nước sạch Hà Nội", "HNW001");
                addProviderRow("HCMWATER", "Cấp nước TP.HCM", "HCMW01");
                break;
            case "PHONE":
                addProviderRow("MOBIFONE", "MobiFone", "MB001");
                addProviderRow("VINAPHONE", "VinaPhone", "VNP001");
                break;
            case "TV":
                addProviderRow("SCTV", "Truyền hình cáp SCTV", "SCTV001");
                addProviderRow("VIETTEL_TV", "Viettel TV", "VTV001");
                break;
            case "ELECTRICITY":
            default:
                addProviderRow("EVN_HN", "EVN Hà Nội", "EVN001, EVN002");
                addProviderRow("EVN_HCM", "EVN TP. Hồ Chí Minh", "EVNHCM01");
                break;
        }
    }

    private void addProviderRow(String providerCode, String providerName, String sampleCodeHint) {
        TextView row = new TextView(this);
        row.setText(providerName);
        row.setTextSize(16);
        row.setTextColor(0xFF222222);
        row.setPadding(0, 24, 0, 24);

        row.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        row.setOnClickListener(v -> {
            Intent intent = new Intent(BillProviderActivity.this, BillCustomerCodeActivity.class);
            intent.putExtra("serviceCode", serviceCode);
            intent.putExtra("serviceName", serviceName);
            intent.putExtra("providerCode", providerCode);
            intent.putExtra("providerName", providerName);
            intent.putExtra("sampleCodeHint", sampleCodeHint);
            startActivity(intent);
        });

        layoutProviderList.addView(row);
    }
}
