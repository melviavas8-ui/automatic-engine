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
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;






public class DaamkyClickGui extends Screen {

    static final float W = 560, H = 410, SW = 132;
    static final float CARD_W = 184, CARD_H = 42, CARD_PITCH = 49, COL1 = 145, COL2 = 0;
    static final float CLIP_T = 44, CLIP_B = H - 8;
    static final float PANEL_W = 204, PANEL_MAX_H = 330, HEAD_H = 22, LABEL_W = 140;
    static final float OPT_H = 16, SLIDER_H = 33;
    static final int PANEL_BG = 0xFF080E18, TOG_OFF = 0xFF36506D, KNOB = 0xFFF5FAFF, THUMB = 0xFF587DA8;


    static final int WIN = 0xFF080D16, WIN_BORDER = 0xFF1E2D43, SEP = 0xFF17243A;
    static final int WHITE = 0xFFFFFFFF, DIM = 0xFF8394AA, ACCENT_TXT = 0xFF79B4FF;
    static final int CARD_OFF = 0xFF0D1522, CARD_OFF_BORDER = 0xFF263B57;


    static final float OPEN_TIME = 0.6f, CLOSE_TIME = 0.22f, PANEL_OPEN = 0.45f, PANEL_CLOSE = 0.2f;
    static final float BOUNCE = 2.2f;

    private record Nav(String label, int icon, int cat, float cy) {}
    private static final Nav[] NAV = {
            new Nav("Combat", Icons.COMBAT, 0, 81), new Nav("Movement", Icons.MOVEMENT, 1, 112.5f),
            new Nav("Render", Icons.RENDER, 2, 144), new Nav("Player", Icons.PLAYER, 3, 175.5f),
            new Nav("Util", Icons.UTIL, 4, 207), new Nav("Configs", Icons.CONFIGS, 5, 258),
            new Nav("Themes", Icons.THEMES, 6, 290), new Nav("AutoBuy", Icons.ITEMS, 7, 341)};

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

    private final class Panel {
        final Mod mod; float x, y, scroll, scrollT, maxScroll, t; boolean closing;
        Panel(Mod mod, float x, float y) { this.mod = mod; this.x = x; this.y = y; }
        float contentH() { float h = 0; for (Setting s : Modules.rows(mod)) h += rowH(s); return h; }
        float height() { return Math.min(HEAD_H + contentH() + 4, PANEL_MAX_H); }
    }


    private final List<Hit> hits = new ArrayList<>();
    private final List<Panel> panels = new ArrayList<>();
    private final Map<K, Float> anim = new HashMap<>();
    private int cat = 0;
    private float scroll, scrollT, maxScroll;
    private String query = "";
    private boolean searchFocus;
    private Panel drag;
    private Slider dragSlider;
    private float dragSliderX, dragSliderW;
    private float dragX, dragY, ox, oy, S = 1f, mlx, mly;

    private float curOx, curOy, curS = 1f;
    private Matrix3x2f m = new Matrix3x2f();
    private DrawContext ctx;


    private long lastNs;
    private float dt, time, openT, catT0 = 0.12f, alpha = 1f, ga = 1f;
    private boolean closing, covered;
    private static final int CAT_CONFIGS = 5;
    private boolean cfgFocus, dblClick;

    public DaamkyClickGui() { super(Text.literal("Daamky")); }

    @Override public boolean shouldPause() { return false; }


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
        daamky.render.Gfx.scissor = new net.minecraft.client.gui.ScreenRect(l, tp, Math.max(0, r - l), Math.max(0, bt - tp));
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

    private List<Mod> visible() {
        List<Mod> out = new ArrayList<>();
        String q = query.toLowerCase();
        for (Mod mod : Modules.ALL) {
            if (q.isEmpty() ? mod.category == cat : mod.name.toLowerCase().contains(q)) out.add(mod);
        }
        return out;
    }

    private void openPanel(Mod mod) {
        for (Panel p : panels) {
            if (p.mod == mod) { p.closing = false; front(p); return; }
        }
        panels.clear();
        panels.add(new Panel(mod, W - PANEL_W - 8, 48));
    }

    private void front(Panel p) { panels.remove(p); panels.add(p); }

    private void selectCat(int target) {
        if (cat == target && query.isEmpty()) return;
        cat = target; query = ""; scroll = scrollT = 0; catT0 = time;
        if (target == CAT_CONFIGS) ConfigsBridge.refresh();
    }



    @Override
    public void close() { closing = true; drag = null; dragSlider = null; }

    @Override
    public void tick() {
        super.tick();
        if (closing && openT <= 0f) super.close();
        else if (!closing && GuiStyle.isDropDown()) {
            // The Type setting was switched to DropDown.
            if (client != null) client.setScreen(new DaamkyDropDownGui());
        }
    }


    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
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


        openT = clamp01(openT + (closing ? -dt / CLOSE_TIME : dt / OPEN_TIME));
        float k = closing ? openT * openT : easeOutBack(openT, BOUNCE);
        ga = clamp01(openT * 2.5f);

        if (daamky.render.Theme.blur) super.render(c, mx, my, delta);
        else c.fill(0, 0, width, height, ((int) (0x70 * ga) << 24));
        if (ga <= 0.003f) return;

        float base = Math.min(1f, Math.min((height - 10f) / H, (width - 10f) / W));
        S = base * (0.65f + 0.35f * k);
        ox = (width - W * S) / 2f;
        oy = (height - H * S) / 2f;
        curOx = ox; curOy = oy; curS = S;
        mlx = (mx - ox) / S;
        mly = (my - oy) / S;
        alpha = ga;

        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.translate(ox, oy);
        ms.scale(S, S);
        Matrix3x2f mainM = new Matrix3x2f(ms);
        m = mainM;

        updateCover(-1);
        drawWindow();
        drawSidebar();
        drawTop();
        if (cat == CAT_CONFIGS && query.isEmpty()) {
            drawConfigList();
            if (panels.isEmpty()) drawConfigPanel();
        } else {
            drawCards();
            if (panels.isEmpty()) drawInspector();
        }
        for (Panel p : new ArrayList<>(panels)) {
            m = mainM; alpha = ga; curOx = ox; curOy = oy; curS = S;
            drawPanel(p);
        }
        m = mainM; alpha = ga; curOx = ox; curOy = oy; curS = S;

        ms.popMatrix();
    }

    private void drawWindow() {
        fill(0, 0, W, H, 14, WIN);
        border(0, 0, W, H, 14, 1f, WIN_BORDER);
        fill(SW, 12, 1, H - 24, 0, SEP);
        fill(W - PANEL_W - 14, 44, 1, H - 54, 0, SEP);
        hit(0, 0, W, H, null, null);
    }

    private void drawSidebar() {

        gglow(7, 7, 34, 34, 9, 8, 0x50);
        grad(7, 7, 34, 34, 9);
        ico(Icons.ATOM, 12, 12, 24, WHITE);
        tb("Daamky", 48, ty(23, 15), 15, WHITE);
        tr("1.0 RELEASE", 49, ty(36, 6.5f), 6.5f, ACCENT_TXT);

        tr("Модули", 10, ty(56, 8), 8, DIM);
        tr("Настройки", 10, ty(234, 8), 8, DIM);
        tr("Дополнительно", 10, ty(315, 8), 8, DIM);

        for (Nav n : NAV) {
            boolean sel = n.cat == cat && query.isEmpty(), dim = false;
            int tc = dim ? 0x59FFFFFF : WHITE;
            float top = n.cy - 13;


            float t = lin(n, NAVK, sel ? 1f : 0f, 0.28f);
            float hv = ap(n, HOV, !dim && hov(8, top, 118, 26) ? 1f : 0f, 16f);
            if (hv > 0.01f) fill(8, top, 118, 26, 8, ((int) (0x16 * hv) << 24) | 0xFFFFFF);
            float sw = 118 * easeInOut(t);
            if (sw >= 3f) {
                float f = clamp01(sw / 22f);
                gglowA(8, top, sw, 26, 8, 8, 0x80, f);
                gradA(8, top, sw, 26, 8, 1f);
            }
            ico(n.icon, 13, n.cy - 7, 14, tc);
            tb(n.label, 34, ty(n.cy, 11), 11, tc);
            if (!dim) { final int target = n.cat; hit(8, top, 118, 26, () -> selectCat(target), null); }
        }


        float cy = 392;
        fill(8, cy - 9.5f, 19, 19, 9.5f, 0xFFDCDCDC);
        ico(Icons.POWER, 11.25f, cy - 6.25f, 12.5f, 0xFF15151A);
        tb("Выйти", 31, ty(cy, 10), 10, 0xFFE9E9E9);
        hit(8, cy - 13, 118, 26, this::close, null);
    }

    private void drawTop() {

        float sx = 145, sy = 12, sw = 350, sh = 23;
        float fo = ap(this, EN, searchFocus ? 1f : 0f, 14f);
        fill(sx, sy, sw, sh, 11.5f, 0xFF101B2A);
        border(sx, sy, sw, sh, 11.5f, 1f, mix(0xFF24364F, 0xFF4C96F7, fo));
        ico(Icons.SEARCH, sx + 8, sy + 6, 11, WHITE);
        if (query.isEmpty() && !searchFocus) tr("Поиск", sx + 24, ty(sy + sh / 2, 9.5f), 9.5f, WHITE);
        else tr(query + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : ""), sx + 24, ty(sy + sh / 2, 9.5f), 9.5f, WHITE);
        hit(sx, sy, sw, sh, () -> searchFocus = true, null);

        ico(Icons.FOLDER, 509, 15, 16, 0xFFE2E2E8);
        hit(505, 11, 24, 24, () -> selectCat(CAT_CONFIGS), null);
        ico(Icons.GEAR, 533, 15, 16, 0xFFE2E2E8);
    }

    private void drawConfigList() {
        List<String> list = ConfigsBridge.list();
        float pitch = 40f, cardH = 34f;
        float total = list.size() * pitch + 10, viewH = CLIP_B - CLIP_T + 12;
        maxScroll = Math.max(0, total - viewH);
        scrollT = Math.max(0, Math.min(maxScroll, scrollT));
        scroll = Math.max(0, Math.min(maxScroll, lerpTo(scroll, scrollT, 18f)));

        float clipRight = W - PANEL_W - 14;
        scissor(COL1 - 12, CLIP_T, clipRight, CLIP_B);
        if (list.isEmpty()) {
            float tw = Txt.wBold("Конфигов пока нет", 11);
            tb("Конфигов пока нет", COL1 + (CARD_W - tw) / 2f, ty(120, 11), 11, WHITE);
            float yy = 138;
            for (String line : wrap("Введи имя справа и нажми «Сохранить», чтобы создать первый конфиг.", 8, CARD_W - 20)) {
                float lw = Txt.wBold(line, 8);
                tb(line, COL1 + (CARD_W - lw) / 2f, ty(yy, 8), 8, DIM);
                yy += 11;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            float y = 56 + i * pitch - scroll;
            if (y + cardH < CLIP_T || y > CLIP_B) continue;
            drawConfigCard(list.get(i), COL1, y, cardH);
        }
        endScissor();

        if (maxScroll > 0) {
            float trackT = CLIP_T + 6, trackH = CLIP_B - CLIP_T - 12;
            float th = Math.max(30, trackH * viewH / total), tyy = trackT + (trackH - th) * (scroll / maxScroll);
            fill(clipRight - 7, trackT, 3, trackH, 1.5f, 0xFF17243A);
            grad(clipRight - 7, tyy, 3, th, 1.5f);
        }
    }

    private void drawConfigCard(String name, float x, float y, float h) {
        boolean sel = name.equals(ConfigsBridge.selected);
        float vy = Math.max(y, CLIP_T), vh = Math.min(y + h, CLIP_B) - vy;
        float en = ap(name, EN, sel ? 1f : 0f, 11f);
        float hv = ap(name, HOV, vh > 0 && hov(x, vy, CARD_W, vh) ? 1f : 0f, 16f);
        gglowA(x, y, CARD_W, h, 7, 9, 0x80, en);
        fill(x, y, CARD_W, h, 7, CARD_OFF);
        gradA(x, y, CARD_W, h, 7, en);
        border(x, y, CARD_W, h, 7, 1f, mix(mix(CARD_OFF_BORDER, 0xFF4779B4, hv), 0x38FFFFFF, en));
        if (hv > 0.01f) fill(x, y, CARD_W, h, 7, ((int) (0x0C * hv * (1f - en)) << 24) | 0xFFFFFF);
        ico(Icons.CONFIGS, x + 10, y + h / 2f - 7, 14, WHITE);
        tb(name, x + 32, y + 6.5f, 11, WHITE);
        tr(sel ? "Выбран · двойной клик — загрузить" : "Нажми, чтобы выбрать", x + 32, y + 21, 7.5f, sel ? 0xFFE1EEFF : DIM);
        if (vh > 0) hit(x, vy, CARD_W, vh, () -> {
            ConfigsBridge.selected = name;
            if (dblClick) ConfigsBridge.loadSelected();
        }, () -> ConfigsBridge.selected = name);
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

    private void drawConfigPanel() {
        float x = W - PANEL_W - 8, y = 48, h = H - 58, ix = x + 12, iw = PANEL_W - 24;
        gglow(x, y, PANEL_W, h, 10, 10, 0x35);
        fill(x, y, PANEL_W, h, 10, PANEL_BG);
        border(x, y, PANEL_W, h, 10, 1f, 0xFF24364F);
        grad(x, y, PANEL_W, 30, 9);
        ico(Icons.CONFIGS, x + 10, y + 8, 13, WHITE);
        tb("КОНФИГИ", x + 30, ty(y + 15, 10), 10, WHITE);

        tb("Имя конфига", ix, ty(y + 46, 8), 8, DIM);
        float fy = y + 54, fh = 22;
        float fo = ap("cfgfield", EN, cfgFocus ? 1f : 0f, 14f);
        fill(ix, fy, iw, fh, 8, 0xFF101B2A);
        border(ix, fy, iw, fh, 8, 1f, mix(0xFF263A55, 0xFF4C96F7, fo));
        String typed = ConfigsBridge.name;
        if (typed.isEmpty() && !cfgFocus) {
            tr("Например: legit", ix + 9, ty(fy + fh / 2f, 9), 9, DIM);
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
            int col = kind == ConfigsBridge.OK ? 0xFF72DCA4 : kind == ConfigsBridge.ERROR ? 0xFFFF7C8C : ACCENT_TXT;
            float sy = y + 176;
            for (String line : wrap(status, 8, iw)) {
                tb(line, ix, ty(sy, 8), 8, col);
                sy += 11;
            }
        }

        float by = y + h - 56;
        fill(ix, by - 8, iw, 1, 0, 0xFF17243A);
        tb("Выбран:", ix, ty(by + 4, 8), 8, DIM);
        String sel = ConfigsBridge.selected == null ? "—" : ConfigsBridge.selected;
        tb(sel, ix + 44, ty(by + 4, 8), 8, ConfigsBridge.selected == null ? DIM : WHITE);
        float hy = by + 20;
        for (String line : wrap("Пустое имя — перезапишет выбранный конфиг. Конфиги лежат в папке daamky/config.", 7.5f, iw)) {
            tr(line, ix, ty(hy, 7.5f), 7.5f, DIM);
            hy += 10;
        }
    }

    private void drawInspector() {
        float x = W - PANEL_W - 8, y = 48, h = H - 58;
        gglow(x, y, PANEL_W, h, 10, 10, 0x35);
        fill(x, y, PANEL_W, h, 10, PANEL_BG);
        border(x, y, PANEL_W, h, 10, 1f, 0xFF24364F);
        grad(x, y, PANEL_W, 30, 9);
        ico(Icons.ATOM, x + 10, y + 8, 13, WHITE);
        tb("DAAMKY", x + 30, ty(y + 15, 10), 10, WHITE);
        tb("1.0 RELEASE", x + PANEL_W - 65, ty(y + 15, 7), 7, 0xFFD1E6FF);
        float cy = y + 86;
        float titleW = Txt.wBold("Выбери модуль", 12);
        tb("Выбери модуль", x + (PANEL_W - titleW) / 2f, ty(cy, 12), 12, WHITE);
        float hintW = Txt.wBold("Настройки появятся здесь", 8);
        tb("Настройки появятся здесь", x + (PANEL_W - hintW) / 2f, ty(cy + 22, 8), 8, DIM);
        fill(x + 18, cy + 42, PANEL_W - 36, 1, 0, 0xFF20314A);
        tb("INTERFACE TEST", x + 14, cy + 60, 7, ACCENT_TXT);
        tb("Лучшая Аура под ST — SpookyTime2", x + 14, cy + 78, 8, 0xFFD7E5F6);
        tb("Лучшая Аура под FT — Legit Snap", x + 14, cy + 94, 8, 0xFFD7E5F6);
        tb("Right Shift  —  ClickGUI", x + 14, cy + 122, 8, DIM);
    }

    private void drawCards() {
        List<Mod> list = visible();
        int rows = list.size();
        float total = rows * CARD_PITCH + 10, viewH = CLIP_B - CLIP_T + 12;
        maxScroll = Math.max(0, total - viewH);
        scrollT = Math.max(0, Math.min(maxScroll, scrollT));
        scroll = Math.max(0, Math.min(maxScroll, lerpTo(scroll, scrollT, 18f)));

        float clipRight = W - PANEL_W - 14;
        scissor(COL1 - 12, CLIP_T, clipRight, CLIP_B);
        float base = alpha;
        for (int i = 0; i < list.size(); i++) {
            Mod mod = list.get(i);
            float x = COL1, y = 56 + i * CARD_PITCH - scroll;
            if (y + CARD_H < CLIP_T || y > CLIP_B) continue;
            drawCard(mod, x, y, 0f);
        }
        alpha = base;
        endScissor();

        if (maxScroll > 0) {
            float trackT = CLIP_T + 6, trackH = CLIP_B - CLIP_T - 12;
            float th = Math.max(30, trackH * viewH / total), ty = trackT + (trackH - th) * (scroll / maxScroll);
            fill(clipRight - 7, trackT, 3, trackH, 1.5f, 0xFF17243A);
            grad(clipRight - 7, ty, 3, th, 1.5f);
        }
    }

    private void drawCard(Mod mod, float x, float baseY, float yOff) {
        float vy = Math.max(baseY, CLIP_T), vh = Math.min(baseY + CARD_H, CLIP_B) - vy;
        float en = ap(mod, EN, mod.enabled ? 1f : 0f, 11f);
        float hv = ap(mod, HOV, vh > 0 && hov(x, vy, CARD_W, vh) ? 1f : 0f, 16f);
        float pop = ap(mod, POP, 0f, 9f);
        float y = baseY + yOff;


        Matrix3x2f saved = m;
        float sc = 1f - 0.045f * pop, ccx = x + CARD_W / 2f, ccy = y + CARD_H / 2f;
        if (pop > 0.002f) m = new Matrix3x2f(saved).translate(ccx, ccy).scale(sc, sc).translate(-ccx, -ccy);

        gglowA(x, y, CARD_W, CARD_H, 7, 9, 0x80, en);
        fill(x, y, CARD_W, CARD_H, 7, CARD_OFF);
        gradA(x, y, CARD_W, CARD_H, 7, en);
        border(x, y, CARD_W, CARD_H, 7, 1f, mix(mix(CARD_OFF_BORDER, 0xFF4779B4, hv), 0x38FFFFFF, en));
        if (hv > 0.01f) fill(x, y, CARD_W, CARD_H, 7, ((int) (0x0C * hv * (1f - en)) << 24) | 0xFFFFFF);

        final float ts = 11.5f;
        tb(mod.name, x + 8, y + 4f, ts, WHITE);
        float ix = x + 8 + Txt.wBold(mod.name, ts) + 4;
        float gearX = -1;
        if (mod.hasSettings()) { gearX = ix; ico(Icons.GEAR, ix, y + 5.2f, 11, WHITE); ix += 13.5f; }
        ico(Icons.CLIP, ix, y + 5.2f, 11, 0xFFEDEDF3);

        if (binding == mod || mod.hasBind()) {
            String bt = binding == mod ? "[...]" : "[" + mod.keyDisplay() + "]";
            tb(bt, x + CARD_W - 8 - Txt.wBold(bt, 8), y + 5.5f, 8, binding == mod ? ACCENT_TXT : 0xFFE1EEFF);
        }
        int dc = mix(0xFFDADAE1, 0xFFEAF4FF, en);
        float ly = y + 17f;
        for (String line : wrap(mod.desc, 7.5f, CARD_W - 14)) {
            if (ly + 9 > y + CARD_H) break;
            tb(line, x + 8, ly, 7.5f, dc);
            ly += 8.8f;
        }
        m = saved;


        if (vh <= 0) return;
        if (!closing) hits.add(new Hit(x, vy, CARD_W, vh, () -> { mod.setEnabled(!mod.enabled); anim.put(new K(mod, POP), 1f); },
                mod.hasSettings() ? () -> openPanel(mod) : null, () -> binding = mod));
        if (gearX >= 0) hit(gearX - 2, baseY + 3, 15, 15, () -> openPanel(mod), () -> openPanel(mod));
    }


    private void hitClip(float x, float y, float w, float h, float vt, float vb, Runnable l, Runnable r) {
        float y0 = Math.max(y, vt), y1 = Math.min(y + h, vb);
        if (y1 > y0) hit(x, y0, w, y1 - y0, l, r);
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


    private float cx, cy;

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
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
        searchFocus = false;
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
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
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
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        drag = null;
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
        if (searchFocus && !chr.isEmpty() && chr.codePointAt(0) >= 32 && query.length() < 24) { query += chr; scroll = scrollT = 0; return true; }
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
        try { return net.minecraft.client.util.InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString(); }
        catch (Exception e) { return "?"; }
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
        if (searchFocus && keyCode == 259  && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; }
        if (searchFocus && keyCode == 256 ) { searchFocus = false; return true; }
        return super.keyPressed(input);
    }
}
