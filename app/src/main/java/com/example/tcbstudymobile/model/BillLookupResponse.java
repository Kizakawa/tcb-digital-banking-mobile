package com.example.tcbstudymobile.model;

import java.util.List;

public class BillLookupResponse {

    private String status;
    private String message;
    private List<BillItem> data;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public List<BillItem> getData() {
        return data;
    }

    public static class BillItem {
        private int billId;
        private String serviceName;
        private String providerName;
        private String customerCode;
        private String customerName;
        private String billPeriod;
        private double amount;
        private String dueDate;
        private String status;

        public int getBillId() {
            return billId;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getCustomerCode() {
            return customerCode;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getBillPeriod() {
            return billPeriod;
        }

        public double getAmount() {
            return amount;
        }

        public String getDueDate() {
            return dueDate;
        }

        public String getStatus() {
            return status;
        }
    }
}
