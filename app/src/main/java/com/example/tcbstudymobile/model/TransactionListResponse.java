package com.example.tcbstudymobile.model;

import java.util.Collections;
import java.util.List;

public class TransactionListResponse {

    private String status;
    private String message;
    private PageData data;

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public int getTotal() {
        return data != null ? data.total : 0;
    }

    public List<TransactionItem> getData() {
        if (data == null || data.transactions == null) {
            return Collections.emptyList();
        }
        return data.transactions;
    }

    public static class PageData {
        private int total;
        private int page;
        private int limit;
        private List<TransactionItem> transactions;
    }
}
