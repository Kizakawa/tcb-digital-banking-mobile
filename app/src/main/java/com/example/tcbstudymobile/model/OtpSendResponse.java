package com.example.tcbstudymobile.model;

public class OtpSendResponse {

    private String status;
    private String message;
    private String otpCode;
    private int expiresIn;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public int getExpiresIn() {
        return expiresIn;
    }
}