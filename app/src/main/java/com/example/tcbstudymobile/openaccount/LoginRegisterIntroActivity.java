package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

import java.text.NumberFormat;
import java.util.Locale;

public class LoginRegisterIntroActivity extends AppCompatActivity {

    private TextView txtBackLoginIntro, txtMaskedPhoneIntro,
            txtAccountNumberPreview, txtAccountFeePreview;

    private Button btnContinueLoginIntro, btnChangeLoginName;
    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_register_intro);

        txtBackLoginIntro = findViewById(R.id.txtBackLoginIntro);
        txtMaskedPhoneIntro = findViewById(R.id.txtMaskedPhoneIntro);
        txtAccountNumberPreview = findViewById(R.id.txtAccountNumberPreview);
        txtAccountFeePreview = findViewById(R.id.txtAccountFeePreview);
        btnContinueLoginIntro = findViewById(R.id.btnContinueLoginIntro);
        btnChangeLoginName = findViewById(R.id.btnChangeLoginName);

        openAccountManager = new OpenAccountManager(this);

        String phone = openAccountManager.getPhone();

        /*
         * Không được lưu username = "******0002".
         * "******0002" chỉ dùng để hiển thị, không được gửi lên backend.
         *
         * Username backend chỉ nhận:
         * - 3 đến 30 ký tự
         * - chữ, số, dấu chấm, gạch dưới
         *
         * Vì vậy username mặc định nên là user + 4 số cuối SĐT + vài số phụ.
         */
        String defaultUsername = generateDefaultUsername(phone);

        openAccountManager.setUsername(defaultUsername);

        txtMaskedPhoneIntro.setText(maskUsernameForDisplay(defaultUsername));
        txtAccountNumberPreview.setText("Số tài khoản: " + openAccountManager.getAccountNumber());
        txtAccountFeePreview.setText("Phí số tài khoản: " + formatMoney(openAccountManager.getAccountFee()));

        txtBackLoginIntro.setOnClickListener(v -> finish());

        btnContinueLoginIntro.setOnClickListener(v -> {
            Intent intent = new Intent(LoginRegisterIntroActivity.this, PasswordCreateActivity.class);
            startActivity(intent);
        });

        btnChangeLoginName.setOnClickListener(v -> {
            Intent intent = new Intent(LoginRegisterIntroActivity.this, UsernameCreateActivity.class);
            startActivity(intent);
        });
    }

    private String generateDefaultUsername(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "user" + (System.currentTimeMillis() % 100000);
        }

        String digitsOnly = phone.replaceAll("[^0-9]", "");

        if (digitsOnly.length() >= 4) {
            String last4 = digitsOnly.substring(digitsOnly.length() - 4);

            /*
             * Dùng thêm 2 số cuối của thời gian để giảm khả năng trùng username
             * khi test nhiều lần với dữ liệu tương tự.
             */
            String suffix = String.valueOf(System.currentTimeMillis() % 100);

            return "user" + last4 + suffix;
        }

        return "user" + (System.currentTimeMillis() % 100000);
    }

    private String maskUsernameForDisplay(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "******";
        }

        String value = username.trim();

        if (value.length() <= 4) {
            return "****";
        }

        return "******" + value.substring(value.length() - 4);
    }

    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VND";
    }
}