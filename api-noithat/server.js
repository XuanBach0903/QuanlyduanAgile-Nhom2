require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const morgan = require('morgan');
const cors = require('cors');
const http = require('http');
const SocketHandler = require('./src/socket/SocketHandler');

// const danhMucRoutes = require('./src/routes/danhMuc.routes'); // TODO: tạo file
const sanPhamRoutes = require('./src/routes/sanPham.routes');
const taiKhoanRoutes = require('./src/routes/taiKhoan.routes');
const gioHangRoutes = require('./src/routes/gioHang.routes');
const donHangRoutes = require('./src/routes/donHang.routes');
const paymentRoutes = require('./src/routes/payment.routes');
const chatRoutes = require('./src/routes/chat.routes');
const danhGiaRoutes = require('./src/routes/danhGia.routes');
// const thanhToanRoutes = require('./src/routes/thanhToan.routes'); // TODO: tạo file
// const chatRoutes = require('./src/routes/chat.routes'); // TODO: tạo file
// const danhGiaRoutes = require('./src/routes/danhGia.routes'); // TODO: tạo file
const adminSanPhamRoutes = require('./src/routes/adminSanPham.routes');
// const adminDonHangRoutes = require('./src/routes/adminDonHang.routes'); // TODO: tạo file
// const adminHoaDonRoutes = require('./src/routes/adminHoaDon.routes'); // TODO: tạo file
const adminDanhMucRoutes = require('./src/routes/adminDanhMuc.routes');
// const adminKhachHangRoutes = require('./src/routes/adminKhachHang.routes'); // TODO: tạo file
// const adminChatRoutes = require('./src/routes/adminChat.routes'); // TODO: tạo file
// const adminThongKeRoutes = require('./src/routes/adminThongKe.routes'); // TODO: tạo file

const app = express();

// Tạo HTTP server cho Socket.IO
const server = http.createServer(app);

// Khởi tạo Socket.IO handler
const socketHandler = new SocketHandler(server);

// Middleware chung
app.use(cors());
app.use(express.json());
app.use(morgan('dev'));

// Kết nối MongoDB
const mongoUri = process.env.MONGODB_URI || 'mongodb://127.0.0.1:27017/banhang';

mongoose
  .connect(mongoUri)
  .then(() => {
    console.log('MongoDB connected');
  })
  .catch((err) => {
    console.error('MongoDB connection error:', err.message);
  });

// Routes
// app.use('/danh-muc', danhMucRoutes);
app.use('/san-pham', sanPhamRoutes);
app.use('/tai-khoan', taiKhoanRoutes);
app.use('/gio-hang', gioHangRoutes);
app.use('/don-hang', donHangRoutes);
app.use('/payment', paymentRoutes);
app.use('/chat', chatRoutes);
app.use('/', danhGiaRoutes);
// app.use('/', thanhToanRoutes);
// app.use('/chat', chatRoutes);
// app.use('/', danhGiaRoutes);
app.use('/', adminSanPhamRoutes);
// app.use('/', adminDonHangRoutes);
// app.use(adminHoaDonRoutes);
app.use(adminDanhMucRoutes);
// app.use('/', adminKhachHangRoutes);
// app.use('/', adminChatRoutes);
// app.use('/', adminThongKeRoutes);

// Health check
app.get('/', (req, res) => {
  res.json({ message: 'API ban hang ban ghe running' });
});

// Middleware xử lý lỗi đơn giản
app.use((err, req, res, next) => {
  console.error(err);
  const status = err.status || 500;
  res.status(status).json({
    message: err.message || 'Internal server error',
  });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
  console.log(`Socket.IO server is ready`);
});
