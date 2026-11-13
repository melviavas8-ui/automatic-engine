package daamky.ua.api.module.impl.combat.aura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;

public final class MathAngle {
   private MathAngle() {
   }

   public static Angle fromVec2f(Vec2f vector) {
      return new Angle(vector.y, vector.x);
   }

   public static Angle fromVec3d(Vec3d vector) {
      return new Angle(
         (float)MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)MathHelper.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   public static boolean counter(float target) {
      return (float)AuraModule.getInstance().getAttackHandler().getCount() % target == 0.0F && AuraModule.getInstance().getAttackHandler().getCount() > 0;
   }

   public static Angle fromTo(Vec3d from, Vec3d to) {
      return fromVec3d(to.subtract(from));
   }

   public static Angle calculateDelta(Angle start, Angle end) {
      return new Angle(MathHelper.wrapDegrees(end.getYaw() - start.getYaw()), MathHelper.wrapDegrees(end.getPitch() - start.getPitch()));
   }

   public static Angle calculateAngle(Vec3d to) {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.player != null ? fromVec3d(to.subtract(client.player.getEyePos())) : Angle.DEFAULT;
   }

   public static Angle cameraAngle() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null && client.player != null ? new Angle(client.player.getYaw(), client.player.getPitch()) : Angle.DEFAULT;
   }

   public static double difference(Angle first, Angle second) {
      if (first != null && second != null) {
         Angle delta = calculateDelta(first, second);
         return Math.hypot((double)delta.getYaw(), (double)delta.getPitch());
      } else {
         return 0.0;
      }
   }
}
