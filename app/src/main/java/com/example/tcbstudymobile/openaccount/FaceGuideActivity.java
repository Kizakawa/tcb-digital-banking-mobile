package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.EkycFaceRequest;
import com.example.tcbstudymobile.network.ApiClient;
import com.example.tcbstudymobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FaceGuideActivity extends AppCompatActivity {

    private TextView txtBackFaceGuide;
    private Button btnStartFaceVerify;

    private OpenAccountManager openAccountManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_guide);

        txtBackFaceGuide = findViewById(R.id.txtBackFaceGuide);
        btnStartFaceVerify = findViewById(R.id.btnStartFaceVerify);

        openAccountManager = new OpenAccountManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        txtBackFaceGuide.setOnClickListener(v -> finish());

        btnStartFaceVerify.setOnClickListener(v -> verifyFaceWithServer());
    }

    private void verifyFaceWithServer() {
        String phone = openAccountManager.getPhone();

        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu số điện thoại đăng ký", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!openAccountManager.isCccdVerified()) {
            Toast.makeText(this, "Vui lòng xác minh CCCD trước", Toast.LENGTH_SHORT).show();
            return;
        }

        btnStartFaceVerify.setEnabled(false);
        Toast.makeText(this, "Đang xác minh khuôn mặt...", Toast.LENGTH_SHORT).show();

        EkycFaceRequest request = new EkycFaceRequest(phone);

        apiService.verifyFace(request).enqueue(new Callback<BasicResponse>() {
            @Override
            public void onResponse(Call<BasicResponse> call, Response<BasicResponse> response) {
                btnStartFaceVerify.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    BasicResponse basicResponse = response.body();

                    if ("success".equals(basicResponse.getStatus())) {
                        openAccountManager.setFaceVerified(true);

                        String method = openAccountManager.getIdentityMethod();

                        if ("VNEID".equals(method)) {
                            Intent intent = new Intent(FaceGuideActivity.this, IdentitySuccessActivity.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(FaceGuideActivity.this, VneidLinkActivity.class);
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(FaceGuideActivity.this, basicResponse.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FaceGuideActivity.this, "Xác minh khuôn mặt thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<BasicResponse> call, Throwable t) {
                btnStartFaceVerify.setEnabled(true);
                Toast.makeText(FaceGuideActivity.this, "Không kết nối được server: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}