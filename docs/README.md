# Documentation / Mục lục tài liệu

[English overview](../README.md) · [Tổng quan tiếng Việt](../README_vi.md)

Các hướng dẫn hiện hành bên dưới được đối chiếu với source ngày 28/10/2026. README gốc tóm tắt cách chơi/chạy; mỗi bảng thông số chi tiết có một nơi chính để cập nhật. Các file trong `history/` chỉ lưu bằng chứng thử nghiệm ở thời điểm ghi chép.

| Cần tìm | Đọc tại | Nguồn đối chiếu chính |
| --- | --- | --- |
| Chạy, điều khiển, save, tests | [running-and-testing.md](running-and-testing.md) | Launcher, GameController, InputManager, PersistenceManager, scripts |
| Kiến trúc và vị trí code | [architecture.md](architecture.md) | Java2DApp, GameWorld, FairnessValidator |
| Buff, cá, hazard, platform, điểm | [gameplay-thong-so.md](gameplay-thong-so.md) | ProceduralGenerator, GenerationRules, Player, GameWorld |
| Catalog, giá, xác suất, loadout | [shop-and-loadout.md](shop-and-loadout.md) | ShopManager, GachaSequence, GameController |
| Render và kiểm chứng hiệu năng | [performance.md](performance.md) | FramePacer, DrawContext, AssetManager, desktop probes |
| Portable và bài nộp | [portable-windows.md](portable-windows.md) | build-direct-portable.ps1, tools/portable-direct |
| Ảnh và chữ UI | [Image asset guide](../src/main/resources/assets/images/README.md) | AssetManager, GameRenderer, ArtText |
| Nhạc và SFX | [Audio asset guide](../src/main/resources/assets/audio/README.md) | AudioResources, AudioManager |
| Tạo asset và script build | [Tools](../tools/README.md) | GenerateUiArt, GenerateCountdownArt, hai script portable |
| Prompt title/logo | [Branding record](../src/main/resources/assets/images/ui/BRANDING_PROMPTS.md) | title_dodging_cat.png, team_tadq.png |
| Số đo và đóng gói cũ | [History](history/README.md) | Các báo cáo ghi ngày |

Khi đổi hành vi game, cập nhật hướng dẫn chủ đề tương ứng và phần tóm tắt ở hai README gốc nếu cần. Giữ liên kết tương đối để đọc được trên GitHub, trong clone và trong ZIP. Thư mục `target/` chứa output kiểm thử có thể xóa/tạo lại; không dùng nó làm vị trí tài liệu chính. Các Markdown dưới `tools/portable-direct/licenses/` là thông báo pháp lý của thư viện, không phải hướng dẫn gameplay.
