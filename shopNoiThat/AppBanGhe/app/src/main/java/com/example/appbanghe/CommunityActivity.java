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

public class CommunityActivity extends AppCompatActivity {

    private RecyclerView rvPosts;
    private RecyclerView rvMembers;
    private EditText etPostContent;
    private Button btnPostShare;
    private TextView tvMemberCount;
    private LinearLayout llTrendingTopics;
    private ImageView imgCommunityBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        initViews();
        setupCommunityBanner();
        setupPosts();
        setupMembers();
        setupTrendingTopics();
        setupClickListeners();
    }

    private void initViews() {
        rvPosts = findViewById(R.id.rv_posts);
        rvMembers = findViewById(R.id.rv_members);
        etPostContent = findViewById(R.id.et_post_content);
        btnPostShare = findViewById(R.id.btn_post_share);
        tvMemberCount = findViewById(R.id.tv_member_count);
        llTrendingTopics = findViewById(R.id.ll_trending_topics);
        imgCommunityBanner = findViewById(R.id.img_community_banner);
    }

    private void setupCommunityBanner() {
        Glide.with(this)
                .load("https://via.placeholder.com/400x150/4CAF50/FFFFFF?text=Cộng+Đồng+Nội+Thất")
                .into(imgCommunityBanner);
        
        tvMemberCount.setText("1,234 thành viên");
    }

    private void setupPosts() {
        CommunityPostAdapter adapter = new CommunityPostAdapter(this, new CommunityPostAdapter.Listener() {
            @Override
            public void onLikePost(JsonObject post) {
                likePost(post);
            }

            @Override
            public void onCommentPost(JsonObject post) {
                commentPost(post);
            }

            @Override
            public void onSharePost(JsonObject post) {
                sharePost(post);
            }

            @Override
            public void onViewProfile(String userId) {
                viewUserProfile(userId);
            }
        });
        
        rvPosts.setLayoutManager(new LinearLayoutManager(this));
        rvPosts.setAdapter(adapter);
        
        // Add sample posts
        com.google.gson.JsonArray posts = new com.google.gson.JsonArray();
        posts.add(createSamplePost("user1", "Nguyễn Văn A", "Mới mua ghế sofa này, chất lượng quá tuyệt vời! 👍", "sofa_review", 45, 12));
        posts.add(createSamplePost("user2", "Trần Thị B", "Ai có kinh nghiệm về bàn làm việc không ạ? Em đang phân vân giữa 2 mẫu", "desk_question", 23, 8));
        posts.add(createSamplePost("user3", "Lê Văn C", "Chia sẻ cách bài trí phòng khách nhỏ gọn mà vẫn sang trọng", "room_tips", 67, 25));
        
        adapter.updateData(posts);
    }

    private JsonObject createSamplePost(String userId, String userName, String content, String type, int likes, int comments) {
        JsonObject post = new JsonObject();
        post.addProperty("userId", userId);
        post.addProperty("userName", userName);
        post.addProperty("content", content);
        post.addProperty("type", type);
        post.addProperty("likes", likes);
        post.addProperty("comments", comments);
        post.addProperty("timestamp", System.currentTimeMillis() - (long)(Math.random() * 86400000));
        post.addProperty("liked", false);
        return post;
    }

    private void setupMembers() {
        CommunityMemberAdapter adapter = new CommunityMemberAdapter(this, new CommunityMemberAdapter.Listener() {
            @Override
            public void onFollowMember(String userId) {
                followMember(userId);
            }

            @Override
            public void onViewMemberProfile(String userId) {
                viewUserProfile(userId);
            }
        });
        
        rvMembers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvMembers.setAdapter(adapter);
        
        // Add sample members
        com.google.gson.JsonArray members = new com.google.gson.JsonArray();
        members.add(createSampleMember("user1", "Nguyễn Văn A", "Chuyên gia nội thất", true));
        members.add(createSampleMember("user2", "Trần Thị B", "Thành viên tích cực", false));
        members.add(createSampleMember("user3", "Lê Văn C", "Designer", false));
        
        adapter.updateData(members);
    }

    private JsonObject createSampleMember(String userId, String name, String role, boolean isFollowed) {
        JsonObject member = new JsonObject();
        member.addProperty("userId", userId);
        member.addProperty("name", name);
        member.addProperty("role", role);
        member.addProperty("isFollowed", isFollowed);
        return member;
    }

    private void setupTrendingTopics() {
        String[] topics = {
            "#sofa-decor",
            "#phong-khach-hien-dai",
            "#thiet-ke-noi-that",
            "#ghe-lam-viec",
            "#deco-tips"
        };

        for (String topic : topics) {
            View topicView = getLayoutInflater().inflate(R.layout.item_trending_topic, llTrendingTopics, false);
            TextView tvTopicText = topicView.findViewById(R.id.tv_topic_text);
            
            tvTopicText.setText(topic);
            
            topicView.setOnClickListener(v -> {
                filterPostsByTopic(topic);
            });
            
            llTrendingTopics.addView(topicView);
        }
    }

    private void setupClickListeners() {
        btnPostShare.setOnClickListener(v -> {
            sharePost();
        });

        findViewById(R.id.btn_create_poll).setOnClickListener(v -> {
            createPoll();
        });

        findViewById(R.id.btn_view_guidelines).setOnClickListener(v -> {
            viewCommunityGuidelines();
        });
    }

    private void likePost(JsonObject post) {
        boolean isLiked = post.get("liked").getAsBoolean();
        int currentLikes = post.get("likes").getAsInt();
        
        post.addProperty("liked", !isLiked);
        post.addProperty("likes", isLiked ? currentLikes - 1 : currentLikes + 1);
        
        Toast.makeText(this, isLiked ? "Bỏ thích" : "Đã thích", Toast.LENGTH_SHORT).show();
    }

    private void commentPost(JsonObject post) {
        String userName = post.get("userName").getAsString();
        Toast.makeText(this, "Bình luận bài viết của " + userName, Toast.LENGTH_SHORT).show();
    }

    private void sharePost(JsonObject post) {
        Toast.makeText(this, "Chia sẻ bài viết", Toast.LENGTH_SHORT).show();
    }

    private void viewUserProfile(String userId) {
        Toast.makeText(this, "Xem hồ sơ: " + userId, Toast.LENGTH_SHORT).show();
    }

    private void followMember(String userId) {
        Toast.makeText(this, "Theo dõi thành viên: " + userId, Toast.LENGTH_SHORT).show();
    }

    private void filterPostsByTopic(String topic) {
        Toast.makeText(this, "Lọc bài viết theo: " + topic, Toast.LENGTH_SHORT).show();
    }

    private void sharePost() {
        String content = etPostContent.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Simulate posting
        Toast.makeText(this, "Đã đăng bài viết!", Toast.LENGTH_SHORT).show();
        etPostContent.setText("");
        
        // Refresh posts
        setupPosts();
    }

    private void createPoll() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Tạo bình chọn")
                .setMessage("Chức năng tạo bình chọn sẽ sớm có!")
                .setPositiveButton("OK", null)
                .show();
    }

    private void viewCommunityGuidelines() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Quy định cộng đồng")
                .setMessage("1. Tôn trọng các thành viên khác\n" +
                          "2. Không spam nội dung\n" +
                          "3. Chia sẻ thông tin hữu ích\n" +
                          "4. Không đăng nội dung không phù hợp")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
