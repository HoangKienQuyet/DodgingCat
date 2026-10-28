# Shop, gacha và loadout

[Mục lục](README.md) · [README English](../README.md) · [README tiếng Việt](../README_vi.md)

Đối chiếu [ShopManager](../src/main/java/com/dodgingcat/shop/ShopManager.java), [GachaSequence](../src/main/java/com/dodgingcat/shop/GachaSequence.java), [GameController](../src/main/java/com/dodgingcat/core/GameController.java) và [GameRenderer](../src/main/java/com/dodgingcat/ui/GameRenderer.java).

## Catalog và mở khóa

Profile mới có **Kitchen, Ginger Tabby và Tuxedo**. Tổng cộng năm map, 15 skin. Ví chỉ hiện trong shop; HUD trong trận hiện số cá nhặt của lượt.

| Map ID | Tên | Mở khóa |
| --- | --- | --- |
| kitchen | Kitchen / Nhà Bếp | Có sẵn |
| living_room | Living Room / Phòng Khách | 2.500 cá |
| bedroom | Bedroom / Phòng Ngủ | 2.500 cá |
| garden | Garden / Sân Vườn | 2.500 cá |
| rooftop | Rooftop / Mái Nhà | 2.500 cá |

Catalog cũng đặt giá Kitchen là 2.500, nhưng vì profile mới đã sở hữu nên không có giao dịch mua lại. Map không có phân hạng rarity trong UI. Bấm card map để preview background và BGM kể cả khi khóa; bấm Buy mới trừ tiền. Preview không trang bị map hoặc thay map đã chơi gần nhất. Mua không đủ tiền không mở khóa.

| Rarity | Skin ID → Tên hiển thị English | Số lượng |
| --- | --- | ---: |
| Common | ginger → Ginger Tabby; tuxedo → Tuxedo; snow_white → Snow White; classic_gray → Classic Gray; calico → Calico Tri-color | 5 |
| Uncommon | siamese → Siamese; bengal → Bengal Leopard; mochi → Mochi; blue_russian → Blue Russian | 4 |
| Rare | lava → Lava Tabby; super → Super Cat; pirate → Pirate Cat | 3 |
| Legendary | cyber → Neon Cat; ghost → Spirit Cat; santa → Santa Cat | 3 |

UI hiện tại chỉ mở skin bằng **gacha không trùng**. Các trường giá từng skin và API `buyItem` vẫn tồn tại trong model, nhưng click thẻ skin không mua lẻ trong controller. Vì vậy không dùng các trường giá cũ đó để mô tả kinh tế người chơi.

## Giá và xác suất

Giá lần quay thành công kế tiếp lấy theo `skinGachaPulls`, bắt đầu từ index 0:

`50, 100, 200, 350, 500, 750, 1000, 1350, 1750, 2200, 2800, 3500, 4500, 6000, 8000` cá.

Từ index cuối giữ mức 8.000. Profile mới có 13 skin cần mở, nên nếu không sửa profile sẽ dùng 13 giá đầu, tổng 19.050 cá. Không đủ tiền hoặc đã đủ bộ sưu tập: không trừ tiền, không tăng số lượt, không quay.

| Rarity | Trọng số mỗi skin chưa sở hữu | Số còn lại ở profile mới | Xác suất rarity ở lượt đầu |
| --- | ---: | ---: | ---: |
| Common | 100 | 3 | 300/541 ≈ 55,45% |
| Uncommon | 40 | 4 | 160/541 ≈ 29,57% |
| Rare | 20 | 3 | 60/541 ≈ 11,09% |
| Legendary | 7 | 3 | 21/541 ≈ 3,88% |

Xác suất rarity = trọng số × số skin còn lại của rarity / tổng trọng số. Sau đó chọn đều trong rarity đã trúng. Xác suất được tính lại sau mỗi lần mở khóa; không phải tỉ lệ rarity cố định cho mọi lượt, không có pity counter. Giải thưởng được chọn, trừ tiền, thêm sở hữu và lưu **ngay khi bắt đầu pull**, trước hoạt ảnh. Đóng game giữa spin không hoàn tiền; skin đã được lưu.

## Trình tự quay và âm thanh

1. Tạo reel ngẫu nhiên để trình diễn, đặt đúng skin trúng vào `winningIndex`. Random reel riêng không thay giải thưởng đã chọn.
2. `SPINNING`: tiến độ theo playback `spin` và chỉ dừng khi audio kết thúc. File hiện tại `spin.wav` dài 11,312s; thay file thì dùng duration mới. Khi mute/thiết bị lỗi dùng tiến độ im lặng theo duration decode được; nếu không load được sample, fallback mặc định 3,2s.
3. `LANDED`: giữ đúng ô trúng 0,55s.
4. Legendary có `LEGENDARY_REVEAL` 1,8s hội tụ/bung hạt, rồi mới tới box. Neon dùng cyan/circuit, Spirit dùng sao trắng xanh, Santa dùng trắng đỏ/hộp quà. Common/Uncommon đi thẳng tới box; Rare có pháo hoa vàng khi box hiện.
5. `RESULT`: box preview skin trúng và `congrat` SFX. Box tự đóng sau 2s, hoặc click/Esc bỏ qua ở phase này; không chờ SFX chúc mừng phát hết. Click trong lúc spinning không bỏ qua spin.

Shop không nhận thao tác nền khi modal đang active. Không có dòng thông báo trúng cũ phía trên nút gacha. Hover các card và nút phát UI hover khi đi vào control.

## Trang bị và nhạc

Trang bị thực hiện trong loadout, không trong shop. Tab Map liệt kê map sở hữu; tab Skin có cả thẻ khóa nhưng chỉ chọn được skin sở hữu. P1/P2 có con trỏ riêng qua bàn phím, còn click chuột thay skin P1; con lăn dùng chung viewport. Phím xác nhận và Start được mô tả ở [hướng dẫn chơi](running-and-testing.md).

Menu dùng background `lastPlayedMap`; biến này chỉ cập nhật khi bắt đầu trận, khác với map đang chọn. Loadout dùng background map đang chọn nhưng vẫn nhạc menu. Trong shop tab Map dùng background/nhạc map preview; tab Skin dùng nhạc menu. Countdown và gameplay dùng nhạc map; pause/Game Over dừng BGM.

| Context / Map | BGM trong `assets/audio/bgm/` |
| --- | --- |
| Menu/loadout/leaderboard/about/skin shop | BGM-menu.mp3 |
| kitchen | kitchen_Pixabay.mp3 |
| living_room | Living room.mp3 |
| bedroom | Bedroom.mp3 |
| garden | Garden.mp3 |
| rooftop | Rooftop.mp3 |

Neon/Spirit/Santa có hiệu ứng hình ảnh, âm di chuyển và hình tiền riêng. Skin không đổi tốc độ, hitbox, giá trị tiền hoặc tỉ lệ spawn. Xem [ảnh](../src/main/resources/assets/images/README.md), [âm thanh](../src/main/resources/assets/audio/README.md) và [gameplay](gameplay-thong-so.md).
