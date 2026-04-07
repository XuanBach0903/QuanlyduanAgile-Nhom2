package com.example.appbanghe.network.dto;

public class LoginRequest {
    public String email;
    public String matKhau;

    public LoginRequest(String email, String matKhau) {
        this.email = email;
        this.matKhau = matKhau;
    }
}
