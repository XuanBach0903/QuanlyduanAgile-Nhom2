const mongoose = require('mongoose');

const DonHangItemSchema = new mongoose.Schema(
  {
    san_pham_id: { type: mongoose.Schema.Types.ObjectId, ref: 'SanPham', required: true },
    so_luong: { type: Number, required: true, min: 1 },
    don_gia: { type: Number, required: true },
  },
  { _id: false }
);

const DonHangSchema = new mongoose.Schema(
  {
    nguoi_dung_id: { type: mongoose.Schema.Types.ObjectId, ref: 'NguoiDung', required: true },
    dia_chi_giao_hang: { type: String, required: true },
    ghi_chu: { type: String },
    phuong_thuc_thanh_toan: { type: String, enum: ['COD', 'VISA'], required: true },
    trang_thai_don_hang: {
      type: String,
      enum: [
        'CHO_XAC_NHAN',
        'DA_XAC_NHAN',
        'DANG_GIAO',
        'DA_GIAO_CHO_XAC_NHAN',
        'THANH_CONG',
        'CHO_XU_LY_HOAN',
        'HUY',
        'HOAN_DON',
      ],
      default: 'CHO_XAC_NHAN',
    },
    trang_thai_thanh_toan: {
      type: String,
      enum: ['CHUA_THANH_TOAN', 'DA_THANH_TOAN', 'DA_HOAN_TIEN'],
      default: 'CHUA_THANH_TOAN',
    },
    ly_do_huy: { type: String },
    ly_do_hoan: { type: String },
    loi_nhan_admin_hoan: { type: String },
    da_tru_kho: { type: Boolean, default: false },
    items: [DonHangItemSchema],
    tong_tien: { type: Number, required: true },
    ngay_tao: { type: Date, default: Date.now },
    ngay_cap_nhat: { type: Date, default: Date.now },
  },
  {
    collection: 'don_hang',
  }
);

module.exports = mongoose.model('DonHang', DonHangSchema);
