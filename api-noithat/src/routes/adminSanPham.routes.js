const express = require('express');
const {
  danhSachSanPhamAdmin,
  taoSanPham,
  capNhatSanPham,
  xoaSanPham,
  themHinhAnh,
  xoaHinhAnh,
} = require('../controllers/adminSanPham.controller');
const { requireAuth, requireRole } = require('../middleware/auth');

const router = express.Router();

// Chỉ ADMIN được dùng các route này

// Danh sách sản phẩm
router.get('/admin/san-pham', requireAuth, requireRole('ADMIN'), danhSachSanPhamAdmin);

// Thêm sản phẩm mới
router.post('/admin/san-pham', requireAuth, requireRole('ADMIN'), taoSanPham);

// Sửa sản phẩm
router.patch('/admin/san-pham/:id', requireAuth, requireRole('ADMIN'), capNhatSanPham);

// Xoá sản phẩm
router.delete('/admin/san-pham/:id', requireAuth, requireRole('ADMIN'), xoaSanPham);

// Thêm hình ảnh cho sản phẩm
router.post('/admin/san-pham/:id/hinh-anh', requireAuth, requireRole('ADMIN'), themHinhAnh);

// Xoá 1 hình ảnh theo index
router.delete(
  '/admin/san-pham/:id/hinh-anh/:index',
  requireAuth,
  requireRole('ADMIN'),
  xoaHinhAnh
);

module.exports = router;
