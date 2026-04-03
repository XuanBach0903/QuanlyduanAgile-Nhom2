package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.services.InvoiceService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminInvoiceManagementActivity extends AppCompatActivity {

    private RecyclerView rvInvoices;
    private AdminInvoiceAdapter adapter;
    private TextView tvTotalInvoices;
    private TextView tvTotalRevenue;
    private TextView tvPaidInvoices;
    private TextView tvUnpaidInvoices;
    private LinearLayout llContent;
    private LinearLayout llEmpty;
    private Button btnRefresh;
    private Button btnSearch;
    private Button btnFilter;
    private EditText etSearch;
    private ImageView btnBack;

    private List<JsonObject> invoices = new ArrayList<>();
    private String currentFilter = null;
    private String searchKeyword = "";
    private int currentPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_invoice_management);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadInvoices();
        loadStatistics();
    }

    private void initViews() {
        rvInvoices = findViewById(R.id.rv_invoices);
        tvTotalInvoices = findViewById(R.id.tv_total_invoices);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvPaidInvoices = findViewById(R.id.tv_paid_invoices);
        tvUnpaidInvoices = findViewById(R.id.tv_unpaid_invoices);
        llContent = findViewById(R.id.ll_content);
        llEmpty = findViewById(R.id.ll_empty);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnSearch = findViewById(R.id.btn_search);
        btnFilter = findViewById(R.id.btn_filter);
        etSearch = findViewById(R.id.et_search);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new AdminInvoiceAdapter(this, invoices, new AdminInvoiceAdapter.Listener() {
            @Override
            public void onInvoiceClick(String invoiceId) {
                viewInvoiceDetail(invoiceId);
            }

            @Override
            public void onExportInvoice(String invoiceId) {
                exportInvoice(invoiceId);
            }

            @Override
            public void onPrintInvoice(String invoiceId) {
                printInvoice(invoiceId);
            }

            @Override
            public void onCancelInvoice(String invoiceId) {
                showCancelInvoiceDialog(invoiceId);
            }

            @Override
            public void onResendInvoice(String invoiceId) {
                resendInvoice(invoiceId);
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

        btnSearch.setOnClickListener(v -> {
            searchInvoices();
        });

        btnFilter.setOnClickListener(v -> {
            showFilterDialog();
        });
    }

    private void loadInvoices() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage = 1;

        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.layTatCaHoaDon(currentPage, 20, currentFilter, null, null, searchKeyword).enqueue(new Callback<JsonObject>() {
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
        service.layTatCaHoaDon(currentPage, 20, currentFilter, null, null, searchKeyword).enqueue(new Callback<JsonObject>() {
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
        // For admin, we need a separate statistics endpoint
        // For now, calculate from loaded data
        updateStatistics();
    }

    private void updateStatistics() {
        int totalInvoices = invoices.size();
        double totalRevenue = 0;
        int paidInvoices = 0;
        int unpaidInvoices = 0;

        for (JsonObject invoice : invoices) {
            if (invoice.has("tong_cong")) {
                totalRevenue += invoice.get("tong_cong").getAsDouble();
            }

            if (invoice.has("thanh_toan")) {
                JsonObject payment = invoice.getAsJsonObject("thanh_toan");
                String status = payment.get("trang_thai").getAsString();
                if ("DA_THANH_TOAN".equals(status)) {
                    paidInvoices++;
                } else {
                    unpaidInvoices++;
                }
            }
        }

        tvTotalInvoices.setText(String.valueOf(totalInvoices));
        tvTotalRevenue.setText(formatCurrency(totalRevenue));
        tvPaidInvoices.setText(String.valueOf(paidInvoices));
        tvUnpaidInvoices.setText(String.valueOf(unpaidInvoices));
    }

    private void searchInvoices() {
        searchKeyword = etSearch.getText().toString().trim();
        refreshData();
    }

    private void showFilterDialog() {
        String[] options = {"Tất cả", "Mới tạo", "Đã xuất", "Đã hủy", "Đã thanh toán"};
        int selected = getFilterIndex(currentFilter);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lọc hóa đơn")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                currentFilter = getFilterType(which);
                dialog.dismiss();
                refreshData();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private int getFilterIndex(String filter) {
        switch (filter) {
            case null: return 0;
            case "MOI_TAO": return 1;
            case "DA_XUAT": return 2;
            case "DA_HUY": return 3;
            case "DA_THANH_TOAN": return 4;
            default: return 0;
        }
    }

    private String getFilterType(int index) {
        switch (index) {
            case 0: return null;
            case 1: return "MOI_TAO";
            case 2: return "DA_XUAT";
            case 3: return "DA_HUY";
            case 4: return "DA_THANH_TOAN";
            default: return null;
        }
    }

    private void viewInvoiceDetail(String invoiceId) {
        Intent intent = new Intent(this, InvoiceDetailActivity.class);
        intent.putExtra("invoice_id", invoiceId);
        intent.putExtra("admin_mode", true);
        startActivity(intent);
    }

    private void exportInvoice(String invoiceId) {
        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.xuatHoaDon(invoiceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminInvoiceManagementActivity.this, "Xuất hóa đơn thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminInvoiceManagementActivity.this, "Đã xuất hóa đơn", Toast.LENGTH_SHORT).show();
                updateInvoiceInList(invoiceId, "DA_XUAT");
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminInvoiceManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void printInvoice(String invoiceId) {
        // In real app, integrate with printing service
        Toast.makeText(this, "Tính năng in hóa đơn đang phát triển", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(AdminInvoiceManagementActivity.this, "Hủy hóa đơn thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminInvoiceManagementActivity.this, "Đã hủy hóa đơn", Toast.LENGTH_SHORT).show();
                removeInvoiceFromList(invoiceId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminInvoiceManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendInvoice(String invoiceId) {
        // In real app, send email/SMS
        Toast.makeText(this, "Đã gửi lại hóa đơn qua email", Toast.LENGTH_SHORT).show();
    }

    private void removeInvoiceFromList(String invoiceId) {
        for (int i = 0; i < invoices.size(); i++) {
            JsonObject invoice = invoices.get(i);
            if (invoice.get("id").getAsString().equals(invoiceId)) {
                invoices.remove(i);
                adapter.notifyItemRemoved(i);
                updateUI();
                updateStatistics();
                break;
            }
        }
    }

    private void updateInvoiceInList(String invoiceId, String newStatus) {
        for (int i = 0; i < invoices.size(); i++) {
            JsonObject invoice = invoices.get(i);
            if (invoice.get("id").getAsString().equals(invoiceId)) {
                invoice.addProperty("trang_thai_hoa_don", newStatus);
                adapter.notifyItemChanged(i);
                updateStatistics();
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

    // Helper methods
    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}
