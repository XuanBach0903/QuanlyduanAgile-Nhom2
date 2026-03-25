import React, { useState, useEffect } from 'react'
import './App.css'
import { CategoryTab } from './tabs/CategoryTab.jsx'
import { ProductTab } from './tabs/ProductTab.jsx'
import { LoginPage } from './LoginPage.jsx'
import { RegisterPage } from './RegisterPage.jsx'

function App() {
  const [activeTab, setActiveTab] = useState('categories')
  const [isAuthenticated, setIsAuthenticated] = useState(false)
  const [showLogin, setShowLogin] = useState(true)
  const [currentUser, setCurrentUser] = useState(null)

  useEffect(() => {
    const token = localStorage.getItem('adminToken')
    const user = localStorage.getItem('adminUser')
    
    if (token && user) {
      setIsAuthenticated(true)
      setCurrentUser(JSON.parse(user))
    }
  }, [])

  function handleLoginSuccess(token, user) {
    localStorage.setItem('adminToken', token)
    localStorage.setItem('adminUser', JSON.stringify(user))
    setIsAuthenticated(true)
    setCurrentUser(user)
  }

  function handleLogout() {
    localStorage.removeItem('adminToken')
    localStorage.removeItem('adminUser')
    setIsAuthenticated(false)
    setCurrentUser(null)
    setActiveTab('categories')
  }

  function handleRegisterSuccess(userData) {
    // After successful registration, switch to login
    setShowLogin(true)
  }

  if (!isAuthenticated) {
    return (
      <div className="app">
        <div style={{ 
          position: 'absolute', 
          top: '20px', 
          right: '20px', 
          zIndex: 1000 
        }}>
          <button
            onClick={() => setShowLogin(!showLogin)}
            style={{
              padding: '8px 16px',
              backgroundColor: '#111827',
              color: 'white',
              border: 'none',
              borderRadius: '6px',
              cursor: 'pointer',
              fontSize: '13px'
            }}
          >
            {showLogin ? 'Đăng ký' : 'Đăng nhập'}
          </button>
        </div>
        
        {showLogin ? (
          <LoginPage onLoginSuccess={handleLoginSuccess} />
        ) : (
          <RegisterPage onRegisterSuccess={handleRegisterSuccess} />
        )}
      </div>
    )
  }

  return (
    <div className="app">
      <header className="app-header">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%' }}>
          <h1>Quản Lý Nội Thất</h1>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span style={{ fontSize: '14px', color: '#6b7280' }}>
              Xin chào, {currentUser?.hoTen || 'Admin'}
            </span>
            <button
              onClick={handleLogout}
              style={{
                padding: '6px 12px',
                backgroundColor: '#dc2626',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer',
                fontSize: '12px'
              }}
            >
              Đăng xuất
            </button>
          </div>
        </div>
        <div className="tab-buttons">
          <button
            className={`tab-btn ${activeTab === 'categories' ? 'active' : ''}`}
            onClick={() => setActiveTab('categories')}
          >
            Danh Mục
          </button>
          <button
            className={`tab-btn ${activeTab === 'products' ? 'active' : ''}`}
            onClick={() => setActiveTab('products')}
          >
            Sản Phẩm
          </button>
        </div>
      </header>
      <main className="app-main">
        {activeTab === 'categories' && <CategoryTab />}
        {activeTab === 'products' && <ProductTab />}
      </main>
    </div>
  )
}

export default App
