package com.example.tcbstudymobile.bill;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.BillLookupRequest;
import com.example.tcbstudymobile.model.BillLookupResponse;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;
import com.example.tcbstudymobile.network.ErrorParser;
import com.example.tcbstudymobile.storage.SessionManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BillCustomerCodeActivity extends AppCompatActivity {

    private String serviceCode, serviceName, providerCode, providerName, sampleCodeHint;
    private TextView txtCloseCustomerCode, txtBlueServiceName, txtProviderNameInput, txtBillInputMessage;
    private EditText edtCustomerCode;
    private Button btnContinueBill;

    private SessionManager sessionManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_customer_code);

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        serviceCode = getIntent().getStringExtra("serviceCode");
        serviceName = getIntent().getStringExtra("serviceName");
        providerCode = getIntent().getStringExtra("providerCode");
        providerName = getIntent().getStringExtra("providerName");
        sampleCodeHint = getIntent().getStringExtra("sampleCodeHint");

        if (serviceCode == null) serviceCode = "ELECTRICITY";
        if (serviceName == null) serviceName = "hóa đơn";
        if (providerCode == null) providerCode = "EVN_HN";
        if (providerName == null) providerName = "EVN Hà Nội";

        txtCloseCustomerCode = findViewById(R.id.txtCloseCustomerCode);
        txtBlueServiceName = findViewById(R.id.txtBlueServiceName);
        txtProviderNameInput = findViewById(R.id.txtProviderNameInput);
        txtBillInputMessage = findViewById(R.id.txtBillInputMessage);
        edtCustomerCode = findViewById(R.id.edtCustomerCode);
        btnContinueBill = findViewById(R.id.btnContinueBill);

        txtBlueServiceName.setText(serviceName);
        txtProviderNameInput.setText("cho " + providerName);

        if (sampleCodeHint != null && !sampleCodeHint.isEmpty()) {
            txtBillInputMessage.setTextColor(0xFF666666);
            txtBillInputMessage.setText("Mã KH mẫu để tra cứu: " + sampleCodeHint);
        }

        txtCloseCustomerCode.setOnClickListener(v -> finish());
        txtBlueServiceName.setOnClickListener(v -> finish());
        btnContinueBill.setOnClickListener(v -> lookupBillFromServer());
    }

    private void lookupBillFromServer() {
        String customerCode = edtCustomerCode.getText().toString().trim().toUpperCase();

        if (customerCode.isEmpty()) {
            txtBillInputMessage.setTextColor(0xFFD71920);
            txtBillInputMessage.setText("Vui lòng nhập mã hợp đồng / mã khách hàng");
            return;
        }

        String token = sessionManager.getToken();
        if (token == null || token.trim().isEmpty()) {
            txtBillInputMessage.setTextColor(0xFFD71920);
            txtBillInputMessage.setText("Phiên đăng nhập không hợp lệ. Vui lòng đăng nhập lại.");
            return;
        }

        btnContinueBill.setEnabled(false);
        txtBillInputMessage.setTextColor(0xFF555555);
        txtBillInputMessage.setText("Đang tra cứu hóa đơn...");

        BillLookupRequest request = new BillLookupRequest(serviceCode, providerCode, customerCode);
        apiService.lookupBills("Bearer " + token, request).enqueue(new Callback<BillLookupResponse>() {
            @Override
            public void onResponse(Call<BillLookupResponse> call, Response<BillLookupResponse> response) {
                btnContinueBill.setEnabled(true);

                if (response.isSuccessful() && response.body() != null
                        && "success".equals(response.body().getStatus())) {
                    List<BillLookupResponse.BillItem> bills = response.body().getData();
                    if (bills == null || bills.isEmpty()) {
                        txtBillInputMessage.setTextColor(0xFFD71920);
                        txtBillInputMessage.setText("Không tìm thấy hóa đơn cho mã KH này"
                                + (sampleCodeHint != null ? " (Gợi ý: " + sampleCodeHint + ")" : ""));
                        return;
                    }

                    BillLookupResponse.BillItem unpaidBill = null;
                    for (BillLookupResponse.BillItem b : bills) {
                        if ("UNPAID".equalsIgnoreCase(b.getStatus())) {
                            unpaidBill = b;
                            break;
                        }
                    }

                    if (unpaidBill == null) {
                        txtBillInputMessage.setTextColor(0xFF168038);
                        txtBillInputMessage.setText("Khách hàng này không còn hóa đơn nợ cước (đã thanh toán hết).");
                        return;
                    }

                    Intent intent = new Intent(BillCustomerCodeActivity.this, BillReviewActivity.class);
                    intent.putExtra("billId", unpaidBill.getBillId());
                    intent.putExtra("serviceName", unpaidBill.getServiceName());
                    intent.putExtra("providerName", unpaidBill.getProviderName());
                    intent.putExtra("customerCode", unpaidBill.getCustomerCode());
                    intent.putExtra("customerName", unpaidBill.getCustomerName());
                    intent.putExtra("billPeriod", unpaidBill.getBillPeriod());
                    intent.putExtra("amount", unpaidBill.getAmount());
                    startActivity(intent);
                } else {
                    txtBillInputMessage.setTextColor(0xFFD71920);
                    txtBillInputMessage.setText(ErrorParser.parse(response, "Không tra cứu được hóa đơn."));
                }
            }

            @Override
            public void onFailure(Call<BillLookupResponse> call, Throwable t) {
                btnContinueBill.setEnabled(true);
                txtBillInputMessage.setTextColor(0xFFD71920);
                txtBillInputMessage.setText("Không kết nối được server: " + t.getMessage());
            }
        });
    }
}
