package com.example.tcbstudymobile.openaccount;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.tcbstudymobile.R;

public class OnlineOpenIntroActivity extends AppCompatActivity {

    private TextView txtBackOnlineIntro;
    private Button btnStartOnlineOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online_open_intro);

        txtBackOnlineIntro = findViewById(R.id.txtBackOnlineIntro);
        btnStartOnlineOpen = findViewById(R.id.btnStartOnlineOpen);

        txtBackOnlineIntro.setOnClickListener(v -> finish());

        btnStartOnlineOpen.setOnClickListener(v -> {
            Intent intent = new Intent(OnlineOpenIntroActivity.this, PhoneInputActivity.class);
            startActivity(intent);
        });
    }
}