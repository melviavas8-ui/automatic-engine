package daamky.ua.api.module.impl.combat;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoGApple extends Module {
   private final NumberSetting healthThreshold = this.register(new NumberSetting("Health", "Health threshold.", 16.0, 3.0, 20.0, 0.5));
   private final BooleanSetting smartMode = this.register(new BooleanSetting("Smart", "Use hotbar apples, otherwise offhand only.", true));
   private final BooleanSetting goldenHearts = this.register(new BooleanSetting("Golden Hearts", "Count absorption hearts.", true));
   private final BooleanSetting returnSlot = this.register(new BooleanSetting("Return Slot", "Return previous slot after eating.", true));
   private boolean eating;
   private int previousSlot = -1;

   public AutoGApple() {
      super("Auto GApple", "Automatically eats golden apples.", ModuleCategory.COMBAT);
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         this.handleEating(client);
      }
   }

   @Override
   protected void onEnable() {
      this.eating = false;
      this.previousSlot = -1;
   }

   @Override
   protected void onDisable() {
      this.stopEating(Minecraft.getInstance());
   }

   private void handleEating(Minecraft client) {
      if (this.canEat(client)) {
         if (this.smartMode.getValue() && !this.hasGappleInHand(client)) {
            this.swapToGappleSlot(client);
         }

         this.startEating(client);
      } else if (this.eating && !this.shouldContinueEating(client)) {
         this.stopEating(client);
      }
   }

   private boolean canEat(Minecraft client) {
      if (client.player.isDeadOrDying() || client.player.getCooldowns().isOnCooldown(Items.GOLDEN_APPLE.getDefaultInstance())) {
         return false;
      } else if (this.smartMode.getValue() ? this.hasGapple(client) : this.hasGappleInOffhand(client)) {
         float health = client.player.getHealth();
         if (this.goldenHearts.getValue()) {
            health += client.player.getAbsorptionAmount();
         }

         return health <= this.healthThreshold.getFloat();
      } else {
         return false;
      }
   }

   private boolean shouldContinueEating(Minecraft client) {
      return !client.player.isDeadOrDying() && client.player.isUsingItem() && client.player.getUseItem().getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGappleInHand(Minecraft client) {
      return client.player.getMainHandItem().getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGappleInOffhand(Minecraft client) {
      return client.player.getOffhandItem().getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGapple(Minecraft client) {
      return this.hasGappleInOffhand(client) || this.findGappleInHotbar(client) != -1;
   }

   private int findGappleInHotbar(Minecraft client) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = client.player.getInventory().getItem(i);
         if (stack.getItem() == Items.GOLDEN_APPLE) {
            return i;
         }
      }

      return -1;
   }

   private void swapToGappleSlot(Minecraft client) {
      int slot = this.findGappleInHotbar(client);
      int current = client.player.getInventory().getSelectedSlot();
      if (slot != -1 && current != slot) {
         if (this.previousSlot == -1 && this.returnSlot.getValue()) {
            this.previousSlot = current;
         }

         client.player.getInventory().setSelectedSlot(slot);
      }
   }

   private void startEating(Minecraft client) {
      if (!this.eating && !client.options.keyUse.isDown()) {
         client.options.keyUse.setDown(true);
         this.eating = true;
      }
   }

   private void stopEating(Minecraft client) {
      if (this.eating) {
         client.options.keyUse.setDown(false);
         this.eating = false;
         if (client.player != null && this.previousSlot != -1 && this.returnSlot.getValue()) {
            client.player.getInventory().setSelectedSlot(this.previousSlot);
            this.previousSlot = -1;
         }
      }
   }
}
