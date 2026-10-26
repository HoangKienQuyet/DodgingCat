# Dodging Cat pixel-art asset guide

`/assets/pixel/` is the classpath root for standalone PNG artwork; put its
files below `src/main/resources/assets/pixel/`. The generated cozy-room scene
is currently the only shipped runtime PNG. All other optional pixel assets
continue to use the existing original JavaFX Canvas and `PixelSpriteLibrary`
fallbacks; no placeholder image files have been created.

`ClasspathImageLoader` caches each load and returns an empty result for a
missing or invalid PNG. `GameCanvasRenderer` and `UiKit` then use their current
fallback rendering, without changing model coordinates or collision bounds.

## Required paths and dimensions

The following are the exact paths the renderer will load when a real PNG is
added. Dimensions are source-pixel recommendations based on the current 2x
pixel scale. Rendering always uses the model dimensions in the final column.

| Role | Expected resource path | Recommended source size | Rendered size / model bound | Transparency |
| --- | --- | ---: | --- | --- |
| Room background | `/assets/pixel/world/cozy-room.png` | 900 x 540 | 900 x 540 world | Opaque preferred |
| Furniture platform tile | `/assets/pixel/platforms/furniture-platform.png` | 32 x 8 | platform width x 16 | Alpha optional |
| Cat idle | `/assets/pixel/cat/idle.png` | 28 x 23 | 56 x 46 | Required |
| Cat run frame 0 | `/assets/pixel/cat/run-0.png` | 28 x 23 | 56 x 46 | Required |
| Cat run frame 1 | `/assets/pixel/cat/run-1.png` | 28 x 23 | 56 x 46 | Required |
| Cat jump | `/assets/pixel/cat/jump.png` | 28 x 23 | 56 x 46 | Required |
| Cat fall | `/assets/pixel/cat/fall.png` | 28 x 23 | 56 x 46 | Required |
| Normal food | `/assets/pixel/food/normal-food.png` | 11 x 11 | 22 x 22 | Required |
| Golden food | `/assets/pixel/food/golden-food.png` | 13 x 13 | 26 x 26 | Required |
| Flower pot hazard | `/assets/pixel/hazards/flower-pot.png` | 19 x 21 | 38 x 42 | Required |
| Paper plane hazard | `/assets/pixel/hazards/paper-plane.png` | 27 x 14 | 54 x 28 | Required |
| Robot vacuum hazard | `/assets/pixel/hazards/robot-vacuum.png` | 27 x 14 | 54 x 28 | Required |
| Rolling ball hazard | `/assets/pixel/hazards/rolling-ball.png` | 18 x 18 | 36 x 36 | Required |
| HUD life icon | `/assets/pixel/ui/icons/life.png` | 16 x 16 | 16 x 16 | Required |
| HUD pause icon | `/assets/pixel/ui/icons/pause.png` | 14 x 14 | 14 x 14 | Required |
| HUD menu icon | `/assets/pixel/ui/icons/menu.png` | 14 x 14 | 14 x 14 | Required |
| Primary button skin | `/assets/pixel/ui/buttons/primary.png` | 192 x 48 | button background | Alpha optional |
| Blue button skin | `/assets/pixel/ui/buttons/blue.png` | 192 x 48 | button background | Alpha optional |
| Gold button skin | `/assets/pixel/ui/buttons/gold.png` | 192 x 48 | button background | Alpha optional |
| Purple button skin | `/assets/pixel/ui/buttons/purple.png` | 192 x 48 | button background | Alpha optional |
| Navy button skin | `/assets/pixel/ui/buttons/navy.png` | 192 x 48 | button background | Alpha optional |
| Danger button skin | `/assets/pixel/ui/buttons/danger.png` | 192 x 48 | button background | Alpha optional |

## Shipped runtime asset

| Role | Resource path | Source | Source dimensions | Runtime dimensions | Processing |
| --- | --- | --- | ---: | ---: | --- |
| Cozy room background | `/assets/pixel/world/cozy-room.png` | OpenAI image generation tool, using the local concept references as style and composition references | 1619 x 972 | 900 x 540 | Nearest-neighbor resize; fully opaque standalone scene |

The image contains the room layer only: it has no cat, collectible, hazard,
HUD, button, label, or text. Its generation provenance and licensing status
are recorded in `manifest.json`; no third-party licence is claimed for it.

The file list above is also defined in `com.dodgingcat.ui.PixelAssetPaths`.
PNG is required: the loader rejects non-PNG resource paths. The room background
is deliberately a single standalone scene, not a concept collage.

## Placement and animation rules

- Use transparent canvases for characters, food, hazards, and icons. Leave a
  transparent margin rather than changing the collision rectangle.
- Keep all cat frames exactly 28 x 23 with the feet aligned to the same bottom
  edge. `run-0` and `run-1` alternate at 10 frames per second; `idle`, `jump`,
  and `fall` are individual files. The renderer mirrors a frame for leftward
  movement, so do not create left-facing duplicates.
- Hazard and food images are drawn into their existing world/model rectangle.
  Image dimensions must never be used to modify entity width, height, position,
  movement, scoring, spawn safety, or collision detection.
- `GameCanvasRenderer` disables image smoothing before drawing. Keep hard pixel
  edges; avoid anti-aliased pixels that depend on fractional scaling.
- Use the upper-left of each sprite canvas as its drawing origin. Preserve the
  recommended canvas size if the artwork needs to remain visually centered in
  the current collision bound.

## Adding an asset safely

1. Put the verified PNG at its exact path in this table. Do not add a dummy or
   fake replacement solely to satisfy a missing path.
2. Check it at 2x in the running game. The fallback disappears automatically
   only for the resource that was supplied; other missing resources continue to
   fall back safely.
3. If a new kind of asset is needed, add its constant to `PixelAssetPaths` and
   a focused renderer or `UiKit` integration point. Do not expose PNG dimensions
   to the game model.
4. Record its exact source filename, original URL, author/publisher, license,
   modifications, display scale, and game use in `manifest.json`. Update
   `ATTRIBUTION.md` and retain the relevant license text where appropriate.
5. Run `mvn clean test` and `mvn clean compile` after integrating it.

## Current original fallback artwork

`com.dodgingcat.ui.PixelSpriteLibrary` creates the current sprite fallbacks as
in-memory `WritableImage` instances. Their pixels and palette are original work
in this codebase and are not imported third-party artwork; therefore they do
not require external attribution. They are temporary compatibility rendering,
not evidence that the approved Cozy Pixel Art concepts have been integrated.
