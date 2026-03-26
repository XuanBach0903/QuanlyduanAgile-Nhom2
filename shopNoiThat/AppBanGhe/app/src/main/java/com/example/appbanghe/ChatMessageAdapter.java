package com.example.appbanghe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.view.Gravity;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {

    private final Context context;
    private JsonArray items = new JsonArray();

    public ChatMessageAdapter(Context context) {
        this.context = context;
    }

    public void setItems(JsonArray items) {
        this.items = items == null ? new JsonArray() : items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        JsonObject obj = getObject(items, position);
        String content = extractString(obj, "noiDung", "content", "message");
        String role = extractString(obj, "vaiTro", "vai_tro", "role");
        holder.tvMessage.setText(content.isEmpty() ? "" : content);

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (holder.container != null) {
            holder.container.setGravity(isAdmin ? Gravity.START : Gravity.END);
        }

        if (holder.tvMessage != null) {
            holder.tvMessage.setBackgroundResource(isAdmin ? R.drawable.bg_chat_bubble_admin : R.drawable.bg_chat_bubble_user);
            holder.tvMessage.setTextColor(holder.tvMessage.getResources().getColor(isAdmin ? R.color.text_primary : R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return items == null ? 0 : items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvMessage;
        LinearLayout container;

        VH(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            container = itemView.findViewById(R.id.container);
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
}
