# Thông số gameplay — 28/10/2026

Đối chiếu trực tiếp với `ProceduralGenerator`, `GenerationRules`, `GameWorld`, `Player`, `BuffType`, `FishType` và các lớp hazard. Thời gian dưới đây là giây trong simulation; pause không làm các bộ đếm này chạy tiếp. Khoảng cách/tốc độ dùng đơn vị logic của game, chiều cao sân luôn 640; fullscreen tăng chiều rộng theo tỉ lệ màn hình.

## Buff

| Buff | Tỉ lệ cơ sở khi chọn loại | Khi 2P có đồng đội chết | Cooldown riêng | Tồn tại trên sân | Hiệu lực sau khi nhặt |
| --- | ---: | ---: | ---: | ---: | --- |
| Double Score | 40% | 32% | 25s | 8s | 5s; điểm sống sót từ 10 lên 20 điểm/s |
| Bomb | 25% | 20% | 35s | 5s | Tức thì: xóa mọi hazard đang active |
| Invincibility | 20% | 16% | 35s | 4s | Miễn sát thương 5s |
| Shield | 15% | 12% | 45s | 4s | 5s hoặc chặn một đòn chí mạng; sau khi vỡ có 1s miễn sát thương |
| Heart | 0% | 20% | 45s | 4s | Hồi sinh một đồng đội, người hồi sinh được miễn sát thương 3,5s |

- Đây là trọng số chọn **loại buff**, không phải xác suất xuất hiện mỗi frame. Loại đang cooldown bị loại khỏi pool; các trọng số còn lại được chuẩn hóa lại. Ví dụ chỉ còn Shield đủ điều kiện thì Shield có xác suất 100%.
- Mọi buff cộng **50 điểm đúng một lần khi nhặt**, kể cả Heart và Double Score. Bonus này cố định, không nhân đôi bởi Double Score.
- Không ai nhặt: cooldown bắt đầu khi vật phẩm hết hạn và biến mất. Có người nhặt: buff có thời hạn bắt đầu cooldown khi hết hiệu lực hoặc bị kết thúc sớm (ví dụ khiên vỡ). Bomb hoàn tất tức thì nên bắt đầu cooldown sau khi nhặt. Heart chờ hết 3,5s bảo vệ hồi sinh; nếu chuyển thành khiên thì chờ khiên hết/vỡ. Không tính cooldown từ lúc spawn và không tính khoảng thời gian vật phẩm còn trên sân/hiệu lực còn hoạt động vào cooldown.
- Bộ đếm kiểm tra khởi tạo 0,5s và đặt lại 0,5s sau mỗi lần thử. Chỉ sinh khi không còn buff trên sân, không người chơi nào đang giữ buff có thời hạn, còn ít nhất một người sống và có platform trên cao chưa sập. Không có lần tung xác suất spawn riêng nữa. Nếu bộ đếm đã hết trong lúc điều kiện chưa hợp lệ thì khi điều kiện hợp lệ có thể thử ngay.
- Shield, Invincibility và Double Score không cộng dồn trên cùng một người; áp dụng một loại sẽ xóa các hiệu ứng có thời hạn trước đó. Bomb không xóa buff người chơi đang có.
- Heart chỉ được đưa vào pool khi chơi 2P và có đồng đội chết. Nếu lúc nhặt không còn ai cần hồi sinh thì chuyển thành Shield 5s cho người nhặt.
- Chỉ số tỉ lệ ở bảng là khi mọi loại phù hợp đều hết cooldown. Buff nhấp nháy trong 1,2s cuối trước khi biến mất.

## Tiền

| Loại | Tỉ lệ mỗi lần spawn | Giá trị | Tồn tại trên sân | Kích thước logic |
| --- | ---: | ---: | ---: | --- |
| Fresh Fish | 85% | 1 fish | 5s | 52 × 42 |
| Canned Fish | 15% | 5 fish | 5s | 48 × 42 |

- Lần xuất hiện đầu sau 4s; cooldown 4s tính từ lúc spawn thành công, không chờ nhặt hoặc hết hạn. Mỗi vật phẩm tồn tại tối đa 5s. Giới hạn 5 vật phẩm tiền trên sân. Khi đã đạt giới hạn, đợi đến khi có chỗ trống để thử sinh tiếp; thiếu platform không làm reset cooldown.
- Chọn một platform chưa sập, gồm cả sàn, rồi đặt tiền phía trên platform. Không có cooldown riêng cho từng mệnh giá.
- Kỳ vọng mỗi lần spawn là 1,6 fish; 15 lần spawn/phút tương ứng trung bình lý thuyết 24 fish/phút nếu nhặt hết, đủ platform và không bị giới hạn. Đây không phải số tiền bảo đảm thu được.
- Tiền được cộng vào phần thu được trong lượt chơi và lưu vào ví khi kết thúc lượt. Không có effect duration; đó là tiền tích lũy. Nhấp nháy trong 1,2s cuối.
- Skin Neon/Spirit/Santa thay hình tiền theo skin người chơi lúc spawn, giữ nguyên xác suất và giá trị.

## Hazard

| Thời gian sống sót | Khoảng spawn mục tiêu | Tối đa active | Hệ số tốc độ | Vật rơi | Laser / Cannon / Orbital, mỗi loại |
| --- | ---: | ---: | ---: | ---: | ---: |
| Dưới 30s | 1,9s | 6 | ×1,10 | 50% | 16,67% |
| 30–dưới 60s | 1,5s | 9 | ×1,20 | 45% | 18,33% |
| 60–dưới 120s | 1,15s | 12 | ×1,40 | 40% | 20% |
| Từ 120s | 0,75s | 15 | ×1,55 | 40% | 20% |

Tỉ lệ trên áp dụng cho **ứng viên trước kiểm tra fairness**. Ứng viên không an toàn bị loại, nên tỉ lệ thực tế có thể khác và khoảng spawn có thể dài hơn. Mỗi lần thử có tối đa 6 ứng viên, khoảng cách spawn tối thiểu tuyệt đối 0,35s. Hazard đang cảnh báo cũng được tính vào giới hạn active.

| Loại | Cảnh báo | Chuyển động / kích thước | Thời gian tồn tại |
| --- | --- | --- | --- |
| Vật rơi | Không có telegraph riêng | Vận tốc đầu 325–375 × hệ số, gia tốc 90–145 × hệ số; kích thước từng vật ×1,20 | Đến khi rời màn hình |
| Laser | 1s | Ngang/dọc 50/50; tia dày 24, phủ toàn chiều ngang/dọc sân | Bắn 0,45s sau cảnh báo |
| Cannon | 1s | Ngang/dọc 50/50; hai hướng bắn 50/50; đạn đường kính 52; tốc độ 340–480 × hệ số | Đến khi đạn rời màn hình |
| Orbital | Không có telegraph riêng | 50% con lắc, 50% quỹ đạo tròn; quả nặng đường kính 52; tần số góc π/0,9 rad/s, chu kỳ 1,8s | 3,5–5s; tối đa 2 orbital active |

Con lắc: dây dài 190–270, góc biên 55–80°. Quỹ đạo tròn: bán kính 120–185. Tốc độ góc orbital cố định, không nhân hệ số độ khó.

Các kiểm tra fairness chính: vùng tránh vật rơi 125 theo X / 110 theo Y; thời gian phản ứng ước tính tối thiểu 0,40s; quỹ đạo orbital phải giữ khoảng hở ít nhất 30 trong dự báo 0,75s; tối đa hai kênh cảnh báo Laser/Cannon đồng thời. Đây là các ràng buộc chọn spawn, không bảo đảm người chơi bất tử khi đứng yên.

Mỗi map có 5 vật rơi, chọn đều 20% trong nhóm vật rơi:

| Map | Vật rơi |
| --- | --- |
| Kitchen | Knife, Frying Pan, Rolling Pin, Salt Shaker, Blender |
| Living Room | Plant Pot, Remote Control, Book, Cushion, Lamp |
| Bedroom | Alarm Clock, Pillow, Slipper, Mirror Shard, Toy Block |
| Garden | Apple, Bee Hive, Trowel, Watermelon, Acorn |
| Rooftop | Brick, Satellite Dish, Antenna, Weather Vane, Lightning |

Từ 60s bắt đầu cơ chế sập platform: đợi thêm 5s để kiểm tra lần đầu, các lần sau cách 7–12s; cảnh báo 1,5s; sập 4s rồi thử tìm vị trí hồi lại có thể tiếp cận; tối đa 2 platform sập. Có thể chậm hơn nếu chưa tìm được vị trí an toàn.

## Gacha hiện tại

Chỉ chọn skin chưa sở hữu, không có trùng. Trọng số **mỗi skin còn lại**: Common 100, Uncommon 40, Rare 20, Legendary 7. Xác suất một rarity bằng trọng số nhân số skin còn lại, chia tổng trọng số.

Với tài khoản mới đã có Ginger và Tuxedo: còn 3 common, 4 uncommon, 3 rare, 3 legendary; tổng trọng số 541. Xác suất theo rarity xấp xỉ **55,45% / 29,57% / 11,09% / 3,88%**. Xác suất được tính lại sau mỗi lần mở khóa.

`spin.wav` dài 11,312s; reel tiến theo vị trí phát thực tế và chỉ hoàn tất khi âm thanh kết thúc. Giữ ô trúng 0,55s, legendary có thêm 1,8s hội tụ/bùng hạt, sau đó hiện box kết quả và phát `congrat.wav` (4,488s). Box tự đóng sau 2s hoặc click; common/uncommon không có pháo hoa, rare có pháo hoa vàng, legendary có hiệu ứng riêng theo skin.
