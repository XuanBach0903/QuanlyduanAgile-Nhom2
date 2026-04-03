import React, { useState, useEffect } from 'react'
import { config } from '../config'

function OrderTab() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [showDetail, setShowDetail] = useState(false)

  useEffect(() => {
    fetchOrders()
  }, [])

  const fetchOrders = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('adminToken')
      
      if (!token) {
        setError('Bạn cần đăng nhập để xem đơn hàng')
        setLoading(false)
        return
      }

      const response = await fetch(`${config.API_BASE_URL}/don-hang`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error('Không thể tải danh sách đơn hàng')
      }

      const data = await response.json()
      setOrders(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const fetchOrderDetail = async (orderId) => {
    try {
      const token = localStorage.getItem('adminToken')
      
      const response = await fetch(`${config.API_BASE_URL}/don-hang/${orderId}`, {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        }
      })

      if (!response.ok) {
        throw new Error('Không thể tải chi tiết đơn hàng')
      }

      const data = await response.json()
      setSelectedOrder(data)
      setShowDetail(true)
    } catch (err) {
      setError(err.message)
    }
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'CHO_XAC_NHAN':
        return '#f59e0b'
      case 'DA_XAC_NHAN':
        return '#3b82f6'
      case 'DANG_GIAO':
        return '#8b5cf6'
      case 'DA_GIAO_CHO_XAC_NHAN':
        return '#06b6d4'
      case 'THANH_CONG':
        return '#10b981'
      case 'HUY':
        return '#ef4444'
      case 'CHO_XU_LY_HOAN':
        return '#f97316'
      default:
        return '#6b7280'
    }
  }

  const getStatusText = (status) => {
    switch (status) {
      case 'CHO_XAC_NHAN':
        return 'Chờ xác nhận'
      case 'DA_XAC_NHAN':
        return 'Đã xác nhận'
      case 'DANG_GIAO':
        return 'Đang giao'
      case 'DA_GIAO_CHO_XAC_NHAN':
        return 'Đã giao - Chờ xác nhận'
      case 'THANH_CONG':
        return 'Thành công'
      case 'HUY':
        return 'Đã hủy'
      case 'CHO_XU_LY_HOAN':
        return 'Chờ xử lý hoàn'
      default:
        return status
    }
  }

  const getPaymentStatusText = (status) => {
    switch (status) {
      case 'CHUA_THANH_TOAN':
        return 'Chưa thanh toán'
      case 'DA_THANH_TOAN':
        return 'Đã thanh toán'
      default:
        return status
    }
  }

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleString('vi-VN')
  }

  const formatCurrency = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount)
  }

  const huyDonHang = async (orderId, lyDo) => {
    try {
      const token = localStorage.getItem('adminToken')
      
      const response = await fetch(`${config.API_BASE_URL}/don-hang/${orderId}/huy`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ lyDo })
      })

      if (!response.ok) {
        const error = await response.json()
        throw new Error(error.message || 'Không thể hủy đơn hàng')
      }

      const data = await response.json()
      alert(data.message || 'Đã hủy đơn hàng thành công')
      fetchOrders() // Refresh danh sách
    } catch (err) {
      alert(err.message)
    }
  }

  const handleHuyDon = (orderId) => {
    const reasons = [
      'Thay đổi ý định',
      'Tìm được sản phẩm tốt hơn',
      'Không còn nhu cầu',
      'Vấn đề về thanh toán',
      'Thời gian giao hàng quá lâu',
      'Lý do khác'
    ]

    let selectedReason = reasons[0]
    let customReason = ''

    const dialog = document.createElement('div')
    dialog.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    `

    const content = document.createElement('div')
    content.style.cssText = `
      background: white;
      padding: 20px;
      border-radius: 8px;
      max-width: 400px;
      width: 90%;
    `

    content.innerHTML = `
      <h3 style="margin: 0 0 15px 0;">Hủy đơn hàng</h3>
      <p style="margin: 0 0 15px 0; color: #6b7280;">Vui lòng chọn lý do hủy đơn:</p>
      <div id="reason-list" style="margin-bottom: 15px;"></div>
      <input type="text" id="custom-reason" placeholder="Nhập lý do khác" style="
        width: 100%;
        padding: 8px;
        border: 1px solid #d1d5db;
        border-radius: 4px;
        margin-bottom: 15px;
        display: none;
      ">
      <div style="display: flex; gap: 10px; justify-content: flex-end;">
        <button id="btn-cancel" style="
          padding: 8px 16px;
          border: 1px solid #d1d5db;
          background: white;
          border-radius: 4px;
          cursor: pointer;
        ">Đóng</button>
        <button id="btn-confirm" style="
          padding: 8px 16px;
          border: none;
          background: #ef4444;
          color: white;
          border-radius: 4px;
          cursor: pointer;
        ">Hủy đơn</button>
      </div>
    `

    const reasonList = content.querySelector('#reason-list')
    const customReasonInput = content.querySelector('#custom-reason')
    
    reasons.forEach((reason, index) => {
      const radioDiv = document.createElement('div')
      radioDiv.style.cssText = 'margin-bottom: 8px;'
      radioDiv.innerHTML = `
        <label style="display: flex; align-items: center; cursor: pointer;">
          <input type="radio" name="reason" value="${reason}" ${index === 0 ? 'checked' : ''} style="margin-right: 8px;">
          <span>${reason}</span>
        </label>
      `
      reasonList.appendChild(radioDiv)
    })

    const radios = reasonList.querySelectorAll('input[type="radio"]')
    radios.forEach(radio => {
      radio.addEventListener('change', (e) => {
        selectedReason = e.target.value
        customReasonInput.style.display = selectedReason === 'Lý do khác' ? 'block' : 'none'
      })
    })

    content.querySelector('#btn-cancel').addEventListener('click', () => {
      document.body.removeChild(dialog)
    })

    content.querySelector('#btn-confirm').addEventListener('click', () => {
      let finalReason = selectedReason
      if (selectedReason === 'Lý do khác') {
        customReason = customReasonInput.value.trim()
        if (!customReason) {
          alert('Vui lòng nhập lý do hủy đơn')
          return
        }
        finalReason = customReason
      }
      
      if (confirm(`Bạn có chắc chắn muốn hủy đơn hàng với lý do: "${finalReason}"?`)) {
        huyDonHang(orderId, finalReason)
        document.body.removeChild(dialog)
      }
    })

    dialog.appendChild(content)
    document.body.appendChild(dialog)

    dialog.addEventListener('click', (e) => {
      if (e.target === dialog) {
        document.body.removeChild(dialog)
      }
    })
  }

  const thanhToanDonHang = async (orderId, phuongThuc) => {
    try {
      const token = localStorage.getItem('adminToken')
      
      if (phuongThuc === 'COD') {
        // Xử lý thanh toán COD
        const response = await fetch(`${config.API_BASE_URL}/payment/cod/confirm`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ 
            orderId: orderId,
            amount: selectedOrder?.tongTien || 0,
            orderInfo: `Thanh toán COD đơn hàng ${orderId}`
          })
        })

        if (!response.ok) {
          const error = await response.json()
          throw new Error(error.message || 'Không thể xác nhận thanh toán COD')
        }

        const data = await response.json()
        alert(data.message || 'Đã xác nhận thanh toán COD thành công')
        fetchOrders()
        setShowDetail(false)
      } else if (phuongThuc === 'VNPAY') {
        // Tạo URL thanh toán VNPAY
        const response = await fetch(`${config.API_BASE_URL}/payment/vnpay/create`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ 
            orderId: orderId,
            amount: selectedOrder?.tongTien || 0,
            orderInfo: `Thanh toán VNPAY đơn hàng ${orderId}`,
            returnUrl: `${window.location.origin}/payment/success`,
            ipAddr: '127.0.0.1'
          })
        })

        if (!response.ok) {
          const error = await response.json()
          throw new Error(error.message || 'Không thể tạo thanh toán VNPAY')
        }

        const data = await response.json()
        if (data.paymentUrl) {
          // Chuyển hướng đến trang thanh toán VNPAY
          window.open(data.paymentUrl, '_blank')
          alert('Đang mở trang thanh toán VNPAY. Vui lòng hoàn tất thanh toán và quay lại để cập nhật trạng thái.')
        }
      } else if (phuongThuc === 'PAYPAL') {
        // Tạo URL thanh toán PayPal
        const response = await fetch(`${config.API_BASE_URL}/payment/paypal/create`, {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ 
            orderId: orderId,
            amount: selectedOrder?.tongTien || 0,
            orderInfo: `Thanh toán PayPal đơn hàng ${orderId}`,
            returnUrl: `${window.location.origin}/payment/success`,
            cancelUrl: `${window.location.origin}/payment/cancel`
          })
        })

        if (!response.ok) {
          const error = await response.json()
          throw new Error(error.message || 'Không thể tạo thanh toán PayPal')
        }

        const data = await response.json()
        if (data.paymentUrl) {
          // Chuyển hướng đến trang thanh toán PayPal
          window.open(data.paymentUrl, '_blank')
          alert('Đang mở trang thanh toán PayPal. Vui lòng hoàn tất thanh toán và quay lại để cập nhật trạng thái.')
        }
      }
    } catch (err) {
      alert(err.message)
    }
  }

  const showPaymentDialog = (orderId) => {
    const dialog = document.createElement('div')
    dialog.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    `

    const content = document.createElement('div')
    content.style.cssText = `
      background: white;
      padding: 20px;
      border-radius: 8px;
      max-width: 400px;
      width: 90%;
    `

    content.innerHTML = `
      <h3 style="margin: 0 0 15px 0;">Chọn phương thức thanh toán</h3>
      <p style="margin: 0 0 15px 0; color: #6b7280;">Vui lòng chọn phương thức thanh toán:</p>
      <div style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px;">
        <button id="btn-cod" style="
          padding: 12px;
          border: 1px solid #d1d5db;
          background: white;
          border-radius: 4px;
          cursor: pointer;
          text-align: left;
        ">
          <div style="font-weight: bold;">💵 Thanh toán COD</div>
          <div style="font-size: 12px; color: #6b7280;">Thanh toán khi nhận hàng</div>
        </button>
        <button id="btn-vnpay" style="
          padding: 12px;
          border: 1px solid #d1d5db;
          background: white;
          border-radius: 4px;
          cursor: pointer;
          text-align: left;
        ">
          <div style="font-weight: bold;">🏦 Thanh toán VNPAY</div>
          <div style="font-size: 12px; color: #6b7280;">Thẻ ATM/Visa/Mastercard</div>
        </button>
        <button id="btn-paypal" style="
          padding: 12px;
          border: 1px solid #d1d5db;
          background: white;
          border-radius: 4px;
          cursor: pointer;
          text-align: left;
        ">
          <div style="font-weight: bold;">💳 Thanh toán PayPal</div>
          <div style="font-size: 12px; color: #6b7280;">Thanh toán quốc tế</div>
        </button>
      </div>
      <div style="display: flex; gap: 10px; justify-content: flex-end;">
        <button id="btn-cancel" style="
          padding: 8px 16px;
          border: 1px solid #d1d5db;
          background: white;
          border-radius: 4px;
          cursor: pointer;
        ">Hủy</button>
      </div>
    `

    content.querySelector('#btn-cod').addEventListener('click', () => {
      thanhToanDonHang(orderId, 'COD')
      document.body.removeChild(dialog)
    })

    content.querySelector('#btn-vnpay').addEventListener('click', () => {
      thanhToanDonHang(orderId, 'VNPAY')
      document.body.removeChild(dialog)
    })

    content.querySelector('#btn-paypal').addEventListener('click', () => {
      thanhToanDonHang(orderId, 'PAYPAL')
      document.body.removeChild(dialog)
    })

    content.querySelector('#btn-cancel').addEventListener('click', () => {
      document.body.removeChild(dialog)
    })

    dialog.appendChild(content)
    document.body.appendChild(dialog)

    dialog.addEventListener('click', (e) => {
      if (e.target === dialog) {
        document.body.removeChild(dialog)
      }
    })
  }

  const xemTrangThaiThanhToan = async (orderId) => {
    try {
      const token = localStorage.getItem('adminToken')
      
      const response = await fetch(`${config.API_BASE_URL}/payment/status/${orderId}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      })

      if (!response.ok) {
        const error = await response.json()
        throw new Error(error.message || 'Không thể tải trạng thái thanh toán')
      }

      const data = await response.json()
      showPaymentStatusDialog(data)
    } catch (err) {
      alert(err.message)
    }
  }

  const showPaymentStatusDialog = (paymentInfo) => {
    const dialog = document.createElement('div')
    dialog.style.cssText = `
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0,0,0,0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    `

    const content = document.createElement('div')
    content.style.cssText = `
      background: white;
      padding: 24px;
      border-radius: 12px;
      max-width: 500px;
      width: 90%;
      max-height: 80vh;
      overflow-y: auto;
    `

    const statusColor = {
      'DA_THANH_TOAN': '#10b981',
      'CHUA_THANH_TOAN': '#f59e0b',
      'DANG_XU_LY': '#3b82f6',
      'THAT_BAI': '#ef4444',
      'HET_HAN': '#ef4444'
    }[paymentInfo.paymentStatus] || '#6b7280'

    const statusText = {
      'DA_THANH_TOAN': 'Đã thanh toán',
      'CHUA_THANH_TOAN': 'Chưa thanh toán',
      'DANG_XU_LY': 'Đang xử lý',
      'THAT_BAI': 'Thất bại',
      'HET_HAN': 'Hết hạn'
    }[paymentInfo.paymentStatus] || paymentInfo.paymentStatus

    const methodText = {
      'COD': 'Thanh toán khi nhận hàng',
      'VNPAY': 'Ví điện tử VNPAY',
      'VISA': 'Thẻ Visa/Mastercard',
      'PAYPAL': 'PayPal'
    }[paymentInfo.paymentMethod] || paymentInfo.paymentMethod

    content.innerHTML = `
      <h3 style="margin: 0 0 20px 0; color: #111827;">Trạng thái thanh toán</h3>
      
      <div style="display: grid; gap: 16px;">
        <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
          <span style="color: #6b7280;">Mã đơn hàng:</span>
          <span style="font-weight: 600;">#${paymentInfo.orderId}</span>
        </div>
        
        <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
          <span style="color: #6b7280;">Phương thức:</span>
          <span style="font-weight: 600;">${methodText}</span>
        </div>
        
        <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
          <span style="color: #6b7280;">Trạng thái:</span>
          <span style="font-weight: 600; color: ${statusColor};">${statusText}</span>
        </div>
        
        <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
          <span style="color: #6b7280;">Số tiền:</span>
          <span style="font-weight: 600; color: #111827;">${formatCurrency(paymentInfo.amount)}</span>
        </div>
        
        <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
          <span style="color: #6b7280;">Ngày đặt hàng:</span>
          <span style="font-weight: 600;">${new Date(paymentInfo.orderDate).toLocaleString('vi-VN')}</span>
        </div>
        
        ${paymentInfo.transactionId ? `
        <div style="display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #e5e7eb;">
          <span style="color: #6b7280;">Mã giao dịch:</span>
          <span style="font-weight: 600; font-family: monospace;">${paymentInfo.transactionId}</span>
        </div>
        ` : ''}
      </div>
      
      <div style="margin-top: 24px; display: flex; gap: 12px; justify-content: flex-end;">
        ${paymentInfo.paymentStatus === 'CHUA_THANH_TOAN' ? `
          <button id="btn-pay-now" style="
            padding: 10px 20px;
            background: #10b981;
            color: white;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-weight: 600;
          ">Thanh toán ngay</button>
        ` : ''}
        
        <button id="btn-close" style="
          padding: 10px 20px;
          background: #f3f4f6;
          color: #374151;
          border: 1px solid #d1d5db;
          border-radius: 6px;
          cursor: pointer;
          font-weight: 600;
        ">Đóng</button>
      </div>
    `

    content.querySelector('#btn-close').addEventListener('click', () => {
      document.body.removeChild(dialog)
    })

    if (paymentInfo.paymentStatus === 'CHUA_THANH_TOAN') {
      content.querySelector('#btn-pay-now').addEventListener('click', () => {
        document.body.removeChild(dialog)
        showPaymentDialog(paymentInfo.orderId)
      })
    }

    dialog.appendChild(content)
    document.body.appendChild(dialog)

    dialog.addEventListener('click', (e) => {
      if (e.target === dialog) {
        document.body.removeChild(dialog)
      }
    })
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px' }}>
        <div>Đang tải đơn hàng...</div>
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ textAlign: 'center', padding: '40px' }}>
        <div style={{ color: '#ef4444', marginBottom: '16px' }}>{error}</div>
        <button
          onClick={fetchOrders}
          style={{
            padding: '8px 16px',
            backgroundColor: '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer'
          }}
        >
          Thử lại
        </button>
      </div>
    )
  }

  if (showDetail && selectedOrder) {
    return (
      <div style={{ padding: '20px' }}>
        <div style={{ marginBottom: '20px' }}>
          <button
            onClick={() => setShowDetail(false)}
            style={{
              padding: '8px 16px',
              backgroundColor: '#6b7280',
              color: 'white',
              border: 'none',
              borderRadius: '4px',
              cursor: 'pointer'
            }}
          >
            ← Quay lại
          </button>
        </div>

        <div style={{ 
          backgroundColor: 'white', 
          padding: '20px', 
          borderRadius: '8px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <h2 style={{ marginBottom: '20px' }}>Chi tiết đơn hàng #{selectedOrder.id}</h2>
          
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px', marginBottom: '20px' }}>
            <div>
              <h4>Thông tin giao hàng</h4>
              <p><strong>Địa chỉ:</strong> {selectedOrder.diaChiGiaoHang}</p>
              <p><strong>Số điện thoại:</strong> {selectedOrder.soDienThoai}</p>
              <p><strong>Ghi chú:</strong> {selectedOrder.ghiChu || 'Không có'}</p>
            </div>
            
            <div>
              <h4>Thông tin thanh toán</h4>
              <p><strong>Phương thức:</strong> {selectedOrder.phuongThucThanhToan}</p>
              <p><strong>Trạng thái:</strong> {getPaymentStatusText(selectedOrder.trangThaiThanhToan)}</p>
              <p><strong>Tổng tiền:</strong> {formatCurrency(selectedOrder.tongTien)}</p>
            </div>
          </div>

          <div style={{ marginBottom: '20px' }}>
            <h4>Trạng thái đơn hàng</h4>
            <div style={{
              display: 'inline-block',
              padding: '4px 12px',
              borderRadius: '4px',
              backgroundColor: getStatusColor(selectedOrder.trangThaiDonHang),
              color: 'white',
              fontSize: '14px'
            }}>
              {getStatusText(selectedOrder.trangThaiDonHang)}
            </div>
          </div>

          {selectedOrder.lyDoHuy && (
            <div style={{ marginBottom: '20px' }}>
              <h4>Lý do hủy</h4>
              <p style={{ color: '#ef4444' }}>{selectedOrder.lyDoHuy}</p>
            </div>
          )}

          {selectedOrder.lyDoHoan && (
            <div style={{ marginBottom: '20px' }}>
              <h4>Lý do hoàn hàng</h4>
              <p style={{ color: '#f97316' }}>{selectedOrder.lyDoHoan}</p>
            </div>
          )}

          <div>
            <h4>Sản phẩm</h4>
            <div style={{ border: '1px solid #e5e7eb', borderRadius: '4px' }}>
              {selectedOrder.items.map((item, index) => (
                <div
                  key={index}
                  style={{
                    display: 'flex',
                    padding: '12px',
                    borderBottom: index < selectedOrder.items.length - 1 ? '1px solid #e5e7eb' : 'none',
                    alignItems: 'center'
                  }}
                >
                  <img
                    src={item.hinhDaiDien}
                    alt={item.tenSanPham}
                    style={{
                      width: '60px',
                      height: '60px',
                      objectFit: 'cover',
                      borderRadius: '4px',
                      marginRight: '12px'
                    }}
                  />
                  <div style={{ flex: 1 }}>
                    <h5 style={{ margin: '0 0 4px 0' }}>{item.tenSanPham}</h5>
                    <p style={{ margin: '0', color: '#6b7280', fontSize: '14px' }}>
                      Số lượng: {item.soLuong} × {formatCurrency(item.donGia)}
                    </p>
                    {item.danhGia && (
                      <p style={{ margin: '4px 0 0 0', color: '#10b981', fontSize: '12px' }}>
                        Đã đánh giá: {item.danhGia.soSao}/5 ⭐
                      </p>
                    )}
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <strong>{formatCurrency(item.soLuong * item.donGia)}</strong>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div style={{ marginTop: '20px', textAlign: 'right' }}>
            <p style={{ fontSize: '18px', fontWeight: 'bold' }}>
              Tổng cộng: {formatCurrency(selectedOrder.tongTien)}
            </p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'flex-end', marginTop: '12px' }}>
              <button
                style={{
                  padding: '8px 16px',
                  backgroundColor: '#6366f1',
                  color: 'white',
                  border: 'none',
                  borderRadius: '4px',
                  cursor: 'pointer',
                  fontSize: '14px'
                }}
                onClick={() => xemTrangThaiThanhToan(selectedOrder.id)}
              >
                Xem trạng thái thanh toán
              </button>
              {selectedOrder.trangThaiThanhToan === 'CHUA_THANH_TOAN' && (
                <button
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#10b981',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '14px'
                  }}
                  onClick={() => showPaymentDialog(selectedOrder.id)}
                >
                  Thanh toán ngay
                </button>
              )}
              {['CHO_XAC_NHAN', 'DA_XAC_NHAN'].includes(selectedOrder.trangThaiDonHang) && (
                <button
                  style={{
                    padding: '8px 16px',
                    backgroundColor: '#ef4444',
                    color: 'white',
                    border: 'none',
                    borderRadius: '4px',
                    cursor: 'pointer',
                    fontSize: '14px'
                  }}
                  onClick={() => handleHuyDon(selectedOrder.id)}
                >
                  Hủy đơn hàng
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={{ padding: '20px' }}>
      <h2 style={{ marginBottom: '20px' }}>Danh sách đơn hàng</h2>
      
      {orders.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>
          Chưa có đơn hàng nào
        </div>
      ) : (
        <div style={{ 
          display: 'grid', 
          gap: '16px',
          gridTemplateColumns: 'repeat(auto-fill, minmax(300px, 1fr))'
        }}>
          {orders.map((order) => (
            <div
              key={order.id}
              style={{
                backgroundColor: 'white',
                padding: '16px',
                borderRadius: '8px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                cursor: 'pointer',
                transition: 'transform 0.2s',
                border: '1px solid #e5e7eb'
              }}
              onClick={() => fetchOrderDetail(order.id)}
              onMouseEnter={(e) => e.currentTarget.style.transform = 'translateY(-2px)'}
              onMouseLeave={(e) => e.currentTarget.style.transform = 'translateY(0)'}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', marginBottom: '12px' }}>
                <h4 style={{ margin: '0' }}>Đơn hàng #{order.id}</h4>
                <div style={{
                  padding: '4px 8px',
                  borderRadius: '4px',
                  backgroundColor: getStatusColor(order.trangThaiDonHang),
                  color: 'white',
                  fontSize: '12px',
                  whiteSpace: 'nowrap'
                }}>
                  {getStatusText(order.trangThaiDonHang)}
                </div>
              </div>
              
              <div style={{ fontSize: '14px', color: '#6b7280' }}>
                <p style={{ margin: '4px 0' }}>
                  <strong>Ngày đặt:</strong> {formatDate(order.ngayTao)}
                </p>
                <p style={{ margin: '4px 0' }}>
                  <strong>Phương thức:</strong> {order.phuongThucThanhToan}
                </p>
                <p style={{ margin: '4px 0' }}>
                  <strong>Thanh toán:</strong> {getPaymentStatusText(order.trangThaiThanhToan)}
                </p>
              </div>
              
              <div style={{ 
                marginTop: '12px', 
                paddingTop: '12px', 
                borderTop: '1px solid #e5e7eb',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <span style={{ fontSize: '16px', fontWeight: 'bold', color: '#111827' }}>
                  {formatCurrency(order.tongTien)}
                </span>
                <div style={{ display: 'flex', gap: '8px' }}>
                  {order.trangThaiThanhToan === 'CHUA_THANH_TOAN' && (
                    <button
                      style={{
                        padding: '6px 12px',
                        backgroundColor: '#10b981',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '12px'
                      }}
                      onClick={(e) => {
                        e.stopPropagation()
                        showPaymentDialog(order.id)
                      }}
                    >
                      Thanh toán
                    </button>
                  )}
                  <button
                    style={{
                      padding: '6px 12px',
                      backgroundColor: '#3b82f6',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                    onClick={(e) => {
                      e.stopPropagation()
                      xemTrangThaiThanhToan(order.id)
                    }}
                  >
                    Trạng thái
                  </button>
                  {['CHO_XAC_NHAN', 'DA_XAC_NHAN'].includes(order.trangThaiDonHang) && (
                    <button
                      style={{
                        padding: '6px 12px',
                        backgroundColor: '#ef4444',
                        color: 'white',
                        border: 'none',
                        borderRadius: '4px',
                        cursor: 'pointer',
                        fontSize: '12px'
                      }}
                      onClick={(e) => {
                        e.stopPropagation()
                        handleHuyDon(order.id)
                      }}
                    >
                      Hủy đơn
                    </button>
                  )}
                  <button
                    style={{
                      padding: '6px 12px',
                      backgroundColor: '#3b82f6',
                      color: 'white',
                      border: 'none',
                      borderRadius: '4px',
                      cursor: 'pointer',
                      fontSize: '12px'
                    }}
                    onClick={(e) => {
                      e.stopPropagation()
                      fetchOrderDetail(order.id)
                    }}
                  >
                    Xem chi tiết
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export { OrderTab }
