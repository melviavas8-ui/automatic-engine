package daamky.gui;

import daamky.ua.api.config.ConfigManager;
import daamky.ua.manager.Manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Shared config actions for both ClickGUI styles (Window and DropDown). */
public final class ConfigsBridge {
    public static final int OK = 0, ERROR = 1, INFO = 2;

    /** Text typed into the "config name" field. */
    public static String name = "";
    /** Config currently selected in the list (file name without extension), or null. */
    public static String selected;

    private static String status = "";
    private static int statusKind;
    private static long statusUntil;
    private static List<String> cache = List.of();
    private static long cacheAt;

    private ConfigsBridge() {}

    private static ConfigManager manager() {
        Manager m = Manager.getInstance();
        return m == null ? null : m.getConfigManager();
    }

    private static ConfigManager require() {
        ConfigManager m = manager();
        if (m == null) report(ERROR, "Менеджер конфигов недоступен");
        return m;
    }

    public static List<String> list() {
        if (System.currentTimeMillis() - cacheAt > 2000L) refresh();
        return cache;
    }

    public static void refresh() {
        cacheAt = System.currentTimeMillis();
        ConfigManager m = manager();
        if (m == null) {
            cache = List.of();
            return;
        }
        try {
            cache = List.copyOf(m.listUserConfigs());
        } catch (RuntimeException e) {
            cache = List.of();
        }
        if (selected != null && !cache.contains(selected)) selected = null;
    }

    /** Saves under the typed name, or overwrites the selected config when the field is empty. */
    public static void save() {
        ConfigManager m = require();
        if (m == null) return;
        String raw = name == null ? "" : name.trim();
        String target = raw.isEmpty() ? selected : ConfigManager.normalizeName(raw);
        if (target == null || target.isBlank()) {
            report(ERROR, raw.isEmpty() ? "Введи имя конфига" : "Недопустимое имя конфига");
            return;
        }
        try {
            m.save(target);
            name = "";
            refresh();
            selected = target;
            report(OK, "Сохранён: " + target);
        } catch (RuntimeException e) {
            report(ERROR, "Не удалось сохранить: " + reason(e));
        }
    }

    public static void loadSelected() {
        ConfigManager m = require();
        if (m == null) return;
        String target = selected;
        if (target == null) {
            report(ERROR, "Выбери конфиг в списке");
            return;
        }
        try {
            if (!m.exists(target)) {
                refresh();
                report(ERROR, "Файл конфига не найден");
                return;
            }
            m.load(target);
            // Persist the loaded state as the current one, otherwise it is lost on restart.
            m.saveAll();
            Modules.refreshFromTarget();
            report(OK, "Загружен: " + target);
        } catch (RuntimeException e) {
            report(ERROR, "Не удалось загрузить: " + reason(e));
        }
    }

    public static void deleteSelected() {
        ConfigManager m = require();
        if (m == null) return;
        String target = selected;
        if (target == null) {
            report(ERROR, "Выбери конфиг в списке");
            return;
        }
        try {
            m.delete(target);
            selected = null;
            refresh();
            report(OK, "Удалён: " + target);
        } catch (RuntimeException e) {
            report(ERROR, "Не удалось удалить: " + reason(e));
        }
    }

    public static void openFolder() {
        Path dir = ConfigManager.userConfigDirectory();
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
            // Opening may still work if the folder already exists.
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String path = dir.toAbsolutePath().toString();
            ProcessBuilder builder = os.contains("win") ? new ProcessBuilder("explorer.exe", path)
                    : os.contains("mac") ? new ProcessBuilder("open", path) : new ProcessBuilder("xdg-open", path);
            builder.start();
            refresh();
            report(INFO, "Папка открыта");
        } catch (Exception e) {
            report(INFO, "Папка: " + dir.toAbsolutePath());
        }
    }

    public static String status() {
        return System.currentTimeMillis() > statusUntil ? "" : status;
    }

    public static int statusKind() { return statusKind; }

    private static void report(int kind, String text) {
        status = text;
        statusKind = kind;
        statusUntil = System.currentTimeMillis() + (kind == INFO ? 6000L : 4000L);
    }

    private static String reason(RuntimeException e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        String text = cause.getMessage();
        return text == null || text.isBlank() ? cause.getClass().getSimpleName() : text;
    }
}
