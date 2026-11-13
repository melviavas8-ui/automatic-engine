package daamky.hud;

import daamky.gui.Modules;
import daamky.gui.Modules.Mod;
import daamky.render.MsdfText;
import daamky.render.SdfRect;
import daamky.render.Txt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.Team;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;






public final class DaamkyHud {
    private DaamkyHud() {}


    static final int BODY = 0xFF080D16, BORDER = 0xAA3C82D8, WHITE = 0xFFFFFFFF, RED = 0xFFFF6B78,
            GREEN = 0xFF62DFA0, YELLOW = 0xFFFFD36A, TRACK = 0xFF17263B;
    static final float HEAD = 19, ROW = 14, PANEL_W = 130, R = 7;


    private static MinecraftClient mc;
    private static DrawContext ctx;
    private static Matrix3x2f base, m;
    private static float alpha = 1f, dt, time, sw, sh;
    private static long lastNs;
    private static float curScale = 1f, curCx, curCy, curDy;


    static float clamp01(float v) { return v < 0 ? 0 : Math.min(v, 1); }
    static float easeOutBack(float t, float s) { t = clamp01(t); float u = t - 1; return 1 + (s + 1) * u * u * u + s * u * u; }
    static float easeOutCubic(float t) { float u = 1 - clamp01(t); return 1 - u * u * u; }
    static float ap(float cur, float target, float speed) {
        float v = cur + (target - cur) * (1f - (float) Math.exp(-speed * dt));
        return Math.abs(target - v) < 0.0005f ? target : v;
    }
    static float lin(float cur, float target, float dur) {
        float st = dt / dur;
        return cur < target ? Math.min(target, cur + st) : Math.max(target, cur - st);
    }
    static float ty(float center, float size) { return center - size * 0.58f; }

    static int A(int c) {
        int a = Math.round(((c >>> 24) & 0xFF) * alpha);
        return (a << 24) | (c & 0xFFFFFF);
    }
    static void fill(float x, float y, float w, float h, float r, int c) { SdfRect.fill(m, x, y, w, h, r, A(c)); }
    static void border(float x, float y, float w, float h, float r, int c) { SdfRect.border(m, x, y, w, h, r, 1f, A(c)); }
    static void grad(float x, float y, float w, float h, float r) { SdfRect.flow(m, x, y, w, h, r, alpha); }
    static void gglow(float x, float y, float w, float h, float r, float spread, int a) {
        int v = Math.round(a * alpha);
        if (v > 0) SdfRect.flowGlow(m, x, y, w, h, r, spread, v);
    }
    static float tb(String s, float x, float y, float size, int c) { return Txt.bold(m, s, x, y, size, A(c)); }
    static float wb(String s, float size) { return Txt.wBold(s, size); }


    static int gcol(float u) {
        u = clamp01(u);
        float[] a = SdfRect.GA, b = SdfRect.GB, c = SdfRect.GC, f;
        float t;
        if (u < 0.5f) { t = u * 2f; f = new float[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t}; }
        else { t = (u - 0.5f) * 2f; f = new float[]{b[0] + (c[0] - b[0]) * t, b[1] + (c[1] - b[1]) * t, b[2] + (c[2] - b[2]) * t}; }
        return 0xFF000000 | (Math.round(f[0] * 255) << 16) | (Math.round(f[1] * 255) << 8) | Math.round(f[2] * 255);
    }


    private interface HeadDraw { void draw(DrawContext c, int x, int y, int size); }
    private record Row(String id, String label, String tag, int tagC, String value, int valC) {}

    private abstract static class El {
        final float dfx, dfy;
        float fx, fy, x, y, w, h, vis;
        El(float dfx, float dfy) { this.dfx = fx = dfx; this.dfy = fy = dfy; }
        abstract boolean enabled();

        abstract boolean want(boolean chat);
        abstract void layout();
        abstract void draw();
        void hidden() {}
        float slideY(float k) { return 0; }
    }


    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final El WATERMARK = new El(0.012f, 0.014f) {
        String text = "";
        boolean enabled() { return Modules.WM_ON.value; }
        boolean want(boolean chat) { return true; }
        void layout() {
            String s = "DAAMKY 1.0 RELEASE  »  " + mc.getSession().getUsername() + "  »  " + LocalTime.now().format(TIME);
            text = Modules.WM_LOWER.value ? s.toLowerCase() : s.toUpperCase(Locale.ROOT);
            w = wb(text, Modules.WM_SIZE.value) + 12;
            h = Modules.WM_SIZE.value + 8;
        }
        void draw() {
            gglow(x, y, w, h, 6, 11, 0x70);
            fill(x, y, w, h, 6, BODY);
            border(x, y, w, h, 6, BORDER);
            tb(text, x + 6, ty(y + h / 2f, Modules.WM_SIZE.value), Modules.WM_SIZE.value, WHITE);
        }
        float slideY(float k) { return -(1f - k) * 14f; }
    };


    private static final class RowS { String label, tag, value; int tagC, valC; float t; boolean present; }

    private abstract static class ListPanel extends El {
        final String title;
        final Map<String, RowS> rows = new LinkedHashMap<>();
        List<Row> cur = List.of();
        ListPanel(String title, float fx, float fy) { super(fx, fy); this.title = title; }
        abstract boolean on();
        abstract List<Row> collect();
        abstract Row preview();

        boolean enabled() { return on(); }

        boolean want(boolean chat) {
            cur = collect();
            if (cur.isEmpty() && chat) cur = List.of(preview());
            if (cur.isEmpty()) return false;
            for (RowS r : rows.values()) r.present = false;
            for (Row r : cur) {
                RowS s = rows.computeIfAbsent(r.id, k -> new RowS());
                s.label = r.label; s.tag = r.tag; s.tagC = r.tagC; s.value = r.value; s.valC = r.valC; s.present = true;
            }
            return true;
        }

        void hidden() { rows.clear(); }

        void layout() {
            float need = PANEL_W, sum = 0;
            for (Iterator<RowS> it = rows.values().iterator(); it.hasNext(); ) {
                RowS r = it.next();
                r.t = lin(r.t, r.present ? 1f : 0f, 0.25f);
                if (!r.present && r.t <= 0f) { it.remove(); continue; }
                float need2 = 7 + wb(r.label, 8) + (r.tag != null ? 4 + wb(r.tag, 8) : 0) + 14 + wb(r.value, 8) + 7;
                need = Math.max(need, need2);
                sum += ROW * easeOutCubic(r.t);
            }
            w = need;
            h = HEAD + sum + 4;
        }

        void draw() {
            gglow(x, y, w, h, R, 13, 0x78);
            fill(x, y, w, h, R, BODY);
            grad(x, y, w, Math.min(HEAD + R, h), R);
            fill(x, y + HEAD, w, Math.max(0, Math.min(R + 1, h - HEAD - 2)), 0, BODY);
            border(x, y, w, h, R, BORDER);
            float tw = wb(title, 10);
            tb(title, x + (w - tw) / 2f, ty(y + HEAD / 2f, 10), 10, WHITE);

            float ry = y + HEAD;
            float base = alpha;
            for (RowS r : rows.values()) {
                float e = easeOutCubic(r.t), rh = ROW * e, c = ry + rh / 2f;
                if (e > 0.02f) {
                    alpha = base * e;
                    float sl = (1f - e) * -8f, lx = x + 7 + sl;
                    float lw = tb(r.label, lx, ty(c, 8), 8, WHITE);
                    if (r.tag != null) tb(r.tag, lx + lw + 4, ty(c, 8), 8, r.tagC);
                    tb(r.value, x + w - 7 - wb(r.value, 8) + sl, ty(c, 8), 8, r.valC);
                    alpha = base;
                }
                ry += rh;
            }
        }
    }

    private static final Map<String, float[]> CD = new HashMap<>();

    private static final ListPanel POTIONS = new ListPanel("Potions", 0.805f, 0.285f) {
        boolean on() { return Modules.PO_ON.value; }
        Row preview() { return new Row("p", "Preview Potion", "999", RED, "**:**", 0xFFFFFFFF); }
        List<Row> collect() {
            List<Row> out = new ArrayList<>();
            if (mc.player == null) return out;
            for (StatusEffectInstance e : mc.player.getStatusEffects()) {
                String name = e.getEffectType().value().getName().getString();
                String tag = e.getAmplifier() > 0 ? String.valueOf(e.getAmplifier() + 1) : null;
                String val;
                if (e.isInfinite()) val = "**:**";
                else { int s = e.getDuration() / 20; val = (s / 60) + ":" + String.format("%02d", s % 60); }
                out.add(new Row(e.getTranslationKey(), name, tag, RED, val, 0xFFFFFFFF));
            }
            return out;
        }
    };

    private static final ListPanel COOLDOWNS = new ListPanel("Cooldowns", 0.012f, 0.295f) {
        boolean on() { return Modules.CD_ON.value; }
        Row preview() { return new Row("p", "Preview Cooldown", null, 0, "**:**", 0xFFFFFFFF); }
        List<Row> collect() {
            List<Row> out = new ArrayList<>();
            if (mc.player == null) return out;
            Set<String> seen = new HashSet<>();
            for (int i = 0; i < 10; i++) {
                ItemStack st = i < 9 ? mc.player.getInventory().getStack(i) : mc.player.getOffHandStack();
                if (st.isEmpty()) continue;
                String id = st.getItem().getTranslationKey();
                if (!seen.add(id)) continue;
                float p = mc.player.getItemCooldownManager().getCooldownProgress(st, 0f);
                if (p <= 0f) { CD.remove(id); continue; }
                float[] s = CD.computeIfAbsent(id, k -> new float[]{p, 0f, time});
                float el = time - s[2];
                if (el > 0.05f) {
                    float rate = (s[0] - p) / el;
                    s[1] = s[1] == 0f ? rate : s[1] + (rate - s[1]) * 0.2f;
                    s[0] = p; s[2] = time;
                }
                String val = s[1] > 0.001f ? String.format(Locale.ROOT, "%.1fs", p / s[1]) : Math.round(p * 100) + "%";
                out.add(new Row(id, st.getName().getString(), null, 0, val, 0xFFFFFFFF));
            }
            return out;
        }
    };

    private static final ListPanel KEYBINDS = new ListPanel("Keybinds", 0.012f, 0.485f) {
        boolean on() { return Modules.KB_ON.value; }
        Row preview() { return new Row("p", "Preview Bind", null, 0, "[Key]", 0xFFFFFFFF); }
        List<Row> collect() {
            List<Row> out = new ArrayList<>();
            for (Mod mod : Modules.ALL)
                if (mod.enabled && mod.hasBind())
                    out.add(new Row(mod.name, mod.name, null, 0, "[" + mod.keyDisplay() + "]", 0xFFFFFFFF));
            return out;
        }
    };

    private static final String[] STAFF_WORDS = {"admin", "moder", "helper", "owner", "curator", "staff", "dev", "support",
            "админ", "модер", "хелпер", "куратор", "стаж", "владел", "разработ", "саппорт"};

    private static final ListPanel STAFF = new ListPanel("Staff Viewer", 0.805f, 0.68f) {
        boolean on() { return Modules.SV_ON.value; }
        Row preview() { return new Row("p", "Preview Staff", null, 0, "Status", YELLOW); }
        List<Row> collect() {
            List<Row> out = new ArrayList<>();
            if (mc.getNetworkHandler() == null || mc.world == null) return out;
            for (PlayerListEntry e : mc.getNetworkHandler().getPlayerList()) {
                String name = e.getProfile().name();
                if (mc.player != null && name.equals(mc.player.getGameProfile().name())) continue;
                Team t = mc.world.getScoreboard().getScoreHolderTeam(name);
                String tag = (t == null ? "" : t.getPrefix().getString()) + (e.getDisplayName() == null ? "" : e.getDisplayName().getString());
                tag = tag.toLowerCase(Locale.ROOT);
                boolean staff = false;
                for (String wd : STAFF_WORDS) if (tag.contains(wd)) { staff = true; break; }
                if (!staff) continue;
                boolean near = mc.world.getPlayerByUuid(e.getProfile().id()) != null;
                out.add(new Row(name, name, null, 0, near ? "Near" : "Online", near ? GREEN : YELLOW));
            }
            return out;
        }
    };


    private static final El TARGET = new El(0.38f, 0.30f) {
        LivingEntity target;
        float lastSeen = -100f, hpShown = 1f, hpTrail = 1f;
        String name = "Player", hpText = "HP: 20.0";
        float frac = 1f;
        HeadDraw head;

        boolean enabled() { return Modules.TH_ON.value; }

        boolean want(boolean chat) {
            if (mc.player == null) return false;
            if (mc.targetedEntity instanceof LivingEntity le && le.isAlive()) { target = le; lastSeen = time; }
            if (target != null && time - lastSeen < 1.5f) {
                snapshot(target);
                return true;
            }
            if (chat || Modules.TH_TEST.value) {
                name = "Player"; hpText = "HP: 20.0"; frac = 1f;
                head = (c, x, y, s) -> PlayerSkinDrawer.draw(c, mc.player.getSkin(), x, y, s);
                headLetter = null;
                return true;
            }
            return false;
        }

        void snapshot(LivingEntity le) {
            name = le.getName().getString();
            float hp = le.getHealth() + le.getAbsorptionAmount();
            hpText = String.format(Locale.ROOT, "HP: %.1f", hp);
            frac = clamp01(le.getHealth() / Math.max(1f, le.getMaxHealth()));
            if (le instanceof AbstractClientPlayerEntity pl) head = (c, x, y, s) -> PlayerSkinDrawer.draw(c, pl.getSkin(), x, y, s);
            else {
                final String letter = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(Locale.ROOT);
                head = (c, x, y, s) -> {};
                headLetter = letter;
                return;
            }
            headLetter = null;
        }
        String headLetter;

        void layout() {
            w = Math.max(132, 43 + wb(name, 11) + 10);
            h = 46;
            hpShown = ap(hpShown, frac, 10f);
            hpTrail = ap(hpTrail, frac, 3f);
        }

        void draw() {
            gglow(x, y, w, h, R, 13, 0x78);
            fill(x, y, w, h, R, BODY);
            border(x, y, w, h, R, BORDER);


            float hs = 32, hx = x + 7, hy = y + (h - hs) / 2f;
            if (headLetter != null) {
                fill(hx, hy, hs, hs, 7, 0xFF18181E);
                float lw = wb(headLetter, 16);
                tb(headLetter, hx + (hs - lw) / 2f, ty(hy + hs / 2f, 16), 16, WHITE);
            } else if (head != null) {
                var ms = ctx.getMatrices();
                ms.pushMatrix();
                ms.translate(0, curDy);
                ms.translate(curCx, curCy);
                ms.scale(curScale, curScale);
                ms.translate(-curCx, -curCy);
                head.draw(ctx, Math.round(hx), Math.round(hy), Math.round(hs));
                ms.popMatrix();
                SdfRect.cutout(m, Math.round(hx), Math.round(hy), Math.round(hs), Math.round(hs), 7, BODY);
            }

            float tx = x + 43;
            tb(name, tx, y + 6, 11, WHITE);
            tb(hpText, tx, y + 20, 8, 0xFFDDEBFF);


            float bx = tx, by = y + 31, bw = w - 43 - 8, bh = 9;
            fill(bx, by, bw, bh, 4.5f, TRACK);
            if (hpTrail > hpShown) fill(bx, by, Math.max(bh, bw * hpTrail), bh, 4.5f, 0x66FFFFFF);
            grad(bx, by, Math.max(bh, bw * hpShown), bh, 4.5f);
        }
    };

    private static final El[] ELEMENTS = {WATERMARK, KEYBINDS, COOLDOWNS, POTIONS, STAFF, TARGET};



    private static final class AE { final Mod mod; float t; boolean present; String name; float w; AE(Mod m) { mod = m; } }
    private static final List<AE> ARR = new ArrayList<>();
    static final float ARR_H = 11f, ARR_SIZE = 8.5f;


    private static float arrFx = 1f, arrFy = 0.004f, arrBx, arrBy, arrBw, arrBh, arrGx, arrGy;
    private static boolean arrDrag, chatNow;

    private static void drawArrayList() {
        boolean on = Modules.HUD.enabled && Modules.ARR_ON.value;
        for (AE a : ARR) a.present = false;
        if (on) for (Mod mod : Modules.ALL) {
            if (!mod.enabled || mod == Modules.HUD || mod.category >= 5) continue;
            AE found = null;
            for (AE a : ARR) if (a.mod == mod) { found = a; break; }
            if (found == null) { found = new AE(mod); ARR.add(found); }
            found.present = true;
        }
        for (Iterator<AE> it = ARR.iterator(); it.hasNext(); ) {
            AE a = it.next();
            a.t = lin(a.t, a.present ? 1f : 0f, 0.3f);
            if (!a.present && a.t <= 0f) { it.remove(); continue; }
            a.name = Modules.ARR_LOWER.value ? a.mod.name.toLowerCase() : a.mod.name;
            a.w = wb(a.name, ARR_SIZE) + 9;
        }
        if (ARR.isEmpty()) return;
        ARR.sort((p, q) -> Float.compare(q.w, p.w));


        int n = ARR.size();
        float[] ys = new float[n], hs = new float[n];
        float y = 0;
        for (int i = 0; i < n; i++) { ys[i] = y; hs[i] = ARR_H * easeOutCubic(ARR.get(i).t); y += hs[i]; }
        float total = y;
        float maxW = 0;
        for (AE a : ARR) maxW = Math.max(maxW, a.w);
        float ax = Math.max(0, Math.min(sw, arrFx * sw));
        boolean left = ax < sw / 2f;
        float top = Math.max(0, Math.min(sh - total, arrFy * sh));
        arrBx = left ? ax : ax - maxW; arrBy = top; arrBw = maxW; arrBh = total;


        if (Modules.ARR_GLOW.value) for (int i = 0; i < n; i++) {
            AE a = ARR.get(i);
            if (hs[i] < 0.5f) continue;
            int col = gcol(arrU(ys[i] + hs[i] / 2f, total));
            SdfRect.glow(m, left ? ax : ax - a.w, top + ys[i], a.w, hs[i], 0, 8, (Math.round(0x78 * easeOutCubic(a.t)) << 24) | (col & 0xFFFFFF));
        }

        for (int i = 0; i < n; i++) {
            AE a = ARR.get(i);
            if (hs[i] < 0.3f) continue;
            SdfRect.fill(m, left ? ax : ax - a.w, top + ys[i], a.w, hs[i] + 0.6f, 0, 0xFF07070A);
        }

        for (int i = 0; i < n; i++) {
            AE a = ARR.get(i);
            float e = easeOutCubic(a.t);
            if (e < 0.35f) continue;
            int col = gcol(arrU(ys[i] + hs[i] / 2f, total));
            int c = (Math.round(255 * clamp01((e - 0.35f) / 0.65f)) << 24) | (col & 0xFFFFFF);
            Txt.bold(m, a.name, (left ? ax : ax - a.w) + 4.5f, ty(top + ys[i] + hs[i] / 2f, ARR_SIZE), ARR_SIZE, c);
        }
    }

    static void arrHighlight() {
        if (!chatNow || arrBw <= 0 || ARR.isEmpty()) return;
        if (!arrDrag && !(mouseX() >= arrBx && mouseX() < arrBx + arrBw && mouseY() >= arrBy && mouseY() < arrBy + arrBh)) return;
        SdfRect.border(m, arrBx - 2, arrBy - 2, arrBw + 4, arrBh + 4, 4, 1f, arrDrag ? 0xCCFFFFFF : 0x55FFFFFF);
    }


    private static float arrU(float yy, float total) {
        float u = yy / Math.max(60f, total);
        return clamp01(u * 0.95f + 0.06f * (float) Math.sin(time * 1.2f - yy * 0.05f));
    }


    private static El dragEl;
    private static float gx, gy;
    private static boolean wasDown;

    private static void handleDrag(boolean chat) {
        if (!chat) { dragEl = null; arrDrag = false; wasDown = false; return; }
        var win = mc.getWindow();
        float mx = mouseX(), my = mouseY();
        boolean down = GLFW.glfwGetMouseButton(win.getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown && !ARR.isEmpty() && arrBw > 0 && mx >= arrBx && mx < arrBx + arrBw && my >= arrBy && my < arrBy + arrBh) {
            arrDrag = true; arrGx = mx - arrFx * sw; arrGy = my - arrBy;
        }
        if (arrDrag) {
            if (!down) arrDrag = false;
            else { arrFx = clamp01((mx - arrGx) / sw); arrFy = clamp01((my - arrGy) / sh); }
            wasDown = down;
            return;
        }
        if (down && !wasDown) {
            for (int i = ELEMENTS.length - 1; i >= 0; i--) {
                El e = ELEMENTS[i];
                if (e.vis > 0.5f && mx >= e.x && mx < e.x + e.w && my >= e.y && my < e.y + e.h) {
                    dragEl = e; gx = mx - e.x; gy = my - e.y;
                    break;
                }
            }
        }
        if (!down) dragEl = null;
        if (dragEl != null) {
            float nx = Math.max(0, Math.min(sw - dragEl.w, mx - gx)), ny = Math.max(0, Math.min(sh - dragEl.h, my - gy));
            dragEl.fx = nx / sw; dragEl.fy = ny / sh;
        }
        wasDown = down;
    }

    static float mouseX() { return (float) (mc.mouse.getX() * sw / mc.getWindow().getWidth()); }
    static float mouseY() { return (float) (mc.mouse.getY() * sh / mc.getWindow().getHeight()); }

    static boolean hovered(El e) {
        var win = mc.getWindow();
        float mx = (float) (mc.mouse.getX() * sw / win.getWidth()), my = (float) (mc.mouse.getY() * sh / win.getHeight());
        return mx >= e.x && mx < e.x + e.w && my >= e.y && my < e.y + e.h;
    }


    static String keyName(int key) {
        try { return InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString(); }
        catch (Exception ex) { return "?"; }
    }


    public static void render(DrawContext c) {
        Modules.refreshFromTarget();
        mc = MinecraftClient.getInstance();
        if (Modules.HUD == null || !Modules.HUD.enabled || mc.player == null || mc.world == null
                || mc.options.hudHidden || daamky.render.Fonts.regular() == null) return;
        ctx = c;
        daamky.render.Gfx.ctx = c;
        long now = System.nanoTime();
        dt = lastNs == 0 ? 0.016f : Math.min(0.05f, (now - lastNs) / 1e9f);
        lastNs = now;
        time += dt;
        sw = mc.getWindow().getScaledWidth();
        sh = mc.getWindow().getScaledHeight();
        MsdfText.msdfMode = Modules.useMsdf();
        MsdfText.baseWeight = Modules.weight();

        base = new Matrix3x2f(c.getMatrices());
        m = base;
        alpha = 1f;

        boolean chat = mc.currentScreen instanceof ChatScreen;
        handleDrag(chat);

        chatNow = chat;
        drawArrayList();
        arrHighlight();

        for (El e : ELEMENTS) {
            boolean show = Modules.HUD.enabled && e.enabled() && e.want(chat);
            e.vis = lin(e.vis, show ? 1f : 0f, show ? 0.5f : 0.22f);
            if (e.vis <= 0f) { e.hidden(); continue; }
            e.layout();
            e.x = Math.max(0, Math.min(sw - e.w, e.fx * sw));
            e.y = Math.max(0, Math.min(sh - e.h, e.fy * sh));


            float k = show ? easeOutBack(e.vis, 1.8f) : e.vis * e.vis;
            float sc = 0.7f + 0.3f * k;
            curScale = sc;
            curDy = e.slideY(k);
            curCx = e.x + e.w / 2f;
            curCy = e.y + e.h / 2f;
            m = new Matrix3x2f(base).translate(0, curDy).translate(curCx, curCy).scale(sc, sc).translate(-curCx, -curCy);
            alpha = clamp01(e.vis * 2.5f);
            e.draw();

            if (chat && (e == dragEl || hovered(e))) {
                float a0 = alpha;
                border(e.x - 2, e.y - 2, e.w + 4, e.h + 4, R + 2, e == dragEl ? 0xCCFFFFFF : 0x55FFFFFF);
                alpha = a0;
            }
        }
        m = base;
        alpha = 1f;
    }
}
