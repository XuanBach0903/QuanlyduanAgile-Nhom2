const mongoose = require('mongoose');

const KichThuocSchema = new mongoose.Schema(
  {
    dai_cm: Number,
    rong_cm: Number,
    cao_cm: Number,
  },
  { _id: false }
);

const SanPhamSchema = new mongoose.Schema(
  {
    danh_muc_id: { type: mongoose.Schema.Types.ObjectId, ref: 'DanhMuc', required: true },
    ten: { type: String, required: true, trim: true },
    gia: { type: Number, required: true },
    kich_thuoc: KichThuocSchema,
    chat_lieu: { type: String },
    mo_ta: { type: String },
    hinh_anh: [{ type: String }],
    hinh_dai_dien: { type: String },
    ton_kho: { type: Number, default: 0 },
    da_ban: { type: Number, default: 0 },
    trang_thai: { type: Number, default: 1 },
    ngay_tao: { type: Date, default: Date.now },
    ngay_cap_nhat: { type: Date, default: Date.now },
  },
  {
    collection: 'san_pham',
  }
);

module.exports = mongoose.model('SanPham', SanPhamSchema);
