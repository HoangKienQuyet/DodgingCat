package com.dodgingcat.shop;

import javafx.scene.paint.Color;

/**
 * Cosmetic skin and map rarities.
 */
public enum SkinRarity {
    COMMON("COMMON", "Thường", Color.web("#B0BEC5")),
    UNCOMMON("UNCOMMON", "Đặc Trưng", Color.web("#66BB6A")),
    RARE("RARE", "Hiếm", Color.web("#42A5F5")),
    LEGENDARY("LEGENDARY", "Huyền Thoại", Color.web("#FFB300"));

    private final String enName;
    private final String viName;
    private final Color color;

    SkinRarity(String enName, String viName, Color color) {
        this.enName = enName;
        this.viName = viName;
        this.color = color;
    }

    public String getDisplayName(boolean isVi) {
        return isVi ? viName : enName;
    }

    public Color getColor() {
        return color;
    }
}
