const mongoose = require('mongoose');

const xacNhanGiaoHangSchema = new mongoose.Schema({
  don_hang_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'DonHang',
    required: true
  },
  nguoi_dung_id: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'NguoiDung',
    required: true
  },
  nguoi_giao_hang: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'NguoiDung'
  },
  trang_thai_xac_nhan: {
    type: String,
    enum: ['CHO_XAC_NHAN', 'DA_XAC_NHAN', 'TU_CHOI'],
    default: 'CHO_XAC_NHAN'
  },
  thoi_gian_xac_nhan: {
    type: Date
  },
  thoi_gian_tu_choi: {
    type: Date
  },
  ly_do_tu_choi: {
    type: String,
    trim: true,
    maxlength: 500
  },
  hinh_anh_xac_nhan: [{
    type: String,
    validate: {
      validator: function(v) {
        return v.length <= 5; // Tối đa 5 hình ảnh
      },
      message: 'Không thể tải lên quá 5 hình ảnh'
    }
  }],
  dia_chi_giao_hang_thuc_te: {
    type: String,
    trim: true
  },
  nguoi_nhan_hang: {
    ten: {
      type: String,
      required: true,
      trim: true
    },
    so_dien_thoai: {
      type: String,
      required: true,
      trim: true
    },
    ghi_chu: {
      type: String,
      trim: true,
      maxlength: 200
    }
  },
  thong_tin_giao_hang: {
    thoi_gian_bat_dau_giao: {
      type: Date
    },
    thoi_gian_hoan_tat_giao: {
      type: Date
    },
    khoang_cach: {
      type: Number, // km
      min: 0
    },
    phi_giao_hang: {
      type: Number,
      min: 0
    },
    phuong_tien_giao_hang: {
      type: String,
      enum: ['XE_MAY', 'O_TO', 'XE_BAGAC', 'BO_THUNG'],
      default: 'XE_MAY'
    }
  },
  danh_gia_giao_hang: {
    so_sao: {
      type: Number,
      min: 1,
      max: 5
    },
    noi_dung: {
      type: String,
      trim: true,
      maxlength: 500
    },
    ngay_danh_gia: {
      type: Date
    }
  },
  trang_thai_don_hang_cu: {
    type: String,
    required: true
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
  collection: 'xac_nhan_giao_hang'
});

// Indexes for performance
xacNhanGiaoHangSchema.index({ don_hang_id: 1 });
xacNhanGiaoHangSchema.index({ nguoi_dung_id: 1 });
xacNhanGiaoHangSchema.index({ trang_thai_xac_nhan: 1 });
xacNhanGiaoHangSchema.index({ ngay_tao: -1 });
xacNhanGiaoHangSchema.index({ nguoi_giao_hang: 1 });

// Virtual fields
xacNhanGiaoHangSchema.virtual('da_xac_nhan').get(function() {
  return this.trang_thai_xac_nhan === 'DA_XAC_NHAN';
});

xacNhanGiaoHangSchema.virtual('da_tu_choi').get(function() {
  return this.trang_thai_xac_nhan === 'TU_CHOI';
});

xacNhanGiaoHangSchema.virtual('ngay_tao_format').get(function() {
  return this.ngay_tao.toLocaleString('vi-VN');
});

xacNhanGiaoHangSchema.virtual('thoi_gian_cho_xac_nhan').get(function() {
  if (this.thoi_gian_xac_nhan) {
    return this.thoi_gian_xac_nhan - this.ngay_tao;
  }
  return Date.now() - this.ngay_tao;
});

// Methods
xacNhanGiaoHangSchema.methods.xacNhanGiaoHang = function(nguoiGiaoHangId, thongTinGiaoHang) {
  if (this.trang_thai_xac_nhan !== 'CHO_XAC_NHAN') {
    throw new Error('Chỉ xác nhận đơn hàng đang chờ xác nhận');
  }

  this.trang_thai_xac_nhan = 'DA_XAC_NHAN';
  this.nguoi_giao_hang = nguoiGiaoHangId;
  this.thoi_gian_xac_nhan = new Date();
  this.ngay_cap_nhat = new Date();

  if (thongTinGiaoHang) {
    if (thongTinGiaoHang.thoi_gian_bat_dau_giao) {
      this.thong_tin_giao_hang.thoi_gian_bat_dau_giao = thongTinGiaoHang.thoi_gian_bat_dau_giao;
    }
    if (thongTinGiaoHang.phuong_tien_giao_hang) {
      this.thong_tin_giao_hang.phuong_tien_giao_hang = thongTinGiaoHang.phuong_tien_giao_hang;
    }
    if (thongTinGiaoHang.phi_giao_hang !== undefined) {
      this.thong_tin_giao_hang.phi_giao_hang = thongTinGiaoHang.phi_giao_hang;
    }
  }
};

xacNhanGiaoHangSchema.methods.tuChoiGiaoHang = function(lyDo) {
  if (this.trang_thai_xac_nhan !== 'CHO_XAC_NHAN') {
    throw new Error('Chỉ từ chối đơn hàng đang chờ xác nhận');
  }

  this.trang_thai_xac_nhan = 'TU_CHOI';
  this.ly_do_tu_choi = lyDo || 'Người dùng từ chối nhận hàng';
  this.thoi_gian_tu_choi = new Date();
  this.ngay_cap_nhat = new Date();
};

xacNhanGiaoHangSchema.methods.hoanTatGiaoHang = function() {
  if (this.trang_thai_xac_nhan !== 'DA_XAC_NHAN') {
    throw new Error('Chỉ hoàn tất giao hàng cho đơn hàng đã xác nhận');
  }

  this.thong_tin_giao_hang.thoi_gian_hoan_tat_giao = new Date();
  this.ngay_cap_nhat = new Date();
};

xacNhanGiaoHangSchema.methods.danhGiaGiaoHang = function(soSao, noiDung) {
  if (this.trang_thai_xac_nhan !== 'DA_XAC_NHAN') {
    throw new Error('Chỉ đánh giá giao hàng đã hoàn tất');
  }

  if (!this.thong_tin_giao_hang.thoi_gian_hoan_tat_giao) {
    throw new Error('Chỉ đánh giá sau khi giao hàng hoàn tất');
  }

  this.danh_gia_giao_hang = {
    so_sao: soSao,
    noi_dung: noiDung,
    ngay_danh_gia: new Date()
  };
  this.ngay_cap_nhat = new Date();
};

xacNhanGiaoHangSchema.methods.toClientObject = function() {
  return {
    id: this._id,
    don_hang_id: this.don_hang_id,
    nguoi_dung_id: this.nguoi_dung_id,
    nguoi_giao_hang: this.nguoi_giao_hang,
    trang_thai_xac_nhan: this.trang_thai_xac_nhan,
    thoi_gian_xac_nhan: this.thoi_gian_xac_nhan,
    thoi_gian_tu_choi: this.thoi_gian_tu_choi,
    ly_do_tu_choi: this.ly_do_tu_choi,
    hinh_anh_xac_nhan: this.hinh_anh_xac_nhan,
    dia_chi_giao_hang_thuc_te: this.dia_chi_giao_hang_thuc_te,
    nguoi_nhan_hang: this.nguoi_nhan_hang,
    thong_tin_giao_hang: this.thong_tin_giao_hang,
    danh_gia_giao_hang: this.danh_gia_giao_hang,
    trang_thai_don_hang_cu: this.trang_thai_don_hang_cu,
    ngay_tao: this.ngay_tao,
    ngay_cap_nhat: this.ngay_cap_nhat,
    ngay_tao_format: this.ngay_tao_format,
    da_xac_nhan: this.da_xac_nhan,
    da_tu_choi: this.da_tu_choi,
    thoi_gian_cho_xac_nhan: this.thoi_gian_cho_xac_nhan
  };
};

// Static methods
xacNhanGiaoHangSchema.statics.taoYeuCauXacNhan = function(donHangId, nguoiDungId, nguoiNhanHang) {
  const DonHang = mongoose.model('DonHang');
  
  return DonHang.findById(donHangId).then(donHang => {
    if (!donHang) {
      throw new Error('Không tìm thấy đơn hàng');
    }

    if (donHang.trang_thai_don_hang !== 'DANG_GIAO') {
      throw new Error('Chỉ tạo yêu cầu xác nhận cho đơn hàng đang giao');
    }

    // Kiểm tra đã có yêu cầu xác nhận chưa
    return this.findOne({ don_hang_id: donHangId }).then(existing => {
      if (existing) {
        throw new Error('Đơn hàng này đã có yêu cầu xác nhận');
      }

      const xacNhan = new this({
        don_hang_id: donHangId,
        nguoi_dung_id: nguoiDungId,
        trang_thai_don_hang_cu: donHang.trang_thai_don_hang,
        nguoi_nhan_hang: nguoiNhanHang || {
          ten: 'Người nhận',
          so_dien_thoai: '',
          ghi_chu: ''
        }
      });

      return xacNhan.save();
    });
  });
};

xacNhanGiaoHangSchema.statics.layDanhSachXacNhan = function(nguoiDungId, options = {}) {
  const { page = 1, limit = 10, trangThai, tuNgay, denNgay } = options;
  const skip = (page - 1) * limit;
  
  const query = { nguoi_dung_id: nguoiDungId };
  
  if (trangThai) {
    query.trang_thai_xac_nhan = trangThai;
  }
  
  if (tuNgay || denNgay) {
    query.ngay_tao = {};
    if (tuNgay) query.ngay_tao.$gte = new Date(tuNgay);
    if (denNgay) query.ngay_tao.$lte = new Date(denNgay);
  }

  return this.find(query)
    .populate('don_hang_id', 'ma_don_hang tong_tien ngay_tao')
    .populate('nguoi_giao_hang', 'hoTen email soDienThoai')
    .sort({ ngay_tao: -1 })
    .skip(skip)
    .limit(limit);
};

xacNhanGiaoHangSchema.statics.layDanhSachChoXacNhan = function(options = {}) {
  const { page = 1, limit = 10 } = options;
  const skip = (page - 1) * limit;

  return this.find({ trang_thai_xac_nhan: 'CHO_XAC_NHAN' })
    .populate('don_hang_id', 'ma_don_hang tong_tien dia_chi_giao_hang')
    .populate('nguoi_dung_id', 'hoTen email soDienThoai')
    .sort({ ngay_tao: -1 })
    .skip(skip)
    .limit(limit);
};

xacNhanGiaoHangSchema.statics.layThongKeXacNhan = function(nguoiGiaoHangId, options = {}) {
  const { tuNgay, denNgay } = options;
  
  const matchStage = {};
  
  if (nguoiGiaoHangId) {
    matchStage.nguoi_giao_hang = new mongoose.Types.ObjectId(nguoiGiaoHangId);
  }
  
  if (tuNgay || denNgay) {
    matchStage.ngay_tao = {};
    if (tuNgay) matchStage.ngay_tao.$gte = new Date(tuNgay);
    if (denNgay) matchStage.ngay_tao.$lte = new Date(denNgay);
  }

  return this.aggregate([
    { $match: matchStage },
    {
      $group: {
        _id: null,
        tong_yeu_cau: { $sum: 1 },
        da_xac_nhan: {
          $sum: { $cond: [{ $eq: ['$trang_thai_xac_nhan', 'DA_XAC_NHAN'] }, 1, 0] }
        },
        da_tu_choi: {
          $sum: { $cond: [{ $eq: ['$trang_thai_xac_nhan', 'TU_CHOI'] }, 1, 0] }
        },
        cho_xac_nhan: {
          $sum: { $cond: [{ $eq: ['$trang_thai_xac_nhan', 'CHO_XAC_NHAN'] }, 1, 0] }
        },
        trung_binh_thoi_gian_xac_nhan: {
          $avg: {
            $subtract: ['$thoi_gian_xac_nhan', '$ngay_tao']
          }
        },
        trung_binh_danh_gia: { $avg: '$danh_gia_giao_hang.so_sao' }
      }
    }
  ]);
};

// Pre-save middleware
xacNhanGiaoHangSchema.pre('save', function(next) {
  this.ngay_cap_nhat = new Date();
  next();
});

const XacNhanGiaoHang = mongoose.model('XacNhanGiaoHang', xacNhanGiaoHangSchema);

module.exports = XacNhanGiaoHang;
