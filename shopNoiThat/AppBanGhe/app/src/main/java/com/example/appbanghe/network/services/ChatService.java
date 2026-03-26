package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.ChatMessageRequest;
import com.example.appbanghe.network.dto.CreateChatRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ChatService {

    @POST("chat")
    Call<JsonObject> taoChat(@Body CreateChatRequest body);

    @GET("chat/{chatId}/tin-nhan")
    Call<JsonObject> danhSachTinNhan(@Path("chatId") String chatId);

    @POST("chat/{chatId}/tin-nhan")
    Call<JsonObject> guiTinNhan(@Path("chatId") String chatId, @Body ChatMessageRequest body);
}
