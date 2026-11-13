package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.CommandManager;
import daamky.ua.api.command.helpers.Paginator;
import daamky.ua.api.command.helpers.TabCompleteHelper;

public final class HelpCommand extends Command {
   public HelpCommand() {
      super("help", "Shows available commands");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      if (args.length != 0 && !this.isInteger(args[0])) {
         Command command = manager.getCommand(args[0]);
         if (command == null) {
            this.logDirect("Command '" + args[0] + "' not found.", Formatting.RED);
         } else {
            this.logDirectRaw(Text.literal(getLine()));
            this.logDirect(command.getName().toUpperCase());
            this.logDirectRaw(Text.literal(getLine()));

            for (String line : command.getLongDesc()) {
               if (!line.isEmpty()) {
                  this.logDirect(line);
               }
            }

            this.logDirectRaw(Text.literal(getLine()));
         }
      } else {
         int page = args.length > 0 && this.isInteger(args[0]) ? Integer.parseInt(args[0]) : 1;
         List<Command> commands = manager.getCommands().stream().filter(commandx -> !commandx.hiddenFromHelp()).collect(Collectors.toList());
         Paginator<Command> paginator = new Paginator<>(commands);
         paginator.setPage(page);
         paginator.display(
            () -> {
               this.logDirectRaw(Text.literal(getLine()));
               this.logDirect("Available commands");
               this.logDirectRaw(Text.literal(getLine()));
            },
            commandx -> {
               String fullName = manager.getPrefix() + commandx.getName();
               MutableText hover = Text.literal(fullName)
                  .formatted(Formatting.WHITE)
                  .append(Text.literal("\n" + commandx.getShortDesc()).formatted(Formatting.GRAY));
               return Text.literal(fullName)
                  .formatted(Formatting.WHITE)
                  .append(Text.literal(" - " + commandx.getShortDesc()).formatted(Formatting.GRAY))
                  .styled(
                     style -> style.withHoverEvent(new ShowText(hover)).withClickEvent(new RunCommand(manager.getPrefix() + label + " " + commandx.getName()))
                  );
            },
            manager.getPrefix() + label
         );
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      return args.length == 1 ? new TabCompleteHelper().filterPrefix(args[0]).addCommands(CommandManager.getInstance()).stream() : Stream.empty();
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Shows command help.", "Usage:", "> help", "> help <command>");
   }

   private boolean isInteger(String value) {
      try {
         Integer.parseInt(value);
         return true;
      } catch (NumberFormatException var3) {
         return false;
      }
   }

   public static String getLine() {
      MinecraftClient mc = MinecraftClient.getInstance();
      int dashCount = mc != null && mc.textRenderer != null
         ? Math.max(18, Math.min(80, mc.getWindow().getScaledWidth() / Math.max(4, mc.textRenderer.getWidth("-")) / 3))
         : 24;
      return "§8§m" + "-".repeat(dashCount);
   }
}
