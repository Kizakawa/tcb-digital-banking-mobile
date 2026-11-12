package com.example.tcbstudymobile.model;

public class TransferConfirmRequest {

    private String referenceCode;
    private String otpCode;

    public TransferConfirmRequest(String referenceCode, String otpCode) {
        this.referenceCode = referenceCode;
        this.otpCode = otpCode;
    }
}
