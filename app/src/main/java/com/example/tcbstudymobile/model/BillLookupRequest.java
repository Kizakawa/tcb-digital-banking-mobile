package com.example.tcbstudymobile.model;

public class BillLookupRequest {

    private String serviceCode;
    private String providerCode;
    private String customerCode;

    public BillLookupRequest(String serviceCode, String providerCode, String customerCode) {
        this.serviceCode = serviceCode;
        this.providerCode = providerCode;
        this.customerCode = customerCode;
    }
}
