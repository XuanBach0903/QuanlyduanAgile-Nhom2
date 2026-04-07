package com.example.appbanghe.network.dto;

public class ReviewRequest {
    public int soSao;
    public String noiDung;
    public String orderId;

    public ReviewRequest(int soSao, String noiDung, String orderId) {
        this.soSao = soSao;
        this.noiDung = noiDung;
        this.orderId = orderId;
    }
}
