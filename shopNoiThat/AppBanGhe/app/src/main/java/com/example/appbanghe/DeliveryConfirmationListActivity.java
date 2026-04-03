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
import com.example.appbanghe.network.services.DeliveryConfirmationService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryConfirmationListActivity extends AppCompatActivity {

    private RecyclerView rvConfirmations;
    private DeliveryConfirmationAdapter adapter;
    private TextView tvTotalConfirmations;
    private TextView tvConfirmedCount;
    private TextView tvRejectedCount;
    private TextView tvPendingCount;
    private LinearLayout llContent;
    private LinearLayout llEmpty;
    private Button btnRefresh;
    private Button btnFilter;
    private ImageView btnBack;

    private List<JsonObject> confirmations = new ArrayList<>();
    private String currentFilter = null; // null = all, CHO_XAC_NHAN, DA_XAC_NHAN, TU_CHOI
    private int currentPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_confirmation_list);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadConfirmations();
        loadStatistics();
    }

    private void initViews() {
        rvConfirmations = findViewById(R.id.rv_confirmations);
        tvTotalConfirmations = findViewById(R.id.tv_total_confirmations);
        tvConfirmedCount = findViewById(R.id.tv_confirmed_count);
        tvRejectedCount = findViewById(R.id.tv_rejected_count);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        llContent = findViewById(R.id.ll_content);
        llEmpty = findViewById(R.id.ll_empty);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnFilter = findViewById(R.id.btn_filter);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new DeliveryConfirmationAdapter(this, confirmations, new DeliveryConfirmationAdapter.Listener() {
            @Override
            public void onConfirmationClick(String confirmationId) {
                viewConfirmationDetail(confirmationId);
            }

            @Override
            public void onConfirmDelivery(String confirmationId) {
                confirmDelivery(confirmationId);
            }

            @Override
            public void onRejectDelivery(String confirmationId) {
                showRejectDialog(confirmationId);
            }

            @Override
            public void onRateDelivery(String confirmationId) {
                rateDelivery(confirmationId);
            }
        });

        rvConfirmations.setLayoutManager(new LinearLayoutManager(this));
        rvConfirmations.setAdapter(adapter);

        // Load more on scroll
        rvConfirmations.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        loadMoreConfirmations();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            refreshData();
        });

        btnFilter.setOnClickListener(v -> {
            showFilterDialog();
        });
    }

    private void loadConfirmations() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage = 1;

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.layDanhSachXacNhan(currentPage, 20, currentFilter, null, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;

                if (!response.isSuccessful()) {
                    showError("Không thể tải danh sách xác nhận");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        
                        if (data.has("xac_nhan")) {
                            JsonArray confirmationArray = data.getAsJsonArray("xac_nhan");
                            confirmations.clear();
                            
                            for (int i = 0; i < confirmationArray.size(); i++) {
                                confirmations.add(confirmationArray.get(i).getAsJsonObject());
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

    private void loadMoreConfirmations() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage++;

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.layDanhSachXacNhan(currentPage, 20, currentFilter, null, null).enqueue(new Callback<JsonObject>() {
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
                        
                        if (data.has("xac_nhan")) {
                            JsonArray confirmationArray = data.getAsJsonArray("xac_nhan");
                            int oldSize = confirmations.size();
                            
                            for (int i = 0; i < confirmationArray.size(); i++) {
                                confirmations.add(confirmationArray.get(i).getAsJsonObject());
                            }
                            
                            adapter.notifyItemRangeInserted(oldSize, confirmationArray.size());
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
        // Calculate from loaded data for now
        updateStatistics();
    }

    private void updateStatistics() {
        int totalConfirmations = confirmations.size();
        int confirmedCount = 0;
        int rejectedCount = 0;
        int pendingCount = 0;

        for (JsonObject confirmation : confirmations) {
            String status = confirmation.get("trang_thai_xac_nhan").getAsString();
            switch (status) {
                case "DA_XAC_NHAN":
                    confirmedCount++;
                    break;
                case "TU_CHOI":
                    rejectedCount++;
                    break;
                case "CHO_XAC_NHAN":
                    pendingCount++;
                    break;
            }
        }

        tvTotalConfirmations.setText(String.valueOf(totalConfirmations));
        tvConfirmedCount.setText(String.valueOf(confirmedCount));
        tvRejectedCount.setText(String.valueOf(rejectedCount));
        tvPendingCount.setText(String.valueOf(pendingCount));
    }

    private void showFilterDialog() {
        String[] options = {"Tất cả", "Chờ xác nhận", "Đã xác nhận", "Từ chối"};
        int selected = getFilterIndex(currentFilter);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lọc xác nhận giao hàng")
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
            case "CHO_XAC_NHAN": return 1;
            case "DA_XAC_NHAN": return 2;
            case "TU_CHOI": return 3;
            default: return 0;
        }
    }

    private String getFilterType(int index) {
        switch (index) {
            case 0: return null;
            case 1: return "CHO_XAC_NHAN";
            case 2: return "DA_XAC_NHAN";
            case 3: return "TU_CHOI";
            default: return null;
        }
    }

    private void viewConfirmationDetail(String confirmationId) {
        Intent intent = new Intent(this, DeliveryConfirmationDetailActivity.class);
        intent.putExtra("confirmation_id", confirmationId);
        startActivity(intent);
    }

    private void confirmDelivery(String confirmationId) {
        // In real app, show confirmation dialog first
        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.xacNhanGiaoHang(confirmationId, null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(DeliveryConfirmationListActivity.this, "Xác nhận thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DeliveryConfirmationListActivity.this, "Xác nhận giao hàng thành công", Toast.LENGTH_SHORT).show();
                updateConfirmationInList(confirmationId, "DA_XAC_NHAN");
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(DeliveryConfirmationListActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectDialog(String confirmationId) {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Nhập lý do từ chối");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Từ chối giao hàng")
            .setView(etReason)
            .setPositiveButton("Từ chối", (dialog, which) -> {
                String reason = etReason.getText().toString().trim();
                rejectDelivery(confirmationId, reason);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void rejectDelivery(String confirmationId, String reason) {
        DeliveryConfirmationRequest request = new DeliveryConfirmationRequest(reason);

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.tuChoiGiaoHang(confirmationId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(DeliveryConfirmationListActivity.this, "Từ chối thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DeliveryConfirmationListActivity.this, "Đã từ chối giao hàng", Toast.LENGTH_SHORT).show();
                updateConfirmationInList(confirmationId, "TU_CHOI");
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(DeliveryConfirmationListActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rateDelivery(String confirmationId) {
        Intent intent = new Intent(this, DeliveryRatingActivity.class);
        intent.putExtra("confirmation_id", confirmationId);
        startActivityForResult(intent, 1001);
    }

    private void updateConfirmationInList(String confirmationId, String newStatus) {
        for (int i = 0; i < confirmations.size(); i++) {
            JsonObject confirmation = confirmations.get(i);
            if (confirmation.get("id").getAsString().equals(confirmationId)) {
                confirmation.addProperty("trang_thai_xac_nhan", newStatus);
                adapter.notifyItemChanged(i);
                updateStatistics();
                break;
            }
        }
    }

    private void refreshData() {
        confirmations.clear();
        adapter.notifyDataSetChanged();
        loadConfirmations();
        loadStatistics();
    }

    private void updateUI() {
        if (confirmations.isEmpty()) {
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
            // Rating completed
            refreshData();
        }
    }
}
