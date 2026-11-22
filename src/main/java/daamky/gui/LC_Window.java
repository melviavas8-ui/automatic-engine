package daamky.gui;

import daamky.gui.Modules.Bool;
import daamky.gui.Modules.BindValue;
import daamky.gui.Modules.Button;
import daamky.gui.Modules.ColorValue;
import daamky.gui.Modules.Group;
import daamky.gui.Modules.Mod;
import daamky.gui.Modules.Mode;
import daamky.gui.Modules.Multi;
import daamky.gui.Modules.Section;
import daamky.gui.Modules.Setting;
import daamky.gui.Modules.Slider;
import daamky.gui.Modules.TextValue;
import daamky.render.Icons;
import daamky.render.MsdfText;
import daamky.render.SdfRect;
import daamky.render.Txt;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Liberty City "Window" ClickGUI: sidebar with categories, header with search and a two-column
 * grid of module cards whose settings sit inside the cards. Built on the same Modules adapter and
 * render stack as the legacy window, so every setting type, bind and config works the same way.
 * Colors come from the active Liberty City theme (Gray / Red / Blue).
 */
public class LC_Window extends Screen {

    static final float W = 700, H = 440, SW = 150, HEAD_H = 46, PAD = 14, GAP = 12;
    static final float CLIP_T = HEAD_H + 4, CLIP_B = H - 10;
    static final float CARD_HEAD = 38, LABEL_W = 150, OPT_H = 16, SLIDER_H = 33;
    /** Target Minecraft GUI scale the design was made for, keeps the physical size equal at any GUI Scale. */
    static final float REF_GUI_SCALE = 2f;
    static final float OPEN_TIME = 0.5f, CLOSE_TIME = 0.2f, BOUNCE = 2.0f;
    static final int WHITE = 0xFFFFFFFF;
    static final int CAT_CONFIGS = 5;

    // Palette, refreshed every frame from the active theme.
    private int win, side, card, cardHov, border, sep, dim, accent, accTxt, inBg, inBorder, togOff, knob,
            trackBg, sepC, popBg, valTxt, optTxt;
    /** Width of the rows currently being drawn (card width). */
    private float rowPW = 255;

    private record Nav(String label, int icon, int cat) {}
    private static final Nav[] NAV_MAIN = {
            new Nav("Combat", Icons.COMBAT, 0), new Nav("Movement", Icons.MOVEMENT, 1),
            new Nav("Render", Icons.RENDER, 2), new Nav("Player", Icons.PLAYER, 3), new Nav("Util", Icons.UTIL, 4)};
    private static final Nav[] NAV_SYS = {
            new Nav("Configs", Icons.CONFIGS, CAT_CONFIGS), new Nav("Display", Icons.THEMES, 6)};
    private static final Nav[] NAV_EXTRA = {new Nav("AutoBuy", Icons.ITEMS, 7)};

    private record Hit(float x, float y, float w, float h, Runnable left, Runnable right, Runnable mid) {
        Hit(float x, float y, float w, float h, Runnable left, Runnable right) { this(x, y, w, h, left, right, null); }
    }
    private record K(Object o, int kind) {}
    private static final int EN = 0, HOV = 1, TOG = 2, DD = 3, SEL = 4, SL = 5, POP = 6, NAVK = 7, EXPK = 8;
    private record Popup(Mode md, float bx, float by, float bw, float bh, float vt, float vb) {}

    private final List<Hit> hits = new ArrayList<>();
    private final Map<K, Float> anim = new HashMap<>();
    private final Map<Mod, Boolean> expanded = new HashMap<>();
    private Popup popup;
    private boolean ddKeep;
    private Mod binding;
    private BindValue bindingSetting;
    private TextValue editingText;
    private ColorValue editingColor;
    private String editBuffer = "";
    private Slider dragSlider;
    private float dragSliderX, dragSliderW;

    private int cat = 0;
    private float scroll, scrollT, maxScroll;
    private String query = "";
    private boolean searchFocus, cfgFocus, dblClick;
    private float ox, oy, S = 1f, mlx, mly, cx, cy;
    private float curOx, curOy, curS = 1f;
    private Matrix3x2f m = new Matrix3x2f();
    private DrawContext ctx;
    private long lastNs;
    private float dt, time, openT, catT0 = 0.05f, alpha = 1f, ga = 1f;
    private boolean closing;

    public LC_Window() { super(Text.literal("Daamky")); }

    @Override public boolean shouldPause() { return false; }

    // ---------------------------------------------------------------- math / helpers

    static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    static float easeOutBack(float t, float s) { t = clamp01(t); float u = t - 1; return 1 + (s + 1) * u * u * u + s * u * u; }
    static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    static int mix(int a, int b, float t) {
        int r = 0;
        for (int sh = 24; sh >= 0; sh -= 8) {
            int ca = (a >>> sh) & 0xFF, cb = (b >>> sh) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * t) & 0xFF) << sh;
        }
        return r;
    }

    private static int rgb(java.awt.Color c) { return 0xFF000000 | (c.getRGB() & 0xFFFFFF); }

    private void updatePalette() {
        ClientTheme t = ThemeManager.current();
        win = rgb(t.surfaceDeep());
        side = rgb(t.surfaceDark());
        card = rgb(t.surface());
        cardHov = rgb(t.surfaceBright());
        accent = rgb(t.accent());
        accTxt = rgb(t.accentBright());
        border = 0x30FFFFFF;
        sep = 0x18FFFFFF;
        sepC = 0x26FFFFFF;
        dim = 0xFF8D939C;
        valTxt = 0xFFC9CED6;
        optTxt = 0xFFA9AEB6;
        inBg = mix(win, WHITE, 0.05f);
        inBorder = 0x34FFFFFF;
        togOff = mix(card, WHITE, 0.16f);
        knob = 0xFFF2F4F7;
        trackBg = mix(card, 0xFF000000, 0.35f);
        popBg = mix(win, WHITE, 0.05f);
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
    /** Flat accent fill (the Liberty City look has no animated gradients). */
    private void grad(float x, float y, float w, float h, float r) { fill(x, y, w, h, r, accent); }
    private void gradA(float x, float y, float w, float h, float r, float a) {
        if (a > 0.003f) fill(x, y, w, h, r, (Math.round(255 * clamp01(a)) << 24) | (accent & 0xFFFFFF));
    }
    private void gglow(float x, float y, float w, float h, float r, float spread, int a) { }
    private void gglowA(float x, float y, float w, float h, float r, float spread, int a, float k) { }
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
        return !closing && mlx >= x && mlx < x + w && mly >= y && mly < y + h;
    }
    private boolean hovClip(float x, float y, float w, float h, float vt, float vb) {
        float y0 = Math.max(y, vt), y1 = Math.min(y + h, vb);
        return y1 > y0 && hov(x, y0, w, y1 - y0);
    }
    private void hitClip(float x, float y, float w, float h, float vt, float vb, Runnable l, Runnable r) {
        float y0 = Math.max(y, vt), y1 = Math.min(y + h, vb);
        if (y1 > y0) hit(x, y0, w, y1 - y0, l, r);
    }
    private void scissor(float x1, float y1, float x2, float y2) {
        int l = (int) Math.floor(curOx + x1 * curS), tp = (int) Math.floor(curOy + y1 * curS);
        int r = (int) Math.ceil(curOx + x2 * curS), bt = (int) Math.ceil(curOy + y2 * curS);
        daamky.render.Gfx.scissor = new net.minecraft.client.gui.ScreenRect(l, tp, Math.max(0, r - l), Math.max(0, bt - tp));
    }
    private void endScissor() { daamky.render.Gfx.scissor = null; }

    // ---------------------------------------------------------------- data

    private List<Mod> visible() {
        List<Mod> out = new ArrayList<>();
        String q = query.toLowerCase();
        for (Mod mod : Modules.ALL) {
            if (q.isEmpty() ? mod.category == cat : mod.name.toLowerCase().contains(q)) out.add(mod);
        }
        return out;
    }

    private boolean isExpanded(Mod mod) { return expanded.getOrDefault(mod, Boolean.TRUE); }

    private float contentH(Mod mod) {
        float h = 0;
        for (Setting s : Modules.rows(mod)) h += rowH(s);
        return h;
    }

    private float cardHeight(Mod mod) {
        if (!mod.hasSettings()) return CARD_HEAD;
        float e = ap(mod, EXPK, isExpanded(mod) ? 1f : 0f, 14f);
        return CARD_HEAD + e * (contentH(mod) + 8);
    }

    private void selectCat(int target) {
        if (cat == target && query.isEmpty()) return;
        cat = target; query = ""; scroll = scrollT = 0; catT0 = time;
        if (target == CAT_CONFIGS) ConfigsBridge.refresh();
    }

    private String title() {
        if (!query.isEmpty()) return "Поиск";
        for (Nav[] group : new Nav[][]{NAV_MAIN, NAV_SYS, NAV_EXTRA}) for (Nav n : group) if (n.cat == cat) return n.label;
        return "Daamky";
    }

    // ---------------------------------------------------------------- screen lifecycle

    @Override
    public void close() { closing = true; dragSlider = null; }

    @Override
    public void tick() {
        super.tick();
        if (closing && openT <= 0f) super.close();
        else if (!closing && GuiStyle.isDropDown()) {
            // The Type setting was switched to DropDown.
            if (client != null) client.setScreen(GuiStyle.create());
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        ctx = c;
        Modules.refreshFromTarget();
        updatePalette();
        daamky.render.Gfx.ctx = c;
        MsdfText.msdfMode = Modules.useMsdf();
        MsdfText.baseWeight = Modules.weight();
        hits.clear();
        popup = null;

        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        time += dt;

        openT = clamp01(openT + (closing ? -dt / CLOSE_TIME : dt / OPEN_TIME));
        float k = closing ? openT * openT : easeOutBack(openT, BOUNCE);
        ga = clamp01(openT * 2.5f);

        if (daamky.render.Theme.blur) super.render(c, mx, my, delta);
        else c.fill(0, 0, width, height, ((int) (0x70 * ga) << 24));
        if (ga <= 0.003f) return;

        float guiScale = (float) client.getWindow().getScaleFactor();
        float fit = Math.min((height - 10f) / H, (width - 10f) / W);
        float base = Math.min(REF_GUI_SCALE / guiScale, fit);
        S = Math.min(base * (0.7f + 0.3f * k), fit);
        float rise = (1f - Math.min(k, 1f)) * 22f * S;
        ox = (width - W * S) / 2f;
        oy = (height - H * S) / 2f + rise;
        curOx = ox; curOy = oy; curS = S;
        mlx = (mx - ox) / S;
        mly = (my - oy) / S;
        alpha = ga;

        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.translate(ox, oy);
        ms.scale(S, S);
        m = new Matrix3x2f(ms);

        drawWindow();
        drawSidebar();
        if (cat == CAT_CONFIGS && query.isEmpty()) drawConfigs();
        else drawCards();
        drawHeader();
        if (popup != null) drawPopup(popup);

        ms.popMatrix();
    }

    // ---------------------------------------------------------------- frame

    private void drawWindow() {
        fill(0, 0, W, H, 8, win);
        border(0, 0, W, H, 8, 1f, border);
        hit(0, 0, W, H, null, null);
    }

    private void drawSidebar() {
        fill(0, 0, SW, H, 8, side);
        fill(SW - 8, 0, 8, H, 0, side);
        fill(SW, 0, 1, H, 0, sep);
        fill(0, 0, SW, 2, 0, accent);

        tb("<_", 14, ty(27, 17), 17, accTxt);
        tb("daamky.", 14 + Txt.wBold("<_ ", 17), ty(27, 17), 17, WHITE);
        tr("1.5 RELEASE", 15, ty(43, 6.5f), 6.5f, dim);

        float y = 62;
        tr("МОДУЛИ", 14, ty(y, 7), 7, dim); y += 11;
        for (Nav n : NAV_MAIN) { drawNav(n, y); y += 27; }
        y += 8;
        tr("СИСТЕМА", 14, ty(y, 7), 7, dim); y += 11;
        for (Nav n : NAV_SYS) { drawNav(n, y); y += 27; }
        y += 8;
        tr("ДОПОЛНИТЕЛЬНО", 14, ty(y, 7), 7, dim); y += 11;
        for (Nav n : NAV_EXTRA) { drawNav(n, y); y += 27; }

        float by = H - 38, bh = 24;
        float hv = ap("exit", HOV, hov(10, by, SW - 20, bh) ? 1f : 0f, 16f);
        fill(10, by, SW - 20, bh, 6, mix(inBg, 0xFFFFFFFF, 0.06f * hv));
        border(10, by, SW - 20, bh, 6, 1f, mix(inBorder, 0x55FFFFFF, hv));
        ico(Icons.POWER, 19, by + 6, 12, mix(dim, WHITE, hv));
        tb("Закрыть", 37, ty(by + bh / 2f, 9), 9, mix(valTxt, WHITE, hv));
        hit(10, by, SW - 20, bh, this::close, null);
    }

    private void drawNav(Nav n, float top) {
        float h = 24, x = 8, w = SW - 16;
        boolean sel = n.cat == cat && query.isEmpty();
        float t = lin(n, NAVK, sel ? 1f : 0f, 0.2f);
        float hv = ap(n, HOV, hov(x, top, w, h) ? 1f : 0f, 16f);
        if (hv > 0.01f) fill(x, top, w, h, 6, ((int) (0x14 * hv) << 24) | 0xFFFFFF);
        if (t > 0.01f) {
            fill(x, top, w, h, 6, (Math.round(0x2A * t) << 24) | (accent & 0xFFFFFF));
            fill(x, top + 5, 2.5f, h - 10, 1.25f, (Math.round(255 * t) << 24) | (accTxt & 0xFFFFFF));
        }
        int tc = mix(valTxt, WHITE, Math.max(t, hv * 0.6f));
        ico(n.icon, x + 10, top + h / 2f - 6.5f, 13, mix(dim, accTxt, t));
        tb(n.label, x + 30, ty(top + h / 2f, 10), 10, tc);
        final int target = n.cat;
        hit(x, top, w, h, () -> selectCat(target), null);
    }

    private void drawHeader() {
        float x0 = SW + PAD;
        tb(title(), x0, ty(HEAD_H / 2f, 15), 15, WHITE);
        if (query.isEmpty() && cat != CAT_CONFIGS) {
            String count = visible().size() + " модулей";
            tr(count, x0 + Txt.wBold(title(), 15) + 8, ty(HEAD_H / 2f + 1, 8), 8, dim);
        }

        float sw = 190, sh = 24, sx = W - PAD - sw, sy = (HEAD_H - sh) / 2f;
        float fo = ap(this, EN, searchFocus ? 1f : 0f, 14f);
        fill(sx, sy, sw, sh, 6, inBg);
        border(sx, sy, sw, sh, 6, 1f, mix(inBorder, accTxt, fo));
        ico(Icons.SEARCH, sx + 8, sy + 6, 11, mix(dim, WHITE, fo));
        String shown = query + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        if (query.isEmpty() && !searchFocus) tr("Поиск", sx + 24, ty(sy + sh / 2f, 9.5f), 9.5f, dim);
        else tr(shown, sx + 24, ty(sy + sh / 2f, 9.5f), 9.5f, WHITE);
        hit(sx, sy, sw, sh, () -> searchFocus = true, null);

        fill(SW + 1, HEAD_H, W - SW - 1, 1, 0, sep);
    }

    // ---------------------------------------------------------------- module cards

    private void drawCards() {
        List<Mod> list = visible();
        float colW = (W - SW - PAD * 2 - GAP) / 2f;
        float c1 = SW + PAD, c2 = c1 + colW + GAP;
        float[] colY = {0, 0};
        float[][] pos = new float[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            float ch = cardHeight(list.get(i));
            int col = colY[1] < colY[0] ? 1 : 0;
            pos[i] = new float[]{col == 0 ? c1 : c2, colY[col], ch};
            colY[col] += ch + GAP;
        }
        float total = Math.max(colY[0], colY[1]);
        float viewH = CLIP_B - CLIP_T;
        maxScroll = Math.max(0, total - viewH + 4);
        scrollT = Math.max(0, Math.min(maxScroll, scrollT));
        scroll = Math.max(0, Math.min(maxScroll, lerpTo(scroll, scrollT, 18f)));

        scissor(SW + 1, CLIP_T, W - 2, CLIP_B);
        float base = alpha;
        for (int i = 0; i < list.size(); i++) {
            float y = CLIP_T + 4 + pos[i][1] - scroll;
            if (y + pos[i][2] < CLIP_T || y > CLIP_B) continue;
            drawCard(list.get(i), i, pos[i][0], y, colW, pos[i][2]);
        }
        alpha = base;
        endScissor();

        if (list.isEmpty()) {
            String s = query.isEmpty() ? "В этой категории пока пусто" : "Ничего не найдено";
            float tw = Txt.wBold(s, 11);
            tb(s, SW + (W - SW - tw) / 2f, ty(HEAD_H + 80, 11), 11, dim);
        }

        if (maxScroll > 0) {
            float trackT = CLIP_T + 4, trackH = viewH - 8;
            float th = Math.max(30, trackH * viewH / (total + 4)), tyy = trackT + (trackH - th) * (scroll / maxScroll);
            fill(W - 8, trackT, 3, trackH, 1.5f, sep);
            fill(W - 8, tyy, 3, th, 1.5f, accent);
        }
    }

    private void drawCard(Mod mod, int index, float x, float y0, float w, float h) {
        float appear = clamp01((time - catT0 - index * 0.03f) / 0.32f);
        if (appear <= 0.003f) return;
        float dy = (1f - easeOutBack(appear, 1.8f)) * 16f;
        float y = y0 + dy;
        float savedAlpha = alpha;
        alpha = savedAlpha * clamp01(appear * 2.5f);
        float savedOy = curOy;
        Matrix3x2f saved = m;
        if (dy != 0f) {
            m = new Matrix3x2f(saved).translate(0, dy);
            curOy = savedOy + dy * curS;
        }

        float vt = Math.max(CLIP_T, y + CARD_HEAD), vb = Math.min(CLIP_B, y + h - 4);
        float hvT = Math.max(y, CLIP_T), hvH = Math.min(y + CARD_HEAD, CLIP_B) - hvT;
        float en = ap(mod, EN, mod.enabled ? 1f : 0f, 11f);
        float hv = ap(mod, HOV, hvH > 0 && hov(x, hvT, w, hvH) ? 1f : 0f, 16f);
        float pop = ap(mod, POP, 0f, 9f);

        fill(x, y, w, h, 7, mix(card, cardHov, hv * 0.6f));
        border(x, y, w, h, 7, 1f, mix(border, (0x80 << 24) | (accent & 0xFFFFFF), en));
        if (en > 0.01f) fill(x, y + 6, 2.5f, CARD_HEAD - 12, 1.25f, (Math.round(255 * en) << 24) | (accTxt & 0xFFFFFF));

        // Header: name, description, bind, expand arrow, switch.
        float tx = x + 12, right = x + w - 12;
        float tw = 26, th = 13, tgx = right - tw, tgy = y + (CARD_HEAD - th) / 2f;
        float t = ap(mod, TOG, mod.enabled ? 1f : 0f, 14f);
        float kn = th - 3, kw = kn + (float) Math.sin(Math.PI * t) * 3f;
        fill(tgx, tgy, tw, th, th / 2f, mix(togOff, accent, t));
        fill(tgx + 1.5f + t * (tw - 3f - kw), tgy + 1.5f, kw * (1f + 0.1f * pop), kn, kn / 2f, knob);

        float rx = tgx - 8;
        if (mod.hasSettings()) {
            float e = peek(mod, EXPK, isExpanded(mod) ? 1f : 0f);
            Matrix3x2f s2 = m;
            float ax = rx - 5, ay = y + CARD_HEAD / 2f;
            m = new Matrix3x2f(s2).translate(ax, ay).rotate((float) Math.PI * e).translate(-ax, -ay);
            ico(Icons.CHEVRON, ax - 3.5f, ay - 3.5f, 7, mix(dim, WHITE, hv));
            m = s2;
            rx -= 16;
        }
        if (binding == mod || mod.hasBind()) {
            String bt = binding == mod ? "[...]" : "[" + mod.keyDisplay() + "]";
            float bw = Txt.wBold(bt, 8);
            tb(bt, rx - bw, ty(y + CARD_HEAD / 2f, 8), 8, binding == mod ? accTxt : valTxt);
            rx -= bw + 8;
        }
        tb(mod.name, tx, y + 7, 11.5f, WHITE);
        String desc = clipRight(mod.desc, Math.max(40, rx - tx - 4), 7.5f);
        tr(desc, tx, y + 22, 7.5f, mix(dim, valTxt, en));

        // Header hits first, so row hits registered later win.
        if (hvH > 0 && !closing) {
            hits.add(new Hit(x, hvT, w, hvH,
                    () -> { mod.setEnabled(!mod.enabled); anim.put(new K(mod, POP), 1f); },
                    mod.hasSettings() ? () -> expanded.put(mod, !isExpanded(mod)) : null,
                    () -> binding = mod));
            if (mod.hasSettings()) hitClip(rx - 2, y, tgx - 8 - (rx - 2), CARD_HEAD, CLIP_T, CLIP_B,
                    () -> expanded.put(mod, !isExpanded(mod)), null);
        }

        // Inline settings.
        if (mod.hasSettings() && h > CARD_HEAD + 2) {
            fill(x + 10, y + CARD_HEAD - 1, w - 20, 1, 0, sep);
            rowPW = w;
            float sy0 = Math.max(y + CARD_HEAD, CLIP_T), sy1 = Math.min(y + h - 2, CLIP_B);
            if (sy1 > sy0) {
                scissor(x + 1, sy0, x + w - 1, sy1);
                float ry = y + CARD_HEAD + 2;
                for (Setting s : Modules.rows(mod)) {
                    float rh = rowH(s);
                    if (ry + rh >= vt && ry <= vb) drawSetting(mod, s, x, ry, rh, vt, vb);
                    ry += rh;
                }
                scissor(SW + 1, CLIP_T, W - 2, CLIP_B);
            }
        }

        m = saved;
        curOy = savedOy;
        alpha = savedAlpha;
    }

    private String clipRight(String value, float maxWidth, float size) {
        String result = value == null ? "" : value;
        if (Txt.wReg(result, size) <= maxWidth) return result;
        while (result.length() > 1 && Txt.wReg(result + "…", size) > maxWidth) result = result.substring(0, result.length() - 1);
        return result + "…";
    }

    private void drawSetting(Mod pm, Setting s, float x, float ry, float rh, float vt, float vb) {
        float right = x + rowPW - 14;
        if (s instanceof Section sec) {
            float cy2 = ry + rh / 2f, w = Txt.wBold(sec.name, 9.5f), cx2 = x + (rowPW - w) / 2f;
            tb(sec.name, cx2, ty(cy2, 9.5f), 9.5f, accTxt);
            fill(x + 8, cy2, cx2 - 7 - (x + 8), 1, 0, sepC);
            fill(cx2 + w + 7, cy2, right - (cx2 + w + 7), 1, 0, sepC);
        } else if (s instanceof Group g) {
            drawGroup(g, x, ry, rh, right, vt, vb);
        } else if (s instanceof Button btn) {
            drawButton(btn, x, ry, right, vt, vb);
        } else if (s instanceof Bool b) {
            drawBool(b, x, ry, rh, right, vt, vb);
        } else if (s instanceof Slider sl) {
            drawSlider(sl, x, ry, right, vt, vb);
        } else if (s instanceof Mode md) {
            drawMode(pm, md, x, ry, right, vt, vb);
        } else if (s instanceof TextValue text) {
            drawTextValue(text, x, ry, right, vt, vb);
        } else if (s instanceof ColorValue color) {
            drawColorValue(color, x, ry, right, vt, vb);
        } else if (s instanceof BindValue bind) {
            drawBindValue(bind, x, ry, right, vt, vb);
        }
    }

    // ---------------------------------------------------------------- configs page

    private void drawConfigs() {
        List<String> list = ConfigsBridge.list();
        float colW = (W - SW - PAD * 2 - GAP) / 2f;
        float c1 = SW + PAD, c2 = c1 + colW + GAP;
        float pitch = 40f, cardH = 34f;
        float total = list.size() * pitch + 6, viewH = CLIP_B - CLIP_T;
        maxScroll = Math.max(0, total - viewH);
        scrollT = Math.max(0, Math.min(maxScroll, scrollT));
        scroll = Math.max(0, Math.min(maxScroll, lerpTo(scroll, scrollT, 18f)));

        scissor(SW + 1, CLIP_T, c2 - 4, CLIP_B);
        if (list.isEmpty()) {
            String s = "Конфигов пока нет";
            tb(s, c1 + (colW - Txt.wBold(s, 11)) / 2f, ty(CLIP_T + 70, 11), 11, WHITE);
            float yy = CLIP_T + 88;
            for (String line : wrap("Введи имя справа и нажми «Сохранить», чтобы создать первый конфиг.", 8, colW - 20)) {
                tb(line, c1 + (colW - Txt.wBold(line, 8)) / 2f, ty(yy, 8), 8, dim);
                yy += 11;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            float y = CLIP_T + 4 + i * pitch - scroll;
            if (y + cardH < CLIP_T || y > CLIP_B) continue;
            drawConfigCard(list.get(i), c1, y, colW, cardH);
        }
        endScissor();
        drawConfigPanel(c2, CLIP_T + 4, colW, CLIP_B - CLIP_T - 4);
    }

    private void drawConfigCard(String name, float x, float y, float w, float h) {
        boolean sel = name.equals(ConfigsBridge.selected);
        float vy = Math.max(y, CLIP_T), vh = Math.min(y + h, CLIP_B) - vy;
        float en = ap(name, EN, sel ? 1f : 0f, 11f);
        float hv = ap(name, HOV, vh > 0 && hov(x, vy, w, vh) ? 1f : 0f, 16f);
        fill(x, y, w, h, 7, mix(card, cardHov, hv * 0.6f));
        border(x, y, w, h, 7, 1f, mix(border, (0x80 << 24) | (accent & 0xFFFFFF), en));
        if (en > 0.01f) fill(x, y + 6, 2.5f, h - 12, 1.25f, (Math.round(255 * en) << 24) | (accTxt & 0xFFFFFF));
        ico(Icons.CONFIGS, x + 12, y + h / 2f - 7, 14, mix(dim, WHITE, Math.max(en, hv)));
        tb(name, x + 34, y + 6.5f, 11, WHITE);
        tr(sel ? "Выбран · двойной клик — загрузить" : "Нажми, чтобы выбрать", x + 34, y + 21, 7.5f, sel ? accTxt : dim);
        if (vh > 0) hit(x, vy, w, vh, () -> {
            ConfigsBridge.selected = name;
            if (dblClick) ConfigsBridge.loadSelected();
        }, () -> ConfigsBridge.selected = name);
    }

    private void cfgButton(String label, float x, float y, float w, float h, boolean enabled, boolean primary, Runnable action) {
        String key = "cfgbtn:" + label;
        float hv = ap(key, HOV, enabled && hov(x, y, w, h) ? 1f : 0f, 16f);
        float pr = ap(key, POP, 0f, 10f);
        fill(x, y, w, h, 6, inBg);
        if (enabled) gradA(x, y, w, h, 6, primary ? 0.75f + 0.25f * hv : 0.12f + 0.2f * hv + 0.3f * pr);
        border(x, y, w, h, 6, 1f, mix(inBorder, 0x55FFFFFF, hv));
        float tw = Txt.wBold(label, 9);
        tb(label, x + (w - tw) / 2f, ty(y + h / 2f, 9), 9, enabled ? WHITE : 0xFF5E6672);
        if (enabled) hit(x, y, w, h, () -> { action.run(); anim.put(new K(key, POP), 1f); }, null);
    }

    private void drawConfigPanel(float x, float y, float w, float h) {
        float ix = x + 12, iw = w - 24;
        fill(x, y, w, h, 7, card);
        border(x, y, w, h, 7, 1f, border);
        fill(x, y + 30, w, 1, 0, sep);
        ico(Icons.CONFIGS, x + 12, y + 8, 13, accTxt);
        tb("КОНФИГИ", x + 32, ty(y + 15, 10), 10, WHITE);

        tb("Имя конфига", ix, ty(y + 46, 8), 8, dim);
        float fy = y + 54, fh = 22;
        float fo = ap("cfgfield", EN, cfgFocus ? 1f : 0f, 14f);
        fill(ix, fy, iw, fh, 6, inBg);
        border(ix, fy, iw, fh, 6, 1f, mix(inBorder, accTxt, fo));
        String typed = ConfigsBridge.name;
        if (typed.isEmpty() && !cfgFocus) {
            tr("Например: legit", ix + 9, ty(fy + fh / 2f, 9), 9, dim);
        } else {
            String shown = typed + (cfgFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            while (!shown.isEmpty() && Txt.wBold(shown, 9) > iw - 18) shown = shown.substring(1);
            tb(shown, ix + 9, ty(fy + fh / 2f, 9), 9, WHITE);
        }
        hit(ix, fy, iw, fh, () -> cfgFocus = true, null);

        boolean hasSel = ConfigsBridge.selected != null;
        float bw = (iw - 6) / 2f;
        cfgButton("Сохранить", ix, y + 86, iw, 22, true, true, ConfigsBridge::save);
        cfgButton("Загрузить", ix, y + 114, bw, 22, hasSel, false, ConfigsBridge::loadSelected);
        cfgButton("Удалить", ix + bw + 6, y + 114, bw, 22, hasSel, false, ConfigsBridge::deleteSelected);
        cfgButton("Открыть папку", ix, y + 142, iw, 22, true, false, ConfigsBridge::openFolder);

        String status = ConfigsBridge.status();
        if (!status.isEmpty()) {
            int kind = ConfigsBridge.statusKind();
            int col = kind == ConfigsBridge.OK ? 0xFF72DCA4 : kind == ConfigsBridge.ERROR ? 0xFFFF7C8C : accTxt;
            float sy = y + 176;
            for (String line : wrap(status, 8, iw)) {
                tb(line, ix, ty(sy, 8), 8, col);
                sy += 11;
            }
        }

        float by = y + h - 56;
        fill(ix, by - 8, iw, 1, 0, sep);
        tb("Выбран:", ix, ty(by + 4, 8), 8, dim);
        String sel = ConfigsBridge.selected == null ? "—" : ConfigsBridge.selected;
        tb(sel, ix + 44, ty(by + 4, 8), 8, ConfigsBridge.selected == null ? dim : WHITE);
        float hy = by + 20;
        for (String line : wrap("Пустое имя — перезапишет выбранный конфиг. Конфиги лежат в папке daamky/config.", 7.5f, iw)) {
            tr(line, ix, ty(hy, 7.5f), 7.5f, dim);
            hy += 10;
        }
    }

    // ---------------------------------------------------------------- setting rows (shared logic with the legacy window)

    private void drawTextValue(TextValue value, float x, float ry, float right, float vt, float vb) {
        tb(value.name, x + 8, ty(ry + 14.5f, 9.5f), 9.5f, WHITE);
        String shown = editingText == value ? editBuffer + "|" : value.text();
        if (shown.isEmpty()) shown = "—";
        float bw = 82, bh = 17, bx = right - bw, by = ry + 6;
        fill(bx, by, bw, bh, 6, inBg);
        border(bx, by, bw, bh, 6, 1f, editingText == value ? accTxt : inBorder);
        tb(clipText(shown, bw - 10, 8f), bx + 5, ty(by + bh / 2f, 8f), 8f,
                editingText == value ? accTxt : valTxt);
        hitClip(bx, ry, bw, 29, vt, vb, () -> {
            editingText = value; editingColor = null; editBuffer = value.text();
        }, null);
    }

    private void drawColorValue(ColorValue value, float x, float ry, float right, float vt, float vb) {
        tb(value.name, x + 8, ty(ry + 14.5f, 9.5f), 9.5f, WHITE);
        String shown = editingColor == value ? editBuffer + "|" : value.text();
        float bw = 82, bh = 17, bx = right - bw, by = ry + 6;
        fill(bx, by, bw, bh, 6, inBg);
        fill(bx + 3, by + 3, 10, bh - 6, 3, value.value.getRGB());
        border(bx, by, bw, bh, 6, 1f, editingColor == value ? accTxt : inBorder);
        tb(clipText(shown, bw - 18, 8f), bx + 16, ty(by + bh / 2f, 8f), 8f,
                editingColor == value ? accTxt : valTxt);
        hitClip(bx, ry, bw, 29, vt, vb, () -> {
            editingColor = value; editingText = null; editBuffer = value.text();
        }, null);
    }

    private void drawBindValue(BindValue value, float x, float ry, float right, float vt, float vb) {
        tb(value.name, x + 8, ty(ry + 14.5f, 9.5f), 9.5f, WHITE);
        boolean waiting = bindingSetting == value;
        String shown = waiting ? "PRESS KEY" : value.text();
        float bw = 82, bh = 17, bx = right - bw, by = ry + 6;
        fill(bx, by, bw, bh, 6, inBg);
        border(bx, by, bw, bh, 6, 1f, waiting ? accTxt : inBorder);
        float tw = Txt.wBold(shown, 8f);
        tb(shown, bx + (bw - tw) / 2f, ty(by + bh / 2f, 8f), 8f, waiting ? accTxt : valTxt);
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
        float cy = ry + rh / 2f, w = Txt.wBold(groupLabel, 9.5f), cx = x + (rowPW - w) / 2f;
        float hv = ap(g, HOV, hovClip(x + 4, ry, rowPW - 8, rh, vt, vb) ? 1f : 0f, 16f);
        tb(groupLabel, cx, ty(cy, 9.5f), 9.5f, mix(accTxt, WHITE, hv * 0.5f));
        fill(x + 8, cy, cx - 7 - (x + 8), 1, 0, sepC);
        fill(cx + w + 7, cy, right - 12 - (cx + w + 7), 1, 0, sepC);
        float t = ap(g, TOG, g.open ? 1f : 0f, 14f);
        Matrix3x2f saved = m;
        float ax = right - 7f, ay = cy;
        m = new Matrix3x2f(saved).translate(ax, ay).rotate((float) Math.PI * t).translate(-ax, -ay);
        ico(Icons.CHEVRON, ax - 3.25f, ay - 3.25f, 6.5f, WHITE);
        m = saved;
        hitClip(x + 4, ry, rowPW - 8, rh, vt, vb, () -> g.open = !g.open, null);
    }

    private void drawButton(Button b, float x, float ry, float right, float vt, float vb) {
        float bx = x + 8, bw = right - bx, bh = 17, by = ry + (25.5f - bh) / 2f;
        float hv = ap(b, HOV, hovClip(bx, by, bw, bh, vt, vb) ? 1f : 0f, 16f);
        float pr = ap(b, POP, 0f, 10f);
        fill(bx, by, bw, bh, 8, inBg);
        gradA(bx, by, bw, bh, 8, 0.35f + 0.4f * hv + 0.25f * pr);
        border(bx, by, bw, bh, 8, 1f, mix(inBorder, 0x38FFFFFF, hv));
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
        fill(tx, tyy, tw, th, 6, togOff);
        gradA(tx, tyy, tw, th, 6, t);
        fill(tx + 1.5f + t * (tw - 3f - kw), tyy + 1.5f, kw, kn, kn / 2f, knob);
        hitClip(x + 4, ry, rowPW - 8, rh, vt, vb, () -> b.set(!b.value), null);
    }

    private void drawSlider(Slider sl, float x, float ry, float right, float vt, float vb) {
        tb(sl.name, x + 8, ty(ry + 10, 9.5f), 9.5f, WHITE);
        String val = sl.text();
        tb(val, right - Txt.wBold(val, 8.5f), ty(ry + 10, 8.5f), 8.5f, accTxt);

        float trx = x + 8, trw = right - trx, trY = ry + 23;
        boolean dragging = dragSlider == sl;
        boolean over = hovClip(trx - 5, ry + 14, trw + 10, 18, vt, vb);
        float shown = ap(sl, SL, sl.frac(), dragging ? 40f : 16f);
        float hv = ap(sl, HOV, dragging || over ? 1f : 0f, 16f);

        fill(trx, trY - 2, trw, 4, 2, trackBg);
        float fw = Math.max(4f, trw * shown);
        grad(trx, trY - 2, fw, 4, 2);
        float kr = 4f + 1.5f * hv, kx = trx + trw * shown;
        gglowA(kx - kr, trY - kr, kr * 2, kr * 2, kr, 5, 0x60, hv);
        fill(kx - kr, trY - kr, kr * 2, kr * 2, kr, knob);

        final float fx = trx, fwid = trw;
        hitClip(trx - 5, ry + 14, trw + 10, 18, vt, vb, () -> {
            dragSlider = sl; dragSliderX = fx; dragSliderW = fwid;
            sl.set(sl.min + clamp01((cx - fx) / fwid) * (sl.max - sl.min));
        }, null);
    }

    private void drawMode(Mod pm, Mode md, float x, float ry, float right, float vt, float vb) {
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
            if (md.open) for (Setting o : pm.flat()) if (o instanceof Mode om && om != md) om.open = false;
        }, null);
    }

    private void drawPopup(Popup pp) {
        Mode md = pp.md;
        float dd = peek(md, DD, md.open ? 1f : 0f);
        int n = md.modes.length;
        float full = n * OPT_H + 4, listH = dd * full, h = pp.bh + listH;
        float bx = pp.bx, by = pp.by, bw = pp.bw, c0 = by + pp.bh / 2f;

        gglow(bx, by, bw, pp.bh, 7.5f, 5, 0x50);
        fill(bx, by, bw, h, 7.5f, popBg);
        border(bx, by, bw, h, 7.5f, 1f, inBorder);
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
            tb(md.modes[i], bx + 7, ty(oy0 + (OPT_H - 2) / 2f, 8.5f), 8.5f, sel ? WHITE : optTxt);
            alpha = base;
            if (md.open && dd > 0.6f) {
                final int idx = i;
                hit(bx + 3, oy0, bw - 6, OPT_H - 2, () -> { md.select(idx); md.open = false; }, null);
            }
        }
    }

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
    // ---------------------------------------------------------------- input

    private void closeDropdowns() {
        for (Mod mod : Modules.ALL) for (Setting st : mod.flat()) if (st instanceof Mode mm && mm.open) mm.open = false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
        double mx = click.x(), my = click.y();
        int button = click.button();
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
        searchFocus = false;
        cfgFocus = false;
        dblClick = dbl;
        ddKeep = false;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (cx >= h.x && cx < h.x + h.w && cy >= h.y && cy < h.y + h.h) {
                Runnable r = button == 1 ? h.right : button == 2 ? h.mid : h.left;
                if (r != null) r.run();
                if (!ddKeep) closeDropdowns();
                return true;
            }
        }
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragSlider != null) {
            float lx = (float) ((click.x() - ox) / S);
            dragSlider.set(dragSlider.min + clamp01((lx - dragSliderX) / dragSliderW) * (dragSlider.max - dragSlider.min));
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        scrollT = Math.max(0, Math.min(maxScroll, scrollT - (float) vAmount * 28f));
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        String chr = input.asString();
        if ((editingText != null || editingColor != null) && !chr.isEmpty() && chr.codePointAt(0) >= 32) {
            if (editBuffer.length() < 160) editBuffer += chr;
            applyEditBuffer();
            return true;
        }
        if (cfgFocus && !chr.isEmpty() && chr.codePointAt(0) >= 32) {
            if (ConfigsBridge.name.length() < 32) ConfigsBridge.name += chr;
            return true;
        }
        if (searchFocus && !chr.isEmpty() && chr.codePointAt(0) >= 32 && query.length() < 24) {
            query += chr;
            scroll = scrollT = 0;
            catT0 = time;
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

    @Override
    public boolean keyPressed(KeyInput input) {
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
        if (searchFocus && keyCode == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; }
        if (searchFocus && keyCode == 256) { searchFocus = false; return true; }
        return super.keyPressed(input);
    }
}
