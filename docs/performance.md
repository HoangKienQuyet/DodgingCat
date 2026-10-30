# Render native và kiểm chứng hiện tại

[Mục lục](README.md) · [README English](../README.md) · [README tiếng Việt](../README_vi.md)

## Cấu hình đang chạy

[Launcher](../src/main/java/com/dodgingcat/Launcher.java) chọn Java2D/AWT mặc định. BufferStrategy hai buffer thuộc `java.desktop` vẽ ở kích thước pixel vật lý có xét DPI. Chiều cao logic 640 không phải độ phân giải ảnh render: scale lên pixel đích trực tiếp, không có giới hạn 720p mặc định. JavaFX vẫn cần cho ảnh/media và backend so sánh. Không dùng OpenGL engine bên thứ ba.

[FramePacer](../src/main/java/com/dodgingcat/app/FramePacer.java) nhắm 60 frame/s bằng deadline trên luồng game riêng, không chạy bù hàng loạt frame. Đây là mục tiêu điều phối, không bảo đảm mọi cấu hình máy đạt 60 FPS. Bộ JavaFX tùy chọn có diagnostic `dodgingcat.renderHeight`; giá trị mặc định là 0/native, không dùng cấu hình 720p của thử nghiệm cũ.

## Cách giảm công việc lặp

- DrawContext cache ảnh đã scale, gradient, panel và viền theo kích thước pixel đích; lấy mẫu bicubic từ ảnh gốc. Cache có giới hạn/weak keys tùy loại để tránh giữ mọi kích thước resize.
- MapBackdropCache chuẩn bị nền và các mức dim cho menu/shop/gacha trên worker. Không vẽ catalog bên dưới modal gacha.
- AssetManager decode ảnh gốc, chuẩn bị alpha bounds, các frame preview và các kích thước buff/tiền trên worker; chuẩn bị lại khi scale thay đổi. Không cắt chất lượng source xuống 720p hoặc decode pickup ở 192px như thử nghiệm cũ.
- Boot làm nóng các đường vẽ trong thời gian title/logo. Không có điều kiện phải chờ đủ 120 warmup frame trước khi sang menu; worker có thể tiếp tục nếu chưa hoàn tất.
- ArtText ghép/cache chuỗi từ atlas glyph; game không raster hóa glyph hệ thống mỗi frame.
- SFX decode/mở Clip trên audio worker; scheduler riêng xử lý âm trễ. BGM dùng JavaFX MediaPlayer. Event AWT được chuyển vào hàng đợi để model không bị sửa đồng thời.

## Kiểm thử ngày 28/10/2026

Trong lần rà tài liệu này, `mvn -q test` hoàn tất exit code 0: **50 test được phát hiện, 44 đạt, 6 probe tùy chọn skipped, 0 failure/error**. Có UI snapshot và kiểm tra logic; không chạy lại benchmark FPS trong lần chỉ sửa tài liệu này. Lệnh/probe và vị trí output ở [running-and-testing.md](running-and-testing.md).

Báo cáo [native ngày 28/10](history/performance-native-2026-10-28.md) ghi phép đo trước đó trên máy Windows: năm cảnh Menu, Shop, Gameplay 2P, Spin, Legendary reveal/result có FPS trung bình xấp xỉ 60 ở windowed native 1200×800 và fullscreen native 1920×1080. Shop vẫn có frame 25,22ms. Ứng dụng thật được ghi khoảng 60,00 FPS trung bình sau warmup; boot từ title đầu đến menu khoảng 4,005s.

Đây là số đo đã ghi, không phải benchmark vừa chạy lại. Probe scene chỉ lấy mẫu 3s/cảnh sau 1s warmup; không mô tả mọi trận dài, lần đổi map hoặc tải GPU từ chương trình khác. FPS ứng dụng/BufferStrategy không phải phép đo phần cứng của từng lần màn hình quét. Không dùng số FPS JavaFX snapshot/readback hoặc thử 720p để tuyên bố hiệu năng native hiện tại.

## Kiểm tra khi có thay đổi

Sau thay đổi gameplay/assets/rendering, chạy `mvn test`, rồi probe scene native và ứng dụng thật riêng nếu cần đo hiệu năng. Ghi OS, JDK, GPU, DPI, kích thước pixel, warmup, thời gian lấy mẫu, FPS trung bình và phân bố frame time; không chỉ chụp bộ đếm FPS ở một thời điểm. Dùng profile probe trong `target/` để không tác động save cá nhân. Các báo cáo cũ được giữ trong [history](history/README.md).
