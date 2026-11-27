package daamky.ua.api.module.impl.misc;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.chat.ChatMessage;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.network.Network;

public final class JoinerHelper extends Module {
   private static final Pattern PAGE_PATTERN = Pattern.compile("(\\d+)\\s*/\\s*(\\d+)");
   private static JoinerHelper instance;
   private final ModeSetting serverSelection = this.register(new ModeSetting("Server", "Target server.", "ReallyWorld", "ReallyWorld", "SpookyTime Duels"));
   private final NumberSetting griefSelection = this.register(new NumberSetting("Grief Number", "ReallyWorld grief number.", 1.0, 1.0, 70.0, 1.0));
   private final NumberSetting clickDelay = this.register(new NumberSetting("Delay", "Delay between actions in seconds.", 0.35, 0.1, 2.0, 0.05));
   private long lastActionTime;
   private long lastScreenChangeTime;
   private long lastJoinAttemptTime;
   private boolean retryQueued;
   private boolean successNotified;
   private String lastScreenSignature = "";

   public JoinerHelper() {
      super("Joiner Helper", "Automatically joins the selected server.", ModuleCategory.MISC);
      instance = this;
      this.griefSelection.visibleWhen(() -> this.serverSelection.is("ReallyWorld"));
   }

   public static JoinerHelper getInstance() {
      return instance;
   }

   @Override
   protected void onEnable() {
      this.resetState(true);
   }

   @Override
   protected void onDisable() {
      this.resetState(false);
   }

   @SubscribeEvent
   private void onTick(TickEvent event) {
      if (mc.player != null && mc.gameMode != null && mc.getConnection() != null) {
         if (GLFW.glfwGetKey(mc.getWindow().handle(), 260) == 1) {
            this.setEnabled(false);
         } else if (this.isCurrentServerSupported()) {
            this.updateScreenSignature();
            if (this.isAlreadyOnTarget()) {
               this.retryQueued = false;
               this.lastJoinAttemptTime = 0L;
               if (!this.successNotified) {
                  this.successNotified = true;
                  if (this.serverSelection.is("ReallyWorld")) {
                     ChatMessage.brandmessage("Joined Grief #" + this.targetGrief());
                  } else {
                     ChatMessage.brandmessage("Joined SpookyTime Duels");
                  }
               }

               if (this.serverSelection.is("SpookyTime Duels")) {
                  this.setEnabled(false);
               }
            } else if (mc.screen != null) {
               if (mc.screen instanceof ContainerScreen container && this.screenSettled()) {
                  if (this.serverSelection.is("ReallyWorld")) {
                     this.handleReallyWorld(container);
                  } else if (this.serverSelection.is("SpookyTime Duels")) {
                     this.handleSpookyTime(container);
                  }

                  return;
               }
            } else if (this.retryQueued) {
               if (this.lastJoinAttemptTime <= 0L || System.currentTimeMillis() - this.lastJoinAttemptTime >= 5000L) {
                  if (this.canAct(this.baseDelayMs() + 120L)) {
                     this.openSelector();
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && event.getPacket() instanceof ClientboundDisconnectPacket packet) {
         String message = clean(packet.reason() == null ? "" : packet.reason().getString());
         if (containsAny(
            message,
            "к сожалению сервер переполнен",
            "сервер переполнен",
            "сервер заполнен",
            "подождите 20 секунд",
            "подождите несколько секунд перед повторным подключением",
            "вы уже подключены на этот сервер",
            "вы были кикнуты",
            "большой поток игроков"
         )) {
            this.retryQueued = true;
            this.successNotified = false;
            this.lastJoinAttemptTime = 0L;
            this.lastActionTime = 0L;
            this.lastScreenChangeTime = System.currentTimeMillis();
         }
      }
   }

   private void handleReallyWorld(ContainerScreen container) {
      String title = clean(container.getTitle().getString());
      AbstractContainerMenu menu = container.getMenu();
      int targetGrief = this.targetGrief();
      if (title.contains("выбор мира грифа")) {
         int currentPage = this.parseCurrentPage(title);
         int targetPage = this.getTargetPage(targetGrief);
         if (currentPage != -1 && currentPage != targetPage) {
            if (currentPage < targetPage) {
               int nextPageSlot = this.findSlotByName(menu, "следующая страница");
               if (nextPageSlot != -1 && this.canAct(this.pageDelayMs())) {
                  this.clickContainerSlot(menu.containerId, nextPageSlot);
               }
            } else {
               int previousPageSlot = this.findSlotByName(menu, "предыдущая страница");
               if (previousPageSlot != -1 && this.canAct(this.pageDelayMs())) {
                  this.clickContainerSlot(menu.containerId, previousPageSlot);
               }
            }
         } else {
            int griefSlot = this.findSlotByName(menu, "гриф #" + targetGrief);
            if (griefSlot != -1 && this.canAct(this.targetDelayMs())) {
               this.clickContainerSlot(menu.containerId, griefSlot);
               this.lastJoinAttemptTime = System.currentTimeMillis();
            }
         }
      } else {
         int survivalSlot = this.findSlotByName(menu, "гриферское выживание");
         if (survivalSlot != -1 && this.canAct(this.menuDelayMs())) {
            this.clickContainerSlot(menu.containerId, survivalSlot);
         }
      }
   }

   private void handleSpookyTime(ContainerScreen container) {
      String title = clean(container.getTitle().getString());
      AbstractContainerMenu menu = container.getMenu();
      if (title.contains("выберите режим")) {
         int duelsModeSlot = this.findSlotByName(menu, "дуэли 1.16.5");
         if (duelsModeSlot == -1) {
            duelsModeSlot = this.findSlotByItem(menu, Items.RESPAWN_ANCHOR);
         }

         if (duelsModeSlot == -1) {
            duelsModeSlot = this.findNonEmptySlot(menu, 14);
         }

         if (duelsModeSlot == -1) {
            duelsModeSlot = this.findSlotByName(menu, "дуэли");
         }

         if (duelsModeSlot != -1 && this.canAct(this.menuDelayMs())) {
            this.clickContainerSlot(menu.containerId, duelsModeSlot);
         }
      } else {
         int duelServerSlot = this.findSlotByName(menu, "1duels");
         if (duelServerSlot == -1) {
            duelServerSlot = this.findSlotByName(menu, "1 дуэл");
         }

         if (duelServerSlot == -1) {
            duelServerSlot = this.findSlotByItem(menu, Items.STICKY_PISTON);
         }

         if (duelServerSlot != -1 && this.canAct(this.targetDelayMs())) {
            this.clickContainerSlot(menu.containerId, duelServerSlot);
            this.lastJoinAttemptTime = System.currentTimeMillis();
         } else {
            int duelsSlot = this.findSlotByName(menu, "дуэли");
            if (duelsSlot != -1 && this.canAct(this.menuDelayMs())) {
               this.clickContainerSlot(menu.containerId, duelsSlot);
            }
         }
      }
   }

   private boolean isAlreadyOnTarget() {
      return this.serverSelection.is("SpookyTime Duels") ? this.isOnSpookyDuels() : this.isOnSelectedReallyWorldGrief();
   }

   private boolean isOnSelectedReallyWorldGrief() {
      if (mc.level == null) {
         return false;
      } else {
         String targetName = "гриф #" + this.targetGrief();
         Scoreboard scoreboard = mc.level.getScoreboard();
         Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
         if (objective == null) {
            return false;
         } else if (clean(objective.getDisplayName().getString()).contains(targetName)) {
            return true;
         } else {
            for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
               String row = clean(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getString());
               if (row.contains(targetName)) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean isOnSpookyDuels() {
      if (mc.level == null) {
         return false;
      } else if (this.hasSpookySidebarWithDuelsMarker()) {
         return true;
      } else {
         return this.lastJoinAttemptTime <= 0L ? false : this.hasSpookySidebarLoaded() || this.hasSpookyJoinHologram();
      }
   }

   private boolean hasSpookySidebarWithDuelsMarker() {
      Scoreboard scoreboard = mc.level.getScoreboard();
      Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (objective == null) {
         return false;
      } else {
         String header = clean(objective.getDisplayName().getString());
         if (!header.contains("spookytime.net")) {
            return false;
         } else {
            for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
               String row = clean(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getString());
               if (row.contains("дуэл") || row.contains("duels") || row.contains("1duels")) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   private boolean hasSpookySidebarLoaded() {
      Scoreboard scoreboard = mc.level.getScoreboard();
      Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
      if (objective == null) {
         return false;
      } else {
         String header = clean(objective.getDisplayName().getString());
         int nonEmptyRows = 0;
         if (!header.isEmpty()) {
            nonEmptyRows++;
            if (header.contains("spooky")) {
               return true;
            }
         }

         for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            String row = clean(PlayerTeam.formatNameForTeam(scoreboard.getPlayersTeam(entry.owner()), entry.ownerName()).getString());
            if (!row.isEmpty()) {
               nonEmptyRows++;
               if (row.contains("spooky") || row.contains("донат") || row.contains("donate") || row.contains("онлайн") || row.contains("online")) {
                  return true;
               }
            }
         }

         return nonEmptyRows >= 3;
      }
   }

   private boolean hasSpookyJoinHologram() {
      if (mc.player != null && mc.level != null) {
         for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.hasCustomName() && !(mc.player.distanceToSqr(entity) > 4096.0)) {
               String name = clean(entity.getDisplayName().getString());
               if (name.contains("донат сервера") || name.contains("донат") && name.contains("сервер") || name.contains("donate server")) {
                  return true;
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private void openSelector() {
      int selectorSlot = this.findSelectorSlot();
      if (selectorSlot != -1) {
         if (mc.player.getInventory().getSelectedSlot() != selectorSlot) {
            mc.getConnection().send(new ServerboundSetCarriedItemPacket(selectorSlot));
            mc.player.getInventory().setSelectedSlot(selectorSlot);
         }

         InventoryUtils.use(InteractionHand.MAIN_HAND);
         this.lastActionTime = System.currentTimeMillis();
         this.lastScreenChangeTime = this.lastActionTime;
         this.successNotified = false;
      }
   }

   private boolean isCurrentServerSupported() {
      if (this.serverSelection.is("ReallyWorld")) {
         return Network.isReallyWorld();
      } else if (!this.serverSelection.is("SpookyTime Duels")) {
         return false;
      } else if (Network.isSpookyTime()) {
         return true;
      } else {
         String address = mc.getConnection().getServerData() != null && mc.getConnection().getServerData().ip != null
            ? clean(mc.getConnection().getServerData().ip)
            : "";
         return address.contains("spooky") || clean(mc.getConnection().serverBrand()).contains("spooky");
      }
   }

   private int findSelectorSlot() {
      int preferredCompassSlot = this.findPreferredCompassSlot(0, 4);
      if (preferredCompassSlot != -1) {
         return preferredCompassSlot;
      } else {
         int compassSlot = InventoryUtils.findItemInHotbar(Items.COMPASS);
         if (compassSlot != -1) {
            return compassSlot;
         } else {
            for (int i = 0; i < 9; i++) {
               ItemStack stack = mc.player.getInventory().getItem(i);
               String name = clean(stack.getHoverName().getString());
               if (name.contains("компас") || name.contains("меню")) {
                  return i;
               }
            }

            return -1;
         }
      }
   }

   private int findPreferredCompassSlot(int... hotbarSlots) {
      for (int hotbarSlot : hotbarSlots) {
         if (hotbarSlot >= 0 && hotbarSlot <= 8) {
            ItemStack stack = mc.player.getInventory().getItem(hotbarSlot);
            if (!stack.isEmpty()) {
               if (stack.getItem() == Items.COMPASS) {
                  return hotbarSlot;
               }

               String name = clean(stack.getHoverName().getString());
               if (name.contains("компас") || name.contains("меню")) {
                  return hotbarSlot;
               }
            }
         }
      }

      return -1;
   }

   private int findSlotByName(AbstractContainerMenu menu, String target) {
      String cleanTarget = clean(target);

      for (Slot slot : menu.slots) {
         if (slot != null && !slot.getItem().isEmpty()) {
            String name = clean(slot.getItem().getHoverName().getString());
            if (name.contains(cleanTarget)) {
               return slot.index;
            }
         }
      }

      return -1;
   }

   private int findSlotByItem(AbstractContainerMenu menu, Item item) {
      for (Slot slot : menu.slots) {
         if (slot != null && !slot.getItem().isEmpty() && slot.getItem().getItem() == item) {
            return slot.index;
         }
      }

      return -1;
   }

   private int findNonEmptySlot(AbstractContainerMenu menu, int slotId) {
      for (Slot slot : menu.slots) {
         if (slot != null && slot.index == slotId && !slot.getItem().isEmpty()) {
            return slot.index;
         }
      }

      return -1;
   }

   private void clickContainerSlot(int syncId, int slotId) {
      mc.gameMode.handleInventoryMouseClick(syncId, slotId, 0, ClickType.PICKUP, mc.player);
      this.lastActionTime = System.currentTimeMillis();
   }

   private int getTargetPage(int griefNumber) {
      return (Math.max(1, griefNumber) - 1) / 32 + 1;
   }

   private int parseCurrentPage(String title) {
      Matcher matcher = PAGE_PATTERN.matcher(title);
      if (!matcher.find()) {
         return -1;
      } else {
         try {
            return Integer.parseInt(matcher.group(1));
         } catch (NumberFormatException var4) {
            return -1;
         }
      }
   }

   private void updateScreenSignature() {
      String currentSignature;
      if (mc.screen instanceof ContainerScreen container) {
         currentSignature = container.getClass().getName() + "|" + clean(container.getTitle().getString());
      } else if (mc.screen != null) {
         currentSignature = mc.screen.getClass().getName();
      } else {
         currentSignature = "";
      }

      if (!currentSignature.equals(this.lastScreenSignature)) {
         this.lastScreenSignature = currentSignature;
         this.lastScreenChangeTime = System.currentTimeMillis();
      }
   }

   private boolean canAct(long delayMs) {
      return System.currentTimeMillis() - this.lastActionTime >= delayMs;
   }

   private boolean screenSettled() {
      return System.currentTimeMillis() - this.lastScreenChangeTime >= 180L;
   }

   private long baseDelayMs() {
      return (long)Math.max(100.0, this.clickDelay.getValue() * 1000.0);
   }

   private long menuDelayMs() {
      return this.baseDelayMs() + 60L;
   }

   private long pageDelayMs() {
      return this.baseDelayMs() + 110L;
   }

   private long targetDelayMs() {
      return this.baseDelayMs() + 90L;
   }

   private void resetState(boolean queueRetry) {
      this.lastActionTime = 0L;
      this.lastScreenChangeTime = 0L;
      this.lastJoinAttemptTime = 0L;
      this.retryQueued = queueRetry;
      this.successNotified = false;
      this.lastScreenSignature = "";
   }

   private int targetGrief() {
      return this.griefSelection.getValue().intValue();
   }

   private static String clean(String value) {
      if (value != null && !value.isEmpty()) {
         StringBuilder out = new StringBuilder(value.length());
         boolean skipFormatting = false;

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (skipFormatting) {
               skipFormatting = false;
            } else if (c == 167) {
               skipFormatting = true;
            } else if (c != 194) {
               out.append(c == 160 ? ' ' : c);
            }
         }

         return out.toString().toLowerCase(Locale.ROOT).trim();
      } else {
         return "";
      }
   }

   private static boolean containsAny(String value, String... patterns) {
      for (String pattern : patterns) {
         if (value.contains(pattern)) {
            return true;
         }
      }

      return false;
   }
}
