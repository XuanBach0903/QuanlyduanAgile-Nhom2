package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.ChatMessageRequest;
import com.example.appbanghe.network.dto.CreateChatRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ChatService {

    // Legacy API - Keep for compatibility
    @POST("chat")
    Call<JsonObject> taoChat(@Body CreateChatRequest body);

    @GET("chat/{chatId}/tin-nhan")
    Call<JsonObject> danhSachTinNhan(@Path("chatId") String chatId);

    @POST("chat/{chatId}/tin-nhan")
    Call<JsonObject> guiTinNhan(@Path("chatId") String chatId, @Body ChatMessageRequest body);

    // New Chat API
    @POST("chat/tin-nhan")
    Call<JsonObject> guiTinNhanMoi(@Body ChatMessageRequest request);

    @GET("chat/lich-su")
    Call<JsonObject> layLichSuChat(@Query("limit") int limit);

    @POST("chat/admin/tin-nhan")
    Call<JsonObject> guiTinNhanAdmin(@Body ChatMessageRequest request);

    @GET("chat/admin/lich-su")
    Call<JsonObject> layLichSuChatAdmin(@Query("nguoi_dung_id") String nguoiDungId, @Query("limit") int limit);

    @GET("chat/admin/phong-chat")
    Call<JsonObject> layDanhSachPhongChat();

    @POST("chat/san-pham")
    Call<JsonObject> guiThongTinSanPham(@Body ChatMessageRequest request);

    @GET("chat/tim-kiem")
    Call<JsonObject> timKiemTinNhan(@Query("tu_khoa") String tuKhoa);

    @GET("chat/admin/chua-doc")
    Call<JsonObject> demTinNhanChuaDoc();

    @GET("chat/admin/thong-ke")
    Call<JsonObject> layThongKeChat();

    @POST("chat/admin/da-doc")
    Call<JsonObject> danhDauDaDoc(@Body JsonObject request);
}
