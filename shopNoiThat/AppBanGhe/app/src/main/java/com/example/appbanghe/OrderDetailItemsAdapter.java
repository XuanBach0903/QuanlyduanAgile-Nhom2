package com.example.appbanghe;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.appbanghe.network.ApiConfig;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

public class OrderDetailItemsAdapter extends RecyclerView.Adapter<OrderDetailItemsAdapter.VH> {

    private final Context context;
    private JsonArray items = new JsonArray();

    public OrderDetailItemsAdapter(Context context) {
        this.context = context;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_order_detail_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject obj = getObject(items, position);
        if (obj == null) {
            holder.tvName.setText("Sản phẩm");
            holder.tvMeta.setText("Số lượng: 0 | Đơn giá: 0đ");
            holder.tvTotal.setText("0đ");
            return;
        }

        String name = getString(obj, "tenSanPham", "ten", "name");
        int qty = getInt(obj, "soLuong", "qty", "quantity");
        long unit = getLong(obj, "donGia", "gia", "price");
        String imageUrl = getImageUrl(obj);

        if (obj.has("sanPham") && obj.get("sanPham").isJsonObject()) {
            JsonObject sp = obj.getAsJsonObject("sanPham");
            if (name.isEmpty()) {
                name = getString(sp, "ten", "tenSanPham", "name");
            }
            if (unit <= 0) {
                unit = getLong(sp, "gia", "donGia", "price");
            }
            if (imageUrl.isEmpty()) {
                imageUrl = getImageUrl(sp);
            }
        }
        long total = unit * Math.max(0, qty);

        holder.tvName.setText(name.isEmpty() ? "Sản phẩm" : name);
        holder.tvMeta.setText("Số lượng: " + Math.max(0, qty) + " | Đơn giá: " + formatMoney(unit));
        holder.tvTotal.setText(formatMoney(total));

        if (holder.tvReview != null) {
            if (obj.has("danhGia") && obj.get("danhGia").isJsonObject()) {
                JsonObject dg = obj.getAsJsonObject("danhGia");
                int stars = (int) getLong(dg, "soSao", "so_sao");
                String content = getString(dg, "noiDung", "noi_dung");
                holder.tvReview.setVisibility(View.VISIBLE);
                holder.tvReview.setText("Đã đánh giá: " + renderStars(stars) + (content.isEmpty() ? "" : (" - " + content)));
            } else {
                holder.tvReview.setVisibility(View.GONE);
            }
        }

        if (holder.img != null) {
            String finalUrl = toAbsoluteImageUrl(imageUrl);
            Log.d("OrderDetailItemsAdapter", "detail imageUrl=" + imageUrl + " -> " + finalUrl);
            if (finalUrl == null || finalUrl.trim().isEmpty()) {
                holder.img.setImageResource(android.R.drawable.ic_menu_gallery);
            } else {
                Glide.with(context)
                        .load(finalUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(holder.img);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;
        TextView tvMeta;
        TextView tvReview;
        TextView tvTotal;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_detail_item);
            tvName = itemView.findViewById(R.id.tv_detail_item_name);
            tvMeta = itemView.findViewById(R.id.tv_detail_item_meta);
            tvReview = itemView.findViewById(R.id.tv_detail_item_review);
            tvTotal = itemView.findViewById(R.id.tv_detail_item_total);
        }
    }

    private String getImageUrl(JsonObject obj) {
        if (obj == null) return "";
        String url = getString(obj, "hinhDaiDien", "image", "anh", "imageUrl", "url");
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

    private JsonObject getObject(JsonArray arr, int index) {
        if (arr == null || index < 0 || index >= arr.size()) return null;
        JsonElement el = arr.get(index);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
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

    private static String renderStars(int soSao) {
        int s = Math.max(0, Math.min(5, soSao));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= s ? '★' : '☆');
        }
        return sb.toString();
    }
}
