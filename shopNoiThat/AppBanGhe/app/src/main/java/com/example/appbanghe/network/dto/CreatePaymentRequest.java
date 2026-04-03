package com.example.appbanghe.network.dto;

public class CreatePaymentRequest {
    public String orderId;
    public double amount;
    public String orderInfo;
    public String returnUrl;
    public String cancelUrl;
    public String ipAddr;

    public CreatePaymentRequest(String orderId, double amount, String orderInfo) {
        this.orderId = orderId;
        this.amount = amount;
        this.orderInfo = orderInfo;
    }

    public CreatePaymentRequest(String orderId, double amount, String orderInfo, String returnUrl, String cancelUrl) {
        this.orderId = orderId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.returnUrl = returnUrl;
        this.cancelUrl = cancelUrl;
    }
}
