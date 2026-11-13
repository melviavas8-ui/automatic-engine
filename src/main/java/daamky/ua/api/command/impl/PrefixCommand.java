package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.CommandManager;

public final class PrefixCommand extends Command {
   public PrefixCommand() {
      super("prefix", "Changes command prefix");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      if (args.length == 0) {
         this.logDirect("Current prefix: " + manager.getPrefix());
         this.logDirect("Usage: prefix set <symbol>");
      } else if (args[0].equalsIgnoreCase("set") && args.length >= 2) {
         String newPrefix = args[1];
         if (newPrefix.length() <= 3 && !newPrefix.contains(" ")) {
            manager.setPrefix(newPrefix);
            this.logDirect("Prefix changed to: " + newPrefix, Formatting.GREEN);
         } else {
            this.logDirect("Prefix must be 1-3 chars without spaces.", Formatting.RED);
         }
      } else {
         this.logDirect("Usage: prefix set <symbol>", Formatting.RED);
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return Stream.of("set").filter(value -> value.startsWith(args[0].toLowerCase()));
      } else {
         return args.length == 2 && args[0].equalsIgnoreCase("set")
            ? Stream.of(".", "!", "$", "#", "-", "/").filter(value -> value.startsWith(args[1]))
            : Stream.empty();
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Changes command prefix.", "Usage:", "> prefix", "> prefix set <symbol>");
   }
}
