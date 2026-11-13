package daamky.ua.api.module.impl.misc;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;

public final class AutoDuel extends Module {
   private static final Pattern NICK_PATTERN = Pattern.compile("^\\w{3,16}$");
   private final ModeSetting mode = this.register(
      new ModeSetting(
         "Mode", "Duel kit mode.", "Spheres", "Spheres", "Shield", "Spikes 3", "Netherite", "Cheater Paradise", "Bow", "Classic", "Totems", "NoDebuff"
      )
   );
   private final NumberSetting slowTime = this.register(new NumberSetting("Send Speed", "Delay between duel requests.", 500.0, 300.0, 1000.0, 10.0));
   private final BooleanSetting moneyMode = this.register(new BooleanSetting("Play For Money", "Adds money amount to duel request.", false));
   private final StringSetting money = this.register(new StringSetting("Money", "Money amount for duel request.", "10000", 16));
   private final List<String> sent = new ArrayList<>();
   private final StopWatch counter = new StopWatch();
   private final StopWatch clearCounter = new StopWatch();
   private final StopWatch choiceCounter = new StopWatch();
   private final StopWatch confirmCounter = new StopWatch();
   private double lastPosX;
   private double lastPosY;
   private double lastPosZ;

   public AutoDuel() {
      super("Auto Duel", "Automates duel requests.", ModuleCategory.MISC);
      this.money.visibleWhen(this.moneyMode::getValue);
   }

   @Override
   protected void onEnable() {
      this.counter.reset();
      this.clearCounter.reset();
      this.choiceCounter.reset();
      this.confirmCounter.reset();
      this.sent.clear();
      if (mc.player != null) {
         this.lastPosX = mc.player.getX();
         this.lastPosY = mc.player.getY();
         this.lastPosZ = mc.player.getZ();
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.player.networkHandler != null && mc.interactionManager != null) {
         this.handleDuelLogic();
         this.handleScreenInteraction();
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && event.getPacket() instanceof GameMessageS2CPacket chat) {
         String text = chat.content().getString().toLowerCase(Locale.ROOT);
         if (text.contains("начало") && text.contains("через") && text.contains("секунд")
            || text.contains("дуэли") && text.contains("запрещено") && text.contains("команд")) {
            this.setEnabled(false);
         }
      }
   }

   private void handleDuelLogic() {
      List<String> players = this.getOnlinePlayers();
      double distance = Math.sqrt(
         Math.pow(this.lastPosX - mc.player.getX(), 2.0) + Math.pow(this.lastPosY - mc.player.getY(), 2.0) + Math.pow(this.lastPosZ - mc.player.getZ(), 2.0)
      );
      if (distance > 500.0) {
         this.setEnabled(false);
      } else {
         this.lastPosX = mc.player.getX();
         this.lastPosY = mc.player.getY();
         this.lastPosZ = mc.player.getZ();
         if (this.clearCounter.finished((double)(800L * (long)Math.max(1, players.size())))) {
            this.sent.clear();
            this.clearCounter.reset();
         }

         String ownName = mc.player.getGameProfile().name();

         for (String player : players) {
            if (!this.sent.contains(player) && !player.equals(ownName) && this.counter.finished(this.slowTime.getValue())) {
               this.sendDuelRequest(player);
               this.sent.add(player);
               this.counter.reset();
            }
         }
      }
   }

   private void sendDuelRequest(String player) {
      if (this.moneyMode.getValue()) {
         mc.player.networkHandler.sendChatCommand("duel " + player + " " + this.money.getValue());
      } else {
         mc.player.networkHandler.sendChatCommand("duel " + player);
      }
   }

   private void handleScreenInteraction() {
      if (mc.currentScreen != null) {
         ScreenHandler title = mc.player.currentScreenHandler;
         if (title instanceof ScreenHandler) {
            String titlex = mc.currentScreen.getTitle().getString().toLowerCase(Locale.ROOT);
            if (titlex.contains("выбор набора") && this.choiceCounter.finished(150.0)) {
               int slotId = this.getKitSlot();
               if (slotId >= 0) {
                  mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, mc.player);
               }

               this.choiceCounter.reset();
               return;
            }

            if (titlex.contains("настройка поединка") && this.confirmCounter.finished(150.0)) {
               mc.interactionManager.clickSlot(title.syncId, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
               this.confirmCounter.reset();
            }

            return;
         }
      }
   }

   private int getKitSlot() {
      String var1 = this.mode.getValue();

      return switch (var1) {
         case "Shield" -> 0;
         case "Spikes 3" -> 1;
         case "Bow" -> 2;
         case "Totems" -> 3;
         case "NoDebuff" -> 4;
         case "Spheres" -> 5;
         case "Classic" -> 6;
         case "Cheater Paradise" -> 7;
         case "Netherite" -> 8;
         default -> -1;
      };
   }

   private List<String> getOnlinePlayers() {
      List<String> players = new ArrayList<>();
      if (mc.player != null && mc.player.networkHandler != null) {
         for (PlayerListEntry info : mc.player.networkHandler.getPlayerList()) {
            GameProfile profile = info.getProfile();
            String name = profile.name();
            if (NICK_PATTERN.matcher(name).matches()) {
               players.add(name);
            }
         }

         return players;
      } else {
         return players;
      }
   }
}
