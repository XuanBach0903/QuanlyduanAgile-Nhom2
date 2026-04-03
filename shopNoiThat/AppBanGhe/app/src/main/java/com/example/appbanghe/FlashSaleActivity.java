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

import com.bumptech.glide.Glide;
import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.SessionManager;
import com.example.appbanghe.network.services.ProductService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FlashSaleActivity extends AppCompatActivity {

    private ImageView imgFlashSaleBanner;
    private LinearLayout llFlashSaleProducts;
    private TextView tvCountdownTimer;
    private Button btnViewAllDeals;
    private EditText etSearchFlashSale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_sale);

        initViews();
        setupFlashSaleBanner();
        setupCountdownTimer();
        loadFlashSaleProducts();
        setupClickListeners();
    }

    private void initViews() {
        imgFlashSaleBanner = findViewById(R.id.img_flash_sale_banner);
        llFlashSaleProducts = findViewById(R.id.ll_flash_sale_products);
        tvCountdownTimer = findViewById(R.id.tv_countdown_timer);
        btnViewAllDeals = findViewById(R.id.btn_view_all_deals);
        etSearchFlashSale = findViewById(R.id.et_search_flash_sale);
    }

    private void setupFlashSaleBanner() {
        // Load flash sale banner image
        Glide.with(this)
                .load("https://via.placeholder.com/400x200/FF4444/FFFFFF?text=FLASH+SALE")
                .into(imgFlashSaleBanner);
    }

    private void setupCountdownTimer() {
        // Simulate countdown timer
        new Thread(() -> {
            int hours = 2, minutes = 30, seconds = 0;
            while (true) {
                try {
                    Thread.sleep(1000);
                    runOnUiThread(() -> {
                        if (seconds == 0) {
                            if (minutes == 0) {
                                if (hours == 0) {
                                    tvCountdownTimer.setText("Đã kết thúc!");
                                    return;
                                }
                                hours--;
                                minutes = 59;
                            } else {
                                minutes--;
                            }
                            seconds = 59;
                        } else {
                            seconds--;
                        }
                        
                        String timeText = String.format("%02d:%02d:%02d", hours, minutes, seconds);
                        tvCountdownTimer.setText(timeText);
                    });
                } catch (InterruptedException e) {
                    break;
                }
            }
        }).start();
    }

    private void loadFlashSaleProducts() {
        ProductService productService = ApiClient.createService(this, ProductService.class);
        productService.searchSanPham("", 1, 10).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("data") && data.get("data").isJsonArray()) {
                        displayFlashSaleProducts(data.getAsJsonArray("data"));
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(FlashSaleActivity.this, "Lỗi tải sản phẩm: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayFlashSaleProducts(com.google.gson.JsonArray products) {
        llFlashSaleProducts.removeAllViews();
        
        for (int i = 0; i < Math.min(products.size(), 6); i++) {
            JsonObject product = products.get(i).getAsJsonObject();
            View productView = createFlashSaleProductView(product);
            llFlashSaleProducts.addView(productView);
        }
    }

    private View createFlashSaleProductView(JsonObject product) {
        View view = getLayoutInflater().inflate(R.layout.item_flash_sale_product, llFlashSaleProducts, false);
        
        ImageView imgProduct = view.findViewById(R.id.img_product);
        TextView tvProductName = view.findViewById(R.id.tv_product_name);
        TextView tvOriginalPrice = view.findViewById(R.id.tv_original_price);
        TextView tvSalePrice = view.findViewById(R.id.tv_sale_price);
        TextView tvDiscount = view.findViewById(R.id.tv_discount);
        TextView tvSoldCount = view.findViewById(R.id.tv_sold_count);
        Button btnAddToCart = view.findViewById(R.id.btn_add_to_cart);

        // Set product data
        String productName = getString(product, "ten");
        String imageUrl = getString(product, "hinh_dai_dien");
        double gia = getDouble(product, "gia");
        double giaSale = gia * 0.7; // 30% discount for flash sale
        int soldCount = (int) (Math.random() * 50) + 10;

        tvProductName.setText(productName);
        tvOriginalPrice.setText(formatCurrency(gia));
        tvSalePrice.setText(formatCurrency(giaSale));
        tvDiscount.setText("-30%");
        tvSoldCount.setText("Đã bán " + soldCount);

        // Load product image
        if (!imageUrl.isEmpty()) {
            Glide.with(this).load(imageUrl).into(imgProduct);
        }

        // Add to cart functionality
        btnAddToCart.setOnClickListener(v -> {
            String productId = getString(product, "id");
            if (!productId.isEmpty()) {
                // Add to cart logic here
                Toast.makeText(this, "Đã thêm " + productName + " vào giỏ hàng!", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void setupClickListeners() {
        btnViewAllDeals.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("flash_sale", true);
            startActivity(intent);
        });

        etSearchFlashSale.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchFlashSaleProducts();
                return true;
            }
            return false;
        });
    }

    private void searchFlashSaleProducts() {
        String keyword = etSearchFlashSale.getText().toString().trim();
        // Implement search logic
        Toast.makeText(this, "Tìm kiếm: " + keyword, Toast.LENGTH_SHORT).show();
    }

    // Helper methods
    private String getString(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : "";
    }

    private double getDouble(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsDouble() : 0;
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}
