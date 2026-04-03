const http = require('http');
const socketIo = require('socket.io');
const ChatService = require('../services/ChatService');

const chatService = new ChatService();

class SocketHandler {
  constructor(server) {
    this.io = socketIo(server, {
      cors: {
        origin: process.env.CLIENT_URL || "http://localhost:3000",
        methods: ["GET", "POST"]
      }
    });

    // Make io globally available for ChatService
    global.io = this.io;

    this.setupEventHandlers();
  }

  setupEventHandlers() {
    this.io.on('connection', (socket) => {
      console.log(`User connected: ${socket.id}`);

      // User đăng ký
      socket.on('user_register', (data) => {
        const { userId, userType } = data;
        
        if (userType === 'KHACH_HANG') {
          chatService.userConnect(userId, socket.id);
          socket.join(`user_${userId}`);
        } else if (userType === 'ADMIN') {
          chatService.adminConnect(userId, socket.id);
          socket.join(`admin_${userId}`);
          
          // Gửi số tin nhắn chưa đọc cho admin
          this.sendUnreadCount(userId, socket);
        }

        socket.emit('register_success', { userId, userType });
      });

      // Gửi tin nhắn real-time
      socket.on('send_message', async (data) => {
        try {
          const { nguoi_dung_id, admin_id, tin_nhan, loai_tin_nhan, gui_boi, thong_tin_san_pham } = data;
          
          let message;
          if (gui_boi === 'KHACH_HANG') {
            message = await chatService.guiTinNhanKhachHang(nguoi_dung_id, tin_nhan, loai_tin_nhan, thong_tin_san_pham);
          } else if (gui_boi === 'ADMIN') {
            message = await chatService.guiTinNhanAdmin(admin_id, nguoi_dung_id, tin_nhan, loai_tin_nhan, thong_tin_san_pham);
          }

          // Broadcast tin nhắn đến các client liên quan
          this.broadcastMessage(message);
        } catch (error) {
          socket.emit('message_error', { message: error.message });
        }
      });

      // Admin đang gõ tin nhắn
      socket.on('admin_typing', (data) => {
        const { nguoi_dung_id, admin_id } = data;
        
        // Gửi thông báo đang gõ đến khách hàng
        socket.to(`user_${nguoi_dung_id}`).emit('admin_typing', {
          admin_id: admin_id,
          is_typing: true
        });
      });

      // Admin ngừng gõ tin nhắn
      socket.on('admin_stop_typing', (data) => {
        const { nguoi_dung_id, admin_id } = data;
        
        socket.to(`user_${nguoi_dung_id}`).emit('admin_typing', {
          admin_id: admin_id,
          is_typing: false
        });
      });

      // Khách hàng đang gõ tin nhắn
      socket.on('user_typing', (data) => {
        const { nguoi_dung_id, admin_id } = data;
        
        // Gửi thông báo đang gõ đến admin
        socket.to(`admin_${admin_id}`).emit('user_typing', {
          nguoi_dung_id: nguoi_dung_id,
          is_typing: true
        });
      });

      // Khách hàng ngừng gõ tin nhắn
      socket.on('user_stop_typing', (data) => {
        const { nguoi_dung_id, admin_id } = data;
        
        socket.to(`admin_${admin_id}`).emit('user_typing', {
          nguoi_dung_id: nguoi_dung_id,
          is_typing: false
        });
      });

      // Admin đánh dấu đã đọc
      socket.on('mark_as_read', async (data) => {
        try {
          const { nguoi_dung_id, admin_id } = data;
          await chatService.danhDauDaDoc(nguoi_dung_id, admin_id);
          
          // Gửi xác nhận đã đọc
          socket.to(`user_${nguoi_dung_id}`).emit('messages_read', {
            admin_id: admin_id
          });
        } catch (error) {
          socket.emit('mark_read_error', { message: error.message });
        }
      });

      // Lấy danh sách admin online
      socket.on('get_online_admins', () => {
        const onlineAdmins = Array.from(chatService.adminSockets.keys());
        socket.emit('online_admins', onlineAdmins);
      });

      // Lấy danh sách user online
      socket.on('get_online_users', () => {
        const onlineUsers = Array.from(chatService.onlineUsers.keys());
        socket.emit('online_users', onlineUsers);
      });

      // Disconnect
      socket.on('disconnect', () => {
        console.log(`User disconnected: ${socket.id}`);
        
        // Tìm và xóa user khỏi danh sách online
        this.removeUserFromOnlineList(socket.id);
      });
    });
  }

  // Broadcast tin nhắn đến các client liên quan
  broadcastMessage(message) {
    // Gửi đến khách hàng
    this.io.to(`user_${message.nguoi_dung_id}`).emit('new_message', message);
    
    // Gửi đến admin
    this.io.to(`admin_${message.admin_id}`).emit('new_message', message);
  }

  // Gửi số tin nhắn chưa đọc cho admin
  async sendUnreadCount(adminId, socket) {
    try {
      const count = await chatService.demTinNhanChuaDoc(adminId);
      socket.emit('unread_count', { count });
    } catch (error) {
      console.error('Error sending unread count:', error);
    }
  }

  // Xóa user khỏi danh sách online khi disconnect
  removeUserFromOnlineList(socketId) {
    // Tìm và xóa khỏi online users
    for (const [userId, userSocketId] of chatService.onlineUsers.entries()) {
      if (userSocketId === socketId) {
        chatService.userDisconnect(userId);
        break;
      }
    }

    // Tìm và xóa khỏi admin sockets
    for (const [adminId, adminSocketId] of chatService.adminSockets.entries()) {
      if (adminSocketId === socketId) {
        chatService.adminDisconnect(adminId);
        break;
      }
    }
  }

  // Gửi thông báo đến tất cả admin
  broadcastToAdmins(event, data) {
    this.io.emit(event, data);
  }

  // Gửi thông báo đến user cụ thể
  sendToUser(userId, event, data) {
    this.io.to(`user_${userId}`).emit(event, data);
  }

  // Gửi thông báo đến admin cụ thể
  sendToAdmin(adminId, event, data) {
    this.io.to(`admin_${adminId}`).emit(event, data);
  }
}

module.exports = SocketHandler;
