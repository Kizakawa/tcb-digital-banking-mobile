package com.example.tcbstudymobile.model;

public class EkycCccdRequest {

    private String phone;
    private String cccdNumber;
    private String fullName;

    public EkycCccdRequest(String phone, String cccdNumber, String fullName) {
        this.phone = phone;
        this.cccdNumber = cccdNumber;
        this.fullName = fullName;
    }
}