package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.example.appbanghe.network.dto.UpdateCartItemRequest;
import com.example.appbanghe.network.services.CartService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CartActivity extends AppCompatActivity {

    private View emptyContainer;
    private View contentContainer;
    private RecyclerView rv;
    private CartAdapter adapter;

    private TextView tvSubtotal;
    private TextView tvTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cart);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.cart_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        emptyContainer = findViewById(R.id.empty_container);
        contentContainer = findViewById(R.id.content_container);

        Button btnContinue = findViewById(R.id.btn_continue_shopping);
        if (btnContinue != null) {
            btnContinue.setOnClickListener(v -> {
                startActivity(new Intent(CartActivity.this, MainActivity.class));
                finish();
            });
        }

        rv = findViewById(R.id.rv_cart);
        adapter = new CartAdapter(this, new CartAdapter.Listener() {
            @Override
            public void onChangeQty(String itemId, int newQty) {
                updateQty(itemId, newQty);
            }

            @Override
            public void onDelete(String itemId) {
                deleteItem(itemId);
            }
        });
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvTotal = findViewById(R.id.tv_total);

        Button btnCheckout = findViewById(R.id.btn_checkout);
        if (btnCheckout != null) {
            btnCheckout.setOnClickListener(v -> startActivity(new Intent(CartActivity.this, CheckoutActivity.class)));
        }

        loadCart();
    }

    private void loadCart() {
        CartService service = ApiClient.createService(this, CartService.class);
        service.getGioHang().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CartActivity.this, "Không tải được giỏ hàng", Toast.LENGTH_SHORT).show();
                    showEmpty();
                    return;
                }

                JsonArray items = extractCartItems(response.body());
                if (items == null || items.size() == 0) {
                    showEmpty();
                    return;
                }

                adapter.setItems(items);
                updateTotals(items);
                showContent();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private void showEmpty() {
        if (emptyContainer != null) emptyContainer.setVisibility(View.VISIBLE);
        if (contentContainer != null) contentContainer.setVisibility(View.GONE);
    }

    private void showContent() {
        if (emptyContainer != null) emptyContainer.setVisibility(View.GONE);
        if (contentContainer != null) contentContainer.setVisibility(View.VISIBLE);
    }

    private void updateQty(String itemId, int newQty) {
        CartService service = ApiClient.createService(this, CartService.class);
        service.updateMatHang(itemId, new UpdateCartItemRequest(newQty)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(CartActivity.this, "Cập nhật số lượng thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                loadCart();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi cập nhật: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteItem(String itemId) {
        CartService service = ApiClient.createService(this, CartService.class);
        service.deleteMatHang(itemId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(CartActivity.this, "Xóa thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }
                loadCart();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CartActivity.this, "Lỗi xóa: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateTotals(JsonArray items) {
        long subtotal = 0;
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                JsonElement el = items.get(i);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject it = el.getAsJsonObject();
                int qty = getInt(it, "soLuong", "qty", "quantity");
                long unitPrice = getLong(it, "gia", "donGia", "price");
                if (unitPrice <= 0 && it.has("sanPham") && it.get("sanPham").isJsonObject()) {
                    unitPrice = getLong(it.getAsJsonObject("sanPham"), "gia", "donGia", "price");
                }
                subtotal += unitPrice * Math.max(0, qty);
            }
        }
        if (tvSubtotal != null) tvSubtotal.setText(formatMoney(subtotal));
        if (tvTotal != null) tvTotal.setText(formatMoney(subtotal));
    }

    private int getInt(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return 0;
        for (String k : keys) {
            if (k == null) continue;
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try {
                    return obj.get(k).getAsInt();
                } catch (Exception e) {
                    try {
                        return Integer.parseInt(obj.get(k).getAsString());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return 0;
    }

    private long getLong(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return 0;
        for (String k : keys) {
            if (k == null) continue;
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try {
                    return obj.get(k).getAsLong();
                } catch (Exception e) {
                    try {
                        return Long.parseLong(obj.get(k).getAsString());
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return 0;
    }

    private String formatMoney(long vnd) {
        if (vnd <= 0) return "0đ";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }

    private JsonArray extractCartItems(JsonObject body) {
        if (body == null) return null;

        if (body.has("danhSach") && body.get("danhSach").isJsonArray()) {
            return body.getAsJsonArray("danhSach");
        }
        if (body.has("items") && body.get("items").isJsonArray()) {
            return body.getAsJsonArray("items");
        }
        if (body.has("data")) {
            JsonElement data = body.get("data");
            if (data != null && data.isJsonObject()) {
                JsonObject obj = data.getAsJsonObject();
                if (obj.has("danhSach") && obj.get("danhSach").isJsonArray()) {
                    return obj.getAsJsonArray("danhSach");
                }
                if (obj.has("items") && obj.get("items").isJsonArray()) {
                    return obj.getAsJsonArray("items");
                }
            }
        }
        return null;
    }
}
