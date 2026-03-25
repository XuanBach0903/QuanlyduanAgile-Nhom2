package com.example.appbanghe;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PolicyActivity extends AppCompatActivity {

    public static final String EXTRA_TYPE = "extra_type";
    public static final String TYPE_POLICY = "policy";
    public static final String TYPE_TERMS = "terms";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_policy);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.policy_root), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvContent = findViewById(R.id.tv_content);

        Intent it = getIntent();
        String type = it == null ? TYPE_POLICY : it.getStringExtra(EXTRA_TYPE);
        if (type == null || type.trim().isEmpty()) {
            type = TYPE_POLICY;
        }

        if (TYPE_TERMS.equals(type)) {
            if (tvTitle != null) tvTitle.setText("Điều khoản");
            if (tvContent != null) tvContent.setText(buildTermsContent());
            return;
        }

        if (tvTitle != null) tvTitle.setText("Chính sách");
        if (tvContent != null) tvContent.setText(buildPolicyContent());
    }

    private String buildPolicyContent() {
        return "CHÍNH SÁCH BÁN HÀNG\n\n"
                + "1. Chính sách đặt hàng\n"
                + "- Khách hàng đặt hàng trực tiếp trên ứng dụng.\n"
                + "- Sau khi đặt hàng thành công, shop sẽ xác nhận và xử lý đơn trong giờ làm việc.\n\n"
                + "2. Chính sách giao hàng\n"
                + "- Giao hàng nội thành Hà Nội và hỗ trợ giao tỉnh (tùy khu vực).\n"
                + "- Thời gian giao dự kiến: 1-3 ngày (nội thành), 3-7 ngày (tỉnh).\n"
                + "- Phí vận chuyển được thông báo trước khi xác nhận đơn.\n\n"
                + "3. Chính sách đổi trả\n"
                + "- Hỗ trợ đổi/trả trong 3 ngày kể từ khi nhận hàng nếu sản phẩm lỗi do nhà sản xuất.\n"
                + "- Sản phẩm cần còn nguyên vẹn, không trầy xước do sử dụng sai cách.\n\n"
                + "4. Chính sách bảo hành\n"
                + "- Bảo hành theo từng dòng sản phẩm (thông tin cụ thể hiển thị ở mô tả).\n"
                + "- Không bảo hành lỗi do tác động ngoại lực hoặc sử dụng không đúng hướng dẫn.\n\n"
                + "5. Hỗ trợ & tư vấn\n"
                + "- Vui lòng nhắn tin trong mục Tư vấn để được hỗ trợ nhanh nhất.";
    }

    private String buildTermsContent() {
        return "ĐIỀU KHOẢN & ĐIỀU KIỆN\n\n"
                + "1. Quy định chung\n"
                + "- Khi sử dụng ứng dụng, bạn đồng ý với các điều khoản này.\n"
                + "- Shop có quyền thay đổi nội dung điều khoản mà không cần thông báo trước.\n\n"
                + "2. Giá cả & thông tin sản phẩm\n"
                + "- Giá sản phẩm có thể thay đổi theo chương trình khuyến mãi.\n"
                + "- Hình ảnh sản phẩm mang tính minh họa, màu sắc có thể chênh lệch tùy thiết bị.\n\n"
                + "3. Thanh toán\n"
                + "- Hỗ trợ thanh toán COD và các hình thức khác theo từng thời điểm.\n"
                + "- Đơn hàng chỉ được xử lý khi thông tin giao hàng hợp lệ.\n\n"
                + "4. Quyền riêng tư\n"
                + "- Thông tin khách hàng được sử dụng để xử lý đơn và hỗ trợ chăm sóc khách hàng.\n"
                + "- Shop cam kết không chia sẻ thông tin cho bên thứ ba khi chưa có sự đồng ý.\n\n"
                + "5. Giới hạn trách nhiệm\n"
                + "- Shop không chịu trách nhiệm với các thiệt hại gián tiếp phát sinh từ việc sử dụng ứng dụng.";
    }
}
