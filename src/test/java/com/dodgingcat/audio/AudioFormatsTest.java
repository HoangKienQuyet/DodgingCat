package com.dodgingcat.audio;

import org.junit.jupiter.api.Test;
import java.net.URI;
import static org.junit.jupiter.api.Assertions.*;

class AudioFormatsTest {
    @Test void convertedMp3IsPreferredAndWavStillWorksWithEitherRequestedExtension() throws Exception {
        var mp3 = URI.create("file:/audio/spin.mp3").toURL();
        var wav = URI.create("file:/audio/spin.wav").toURL();
        assertEquals(mp3, AudioResources.resolve("sfx", "spin.wav", p -> p.endsWith(".mp3") ? mp3 : wav));
        assertEquals(wav, AudioResources.resolve("sfx", "spin.mp3", p -> p.endsWith(".wav") ? wav : null));
        assertNull(AudioResources.resolve("sfx", "missing.wav", p -> null));
    }

    @Test void actualMp3AndWavDecodeIntoPlayablePcmWithoutAnAudioDevice() throws Exception {
        for (String resource : new String[]{"/assets/audio/bgm/BGM-menu.mp3", "/assets/audio/bgm/Garden.mp3", "/assets/audio/sfx/spin.wav"}) {
            var sample = AudioManager.decodeSample(getClass().getResource(resource));
            assertTrue(sample.bytes().length > 0);
            assertTrue(sample.format().getSampleRate() > 0);
            assertTrue(sample.format().getChannels() > 0);
            assertEquals(0, sample.bytes().length % sample.format().getFrameSize());
        }
    }
}
