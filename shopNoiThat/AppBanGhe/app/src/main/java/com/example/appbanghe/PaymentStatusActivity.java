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
import com.example.appbanghe.network.services.PaymentService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PaymentStatusActivity extends AppCompatActivity {

    private RecyclerView rvPaymentHistory;
    private PaymentStatusAdapter adapter;
    private TextView tvTotalAmount;
    private TextView tvCompletedPayments;
    private TextView tvPendingPayments;
    private TextView tvFailedPayments;
    private LinearLayout llContent;
    private LinearLayout llEmpty;
    private Button btnRefresh;
    private Button btnRetryPayment;
    private ImageView btnBack;

    private List<JsonObject> paymentHistory = new ArrayList<>();
    private String selectedOrderId;
    private double selectedAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_status);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadPaymentHistory();
        loadPaymentStatistics();
    }

    private void initViews() {
        rvPaymentHistory = findViewById(R.id.rv_payment_history);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvCompletedPayments = findViewById(R.id.tv_completed_payments);
        tvPendingPayments = findViewById(R.id.tv_pending_payments);
        tvFailedPayments = findViewById(R.id.tv_failed_payments);
        llContent = findViewById(R.id.ll_content);
        llEmpty = findViewById(R.id.ll_empty);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnRetryPayment = findViewById(R.id.btn_retry_payment);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new PaymentStatusAdapter(this, paymentHistory, new PaymentStatusAdapter.Listener() {
            @Override
            public void onPaymentClick(String orderId, double amount, String status) {
                showPaymentDetail(orderId, amount, status);
            }

            @Override
            public void onRetryPayment(String orderId, double amount) {
                retryPayment(orderId, amount);
            }

            @Override
            public void onViewReceipt(String orderId) {
                viewPaymentReceipt(orderId);
            }
        });

        rvPaymentHistory.setLayoutManager(new LinearLayoutManager(this));
        rvPayments.setAdapter(adapter);
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            refreshData();
        });

        btnRetryPayment.setOnClickListener(v -> {
            if (selectedOrderId != null) {
                retryPayment(selectedOrderId, selectedAmount);
            } else {
                Toast.makeText(this, "Vui lòng chọn đơn hàng để thanh toán lại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadPaymentHistory() {
        showLoading(true);

        PaymentService service = ApiClient.createService(this, PaymentService.class);
        service.getPaymentHistory(1, 50).enqueue(new Callback<JsonObject>() {
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
                        JsonArray payments = responseBody.getAsJsonArray("payments");
                        paymentHistory.clear();

                        for (int i = 0; i < payments.size(); i++) {
                            paymentHistory.add(payments.get(i).getAsJsonObject());
                        }

                        adapter.notifyDataSetChanged();
                        updateUI();
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

    private void loadPaymentStatistics() {
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        service.getPaymentStatistics().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject stats = responseBody.getAsJsonObject("data");
                        updateStatistics(stats);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void updateStatistics(JsonObject stats) {
        if (stats.has("tong_tien")) {
            double totalAmount = stats.get("tong_tien").getAsDouble();
            tvTotalAmount.setText(formatCurrency(totalAmount));
        }

        if (stats.has("so_thanh_toan_hoan_tat")) {
            int completed = stats.get("so_thanh_toan_hoan_tat").getAsInt();
            tvCompletedPayments.setText(String.valueOf(completed));
        }

        if (stats.has("so_thanh_toan_dang_xu_ly")) {
            int pending = stats.get("so_thanh_toan_dang_xu_ly").getAsInt();
            tvPendingPayments.setText(String.valueOf(pending));
        }

        if (stats.has("so_thanh_toan_that_bai")) {
            int failed = stats.get("so_thanh_toan_that_bai").getAsInt();
            tvFailedPayments.setText(String.valueOf(failed));
        }
    }

    private void showPaymentDetail(String orderId, double amount, String status) {
        selectedOrderId = orderId;
        selectedAmount = amount;

        // Enable/disable retry button based on status
        btnRetryPayment.setEnabled("THAT_BAI".equals(status) || "HET_HAN".equals(status));

        // Show payment detail dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_payment_detail, null);
        
        TextView tvOrderId = dialogView.findViewById(R.id.tv_order_id);
        TextView tvAmount = dialogView.findViewById(R.id.tv_amount);
        TextView tvStatus = dialogView.findViewById(R.id.tv_status);
        TextView tvPaymentMethod = dialogView.findViewById(R.id.tv_payment_method);
        TextView tvPaymentDate = dialogView.findViewById(R.id.tv_payment_date);
        TextView tvTransactionId = dialogView.findViewById(R.id.tv_transaction_id);
        Button btnViewReceipt = dialogView.findViewById(R.id.btn_view_receipt);
        Button btnClose = dialogView.findViewById(R.id.btn_close);

        tvOrderId.setText("Đơn hàng: #" + orderId);
        tvAmount.setText(formatCurrency(amount));
        tvStatus.setText(getStatusText(status));
        tvStatus.setTextColor(getStatusColor(status));

        // Load detailed payment info
        loadPaymentDetail(orderId, dialogView);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        btnViewReceipt.setOnClickListener(v -> {
            viewPaymentReceipt(orderId);
        });

        btnClose.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void loadPaymentDetail(String orderId, View dialogView) {
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        service.getPaymentStatus(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null) {
                        TextView tvPaymentMethod = dialogView.findViewById(R.id.tv_payment_method);
                        TextView tvPaymentDate = dialogView.findViewById(R.id.tv_payment_date);
                        TextView tvTransactionId = dialogView.findViewById(R.id.tv_transaction_id);

                        tvPaymentMethod.setText(getPaymentMethodText(responseBody.get("paymentMethod").getAsString()));
                        
                        if (responseBody.has("orderDate")) {
                            String orderDate = responseBody.get("orderDate").getAsString();
                            tvPaymentDate.setText(formatDate(orderDate));
                        }

                        if (responseBody.has("transactionId")) {
                            String transactionId = responseBody.get("transactionId").getAsString();
                            tvTransactionId.setText(transactionId);
                        } else {
                            tvTransactionId.setText("Chưa có");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void retryPayment(String orderId, double amount) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("order_id", orderId);
        intent.putExtra("amount", amount);
        intent.putExtra("order_info", "Thanh toán lại đơn hàng #" + orderId);
        startActivityForResult(intent, 1001);
    }

    private void viewPaymentReceipt(String orderId) {
        Intent intent = new Intent(this, PaymentReceiptActivity.class);
        intent.putExtra("order_id", orderId);
        startActivity(intent);
    }

    private void refreshData() {
        paymentHistory.clear();
        adapter.notifyDataSetChanged();
        loadPaymentHistory();
        loadPaymentStatistics();
    }

    private void updateUI() {
        if (paymentHistory.isEmpty()) {
            showEmptyState();
        } else {
            showContent();
        }
    }

    private void showContent() {
        llContent.setVisibility(View.VISIBLE);
        llEmpty.setVisibility(View.GONE);
    }

    private void showEmptyState() {
        llContent.setVisibility(View.GONE);
        llEmpty.setVisibility(View.VISIBLE);
    }

    private void showLoading(boolean isLoading) {
        btnRefresh.setEnabled(!isLoading);
        btnRefresh.setText(isLoading ? "Đang tải..." : "Làm mới");
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        llEmpty.setVisibility(View.VISIBLE);
        llEmpty.findViewById(R.id.tv_empty_message).setText(message + "\n\nNhấn làm mới để thử lại.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Payment was successful, refresh data
            refreshData();
        }
    }

    // Helper methods
    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "DA_THANH_TOAN": return "Đã thanh toán";
            case "CHUA_THANH_TOAN": return "Chưa thanh toán";
            case "DANG_XU_LY": return "Đang xử lý";
            case "THAT_BAI": return "Thất bại";
            case "HET_HAN": return "Hết hạn";
            default: return status;
        }
    }

    private int getStatusColor(String status) {
        switch (status) {
            case "DA_THANH_TOAN": return getResources().getColor(android.R.color.holo_green_dark);
            case "CHUA_THANH_TOAN": 
            case "DANG_XU_LY": return getResources().getColor(android.R.color.holo_orange_dark);
            case "THAT_BAI": 
            case "HET_HAN": return getResources().getColor(android.R.color.holo_red_dark);
            default: return getResources().getColor(android.R.color.primary_text_light);
        }
    }

    private String getPaymentMethodText(String method) {
        switch (method) {
            case "COD": return "Thanh toán khi nhận hàng";
            case "VNPAY": return "Ví điện tử VNPAY";
            case "VISA": return "Thẻ Visa/Mastercard";
            case "PAYPAL": return "PayPal";
            default: return method;
        }
    }

    private String formatDate(String dateString) {
        try {
            // Format date string to local format
            return dateString; // Simplified for now
        } catch (Exception e) {
            return dateString;
        }
    }
}
