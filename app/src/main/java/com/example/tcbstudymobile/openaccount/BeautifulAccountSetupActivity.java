package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.CheckExistResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BeautifulAccountSetupActivity extends AppCompatActivity {

    private TextView txtBackBeautifulAccount, txtSuggest6868, txtSuggest9999,
            txtSuggest123456, txtBeautifulFee, txtBeautifulMessage;

    private EditText edtBeautifulAccountNumber;
    private Button btnContinueBeautifulAccount;

    private OpenAccountManager openAccountManager;
    private ApiService apiService;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable accountCheckRunnable;

    private boolean isAccountNumberAvailable = false;
    private String lastCheckedAccountNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beautiful_account_setup);

        txtBackBeautifulAccount = findViewById(R.id.txtBackBeautifulAccount);
        txtSuggest6868 = findViewById(R.id.txtSuggest6868);
        txtSuggest9999 = findViewById(R.id.txtSuggest9999);
        txtSuggest123456 = findViewById(R.id.txtSuggest123456);
        txtBeautifulFee = findViewById(R.id.txtBeautifulFee);
        txtBeautifulMessage = findViewById(R.id.txtBeautifulMessage);
        edtBeautifulAccountNumber = findViewById(R.id.edtBeautifulAccountNumber);
        btnContinueBeautifulAccount = findViewById(R.id.btnContinueBeautifulAccount);

        openAccountManager = new OpenAccountManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        txtBackBeautifulAccount.setOnClickListener(v -> finish());

        txtSuggest6868.setOnClickListener(v -> setSuggestedNumber("19036868"));
        txtSuggest9999.setOnClickListener(v -> setSuggestedNumber("19039999"));
        txtSuggest123456.setOnClickListener(v -> setSuggestedNumber("1903123456"));

        edtBeautifulAccountNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                handleAccountNumberTyping(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnContinueBeautifulAccount.setOnClickListener(v -> validateAndContinue());
    }

    private void setSuggestedNumber(String number) {
        edtBeautifulAccountNumber.setText(number);
        edtBeautifulAccountNumber.setSelection(number.length());
    }

    private void handleAccountNumberTyping(String accountNumber) {
        isAccountNumberAvailable = false;
        lastCheckedAccountNumber = "";

        if (accountCheckRunnable != null) {
            handler.removeCallbacks(accountCheckRunnable);
        }

        updateFeePreview(accountNumber);

        if (accountNumber.isEmpty()) {
            txtBeautifulMessage.setText("");
            return;
        }

        if (!accountNumber.matches("^[0-9]{6,14}$")) {
            txtBeautifulMessage.setText("Số tài khoản phải gồm 6 đến 14 chữ số");
            return;
        }

        txtBeautifulMessage.setText("Đang kiểm tra số tài khoản...");

        accountCheckRunnable = () -> checkAccountNumberExists(accountNumber, false);
        handler.postDelayed(accountCheckRunnable, 500);
    }

    private void updateFeePreview(String accountNumber) {
        double fee = calculateBeautifulAccountFee(accountNumber);
        txtBeautifulFee.setText("Phí dự kiến: " + formatMoney(fee));
    }

    private void validateAndContinue() {
        String accountNumber = edtBeautifulAccountNumber.getText().toString().trim();

        if (accountNumber.isEmpty()) {
            txtBeautifulMessage.setText("Vui lòng nhập số tài khoản mong muốn");
            return;
        }

        if (!accountNumber.matches("^[0-9]{6,14}$")) {
            txtBeautifulMessage.setText("Số tài khoản phải gồm 6 đến 14 chữ số");
            return;
        }

        if (accountNumber.equals(lastCheckedAccountNumber) && isAccountNumberAvailable) {
            saveAndContinue(accountNumber);
            return;
        }

        checkAccountNumberExists(accountNumber, true);
    }

    private void checkAccountNumberExists(String accountNumber, boolean continueIfAvailable) {
        btnContinueBeautifulAccount.setEnabled(false);
        txtBeautifulMessage.setText("Đang kiểm tra số tài khoản...");

        apiService.checkAccountNumber(accountNumber).enqueue(new Callback<CheckExistResponse>() {
            @Override
            public void onResponse(Call<CheckExistResponse> call, Response<CheckExistResponse> response) {
                btnContinueBeautifulAccount.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    CheckExistResponse checkResponse = response.body();

                    lastCheckedAccountNumber = accountNumber;

                    if (checkResponse.isExists()) {
                        isAccountNumberAvailable = false;
                        txtBeautifulMessage.setText("Số tài khoản đã tồn tại. Vui lòng chọn số khác.");
                    } else {
                        isAccountNumberAvailable = true;
                        txtBeautifulMessage.setText("Số tài khoản có thể sử dụng.");

                        if (continueIfAvailable) {
                            saveAndContinue(accountNumber);
                        }
                    }
                } else {
                    isAccountNumberAvailable = false;
                    txtBeautifulMessage.setText("Không kiểm tra được số tài khoản. Vui lòng thử lại.");
                }
            }

            @Override
            public void onFailure(Call<CheckExistResponse> call, Throwable t) {
                btnContinueBeautifulAccount.setEnabled(true);
                isAccountNumberAvailable = false;
                txtBeautifulMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void saveAndContinue(String accountNumber) {
        double fee = calculateBeautifulAccountFee(accountNumber);

        openAccountManager.setAccountNumber(accountNumber);
        openAccountManager.setAccountFee(fee);

        Intent intent = new Intent(BeautifulAccountSetupActivity.this, LoginRegisterIntroActivity.class);
        startActivity(intent);
    }

    private double calculateBeautifulAccountFee(String number) {
        if (number == null || number.isEmpty()) return 0;

        if (number.matches(".*(9999|8888|6666).*")) return 100000;
        if (number.matches(".*(123456|234567|345678|654321).*")) return 150000;
        if (number.matches(".*(6868|8686).*")) return 50000;
        if (number.matches(".*(68|86)$")) return 20000;

        return 0;
    }

    private String formatMoney(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + " VND";
    }
}