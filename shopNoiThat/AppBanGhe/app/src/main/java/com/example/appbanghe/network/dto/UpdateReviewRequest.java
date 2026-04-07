package com.example.appbanghe.network.dto;

public class UpdateReviewRequest {
    public int soSao;
    public String noiDung;
    public List<String> hinhAnh;

    public UpdateReviewRequest(int soSao, String noiDung) {
        this.soSao = soSao;
        this.noiDung = noiDung;
    }

    public UpdateReviewRequest(int soSao, String noiDung, List<String> hinhAnh) {
        this.soSao = soSao;
        this.noiDung = noiDung;
        this.hinhAnh = hinhAnh;
    }
}
