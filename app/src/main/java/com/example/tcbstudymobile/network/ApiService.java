package com.example.tcbstudymobile.network;

import com.example.tcbstudymobile.model.AccountResponse;
import com.example.tcbstudymobile.model.BasicResponse;
import com.example.tcbstudymobile.model.BillLookupRequest;
import com.example.tcbstudymobile.model.BillLookupResponse;
import com.example.tcbstudymobile.model.BillPaymentRequest;
import com.example.tcbstudymobile.model.BillPaymentResponse;
import com.example.tcbstudymobile.model.CheckExistResponse;
import com.example.tcbstudymobile.model.EkycCccdRequest;
import com.example.tcbstudymobile.model.EkycFaceRequest;
import com.example.tcbstudymobile.model.LoginRequest;
import com.example.tcbstudymobile.model.LoginResponse;
import com.example.tcbstudymobile.model.OtpSendRequest;
import com.example.tcbstudymobile.model.OtpSendResponse;
import com.example.tcbstudymobile.model.OtpVerifyRequest;
import com.example.tcbstudymobile.model.ReceiverLookupResponse;
import com.example.tcbstudymobile.model.RegisterRequest;
import com.example.tcbstudymobile.model.RegisterResponse;
import com.example.tcbstudymobile.model.ResetPasswordRequest;
import com.example.tcbstudymobile.model.SummaryResponse;
import com.example.tcbstudymobile.model.TransactionListResponse;
import com.example.tcbstudymobile.model.TransferConfirmRequest;
import com.example.tcbstudymobile.model.TransferInitiateRequest;
import com.example.tcbstudymobile.model.TransferInitiateResponse;
import com.example.tcbstudymobile.model.TransferRequest;
import com.example.tcbstudymobile.model.TransferResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // ================= AUTH =================

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/logout")
    Call<BasicResponse> logout(@Header("Authorization") String token);

    @POST("api/auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @POST("api/auth/reset-password")
    Call<BasicResponse> resetPassword(@Body ResetPasswordRequest request);

    // ================= OTP =================

    @POST("api/otp/send")
    Call<OtpSendResponse> sendOtp(@Body OtpSendRequest request);

    @POST("api/otp/send")
    Call<OtpSendResponse> sendOtpAuthenticated(
            @Header("Authorization") String token,
            @Body OtpSendRequest request
    );

    @POST("api/otp/verify")
    Call<BasicResponse> verifyOtp(@Body OtpVerifyRequest request);

    // ================= EKYC =================

    @POST("api/ekyc/verify-cccd")
    Call<BasicResponse> verifyCccd(@Body EkycCccdRequest request);

    @POST("api/ekyc/verify-face")
    Call<BasicResponse> verifyFace(@Body EkycFaceRequest request);

    // ================= CHECK =================

    @GET("api/auth/check-phone/{phone}")
    Call<CheckExistResponse> checkPhone(@Path("phone") String phone);

    @GET("api/account/check-number/{accountNumber}")
    Call<CheckExistResponse> checkAccountNumber(@Path("accountNumber") String accountNumber);

    // ================= ACCOUNT =================

    @GET("api/account/me")
    Call<AccountResponse> getAccount(@Header("Authorization") String token);

    @GET("api/account/lookup/{accountNumber}")
    Call<ReceiverLookupResponse> lookupReceiver(
            @Header("Authorization") String token,
            @Path("accountNumber") String accountNumber
    );

    // ================= TRANSFER (2-STEP OTP + ALIAS) =================

    @POST("api/transfers/initiate")
    Call<TransferInitiateResponse> initiateTransfer(
            @Header("Authorization") String token,
            @Body TransferInitiateRequest request
    );

    @POST("api/transfers/confirm")
    Call<TransferResponse> confirmTransfer(
            @Header("Authorization") String token,
            @Body TransferConfirmRequest request
    );

    @POST("api/transactions/transfer")
    Call<TransferResponse> transfer(
            @Header("Authorization") String token,
            @Body TransferRequest request
    );

    // ================= TRANSACTIONS =================

    @GET("api/transactions")
    Call<TransactionListResponse> getTransactions(@Header("Authorization") String token);

    @GET("api/transactions/summary")
    Call<SummaryResponse> getSummary(@Header("Authorization") String token);

    // ================= BILL =================

    @POST("api/bills/lookup")
    Call<BillLookupResponse> lookupBills(
            @Header("Authorization") String token,
            @Body BillLookupRequest request
    );

    @POST("api/bills/pay")
    Call<BillPaymentResponse> payBill(
            @Header("Authorization") String token,
            @Body BillPaymentRequest request
    );
}
