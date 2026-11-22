package daamky.ua.api.drag.impl;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import daamky.ua.api.config.ConfigManager;

/**
 * Система языков клиента. Тексты лежат в ресурсах: assets/daamky/clientlang/{uk,ru,en}.json.
 * Формат файла: {"names": {...}, "descriptions": {...}, "words": [[from,to],...], "descWords": [[from,to],...]}.
 * Названия модулей (assets/daamky/clientlang/protected.json) никогда не переводятся.
 * Язык по умолчанию: украинский. Если ключа нет в файле языка, используется запасная замена слов (words),
 * а если и её нет, показывается исходный английский текст.
 */
public final class ClientLanguage {
   private static final Gson GSON = new Gson();
   private static final String CONFIG_FILE = "language.daamky";
   private static final String RESOURCE_DIR = "/assets/daamky/clientlang/";
   private static final int FALLBACK_CACHE_LIMIT = 4096;
   private static final Map<ClientLanguage.Lang, Pack> PACKS = new EnumMap<>(ClientLanguage.Lang.class);
   private static final Set<String> PROTECTED = new HashSet<>();
   private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
   private static final Map<String, String> NAME_FALLBACK_CACHE = new ConcurrentHashMap<>();
   private static final Map<String, String> DESCRIPTION_FALLBACK_CACHE = new ConcurrentHashMap<>();
   private static ClientLanguage.Lang current = ClientLanguage.Lang.UK;

   private ClientLanguage() {
   }

   public static String tr(String text) {
      if (text == null || text.isBlank()) {
         return text == null ? "" : text;
      } else if (PROTECTED.contains(text)) {
         return text;
      } else {
         Pack pack = pack(current);
         String translated = pack.names.get(text);
         if (translated != null) {
            return translated;
         }

         translated = pack.descriptions.get(text);
         return translated != null ? translated : cachedFallback(NAME_FALLBACK_CACHE, pack, text, false);
      }
   }

   public static String trDescription(String text) {
      if (text == null || text.isBlank()) {
         return text == null ? "" : text;
      } else {
         Pack pack = pack(current);
         String translated = pack.descriptions.get(text);
         if (translated != null) {
            return translated;
         }

         translated = pack.names.get(text);
         return translated != null ? translated : cachedFallback(DESCRIPTION_FALLBACK_CACHE, pack, text, true);
      }
   }

   private static String cachedFallback(Map<String, String> cache, Pack pack, String text, boolean description) {
      if (pack.words.isEmpty() && (!description || pack.descWords.isEmpty())) {
         return text;
      }

      String cached = cache.get(text);
      if (cached != null) {
         return cached;
      }

      String value = text;
      for (String[] pair : pack.words) {
         value = value.replace(pair[0], pair[1]);
      }

      if (description) {
         for (String[] pair : pack.descWords) {
            value = value.replace(pair[0], pair[1]);
         }
      }

      if (cache.size() >= FALLBACK_CACHE_LIMIT) {
         cache.clear();
      }

      cache.put(text, value);
      return value;
   }

   /** Переключает на следующий язык по кругу. */
   public static void toggle() {
      ClientLanguage.Lang[] values = ClientLanguage.Lang.values();
      set(values[(current.ordinal() + 1) % values.length]);
   }

   public static void set(ClientLanguage.Lang lang) {
      if (lang != null && lang != current) {
         current = lang;
         NAME_FALLBACK_CACHE.clear();
         DESCRIPTION_FALLBACK_CACHE.clear();
         saveLanguage();
         for (Runnable listener : LISTENERS) {
            listener.run();
         }
      }
   }

   public static ClientLanguage.Lang current() {
      return current;
   }

   public static String buttonText() {
      return current.name();
   }

   /** Вызывается при смене языка (например, чтобы синхронизировать настройку в Appearance). */
   public static void addChangeListener(Runnable listener) {
      LISTENERS.add(listener);
   }

   private static Pack pack(ClientLanguage.Lang lang) {
      return PACKS.computeIfAbsent(lang, ClientLanguage::loadPack);
   }

   private static Pack loadPack(ClientLanguage.Lang lang) {
      Pack pack = new Pack();
      JsonObject json = readResource(lang.code() + ".json", JsonObject.class);
      if (json != null) {
         readStringMap(json, "names", pack.names);
         readStringMap(json, "descriptions", pack.descriptions);
         readPairs(json, "words", pack.words);
         readPairs(json, "descWords", pack.descWords);
      }

      return pack;
   }

   private static void readStringMap(JsonObject json, String key, Map<String, String> target) {
      if (json.has(key) && json.get(key).isJsonObject()) {
         for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject(key).entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
               target.put(entry.getKey(), entry.getValue().getAsString());
            }
         }
      }
   }

   private static void readPairs(JsonObject json, String key, List<String[]> target) {
      if (json.has(key) && json.get(key).isJsonArray()) {
         for (JsonElement element : json.getAsJsonArray(key)) {
            if (element.isJsonArray() && element.getAsJsonArray().size() == 2) {
               JsonArray pair = element.getAsJsonArray();
               target.add(new String[]{pair.get(0).getAsString(), pair.get(1).getAsString()});
            }
         }
      }
   }

   private static <T> T readResource(String name, Class<T> type) {
      try (InputStream stream = ClientLanguage.class.getResourceAsStream(RESOURCE_DIR + name)) {
         if (stream == null) {
            return null;
         }

         try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, type);
         }
      } catch (Exception var8) {
         return null;
      }
   }

   private static void loadProtected() {
      JsonArray array = readResource("protected.json", JsonArray.class);
      if (array != null) {
         for (JsonElement element : array) {
            PROTECTED.add(element.getAsString());
         }
      }
   }

   private static void loadLanguage() {
      try {
         Path file = ConfigManager.systemDirectory().resolve(CONFIG_FILE);
         if (!Files.exists(file)) {
            return;
         }

         try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
            // Ключ "lang" появился вместе с украинским: старый ключ "language" (EN/RU) игнорируем, по умолчанию UK.
            if (json != null && json.has("lang")) {
               current = ClientLanguage.Lang.fromCode(json.get("lang").getAsString());
            }
         }
      } catch (Exception var6) {
      }
   }

   private static void saveLanguage() {
      try {
         Files.createDirectories(ConfigManager.systemDirectory());
         Path file = ConfigManager.systemDirectory().resolve(CONFIG_FILE);
         JsonObject json = new JsonObject();
         json.addProperty("lang", current.code());

         try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
         }
      } catch (Exception var6) {
      }
   }

   static {
      loadProtected();
      loadLanguage();
   }

   private static final class Pack {
      final Map<String, String> names = new HashMap<>();
      final Map<String, String> descriptions = new HashMap<>();
      final List<String[]> words = new ArrayList<>();
      final List<String[]> descWords = new ArrayList<>();
   }

   public static enum Lang {
      UK("uk", "Українська"),
      RU("ru", "Русский"),
      EN("en", "English");

      private final String code;
      private final String title;

      private Lang(String code, String title) {
         this.code = code;
         this.title = title;
      }

      public String code() {
         return this.code;
      }

      public String title() {
         return this.title;
      }

      public static ClientLanguage.Lang fromCode(String code) {
         for (ClientLanguage.Lang lang : values()) {
            if (lang.code.equalsIgnoreCase(code) || lang.name().equalsIgnoreCase(code)) {
               return lang;
            }
         }

         return UK;
      }
   }
}
