package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;

@Mixin({ServerboundUseItemPacket.class})
public abstract class PlayerInteractItemC2SPacketMixin {
   @Mutable
   @Shadow
   @Final
   private float yRot;
   @Mutable
   @Shadow
   @Final
   private float xRot;

   public PlayerInteractItemC2SPacketMixin() {
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/world/InteractionHand;IFF)V"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$modifyUseItemRotation(InteractionHand hand, int sequence, float yaw, float pitch, CallbackInfo ci) {
      AngleConnection controller = AngleConnection.INSTANCE;
      if (controller.shouldApplyPacketRotation()) {
         this.yRot = controller.getPacketYaw();
         this.xRot = controller.getPacketPitch();
      }
   }
}
