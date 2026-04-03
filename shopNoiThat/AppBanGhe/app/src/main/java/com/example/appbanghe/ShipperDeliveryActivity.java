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

public class ShipperDeliveryActivity extends AppCompatActivity {

    private RecyclerView rvPendingDeliveries;
    private RecyclerView rvMyDeliveries;
    private ShipperDeliveryAdapter pendingAdapter;
    private ShipperDeliveryAdapter myDeliveriesAdapter;
    private TextView tvPendingCount;
    private TextView tvMyDeliveriesCount;
    private TextView tvTodayDeliveries;
    private TextView tvEarnings;
    private LinearLayout llContent;
    private LinearLayout llEmpty;
    private Button btnRefresh;
    private Button btnMap;
    private ImageView btnBack;

    private List<JsonObject> pendingDeliveries = new ArrayList<>();
    private List<JsonObject> myDeliveries = new ArrayList<>();
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shipper_delivery);

        initViews();
        setupRecyclerViews();
        setupClickListeners();
        loadPendingDeliveries();
        loadMyDeliveries();
        loadStatistics();
    }

    private void initViews() {
        rvPendingDeliveries = findViewById(R.id.rv_pending_deliveries);
        rvMyDeliveries = findViewById(R.id.rv_my_deliveries);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        tvMyDeliveriesCount = findViewById(R.id.tv_my_deliveries_count);
        tvTodayDeliveries = findViewById(R.id.tv_today_deliveries);
        tvEarnings = findViewById(R.id.tv_earnings);
        llContent = findViewById(R.id.ll_content);
        llEmpty = findViewById(R.id.ll_empty);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnMap = findViewById(R.id.btn_map);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerViews() {
        // Pending deliveries adapter
        pendingAdapter = new ShipperDeliveryAdapter(this, pendingDeliveries, new ShipperDeliveryAdapter.Listener() {
            @Override
            public void onAcceptDelivery(String confirmationId) {
                acceptDelivery(confirmationId);
            }

            @Override
            public void onViewDetails(String confirmationId) {
                viewDeliveryDetails(confirmationId);
            }

            @Override
            public void onNavigate(String address) {
                navigateToAddress(address);
            }
        });

        rvPendingDeliveries.setLayoutManager(new LinearLayoutManager(this));
        rvPendingDeliveries.setAdapter(pendingAdapter);

        // My deliveries adapter
        myDeliveriesAdapter = new ShipperDeliveryAdapter(this, myDeliveries, new ShipperDeliveryAdapter.Listener() {
            @Override
            public void onCompleteDelivery(String confirmationId) {
                completeDelivery(confirmationId);
            }

            @Override
            public void onViewDetails(String confirmationId) {
                viewDeliveryDetails(confirmationId);
            }

            @Override
            public void onNavigate(String address) {
                navigateToAddress(address);
            }
        });

        rvMyDeliveries.setLayoutManager(new LinearLayoutManager(this));
        rvMyDeliveries.setAdapter(myDeliveriesAdapter);
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            refreshData();
        });

        btnMap.setOnClickListener(v -> {
            openDeliveryMap();
        });
    }

    private void loadPendingDeliveries() {
        if (isLoading) return;
        
        isLoading = true;

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.layDanhSachChoXacNhan(1, 20).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;

                if (!response.isSuccessful()) {
                    showError("Không thể tải danh sách chờ giao");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonArray deliveryArray = responseBody.getAsJsonArray("data");
                        pendingDeliveries.clear();
                        
                        for (int i = 0; i < deliveryArray.size(); i++) {
                            pendingDeliveries.add(deliveryArray.get(i).getAsJsonObject());
                        }
                        
                        pendingAdapter.notifyDataSetChanged();
                        updateUI();
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

    private void loadMyDeliveries() {
        // Load deliveries assigned to current shipper
        // This would require a separate API endpoint or filtering
        // For now, simulate with empty data
        myDeliveries.clear();
        myDeliveriesAdapter.notifyDataSetChanged();
        updateUI();
    }

    private void loadStatistics() {
        // Load shipper statistics
        // This would require a statistics API endpoint
        tvTodayDeliveries.setText("0");
        tvEarnings.setText("0 VNĐ");
    }

    private void acceptDelivery(String confirmationId) {
        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.nhanGiaoHang(confirmationId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ShipperDeliveryActivity.this, "Nhận giao hàng thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(ShipperDeliveryActivity.this, "Nhận giao hàng thành công!", Toast.LENGTH_SHORT).show();
                
                // Move from pending to my deliveries
                moveDeliveryToMyList(confirmationId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ShipperDeliveryActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void completeDelivery(String confirmationId) {
        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.hoanTatGiaoHang(confirmationId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ShipperDeliveryActivity.this, "Hoàn tất giao hàng thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(ShipperDeliveryActivity.this, "Hoàn tất giao hàng thành công!", Toast.LENGTH_SHORT).show();
                
                // Remove from my deliveries
                removeDeliveryFromMyList(confirmationId);
                
                // Update statistics
                updateDeliveryStats();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ShipperDeliveryActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void viewDeliveryDetails(String confirmationId) {
        Intent intent = new Intent(this, DeliveryConfirmationDetailActivity.class);
        intent.putExtra("confirmation_id", confirmationId);
        intent.putExtra("shipper_mode", true);
        startActivity(intent);
    }

    private void navigateToAddress(String address) {
        // Open Google Maps or other navigation app
        Intent intent = new Intent(Intent.ACTION_VIEW, 
            Uri.parse("google.navigation:q=" + Uri.encode(address)));
        intent.setPackage("com.google.android.apps.maps");
        
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            // Fallback to generic map
            intent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("geo:0,0?q=" + Uri.encode(address)));
            startActivity(intent);
        }
    }

    private void openDeliveryMap() {
        // Open map view with all delivery locations
        Toast.makeText(this, "Tính năng bản đồ đang phát triển", Toast.LENGTH_SHORT).show();
    }

    private void moveDeliveryToMyList(String confirmationId) {
        for (int i = 0; i < pendingDeliveries.size(); i++) {
            JsonObject delivery = pendingDeliveries.get(i);
            if (delivery.get("id").getAsString().equals(confirmationId)) {
                pendingDeliveries.remove(i);
                pendingAdapter.notifyItemRemoved(i);
                
                // Add to my deliveries
                delivery.addProperty("trang_thai_shipper", "DANG_GIAO");
                myDeliveries.add(delivery);
                myDeliveriesAdapter.notifyItemInserted(myDeliveries.size() - 1);
                
                updateUI();
                break;
            }
        }
    }

    private void removeDeliveryFromMyList(String confirmationId) {
        for (int i = 0; i < myDeliveries.size(); i++) {
            JsonObject delivery = myDeliveries.get(i);
            if (delivery.get("id").getAsString().equals(confirmationId)) {
                myDeliveries.remove(i);
                myDeliveriesAdapter.notifyItemRemoved(i);
                updateUI();
                break;
            }
        }
    }

    private void updateDeliveryStats() {
        // Update today's deliveries count
        int todayCount = Integer.parseInt(tvTodayDeliveries.getText().toString());
        tvTodayDeliveries.setText(String.valueOf(todayCount + 1));
        
        // Update earnings (simplified calculation)
        String currentEarnings = tvEarnings.getText().toString().replace(" VNĐ", "").replace(",", "");
        double earnings = Double.parseDouble(currentEarnings);
        earnings += 15000; // Example delivery fee
        tvEarnings.setText(String.format("%,.0f VNĐ", earnings));
    }

    private void refreshData() {
        pendingDeliveries.clear();
        myDeliveries.clear();
        pendingAdapter.notifyDataSetChanged();
        myDeliveriesAdapter.notifyDataSetChanged();
        
        loadPendingDeliveries();
        loadMyDeliveries();
        loadStatistics();
    }

    private void updateUI() {
        tvPendingCount.setText(String.valueOf(pendingDeliveries.size()));
        tvMyDeliveriesCount.setText(String.valueOf(myDeliveries.size()));

        if (pendingDeliveries.isEmpty() && myDeliveries.isEmpty()) {
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
}
