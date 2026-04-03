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

public class GamificationActivity extends AppCompatActivity {

    private RecyclerView rvChallenges;
    private RecyclerView rvLeaderboard;
    private TextView tvUserRank;
    private TextView tvUserScore;
    private TextView tvCurrentStreak;
    private LinearLayout llAchievements;
    private Button btnStartChallenge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamification);

        initViews();
        setupUserProfile();
        setupChallenges();
        setupLeaderboard();
        setupAchievements();
        setupClickListeners();
    }

    private void initViews() {
        rvChallenges = findViewById(R.id.rv_challenges);
        rvLeaderboard = findViewById(R.id.rv_leaderboard);
        tvUserRank = findViewById(R.id.tv_user_rank);
        tvUserScore = findViewById(R.id.tv_user_score);
        tvCurrentStreak = findViewById(R.id.tv_current_streak);
        llAchievements = findViewById(R.id.ll_achievements);
        btnStartChallenge = findViewById(R.id.btn_start_challenge);
    }

    private void setupUserProfile() {
        // Simulate user game data
        tvUserRank.setText("#15");
        tvUserScore.setText("2,450 điểm");
        tvCurrentStreak.setText("7 ngày liên tiếp");
    }

    private void setupChallenges() {
        ChallengeAdapter adapter = new ChallengeAdapter(this, new ChallengeAdapter.Listener() {
            @Override
            public void onStartChallenge(JsonObject challenge) {
                startChallenge(challenge);
            }

            @Override
            public void onViewProgress(JsonObject challenge) {
                viewChallengeProgress(challenge);
            }
        });
        
        rvChallenges.setLayoutManager(new LinearLayoutManager(this));
        rvChallenges.setAdapter(adapter);
        
        // Add sample challenges
        com.google.gson.JsonArray challenges = new com.google.gson.JsonArray();
        challenges.add(createSampleChallenge("Mua 5 sản phẩm", "Mua 5 sản phẩm bất kỳ", 5, 2, "shopping", 100));
        challenges.add(createSampleChallenge("Đánh giá 3 sản phẩm", "Để lại đánh giá cho 3 sản phẩm đã mua", 3, 1, "review", 150));
        challenges.add(createSampleChallenge("Check-in 7 ngày", "Check-in hàng ngày trong 7 ngày", 7, 5, "checkin", 200));
        challenges.add(createSampleChallenge("Giới thiệu 2 bạn", "Giới thiệu 2 bạn bè mới", 2, 0, "referral", 300));
        
        adapter.updateData(challenges);
    }

    private JsonObject createSampleChallenge(String title, String description, int target, int progress, String type, int rewardPoints) {
        JsonObject challenge = new JsonObject();
        challenge.addProperty("title", title);
        challenge.addProperty("description", description);
        challenge.addProperty("target", target);
        challenge.addProperty("progress", progress);
        challenge.addProperty("type", type);
        challenge.addProperty("rewardPoints", rewardPoints);
        challenge.addProperty("started", progress > 0);
        return challenge;
    }

    private void setupLeaderboard() {
        LeaderboardAdapter adapter = new LeaderboardAdapter(this);
        rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        rvLeaderboard.setAdapter(adapter);
        
        // Add sample leaderboard data
        com.google.gson.JsonArray leaderboard = new com.google.gson.JsonArray();
        leaderboard.add(createLeaderboardEntry(1, "Nguyễn Văn A", 3200, "https://via.placeholder.com/50"));
        leaderboard.add(createLeaderboardEntry(2, "Trần Thị B", 2800, "https://via.placeholder.com/50"));
        leaderboard.add(createLeaderboardEntry(3, "Lê Văn C", 2500, "https://via.placeholder.com/50"));
        leaderboard.add(createLeaderboardEntry(15, "Bạn", 2450, "https://via.placeholder.com/50"));
        
        adapter.updateData(leaderboard);
    }

    private JsonObject createLeaderboardEntry(int rank, String name, int score, String avatar) {
        JsonObject entry = new JsonObject();
        entry.addProperty("rank", rank);
        entry.addProperty("name", name);
        entry.addProperty("score", score);
        entry.addProperty("avatar", avatar);
        return entry;
    }

    private void setupAchievements() {
        String[] achievements = {
            "🛒 Chuyên gia mua sắm - Mua 10 sản phẩm",
            "⭐ Siêu đánh giá - 20 đánh giá chất lượng",
            "🔥 Lửa chơi - Check-in 30 ngày liên tiếp",
            "👥 Influencer - Giới thiệu 10 bạn bè"
        };

        for (String achievement : achievements) {
            View achievementView = getLayoutInflater().inflate(R.layout.item_achievement, llAchievements, false);
            TextView tvAchievementText = achievementView.findViewById(R.id.tv_achievement_text);
            ImageView ivAchievementIcon = achievementView.findViewById(R.id.iv_achievement_icon);
            
            tvAchievementText.setText(achievement);
            ivAchievementIcon.setImageResource(android.R.drawable.btn_star_big_on);
            
            llAchievements.addView(achievementView);
        }
    }

    private void setupClickListeners() {
        btnStartChallenge.setOnClickListener(v -> {
            showAvailableChallenges();
        });

        findViewById(R.id.btn_view_all_achievements).setOnClickListener(v -> {
            viewAllAchievements();
        });
    }

    private void startChallenge(JsonObject challenge) {
        String title = challenge.get("title").getAsString();
        Toast.makeText(this, "Bắt đầu thử thách: " + title, Toast.LENGTH_SHORT).show();
        
        // Update challenge status
        challenge.addProperty("started", true);
    }

    private void viewChallengeProgress(JsonObject challenge) {
        String title = challenge.get("title").getAsString();
        int progress = challenge.get("progress").getAsInt();
        int target = challenge.get("target").getAsInt();
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tiến độ: " + title)
                .setMessage("Hoàn thành: " + progress + "/" + target)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAvailableChallenges() {
        // Show dialog with all available challenges
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Thử thách có sẵn")
                .setMessage("1. Mua 5 sản phẩm - 100 điểm\n" +
                          "2. Đánh giá 3 sản phẩm - 150 điểm\n" +
                          "3. Check-in 7 ngày - 200 điểm\n" +
                          "4. Giới thiệu 2 bạn - 300 điểm")
                .setPositiveButton("Bắt đầu", (dialog, which) -> {
                    Toast.makeText(this, "Chọn thử thách để bắt đầu!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void viewAllAchievements() {
        // Navigate to achievements screen
        Toast.makeText(this, "Xem tất cả thành tích", Toast.LENGTH_SHORT).show();
    }
}
