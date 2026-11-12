package com.example.tcbstudymobile.model;

public class User {

    private int id;
    private String fullName;
    private String phone;
    private String email;
    private double balance;

    private String username;
    private String accountNumber;
    private String accountType;
    private double accountFee;
    private String cccdNumber;
    private String job;
    private String createdAt;

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public double getBalance() {
        return balance;
    }

    public String getUsername() {
        return username;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getAccountType() {
        return accountType;
    }

    public double getAccountFee() {
        return accountFee;
    }

    public String getCccdNumber() {
        return cccdNumber;
    }

    public String getJob() {
        return job;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}