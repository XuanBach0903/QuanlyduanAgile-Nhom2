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
import com.example.appbanghe.network.services.ReviewService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminReviewManagementActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewManagementAdapter adapter;
    private TextView tvEmptyState;
    private LinearLayout llContent;
    private Button btnRefresh;
    private Button btnFilter;
    private TextView tvPendingCount;
    private ImageView btnBack;

    private List<JsonObject> reviews = new ArrayList<>();
    private String currentFilter = "CHO_DUYET"; // CHO_DUYET, DA_DUYET, TU_CHOI, TAT_CA
    private int currentPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_review_management);

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadReviews();
        loadStatistics();
    }

    private void initViews() {
        rvReviews = findViewById(R.id.rv_reviews);
        tvEmptyState = findViewById(R.id.tv_empty_state);
        llContent = findViewById(R.id.ll_content);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnFilter = findViewById(R.id.btn_filter);
        tvPendingCount = findViewById(R.id.tv_pending_count);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ReviewManagementAdapter(this, reviews, new ReviewManagementAdapter.Listener() {
            @Override
            public void onApproveReview(String reviewId) {
                approveReview(reviewId);
            }

            @Override
            public void onRejectReview(String reviewId) {
                showRejectDialog(reviewId);
            }

            @Override
            public void onReplyReview(String reviewId) {
                showReplyDialog(reviewId);
            }

            @Override
            public void onViewReview(String reviewId) {
                viewReviewDetail(reviewId);
            }

            @Override
            public void onDeleteReview(String reviewId) {
                showDeleteConfirmDialog(reviewId);
            }
        });

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);

        // Load more on scroll
        rvReviews.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        loadMoreReviews();
                    }
                }
            }
        });
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            refreshReviews();
        });

        btnFilter.setOnClickListener(v -> {
            showFilterDialog();
        });
    }

    private void loadReviews() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage = 1;

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        
        Call<JsonObject> call;
        if ("CHO_DUYET".equals(currentFilter)) {
            call = service.layDanhGiaCanDuyet(currentPage, 20);
        } else {
            // For other filters, we'd need to implement additional API endpoints
            // For now, load pending reviews as default
            call = service.layDanhGiaCanDuyet(currentPage, 20);
        }

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;

                if (!response.isSuccessful()) {
                    showError("Không thể tải danh sách đánh giá");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        
                        if (data.has("danh_gia")) {
                            JsonArray reviewArray = data.getAsJsonArray("danh_gia");
                            reviews.clear();
                            
                            for (int i = 0; i < reviewArray.size(); i++) {
                                reviews.add(reviewArray.get(i).getAsJsonObject());
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

    private void loadMoreReviews() {
        if (isLoading) return;
        
        isLoading = true;
        currentPage++;

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.layDanhGiaCanDuyet(currentPage, 20).enqueue(new Callback<JsonObject>() {
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
                        
                        if (data.has("danh_gia")) {
                            JsonArray reviewArray = data.getAsJsonArray("danh_gia");
                            int oldSize = reviews.size();
                            
                            for (int i = 0; i < reviewArray.size(); i++) {
                                reviews.add(reviewArray.get(i).getAsJsonObject());
                            }
                            
                            adapter.notifyItemRangeInserted(oldSize, reviewArray.size());
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
        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.layThongKeDanhGia(null).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        
                        if (data.has("danh_gia_hom_nay")) {
                            int todayReviews = data.get("danh_gia_hom_nay").getAsInt();
                            tvPendingCount.setText(todayReviews + " đánh giá mới hôm nay");
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

    private void updateUI() {
        if (reviews.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            llContent.setVisibility(View.GONE);
            tvEmptyState.setText("Không có đánh giá nào cần duyệt");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            llContent.setVisibility(View.VISIBLE);
        }
    }

    private void showFilterDialog() {
        String[] options = {"Chờ duyệt", "Đã duyệt", "Từ chối", "Tất cả"};
        int selected = getFilterIndex(currentFilter);

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lọc đánh giá")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                currentFilter = getFilterType(which);
                dialog.dismiss();
                refreshReviews();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private int getFilterIndex(String filter) {
        switch (filter) {
            case "CHO_DUYET": return 0;
            case "DA_DUYET": return 1;
            case "TU_CHOI": return 2;
            case "TAT_CA": return 3;
            default: return 0;
        }
    }

    private String getFilterType(int index) {
        switch (index) {
            case 0: return "CHO_DUYET";
            case 1: return "DA_DUYET";
            case 2: return "TU_CHOI";
            case 3: return "TAT_CA";
            default: return "CHO_DUYET";
        }
    }

    private void refreshReviews() {
        reviews.clear();
        adapter.notifyDataSetChanged();
        loadReviews();
        loadStatistics();
    }

    private void approveReview(String reviewId) {
        JsonObject request = new JsonObject();
        request.addProperty("trang_thai", "DA_DUYET");

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.duyetDanhGia(reviewId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminReviewManagementActivity.this, "Duyệt đánh giá thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminReviewManagementActivity.this, "Đã duyệt đánh giá", Toast.LENGTH_SHORT).show();
                removeReviewFromList(reviewId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminReviewManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectDialog(String reviewId) {
        EditText etReason = new EditText(this);
        etReason.setHint("Nhập lý do từ chối");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Từ chối đánh giá")
            .setView(etReason)
            .setPositiveButton("Từ chối", (dialog, which) -> {
                String reason = etReason.getText().toString().trim();
                rejectReview(reviewId, reason);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void rejectReview(String reviewId, String reason) {
        JsonObject request = new JsonObject();
        request.addProperty("trang_thai", "TU_CHOI");
        request.addProperty("ly_do_tu_choi", reason);

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.duyetDanhGia(reviewId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminReviewManagementActivity.this, "Từ chối đánh giá thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminReviewManagementActivity.this, "Đã từ chối đánh giá", Toast.LENGTH_SHORT).show();
                removeReviewFromList(reviewId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminReviewManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showReplyDialog(String reviewId) {
        EditText etReply = new EditText(this);
        etReply.setHint("Nhập phản hồi của shop");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Phản hồi đánh giá")
            .setView(etReply)
            .setPositiveButton("Gửi", (dialog, which) -> {
                String reply = etReply.getText().toString().trim();
                if (!reply.isEmpty()) {
                    replyReview(reviewId, reply);
                }
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void replyReview(String reviewId, String reply) {
        JsonObject request = new JsonObject();
        request.addProperty("phan_hoi", reply);

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.phanHoiDanhGia(reviewId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminReviewManagementActivity.this, "Phản hồi thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminReviewManagementActivity.this, "Đã phản hồi đánh giá", Toast.LENGTH_SHORT).show();
                updateReviewInList(reviewId, reply);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminReviewManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void viewReviewDetail(String reviewId) {
        Intent intent = new Intent(this, ReviewDetailActivity.class);
        intent.putExtra("review_id", reviewId);
        intent.putExtra("admin_mode", true);
        startActivity(intent);
    }

    private void showDeleteConfirmDialog(String reviewId) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Xóa đánh giá")
            .setMessage("Bạn có chắc chắn muốn xóa đánh giá này?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                deleteReview(reviewId);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void deleteReview(String reviewId) {
        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.xoaDanhGia(reviewId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminReviewManagementActivity.this, "Xóa đánh giá thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(AdminReviewManagementActivity.this, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show();
                removeReviewFromList(reviewId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminReviewManagementActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void removeReviewFromList(String reviewId) {
        for (int i = 0; i < reviews.size(); i++) {
            JsonObject review = reviews.get(i);
            if (review.get("id").getAsString().equals(reviewId)) {
                reviews.remove(i);
                adapter.notifyItemRemoved(i);
                updateUI();
                break;
            }
        }
    }

    private void updateReviewInList(String reviewId, String reply) {
        for (int i = 0; i < reviews.size(); i++) {
            JsonObject review = reviews.get(i);
            if (review.get("id").getAsString().equals(reviewId)) {
                review.addProperty("phan_hoi_cua_shop", reply);
                adapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        tvEmptyState.setText(message + "\n\nNhấn làm mới để thử lại.");
        tvEmptyState.setVisibility(View.VISIBLE);
        llContent.setVisibility(View.GONE);
    }
}
