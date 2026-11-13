package daamky.ua.api.module.impl.player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.InventoryTask;

public final class ChestStealer extends Module {
   private static final int STOP_BEFORE_LOOT_TICKS = 2;
   private static final String[] LOOT_ITEMS = new String[]{
      "Totem",
      "Netherite Helmet",
      "Netherite Chestplate",
      "Netherite Leggings",
      "Netherite Boots",
      "Netherite Sword",
      "Netherite Pickaxe",
      "Enchanted Golden Apple",
      "Player Head",
      "Shulker Box",
      "Netherite Ingot",
      "Dragon Head",
      "Elytra",
      "Snowball",
      "Splash Potion",
      "Tripwire Hook",
      "Netherite Scrap",
      "Beacon",
      "Villager Spawn Egg"
   };
   private final StopWatch lootWatch = new StopWatch();
   private final Map<Item, String> itemNames = new HashMap<>();
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Chest looting mode.", "Default", "Default", "FT Warden"));
   private final NumberSetting delay = this.register(new NumberSetting("Delay", "Delay between slots.", 100.0, 0.0, 1000.0, 10.0));
   private final BooleanSetting whitelistEnabled = this.register(new BooleanSetting("Whitelist", "Loot only selected items.", false));
   private final MultiModeSetting defaultWhitelist = this.register(new MultiModeSetting("Default Loot", "Selected Default loot.", LOOT_ITEMS));
   private final MultiModeSetting ftWardenWhitelist = this.register(new MultiModeSetting("FT Warden Loot", "Selected FT Warden loot.", LOOT_ITEMS));
   private boolean chestSessionActive;
   private int stopTicksRemaining;

   public ChestStealer() {
      super("Chest Stealer", "Automatically loots chests.", ModuleCategory.PLAYER);
      this.delay.visibleWhen(() -> this.mode.is("Default"));
      this.defaultWhitelist.visibleWhen(() -> this.whitelistEnabled.getValue() && this.mode.is("Default"));
      this.ftWardenWhitelist.visibleWhen(() -> this.whitelistEnabled.getValue() && this.mode.is("FT Warden"));
      this.fillItemNames();
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null) {
         if (client.player.currentScreenHandler instanceof GenericContainerScreenHandler menu) {
            if (this.mode.is("FT Warden")) {
               if (!this.chestSessionActive) {
                  this.chestSessionActive = true;
                  this.stopTicksRemaining = 2;
                  this.lootWatch.reset();
               }

               client.player.setSprinting(false);
               if (this.stopTicksRemaining > 0) {
                  this.stopTicksRemaining--;
               } else {
                  this.stealAllFromChest(menu);
               }
            } else {
               this.chestSessionActive = false;
               this.stopTicksRemaining = 0;
               if (this.delay.getValue() <= 0.0) {
                  this.stealAllFromChest(menu);
               } else if (this.lootWatch.every(this.delay.getValue())) {
                  this.quickMoveNextSlot(menu);
               }
            }
         } else {
            this.resetState();
         }
      } else {
         this.resetState();
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.mode.is("FT Warden")
         && this.chestSessionActive
         && client.player != null
         && client.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
         event.inputNone();
         client.player.setSprinting(false);
      }
   }

   private void quickMoveNextSlot(GenericContainerScreenHandler menu) {
      int chestSlots = menu.getRows() * 9;

      for (int slotIndex = 0; slotIndex < chestSlots; slotIndex++) {
         Slot slot = (Slot)menu.slots.get(slotIndex);
         if (slot.hasStack() && this.shouldLoot(slot.getStack())) {
            InventoryTask.clickSlot(slotIndex, 0, SlotActionType.QUICK_MOVE, true);
            break;
         }
      }

      this.closeIfChestEmpty(menu);
   }

   private void stealAllFromChest(GenericContainerScreenHandler menu) {
      int chestSlots = menu.getRows() * 9;

      for (int slotIndex = 0; slotIndex < chestSlots; slotIndex++) {
         Slot slot = (Slot)menu.slots.get(slotIndex);
         if (slot.hasStack() && this.shouldLoot(slot.getStack())) {
            InventoryTask.clickSlot(slotIndex, 0, SlotActionType.QUICK_MOVE, true);
         }
      }

      this.closeIfChestEmpty(menu);
   }

   private void closeIfChestEmpty(GenericContainerScreenHandler menu) {
      int chestSlots = menu.getRows() * 9;

      for (int slotIndex = 0; slotIndex < chestSlots; slotIndex++) {
         Slot slot = (Slot)menu.slots.get(slotIndex);
         if (slot.hasStack() && this.shouldLoot(slot.getStack())) {
            return;
         }
      }

      InventoryTask.closeScreen(false);
   }

   private boolean shouldLoot(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (!this.whitelistEnabled.getValue()) {
         return true;
      } else {
         Set<String> selected = this.getActiveWhitelist().getValue();
         if (selected != null && !selected.isEmpty()) {
            String itemName = this.resolveWhitelistName(stack);
            return itemName != null && selected.contains(itemName);
         } else {
            return true;
         }
      }
   }

   private MultiModeSetting getActiveWhitelist() {
      return this.mode.is("FT Warden") ? this.ftWardenWhitelist : this.defaultWhitelist;
   }

   private String resolveWhitelistName(ItemStack stack) {
      Item item = stack.getItem();
      String mapped = this.itemNames.get(item);
      if (mapped != null) {
         return mapped;
      } else {
         if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
            return "Shulker Box";
         }

         return null;
      }
   }

   private void fillItemNames() {
      this.itemNames.put(Items.TOTEM_OF_UNDYING, "Totem");
      this.itemNames.put(Items.NETHERITE_HELMET, "Netherite Helmet");
      this.itemNames.put(Items.NETHERITE_CHESTPLATE, "Netherite Chestplate");
      this.itemNames.put(Items.NETHERITE_LEGGINGS, "Netherite Leggings");
      this.itemNames.put(Items.NETHERITE_BOOTS, "Netherite Boots");
      this.itemNames.put(Items.NETHERITE_SWORD, "Netherite Sword");
      this.itemNames.put(Items.NETHERITE_PICKAXE, "Netherite Pickaxe");
      this.itemNames.put(Items.ENCHANTED_GOLDEN_APPLE, "Enchanted Golden Apple");
      this.itemNames.put(Items.PLAYER_HEAD, "Player Head");
      this.itemNames.put(Items.NETHERITE_INGOT, "Netherite Ingot");
      this.itemNames.put(Items.DRAGON_HEAD, "Dragon Head");
      this.itemNames.put(Items.ELYTRA, "Elytra");
      this.itemNames.put(Items.SNOWBALL, "Snowball");
      this.itemNames.put(Items.SPLASH_POTION, "Splash Potion");
      this.itemNames.put(Items.TRIPWIRE_HOOK, "Tripwire Hook");
      this.itemNames.put(Items.NETHERITE_SCRAP, "Netherite Scrap");
      this.itemNames.put(Items.BEACON, "Beacon");
      this.itemNames.put(Items.VILLAGER_SPAWN_EGG, "Villager Spawn Egg");
   }

   private void resetState() {
      this.lootWatch.reset();
      this.chestSessionActive = false;
      this.stopTicksRemaining = 0;
   }
}
