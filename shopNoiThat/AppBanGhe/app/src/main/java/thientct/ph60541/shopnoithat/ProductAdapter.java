package thientct.ph60541.shopnoithat;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private Context context;
    private Listener listener;
    private List<JsonObject> items;

    public interface Listener {
        void onDetail(JsonObject item);
        void onAddToCart(JsonObject item);
    }

    public ProductAdapter(Context context, Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<JsonObject> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void appendItems(List<JsonObject> items) {
        if (this.items == null) {
            this.items = items;
        } else {
            this.items.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (items == null || position >= items.size()) {
            return;
        }

        JsonObject item = items.get(position);
        
        // Set product name
        String name = getString(item, "ten");
        if (name.isEmpty()) {
            name = getString(item, "name");
        }
        holder.tvName.setText(name);

        // Set price
        Integer price = getInt(item, "gia");
        if (price != null) {
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            holder.tvPrice.setText(formatter.format(price) + " đ");
        } else {
            holder.tvPrice.setText("Liên hệ");
        }

        // Set image with emoji fallback
        String imageUrl = getString(item, "hinhDaiDien");
        if (imageUrl.isEmpty()) {
            imageUrl = getString(item, "image");
        }
        
        // Use emoji if no image URL
        if (!imageUrl.isEmpty() && imageUrl.startsWith("🪑") || imageUrl.startsWith("🪑") || imageUrl.startsWith("🛋️") || imageUrl.startsWith("🗄️") || imageUrl.startsWith("📚") || imageUrl.startsWith("🛏️")) {
            holder.imgProduct.setText(imageUrl);
            holder.imgProduct.setTextSize(48);
            holder.imgProduct.setBackgroundColor(0xFFF5F5F5);
            holder.imgProduct.setGravity(android.view.Gravity.CENTER);
        } else {
            holder.imgProduct.setText("");
            holder.imgProduct.setBackgroundColor(0xFFFFFFFF);
            if (!imageUrl.isEmpty()) {
                Glide.with(context)
                        .load(imageUrl)
                        .placeholder(R.drawable.bg_input)
                        .error(R.drawable.bg_input)
                        .into(holder.imgProduct);
            }
        }

        // Set stock status
        Integer stock = getInt(item, "tonKho");
        if (stock != null) {
            if (stock > 0) {
                holder.tvStock.setText("✓ Còn " + stock);
                holder.tvStock.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                holder.tvStock.setText("✗ Hết hàng");
                holder.tvStock.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            }
        } else {
            holder.tvStock.setVisibility(View.GONE);
        }

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDetail(item);
            }
        });

        holder.btnAddToCart.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAddToCart(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgProduct;
        TextView tvName;
        TextView tvPrice;
        TextView tvStock;
        ImageView btnAddToCart;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.img_product);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvStock = itemView.findViewById(R.id.tv_stock);
            btnAddToCart = itemView.findViewById(R.id.btn_add_to_cart);
        }
    }

    private static String getString(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return "";
        }
        try {
            return obj.get(key).getAsString();
        } catch (Exception e) {
            return "";
        }
    }

    private static Integer getInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        try {
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(obj.get(key).getAsString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
