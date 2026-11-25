package daamky.ua.newmenu.mainmenu;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

/**
 * Plays the main menu background video.
 *
 * <p>The video is stored as a pack of JPEG frames ({@code menu_video.bin}), because Minecraft has no video decoder.
 * Frames are decoded on a background thread (STB, through {@link NativeImage}) and uploaded on the render thread.
 * The frame is chosen by time, so the video never slows down: if the PC is slow, frames are just skipped.
 *
 * <p>Pack format (big endian): "DMV1", int fps, int frameCount, int width, int height,
 * int[frameCount + 1] offsets, then the JPEG data.
 */
public final class MenuVideo {
    private static final String RESOURCE = "/assets/daamkymenu/mainmenu/menu_video.bin";
    /** The worker stops (and frees the pack) when the menu has not asked for a frame for this long. */
    private static final long IDLE_FREE_NANOS = 20_000_000_000L;
    /** Decoding is paused when the menu is not drawn (other screens are open). */
    private static final long IDLE_PAUSE_NANOS = 400_000_000L;
    private static final MenuVideo INSTANCE = new MenuVideo();

    private final AtomicReference<NativeImage> pending = new AtomicReference<>();
    private volatile boolean started;
    private volatile boolean failed;
    private volatile long lastUsedNanos;
    private volatile float aspect = 16.0F / 9.0F;

    private NativeImageBackedTexture current;
    private NativeImageBackedTexture retired;
    private int currentGlId;

    private MenuVideo() {
    }

    public static MenuVideo getInstance() {
        return INSTANCE;
    }

    public float aspect() {
        return aspect;
    }

    /**
     * Call from the render thread every frame the background is drawn.
     * Returns the GL id of the current video frame, or 0 while nothing is ready (or the video is unavailable).
     */
    public int frameGlId() {
        lastUsedNanos = System.nanoTime();
        if (failed) return 0;
        if (!started) {
            started = true;
            Thread thread = new Thread(this::run, "daamky-menu-video");
            thread.setDaemon(true);
            thread.start();
        }
        NativeImage image = pending.getAndSet(null);
        if (image != null) upload(image);
        return currentGlId;
    }

    private void upload(NativeImage image) {
        NativeImageBackedTexture texture = null;
        try {
            texture = new NativeImageBackedTexture(() -> "daamky_menu_video", image);
            texture.upload();
            if (texture.getGlTexture() instanceof GlTexture glTexture) {
                // The previous frame may still be referenced by the frame being drawn, so it is freed one frame later.
                if (retired != null) retired.close();
                retired = current;
                current = texture;
                currentGlId = glTexture.getGlId();
            } else {
                texture.close();
            }
        } catch (RuntimeException exception) {
            if (texture != null) texture.close();
            else image.close();
        }
    }

    private void run() {
        try {
            byte[] data;
            try (InputStream in = MenuVideo.class.getResourceAsStream(RESOURCE)) {
                if (in == null) throw new IllegalStateException(RESOURCE + " not found");
                data = in.readAllBytes();
            }
            DataInputStream header = new DataInputStream(new ByteArrayInputStream(data));
            if (header.readInt() != 0x444D5631) throw new IllegalStateException("bad video pack header");
            int fps = header.readInt();
            int frames = header.readInt();
            int width = header.readInt();
            int height = header.readInt();
            if (fps <= 0 || frames <= 0 || width <= 0 || height <= 0) throw new IllegalStateException("bad video pack");
            int[] offsets = new int[frames + 1];
            for (int i = 0; i <= frames; i++) offsets[i] = header.readInt();
            int base = 20 + 4 * (frames + 1);
            aspect = (float) width / (float) height;

            long startNanos = System.nanoTime();
            int last = -1;
            while (true) {
                long now = System.nanoTime();
                long idle = now - lastUsedNanos;
                if (idle > IDLE_FREE_NANOS) break;
                if (idle > IDLE_PAUSE_NANOS) {
                    // menu is hidden: wait, and keep the video position from jumping when it comes back
                    Thread.sleep(50L);
                    startNanos += System.nanoTime() - now;
                    continue;
                }
                int target = (int) (((now - startNanos) / 1_000_000_000.0) * fps) % frames;
                if (target == last) {
                    Thread.sleep(3L);
                    continue;
                }
                int from = base + offsets[target];
                int length = offsets[target + 1] - offsets[target];
                NativeImage image = NativeImage.read(new ByteArrayInputStream(data, from, length));
                NativeImage old = pending.getAndSet(image);
                if (old != null) old.close();
                last = target;
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable throwable) {
            failed = true;
            System.err.println("[Daamky] Menu video disabled: " + throwable);
        } finally {
            NativeImage leftover = pending.getAndSet(null);
            if (leftover != null) leftover.close();
            started = false;
        }
    }
}
