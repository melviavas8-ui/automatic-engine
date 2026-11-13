package daamky.ua.api.command;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import lightning.sdk.LightningSDK;
import lightning.sdk.annotation.LgVM;
import lightning.sdk.annotation.LgVM.Level;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import daamky.ua.api.command.impl.BindCommand;
import daamky.ua.api.command.impl.ConfigCommand;
import daamky.ua.api.command.impl.FriendCommand;
import daamky.ua.api.command.impl.HelpCommand;
import daamky.ua.api.command.impl.MacroCommand;
import daamky.ua.api.command.impl.PrefixCommand;
import daamky.ua.api.command.impl.StaffCommand;
import daamky.ua.api.command.impl.VClipCommand;
import daamky.ua.api.command.impl.WayCommand;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.ChatEvent;
import daamky.ua.api.events.impl.TabCompleteEvent;
import daamky.ua.utils.repository.RepositoryStorage;
import daamky.ua.utils.string.chat.ChatMessage;

public final class CommandManager {
   private static CommandManager instance;
   private final List<Command> commands = new CopyOnWriteArrayList<>();
   private String prefix = ".";

   public CommandManager() {
      instance = this;
   }

   public static CommandManager getInstance() {
      return instance;
   }

   @LgVM(Level.FULL)
   public void init() {
      if (LightningSDK.checkAuthAlive()) {
         JsonObject prefixConfig = RepositoryStorage.readObject("prefix.daamky");
         this.prefix = prefixConfig.has("prefix") ? prefixConfig.get("prefix").getAsString() : ".";
         this.registerCommand(new HelpCommand());
         this.registerCommand(new ConfigCommand());
         this.registerCommand(new FriendCommand());
         this.registerCommand(new MacroCommand());
         this.registerCommand(new BindCommand());
         this.registerCommand(new PrefixCommand());
         this.registerCommand(new WayCommand());
         this.registerCommand(new StaffCommand());
         this.registerCommand(new VClipCommand());
      }
   }

   public void registerCommand(Command command) {
      this.commands.add(command);
   }

   public Command getCommand(String name) {
      return this.commands.stream().filter(command -> command.matches(name)).findFirst().orElse(null);
   }

   public List<Command> getCommands() {
      return new ArrayList<>(this.commands);
   }

   public String getPrefix() {
      return this.prefix;
   }

   public void setPrefix(String prefix) {
      this.prefix = prefix != null && !prefix.isBlank() ? prefix : ".";
      JsonObject object = new JsonObject();
      object.addProperty("prefix", this.prefix);
      RepositoryStorage.write("prefix.daamky", object);
   }

   @SubscribeEvent
   private void onChat(ChatEvent event) {
      String msg = event.getMessage();
      if (msg.startsWith(this.prefix)) {
         event.cancel();
         String commandStr = msg.substring(this.prefix.length());
         if (commandStr.trim().isEmpty()) {
            this.execute("help");
         } else {
            if (!this.execute(commandStr)) {
               this.sendError("Unknown command. Use " + this.prefix + "help.");
            }
         }
      }
   }

   @SubscribeEvent
   private void onTabComplete(TabCompleteEvent event) {
      String eventPrefix = event.getPrefix();
      if (eventPrefix.startsWith(this.prefix)) {
         String msg = eventPrefix.substring(this.prefix.length());
         Stream<String> stream = this.tabComplete(msg);
         String[] parts = msg.split(" ", -1);
         if (parts.length <= 1) {
            stream = stream.map(value -> this.prefix + value);
         }

         event.setCompletions(stream.toArray(String[]::new));
      }
   }

   public boolean execute(String input) {
      if (input != null && !input.trim().isEmpty()) {
         String[] parts = input.trim().split("\\s+", 2);
         String commandName = parts[0];
         String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];
         Command command = this.getCommand(commandName);
         if (command == null) {
            return false;
         } else {
            try {
               command.execute(commandName, args);
               return true;
            } catch (Exception var7) {
               this.sendError("Command error: " + var7.getMessage());
               var7.printStackTrace();
               return true;
            }
         }
      } else {
         return this.execute("help");
      }
   }

   public Stream<String> tabComplete(String input) {
      if (input == null) {
         input = "";
      }

      String[] args = input.split("\\s+", -1);
      if (args.length <= 1) {
         String partial = args.length == 0 ? "" : args[0].toLowerCase();
         return this.getCommandSuggestions(partial);
      } else {
         Command command = this.getCommand(args[0]);
         return command != null ? command.tabComplete(args[0], Arrays.copyOfRange(args, 1, args.length)) : Stream.empty();
      }
   }

   private Stream<String> getCommandSuggestions(String partial) {
      Set<String> suggestions = new LinkedHashSet<>();

      for (Command command : this.commands) {
         if (command.getName().toLowerCase().startsWith(partial)) {
            suggestions.add(command.getName());
         }

         for (String alias : command.getAliases()) {
            if (alias.toLowerCase().startsWith(partial)) {
               suggestions.add(alias);
            }
         }
      }

      return suggestions.stream().sorted();
   }

   public void sendMessage(String message) {
      ChatMessage.brandmessage(message);
   }

   public void sendSuccess(String message) {
      this.sendRaw(
         ChatMessage.brandmessage()
            .copy()
            .append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY))
            .append(Text.literal(message).formatted(Formatting.GREEN))
      );
   }

   public void sendError(String message) {
      this.sendRaw(
         ChatMessage.brandmessage().copy().append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY)).append(Text.literal(message).formatted(Formatting.RED))
      );
   }

   public void sendRaw(Text text) {
      if (MinecraftClient.getInstance().player != null) {
         MinecraftClient.getInstance().player.sendMessage(text, false);
      }
   }
}
