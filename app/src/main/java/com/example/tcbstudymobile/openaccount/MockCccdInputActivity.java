package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class MockCccdInputActivity extends AppCompatActivity {

    private TextView txtBackMockCccd, txtMockCccdTitle, txtMockCccdDesc, txtMockCccdMessage;
    private EditText edtMockFullName, edtMockCccdNumber, edtMockDob,
            edtMockGender, edtMockIssueDate, edtMockIssuePlace, edtMockAddress;
    private Button btnContinueMockCccd;

    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mock_cccd_input);

        txtBackMockCccd = findViewById(R.id.txtBackMockCccd);
        txtMockCccdTitle = findViewById(R.id.txtMockCccdTitle);
        txtMockCccdDesc = findViewById(R.id.txtMockCccdDesc);
        txtMockCccdMessage = findViewById(R.id.txtMockCccdMessage);

        edtMockFullName = findViewById(R.id.edtMockFullName);
        edtMockCccdNumber = findViewById(R.id.edtMockCccdNumber);
        edtMockDob = findViewById(R.id.edtMockDob);
        edtMockGender = findViewById(R.id.edtMockGender);
        edtMockIssueDate = findViewById(R.id.edtMockIssueDate);
        edtMockIssuePlace = findViewById(R.id.edtMockIssuePlace);
        edtMockAddress = findViewById(R.id.edtMockAddress);

        btnContinueMockCccd = findViewById(R.id.btnContinueMockCccd);

        openAccountManager = new OpenAccountManager(this);

        setupTitleByIdentityMethod();
        fillOldDataOrDefault();

        txtBackMockCccd.setOnClickListener(v -> finish());

        btnContinueMockCccd.setOnClickListener(v -> validateAndContinue());
    }

    private void setupTitleByIdentityMethod() {
        String method = openAccountManager.getIdentityMethod();

        if ("VNEID".equals(method)) {
            txtMockCccdTitle.setText("Nhập dữ liệu định danh mô phỏng từ VNeID");
            txtMockCccdDesc.setText("Do chưa tích hợp VNeID thật, vui lòng nhập dữ liệu định danh mô phỏng để hệ thống xác minh eKYC.");
        } else {
            txtMockCccdTitle.setText("Nhập dữ liệu CCCD mô phỏng");
            txtMockCccdDesc.setText("Do chưa tích hợp OCR CCCD thật, vui lòng nhập dữ liệu mô phỏng như kết quả quét CCCD.");
        }
    }

    private void fillOldDataOrDefault() {
        String oldFullName = openAccountManager.getFullName();
        String oldCccd = openAccountManager.getCccdNumber();

        if (oldFullName != null && !oldFullName.trim().isEmpty()) {
            edtMockFullName.setText(oldFullName);
        } else {
            edtMockFullName.setText("Phan Quoc Bao");
        }

        if (oldCccd != null && !oldCccd.trim().isEmpty()) {
            edtMockCccdNumber.setText(oldCccd);
        } else {
            edtMockCccdNumber.setText(generateDemoCccdFromPhone());
        }

        String oldDob = openAccountManager.getDateOfBirth();
        String oldGender = openAccountManager.getGender();
        String oldIssueDate = openAccountManager.getIssueDate();
        String oldIssuePlace = openAccountManager.getIssuePlace();
        String oldAddress = openAccountManager.getAddress();

        edtMockDob.setText(oldDob == null || oldDob.trim().isEmpty() ? "01/01/2003" : oldDob);
        edtMockGender.setText(oldGender == null || oldGender.trim().isEmpty() ? "Nam" : oldGender);
        edtMockIssueDate.setText(oldIssueDate == null || oldIssueDate.trim().isEmpty() ? "01/01/2021" : oldIssueDate);
        edtMockIssuePlace.setText(oldIssuePlace == null || oldIssuePlace.trim().isEmpty() ? "Hà Nội" : oldIssuePlace);
        edtMockAddress.setText(oldAddress == null || oldAddress.trim().isEmpty()
                ? "Hà Nội, Việt Nam"
                : oldAddress);
    }

    private String generateDemoCccdFromPhone() {
        String phone = openAccountManager.getPhone();

        if (phone != null && phone.matches("^[0-9]{10}$")) {
            return "00" + phone;
        }

        long suffix = System.currentTimeMillis() % 1000000000L;
        return "001" + String.format("%09d", suffix);
    }

    private void validateAndContinue() {
        String fullName = edtMockFullName.getText().toString().trim();
        String cccdNumber = edtMockCccdNumber.getText().toString().trim();
        String dob = edtMockDob.getText().toString().trim();
        String gender = edtMockGender.getText().toString().trim();
        String issueDate = edtMockIssueDate.getText().toString().trim();
        String issuePlace = edtMockIssuePlace.getText().toString().trim();
        String address = edtMockAddress.getText().toString().trim();

        if (fullName.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập họ và tên");
            return;
        }

        if (fullName.length() < 5) {
            txtMockCccdMessage.setText("Họ và tên phải có ít nhất 5 ký tự");
            return;
        }

        if (!fullName.matches("^[\\p{L}\\s'.-]+$")) {
            txtMockCccdMessage.setText("Họ và tên chỉ được gồm chữ cái và khoảng trắng");
            return;
        }

        if (cccdNumber.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập số CCCD");
            return;
        }

        if (!cccdNumber.matches("^[0-9]{12}$")) {
            txtMockCccdMessage.setText("Số CCCD phải gồm đúng 12 chữ số");
            return;
        }

        if (dob.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập ngày sinh");
            return;
        }

        if (gender.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập giới tính");
            return;
        }

        if (issueDate.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập ngày cấp CCCD");
            return;
        }

        if (issuePlace.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập nơi cấp CCCD");
            return;
        }

        if (address.isEmpty()) {
            txtMockCccdMessage.setText("Vui lòng nhập địa chỉ thường trú");
            return;
        }

        openAccountManager.setFullName(fullName);
        openAccountManager.setCccdNumber(cccdNumber);
        openAccountManager.setDateOfBirth(dob);
        openAccountManager.setGender(gender);
        openAccountManager.setIssueDate(issueDate);
        openAccountManager.setIssuePlace(issuePlace);
        openAccountManager.setAddress(address);

        // Vì người dùng nhập/chỉnh lại dữ liệu định danh, reset trạng thái xác minh cũ
        openAccountManager.setCccdVerified(false);
        openAccountManager.setFaceVerified(false);

        Intent intent = new Intent(MockCccdInputActivity.this, CccdConfirmActivity.class);
        startActivity(intent);
    }
}