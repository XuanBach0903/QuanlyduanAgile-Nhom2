import React, { useState, useEffect } from 'react';
import { API_BASE_URL } from './config.js';
import { ProductDetailPage } from './ProductDetailPage.jsx';
import './components/AdminLayout.css';

export function SearchPage() {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState('');
  const [priceMin, setPriceMin] = useState('');
  const [priceMax, setPriceMax] = useState('');
  const [stockMin, setStockMin] = useState('');
  const [sortBy, setSortBy] = useState('ngay_tao');
  const [sortOrder, setSortOrder] = useState('desc');
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [selectedProductId, setSelectedProductId] = useState(null);

  // Load categories for filter dropdown
  useEffect(() => {
    async function fetchCategories() {
      try {
        const token = localStorage.getItem('adminToken');
        const res = await fetch(`${API_BASE_URL}/admin/danh-muc`, {
          headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
          },
        });
        if (res.ok) {
          const data = await res.json();
          setCategories(Array.isArray(data) ? data : data.danhSach || []);
        }
      } catch (err) {
        console.error('Failed to load categories:', err);
      }
    }
    fetchCategories();
  }, []);

  // Search products
  useEffect(() => {
    let active = true;
    async function searchProducts() {
      try {
        setLoading(true);
        setError('');
        
        const params = new URLSearchParams();
        if (searchTerm.trim()) {
          params.set('tuKhoa', searchTerm.trim());
        }
        if (selectedCategoryId) {
          params.set('danhMucId', selectedCategoryId);
        }
        if (priceMin) {
          params.set('giaMin', priceMin);
        }
        if (priceMax) {
          params.set('giaMax', priceMax);
        }
        if (stockMin) {
          params.set('tonKhoMin', stockMin);
        }
        params.set('sortBy', sortBy);
        params.set('sortOrder', sortOrder);
        params.set('trang', currentPage);
        params.set('gioiHan', 12);

        const url = `${API_BASE_URL}/san-pham${params.toString() ? `?${params.toString()}` : ''}`;
        const res = await fetch(url, {
          headers: {
            'Content-Type': 'application/json',
          },
        });

        if (!res.ok) {
          throw new Error(`Tìm kiếm thất bại: ${res.status}`);
        }

        const data = await res.json();
        if (!active) return;

        setProducts(data.danhSach || []);
        setTotalPages(data.tongTrang || 1);
      } catch (err) {
        if (!active) return;
        setError(err.message || 'Không thể tìm kiếm sản phẩm');
      } finally {
        if (active) setLoading(false);
      }
    }

    if (searchTerm.trim() || selectedCategoryId || priceMin || priceMax || stockMin) {
      searchProducts();
    } else {
      setProducts([]);
      setTotalPages(1);
    }

    return () => {
      active = false;
    };
  }, [searchTerm, selectedCategoryId, priceMin, priceMax, stockMin, sortBy, sortOrder, currentPage]);

  function handleSearch(e) {
    e.preventDefault();
    setCurrentPage(1); // Reset to first page when searching
  }

  function handlePageChange(page) {
    setCurrentPage(page);
  }

  function handleProductClick(productId) {
    setSelectedProductId(productId);
  }

  function handleCloseDetail() {
    setSelectedProductId(null);
  }

  function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  }

  return (
    <>
      {selectedProductId ? (
        <ProductDetailPage 
          productId={selectedProductId} 
          onClose={handleCloseDetail} 
        />
      ) : (
        <div className="admin-root">
          <div className="admin-panel">
            <div className="admin-panel-title">Tìm Kiếm Sản Phẩm</div>
        
        {/* Search Form */}
        <form onSubmit={handleSearch} style={{ marginBottom: 20 }}>
          {/* Basic Search Row */}
          <div style={{ display: 'flex', gap: 10, marginBottom: 15 }}>
            <input
              type="text"
              placeholder="Nhập tên sản phẩm cần tìm..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              style={{
                flex: 1,
                padding: '8px 12px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 14,
              }}
            />
            
            <select
              value={selectedCategoryId}
              onChange={(e) => setSelectedCategoryId(e.target.value)}
              style={{
                padding: '8px 12px',
                borderRadius: 6,
                border: '1px solid #e5e7eb',
                fontSize: 14,
                minWidth: 150,
              }}
            >
              <option value="">Tất cả danh mục</option>
              {categories.map(cat => (
                <option key={cat.id} value={cat.id}>
                  {cat.ten}
                </option>
              ))}
            </select>
            
            <button
              type="submit"
              className="admin-header-btn"
              disabled={loading}
              style={{ padding: '8px 16px' }}
            >
              {loading ? 'Đang tìm...' : 'Tìm kiếm'}
            </button>
          </div>

          {/* Advanced Filters Row */}
          <div style={{ display: 'flex', gap: 10, marginBottom: 15, flexWrap: 'wrap' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
              <label style={{ fontSize: 13, color: '#6b7280' }}>Giá:</label>
              <input
                type="number"
                placeholder="Từ"
                value={priceMin}
                onChange={(e) => setPriceMin(e.target.value)}
                style={{
                  width: 80,
                  padding: '6px 8px',
                  borderRadius: 4,
                  border: '1px solid #e5e7eb',
                  fontSize: 13,
                }}
              />
              <span style={{ color: '#6b7280' }}>-</span>
              <input
                type="number"
                placeholder="Đến"
                value={priceMax}
                onChange={(e) => setPriceMax(e.target.value)}
                style={{
                  width: 80,
                  padding: '6px 8px',
                  borderRadius: 4,
                  border: '1px solid #e5e7eb',
                  fontSize: 13,
                }}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
              <label style={{ fontSize: 13, color: '#6b7280' }}>Tồn kho tối thiểu:</label>
              <input
                type="number"
                placeholder="0"
                value={stockMin}
                onChange={(e) => setStockMin(e.target.value)}
                style={{
                  width: 60,
                  padding: '6px 8px',
                  borderRadius: 4,
                  border: '1px solid #e5e7eb',
                  fontSize: 13,
                }}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: 5 }}>
              <label style={{ fontSize: 13, color: '#6b7280' }}>Sắp xếp:</label>
              <select
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
                style={{
                  padding: '6px 8px',
                  borderRadius: 4,
                  border: '1px solid #e5e7eb',
                  fontSize: 13,
                }}
              >
                <option value="ngay_tao">Ngày tạo</option>
                <option value="gia">Giá</option>
                <option value="ten">Tên</option>
                <option value="ton_kho">Tồn kho</option>
              </select>
              <select
                value={sortOrder}
                onChange={(e) => setSortOrder(e.target.value)}
                style={{
                  padding: '6px 8px',
                  borderRadius: 4,
                  border: '1px solid #e5e7eb',
                  fontSize: 13,
                }}
              >
                <option value="desc">Giảm dần</option>
                <option value="asc">Tăng dần</option>
              </select>
            </div>

            <button
              type="button"
              onClick={() => {
                setSearchTerm('');
                setSelectedCategoryId('');
                setPriceMin('');
                setPriceMax('');
                setStockMin('');
                setSortBy('ngay_tao');
                setSortOrder('desc');
                setCurrentPage(1);
              }}
              style={{
                padding: '6px 12px',
                borderRadius: 4,
                border: '1px solid #dc2626',
                backgroundColor: 'white',
                color: '#dc2626',
                cursor: 'pointer',
                fontSize: 13,
              }}
            >
              Xóa bộ lọc
            </button>
          </div>
        </form>

        {/* Error Message */}
        {error && (
          <div style={{ 
            color: '#b91c1c', 
            fontSize: 13, 
            marginBottom: 12, 
            padding: '8px', 
            backgroundColor: '#fef2f2', 
            borderRadius: 4 
          }}>
            {error}
          </div>
        )}

        {/* Loading State */}
        {loading && (
          <div style={{ textAlign: 'center', padding: 20, color: '#6b7280' }}>
            Đang tìm kiếm sản phẩm...
          </div>
        )}

        {/* Search Results */}
        {!loading && products.length > 0 && (
          <div>
            <div style={{ marginBottom: 12, fontSize: 14, color: '#6b7280' }}>
              Tìm thấy {products.length} sản phẩm
            </div>
            
            <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 12, fontStyle: 'italic' }}>
              Nhấp vào sản phẩm để xem chi tiết
            </div>
            
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(250px, 1fr))', gap: 16 }}>
              {products.map(product => (
                <div key={product.id} style={{
                  border: '1px solid #e5e7eb',
                  borderRadius: 8,
                  padding: 12,
                  backgroundColor: 'white',
                  cursor: 'pointer'
                }}
                onClick={() => handleProductClick(product.id)}>
                  {/* Product Image */}
                  <div style={{
                    width: '100%',
                    height: 150,
                    backgroundColor: '#f9fafb',
                    borderRadius: 4,
                    marginBottom: 8,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    overflow: 'hidden'
                  }}>
                    {product.hinhDaiDien ? (
                      <img 
                        src={product.hinhDaiDien} 
                        alt={product.ten}
                        style={{ 
                          width: '100%', 
                          height: '100%', 
                          objectFit: 'cover' 
                        }}
                      />
                    ) : (
                      <div style={{ color: '#9ca3af', fontSize: 12 }}>
                        Không có hình ảnh
                      </div>
                    )}
                  </div>

                  {/* Product Info */}
                  <div>
                    <div style={{
                      fontSize: 14,
                      fontWeight: 600,
                      marginBottom: 4,
                      color: '#111827',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}>
                      {product.ten}
                    </div>
                    
                    <div style={{
                      fontSize: 16,
                      fontWeight: 'bold',
                      color: '#f59e0b',
                      marginBottom: 8
                    }}>
                      {formatPrice(product.gia)}
                    </div>

                    <div style={{ fontSize: 12, color: '#6b7280', marginBottom: 4 }}>
                      Tồn kho: {product.tonKho || 0}
                    </div>

                    {product.soSaoTrungBinh > 0 && (
                      <div style={{ fontSize: 12, color: '#6b7280' }}>
                        ⭐ {product.soSaoTrungBinh.toFixed(1)} ({product.soLuotDanhGia} đánh giá)
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>

            {/* Pagination */}
            {totalPages > 1 && (
              <div style={{ 
                display: 'flex', 
                justifyContent: 'center', 
                gap: 8, 
                marginTop: 20 
              }}>
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  style={{
                    padding: '6px 12px',
                    border: '1px solid #e5e7eb',
                    borderRadius: 4,
                    backgroundColor: currentPage === 1 ? '#f9fafb' : 'white',
                    cursor: currentPage === 1 ? 'not-allowed' : 'pointer'
                  }}
                >
                  Trước
                </button>
                
                {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                  <button
                    key={page}
                    onClick={() => handlePageChange(page)}
                    style={{
                      padding: '6px 12px',
                      border: '1px solid #e5e7eb',
                      borderRadius: 4,
                      backgroundColor: currentPage === page ? '#111827' : 'white',
                      color: currentPage === page ? 'white' : '#111827',
                      cursor: 'pointer'
                    }}
                  >
                    {page}
                  </button>
                ))}
                
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  style={{
                    padding: '6px 12px',
                    border: '1px solid #e5e7eb',
                    borderRadius: 4,
                    backgroundColor: currentPage === totalPages ? '#f9fafb' : 'white',
                    cursor: currentPage === totalPages ? 'not-allowed' : 'pointer'
                  }}
                >
                  Sau
                </button>
              </div>
            )}
          </div>
        )}

        {/* No Results */}
        {!loading && !error && (searchTerm || selectedCategoryId || priceMin || priceMax || stockMin) && products.length === 0 && (
          <div style={{ 
            textAlign: 'center', 
            padding: 40, 
            color: '#6b7280' 
          }}>
            Không tìm thấy sản phẩm nào phù hợp với bộ lọc đã chọn
          </div>
        )}

        {/* Initial State */}
        {!loading && !error && !searchTerm && !selectedCategoryId && !priceMin && !priceMax && !stockMin && (
          <div style={{ 
            textAlign: 'center', 
            padding: 40, 
            color: '#6b7280' 
          }}>
            Nhập từ khóa hoặc chọn bộ lọc để tìm kiếm sản phẩm
          </div>
        )}
          </div>
        </div>
      )}
    </>
  );
}
