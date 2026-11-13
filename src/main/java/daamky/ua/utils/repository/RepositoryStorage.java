package daamky.ua.utils.repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import daamky.ua.api.config.ConfigManager;

public final class RepositoryStorage {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

   private RepositoryStorage() {
   }

   public static Path root() {
      return ConfigManager.systemDirectory();
   }

   public static List<String> loadStringList(String fileName, String key) {
      List<String> result = new ArrayList<>();
      Path file = resolveForRead(fileName);
      if (!Files.exists(file)) {
         return result;
      } else {
         try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();

            for (JsonElement element : object.has(key) && object.get(key).isJsonArray() ? object.getAsJsonArray(key) : new JsonArray()) {
               if (element.isJsonPrimitive()) {
                  result.add(element.getAsString());
               }
            }
         } catch (Exception var11) {
         }

         return result;
      }
   }

   public static void saveStringList(String fileName, String key, List<String> values) {
      JsonObject object = new JsonObject();
      JsonArray array = new JsonArray();
      if (values != null) {
         for (String value : values) {
            array.add(value);
         }
      }

      object.add(key, array);
      write(fileName, object);
   }

   public static JsonObject readObject(String fileName) {
      Path file = resolveForRead(fileName);
      if (!Files.exists(file)) {
         return new JsonObject();
      } else {
         try {
            JsonObject var4;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               JsonElement element = JsonParser.parseReader(reader);
               var4 = element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
            }

            return var4;
         } catch (Exception var7) {
            return new JsonObject();
         }
      }
   }

   public static void write(String fileName, JsonObject object) {
      try {
         Files.createDirectories(root());

         try (Writer writer = Files.newBufferedWriter(resolveForWrite(fileName), StandardCharsets.UTF_8)) {
            GSON.toJson(object, writer);
         }
      } catch (Exception var7) {
      }
   }

   public static String daamkyFile(String baseName) {
      String normalized = baseName == null ? "" : baseName.trim();
      if (normalized.endsWith(".daamky")) {
         return normalized;
      } else {
         int dot = normalized.lastIndexOf(46);
         if (dot >= 0) {
            normalized = normalized.substring(0, dot);
         }

         return normalized + ".daamky";
      }
   }

   private static Path resolveForWrite(String fileName) {
      return root().resolve(daamkyFile(fileName));
   }

   private static Path resolveForRead(String fileName) {
      Path daamkyFile = resolveForWrite(fileName);
      if (Files.exists(daamkyFile)) {
         return daamkyFile;
      } else {
         Path legacyFile = root().resolve(fileName);
         if (Files.exists(legacyFile)) {
            return legacyFile;
         } else {
            Path oldConfigFile = ConfigManager.legacyDirectory().resolve(fileName);
            if (Files.exists(oldConfigFile)) {
               return oldConfigFile;
            } else {
               Path oldConfigdaamkyFile = ConfigManager.legacyDirectory().resolve(daamkyFile(fileName));
               return Files.exists(oldConfigdaamkyFile) ? oldConfigdaamkyFile : daamkyFile;
            }
         }
      }
   }
}
