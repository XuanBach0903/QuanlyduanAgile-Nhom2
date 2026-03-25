package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.ReviewRequest;
import com.example.appbanghe.network.services.OrderService;
import com.example.appbanghe.network.services.ProductService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReviewOrderActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_ID = "extra_order_id";

    private String orderId;
    private ReviewOrderItemsAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_review_order);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent it = getIntent();
        orderId = it == null ? null : it.getStringExtra(EXTRA_ORDER_ID);

        ImageView btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        Button btnSubmit = findViewById(R.id.btn_submit);
        if (btnSubmit != null) {
            btnSubmit.setOnClickListener(v -> submitReviews());
        }

        RecyclerView rv = findViewById(R.id.rv_items);
        adapter = new ReviewOrderItemsAdapter(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        loadOrderDetail();
    }

    private void loadOrderDetail() {
        if (orderId == null || orderId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu mã đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        OrderService service = ApiClient.createService(this, OrderService.class);
        service.chiTietDonHang(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ReviewOrderActivity.this, "Không tải được chi tiết đơn: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject data = extractObject(response.body());
                JsonArray items = null;
                if (data != null && data.has("items") && data.get("items").isJsonArray()) {
                    items = data.getAsJsonArray("items");
                }

                adapter.setItems(items);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ReviewOrderActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitReviews() {
        JsonArray items = adapter == null ? null : adapter.getItems();
        if (items == null || items.size() == 0) {
            Toast.makeText(this, "Không có sản phẩm để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        int toSend = 0;
        for (int i = 0; i < items.size(); i++) {
            JsonObject obj = getObject(items, i);
            if (obj == null) continue;
            boolean hasReview = obj.has("danhGia") && obj.get("danhGia").isJsonObject();
            if (hasReview) continue;

            int stars = getInt(obj, "_draftStars");
            String content = getString(obj, "_draftContent");

            if (stars <= 0 && (content == null || content.trim().isEmpty())) {
                continue;
            }

            if (stars < 1 || stars > 5) {
                Toast.makeText(this, "Số sao phải từ 1 đến 5", Toast.LENGTH_SHORT).show();
                return;
            }
            if (content == null || content.trim().isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
                return;
            }

            String productId = getString(obj, "sanPhamId", "san_pham_id", "productId", "id");
            if (productId == null || productId.trim().isEmpty()) {
                Toast.makeText(this, "Thiếu id sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
            toSend++;
        }

        if (toSend == 0) {
            Toast.makeText(this, "Chưa có đánh giá nào để gửi", Toast.LENGTH_SHORT).show();
            return;
        }

        final int[] remaining = new int[]{toSend};
        final boolean[] hasError = new boolean[]{false};

        ProductService service = ApiClient.createService(this, ProductService.class);

        for (int i = 0; i < items.size(); i++) {
            JsonObject obj = getObject(items, i);
            if (obj == null) continue;
            boolean hasReview = obj.has("danhGia") && obj.get("danhGia").isJsonObject();
            if (hasReview) continue;

            int stars = getInt(obj, "_draftStars");
            String content = getString(obj, "_draftContent");
            if (stars <= 0 && (content == null || content.trim().isEmpty())) {
                continue;
            }

            final int starsFinal = stars;
            final String contentFinal = content == null ? "" : content.trim();
            final JsonObject objFinal = obj;
            final String productId = getString(obj, "sanPhamId", "san_pham_id", "productId", "id");

            service.taoDanhGia(productId, new ReviewRequest(starsFinal, contentFinal, orderId)).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    if (!response.isSuccessful()) {
                        hasError[0] = true;
                    } else {
                        JsonObject dg = new JsonObject();
                        dg.addProperty("soSao", starsFinal);
                        dg.addProperty("noiDung", contentFinal);
                        objFinal.add("danhGia", dg);
                    }

                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        if (adapter != null) adapter.notifyDataSetChanged();
                        if (hasError[0]) {
                            Toast.makeText(ReviewOrderActivity.this, "Có đánh giá gửi thất bại", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ReviewOrderActivity.this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
                        }
                        setResult(RESULT_OK);
                        finish();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    hasError[0] = true;
                    remaining[0]--;
                    if (remaining[0] <= 0) {
                        if (adapter != null) adapter.notifyDataSetChanged();
                        Toast.makeText(ReviewOrderActivity.this, "Có đánh giá gửi thất bại", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            });
        }
    }

    private JsonObject extractObject(JsonObject root) {
        if (root == null) return null;
        if (root.has("data") && root.get("data").isJsonObject()) {
            return root.getAsJsonObject("data");
        }
        return root;
    }

    private JsonObject getObject(JsonArray arr, int index) {
        if (arr == null || index < 0 || index >= arr.size()) return null;
        JsonElement el = arr.get(index);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static String getString(JsonObject obj, String... keys) {
        if (obj == null) return "";
        for (String key : keys) {
            if (!obj.has(key)) continue;
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) continue;
            try {
                String v = el.getAsString();
                if (v != null) return v;
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private static int getInt(JsonObject obj, String... keys) {
        if (obj == null) return 0;
        for (String key : keys) {
            if (!obj.has(key)) continue;
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) continue;
            try {
                return el.getAsInt();
            } catch (Exception e) {
                try {
                    return Integer.parseInt(el.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }
}
