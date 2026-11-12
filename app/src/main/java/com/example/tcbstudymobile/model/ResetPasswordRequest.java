package com.example.tcbstudymobile.model;

public class ResetPasswordRequest {

    private String phone;
    private String newPassword;

    public ResetPasswordRequest(String phone, String newPassword) {
        this.phone = phone;
        this.newPassword = newPassword;
    }
}