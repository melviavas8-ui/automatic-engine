package daamky.render;

import org.joml.Matrix3x2f;


public final class Txt {

    private static final float FAUX_BOLD = 0.10f;

    private Txt() {}

    public static float reg(Matrix3x2f m, String s, float x, float y, float size, int color) {
        return MsdfText.draw(m, Fonts.regular(), s, x, y, size, color, MsdfText.baseWeight);
    }

    public static float bold(Matrix3x2f m, String s, float x, float y, float size, int color) {
        FontAtlas b = Fonts.bold();
        if (b != null) return MsdfText.draw(m, b, s, x, y, size, color, MsdfText.baseWeight);
        return MsdfText.draw(m, Fonts.regular(), s, x, y, size, color, MsdfText.baseWeight + FAUX_BOLD);
    }

    public static float wReg(String s, float size) { return MsdfText.width(Fonts.regular(), s, size); }

    public static float wBold(String s, float size) {
        FontAtlas b = Fonts.bold();
        return MsdfText.width(b != null ? b : Fonts.regular(), s, size);
    }


    public static void icon(Matrix3x2f m, int cp, float x, float y, float size, int color) {
        MsdfText.draw(m, Fonts.icons(), new String(Character.toChars(cp)), x, y, size, color, 0f);
    }
}
