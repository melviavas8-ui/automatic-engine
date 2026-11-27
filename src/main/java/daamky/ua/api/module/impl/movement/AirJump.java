package daamky.ua.api.module.impl.movement;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import net.minecraft.world.phys.AABB;

public final class AirJump extends Module {
   public AirJump() {
      super("AirJump", "", ModuleCategory.MOVEMENT);
   }

   @SubscribeEvent
   private void onMovePost(InputEvent event) {
      if (mc.player != null && mc.level != null && event.getInput().jump()) {
         AABB box = mc.player.getBoundingBox().move(0.0, -1.0, 0.0).inflate(0.1, 0.0, 0.1);
         boolean ХАЧЕБАНЫЙ = !mc.level.noCollision(mc.player, box);
         if (ХАЧЕБАНЫЙ && mc.player.tickCount % 2 == 0) {
            mc.player.fallDistance = 0.0;
            mc.player.jumpFromGround();
         }
      }
   }
}
