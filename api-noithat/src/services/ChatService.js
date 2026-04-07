const Chat = require('../models/Chat');
const NguoiDung = require('../models/NguoiDung');
const SanPham = require('../models/SanPham');

class ChatService {
  constructor() {
    this.onlineUsers = new Map(); // userId -> socketId
    this.adminSockets = new Map(); // adminId -> socketId
  }

  // User connects
  userConnect(userId, socketId) {
    this.onlineUsers.set(userId, socketId);
    console.log(`User ${userId} connected with socket ${socketId}`);
  }

  // Admin connects
  adminConnect(adminId, socketId) {
    this.adminSockets.set(adminId, socketId);
    console.log(`Admin ${adminId} connected with socket ${socketId}`);
  }

  // User disconnects
  userDisconnect(userId) {
    this.onlineUsers.delete(userId);
    console.log(`User ${userId} disconnected`);
  }

  // Admin disconnects
  adminDisconnect(adminId) {
    this.adminSockets.delete(adminId);
    console.log(`Admin ${adminId} disconnected`);
  }

  // Gửi tin nhắn từ khách hàng
  async guiTinNhanKhachHang(nguoiDungId, tinNhan, loaiTinNhan = 'TEXT', thongTinSanPham = null) {
    try {
      // Tìm admin online (nếu có nhiều admin, chọn admin có ít tin nhắn chờ nhất)
      const adminId = await this.findAvailableAdmin();
      
      if (!adminId) {
        throw new Error('Không có admin online. Vui lòng thử lại sau.');
      }

      // Tạo tin nhắn mới
      const chatMessage = await Chat.create({
        nguoi_dung_id: nguoiDungId,
        admin_id: adminId,
        tin_nhan: tinNhan,
        loai_tin_nhan: loaiTinNhan,
        thong_tin_san_pham: thongTinSanPham,
        gui_boi: 'KHACH_HANG',
        trang_thai: 'DA_GUI'
      });

      // Populate thông tin
      const populatedMessage = await Chat.findById(chatMessage._id)
        .populate('nguoi_dung_id', 'hoTen email')
        .populate('admin_id', 'hoTen email');

      // Gửi tin nhắn đến admin online
      const adminSocketId = this.adminSockets.get(adminId.toString());
      if (adminSocketId && global.io) {
        global.io.to(adminSocketId).emit('tin_nhan_moi', populatedMessage.toClientObject());
      }

      return populatedMessage.toClientObject();
    } catch (error) {
      throw new Error(`Lỗi gửi tin nhắn: ${error.message}`);
    }
  }

  // Gửi tin nhắn từ admin
  async guiTinNhanAdmin(adminId, nguoiDungId, tinNhan, loaiTinNhan = 'TEXT', thongTinSanPham = null) {
    try {
      // Tạo tin nhắn mới
      const chatMessage = await Chat.create({
        nguoi_dung_id: nguoiDungId,
        admin_id: adminId,
        tin_nhan: tinNhan,
        loai_tin_nhan: loaiTinNhan,
        thong_tin_san_pham: thongTinSanPham,
        gui_boi: 'ADMIN',
        trang_thai: 'DA_TRA_LOI'
      });

      // Đánh dấu tin nhắn trước đó của khách hàng là đã đọc
      await Chat.danhDauDaDoc(nguoiDungId, adminId);

      // Populate thông tin
      const populatedMessage = await Chat.findById(chatMessage._id)
        .populate('nguoi_dung_id', 'hoTen email')
        .populate('admin_id', 'hoTen email');

      // Gửi tin nhắn đến khách hàng online
      const userSocketId = this.onlineUsers.get(nguoiDungId.toString());
      if (userSocketId && global.io) {
        global.io.to(userSocketId).emit('tin_nhan_moi', populatedMessage.toClientObject());
      }

      return populatedMessage.toClientObject();
    } catch (error) {
      throw new Error(`Lỗi gửi tin nhắn: ${error.message}`);
    }
  }

  // Lấy lịch sử chat của khách hàng
  async layLichSuChatKhachHang(nguoiDungId, limit = 50) {
    try {
      const messages = await Chat.layTinNhanCuaKhachHang(nguoiDungId, limit);
      return messages.map(msg => msg.toClientObject());
    } catch (error) {
      throw new Error(`Lỗi lấy lịch sử chat: ${error.message}`);
    }
  }

  // Lấy lịch sử chat của admin
  async layLichSuChatAdmin(adminId, limit = 50) {
    try {
      const messages = await Chat.layTinNhanCuaAdmin(adminId, limit);
      return messages.map(msg => msg.toClientObject());
    } catch (error) {
      throw new Error(`Lỗi lấy lịch sử chat: ${error.message}`);
    }
  }

  // Lấy danh sách phòng chat của admin
  async layDanhSachPhongChat(adminId) {
    try {
      const chatRooms = await Chat.layDanhSachPhongChat(adminId);
      return chatRooms;
    } catch (error) {
      throw new Error(`Lỗi lấy danh sách phòng chat: ${error.message}`);
    }
  }

  // Gửi thông tin sản phẩm trong chat
  async guiThongTinSanPham(nguoiDungId, sanPhamId, adminId = null) {
    try {
      // Lấy thông tin sản phẩm
      const sanPham = await SanPham.findById(sanPhamId);
      if (!sanPham) {
        throw new Error('Sản phẩm không tồn tại');
      }

      const thongTinSanPham = {
        san_pham_id: sanPham._id,
        ten_san_pham: sanPham.ten,
        hinh_anh: sanPham.hinh_dai_dien || sanPham.hinh_anh[0],
        gia: sanPham.gia
      };

      let tinNhan = `Sản phẩm: ${sanPham.ten}\nGiá: ${sanPham.gia.toLocaleString('vi-VN')} VNĐ`;

      if (adminId) {
        // Admin gửi thông tin sản phẩm
        return await this.guiTinNhanAdmin(adminId, nguoiDungId, tinNhan, 'PRODUCT', thongTinSanPham);
      } else {
        // Khách hàng gửi thông tin sản phẩm
        return await this.guiTinNhanKhachHang(nguoiDungId, tinNhan, 'PRODUCT', thongTinSanPham);
      }
    } catch (error) {
      throw new Error(`Lỗi gửi thông tin sản phẩm: ${error.message}`);
    }
  }

  // Tìm admin available
  async findAvailableAdmin() {
    try {
      // Ưu tiên admin đang online
      if (this.adminSockets.size > 0) {
        const onlineAdminIds = Array.from(this.adminSockets.keys());
        
        // Tìm admin có ít tin nhắn chờ nhất
        const adminStats = await Chat.aggregate([
          { $match: { admin_id: { $in: onlineAdminIds.map(id => new mongoose.Types.ObjectId(id)) } } },
          { $group: {
            _id: '$admin_id',
            so_tin_nhan_chua_doc: {
              $sum: { $cond: [{ $eq: ['$trang_thai', 'DA_GUI'] }, 1, 0] }
            }
          }},
          { $sort: { so_tin_nhan_chua_doc: 1 } }
        ]);

        if (adminStats.length > 0) {
          return adminStats[0]._id;
        }

        // Nếu không có thống kê, trả về admin online đầu tiên
        return new mongoose.Types.ObjectId(onlineAdminIds[0]);
      }

      // Nếu không có admin online, tìm admin mặc định
      const adminMacDinh = await NguoiDung.findOne({ vai_tro: 'ADMIN' });
      if (adminMacDinh) {
        return adminMacDinh._id;
      }

      return null;
    } catch (error) {
      console.error('Lỗi tìm admin available:', error);
      return null;
    }
  }

  // Đếm tin nhắn chưa đọc của admin
  async demTinNhanChuaDoc(adminId) {
    try {
      const count = await Chat.countDocuments({
        admin_id: adminId,
        trang_thai: 'DA_GUI',
        gui_boi: 'KHACH_HANG'
      });
      return count;
    } catch (error) {
      throw new Error(`Lỗi đếm tin nhắn chưa đọc: ${error.message}`);
    }
  }

  // Tìm kiếm tin nhắn
  async timKiemTinNhan(nguoiDungId, tuKhoa, adminId = null) {
    try {
      let query = {
        nguoi_dung_id: nguoiDungId,
        da_xoa: false,
        tin_nhan: { $regex: tuKhoa, $options: 'i' }
      };

      if (adminId) {
        query.admin_id = adminId;
      }

      const messages = await Chat.find(query)
        .sort({ ngay_tao: -1 })
        .limit(20)
        .populate('admin_id', 'hoTen email')
        .populate('nguoi_dung_id', 'hoTen email');

      return messages.map(msg => msg.toClientObject());
    } catch (error) {
      throw new Error(`Lỗi tìm kiếm tin nhắn: ${error.message}`);
    }
  }

  // Xóa tin nhắn
  async xoaTinNhan(tinNhanId, nguoiDungId) {
    try {
      const message = await Chat.findOneAndUpdate(
        { _id: tinNhanId, nguoi_dung_id: nguoiDungId },
        { da_xoa: true },
        { new: true }
      );

      if (!message) {
        throw new Error('Tin nhắn không tồn tại hoặc không có quyền xóa');
      }

      return message.toClientObject();
    } catch (error) {
      throw new Error(`Lỗi xóa tin nhắn: ${error.message}`);
    }
  }

  // Lấy thống kê chat
  async layThongKeChat(adminId) {
    try {
      const stats = await Chat.aggregate([
        { $match: { admin_id: new mongoose.Types.ObjectId(adminId) } },
        { $group: {
          _id: null,
          tong_tin_nhan: { $sum: 1 },
          tin_nhan_hom_nay: {
            $sum: {
              $cond: [
                { $gte: ['$ngay_tao', new Date(new Date().setHours(0, 0, 0, 0))] },
                1,
                0
              ]
            }
          },
          tin_nhan_chua_doc: {
            $sum: { $cond: [{ $eq: ['$trang_thai', 'DA_GUI'] }, 1, 0] }
          }
        }}
      ]);

      return stats[0] || {
        tong_tin_nhan: 0,
        tin_nhan_hom_nay: 0,
        tin_nhan_chua_doc: 0
      };
    } catch (error) {
      throw new Error(`Lỗi lấy thống kê chat: ${error.message}`);
    }
  }
}

module.exports = ChatService;
