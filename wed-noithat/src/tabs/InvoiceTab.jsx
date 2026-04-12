import React, { useState, useEffect } from 'react'
import { API_BASE_URL } from '../config.js'

// Màu sắc
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

// Trạng thái hóa đơn
const INVOICE_STATUS = {
  DRAFT: { color: COLORS.gray, text: 'Nháp' },
  PENDING: { color: COLORS.warning, text: 'Chờ xử lý' },
  ISSUED: { color: COLORS.info, text: 'Đã xuất' },
  PAID: { color: COLORS.success, text: 'Đã thanh toán' },
  CANCELLED: { color: COLORS.danger, text: 'Đã hủy' }
}

function InvoiceTab() {
  const [invoices, setInvoices] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedInvoice, setSelectedInvoice] = useState(null)
  const [search, setSearch] = useState('')
  const [toast, setToast] = useState(null)
  const [dateFilter, setDateFilter] = useState('')

  useEffect(() => {
    fetchInvoices()
  }, [])

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const fetchInvoices = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('adminToken')
      if (!token) {
        setError('Bạn cần đăng nhập')
        setLoading(false)
        return
      }

      const res = await fetch(`${API_BASE_URL}/admin/hoa-don`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải danh sách hóa đơn')
      
      const data = await res.json()
      setInvoices(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const searchInvoices = async () => {
    if (!search.trim()) {
      fetchInvoices()
      return
    }

    try {
      setLoading(true)
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/hoa-don/tim-kiem?keyword=${encodeURIComponent(search)}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Tìm kiếm thất bại')
      
      const data = await res.json()
      setInvoices(data)
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  const exportInvoice = async (invoiceId) => {
    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/hoa-don/${invoiceId}/xuat`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Xuất hóa đơn thất bại')

      showToast('Đã xuất hóa đơn thành công')
      fetchInvoices()
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const downloadPDF = async (invoiceId) => {
    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/hoa-don/${invoiceId}/pdf`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Tải PDF thất bại')

      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `hoa-don-${invoiceId}.pdf`
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      showToast('Đã tải hóa đơn PDF')
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
    const config = INVOICE_STATUS[status] || { color: COLORS.gray, text: status }
    return {
      backgroundColor: config.color,
      color: 'white',
      padding: '4px 12px',
      borderRadius: '20px',
      fontSize: '12px',
      fontWeight: 500
    }
  }

  // Lọc hóa đơn theo ngày
  const filteredInvoices = dateFilter
    ? invoices.filter(inv => {
        const invDate = new Date(inv.ngayTao).toISOString().split('T')[0]
        return invDate === dateFilter
      })
    : invoices

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
          onClick={fetchInvoices}
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

  // Chi tiết hóa đơn
  if (selectedInvoice) {
    return (
      <div style={{ padding: '20px' }}>
        {/* Toast */}
        {toast && (
          <div style={{
            position: 'fixed',
            top: '20px',
            right: '20px',
            padding: '12px 20px',
            borderRadius: '8px',
            backgroundColor: toast.type === 'success' ? COLORS.success : COLORS.danger,
            color: 'white',
            zIndex: 1000
          }}>
            {toast.message}
          </div>
        )}

        <button
          onClick={() => setSelectedInvoice(null)}
          style={{
            padding: '10px 20px',
            marginBottom: '20px',
            backgroundColor: COLORS.grayLight,
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '14px'
          }}
        >
          ← Quay lại
        </button>

        <div style={{
          backgroundColor: 'white',
          padding: '24px',
          borderRadius: '12px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        }}>
          <div style={{ 
            display: 'flex', 
            justifyContent: 'space-between', 
            alignItems: 'center', 
            marginBottom: '24px',
            paddingBottom: '20px',
            borderBottom: `1px solid ${COLORS.border}`
          }}>
            <div>
              <h2 style={{ margin: '0 0 8px' }}>HÓA ĐƠN #{selectedInvoice.maHoaDon || selectedInvoice.id}</h2>
              <p style={{ color: COLORS.gray, margin: 0 }}>Ngày tạo: {formatDate(selectedInvoice.ngayTao)}</p>
            </div>
            <span style={getStatusStyle(selectedInvoice.trangThai)}>
              {(INVOICE_STATUS[selectedInvoice.trangThai] || {}).text || selectedInvoice.trangThai}
            </span>
          </div>

          <div style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))',
            gap: '20px',
            marginBottom: '24px'
          }}>
            <div>
              <h4 style={{ color: COLORS.gray, marginBottom: '12px' }}>Thông tin khách hàng</h4>
              <p><strong>Tên:</strong> {selectedInvoice.tenKhachHang}</p>
              <p><strong>Email:</strong> {selectedInvoice.email}</p>
              <p><strong>SĐT:</strong> {selectedInvoice.soDienThoai}</p>
            </div>
            <div>
              <h4 style={{ color: COLORS.gray, marginBottom: '12px' }}>Thông tin đơn hàng</h4>
              <p><strong>Mã đơn hàng:</strong> #{selectedInvoice.donHangId}</p>
              <p><strong>Phương thức TT:</strong> {selectedInvoice.phuongThucThanhToan}</p>
              <p><strong>Ngày thanh toán:</strong> {selectedInvoice.ngayThanhToan ? formatDate(selectedInvoice.ngayThanhToan) : 'Chưa thanh toán'}</p>
            </div>
          </div>

          <h4 style={{ marginBottom: '12px' }}>Chi tiết sản phẩm</h4>
          <div style={{ border: `1px solid ${COLORS.border}`, borderRadius: '8px', overflow: 'hidden' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead>
                <tr style={{ backgroundColor: COLORS.grayLight }}>
                  <th style={{ padding: '12px', textAlign: 'left' }}>Sản phẩm</th>
                  <th style={{ padding: '12px', textAlign: 'center' }}>SL</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>Đơn giá</th>
                  <th style={{ padding: '12px', textAlign: 'right' }}>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                {(selectedInvoice.chiTiet || []).map((item, idx) => (
                  <tr key={idx} style={{ borderTop: `1px solid ${COLORS.border}` }}>
                    <td style={{ padding: '12px' }}>{item.tenSanPham}</td>
                    <td style={{ padding: '12px', textAlign: 'center' }}>{item.soLuong}</td>
                    <td style={{ padding: '12px', textAlign: 'right' }}>{formatMoney(item.donGia)}</td>
                    <td style={{ padding: '12px', textAlign: 'right' }}>{formatMoney(item.soLuong * item.donGia)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div style={{
            marginTop: '24px',
            padding: '20px',
            backgroundColor: COLORS.grayLight,
            borderRadius: '8px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span>Tạm tính:</span>
              <span>{formatMoney(selectedInvoice.tamTinh || selectedInvoice.tongTien)}</span>
            </div>
            {selectedInvoice.giamGia > 0 && (
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px', color: COLORS.success }}>
                <span>Giảm giá:</span>
                <span>-{formatMoney(selectedInvoice.giamGia)}</span>
              </div>
            )}
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '8px' }}>
              <span>Thuế VAT (10%):</span>
              <span>{formatMoney(selectedInvoice.thue || 0)}</span>
            </div>
            <div style={{ 
              display: 'flex', 
              justifyContent: 'space-between', 
              marginTop: '12px',
              paddingTop: '12px',
              borderTop: `2px solid ${COLORS.border}`,
              fontSize: '18px',
              fontWeight: 'bold'
            }}>
              <span>Tổng cộng:</span>
              <span style={{ color: COLORS.primary }}>{formatMoney(selectedInvoice.tongTien)}</span>
            </div>
          </div>

          {/* Actions */}
          <div style={{ marginTop: '24px', display: 'flex', gap: '12px', justifyContent: 'flex-end' }}>
            <button
              onClick={() => downloadPDF(selectedInvoice.id)}
              style={{
                padding: '12px 24px',
                backgroundColor: COLORS.info,
                color: 'white',
                border: 'none',
                borderRadius: '8px',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              📥 Tải PDF
            </button>
            {selectedInvoice.trangThai !== 'ISSUED' && selectedInvoice.trangThai !== 'PAID' && (
              <button
                onClick={() => exportInvoice(selectedInvoice.id)}
                style={{
                  padding: '12px 24px',
                  backgroundColor: COLORS.success,
                  color: 'white',
                  border: 'none',
                  borderRadius: '8px',
                  cursor: 'pointer',
                  fontSize: '14px'
                }}
              >
                ✓ Xuất hóa đơn
              </button>
            )}
          </div>
        </div>
      </div>
    )
  }

  // Danh sách hóa đơn
  return (
    <div style={{ padding: '20px' }}>
      {/* Toast */}
      {toast && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          padding: '12px 20px',
          borderRadius: '8px',
          backgroundColor: toast.type === 'success' ? COLORS.success : COLORS.danger,
          color: 'white',
          zIndex: 1000
        }}>
          {toast.message}
        </div>
      )}

      <div style={{ 
        display: 'flex', 
        justifyContent: 'space-between', 
        alignItems: 'center', 
        marginBottom: '20px' 
      }}>
        <h2 style={{ margin: 0 }}>📄 Quản lý hóa đơn</h2>
        <button
          onClick={fetchInvoices}
          style={{
            padding: '10px 20px',
            backgroundColor: COLORS.primary,
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer'
          }}
        >
          🔄 Làm mới
        </button>
      </div>

      {/* Filter & Search */}
      <div style={{ 
        display: 'flex', 
        gap: '12px', 
        marginBottom: '20px',
        flexWrap: 'wrap'
      }}>
        <div style={{ position: 'relative', flex: 1, minWidth: '200px' }}>
          <span style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)' }}>🔍</span>
          <input
            placeholder="Tìm kiếm hóa đơn..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && searchInvoices()}
            style={{
              width: '100%',
              padding: '10px 12px 10px 40px',
              borderRadius: '8px',
              border: `1px solid ${COLORS.border}`,
              fontSize: '14px',
              backgroundColor: COLORS.grayLight,
              outline: 'none'
            }}
          />
        </div>
        <input
          type="date"
          value={dateFilter}
          onChange={(e) => setDateFilter(e.target.value)}
          style={{
            padding: '10px 12px',
            borderRadius: '8px',
            border: `1px solid ${COLORS.border}`,
            fontSize: '14px'
          }}
        />
        <button
          onClick={searchInvoices}
          style={{
            padding: '10px 20px',
            backgroundColor: COLORS.primary,
            color: 'white',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer'
          }}
        >
          Tìm kiếm
        </button>
        {(search || dateFilter) && (
          <button
            onClick={() => {
              setSearch('')
              setDateFilter('')
              fetchInvoices()
            }}
            style={{
              padding: '10px 20px',
              backgroundColor: COLORS.grayLight,
              color: COLORS.gray,
              border: `1px solid ${COLORS.border}`,
              borderRadius: '8px',
              cursor: 'pointer'
            }}
          >
            Xóa bộ lọc
          </button>
        )}
      </div>

      {filteredInvoices.length === 0 ? (
        <div style={{ textAlign: 'center', padding: '40px', color: COLORS.gray }}>
          <p>Chưa có hóa đơn nào</p>
        </div>
      ) : (
        <div style={{
          display: 'grid',
          gap: '16px',
          gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))'
        }}>
          {filteredInvoices.map(invoice => (
            <div
              key={invoice.id}
              style={{
                backgroundColor: 'white',
                padding: '20px',
                borderRadius: '12px',
                boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
                cursor: 'pointer',
                transition: 'transform 0.2s, box-shadow 0.2s',
                border: `1px solid ${COLORS.border}`
              }}
              onClick={() => setSelectedInvoice(invoice)}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = 'translateY(-4px)'
                e.currentTarget.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = 'translateY(0)'
                e.currentTarget.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)'
              }}
            >
              <div style={{ 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center',
                marginBottom: '12px' 
              }}>
                <h4 style={{ margin: 0 }}>HD #{invoice.maHoaDon || invoice.id}</h4>
                <span style={getStatusStyle(invoice.trangThai)}>
                  {(INVOICE_STATUS[invoice.trangThai] || {}).text || invoice.trangThai}
                </span>
              </div>

              <div style={{ fontSize: '14px', color: COLORS.gray, marginBottom: '12px' }}>
                <p style={{ margin: '4px 0' }}>👤 {invoice.tenKhachHang}</p>
                <p style={{ margin: '4px 0' }}>📅 {formatDate(invoice.ngayTao)}</p>
                <p style={{ margin: '4px 0' }}>📦 Đơn hàng #{invoice.donHangId}</p>
              </div>

              <div style={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
                paddingTop: '12px',
                borderTop: `1px solid ${COLORS.border}`
              }}>
                <span style={{ fontSize: '18px', fontWeight: 'bold', color: COLORS.primary }}>
                  {formatMoney(invoice.tongTien)}
                </span>
                <span style={{ fontSize: '12px', color: COLORS.gray }}>
                  {(invoice.chiTiet || []).length} sản phẩm →
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

export { InvoiceTab }
