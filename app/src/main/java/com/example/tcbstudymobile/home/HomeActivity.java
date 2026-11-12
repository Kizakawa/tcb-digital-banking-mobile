package com.example.tcbstudymobile.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.account.AccountInfoActivity;
import com.example.tcbstudymobile.auth.MainActivity;
import com.example.tcbstudymobile.bill.BillServiceActivity;
import com.example.tcbstudymobile.history.HistoryActivity;
import com.example.tcbstudymobile.model.AccountResponse;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.User;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.statistics.StatisticsActivity;
import com.example.tcbstudymobile.storage.SessionManager;
import com.example.tcbstudymobile.transfer.TransferActivity;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeActivity extends AppCompatActivity {

    private TextView txtWelcome;
    private TextView txtBalance;
    private TextView txtHomeMessage;
    private TextView txtAccountNumberHome;
    private TextView txtAccountTypeHome;

    private Button btnAccountInfo;
    private Button btnTransfer;
    private Button btnBill;
    private Button btnHistory;
    private Button btnStatistics;
    private Button btnLogout;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        initViews();
        setupEvents();
        showLocalSessionData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAccountFromServer();
    }

    private void initViews() {
        txtWelcome = findViewById(R.id.txtWelcome);
        txtBalance = findViewById(R.id.txtBalance);
        txtHomeMessage = findViewById(R.id.txtHomeMessage);
        txtAccountNumberHome = findViewById(R.id.txtAccountNumberHome);
        txtAccountTypeHome = findViewById(R.id.txtAccountTypeHome);

        btnAccountInfo = findViewById(R.id.btnAccountInfo);
        btnTransfer = findViewById(R.id.btnTransfer);
        btnBill = findViewById(R.id.btnBill);
        btnHistory = findViewById(R.id.btnHistory);
        btnStatistics = findViewById(R.id.btnStatistics);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupEvents() {
        btnAccountInfo.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, AccountInfoActivity.class);
            startActivity(intent);
        });

        btnTransfer.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TransferActivity.class);
            startActivity(intent);
        });

        btnBill.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, BillServiceActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        btnStatistics.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void showLocalSessionData() {
        String fullName = sessionManager.getFullName();
        double balance = sessionManager.getBalance();

        if (fullName == null || fullName.trim().isEmpty()) {
            txtWelcome.setText("Xin chào");
        } else {
            txtWelcome.setText("Xin chào, " + fullName);
        }

        txtBalance.setText(formatMoney(balance));
        txtAccountNumberHome.setText("Số tài khoản: Đang tải...");
        txtAccountTypeHome.setText("Loại tài khoản: Đang tải...");
    }

    private void loadAccountFromServer() {
        String token = sessionManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            txtHomeMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        txtHomeMessage.setText("");

        apiService.getAccount("Bearer " + token).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountResponse accountResponse = response.body();

                    if ("success".equals(accountResponse.getStatus()) && accountResponse.getData() != null) {
                        updateHomeWithUser(accountResponse.getData());
                    } else {
                        txtHomeMessage.setText("Không lấy được thông tin tài khoản");
                    }
                } else {
                    txtHomeMessage.setText("Không tải được dữ liệu tài khoản. Vui lòng đăng nhập lại.");
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable t) {
                txtHomeMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void updateHomeWithUser(User user) {
        String fullName = user.getFullName();
        String phone = user.getPhone();
        double balance = user.getBalance();

        if (fullName == null || fullName.trim().isEmpty()) {
            txtWelcome.setText("Xin chào");
        } else {
            txtWelcome.setText("Xin chào, " + fullName);
        }

        txtBalance.setText(formatMoney(balance));

        txtAccountNumberHome.setText("Số tài khoản: " + safeText(user.getAccountNumber()));
        txtAccountTypeHome.setText("Loại tài khoản: " + safeText(user.getAccountType()));

        sessionManager.saveLoginSession(
                sessionManager.getToken(),
                fullName,
                phone,
                balance
        );
    }

    private void logout() {
        String token = sessionManager.getToken();
        btnLogout.setEnabled(false);

        if (token != null && !token.trim().isEmpty()) {
            apiService.logout("Bearer " + token).enqueue(new Callback<BasicResponse>() {
                @Override
                public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                    completeLocalLogout();
                }

                @Override
                public void onFailure(Call<BasicResponse> call, Throwable t) {
                    completeLocalLogout();
                }
            });
        } else {
            completeLocalLogout();
        }
    }

    private void completeLocalLogout() {
        sessionManager.clearSession();

        Toast.makeText(
                HomeActivity.this,
                "Đã đăng xuất an toàn",
                Toast.LENGTH_SHORT
        ).show();

        Intent intent = new Intent(HomeActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
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