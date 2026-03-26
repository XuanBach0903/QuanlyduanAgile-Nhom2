const mongoose = require('mongoose');

const DanhGiaSchema = new mongoose.Schema(
  {
    san_pham_id: { type: mongoose.Schema.Types.ObjectId, ref: 'SanPham', required: true },
    nguoi_dung_id: { type: mongoose.Schema.Types.ObjectId, ref: 'NguoiDung', required: true },
    so_sao: { type: Number, required: true, min: 1, max: 5 },
    noi_dung: { type: String, required: true, trim: true },
    ngay_tao: { type: Date, default: Date.now },
    ngay_cap_nhat: { type: Date, default: Date.now },
  },
  {
    collection: 'danh_gia',
  }
);

module.exports = mongoose.model('DanhGia', DanhGiaSchema);
