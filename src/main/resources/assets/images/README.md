# Image and Sprite Asset Guide

The game supports image assets with JavaFX Canvas fallbacks. Kitchen and Ginger were the original art pilot. Map backgrounds, map-specific platforms, collectibles, buff icons, and universal cannon/laser warning sprites are integrated. Map-specific falling-object sprites can still fall back to code-drawn art until their individual assets are supplied.

```text
assets/images/
├── backgrounds/   <map-id>.png (original-resolution source, used at every aspect ratio)
├── characters/    <skin-id>_sheet.png
├── hazards/       warning_sign.png, cannonball.png, pendulum.png, *_hazards.png
├── buffs/         heart.png, bomb.png, shield.png, invincibility.png, double_score.png
├── currency/      fish.png, fish_can.png
├── platforms/     kitchen_counter.png, <map-id>_platform.png
└── ui/            title_dodging_cat.png, button_plaque.png, team_tadq.png, art_glyphs.png
```

The renderer keeps a uniform scale and adapts the logical playfield width to the window, so wide displays gain more playable area without stretching sprites. Every viewport uses the same original map PNG with a floor-aligned cover crop. The unused `_wide` copies were removed during submission packaging; this does not change the map artwork drawn by the game. Each map shares the Kitchen floor collider baseline at 85.55% of logical height. Living Room is a morning scene, Bedroom and Rooftop are nighttime scenes, and Kitchen and Garden remain in late-afternoon light.

Each map has its own elevated platform artwork: Kitchen counter, Living Room sofa cushion, Bedroom pillow, Garden stone-and-grass ledge, and Rooftop brickwork. The assets are transparent PNG cutouts. The renderer trims transparent padding for the four new platform images; warning cracks and break particles remain code-drawn.

Every character skin uses a 6-column × 4-row sheet. Rows are idle, run, jump ascent, and falling. Skin cards and menu previews cycle only the idle row. Gameplay selects run, jump, and falling rows from the player's movement state. The renderer waits for the sheet to load rather than drawing a procedural cat.

The illustrated title and logo are authored PNG assets; their reference images and ImageGen prompts are recorded in `ui/BRANDING_PROMPTS.md`. `java tools/GenerateUiArt.java` regenerates only the button plaque and raster glyph atlas, preserving the title and logo. Runtime UI labels use `art_glyphs.png` and `art_glyphs.txt` instead of drawing a system font. Countdown labels are separate high-resolution PNGs regenerated with `java tools/GenerateCountdownArt.java`.

`platforms/kitchen_counter.png` is the Kitchen ledge image and keeps its established source crop. Every elevated platform is eligible for the crumble schedule, with at most two collapsed at once; the floor never crumbles.

`buffs/` contains five transparent pickup images used directly by the renderer. `currency/fish.png` and `currency/fish_can.png` are transparent pickup images; the can still represents the existing five-fish value.

Use transparent PNGs for characters, hazards, buffs, pickups, and platform art. Backgrounds are full-scene images for their supported aspect ratios, while platforms and individual objects are resized to their in-game bounds.

Universal hazard images: `warning_sign.png`, `cannonball.png`, and `pendulum.png`. Laser warning lines and particle/screen-shake effects are still drawn by code.

Map-specific hazard IDs are `knife`, `frying_pan`, `rolling_pin`, `salt_shaker`, `blender`, `plant_pot`, `remote_control`, `book`, `cushion`, `lamp`, `alarm_clock`, `pillow`, `slipper`, `mirror_shard`, `toy_block`, `apple`, `bee_hive`, `trowel`, `watermelon`, `acorn`, `brick`, `satellite_dish`, `antenna`, `weather_vane`, and `lightning`.

Each `*_hazards.png` is a 2-column × 3-row transparent spritesheet for that map's five falling hazard types; the sixth cell is intentionally empty. The renderer uses procedural drawing when an optional file is missing. Keep original asset packs and license/credit information in project documentation; do not assume that “free download” means the same reuse terms for every pack.
