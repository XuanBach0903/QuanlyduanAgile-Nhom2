import React, { useEffect, useState } from 'react';
import { API_BASE_URL } from '../config.js';

// Màu sắc chính
const COLORS = {
  primary: '#4f46e5',
  primaryHover: '#4338ca',
  primaryLight: '#e0e7ff',
  danger: '#ef4444',
  dangerHover: '#dc2626',
  dangerLight: '#fee2e2',
  success: '#10b981',
  successLight: '#d1fae5',
  warning: '#f59e0b',
  warningLight: '#fef3c7',
  gray: '#6b7280',
  grayLight: '#f3f4f6',
  grayBorder: '#e5e7eb',
  white: '#ffffff',
  text: '#1f2937',
  textLight: '#6b7280',
};

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
  const [toast, setToast] = useState(null); // { type: 'success'|'error', message: '' }
  const [imagePreview, setImagePreview] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null); // productId cần xóa
  const [formErrors, setFormErrors] = useState({});
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
          throw new Error(`Lỗi tải sản phẩm: ${res.status}`);
        }
        const data = await res.json();
        if (!active) return;
        // API admin /admin/san-pham trả về một mảng các sản phẩm
        setProducts(Array.isArray(data) ? data : data.danhSach || []);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Không thể tải dữ liệu sản phẩm');
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
        throw new Error(`Lỗi tải sản phẩm: ${res.status}`);
      }
      const data = await res.json();
      setProducts(Array.isArray(data) ? data : data.danhSach || []);
    } catch (err) {
      setError(err.message || 'Không thể tải dữ liệu sản phẩm');
    } finally {
      setLoading(false);
    }
  }

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    // Xóa lỗi khi user nhập
    if (formErrors[name]) {
      setFormErrors((prev) => ({ ...prev, [name]: null }));
    }
    // Preview ảnh khi nhập URL hình đại diện
    if (name === 'hinhDaiDien') {
      setImagePreview(value);
    }
  }

  // Toast helper
  function showToast(type, message) {
    setToast({ type, message });
    setTimeout(() => setToast(null), 3000);
  }

  // Validate form
  function validateForm() {
    const errors = {};
    if (!form.ten.trim()) errors.ten = 'Vui lòng nhập tên sản phẩm';
    if (!form.danhMucId) errors.danhMucId = 'Vui lòng chọn danh mục';
    if (form.gia && Number(form.gia) < 0) errors.gia = 'Giá không được âm';
    if (form.tonKho && Number(form.tonKho) < 0) errors.tonKho = 'Tồn kho không được âm';
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  }

  async function handleCreate(e) {
    e.preventDefault();
    if (!validateForm()) {
      showToast('error', 'Vui lòng kiểm tra lại thông tin');
      return;
    }
    try {
      setCreateError('');
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
      showToast('success', editingId ? 'Cập nhật sản phẩm thành công!' : 'Thêm sản phẩm thành công!');
    } catch (err) {
      setCreateError(err.message || (editingId ? 'Cập nhật sản phẩm thất bại' : 'Thêm sản phẩm thất bại'));
      showToast('error', err.message || 'Đã có lỗi xảy ra');
    }
  }

  async function handleDelete(productId) {
    setConfirmDelete(productId);
  }

  async function confirmDeleteProduct() {
    if (!confirmDelete) return;
    try {
      const token = localStorage.getItem('adminToken');
      const res = await fetch(`${API_BASE_URL}/admin/san-pham/${confirmDelete}`, {
        method: 'DELETE',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });
      if (!res.ok) {
        const data = await res.json().catch(() => ({}));
        throw new Error(data.message || `Xóa sản phẩm thất bại (${res.status})`);
      }
      await reloadProducts();
      showToast('success', 'Xóa sản phẩm thành công!');
    } catch (err) {
      showToast('error', err.message || 'Không thể xóa sản phẩm');
    } finally {
      setConfirmDelete(null);
    }
  }

  return (
    <div className="admin-panel" style={{ backgroundColor: COLORS.white, borderRadius: '12px', padding: '20px' }}>
      {/* Toast Notification */}
      {toast && (
        <div style={{
          position: 'fixed',
          top: '20px',
          right: '20px',
          padding: '12px 20px',
          borderRadius: '8px',
          backgroundColor: toast.type === 'success' ? COLORS.success : COLORS.danger,
          color: 'white',
          fontSize: '14px',
          fontWeight: 500,
          zIndex: 1000,
          boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
          animation: 'slideIn 0.3s ease-out',
        }}>
          {toast.type === 'success' ? '✓ ' : '✗ '}{toast.message}
          <style>{`@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }`}</style>
        </div>
      )}

      {/* Modal xác nhận xóa */}
      {confirmDelete && (
        <div style={{
          position: 'fixed',
          inset: 0,
          backgroundColor: 'rgba(0,0,0,0.5)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          zIndex: 100,
        }}>
          <div style={{
            backgroundColor: COLORS.white,
            borderRadius: '12px',
            padding: '24px',
            width: '100%',
            maxWidth: '400px',
            textAlign: 'center',
          }}>
            <div style={{ fontSize: '48px', marginBottom: '16px' }}>🗑️</div>
            <h3 style={{ margin: '0 0 8px', color: COLORS.text }}>Xác nhận xóa</h3>
            <p style={{ color: COLORS.gray, marginBottom: '24px' }}>Bạn có chắc muốn xóa sản phẩm này? Hành động này không thể hoàn tác.</p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                onClick={() => setConfirmDelete(null)}
                style={{
                  padding: '10px 20px',
                  borderRadius: '8px',
                  border: `1px solid ${COLORS.grayBorder}`,
                  backgroundColor: COLORS.white,
                  color: COLORS.text,
                  cursor: 'pointer',
                  fontSize: '14px',
                }}
              >
                Hủy
              </button>
              <button
                onClick={confirmDeleteProduct}
                style={{
                  padding: '10px 20px',
                  borderRadius: '8px',
                  border: 'none',
                  backgroundColor: COLORS.danger,
                  color: 'white',
                  cursor: 'pointer',
                  fontSize: '14px',
                }}
              >
                Xóa
              </button>
            </div>
          </div>
        </div>
      )}

      <div className="admin-panel-title" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
        <span style={{ fontSize: '20px', fontWeight: 600, color: COLORS.text }}>📦 Quản lý sản phẩm</span>
        <button
          style={{
            backgroundColor: COLORS.primary,
            color: 'white',
            border: 'none',
            padding: '10px 20px',
            borderRadius: '8px',
            cursor: 'pointer',
            fontSize: '14px',
            fontWeight: 500,
            display: 'flex',
            alignItems: 'center',
            gap: '6px',
            transition: 'all 0.2s',
          }}
          onMouseEnter={(e) => e.target.style.backgroundColor = COLORS.primaryHover}
          onMouseLeave={(e) => e.target.style.backgroundColor = COLORS.primary}
          onClick={async () => {
            if (!formOpen) {
              await fetchCategories();
            }
            setEditingId(null);
            setImagePreview('');
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
            setFormErrors({});
            setFormOpen(true);
          }}
        >
          + Thêm sản phẩm
        </button>
      </div>
      <div style={{ marginBottom: '16px' }}>
        <div style={{ position: 'relative' }}>
          <span style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: COLORS.gray }}>🔍</span>
          <input
            placeholder="Tìm kiếm sản phẩm theo tên..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            style={{
              width: '100%',
              padding: '10px 12px 10px 40px',
              borderRadius: '8px',
              border: `1px solid ${COLORS.grayBorder}`,
              fontSize: '14px',
              backgroundColor: COLORS.grayLight,
              outline: 'none',
              transition: 'all 0.2s',
            }}
            onFocus={(e) => e.target.style.borderColor = COLORS.primary}
            onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
          />
        </div>
      </div>
      {categories.length > 0 && (
        <div style={{ marginBottom: '16px', display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}>
          <span style={{ fontSize: '14px', color: COLORS.gray, fontWeight: 500 }}>Danh mục:</span>
          <button
            type="button"
            onClick={() => setSelectedCategoryId('')}
            style={{
              padding: '6px 14px',
              backgroundColor: selectedCategoryId ? COLORS.grayLight : COLORS.primary,
              color: selectedCategoryId ? COLORS.text : COLORS.white,
              borderRadius: '20px',
              fontSize: '13px',
              border: 'none',
              cursor: 'pointer',
              fontWeight: 500,
              transition: 'all 0.2s',
            }}
          >
            Tất cả
          </button>
          {categories.map((c) => (
            <button
              key={c.id}
              type="button"
              onClick={() => setSelectedCategoryId(c.id)}
              style={{
                padding: '6px 14px',
                backgroundColor: selectedCategoryId === c.id ? COLORS.primary : COLORS.grayLight,
                color: selectedCategoryId === c.id ? COLORS.white : COLORS.text,
                borderRadius: '20px',
                fontSize: '13px',
                border: 'none',
                cursor: 'pointer',
                fontWeight: 500,
                transition: 'all 0.2s',
              }}
            >
              {c.ten}
            </button>
          ))}
        </div>
      )}
      {loading && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '40px' }}>
          <div style={{
            width: '40px',
            height: '40px',
            border: `3px solid ${COLORS.grayBorder}`,
            borderTop: `3px solid ${COLORS.primary}`,
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
          }} />
          <style>{`@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }`}</style>
        </div>
      )}
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
                      style={{ marginRight: 4, backgroundColor: COLORS.warningLight, color: COLORS.warning, border: 'none', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', transition: 'all 0.2s' }}
                      onClick={() => handleShowReviews(p)}
                      onMouseEnter={(e) => e.target.style.backgroundColor = COLORS.warning}
                      onMouseLeave={(e) => e.target.style.backgroundColor = COLORS.warningLight}
                    >
                      ⭐ Đánh giá
                    </button>
                    <button
                      className="admin-header-btn"
                      style={{ marginRight: 4, backgroundColor: COLORS.primaryLight, color: COLORS.primary, border: 'none', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', transition: 'all 0.2s' }}
                      onClick={() => {
                        setEditingId(p.id);
                        setImagePreview(p.hinhDaiDien || '');
                        setForm({
                          ten: p.ten || '',
                          gia: p.gia ?? '',
                          tonKho: p.tonKho ?? '',
                          danhMucId: p.danhMuc?.id || '',
                          chatLieu: p.chatLieu || '',
                          moTa: p.moTa || '',
                          dai_cm: p.kichThuoc?.dai_cm ?? '',
                          rong_cm: p.kichThuoc?.rong_cm ?? '',
                          cao_cm: p.kichThuoc?.cao_cm ?? '',
                          hinhDaiDien: p.hinhDaiDien || '',
                          hinhAnh: Array.isArray(p.hinhAnh)
                            ? p.hinhAnh.join(', ')
                            : p.hinhAnh || '',
                        });
                        setCreateError('');
                        setFormErrors({});
                        setFormOpen(true);
                      }}
                      onMouseEnter={(e) => e.target.style.backgroundColor = COLORS.primary}
                      onMouseLeave={(e) => e.target.style.backgroundColor = COLORS.primaryLight}
                    >
                      ✏️ Sửa
                    </button>
                    <button
                      className="admin-header-btn"
                      style={{ backgroundColor: COLORS.dangerLight, color: COLORS.danger, border: 'none', padding: '6px 12px', borderRadius: '6px', cursor: 'pointer', fontSize: '12px', transition: 'all 0.2s' }}
                      onClick={() => handleDelete(p.id)}
                      onMouseEnter={(e) => e.target.style.backgroundColor = COLORS.danger}
                      onMouseLeave={(e) => e.target.style.backgroundColor = COLORS.dangerLight}
                    >
                      🗑️ Xóa
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
            backgroundColor: 'rgba(0,0,0,0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 50,
          }}
        >
          <div
            style={{
              backgroundColor: COLORS.white,
              borderRadius: '16px',
              padding: '24px',
              width: '100%',
              maxWidth: '480px',
              maxHeight: '90vh',
              overflowY: 'auto',
              boxShadow: '0 25px 50px rgba(0,0,0,0.25)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
              <div style={{ fontWeight: 700, fontSize: '18px', color: COLORS.text }}>
                {editingId ? '✏️ Sửa sản phẩm' : '➕ Thêm sản phẩm mới'}
              </div>
              <button
                type="button"
                onClick={() => {
                  setFormOpen(false);
                  setEditingId(null);
                  setCreateError('');
                  setFormErrors({});
                  setImagePreview('');
                }}
                style={{ border: 'none', background: 'transparent', fontSize: '24px', cursor: 'pointer', color: COLORS.gray }}
              >
                ×
              </button>
            </div>
            <form onSubmit={handleCreate}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                {/* Preview ảnh */}
                {imagePreview && (
                  <div style={{ textAlign: 'center', marginBottom: '8px' }}>
                    <img
                      src={imagePreview}
                      alt="Preview"
                      style={{ maxWidth: '100%', maxHeight: '150px', borderRadius: '8px', objectFit: 'cover' }}
                      onError={() => setImagePreview('')}
                    />
                  </div>
                )}
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>
                    Tên sản phẩm <span style={{ color: COLORS.danger }}>*</span>
                  </label>
                  <input
                    name="ten"
                    value={form.ten}
                    onChange={handleChange}
                    placeholder="Nhập tên sản phẩm"
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: `1px solid ${formErrors.ten ? COLORS.danger : COLORS.grayBorder}`,
                      fontSize: '14px',
                      outline: 'none',
                      transition: 'all 0.2s',
                    }}
                    onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                    onBlur={(e) => e.target.style.borderColor = formErrors.ten ? COLORS.danger : COLORS.grayBorder}
                  />
                  {formErrors.ten && <span style={{ color: COLORS.danger, fontSize: '12px', marginTop: '4px' }}>{formErrors.ten}</span>}
                </div>
                <div style={{ display: 'flex', gap: '12px' }}>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>Giá (VNĐ)</label>
                    <input
                      name="gia"
                      type="number"
                      min="0"
                      value={form.gia}
                      onChange={handleChange}
                      placeholder="0"
                      style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        border: `1px solid ${formErrors.gia ? COLORS.danger : COLORS.grayBorder}`,
                        fontSize: '14px',
                        outline: 'none',
                      }}
                      onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                      onBlur={(e) => e.target.style.borderColor = formErrors.gia ? COLORS.danger : COLORS.grayBorder}
                    />
                    {formErrors.gia && <span style={{ color: COLORS.danger, fontSize: '12px', marginTop: '4px' }}>{formErrors.gia}</span>}
                  </div>
                  <div style={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>Tồn kho</label>
                    <input
                      name="tonKho"
                      type="number"
                      min="0"
                      value={form.tonKho}
                      onChange={handleChange}
                      placeholder="0"
                      style={{
                        padding: '10px 12px',
                        borderRadius: '8px',
                        border: `1px solid ${formErrors.tonKho ? COLORS.danger : COLORS.grayBorder}`,
                        fontSize: '14px',
                        outline: 'none',
                      }}
                      onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                      onBlur={(e) => e.target.style.borderColor = formErrors.tonKho ? COLORS.danger : COLORS.grayBorder}
                    />
                    {formErrors.tonKho && <span style={{ color: COLORS.danger, fontSize: '12px', marginTop: '4px' }}>{formErrors.tonKho}</span>}
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>Mô tả</label>
                  <textarea
                    name="moTa"
                    value={form.moTa}
                    onChange={handleChange}
                    placeholder="Mô tả sản phẩm..."
                    rows={3}
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: `1px solid ${COLORS.grayBorder}`,
                      fontSize: '14px',
                      resize: 'vertical',
                      outline: 'none',
                    }}
                    onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                    onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>Chất liệu</label>
                  <input
                    name="chatLieu"
                    value={form.chatLieu}
                    onChange={handleChange}
                    placeholder="VD: Gỗ sồi, Da PU..."
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: `1px solid ${COLORS.grayBorder}`,
                      fontSize: '14px',
                      outline: 'none',
                    }}
                    onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                    onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                  />
                </div>
                <div>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text, display: 'block' }}>Kích thước (cm)</label>
                  <div style={{ display: 'flex', gap: '12px' }}>
                    <div style={{ flex: 1 }}>
                      <input
                        name="dai_cm"
                        type="number"
                        min="0"
                        value={form.dai_cm}
                        onChange={handleChange}
                        placeholder="Dài"
                        style={{
                          padding: '10px 12px',
                          borderRadius: '8px',
                          border: `1px solid ${COLORS.grayBorder}`,
                          fontSize: '14px',
                          width: '100%',
                          outline: 'none',
                        }}
                        onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                        onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                      />
                    </div>
                    <div style={{ flex: 1 }}>
                      <input
                        name="rong_cm"
                        type="number"
                        min="0"
                        value={form.rong_cm}
                        onChange={handleChange}
                        placeholder="Rộng"
                        style={{
                          padding: '10px 12px',
                          borderRadius: '8px',
                          border: `1px solid ${COLORS.grayBorder}`,
                          fontSize: '14px',
                          width: '100%',
                          outline: 'none',
                        }}
                        onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                        onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                      />
                    </div>
                    <div style={{ flex: 1 }}>
                      <input
                        name="cao_cm"
                        type="number"
                        min="0"
                        value={form.cao_cm}
                        onChange={handleChange}
                        placeholder="Cao"
                        style={{
                          padding: '10px 12px',
                          borderRadius: '8px',
                          border: `1px solid ${COLORS.grayBorder}`,
                          fontSize: '14px',
                          width: '100%',
                          outline: 'none',
                        }}
                        onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                        onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                      />
                    </div>
                  </div>
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>Hình ảnh (URL, phân cách bằng dấu phẩy)</label>
                  <input
                    name="hinhAnh"
                    value={form.hinhAnh}
                    onChange={handleChange}
                    placeholder="https://...1.jpg, https://...2.jpg"
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: `1px solid ${COLORS.grayBorder}`,
                      fontSize: '14px',
                      outline: 'none',
                    }}
                    onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                    onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>URL hình đại diện</label>
                  <input
                    name="hinhDaiDien"
                    value={form.hinhDaiDien}
                    onChange={handleChange}
                    placeholder="https://...jpg"
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: `1px solid ${COLORS.grayBorder}`,
                      fontSize: '14px',
                      outline: 'none',
                    }}
                    onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                    onBlur={(e) => e.target.style.borderColor = COLORS.grayBorder}
                  />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column' }}>
                  <label style={{ fontSize: '13px', marginBottom: '6px', fontWeight: 500, color: COLORS.text }}>
                    Danh mục <span style={{ color: COLORS.danger }}>*</span>
                  </label>
                  <select
                    name="danhMucId"
                    value={form.danhMucId}
                    onChange={handleChange}
                    style={{
                      padding: '10px 12px',
                      borderRadius: '8px',
                      border: `1px solid ${formErrors.danhMucId ? COLORS.danger : COLORS.grayBorder}`,
                      fontSize: '14px',
                      outline: 'none',
                      backgroundColor: COLORS.white,
                    }}
                    onFocus={(e) => e.target.style.borderColor = COLORS.primary}
                    onBlur={(e) => e.target.style.borderColor = formErrors.danhMucId ? COLORS.danger : COLORS.grayBorder}
                  >
                    <option value="">-- Chọn danh mục --</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.ten}
                      </option>
                    ))}
                  </select>
                  {formErrors.danhMucId && <span style={{ color: COLORS.danger, fontSize: '12px', marginTop: '4px' }}>{formErrors.danhMucId}</span>}
                </div>
              </div>
              {createError && (
                <div style={{ marginTop: '12px', padding: '10px', borderRadius: '8px', backgroundColor: COLORS.dangerLight, color: COLORS.danger, fontSize: '13px' }}>
                  ⚠️ {createError}
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '12px', marginTop: '20px' }}>
                <button
                  type="button"
                  onClick={() => {
                    setFormOpen(false);
                    setEditingId(null);
                    setCreateError('');
                    setFormErrors({});
                    setImagePreview('');
                  }}
                  style={{
                    padding: '12px 20px',
                    borderRadius: '8px',
                    border: `1px solid ${COLORS.grayBorder}`,
                    backgroundColor: COLORS.white,
                    color: COLORS.text,
                    cursor: 'pointer',
                    fontSize: '14px',
                    fontWeight: 500,
                  }}
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  style={{
                    padding: '12px 24px',
                    borderRadius: '8px',
                    border: 'none',
                    backgroundColor: COLORS.primary,
                    color: 'white',
                    cursor: 'pointer',
                    fontSize: '14px',
                    fontWeight: 500,
                    transition: 'all 0.2s',
                  }}
                  onMouseEnter={(e) => e.target.style.backgroundColor = COLORS.primaryHover}
                  onMouseLeave={(e) => e.target.style.backgroundColor = COLORS.primary}
                >
                  {editingId ? '💾 Cập nhật' : '➕ Thêm mới'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
