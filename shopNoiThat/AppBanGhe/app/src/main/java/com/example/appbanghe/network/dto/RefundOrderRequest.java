package com.example.appbanghe.network.dto;

public class RefundOrderRequest {
    public String lyDo;

    public String lyDoHoan;
    public String reason;

    public VisaInfo visa;

    public VisaInfo visaInfo;
    public VisaInfo thongTinVisa;
    public VisaInfo thongTinThe;
    public VisaInfo theVisa;

    public static class VisaInfo {
        public String soThe;
        public String tenChuThe;
        public int thangHetHan;
        public int namHetHan;
        public String cvv;

        public String soTheVisa;
        public String cardNumber;
        public String soTheNganHang;

        public String cardHolderName;
        public String tenChuTaiKhoan;

        public int expMonth;
        public int expYear;
        public int thang;
        public int nam;

        public String cvc;

        public VisaInfo(String soThe, String tenChuThe, int thangHetHan, int namHetHan, String cvv) {
            this.soThe = soThe;
            this.tenChuThe = tenChuThe;
            this.thangHetHan = thangHetHan;
            this.namHetHan = namHetHan;
            this.cvv = cvv;

            this.soTheVisa = soThe;
            this.cardNumber = soThe;
            this.soTheNganHang = soThe;

            this.cardHolderName = tenChuThe;
            this.tenChuTaiKhoan = tenChuThe;

            this.expMonth = thangHetHan;
            this.expYear = namHetHan;
            this.thang = thangHetHan;
            this.nam = namHetHan;

            this.cvc = cvv;
        }
    }

    public RefundOrderRequest(String lyDo) {
        this(lyDo, null);
    }

    public RefundOrderRequest(String lyDo, VisaInfo visa) {
        this.lyDo = lyDo;
        this.lyDoHoan = lyDo;
        this.reason = lyDo;
        this.visa = visa;
        this.visaInfo = visa;
        this.thongTinVisa = visa;
        this.thongTinThe = visa;
        this.theVisa = visa;
    }
}
