package com.example.appbanghe;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class AvatarPickerAdapter extends RecyclerView.Adapter<AvatarPickerAdapter.VH> {

    public interface Listener {
        void onPick(String avatarName);
    }

    private final Context context;
    private final Listener listener;
    private final List<String> items = new ArrayList<>();

    public AvatarPickerAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<String> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_avatar, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String name = items.get(position);
        Object model = AvatarUtil.resolveModel(context, name, name);
        Glide.with(context)
                .load(model)
                .circleCrop()
                .into(holder.img);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPick(name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img;

        VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.img_avatar_item);
        }
    }
}
