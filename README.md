# Dodging Cat

[Tiếng Việt](README_vi.md) · [Documentation index](docs/README.md)

Dodging Cat is a Java desktop survival platformer with single player and local two-player co-op. Dodge falling objects, lasers, cannonballs and swinging/orbiting weights; collect buffs and fish; unlock skins through gacha and buy additional maps.

The default renderer is **Java2D/AWT at native pixel resolution**, paced at 60 FPS. JavaFX is still required for image/media services and the optional JavaFX renderer. This is not a JDK-only application. Build versions come from [pom.xml](pom.xml): **JDK 25**, JavaFX 25, Gson 2.13.2, JLayer 1.0.1 and JUnit 5.10.2. The Maven artifact version is 1.0.0.

## Run and test

Run these commands from the folder containing `pom.xml`, with JDK 25 and Maven available:

```powershell
mvn compile javafx:run
mvn test
```

On Windows, double-click `run.cmd` or `test.cmd`. These call `dev.ps1`, which finds an installed JDK and uses an installed Maven or downloads Maven 3.9.11 when needed. Dependencies need Internet access on the first Maven build. The helper does not install a JDK; the project requires release 25 even though an older helper error message mentions 23+.

For a demo without installed Java or Maven, use the separately built [Windows x64 portable package](docs/portable-windows.md). Extract it fully, open `Run.cmd`, and use `Build.cmd` after editing Java source. A Git clone alone does not contain that package's runtime.

## Play

The startup sequence displays the title for 2 seconds and TADQ logo for 2 seconds once rendering starts. The menu shows the last map on which a run was started, without a cat mascot. Click **1P** or **2P**, choose a map and skin in loadout, then click Start. A three-second `3 → 2 → 1 → GO` countdown precedes simulation.

| Action | Player 1 | Player 2 |
| --- | --- | --- |
| Move | A / D | Left / Right |
| Jump | W | Up |
| Drop through elevated platforms | S | Down |

Use the mouse for menu buttons and map selection. In 2P skin selection, P1 uses WASD + Space, P2 uses arrows + Enter; the mouse wheel scrolls the grid and clicking a skin changes P1. `Tab` switches loadout tabs. `Esc` or `P` pauses gameplay; `R` restarts. `F11` toggles fullscreen; `F10` toggles FPS information.

**F9 is a development shortcut:** the first press refills the wallet to 1,000,000 fish while enabled; the next press replaces the profile with first-player defaults, including unlocks, wallet and leaderboards.

Score grows by 10 points per simulation second, or 20 while a living player has Double Score. Every collected buff adds 50 points. Both players share the run score and fish count; co-op ends when both cats are dead. Fish and the leaderboard entry are saved once at Game Over. Restarting, quitting or returning to menu before that discards the unfinished run's rewards. Normal saves are in `%USERPROFILE%/.dodgingcat/dodging_cat_profile.json`; portable saves are beside that copy in `data/profile.json`.

## Documentation map

Detailed guides are primarily in Vietnamese, using the same class names and IDs as the source. Each guide links back to this index.

| Topic | Guide |
| --- | --- |
| Installation, controls, save/reset behavior, test commands | [Run, play and test](docs/running-and-testing.md) |
| States, render loop, collision and fairness rules | [Architecture and source map](docs/architecture.md) |
| Buff rates/cooldowns, currency, hazards, platforms and scoring | [Gameplay parameters](docs/gameplay-thong-so.md) |
| All 15 skins, five maps, prices, odds and reveal sequence | [Shop, gacha and loadout](docs/shop-and-loadout.md) |
| Native rendering, measurement limits and current verification | [Performance and verification](docs/performance.md) |
| Offline Windows demo and submission packaging | [Portable package](docs/portable-windows.md) |
| Image grids, raster lettering, alpha bounds and branding | [Image assets](src/main/resources/assets/images/README.md) |
| WAV/MP3 names, playback and conversion rules | [Audio assets](src/main/resources/assets/audio/README.md) |
| Art generators, packaging scripts and helper templates | [Tools](tools/README.md) |
| Older performance/packaging experiments | [Historical records](docs/history/README.md) |

## Repository layout

```text
src/main/java/com/dodgingcat/   application, simulation, UI, generation, audio, saves
src/main/resources/           game images, audio and legacy JavaFX CSS
src/test/java/com/dodgingcat/  logic tests, UI snapshots and opt-in desktop probes
docs/                        detailed guides and historical evidence
tools/                       art generators and portable script templates
pom.xml                      Maven build and dependency versions
```

`target/`, `.git/`, IDE caches and `deliverables/` are not part of a source submission. Generated portable archives, the final report and slides are not stored in this branch. See the packaging guide for the one-file 100 MB LMS constraint supplied by the team.

## Team TADQ

| Student ID | Name |
| --- | --- |
| 2301040209 | Hoang Kien Quyet |
| 2301040013 | Tran Quang Anh |
| 2301040042 | Nguyen Cong Doan |
| 2301040171 | Tran Gia Thanh |

All four members developed separate game prototypes. The group selected this version, developed by Tran Quang Anh, as its submission base. Later integration, tests, documentation and packaging contributions must be recorded from the work each member actually completes; this README does not assign unverified implementation responsibilities.

Documentation checked against the source on **28 October 2026**. Performance figures are measurements on a particular machine, not a promise of 60 FPS on every computer. Runtime behavior and active code take precedence over old comments and historical experiment reports.
