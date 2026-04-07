package com.example.appbanghe;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
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

public class ReviewOrderItemsAdapter extends RecyclerView.Adapter<ReviewOrderItemsAdapter.VH> {

    private final Context context;
    private JsonArray items = new JsonArray();

    public ReviewOrderItemsAdapter(Context context) {
        this.context = context;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        notifyDataSetChanged();
    }

    public JsonArray getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_review_order_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject obj = getObject(items, position);
        if (obj == null) {
            holder.tvName.setText("Sản phẩm");
            holder.tvMeta.setText("");
            holder.tvTotal.setText("0đ");
            holder.tvAlready.setVisibility(View.GONE);
            holder.rating.setRating(0);
            holder.etContent.setText(null);
            holder.rating.setIsIndicator(false);
            holder.etContent.setEnabled(true);
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

        String finalUrl = toAbsoluteImageUrl(imageUrl);
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

        boolean hasReview = obj.has("danhGia") && obj.get("danhGia").isJsonObject();
        if (hasReview) {
            JsonObject dg = obj.getAsJsonObject("danhGia");
            int stars = getInt(dg, "soSao", "so_sao");
            String content = getString(dg, "noiDung", "noi_dung");

            holder.rating.setRating(Math.max(0, Math.min(5, stars)));
            holder.rating.setIsIndicator(true);
            holder.etContent.setText(content);
            holder.etContent.setEnabled(false);
            holder.tvAlready.setVisibility(View.VISIBLE);
            holder.tvAlready.setText("Bạn đã đánh giá sản phẩm này");

            obj.addProperty("_draftStars", stars);
            obj.addProperty("_draftContent", content);
        } else {
            holder.rating.setIsIndicator(false);
            holder.etContent.setEnabled(true);
            holder.tvAlready.setVisibility(View.GONE);

            if (obj.has("_draftStars")) {
                holder.rating.setRating(getInt(obj, "_draftStars"));
            } else {
                holder.rating.setRating(0);
            }

            if (obj.has("_draftContent")) {
                holder.etContent.setText(getString(obj, "_draftContent"));
            } else {
                holder.etContent.setText(null);
            }

            holder.rating.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
                if (!fromUser) return;
                obj.addProperty("_draftStars", (int) rating);
            });

            holder.etContent.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    obj.addProperty("_draftContent", s == null ? "" : s.toString());
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
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
        TextView tvTotal;
        RatingBar rating;
        EditText etContent;
        TextView tvAlready;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img);
            tvName = itemView.findViewById(R.id.tv_name);
            tvMeta = itemView.findViewById(R.id.tv_meta);
            tvTotal = itemView.findViewById(R.id.tv_total);
            rating = itemView.findViewById(R.id.rating);
            etContent = itemView.findViewById(R.id.et_content);
            tvAlready = itemView.findViewById(R.id.tv_already);
        }
    }

    private static String getString(JsonObject obj, String... keys) {
        if (obj == null) return "";
        for (String key : keys) {
            if (!obj.has(key)) continue;
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) continue;
            try {
                String v = el.getAsString();
                if (v != null) return v;
            } catch (Exception ignored) {
            }
        }
        return "";
    }

    private static int getInt(JsonObject obj, String... keys) {
        if (obj == null) return 0;
        for (String key : keys) {
            if (!obj.has(key)) continue;
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) continue;
            try {
                return el.getAsInt();
            } catch (Exception e) {
                try {
                    return Integer.parseInt(el.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private static long getLong(JsonObject obj, String... keys) {
        if (obj == null) return 0;
        for (String key : keys) {
            if (!obj.has(key)) continue;
            JsonElement el = obj.get(key);
            if (el == null || el.isJsonNull()) continue;
            try {
                return el.getAsLong();
            } catch (Exception e) {
                try {
                    return Long.parseLong(el.getAsString());
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
    }

    private JsonObject getObject(JsonArray arr, int index) {
        if (arr == null || index < 0 || index >= arr.size()) return null;
        JsonElement el = arr.get(index);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
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

    private static String formatMoney(long vnd) {
        if (vnd <= 0) return "0đ";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }
}
