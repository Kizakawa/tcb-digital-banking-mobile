package com.example.tcbstudymobile.openaccount;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.tcbstudymobile.storage.SecureStorageHelper;

public class OpenAccountManager {

    private static final String PREF_NAME = "OPEN_ACCOUNT_DRAFT_SECURE";

    /**
     * Tuyệt đối KHÔNG ghi mật khẩu nháp xuống SharedPreferences XML trên đĩa.
     * Chỉ giữ tạm thời trên RAM trong phiên mở tài khoản và xóa sạch ngay sau khi hoàn tất.
     */
    private static volatile String transientPassword = "";

    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    public OpenAccountManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void clearAll() {
        transientPassword = "";
        editor.clear();
        editor.apply();
    }

    // ================= PHONE / OTP =================

    public void setPhone(String value) {
        editor.putString("phone", value).apply();
    }

    public String getPhone() {
        return prefs.getString("phone", "");
    }

    public void setOtpVerified(boolean value) {
        editor.putBoolean("otpVerified", value).apply();
    }

    public boolean isOtpVerified() {
        return prefs.getBoolean("otpVerified", false);
    }

    public void setLastRegisterOtpCode(String value) {
        editor.putString("lastRegisterOtpCode", value == null ? "" : value).apply();
    }

    public String getLastRegisterOtpCode() {
        return prefs.getString("lastRegisterOtpCode", "");
    }

    // ================= ACCOUNT PRODUCT =================

    public void setAccountProduct(String value) {
        editor.putString("accountProduct", value).apply();
    }

    public String getAccountProduct() {
        return prefs.getString("accountProduct", "");
    }

    // ================= IDENTITY (CCCD ENCRYPTED VIA KEYSTORE) =================

    public void setIdentityMethod(String value) {
        editor.putString("identityMethod", value).apply();
    }

    public String getIdentityMethod() {
        return prefs.getString("identityMethod", "");
    }

    public void setCccdNumber(String value) {
        editor.putString("cccdNumber_enc", SecureStorageHelper.encrypt(value)).apply();
    }

    public String getCccdNumber() {
        return SecureStorageHelper.decrypt(prefs.getString("cccdNumber_enc", ""));
    }

    public void setCccdVerified(boolean value) {
        editor.putBoolean("cccdVerified", value).apply();
    }

    public boolean isCccdVerified() {
        return prefs.getBoolean("cccdVerified", false);
    }

    public void setFullName(String value) {
        editor.putString("fullName", value).apply();
    }

    public String getFullName() {
        return prefs.getString("fullName", "");
    }

    public void setDateOfBirth(String value) {
        editor.putString("dateOfBirth", value).apply();
    }

    public String getDateOfBirth() {
        return prefs.getString("dateOfBirth", "");
    }

    public void setGender(String value) {
        editor.putString("gender", value).apply();
    }

    public String getGender() {
        return prefs.getString("gender", "");
    }

    public void setIssueDate(String value) {
        editor.putString("issueDate", value).apply();
    }

    public String getIssueDate() {
        return prefs.getString("issueDate", "");
    }

    public void setIssuePlace(String value) {
        editor.putString("issuePlace", value).apply();
    }

    public String getIssuePlace() {
        return prefs.getString("issuePlace", "");
    }

    public void setAddress(String value) {
        editor.putString("address", value).apply();
    }

    public String getAddress() {
        return prefs.getString("address", "");
    }

    public void setFaceVerified(boolean value) {
        editor.putBoolean("faceVerified", value).apply();
    }

    public boolean isFaceVerified() {
        return prefs.getBoolean("faceVerified", false);
    }

    public void setVneidLinked(boolean value) {
        editor.putBoolean("vneidLinked", value).apply();
    }

    public boolean isVneidLinked() {
        return prefs.getBoolean("vneidLinked", false);
    }

    // ================= EXTRA INFO =================

    public void setEmail(String value) {
        editor.putString("email", value).apply();
    }

    public String getEmail() {
        return prefs.getString("email", "");
    }

    public void setJob(String value) {
        editor.putString("job", value).apply();
    }

    public String getJob() {
        return prefs.getString("job", "");
    }

    // ================= ACCOUNT NUMBER =================

    public void setAccountType(String value) {
        editor.putString("accountType", value).apply();
    }

    public String getAccountType() {
        return prefs.getString("accountType", "");
    }

    public void setAccountNumber(String value) {
        editor.putString("accountNumber", value).apply();
    }

    public String getAccountNumber() {
        return prefs.getString("accountNumber", "");
    }

    public void setAccountFee(double value) {
        editor.putFloat("accountFee", (float) value).apply();
    }

    public double getAccountFee() {
        return prefs.getFloat("accountFee", 0);
    }

    // ================= LOGIN INFO =================

    public void setUsername(String value) {
        editor.putString("username", value).apply();
    }

    public String getUsername() {
        return prefs.getString("username", "");
    }

    public void setPassword(String value) {
        transientPassword = value == null ? "" : value;
    }

    public String getPassword() {
        return transientPassword;
    }
}
