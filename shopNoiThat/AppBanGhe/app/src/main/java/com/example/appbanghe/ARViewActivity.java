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

public class ARViewActivity extends AppCompatActivity {

    private ImageView imgProduct;
    private TextView tvProductName;
    private TextView tvProductPrice;
    private TextView tvDimensions;
    private Button btnStartAR;
    private Button btnTakePhoto;
    private Button btnViewInRoom;
    private LinearLayout llColorOptions;
    private LinearLayout llSimilarProducts;
    private RecyclerView rvReviews;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar_view);

        initViews();
        loadProductInfo();
        setupColorOptions();
        setupSimilarProducts();
        setupReviews();
        setupClickListeners();
    }

    private void initViews() {
        imgProduct = findViewById(R.id.img_product);
        tvProductName = findViewById(R.id.tv_product_name);
        tvProductPrice = findViewById(R.id.tv_product_price);
        tvDimensions = findViewById(R.id.tv_dimensions);
        btnStartAR = findViewById(R.id.btn_start_ar);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnViewInRoom = findViewById(R.id.btn_view_in_room);
        llColorOptions = findViewById(R.id.ll_color_options);
        llSimilarProducts = findViewById(R.id.ll_similar_products);
        rvReviews = findViewById(R.id.rv_reviews);
    }

    private void loadProductInfo() {
        // Get product from intent
        Intent intent = getIntent();
        String productId = intent.getStringExtra("product_id");
        
        // Simulate product data
        tvProductName.setText("Ghế Sofa Hiện Đại");
        tvProductPrice.setText("12,500,000 VNĐ");
        tvDimensions.setText("Kích thước: 180x80x75 cm");
        
        // Load product image
        Glide.with(this)
                .load("https://via.placeholder.com/300x200/8B4513/FFFFFF?text=Sofa")
                .into(imgProduct);
    }

    private void setupColorOptions() {
        String[] colors = {"Nâu", "Xám", "Be", "Đen"};
        int[] colorCodes = {0x8B4513, 0x808080, 0xF5F5DC, 0x000000};
        
        for (int i = 0; i < colors.length; i++) {
            View colorOption = getLayoutInflater().inflate(R.layout.item_color_option, llColorOptions, false);
            ImageView ivColor = colorOption.findViewById(R.id.iv_color);
            TextView tvColorName = colorOption.findViewById(R.id.tv_color_name);
            
            ivColor.setBackgroundColor(colorCodes[i]);
            tvColorName.setText(colors[i]);
            
            colorOption.setOnClickListener(v -> {
                selectColor(colors[i], colorCodes[i]);
            });
            
            llColorOptions.addView(colorOption);
        }
    }

    private void setupSimilarProducts() {
        String[] similarProducts = {"Ghế Sofa Cổ Điển", "Ghế Sofa Giường", "Ghế Sofa Vải"};
        
        for (String product : similarProducts) {
            View productView = getLayoutInflater().inflate(R.layout.item_similar_product, llSimilarProducts, false);
            TextView tvProductName = productView.findViewById(R.id.tv_product_name);
            ImageView ivProduct = productView.findViewById(R.id.iv_product);
            
            tvProductName.setText(product);
            
            // Load product image
            Glide.with(this)
                    .load("https://via.placeholder.com/100x100/4169E1/FFFFFF?text=Product")
                    .into(ivProduct);
            
            productView.setOnClickListener(v -> {
                viewSimilarProduct(product);
            });
            
            llSimilarProducts.addView(productView);
        }
    }

    private void setupReviews() {
        ARReviewAdapter adapter = new ARReviewAdapter(this);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        rvReviews.setAdapter(adapter);
        
        // Add sample reviews
        com.google.gson.JsonArray reviews = new com.google.gson.JsonArray();
        reviews.add(createReview("Nguyễn Văn A", "AR rất thực tế, giúp mình hình dung tốt hơn", 5));
        reviews.add(createReview("Trần Thị B", "Màu sắc trong AR giống với thực tế", 4));
        reviews.add(createReview("Lê Văn C", "Tính năng rất hay, nên có thêm sản phẩm", 5));
        
        adapter.updateData(reviews);
    }

    private JsonObject createReview(String userName, String comment, int rating) {
        JsonObject review = new JsonObject();
        review.addProperty("userName", userName);
        review.addProperty("comment", comment);
        review.addProperty("rating", rating);
        return review;
    }

    private void setupClickListeners() {
        btnStartAR.setOnClickListener(v -> {
            startARExperience();
        });

        btnTakePhoto.setOnClickListener(v -> {
            takeARPhoto();
        });

        btnViewInRoom.setOnClickListener(v -> {
            viewInMyRoom();
        });

        findViewById(R.id.btn_measure_room).setOnClickListener(v -> {
            measureRoom();
        });

        findViewById(R.id.btn_save_design).setOnClickListener(v -> {
            saveDesign();
        });
    }

    private void selectColor(String colorName, int colorCode) {
        Toast.makeText(this, "Đã chọn màu: " + colorName, Toast.LENGTH_SHORT).show();
        
        // Update product image with selected color
        // This would typically load a different image based on color
    }

    private void viewSimilarProduct(String productName) {
        Toast.makeText(this, "Xem sản phẩm tương tự: " + productName, Toast.LENGTH_SHORT).show();
        
        // Navigate to product detail
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra("product_name", productName);
        startActivity(intent);
    }

    private void startARExperience() {
        // Check if AR is supported
        if (isARSupported()) {
            Toast.makeText(this, "Bắt đầu trải nghiệm AR...", Toast.LENGTH_SHORT).show();
            
            // Launch AR camera view
            // This would integrate with ARCore/ARKit
            simulateARExperience();
        } else {
            Toast.makeText(this, "Thiết bị không hỗ trợ AR", Toast.LENGTH_SHORT).show();
        }
    }

    private void takeARPhoto() {
        Toast.makeText(this, "Chụp ảnh AR...", Toast.LENGTH_SHORT).show();
        
        // Simulate taking photo
        // In real implementation, this would capture the AR view
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, "Đã lưu ảnh vào thư viện", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    private void viewInMyRoom() {
        Toast.makeText(this, "Xem trong phòng của tôi...", Toast.LENGTH_SHORT).show();
        
        // This would open camera with AR overlay
        simulateRoomView();
    }

    private void measureRoom() {
        Toast.makeText(this, "Bắt đầu đo phòng...", Toast.LENGTH_SHORT).show();
        
        // Simulate room measurement
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Kết quả đo phòng")
                .setMessage("Chiều dài: 4.5m\nChiều rộng: 3.2m\nChiều cao: 2.8m\nDiện tích: 14.4m²")
                .setPositiveButton("OK", null)
                .show();
    }

    private void saveDesign() {
        Toast.makeText(this, "Đã lưu thiết kế", Toast.LENGTH_SHORT).show();
        
        // Save current AR configuration
    }

    private boolean isARSupported() {
        // Check if device supports AR
        // In real implementation, check ARCore availability
        return true; // Simulate AR support
    }

    private void simulateARExperience() {
        // Simulate AR loading
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, "AR đã sẵn sàng! Di chuyển thiết bị để xem sản phẩm", Toast.LENGTH_LONG).show();
        }, 2000);
    }

    private void simulateRoomView() {
        // Simulate room view loading
        new android.os.Handler().postDelayed(() -> {
            Toast.makeText(this, "Đã tải phòng của bạn. Đặt sản phẩm vào vị trí mong muốn", Toast.LENGTH_LONG).show();
        }, 1500);
    }
}
