const express = require('express');
const router = express.Router();
const PaymentService = require('../services/PaymentService');
const DonHang = require('../models/DonHang');
const { requireAuth } = require('../middleware/auth');

const paymentService = new PaymentService();

// Tạo thanh toán VNPAY
router.post('/vnpay/create', requireAuth, async (req, res, next) => {
  try {
    const { orderId, amount, orderInfo } = req.body;
    const ipAddr = req.ip || req.connection.remoteAddress || '127.0.0.1';

    if (!orderId || !amount) {
      return res.status(400).json({ message: 'orderId và amount là bắt buộc' });
    }

    // Kiểm tra đơn hàng tồn tại
    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: req.user.id });
    if (!donHang) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Tạo URL thanh toán
    const paymentUrl = paymentService.createVnpayPaymentUrl(
      orderId, 
      amount, 
      orderInfo || `Thanh toan don hang ${orderId}`, 
      ipAddr
    );

    res.json({
      success: true,
      paymentUrl: paymentUrl,
      orderId: orderId,
      amount: amount
    });
  } catch (error) {
    next(error);
  }
});

// Xử lý callback từ VNPAY
router.get('/vnpay/return', async (req, res, next) => {
  try {
    const vnp_Params = req.query;
    
    // Xác minh chữ ký
    const isValid = paymentService.verifyVnpayReturn(vnp_Params);
    
    if (!isValid) {
      return res.status(400).json({ message: 'Chữ ký không hợp lệ' });
    }

    const responseCode = vnp_Params.vnp_ResponseCode;
    const orderId = vnp_Params.vnp_TxnRef;

    // Cập nhật trạng thái đơn hàng
    if (responseCode === '00') {
      // Thanh toán thành công
      await DonHang.findByIdAndUpdate(orderId, {
        trang_thai_thanh_toan: 'DA_THANH_TOAN',
        ngay_cap_nhat: new Date()
      });

      // Redirect về trang thành công
      res.redirect(`${process.env.CLIENT_URL}/payment/success?orderId=${orderId}&method=VNPAY`);
    } else {
      // Thanh toán thất bại
      await DonHang.findByIdAndUpdate(orderId, {
        trang_thai_thanh_toan: 'THAT_BAI',
        ngay_cap_nhat: new Date()
      });

      // Redirect về trang thất bại
      res.redirect(`${process.env.CLIENT_URL}/payment/failed?orderId=${orderId}&method=VNPAY&code=${responseCode}`);
    }
  } catch (error) {
    next(error);
  }
});

// IPN handler từ VNPAY (server-to-server)
router.post('/vnpay/ipn', async (req, res, next) => {
  try {
    const vnp_Params = req.body;
    
    // Xác minh chữ ký
    const isValid = paymentService.verifyVnpayReturn(vnp_Params);
    
    if (!isValid) {
      return res.status(400).json({ message: 'Chữ ký không hợp lệ' });
    }

    const responseCode = vnp_Params.vnp_ResponseCode;
    const orderId = vnp_Params.vnp_TxnRef;

    // Cập nhật trạng thái đơn hàng
    if (responseCode === '00') {
      await DonHang.findByIdAndUpdate(orderId, {
        trang_thai_thanh_toan: 'DA_THANH_TOAN',
        ngay_cap_nhat: new Date()
      });

      res.json({ RspCode: '00', Message: 'success' });
    } else {
      await DonHang.findByIdAndUpdate(orderId, {
        trang_thai_thanh_toan: 'THAT_BAI',
        ngay_cap_nhat: new Date()
      });

      res.json({ RspCode: '00', Message: 'success' });
    }
  } catch (error) {
    next(error);
  }
});

// Tạo thanh toán PayPal
router.post('/paypal/create', requireAuth, async (req, res, next) => {
  try {
    const { orderId, amount, returnUrl, cancelUrl } = req.body;

    if (!orderId || !amount) {
      return res.status(400).json({ message: 'orderId và amount là bắt buộc' });
    }

    // Kiểm tra đơn hàng tồn tại
    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: req.user.id });
    if (!donHang) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // Tạo URL thanh toán PayPal
    const paymentUrl = paymentService.createPaypalPaymentUrl(
      orderId,
      amount,
      returnUrl || `${process.env.CLIENT_URL}/payment/paypal/return`,
      cancelUrl || `${process.env.CLIENT_URL}/payment/paypal/cancel`
    );

    res.json({
      success: true,
      paymentUrl: paymentUrl,
      orderId: orderId,
      amount: amount
    });
  } catch (error) {
    next(error);
  }
});

// Xử lý callback từ PayPal
router.post('/paypal/return', async (req, res, next) => {
  try {
    const { orderId, status, transactionId } = req.body;

    if (!orderId || !status) {
      return res.status(400).json({ message: 'orderId và status là bắt buộc' });
    }

    // Cập nhật trạng thái đơn hàng
    if (status === 'completed') {
      await DonHang.findByIdAndUpdate(orderId, {
        trang_thai_thanh_toan: 'DA_THANH_TOAN',
        ngay_cap_nhat: new Date()
      });

      res.json({
        success: true,
        message: 'Thanh toán PayPal thành công',
        orderId: orderId,
        transactionId: transactionId
      });
    } else {
      await DonHang.findByIdAndUpdate(orderId, {
        trang_thai_thanh_toan: 'THAT_BAI',
        ngay_cap_nhat: new Date()
      });

      res.json({
        success: false,
        message: 'Thanh toán PayPal thất bại',
        orderId: orderId
      });
    }
  } catch (error) {
    next(error);
  }
});

// Xử lý thanh toán COD
router.post('/cod/confirm', requireAuth, async (req, res, next) => {
  try {
    const { orderId } = req.body;

    if (!orderId) {
      return res.status(400).json({ message: 'orderId là bắt buộc' });
    }

    // Kiểm tra đơn hàng tồn tại
    const donHang = await DonHang.findOne({ _id: orderId, nguoi_dung_id: req.user.id });
    if (!donHang) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    // COD không cần xử lý thanh toán ngay
    // Chỉ xác nhận đơn hàng
    const result = await paymentService.processCodPayment(orderId, donHang.tong_tien);

    res.json(result);
  } catch (error) {
    next(error);
  }
});

// Kiểm tra trạng thái thanh toán
router.get('/status/:orderId', requireAuth, async (req, res, next) => {
  try {
    const { orderId } = req.params;

    const donHang = await DonHang.findOne({ 
      _id: orderId, 
      nguoi_dung_id: req.user.id 
    }).select('trang_thai_thanh_toan phuong_thuc_thanh_toan tong_tien ngay_tao');

    if (!donHang) {
      return res.status(404).json({ message: 'Không tìm thấy đơn hàng' });
    }

    res.json({
      orderId: orderId,
      paymentMethod: donHang.phuong_thuc_thanh_toan,
      paymentStatus: donHang.trang_thai_thanh_toan,
      amount: donHang.tong_tien,
      orderDate: donHang.ngay_tao
    });
  } catch (error) {
    next(error);
  }
});

// Lấy lịch sử thanh toán
router.get('/history', requireAuth, async (req, res, next) => {
  try {
    const { page = 1, limit = 10 } = req.query;
    const skip = (page - 1) * limit;

    const donHangs = await DonHang.find({ 
      nguoi_dung_id: req.user.id 
    })
    .select('trang_thai_thanh_toan phuong_thuc_thanh_toan tong_tien ngay_tao ngay_cap_nhat')
    .sort({ ngay_tao: -1 })
    .skip(skip)
    .limit(parseInt(limit));

    const total = await DonHang.countDocuments({ nguoi_dung_id: req.user.id });

    res.json({
      payments: donHangs.map(donHang => ({
        orderId: donHang._id,
        paymentMethod: donHang.phuong_thuc_thanh_toan,
        paymentStatus: donHang.trang_thai_thanh_toan,
        amount: donHang.tong_tien,
        orderDate: donHang.ngay_tao,
        updatedDate: donHang.ngay_cap_nhat
      })),
      pagination: {
        current: parseInt(page),
        total: Math.ceil(total / limit),
        count: total
      }
    });
  } catch (error) {
    next(error);
  }
});

module.exports = router;
