# Dodging Cat (Phiên Bản Antigravity v1.2)

> **Bản tài liệu làm việc để cả nhóm rà soát.** Các tài liệu gameplay và hiệu năng được dẫn bên dưới phản ánh mã nguồn hiện tại; những phần mô tả khác cần được đối chiếu trước khi nộp.

Thông số gameplay hiện hành (buff, tiền, hazard và gacha): [bảng đối chiếu ngày 28/10/2026](docs/gameplay-thong-so.md). Các thông số trong bảng này được đối chiếu với mã nguồn và thay thế số liệu cũ ở các phần mô tả bên dưới.

Render mặc định bằng Java2D/AWT thuộc `java.desktop` của JDK, ở độ phân giải native. Giữ ảnh và độ phân giải decode gốc, bỏ giới hạn 720p; JavaFX tiếp tục phục vụ image/media và backend đối chiếu. [Kiểm chứng chất lượng và hiệu năng hiện tại](docs/performance-native-2026-10-28.md). Chạy `run.cmd` hoặc `./dev.ps1 run`; F11 đổi fullscreen, F10 xem FPS.

Bản Windows portable có source `src` và `pom.xml` ngay trong ZIP, kèm Java compiler để sửa/build offline. Xem [hướng dẫn portable](docs/portable-windows.md), giải nén rồi mở `Run.cmd`; sau khi sửa source thì mở `Build.cmd`.

> **Tài Liệu Dự Án Chi Tiết & Báo Cáo Triển Khai Kỹ Thuật (Tiếng Việt)**

Dodging Cat là tựa game **2D Action Survival Platformer** được phát triển bằng Java (JDK 25), JavaFX 25, Maven và JUnit 5. Trò chơi kết hợp cơ chế né tránh sinh tồn hardcore, chế độ chơi đôi **Local Co-op**, màn hình chọn tải đồ (loadout) trước trận, bệ ngẫu nhiên hóa động, 3 hiểm họa chung + hiểm họa riêng từng map, kinh tế **tiền tệ Cá (Fishes)**, cửa hàng skin/map, hỗ trợ toàn diện chuột & bàn phím, cùng đóng góp kỹ thuật cốt lõi: **Sinh Nội Dung Ngẫu Nhiên Có Kiểm Soát Độ Công Bằng (Fairness-Aware Procedural Generation)**.

---

## 1. Thông Tin Dự Án

| Hạng mục | Chi tiết |
| :--- | :--- |
| **Tên Dự Án** | Dodging Cat (Antigravity Version) |
| **Nhóm** | TADQ |
| **Môn Học** | Phát triển Phần mềm bằng Java (Năm học 2026–2027) |
| **Giảng Viên Hướng Dẫn** | Ngô Văn Quyền |
| **Quy Mô** | 4 thành viên |
| **Thể Loại** | 2D Action Survival Platformer |
| **Chế Độ Chơi** | Chơi Đơn (Single-player) & Chơi Đôi (Local Co-op WASD + Phím Mũi Tên) |
| **Đóng Góp Kỹ Thuật Chính** | **Fairness-Aware Procedural Generation** |
| **Đường Dẫn Dự Án** | Thư mục gốc sau khi clone hoặc giải nén dự án |
| **Trạng Thái** | 🟡 **Đang phát triển; còn hoàn thiện asset và kiểm thử trải nghiệm** |

### Thành Viên Nhóm Và Ghi Nhận Đóng Góp

Cả bốn thành viên đã phát triển các bản game riêng. Nhóm chọn bản này, do Trần Quang Anh phát triển, làm nền cho bài nộp cuối kỳ. Các phần tích hợp, kiểm thử, tài liệu và đóng gói về sau cần được ghi theo công việc thực tế của từng người; bảng đóng góp cuối cùng vẫn đang được cập nhật.

| STT | Mã Sinh Viên | Họ và Tên | Ghi nhận hiện tại |
| :-: | :--- | :--- | :--- |
| 1 | `2301040209` | **Hoàng Kiên Quyết** | Phát triển bản game riêng; phần đóng góp vào bản này sẽ được cập nhật |
| 2 | `2301040013` | **Trần Quang Anh** | Phát triển bản game được chọn làm nền |
| 3 | `2301040171` | **Trần Gia Thành** | Phát triển bản game riêng; phần đóng góp vào bản này sẽ được cập nhật |
| 4 | `2301040042` | **Nguyễn Công Đoan** | Phát triển bản game riêng; phần đóng góp vào bản này sẽ được cập nhật |

---

## 2. Các Nâng Cấp Chi Tiết Theo Phản Hồi

**Trạng thái hình ảnh/âm thanh:** Game dùng background PNG, sprite sheet skin, chữ UI bằng atlas ảnh, nhạc từng map và SFX gameplay/UI. Cache đúng kích thước pixel tránh lặp lại scaling và vẽ gradient mỗi frame; không giảm chất lượng asset.

### 2.1 Màn Hình Chính Chuẩn Brotato & Hỗ Trợ Chuột Toàn Diện
- Mèo trung tâm đứng trên sân khấu ánh đèn, xung quanh là bóng đen mờ ảo.
- Hỗ trợ chuột đầy đủ: di chuột sáng nút, nhấp chuột chọn màn, đổi tab, mua đồ. Phím bàn phím vẫn hoạt động song song.
- Nút bấm tinh gọn: `Single Player`, `Local Co-op`, `Shop`, `Leaderboard`, `Settings`, `Quit`.
- Bố cục menu: `Single Player`, `Local Co-op`, `Shop` bên trái; `Leaderboard`, `Settings`, `Quit` bên phải. Nếu có ván đang tạm dừng, menu hiện thêm nút `Resume`.
- Góc trên bên phải chỉ hiển thị số Cá; đổi ngôn ngữ và chỉnh âm thanh nằm trong `Settings`.
- Hỗ trợ **Toàn Màn Hình (F11)** hoặc qua mục Cài đặt.

### 2.2 Màn Hình Chọn Bản Đồ & Trang Phục (Pre-Game Loadout)
- Bấm Play sẽ chuyển đến màn hình chọn Trang phục và Bản đồ đã sở hữu trước khi vào trận.
- Thẻ bản đồ chỉ hiển thị tên và trạng thái đã chọn; không chia độ hiếm. Giá map giữ nguyên. Skin vẫn có thể giữ phân hạng độ hiếm.

### 2.3 Cửa Hàng Cá (Shop) Tách Biệt 2 Tab
- Chia làm 2 trang riêng: **Trang Phục Mèo (Skins)** và **Bản Đồ (Maps)**.
- Shop chỉ phục vụ Mua (Buy), việc trang bị chuyển về màn hình Pre-game.
- Giá cả cân đối: cần chơi trung bình ~20 ván mới mua được skin hiếm, map đắt gấp đôi.

### 2.4 Bệ Ngẫu Nhiên Hóa Động (Procedural Dynamic Platforms)
- Mỗi ván chơi sinh ngẫu nhiên 4–7 bệ nổi trên 3–4 tầng, gồm vị trí và độ rộng khác nhau; các bệ được kiểm tra khả năng nối bằng cú nhảy và trải trên vùng chơi. Sau 60 giây, bệ vỡ xuất hiện lại ở vị trí mới có thể tiếp cận; đôi khi game thêm nhánh bệ mới, tối đa 10 bệ nổi.
- Sinh ngẫu nhiên 4–7 bệ theo 3–4 tầng; bệ thấp nhất nằm trong tầm nhảy từ sàn, khoảng cách dọc giữa các tầng là 65–95 px, bệ cao nhất cách trần ít nhất 125 px, và các bệ được kiểm tra liên kết để có đường nhảy lên.
- Giữ vững cơ chế nhảy xuyên, drop-down bằng phím Xuống, và bệ rung nứt tự hủy sau 60 giây.

### 2.5 Ba Hiểm Họa Chung + Hiểm Họa Riêng Từng Map
- **3 Hiểm Họa Chung (Xuất hiện ở mọi map)**:
  1. **Quả Tạ Quỹ Đạo**: Đung đưa trên dây xích kim loại theo hàm điều hòa.
  2. **Laser**: Cả dạng **Ngang và Dọc** với vệt laser đỏ cảnh báo nhấp nháy trước khi xả tia!
  3. **Đại Bác**: Cả dạng **Ngang và Dọc** với vệt ngắm cảnh báo trước khi bắn đại bác bốc lửa!
- **Hiểm Họa Rơi Riêng Từng Map**:
  - *Nhà Bếp*: Dao, Chảo rán, Cây cán bột, Lọ muối, Máy xay sinh tố.
  - *Phòng Khách*: Chậu cây, Điều khiển TV, Sách dày, Gối tựa, Đèn bàn.
  - *Phòng Ngủ*: Đồng hồ báo thức, Gối ngủ, Dép lông, Mảnh gương, Khối gỗ đồ chơi.
  - *Sân Vườn*: Quả táo đỏ, Tổ ong, Xẻng làm vườn, Quả dưa hấu, Hạt dẻ.
  - *Mái Nhà*: Viên gạch đỏ, Chảo vệ tinh, Ăng-ten TV, Chong chóng gió, Tia sét.

### 2.6 Độ Hiếm & Phụ Kiện Trang Phục Mèo (Cat Skins)
- **Common (Thường)**: Mèo Cam, Tuxedo, Tuyết Trắng, Xám Tro.
- **Uncommon (Đặc trưng)**: Mèo Tam Thể, Mèo Xiêm, Mèo Báo Bengal.
- **Rare (Hiếm)**: Cyber Neon (kính bảo hộ phát sáng), Mèo Nham Thạch (sọc dung nham), Mèo Bóng Ma (trong suốt ma mị).
- **Legendary (Huyền thoại)**: Mèo Ông Già Noel (mũ Noel + bông tuyết), Siêu Mèo (áo choàng đỏ tung bay + huy hiệu), Mèo Cướp Biển (mũ thuyền trưởng + bịt mắt + khăn đỏ).
- Hoạt ảnh bước chân, vẫy đuôi, chớp mắt, co giãn khi nhảy.

### 2.7 Bối Cảnh Bản Đồ Chi Tiết
- *Nhà Bếp*: Tường gạch ca-rô, giá treo chảo đồng, tủ bếp gỗ.
- *Phòng Khách*: Giấy dán tường sọc ấm, kệ sách lớn nhiều ngăn, tranh treo tường.
- *Phòng Ngủ*: Cửa sổ vòm ngắm trăng sao lung linh, sàn thảm êm.
- *Sân Vườn*: Bãi cỏ xanh mướt mọc mầm, hàng rào gỗ trắng, mây trôi nhẹ nhàng.
- *Mái Nhà*: Sàn ngói ống khói, trăng tròn sáng rực, bóng các tòa nhà chọc trời ban đêm.

### 2.8 Giao Diện HUD & Game Over Tinh Chỉnh
- HUD gọn gàng: `THỜI GIAN: 00:24.5s`, `ĐIỂM: 245`, `🐟 CÁ: +14`, `KỶ LỤC: 1250`. Bỏ trạng thái P1 không cần thiết.
- Khiên đỡ đòn vỡ hạt kính trong suốt, không hiện chữ `DEFLECTED` gây rối mắt.
- Mỗi lần có một buff trên sân hoặc một hiệu ứng có thời hạn đang hoạt động; tất cả buff cộng 50 điểm. Cooldown tính sau khi biến mất/hết hiệu lực: x2 25s, Bomb/Invincibility 35s, Shield/Heart 45s. Heart chỉ xuất hiện trong Co-op khi có đồng đội gục. Xem bảng thông số hiện hành để biết lifetime và trọng số.
- Khi chết không hiện `K.O.!`, thay bằng màn hình Game Over nổi bật kèm **Danh Hiệu Sinh Tồn**:
  - $< 20$s: **"Mèo Mới Vào Nghề" (Noob Kitten)**
  - $20 - 60$s: **"Mèo Nhanh Nhẹn" (Quick Paws)**
  - $60 - 120$s: **"Bậc Thầy Nhào Lộn" (Acrobatic Feline)**
  - $120 - 200$s: **"Bóng Ma Né Đòn" (Shadow Dodger)**
  - $> 200$s: **"Vua Sinh Tồn" (King of Dodge)**
- Game Over chỉ có **Chơi Lại** và **Màn Hình Chính**. Nếu đang chơi dở và chọn quay về menu từ màn hình tạm dừng, menu có nút **Tiếp Tục** để trở lại ván đó.

---

## 3. Hướng Dẫn Khởi Chạy

Chạy bằng 1 click trên Windows:
```powershell
.\dev.ps1 -Task run
```
*(hoặc nhấp đúp file `run.cmd`)*
*Nhấn `F9` để bật chế độ dev với Cá được nạp liên tục. Nhấn `F9` lần nữa để đặt lại hồ sơ về mặc định của người chơi lần đầu, bao gồm điểm số và vật phẩm đã mở khóa.*

Chạy kiểm thử tự động:
```powershell
.\dev.ps1 -Task test
```
