package com.example.tcbstudymobile.model;

public class OtpVerifyRequest {

    private String phone;
    private String code;
    private String purpose;

    public OtpVerifyRequest(String phone, String code, String purpose) {
        this.phone = phone;
        this.code = code;
        this.purpose = purpose;
    }
}