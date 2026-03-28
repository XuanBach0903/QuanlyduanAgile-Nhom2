<<<<<<< HEAD
=======
//test: verify chức năng tìm kiếm sản phẩm

>>>>>>> develop
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const NguoiDung = require('../models/NguoiDung');

<<<<<<< HEAD
// =======================
// ĐĂNG KÝ
// =======================
async function dangKy(req, res, next) {
  try {
    let { hoTen, email, soDienThoai, matKhau } = req.body;

    // ❌ BUG: thiếu validate dữ liệu đầu vào (format email, độ dài mật khẩu)
    // 👉 hiện tại chỉ check rỗng, chưa check đúng định dạng
=======
// POST /tai-khoan/dang-ky
async function dangKy(req, res, next) {
  try {
    const { hoTen, email, soDienThoai, matKhau } = req.body;

>>>>>>> develop
    if (!hoTen || !email || !matKhau) {
      return res.status(400).json({ message: 'hoTen, email, matKhau la bat buoc' });
    }

<<<<<<< HEAD
    // ❌ BUG: chưa trim dữ liệu → dễ lỗi khi login (email có khoảng trắng)
    // 👉 fix: dùng trim()
    email = email.trim().toLowerCase();

    // ❌ BUG: chưa validate định dạng email
    // 👉 fix: dùng regex kiểm tra email
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
      return res.status(400).json({ message: 'Email khong hop le' });
    }

    // ❌ BUG: chưa validate độ dài mật khẩu
    // 👉 fix: yêu cầu >= 6 ký tự
    if (matKhau.length < 6) {
      return res.status(400).json({ message: 'Mat khau phai >= 6 ky tu' });
    }

    // ❌ BUG: chưa validate số điện thoại
    if (soDienThoai && soDienThoai.length < 9) {
      return res.status(400).json({ message: 'So dien thoai khong hop le' });
    }

    const existed = await NguoiDung.findOne({ email });
=======
    const existed = await NguoiDung.findOne({ email: email.toLowerCase() });
>>>>>>> develop
    if (existed) {
      return res.status(409).json({ message: 'Email da ton tai' });
    }

    const hash = await bcrypt.hash(matKhau, 10);

    const user = await NguoiDung.create({
<<<<<<< HEAD
      ho_ten: hoTen.trim(),
      email,
=======
      ho_ten: hoTen,
      email: email.toLowerCase(),
>>>>>>> develop
      so_dien_thoai: soDienThoai || '',
      mat_khau_hash: hash,
      vai_tro: 'KHACH_HANG',
    });

    res.status(201).json({
      id: user._id,
      hoTen: user.ho_ten,
      email: user.email,
      soDienThoai: user.so_dien_thoai,
      imgUrl: user.img_url || '',
      vaiTro: user.vai_tro,
    });
  } catch (err) {
    next(err);
  }
}

<<<<<<< HEAD
// =======================
// ĐĂNG NHẬP
// =======================
async function dangNhap(req, res, next) {
  try {
    let { email, matKhau } = req.body;

    // ❌ BUG: thiếu validate dữ liệu đầu vào
=======
// PATCH /tai-khoan/toi
// Khach hang tu cap nhat ho ten, so dien thoai (KHONG duoc doi email o day)
async function capNhatTaiKhoan(req, res, next) {
  try {
    const current = req.currentUser;
    if (!current) {
      return res.status(401).json({ message: 'Chua dang nhap' });
    }

    const { hoTen, soDienThoai, diaChi, imgUrl } = req.body;

    const user = await NguoiDung.findById(current._id);
    if (!user) {
      return res.status(404).json({ message: 'Khong tim thay tai khoan' });
    }

    if (typeof hoTen === 'string' && hoTen.trim() !== '') {
      user.ho_ten = hoTen.trim();
    }
    if (typeof soDienThoai === 'string') {
      user.so_dien_thoai = soDienThoai.trim();
    }
    if (typeof diaChi === 'string') {
      user.dia_chi = diaChi.trim();
    }
    if (typeof imgUrl === 'string') {
      user.img_url = imgUrl.trim();
    }
    user.ngay_cap_nhat = new Date();

    await user.save();

    res.json({
      id: user._id,
      hoTen: user.ho_ten,
      email: user.email,
      soDienThoai: user.so_dien_thoai,
      diaChi: user.dia_chi || '',
      imgUrl: user.img_url || '',
      vaiTro: user.vai_tro,
    });
  } catch (err) {
    next(err);
  }
}

// POST /tai-khoan/doi-mat-khau
// Body: { matKhauCu, matKhauMoi, xacNhanMatKhau }
async function doiMatKhau(req, res, next) {
  try {
    const current = req.currentUser;
    if (!current) {
      return res.status(401).json({ message: 'Chua dang nhap' });
    }

    const { matKhauCu, matKhauMoi, xacNhanMatKhau } = req.body;
    if (!matKhauCu || !matKhauMoi || !xacNhanMatKhau) {
      return res
        .status(400)
        .json({ message: 'matKhauCu, matKhauMoi, xacNhanMatKhau la bat buoc' });
    }
    if (matKhauMoi.length < 6) {
      return res.status(400).json({ message: 'Mat khau moi phai it nhat 6 ky tu' });
    }

    if (matKhauMoi !== xacNhanMatKhau) {
      return res.status(400).json({ message: 'Xac nhan mat khau khong khop' });
    }

    const user = await NguoiDung.findById(current._id);
    if (!user) {
      return res.status(404).json({ message: 'Khong tim thay tai khoan' });
    }

    const match = await bcrypt.compare(matKhauCu, user.mat_khau_hash);
    if (!match) {
      return res.status(400).json({ message: 'Mat khau cu khong dung' });
    }

    user.mat_khau_hash = await bcrypt.hash(matKhauMoi, 10);
    user.ngay_cap_nhat = new Date();
    await user.save();

    res.json({ message: 'Doi mat khau thanh cong' });
  } catch (err) {
    next(err);
  }
}

// POST /tai-khoan/dang-nhap
async function dangNhap(req, res, next) {
  try {
    const { email, matKhau } = req.body;
>>>>>>> develop
    if (!email || !matKhau) {
      return res.status(400).json({ message: 'email, matKhau la bat buoc' });
    }

<<<<<<< HEAD
    // ❌ BUG: chưa trim email → dễ sai khi login
    email = email.trim().toLowerCase();

    const user = await NguoiDung.findOne({ email });

    // ❌ BUG: không phân biệt rõ user không tồn tại
    if (!user) {
      return res.status(401).json({ message: 'Tai khoan khong ton tai' });
    }

    // ❌ BUG: chưa kiểm tra trạng thái tài khoản (có thể bị khóa)
=======
    // Tim nguoi dung theo email de phan biet tai khoan bi khoa va tai khoan khong ton tai
    const user = await NguoiDung.findOne({ email: email.toLowerCase() });
    if (!user) {
      // Tai khoan da bi xoa hoac khong ton tai
      return res.status(401).json({ message: 'Tai khoan khong ton tai' });
    }

    // Kiem tra tai khoan co bi khoa hay khong
>>>>>>> develop
    if (user.trang_thai !== 1) {
      return res.status(403).json({ message: 'Tai khoan da bi khoa' });
    }

    const match = await bcrypt.compare(matKhau, user.mat_khau_hash);
<<<<<<< HEAD

    // ❌ BUG: sai mật khẩu nhưng thông báo chưa rõ
=======
>>>>>>> develop
    if (!match) {
      return res.status(401).json({ message: 'Email hoac mat khau khong dung' });
    }

    const payload = {
      id: user._id,
      email: user.email,
      vaiTro: user.vai_tro,
    };
<<<<<<< HEAD

    const token = jwt.sign(
      payload,
      process.env.JWT_SECRET || 'default_secret',
      { expiresIn: '7d' }
    );
=======
    const token = jwt.sign(payload, process.env.JWT_SECRET || 'default_secret', {
      expiresIn: '7d',
    });
>>>>>>> develop

    res.json({
      token,
      user: {
        id: user._id,
        hoTen: user.ho_ten,
        email: user.email,
        soDienThoai: user.so_dien_thoai,
        diaChi: user.dia_chi || '',
        imgUrl: user.img_url || '',
        vaiTro: user.vai_tro,
      },
    });
  } catch (err) {
    next(err);
  }
}

<<<<<<< HEAD
// =======================
// ĐỔI MẬT KHẨU
// =======================
async function doiMatKhau(req, res, next) {
  try {
    const current = req.currentUser;

    if (!current) {
      return res.status(401).json({ message: 'Chua dang nhap' });
    }

    const { matKhauCu, matKhauMoi, xacNhanMatKhau } = req.body;

    // ❌ BUG: thiếu validate dữ liệu
    if (!matKhauCu || !matKhauMoi || !xacNhanMatKhau) {
      return res.status(400).json({ message: 'Thieu thong tin' });
    }

    if (matKhauMoi.length < 6) {
      return res.status(400).json({ message: 'Mat khau moi phai >= 6 ky tu' });
    }

    if (matKhauMoi !== xacNhanMatKhau) {
      return res.status(400).json({ message: 'Xac nhan mat khau khong khop' });
    }

    const user = await NguoiDung.findById(current._id);

    if (!user) {
      return res.status(404).json({ message: 'Khong tim thay tai khoan' });
    }

    const match = await bcrypt.compare(matKhauCu, user.mat_khau_hash);

    if (!match) {
      return res.status(400).json({ message: 'Mat khau cu khong dung' });
    }

    user.mat_khau_hash = await bcrypt.hash(matKhauMoi, 10);
    user.ngay_cap_nhat = new Date();

    await user.save();

    res.json({ message: 'Doi mat khau thanh cong' });
  } catch (err) {
    next(err);
  }
}

// =======================
// ĐĂNG XUẤT
// =======================
async function dangXuat(req, res, next) {
  try {
    // ❌ BUG: JWT lưu phía client nên backend không thực sự logout
    // 👉 đây là hành vi bình thường của JWT
=======
// POST /tai-khoan/dang-xuat
// Với JWT lưu phía client, backend chỉ cần trả OK.
async function dangXuat(req, res, next) {
  try {
>>>>>>> develop
    return res.json({ message: 'Dang xuat thanh cong' });
  } catch (err) {
    next(err);
  }
}

<<<<<<< HEAD
// =======================
// LẤY THÔNG TIN
// =======================
async function thongTinTaiKhoan(req, res, next) {
  try {
    const user = req.currentUser;

=======
// GET /tai-khoan/toi
async function thongTinTaiKhoan(req, res, next) {
  try {
    const user = req.currentUser;
>>>>>>> develop
    if (!user) {
      return res.status(401).json({ message: 'Chua dang nhap' });
    }

    res.json({
      id: user._id,
      hoTen: user.ho_ten,
      email: user.email,
      soDienThoai: user.so_dien_thoai,
      diaChi: user.dia_chi || '',
      imgUrl: user.img_url || '',
      vaiTro: user.vai_tro,
    });
  } catch (err) {
    next(err);
  }
}

<<<<<<< HEAD
// =======================
// UPDATE PROFILE
// =======================
async function capNhatTaiKhoan(req, res, next) {
  try {
    const current = req.currentUser;

    if (!current) {
      return res.status(401).json({ message: 'Chua dang nhap' });
    }

    const { hoTen, soDienThoai, diaChi, imgUrl } = req.body;

    const user = await NguoiDung.findById(current._id);

    if (!user) {
      return res.status(404).json({ message: 'Khong tim thay tai khoan' });
    }

    // ❌ BUG: chưa validate dữ liệu update
    if (hoTen) user.ho_ten = hoTen.trim();
    if (soDienThoai) user.so_dien_thoai = soDienThoai.trim();
    if (diaChi) user.dia_chi = diaChi.trim();
    if (imgUrl) user.img_url = imgUrl.trim();

    user.ngay_cap_nhat = new Date();

    await user.save();

    res.json({
      id: user._id,
      hoTen: user.ho_ten,
      email: user.email,
      soDienThoai: user.so_dien_thoai,
      diaChi: user.dia_chi || '',
      imgUrl: user.img_url || '',
      vaiTro: user.vai_tro,
    });
  } catch (err) {
    next(err);
  }
}

=======
>>>>>>> develop
module.exports = {
  dangKy,
  dangNhap,
  dangXuat,
  thongTinTaiKhoan,
  capNhatTaiKhoan,
  doiMatKhau,
<<<<<<< HEAD
};
=======
};
>>>>>>> develop
