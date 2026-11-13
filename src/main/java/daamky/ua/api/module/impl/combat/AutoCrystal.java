package daamky.ua.api.module.impl.combat;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public class AutoCrystal extends Module {
   private final BooleanSetting autoAddCrystal = this.register(new BooleanSetting("Auto Place", "Automatically places end crystals.", true));
   private final NumberSetting addCooldown = this.register(new NumberSetting("Place Delay", "Delay between crystal placements in ticks.", 0.0, 0.0, 5.0, 1.0));
   private final NumberSetting attackCooldown = this.register(new NumberSetting("Attack Delay", "Delay between crystal attacks in ticks.", 0.0, 0.0, 5.0, 1.0));
   private final NumberSetting attackRange = this.register(new NumberSetting("Attack Range", "Range for 360 crystal attacks.", 6.0, 1.0, 6.0, 0.1));
   private int attackCrystalCooldown;
   private int addCrystalCooldown;
   private int previousCrystalSlot = -1;
   private int restoreSlot = -1;
   private int restoreDelay;
   private int pendingInventorySlot = -1;
   private int pendingHotbarSlot = -1;
   private int restoreInventorySlot = -1;
   private int restoreHotbarSlot = -1;
   private int restoreInventoryDelay;

   public AutoCrystal() {
      super("Auto Crystal", "Places and attacks end crystals.", ModuleCategory.COMBAT);
   }

   @SubscribeEvent
   private void onPreTick(TickEvent.Pre event) {
      MinecraftClient client = event.getClient();
      if (client.player != null && client.world != null && client.interactionManager != null) {
         boolean restoring = this.tickRestoreInventory() | this.tickRestoreSlot(client);
         if (!restoring && this.autoAddCrystal.getValue()) {
            this.addCrystal(client);
         }

         this.attackCrystal(client);
      } else {
         this.resetCooldowns();
      }
   }

   @Override
   protected void onEnable() {
      this.resetCooldowns();
   }

   private void addCrystal(MinecraftClient client) {
      if (client.crosshairTarget instanceof BlockHitResult obsidianTarget) {
         BlockPos var8 = obsidianTarget.getBlockPos();
         BlockState state = client.world.getBlockState(var8);
         if (state.getBlock() != Blocks.OBSIDIAN && state.getBlock() != Blocks.BEDROCK) {
            this.cancelPendingInventorySwap();
         } else if ((double)this.addCrystalCooldown < this.addCooldown.getValue()) {
            this.addCrystalCooldown++;
         } else if (this.hasCrystalOn(client, var8)) {
            this.cancelPendingInventorySwap();
         } else {
            int crystalSlot = InventoryUtils.findHotbarItem(Items.END_CRYSTAL);
            if (crystalSlot == -1) {
               int inventorySlot = InventoryUtils.findItemInInventory(Items.END_CRYSTAL);
               if (inventorySlot == -1) {
                  this.cancelPendingInventorySwap();
               } else {
                  this.swapCrystalFromInventory(client, inventorySlot);
               }
            } else {
               int selectedSlot = client.player.getInventory().getSelectedSlot();
               if (selectedSlot != crystalSlot) {
                  this.previousCrystalSlot = selectedSlot;
                  InventoryUtils.syncSelectedHotbarSlot(crystalSlot);
               } else {
                  ActionResult result = client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, obsidianTarget);
                  if (result.isAccepted()) {
                     client.player.swingHand(Hand.MAIN_HAND);
                     this.scheduleRestoreSlot();
                     this.scheduleRestoreInventory();
                  }

                  this.addCrystalCooldown = 0;
               }
            }
         }
      } else {
         this.cancelPendingInventorySwap();
      }
   }

   private void attackCrystal(MinecraftClient client) {
      if ((double)this.attackCrystalCooldown < this.attackCooldown.getValue()) {
         this.attackCrystalCooldown++;
      } else {
         EndCrystalEntity crystal = this.findCrystalInHitbox(client);
         if (crystal != null) {
            client.interactionManager.attackEntity(client.player, crystal);
            client.player.swingHand(Hand.MAIN_HAND);
            this.attackCrystalCooldown = 0;
         }
      }
   }

   private EndCrystalEntity findCrystalInHitbox(MinecraftClient client) {
      double range = this.attackRange.getValue();
      double rangeSq = range * range;
      if (client.crosshairTarget instanceof EntityHitResult hit) {
         if (!(hit.getEntity() instanceof EndCrystalEntity crystal) || !crystal.isAlive()) {
            return null;
         }

         return client.player.squaredDistanceTo(crystal) > rangeSq ? null : crystal;
      } else {
         return null;
      }
   }

   private boolean hasCrystalOn(MinecraftClient client, BlockPos basePos) {
      BlockPos crystalPos = basePos.up();
      Box searchBox = new Box(
            (double)crystalPos.getX(),
            (double)crystalPos.getY(),
            (double)crystalPos.getZ(),
            (double)crystalPos.getX() + 1.0,
            (double)crystalPos.getY() + 2.0,
            (double)crystalPos.getZ() + 1.0
         )
         .expand(0.1);

      for (Entity entity : client.world.getOtherEntities(client.player, searchBox, entityx -> entityx instanceof EndCrystalEntity)) {
         if (entity.isAlive()) {
            return true;
         }
      }

      return false;
   }

   private void resetCooldowns() {
      this.attackCrystalCooldown = 0;
      this.addCrystalCooldown = 0;
      this.previousCrystalSlot = -1;
      this.restoreSlot = -1;
      this.restoreDelay = 0;
      this.pendingInventorySlot = -1;
      this.pendingHotbarSlot = -1;
      this.restoreInventorySlot = -1;
      this.restoreHotbarSlot = -1;
      this.restoreInventoryDelay = 0;
   }

   private void scheduleRestoreSlot() {
      if (this.previousCrystalSlot != -1) {
         this.restoreSlot = this.previousCrystalSlot;
         this.restoreDelay = 2;
         this.previousCrystalSlot = -1;
      }
   }

   private boolean tickRestoreSlot(MinecraftClient client) {
      if (this.restoreSlot == -1) {
         return false;
      } else if (this.restoreDelay > 0) {
         this.restoreDelay--;
         return true;
      } else {
         int slot = this.restoreSlot;
         this.restoreSlot = -1;
         if (client.player.getInventory().getSelectedSlot() != slot) {
            InventoryUtils.syncSelectedHotbarSlot(slot);
         }

         return true;
      }
   }

   private void swapCrystalFromInventory(MinecraftClient client, int inventorySlot) {
      if (this.pendingInventorySlot == -1 && this.restoreInventorySlot == -1) {
         int selectedSlot = client.player.getInventory().getSelectedSlot();
         int wrappedSlot = InventoryUtils.wrapSlot(inventorySlot);
         this.pendingInventorySlot = wrappedSlot;
         this.pendingHotbarSlot = selectedSlot;
         InventoryUtils.click(wrappedSlot, selectedSlot, SlotActionType.SWAP);
      }
   }

   private void scheduleRestoreInventory() {
      if (this.pendingInventorySlot != -1 && this.pendingHotbarSlot != -1) {
         this.restoreInventorySlot = this.pendingInventorySlot;
         this.restoreHotbarSlot = this.pendingHotbarSlot;
         this.restoreInventoryDelay = 2;
         this.pendingInventorySlot = -1;
         this.pendingHotbarSlot = -1;
      }
   }

   private void cancelPendingInventorySwap() {
      if (this.pendingInventorySlot != -1 && this.pendingHotbarSlot != -1 && this.restoreInventorySlot == -1) {
         this.restoreInventorySlot = this.pendingInventorySlot;
         this.restoreHotbarSlot = this.pendingHotbarSlot;
         this.restoreInventoryDelay = 1;
         this.pendingInventorySlot = -1;
         this.pendingHotbarSlot = -1;
      }
   }

   private boolean tickRestoreInventory() {
      if (this.restoreInventorySlot == -1 || this.restoreHotbarSlot == -1) {
         return false;
      } else if (this.restoreInventoryDelay > 0) {
         this.restoreInventoryDelay--;
         return true;
      } else {
         int inventorySlot = this.restoreInventorySlot;
         int hotbarSlot = this.restoreHotbarSlot;
         this.restoreInventorySlot = -1;
         this.restoreHotbarSlot = -1;
         InventoryUtils.click(inventorySlot, hotbarSlot, SlotActionType.SWAP);
         return true;
      }
   }
}
