package daamky.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import daamky.ua.api.config.ConfigManager;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Overall look of the client: Legacy (the classic GUI, HUD and themes) or Liberty City.
 * Lives in its own file (system/style.daamky), like the theme and the GUI type, so loading
 * somebody else's config never switches your style. The file does not exist until the player
 * has picked a style once (first-launch questionnaire or Appearance), see {@link #needsSetup()}.
 */
public final class UiStyle {
    public static final String LEGACY = "Legacy";
    public static final String LIBERTY = "Liberty City";
    public static final List<String> NAMES = List.of(LEGACY, LIBERTY);

    private static final Gson GSON = new Gson();
    private static final String FILE = "style.daamky";
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static boolean configured;
    private static String current = load();

    private UiStyle() {}

    public static String current() { return current; }

    public static boolean isLiberty() { return LIBERTY.equals(current); }

    public static boolean isLegacy() { return !isLiberty(); }

    /** True until the player has chosen a style at least once (nothing saved yet). */
    public static boolean needsSetup() { return !configured; }

    /** Returns the canonical style name for the input, or the current style when it is unknown. */
    public static String find(String name) {
        String exact = findExact(name);
        return exact == null ? current : exact;
    }

    /** Selects a style and remembers that the choice was made. Listeners run only when the style actually changed. */
    public static void select(String name) {
        String exact = findExact(name);
        if (exact == null) return;
        boolean changed = !exact.equals(current);
        boolean first = !configured;
        current = exact;
        configured = true;
        if (changed || first) save();
        if (changed) {
            for (Runnable listener : LISTENERS) listener.run();
        }
    }

    /** Called after the style changed (Appearance uses it to refresh its theme and GUI type lists). */
    public static void addChangeListener(Runnable listener) {
        if (listener != null) LISTENERS.add(listener);
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
            if (!Files.exists(file)) return LEGACY;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null && json.has("style")) {
                    String exact = findExact(json.get("style").getAsString());
                    if (exact != null) {
                        configured = true;
                        return exact;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to the legacy style.
        }
        return LEGACY;
    }

    private static void save() {
        try {
            Files.createDirectories(ConfigManager.systemDirectory());
            Path file = ConfigManager.systemDirectory().resolve(FILE);
            JsonObject json = new JsonObject();
            json.addProperty("style", current);
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(json, writer);
            }
        } catch (Exception ignored) {
            // Not being able to persist the style must never break the client.
        }
    }
}
