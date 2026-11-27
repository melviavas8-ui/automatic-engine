package daamky.ua.api.module.impl.combat.aura.rotations;

import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class LegitAngle extends RotateConstructor {
   private long lastFrameNs;
   private Angle lastOut = new Angle(0.0F, 0.0F);

   public LegitAngle() {
      super("Legit");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      Minecraft client = Minecraft.getInstance();
      if (client != null && client.player != null) {
         return new Angle(client.player.getYRot(), client.player.getXRot());
      } else {
         return currentAngle != null ? currentAngle : new Angle(0.0F, 0.0F);
      }
   }

   public Angle stepFrame(Entity entity) {
      Minecraft client = Minecraft.getInstance();
      AuraModule aura = AuraModule.getInstance();
      if (client != null && client.player != null && aura != null && entity instanceof LivingEntity living) {
         Vec3 direction = living.position()
            .add(0.0, Mth.clamp(client.player.getEyeY() - living.getY(), 0.0, 1.0), 0.0)
            .subtract(client.player.getEyePosition());
         if (direction.lengthSqr() < 1.0E-8) {
            this.lastOut = new Angle(client.player.getYRot(), client.player.getXRot());
            return this.lastOut;
         } else {
            direction = direction.normalize();
            float targetYaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float currentYaw = client.player.getYRot();
            float yawDelta = Mth.wrapDegrees(targetYaw - currentYaw);
            float speed = Mth.clamp(aura.getLegitSpeed().getFloat(), 0.02F, 0.4F);
            float frameScale = Mth.clamp(this.frameDelta(), 0.25F, 4.0F);
            float factor = 1.0F - (float)Math.pow((double)(1.0F - speed), (double)frameScale);
            this.lastOut = new Angle(currentYaw + yawDelta * factor, client.player.getXRot());
            return this.lastOut;
         }
      } else {
         float yaw = client != null && client.player != null ? client.player.getYRot() : 0.0F;
         float pitch = client != null && client.player != null ? client.player.getXRot() : 0.0F;
         this.lastOut = new Angle(yaw, pitch);
         return this.lastOut;
      }
   }

   public Angle getLast() {
      return this.lastOut;
   }

   private float frameDelta() {
      long now = System.nanoTime();
      if (this.lastFrameNs == 0L) {
         this.lastFrameNs = now;
         return 1.0F;
      } else {
         float scale = (float)(now - this.lastFrameNs) / 1.6666667E7F;
         this.lastFrameNs = now;
         return scale;
      }
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }

   public void reset() {
      this.lastFrameNs = 0L;
      this.lastOut = new Angle(0.0F, 0.0F);
   }
}
