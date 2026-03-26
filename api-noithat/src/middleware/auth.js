const jwt = require('jsonwebtoken');
const NguoiDung = require('../models/NguoiDung');

async function requireAuth(req, res, next) {
  const authHeader = req.headers.authorization || '';
  const token = authHeader.startsWith('Bearer ') ? authHeader.slice(7) : null;
  if (!token) {
    return res.status(401).json({ message: 'Chua dang nhap' });
  }

  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET || 'default_secret');

    // Kiểm tra tài khoản còn tồn tại và chưa bị khoá
    const user = await NguoiDung.findById(payload.id).select('-mat_khau_hash');
    if (!user) {
      return res.status(401).json({ message: 'Tai khoan khong ton tai' });
    }
    if (user.trang_thai !== 1) {
      return res.status(403).json({ message: 'Tai khoan da bi khoa' });
    }

    req.user = {
      id: user._id.toString(),
      email: user.email,
      vaiTro: user.vai_tro,
    };
    // Đồng thời đính kèm currentUser để các middleware khác dùng
    req.currentUser = user;

    next();
  } catch (err) {
    return res.status(401).json({ message: 'Token khong hop le hoac het han' });
  }
}

function requireRole(role) {
  return (req, res, next) => {
    if (!req.user || req.user.vaiTro !== role) {
      return res.status(403).json({ message: 'Khong co quyen truy cap' });
    }
    next();
  };
}

async function attachCurrentUser(req, res, next) {
  try {
    // requireAuth đã đính kèm currentUser nếu có
    if (req.currentUser) return next();
    if (!req.user || !req.user.id) return next();

    const user = await NguoiDung.findById(req.user.id).select('-mat_khau_hash');
    if (!user) return next();
    if (user.trang_thai !== 1) {
      return res.status(403).json({ message: 'Tai khoan da bi khoa' });
    }

    req.currentUser = user;
    next();
  } catch (err) {
    next(err);
  }
}

module.exports = {
  requireAuth,
  requireRole,
  attachCurrentUser,
};
