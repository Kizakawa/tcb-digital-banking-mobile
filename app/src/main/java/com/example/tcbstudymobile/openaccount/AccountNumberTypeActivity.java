package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

import java.util.Random;

public class AccountNumberTypeActivity extends AppCompatActivity {

    private TextView txtBackAccountType;
    private LinearLayout cardNormalAccount, cardBeautifulAccount;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_number_type);

        txtBackAccountType = findViewById(R.id.txtBackAccountType);
        cardNormalAccount = findViewById(R.id.cardNormalAccount);
        cardBeautifulAccount = findViewById(R.id.cardBeautifulAccount);

        openAccountManager = new OpenAccountManager(this);

        txtBackAccountType.setOnClickListener(v -> finish());

        cardNormalAccount.setOnClickListener(v -> {
            openAccountManager.setAccountType("Tài khoản thường");
            openAccountManager.setAccountNumber(generateNormalAccountNumber());
            openAccountManager.setAccountFee(0);

            Intent intent = new Intent(AccountNumberTypeActivity.this, LoginRegisterIntroActivity.class);
            startActivity(intent);
        });

        cardBeautifulAccount.setOnClickListener(v -> {
            openAccountManager.setAccountType("Tài khoản số đẹp");

            Intent intent = new Intent(AccountNumberTypeActivity.this, BeautifulAccountSetupActivity.class);
            startActivity(intent);
        });
    }

    private String generateNormalAccountNumber() {
        Random random = new Random();
        int randomPart = 10000000 + random.nextInt(90000000);
        return "1903" + randomPart;
    }
}