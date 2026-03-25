package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.CancelOrderRequest;
import com.example.appbanghe.network.dto.CreateOrderRequest;
import com.example.appbanghe.network.dto.RefundOrderRequest;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface OrderService {

    @POST("don-hang")
    Call<JsonObject> taoDonHang(@Body CreateOrderRequest body);

    @GET("don-hang")
    Call<JsonElement> danhSachDonHangCuaToi();

    @GET("don-hang/{orderId}")
    Call<JsonObject> chiTietDonHang(@Path("orderId") String orderId);

    @POST("don-hang/{orderId}/huy")
    Call<JsonObject> huyDon(@Path("orderId") String orderId, @Body CancelOrderRequest body);

    @POST("don-hang/{orderId}/xac-nhan-da-nhan")
    Call<JsonObject> xacNhanDaNhan(@Path("orderId") String orderId);

    @POST("don-hang/{orderId}/hoan")
    Call<JsonObject> hoanDon(@Path("orderId") String orderId, @Body RefundOrderRequest body);

    @GET("don-hang/{orderId}/thanh-toan")
    Call<JsonObject> trangThaiThanhToan(@Path("orderId") String orderId);
}
