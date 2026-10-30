package com.dodgingcat.ui;

import javafx.scene.paint.Color;

/**
 * Visual styling and atmospheric themes for each of the 5 game environments.
 */
public enum Theme {
    KITCHEN("kitchen", "Kitchen", "Nhà Bếp",
            Color.web("#FFF3E0"), Color.web("#FFE0B2"),
            Color.web("#8D6E63"), Color.web("#FFB74D"),
            Color.web("#E65100"), Color.web("#D7CCC8")),

    LIVING_ROOM("living_room", "Living Room", "Phòng Khách",
            Color.web("#EDE7F6"), Color.web("#D1C4E9"),
            Color.web("#5D4037"), Color.web("#9575CD"),
            Color.web("#512DA8"), Color.web("#D1C4E9")),

    BEDROOM("bedroom", "Bedroom", "Phòng Ngủ",
            Color.web("#E8EAF6"), Color.web("#C5CAE9"),
            Color.web("#3949AB"), Color.web("#7986CB"),
            Color.web("#1A237E"), Color.web("#C5CAE9")),

    GARDEN("garden", "Garden", "Sân Vườn",
            Color.web("#E8F5E9"), Color.web("#C8E6C9"),
            Color.web("#2E7D32"), Color.web("#81C784"),
            Color.web("#1B5E20"), Color.web("#A5D6A7")),

    ROOFTOP("rooftop", "Rooftop", "Mái Nhà",
            Color.web("#1A237E"), Color.web("#121212"),
            Color.web("#37474F"), Color.web("#546E7A"),
            Color.web("#ECEFF1"), Color.web("#90A4AE"));

    private final String id;
    private final String nameEn;
    private final String nameVi;
    private final Color skyTop;
    private final Color skyBottom;
    private final Color floorColor;
    private final Color platformColor;
    private final Color accentColor;
    private final Color particleColor;

    Theme(String id, String nameEn, String nameVi, Color skyTop, Color skyBottom, Color floorColor, Color platformColor, Color accentColor, Color particleColor) {
        this.id = id;
        this.nameEn = nameEn;
        this.nameVi = nameVi;
        this.skyTop = skyTop;
        this.skyBottom = skyBottom;
        this.floorColor = floorColor;
        this.platformColor = platformColor;
        this.accentColor = accentColor;
        this.particleColor = particleColor;
    }

    public static Theme fromId(String id) {
        if (id == null) return KITCHEN;
        for (Theme t : values()) {
            if (t.id.equalsIgnoreCase(id)) return t;
        }
        return KITCHEN;
    }

    public String getId() { return id; }
    public String getDisplayName(boolean isVi) { return isVi ? nameVi : nameEn; }
    public Color getSkyTop() { return skyTop; }
    public Color getSkyBottom() { return skyBottom; }
    public Color getFloorColor() { return floorColor; }
    public Color getPlatformColor() { return platformColor; }
    public Color getAccentColor() { return accentColor; }
    public Color getParticleColor() { return particleColor; }
}
