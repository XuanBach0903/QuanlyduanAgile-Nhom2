const express = require('express');
const router = express.Router();
const {
  taoHoaDon,
  layDanhSachHoaDon,
  layChiTietHoaDon,
  xuatHoaDon,
  huyHoaDon,
  taiHoaDonPDF,
  layThongKeHoaDon,
  layTatCaHoaDon,
  timKiemHoaDon
} = require('../controllers/hoaDon.controller');
const { requireAuth } = require('../middleware/auth');

// Customer routes
router.post('/hoa-don', requireAuth, taoHoaDon);
router.get('/hoa-don', requireAuth, layDanhSachHoaDon);
router.get('/hoa-don/chi-tiet/:hoa_don_id', requireAuth, layChiTietHoaDon);
router.delete('/hoa-don/:hoa_don_id', requireAuth, huyHoaDon);
router.get('/hoa-don/:hoa_don_id/pdf', requireAuth, taiHoaDonPDF);
router.get('/hoa-don/thong-ke', requireAuth, layThongKeHoaDon);

// Admin routes
router.post('/admin/hoa-don/:hoa_don_id/xuat', requireAuth, xuatHoaDon);
router.get('/admin/hoa-don', requireAuth, layTatCaHoaDon);
router.get('/admin/hoa-don/tim-kiem', requireAuth, timKiemHoaDon);

module.exports = router;
