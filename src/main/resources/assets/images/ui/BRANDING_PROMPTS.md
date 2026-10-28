# Illustrated branding assets

Title generated on 2026-10-27; team logo lettering corrected on 2026-10-28 with the built-in `image_gen` tool in transparent edit mode. The existing title and logo were explicitly replaced at the user's request. No API/CLI fallback was used.

Final assets:

- `title_dodging_cat.png`: 2161 × 728 PNG, genuine alpha transparency. Visible alpha bounds: x=51..2128, y=145..561.
- `team_tadq.png`: 1254 × 1254 RGBA PNG, genuine alpha transparency, painted orange kitten portrait and exact `TADQ` lettering. Full resolution is retained.

The renderer should fit the nontransparent content rather than the complete canvas, preserving the generated alpha. `tools/GenerateUiArt.java` deliberately leaves these two files untouched.

## Title prompt

Use case: style-transfer / logo-brand.
Asset type: production transparent PNG game title wordmark.
Edit target: first image, current plain title "Dodging Cat". Replace its visual design completely while retaining the exact title text.
Reference images: second image is the game's ginger cat sprite sheet (style reference only, do not reproduce a sheet); third image is the game's warm hand-painted kitchen background (style and palette reference only).
Primary request: create a beautiful, professionally illustrated title reading exactly "Dodging Cat" in ONE horizontal line. Hand-painted whimsical storybook lettering with rounded irregular carved shapes, subtle soft painted shading and warm amber highlights, ivory cream letter faces and rich chocolate/terracotta outline. This should belong to the same polished cute 2D painted game as the orange cat and kitchen. Make the lettering bespoke and playful, with a restrained feline character cue such as tiny cat ears integrated into the C and a curved tail terminal; prioritize legibility and elegant composition.
Composition: wide wordmark, all letters fully visible, balanced letter spacing, substantial solid readable letter forms, minimal surrounding transparent margin, no rectangular board, no background scene, no full cat character. Transparent holes and outside background. Centered composition with gently dimensional painted volume, no metallic fantasy bevels.
Text (verbatim): "Dodging Cat". Preserve capitalization D and C; spelling D-o-d-g-i-n-g space C-a-t.
Constraints: genuine transparent background alpha, clean anti-aliased edges, suitable for display around 660 by 170 pixels but generate at high resolution. Only the title artwork; no watermark, no subtitle, no border frame, no flat generic computer typography, no neon, no black or white opaque backdrop.

Inputs in order: previous `ui/title_dodging_cat.png`, `characters/ginger_sheet.png`, `backgrounds/kitchen.png`.

## TADQ logo correction prompt

Use case: text-localization / precise in-image lettering correction.
Edit target: the existing illustrated game team emblem.
Primary request: correct the team name to exactly TADQ, uppercase letters in this order: T-A-D-Q.
Render the exact text "TADQ" in the same chunky hand-painted warm ivory/gold lettering, chocolate-brown dimensional outline and soft shading as the current lettering. Make the D and Q distinct and unmistakable, with a clear short diagonal tail on the Q. Fit all four letters comfortably within the lower badge, fully visible with balanced spacing.
Preserve the orange kitten portrait, expression, eyes, pose, brushwork, circular brown and gold frame, proportions and compact square composition. Preserve the current polished painted cartoon game art style and high resolution. Do not introduce extra words or decoration. Genuine alpha transparency outside the badge, no opaque backdrop and no checkerboard pixels.

The input was the previous illustrated emblem; the final project asset is `ui/team_tadq.png`.
