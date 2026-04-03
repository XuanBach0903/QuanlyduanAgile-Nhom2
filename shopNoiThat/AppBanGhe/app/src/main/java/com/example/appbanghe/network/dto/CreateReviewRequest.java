package com.example.appbanghe.network.dto;

import java.util.List;

public class CreateReviewRequest {
    public String sanPhamId;
    public String donHangId;
    public int soSao;
    public String noiDung;
    public List<String> hinhAnh;
    public String loaiDanhGia;

    public CreateReviewRequest(String sanPhamId, String donHangId, int soSao, String noiDung) {
        this.sanPhamId = sanPhamId;
        this.donHangId = donHangId;
        this.soSao = soSao;
        this.noiDung = noiDung;
        this.loaiDanhGia = "SAN_PHAM";
    }

    public CreateReviewRequest(String sanPhamId, String donHangId, int soSao, String noiDung, List<String> hinhAnh) {
        this.sanPhamId = sanPhamId;
        this.donHangId = donHangId;
        this.soSao = soSao;
        this.noiDung = noiDung;
        this.hinhAnh = hinhAnh;
        this.loaiDanhGia = "SAN_PHAM";
    }
}
