# Chạy, chơi và kiểm thử

[Mục lục](README.md) · [README English](../README.md) · [README tiếng Việt](../README_vi.md)

## Source development

[pom.xml](../pom.xml) yêu cầu JDK 25 (`maven.compiler.release=25`). Dùng Maven đã cài hoặc helper Windows [dev.ps1](../dev.ps1). `JAVA_HOME` nên trỏ đến JDK 25; helper lấy ứng viên đầu có `javac.exe` và không tự kiểm tra version. Vì vậy việc nó tìm thấy JDK cũ không chứng minh build được.

```powershell
java -version
javac -version
mvn -version
mvn compile javafx:run
mvn test
mvn package
```

`run.cmd` tương đương `dev.ps1 -Task run` (compile rồi javafx:run); `test.cmd` chạy task test; `dev.ps1 -Task package` chạy Maven package. Helper tải Maven 3.9.11 về `.tools/` nếu không tìm thấy Maven, nhưng không tải JDK. Lần build đầu tải dependency từ Maven Central. JAR Maven tạo trong `target/` không phải gói chạy độc lập kèm runtime/dependencies.

Tên goal `javafx:run` không quyết định backend vẽ: [Launcher](../src/main/java/com/dodgingcat/Launcher.java) chọn Java2D mặc định. Backend JavaFX dùng JVM property `dodgingcat.renderer=javafx` để so sánh; xem [kiến trúc](architecture.md). Bản portable offline có quy trình riêng trong [portable-windows.md](portable-windows.md).

## Màn hình và điều khiển

| Màn hình | Hành vi hiện tại |
| --- | --- |
| Boot | Title 2s, logo TADQ 2s tính từ lúc bắt đầu hiển thị; tải toolkit trước frame đầu và giải nén portable lần đầu là thời gian riêng |
| Menu | Trái: Shop, BGM, SFX. Phải: Leaderboard, Language, About Us. 1P/2P ở dưới title; Quit ở góc dưới. Dùng map của lần bắt đầu trận gần nhất, không vẽ mèo |
| Âm lượng | Bấm BGM/SFX để chuyển qua các mức 0/20/40/60/80/100%; mặc định profile là BGM 70%, SFX 80%. Không có slider hoặc màn Settings riêng |
| Loadout | Bấm tab hoặc Tab để đổi Map/Skin. Map chỉ liệt kê map sở hữu, chọn bằng chuột. Skin có cả skin khóa để xem; chỉ trang bị skin sở hữu |
| Skin 1P | Click skin đổi cho P1, con lăn cuộn lưới |
| Skin 2P | P1 WASD di chuyển con trỏ, Space chọn/hủy xác nhận; P2 phím mũi tên, Enter chọn/hủy. Con lăn cuộn; click skin chỉ đổi P1. Hai dấu ready hiện trạng thái; nút Start vẫn dùng loadout đã lưu, không bắt buộc cả hai ready |
| Countdown | Tổng 3s: 3 trong 1s, 2 trong 1s, 1 trong 0,7s, GO trong 0,3s; world chưa update |
| Gameplay | P1 A/D, W nhảy, S rơi xuyên bệ; P2 Trái/Phải, Lên nhảy, Xuống rơi xuyên. Sàn không cho rơi xuyên. Không dùng Space để nhảy |
| Pause | Esc/P khi đang chơi. Resume bằng nút, Esc/P, Space hoặc Enter; R restart; M về menu. Có nút BGM/SFX. BGM dừng ở pause |
| Game Over | Sau cả hai chết (hoặc mèo duy nhất chết), đợi 0,85s rồi lưu kết quả. R hoặc Play Again chơi lại cùng loadout; Esc hoặc Main Menu về menu |
| Shop | Con lăn cuộn skin, nút gacha mở skin. Tab map cho preview ảnh nền/nhạc cả khi chưa mua, nút Buy mới mua map. Esc hoặc Back về menu khi không có modal quay |
| Leaderboard/About Us | Esc hoặc Back về menu. About Us hiện logo TADQ và tên/mã bốn thành viên |

F11 đổi fullscreen/windowed, F10 bật FPS ở ứng dụng. Backend Java2D bắt đầu fullscreen; cửa sổ mặc định nhắm 960×640 pixel vật lý có xét DPI. Mất focus khi đang chơi tự pause và xóa trạng thái phím. Về menu từ pause hủy world; menu không có chức năng tiếp tục ván đã bỏ.

## Save và dev mode

[PersistenceManager](../src/main/java/com/dodgingcat/persistence/PersistenceManager.java) dùng Gson lưu JSON:

| Trường hợp | Vị trí |
| --- | --- |
| Chạy source thông thường | `${user.home}/.dodgingcat/dodging_cat_profile.json` |
| Portable | `data/profile.json` cạnh bản portable gốc, kể cả khi runtime dùng cache ASCII |
| Override khi chạy JVM | `dodgingcat.profile` là đường dẫn save, hoặc `dodgingcat.profilePathFile` là file UTF-8 ghi đường dẫn save; loại path-file ưu tiên |

Profile lưu ví, map/skin sở hữu, loadout, map chơi gần nhất, âm lượng, ngôn ngữ, số lần gacha và hai leaderboard. Mặc định: 0 cá, Kitchen, Ginger/Tuxedo, English, BGM 70%, SFX 80%. Save hỏng/không đọc được sẽ tạo mặc định và cố ghi lại; không có cam kết backup/ghi file nguyên tử. Không lưu vị trí người chơi hoặc checkpoint ván đang chơi.

Kết quả trận được cộng đúng một lần khi chuyển vào Game Over. Restart, Quit, đóng cửa sổ hoặc về menu giữa trận không gửi thưởng của trận đó vào ví. Mua map, quay gacha, đổi loadout/ngôn ngữ/âm lượng được lưu ngay.

F9 bật dev money, bù ví lên 1.000.000 mỗi update. F9 lần tiếp theo tạo profile mới hoàn toàn và về menu; đây là thao tác xóa tiến độ, không phải khôi phục ví trước khi bật dev.

Leaderboard tách Single/Co-op, sắp theo điểm giảm dần; điểm bằng nhau không có tie-break theo thời gian. Mỗi bảng lưu tối đa 10 lượt, UI chỉ hiện 6. Thông báo kỷ lục/âm thanh kỷ lục dành cho lượt lọt 6 vị trí hiển thị. Tên entry là `Single Cat` hoặc `Co-op Team`, không có màn nhập tên.

## Kiểm thử

`mvn test` chạy kiểm thử buff/cooldown, cá, player/platform, fairness/difficulty, save/load, gacha, định dạng âm thanh và UI snapshot. UI snapshot khởi động JavaFX và cần phiên desktop có graphics; đây không phải suite hoàn toàn headless. Output nằm trong `target/surefire-reports/` và `target/ui-preview/`, dùng profile kiểm thử riêng.

Sáu probe sau chỉ chạy khi bật property; chạy riêng từng probe trong desktop nhàn, không mở game khác cùng lúc đo:

| Probe | Lệnh Maven (PowerShell) | Output |
| --- | --- | --- |
| Các scene Java2D native | `mvn -q '-Dtest=Java2DSceneProbeTest' '-Ddodgingcat.awtSceneProbe=true' '-Ddodgingcat.awtSceneProbe.capped=true' test` | `target/java2d-scenes-60fps.txt` |
| Ứng dụng thật, input/F11/audio | `mvn -q '-Dtest=Java2DAppTest' '-Ddodgingcat.awtAppProbe=true' test` | `target/java2d-app-probe.txt` |
| BufferStrategy Java2D | `mvn -q '-Dtest=Java2DProbeTest' '-Ddodgingcat.awtProbe=true' test` | `target/java2d-probe.txt` |
| SFX spin thực | `mvn -q '-Dtest=AudioPlaybackTest' '-Ddodgingcat.audioProbe=true' test` | Assertion trong Surefire |
| Scene JavaFX so sánh | `mvn -q '-Dtest=SceneFrameProbeTest' '-Ddodgingcat.sceneProbe=true' test` | `target/scene-frame-probe-<heightCap>.txt` |
| JavaFX snapshot/readback diagnostic | `mvn -q '-Dtest=PerformanceProbeTest' '-Ddodgingcat.perfProbe=true' test` | `target/performance-probe.txt` |

Kết quả chạy hiện tại và giới hạn phép đo nằm ở [performance.md](performance.md). Build portable bằng compiler đi kèm chỉ biên dịch main source; không thay thế `mvn test`.
