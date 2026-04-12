import React, { useState, useEffect } from 'react'
import { API_BASE_URL } from '../config.js'

// Màu sắc chính
const COLORS = {
  primary: '#4f46e5',
  success: '#10b981',
  danger: '#ef4444',
  warning: '#f59e0b',
  info: '#3b82f6',
  gray: '#6b7280',
  grayLight: '#f3f4f6',
  border: '#e5e7eb'
}

// Trạng thái đơn hàng
const ORDER_STATUS = {
  CHO_XAC_NHAN: { color: COLORS.warning, text: 'Chờ xác nhận' },
  DA_XAC_NHAN: { color: COLORS.info, text: 'Đã xác nhận' },
  DANG_GIAO: { color: COLORS.primary, text: 'Đang giao' },
  DA_GIAO: { color: COLORS.success, text: 'Đã giao' },
  THANH_CONG: { color: COLORS.success, text: 'Thành công' },
  HUY: { color: COLORS.danger, text: 'Đã hủy' }
}

const PAYMENT_STATUS = {
  CHUA_THANH_TOAN: 'Chưa thanh toán',
  DA_THANH_TOAN: 'Đã thanh toán'
}

function OrderTab() {
  const [orders, setOrders] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedOrder, setSelectedOrder] = useState(null)
  const [showCancelModal, setShowCancelModal] = useState(false)
  const [cancelReason, setCancelReason] = useState('')
  const [toast, setToast] = useState(null)

  useEffect(() => {
    fetchOrders()
  }, [])

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const fetchOrders = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('adminToken')
      if (!token) {
        setError('Bạn cần đăng nhập')
        setLoading(false)
        return
      }

      const res = await fetch(`${API_BASE_URL}/don-hang`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải đơn hàng')
      const data = await res.json()
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
      const res = await fetch(`${API_BASE_URL}/don-hang/${orderId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải chi tiết')
      const data = await res.json()
      setSelectedOrder(data)
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const cancelOrder = async () => {
    if (!cancelReason.trim()) {
      showToast('Vui lòng nhập lý do hủy', 'error')
      return
    }

    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/don-hang/${selectedOrder.id}/huy`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ lyDo: cancelReason })
      })

      if (!res.ok) throw new Error('Hủy đơn hàng thất bại')

      showToast('Đã hủy đơn hàng thành công')
      setShowCancelModal(false)
      setCancelReason('')
      setSelectedOrder(null)
      fetchOrders()
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const formatMoney = (amount) => {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(amount)
  }

  const formatDate = (date) => {
    return new Date(date).toLocaleDateString('vi-VN')
  }

  const getStatusStyle = (status) => {
    const config = ORDER_STATUS[status] || { color: COLORS.gray, text: status }
    return {
      backgroundColor: config.color,
      color: 'white',
      padding: '4px 12px',
      borderRadius: '20px',
      fontSize: '12px',
      fontWeight: 500
    }
  }

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: '40px' }}>
        <div style={{
          width: '40px',
          height: '40px',
          border: `3px solid ${COLORS.border}`,
          borderTop: `3px solid ${COLORS.primary}`,
          borderRadius: '50%',
          animation: 'spin 1s linear infinite',
          margin: '0 auto 16px'
        }} />
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        <p>Đang tải...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div style={{ textAlign: 'center', padding: '40px', color: COLORS.danger }}>
        <p>{error}</p>
        <button
          onClick={fetchOrders}
          style={{
            padding: '10px 20px',
            backgroundColor: COLORS.primary,
            color: 'white',
            border: 'none',
            borderRadius: '8px',
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
