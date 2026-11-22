package daamky.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import daamky.ua.api.config.ConfigManager;
import net.minecraft.client.gui.screen.Screen;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Selected ClickGUI type. Each {@link UiStyle} remembers its own type (Legacy: Window or DropDown,
 * Liberty City: Window, DropDown or CS). Like the client theme it lives in its own file
 * (system/gui.daamky), so loading somebody else's config never switches your GUI behind your back.
 */
public final class GuiStyle {
    public static final String WINDOW = "Window";
    public static final String DROPDOWN = "DropDown";
    public static final String CS = "CS";
    /** Types of the Legacy style. */
    public static final List<String> NAMES = List.of(WINDOW, DROPDOWN);
    /** Types of the Liberty City style. */
    public static final List<String> LIBERTY_NAMES = List.of(WINDOW, DROPDOWN, CS);

    private static final Gson GSON = new Gson();
    private static final String FILE = "gui.daamky";
    private static String legacyType = WINDOW;
    private static String libertyType = WINDOW;

    static { load(); }

    private GuiStyle() {}

    /** Types available in the active style. */
    public static List<String> names() { return UiStyle.isLiberty() ? LIBERTY_NAMES : NAMES; }

    public static String current() { return UiStyle.isLiberty() ? libertyType : legacyType; }

    public static boolean isDropDown() { return DROPDOWN.equals(current()); }

    /** Returns the canonical type name for the input, or the current type when it is unknown. */
    public static String find(String name) {
        String exact = findExact(name);
        return exact == null ? current() : exact;
    }

    public static void select(String name) {
        String exact = findExact(name);
        if (exact != null && !exact.equals(current())) {
            if (UiStyle.isLiberty()) libertyType = exact; else legacyType = exact;
            save();
        }
    }

    /** True for any of the client's own ClickGUI screens, whatever style is selected. */
    public static boolean isClientGui(Screen screen) {
        return screen instanceof DaamkyClickGui || screen instanceof DaamkyDropDownGui || screen instanceof LC_Window;
    }

    /**
     * Creates the ClickGUI screen for the selected style and type.
     * Liberty City opens LC_Window (also for the CS type, until CS gets its own screen) and,
     * until LC_Dropdown is added in a later stage, the classic DropDown screen.
     */
    public static Screen create() {
        if (UiStyle.isLiberty()) {
            return isDropDown() ? new DaamkyDropDownGui() : new LC_Window();
        }
        return isDropDown() ? new DaamkyDropDownGui() : new DaamkyClickGui();
    }

    private static String findExact(String name) {
        if (name != null) {
            for (String type : names()) {
                if (type.equalsIgnoreCase(name.trim())) return type;
            }
        }
        return null;
    }

    private static void load() {
        try {
            Path file = ConfigManager.systemDirectory().resolve(FILE);
            if (!Files.exists(file)) return;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) return;
                if (json.has("type")) {
                    String type = exactIn(NAMES, json.get("type").getAsString());
                    if (type != null) legacyType = type;
                }
                if (json.has("lcType")) {
                    String type = exactIn(LIBERTY_NAMES, json.get("lcType").getAsString());
                    if (type != null) libertyType = type;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the default window type.
        }
    }

    private static String exactIn(List<String> list, String name) {
        if (name != null) {
            for (String type : list) {
                if (type.equalsIgnoreCase(name.trim())) return type;
            }
        }
        return null;
    }

    private static void save() {
        try {
            Files.createDirectories(ConfigManager.systemDirectory());
            Path file = ConfigManager.systemDirectory().resolve(FILE);
            JsonObject json = new JsonObject();
            json.addProperty("type", legacyType);
            json.addProperty("lcType", libertyType);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception ignored) {
            // Not being able to persist the type must never break the GUI.
        }
    }
}
