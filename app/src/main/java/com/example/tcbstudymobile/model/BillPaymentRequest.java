package com.example.tcbstudymobile.model;

public class BillPaymentRequest {

    private Integer billId;
    private String otpCode;

    private String serviceName;
    private String providerName;
    private String customerCode;
    private String customerName;
    private String billPeriod;
    private Double amount;

    public BillPaymentRequest(int billId, String otpCode) {
        this.billId = billId;
        this.otpCode = otpCode;
    }

    public BillPaymentRequest(
            String serviceName,
            String providerName,
            String customerCode,
            String customerName,
            String billPeriod,
            double amount
    ) {
        this.serviceName = serviceName;
        this.providerName = providerName;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.billPeriod = billPeriod;
        this.amount = amount;
    }
}
