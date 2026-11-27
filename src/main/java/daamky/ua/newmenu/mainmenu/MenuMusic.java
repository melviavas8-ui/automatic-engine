package daamky.ua.newmenu.mainmenu;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * Looping background music of the main menu ({@code menu_music.ogg}).
 *
 * <p>Plays through the OpenAL context Minecraft already created (same approach as the Music Lyrics module),
 * fades in when the menu opens and fades out when a world is joined. The music keeps playing on the vanilla
 * sub-screens (world list, server list, options) because it is driven by the client tick, not by a screen.
 */
public final class MenuMusic {
    private static final String RESOURCE = "/assets/daamkymenu/mainmenu/menu_music.ogg";
    /** Loudness of the menu music, 0..1. */
    public static final float VOLUME = 0.40F;
    private static final float FADE_IN_SECONDS = 2.0F;
    private static final float FADE_OUT_SECONDS = 0.7F;
    private static final int BUFFER_COUNT = 4;
    private static final int MAX_ATTEMPTS = 8;

    private static boolean hooked;
    private static boolean wanted;
    private static boolean playing;
    private static boolean stopping;
    private static int attempts;
    private static long retryAtNanos;
    private static long fadeStartNanos;

    private static ByteBuffer data;
    private static long decoder;
    private static int source;
    private static int[] buffers = new int[0];
    private static int channels;
    private static int sampleRate;
    private static ShortBuffer pcm;

    private MenuMusic() {
    }

    /** Call when the main menu opens. Safe to call repeatedly. */
    public static void start() {
        wanted = true;
        stopping = false;
        if (!hooked) {
            hooked = true;
            ClientTickEvents.END_CLIENT_TICK.register((ClientTickEvents.EndTick) MenuMusic::onClientTick);
        }
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.level == null && !playing) tryOpen();
    }

    /** Fades the music out and stops it. */
    public static void stop() {
        wanted = false;
        beginStop();
    }

    private static void beginStop() {
        if (playing && !stopping) {
            stopping = true;
            fadeStartNanos = System.nanoTime();
        }
    }

    private static void onClientTick(Minecraft client) {
        if (!wanted && !playing) return;
        if (client.level != null || client.player != null) {
            // a game was joined: the menu is gone
            wanted = false;
            beginStop();
        }
        if (wanted && !playing) tryOpen();
        if (!playing) return;
        try {
            update();
        } catch (Throwable throwable) {
            close();
        }
    }

    private static void tryOpen() {
        if (attempts >= MAX_ATTEMPTS || System.nanoTime() < retryAtNanos) return;
        try {
            open();
            playing = true;
            stopping = false;
            fadeStartNanos = System.nanoTime();
        } catch (Throwable throwable) {
            close();
            attempts++;
            // the sound engine may not be ready yet right after the game starts, so try again a bit later
            retryAtNanos = System.nanoTime() + 2_000_000_000L;
            if (attempts == MAX_ATTEMPTS) System.err.println("[Daamky] Menu music disabled: " + throwable);
        }
    }

    private static void open() throws Exception {
        byte[] bytes;
        try (InputStream in = MenuMusic.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException(RESOURCE + " not found");
            bytes = in.readAllBytes();
        }
        data = MemoryUtil.memAlloc(bytes.length);
        data.put(bytes);
        data.flip();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            decoder = STBVorbis.stb_vorbis_open_memory(data, error, null);
            if (decoder == 0L) throw new IllegalStateException("stb_vorbis error " + error.get(0));
        }
        STBVorbisInfo info = STBVorbisInfo.malloc();
        try {
            STBVorbis.stb_vorbis_get_info(decoder, info);
            channels = info.channels();
            sampleRate = info.sample_rate();
        } finally {
            info.free();
        }
        if (channels < 1 || channels > 2 || sampleRate <= 0) throw new IllegalStateException("unsupported OGG");

        pcm = BufferUtils.createShortBuffer(Math.max(4096, sampleRate) * channels);
        source = AL10.alGenSources();
        AL10.alSourcef(source, AL10.AL_GAIN, 0.0F);
        buffers = new int[BUFFER_COUNT];
        for (int i = 0; i < BUFFER_COUNT; i++) buffers[i] = AL10.alGenBuffers();
        for (int buffer : buffers) {
            if (!fill(buffer)) break;
            AL10.alSourceQueueBuffers(source, buffer);
        }
        if (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) == 0) throw new IllegalStateException("empty OGG");
        AL10.alSourcePlay(source);
    }

    private static void update() {
        float seconds = (System.nanoTime() - fadeStartNanos) / 1_000_000_000.0F;
        float gain;
        if (stopping) {
            float t = seconds / FADE_OUT_SECONDS;
            if (t >= 1.0F) {
                close();
                return;
            }
            gain = VOLUME * (1.0F - t);
        } else {
            gain = VOLUME * Math.min(1.0F, seconds / FADE_IN_SECONDS);
        }
        AL10.alSourcef(source, AL10.AL_GAIN, gain);

        int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            if (fill(buffer)) AL10.alSourceQueueBuffers(source, buffer);
        }
        if (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) > 0
                && AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
            AL10.alSourcePlay(source);
        }
    }

    /** Decodes the next chunk; restarts the track from the beginning when it ends (loop). */
    private static boolean fill(int buffer) {
        for (int attempt = 0; attempt < 2; attempt++) {
            pcm.clear();
            int samples = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            if (samples > 0) {
                // STB writes into the native memory without moving the Java position, so the limit is set by hand
                pcm.position(0);
                pcm.limit(samples * channels);
                int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
                AL10.alBufferData(buffer, format, pcm, sampleRate);
                return true;
            }
            STBVorbis.stb_vorbis_seek_start(decoder);
        }
        return false;
    }

    private static void close() {
        try {
            if (source != 0) {
                AL10.alSourceStop(source);
                int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
                while (queued-- > 0) AL10.alSourceUnqueueBuffers(source);
                AL10.alDeleteSources(source);
            }
            for (int buffer : buffers) if (buffer != 0) AL10.alDeleteBuffers(buffer);
        } catch (Throwable ignored) {
        }
        source = 0;
        buffers = new int[0];
        pcm = null;
        if (decoder != 0L) {
            STBVorbis.stb_vorbis_close(decoder);
            decoder = 0L;
        }
        if (data != null) {
            MemoryUtil.memFree(data);
            data = null;
        }
        playing = false;
        stopping = false;
    }
}
