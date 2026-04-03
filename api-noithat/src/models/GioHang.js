const mongoose = require('mongoose');

const GioHangItemSchema = new mongoose.Schema(
  {
    san_pham_id: { type: mongoose.Schema.Types.ObjectId, ref: 'SanPham', required: true },
    so_luong: { type: Number, required: true, min: 1 },
    don_gia: { type: Number, required: true },
  },
  { _id: true }
);

const GioHangSchema = new mongoose.Schema(
  {
    nguoi_dung_id: { type: mongoose.Schema.Types.ObjectId, ref: 'NguoiDung', required: true, unique: true },
    danh_sach: [GioHangItemSchema],
    tong_tien: { type: Number, default: 0 },
    ngay_tao: { type: Date, default: Date.now },
    ngay_cap_nhat: { type: Date, default: Date.now },
  },
  {
    collection: 'gio_hang',
  }
);

module.exports = mongoose.model('GioHang', GioHangSchema);
