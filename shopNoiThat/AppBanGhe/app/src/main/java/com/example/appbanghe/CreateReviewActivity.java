package com.example.appbanghe;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.CreateReviewRequest;
import com.example.appbanghe.network.services.ReviewService;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateReviewActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int MAX_IMAGES = 5;

    private String productId;
    private String orderId;
    private String productName;
    private double productPrice;

    private TextView tvProductName;
    private TextView tvProductPrice;
    private RatingBar rbRating;
    private EditText etReviewContent;
    private ImageButton btnAddImage;
    private RecyclerView rvImages;
    private Button btnSubmit;
    private Button btnCancel;
    private ImageView btnBack;

    private ReviewImageAdapter imageAdapter;
    private List<String> imageUrls = new ArrayList<>();
    private List<Uri> imageUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_review);

        // Get product info from intent
        Intent intent = getIntent();
        productId = intent.getStringExtra("product_id");
        orderId = intent.getStringExtra("order_id");
        productName = intent.getStringExtra("product_name");
        productPrice = intent.getDoubleExtra("product_price", 0);

        if (productId == null || orderId == null) {
            Toast.makeText(this, "Không có thông tin sản phẩm hoặc đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupProductInfo();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initViews() {
        tvProductName = findViewById(R.id.tv_product_name);
        tvProductPrice = findViewById(R.id.tv_product_price);
        rbRating = findViewById(R.id.rb_rating);
        etReviewContent = findViewById(R.id.et_review_content);
        btnAddImage = findViewById(R.id.btn_add_image);
        rvImages = findViewById(R.id.rv_images);
        btnSubmit = findViewById(R.id.btn_submit);
        btnCancel = findViewById(R.id.btn_cancel);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupProductInfo() {
        tvProductName.setText(productName);
        tvProductPrice.setText(formatCurrency(productPrice));
    }

    private void setupRecyclerView() {
        imageAdapter = new ReviewImageAdapter(this, imageUris, new ReviewImageAdapter.Listener() {
            @Override
            public void onImageRemoved(int position) {
                imageUris.remove(position);
                imageAdapter.notifyItemRemoved(position);
                updateAddImageButton();
            }
        });
        rvImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvImages.setAdapter(imageAdapter);
    }

    private void setupClickListeners() {
        btnAddImage.setOnClickListener(v -> showImageOptions());

        btnCancel.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> submitReview());

        // Rating change listener
        rbRating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                // Show rating description
                String description = getRatingDescription((int) rating);
                Toast.makeText(this, description, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showImageOptions() {
        if (imageUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Tối đa " + MAX_IMAGES + " hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Thêm hình ảnh")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    openCamera();
                } else {
                    openGallery();
                }
            })
            .show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void openGallery() {
        Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri imageUri = null;

            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                // Save bitmap to temp file and get URI
                imageUri = getImageUri(imageBitmap);
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
            }

            if (imageUri != null && imageUris.size() < MAX_IMAGES) {
                imageUris.add(imageUri);
                imageAdapter.notifyItemInserted(imageUris.size() - 1);
                updateAddImageButton();
            }
        }
    }

    private Uri getImageUri(Bitmap bitmap) {
        // In a real app, save bitmap to file and return URI
        // For now, return a dummy URI
        return Uri.parse("file:///dummy_image_" + System.currentTimeMillis() + ".jpg");
    }

    private void updateAddImageButton() {
        btnAddImage.setVisibility(imageUris.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void submitReview() {
        int rating = (int) rbRating.getRating();
        String content = etReviewContent.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert image URIs to URLs (in real app, upload to server)
        List<String> imageUrls = new ArrayList<>();
        for (Uri uri : imageUris) {
            // In real implementation, upload image to server and get URL
            imageUrls.add(uri.toString());
        }

        CreateReviewRequest request = new CreateReviewRequest(
            productId,
            orderId,
            rating,
            content,
            imageUrls
        );

        submitReviewToServer(request);
    }

    private void submitReviewToServer(CreateReviewRequest request) {
        // Show loading
        btnSubmit.setEnabled(false);
        btnSubmit.setText("Đang gửi...");

        ReviewService service = ApiClient.createService(this, ReviewService.class);
        service.taoDanhGia(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // Restore button
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Gửi đánh giá");

                if (!response.isSuccessful()) {
                    String errorMsg = "Gửi đánh giá thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            com.google.gson.JsonObject errorObj = new com.google.gson.Gson().fromJson(errorStr, com.google.gson.JsonObject.class);
                            if (errorObj.has("message")) {
                                errorMsg = errorObj.get("message").getAsString();
                            }
                        }
                    } catch (IOException e) {
                        // Ignore parsing errors
                    }
                    
                    Toast.makeText(CreateReviewActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.get("success").getAsBoolean()) {
                        Toast.makeText(CreateReviewActivity.this, "Gửi đánh giá thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Return result to parent activity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("review_created", true);
                        resultIntent.putExtra("product_id", productId);
                        setResult(RESULT_OK, resultIntent);
                        
                        finish();
                    } else {
                        String message = responseBody.get("message").getAsString();
                        Toast.makeText(CreateReviewActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(CreateReviewActivity.this, "Lỗi xử lý kết quả", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Restore button
                btnSubmit.setEnabled(true);
                btnSubmit.setText("Gửi đánh giá");
                
                Toast.makeText(CreateReviewActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getRatingDescription(int rating) {
        switch (rating) {
            case 1: return "Rất không hài lòng";
            case 2: return "Không hài lòng";
            case 3: return "Bình thường";
            case 4: return "Hài lòng";
            case 5: return "Rất hài lòng";
            default: return "";
        }
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }
}
