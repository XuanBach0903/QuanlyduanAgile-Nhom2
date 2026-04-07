package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.CancelOrderRequest;
import com.example.appbanghe.network.services.OrderService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderManagementActivity extends AppCompatActivity {

    private RecyclerView rvOrders;
    private OrderManagementAdapter adapter;
    private TextView tvEmptyState;
    private LinearLayout llContent;
    private Button btnRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_management);

        initViews();
        setupRecyclerView();
        loadOrders();
        setupClickListeners();
    }

    private void initViews() {
        rvOrders = findViewById(R.id.rv_orders);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        llContent = findViewById(R.id.ll_content);
        btnRefresh = findViewById(R.id.btn_refresh);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new OrderManagementAdapter(this, new OrderManagementAdapter.Listener() {
            @Override
            public void onViewDetail(String orderId) {
                viewOrderDetail(orderId);
            }

            @Override
            public void onCancelOrder(String orderId, String orderInfo) {
                showCancelOrderDialog(orderId, orderInfo);
            }

            @Override
            public void onConfirmReceived(String orderId) {
                confirmOrderReceived(orderId);
            }

            @Override
            public void onRequestRefund(String orderId) {
                requestRefund(orderId);
            }
        });

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(adapter);
    }

    private void loadOrders() {
        showLoading(true);
        
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.danhSachDonHangCuaToi().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                showLoading(false);
                
                if (!response.isSuccessful()) {
                    showError("Không thể tải danh sách đơn hàng");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonArray orders = responseBody.getAsJsonArray("data");
                        displayOrders(orders);
                    } else {
                        showEmptyState();
                    }
                } catch (Exception e) {
                    showError("Lỗi xử lý dữ liệu: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showLoading(false);
                showError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void displayOrders(JsonArray orders) {
        if (orders.size() == 0) {
            showEmptyState();
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        llContent.setVisibility(View.VISIBLE);
        adapter.updateOrders(orders);
    }

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        llContent.setVisibility(View.GONE);
    }

    private void showLoading(boolean isLoading) {
        btnRefresh.setEnabled(!isLoading);
        btnRefresh.setText(isLoading ? "Đang tải..." : "Làm mới");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        tvEmptyState.setText(message + "\n\nNhấn làm mới để thử lại.");
        tvEmptyState.setVisibility(View.VISIBLE);
        llContent.setVisibility(View.GONE);
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> loadOrders());
    }

    private void viewOrderDetail(String orderId) {
        Intent intent = new Intent(this, OrderDetailActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }

    private void showCancelOrderDialog(String orderId, String orderInfo) {
        // Check if order can be cancelled
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.chiTietDonHang(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(OrderManagementActivity.this, "Không thể tải thông tin đơn hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject orderData = response.body();
                if (orderData != null && orderData.has("trangThaiDonHang")) {
                    String status = orderData.get("trangThaiDonHang").getAsString();
                    
                    if (!status.equals("CHO_XAC_NHAN") && !status.equals("DA_XAC_NHAN")) {
                        Toast.makeText(OrderManagementActivity.this, 
                            "Không thể hủy đơn hàng ở trạng thái: " + getStatusText(status), 
                            Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Proceed with cancellation
                    Intent intent = new Intent(OrderManagementActivity.this, CancelOrderActivity.class);
                    intent.putExtra("order_id", orderId);
                    intent.putExtra("order_info", orderInfo);
                    startActivityForResult(intent, 1001);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OrderManagementActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmOrderReceived(String orderId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xác nhận đã nhận hàng")
            .setMessage("Bạn có chắc chắn đã nhận được hàng?")
            .setPositiveButton("Đồng ý", (dialog, which) -> {
                performConfirmReceived(orderId);
            })
            .setNegativeButton("Không", null)
            .show();
    }

    private void performConfirmReceived(String orderId) {
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.xacNhanDaNhan(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(OrderManagementActivity.this, "Xác nhận thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                Toast.makeText(OrderManagementActivity.this, "Đã xác nhận nhận hàng", Toast.LENGTH_SHORT).show();
                loadOrders(); // Refresh list
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OrderManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestRefund(String orderId) {
        // Navigate to refund request activity or show dialog
        Toast.makeText(this, "Chức năng yêu cầu hoàn tiền sẽ sớm có!", Toast.LENGTH_SHORT).show();
    }

    private String getStatusText(String status) {
        switch (status) {
            case "CHO_XAC_NHAN": return "Chờ xác nhận";
            case "DA_XAC_NHAN": return "Đã xác nhận";
            case "DANG_GIAO": return "Đang giao";
            case "DA_GIAO_CHO_XAC_NHAN": return "Đã giao - Chờ xác nhận";
            case "THANH_CONG": return "Thành công";
            case "HUY": return "Đã hủy";
            default: return status;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Order was cancelled, refresh the list
            loadOrders();
        }
    }
}
