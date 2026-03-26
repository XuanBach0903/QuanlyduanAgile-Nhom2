package com.example.appbanghe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {

    public interface Listener {
        void onDetail(JsonObject item);

        void onAddToCart(JsonObject item);
    }

    private static double getDouble(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return 0;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return 0;
        }
        try {
            return el.getAsDouble();
        } catch (Exception e) {
            try {
                return Double.parseDouble(el.getAsString());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private final Context context;
    private final Listener listener;
    private final List<JsonObject> items = new ArrayList<>();

    public ProductAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<JsonObject> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void appendItems(List<JsonObject> more) {
        if (more == null || more.isEmpty()) {
            return;
        }
        int start = items.size();
        items.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject item = items.get(position);

        holder.tvName.setText(getString(item, "ten"));
        holder.tvPrice.setText(formatMoney(getLong(item, "gia")));

        double rating = getDouble(item, "soSaoTrungBinh");
        if (rating <= 0) {
            rating = getDouble(item, "rating");
        }
        if (holder.tvRating != null) {
            holder.tvRating.setText("★ " + String.format(Locale.US, "%.1f", rating));
        }

        if (holder.tvSold != null) {
            long sold = getLong(item, "daBan");
            holder.tvSold.setText("Đã bán " + formatSold((int) sold));
        }

        long stock = getLong(item, "tonKhoConLai");
        if (stock == 0) {
            stock = getLong(item, "tonKho");
        }
        if (stock == 0) {
            stock = getLong(item, "soLuongTon");
        }
        if (stock == 0) {
            stock = getLong(item, "ton");
        }

        holder.tvStock.setVisibility(View.GONE);

        String imageUrl = getString(item, "hinhDaiDien");
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = getString(item, "image");
        }

        Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .into(holder.img);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDetail(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView tvName;
        TextView tvPrice;
        TextView tvStock;
        TextView tvRating;
        TextView tvSold;
        Button btnDetail;
        Button btnAdd;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_product);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvStock = itemView.findViewById(R.id.tv_stock);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvSold = itemView.findViewById(R.id.tv_sold);
            btnDetail = itemView.findViewById(R.id.btn_detail);
            btnAdd = itemView.findViewById(R.id.btn_add_cart);
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return "";
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return "";
        }
        try {
            return el.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static long getLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return 0;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return 0;
        }
        try {
            return el.getAsLong();
        } catch (Exception e) {
            try {
                return Long.parseLong(el.getAsString());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }

    private static String formatMoney(long vnd) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(vnd) + "đ";
    }

    private static String formatSold(int sold) {
        // Luôn hiển thị đúng số lượng đã bán, không rút gọn thành k+
        return String.valueOf(sold);
    }
}
