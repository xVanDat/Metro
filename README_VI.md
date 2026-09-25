# Metro

Trình phát nhạc ngoại tuyến theo Material Design dành cho Android, được phát triển dựa trên
[MuntashirAkon/Metro](https://github.com/MuntashirAkon/Metro).

[English](README.md)

Bản fork này giữ nguyên tính tự do và khả năng hoạt động hoàn toàn ngoại tuyến của Metro, đồng
thời bổ sung các bản sửa lỗi lưu trữ, công cụ phát nhạc và giao diện phù hợp hơn với Android mới.

## Những tính năng được thêm so với Metro gốc

### Thư viện và bộ nhớ

- Sửa blacklist thư mục trên Android 13/14 bằng trình chọn thư mục hệ thống.
- Blacklist hoạt động với cả bộ nhớ trong và thẻ SD rời.
- Chức năng **Ẩn bài hát** riêng biệt, kèm màn hình khôi phục bài đã ẩn trong Cài đặt.
- Nhập tệp playlist bằng Android Files. Hỗ trợ M3U, M3U8, PLS, XSPF, WPL và ASX; vẫn có thể
  nhập playlist từ MediaStore.
- Quét lại toàn bộ thư viện trên các vùng lưu trữ khả dụng.
- Tìm kiếm không phân biệt dấu, thuận tiện hơn với tên bài hát tiếng Việt.
- Backup mở rộng cho blacklist, lịch sử, số lần phát và hàng đợi hiện tại.

### Phát nhạc

- Ghi nhớ vị trí riêng cho từng tệp âm thanh dài, phù hợp với mix, podcast và sách nói.
- Dấu trang phát nhạc có thể thêm, mở và xóa ngay trong trình phát.
- Chuẩn hóa âm lượng ReplayGain an toàn với chế độ bài hát và album.
- Cải thiện cách hiển thị lời đồng bộ trong các giao diện trình phát mới.

### Giao diện và tùy biến

- Tab **Trang chủ** với lời chào tự đổi theo buổi sáng, chiều và tối.
- Giao diện trình phát chuyển sắc chuyển động lấy màu từ ảnh album.
- Giao diện nghe nhạc ngoại tuyến lấy cảm hứng từ YouTube Music và Spotify.
- Chọn phông chữ TTF/OTF bằng Android Files và dễ dàng trở về phông mặc định.
- Trình chọn giao diện tối giản và phần Cài đặt được sắp xếp lại.
- Giao diện tiếng Anh và tiếng Việt đầy đủ.

## Các tính năng chính của Metro

- Phát nhạc hoàn toàn ngoại tuyến, không yêu cầu quyền Internet.
- Duyệt nhạc theo bài hát, album, nghệ sĩ, playlist, thể loại và thư mục.
- Nhiều giao diện ứng dụng và trình phát, hỗ trợ Material You cùng màu động.
- Phát liền mạch, chuyển bài mượt, hẹn giờ tắt và chế độ lái xe.
- Lời bài hát đồng bộ, sửa thẻ nhạc, sắp xếp hàng đợi và playlist thông minh.
- Android Auto, điều khiển tai nghe/Bluetooth, widget và màn hình khóa.
- Tạo, chỉnh sửa, xuất và nhập playlist từ MediaStore.

## Ngôn ngữ

Ứng dụng chỉ đi kèm tài nguyên tiếng Anh và tiếng Việt. Tùy chọn **Theo hệ thống** sẽ dùng tiếng
Việt trên thiết bị đặt tiếng Việt và tự chuyển về tiếng Anh với các ngôn ngữ hệ thống khác. Bạn
cũng có thể chọn trực tiếp một trong hai ngôn ngữ trong Cài đặt.

Mọi chuỗi mới hiển thị cho người dùng trong bản fork phải được thêm đồng thời vào tài nguyên
tiếng Anh và tiếng Việt.

## Biên dịch

Không cần Android Studio. Xem [BUILDING.md](BUILDING.md) để biết lệnh PowerShell tạo APK debug
và release có chữ ký, bao gồm cấu hình dành cho máy ít RAM.

## Giấy phép

Metro được phát hành theo [GNU General Public License v3.0](LICENSE.md).

> Metro là trình phát nhạc ngoại tuyến. Ứng dụng không tải xuống hoặc phát nhạc trực tuyến.
