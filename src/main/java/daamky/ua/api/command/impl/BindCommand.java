package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.helpers.TabCompleteHelper;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.manager.Manager;
import daamky.ua.utils.string.KeyHelper;

public final class BindCommand extends Command {
   public BindCommand() {
      super("bind", "Manage module binds", "b");
   }

   @Override
   public void execute(String label, String[] args) {
      ModuleManager modules = Manager.getModules();
      if (modules == null) {
         this.logDirect("Module manager is unavailable.", Formatting.RED);
      } else {
         String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
         switch (action) {
            case "set":
            case "add":
               this.setBind(modules, args);
               break;
            case "remove":
            case "del":
            case "delete":
               this.clearBind(modules, args);
               break;
            case "clear":
               this.clearAll(modules);
               break;
            case "list":
               this.listBinds(modules);
               break;
            default:
               this.logDirect("Usage: bind set <module> <key> | bind remove <module> | bind list | bind clear");
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      ModuleManager modules = Manager.getModules();
      if (args.length == 1) {
         return new TabCompleteHelper().append("set", "remove", "list", "clear").sortAlphabetically().filterPrefix(args[0]).stream();
      } else if (modules == null) {
         return Stream.empty();
      } else if (args.length != 2 || !args[0].equalsIgnoreCase("set") && !args[0].equalsIgnoreCase("remove")) {
         return args.length == 3 && args[0].equalsIgnoreCase("set")
            ? new TabCompleteHelper().append(KeyHelper.getAllKeyNames()).filterPrefix(args[2]).stream()
            : Stream.empty();
      } else {
         return new TabCompleteHelper().append(modules.getModules().stream().map(Module::getName).toArray(String[]::new)).filterPrefix(args[1]).stream();
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Manages module key binds.", "Usage:", "> bind set <module> <key>", "> bind remove <module>", "> bind list", "> bind clear");
   }

   private void setBind(ModuleManager modules, String[] args) {
      if (args.length < 3) {
         this.logDirect("Usage: bind set <module> <key>", Formatting.RED);
      } else {
         Module module = this.findModule(modules, args[1]);
         if (module == null) {
            this.logDirect("Module not found: " + args[1], Formatting.RED);
         } else {
            int code = KeyHelper.getKeyCode(args[2]);
            if (code != -1 && code >= 0) {
               module.setBind(this.isMouseName(args[2]) ? KeyBind.mouse(code) : KeyBind.keyboard(code));
               this.saveConfig();
               this.logDirect(module.getName() + " bound to " + module.getBind().getDisplayName(), Formatting.GREEN);
            } else {
               this.logDirect("Unknown key: " + args[2], Formatting.RED);
            }
         }
      }
   }

   private void clearBind(ModuleManager modules, String[] args) {
      if (args.length < 2) {
         this.logDirect("Usage: bind remove <module>", Formatting.RED);
      } else {
         Module module = this.findModule(modules, args[1]);
         if (module == null) {
            this.logDirect("Module not found: " + args[1], Formatting.RED);
         } else {
            module.setBind(KeyBind.NONE);
            this.saveConfig();
            this.logDirect(module.getName() + " bind removed.", Formatting.GREEN);
         }
      }
   }

   private void clearAll(ModuleManager modules) {
      for (Module module : modules.getModules()) {
         module.setBind(KeyBind.NONE);
      }

      this.saveConfig();
      this.logDirect("All binds removed.", Formatting.GREEN);
   }

   private void listBinds(ModuleManager modules) {
      List<Module> bound = modules.getModules()
         .stream()
         .filter(modulex -> modulex.getBind().isBound())
         .sorted(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER))
         .toList();
      if (bound.isEmpty()) {
         this.logDirect("No module binds.", Formatting.RED);
      } else {
         for (Module module : bound) {
            this.logDirect(module.getName() + " -> " + module.getBind().getDisplayName());
         }
      }
   }

   private Module findModule(ModuleManager modules, String name) {
      return modules.getByName(name).orElse(null);
   }

   private boolean isMouseName(String name) {
      String value = name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
      return value.startsWith("MOUSE") || value.matches("M\\d+") || value.equals("LMB") || value.equals("RMB") || value.equals("MMB");
   }

   private void saveConfig() {
      Manager manager = Manager.getInstance();
      if (manager != null && manager.getConfigManager() != null) {
         manager.getConfigManager().saveAll();
      }
   }
}
