package daamky.ua.api.module.impl.movement;

import net.minecraft.client.MinecraftClient;
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
      MinecraftClient client = MinecraftClient.getInstance();
      this.lastYaw = client.player != null ? client.player.getYaw() : 0.0F;
      this.lastPitch = client.player != null ? client.player.getPitch() : 0.0F;
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         boolean moving = MoveUtil.hasPlayerMovement();
         float yaw = client.player.getYaw();
         if (this.mode.is("Matrix")) {
            this.handleMatrixMode(moving);
         } else if (this.mode.is("Grim")) {
            this.handleGrimMode(client, moving);
         }

         this.lastYaw = yaw;
         this.lastPitch = client.player.getPitch();
      }
   }

   private void handleMatrixMode(boolean moving) {
      if (moving) {
         MoveUtil.moveYaw(MinecraftClient.getInstance().player.getYaw());
         MoveUtil.setVelocity(this.speed.getValue() * 1.5);
      } else {
         MoveUtil.setVelocity(0.0);
      }
   }

   private void handleGrimMode(MinecraftClient client, boolean moving) {
      if (moving) {
         float yaw = MoveUtil.moveYaw(client.player.getYaw());
         this.rotation.setYaw(yaw);
         this.rotation.setPitch(client.player.getPitch());
         if (AuraModule.target == null) {
            AngleConnection.INSTANCE.rotateTo(this.rotation, 1, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
         }
      }
   }
}
