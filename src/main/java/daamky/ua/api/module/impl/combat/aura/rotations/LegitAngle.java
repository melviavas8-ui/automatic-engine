package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;

public final class LegitAngle extends RotateConstructor {
   private long lastFrameNs;
   private Angle lastOut = new Angle(0.0F, 0.0F);

   public LegitAngle() {
      super("Legit");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.player != null) {
         return new Angle(client.player.getYaw(), client.player.getPitch());
      } else {
         return currentAngle != null ? currentAngle : new Angle(0.0F, 0.0F);
      }
   }

   public Angle stepFrame(Entity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      AuraModule aura = AuraModule.getInstance();
      if (client != null && client.player != null && aura != null && entity instanceof LivingEntity living) {
         Vec3d direction = living.getEntityPos()
            .add(0.0, MathHelper.clamp(client.player.getEyeY() - living.getY(), 0.0, 1.0), 0.0)
            .subtract(client.player.getEyePos());
         if (direction.lengthSquared() < 1.0E-8) {
            this.lastOut = new Angle(client.player.getYaw(), client.player.getPitch());
            return this.lastOut;
         } else {
            direction = direction.normalize();
            float targetYaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
            float currentYaw = client.player.getYaw();
            float yawDelta = MathHelper.wrapDegrees(targetYaw - currentYaw);
            float speed = MathHelper.clamp(aura.getLegitSpeed().getFloat(), 0.02F, 0.4F);
            float frameScale = MathHelper.clamp(this.frameDelta(), 0.25F, 4.0F);
            float factor = 1.0F - (float)Math.pow((double)(1.0F - speed), (double)frameScale);
            this.lastOut = new Angle(currentYaw + yawDelta * factor, client.player.getPitch());
            return this.lastOut;
         }
      } else {
         float yaw = client != null && client.player != null ? client.player.getYaw() : 0.0F;
         float pitch = client != null && client.player != null ? client.player.getPitch() : 0.0F;
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
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }

   public void reset() {
      this.lastFrameNs = 0L;
      this.lastOut = new Angle(0.0F, 0.0F);
   }
}
