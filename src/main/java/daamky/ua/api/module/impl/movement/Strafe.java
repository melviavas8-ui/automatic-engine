package daamky.ua.api.module.impl.movement;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.move.MoveUtil;
import net.minecraft.client.Minecraft;

public final class Strafe extends Module {
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Strafe mode.", "Matrix", "Matrix", "Grim"));
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Strafe speed.", 0.32, 0.05, 1.0, 0.01));
   private final Angle rotation = new Angle(0.0F, 0.0F);
   private float lastYaw;
   private float lastPitch;

   public Strafe() {
      super("Strafe", "Stabilizes directional movement.", ModuleCategory.MOVEMENT);
      this.speed.visibleWhen(() -> this.mode.is("Matrix"));
   }

   @Override
   protected void onEnable() {
      Minecraft client = Minecraft.getInstance();
      this.lastYaw = client.player != null ? client.player.getYRot() : 0.0F;
      this.lastPitch = client.player != null ? client.player.getXRot() : 0.0F;
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         boolean moving = MoveUtil.hasPlayerMovement();
         float yaw = client.player.getYRot();
         if (this.mode.is("Matrix")) {
            this.handleMatrixMode(moving);
         } else if (this.mode.is("Grim")) {
            this.handleGrimMode(client, moving);
         }

         this.lastYaw = yaw;
         this.lastPitch = client.player.getXRot();
      }
   }

   private void handleMatrixMode(boolean moving) {
      if (moving) {
         MoveUtil.moveYaw(Minecraft.getInstance().player.getYRot());
         MoveUtil.setVelocity(this.speed.getValue() * 1.5);
      } else {
         MoveUtil.setVelocity(0.0);
      }
   }

   private void handleGrimMode(Minecraft client, boolean moving) {
      if (moving) {
         float yaw = MoveUtil.moveYaw(client.player.getYRot());
         this.rotation.setYaw(yaw);
         this.rotation.setPitch(client.player.getXRot());
         if (AuraModule.target == null) {
            AngleConnection.INSTANCE.rotateTo(this.rotation, 1, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
         }
      }
   }
}
