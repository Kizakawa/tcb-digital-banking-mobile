package com.example.tcbstudymobile.bill;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.home.HomeActivity;
import com.example.tcbstudymobile.R;

import java.text.NumberFormat;
import java.util.Locale;

public class BillSuccessActivity extends AppCompatActivity {

    private TextView txtSuccessAmount, txtSuccessProvider, txtSuccessDetails;
    private Button btnFinishBill;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_success);

        String providerName = getIntent().getStringExtra("providerName");
        String customerCode = getIntent().getStringExtra("customerCode");
        String customerName = getIntent().getStringExtra("customerName");
        String billPeriod = getIntent().getStringExtra("billPeriod");
        double amount = getIntent().getDoubleExtra("amount", 50000);

        txtSuccessAmount = findViewById(R.id.txtSuccessAmount);
        txtSuccessProvider = findViewById(R.id.txtSuccessProvider);
        txtSuccessDetails = findViewById(R.id.txtSuccessDetails);
        btnFinishBill = findViewById(R.id.btnFinishBill);

        txtSuccessAmount.setText("VND " + formatMoneyNumber(amount));
        txtSuccessProvider.setText("Cho " + providerName);

        int transactionId = getIntent().getIntExtra("transactionId", 0);
        String displayTransactionId;

        if (transactionId > 0) {
            displayTransactionId = "BILL" + transactionId;
        } else {
            displayTransactionId = "BILL" + System.currentTimeMillis();
        }

        txtSuccessDetails.setText(
                "Mã hợp đồng\n" + customerCode +
                        "\n\nThông tin hóa đơn\n" + customerName +
                        "\nKỳ thanh toán: " + billPeriod +
                        "\n\nNgày thanh toán\n12 tháng 4, 2021" +
                        "\n\nMã giao dịch\n" + displayTransactionId
        );

        btnFinishBill.setOnClickListener(v -> {
            Intent intent = new Intent(BillSuccessActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private String formatMoneyNumber(double value) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(value);
    }
}