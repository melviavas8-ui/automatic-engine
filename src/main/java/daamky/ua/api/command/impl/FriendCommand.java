package daamky.ua.api.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.text.ClickEvent.RunCommand;
import net.minecraft.text.HoverEvent.ShowText;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.CommandManager;
import daamky.ua.api.command.helpers.Paginator;
import daamky.ua.api.command.helpers.TabCompleteHelper;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class FriendCommand extends Command {
   public FriendCommand() {
      super("friend", "Manage friends", "f", "friends");
   }

   @Override
   public void execute(String label, String[] args) {
      CommandManager manager = CommandManager.getInstance();
      String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
      switch (action) {
         case "add":
            if (args.length < 2) {
               this.logDirect("Usage: friend add <name>", Formatting.RED);
               return;
            }

            if (FriendUtils.isFriend(args[1])) {
               this.logDirect(args[1] + " is already friend.", Formatting.RED);
               return;
            }

            FriendUtils.addFriendAndSave(args[1]);
            this.logDirect(args[1] + " added to friends.", Formatting.GREEN);
            break;
         case "remove":
         case "del":
         case "delete":
            if (args.length < 2) {
               this.logDirect("Usage: friend remove <name>", Formatting.RED);
               return;
            }

            FriendUtils.removeFriendAndSave(args[1]);
            this.logDirect(args[1] + " removed from friends.", Formatting.GREEN);
            break;
         case "clear":
            int count = FriendUtils.size();
            FriendUtils.clearAndSave();
            this.logDirect("Friend list cleared. Removed: " + count, Formatting.GREEN);
            break;
         case "list":
            List<String> friends = FriendUtils.getFriendNames();
            if (friends.isEmpty()) {
               this.logDirect("Friend list is empty.", Formatting.RED);
               return;
            }

            int page = args.length > 1 ? this.parsePage(args[1]) : 1;
            Paginator<String> paginator = new Paginator<>(friends);
            paginator.setPage(page);
            paginator.display(
               () -> {
                  this.logDirectRaw(Text.literal(HelpCommand.getLine()));
                  this.logDirect("Friends (" + friends.size() + ")");
               },
               friend -> Text.literal("  §a● §f" + friend)
                     .styled(
                        style -> style.withHoverEvent(new ShowText(Text.literal("Click to remove " + friend)))
                              .withClickEvent(new RunCommand(manager.getPrefix() + "friend remove " + friend))
                     ),
               manager.getPrefix() + label + " list"
            );
            break;
         default:
            this.logDirect("Usage: friend add/remove/list/clear");
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      if (args.length == 1) {
         return new TabCompleteHelper().append("add", "remove", "list", "clear").sortAlphabetically().filterPrefix(args[0]).stream();
      } else {
         if (args.length == 2) {
            String action = args[0].toLowerCase(Locale.ROOT);
            if (action.equals("add")) {
               return new TabCompleteHelper().append(this.getOnlinePlayers().toArray(new String[0])).filterPrefix(args[1]).stream();
            }

            if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
               return new TabCompleteHelper().append(FriendUtils.getFriendNames().toArray(new String[0])).filterPrefix(args[1]).stream();
            }
         }

         return Stream.empty();
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Manages friend list.", "Usage:", "> friend add <name>", "> friend remove <name>", "> friend list", "> friend clear");
   }

   private int parsePage(String value) {
      try {
         return Integer.parseInt(value);
      } catch (NumberFormatException var3) {
         return 1;
      }
   }

   private List<String> getOnlinePlayers() {
      List<String> players = new ArrayList<>();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getNetworkHandler() != null) {
         for (PlayerListEntry info : mc.getNetworkHandler().getPlayerList()) {
            String name = info.getProfile().name();
            if (!FriendUtils.isFriend(name)) {
               players.add(name);
            }
         }
      }

      return players;
   }
}
