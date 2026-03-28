<<<<<<< HEAD
# QuanlyduanAgile-Nhom2
=======
# Quản Lý Nội Thất - Danh Mục Sản Phẩm

Dự án này đã được tích hợp đầy đủ chức năng hiển thị và quản lý danh mục sản phẩm từ 3 nguồn code:
- wedbanhang (React frontend)
- api-banhang (Node.js backend)
- AppBanGhe (Android app)

## Cấu trúc dự án

```
QuanlyduanAgile-Nhom2/
├── api-noithat/          # Backend API
│   ├── src/
│   │   ├── controllers/
│   │   │   └── adminDanhMuc.controller.js
│   │   ├── models/
│   │   │   └── DanhMuc.js
│   │   └── routes/
│   │       └── adminDanhMuc.routes.js
│   ├── server.js
│   └── package.json
├── wed-noithat/          # Frontend React
│   ├── src/
│   │   ├── tabs/
│   │   │   ├── CategoryTab.jsx
│   │   │   └── ProductTab.jsx
│   │   ├── App.jsx
│   │   ├── config.js
│   │   ├── App.css
│   │   └── index.css
│   └── package.json
└── shopNoiThat/         # Android app (đã tích hợp từ AppBanGhe)
    ├── app/src/main/
    │   ├── java/thientct/ph60541/shopnoithat/
    │   │   ├── MainActivity.java
    │   │   ├── ProductAdapter.java
    │   │   └── network/services/CatalogService.java
    │   └── res/
    │       ├── layout/
    │       │   ├── activity_main.xml
    │       │   ├── item_product.xml
    │       │   └── drawer_menu.xml
    │       ├── drawable/
    │       └── values/
    └── build.gradle
```

## Chức năng đã tích hợp

### 1. Quản lý Danh mục (CategoryTab.jsx)
- Hiển thị danh sách danh mục sản phẩm
- Thêm danh mục mới
- Sửa thông tin danh mục
- Xóa danh mục
- Tìm kiếm danh mục theo tên hoặc mô tả
- Giao diện bảng với đầy đủ thông tin (ID, Tên, Mô tả, Thao tác)

### 2. Quản lý Sản phẩm (ProductTab.jsx)
- Hiển thị danh sách sản phẩm với hình ảnh
- Lọc sản phẩm theo danh mục
- Thêm/Sửa/Xóa sản phẩm
- Quản lý đánh giá sản phẩm
- Form chi tiết với đầy đủ thuộc tính (tên, giá, kích thước, chất liệu, etc.)

### 3. Backend API (adminDanhMuc.controller.js)
- API endpoints cho quản lý danh mục:
  - `GET /admin/danh-muc` - Lấy danh sách danh mục
  - `POST /admin/danh-muc` - Thêm danh mục mới
  - `PATCH /admin/danh-muc/:id` - Cập nhật danh mục
  - `DELETE /admin/danh-muc/:id` - Xóa danh mục
- Tự động tạo slug từ tên danh mục
- Validation và error handling

### 4. Android App (shopNoiThat)
- **MainActivity.java** - Màn hình chính với hiển thị danh mục
- **ProductAdapter.java** - Adapter cho RecyclerView sản phẩm
- **CatalogService.java** - Service để gọi API danh mục
- **Giao diện Material Design** với:
  - ChipGroup để hiển thị danh mục
  - RecyclerView cho sản phẩm
  - Drawer menu cho điều hướng
  - Search functionality
  - Cart badge
- **Demo data** cho danh mục: Bàn, Ghế, Sofa, Tủ, Kệ, Giường, Bàn làm việc

## Cài đặt và chạy

### Backend (api-noithat)
```bash
cd api-noithat
npm install
cp env.example .env  # Cấu hình biến môi trường
npm run dev
```

### Frontend (wed-noithat)
```bash
cd wed-noithat
npm install
npm run dev
```

### Android (shopNoiThat)
```bash
cd shopNoiThat
./gradlew assembleDebug    # Build APK
./gradlew installDebug    # Install trên device/emulator
```

## Cấu hình

### Backend (.env)
```
PORT=4000
MONGODB_URI=mongodb://127.0.0.1:27017/noithat
JWT_SECRET=your_jwt_secret_key_here
```

### Frontend (config.js)
```javascript
export const API_BASE_URL = 'http://localhost:4000'
```

### Android (MainActivity.java)
```java
// Demo categories đã được hardcode
String[] demoCategories = {"Bàn", "Ghế", "Sofa", "Tủ", "Kệ", "Giường", "Bàn làm việc"};
```

## Tính năng nổi bật

### Web App
1. **Giao diện hiện đại**: Sử dụng React với CSS styling chuyên nghiệp
2. **Responsive**: Tương thích trên mọi kích thước màn hình
3. **Real-time**: Cập nhật dữ liệu ngay lập tức sau khi thêm/sửa/xóa
4. **Search & Filter**: Tìm kiếm và lọc dữ liệu tiện lợi
5. **Modal forms**: Form thêm/sửa trong modal đẹp mắt
6. **Error handling**: Xử lý lỗi và thông báo người dùng
7. **Data validation**: Kiểm tra dữ liệu đầu vào

### Android App
1. **Material Design**: Giao diện hiện đại theo Google Material Design
2. **Chip Categories**: Danh mục hiển thị dạng chip có thể scroll
3. **RecyclerView**: Hiển thị sản phẩm dạng grid với infinite scroll
4. **Drawer Navigation**: Menu drawer với các chức năng chính
5. **Search Functionality**: Tìm kiếm sản phẩm real-time
6. **Cart Integration**: Badge giỏ hàng với số lượng
7. **Demo Data**: Data mẫu để test chức năng

## API Endpoints

### Danh mục
- `GET /admin/danh-muc` - Lấy tất cả danh mục
- `POST /admin/danh-muc` - Tạo danh mục mới
- `PATCH /admin/danh-muc/:id` - Cập nhật danh mục
- `DELETE /admin/danh-muc/:id` - Xóa danh mục

### Sản phẩm (đã có sẵn)
- `GET /admin/san-pham` - Lấy danh sách sản phẩm
- `POST /admin/san-pham` - Thêm sản phẩm mới
- `PATCH /admin/san-pham/:id` - Cập nhật sản phẩm
- `DELETE /admin/san-pham/:id` - Xóa sản phẩm

## Công nghệ sử dụng

### Backend
- Node.js
- Express.js
- MongoDB + Mongoose
- JWT (cho authentication)
- Morgan (logging)

### Frontend Web
- React 19
- Vite
- CSS3
- Fetch API

### Android
- Java
- Android SDK
- RecyclerView
- Material Components
- Retrofit (cho API calls)
- Glide (cho image loading)

## Lưu ý

- Backend chạy trên port 4000 (mặc định)
- Frontend chạy trên port 5173 (Vite default)
- Cần cài đặt MongoDB để chạy được dự án
- Có thể cấu hình lại các port trong file .env và package.json
- Android app sử dụng demo data, cần kết nối với API để sử dụng data thật
>>>>>>> develop
