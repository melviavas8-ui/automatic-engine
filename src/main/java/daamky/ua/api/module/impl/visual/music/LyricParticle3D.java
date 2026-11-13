package daamky.ua.api.module.impl.visual.music;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/**
 * A timestamped lyric rendered as a custom-font billboard in 3D world space.
 * The glow is drawn as soft sprite quads behind the text (see {@link #renderGlow}),
 * the text itself is drawn once, crisp, by {@link #renderText}.
 */
public final class LyricParticle3D {
    private static final Identifier LYRIC_FONT = Identifier.of("daamky", "music_lyrics");
    private static final Style LYRIC_STYLE =
        Style.EMPTY.withFont(new StyleSpriteSource.Font(LYRIC_FONT));

    private static final float VAPORIZE_START = 0.64F;
    /** Glow brightness left over at the moment the text is gone (start of the afterglow). */
    private static final float REMNANT = 0.55F;

    private final String text;
    private final Vec3d basePosition;
    private final long spawnTimeMs;
    private final long durationMs;
    private final float floatHeight;
    private final float tiltSign;
    private final float hueSeed = (float) Math.random();

    private Text styledText;
    private float cachedWidth = -1.0F;

    // Animation state, recomputed every frame by computeAnim().
    private float aAlpha;
    private float aGlow;
    private float aScale;
    private float aTilt;
    private float aOffX;
    private double aOffY;

    public LyricParticle3D(String text, Vec3d basePosition, long spawnTimeMs, long durationMs, float floatHeight) {
        this.text = text;
        this.basePosition = basePosition;
        this.spawnTimeMs = spawnTimeMs;
        this.durationMs = Math.max(1L, durationMs);
        this.floatHeight = floatHeight;
        this.tiltSign = Math.random() > 0.5 ? 1.0F : -1.0F;
    }

    public Vec3d getBasePosition() {
        return this.basePosition;
    }

    /** True while the text is still visible (the glow may outlive it, see {@link #isDead}). */
    public boolean isTextAlive(long nowMs) {
        return nowMs - this.spawnTimeMs < this.durationMs;
    }

    public boolean isDead(long nowMs, long afterglowMs) {
        return nowMs - this.spawnTimeMs >= this.durationMs + Math.max(0L, afterglowMs);
    }

    // ---------------------------------------------------------------- glow

    public void renderGlow(MatrixStack matrices, Camera camera, TextRenderer font, String animation,
                           VertexConsumer vc, long nowMs, LyricGlow glow) {
        if (glow.intensity <= 0.01F) return;
        long elapsed = nowMs - this.spawnTimeMs;
        long afterglow = Math.max(0L, glow.afterglowMs);
        if (elapsed < 0L || elapsed >= this.durationMs + afterglow) return;

        float a;
        if (elapsed < this.durationMs) {
            float progress = MathHelper.clamp((float) elapsed / (float) this.durationMs, 0.0F, 1.0F);
            this.computeAnim(animation, progress, elapsed, afterglow);
            a = this.aGlow;
        } else {
            // Text is gone: the halo stays at its final position, slowly drifts up, widens and fades out.
            this.computeAnim(animation, 1.0F, this.durationMs, afterglow);
            float u = MathHelper.clamp((float) (elapsed - this.durationMs) / (float) afterglow, 0.0F, 1.0F);
            float k = 1.0F - u;
            a = REMNANT * k * k;
            this.aScale *= 1.0F + 0.12F * u;
            this.aOffY += 0.10 * u;
        }

        float pulse = glow.pulse
            ? 0.84F + 0.16F * (float) Math.sin(elapsed * 0.0035 * glow.pulseSpeed)
            : 1.0F;
        float level = glow.intensity * a * pulse;
        if (level <= 0.01F) return;

        float width = this.lineWidth(font);
        float fh = font.fontHeight;

        this.beginBillboard(matrices, camera, this.aOffY, this.aTilt, this.aScale);
        MatrixStack.Entry pose = matrices.peek();
        float startX = -width / 2.0F + this.aOffX;

        // Wide soft halo.
        float outerSize = fh * 2.6F * glow.size;
        this.drawHaloRow(vc, pose, startX, width, outerSize, outerSize * 0.42F, 0.13F * level, 0.0F, nowMs, glow);
        // Tight halo hugging the letters, slightly whitened so it reads as light, not paint.
        if (glow.core > 0.01F) {
            float coreSize = fh * 1.45F * (0.8F + 0.2F * glow.size);
            this.drawHaloRow(vc, pose, startX, width, coreSize, coreSize * 0.45F, 0.30F * glow.core * level, 0.28F, nowMs, glow);
        }
        matrices.pop();
    }

    private void drawHaloRow(VertexConsumer vc, MatrixStack.Entry pose, float startX, float width,
                             float size, float step, float alpha, float whiten, long nowMs, LyricGlow glow) {
        int alphaByte = Math.round(MathHelper.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        if (alphaByte <= 0) return;
        int segments = MathHelper.clamp((int) Math.ceil(width / Math.max(0.5F, step)), 1, 48);
        float half = size * 0.5F;
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / (float) segments;
            int rgb = this.glowRgb(t, nowMs, glow);
            if (whiten > 0.0F) rgb = lerpRgb(rgb, 0xFFFFFF, whiten);
            quad(vc, pose, startX + width * t, 0.0F, half, (alphaByte << 24) | rgb);
        }
    }

    private int glowRgb(float t, long nowMs, LyricGlow glow) {
        if (glow.rainbow) {
            float hue = (((nowMs % 8000L) / 8000.0F) + this.hueSeed + t * 0.30F) % 1.0F;
            return java.awt.Color.HSBtoRGB(hue, 0.60F, 1.0F) & 0xFFFFFF;
        }
        if (glow.gradient) return lerpRgb(glow.colorA, glow.colorB, t);
        return glow.colorA & 0xFFFFFF;
    }

    private static int lerpRgb(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
        int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
        int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
        return (MathHelper.clamp(r, 0, 255) << 16) | (MathHelper.clamp(g, 0, 255) << 8) | MathHelper.clamp(bl, 0, 255);
    }

    private static void quad(VertexConsumer vc, MatrixStack.Entry pose, float cx, float cy, float h, int argb) {
        vc.vertex(pose, cx - h, cy - h, 0.0F).texture(0.0F, 0.0F).color(argb);
        vc.vertex(pose, cx + h, cy - h, 0.0F).texture(1.0F, 0.0F).color(argb);
        vc.vertex(pose, cx + h, cy + h, 0.0F).texture(1.0F, 1.0F).color(argb);
        vc.vertex(pose, cx - h, cy + h, 0.0F).texture(0.0F, 1.0F).color(argb);
    }

    // ---------------------------------------------------------------- text

    public void renderText(MatrixStack matrices, Camera camera, TextRenderer font, String animation,
                           VertexConsumerProvider consumers, long nowMs, LyricGlow glow) {
        long elapsed = nowMs - this.spawnTimeMs;
        if (elapsed < 0L || elapsed >= this.durationMs) return;

        float progress = MathHelper.clamp((float) elapsed / (float) this.durationMs, 0.0F, 1.0F);
        this.computeAnim(animation, progress, elapsed, Math.max(0L, glow.afterglowMs));

        if ("Vaporize".equals(animation) && progress >= VAPORIZE_START) {
            this.renderEvaporatingLetters(matrices, camera, font, consumers, nowMs, progress, glow.textColor, (float) this.aOffY);
            return;
        }
        if (this.aAlpha <= 0.001F) return;

        Text styled = this.lineText();
        float width = this.lineWidth(font);
        float x = -width / 2.0F + this.aOffX;
        float y = -font.fontHeight / 2.0F;
        this.beginBillboard(matrices, camera, this.aOffY, this.aTilt, this.aScale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        draw(font, styled, x, y, withAlpha(glow.textColor, Math.round(255.0F * this.aAlpha)), matrix, consumers);
        matrices.pop();
    }

    private void renderEvaporatingLetters(MatrixStack matrices, Camera camera, TextRenderer font,
                                           VertexConsumerProvider consumers, long nowMs, float progress,
                                           int textColor, float baseOffsetY) {
        float cursorX = -this.lineWidth(font) / 2.0F;
        float baseY = -font.fontHeight / 2.0F;
        int[] codePoints = this.text.codePoints().toArray();
        int count = Math.max(1, codePoints.length);
        long elapsed = nowMs - this.spawnTimeMs;

        this.beginBillboard(matrices, camera, baseOffsetY, 0.0F, 1.0F);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        for (int i = 0; i < codePoints.length; i++) {
            String glyph = new String(Character.toChars(codePoints[i]));
            Text styledGlyph = Text.literal(glyph).setStyle(LYRIC_STYLE);
            float glyphWidth = font.getWidth(styledGlyph);
            float stagger = (float) i / count * 0.19F;
            float local = MathHelper.clamp((progress - (VAPORIZE_START + stagger)) / (0.36F - stagger), 0.0F, 1.0F);
            float fade = 1.0F - easeInCubic(local);
            float rise = easeOutCubic(local) * (0.12F + (i % 3) * 0.035F);
            float drift = (float) Math.sin(i * 2.17 + elapsed * 0.004) * 0.035F * local;
            float tilt = (float) Math.sin(i * 1.71) * 7.0F * local;
            if (fade > 0.002F) {
                matrices.push();
                matrices.translate(cursorX + drift, baseY - rise, 0.0F);
                if (tilt != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));
                Matrix4f glyphMatrix = matrices.peek().getPositionMatrix();
                draw(font, styledGlyph, 0.0F, 0.0F, withAlpha(textColor, Math.round(255.0F * fade)), glyphMatrix, consumers);
                matrices.pop();
            }
            cursorX += glyphWidth;
        }
        matrices.pop();
    }

    // ------------------------------------------------------------ animation

    /**
     * Fills the a* fields: text alpha, glow alpha, scale, tilt and offsets for the given animation.
     * The glow follows the text alpha, but when an afterglow is set it fades only down to REMNANT
     * while the text vanishes, and the rest of the fade happens after the text is gone.
     */
    private void computeAnim(String animation, float progress, long elapsed, long afterglowMs) {
        float alpha = 1.0F;
        float scale = 1.0F;
        float tilt = 0.0F;
        float offsetX = 0.0F;
        float outT = 0.0F; // raw 0..1 progress of the fade-out part
        double offsetY = (float) (Math.sin(elapsed * 0.0018) * 0.018) * this.floatHeight;
        boolean vaporizing = false;

        if ("Vaporize".equals(animation) && progress >= VAPORIZE_START) {
            vaporizing = true;
            outT = MathHelper.clamp((progress - VAPORIZE_START) / (1.0F - VAPORIZE_START), 0.0F, 1.0F);
            alpha = 0.0F; // text is drawn per glyph in this phase
        } else {
            switch (animation) {
                case "Typewriter" -> {
                    if (progress < 0.30F) {
                        float t = easeOutCubic(progress / 0.30F);
                        alpha = t;
                        scale = 0.96F + 0.04F * t;
                        offsetY -= 0.10F * (1.0F - t);
                    } else if (progress >= 0.75F) {
                        outT = (progress - 0.75F) / 0.25F;
                        float t = easeInCubic(outT);
                        alpha = 1.0F - t;
                        offsetY += 0.10F * t;
                    }
                }
                case "PopScale" -> {
                    if (progress < 0.18F) {
                        float t = easeOutBack(progress / 0.18F);
                        alpha = easeOutCubic(progress / 0.18F);
                        scale = 0.35F + 0.65F * t;
                        offsetY -= 0.04F * (1.0F - t);
                    } else if (progress >= 0.80F) {
                        outT = (progress - 0.80F) / 0.20F;
                        float t = easeInCubic(outT);
                        alpha = 1.0F - t;
                        scale = 1.0F - 0.08F * t;
                        offsetY += 0.08F * t;
                    }
                }
                case "KineticSlide" -> {
                    if (progress < 0.20F) {
                        float t = easeOutCubic(progress / 0.20F);
                        alpha = t;
                        offsetY -= 0.18F * (1.0F - t);
                        offsetX = -0.12F * this.tiltSign * (1.0F - t);
                        scale = 0.94F + 0.06F * t;
                        tilt = (1.0F - t) * 5.0F * this.tiltSign;
                    } else if (progress >= 0.80F) {
                        outT = (progress - 0.80F) / 0.20F;
                        float t = easeInCubic(outT);
                        alpha = 1.0F - t;
                        offsetY += 0.16F * t;
                        offsetX += 0.08F * this.tiltSign * t;
                        scale = 1.0F - 0.05F * t;
                    }
                }
                case "Fade" -> {
                    if (progress < 0.15F) {
                        alpha = easeOutCubic(progress / 0.15F);
                    } else if (progress >= 0.80F) {
                        outT = (progress - 0.80F) / 0.20F;
                        alpha = 1.0F - easeInCubic(outT);
                    }
                }
                default -> { // LyricFlow (and the first part of Vaporize)
                    if (progress < 0.18F) {
                        float t = easeOutCubic(progress / 0.18F);
                        alpha = t;
                        offsetY -= 0.16F * (1.0F - t);
                        scale = 0.90F + 0.10F * t;
                    } else if (progress >= 0.72F) {
                        outT = (progress - 0.72F) / 0.28F;
                        float t = easeInCubic(outT);
                        alpha = 1.0F - t;
                        offsetY += 0.20F * t;
                        scale = 1.0F - 0.04F * t;
                    }
                }
            }
        }

        float glowAlpha = alpha;
        if (outT > 0.0F) {
            if (afterglowMs > 0L) {
                float s = outT * outT * (3.0F - 2.0F * outT);
                glowAlpha = 1.0F - (1.0F - REMNANT) * s;
            } else if (vaporizing) {
                glowAlpha = 1.0F - easeInCubic(outT);
            }
        }

        this.aAlpha = MathHelper.clamp(alpha, 0.0F, 1.0F);
        this.aGlow = MathHelper.clamp(glowAlpha, 0.0F, 1.0F);
        this.aScale = scale;
        this.aTilt = tilt;
        this.aOffX = offsetX;
        this.aOffY = offsetY;
    }

    // -------------------------------------------------------------- helpers

    private Text lineText() {
        if (this.styledText == null) this.styledText = Text.literal(this.text).setStyle(LYRIC_STYLE);
        return this.styledText;
    }

    private float lineWidth(TextRenderer font) {
        if (this.cachedWidth < 0.0F) this.cachedWidth = font.getWidth(this.lineText());
        return this.cachedWidth;
    }

    private void beginBillboard(MatrixStack matrices, Camera camera, double offsetY, float tilt, float scale) {
        Vec3d cameraPos = camera.getCameraPos();
        matrices.push();
        matrices.translate(
            this.basePosition.x - cameraPos.x,
            this.basePosition.y + offsetY - cameraPos.y,
            this.basePosition.z - cameraPos.z
        );
        matrices.multiply(camera.getRotation());
        if (tilt != 0.0F) matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(tilt));
        float worldScale = 0.02F * scale;
        matrices.scale(worldScale, -worldScale, worldScale);
    }

    private static void draw(TextRenderer renderer, Text text, float x, float y, int color,
                             Matrix4f matrix, VertexConsumerProvider consumers) {
        renderer.draw(text, x, y, color, false, matrix, consumers, TextLayerType.NORMAL, 0, 15728880);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    private static float easeOutCubic(float t) { return 1.0F - (float) Math.pow(1.0F - t, 3.0); }
    private static float easeInCubic(float t) { return t * t * t; }
    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(t - 1.0F, 3.0) + c1 * (float) Math.pow(t - 1.0F, 2.0);
    }
}
