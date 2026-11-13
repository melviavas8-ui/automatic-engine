package daamky.ua.api.module.impl.combat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionAndOnGround;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.JumpEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;

public class Criticals extends Module {
   private boolean funTimeWeakJump;
   public final ModeSetting mode = this.register(new ModeSetting("Mode", "Packet critical mode.", "Vanilla", "Vanilla", "Matrix", "FunTime"));
   public static volatile Criticals INSTANCE;

   public Criticals() {
      super("Criticals", "Пакетно помогает нанести крит", ModuleCategory.COMBAT);
      INSTANCE = this;
   }

   public static Criticals getInstance() {
      return INSTANCE;
   }

   @SubscribeEvent
   public void onTick(TickEvent.Post event) {
      ClientPlayerEntity player = event.getClient().player;
      AuraModule aura = AuraModule.getInstance();
      if (player != null && player.networkHandler != null && aura != null) {
         if (this.mode.is("Vanilla")) {
            if (!aura.canAttack(0)) {
               return;
            }

            if (player.fallDistance <= 0.0) {
               player.networkHandler
                  .sendPacket(new PositionAndOnGround(player.getX(), player.getY() + 0.0125, player.getZ(), false, player.horizontalCollision));
               player.networkHandler.sendPacket(new PositionAndOnGround(player.getX(), player.getY(), player.getZ(), false, player.horizontalCollision));
            }
         }

         if (this.mode.is("Matrix") && aura.canAttack(0)) {
            player.setVelocity(new Vec3d(0.0, -0.1, 0.0));
         }
      }
   }

   @SubscribeEvent
   public void onJump(JumpEvent event) {
      if (!this.mode.is("FunTime")) {
         this.funTimeWeakJump = false;
      } else {
         event.setMotionMultiplier(0.925);
      }
   }

   @Override
   protected void onDisable() {
      this.funTimeWeakJump = false;
   }
}
