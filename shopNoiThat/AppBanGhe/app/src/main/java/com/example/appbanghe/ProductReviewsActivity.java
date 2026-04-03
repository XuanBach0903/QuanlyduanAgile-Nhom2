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
import com.example.appbanghe.network.services.ReviewService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductReviewsActivity extends AppCompatActivity {

    private static final String PRODUCT_ID_EXTRA = "product_id";
    private static final String PRODUCT_NAME_EXTRA = "product_name";

    private String productId;
    private String productName;

    private TextView tvProductName;
    private TextView tvAverageRating;
    private TextView tvTotalReviews;
    private LinearLayout llRatingBars;
    private RecyclerView rvReviews;
    private Button btnWriteReview;
    private Button btnFilter;
    private ImageView btnBack;

    private ReviewAdapter reviewAdapter;
    private List<JsonObject> reviews = new ArrayList<>();
    private ReviewFilter filter = new ReviewFilter();
    private int currentPage = 1;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_reviews);

        // Get product info from intent
        Intent intent = getIntent();
        productId = intent.getStringExtra(PRODUCT_ID_EXTRA);
        productName = intent.getStringExtra(PRODUCT_NAME_EXTRA);

        if (productId == null) {
            Toast.makeText(this, "Không có thông tin sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupProductInfo();
        setupRecyclerView();
        setupClickListeners();
        loadReviews();
    }

    private void initViews() {
        tvProductName = findViewById(R.id.tv_product_name);
        tvAverageRating = findViewById(R.id.tv_average_rating);
        tvTotalReviews = findViewById(R.id.tv_total_reviews);
        llRatingBars = findViewById(R.id.ll_rating_bars);
        rvReviews = findViewById(R.id.rv_reviews);
        btnWriteReview = findViewById(R.id.btn_write_review);
        btnFilter = findViewById(R.id.btn_filter);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupProductInfo() {
        tvProductName.setText(productName != null ? productName : "Đánh giá sản phẩm");
    }

    private void setupRecyclerView() {
        reviewAdapter = new ReviewAdapter(this, reviews, new ReviewAdapter.Listener() {
            @Override
            public void onReviewClick(String reviewId) {
                // Open review detail
                openReviewDetail(reviewId);
            }

            @Override
            public void onLikeReview(String reviewId, boolean isLiked) {
                toggleLikeReview(reviewId);
            }

            @Override
            public void onReportReview(String reviewId) {
                reportReview(reviewId);
            }
        });

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(reviewAdapter);

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
        btnWriteReview.setOnClickListener(v -> {
            openCreateReview();
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
        service.layDanhGiaSanPham(productId, currentPage, 20).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoading = false;

                if (!response.isSuccessful()) {
                    showError("Không thể tải đánh giá");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        
                        // Load reviews
                        if (data.has("danh_gia")) {
                            JsonArray reviewArray = data.getAsJsonArray("danh_gia");
                            reviews.clear();
                            
                            for (int i = 0; i < reviewArray.size(); i++) {
                                reviews.add(reviewArray.get(i).getAsJsonObject());
                            }
                            
                            reviewAdapter.notifyDataSetChanged();
                        }

                        // Load statistics
                        if (data.has("thong_ke")) {
                            updateStatistics(data.getAsJsonObject("thong_ke"));
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
        service.layDanhGiaSanPham(productId, currentPage, 20).enqueue(new Callback<JsonObject>() {
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
                            
                            reviewAdapter.notifyItemRangeInserted(oldSize, reviewArray.size());
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

    private void updateStatistics(JsonObject stats) {
        if (stats.has("trung_binh_sao")) {
            double avgRating = stats.get("trung_binh_sao").getAsDouble();
            tvAverageRating.setText(String.format("%.1f", avgRating));
        }

        if (stats.has("tong_danh_gia")) {
            int totalReviews = stats.get("tong_danh_gia").getAsInt();
            tvTotalReviews.setText(totalReviews + " đánh giá");
        }

        if (stats.has("danh_gia_theo_sao")) {
            updateRatingBars(stats.getAsJsonArray("danh_gia_theo_sao"));
        }
    }

    private void updateRatingBars(JsonArray ratingStats) {
        llRatingBars.removeAllViews();

        for (int i = 5; i >= 1; i--) {
            int count = 0;
            
            // Find count for this star rating
            for (JsonElement element : ratingStats) {
                JsonObject stat = element.getAsJsonObject();
                if (stat.get("sao").getAsInt() == i) {
                    count = stat.get("so_luong").getAsInt();
                    break;
                }
            }

            // Create rating bar view
            View ratingBarView = getLayoutInflater().inflate(R.layout.item_rating_bar, llRatingBars, false);
            
            TextView tvStars = ratingBarView.findViewById(R.id.tv_stars);
            TextView tvCount = ratingBarView.findViewById(R.id.tv_count);
            android.widget.ProgressBar progressBar = ratingBarView.findViewById(R.id.progress_bar);

            tvStars.setText(i + " sao");
            tvCount.setText("(" + count + ")");
            
            int totalReviews = reviews.size();
            int progress = totalReviews > 0 ? (count * 100 / totalReviews) : 0;
            progressBar.setProgress(progress);

            llRatingBars.addView(ratingBarView);
        }
    }

    private void showFilterDialog() {
        String[] options = {"Tất cả", "5 sao", "4 sao", "3 sao", "2 sao", "1 sao", "Có hình ảnh", "Có phản hồi"};
        int selected = filter.getSelectedIndex();

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Lọc đánh giá")
            .setSingleChoiceItems(options, selected, (dialog, which) -> {
                filter.setSelectedIndex(which);
                dialog.dismiss();
                applyFilter();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void applyFilter() {
        currentPage = 1;
        reviews.clear();
        reviewAdapter.notifyDataSetChanged();
        loadReviews();
    }

    private void openCreateReview() {
        Intent intent = new Intent(this, CreateReviewActivity.class);
        intent.putExtra("product_id", productId);
        intent.putExtra("product_name", productName);
        startActivityForResult(intent, 1001);
    }

    private void openReviewDetail(String reviewId) {
        Intent intent = new Intent(this, ReviewDetailActivity.class);
        intent.putExtra("review_id", reviewId);
        startActivity(intent);
    }

    private void toggleLikeReview(String reviewId) {
        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.thichDanhGia(reviewId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        boolean isLiked = data.get("da_thich").getAsBoolean();
                        int likeCount = data.get("so_thich").getAsInt();
                        
                        // Update review in list
                        updateReviewLikeStatus(reviewId, isLiked, likeCount);
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

    private void updateReviewLikeStatus(String reviewId, boolean isLiked, int likeCount) {
        for (int i = 0; i < reviews.size(); i++) {
            JsonObject review = reviews.get(i);
            if (review.get("id").getAsString().equals(reviewId)) {
                review.addProperty("da_thich", isLiked);
                review.addProperty("so_thich", likeCount);
                reviewAdapter.notifyItemChanged(i);
                break;
            }
        }
    }

    private void reportReview(String reviewId) {
        String[] reasons = {"Spam", "Không phù hợp", "Sao chép", "Khác"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Báo cáo đánh giá")
            .setItems(reasons, (dialog, which) -> {
                String reason = reasons[which].toUpperCase();
                submitReport(reviewId, reason);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void submitReport(String reviewId, String reason) {
        JsonObject request = new JsonObject();
        request.addProperty("ly_do", reason);

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.baoCaoDanhGia(reviewId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ProductReviewsActivity.this, "Báo cáo thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(ProductReviewsActivity.this, "Đã báo cáo đánh giá", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProductReviewsActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Review created, refresh list
            loadReviews();
        }
    }

    // Filter helper class
    private static class ReviewFilter {
        private int selectedIndex = 0;

        public int getSelectedIndex() {
            return selectedIndex;
        }

        public void setSelectedIndex(int selectedIndex) {
            this.selectedIndex = selectedIndex;
        }

        public String getFilterType() {
            switch (selectedIndex) {
                case 1: return "5";
                case 2: return "4";
                case 3: return "3";
                case 4: return "2";
                case 5: return "1";
                case 6: return "co_hinh_anh";
                case 7: return "co_phan_hoi";
                default: return null;
            }
        }
    }
}
