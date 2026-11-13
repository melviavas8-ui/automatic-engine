package daamky.ua.screens.mainmenu;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;

final class AltAccountStore {
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

   private AltAccountStore() {
   }

   static List<AltAccountStore.Account> load() {
      Path path = path();
      if (!Files.isRegularFile(path)) {
         return new ArrayList<>();
      } else {
         try {
            Object var12;
            try (Reader reader = Files.newBufferedReader(path)) {
               AltAccountStore.Account[] loaded = (AltAccountStore.Account[])GSON.fromJson(reader, AltAccountStore.Account[].class);
               if (loaded == null) {
                  return new ArrayList<>();
               }

               List<AltAccountStore.Account> result = new ArrayList<>();

               for (AltAccountStore.Account account : loaded) {
                  if (account != null && account.name != null && !account.name.isBlank()) {
                     result.add(account);
                  }
               }

               var12 = result;
            }

            return (List<AltAccountStore.Account>)var12;
         } catch (RuntimeException | IOException var10) {
            return new ArrayList<>();
         }
      }
   }

   static void save(Collection<AltAccountStore.Account> accounts) {
      Path path = path();

      try {
         Files.createDirectories(path.getParent());

         try (Writer writer = Files.newBufferedWriter(path)) {
            GSON.toJson(accounts, writer);
         }
      } catch (IOException var7) {
      }
   }

   static boolean migrateLegacyDefaultPin(List<AltAccountStore.Account> accounts, String currentName) {
      Path marker = path().getParent().resolve(".accounts-v2");
      if (Files.exists(marker)) {
         return false;
      } else {
         boolean changed = accounts.size() == 1 && accounts.get(0).pinned() && accounts.get(0).name().equalsIgnoreCase(currentName);
         if (changed) {
            accounts.get(0).setPinned(false);
         }

         try {
            Files.createDirectories(marker.getParent());
            Files.writeString(marker, "default account pin migrated");
         } catch (IOException var5) {
         }

         return changed;
      }
   }

   private static Path path() {
      return MinecraftClient.getInstance().runDirectory.toPath().resolve("daamky").resolve("accounts.json");
   }

   static final class Account {
      private String name;
      private boolean pinned;
      private boolean selected;

      Account(String name, boolean pinned) {
         this(name, pinned, false);
      }

      Account(String name, boolean pinned, boolean selected) {
         this.name = name;
         this.pinned = pinned;
         this.selected = selected;
      }

      String name() {
         return this.name;
      }

      boolean pinned() {
         return this.pinned;
      }

      void setPinned(boolean pinned) {
         this.pinned = pinned;
      }

      boolean selected() {
         return this.selected;
      }

      void setSelected(boolean selected) {
         this.selected = selected;
      }
   }
}
