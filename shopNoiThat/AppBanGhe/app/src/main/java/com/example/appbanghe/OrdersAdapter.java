package com.example.appbanghe;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appbanghe.network.ApiConfig;
import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.services.OrderService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    private final Context context;
    private JsonArray items = new JsonArray();

    private final Map<String, JsonArray> orderItemsCache = new HashMap<>();
    private final Set<String> inFlight = new HashSet<>();

    public interface Listener {
        void onViewDetail(String orderId);
    }

    private Listener listener;

    public OrdersAdapter(Context context) {
        this.context = context;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        orderItemsCache.clear();
        inFlight.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject obj = getObject(items, position);
        if (obj == null) {
            holder.tvOrderId.setText("Đơn hàng");
            holder.tvOrderDate.setText("-");
            holder.tvOrderStatusBadge.setText("-");
            holder.tvPaymentStatus.setText("-");
            holder.tvTotal.setText("0đ");
            return;
        }

        String id = extractString(obj, "id", "_id", "ma", "maDonHang", "code");
        String ngayTao = extractString(obj, "ngayTao", "ngay_tao", "createdAt");

        String trangThaiDonHang = extractString(obj, "trangThaiDonHang", "trang_thai_don_hang", "trangThai", "status");
        String trangThaiThanhToan = extractString(obj, "trangThaiThanhToan", "trang_thai_thanh_toan");
        if ("DA_HOAN_TIEN".equals(trangThaiThanhToan)) {
            trangThaiDonHang = "HOAN_DON";
        }
        long total = extractLong(obj, "tongTien", "thanhTien", "total");

        holder.tvOrderId.setText(id.isEmpty() ? "Đơn hàng" : id);
        holder.tvOrderDate.setText(ngayTao.isEmpty() ? "" : ("Ngày đặt: " + ngayTao));
        holder.tvOrderStatusBadge.setText(trangThaiDonHang.isEmpty() ? "-" : toOrderStatusLabel(trangThaiDonHang));
        applyOrderStatusStyle(holder, trangThaiDonHang);
        holder.tvPaymentStatus.setText(trangThaiThanhToan.isEmpty() ? "" : toPaymentStatusLabel(trangThaiThanhToan));
        holder.tvTotal.setText(formatMoney(total));

        JsonArray orderItems = null;
        if (obj.has("items") && obj.get("items").isJsonArray()) {
            orderItems = obj.getAsJsonArray("items");
        }
        if (orderItems == null && !id.isEmpty()) {
            orderItems = orderItemsCache.get(id);
        }

        if (orderItems != null) {
            renderPreview(holder.previewContainer, orderItems);
        } else {
            if (holder.previewContainer != null) {
                holder.previewContainer.removeAllViews();
            }
            if (!id.isEmpty()) {
                maybeFetchOrderItems(id, holder);
            }
        }

        if (holder.btnViewDetail != null) {
            holder.btnViewDetail.setOnClickListener(v -> {
                if (listener != null && !id.isEmpty()) {
                    listener.onViewDetail(id);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderId;
        TextView tvOrderDate;
        TextView tvOrderStatusBadge;
        TextView tvPaymentStatus;
        TextView tvTotal;
        LinearLayout previewContainer;
        Button btnViewDetail;

        VH(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvOrderDate = itemView.findViewById(R.id.tv_order_date);
            tvOrderStatusBadge = itemView.findViewById(R.id.tv_order_status_badge);
            tvPaymentStatus = itemView.findViewById(R.id.tv_payment_status);
            tvTotal = itemView.findViewById(R.id.tv_order_total);
            previewContainer = itemView.findViewById(R.id.preview_container);
            btnViewDetail = itemView.findViewById(R.id.btn_view_detail);
        }
    }

    private JsonObject getObject(JsonArray arr, int index) {
        if (arr == null || index < 0 || index >= arr.size()) return null;
        JsonElement el = arr.get(index);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private String extractString(JsonObject obj, String... keys) {
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

    private long extractLong(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return 0;
        for (String k : keys) {
            if (k == null) continue;
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try {
                    return obj.get(k).getAsLong();
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }


    private void maybeFetchOrderItems(String orderId, VH holder) {
        if (orderId == null || orderId.trim().isEmpty()) return;
        if (orderItemsCache.containsKey(orderId)) return;
        if (inFlight.contains(orderId)) return;
        inFlight.add(orderId);

        OrderService service = ApiClient.createService(context, OrderService.class);
        service.chiTietDonHang(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                inFlight.remove(orderId);
                if (!response.isSuccessful() || response.body() == null) {
                    return;
                }

                JsonObject data = unwrapData(response.body());
                JsonArray list = null;
                if (data.has("items") && data.get("items").isJsonArray()) {
                    list = data.getAsJsonArray("items");
                }
                if (list == null && data.has("danhSach") && data.get("danhSach").isJsonArray()) {
                    list = data.getAsJsonArray("danhSach");
                }

                if (list == null) {
                    list = new JsonArray();
                }
                orderItemsCache.put(orderId, list);

                int pos = holder == null ? RecyclerView.NO_POSITION : holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    notifyItemChanged(pos);
                } else {
                    notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                inFlight.remove(orderId);
            }
        });
    }

    private void renderPreview(LinearLayout container, JsonArray items) {
        if (container == null) return;
        container.removeAllViews();
        if (items == null || items.size() == 0) return;

        for (int i = 0; i < items.size(); i++) {
            JsonObject it = items.get(i).isJsonObject() ? items.get(i).getAsJsonObject() : null;
            if (it == null) continue;

            View row = LayoutInflater.from(context).inflate(R.layout.item_order_preview, container, false);
            ImageView img = row.findViewById(R.id.img_preview);
            TextView tvName = row.findViewById(R.id.tv_preview_name);
            TextView tvMeta = row.findViewById(R.id.tv_preview_meta);
            TextView tvTotal = row.findViewById(R.id.tv_preview_total);

            String name = extractString(it, "tenSanPham", "ten", "name");
            int qty = (int) extractLong(it, "soLuong", "qty", "quantity");
            long unit = extractLong(it, "donGia", "gia", "price");
            String imageUrl = extractImageUrl(it);

            if (it.has("sanPham") && it.get("sanPham").isJsonObject()) {
                JsonObject sp = it.getAsJsonObject("sanPham");
                if (name.isEmpty()) {
                    name = extractString(sp, "ten", "tenSanPham", "name");
                }
                if (unit <= 0) {
                    unit = extractLong(sp, "gia", "donGia", "price");
                }
                if (imageUrl.isEmpty()) {
                    imageUrl = extractImageUrl(sp);
                }
            }

            qty = Math.max(1, qty);
            long total = unit * qty;

            if (tvName != null) tvName.setText(name.isEmpty() ? "Sản phẩm" : name);
            if (tvMeta != null) tvMeta.setText("Số lượng: " + qty + " | Đơn giá: " + formatMoney(unit));
            if (tvTotal != null) tvTotal.setText(formatMoney(total));

            if (img != null) {
                String finalUrl = toAbsoluteImageUrl(imageUrl);
                Log.d("OrdersAdapter", "preview imageUrl=" + imageUrl + " -> " + finalUrl);
                if (finalUrl == null || finalUrl.trim().isEmpty()) {
                    img.setImageResource(android.R.drawable.ic_menu_gallery);
                } else {
                    Glide.with(context)
                            .load(finalUrl)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .centerCrop()
                            .into(img);
                }
            }

            container.addView(row);

            if (i < items.size() - 1) {
                View spacer = new View(context);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (6 * context.getResources().getDisplayMetrics().density));
                spacer.setLayoutParams(lp);
                container.addView(spacer);
            }
        }
    }

    private JsonObject unwrapData(JsonObject body) {
        if (body == null) return new JsonObject();
        if (body.has("data") && body.get("data").isJsonObject()) {
            return body.getAsJsonObject("data");
        }
        return body;
    }

    private String extractImageUrl(JsonObject obj) {
        if (obj == null) return "";
        String url = extractString(obj, "hinhDaiDien", "image", "anh", "imageUrl", "url");
        if (!url.isEmpty()) return url;
        if (obj.has("hinhAnh") && obj.get("hinhAnh").isJsonArray()) {
            try {
                JsonArray arr = obj.getAsJsonArray("hinhAnh");
                if (arr.size() > 0 && arr.get(0).isJsonPrimitive()) {
                    return arr.get(0).getAsString();
                }
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private String toAbsoluteImageUrl(String rawUrl) {
        if (rawUrl == null) return "";
        String url = rawUrl.trim();
        if (url.isEmpty()) return "";

        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
                    .replace("http://localhost", "http://10.0.2.2")
                    .replace("http://127.0.0.1", "http://10.0.2.2")
                    .replace("https://localhost", "https://10.0.2.2")
                    .replace("https://127.0.0.1", "https://10.0.2.2");
        }

        String base = ApiConfig.BASE_URL;
        if (base == null) base = "";
        if (!base.endsWith("/")) base = base + "/";

        if (url.startsWith("/")) {
            url = url.substring(1);
        }
        return base + url;
    }

    private void applyOrderStatusStyle(VH holder, String status) {
        if (holder == null || holder.tvOrderStatusBadge == null) return;
        String s = status == null ? "" : status.trim();
        if (s.equals("THANH_CONG")) {
            holder.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_green);
            holder.tvOrderStatusBadge.setTextColor(0xFF166534);
            return;
        }
        if (s.equals("HUY") || s.equals("HOAN_DON")) {
            holder.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_red);
            holder.tvOrderStatusBadge.setTextColor(0xFF991B1B);
            return;
        }
        holder.tvOrderStatusBadge.setBackgroundResource(R.drawable.bg_badge_yellow);
        holder.tvOrderStatusBadge.setTextColor(0xFF92400E);
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

    private String formatMoney(long vnd) {
        if (vnd <= 0) return "0đ";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }
}
