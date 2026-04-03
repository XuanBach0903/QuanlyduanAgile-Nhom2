const mongoose = require('mongoose');
const GioHang = require('../models/GioHang');
const SanPham = require('../models/SanPham');

function pickCover(sp) {
  const cover = sp?.hinh_dai_dien;
  if (typeof cover === 'string' && cover.trim() !== '') {
    return cover;
  }
  const arr = sp?.hinh_anh;
  if (Array.isArray(arr) && arr.length > 0 && typeof arr[0] === 'string') {
    return arr[0];
  }
  return '';
}

function tinhTongTien(danhSach) {
  return danhSach.reduce((sum, item) => sum + item.so_luong * item.don_gia, 0);
}

// GET /gio-hang
async function xemGioHang(req, res, next) {
  try {
    const userId = req.user.id;

    let gioHang = await GioHang.findOne({ nguoi_dung_id: userId }).populate(
      'danh_sach.san_pham_id'
    );

    if (!gioHang) {
      return res.json({
        tongTien: 0,
        danhSach: [],
      });
    }

    const responseItems = gioHang.danh_sach.map((item) => ({
      itemId: item._id,
      sanPhamId: item.san_pham_id?._id || item.san_pham_id,
      tenSanPham: item.san_pham_id?.ten,
      hinhDaiDien: pickCover(item.san_pham_id),
      soLuong: item.so_luong,
      donGia: item.don_gia,
      thanhTien: item.so_luong * item.don_gia,
    }));

    res.json({
      tongTien: gioHang.tong_tien,
      danhSach: responseItems,
    });
  } catch (err) {
    next(err);
  }
}

// POST /gio-hang/mat-hang
// Body: { sanPhamId, soLuong }
async function themMatHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { sanPhamId, soLuong } = req.body;

    if (!sanPhamId || !soLuong || soLuong <= 0) {
      return res.status(400).json({ message: 'sanPhamId va soLuong > 0 la bat buoc' });
    }

    if (!mongoose.isValidObjectId(sanPhamId)) {
      return res.status(400).json({ message: 'sanPhamId khong hop le' });
    }

    const sp = await SanPham.findById(sanPhamId);
    if (!sp || sp.trang_thai !== 1) {
      return res.status(404).json({ message: 'San pham khong ton tai hoac khong ban' });
    }

    let gioHang = await GioHang.findOne({ nguoi_dung_id: userId });
    if (!gioHang) {
      gioHang = new GioHang({
        nguoi_dung_id: userId,
        danh_sach: [],
        tong_tien: 0,
      });
    }

    const donGia = sp.gia;

    const existingItem = gioHang.danh_sach.find((item) =>
      item.san_pham_id.toString() === sanPhamId
    );

    if (existingItem) {
      existingItem.so_luong += soLuong;
      existingItem.don_gia = donGia; // update don gia moi nhat
    } else {
      gioHang.danh_sach.push({
        san_pham_id: sanPhamId,
        so_luong: soLuong,
        don_gia: donGia,
      });
    }

    gioHang.tong_tien = tinhTongTien(gioHang.danh_sach);
    gioHang.ngay_cap_nhat = new Date();

    await gioHang.save();

    res.status(201).json({ message: 'Da them vao gio hang' });
  } catch (err) {
    next(err);
  }
}

// PATCH /gio-hang/mat-hang/:itemId
// Body: { soLuong }
async function capNhatSoLuong(req, res, next) {
  try {
    const userId = req.user.id;
    const { itemId } = req.params;
    const { soLuong } = req.body;

    if (!soLuong || soLuong <= 0) {
      return res.status(400).json({ message: 'soLuong > 0 la bat buoc' });
    }

    const gioHang = await GioHang.findOne({ nguoi_dung_id: userId });
    if (!gioHang) {
      return res.status(404).json({ message: 'Gio hang khong ton tai' });
    }

    const item = gioHang.danh_sach.id(itemId);
    if (!item) {
      return res.status(404).json({ message: 'Mat hang khong ton tai trong gio' });
    }

    item.so_luong = soLuong;
    gioHang.tong_tien = tinhTongTien(gioHang.danh_sach);
    gioHang.ngay_cap_nhat = new Date();

    await gioHang.save();

    res.json({ message: 'Cap nhat so luong thanh cong' });
  } catch (err) {
    next(err);
  }
}

// DELETE /gio-hang/mat-hang/:itemId
async function xoaMatHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { itemId } = req.params;

    const gioHang = await GioHang.findOne({ nguoi_dung_id: userId });
    if (!gioHang) {
      return res.status(404).json({ message: 'Gio hang khong ton tai' });
    }

    const item = gioHang.danh_sach.id(itemId);
    if (!item) {
      return res.status(404).json({ message: 'Mat hang khong ton tai trong gio' });
    }

    // Mongoose 7 không còn item.remove() cho subdocument, dùng pull để xoá khỏi mảng
    gioHang.danh_sach.pull(item._id);
    gioHang.tong_tien = tinhTongTien(gioHang.danh_sach);
    gioHang.ngay_cap_nhat = new Date();

    await gioHang.save();

    res.json({ message: 'Da xoa mat hang khoi gio' });
  } catch (err) {
    next(err);
  }
}

// DELETE /gio-hang
async function xoaToanBoGio(req, res, next) {
  try {
    const userId = req.user.id;

    const gioHang = await GioHang.findOne({ nguoi_dung_id: userId });
    if (!gioHang) {
      return res.json({ message: 'Gio hang da rong' });
    }

    gioHang.danh_sach = [];
    gioHang.tong_tien = 0;
    gioHang.ngay_cap_nhat = new Date();
    await gioHang.save();

    res.json({ message: 'Da xoa toan bo gio hang' });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  xemGioHang,
  themMatHang,
  capNhatSoLuong,
  xoaMatHang,
  xoaToanBoGio,
};
