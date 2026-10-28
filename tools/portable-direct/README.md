# Direct-source portable templates

[Tools index](../README.md) · [Portable guide](../../docs/portable-windows.md) · [Root README](../../README.md)

Used by [build-direct-portable.ps1](../../build-direct-portable.ps1), the current Windows x64 package builder. The script copies Run.cmd and Build.cmd to the package root; internal helpers, 7zr.exe and licenses go under support/. README-PORTABLE.txt becomes the package's README.txt.

| File | Role |
| --- | --- |
| Run.cmd | Prepare the runtime, then start Launcher with the bundled Java and resources from src/main/resources |
| Build.cmd / Compile.cmd | Prepare and compile main Java source with bundled javac, replace game-code.jar, keep the previous JAR |
| Prepare.ps1 | Check platform.sha256 and unpack platform.7z on first use |
| Resolve-root.ps1 | Resolve ASCII runtime cache when needed, synchronize source and record the original profile location |
| 7zr.exe | Bundled archive extractor; the builder also uses it to compress runtime/dependencies |
| Open-source.cmd | Older helper retained in templates; current builder does not include it because src is visible directly |
| licenses/ / THIRD-PARTY.txt | Third-party notices and supporting license/source materials |

These templates require generated support/platform.7z and the package layout. They cannot run the game directly from this template folder. See the portable guide for complete run/build steps. This README is for maintainers; the player quick guide is README-PORTABLE.txt.
