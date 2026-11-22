package daamky.ua.newmenu.video;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.function.LongSupplier;

/**
 * Pure-Java H.264 (mp4) decoder that follows an external media clock. It knows nothing about
 * Minecraft: a background thread decodes frames, converts the ones that are actually due to RGBA
 * and the render thread picks the newest one with {@link #copyLatestFrame(ByteBuffer, int)}.
 *
 * When the device is too slow the thread keeps decoding (a P-frame chain cannot be skipped) but
 * stops converting late frames, so the picture jumps forward instead of lagging behind the music.
 * Any failure only marks the decoder as failed; the menu then shows the still background.
 */
public final class VideoDecoder implements Runnable {
    private final File file;
    private final LongSupplier mediaNanos;
    private final Object lock = new Object();

    private volatile boolean running;
    private volatile boolean finished;
    private volatile boolean failed;
    private volatile boolean wanted = true;
    private volatile int width;
    private volatile int height;
    private volatile long lastWantedAt = System.nanoTime();
    private long lastPublishAt;
    private Thread thread;

    private byte[] frontBuffer;
    private byte[] backBuffer;
    private long publishedSeq;
    private long consumedSeq;
    private double fps = 25.0;

    /**
     * @param file       mp4 file on disk
     * @param mediaNanos media time in nanoseconds since the music started; negative means "not started yet"
     */
    public VideoDecoder(File file, LongSupplier mediaNanos) {
        this.file = file;
        this.mediaNanos = mediaNanos;
    }

    public synchronized void start() {
        if (thread != null) return;
        running = true;
        thread = new Thread(this, "daamky-menu-video");
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        thread.start();
    }

    public synchronized void stop() {
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isFinished() { return finished; }

    public boolean isFailed() { return failed; }

    public int width() { return width; }

    public int height() { return height; }

    /** The renderer calls this every frame it shows the video; frames are only converted while somebody watches. */
    public void markWanted() { lastWantedAt = System.nanoTime(); }

    /**
     * Copies the newest decoded frame as RGBA into {@code dst} (starting at position 0, limit untouched).
     * Returns false when there is nothing newer than what was copied before.
     */
    public boolean copyLatestFrame(ByteBuffer dst, int byteCount) {
        synchronized (lock) {
            if (frontBuffer == null || publishedSeq == consumedSeq || dst.capacity() < byteCount) return false;
            dst.clear();
            dst.put(frontBuffer, 0, byteCount);
            dst.flip();
            consumedSeq = publishedSeq;
            return true;
        }
    }

    @Override
    public void run() {
        try {
            FrameGrab grab = FrameGrab.createFrameGrab(NIOUtils.readableChannel(file));
            DemuxerTrackMeta meta = grab.getVideoTrack().getMeta();
            if (meta != null && meta.getTotalDuration() > 0 && meta.getTotalFrames() > 0) {
                fps = meta.getTotalFrames() / meta.getTotalDuration();
            }
            if (!(fps > 1.0 && fps < 240.0)) fps = 25.0;

            int nextIndex = 0;
            while (running) {
                long t = mediaNanos.getAsLong();
                if (t < 0) {
                    Thread.sleep(4);
                    continue;
                }
                int target = (int) (t * fps / 1_000_000_000.0);
                // Never run more than one frame ahead of the clock.
                if (nextIndex > target + 1) {
                    Thread.sleep(3);
                    continue;
                }

                Picture picture = grab.getNativeFrame();
                if (picture == null) {
                    finished = true;
                    break;
                }

                long now = System.nanoTime();
                boolean late = nextIndex < target - 1;
                boolean watched = now - lastWantedAt < 1_500_000_000L;
                // A late frame is still shown now and then, so a slow device gets a choppy picture instead of a frozen one.
                if (watched && (!late || now - lastPublishAt > 100_000_000L)) {
                    publish(picture);
                    lastPublishAt = now;
                }
                nextIndex++;

                // Far behind the music: jump to the nearest earlier keyframe instead of decoding every missed frame.
                if (target - nextIndex > (int) (fps * 1.5)) {
                    grab.seekToFrameSloppy(target);
                    nextIndex = (int) grab.getVideoTrack().getCurFrame();
                }
            }
        } catch (InterruptedException ignored) {
            // stop() was called
        } catch (Throwable t) {
            failed = true;
        } finally {
            running = false;
        }
    }

    private void publish(Picture picture) {
        int w = picture.getCroppedWidth();
        int h = picture.getCroppedHeight();
        int needed = w * h * 4;
        if (backBuffer == null || backBuffer.length != needed) backBuffer = new byte[needed];
        yuv420ToRgba(picture, w, h, backBuffer);
        synchronized (lock) {
            byte[] swap = frontBuffer;
            frontBuffer = backBuffer;
            backBuffer = swap;
            width = w;
            height = h;
            publishedSeq++;
        }
    }

    /**
     * BT.709, limited (tv) range, 4:2:0 planar. JCodec keeps samples as signed bytes (value - 128).
     * The source plane stride is the full (macroblock aligned) picture width, the output is cropped.
     */
    static void yuv420ToRgba(Picture p, int w, int h, byte[] out) {
        byte[][] data = p.getData();
        byte[] yp = data[0];
        byte[] up = data[1];
        byte[] vp = data[2];
        int yStride = p.getWidth();
        int cStride = yStride >> 1;
        int o = 0;
        for (int y = 0; y < h; y++) {
            int yRow = y * yStride;
            int cRow = (y >> 1) * cStride;
            for (int x = 0; x < w; x++) {
                int yy = (yp[yRow + x] + 128) - 16;
                int cIdx = cRow + (x >> 1);
                int u = (up[cIdx] + 128) - 128;
                int v = (vp[cIdx] + 128) - 128;
                int c = yy * 1192; // 1.164 * 1024
                int r = (c + 1836 * v) >> 10; // 1.793
                int g = (c - 218 * u - 546 * v) >> 10; // 0.213, 0.533
                int b = (c + 2163 * u) >> 10; // 2.112
                out[o++] = (byte) (r < 0 ? 0 : Math.min(r, 255));
                out[o++] = (byte) (g < 0 ? 0 : Math.min(g, 255));
                out[o++] = (byte) (b < 0 ? 0 : Math.min(b, 255));
                out[o++] = (byte) 255;
            }
        }
    }
}
