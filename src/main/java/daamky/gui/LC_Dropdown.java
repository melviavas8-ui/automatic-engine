package daamky.gui;

import daamky.gui.Modules.BindValue;
import daamky.gui.Modules.Bool;
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
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Liberty City "Dropdown" ClickGUI (layout and look of the Altex "delta" panels): every category is a
 * fixed 128 px column with a header, modules are rows with their description under the name and a switch
 * on the right. Right click on a row folds its settings out inside the row, middle click binds a key.
 * Settings use compact elements: check box, slider, mode chips, color palette, key box, button.
 * Everything is read from and written to the real settings through {@link Modules}.
 *
 * Opening: the columns fly in from near the camera at the sides toward the middle (outer columns first,
 * middle ones last, with a small bounce), then the rows cascade in. Colors come from the active
 * Liberty City theme: the accent is the theme's, everything else is neutral gray like the original.
 */
public class LC_Dropdown extends Screen {

    // ---- layout (design units; the whole group is scaled so the physical size matches GUI scale 2) ----
    static final float REF_GUI_SCALE = 2f;
    static final float COL_W = 128, COL_GAP = 5, HEAD_H = 20, COL_H = 262;
    static final float SEARCH_W = 130, SEARCH_H = 16, SEARCH_GAP = 8;
    static final float ROW_H = 16, DESC_SIZE = 6, DESC_LINE_H = 8, DESC_PAD = 2;
    static final int DESC_MAX_LINES = 3;
    static final float OPEN_TIME = 0.5f, CLOSE_TIME = 0.2f;
    /** Fly-in of one column (seconds) and the extra delay of the middle ones; ANIM + MAX_DELAY stays within OPEN_TIME. */
    static final float PANEL_ANIM = 0.34f, MAX_DELAY = 0.14f, NEAR_SCALE = 1.55f;
    static final int WHITE = 0xFFFFFFFF, PANEL = 0xFF171717, STROKE = 0xFF232323, STROKE2 = 0xFF282828, DISABLED = 0xFF434651;

    private record Cat(String label, int icon, int id) {}
    private static final Cat[] CATS = {
            new Cat("Combat", Icons.COMBAT, 0), new Cat("Movement", Icons.MOVEMENT, 1), new Cat("Render", Icons.RENDER, 2),
            new Cat("Player", Icons.PLAYER, 3), new Cat("Util", Icons.UTIL, 4), new Cat("Display", Icons.THEMES, 6),
            new Cat("AutoBuy", Icons.ITEMS, 7)};

    private static final class Panel {
        final Cat cat;
        List<Mod> mods = List.of();
        float x, y, scroll, scrollT, content, view;
        Panel(Cat cat) { this.cat = cat; }
    }

    private record Hit(float x, float y, float w, float h, Runnable left, Runnable right, Runnable mid) {}
    private record K(Object o, int kind) {}
    private static final int EN = 0, HOV = 1, EXT = 2, CHK = 3, CHIP = 4, GRP = 5, BTN = 6, BIND = 7, FOCUS = 8;
    private enum Drag { NONE, AREA, HUE, ALPHA }

    private final List<Panel> panels = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<K, Float> anim = new HashMap<>();
    private final Set<Mod> expanded = new HashSet<>();
    private final Map<Mod, String[]> descCache = new HashMap<>();
    private float descCacheWidth = -1f;

    private Mod binding;
    private BindValue bindingSetting;
    private TextValue editingText;
    private String editBuffer = "";
    private String query = "";
    private boolean searchFocus;

    private Slider dragSlider;
    private float dragSliderX, dragSliderW;

    // color palette popup
    private ColorValue paletteOf;
    private float palX, palY, palH, palS, palB, palA = 1f;
    private Drag palDrag = Drag.NONE;

    // current transform of the column being drawn (scale about the screen center) and clip of its view
    private float tfS = 1f, tfCx, tfCy;
    private float clipT, clipB, clipL, clipR;

    private float S = 1f, dw, dh, mlx, mly, clickX, clickY;
    private Matrix3x2f m = new Matrix3x2f();
    private DrawContext ctx;
    private long lastNs;
    private float dt, time, openT, alpha = 1f, ga = 1f;
    private boolean closing;

    // palette (accent from the theme, the rest is neutral like the original)
    private int accent, accTxt;

    public LC_Dropdown() {
        super(Text.literal("Daamky"));
        for (Cat c : CATS) panels.add(new Panel(c));
    }

    @Override public boolean shouldPause() { return false; }

    // ---------------------------------------------------------------- math / helpers

    private static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    private static float easeOutCubic(float t) { t = clamp01(t); float u = 1 - t; return 1 - u * u * u; }
    private static float easeOutBack(float t, float k) { t = clamp01(t); float u = t - 1; return 1 + (k + 1) * u * u * u + k * u * u; }
    private static float ty(float center, float size) { return center - size * 0.58f; }

    private static int mix(int a, int b, float t) {
        int r = 0;
        for (int sh = 24; sh >= 0; sh -= 8) {
            int ca = (a >>> sh) & 0xFF, cb = (b >>> sh) & 0xFF;
            r |= (Math.round(ca + (cb - ca) * t) & 0xFF) << sh;
        }
        return r;
    }

    /** Same color with its alpha multiplied by a (0..1). */
    private static int ca(int c, float a) {
        int v = Math.round(((c >>> 24) & 0xFF) * clamp01(a));
        return (v << 24) | (c & 0xFFFFFF);
    }

    private static int rgb(Color c) { return 0xFF000000 | (c.getRGB() & 0xFFFFFF); }

    private void updatePalette() {
        ClientTheme t = ThemeManager.current();
        accent = rgb(t.accent());
        accTxt = rgb(t.accentBright());
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

    private float peek(Object o, int kind, float def) {
        Float v = anim.get(new K(o, kind));
        return v == null ? def : v;
    }

    private int A(int c) { return ca(c, alpha); }
    private void fill(float x, float y, float w, float h, float r, int c) { SdfRect.fill(m, x, y, w, h, r, A(c)); }
    private void border(float x, float y, float w, float h, float r, float t, int c) { SdfRect.border(m, x, y, w, h, r, t, A(c)); }
    private void gradient(float x, float y, float w, float h, int tl, int tr, int br, int bl) {
        SdfRect.gradient(m, x, y, w, h, 0, A(tl), A(tr), A(br), A(bl));
    }
    private float tb(String s, float x, float y, float size, int c) { return Txt.bold(m, s, x, y, size, A(c)); }
    private float tr(String s, float x, float y, float size, int c) { return Txt.reg(m, s, x, y, size, A(c)); }
    private void ico(int cp, float x, float y, float size, int c) {
        String img = daamky.render.Img.forIcon(cp);
        if (img != null && daamky.render.Img.draw(m, img, x, y, size, size, A(c))) return;
        Txt.icon(m, cp, x, y, size, A(c));
    }

    private String clip(String value, float maxWidth, float size) {
        String r = value == null ? "" : value;
        if (Txt.wReg(r, size) <= maxWidth) return r;
        while (r.length() > 1 && Txt.wReg(r + "…", size) > maxWidth) r = r.substring(0, r.length() - 1);
        return r + "…";
    }

    // hit areas live in screen design units, so the column transform is applied when they are registered
    private void addHit(float x, float y, float w, float h, Runnable l, Runnable r, Runnable mid) {
        if (closing || w <= 0f || h <= 0f) return;
        float y0 = Math.max(y, clipT), y1 = Math.min(y + h, clipB);
        if (y1 <= y0) return;
        hits.add(new Hit(tfCx + (x - tfCx) * tfS, tfCy + (y0 - tfCy) * tfS, w * tfS, (y1 - y0) * tfS, l, r, mid));
    }

    private boolean hov(float x, float y, float w, float h) {
        if (closing) return false;
        float y0 = Math.max(y, clipT), y1 = Math.min(y + h, clipB);
        if (y1 <= y0) return false;
        float lx = tfCx + (mlx - tfCx) / tfS;
        float ly = tfCy + (mly - tfCy) / tfS;
        return lx >= x && lx < x + w && ly >= y0 && ly < y1;
    }

    private void scissor(float x1, float y1, float x2, float y2) {
        x1 = Math.max(x1, clipL); x2 = Math.min(x2, clipR);
        y1 = Math.max(y1, clipT); y2 = Math.min(y2, clipB);
        int l = (int) Math.floor((tfCx + (x1 - tfCx) * tfS) * S), tp = (int) Math.floor((tfCy + (y1 - tfCy) * tfS) * S);
        int r = (int) Math.ceil((tfCx + (x2 - tfCx) * tfS) * S), bt = (int) Math.ceil((tfCy + (y2 - tfCy) * tfS) * S);
        daamky.render.Gfx.scissor = new ScreenRect(l, tp, Math.max(0, r - l), Math.max(0, bt - tp));
    }

    private void endScissor() { daamky.render.Gfx.scissor = null; }

    // ---------------------------------------------------------------- data

    private static String key(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(c)) sb.append(c);
        }
        return sb.toString();
    }

    private List<Mod> modsFor(Cat cat) {
        String q = key(query);
        List<Mod> out = new ArrayList<>();
        for (Mod mod : Modules.ALL) {
            if (mod.category == cat.id && (q.isEmpty() || key(mod.name).contains(q))) out.add(mod);
        }
        out.sort(Comparator.comparing(mod -> mod.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    private static void collect(List<Setting> in, List<Setting> out) {
        for (Setting s : in) {
            if (!s.isVisible()) continue;
            out.add(s);
            if (s instanceof Group g && !(s instanceof Multi) && g.open) collect(g.children, out);
        }
    }

    private static List<Setting> rowsOf(Mod mod) {
        List<Setting> out = new ArrayList<>();
        collect(mod.settings, out);
        return out;
    }

    /** Words wrapped to the width, at most three lines, "..." when the text is longer. */
    private String[] descLines(Mod mod, float width) {
        if (width != descCacheWidth) { descCache.clear(); descCacheWidth = width; }
        String[] cached = descCache.get(mod);
        if (cached != null) return cached;
        List<String> lines = new ArrayList<>();
        String text = mod.desc == null ? "" : mod.desc.trim();
        if (!text.isEmpty()) {
            StringBuilder line = new StringBuilder();
            String[] words = text.split(" +");
            int i = 0;
            for (; i < words.length; i++) {
                String candidate = line.length() == 0 ? words[i] : line + " " + words[i];
                if (Txt.wReg(candidate, DESC_SIZE) <= width || line.length() == 0) {
                    line = new StringBuilder(candidate);
                    continue;
                }
                lines.add(line.toString());
                line = new StringBuilder(words[i]);
                if (lines.size() >= DESC_MAX_LINES) break;
            }
            boolean more = i < words.length - 1 || lines.size() >= DESC_MAX_LINES;
            if (lines.size() < DESC_MAX_LINES && line.length() > 0) lines.add(line.toString());
            if (more && !lines.isEmpty()) lines.set(lines.size() - 1, lines.get(lines.size() - 1) + "...");
        }
        cached = lines.toArray(new String[0]);
        descCache.put(mod, cached);
        return cached;
    }

    private float descHeight(Mod mod, float width) {
        int n = descLines(mod, width).length;
        return n == 0 ? 0f : DESC_PAD + n * DESC_LINE_H + DESC_PAD;
    }

    private float chipsHeight(List<String> options, float w) {
        float x = 0, rows = 1;
        for (String option : options) {
            float cw = Txt.wReg(option, 6.25f) + 6;
            if (x + cw > w) { x = 0; rows++; }
            x += cw + 3;
        }
        return 6.5f + 5 + (rows - 1) * 12 + 9;
    }

    private static List<String> optionNames(Setting s) {
        List<String> names = new ArrayList<>();
        if (s instanceof Mode md) { for (String n : md.modes) names.add(n); }
        else if (s instanceof Multi mu) { for (Setting c : mu.children) names.add(c.name); }
        return names;
    }

    private float elementHeight(Setting s, float w) {
        if (s instanceof Section) return 9;
        if (s instanceof Group && !(s instanceof Multi)) return 11;
        if (s instanceof Mode || s instanceof Multi) return chipsHeight(optionNames(s), w);
        if (s instanceof Slider) return 18;
        if (s instanceof Button) return 14;
        if (s instanceof TextValue) return 12;
        return 11;
    }

    private float settingsHeight(Mod mod, float w) {
        float h = 0;
        for (Setting s : rowsOf(mod)) h += elementHeight(s, w) + 4;
        return h;
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    public void close() {
        closing = true;
        dragSlider = null;
        paletteOf = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (closing) {
            if (openT <= 0f) super.close();
        } else if (!GuiStyle.isDropDown()) {
            if (client != null) client.setScreen(GuiStyle.create());
        }
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        ctx = c;
        Modules.refreshFromTarget();
        updatePalette();
        daamky.render.Gfx.ctx = c;
        daamky.render.Gfx.scissor = null;
        MsdfText.msdfMode = Modules.useMsdf();
        MsdfText.baseWeight = Modules.weight();
        hits.clear();

        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        time += dt;

        openT = clamp01(openT + (closing ? -dt / CLOSE_TIME : dt / OPEN_TIME));
        ga = clamp01(openT * 2.5f);

        if (daamky.render.Theme.blur) super.render(c, mx, my, delta);
        else c.fill(0, 0, width, height, ((int) (0x70 * ga) << 24));
        if (ga <= 0.003f) return;

        int n = panels.size();
        float groupW = n * COL_W + (n - 1) * COL_GAP;
        float groupH = COL_H + SEARCH_GAP + SEARCH_H;
        float guiScale = (float) client.getWindow().getScaleFactor();
        S = Math.max(0.25f, Math.min(REF_GUI_SCALE / guiScale, Math.min((width - 16f) / groupW, (height - 16f) / groupH)));
        dw = width / S;
        dh = height / S;
        mlx = mx / S;
        mly = my / S;

        float startX = (dw - groupW) / 2f;
        float colY = (dh - groupH) / 2f;
        for (int i = 0; i < n; i++) {
            Panel p = panels.get(i);
            p.mods = modsFor(p.cat);
            p.x = startX + i * (COL_W + COL_GAP);
            p.y = colY;
        }

        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.scale(S, S);
        Matrix3x2f base = new Matrix3x2f(ms);
        m = base;
        alpha = ga;
        tfS = 1f; tfCx = dw / 2f; tfCy = dh / 2f;
        clipL = 0; clipR = dw; clipT = 0; clipB = dh;

        float mid = (n - 1) / 2f;
        float spread = Math.max(0.5f, mid);
        for (int i = 0; i < n; i++) {
            Panel p = panels.get(i);
            // Outer columns first, middle ones last; scaling about the screen center pushes the side columns outward while big.
            float delay = (1f - Math.abs(i - mid) / spread) * MAX_DELAY;
            float local = clamp01((openT * OPEN_TIME - delay) / PANEL_ANIM);
            float e = closing ? openT * openT : easeOutBack(local, 1.35f);
            tfS = closing ? 0.94f + 0.06f * openT : 1f + (NEAR_SCALE - 1f) * (1f - e);
            tfCx = dw / 2f;
            tfCy = dh / 2f;
            alpha = ga * (closing ? 1f : clamp01(local * 2.5f));
            m = new Matrix3x2f(base).translate(tfCx, tfCy).scale(tfS, tfS).translate(-tfCx, -tfCy);
            drawPanel(p, i, n, closing ? 1f : openT);
        }

        m = base;
        alpha = ga;
        tfS = 1f; tfCx = dw / 2f; tfCy = dh / 2f;
        clipL = 0; clipR = dw; clipT = 0; clipB = dh;
        drawSearch(startX, colY + COL_H + SEARCH_GAP, groupW);
        drawPalette();
        endScissor();
        ms.popMatrix();
    }

    // ---------------------------------------------------------------- column

    private float reveal(float front, float column, float rowT) {
        float start = column * 0.45f + rowT * 0.35f;
        return easeOutCubic(clamp01((front - start) / 0.20f));
    }

    private void drawPanel(Panel p, int index, int count, float front) {
        float x = p.x, y = p.y, w = COL_W, h = COL_H;
        float pa = clamp01(front / 0.30f);
        fill(x, y, w, h, 8, ca(PANEL, 1f));
        border(x, y, w, h, 8, 0.5f, STROKE);

        // header: category name on the left, its icon in the accent color on the right
        float hc = y + HEAD_H * 0.5f + 1.5f;
        float iconSize = 10;
        tb(p.cat.label, x + 6, ty(hc, 10), 10, WHITE);
        ico(p.cat.icon, x + w - 6 - iconSize, hc - iconSize / 2f, iconSize, accent);

        float y0 = y + HEAD_H + 3;
        float view = (y + h - 6) - y0 + 4;
        float rowX = x + 6, rowW = w - 12;
        float descW = Math.max(20f, rowW - 9f);

        // content height
        float[] heights = new float[p.mods.size()];
        float content = 0;
        for (int i = 0; i < p.mods.size(); i++) {
            Mod mod = p.mods.get(i);
            float ext = mod.hasSettings() ? peek(mod, EXT, 0f) : 0f;
            heights[i] = ROW_H + descHeight(mod, descW) + (mod.hasSettings() ? settingsHeight(mod, rowW - 9f) * ext : 0f);
            content += heights[i] + 4;
        }
        p.content = content;
        p.view = view;
        float maxScroll = Math.max(0f, content - view);
        p.scrollT = Math.max(0f, Math.min(maxScroll, p.scrollT));
        p.scroll = Math.max(0f, Math.min(maxScroll, lerpTo(p.scroll, p.scrollT, 16f)));

        clipL = x; clipR = x + w; clipT = y0; clipB = y0 + view;
        scissor(x, y0, x + w, y0 + view);

        float cy = y0 - p.scroll;
        float colFrac = count > 1 ? (float) index / (count - 1) : 0f;
        for (int i = 0; i < p.mods.size(); i++) {
            Mod mod = p.mods.get(i);
            float rowT = content > 0 ? (cy - (y0 - p.scroll)) / content : 0f;
            float appear = reveal(front, colFrac, rowT);
            float ry = cy + (1f - appear) * 6f;
            if (ry + heights[i] > y0 - 2 && ry < y0 + view + 2) {
                drawModule(mod, rowX, ry, rowW, descW, heights[i], y0 + view, appear);
            }
            cy += heights[i] + 4;
        }
        endScissor();
        clipT = 0; clipB = dh; clipL = 0; clipR = dw;

        if (p.mods.isEmpty()) {
            String s = query.isEmpty() ? "Пусто" : "Не найдено";
            tr(s, x + (w - Txt.wReg(s, 7)) / 2f, ty(y + HEAD_H + 30, 7), 7, ca(WHITE, 0.4f * pa));
        }
    }

    private void drawModule(Mod mod, float rx, float ry, float rw, float descW, float total, float bottom, float appear) {
        float headTotal = ROW_H + descHeight(mod, descW);
        float center = ry + ROW_H * 0.5f;
        float act = ap(mod, EN, mod.enabled ? 1f : 0f, 9f);
        boolean over = hov(rx, ry, rw, headTotal);
        float hv = ap(mod, HOV, over ? 1f : 0f, 14f);
        float fade = clamp01((bottom - ry) / 16f) * appear;
        float ext = mod.hasSettings() ? ap(mod, EXT, expanded.contains(mod) ? 1f : 0f, 11f) : 0f;

        // row plate
        fill(rx, ry, rw, total, 4, ca(0xFF232323, 0.47f * fade));
        fill(rx, ry, rw, total, 4, ca(accent, 0.16f * act * fade));
        fill(rx, ry, rw, total, 4, ca(accent, 0.07f * hv * fade));
        float bd = Math.min(1f, (0.5f + 0.35f * act + 0.3f * hv) * fade);
        border(rx, ry, rw, total, 4, 0.5f, ca(0xFF232323, bd));

        boolean hasSwitch = true;
        boolean hasSettings = mod.hasSettings();
        float nameX = rx + 4;
        float rightReserve = 18f + (hasSettings ? Txt.wBold("...", 10) + 2f : 0f);
        float nameMax = Math.max(10f, rx + rw - 4 - rightReserve - nameX);
        String name = mod.name;
        while (name.length() > 1 && Txt.wBold(name, 7.25f) > nameMax) name = name.substring(0, name.length() - 1);
        tb(name, nameX, ty(center - 0.5f, 7.25f), 7.25f, ca(WHITE, fade));

        // description under the name
        String[] desc = descLines(mod, descW);
        float descTop = ry + ROW_H + DESC_PAD;
        for (int i = 0; i < desc.length; i++) {
            tr(desc[i], nameX, ty(descTop + i * DESC_LINE_H + DESC_LINE_H * 0.5f, DESC_SIZE), DESC_SIZE, ca(WHITE, 0.5f * fade));
        }

        // key box after the name
        boolean binds = binding == mod;
        float bindT = ap(mod, BIND, binds || mod.hasBind() ? 1f : 0f, 14f);
        if (bindT > 0.02f) {
            String bt = binds ? "?" : mod.keyDisplay();
            float bw = 8f + Txt.wReg(bt, 6f);
            float bx = rx + 4 + Txt.wBold(name, 7.25f) + 4;
            if (bx + bw < rx + rw - rightReserve) {
                fill(bx, center - 4.5f, bw, 9, 2, ca(accent, 0.157f * bindT * fade));
                border(bx, center - 4.5f, bw, 9, 2, 0.5f, ca(0xFF232323, bindT * fade));
                tr(bt, bx + 4, ty(center, 6), 6, ca(WHITE, bindT * fade));
            }
        }

        // "..." when the module has settings
        if (hasSettings) {
            float dx = rx + rw - 4 - Txt.wBold("...", 10) - (hasSwitch ? 18f : 0f);
            tb("...", dx, ty(center - 1.5f, 10), 10, ca(mix(0xFF8D939C, WHITE, Math.max(ext, hv)), fade));
        }

        // switch
        float tgx = rx + rw - 4 - 14, tgy = center - 4.25f;
        int track = mix(0xFF686870, accent, act);
        fill(tgx, tgy, 14, 8.5f, 3.25f, ca(track, 0.49f * fade));
        border(tgx, tgy, 14, 8.5f, 3.25f, 0.3f, ca(0xFF232323, fade));
        fill(tgx + 1.5f + 5.5f * act, tgy + 1.5f, 5.5f, 5.5f, 1.75f, ca(mix(0xFFC8C8CE, WHITE, act), fade));

        // click: left toggles, right folds the settings out, middle binds a key
        addHit(rx, ry, rw, headTotal,
                () -> mod.setEnabled(!mod.enabled),
                hasSettings ? () -> { if (!expanded.remove(mod)) expanded.add(mod); closePalette(); } : null,
                () -> binding = mod);

        // folded-out settings
        if (hasSettings && ext > 0.01f) {
            float setW = rw - 9f;
            float sx = rx + 4.5f;
            float setTop = ry + headTotal;
            float sy = setTop - 4f * (1f - ext);
            scissor(sx, Math.max(setTop - 1, clipT), sx + setW, ry + total);
            float oldT = clipT, oldB = clipB;
            clipT = Math.max(clipT, setTop - 1);
            clipB = Math.min(clipB, ry + total);
            float off = 0;
            float savedAlpha = alpha;
            alpha = savedAlpha * ext;
            for (Setting s : rowsOf(mod)) {
                float eh = elementHeight(s, setW);
                float eY = sy + off - 4f * (1f - ext) * 0f;
                drawElement(s, sx, eY, setW, eh, act);
                off += eh + 4;
            }
            alpha = savedAlpha;
            clipT = oldT;
            clipB = oldB;
            scissor(clipL, clipT, clipR, clipB);
        }
    }

    // ---------------------------------------------------------------- setting elements

    private void drawElement(Setting s, float x, float y, float w, float h, float activation) {
        if (s instanceof Section sec) drawSection(sec, x, y, w, h);
        else if (s instanceof Multi mu) drawChips(mu, x, y, w, h);
        else if (s instanceof Group g) drawGroup(g, x, y, w, h);
        else if (s instanceof Bool b) drawBool(b, x, y, w, h);
        else if (s instanceof Slider sl) drawSlider(sl, x, y, w);
        else if (s instanceof Mode md) drawChips(md, x, y, w, h);
        else if (s instanceof TextValue t) drawText(t, x, y, w, h);
        else if (s instanceof ColorValue cv) drawColor(cv, x, y, w, h);
        else if (s instanceof BindValue bv) drawBind(bv, x, y, w, h);
        else if (s instanceof Button btn) drawButton(btn, x, y, w, h);
    }

    private void label(String text, float x, float y, float h, float maxW, int color) {
        tr(clip(text, maxW, 6.5f), x, ty(y + h / 2f, 6.5f), 6.5f, color);
    }

    private void drawSection(Section sec, float x, float y, float w, float h) {
        float tw = Txt.wBold(sec.name, 6.5f), cx = x + (w - tw) / 2f, cy = y + h / 2f;
        tb(sec.name, cx, ty(cy, 6.5f), 6.5f, accTxt);
        fill(x, cy, Math.max(0f, cx - 4 - x), 0.5f, 0, ca(WHITE, 0.14f));
        fill(cx + tw + 4, cy, Math.max(0f, x + w - (cx + tw + 4)), 0.5f, 0, ca(WHITE, 0.14f));
    }

    private void drawGroup(Group g, float x, float y, float w, float h) {
        boolean over = hov(x, y, w, h);
        float hv = ap(g, HOV, over ? 1f : 0f, 14f);
        float open = ap(g, GRP, g.open ? 1f : 0f, 14f);
        tb(g.name, x, ty(y + h / 2f, 6.5f), 6.5f, mix(accTxt, WHITE, hv * 0.5f));
        Matrix3x2f saved = m;
        float ax = x + w - 4, ay = y + h / 2f;
        m = new Matrix3x2f(saved).translate(ax, ay).rotate((float) Math.PI * open).translate(-ax, -ay);
        ico(Icons.CHEVRON, ax - 3.25f, ay - 3.25f, 6.5f, WHITE);
        m = saved;
        addHit(x, y, w, h, () -> g.open = !g.open, null, null);
    }

    private void drawBool(Bool b, float x, float y, float w, float h) {
        boolean over = hov(x, y, w, h);
        float on = ap(b, CHK, b.value ? 1f : 0f, 11f);
        float cy = y + h / 2f;
        label(b.name, x, y, h, w - 11 - 4, WHITE);
        float bx = x + w - 11, by = cy - 5.5f;
        fill(bx, by, 11, 11, 3, ca(accent, 0.039f + 0.25f * on));
        border(bx, by, 11, 11, 3, 0.5f, mix(STROKE, accent, on * 0.8f));
        if (on > 0.02f) {
            float s = 5.5f * on;
            fill(bx + (11 - s) / 2f, by + (11 - s) / 2f, s, s, 1.5f, ca(WHITE, on));
        } else if (over) {
            fill(bx + 3.5f, by + 3.5f, 4, 4, 1.2f, ca(WHITE, 0.15f));
        }
        addHit(x, y, w, h, () -> b.set(!b.value), null, null);
    }

    private void drawSlider(Slider sl, float x, float y, float w) {
        float progress = sl.frac();
        String value = sl.text();
        float bw = Txt.wReg(value, 6.25f) + 6, bh = 6.25f + 2;
        float bx = x + w - bw;
        label(sl.name, x, y + 0.5f, 6.5f, bx - x - 4, WHITE);
        fill(bx, y, bw, bh, 2, ca(accent, 0.031f));
        border(bx, y, bw, bh, 2, 0.5f, STROKE);
        tr(value, bx + (bw - Txt.wReg(value, 6.25f)) / 2f, ty(y + bh / 2f, 6.25f), 6.25f, WHITE);

        float trackY = y + 6.5f + 6.5f, trackH = 2, knob = 7;
        fill(x, trackY, w, trackH, 1, ca(0xFF32343C, 0.35f));
        fill(x, trackY, w * progress, trackH, 1, accent);
        fill(x + (w - knob) * progress, trackY + trackH / 2f - knob / 2f, knob, knob, knob / 2f, WHITE);
        final float fx = x, fw = w;
        addHit(x, y + 6.5f, w, 11, () -> {
            dragSlider = sl; dragSliderX = fx; dragSliderW = fw;
            setSliderFromMouse(sl, fx, fw, clickX);
        }, null, null);
    }

    private void setSliderFromMouse(Slider sl, float x, float w, float mouseX) {
        float f = clamp01((mouseX - x) / w);
        sl.set(sl.min + f * (sl.max - sl.min));
    }

    private void drawChips(Setting s, float x, float y, float w, float h) {
        String name = s.name;
        label(name, x, y, 6.5f, w - 4, WHITE);
        float cx = x, cy = y + 6.5f + 5;
        List<String> options = optionNames(s);
        float lum = accent == 0 ? 0 : ((accent >> 16 & 255) * 0.299f + (accent >> 8 & 255) * 0.587f + (accent & 255) * 0.114f) / 255f;
        int onAccent = lum < 0.45f ? WHITE : 0xFF0A0A0C;
        for (int i = 0; i < options.size(); i++) {
            String option = options.get(i);
            float cw = Txt.wReg(option, 6.25f) + 6;
            if (cx + cw > x + w) { cx = x; cy += 12; }
            boolean selected = s instanceof Mode md ? md.index == i : ((Bool) ((Multi) s).children.get(i)).value;
            float v = ap(s.name + "#" + System.identityHashCode(s) + ":" + i, CHIP, selected ? 1f : 0f, 12f);
            int plate = mix(WHITE, accent, v);
            fill(cx, cy, cw, 9, 2, ca(plate, (5f + 250f * v) / 255f));
            border(cx, cy, cw, 9, 2, 0.5f, STROKE);
            int tc = mix(DISABLED, onAccent, v);
            tr(option, cx + (cw - Txt.wReg(option, 6.25f)) / 2f, ty(cy + 4.5f, 6.25f), 6.25f, tc);
            final int idx = i;
            addHit(cx, cy, cw, 9, () -> {
                if (s instanceof Mode md) md.select(idx);
                else { Bool b = (Bool) ((Multi) s).children.get(idx); b.set(!b.value); }
            }, null, null);
            cx += cw + 3;
        }
    }

    private void drawText(TextValue t, float x, float y, float w, float h) {
        boolean editing = editingText == t;
        float fo = ap(t, FOCUS, editing ? 1f : 0f, 14f);
        fill(x, y, w, h, 3, 0xFF121214);
        border(x, y, w, h, 3, 0.5f, mix(0xFF282828, accent, fo));
        String shown = editing ? editBuffer + ((System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "") : t.text();
        boolean empty = !editing && (shown == null || shown.isEmpty());
        if (empty) shown = t.name;
        while (shown.length() > 1 && Txt.wReg(shown, 6.5f) > w - 8) shown = shown.substring(editing ? 1 : 0, editing ? shown.length() : shown.length() - 1);
        tr(shown, x + 4, ty(y + h / 2f, 6.5f), 6.5f, empty ? ca(WHITE, 0.4f) : WHITE);
        addHit(x, y, w, h, () -> { editingText = t; editBuffer = t.text(); }, null, null);
    }

    private void drawBind(BindValue v, float x, float y, float w, float h) {
        boolean capturing = bindingSetting == v;
        float cap = ap(v, BIND, capturing ? 1f : 0f, 12f);
        String value = capturing ? "..." : v.text();
        float bw = Txt.wReg(value, 6.5f) + 8, bh = 6.5f + 3;
        float bx = x + w - bw, by = y + h / 2f - bh / 2f + 0.5f;
        label(v.name, x, y, h, bx - x - 4, WHITE);
        fill(bx, by, bw, bh, 2, ca(accent, 0.039f + 0.2f * cap));
        border(bx, by, bw, bh, 2, 0.5f, mix(0xFF282828, accent, cap));
        tr(value, bx + 4, ty(by + bh / 2f, 6.5f), 6.5f, WHITE);
        addHit(x, y, w, h, () -> bindingSetting = v, null, () -> v.set(KeyBind.NONE));
    }

    private void drawButton(Button b, float x, float y, float w, float h) {
        boolean over = hov(x, y, w, h);
        float hv = ap(b, BTN, over ? 1f : 0f, 14f);
        fill(x, y, w, h, 4, ca(accent, (10f + 20f * hv) / 255f));
        border(x, y, w, h, 4, 0.5f, 0xFF282828);
        tb(b.name, x + (w - Txt.wBold(b.name, 7)) / 2f, ty(y + h / 2f, 7), 7, WHITE);
        addHit(x, y, w, h, b::press, null, null);
    }

    private void drawColor(ColorValue v, float x, float y, float w, float h) {
        float cy = y + h / 2f;
        float bx = x + w - 11, by = cy - 5.5f;
        label(v.name, x, y, h, bx - x - 4, WHITE);
        fill(bx, by, 11, 11, 2, ca(accent, 0.039f));
        border(bx, by, 11, 11, 2, 0.5f, paletteOf == v ? accent : 0xFF282828);
        fill(bx + 2, by + 2, 7, 7, 1.5f, 0xFF000000 | (v.value.getRGB() & 0xFFFFFF));
        final float ax = x + w;
        addHit(x, y, w, h, () -> togglePalette(v, ax, by), null, null);
    }

    // ---------------------------------------------------------------- color palette popup

    private void togglePalette(ColorValue v, float anchorRight, float anchorY) {
        if (paletteOf == v) { closePalette(); return; }
        paletteOf = v;
        float[] hsb = Color.RGBtoHSB(v.value.getRed(), v.value.getGreen(), v.value.getBlue(), null);
        palH = hsb[0]; palS = hsb[1]; palB = hsb[2]; palA = v.value.getAlpha() / 255f;
        palX = anchorRight + 6;
        palY = anchorY;
        palDrag = Drag.NONE;
    }

    private void closePalette() { paletteOf = null; palDrag = Drag.NONE; }

    private float[] palArea() { return new float[]{palX + 5, palY + 5, 56, 56}; }
    private float[] palHue() { return new float[]{palX + 5 + 56 + 5, palY + 5, 4, 56}; }
    private float[] palAlpha() { return new float[]{palX + 5 + 56 + 5 + 4 + 5, palY + 5, 4, 56}; }

    private void applyPalette() {
        if (paletteOf == null) return;
        int rgb = Color.HSBtoRGB(palH, palS, palB) & 0xFFFFFF;
        paletteOf.set(new Color(((Math.round(palA * 255f) & 0xFF) << 24) | rgb, true));
    }

    private void updatePaletteDrag(float mouseX, float mouseY) {
        float[] a = palArea(), h = palHue(), al = palAlpha();
        if (palDrag == Drag.AREA) {
            palS = clamp01((mouseX - a[0]) / a[2]);
            palB = 1f - clamp01((mouseY - a[1]) / a[3]);
        } else if (palDrag == Drag.HUE) {
            palH = clamp01((mouseY - h[1]) / h[3]);
        } else if (palDrag == Drag.ALPHA) {
            palA = 1f - clamp01((mouseY - al[1]) / al[3]);
        } else return;
        applyPalette();
    }

    private void drawPalette() {
        float open = ap("palette", FOCUS, paletteOf != null ? 1f : 0f, 16f);
        if (open <= 0.01f) return;
        if (paletteOf == null) return;
        float a0 = alpha;
        alpha = ga * open;
        float w = 84, h = 66;
        float px = Math.min(palX, dw - w - 4), py = Math.min(palY, dh - h - 4);
        palX = px; palY = py;
        fill(px, py, w, h, 4, ca(0xFF000000, 0.65f));
        border(px, py, w, h, 4, 0.5f, 0xFF282828);
        float[] a = palArea(), hu = palHue(), al = palAlpha();
        int hueColor = 0xFF000000 | (Color.HSBtoRGB(palH, 1f, 1f) & 0xFFFFFF);
        // saturation/brightness square: white to hue horizontally, transparent to black vertically
        gradient(a[0], a[1], a[2], a[3], WHITE, hueColor, hueColor, WHITE);
        gradient(a[0], a[1], a[2], a[3], 0x00000000, 0x00000000, 0xFF000000, 0xFF000000);
        border(a[0], a[1], a[2], a[3], 2, 0.5f, 0xFF232323);
        float cx = Math.max(a[0] + 2, Math.min(a[0] + a[2] - 2, a[0] + palS * a[2]));
        float cy = Math.max(a[1] + 2, Math.min(a[1] + a[3] - 2, a[1] + (1f - palB) * a[3]));
        border(cx - 2, cy - 2, 4, 4, 2, 0.5f, WHITE);
        // hue bar: six segments between the primary colors
        int[] stops = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
        for (int i = 0; i < 6; i++) {
            gradient(hu[0], hu[1] + hu[3] * i / 6f, hu[2], hu[3] / 6f, stops[i], stops[i], stops[i + 1], stops[i + 1]);
        }
        fill(hu[0] - 1, hu[1] + palH * hu[3] - 0.5f, hu[2] + 2, 1, 0, WHITE);
        // alpha bar: the color fading out downward
        int rgb = 0xFF000000 | (Color.HSBtoRGB(palH, palS, palB) & 0xFFFFFF);
        fill(al[0], al[1], al[2], al[3], 1, 0xFF303034);
        gradient(al[0], al[1], al[2], al[3], rgb, rgb, rgb & 0x00FFFFFF, rgb & 0x00FFFFFF);
        fill(al[0] - 1, al[1] + (1f - palA) * al[3] - 0.5f, al[2] + 2, 1, 0, WHITE);
        // the palette is on top of everything: it gets its own hit areas, registered last
        hits.add(new Hit(px, py, w, h, null, null, null));
        hits.add(new Hit(a[0], a[1], a[2], a[3], () -> { palDrag = Drag.AREA; updatePaletteDrag(clickX, clickY); }, null, null));
        hits.add(new Hit(hu[0] - 1, hu[1], hu[2] + 2, hu[3], () -> { palDrag = Drag.HUE; updatePaletteDrag(clickX, clickY); }, null, null));
        hits.add(new Hit(al[0] - 1, al[1], al[2] + 2, al[3], () -> { palDrag = Drag.ALPHA; updatePaletteDrag(clickX, clickY); }, null, null));
        alpha = a0;
    }

    // ---------------------------------------------------------------- search

    private void drawSearch(float startX, float sy, float groupW) {
        float sx = startX + (groupW - SEARCH_W) / 2f;
        float fo = ap("search", FOCUS, searchFocus ? 1f : 0f, 14f);
        fill(sx, sy, SEARCH_W, SEARCH_H, 5, ca(PANEL, 1f));
        border(sx, sy, SEARCH_W, SEARCH_H, 5, 0.5f, mix(STROKE, accent, fo));
        boolean empty = query.isEmpty() && !searchFocus;
        String shown = query + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
        while (shown.length() > 1 && Txt.wReg(shown, 7) > SEARCH_W - 14) shown = shown.substring(1);
        tr(empty ? "Поиск модулей" : shown, sx + 7, ty(sy + SEARCH_H / 2f, 7), 7, empty ? ca(WHITE, 0.4f) : WHITE);
        if (!closing) hits.add(new Hit(sx, sy, SEARCH_W, SEARCH_H, () -> searchFocus = true, null, null));
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
        if (closing) return true;
        int button = click.button();
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
        float cx = (float) (click.x() / S), cy = (float) (click.y() / S);
        clickX = cx;
        clickY = cy;
        boolean wasEditing = editingText != null;
        if (wasEditing) { finishEdit(); }
        searchFocus = false;
        boolean paletteWasOpen = paletteOf != null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (cx >= h.x && cx < h.x + h.w && cy >= h.y && cy < h.y + h.h) {
                Runnable r = button == 1 ? h.right : button == 2 ? h.mid : h.left;
                if (r != null) r.run();
                return true;
            }
        }
        if (paletteWasOpen) closePalette();
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        float x = (float) (click.x() / S), y = (float) (click.y() / S);
        if (palDrag != Drag.NONE) { updatePaletteDrag(x, y); return true; }
        if (dragSlider != null) { setSliderFromMouse(dragSlider, dragSliderX, dragSliderW, x); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragSlider = null;
        palDrag = Drag.NONE;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        float x = (float) (mx / S), y = (float) (my / S);
        for (Panel p : panels) {
            if (x >= p.x && x < p.x + COL_W && y >= p.y && y < p.y + COL_H) {
                p.scrollT = Math.max(0f, Math.min(Math.max(0f, p.content - p.view), p.scrollT - (float) vAmount * 15f));
                closePalette();
                return true;
            }
        }
        return super.mouseScrolled(mx, my, hAmount, vAmount);
    }

    private void applyEdit() { if (editingText != null) editingText.set(editBuffer); }

    private void finishEdit() { applyEdit(); editingText = null; editBuffer = ""; }

    @Override
    public boolean charTyped(CharInput input) {
        String chr = input.asString();
        boolean printable = !chr.isEmpty() && chr.codePointAt(0) >= 32;
        if (editingText != null && printable) {
            if (editBuffer.length() < 160) editBuffer += chr;
            applyEdit();
            return true;
        }
        if (searchFocus && printable && query.length() < 24) {
            query += chr;
            for (Panel p : panels) p.scroll = p.scrollT = 0;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        boolean cancel = keyCode == 256 || keyCode == 259 || keyCode == 261;
        if (bindingSetting != null) {
            bindingSetting.set(cancel ? KeyBind.NONE : KeyBind.keyboard(keyCode));
            bindingSetting = null;
            return true;
        }
        if (binding != null) {
            binding.setBind(cancel ? KeyBind.NONE : KeyBind.keyboard(keyCode));
            binding = null;
            return true;
        }
        if (editingText != null) {
            if (keyCode == 257 || keyCode == 335) { finishEdit(); return true; }
            if (keyCode == 256) { editingText = null; editBuffer = ""; return true; }
            if ((keyCode == 259 || keyCode == 261) && !editBuffer.isEmpty()) {
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                applyEdit();
            }
            return true;
        }
        if (keyCode == 70 && (input.modifiers() & 2) != 0) { searchFocus = !searchFocus; return true; }
        if (searchFocus) {
            if (keyCode == 259 && !query.isEmpty()) { query = query.substring(0, query.length() - 1); return true; }
            if (keyCode == 256) { searchFocus = false; return true; }
        }
        return super.keyPressed(input);
    }
}
