package com.example.tcbstudymobile.model;

public class RegisterRequest {

    private String fullName;
    private String phone;
    private String email;
    private String password;

    private String username;
    private String accountNumber;
    private String accountType;
    private double accountFee;
    private String cccdNumber;
    private String job;

    public RegisterRequest(String fullName, String phone, String email, String password) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.username = "";
        this.accountNumber = "";
        this.accountType = "";
        this.accountFee = 0;
        this.cccdNumber = "";
        this.job = "";
    }

    public RegisterRequest(
            String fullName,
            String phone,
            String email,
            String password,
            String username,
            String accountNumber,
            String accountType,
            double accountFee,
            String cccdNumber,
            String job
    ) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.password = password;
        this.username = username;
        this.accountNumber = accountNumber;
        this.accountType = accountType;
        this.accountFee = accountFee;
        this.cccdNumber = cccdNumber;
        this.job = job;
    }
}