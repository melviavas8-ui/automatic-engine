package daamky.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.texture.*;
import net.minecraft.client.gl.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.render.*;
import org.joml.Matrix3x2f;

public final class MsdfText {

    public static boolean msdfMode = true;

    public static float baseWeight = 0.03f;

    private MsdfText() {}

    public static float width(FontAtlas f, String s, float size) {
        if (f == null) return 0;
        float w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            float[] g = f.glyphs.get(cp);
            if (g != null) w += g[1] * size;
        }
        return w;
    }


    public static float draw(Matrix3x2f m, FontAtlas f, String s, float x, float y, float size, int color) {
        return draw(m, f, s, x, y, size, color, 0f);
    }


    public static float draw(Matrix3x2f m, FontAtlas f, String s, float x, float y, float size, int color, float weight) {
        if (f == null || s.isEmpty()) return 0;
        float k = size / f.scale, pen = x, baseline = y + f.ascent * size;
        Gfx.Batch b = new Gfx.Batch(m);
        int rng = Math.round(f.range * 16f), wt = Math.round(weight * 10000f);
        int quads = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            float[] g = f.glyphs.get(cp);
            if (g == null) g = f.glyphs.get((int) '?');
            if (g == null) continue;
            if (cp != ' ') {
                int idx = (int) g[0];
                float x0 = pen - f.pad * k, x1 = pen + (f.cell - f.pad) * k;
                float y0 = baseline - f.base * k, y1 = baseline + (f.cell - f.base) * k;
                float u0 = (idx % f.cols) * f.cell / (float) f.w, u1 = u0 + f.cell / (float) f.w;
                float v0 = (idx / f.cols) * f.cell / (float) f.h, v1 = v0 + f.cell / (float) f.h;
                b.v(x0, y0, 0f, color, u0, v0, rng, 0, wt, 0, 0f);
                b.v(x0, y1, 0f, color, u0, v1, rng, 0, wt, 0, 0f);
                b.v(x1, y1, 0f, color, u1, v1, rng, 0, wt, 0, 0f);
                b.v(x1, y0, 0f, color, u1, v0, rng, 0, wt, 0, 0f);
                quads++;
            }
            pen += g[1] * size;
        }
        if (quads == 0) return pen - x;
        boolean useSdf = !msdfMode && f.sdfView != null;
        var view = useSdf ? f.sdfView : f.msdfView;
        if (view == null) return pen - x;
        b.submit(useSdf ? Gfx.SDF : Gfx.MSDF, TextureSetup.of(view, RenderSystem.getSamplerCache().get(FilterMode.LINEAR)));
        return pen - x;
    }
}
