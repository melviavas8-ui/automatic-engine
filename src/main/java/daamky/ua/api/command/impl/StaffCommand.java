package daamky.ua.api.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.helpers.TabCompleteHelper;
import daamky.ua.utils.repository.staff.StaffUtils;

public final class StaffCommand extends Command {
   public StaffCommand() {
      super("staff", "Manage staff list");
   }

   @Override
   public void execute(String label, String[] args) {
      String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
      switch (action) {
         case "add":
            if (args.length < 2) {
               this.logDirect("Usage: staff add <name>", ChatFormatting.RED);
               return;
            }

            StaffUtils.addStaffAndSave(args[1]);
            this.logDirect(args[1] + " added to staff.", ChatFormatting.GREEN);
            break;
         case "remove":
         case "del":
         case "delete":
            if (args.length < 2) {
               this.logDirect("Usage: staff remove <name>", ChatFormatting.RED);
               return;
            }

            StaffUtils.removeStaffAndSave(args[1]);
            this.logDirect(args[1] + " removed from staff.", ChatFormatting.GREEN);
            break;
         case "clear":
            int count = StaffUtils.size();
            StaffUtils.clearAndSave();
            this.logDirect("Staff list cleared. Removed: " + count, ChatFormatting.GREEN);
            break;
         case "list":
            List<String> staff = StaffUtils.getStaffNames();
            if (staff.isEmpty()) {
               this.logDirect("Staff list is empty.", ChatFormatting.RED);
               return;
            }

            this.logDirect("Staff (" + staff.size() + "): " + String.join(", ", staff));
            break;
         default:
            this.logDirect("Usage: staff add/remove/list/clear");
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

            if (action.equals("remove")) {
               return new TabCompleteHelper().append(StaffUtils.getStaffNames().toArray(new String[0])).filterPrefix(args[1]).stream();
            }
         }

         return Stream.empty();
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList("Manages staff list.", "Usage:", "> staff add <name>", "> staff remove <name>", "> staff list", "> staff clear");
   }

   private List<String> getOnlinePlayers() {
      List<String> players = new ArrayList<>();
      Minecraft mc = Minecraft.getInstance();
      if (mc.getConnection() != null) {
         for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            players.add(info.getProfile().name());
         }
      }

      return players;
   }
}
