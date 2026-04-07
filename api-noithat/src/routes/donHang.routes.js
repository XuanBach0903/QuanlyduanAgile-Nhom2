const express = require('express');
const {
  taoDonHang,
  danhSachDonHangCuaToi,
  chiTietDonHang,
  huyDonHang,
  xacNhanDaNhan,
  hoanDonKhach,
  trangThaiThanhToan,
} = require('../controllers/donHang.controller');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();

// Tạo đơn hàng
router.post('/', requireAuth, taoDonHang);

// Danh sách đơn hàng của tôi
router.get('/', requireAuth, danhSachDonHangCuaToi);

// Chi tiết đơn hàng
router.get('/:orderId', requireAuth, chiTietDonHang);

// Hủy đơn hàng
router.post('/:orderId/huy', requireAuth, huyDonHang);

// Hoàn đơn (khach yeu cau hoan don da thanh toan VISA, trang thai THANH_CONG)
router.post('/:orderId/hoan', requireAuth, hoanDonKhach);

// Khách xác nhận đã nhận hàng
router.post('/:orderId/xac-nhan-da-nhan', requireAuth, xacNhanDaNhan);

// Trạng thái thanh toán của đơn hàng
router.get('/:orderId/thanh-toan', requireAuth, trangThaiThanhToan);

module.exports = router;
