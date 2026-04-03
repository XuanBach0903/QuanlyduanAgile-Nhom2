const mongoose = require('mongoose');

const chatSchema = new mongoose.Schema({
  nguoi_dung_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'NguoiDung',
    required: true
  },
  admin_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'NguoiDung',
    required: true
  },
  tin_nhan: {
    type: String,
    required: true,
    trim: true,
    maxlength: 1000
  },
  loai_tin_nhan: {
    type: String,
    enum: ['TEXT', 'IMAGE', 'FILE', 'PRODUCT'],
    default: 'TEXT'
  },
  url_file: {
    type: String,
    default: null
  },
  thong_tin_san_pham: {
    san_pham_id: String,
    ten_san_pham: String,
    hinh_anh: String,
    gia: Number
  },
  trang_thai: {
    type: String,
    enum: ['DA_GUI', 'DA_DOC', 'DA_TRA_LOI'],
    default: 'DA_GUI'
  },
  gui_boi: {
    type: String,
    enum: ['KHACH_HANG', 'ADMIN'],
    required: true
  },
  ngay_tao: {
    type: Date,
    default: Date.now
  },
  ngay_cap_nhat: {
    type: Date,
    default: Date.now
  },
  da_xoa: {
    type: Boolean,
    default: false
  }
}, {
  timestamps: true
});

// Indexes for performance
chatSchema.index({ nguoi_dung_id: 1, ngay_tao: -1 });
chatSchema.index({ admin_id: 1, ngay_tao: -1 });
chatSchema.index({ trang_thai: 1 });
chatSchema.index({ ngay_tao: -1 });

// Virtual for formatted date
chatSchema.virtual('ngay_tao_format').get(function() {
  return this.ngay_tao.toLocaleString('vi-VN');
});

// Methods
chatSchema.methods.toClientObject = function() {
  return {
    id: this._id,
    nguoi_dung_id: this.nguoi_dung_id,
    admin_id: this.admin_id,
    tin_nhan: this.tin_nhan,
    loai_tin_nhan: this.loai_tin_nhan,
    url_file: this.url_file,
    thong_tin_san_pham: this.thong_tin_san_pham,
    trang_thai: this.trang_thai,
    gui_boi: this.gui_boi,
    ngay_tao: this.ngay_tao,
    ngay_tao_format: this.ngay_tao_format
  };
};

// Static methods
chatSchema.statics.layTinNhanCuaKhachHang = function(nguoiDungId, limit = 50) {
  return this.find({ 
    nguoi_dung_id: nguoiDungId, 
    da_xoa: false 
  })
  .sort({ ngay_tao: -1 })
  .limit(limit)
  .populate('admin_id', 'hoTen email')
  .populate('nguoi_dung_id', 'hoTen email');
};

chatSchema.statics.layTinNhanCuaAdmin = function(adminId, limit = 50) {
  return this.find({ 
    admin_id: adminId, 
    da_xoa: false 
  })
  .sort({ ngay_tao: -1 })
  .limit(limit)
  .populate('nguoi_dung_id', 'hoTen email')
  .populate('admin_id', 'hoTen email');
};

chatSchema.statics.layDanhSachPhongChat = function(adminId) {
  return this.aggregate([
    { $match: { admin_id: new mongoose.Types.ObjectId(adminId), da_xoa: false } },
    { $sort: { ngay_tao: -1 } },
    { $group: {
      _id: '$nguoi_dung_id',
      tin_nhan_moi_nhat: { $first: '$$ROOT' },
      so_tin_nhan_chua_doc: {
        $sum: { $cond: [{ $eq: ['$trang_thai', 'DA_GUI'] }, 1, 0] }
      }
    }},
    { $lookup: {
      from: 'nguoidungs',
      localField: '_id',
      foreignField: '_id',
      as: 'khach_hang'
    }},
    { $unwind: '$khach_hang' },
    { $project: {
      nguoi_dung_id: '$_id',
      hoTen: '$khach_hang.hoTen',
      email: '$khach_hang.email',
      tin_nhan_cuoi: '$tin_nhan_moi_nhat.tin_nhan',
      ngay_tao_cuoi: '$tin_nhan_moi_nhat.ngay_tao',
      so_tin_nhan_chua_doc: '$so_tin_nhan_chua_doc'
    }},
    { $sort: { ngay_tao_cuoi: -1 } }
  ]);
};

chatSchema.statics.danhDauDaDoc = function(nguoiDungId, adminId) {
  return this.updateMany(
    { 
      nguoi_dung_id: nguoiDungId, 
      admin_id: adminId, 
      trang_thai: 'DA_GUI',
      gui_boi: 'KHACH_HANG'
    },
    { trang_thai: 'DA_DOC' }
  );
};

const Chat = mongoose.model('Chat', chatSchema);

module.exports = Chat;
