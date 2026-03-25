package com.example.appbanghe;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.SessionManager;
import com.example.appbanghe.network.dto.ChangePasswordRequest;
import com.example.appbanghe.network.dto.UpdateProfileRequest;
import com.example.appbanghe.network.services.AccountService;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import okhttp3.ResponseBody;

public class AccountActivity extends AppCompatActivity {

    private EditText etName;
    private EditText etEmail;
    private EditText etPhone;
    private EditText etAddress;

    private ImageView imgAvatar;
    private EditText etImgUrl;

    private EditText etOldPassword;
    private EditText etNewPassword;
    private EditText etConfirmPassword;

    private TextView tvCard1;
    private TextView tvCard2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.account_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        etName = findViewById(R.id.et_account_name);
        etEmail = findViewById(R.id.et_account_email);
        etPhone = findViewById(R.id.et_account_phone);
        etAddress = findViewById(R.id.et_account_address);

        imgAvatar = findViewById(R.id.img_account_avatar);
        etImgUrl = findViewById(R.id.et_account_imgurl);

        if (imgAvatar != null) {
            imgAvatar.setOnClickListener(v -> showAvatarPicker());
        }

        etOldPassword = findViewById(R.id.et_old_password);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        Button btnUpdate = findViewById(R.id.btn_update_account);
        if (btnUpdate != null) {
            btnUpdate.setOnClickListener(v -> doUpdateProfile());
        }

        Button btnChangePass = findViewById(R.id.btn_change_password);
        if (btnChangePass != null) {
            btnChangePass.setOnClickListener(v -> doChangePassword());
        }

        tvCard1 = findViewById(R.id.tv_card_1);
        tvCard2 = findViewById(R.id.tv_card_2);

        Button btnCard1Edit = findViewById(R.id.btn_card_1_edit);
        if (btnCard1Edit != null) {
            btnCard1Edit.setOnClickListener(v -> showEditCardDialog(1));
        }
        Button btnCard1Clear = findViewById(R.id.btn_card_1_clear);
        if (btnCard1Clear != null) {
            btnCard1Clear.setOnClickListener(v -> {
                VisaCardStorage.clearCard(AccountActivity.this, 1);
                refreshCards();
            });
        }

        Button btnCard2Edit = findViewById(R.id.btn_card_2_edit);
        if (btnCard2Edit != null) {
            btnCard2Edit.setOnClickListener(v -> showEditCardDialog(2));
        }
        Button btnCard2Clear = findViewById(R.id.btn_card_2_clear);
        if (btnCard2Clear != null) {
            btnCard2Clear.setOnClickListener(v -> {
                VisaCardStorage.clearCard(AccountActivity.this, 2);
                refreshCards();
            });
        }

        loadProfile();
        refreshCards();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCards();
    }

    private void refreshCards() {
        VisaCardStorage.VisaCard c1 = VisaCardStorage.getCard(this, 1);
        VisaCardStorage.VisaCard c2 = VisaCardStorage.getCard(this, 2);

        if (tvCard1 != null) {
            if (c1 == null) {
                tvCard1.setText("Chưa có thẻ");
            } else {
                String name = c1.tenChuThe == null ? "" : c1.tenChuThe.trim();
                tvCard1.setText(c1.masked() + (name.isEmpty() ? "" : (" - " + name)));
            }
        }
        if (tvCard2 != null) {
            if (c2 == null) {
                tvCard2.setText("Chưa có thẻ");
            } else {
                String name = c2.tenChuThe == null ? "" : c2.tenChuThe.trim();
                tvCard2.setText(c2.masked() + (name.isEmpty() ? "" : (" - " + name)));
            }
        }
    }

    private void showEditCardDialog(int slot) {
        VisaCardStorage.VisaCard existing = VisaCardStorage.getCard(this, slot);

        final EditText etSoThe = new EditText(this);
        etSoThe.setHint("Số thẻ VISA");
        etSoThe.setInputType(InputType.TYPE_CLASS_NUMBER);

        final EditText etTen = new EditText(this);
        etTen.setHint("Tên chủ thẻ");
        etTen.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        final EditText etMM = new EditText(this);
        etMM.setHint("Tháng hết hạn (MM)");
        etMM.setInputType(InputType.TYPE_CLASS_NUMBER);

        final EditText etYY = new EditText(this);
        etYY.setHint("Năm hết hạn (YYYY)");
        etYY.setInputType(InputType.TYPE_CLASS_NUMBER);

        final EditText etCvv = new EditText(this);
        etCvv.setHint("CVV");
        etCvv.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        if (existing != null) {
            if (existing.soThe != null) etSoThe.setText(existing.soThe);
            if (existing.tenChuThe != null) etTen.setText(existing.tenChuThe);
            if (existing.thangHetHan > 0) etMM.setText(String.valueOf(existing.thangHetHan));
            if (existing.namHetHan > 0) etYY.setText(String.valueOf(existing.namHetHan));
            if (existing.cvv != null) etCvv.setText(existing.cvv);
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        container.setPadding(pad, pad, pad, 0);
        container.addView(etSoThe);
        container.addView(etTen);
        container.addView(etMM);
        container.addView(etYY);
        container.addView(etCvv);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(container);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(slot == 2 ? "Thẻ 2" : "Thẻ 1")
                .setView(scrollView)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .setPositiveButton("Lưu", null)
                .create();

        dialog.setOnShowListener(d -> {
            Button btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (btnPositive != null) {
                btnPositive.setOnClickListener(v -> {
                    String soThe = etSoThe.getText() == null ? "" : etSoThe.getText().toString().trim();
                    String ten = etTen.getText() == null ? "" : etTen.getText().toString().trim();
                    String mmRaw = etMM.getText() == null ? "" : etMM.getText().toString().trim();
                    String yyRaw = etYY.getText() == null ? "" : etYY.getText().toString().trim();
                    String cvv = etCvv.getText() == null ? "" : etCvv.getText().toString().trim();

                    if (soThe.isEmpty() || soThe.length() < 12) {
                        Toast.makeText(AccountActivity.this, "Vui lòng nhập số thẻ hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (ten.isEmpty()) {
                        Toast.makeText(AccountActivity.this, "Vui lòng nhập tên chủ thẻ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int mm;
                    int yy;
                    try {
                        mm = Integer.parseInt(mmRaw);
                        yy = Integer.parseInt(yyRaw);
                    } catch (Exception e) {
                        Toast.makeText(AccountActivity.this, "Vui lòng nhập tháng/năm hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (mm < 1 || mm > 12) {
                        Toast.makeText(AccountActivity.this, "Tháng hết hạn không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
                    if (yy < currentYear) {
                        Toast.makeText(AccountActivity.this, "Năm hết hạn không hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (cvv.isEmpty() || cvv.length() < 3) {
                        Toast.makeText(AccountActivity.this, "Vui lòng nhập CVV hợp lệ", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    VisaCardStorage.saveCard(AccountActivity.this, slot, new VisaCardStorage.VisaCard(soThe, ten, mm, yy, cvv));
                    refreshCards();
                    dialog.dismiss();
                });
            }
        });

        dialog.show();
    }

    private void loadProfile() {
        AccountService service = ApiClient.createService(this, AccountService.class);
        service.toi().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(AccountActivity.this, "Không tải được thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                JsonObject data = extractUser(response.body());
                if (data == null) {
                    Toast.makeText(AccountActivity.this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
                    return;
                }

                String name = getString(data, "hoTen", "ho_ten", "ten", "name");
                String email = getString(data, "email");
                String phone = getString(data, "soDienThoai", "so_dien_thoai", "phone");
                String address = getString(data, "diaChi", "dia_chi", "diachi", "address");
                String imgUrl = getString(data, "imgUrl", "img_url", "avatar", "avatarUrl");

                if (etName != null) etName.setText(name);
                if (etEmail != null) etEmail.setText(email);
                if (etPhone != null) etPhone.setText(phone);
                if (etAddress != null) etAddress.setText(address);
                if (etImgUrl != null) etImgUrl.setText(imgUrl);

                SessionManager sm = new SessionManager(AccountActivity.this);
                if (email != null && !email.trim().isEmpty()) {
                    sm.setUserEmail(email);
                }
                sm.setUserImgUrl(imgUrl);

                bindAvatar(imgUrl, email);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AccountActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindAvatar(String imgUrl, String email) {
        if (imgAvatar == null) return;
        Object model = AvatarUtil.resolveModel(this, imgUrl, email);
        Glide.with(this)
                .load(model)
                .circleCrop()
                .into(imgAvatar);
    }

    private void showAvatarPicker() {
        View v = getLayoutInflater().inflate(R.layout.dialog_avatar_picker, null);
        RecyclerView rv = v.findViewById(R.id.rv_avatar_picker);

        final AlertDialog[] dialogRef = new AlertDialog[1];

        AvatarPickerAdapter adapter = new AvatarPickerAdapter(this, avatarName -> {
            if (dialogRef[0] != null) {
                dialogRef[0].dismiss();
            }
            if (etImgUrl != null) {
                etImgUrl.setText(avatarName);
            }
            String email = etEmail == null ? "" : String.valueOf(etEmail.getText()).trim();
            bindAvatar(avatarName, email);
            doUpdateProfile();
        });

        if (rv != null) {
            rv.setLayoutManager(new GridLayoutManager(this, 3));
            rv.setAdapter(adapter);
        }

        adapter.setItems(AvatarUtil.listAvatarNames(this));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Chọn ảnh đại diện")
                .setView(v)
                .setNegativeButton("Đóng", (d, w) -> d.dismiss())
                .create();

        dialogRef[0] = dialog;
        dialog.show();
    }

    private void doUpdateProfile() {
        String hoTen = etName == null ? "" : String.valueOf(etName.getText()).trim();
        String soDienThoai = etPhone == null ? "" : String.valueOf(etPhone.getText()).trim();
        String diaChi = etAddress == null ? "" : String.valueOf(etAddress.getText()).trim();
        String imgUrl = etImgUrl == null ? "" : String.valueOf(etImgUrl.getText()).trim();

        UpdateProfileRequest body = new UpdateProfileRequest(
                hoTen.isEmpty() ? null : hoTen,
                soDienThoai.isEmpty() ? null : soDienThoai,
                diaChi.isEmpty() ? null : diaChi,
                imgUrl.isEmpty() ? null : imgUrl
        );

        AccountService service = ApiClient.createService(this, AccountService.class);
        service.capNhatToi(body).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String msg = extractMessage(response.body());
                    if (msg.isEmpty()) {
                        msg = extractErrorMessage(response);
                    }
                    Toast.makeText(AccountActivity.this, msg.isEmpty() ? ("Cập nhật thất bại: HTTP " + response.code()) : msg, Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(AccountActivity.this, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show();

                JsonObject updated = extractUser(response.body());
                if (updated != null) {
                    String name2 = getString(updated, "hoTen", "ho_ten", "ten", "name");
                    String phone2 = getString(updated, "soDienThoai", "so_dien_thoai", "phone");
                    String address2 = getString(updated, "diaChi", "dia_chi", "diachi", "address");
                    String img2 = getString(updated, "imgUrl", "img_url");
                    if (etName != null && !name2.isEmpty()) etName.setText(name2);
                    if (etPhone != null && !phone2.isEmpty()) etPhone.setText(phone2);
                    if (etAddress != null) etAddress.setText(address2);
                    if (etImgUrl != null) etImgUrl.setText(img2);

                    SessionManager sm = new SessionManager(AccountActivity.this);
                    String email2 = etEmail == null ? "" : String.valueOf(etEmail.getText()).trim();
                    if (!email2.isEmpty()) sm.setUserEmail(email2);
                    sm.setUserImgUrl(img2);
                    bindAvatar(img2, email2);
                } else {
                    if (etName != null) etName.setText(hoTen);
                    if (etPhone != null) etPhone.setText(soDienThoai);
                    if (etAddress != null) etAddress.setText(diaChi);
                    if (etImgUrl != null) etImgUrl.setText(imgUrl);

                    SessionManager sm = new SessionManager(AccountActivity.this);
                    String email2 = etEmail == null ? "" : String.valueOf(etEmail.getText()).trim();
                    if (!email2.isEmpty()) sm.setUserEmail(email2);
                    sm.setUserImgUrl(imgUrl);
                    bindAvatar(imgUrl, email2);
                }

                loadProfile();
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AccountActivity.this, "Lỗi cập nhật: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void doChangePassword() {
        String matKhauCu = etOldPassword == null ? "" : String.valueOf(etOldPassword.getText());
        String matKhauMoi = etNewPassword == null ? "" : String.valueOf(etNewPassword.getText());
        String xacNhan = etConfirmPassword == null ? "" : String.valueOf(etConfirmPassword.getText());

        matKhauCu = matKhauCu.trim();
        matKhauMoi = matKhauMoi.trim();
        xacNhan = xacNhan.trim();

        if (matKhauCu.isEmpty() || matKhauMoi.isEmpty() || xacNhan.isEmpty()) {
            Toast.makeText(this, "matKhauCu, matKhauMoi, xacNhanMatKhau là bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (matKhauMoi.length() < 6) {
            Toast.makeText(this, "Mật khẩu mới phải ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!matKhauMoi.equals(xacNhan)) {
            Toast.makeText(this, "Xác nhận mật khẩu không khớp", Toast.LENGTH_SHORT).show();
            return;
        }

        AccountService service = ApiClient.createService(this, AccountService.class);
        service.doiMatKhau(new ChangePasswordRequest(matKhauCu, matKhauMoi, xacNhan)).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    String msg = extractMessage(response.body());
                    if (msg.isEmpty()) {
                        msg = extractErrorMessage(response);
                    }
                    if (msg.isEmpty()) {
                        msg = "Đổi mật khẩu thất bại: HTTP " + response.code();
                    }
                    Toast.makeText(AccountActivity.this, msg, Toast.LENGTH_SHORT).show();
                    return;
                }

                String msg = extractMessage(response.body());
                Toast.makeText(AccountActivity.this, msg.isEmpty() ? "Đổi mật khẩu thành công" : msg, Toast.LENGTH_SHORT).show();

                if (etOldPassword != null) etOldPassword.setText(null);
                if (etNewPassword != null) etNewPassword.setText(null);
                if (etConfirmPassword != null) etConfirmPassword.setText(null);
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(AccountActivity.this, "Lỗi đổi mật khẩu: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private JsonObject extractUser(JsonObject body) {
        if (body == null) return null;
        if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            if (data.has("user") && data.get("user").isJsonObject()) {
                return data.getAsJsonObject("user");
            }
            if (data.has("taiKhoan") && data.get("taiKhoan").isJsonObject()) {
                return data.getAsJsonObject("taiKhoan");
            }
            if (data.has("tai_khoan") && data.get("tai_khoan").isJsonObject()) {
                return data.getAsJsonObject("tai_khoan");
            }
            return data;
        }
        return body;
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

    private String extractMessage(JsonObject body) {
        if (body == null) {
            return "";
        }
        if (body.has("message") && !body.get("message").isJsonNull()) {
            try {
                return body.get("message").getAsString();
            } catch (Exception ignored) {
            }
        }
        if (body.has("data") && body.get("data").isJsonObject()) {
            JsonObject data = body.getAsJsonObject("data");
            if (data.has("message") && !data.get("message").isJsonNull()) {
                try {
                    return data.get("message").getAsString();
                } catch (Exception ignored) {
                }
            }
        }
        return "";
    }

    private String extractErrorMessage(Response<?> response) {
        if (response == null) return "";
        try {
            ResponseBody err = response.errorBody();
            if (err == null) return "";
            String raw = err.string();
            if (raw == null || raw.trim().isEmpty()) return "";

            JsonElement el = new Gson().fromJson(raw, JsonElement.class);
            if (el != null && el.isJsonObject()) {
                JsonObject obj = el.getAsJsonObject();
                String msg = extractMessage(obj);
                if (!msg.isEmpty()) return msg;
            }
            return raw;
        } catch (Exception ignored) {
            return "";
        }
    }
}
