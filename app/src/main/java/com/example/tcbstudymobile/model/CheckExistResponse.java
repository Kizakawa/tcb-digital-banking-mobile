package com.example.tcbstudymobile.model;

public class CheckExistResponse {

    private String status;
    private boolean exists;
    private String message;

    public String getStatus() {
        return status;
    }

    public boolean isExists() {
        return exists;
    }

    public String getMessage() {
        return message;
    }
}