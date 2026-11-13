package daamky.ua.api.module.impl.combat.aura.rotations;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;
import daamky.ua.api.module.impl.combat.aura.utils.FromVector;

public final class FTProfileAngle extends RotateConstructor {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private static int angleCounter;

   public FTProfileAngle() {
      super("FunTime New");
   }

   @Override
   public Angle limitAngleChange(Angle current, Angle target, Vec3d aimPoint, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      if (aura == null || MC.player == null) {
         return current;
      } else if (entity == null) {
         MC.options.attackKey.setPressed(false);
         Angle delta = MathAngle.calculateDelta(current, target);
         return new Angle(
            current.getYaw() + MathHelper.clamp(delta.getYaw(), -60.0F, 25.0F), current.getPitch() + MathHelper.clamp(delta.getPitch(), -15.0F, 15.0F)
         );
      } else {
         StrikeManager attackHandler = aura.getAttackHandlerRaw();
         StrikerConstructor.AttackPerpetratorConfigurable config = aura.getConfig();
         if (!attackHandler.canAttack(config, 1)) {
            target = MathAngle.fromVec3d(FromVector.closest(entity).subtract(MC.player.getEyePos()));
         }

         Angle delta = MathAngle.calculateDelta(current, target);
         boolean attackWindow = attackHandler.canAttack(config, 0);
         boolean afterAttack = !attackHandler.getAttackTimer().finished(400.0);
         float yawSpeed = attackWindow ? 60.0F : MathUtils.getRandom(0.0F, 20.0F);
         float pitchSpeed = attackWindow ? 15.0F : MathUtils.getRandom(0.0F, 5.0F);
         if (MC.player.distanceTo(entity) > 3.0F) {
            pitchSpeed = 0.0F;
         }

         if (!attackHandler.getAttackTimer().finished(300.0)) {
            pitchSpeed = 3.0F;
         }

         if (RaycastAngle.rayTrace(4.0, entity.getBoundingBox())) {
            pitchSpeed = MathUtils.getRandom(0.0F, 6.0F);
         }

         float yawJitter = MathUtils.smoothRandom(-MathUtils.smoothRandom(12.0F, 16.0F, 0.0F), MathUtils.smoothRandom(22.0F, 26.0F, 0.0F), 0.0F);
         float pitchJitter = MathUtils.smoothRandom(-MathUtils.smoothRandom(1.0F, 4.0F, 12.0F), MathUtils.smoothRandom(1.0F, 4.0F, 12.0F), 5.0F);
         float yawOffset = afterAttack ? MathUtils.smoothRandom(-35.0F, 35.0F, 8.0F) + yawJitter : yawJitter;
         if (attackHandler.canAttack(config, 0)) {
            yawOffset = 0.0F;
         }

         float yaw = current.getYaw() + MathHelper.clamp(delta.getYaw() + yawOffset, -yawSpeed, yawSpeed);
         if (counter(31) && !attackHandler.getAttackTimer().finished(300.0) && attackHandler.getAttackTimer().finished(200.0)) {
            MC.options.attackKey.setPressed(true);
         } else {
            MC.options.attackKey.setPressed(false);
         }

         float playerPitch = MC.player.getPitch();
         float pitch = MathHelper.clamp(current.getPitch() + MathHelper.clamp(delta.getPitch(), -pitchSpeed, pitchSpeed), -90.0F, 90.0F) + pitchJitter;
         return new Angle(yaw, MC.player.getPitch() + pitchJitter);
      }
   }

   private static boolean counter(int delay) {
      angleCounter++;
      return angleCounter % delay == 0;
   }

   @Override
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
