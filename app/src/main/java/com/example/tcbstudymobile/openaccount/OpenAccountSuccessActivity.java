package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.auth.MainActivity;

public class OpenAccountSuccessActivity extends AppCompatActivity {

    private TextView txtOpenSuccessInfo;
    private Button btnLoginAfterOpen;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_account_success);

        txtOpenSuccessInfo = findViewById(R.id.txtOpenSuccessInfo);
        btnLoginAfterOpen = findViewById(R.id.btnLoginAfterOpen);

        openAccountManager = new OpenAccountManager(this);

        txtOpenSuccessInfo.setText(
                "Số tài khoản:        " + openAccountManager.getAccountNumber() +
                        "\nChủ tài khoản:      " + openAccountManager.getFullName() +
                        "\nLoại tài khoản:     " + openAccountManager.getAccountType() +
                        "\nPhí số tài khoản:   " + openAccountManager.getAccountFee() + " VND" +
                        "\nTên đăng nhập:      " + openAccountManager.getUsername() +
                        "\nNgày kích hoạt:     10 Tháng 10, 2024"
        );

        btnLoginAfterOpen.setOnClickListener(v -> {
            Intent intent = new Intent(OpenAccountSuccessActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}