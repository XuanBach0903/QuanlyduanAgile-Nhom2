package com.example.appbanghe.network.dto;

public class VisaPaymentRequest {
    public String donHangId;
    public String soThe;
    public String tenChuThe;
    public int thangHetHan;
    public int namHetHan;
    public String cvv;

    public VisaPaymentRequest(String donHangId, String soThe, String tenChuThe, int thangHetHan, int namHetHan, String cvv) {
        this.donHangId = donHangId;
        this.soThe = soThe;
        this.tenChuThe = tenChuThe;
        this.thangHetHan = thangHetHan;
        this.namHetHan = namHetHan;
        this.cvv = cvv;
    }
}
