package com.example.tcbstudymobile.model;

public class OtpSendRequest {

    private String phone;
    private String purpose;
    private String referenceCode;

    public OtpSendRequest(String phone, String purpose) {
        this.phone = phone;
        this.purpose = purpose;
        this.referenceCode = "";
    }

    public OtpSendRequest(String phone, String purpose, String referenceCode) {
        this.phone = phone;
        this.purpose = purpose;
        this.referenceCode = referenceCode;
    }
}