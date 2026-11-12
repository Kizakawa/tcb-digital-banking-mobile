package com.example.tcbstudymobile.model;

public class LoginRequest {

    private String phone;
    private String password;

    public LoginRequest(String phone, String password) {
        this.phone = phone;
        this.password = password;
    }
}