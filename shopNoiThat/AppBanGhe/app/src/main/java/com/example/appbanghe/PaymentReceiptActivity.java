package com.example.appbanghe;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.appbanghe.network.ApiClient;
import com.example.appbanghe.network.services.PaymentService;
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

public class PaymentReceiptActivity extends AppCompatActivity {

    private String orderId;
    private JsonObject paymentInfo;

    private TextView tvOrderId;
    private TextView tvPaymentDate;
    private TextView tvPaymentMethod;
    private TextView tvAmount;
    private TextView tvStatus;
    private TextView tvCustomerName;
    private TextView tvCustomerEmail;
    private TextView tvTransactionId;
    private ImageView ivQRCode;
    private Button btnDownloadPDF;
    private Button btnShare;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_receipt);

        // Get order ID from intent
        Intent intent = getIntent();
        orderId = intent.getStringExtra("order_id");

        if (orderId == null) {
            Toast.makeText(this, "Không có thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadPaymentInfo();
        setupClickListeners();
    }

    private void initViews() {
        tvOrderId = findViewById(R.id.tv_order_id);
        tvPaymentDate = findViewById(R.id.tv_payment_date);
        tvPaymentMethod = findViewById(R.id.tv_payment_method);
        tvAmount = findViewById(R.id.tv_amount);
        tvStatus = findViewById(R.id.tv_status);
        tvCustomerName = findViewById(R.id.tv_customer_name);
        tvCustomerEmail = findViewById(R.id.tv_customer_email);
        tvTransactionId = findViewById(R.id.tv_transaction_id);
        ivQRCode = findViewById(R.id.iv_qr_code);
        btnDownloadPDF = findViewById(R.id.btn_download_pdf);
        btnShare = findViewById(R.id.btn_share);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupClickListeners() {
        btnDownloadPDF.setOnClickListener(v -> {
            generatePDFReceipt();
        });

        btnShare.setOnClickListener(v -> {
            shareReceipt();
        });
    }

    private void loadPaymentInfo() {
        PaymentService service = ApiClient.createService(this, PaymentService.class);
        service.getPaymentStatus(orderId).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (!response.isSuccessful()) {
                    showError("Không thể tải thông tin thanh toán");
                    return;
                }

                try {
                    JsonObject responseBody = response.body();
                    if (responseBody != null) {
                        paymentInfo = responseBody;
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
        if (paymentInfo == null) return;

        tvOrderId.setText("#" + orderId);
        
        if (paymentInfo.has("paymentMethod")) {
            String method = paymentInfo.get("paymentMethod").getAsString();
            tvPaymentMethod.setText(getPaymentMethodText(method));
        }

        if (paymentInfo.has("paymentStatus")) {
            String status = paymentInfo.get("paymentStatus").getAsString();
            tvStatus.setText(getStatusText(status));
            tvStatus.setTextColor(getStatusColor(status));
        }

        if (paymentInfo.has("amount")) {
            double amount = paymentInfo.get("amount").getAsDouble();
            tvAmount.setText(formatCurrency(amount));
        }

        if (paymentInfo.has("orderDate")) {
            String orderDate = paymentInfo.get("orderDate").getAsString();
            tvPaymentDate.setText(formatDateTime(orderDate));
        }

        if (paymentInfo.has("transactionId")) {
            String transactionId = paymentInfo.get("transactionId").getAsString();
            tvTransactionId.setText(transactionId);
        } else {
            tvTransactionId.setText("Chưa có");
        }

        // Load customer info (in real app, get from user session)
        tvCustomerName.setText("Khách hàng");
        tvCustomerEmail.setText("customer@example.com");

        // Generate QR code
        generateQRCode();
    }

    private void generateQRCode() {
        // In a real app, use a QR code library like ZXing
        // For now, show a placeholder
        try {
            // Create a simple QR code placeholder
            Bitmap qrBitmap = createQRCodePlaceholder();
            ivQRCode.setImageBitmap(qrBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap createQRCodePlaceholder() {
        int size = 200;
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        
        // Create a simple pattern as QR placeholder
        int cellSize = size / 10;
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                if ((i + j) % 2 == 0) {
                    canvas.drawRect(i * cellSize, j * cellSize, (i + 1) * cellSize, (j + 1) * cellSize, paint);
                }
            }
        }
        
        return bitmap;
    }

    private void generatePDFReceipt() {
        try {
            // Create PDF document
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(300, 600, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(24);
            paint.setTypeface(Typeface.DEFAULT_BOLD);

            // Title
            canvas.drawText("BIÊN LAI THANH TOÁN", 20, 40, paint);

            paint.setTextSize(16);
            paint.setTypeface(Typeface.DEFAULT);

            int yPosition = 80;
            int lineHeight = 30;

            // Order info
            canvas.drawText("Mã đơn hàng: #" + orderId, 20, yPosition, paint);
            yPosition += lineHeight;

            if (paymentInfo != null) {
                if (paymentInfo.has("paymentDate")) {
                    canvas.drawText("Ngày thanh toán: " + formatDateTime(paymentInfo.get("paymentDate").getAsString()), 20, yPosition, paint);
                    yPosition += lineHeight;
                }

                if (paymentInfo.has("paymentMethod")) {
                    canvas.drawText("Phương thức: " + getPaymentMethodText(paymentInfo.get("paymentMethod").getAsString()), 20, yPosition, paint);
                    yPosition += lineHeight;
                }

                if (paymentInfo.has("amount")) {
                    canvas.drawText("Số tiền: " + formatCurrency(paymentInfo.get("amount").getAsDouble()), 20, yPosition, paint);
                    yPosition += lineHeight;
                }

                if (paymentInfo.has("transactionId")) {
                    canvas.drawText("Mã giao dịch: " + paymentInfo.get("transactionId").getAsString(), 20, yPosition, paint);
                    yPosition += lineHeight;
                }
            }

            // Customer info
            yPosition += lineHeight;
            canvas.drawText("Thông tin khách hàng:", 20, yPosition, paint);
            yPosition += lineHeight;
            canvas.drawText("Họ tên: Khách hàng", 20, yPosition, paint);
            yPosition += lineHeight;
            canvas.drawText("Email: customer@example.com", 20, yPosition, paint);
            yPosition += lineHeight;

            // Footer
            yPosition += lineHeight;
            canvas.drawText("Cảm ơn đã mua hàng!", 20, yPosition, paint);

            document.finishPage(page);
            document.close();

            // Save PDF to file
            File pdfFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "receipt_" + orderId + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.close();

            Toast.makeText(this, "Đã lưu biên lai: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (IOException e) {
            Toast.makeText(this, "Lỗi tạo PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareReceipt() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Biên lai thanh toán đơn hàng #" + orderId);
        
        String shareText = "Biên lai thanh toán\n" +
                "Mã đơn hàng: #" + orderId + "\n";
        
        if (paymentInfo != null) {
            if (paymentInfo.has("amount")) {
                shareText += "Số tiền: " + formatCurrency(paymentInfo.get("amount").getAsDouble()) + "\n";
            }
            if (paymentInfo.has("paymentMethod")) {
                shareText += "Phương thức: " + getPaymentMethodText(paymentInfo.get("paymentMethod").getAsString()) + "\n";
            }
            if (paymentInfo.has("transactionId")) {
                shareText += "Mã giao dịch: " + paymentInfo.get("transactionId").getAsString() + "\n";
            }
        }
        
        shareText += "Ngày: " + new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Chia sẻ biên lai"));
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Helper methods
    private String formatCurrency(double amount) {
        return String.format("%,.0f VNĐ", amount);
    }

    private String getStatusText(String status) {
        switch (status) {
            case "DA_THANH_TOAN": return "Đã thanh toán";
            case "CHUA_THANH_TOAN": return "Chưa thanh toán";
            case "DANG_XU_LY": return "Đang xử lý";
            case "THAT_BAI": return "Thất bại";
            case "HET_HAN": return "Hết hạn";
            default: return status;
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

    private String getStatusColor(String status) {
        switch (status) {
            case "DA_THANH_TOAN": return "#4CAF50";
            case "CHUA_THANH_TOAN": 
            case "DANG_XU_LY": return "#FF9800";
            case "THAT_BAI": 
            case "HET_HAN": return "#F44336";
            default: return "#757575";
        }
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
}
