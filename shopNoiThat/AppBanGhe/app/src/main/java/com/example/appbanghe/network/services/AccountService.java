package com.example.appbanghe.network.services;

import com.example.appbanghe.network.dto.ChangePasswordRequest;
import com.example.appbanghe.network.dto.LoginRequest;
import com.example.appbanghe.network.dto.RegisterRequest;
import com.example.appbanghe.network.dto.UpdateProfileRequest;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;

public interface AccountService {
<<<<<<< HEAD

=======
// fix: sửa lỗi tìm kiếm sản phẩm
>>>>>>> develop
    @POST("tai-khoan/dang-ky")
    Call<JsonObject> dangKy(@Body RegisterRequest body);

    @POST("tai-khoan/dang-nhap")
    Call<JsonObject> dangNhap(@Body LoginRequest body);

    @POST("tai-khoan/dang-xuat")
    Call<JsonObject> dangXuat();

    @GET("tai-khoan/toi")
    Call<JsonObject> toi();

    @PATCH("tai-khoan/toi")
    Call<JsonObject> capNhatToi(@Body UpdateProfileRequest body);

    @POST("tai-khoan/doi-mat-khau")
    Call<JsonObject> doiMatKhau(@Body ChangePasswordRequest body);
}
