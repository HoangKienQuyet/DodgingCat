# Kiến trúc Dodging Cat v1.3

Tài liệu cập nhật ngày **26/10/2026**, mô tả hai bản đồ Garden/Kitchen, Solo, Versus tại chỗ và cửa hàng mở khóa buff. Toàn bộ chữ trong game là tiếng Anh; tài liệu phát triển dùng tiếng Việt. Đề cương `README_vi.md` là đầu vào thiết kế, không phải danh sách tính năng đã hoàn thành.

## Các thành phần

```text
Launcher
└── DodgingCatApp (JavaFX)
    ├── GameWorld (mô phỏng Java thuần)
    │   ├── List<Player> (1 mèo Solo hoặc 2 mèo Versus)
    │   ├── List<Platform>
    │   ├── List<Hazard>
    │   ├── List<Pickup>
    │   └── List<PickupNotice>
    ├── GameRenderer (Canvas)
    ├── Profile (tiến trình trong bộ nhớ)
    └── SaveStore (Gson + file JSON)
```

| Vị trí | Trách nhiệm |
| --- | --- |
| `com.dodgingcat.Launcher` | Điểm vào riêng, gọi ứng dụng JavaFX; thuận tiện cho bản JAR chạy bằng classpath |
| `DodgingCatApp` | Cửa sổ, bàn phím, menu, HUD, tủ đồ, vòng lặp hiển thị và thời điểm lưu |
| `game/GameWorld` | Chế độ, bản đồ, thời gian, di chuyển, va chạm, bẫy/vật phẩm, điểm, loại người chơi và kết quả vòng |
| `game/Player`, `Hazard`, `Pickup` | Dữ liệu và hành vi nhỏ của từng đối tượng |
| `ui/GameRenderer` | Vẽ nền/bệ theo map, mèo và hoạt ảnh, cá, buff, bẫy và chữ nổi tại vị trí nhặt đồ |
| `save/Profile`, `RunResult` | Kỷ lục/lịch sử Solo, tiền cá Solo, ngoại hình, buff đã mở khóa và giao dịch |
| `save/SaveStore` | Đọc, kiểm tra, phục hồi và thay thế file JSON |
| `src/main/resources/com/dodgingcat/style.css` | Kiểu dáng các node giao diện JavaFX |

`GameWorld`, `Profile` và `SaveStore` không phụ thuộc JavaFX, nên có thể kiểm thử mà không mở cửa sổ. Ứng dụng kết hợp trực tiếp các thành phần bằng các trường đối tượng. Trạng thái dùng một enum; chưa có lớp State riêng, event bus, ECS, hệ plugin hay các subsystem được đề cương dự kiến.

## Vòng lặp và trạng thái

`AnimationTimer` trên JavaFX Application Thread lấy thời gian giữa hai khung hình, đọc các phím đang giữ rồi gọi `GameWorld.update(dt, inputP1, inputP2)`. Mô phỏng dùng bộ tích lũy với bước cố định **1/120 giây**. Mỗi lần cập nhật chỉ nhận tối đa 0,25 giây để một lần cửa sổ bị treo lâu không lập tức tua cả lượt chơi.

Sau cập nhật, ứng dụng ghi kết quả nếu lượt vừa kết thúc, đồng bộ menu/HUD và gọi renderer. Trong lượt chơi, hoạt ảnh dùng `world.elapsed()` nên đứng yên khi pause; menu và mèo xem trước dùng đồng hồ hiển thị riêng. Hoạt ảnh không quyết định va chạm hoặc điểm. Toàn bộ xử lý hiện chạy trên một luồng JavaFX, không có luồng nền cho gameplay.

```text
MENU ── bắt đầu ──> PLAYING ── hết giờ Versus / mọi mèo bị loại ──> GAME_OVER
                     │  ▲                       │
            tạm dừng │  │ tiếp tục               └── chơi lại → PLAYING
                     ▼  │
                    PAUSED ── bỏ lượt ──> MENU
```

`GameWorld.State` gồm `MENU`, `PLAYING`, `PAUSED`, `GAME_OVER`. Khi không ở `PLAYING`, mô phỏng không tiến thời gian. Mất tiêu điểm sẽ xóa các phím đang giữ và tạm dừng. Menu/tủ đồ là các JavaFX node phủ lên Canvas; cờ `shopOpen` thuộc giao diện, không tạo thêm trạng thái mô phỏng.

`GameWorld.Mode` gồm `SOLO`, `VERSUS`. Mỗi `Player` có `id`, `alive`, số cá, khiên và thời gian hiệu ứng riêng. Versus có `VERSUS_DURATION_SECONDS = 60` và `timeRemaining()` cho HUD. Vòng kết thúc khi hết 60 giây mô phỏng hoặc cả hai mèo bị loại sớm; một mèo bị loại không dừng vòng nếu vẫn còn giờ và người khác sống. `winnerId()` trả 1 hoặc 2 theo số cá cao hơn, 0 nếu hòa hoặc chưa có kết quả. Không xét thời gian sống để phá hòa. Pause không tiêu thời gian đấu.

P1 Versus dùng A/D, W/Space và S; P2 dùng các phím mũi tên. Solo cho phép cả hai nhóm phím. Renderer vẽ nhãn P1/P2 và màu khác nhau, lấy P2 bằng màu kế tiếp trong ba ngoại hình. Mèo đã loại dùng hình mờ đứng yên, dấu X và nhãn; không tiếp tục hoạt ảnh hoặc hiện buff.

`GameWorld.MapTheme` gồm `GARDEN`, `KITCHEN`. Constructor đầy đủ là `(seed, mode, unlockedBoosts, mapTheme)`; các overload cũ mặc định Garden để giữ tương thích. Map quyết định danh sách bệ và nhóm bẫy của world, được giữ cố định trong một lượt. `DodgingCatApp.selectedMap` chỉ là lựa chọn của phiên hiện tại: cả hai map mở miễn phí tại menu; chơi lại/đổi mode giữ map, khởi động ứng dụng mới mặc định Garden. Không có hệ tự qua màn và không thêm trường map vào file lưu.

`lastEvent` và `eventVersion` phục vụ các thông báo trạng thái như bắt đầu, đỡ đòn, loại mèo và kết thúc lượt. Các lần nhặt cá/buff dùng danh sách `PickupNotice` riêng, không thay banner trạng thái trên đầu màn hình.

`PickupNotice` là record bất biến gồm id, playerId, tọa độ tâm vật phẩm, nội dung, loại buff và `bornAt` theo đồng hồ mô phỏng. Mỗi lần nhặt tạo một record riêng; cá ghi đúng `+1 fish` hoặc `+2 fish`. Engine giữ tối đa 32 record, tự bỏ sau `PICKUP_NOTICE_SECONDS = 1.2`, xóa khi bắt đầu lại/về menu và cung cấp danh sách chỉ đọc.

Renderer vẽ các nhãn sau nhân vật, trước thanh điều khiển, chỉ ở `PLAYING`/`PAUSED`. Nhãn bám tọa độ vật phẩm đã lưu, bay lên khoảng 36 pixel và mờ dần cuối đời; không bám theo mèo. Chữ đậm có viền tối, màu theo loại vật phẩm và tiền tố P1/P2 trong Versus. Vị trí x được chặn theo độ rộng chữ, y tránh HUD; các nhãn gần nhau được cấp hàng riêng một lần, không đổi hàng khi nhãn cũ hết hạn. Tuổi nhãn lấy từ `elapsed − bornAt`, nên pause cũng dừng hoạt ảnh nhặt đồ.

## Hệ tọa độ và di chuyển

- Thế giới logic: **960 × 640**, sàn tại `y = 576`. Giao diện bao gồm Canvas và thanh bên, được co theo cửa sổ mà không đổi tọa độ vật lý.
- `Player.x/y` và `Hazard.x/y` là góc trái trên. Player có khung 32 × 38; kiểm tra sát thương thu nhỏ vào trong để bớt khắt khe hơn hình vẽ.
- `Pickup.x/y` là tâm; nhặt đồ dùng khoảng cách giữa hình tròn bán kính 11 và hình chữ nhật player.
- `Platform` là record `(x, y, width)`, với `y` là mặt trên. Mỗi map có 5 bệ cố định với tọa độ riêng; chưa có bộ sinh hoặc bệ sụp.

| Map | Bệ thấp trái/phải `(x, y, width)` | Bệ giữa | Bệ cao trái/phải |
| --- | --- | --- | --- |
| Garden | `(130, 462, 185)` / `(645, 462, 185)` | `(385, 360, 190)` | `(130, 258, 185)` / `(645, 258, 185)` |
| Kitchen | `(95, 466, 210)` / `(655, 466, 210)` | `(365, 356, 230)` | `(150, 248, 180)` / `(630, 248, 180)` |

Va chạm, làn bẫy ngang, vị trí sinh cá/buff và fixture đặt player đều dùng danh sách bệ của map đang chơi. Các độ cao khác nhau không chỉ là thay màu nền.

Khi đặt cá/buff, bộ sinh thử tối đa 8 vị trí trên sàn hoặc bệ của map, tránh các vật phẩm quá gần và tránh chồng lên bẫy đang hoạt động với padding. Đây là kiểm tra tại thời điểm sinh; vật phẩm không được bảo đảm an toàn trong toàn bộ thời gian tồn tại vì bẫy có thể di chuyển đến sau.

Trọng lực và gia tốc cập nhật vận tốc mỗi bước. Bệ chỉ chặn khi player đang rơi và đi qua mặt trên, nên có thể nhảy xuyên từ dưới. Nhấn xuống tạm bỏ va chạm với bệ. Nhảy có khoảng đệm đầu vào 0,12 giây và khoảng cho phép 0,10 giây ngay sau khi rời mép bệ. Phím phải được nhả rồi nhấn lại để tạo lần nhảy mới.

Hoạt ảnh mèo được vẽ bằng mã Canvas: bước chân luân phiên, đuôi đung đưa, co chân và kéo dài thân khi bật lên, dang chân khi rơi. Renderer theo dõi tiếp đất riêng cho từng mèo để nén thân và tạo bụi trong 0,16 giây. Các tư thế không thay kích thước hitbox, vận tốc hay quy tắc va chạm. Mèo bị loại giữ tư thế tĩnh.

## Bẫy, buff và điểm

`HazardKind` có `KNIFE`, `DART`, `LASER`, `POISON`, `SCISSORS`, `FLAME`. Garden chỉ sinh `DART` (mũi tên bay ngang) và `POISON` (vùng độc trôi ngang). Kitchen chỉ sinh `KNIFE` (dao rơi), `SCISSORS` (kéo bay ngang) và `FLAME` (lửa gas trên sàn/bệ). Nhóm bẫy được chọn theo map, không trộn bẫy Kitchen vào Garden.

Các bẫy tự sinh hoạt động ngay, không có giai đoạn báo trước; mũi tên không có đường hay dấu ngắm cảnh báo. Lửa gas đứng yên, được đặt trên một mặt sàn/bệ hợp lệ và tự hết sau `Hazard.FLAME_LIFETIME_SECONDS = 1.65`. `LASER` vẫn được giữ cho fixture và các test hồi quy: có cảnh báo rồi hoạt động trong 0,62 giây. **Cả hai map không sinh laser tự nhiên**, không có bộ đếm lịch laser trong gameplay v1.3.

`BuffKind` có `SHIELD`, `INVINCIBILITY`, `DOUBLE_FISH`, `BOMB`; pickup có `kind == null` là cá. Hiệu ứng chỉ áp dụng cho người nhặt: khiên giữ đến khi đỡ một đòn, miễn sát thương 5 giây, hoặc nhân đôi cá 5 giây. Bom xóa danh sách hazard chung ngay. Các bộ đếm và cờ bảo vệ thuộc từng `Player`; renderer/HUD không lấy buff từ trạng thái chung của world. Không có máu hoặc hồi sinh.

`score()` là điểm sinh tồn theo thời gian của vòng, lấy phần nguyên; `elapsed()` là thời gian mô phỏng. Buff nhân đôi cá không đổi tốc độ cộng điểm. Cá nhặt tăng 1 hoặc 2 cho đúng người nhặt, quyết định kết quả Versus và được cộng tổng vào `world.fishCollected()`.

Khi Solo chuyển sang `GAME_OVER`, ứng dụng gọi `Profile.recordRun(world.score(), world.fishCollected())` đúng một lần qua cờ `runRecorded` rồi lưu. **Versus bỏ qua hoàn toàn bước ghi kết quả/lưu hồ sơ này**: cá chỉ quyết định thắng/thua, không cộng tiền cửa hàng, không đổi kỷ lục hoặc lịch sử Solo. Bỏ lượt hoặc đóng ứng dụng giữa lượt Solo chưa ghi kết quả.

`GameWorld.Boost` xác định bốn buff cửa hàng: `SHIELD`, `INVINCIBILITY`, `DOUBLE_FISH`, `BOMB`. `Profile` giữ tập đã mở khóa và truyền tập đó vào world khi bắt đầu lượt. Trong Solo, **nhóm buff mặc định rỗng**; bom cũng cần mua mở khóa như ba loại còn lại. Khi nhóm rỗng, engine không thử chọn một phần tử để sinh buff. Mua chỉ mở khóa vĩnh viễn **khả năng rơi ngẫu nhiên**, không trang bị, không cấp buff đầu lượt, không cung cấp hành động kích hoạt thủ công. **Versus dùng đầy đủ bốn loại buff bất kể hồ sơ**.

## Luật sinh bẫy và giới hạn kết luận

Bộ sinh nằm trong `GameWorld`, dùng `java.util.Random` với seed truyền vào constructor. Một đợt tạo bẫy thông thường thử tối đa 6 ứng viên. Ứng viên phải qua `canSpawn` trước khi được thêm vào danh sách.

Các ràng buộc đang có:

1. Giới hạn tổng số hazard: `min(6, 3 + floor(elapsed / 30))`.
2. Bẫy đang hoạt động không được sinh chồng lên khung của mèo còn sống, có thêm padding 3 pixel.
3. Các dao cách nhau ít nhất 85 pixel theo trục ngang; các bẫy ngang (mũi tên, độc, kéo) cách nhau ít nhất 60 pixel theo trục dọc.
4. Các hành lang dao, mở rộng 35 pixel mỗi bên, không phủ quá nửa chiều ngang thế giới.
5. Dao phải chừa một điểm né ngang lấy mẫu mỗi 8 pixel cho **mọi mèo còn sống**, theo ngân sách thời gian rơi thực còn lại có trừ biên phản ứng/gia tốc.
6. Bẫy ngang cùng làn dọc với mèo phải còn ít nhất 0,45 giây bay đến khung hiện tại của mèo; bẫy đang bay ra xa được loại khỏi phép kiểm tra đó.
7. Lửa phải nằm trong bề rộng một mặt sàn/bệ hợp lệ và cách khung mọi mèo sống ít nhất 120 pixel theo khoảng cách giữa hai hình chữ nhật. Hai ngọn lửa gần cùng độ cao phải có khoảng tách ngang.
8. Đường API laser cũ vẫn yêu cầu cảnh báo tối thiểu 0,9 giây và hạn chế sinh cùng các bẫy khác; đây là kiểm tra hồi quy, không phải nội dung tự sinh của hai map.

Khoảng sinh thông thường giảm theo thời gian, có mức sàn 0,72 giây. Bẫy đầu tiên được thử sau 2,8 giây; nhóm bẫy của map có thể được chọn ngay từ đợt này. Khi một đợt không chấp nhận được ứng viên, engine đặt lịch thử lại; các ứng viên vẫn phải đi qua cùng bộ luật.

Đây là **kiểm tra cục bộ**, không tìm toàn bộ quỹ đạo nhảy/rơi, không dự đoán mọi lựa chọn của hai người và không chứng minh vòng chơi luôn giải được. Điểm né ngang lấy mẫu và thời gian bay không bao quát mọi tương tác với các bẫy đang chuyển động. Các test seed xác nhận một số tình huống cụ thể; không suy ra bảo đảm cho mọi seed hoặc mức kỹ năng.

Cùng seed, map, chế độ, tập buff đã mở khóa, trạng thái khởi đầu và chuỗi `Input` của từng người theo cùng bước thời gian cho kết quả mô phỏng lặp lại. Chạy giao diện thông thường dùng seed mới từ `System.nanoTime()`. Đối số ứng dụng `--seed=<số>` cố định seed; việc người dùng bấm phím theo thời gian thực vẫn không phải một bản replay đầu vào.

## Lưu tiến trình

`Profile` giữ 3 ngoại hình: cam miễn phí, kem 25 cá, mun 50 cá; cùng bốn mở khóa buff: khiên **25**, miễn sát thương **40**, nhân đôi cá **60**, bom **50**. Giao dịch không thể tiêu quá số dư hoặc tính tiền lại món đã sở hữu. Hồ sơ giữ tối đa 10 `RunResult`, mới nhất trước. Lịch sử được cung cấp dưới dạng bản chụp không sửa được; các danh sách đối tượng của world cũng chỉ cho đọc từ bên ngoài.

`SaveStore` mặc định dùng `~/.dodgingcat/profile.json`. Đối số ứng dụng `--save=<đường-dẫn>` cho phép dùng file khác, hữu ích khi kiểm thử mà không đụng hồ sơ thật. **Schema 2** thêm các buff đã mở khóa bên cạnh kỷ lục, tiền cá, ngoại hình và lịch sử. Khi đọc schema 1, dữ liệu hợp lệ trước đó được giữ nguyên và khởi tạo tập buff rỗng; lần lưu tiếp theo dùng schema 2. Hồ sơ schema 2 cũ giữ nguyên các mục đã mua; thiếu `BOMB` trong tập mở khóa nghĩa là bom còn khóa. v1.3 giữ nguyên schema và toàn bộ API giao dịch/lưu; lựa chọn map không được ghi vào JSON.

Đọc JSON dùng Gson ở chế độ strict, kiểm tra số hữu hạn, số nguyên không âm, ngoại hình hợp lệ và giới hạn lịch sử. File thiếu trả hồ sơ mới. File hỏng hoặc schema không hỗ trợ được giữ lại, cố sao chép thành `.corrupt-….bak` trước khi dùng mặc định; trường lỗi trong JSON còn đọc được được sửa có cảnh báo.

Khi ghi, hệ thống tạo file tạm cùng thư mục, ghi UTF-8 và flush qua `FileChannel.force(true)`, rồi thay file đích bằng atomic move nếu hệ thống file hỗ trợ. Nếu không hỗ trợ, dùng phép thay thế thông thường. Lỗi được trả qua `warning()` và hiện trên thanh bên. Đây là lưu cục bộ, chưa có đồng bộ cloud hoặc xử lý nhiều phiên ứng dụng cùng sửa một hồ sơ.

## Kiểm thử và phát triển tiếp

Test mô phỏng dùng bước thời gian cố định và fixture trong cùng package; test lưu dùng thư mục tạm. Phạm vi v1.3 bổ sung map Garden/Kitchen và nhóm bẫy/bố cục riêng, bên cạnh Solo/Versus 60 giây, cá/hiệu ứng riêng, thắng/hòa, kinh tế Solo, bốn buff và hồ sơ cũ. Chạy `test.cmd` hoặc `dev.ps1 -Task test`; kết quả từng lần ở `target/surefire-reports`. [TESTING.md](TESTING.md) tách trạng thái xác minh v1.3 khỏi kết quả lịch sử v1.2; không dùng kết quả phiên bản trước để kết luận về thay đổi mới.

Ứng dụng có chế độ `--smoke-test` để kiểm tra nút/phím trên scene thật, chuyển chế độ, cửa hàng, pause, kết thúc vòng, lưu và chụp PNG. Đây là kiểm tra desktop, không thay thế kiểm thử tương tác của người chơi. Chưa có báo cáo benchmark FPS, chứng minh độ công bằng toàn cục hoặc kiểm thử Android/co-op.

## Bản phân phối Windows

`package.ps1` gọi `dev.ps1 -Task package`, lấy JAR và các dependency Windows, rồi dùng `jlink` tạo runtime Java thu gọn. JavaFX đặt riêng trong `lib/` và nạp qua module path; Gson đặt trên classpath.

```text
DodgingCat-Windows-x64.zip
└── DodgingCat/
    ├── Play.cmd
    ├── dodging-cat.jar
    ├── lib/         (JavaFX Windows và Gson)
    └── runtime/     (Java runtime từ jlink)
```

`Play.cmd` dùng `runtime/bin/java.exe`, thêm các module `javafx.controls`, `javafx.swing`, rồi gọi `com.dodgingcat.Launcher`. Launcher chuyển tiếp đối số ứng dụng. Bản ZIP bàn giao dùng Java 23, dành cho Windows x64, chạy offline và không gọi Maven hoặc yêu cầu cài JDK. `run.cmd` trong mã nguồn là luồng khác: chạy `dev.ps1` để biên dịch và khởi động qua Maven.

Runtime là thành phần của bản ZIP, không được tạo thêm khi mở game. Giữ nguyên toàn bộ thư mục sau giải nén. Lưu tiến trình vẫn ở thư mục home của người dùng, không ghi vào `runtime/` hay thay đổi mã nguồn.

Tên gói bàn giao v1.3 là `DodgingCat-Windows-x64-v1.3.zip` và `DodgingCat-Source-v1.3.zip`. Script `package.ps1` vẫn tạo tên ZIP không kèm phiên bản như cây trên; bản bàn giao được đặt tên theo phiên bản để phân biệt các lần phát hành.

## Phần còn phát triển

Các hướng mở rộng còn lại gồm âm thanh, bản đồ bổ sung, bệ động/sụp, replay đầu vào, lịch sử trên UI và kiểm tra quỹ đạo né bẫy sâu hơn. Nếu thêm các phần đó, cần cập nhật phạm vi và bằng chứng kiểm thử thay vì xem các mục trong đề cương là tính năng sẵn có.
