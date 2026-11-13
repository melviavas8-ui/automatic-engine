package daamky.ua.screens.mainmenu;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.EntryListWidget;
import daamky.ua.mixin.accessor.AbstractSelectionListAccessor;

public final class WorldOrderStore {
   private WorldOrderStore() {
   }

   public static void apply(EntryListWidget<?> list) {
      List<String> order = load();
      if (!order.isEmpty()) {
         List<?> entries = list.children();
         List<Integer> worldSlots = worldSlots(entries);
         int destination = 0;

         for (String levelId : order) {
            int source = findWorld(entries, worldSlots, destination, levelId);
            if (source >= 0 && destination < worldSlots.size()) {
               int target = worldSlots.get(destination++);
               if (source != target) {
                  ((AbstractSelectionListAccessor)list).daamky$swapEntries(source, target);
               }
            }
         }
      }
   }

   public static void save(EntryListWidget<?> list) {
      List<String> lines = new ArrayList<>();
      Encoder encoder = Base64.getUrlEncoder().withoutPadding();

      for (Object entry : list.children()) {
         if (entry instanceof WorldListEntryOrderAccess world) {
            lines.add(encoder.encodeToString(world.daamky$getLevelId().getBytes(StandardCharsets.UTF_8)));
         }
      }

      try {
         Path file = orderFile();
         Files.createDirectories(file.getParent());
         Files.write(file, lines, StandardCharsets.UTF_8);
      } catch (IOException var6) {
      }
   }

   private static List<String> load() {
      Path file = orderFile();
      if (!Files.isRegularFile(file)) {
         return List.of();
      } else {
         Decoder decoder = Base64.getUrlDecoder();
         List<String> order = new ArrayList<>();

         try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
               if (!line.isBlank()) {
                  order.add(new String(decoder.decode(line), StandardCharsets.UTF_8));
               }
            }

            return order;
         } catch (IllegalArgumentException | IOException var5) {
            return List.of();
         }
      }
   }

   private static List<Integer> worldSlots(List<?> entries) {
      List<Integer> slots = new ArrayList<>();

      for (int i = 0; i < entries.size(); i++) {
         if (entries.get(i) instanceof WorldListEntryOrderAccess) {
            slots.add(i);
         }
      }

      return slots;
   }

   private static int findWorld(List<?> entries, List<Integer> slots, int start, String levelId) {
      for (int i = start; i < slots.size(); i++) {
         int slot = slots.get(i);
         if (entries.get(slot) instanceof WorldListEntryOrderAccess world && world.daamky$getLevelId().equals(levelId)) {
            return slot;
         }
      }

      return -1;
   }

   private static Path orderFile() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("config").resolve("daamky-world-order.txt");
   }
}
