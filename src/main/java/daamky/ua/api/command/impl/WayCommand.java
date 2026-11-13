package daamky.ua.api.command.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
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
         this.logDirect("Игрок недоступен.", Formatting.RED);
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
               this.logDirect("Удалены все точки: " + count, Formatting.GREEN);
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
         this.logDirect("Использование: .way add <название> [x y z]", Formatting.RED);
      } else if (args.length != 2 && args.length != 5) {
         this.logDirect("Координаты указываются полностью: x y z.", Formatting.RED);
      } else {
         String server = repository.getCurrentServer();
         if (server.isBlank()) {
            this.logDirect("Точку можно поставить только на сервере.", Formatting.RED);
         } else {
            String name = args[1];
            if (repository.hasWay(name, server)) {
               this.logDirect("Точка с таким названием уже есть на этом сервере.", Formatting.RED);
            } else {
               BlockPos pos = mc.player.getBlockPos();
               if (args.length == 5) {
                  try {
                     pos = new BlockPos(Integer.parseInt(args[2]), Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                  } catch (NumberFormatException var7) {
                     this.logDirect("Неверные координаты.", Formatting.RED);
                     return;
                  }
               }

               repository.addWayAndSave(name, pos, server);
               this.logDirect("Точка " + name + " добавлена: " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), Formatting.GREEN);
            }
         }
      }
   }

   private void removeWaypoint(WayRepository repository, String[] args) {
      if (args.length < 2) {
         this.logDirect("Использование: .way remove <название>", Formatting.RED);
      } else {
         String server = repository.getCurrentServer();
         if (server.isBlank()) {
            this.logDirect("Точки доступны только на сервере.", Formatting.RED);
         } else {
            String name = args[1];
            if (!repository.hasWay(name, server)) {
               this.logDirect("Такой точки на этом сервере нет.", Formatting.RED);
            } else {
               repository.deleteWayAndSave(name, server);
               this.logDirect("Точка " + name + " удалена.", Formatting.GREEN);
            }
         }
      }
   }

   private void clearCurrentServer(WayRepository repository) {
      String server = repository.getCurrentServer();
      if (server.isBlank()) {
         this.logDirect("Точки доступны только на сервере.", Formatting.RED);
      } else {
         int removed = repository.clearServerAndSave(server);
         this.logDirect("Удалено точек на этом сервере: " + removed, Formatting.GREEN);
      }
   }

   private void listWaypoints(WayRepository repository) {
      String server = repository.getCurrentServer();
      if (server.isBlank()) {
         this.logDirect("Точки доступны только на сервере.", Formatting.RED);
      } else {
         List<Way> ways = repository.getWaysForServer(server);
         if (ways.isEmpty()) {
            this.logDirect("На этом сервере точек нет.", Formatting.RED);
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
      BlockPos pos = mc.player == null ? BlockPos.ORIGIN : mc.player.getBlockPos();
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
