package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.ChatMessageRequest;
import com.example.appbanghe.network.services.ChatService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CustomerChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private TextView tvAdminName;
    private TextView tvAdminStatus;
    private LinearLayout llTypingIndicator;
    private ImageView btnBack;

    private ChatMessageAdapter adapter;
    private List<JsonObject> messages = new ArrayList<>();
    private String currentUserId;
    private String currentAdminId;
    private boolean isAdminOnline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_chat);

        initViews();
        setupRecyclerView();
        loadChatHistory();
        setupClickListeners();
        
        // Register for chat
        registerForChat();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnAttach = findViewById(R.id.btn_attach);
        tvAdminName = findViewById(R.id.tv_admin_name);
        tvAdminStatus = findViewById(R.id.tv_admin_status);
        llTypingIndicator = findViewById(R.id.ll_typing_indicator);
        btnBack = findViewById(R.id.btn_back);

        currentUserId = getCurrentUserId();
        
        tvAdminName.setText("Hỗ trợ khách hàng");
        tvAdminStatus.setText(isAdminOnline ? "Online" : "Offline");
        tvAdminStatus.setTextColor(isAdminOnline ? 
            getResources().getColor(android.R.color.holo_green_dark) : 
            getResources().getColor(android.R.color.holo_red_dark));

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ChatMessageAdapter(this, messages, currentUserId, false);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Auto scroll to bottom
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvMessages.post(() -> rvMessages.scrollToPosition(messages.size() - 1));
            }
        });
    }

    private void loadChatHistory() {
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.layLichSuChat(50).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonArray messageArray = responseBody.getAsJsonArray("data");
                        messages.clear();
                        
                        for (int i = 0; i < messageArray.size(); i++) {
                            messages.add(messageArray.get(i).getAsJsonObject());
                        }
                        
                        adapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messages.size() - 1);
                        
                        // Get admin ID from first message
                        if (messages.size() > 0) {
                            JsonObject firstMessage = messages.get(0);
                            if (firstMessage.has("admin_id")) {
                                currentAdminId = firstMessage.get("admin_id").getAsString();
                            }
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

    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> {
            String message = etMessage.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message, "TEXT");
                etMessage.setText("");
            }
        });

        btnAttach.setOnClickListener(v -> {
            showAttachmentOptions();
        });

        // Typing indicator
        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // User started typing
                sendTypingIndicator(true);
            }
        });
    }

    private void sendMessage(String message, String messageType) {
        ChatMessageRequest request = new ChatMessageRequest(message, messageType, "KHACH_HANG");
        
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.guiTinNhanMoi(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(CustomerChatActivity.this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject newMessage = responseBody.getAsJsonObject("data");
                        messages.add(newMessage);
                        adapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                        
                        // Get admin ID if not set
                        if (currentAdminId == null && newMessage.has("admin_id")) {
                            currentAdminId = newMessage.get("admin_id").getAsString();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CustomerChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendTypingIndicator(boolean isTyping) {
        // In real implementation, this would use WebSocket
        // For now, we'll simulate it
        if (currentAdminId != null) {
            // Send typing indicator to admin
            // This would be implemented with Socket.IO
        }
    }

    private void showAttachmentOptions() {
        String[] options = {"Chụp ảnh", "Chọn ảnh từ thư viện", "Gửi sản phẩm"};
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Gửi đính kèm")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        // Camera
                        openCamera();
                        break;
                    case 1:
                        // Gallery
                        openGallery();
                        break;
                    case 2:
                        // Product
                        showProductSelector();
                        break;
                }
            })
            .show();
    }

    private void openCamera() {
        // Implement camera functionality
        Toast.makeText(this, "Chức năng camera sẽ sớm có!", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        // Implement gallery functionality
        Toast.makeText(this, "Chức năng thư viện sẽ sớm có!", Toast.LENGTH_SHORT).show();
    }

    private void showProductSelector() {
        // Navigate to product selector
        Intent intent = new Intent(this, com.example.appbanghe.MainActivity.class);
        intent.putExtra("select_for_chat", true);
        startActivityForResult(intent, 1001);
    }

    private void registerForChat() {
        // Register user for chat
        // In real implementation, this would use Socket.IO
        Toast.makeText(this, "Đã kết nối với hỗ trợ khách hàng", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentUserId() {
        // Get current user ID from session
        // For now, return a dummy ID
        return "user_" + System.currentTimeMillis();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Product selected for chat
            String productId = data.getStringExtra("product_id");
            String productName = data.getStringExtra("product_name");
            
            if (productId != null) {
                sendProductInfo(productId, productName);
            }
        }
    }

    private void sendProductInfo(String productId, String productName) {
        ChatMessageRequest request = new ChatMessageRequest(
            currentUserId, 
            currentAdminId, 
            "Sản phẩm: " + productName, 
            "PRODUCT", 
            "KHACH_HANG", 
            productId
        );
        
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.guiThongTinSanPham(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(CustomerChatActivity.this, "Gửi thông tin sản phẩm thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject newMessage = responseBody.getAsJsonObject("data");
                        messages.add(newMessage);
                        adapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CustomerChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Mark messages as read
        markMessagesAsRead();
    }

    private void markMessagesAsRead() {
        if (currentAdminId != null) {
            JsonObject request = new JsonObject();
            request.addProperty("nguoi_dung_id", currentUserId);
            
            ChatService service = ApiClient.createService(this, ChatService.class);
            service.danhDauDaDoc(request).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    // Handle response
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    // Handle error
                }
            });
        }
    }
}
