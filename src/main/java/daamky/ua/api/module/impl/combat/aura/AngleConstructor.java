package daamky.ua.api.module.impl.combat.aura;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.rotations.FunTimeSnapAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.funtime.FunTimeResetAngle;

public class AngleConstructor {
   private final Angle angle;
   private final Vec3d vec3d;
   private final Entity entity;
   private final RotateConstructor angleSmooth;
   private final int ticksUntilReset;
   private final float resetThreshold;
   private final boolean moveCorrection;
   private final boolean freeCorrection;
   private boolean changeLook;

   public AngleConstructor(
      Angle angle,
      Vec3d vec3d,
      Entity entity,
      RotateConstructor angleSmooth,
      int ticksUntilReset,
      float resetThreshold,
      boolean moveCorrection,
      boolean freeCorrection
   ) {
      this.angle = angle;
      this.vec3d = vec3d;
      this.entity = entity;
      this.angleSmooth = angleSmooth;
      this.ticksUntilReset = ticksUntilReset;
      this.resetThreshold = resetThreshold;
      this.moveCorrection = moveCorrection;
      this.freeCorrection = freeCorrection;
   }

   public Angle nextRotation(Angle fromAngle, boolean isResetting) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (isResetting && client != null && client.player != null) {
         Vec2f rotationVector = client.player.getRotationClient();
         RotateConstructor resetSmooth = (RotateConstructor)(this.angleSmooth instanceof FunTimeSnapAngle ? FunTimeResetAngle.INSTANCE : this.angleSmooth);
         return resetSmooth.limitAngleChange(fromAngle, MathAngle.fromVec2f(rotationVector));
      } else {
         return this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
      }
   }

   public Angle getAngle() {
      return this.angle;
   }

   public Vec3d getVec3d() {
      return this.vec3d;
   }

   public Entity getEntity() {
      return this.entity;
   }

   public int getTicksUntilReset() {
      return this.ticksUntilReset;
   }

   public float getResetThreshold() {
      return this.resetThreshold;
   }

   public boolean isMoveCorrection() {
      return this.moveCorrection;
   }

   public boolean isFreeCorrection() {
      return this.freeCorrection;
   }

   public boolean isChangeLook() {
      return this.changeLook;
   }

   public void setChangeLook(boolean changeLook) {
      this.changeLook = changeLook;
   }
}
