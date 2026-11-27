package daamky.ua.api.module.impl.player;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.mixin.accessor.LivingEntityAccessor;
import daamky.ua.mixin.accessor.MinecraftAccessor;
import daamky.ua.mixin.accessor.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

public final class NoDelay extends Module {
   private static NoDelay instance;
   private final MultiModeSetting ignoreSetting = this.register(
      new MultiModeSetting(
         "Type", "Client delays to remove.", new String[]{"Jump", "Right Click", "Break Delay", "Experience Bottle"}, "Jump", "Experience Bottle"
      )
   );

   public NoDelay() {
      super("No Delay", "Removes selected client delays.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static NoDelay getInstance() {
      return instance;
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null) {
         if (this.ignoreSetting.isSelected("Break Delay") && client.gameMode != null) {
            ((MultiPlayerGameModeAccessor)client.gameMode).daamky$setDestroyDelay(0);
         }

         if (this.ignoreSetting.isSelected("Jump")) {
            ((LivingEntityAccessor)client.player).daamky$setJumpingCooldown(0);
         }

         if (this.shouldBypassItemUseCooldown()) {
            ((MinecraftAccessor)client).daamky$setRightClickDelay(0);
         }
      }
   }

   public boolean shouldBypassItemUseCooldown() {
      Minecraft client = Minecraft.getInstance();
      if (!this.isEnabled() || client.player == null) {
         return false;
      } else {
         return this.ignoreSetting.isSelected("Right Click") ? true : this.shouldBypassExperienceBottleUse();
      }
   }

   public boolean shouldBypassExperienceBottleUse() {
      Minecraft client = Minecraft.getInstance();
      return this.isEnabled()
         && client.player != null
         && this.ignoreSetting.isSelected("Experience Bottle")
         && (client.player.getMainHandItem().getItem() == Items.EXPERIENCE_BOTTLE || client.player.getOffhandItem().getItem() == Items.EXPERIENCE_BOTTLE);
   }
}
