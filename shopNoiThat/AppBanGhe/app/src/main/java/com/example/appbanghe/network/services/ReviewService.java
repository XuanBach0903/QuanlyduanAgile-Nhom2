package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.CreateReviewRequest;
import com.example.appbanghe.network.dto.UpdateReviewRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ReviewService {

    // Customer APIs
    @POST("danh-gia")
    Call<JsonObject> taoDanhGia(@Body CreateReviewRequest request);

    @GET("danh-gia/san-pham/{san_pham_id}")
    Call<JsonObject> layDanhGiaSanPham(@Path("san_pham_id") String sanPhamId, @Query("page") int page, @Query("limit") int limit);

    @GET("danh-gia/cua-toi")
    Call<JsonObject> layDanhGiaCuaToi(@Query("page") int page, @Query("limit") int limit);

    @PUT("danh-gia/{danh_gia_id}")
    Call<JsonObject> capNhatDanhGia(@Path("danh_gia_id") String danhGiaId, @Body UpdateReviewRequest request);

    @DELETE("danh-gia/{danh_gia_id}")
    Call<JsonObject> xoaDanhGia(@Path("danh_gia_id") String danhGiaId);

    @POST("danh-gia/{danh_gia_id}/thich")
    Call<JsonObject> thichDanhGia(@Path("danh_gia_id") String danhGiaId);

    @POST("danh-gia/{danh_gia_id}/bao-cao")
    Call<JsonObject> baoCaoDanhGia(@Path("danh_gia_id") String danhGiaId, @Body JsonObject request);

    // Admin APIs
    @POST("admin/danh-gia/{danh_gia_id}/phan-hoi")
    Call<JsonObject> phanHoiDanhGia(@Path("danh_gia_id") String danhGiaId, @Body JsonObject request);

    @PUT("admin/danh-gia/{danh_gia_id}/duyet")
    Call<JsonObject> duyetDanhGia(@Path("danh_gia_id") String danhGiaId, @Body JsonObject request);

    @GET("admin/danh-gia/can-duyet")
    Call<JsonObject> layDanhGiaCanDuyet(@Query("page") int page, @Query("limit") int limit);

    @GET("admin/danh-gia/thong-ke")
    Call<JsonObject> layThongKeDanhGia(@Query("san_pham_id") String sanPhamId);
}
