package com.example.appbanghe.network.dto;

public class CreateOrderRequest {
    public String diaChiGiaoHang;
    public String ghiChu;
    public String phuongThucThanhToan;

    public CreateOrderRequest(String diaChiGiaoHang, String ghiChu, String phuongThucThanhToan) {
        this.diaChiGiaoHang = diaChiGiaoHang;
        this.ghiChu = ghiChu;
        this.phuongThucThanhToan = phuongThucThanhToan;
    }
}
