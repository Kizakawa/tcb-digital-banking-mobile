package com.example.tcbstudymobile.transfer;

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
import com.example.tcbstudymobile.model.ReceiverLookupResponse;
import com.example.tcbstudymobile.model.TransferConfirmRequest;
import com.example.tcbstudymobile.model.TransferInitiateRequest;
import com.example.tcbstudymobile.model.TransferInitiateResponse;
import com.example.tcbstudymobile.model.TransferResponse;
import com.example.tcbstudymobile.model.User;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.network.ErrorParser;
import com.example.tcbstudymobile.storage.SessionManager;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TransferActivity extends AppCompatActivity {

    private TextView txtBackTransfer;
    private TextView txtTransferMessage;
    private TextView txtReceiverInfo;

    private EditText edtReceiverAccountNumber;
    private EditText edtTransferAmount;
    private EditText edtTransferContent;

    private Button btnCheckReceiver;
    private Button btnTransferMoney;

    private SessionManager sessionManager;
    private ApiService apiService;

    private User selectedReceiver;
    private String checkedAccountNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);

        initViews();

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        txtBackTransfer.setOnClickListener(v -> finish());
        btnCheckReceiver.setOnClickListener(v -> checkReceiver());
        btnTransferMoney.setOnClickListener(v -> validateAndConfirmTransfer());
    }

    private void initViews() {
        txtBackTransfer = findViewById(R.id.txtBackTransfer);
        txtTransferMessage = findViewById(R.id.txtTransferMessage);
        txtReceiverInfo = findViewById(R.id.txtReceiverInfo);

        edtReceiverAccountNumber = findViewById(R.id.edtReceiverAccountNumber);
        edtTransferAmount = findViewById(R.id.edtTransferAmount);
        edtTransferContent = findViewById(R.id.edtTransferContent);

        btnCheckReceiver = findViewById(R.id.btnCheckReceiver);
        btnTransferMoney = findViewById(R.id.btnTransferMoney);
    }

    private void checkReceiver() {
        String accountNumber = edtReceiverAccountNumber.getText().toString().trim();

        selectedReceiver = null;
        checkedAccountNumber = "";
        txtReceiverInfo.setText("");

        if (accountNumber.isEmpty()) {
            txtTransferMessage.setText("Vui lòng nhập số tài khoản người nhận");
            return;
        }

        if (!accountNumber.matches("^[0-9]{6,14}$")) {
            txtTransferMessage.setText("Số tài khoản phải gồm 6 đến 14 chữ số");
            return;
        }

        String token = sessionManager.getToken();

        if (token == null || token.trim().isEmpty()) {
            txtTransferMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        btnCheckReceiver.setEnabled(false);
        txtTransferMessage.setText("Đang kiểm tra người nhận...");

        apiService.lookupReceiver("Bearer " + token, accountNumber)
                .enqueue(new Callback<ReceiverLookupResponse>() {
                    @Override
                    public void onResponse(Call<ReceiverLookupResponse> call, Response<ReceiverLookupResponse> response) {
                        btnCheckReceiver.setEnabled(true);

                        if (response.isSuccessful() && response.body() != null) {
                            ReceiverLookupResponse lookupResponse = response.body();

                            if ("success".equals(lookupResponse.getStatus()) && lookupResponse.getData() != null) {
                                selectedReceiver = lookupResponse.getData();
                                checkedAccountNumber = accountNumber;

                                txtReceiverInfo.setText(
                                        "Người nhận: " + safeText(selectedReceiver.getFullName()) +
                                                "\nSố tài khoản: " + safeText(selectedReceiver.getAccountNumber()) +
                                                "\nLoại tài khoản: " + safeText(selectedReceiver.getAccountType())
                                );

                                txtTransferMessage.setText("Đã tìm thấy người nhận.");
                            } else {
                                txtTransferMessage.setText("Không tìm thấy người nhận.");
                            }
                        } else {
                            txtTransferMessage.setText(
                                    ErrorParser.parse(response, "Không tìm thấy số tài khoản người nhận.")
                            );
                        }
                    }

                    @Override
                    public void onFailure(Call<ReceiverLookupResponse> call, Throwable t) {
                        btnCheckReceiver.setEnabled(true);
                        txtTransferMessage.setText("Không kết nối được server: " + t.getMessage());
                    }
                });
    }

    private void validateAndConfirmTransfer() {
        String accountNumber = edtReceiverAccountNumber.getText().toString().trim();
        String amountText = edtTransferAmount.getText().toString().trim();
        String content = edtTransferContent.getText().toString().trim();

        if (accountNumber.isEmpty()) {
            txtTransferMessage.setText("Vui lòng nhập số tài khoản người nhận");
            return;
        }

        if (selectedReceiver == null || !accountNumber.equals(checkedAccountNumber)) {
            txtTransferMessage.setText("Vui lòng kiểm tra người nhận trước khi chuyển tiền");
            return;
        }

        if (amountText.isEmpty()) {
            txtTransferMessage.setText("Vui lòng nhập số tiền");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (Exception e) {
            txtTransferMessage.setText("Số tiền không hợp lệ");
            return;
        }

        if (amount < 1000) {
            txtTransferMessage.setText("Số tiền chuyển tối thiểu là 1.000 VND");
            return;
        }

        if (content.isEmpty()) {
            content = "Chuyển tiền nội bộ";
        }

        showConfirmDialog(accountNumber, amount, content);
    }

    private void showConfirmDialog(String accountNumber, double amount, String content) {
        String message =
                "Người nhận: " + safeText(selectedReceiver.getFullName()) +
                        "\nSố tài khoản: " + accountNumber +
                        "\nSố tiền: " + formatMoney(amount) +
                        "\nNội dung: " + content +
                        "\n\nNhấn Tiếp tục để nhận mã OTP xác thực giao dịch.";

        new AlertDialog.Builder(this)
                .setTitle("Xác nhận chuyển tiền")
                .setMessage(message)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tiếp tục", (dialog, which) ->
                        initiateTransfer(accountNumber, amount, content)
                )
                .show();
    }

    private void initiateTransfer(String accountNumber, double amount, String content) {
        String token = sessionManager.getToken();
        if (token == null || token.trim().isEmpty()) {
            txtTransferMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        btnTransferMoney.setEnabled(false);
        txtTransferMessage.setText("Đang khởi tạo giao dịch và gửi mã OTP...");

        TransferInitiateRequest request = new TransferInitiateRequest(
                accountNumber,
                amount,
                content,
                UUID.randomUUID().toString()
        );

        apiService.initiateTransfer("Bearer " + token, request).enqueue(new Callback<TransferInitiateResponse>() {
            @Override
            public void onResponse(Call<TransferInitiateResponse> call, Response<TransferInitiateResponse> response) {
                btnTransferMoney.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())
                        && response.body().getData() != null) {
                    String refCode = response.body().getData().getReferenceCode();
                    txtTransferMessage.setText("Đã gửi mã OTP cho giao dịch " + refCode);
                    showOtpInputDialog(refCode, accountNumber, amount, content);
                } else {
                    txtTransferMessage.setText(
                            ErrorParser.parse(response, "Không khởi tạo được giao dịch chuyển tiền.")
                    );
                }
            }

            @Override
            public void onFailure(Call<TransferInitiateResponse> call, Throwable t) {
                btnTransferMoney.setEnabled(true);
                txtTransferMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void showOtpInputDialog(String referenceCode, String accountNumber, double amount, String content) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(48, 24, 48, 12);

        EditText edtOtp = new EditText(this);
        edtOtp.setHint("Nhập 6 chữ số OTP");
        edtOtp.setInputType(InputType.TYPE_CLASS_NUMBER);
        edtOtp.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
        container.addView(edtOtp);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Xác thực OTP giao dịch")
                .setMessage("Mã tham chiếu: " + referenceCode
                        + "\nSố tiền: " + formatMoney(amount)
                        + "\nVui lòng nhập mã OTP 6 số đã gửi về điện thoại.")
                .setView(container)
                .setCancelable(false)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xác nhận", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnConfirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnConfirm.setOnClickListener(v -> {
                String otp = edtOtp.getText().toString().trim();
                if (!otp.matches("^[0-9]{6}$")) {
                    edtOtp.setError("Mã OTP phải gồm 6 chữ số");
                    return;
                }
                confirmTransferWithOtp(dialog, referenceCode, otp, accountNumber, amount, content);
            });
        });

        dialog.show();
    }

    private void confirmTransferWithOtp(
            AlertDialog otpDialog,
            String referenceCode,
            String otpCode,
            String accountNumber,
            double amount,
            String content
    ) {
        String token = sessionManager.getToken();
        btnTransferMoney.setEnabled(false);
        txtTransferMessage.setText("Đang xác thực OTP và chuyển tiền...");

        TransferConfirmRequest confirmReq = new TransferConfirmRequest(referenceCode, otpCode);
        apiService.confirmTransfer("Bearer " + token, confirmReq).enqueue(new Callback<TransferResponse>() {
            @Override
            public void onResponse(Call<TransferResponse> call, Response<TransferResponse> response) {
                btnTransferMoney.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    otpDialog.dismiss();
                    double newBalance = response.body().getBalance();
                    sessionManager.updateBalance(newBalance);
                    openSuccessScreen(accountNumber, amount, content, newBalance);
                } else {
                    String err = ErrorParser.parse(response, "Xác thực OTP thất bại.");
                    txtTransferMessage.setText(err);
                    otpDialog.setMessage("Mã tham chiếu: " + referenceCode + "\nLỗi: " + err);
                }
            }

            @Override
            public void onFailure(Call<TransferResponse> call, Throwable t) {
                btnTransferMoney.setEnabled(true);
                txtTransferMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }

    private void openSuccessScreen(String accountNumber, double amount, String content, double newBalance) {
        String receiverName = selectedReceiver != null ? selectedReceiver.getFullName() : "";

        Intent intent = new Intent(TransferActivity.this, TransferSuccessActivity.class);
        intent.putExtra(TransferSuccessActivity.EXTRA_RECEIVER_NAME, receiverName);
        intent.putExtra(TransferSuccessActivity.EXTRA_RECEIVER_ACCOUNT_NUMBER, accountNumber);
        intent.putExtra(TransferSuccessActivity.EXTRA_AMOUNT, amount);
        intent.putExtra(TransferSuccessActivity.EXTRA_CONTENT, content);
        intent.putExtra(TransferSuccessActivity.EXTRA_NEW_BALANCE, newBalance);

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
