package com.example.appbanghe;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.services.InvoiceService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InvoiceDetailActivity extends AppCompatActivity {

    private String invoiceId;
    private JsonObject invoiceInfo;

    private TextView tvInvoiceNumber;
    private TextView tvInvoiceDate;
    private TextView tvCustomerName;
    private TextView tvCustomerEmail;
    private TextView tvCustomerPhone;
    private TextView tvCustomerAddress;
    private TextView tvPaymentMethod;
    private TextView tvPaymentStatus;
    private TextView tvInvoiceStatus;
    private TextView tvSubtotal;
    private TextView tvDiscount;
    private TextView tvVAT;
    private TextView tvShipping;
    private TextView tvTotal;
    private RecyclerView rvItems;
    private LinearLayout llItemsContainer;
    private Button btnDownloadPDF;
    private Button btnPay;
    private Button btnCancel;
    private ImageView btnBack;

    private InvoiceItemAdapter itemAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_detail);

        // Get invoice ID from intent
        Intent intent = getIntent();
        invoiceId = intent.getStringExtra("invoice_id");

        if (invoiceId == null) {
            Toast.makeText(this, "Không có thông tin hóa đơn", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupClickListeners();
        loadInvoiceDetail();
    }

    private void initViews() {
        tvInvoiceNumber = findViewById(R.id.tv_invoice_number);
        tvInvoiceDate = findViewById(R.id.tv_invoice_date);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerEmail = findViewById(R.id.tv_customer_email);
        tvCustomerPhone = findViewById(R.id.tv_customer_phone);
        tvCustomerAddress = findViewById(R.id.tv_customer_address);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvPaymentStatus = findViewById(R.id.tv_payment_status);
        tvInvoiceStatus = findViewById(R.id.tv_invoice_status);
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDiscount = findViewById(R.id.tv_discount);
        tvVAT = findViewById(R.id.tv_vat);
        tvShipping = findViewById(R.id.tv_shipping);
        tvTotal = findViewById(R.id.tv_total);
        rvItems = findViewById(R.id.rv_items);
        llItemsContainer = findViewById(R.id.ll_items_container);
        btnDownloadPDF = findViewById(R.id.btn_download_pdf);
        btnPay = findViewById(R.id.btn_pay);
        btnCancel = findViewById(R.id.btn_cancel);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        itemAdapter = new InvoiceItemAdapter(this, new ArrayList<>());
        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(itemAdapter);
    }

    private void setupClickListeners() {
        btnDownloadPDF.setOnClickListener(v -> {
            generateInvoicePDF();
        });

        btnPay.setOnClickListener(v -> {
            payInvoice();
        });

        btnCancel.setOnClickListener(v -> {
            showCancelDialog();
        });
    }

    private void loadInvoiceDetail() {
        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.layChiTietHoaDon(invoiceId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    showError("Không thể tải thông tin hóa đơn");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null && responseBody.has("data")) {
                        invoiceInfo = responseBody.getAsJsonObject("data");
                        updateUI();
                    }
                } catch (Exception e) {
                    showError("Lỗi xử lý dữ liệu: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                showError("Lỗi mạng: " + t.getMessage());
            }
        });
    }

    private void updateUI() {
        if (invoiceInfo == null) return;

        // Basic info
        tvInvoiceNumber.setText("Hóa đơn #" + invoiceInfo.get("ma_hoa_don").getAsString());
        
        if (invoiceInfo.has("ngay_tao")) {
            String invoiceDate = invoiceInfo.get("ngay_tao").getAsString();
            tvInvoiceDate.setText(formatDateTime(invoiceDate));
        }

        // Customer info
        if (invoiceInfo.has("khach_hang")) {
            JsonObject customer = invoiceInfo.getAsJsonObject("khach_hang");
            tvCustomerName.setText(customer.get("ho_ten").getAsString());
            tvCustomerEmail.setText(customer.get("email").getAsString());
            tvCustomerPhone.setText(customer.get("so_dien_thoai").getAsString());
            tvCustomerAddress.setText(customer.get("dia_chi").getAsString());
        }

        // Payment info
        if (invoiceInfo.has("thanh_toan")) {
            JsonObject payment = invoiceInfo.getAsJsonObject("thanh_toan");
            
            String method = payment.get("phuong_thuc").getAsString();
            tvPaymentMethod.setText(getPaymentMethodText(method));
            
            String status = payment.get("trang_thai").getAsString();
            tvPaymentStatus.setText(getPaymentStatusText(status));
            tvPaymentStatus.setTextColor(getPaymentStatusColor(status));
        }

        // Invoice status
        if (invoiceInfo.has("trang_thai_hoa_don")) {
            String status = invoiceInfo.get("trang_thai_hoa_don").getAsString();
            tvInvoiceStatus.setText(getInvoiceStatusText(status));
            tvInvoiceStatus.setTextColor(getInvoiceStatusColor(status));
        }

        // Totals
        if (invoiceInfo.has("tong_tien_hang")) {
            double subtotal = invoiceInfo.get("tong_tien_hang").getAsDouble();
            tvSubtotal.setText(formatCurrency(subtotal));
        }

        if (invoiceInfo.has("tong_giam_gia")) {
            double discount = invoiceInfo.get("tong_giam_gia").getAsDouble();
            tvDiscount.setText(formatCurrency(discount));
        }

        if (invoiceInfo.has("tong_thue_vat")) {
            double vat = invoiceInfo.get("tong_thue_vat").getAsDouble();
            tvVAT.setText(formatCurrency(vat));
        }

        if (invoiceInfo.has("thong_tin_bo_sung")) {
            JsonObject additional = invoiceInfo.getAsJsonObject("thong_tin_bo_sung");
            if (additional.has("phi_giao_hang")) {
                double shipping = additional.get("phi_giao_hang").getAsDouble();
                tvShipping.setText(formatCurrency(shipping));
            }
        }

        if (invoiceInfo.has("tong_cong")) {
            double total = invoiceInfo.get("tong_cong").getAsDouble();
            tvTotal.setText(formatCurrency(total));
        }

        // Items
        if (invoiceInfo.has("chi_tiet_hoa_don")) {
            JsonArray items = invoiceInfo.getAsJsonArray("chi_tiet_hoa_don");
            displayItems(items);
        }

        // Update button visibility based on status
        updateButtonVisibility();
    }

    private void displayItems(JsonArray items) {
        // Clear existing items
        llItemsContainer.removeAllViews();
        itemAdapter.clearItems();

        for (int i = 0; i < items.size(); i++) {
            JsonObject item = items.get(i).getAsJsonObject();
            itemAdapter.addItem(item);

            // Also create view for display
            View itemView = createItemView(item);
            llItemsContainer.addView(itemView);
        }
    }

    private View createItemView(JsonObject item) {
        View view = getLayoutInflater().inflate(R.layout.item_invoice_detail, llItemsContainer, false);

        TextView tvProductName = view.findViewById(R.id.tv_product_name);
        TextView tvQuantity = view.findViewById(R.id.tv_quantity);
        TextView tvUnitPrice = view.findViewById(R.id.tv_unit_price);
        TextView tvTotal = view.findViewById(R.id.tv_total);

        tvProductName.setText(item.get("ten_san_pham").getAsString());
        tvQuantity.setText("x" + item.get("so_luong").getAsString());
        
        double unitPrice = item.get("don_gia").getAsDouble();
        double itemTotal = item.get("thanh_tien").getAsDouble();
        
        tvUnitPrice.setText(formatCurrency(unitPrice));
        tvTotal.setText(formatCurrency(itemTotal));

        return view;
    }

    private void updateButtonVisibility() {
        if (invoiceInfo == null) return;

        String paymentStatus = "";
        String invoiceStatus = "";

        if (invoiceInfo.has("thanh_toan")) {
            paymentStatus = invoiceInfo.getAsJsonObject("thanh_toan").get("trang_thai").getAsString();
        }

        if (invoiceInfo.has("trang_thai_hoa_don")) {
            invoiceStatus = invoiceInfo.get("trang_thai_hoa_don").getAsString();
        }

        // Show/hide pay button
        btnPay.setVisibility("CHUA_THANH_TOAN".equals(paymentStatus) ? View.VISIBLE : View.GONE);

        // Show/hide cancel button
        boolean canCancel = !"DA_XUAT".equals(invoiceStatus) && !"DA_THANH_TOAN".equals(paymentStatus);
        btnCancel.setVisibility(canCancel ? View.VISIBLE : View.GONE);
    }

    private void generateInvoicePDF() {
        try {
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(24);
            paint.setTypeface(Typeface.DEFAULT_BOLD);

            int yPosition = 50;
            int lineHeight = 30;
            int margin = 50;

            // Title
            canvas.drawText("HÓA ĐƠN", margin, yPosition, paint);
            yPosition += lineHeight * 2;

            paint.setTextSize(16);
            paint.setTypeface(Typeface.DEFAULT);

            // Invoice number
            if (invoiceInfo != null) {
                canvas.drawText("Mã hóa đơn: " + invoiceInfo.get("ma_hoa_don").getAsString(), margin, yPosition, paint);
                yPosition += lineHeight;

                // Date
                if (invoiceInfo.has("ngay_tao")) {
                    canvas.drawText("Ngày: " + formatDateTime(invoiceInfo.get("ngay_tao").getAsString()), margin, yPosition, paint);
                    yPosition += lineHeight;
                }

                // Customer info
                yPosition += lineHeight;
                canvas.drawText("THÔNG TIN KHÁCH HÀNG", margin, yPosition, paint);
                yPosition += lineHeight;

                if (invoiceInfo.has("khach_hang")) {
                    JsonObject customer = invoiceInfo.getAsJsonObject("khach_hang");
                    canvas.drawText("Họ tên: " + customer.get("ho_ten").getAsString(), margin, yPosition, paint);
                    yPosition += lineHeight;
                    canvas.drawText("Email: " + customer.get("email").getAsString(), margin, yPosition, paint);
                    yPosition += lineHeight;
                    canvas.drawText("Điện thoại: " + customer.get("so_dien_thoai").getAsString(), margin, yPosition, paint);
                    yPosition += lineHeight;
                    canvas.drawText("Địa chỉ: " + customer.get("dia_chi").getAsString(), margin, yPosition, paint);
                    yPosition += lineHeight;
                }

                // Items
                yPosition += lineHeight;
                canvas.drawText("CHI TIẾT HÓA ĐƠN", margin, yPosition, paint);
                yPosition += lineHeight;

                if (invoiceInfo.has("chi_tiet_hoa_don")) {
                    JsonArray items = invoiceInfo.getAsJsonArray("chi_tiet_hoa_don");
                    for (int i = 0; i < items.size(); i++) {
                        JsonObject item = items.get(i).getAsJsonObject();
                        canvas.drawText(item.get("ten_san_pham").getAsString() + " x" + item.get("so_luong").getAsString(), margin, yPosition, paint);
                        yPosition += lineHeight;
                        canvas.drawText(formatCurrency(item.get("thanh_tien").getAsDouble()), margin + 300, yPosition, paint);
                        yPosition += lineHeight;
                    }
                }

                // Totals
                yPosition += lineHeight;
                canvas.drawText("TỔNG CỘNG: " + formatCurrency(invoiceInfo.get("tong_cong").getAsDouble()), margin, yPosition, paint);
            }

            document.finishPage(page);
            document.close();

            // Save PDF
            File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Invoice_" + invoiceId + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.close();

            Toast.makeText(this, "Đã lưu hóa đơn PDF: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Lỗi tạo PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void payInvoice() {
        Intent intent = new Intent(this, PaymentActivity.class);
        intent.putExtra("invoice_id", invoiceId);
        startActivityForResult(intent, 1001);
    }

    private void showCancelDialog() {
        android.widget.EditText etReason = new android.widget.EditText(this);
        etReason.setHint("Nhập lý do hủy hóa đơn");

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hủy hóa đơn")
            .setView(etReason)
            .setPositiveButton("Hủy", (dialog, which) -> {
                String reason = etReason.getText().toString().trim();
                cancelInvoice(reason);
            })
            .setNegativeButton("Đóng", null)
            .show();
    }

    private void cancelInvoice(String reason) {
        JsonObject request = new JsonObject();
        request.addProperty("ly_do", reason);

        InvoiceService service = ApiClient.createService(this, InvoiceService.class);
        service.huyHoaDon(invoiceId, request).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(InvoiceDetailActivity.this, "Hủy hóa đơn thất bại", Toast.LENGTH_SHORT).show();
                    return;
                }

                Toast.makeText(InvoiceDetailActivity.this, "Đã hủy hóa đơn", Toast.LENGTH_SHORT).show();
                loadInvoiceDetail(); // Reload data
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(InvoiceDetailActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            // Payment completed, reload data
            loadInvoiceDetail();
        }
    }

    // Helper methods
    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    private String formatDateTime(String dateString) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            
            Date date = inputFormat.parse(dateString);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateString;
        }
    }

    private String getPaymentMethodText(String method) {
        switch (method) {
            case "COD": return "Thanh toán khi nhận hàng";
            case "VNPAY": return "Ví điện tử VNPAY";
            case "VISA": return "Thẻ Visa/Mastercard";
            case "PAYPAL": return "PayPal";
            default: return method;
        }
    }

    private String getPaymentStatusText(String status) {
        switch (status) {
            case "DA_THANH_TOAN": return "Đã thanh toán";
            case "CHUA_THANH_TOAN": return "Chưa thanh toán";
            case "DANG_XU_LY": return "Đang xử lý";
            case "THAT_BAI": return "Thất bại";
            default: return status;
        }
    }

    private int getPaymentStatusColor(String status) {
        switch (status) {
            case "DA_THANH_TOAN": return getResources().getColor(android.R.color.holo_green_dark);
            case "CHUA_THANH_TOAN": 
            case "DANG_XU_LY": return getResources().getColor(android.R.color.holo_orange_dark);
            case "THAT_BAI": return getResources().getColor(android.R.color.holo_red_dark);
            default: return getResources().getColor(android.R.color.primary_text_light);
        }
    }

    private String getInvoiceStatusText(String status) {
        switch (status) {
            case "MOI_TAO": return "Mới tạo";
            case "DA_XUAT": return "Đã xuất";
            case "DA_HUY": return "Đã hủy";
            case "DA_THANH_TOAN": return "Đã thanh toán";
            default: return status;
        }
    }

    private int getInvoiceStatusColor(String status) {
        switch (status) {
            case "MOI_TAO": return getResources().getColor(android.R.color.holo_blue_dark);
            case "DA_XUAT": return getResources().getColor(android.R.color.holo_green_dark);
            case "DA_HUY": return getResources().getColor(android.R.color.holo_red_dark);
            case "DA_THANH_TOAN": return getResources().getColor(android.R.color.holo_green_dark);
            default: return getResources().getColor(android.R.color.primary_text_light);
        }
    }
}
