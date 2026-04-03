const express = require('express');
const {
  xemGioHang,
  themMatHang,
  capNhatSoLuong,
  xoaMatHang,
  xoaToanBoGio,
} = require('../controllers/gioHang.controller');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// Xem giỏ hàng của tài khoản hiện tại
router.get('/', requireAuth, xemGioHang);

// Thêm sản phẩm vào giỏ
router.post('/mat-hang', requireAuth, themMatHang);

// Tăng/giảm số lượng 1 mặt hàng trong giỏ
router.patch('/mat-hang/:itemId', requireAuth, capNhatSoLuong);

// Xóa 1 mặt hàng khỏi giỏ
router.delete('/mat-hang/:itemId', requireAuth, xoaMatHang);

// Xóa toàn bộ giỏ
router.delete('/', requireAuth, xoaToanBoGio);

module.exports = router;
