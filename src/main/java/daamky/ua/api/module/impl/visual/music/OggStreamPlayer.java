package daamky.ua.api.module.impl.visual.music;

import java.nio.ShortBuffer;
import java.nio.IntBuffer;
import org.lwjgl.system.MemoryStack;
import java.nio.file.Path;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;

/** Streams OGG/Vorbis into the already initialized Minecraft OpenAL context. */
public final class OggStreamPlayer implements AutoCloseable {
    private static final int BUFFER_COUNT = 4;
    private static final int CHUNK_SECONDS = 1;

    private long decoder;
    private int source;
    private int[] buffers = new int[0];
    private int channels;
    private int sampleRate;
    private boolean eof;
    private boolean started;
    private long durationMillis;
    private ShortBuffer pcm;

    public void open(Path file) {
        close();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer error = stack.mallocInt(1);
            decoder = STBVorbis.stb_vorbis_open_filename(file.toAbsolutePath().toString(), error, null);
            if (decoder == 0L) throw new IllegalArgumentException("Не удалось открыть OGG (Vorbis), код " + error.get(0));
        }

        STBVorbisInfo info = STBVorbisInfo.malloc();
        try {
            STBVorbis.stb_vorbis_get_info(decoder, info);
            channels = info.channels();
            sampleRate = info.sample_rate();
            int totalSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            durationMillis = totalSamples > 0 && sampleRate > 0
                ? Math.max(1L, (long) totalSamples * 1000L / sampleRate) : 0L;
        } finally {
            info.free();
        }
        if (channels < 1 || channels > 2 || sampleRate <= 0) {
            close();
            throw new IllegalArgumentException("Поддерживаются только mono/stereo OGG-файлы");
        }

        pcm = BufferUtils.createShortBuffer(Math.max(4096, sampleRate * CHUNK_SECONDS) * channels);
        source = AL10.alGenSources();
        buffers = new int[BUFFER_COUNT];
        for (int i = 0; i < BUFFER_COUNT; i++) buffers[i] = AL10.alGenBuffers();
        for (int buffer : buffers) {
            if (!fill(buffer)) break;
            AL10.alSourceQueueBuffers(source, buffer);
        }
        if (AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) == 0) {
            close();
            throw new IllegalArgumentException("В OGG нет аудиоданных");
        }
        AL10.alSourcePlay(source);
        started = true;
    }

    /** Updates the streaming queue. The return value only signals a drained decoder queue. */
    public boolean tick() {
        if (!started || source == 0) return true;
        int processed = AL10.alGetSourcei(source, AL10.AL_BUFFERS_PROCESSED);
        while (processed-- > 0) {
            int buffer = AL10.alSourceUnqueueBuffers(source);
            if (!eof && fill(buffer)) AL10.alSourceQueueBuffers(source, buffer);
        }
        int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
        if (queued == 0) return eof;
        int state = AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE);
        if (state != AL10.AL_PLAYING) AL10.alSourcePlay(source);
        return false;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isDrained() {
        if (!started || source == 0) return true;
        return eof && AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED) == 0;
    }

    private boolean fill(int buffer) {
        int maxSamples = Math.max(4096, sampleRate * CHUNK_SECONDS);
        if (this.pcm == null || this.pcm.capacity() < maxSamples * channels) return false;
        this.pcm.clear();
        int samples = STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, this.pcm);
        if (samples <= 0) {
            eof = true;
            return false;
        }
        // STB writes directly into the native ShortBuffer memory and does not advance
        // the Java position. Calling flip() here would therefore set limit to zero,
        // queueing silent OpenAL buffers. Expose exactly the decoded interleaved samples.
        this.pcm.position(0);
        this.pcm.limit(samples * channels);
        int format = channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16;
        AL10.alBufferData(buffer, format, this.pcm, sampleRate);
        return true;
    }

    @Override
    public void close() {
        if (source != 0) {
            AL10.alSourceStop(source);
            int queued = AL10.alGetSourcei(source, AL10.AL_BUFFERS_QUEUED);
            while (queued-- > 0) AL10.alSourceUnqueueBuffers(source);
            AL10.alDeleteSources(source);
            source = 0;
        }
        for (int buffer : buffers) if (buffer != 0) AL10.alDeleteBuffers(buffer);
        buffers = new int[0];
        pcm = null;
        if (decoder != 0L) {
            STBVorbis.stb_vorbis_close(decoder);
            decoder = 0L;
        }
        eof = false;
        started = false;
        durationMillis = 0L;
    }
}
