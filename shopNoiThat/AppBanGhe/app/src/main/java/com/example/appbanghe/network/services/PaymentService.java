package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.CreatePaymentRequest;
import com.example.appbanghe.network.dto.VisaPaymentRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface PaymentService {

    @POST("payment/vnpay/create")
    Call<JsonObject> createVnpayPayment(@Body CreatePaymentRequest request);

    @POST("payment/paypal/create")
    Call<JsonObject> createPaypalPayment(@Body CreatePaymentRequest request);

    @POST("payment/cod/confirm")
    Call<JsonObject> confirmCodPayment(@Body CreatePaymentRequest request);

    @GET("payment/status/{orderId}")
    Call<JsonObject> getPaymentStatus(@Path("orderId") String orderId);

    @GET("payment/history")
    Call<JsonObject> getPaymentHistory(
        @Query("page") int page,
        @Query("limit") int limit
    );

    // Legacy API - Keep for compatibility
    @POST("thanh-toan/visa")
    Call<JsonObject> thanhToanVisa(@Body VisaPaymentRequest body);

    @GET("thanh-toan/{paymentId}")
    Call<JsonObject> chiTietGiaoDich(@Path("paymentId") String paymentId);
}
