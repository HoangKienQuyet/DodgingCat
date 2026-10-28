# Windows x64 portable

## Bản source trực tiếp dùng cho bài nộp

`DodgingCat-source-portable-Windows-x64.zip` có `src`, `pom.xml`, README và
asset gốc ngay ở cấp đầu. Giải nén ZIP, mở `Run.cmd` để chơi. Lần đầu script
giải nén `support/platform.7z` chứa Java 25, JavaFX và thư viện; không cần mạng, Java,
JDK, Maven hay quyền quản trị trên máy demo. Không mở game ngay trong ZIP viewer.

Khi cần sửa code ở buổi vấn đáp, mở `src/main/java`, lưu thay đổi, đóng game,
chạy `Build.cmd`, rồi mở lại `Run.cmd`. Build dùng compiler đi kèm và biên dịch
main source; Maven/JUnit tests có trong project nhưng cần môi trường Maven/JUnit
riêng nếu muốn chạy toàn bộ test. File `README.txt` trong ZIP cũng ghi
đầy đủ hướng dẫn. ZIP này giữ nguyên PNG, WAV, MP3 trong `src/main/resources`;
không thay độ phân giải hoặc chuyển mã asset.

Tạo lại gói sau khi sửa project bằng `./build-direct-portable.ps1` trên máy có
JDK 25, Maven và dependencies đã cache. Script chỉ tạo output mới, không ghi
đè gói đã phát hành. Không đưa `target`, `.git`, IDE cache hay `deliverables`
vào ZIP. Sau khi thêm report/slides, kiểm tra lại một file ZIP cuối cùng dưới
100.000.000 byte theo giới hạn LMS.

## Gói lossless cũ

Gói `DodgingCat-Windows-x64-portable.zip` chứa runtime Java 25, JavaFX native,
Gson và source. Bản **buildable** có thêm compiler Java để build offline;
bản **demo** gọn hơn, chỉ dành cho việc chạy game và xem source/tests.
Máy demo không cần cài Java, JDK, Maven hoặc 7-Zip. Chỉ dành cho Windows 64-bit.

## Chạy tại máy demo

1. Giải nén toàn bộ ZIP ra thư mục được phép ghi; không chạy trong cửa sổ xem ZIP.
2. Mở `Run.cmd` và đợi bước chuẩn bị lần đầu hoàn tất. Bước này giải nén runtime,
   khôi phục PNG/WAV gốc và kiểm tra dữ liệu; không tải gì từ mạng.
3. Những lần sau tiếp tục mở `Run.cmd`. F11 đổi fullscreen, F10 xem FPS.
4. Giữ nguyên cả thư mục. Game dùng chung assets trong `DodgingCat/source`,
   không được xóa thư mục source hoặc runtime.

Dữ liệu lưu tại `DodgingCat/data/profile.json` cạnh bản portable, không dùng
profile của bản phát triển trên máy cá nhân. F9 lần hai reset profile này.

Nên dùng đường dẫn ASCII, ví dụ `C:\Demo\DodgingCat-Windows-x64`. Nếu đường dẫn
có dấu tiếng Việt, script tạo bản cache vật lý tại `%LOCALAPPDATA%` hoặc
`%PUBLIC%`/`DodgingCatPortableRuntime` để tránh lỗi native-library của Java trên
Windows. Source sửa ở bản gốc sẽ được đồng bộ sang cache trước khi build;
profile vẫn ở bản gốc. Cần khoảng 400 MB ổ trống với đường dẫn ASCII, hoặc
700 MB khi cần cache. Không cần quyền quản trị; máy trường vẫn phải cho phép
chạy chương trình và PowerShell cục bộ.

## Sửa code khi interview

Với bản buildable, mở `Open-source.cmd`, sửa Java trong thư mục vừa mở, đóng
game rồi chạy `Build.cmd`. Script dùng `javac` và `jar` đóng gói sẵn, ghi lại
JAR và giữ bản trước đó trong `DodgingCat/build`. Sau đó chạy `Run.cmd`.
Bản demo không có `Build.cmd`; cần dùng bản buildable hoặc máy có JDK/Maven
để biên dịch source.

Build offline này biên dịch main source, không chạy Maven/JUnit. Source Maven
và tests vẫn được kèm để phát triển theo cách thông thường. Build lại toàn bộ
gói portable bằng `build-portable.ps1` cần JDK 25, Maven và dependencies trên máy
đóng gói; yêu cầu đó không áp dụng cho máy chạy demo.

## Giữ chất lượng và giới hạn LMS

Assets được nén lossless để vận chuyển rồi khôi phục trước khi chạy game:
PNG được đối chiếu kích thước và SHA-256 của mọi pixel ARGB, giữ các metadata
hiển thị; WAV được đối chiếu SHA-256 toàn file. MP3 được giữ nguyên.
PNG đã nén tốt hơn WebP (đặc biệt PNG palette) được giữ trực tiếp để tránh tăng
dung lượng; cache ảnh đóng gói chỉ tái sử dụng khi SHA-256 của source trùng khớp.
Không resize, không hạ độ phân giải render hoặc decode.

Tạo bản demo bằng `./build-portable.ps1 -DemoOnly`. Công cụ và cache build nằm
trong `target/`, không được đưa vào ZIP. Bản demo giữ source/tests, bỏ compiler
đi kèm để chừa dung lượng cho report và slides.

Build từ chối xuất bản ZIP từ 100.000.000 byte trở lên. Số byte và SHA-256 của
artifact nằm trong `build-report.json` bên ngoài ZIP. Đây là gói game/source;
báo cáo và slides chưa nằm trong đó. Khi thêm tài liệu vào một ZIP nộp LMS,
phải đo lại tổng kích thước; không thể dựa vào kích thước gói game riêng.

`Troubleshoot.cmd` kiểm tra/khôi phục assets và ghi lỗi vào `game-debug.log`.
Log chuẩn bị ở `DodgingCat/prepare-assets.log`; nếu dùng cache, xem log trong
cache được ghi tại `portable-root.txt`.
