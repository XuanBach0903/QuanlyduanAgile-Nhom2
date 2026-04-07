const express = require('express');
const {
  getSanPhamList,
  getSanPhamDetail,
  getSanPhamImages,
} = require('../controllers/sanPham.controller');

const router = express.Router();

// GET /san-pham
router.get('/', getSanPhamList);

// GET /san-pham/:id
router.get('/:id', getSanPhamDetail);

// GET /san-pham/:id/hinh-anh
router.get('/:id/hinh-anh', getSanPhamImages);

module.exports = router;
