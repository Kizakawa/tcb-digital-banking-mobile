package com.example.tcbstudymobile.statistics;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.SummaryData;
import com.example.tcbstudymobile.model.SummaryResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.storage.SessionManager;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatisticsActivity extends AppCompatActivity {

    private TextView txtBackStatistics;
    private TextView txtSummaryBalance;
    private TextView txtTotalMoneyIn, txtTotalMoneyOut, txtTransactionCount;
    private TextView txtTotalIncome, txtTotalTransferIn, txtTotalExpense,
            txtTotalTransferOut, txtTotalBillPayment;
    private TextView txtStatisticsMessage;

    private ApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        txtBackStatistics = findViewById(R.id.txtBackStatistics);
        txtSummaryBalance = findViewById(R.id.txtSummaryBalance);

        txtTotalMoneyIn = findViewById(R.id.txtTotalMoneyIn);
        txtTotalMoneyOut = findViewById(R.id.txtTotalMoneyOut);
        txtTransactionCount = findViewById(R.id.txtTransactionCount);

        txtTotalIncome = findViewById(R.id.txtTotalIncome);
        txtTotalTransferIn = findViewById(R.id.txtTotalTransferIn);
        txtTotalExpense = findViewById(R.id.txtTotalExpense);
        txtTotalTransferOut = findViewById(R.id.txtTotalTransferOut);
        txtTotalBillPayment = findViewById(R.id.txtTotalBillPayment);

        txtStatisticsMessage = findViewById(R.id.txtStatisticsMessage);

        apiService = ApiClient.getClient().create(ApiService.class);
        sessionManager = new SessionManager(this);

        txtBackStatistics.setOnClickListener(v -> finish());

        loadSummary();
    }

    private void loadSummary() {
        String token = sessionManager.getToken();

        if (token == null || token.isEmpty()) {
            txtStatisticsMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        txtStatisticsMessage.setText("Đang tải thống kê...");

        apiService.getSummary("Bearer " + token).enqueue(new Callback<SummaryResponse>() {
            @Override
            public void onResponse(Call<SummaryResponse> call, Response<SummaryResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SummaryResponse summaryResponse = response.body();

                    if ("success".equals(summaryResponse.getStatus()) && summaryResponse.getData() != null) {
                        showSummary(summaryResponse.getData());
                        txtStatisticsMessage.setText("");
                    } else {
                        txtStatisticsMessage.setText("Không tải được dữ liệu thống kê.");
                    }
                } else {
                    txtStatisticsMessage.setText("Không tải được dữ liệu thống kê.");
                }
            }

            @Override
            public void onFailure(Call<SummaryResponse> call, Throwable t) {
                txtStatisticsMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void showSummary(SummaryData data) {
        sessionManager.updateBalance(data.getBalance());

        txtSummaryBalance.setText(formatMoney(data.getBalance()));

        txtTotalMoneyIn.setText("Tổng tiền vào: " + formatMoney(data.getTotalMoneyIn()));
        txtTotalMoneyOut.setText("Tổng tiền ra: " + formatMoney(data.getTotalMoneyOut()));
        txtTransactionCount.setText("Số giao dịch: " + data.getTransactionCount());

        txtTotalIncome.setText("Khoản thu: " + formatMoney(data.getTotalIncome()));
        txtTotalTransferIn.setText("Tiền nhận: " + formatMoney(data.getTotalTransferIn()));
        txtTotalExpense.setText("Khoản chi: " + formatMoney(data.getTotalExpense()));
        txtTotalTransferOut.setText("Chuyển tiền đi: " + formatMoney(data.getTotalTransferOut()));
        txtTotalBillPayment.setText("Thanh toán hóa đơn: " + formatMoney(data.getTotalBillPayment()));
    }

    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VND";
    }
}