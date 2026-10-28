# Kiểm chứng UI, gacha và hiệu năng — 28/10/2026

> Báo cáo lịch sử của cấu hình thử 720p. Cấu hình đó đã được bỏ theo yêu cầu tiếp theo: mặc định hiện tại render native, không upscale từ 720p. Xem [kết quả native và thay đổi buff/tiền](performance-native-2026-10-28.md).

## Kết quả và giới hạn

Đã hoàn tất sửa thẻ skin/khóa, reel dừng đúng giải thưởng, trình tự âm thanh, hiệu ứng theo rarity, số `01`, hover nút shop và kích thước title/1P/2P. Mục tiêu **fullscreen luôn giữ 60 FPS chưa đạt** trên máy đo; không nên coi các tối ưu dưới đây là đã giải quyết hoàn toàn vấn đề này.

Bài đo dùng Stage hiển thị thực, không chụp Canvas trong lúc đo. Mỗi cảnh có 1s làm nóng rồi 3s lấy mẫu. Đếm cả AnimationTimer và số frame được JavaFX trình bày bằng PerformanceTracker; hai giá trị gần nhau. Gameplay dùng 2 người chơi, 5 buff, 5 vật phẩm tiền và 5 vật rơi ban đầu, cùng generator hoạt động; người chơi được giữ bất tử để cảnh không chuyển sang Game Over. Đây là cảnh stress kiểm soát, không phải phép đo một ván chơi dài.

Kết quả cuối với cấu hình mặc định mới, Direct3D / NVIDIA GeForce MX550:

| Cảnh | Windowed: FPS thực được trình bày | Fullscreen: FPS thực được trình bày | Fullscreen p95 thời gian giữa frame |
| --- | ---: | ---: | ---: |
| Menu | 62,42 | 50,80 | 32,36ms |
| Shop | 62,51 | 40,56 | 32,56ms |
| Gameplay 2P | 62,50 | 52,75 | 32,28ms |
| Spin | 62,49 | 52,48 | 32,40ms |

Windowed 960×640; fullscreen vật lý 1920×1080, bộ đệm Canvas 1280×720 được phóng đều để phủ viewport. Mức khoảng 62,5 ở windowed tương ứng nhịp JavaFX khoảng 16ms; đó không phải bảo đảm tốc độ refresh của màn hình. Khoảng frame lớn nhất trong phép đo cuối: windowed 17,21ms, fullscreen 33,45ms. Mẫu ngắn này không loại trừ spike trong một ván dài hoặc khi đổi map/skin chưa được GPU sử dụng trước đó.

Trước khi thêm cache backdrop, cùng bộ đệm/cấu hình/scene stress: fullscreen menu 48,20, shop 37,43, gameplay 50,52, spin 49,87 FPS thực được trình bày; windowed gameplay có spike 94,87ms. Cache đã giảm việc resample/tô tối nền lặp lại trên GPU và cải thiện phép đo, nhưng vẫn còn chênh lệch với mục tiêu 60 FPS.

Các phép đo trước phiên tối ưu, ở DPI mặc định và Canvas fullscreen native, ghi shop khoảng 14,62 và gameplay 30,51 FPS. Chúng dùng bài đo cũ nên chỉ là tham khảo; không so trực tiếp như một benchmark cùng điều kiện. Chạy bài đo bên ngoài sandbox cũng tái hiện việc fullscreen chậm hơn windowed. Chỉ vẽ một ảnh nền ở 1080p ghi khoảng 55 FPS trên desktop này. JFR cho thấy phần lớn mẫu của luồng render nằm trong `D3DSwapChain.nPresent`; đây là dấu hiệu điểm nghẽn trình bày/render, không chứng minh rằng driver hay phần cứng có lỗi.

## Thay đổi đang áp dụng

- Bỏ path clip phủ toàn màn hình ở app/nền; cắt vùng nguồn ảnh để giữ đúng tỉ lệ và điểm neo mặt sàn.
- Chuẩn bị backdrop đúng kích thước bộ đệm và các bản tối cho menu/shop bằng worker, cache giới hạn 4 kích thước/map. Không làm công việc bitmap này trên luồng gameplay.
- Preview lấy sáu idle frame đã cắt theo vùng alpha, tối đa 256px, căn giữa vùng preview; không upload cả atlas nhân vật cho từng thẻ shop. Quét alpha dùng bộ đệm pixel thay vì gọi chuyển màu cho từng pixel.
- Không vẽ toàn bộ catalog phía dưới modal spin/reveal/kết quả. Không vẽ mèo mascot của menu ở giữa các hàng thẻ shop.
- Hover dùng PNG có cùng alpha với nút gốc, không dùng shader blur hay dao động opacity.
- Trên Windows, mặc định `glass.win.uiScale=1.0` để tránh Canvas tự nhân bộ đệm lên 2× ở desktop DPI 125%. Game vẫn tự co giãn bố cục theo viewport. `prism.vsync=false`; AnimationTimer và compositor Windows tiếp tục điều phối trình bày. Tùy chọn JVM người chạy đặt rõ vẫn có quyền ưu tiên.
- Bộ đệm giới hạn chiều cao 720; tọa độ chuột vẫn tính từ kích thước viewport, giữ nguyên vùng click. Đổi lại fullscreen được upscale từ 720p, có thể mềm hơn native. Đặt `-Ddodgingcat.renderHeight=0` để chạy bộ đệm native khi cần so chất lượng.
- Các tối ưu trước vẫn giữ: giải mã pickup ở kích thước gần sử dụng, metadata sprite tính trên worker, pool SFX và mở/đóng clip ngoài luồng game, bỏ bộ lọc exact-16,67ms làm mất pulse.

Các thử nghiệm software pipeline, borderless, tăng VRAM budget, force uploading painter và thay nhịp pulse không được đưa vào cấu hình mặc định vì không giải quyết được giới hạn fullscreen trên máy đo.

## Chạy lại

```powershell
mvn -q test
mvn -q '-Dtest=SceneFrameProbeTest' '-Ddodgingcat.sceneProbe=true' test
mvn -q '-Dtest=AudioPlaybackTest' '-Ddodgingcat.audioProbe=true' test
```

Bài đo FPS ghi `target/scene-frame-probe-720.txt`. Chạy riêng từng probe; không chạy thêm game hay UI snapshot cùng lúc đo. Probe âm thanh dùng native Java Sound với âm lượng rất nhỏ, kiểm tra file spin đầy đủ trước tín hiệu kết thúc. File WAV dài danh nghĩa 11,312s; reel dùng tiến độ và kết thúc thực của clip, không dùng một timer cố định 11,312s.

UI snapshot kiểm tra cả tiếng Việt/Anh, tỉ lệ thường/rộng, hình 1920×1080 qua bộ đệm 720p, cuộn chuột 2P, dừng reel và các bước reveal của 5 giải thưởng, danh sách leaderboard có dữ liệu và tự đóng/click đóng box. Ảnh nằm trong `target/ui-preview`. Kiểm thử logic gacha bao phủ tất cả 15 skin qua 20 seed/skin, ranh giới trọng số rarity và trường hợp đã đủ bộ sưu tập.

Thông số gameplay: [buff, tiền, hazard và xác suất gacha](gameplay-thong-so.md).
