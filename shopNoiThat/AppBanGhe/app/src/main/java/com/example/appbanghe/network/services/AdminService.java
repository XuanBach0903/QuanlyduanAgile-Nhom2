package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.AdminRefundRequest;
import com.example.appbanghe.network.dto.AdminUpdateOrderStatusRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface AdminService {

    @GET("admin/don-hang")
    Call<JsonObject> danhSachDonHang(@Query("trangThai") String trangThai);

    @PATCH("admin/don-hang/{orderId}/trang-thai")
    Call<JsonObject> capNhatTrangThaiDonHang(@Path("orderId") long orderId, @Body AdminUpdateOrderStatusRequest body);

    @POST("admin/don-hang/{orderId}/hoan")
    Call<JsonObject> hoanDon(@Path("orderId") long orderId, @Body AdminRefundRequest body);

    @GET("admin/hoa-don")
    Call<JsonObject> danhSachHoaDon();

    @GET("admin/hoa-don/{invoiceId}")
    Call<JsonObject> chiTietHoaDon(@Path("invoiceId") long invoiceId);

    @POST("admin/hoa-don/{invoiceId}/xuat")
    Call<JsonObject> xuatHoaDon(@Path("invoiceId") long invoiceId);

    @GET("admin/khach-hang")
    Call<JsonObject> danhSachKhachHang();

    @GET("admin/khach-hang/{userId}")
    Call<JsonObject> chiTietKhachHang(@Path("userId") long userId);

    @PATCH("admin/khach-hang/{userId}")
    Call<JsonObject> capNhatKhachHang(@Path("userId") long userId, @Body JsonObject body);

    @GET("admin/chat")
    Call<JsonObject> danhSachChat();

    @GET("admin/chat/{chatId}/tin-nhan")
    Call<JsonObject> adminDanhSachTinNhan(@Path("chatId") long chatId);

    @POST("admin/chat/{chatId}/tin-nhan")
    Call<JsonObject> adminGuiTinNhan(@Path("chatId") long chatId, @Body JsonObject body);

    @POST("admin/san-pham")
    Call<JsonObject> themSanPham(@Body JsonObject body);

    @PATCH("admin/san-pham/{id}")
    Call<JsonObject> suaSanPham(@Path("id") long id, @Body JsonObject body);

    @DELETE("admin/san-pham/{id}")
    Call<JsonObject> xoaSanPham(@Path("id") long id);

    @POST("admin/san-pham/{id}/hinh-anh")
    Call<JsonObject> themHinhAnh(@Path("id") long id, @Body JsonObject body);

    @DELETE("admin/san-pham/{id}/hinh-anh/{imageId}")
    Call<JsonObject> xoaHinhAnh(@Path("id") long id, @Path("imageId") long imageId);
}
