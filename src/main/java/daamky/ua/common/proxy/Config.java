package daamky.ua.common.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import net.fabricmc.loader.api.FabricLoader;
import daamky.ua.api.config.ConfigManager;

public class Config {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final Type ACCOUNTS_TYPE = (new TypeToken<HashMap<String, Proxy>>() {
   }).getType();
   private static final String CONFIG_FILE_NAME = "proxy.daamky";
   public static HashMap<String, Proxy> accounts = new HashMap<>();
   public static String lastPlayerName = "";

   public Config() {
   }

   public static void loadConfig() {
      try {
         Path file = resolveForRead();
         if (!Files.exists(file)) {
            saveConfig();
         } else {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               JsonElement rootElement = JsonParser.parseReader(reader);
               if (rootElement != null && rootElement.isJsonObject()) {
                  JsonObject configJson = rootElement.getAsJsonObject();
                  if (configJson.has("proxy-enabled")) {
                     ProxyServer.proxyEnabled = configJson.get("proxy-enabled").getAsBoolean();
                  }

                  if (configJson.has("accounts")) {
                     accounts = (HashMap<String, Proxy>)GSON.fromJson(configJson.get("accounts"), ACCOUNTS_TYPE);
                  }

                  if (accounts == null) {
                     accounts = new HashMap<>();
                  }

                  ProxyServer.proxy = accounts.getOrDefault("", new Proxy());
                  return;
               }
            }
         }
      } catch (Exception var6) {
         System.out.println("Error reading proxy config");
         var6.printStackTrace();
      }
   }

   public static void setDefaultProxy(Proxy proxy) {
      accounts.put("", proxy);
   }

   public static void saveConfig() {
      try {
         Files.createDirectories(ConfigManager.systemDirectory());
         JsonObject configJson = new JsonObject();
         configJson.addProperty("proxy-enabled", ProxyServer.proxyEnabled);
         configJson.add("accounts", GSON.toJsonTree(accounts));

         try (Writer writer = Files.newBufferedWriter(resolveForWrite(), StandardCharsets.UTF_8)) {
            GSON.toJson(configJson, writer);
         }
      } catch (Exception var6) {
         System.out.println("Error writing proxy config");
         var6.printStackTrace();
      }
   }

   public static void selectProxyForPlayer(String playerName) {
      if (playerName != null && !playerName.equals(lastPlayerName)) {
         lastPlayerName = playerName;
         ProxyServer.proxy = accounts.getOrDefault(playerName, accounts.getOrDefault("", new Proxy()));
      }
   }

   private static Path resolveForWrite() {
      return ConfigManager.systemDirectory().resolve("proxy.daamky");
   }

   private static Path resolveForRead() {
      Path current = resolveForWrite();
      if (Files.exists(current)) {
         return current;
      } else {
         Path legacy = FabricLoader.getInstance().getGameDir().resolve("Dfwfwf").resolve("Proxyconfig.json");
         return Files.exists(legacy) ? legacy : current;
      }
   }
}
