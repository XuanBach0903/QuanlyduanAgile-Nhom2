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

import com.bumptech.glide.Glide;
import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.SessionManager;
import com.example.appbanghe.network.services.ProductService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VoucherActivity extends AppCompatActivity {

    private RecyclerView rvVouchers;
    private VoucherAdapter voucherAdapter;
    private TextView tvUserPoints;
    private Button btnEarnPoints;
    private LinearLayout llFeaturedDeals;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher);

        initViews();
        setupRecyclerView();
        loadUserPoints();
        loadVouchers();
        setupFeaturedDeals();
        setupClickListeners();
    }

    private void initViews() {
        rvVouchers = findViewById(R.id.rv_vouchers);
        tvUserPoints = findViewById(R.id.tv_user_points);
        btnEarnPoints = findViewById(R.id.btn_earn_points);
        llFeaturedDeals = findViewById(R.id.ll_featured_deals);
    }

    private void setupRecyclerView() {
        voucherAdapter = new VoucherAdapter(this, new VoucherAdapter.Listener() {
            @Override
            public void onClaimVoucher(JsonObject voucher) {
                claimVoucher(voucher);
            }

            @Override
            public void onUseVoucher(JsonObject voucher) {
                useVoucher(voucher);
            }
        });

        rvVouchers.setLayoutManager(new LinearLayoutManager(this));
        rvVouchers.setAdapter(voucherAdapter);
    }

    private void loadUserPoints() {
        // Simulate loading user points
        int currentPoints = 1250;
        tvUserPoints.setText("Điểm của bạn: " + currentPoints);
    }

    private void loadVouchers() {
        // Simulate voucher data
        com.google.gson.JsonArray vouchers = new com.google.gson.JsonArray();
        
        // Add sample vouchers
        vouchers.add(createSampleVoucher("Giảm 50K", "Đơn hàng từ 300K", 50000, 300000, "DISCOUNT50K"));
        vouchers.add(createSampleVoucher("Giảm 10%", "Tối đa 100K", 10, 0, "PERCENT10"));
        vouchers.add(createSampleVoucher("Freeship", "Đơn hàng từ 100K", 0, 100000, "FREESHIP"));
        vouchers.add(createSampleVoucher("Giảm 200K", "Đơn hàng từ 1M", 200000, 1000000, "DISCOUNT200K"));
        
        voucherAdapter.updateData(vouchers);
    }

    private JsonObject createSampleVoucher(String title, String description, int discountValue, int minOrder, String code) {
        JsonObject voucher = new JsonObject();
        voucher.addProperty("title", title);
        voucher.addProperty("description", description);
        voucher.addProperty("discountValue", discountValue);
        voucher.addProperty("minOrder", minOrder);
        voucher.addProperty("code", code);
        voucher.addProperty("pointsRequired", discountValue / 10);
        voucher.addProperty("claimed", false);
        return voucher;
    }

    private void setupFeaturedDeals() {
        String[] featuredDeals = {
            "Đổi điểm lấy voucher Giảm 100K",
            "Tích điểm đổi quà tặng độc quyền",
            "Tháng sinh nhật - Voucher x2 điểm"
        };

        for (String deal : featuredDeals) {
            View dealView = getLayoutInflater().inflate(R.layout.item_featured_deal, llFeaturedDeals, false);
            TextView tvDealTitle = dealView.findViewById(R.id.tv_deal_title);
            tvDealTitle.setText(deal);
            
            dealView.setOnClickListener(v -> {
                Toast.makeText(this, "Xem chi tiết: " + deal, Toast.LENGTH_SHORT).show();
            });
            
            llFeaturedDeals.addView(dealView);
        }
    }

    private void setupClickListeners() {
        btnEarnPoints.setOnClickListener(v -> {
            showEarnPointsDialog();
        });
    }

    private void claimVoucher(JsonObject voucher) {
        int pointsRequired = voucher.get("pointsRequired").getAsInt();
        String title = voucher.get("title").getAsString();
        
        // Simulate claiming voucher
        voucher.addProperty("claimed", true);
        voucherAdapter.notifyDataSetChanged();
        
        Toast.makeText(this, "Đã đổi " + title + " thành công!", Toast.LENGTH_SHORT).show();
    }

    private void useVoucher(JsonObject voucher) {
        String code = voucher.get("code").getAsString();
        
        // Copy code to clipboard (simplified)
        Toast.makeText(this, "Mã voucher " + code + " đã được sao chép!", Toast.LENGTH_SHORT).show();
        
        // Navigate to main activity to use voucher
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("voucher_code", code);
        startActivity(intent);
    }

    private void showEarnPointsDialog() {
        // Create a simple dialog showing ways to earn points
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Cách tích điểm")
                .setMessage("1. Mua hàng: 1 điểm / 10K VNĐ\n" +
                          "2. Đánh giá sản phẩm: 50 điểm\n" +
                          "3. Giới thiệu bạn bè: 100 điểm\n" +
                          "4. Check-in hàng ngày: 10 điểm")
                .setPositiveButton("OK", null)
                .show();
    }
}
