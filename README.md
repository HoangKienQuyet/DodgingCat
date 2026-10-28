# Dodging Cat 

> **Working project documentation for team review.** Gameplay and performance notes linked below reflect the current implementation; other descriptions may need updating before submission.

Dodging Cat is a 2D Action Survival Platformer developed in Java (JDK 25), Maven, and JUnit 5. Desktop rendering uses core Java2D/AWT (`java.desktop`) at native display resolution, with JavaFX retained for existing image/media services and an optional comparison renderer. It features single-player survival, **Local Co-op**, loadouts, procedural platforms, hazards, buffs, fish currency, and **Fairness-Aware Procedural Generation**.

Current [gameplay values](docs/gameplay-thong-so.md) and [native rendering measurements](docs/performance-native-2026-10-28.md) supersede older descriptions below. Run with `run.cmd` or `./dev.ps1 run`; F11 switches fullscreen and F10 displays FPS.

For a Windows demo without installed Java/Maven, use the [direct-source portable package](docs/portable-windows.md). The ZIP exposes `src` and `pom.xml` immediately and bundles an offline Java compiler. Open `Run.cmd` after extraction; use `Build.cmd` after editing source.

---

## 1. Project Information

| Item | Details |
| :--- | :--- |
| **Project Name** | Dodging Cat  |
| **Team** | TADQ |
| **Course** | Java Software Development (Academic Year 2026–2027) |
| **Instructor** | Ngô Văn Quyền |
| **Team Size** | 4 members |
| **Genre** | 2D Action Survival Platformer |
| **Game Modes** | Single-player Survival & Local Co-op (WASD + Arrow Keys) |
| **Main Technical Contribution** | **Fairness-Aware Procedural Generation** |
| **Development Path** | Repository root after cloning or extracting the project |
| **Status** | 🟡 **In Development; visual/audio assets and playtest refinement remain** |

### Team Members and Contribution Record

All four members built separate game prototypes. The group selected this version, developed by Tran Quang Anh, as the base for the final submission. Record any later integration, tests, documentation, or packaging work against the changes each member actually makes; the final contribution breakdown is still being prepared.

| # | Student ID | Name | Current record |
| :-: | :--- | :--- | :--- |
| 1 | `2301040209` | **Hoang Kien Quyet** | Independent prototype; contribution to this version to be documented |
| 2 | `2301040013` | **Tran Quang Anh** | Initial implementation of the selected game version |
| 3 | `2301040171` | **Tran Gia Thanh** | Independent prototype; contribution to this version to be documented |
| 4 | `2301040042` | **Nguyen Cong Doan** | Independent prototype; contribution to this version to be documented |

---

## 2. Core Gameplay & Features

**Visual/audio status:** The game uses PNG backgrounds, animated skin sheets, authored raster UI lettering, map music and gameplay/UI SFX. Source artwork and decode resolution are preserved; native-sized drawing caches avoid repeating scaling and panel painting every frame.

```
Main Menu → Pre-Game Loadout (Map & Skin) → Move / Jump / Drop-down →
Dodge Hazards → Collect Buffs & Fishes → Survive →
Game Over (Survival Title) → Replay / Shop
```

### 2.1 Brotato-Style Main Menu & Mouse Support
- **Atmospheric Visuals**: Centered animated cat standing on a spotlight stage with ambient shadow silhouettes lurking in the background.
- **Full Desktop Mouse Controls**: Clickable buttons, hover glows, and tab navigation, alongside complete keyboard support.
- **Fullscreen Mode**: Toggle fullscreen seamlessly with `F11` or via the Settings menu.
- **Top Bar**: Currency balance only. Language and audio options are inside Settings.
- **Main Menu Layout**: Single Player, Local Co-op, and Shop on the left; Leaderboard, Settings, and Quit on the right. A suspended run adds a `Resume` button.
- **Settings & Audio Screen**: BGM volume slider, SFX volume slider, language toggle, and fullscreen toggle.

### 2.2 Pre-Game Loadout Selection Screen
- Opens when choosing **Single Player** or **Local Co-op**.
- Allows players to select an owned **Map** and **Skin** (P1 and P2) with live preview cards before launching the survival run.

### 2.3 Shop Architecture
- Separate tabs for **Skins** and **Maps**.
- Skin cards retain rarity styling. Map cards have no rarity labels or tiers; map prices remain unchanged.
- Shop is strictly for purchasing; equipping is handled in the pre-game loadout.
- Rebalanced pricing requiring dedicated gameplay runs (~20 runs per skin, maps $2\times$).

### 2.4 Dynamic Procedural Platforms
- No more static identical platforms! Each run generates 4–7 elevated platforms across 3–4 randomized tiers, validates connected jumps, keeps the first tier within jump range of the safe floor, and spreads platforms across the play area. After 60 seconds, crumbling platforms respawn at newly randomized reachable positions; occasional extra branches can grow the layout to a maximum of 10 elevated platforms.
- **One-Way Platforms**: Jump through from underneath ($v_y < 0$), land firmly on top ($v_y \ge 0$).
- **Drop-Down**: Press the `Down` key while standing on any one-way platform to drop through.
- **Crumbling Platforms (> 60s)**: At least one upper-tier platform can crumble. It shakes and cracks for 1.8s, stays collapsed for 4.0s, then respawns at a newly selected reachable location.

### 2.5 The 3 Universal Hazards + Map-Specific Hazards
- **3 Universal Hazards** (featured on ALL maps):
  1. **Pendulum Weight (Quả Tạ)**: Heavy iron weight oscillating on a steel chain along $\theta(t) = \theta_0 \cos(\omega t)$.
  2. **Laser Hazard**: Available in **both Horizontal and Vertical** orientations with animated telegraph warning lines before firing!
  3. **Cannon Hazard**: Available in **both Horizontal and Vertical** (mortar drop / upward shot) orientations with telegraph aim lines and fiery cannonballs!
- **Map-Specific Falling Hazards**:
  - *Kitchen*: Knife, Frying Pan, Rolling Pin, Salt Shaker, Blender.
  - *Living Room*: Plant Pot, Remote Control, Hardcover Book, Cushion, Lamp.
  - *Bedroom*: Twin-bell Alarm Clock, Fluffy Pillow, Slipper, Mirror Shard, Toy Block.
  - *Garden*: Red Apple, Bee Hive, Garden Trowel, Watermelon, Acorn.
  - *Rooftop*: Heavy Brick, Satellite Dish, TV Antenna, Weather Vane, Lightning Bolt.

### 2.6 Cat Skins & Rarities
- **Common**: Ginger Tabby, Tuxedo, Snow White, Classic Gray (solid & two-tone coats).
- **Uncommon**: Calico Tri-color, Siamese, Bengal Leopard (distinct breed patterns).
- **Rare**: Cyber Neon (visor + neon circuits), Lava Tabby (magma stripes), Ghost Cat (translucent spectral wisps).
- **Legendary**: Santa Cat (Santa hat + snowflake particles), Super Cat (fluttering red cape + hero badge), Pirate Cat (tricorn captain hat + eye patch + red bandana).
- Full animations: walk cycle, wagging tails, eye blinks, jump squash/stretch, and accessory particle trails.

### 2.7 Thematic Map Visuals
- **Kitchen**: Checkerboard tile floor, wall grid, hanging copper pans rack, cupboards.
- **Living Room**: Wood parquet floor, striped wallpaper, large bookshelf with books, cozy painting.
- **Bedroom**: Starry arched window showing moon and twinkling stars, pastel wallpaper, soft carpet floor.
- **Garden**: Fresh green grass floor with grass blades, wooden picket fence, drifting fluffy clouds.
- **Rooftop**: Terracotta chimney bricks floor, deep night sky with glowing crescent moon, skyscraper skyline.

### 2.8 Clean HUD, Buff Visuals & Game Over Screen
- **Clean In-Game HUD**: Displays `TIME: 00:24.5s`, `SCORE: 245`, `🐟 FISHES: +14`, `BEST: 1250`. Redundant P1 state removed.
- **Clean Buff Visuals**: Shield hits trigger bubble glass shatter particles; no cluttering floating text.
- **Buff Limits**: One pickup or timed effect at a time. Every pickup grants 50 points. Type cooldowns begin after despawn/effect completion: Double Score 25s, Bomb/Invincibility 35s, Shield/Heart 45s. Heart requires a downed teammate. See the current gameplay table for lifetimes and weights.
- **Game Over Screen**: Prominent full-screen banner with celebratory **Survival Titles / Ranks**:
  - $< 20$s: **"Noob Kitten" / "Mèo Mới Vào Nghề"**
  - $20 - 60$s: **"Quick Paws" / "Mèo Nhanh Nhẹn"**
  - $60 - 120$s: **"Acrobatic Feline" / "Bậc Thầy Nhào Lộn"**
  - $120 - 200$s: **"Shadow Dodger" / "Bóng Ma Né Đòn"**
  - $> 200$s: **"King of Dodge" / "Vua Sinh Tồn"**
- Displays run-only fishes collected, score, and buttons: `Play Again` and `Main Menu`.
- **Pause Menu**: Includes `Resume`, `Restart`, and `Return to Main Menu`.

---

## 3. Technical Contribution: Fairness-Aware Procedural Generation

Every procedural candidate must pass 5 explicit safety rules evaluated by `FairnessValidator`:
1. **Minimum Spawn Delay**: Mandatory cooldown between spawns.
2. **Active Hazard Limit**: Tier-based cap on simultaneous hazards (0–<30s: 3, 30–<60s: 6, 60–<120s: 9, ≥120s: 12). This limits hazards active at one time, not the cumulative number spawned during a run.
3. **Safe-Zone Padding**: Minimum physical clearance buffer around living cats.
4. **Human Reaction Time Window**: Rejects any candidate impacting in under 280ms.
5. **Escape Corridor Density**: Caps simultaneous laser and cannon warning channels to prevent unavoidable walls.

$$\text{Invalid Generation Rate} = \frac{\text{Candidates Rejected}}{\text{Total Candidates Generated}}$$

---

## 4. How to Run the Game

Navigate to `E:\ProjectJSD\AntigravityVersion`:

1. **One-Click Launch**: Double-click **`run.cmd`** or in PowerShell:
   ```powershell
   .\dev.ps1 -Task run
   ```
   Press `F9` to enable development mode with a continually refilled fish balance. Press `F9` again to reset the saved profile to the first-time player defaults (including scores and unlocks).
2. **Run Automated Tests**: Double-click **`test.cmd`** or in PowerShell:
   ```powershell
   .\dev.ps1 -Task test
   ```
3. **Standard Maven Commands**:
   ```bash
   mvn clean test
   mvn javafx:run
   ```
