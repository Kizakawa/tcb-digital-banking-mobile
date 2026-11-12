package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class AccountProductActivity extends AppCompatActivity {

    private TextView txtBackProduct;
    private LinearLayout cardStandardAccount, cardMerchantAccount;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_product);

        txtBackProduct = findViewById(R.id.txtBackProduct);
        cardStandardAccount = findViewById(R.id.cardStandardAccount);
        cardMerchantAccount = findViewById(R.id.cardMerchantAccount);

        openAccountManager = new OpenAccountManager(this);

        txtBackProduct.setOnClickListener(v -> finish());

        cardStandardAccount.setOnClickListener(v -> {
            openAccountManager.setAccountProduct("Tài khoản cá nhân tiêu chuẩn");
            openIntro();
        });

        cardMerchantAccount.setOnClickListener(v -> {
            openAccountManager.setAccountProduct("Tài khoản tối ưu cho nhà bán hàng");
            openIntro();
        });
    }

    private void openIntro() {
        Intent intent = new Intent(AccountProductActivity.this, OnlineOpenIntroActivity.class);
        startActivity(intent);
    }
}