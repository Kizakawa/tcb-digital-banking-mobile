package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.auth.MainActivity;

public class OpenAccountEntryActivity extends AppCompatActivity {

    private Button btnNewCustomer, btnLoginExisting;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_account_entry);

        btnNewCustomer = findViewById(R.id.btnNewCustomer);
        btnLoginExisting = findViewById(R.id.btnLoginExisting);

        openAccountManager = new OpenAccountManager(this);

        btnNewCustomer.setOnClickListener(v -> {
            openAccountManager.clearAll();

            Intent intent = new Intent(OpenAccountEntryActivity.this, AccountProductActivity.class);
            startActivity(intent);
        });

        btnLoginExisting.setOnClickListener(v -> {
            Intent intent = new Intent(OpenAccountEntryActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
}