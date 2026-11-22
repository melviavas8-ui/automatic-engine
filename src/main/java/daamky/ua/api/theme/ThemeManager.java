package daamky.ua.api.theme;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.awt.Color;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import daamky.gui.UiStyle;
import daamky.ua.api.config.ConfigManager;

public final class ThemeManager {
   private static final Gson GSON = new Gson();
   private static final String CONFIG_FILE = "theme.daamky";
   /** Themes of the Legacy style. The default one is Midnight. */
   private static final List<ClientTheme> LEGACY_THEMES = List.of(
      theme("Purple", 247, 133, 255, 48, 35, 62),
      theme("Midnight", 105, 145, 255, 21, 27, 43),
      theme("Ocean", 52, 201, 255, 19, 43, 61),
      theme("Aurora", 78, 230, 169, 24, 48, 46),
      theme("Rose", 255, 100, 170, 58, 31, 49),
      theme("Sunset", 255, 146, 72, 58, 38, 34),
      theme("Amber", 255, 199, 74, 54, 45, 27),
      theme("Crimson", 255, 80, 106, 59, 29, 38),
      theme("Dracula", 189, 147, 249, 43, 45, 62),
      theme("Nord", 136, 192, 208, 36, 43, 59),
      theme("Mono", 220, 225, 235, 35, 38, 46)
   );
   /** Themes of the Liberty City style. The default one is Gray. */
   private static final List<ClientTheme> LIBERTY_THEMES = List.of(
      theme("Gray", 176, 181, 190, 30, 32, 36),
      theme("Red", 214, 52, 60, 40, 23, 25),
      theme("Blue", 82, 134, 224, 22, 29, 42)
   );
   private static ClientTheme legacyCurrent = LEGACY_THEMES.get(1);
   private static ClientTheme libertyCurrent = LIBERTY_THEMES.get(0);

   static {
      loadThemes();
   }

   private ThemeManager() {
   }

   private static List<ClientTheme> active() {
      return UiStyle.isLiberty() ? LIBERTY_THEMES : LEGACY_THEMES;
   }

   /** Themes of the active style. */
   public static List<ClientTheme> themes() {
      return active();
   }

   public static List<String> names() {
      return active().stream().map(ClientTheme::name).toList();
   }

   /** Current theme of the active style. */
   public static ClientTheme current() {
      return UiStyle.isLiberty() ? libertyCurrent : legacyCurrent;
   }

   public static ClientTheme find(String name) {
      ClientTheme theme = findExact(name);
      return theme == null ? current() : theme;
   }

   public static void select(String name) {
      ClientTheme theme = findExact(name);
      if (theme != null && theme != current()) {
         if (UiStyle.isLiberty()) {
            libertyCurrent = theme;
         } else {
            legacyCurrent = theme;
         }

         saveTheme();
      }
   }

   private static ClientTheme theme(String name, int red, int green, int blue, int surfaceRed, int surfaceGreen, int surfaceBlue) {
      return new ClientTheme(name, new Color(red, green, blue), new Color(surfaceRed, surfaceGreen, surfaceBlue));
   }

   private static ClientTheme findExact(String name) {
      if (name != null) {
         for (ClientTheme theme : active()) {
            if (theme.name().equalsIgnoreCase(name)) {
               return theme;
            }
         }
      }

      return null;
   }

   private static ClientTheme exactIn(List<ClientTheme> list, String name, ClientTheme fallback) {
      if (name != null) {
         for (ClientTheme theme : list) {
            if (theme.name().equalsIgnoreCase(name)) {
               return theme;
            }
         }
      }

      return fallback;
   }

   private static void loadThemes() {
      try {
         Path file = ConfigManager.systemDirectory().resolve(CONFIG_FILE);
         if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
               if (json != null) {
                  if (json.has("theme")) {
                     legacyCurrent = exactIn(LEGACY_THEMES, json.get("theme").getAsString(), legacyCurrent);
                  }

                  if (json.has("lcTheme")) {
                     libertyCurrent = exactIn(LIBERTY_THEMES, json.get("lcTheme").getAsString(), libertyCurrent);
                  }
               }
            }
         }
      } catch (Exception var7) {
      }
   }

   private static void saveTheme() {
      try {
         Files.createDirectories(ConfigManager.systemDirectory());
         Path file = ConfigManager.systemDirectory().resolve(CONFIG_FILE);
         JsonObject json = new JsonObject();
         json.addProperty("theme", legacyCurrent.name());
         json.addProperty("lcTheme", libertyCurrent.name());

         try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
         }
      } catch (Exception var7) {
      }
   }
}
