const mongoose = require('mongoose');

const NguoiDungSchema = new mongoose.Schema(
  {
    ho_ten: { type: String, required: true, trim: true },
    email: { type: String, required: true, unique: true, lowercase: true, trim: true },
    so_dien_thoai: { type: String, trim: true },
    dia_chi: { type: String, trim: true },
    img_url: { type: String, trim: true },
    mat_khau_hash: { type: String, required: true },
    vai_tro: { type: String, enum: ['ADMIN', 'KHACH_HANG'], default: 'KHACH_HANG' },
    trang_thai: { type: Number, default: 1 },
    ngay_tao: { type: Date, default: Date.now },
    ngay_cap_nhat: { type: Date, default: Date.now },
  },
  {
    collection: 'nguoi_dung',
  }
);
<<<<<<< HEAD

=======
//fix: sửa lỗi tìm kiếm sản phẩm
>>>>>>> develop
module.exports = mongoose.model('NguoiDung', NguoiDungSchema);
