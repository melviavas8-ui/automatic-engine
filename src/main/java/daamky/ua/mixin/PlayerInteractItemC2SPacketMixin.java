package daamky.ua.mixin;

import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;

@Mixin({PlayerInteractItemC2SPacket.class})
public abstract class PlayerInteractItemC2SPacketMixin {
   @Mutable
   @Shadow
   @Final
   private float yaw;
   @Mutable
   @Shadow
   @Final
   private float pitch;

   public PlayerInteractItemC2SPacketMixin() {
   }

   @Inject(
      method = {"<init>(Lnet/minecraft/util/Hand;IFF)V"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$modifyUseItemRotation(Hand hand, int sequence, float yaw, float pitch, CallbackInfo ci) {
      AngleConnection controller = AngleConnection.INSTANCE;
      if (controller.shouldApplyPacketRotation()) {
         this.yaw = controller.getPacketYaw();
         this.pitch = controller.getPacketPitch();
      }
   }
}
