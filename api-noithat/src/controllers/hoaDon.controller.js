const HoaDon = require('../models/HoaDon');
const DonHang = require('../models/DonHang');
const NguoiDung = require('../models/NguoiDung');
const SanPham = require('../models/SanPham');

// Tạo hóa đơn từ đơn hàng
async function taoHoaDon(req, res, next) {
  try {
    const userId = req.user.id;
    const { don_hang_id } = req.body;

    if (!don_hang_id) {
      return res.status(400).json({ message: 'don_hang_id là bắt buộc' });
    }

    // Kiểm tra đơn hàng tồn tại và thuộc về user
    const donHang = await DonHang.findOne({ 
      _id: don_hang_id, 
      nguoi_dung_id: userId 
    }).populate('items.san_pham_id');

    if (!donHang) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Chỉ tạo hóa đơn cho đơn hàng đã xác nhận
    if (!['DA_XAC_NHAN', 'DANG_GIAO', 'DA_GIAO_CHO_XAC_NHAN', 'THANH_CONG'].includes(donHang.trang_thai_don_hang)) {
      return res.status(400).json({ message: 'Chỉ tạo hóa đơn cho đơn hàng đã xác nhận' });
    }

    // Kiểm tra đã có hóa đơn cho đơn hàng này chưa
    const existingInvoice = await HoaDon.findOne({ don_hang_id: don_hang_id });
    if (existingInvoice) {
      return res.status(400).json({ message: 'Đơn hàng này đã có hóa đơn' });
    }

    // Tạo hóa đơn mới
    const hoaDon = await HoaDon.taoHoaDonTuDonHang(don_hang_id, userId);

    // Populate thông tin chi tiết
    const populatedInvoice = await HoaDon.findById(hoaDon._id)
      .populate('don_hang_id', 'ma_don_hang trang_thai_don_hang ngay_tao')
      .populate('nguoi_dung_id', 'hoTen email soDienThoai')
      .populate('chi_tiet_hoa_don.san_pham_id', 'ten hinh_anh');

    res.status(201).json({
      success: true,
      message: 'Tạo hóa đơn thành công',
      data: populatedInvoice.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Lấy danh sách hóa đơn của người dùng
async function layDanhSachHoaDon(req, res, next) {
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

    const hoaDons = await HoaDon.layDanhSachHoaDon(userId, options);
    const total = await HoaDon.countDocuments({ nguoi_dung_id: userId });

    res.json({
      success: true,
      data: {
        hoa_don: hoaDons.map(hd => hd.toClientObject()),
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

// Lấy chi tiết hóa đơn
async function layChiTietHoaDon(req, res, next) {
  try {
    const userId = req.user.id;
    const { hoa_don_id } = req.params;

    if (!hoa_don_id) {
      return res.status(400).json({ message: 'hoa_don_id là bắt buộc' });
    }

    const hoaDon = await HoaDon.findOne({ 
      _id: hoa_don_id, 
      nguoi_dung_id: userId 
    })
    .populate('don_hang_id', 'ma_don_hang trang_thai_don_hang ngay_tao items')
    .populate('nguoi_dung_id', 'hoTen email soDienThoai diaChi')
    .populate('chi_tiet_hoa_don.san_pham_id', 'ten hinh_anh moTa')
    .populate('nguoi_xuat', 'hoTen email');

    if (!hoaDon) {
      return res.status(404).json({ message: 'Không tìm thấy hóa đơn' });
    }

    res.json({
      success: true,
      data: hoaDon.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Xuất hóa đơn (chỉ cho admin)
async function xuatHoaDon(req, res, next) {
  try {
    const adminId = req.user.id;
    const { hoa_don_id } = req.params;

    if (!hoa_don_id) {
      return res.status(400).json({ message: 'hoa_don_id là bắt buộc' });
    }

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const hoaDon = await HoaDon.findById(hoa_don_id);
    if (!hoaDon) {
      return res.status(404).json({ message: 'Không tìm thấy hóa đơn' });
    }

    if (hoaDon.trang_thai_hoa_don === 'DA_XUAT') {
      return res.status(400).json({ message: 'Hóa đơn đã được xuất' });
    }

    hoaDon.xuatHoaDon(adminId);
    await hoaDon.save();

    const populatedInvoice = await HoaDon.findById(hoaDon._id)
      .populate('nguoi_xuat', 'hoTen email');

    res.json({
      success: true,
      message: 'Xuất hóa đơn thành công',
      data: populatedInvoice.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Hủy hóa đơn
async function huyHoaDon(req, res, next) {
  try {
    const userId = req.user.id;
    const { hoa_don_id } = req.params;
    const { ly_do } = req.body;

    if (!hoa_don_id) {
      return res.status(400).json({ message: 'hoa_don_id là bắt buộc' });
    }

    const hoaDon = await HoaDon.findOne({ 
      _id: hoa_don_id, 
      nguoi_dung_id: userId 
    });

    if (!hoaDon) {
      return res.status(404).json({ message: 'Không tìm thấy hóa đơn' });
    }

    if (hoaDon.trang_thai_hoa_don === 'DA_XUAT') {
      return res.status(400).json({ message: 'Không thể hủy hóa đơn đã xuất' });
    }

    if (hoaDon.thanh_toan.trang_thai === 'DA_THANH_TOAN') {
      return res.status(400).json({ message: 'Không thể hủy hóa đơn đã thanh toán' });
    }

    hoaDon.huyHoaDon(ly_do);
    await hoaDon.save();

    res.json({
      success: true,
      message: 'Hủy hóa đơn thành công',
      data: hoaDon.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Tải hóa đơn PDF
async function taiHoaDonPDF(req, res, next) {
  try {
    const userId = req.user.id;
    const { hoa_don_id } = req.params;

    if (!hoa_don_id) {
      return res.status(400).json({ message: 'hoa_don_id là bắt buộc' });
    }

    const hoaDon = await HoaDon.findOne({ 
      _id: hoa_don_id, 
      nguoi_dung_id: userId 
    })
    .populate('don_hang_id', 'ma_don_hang')
    .populate('nguoi_dung_id', 'hoTen email soDienThoai diaChi')
    .populate('chi_tiet_hoa_don.san_pham_id', 'ten hinh_anh');

    if (!hoaDon) {
      return res.status(404).json({ message: 'Không tìm thấy hóa đơn' });
    }

    // Trong thực tế, tạo PDF và trả về file
    // Hiện tại trả về URL download
    const pdfUrl = `/api/hoa-don/${hoa_don_id}/pdf`;
    
    res.json({
      success: true,
      message: 'Tạo PDF thành công',
      data: {
        download_url: pdfUrl,
        file_name: `HoaDon_${hoaDon.ma_hoa_don}.pdf`
      }
    });
  } catch (error) {
    next(error);
  }
}

// Lấy thống kê hóa đơn
async function layThongKeHoaDon(req, res, next) {
  try {
    const userId = req.user.id;
    const { tu_ngay, den_ngay } = req.query;

    const options = {
      tuNgay: tu_ngay,
      denNgay: den_ngay
    };

    const stats = await HoaDon.layThongKeHoaDon(userId, options);

    res.json({
      success: true,
      data: stats[0] || {
        tong_hoa_don: 0,
        tong_tien: 0,
        hoa_don_da_thanh_toan: 0,
        hoa_don_chua_thanh_toan: 0,
        trung_binh_tien: 0
      }
    });
  } catch (error) {
    next(error);
  }
}

// Admin: Lấy tất cả hóa đơn
async function layTatCaHoaDon(req, res, next) {
  try {
    const adminId = req.user.id;
    const {
      page = 1,
      limit = 10,
      trang_thai,
      tu_ngay,
      den_ngay,
      tim_kiem
    } = req.query;

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const skip = (page - 1) * limit;
    const query = {};
    
    if (trang_thai) {
      query.trang_thai_hoa_don = trang_thai;
    }
    
    if (tu_ngay || den_ngay) {
      query.ngay_tao = {};
      if (tu_ngay) query.ngay_tao.$gte = new Date(tu_ngay);
      if (den_ngay) query.ngay_tao.$lte = new Date(den_ngay);
    }

    if (tim_kiem) {
      query.$or = [
        { ma_hoa_don: { $regex: tim_kiem, $options: 'i' } },
        { 'khach_hang.ho_ten': { $regex: tim_khem, $options: 'i' } },
        { 'khach_hang.email': { $regex: tim_khem, $options: 'i' } }
      ];
    }

    const hoaDons = await HoaDon.find(query)
      .populate('nguoi_dung_id', 'hoTen email')
      .populate('don_hang_id', 'ma_don_hang')
      .sort({ ngay_tao: -1 })
      .skip(skip)
      .limit(parseInt(limit));

    const total = await HoaDon.countDocuments(query);

    res.json({
      success: true,
      data: {
        hoa_don: hoaDons.map(hd => hd.toClientObject()),
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

// Admin: Tìm kiếm hóa đơn
async function timKiemHoaDon(req, res, next) {
  try {
    const adminId = req.user.id;
    const { keyword } = req.query;

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    if (!keyword) {
      return res.status(400).json({ message: 'Từ khóa tìm kiếm là bắt buộc' });
    }

    const hoaDons = await HoaDon.find({
      $or: [
        { ma_hoa_don: { $regex: keyword, $options: 'i' } },
        { 'khach_hang.ho_ten': { $regex: keyword, $options: 'i' } },
        { 'khach_hang.email': { $regex: keyword, $options: 'i' } },
        { 'khach_hang.so_dien_thoai': { $regex: keyword, $options: 'i' } }
      ]
    })
    .populate('nguoi_dung_id', 'hoTen email')
    .populate('don_hang_id', 'ma_don_hang')
    .sort({ ngay_tao: -1 })
    .limit(20);

    res.json({
      success: true,
      data: hoaDons.map(hd => hd.toClientObject())
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  taoHoaDon,
  layDanhSachHoaDon,
  layChiTietHoaDon,
  xuatHoaDon,
  huyHoaDon,
  taiHoaDonPDF,
  layThongKeHoaDon,
  layTatCaHoaDon,
  timKiemHoaDon
};
