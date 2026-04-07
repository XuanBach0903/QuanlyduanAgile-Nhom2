const XacNhanGiaoHang = require('../models/XacNhanGiaoHang');
const DonHang = require('../models/DonHang');
const NguoiDung = require('../models/NguoiDung');

// Tạo yêu cầu xác nhận giao hàng
async function taoYeuCauXacNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { don_hang_id, nguoi_nhan_hang } = req.body;

    if (!don_hang_id) {
      return res.status(400).json({ message: 'don_hang_id là bắt buộc' });
    }

    // Kiểm tra đơn hàng tồn tại và thuộc về user
    const donHang = await DonHang.findOne({ 
      _id: don_hang_id, 
      nguoi_dung_id: userId 
    });

    if (!donHang) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Chỉ tạo yêu cầu xác nhận cho đơn hàng đang giao
    if (donHang.trang_thai_don_hang !== 'DANG_GIAO') {
      return res.status(400).json({ message: 'Chỉ tạo yêu cầu xác nhận cho đơn hàng đang giao' });
    }

    // Tạo yêu cầu xác nhận
    const xacNhan = await XacNhanGiaoHang.taoYeuCauXacNhan(don_hang_id, userId, nguoi_nhan_hang);

    // Populate thông tin
    const populatedXacNhan = await XacNhanGiaoHang.findById(xacNhan._id)
      .populate('don_hang_id', 'ma_don_hang tong_tien ngay_tao items')
      .populate('nguoi_dung_id', 'hoTen email soDienThoai');

    res.status(201).json({
      success: true,
      message: 'Tạo yêu cầu xác nhận thành công',
      data: populatedXacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Xác nhận giao hàng
async function xacNhanGiaoHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { xac_nhan_id } = req.params;
    const { thong_tin_giao_hang } = req.body;

    if (!xac_nhan_id) {
      return res.status(400).json({ message: 'xac_nhan_id là bắt buộc' });
    }

    const xacNhan = await XacNhanGiaoHang.findOne({ 
      _id: xac_nhan_id, 
      nguoi_dung_id: userId 
    });

    if (!xacNhan) {
      return res.status(404).json({ message: 'Không tìm thấy yêu cầu xác nhận' });
    }

    // Xác nhận giao hàng
    xacNhan.xacNhanGiaoHang(userId, thong_tin_giao_hang);
    await xacNhan.save();

    // Cập nhật trạng thái đơn hàng
    await DonHang.findByIdAndUpdate(xacNhan.don_hang_id, {
      trang_thai_don_hang: 'DA_GIAO_CHO_XAC_NHAN'
    });

    // Populate thông tin
    const populatedXacNhan = await XacNhanGiaoHang.findById(xacNhan._id)
      .populate('don_hang_id', 'ma_don_hang trang_thai_don_hang')
      .populate('nguoi_dung_id', 'hoTen email');

    res.json({
      success: true,
      message: 'Xác nhận giao hàng thành công',
      data: populatedXacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Từ chối giao hàng
async function tuChoiGiaoHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { xac_nhan_id } = req.params;
    const { ly_do } = req.body;

    if (!xac_nhan_id) {
      return res.status(400).json({ message: 'xac_nhan_id là bắt buộc' });
    }

    const xacNhan = await XacNhanGiaoHang.findOne({ 
      _id: xac_nhan_id, 
      nguoi_dung_id: userId 
    });

    if (!xacNhan) {
      return res.status(404).json({ message: 'Không tìm thấy yêu cầu xác nhận' });
    }

    // Từ chối giao hàng
    xacNhan.tuChoiGiaoHang(lyDo);
    await xacNhan.save();

    // Cập nhật trạng thái đơn hàng về trạng thái cũ
    await DonHang.findByIdAndUpdate(xacNhan.don_hang_id, {
      trang_thai_don_hang: 'CHO_XAC_NHAN' // Hoặc trạng thái phù hợp khác
    });

    res.json({
      success: true,
      message: 'Từ chối giao hàng thành công',
      data: xacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Hoàn tất giao hàng (cho shipper)
async function hoanTatGiaoHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { xac_nhan_id } = req.params;

    if (!xac_nhan_id) {
      return res.status(400).json({ message: 'xac_nhan_id là bắt buộc' });
    }

    // Kiểm tra quyền shipper
    const user = await NguoiDung.findById(userId);
    if (!user || user.vai_tro !== 'SHIPPER') {
      return res.status(403).json({ message: 'Chỉ shipper mới có quyền hoàn tất giao hàng' });
    }

    const xacNhan = await XacNhanGiaoHang.findOne({ 
      _id: xac_nhan_id,
      nguoi_giao_hang: userId
    });

    if (!xacNhan) {
      return res.status(404).json({ message: 'Không tìm thấy yêu cầu xác nhận hoặc bạn không phải người giao hàng' });
    }

    // Hoàn tất giao hàng
    xacNhan.hoanTatGiaoHang();
    await xacNhan.save();

    // Cập nhật trạng thái đơn hàng
    await DonHang.findByIdAndUpdate(xacNhan.don_hang_id, {
      trang_thai_don_hang: 'DA_GIAO_CHO_XAC_NHAN'
    });

    res.json({
      success: true,
      message: 'Hoàn tất giao hàng thành công',
      data: xacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Đánh giá giao hàng
async function danhGiaGiaoHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { xac_nhan_id } = req.params;
    const { so_sao, noi_dung } = req.body;

    if (!xac_nhan_id) {
      return res.status(400).json({ message: 'xac_nhan_id là bắt buộc' });
    }

    if (!so_sao || so_sao < 1 || so_sao > 5) {
      return res.status(400).json({ message: 'Số sao phải từ 1 đến 5' });
    }

    const xacNhan = await XacNhanGiaoHang.findOne({ 
      _id: xac_nhan_id, 
      nguoi_dung_id: userId 
    });

    if (!xacNhan) {
      return res.status(404).json({ message: 'Không tìm thấy yêu cầu xác nhận' });
    }

    // Đánh giá giao hàng
    xacNhan.danhGiaGiaoHang(soSao, noiDung);
    await xacNhan.save();

    res.json({
      success: true,
      message: 'Đánh giá giao hàng thành công',
      data: xacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Lấy danh sách xác nhận của người dùng
async function layDanhSachXacNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const {
      page = 1,
      limit = 10,
      trang_thai,
      tu_ngay,
      den_ngay
    } = req.query;

    const options = {
      page: parseInt(page),
      limit: parseInt(limit),
      trangThai: trang_thai,
      tuNgay: tu_ngay,
      denNgay: den_ngay
    };

    const xacNhans = await XacNhanGiaoHang.layDanhSachXacNhan(userId, options);
    const total = await XacNhanGiaoHang.countDocuments({ nguoi_dung_id: userId });

    res.json({
      success: true,
      data: {
        xac_nhan: xacNhans.map(xn => xn.toClientObject()),
        pagination: {
          current: parseInt(page),
          total: Math.ceil(total / limit),
          count: total
        }
      }
    });
  } catch (error) {
    next(error);
  }
}

// Lấy chi tiết xác nhận
async function layChiTietXacNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { xac_nhan_id } = req.params;

    if (!xac_nhan_id) {
      return res.status(400).json({ message: 'xac_nhan_id là bắt buộc' });
    }

    const xacNhan = await XacNhanGiaoHang.findOne({ 
      _id: xac_nhan_id, 
      nguoi_dung_id: userId 
    })
    .populate('don_hang_id', 'ma_don_hamg tong_tien ngay_tao items dia_chi_giao_hang')
    .populate('nguoi_dung_id', 'hoTen email soDienThoai')
    .populate('nguoi_giao_hang', 'hoTen email soDienThoai');

    if (!xacNhan) {
      return res.status(404).json({ message: 'Không tìm thấy yêu cầu xác nhận' });
    }

    res.json({
      success: true,
      data: xacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Shipper: Lấy danh sách chờ xác nhận
async function layDanhSachChoXacNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { page = 1, limit = 10 } = req.query;

    // Kiểm tra quyền shipper
    const user = await NguoiDung.findById(userId);
    if (!user || user.vai_tro !== 'SHIPPER') {
      return res.status(403).json({ message: 'Chỉ shipper mới có quyền xem danh sách chờ xác nhận' });
    }

    const options = {
      page: parseInt(page),
      limit: parseInt(limit)
    };

    const xacNhans = await XacNhanGiaoHang.layDanhSachChoXacNhan(options);

    res.json({
      success: true,
      data: xacNhans.map(xn => xn.toClientObject())
    });
  } catch (error) {
    next(error);
  }
}

// Shipper: Nhận giao hàng
async function nhanGiaoHang(req, res, next) {
  try {
    const userId = req.user.id;
    const { xac_nhan_id } = req.params;

    if (!xac_nhan_id) {
      return res.status(400).json({ message: 'xac_nhan_id là bắt buộc' });
    }

    // Kiểm tra quyền shipper
    const user = await NguoiDung.findById(userId);
    if (!user || user.vai_tro !== 'SHIPPER') {
      return res.status(403).json({ message: 'Chỉ shipper mới có quyền nhận giao hàng' });
    }

    const xacNhan = await XacNhanGiaoHang.findById(xac_nhan_id);
    if (!xacNhan) {
      return res.status(404).json({ message: 'Không tìm thấy yêu cầu xác nhận' });
    }

    if (xacNhan.trang_thai_xac_nhan !== 'CHO_XAC_NHAN') {
      return res.status(400).json({ message: 'Yêu cầu xác nhận không ở trạng thái chờ' });
    }

    // Gán shipper cho yêu cầu
    xacNhan.nguoi_giao_hang = userId;
    xacNhan.thong_tin_giao_hang.thoi_gian_bat_dau_giao = new Date();
    await xacNhan.save();

    // Cập nhật trạng thái đơn hàng
    await DonHang.findByIdAndUpdate(xacNhan.don_hang_id, {
      trang_thai_don_hang: 'DANG_GIAO'
    });

    res.json({
      success: true,
      message: 'Nhận giao hàng thành công',
      data: xacNhan.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Admin: Lấy thống kê xác nhận
async function layThongKeXacNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { nguoi_giao_hang_id, tu_ngay, den_ngay } = req.query;

    // Kiểm tra quyền admin
    const user = await NguoiDung.findById(userId);
    if (!user || user.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền xem thống kê' });
    }

    const options = {
      tuNgay: tu_ngay,
      denNgay: den_ngay
    };

    const stats = await XacNhanGiaoHang.layThongKeXacNhan(nguoi_giao_hang_id, options);

    res.json({
      success: true,
      data: stats[0] || {
        tong_yeu_cau: 0,
        da_xac_nhan: 0,
        da_tu_choi: 0,
        cho_xac_nhan: 0,
        trung_binh_thoi_gian_xac_nhan: 0,
        trung_binh_danh_gia: 0
      }
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  taoYeuCauXacNhan,
  xacNhanGiaoHang,
  tuChoiGiaoHang,
  hoanTatGiaoHang,
  danhGiaGiaoHang,
  layDanhSachXacNhan,
  layChiTietXacNhan,
  layDanhSachChoXacNhan,
  nhanGiaoHang,
  layThongKeXacNhan
};
