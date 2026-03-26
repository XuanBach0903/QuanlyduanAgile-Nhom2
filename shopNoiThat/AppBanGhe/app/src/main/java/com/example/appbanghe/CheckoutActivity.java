package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.CreateOrderRequest;
import com.example.appbanghe.network.dto.VisaPaymentRequest;
import com.example.appbanghe.network.services.AccountService;
import com.example.appbanghe.network.services.CartService;
import com.example.appbanghe.network.services.OrderService;
import com.example.appbanghe.network.services.PaymentService;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class CheckoutActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etAddress;

    private RadioButton rbCod;
    private RadioButton rbVisa;

    private RecyclerView rvSummary;
    private OrderSummaryAdapter summaryAdapter;

    private TextView tvOrderTotal;

    private JsonArray cartItems;
    private long total;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_checkout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.checkout_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etName = findViewById(R.id.et_checkout_name);
        etEmail = findViewById(R.id.et_checkout_email);
        etPhone = findViewById(R.id.et_checkout_phone);
        etAddress = findViewById(R.id.et_checkout_address);

        rbCod = findViewById(R.id.rb_cod);
        rbVisa = findViewById(R.id.rb_visa);

        rvSummary = findViewById(R.id.rv_order_items);
        summaryAdapter = new OrderSummaryAdapter(this);
        rvSummary.setLayoutManager(new LinearLayoutManager(this));
        rvSummary.setAdapter(summaryAdapter);

        tvOrderTotal = findViewById(R.id.tv_order_total);

        Button btnConfirm = findViewById(R.id.btn_confirm_order);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> onConfirm());
        }

        loadProfile();
        loadCart();
    }

    private void loadProfile() {
        AccountService service = ApiClient.createService(this, AccountService.class);
        service.toi().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                JsonObject data = extractUser(response.body());
                if (data == null) return;

                String name = getString(data, "hoTen", "ho_ten", "ten", "name");
                String email = getString(data, "email");
                String phone = getString(data, "soDienThoai", "so_dien_thoai", "phone");
                String address = getString(data, "diaChi", "dia_chi", "diachi", "address");

                if (etName != null) etName.setText(name);
                if (etEmail != null) etEmail.setText(email);
                if (etPhone != null) etPhone.setText(phone);
                if (etAddress != null) etAddress.setText(address);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
            }
        });
    }

    private void loadCart() {
        CartService service = ApiClient.createService(this, CartService.class);
        service.getGioHang().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(CheckoutActivity.this, "Không tải được giỏ hàng", Toast.LENGTH_SHORT).show();
                    return;
                }
                cartItems = extractCartItems(response.body());
                if (cartItems == null) cartItems = new JsonArray();
                summaryAdapter.setItems(cartItems);

                long serverTotal = getLong(response.body(), "tongTien");
                if (serverTotal <= 0 && response.body().has("data") && response.body().get("data").isJsonObject()) {
                    serverTotal = getLong(response.body().getAsJsonObject("data"), "tongTien");
                }
                total = serverTotal > 0 ? serverTotal : calculateTotal(cartItems);
                if (tvOrderTotal != null) {
                    tvOrderTotal.setText(formatMoney(total));
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onConfirm() {
        String phone = etPhone == null ? "" : String.valueOf(etPhone.getText()).trim();
        String address = etAddress == null ? "" : String.valueOf(etAddress.getText()).trim();

        if (TextUtils.isEmpty(phone)) {
            Toast.makeText(this, "Số điện thoại là bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(address)) {
            Toast.makeText(this, "Địa chỉ giao hàng là bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cartItems == null || cartItems.size() == 0) {
            Toast.makeText(this, "Giỏ hàng đang trống", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean isVisa = rbVisa != null && rbVisa.isChecked();
        String method = isVisa ? "VISA" : "COD";

        OrderService orderService = ApiClient.createService(this, OrderService.class);
        orderService.taoDonHang(new CreateOrderRequest(address, "", method)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String msg = extractErrorMessage(response);
                    Toast.makeText(CheckoutActivity.this, msg.isEmpty() ? ("Tạo đơn thất bại: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                String orderId = extractOrderId(response.body());
                if (orderId.isEmpty()) {
                    Toast.makeText(CheckoutActivity.this, "Không lấy được mã đơn hàng", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isVisa) {
                    Toast.makeText(CheckoutActivity.this, "Đặt hàng thành công", Toast.LENGTH_SHORT).show();
                    goOrdersFromCheckout();
                    return;
                }

                showVisaDialog(orderId);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Lỗi tạo đơn: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showVisaDialog(String orderId) {
        VisaCardStorage.VisaCard c1 = VisaCardStorage.getCard(this, 1);
        VisaCardStorage.VisaCard c2 = VisaCardStorage.getCard(this, 2);
        if (c1 == null && c2 == null) {
            Toast.makeText(this, "Bạn chưa lưu thẻ. Vui lòng vào Tài Khoản > Thẻ Thanh Toán để thêm thẻ", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AccountActivity.class));
            return;
        }

        if (c1 != null && c2 == null) {
            doVisaPayment(orderId, c1.soThe, c1.tenChuThe, c1.thangHetHan, c1.namHetHan, c1.cvv, null);
            return;
        }
        if (c2 != null && c1 == null) {
            doVisaPayment(orderId, c2.soThe, c2.tenChuThe, c2.thangHetHan, c2.namHetHan, c2.cvv, null);
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (c1 != null) {
            sb.append("Thẻ 1: ").append(c1.masked());
            if (c1.tenChuThe != null && !c1.tenChuThe.trim().isEmpty()) {
                sb.append(" - ").append(c1.tenChuThe.trim());
            }
        }
        final String label1 = sb.length() == 0 ? "Thẻ 1" : sb.toString();

        sb.setLength(0);
        if (c2 != null) {
            sb.append("Thẻ 2: ").append(c2.masked());
            if (c2.tenChuThe != null && !c2.tenChuThe.trim().isEmpty()) {
                sb.append(" - ").append(c2.tenChuThe.trim());
            }
        }
        final String label2 = sb.length() == 0 ? "Thẻ 2" : sb.toString();

        java.util.ArrayList<String> options = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> slots = new java.util.ArrayList<>();
        options.add(label1);
        slots.add(1);
        options.add(label2);
        slots.add(2);

        final int[] selected = new int[]{0};
        new AlertDialog.Builder(this)
                .setTitle("Chọn thẻ thanh toán")
                .setSingleChoiceItems(options.toArray(new String[0]), 0, (d, which) -> selected[0] = which)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    int slot = slots.get(selected[0]);
                    VisaCardStorage.VisaCard card = VisaCardStorage.getCard(CheckoutActivity.this, slot);
                    if (card == null) return;
                    doVisaPayment(orderId, card.soThe, card.tenChuThe, card.thangHetHan, card.namHetHan, card.cvv, null);
                })
                .show();
    }

    private void showVisaDialogManual(String orderId) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_visa_payment, null, false);

        TextView tvTotal = v.findViewById(R.id.tv_total_amount);
        EditText etCard = v.findViewById(R.id.et_card_number);
        EditText etHolder = v.findViewById(R.id.et_card_holder);
        EditText etExp = v.findViewById(R.id.et_expiry);
        EditText etCvv = v.findViewById(R.id.et_cvv);

        if (tvTotal != null) {
            tvTotal.setText("Tổng thanh toán: " + formatMoney(total));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .create();

        Button btnCancel = v.findViewById(R.id.btn_cancel);
        Button btnPay = v.findViewById(R.id.btn_pay);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(x -> dialog.dismiss());
        }

        if (btnPay != null) {
            btnPay.setText("Thanh Toán " + formatMoney(total));
            btnPay.setOnClickListener(x -> {
                String soTheRaw = etCard == null ? "" : String.valueOf(etCard.getText());
                String ten = etHolder == null ? "" : String.valueOf(etHolder.getText()).trim();
                String exp = etExp == null ? "" : String.valueOf(etExp.getText()).trim();
                String cvv = etCvv == null ? "" : String.valueOf(etCvv.getText()).trim();

                String soThe = soTheRaw.replaceAll("\\s+", "");

                if (soThe.length() < 12) {
                    Toast.makeText(CheckoutActivity.this, "Số thẻ phải ít nhất 12 ký tự", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!soThe.matches("\\d+")) {
                    Toast.makeText(CheckoutActivity.this, "Số thẻ chỉ gồm chữ số", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (ten.isEmpty()) {
                    Toast.makeText(CheckoutActivity.this, "Tên trên thẻ là bắt buộc", Toast.LENGTH_SHORT).show();
                    return;
                }
                int[] mmYY = parseExpiry(exp);
                if (mmYY == null) {
                    Toast.makeText(CheckoutActivity.this, "Ngày hết hạn không hợp lệ (MM/YY)", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (cvv.length() != 3 || !cvv.matches("\\d{3}")) {
                    Toast.makeText(CheckoutActivity.this, "CVV phải 3 số", Toast.LENGTH_SHORT).show();
                    return;
                }

                doVisaPayment(orderId, soThe, ten, mmYY[0], mmYY[1], cvv, dialog);
            });
        }

        dialog.show();
    }

    private void doVisaPayment(String orderId, String soThe, String tenChuThe, int thang, int nam, String cvv, AlertDialog dialog) {
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        service.thanhToanVisa(new VisaPaymentRequest(orderId, soThe, tenChuThe, thang, nam, cvv)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String msg = extractErrorMessage(response);
                    Toast.makeText(CheckoutActivity.this, msg.isEmpty() ? ("Thanh toán thất bại: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (dialog != null) dialog.dismiss();
                Toast.makeText(CheckoutActivity.this, "Thanh toán thành công", Toast.LENGTH_SHORT).show();
                goOrdersFromCheckout();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(CheckoutActivity.this, "Lỗi thanh toán: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goOrders() {
        startActivity(new Intent(this, OrdersActivity.class));
        finish();
    }

    private void goOrdersFromCheckout() {
        Intent i = new Intent(this, OrdersActivity.class);
        i.putExtra(OrdersActivity.EXTRA_FROM_CHECKOUT, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void goHome() {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private int[] parseExpiry(String exp) {
        if (exp == null) return null;
        String s = exp.trim();
        if (!s.matches("\\d{2}/\\d{2}")) return null;
        try {
            int mm = Integer.parseInt(s.substring(0, 2));
            int yy = Integer.parseInt(s.substring(3, 5));
            if (mm < 1 || mm > 12) return null;
            int year = 2000 + yy;
            return new int[]{mm, year};
        } catch (Exception e) {
            return null;
        }
    }

    private long calculateTotal(JsonArray items) {
        long subtotal = 0;
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                JsonElement el = items.get(i);
                if (el == null || !el.isJsonObject()) continue;
                JsonObject it = el.getAsJsonObject();
                int qty = getInt(it, "soLuong", "qty", "quantity");
                long thanhTien = getLong(it, "thanhTien");
                if (thanhTien > 0) {
                    subtotal += thanhTien;
                    continue;
                }

                long unitPrice = getLong(it, "gia", "donGia", "price");
                if (unitPrice <= 0 && it.has("sanPham") && it.get("sanPham").isJsonObject()) {
                    unitPrice = getLong(it.getAsJsonObject("sanPham"), "gia", "donGia", "price");
                }
                subtotal += unitPrice * Math.max(0, qty);
            }
        }
        return subtotal;
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

    private String extractOrderId(JsonObject body) {
        if (body == null) return "";
        String id = getString(body, "id", "donHangId", "orderId");
        if (!id.isEmpty()) return id;

        if (body.has("data")) {
            JsonElement data = body.get("data");
            if (data != null && data.isJsonObject()) {
                JsonObject obj = data.getAsJsonObject();
                String id2 = getString(obj, "id", "donHangId", "orderId");
                if (!id2.isEmpty()) return id2;
            }
        }

        return "";
    }

    private JsonObject extractUser(JsonObject body) {
        if (body == null) return null;
        if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            if (data.has("user") && data.get("user").isJsonObject()) {
                return data.getAsJsonObject("user");
            }
            if (data.has("taiKhoan") && data.get("taiKhoan").isJsonObject()) {
                return data.getAsJsonObject("taiKhoan");
            }
            if (data.has("tai_khoan") && data.get("tai_khoan").isJsonObject()) {
                return data.getAsJsonObject("tai_khoan");
            }
            return data;
        }
        return body;
    }

    private String getString(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return "";
        for (String k : keys) {
            if (k == null) continue;
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try {
                    return obj.get(k).getAsString();
                } catch (Exception ignored) {
                }
            }
        }
        return "";
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
        Long v = getLongObj(obj, keys);
        return v == null ? 0 : v;
    }

    private Long getLongObj(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return null;
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
        return null;
    }

    private String formatMoney(long vnd) {
        if (vnd <= 0) return "0đ";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }

    private String extractErrorMessage(Response<?> response) {
        if (response == null) return "";
        try {
            ResponseBody err = response.errorBody();
            if (err == null) return "";
            String raw = err.string();
            if (raw == null || raw.trim().isEmpty()) return "";

            JsonElement el = new Gson().fromJson(raw, JsonElement.class);
            if (el != null && el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String msg = getString(obj, "message");
                if (!msg.isEmpty()) return msg;
            }
            return raw;
        } catch (Exception ignored) {
            return "";
        }
    }
}
