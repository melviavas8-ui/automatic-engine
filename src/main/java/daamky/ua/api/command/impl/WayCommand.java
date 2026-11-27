package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import daamky.ua.IMinecraft;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.helpers.TabCompleteHelper;
import daamky.ua.utils.repository.way.Way;
import daamky.ua.utils.repository.way.WayRepository;

public final class WayCommand extends Command implements IMinecraft {
   public WayCommand() {
      super("way", "Управление точками", "waypoint", "wp");
   }

   @Override
   public void execute(String label, String[] args) {
      if (mc.player == null) {
         this.logDirect("Игрок недоступен.", ChatFormatting.RED);
      } else {
         WayRepository repository = WayRepository.getInstance();
         String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "list";
         switch (action) {
            case "add":
               this.addWaypoint(repository, args);
               break;
            case "remove":
            case "del":
            case "delete":
               this.removeWaypoint(repository, args);
               break;
            case "clear":
               this.clearCurrentServer(repository);
               break;
            case "clearall":
               int count = repository.size();
               repository.clearListAndSave();
               this.logDirect("Удалены все точки: " + count, ChatFormatting.GREEN);
               break;
            case "list":
               this.listWaypoints(repository);
               break;
            default:
               this.logDirect("Использование: .way add/remove/list/clear/clearall");
         }
      }
   }

   @Override
   public Stream<String> tabComplete(String label, String[] args) {
      WayRepository repository = WayRepository.getInstance();
      if (args.length == 1) {
         return new TabCompleteHelper().append("add", "remove", "list", "clear", "clearall").sortAlphabetically().filterPrefix(args[0]).stream();
      } else {
         String action = args.length > 0 ? args[0].toLowerCase(Locale.ROOT) : "";
         if (args.length == 2 && action.equals("add")) {
            return new TabCompleteHelper().append("Название").filterPrefix(args[1]).stream();
         } else if (args.length == 2 && this.isRemoveAction(action)) {
            return new TabCompleteHelper()
               .append(repository.getWayNamesForServer(repository.getCurrentServer()).toArray(new String[0]))
               .filterPrefix(args[1])
               .stream();
         } else {
            return action.equals("add") && args.length >= 3 && args.length <= 5
               ? this.coordinateSuggestions(args.length, args[args.length - 1])
               : Stream.empty();
         }
      }
   }

   @Override
   public List<String> getLongDesc() {
      return Arrays.asList(
         "Управляет серверными точками.", "Использование:", "> way add <название> [x y z]", "> way remove <название>", "> way list", "> way clear"
      );
   }

   private void addWaypoint(WayRepository repository, String[] args) {
      if (args.length < 2) {
         this.logDirect("Использование: .way add <название> [x y z]", ChatFormatting.RED);
      } else if (args.length != 2 && args.length != 5) {
         this.logDirect("Координаты указываются полностью: x y z.", ChatFormatting.RED);
      } else {
         String server = repository.getCurrentServer();
         if (server.isBlank()) {
            this.logDirect("Точку можно поставить только на сервере.", ChatFormatting.RED);
         } else {
            String name = args[1];
            if (repository.hasWay(name, server)) {
               this.logDirect("Точка с таким названием уже есть на этом сервере.", ChatFormatting.RED);
            } else {
               BlockPos pos = mc.player.blockPosition();
               if (args.length == 5) {
                  try {
                     pos = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                  } catch (NumberFormatException var7) {
                     this.logDirect("Неверные координаты.", ChatFormatting.RED);
                     return;
                  }
               }

               repository.addWayAndSave(name, pos, server);
               this.logDirect("Точка " + name + " добавлена: " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), ChatFormatting.GREEN);
            }
         }
      }
   }

   private void removeWaypoint(WayRepository repository, String[] args) {
      if (args.length < 2) {
         this.logDirect("Использование: .way remove <название>", ChatFormatting.RED);
      } else {
         String server = repository.getCurrentServer();
         if (server.isBlank()) {
            this.logDirect("Точки доступны только на сервере.", ChatFormatting.RED);
         } else {
            String name = args[1];
            if (!repository.hasWay(name, server)) {
               this.logDirect("Такой точки на этом сервере нет.", ChatFormatting.RED);
            } else {
               repository.deleteWayAndSave(name, server);
               this.logDirect("Точка " + name + " удалена.", ChatFormatting.GREEN);
            }
         }
      }
   }

   private void clearCurrentServer(WayRepository repository) {
      String server = repository.getCurrentServer();
      if (server.isBlank()) {
         this.logDirect("Точки доступны только на сервере.", ChatFormatting.RED);
      } else {
         int removed = repository.clearServerAndSave(server);
         this.logDirect("Удалено точек на этом сервере: " + removed, ChatFormatting.GREEN);
      }
   }

   private void listWaypoints(WayRepository repository) {
      String server = repository.getCurrentServer();
      if (server.isBlank()) {
         this.logDirect("Точки доступны только на сервере.", ChatFormatting.RED);
      } else {
         List<Way> ways = repository.getWaysForServer(server);
         if (ways.isEmpty()) {
            this.logDirect("На этом сервере точек нет.", ChatFormatting.RED);
         } else {
            for (Way way : ways) {
               BlockPos pos = way.pos();
               this.logDirect("§f" + way.name() + " §8[" + pos.getX() + " " + pos.getY() + " " + pos.getZ() + "]");
            }
         }
      }
   }

   private Stream<String> coordinateSuggestions(int argCount, String prefix) {
      TabCompleteHelper helper = new TabCompleteHelper();
      BlockPos pos = mc.player == null ? BlockPos.ZERO : mc.player.blockPosition();
      if (argCount == 3) {
         helper.append(String.valueOf(pos.getX()), "x");
      } else if (argCount == 4) {
         helper.append(String.valueOf(pos.getY()), "y");
      } else if (argCount == 5) {
         helper.append(String.valueOf(pos.getZ()), "z");
      }

      return helper.filterPrefix(prefix).stream();
   }

   private boolean isRemoveAction(String action) {
      return action.equals("remove") || action.equals("del") || action.equals("delete");
   }
}
