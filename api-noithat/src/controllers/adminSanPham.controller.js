const mongoose = require('mongoose');
const SanPham = require('../models/SanPham');

// GET /admin/san-pham
// Danh sách sản phẩm cho admin (trả đầy đủ thông tin, có hỗ trợ tìm kiếm)
async function danhSachSanPhamAdmin(req, res, next) {
  try {
    const { tuKhoa, danhMucId } = req.query;

    const filter = {};
    if (danhMucId && mongoose.isValidObjectId(danhMucId)) {
      filter.danh_muc_id = danhMucId;
    }
    if (tuKhoa && tuKhoa.trim() !== '') {
      const regex = new RegExp(tuKhoa.trim(), 'i');
      filter.ten = regex;
    }

    const list = await SanPham.find(filter).populate('danh_muc_id');

    const result = list.map((sp) => ({
      id: sp._id,
      ten: sp.ten,
      gia: sp.gia,
      kichThuoc: sp.kich_thuoc
        ? {
            dai_cm: sp.kich_thuoc.dai_cm,
            rong_cm: sp.kich_thuoc.rong_cm,
            cao_cm: sp.kich_thuoc.cao_cm,
          }
        : undefined,
      chatLieu: sp.chat_lieu,
      moTa: sp.mo_ta,
      hinhAnh: sp.hinh_anh || [],
      hinhDaiDien: sp.hinh_dai_dien,
      tonKho: sp.ton_kho,
      trangThai: sp.trang_thai,
      danhMuc: sp.danh_muc_id
        ? {
            id: sp.danh_muc_id._id,
            ten: sp.danh_muc_id.ten,
            slug: sp.danh_muc_id.slug,
          }
        : null,
    }));

    res.json(result);
  } catch (err) {
    next(err);
  }
}

// POST /admin/san-pham
async function taoSanPham(req, res, next) {
  try {
    const {
      danhMucId,
      ten,
      gia,
      kichThuoc,
      chatLieu,
      moTa,
      hinhDaiDien,
      hinhAnh,
      tonKho,
      trangThai,
    } = req.body;

    // Kiểm tra và gán giá trị mặc định cho các trường thiếu
    const finalDanhMucId = danhMucId || '000000000000000000000000'; // Giá trị mặc định
    const finalTen = ten || 'Sản phẩm chưa đặt tên';
    const finalGia = gia || 0;

    if (danhMucId && !mongoose.isValidObjectId(danhMucId)) {
      return res.status(400).json({ message: 'danhMucId khong hop le' });
    }

    const sp = await SanPham.create({
      danh_muc_id: finalDanhMucId,
      ten: finalTen,
      gia: finalGia,
      kich_thuoc: kichThuoc || null,
      chat_lieu: chatLieu || '',
      mo_ta: moTa || '',
      hinh_anh: Array.isArray(hinhAnh) ? hinhAnh : [],
      hinh_dai_dien: hinhDaiDien || '',
      ton_kho: tonKho ?? 0,
      trang_thai: trangThai ?? 1,
    });

    res.status(201).json({ id: sp._id });
  } catch (err) {
    next(err);
  }
}

// PATCH /admin/san-pham/:id
async function capNhatSanPham(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isValidObjectId(id)) {
      return res.status(400).json({ message: 'id khong hop le' });
    }

    const update = {};
    const {
      danhMucId,
      ten,
      gia,
      kichThuoc,
      chatLieu,
      moTa,
      hinhDaiDien,
      tonKho,
      trangThai,
    } = req.body;

    if (danhMucId) {
      if (!mongoose.isValidObjectId(danhMucId)) {
        return res.status(400).json({ message: 'danhMucId khong hop le' });
      }
      update.danh_muc_id = danhMucId;
    }
    if (ten !== undefined) update.ten = ten;
    if (gia !== undefined) update.gia = gia;
    if (kichThuoc !== undefined) update.kich_thuoc = kichThuoc;
    if (chatLieu !== undefined) update.chat_lieu = chatLieu;
    if (moTa !== undefined) update.mo_ta = moTa;
    if (hinhDaiDien !== undefined) update.hinh_dai_dien = hinhDaiDien;
    if (tonKho !== undefined) update.ton_kho = tonKho;
    if (trangThai !== undefined) update.trang_thai = trangThai;

    update.ngay_cap_nhat = new Date();

    const sp = await SanPham.findByIdAndUpdate(id, update, { new: true });
    if (!sp) {
      return res.status(404).json({ message: 'Khong tim thay san pham' });
    }

    res.json({ message: 'Cap nhat san pham thanh cong' });
  } catch (err) {
    next(err);
  }
}

// DELETE /admin/san-pham/:id
async function xoaSanPham(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isValidObjectId(id)) {
      return res.status(400).json({ message: 'id khong hop le' });
    }

    const sp = await SanPham.findByIdAndDelete(id);
    if (!sp) {
      return res.status(404).json({ message: 'Khong tim thay san pham' });
    }

    res.json({ message: 'Da xoa san pham' });
  } catch (err) {
    next(err);
  }
}

// POST /admin/san-pham/:id/hinh-anh
async function themHinhAnh(req, res, next) {
  try {
    const { id } = req.params;
    const { url } = req.body;

    if (!mongoose.isValidObjectId(id)) {
      return res.status(400).json({ message: 'id khong hop le' });
    }
    if (!url) {
      return res.status(400).json({ message: 'url la bat buoc' });
    }

    const sp = await SanPham.findById(id);
    if (!sp) {
      return res.status(404).json({ message: 'Khong tim thay san pham' });
    }

    sp.hinh_anh = sp.hinh_anh || [];
    sp.hinh_anh.push(url);
    sp.ngay_cap_nhat = new Date();
    await sp.save();

    res.status(201).json({ message: 'Da them hinh anh' });
  } catch (err) {
    next(err);
  }
}

// DELETE /admin/san-pham/:id/hinh-anh/:index
// Đơn giản xoá theo index trong mảng hinh_anh
async function xoaHinhAnh(req, res, next) {
  try {
    const { id, index } = req.params;

    if (!mongoose.isValidObjectId(id)) {
      return res.status(400).json({ message: 'id khong hop le' });
    }

    const idx = parseInt(index, 10);
    if (Number.isNaN(idx) || idx < 0) {
      return res.status(400).json({ message: 'index khong hop le' });
    }

    const sp = await SanPham.findById(id);
    if (!sp) {
      return res.status(404).json({ message: 'Khong tim thay san pham' });
    }

    if (!sp.hinh_anh || idx >= sp.hinh_anh.length) {
      return res.status(400).json({ message: 'Khong co hinh anh o vi tri nay' });
    }

    sp.hinh_anh.splice(idx, 1);
    sp.ngay_cap_nhat = new Date();
    await sp.save();

    res.json({ message: 'Da xoa hinh anh' });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  danhSachSanPhamAdmin,
  taoSanPham,
  capNhatSanPham,
  xoaSanPham,
  themHinhAnh,
  xoaHinhAnh,
};
