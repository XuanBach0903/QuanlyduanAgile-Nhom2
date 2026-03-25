package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.VisaPaymentRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface PaymentService {

    @POST("thanh-toan/visa")
    Call<JsonObject> thanhToanVisa(@Body VisaPaymentRequest body);

    @GET("thanh-toan/{paymentId}")
    Call<JsonObject> chiTietGiaoDich(@Path("paymentId") String paymentId);
}
