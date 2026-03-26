import React, { useEffect, useState } from 'react';
import { API_BASE_URL } from '../config.js';

export function CategoryTab() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [categories, setCategories] = useState([]);
  const [search, setSearch] = useState('');
  const [modalOpen, setModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState({ ten: '', moTa: '' });
  const [formError, setFormError] = useState('');

  useEffect(() => {
    reloadCategories();
  }, []);

  async function reloadCategories() {
    try {
      setLoading(true);
      setError('');
      const token = localStorage.getItem('adminToken');
      const res = await fetch(`${API_BASE_URL}/admin/danh-muc`, {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!res.ok) {
        throw new Error(`Loi tai danh muc: ${res.status}`);
      }
      const data = await res.json();
      setCategories(Array.isArray(data) ? data : data.danhSach || []);
    } catch (err) {
      setError(err.message || 'Khong the tai danh sach danh muc');
    } finally {
      setLoading(false);
    }
  }

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    try {
      setFormError('');
      if (!form.ten) {
        setFormError('Ten danh muc la bat buoc');
        return;
      }
      const token = localStorage.getItem('adminToken');
      const url = editingId
        ? `${API_BASE_URL}/admin/danh-muc/${editingId}`
        : `${API_BASE_URL}/admin/danh-muc`;
      const method = editingId ? 'PATCH' : 'POST';
      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ ten: form.ten, moTa: form.moTa }),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(
          data.message ||
            `${editingId ? 'Cap nhat' : 'Them'} danh muc that bai (${res.status})`
        );
      }
      setForm({ ten: '', moTa: '' });
      setEditingId(null);
      setModalOpen(false);
      await reloadCategories();
    } catch (err) {
      setFormError(
        err.message || (editingId ? 'Cap nhat danh muc that bai' : 'Them danh muc that bai')
      );
    }
  }

  async function handleDelete(id) {
    if (!window.confirm('Ban co chac muon xoa danh muc nay?')) return;
    try {
      const token = localStorage.getItem('adminToken');
      const res = await fetch(`${API_BASE_URL}/admin/danh-muc/${id}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || `Xoa danh muc that bai (${res.status})`);
      }
      await reloadCategories();
    } catch (err) {
      alert(err.message || 'Khong the xoa danh muc');
    }
  }

  const filtered = categories.filter((c) => {
    const q = search.toLowerCase();
    return (
      !q ||
      c.ten?.toLowerCase().includes(q) ||
      c.moTa?.toLowerCase().includes(q)
    );
  });

  return (
    <div className="admin-panel">
      <div className="admin-panel-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>Quan ly danh muc</span>
        <button
          className="admin-header-btn"
          style={{ backgroundColor: '#111827', color: 'white' }}
          onClick={() => setModalOpen(true)}
        >
          + Them Danh muc
        </button>
      </div>
      <div style={{ marginTop: 8, marginBottom: 8 }}>
        <input
          placeholder="Tim kiem danh muc theo ten hoac mo ta..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: '100%', padding: '6px 8px', borderRadius: 6, border: '1px solid #e5e7eb', fontSize: 13 }}
        />
      </div>
      {loading && <div className="admin-panel-placeholder">Dang tai danh sach danh muc...</div>}
      {error && !loading && (
        <div className="admin-panel-placeholder" style={{ color: '#b91c1c' }}>
          {error}
        </div>
      )}
      {!loading && !error && (
        <div style={{ overflowX: 'auto', marginTop: 8 }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb' }}>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>ID</th>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Ten danh muc</th>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Mo ta</th>
                <th style={{ textAlign: 'center', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Thao tac</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((c, idx) => (
                <tr key={c.id}>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>{c.slug || `cat-${idx + 1}`}</td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>{c.ten}</td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>{c.moTa || '-'}</td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6', textAlign: 'center' }}>
                    <button
                      className="admin-header-btn"
                      style={{ marginRight: 4 }}
                      onClick={() => {
                        setForm({ ten: c.ten || '', moTa: c.moTa || '' });
                        setEditingId(c.id);
                        setFormError('');
                        setModalOpen(true);
                      }}
                    >
                      Sua
                    </button>
                    <button
                      className="admin-header-btn"
                      style={{ backgroundColor: '#fee2e2', color: '#b91c1c' }}
                      onClick={() => handleDelete(c.id)}
                    >
                      Xoa
                    </button>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ padding: '10px', textAlign: 'center', fontSize: 12, color: '#9ca3af' }}>
                    Chua co danh muc nao.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
      {modalOpen && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.35)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
          }}
        >
          <div
            style={{
              backgroundColor: 'white',
              borderRadius: 12,
              padding: 20,
              width: '100%',
              maxWidth: 420,
              boxShadow: '0 20px 40px rgba(15,23,42,0.35)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
              <div style={{ fontWeight: 600, fontSize: 16 }}>
                {editingId ? 'Sua Danh Muc' : 'Them Danh Muc Moi'}
              </div>
              <button
                type="button"
                onClick={() => {
                  setModalOpen(false);
                  setEditingId(null);
                  setFormError('');
                }}
                style={{ border: 'none', background: 'transparent', fontSize: 18, cursor: 'pointer' }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleSubmit}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Ten danh muc</label>
                  <input
                    name="ten"
                    value={form.ten}
                    onChange={handleChange}
                    required
                    placeholder="Nhap ten danh muc"
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Mo ta</label>
                  <textarea
                    name="moTa"
                    value={form.moTa}
                    onChange={handleChange}
                    rows={2}
                    placeholder="Nhap mo ta danh muc"
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, resize: 'vertical' }}
                  />
                </div>
              </div>
              {formError && (
                <div style={{ marginTop: 8, fontSize: 12, color: '#b91c1c' }}>{formError}</div>
              )}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 14 }}>
                <button
                  type="button"
                  className="admin-header-btn"
                  style={{ backgroundColor: '#e5e7eb', color: '#111827' }}
                  onClick={() => setModalOpen(false)}
                >
                  Huy
                </button>
                <button
                  type="submit"
                  className="admin-header-btn"
                  style={{ backgroundColor: '#111827', color: 'white' }}
                >
                  {editingId ? 'Cap nhat' : 'Them'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
