const mongoose = require('mongoose');

const danhGiaSchema = new mongoose.Schema({
  san_pham_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'SanPham',
    required: true
  },
  nguoi_dung_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'NguoiDung',
    required: true
  },
  don_hang_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'DonHang',
    required: true
  },
  so_sao: {
    type: Number,
    required: true,
    min: 1,
    max: 5
  },
  noi_dung: {
    type: String,
    required: true,
    trim: true,
    maxlength: 1000
  },
  hinh_anh: [{
    type: String,
    validate: {
      validator: function(v) {
        return v.length <= 5; // Tối đa 5 hình ảnh
      },
      message: 'Không thể tải lên quá 5 hình ảnh'
    }
  }],
  loai_danh_gia: {
    type: String,
    enum: ['SAN_PHAM', 'GIAO_HANG', 'DICH_VU'],
    default: 'SAN_PHAM'
  },
  trang_thai: {
    type: String,
    enum: ['CHO_DUYET', 'DA_DUYET', 'TU_CHOI'],
    default: 'DA_DUYET'
  },
  ly_do_tu_choi: {
    type: String,
    trim: true
  },
  phan_hoi_cua_shop: {
    type: String,
    trim: true,
    maxlength: 500
  },
  ngay_tao_phan_hoi: {
    type: Date
  },
  thich: [{
    nguoi_dung_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'NguoiDung'
    },
    ngay_thich: {
      type: Date,
      default: Date.now
    }
  }],
  bao_cao: [{
    nguoi_dung_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'NguoiDung'
    },
    ly_do: {
      type: String,
      enum: ['SPAM', 'KHONG_PHU_HOP', 'SAO_CHEP', 'KHAC'],
      required: true
    },
    ngay_bao_cao: {
      type: Date,
      default: Date.now
    }
  }],
  da_xoa: {
    type: Boolean,
    default: false
  },
  ngay_tao: {
    type: Date,
    default: Date.now
  },
  ngay_cap_nhat: {
    type: Date,
    default: Date.now
  }
}, {
  timestamps: true,
  collection: 'danh_gia'
});

// Indexes for performance
danhGiaSchema.index({ san_pham_id: 1, trang_thai: 1, ngay_tao: -1 });
danhGiaSchema.index({ nguoi_dung_id: 1, san_pham_id: 1 });
danhGiaSchema.index({ don_hang_id: 1 });
danhGiaSchema.index({ so_sao: 1 });
danhGiaSchema.index({ ngay_tao: -1 });
danhGiaSchema.index({ 'thich.nguoi_dung_id': 1 });

// Virtual fields
danhGiaSchema.virtual('so_thich').get(function() {
  return this.thich ? this.thich.length : 0;
});

danhGiaSchema.virtual('so_bao_cao').get(function() {
  return this.bao_cao ? this.bao_cao.length : 0;
});

danhGiaSchema.virtual('ngay_tao_format').get(function() {
  return this.ngay_tao.toLocaleString('vi-VN');
});

// Methods
danhGiaSchema.methods.toClientObject = function() {
  return {
    id: this._id,
    san_pham_id: this.san_pham_id,
    nguoi_dung_id: this.nguoi_dung_id,
    don_hang_id: this.don_hang_id,
    so_sao: this.so_sao,
    noi_dung: this.noi_dung,
    hinh_anh: this.hinh_anh,
    loai_danh_gia: this.loai_danh_gia,
    trang_thai: this.trang_thai,
    phan_hoi_cua_shop: this.phan_hoi_cua_shop,
    ngay_tao_phan_hoi: this.ngay_tao_phan_hoi,
    so_thich: this.so_thich,
    so_bao_cao: this.so_bao_cao,
    da_thich: this.checkUserLiked(),
    ngay_tao: this.ngay_tao,
    ngay_tao_format: this.ngay_tao_format
  };
};

danhGiaSchema.methods.checkUserLiked = function(userId) {
  if (!userId || !this.thich) return false;
  return this.thich.some(like => like.nguoi_dung_id.toString() === userId.toString());
};

danhGiaSchema.methods.toggleLike = function(userId) {
  if (!userId) throw new Error('User ID is required');
  
  const existingLikeIndex = this.thich.findIndex(
    like => like.nguoi_dung_id.toString() === userId.toString()
  );
  
  if (existingLikeIndex > -1) {
    // Unlike
    this.thich.splice(existingLikeIndex, 1);
    return false;
  } else {
    // Like
    this.thich.push({ nguoi_dung_id: userId });
    return true;
  }
};

danhGiaSchema.methods.addReport = function(userId, lyDo) {
  if (!userId || !lyDo) throw new Error('User ID và lý do là bắt buộc');
  
  // Check if user already reported
  const existingReport = this.bao_cao.find(
    report => report.nguoi_dung_id.toString() === userId.toString()
  );
  
  if (existingReport) {
    throw new Error('Bạn đã báo cáo đánh giá này rồi');
  }
  
  this.bao_cao.push({ nguoi_dung_id: userId, ly_do: lyDo });
};

danhGiaSchema.methods.addShopReply = function(phanHoi) {
  if (!phanHoi || phanHoi.trim() === '') {
    throw new Error('Nội dung phản hồi không được để trống');
  }
  
  this.phan_hoi_cua_shop = phanHoi.trim();
  this.ngay_tao_phan_hoi = new Date();
  this.ngay_cap_nhat = new Date();
};

// Static methods
danhGiaSchema.statics.layDanhGiaCuaSanPham = function(sanPhamId, options = {}) {
  const {
    page = 1,
    limit = 10,
    saoFilter = null,
    coHinhAnh = false,
    coPhanHoi = false
  } = options;
  
  const skip = (page - 1) * limit;
  const query = {
    san_pham_id: sanPhamId,
    trang_thai: 'DA_DUYET',
    da_xoa: false
  };
  
  if (saoFilter && saoFilter >= 1 && saoFilter <= 5) {
    query.so_sao = saoFilter;
  }
  
  if (coHinhAnh) {
    query.hinh_anh = { $exists: true, $ne: [] };
  }
  
  if (coPhanHoi) {
    query.phan_hoi_cua_shop = { $exists: true, $ne: null };
  }
  
  return this.find(query)
    .populate('nguoi_dung_id', 'hoTen email hinh_anh')
    .sort({ ngay_tao: -1 })
    .skip(skip)
    .limit(limit);
};

danhGiaSchema.statics.layThongKeDanhGia = function(sanPhamId) {
  return this.aggregate([
    { $match: { san_pham_id: new mongoose.Types.ObjectId(sanPhamId), trang_thai: 'DA_DUYET', da_xoa: false } },
    {
      $group: {
        _id: '$so_sao',
        so_luong: { $sum: 1 }
      }
    },
    { $sort: { _id: 1 } },
    {
      $group: {
        _id: null,
        danh_gia_theo_sao: {
          $push: {
            sao: '$_id',
            so_luong: '$so_luong'
          }
        },
        tong_danh_gia: { $sum: '$so_luong' },
        trung_binh_sao: { $avg: '$_id' }
      }
    }
  ]);
};

danhGiaSchema.statics.kiemTraDaDanhGia = function(sanPhamId, nguoiDungId) {
  return this.findOne({
    san_pham_id: sanPhamId,
    nguoi_dung_id: nguoiDungId,
    da_xoa: false
  });
};

danhGiaSchema.statics.layDanhGiaCuaNguoiDung = function(nguoiDungId, options = {}) {
  const { page = 1, limit = 10 } = options;
  const skip = (page - 1) * limit;
  
  return this.find({
    nguoi_dung_id: nguoiDungId,
    da_xoa: false
  })
  .populate('san_pham_id', 'ten hinh_anh gia')
  .populate('don_hang_id', 'trang_thai_don_hang ngay_tao')
  .sort({ ngay_tao: -1 })
  .skip(skip)
  .limit(limit);
};

danhGiaSchema.statics.layDanhGiaCanDuyet = function(options = {}) {
  const { page = 1, limit = 10 } = options;
  const skip = (page - 1) * limit;
  
  return this.find({
    trang_thai: 'CHO_DUYET',
    da_xoa: false
  })
  .populate('san_pham_id', 'ten hinh_anh')
  .populate('nguoi_dung_id', 'hoTen email')
  .populate('don_hang_id', 'ma_don_hang ngay_tao')
  .sort({ ngay_tao: -1 })
  .skip(skip)
  .limit(limit);
};

// Pre-save middleware
danhGiaSchema.pre('save', function(next) {
  this.ngay_cap_nhat = new Date();
  next();
});

const DanhGia = mongoose.model('DanhGia', danhGiaSchema);

module.exports = DanhGia;
