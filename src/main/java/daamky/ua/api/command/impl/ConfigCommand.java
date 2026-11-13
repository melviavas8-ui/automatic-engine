package daamky.ua.api.command.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.Command;
import daamky.ua.api.config.ConfigManager;
import daamky.ua.api.config.exception.ConfigException;
import daamky.ua.manager.Manager;
import daamky.ua.utils.repository.RepositoryStorage;

public final class ConfigCommand extends Command {
   public ConfigCommand() {
      super("config", "Configuration management", "cfg");
   }

   @Override
   public void execute(String label, String[] args) {
      String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "save";
      switch (action) {
         case "save":
            ConfigManager configManagerxx = this.configManager();
            if (configManagerxx != null) {
               if (args.length > 1) {
                  configManagerxx.save(args[1]);
                  this.logDirect("Config saved: " + args[1], Formatting.GREEN);
               } else {
                  configManagerxx.saveAll();
                  this.logDirect("Config saved.", Formatting.GREEN);
               }
            }
            break;
         case "load":
            ConfigManager configManagerx = this.configManager();
            if (configManagerx == null) {
               return;
            }

            if (args.length < 2) {
               this.logDirect("Usage: config load <name>", Formatting.RED);
               return;
            }

            try {
               configManagerx.load(args[1]);
               this.logDirect("Config loaded: " + args[1], Formatting.GREEN);
            } catch (ConfigException var10) {
               this.logDirect("Failed to load config: " + var10.getMessage(), Formatting.RED);
            }
            break;
         case "list":
            ConfigManager configManager = this.configManager();
            if (configManager == null) {
               return;
            }

            List<String> configs = configManager.listUserConfigs();
            if (configs.isEmpty()) {
               this.logDirect("Config list is empty.", Formatting.RED);
               return;
            }

            this.logDirect("Configs: " + String.join(", ", configs));
            break;
         case "dir":
            try {
               this.openDirectory(ConfigManager.userConfigDirectory());
               this.logDirect("Config folder opened.", Formatting.GREEN);
            } catch (IOException var9) {
               this.logDirect("Failed to open config folder: " + var9.getMessage(), Formatting.RED);
            }
            break;
         case "system":
            try {
               this.openDirectory(RepositoryStorage.root());
               this.logDirect("System folder opened.", Formatting.GREEN);
            } catch (IOException var8) {
               this.logDirect("Failed to open system folder: " + var8.getMessage(), Formatting.RED);
            }
            break;
         default:
            this.logDirect("Usage: config save [name] | config load <name> | config list | config dir | config system");
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return Stream.of("save", "load", "list", "dir", "system").filter(value -> value.startsWith(args[0].toLowerCase(Locale.ROOT)));
      } else if (args.length == 2 && args[0].equalsIgnoreCase("load")) {
         ConfigManager configManager = this.configManager();
         return configManager == null
            ? Stream.empty()
            : configManager.listUserConfigs().stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)));
      } else {
         return Stream.empty();
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Saves and loads configs.",
         "Usage:",
         "> config save",
         "> config save <name>",
         "> config load <name>",
         "> config list",
         "> config dir",
         "> config system"
      );
   }

   private ConfigManager configManager() {
      if (Manager.getInstance() != null && Manager.getInstance().getConfigManager() != null) {
         return Manager.getInstance().getConfigManager();
      } else {
         this.logDirect("Config manager is unavailable.", Formatting.RED);
         return null;
      }
   }

   private void openDirectory(Path directory) throws IOException {
      String os = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      ProcessBuilder builder = os.contains("win")
         ? new ProcessBuilder("explorer.exe", directory.toAbsolutePath().toString())
         : (
            os.contains("mac")
               ? new ProcessBuilder("open", directory.toAbsolutePath().toString())
               : new ProcessBuilder("xdg-open", directory.toAbsolutePath().toString())
         );
      builder.start();
   }
}
