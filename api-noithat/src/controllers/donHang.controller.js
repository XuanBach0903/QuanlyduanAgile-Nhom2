const mongoose = require('mongoose');
const DonHang = require('../models/DonHang');
const GioHang = require('../models/GioHang');
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

// POST /don-hang
// Body: { diaChiGiaoHang, ghiChu, phuongThucThanhToan: 'COD'|'VISA' }
async function taoDonHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { diaChiGiaoHang, ghiChu, phuongThucThanhToan } = req.body;

    if (!diaChiGiaoHang || !phuongThucThanhToan) {
      return res.status(400).json({ message: 'diaChiGiaoHang va phuongThucThanhToan la bat buoc' });
    }
    if (!['COD', 'VISA'].includes(phuongThucThanhToan)) {
      return res.status(400).json({ message: 'phuongThucThanhToan chi cho phep COD hoac VISA' });
    }

    const gioHang = await GioHang.findOne({ nguoi_dung_id: userId }).populate('danh_sach.san_pham_id');
    if (!gioHang || !gioHang.danh_sach.length) {
      return res.status(400).json({ message: 'Gio hang dang rong' });
    }

    const items = gioHang.danh_sach.map((item) => ({
      san_pham_id: item.san_pham_id._id || item.san_pham_id,
      so_luong: item.so_luong,
      don_gia: item.don_gia,
    }));

    const tongTien = gioHang.tong_tien;

    const donHang = await DonHang.create({
      nguoi_dung_id: userId,
      dia_chi_giao_hang: diaChiGiaoHang,
      ghi_chu: ghiChu || '',
      phuong_thuc_thanh_toan: phuongThucThanhToan,
      trang_thai_don_hang: 'CHO_XAC_NHAN',
      trang_thai_thanh_toan: 'CHUA_THANH_TOAN',
      items,
      tong_tien: tongTien,
    });

    // Sau khi tạo đơn hàng, clear giỏ hàng
    gioHang.danh_sach = [];
    gioHang.tong_tien = 0;
    gioHang.ngay_cap_nhat = new Date();
    await gioHang.save();

    res.status(201).json({
      id: donHang._id,
      trangThaiDonHang: donHang.trang_thai_don_hang,
      trangThaiThanhToan: donHang.trang_thai_thanh_toan,
      tongTien: donHang.tong_tien,
    });
  } catch (err) {
    next(err);
  }
}

// GET /don-hang (danh sách đơn hàng của tôi)
async function danhSachDonHangCuaToi(req, res, next) {
  try {
    const userId = req.user.id;

    const list = await DonHang.find({ nguoi_dung_id: userId })
      .sort({ ngay_tao: -1 })
      .select('-items');

    res.json(
      list.map((d) => ({
        id: d._id,
        tongTien: d.tong_tien,
        trangThaiDonHang: d.trang_thai_don_hang,
        trangThaiThanhToan: d.trang_thai_thanh_toan,
        phuongThucThanhToan: d.phuong_thuc_thanh_toan,
        lyDoHuy: d.ly_do_huy || null,
        lyDoHoan: d.ly_do_hoan || null,
        loiNhanAdminHoan: d.loi_nhan_admin_hoan || null,
        ngayTao: d.ngay_tao,
      }))
    );
  } catch (err) {
    next(err);
  }
}

// GET /don-hang/:orderId (chi tiết đơn hàng)
async function chiTietDonHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { orderId } = req.params;

    if (!mongoose.isValidObjectId(orderId)) {
      return res.status(400).json({ message: 'orderId khong hop le' });
    }

    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: userId })
      .populate('items.san_pham_id')
      .populate('nguoi_dung_id', 'so_dien_thoai');

    if (!donHang) {
      return res.status(404).json({ message: 'Khong tim thay don hang' });
    }

    const danhGiaList = await DanhGia.find({ don_hang_id: orderId, nguoi_dung_id: userId }).select(
      'san_pham_id so_sao noi_dung ngay_tao'
    );
    const danhGiaMap = new Map(
      (danhGiaList || []).map((dg) => [String(dg.san_pham_id), dg])
    );

    res.json({
      id: donHang._id,
      diaChiGiaoHang: donHang.dia_chi_giao_hang,
      soDienThoai: donHang.nguoi_dung_id?.so_dien_thoai || null,
      ghiChu: donHang.ghi_chu,
      phuongThucThanhToan: donHang.phuong_thuc_thanh_toan,
      trangThaiDonHang: donHang.trang_thai_don_hang,
      trangThaiThanhToan: donHang.trang_thai_thanh_toan,
      lyDoHuy: donHang.ly_do_huy || null,
      lyDoHoan: donHang.ly_do_hoan || null,
      loiNhanAdminHoan: donHang.loi_nhan_admin_hoan || null,
      tongTien: donHang.tong_tien,
      ngayTao: donHang.ngay_tao,
      items: donHang.items.map((it) => ({
        sanPhamId: it.san_pham_id?._id || it.san_pham_id,
        tenSanPham: it.san_pham_id?.ten,
        hinhDaiDien: pickCover(it.san_pham_id),
        hinhAnh: it.san_pham_id?.hinh_anh || [],
        soLuong: it.so_luong,
        donGia: it.don_gia,
        danhGia: (() => {
          const dg = danhGiaMap.get(String(it.san_pham_id?._id || it.san_pham_id));
          if (!dg) return null;
          return {
            soSao: dg.so_sao,
            noiDung: dg.noi_dung,
            ngayTao: dg.ngay_tao,
          };
        })(),
      })),
    });
  } catch (err) {
    next(err);
  }
}

// POST /don-hang/:orderId/huy
async function huyDonHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { orderId } = req.params;
    const { lyDo } = req.body || {};

    if (!mongoose.isValidObjectId(orderId)) {
      return res.status(400).json({ message: 'orderId khong hop le' });
    }

    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: userId });
    if (!donHang) {
      return res.status(404).json({ message: 'Khong tim thay don hang' });
    }

    if (!['CHO_XAC_NHAN', 'DA_XAC_NHAN'].includes(donHang.trang_thai_don_hang)) {
      return res.status(400).json({ message: 'Khong the huy don o trang thai hien tai' });
    }

    donHang.trang_thai_don_hang = 'HUY';
    donHang.ly_do_huy = lyDo || donHang.ly_do_huy || '';
    donHang.ngay_cap_nhat = new Date();
    await donHang.save();

    res.json({ message: 'Da huy don hang thanh cong', lyDo: donHang.ly_do_huy || null });
  } catch (err) {
    next(err);
  }
}

// POST /don-hang/:orderId/xac-nhan-da-nhan
async function xacNhanDaNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { orderId } = req.params;

    if (!mongoose.isValidObjectId(orderId)) {
      return res.status(400).json({ message: 'orderId khong hop le' });
    }

    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: userId });
    if (!donHang) {
      return res.status(404).json({ message: 'Khong tim thay don hang' });
    }

    if (donHang.trang_thai_don_hang !== 'DA_GIAO_CHO_XAC_NHAN') {
      return res.status(400).json({
        message: 'Chi duoc xac nhan da nhan khi don o trang thai DA_GIAO_CHO_XAC_NHAN',
      });
    }

    if (!donHang.da_tru_kho) {
      for (const it of donHang.items || []) {
        const spId = it.san_pham_id;
        const soLuong = it.so_luong || 0;
        if (!spId || soLuong <= 0) continue;

        const sp = await SanPham.findById(spId).select('ton_kho da_ban');
        if (!sp) {
          return res.status(400).json({ message: 'San pham khong ton tai trong don hang' });
        }
        const tonKho = typeof sp.ton_kho === 'number' ? sp.ton_kho : 0;
        const daBan = typeof sp.da_ban === 'number' ? sp.da_ban : 0;
        const conLai = tonKho - daBan;
        if (conLai < soLuong) {
          return res.status(400).json({ message: 'San pham khong du ton kho de xac nhan thanh cong' });
        }
      }

      for (const it of donHang.items || []) {
        const spId = it.san_pham_id;
        const soLuong = it.so_luong || 0;
        if (!spId || soLuong <= 0) continue;
        await SanPham.updateOne(
          { _id: spId },
          { $inc: { da_ban: soLuong }, $set: { ngay_cap_nhat: new Date() } }
        );
      }

      donHang.da_tru_kho = true;
    }

    donHang.trang_thai_don_hang = 'THANH_CONG';
    if (donHang.phuong_thuc_thanh_toan === 'COD') {
      donHang.trang_thai_thanh_toan = 'DA_THANH_TOAN';
    }
    donHang.ngay_cap_nhat = new Date();

    await donHang.save();

    res.json({ message: 'Da xac nhan da nhan hang, don hang thanh cong' });
  } catch (err) {
    next(err);
  }
}

// POST /don-hang/:orderId/hoan
// Body: { lyDo }
async function hoanDonKhach(req, res, next) {
  try {
    const userId = req.user.id;
    const { orderId } = req.params;
    const { lyDo } = req.body || {};

    if (!mongoose.isValidObjectId(orderId)) {
      return res.status(400).json({ message: 'orderId khong hop le' });
    }

    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: userId });
    if (!donHang) {
      return res.status(404).json({ message: 'Khong tim thay don hang' });
    }

    if (donHang.trang_thai_don_hang !== 'THANH_CONG') {
      return res.status(400).json({ message: 'Chi duoc hoan don khi don o trang thai THANH_CONG' });
    }

    if (donHang.trang_thai_thanh_toan !== 'DA_THANH_TOAN') {
      return res.status(400).json({ message: 'Chi gui yeu cau hoan don cho don da thanh toan' });
    }

    donHang.trang_thai_don_hang = 'CHO_XU_LY_HOAN';
    donHang.ly_do_hoan = lyDo || donHang.ly_do_hoan || '';
    donHang.ngay_cap_nhat = new Date();
    await donHang.save();

    return res.json({
      id: donHang._id,
      message: 'Da gui yeu cau hoan don, vui long cho admin xu ly',
      lyDo: lyDo || null,
      trangThaiDonHang: donHang.trang_thai_don_hang,
      trangThaiThanhToan: donHang.trang_thai_thanh_toan,
      phuongThucThanhToan: donHang.phuong_thuc_thanh_toan,
      tongTien: donHang.tong_tien,
    });
  } catch (err) {
    next(err);
  }
}

// GET /don-hang/:orderId/thanh-toan
async function trangThaiThanhToan(req, res, next) {
  try {
    const userId = req.user.id;
    const { orderId } = req.params;

    if (!mongoose.isValidObjectId(orderId)) {
      return res.status(400).json({ message: 'orderId khong hop le' });
    }

    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: userId })
      .select('phuong_thuc_thanh_toan trang_thai_thanh_toan tong_tien');

    if (!donHang) {
      return res.status(404).json({ message: 'Khong tim thay don hang' });
    }

    res.json({
      phuongThucThanhToan: donHang.phuong_thuc_thanh_toan,
      trangThaiThanhToan: donHang.trang_thai_thanh_toan,
      tongTien: donHang.tong_tien,
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  taoDonHang,
  danhSachDonHangCuaToi,
  chiTietDonHang,
  huyDonHang,
  xacNhanDaNhan,
  hoanDonKhach,
  trangThaiThanhToan,
};
