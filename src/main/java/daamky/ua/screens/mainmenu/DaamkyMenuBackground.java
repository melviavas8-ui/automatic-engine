package daamky.ua.screens.mainmenu;

import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;
import daamky.render.Gfx;
import daamky.render.Img;
import daamky.render.SdfRect;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.zippy.BuiltZippy;

/** Shared cinematic background for the title screen and its themed sub-screens. */
public final class DaamkyMenuBackground {
    private DaamkyMenuBackground() {
    }

    public static void render(DrawContext context, int width, int height) {
        if (context == null || width <= 0 || height <= 0) {
            return;
        }

        Gfx.ctx = context;
        Gfx.scissor = null;
        Matrix3x2f matrix = new Matrix3x2f(context.getMatrices());

        // Keep a dark fallback visible while the resource manager is resolving the texture.
        SdfRect.fill(matrix, 0.0F, 0.0F, width, height, 0.0F, 0xFF050912);
        Img.draw(matrix, "menu-background.png", 0.0F, 0.0F, width, height, 0xFFFFFFFF);

        // The left vignette keeps the hero and menu controls readable over the scene.
        SdfRect.gradient(
            matrix,
            0.0F,
            0.0F,
            width * 0.72F,
            height,
            0.0F,
            0xE8050A13,
            0xA5080F1A,
            0x00081018,
            0xB8050A13
        );
        SdfRect.fill(matrix, 0.0F, 0.0F, width, height, 0.0F, 0x25030A14);
    }
    /** Original animated backdrop retained for the settings screens. */
    public static void renderLegacy(DrawContext graphics, int width, int height) {
        if (!VanillaMenuTheme.active()) {
            width = Render2D.getFixedScaledWidth();
            height = Render2D.getFixedScaledHeight();
        }

        Render2D.beginFrame(graphics);
        Render2D.rect(-2.0F, -2.0F, (float)width + 4.0F, (float)height + 4.0F, -16776697);
        float time = (float)(System.nanoTime() % 240000000000L) / 1.0E9F;
        float pulseA = wave(time * 0.18F, 0.0F);
        float pulseB = wave(time * 0.14F, 2.1F);
        float pulseC = wave(time * 0.11F, 4.4F);
        Render2D.zippy(new BuiltZippy(
            (float)(-width) * 0.24F, (float)(-height) * 0.28F,
            (float)width * 1.48F, (float)height * 1.55F,
            0.0F, 0.0F, 0.0F, 0.0F, alpha(-16756824, Math.round(55.0F + pulseA * 70.0F)), 1.0F, -27.0F
        ));
        Render2D.zippy(new BuiltZippy(
            (float)(-width) * 0.08F, (float)(-height) * 0.14F,
            (float)width * 1.18F, (float)height * 1.28F,
            0.0F, 0.0F, 0.0F, 0.0F, alpha(-16740376, Math.round(40.0F + pulseB * 65.0F)), 1.0F, 19.0F
        ));
        Render2D.zippy(new BuiltZippy(
            (float)(-width) * 0.35F, (float)(-height) * 0.24F,
            (float)width * 1.45F, (float)height * 1.52F,
            0.0F, 0.0F, 0.0F, 0.0F, alpha(-15030304, Math.round(20.0F + pulseC * 52.0F)), 1.0F, 63.0F
        ));
        Render2D.rect(0.0F, 0.0F, (float)width, (float)height, 0.0F,
            -671088638, -1258289648, -838860281, -419430399);
        Render2D.rect(0.0F, 0.0F, (float)width, (float)height, 0.0F,
            436220776, 100663296, 570427416, 150994944);
        Render2D.flush();
    }

    private static float wave(float value, float phase) {
        return 0.5F + 0.5F * (float)Math.sin((double)(value + phase));
    }

    private static int alpha(int color, int alpha) {
        return Math.max(0, Math.min(255, alpha)) << 24 | color & 0x00FFFFFF;
    }

}
