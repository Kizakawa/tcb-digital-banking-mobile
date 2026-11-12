package com.example.tcbstudymobile.storage;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREF_NAME = "TCB_STUDY_SESSION_SECURE";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void saveLoginSession(String token, String fullName, String phone, double balance) {
        editor.putString("token_enc", SecureStorageHelper.encrypt(token));
        editor.putString("fullName", fullName);
        editor.putString("phone", phone);
        editor.putFloat("balance", (float) balance);
        editor.apply();
    }

    public String getToken() {
        String enc = prefs.getString("token_enc", "");
        return SecureStorageHelper.decrypt(enc);
    }

    public String getFullName() {
        return prefs.getString("fullName", "");
    }

    public String getPhone() {
        return prefs.getString("phone", "");
    }

    public double getBalance() {
        return prefs.getFloat("balance", 0);
    }

    public void updateBalance(double balance) {
        editor.putFloat("balance", (float) balance);
        editor.apply();
    }

    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}
