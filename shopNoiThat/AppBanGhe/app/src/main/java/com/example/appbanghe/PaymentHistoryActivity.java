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

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.services.PaymentService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import com.google.gson.JsonObject;

public class PaymentHistoryActivity extends AppCompatActivity {

    private RecyclerView rvPaymentHistory;
    private PaymentHistoryAdapter adapter;
    private TextView tvEmptyState;
    private LinearLayout llContent;
    private Button btnRefresh;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        initViews();
        setupRecyclerView();
        loadPaymentHistory();
        setupClickListeners();
    }

    private void initViews() {
        rvPaymentHistory = findViewById(R.id.rv_payment_history);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        llContent = findViewById(R.id.ll_content);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new PaymentHistoryAdapter(this, new PaymentHistoryAdapter.Listener() {
            @Override
            public void onViewDetail(String orderId) {
                viewPaymentDetail(orderId);
            }

            @Override
            public void onRetryPayment(String orderId, double amount) {
                retryPayment(orderId, amount);
            }
        });

        rvPaymentHistory.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        rvPaymentHistory.setAdapter(adapter);
    }

    private void loadPaymentHistory() {
        showLoading(true);
        
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        service.getPaymentHistory(1, 20).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                showLoading(false);
                
                if (!response.isSuccessful()) {
                    showError("Không thể tải lịch sử thanh toán");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("payments")) {
                        com.google.gson.JsonArray payments = responseBody.getAsJsonArray("payments");
                        displayPaymentHistory(payments);
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

    private void displayPaymentHistory(com.google.gson.JsonArray payments) {
        if (payments.size() == 0) {
            showEmptyState();
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        llContent.setVisibility(View.VISIBLE);
        adapter.updatePayments(payments);
    }

    private void showEmptyState() {
        tvEmptyState.setVisibility(View.VISIBLE);
        llContent.setVisibility(View.GONE);
        tvEmptyState.setText("Chưa có lịch sử thanh toán nào");
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
        btnRefresh.setOnClickListener(v -> loadPaymentHistory());
    }

    private void viewPaymentDetail(String orderId) {
        Intent intent = new Intent(this, PaymentDetailActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }

    private void retryPayment(String orderId, double amount) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("order_id", orderId);
        intent.putExtra("amount", amount);
        intent.putExtra("order_info", "Thanh toán lại đơn hàng #" + orderId);
        startActivityForResult(intent, 1001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Payment was successful, refresh the list
            loadPaymentHistory();
        }
    }
}
