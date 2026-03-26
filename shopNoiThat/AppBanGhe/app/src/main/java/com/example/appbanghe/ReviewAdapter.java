package com.example.appbanghe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {

    private final Context context;
    private JsonArray items = new JsonArray();

    public ReviewAdapter(Context context) {
        this.context = context;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject obj = getObject(items, position);
        if (obj == null) {
            holder.tvName.setText("Khách hàng");
            holder.tvStars.setText("★★★★★");
            holder.tvContent.setText("");
            holder.tvDate.setText("");
            if (holder.imgAvatar != null) {
                Object model = AvatarUtil.resolveModel(context, "", null);
                Glide.with(context)
                        .load(model)
                        .circleCrop()
                        .into(holder.imgAvatar);
            }
            return;
        }

        String name = getString(obj, "nguoiDungTen");
        String imgUrl = getString(obj, "nguoiDungImgUrl");
        String uid = getString(obj, "nguoiDungId");
        int stars = (int) getLong(obj, "soSao");
        String content = getString(obj, "noiDung");
        String date = getString(obj, "ngayTao");

        holder.tvName.setText(name.isEmpty() ? "Khách hàng" : name);
        holder.tvStars.setText(renderStars(stars));
        holder.tvContent.setText(content);
        holder.tvDate.setText(formatDate(date));

        if (holder.imgAvatar != null) {
            String seed = !uid.isEmpty() ? uid : name;
            Object model = AvatarUtil.resolveModel(context, imgUrl, seed);
            Glide.with(context)
                    .load(model)
                    .circleCrop()
                    .into(holder.imgAvatar);
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView tvName;
        TextView tvStars;
        TextView tvContent;
        TextView tvDate;

        VH(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.img_review_avatar);
            tvName = itemView.findViewById(R.id.tv_review_name);
            tvStars = itemView.findViewById(R.id.tv_review_stars);
            tvContent = itemView.findViewById(R.id.tv_review_content);
            tvDate = itemView.findViewById(R.id.tv_review_date);
        }
    }

    private JsonObject getObject(JsonArray arr, int index) {
        if (arr == null || index < 0 || index >= arr.size()) return null;
        JsonElement el = arr.get(index);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "";
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return "";
        try {
            return el.getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static long getLong(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return 0;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return 0;
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

    private static String renderStars(int soSao) {
        int s = Math.max(0, Math.min(5, soSao));
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            sb.append(i <= s ? '★' : '☆');
        }
        return sb.toString();
    }

    private static String formatDate(String iso) {
        if (iso == null) return "";
        String v = iso.trim();
        if (v.length() >= 10) {
            return v.substring(0, 10);
        }
        return v;
    }
}
