package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.EkycCccdRequest;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CccdConfirmActivity extends AppCompatActivity {

    private TextView txtBackCccdConfirm, txtCccdInfo;
    private Button btnConfirmCccdInfo, btnRetakeCccd;

    private OpenAccountManager openAccountManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cccd_confirm);

        txtBackCccdConfirm = findViewById(R.id.txtBackCccdConfirm);
        txtCccdInfo = findViewById(R.id.txtCccdInfo);
        btnConfirmCccdInfo = findViewById(R.id.btnConfirmCccdInfo);
        btnRetakeCccd = findViewById(R.id.btnRetakeCccd);

        openAccountManager = new OpenAccountManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        showIdentityInfo();

        txtBackCccdConfirm.setOnClickListener(v -> finish());

        btnRetakeCccd.setOnClickListener(v -> {
            Intent intent = new Intent(CccdConfirmActivity.this, MockCccdInputActivity.class);
            startActivity(intent);
            finish();
        });

        btnConfirmCccdInfo.setOnClickListener(v -> verifyCccdWithServer());
    }

    private void showIdentityInfo() {
        String fullName = openAccountManager.getFullName();
        String dob = openAccountManager.getDateOfBirth();
        String gender = openAccountManager.getGender();
        String cccdNumber = openAccountManager.getCccdNumber();
        String issueDate = openAccountManager.getIssueDate();
        String issuePlace = openAccountManager.getIssuePlace();
        String address = openAccountManager.getAddress();

        if (fullName == null || fullName.trim().isEmpty()
                || cccdNumber == null || cccdNumber.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu dữ liệu CCCD mô phỏng. Vui lòng nhập lại.", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(CccdConfirmActivity.this, MockCccdInputActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        txtCccdInfo.setText(
                "Họ và tên:                 " + fullName +
                        "\nNgày sinh:              " + safe(dob) +
                        "\nGiới tính:                 " + safe(gender) +
                        "\nSố CCCD:                " + cccdNumber +
                        "\nNgày cấp:                " + safe(issueDate) +
                        "\nNơi cấp:                  " + safe(issuePlace) +
                        "\nĐịa chỉ thường trú:  " + safe(address)
        );
    }

    private String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value;
    }

    private void verifyCccdWithServer() {
        String phone = openAccountManager.getPhone();
        String fullName = openAccountManager.getFullName();
        String cccdNumber = openAccountManager.getCccdNumber();

        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu số điện thoại đăng ký", Toast.LENGTH_SHORT).show();
            return;
        }

        if (fullName == null || fullName.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu họ tên định danh", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cccdNumber == null || !cccdNumber.matches("^[0-9]{12}$")) {
            Toast.makeText(this, "Số CCCD phải gồm đúng 12 chữ số", Toast.LENGTH_SHORT).show();
            return;
        }

        btnConfirmCccdInfo.setEnabled(false);
        Toast.makeText(this, "Đang xác minh CCCD...", Toast.LENGTH_SHORT).show();

        EkycCccdRequest request = new EkycCccdRequest(phone, cccdNumber, fullName);

        apiService.verifyCccd(request).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                btnConfirmCccdInfo.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    BasicResponse basicResponse = response.body();

                    if ("success".equals(basicResponse.getStatus())) {
                        openAccountManager.setCccdVerified(true);
                        openAccountManager.setFaceVerified(false);

                        Intent intent = new Intent(CccdConfirmActivity.this, FaceGuideActivity.class);
                        startActivity(intent);
                    } else {
                        Toast.makeText(CccdConfirmActivity.this, basicResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(CccdConfirmActivity.this, "Xác minh CCCD thất bại. CCCD có thể đã được đăng ký.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                btnConfirmCccdInfo.setEnabled(true);
                Toast.makeText(CccdConfirmActivity.this, "Không kết nối được server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}