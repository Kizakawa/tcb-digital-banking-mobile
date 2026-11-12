package com.example.tcbstudymobile.model;

public class BillPaymentResponse {

    private String status;
    private String message;
    private double balance;
    private int transactionId;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public double getBalance() {
        return balance;
    }

    public int getTransactionId() {
        return transactionId;
    }
}