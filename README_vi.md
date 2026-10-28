# Dodging Cat

[English](README.md) · [Mục lục tài liệu](docs/README.md)

Dodging Cat là game desktop sinh tồn trên các bệ, viết bằng Java, có chế độ một người và hai người cùng bàn phím. Người chơi né vật rơi, laser, đạn đại bác và quả tạ chuyển động; nhặt buff và cá; dùng gacha mở skin và mua thêm map.

Bộ render mặc định là **Java2D/AWT ở độ phân giải pixel native**, điều phối theo mục tiêu 60 FPS. JavaFX vẫn phục vụ ảnh/âm thanh và bộ render JavaFX tùy chọn, nên game vẫn có dependency ngoài JDK. Phiên bản trong [pom.xml](pom.xml): **JDK 25**, JavaFX 25, Gson 2.13.2, JLayer 1.0.1 và JUnit 5.10.2. Phiên bản artifact Maven là 1.0.0.

## Chạy và kiểm thử

Chạy trong thư mục có `pom.xml`, với JDK 25 và Maven:

```powershell
mvn compile javafx:run
mvn test
```

Trên Windows, có thể mở `run.cmd` hoặc `test.cmd`. Hai file gọi `dev.ps1` để tìm JDK đã cài và dùng Maven có sẵn, hoặc tải Maven 3.9.11 nếu chưa có. Lần build Maven đầu cần mạng để tải dependency. Script không cài JDK; project yêu cầu release 25 dù thông báo cũ của helper có nhắc 23+.

Máy demo chưa cài Java/Maven cần [gói Windows x64 portable](docs/portable-windows.md) được build riêng. Giải nén toàn bộ, mở `Run.cmd`; sửa Java xong dùng `Build.cmd`. Clone GitHub chỉ có source và công cụ đóng gói, không có runtime portable.

## Cách chơi

Sau khi bắt đầu render, màn title hiện 2 giây rồi logo TADQ hiện 2 giây. Menu dùng map đã bắt đầu chơi gần nhất và không có mèo mascot. Bấm **1P** hoặc **2P**, chọn map/skin ở loadout rồi bấm Start. Game đếm `3 → 2 → 1 → GO` trong tổng cộng 3 giây trước khi simulation chạy.

| Thao tác | Người chơi 1 | Người chơi 2 |
| --- | --- | --- |
| Di chuyển | A / D | Trái / Phải |
| Nhảy | W | Lên |
| Rơi xuyên bệ nổi | S | Xuống |

Dùng chuột bấm menu và chọn map. Ở tab skin của 2P, P1 dùng WASD + Space, P2 dùng phím mũi tên + Enter; con lăn cuộn lưới, click skin đổi cho P1. `Tab` đổi tab loadout. `Esc` hoặc `P` pause khi chơi; `R` chơi lại. `F11` đổi toàn màn hình; `F10` bật thông tin FPS.

**F9 là phím dev:** lần đầu bật chế độ luôn bù ví lên 1.000.000 cá; lần tiếp theo thay profile bằng mặc định người chơi mới, xóa cả tiền, mở khóa và bảng xếp hạng.

Điểm tăng 10 mỗi giây simulation, hoặc 20 khi có người sống đang giữ Double Score. Mỗi buff nhặt được cộng 50 điểm. Hai người dùng chung điểm và số cá của lượt; 2P kết thúc khi cả hai mèo chết. Cá và kết quả chỉ được lưu một lần ở Game Over. Restart, thoát game hoặc về menu trước đó sẽ bỏ phần thưởng của lượt chưa hoàn tất. Save thường ở `%USERPROFILE%/.dodgingcat/dodging_cat_profile.json`; save portable ở `data/profile.json` cạnh bản đang chạy.

## Tra cứu tài liệu

| Nội dung | Tài liệu |
| --- | --- |
| Môi trường, điều khiển, save/reset và lệnh kiểm thử | [Chạy, chơi và kiểm thử](docs/running-and-testing.md) |
| State, vòng lặp render, collision và các luật fairness | [Kiến trúc và bản đồ mã nguồn](docs/architecture.md) |
| Tỉ lệ/cooldown buff, tiền, hazard, platform và điểm | [Thông số gameplay](docs/gameplay-thong-so.md) |
| Đủ 15 skin, năm map, giá, xác suất và hoạt ảnh quay | [Shop, gacha và loadout](docs/shop-and-loadout.md) |
| Render native, giới hạn phép đo và kết quả kiểm chứng | [Hiệu năng và kiểm chứng](docs/performance.md) |
| Demo offline Windows và đóng gói nộp bài | [Gói portable](docs/portable-windows.md) |
| Sprite grid, chữ raster, vùng alpha và branding | [Asset hình ảnh](src/main/resources/assets/images/README.md) |
| Tên WAV/MP3, cách phát và quy tắc chuyển định dạng | [Asset âm thanh](src/main/resources/assets/audio/README.md) |
| Công cụ tạo ảnh, script đóng gói và template | [Công cụ](tools/README.md) |
| Các phép đo/thử nghiệm đóng gói trước đây | [Hồ sơ lịch sử](docs/history/README.md) |

## Cấu trúc repository

```text
src/main/java/com/dodgingcat/   ứng dụng, simulation, UI, generation, audio, save
src/main/resources/           ảnh, âm thanh và CSS JavaFX cũ
src/test/java/com/dodgingcat/  test logic, UI snapshot và probe desktop tùy chọn
docs/                        hướng dẫn chi tiết và hồ sơ lịch sử
tools/                       công cụ tạo ảnh và template script portable
pom.xml                      build Maven và phiên bản dependency
```

`target/`, `.git/`, cache IDE và `deliverables/` không thuộc source nộp bài. ZIP portable được sinh riêng; report và slide cuối cùng chưa có trong nhánh này. Hướng dẫn đóng gói trình bày giới hạn một file 100 MB do nhóm cung cấp.

## Nhóm TADQ

| Mã sinh viên | Họ tên |
| --- | --- |
| 2301040209 | Hoàng Kiên Quyết |
| 2301040013 | Trần Quang Anh |
| 2301040042 | Nguyễn Công Đoan |
| 2301040171 | Trần Gia Thành |

Cả bốn thành viên phát triển các bản game riêng. Nhóm chọn bản này, do Trần Quang Anh phát triển, làm nền cho bài nộp. Đóng góp tích hợp, kiểm thử, tài liệu và đóng gói về sau cần được ghi theo phần việc từng người thực sự hoàn tất; README không gán các trách nhiệm lập trình chưa được xác nhận.

Tài liệu được đối chiếu với source ngày **28/10/2026**. Số đo hiệu năng chỉ phản ánh máy và kịch bản đo cụ thể, không bảo đảm mọi máy đều đạt 60 FPS. Khi sửa tài liệu, đối chiếu phần code đang được gọi thay vì dựa vào comment cũ hoặc báo cáo thử nghiệm.
