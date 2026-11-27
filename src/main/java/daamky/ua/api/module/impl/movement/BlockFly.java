package daamky.ua.api.module.impl.movement;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public final class BlockFly extends Module {
   private static final double MAX_GROUND_SPEED = 0.16779580771923064;
   private boolean ownsScaffold;

   public BlockFly() {
      super("BlockFly", "Move over automatically placed blocks.", ModuleCategory.MOVEMENT);
   }

   @Override
   protected void onEnable() {
      this.ownsScaffold = false;
      this.enableScaffold();
   }

   @Override
   protected void onDisable() {
      Scaffold scaffold = Scaffold.getInstance();
      if (this.ownsScaffold && scaffold != null && scaffold.isEnabled()) {
         scaffold.setEnabled(false);
      }

      this.ownsScaffold = false;
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         this.enableScaffold();
         if (client.player.onGround()) {
            Vec3 movement = client.player.getDeltaMovement();
            double horizontalSpeed = Math.hypot(movement.x, movement.z);
            if (!(horizontalSpeed <= 0.16779580771923064) && !(horizontalSpeed <= 1.0E-6)) {
               double scale = 0.16779580771923064 / horizontalSpeed;
               client.player.setDeltaMovement(movement.x * scale, movement.y, movement.z * scale);
            }
         }
      }
   }

   private void enableScaffold() {
      Scaffold scaffold = Scaffold.getInstance();
      if (scaffold != null && !scaffold.isEnabled()) {
         scaffold.setEnabled(true);
         this.ownsScaffold = true;
      }
   }
}
