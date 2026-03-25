import React, { useState, useEffect } from 'react';
import { API_BASE_URL } from './config.js';
import './components/AdminLayout.css';

export function ProductDetailPage({ productId, onClose }) {
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeImageIndex, setActiveImageIndex] = useState(0);
  const [selectedQuantity, setSelectedQuantity] = useState(1);

  useEffect(() => {
    if (!productId) return;
    
    async function fetchProductDetail() {
      try {
        setLoading(true);
        setError('');
        
        const res = await fetch(`${API_BASE_URL}/san-pham/${productId}`);
        if (!res.ok) {
          throw new Error(`Không thể tải chi tiết sản phẩm: ${res.status}`);
        }
        
        const data = await res.json();
        setProduct(data);
        setActiveImageIndex(0); // Reset to first image
      } catch (err) {
        setError(err.message || 'Không thể tải thông tin sản phẩm');
      } finally {
        setLoading(false);
      }
    }

    fetchProductDetail();
  }, [productId]);

  function formatPrice(price) {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(price);
  }

  function handleImageClick(index) {
    setActiveImageIndex(index);
  }

  function handleQuantityChange(action) {
    if (!product) return;
    
    if (action === 'increase' && selectedQuantity < (product.tonKho || 999)) {
      setSelectedQuantity(prev => prev + 1);
    } else if (action === 'decrease' && selectedQuantity > 1) {
      setSelectedQuantity(prev => prev - 1);
    }
  }

  function handleAddToCart() {
    if (!product) return;
    
    // TODO: Implement add to cart functionality
    alert(`Đã thêm ${selectedQuantity} ${product.ten} vào giỏ hàng`);
  }

  if (loading) {
    return (
      <div className="admin-root" style={{ justifyContent: 'center', alignItems: 'center' }}>
        <div className="admin-panel-placeholder">Đang tải chi tiết sản phẩm...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="admin-root" style={{ justifyContent: 'center', alignItems: 'center' }}>
        <div className="admin-panel-placeholder" style={{ color: '#b91c1c' }}>
          {error}
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="admin-root" style={{ justifyContent: 'center', alignItems: 'center' }}>
        <div className="admin-panel-placeholder">Không tìm thấy sản phẩm</div>
      </div>
    );
  }

  const allImages = product.hinhAnh || [];
  const currentImage = product.hinhDaiDien || (allImages.length > 0 ? allImages[0] : '');

  return (
    <div className="admin-root" style={{ justifyContent: 'center', alignItems: 'center' }}>
      <div className="admin-panel" style={{ maxWidth: 1200, width: '100%', maxHeight: '90vh', overflowY: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
          <div className="admin-panel-title">Chi Tiết Sản Phẩm</div>
          <button
            type="button"
            onClick={onClose}
            style={{
              border: 'none',
              background: 'transparent',
              fontSize: 24,
              cursor: 'pointer',
              color: '#6b7280'
            }}
          >
            ×
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 30 }}>
          {/* Left Column - Images */}
          <div>
            {/* Main Image */}
            <div style={{
              width: '100%',
              height: 400,
              backgroundColor: '#f9fafb',
              borderRadius: 8,
              marginBottom: 15,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              overflow: 'hidden',
              border: '1px solid #e5e7eb'
            }}>
              {currentImage ? (
                <img
                  src={currentImage}
                  alt={product.ten}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'cover'
                  }}
                />
              ) : (
                <div style={{ color: '#9ca3af', textAlign: 'center', padding: 20 }}>
                  Không có hình ảnh
                </div>
              )}
            </div>

            {/* Thumbnail Gallery */}
            {allImages.length > 1 && (
              <div style={{ display: 'flex', gap: 8, overflowX: 'auto' }}>
                {allImages.map((img, index) => (
                  <div
                    key={index}
                    onClick={() => handleImageClick(index)}
                    style={{
                      width: 60,
                      height: 60,
                      borderRadius: 4,
                      overflow: 'hidden',
                      cursor: 'pointer',
                      border: activeImageIndex === index ? '2px solid #111827' : '1px solid #e5e7eb',
                      flexShrink: 0
                    }}
                  >
                    <img
                      src={img}
                      alt={`${product.ten} ${index + 1}`}
                      style={{
                        width: '100%',
                        height: '100%',
                        objectFit: 'cover'
                      }}
                    />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Right Column - Product Info */}
          <div>
            {/* Product Name */}
            <h1 style={{ fontSize: 24, fontWeight: 'bold', marginBottom: 10, color: '#111827' }}>
              {product.ten}
            </h1>

            {/* Price */}
            <div style={{ fontSize: 28, fontWeight: 'bold', color: '#f59e0b', marginBottom: 15 }}>
              {formatPrice(product.gia)}
            </div>

            {/* Stock Status */}
            <div style={{ marginBottom: 20 }}>
              {product.tonKho > 0 ? (
                <div style={{ color: '#16a34a', fontSize: 14, fontWeight: 600 }}>
                  ✓ Còn hàng ({product.tonKho} sản phẩm)
                </div>
              ) : (
                <div style={{ color: '#dc2626', fontSize: 14, fontWeight: 600 }}>
                  ✗ Hết hàng
                </div>
              )}
            </div>

            {/* Category */}
            {product.danhMuc && (
              <div style={{ marginBottom: 15 }}>
                <span style={{ color: '#6b7280', fontSize: 14 }}>Danh mục: </span>
                <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                  {product.danhMuc.ten}
                </span>
              </div>
            )}

            {/* Description */}
            {product.moTa && (
              <div style={{ marginBottom: 20 }}>
                <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 8, color: '#111827' }}>
                  Mô tả sản phẩm
                </h3>
                <p style={{ color: '#4b5563', lineHeight: 1.6, fontSize: 14 }}>
                  {product.moTa}
                </p>
              </div>
            )}

            {/* Specifications */}
            <div style={{ marginBottom: 25 }}>
              <h3 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12, color: '#111827' }}>
                Thông số kỹ thuật
              </h3>
              <div style={{ backgroundColor: '#f9fafb', borderRadius: 8, padding: 15 }}>
                <div style={{ display: 'grid', gap: 10 }}>
                  {product.chatLieu && (
                    <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                      <span style={{ color: '#6b7280', fontSize: 14 }}>Chất liệu:</span>
                      <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                        {product.chatLieu}
                      </span>
                    </div>
                  )}
                  
                  {product.kichThuoc && (
                    <>
                      {product.kichThuoc.dai_cm && (
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span style={{ color: '#6b7280', fontSize: 14 }}>Chiều dài:</span>
                          <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                            {product.kichThuoc.dai_cm} cm
                          </span>
                        </div>
                      )}
                      {product.kichThuoc.rong_cm && (
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span style={{ color: '#6b7280', fontSize: 14 }}>Chiều rộng:</span>
                          <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                            {product.kichThuoc.rong_cm} cm
                          </span>
                        </div>
                      )}
                      {product.kichThuoc.cao_cm && (
                        <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                          <span style={{ color: '#6b7280', fontSize: 14 }}>Chiều cao:</span>
                          <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                            {product.kichThuoc.cao_cm} cm
                          </span>
                        </div>
                      )}
                    </>
                  )}
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#6b7280', fontSize: 14 }}>Tồn kho:</span>
                    <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                      {product.tonKho || 0} sản phẩm
                    </span>
                  </div>
                  
                  <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                    <span style={{ color: '#6b7280', fontSize: 14 }}>Đã bán:</span>
                    <span style={{ color: '#111827', fontSize: 14, fontWeight: 600 }}>
                      {product.daBan || 0} sản phẩm
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* Quantity Selector and Add to Cart */}
            {product.tonKho > 0 && (
              <div style={{ display: 'flex', gap: 15, alignItems: 'center', marginBottom: 25 }}>
                <div style={{ display: 'flex', alignItems: 'center', border: '1px solid #e5e7eb', borderRadius: 6 }}>
                  <button
                    onClick={() => handleQuantityChange('decrease')}
                    disabled={selectedQuantity <= 1}
                    style={{
                      padding: '8px 12px',
                      border: 'none',
                      backgroundColor: 'transparent',
                      cursor: selectedQuantity <= 1 ? 'not-allowed' : 'pointer',
                      fontSize: 18
                    }}
                  >
                    -
                  </button>
                  <span style={{ padding: '8px 16px', fontSize: 16, fontWeight: 600, minWidth: 50, textAlign: 'center' }}>
                    {selectedQuantity}
                  </span>
                  <button
                    onClick={() => handleQuantityChange('increase')}
                    disabled={selectedQuantity >= (product.tonKho || 999)}
                    style={{
                      padding: '8px 12px',
                      border: 'none',
                      backgroundColor: 'transparent',
                      cursor: selectedQuantity >= (product.tonKho || 999) ? 'not-allowed' : 'pointer',
                      fontSize: 18
                    }}
                  >
                    +
                  </button>
                </div>

                <button
                  onClick={handleAddToCart}
                  style={{
                    padding: '12px 24px',
                    backgroundColor: '#111827',
                    color: 'white',
                    border: 'none',
                    borderRadius: 6,
                    cursor: 'pointer',
                    fontSize: 16,
                    fontWeight: 600,
                    flex: 1
                  }}
                >
                  Thêm vào giỏ hàng
                </button>
              </div>
            )}

            {/* Out of Stock Message */}
            {product.tonKho <= 0 && (
              <div style={{
                padding: 15,
                backgroundColor: '#fef2f2',
                border: '1px solid #fecaca',
                borderRadius: 6,
                textAlign: 'center',
                color: '#dc2626',
                fontWeight: 600
              }}>
                Sản phẩm hiện đang hết hàng
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
