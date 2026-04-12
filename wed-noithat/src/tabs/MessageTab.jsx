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

function MessageTab() {
  const [chatRooms, setChatRooms] = useState([])
  const [selectedRoom, setSelectedRoom] = useState(null)
  const [messages, setMessages] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [newMessage, setNewMessage] = useState('')
  const [search, setSearch] = useState('')
  const [stats, setStats] = useState(null)
  const [toast, setToast] = useState(null)
  const [unreadCount, setUnreadCount] = useState(0)

  useEffect(() => {
    fetchChatRooms()
    fetchStats()
    fetchUnreadCount()
  }, [])

  useEffect(() => {
    if (selectedRoom) {
      fetchMessages(selectedRoom.userId)
      markAsRead(selectedRoom.userId)
    }
  }, [selectedRoom])

  const showToast = (message, type = 'success') => {
    setToast({ message, type })
    setTimeout(() => setToast(null), 3000)
  }

  const fetchChatRooms = async () => {
    try {
      setLoading(true)
      const token = localStorage.getItem('adminToken')
      if (!token) {
        setError('Bạn cần đăng nhập')
        return
      }

      const res = await fetch(`${API_BASE_URL}/admin/phong-chat`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải danh sách phòng chat')
      
      const data = await res.json()
      setChatRooms(data)
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const fetchStats = async () => {
    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/thong-ke`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải thống kê')
      
      const data = await res.json()
      setStats(data)
    } catch (err) {
      console.error(err)
    }
  }

  const fetchUnreadCount = async () => {
    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/chua-doc`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải số tin chưa đọc')
      
      const data = await res.json()
      setUnreadCount(data.count || 0)
    } catch (err) {
      console.error(err)
    }
  }

  const fetchMessages = async (userId) => {
    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/lich-su?userId=${userId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Không thể tải lịch sử chat')
      
      const data = await res.json()
      setMessages(data)
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const sendMessage = async () => {
    if (!newMessage.trim() || !selectedRoom) return

    try {
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/tin-nhan`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          userId: selectedRoom.userId,
          noiDung: newMessage,
          loai: 'TEXT'
        })
      })

      if (!res.ok) throw new Error('Gửi tin nhắn thất bại')

      setNewMessage('')
      fetchMessages(selectedRoom.userId)
      showToast('Đã gửi tin nhắn')
    } catch (err) {
      showToast(err.message, 'error')
    }
  }

  const markAsRead = async (userId) => {
    try {
      const token = localStorage.getItem('adminToken')
      await fetch(`${API_BASE_URL}/admin/da-doc`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ userId })
      })
      fetchUnreadCount()
      fetchChatRooms()
    } catch (err) {
      console.error(err)
    }
  }

  const searchMessages = async () => {
    if (!search.trim()) {
      fetchChatRooms()
      return
    }

    try {
      setLoading(true)
      const token = localStorage.getItem('adminToken')
      const res = await fetch(`${API_BASE_URL}/admin/tim-kiem?keyword=${encodeURIComponent(search)}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      })

      if (!res.ok) throw new Error('Tìm kiếm thất bại')
      
      const data = await res.json()
      // Group by user
      const grouped = data.reduce((acc, msg) => {
        const userId = msg.nguoiGui === 'ADMIN' ? msg.nguoiNhan : msg.nguoiGui
        if (!acc[userId]) {
          acc[userId] = {
            userId,
            tenKhachHang: msg.tenNguoiGui || msg.tenNguoiNhan || `User ${userId}`,
            messages: []
          }
        }
        acc[userId].messages.push(msg)
        return acc
      }, {})
      setChatRooms(Object.values(grouped))
    } catch (err) {
      showToast(err.message, 'error')
    } finally {
      setLoading(false)
    }
  }

  const formatTime = (date) => {
    return new Date(date).toLocaleTimeString('vi-VN', { 
      hour: '2-digit', 
      minute: '2-digit',
      day: '2-digit',
      month: '2-digit'
    })
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
          onClick={fetchChatRooms}
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

  return (
    <div style={{ padding: '20px', height: 'calc(100vh - 150px)' }}>
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

      {/* Stats */}
      {stats && (
        <div style={{ 
          display: 'flex', 
          gap: '16px', 
          marginBottom: '20px',
          flexWrap: 'wrap'
        }}>
          <div style={{
            backgroundColor: 'white',
            padding: '16px 24px',
            borderRadius: '12px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px'
          }}>
            <span style={{ fontSize: '24px' }}>💬</span>
            <div>
              <p style={{ margin: 0, color: COLORS.gray, fontSize: '14px' }}>Tổng tin nhắn</p>
              <p style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>{stats.tongTinNhan || 0}</p>
            </div>
          </div>
          <div style={{
            backgroundColor: 'white',
            padding: '16px 24px',
            borderRadius: '12px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px'
          }}>
            <span style={{ fontSize: '24px' }}>👥</span>
            <div>
              <p style={{ margin: 0, color: COLORS.gray, fontSize: '14px' }}>Khách hàng</p>
              <p style={{ margin: 0, fontSize: '20px', fontWeight: 'bold' }}>{stats.tongKhachHang || 0}</p>
            </div>
          </div>
          <div style={{
            backgroundColor: unreadCount > 0 ? '#fef2f2' : 'white',
            padding: '16px 24px',
            borderRadius: '12px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            border: unreadCount > 0 ? `1px solid ${COLORS.danger}` : 'none'
          }}>
            <span style={{ fontSize: '24px' }}>🔔</span>
            <div>
              <p style={{ margin: 0, color: unreadCount > 0 ? COLORS.danger : COLORS.gray, fontSize: '14px' }}>
                Chưa đọc
              </p>
              <p style={{ margin: 0, fontSize: '20px', fontWeight: 'bold', color: unreadCount > 0 ? COLORS.danger : 'inherit' }}>
                {unreadCount}
              </p>
            </div>
          </div>
        </div>
      )}

      <div style={{ display: 'flex', gap: '20px', height: '100%' }}>
        {/* Sidebar - Chat Rooms */}
        <div style={{ 
          width: '320px', 
          backgroundColor: 'white',
          borderRadius: '12px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          display: 'flex',
          flexDirection: 'column'
        }}>
          <div style={{ padding: '16px', borderBottom: `1px solid ${COLORS.border}` }}>
            <h3 style={{ margin: '0 0 12px' }}>💬 Tin nhắn</h3>
            <div style={{ position: 'relative' }}>
              <span style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)' }}>🔍</span>
              <input
                placeholder="Tìm kiếm..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && searchMessages()}
                style={{
                  width: '100%',
                  padding: '8px 10px 8px 32px',
                  borderRadius: '20px',
                  border: `1px solid ${COLORS.border}`,
                  fontSize: '14px',
                  outline: 'none'
                }}
              />
            </div>
          </div>

          <div style={{ flex: 1, overflowY: 'auto' }}>
            {chatRooms.length === 0 ? (
              <div style={{ textAlign: 'center', padding: '40px 20px', color: COLORS.gray }}>
                <p>Chưa có tin nhắn</p>
              </div>
            ) : (
              chatRooms.map((room, index) => (
                <div
                  key={room.userId || index}
                  onClick={() => setSelectedRoom(room)}
                  style={{
                    padding: '16px',
                    borderBottom: `1px solid ${COLORS.border}`,
                    cursor: 'pointer',
                    backgroundColor: selectedRoom?.userId === room.userId ? COLORS.grayLight : 'white',
                    transition: 'background-color 0.2s'
                  }}
                  onMouseEnter={(e) => {
                    if (selectedRoom?.userId !== room.userId) {
                      e.currentTarget.style.backgroundColor = '#f9fafb'
                    }
                  }}
                  onMouseLeave={(e) => {
                    if (selectedRoom?.userId !== room.userId) {
                      e.currentTarget.style.backgroundColor = 'white'
                    }
                  }}
                >
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '4px' }}>
                    <strong style={{ fontSize: '14px' }}>{room.tenKhachHang || `Khách hàng #${room.userId}`}</strong>
                    {room.tinNhanChuaDoc > 0 && (
                      <span style={{
                        backgroundColor: COLORS.danger,
                        color: 'white',
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '12px'
                      }}>
                        {room.tinNhanChuaDoc}
                      </span>
                    )}
                  </div>
                  <p style={{ 
                    margin: 0, 
                    fontSize: '13px', 
                    color: COLORS.gray,
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis'
                  }}>
                    {room.tinNhanCuoi || 'Chưa có tin nhắn'}
                  </p>
                  {room.thoiGianCuoi && (
                    <p style={{ margin: '4px 0 0', fontSize: '12px', color: COLORS.gray }}>
                      {formatTime(room.thoiGianCuoi)}
                    </p>
                  )}
                </div>
              ))
            )}
          </div>
        </div>

        {/* Chat Area */}
        <div style={{ 
          flex: 1, 
          backgroundColor: 'white',
          borderRadius: '12px',
          boxShadow: '0 1px 3px rgba(0,0,0,0.1)',
          display: 'flex',
          flexDirection: 'column'
        }}>
          {!selectedRoom ? (
            <div style={{ 
              flex: 1, 
              display: 'flex', 
              alignItems: 'center', 
              justifyContent: 'center',
              color: COLORS.gray
            }}>
              <div style={{ textAlign: 'center' }}>
                <p style={{ fontSize: '48px', margin: 0 }}>💬</p>
                <p>Chọn một cuộc trò chuyện để bắt đầu</p>
              </div>
            </div>
          ) : (
            <>
              {/* Chat Header */}
              <div style={{ 
                padding: '16px 20px', 
                borderBottom: `1px solid ${COLORS.border}`,
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}>
                <div>
                  <h4 style={{ margin: 0 }}>{selectedRoom.tenKhachHang || `Khách hàng #${selectedRoom.userId}`}</h4>
                  <p style={{ margin: '4px 0 0', fontSize: '13px', color: COLORS.gray }}>
                    ID: {selectedRoom.userId}
                  </p>
                </div>
                <button
                  onClick={() => setSelectedRoom(null)}
                  style={{
                    padding: '8px 16px',
                    backgroundColor: COLORS.grayLight,
                    border: 'none',
                    borderRadius: '6px',
                    cursor: 'pointer'
                  }}
                >
                  Đóng
                </button>
              </div>

              {/* Messages */}
              <div style={{ 
                flex: 1, 
                overflowY: 'auto', 
                padding: '20px',
                backgroundColor: '#f9fafb'
              }}>
                {messages.length === 0 ? (
                  <div style={{ textAlign: 'center', color: COLORS.gray, marginTop: '40px' }}>
                    <p>Chưa có tin nhắn. Hãy gửi tin nhắn đầu tiên!</p>
                  </div>
                ) : (
                  messages.map((msg, idx) => (
                    <div
                      key={msg.id || idx}
                      style={{
                        display: 'flex',
                        justifyContent: msg.nguoiGui === 'ADMIN' ? 'flex-end' : 'flex-start',
                        marginBottom: '12px'
                      }}
                    >
                      <div style={{
                        maxWidth: '70%',
                        padding: '12px 16px',
                        borderRadius: '16px',
                        backgroundColor: msg.nguoiGui === 'ADMIN' ? COLORS.primary : 'white',
                        color: msg.nguoiGui === 'ADMIN' ? 'white' : 'black',
                        boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
                        border: msg.nguoiGui === 'ADMIN' ? 'none' : `1px solid ${COLORS.border}`
                      }}>
                        <p style={{ margin: 0, fontSize: '14px', lineHeight: '1.5' }}>
                          {msg.noiDung}
                        </p>
                        <p style={{ 
                          margin: '4px 0 0', 
                          fontSize: '11px', 
                          opacity: 0.7,
                          textAlign: 'right'
                        }}>
                          {formatTime(msg.thoiGian)}
                          {msg.daDoc && msg.nguoiGui === 'ADMIN' && ' ✓'}
                        </p>
                      </div>
                    </div>
                  ))
                )}
              </div>

              {/* Input Area */}
              <div style={{ 
                padding: '16px 20px', 
                borderTop: `1px solid ${COLORS.border}`,
                backgroundColor: 'white'
              }}>
                <div style={{ display: 'flex', gap: '12px' }}>
                  <input
                    placeholder="Nhập tin nhắn..."
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && sendMessage()}
                    style={{
                      flex: 1,
                      padding: '12px 16px',
                      borderRadius: '24px',
                      border: `1px solid ${COLORS.border}`,
                      fontSize: '14px',
                      outline: 'none'
                    }}
                  />
                  <button
                    onClick={sendMessage}
                    disabled={!newMessage.trim()}
                    style={{
                      padding: '12px 24px',
                      backgroundColor: COLORS.primary,
                      color: 'white',
                      border: 'none',
                      borderRadius: '24px',
                      cursor: newMessage.trim() ? 'pointer' : 'not-allowed',
                      opacity: newMessage.trim() ? 1 : 0.5,
                      fontSize: '14px'
                    }}
                  >
                    Gửi
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export { MessageTab }
