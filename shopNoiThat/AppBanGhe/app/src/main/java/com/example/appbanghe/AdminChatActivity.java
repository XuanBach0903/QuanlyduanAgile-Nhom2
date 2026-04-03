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

public class AdminChatActivity extends AppCompatActivity {

    private RecyclerView rvChatRooms;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnAttach;
    private TextView tvCustomerName;
    private TextView tvUnreadCount;
    private LinearLayout llChatList;
    private LinearLayout llChatDetail;
    private ImageView btnBack;

    private ChatRoomAdapter chatRoomAdapter;
    private ChatMessageAdapter messageAdapter;
    private List<JsonObject> chatRooms = new ArrayList<>();
    private List<JsonObject> messages = new ArrayList<>();
    private String currentUserId;
    private String selectedCustomerId;
    private String selectedCustomerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_chat);

        initViews();
        setupRecyclerViews();
        loadChatRooms();
        setupClickListeners();
        
        // Register as admin
        registerAsAdmin();
    }

    private void initViews() {
        rvChatRooms = findViewById(R.id.rv_chat_rooms);
        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnAttach = findViewById(R.id.btn_attach);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvUnreadCount = findViewById(R.id.tv_unread_count);
        llChatList = findViewById(R.id.ll_chat_list);
        llChatDetail = findViewById(R.id.ll_chat_detail);
        btnBack = findViewById(R.id.btn_back);

        currentUserId = getCurrentAdminId();
        
        btnBack.setOnClickListener(v -> {
            if (llChatDetail.getVisibility() == View.VISIBLE) {
                // Back to chat list
                showChatList();
            } else {
                finish();
            }
        });
    }

    private void setupRecyclerViews() {
        // Chat rooms list
        chatRoomAdapter = new ChatRoomAdapter(this, chatRooms, new ChatRoomAdapter.Listener() {
            @Override
            public void onChatRoomSelected(String customerId, String customerName) {
                openChatRoom(customerId, customerName);
            }
        });
        rvChatRooms.setLayoutManager(new LinearLayoutManager(this));
        rvChatRooms.setAdapter(chatRoomAdapter);

        // Messages list
        messageAdapter = new ChatMessageAdapter(this, messages, currentUserId, true);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messageAdapter);

        // Auto scroll to bottom
        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvMessages.post(() -> rvMessages.scrollToPosition(messages.size() - 1));
            }
        });
    }

    private void loadChatRooms() {
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.layDanhSachPhongChat().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonArray roomArray = responseBody.getAsJsonArray("data");
                        chatRooms.clear();
                        
                        for (int i = 0; i < roomArray.size(); i++) {
                            chatRooms.add(roomArray.get(i).getAsJsonObject());
                        }
                        
                        chatRoomAdapter.notifyDataSetChanged();
                        updateUnreadCount();
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
            if (!message.isEmpty() && selectedCustomerId != null) {
                sendMessage(message, "TEXT");
                etMessage.setText("");
            }
        });

        btnAttach.setOnClickListener(v -> {
            showAttachmentOptions();
        });

        // Typing indicator
        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && selectedCustomerId != null) {
                // Admin started typing
                sendTypingIndicator(true);
            }
        });
    }

    private void openChatRoom(String customerId, String customerName) {
        selectedCustomerId = customerId;
        selectedCustomerName = customerName;
        
        tvCustomerName.setText(customerName);
        showChatDetail();
        loadChatHistory(customerId);
        markMessagesAsRead(customerId);
    }

    private void loadChatHistory(String customerId) {
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.layLichSuChatAdmin(customerId, 50).enqueue(new Callback<JsonObject>() {
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
                        
                        messageAdapter.notifyDataSetChanged();
                        rvMessages.scrollToPosition(messages.size() - 1);
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

    private void sendMessage(String message, String messageType) {
        ChatMessageRequest request = new ChatMessageRequest(
            selectedCustomerId, 
            currentUserId, 
            message, 
            messageType, 
            "ADMIN"
        );
        
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.guiTinNhanAdmin(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminChatActivity.this, "Gửi tin nhắn thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject newMessage = responseBody.getAsJsonObject("data");
                        messages.add(newMessage);
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendTypingIndicator(boolean isTyping) {
        // In real implementation, this would use WebSocket
        // For now, we'll simulate it
        if (selectedCustomerId != null) {
            // Send typing indicator to customer
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
        Toast.makeText(this, "Chức năng camera sẽ sớm có!", Toast.LENGTH_SHORT).show();
    }

    private void openGallery() {
        Toast.makeText(this, "Chức năng thư viện sẽ sớm có!", Toast.LENGTH_SHORT).show();
    }

    private void showProductSelector() {
        Intent intent = new Intent(this, com.example.appbanghe.MainActivity.class);
        intent.putExtra("select_for_chat", true);
        intent.putExtra("admin_mode", true);
        startActivityForResult(intent, 1001);
    }

    private void markMessagesAsRead(String customerId) {
        JsonObject request = new JsonObject();
        request.addProperty("nguoi_dung_id", customerId);
        
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.danhDauDaDoc(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                // Update unread count
                updateUnreadCount();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                // Handle error
            }
        });
    }

    private void updateUnreadCount() {
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.demTinNhanChuaDoc().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject data = responseBody.getAsJsonObject("data");
                        int unreadCount = data.get("so_tin_nhan_chua_doc").getAsInt();
                        
                        if (unreadCount > 0) {
                            tvUnreadCount.setText(String.valueOf(unreadCount));
                            tvUnreadCount.setVisibility(View.VISIBLE);
                        } else {
                            tvUnreadCount.setVisibility(View.GONE);
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

    private void showChatList() {
        llChatList.setVisibility(View.VISIBLE);
        llChatDetail.setVisibility(View.GONE);
        selectedCustomerId = null;
        selectedCustomerName = null;
    }

    private void showChatDetail() {
        llChatList.setVisibility(View.GONE);
        llChatDetail.setVisibility(View.VISIBLE);
    }

    private void registerAsAdmin() {
        // Register as admin for chat
        // In real implementation, this would use Socket.IO
        Toast.makeText(this, "Đã đăng ký sebagai admin hỗ trợ", Toast.LENGTH_SHORT).show();
    }

    private String getCurrentAdminId() {
        // Get current admin ID from session
        // For now, return a dummy ID
        return "admin_" + System.currentTimeMillis();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Product selected for chat
            String productId = data.getStringExtra("product_id");
            String productName = data.getStringExtra("product_name");
            
            if (productId != null && selectedCustomerId != null) {
                sendProductInfo(productId, productName);
            }
        }
    }

    private void sendProductInfo(String productId, String productName) {
        ChatMessageRequest request = new ChatMessageRequest(
            selectedCustomerId, 
            currentUserId, 
            "Sản phẩm: " + productName, 
            "PRODUCT", 
            "ADMIN", 
            productId
        );
        
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.guiThongTinSanPham(request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(AdminChatActivity.this, "Gửi thông tin sản phẩm thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        JsonObject newMessage = responseBody.getAsJsonObject("data");
                        messages.add(newMessage);
                        messageAdapter.notifyItemInserted(messages.size() - 1);
                        rvMessages.scrollToPosition(messages.size() - 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AdminChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh chat rooms
        loadChatRooms();
    }
}
