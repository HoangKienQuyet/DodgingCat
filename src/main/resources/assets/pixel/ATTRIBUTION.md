# Dodging Cat pixel-art attribution

## Current status

No third-party raster asset is shipped in this project as part of the Cozy
Pixel Art prototype. Consequently, there is no third-party runtime artwork to
attribute yet. The existing `PixelSpriteLibrary` images remain original,
runtime-generated placeholders from the earlier experiment; this document does
not present them as imported production art.

## Verified, not imported candidates

The following payloads were downloaded from their original publisher/author
pages into a temporary inspection folder on 2026-10-25. They were inspected,
then deliberately excluded from the repository because they cannot form the
required coherent cozy side-view set. No archive or extracted source file from
this list is included in runtime resources.

| Candidate | Publisher / author | Licence verified | Original source |
| --- | --- | --- | --- |
| Pixel Platformer 1.2 | Kenney | CC0 1.0; archive `License.txt` | [Kenney Pixel Platformer](https://kenney.nl/assets/pixel-platformer) |
| Pixel Pack | Kenney | CC0 1.0; archive `License.txt` | [Kenney Pixel Pack](https://kenney.nl/assets/pixel-pack) |
| Pixel UI Pack | Kenney Vleugels / Kenney, with help by Lynn Evers | CC0 1.0; archive `License.txt` | [Kenney Pixel UI Pack](https://kenney.nl/assets/pixel-ui-pack) |
| `cat_run.png` | alizard | CC0 1.0; original page | [OpenGameArt: pixel cat](https://opengameart.org/content/pixel-cat-0) |

The full inspection record, exact download URLs, SHA-256 values, files
inspected, and reasons for non-selection are in `manifest.json`.

## Requirement for future imports

Before an asset is copied under `src/main/resources/assets/pixel`, add an entry
to `manifest.json` and update this document with its exact source filename,
author/publisher, original URL, licence, modifications, and game use. Keep a
copy of the licence file when the source archive supplies one. Do not list an
asset as shipped until that exact file is present in the repository.
