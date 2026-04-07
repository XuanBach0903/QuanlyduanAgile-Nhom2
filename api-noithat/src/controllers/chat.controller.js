const ChatService = require('../services/ChatService');
const Chat = require('../models/Chat');
const NguoiDung = require('../models/NguoiDung');
const SanPham = require('../models/SanPham');

const chatService = new ChatService();

// Gửi tin nhắn từ khách hàng
async function guiTinNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { tin_nhan, loai_tin_nhan = 'TEXT', san_pham_id } = req.body;

    if (!tin_nhan || tin_nhan.trim() === '') {
      return res.status(400).json({ message: 'Nội dung tin nhắn không được để trống' });
    }

    let thongTinSanPham = null;
    
    // Nếu có thông tin sản phẩm, lấy thông tin sản phẩm
    if (san_pham_id) {
      const sanPham = await SanPham.findById(san_pham_id);
      if (!sanPham) {
        return res.status(404).json({ message: 'Sản phẩm không tồn tại' });
      }

      thongTinSanPham = {
        san_pham_id: sanPham._id,
        ten_san_pham: sanPham.ten,
        hinh_anh: sanPham.hinh_dai_dien || sanPham.hinh_anh[0],
        gia: sanPham.gia
      };
    }

    const message = await chatService.guiTinNhanKhachHang(userId, tin_nhan, loai_tin_nhan, thongTinSanPham);
    
    res.status(201).json({
      success: true,
      message: 'Gửi tin nhắn thành công',
      data: message
    });
  } catch (error) {
    next(error);
  }
}

// Lấy lịch sử chat của khách hàng
async function layLichSuChat(req, res, next) {
  try {
    const userId = req.user.id;
    const { limit = 50 } = req.query;

    const messages = await chatService.layLichSuChatKhachHang(userId, parseInt(limit));
    
    res.json({
      success: true,
      data: messages
    });
  } catch (error) {
    next(error);
  }
}

// Gửi tin nhắn từ admin
async function guiTinNhanAdmin(req, res, next) {
  try {
    const adminId = req.user.id;
    const { nguoi_dung_id, tin_nhan, loai_tin_nhan = 'TEXT', san_pham_id } = req.body;

    if (!nguoi_dung_id) {
      return res.status(400).json({ message: 'ID người dùng là bắt buộc' });
    }

    if (!tin_nhan || tin_nhan.trim() === '') {
      return res.status(400).json({ message: 'Nội dung tin nhắn không được để trống' });
    }

    // Kiểm tra admin có quyền không
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    let thongTinSanPham = null;
    
    // Nếu có thông tin sản phẩm, lấy thông tin sản phẩm
    if (san_pham_id) {
      const sanPham = await SanPham.findById(san_pham_id);
      if (!sanPham) {
        return res.status(404).json({ message: 'Sản phẩm không tồn tại' });
      }

      thongTinSanPham = {
        san_pham_id: sanPham._id,
        ten_san_pham: sanPham.ten,
        hinh_anh: sanPham.hinh_dai_dien || sanPham.hinh_anh[0],
        gia: sanPham.gia
      };
    }

    const message = await chatService.guiTinNhanAdmin(adminId, nguoi_dung_id, tin_nhan, loai_tin_nhan, thongTinSanPham);
    
    res.status(201).json({
      success: true,
      message: 'Gửi tin nhắn thành công',
      data: message
    });
  } catch (error) {
    next(error);
  }
}

// Lấy lịch sử chat của admin
async function layLichSuChatAdmin(req, res, next) {
  try {
    const adminId = req.user.id;
    const { nguoi_dung_id, limit = 50 } = req.query;

    // Kiểm tra admin có quyền không
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    let messages;
    if (nguoi_dung_id) {
      // Lấy chat với một khách hàng cụ thể
      messages = await chatService.layLichSuChatKhachHang(nguoi_dung_id, parseInt(limit));
    } else {
      // Lấy tất cả chat của admin
      messages = await chatService.layLichSuChatAdmin(adminId, parseInt(limit));
    }
    
    res.json({
      success: true,
      data: messages
    });
  } catch (error) {
    next(error);
  }
}

// Lấy danh sách phòng chat của admin
async function layDanhSachPhongChat(req, res, next) {
  try {
    const adminId = req.user.id;

    // Kiểm tra admin có quyền không
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const chatRooms = await chatService.layDanhSachPhongChat(adminId);
    
    res.json({
      success: true,
      data: chatRooms
    });
  } catch (error) {
    next(error);
  }
}

// Gửi thông tin sản phẩm trong chat
async function guiThongTinSanPham(req, res, next) {
  try {
    const userId = req.user.id;
    const { san_pham_id, nguoi_dung_id } = req.body;

    if (!san_pham_id) {
      return res.status(400).json({ message: 'ID sản phẩm là bắt buộc' });
    }

    let message;
    if (nguoi_dung_id) {
      // Admin gửi cho khách hàng
      const admin = await NguoiDung.findById(userId);
      if (!admin || admin.vai_tro !== 'ADMIN') {
        return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
      }
      message = await chatService.guiThongTinSanPham(nguoi_dung_id, san_pham_id, userId);
    } else {
      // Khách hàng gửi
      message = await chatService.guiThongTinSanPham(userId, san_pham_id);
    }
    
    res.json({
      success: true,
      message: 'Gửi thông tin sản phẩm thành công',
      data: message
    });
  } catch (error) {
    next(error);
  }
}

// Tìm kiếm tin nhắn
async function timKiemTinNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { tu_khoa, nguoi_dung_id } = req.query;

    if (!tu_khoa || tu_khoa.trim() === '') {
      return res.status(400).json({ message: 'Từ khóa tìm kiếm không được để trống' });
    }

    let messages;
    if (nguoi_dung_id) {
      // Admin tìm kiếm chat với khách hàng
      const admin = await NguoiDung.findById(userId);
      if (!admin || admin.vai_tro !== 'ADMIN') {
        return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
      }
      messages = await chatService.timKiemTinNhan(nguoi_dung_id, tu_khoa, userId);
    } else {
      // Khách hàng tìm kiếm chat của mình
      messages = await chatService.timKiemTinNhan(userId, tu_khoa);
    }
    
    res.json({
      success: true,
      data: messages
    });
  } catch (error) {
    next(error);
  }
}

// Xóa tin nhắn
async function xoaTinNhan(req, res, next) {
  try {
    const userId = req.user.id;
    const { tin_nhan_id } = req.params;

    if (!tin_nhan_id) {
      return res.status(400).json({ message: 'ID tin nhắn là bắt buộc' });
    }

    const message = await chatService.xoaTinNhan(tin_nhan_id, userId);
    
    res.json({
      success: true,
      message: 'Xóa tin nhắn thành công',
      data: message
    });
  } catch (error) {
    next(error);
  }
}

// Đếm tin nhắn chưa đọc của admin
async function demTinNhanChuaDoc(req, res, next) {
  try {
    const adminId = req.user.id;

    // Kiểm tra admin có quyền không
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const count = await chatService.demTinNhanChuaDoc(adminId);
    
    res.json({
      success: true,
      data: { so_tin_nhan_chua_doc: count }
    });
  } catch (error) {
    next(error);
  }
}

// Lấy thống kê chat
async function layThongKeChat(req, res, next) {
  try {
    const adminId = req.user.id;

    // Kiểm tra admin có quyền không
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    const stats = await chatService.layThongKeChat(adminId);
    
    res.json({
      success: true,
      data: stats
    });
  } catch (error) {
    next(error);
  }
}

// Đánh dấu đã đọc tin nhắn
async function danhDauDaDoc(req, res, next) {
  try {
    const adminId = req.user.id;
    const { nguoi_dung_id } = req.body;

    if (!nguoi_dung_id) {
      return res.status(400).json({ message: 'ID người dùng là bắt buộc' });
    }

    // Kiểm tra admin có quyền không
    const admin = await NguoiDung.findById(adminId);
    if (!admin || admin.vai_tro !== 'ADMIN') {
      return res.status(403).json({ message: 'Không có quyền thực hiện thao tác này' });
    }

    await Chat.danhDauDaDoc(nguoi_dung_id, adminId);
    
    res.json({
      success: true,
      message: 'Đã đánh dấu tin nhắn là đã đọc'
    });
  } catch (error) {
    next(error);
  }
}

module.exports = {
  guiTinNhan,
  layLichSuChat,
  guiTinNhanAdmin,
  layLichSuChatAdmin,
  layDanhSachPhongChat,
  guiThongTinSanPham,
  timKiemTinNhan,
  xoaTinNhan,
  demTinNhanChuaDoc,
  layThongKeChat,
  danhDauDaDoc
};
