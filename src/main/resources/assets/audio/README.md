# Audio assets

Music is read from `bgm/`, sound effects from `sfx/`. Both WAV and MP3 are
supported. Keep the same base name when converting, for example
`sfx/spin.wav` to `sfx/spin.mp3`; no Java call sites need changing.
If both files exist, MP3 takes priority. Remove the obsolete WAV after checking
the converted audio, otherwise both files still take space in the package.

The menu uses `bgm/BGM-menu.mp3`; Garden uses `bgm/Garden.mp3`.
The other maps keep their existing names.

Music streams through JavaFX MediaPlayer. WAV SFX use Java Sound; MP3 SFX decode
once with the bundled pure Java JLayer library and then use the same cached PCM
voice pool. Looping, volume, delayed effects, and tracked gacha playback work
with either format. Decode/file I/O runs on the sound worker, not the game loop.

Avoid adding silence at either end of spin audio: the reel uses its actual full
playback duration before showing the congratulations box. MP3 conversion can
introduce a small encoder delay; verify the converted track before submission.

Retain source/creator/license attribution for all downloaded audio.