package daamky.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import daamky.ua.api.config.ConfigManager;
import daamky.ua.api.settings.bind.KeyBind;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * "DropDown" ClickGUI style: one draggable column per category, modules expand in place.
 * Everything is read from and written to the real module settings through {@link Modules},
 * so it behaves exactly like the Window style.
 */
public class DaamkyDropDownGui extends Screen {

    // ---- layout (design units, GUI-scale independent like the Window style) ----
    static final float REF_GUI_SCALE = 2f;
    static final float PW = 128f, GAP = 10f, HEAD_H = 24f, ROW_H = 20f, ROW_GAP = 3f, PAD = 4f, TOP = 36f;
    static final float SUB_PX = 6f, SUB_PT = 5f, STEP = 5f, OPT_H = 14f;
    static final float IW = PW - PAD * 2f - 4f - SUB_PX * 2f;
    static final float CFG_HEAD = 92f, CFG_ROW = 21f;

    static final int PANEL_BG = 0xFF080E18, PANEL_BORDER = 0xFF24364F, ROW_OFF = 0xFF0D1522, ROW_OFF_BORDER = 0xFF263B57;
    static final int SUB_BG = 0xFF0A121F, SUB_BORDER = 0xFF1E2D43, FIELD = 0xFF101B2A, FIELD_BORDER = 0xFF263A55;
    static final int TOG_OFF = 0xFF36506D, KNOB = 0xFFF5FAFF, THUMB = 0xFF587DA8, LINE = 0xFF20314A;
    static final int WHITE = 0xFFFFFFFF, DIM = 0xFF8394AA, ACCENT_TXT = 0xFF79B4FF, OK_TXT = 0xFF72DCA4, ERR_TXT = 0xFFFF7C8C;
    static final float OPEN_TIME = 0.5f, CLOSE_TIME = 0.2f;

    private static final int CAT_CONFIGS = 5;

    private record Cat(String label, int icon, int id) {}
    private static final Cat[] CATS = {
            new Cat("Combat", Icons.COMBAT, 0), new Cat("Movement", Icons.MOVEMENT, 1), new Cat("Render", Icons.RENDER, 2),
            new Cat("Player", Icons.PLAYER, 3), new Cat("Util", Icons.UTIL, 4), new Cat("Configs", Icons.CONFIGS, CAT_CONFIGS),
            new Cat("Themes", Icons.THEMES, 6), new Cat("AutoBuy", Icons.ITEMS, 7)};

    private record Row(Setting s, int depth) {}
    private record K(Object o, int kind) {}
    private static final int EN = 0, HOV = 1, TOG = 2, EXP = 3, DD = 4, SEL = 5, SL = 6, COL = 7, POP = 8;

    private record Hit(float x, float y, float w, float h, Runnable left, Runnable right, Runnable mid) {}

    // ---- state that survives closing the screen ----
    private static final class PanelState { float x, y; boolean collapsed, placed; }
    private static final Map<Integer, PanelState> STATES = new HashMap<>();
    private static final List<Integer> ORDER = new ArrayList<>();
    private static final Set<String> EXPANDED = new HashSet<>();
    private static boolean loaded;

    private static final class Panel {
        final Cat cat; final PanelState st;
        float scroll, scrollT, maxScroll, contentH, bodyH, h, col = 1f;
        List<Mod> mods = List.of();
        boolean visible;
        Panel(Cat cat, PanelState st) { this.cat = cat; this.st = st; }
    }

    private final List<Panel> panels = new ArrayList<>();
    private List<Panel> frameOrder = List.of();
    private final List<Hit> hits = new ArrayList<>();
    private final Map<K, Float> anim = new HashMap<>();

    private Mod binding;
    private BindValue bindingSetting;
    private TextValue editingText;
    private ColorValue editingColor;
    private String editBuffer = "";
    private String query = "";
    private boolean searchFocus, cfgFocus, dblClick;

    private Panel dragPanel;
    private Slider dragSlider;
    private float dragSliderX, dragSliderW, dragDX, dragDY, cx, cy;

    private Mod tip, lastTip;
    private float tipT;

    private float S = 1f, dw, dh, mlx, mly, curDy;
    private Matrix3x2f m = new Matrix3x2f();
    private DrawContext ctx;
    private long lastNs;
    private float dt, time, openT, alpha = 1f, ga = 1f;
    private boolean closing, covered;

    public DaamkyDropDownGui() {
        super(Text.literal("Daamky"));
        loadStates();
        for (Cat c : CATS) panels.add(new Panel(c, STATES.computeIfAbsent(c.id(), k -> new PanelState())));
        ConfigsBridge.refresh();
    }

    @Override public boolean shouldPause() { return false; }

    // =========================================================== persistence

    private static Path stateFile() { return ConfigManager.systemDirectory().resolve("dropdown.daamky"); }

    private static void loadStates() {
        if (loaded) return;
        loaded = true;
        try {
            Path file = stateFile();
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("panels")) {
                        JsonObject ps = root.getAsJsonObject("panels");
                        for (String key : ps.keySet()) {
                            JsonObject o = ps.getAsJsonObject(key);
                            PanelState st = new PanelState();
                            st.x = o.get("x").getAsFloat();
                            st.y = o.get("y").getAsFloat();
                            st.collapsed = o.has("collapsed") && o.get("collapsed").getAsBoolean();
                            st.placed = true;
                            STATES.put(Integer.parseInt(key), st);
                        }
                    }
                    if (root.has("order")) for (JsonElement e : root.getAsJsonArray("order")) ORDER.add(e.getAsInt());
                    if (root.has("expanded")) for (JsonElement e : root.getAsJsonArray("expanded")) EXPANDED.add(e.getAsString());
                }
            }
        } catch (Exception ignored) {
            STATES.clear();
            ORDER.clear();
            EXPANDED.clear();
        }
        for (Cat c : CATS) if (!ORDER.contains(c.id())) ORDER.add(c.id());
    }

    private static void saveStates() {
        try {
            JsonObject root = new JsonObject();
            JsonObject ps = new JsonObject();
            for (Map.Entry<Integer, PanelState> e : STATES.entrySet()) {
                PanelState st = e.getValue();
                if (!st.placed) continue;
                JsonObject o = new JsonObject();
                o.addProperty("x", st.x);
                o.addProperty("y", st.y);
                o.addProperty("collapsed", st.collapsed);
                ps.add(String.valueOf(e.getKey()), o);
            }
            root.add("panels", ps);
            JsonArray order = new JsonArray();
            for (int id : ORDER) order.add(id);
            root.add("order", order);
            JsonArray expanded = new JsonArray();
            for (String name : EXPANDED) expanded.add(name);
            root.add("expanded", expanded);
            Files.createDirectories(ConfigManager.systemDirectory());
            try (Writer writer = Files.newBufferedWriter(stateFile(), StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
            // Panel layout is a convenience; failing to save it must not break the GUI.
        }
    }

    // =========================================================== math / animation helpers

    private static float clamp01(float v) { return DaamkyClickGui.clamp01(v); }
    private static float easeOutCubic(float t) { return DaamkyClickGui.easeOutCubic(t); }
    private static int mix(int a, int b, float t) { return DaamkyClickGui.mix(a, b, t); }
    private static float ty(float center, float size) { return center - size * 0.58f; }

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

    private int A(int c) {
        int a = Math.round(((c >>> 24) & 0xFF) * alpha);
        return (a << 24) | (c & 0xFFFFFF);
    }

    private void fill(float x, float y, float w, float h, float r, int c) { SdfRect.fill(m, x, y, w, h, r, A(c)); }
    private void border(float x, float y, float w, float h, float r, float t, int c) { SdfRect.border(m, x, y, w, h, r, t, A(c)); }
    private void grad(float x, float y, float w, float h, float r) { SdfRect.flow(m, x, y, w, h, r, alpha); }
    private void gradA(float x, float y, float w, float h, float r, float a) { if (a > 0.003f) SdfRect.flow(m, x, y, w, h, r, alpha * a); }
    private void gglow(float x, float y, float w, float h, float r, float spread, int a) {
        if (daamky.render.Theme.glow) SdfRect.flowGlow(m, x, y, w, h, r, spread, Math.round(a * alpha));
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

    private void chevron(float cxp, float cyp, float turn, int c) {
        Matrix3x2f saved = m;
        m = new Matrix3x2f(saved).translate(cxp, cyp).rotate((float) Math.PI * turn).translate(-cxp, -cyp);
        ico(Icons.CHEVRON, cxp - 3.25f, cyp - 3.25f, 6.5f, c);
        m = saved;
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

    private static String fit(String s, float size, float maxW) {
        if (Txt.wBold(s, size) <= maxW) return s;
        String r = s;
        while (r.length() > 1 && Txt.wBold(r + "..", size) > maxW) r = r.substring(0, r.length() - 1);
        return r + "..";
    }

    private static String clipText(String value, float maxWidth, float size) {
        String result = value == null ? "" : value;
        while (!result.isEmpty() && Txt.wBold(result, size) > maxWidth) result = result.substring(1);
        return result;
    }

    // =========================================================== hit-testing / scissor

    private void addHit(float x, float y, float w, float h, Runnable l, Runnable r, Runnable mid) {
        if (!closing && w > 0f && h > 0f) hits.add(new Hit(x, y + curDy, w, h, l, r, mid));
    }

    private void hit(float x, float y, float w, float h, Runnable l, Runnable r) { addHit(x, y, w, h, l, r, null); }

    private void hitC(float x, float y, float w, float h, float ct, float cb, Runnable l, Runnable r) {
        float y0 = Math.max(y, ct), y1 = Math.min(y + h, cb);
        if (y1 > y0) hit(x, y0, w, y1 - y0, l, r);
    }

    private boolean hov(float x, float y, float w, float h) {
        float ly = mly - curDy;
        return !covered && !closing && mlx >= x && mlx < x + w && ly >= y && ly < y + h;
    }

    private boolean hovC(float x, float y, float w, float h, float ct, float cb) {
        float y0 = Math.max(y, ct), y1 = Math.min(y + h, cb);
        return y1 > y0 && hov(x, y0, w, y1 - y0);
    }

    private void scissor(float x1, float y1, float x2, float y2) {
        int l = (int) Math.floor(x1 * S), tp = (int) Math.floor((y1 + curDy) * S);
        int r = (int) Math.ceil(x2 * S), bt = (int) Math.ceil((y2 + curDy) * S);
        daamky.render.Gfx.scissor = new ScreenRect(l, tp, Math.max(0, r - l), Math.max(0, bt - tp));
    }

    private void endScissor() { daamky.render.Gfx.scissor = null; }

    // =========================================================== data helpers

    private List<Mod> modsFor(Cat cat) {
        List<Mod> out = new ArrayList<>();
        String q = query.toLowerCase(Locale.ROOT);
        for (Mod mod : Modules.ALL) {
            if (mod.category != cat.id()) continue;
            if (!q.isEmpty() && !mod.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(mod);
        }
        return out;
    }

    private static void collectRows(List<Setting> in, int depth, List<Row> out) {
        for (Setting s : in) {
            if (!s.isVisible()) continue;
            out.add(new Row(s, depth));
            if (s instanceof Group g && g.open) collectRows(g.children, depth + 1, out);
        }
    }

    private static List<Row> rowsOf(Mod mod) {
        List<Row> out = new ArrayList<>();
        collectRows(mod.settings, 0, out);
        return out;
    }

    private static boolean isExpanded(Mod mod) { return mod.hasSettings() && EXPANDED.contains(mod.name); }

    private void toggleExpand(Mod mod) {
        if (!mod.hasSettings()) return;
        if (!EXPANDED.remove(mod.name)) EXPANDED.add(mod.name);
        saveStates();
    }

    private float settingH(Row r, float iw) {
        Setting s = r.s();
        if (s instanceof Section || s instanceof Group) return 17f;
        if (s instanceof Button) return 20f;
        if (s instanceof Bool b) return Math.max(18f, 8f + wrap(b.name, 8.5f, iw - 30f).size() * 10f);
        if (s instanceof Slider) return 26f;
        if (s instanceof Mode md) {
            float dd = peek(md, DD, md.open ? 1f : 0f);
            return 29f + dd * (md.modes.length * OPT_H + 4f);
        }
        return 29f;
    }

    private float subHeight(Mod mod) {
        float h = SUB_PT * 2f;
        for (Row r : rowsOf(mod)) h += settingH(r, IW - r.depth() * STEP);
        return h;
    }

    private float subVisible(Mod mod) {
        boolean open = isExpanded(mod);
        float ex = peek(mod, EXP, open ? 1f : 0f);
        if (ex < 0.003f && !open) return 0f;
        return subHeight(mod) * ex;
    }

    private float blockH(Mod mod) {
        float sv = subVisible(mod);
        return ROW_H + ROW_GAP + (sv > 0.5f ? sv + ROW_GAP : 0f);
    }

    private float measure(Panel p) {
        if (p.cat.id() == CAT_CONFIGS) {
            int n = ConfigsBridge.list().size();
            return CFG_HEAD + (n == 0 ? 16f : n * CFG_ROW);
        }
        float h = 0f;
        for (Mod mod : p.mods) h += blockH(mod);
        return h;
    }

    /** Advances every expand / dropdown animation exactly once per frame, before anything is measured. */
    private void tickAnims(List<Panel> vis) {
        for (Panel p : vis) {
            for (Mod mod : p.mods) {
                boolean open = isExpanded(mod);
                float ex = ap(mod, EXP, open ? 1f : 0f, 12f);
                if (ex > 0.003f || open) {
                    for (Setting s : mod.flat()) if (s instanceof Mode md) ap(md, DD, md.open ? 1f : 0f, 13f);
                }
            }
        }
    }

    private void front(Panel p) {
        ORDER.remove(Integer.valueOf(p.cat.id()));
        ORDER.add(p.cat.id());
    }

    private void toggleCollapse(Panel p) {
        p.st.collapsed = !p.st.collapsed;
        saveStates();
    }

    private List<Panel> ordered(List<Panel> vis) {
        List<Panel> out = new ArrayList<>(vis);
        out.sort(Comparator.comparingInt(pp -> ORDER.indexOf(pp.cat.id())));
        return out;
    }

    private void updateCover(List<Panel> ordered, int idx) {
        covered = false;
        for (int j = idx + 1; j < ordered.size(); j++) {
            Panel q = ordered.get(j);
            if (mlx >= q.st.x && mlx < q.st.x + PW && mly >= q.st.y && mly < q.st.y + q.h) { covered = true; return; }
        }
    }

    // =========================================================== screen lifecycle

    @Override
    public void close() {
        saveStates();
        closing = true;
        dragPanel = null;
        dragSlider = null;
    }

    @Override
    public void tick() {
        super.tick();
        if (closing) {
            if (openT <= 0f) super.close();
        } else if (!GuiStyle.isDropDown()) {
            // The Type setting was switched back to Window.
            saveStates();
            if (client != null) client.setScreen(new DaamkyClickGui());
        }
    }

    // =========================================================== render

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        ctx = c;
        Modules.refreshFromTarget();
        daamky.render.Gfx.ctx = c;
        daamky.render.Gfx.scissor = null;
        MsdfText.msdfMode = Modules.useMsdf();
        MsdfText.baseWeight = Modules.weight();
        hits.clear();
        tip = null;

        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        time += dt;

        openT = clamp01(openT + (closing ? -dt / CLOSE_TIME : dt / OPEN_TIME));
        ga = clamp01(openT * 2.5f);

        if (daamky.render.Theme.blur) super.render(c, mx, my, delta);
        else c.fill(0, 0, width, height, ((int) (0x70 * ga) << 24));
        if (ga <= 0.003f) return;

        List<Panel> vis = new ArrayList<>();
        for (Panel p : panels) {
            boolean configs = p.cat.id() == CAT_CONFIGS;
            p.mods = configs ? List.of() : modsFor(p.cat);
            p.visible = configs ? query.isEmpty() : !p.mods.isEmpty();
            if (p.visible) vis.add(p);
        }
        int n = vis.size();
        float totalW = n * PW + Math.max(0, n - 1) * GAP;

        float guiScale = (float) client.getWindow().getScaleFactor();
        S = Math.max(0.3f, Math.min(REF_GUI_SCALE / guiScale, (width - 16f) / Math.max(totalW, PW)));
        dw = width / S;
        dh = height / S;
        mlx = mx / S;
        mly = my / S;

        float startX = Math.max(8f, (dw - totalW) / 2f);
        int idx = 0;
        for (Panel p : vis) {
            PanelState st = p.st;
            if (!st.placed) { st.x = startX + idx * (PW + GAP); st.y = TOP; st.placed = true; }
            idx++;
            st.x = Math.max(6f, Math.min(st.x, Math.max(6f, dw - PW - 6f)));
            st.y = Math.max(6f, Math.min(st.y, Math.max(6f, dh - HEAD_H - 6f)));
        }

        tickAnims(vis);
        for (Panel p : vis) {
            p.contentH = measure(p);
            float bodyMax = Math.max(48f, dh - p.st.y - HEAD_H - 10f);
            p.bodyH = Math.min(p.contentH + PAD * 2f, bodyMax);
            p.col = ap(p, COL, p.st.collapsed ? 0f : 1f, 12f);
            p.h = HEAD_H + p.bodyH * p.col;
            p.maxScroll = Math.max(0f, p.contentH + PAD * 2f - p.bodyH);
            p.scrollT = Math.max(0f, Math.min(p.maxScroll, p.scrollT));
            p.scroll = Math.max(0f, Math.min(p.maxScroll, lerpTo(p.scroll, p.scrollT, 18f)));
        }

        var ms = c.getMatrices();
        ms.pushMatrix();
        ms.scale(S, S);
        Matrix3x2f base = new Matrix3x2f(ms);
        m = base;
        alpha = ga;
        curDy = 0f;
        covered = false;
        drawSearch();
        drawHint();

        List<Panel> ordered = ordered(vis);
        frameOrder = ordered;
        for (int zi = 0; zi < ordered.size(); zi++) {
            Panel p = ordered.get(zi);
            int ai = vis.indexOf(p);
            float pt = clamp01((openT - ai * 0.035f) / 0.6f);
            alpha = ga * clamp01(pt * 2.2f);
            curDy = closing ? (1f - openT) * 8f : (1f - easeOutCubic(pt)) * 18f;
            m = new Matrix3x2f(base).translate(0f, curDy);
            updateCover(ordered, zi);
            drawPanel(p);
        }
        m = base;
        alpha = ga;
        curDy = 0f;
        covered = false;
        drawTooltip();
        endScissor();
        ms.popMatrix();
    }

    private void drawSearch() {
        float sw = 190f, sh = 20f, sx = (dw - sw) / 2f, sy = 8f;
        float fo = ap(this, EN, searchFocus ? 1f : 0f, 14f);
        fill(sx, sy, sw, sh, 10f, FIELD);
        border(sx, sy, sw, sh, 10f, 1f, mix(0xFF24364F, 0xFF4C96F7, fo));
        ico(Icons.SEARCH, sx + 8f, sy + 5f, 10f, WHITE);
        if (query.isEmpty() && !searchFocus) tr("Поиск", sx + 23f, ty(sy + sh / 2f, 9f), 9f, DIM);
        else tr(query + (searchFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : ""), sx + 23f, ty(sy + sh / 2f, 9f), 9f, WHITE);
        hit(sx, sy, sw, sh, () -> searchFocus = true, null);
    }

    private void drawHint() {
        String s = "ЛКМ — вкл/выкл   ·   ПКМ — настройки   ·   СКМ — бинд   ·   Right Shift — закрыть";
        float size = 7f, w = Txt.wBold(s, size);
        if (w > dw - 16f) return;
        tb(s, (dw - w) / 2f, dh - 14f, size, 0xFF5E6E84);
    }

    private void drawPanel(Panel p) {
        PanelState st = p.st;
        float x = st.x, y = st.y, h = p.h;

        hit(x, y, PW, h, () -> front(p), () -> front(p));

        gglow(x, y, PW, h, 9f, 10f, 0x40);
        fill(x, y, PW, h, 9f, PANEL_BG);
        border(x, y, PW, h, 9f, 1f, PANEL_BORDER);

        grad(x, y, PW, HEAD_H, 8f);
        ico(p.cat.icon(), x + 8f, y + (HEAD_H - 12f) / 2f, 12f, WHITE);
        float tw = Txt.wBold(p.cat.label(), 11f);
        tb(p.cat.label(), x + (PW - tw) / 2f, ty(y + HEAD_H / 2f, 11f), 11f, WHITE);
        chevron(x + PW - 13f, y + HEAD_H / 2f, p.col, WHITE);
        hit(x, y, PW - 24f, HEAD_H, () -> { front(p); dragPanel = p; dragDX = cx - st.x; dragDY = cy - st.y; }, () -> toggleCollapse(p));
        hit(x + PW - 24f, y, 24f, HEAD_H, () -> toggleCollapse(p), () -> toggleCollapse(p));

        float vt = y + HEAD_H, vb = y + h - 1f;
        if (vb - vt <= 3f || p.col <= 0.02f) return;

        scissor(x + 1f, vt, x + PW - 1f, vb);
        float yy = vt + PAD - p.scroll;
        if (p.cat.id() == CAT_CONFIGS) {
            drawConfigs(x, yy, vt, vb);
        } else {
            for (Mod mod : p.mods) yy = drawModule(mod, x, yy, vt, vb);
        }
        endScissor();

        int clear = 0x000A0A0C;
        if (p.scroll > 0.5f) vgrad(x + 1f, vt, PW - 2f, 12f, PANEL_BG, PANEL_BG, clear, clear);
        if (p.scroll < p.maxScroll - 0.5f) vgrad(x + 1f, vb - 12f, PW - 2f, 12f, clear, clear, PANEL_BG, PANEL_BG);
        if (p.maxScroll > 0f) {
            float trT = vt + 5f, trH = (vb - vt) - 10f, ch = p.contentH + PAD * 2f;
            float th = Math.max(20f, trH * (vb - vt) / ch), thY = trT + (trH - th) * (p.scroll / p.maxScroll);
            fill(x + PW - 5f, trT, 2.5f, trH, 1.25f, 0xFF16161A);
            fill(x + PW - 5f, thY, 2.5f, th, 1.25f, THUMB);
        }
    }

    // =========================================================== modules

    private float drawModule(Mod mod, float px, float y, float vt, float vb) {
        float x = px + PAD, w = PW - PAD * 2f;
        float en = ap(mod, EN, mod.enabled ? 1f : 0f, 11f);
        float ex = peek(mod, EXP, 0f);
        float sv = subVisible(mod);

        if (y + ROW_H >= vt && y <= vb) {
            float vy = Math.max(y, vt), vh = Math.min(y + ROW_H, vb) - vy;
            boolean hovered = vh > 0f && hov(x, vy, w, vh);
            if (hovered) tip = mod;
            float hv = ap(mod, HOV, hovered ? 1f : 0f, 16f);
            float pop = ap(mod, POP, 0f, 9f);

            Matrix3x2f saved = m;
            float ccx = x + w / 2f, ccy = y + ROW_H / 2f, sc = 1f - 0.04f * pop;
            if (pop > 0.002f) m = new Matrix3x2f(saved).translate(ccx, ccy).scale(sc, sc).translate(-ccx, -ccy);

            gglowA(x, y, w, ROW_H, 6f, 8f, 0x70, en);
            fill(x, y, w, ROW_H, 6f, ROW_OFF);
            gradA(x, y, w, ROW_H, 6f, en);
            border(x, y, w, ROW_H, 6f, 1f, mix(mix(ROW_OFF_BORDER, 0xFF4779B4, hv), 0x38FFFFFF, en));
            if (hv > 0.01f) fill(x, y, w, ROW_H, 6f, ((int) (0x0C * hv * (1f - en)) << 24) | 0xFFFFFF);

            float right = x + w - 6f;
            if (mod.hasSettings()) {
                chevron(x + w - 11f, y + ROW_H / 2f, ex, WHITE);
                right -= 13f;
            }
            if (binding == mod || mod.hasBind()) {
                String bt = binding == mod ? "[...]" : "[" + mod.keyDisplay() + "]";
                float bw = Txt.wBold(bt, 7.5f);
                tb(bt, right - bw, ty(y + ROW_H / 2f, 7.5f), 7.5f, binding == mod ? ACCENT_TXT : 0xFFE1EEFF);
                right -= bw + 5f;
            }
            tb(fit(mod.name, 9.5f, right - (x + 8f)), x + 8f, ty(y + ROW_H / 2f, 9.5f), 9.5f, WHITE);
            m = saved;

            if (vh > 0f) {
                addHit(x, vy, w, vh, () -> { mod.setEnabled(!mod.enabled); anim.put(new K(mod, POP), 1f); },
                        mod.hasSettings() ? () -> toggleExpand(mod) : null, () -> binding = mod);
                if (mod.hasSettings()) addHit(x + w - 20f, vy, 20f, vh, () -> toggleExpand(mod), () -> toggleExpand(mod), null);
            }
        }

        float next = y + ROW_H + ROW_GAP;
        if (sv > 0.5f) {
            drawSub(mod, px, next, sv, vt, vb);
            next += sv + ROW_GAP;
        }
        return next;
    }

    private void drawSub(Mod mod, float px, float sy, float sv, float vt, float vb) {
        float x = px + PAD, w = PW - PAD * 2f;
        float top = Math.max(sy, vt), bot = Math.min(sy + sv, vb);
        if (bot - top <= 0.5f) return;

        fill(x + 2f, sy, w - 4f, sv, 6f, SUB_BG);
        border(x + 2f, sy, w - 4f, sv, 6f, 1f, SUB_BORDER);
        gradA(x + 2f, sy + 5f, 2f, Math.max(0f, sv - 10f), 1f, 0.9f);

        scissor(px + 1f, top, px + PW - 1f, bot);
        float ix0 = x + 2f + SUB_PX;
        float ry = sy + SUB_PT;
        for (Row r : rowsOf(mod)) {
            float iw = IW - r.depth() * STEP;
            float rh = settingH(r, iw);
            if (ry + rh >= top && ry <= bot) drawSetting(mod, r.s(), ix0 + r.depth() * STEP, ry, iw, rh, top, bot);
            ry += rh;
        }
        scissor(px + 1f, vt, px + PW - 1f, vb);
    }

    private void drawSetting(Mod owner, Setting s, float x, float ry, float iw, float rh, float ct, float cb) {
        if (s instanceof Section sec) drawSection(sec, x, ry, iw, rh);
        else if (s instanceof Group g) drawGroup(g, x, ry, iw, rh, ct, cb);
        else if (s instanceof Button b) drawButton(b, x, ry, iw, ct, cb);
        else if (s instanceof Bool b) drawBool(b, x, ry, iw, rh, ct, cb);
        else if (s instanceof Slider sl) drawSlider(sl, x, ry, iw, ct, cb);
        else if (s instanceof Mode md) drawMode(owner, md, x, ry, iw, ct, cb);
        else if (s instanceof TextValue tv) drawTextValue(tv, x, ry, iw, ct, cb);
        else if (s instanceof ColorValue cv) drawColorValue(cv, x, ry, iw, ct, cb);
        else if (s instanceof BindValue bv) drawBindValue(bv, x, ry, iw, ct, cb);
    }

    private void drawSection(Section sec, float x, float ry, float iw, float rh) {
        float c0 = ry + rh / 2f, w = Txt.wBold(sec.name, 8.5f);
        tb(sec.name, x, ty(c0, 8.5f), 8.5f, ACCENT_TXT);
        fill(x + w + 6f, c0, Math.max(0f, iw - w - 6f), 1f, 0f, LINE);
    }

    private void drawGroup(Group g, float x, float ry, float iw, float rh, float ct, float cb) {
        String label = g instanceof Multi mu ? g.name + " (" + mu.selectedCount() + ")" : g.name;
        float c0 = ry + rh / 2f, lw = Txt.wBold(label, 8.5f);
        float hv = ap(g, HOV, hovC(x - 2f, ry, iw + 4f, rh, ct, cb) ? 1f : 0f, 16f);
        tb(label, x, ty(c0, 8.5f), 8.5f, mix(ACCENT_TXT, WHITE, hv * 0.5f));
        fill(x + lw + 6f, c0, Math.max(0f, iw - lw - 6f - 12f), 1f, 0f, LINE);
        chevron(x + iw - 4f, c0, ap(g, TOG, g.open ? 1f : 0f, 14f), WHITE);
        hitC(x - 2f, ry, iw + 4f, rh, ct, cb, () -> g.open = !g.open, null);
    }

    private void drawButton(Button b, float x, float ry, float iw, float ct, float cb) {
        float bh = 15f, by = ry + 2.5f;
        float hv = ap(b, HOV, hovC(x, by, iw, bh, ct, cb) ? 1f : 0f, 16f);
        float pr = ap(b, POP, 0f, 10f);
        fill(x, by, iw, bh, 7f, FIELD);
        gradA(x, by, iw, bh, 7f, 0.35f + 0.4f * hv + 0.25f * pr);
        border(x, by, iw, bh, 7f, 1f, mix(FIELD_BORDER, 0x38FFFFFF, hv));
        float tw = Txt.wBold(b.name, 8.5f);
        tb(b.name, x + (iw - tw) / 2f, ty(by + bh / 2f, 8.5f), 8.5f, WHITE);
        hitC(x, by, iw, bh, ct, cb, () -> { b.press(); anim.put(new K(b, POP), 1f); }, null);
    }

    private void drawBool(Bool b, float x, float ry, float iw, float rh, float ct, float cb) {
        List<String> lines = wrap(b.name, 8.5f, iw - 30f);
        float c0 = ry + 9f;
        for (int i = 0; i < lines.size(); i++) tb(lines.get(i), x, ty(c0 + i * 10f, 8.5f), 8.5f, WHITE);
        float tw = 22f, th = 11f, tx = x + iw - tw, tyy = ry + rh / 2f - th / 2f, kn = th - 3f;
        float t = ap(b, TOG, b.value ? 1f : 0f, 14f);
        float kw = kn + (float) Math.sin(Math.PI * t) * 3f;
        fill(tx, tyy, tw, th, 5.5f, TOG_OFF);
        gradA(tx, tyy, tw, th, 5.5f, t);
        fill(tx + 1.5f + t * (tw - 3f - kw), tyy + 1.5f, kw, kn, kn / 2f, KNOB);
        hitC(x - 2f, ry, iw + 4f, rh, ct, cb, () -> b.set(!b.value), null);
    }

    private void drawSlider(Slider sl, float x, float ry, float iw, float ct, float cb) {
        tb(sl.name, x, ty(ry + 7f, 8.5f), 8.5f, WHITE);
        String val = sl.text();
        tb(val, x + iw - Txt.wBold(val, 8f), ty(ry + 7f, 8f), 8f, ACCENT_TXT);

        float trY = ry + 18f;
        boolean dragging = dragSlider == sl;
        boolean over = hovC(x - 4f, ry + 10f, iw + 8f, 16f, ct, cb);
        float shown = ap(sl, SL, sl.frac(), dragging ? 40f : 16f);
        float hv = ap(sl, HOV, dragging || over ? 1f : 0f, 16f);
        fill(x, trY - 2f, iw, 4f, 2f, 0xFF1A2A40);
        grad(x, trY - 2f, Math.max(4f, iw * shown), 4f, 2f);
        float kr = 3.5f + 1.5f * hv, kx = x + iw * shown;
        gglowA(kx - kr, trY - kr, kr * 2f, kr * 2f, kr, 5f, 0x60, hv);
        fill(kx - kr, trY - kr, kr * 2f, kr * 2f, kr, KNOB);

        final float fx = x, fw = iw;
        hitC(x - 4f, ry + 10f, iw + 8f, 16f, ct, cb, () -> {
            dragSlider = sl; dragSliderX = fx; dragSliderW = fw;
            sl.set(sl.min + clamp01((cx - fx) / fw) * (sl.max - sl.min));
        }, null);
    }

    private void drawMode(Mod owner, Mode md, float x, float ry, float iw, float ct, float cb) {
        tb(md.name, x, ty(ry + 6f, 8f), 8f, DIM);
        float by = ry + 12f, bh = 15f;
        float dd = peek(md, DD, md.open ? 1f : 0f);
        int n = md.modes.length;
        float listH = dd * (n * OPT_H + 4f);

        fill(x, by, iw, bh + listH, 6f, 0xFF0C1624);
        border(x, by, iw, bh + listH, 6f, 1f, FIELD_BORDER);
        gradA(x, by, iw, bh, 6f, 0.55f + 0.45f * dd);
        tb(fit(md.get(), 8.5f, iw - 22f), x + 7f, ty(by + bh / 2f, 8.5f), 8.5f, WHITE);
        chevron(x + iw - 8f, by + bh / 2f, dd, WHITE);
        hitC(x, by, iw, bh, ct, cb, () -> {
            if (!md.open) for (Setting o : owner.flat()) if (o instanceof Mode om && om != md) om.open = false;
            md.open = !md.open;
        }, null);

        if (listH < 1f) return;
        float listTop = by + bh + 2f;
        float sa = ap(md, SEL, md.index, 16f);
        float pillA = clamp01((listH - 2f - sa * OPT_H) / OPT_H);
        gradA(x + 3f, listTop + sa * OPT_H, iw - 6f, OPT_H - 2f, 5f, pillA);
        float baseA = alpha;
        for (int i = 0; i < n; i++) {
            float oy = listTop + i * OPT_H;
            float oa = clamp01((listH - 2f - i * OPT_H) / OPT_H);
            if (oa <= 0.01f) break;
            alpha = baseA * oa;
            boolean sel = i == md.index;
            if (!sel && md.open && hovC(x + 3f, oy, iw - 6f, OPT_H - 2f, ct, cb)) fill(x + 3f, oy, iw - 6f, OPT_H - 2f, 5f, 0x1CFFFFFF);
            tb(fit(md.modes[i], 8.5f, iw - 14f), x + 7f, ty(oy + (OPT_H - 2f) / 2f, 8.5f), 8.5f, sel ? WHITE : 0xFFA8A8B2);
            alpha = baseA;
            if (md.open && dd > 0.6f) {
                final int pick = i;
                hitC(x + 3f, oy, iw - 6f, OPT_H - 2f, ct, cb, () -> { md.select(pick); md.open = false; }, null);
            }
        }
    }

    private void fieldBox(float x, float by, float iw, float bh, boolean active) {
        fill(x, by, iw, bh, 6f, FIELD);
        border(x, by, iw, bh, 6f, 1f, active ? ACCENT_TXT : FIELD_BORDER);
    }

    private void drawTextValue(TextValue v, float x, float ry, float iw, float ct, float cb) {
        tb(v.name, x, ty(ry + 6f, 8f), 8f, DIM);
        float by = ry + 12f, bh = 15f;
        boolean ed = editingText == v;
        String shown = ed ? editBuffer + "|" : v.text();
        if (shown.isEmpty()) shown = "—";
        fieldBox(x, by, iw, bh, ed);
        tb(clipText(shown, iw - 10f, 8f), x + 5f, ty(by + bh / 2f, 8f), 8f, ed ? ACCENT_TXT : 0xFFB9C8DA);
        hitC(x, by, iw, bh, ct, cb, () -> { editingText = v; editingColor = null; editBuffer = v.text(); }, null);
    }

    private void drawColorValue(ColorValue v, float x, float ry, float iw, float ct, float cb) {
        tb(v.name, x, ty(ry + 6f, 8f), 8f, DIM);
        float by = ry + 12f, bh = 15f;
        boolean ed = editingColor == v;
        String shown = ed ? editBuffer + "|" : v.text();
        fieldBox(x, by, iw, bh, ed);
        fill(x + 3f, by + 3f, 10f, bh - 6f, 3f, v.value.getRGB());
        tb(clipText(shown, iw - 22f, 8f), x + 17f, ty(by + bh / 2f, 8f), 8f, ed ? ACCENT_TXT : 0xFFB9C8DA);
        hitC(x, by, iw, bh, ct, cb, () -> { editingColor = v; editingText = null; editBuffer = v.text(); }, null);
    }

    private void drawBindValue(BindValue v, float x, float ry, float iw, float ct, float cb) {
        tb(v.name, x, ty(ry + 6f, 8f), 8f, DIM);
        float by = ry + 12f, bh = 15f;
        boolean waiting = bindingSetting == v;
        String shown = waiting ? "PRESS KEY" : v.text();
        fieldBox(x, by, iw, bh, waiting);
        float tw = Txt.wBold(shown, 8f);
        tb(shown, x + (iw - tw) / 2f, ty(by + bh / 2f, 8f), 8f, waiting ? ACCENT_TXT : 0xFFB9C8DA);
        hitC(x, by, iw, bh, ct, cb, () -> { bindingSetting = v; editingText = null; editingColor = null; },
                () -> v.set(KeyBind.NONE));
    }

    // =========================================================== configs panel

    private void miniButton(String label, float x, float y, float w, float h, boolean enabled, boolean primary,
                            float ct, float cb, Runnable action) {
        String key = "dd:" + label;
        float hv = ap(key, HOV, enabled && hovC(x, y, w, h, ct, cb) ? 1f : 0f, 16f);
        float pr = ap(key, POP, 0f, 10f);
        fill(x, y, w, h, 7f, FIELD);
        if (enabled) gradA(x, y, w, h, 7f, primary ? 0.8f + 0.2f * hv : 0.25f + 0.4f * hv + 0.3f * pr);
        border(x, y, w, h, 7f, 1f, mix(FIELD_BORDER, 0x38FFFFFF, hv));
        float tw = Txt.wBold(label, 8.5f);
        tb(label, x + (w - tw) / 2f, ty(y + h / 2f, 8.5f), 8.5f, enabled ? WHITE : 0xFF5E6E84);
        if (enabled) hitC(x, y, w, h, ct, cb, () -> { action.run(); anim.put(new K(key, POP), 1f); }, null);
    }

    private void drawConfigs(float px, float y0, float vt, float vb) {
        float x = px + PAD, w = PW - PAD * 2f;
        List<String> list = ConfigsBridge.list();
        boolean hasSel = ConfigsBridge.selected != null;

        float fo = ap("dd:cfgfield", EN, cfgFocus ? 1f : 0f, 14f);
        fill(x, y0, w, 18f, 6f, FIELD);
        border(x, y0, w, 18f, 6f, 1f, mix(FIELD_BORDER, ACCENT_TXT, fo));
        String typed = ConfigsBridge.name;
        if (typed.isEmpty() && !cfgFocus) {
            tr("Имя конфига...", x + 7f, ty(y0 + 9f, 8.5f), 8.5f, DIM);
        } else {
            String shown = typed + (cfgFocus && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : "");
            tb(clipText(shown, w - 14f, 8.5f), x + 7f, ty(y0 + 9f, 8.5f), 8.5f, WHITE);
        }
        hitC(x, y0, w, 18f, vt, vb, () -> cfgFocus = true, null);

        float bw = (w - 4f) / 2f;
        miniButton("Сохранить", x, y0 + 21f, bw, 17f, true, true, vt, vb, ConfigsBridge::save);
        miniButton("Загрузить", x + bw + 4f, y0 + 21f, bw, 17f, hasSel, false, vt, vb, ConfigsBridge::loadSelected);
        miniButton("Удалить", x, y0 + 41f, bw, 17f, hasSel, false, vt, vb, ConfigsBridge::deleteSelected);
        miniButton("Папка", x + bw + 4f, y0 + 41f, bw, 17f, true, false, vt, vb, ConfigsBridge::openFolder);

        String status = ConfigsBridge.status();
        if (!status.isEmpty()) {
            int kind = ConfigsBridge.statusKind();
            int col = kind == ConfigsBridge.OK ? OK_TXT : kind == ConfigsBridge.ERROR ? ERR_TXT : ACCENT_TXT;
            tb(fit(status, 7.5f, w - 4f), x + 2f, ty(y0 + 67f, 7.5f), 7.5f, col);
        }

        String label = "Файлы (" + list.size() + ")";
        float lw = Txt.wBold(label, 8.5f);
        tb(label, x + 2f, ty(y0 + 83f, 8.5f), 8.5f, ACCENT_TXT);
        fill(x + lw + 8f, y0 + 83f, Math.max(0f, w - lw - 10f), 1f, 0f, LINE);

        float ry = y0 + CFG_HEAD;
        if (list.isEmpty()) {
            tr("Пока пусто", x + 2f, ty(ry + 8f, 8.5f), 8.5f, DIM);
            return;
        }
        for (String name : list) {
            if (ry + 18f >= vt && ry <= vb) drawConfigRow(name, x, ry, w, vt, vb);
            ry += CFG_ROW;
        }
    }

    private void drawConfigRow(String name, float x, float ry, float w, float vt, float vb) {
        boolean sel = name.equals(ConfigsBridge.selected);
        String key = "dd:row:" + name;
        float en = ap(key, EN, sel ? 1f : 0f, 11f);
        float hv = ap(key, HOV, hovC(x, ry, w, 18f, vt, vb) ? 1f : 0f, 16f);
        gglowA(x, ry, w, 18f, 6f, 7f, 0x60, en);
        fill(x, ry, w, 18f, 6f, ROW_OFF);
        gradA(x, ry, w, 18f, 6f, en);
        border(x, ry, w, 18f, 6f, 1f, mix(mix(ROW_OFF_BORDER, 0xFF4779B4, hv), 0x38FFFFFF, en));
        tb(fit(name, 9f, w - 14f), x + 7f, ty(ry + 9f, 9f), 9f, WHITE);
        hitC(x, ry, w, 18f, vt, vb, () -> {
            ConfigsBridge.selected = name;
            if (dblClick) ConfigsBridge.loadSelected();
        }, () -> ConfigsBridge.selected = name);
    }

    // =========================================================== tooltip

    private void drawTooltip() {
        if (tip != lastTip) { lastTip = tip; tipT = 0f; }
        else if (tip != null) tipT += dt;
        if (tip == null || dragPanel != null || closing || tipT < 0.35f) return;
        String desc = tip.desc;
        if (desc == null || desc.isBlank()) return;

        float a = clamp01((tipT - 0.35f) * 6f);
        List<String> lines = wrap(desc, 8f, 150f);
        float tw = 0f;
        for (String l : lines) tw = Math.max(tw, Txt.wBold(l, 8f));
        float w = tw + 16f, h = lines.size() * 10f + 8f;
        float x = mlx + 12f, y = mly + 14f;
        if (x + w > dw - 6f) x = mlx - w - 8f;
        if (y + h > dh - 6f) y = dh - 6f - h;
        x = Math.max(6f, x);
        y = Math.max(6f, y);

        alpha = ga * a;
        gglow(x, y, w, h, 6f, 7f, 0x40);
        fill(x, y, w, h, 6f, 0xF20A121F);
        border(x, y, w, h, 6f, 1f, PANEL_BORDER);
        gradA(x + 3f, y + 4f, 2f, h - 8f, 1f, 1f);
        for (int i = 0; i < lines.size(); i++) tb(lines.get(i), x + 10f, ty(y + 9f + i * 10f, 8f), 8f, 0xFFD7E5F6);
        alpha = ga;
    }

    // =========================================================== input

    @Override
    public boolean mouseClicked(Click click, boolean dbl) {
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
        cx = (float) (click.x() / S);
        cy = (float) (click.y() / S);
        dblClick = dbl;
        boolean wasEditing = editingText != null || editingColor != null;
        searchFocus = false;
        cfgFocus = false;
        if (wasEditing) finishEdit();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (cx >= h.x && cx < h.x + h.w && cy >= h.y && cy < h.y + h.h) {
                Runnable r = button == 1 ? h.right : button == 2 ? h.mid : h.left;
                if (r != null) r.run();
                return true;
            }
        }
        return super.mouseClicked(click, dbl);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragSlider != null) {
            float lx = (float) (click.x() / S);
            dragSlider.set(dragSlider.min + clamp01((lx - dragSliderX) / dragSliderW) * (dragSlider.max - dragSlider.min));
            return true;
        }
        if (dragPanel != null) {
            dragPanel.st.x = (float) (click.x() / S) - dragDX;
            dragPanel.st.y = (float) (click.y() / S) - dragDY;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragPanel != null) saveStates();
        dragPanel = null;
        dragSlider = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        float lx = (float) (mx / S), ly = (float) (my / S);
        for (int i = frameOrder.size() - 1; i >= 0; i--) {
            Panel p = frameOrder.get(i);
            if (lx >= p.st.x && lx < p.st.x + PW && ly >= p.st.y && ly < p.st.y + p.h) {
                p.scrollT = Math.max(0f, Math.min(p.maxScroll, p.scrollT - (float) vAmount * 26f));
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(CharInput input) {
        String chr = input.asString();
        boolean printable = !chr.isEmpty() && chr.codePointAt(0) >= 32;
        if ((editingText != null || editingColor != null) && printable) {
            if (editBuffer.length() < 160) editBuffer += chr;
            applyEditBuffer();
            return true;
        }
        if (cfgFocus && printable) {
            if (ConfigsBridge.name.length() < 32) ConfigsBridge.name += chr;
            return true;
        }
        if (searchFocus && printable && query.length() < 24) {
            query += chr;
            for (Panel p : panels) p.scrollT = 0f;
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (bindingSetting != null) {
            bindingSetting.set((keyCode == 256 || keyCode == 259 || keyCode == 261) ? KeyBind.NONE : KeyBind.keyboard(keyCode));
            bindingSetting = null;
            return true;
        }
        if (binding != null) {
            binding.setBind((keyCode == 256 || keyCode == 259 || keyCode == 261) ? KeyBind.NONE : KeyBind.keyboard(keyCode));
            binding = null;
            return true;
        }
        if (editingText != null || editingColor != null) {
            if (keyCode == 257 || keyCode == 335) { finishEdit(); return true; }
            if (keyCode == 256) { editingText = null; editingColor = null; editBuffer = ""; return true; }
            if ((keyCode == 259 || keyCode == 261) && !editBuffer.isEmpty()) {
                editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                applyEditBuffer();
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

    private void applyEditBuffer() {
        if (editingText != null) editingText.set(editBuffer);
        if (editingColor != null) {
            java.awt.Color parsed = parseColor(editBuffer);
            if (parsed != null) editingColor.set(parsed);
        }
    }

    private void finishEdit() {
        applyEditBuffer();
        editingText = null;
        editingColor = null;
        editBuffer = "";
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
        } catch (NumberFormatException ignored) {
            // Not a valid hex colour yet; the user is still typing.
        }
        return null;
    }
}
