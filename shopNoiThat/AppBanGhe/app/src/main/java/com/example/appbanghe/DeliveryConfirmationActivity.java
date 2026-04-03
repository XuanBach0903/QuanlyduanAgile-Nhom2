package com.example.appbanghe;

import android.content.Intent;
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
import com.example.appbanghe.network.dto.CreateDeliveryConfirmationRequest;
import com.example.appbanghe.network.dto.DeliveryConfirmationRequest;
import com.example.appbanghe.network.dto.DeliveryInfoRequest;
import com.example.appbanghe.network.dto.DeliveryRatingRequest;
import com.example.appbanghe.network.services.DeliveryConfirmationService;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeliveryConfirmationActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int MAX_IMAGES = 5;

    private String orderId;
    private String orderNumber;
    private double orderAmount;

    private TextView tvOrderNumber;
    private TextView tvOrderAmount;
    private TextView tvDeliveryAddress;
    private EditText etReceiverName;
    private EditText etReceiverPhone;
    private EditText etReceiverNote;
    private ImageButton btnAddPhoto;
    private RecyclerView rvPhotos;
    private RatingBar rbDeliveryRating;
    private EditText etDeliveryReview;
    private Button btnCreateConfirmation;
    private Button btnConfirmDelivery;
    private Button btnRejectDelivery;
    private Button btnRateDelivery;
    private ImageView btnBack;

    private DeliveryPhotoAdapter photoAdapter;
    private List<String> photoUrls = new ArrayList<>();
    private List<Uri> photoUris = new ArrayList<>();
    private String confirmationId;
    private String currentStatus = "CHO_XAC_NHAN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_confirmation);

        // Get order info from intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("order_id");
        orderNumber = intent.getStringExtra("order_number");
        orderAmount = intent.getDoubleExtra("order_amount", 0);

        if (orderId == null) {
            Toast.makeText(this, "Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupOrderInfo();
        setupRecyclerView();
        setupClickListeners();
    }

    private void initViews() {
        tvOrderNumber = findViewById(R.id.tv_order_number);
        tvOrderAmount = findViewById(R.id.tv_order_amount);
        tvDeliveryAddress = findViewById(R.id.tv_delivery_address);
        etReceiverName = findViewById(R.id.et_receiver_name);
        etReceiverPhone = findViewById(R.id.et_receiver_phone);
        etReceiverNote = findViewById(R.id.et_receiver_note);
        btnAddPhoto = findViewById(R.id.btn_add_photo);
        rvPhotos = findViewById(R.id.rv_photos);
        rbDeliveryRating = findViewById(R.id.rb_delivery_rating);
        etDeliveryReview = findViewById(R.id.et_delivery_review);
        btnCreateConfirmation = findViewById(R.id.btn_create_confirmation);
        btnConfirmDelivery = findViewById(R.id.btn_confirm_delivery);
        btnRejectDelivery = findViewById(R.id.btn_reject_delivery);
        btnRateDelivery = findViewById(R.id.btn_rate_delivery);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupOrderInfo() {
        tvOrderNumber.setText(orderNumber != null ? orderNumber : "Đơn hàng #" + orderId);
        tvOrderAmount.setText(formatCurrency(orderAmount));
        
        // Get delivery address from order (in real app, fetch from API)
        tvDeliveryAddress.setText("Địa chỉ giao hàng sẽ được hiển thị");
    }

    private void setupRecyclerView() {
        photoAdapter = new DeliveryPhotoAdapter(this, photoUris, new DeliveryPhotoAdapter.Listener() {
            @Override
            public void onPhotoRemoved(int position) {
                photoUris.remove(position);
                photoAdapter.notifyItemRemoved(position);
                updateAddPhotoButton();
            }
        });
        rvPhotos.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvPhotos.setAdapter(photoAdapter);
    }

    private void setupClickListeners() {
        btnAddPhoto.setOnClickListener(v -> showPhotoOptions());

        btnCreateConfirmation.setOnClickListener(v -> {
            createDeliveryConfirmation();
        });

        btnConfirmDelivery.setOnClickListener(v -> {
            confirmDelivery();
        });

        btnRejectDelivery.setOnClickListener(v -> {
            showRejectDialog();
        });

        btnRateDelivery.setOnClickListener(v -> {
            rateDelivery();
        });

        // Rating change listener
        rbDeliveryRating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                String description = getRatingDescription((int) rating);
                Toast.makeText(this, description, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showPhotoOptions() {
        if (photoUris.size() >= MAX_IMAGES) {
            Toast.makeText(this, "Tối đa " + MAX_IMAGES + " hình ảnh", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Chụp ảnh", "Chọn từ thư viện"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Thêm hình ảnh xác nhận")
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
                // In real app, save bitmap to file and get URI
                imageUri = getImageUri();
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
            }

            if (imageUri != null && photoUris.size() < MAX_IMAGES) {
                photoUris.add(imageUri);
                photoAdapter.notifyItemInserted(photoUris.size() - 1);
                updateAddPhotoButton();
            }
        }
    }

    private Uri getImageUri() {
        // In real app, save image to file and return URI
        return Uri.parse("file:///delivery_photo_" + System.currentTimeMillis() + ".jpg");
    }

    private void updateAddPhotoButton() {
        btnAddPhoto.setVisibility(photoUris.size() >= MAX_IMAGES ? View.GONE : View.VISIBLE);
    }

    private void createDeliveryConfirmation() {
        String receiverName = etReceiverName.getText().toString().trim();
        String receiverPhone = etReceiverPhone.getText().toString().trim();
        String receiverNote = etReceiverNote.getText().toString().trim();

        if (receiverName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên người nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (receiverPhone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại người nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert photo URIs to URLs (in real app, upload to server)
        List<String> imageUrls = new ArrayList<>();
        for (Uri uri : photoUris) {
            imageUrls.add(uri.toString());
        }

        CreateDeliveryConfirmationRequest.NguoiNhanHang nguoiNhanHang = 
            new CreateDeliveryConfirmationRequest.NguoiNhanHang(receiverName, receiverPhone, receiverNote);

        CreateDeliveryConfirmationRequest request = new CreateDeliveryConfirmationRequest(orderId, nguoiNhanHang);

        submitConfirmationRequest(request);
    }

    private void submitConfirmationRequest(CreateDeliveryConfirmationRequest request) {
        btnCreateConfirmation.setEnabled(false);
        btnCreateConfirmation.setText("Đang gửi...");

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.taoYeuCauXacNhan(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                btnCreateConfirmation.setEnabled(true);
                btnCreateConfirmation.setText("Tạo yêu cầu xác nhận");

                if (!response.isSuccessful()) {
                    String errorMsg = "Tạo yêu cầu thất bại";
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
                    
                    Toast.makeText(DeliveryConfirmationActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.get("success").getAsBoolean()) {
                        Toast.makeText(DeliveryConfirmationActivity.this, "Tạo yêu cầu thành công!", Toast.LENGTH_SHORT).show();
                        
                        // Get confirmation ID and update UI
                        JsonObject data = responseBody.getAsJsonObject("data");
                        confirmationId = data.get("id").getAsString();
                        currentStatus = "CHO_XAC_NHAN";
                        updateUIForStatus();
                    } else {
                        String message = responseBody.get("message").getAsString();
                        Toast.makeText(DeliveryConfirmationActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(DeliveryConfirmationActivity.this, "Lỗi xử lý kết quả", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                btnCreateConfirmation.setEnabled(true);
                btnCreateConfirmation.setText("Tạo yêu cầu xác nhận");
                
                Toast.makeText(DeliveryConfirmationActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelivery() {
        if (confirmationId == null) {
            Toast.makeText(this, "Chưa có yêu cầu xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        DeliveryInfoRequest request = new DeliveryInfoRequest("XE_MAY", 0.0);

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.xacNhanGiaoHang(confirmationId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(DeliveryConfirmationActivity.this, "Xác nhận thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.get("success").getAsBoolean()) {
                        Toast.makeText(DeliveryConfirmationActivity.this, "Xác nhận giao hàng thành công!", Toast.LENGTH_SHORT).show();
                        currentStatus = "DA_XAC_NHAN";
                        updateUIForStatus();
                    } else {
                        String message = responseBody.get("message").getAsString();
                        Toast.makeText(DeliveryConfirmationActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(DeliveryConfirmationActivity.this, "Lỗi xử lý kết quả", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(DeliveryConfirmationActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showRejectDialog() {
        if (confirmationId == null) {
            Toast.makeText(this, "Chưa có yêu cầu xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText etReason = new EditText(this);
        etReason.setHint("Nhập lý do từ chối");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Từ chối giao hàng")
            .setView(etReason)
            .setPositiveButton("Từ chối", (dialog, which) -> {
                String reason = etReason.getText().toString().trim();
                rejectDelivery(reason);
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void rejectDelivery(String reason) {
        DeliveryConfirmationRequest request = new DeliveryConfirmationRequest(reason);

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.tuChoiGiaoHang(confirmationId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(DeliveryConfirmationActivity.this, "Từ chối thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DeliveryConfirmationActivity.this, "Đã từ chối giao hàng", Toast.LENGTH_SHORT).show();
                currentStatus = "TU_CHOI";
                updateUIForStatus();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(DeliveryConfirmationActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void rateDelivery() {
        if (confirmationId == null) {
            Toast.makeText(this, "Chưa có yêu cầu xác nhận", Toast.LENGTH_SHORT).show();
            return;
        }

        int rating = (int) rbDeliveryRating.getRating();
        String review = etDeliveryReview.getText().toString().trim();

        if (rating == 0) {
            Toast.makeText(this, "Vui lòng chọn số sao đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }

        DeliveryRatingRequest request = new DeliveryRatingRequest(rating, review);

        DeliveryConfirmationService service = ApiClient.createService(this, DeliveryConfirmationService.class);
        service.danhGiaGiaoHang(confirmationId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(DeliveryConfirmationActivity.this, "Đánh giá thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(DeliveryConfirmationActivity.this, "Đánh giá giao hàng thành công!", Toast.LENGTH_SHORT).show();
                rbDeliveryRating.setEnabled(false);
                etDeliveryReview.setEnabled(false);
                btnRateDelivery.setEnabled(false);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(DeliveryConfirmationActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIForStatus() {
        switch (currentStatus) {
            case "CHO_XAC_NHAN":
                btnCreateConfirmation.setVisibility(View.GONE);
                btnConfirmDelivery.setVisibility(View.VISIBLE);
                btnRejectDelivery.setVisibility(View.VISIBLE);
                btnRateDelivery.setVisibility(View.GONE);
                break;
            case "DA_XAC_NHAN":
                btnCreateConfirmation.setVisibility(View.GONE);
                btnConfirmDelivery.setVisibility(View.GONE);
                btnRejectDelivery.setVisibility(View.GONE);
                btnRateDelivery.setVisibility(View.VISIBLE);
                break;
            case "TU_CHOI":
                btnCreateConfirmation.setVisibility(View.GONE);
                btnConfirmDelivery.setVisibility(View.GONE);
                btnRejectDelivery.setVisibility(View.GONE);
                btnRateDelivery.setVisibility(View.GONE);
                break;
        }
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
