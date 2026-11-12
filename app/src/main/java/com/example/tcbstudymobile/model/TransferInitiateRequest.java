package com.example.tcbstudymobile.model;

public class TransferInitiateRequest {

    private String receiverAccountNumber;
    private double amount;
    private String content;
    private String idempotencyKey;

    public TransferInitiateRequest(
            String receiverAccountNumber,
            double amount,
            String content,
            String idempotencyKey
    ) {
        this.receiverAccountNumber = receiverAccountNumber;
        this.amount = amount;
        this.content = content;
        this.idempotencyKey = idempotencyKey;
    }
}
