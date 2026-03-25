package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.AddCartItemRequest;
import com.example.appbanghe.network.dto.UpdateCartItemRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface CartService {

    @GET("gio-hang")
    Call<JsonObject> getGioHang();

    @POST("gio-hang/mat-hang")
    Call<JsonObject> addMatHang(@Body AddCartItemRequest body);

    @PATCH("gio-hang/mat-hang/{itemId}")
    Call<JsonObject> updateMatHang(@Path("itemId") String itemId, @Body UpdateCartItemRequest body);

    @DELETE("gio-hang/mat-hang/{itemId}")
    Call<JsonObject> deleteMatHang(@Path("itemId") String itemId);

    @DELETE("gio-hang")
    Call<JsonObject> clearGioHang();
}
