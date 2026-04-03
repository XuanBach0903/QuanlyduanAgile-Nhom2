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

import com.bumptech.glide.Glide;
import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.SessionManager;
import com.example.appbanghe.network.services.ProductService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoyaltyProgramActivity extends AppCompatActivity {

    private RecyclerView rvRewards;
    private TextView tvCurrentLevel;
    private TextView tvCurrentPoints;
    private TextView tvNextLevelPoints;
    private ProgressBar progressBarLevel;
    private LinearLayout llBenefits;
    private Button btnViewHistory;
    private EditText etReferFriend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loyalty_program);

        initViews();
        setupLoyaltyLevel();
        setupRewards();
        setupBenefits();
        setupClickListeners();
    }

    private void initViews() {
        rvRewards = findViewById(R.id.rv_rewards);
        tvCurrentLevel = findViewById(R.id.tv_current_level);
        tvCurrentPoints = findViewById(R.id.tv_current_points);
        tvNextLevelPoints = findViewById(R.id.tv_next_level_points);
        progressBarLevel = findViewById(R.id.progress_bar_level);
        llBenefits = findViewById(R.id.ll_benefits);
        btnViewHistory = findViewById(R.id.btn_view_history);
        etReferFriend = findViewById(R.id.et_refer_friend);
    }

    private void setupLoyaltyLevel() {
        // Simulate user loyalty data
        int currentPoints = 1250;
        String currentLevel = "Bạc";
        int nextLevelPoints = 2000;
        String nextLevel = "Vàng";
        
        tvCurrentLevel.setText("Hạng: " + currentLevel);
        tvCurrentPoints.setText("Điểm: " + currentPoints);
        tvNextLevelPoints.setText("Cần " + (nextLevelPoints - currentPoints) + " điểm để lên " + nextLevel);
        
        int progress = (int) ((double) currentPoints / nextLevelPoints * 100);
        progressBarLevel.setProgress(progress);
    }

    private void setupRewards() {
        // Setup rewards RecyclerView
        LoyaltyRewardAdapter adapter = new LoyaltyRewardAdapter(this, new LoyaltyRewardAdapter.Listener() {
            @Override
            public void onRedeemReward(JsonObject reward) {
                redeemReward(reward);
            }
        });
        
        rvRewards.setLayoutManager(new LinearLayoutManager(this));
        rvRewards.setAdapter(adapter);
        
        // Add sample rewards
        com.google.gson.JsonArray rewards = new com.google.gson.JsonArray();
        rewards.add(createSampleReward("Voucher 50K", 500, "voucher_50k"));
        rewards.add(createSampleReward("Voucher 100K", 900, "voucher_100k"));
        rewards.add(createSampleReward("Giao hàng miễn phí 1 tháng", 1200, "freeship_month"));
        rewards.add(createSampleReward("Quà tặng độc quyền", 2000, "exclusive_gift"));
        
        adapter.updateData(rewards);
    }

    private JsonObject createSampleReward(String title, int pointsRequired, String type) {
        JsonObject reward = new JsonObject();
        reward.addProperty("title", title);
        reward.addProperty("pointsRequired", pointsRequired);
        reward.addProperty("type", type);
        reward.addProperty("available", true);
        return reward;
    }

    private void setupBenefits() {
        String[] benefits = {
            "Tích điểm trên mọi đơn hàng",
            "Đổi điểm lấy voucher hấp dẫn",
            "Ưu tiên xem sản phẩm mới",
            "Giảm giá đặc biệt vào sinh nhật",
            "Hỗ trợ khách hàng VIP"
        };

        for (String benefit : benefits) {
            View benefitView = getLayoutInflater().inflate(R.layout.item_loyalty_benefit, llBenefits, false);
            TextView tvBenefitText = benefitView.findViewById(R.id.tv_benefit_text);
            ImageView ivBenefitIcon = benefitView.findViewById(R.id.iv_benefit_icon);
            
            tvBenefitText.setText(benefit);
            ivBenefitIcon.setImageResource(android.R.drawable.star_big_on);
            
            llBenefits.addView(benefitView);
        }
    }

    private void setupClickListeners() {
        btnViewHistory.setOnClickListener(v -> {
            showPointsHistory();
        });

        findViewById(R.id.btn_refer_friend).setOnClickListener(v -> {
            referFriend();
        });

        findViewById(R.id.btn_daily_checkin).setOnClickListener(v -> {
            dailyCheckIn();
        });
    }

    private void redeemReward(JsonObject reward) {
        String title = reward.get("title").getAsString();
        int pointsRequired = reward.get("pointsRequired").getAsInt();
        
        // Simulate redemption
        Toast.makeText(this, "Đã đổi " + title + " thành công!", Toast.LENGTH_SHORT).show();
        
        // Update points and level
        setupLoyaltyLevel();
    }

    private void showPointsHistory() {
        // Show points history dialog
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Lịch sử tích điểm")
                .setMessage("15/03/2024: +50 điểm (Đánh giá sản phẩm)\n" +
                          "14/03/2024: +120 điểm (Mua hàng 1.2M VNĐ)\n" +
                          "13/03/2024: +10 điểm (Check-in hàng ngày)\n" +
                          "12/03/2024: -500 điểm (Đổi voucher 50K)")
                .setPositiveButton("OK", null)
                .show();
    }

    private void referFriend() {
        String friendEmail = etReferFriend.getText().toString().trim();
        if (friendEmail.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập email bạn bè", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Simulate referral
        Toast.makeText(this, "Đã gửi lời mời đến " + friendEmail, Toast.LENGTH_SHORT).show();
        etReferFriend.setText("");
    }

    private void dailyCheckIn() {
        // Simulate daily check-in
        Toast.makeText(this, "Check-in thành công! +10 điểm", Toast.LENGTH_SHORT).show();
        setupLoyaltyLevel();
    }
}
