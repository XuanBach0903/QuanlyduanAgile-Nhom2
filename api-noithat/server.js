require('dotenv').config();
const express = require('express');
const mongoose = require('mongoose');
const morgan = require('morgan');
const cors = require('cors');

const adminDanhMucRoutes = require('./src/routes/adminDanhMuc.routes')

// ❌ BUG: file danhMuc.routes.js không tồn tại trong src/routes
// 👉 nguyên nhân: bạn chỉ có adminDanhMuc.routes.js
// 👉 fix: tạo file hoặc dùng adminDanhMucRoutes thay thế
// const danhMucRoutes = require('./src/routes/danhMuc.routes');

const sanPhamRoutes = require('./src/routes/sanPham.routes');
const taiKhoanRoutes = require('./src/routes/taiKhoan.routes');

// ❌ BUG: có thể các file dưới chưa tồn tại → dễ crash server
// 👉 nên comment nếu chưa làm Sprint sau
const gioHangRoutes = require('./src/routes/gioHang.routes');
const donHangRoutes = require('./src/routes/donHang.routes');
const thanhToanRoutes = require('./src/routes/thanhToan.routes');
const chatRoutes = require('./src/routes/chat.routes');
const danhGiaRoutes = require('./src/routes/danhGia.routes');

const adminSanPhamRoutes = require('./src/routes/adminSanPham.routes');
const adminDonHangRoutes = require('./src/routes/adminDonHang.routes');
const adminHoaDonRoutes = require('./src/routes/adminHoaDon.routes');

// ❌ BUG: khai báo trùng biến adminDanhMucRoutes (2 lần)
// 👉 sẽ gây lỗi "Identifier already declared"
const adminDanhMucRoutes = require('./src/routes/adminDanhMuc.routes');

const adminKhachHangRoutes = require('./src/routes/adminKhachHang.routes');
const adminChatRoutes = require('./src/routes/adminChat.routes');
const adminThongKeRoutes = require('./src/routes/adminThongKe.routes');

const app = express();

// ❌ BUG: mount admin route ở "/" → dễ đè route khác
// 👉 nên dùng /admin
app.use('/', adminDanhMucRoutes);

// ❌ BUG: đã comment cors → frontend sẽ gọi API bị chặn
// 👉 fix: bật lại cors
// app.use(cors());

app.use(express.json());
app.use(morgan('dev'));

// MongoDB OK

// ❌ BUG: dùng danhMucRoutes nhưng đã comment require phía trên → crash
app.use('/danh-muc', danhMucRoutes);

app.use('/san-pham', sanPhamRoutes);
app.use('/tai-khoan', taiKhoanRoutes);

// ❌ BUG: các route này nếu file chưa tồn tại → server crash
app.use('/gio-hang', gioHangRoutes);
app.use('/don-hang', donHangRoutes);
app.use('/', thanhToanRoutes);
app.use('/chat', chatRoutes);
app.use('/', danhGiaRoutes);
app.use('/', adminSanPhamRoutes);
app.use('/', adminDonHangRoutes);
app.use(adminHoaDonRoutes);
app.use(adminDanhMucRoutes);
app.use('/', adminKhachHangRoutes);
app.use('/', adminChatRoutes);
app.use('/', adminThongKeRoutes);

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

const port = process.env.PORT || 4000;
app.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
