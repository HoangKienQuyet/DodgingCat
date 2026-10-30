# Development and packaging tools

[Documentation index](../docs/README.md) · [English README](../README.md) · [README tiếng Việt](../README_vi.md)

| Tool | Purpose | How to use |
| --- | --- | --- |
| [GenerateUiArt.java](GenerateUiArt.java) | Generates button plaque/hover and raster glyph atlas + metadata from Java source characters | From repo root: `java tools/GenerateUiArt.java` |
| [GenerateCountdownArt.java](GenerateCountdownArt.java) | Generates 3/2/1 and English/Vietnamese GO PNGs | From repo root: `java tools/GenerateCountdownArt.java` |
| [portable-direct](portable-direct/README.md) | Templates for the current direct-source portable package | Invoked by root `build-direct-portable.ps1` |
| [portable](portable/README.md) | Templates for the earlier lossless payload package | Invoked by root `build-portable.ps1` |

Art generators require a JDK and **overwrite generated image files**; normal run/build does not invoke them. GenerateUiArt uses Georgia/Arial at generation time to bake glyph images, so output can depend on fonts installed on the generation machine. Runtime labels use those images, not system glyph rendering. The generators do not redraw the illustrated title or TADQ logo. [Image guide](../src/main/resources/assets/images/README.md) describes formats and [branding record](../src/main/resources/assets/images/ui/BRANDING_PROMPTS.md) keeps the prompts.

Portable template commands are not standalone launchers from inside this tools folder: they expect the layout created by their matching build script. Use [portable-windows.md](../docs/portable-windows.md) for the current package, prerequisites and LMS size checks. Legal Markdown under `portable-direct/licenses/` belongs to third-party notices and must be preserved separately from game documentation.
