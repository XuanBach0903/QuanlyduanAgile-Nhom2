import React, { useState } from 'react';
import { API_BASE_URL } from './config.js';
import './components/AdminLayout.css';

export function RegisterPage({ onRegisterSuccess }) {
  const [formData, setFormData] = useState({
    hoTen: '',
    email: '',
    soDienThoai: '',
    matKhau: '',
    xacNhanMatKhau: ''
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  function handleChange(e) {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Validation
    if (!formData.hoTen || !formData.email || !formData.matKhau) {
      setError('Vui lòng điền đầy đủ thông tin bắt buộc');
      return;
    }

    if (formData.matKhau.length < 6) {
      setError('Mật khẩu phải có ít nhất 6 ký tự');
      return;
    }

    if (formData.matKhau !== formData.xacNhanMatKhau) {
      setError('Xác nhận mật khẩu không khớp');
      return;
    }

    try {
      setLoading(true);
      const res = await fetch(`${API_BASE_URL}/tai-khoan/dang-ky`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          hoTen: formData.hoTen,
          email: formData.email,
          soDienThoai: formData.soDienThoai,
          matKhau: formData.matKhau
        }),
      });

      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || `Đăng ký thất bại (${res.status})`);
      }

      const data = await res.json();
      setSuccess('Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.');
      
      // Clear form
      setFormData({
        hoTen: '',
        email: '',
        soDienThoai: '',
        matKhau: '',
        xacNhanMatKhau: ''
      });

      // Notify parent component
      if (onRegisterSuccess) {
        onRegisterSuccess(data);
      }

    } catch (err) {
      setError(err.message || 'Đăng ký thất bại');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-root" style={{ justifyContent: 'center', alignItems: 'center' }}>
      <div className="admin-panel" style={{ maxWidth: 400, width: '100%' }}>
        <div className="admin-panel-title" style={{ marginBottom: 12 }}>
          Đăng ký tài khoản
        </div>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>
              Họ tên <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="text"
              name="hoTen"
              value={formData.hoTen}
              onChange={handleChange}
              style={{
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 13,
              }}
              required
            />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>
              Email <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              style={{
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 13,
              }}
              required
            />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>
              Số điện thoại
            </label>
            <input
              type="tel"
              name="soDienThoai"
              value={formData.soDienThoai}
              onChange={handleChange}
              style={{
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 13,
              }}
            />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>
              Mật khẩu <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              name="matKhau"
              value={formData.matKhau}
              onChange={handleChange}
              style={{
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 13,
              }}
              required
              minLength="6"
            />
          </div>

          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>
              Xác nhận mật khẩu <span style={{ color: '#dc2626' }}>*</span>
            </label>
            <input
              type="password"
              name="xacNhanMatKhau"
              value={formData.xacNhanMatKhau}
              onChange={handleChange}
              style={{
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 13,
              }}
              required
              minLength="6"
            />
          </div>

          {error && (
            <div style={{ color: '#b91c1c', fontSize: 12, marginBottom: 8, padding: '8px', backgroundColor: '#fef2f2', borderRadius: 4 }}>
              {error}
            </div>
          )}

          {success && (
            <div style={{ color: '#16a34a', fontSize: 12, marginBottom: 8, padding: '8px', backgroundColor: '#f0fdf4', borderRadius: 4 }}>
              {success}
            </div>
          )}

          <button
            type="submit"
            className="admin-header-btn"
            style={{ width: '100%', backgroundColor: '#16a34a', color: 'white', marginTop: 4 }}
            disabled={loading}
          >
            {loading ? 'Đang đăng ký...' : 'Đăng ký'}
          </button>
        </form>
      </div>
    </div>
  );
}
