package com.example.appbanghe.network.dto;

public class AddCartItemRequest {
    public String sanPhamId;
    public int soLuong;

    public AddCartItemRequest(String sanPhamId, int soLuong) {
        this.sanPhamId = sanPhamId;
        this.soLuong = soLuong;
    }
}
