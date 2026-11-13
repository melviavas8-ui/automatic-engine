package daamky.ua.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;

@Mixin({Item.class})
public abstract class ItemMixin {
   public ItemMixin() {
   }

   @Redirect(
      method = {"raycast"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getPitch()F"
      ),
      require = 0
   )
   private static float daamky$itemPitch(PlayerEntity player) {
      AngleConnection controller = AngleConnection.INSTANCE;
      return controller.shouldApplyPacketRotation() ? controller.getPacketPitch() : player.getPitch();
   }

   @Redirect(
      method = {"raycast"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"
      ),
      require = 0
   )
   private static float daamky$itemYaw(PlayerEntity player) {
      AngleConnection controller = AngleConnection.INSTANCE;
      return controller.shouldApplyPacketRotation() ? controller.getPacketYaw() : player.getYaw();
   }
}
