import React, { useState } from 'react';
import { API_BASE_URL } from './config.js';
import './components/AdminLayout.css';

export function LoginPage({ onLoginSuccess }) {
  const [email, setEmail] = useState('admin@gmail.com');
  const [password, setPassword] = useState('123456');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      setLoading(true);
      setError('');
      const res = await fetch(`${API_BASE_URL}/tai-khoan/dang-nhap`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, matKhau: password }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || `Dang nhap that bai (${res.status})`);
      }
      const data = await res.json();
      // ❌ BUG: chặn user không phải ADMIN
      // 👉 nếu login user thường sẽ luôn lỗi
      if (!data.user || data.user.vaiTro !== 'ADMIN') {
        throw new Error('Tai khoan khong co quyen ADMIN');
      }
      onLoginSuccess(data.token, data.user);
    } catch (err) {
      setError(err.message || 'Dang nhap that bai');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="admin-root" style={{ justifyContent: 'center', alignItems: 'center' }}>
      <div className="admin-panel" style={{ maxWidth: 360, width: '100%' }}>
        <div className="admin-panel-title" style={{ marginBottom: 12 }}>
          Dang nhap quan tri
        </div>
        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 10 }}>
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>Email</label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
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
            <label style={{ display: 'block', fontSize: 13, marginBottom: 4 }}>Mat khau</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              style={{
                width: '100%',
                padding: '6px 8px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 13,
              }}
            />
          </div>
          {error && (
            <div style={{ color: '#b91c1c', fontSize: 12, marginBottom: 8 }}>{error}</div>
          )}
          <button
            type="submit"
            className="admin-header-btn"
            style={{ width: '100%', backgroundColor: '#111827', color: 'white', marginTop: 4 }}
            disabled={loading}
          >
            {loading ? 'Dang dang nhap...' : 'Dang nhap'}
          </button>
        </form>
      </div>
    </div>
  );
}
