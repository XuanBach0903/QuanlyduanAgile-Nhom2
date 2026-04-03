package com.example.appbanghe.network.dto;

public class CreateDeliveryConfirmationRequest {
    public String donHangId;
    public NguoiNhanHang nguoiNhanHang;

    public CreateDeliveryConfirmationRequest(String donHangId, NguoiNhanHang nguoiNhanHang) {
        this.donHangId = donHangId;
        this.nguoiNhanHang = nguoiNhanHang;
    }

    public static class NguoiNhanHang {
        public String ten;
        public String soDienThoai;
        public String ghiChu;

        public NguoiNhanHang(String ten, String soDienThoai, String ghiChu) {
            this.ten = ten;
            this.soDienThoai = soDienThoai;
            this.ghiChu = ghiChu;
        }
    }
}
