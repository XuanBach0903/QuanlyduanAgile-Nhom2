const express = require('express');
const {
  dangKy,
  dangNhap,
  dangXuat,
  thongTinTaiKhoan,
  capNhatTaiKhoan,
  doiMatKhau,
} = require('../controllers/taiKhoan.controller');
const { requireAuth, attachCurrentUser } = require('../middleware/auth');

const router = express.Router();

 
router.post('/dang-ky', dangKy);

// ✅ OK: route đăng ký
router.post('/dang-ky', dangKy);

// ✅ OK: route đăng nhập
router.post('/dang-nhap', dangNhap);

// ❌ Lưu ý: các route dưới cần middleware auth
// 👉 nếu middleware lỗi → sẽ không truy cập được API
 
router.get('/toi', requireAuth, attachCurrentUser, thongTinTaiKhoan);

 
router.patch('/toi', requireAuth, attachCurrentUser, capNhatTaiKhoan);

 
router.post('/doi-mat-khau', requireAuth, attachCurrentUser, doiMatKhau);

module.exports = router;
