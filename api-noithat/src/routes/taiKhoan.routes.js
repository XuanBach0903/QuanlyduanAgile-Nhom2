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

 
router.post('/dang-nhap', dangNhap);

 
router.post('/dang-xuat', requireAuth, dangXuat);

 
router.get('/toi', requireAuth, attachCurrentUser, thongTinTaiKhoan);

 
router.patch('/toi', requireAuth, attachCurrentUser, capNhatTaiKhoan);

 
router.post('/doi-mat-khau', requireAuth, attachCurrentUser, doiMatKhau);

module.exports = router;
