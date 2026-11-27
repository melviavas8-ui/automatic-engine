package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.CameraPositionEvent;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.player.FreeLook;
import daamky.ua.api.module.impl.visual.Animations;
import daamky.ua.manager.Manager;
import daamky.ua.utils.modules.warden.rotation.FreeLookController;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin({Camera.class})
public abstract class CameraMixin {
   public CameraMixin() {
   }

   @Shadow
   protected abstract void setPosition(double var1, double var3, double var5);

   @Shadow
   protected abstract void move(float var1, float var2, float var3);

   @ModifyExpressionValue(
      method = {"setup"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getViewYRot(F)F"
      )}
   )
   private float daamky$freeLookYaw(float original) {
      if (FreeLookController.isActive()) {
         return FreeLookController.getFreeYaw();
      } else {
         Angle angle = FreeLook.getActiveAngle();
         if (angle != null) {
            return angle.getYaw();
         } else {
            Angle legitAngle = this.daamky$getLegitAuraCameraAngle();
            return legitAngle != null ? legitAngle.getYaw() : original;
         }
      }
   }

   @ModifyExpressionValue(
      method = {"setup"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getViewXRot(F)F"
      )}
   )
   private float daamky$freeLookPitch(float original) {
      if (FreeLookController.isActive()) {
         return FreeLookController.getFreePitch();
      } else {
         Angle angle = FreeLook.getActiveAngle();
         if (angle != null) {
            return angle.getPitch();
         } else {
            Angle legitAngle = this.daamky$getLegitAuraCameraAngle();
            return legitAngle != null ? legitAngle.getPitch() : original;
         }
      }
   }

   @Unique
   private Angle daamky$getLegitAuraCameraAngle() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() && aura.isLegitMode() ? AngleConnection.INSTANCE.getCurrentAngle() : null;
   }

   @Inject(
      method = {"setPosition(Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$cameraPositionHook(Vec3 pos, CallbackInfo ci) {
      CameraPositionEvent event = Manager.postEvent(new CameraPositionEvent(pos));
      Vec3 nextPos = event.getPos();
      if (nextPos != null && Double.isFinite(nextPos.x) && Double.isFinite(nextPos.y) && Double.isFinite(nextPos.z)) {
         if (!(nextPos.distanceToSqr(pos) <= 1.0E-8)) {
            this.setPosition(nextPos.x, nextPos.y, nextPos.z);
            ci.cancel();
         }
      }
   }

   @ModifyExpressionValue(
      method = {"setup"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"
      )}
   )
   private float daamky$animateThirdPersonDistance(float zoom) {
      return zoom * Animations.perspectiveProgress(true);
   }

   @Inject(
      method = {"setup"},
      at = {@At("TAIL")}
   )
   private void daamky$animateFirstPersonReturn(Level level, Entity entity, boolean detached, boolean reverse, float partialTick, CallbackInfo ci) {
      if (!detached && Animations.perspectiveEnabled()) {
         float remainingThirdPerson = Animations.perspectiveProgress(false);
         if (remainingThirdPerson > 0.001F) {
            this.move(-4.0F * remainingThirdPerson, 0.0F, 0.0F);
         }
      }
   }
}
