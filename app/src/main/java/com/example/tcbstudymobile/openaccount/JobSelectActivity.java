package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class JobSelectActivity extends AppCompatActivity {

    private TextView txtBackJob;
    private TextView jobAdmin, jobCustomerService, jobHr, jobAccounting,
            jobBusiness, jobMarketing, jobProduction, jobStudent, jobOther;

    private OpenAccountManager openAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_select);

        txtBackJob = findViewById(R.id.txtBackJob);

        jobAdmin = findViewById(R.id.jobAdmin);
        jobCustomerService = findViewById(R.id.jobCustomerService);
        jobHr = findViewById(R.id.jobHr);
        jobAccounting = findViewById(R.id.jobAccounting);
        jobBusiness = findViewById(R.id.jobBusiness);
        jobMarketing = findViewById(R.id.jobMarketing);
        jobProduction = findViewById(R.id.jobProduction);
        jobStudent = findViewById(R.id.jobStudent);
        jobOther = findViewById(R.id.jobOther);

        openAccountManager = new OpenAccountManager(this);

        txtBackJob.setOnClickListener(v -> finish());

        jobAdmin.setOnClickListener(v -> selectJob("Hành chính tổng hợp"));
        jobCustomerService.setOnClickListener(v -> selectJob("Chăm sóc khách hàng"));
        jobHr.setOnClickListener(v -> selectJob("Nhân sự"));
        jobAccounting.setOnClickListener(v -> selectJob("Kế toán"));
        jobBusiness.setOnClickListener(v -> selectJob("Kinh doanh"));
        jobMarketing.setOnClickListener(v -> selectJob("Tiếp thị"));
        jobProduction.setOnClickListener(v -> selectJob("Xây dựng / sản xuất"));
        jobStudent.setOnClickListener(v -> selectJob("Sinh viên"));
        jobOther.setOnClickListener(v -> selectJob("Khác"));
    }

    private void selectJob(String job) {
        openAccountManager.setJob(job);

        Intent intent = new Intent(JobSelectActivity.this, AccountNumberTypeActivity.class);
        startActivity(intent);
    }
}