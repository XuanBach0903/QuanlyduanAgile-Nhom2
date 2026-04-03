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
          ))}
        </div>
      )}
    </div>
  )
}

export { OrderTab }
