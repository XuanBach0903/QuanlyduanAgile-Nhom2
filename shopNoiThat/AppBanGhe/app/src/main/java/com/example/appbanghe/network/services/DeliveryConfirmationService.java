package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.CreateDeliveryConfirmationRequest;
import com.example.appbanghe.network.dto.DeliveryConfirmationRequest;
import com.example.appbanghe.network.dto.DeliveryRatingRequest;
import com.example.appbanghe.network.dto.DeliveryInfoRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface DeliveryConfirmationService {

    // Customer APIs
    @POST("xac-nhan-giao-hang")
    Call<JsonObject> taoYeuCauXacNhan(@Body CreateDeliveryConfirmationRequest request);

    @POST("xac-nhan-giao-hang/{xac_nhan_id}/xac-nhan")
    Call<JsonObject> xacNhanGiaoHang(@Path("xac_nhan_id") String xacNhanId, @Body DeliveryInfoRequest request);

    @POST("xac-nhan-giao-hang/{xac_nhan_id}/tu-choi")
    Call<JsonObject> tuChoiGiaoHang(@Path("xac_nhan_id") String xacNhanId, @Body DeliveryConfirmationRequest request);

    @POST("xac-nhan-giao-hang/{xac_nhan_id}/danh-gia")
    Call<JsonObject> danhGiaGiaoHang(@Path("xac_nhan_id") String xacNhanId, @Body DeliveryRatingRequest request);

    @GET("xac-nhan-giao-hang")
    Call<JsonObject> layDanhSachXacNhan(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("trang_thai") String trangThai,
        @Query("tu_ngay") String tuNgay,
        @Query("den_ngay") String denNgay
    );

    @GET("xac-nhan-giao-hang/{xac_nhan_id}")
    Call<JsonObject> layChiTietXacNhan(@Path("xac_nhan_id") String xacNhanId);

    // Shipper APIs
    @GET("shipper/xac-nhan-giao-hang/cho-xac-nhan")
    Call<JsonObject> layDanhSachChoXacNhan(@Query("page") int page, @Query("limit") int limit);

    @POST("shipper/xac-nhan-giao-hang/{xac_nhan_id}/nhan")
    Call<JsonObject> nhanGiaoHang(@Path("xac_nhan_id") String xacNhanId);

    @POST("shipper/xac-nhan-giao-hang/{xac_nhan_id}/hoan-tat")
    Call<JsonObject> hoanTatGiaoHang(@Path("xac_nhan_id") String xacNhanId);

    // Admin APIs
    @GET("admin/xac-nhan-giao-hang/thong-ke")
    Call<JsonObject> layThongKeXacNhan(@Query("nguoi_giao_hang_id") String nguoiGiaoHangId, @Query("tu_ngay") String tuNgay, @Query("den_ngay") String denNgay);
}
