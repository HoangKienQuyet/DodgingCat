# Windows x64 portable và đóng gói bài nộp

[Mục lục](README.md) · [README English](../README.md) · [README tiếng Việt](../README_vi.md) · [Công cụ đóng gói](../tools/README.md)

## Bản source trực tiếp hiện dùng

[build-direct-portable.ps1](../build-direct-portable.ps1) tạo `DodgingCat-source-portable-Windows-x64.zip`. Sau khi giải nén, thư mục `DodgingCat/` có:

```text
Run.cmd       chạy game bằng runtime đi kèm
Build.cmd     biên dịch lại main source bằng compiler đi kèm
README.txt    hướng dẫn nhanh riêng cho portable
README.md / README_vi.md
pom.xml
src/          main source, tests và PNG/WAV/MP3 gốc
docs/         tài liệu
support/      archive runtime/dependencies, helper và license
```

1. Giải nén toàn bộ vào thư mục được ghi; không chạy trong ZIP viewer.
2. Mở `Run.cmd`. Lần đầu kiểm tra hash và giải nén Java 25/JavaFX/Gson/JLayer từ `support/platform.7z` vào `support/runtime` và `support/app`.
3. Khi interview cần sửa code, đóng game, sửa `src/main/java`, chạy `Build.cmd`, rồi `Run.cmd`. Source resource đọc trực tiếp từ `src/main/resources`.

Không cần cài Java/JDK/Maven/7-Zip, mạng hoặc quyền admin trên máy demo. Chỉ hỗ trợ Windows x64, cần cho phép chạy executable và PowerShell. Compiler đi kèm build main source offline, không chạy JUnit; muốn chạy test Maven cần Maven và dependencies/JUnit riêng. `Build.cmd` giữ JAR cũ trong `support/build/<id>/previous-game-code.jar`.

Đường dẫn có ký tự ngoài ASCII khiến helper tạo cache vật lý trong `%LOCALAPPDATA%` hoặc `%PUBLIC%/DodgingCatPortableRuntime/<SID>/<hash>`. Source được đồng bộ trước mỗi lần prepare/build; save luôn ở `data/profile.json` cạnh bản gốc. Vị trí runtime ghi trong `support/portable-root.txt`. Nên dùng đường dẫn ASCII như `C:/Demo/DodgingCat`; chừa khoảng 500 MB ổ trống, nhiều hơn nếu có nhiều bản/cache/build. Không xóa `src` hoặc `support` vì game cần assets/runtime ở đó.

## Tạo lại gói

Máy đóng gói cần JDK 25 có `jlink`, Maven trên PATH và dependencies Windows đã cache trong repository Maven. Chạy ở repository root:

```powershell
.\build-direct-portable.ps1
```

Có thể truyền `-OutputDirectory '<thư mục mới>'` và `-MavenRepository '<.m2/repository>'`. Output phải chưa tồn tại. Script compile main với `-DskipTests`; chạy `mvn test` riêng trước phát hành. Script dùng `tools/portable-direct/7zr.exe`, jlink runtime kèm javac/jar, rồi ZIP source và runtime nén. Không resize hoặc chuyển mã PNG/WAV/MP3. Source JDK/compiler/third-party notices được giữ theo template license đi kèm.

Build report ngoài ZIP ghi byte, SHA-256 ZIP và SHA-256 platform archive. Gói hiện tại trên máy tác giả nằm trong `deliverables/` và bị Git ignore; không coi đường dẫn artifact trên máy tác giả là file có sẵn ở clone GitHub. Sau sửa source phải build lại để JAR khớp source. Nếu chỉ cập nhật tài liệu trong ZIP, ghi lại hash/kích thước và ghi rõ kiểm chứng chạy trước đó áp dụng cho code/runtime không đổi.

## Bài nộp LMS

Giới hạn do người dùng cung cấp: **một file, 100 MB**. Dùng ngưỡng bảo thủ dưới **100.000.000 byte** cho ZIP cuối, gồm game/source portable, report và slide. ZIP game riêng chưa phải bài nộp hoàn chỉnh. Kích thước sau thêm report/slide phải đo lại; không lấy dung lượng thư mục giải nén thay dung lượng ZIP.

Direct-source package không chép `target`, `.git`, cache IDE hoặc các deliverable trước. Nó giữ tests vì cần source để review/debug; tests không tự chạy khi demo. Giữ giấy phép thư viện. Trước khi nộp, giải nén ZIP cuối ở thư mục mới, thử Run, sửa/build một thay đổi kiểm chứng trên bản thử riêng, kiểm tra tiếng Việt/ASCII, âm thanh và tài liệu. Quy định môn học về report/slide/interview cần đối chiếu tài liệu đề bài chính thức; đây là hướng dẫn kỹ thuật của repository.

## Công cụ legacy còn trong repo

[build-portable.ps1](../build-portable.ps1) và [tools/portable](../tools/portable/README.md) là phương án đóng gói lossless trước đây: source nằm dưới `DodgingCat/source`, runtime/assets cần chuẩn bị từ payload, có launcher native/Troubleshoot. `-DemoOnly` bỏ compiler nhưng vẫn giữ source. Cấu trúc và đường dẫn này khác bản direct-source ở trên; không áp dụng hướng dẫn `source/src` cho bản hiện tại. Xem lịch sử ở [history](history/README.md).
