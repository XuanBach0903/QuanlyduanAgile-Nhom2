const express = require('express');
const router = express.Router();
const {
  taoDanhGia,
  layDanhGiaSanPham,
  layDanhGiaNguoiDung,
  capNhatDanhGia,
  xoaDanhGia,
  thichDanhGia,
  baoCaoDanhGia,
  phanHoiDanhGia,
  duyetDanhGia,
  layDanhGiaCanDuyet,
  layThongKeDanhGia
} = require('../controllers/danhGia.controller');
const { requireAuth } = require('../middleware/auth');

// Routes cho khách hàng
router.post('/danh-gia', requireAuth, taoDanhGia);
router.get('/danh-gia/san-pham/:san_pham_id', layDanhGiaSanPham);
router.get('/danh-gia/cua-toi', requireAuth, layDanhGiaNguoiDung);
router.put('/danh-gia/:danh_gia_id', requireAuth, capNhatDanhGia);
router.delete('/danh-gia/:danh_gia_id', requireAuth, xoaDanhGia);
router.post('/danh-gia/:danh_gia_id/thich', requireAuth, thichDanhGia);
router.post('/danh-gia/:danh_gia_id/bao-cao', requireAuth, baoCaoDanhGia);

// Routes cho admin
router.post('/admin/danh-gia/:danh_gia_id/phan-hoi', requireAuth, phanHoiDanhGia);
router.put('/admin/danh-gia/:danh_gia_id/duyet', requireAuth, duyetDanhGia);
router.get('/admin/danh-gia/can-duyet', requireAuth, layDanhGiaCanDuyet);
router.get('/admin/danh-gia/thong-ke', requireAuth, layThongKeDanhGia);

module.exports = router;
