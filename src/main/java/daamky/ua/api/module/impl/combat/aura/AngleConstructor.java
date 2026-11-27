package daamky.ua.api.module.impl.combat.aura;

import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.rotations.FunTimeSnapAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.funtime.FunTimeResetAngle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class AngleConstructor {
   private final Angle angle;
   private final Vec3 vec3d;
   private final Entity entity;
   private final RotateConstructor angleSmooth;
   private final int ticksUntilReset;
   private final float resetThreshold;
   private final boolean moveCorrection;
   private final boolean freeCorrection;
   private boolean changeLook;

   public AngleConstructor(
      Angle angle,
      Vec3 vec3d,
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
      Minecraft client = Minecraft.getInstance();
      if (isResetting && client != null && client.player != null) {
         Vec2 rotationVector = client.player.getRotationVector();
         RotateConstructor resetSmooth = (RotateConstructor)(this.angleSmooth instanceof FunTimeSnapAngle ? FunTimeResetAngle.INSTANCE : this.angleSmooth);
         return resetSmooth.limitAngleChange(fromAngle, MathAngle.fromVec2f(rotationVector));
      } else {
         return this.angleSmooth.limitAngleChange(fromAngle, this.angle, this.vec3d, this.entity);
      }
   }

   public Angle getAngle() {
      return this.angle;
   }

   public Vec3 getVec3d() {
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
