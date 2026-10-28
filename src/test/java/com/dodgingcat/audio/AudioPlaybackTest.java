package com.dodgingcat.audio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/** Real-time opt-in check, run alone so it cannot change another UI test's audio settings. */
@EnabledIfSystemProperty(named = "dodgingcat.audioProbe", matches = "true")
class AudioPlaybackTest {
    @Test void spinPlaybackFinishesAtTheWavEnd() throws Exception {
        var audio = AudioManager.getInstance();
        audio.setMuted(false);
        audio.setSfxVolume(.001); // Exercise the native mixer without audible volume.
        // Simulate converting an SFX to MP3 while its call site still requests WAV.
        var fixture = java.nio.file.Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                .resolve("assets/audio/sfx/format_probe.mp3");
        java.nio.file.Files.createDirectories(fixture.getParent());
        try (var input = getClass().getResourceAsStream("/assets/audio/bgm/BGM-menu.mp3")) {
            java.nio.file.Files.write(fixture, input.readNBytes(65536));
        }
        try {
            var converted = audio.playSfxTracked("format_probe.wav");
            long readyDeadline = System.nanoTime() + 8_000_000_000L;
            while (!converted.isReady() && System.nanoTime() < readyDeadline) Thread.sleep(10);
            assertTrue(converted.isReady());
            var nativeClip = converted.getClass().getDeclaredField("clip"); nativeClip.setAccessible(true);
            assertNotNull(nativeClip.get(converted), "Converted MP3 must use a native decoded PCM clip");
            long playbackDeadline = System.nanoTime() + 3_000_000_000L;
            while (converted.getProgress() <= 0 && System.nanoTime() < playbackDeadline) Thread.sleep(20);
            assertTrue(converted.getProgress() > 0, "Decoded MP3 must advance on the audio device");
            converted.cancel();
        } finally { java.nio.file.Files.deleteIfExists(fixture); }
        var playback = audio.playSfxTracked("spin.wav");
        long start = System.nanoTime(), previous = start;
        try {
            while (!playback.isReady() && System.nanoTime() - start < 3_000_000_000L) Thread.sleep(10);
            assertTrue(playback.isReady());
            assertEquals(11.312, playback.getDurationSeconds(), .001);
            start = previous = System.nanoTime();
            double progress = 0;
            while (!playback.isFinished() && System.nanoTime() - start < 15_000_000_000L) {
                long now = System.nanoTime();
                playback.update((now - previous) / 1e9); previous = now;
                assertTrue(playback.getProgress() >= progress, "Audio position must not run backwards");
                progress = playback.getProgress();
                Thread.sleep(15);
            }
            assertTrue(playback.isFinished(), "A completed native clip must release the spin phase");
            assertTrue((System.nanoTime() - start) / 1e9 >= 10.9, "Spin must not end before its complete WAV");
            assertEquals(1, playback.getProgress());
            var clip = playback.getClass().getDeclaredField("clip"); clip.setAccessible(true);
            System.out.printf("Spin audio: %.3fs, %s%n", (System.nanoTime() - start) / 1e9,
                    clip.get(playback) == null ? "silent fallback" : "native Java Sound clip");
        } finally { playback.cancel(); audio.shutdown(); }
    }
}
