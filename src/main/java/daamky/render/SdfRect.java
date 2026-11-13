package daamky.render;

import net.minecraft.client.texture.*;
import net.minecraft.client.gl.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.render.*;
import org.joml.Matrix3x2f;


public final class SdfRect {

    public static final float[] GA = {0.73f, 0.86f, 1.00f}, GB = {0.28f, 0.57f, 0.98f}, GC = {0.09f, 0.25f, 0.64f};

    private SdfRect() {}


    private static double acc;
    private static long lastNs;
    private static float time() {
        long n = System.nanoTime();
        if (lastNs == 0) lastNs = n;
        acc = (acc + (n - lastNs) / 1e9 * Theme.speed) % 100.0;
        lastNs = n;
        return (float) acc;
    }


    public static void flow(Matrix3x2f m, float x, float y, float w, float h, float r) {
        int c = 0xFF000000 | Theme.rgb();
        quad(m, x, y, w, h, 1f, 3, r, 0f, c, c, c, c);
    }


    public static void flow(Matrix3x2f m, float x, float y, float w, float h, float r, float a) {
        int c = (Math.round(Math.max(0f, Math.min(1f, a)) * 255f) << 24) | Theme.rgb();
        quad(m, x, y, w, h, 1f, 3, r, 0f, c, c, c, c);
    }


    public static void flowGlow(Matrix3x2f m, float x, float y, float w, float h, float r, float spread, int alpha) {
        int c = (alpha << 24) | Theme.rgb();
        quad(m, x, y, w, h, spread + 1f, 4, r, spread, c, c, c, c);
    }

    public static void fill(Matrix3x2f m, float x, float y, float w, float h, float r, int c) {
        quad(m, x, y, w, h, 1f, 0, r, 0f, c, c, c, c);
    }


    public static void gradient(Matrix3x2f m, float x, float y, float w, float h, float r, int tl, int tr, int br, int bl) {
        quad(m, x, y, w, h, 1f, 0, r, 0f, tl, tr, br, bl);
    }


    public static void cutout(Matrix3x2f m, float x, float y, float w, float h, float r, int c) {
        quad(m, x, y, w, h, 1f, 5, r, 0f, c, c, c, c);
    }

    public static void border(Matrix3x2f m, float x, float y, float w, float h, float r, float thickness, int c) {
        quad(m, x, y, w, h, 1f, 1, r, thickness, c, c, c, c);
    }

    public static void glow(Matrix3x2f m, float x, float y, float w, float h, float r, float spread, int c) {
        quad(m, x, y, w, h, spread + 1f, 2, r, spread, c, c, c, c);
    }

    private static void quad(Matrix3x2f m, float x, float y, float w, float h, float e, int mode, float r, float extra,
                             int tl, int tr, int br, int bl) {
        if (w <= 0 || h <= 0 || Gfx.ctx == null) return;
        float hw = w / 2f, hh = h / 2f, z = time();
        int hx = Math.round(hw * 32f), hy = Math.round(hh * 32f);
        int rr = Math.round(Math.max(0f, Math.min(r, Math.min(hw, hh))) * 64f), ex = Math.round(extra * 64f);
        float nx = mode / 127f;
        Gfx.Batch b = new Gfx.Batch(m);
        b.v(x - e, y - e, z, tl, -hw - e, -hh - e, hx, hy, rr, ex, nx);
        b.v(x - e, y + h + e, z, bl, -hw - e, hh + e, hx, hy, rr, ex, nx);
        b.v(x + w + e, y + h + e, z, br, hw + e, hh + e, hx, hy, rr, ex, nx);
        b.v(x + w + e, y - e, z, tr, hw + e, -hh - e, hx, hy, rr, ex, nx);
        b.submit(Gfx.RECT, TextureSetup.empty());
    }
}
