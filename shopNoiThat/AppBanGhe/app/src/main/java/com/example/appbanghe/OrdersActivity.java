package com.example.appbanghe;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.dto.CancelOrderRequest;
import com.example.appbanghe.network.dto.RefundOrderRequest;
import com.example.appbanghe.network.dto.ReviewRequest;
import com.example.appbanghe.network.services.OrderService;
import com.example.appbanghe.network.services.ProductService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;

import java.util.HashSet;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class OrdersActivity extends AppCompatActivity {

    private static final int REQ_REVIEW_ORDER = 2001;

    public static final String EXTRA_FROM_CHECKOUT = "extra_from_checkout";

    private String pendingReviewOrderId;

    private boolean skipNextResumeReload;

    private String pendingRefundOrderId;

    private boolean fromCheckout;

    private static final String PREFS_LOCAL_REFUND = "local_refund";
    private static final String KEY_REFUND_IDS = "refund_ids";
    private static final String KEY_REFUND_REASON_PREFIX = "refund_reason_";

    private static final String PREFS_ADMIN_MSG = "admin_msg";
    private static final String KEY_ADMIN_MSG_PREFIX = "admin_msg_";

    private View emptyContainer;
    private View contentContainer;
    private OrdersAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_orders);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.orders_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent it = getIntent();
        fromCheckout = it != null && it.getBooleanExtra(EXTRA_FROM_CHECKOUT, false);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> handleBack());
        }

        emptyContainer = findViewById(R.id.empty_container);
        contentContainer = findViewById(R.id.content_container);

        RecyclerView rv = findViewById(R.id.rv_orders);
        adapter = new OrdersAdapter(this);
        adapter.setListener(orderId -> showOrderDetailDialog(orderId));
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        loadOrders();
    }

    @Override
    public void onBackPressed() {
        handleBack();
    }

    private void handleBack() {
        if (fromCheckout) {
            Intent i = new Intent(this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            finish();
            return;
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (skipNextResumeReload) {
            skipNextResumeReload = false;
            return;
        }
        loadOrders();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_REVIEW_ORDER && resultCode == RESULT_OK) {
            String oid = pendingReviewOrderId;
            pendingReviewOrderId = null;
            skipNextResumeReload = true;
            if (oid != null && !oid.trim().isEmpty()) {
                showOrderDetailDialog(oid);
            }
            loadOrders();
        }
    }

    private void loadOrders() {
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.danhSachDonHangCuaToi().enqueue(new Callback<JsonElement>() {
            @Override
            public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(OrdersActivity.this, "Không tải được đơn hàng", Toast.LENGTH_SHORT).show();
                    showEmpty();
                    return;
                }

                JsonArray items = extractList(response.body());
                if (items == null || items.size() == 0) {
                    showEmpty();
                    return;
                }

                applyLocalRefundOverride(items);

                maybeNotifyAdminMessages(items);

                adapter.setItems(items);
                showContent();
            }

            @Override
            public void onFailure(Call<JsonElement> call, Throwable t) {
                Toast.makeText(OrdersActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                showEmpty();
            }
        });
    }

    private boolean hasUnratedItems(JsonArray items) {
        if (items == null || items.size() == 0) return false;
        for (int i = 0; i < items.size(); i++) {
            JsonElement el = items.get(i);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (!obj.has("danhGia") || obj.get("danhGia").isJsonNull()) {
                String productId = getString(obj, "sanPhamId", "san_pham_id", "productId", "id");
                if (productId != null && !productId.trim().isEmpty()) return true;
            }
        }
        return false;
    }

    private void startRatingFlow(String orderId, JsonArray items, AlertDialog parentDialog) {
        if (orderId == null || orderId.trim().isEmpty()) return;
        if (items == null || items.size() == 0) {
            Toast.makeText(this, "Không có sản phẩm để đánh giá", Toast.LENGTH_SHORT).show();
            return;
        }
        rateNextItem(orderId, items, 0, parentDialog);
    }

    private void rateNextItem(String orderId, JsonArray items, int startIndex, AlertDialog parentDialog) {
        if (items == null || startIndex >= items.size()) {
            Toast.makeText(this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
            if (parentDialog != null) parentDialog.dismiss();
            loadOrders();
            return;
        }

        int idxToRate = -1;
        for (int i = startIndex; i < items.size(); i++) {
            JsonElement el = items.get(i);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();
            if (obj.has("danhGia") && !obj.get("danhGia").isJsonNull()) continue;
            idxToRate = i;
            break;
        }

        if (idxToRate < 0) {
            Toast.makeText(this, "Đã gửi đánh giá", Toast.LENGTH_SHORT).show();
            if (parentDialog != null) parentDialog.dismiss();
            loadOrders();
            return;
        }

        final int nextIndex = idxToRate + 1;
        final JsonObject itemFinal = items.get(idxToRate).getAsJsonObject();
        final String productId = getString(itemFinal, "sanPhamId", "san_pham_id", "productId", "id");
        final String productName = getString(itemFinal, "tenSanPham", "ten", "name");

        final EditText etStars = new EditText(this);
        etStars.setHint("Số sao (1-5)");
        etStars.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        final EditText etContent = new EditText(this);
        etContent.setHint("Nhập nội dung đánh giá...");
        etContent.setMinLines(3);
        etContent.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);
        container.addView(etStars);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (10 * getResources().getDisplayMetrics().density)
        ));
        container.addView(spacer);
        container.addView(etContent);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(container);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Đánh giá" + (productName == null || productName.trim().isEmpty() ? "" : (": " + productName)))
                .setView(scroll)
                .setNegativeButton("Bỏ qua", (d, w) -> {
                    d.dismiss();
                    rateNextItem(orderId, items, nextIndex, parentDialog);
                })
                .setPositiveButton("Gửi", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btnPositive != null) {
                btnPositive.setOnClickListener(v -> {
                    int stars;
                    try {
                        stars = Integer.parseInt(String.valueOf(etStars.getText()).trim());
                    } catch (Exception e) {
                        stars = 0;
                    }
                    String content = etContent.getText() == null ? "" : etContent.getText().toString().trim();

                    if (stars < 1 || stars > 5) {
                        Toast.makeText(this, "Số sao phải từ 1 đến 5", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (content.isEmpty()) {
                        Toast.makeText(this, "Nhập nội dung đánh giá", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (productId == null || productId.trim().isEmpty()) {
                        Toast.makeText(this, "Thiếu id sản phẩm", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    final int starsFinal = stars;
                    final String contentFinal = content;

                    btnPositive.setEnabled(false);

                    ProductService service = ApiClient.createService(this, ProductService.class);
                    service.taoDanhGia(productId, new ReviewRequest(starsFinal, contentFinal, orderId)).enqueue(new Callback<JsonObject>() {
                        @Override
                        public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                            btnPositive.setEnabled(true);
                            if (!response.isSuccessful()) {
                                Toast.makeText(OrdersActivity.this, "Gửi đánh giá thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                                return;
                            }

                            JsonObject dgObj = new JsonObject();
                            dgObj.addProperty("soSao", starsFinal);
                            dgObj.addProperty("noiDung", contentFinal);
                            itemFinal.add("danhGia", dgObj);

                            dialog.dismiss();
                            rateNextItem(orderId, items, nextIndex, parentDialog);
                        }

                        @Override
                        public void onFailure(Call<JsonObject> call, Throwable t) {
                            btnPositive.setEnabled(true);
                            Toast.makeText(OrdersActivity.this, "Lỗi gửi đánh giá: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }
        });

        dialog.show();
    }

    private void applyLocalRefundOverride(JsonArray items) {
        Set<String> ids = getLocalRefundIds();
        if (ids == null || ids.isEmpty() || items == null) return;

        for (int i = 0; i < items.size(); i++) {
            JsonElement el = items.get(i);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            String id = getString(obj, "id", "_id", "ma", "maDonHang", "code", "donHangId", "orderId");
            if (id.isEmpty()) continue;

            String serverStatus = getString(obj, "trangThaiDonHang", "trang_thai_don_hang", "trangThai", "status");
            String payStatus = getString(obj, "trangThaiThanhToan", "trang_thai_thanh_toan");

            if ("HOAN_DON".equals(serverStatus) || "DA_HOAN_TIEN".equals(payStatus)) {
                if (ids.contains(id)) {
                    clearLocalRefundRequested(id);
                }
                continue;
            }

            if (ids.contains(id)) {
                obj.addProperty("trangThaiDonHang", "CHO_XU_LY_HOAN");
                obj.addProperty("trangThai", "CHO_XU_LY_HOAN");
                obj.addProperty("status", "CHO_XU_LY_HOAN");
            }
        }
    }

    private Set<String> getLocalRefundIds() {
        SharedPreferences prefs = getSharedPreferences(PREFS_LOCAL_REFUND, MODE_PRIVATE);
        Set<String> raw = prefs.getStringSet(KEY_REFUND_IDS, null);
        return raw == null ? new HashSet<>() : new HashSet<>(raw);
    }

    private void markLocalRefundRequested(String orderId, String reason) {
        if (orderId == null || orderId.trim().isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_LOCAL_REFUND, MODE_PRIVATE);
        Set<String> ids = getLocalRefundIds();
        ids.add(orderId);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putStringSet(KEY_REFUND_IDS, ids);
        if (reason != null) {
            ed.putString(KEY_REFUND_REASON_PREFIX + orderId, reason);
        }
        ed.apply();
    }

    private void clearLocalRefundRequested(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_LOCAL_REFUND, MODE_PRIVATE);
        Set<String> ids = getLocalRefundIds();
        if (ids.remove(orderId)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putStringSet(KEY_REFUND_IDS, ids);
            ed.remove(KEY_REFUND_REASON_PREFIX + orderId);
            ed.apply();
        }
    }

    private String getLocalRefundReason(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) return "";
        SharedPreferences prefs = getSharedPreferences(PREFS_LOCAL_REFUND, MODE_PRIVATE);
        String v = prefs.getString(KEY_REFUND_REASON_PREFIX + orderId, "");
        return v == null ? "" : v;
    }

    private String getSeenAdminMessage(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) return "";
        SharedPreferences prefs = getSharedPreferences(PREFS_ADMIN_MSG, MODE_PRIVATE);
        String v = prefs.getString(KEY_ADMIN_MSG_PREFIX + orderId, "");
        return v == null ? "" : v;
    }

    private void markAdminMessageSeen(String orderId, String message) {
        if (orderId == null || orderId.trim().isEmpty()) return;
        SharedPreferences prefs = getSharedPreferences(PREFS_ADMIN_MSG, MODE_PRIVATE);
        prefs.edit().putString(KEY_ADMIN_MSG_PREFIX + orderId, message == null ? "" : message).apply();
    }

    private void maybeNotifyAdminMessages(JsonArray items) {
        if (items == null || items.size() == 0) return;
        for (int i = 0; i < items.size(); i++) {
            JsonElement el = items.get(i);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            String id = getString(obj, "id", "_id", "ma", "maDonHang", "code", "donHangId", "orderId");
            if (id.isEmpty()) continue;

            String adminMsg = getString(obj, "loiNhanAdminHoan", "loi_nhan_admin_hoan");
            if (adminMsg == null || adminMsg.trim().isEmpty()) continue;

            String seen = getSeenAdminMessage(id);
            if (seen != null && seen.equals(adminMsg)) continue;

            Toast.makeText(this, "Admin nhắn: " + adminMsg, Toast.LENGTH_LONG).show();
            markAdminMessageSeen(id, adminMsg);
        }
    }

    private void showCancelReasonDialog(String orderId, AlertDialog parentDialog) {
        final String[] reasons = new String[]{
                "Đổi ý",
                "Đặt nhầm sản phẩm",
                "Muốn thay đổi địa chỉ/SDT",
                "Tìm được giá tốt hơn",
                "Khác (tự nhập)"
        };

        final int[] selected = new int[]{0};
        final EditText etCustom = new EditText(this);
        etCustom.setHint("Nhập lý do hủy");
        etCustom.setVisibility(View.GONE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);
        container.addView(etCustom);

        new AlertDialog.Builder(this)
                .setTitle("Hủy đơn")
                .setSingleChoiceItems(reasons, 0, (d, which) -> {
                    selected[0] = which;
                    boolean isCustom = which == reasons.length - 1;
                    etCustom.setVisibility(isCustom ? View.VISIBLE : View.GONE);
                })
                .setView(container)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .setPositiveButton("Hủy đơn", (d, w) -> {
                    String reason = reasons[selected[0]];
                    if (selected[0] == reasons.length - 1) {
                        String custom = etCustom.getText() == null ? "" : etCustom.getText().toString().trim();
                        if (custom.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        reason = custom;
                    }
                    cancelOrder(orderId, reason, parentDialog);
                })
                .show();
    }

    private void cancelOrder(String orderId, String reason, AlertDialog dialog) {
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.huyDon(orderId, new CancelOrderRequest(reason)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String msg = extractErrorMessage(response);
                    Toast.makeText(OrdersActivity.this, msg.isEmpty() ? ("Hủy đơn thất bại: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(OrdersActivity.this, "Đã hủy đơn", Toast.LENGTH_SHORT).show();
                if (dialog != null) dialog.dismiss();
                loadOrders();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OrdersActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
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

    private JsonArray extractList(JsonElement body) {
        if (body == null || body.isJsonNull()) return null;

        if (body.isJsonArray()) {
            return body.getAsJsonArray();
        }

        if (!body.isJsonObject()) {
            return null;
        }

        JsonObject obj = body.getAsJsonObject();
        if (obj.has("danhSach") && obj.get("danhSach").isJsonArray()) {
            return obj.getAsJsonArray("danhSach");
        }
        if (obj.has("items") && obj.get("items").isJsonArray()) {
            return obj.getAsJsonArray("items");
        }
        if (obj.has("data")) {
            JsonElement data = obj.get("data");
            if (data != null && data.isJsonArray()) {
                return data.getAsJsonArray();
            }
            if (data != null && data.isJsonObject()) {
                JsonObject dataObj = data.getAsJsonObject();
                if (dataObj.has("danhSach") && dataObj.get("danhSach").isJsonArray()) {
                    return dataObj.getAsJsonArray("danhSach");
                }
                if (dataObj.has("items") && dataObj.get("items").isJsonArray()) {
                    return dataObj.getAsJsonArray("items");
                }
            }
        }

        return null;
    }

    private void showOrderDetailDialog(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return;
        }

        View v = LayoutInflater.from(this).inflate(R.layout.dialog_order_detail, null, false);

        ImageView btnClose = v.findViewById(R.id.btn_close);
        TextView tvId = v.findViewById(R.id.tv_detail_order_id);
        TextView tvDate = v.findViewById(R.id.tv_detail_date);
        TextView tvStatus = v.findViewById(R.id.tv_detail_order_status);
        TextView tvPayment = v.findViewById(R.id.tv_detail_payment);
        TextView tvPhone = v.findViewById(R.id.tv_detail_phone);
        TextView tvAddress = v.findViewById(R.id.tv_detail_address);
        TextView tvTotal = v.findViewById(R.id.tv_detail_total);
        View layoutReason = v.findViewById(R.id.layout_reason);
        TextView tvReasonLabel = v.findViewById(R.id.tv_detail_reason_label);
        TextView tvReasonValue = v.findViewById(R.id.tv_detail_reason_value);

        View layoutAdminMsg = v.findViewById(R.id.layout_admin_message);
        TextView tvAdminMsgLabel = v.findViewById(R.id.tv_detail_admin_message_label);
        TextView tvAdminMsgValue = v.findViewById(R.id.tv_detail_admin_message_value);

        TextView tvRefundNotice = v.findViewById(R.id.tv_refund_notice);

        RecyclerView rvDetailItems = v.findViewById(R.id.rv_detail_items);
        OrderDetailItemsAdapter detailItemsAdapter = null;
        if (rvDetailItems != null) {
            detailItemsAdapter = new OrderDetailItemsAdapter(this);
            rvDetailItems.setLayoutManager(new LinearLayoutManager(this));
            rvDetailItems.setAdapter(detailItemsAdapter);
        }

        Button btnCancel = v.findViewById(R.id.btn_cancel_order);
        Button btnRefund = v.findViewById(R.id.btn_refund_order);
        Button btnConfirm = v.findViewById(R.id.btn_confirm_received);
        Button btnRate = v.findViewById(R.id.btn_rate_order);

        final String[] orderStatusHolder = new String[]{""};
        final String[] paymentMethodHolder = new String[]{""};
        final String[] paymentStatusHolder = new String[]{""};
        final JsonArray[] orderItemsHolder = new JsonArray[]{null};
        final OrderDetailItemsAdapter[] detailItemsAdapterHolder = new OrderDetailItemsAdapter[]{detailItemsAdapter};

        if (tvId != null) tvId.setText(orderId);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(v)
                .create();

        if (btnClose != null) {
            btnClose.setOnClickListener(x -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(x -> confirmReceived(orderId, dialog));
        }

        if (btnCancel != null) {
            btnCancel.setOnClickListener(x -> showCancelReasonDialog(orderId, dialog));
        }

        if (btnRefund != null) {
            btnRefund.setOnClickListener(x -> {
                pendingRefundOrderId = orderId;
                if (dialog != null) dialog.dismiss();
                showRefundDialog(orderId, paymentMethodHolder[0], paymentStatusHolder[0]);
            });
        }

        if (btnRate != null) {
            btnRate.setOnClickListener(x -> {
                pendingReviewOrderId = orderId;
                if (dialog != null) dialog.dismiss();
                Intent intent = new Intent(OrdersActivity.this, ReviewOrderActivity.class);
                intent.putExtra(ReviewOrderActivity.EXTRA_ORDER_ID, orderId);
                startActivityForResult(intent, REQ_REVIEW_ORDER);
            });
        }

        dialog.show();

        OrderService service = ApiClient.createService(this, OrderService.class);
        service.chiTietDonHang(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    String msg = extractErrorMessage(response);
                    Toast.makeText(OrdersActivity.this, msg.isEmpty() ? ("Không tải được chi tiết: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject data = extractObject(response.body());
                String ngayTao = getString(data, "ngayTao", "ngay_tao", "createdAt");
                String trangThai = getString(data, "trangThaiDonHang", "trang_thai_don_hang", "trangThai");
                String payStatusDetail = getString(data, "trangThaiThanhToan", "trang_thai_thanh_toan");
                JsonArray itemsArr = null;
                if (data.has("items") && data.get("items").isJsonArray()) {
                    itemsArr = data.getAsJsonArray("items");
                }
                orderItemsHolder[0] = itemsArr;

                if (detailItemsAdapterHolder[0] != null) {
                    detailItemsAdapterHolder[0].setItems(itemsArr);
                }
                String diaChi = getString(data, "diaChiGiaoHang", "dia_chi_giao_hang", "diaChi");
                String sdt = getString(data, "soDienThoai", "so_dien_thoai", "sdt", "phone");
                if ((sdt == null || sdt.trim().isEmpty()) && data.has("khachHang") && data.get("khachHang").isJsonObject()) {
                    sdt = getString(data.getAsJsonObject("khachHang"), "soDienThoai", "so_dien_thoai", "sdt", "phone");
                }
                if ((sdt == null || sdt.trim().isEmpty()) && data.has("nguoiDung") && data.get("nguoiDung").isJsonObject()) {
                    sdt = getString(data.getAsJsonObject("nguoiDung"), "soDienThoai", "so_dien_thoai", "sdt", "phone");
                }

                String lyDoHuy = getString(data, "lyDoHuy", "ly_do_huy");
                String lyDoHoan = getString(data, "lyDoHoan", "ly_do_hoan");
                String loiNhanAdminHoan = getString(data, "loiNhanAdminHoan", "loi_nhan_admin_hoan");

                Set<String> localRefundIds = getLocalRefundIds();
                if (localRefundIds.contains(orderId)) {
                    trangThai = "CHO_XU_LY_HOAN";
                    if (lyDoHoan == null || lyDoHoan.trim().isEmpty()) {
                        lyDoHoan = getLocalRefundReason(orderId);
                    }
                }

                if (tvDate != null) tvDate.setText(ngayTao);
                if (tvAddress != null) tvAddress.setText(diaChi);
                if (tvPhone != null) tvPhone.setText(sdt);
                if (tvTotal != null) tvTotal.setText(formatMoney(getLong(data, "tongTien", "tong_tien")));

                if (layoutReason != null && tvReasonLabel != null && tvReasonValue != null) {
                    String st = trangThai == null ? "" : trangThai.trim();
                    if ("HUY".equals(st) && lyDoHuy != null && !lyDoHuy.trim().isEmpty()) {
                        layoutReason.setVisibility(View.VISIBLE);
                        tvReasonLabel.setText("Lý do hủy");
                        tvReasonValue.setText(lyDoHuy);
                    } else if (("CHO_XU_LY_HOAN".equals(st) || "HOAN_DON".equals(st)) && lyDoHoan != null && !lyDoHoan.trim().isEmpty()) {
                        layoutReason.setVisibility(View.VISIBLE);
                        tvReasonLabel.setText("Lý do hoàn đơn");
                        tvReasonValue.setText(lyDoHoan);
                    } else {
                        layoutReason.setVisibility(View.GONE);
                    }
                }

                if (layoutAdminMsg != null && tvAdminMsgLabel != null && tvAdminMsgValue != null) {
                    if (loiNhanAdminHoan != null && !loiNhanAdminHoan.trim().isEmpty()) {
                        layoutAdminMsg.setVisibility(View.VISIBLE);
                        tvAdminMsgLabel.setText("Lời nhắn từ admin");
                        tvAdminMsgValue.setText(loiNhanAdminHoan);
                        markAdminMessageSeen(orderId, loiNhanAdminHoan);
                    } else {
                        layoutAdminMsg.setVisibility(View.GONE);
                    }
                }

                if (tvRefundNotice != null) {
                    String st = trangThai == null ? "" : trangThai.trim();
                    String ps = payStatusDetail == null ? "" : payStatusDetail.trim();
                    if ("CHO_XU_LY_HOAN".equals(st) || "HOAN_DON".equals(st) || "DA_HOAN_TIEN".equals(ps)) {
                        tvRefundNotice.setVisibility(View.VISIBLE);
                        tvRefundNotice.setText("Lưu ý: Tiền hoàn có thể về tài khoản trong 7–14 ngày. Nếu quá thời gian chưa nhận được, vui lòng liên hệ CSKH.");
                    } else {
                        tvRefundNotice.setVisibility(View.GONE);
                    }
                }

                if (tvStatus != null) {
                    tvStatus.setText(toOrderStatusLabel(trangThai));
                    applyOrderStatusStyle(tvStatus, trangThai);
                }

                orderStatusHolder[0] = trangThai;

                updateActionButtons(btnCancel, btnConfirm, btnRefund, orderStatusHolder[0], paymentMethodHolder[0], paymentStatusHolder[0]);

                if (btnRefund != null) {
                    String st = trangThai == null ? "" : trangThai.trim();
                    if ("CHO_XU_LY_HOAN".equals(st)) {
                        btnRefund.setVisibility(View.VISIBLE);
                        btnRefund.setEnabled(false);
                        btnRefund.setText("Đang chờ người bán phản hồi");
                    } else {
                        btnRefund.setEnabled(true);
                        btnRefund.setText("Hoàn đơn");
                    }
                }

                if (btnRate != null) {
                    String st = trangThai == null ? "" : trangThai.trim();
                    boolean canRate = "THANH_CONG".equals(st) && hasUnratedItems(orderItemsHolder[0]);
                    btnRate.setVisibility(canRate ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OrdersActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        service.trangThaiThanhToan(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }
                JsonObject data = extractObject(response.body());
                String method = getString(data, "phuongThucThanhToan", "phuong_thuc_thanh_toan");
                String status = getString(data, "trangThaiThanhToan", "trang_thai_thanh_toan");
                String methodLabel = method.equals("VISA") ? "Thẻ (VISA)" : (method.equals("COD") ? "Tiền mặt (COD)" : method);
                String payLabel = toPaymentStatusLabel(status);
                if (tvPayment != null) {
                    tvPayment.setText(methodLabel + (payLabel.isEmpty() ? "" : (" - " + payLabel)));
                }

                paymentMethodHolder[0] = method;
                paymentStatusHolder[0] = status;
                updateActionButtons(btnCancel, btnConfirm, btnRefund, orderStatusHolder[0], paymentMethodHolder[0], paymentStatusHolder[0]);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
            }
        });
    }

    private void confirmReceived(String orderId, AlertDialog dialog) {
        OrderService service = ApiClient.createService(this, OrderService.class);
        service.xacNhanDaNhan(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String msg = extractErrorMessage(response);
                    Toast.makeText(OrdersActivity.this, msg.isEmpty() ? ("Xác nhận thất bại: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(OrdersActivity.this, "Đã xác nhận nhận hàng", Toast.LENGTH_SHORT).show();
                if (dialog != null) dialog.dismiss();
                loadOrders();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OrdersActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateActionButtons(Button btnCancel, Button btnConfirm, Button btnRefund, String orderStatus, String paymentMethod, String paymentStatus) {
        String os = orderStatus == null ? "" : orderStatus.trim();
        String pm = paymentMethod == null ? "" : paymentMethod.trim();
        String ps = paymentStatus == null ? "" : paymentStatus.trim();

        if (btnCancel != null) {
            boolean canCancel = "CHO_XAC_NHAN".equals(os);
            btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
        }

        if (btnConfirm != null) {
            btnConfirm.setVisibility("DA_GIAO_CHO_XAC_NHAN".equals(os) ? View.VISIBLE : View.GONE);
        }

        if (btnRefund != null) {
            boolean canRefund = "THANH_CONG".equals(os);
            if ("HOAN_DON".equals(os) || "HUY".equals(os)) {
                canRefund = false;
            }
            if ("CHO_XU_LY_HOAN".equals(os)) {
                btnRefund.setVisibility(View.VISIBLE);
                btnRefund.setEnabled(false);
                btnRefund.setText("Đang chờ người bán phản hồi");
            } else {
                btnRefund.setEnabled(true);
                btnRefund.setText("Hoàn đơn");
                btnRefund.setVisibility(canRefund ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void showRefundDialog(String orderId, String paymentMethod, String paymentStatus) {
        String pm = paymentMethod == null ? "" : paymentMethod.trim();
        String ps = paymentStatus == null ? "" : paymentStatus.trim();

        if (!"DA_THANH_TOAN".equals(ps)) {
            Toast.makeText(this, "Chỉ hỗ trợ hoàn đơn khi đơn đã thanh toán", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] reasons = new String[]{
                "Sản phẩm lỗi/hư hỏng",
                "Giao sai sản phẩm",
                "Không đúng mô tả",
                "Đổi ý",
                "Khác (tự nhập)"
        };

        final int[] selected = new int[]{0};
        final EditText etCustom = new EditText(this);
        etCustom.setHint("Nhập lý do");
        etCustom.setVisibility(View.GONE);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);
        container.addView(etCustom);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Hoàn đơn")
                .setSingleChoiceItems(reasons, 0, (d, which) -> {
                    selected[0] = which;
                    boolean isCustom = which == reasons.length - 1;
                    etCustom.setVisibility(isCustom ? View.VISIBLE : View.GONE);
                })
                .setView(scrollView)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .setPositiveButton("Gửi yêu cầu", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btnPositive != null) {
                btnPositive.setOnClickListener(v -> {
                    String reason = reasons[selected[0]];
                    if (selected[0] == reasons.length - 1) {
                        String custom = etCustom.getText() == null ? "" : etCustom.getText().toString().trim();
                        if (custom.isEmpty()) {
                            Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        reason = custom;
                    }

                    if ("COD".equals(pm)) {
                        dialog.dismiss();
                        showVisaInfoDialog(orderId, reason);
                        return;
                    }

                    requestRefund(orderId, reason, null, dialog);
                });
            }
        });

        dialog.show();
    }

    private void showVisaInfoDialog(String orderId, String reason) {
        VisaCardStorage.VisaCard c1 = VisaCardStorage.getCard(this, 1);
        VisaCardStorage.VisaCard c2 = VisaCardStorage.getCard(this, 2);
        if (c1 == null && c2 == null) {
            Toast.makeText(this, "Bạn chưa lưu thẻ. Vui lòng vào Tài Khoản > Thẻ Thanh Toán để thêm thẻ", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, AccountActivity.class));
            return;
        }

        if (c1 != null && c2 == null) {
            RefundOrderRequest.VisaInfo visaInfo = new RefundOrderRequest.VisaInfo(c1.soThe, c1.tenChuThe, c1.thangHetHan, c1.namHetHan, c1.cvv);
            requestRefund(orderId, reason, visaInfo, null);
            return;
        }
        if (c2 != null && c1 == null) {
            RefundOrderRequest.VisaInfo visaInfo = new RefundOrderRequest.VisaInfo(c2.soThe, c2.tenChuThe, c2.thangHetHan, c2.namHetHan, c2.cvv);
            requestRefund(orderId, reason, visaInfo, null);
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
                .setTitle("Chọn thẻ hoàn tiền")
                .setSingleChoiceItems(options.toArray(new String[0]), 0, (d, which) -> selected[0] = which)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .setPositiveButton("Tiếp tục", (d, w) -> {
                    int slot = slots.get(selected[0]);
                    VisaCardStorage.VisaCard card = VisaCardStorage.getCard(OrdersActivity.this, slot);
                    if (card == null) return;
                    RefundOrderRequest.VisaInfo visaInfo = new RefundOrderRequest.VisaInfo(card.soThe, card.tenChuThe, card.thangHetHan, card.namHetHan, card.cvv);
                    requestRefund(orderId, reason, visaInfo, null);
                })
                .show();
    }

    private void showVisaInfoDialogManual(String orderId, String reason) {
        final EditText etCardNumber = new EditText(this);
        etCardNumber.setHint("Số thẻ VISA");
        final EditText etCardName = new EditText(this);
        etCardName.setHint("Tên chủ thẻ");
        final EditText etExpMonth = new EditText(this);
        etExpMonth.setHint("Tháng hết hạn (MM)");
        final EditText etExpYear = new EditText(this);
        etExpYear.setHint("Năm hết hạn (YYYY)");
        final EditText etCvv = new EditText(this);
        etCvv.setHint("CVV");

        etCardNumber.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etCardName.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        etExpMonth.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etExpYear.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        etCvv.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);

        container.addView(etCardNumber);
        container.addView(etCardName);
        container.addView(etExpMonth);
        container.addView(etExpYear);
        container.addView(etCvv);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Thông tin thẻ VISA")
                .setMessage("Cần nhập thẻ VISA để hoàn tiền")
                .setView(scrollView)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .setPositiveButton("Gửi yêu cầu", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btnPositive != null) {
                btnPositive.setOnClickListener(v -> {
                    String soThe = etCardNumber.getText() == null ? "" : etCardNumber.getText().toString().trim();
                    String tenChuThe = etCardName.getText() == null ? "" : etCardName.getText().toString().trim();
                    String mmRaw = etExpMonth.getText() == null ? "" : etExpMonth.getText().toString().trim();
                    String yyRaw = etExpYear.getText() == null ? "" : etExpYear.getText().toString().trim();
                    String cvv = etCvv.getText() == null ? "" : etCvv.getText().toString().trim();

                    if (soThe.isEmpty() || soThe.length() < 12) {
                        Toast.makeText(this, "Vui lòng nhập số thẻ hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tenChuThe.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập tên chủ thẻ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int mm;
                    int yy;
                    try {
                        mm = Integer.parseInt(mmRaw);
                        yy = Integer.parseInt(yyRaw);
                    } catch (Exception e) {
                        Toast.makeText(this, "Vui lòng nhập tháng/năm hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mm < 1 || mm > 12) {
                        Toast.makeText(this, "Tháng hết hạn không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                    if (yy < currentYear) {
                        Toast.makeText(this, "Năm hết hạn không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (cvv.isEmpty() || cvv.length() < 3) {
                        Toast.makeText(this, "Vui lòng nhập CVV hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    RefundOrderRequest.VisaInfo visaInfo = new RefundOrderRequest.VisaInfo(soThe, tenChuThe, mm, yy, cvv);
                    requestRefund(orderId, reason, visaInfo, dialog);
                });
            }
        });

        dialog.show();
    }

    private void requestRefund(String orderId, String reason, RefundOrderRequest.VisaInfo visaInfo, AlertDialog dialog) {
        if (reason == null || reason.trim().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
            return;
        }

        OrderService service = ApiClient.createService(this, OrderService.class);
        service.hoanDon(orderId, new RefundOrderRequest(reason.trim(), visaInfo)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String msg = extractErrorMessage(response);
                    String lower = msg == null ? "" : msg.toLowerCase();
                    if (lower.contains("can nhap") && lower.contains("visa") && (lower.contains("hoan") || lower.contains("refund"))) {
                        markLocalRefundRequested(orderId, reason);
                        Toast.makeText(OrdersActivity.this, "Đã gửi yêu cầu hoàn đơn, vui lòng chờ admin xử lý", Toast.LENGTH_SHORT).show();
                        if (dialog != null) dialog.dismiss();
                        showOrderDetailDialog(orderId);
                        loadOrders();
                        return;
                    }
                    Toast.makeText(OrdersActivity.this, msg.isEmpty() ? ("Hoàn đơn thất bại: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                markLocalRefundRequested(orderId, reason);
                Toast.makeText(OrdersActivity.this, "Đã gửi yêu cầu hoàn đơn, vui lòng chờ admin xử lý", Toast.LENGTH_SHORT).show();
                if (dialog != null) dialog.dismiss();
                showOrderDetailDialog(orderId);
                loadOrders();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(OrdersActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private JsonObject extractObject(JsonObject body) {
        if (body == null) return new JsonObject();
        if (body.has("data") && body.get("data").isJsonObject()) {
            return body.getAsJsonObject("data");
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
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }

    private void applyOrderStatusStyle(TextView tv, String status) {
        if (tv == null) return;
        String s = status == null ? "" : status.trim();
        if (s.equals("THANH_CONG")) {
            tv.setBackgroundResource(R.drawable.bg_badge_green);
            tv.setTextColor(0xFF166534);
            return;
        }
        if (s.equals("HUY") || s.equals("HOAN_DON")) {
            tv.setBackgroundResource(R.drawable.bg_badge_red);
            tv.setTextColor(0xFF991B1B);
            return;
        }
        tv.setBackgroundResource(R.drawable.bg_badge_yellow);
        tv.setTextColor(0xFF92400E);
    }

    private String toOrderStatusLabel(String status) {
        if (status == null) return "-";
        switch (status) {
            case "CHO_XAC_NHAN":
                return "Chờ xử lý";
            case "DA_XAC_NHAN":
                return "Đã xác nhận";
            case "DANG_GIAO":
                return "Đang giao";
            case "DA_GIAO_CHO_XAC_NHAN":
                return "Chờ xác nhận";
            case "THANH_CONG":
                return "Thành công";
            case "CHO_XU_LY_HOAN":
                return "Chờ xử lý hoàn";
            case "HUY":
                return "Đã hủy";
            case "HOAN_DON":
                return "Hoàn đơn thành công";
            default:
                return status;
        }
    }

    private String toPaymentStatusLabel(String status) {
        if (status == null) return "";
        switch (status) {
            case "CHUA_THANH_TOAN":
                return "Chưa thanh toán";
            case "DA_THANH_TOAN":
                return "Đã thanh toán";
            case "DA_HOAN_TIEN":
                return "Đã hoàn tiền";
            default:
                return status;
        }
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
