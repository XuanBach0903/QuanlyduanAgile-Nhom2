const DanhGia = require('../models/DanhGia');
const DonHang = require('../models/DonHang');
const SanPham = require('../models/SanPham');
const NguoiDung = require('../models/NguoiDung');

// Tạo đánh giá sản phẩm
async function taoDanhGia(req, res, next) {
  try {
    const userId = req.user.id;
    const { san_pham_id, don_hang_id, so_sao, noi_dung, hinh_anh, loai_danh_gia } = req.body;

    if (!san_pham_id || !don_hang_id || !so_sao || !noi_dung) {
      return res.status(400).json({ message: 'san_pham_id, don_hang_id, so_sao, và noi_dung là bắt buộc' });
    }

    if (so_sao < 1 || so_sao > 5) {
      return res.status(400).json({ message: 'Số sao phải từ 1 đến 5' });
    }

    // Kiểm tra đơn hàng tồn tại và thuộc về user
    const donHang = await DonHang.findOne({ 
      _id: don_hang_id, 
      nguoi_dung_id: userId,
      trang_thai_don_hang: 'DA_GIAO_CHO_XAC_NHAN'
    });

    if (!donHang) {
      return res.status(404).json({ message: 'Đơn hàng không tồn tại hoặc chưa được giao' });
    }

    // Kiểm tra sản phẩm có trong đơn hàng không
    const productInOrder = donHang.items.some(item => 
      item.san_pham_id.toString() === san_pham_id
    );

    if (!productInOrder) {
      return res.status(400).json({ message: 'Sản phẩm không có trong đơn hàng' });
    }

    // Kiểm tra đã đánh giá chưa
    const existingReview = await DanhGia.kiemTraDaDanhGia(san_pham_id, userId);
    if (existingReview) {
      return res.status(400).json({ message: 'Bạn đã đánh giá sản phẩm này rồi' });
    }

    // Tạo đánh giá mới
    const danhGia = await DanhGia.create({
      san_pham_id: san_pham_id,
      nguoi_dung_id: userId,
      don_hang_id: don_hang_id,
      so_sao: so_sao,
      noi_dung: noi_dung.trim(),
      hinh_anh: hinh_anh || [],
      loai_danh_gia: loai_danh_gia || 'SAN_PHAM'
    });

    // Populate thông tin
    const populatedReview = await DanhGia.findById(danhGia._id)
      .populate('nguoi_dung_id', 'hoTen email hinh_anh')
      .populate('san_pham_id', 'ten hinh_anh gia')
      .populate('don_hang_id', 'ma_don_hang ngay_tao');

    res.status(201).json({
      success: true,
      message: 'Tạo đánh giá thành công',
      data: populatedReview.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Lấy danh sách đánh giá của sản phẩm
async function layDanhGiaSanPham(req, res, next) {
  try {
    const { san_pham_id } = req.params;
    const {
      page = 1,
      limit = 10,
      sao_filter,
      co_hinh_anh,
      co_phan_hoi
    } = req.query;

    if (!san_pham_id) {
      return res.status(400).json({ message: 'san_pham_id là bắt buộc' });
    }

    const options = {
      page: parseInt(page),
      limit: parseInt(limit),
      saoFilter: sao_filter ? parseInt(sao_filter) : null,
      coHinhAnh: co_hinh_anh === 'true',
      coPhanHoi: co_phan_hoi === 'true'
    };

    const danhGias = await DanhGia.layDanhGiaCuaSanPham(san_pham_id, options);
    const thongKe = await DanhGia.layThongKeDanhGia(san_pham_id);

    res.json({
      success: true,
      data: {
        danh_gia: danhGias.map(dg => dg.toClientObject()),
        thong_ke: thongKe[0] || {
          danh_gia_theo_sao: [],
          tong_danh_gia: 0,
          trung_binh_sao: 0
        },
        pagination: {
          current: parseInt(page),
          limit: parseInt(limit)
        }
      }
    });
  } catch (error) {
    next(error);
  }
}

// Lấy danh sách đánh giá của người dùng
async function layDanhGiaNguoiDung(req, res, next) {
  try {
    const userId = req.user.id;
    const { page = 1, limit = 10 } = req.query;

    const options = {
      page: parseInt(page),
      limit: parseInt(limit)
    };

    const danhGias = await DanhGia.layDanhGiaCuaNguoiDung(userId, options);

    res.json({
      success: true,
      data: {
        danh_gia: danhGias.map(dg => dg.toClientObject()),
        pagination: {
          current: parseInt(page),
          limit: parseInt(limit)
        }
      }
    });
  } catch (error) {
    next(error);
  }
}

// Cập nhật đánh giá
async function capNhatDanhGia(req, res, next) {
  try {
    const userId = req.user.id;
    const { danh_gia_id } = req.params;
    const { so_sao, noi_dung, hinh_anh } = req.body;

    if (!danh_gia_id) {
      return res.status(400).json({ message: 'danh_gia_id là bắt buộc' });
    }

    if (so_sao && (so_sao < 1 || so_sao > 5)) {
      return res.status(400).json({ message: 'Số sao phải từ 1 đến 5' });
    }

    // Tìm đánh giá của user
    const danhGia = await DanhGia.findOne({
      _id: danh_gia_id,
      nguoi_dung_id: userId,
      da_xoa: false
    });

    if (!danhGia) {
      return res.status(404).json({ message: 'Đánh giá không tồn tại' });
    }

    // Chỉ cho phép cập nhật trong 24h
    const now = new Date();
    const diffHours = (now - danhGia.ngay_tao) / (1000 * 60 * 60);
    if (diffHours > 24) {
      return res.status(400).json({ message: 'Chỉ được chỉnh sửa đánh giá trong vòng 24 giờ' });
    }

    // Cập nhật thông tin
    if (so_sao) danhGia.so_sao = so_sao;
    if (noi_dung) danhGia.noi_dung = noi_dung.trim();
    if (hinh_anh) danhGia.hinh_anh = hinh_anh;

    await danhGia.save();

    // Populate thông tin
    const populatedReview = await DanhGia.findById(danhGia._id)
      .populate('nguoi_dung_id', 'hoTen email hinh_anh')
      .populate('san_pham_id', 'ten hinh_anh gia')
      .populate('don_hang_id', 'ma_don_hang ngay_tao');

    res.json({
      success: true,
      message: 'Cập nhật đánh giá thành công',
      data: populatedReview.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Xóa đánh giá
async function xoaDanhGia(req, res, next) {
  try {
    const userId = req.user.id;
    const { danh_gia_id } = req.params;

    if (!danh_gia_id) {
      return res.status(400).json({ message: 'danh_gia_id là bắt buộc' });
    }

    // Tìm đánh giá của user
    const danhGia = await DanhGia.findOne({
      _id: danh_gia_id,
      nguoi_dung_id: userId,
      da_xoa: false
    });

    if (!danhGia) {
      return res.status(404).json({ message: 'Đánh giá không tồn tại' });
    }

    // Soft delete
    danhGia.da_xoa = true;
    danhGia.ngay_cap_nhat = new Date();
    await danhGia.save();

    res.json({
      success: true,
      message: 'Xóa đánh giá thành công'
    });
  } catch (error) {
    next(error);
  }
}

// Like/unlike đánh giá
async function thichDanhGia(req, res, next) {
  try {
    const userId = req.user.id;
    const { danh_gia_id } = req.params;

    if (!danh_gia_id) {
      return res.status(400).json({ message: 'danh_gia_id là bắt buộc' });
    }

    const danhGia = await DanhGia.findById(danh_gia_id);
    if (!danhGia || danhGia.da_xoa) {
      return res.status(404).json({ message: 'Đánh giá không tồn tại' });
    }

    const isLiked = danhGia.toggleLike(userId);
    await danhGia.save();

    res.json({
      success: true,
      message: isLiked ? 'Đã thích đánh giá' : 'Đã bỏ thích đánh giá',
      data: {
        da_thich: isLiked,
        so_thich: danhGia.so_thich
      }
    });
  } catch (error) {
    next(error);
  }
}

// Báo cáo đánh giá
async function baoCaoDanhGia(req, res, next) {
  try {
    const userId = req.user.id;
    const { danh_gia_id } = req.params;
    const { ly_do } = req.body;

    if (!danh_gia_id) {
      return res.status(400).json({ message: 'danh_gia_id là bắt buộc' });
    }

    if (!ly_do) {
      return res.status(400).json({ message: 'ly_do là bắt buộc' });
    }

    const danhGia = await DanhGia.findById(danh_gia_id);
    if (!danhGia || danhGia.da_xoa) {
      return res.status(404).json({ message: 'Đánh giá không tồn tại' });
    }

    danhGia.addReport(userId, ly_do);
    await danhGia.save();

    res.json({
      success: true,
      message: 'Báo cáo đánh giá thành công'
    });
  } catch (error) {
    next(error);
  }
}

// Admin: Phản hồi đánh giá
async function phanHoiDanhGia(req, res, next) {
  try {
    const adminId = req.user.id;
    const { danh_gia_id } = req.params;
    const { phan_hoi } = req.body;

    if (!danh_gia_id) {
      return res.status(400).json({ message: 'danh_gia_id là bắt buộc' });
    }

    if (!phan_hoi || phan_hoi.trim() === '') {
      return res.status(400).json({ message: 'Nội dung phản hồi không được để trống' });
    }

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const danhGia = await DanhGia.findById(danh_gia_id);
    if (!danhGia || danhGia.da_xoa) {
      return res.status(404).json({ message: 'Đánh giá không tồn tại' });
    }

    danhGia.addShopReply(phan_hoi.trim());
    await danhGia.save();

    // Populate thông tin
    const populatedReview = await DanhGia.findById(danhGia._id)
      .populate('nguoi_dung_id', 'hoTen email hinh_anh')
      .populate('san_pham_id', 'ten hinh_anh gia');

    res.json({
      success: true,
      message: 'Phản hồi đánh giá thành công',
      data: populatedReview.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Admin: Duyệt/Từ chối đánh giá
async function duyetDanhGia(req, res, next) {
  try {
    const adminId = req.user.id;
    const { danh_gia_id } = req.params;
    const { trang_thai, ly_do_tu_choi } = req.body;

    if (!danh_gia_id) {
      return res.status(400).json({ message: 'danh_gia_id là bắt buộc' });
    }

    if (!trang_thai || !['DA_DUYET', 'TU_CHOI'].includes(trang_thai)) {
      return res.status(400).json({ message: 'trang_thai phải là DA_DUYET hoặc TU_CHOI' });
    }

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const danhGia = await DanhGia.findById(danh_gia_id);
    if (!danhGia || danhGia.da_xoa) {
      return res.status(404).json({ message: 'Đánh giá không tồn tại' });
    }

    danhGia.trang_thai = trang_thai;
    if (trang_thai === 'TU_CHOI') {
      danhGia.ly_do_tu_choi = ly_do_tu_choi || 'Đánh giá không phù hợp';
    } else {
      danhGia.ly_do_tu_choi = null;
    }
    danhGia.ngay_cap_nhat = new Date();
    await danhGia.save();

    res.json({
      success: true,
      message: trang_thai === 'DA_DUYET' ? 'Duyệt đánh giá thành công' : 'Từ chối đánh giá thành công',
      data: danhGia.toClientObject()
    });
  } catch (error) {
    next(error);
  }
}

// Admin: Lấy danh sách đánh giá cần duyệt
async function layDanhGiaCanDuyet(req, res, next) {
  try {
    const adminId = req.user.id;
    const { page = 1, limit = 10 } = req.query;

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const options = {
      page: parseInt(page),
      limit: parseInt(limit)
    };

    const danhGias = await DanhGia.layDanhGiaCanDuyet(options);

    res.json({
      success: true,
      data: {
        danh_gia: danhGias.map(dg => dg.toClientObject()),
        pagination: {
          current: parseInt(page),
          limit: parseInt(limit)
        }
      }
    });
  } catch (error) {
    next(error);
  }
}

// Admin: Lấy thống kê đánh giá
async function layThongKeDanhGia(req, res, next) {
  try {
    const adminId = req.user.id;
    const { san_pham_id } = req.query;

    // Kiểm tra admin quyền
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    let thongKe;
    if (san_pham_id) {
      // Thống kê cho một sản phẩm
      thongKe = await DanhGia.layThongKeDanhGia(san_pham_id);
    } else {
      // Thống kê tổng quan
      thongKe = await DanhGia.aggregate([
        { $match: { trang_thai: 'DA_DUYET', da_xoa: false } },
        {
          $group: {
            _id: null,
            tong_danh_gia: { $sum: 1 },
            trung_binh_sao: { $avg: '$so_sao' },
            danh_gia_hom_nay: {
              $sum: {
                $cond: [
                  { $gte: ['$ngay_tao', new Date(new Date().setHours(0, 0, 0, 0))] },
                  1,
                  0
                ]
              }
            }
          }
        }
      ]);
    }

    res.json({
      success: true,
      data: thongKe[0] || {
        tong_danh_gia: 0,
        trung_binh_sao: 0,
        danh_gia_hom_nay: 0
      }
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  taoDanhGia,
  layDanhGiaSanPham,
  layDanhGiaNguoiDung,
  capNhatDanhGia,
  xoaDanhGia,
  thichDanhGia,
  baoCaoDanhGia,
  phanHoiDanhGia,
  duyetDanhGia,
  layDanhGiaCanDuyet,
  layThongKeDanhGia
};
