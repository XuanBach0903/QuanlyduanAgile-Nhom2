package thientct.ph60541.shopnoithat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.graphics.Rect;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private EditText etSearch;
    private ChipGroup chipGroup;
    private RecyclerView rvProducts;
    private ProductAdapter adapter;

    private TextView tvCartBadge;
    private TextView tvOrdersBadge;
    private ImageView imgUserAvatar;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private String selectedCategoryId = null;
    private String keyword = "";
    private int page = 1;
    private final int limit = 6;
    private int totalPages = Integer.MAX_VALUE;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        drawerLayout = findViewById(R.id.drawer_layout);
        etSearch = findViewById(R.id.et_search);
        imgUserAvatar = findViewById(R.id.img_user_avatar);
        chipGroup = findViewById(R.id.chip_group_categories);
        rvProducts = findViewById(R.id.rv_products);
        tvCartBadge = findViewById(R.id.tv_cart_badge);

        if (imgUserAvatar != null) {
            imgUserAvatar.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AccountActivity.class)));
        }

        adapter = new ProductAdapter(this, new ProductAdapter.Listener() {
            @Override
            public void onDetail(JsonObject item) {
                String productId = getString(item, "id");
                if (productId.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Không có id sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.EXTRA_PRODUCT_ID, productId);
                startActivity(intent);
            }

            @Override
            public void onAddToCart(JsonObject item) {
                String productId = getString(item, "id");
                if (productId.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Không có id sản phẩm", Toast.LENGTH_SHORT).show();
                    return;
                }
                // CartService cartService = ApiClient.createService(MainActivity.this, CartService.class);
                // cartService.addMatHang(new AddCartItemRequest(productId, 1)).enqueue(new Callback<JsonObject>() {
                //     @Override
                //     public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                //         if (!response.isSuccessful()) {
                //             Toast.makeText(MainActivity.this, "Thêm vào giỏ thất bại: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
                //             return;
                //         }
                //         Toast.makeText(MainActivity.this, "Đã thêm vào giỏ", Toast.LENGTH_SHORT).show();
                //         refreshCartBadge();
                //     }

                //     @Override
                //     public void onFailure(Call<JsonObject> call, Throwable t) {
                //         Toast.makeText(MainActivity.this, "Lỗi thêm giỏ: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                //     }
                // });
            }
        });

        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setAdapter(adapter);

        rvProducts.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
                int spacing = dpToPx(10);
                int pos = parent.getChildAdapterPosition(view);
                if (pos == RecyclerView.NO_POSITION) return;

                int column = pos % 2;
                outRect.left = column == 0 ? 0 : spacing / 2;
                outRect.right = column == 0 ? spacing / 2 : 0;
                outRect.top = pos < 2 ? 0 : spacing;
                outRect.bottom = 0;
            }
        });
        rvProducts.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) {
                    return;
                }
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (!(layoutManager instanceof GridLayoutManager)) {
                    return;
                }
                GridLayoutManager lm = (GridLayoutManager) layoutManager;
                int last = lm.findLastVisibleItemPosition();
                int total = lm.getItemCount();
                if (!isLoading && page < totalPages && last >= total - 3) {
                    loadNextPage();
                }
            }
        });

        setupDrawerActions();
        setupSearch();

        renderDefaultCategoryChip();
        loadCategories();
        refreshProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCartBadge();
        refreshOrdersBadge();
        bindUserAvatar();
    }

    private void bindUserAvatar() {
        if (imgUserAvatar == null) return;
        // SessionManager sm = new SessionManager(this);
        // String email = sm.getUserEmail();
        // String imgUrl = sm.getUserImgUrl();
        // Object model = AvatarUtil.resolveModel(this, imgUrl, email);
        // Glide.with(this)
        //         .load(model)
        //         .circleCrop()
        //         .into(imgUserAvatar);
    }

    private void setupDrawerActions() {
        ImageButton btnMenu = findViewById(R.id.btn_menu);
        btnMenu.setOnClickListener(v -> drawerLayout.openDrawer(findViewById(R.id.drawer_container)));

        ImageButton btnCart = findViewById(R.id.btn_cart);
        if (btnCart != null) {
            btnCart.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, CartActivity.class)));
        }

        View close = findViewById(R.id.btn_close_drawer);
        if (close != null) {
            close.setOnClickListener(v -> drawerLayout.closeDrawer(findViewById(R.id.drawer_container)));
        }

        View orders = findViewById(R.id.btn_menu_orders);
        tvOrdersBadge = findViewById(R.id.tv_orders_badge);
        if (orders != null) {
            orders.setOnClickListener(v -> {
                drawerLayout.closeDrawer(findViewById(R.id.drawer_container));
                startActivity(new Intent(MainActivity.this, OrdersActivity.class));
            });
        }

        View chat = findViewById(R.id.btn_menu_chat);
        if (chat != null) {
            chat.setOnClickListener(v -> {
                drawerLayout.closeDrawer(findViewById(R.id.drawer_container));
                startActivity(new Intent(MainActivity.this, ChatActivity.class));
            });
        }

        View account = findViewById(R.id.btn_menu_account);
        if (account != null) {
            account.setOnClickListener(v -> {
                drawerLayout.closeDrawer(findViewById(R.id.drawer_container));
                startActivity(new Intent(MainActivity.this, AccountActivity.class));
            });
        }

        View logout = findViewById(R.id.btn_menu_logout);
        if (logout != null) {
            logout.setOnClickListener(v -> doLogout());
        }
    }

    private void setupSearch() {
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean isSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
            if (!isSearch && event != null) {
                isSearch = event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN;
            }
            if (isSearch) {
                keyword = String.valueOf(etSearch.getText()).trim();
                refreshProducts();
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String v = String.valueOf(s).trim();

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    if (!v.equals(keyword)) {
                        keyword = v;
                        refreshProducts();
                    }
                };
                searchHandler.postDelayed(searchRunnable, 300);
            }
        });
    }

    private void refreshOrdersBadge() {
        // OrderService service = ApiClient.createService(this, OrderService.class);
        // service.danhSachDonHangCuaToi().enqueue(new Callback<JsonElement>() {
        //     @Override
        //     public void onResponse(Call<JsonElement> call, Response<JsonElement> response) {
        //         if (!response.isSuccessful() || response.body() == null) {
        //             setOrdersBadgeCount(0);
        //             return;
        //         }

        //         int count = extractOrderCount(response.body());
        //         setOrdersBadgeCount(count);
        //     }

        //     @Override
        //     public void onFailure(Call<JsonElement> call, Throwable t) {
        //         setOrdersBadgeCount(0);
        //     }
        // });
    }

    private void setOrdersBadgeCount(int count) {
        if (tvOrdersBadge == null) {
            return;
        }
        if (count <= 0) {
            tvOrdersBadge.setVisibility(View.GONE);
            return;
        }
        tvOrdersBadge.setVisibility(View.VISIBLE);
        tvOrdersBadge.setText(String.valueOf(Math.min(count, 99)));
    }

    private int extractOrderCount(JsonElement body) {
        JsonArray list = null;
        if (body == null || body.isJsonNull()) return 0;
        if (body.isJsonArray()) {
            list = body.getAsJsonArray();
        } else if (body.isJsonObject()) {
            JsonObject obj = body.getAsJsonObject();
            if (obj.has("danhSach") && obj.get("danhSach").isJsonArray()) {
                list = obj.getAsJsonArray("danhSach");
            } else if (obj.has("items") && obj.get("items").isJsonArray()) {
                list = obj.getAsJsonArray("items");
            } else if (obj.has("data")) {
                JsonElement data = obj.get("data");
                if (data != null && data.isJsonArray()) {
                    list = data.getAsJsonArray();
                } else if (data != null && data.isJsonObject()) {
                    JsonObject dataObj = data.getAsJsonObject();
                    if (dataObj.has("danhSach") && dataObj.get("danhSach").isJsonArray()) {
                        list = dataObj.getAsJsonArray("danhSach");
                    } else if (dataObj.has("items") && dataObj.get("items").isJsonArray()) {
                        list = dataObj.getAsJsonArray("items");
                    }
                }
            }
        }

        return list == null ? 0 : list.size();
    }

    private void renderDefaultCategoryChip() {
        chipGroup.removeAllViews();
        Chip chipAll = new Chip(this);
        chipAll.setText("Tất Cả");
        chipAll.setCheckable(true);
        chipAll.setChecked(true);
        styleCategoryChip(chipAll);
        chipAll.setOnClickListener(v -> {
            selectedCategoryId = null;
            refreshProducts();
        });
        chipGroup.addView(chipAll);
    }

    private void loadCategories() {
        // CatalogService service = ApiClient.createService(this, CatalogService.class);
        // service.getDanhMuc().enqueue(new Callback<JsonArray>() {
        //     @Override
        //     public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
        //         if (!response.isSuccessful()) {
        //             Toast.makeText(MainActivity.this, "Lỗi danh mục: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
        //             return;
        //         }
        //         JsonArray arr = response.body();
        //         if (arr == null) {
        //             return;
        //         }
        //         for (int i = 0; i < arr.size(); i++) {
        //             JsonElement el = arr.get(i);
        //             if (!el.isJsonObject()) {
        //                 continue;
        //             }
        //             JsonObject obj = el.getAsJsonObject();
        //             String id = getString(obj, "id");
        //             if (id.isEmpty()) {
        //                 Integer numericId = getInt(obj, "id");
        //                 if (numericId != null) {
        //                     id = String.valueOf(numericId);
        //                 }
        //             }
        //             String name = getString(obj, "ten");
        //             if (name.isEmpty()) {
        //                 name = getString(obj, "name");
        //             }
        //             if (id.isEmpty() || name.isEmpty()) {
        //                 continue;
        //             }

        //             final String finalId = id;

        //             Chip chip = new Chip(MainActivity.this);
        //             chip.setText(name);
        //             chip.setCheckable(true);
        //             styleCategoryChip(chip);
        //             chip.setOnClickListener(v -> {
        //                 selectedCategoryId = finalId;
        //                 refreshProducts();
        //             });
        //             chipGroup.addView(chip);
        //         }
        //     }

        //     @Override
        //     public void onFailure(Call<JsonArray> call, Throwable t) {
        //         Toast.makeText(MainActivity.this, "Lỗi danh mục: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        //     }
        // });
        
        // Demo data for categories with furniture icons
        String[] demoCategories = {"🪑 Bàn", "🪑 Ghế", "🛋️ Sofa", "🗄️ Tủ", "📚 Kệ", "🛏️ Giường", "🪑 Bàn làm việc"};
        int[] demoColors = {0xFF4CAF50, 0xFF2196F3, 0xFF9C27B0, 0xFFFF9800, 0xFF795548, 0xFF607D8B, 0xFF3F51B5};
        
        for (int i = 0; i < demoCategories.length; i++) {
            final String categoryId = String.valueOf(i + 1);
            final String categoryName = demoCategories[i];
            
            Chip chip = new Chip(this);
            chip.setText(categoryName);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(android.R.color.white);
            chip.setTextColor(demoColors[i]);
            chip.setChipStrokeColor(demoColors[i]);
            styleCategoryChip(chip);
            chip.setOnClickListener(v -> {
                selectedCategoryId = categoryId;
                refreshProducts();
            });
            chipGroup.addView(chip);
        }
    }

    private void refreshProducts() {
        page = 1;
        totalPages = Integer.MAX_VALUE;
        adapter.setItems(null);
        loadProductsPage(page, true);
    }

    private void loadNextPage() {
        if (page >= totalPages) {
            return;
        }
        loadProductsPage(page + 1, false);
    }

    private void loadProductsPage(int targetPage, boolean replace) {
        isLoading = true;

        // ProductService service = ApiClient.createService(this, ProductService.class);
        // service.getSanPham(selectedCategoryId, keyword.isEmpty() ? null : keyword, targetPage, limit)
        //         .enqueue(new Callback<JsonObject>() {
        //             @Override
        //             public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
        //                 isLoading = false;
        //                 if (!response.isSuccessful()) {
        //                     Toast.makeText(MainActivity.this, "Lỗi sản phẩm: HTTP " + response.code(), Toast.LENGTH_SHORT).show();
        //                     return;
        //                 }

        //                 JsonObject body = response.body();
        //                 if (body == null) {
        //                     return;
        //                 }

        //                 Integer total = getInt(body, "tongTrang");
        //                 if (total == null && body.has("data") && body.get("data").isJsonObject()) {
        //                     total = getInt(body.getAsJsonObject("data"), "tongTrang");
        //                 }
        //                 if (total != null && total > 0) {
        //                     totalPages = total;
        //                 }

        //                 JsonArray list = extractProductList(body);
        //                 java.util.List<JsonObject> parsed = new java.util.ArrayList<>();
        //                 if (list != null) {
        //                     for (int i = 0; i < list.size(); i++) {
        //                         JsonElement el = list.get(i);
        //                         if (el != null && el.isJsonObject()) {
        //                             parsed.add(el.getAsJsonObject());
        //                         }
        //                     }
        //                 }

        //                 page = targetPage;
        //                 if (replace) {
        //                     adapter.setItems(parsed);
        //                 } else {
        //                     adapter.appendItems(parsed);
        //                 }
        //             }

        //             @Override
        //             public void onFailure(Call<JsonObject> call, Throwable t) {
        //                 isLoading = false;
        //                 Toast.makeText(MainActivity.this, "Lỗi sản phẩm: " + t.getMessage(), Toast.LENGTH_SHORT).show();
        //             }
        //         });
        
        // Demo data for products with furniture icons
        isLoading = false;
        java.util.List<JsonObject> demoProducts = new java.util.ArrayList<>();
        String[] demoProductNames = {"Bàn Gỗ Sồi", "Ghế Văn Phòng", "Sofa Da Nang", "Tủ Quần Áo", "Kệ Sách", "Giường Ngủ", "Bàn Làm Việc"};
        int[] demoPrices = {1500000, 2500000, 8500000, 3200000, 1800000, 4500000, 2800000};
        String[] demoImages = {"🪑", "🪑", "🛋️", "🗄️", "📚", "🛏️", "🪑"};
        
        for (int i = 0; i < demoProductNames.length; i++) {
            JsonObject product = new JsonObject();
            product.addProperty("id", String.valueOf(i + 1));
            product.addProperty("ten", demoProductNames[i]);
            product.addProperty("gia", demoPrices[i]);
            product.addProperty("hinhDaiDien", demoImages[i]);
            product.addProperty("tonKho", (i + 1) * 5);
            demoProducts.add(product);
        }
        
        page = targetPage;
        if (replace) {
            adapter.setItems(demoProducts);
        } else {
            adapter.appendItems(demoProducts);
        }
    }

    private JsonArray extractProductList(JsonObject body) {
        if (body.has("danhSach") && body.get("danhSach").isJsonArray()) {
            return body.getAsJsonArray("danhSach");
        }
        if (body.has("items") && body.get("items").isJsonArray()) {
            return body.getAsJsonArray("items");
        }
        if (body.has("data")) {
            JsonElement data = body.get("data");
            if (data != null && data.isJsonObject()) {
                JsonObject obj = data.getAsJsonObject();
                if (obj.has("danhSach") && obj.get("danhSach").isJsonArray()) {
                    return obj.getAsJsonArray("danhSach");
                }
                if (obj.has("items") && obj.get("items").isJsonArray()) {
                    return obj.getAsJsonArray("items");
                }
            }
        }
        return null;
    }

    private void doLogout() {
        // AccountService service = ApiClient.createService(this, AccountService.class);
        // service.dangXuat().enqueue(new Callback<JsonObject>() {
        //     @Override
        //     public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
        //         new SessionManager(MainActivity.this).clear();
        //         Toast.makeText(MainActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
        //         startActivity(new Intent(MainActivity.this, AuthActivity.class));
        //         finish();
        //     }

        //     @Override
        //     public void onFailure(Call<JsonObject> call, Throwable t) {
        //         new SessionManager(MainActivity.this).clear();
        //         startActivity(new Intent(MainActivity.this, AuthActivity.class));
        //         finish();
        //     }
        // });
    }

    private void refreshCartBadge() {
        // CartService service = ApiClient.createService(this, CartService.class);
        // service.getGioHang().enqueue(new Callback<JsonObject>() {
        //     @Override
        //     public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
        //         if (!response.isSuccessful() || response.body() == null) {
        //             setCartBadgeCount(0);
        //             return;
        //         }

        //         int count = extractCartCount(response.body());
        //         setCartBadgeCount(count);
        //     }

        //     @Override
        //     public void onFailure(Call<JsonObject> call, Throwable t) {
        //         setCartBadgeCount(0);
        //     }
        // });
    }

    private void setCartBadgeCount(int count) {
        if (tvCartBadge == null) {
            return;
        }
        if (count <= 0) {
            tvCartBadge.setVisibility(View.GONE);
            return;
        }
        tvCartBadge.setVisibility(View.VISIBLE);
        tvCartBadge.setText(String.valueOf(Math.min(count, 99)));
    }

    private int extractCartCount(JsonObject body) {
        JsonArray list = null;
        if (body.has("danhSach") && body.get("danhSach").isJsonArray()) {
            list = body.getAsJsonArray("danhSach");
        } else if (body.has("items") && body.get("items").isJsonArray()) {
            list = body.getAsJsonArray("items");
        } else if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            if (data.has("danhSach") && data.get("danhSach").isJsonArray()) {
                list = data.getAsJsonArray("danhSach");
            } else if (data.has("items") && data.get("items").isJsonArray()) {
                list = data.getAsJsonArray("items");
            }
        }

        if (list == null) {
            return 0;
        }

        int total = 0;
        for (int i = 0; i < list.size(); i++) {
            JsonElement el = list.get(i);
            if (el == null || !el.isJsonObject()) continue;
            JsonObject it = el.getAsJsonObject();
            Integer qty = getInt(it, "soLuong");
            if (qty == null) {
                qty = getInt(it, "qty");
            }
            if (qty == null) {
                qty = getInt(it, "quantity");
            }
            total += qty == null ? 0 : Math.max(0, qty);
        }
        return total;
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

    private static Integer getInt(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) {
            return null;
        }
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        try {
            return el.getAsInt();
        } catch (Exception e) {
            try {
                return Integer.parseInt(el.getAsString());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private void styleCategoryChip(Chip chip) {
        if (chip == null) return;
        chip.setMinHeight(dpToPx(32));
        chip.setTextSize(13);
        int px = dpToPx(12);
        chip.setPadding(px, 0, px, 0);
    }

    private int dpToPx(int dp) {
        float d = getResources().getDisplayMetrics().density;
        return Math.round(dp * d);
    }
}
