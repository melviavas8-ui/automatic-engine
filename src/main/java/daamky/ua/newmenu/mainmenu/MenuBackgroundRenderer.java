package daamky.ua.newmenu.mainmenu;

import daamky.ua.newmenu.render.Render2D;
import daamky.ua.newmenu.render.util.RenderColor;
import daamky.ua.newmenu.video.MenuMedia;
import daamky.ua.newmenu.video.VideoDecoder;
import java.io.InputStream;
import java.nio.ByteBuffer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Background of the main menu and the account manager: the looping menu video, with the still
 * picture (mainmenu_background.png) as the fallback. When the video ends, fails, or there is
 * no session at all, the still picture fades in over the last frame.
 */
public final class MenuBackgroundRenderer {
    private static final Identifier STILL_ID = Identifier.of("daamkymenu", "mainmenu/mainmenu_background.png");
    private static final String STILL_PATH = "/assets/daamkymenu/mainmenu/mainmenu_background.png";
    private static final float STILL_FADE_SECONDS = 1.4F;
    private static final MenuBackgroundRenderer INSTANCE = new MenuBackgroundRenderer();

    private NativeImageBackedTexture stillTexture;
    private int stillGlId;
    private boolean stillLoadAttempted;
    private float stillAspect = 16.0F / 9.0F;

    private NativeImageBackedTexture videoTexture;
    private int videoGlId;
    private int videoW;
    private int videoH;
    private ByteBuffer videoBuffer;
    private boolean videoShown;
    private long stillFadeStartNs;

    private MenuBackgroundRenderer() {}

    public static MenuBackgroundRenderer getInstance() {
        return INSTANCE;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight, float alpha) {
        if (alpha <= 0.001F) return;

        MenuMedia media = MenuMedia.INSTANCE;
        boolean still = media.videoOver() || !media.hasSession();
        VideoDecoder decoder = media.decoder();

        // Base: black, then the video (kept under the still while it fades in).
        Render2D.drawRound(context, 0, 0, screenWidth, screenHeight, 0.0F, RenderColor.multiplyAlpha(0xFF000000, alpha));
        if (decoder != null) decoder.markWanted();
        if (decoder != null) uploadVideo(decoder);
        if (videoShown) {
            drawCover(context, videoGlId, videoW / (float) videoH, screenWidth, screenHeight, RenderColor.multiplyAlpha(0xFFFFFFFF, alpha));
        }

        if (still) {
            ensureStillLoaded();
            if (stillFadeStartNs == 0L) stillFadeStartNs = System.nanoTime();
            // Without any video frame on screen there is nothing to fade over: show the still right away.
            float blend = videoShown && media.hasSession()
                    ? Math.min(1.0F, (System.nanoTime() - stillFadeStartNs) / (STILL_FADE_SECONDS * 1.0E9F))
                    : 1.0F;
            if (stillGlId > 0) {
                drawCover(context, stillGlId, stillAspect, screenWidth, screenHeight, RenderColor.multiplyAlpha(0xFFFFFFFF, alpha * blend));
            }
        } else {
            stillFadeStartNs = 0L;
        }

        // Darkening on the left (buttons) and at the bottom (footer) so text stays readable on any frame.
        Render2D.drawGradientRoundLR(context, 0, 0, screenWidth * 0.62F, screenHeight, 0.0F,
                RenderColor.multiplyAlpha(0xA0000000, alpha), RenderColor.multiplyAlpha(0x00000000, alpha));
        Render2D.drawGradientRoundTB(context, 0, screenHeight * 0.72F, screenWidth, screenHeight * 0.28F, 0.0F,
                RenderColor.multiplyAlpha(0x00000000, alpha), RenderColor.multiplyAlpha(0x90000000, alpha));
    }

    /** Draws a texture scaled to cover the whole screen (centered, cropped). */
    private static void drawCover(DrawContext context, int glId, float aspect, int screenWidth, int screenHeight, int tint) {
        float screenAspect = (float) screenWidth / (float) Math.max(1, screenHeight);
        float drawW;
        float drawH;
        if (screenAspect > aspect) {
            drawW = screenWidth;
            drawH = screenWidth / aspect;
        } else {
            drawH = screenHeight;
            drawW = screenHeight * aspect;
        }
        Render2D.drawRoundTexture(context, glId, (screenWidth - drawW) * 0.5F, (screenHeight - drawH) * 0.5F, drawW, drawH, 0.0F, tint);
    }

    private void uploadVideo(VideoDecoder decoder) {
        try {
            int w = decoder.width();
            int h = decoder.height();
            if (w <= 0 || h <= 0) return;
            if (videoTexture == null) {
                NativeImage image = new NativeImage(w, h, true);
                videoTexture = new NativeImageBackedTexture(() -> "daamky_menu_video", image);
                videoTexture.upload();
                if (!(videoTexture.getGlTexture() instanceof GlTexture glTexture)) {
                    videoTexture = null;
                    return;
                }
                videoGlId = glTexture.getGlId();
                videoW = w;
                videoH = h;
                videoBuffer = BufferUtils.createByteBuffer(w * h * 4);
            }
            if (w != videoW || h != videoH) return;
            if (decoder.copyLatestFrame(videoBuffer, w * h * 4)) {
                int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, videoGlId);
                GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 4);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, w, h, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, videoBuffer);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, previous);
                videoShown = true;
            }
        } catch (Throwable ignored) {
            // A broken upload only leaves the still background visible.
        }
    }

    private void ensureStillLoaded() {
        if (stillLoadAttempted) return;
        stillLoadAttempted = true;
        try (InputStream in = MenuBackgroundRenderer.class.getResourceAsStream(STILL_PATH)) {
            if (in == null) return;
            NativeImage image = NativeImage.read(in);
            stillAspect = image.getWidth() / (float) Math.max(1, image.getHeight());
            stillTexture = new NativeImageBackedTexture(() -> "daamky_menu_still", image);
            stillTexture.upload();
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getTextureManager() != null) {
                mc.getTextureManager().registerTexture(STILL_ID, stillTexture);
            }
            if (stillTexture.getGlTexture() instanceof GlTexture glTexture) {
                stillGlId = glTexture.getGlId();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
