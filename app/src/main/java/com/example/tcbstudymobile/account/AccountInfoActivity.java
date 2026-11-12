package com.example.tcbstudymobile.account;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.AccountResponse;
import com.example.tcbstudymobile.model.User;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.storage.SessionManager;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccountInfoActivity extends AppCompatActivity {

    private TextView txtBackAccountInfo;
    private TextView txtAccountInfoName;
    private TextView txtAccountInfoBalance;
    private TextView txtAccountInfoContent;
    private TextView txtAccountInfoMessage;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_info);

        txtBackAccountInfo = findViewById(R.id.txtBackAccountInfo);
        txtAccountInfoName = findViewById(R.id.txtAccountInfoName);
        txtAccountInfoBalance = findViewById(R.id.txtAccountInfoBalance);
        txtAccountInfoContent = findViewById(R.id.txtAccountInfoContent);
        txtAccountInfoMessage = findViewById(R.id.txtAccountInfoMessage);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        txtBackAccountInfo.setOnClickListener(v -> finish());

        loadAccountInfo();
    }

    private void loadAccountInfo() {
        String token = sessionManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            txtAccountInfoMessage.setText("Không tìm thấy phiên đăng nhập");
            return;
        }

        txtAccountInfoMessage.setText("Đang tải thông tin tài khoản...");

        apiService.getAccount("Bearer " + token).enqueue(new Callback<AccountResponse>() {
            @Override
            public void onResponse(Call<AccountResponse> call, Response<AccountResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AccountResponse accountResponse = response.body();

                    if ("success".equals(accountResponse.getStatus()) && accountResponse.getData() != null) {
                        showAccount(accountResponse.getData());
                        txtAccountInfoMessage.setText("");
                    } else {
                        txtAccountInfoMessage.setText("Không lấy được thông tin tài khoản");
                    }
                } else {
                    txtAccountInfoMessage.setText("Lỗi tải tài khoản. Vui lòng đăng nhập lại.");
                }
            }

            @Override
            public void onFailure(Call<AccountResponse> call, Throwable t) {
                txtAccountInfoMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void showAccount(User user) {
        txtAccountInfoName.setText(safeText(user.getFullName()));
        txtAccountInfoBalance.setText(formatMoney(user.getBalance()));

        txtAccountInfoContent.setText(
                "Số tài khoản:        " + safeText(user.getAccountNumber()) +
                        "\nLoại tài khoản:     " + safeText(user.getAccountType()) +
                        "\nPhí số tài khoản:   " + formatMoney(user.getAccountFee()) +
                        "\nTên đăng nhập:      " + safeText(user.getUsername()) +
                        "\nSố điện thoại:      " + safeText(user.getPhone()) +
                        "\nEmail:              " + safeText(user.getEmail()) +
                        "\nSố CCCD:            " + safeText(user.getCccdNumber()) +
                        "\nNghề nghiệp:        " + safeText(user.getJob()) +
                        "\nNgày tạo:           " + safeText(user.getCreatedAt())
        );
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