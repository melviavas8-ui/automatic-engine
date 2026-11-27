package daamky.ua.newmenu.mainmenu;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

/**
 * Plays the main menu background video.
 *
 * <p>The video is stored as a pack of JPEG frames ({@code menu_video.bin}), because Minecraft has no video decoder.
 * Frames are decoded on a background thread (STB, through {@link NativeImage}) and uploaded on the render thread.
 * The frame is chosen by time, so the video never slows down: if the PC is slow, frames are just skipped.
 *
 * <p>Decoding goes through {@link NativeImage} (STB) first. If Minecraft refuses to read JPEG on this device, the worker
 * switches to {@link ImageIO} for the rest of the session. {@link #status()} says what is happening, for the on-screen note.
 *
 * <p>Pack format (big endian): "DMV1", int fps, int frameCount, int width, int height,
 * int[frameCount + 1] offsets, then the JPEG data.
 */
public final class MenuVideo {
    private static final String RESOURCE = "/assets/daamkymenu/mainmenu/menu_video.bin";
    private static final net.minecraft.resources.Identifier RESOURCE_ID =
            net.minecraft.resources.Identifier.fromNamespaceAndPath("daamkymenu", "mainmenu/menu_video.bin");
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
    private volatile String status = "ожидание";
    private volatile long firstAskNanos;
    private volatile boolean useImageIo;
    private volatile int framesDecoded;
    private volatile int framesShown;

    private DynamicTexture current;
    private DynamicTexture retired;
    private int currentGlId;

    private MenuVideo() {
    }

    public static MenuVideo getInstance() {
        return INSTANCE;
    }

    public float aspect() {
        return aspect;
    }

    /** Short human-readable state of the video: loading, playing or the reason it does not play. */
    public String status() {
        return status;
    }

    /** True when the video failed, or nothing has been shown a few seconds after the first request. */
    public boolean needsAttention() {
        if (failed) return true;
        return firstAskNanos != 0L && currentGlId == 0 && System.nanoTime() - firstAskNanos > 4_000_000_000L;
    }

    /**
     * Call from the render thread every frame the background is drawn.
     * Returns the GL id of the current video frame, or 0 while nothing is ready (or the video is unavailable).
     */
    public int frameGlId() {
        lastUsedNanos = System.nanoTime();
        if (firstAskNanos == 0L) firstAskNanos = lastUsedNanos;
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


    /**
     * Load from the mod classpath first, then from Minecraft's resource manager.
     * The second path also works when the resource is supplied by the active resource pack.
     */
    private InputStream openVideoResource() throws IOException {
        InputStream stream = MenuVideo.class.getResourceAsStream(RESOURCE);
        if (stream != null) return stream;

        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getResourceManager() != null) {
            return mc.getResourceManager().open(RESOURCE_ID);
        }
        throw new IOException("Video resource not found: " + RESOURCE_ID);
    }

    private void upload(NativeImage image) {
        DynamicTexture texture = null;
        try {
            texture = new DynamicTexture(() -> "daamky_menu_video", image);
            texture.upload();
            if (texture.getTexture() instanceof GlTexture glTexture) {
                // The previous frame may still be referenced by the frame being drawn, so it is freed one frame later.
                if (retired != null) retired.close();
                retired = current;
                current = texture;
                currentGlId = glTexture.glId();
                framesShown++;
                status = "играет, кадров показано: " + framesShown + (useImageIo ? " (ImageIO)" : " (NativeImage)");
            } else {
                String type = texture.getTexture() == null ? "null" : texture.getTexture().getClass().getName();
                texture.close();
                if (!failed) {
                    failed = true;
                    status = "ОШИБКА: текстура кадра не GlTexture (" + type + ")";
                    System.err.println("[Daamky] Menu video disabled: " + status);
                }
            }
        } catch (RuntimeException exception) {
            if (texture != null) texture.close();
            else image.close();
            if (!failed) {
                failed = true;
                status = "ОШИБКА загрузки кадра: " + exception;
                System.err.println("[Daamky] Menu video disabled (frame upload failed): " + exception);
                exception.printStackTrace();
            }
        }
    }

    /** Decodes one JPEG frame to an RGBA image. */
    private NativeImage decode(byte[] data, int from, int length) throws IOException {
        if (!useImageIo) {
            try {
                return NativeImage.read(NativeImage.Format.RGBA, new ByteArrayInputStream(data, from, length));
            } catch (IOException | RuntimeException exception) {
                useImageIo = true;
                System.err.println("[Daamky] NativeImage cannot read the video frames, switching to ImageIO: " + exception);
            }
        }
        BufferedImage frame = ImageIO.read(new ByteArrayInputStream(data, from, length));
        if (frame == null) throw new IOException("ImageIO has no JPEG reader on this device");
        int w = frame.getWidth();
        int h = frame.getHeight();
        int[] argb = frame.getRGB(0, 0, w, h, null, 0, w);
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, w, h, true);
        try {
            for (int y = 0; y < h; y++) {
                int row = y * w;
                for (int x = 0; x < w; x++) {
                    image.setPixel(x, y, argb[row + x] | 0xFF000000);
                }
            }
        } catch (RuntimeException exception) {
            image.close();
            throw exception;
        }
        return image;
    }

    private void run() {
        try {
            status = "читаю видео-пак";
            byte[] data;
            try (InputStream in = openVideoResource()) {
                data = in.readAllBytes();
            }
            status = "пак прочитан: " + (data.length / 1024) + " КБ, декодирую";
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
                NativeImage image = decode(data, from, length);
                framesDecoded++;
                if (framesShown == 0) status = "кадров декодировано: " + framesDecoded + ", жду загрузку в GPU";
                NativeImage old = pending.getAndSet(image);
                if (old != null) old.close();
                last = target;
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } catch (Throwable throwable) {
            failed = true;
            status = "ОШИБКА: " + throwable;
            System.err.println("[Daamky] Menu video disabled: " + throwable);
            throwable.printStackTrace();
        } finally {
            NativeImage leftover = pending.getAndSet(null);
            if (leftover != null) leftover.close();
            started = false;
        }
    }
}
