package com.example.appbanghe;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.CreatePaymentRequest;
import com.example.appbanghe.network.services.PaymentService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.JsonObject;

public class PaymentActivity extends AppCompatActivity {

    private String orderId;
    private double amount;
    private String orderInfo;
    
    private TextView tvOrderId;
    private TextView tvAmount;
    private TextView tvOrderInfo;
    private Button btnCodPayment;
    private Button btnVisaPayment;
    private Button btnPaypalPayment;
    private LinearLayout llPaymentMethods;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        // Get order info from intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("order_id");
        amount = intent.getDoubleExtra("amount", 0);
        orderInfo = intent.getStringExtra("order_info");
        
        if (orderId == null || amount == 0) {
            Toast.makeText(this, "Không có thông tin thanh toán", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupOrderInfo();
        setupClickListeners();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tv_order_id);
        tvAmount = findViewById(R.id.tv_amount);
        tvOrderInfo = findViewById(R.id.tv_order_info);
        btnCodPayment = findViewById(R.id.btn_cod_payment);
        btnVisaPayment = findViewById(R.id.btn_visa_payment);
        btnPaypalPayment = findViewById(R.id.btn_paypal_payment);
        llPaymentMethods = findViewById(R.id.ll_payment_methods);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupOrderInfo() {
        tvOrderId.setText("Đơn hàng: #" + orderId);
        tvAmount.setText(formatCurrency(amount));
        tvOrderInfo.setText(orderInfo != null ? orderInfo : "Thanh toán đơn hàng #" + orderId);
    }

    private void setupClickListeners() {
        btnCodPayment.setOnClickListener(v -> {
            showCodConfirmationDialog();
        });

        btnVisaPayment.setOnClickListener(v -> {
            processVisaPayment();
        });

        btnPaypalPayment.setOnClickListener(v -> {
            processPaypalPayment();
        });
    }

    private void showCodConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận thanh toán COD")
            .setMessage("Bạn sẽ thanh toán khi nhận hàng. Tiền mặt: " + formatCurrency(amount))
            .setPositiveButton("Đồng ý", (dialog, which) -> {
                processCodPayment();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void processCodPayment() {
        showLoading(true);
        
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        CreatePaymentRequest request = new CreatePaymentRequest(orderId, amount, orderInfo);
        
        service.confirmCodPayment(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                showLoading(false);
                
                if (!response.isSuccessful()) {
                    showError("Xác nhận thanh toán COD thất bại");
                    return;
                }

                try {
                    JsonObject result = response.body();
                    if (result != null && result.get("success").getAsBoolean()) {
                        showPaymentSuccess("COD", result.get("message").getAsString());
                    } else {
                        showError(result.get("message").getAsString());
                    }
                } catch (Exception e) {
                    showError("Lỗi xử lý kết quả");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showLoading(false);
                showError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void processVisaPayment() {
        showLoading(true);
        
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        CreatePaymentRequest request = new CreatePaymentRequest(
            orderId, 
            amount, 
            orderInfo,
            "app://payment/return",
            "app://payment/cancel"
        );
        
        // Add IP address
        request.ipAddr = getLocalIpAddress();
        
        service.createVnpayPayment(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                showLoading(false);
                
                if (!response.isSuccessful()) {
                    showError("Tạo thanh toán VNPAY thất bại");
                    return;
                }

                try {
                    JsonObject result = response.body();
                    if (result != null && result.get("success").getAsBoolean()) {
                        String paymentUrl = result.get("paymentUrl").getAsString();
                        openPaymentUrl(paymentUrl, "VNPAY");
                    } else {
                        showError(result.get("message").getAsString());
                    }
                } catch (Exception e) {
                    showError("Lỗi xử lý kết quả");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showLoading(false);
                showError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void processPaypalPayment() {
        showLoading(true);
        
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        CreatePaymentRequest request = new CreatePaymentRequest(
            orderId, 
            amount, 
            orderInfo,
            "app://payment/paypal/return",
            "app://payment/paypal/cancel"
        );
        
        service.createPaypalPayment(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                showLoading(false);
                
                if (!response.isSuccessful()) {
                    showError("Tạo thanh toán PayPal thất bại");
                    return;
                }

                try {
                    JsonObject result = response.body();
                    if (result != null && result.get("success").getAsBoolean()) {
                        String paymentUrl = result.get("paymentUrl").getAsString();
                        openPaymentUrl(paymentUrl, "PayPal");
                    } else {
                        showError(result.get("message").getAsString());
                    }
                } catch (Exception e) {
                    showError("Lỗi xử lý kết quả");
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showLoading(false);
                showError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void openPaymentUrl(String url, String provider) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
            
            // Show instruction to user
            Toast.makeText(this, 
                "Đang mở " + provider + " để thanh toán. Vui lòng quay lại ứng dụng sau khi hoàn tất.", 
                Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            showError("Không thể mở trang thanh toán");
        }
    }

    private void showPaymentSuccess(String method, String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Thanh toán thành công")
            .setMessage(message)
            .setPositiveButton("OK", (dialog, which) -> {
                // Return success to previous activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("payment_success", true);
                resultIntent.putExtra("payment_method", method);
                resultIntent.putExtra("order_id", orderId);
                setResult(RESULT_OK, resultIntent);
                finish();
            })
            .setCancelable(false)
            .show();
    }

    private void showError(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lỗi thanh toán")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }

    private void showLoading(boolean isLoading) {
        btnCodPayment.setEnabled(!isLoading);
        btnVisaPayment.setEnabled(!isLoading);
        btnPaypalPayment.setEnabled(!isLoading);
        
        if (isLoading) {
            btnCodPayment.setText("Đang xử lý...");
            btnVisaPayment.setText("Đang xử lý...");
            btnPaypalPayment.setText("Đang xử lý...");
        } else {
            btnCodPayment.setText("Thanh toán COD");
            btnVisaPayment.setText("Thanh toán VNPAY");
            btnPaypalPayment.setText("Thanh toán PayPal");
        }
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    private String getLocalIpAddress() {
        // Simple implementation - in real app, get actual IP
        return "127.0.0.1";
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        
        // Handle payment return from browser
        Uri uri = intent.getData();
        if (uri != null) {
            handlePaymentReturn(uri);
        }
    }

    private void handlePaymentReturn(Uri uri) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        
        if ("app".equals(scheme) && "payment".equals(host)) {
            String path = uri.getPath();
            
            if ("/return".equals(path)) {
                // Payment success
                String orderId = uri.getQueryParameter("orderId");
                String method = uri.getQueryParameter("method");
                
                showPaymentSuccess(method != null ? method : "Unknown", 
                    "Thanh toán thành công cho đơn hàng #" + orderId);
            } else if ("/cancel".equals(path)) {
                // Payment cancelled
                showError("Thanh toán đã bị hủy");
            }
        }
    }
}
