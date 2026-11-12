package com.example.tcbstudymobile.model;

public class LoginResponse {

    private String status;
    private String message;
    private String token;
    private User user;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }
}