package com.example.appbanghe.network.dto;

public class ChatMessageRequest {
    public String nguoiDungId;
    public String adminId;
    public String tinNhan;
    public String loaiTinNhan;
    public String sanPhamId;
    public String guiBoi;
    public String noiDung; // For backward compatibility

    public ChatMessageRequest(String noiDung) {
        this.noiDung = noiDung;
        this.tinNhan = noiDung;
        this.loaiTinNhan = "TEXT";
        this.guiBoi = "KHACH_HANG";
    }

    public ChatMessageRequest(String tinNhan, String loaiTinNhan, String guiBoi) {
        this.tinNhan = tinNhan;
        this.noiDung = tinNhan; // For backward compatibility
        this.loaiTinNhan = loaiTinNhan;
        this.guiBoi = guiBoi;
    }

    public ChatMessageRequest(String nguoiDungId, String adminId, String tinNhan, String loaiTinNhan, String guiBoi) {
        this.nguoiDungId = nguoiDungId;
        this.adminId = adminId;
        this.tinNhan = tinNhan;
        this.noiDung = tinNhan; // For backward compatibility
        this.loaiTinNhan = loaiTinNhan;
        this.guiBoi = guiBoi;
    }

    public ChatMessageRequest(String nguoiDungId, String adminId, String tinNhan, String loaiTinNhan, String guiBoi, String sanPhamId) {
        this.nguoiDungId = nguoiDungId;
        this.adminId = adminId;
        this.tinNhan = tinNhan;
        this.noiDung = tinNhan; // For backward compatibility
        this.loaiTinNhan = loaiTinNhan;
        this.guiBoi = guiBoi;
        this.sanPhamId = sanPhamId;
    }
}
