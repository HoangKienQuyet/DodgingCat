# Audio asset guide

[Documentation index](../../../../../docs/README.md) · [English README](../../../../../README.md) · [README tiếng Việt](../../../../../README_vi.md) · [Image guide](../images/README.md)

[AudioResources](../../../java/com/dodgingcat/audio/AudioResources.java) resolves logical names in bgm/ and sfx/. Both WAV and MP3 are supported: it strips either extension, tries `<stem>.mp3` first, then `<stem>.wav`. Keep exact base names and capitalization when converting; callers that still say spin.wav also resolve spin.mp3. If both exist, MP3 wins but both occupy package space.

## Current files and routing

All six BGM files currently use MP3: BGM-menu, kitchen_Pixabay, Living room, Bedroom, Garden and Rooftop. All 25 SFX currently use WAV:

```text
bomb_explode, buff_collect, cannon_shot, congrat, countdown_tick,
cyber_move, fish_collect, game_over, hazard_warning, jump, land,
laser_shot, movement, pendulum_swing, platform_crumble, player_death,
record_new, revive, santa_move, shield_activation, shield_break,
spin, spirit_move, ui_hover, ui_select
```

[Shop/loadout](../../../../../docs/shop-and-loadout.md) contains the BGM routing table and gacha sequence. Menu/loadout/leaderboard/about/skin shop use menu music; map preview and active gameplay use map music. Pause/Game Over stop BGM. Normal movement uses repeated footsteps; Neon/Spirit/Santa have their own movement sounds. Shield activation uses a looping sound while a shield is present.

## Playback implementation

Music streams through JavaFX MediaPlayer. WAV SFX decode with Java Sound; MP3 SFX decode with pure-Java JLayer into PCM and use the same cached Clip voice pool. Decode and file/Clip operations run on the audio worker; delayed/loop events have a scheduler. At most three ordinary voices per sound are reused. Requests may be skipped while muted, at zero volume or when the SFX queue is overloaded; a missing device/sound should not freeze gameplay.

Tracked gacha SFX uses its own playback progress/end. The current spin.wav is 11,312s; congrat.wav is 4,488s. Spin landing waits for spin playback completion, then the reveal sequence runs; the result box lasts 2s or click/Esc, independently of congrat's remaining duration. When silent or playback fails, tracked playback advances by decoded duration (3,2s fallback if no sample loads). This prevents a muted shop from waiting forever.

## Converting/replacing audio

Convert a selected file to the same stem.mp3, listen for clipping/silence/encoder delay and test its actual use, then remove the obsolete WAV if the conversion is accepted. Restart the game to clear decoded caches. MP3 is lossy: support for the format does not mean every conversion preserves WAV fidelity. Do not add silence to spin, because it changes how long the reel waits. Keep source/creator/license attribution; the filename or a download site's name alone does not establish reuse rights.
