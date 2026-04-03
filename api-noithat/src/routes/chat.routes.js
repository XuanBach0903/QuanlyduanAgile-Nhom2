const express = require('express');
const router = express.Router();
const {
  guiTinNhan,
  layLichSuChat,
  guiTinNhanAdmin,
  layLichSuChatAdmin,
  layDanhSachPhongChat,
  guiThongTinSanPham,
  timKiemTinNhan,
  xoaTinNhan,
  demTinNhanChuaDoc,
  layThongKeChat,
  danhDauDaDoc
} = require('../controllers/chat.controller');
const { requireAuth } = require('../middleware/auth');

// Routes cho khách hàng
router.post('/tin-nhan', requireAuth, guiTinNhan);
router.get('/lich-su', requireAuth, layLichSuChat);
router.post('/san-phham', requireAuth, guiThongTinSanPham);
router.get('/tim-kiem', requireAuth, timKiemTinNhan);
router.delete('/tin-nhan/:tin_nhan_id', requireAuth, xoaTinNhan);

// Routes cho admin
router.post('/admin/tin-nhan', requireAuth, guiTinNhanAdmin);
router.get('/admin/lich-su', requireAuth, layLichSuChatAdmin);
router.get('/admin/phong-chat', requireAuth, layDanhSachPhongChat);
router.post('/admin/san-pham', requireAuth, guiThongTinSanPham);
router.get('/admin/tim-kiem', requireAuth, timKiemTinNhan);
router.get('/admin/chua-doc', requireAuth, demTinNhanChuaDoc);
router.get('/admin/thong-ke', requireAuth, layThongKeChat);
router.post('/admin/da-doc', requireAuth, danhDauDaDoc);

module.exports = router;
