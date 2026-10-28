# Rendering and frame pacing measurements — 2026-10-27

[History index](README.md) · [Current documentation](../README.md) · [Current performance](../performance.md) · [Current packaging](../portable-windows.md)

> Historical record only / Hồ sơ lịch sử. Measurements, counts, paths and descriptions below belong to the experiment when recorded; they are not current configuration or guaranteed available artifacts. Current fish spawn is 4s, render is native Java2D and test counts are in the current verification guide. The old 720p/decode-192px choices and backup paths must not be used as present-day instructions.

Run the opt-in probe by itself (it starts a JavaFX toolkit):

```powershell
mvn -q '-Dtest=PerformanceProbeTest' '-Ddodgingcat.perfProbe=true' test
```

The probe writes `target/performance-probe.txt`. It first observes two seconds of unloaded JavaFX pulses and compares the old exact 16.666666 ms frame gate with the available pulses. It then draws a 1920×1080 Canvas containing the Kitchen background, 24 fish, all five buff icons, an animated Ginger cat, and a raster HUD. Every frame forces a Canvas snapshot into a reusable image, which includes rendering and GPU readback rather than only measuring Java method submission time. Assets are decoded for four seconds before observation.

| Measurement | Before | After |
| --- | ---: | ---: |
| Draw and forced readback, median | 260.67 ms | 25.72 ms |
| Draw and forced readback, 95th percentile | 320.31 ms | 29.74 ms |
| Available unloaded JavaFX pulses | — | 62.99 per second |
| Frames accepted by the old gate on those same pulses | — | 32.49 per second (65 of 126 pulses) |

The old gate rejected pulses arriving slightly sooner than 16.666666 ms and accepted roughly half of the pulses on this machine. The app now updates and draws once per JavaFX AnimationTimer pulse without that second gate.

Pickup and buff textures previously decoded at about 1400 pixels despite typically appearing at 30–60 pixels. They now decode at a maximum edge of 192 pixels, while source art remains intact. Alpha bounds are scaled to the decoded dimensions. Character foot baselines and hazard frame bounds are prepared outside the JavaFX thread, and the fallback foot search stops at the first opaque row from the bottom. Raster text is cached as complete strings, reducing repeated Canvas image commands. Sound effect voices reuse decoded samples and native clips; looping clip opening and closing also run on the audio worker.

These figures are a controlled rendering diagnostic, not live game FPS. Forced snapshots add GPU readback work that the game does not normally perform. The first frame also includes texture upload and initialization (418.67 ms in the final run); the diagnostic starts directly in its stress scene rather than going through the normal boot and menu. A long interactive gameplay session on the user's display is still needed to characterize the full distribution of frame times under actual play.
