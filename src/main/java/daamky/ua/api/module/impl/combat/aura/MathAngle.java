package daamky.ua.api.module.impl.combat.aura;

import daamky.ua.api.module.impl.combat.AuraModule;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public final class MathAngle {
   private MathAngle() {
   }

   public static Angle fromVec2f(Vec2 vector) {
      return new Angle(vector.y, vector.x);
   }

   public static Angle fromVec3d(Vec3 vector) {
      return new Angle(
         (float)Mth.wrapDegrees(Math.toDegrees(Math.atan2(vector.z, vector.x)) - 90.0),
         (float)Mth.wrapDegrees(Math.toDegrees(-Math.atan2(vector.y, Math.hypot(vector.x, vector.z))))
      );
   }

   public static boolean counter(float target) {
      return (float)AuraModule.getInstance().getAttackHandler().getCount() % target == 0.0F && AuraModule.getInstance().getAttackHandler().getCount() > 0;
   }

   public static Angle fromTo(Vec3 from, Vec3 to) {
      return fromVec3d(to.subtract(from));
   }

   public static Angle calculateDelta(Angle start, Angle end) {
      return new Angle(Mth.wrapDegrees(end.getYaw() - start.getYaw()), Mth.wrapDegrees(end.getPitch() - start.getPitch()));
   }

   public static Angle calculateAngle(Vec3 to) {
      Minecraft client = Minecraft.getInstance();
      return client != null && client.player != null ? fromVec3d(to.subtract(client.player.getEyePosition())) : Angle.DEFAULT;
   }

   public static Angle cameraAngle() {
      Minecraft client = Minecraft.getInstance();
      return client != null && client.player != null ? new Angle(client.player.getYRot(), client.player.getXRot()) : Angle.DEFAULT;
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
