package com.example.tcbstudymobile.history;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.home.HomeActivity;
import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.TransactionItem;
import com.example.tcbstudymobile.model.TransactionListResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.storage.SessionManager;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HistoryActivity extends AppCompatActivity {

    private TextView txtHistoryMessage, txtBackHomeFromHistory;
    private LinearLayout layoutTransactionList;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        txtHistoryMessage = findViewById(R.id.txtHistoryMessage);
        txtBackHomeFromHistory = findViewById(R.id.txtBackHomeFromHistory);
        layoutTransactionList = findViewById(R.id.layoutTransactionList);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        txtBackHomeFromHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HistoryActivity.this, HomeActivity.class);
            startActivity(intent);
            finish();
        });

        loadTransactions();
    }

    private void loadTransactions() {
        String token = sessionManager.getToken();

        if (token == null || token.isEmpty()) {
            txtHistoryMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        txtHistoryMessage.setText("Đang tải lịch sử giao dịch...");

        apiService.getTransactions("Bearer " + token).enqueue(new Callback<TransactionListResponse>() {
            @Override
            public void onResponse(Call<TransactionListResponse> call, Response<TransactionListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    TransactionListResponse transactionResponse = response.body();

                    if ("success".equals(transactionResponse.getStatus())) {
                        List<TransactionItem> transactions = transactionResponse.getData();

                        layoutTransactionList.removeAllViews();

                        if (transactions == null || transactions.isEmpty()) {
                            txtHistoryMessage.setText("Chưa có giao dịch nào.");
                            return;
                        }

                        txtHistoryMessage.setText("Tổng số giao dịch: " + transactionResponse.getTotal());

                        for (TransactionItem item : transactions) {
                            addTransactionView(item);
                        }
                    } else {
                        txtHistoryMessage.setText("Không tải được lịch sử giao dịch.");
                    }
                } else {
                    txtHistoryMessage.setText("Không tải được lịch sử giao dịch.");
                }
            }

            @Override
            public void onFailure(Call<TransactionListResponse> call, Throwable t) {
                txtHistoryMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void addTransactionView(TransactionItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 20, 24, 20);
        card.setBackgroundColor(Color.WHITE);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 18);
        card.setLayoutParams(cardParams);

        TextView txtType = new TextView(this);
        txtType.setText(getTypeText(item.getType()));
        txtType.setTextSize(17);
        txtType.setTextColor(Color.parseColor("#222222"));
        txtType.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView txtAmount = new TextView(this);
        txtAmount.setText(getAmountPrefix(item.getType()) + formatMoney(item.getAmount()));
        txtAmount.setTextSize(20);
        txtAmount.setTextColor(getAmountColor(item.getType()));
        txtAmount.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView txtContent = new TextView(this);
        txtContent.setText("Nội dung: " + safeText(item.getContent()));
        txtContent.setTextSize(14);
        txtContent.setTextColor(Color.parseColor("#555555"));

        TextView txtReceiver = new TextView(this);
        txtReceiver.setText("Đối tác: " + safeText(item.getReceiverPhone()));
        txtReceiver.setTextSize(14);
        txtReceiver.setTextColor(Color.parseColor("#555555"));

        TextView txtRef = new TextView(this);
        txtRef.setText("Mã GD: " + safeText(item.getReferenceCode())
                + (item.getBalanceAfter() != null ? " | Số dư sau GD: " + formatMoney(item.getBalanceAfter()) : ""));
        txtRef.setTextSize(13);
        txtRef.setTextColor(Color.parseColor("#555555"));

        TextView txtStatus = new TextView(this);
        txtStatus.setText("Trạng thái: " + safeText(item.getStatus()));
        txtStatus.setTextSize(14);
        txtStatus.setTextColor(Color.parseColor("#555555"));

        TextView txtDate = new TextView(this);
        txtDate.setText("Thời gian: " + safeText(item.getCreatedAt()));
        txtDate.setTextSize(14);
        txtDate.setTextColor(Color.parseColor("#777777"));

        card.addView(txtType);
        card.addView(txtAmount);
        card.addView(txtContent);
        card.addView(txtReceiver);
        card.addView(txtRef);
        card.addView(txtStatus);
        card.addView(txtDate);

        layoutTransactionList.addView(card);
    }

    private String getTypeText(String type) {
        if (type == null) return "Giao dịch";

        switch (type) {
            case "INCOME":
                return "Khoản thu";
            case "EXPENSE":
                return "Khoản chi";
            case "TRANSFER_OUT":
                return "Chuyển tiền đi";
            case "TRANSFER_IN":
                return "Nhận tiền";
            case "BILL_PAYMENT":
                return "Thanh toán hóa đơn";
            default:
                return type;
        }
    }

    private String getAmountPrefix(String type) {
        if ("EXPENSE".equals(type)
                || "TRANSFER_OUT".equals(type)
                || "BILL_PAYMENT".equals(type)) {
            return "- ";
        }

        return "+ ";
    }

    private int getAmountColor(String type) {
        if ("EXPENSE".equals(type)
                || "TRANSFER_OUT".equals(type)
                || "BILL_PAYMENT".equals(type)) {
            return Color.parseColor("#D71920");
        }

        return Color.parseColor("#168038");
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