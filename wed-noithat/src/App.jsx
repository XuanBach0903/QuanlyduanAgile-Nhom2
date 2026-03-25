import React, { useState } from 'react'
import './App.css'
import { CategoryTab } from './tabs/CategoryTab.jsx'
import { ProductTab } from './tabs/ProductTab.jsx'

function App() {
  const [activeTab, setActiveTab] = useState('categories')

  return (
    <div className="app">
      <header className="app-header">
        <h1>Quản Lý Nội Thất</h1>
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
