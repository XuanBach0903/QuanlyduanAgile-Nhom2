const express = require('express');
const router = express.Router();
const {
  taoYeuCauXacNhan,
  xacNhanGiaoHang,
  tuChoiGiaoHang,
  hoanTatGiaoHang,
  danhGiaGiaoHang,
  layDanhSachXacNhan,
  layChiTietXacNhan,
  layDanhSachChoXacNhan,
  nhanGiaoHang,
  layThongKeXacNhan
} = require('../controllers/xacNhanGiaoHang.controller');
const { requireAuth } = require('../middleware/auth');

// Customer routes
router.post('/xac-nhan-giao-hang', requireAuth, taoYeuCauXacNhan);
router.post('/xac-nhan-giao-hang/:xac_nhan_id/xac-nhan', requireAuth, xacNhanGiaoHang);
router.post('/xac-nhan-giao-hang/:xac_nhan_id/tu-choi', requireAuth, tuChoiGiaoHang);
router.post('/xac-nhan-giao-hang/:xac_nhan_id/danh-gia', requireAuth, danhGiaGiaoHang);
router.get('/xac-nhan-giao-hang', requireAuth, layDanhSachXacNhan);
router.get('/xac-nhan-giao-hang/:xac_nhan_id', requireAuth, layChiTietXacNhan);

// Shipper routes
router.get('/shipper/xac-nhan-giao-hang/cho-xac-nhan', requireAuth, layDanhSachChoXacNhan);
router.post('/shipper/xac-nhan-giao-hang/:xac_nhan_id/nhan', requireAuth, nhanGiaoHang);
router.post('/shipper/xac-nhan-giao-hang/:xac_nhan_id/hoan-tat', requireAuth, hoanTatGiaoHang);

// Admin routes
router.get('/admin/xac-nhan-giao-hang/thong-ke', requireAuth, layThongKeXacNhan);

module.exports = router;
