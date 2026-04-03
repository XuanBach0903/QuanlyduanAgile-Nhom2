package com.example.appbanghe;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.CancelOrderRequest;
import com.example.appbanghe.network.services.OrderService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.JsonObject;

public class CancelOrderActivity extends AppCompatActivity {

    private String orderId;
    private String orderInfo;
    private EditText etReason;
    private Button btnCancelOrder;
    private Button btnBack;
    private TextView tvOrderInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cancel_order);

        // Get order info from intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("order_id");
        orderInfo = intent.getStringExtra("order_info");
        
        if (orderId == null) {
            Toast.makeText(this, "Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupOrderInfo();
        setupClickListeners();
    }

    private void initViews() {
        tvOrderInfo = findViewById(R.id.tv_order_info);
        etReason = findViewById(R.id.et_reason);
        btnCancelOrder = findViewById(R.id.btn_cancel_order);
        btnBack = findViewById(R.id.btn_back);

        // Setup predefined reasons
        String[] reasons = {
            "Thay đổi ý định",
            "Tìm được sản phẩm tốt hơn", 
            "Không còn nhu cầu",
            "Vấn đề về thanh toán",
            "Thời gian giao hàng quá lâu",
            "Lý do khác"
        };

        LinearLayout llReasons = findViewById(R.id.ll_reasons);
        
        for (String reason : reasons) {
            Button reasonButton = new Button(this);
            reasonButton.setText(reason);
            reasonButton.setBackgroundResource(android.R.drawable.btn_default);
            reasonButton.setOnClickListener(v -> {
                etReason.setText(reason);
                // Highlight selected button
                for (int i = 0; i < llReasons.getChildCount(); i++) {
                    llReasons.getChildAt(i).setAlpha(0.7f);
                }
                reasonButton.setAlpha(1.0f);
            });
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 8, 0, 8);
            reasonButton.setLayoutParams(params);
            
            llReasons.addView(reasonButton);
        }
    }

    private void setupOrderInfo() {
        if (orderInfo != null) {
            tvOrderInfo.setText(orderInfo);
        } else {
            tvOrderInfo.setText("Đơn hàng #" + orderId);
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnCancelOrder.setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            
            if (reason.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn hoặc nhập lý do hủy đơn", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Confirm cancellation
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận hủy đơn")
                .setMessage("Bạn có chắc chắn muốn hủy đơn hàng với lý do: \"" + reason + "\"?")
                .setPositiveButton("Đồng ý", (dialog, which) -> {
                    performCancelOrder(reason);
                })
                .setNegativeButton("Không", null)
                .show();
        });
    }

    private void performCancelOrder(String reason) {
        // Show loading
        btnCancelOrder.setEnabled(false);
        btnCancelOrder.setText("Đang xử lý...");
        
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.huyDon(orderId, new CancelOrderRequest(reason)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // Restore button
                btnCancelOrder.setEnabled(true);
                btnCancelOrder.setText("Hủy đơn hàng");
                
                if (!response.isSuccessful()) {
                    String errorMsg = "Hủy đơn thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            com.google.gson.JsonObject errorObj = new com.google.gson.Gson().fromJson(errorStr, com.google.gson.JsonObject.class);
                            if (errorObj.has("message")) {
                                errorMsg = errorObj.get("message").getAsString();
                            }
                        }
                    } catch (Exception e) {
                        // Ignore parsing errors
                    }
                    
                    Toast.makeText(CancelOrderActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Success
                Toast.makeText(CancelOrderActivity.this, "Đã hủy đơn hàng thành công", Toast.LENGTH_SHORT).show();
                
                // Return result to parent activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("cancelled", true);
                resultIntent.putExtra("order_id", orderId);
                setResult(RESULT_OK, resultIntent);
                
                finish();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Restore button
                btnCancelOrder.setEnabled(true);
                btnCancelOrder.setText("Hủy đơn hàng");
                
                Toast.makeText(CancelOrderActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
