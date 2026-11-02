# Danh mục, sản phẩm, tìm kiếm và bộ lọc — 20 phần

## Phạm vi và cách chạy

Nguồn tham khảo: `CarShowroom-copy/backend` (Category, CategoryRepo,
CatalogController, SearchService) và luồng catalog của frontend nguồn.
Đích triển khai: Java 21, Spring Boot và Thymeleaf trong `src/main`.
Không cần Node/Vue để chạy các trang Java bên dưới.

Kế hoạch ban đầu chia 20 ngày theo nhóm tính năng, chưa có 20 đặc tả chi tiết.
Bảng này cụ thể hóa phạm vi dựa trên dữ liệu Car hiện có, không đồng nghĩa
đã chuyển mọi module của dự án nguồn.

Chạy ứng dụng bằng Maven/Spring Boot như dự án hiện tại. Mở `/catalog`,
hoặc chọn “Khám phá sản phẩm · Tìm kiếm & bộ lọc” trên trang `/home`.
Trang chi tiết là `/catalog/{id}`. Form GET giữ các điều kiện trong URL;
phân trang gửi lại toàn bộ bộ lọc và nút xóa lọc trở về `/catalog`.

## Đối chiếu 20 phần

| Phần | Kết quả trong bản Java |
|---|---|
| 1 | Danh mục có mã và tên ổn định, tương thích dữ liệu Car |
| 2 | Category lưu DB, nhóm và thứ tự; chỉ bổ sung danh mục còn thiếu |
| 3 | Đếm sản phẩm thực tế và hiển thị cả danh mục trống |
| 4 | Điều hướng danh mục từ `/catalog` và API metadata riêng |
| 5 | Danh sách sản phẩm từ CarRepository, không dùng dữ liệu giả ở giao diện |
| 6 | Phân trang và tổng số kết quả, giới hạn tối đa 60 mục/trang |
| 7 | Trang Java và API chi tiết sản phẩm, trả 404 khi không tồn tại |
| 8 | Giá, giá cũ hợp lệ, ảnh sản phẩm và ảnh tải chậm |
| 9 | Mã sản phẩm, tỷ lệ, vật liệu, mô tả, tính năng và tình trạng còn hàng |
| 10 | Sản phẩm cùng danh mục, loại chính sản phẩm đang xem |
| 11 | Tìm kiếm không phân biệt hoa/thường và dấu tiếng Việt |
| 12 | Tìm theo tên, mã, danh mục và thuộc tính/mô tả |
| 13 | Đồng nghĩa cơ bản và chịu một lỗi gõ với từ dài từ 4 ký tự |
| 14 | Nhận tỷ lệ 1:10/1/10, giá “dưới/trên/từ/tối đa”, đơn vị k/tr/triệu; gợi ý tối đa 6 mục |
| 15 | Lọc danh mục và nhóm, áp dụng đồng thời với từ khóa |
| 16 | Lọc giá từ/đến, kết hợp với mức giá trích từ câu tìm kiếm |
| 17 | Lọc tỷ lệ, đánh giá tối thiểu và còn hàng |
| 18 | Sắp xếp giá, tên, đánh giá, bán chạy và mới nhập, có ID làm tiêu chí phụ ổn định |
| 19 | Kiểm tra dữ liệu lọc, chống tràn phân trang, thông báo rỗng, escape nội dung, bố cục điện thoại |
| 20 | Kiểm tra hồi quy Java, API và render Thymeleaf; bảo toàn dữ liệu khi khởi động lại |

## API

- `GET /api/products`: các tham số `q`, `category` (hoặc alias `type`),
  `grp`, `scale`, `minPrice`, `maxPrice`, `minRating`, `inStock=true`,
  `sort`, `page` (bắt đầu 0), `size` (mặc định 12).
- `GET /api/products/{id}`: `product`, `related`, `variants`, `flash`.
- `GET /api/products/suggest?q=...`: tên/mã/ảnh của tối đa 6 sản phẩm phù hợp.
- `GET /api/products/search-meta?q=...`: tỷ lệ, giá min/max và các từ tìm kiếm.
- `GET /api/catalog/categories`: mã, tên, nhóm, ảnh và số lượng thực tế.
- `GET /api/catalog/options`: tỷ lệ hiện có và tên các kiểu sắp xếp.

Ví dụ: `/catalog?q=RC+1%3A10+dưới+3+triệu&inStock=true&sort=price_asc`.
Các điều kiện lọc giao nhau (AND). Danh mục có thể truyền nhiều mã phân tách
bằng dấu phẩy qua API. Sai giá, đánh giá, kiểu sắp xếp hoặc kiểu số trả HTTP 400.
Số trang âm về 0; size được giới hạn 1–60. Từ khóa tối đa 200 ký tự.

## Giới hạn thực tế

- Đây là catalog đọc dữ liệu, không thêm chức năng sửa/xóa sản phẩm công khai.
  Checkout, thanh toán, biến thể, đánh giá do khách gửi, tìm bằng ảnh và quản trị
  thuộc các module riêng, không nằm trong 20 phần trên.
- Car chỉ có boolean `inStock`; không tạo số lượng tồn kho giả và chưa liên kết
  với module kho của thành viên khác. Không có filter theo tuổi hay brand ID
  vì dữ liệu Car chưa có các trường đó. Tên hãng vẫn tìm được bằng từ khóa.
- `newest` dựa trên ID tăng dần khi nhập, chưa có trường thời điểm tạo.
  `featured` dùng số đã bán vì Car chưa có cờ featured. Điểm và số đã bán
  phản ánh dữ liệu hiện có, bao gồm giá trị mẫu có sẵn trong entity.
- Thuật toán tải catalog vào bộ nhớ để tìm không dấu và chịu lỗi gõ như cách
  làm nguồn; phù hợp catalog nhỏ hiện tại. Catalog lớn cần chỉ mục tìm kiếm
  và lọc/phân trang ở DB trước khi mở rộng.
- Giữ `/api/cars` và `/api/categories` cũ để không phá giao diện hiện có.
  Vue trong `frontend/` là bản tham khảo đã nhập trước; chưa tương thích
  hoàn toàn hợp đồng category, stock, brand và các API ngoài catalog.
- Không có migration tự động dữ liệu sản phẩm nguồn hoặc ghi đè DB hiện tại.
  Sản phẩm hiện có và ID được giữ nguyên; chỉ DB rỗng nhận dữ liệu mẫu.

## Kiểm tra

```sh
mvn -Dtest=CatalogServiceTests,CatalogWebTests,CarSeedTests,ProductApiControllerTests,CategoryServiceTests test
node --check src/main/resources/static/js/catalog.js
```

24 kiểm tra Java đã đạt trong lần triển khai: 13 kiểm tra dịch vụ catalog,
5 kiểm tra HTTP/render Thymeleaf và 6 kiểm tra chi tiết, seed và danh mục cũ.
Các kiểm tra web dùng repository mock, không kết nối hoặc sửa DB thật.
Chưa kiểm thử toàn bộ ứng dụng với SQL Server hoặc toàn bộ checkout.
