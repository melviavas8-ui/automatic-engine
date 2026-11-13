package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.helpers.TabCompleteHelper;
import daamky.ua.utils.repository.macro.Macro;
import daamky.ua.utils.repository.macro.MacroRepository;
import daamky.ua.utils.string.KeyHelper;

public final class MacroCommand extends Command {
   public MacroCommand() {
      super("macro", "Manage macros");
   }

   @Override
   public void execute(String label, String[] args) {
      MacroRepository repository = MacroRepository.getInstance();
      String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
      switch (action) {
         case "add":
            if (args.length < 4) {
               this.logDirect("Usage: macro add <key> <name> <message>", Formatting.RED);
               return;
            }

            int key = KeyHelper.getKeyCode(args[1]);
            if (key < 0) {
               this.logDirect("Unknown key: " + args[1], Formatting.RED);
               return;
            }

            String name = args[2];
            String message = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            repository.addMacroAndSave(name, message, key);
            this.logDirect("Macro " + name + " added.", Formatting.GREEN);
            break;
         case "remove":
         case "del":
         case "delete":
            if (args.length < 2) {
               this.logDirect("Usage: macro remove <name>", Formatting.RED);
               return;
            }

            repository.deleteMacroAndSave(args[1]);
            this.logDirect("Macro " + args[1] + " removed.", Formatting.GREEN);
            break;
         case "clear":
            int count = repository.size();
            repository.clearListAndSave();
            this.logDirect("Macros cleared. Removed: " + count, Formatting.GREEN);
            break;
         case "list":
            if (repository.getMacroList().isEmpty()) {
               this.logDirect("Macro list is empty.", Formatting.RED);
               return;
            }

            for (Macro macro : repository.getMacroList()) {
               this.logDirect("§f" + macro.name() + " §8[" + KeyHelper.getKeyName(macro.key()) + "] §7" + macro.message());
            }
            break;
         default:
            this.logDirect("Usage: macro add/remove/list/clear");
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return new TabCompleteHelper().append("add", "remove", "list", "clear").sortAlphabetically().filterPrefix(args[0]).stream();
      } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
         return new TabCompleteHelper().append(KeyHelper.getAllKeyNames()).filterPrefix(args[1]).stream();
      } else {
         return args.length == 2 && args[0].equalsIgnoreCase("remove")
            ? new TabCompleteHelper().append(MacroRepository.getInstance().getMacroNames().toArray(new String[0])).filterPrefix(args[1]).stream()
            : Stream.empty();
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Manages macros.", "Usage:", "> macro add <key> <name> <message>", "> macro remove <name>", "> macro list", "> macro clear");
   }
}
