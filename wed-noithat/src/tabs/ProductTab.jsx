import React, { useEffect, useState } from 'react';
import { API_BASE_URL } from '../config.js';

export function ProductTab() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [products, setProducts] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [formOpen, setFormOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [createError, setCreateError] = useState('');
  const [categories, setCategories] = useState([]);
  const [reviewProduct, setReviewProduct] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [reviewsSummary, setReviewsSummary] = useState({ soLuong: 0, diemTrungBinh: 0 });
  const [reviewsLoading, setReviewsLoading] = useState(false);
  const [reviewsError, setReviewsError] = useState('');
  const [reviewSort, setReviewSort] = useState('moiNhat'); // moiNhat | cuNhat
  const [form, setForm] = useState({
    ten: '',
    gia: '',
    tonKho: '',
    danhMucId: '',
    chatLieu: '',
    moTa: '',
    dai_cm: '',
    rong_cm: '',
    cao_cm: '',
    hinhDaiDien: '',
    hinhAnh: '', // nhiều URL, phân cách bởi dấu phẩy
  });

  useEffect(() => {
    let active = true;
    async function fetchProducts() {
      try {
        setLoading(true);
        setError('');
        const token = localStorage.getItem('adminToken');
        const params = new URLSearchParams();
        if (search && search.trim() !== '') {
          params.set('tuKhoa', search.trim());
        }
        if (selectedCategoryId) {
          params.set('danhMucId', selectedCategoryId);
        }
        const url = `${API_BASE_URL}/admin/san-pham${params.toString() ? `?${params.toString()}` : ''}`;
        const res = await fetch(url, {
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        });
        if (!res.ok) {
          throw new Error(`Loi tai san pham: ${res.status}`);
        }
        const data = await res.json();
        if (!active) return;
        // API admin /admin/san-pham trả về một mảng các sản phẩm
        setProducts(Array.isArray(data) ? data : data.danhSach || []);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Khong the tai du lieu san pham');
      } finally {
        if (active) setLoading(false);
      }
    }

    fetchProducts();
    return () => {
      active = false;
    };
  }, [search, selectedCategoryId]);

  // Load danh mục cho dropdown
  async function fetchCategories() {
    try {
      const token = localStorage.getItem('adminToken');
      const res = await fetch(`${API_BASE_URL}/admin/danh-muc`, {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!res.ok) return;
      const data = await res.json();
      // adminDanhMuc trả về mảng [{ id, ten, moTa, slug }]
      setCategories(Array.isArray(data) ? data : data.danhSach || []);
    } catch (err) {
      // bỏ qua lỗi, vẫn cho phép nhập sản phẩm nhưng không chọn được danh mục
    }
  }

  async function handleShowReviews(product) {
    if (!product) {
      setReviewProduct(null);
      setReviews([]);
      setReviewsSummary({ soLuong: 0, diemTrungBinh: 0 });
      setReviewsError('');
      return;
    }

    try {
      setReviewProduct(product);
      setReviewsLoading(true);
      setReviewsError('');

      const res = await fetch(`${API_BASE_URL}/san-pham/${product.id}/danh-gia`);
      if (!res.ok) {
        throw new Error(`Khong tai duoc danh gia (${res.status})`);
      }
      const data = await res.json();
      setReviewsSummary({
        soLuong: data.soLuong || 0,
        diemTrungBinh: data.diemTrungBinh || 0,
      });
      setReviews(data.danhSach || []);
    } catch (err) {
      setReviewsError(err.message || 'Khong the tai danh gia');
      setReviews([]);
      setReviewsSummary({ soLuong: 0, diemTrungBinh: 0 });
    } finally {
      setReviewsLoading(false);
    }
  }

  useEffect(() => {
    fetchCategories();
  }, []);

  async function reloadProducts() {
    // Hàm nhỏ để reload danh sách sau khi thêm / xóa
    setLoading(true);
    setError('');
    try {
      const token = localStorage.getItem('adminToken');
      const params = new URLSearchParams();
      if (search && search.trim() !== '') {
        params.set('tuKhoa', search.trim());
      }
      if (selectedCategoryId) {
        params.set('danhMucId', selectedCategoryId);
      }
      const url = `${API_BASE_URL}/admin/san-pham${params.toString() ? `?${params.toString()}` : ''}`;
      const res = await fetch(url, {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!res.ok) {
        throw new Error(`Loi tai san pham: ${res.status}`);
      }
      const data = await res.json();
      setProducts(Array.isArray(data) ? data : data.danhSach || []);
    } catch (err) {
      setError(err.message || 'Khong the tai du lieu san pham');
    } finally {
      setLoading(false);
    }
  }

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  }

  async function handleCreate(e) {
    e.preventDefault();
    try {
      setCreateError('');
      if (!form.danhMucId) {
        setCreateError('Vui long chon danh muc');
        return;
      }
      const token = localStorage.getItem('adminToken');
      let hinhAnhArray;
      if (typeof form.hinhAnh === 'string') {
        hinhAnhArray = form.hinhAnh
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean);
      } else if (Array.isArray(form.hinhAnh)) {
        hinhAnhArray = form.hinhAnh.filter(Boolean);
      } else {
        hinhAnhArray = undefined;
      }

      const body = {
        ten: form.ten,
        gia: form.gia ? Number(form.gia) : undefined,
        tonKho: form.tonKho ? Number(form.tonKho) : undefined,
        danhMucId: form.danhMucId,
        chatLieu: form.chatLieu || undefined,
        moTa: form.moTa || undefined,
        kichThuoc:
          form.dai_cm || form.rong_cm || form.cao_cm
            ? {
                dai_cm: form.dai_cm ? Number(form.dai_cm) : undefined,
                rong_cm: form.rong_cm ? Number(form.rong_cm) : undefined,
                cao_cm: form.cao_cm ? Number(form.cao_cm) : undefined,
              }
            : undefined,
        hinhDaiDien: form.hinhDaiDien || undefined,
        hinhAnh: hinhAnhArray && hinhAnhArray.length > 0 ? hinhAnhArray : undefined,
      };
      const url = editingId
        ? `${API_BASE_URL}/admin/san-pham/${editingId}`
        : `${API_BASE_URL}/admin/san-pham`;
      const method = editingId ? 'PATCH' : 'POST';
      const res = await fetch(url, {
        method,
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(
          data.message ||
            `${editingId ? 'Cap nhat' : 'Them'} san pham that bai (${res.status})`
        );
      }

      // Reset form và reload danh sách
      setForm({
        ten: '',
        gia: '',
        tonKho: '',
        danhMucId: '',
        chatLieu: '',
        moTa: '',
        dai_cm: '',
        rong_cm: '',
        cao_cm: '',
        hinhDaiDien: '',
        hinhAnh: '',
      });
      setEditingId(null);
      setFormOpen(false);
      await reloadProducts();
    } catch (err) {
      setCreateError(
        err.message || (editingId ? 'Cap nhat san pham that bai' : 'Them san pham that bai')
      );
    } finally {
      // không đóng form tự động, chỉ reset lỗi
    }
  }

  async function handleDelete(productId) {
    if (!window.confirm('Ban co chac muon xoa san pham nay?')) return;
    try {
      const token = localStorage.getItem('adminToken');
      const res = await fetch(`${API_BASE_URL}/admin/san-pham/${productId}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || `Xoa san pham that bai (${res.status})`);
      }
      await reloadProducts();
    } catch (err) {
      alert(err.message || 'Khong the xoa san pham');
    }
  }

  return (
    <div className="admin-panel">
      <div className="admin-panel-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <span>Quan ly san pham</span>
        <button
          className="admin-header-btn"
          style={{ backgroundColor: '#111827', color: 'white' }}
          onClick={async () => {
            // Mở form ở chế độ thêm mới
            if (!formOpen) {
              await fetchCategories();
            }
            setEditingId(null);
            setForm({
              ten: '',
              gia: '',
              tonKho: '',
              danhMucId: '',
              chatLieu: '',
              moTa: '',
              dai_cm: '',
              rong_cm: '',
              cao_cm: '',
              hinhDaiDien: '',
              hinhAnh: '',
            });
            setCreateError('');
            setFormOpen(true);
          }}
        >
          + Them san pham
        </button>
      </div>
      <div style={{ marginTop: 8, marginBottom: 8 }}>
        <input
          placeholder="Tim kiem san pham theo ten..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ width: '100%', padding: '6px 8px', borderRadius: 6, border: '1px solid #e5e7eb', fontSize: 13 }}
        />
      </div>
      {categories.length > 0 && (
        <div style={{ marginBottom: 8, display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          <button
            type="button"
            className="admin-header-btn"
            onClick={() => setSelectedCategoryId('')}
            style={{
              padding: '4px 10px',
              backgroundColor: selectedCategoryId ? '#f3f4f6' : '#111827',
              color: selectedCategoryId ? '#111827' : '#ffffff',
              borderRadius: 999,
              fontSize: 12,
            }}
          >
            Tat ca
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              type="button"
              className="admin-header-btn"
              onClick={() => setSelectedCategoryId(c.id)}
              style={{
                padding: '4px 10px',
                backgroundColor: selectedCategoryId === c.id ? '#111827' : '#f3f4f6',
                color: selectedCategoryId === c.id ? '#ffffff' : '#111827',
                borderRadius: 999,
                fontSize: 12,
              }}
            >
              {c.ten}
            </button>
          ))}
        </div>
      )}
      {loading && <div className="admin-panel-placeholder">Dang tai danh sach san pham...</div>}
      {error && !loading && (
        <div className="admin-panel-placeholder" style={{ color: '#b91c1c' }}>
          {error}
        </div>
      )}
      {!loading && !error && (
        <div style={{ display: 'flex', gap: 12, marginTop: 8 }}>
          <div style={{ flex: reviewProduct ? 2 : 1, overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
            <thead>
              <tr style={{ backgroundColor: '#f9fafb' }}>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5', width: 60 }}>Anh</th>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Ten</th>
                <th style={{ textAlign: 'right', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Gia</th>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Danh muc</th>
                <th style={{ textAlign: 'left', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Chat lieu</th>
                <th style={{ textAlign: 'right', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Ton kho</th>
                <th style={{ textAlign: 'center', padding: '8px 10px', borderBottom: '1px solid #e5e5e5' }}>Thao tac</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p) => (
                <tr key={p.id}>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>
                    {p.hinhDaiDien || (p.hinhAnh && p.hinhAnh.length > 0) ? (
                      <img
                        src={p.hinhDaiDien || p.hinhAnh[0]}
                        alt={p.ten}
                        style={{ width: 44, height: 44, objectFit: 'cover', borderRadius: 8, border: '1px solid #e5e7eb' }}
                        onError={(e) => {
                          e.target.style.visibility = 'hidden';
                        }}
                      />
                    ) : (
                      <span style={{ fontSize: 11, color: '#9ca3af' }}>Khong co anh</span>
                    )}
                  </td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>{p.ten}</td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6', textAlign: 'right' }}>
                    {p.gia?.toLocaleString('vi-VN')} đ
                  </td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>
                    {p.danhMuc?.ten || '-'}
                  </td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6' }}>{p.chatLieu || '-'}</td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6', textAlign: 'right' }}>
                    {p.tonKho ?? '-'}
                  </td>
                  <td style={{ padding: '8px 10px', borderBottom: '1px solid #f3f4f6', textAlign: 'center' }}>
                    <button
                      className="admin-header-btn"
                      style={{ marginRight: 4, backgroundColor: '#e5e7eb', color: '#111827' }}
                      onClick={() => handleShowReviews(p)}
                    >
                      Danh gia
                    </button>
                    <button
                      className="admin-header-btn"
                      style={{ marginRight: 4 }}
                      onClick={() => {
                        setEditingId(p.id);
                        setForm({
                          ten: p.ten,
                          gia: p.gia,
                          tonKho: p.tonKho,
                          danhMucId: p.danhMuc?.id || '',
                          chatLieu: p.chatLieu,
                          moTa: p.moTa,
                          dai_cm: p.kichThuoc?.dai_cm,
                          rong_cm: p.kichThuoc?.rong_cm,
                          cao_cm: p.kichThuoc?.cao_cm,
                          hinhDaiDien: p.hinhDaiDien,
                          hinhAnh: Array.isArray(p.hinhAnh)
                            ? p.hinhAnh.join(', ')
                            : p.hinhAnh || '',
                        });
                        setCreateError('');
                        setFormOpen(true);
                      }}
                    >
                      Sua
                    </button>
                    <button
                      className="admin-header-btn"
                      style={{ backgroundColor: '#fee2e2', color: '#b91c1c' }}
                      onClick={() => handleDelete(p.id)}
                    >
                      Xoa
                    </button>
                  </td>
                </tr>
              ))}
              {products.length === 0 && (
                <tr>
                  <td colSpan={7} style={{ padding: '10px', textAlign: 'center', fontSize: 12, color: '#9ca3af' }}>
                    Chua co san pham nao.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          </div>

          {/* Panel đánh giá bên phải – chỉ hiển thị khi đã chọn sản phẩm */}
          {reviewProduct && (
            <div style={{ flex: 1, minWidth: 260 }}>
              <div
                className="admin-panel"
                style={{
                  margin: 0,
                  height: '100%',
                  transform: 'translateX(0)',
                  transition: 'transform 0.25s ease-out',
                  boxShadow: '0 10px 25px rgba(15,23,42,0.15)',
                }}
              >
                <div className="admin-panel-title">
                  Danh gia san pham
                </div>
                <>
                  <div style={{ fontSize: 13, marginTop: 6 }}>
                    <div style={{ fontWeight: 600 }}>{reviewProduct.ten}</div>
                    <div style={{ marginTop: 2 }}>
                      <span style={{ fontWeight: 600 }}>Diem trung binh:</span>{' '}
                      <span>
                        {reviewsSummary.diemTrungBinh} / 5 ({reviewsSummary.soLuong} danh gia)
                      </span>
                    </div>
                  </div>
                  {/* Nút sort mới nhất / cũ nhất */}
                  <div style={{ marginTop: 6, display: 'flex', gap: 6, fontSize: 12 }}>
                    <button
                      type="button"
                      className="admin-header-btn"
                      onClick={() => setReviewSort('moiNhat')}
                      style={{
                        padding: '3px 8px',
                        backgroundColor: reviewSort === 'moiNhat' ? '#111827' : '#e5e7eb',
                        color: reviewSort === 'moiNhat' ? '#ffffff' : '#111827',
                      }}
                    >
                      Moi nhat
                    </button>
                    <button
                      type="button"
                      className="admin-header-btn"
                      onClick={() => setReviewSort('cuNhat')}
                      style={{
                        padding: '3px 8px',
                        backgroundColor: reviewSort === 'cuNhat' ? '#111827' : '#e5e7eb',
                        color: reviewSort === 'cuNhat' ? '#ffffff' : '#111827',
                      }}
                    >
                      Cu nhat
                    </button>
                  </div>
                  {reviewsLoading && (
                    <div className="admin-panel-placeholder">Dang tai danh gia...</div>
                  )}
                  {reviewsError && !reviewsLoading && (
                    <div className="admin-panel-placeholder" style={{ color: '#b91c1c' }}>
                      {reviewsError}
                    </div>
                  )}
                  {!reviewsLoading && !reviewsError && (
                    <div
                      style={{
                        marginTop: 8,
                        maxHeight: 260,
                        overflowY: 'auto',
                        borderRadius: 8,
                        border: '1px solid #e5e7eb',
                      }}
                    >
                      {reviews.length === 0 && (
                        <div
                          style={{
                            padding: '8px 10px',
                            fontSize: 13,
                            color: '#6b7280',
                            textAlign: 'center',
                          }}
                        >
                          Chua co danh gia nao.
                        </div>
                      )}
                      {([...reviews]
                        .sort((a, b) => {
                          const ta = new Date(a.ngayTao || 0).getTime();
                          const tb = new Date(b.ngayTao || 0).getTime();
                          if (Number.isNaN(ta) || Number.isNaN(tb)) return 0;
                          return reviewSort === 'moiNhat' ? tb - ta : ta - tb;
                        }))
                        .map((rv) => (
                        <div
                          key={rv.id}
                          style={{
                            padding: '8px 10px',
                            borderBottom: '1px solid #f3f4f6',
                            fontSize: 13,
                          }}
                        >
                          <div style={{ marginBottom: 2 }}>
                            <span style={{ fontWeight: 600 }}>
                              {rv.nguoiDungTen || 'Khach hang'}
                            </span>{' '}
                            <span style={{ color: '#f59e0b' }}>
                              {'★'.repeat(rv.soSao || 0)}
                            </span>
                          </div>
                          {rv.noiDung && (
                            <div style={{ color: '#4b5563' }}>{rv.noiDung}</div>
                          )}
                          {rv.ngayTao && (
                            <div style={{ color: '#9ca3af', fontSize: 11, marginTop: 2 }}>
                              {new Date(rv.ngayTao).toLocaleString('vi-VN')}
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </>
              </div>
            </div>
          )}
        </div>
      )}
      {formOpen && (
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
                {editingId ? 'Sua San Pham' : 'Them San Pham'}
              </div>
              <button
                type="button"
                onClick={() => {
                  setFormOpen(false);
                  setEditingId(null);
                  setCreateError('');
                }}
                style={{ border: 'none', background: 'transparent', fontSize: 18, cursor: 'pointer' }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleCreate}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Ten san pham</label>
                  <input
                    name="ten"
                    value={form.ten}
                    onChange={handleChange}
                    required
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Gia (VND)</label>
                  <input
                    name="gia"
                    type="number"
                    min="0"
                    value={form.gia}
                    onChange={handleChange}
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
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, resize: 'vertical' }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Chat lieu</label>
                  <input
                    name="chatLieu"
                    value={form.chatLieu}
                    onChange={handleChange}
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  />
                </div>
                <div style={{ display: 'flex', gap: 8 }}>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <label style={{ fontSize: 12, marginBottom: 4 }}>Dai (cm)</label>
                    <input
                      name="dai_cm"
                      type="number"
                      min="0"
                      value={form.dai_cm}
                      onChange={handleChange}
                      style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                    />
                  </div>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <label style={{ fontSize: 12, marginBottom: 4 }}>Rong (cm)</label>
                    <input
                      name="rong_cm"
                      type="number"
                      min="0"
                      value={form.rong_cm}
                      onChange={handleChange}
                      style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                    />
                  </div>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <label style={{ fontSize: 12, marginBottom: 4 }}>Cao (cm)</label>
                    <input
                      name="cao_cm"
                      type="number"
                      min="0"
                      value={form.cao_cm}
                      onChange={handleChange}
                      style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                    />
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Hinh anh (URL, phan cach boi dau phay)</label>
                  <input
                    name="hinhAnh"
                    value={form.hinhAnh}
                    onChange={handleChange}
                    placeholder="https://...1.jpg, https://...2.jpg"
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>URL hinh dai dien</label>
                  <input
                    name="hinhDaiDien"
                    value={form.hinhDaiDien}
                    onChange={handleChange}
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>Danh muc</label>
                  <select
                    name="danhMucId"
                    value={form.danhMucId}
                    onChange={handleChange}
                    required
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  >
                    <option value="">-- Chon danh muc --</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.ten}
                      </option>
                    ))}
                  </select>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: 12, marginBottom: 4 }}>So luong</label>
                  <input
                    name="tonKho"
                    type="number"
                    min="0"
                    value={form.tonKho}
                    onChange={handleChange}
                    style={{ padding: '6px 8px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13 }}
                  />
                </div>
              </div>
              {createError && (
                <div style={{ marginTop: 8, fontSize: 12, color: '#b91c1c' }}>{createError}</div>
              )}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 14 }}>
                <button
                  type="button"
                  className="admin-header-btn"
                  style={{ backgroundColor: '#e5e7eb', color: '#111827' }}
                  onClick={() => {
                    setFormOpen(false);
                    setEditingId(null);
                    setCreateError('');
                  }}
                >
                  Huy
                </button>
                <button
                  type="submit"
                  className="admin-header-btn"
                  style={{ backgroundColor: '#111827', color: 'white' }}
                >
                  {editingId ? 'Cap nhat' : 'Them moi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
