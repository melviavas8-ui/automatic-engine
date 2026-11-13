package daamky.ua.utils.repository.macro;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.utils.repository.RepositoryStorage;

public final class MacroRepository {
   private static final MacroRepository INSTANCE = new MacroRepository();
   private final List<Macro> macroList = new ArrayList<>();
   private final MinecraftClient mc = MinecraftClient.getInstance();

   private MacroRepository() {
   }

   public static MacroRepository getInstance() {
      return INSTANCE;
   }

   public void load() {
      this.macroList.clear();
      JsonObject root = RepositoryStorage.readObject("macros.daamky");

      for (JsonElement element : root.has("macros") && root.get("macros").isJsonArray() ? root.getAsJsonArray("macros") : new JsonArray()) {
         if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            String name = object.has("name") ? object.get("name").getAsString() : "";
            String message = object.has("message") ? object.get("message").getAsString() : "";
            int key = object.has("key") ? object.get("key").getAsInt() : -1;
            if (!name.isBlank() && !message.isBlank()) {
               this.macroList.add(new Macro(name, message, key));
            }
         }
      }
   }

   public void save() {
      JsonObject root = new JsonObject();
      JsonArray array = new JsonArray();

      for (Macro macro : this.macroList) {
         JsonObject object = new JsonObject();
         object.addProperty("name", macro.name());
         object.addProperty("message", macro.message());
         object.addProperty("key", macro.key());
         array.add(object);
      }

      root.add("macros", array);
      RepositoryStorage.write("macros.daamky", root);
   }

   public void addMacro(String name, String message, int key) {
      this.macroList.add(new Macro(name, message, key));
   }

   public void addMacroAndSave(String name, String message, int key) {
      this.addMacro(name, message, key);
      this.save();
   }

   public boolean hasMacro(String name) {
      return this.macroList.stream().anyMatch(macro -> macro.name().equalsIgnoreCase(name));
   }

   public Optional<Macro> getMacro(String name) {
      return this.macroList.stream().filter(macro -> macro.name().equalsIgnoreCase(name)).findFirst();
   }

   public void deleteMacro(String name) {
      this.macroList.removeIf(macro -> macro.name().equalsIgnoreCase(name));
   }

   public void deleteMacroAndSave(String name) {
      this.deleteMacro(name);
      this.save();
   }

   public void clearList() {
      this.macroList.clear();
   }

   public void clearListAndSave() {
      this.clearList();
      this.save();
   }

   public int size() {
      return this.macroList.size();
   }

   public List<Macro> getMacroList() {
      return this.macroList;
   }

   public List<String> getMacroNames() {
      return this.macroList.stream().map(Macro::name).collect(Collectors.toList());
   }

   public void setMacros(List<Macro> macros) {
      this.macroList.clear();
      if (macros != null) {
         this.macroList.addAll(macros);
      }
   }

   @SubscribeEvent
   private void onKey(KeyEvent event) {
      if (this.mc.player != null && this.mc.currentScreen == null && event.action() == 1) {
         this.macroList.stream().filter(macro -> macro.key() == event.key()).findFirst().ifPresent(macro -> {
            String message = macro.message();
            if (message.startsWith("/")) {
               this.mc.player.networkHandler.sendChatCommand(message.substring(1));
            } else {
               this.mc.player.networkHandler.sendChatMessage(message);
            }
         });
      }
   }
}
