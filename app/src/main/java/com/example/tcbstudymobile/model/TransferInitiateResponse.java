package com.example.tcbstudymobile.model;

public class TransferInitiateResponse {

    private String status;
    private String message;
    private InitiateData data;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public InitiateData getData() {
        return data;
    }

    public static class InitiateData {
        private String referenceCode;
        private String receiverName;
        private String receiverAccount;
        private double amount;
        private double fee;
        private String expiresAt;
        private String otpCode;

        public String getReferenceCode() {
            return referenceCode;
        }

        public String getReceiverName() {
            return receiverName;
        }

        public String getReceiverAccount() {
            return receiverAccount;
        }

        public double getAmount() {
            return amount;
        }

        public double getFee() {
            return fee;
        }

        public String getExpiresAt() {
            return expiresAt;
        }

        public String getOtpCode() {
            return otpCode;
        }
    }
}
