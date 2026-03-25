package com.example.appbanghe;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.AddCartItemRequest;
import com.example.appbanghe.network.dto.ReviewRequest;
import com.example.appbanghe.network.services.CartService;
import com.example.appbanghe.network.services.ProductService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "extra_product_id";

    private String productId;

    private ImageView img;
    private TextView tvName;
    private TextView tvPrice;
    private TextView tvStock;

    private TextView tvDesc;
    private TextView tvMaterial;
    private TextView tvSize;
    private TextView tvCategory;

    private TextView tvQty;
    private int qty = 1;

    private TextView tvReviewSummary;
    private TextView tvReviewEmpty;
    private RecyclerView rvReviews;
    private ReviewAdapter reviewAdapter;
    private EditText etReviewStars;
    private EditText etReviewContent;
    private Button btnSubmitReview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_product_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detail_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        if (productId == null || productId.trim().isEmpty()) {
            Toast.makeText(this, "Thiếu id sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        img = findViewById(R.id.img_cover);
        tvName = findViewById(R.id.tv_name);
        tvPrice = findViewById(R.id.tv_price);
        tvStock = findViewById(R.id.tv_stock);

        tvDesc = findViewById(R.id.tv_desc);
        tvMaterial = findViewById(R.id.tv_material);
        tvSize = findViewById(R.id.tv_size);
        tvCategory = findViewById(R.id.tv_category);

        tvQty = findViewById(R.id.tv_qty);
        ImageButton btnMinus = findViewById(R.id.btn_minus);
        ImageButton btnPlus = findViewById(R.id.btn_plus);

        if (btnMinus != null) {
            btnMinus.setOnClickListener(v -> setQty(qty - 1));
        }
        if (btnPlus != null) {
            btnPlus.setOnClickListener(v -> setQty(qty + 1));
        }

        Button btnAdd = findViewById(R.id.btn_add_to_cart);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> addToCart());
        }

        tvReviewSummary = findViewById(R.id.tv_review_summary);
        tvReviewEmpty = findViewById(R.id.tv_review_empty);
        rvReviews = findViewById(R.id.rv_reviews);
        etReviewStars = findViewById(R.id.et_review_stars);
        etReviewContent = findViewById(R.id.et_review_content);
        btnSubmitReview = findViewById(R.id.btn_submit_review);

        if (rvReviews != null) {
            reviewAdapter = new ReviewAdapter(this);
            rvReviews.setLayoutManager(new LinearLayoutManager(this));
            rvReviews.setAdapter(reviewAdapter);
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setVisibility(View.GONE);
        }

        if (etReviewStars != null) {
            etReviewStars.setVisibility(View.GONE);
        }
        if (etReviewContent != null) {
            etReviewContent.setVisibility(View.GONE);
        }

        setQty(1);
        loadDetail();
        loadReviews();
    }

    private void setQty(int value) {
        qty = Math.max(1, value);
        if (tvQty != null) {
            tvQty.setText(String.valueOf(qty));
        }
    }

    private void loadDetail() {
        ProductService service = ApiClient.createService(this, ProductService.class);
        service.getSanPhamChiTiet(productId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ProductDetailActivity.this, "Không tải được chi tiết", Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject data = extractData(response.body());
                bindData(data);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadReviews() {
        ProductService service = ApiClient.createService(this, ProductService.class);
        service.getDanhGia(productId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    setReviewSummary(null, null);
                    setReviewList(null);
                    return;
                }

                JsonObject body = response.body();
                Double avg = getDouble(body, "diemTrungBinh");
                Long count = getLongObj(body, "soLuong");
                setReviewSummary(avg, count);

                JsonArray list = null;
                if (body.has("danhSach") && body.get("danhSach").isJsonArray()) {
                    list = body.getAsJsonArray("danhSach");
                }
                setReviewList(list);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                setReviewSummary(null, null);
                setReviewList(null);
            }
        });
    }

    private void submitReview() {
        int stars;
        try {
            stars = Integer.parseInt(String.valueOf(etReviewStars.getText()).trim());
        } catch (Exception e) {
            stars = 0;
        }

        String content = etReviewContent == null ? "" : String.valueOf(etReviewContent.getText()).trim();

        if (stars < 1 || stars > 5) {
            Toast.makeText(this, "Số sao phải từ 1 đến 5", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) {
            Toast.makeText(this, "Nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnSubmitReview != null) {
            btnSubmitReview.setEnabled(false);
        }

        ProductService service = ApiClient.createService(this, ProductService.class);
        service.taoDanhGia(productId, new ReviewRequest(stars, content, "")).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (btnSubmitReview != null) {
                    btnSubmitReview.setEnabled(true);
                }

                if (!response.isSuccessful()) {
                    if (response.code() == 401 || response.code() == 403) {
                        Toast.makeText(ProductDetailActivity.this, "Bạn cần đăng nhập để đánh giá", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ProductDetailActivity.this, "Gửi đánh giá thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                    return;
                }

                if (etReviewStars != null) {
                    etReviewStars.setText(null);
                }
                if (etReviewContent != null) {
                    etReviewContent.setText(null);
                }
                Toast.makeText(ProductDetailActivity.this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
                loadReviews();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                if (btnSubmitReview != null) {
                    btnSubmitReview.setEnabled(true);
                }
                Toast.makeText(ProductDetailActivity.this, "Lỗi gửi đánh giá: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setReviewSummary(Double avg, Long count) {
        if (tvReviewSummary == null) {
            return;
        }
        if (avg == null || count == null) {
            tvReviewSummary.setText("⭐ 0.0 (0 đánh giá)");
            return;
        }
        tvReviewSummary.setText("⭐ " + String.format(java.util.Locale.US, "%.1f", avg) + " (" + count + " đánh giá)");
    }

    private void setReviewList(JsonArray list) {
        boolean empty = list == null || list.size() == 0;
        if (tvReviewEmpty != null) {
            tvReviewEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        }
        if (rvReviews != null) {
            rvReviews.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
        if (reviewAdapter != null) {
            reviewAdapter.setItems(list);
        }
    }

    private void bindData(JsonObject item) {
        if (item == null) return;

        String name = getString(item, "ten");
        long price = getLong(item, "gia");
        long stock = getLong(item, "tonKhoConLai");
        if (stock == 0) {
            stock = getLong(item, "tonKho");
        }

        if (tvName != null) tvName.setText(name);
        if (tvPrice != null) tvPrice.setText(formatMoney(price));

        if (tvStock != null) {
            if (stock > 0) {
                tvStock.setVisibility(View.VISIBLE);
                tvStock.setText("Còn " + stock);
            } else {
                tvStock.setVisibility(View.GONE);
            }
        }

        String imageUrl = getString(item, "hinhDaiDien");
        if (imageUrl.isEmpty()) {
            JsonArray images = getArray(item, "hinhAnh");
            if (images != null && images.size() > 0 && images.get(0).isJsonPrimitive()) {
                try {
                    imageUrl = images.get(0).getAsString();
                } catch (Exception ignored) {
                }
            }
        }

        if (img != null) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(img);
        }

        if (tvDesc != null) tvDesc.setText(getString(item, "moTa"));
        if (tvMaterial != null) tvMaterial.setText(getString(item, "chatLieu"));

        String sizeText = "";
        if (item.has("kichThuoc") && item.get("kichThuoc").isJsonObject()) {
            JsonObject s = item.getAsJsonObject("kichThuoc");
            long dai = getLong(s, "dai_cm");
            long rong = getLong(s, "rong_cm");
            long cao = getLong(s, "cao_cm");
            if (dai > 0 || rong > 0 || cao > 0) {
                sizeText = dai + "cm x " + rong + "cm x " + cao + "cm";
            }
        }
        if (tvSize != null) tvSize.setText(sizeText);

        String catName = "";
        if (item.has("danhMuc") && item.get("danhMuc").isJsonObject()) {
            catName = getString(item.getAsJsonObject("danhMuc"), "ten");
        }
        if (tvCategory != null) tvCategory.setText(catName);
    }

    private void addToCart() {
        CartService service = ApiClient.createService(this, CartService.class);
        service.addMatHang(new AddCartItemRequest(productId, qty)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ProductDetailActivity.this, "Thêm vào giỏ thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(ProductDetailActivity.this, "Đã thêm vào giỏ", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ProductDetailActivity.this, "Lỗi thêm giỏ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private JsonObject extractData(JsonObject body) {
        if (body == null) return null;
        if (body.has("data") && body.get("data").isJsonObject()) {
            return body.getAsJsonObject("data");
        }
        return body;
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "";
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        try {
            return el.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static long getLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return 0;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
        try {
            return el.getAsLong();
        } catch (Exception e) {
            try {
                return Long.parseLong(el.getAsString());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private static Long getLongObj(JsonObject obj, String key) {
        long v = getLong(obj, key);
        return v;
    }

    private static Double getDouble(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return null;
        try {
            return el.getAsDouble();
        } catch (Exception e) {
            try {
                return Double.parseDouble(el.getAsString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private static JsonArray getArray(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return null;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull() || !el.isJsonArray()) return null;
        return el.getAsJsonArray();
    }

    private static String formatMoney(long vnd) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }
}
