package com.dodgingcat.ui;

import com.dodgingcat.game.GameWorld;
import com.dodgingcat.game.GameWorld.BuffKind;
import com.dodgingcat.game.GameWorld.HazardKind;
import com.dodgingcat.game.GameWorld.Mode;
import com.dodgingcat.game.GameWorld.PickupNotice;
import com.dodgingcat.game.GameWorld.Platform;
import com.dodgingcat.game.Hazard;
import com.dodgingcat.game.Pickup;
import com.dodgingcat.game.Player;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Code-drawn garden and kitchen artwork. All coordinates use the world's 960 by 640 canvas. */
public final class GameRenderer {
    private static final Color INK = Color.web("#183C3A");
    private static final Color WOOD = Color.web("#BC8758");
    private static final Color GRASS = Color.web("#97C27A");
    private static final Color CREAM = Color.web("#FFF1CA");
    private static final Color AMBER = Color.web("#FFD188");
    private static final Color CYAN = Color.web("#8DE3DE");
    private static final Color DANGER = Color.web("#FF806C");
    private static final Font CONTROL_FONT = Font.font("Segoe UI", FontWeight.SEMI_BOLD, 12);
    private static final Font TOKEN_FONT = Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 13);
    private static final Font PLAYER_FONT = Font.font("Segoe UI", FontWeight.BOLD, 10);
    private static final Font BUFF_CAPTION_FONT = Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 7);
    private static final Font NOTICE_FONT = Font.font("Segoe UI", FontWeight.EXTRA_BOLD, 15);
    private final Map<Long, NoticeLayout> noticeLayouts = new HashMap<>();
    private GameWorld noticeWorld;
    private GameWorld animationWorld;
    private double animationTime = -1;
    private final Map<Integer, CatMotion> catMotions = new HashMap<>();

    private static final class CatMotion {
        boolean grounded;
        double landedAt = -10;
        CatMotion(boolean grounded) { this.grounded = grounded; }
    }

    private static final CatPalette[] CATS = {
        new CatPalette("#644332", "#F4AA61", "#F8CA85", "#D98246", "#FFE7B3"),
        new CatPalette("#75624B", "#EAD9AE", "#FFF0CC", "#C9B489", "#FFF5DC"),
        new CatPalette("#24383E", "#667E87", "#93A8AB", "#435A65", "#D5E2DC"),
        new CatPalette("#634A61", "#BE98BA", "#DEC1D5", "#98739A", "#FBE3E4")
    };

    public GameRenderer() {
    }

    public void render(GraphicsContext g, GameWorld world, double visualTime, int skinIndex) {
        double time = world.state() == GameWorld.State.MENU ? visualTime : world.elapsed();
        if (animationWorld != world || world.elapsed() < animationTime) {
            catMotions.clear(); animationWorld = world;
        }
        animationTime = world.elapsed();
        boolean kitchen = world.mapTheme() == GameWorld.MapTheme.KITCHEN;
        g.save();
        g.setGlobalAlpha(1);
        g.setLineDashes();
        if (kitchen) drawKitchenBackground(g); else drawBackground(g, time);
        for (Platform platform : world.platforms()) {
            if (kitchen) drawKitchenPlatform(g, platform); else drawPlatform(g, platform);
        }
        if (kitchen) drawKitchenFloor(g); else drawFloor(g);
        for (Hazard hazard : world.hazards()) {
            if (hazard.kind() == HazardKind.LASER && !hazard.active()) {
                drawLaserWarning(g, hazard, time);
            }
        }
        for (Pickup pickup : world.pickups()) {
            drawPickup(g, pickup, time);
        }
        for (Hazard hazard : world.hazards()) {
            // Knives and darts are visible from their first frame, even for an old saved fixture.
            if (hazard.kind() != HazardKind.LASER || hazard.active()) {
                drawHazard(g, hazard, time);
            }
        }
        boolean versus = world.mode() == Mode.VERSUS;
        for (Player player : world.players()) {
            if (!player.alive()) drawEliminatedPlayer(g, player, playerSkin(player, skinIndex), versus);
        }
        for (Player player : world.players()) {
            if (player.alive()) drawPlayer(g, player, time, playerSkin(player, skinIndex), versus);
        }
        drawPickupNotices(g, world, versus);
        drawControls(g, versus);
        g.restore();
    }

    private int playerSkin(Player player, int selectedSkin) {
        return Math.floorMod(selectedSkin + (player.id() == 2 ? 1 : 0), 3);
    }

    private void drawPickupNotices(GraphicsContext g, GameWorld world, boolean versus) {
        if (noticeWorld != world) {
            noticeLayouts.clear();
            noticeWorld = world;
        }
        if (world.state() != GameWorld.State.PLAYING && world.state() != GameWorld.State.PAUSED) {
            noticeLayouts.clear();
            return;
        }
        Set<Long> activeIds = new HashSet<>();
        for (PickupNotice notice : world.pickupNotices()) activeIds.add(notice.id());
        noticeLayouts.keySet().retainAll(activeIds);
        g.save();
        g.setFont(NOTICE_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setLineJoin(StrokeLineJoin.ROUND);
        for (PickupNotice notice : world.pickupNotices()) {
            double age = Math.max(0, world.elapsed() - notice.bornAt());
            if (age >= GameWorld.PICKUP_NOTICE_SECONDS) continue;
            NoticeLayout layout = noticeLayouts.get(notice.id());
            if (layout == null || !layout.notice().equals(notice)) {
                layout = layoutNotice(notice, world.elapsed(), versus);
                noticeLayouts.put(notice.id(), layout);
            }
            double progress = age / GameWorld.PICKUP_NOTICE_SECONDS;
            double opacity = Math.min(1, (1 - progress) / .35);
            double y = noticeY(layout, world.elapsed());
            g.setGlobalAlpha(opacity);
            g.setStroke(Color.web("#071E20", .45));
            g.setLineWidth(5);
            g.strokeText(layout.text(), layout.x() + 1, y + 2, layout.width());
            g.setStroke(Color.web("#173A36", .97));
            g.setLineWidth(3);
            g.strokeText(layout.text(), layout.x(), y, layout.width());
            g.setFill(noticeColor(notice.kind()));
            g.fillText(layout.text(), layout.x(), y, layout.width());
        }
        g.restore();
    }

    private NoticeLayout layoutNotice(PickupNotice notice, double elapsed, boolean versus) {
        String text = (versus ? "P" + notice.playerId() + " · " : "") + notice.text();
        Text measure = new Text(text);
        measure.setFont(NOTICE_FONT);
        double width = Math.min(GameWorld.WIDTH - 28, Math.ceil(measure.getLayoutBounds().getWidth()) + 2);
        double x = Math.max(width / 2 + 12, Math.min(GameWorld.WIDTH - width / 2 - 12, notice.x()));
        double originY = Math.max(92, Math.min(GameWorld.FLOOR_Y - 10, notice.y() - 14));
        NoticeLayout fallback = new NoticeLayout(notice, text, x, originY, width);
        // Allocate once per immutable notice. Older labels expiring never reflow newer labels.
        for (int direction : new int[]{-1, 1}) {
            for (int lane = direction == -1 ? 0 : 1; lane < 24; lane++) {
                double baseY = originY + direction * lane * 25;
                if (baseY < 92 || baseY > GameWorld.FLOOR_Y - 10) break;
                NoticeLayout candidate = new NoticeLayout(notice, text, x, baseY, width);
                if (!overlapsNotice(candidate, elapsed)) return candidate;
            }
        }
        return fallback;
    }

    private boolean overlapsNotice(NoticeLayout candidate, double elapsed) {
        for (NoticeLayout existing : noticeLayouts.values()) {
            if (existing.notice().id() == candidate.notice().id()) continue;
            if (Math.abs(existing.x() - candidate.x()) < (existing.width() + candidate.width()) / 2 + 8
                    && Math.abs(noticeY(existing, elapsed) - noticeY(candidate, elapsed)) < 24) return true;
        }
        return false;
    }

    private double noticeY(NoticeLayout layout, double elapsed) {
        double progress = Math.max(0, Math.min(1,
                (elapsed - layout.notice().bornAt()) / GameWorld.PICKUP_NOTICE_SECONDS));
        return Math.max(92, layout.baseY() - 36 * progress);
    }

    private Color noticeColor(BuffKind kind) {
        if (kind == null) return AMBER;
        return switch (kind) {
            case SHIELD -> CYAN;
            case INVINCIBILITY -> Color.web("#E1C7FF");
            case DOUBLE_FISH -> Color.web("#FFE39A");
            case BOMB -> DANGER;
        };
    }

    private record NoticeLayout(PickupNotice notice, String text, double x, double baseY, double width) { }

    /** A small code-drawn cat suitable for menus and the skin selector. */
    public void renderPreview(GraphicsContext g, double centerX, double baselineY,
                              double scale, int skinIndex, double visualTime) {
        g.save();
        g.translate(centerX, baselineY);
        g.scale(scale, scale);
        drawCat(g, skinIndex, visualTime, false, true, false);
        g.restore();
    }

    private void drawBackground(GraphicsContext g, double t) {
        g.setFill(new LinearGradient(0, 0, 0, 640, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#416B63")),
                new Stop(.55, Color.web("#294F4B")),
                new Stop(1, Color.web("#163C3B"))));
        g.fillRect(0, 0, 960, 640);

        // A greenhouse window makes a quiet frame around the play area.
        g.setFill(Color.web("#789183", .10));
        g.fillRoundRect(84, -70, 792, 620, 250, 250);
        g.setStroke(Color.web("#A9C5A0", .12));
        g.setLineWidth(8);
        g.strokeRoundRect(84, -70, 792, 620, 250, 250);
        g.setLineWidth(3);
        for (int i = 0; i < 6; i++) {
            double x = 120 + i * 144;
            g.strokeLine(x, 40, x, 544);
        }
        for (int y = 152; y < 550; y += 128) {
            g.strokeLine(93, y, 867, y);
        }
        g.setFill(new LinearGradient(110, 55, 630, 530, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#FAE8AD", .075)),
                new Stop(1, Color.web("#FAE8AD", 0))));
        g.fillPolygon(new double[]{110, 245, 710, 490},
                new double[]{0, 0, 576, 576}, 4);

        // Distant foliage remains subdued so that threats read immediately.
        drawDistantPlant(g, 70, 574, .95, false);
        drawDistantPlant(g, 890, 574, 1.12, true);
        drawDistantPlant(g, 337, 574, .70, false);
        drawDistantPlant(g, 615, 574, .70, true);
        drawHangingPlant(g, 234, 81, t, false);
        drawHangingPlant(g, 726, 81, t, true);

        drawPost(g, 42);
        drawPost(g, 898);
        drawLantern(g, 65, 172, t);
        drawLantern(g, 895, 172, t + 1.7);
        drawSideVine(g, 25, false);
        drawSideVine(g, 935, true);

        for (int i = 0; i < 23; i++) {
            double px = 82 + ((i * 173) % 800) + Math.sin(t * .24 + i * 2.1) * 9;
            double py = 92 + ((i * 97 + 31) % 454) + Math.sin(t * .35 + i) * 11;
            double pulse = .14 + .12 * (.5 + .5 * Math.sin(t * .8 + i));
            g.setFill(Color.web("#FFF0B0", pulse));
            g.fillRect(Math.rint(px), Math.rint(py), i % 4 == 0 ? 3 : 2, 2);
        }
    }

    private void drawKitchenBackground(GraphicsContext g) {
        g.setFill(new LinearGradient(0, 0, 0, 576, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3E535A")), new Stop(1, Color.web("#263D43"))));
        g.fillRect(0, 0, 960, 640);
        // Quiet tiled backsplash behind the brightly outlined traps and cats.
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 12; col++) {
                g.setFill(Color.web((row + col) % 2 == 0 ? "#466065" : "#40595E", .65));
                g.fillRoundRect(col * 80 + 2, row * 74 + 2, 76, 70, 5, 5);
            }
        }
        g.setStroke(Color.web("#83958A", .35)); g.setLineWidth(3);
        g.strokeLine(46, 146, 914, 146);
        // Wall cupboards and a central extractor hood establish the room silhouette.
        for (int x : new int[]{75, 685}) {
            g.setFill(Color.web("#806952")); g.fillRoundRect(x, 82, 200, 112, 8, 8);
            for (int door = 0; door < 2; door++) {
                g.setFill(Color.web("#9A8061")); g.fillRoundRect(x + 7 + door * 98, 90, 88, 96, 5, 5);
                g.setFill(Color.web("#C2AE85")); g.fillRect(x + 78 + door * 22, 130, 4, 19);
            }
        }
        g.setFill(Color.web("#6B7F7C")); g.fillRect(443, 78, 74, 53);
        g.fillPolygon(new double[]{443, 517, 554, 406}, new double[]{119, 119, 158, 158}, 4);
        g.setFill(Color.web("#A1B0A0")); g.fillRoundRect(404, 157, 152, 12, 4, 4);
        g.setFill(Color.web("#213A40"));
        for (int i = 0; i < 7; i++) g.fillRect(422 + 17 * i, 160, 9, 3);
        // Hanging utensils are muted decor, placed at the sides outside the central action.
        for (int x : new int[]{43, 905}) {
            g.setStroke(Color.web("#A0ADA0", .5)); g.setLineWidth(4);
            g.strokeLine(x, 220, x, 273);
            g.setFill(Color.web("#8D9D94", .45)); g.fillOval(x - 11, 270, 22, 29);
            g.setStroke(Color.web("#1F353C", .7)); g.setLineWidth(6);
            g.strokeLine(x + (x < 100 ? 27 : -27), 330, x + (x < 100 ? 27 : -27), 354);
            g.setFill(Color.web("#263F47", .8)); g.fillOval(x - 12, 352, 45, 45);
            g.setStroke(Color.web("#8C9A8C", .35)); g.setLineWidth(3); g.strokeOval(x - 8, 356, 37, 37);
        }
        g.setFill(Color.web("#142F37", .34)); g.fillRect(0, 510, 960, 66);
    }

    private void drawKitchenPlatform(GraphicsContext g, Platform p) {
        g.setFill(Color.web("#10282E", .35)); g.fillRoundRect(p.x() - 3, p.y() + 7, p.width() + 6, 22, 5, 5);
        g.setFill(Color.web("#233A42")); g.fillRoundRect(p.x() - 2, p.y(), p.width() + 4, 19, 4, 4);
        g.setFill(Color.web("#889C98")); g.fillRect(p.x(), p.y(), p.width(), 8);
        g.setFill(Color.web("#D5DDC9")); g.fillRect(p.x(), p.y(), p.width(), 3);
        g.setFill(Color.web("#576D70")); g.fillRect(p.x() + 2, p.y() + 8, p.width() - 4, 7);
        g.setFill(Color.web("#314A51"));
        g.fillRect(p.x() + 20, p.y() + 18, 9, 17);
        g.fillRect(p.x() + p.width() - 29, p.y() + 18, 9, 17);
    }

    private void drawKitchenFloor(GraphicsContext g) {
        g.setFill(Color.web("#1A3037")); g.fillRect(0, GameWorld.FLOOR_Y, 960, 64);
        g.setFill(Color.web("#B5BBA5")); g.fillRect(0, GameWorld.FLOOR_Y, 960, 5);
        g.setFill(Color.web("#657F7C")); g.fillRect(0, 581, 960, 10);
        for (int x = 0; x < 960; x += 120) {
            g.setFill(Color.web("#6C624F")); g.fillRoundRect(x + 3, 593, 114, 43, 4, 4);
            g.setFill(Color.web("#B2A27B")); g.fillRoundRect(x + 43, 596, 34, 3, 3, 3);
        }
    }

    private void drawPost(GraphicsContext g, double x) {
        g.setFill(Color.web("#173B36", .7));
        g.fillRect(x - 4, 0, 26, 576);
        g.setFill(Color.web("#567264", .7));
        g.fillRect(x, 0, 14, 576);
        g.setFill(Color.web("#A9B68C", .15));
        g.fillRect(x + 2, 0, 3, 576);
        g.setFill(Color.web("#203D35"));
        g.fillRect(x - 4, 222, 23, 6);
        g.fillRect(x - 4, 448, 23, 6);
    }

    private void drawDistantPlant(GraphicsContext g, double x, double y, double scale,
                                  boolean mirror) {
        g.save();
        g.translate(x, y);
        g.scale(mirror ? -scale : scale, scale);
        g.setStroke(Color.web("#4C7561", .38));
        g.setLineWidth(5);
        g.strokeLine(0, 0, 3, -142);
        for (int i = 0; i < 5; i++) {
            double ly = -21 - i * 25;
            g.setFill(Color.web(i % 2 == 0 ? "#56866D" : "#729478", .24));
            g.fillPolygon(new double[]{0, -36, -53, -25},
                    new double[]{ly, ly - 30, ly - 33, ly + 1}, 4);
            g.fillPolygon(new double[]{2, 30, 48, 23},
                    new double[]{ly - 9, ly - 38, ly - 38, ly - 4}, 4);
        }
        g.restore();
    }

    private void drawHangingPlant(GraphicsContext g, double x, double y, double t,
                                  boolean mirror) {
        g.save();
        g.translate(x + Math.sin(t * .6 + (mirror ? 1 : 0)) * 1.3, y);
        g.setStroke(Color.web("#A5BA95", .4));
        g.setLineWidth(2);
        g.strokeLine(-17, 57, 0, -55);
        g.strokeLine(17, 57, 0, -55);
        g.setFill(Color.web("#173E36", .45));
        g.fillRect(-24, 58, 48, 9);
        g.setFill(Color.web("#9A7752", .48));
        g.fillPolygon(new double[]{-23, 23, 16, -16}, new double[]{54, 54, 76, 76}, 4);
        g.setFill(Color.web("#BCC18B", .35));
        g.fillRect(-24, 51, 48, 6);
        g.setStroke(Color.web("#588369", .8));
        g.setLineWidth(3);
        curve(g, -18, 54, -32, 83, -8, 95, -24, 120);
        curve(g, 13, 55, 31, 88, 8, 109, 24, 140);
        for (int i = 0; i < 6; i++) {
            double ly = 64 + i * 12;
            g.setFill(Color.web(i % 2 == 0 ? "#82A77B" : "#638D6D", .7));
            g.fillPolygon(new double[]{-21, -34, -31, -19},
                    new double[]{ly, ly - 7, ly + 6, ly + 8}, 4);
            g.fillPolygon(new double[]{19, 32, 30, 16},
                    new double[]{ly + 7, ly, ly + 13, ly + 15}, 4);
        }
        g.restore();
    }

    private void drawSideVine(GraphicsContext g, double x, boolean mirror) {
        g.save();
        g.translate(x, 0);
        g.scale(mirror ? -1 : 1, 1);
        g.setStroke(Color.web("#2D604C"));
        g.setLineWidth(4);
        curve(g, 0, 0, 28, 160, -12, 292, 8, 468);
        for (int i = 0; i < 10; i++) {
            double y = 36 + i * 44;
            double offset = Math.sin(i * .9) * 7;
            g.setFill(Color.web(i % 3 == 0 ? "#799862" : "#47795A"));
            g.fillPolygon(new double[]{offset + 6, offset + 25, offset + 28, offset + 12},
                    new double[]{y, y - 13, y + 2, y + 11}, 4);
            g.setFill(Color.web("#355F48"));
            g.fillPolygon(new double[]{offset + 3, offset - 10, offset - 16, offset - 2},
                    new double[]{y + 13, y + 1, y + 13, y + 25}, 4);
        }
        g.restore();
    }

    private void drawLantern(GraphicsContext g, double x, double y, double t) {
        g.setFill(Color.web("#F8CA7B", .035 + Math.sin(t * 1.3) * .007));
        g.fillOval(x - 45, y - 43, 90, 90);
        g.setStroke(Color.web("#203C35"));
        g.setLineWidth(3);
        g.strokeLine(x, y - 51, x, y - 18);
        g.setFill(Color.web("#203C35"));
        g.fillRect(x - 12, y - 18, 24, 32);
        g.fillRect(x - 15, y - 19, 30, 5);
        g.fillRect(x - 15, y + 12, 30, 5);
        g.setFill(Color.web("#E4B875"));
        g.fillRect(x - 8, y - 13, 16, 24);
        g.setFill(Color.web("#FFDB91"));
        g.fillRect(x - 3, y - 10, 6, 18);
    }

    private void drawPlatform(GraphicsContext g, Platform p) {
        double x = p.x();
        double y = p.y();
        double w = p.width();
        g.setFill(Color.web("#0E302E", .26));
        g.fillRoundRect(x - 3, y + 8, w + 6, 22, 8, 8);
        g.setFill(INK);
        g.fillRect(x - 2, y + 2, w + 4, 20);
        g.setFill(WOOD);
        g.fillRect(x, y + 5, w, 13);
        g.setFill(Color.web("#D2A36B"));
        g.fillRect(x + 2, y + 5, w - 4, 3);
        g.setFill(Color.web("#916545"));
        g.fillRect(x, y + 15, w, 4);
        for (double sx = x + 31; sx < x + w - 8; sx += 38) {
            g.setFill(Color.web("#906746", .5));
            g.fillRect(sx, y + 9, 2, 7);
        }
        g.setFill(Color.web("#3D704B"));
        g.fillRect(x - 2, y - 1, w + 4, 6);
        g.setFill(GRASS);
        g.fillRect(x, y - 2, w, 4);
        g.setFill(Color.web("#C7D795"));
        g.fillRect(x + 3, y - 2, w - 6, 1);
        for (int i = 0; i < 4; i++) {
            double gx = x + 13 + i * (w - 26) / 3;
            g.setFill(i % 2 == 0 ? GRASS : Color.web("#7FAF69"));
            g.fillPolygon(new double[]{gx, gx + 2, gx + 5, gx + 7, gx + 9},
                    new double[]{y, y - 6, y - 2, y - 5, y}, 5);
        }
        g.setFill(Color.web("#635B43"));
        g.fillRect(x + 8, y + 10, 3, 3);
        g.fillRect(x + w - 11, y + 10, 3, 3);
        g.setFill(Color.web("#E5B87B"));
        g.fillRect(x + 8, y + 10, 2, 1);
        g.fillRect(x + w - 11, y + 10, 2, 1);
    }

    private void drawFloor(GraphicsContext g) {
        g.setFill(Color.web("#132F2F"));
        g.fillRect(0, GameWorld.FLOOR_Y, 960, 64);
        g.setFill(Color.web("#345044"));
        g.fillRect(0, 580, 960, 16);
        g.setFill(Color.web("#6E8160"));
        g.fillRect(0, 579, 960, 3);
        for (int i = 0; i < 21; i++) {
            double x = i * 48;
            g.setFill(Color.web(i % 3 == 0 ? "#50684E" : "#425C49"));
            g.fillRect(x + 1, 584, 45, 9);
            g.setFill(Color.web("#718468", .22));
            g.fillRect(x + 4, 584, 35, 1);
        }
        g.setFill(Color.web("#3D704B"));
        g.fillRect(0, 573, 960, 7);
        g.setFill(GRASS);
        g.fillRect(0, 573, 960, 3);
        g.setFill(Color.web("#B8D28B"));
        g.fillRect(0, 573, 960, 1);
        for (int i = 0; i < 39; i++) {
            double x = i * 27 + (i % 2) * 6;
            double height = 3 + i % 4;
            g.setFill(i % 2 == 0 ? GRASS : Color.web("#7CAF69"));
            g.fillPolygon(new double[]{x, x + 2, x + 5, x + 7},
                    new double[]{574, 574 - height, 571, 575}, 4);
        }
    }

    private void drawControls(GraphicsContext g, boolean versus) {
        g.setFont(CONTROL_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        if (versus) {
            drawControlChip(g, 22, 424, "P1   A D  ·  W / SPACE  JUMP  ·  S  DROP", AMBER);
            drawControlChip(g, 456, 368, "P2   ← →  ·  ↑  JUMP  ·  ↓  DROP", CYAN);
            drawControlChip(g, 834, 104, "ESC  PAUSE", Color.web("#ABC0AA"));
            return;
        }
        drawControlChip(g, 73, 814,
                "A D / ← →  MOVE   ·   SPACE / ↑  JUMP   ·   S / ↓  DROP   ·   ESC  PAUSE",
                Color.web("#ABC0AA"));
    }

    private void drawControlChip(GraphicsContext g, double x, double width, String text, Color color) {
        g.setFill(Color.web("#0E2829", .55));
        g.fillRoundRect(x, 603, width, 26, 12, 12);
        g.setFill(color);
        g.fillText(text, x + width / 2, 616, width - 16);
    }

    private void drawPickup(GraphicsContext g, Pickup p, double t) {
        double y = p.y() + Math.sin(t * 3.2 + p.x() * .03) * 3;
        if (p.kind() == null) {
            g.setFill(Color.web("#F8D58B", .09));
            g.fillOval(p.x() - 21, y - 20, 42, 40);
            drawFish(g, p.x(), y, 1);
            double sparkle = Math.sin(t * 3 + p.x());
            if (sparkle > .5) {
                drawSparkle(g, p.x() + 13, y - 12, 3, Color.web("#FFE9B6", sparkle * .75));
            }
            return;
        }
        Color color = switch (p.kind()) {
            case SHIELD -> CYAN;
            case INVINCIBILITY -> Color.web("#DAC1FF");
            case DOUBLE_FISH -> AMBER;
            case BOMB -> DANGER;
        };
        g.setFill(color.deriveColor(0, 1, 1, .11));
        g.fillOval(p.x() - 24, y - 24, 48, 48);
        g.setFill(INK);
        g.fillOval(p.x() - 15, y - 15, 30, 30);
        g.setFill(color);
        g.fillOval(p.x() - 12, y - 12, 24, 24);
        g.setFill(color.interpolate(CREAM, .42));
        g.fillArc(p.x() - 10, y - 10, 20, 20, 20, 130, javafx.scene.shape.ArcType.OPEN);
        drawBuffIcon(g, p.kind(), p.x(), y);
    }

    private void drawFish(GraphicsContext g, double x, double y, double scale) {
        g.save();
        g.translate(Math.rint(x), Math.rint(y));
        g.scale(scale, scale);
        g.setFill(INK);
        g.fillPolygon(new double[]{-6, -16, -16, -6}, new double[]{-3, -9, 9, 3}, 4);
        g.fillRect(-7, -8, 18, 16);
        g.fillRect(10, -5, 4, 10);
        g.setFill(Color.web("#EAA35C"));
        g.fillPolygon(new double[]{-7, -13, -13, -7}, new double[]{-1, -5, 5, 2}, 4);
        g.setFill(Color.web("#F5CB80"));
        g.fillRect(-5, -6, 15, 12);
        g.fillRect(10, -3, 2, 6);
        g.setFill(CREAM);
        g.fillRect(-3, -5, 10, 3);
        g.setFill(Color.web("#C8874B"));
        g.fillRect(-1, 3, 5, 2);
        g.fillRect(-1, -10, 5, 3);
        g.setFill(INK);
        g.fillRect(7, -2, 2, 2);
        g.restore();
    }

    private void drawBuffIcon(GraphicsContext g, BuffKind kind, double x, double y) {
        g.setFill(INK);
        switch (kind) {
            case SHIELD -> {
                g.fillPolygon(new double[]{x - 7, x, x + 7, x + 5, x, x - 5},
                        new double[]{y - 6, y - 8, y - 6, y + 3, y + 8, y + 3}, 6);
                g.setStroke(Color.web("#D6FFED"));
                g.setLineWidth(2);
                g.strokeLine(x - 3, y - 4, x - 3, y + 1);
            }
            case INVINCIBILITY -> drawStar(g, x, y, 8, 4, INK);
            case DOUBLE_FISH -> {
                g.setFont(TOKEN_FONT);
                g.setTextAlign(TextAlignment.CENTER);
                g.setTextBaseline(VPos.CENTER);
                g.fillText("×2", x, y - 3);
                g.setFont(BUFF_CAPTION_FONT);
                g.fillText("FISH", x, y + 6);
            }
            case BOMB -> {
                g.fillOval(x - 7, y - 5, 14, 14);
                g.fillRect(x - 1, y - 8, 5, 5);
                g.setStroke(INK);
                g.setLineWidth(2);
                g.strokeLine(x + 3, y - 7, x + 6, y - 10);
                g.setFill(CREAM);
                g.fillRect(x - 4, y - 2, 3, 3);
            }
        }
    }

    private void drawLaserWarning(GraphicsContext g, Hazard h, double t) {
        double pulse = .55 + .25 * Math.sin(t * 13);
        g.save();
        g.setLineWidth(2);
        g.setStroke(AMBER.deriveColor(0, 1, 1, pulse));
        g.setFill(AMBER.deriveColor(0, 1, 1, .045 + pulse * .035));
        g.setLineDashes(7, 7);
        g.fillRect(h.x(), h.y() - 4, h.width(), h.height() + 8);
        g.strokeLine(h.x(), h.y(), h.x() + h.width(), h.y());
        g.strokeLine(h.x(), h.y() + h.height(), h.x() + h.width(), h.y() + h.height());
        g.setLineDashes();
        drawEmitter(g, 24, h.y() + h.height() / 2, false, pulse);
        drawEmitter(g, 936, h.y() + h.height() / 2, false, pulse);
        g.restore();
    }

    private void drawHazard(GraphicsContext g, Hazard h, double t) {
        switch (h.kind()) {
            case KNIFE -> drawKnife(g, h);
            case DART -> drawDart(g, h);
            case LASER -> drawLaser(g, h, t);
            case POISON -> drawPoison(g, h, t);
            case SCISSORS -> drawScissors(g, h, t);
            case FLAME -> drawFlame(g, h, t);
        }
    }

    private void drawPoison(GraphicsContext g, Hazard h, double t) {
        double x = h.x(), y = h.y(), w = h.width(), height = h.height();
        g.setFill(Color.web("#A3D95E", .13)); g.fillOval(x - 8, y - 6, w + 16, height + 12);
        g.setFill(Color.web("#31472C")); g.fillRoundRect(x, y, w, height, 18, 18);
        g.setFill(Color.web("#9CCB5B", .94)); g.fillRoundRect(x + 2, y + 2, w - 4, height - 4, 16, 16);
        for (int i = 0; i < 4; i++) {
            double drift = Math.sin(t * 5 + i * 1.9) * 2;
            g.setFill(Color.web(i % 2 == 0 ? "#D5E788" : "#A9D269", .72));
            g.fillOval(x + 4 + i * 12, y + 3 + drift, 16, 17);
        }
        // A dark skull makes poison distinct from the garden's harmless foliage.
        double cx = x + w / 2;
        g.setFill(Color.web("#3C5030")); g.fillRoundRect(cx - 7, y + 7, 14, 12, 6, 6);
        g.fillRect(cx - 4, y + 16, 8, 6);
        g.setFill(Color.web("#DCEBA3")); g.fillRect(cx - 4, y + 10, 3, 3); g.fillRect(cx + 2, y + 10, 3, 3);
    }

    private void drawScissors(GraphicsContext g, Hazard h, double t) {
        g.save();
        g.translate(h.x() + h.width() / 2, h.y() + h.height() / 2);
        g.scale(h.direction() >= 0 ? 1 : -1, 1);
        double opening = 5 + 3 * (.5 + .5 * Math.sin(t * 11));
        g.setStroke(Color.web("#152C34")); g.setLineWidth(5);
        g.strokeLine(-5, -5, 16, opening); g.strokeLine(-5, 5, 16, -opening);
        g.setStroke(Color.web("#DCE5D8")); g.setLineWidth(3);
        g.strokeLine(-5, -5, 16, opening); g.strokeLine(-5, 5, 16, -opening);
        g.setFill(Color.web("#132C35")); g.fillOval(-18, -11, 15, 12); g.fillOval(-18, 0, 15, 12);
        g.setFill(DANGER); g.fillOval(-16, -9, 11, 8); g.fillOval(-16, 2, 11, 8);
        g.setFill(Color.web("#243C43")); g.fillOval(-13, -7, 5, 4); g.fillOval(-13, 4, 5, 4);
        g.setFill(AMBER); g.fillOval(-3, -3, 6, 6);
        g.restore();
    }

    private void drawFlame(GraphicsContext g, Hazard h, double t) {
        double x = h.x(), y = h.y(), w = h.width(), base = y + h.height();
        double flicker = Math.sin(t * 22 + x) * 3;
        g.setFill(Color.web("#FFAC61", .12)); g.fillRoundRect(x - 8, y - 6, w + 16, h.height() + 8, 20, 20);
        g.setFill(Color.web("#FF784F"));
        g.fillPolygon(new double[]{x, x + 3, x + 9, x + 12, x + w * .55 + flicker, x + w - 8, x + w - 3, x + w, x + w - 4, x + 4},
                new double[]{base - 4, y + 27, y + 36, y + 14, y, y + 30, y + 19, base - 17, base, base}, 10);
        g.setFill(Color.web("#FFD375"));
        g.fillPolygon(new double[]{x + 6, x + 12, x + w * .58, x + w - 7, x + w - 5},
                new double[]{base - 3, y + 36, y + 15 + flicker, y + 41, base - 3}, 5);
        g.setFill(Color.web("#99E6F2"));
        for (int i = 0; i < 4; i++) g.fillOval(x + 3 + i * 8, base - 13 - (i % 2) * 3, 7, 14);
        g.setFill(Color.web("#102D37")); g.fillRoundRect(x - 4, base - 1, w + 8, 7, 4, 4);
        g.setFill(Color.web("#7B989C")); g.fillRect(x, base, w, 2);
    }

    private void drawKnife(GraphicsContext g, Hazard h) {
        double x = h.x(), y = h.y(), w = h.width(), height = h.height();
        double cx = x + w / 2;
        g.setFill(INK);
        g.fillRect(cx - 5, y, 10, height * .28);
        g.fillPolygon(new double[]{x, x + w, x + w - 2, cx, x + 2},
                new double[]{y + height * .28, y + height * .28,
                        y + height * .68, y + height, y + height * .68}, 5);
        g.setFill(Color.web("#D67E63"));
        g.fillRect(cx - 3, y + 1, 6, height * .25);
        g.setFill(Color.web("#FFB18A"));
        g.fillRect(cx - 3, y + 2, 2, height * .20);
        g.setFill(Color.web("#D9E4DF"));
        g.fillPolygon(new double[]{x + 2, x + w - 2, x + w - 3, cx, x + 3},
                new double[]{y + height * .34, y + height * .34,
                        y + height * .65, y + height - 4, y + height * .65}, 5);
        g.setFill(Color.web("#F9FFEA"));
        g.fillPolygon(new double[]{x + 3, cx, cx, x + 4},
                new double[]{y + height * .36, y + height * .36,
                        y + height - 5, y + height * .64}, 4);
        g.setFill(Color.web("#80A7A1"));
        g.fillRect(x + 1, y + height * .28, w - 2, 3);
    }

    private void drawDart(GraphicsContext g, Hazard h) {
        g.save();
        g.translate(h.x() + h.width() / 2, h.y() + h.height() / 2);
        g.scale(h.direction() >= 0 ? 1 : -1, 1);
        double w = h.width(), halfH = h.height() / 2;
        g.setFill(INK);
        g.fillRect(-w / 2, -3, w - 6, 6);
        g.fillPolygon(new double[]{w / 2 - 13, w / 2 + 1, w / 2 - 13},
                new double[]{-halfH, 0, halfH}, 3);
        g.setFill(Color.web("#CB805C"));
        g.fillRect(-w / 2 + 1, -1, w - 12, 2);
        g.setFill(DANGER);
        g.fillPolygon(new double[]{-w / 2, -w / 2 + 10, -w / 2 + 15, -w / 2 + 4},
                new double[]{-halfH + 1, -halfH + 1, -2, -2}, 4);
        g.fillPolygon(new double[]{-w / 2, -w / 2 + 10, -w / 2 + 15, -w / 2 + 4},
                new double[]{halfH - 1, halfH - 1, 2, 2}, 4);
        g.setFill(Color.web("#EEF1DB"));
        g.fillPolygon(new double[]{w / 2 - 11, w / 2 - 2, w / 2 - 11},
                new double[]{-halfH + 2, 0, halfH - 2}, 3);
        g.restore();
    }

    private void drawLaser(GraphicsContext g, Hazard h, double t) {
        double x = h.x(), y = h.y(), w = h.width(), height = h.height();
        g.setFill(Color.web("#FF806C", .075));
        g.fillRect(x, y - 13, w, height + 26);
        g.setFill(Color.web("#FF806C", .16));
        g.fillRect(x, y - 5, w, height + 10);
        g.setFill(Color.web("#FF775F"));
        g.fillRect(x, y, w, height);
        g.setFill(Color.web("#FFE6B8"));
        g.fillRect(x, y + height * .31, w, Math.max(2, height * .38));
        g.setFill(Color.web("#FFF7D7", .7));
        for (int i = 0; i < 8; i++) {
            double sx = x + ((i * 137 + t * 170) % Math.max(1, w));
            g.fillRect(sx, y + height * .40, Math.min(24, x + w - sx), 2);
        }
        drawEmitter(g, 24, y + height / 2, true, 1);
        drawEmitter(g, 936, y + height / 2, true, 1);
    }

    private void drawEmitter(GraphicsContext g, double x, double y, boolean active, double alpha) {
        g.setFill(INK);
        g.fillRoundRect(x - 11, y - 14, 22, 28, 5, 5);
        g.setFill(Color.web("#6B7660"));
        g.fillRect(x - 8, y - 11, 16, 22);
        g.setFill(active ? DANGER : AMBER.deriveColor(0, 1, 1, alpha));
        g.fillRect(x - 6, y - 6, 12, 12);
        g.setFill(active ? CREAM : Color.web("#FCE6AE", alpha * .75));
        g.fillRect(x - 2, y - 4, 4, 8);
    }

    private void drawPlayer(GraphicsContext g, Player p, double t, int skinIndex, boolean versus) {
        CatMotion motion = catMotions.computeIfAbsent(p.id(), id -> new CatMotion(p.grounded()));
        if (p.grounded() && !motion.grounded) motion.landedAt = t;
        motion.grounded = p.grounded();
        double landing = p.grounded() ? Math.max(0, 1 - (t - motion.landedAt) / .16) : 0;
        double cx = Math.rint(p.x() + p.width() / 2);
        double bottom = Math.rint(p.y() + p.height());
        boolean walking = Math.abs(p.vx()) > 8 && p.grounded();
        g.setFill(Color.web("#0D282A", p.grounded() ? .28 : .13));
        g.fillOval(cx - 16, bottom - 3, 32, 7);

        if (p.shieldActive()) {
            g.setFill(CYAN.deriveColor(0, 1, 1, .075));
            g.fillOval(cx - 27, bottom - 53, 54, 57);
            g.setStroke(CYAN.deriveColor(0, 1, 1, .65 + Math.sin(t * 4) * .15));
            g.setLineWidth(2);
            g.strokeOval(cx - 27, bottom - 53, 54, 57);
            g.setStroke(Color.web("#E5FFF0", .75));
            g.strokeArc(cx - 23, bottom - 49, 46, 49, 90, 57, javafx.scene.shape.ArcType.OPEN);
        }
        if (p.invincibilityRemaining() > 0) {
            for (int i = 0; i < 4; i++) {
                double a = t * 2 + i * Math.PI / 2;
                double sx = cx + Math.cos(a) * 28;
                double sy = bottom - 23 + Math.sin(a) * 29;
                drawSparkle(g, sx, sy, 4, i % 2 == 0 ? AMBER : Color.web("#E5C8FF"));
            }
        }
        g.save();
        g.translate(cx, bottom);
        g.scale(p.facingRight() ? 1 : -1, 1);
        drawCat(g, skinIndex, t, walking, p.grounded(), p.vy() < -5, landing);
        g.restore();
        if (landing > 0) {
            g.setFill(Color.web("#E5D9B2", landing * .35));
            double spread = (1 - landing) * 18;
            g.fillOval(cx - 21 - spread, bottom - 4, 8, 4);
            g.fillOval(cx + 13 + spread, bottom - 4, 8, 4);
        }
        if (versus || p.doubleFishRemaining() > 0) drawPlayerTag(g, p, cx, bottom - 59, versus);
    }

    private void drawPlayerTag(GraphicsContext g, Player player, double x, double y, boolean versus) {
        boolean doubleFish = player.doubleFishRemaining() > 0;
        String text = versus ? "P" + player.id() : "";
        if (doubleFish) text += (versus ? " · " : "") + "×2 FISH";
        double width = doubleFish ? (versus ? 90 : 62) : 29;
        x = Math.max(width / 2 + 4, Math.min(GameWorld.WIDTH - width / 2 - 4, x));
        Color identity = player.id() == 2 ? CYAN : AMBER;
        g.setFill(Color.web("#163937", .92));
        g.fillRoundRect(x - width / 2, y - 8, width, 17, 8, 8);
        g.setStroke(identity.deriveColor(0, 1, 1, .55));
        g.setLineWidth(1);
        g.strokeRoundRect(x - width / 2, y - 8, width, 17, 8, 8);
        g.setFont(PLAYER_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(doubleFish ? AMBER : identity);
        g.fillText(text, x, y);
    }

    private void drawEliminatedPlayer(GraphicsContext g, Player p, int skinIndex, boolean versus) {
        double cx = Math.rint(p.x() + p.width() / 2);
        double bottom = Math.rint(p.y() + p.height());
        g.save();
        g.setGlobalAlpha(.25);
        g.translate(cx, bottom);
        g.scale(p.facingRight() ? 1 : -1, 1);
        // A fixed pose: eliminated players have no walking, breathing, blinking, or buff effects.
        drawCat(g, skinIndex, 0, false, false, false);
        g.restore();
        g.setStroke(Color.web("#F3ACA0", .8));
        g.setLineWidth(2.5);
        g.strokeLine(cx - 6, bottom - 29, cx + 6, bottom - 17);
        g.strokeLine(cx + 6, bottom - 29, cx - 6, bottom - 17);
        String text = (versus ? "P" + p.id() + " · " : "") + "OUT";
        double labelX = Math.max(50, Math.min(GameWorld.WIDTH - 50, cx));
        g.setFill(Color.web("#1A3433", .94));
        g.fillRoundRect(labelX - 46, bottom - 65, 92, 17, 8, 8);
        g.setFont(PLAYER_FONT);
        g.setTextAlign(TextAlignment.CENTER);
        g.setTextBaseline(VPos.CENTER);
        g.setFill(Color.web("#D6ACA4"));
        g.fillText(text, labelX, bottom - 57);
    }

    /** Local origin is the center of the feet; the silhouette is deliberately chunky. */
    private void drawCat(GraphicsContext g, int skinIndex, double t, boolean walking,
                         boolean grounded, boolean rising) {
        drawCat(g, skinIndex, t, walking, grounded, rising, 0);
    }

    private void drawCat(GraphicsContext g, int skinIndex, double t, boolean walking,
                         boolean grounded, boolean rising, double landing) {
        CatPalette palette = CATS[Math.floorMod(skinIndex, CATS.length)];
        double stride = walking ? Math.sin(t * 18) : 0;
        double bob = walking ? -Math.abs(stride) * 2 : (grounded ? Math.sin(t * 2) * .55 : 0);
        if (landing > 0) g.scale(1 + .18 * landing, 1 - .20 * landing);
        else if (!grounded) g.scale(rising ? .90 : 1.08, rising ? 1.10 : .95);
        else if (walking) { g.rotate(3); g.scale(1 + Math.abs(stride) * .04, 1 - Math.abs(stride) * .03); }
        g.translate(0, bob);

        // Tail, outline, paws, and head are layered out of whole-pixel blocks.
        g.save();
        g.translate(-10, -6);
        g.rotate(grounded ? Math.sin(t * (walking ? 12 : 3)) * 15 : rising ? 25 : -35);
        g.translate(10, 6);
        g.setFill(palette.outline);
        g.fillRect(-22, -19, 6, 13);
        g.fillRect(-19, -10, 14, 7);
        g.setFill(palette.fur);
        g.fillRect(-20, -17, 3, 10);
        g.fillRect(-17, -8, 10, 3);
        g.setFill(palette.shadow);
        g.fillRect(-20, -17, 3, 3);
        g.restore();

        g.setFill(palette.outline);
        g.fillRect(-11, -21, 24, 19);
        double leftFoot = !grounded && rising ? -5 : -Math.max(0, stride) * 4;
        double rightFoot = !grounded && rising ? -5 : -Math.max(0, -stride) * 4;
        double spread = !grounded ? (rising ? -3 : 4) : 0;
        double leftX = -13 - spread + stride * 4;
        double rightX = 5 + spread - stride * 4;
        g.fillRect(leftX, -7 + leftFoot, 11, 7);
        g.fillRect(rightX, -7 + rightFoot, 11, 7);
        g.setFill(palette.fur);
        g.fillRect(-9, -20, 20, 15);
        g.fillRect(leftX + 2, -6 + leftFoot, 8, 4);
        g.fillRect(rightX + 1, -6 + rightFoot, 8, 4);
        g.setFill(palette.chest);
        g.fillRect(-1, -18, 9, 10);
        g.setFill(palette.shadow);
        g.fillRect(-9, -19, 4, 10);
        g.fillRect(-9, -12, 7, 3);
        if (!grounded) {
            // Front paws tuck during ascent and reach outward while falling.
            double armX = rising ? 9 : 14;
            double armY = rising ? -20 : -24;
            g.setFill(palette.outline); g.fillRect(-armX - 5, armY, 8, 8); g.fillRect(armX - 2, armY, 8, 8);
            g.setFill(palette.light); g.fillRect(-armX - 3, armY + 1, 5, 5); g.fillRect(armX, armY + 1, 5, 5);
        }

        // Square ears and stepped cheeks keep the silhouette readable at game scale.
        double headY = rising ? -38 : -37;
        g.setFill(palette.outline);
        g.fillRect(-15, headY - 4, 8, 11);
        g.fillRect(7, headY - 4, 8, 11);
        g.fillRect(-15, headY + 2, 30, 20);
        g.fillRect(-12, headY + 21, 25, 3);
        g.setFill(palette.fur);
        g.fillRect(-13, headY - 2, 4, 10);
        g.fillRect(9, headY - 2, 4, 10);
        g.fillRect(-13, headY + 4, 26, 16);
        g.fillRect(-10, headY + 20, 21, 2);
        g.setFill(Color.web("#D68F81"));
        g.fillRect(-12, headY, 2, 5);
        g.fillRect(10, headY, 2, 5);
        g.setFill(palette.light);
        g.fillRect(-9, headY + 4, 19, 4);
        g.setFill(palette.shadow);
        g.fillRect(-4, headY + 4, 3, 6);
        g.fillRect(3, headY + 4, 3, 4);
        g.fillRect(-13, headY + 10, 3, 3);

        g.setFill(palette.chest);
        g.fillRect(-3, headY + 14, 13, 6);
        g.fillRect(0, headY + 12, 7, 3);
        g.setFill(palette.outline);
        boolean blink = grounded && (t % 4.6) > 4.43;
        if (blink) {
            g.fillRect(-7, headY + 12, 4, 1);
            g.fillRect(6, headY + 12, 4, 1);
        } else {
            g.fillRect(-7, headY + 10, 4, 5);
            g.fillRect(6, headY + 10, 4, 5);
            g.setFill(CREAM);
            g.fillRect(-6, headY + 10, 1, 2);
            g.fillRect(7, headY + 10, 1, 2);
        }
        g.setFill(Color.web("#A3675F"));
        g.fillRect(1, headY + 15, 3, 2);
        g.setFill(palette.outline);
        g.fillRect(2, headY + 17, 1, 2);
        g.fillRect(0, headY + 18, 2, 1);
        g.fillRect(3, headY + 18, 2, 1);
        g.setFill(Color.web("#FFD8AA", .55));
        g.fillRect(-11, headY + 16, 4, 2);
        g.fillRect(9, headY + 16, 3, 2);
    }

    private void drawSparkle(GraphicsContext g, double x, double y, double size, Color color) {
        g.setFill(color);
        g.fillRect(Math.rint(x) - 1, Math.rint(y) - size, 2, size * 2);
        g.fillRect(Math.rint(x) - size, Math.rint(y) - 1, size * 2, 2);
    }

    private void curve(GraphicsContext g, double startX, double startY, double controlX1,
                       double controlY1, double controlX2, double controlY2,
                       double endX, double endY) {
        g.beginPath();
        g.moveTo(startX, startY);
        g.bezierCurveTo(controlX1, controlY1, controlX2, controlY2, endX, endY);
        g.stroke();
    }

    private void drawStar(GraphicsContext g, double x, double y, double outer, double inner,
                          Color color) {
        double[] xs = new double[10];
        double[] ys = new double[10];
        for (int i = 0; i < 10; i++) {
            double angle = -Math.PI / 2 + i * Math.PI / 5;
            double radius = i % 2 == 0 ? outer : inner;
            xs[i] = x + Math.cos(angle) * radius;
            ys[i] = y + Math.sin(angle) * radius;
        }
        g.setFill(color);
        g.fillPolygon(xs, ys, 10);
    }

    private static final class CatPalette {
        final Color outline;
        final Color fur;
        final Color light;
        final Color shadow;
        final Color chest;

        CatPalette(String outline, String fur, String light, String shadow, String chest) {
            this.outline = Color.web(outline);
            this.fur = Color.web(fur);
            this.light = Color.web(light);
            this.shadow = Color.web(shadow);
            this.chest = Color.web(chest);
        }
    }
}
