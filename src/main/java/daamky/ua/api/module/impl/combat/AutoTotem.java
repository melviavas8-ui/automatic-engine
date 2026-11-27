package daamky.ua.api.module.impl.combat;

import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.mojang.blaze3d.platform.InputConstants;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.repository.friend.FriendUtils;

public class AutoTotem extends Module {
   private static final long EQUIP_ATTEMPT_COOLDOWN_MS = 250L;
   private static final long SLOWING_DOWN_DELAY_MS = 1L;
   private static final long SWAP_DELAY_MS = 25L;
   private static final long AWAIT_SWITCH_TIMEOUT_MS = 50L;
   private static final long RESTORE_SLOT_DELAY_MS = 25L;
   private static final long SPEEDING_UP_DELAY_MS = 25L;
   private final NumberSetting healthThreshold = this.register(new NumberSetting("Health", "Health threshold.", 4.5, 1.0, 20.0, 0.5));
   private final NumberSetting elytraHealth = this.register(new NumberSetting("Elytra Health", "Elytra health threshold.", 8.5, 1.0, 20.0, 0.5));
   private final MultiModeSetting triggers = this.register(
      new MultiModeSetting("Triggers", "Totem equip triggers.", new String[]{"Crystal", "Fall", "Elytra", "Mace"}, "Crystal", "Fall", "Elytra", "Mace")
   );
   private final BooleanSetting returnItem = this.register(new BooleanSetting("Return Item", "Return offhand item after danger.", true));
   private final NumberSetting crystalDistance = this.register(new NumberSetting("Crystal Distance", "Danger crystal distance.", 4.0, 1.0, 6.0, 0.5));
   private final NumberSetting crystalHeight = this.register(new NumberSetting("Crystal Height", "Danger crystal vertical distance.", 2.5, 0.5, 8.0, 0.5));
   private final NumberSetting fallHeight = this.register(new NumberSetting("Fall Height", "Danger fall height.", 15.0, 5.0, 50.0, 1.0));
   private final NumberSetting maceDistance = this.register(
      new NumberSetting("Mace Distance", "Horizontal radius around you to watch for mace players.", 6.0, 2.0, 12.0, 0.5)
   );
   private final BooleanSetting maceOnlyAir = this.register(
      new BooleanSetting("Mace Only Air", "React only when the mace player is airborne (diving). Off = any nearby mace.", true)
   );
   private final BooleanSetting maceIgnoreFriends = this.register(
      new BooleanSetting("Mace Ignore Friends", "Do not treat friends holding a mace as a threat.", true)
   );
   private int savedSlot = -1;
   private int totemSlot = -1;
   private int previousOffhandSourceSlot = -1;
   private float fallStartY;
   private boolean wasFalling;
   private boolean keysOverridden;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private boolean wasSprintPressed;
   private ItemStack previousOffhandStack = ItemStack.EMPTY;
   private boolean needsReturn;
   private long actionStartTime;
   private long lastEquipAttemptMs;
   private AutoTotem.Phase phase = AutoTotem.Phase.READY;

   public AutoTotem() {
      super("Auto Totem", "Automatically equips totems.", ModuleCategory.COMBAT);
      this.crystalDistance.visibleWhen(() -> this.triggers.isSelected("Crystal"));
      this.crystalHeight.visibleWhen(() -> this.triggers.isSelected("Crystal"));
      this.fallHeight.visibleWhen(() -> this.triggers.isSelected("Fall"));
      this.elytraHealth.visibleWhen(() -> this.triggers.isSelected("Elytra"));
      this.maceDistance.visibleWhen(() -> this.triggers.isSelected("Mace"));
      this.maceOnlyAir.visibleWhen(() -> this.triggers.isSelected("Mace"));
      this.maceIgnoreFriends.visibleWhen(() -> this.triggers.isSelected("Mace"));
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.gameMode != null && client.level != null) {
         this.updateFallTracking(client);
         if (this.phase != AutoTotem.Phase.READY) {
            this.execute(client);
         } else {
            boolean shouldEquip = this.shouldEquipTotem(client);
            if (shouldEquip) {
               long now = System.currentTimeMillis();
               if (now - this.lastEquipAttemptMs >= 250L) {
                  this.tryEquipTotem(client);
               }
            } else {
               if (this.needsReturn && this.returnItem.getValue() && !this.previousOffhandStack.isEmpty()) {
                  if (this.isTotemInOffhand(client)) {
                     this.startLegitReturn(client);
                  } else {
                     this.clearReturnState();
                  }
               }
            }
         }
      } else {
         this.resetState();
         this.clearReturnState();
      }
   }

   @Override
   protected void onDisable() {
      this.resetState();
      this.clearReturnState();
   }

   private void tryEquipTotem(Minecraft client) {
      if (this.phase == AutoTotem.Phase.READY && client.screen == null && !this.isPreferredTotemAlreadyInOffhand(client)) {
         this.lastEquipAttemptMs = System.currentTimeMillis();
         Slot slot = this.findPreferredTotemSlot(client);
         if (slot != null) {
            int slotId = this.getMenuSlotId(client, slot);
            if (slotId != -1) {
               this.savedSlot = client.player.getInventory().getSelectedSlot();
               this.totemSlot = slotId;
               this.previousOffhandStack = client.player.getOffhandItem().copy();
               this.previousOffhandSourceSlot = slotId;
               this.needsReturn = !this.previousOffhandStack.isEmpty();
               this.startLegitEquip(client);
            }
         }
      }
   }

   private void startLegitEquip(Minecraft client) {
      this.captureKeyStates(client);
      this.phase = AutoTotem.Phase.SLOWING_DOWN;
      this.actionStartTime = System.currentTimeMillis();
      this.keysOverridden = false;
   }

   private void startLegitReturn(Minecraft client) {
      this.captureKeyStates(client);
      this.phase = AutoTotem.Phase.RETURN_SLOWING_DOWN;
      this.actionStartTime = System.currentTimeMillis();
      this.keysOverridden = false;
   }

   private void execute(Minecraft client) {
      if (client.player != null && client.gameMode != null && client.screen == null) {
         long elapsed = System.currentTimeMillis() - this.actionStartTime;
         switch (this.phase) {
            case READY:
            default:
               break;
            case SLOWING_DOWN:
               this.stopPlayerInput(client);
               if (elapsed > 1L) {
                  this.phase = AutoTotem.Phase.SWAP_TOTEM;
                  this.actionStartTime = System.currentTimeMillis();
               }
               break;
            case SWAP_TOTEM:
               this.stopPlayerInput(client);
               if (elapsed <= 25L) {
                  return;
               }

               if (this.totemSlot == -1 || !this.clickOffhandSwap(client, this.totemSlot)) {
                  this.resetState();
                  return;
               }

               this.phase = AutoTotem.Phase.AWAIT_SWITCH;
               this.actionStartTime = System.currentTimeMillis();
               break;
            case AWAIT_SWITCH:
               this.stopPlayerInput(client);
               if (this.isTotemInOffhand(client) || elapsed > 50L) {
                  this.phase = AutoTotem.Phase.RESTORE_SLOT;
                  this.actionStartTime = System.currentTimeMillis();
               }
               break;
            case RESTORE_SLOT:
               this.stopPlayerInput(client);
               if (elapsed <= 25L) {
                  return;
               }

               if (this.savedSlot >= 0 && this.savedSlot <= 8) {
                  InventoryTask.switchTo(this.savedSlot);
               }

               this.phase = AutoTotem.Phase.SPEEDING_UP;
               this.actionStartTime = System.currentTimeMillis();
               break;
            case RETURN_SLOWING_DOWN:
               this.stopPlayerInput(client);
               if (elapsed > 1L) {
                  this.phase = AutoTotem.Phase.RETURN_ITEM;
                  this.actionStartTime = System.currentTimeMillis();
               }
               break;
            case RETURN_ITEM:
               this.stopPlayerInput(client);
               if (elapsed <= 25L) {
                  return;
               }

               if (this.attemptReturnPreviousItem(client)) {
                  this.clearReturnState();
                  this.phase = AutoTotem.Phase.SPEEDING_UP;
                  this.actionStartTime = System.currentTimeMillis();
               } else {
                  this.resetState();
                  this.clearReturnState();
               }
               break;
            case SPEEDING_UP:
               if (elapsed >= 25L) {
                  this.restoreKeyStates(client);
                  this.resetState();
               }
         }
      } else {
         this.resetState();
      }
   }

   private void updateFallTracking(Minecraft client) {
      boolean falling = client.player.getDeltaMovement().y < -0.1 && !client.player.onGround() && !client.player.onClimbable() && !client.player.isInWater();
      if (falling && !this.wasFalling) {
         this.fallStartY = (float)client.player.getY();
      }

      if (client.player.onGround() || client.player.isInWater() || client.player.onClimbable()) {
         this.fallStartY = (float)client.player.getY();
      }

      this.wasFalling = falling;
   }

   private boolean shouldEquipTotem(Minecraft client) {
      if (client.player.getHealth() <= this.healthThreshold.getFloat()) {
         return true;
      } else if (this.triggers.isSelected("Elytra") && client.player.isFallFlying() && client.player.getHealth() <= this.elytraHealth.getFloat()) {
         return true;
      } else if (this.triggers.isSelected("Fall")
         && this.fallStartY - (float)client.player.getY() >= this.fallHeight.getFloat()
         && client.player.getDeltaMovement().y < -0.1) {
         return true;
      } else {
         return this.triggers.isSelected("Mace") && this.checkMaceDanger(client)
            ? true
            : this.triggers.isSelected("Crystal") && this.checkCrystalDanger(client);
      }
   }

   private boolean checkMaceDanger(Minecraft client) {
      double maxHorizontal = this.maceDistance.getValue();

      for (Player other : client.level.players()) {
         if (other != client.player
            && other.isAlive()
            && !other.isSpectator()
            && (!this.maceIgnoreFriends.getValue() || !FriendUtils.isFriend(other))
            && !AntiBot.shouldIgnore(other)
            && this.isHoldingMace(other)) {
            double dy = other.getY() - client.player.getY();
            if (!(dy < -2.0)) {
               double horizontal = Math.hypot(other.getX() - client.player.getX(), other.getZ() - client.player.getZ());
               if (!(horizontal > maxHorizontal) && (!this.maceOnlyAir.getValue() || !other.onGround())) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean isHoldingMace(Player player) {
      return player.getMainHandItem().getItem() == Items.MACE || player.getOffhandItem().getItem() == Items.MACE;
   }

   private boolean checkCrystalDanger(Minecraft client) {
      double maxHorizontal = this.crystalDistance.getValue();
      double maxVertical = this.crystalHeight.getValue();

      for (Entity entity : client.level.entitiesForRendering()) {
         if (entity instanceof EndCrystal) {
            double vertical = Math.abs(entity.getY() - client.player.getY());
            if (!(vertical > maxVertical)) {
               double horizontal = Math.hypot(entity.getX() - client.player.getX(), entity.getZ() - client.player.getZ());
               if (horizontal <= maxHorizontal) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private Slot findPreferredTotemSlot(Minecraft client) {
      Slot plain = this.findSlot(client, this::isPlainTotem);
      if (plain != null) {
         return plain;
      } else {
         Slot nonSphere = this.findSlot(client, this::isTotemButNotSphere);
         return nonSphere != null ? nonSphere : this.findSlot(client, stack -> stack.getItem() == Items.TOTEM_OF_UNDYING);
      }
   }

   private Slot findSlot(Minecraft client, Predicate<ItemStack> predicate) {
      for (int i = 0; i < client.player.inventoryMenu.slots.size(); i++) {
         Slot slot = client.player.inventoryMenu.getSlot(i);
         if (this.isValidInventorySlot(client, i, slot)) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && predicate.test(stack)) {
               return slot;
            }
         }
      }

      return null;
   }

   private boolean isValidInventorySlot(Minecraft client, int slotId, Slot slot) {
      return slot != null && slot.container == client.player.getInventory() && slotId != 45 && slotId != 46;
   }

   private boolean isPlainTotem(ItemStack stack) {
      return this.isTotemButNotSphere(stack) && !stack.isEnchanted();
   }

   private boolean isTotemButNotSphere(ItemStack stack) {
      return stack.getItem() == Items.TOTEM_OF_UNDYING && !this.isSphereTotem(stack);
   }

   private boolean isSphereTotem(ItemStack stack) {
      if (stack.getItem() != Items.TOTEM_OF_UNDYING) {
         return false;
      } else {
         String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
         return name.contains("sphere");
      }
   }

   private boolean isTotemInOffhand(Minecraft client) {
      return client.player.getOffhandItem().getItem() == Items.TOTEM_OF_UNDYING;
   }

   private boolean isPreferredTotemAlreadyInOffhand(Minecraft client) {
      if (!this.isTotemInOffhand(client)) {
         return false;
      } else {
         ItemStack offhand = client.player.getOffhandItem();
         return this.isPlainTotem(offhand) || this.findSlot(client, this::isPlainTotem) == null;
      }
   }

   private boolean attemptReturnPreviousItem(Minecraft client) {
      if (this.returnItem.getValue() && !this.previousOffhandStack.isEmpty() && this.isTotemInOffhand(client)) {
         if (this.isValidMenuSlot(client, this.previousOffhandSourceSlot)) {
            Slot restoreSlot = client.player.inventoryMenu.getSlot(this.previousOffhandSourceSlot);
            if (this.stackMatchesForReturn(restoreSlot.getItem())) {
               return this.clickOffhandSwap(client, this.previousOffhandSourceSlot);
            }
         }

         Slot exactSlot = this.findSlot(client, this::stackMatchesForReturn);
         if (exactSlot != null) {
            return this.clickOffhandSwap(client, this.getMenuSlotId(client, exactSlot));
         } else {
            Slot sameItemSlot = this.findSlot(client, stack -> !stack.isEmpty() && stack.getItem() == this.previousOffhandStack.getItem());
            return sameItemSlot != null && this.clickOffhandSwap(client, this.getMenuSlotId(client, sameItemSlot));
         }
      } else {
         return false;
      }
   }

   private boolean stackMatchesForReturn(ItemStack stack) {
      return stack != null && !stack.isEmpty() && !this.previousOffhandStack.isEmpty()
         ? stack.getItem() == this.previousOffhandStack.getItem()
            && stack.isEnchanted() == this.previousOffhandStack.isEnchanted()
            && stack.getHoverName().getString().equals(this.previousOffhandStack.getHoverName().getString())
         : false;
   }

   private boolean clickOffhandSwap(Minecraft client, int slotId) {
      if (!this.isValidMenuSlot(client, slotId)) {
         return false;
      } else {
         client.gameMode.handleInventoryMouseClick(client.player.inventoryMenu.containerId, slotId, 40, ClickType.SWAP, client.player);
         return true;
      }
   }

   private boolean isValidMenuSlot(Minecraft client, int slotId) {
      return slotId >= 0 && slotId < client.player.inventoryMenu.slots.size();
   }

   private int getMenuSlotId(Minecraft client, Slot slot) {
      if (slot == null) {
         return -1;
      } else {
         for (int i = 0; i < client.player.inventoryMenu.slots.size(); i++) {
            if (client.player.inventoryMenu.getSlot(i) == slot) {
               return i;
            }
         }

         return -1;
      }
   }

   private void captureKeyStates(Minecraft client) {
      this.wasForwardPressed = this.isCurrentlyPressed(client, client.options.keyUp);
      this.wasBackPressed = this.isCurrentlyPressed(client, client.options.keyDown);
      this.wasLeftPressed = this.isCurrentlyPressed(client, client.options.keyLeft);
      this.wasRightPressed = this.isCurrentlyPressed(client, client.options.keyRight);
      this.wasJumpPressed = this.isCurrentlyPressed(client, client.options.keyJump);
      this.wasSprintPressed = this.isCurrentlyPressed(client, client.options.keySprint);
   }

   private void stopPlayerInput(Minecraft client) {
      client.player.setSprinting(false);
      if (client.options != null) {
         client.options.keyUp.setDown(false);
         client.options.keyDown.setDown(false);
         client.options.keyLeft.setDown(false);
         client.options.keyRight.setDown(false);
         client.options.keyJump.setDown(false);
         client.options.keySprint.setDown(false);
         this.keysOverridden = true;
      }
   }

   private void restoreKeyStates(Minecraft client) {
      if (this.keysOverridden && client.options != null) {
         client.options.keyUp.setDown(this.wasForwardPressed && this.isCurrentlyPressed(client, client.options.keyUp));
         client.options.keyDown.setDown(this.wasBackPressed && this.isCurrentlyPressed(client, client.options.keyDown));
         client.options.keyLeft.setDown(this.wasLeftPressed && this.isCurrentlyPressed(client, client.options.keyLeft));
         client.options.keyRight.setDown(this.wasRightPressed && this.isCurrentlyPressed(client, client.options.keyRight));
         client.options.keyJump.setDown(this.wasJumpPressed && this.isCurrentlyPressed(client, client.options.keyJump));
         client.options.keySprint.setDown(this.wasSprintPressed && this.isCurrentlyPressed(client, client.options.keySprint));
         this.keysOverridden = false;
      }
   }

   private boolean isCurrentlyPressed(Minecraft client, KeyMapping key) {
      return client.getWindow() != null && key != null
         ? InputConstants.isKeyDown(client.getWindow(), InputConstants.getKey(key.saveString()).getValue())
         : false;
   }

   private void clearReturnState() {
      this.previousOffhandStack = ItemStack.EMPTY;
      this.previousOffhandSourceSlot = -1;
      this.needsReturn = false;
   }

   private void resetState() {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         this.restoreKeyStates(client);
      }

      this.savedSlot = -1;
      this.totemSlot = -1;
      this.actionStartTime = 0L;
      this.phase = AutoTotem.Phase.READY;
      this.keysOverridden = false;
   }

   private static enum Phase {
      READY,
      SLOWING_DOWN,
      SWAP_TOTEM,
      AWAIT_SWITCH,
      RESTORE_SLOT,
      RETURN_SLOWING_DOWN,
      RETURN_ITEM,
      SPEEDING_UP;

      private Phase() {
      }
   }
}
