package com.example.tcbstudymobile.transfer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.home.HomeActivity;

import java.text.NumberFormat;
import java.util.Locale;

public class TransferSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_RECEIVER_NAME = "receiverName";
    public static final String EXTRA_RECEIVER_ACCOUNT_NUMBER = "receiverAccountNumber";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_CONTENT = "content";
    public static final String EXTRA_NEW_BALANCE = "newBalance";

    private TextView txtSuccessReceiverName;
    private TextView txtSuccessReceiverAccount;
    private TextView txtSuccessAmount;
    private TextView txtSuccessContent;
    private TextView txtSuccessBalance;

    private Button btnDoneTransfer;
    private Button btnTransferAgain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_success);

        txtSuccessReceiverName = findViewById(R.id.txtSuccessReceiverName);
        txtSuccessReceiverAccount = findViewById(R.id.txtSuccessReceiverAccount);
        txtSuccessAmount = findViewById(R.id.txtSuccessAmount);
        txtSuccessContent = findViewById(R.id.txtSuccessContent);
        txtSuccessBalance = findViewById(R.id.txtSuccessBalance);

        btnDoneTransfer = findViewById(R.id.btnDoneTransfer);
        btnTransferAgain = findViewById(R.id.btnTransferAgain);

        showTransferResult();

        btnDoneTransfer.setOnClickListener(v -> goHome());

        btnTransferAgain.setOnClickListener(v -> transferAgain());
    }

    private void showTransferResult() {
        Intent intent = getIntent();

        String receiverName = intent.getStringExtra(EXTRA_RECEIVER_NAME);
        String receiverAccountNumber = intent.getStringExtra(EXTRA_RECEIVER_ACCOUNT_NUMBER);
        String content = intent.getStringExtra(EXTRA_CONTENT);

        double amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0);
        double newBalance = intent.getDoubleExtra(EXTRA_NEW_BALANCE, 0);

        txtSuccessReceiverName.setText(safeText(receiverName));
        txtSuccessReceiverAccount.setText(safeText(receiverAccountNumber));
        txtSuccessAmount.setText(formatMoney(amount));
        txtSuccessContent.setText(safeText(content));
        txtSuccessBalance.setText(formatMoney(newBalance));
    }

    private void goHome() {
        Intent intent = new Intent(TransferSuccessActivity.this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void transferAgain() {
        Intent intent = new Intent(TransferSuccessActivity.this, TransferActivity.class);
        startActivity(intent);
        finish();
    }

    private String safeText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }

    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VND";
    }
}