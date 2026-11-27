package daamky.gui;

import daamky.gui.Modules.Bool;
import daamky.gui.Modules.Button;
import daamky.gui.Modules.Group;
import daamky.gui.Modules.Mod;
import daamky.gui.Modules.Mode;
import daamky.gui.Modules.Multi;
import daamky.gui.Modules.TextValue;
import daamky.gui.Modules.ColorValue;
import daamky.gui.Modules.BindValue;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.gui.Modules.Section;
import daamky.gui.Modules.Setting;
import daamky.gui.Modules.Slider;
import daamky.render.Icons;
import daamky.render.MsdfText;
import daamky.render.SdfRect;
import daamky.render.Txt;
import org.joml.Matrix3x2f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * Radial ClickGUI: a ring of categories around the screen centre. Hover highlights the category arc,
 * click opens a window with all modules of the category (toggles), right click on a module opens its settings.
 * Module data and the settings panel are the same as in the Window style.
 */
public class DaamkyRadialGui extends Screen {

    static final float W = 720, H = 470;
    static final float PANEL_W = 204, PANEL_MAX_H = 330, HEAD_H = 22, LABEL_W = 140;
    static final float OPT_H = 16, SLIDER_H = 33;
    /** Target Minecraft GUI scale the design was made for. Keeps physical size consistent across GUI Scale / resolution. */
    static final float REF_GUI_SCALE = 2f;
    static final int PANEL_BG = 0xFF080E18, TOG_OFF = 0xFF36506D, KNOB = 0xFFF5FAFF, THUMB = 0xFF587DA8;
    static final int WHITE = 0xFFFFFFFF, DIM = 0xFF8394AA, ACCENT_TXT = 0xFF79B4FF;
    static final int CARD_OFF = 0xFF0D1522, CARD_OFF_BORDER = 0xFF263B57;

    static final float OPEN_TIME = 0.6f, CLOSE_TIME = 0.22f, PANEL_OPEN = 0.45f, PANEL_CLOSE = 0.2f;
    static final float WIN_OPEN = 0.42f, WIN_CLOSE = 0.18f, BOUNCE = 2.2f;

    // ring geometry (design units, centre of the design area)
    static final float CX = W / 2f, CY = H / 2f, R_OUT = 112f, R_IN = 46f, R_ICON = 82f;
    // category window
    static final float WIN_W = 196f, WIN_HEAD = 24f, ROW_H = 25f, WIN_MAX_H = 330f;
    static final int CAT_CONFIGS = 5;

    private record Sec(String label, int icon, int cat) {}
    private static final Sec[] SECS = {
            new Sec("Combat", Icons.COMBAT, 0), new Sec("Movement", Icons.MOVEMENT, 1),
            new Sec("Render", Icons.RENDER, 2), new Sec("Player", Icons.PLAYER, 3),
            new Sec("Util", Icons.UTIL, 4), new Sec("Themes", Icons.THEMES, 6),
            new Sec("AutoBuy", Icons.ITEMS, 7), new Sec("Configs", Icons.CONFIGS, CAT_CONFIGS)};
    private static final int N = SECS.length;
    private static final float SEG = (float) (Math.PI * 2.0 / N);

    private record Hit(float x, float y, float w, float h, Runnable left, Runnable right, Runnable mid) {
        Hit(float x, float y, float w, float h, Runnable left, Runnable right) { this(x, y, w, h, left, right, null); }
    }
    private Mod binding;
    private BindValue bindingSetting;
    private TextValue editingText;
    private ColorValue editingColor;
    private String editBuffer = "";

    private record K(Object o, int kind) {}
    private static final int EN = 0, HOV = 1, TOG = 2, DD = 3, SEL = 4, SL = 5, POP = 6, NAVK = 7;

    private record Popup(Mode md, float bx, float by, float bw, float bh, float vt, float vb) {}
    private Popup popup;
    private boolean ddKeep;

    /** Settings panel of one module (same as in the Window style). */
    private final class Panel {
        final Mod mod; float x, y, scroll, scrollT, maxScroll, t; boolean closing;
        Panel(Mod mod, float x, float y) { this.mod = mod; this.x = x; this.y = y; }
        float contentH() { float h = 0; for (Setting s : Modules.rows(mod)) h += rowH(s); return h; }
        float height() { return Math.min(HEAD_H + contentH() + 4, PANEL_MAX_H); }
    }

    /** Window with all modules of one category (or the config manager). */
    private final class Win {
        final Sec sec; final boolean right; final List<Mod> mods = new ArrayList<>();
        float x, y, t, age, scroll, scrollT, maxScroll; boolean closing;
        Win(Sec sec, boolean right) {
            this.sec = sec; this.right = right;
            if (sec.cat != CAT_CONFIGS) for (Mod mod : Modules.ALL) if (mod.category == sec.cat) mods.add(mod);
        }
        boolean configs() { return sec.cat == CAT_CONFIGS; }
        float contentH() { return mods.size() * ROW_H + 6; }
        float height() { return configs() ? WIN_MAX_H : Math.min(WIN_HEAD + contentH() + 4, WIN_MAX_H); }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Panel> panels = new ArrayList<>();
    private final List<Win> wins = new ArrayList<>();
    private final Map<K, Float> anim = new HashMap<>();
    private Panel drag;
    private Win dragWin;
    private Slider dragSlider;
    private float dragSliderX, dragSliderW;
    private float dragX, dragY, dragWX, dragWY, ox, oy, S = 1f, mlx, mly;
    private float vx0, vx1, vy0, vy1;

    private float curOx, curOy, curS = 1f;
    private Matrix3x2f m = new Matrix3x2f();
    private GuiGraphics ctx;

    private long lastNs;
    private float dt, time, age, openT, alpha = 1f, ga = 1f;
    private boolean closing, covered;
    private boolean cfgFocus, dblClick;
    private int hoverSec = -1;
    private String centerShown = "Daamky";
    private float centerT = 1f;

    public DaamkyRadialGui() { super(Component.literal("Daamky")); }

    @Override public boolean isPauseScreen() { return false; }

    static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    static float easeOutBack(float t, float s) { t = clamp01(t); float u = t - 1; return 1 + (s + 1) * u * u * u + s * u * u; }
    static float easeInOut(float t) { t = clamp01(t); return t < 0.5f ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2f; }
    static float easeOutCubic(float t) { float u = 1 - clamp01(t); return 1 - u * u * u; }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    static int mix(int a, int b, float t) {
        int r = 0;
        for (int sh = 24; sh >= 0; sh -= 8) {
            int ca = (a >>> sh) & 0xFF, cb = (b >>> sh) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * t) & 0xFF) << sh;
        }
        return r;
    }

    private float lerpTo(float cur, float target, float speed) {
        float v = cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - v) < 0.0008f ? target : v;
    }


    private float ap(Object o, int kind, float target, float speed) {
        K k = new K(o, kind);
        Float cur = anim.get(k);
        float v = lerpTo(cur == null ? target : cur, target, speed);
        anim.put(k, v);
        return v;
    }


    private float lin(Object o, int kind, float target, float dur) {
        K k = new K(o, kind);
        Float cur = anim.get(k);
        float v = cur == null ? target : cur, step = dt / dur;
        v = v < target ? Math.min(target, v + step) : Math.max(target, v - step);
        anim.put(k, v);
        return v;
    }

    private float peek(Object o, int kind, float def) {
        Float v = anim.get(new K(o, kind));
        return v == null ? def : v;
    }


    private static float ty(float center, float size) { return center - size * 0.58f; }

    private int A(int c) {
        int a = Math.round(((c >>> 24) & 0xFF) * alpha);
        return (a << 24) | (c & 0xFFFFFF);
    }

    private void fill(float x, float y, float w, float h, float r, int c) { SdfRect.fill(m, x, y, w, h, r, A(c)); }
    private void border(float x, float y, float w, float h, float r, float t, int c) { SdfRect.border(m, x, y, w, h, r, t, A(c)); }
    private void grad(float x, float y, float w, float h, float r) { SdfRect.flow(m, x, y, w, h, r, alpha); }
    private void gradA(float x, float y, float w, float h, float r, float a) { if (a > 0.003f) SdfRect.flow(m, x, y, w, h, r, alpha * a); }
    private void gglow(float x, float y, float w, float h, float r, float spread, int a) {
        if (!daamky.render.Theme.glow) return;
        SdfRect.flowGlow(m, x, y, w, h, r, spread, Math.round(a * alpha));
    }
    private void gglowA(float x, float y, float w, float h, float r, float spread, int a, float k) {
        int v = Math.round(a * alpha * k);
        if (v > 0 && daamky.render.Theme.glow) SdfRect.flowGlow(m, x, y, w, h, r, spread, v);
    }
    private void vgrad(float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        SdfRect.gradient(m, x, y, w, h, 0, A(tl), A(tr), A(br), A(bl));
    }
    private float tb(String s, float x, float y, float size, int c) { return Txt.bold(m, s, x, y, size, A(c)); }
    private float tr(String s, float x, float y, float size, int c) { return Txt.reg(m, s, x, y, size, A(c)); }
    private void ico(int cp, float x, float y, float size, int c) {
        String img = daamky.render.Img.forIcon(cp);
        if (img != null && daamky.render.Img.draw(m, img, x, y, size, size, A(c))) return;
        Txt.icon(m, cp, x, y, size, A(c));
    }
    private void hit(float x, float y, float w, float h, Runnable l, Runnable r) { if (!closing) hits.add(new Hit(x, y, w, h, l, r)); }

    private boolean hov(float x, float y, float w, float h) {
        return !covered && !closing && mlx >= x && mlx < x + w && mly >= y && mly < y + h;
    }

    private boolean hovClip(float x, float y, float w, float h, float vt, float vb) {
        float y0 = Math.max(y, vt), y1 = Math.min(y + h, vb);
        return y1 > y0 && hov(x, y0, w, y1 - y0);
    }


    private void updateCover(int idx) {
        covered = false;
        for (int j = idx + 1; j < panels.size(); j++) {
            Panel p = panels.get(j);
            if (!p.closing && mlx >= p.x && mlx < p.x + PANEL_W && mly >= p.y && mly < p.y + p.height()) { covered = true; return; }
        }
    }



    private void scissor(float x1, float y1, float x2, float y2) {
        int l = (int) Math.floor(curOx + x1 * curS), tp = (int) Math.floor(curOy + y1 * curS);
        int r = (int) Math.ceil(curOx + x2 * curS), bt = (int) Math.ceil(curOy + y2 * curS);
        daamky.render.Gfx.scissor = new net.minecraft.client.gui.navigation.ScreenRectangle(l, tp, Math.max(0, r - l), Math.max(0, bt - tp));
    }

    private void endScissor() { daamky.render.Gfx.scissor = null; }

    private static List<String> wrap(String s, float size, float maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : s.split(" ")) {
            String t = cur.length() == 0 ? word : cur + " " + word;
            if (Txt.wBold(t, size) > maxW && cur.length() > 0) { lines.add(cur.toString()); cur = new StringBuilder(word); }
            else { cur = new StringBuilder(t); }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }


    float rowH(Setting s) {
        if (s instanceof Section || s instanceof Group) return 23;
        if (s instanceof Button) return 25.5f;
        if (s instanceof Bool b) return 25.5f + (wrap(b.name, 9.5f, LABEL_W).size() - 1) * 11.5f;
        if (s instanceof Slider) return SLIDER_H;
        if (s instanceof TextValue || s instanceof ColorValue || s instanceof BindValue) return 29;
        return 28;
    }

    private void hitClip(float x, float y, float w, float h, float vt, float vb, Runnable l, Runnable r) {
        float y0 = Math.max(y, vt), y1 = Math.min(y + h, vb);
        if (y1 > y0) hit(x, y0, w, y1 - y0, l, r);
    }

    private void cfgButton(String label, float x, float y, float w, float h, boolean enabled, boolean primary, Runnable action) {
        String key = "cfgbtn:" + label;
        float hv = ap(key, HOV, enabled && hov(x, y, w, h) ? 1f : 0f, 16f);
        float pr = ap(key, POP, 0f, 10f);
        fill(x, y, w, h, 8, 0xFF101B2A);
        if (enabled) gradA(x, y, w, h, 8, primary ? 0.8f + 0.2f * hv : 0.25f + 0.4f * hv + 0.3f * pr);
        border(x, y, w, h, 8, 1f, mix(0xFF263A55, 0x38FFFFFF, hv));
        float tw = Txt.wBold(label, 9);
        tb(label, x + (w - tw) / 2f, ty(y + h / 2f, 9), 9, enabled ? WHITE : 0xFF5E6E84);
        if (enabled) hit(x, y, w, h, () -> { action.run(); anim.put(new K(key, POP), 1f); }, null);
    }

    private void drawPanel(Panel p) {

        p.t = clamp01(p.t + (p.closing ? -dt / PANEL_CLOSE : dt / PANEL_OPEN));
        if (p.closing && p.t <= 0f) { panels.remove(p); return; }
        float k = p.closing ? p.t * p.t : easeOutBack(p.t, BOUNCE);
        float pa = clamp01(p.t * 2.5f);

        for (Setting s : p.mod.flat()) if (s instanceof Mode md) ap(md, DD, md.open ? 1f : 0f, 13f);

        float x = p.x, y = p.y, h = p.height();
        float pcx = x + PANEL_W / 2f, pcy = y + h / 2f, sc = 0.65f + 0.35f * k, dy = 0f;
        Matrix3x2f outer = m;
        m = new Matrix3x2f(outer).translate(pcx, pcy + dy).scale(sc, sc).translate(-pcx, -pcy);
        curOx = ox + S * pcx * (1f - sc);
        curOy = oy + S * (pcy * (1f - sc) + dy);
        curS = S * sc;
        alpha = ga * pa;
        updateCover(panels.indexOf(p));

        float vt = y + HEAD_H, vb = y + h - 3, vh = vb - vt;
        p.maxScroll = Math.max(0, p.contentH() - vh);
        p.scrollT = Math.max(0, Math.min(p.maxScroll, p.scrollT));
        p.scroll = Math.max(0, Math.min(p.maxScroll, lerpTo(p.scroll, p.scrollT, 18f)));
        if (!p.closing) hit(x, y, PANEL_W, h, () -> front(p), () -> front(p));

        gglow(x, y, PANEL_W, h, 9, 11, 0x55);
        fill(x, y, PANEL_W, h, 9, PANEL_BG);
        border(x, y, PANEL_W, h, 9, 1f, 0xFF24364F);


        grad(x, y, PANEL_W, HEAD_H, 8);
        ico(Icons.GEAR, x + 8, y + 4, 12, WHITE);
        float tw = Txt.wBold(p.mod.name, 11);
        tb(p.mod.name, x + (PANEL_W - tw) / 2f, ty(y + HEAD_H / 2f, 11), 11, WHITE);
        ico(Icons.CLOSE, x + PANEL_W - 18, y + 4.5f, 11, WHITE);
        hit(x, y, PANEL_W - 24, HEAD_H, () -> { front(p); drag = p; dragX = cx - p.x; dragY = cy - p.y; }, null);
        hit(x + PANEL_W - 24, y, 24, HEAD_H, () -> p.closing = true, null);


        popup = null;
        scissor(x + 1, vt, x + PANEL_W - 1, vb);
        float ry = vt - p.scroll;
        for (Setting s : Modules.rows(p.mod)) {
            float rh = rowH(s);
            if (ry + rh >= vt && ry <= vb) drawSetting(p, s, x, ry, rh, vt, vb);
            ry += rh;
        }
        endScissor();


        int clear = 0x000A0A0C;
        if (p.scroll > 0.5f) vgrad(x + 1, vt, PANEL_W - 2, 14, PANEL_BG, PANEL_BG, clear, clear);
        if (p.scroll < p.maxScroll - 0.5f) vgrad(x + 1, vb - 14, PANEL_W - 2, 14, clear, clear, PANEL_BG, PANEL_BG);


        if (p.maxScroll > 0) {
            float trT = vt + 6, trH = vh - 12, ch = p.contentH();
            float th = Math.max(24, trH * vh / ch), thY = trT + (trH - th) * (p.scroll / p.maxScroll);
            float sx = x + PANEL_W - 11;
            fill(sx, trT, 4, trH, 2, 0xFF16161A);
            fill(sx, thY, 4, th, 2, THUMB);
        }
        if (popup != null) drawPopup(popup);
        popup = null;
        m = outer;
    }

    private void drawSetting(Panel p, Setting s, float x, float ry, float rh, float vt, float vb) {
        float right = x + PANEL_W - 19;
        if (s instanceof Section sec) {
            float cy = ry + rh / 2f, w = Txt.wBold(sec.name, 9.5f), cx = x + (PANEL_W - w) / 2f;
            tb(sec.name, cx, ty(cy, 9.5f), 9.5f, ACCENT_TXT);
            fill(x + 8, cy, cx - 7 - (x + 8), 1, 0, 0xFF20314A);
            fill(cx + w + 7, cy, right - (cx + w + 7), 1, 0, 0xFF20314A);
        } else if (s instanceof Group g) {
            drawGroup(g, x, ry, rh, right, vt, vb);
        } else if (s instanceof Button btn) {
            drawButton(btn, x, ry, right, vt, vb);
        } else if (s instanceof Bool b) {
            drawBool(b, x, ry, rh, right, vt, vb);
        } else if (s instanceof Slider sl) {
            drawSlider(sl, x, ry, right, vt, vb);
        } else if (s instanceof Mode md) {
            drawMode(p, md, x, ry, right, vt, vb);
        } else if (s instanceof TextValue text) {
            drawTextValue(text, x, ry, right, vt, vb);
        } else if (s instanceof ColorValue color) {
            drawColorValue(color, x, ry, right, vt, vb);
        } else if (s instanceof BindValue bind) {
            drawBindValue(bind, x, ry, right, vt, vb);
        }
    }


    private void drawTextValue(TextValue value, float x, float ry, float right, float vt, float vb) {
        tb(value.name, x + 8, ty(ry + 14.5f, 9.5f), 9.5f, WHITE);
        String shown = editingText == value ? editBuffer + "|" : value.text();
        if (shown.isEmpty()) shown = "—";
        float bw = 82, bh = 17, bx = right - bw, by = ry + 6;
        fill(bx, by, bw, bh, 6, 0xFF101B2A);
        border(bx, by, bw, bh, 6, 1f, editingText == value ? 0xFF79B4FF : 0xFF263A55);
        tb(clipText(shown, bw - 10, 8f), bx + 5, ty(by + bh / 2f, 8f), 8f,
                editingText == value ? ACCENT_TXT : 0xFFB9C8DA);
        hitClip(bx, ry, bw, 29, vt, vb, () -> {
            editingText = value; editingColor = null; editBuffer = value.text();
        }, null);
    }

    private void drawColorValue(ColorValue value, float x, float ry, float right, float vt, float vb) {
        tb(value.name, x + 8, ty(ry + 14.5f, 9.5f), 9.5f, WHITE);
        String shown = editingColor == value ? editBuffer + "|" : value.text();
        float bw = 82, bh = 17, bx = right - bw, by = ry + 6;
        fill(bx, by, bw, bh, 6, 0xFF101B2A);
        fill(bx + 3, by + 3, 10, bh - 6, 3, value.value.getRGB());
        border(bx, by, bw, bh, 6, 1f, editingColor == value ? 0xFF79B4FF : 0xFF263A55);
        tb(clipText(shown, bw - 18, 8f), bx + 16, ty(by + bh / 2f, 8f), 8f,
                editingColor == value ? ACCENT_TXT : 0xFFB9C8DA);
        hitClip(bx, ry, bw, 29, vt, vb, () -> {
            editingColor = value; editingText = null; editBuffer = value.text();
        }, null);
    }

    private void drawBindValue(BindValue value, float x, float ry, float right, float vt, float vb) {
        tb(value.name, x + 8, ty(ry + 14.5f, 9.5f), 9.5f, WHITE);
        boolean waiting = bindingSetting == value;
        String shown = waiting ? "PRESS KEY" : value.text();
        float bw = 82, bh = 17, bx = right - bw, by = ry + 6;
        fill(bx, by, bw, bh, 6, 0xFF101B2A);
        border(bx, by, bw, bh, 6, 1f, waiting ? 0xFF79B4FF : 0xFF263A55);
        float tw = Txt.wBold(shown, 8f);
        tb(shown, bx + (bw - tw) / 2f, ty(by + bh / 2f, 8f), 8f, waiting ? ACCENT_TXT : 0xFFB9C8DA);
        hitClip(bx, ry, bw, 29, vt, vb, () -> {
            bindingSetting = value; editingText = null; editingColor = null;
        }, () -> value.set(KeyBind.NONE));
    }

    private String clipText(String value, float maxWidth, float size) {
        String result = value == null ? "" : value;
        while (!result.isEmpty() && Txt.wBold(result, size) > maxWidth) result = result.substring(1);
        return result;
    }

    private void drawGroup(Group g, float x, float ry, float rh, float right, float vt, float vb) {
        String groupLabel = g instanceof Multi multi ? g.name + " (" + multi.selectedCount() + ")" : g.name;
        float cy = ry + rh / 2f, w = Txt.wBold(groupLabel, 9.5f), cx = x + (PANEL_W - w) / 2f;
        float hv = ap(g, HOV, hovClip(x + 4, ry, PANEL_W - 8, rh, vt, vb) ? 1f : 0f, 16f);
        tb(groupLabel, cx, ty(cy, 9.5f), 9.5f, mix(ACCENT_TXT, WHITE, hv * 0.5f));
        fill(x + 8, cy, cx - 7 - (x + 8), 1, 0, 0xFF20314A);
        fill(cx + w + 7, cy, right - 12 - (cx + w + 7), 1, 0, 0xFF20314A);
        float t = ap(g, TOG, g.open ? 1f : 0f, 14f);
        Matrix3x2f saved = m;
        float ax = right - 7f, ay = cy;
        m = new Matrix3x2f(saved).translate(ax, ay).rotate((float) Math.PI * t).translate(-ax, -ay);
        ico(Icons.CHEVRON, ax - 3.25f, ay - 3.25f, 6.5f, WHITE);
        m = saved;
        hitClip(x + 4, ry, PANEL_W - 8, rh, vt, vb, () -> g.open = !g.open, null);
    }


    private void drawButton(Button b, float x, float ry, float right, float vt, float vb) {
        float bx = x + 8, bw = right - bx, bh = 17, by = ry + (25.5f - bh) / 2f;
        float hv = ap(b, HOV, hovClip(bx, by, bw, bh, vt, vb) ? 1f : 0f, 16f);
        float pr = ap(b, POP, 0f, 10f);
        fill(bx, by, bw, bh, 8, 0xFF101B2A);
        gradA(bx, by, bw, bh, 8, 0.35f + 0.4f * hv + 0.25f * pr);
        border(bx, by, bw, bh, 8, 1f, mix(0xFF263A55, 0x38FFFFFF, hv));
        float tw = Txt.wBold(b.name, 8.5f);
        tb(b.name, bx + (bw - tw) / 2f, ty(by + bh / 2f, 8.5f), 8.5f, WHITE);
        hitClip(bx, by, bw, bh, vt, vb, () -> { b.press(); anim.put(new K(b, POP), 1f); }, null);
    }


    private void drawBool(Bool b, float x, float ry, float rh, float right, float vt, float vb) {
        List<String> lines = wrap(b.name, 9.5f, LABEL_W);
        float c0 = ry + 12.75f;
        for (int i = 0; i < lines.size(); i++) tb(lines.get(i), x + 8, ty(c0 + i * 11.5f, 9.5f), 9.5f, WHITE);
        float tw = 25, th = 12, tx = right - tw, tyy = c0 - th / 2f, kn = th - 3;
        float t = ap(b, TOG, b.value ? 1f : 0f, 14f);
        float kw = kn + (float) Math.sin(Math.PI * t) * 3f;
        fill(tx, tyy, tw, th, 6, TOG_OFF);
        gradA(tx, tyy, tw, th, 6, t);
        fill(tx + 1.5f + t * (tw - 3f - kw), tyy + 1.5f, kw, kn, kn / 2f, KNOB);
        hitClip(x + 4, ry, PANEL_W - 8, rh, vt, vb, () -> b.set(!b.value), null);
    }


    private void drawSlider(Slider sl, float x, float ry, float right, float vt, float vb) {
        tb(sl.name, x + 8, ty(ry + 10, 9.5f), 9.5f, WHITE);
        String val = sl.text();
        tb(val, right - Txt.wBold(val, 8.5f), ty(ry + 10, 8.5f), 8.5f, ACCENT_TXT);

        float trx = x + 8, trw = right - trx, trY = ry + 23;
        boolean dragging = dragSlider == sl;
        boolean over = hovClip(trx - 5, ry + 14, trw + 10, 18, vt, vb);
        float shown = ap(sl, SL, sl.frac(), dragging ? 40f : 16f);
        float hv = ap(sl, HOV, dragging || over ? 1f : 0f, 16f);

        fill(trx, trY - 2, trw, 4, 2, 0xFF1A2A40);
        float fw = Math.max(4f, trw * shown);
        grad(trx, trY - 2, fw, 4, 2);
        float kr = 4f + 1.5f * hv, kx = trx + trw * shown;
        gglowA(kx - kr, trY - kr, kr * 2, kr * 2, kr, 5, 0x60, hv);
        fill(kx - kr, trY - kr, kr * 2, kr * 2, kr, KNOB);

        final float fx = trx, fwid = trw;
        hitClip(trx - 5, ry + 14, trw + 10, 18, vt, vb, () -> {
            dragSlider = sl; dragSliderX = fx; dragSliderW = fwid;
            sl.set(sl.min + clamp01((cx - fx) / fwid) * (sl.max - sl.min));
        }, null);
    }


    private void drawMode(Panel p, Mode md, float x, float ry, float right, float vt, float vb) {
        float c0 = ry + 14;
        tb(md.name, x + 8, ty(c0, 9.5f), 9.5f, WHITE);
        float maxW = 0;
        for (String o : md.modes) maxW = Math.max(maxW, Txt.wBold(o, 8.5f));
        float dd = peek(md, DD, md.open ? 1f : 0f);

        float cw = Txt.wBold(md.get(), 8.5f) + 7 + 15, mw = maxW + 7 + 15;
        float bw = cw + (mw - cw) * dd, bh = 15, bx = right - bw, by = c0 - bh / 2f;
        if (dd < 0.01f) {
            gglow(bx, by, bw, bh, 7.5f, 5, 0x50);
            grad(bx, by, bw, bh, 7.5f);
            tb(md.get(), bx + 7, ty(c0, 8.5f), 8.5f, WHITE);
            ico(Icons.CHEVRON, bx + bw - 11.5f, c0 - 3.25f, 6.5f, WHITE);
        } else {
            popup = new Popup(md, bx, by, bw, bh, vt, vb);
        }
        hitClip(bx, by, bw, bh, vt, vb, () -> {
            ddKeep = true;
            md.open = !md.open;
            if (md.open) for (Setting o : p.mod.flat()) if (o instanceof Mode om && om != md) om.open = false;
        }, null);
    }

    private void drawPopup(Popup pp) {
        Mode md = pp.md;
        float dd = peek(md, DD, md.open ? 1f : 0f);
        int n = md.modes.length;
        float full = n * OPT_H + 4, listH = dd * full, h = pp.bh + listH;
        float bx = pp.bx, by = pp.by, bw = pp.bw, c0 = by + pp.bh / 2f;

        gglow(bx, by, bw, pp.bh, 7.5f, 5, 0x50);
        fill(bx, by, bw, h, 7.5f, 0xFF0C1624);
        border(bx, by, bw, h, 7.5f, 1f, 0xFF263A55);
        grad(bx, by, bw, pp.bh, 7.5f);
        tb(md.get(), bx + 7, ty(c0, 8.5f), 8.5f, WHITE);
        Matrix3x2f saved = m;
        float acx = bx + bw - 8.25f;
        m = new Matrix3x2f(saved).translate(acx, c0).rotate((float) Math.PI * dd).translate(-acx, -c0);
        ico(Icons.CHEVRON, bx + bw - 11.5f, c0 - 3.25f, 6.5f, WHITE);
        m = saved;

        float sa = ap(md, SEL, md.index, 16f);
        float listTop = by + pp.bh + 2;
        float base = alpha;
        float pillA = clamp01((listH - 2 - sa * OPT_H) / OPT_H);
        gradA(bx + 3, listTop + sa * OPT_H, bw - 6, OPT_H - 2, 5, pillA);
        for (int i = 0; i < n; i++) {
            float oy0 = listTop + i * OPT_H;
            float oa = clamp01((listH - 2 - i * OPT_H) / OPT_H);
            if (oa <= 0.01f) break;
            alpha = base * oa;
            boolean sel = i == md.index;
            if (!sel && md.open && hovClip(bx + 3, oy0, bw - 6, OPT_H - 2, -1e6f, 1e6f))
                fill(bx + 3, oy0, bw - 6, OPT_H - 2, 5, 0x1CFFFFFF);
            tb(md.modes[i], bx + 7, ty(oy0 + (OPT_H - 2) / 2f, 8.5f), 8.5f, sel ? WHITE : 0xFFA8A8B2);
            alpha = base;
            if (md.open && dd > 0.6f) {
                final int idx = i;
                hit(bx + 3, oy0, bw - 6, OPT_H - 2, () -> { md.select(idx); md.open = false; }, null);
            }
        }
    }

    // =========================================================== ring

    static float clampTo(float v, float lo, float hi) { return hi < lo ? lo : Math.max(lo, Math.min(hi, v)); }

    private int sectorAt(float lx, float ly) {
        float dx = lx - CX, dy = ly - CY, d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < R_IN || d > R_OUT + 10f) return -1;
        double a = Math.atan2(dy, dx) + Math.PI / 2.0 + SEG / 2.0;
        double full = Math.PI * 2.0;
        a = ((a % full) + full) % full;
        return Math.min(N - 1, (int) (a / SEG));
    }

    private boolean hasWin(Sec s) {
        for (Win w : wins) if (w.sec == s && !w.closing) return true;
        return false;
    }

    private boolean overWin() {
        for (Win w : wins) {
            if (!w.closing && mlx >= w.x && mlx < w.x + WIN_W && mly >= w.y && mly < w.y + w.height()) return true;
        }
        return false;
    }

    /** Arc of the ring drawn as a chain of small rotated rounded rects with the theme gradient. */
    private void arc(float cxx, float cyy, float r, float a0, float a1, float th, float k) {
        int n = Math.max(3, (int) Math.ceil(Math.abs(a1 - a0) * r / 5f));
        float step = (a1 - a0) / n, len = Math.abs(step) * r + th * 0.8f;
        Matrix3x2f saved = m;
        for (int i = 0; i < n; i++) {
            float am = a0 + step * (i + 0.5f);
            float px = cxx + (float) Math.cos(am) * r, py = cyy + (float) Math.sin(am) * r;
            m = new Matrix3x2f(saved).translate(px, py).rotate(am + (float) Math.PI / 2f);
            gradA(-len / 2f, -th / 2f, len, th, th / 2f, k);
        }
        m = saved;
    }

    private void drawRing(float k) {
        float rs = 0.55f + 0.45f * k;
        boolean blocked = covered || overWin();
        hoverSec = (!closing && !blocked) ? sectorAt(mlx, mly) : -1;

        Matrix3x2f saved = m;
        m = new Matrix3x2f(saved).translate(CX, CY).scale(rs, rs).translate(-CX, -CY);

        float rd = R_OUT + 8f;
        fill(CX - rd, CY - rd, rd * 2f, rd * 2f, rd, 0x66060A12);
        border(CX - R_OUT, CY - R_OUT, R_OUT * 2f, R_OUT * 2f, R_OUT, 1.2f, 0x55FFFFFF);
        border(CX - R_IN, CY - R_IN, R_IN * 2f, R_IN * 2f, R_IN, 1f, 0x33FFFFFF);

        for (int i = 0; i < N; i++) {
            Sec s = SECS[i];
            float hs = ap(s, HOV, hoverSec == i ? 1f : 0f, 14f);
            float se = ap(s, NAVK, hasWin(s) ? 1f : 0f, 12f);
            float a = Math.max(hs * 0.85f, se);
            float th0 = -(float) Math.PI / 2f + i * SEG;

            // staggered fly-in: items come out of the centre one after another while spinning a little
            float in = closing ? 1f : clamp01((age - 0.05f - i * 0.045f) / 0.42f);
            float ie = easeOutBack(in, 1.7f);
            float ang = th0 - (1f - in) * 0.7f;
            float rad = R_ICON * (0.25f + 0.75f * ie) + 4f * hs;

            if (a > 0.01f) {
                float half = SEG * 0.5f * 0.86f * easeOutCubic(a);
                arc(CX, CY, R_OUT, th0 - half, th0 + half, 2f + 3f * a, a);
            }

            float ix = CX + (float) Math.cos(ang) * rad, iy = CY + (float) Math.sin(ang) * rad;
            float base = alpha;
            alpha = base * clamp01(in * 2f);
            gglowA(ix - 14, iy - 14, 28, 28, 14, 9, 0x70, a);
            int tc = mix(0xFFB6C4D8, WHITE, Math.max(hs, se));
            float isz = 16f * (1f + 0.14f * hs);
            ico(s.icon, ix - isz / 2f, iy - isz / 2f - 5f, isz, tc);
            float lw = Txt.wBold(s.label, 7.5f);
            tb(s.label, ix - lw / 2f, ty(iy + 11f, 7.5f), 7.5f, tc);
            alpha = base;
        }

        // centre: logo + name of the hovered category (cross-fades)
        float cin = easeOutBack(clamp01(age / 0.45f), 2f);
        float lsz = 30f * (closing ? 1f : (0.4f + 0.6f * cin));
        gglow(CX - lsz / 2f, CY - 28f, lsz, lsz, 9, 8, 0x50);
        grad(CX - lsz / 2f, CY - 28f, lsz, lsz, 9);
        ico(Icons.ATOM, CX - 11f * lsz / 30f, CY - 28f + (lsz - 22f * lsz / 30f) / 2f, 22f * lsz / 30f, WHITE);

        String want = hoverSec >= 0 ? SECS[hoverSec].label : "Daamky";
        if (!want.equals(centerShown)) {
            centerT -= dt / 0.09f;
            if (centerT <= 0f) { centerT = 0f; centerShown = want; }
        } else {
            centerT = Math.min(1f, centerT + dt / 0.14f);
        }
        float ca = alpha;
        alpha = ca * clamp01(centerT);
        float tw = Txt.wBold(centerShown, 11);
        tb(centerShown, CX - tw / 2f, ty(CY + 20f + (1f - centerT) * 3f, 11), 11, WHITE);
        alpha = ca;

        m = saved;
    }

    // =========================================================== windows

    private void clickSector(int i) {
        Sec s = SECS[i];
        boolean wasOpen = hasWin(s);
        for (Win w : wins) if (!w.closing) closeWin(w);
        if (wasOpen) return;
        Win w = newWin(i);
        wins.add(w);
        if (s.cat == CAT_CONFIGS) ConfigsBridge.refresh();
    }

    private void closeWin(Win w) {
        w.closing = true;
        cfgFocus = false;
        if (dragWin == w) dragWin = null;
        for (Panel p : panels) if (p.mod.category == w.sec.cat) p.closing = true;
    }

    private Win newWin(int i) {
        float th = -(float) Math.PI / 2f + i * SEG;
        boolean right = Math.cos(th) >= -0.01;
        Win w = new Win(SECS[i], right);
        float h = w.height();
        float ay = CY + (float) Math.sin(th) * (R_OUT + 10f);
        float x = right ? CX + R_OUT + 18f : CX - R_OUT - 18f - WIN_W;
        w.x = clampTo(x, vx0 + 4f, vx1 - WIN_W - 4f);
        w.y = clampTo(ay - h / 2f, vy0 + 4f, vy1 - h - 4f);
        return w;
    }

    private void front(Panel p) { panels.remove(p); panels.add(p); }

    private void openPanel(Mod mod, Win from) {
        for (Panel p : panels) {
            if (p.mod == mod) {
                for (Panel o : panels) if (o != p) o.closing = true;
                p.closing = false;
                front(p);
                return;
            }
        }
        for (Panel o : panels) o.closing = true;
        Panel np = new Panel(mod, 0, 0);
        float h = np.height();
        float px = from.right ? from.x + WIN_W + 8f : from.x - 8f - PANEL_W;
        if (px + PANEL_W > vx1 - 4f) px = from.x - 8f - PANEL_W;
        if (px < vx0 + 4f) px = from.x + WIN_W + 8f;
        np.x = clampTo(px, vx0 + 4f, vx1 - PANEL_W - 4f);
        np.y = clampTo(from.y, vy0 + 4f, vy1 - h - 4f);
        panels.add(np);
    }

    private void drawWin(Win w) {
        updateCover(-1);
        w.t = clamp01(w.t + (w.closing ? -dt / WIN_CLOSE : dt / WIN_OPEN));
        if (!w.closing) w.age += dt;
        if (w.closing && w.t <= 0f) { wins.remove(w); return; }
        float k = w.closing ? w.t * w.t : easeOutBack(w.t, BOUNCE);
        float wa = clamp01(w.t * 2.5f);
        boolean live = !w.closing && !closing;
        float x = w.x, y = w.y, h = w.height();

        // the window grows out of the edge that faces the ring
        float px = w.right ? x : x + WIN_W;
        float py = Math.max(y, Math.min(y + h, CY));
        float sc = 0.6f + 0.4f * k;
        Matrix3x2f outer = m;
        m = new Matrix3x2f(outer).translate(px, py).scale(sc, sc).translate(-px, -py);
        curOx = ox + S * px * (1f - sc);
        curOy = oy + S * py * (1f - sc);
        curS = S * sc;
        alpha = ga * wa;

        gglow(x, y, WIN_W, h, 10, 11, 0x55);
        fill(x, y, WIN_W, h, 10, PANEL_BG);
        border(x, y, WIN_W, h, 10, 1f, 0xFF24364F);
        grad(x, y, WIN_W, WIN_HEAD, 9);
        ico(w.sec.icon, x + 8, y + 5.5f, 13, WHITE);
        float tw = Txt.wBold(w.sec.label, 11);
        tb(w.sec.label, x + (WIN_W - tw) / 2f, ty(y + WIN_HEAD / 2f, 11), 11, WHITE);
        ico(Icons.CLOSE, x + WIN_W - 18, y + 6.5f, 11, WHITE);
        if (live) {
            hit(x, y, WIN_W, h, null, null);
            hit(x, y, WIN_W - 24, WIN_HEAD, () -> { dragWin = w; dragWX = cx - w.x; dragWY = cy - w.y; }, null);
            hit(x + WIN_W - 24, y, 24, WIN_HEAD, () -> closeWin(w), null);
        }

        float vt = y + WIN_HEAD + 2, vb = y + h - 4;
        if (w.configs()) drawConfigWin(w, x, y, h, live);
        else drawModRows(w, x, vt, vb, live);
        m = outer;
    }

    private void drawModRows(Win w, float x, float vt, float vb, boolean live) {
        float vh = vb - vt;
        w.maxScroll = Math.max(0, w.contentH() - vh);
        w.scrollT = Math.max(0, Math.min(w.maxScroll, w.scrollT));
        w.scroll = Math.max(0, Math.min(w.maxScroll, lerpTo(w.scroll, w.scrollT, 18f)));

        scissor(x + 1, vt, x + WIN_W - 1, vb);
        float base = alpha;
        for (int i = 0; i < w.mods.size(); i++) {
            Mod mod = w.mods.get(i);
            float rx = x + 6, rw = WIN_W - 18, rh = ROW_H - 3;
            float ry = vt + 3 + i * ROW_H - w.scroll;
            if (ry + rh < vt || ry > vb) continue;

            // rows slide in one after another when the window opens
            float ra = clamp01((w.age - Math.min(i, 10) * 0.03f) / 0.2f);
            float slide = (1f - easeOutCubic(ra)) * (w.right ? -14f : 14f);
            alpha = base * ra;

            float vy = Math.max(ry, vt), vhh = Math.min(ry + rh, vb) - vy;
            float en = ap(mod, EN, mod.enabled ? 1f : 0f, 11f);
            float tg = ap(mod, TOG, mod.enabled ? 1f : 0f, 14f);
            float hv = ap(mod, HOV, live && vhh > 0 && hov(rx, vy, rw, vhh) ? 1f : 0f, 16f);
            float pop = ap(mod, POP, 0f, 9f);

            Matrix3x2f saved = m;
            float sc = 1f - 0.04f * pop, mx0 = rx + rw / 2f, my0 = ry + rh / 2f;
            m = new Matrix3x2f(saved).translate(slide, 0f).translate(mx0, my0).scale(sc, sc).translate(-mx0, -my0);

            gglowA(rx, ry, rw, rh, 6, 7, 0x70, en);
            fill(rx, ry, rw, rh, 6, CARD_OFF);
            gradA(rx, ry, rw, rh, 6, en);
            border(rx, ry, rw, rh, 6, 1f, mix(mix(CARD_OFF_BORDER, 0xFF4779B4, hv), 0x38FFFFFF, en));
            if (hv > 0.01f) fill(rx, ry, rw, rh, 6, ((int) (0x0C * hv * (1f - en)) << 24) | 0xFFFFFF);

            final float ts = 9.5f;
            tb(mod.name, rx + 8, ty(ry + rh / 2f, ts), ts, WHITE);
            if (mod.hasSettings()) {
                float gx = rx + 8 + Txt.wBold(mod.name, ts) + 4;
                ico(Icons.GEAR, gx, ry + rh / 2f - 4.5f, 9, 0xFFB9C8DA);
            }

            float tw = 25, th = 12, tx = rx + rw - 8 - tw, tyy = ry + rh / 2f - th / 2f, kn = th - 3;
            float kw = kn + (float) Math.sin(Math.PI * tg) * 3f;
            fill(tx, tyy, tw, th, 6, TOG_OFF);
            gradA(tx, tyy, tw, th, 6, tg);
            fill(tx + 1.5f + tg * (tw - 3f - kw), tyy + 1.5f, kw, kn, kn / 2f, KNOB);

            if (binding == mod || mod.hasBind()) {
                String bt = binding == mod ? "[...]" : "[" + mod.keyDisplay() + "]";
                tb(bt, tx - 6 - Txt.wBold(bt, 7.5f), ty(ry + rh / 2f, 7.5f), 7.5f, binding == mod ? ACCENT_TXT : 0xFFE1EEFF);
            }
            m = saved;

            if (live && vhh > 0) {
                hits.add(new Hit(rx, vy, rw, vhh,
                        () -> { mod.setEnabled(!mod.enabled); anim.put(new K(mod, POP), 1f); },
                        mod.hasSettings() ? () -> openPanel(mod, w) : null,
                        () -> binding = mod));
            }
        }
        alpha = base;
        endScissor();

        int clear = 0x000A0A0C;
        if (w.scroll > 0.5f) vgrad(x + 1, vt, WIN_W - 2, 14, PANEL_BG, PANEL_BG, clear, clear);
        if (w.scroll < w.maxScroll - 0.5f) vgrad(x + 1, vb - 14, WIN_W - 2, 14, clear, clear, PANEL_BG, PANEL_BG);
        if (w.maxScroll > 0) {
            float trT = vt + 6, trH = vh - 12, ch = w.contentH();
            float th = Math.max(24, trH * vh / ch), thY = trT + (trH - th) * (w.scroll / w.maxScroll);
            float sx = x + WIN_W - 9;
            fill(sx, trT, 3, trH, 1.5f, 0xFF16161A);
            fill(sx, thY, 3, th, 1.5f, THUMB);
        }
    }

    private static String fitEnd(String s, float size, float maxW) {
        String r = s == null ? "" : s;
        while (r.length() > 1 && Txt.wBold(r, size) > maxW) r = r.substring(0, r.length() - 1);
        return r;
    }

    private void drawConfigWin(Win w, float x, float y, float h, boolean live) {
        float ix = x + 10, iw = WIN_W - 20, y0 = y + WIN_HEAD + 8, fh = 22;

        float fo = ap("cfgfield", EN, cfgFocus ? 1f : 0f, 14f);
        fill(ix, y0, iw, fh, 8, 0xFF101B2A);
        border(ix, y0, iw, fh, 8, 1f, mix(0xFF263A55, 0xFF4C96F7, fo));
        String typed = ConfigsBridge.name;
        if (typed.isEmpty() && !cfgFocus) {
            tr("Например: legit", ix + 9, ty(y0 + fh / 2f, 9), 9, DIM);
        } else {
            String shown = typed + (cfgFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            while (!shown.isEmpty() && Txt.wBold(shown, 9) > iw - 18) shown = shown.substring(1);
            tb(shown, ix + 9, ty(y0 + fh / 2f, 9), 9, WHITE);
        }
        if (live) hit(ix, y0, iw, fh, () -> cfgFocus = true, null);
        cfgButton("Сохранить", ix, y0 + 28, iw, 22, true, true, ConfigsBridge::save);

        float lt = y0 + 58, lb = y + h - 92, lh = lb - lt;
        List<String> list = ConfigsBridge.list();
        float pitch = 28f, rowH = 24f, total = list.size() * pitch + 4;
        w.maxScroll = Math.max(0, total - lh);
        w.scrollT = Math.max(0, Math.min(w.maxScroll, w.scrollT));
        w.scroll = Math.max(0, Math.min(w.maxScroll, lerpTo(w.scroll, w.scrollT, 18f)));

        scissor(x + 1, lt, x + WIN_W - 1, lb);
        float base = alpha;
        if (list.isEmpty()) {
            String t1 = "Конфигов пока нет";
            tb(t1, x + (WIN_W - Txt.wBold(t1, 9.5f)) / 2f, ty(lt + 22, 9.5f), 9.5f, DIM);
        }
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i);
            float rx = x + 6, rw = WIN_W - 18, ry = lt + 2 + i * pitch - w.scroll;
            if (ry + rowH < lt || ry > lb) continue;
            float ra = clamp01((w.age - Math.min(i, 8) * 0.03f) / 0.2f);
            float slide = (1f - easeOutCubic(ra)) * (w.right ? -14f : 14f);
            alpha = base * ra;
            boolean sel = name.equals(ConfigsBridge.selected);
            float vy = Math.max(ry, lt), vhh = Math.min(ry + rowH, lb) - vy;
            float en = ap(name, EN, sel ? 1f : 0f, 11f);
            float hv = ap(name, HOV, live && vhh > 0 && hov(rx, vy, rw, vhh) ? 1f : 0f, 16f);
            Matrix3x2f saved = m;
            m = new Matrix3x2f(saved).translate(slide, 0f);
            gglowA(rx, ry, rw, rowH, 6, 7, 0x70, en);
            fill(rx, ry, rw, rowH, 6, CARD_OFF);
            gradA(rx, ry, rw, rowH, 6, en);
            border(rx, ry, rw, rowH, 6, 1f, mix(mix(CARD_OFF_BORDER, 0xFF4779B4, hv), 0x38FFFFFF, en));
            if (hv > 0.01f) fill(rx, ry, rw, rowH, 6, ((int) (0x0C * hv * (1f - en)) << 24) | 0xFFFFFF);
            ico(Icons.CONFIGS, rx + 8, ry + rowH / 2f - 6, 12, WHITE);
            tb(fitEnd(name, 9.5f, rw - 34), rx + 26, ty(ry + rowH / 2f, 9.5f), 9.5f, WHITE);
            m = saved;
            if (live && vhh > 0) {
                hits.add(new Hit(rx, vy, rw, vhh,
                        () -> { ConfigsBridge.selected = name; if (dblClick) ConfigsBridge.loadSelected(); },
                        () -> ConfigsBridge.selected = name));
            }
        }
        alpha = base;
        endScissor();

        String status = ConfigsBridge.status();
        if (!status.isEmpty()) {
            int kind = ConfigsBridge.statusKind();
            int col = kind == ConfigsBridge.OK ? 0xFF72DCA4 : kind == ConfigsBridge.ERROR ? 0xFFFF7C8C : ACCENT_TXT;
            tb(fitEnd(status, 8f, iw), ix, ty(y + h - 78, 8), 8, col);
        }
        boolean hasSel = ConfigsBridge.selected != null;
        float bw = (iw - 6) / 2f;
        cfgButton("Загрузить", ix, y + h - 62, bw, 22, hasSel, false, ConfigsBridge::loadSelected);
        cfgButton("Удалить", ix + bw + 6, y + h - 62, bw, 22, hasSel, false, ConfigsBridge::deleteSelected);
        cfgButton("Открыть папку", ix, y + h - 34, iw, 22, true, false, ConfigsBridge::openFolder);
    }

    // =========================================================== screen lifecycle

    @Override
    public void onClose() { closing = true; drag = null; dragWin = null; dragSlider = null; cfgFocus = false; }

    @Override
    public void tick() {
        super.tick();
        if (closing && openT <= 0f) super.onClose();
        else if (!closing && !GuiStyle.RADIAL.equals(GuiStyle.current())) {
            // The Type setting was switched to another style.
            if (minecraft != null) minecraft.setScreen(GuiStyle.create());
        }
    }

    @Override
    public void render(GuiGraphics c, int mx, int my, float delta) {
        ctx = c;
        Modules.refreshFromTarget();
        daamky.render.Gfx.ctx = c;
        MsdfText.msdfMode = Modules.useMsdf();
        MsdfText.baseWeight = Modules.weight();
        hits.clear();

        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        time += dt;
        if (!closing) age += dt;

        openT = clamp01(openT + (closing ? -dt / CLOSE_TIME : dt / OPEN_TIME));
        float k = closing ? openT * openT : easeOutBack(openT, BOUNCE);
        ga = clamp01(openT * 2.5f);

        if (daamky.render.Theme.blur) super.render(c, mx, my, delta);
        else c.fill(0, 0, width, height, ((int) (0x70 * ga) << 24));
        if (ga <= 0.003f) return;

        float guiScale = (float) minecraft.getWindow().getGuiScale();
        float fit = Math.min((height - 10f) / H, (width - 10f) / W);
        S = Math.min(REF_GUI_SCALE / guiScale, fit);
        ox = (width - W * S) / 2f;
        oy = (height - H * S) / 2f;
        curOx = ox; curOy = oy; curS = S;
        mlx = (mx - ox) / S;
        mly = (my - oy) / S;
        vx0 = -ox / S; vx1 = (width - ox) / S;
        vy0 = -oy / S; vy1 = (height - oy) / S;
        alpha = ga;

        var ms = c.pose();
        ms.pushMatrix();
        ms.translate(ox, oy);
        ms.scale(S, S);
        Matrix3x2f mainM = new Matrix3x2f(ms);
        m = mainM;

        updateCover(-1);
        drawRing(k);
        for (Win w : new ArrayList<>(wins)) {
            m = mainM; alpha = ga; curOx = ox; curOy = oy; curS = S;
            drawWin(w);
        }
        for (Panel p : new ArrayList<>(panels)) {
            m = mainM; alpha = ga; curOx = ox; curOy = oy; curS = S;
            drawPanel(p);
        }
        m = mainM; alpha = ga; curOx = ox; curOy = oy; curS = S;

        ms.popMatrix();
    }

    // =========================================================== input

    private float cx, cy;

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean dbl) {
        double mx = click.x(), my = click.y(); int button = click.button();
        if (closing) return true;
        if (bindingSetting != null) {
            bindingSetting.set(KeyBind.mouse(button));
            bindingSetting = null;
            return true;
        }
        if (binding != null) {
            binding.setBind(KeyBind.mouse(button));
            binding = null;
            return true;
        }
        cx = (float) ((mx - ox) / S);
        cy = (float) ((my - oy) / S);
        cfgFocus = false;
        dblClick = dbl;
        ddKeep = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (cx >= h.x && cx < h.x + h.w && cy >= h.y && cy < h.y + h.h) {
                Runnable r = button == 1 ? h.right : button == 2 ? h.mid : h.left;
                if (r != null) r.run();
                if (!ddKeep) for (Panel pn : panels) for (Setting st : pn.mod.flat()) if (st instanceof Mode mm && mm.open) mm.open = false;
                return true;
            }
        }
        int sec = sectorAt(cx, cy);
        if (sec >= 0 && button == 0) { clickSector(sec); return true; }
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double dx, double dy) {
        double mx = click.x(), my = click.y();
        if (dragSlider != null) {
            float lx = (float) ((mx - ox) / S);
            dragSlider.set(dragSlider.min + clamp01((lx - dragSliderX) / dragSliderW) * (dragSlider.max - dragSlider.min));
            return true;
        }
        if (drag != null) {
            drag.x = (float) ((mx - ox) / S) - dragX;
            drag.y = (float) ((my - oy) / S) - dragY;
            return true;
        }
        if (dragWin != null) {
            dragWin.x = (float) ((mx - ox) / S) - dragWX;
            dragWin.y = (float) ((my - oy) / S) - dragWY;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        drag = null;
        dragWin = null;
        dragSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        float lx = (float) ((mx - ox) / S), ly = (float) ((my - oy) / S);
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (!p.closing && lx >= p.x && lx < p.x + PANEL_W && ly >= p.y && ly < p.y + p.height()) {
                p.scrollT = Math.max(0, Math.min(p.maxScroll, p.scrollT - (float) vAmount * 28f));
                return true;
            }
        }
        for (int i = wins.size() - 1; i >= 0; i--) {
            Win w = wins.get(i);
            if (!w.closing && lx >= w.x && lx < w.x + WIN_W && ly >= w.y && ly < w.y + w.height()) {
                w.scrollT = Math.max(0, Math.min(w.maxScroll, w.scrollT - (float) vAmount * 28f));
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        String chr = input.codepointAsString();
        if ((editingText != null || editingColor != null) && !chr.isEmpty() && chr.codePointAt(0) >= 32) {
            if (editBuffer.length() < 160) editBuffer += chr;
            applyEditBuffer();
            return true;
        }
        if (cfgFocus && !chr.isEmpty() && chr.codePointAt(0) >= 32) {
            if (ConfigsBridge.name.length() < 32) ConfigsBridge.name += chr;
            return true;
        }
        return super.charTyped(input);
    }

    private void applyEditBuffer() {
        if (editingText != null) editingText.set(editBuffer);
        if (editingColor != null) {
            java.awt.Color parsed = parseColor(editBuffer);
            if (parsed != null) editingColor.set(parsed);
        }
    }

    private void finishEdit() {
        applyEditBuffer();
        editingText = null; editingColor = null; editBuffer = "";
    }

    private static java.awt.Color parseColor(String text) {
        if (text == null) return null;
        String value = text.trim();
        if (value.startsWith("#")) value = value.substring(1);
        if (value.startsWith("0x") || value.startsWith("0X")) value = value.substring(2);
        try {
            long number = Long.parseLong(value, 16);
            if (value.length() == 6) return new java.awt.Color((int) number);
            if (value.length() == 8) return new java.awt.Color((int) number, true);
        } catch (NumberFormatException ignored) {}
        return null;
    }

    private static String keyLabel(int key) {
        try { return com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString(); }
        catch (Exception e) { return "?"; }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (bindingSetting != null) {
            bindingSetting.set((keyCode == 256 || keyCode == 259 || keyCode == 261)
                    ? KeyBind.NONE : KeyBind.keyboard(keyCode));
            bindingSetting = null;
            return true;
        }
        if (binding != null) {
            binding.setBind((keyCode == 256 || keyCode == 259 || keyCode == 261)
                    ? KeyBind.NONE : KeyBind.keyboard(keyCode));
            binding = null;
            return true;
        }
        if (editingText != null || editingColor != null) {
            if (keyCode == 257 || keyCode == 335) { finishEdit(); return true; }
            if (keyCode == 256) { editingText = null; editingColor = null; editBuffer = ""; return true; }
            if ((keyCode == 259 || keyCode == 261) && !editBuffer.isEmpty()) {
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1); applyEditBuffer(); return true;
            }
            return true;
        }
        if (cfgFocus) {
            if (keyCode == 257 || keyCode == 335) { ConfigsBridge.save(); return true; }
            if (keyCode == 256) { cfgFocus = false; return true; }
            if ((keyCode == 259 || keyCode == 261) && !ConfigsBridge.name.isEmpty()) {
                ConfigsBridge.name = ConfigsBridge.name.substring(0, ConfigsBridge.name.length() - 1);
            }
            return true;
        }
        return super.keyPressed(input);
    }
}
