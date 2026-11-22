package daamky.ua.newmenu.video;

import daamky.ua.api.config.ConfigManager;
import daamky.ua.utils.sounds.SoundManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Main menu music + video session.
 *
 * The session starts the first time the main menu opens and keeps running over every other menu
 * screen (singleplayer, multiplayer, options...). It ends when the player joins a world or a
 * server, or on its own once the track is over. Music is played by the normal sound engine,
 * the video follows {@link #playbackSeconds()}, which is counted from the moment the music started.
 */
public final class MenuMedia {
    public static final MenuMedia INSTANCE = new MenuMedia();

    /** Second of the video at which the menu buttons spring in. */
    public static final float BUTTONS_AT_SECONDS = 15.0F;
    /** Length of mainmenu_theme.ogg; after that the video is only a still image. */
    private static final float MUSIC_SECONDS = 138.5F;
    private static final String VIDEO_RESOURCE = "/assets/daamkymenu/mainmenu/mainmenu.mp4";
    private static final float NEVER = 100000.0F;

    private enum State { IDLE, PREPARING, PLAYING, NO_VIDEO, ENDED }

    private State state = State.IDLE;
    private long startNano;
    private boolean hadFirstSession;
    private boolean sessionHasIntro;
    private boolean tickRegistered;
    private VideoDecoder decoder;
    private PositionedSoundInstance music;
    private int sessionId;

    private MenuMedia() {}

    /** Called whenever the main menu screen opens. Starts a session if none is running and the player is not in a world. */
    public void onMenuOpened() {
        registerTick();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.world != null) return;
        if (state == State.IDLE) beginSession(mc);
    }

    private void registerTick() {
        if (tickRegistered) return;
        tickRegistered = true;
        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
    }

    private void tick(MinecraftClient mc) {
        if (state == State.IDLE) return;
        if (mc.world != null || mc.player != null) {
            endSession(mc);
            return;
        }
        if (state == State.PLAYING || state == State.PREPARING) {
            // Keep the vanilla menu tracks quiet while ours plays.
            try {
                mc.getMusicTracker().stop();
            } catch (Throwable ignored) {
                // The menu must never fail because of the vanilla music tracker.
            }
        }
        if (state == State.PLAYING && playbackSeconds() > MUSIC_SECONDS + 2.0F) {
            finishNaturally();
        }
    }

    private void beginSession(MinecraftClient mc) {
        state = State.PREPARING;
        int id = ++sessionId;
        sessionHasIntro = !hadFirstSession;
        hadFirstSession = true;
        Thread worker = new Thread(() -> {
            File video = prepareVideoFile();
            mc.execute(() -> {
                if (id != sessionId || state != State.PREPARING) return;
                startPlayback(mc, video);
            });
        }, "daamky-menu-media-prepare");
        worker.setDaemon(true);
        worker.start();
    }

    private void startPlayback(MinecraftClient mc, File video) {
        startNano = System.nanoTime();
        playMusic(mc);
        if (video == null) {
            sessionHasIntro = false;
            state = State.NO_VIDEO;
            return;
        }
        decoder = new VideoDecoder(video, this::mediaNanos);
        decoder.start();
        state = State.PLAYING;
    }

    private void playMusic(MinecraftClient mc) {
        try {
            music = PositionedSoundInstance.ui(SoundManager.MENU_THEME, 1.0F, 0.85F);
            mc.getSoundManager().play(music);
        } catch (Throwable ignored) {
            music = null;
        }
    }

    private void finishNaturally() {
        stopDecoder();
        state = State.ENDED;
    }

    private void endSession(MinecraftClient mc) {
        sessionId++;
        stopDecoder();
        if (music != null) {
            try {
                mc.getSoundManager().stop(music);
            } catch (Throwable ignored) {
                // Nothing to stop.
            }
            music = null;
        }
        state = State.IDLE;
    }

    private void stopDecoder() {
        if (decoder != null) {
            decoder.stop();
            decoder = null;
        }
    }

    private long mediaNanos() {
        return state == State.PLAYING ? System.nanoTime() - startNano : -1L;
    }

    /** Seconds since the music started, or -1 when no session is playing. */
    public float playbackSeconds() {
        return state == State.PLAYING || state == State.NO_VIDEO || state == State.ENDED
                ? (System.nanoTime() - startNano) / 1_000_000_000.0F : -1.0F;
    }

    /**
     * Clock for the buttons' spring-in. While the file is being prepared it stays at zero; without a video,
     * after the video ended, or on every session after the first one the buttons are already there.
     */
    public float buttonsClock() {
        if (state == State.PREPARING) return 0.0F;
        if (!sessionHasIntro || state != State.PLAYING) return NEVER;
        return playbackSeconds();
    }

    /** False when no session exists (for example the menu was opened without one); the still image is shown then. */
    public boolean hasSession() {
        return state != State.IDLE;
    }

    /** The decoder of the running video, or null while there is no video (the still background is used then). */
    public VideoDecoder decoder() {
        return state == State.PLAYING ? decoder : null;
    }

    /** True once the video can no longer provide frames (finished or failed): the renderer fades to the still image. */
    public boolean videoOver() {
        return state == State.NO_VIDEO || state == State.ENDED
                || (state == State.PLAYING && decoder != null && (decoder.isFinished() || decoder.isFailed()));
    }

    /** Copies the bundled video next to the configs once (and again if its size changed). Returns null on failure. */
    private static File prepareVideoFile() {
        try {
            java.net.URL url = MenuMedia.class.getResource(VIDEO_RESOURCE);
            if (url == null) return null;
            long expected = url.openConnection().getContentLengthLong();
            Path dir = ConfigManager.systemDirectory();
            Files.createDirectories(dir);
            Path target = dir.resolve("mainmenu.mp4");
            if (Files.exists(target) && expected > 0 && Files.size(target) == expected) return target.toFile();
            Path tmp = dir.resolve("mainmenu.mp4.part");
            try (InputStream in = url.openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toFile();
        } catch (Throwable t) {
            return null;
        }
    }
}
