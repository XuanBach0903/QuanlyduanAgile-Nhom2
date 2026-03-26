package com.example.appbanghe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.Locale;

public class OrderSummaryAdapter extends RecyclerView.Adapter<OrderSummaryAdapter.VH> {

    private final Context context;
    private JsonArray items = new JsonArray();

    public OrderSummaryAdapter(Context context) {
        this.context = context;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_order_summary, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject item = getObject(items, position);
        if (item == null) {
            holder.tvNameQty.setText("Sản phẩm x0");
            holder.tvPrice.setText("0đ");
            return;
        }

        String name = extractString(item, "tenSanPham", "ten", "name");
        int qty = extractInt(item, "soLuong", "qty", "quantity");
        long unitPrice = extractLong(item, "gia", "donGia", "price");

        if (item.has("sanPham") && item.get("sanPham").isJsonObject()) {
            JsonObject sp = item.getAsJsonObject("sanPham");
            if (name.isEmpty()) {
                name = extractString(sp, "ten", "tenSanPham", "name");
            }
            if (unitPrice <= 0) {
                unitPrice = extractLong(sp, "gia", "donGia", "price");
            }
        }

        qty = Math.max(0, qty);
        long total = unitPrice * qty;

        holder.tvNameQty.setText((name.isEmpty() ? "Sản phẩm" : name) + " × " + qty);
        holder.tvPrice.setText(formatMoney(total));
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvNameQty;
        TextView tvPrice;

        VH(@NonNull View itemView) {
            super(itemView);
            tvNameQty = itemView.findViewById(R.id.tv_item_name_qty);
            tvPrice = itemView.findViewById(R.id.tv_item_total);
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

    private long extractLong(JsonObject obj, String... keys) {
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
}
