# Kiểm tra Dodging Cat v1.3 — 26/10/2026

Môi trường: Windows x64, Oracle JDK 23.0.2, Maven 3.9.11, JavaFX 25. pom.xml mặc định release 25; lần kiểm tra dùng -Dmaven.compiler.release=23 và dependency cache offline.

## Kết quả thực tế

| Kiểm tra | Kết quả |
| --- | --- |
| Maven package | BUILD SUCCESS |
| GameWorldTest | 19 đạt |
| MapWorldTest | 10 đạt |
| PickupNoticeTest | 6 đạt |
| VersusWorldTest | 16 đạt |
| ProfileTest | 5 đạt |
| SaveStoreTest | 11 đạt |
| Tổng JUnit | **67 đạt, 0 lỗi, 0 bỏ qua** |
| Scene trên JDK phát triển | UI_SMOKE_OK v1.3 |
| Bản portable với Java đi kèm | UI_SMOKE_OK v1.3 |
| Preview bẫy và hoạt ảnh | MAP_ANIMATION_PREVIEW_OK |
| Preview thông báo nhặt đồ | PICKUP_NOTICE_PREVIEW_OK |

Mười ca map mới kiểm tra đúng nhóm bẫy được sinh ở mỗi map trên nhiều seed, không tự sinh laser, chuyển động và sát thương của độc/kéo/lửa, lửa hết sau 1,65 giây, bệ hợp lệ, khoảng cách 120px với cả hai mèo, thời gian phản ứng với bẫy ngang và vị trí sinh cá. Các ca nhảy chạy mô phỏng thật từ sàn tới hai bệ thấp, bệ giữa và hai bệ cao của Kitchen. Thông báo engine cũng được kiểm tra bằng tiếng Anh.

Các ca hồi quy kiểm tra điều khiển và hiệu ứng riêng từng mèo, nhặt vật phẩm một lần, khiên một đòn, miễn sát thương/nhân đôi cá 5 giây, Versus 60 giây, pause, thắng/hòa, kinh tế Solo, bốn buff cửa hàng và hồ sơ schema 1/2.

Scene smoke dùng bàn phím và nút thật của JavaFX: Garden Solo, Kitchen Solo, Kitchen Versus, Garden Versus; giữ map khi đổi mode/chơi lại; cửa hàng và lưu mở khóa; banner/nhãn nhặt cá; kết quả theo số cá. Trận Kitchen Versus thu được **5 cá** nhưng không đổi ví, kỷ lục hoặc lịch sử Solo. Smoke dùng file lưu riêng trong .tools/, không dùng tiến trình thật.

Ảnh menu, cửa hàng buff, Kitchen và kết quả đã được kiểm tra để xác nhận chữ tiếng Anh và bố cục. Sau bộ 67 ca, bản cuối được biên dịch lại để đổi nhãn nhỏ trên token ×2 thành FISH, rồi chạy lại smoke bằng runtime portable.

Fixture đồ họa tạo garden-hazards.png, kitchen-hazards.png và cat-animation.png từ renderer thật. Đã kiểm tra trực quan hai map và các tư thế idle/chạy/nhảy/rơi/tiếp đất. Fixture so sánh pixel giữa hai pha chạy, giữa nhảy và rơi, giữa idle và tiếp đất; đồng thời xác nhận ảnh Canvas giữ nguyên từng pixel khi pause dù đồng hồ hiển thị thay đổi. Các ảnh thông báo cá/buff cũng được tạo lại bằng tiếng Anh.

## Tái tạo

```powershell
mvn "-Dmaven.compiler.release=23" package
mvn "-Dmaven.compiler.release=23" "-Djavafx.args=--smoke-test --seed=4242 --save=.tools/check-v13-profile.json --screenshot-dir=.tools/check-v13-screenshots" javafx:run
```

Dùng JDK 25 thì bỏ -Dmaven.compiler.release=23. Phải dùng file --save riêng: smoke có thể cấp cá thử nghiệm và mua đồ. Báo cáo JUnit nằm trong target/surefire-reports/.

## Giới hạn

Chưa thử trên máy sạch, hệ điều hành khác hoặc JDK 25; chưa benchmark FPS. Luật tránh bẫy là ràng buộc cục bộ được kiểm tra trên mẫu hữu hạn, không chứng minh mọi chuỗi bẫy đều né được. Ảnh tĩnh thể hiện từng tư thế; cần kiểm tra nhiều khung hình để đánh giá hoạt ảnh.
