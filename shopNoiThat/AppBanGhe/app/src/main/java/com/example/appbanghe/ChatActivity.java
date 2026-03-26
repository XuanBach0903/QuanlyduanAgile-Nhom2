package com.example.appbanghe;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.ChatMessageRequest;
import com.example.appbanghe.network.dto.CreateChatRequest;
import com.example.appbanghe.network.services.ChatService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String CHAT_PREFS = "chat_prefs";
    private static final String KEY_CHAT_ID = "chat_id";

    private View emptyText;
    private RecyclerView rv;
    private ChatMessageAdapter adapter;
    private EditText input;

    private String chatId;

    private final Handler pollHandler = new Handler(Looper.getMainLooper());
    private Runnable pollRunnable;
    private boolean isLoadingMessages;

    private JsonArray currentMessages = new JsonArray();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chat);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.chat_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        emptyText = findViewById(R.id.tv_empty);
        rv = findViewById(R.id.rv_chat);
        input = findViewById(R.id.et_message);

        adapter = new ChatMessageAdapter(this);
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        rv.setLayoutManager(lm);
        rv.setAdapter(adapter);

        ImageButton btnSend = findViewById(R.id.btn_send);
        if (btnSend != null) {
            btnSend.setOnClickListener(v -> onSend());
        }

        chatId = loadChatId();
        if (chatId != null) {
            loadMessages();
        } else {
            showEmpty(true);
        }

        bindQuickTemplates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    private void onSend() {
        String text = input == null ? "" : String.valueOf(input.getText()).trim();
        if (text.isEmpty()) {
            Toast.makeText(this, "Nhập nội dung", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chatId == null) {
            createChatThenSend(text);
        } else {
            sendMessage(text);
        }
    }

    private void createChatThenSend(String text) {
        ChatService service = ApiClient.createService(this, ChatService.class);
        service.taoChat(new CreateChatRequest(text)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ChatActivity.this, "Không tạo được chat: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                String id = extractChatId(response.body());
                if (id == null) {
                    Toast.makeText(ChatActivity.this, "Không lấy được chatId", Toast.LENGTH_SHORT).show();
                    return;
                }

                chatId = id;
                saveChatId(id);

                startPolling();

                if (input != null) {
                    input.setText(null);
                }

                loadMessages();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendMessage(String text) {
        if (chatId == null) return;

        addLocalOutgoingMessage(text);

        ChatService service = ApiClient.createService(this, ChatService.class);
        service.guiTinNhan(chatId, new ChatMessageRequest(text)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(ChatActivity.this, "Gửi thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                loadMessages();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(ChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMessages() {
        if (chatId == null) return;

        if (isLoadingMessages) return;
        isLoadingMessages = true;

        ChatService service = ApiClient.createService(this, ChatService.class);
        service.danhSachTinNhan(chatId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                isLoadingMessages = false;
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(ChatActivity.this, "Không tải được tin nhắn", Toast.LENGTH_SHORT).show();
                    showEmpty(true);
                    return;
                }

                JsonArray list = extractList(response.body());
                if (list == null || list.size() == 0) {
                    currentMessages = new JsonArray();
                    adapter.setItems(currentMessages);
                    showEmpty(true);
                    return;
                }

                currentMessages = list;
                adapter.setItems(currentMessages);
                showEmpty(false);
                rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                isLoadingMessages = false;
                Toast.makeText(ChatActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty(true);
            }
        });
    }

    private void showEmpty(boolean empty) {
        if (emptyText != null) emptyText.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (rv != null) rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private String loadChatId() {
        SharedPreferences prefs = getSharedPreferences(CHAT_PREFS, MODE_PRIVATE);
        String id = prefs.getString(KEY_CHAT_ID, null);
        if (id == null) return null;
        id = id.trim();
        return id.isEmpty() ? null : id;
    }

    private void saveChatId(String id) {
        getSharedPreferences(CHAT_PREFS, MODE_PRIVATE).edit().putString(KEY_CHAT_ID, id).apply();
    }

    private String extractChatId(JsonObject body) {
        if (body == null) return null;

        String id = extractString(body, "chatId", "id", "_id");
        if (!id.isEmpty()) return id;

        if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            id = extractString(data, "chatId", "id", "_id");
            if (!id.isEmpty()) return id;
        }
        return null;
    }

    private String extractString(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return "";
        for (String k : keys) {
            if (k == null) continue;
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try {
                    String v = obj.get(k).getAsString();
                    if (v != null && !v.trim().isEmpty()) return v.trim();
                } catch (Exception ignored) {
                }
            }
        }
        return "";
    }

    private JsonArray extractList(JsonObject body) {
        if (body == null) return null;

        if (body.has("danhSach") && body.get("danhSach").isJsonArray()) {
            return body.getAsJsonArray("danhSach");
        }
        if (body.has("tinNhan") && body.get("tinNhan").isJsonArray()) {
            return body.getAsJsonArray("tinNhan");
        }
        if (body.has("items") && body.get("items").isJsonArray()) {
            return body.getAsJsonArray("items");
        }
        if (body.has("data")) {
            JsonElement data = body.get("data");
            if (data != null && data.isJsonArray()) {
                return data.getAsJsonArray();
            }
            if (data != null && data.isJsonObject()) {
                JsonObject obj = data.getAsJsonObject();
                if (obj.has("danhSach") && obj.get("danhSach").isJsonArray()) {
                    return obj.getAsJsonArray("danhSach");
                }
                if (obj.has("tinNhan") && obj.get("tinNhan").isJsonArray()) {
                    return obj.getAsJsonArray("tinNhan");
                }
                if (obj.has("items") && obj.get("items").isJsonArray()) {
                    return obj.getAsJsonArray("items");
                }
            }
        }
        return null;
    }

    private void bindQuickTemplates() {
        bindTemplate(R.id.tv_tpl_1, "Mình cần tư vấn bàn làm việc phù hợp phòng nhỏ");
        bindTemplate(R.id.tv_tpl_2, "Tư vấn ghế ngồi làm việc/ghế văn phòng êm lưng");
        bindTemplate(R.id.tv_tpl_3, "Bàn ăn 4 người loại nào bền và dễ vệ sinh?");
        bindTemplate(R.id.tv_tpl_4, "Shop tư vấn giúp mình bàn học cho bé nhé");
        bindTemplate(R.id.tv_tpl_5, "Mình cần bàn trang điểm nhỏ gọn, có gương");
        bindTemplate(R.id.tv_tpl_6, "Có mẫu kệ/tủ để đồ phù hợp phòng khách không?");
        bindTemplate(R.id.tv_tpl_7, "Mình muốn mua sofa/phòng khách, shop tư vấn mẫu phù hợp");
        bindTemplate(R.id.tv_tpl_8, "Cho mình xin kích thước và màu sắc các mẫu đang có");
        bindTemplate(R.id.tv_tpl_9, "Bàn gỗ loại nào chống nước và bền lâu vậy shop?");
        bindTemplate(R.id.tv_tpl_10, "Mình cần combo bàn + ghế giá tốt, shop tư vấn giúp");
        bindTemplate(R.id.tv_tpl_11, "Shop có giao hàng/lắp đặt không? Phí ship thế nào?");
        bindTemplate(R.id.tv_tpl_12, "Có chương trình giảm giá/ưu đãi hôm nay không ạ?");
    }

    private void bindTemplate(int viewId, String content) {
        View v = findViewById(viewId);
        if (!(v instanceof TextView)) return;
        TextView tv = (TextView) v;
        tv.setText(content);
        tv.setOnClickListener(x -> {
            if (input != null) input.setText(content);
            onSend();
        });
    }

    private void addLocalOutgoingMessage(String text) {
        if (text == null) return;
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return;

        JsonObject obj = new JsonObject();
        obj.addProperty("noiDung", trimmed);
        obj.addProperty("vaiTro", "KHACH_HANG");

        if (currentMessages == null) {
            currentMessages = new JsonArray();
        }
        currentMessages.add(obj);
        adapter.setItems(currentMessages);
        showEmpty(false);
        if (rv != null) rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));

        if (input != null) {
            input.setText(null);
        }
    }

    private void startPolling() {
        stopPolling();
        pollRunnable = () -> {
            if (!isFinishing() && chatId != null) {
                loadMessages();
                pollHandler.postDelayed(pollRunnable, 2500);
            }
        };
        pollHandler.postDelayed(pollRunnable, 2500);
    }

    private void stopPolling() {
        if (pollRunnable != null) {
            pollHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }
    }
}
