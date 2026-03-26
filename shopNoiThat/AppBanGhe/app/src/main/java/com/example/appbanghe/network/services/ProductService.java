package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.ReviewRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ProductService {

    @GET("san-pham")
    Call<JsonObject> getSanPham(
            @Query("danhMucId") String danhMucId,
            @Query("tuKhoa") String tuKhoa,
            @Query("trang") Integer trang,
            @Query("gioiHan") Integer gioiHan
    );

    @GET("san-pham/{id}")
    Call<JsonObject> getSanPhamChiTiet(@Path("id") String id);

    @GET("san-pham/{id}/hinh-anh")
    Call<JsonArray> getSanPhamHinhAnh(@Path("id") String id);

    @GET("san-pham/{id}/danh-gia")
    Call<JsonObject> getDanhGia(@Path("id") String productId);

    @POST("san-pham/{id}/danh-gia")
    Call<JsonObject> taoDanhGia(@Path("id") String productId, @Body ReviewRequest body);
}
