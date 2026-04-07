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
import com.example.appbanghe.network.dto.CreateInvoiceRequest;
import com.example.appbanghe.network.services.InvoiceService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceListActivity extends AppCompatActivity {

    private RecyclerView rvInvoices;
    private InvoiceAdapter adapter;
    private TextView tvTotalInvoices;
    private TextView tvTotalAmount;
    private TextView tvPaidInvoices;
    private TextView tvUnpaidInvoices;
    private LinearLayout llContent;
    private LinearLayout llEmpty;
    private Button btnRefresh;
    private Button btnCreateInvoice;
    private ImageView btnBack;

    private List<JsonObject> invoices = new ArrayList<>();
    private String currentFilter = null; // null = all, MOI_TAO, DA_XUAT, DA_HUY, DA_THANH_TOAN
    private int currentPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_list);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadInvoices();
        loadStatistics();
    }

    private void initViews() {
        rvInvoices = findViewById(R.id.rv_invoices);
        tvTotalInvoices = findViewById(R.id.tv_total_invoices);
        tvTotalAmount = findViewById(R.id.tv_total_amount);
        tvPaidInvoices = findViewById(R.id.tv_paid_invoices);
        tvUnpaidInvoices = findViewById(R.id.tv_unpaid_invoices);
        llContent = findViewById(R.id.ll_content);
        llEmpty = findViewById(R.id.ll_empty);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnCreateInvoice = findViewById(R.id.btn_create_invoice);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new InvoiceAdapter(this, invoices, new InvoiceAdapter.Listener() {
            @Override
            public void onInvoiceClick(String invoiceId) {
                viewInvoiceDetail(invoiceId);
            }

            @Override
            public void onCreateInvoice(String orderId) {
                createInvoiceFromOrder(orderId);
            }

            @Override
            public void onDownloadPDF(String invoiceId) {
                downloadInvoicePDF(invoiceId);
            }

            @Override
            public void onCancelInvoice(String invoiceId) {
                showCancelInvoiceDialog(invoiceId);
            }

            @Override
            public void onPayInvoice(String invoiceId) {
                payInvoice(invoiceId);
            }
        });

        rvInvoices.setLayoutManager(new LinearLayoutManager(this));
        rvInvoices.setAdapter(adapter);

        // Load more on scroll
        rvInvoices.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreInvoices();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            refreshData();
        });

        btnCreateInvoice.setOnClickListener(v -> {
            showCreateInvoiceDialog();
        });
    }

    private void loadInvoices() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage = 1;

        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.layDanhSachHoaDon(currentPage, 20, currentFilter, null, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;

                if (!response.isSuccessful()) {
                    showError("Không thể tải danh sách hóa đơn");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        
                        if (data.has("hoa_don")) {
                            JsonArray invoiceArray = data.getAsJsonArray("hoa_don");
                            invoices.clear();
                            
                            for (int i = 0; i < invoiceArray.size(); i++) {
                                invoices.add(invoiceArray.get(i).getAsJsonObject());
                            }
                            
                            adapter.notifyDataSetChanged();
                            updateUI();
                        }
                    }
                } catch (Exception e) {
                    showError("Lỗi xử lý dữ liệu: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading = false;
                showError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void loadMoreInvoices() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage++;

        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.layDanhSachHoaDon(currentPage, 20, currentFilter, null, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;

                if (!response.isSuccessful()) {
                    currentPage--; // Reset page on error
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        
                        if (data.has("hoa_don")) {
                            JsonArray invoiceArray = data.getAsJsonArray("hoa_don");
                            int oldSize = invoices.size();
                            
                            for (int i = 0; i < invoiceArray.size(); i++) {
                                invoices.add(invoiceArray.get(i).getAsJsonObject());
                            }
                            
                            adapter.notifyItemRangeInserted(oldSize, invoiceArray.size());
                        }
                    }
                } catch (Exception e) {
                    currentPage--; // Reset page on error
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoading = false;
                currentPage--; // Reset page on error
            }
        });
    }

    private void loadStatistics() {
        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.layThongKeHoaDon(null, null).enqueue(new Callback<JsonObject>() {
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
        if (stats.has("tong_hoa_don")) {
            int totalInvoices = stats.get("tong_hoa_don").getAsInt();
            tvTotalInvoices.setText(String.valueOf(totalInvoices));
        }

        if (stats.has("tong_tien")) {
            double totalAmount = stats.get("tong_tien").getAsDouble();
            tvTotalAmount.setText(formatCurrency(totalAmount));
        }

        if (stats.has("hoa_don_da_thanh_toan")) {
            int paidInvoices = stats.get("hoa_don_da_thanh_toan").getAsInt();
            tvPaidInvoices.setText(String.valueOf(paidInvoices));
        }

        if (stats.has("hoa_don_chua_thanh_toan")) {
            int unpaidInvoices = stats.get("hoa_don_chua_thanh_toan").getAsInt();
            tvUnpaidInvoices.setText(String.valueOf(unpaidInvoices));
        }
    }

    private void showCreateInvoiceDialog() {
        Intent intent = new Intent(this, OrderListActivity.class);
        intent.putExtra("select_for_invoice", true);
        startActivityForResult(intent, 1001);
    }

    private void createInvoiceFromOrder(String orderId) {
        CreateInvoiceRequest request = new CreateInvoiceRequest(orderId);
        
        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.taoHoaDon(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String errorMsg = "Tạo hóa đơn thất bại";
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
                    
                    Toast.makeText(InvoiceListActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.get("success").getAsBoolean()) {
                        Toast.makeText(InvoiceListActivity.this, "Tạo hóa đơn thành công!", Toast.LENGTH_SHORT).show();
                        refreshData();
                    } else {
                        String message = responseBody.get("message").getAsString();
                        Toast.makeText(InvoiceListActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(InvoiceListActivity.this, "Lỗi xử lý kết quả", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(InvoiceListActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void viewInvoiceDetail(String invoiceId) {
        Intent intent = new Intent(this, InvoiceDetailActivity.class);
        intent.putExtra("invoice_id", invoiceId);
        startActivity(intent);
    }

    private void downloadInvoicePDF(String invoiceId) {
        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.taiHoaDonPDF(invoiceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(InvoiceListActivity.this, "Không thể tải PDF", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.get("success").getAsBoolean()) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        String downloadUrl = data.get("download_url").getAsString();
                        String fileName = data.get("file_name").getAsString();
                        
                        Toast.makeText(InvoiceListActivity.this, "Đã tạo PDF: " + fileName, Toast.LENGTH_SHORT).show();
                        
                        // In real app, download the file
                        // For now, just show success message
                    }
                } catch (Exception e) {
                    Toast.makeText(InvoiceListActivity.this, "Lỗi xử lý PDF", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(InvoiceListActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showCancelInvoiceDialog(String invoiceId) {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Nhập lý do hủy hóa đơn");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hủy hóa đơn")
            .setView(etReason)
            .setPositiveButton("Hủy", (dialog, which) -> {
                String reason = etReason.getText().toString().trim();
                cancelInvoice(invoiceId, reason);
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void cancelInvoice(String invoiceId, String reason) {
        JsonObject request = new JsonObject();
        request.addProperty("ly_do", reason);

        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.huyHoaDon(invoiceId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(InvoiceListActivity.this, "Hủy hóa đơn thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(InvoiceListActivity.this, "Đã hủy hóa đơn", Toast.LENGTH_SHORT).show();
                removeInvoiceFromList(invoiceId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(InvoiceListActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void payInvoice(String invoiceId) {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("invoice_id", invoiceId);
        startActivityForResult(intent, 1002);
    }

    private void removeInvoiceFromList(String invoiceId) {
        for (int i = 0; i < invoices.size(); i++) {
            JsonObject invoice = invoices.get(i);
            if (invoice.get("id").getAsString().equals(invoiceId)) {
                invoices.remove(i);
                adapter.notifyItemRemoved(i);
                updateUI();
                break;
            }
        }
    }

    private void refreshData() {
        invoices.clear();
        adapter.notifyDataSetChanged();
        loadInvoices();
        loadStatistics();
    }

    private void updateUI() {
        if (invoices.isEmpty()) {
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

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        llEmpty.setVisibility(View.VISIBLE);
        llEmpty.findViewById(R.id.tv_empty_message).setText(message + "\n\nNhấn làm mới để thử lại.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Order selected for invoice creation
            String orderId = data.getStringExtra("selected_order_id");
            if (orderId != null) {
                createInvoiceFromOrder(orderId);
            }
        } else if (requestCode == 1002 && resultCode == RESULT_OK) {
            // Payment completed
            refreshData();
        }
    }

    // Helper methods
    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}
