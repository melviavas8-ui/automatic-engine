package daamky.ua.api.module.impl.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;

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
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
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
      this.stopEating(MinecraftClient.getInstance());
   }

   private void handleEating(MinecraftClient client) {
      if (this.canEat(client)) {
         if (this.smartMode.getValue() && !this.hasGappleInHand(client)) {
            this.swapToGappleSlot(client);
         }

         this.startEating(client);
      } else if (this.eating && !this.shouldContinueEating(client)) {
         this.stopEating(client);
      }
   }

   private boolean canEat(MinecraftClient client) {
      if (client.player.isDead() || client.player.getItemCooldownManager().isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) {
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

   private boolean shouldContinueEating(MinecraftClient client) {
      return !client.player.isDead() && client.player.isUsingItem() && client.player.getActiveItem().getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGappleInHand(MinecraftClient client) {
      return client.player.getMainHandStack().getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGappleInOffhand(MinecraftClient client) {
      return client.player.getOffHandStack().getItem() == Items.GOLDEN_APPLE;
   }

   private boolean hasGapple(MinecraftClient client) {
      return this.hasGappleInOffhand(client) || this.findGappleInHotbar(client) != -1;
   }

   private int findGappleInHotbar(MinecraftClient client) {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = client.player.getInventory().getStack(i);
         if (stack.getItem() == Items.GOLDEN_APPLE) {
            return i;
         }
      }

      return -1;
   }

   private void swapToGappleSlot(MinecraftClient client) {
      int slot = this.findGappleInHotbar(client);
      int current = client.player.getInventory().getSelectedSlot();
      if (slot != -1 && current != slot) {
         if (this.previousSlot == -1 && this.returnSlot.getValue()) {
            this.previousSlot = current;
         }

         client.player.getInventory().setSelectedSlot(slot);
      }
   }

   private void startEating(MinecraftClient client) {
      if (!this.eating && !client.options.useKey.isPressed()) {
         client.options.useKey.setPressed(true);
         this.eating = true;
      }
   }

   private void stopEating(MinecraftClient client) {
      if (this.eating) {
         client.options.useKey.setPressed(false);
         this.eating = false;
         if (client.player != null && this.previousSlot != -1 && this.returnSlot.getValue()) {
            client.player.getInventory().setSelectedSlot(this.previousSlot);
            this.previousSlot = -1;
         }
      }
   }
}
