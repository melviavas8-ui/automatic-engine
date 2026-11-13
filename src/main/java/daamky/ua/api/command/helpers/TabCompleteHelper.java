package daamky.ua.api.command.helpers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import daamky.ua.api.command.Command;
import daamky.ua.api.command.CommandManager;

public final class TabCompleteHelper {
   private final List<String> values = new ArrayList<>();

   public TabCompleteHelper() {
   }

   public TabCompleteHelper append(String... entries) {
      if (entries != null) {
         for (String entry : entries) {
            if (entry != null) {
               this.values.add(entry);
            }
         }
      }

      return this;
   }

   public TabCompleteHelper addCommands(CommandManager manager) {
      if (manager != null) {
         for (Command command : manager.getCommands()) {
            this.values.add(command.getName());
            this.values.addAll(command.getAliases());
         }
      }

      return this;
   }

   public TabCompleteHelper sortAlphabetically() {
      this.values.sort(Comparator.comparing(String::toLowerCase));
      return this;
   }

   public TabCompleteHelper filterPrefix(String prefix) {
      String lowered = prefix == null ? "" : prefix.toLowerCase();
      this.values.removeIf(value -> !value.toLowerCase().startsWith(lowered));
      return this;
   }

   public Stream<String> stream() {
      return this.values.stream().distinct();
   }
}
