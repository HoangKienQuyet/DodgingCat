# Render native và cân bằng gameplay — 28/10/2026

## Gameplay

- Mọi buff cộng đúng 50 điểm khi nhặt, không nhân đôi bởi x2.
- Cooldown x2 25s, Bomb/Invincibility 35s, Shield/Heart 45s. Đếm sau khi buff biến mất hoặc hiệu lực kết thúc, gồm trường hợp khiên vỡ và bảo vệ hồi sinh Heart.
- Tiền xuất hiện mỗi 5s, tồn tại 5s, tối đa 5 vật phẩm; 85% +1, 15% +5.
- [Thông số buff, tiền, hazard và gacha](gameplay-thong-so.md).

## Bộ render hiện hành

Launcher mặc định dùng Java2D/AWT và BufferStrategy hai bộ đệm thuộc module `java.desktop` của JDK. Không thêm engine/OpenGL bên thứ ba. JavaFX vẫn là dependency cho image/media hiện có và backend đối chiếu; đây không phải bản loại bỏ mọi dependency ngoài JDK.

Khung hình dùng đúng số pixel của màn hình, có tính DPI Windows. Ví dụ Canvas AWT 1536×864 đơn vị giao diện ở DPI 125% tương ứng bộ đệm vật lý 1920×1080. Không có framebuffer 720p phóng lên 1080p. Ảnh gốc, độ phân giải decode, vùng cắt sprite và atlas chữ được giữ nguyên. Java2D lấy mẫu bicubic trực tiếp từ nguồn sang đúng kích thước pixel đích; không qua ảnh trung gian chất lượng thấp.

API dùng thuộc [Java SE — BufferStrategy](https://docs.oracle.com/en/java/javase/25/docs/api/java.desktop/java/awt/image/BufferStrategy.html).

Chạy `run.cmd`, `./dev.ps1 run` hoặc `mvn compile javafx:run`. F11 đổi fullscreen/windowed, F10 bật FPS. Backend FX cũ còn ở `DodgingCatApp`, có thể chọn khi chạy JVM với `-Ddodgingcat.renderer=javafx`; mặc định là `java2d`.

## Tối ưu

- Chia sẻ toàn bộ bố cục/gameplay/asset qua `DrawContext`; Java2D thực hiện các lệnh bằng Graphics2D.
- Cache ảnh, gradient, nền tối và viền bo góc ở độ phân giải native. Nền menu/shop/gacha chuẩn bị trên worker; không alpha-blend cả màn hình và raster hóa lại khung hộp mỗi frame.
- Chuẩn bị đủ sáu frame preview của mọi skin và kích thước buff/tiền trong lúc boot; việc mở ảnh/phân tích bounds không chặn lần spawn đầu. Khi đổi kích thước, chuẩn bị lại theo scale mới; yêu cầu cũ được bỏ qua.
- Giới hạn kích thước cache, dùng weak key cho ảnh chữ thay đổi liên tục; không giữ vô hạn mọi framebuffer cũ khi resize.
- Khởi động hiển thị title 2s rồi logo 2s theo thời gian thực. Làm nóng đường vẽ trên backbuffer trong khoảng 4s này, sau đó phủ màn khởi động trước khi trình bày; không chờ thêm đủ 120 frame hoặc chờ worker xong mới vào menu. Không quay thật, tiêu tiền hay mở khóa skin. Tải/cache ảnh tiếp tục trên worker nếu chưa hoàn tất.
- Deadline 60 Hz chạy trên luồng render riêng; input AWT được xếp hàng sang luồng này để model không bị sửa đồng thời. Không chạy bù một loạt frame cũ khi quá hạn.
- BGM/MP3 vẫn được gọi trên media thread; mở và phát SFX trên audio worker. Timer âm thanh dùng scheduler riêng.
- Menu chính không vẽ mèo preview, dùng map đã bắt đầu chơi gần nhất (lưu riêng với map đang chọn ở loadout). Loadout không vẽ mascot vào giữa các thẻ. Các nút vẫn dùng asset và chữ raster; font hệ thống chỉ cung cấp số đo cỡ chữ, không vẽ glyph.

## Kiểm chứng

```powershell
mvn -o -q test
mvn -o -q '-Dtest=Java2DSceneProbeTest' '-Ddodgingcat.awtSceneProbe=true' '-Ddodgingcat.awtSceneProbe.capped=true' test
mvn -o -q '-Dtest=Java2DAppTest' '-Ddodgingcat.awtAppProbe=true' test
```

Probe cảnh chạy renderer/game world thực, 1s làm nóng và 3s lấy mẫu mỗi cảnh; gameplay 2P có 5 buff, 5 tiền, 5 vật rơi và generator. Đo trên cửa sổ thật/BufferStrategy, không dùng snapshot trong phép đo. Frame interval gồm cả update, vẽ, trình bày và chờ deadline. Đây là tốc độ hoàn tất frame của ứng dụng, không phải phép đo phần cứng về thời điểm màn hình quét pixel.

Lượt cuối ở môi trường Windows ngoài sandbox:

| Cảnh | Windowed native 1200×800 | Fullscreen native 1920×1080 | Fullscreen p95 / max |
| --- | ---: | ---: | ---: |
| Menu | 60,00 FPS | 60,02 FPS | 17,86 / 18,51ms |
| Shop | 60,02 FPS | 60,02 FPS | 17,96 / 25,22ms |
| Gameplay 2P | 59,97 FPS | 60,00 FPS | 17,80 / 18,04ms |
| Spin | 59,99 FPS | 60,00 FPS | 17,78 / 18,22ms |
| Legendary reveal + result | 60,00 FPS | 59,98 FPS | 17,83 / 18,05ms |

Khởi động hiện tại đo được **4,005s từ frame title đầu đến menu** (title 2s, logo 2s); không giữ logo để chờ đủ warmup.

Ứng dụng thật sau cold start, có F11 qua lại và WAV spin phát thật: **trung bình 60,00 FPS**, thấp nhất **59,95 FPS** trong 17 mẫu cửa sổ đo một giây, lấy cách nhau 0,5s sau 2s làm nóng cảnh. Mẫu FPS này không phải FPS thấp nhất của từng frame. Lượt trước khi làm nóng đường vẽ trong boot còn tụt xuống khoảng 43 FPS ở đầu spin, dù probe cảnh đã đạt 60; vì vậy đã kiểm tra riêng ứng dụng thật thay vì chỉ dùng probe. Shop vẫn ghi nhận một frame 25,22ms trong probe; không tuyên bố tuyệt đối hết mọi hitch.

Kết quả mới được lưu trong `target/java2d-scenes-60fps.txt`. Lượt không giới hạn dùng `target/java2d-scenes.txt`. Kiểm tra ứng dụng thật (input, F11, MP3 và gacha với WAV thật) dùng profile riêng trong `target`, xuất báo cáo `target/java2d-app-probe.txt`, không sửa save của người chơi.

UI snapshot so sánh ảnh Java2D và JavaFX cùng bố cục ở tiếng Anh/Việt, gồm loadout 2P, countdown, pause, leaderboard, gacha và hiệu ứng legendary. Cho phép khác biệt khử răng cưa/làm tròn pixel nhỏ, nhưng kiểm tra sai màu/thiếu ảnh/clip/transform. Ảnh Java2D ở `target/ui-preview/java2d-*.png`; test cũng xác nhận buff, tiền và logo decode đúng kích thước ảnh gốc.

Kiểm tra cuối: **38 test đạt, 0 failure/error**; 6 bài probe tùy chọn được bỏ qua trong suite thường. Probe cảnh và ứng dụng thật đã chạy riêng thành công. `mvn -o -q compile javafx:run` cũng đã mở cửa sổ native qua Launcher mặc định và thoát bằng WM_CLOSE với exit code 0. Kiểm tra này chỉ mở/đóng cửa sổ, không reset hay thao tác mua trên profile người chơi.

Các phép đo FX native trước khi chuyển backend chỉ đạt khoảng 43–46 FPS fullscreen trên máy này. Bản 720p trong `performance-2026-10-28.md` là lịch sử thử nghiệm, không còn là cấu hình chạy hiện hành. Kết quả đo ngắn không bảo đảm mọi máy hoặc mọi trận dài đều không có hitch.
