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
import daamky.ua.api.config.ConfigManager;

public final class ThemeManager {
   private static final Gson GSON = new Gson();
   private static final String CONFIG_FILE = "theme.daamky";
   private static final List<ClientTheme> THEMES = List.of(
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
   private static ClientTheme current = loadTheme();

   private ThemeManager() {
   }

   public static List<ClientTheme> themes() {
      return THEMES;
   }

   public static List<String> names() {
      return THEMES.stream().map(ClientTheme::name).toList();
   }

   public static ClientTheme current() {
      return current;
   }

   public static ClientTheme find(String name) {
      ClientTheme theme = findExact(name);
      return theme == null ? current : theme;
   }

   public static void select(String name) {
      ClientTheme theme = findExact(name);
      if (theme != null && theme != current) {
         current = theme;
         saveTheme();
      }
   }

   private static ClientTheme theme(String name, int red, int green, int blue, int surfaceRed, int surfaceGreen, int surfaceBlue) {
      return new ClientTheme(name, new Color(red, green, blue), new Color(surfaceRed, surfaceGreen, surfaceBlue));
   }

   private static ClientTheme findExact(String name) {
      if (name != null) {
         for (ClientTheme theme : THEMES) {
            if (theme.name().equalsIgnoreCase(name)) {
               return theme;
            }
         }
      }

      return null;
   }

   private static ClientTheme loadTheme() {
      try {
         Path file = ConfigManager.systemDirectory().resolve("theme.daamky");
         if (!Files.exists(file)) {
            return THEMES.get(0);
         } else {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
               if (json != null && json.has("theme")) {
                  ClientTheme theme = findExact(json.get("theme").getAsString());
                  if (theme != null) {
                     return theme;
                  }
               }

               return THEMES.get(0);
            }
         }
      } catch (Exception var7) {
         return THEMES.get(0);
      }
   }

   private static void saveTheme() {
      try {
         Files.createDirectories(ConfigManager.systemDirectory());
         Path file = ConfigManager.systemDirectory().resolve("theme.daamky");
         JsonObject json = new JsonObject();
         json.addProperty("theme", current.name());

         try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            GSON.toJson(json, writer);
         }
      } catch (Exception var7) {
      }
   }
}
