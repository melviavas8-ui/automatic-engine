package daamky.gui;

import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.bind.InputType;
import daamky.ua.api.settings.impl.*;
import daamky.ua.api.module.impl.visual.Hud;

import java.awt.Color;
import java.util.*;
import java.util.function.*;

/**
 * Bridge between the Daamky UI and the original module/settings system.
 * Values are read from and written to the original Setting instances.
 */
public final class Modules {
    private Modules() {}

    public static abstract class Setting {
        public final String name;
        private BooleanSupplier visible = () -> true;
        protected Runnable pull = () -> {};
        Setting(String name) { this.name = name == null ? "" : name; }
        public boolean isVisible() {
            try { pull.run(); return visible.getAsBoolean(); }
            catch (RuntimeException ignored) { return false; }
        }
        public void refresh() { try { pull.run(); } catch (RuntimeException ignored) {} }
        @SuppressWarnings("unchecked")
        public <T extends Setting> T showIf(BooleanSupplier condition) {
            visible = condition == null ? () -> true : condition;
            return (T) this;
        }
    }

    public static final class Section extends Setting {
        public Section(String name) { super(name); }
    }

    public static class Bool extends Setting {
        public boolean value;
        private Consumer<Boolean> push;
        public Bool(String name, boolean value) { super(name); this.value = value; this.push = v -> this.value = v; }
        public void bind(BooleanSupplier getter, Consumer<Boolean> setter) {
            this.pull = () -> this.value = getter.getAsBoolean();
            this.push = setter;
            refresh();
        }
        public void set(boolean next) {
            if (next != value) {
                value = next;
                push.accept(next);
            }
        }
        public boolean on() { return value && isVisible(); }
    }

    public static final class Slider extends Setting {
        public final float min, max, step;
        public final String suffix;
        public float value;
        private Consumer<Float> push;
        public Slider(String name, float value, float min, float max, float step, String suffix) {
            super(name);
            this.min = min; this.max = max; this.step = Math.max(0.0001f, step);
            this.suffix = suffix == null ? "" : suffix; this.value = value;
            this.push = v -> this.value = v;
        }
        public void bind(DoubleSupplier getter, DoubleConsumer setter) {
            this.pull = () -> this.value = (float) getter.getAsDouble();
            this.push = v -> setter.accept(v.doubleValue());
            refresh();
        }
        public void set(float v) {
            v = Math.round((v - min) / step) * step + min;
            v = Math.max(min, Math.min(max, v));
            if (Math.abs(v - value) > 0.00001f) { value = v; push.accept(v); }
        }
        public float frac() { return max <= min ? 0f : (value - min) / (max - min); }
        public String text() {
            String n = step >= 1f ? String.valueOf(Math.round(value))
                    : String.format(Locale.ROOT, step >= 0.1f ? "%.1f" : "%.2f", value);
            return n + suffix;
        }
    }

    public static final class Mode extends Setting {
        public String[] modes;
        public int index;
        public boolean open;
        private Supplier<String> getter;
        private Supplier<List<String>> choices;
        private Consumer<String> setter;
        public Mode(String name, int index, String... modes) {
            super(name);
            this.modes = modes == null ? new String[0] : modes.clone();
            this.index = this.modes.length == 0 ? 0 : Math.max(0, Math.min(this.modes.length - 1, index));
            this.getter = this::get;
            this.choices = () -> Arrays.asList(this.modes);
            this.setter = v -> select(v);
        }
        public void bind(Supplier<String> getter, Consumer<String> setter, Supplier<List<String>> choices) {
            this.getter = getter; this.setter = setter; this.choices = choices;
            this.pull = () -> {
                String current = getter.get();
                List<String> available = new ArrayList<>(choices.get());
                if (current != null && !available.contains(current)) available.add(0, current);
                this.modes = available.toArray(String[]::new);
                int found = -1;
                for (int i = 0; i < modes.length; i++) if (Objects.equals(modes[i], current)) { found = i; break; }
                if (found >= 0) index = found;
            };
            refresh();
        }
        public String get() { return modes.length == 0 ? "—" : modes[Math.max(0, Math.min(index, modes.length - 1))]; }
        public void select(int i) {
            if (modes.length == 0) return;
            i = Math.max(0, Math.min(modes.length - 1, i));
            if (i != index) { index = i; setter.accept(modes[i]); }
        }
        public void select(String mode) {
            for (int i = 0; i < modes.length; i++) if (modes[i].equals(mode)) { select(i); return; }
        }
        public void next() { if (modes.length > 0) select((index + 1) % modes.length); }
        public void prev() { if (modes.length > 0) select((index + modes.length - 1) % modes.length); }
    }

    public static class Group extends Setting {
        public final List<Setting> children = new ArrayList<>();
        public boolean open = true;
        public Group(String name, Setting... children) {
            super(name);
            if (children != null) this.children.addAll(Arrays.asList(children));
        }
        @Override public boolean isVisible() {
            if (!super.isVisible()) return false;
            for (Setting child : children) if (child.isVisible()) return true;
            return children.isEmpty();
        }
    }

    public static final class Multi extends Group {
        private int selectedCount;
        public Multi(String name) { super(name); }
        public void bind(MultiModeSetting setting) {
            this.children.clear();
            for (String mode : setting.getModes()) {
                MultiOption option = new MultiOption(mode);
                option.bind(() -> setting.isSelected(mode), value -> setting.setSelected(mode, value));
                this.children.add(option);
            }
            this.pull = () -> {
                int count = 0;
                for (Setting child : children) {
                    child.refresh();
                    if (child instanceof Bool b && b.value) count++;
                }
                selectedCount = count;
            };
            refresh();
        }
        public int selectedCount() { refresh(); return selectedCount; }
    }

    public static final class MultiOption extends Bool {
        public MultiOption(String name) { super(name, false); }
    }

    public static final class TextValue extends Setting {
        public String value = "";
        private Consumer<String> setter;
        public TextValue(String name) { super(name); setter = v -> value = v; }
        public void bind(Supplier<String> getter, Consumer<String> setter) {
            this.pull = () -> value = Objects.toString(getter.get(), "");
            this.setter = setter; refresh();
        }
        public void set(String text) { value = text == null ? "" : text; setter.accept(value); }
        public String text() { return value; }
    }

    public static final class ColorValue extends Setting {
        public Color value = Color.WHITE;
        private Consumer<Color> setter;
        public ColorValue(String name) { super(name); setter = v -> value = v; }
        public void bind(Supplier<Color> getter, Consumer<Color> setter) {
            this.pull = () -> value = getter.get() == null ? Color.WHITE : getter.get();
            this.setter = setter; refresh();
        }
        public void set(Color color) { value = color == null ? Color.WHITE : color; setter.accept(value); }
        public String text() { return String.format(Locale.ROOT, "#%02X%02X%02X", value.getRed(), value.getGreen(), value.getBlue()); }
    }

    public static final class BindValue extends Setting {
        public KeyBind value = KeyBind.NONE;
        private Consumer<KeyBind> setter;
        public BindValue(String name) { super(name); setter = v -> value = v; }
        public void bind(Supplier<KeyBind> getter, Consumer<KeyBind> setter) {
            this.pull = () -> value = getter.get() == null ? KeyBind.NONE : getter.get();
            this.setter = setter; refresh();
        }
        public void set(KeyBind bind) { value = bind == null ? KeyBind.NONE : bind; setter.accept(value); }
        public String text() { return value.getDisplayName(); }
    }

    public static final class Button extends Setting {
        private final Runnable action;
        public Button(String name, Runnable action) { super(name); this.action = action == null ? () -> {} : action; }
        public void press() { action.run(); }
    }

    public static final class Mod {
        public final String name, desc;
        public final int category;
        public boolean enabled;
        public int key = -1;
        public final List<Setting> settings = new ArrayList<>();
        private final daamky.ua.api.module.Module source;
        private BindValue moduleBind;

        private Mod(daamky.ua.api.module.Module source) {
            this.source = source;
            this.name = source.getName();
            this.desc = source.getDescription();
            this.category = category(source.getCategory());
            refresh();
            BindSetting bind = source.getBindSetting();
            if (bind != null) {
                moduleBind = new BindValue("Module Bind");
                moduleBind.bind(bind::getValue, bind::setValue);
                settings.add(moduleBind);
            }
            for (daamky.ua.api.settings.Setting<?> setting : source.getSettings()) {
                Setting adapted = adapt(setting);
                if (adapted != null) settings.add(adapted);
            }
        }
        public boolean hasSettings() { return !settings.isEmpty(); }
        public List<Setting> flat() { return Modules.flat(this); }
        public void refresh() {
            enabled = source.isEnabled();
            KeyBind bind = source.getBind();
            key = bind != null && bind.getType() == InputType.KEYBOARD ? bind.getCode() : -1;
            for (Setting setting : settings) setting.refresh();
        }
        public void setEnabled(boolean value) { source.setEnabled(value); enabled = source.isEnabled(); }
        public void setBind(KeyBind value) { source.setBind(value); refresh(); }
        public boolean hasBind() { return source.getBind() != null && source.getBind().isBound(); }
        public String keyDisplay() { return source.getBind() == null ? "None" : source.getBind().getDisplayName(); }
        public daamky.ua.api.module.Module source() { return source; }
    }

    public static final List<Mod> ALL = new ArrayList<>();
    public static Mod HUD;
    public static final Slider SOUL_SIZE = new Slider("Размер", 1, .5f, 2, .1f, "x");
    public static final Slider SOUL_SPEED = new Slider("Скорость", 1, .3f, 3, .1f, "x");

    public static final Bool WM_ON = new Bool("Включено", true);
    public static final Bool WM_LOWER = new Bool("Маленькие буквы", false);
    public static final Slider WM_SIZE = new Slider("Размер текста", 8, 6, 14, 1, "");
    public static final Bool ARR_ON = new Bool("Включено", true);
    public static final Bool ARR_GLOW = new Bool("Свечение", true);
    public static final Bool ARR_LOWER = new Bool("Маленькие буквы", false);
    public static final Bool SV_ON = new Bool("Включено", true);
    public static final Bool TH_ON = new Bool("Включено", true);
    public static final Bool TH_TEST = new Bool("Тестовый режим", false);
    public static final Bool PO_ON = new Bool("Включено", true);
    public static final Bool CD_ON = new Bool("Включено", true);
    public static final Bool KB_ON = new Bool("Включено", true);
    public static final Mode ENGINE = new Mode("Режим", 0, "MSDF", "SDF");
    public static final Mode WEIGHT = new Mode("Толщина", 1, "Тонкий", "Обычный", "Жирный");
    public static final Mode DEMO_SIZE = new Mode("Размер", 1, "8", "10", "14", "20");

    public static void bindToTarget(ModuleManager manager) {
        ALL.clear();
        HUD = null;
        if (manager == null) return;
        for (daamky.ua.api.module.Module module : manager.getModules()) {
            Mod mod = new Mod(module);
            ALL.add(mod);
            if (module instanceof Hud) HUD = mod;
        }
        if (HUD != null) bindHudSettings((Hud) HUD.source());
        refreshFromTarget();
    }

    public static void refreshFromTarget() {
        for (Mod mod : ALL) mod.refresh();
        WM_ON.refresh(); WM_LOWER.refresh(); WM_SIZE.refresh();
        ARR_ON.refresh(); ARR_GLOW.refresh(); ARR_LOWER.refresh();
        SV_ON.refresh(); TH_ON.refresh(); TH_TEST.refresh();
        PO_ON.refresh(); CD_ON.refresh(); KB_ON.refresh();
        syncTheme();
    }

    private static void syncTheme() {
        try {
            daamky.ua.api.theme.ClientTheme active = daamky.ua.api.theme.ThemeManager.current();
            Color accent = active.accent();
            float[] hsb = Color.RGBtoHSB(accent.getRed(), accent.getGreen(), accent.getBlue(), null);
            daamky.render.Theme.custom = true;
            daamky.render.Theme.hue = hsb[0] * 360.0f;
            daamky.render.Theme.sat = hsb[1];
            daamky.render.Theme.bri = Math.max(0.72f, hsb[2]);
            daamky.render.Theme.apply();
        } catch (RuntimeException ignored) {
            // Keep the GUI's built-in palette if the target theme manager is not initialized yet.
        }
    }

    private static void bindHudSettings(Hud hud) {
        for (daamky.ua.api.settings.Setting<?> setting : hud.getSettings()) {
            String n = setting.getName();
            if ("GUI Watermark".equals(n) && setting instanceof BooleanSetting s) WM_ON.bind(s::getValue, s::setValue);
            else if ("GUI Watermark Lowercase".equals(n) && setting instanceof BooleanSetting s) WM_LOWER.bind(s::getValue, s::setValue);
            else if ("GUI Watermark Size".equals(n) && setting instanceof NumberSetting s) WM_SIZE.bind(() -> s.getValue(), v -> s.setValue((double) v));
            else if ("GUI ArrayList".equals(n) && setting instanceof BooleanSetting s) ARR_ON.bind(s::getValue, s::setValue);
            else if ("GUI ArrayList Glow".equals(n) && setting instanceof BooleanSetting s) ARR_GLOW.bind(s::getValue, s::setValue);
            else if ("GUI ArrayList Lowercase".equals(n) && setting instanceof BooleanSetting s) ARR_LOWER.bind(s::getValue, s::setValue);
            else if ("GUI Staff Viewer".equals(n) && setting instanceof BooleanSetting s) SV_ON.bind(s::getValue, s::setValue);
            else if ("GUI Target HUD".equals(n) && setting instanceof BooleanSetting s) TH_ON.bind(s::getValue, s::setValue);
            else if ("GUI Target HUD Test".equals(n) && setting instanceof BooleanSetting s) TH_TEST.bind(s::getValue, s::setValue);
            else if ("GUI Potions".equals(n) && setting instanceof BooleanSetting s) PO_ON.bind(s::getValue, s::setValue);
            else if ("GUI Cooldowns".equals(n) && setting instanceof BooleanSetting s) CD_ON.bind(s::getValue, s::setValue);
            else if ("GUI Keybinds".equals(n) && setting instanceof BooleanSetting s) KB_ON.bind(s::getValue, s::setValue);
        }
    }

    private static Setting adapt(daamky.ua.api.settings.Setting<?> raw) {
        Setting result = null;
        if (raw instanceof BooleanSetting s) {
            Bool b = new Bool(s.getName(), s.getValue());
            b.bind(s::getValue, s::setValue); result = b;
        } else if (raw instanceof NumberSetting s) {
            Slider sl = new Slider(s.getName(), s.getFloat(), (float)s.getMin(), (float)s.getMax(), (float)s.getStep(), "");
            sl.bind(() -> s.getValue(), v -> s.setValue(v)); result = sl;
        } else if (raw instanceof ModeSetting s) {
            Mode m = new Mode(s.getName(), 0, s.getModes().toArray(String[]::new));
            m.bind(s::getValue, s::setValue, s::getVisibleModes); result = m;
        } else if (raw instanceof MultiModeSetting s) {
            Multi multi = new Multi(s.getName()); multi.bind(s); result = multi;
        } else if (raw instanceof StringSetting s) {
            TextValue text = new TextValue(s.getName()); text.bind(s::getValue, s::setValue); result = text;
        } else if (raw instanceof ColorSetting s) {
            ColorValue color = new ColorValue(s.getName()); color.bind(s::getValue, s::setValue); result = color;
        } else if (raw instanceof BindSetting s) {
            BindValue bind = new BindValue(s.getName()); bind.bind(s::getValue, s::setValue); result = bind;
        } else if (raw instanceof ButtonSetting s) {
            result = new Button(s.getName(), s::press);
        } else {
            TextValue fallback = new TextValue(raw.getName());
            fallback.bind(() -> Objects.toString(raw.getValue(), ""), value -> setRawString(raw, value));
            result = fallback;
        }
        if (result != null) result.showIf(raw::isVisible);
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setRawString(daamky.ua.api.settings.Setting raw, String value) {
        try { raw.setValue(value); } catch (RuntimeException ignored) {}
    }

    private static int category(ModuleCategory category) {
        if (category == null) return 4;
        return switch (category) {
            case COMBAT -> 0;
            case MOVEMENT -> 1;
            case VISUAL, WORLD -> 2;
            case PLAYER -> 3;
            case MISC -> 4;
            case DISPLAY -> 6;
            case AUTOBUY -> 7;
        };
    }

    public static List<Setting> rows(Mod mod) {
        List<Setting> out = new ArrayList<>();
        collect(mod.settings, out);
        return out;
    }
    private static void collect(List<Setting> in, List<Setting> out) {
        for (Setting s : in) {
            if (!s.isVisible()) continue;
            out.add(s);
            if (s instanceof Group g && g.open) collect(g.children, out);
        }
    }
    public static List<Setting> flat(Mod mod) {
        List<Setting> out = new ArrayList<>();
        flatten(mod.settings, out);
        return out;
    }
    private static void flatten(List<Setting> in, List<Setting> out) {
        for (Setting s : in) { out.add(s); if (s instanceof Group g) flatten(g.children, out); }
    }

    public static boolean useMsdf() { return true; }
    public static float weight() { return 0.03f; }
    public static boolean demoOn() { return false; }
    public static float demoSize() { return 10f; }
    public static boolean wmOn() { return HUD != null && HUD.enabled && WM_ON.value; }
}
