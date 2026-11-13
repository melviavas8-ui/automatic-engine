package daamky.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;







public final class Fonts {
    private static final Map<String, FontAtlas> CACHE = new HashMap<>();
    private static List<String> names;
    private static FontAtlas regular, bold, icons;
    private static boolean tried;

    private static List<String> names() {
        if (names == null) {
            names = new ArrayList<>();
            try (var in = MinecraftClient.getInstance().getResourceManager().open(Identifier.of("daamky", "fonts/index.json"))) {
                for (JsonElement e : JsonParser.parseReader(new InputStreamReader(in)).getAsJsonArray()) names.add(e.getAsString());
            } catch (Exception ignored) {}
        }
        return names;
    }

    private static String find(String key) {
        for (String n : names()) if (!n.equals("icons") && n.contains(key)) return n;
        return null;
    }

    private static void resolve() {
        if (tried) return;
        tried = true;
        String r = find("regular");
        if (r == null) r = find("medium");
        if (r == null) for (String n : names()) if (!n.equals("icons") && !n.contains("bold")) { r = n; break; }
        if (r == null) for (String n : names()) if (!n.equals("icons")) { r = n; break; }
        String b = find("semibold");
        if (b == null) b = find("bold");
        if (r != null) regular = get(r);
        if (b != null) bold = get(b);
        icons = get("icons");
    }

    public static FontAtlas regular() { resolve(); return regular; }

    public static FontAtlas bold() { resolve(); return bold; }
    public static FontAtlas icons() { resolve(); return icons; }

    public static FontAtlas get(String name) { return CACHE.computeIfAbsent(name, FontAtlas::load); }
}
