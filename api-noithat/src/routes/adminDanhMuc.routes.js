const express = require('express');
const {
  danhSachDanhMucAdmin,
  taoDanhMuc,
  capNhatDanhMuc,
  xoaDanhMuc,
} = require('../controllers/adminDanhMuc.controller');

const router = express.Router();

// Routes cho quản lý danh mục
router.get('/admin/danh-muc', danhSachDanhMucAdmin);
router.post('/admin/danh-muc', taoDanhMuc);
router.patch('/admin/danh-muc/:id', capNhatDanhMuc);
router.delete('/admin/danh-muc/:id', xoaDanhMuc);

module.exports = router;
