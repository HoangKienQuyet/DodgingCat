package com.dodgingcat.audio;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.AudioFormat;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Uses JavaFX MediaPlayer for BGM formats such as MP3 and Java Sound clips for SFX.
 */
public class AudioManager {
    private static final AudioManager INSTANCE = new AudioManager();

    private volatile double bgmVolume = 0.7;
    private volatile double sfxVolume = 0.8;
    private volatile boolean muted = false;

    private volatile MediaPlayer currentBgmPlayer = null;
    private volatile String currentBgmTrack = null;
    private String requestedBgm;
    private long bgmRequestId;
    private final Object bgmLock = new Object();
    private volatile Clip currentLoopingSfx = null;
    private volatile String currentLoopingSfxName = null;
    private final Object loopLock = new Object();
    private long loopRequestId;
    private volatile ScheduledFuture<?> delayedLoopStart;
    private volatile String delayedLoopStartName;
    private long delayedLoopRequest;
    private final java.util.concurrent.ScheduledExecutorService soundTimers = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "dodgingcat-sfx-timing"); thread.setDaemon(true); return thread;
    });
    record Sample(AudioFormat format, byte[] bytes) {}
    public final class SfxPlayback {
        private volatile Clip clip;
        private volatile boolean ready, silent, finished, cancelled;
        private volatile double durationSeconds = 3.2;
        private double silentElapsed;

        public void update(double dt) {
            if (!ready || finished) return;
            if (silent) {
                silentElapsed += Math.max(0, dt);
                if (silentElapsed >= durationSeconds) finished = true;
            }
        }
        public boolean isReady() { return ready; }
        public boolean isFinished() { return finished; }
        public double getDurationSeconds() { return durationSeconds; }
        public double getProgress() {
            if (finished) return 1.0;
            if (!ready) return 0.0;
            if (silent) return Math.clamp(silentElapsed / durationSeconds, 0, 1);
            Clip playing = clip;
            return playing == null ? 0 : Math.clamp(playing.getMicrosecondPosition() / (durationSeconds * 1_000_000), 0, 1);
        }
        public void cancel() {
            cancelled = true;
            finished = true;
            Clip playing = clip;
            if (playing != null && !sfxExecutor.isShutdown()) sfxExecutor.execute(() -> {
                playing.stop(); playing.close();
            });
        }
    }
    // Only the audio worker accesses sample/voice pools. Native mixer lines are reused
    // instead of opened and closed on every footstep, pickup, and hazard warning.
    private final Map<String, Sample> samples = new HashMap<>();
    private final Map<String, List<Clip>> voices = new HashMap<>();
    private final ThreadPoolExecutor sfxExecutor = new ThreadPoolExecutor(1, 1, 0L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), runnable -> {
                Thread worker = new Thread(runnable, "dodgingcat-sfx");
                worker.setDaemon(true);
                return worker;
            });

    private AudioManager() {}

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    public void playBgm(String trackName) {
        if (muted) return;
        requestBgm(trackName);
    }

    private void requestBgm(String trackName) {
        long request;
        synchronized (bgmLock) {
            if (java.util.Objects.equals(requestedBgm, trackName)) return;
            requestedBgm = trackName; request = ++bgmRequestId;
        }
        runOnMediaThread(() -> {
            synchronized (bgmLock) { if (request != bgmRequestId) return; }
            if (trackName == null) stopBgmOnMediaThread(); else playBgmOnMediaThread(trackName);
        });
    }

    private static void runOnMediaThread(Runnable action) {
        if (javafx.application.Platform.isFxApplicationThread()) action.run();
        else javafx.application.Platform.runLater(action);
    }

    private void playBgmOnMediaThread(String trackName) {
        if (trackName != null && trackName.equals(currentBgmTrack)
                && currentBgmPlayer != null) return;
        try {
            stopBgmOnMediaThread();
            URL resource = AudioResources.resolve("bgm", trackName);
            if (resource != null) {
                MediaPlayer player = new MediaPlayer(new Media(resource.toExternalForm()));
                currentBgmPlayer = player;
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setVolume(bgmVolume);
                player.setOnError(() -> {
                    if (currentBgmPlayer == player) {
                        player.dispose();
                        currentBgmPlayer = null;
                        currentBgmTrack = null;
                    }
                });
                player.play();
                currentBgmTrack = trackName;
            }
        } catch (Exception ignored) {
            // Graceful fallback if file is absent or format unsupported
        }
    }

    public void stopBgm() {
        requestBgm(null);
    }

    private void stopBgmOnMediaThread() {
        if (currentBgmPlayer != null) {
            try {
                currentBgmPlayer.stop();
                currentBgmPlayer.dispose();
            } catch (Exception ignored) {}
            currentBgmPlayer = null;
        }
        currentBgmTrack = null;
    }

    public void playSfx(String soundName) {
        if (muted || sfxVolume <= 0.0 || sfxExecutor.isShutdown() || sfxExecutor.getQueue().size() >= 32) return;
        double requestedVolume = sfxVolume;
        sfxExecutor.execute(() -> playSfxOnAudioThread(soundName, requestedVolume));
    }

    /** Decode the long spin WAV and prepare the congratulations voice during boot. */
    public void preloadShopSfx() {
        if (sfxExecutor.isShutdown()) return;
        sfxExecutor.execute(() -> {
            try {
                loadSample("spin.wav");
                List<Clip> pool = voices.computeIfAbsent("congrat.wav", ignored -> new ArrayList<>());
                if (pool.isEmpty()) {
                    Clip clip = openClip("congrat.wav");
                    if (clip != null) pool.add(clip);
                }
            } catch (Exception ignored) { }
        });
    }

    /** A dedicated one-shot with actual playback progress/end, also timed correctly when muted. */
    public SfxPlayback playSfxTracked(String soundName) {
        SfxPlayback playback = new SfxPlayback();
        if (sfxExecutor.isShutdown()) {
            playback.silent = true; playback.ready = true;
            return playback;
        }
        sfxExecutor.execute(() -> {
            try {
                Sample sample = loadSample(soundName);
                if (sample != null) playback.durationSeconds = sample.bytes.length
                        / (double) sample.format.getFrameSize() / sample.format.getFrameRate();
                if (playback.cancelled) return;
                if (sample == null || muted || sfxVolume <= 0) {
                    playback.silent = true; playback.ready = true;
                    return;
                }
                Clip clip = openClip(soundName);
                playback.clip = clip;
                clip.addLineListener(event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP
                            && (playback.cancelled || clip.getLongFramePosition() >= clip.getFrameLength() - 1)) {
                        playback.finished = true;
                        if (!sfxExecutor.isShutdown()) sfxExecutor.execute(clip::close);
                    }
                });
                setClipVolume(clip, sfxVolume);
                playback.ready = true;
                if (playback.cancelled) clip.close(); else clip.start();
            } catch (Exception ignored) {
                playback.silent = true;
                playback.ready = true;
            }
        });
        return playback;
    }

    private void playSfxOnAudioThread(String soundName, double requestedVolume) {
        if (muted) return;
        try {
            List<Clip> pool = voices.computeIfAbsent(soundName, ignored -> new ArrayList<>());
            Clip voice = pool.stream().filter(clip -> !clip.isRunning()).findFirst().orElse(null);
            if (voice == null && pool.size() < 3) {
                voice = openClip(soundName);
                if (voice != null) pool.add(voice);
            }
            if (voice == null) return;
            voice.stop();
            voice.setFramePosition(0);
            setClipVolume(voice, requestedVolume);
            voice.start();
        } catch (Exception ignored) {
            // Graceful fallback
        }
    }

    /** Immediately stops all in-flight instances of a named one-shot SFX. */
    public void stopSfx(String soundName) {
        if (sfxExecutor.isShutdown()) return;
        sfxExecutor.execute(() -> {
            List<Clip> pool = voices.get(soundName);
            if (pool == null) return;
            for (Clip clip : pool) {
                try { clip.stop(); clip.setFramePosition(0); } catch (Exception ignored) {}
            }
        });
    }

    private Sample loadSample(String soundName) throws Exception {
        Sample sample = samples.get(soundName);
        if (sample == null) {
            URL resource = AudioResources.resolve("sfx", soundName);
            if (resource == null) return null;
            sample = decodeSample(resource);
            samples.put(soundName, sample);
        }
        return sample;
    }

    static Sample decodeSample(URL resource) throws Exception {
        if (resource.getPath().toLowerCase(java.util.Locale.ROOT).endsWith(".mp3")) {
            try (InputStream raw = resource.openStream();
                 var pcm = new java.io.ByteArrayOutputStream()) {
                var bits = new javazoom.jl.decoder.Bitstream(new BufferedInputStream(raw));
                var decoder = new javazoom.jl.decoder.Decoder();
                AudioFormat format = null;
                try {
                    javazoom.jl.decoder.Header header;
                    while ((header = bits.readFrame()) != null) {
                        var frame = (javazoom.jl.decoder.SampleBuffer) decoder.decodeFrame(header, bits);
                        if (format == null) format = new AudioFormat(frame.getSampleFrequency(), 16, frame.getChannelCount(), true, false);
                        short[] samples = frame.getBuffer();
                        for (int i = 0; i < frame.getBufferLength(); i++) {
                            pcm.write(samples[i] & 255);
                            pcm.write((samples[i] >>> 8) & 255);
                        }
                        bits.closeFrame();
                    }
                } finally { bits.close(); }
                if (format == null) throw new java.io.IOException("No MP3 audio frames: " + resource);
                return new Sample(format, pcm.toByteArray());
            }
        } else {
            try (InputStream raw = resource.openStream();
                 InputStream buffered = new BufferedInputStream(raw);
                 AudioInputStream stream = AudioSystem.getAudioInputStream(buffered)) {
                return new Sample(stream.getFormat(), stream.readAllBytes());
            }
        }
    }

    private Clip openClip(String soundName) throws Exception {
        Sample sample = loadSample(soundName);
        if (sample == null) return null;
        Clip clip = AudioSystem.getClip();
        clip.open(sample.format, sample.bytes, 0, sample.bytes.length);
        return clip;
    }

    public void playSfxAfter(String soundName, double delaySeconds) {
        if (muted || sfxVolume <= 0 || soundTimers.isShutdown()) return;
        soundTimers.schedule(() -> playSfx(soundName), (long) (Math.max(0, delaySeconds) * 1e9), TimeUnit.NANOSECONDS);
    }

    public void playSfxTwice(String soundName, double gapSeconds) {
        playSfx(soundName);
        playSfxAfter(soundName, gapSeconds);
    }

    /** Starts one looping SFX instance; calling again with the same sound is a no-op. */
    public void startLoopingSfx(String soundName) {
        if (soundName == null || soundName.isBlank() || muted || sfxVolume <= 0.0 || sfxExecutor.isShutdown()) return;
        if (soundName.equals(currentLoopingSfxName)) return;
        stopLoopingSfx();
        long request;
        synchronized (loopLock) {
            request = ++loopRequestId;
            currentLoopingSfxName = soundName;
        }
        sfxExecutor.execute(() -> {
            try {
                Clip loop = openClip(soundName);
                synchronized (loopLock) {
                    if (loop == null || request != loopRequestId || muted) {
                        if (loop != null) loop.close();
                        return;
                    }
                    currentLoopingSfx = loop;
                }
                setClipVolume(loop, sfxVolume);
                loop.loop(Clip.LOOP_CONTINUOUSLY);
            } catch (Exception ignored) {
                synchronized (loopLock) {
                    if (request == loopRequestId) currentLoopingSfxName = null;
                }
            }
        });
    }

    public void startLoopingSfxAfter(String soundName, double delaySeconds) {
        cancelDelayedLoopStart();
        if (muted || sfxVolume <= 0 || soundTimers.isShutdown()) return;
        synchronized (loopLock) {
            long request = ++delayedLoopRequest;
            delayedLoopStartName = soundName;
            delayedLoopStart = soundTimers.schedule(() -> {
                synchronized (loopLock) {
                    if (request != delayedLoopRequest) return;
                    delayedLoopStart = null; delayedLoopStartName = null;
                    startLoopingSfx(soundName);
                }
            }, (long) (Math.max(0, delaySeconds) * 1e9), TimeUnit.NANOSECONDS);
        }
    }

    /** Keeps a looping sound aligned with a gameplay state such as any active shield. */
    public void syncLoopingSfx(String soundName, boolean shouldBePlaying) {
        if (shouldBePlaying) {
            if (soundName.equals(delayedLoopStartName)) return;
            if (!soundName.equals(currentLoopingSfxName)) {
                startLoopingSfx(soundName);
            }
        } else if (soundName.equals(currentLoopingSfxName) || delayedLoopStart != null) {
            stopLoopingSfx();
        }
    }

    public void stopLoopingSfx() {
        cancelDelayedLoopStart();
        Clip previous;
        synchronized (loopLock) {
            ++loopRequestId;
            previous = currentLoopingSfx;
            currentLoopingSfx = null;
            currentLoopingSfxName = null;
        }
        if (previous != null && !sfxExecutor.isShutdown()) sfxExecutor.execute(() -> {
            try { previous.stop(); previous.close(); } catch (Exception ignored) {}
        });
    }

    private void cancelDelayedLoopStart() {
        synchronized (loopLock) {
            delayedLoopRequest++;
            if (delayedLoopStart != null) delayedLoopStart.cancel(false);
            delayedLoopStart = null; delayedLoopStartName = null;
        }
    }

    private void setClipVolume(Clip clip, double linearVolume) {
        try {
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (linearVolume <= 0.001) {
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    float dB = (float) (Math.log10(linearVolume) * 20.0);
                    dB = Math.max(gainControl.getMinimum(), Math.min(dB, gainControl.getMaximum()));
                    gainControl.setValue(dB);
                }
            }
        } catch (Exception ignored) {}
    }

    public double getBgmVolume() {
        return bgmVolume;
    }

    public void setBgmVolume(double volume) {
        this.bgmVolume = Math.clamp(volume, 0.0, 1.0);
        if (currentBgmPlayer != null) {
            runOnMediaThread(() -> { if (currentBgmPlayer != null) currentBgmPlayer.setVolume(muted ? 0.0 : this.bgmVolume); });
        }
    }

    public double getSfxVolume() {
        return sfxVolume;
    }

    public void setSfxVolume(double volume) {
        this.sfxVolume = Math.clamp(volume, 0.0, 1.0);
        updateLoopingSfxVolume();
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
        if (currentBgmPlayer != null) runOnMediaThread(() -> {
            if (currentBgmPlayer == null) return;
            if (this.muted) currentBgmPlayer.pause();
            else { currentBgmPlayer.setVolume(bgmVolume); currentBgmPlayer.play(); }
        });
        else if (!muted) synchronized (bgmLock) { requestedBgm = null; }
        updateLoopingSfxVolume();
    }

    public void toggleMute() {
        setMuted(!muted);
    }

    private void updateLoopingSfxVolume() {
        Clip loop = currentLoopingSfx;
        if (loop != null && !sfxExecutor.isShutdown()) sfxExecutor.execute(() -> {
            if (loop.isOpen()) setClipVolume(loop, muted ? 0.0 : sfxVolume);
        });
    }

    public void shutdown() {
        stopBgm();
        stopLoopingSfx();
        soundTimers.shutdownNow();
        if (sfxExecutor.isShutdown()) return;
        sfxExecutor.execute(() -> {
            for (List<Clip> pool : voices.values()) {
                for (Clip clip : pool) {
                    try { clip.stop(); clip.close(); } catch (Exception ignored) {}
                }
            }
            voices.clear();
            samples.clear();
        });
        sfxExecutor.shutdown();
    }
}
