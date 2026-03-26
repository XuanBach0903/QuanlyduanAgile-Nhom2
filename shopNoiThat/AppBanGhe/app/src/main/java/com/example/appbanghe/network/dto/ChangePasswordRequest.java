package com.example.appbanghe.network.dto;

public class ChangePasswordRequest {
    public String matKhauCu;
    public String matKhauMoi;
    public String xacNhanMatKhau;

    public ChangePasswordRequest(String matKhauCu, String matKhauMoi, String xacNhanMatKhau) {
        this.matKhauCu = matKhauCu;
        this.matKhauMoi = matKhauMoi;
        this.xacNhanMatKhau = xacNhanMatKhau;
    }
}
