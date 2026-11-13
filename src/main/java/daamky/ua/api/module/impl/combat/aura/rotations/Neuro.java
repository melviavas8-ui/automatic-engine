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
import daamky.ua.utils.chat.ChatMessage;

public class Neuro extends RotateConstructor {
   float yaw;
   float pitch;

   public Neuro() {
      super("Neuro");
   }

   @Override
   public Angle limitAngleChange(Angle currentAngle, Angle targetAngle, Vec3d vec3d, Entity entity) {
      AuraModule aura = AuraModule.getInstance();
      if (aura == null) {
         return currentAngle;
      } else {
         StrikeManager attackHandler = aura.getAttackHandlerRaw();
         if (attackHandler == null) {
            return currentAngle;
         } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            StrikerConstructor.AttackPerpetratorConfigurable config = aura.getConfig();
            Angle angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
            Angle delta = MathAngle.calculateDelta(new Angle(this.yaw, this.pitch), targetAngle);
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();
            float yawStep = MathHelper.wrapDegrees(currentYaw - this.yaw);
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
   public Vec3d randomValue() {
      return Vec3d.ZERO;
   }
}
