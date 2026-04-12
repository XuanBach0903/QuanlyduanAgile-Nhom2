// test: kiểm tra quản lý sản phẩm
// bug: không cập nhật sản phẩm
const DanhMuc = require('../models/DanhMuc');

// GET /admin/danh-muc
async function danhSachDanhMucAdmin(req, res, next) {
  try {
    const list = await DanhMuc.find().sort({ ten: 1 });
    res.json(
      list.map((dm) => ({
        id: dm._id,
        ten: dm.ten,
        moTa: dm.mo_ta,
        slug: dm.slug,
      }))
    );
  } catch (err) {
    next(err);
  }
}

// POST /admin/danh-muc
async function taoDanhMuc(req, res, next) {
  try {
    const { ten, moTa } = req.body;
    if (!ten) {
      return res.status(400).json({ message: 'Ten danh muc la bat buoc' });
    }
    // Kiểm tra trùng tên (không phân biệt hoa thường)
    const existingName = await DanhMuc.findOne({
      ten: { $regex: new RegExp('^' + ten + '$', 'i') }
    });
    if (existingName) {
      return res.status(400).json({ message: 'Tên danh mục đã tồn tại' });
    }

    const slug = ten
      .toString()
      .toLowerCase()
      .trim()
      .replace(/\s+/g, '-')
      .replace(/[^a-z0-9-]/g, '');

    const existed = await DanhMuc.findOne({ slug });
    if (existed) {
      return res.status(400).json({ message: 'Danh muc da ton tai' });
    }

    const dm = await DanhMuc.create({ ten, slug, mo_ta: moTa });
    res.status(201).json({
      id: dm._id,
      ten: dm.ten,
      moTa: dm.mo_ta,
      slug: dm.slug,
    });
  } catch (err) {
    next(err);
  }
}

// PATCH /admin/danh-muc/:id
async function capNhatDanhMuc(req, res, next) {
  try {
    const { id } = req.params;
    const { ten, moTa } = req.body;

    const dm = await DanhMuc.findById(id);
    if (!dm) {
      return res.status(404).json({ message: 'Khong tim thay danh muc' });
    }

    if (ten) {
      // Kiểm tra trùng tên khi cập nhật (không phân biệt hoa thường, trừ chính nó)
      const existingName = await DanhMuc.findOne({
        _id: { $ne: id },
        ten: { $regex: new RegExp('^' + ten + '$', 'i') }
      });
      if (existingName) {
        return res.status(400).json({ message: 'Tên danh mục đã tồn tại' });
      }

      dm.ten = ten;
      dm.slug = ten
        .toString()
        .toLowerCase()
        .trim()
        .replace(/\s+/g, '-')
        .replace(/[^a-z0-9-]/g, '');
    }
    if (typeof moTa === 'string') {
      dm.mo_ta = moTa;
    }
    dm.ngay_cap_nhat = new Date();

    await dm.save();
    res.json({
      id: dm._id,
      ten: dm.ten,
      moTa: dm.mo_ta,
      slug: dm.slug,
    });
  } catch (err) {
    next(err);
  }
}

// DELETE /admin/danh-muc/:id
async function xoaDanhMuc(req, res, next) {
  try {
    const { id } = req.params;
    const dm = await DanhMuc.findById(id);
    if (!dm) {
      return res.status(404).json({ message: 'Khong tim thay danh muc' });
    }

    await dm.deleteOne();
    res.json({ message: 'Da xoa danh muc' });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  danhSachDanhMucAdmin,
  taoDanhMuc,
  capNhatDanhMuc,
  xoaDanhMuc,
};
