package daamky.ua.newmenu.gui.mainmenu;

import daamky.ua.newmenu.render.Render2D;
import daamky.ua.newmenu.render.util.RenderColor;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.io.InputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class MenuBackgroundRenderer {
    private static final Identifier BACKGROUND = Identifier.of("daamkymenu", "mainmenu/night_village.png");
    private static final Identifier BACKGROUND_BLUR = Identifier.of("daamkymenu", "mainmenu/night_village_blur.png");
    private static final int PARTICLE_COUNT = 36;
    private static final MenuBackgroundRenderer INSTANCE = new MenuBackgroundRenderer();
    private final List<Particle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastTime = System.currentTimeMillis();
    private float globalTime = 0.0F;

    private NativeImageBackedTexture bgTexture = null;
    private NativeImageBackedTexture blurredBgTexture = null;
    private int bgGlId = 0;
    private int blurredBgGlId = 0;
    private boolean loadAttempted = false;

    private void ensureLoaded() {
        if (loadAttempted) return;
        loadAttempted = true;
        bgGlId = loadTexture(BACKGROUND, "/assets/daamkymenu/mainmenu/night_village.png", false);
        blurredBgGlId = loadTexture(BACKGROUND_BLUR, "/assets/daamkymenu/mainmenu/night_village_blur.png", true);
    }

    private int loadTexture(Identifier id, String classpath, boolean blurred) {
        try {
            InputStream stream = MenuBackgroundRenderer.class.getResourceAsStream(classpath);
            if (stream == null) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.getResourceManager() != null) {
                    try {
                        stream = mc.getResourceManager().open(id);
                    } catch (Throwable ignored) {}
                }
            }
            if (stream != null) {
                try (InputStream in = stream) {
                    NativeImage img = NativeImage.read(in);
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(
                            () -> blurred ? "daamky_menu_bg_blurred" : "daamky_menu_bg", img);
                    texture.upload();
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.getTextureManager() != null) {
                        mc.getTextureManager().registerTexture(id, texture);
                    }
                    if (texture.getGlTexture() instanceof GlTexture glTexture) {
                        if (blurred) blurredBgTexture = texture;
                        else bgTexture = texture;
                        return glTexture.getGlId();
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    private static final class Particle {
        float x;
        float y;
        float vx;
        float vy;
        float size;
        float alpha;
        float maxAlpha;
        float phase;

        Particle(Random rnd) {
            reset(rnd, true);
        }

        void reset(Random rnd, boolean initial) {
            this.x = rnd.nextFloat();
            this.y = initial ? rnd.nextFloat() : 1.05F;
            this.vx = (rnd.nextFloat() - 0.5F) * 0.012F;
            this.vy = -0.015F - rnd.nextFloat() * 0.025F;
            this.size = 1.0F + rnd.nextFloat() * 2.2F;
            this.maxAlpha = 0.25F + rnd.nextFloat() * 0.45F;
            this.alpha = initial ? rnd.nextFloat() * maxAlpha : 0.0F;
            this.phase = rnd.nextFloat() * 6.28F;
        }

        void update(float dt, Random rnd) {
            y += vy * dt;
            x += vx * dt;
            phase += dt * 2.0F;
            alpha = maxAlpha * (0.6F + 0.4F * (float) Math.sin(phase));

            if (y < -0.05F || x < -0.05F || x > 1.05F) {
                reset(rnd, false);
            }
        }
    }

    private MenuBackgroundRenderer() {
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            particles.add(new Particle(random));
        }
    }

    public static MenuBackgroundRenderer getInstance() {
        return INSTANCE;
    }

    public void render(DrawContext context, int screenWidth, int screenHeight, float alpha) {
        if (alpha <= 0.001F) return;

        long now = System.currentTimeMillis();
        float dt = Math.min(0.1F, (now - lastTime) / 1000.0F);
        lastTime = now;
        globalTime += dt;

        // Subtle cinematic zoom / drift breathing
        float zoom = 1.03F + (float) Math.sin(globalTime * 0.25F) * 0.015F;
        float shiftX = (float) Math.cos(globalTime * 0.18F) * 6.0F;
        float shiftY = (float) Math.sin(globalTime * 0.14F) * 4.0F;

        // Aspect ratio cover calculation (original image aspect ~ 16:9)
        float imgAspect = 16.0F / 9.0F;
        float screenAspect = (float) screenWidth / (float) Math.max(1, screenHeight);

        float drawW = screenWidth * zoom;
        float drawH = screenHeight * zoom;

        if (screenAspect > imgAspect) {
            drawW = screenWidth * zoom;
            drawH = (screenWidth / imgAspect) * zoom;
        } else {
            drawH = screenHeight * zoom;
            drawW = (screenHeight * imgAspect) * zoom;
        }

        float drawX = (screenWidth - drawW) * 0.5F + shiftX;
        float drawY = (screenHeight - drawH) * 0.5F + shiftY;

        // 1. Draw the night-village background with balanced photographic blur
        ensureLoaded();
        int baseTint = RenderColor.multiplyAlpha(0xFFFFFFFF, alpha);
        if (bgGlId > 0) {
            Render2D.drawRoundTexture(context, bgGlId, drawX, drawY, drawW, drawH, 0.0F, baseTint);
        } else {
            try {
                context.drawTexture(RenderPipelines.GUI_TEXTURED, BACKGROUND, Math.round(drawX), Math.round(drawY), 0.0F, 0.0F, Math.round(drawW), Math.round(drawH), Math.round(drawW), Math.round(drawH));
            } catch (Throwable ignored) {}
        }

        // Blend the blurred version at ~55% opacity for smooth, soft background blur while keeping village structures recognizable
        if (blurredBgGlId > 0) {
            Render2D.drawRoundTexture(context, blurredBgGlId, drawX, drawY, drawW, drawH, 0.0F,
                    RenderColor.multiplyAlpha(0x90FFFFFF, alpha));
        }

        // 2. Soft atmospheric vignette preserving warm lantern glow and night sky
        int overlayTop = RenderColor.multiplyAlpha(0x60060A12, alpha);
        int overlayBottom = RenderColor.multiplyAlpha(0x9006080E, alpha);
        Render2D.drawGradientRoundTB(context, 0, 0, screenWidth, screenHeight, 0.0F, overlayTop, overlayBottom);

        // 3. Ambient soft star/dust particles in background
        for (Particle p : particles) {
            p.update(dt, random);
            float px = p.x * screenWidth;
            float py = p.y * screenHeight;
            float pAlpha = p.alpha * alpha;
            if (pAlpha > 0.01F) {
                int pColor = RenderColor.multiplyAlpha(0xFFBCE3F7, pAlpha);
                Render2D.drawRound(context, px, py, p.size, p.size, p.size * 0.5F, pColor);
            }
        }
    }
}
