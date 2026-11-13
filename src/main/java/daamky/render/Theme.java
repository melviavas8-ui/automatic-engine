package daamky.render;


public final class Theme {
    public static boolean custom;
    private static boolean legacyPalette;
    public static float hue = 212f, sat = 0.62f, bri = 0.98f, speed = 1f;
    public static boolean blur = true, glow = true;

    private static final float[] DA = {0.73f, 0.86f, 1.00f}, DB = {0.28f, 0.57f, 0.98f}, DC = {0.09f, 0.25f, 0.64f};
    private static final float[] LA = {0.88f, 0.78f, 1.00f}, LB = {0.66f, 0.47f, 0.95f}, LC = {0.46f, 0.27f, 0.84f};

    private Theme() {}


    public static int rgb() { return custom ? hsb(hue, sat, bri) : 0xFFFFFF; }

    public static void reset() { custom = false; speed = 1f; blur = true; glow = true; apply(); }

    public static void useLegacy(boolean legacy) {
        legacyPalette = legacy;
        apply();
    }


    public static void apply() {
        float[] a = SdfRect.GA, b = SdfRect.GB, c = SdfRect.GC;
        if (!custom) {
            float[] ca = legacyPalette ? LA : DA;
            float[] cb = legacyPalette ? LB : DB;
            float[] cc = legacyPalette ? LC : DC;
            System.arraycopy(ca, 0, a, 0, 3);
            System.arraycopy(cb, 0, b, 0, 3);
            System.arraycopy(cc, 0, c, 0, 3);
            return;
        }
        int v = hsb(hue, sat, bri);
        float r = (v >> 16 & 255) / 255f, g = (v >> 8 & 255) / 255f, bl = (v & 255) / 255f;
        b[0] = r; b[1] = g; b[2] = bl;
        a[0] = r + (1 - r) * 0.55f; a[1] = g + (1 - g) * 0.55f; a[2] = bl + (1 - bl) * 0.55f;
        c[0] = r * 0.70f; c[1] = g * 0.57f; c[2] = bl * 0.88f;
    }

    private static int hsb(float h, float s, float v) {
        h = ((h % 360f) + 360f) % 360f / 60f;
        int i = (int) h;
        float f = h - i, p = v * (1 - s), q = v * (1 - s * f), t = v * (1 - s * (1 - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        int ri = Math.round(r * 255), gi = Math.round(g * 255), bi = Math.round(b * 255);
        int out = ri << 16 | gi << 8 | bi;
        return out == 0xFFFFFF ? 0xFEFEFE : out;
    }
}
