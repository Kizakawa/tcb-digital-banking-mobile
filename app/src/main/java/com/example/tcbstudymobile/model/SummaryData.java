package com.example.tcbstudymobile.model;

public class SummaryData {

    private double balance;

    private int transactionCount;

    private double totalMoneyIn;
    private double totalMoneyOut;

    private double totalIncome;
    private double totalTransferIn;

    private double totalExpense;
    private double totalTransferOut;
    private double totalBillPayment;

    public double getBalance() {
        return balance;
    }

    public int getTransactionCount() {
        return transactionCount;
    }

    public double getTotalMoneyIn() {
        return totalMoneyIn;
    }

    public double getTotalMoneyOut() {
        return totalMoneyOut;
    }

    public double getTotalIncome() {
        return totalIncome;
    }

    public double getTotalTransferIn() {
        return totalTransferIn;
    }

    public double getTotalExpense() {
        return totalExpense;
    }

    public double getTotalTransferOut() {
        return totalTransferOut;
    }

    public double getTotalBillPayment() {
        return totalBillPayment;
    }
}