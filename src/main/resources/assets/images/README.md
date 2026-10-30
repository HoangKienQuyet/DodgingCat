# Image and sprite asset guide

[Documentation index](../../../../../docs/README.md) · [English README](../../../../../README.md) · [README tiếng Việt](../../../../../README_vi.md) · [Audio guide](../audio/README.md)

The current Java2D and optional JavaFX renderers share [AssetManager](../../../java/com/dodgingcat/ui/AssetManager.java) and [GameRenderer](../../../java/com/dodgingcat/ui/GameRenderer.java). All five backgrounds, 15 skin sheets, five map hazard sheets, five platforms, five buff icons and normal/legendary currency art are included. Missing falling-object art can use code fallbacks; characters deliberately wait for their sheet instead of drawing a procedural cat.

```text
backgrounds/   kitchen, living_room, bedroom, garden, rooftop .png
characters/    <skin-id>_sheet.png (6 columns × 4 rows)
hazards/       <map-id>_hazards.png (2 columns × 3 rows), warning_sign,
               cannonball, pendulum, pendulum_chain .png
platforms/     kitchen_counter.png, <other-map-id>_platform.png
buffs/         heart, bomb, shield, invincibility, double_score .png
currency/      fish.png, fish_can.png; skins/<cyber|ghost|santa>_<fish|can>.png
ui/            title_dodging_cat, team_tadq, button_plaque,
               button_plaque_hover, art_glyphs .png; art_glyphs.txt; countdown/
```

## Backgrounds and platforms

The renderer uses each original `<map>.png` at all aspect ratios with a floor-aligned cover crop. Logical height is 640, width follows viewport ratio, and the floor is at 85.55% of logical height for every map. Native display pixels and DPI determine render scale; no default 720p limit or separate `*_wide` source is used. Cropping to cover may hide scene edges; sprites keep uniform scale.

Kitchen uses kitchen_counter.png with its established source crop; Living Room/Bedroom/Garden/Rooftop use their own trimmed platform art. Platform collision and crumble rules are in [gameplay parameters](../../../../../docs/gameplay-thong-so.md), separate from image bounds. The floor is not a crumbling platform.

## Character sheets and previews

Every skin uses an exact transparent 6×4 grid: row 0 idle, row 1 run, row 2 ascent, row 3 falling. Keep the same canvas/cell sizes and safe gutters when replacing art. Gameplay chooses movement rows; cards use six isolated idle frames fitted and centered in the preview box. Main menu and loadout stage do not draw a cat mascot. Legendary overlays/trails and defeated ghost/tombstone effects are code-drawn additions to sprite art.

Skin IDs/rarities are listed in [shop-and-loadout](../../../../../docs/shop-and-loadout.md). Names such as Neon/Spirit are display names; filenames remain cyber_sheet.png and ghost_sheet.png. Replacing a PNG with the same filename does not require a catalog rename, but a running process caches images and must be restarted.

## Hazard grids and pickups

Each map hazard sheet is 2×3; cells 0–4 in row-major order match the five hazard IDs for that map in [gameplay](../../../../../docs/gameplay-thong-so.md); cell 5 is unused. Alpha bounds are trimmed per frame. warning_sign/cannonball/pendulum/pendulum_chain are universal assets. Laser lines/beams can be drawn by code; optional laser_beam_horizontal.png/laser_beam_vertical.png are referenced by the loader but are not currently supplied files. All five map sheets are supplied, so fallback code is not evidence that their artwork is unfinished.

Buffs use transparent PNG icons. fish.png is +1, fish_can.png is +5. Legendary currency skins change the picture only; their selection/value rules are in gameplay parameters. Particles, warning lines, platform cracks, panel geometry and screen shake still use drawing code.

## UI lettering and branding

Runtime labels go through ArtText using art_glyphs.png and art_glyphs.txt. Text is assembled into cached raster strings, fitted to max width and drawn as images. JavaFX Font objects supply requested size; glyphs are not rendered from the machine's system fonts during gameplay. These are shared button images with raster text layered on top, **not a separate pre-lettered image for every button**. Hover swaps button_plaque_hover.png, whose alpha follows the base asset.

Countdown uses standalone 3/2/1 and go_en/go_vi PNGs. [GenerateUiArt](../../../../../tools/GenerateUiArt.java) bakes the glyph atlas/button images using installed Georgia/Arial at generation time; [GenerateCountdownArt](../../../../../tools/GenerateCountdownArt.java) generates the countdown. Run generators only when intentionally replacing their output; they do not regenerate the illustrated title/logo. Retain transparent alpha and inspect English/Vietnamese text after changing lettering or translations.

Title and team emblem are title_dodging_cat.png and team_tadq.png (TADQ). [BRANDING_PROMPTS.md](ui/BRANDING_PROMPTS.md) records the ImageGen prompts and output sizes; it is provenance, not gameplay configuration. Preserve source/creator/license records for assets; the repository does not establish a blanket redistribution license for all artwork. Source images are decoded at original resolution; native-sized draw caches are derived for rendering, not replacements for source files.
