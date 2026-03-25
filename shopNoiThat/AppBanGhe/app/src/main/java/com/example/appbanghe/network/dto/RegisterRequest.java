package com.example.appbanghe.network.dto;

public class RegisterRequest {
    public String hoTen;
    public String email;
    public String soDienThoai;
    public String matKhau;

    public RegisterRequest(String hoTen, String email, String soDienThoai, String matKhau) {
        this.hoTen = hoTen;
        this.email = email;
        this.soDienThoai = soDienThoai;
        this.matKhau = matKhau;
    }
}
