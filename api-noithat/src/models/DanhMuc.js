const mongoose = require('mongoose');

const DanhMucSchema = new mongoose.Schema(
  {
    ten: { type: String, required: true, trim: true },
    slug: { type: String, required: true, unique: true, lowercase: true, trim: true },
    mo_ta: { type: String },
    ngay_tao: { type: Date, default: Date.now },
    ngay_cap_nhat: { type: Date, default: Date.now },
  },
  {
    collection: 'danh_muc',
  }
);

module.exports = mongoose.model('DanhMuc', DanhMucSchema);
