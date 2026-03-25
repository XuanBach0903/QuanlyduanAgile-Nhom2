const mongoose = require('mongoose');
const SanPham = require('../models/SanPham');
const DanhGia = require('../models/DanhGia');

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

// GET /san-pham?danhMucId=&tuKhoa=&trang=1&gioiHan=6
async function getSanPhamList(req, res, next) {
  try {
    const {
      danhMucId,
      tuKhoa,
      trang = 1,
      gioiHan = 6,
    } = req.query;

    const page = Math.max(parseInt(trang, 10) || 1, 1);
    const limit = Math.max(parseInt(gioiHan, 10) || 6, 1);
    const filter = { trang_thai: 1 };

    if (danhMucId && mongoose.isValidObjectId(danhMucId)) {
      filter.danh_muc_id = danhMucId;
    }

    if (tuKhoa && tuKhoa.trim() !== '') {
      const regex = new RegExp(tuKhoa.trim(), 'i');
      filter.ten = regex;
    }

    const [tongSanPham, danhSach] = await Promise.all([
      SanPham.countDocuments(filter),
      SanPham.find(filter)
        .sort({ ngay_tao: -1 })
        .skip((page - 1) * limit)
        .limit(limit),
    ]);

    const ids = (danhSach || []).map((sp) => sp._id).filter(Boolean);
    const ratingRows = ids.length
      ? await DanhGia.aggregate([
          { $match: { san_pham_id: { $in: ids } } },
          {
            $group: {
              _id: '$san_pham_id',
              soSaoTrungBinh: { $avg: '$so_sao' },
              soLuotDanhGia: { $sum: 1 },
            },
          },
        ])
      : [];
    const ratingMap = new Map(
      (ratingRows || []).map((r) => [String(r._id), { soSaoTrungBinh: r.soSaoTrungBinh, soLuotDanhGia: r.soLuotDanhGia }])
    );

    const tongTrang = Math.ceil(tongSanPham / limit) || 1;

    res.json({
      trang: page,
      gioiHan: limit,
      tongSanPham,
      tongTrang,
      danhSach: danhSach.map((sp) => ({
        id: sp._id,
        ten: sp.ten,
        gia: sp.gia,
        hinhDaiDien: pickCover(sp),
        tonKho: sp.ton_kho,
        tonKhoConLai: (sp.ton_kho || 0) - (sp.da_ban || 0),
        daBan: sp.da_ban || 0,
        soSaoTrungBinh: ratingMap.get(String(sp._id))?.soSaoTrungBinh || 0,
        soLuotDanhGia: ratingMap.get(String(sp._id))?.soLuotDanhGia || 0,
        danhMucId: sp.danh_muc_id,
      })),
    });
  } catch (err) {
    next(err);
  }
}

// GET /san-pham/:id
async function getSanPhamDetail(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isValidObjectId(id)) {
      return res.status(400).json({ message: 'ID san pham khong hop le' });
    }

    const sp = await SanPham.findById(id).populate('danh_muc_id');
    if (!sp) {
      return res.status(404).json({ message: 'Khong tim thay san pham' });
    }

    res.json({
      id: sp._id,
      ten: sp.ten,
      gia: sp.gia,
      kichThuoc: sp.kich_thuoc,
      chatLieu: sp.chat_lieu,
      moTa: sp.mo_ta,
      hinhAnh: sp.hinh_anh,
      hinhDaiDien: pickCover(sp),
      tonKho: sp.ton_kho,
      tonKhoConLai: (sp.ton_kho || 0) - (sp.da_ban || 0),
      trangThai: sp.trang_thai,
      danhMuc: sp.danh_muc_id
        ? {
            id: sp.danh_muc_id._id,
            ten: sp.danh_muc_id.ten,
            slug: sp.danh_muc_id.slug,
          }
        : null,
    });
  } catch (err) {
    next(err);
  }
}

// GET /san-pham/:id/hinh-anh
async function getSanPhamImages(req, res, next) {
  try {
    const { id } = req.params;
    if (!mongoose.isValidObjectId(id)) {
      return res.status(400).json({ message: 'ID san pham khong hop le' });
    }

    const sp = await SanPham.findById(id).select('hinh_anh hinh_dai_dien');
    if (!sp) {
      return res.status(404).json({ message: 'Khong tim thay san pham' });
    }

    res.json({
      id: sp._id,
      hinhAnh: sp.hinh_anh || [],
      hinhDaiDien: pickCover(sp) || null,
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  getSanPhamList,
  getSanPhamDetail,
  getSanPhamImages,
};
