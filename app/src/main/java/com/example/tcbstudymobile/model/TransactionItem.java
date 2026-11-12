package com.example.tcbstudymobile.model;

public class TransactionItem {

    private int id;
    private String referenceCode;
    private String type;
    private double amount;
    private double fee;
    private String status;
    private String content;
    private String counterpartyName;
    private String counterpartyAccount;
    private Double balanceAfter;
    private String createdAt;

    // Tương thích ngược với tên cột cũ nếu có
    private String receiver_phone;
    private String created_at;

    public int getId() {
        return id;
    }

    public String getReferenceCode() {
        return referenceCode == null ? "" : referenceCode;
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getFee() {
        return fee;
    }

    public String getCounterpartyName() {
        return counterpartyName == null ? "" : counterpartyName;
    }

    public String getCounterpartyAccount() {
        return counterpartyAccount == null ? "" : counterpartyAccount;
    }

    public String getReceiverPhone() {
        if (counterpartyName != null && !counterpartyName.trim().isEmpty()) {
            if (counterpartyAccount != null && !counterpartyAccount.trim().isEmpty()) {
                return counterpartyName + " (" + counterpartyAccount + ")";
            }
            return counterpartyName;
        }
        if (counterpartyAccount != null && !counterpartyAccount.trim().isEmpty()) {
            return counterpartyAccount;
        }
        return receiver_phone == null ? "" : receiver_phone;
    }

    public Double getBalanceAfter() {
        return balanceAfter;
    }

    public String getContent() {
        return content;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        if (createdAt != null && !createdAt.trim().isEmpty()) {
            return createdAt;
        }
        return created_at == null ? "" : created_at;
    }
}
