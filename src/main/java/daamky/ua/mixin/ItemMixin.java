package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

@Mixin({Item.class})
public abstract class ItemMixin {
   public ItemMixin() {
   }

   @Redirect(
      method = {"getPlayerPOVHitResult"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getXRot()F"
      ),
      require = 0
   )
   private static float daamky$itemPitch(Player player) {
      AngleConnection controller = AngleConnection.INSTANCE;
      return controller.shouldApplyPacketRotation() ? controller.getPacketPitch() : player.getXRot();
   }

   @Redirect(
      method = {"getPlayerPOVHitResult"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
      ),
      require = 0
   )
   private static float daamky$itemYaw(Player player) {
      AngleConnection controller = AngleConnection.INSTANCE;
      return controller.shouldApplyPacketRotation() ? controller.getPacketYaw() : player.getYRot();
   }
}
