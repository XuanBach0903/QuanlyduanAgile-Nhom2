const mongoose = require('mongoose');

const hoaDonSchema = new mongoose.Schema({
  ma_hoa_don: {
    type: String,
    required: true,
    unique: true,
    default: function() {
      return 'HD' + Date.now() + Math.random().toString(36).substr(2, 5).toUpperCase();
    }
  },
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
  khach_hang: {
    ho_ten: {
      type: String,
      required: true,
      trim: true
    },
    email: {
      type: String,
      required: true,
      trim: true
    },
    so_dien_thoai: {
      type: String,
      required: true,
      trim: true
    },
    dia_chi: {
      type: String,
      required: true,
      trim: true
    }
  },
  chi_tiet_hoa_don: [{
    san_pham_id: {
      type: mongoose.Schema.Types.ObjectId,
      ref: 'SanPham',
      required: true
    },
    ten_san_pham: {
      type: String,
      required: true
    },
    hinh_anh: {
      type: String
    },
    so_luong: {
      type: Number,
      required: true,
      min: 1
    },
    don_gia: {
      type: Number,
      required: true,
      min: 0
    },
    thanh_tien: {
      type: Number,
      required: true,
      min: 0
    },
    giam_gia: {
      type: Number,
      default: 0,
      min: 0
    },
    thue_vat: {
      type: Number,
      default: 0,
      min: 0
    }
  }],
  thanh_toan: {
    phuong_thuc: {
      type: String,
      enum: ['COD', 'VNPAY', 'VISA', 'PAYPAL'],
      required: true
    },
    trang_thai: {
      type: String,
      enum: ['CHUA_THANH_TOAN', 'DA_THANH_TOAN', 'DANG_XU_LY', 'THAT_BAI'],
      default: 'CHUA_THANH_TOAN'
    },
    ngay_thanh_toan: {
      type: Date
    },
    ma_giao_dich: {
      type: String
    }
  },
  tong_tien_hang: {
    type: Number,
    required: true,
    min: 0
  },
  tong_giam_gia: {
    type: Number,
    default: 0,
    min: 0
  },
  tong_thue_vat: {
    type: Number,
    default: 0,
    min: 0
  },
  tong_cong: {
    type: Number,
    required: true,
    min: 0
  },
  thong_tin_bo_sung: {
    ghi_chu: {
      type: String,
      trim: true,
      maxlength: 500
    },
    ma_khuyen_mai: {
      type: String,
      trim: true
    },
    phi_giao_hang: {
      type: Number,
      default: 0,
      min: 0
    }
  },
  trang_thai_hoa_don: {
    type: String,
    enum: ['MOI_TAO', 'DA_XUAT', 'DA_HUY', 'DA_THANH_TOAN'],
    default: 'MOI_TAO'
  },
  ngay_tao: {
    type: Date,
    default: Date.now
  },
  ngay_cap_nhat: {
    type: Date,
    default: Date.now
  },
  ngay_xuat_hoa_don: {
    type: Date
  },
  nguoi_xuat: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'NguoiDung'
  }
}, {
  timestamps: true,
  collection: 'hoa_don'
});

// Indexes for performance
hoaDonSchema.index({ ma_hoa_don: 1 });
hoaDonSchema.index({ don_hang_id: 1 });
hoaDonSchema.index({ nguoi_dung_id: 1 });
hoaDonSchema.index({ ngay_tao: -1 });
hoaDonSchema.index({ trang_thai_hoa_don: 1 });
hoaDonSchema.index({ 'thanh_toan.trang_thai': 1 });

// Virtual fields
hoaDonSchema.virtual('da_thanh_toan').get(function() {
  return this.thanh_toan.trang_thai === 'DA_THANH_TOAN';
});

hoaDonSchema.virtual('ngay_tao_format').get(function() {
  return this.ngay_tao.toLocaleString('vi-VN');
});

// Methods
hoaDonSchema.methods.taoChiTietHoaDon = function(donHang) {
  this.chi_tiet_hoa_don = donHang.items.map(item => ({
    san_pham_id: item.san_pham_id,
    ten_san_pham: item.ten_san_pham || item.san_pham_id,
    hinh_anh: item.hinh_anh,
    so_luong: item.so_luong,
    don_gia: item.don_gia,
    thanh_tien: item.so_luong * item.don_gia,
    giam_gia: item.giam_gia || 0,
    thue_vat: 0 // VAT 10% có thể thêm sau
  }));

  // Tính toán tổng tiền
  this.tong_tien_hang = this.chi_tiet_hoa_don.reduce((sum, item) => sum + item.thanh_tien, 0);
  this.tong_giam_gia = this.chi_tiet_hoa_don.reduce((sum, item) => sum + item.giam_gia, 0);
  this.tong_thue_vat = this.chi_tiet_hoa_don.reduce((sum, item) => sum + item.thue_vat, 0);
  this.tong_cong = this.tong_tien_hang - this.tong_giam_gia + this.tong_thue_vat + (this.thong_tin_bo_sung.phi_giao_hang || 0);
};

hoaDonSchema.methods.xuatHoaDon = function(nguoiXuatId) {
  this.trang_thai_hoa_don = 'DA_XUAT';
  this.ngay_xuat_hoa_don = new Date();
  this.nguoi_xuat = nguoiXuatId;
  this.ngay_cap_nhat = new Date();
};

hoaDonSchema.methods.thanhToanHoaDon = function(thongTinThanhToan) {
  this.thanh_toan.trang_thai = 'DA_THANH_TOAN';
  this.thanh_toan.ngay_thanh_toan = new Date();
  this.thanh_toan.ma_giao_dich = thongTinThanhToan.ma_giao_dich;
  this.trang_thai_hoa_don = 'DA_THANH_TOAN';
  this.ngay_cap_nhat = new Date();
};

hoaDonSchema.methods.huyHoaDon = function(lyDo) {
  this.trang_thai_hoa_don = 'DA_HUY';
  this.thong_tin_bo_sung.ghi_chu = lyDo || 'Hóa đơn đã bị hủy';
  this.ngay_cap_nhat = new Date();
};

hoaDonSchema.methods.toClientObject = function() {
  return {
    id: this._id,
    ma_hoa_don: this.ma_hoa_don,
    don_hang_id: this.don_hang_id,
    nguoi_dung_id: this.nguoi_dung_id,
    khach_hang: this.khach_hang,
    chi_tiet_hoa_don: this.chi_tiet_hoa_don,
    thanh_toan: this.thanh_toan,
    tong_tien_hang: this.tong_tien_hang,
    tong_giam_gia: this.tong_giam_gia,
    tong_thue_vat: this.tong_thue_vat,
    tong_cong: this.tong_cong,
    thong_tin_bo_sung: this.thong_tin_bo_sung,
    trang_thai_hoa_don: this.trang_thai_hoa_don,
    ngay_tao: this.ngay_tao,
    ngay_cap_nhat: this.ngay_cap_nhat,
    ngay_xuat_hoa_don: this.ngay_xuat_hoa_don,
    nguoi_xuat: this.nguoi_xuat,
    ngay_tao_format: this.ngay_tao_format,
    da_thanh_toan: this.da_thanh_toan
  };
};

// Static methods
hoaDonSchema.statics.taoHoaDonTuDonHang = async function(donHangId, nguoiDungId) {
  const DonHang = mongoose.model('DonHang');
  const NguoiDung = mongoose.model('NguoiDung');
  
  const donHang = await DonHang.findById(donHangId).populate('nguoi_dung_id');
  if (!donHang) {
    throw new Error('Không tìm thấy đơn hàng');
  }

  const nguoiDung = await NguoiDung.findById(nguoiDungId || donHang.nguoi_dung_id);
  if (!nguoiDung) {
    throw new Error('Không tìm thấy thông tin khách hàng');
  }

  // Kiểm tra đã có hóa đơn cho đơn hàng này chưa
  const existingInvoice = await this.findOne({ don_hang_id: donHangId });
  if (existingInvoice) {
    throw new Error('Đơn hàng này đã có hóa đơn');
  }

  const hoaDon = new this({
    don_hang_id: donHangId,
    nguoi_dung_id: nguoiDung._id,
    khach_hang: {
      ho_ten: nguoiDung.hoTen || 'Khách hàng',
      email: nguoiDung.email || '',
      so_dien_thoai: nguoiDung.soDienThoai || '',
      dia_chi: donHang.dia_chi_giao_hang || ''
    },
    thanh_toan: {
      phuong_thuc: donHang.phuong_thuc_thanh_toan,
      trang_thai: donHang.trang_thai_thanh_toan === 'DA_THANH_TOAN' ? 'DA_THANH_TOAN' : 'CHUA_THANH_TOAN',
      ngay_thanh_toan: donHang.trang_thai_thanh_toan === 'DA_THANH_TOAN' ? new Date() : null
    },
    thong_tin_bo_sung: {
      ghi_chu: donHang.ghi_chu || '',
      phi_giao_hang: 0 // Có thể tính sau
    }
  });

  hoaDon.taoChiTietHoaDon(donHang);
  await hoaDon.save();

  return hoaDon;
};

hoaDonSchema.statics.layDanhSachHoaDon = function(nguoiDungId, options = {}) {
  const { page = 1, limit = 10, trangThai, tuNgay, denNgay } = options;
  const skip = (page - 1) * limit;
  
  const query = { nguoi_dung_id: nguoiDungId };
  
  if (trangThai) {
    query.trang_thai_hoa_don = trangThai;
  }
  
  if (tuNgay || denNgay) {
    query.ngay_tao = {};
    if (tuNgay) query.ngay_tao.$gte = new Date(tuNgay);
    if (denNgay) query.ngay_tao.$lte = new Date(denNgay);
  }

  return this.find(query)
    .populate('don_hang_id', 'ma_don_hang trang_thai_don_hang')
    .sort({ ngay_tao: -1 })
    .skip(skip)
    .limit(limit);
};

hoaDonSchema.statics.layThongKeHoaDon = function(nguoiDungId, options = {}) {
  const { tuNgay, denNgay } = options;
  
  const matchStage = { nguoi_dung_id: new mongoose.Types.ObjectId(nguoiDungId) };
  
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
        tong_hoa_don: { $sum: 1 },
        tong_tien: { $sum: '$tong_cong' },
        hoa_don_da_thanh_toan: {
          $sum: { $cond: [{ $eq: ['$thanh_toan.trang_thai', 'DA_THANH_TOAN'] }, 1, 0] }
        },
        hoa_don_chua_thanh_toan: {
          $sum: { $cond: [{ $eq: ['$thanh_toan.trang_thai', 'CHUA_THANH_TOAN'] }, 1, 0] }
        },
        trung_binh_tien: { $avg: '$tong_cong' }
      }
    }
  ]);
};

// Pre-save middleware
hoaDonSchema.pre('save', function(next) {
  this.ngay_cap_nhat = new Date();
  next();
});

const HoaDon = mongoose.model('HoaDon', hoaDonSchema);

module.exports = HoaDon;
