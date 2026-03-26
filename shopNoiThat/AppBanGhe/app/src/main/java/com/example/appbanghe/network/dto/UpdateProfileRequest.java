package com.example.appbanghe.network.dto;

public class UpdateProfileRequest {
    public String hoTen;
    public String soDienThoai;
    public String diaChi;
    public String imgUrl;

    public String ho_ten;
    public String so_dien_thoai;
    public String dia_chi;
    public String img_url;

    public UpdateProfileRequest(String hoTen, String soDienThoai, String diaChi, String imgUrl) {
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.diaChi = diaChi;
        this.imgUrl = imgUrl;

        this.ho_ten = hoTen;
        this.so_dien_thoai = soDienThoai;
        this.dia_chi = diaChi;
        this.img_url = imgUrl;
    }
}
