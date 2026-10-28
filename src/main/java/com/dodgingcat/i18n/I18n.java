package com.dodgingcat.i18n;

import java.util.HashMap;
import java.util.Map;

/**
 * Localization dictionary supporting English and Vietnamese.
 */
public class I18n {
    private static Language currentLanguage = Language.EN;
    private static final Map<String, String> EN = new HashMap<>();
    private static final Map<String, String> VI = new HashMap<>();

    static {
        // Main Menu
        put("menu.single_player", "Single Player", "Chơi Đơn");
        put("menu.local_coop", "Local Co-op", "Chơi Đôi (Co-op)");
        put("menu.shop", "Shop", "Cửa Hàng");
        put("menu.leaderboard", "Leaderboard", "Bảng Xếp Hạng");
        put("menu.settings", "Settings", "Cài Đặt");
        put("menu.help", "Help & Controls", "Hướng Dẫn & Phím");
        put("menu.quit", "Quit Game", "Thoát Game");
        put("menu.resume", "▶ RESUME GAME", "▶ TIẾP TỤC");

        // Pre-game Setup
        put("pregame.title", "CHOOSE YOUR LOADOUT", "CHỌN BẢN ĐỒ & TRANG PHỤC");
        put("pregame.map_tab", "Select Map", "Chọn Bản Đồ");
        put("pregame.cat_skin_tab", "Cat Skin", "Trang Phục Mèo");
        put("pregame.p1_skin_tab", "P1 Cat Skin", "Trang Phục P1");
        put("pregame.p2_skin_tab", "P2 Cat Skin", "Trang Phục P2");
        put("pregame.p1_controls", "P1: WASD move · Space select / cancel", "P1: WASD di chuyển · Space chọn / hủy");
        put("pregame.p2_controls", "P2: Arrows move · Enter select / cancel", "P2: Phím mũi tên di chuyển · Enter chọn / hủy");
        put("pregame.p1_ready", "P1 READY · Space to cancel", "P1 ĐÃ CHỌN · Space để hủy");
        put("pregame.p2_ready", "P2 READY · Enter to cancel", "P2 ĐÃ CHỌN · Enter để hủy");
        put("pregame.start", "START SURVIVAL", "BẮT ĐẦU SINH TỒN");
        put("pregame.back", "Back to Menu", "Quay Lại");

        // HUD & Gameplay
        put("hud.time", "TIME", "THỜI GIAN");
        put("hud.score", "SCORE", "ĐIỂM");
        put("hud.fishes", "FISHES", "CÁ");
        put("hud.best", "BEST", "KỶ LỤC");

        // Game Over & Titles
        put("gameover.title", "GAME OVER", "GAME OVER");
        put("gameover.survival_time", "Survival Time", "Thời Gian Sống");
        put("gameover.score", "Final Score", "Điểm Cuối Cùng");
        put("gameover.fishes", "Fishes Collected", "Số Cá Nhặt Được");
        put("gameover.new_record", "★ NEW LEADERBOARD ENTRY · RANK #%d ★", "★ VÀO BẢNG XẾP HẠNG · HẠNG #%d ★");
        put("gameover.retry", "Play Again", "Chơi Lại");
        put("gameover.menu", "Main Menu", "Màn Hình Chính");

        // Survival Titles
        put("title.noob", "Noob Kitten", "Mèo Mới Vào Nghề");
        put("title.quick", "Quick Paws", "Mèo Nhanh Nhẹn");
        put("title.acrobatic", "Acrobatic Feline", "Bậc Thầy Nhào Lộn");
        put("title.shadow", "Shadow Dodger", "Bóng Ma Né Đòn");
        put("title.king", "King of Dodge", "Vua Sinh Tồn");
        put("title.coop.noob", "Noob Kittens", "Những Chú Mèo Mới Vào Nghề");
        put("title.coop.quick", "Quick Paws", "Những Chú Mèo Nhanh Nhẹn");
        put("title.coop.acrobatic", "Acrobatic Felines", "Những Bậc Thầy Nhào Lộn");
        put("title.coop.shadow", "Shadow Dodgers", "Những Bóng Ma Né Đòn");
        put("title.coop.king", "Kings of Dodge", "Những Vị Vua Sinh Tồn");

        // Pause
        put("pause.title", "PAUSED", "TẠM DỪNG");
        put("pause.resume", "Resume", "Tiếp Tục");
        put("pause.restart", "Restart", "Chơi Lại");
        put("pause.menu", "Return to Main Menu", "Về Màn Hình Chính");

        // Shop
        put("shop.title", "FISH SHOP", "CỬA HÀNG CÁ");
        put("shop.skins_tab", "Cat Skins", "Trang Phục Mèo");
        put("shop.maps_tab", "Map Themes", "Bản Đồ");
        put("shop.balance", "Your Fishes", "Số Cá Hiện Có");
        put("shop.buy", "Buy", "Mua");
        put("shop.owned", "OWNED", "ĐÃ SỞ HỮU");
        put("shop.not_enough", "Not enough fishes!", "Không đủ cá!");
        put("shop.page_prev", "◀ Prev", "◀ Trang Trước");
        put("shop.page_next", "Next ▶", "Trang Sau ▶");
        put("shop.page_info", "Page %d / %d", "Trang %d / %d");
        put("shop.back", "Back", "Quay Lại");

        // Settings
        put("settings.title", "SETTINGS & AUDIO", "CÀI ĐẶT & ÂM THANH");
        put("settings.bgm", "Music Volume", "Âm Lượng Nhạc (BGM)");
        put("settings.sfx", "Sound FX Volume", "Âm Lượng Hiệu Ứng (SFX)");
        put("settings.language", "Language", "Ngôn Ngữ");
        put("settings.fullscreen", "Toggle Fullscreen (F11)", "Toàn Màn Hình (F11)");
        put("settings.back", "Back", "Quay Lại");

        // Rarity
        put("rarity.common", "COMMON", "THƯỜNG");
        put("rarity.uncommon", "UNCOMMON", "ĐẶC TRƯNG");
        put("rarity.rare", "RARE", "HIẾM");
        put("rarity.legendary", "LEGENDARY", "HUYỀN THOẠI");
    }

    private static void put(String key, String en, String vi) {
        EN.put(key, en);
        VI.put(key, vi);
    }

    public static String get(String key) {
        Map<String, String> dict = (currentLanguage == Language.VI) ? VI : EN;
        return dict.getOrDefault(key, key);
    }

    public static Language getLanguage() {
        return currentLanguage;
    }

    public static void setLanguage(Language lang) {
        currentLanguage = lang;
    }

    public static void toggleLanguage() {
        currentLanguage = (currentLanguage == Language.EN) ? Language.VI : Language.EN;
    }

    public static String getTitleByTime(double seconds) {
        if (seconds < 20.0) {
            return get("title.noob");
        } else if (seconds < 60.0) {
            return get("title.quick");
        } else if (seconds < 120.0) {
            return get("title.acrobatic");
        } else if (seconds < 200.0) {
            return get("title.shadow");
        } else {
            return get("title.king");
        }
    }

    public static String getCoopTitleByTime(double seconds) {
        if (seconds < 20.0) {
            return get("title.coop.noob");
        } else if (seconds < 60.0) {
            return get("title.coop.quick");
        } else if (seconds < 120.0) {
            return get("title.coop.acrobatic");
        } else if (seconds < 200.0) {
            return get("title.coop.shadow");
        } else {
            return get("title.coop.king");
        }
    }
}
