package com.example.tcbstudymobile.network;

import org.json.JSONObject;

import retrofit2.Response;

public final class ErrorParser {

    private ErrorParser() {
    }

    public static String parse(Response<?> response, String fallbackMessage) {
        if (response == null) {
            return fallbackMessage;
        }
        try {
            if (response.errorBody() != null) {
                String raw = response.errorBody().string();
                if (raw != null && !raw.trim().isEmpty()) {
                    JSONObject obj = new JSONObject(raw);
                    String msg = obj.optString("message", "").trim();
                    if (!msg.isEmpty()) {
                        return msg;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return fallbackMessage;
    }
}
