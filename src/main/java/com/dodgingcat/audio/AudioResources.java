package com.dodgingcat.audio;

import java.net.URL;
import java.util.function.Function;

/** A logical sound keeps its name when its file is converted between WAV and MP3. */
final class AudioResources {
    private AudioResources() {}

    static URL resolve(String folder, String name) {
        return resolve(folder, name, AudioResources.class::getResource);
    }

    static URL resolve(String folder, String name, Function<String, URL> lookup) {
        String stem = name.replaceFirst("(?i)\\.(wav|mp3)$", "");
        // Prefer the converted file when the old WAV has also been left in place.
        URL mp3 = lookup.apply("/assets/audio/" + folder + "/" + stem + ".mp3");
        return mp3 != null ? mp3 : lookup.apply("/assets/audio/" + folder + "/" + stem + ".wav");
    }
}
