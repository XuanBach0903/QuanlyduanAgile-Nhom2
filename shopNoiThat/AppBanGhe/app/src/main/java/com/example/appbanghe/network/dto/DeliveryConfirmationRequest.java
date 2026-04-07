package com.example.appbanghe.network.dto;

public class DeliveryConfirmationRequest {
    public String lyDo;

    public DeliveryConfirmationRequest(String lyDo) {
        this.lyDo = lyDo;
    }
}

public class DeliveryRatingRequest {
    public int sao;
    public String noiDung;

    public DeliveryRatingRequest(int sao, String noiDung) {
        this.sao = sao;
        this.noiDung = noiDung;
    }
}

public class DeliveryInfoRequest {
    public String phuongTienGiaoHang;
    public Double phiGiaoHang;

    public DeliveryInfoRequest(String phuongTienGiaoHang, Double phiGiaoHang) {
        this.phuongTienGiaoHang = phuongTienGiaoHang;
        this.phiGiaoHang = phiGiaoHang;
    }
}
