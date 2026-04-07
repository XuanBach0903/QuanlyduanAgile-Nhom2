package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.CreateInvoiceRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface InvoiceService {

    // Customer APIs
    @POST("hoa-don")
    Call<JsonObject> taoHoaDon(@Body CreateInvoiceRequest request);

    @GET("hoa-don")
    Call<JsonObject> layDanhSachHoaDon(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("trang_thai") String trangThai,
        @Query("tu_ngay") String tuNgay,
        @Query("den_ngay") String denNgay
    );

    @GET("hoa-don/chi-tiet/{hoa_don_id}")
    Call<JsonObject> layChiTietHoaDon(@Path("hoa_don_id") String hoaDonId);

    @POST("hoa-don/{hoa_don_id}/huy")
    Call<JsonObject> huyHoaDon(@Path("hoa_don_id") String hoaDonId, @Body JsonObject request);

    @GET("hoa-don/{hoa_don_id}/pdf")
    Call<JsonObject> taiHoaDonPDF(@Path("hoa_don_id") String hoaDonId);

    @GET("hoa-don/thong-ke")
    Call<JsonObject> layThongKeHoaDon(@Query("tu_ngay") String tuNgay, @Query("den_ngay") String denNgay);

    // Admin APIs
    @POST("admin/hoa-don/{hoa_don_id}/xuat")
    Call<JsonObject> xuatHoaDon(@Path("hoa_don_id") String hoaDonId);

    @GET("admin/hoa-don")
    Call<JsonObject> layTatCaHoaDon(
        @Query("page") int page,
        @Query("limit") int limit,
        @Query("trang_thai") String trangThai,
        @Query("tu_ngay") String tuNgay,
        @Query("den_ngay") String denNgay,
        @Query("tim_kiem") String timKiem
    );

    @GET("admin/hoa-don/tim-kiem")
    Call<JsonObject> timKiemHoaDon(@Query("keyword") String keyword);
}
