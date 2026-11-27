package daamky.ua.api.module.impl.combat.aura.rotations;

import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.utils.chat.ChatMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class Neuro extends RotateConstructor {
   float yaw;
   float pitch;

   public Neuro() {
      super("Neuro");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3 vec3d, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      if (aura == null) {
         return currentAngle;
      } else {
         StrikeManager attackHandler = aura.getAttackHandlerRaw();
         if (attackHandler == null) {
            return currentAngle;
         } else {
            Minecraft mc = Minecraft.getInstance();
            StrikerConstructor.AttackPerpetratorConfigurable config = aura.getConfig();
            Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
            Angle delta = MathAngle.calculateDelta(new Angle(this.yaw, this.pitch), targetAngle);
            float currentYaw = mc.player.getYRot();
            float currentPitch = mc.player.getXRot();
            float yawStep = Mth.wrapDegrees(currentYaw - this.yaw);
            float pitchStep = currentPitch - this.pitch;
            this.yaw = currentYaw;
            this.pitch = currentPitch;
            float deltp = delta.getPitch();
            float delty = delta.getYaw();
            float distance = mc.player.distanceTo(entity);
            ChatMessage.brandmessage("ЯВ" + delty + "PITCH" + deltp);
            this.yaw = currentYaw;
            this.pitch = currentPitch;
            return currentAngle;
         }
      }
   }

   @Override
   public Vec3 randomValue() {
      return Vec3.ZERO;
   }
}
