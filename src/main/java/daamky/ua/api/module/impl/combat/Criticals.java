package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.JumpEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Pos;
import net.minecraft.world.phys.Vec3;

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
      LocalPlayer player = event.getClient().player;
      AuraModule aura = AuraModule.getInstance();
      if (player != null && player.connection != null && aura != null) {
         if (this.mode.is("Vanilla")) {
            if (!aura.canAttack(0)) {
               return;
            }

            if (player.fallDistance <= 0.0) {
               player.connection
                  .send(new Pos(player.getX(), player.getY() + 0.0125, player.getZ(), false, player.horizontalCollision));
               player.connection.send(new Pos(player.getX(), player.getY(), player.getZ(), false, player.horizontalCollision));
            }
         }

         if (this.mode.is("Matrix") && aura.canAttack(0)) {
            player.setDeltaMovement(new Vec3(0.0, -0.1, 0.0));
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
