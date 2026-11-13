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
 * Selected ClickGUI style. Like the client theme it lives in its own file (system/gui.daamky),
 * so loading somebody else's config never switches your GUI behind your back.
 */
public final class GuiStyle {
    public static final String WINDOW = "Window";
    public static final String DROPDOWN = "DropDown";
    public static final List<String> NAMES = List.of(WINDOW, DROPDOWN);

    private static final Gson GSON = new Gson();
    private static final String FILE = "gui.daamky";
    private static String current = load();

    private GuiStyle() {}

    public static String current() { return current; }

    public static boolean isDropDown() { return DROPDOWN.equals(current); }

    /** Returns the canonical style name for the input, or the current style when it is unknown. */
    public static String find(String name) {
        String exact = findExact(name);
        return exact == null ? current : exact;
    }

    public static void select(String name) {
        String exact = findExact(name);
        if (exact != null && !exact.equals(current)) {
            current = exact;
            save();
        }
    }

    /** True for any of the client's own ClickGUI screens, whatever style is selected. */
    public static boolean isClientGui(Screen screen) {
        return screen instanceof DaamkyClickGui || screen instanceof DaamkyDropDownGui;
    }

    /** Creates the ClickGUI screen for the selected style. */
    public static Screen create() {
        return isDropDown() ? new DaamkyDropDownGui() : new DaamkyClickGui();
    }

    private static String findExact(String name) {
        if (name != null) {
            for (String style : NAMES) {
                if (style.equalsIgnoreCase(name.trim())) return style;
            }
        }
        return null;
    }

    private static String load() {
        try {
            Path file = ConfigManager.systemDirectory().resolve(FILE);
            if (!Files.exists(file)) return WINDOW;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has("type")) {
                    String exact = findExact(json.get("type").getAsString());
                    if (exact != null) return exact;
                }
            }
        } catch (Exception ignored) {
            // Fall back to the default window style.
        }
        return WINDOW;
    }

    private static void save() {
        try {
            Files.createDirectories(ConfigManager.systemDirectory());
            Path file = ConfigManager.systemDirectory().resolve(FILE);
            JsonObject json = new JsonObject();
            json.addProperty("type", current);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception ignored) {
            // Not being able to persist the style must never break the GUI.
        }
    }
}
