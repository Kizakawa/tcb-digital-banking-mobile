package com.example.tcbstudymobile.model;

public class TransferRequest {

    private String receiverAccountNumber;
    private double amount;
    private String content;
    private String otpCode;

    public TransferRequest(String receiverAccountNumber, double amount, String content) {
        this(receiverAccountNumber, amount, content, "");
    }

    public TransferRequest(String receiverAccountNumber, double amount, String content, String otpCode) {
        this.receiverAccountNumber = receiverAccountNumber;
        this.amount = amount;
        this.content = content;
        this.otpCode = otpCode;
    }
}
