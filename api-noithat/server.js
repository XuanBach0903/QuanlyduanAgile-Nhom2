require("dotenv").config();
const express = require("express");
const mongoose = require("mongoose");
const morgan = require("morgan");
const cors = require("cors");

// Routes
const sanPhamRoutes = require("./src/routes/sanPham.routes");
const taiKhoanRoutes = require("./src/routes/taiKhoan.routes");
const gioHangRoutes = require("./src/routes/gioHang.routes");
const donHangRoutes = require("./src/routes/donHang.routes");
const paymentRoutes = require("./src/routes/payment.routes");
const chatRoutes = require("./src/routes/chat.routes");
const danhGiaRoutes = require("./src/routes/danhGia.routes");
const hoaDonRoutes = require("./src/routes/hoaDon.routes");
const xacNhanGiaoHangRoutes = require("./src/routes/xacNhanGiaoHang.routes");

// Admin
const adminSanPhamRoutes = require("./src/routes/adminSanPham.routes");
const adminDanhMucRoutes = require("./src/routes/adminDanhMuc.routes");

const app = express();

// Middleware
app.use(cors());
app.use(express.json());
app.use(morgan("dev"));

// MongoDB
const mongoUri = process.env.MONGODB_URI || "mongodb://127.0.0.1:27017/banhang";

mongoose
  .connect(mongoUri)
  .then(() => {
    console.log("MongoDB connected");
  })
  .catch((err) => {
    console.error("MongoDB connection error:", err.message);
  });

// Routes
app.use("/san-pham", sanPhamRoutes);
app.use("/tai-khoan", taiKhoanRoutes);
app.use("/gio-hang", gioHangRoutes);
app.use("/don-hang", donHangRoutes);
app.use("/payment", paymentRoutes);
app.use("/chat", chatRoutes);

app.use("/", danhGiaRoutes);
app.use("/", hoaDonRoutes);
app.use("/", xacNhanGiaoHangRoutes);

app.use("/", adminSanPhamRoutes);
app.use("/", adminDanhMucRoutes);

// Health check
app.get("/", (req, res) => {
  res.json({ message: "API running" });
});

// Error handler
app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({
    message: err.message || "Internal server error",
  });
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});
