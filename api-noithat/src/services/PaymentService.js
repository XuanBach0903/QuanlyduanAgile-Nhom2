const crypto = require('crypto');

class PaymentService {
  constructor() {
    this.vnpayConfig = {
      vnp_TmnCode: process.env.VNPAY_TMN_CODE || 'YOUR_TMN_CODE',
      vnp_HashSecret: process.env.VNPAY_HASH_SECRET || 'YOUR_HASH_SECRET',
      vnp_Url: process.env.VNPAY_URL || 'https://sandbox.vnpayment.vn/paymentv2/vpcpayment.html',
      vnp_ReturnUrl: process.env.VNPAY_RETURN_URL || 'http://localhost:3000/payment/vnpay-return'
    };
  }

  // Tạo URL thanh toán VNPAY
  createVnpayPaymentUrl(orderId, amount, orderInfo, ipAddr) {
    const date = new Date();
    const createDate = this.formatDate(date, 'yyyyMMddHHmmss');
    const orderId = `ORDER${orderId}${date.getTime()}`;
    
    const params = {
      vnp_Version: '2.1.0',
      vnp_Command: 'pay',
      vnp_TmnCode: this.vnpayConfig.vnp_TmnCode,
      vnp_Locale: 'vn',
      vnp_CurrCode: 'VND',
      vnp_TxnRef: orderId,
      vnp_OrderInfo: orderInfo,
      vnp_OrderType: 'billpayment',
      vnp_Amount: amount * 100, // VNPAY yêu cầu số tiền * 100
      vnp_ReturnUrl: this.vnpayConfig.vnp_ReturnUrl,
      vnp_IpAddr: ipAddr,
      vnp_CreateDate: createDate
    };

    // Sắp xếp các tham số theo thứ tự alphabet
    const sortedParams = Object.keys(params)
      .sort()
      .reduce((result, key) => {
        result[key] = params[key];
        return result;
      }, {});

    // Tạo chuỗi hash
    const queryString = Object.keys(sortedParams)
      .map(key => `${key}=${sortedParams[key]}`)
      .join('&');

    const hmac = crypto.createHmac('sha512', this.vnpayConfig.vnp_HashSecret);
    hmac.update(queryString);
    const vnp_SecureHash = hmac.digest('hex');

    return `${this.vnpayConfig.vnp_Url}?${queryString}&vnp_SecureHash=${vnp_SecureHash}`;
  }

  // Xác minh kết quả thanh toán VNPAY
  verifyVnpayReturn(vnp_Params) {
    const secureHash = vnp_Params.vnp_SecureHash;
    delete vnp_Params.vnp_SecureHash;
    delete vnp_Params.vnp_SecureHashType;

    const sortedParams = Object.keys(vnp_Params)
      .sort()
      .reduce((result, key) => {
        result[key] = vnp_Params[key];
        return result;
      }, {});

    const queryString = Object.keys(sortedParams)
      .map(key => `${key}=${sortedParams[key]}`)
      .join('&');

    const hmac = crypto.createHmac('sha512', this.vnpayConfig.vnp_HashSecret);
    hmac.update(queryString);
    const calculatedHash = hmac.digest('hex');

    return secureHash === calculatedHash;
  }

  // Tạo URL thanh toán PayPal (sandbox)
  createPaypalPaymentUrl(orderId, amount, returnUrl, cancelUrl) {
    const paypalUrl = process.env.PAYPAL_URL || 'https://api-m.sandbox.paypal.com';
    const clientId = process.env.PAYPAL_CLIENT_ID || 'YOUR_CLIENT_ID';
    
    // Trong thực tế, cần gọi PayPal API để tạo payment
    // Đây là URL redirect đơn giản cho demo
    const params = new URLSearchParams({
      cmd: '_xclick',
      business: process.env.PAYPAL_BUSINESS_EMAIL || 'your-business@example.com',
      item_name: `Thanh toan don hang ${orderId}`,
      amount: amount.toFixed(2),
      currency_code: 'USD',
      return: returnUrl,
      cancel_return: cancelUrl,
      notify_url: `${process.env.BASE_URL}/payment/paypal/ipn`,
      custom: orderId
    });

    return `https://www.sandbox.paypal.com/cgi-bin/webscr?${params.toString()}`;
  }

  // Xử lý thanh toán COD
  async processCodPayment(orderId, amount) {
    try {
      // COD không cần xử lý thanh toán ngay
      // Chỉ ghi nhận đơn hàng và chờ thanh toán khi nhận hàng
      return {
        success: true,
        method: 'COD',
        orderId: orderId,
        amount: amount,
        message: 'Đặt hàng thành công. Thanh toán khi nhận hàng.',
        status: 'PENDING'
      };
    } catch (error) {
      throw new Error(`Lỗi xử lý thanh toán COD: ${error.message}`);
    }
  }

  // Xử lý thanh toán VISA/VNPAY
  async processVisaPayment(orderId, amount, paymentData) {
    try {
      // Tạo URL thanh toán VNPAY
      const orderInfo = `Thanh toan don hang ${orderId}`;
      const ipAddr = paymentData.ipAddr || '127.0.0.1';
      
      const paymentUrl = this.createVnpayPaymentUrl(orderId, amount, orderInfo, ipAddr);
      
      return {
        success: true,
        method: 'VISA',
        orderId: orderId,
        amount: amount,
        paymentUrl: paymentUrl,
        message: 'Vui lòng hoàn tất thanh toán qua VNPAY.',
        status: 'PENDING_PAYMENT'
      };
    } catch (error) {
      throw new Error(`Lỗi xử lý thanh toán VISA: ${error.message}`);
    }
  }

  // Xác minh và hoàn tất thanh toán
  async confirmPayment(paymentData) {
    try {
      const { method, vnp_Params, paypalParams, orderId } = paymentData;

      if (method === 'VNPAY' && vnp_Params) {
        const isValid = this.verifyVnpayReturn(vnp_Params);
        
        if (!isValid) {
          throw new Error('Chữ ký thanh toán không hợp lệ');
        }

        const responseCode = vnp_Params.vnp_ResponseCode;
        if (responseCode === '00') {
          return {
            success: true,
            method: 'VNPAY',
            orderId: vnp_Params.vnp_TxnRef,
            amount: parseInt(vnp_Params.vnp_Amount) / 100,
            transactionId: vnp_Params.vnp_TransactionNo,
            message: 'Thanh toán VNPAY thành công',
            status: 'COMPLETED'
          };
        } else {
          return {
            success: false,
            method: 'VNPAY',
            orderId: vnp_Params.vnp_TxnRef,
            message: `Thanh toán thất bại: ${this.getVnpayResponseMessage(responseCode)}`,
            status: 'FAILED'
          };
        }
      }

      if (method === 'PAYPAL' && paypalParams) {
        // Xử lý xác minh PayPal (cần implement IPN handler)
        return {
          success: true,
          method: 'PAYPAL',
          orderId: orderId,
          message: 'Thanh toán PayPal thành công',
          status: 'COMPLETED'
        };
      }

      throw new Error('Phương thức thanh toán không được hỗ trợ');
    } catch (error) {
      throw new Error(`Lỗi xác minh thanh toán: ${error.message}`);
    }
  }

  // Lấy thông báo lỗi VNPAY
  getVnpayResponseMessage(responseCode) {
    const messages = {
      '00': 'Giao dịch thành công',
      '01': 'Giao dịch chưa hoàn tất',
      '02': 'Lỗi giao dịch',
      '03': 'Giao dịch bị từ chối',
      '04': 'Sai mã thẻ hoặc hết hạn',
      '05': 'Không đủ tiền trong tài khoản',
      '06': 'Ngân hàng từ chối giao dịch',
      '07': 'Số tiền không hợp lệ',
      '08': 'Kiểm tra checksum không hợp lệ',
      '09': 'Khách hàng hủy giao dịch',
      '10': 'Giao dịch timeout',
      '11': 'Hết hạn giao dịch'
    };
    
    return messages[responseCode] || 'Mã lỗi không xác định';
  }

  // Helper function để format date
  formatDate(date, format) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');

    return format
      .replace('yyyy', year)
      .replace('MM', month)
      .replace('dd', day)
      .replace('HH', hours)
      .replace('mm', minutes)
      .replace('ss', seconds);
  }
}

module.exports = PaymentService;
