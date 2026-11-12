package com.example.tcbstudymobile.bill;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.BillPaymentRequest;
import com.example.tcbstudymobile.model.BillPaymentResponse;
import com.example.tcbstudymobile.model.OtpSendRequest;
import com.example.tcbstudymobile.model.OtpSendResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.network.ErrorParser;
import com.example.tcbstudymobile.storage.SessionManager;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillReviewActivity extends AppCompatActivity {

    private int billId;
    private String serviceName, providerName, customerCode, customerName, billPeriod;
    private double amount;

    private TextView txtBackBillReview, txtReviewProvider, txtReviewCustomerCode,
            txtReviewCustomerName, txtReviewPeriod, txtReviewAmount,
            txtPaymentBalance, txtReviewMessage;

    private Button btnPayNow;
    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_review);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        billId = getIntent().getIntExtra("billId", 0);
        serviceName = getIntent().getStringExtra("serviceName");
        providerName = getIntent().getStringExtra("providerName");
        customerCode = getIntent().getStringExtra("customerCode");
        customerName = getIntent().getStringExtra("customerName");
        billPeriod = getIntent().getStringExtra("billPeriod");
        amount = getIntent().getDoubleExtra("amount", 0);

        if (serviceName == null) serviceName = "hóa đơn";
        if (providerName == null) providerName = "Nhà cung cấp";
        if (customerCode == null) customerCode = "-";
        if (customerName == null) customerName = "-";
        if (billPeriod == null) billPeriod = "-";

        txtBackBillReview = findViewById(R.id.txtBackBillReview);
        txtReviewProvider = findViewById(R.id.txtReviewProvider);
        txtReviewCustomerCode = findViewById(R.id.txtReviewCustomerCode);
        txtReviewCustomerName = findViewById(R.id.txtReviewCustomerName);
        txtReviewPeriod = findViewById(R.id.txtReviewPeriod);
        txtReviewAmount = findViewById(R.id.txtReviewAmount);
        txtPaymentBalance = findViewById(R.id.txtPaymentBalance);
        txtReviewMessage = findViewById(R.id.txtReviewMessage);
        btnPayNow = findViewById(R.id.btnPayNow);

        txtBackBillReview.setOnClickListener(v -> finish());

        txtReviewProvider.setText(providerName);
        txtReviewCustomerCode.setText(customerCode);
        txtReviewCustomerName.setText(customerName);
        txtReviewPeriod.setText("Kỳ: " + billPeriod);
        txtReviewAmount.setText("VND " + formatMoneyNumber(amount));
        txtPaymentBalance.setText("Số dư: " + formatMoney(sessionManager.getBalance()));

        btnPayNow.setOnClickListener(v -> {
            if (billId <= 0) {
                txtReviewMessage.setText("Hóa đơn không hợp lệ. Vui lòng tra cứu lại mã khách hàng.");
                return;
            }
            if (amount > sessionManager.getBalance()) {
                txtReviewMessage.setText("Số dư không đủ để thanh toán");
                return;
            }
            requestBillPaymentOtp();
        });
    }

    private void requestBillPaymentOtp() {
        String token = sessionManager.getToken();
        String phone = sessionManager.getPhone();

        if (token == null || token.trim().isEmpty() || phone == null || phone.trim().isEmpty()) {
            txtReviewMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        btnPayNow.setEnabled(false);
        txtReviewMessage.setText("Đang gửi mã OTP xác thực thanh toán...");

        OtpSendRequest otpReq = new OtpSendRequest(phone, "BILL_PAYMENT", String.valueOf(billId));
        apiService.sendOtpAuthenticated("Bearer " + token, otpReq).enqueue(new Callback<OtpSendResponse>() {
            @Override
            public void onResponse(Call<OtpSendResponse> call, Response<OtpSendResponse> response) {
                btnPayNow.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    txtReviewMessage.setText("");
                    showOtpConfirmDialog();
                } else {
                    txtReviewMessage.setText(
                            ErrorParser.parse(response, "Không gửi được OTP thanh toán hóa đơn.")
                    );
                }
            }

            @Override
            public void onFailure(Call<OtpSendResponse> call, Throwable t) {
                btnPayNow.setEnabled(true);
                txtReviewMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void showOtpConfirmDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 12);

        EditText edtOtp = new EditText(this);
        edtOtp.setHint("Nhập 6 chữ số OTP");
        edtOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtOtp.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        container.addView(edtOtp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Xác nhận thanh toán hóa đơn")
                .setMessage("Nhà cung cấp: " + providerName
                        + "\nMã KH: " + customerCode
                        + "\nSố tiền: " + formatMoney(amount)
                        + "\n\nVui lòng nhập mã OTP 6 số đã gửi về điện thoại.")
                .setView(container)
                .setCancelable(false)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Thanh toán", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnConfirm.setOnClickListener(v -> {
                String otp = edtOtp.getText().toString().trim();
                if (!otp.matches("^[0-9]{6}$")) {
                    edtOtp.setError("Mã OTP phải gồm 6 chữ số");
                    return;
                }
                payBillWithOtp(dialog, otp);
            });
        });

        dialog.show();
    }

    private void payBillWithOtp(AlertDialog dialog, String otpCode) {
        String token = sessionManager.getToken();
        btnPayNow.setEnabled(false);
        txtReviewMessage.setText("Đang xử lý thanh toán...");

        BillPaymentRequest request = new BillPaymentRequest(billId, otpCode);
        apiService.payBill("Bearer " + token, request).enqueue(new Callback<BillPaymentResponse>() {
            @Override
            public void onResponse(Call<BillPaymentResponse> call, Response<BillPaymentResponse> response) {
                btnPayNow.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    dialog.dismiss();
                    BillPaymentResponse billResponse = response.body();
                    sessionManager.updateBalance(billResponse.getBalance());

                    Intent intent = new Intent(BillReviewActivity.this, BillSuccessActivity.class);
                    intent.putExtra("serviceName", serviceName);
                    intent.putExtra("providerName", providerName);
                    intent.putExtra("customerCode", customerCode);
                    intent.putExtra("customerName", customerName);
                    intent.putExtra("billPeriod", billPeriod);
                    intent.putExtra("amount", amount);
                    intent.putExtra("transactionId", billResponse.getTransactionId());
                    startActivity(intent);
                    finish();
                } else {
                    String err = ErrorParser.parse(response, "Thanh toán thất bại. Vui lòng kiểm tra mã OTP hoặc số dư.");
                    txtReviewMessage.setText(err);
                    dialog.setMessage("Mã KH: " + customerCode + "\nLỗi: " + err);
                }
            }

            @Override
            public void onFailure(Call<BillPaymentResponse> call, Throwable t) {
                btnPayNow.setEnabled(true);
                txtReviewMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private String formatMoney(double value) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(value) + " VND";
    }

    private String formatMoneyNumber(double value) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(value);
    }
}
