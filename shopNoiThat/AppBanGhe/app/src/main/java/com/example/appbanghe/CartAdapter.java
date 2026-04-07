package com.example.appbanghe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.VH> {

    public interface Listener {
        void onChangeQty(String itemId, int newQty);

        void onDelete(String itemId);
    }

    private final Context context;
    private final Listener listener;
    private JsonArray items = new JsonArray();

    public CartAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject item = getObject(items, position);
        if (item == null) {
            holder.tvName.setText("Sản phẩm");
            holder.tvQty.setText("1");
            holder.tvUnitPrice.setText("0đ");
            holder.tvTotalPrice.setText("0đ");
            return;
        }

        String itemId = extractString(item, "id", "itemId", "_id");
        String name = extractString(item, "tenSanPham", "ten", "name");
        int qty = extractInt(item, "soLuong", "qty", "quantity");
        long unitPrice = extractLong(item, "gia", "donGia", "price");

        String imageUrl = extractString(item, "hinhDaiDien", "image");

        if (item.has("sanPham") && item.get("sanPham").isJsonObject()) {
            JsonObject sp = item.getAsJsonObject("sanPham");
            if (name.isEmpty()) {
                name = extractString(sp, "ten", "tenSanPham", "name");
            }
            if (unitPrice <= 0) {
                unitPrice = extractLong(sp, "gia", "donGia", "price");
            }
            if (imageUrl.isEmpty()) {
                imageUrl = extractString(sp, "hinhDaiDien", "image");
                if (imageUrl.isEmpty() && sp.has("hinhAnh") && sp.get("hinhAnh").isJsonArray()) {
                    JsonArray arr = sp.getAsJsonArray("hinhAnh");
                    if (arr.size() > 0 && arr.get(0).isJsonPrimitive()) {
                        try {
                            imageUrl = arr.get(0).getAsString();
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        qty = Math.max(1, qty);
        long totalPrice = unitPrice * qty;

        final int finalQty = qty;
        final String finalItemId = itemId;

        holder.tvName.setText(name.isEmpty() ? "Sản phẩm" : name);
        holder.tvQty.setText(String.valueOf(qty));
        holder.tvUnitPrice.setText(formatMoney(unitPrice));
        holder.tvTotalPrice.setText(formatMoney(totalPrice));

        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .into(holder.img);

        holder.btnMinus.setOnClickListener(v -> {
            if (listener != null && !finalItemId.isEmpty()) {
                listener.onChangeQty(finalItemId, Math.max(1, finalQty - 1));
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (listener != null && !finalItemId.isEmpty()) {
                listener.onChangeQty(finalItemId, finalQty + 1);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null && !finalItemId.isEmpty()) {
                listener.onDelete(finalItemId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;
        TextView tvQty;
        TextView tvUnitPrice;
        TextView tvTotalPrice;
        ImageButton btnMinus;
        ImageButton btnPlus;
        ImageButton btnDelete;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_cart);
            tvName = itemView.findViewById(R.id.tv_cart_name);
            tvQty = itemView.findViewById(R.id.tv_cart_qty);
            tvUnitPrice = itemView.findViewById(R.id.tv_cart_unit_price);
            tvTotalPrice = itemView.findViewById(R.id.tv_cart_price);
            btnMinus = itemView.findViewById(R.id.btn_qty_minus);
            btnPlus = itemView.findViewById(R.id.btn_qty_plus);
            btnDelete = itemView.findViewById(R.id.btn_delete);
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

    private int extractInt(JsonObject obj, String... keys) {
        if (obj == null || keys == null) return 0;
        for (String k : keys) {
            if (k == null) continue;
            if (obj.has(k) && !obj.get(k).isJsonNull()) {
                try {
                    return obj.get(k).getAsInt();
                } catch (Exception ignored) {
                }
            }
        }
        return 0;
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

    private String formatMoney(long vnd) {
        if (vnd <= 0) return "0đ";
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }
}
