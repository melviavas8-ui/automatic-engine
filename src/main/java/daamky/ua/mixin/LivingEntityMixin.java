package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.events.impl.JumpEvent;
import daamky.ua.api.events.impl.SwingDurationEvent;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.AngleConstructor;
import daamky.ua.api.module.impl.movement.Fly;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.FogEffectSuppression;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

@Mixin(
   value = {LivingEntity.class},
   priority = 950
)
public abstract class LivingEntityMixin {
   @Unique
   private double daamky$jumpMotionMultiplier = 1.0;

   public LivingEntityMixin() {
   }

   @Shadow
   protected abstract double getEffectiveGravity();

   @Inject(
      method = {"hasEffect"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$hideDarknessFromFogHas(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
      if (FogEffectSuppression.hidesEffect((LivingEntity)(Object)this, effect)) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"getEffect"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$hideDarknessFromFogGet(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
      if (FogEffectSuppression.hidesEffect((LivingEntity)(Object)this, effect)) {
         cir.setReturnValue(null);
      }
   }

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;isFallFlying()Z"
      ),
      require = 0
   )
   private boolean daamky$elytraSpeedTravel(LivingEntity entity) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null && entity == client.player) {
         ModuleManager modules = Manager.getModules();
         if (modules != null) {
            Fly fly = modules.getByType(Fly.class).orElse(null);
            if (fly != null && fly.isEnabled() && fly.isElytraSpeedMode()) {
               return true;
            }
         }

         return entity.isFallFlying();
      } else {
         return entity.isFallFlying();
      }
   }

   @ModifyExpressionValue(
      method = {"jumpFromGround"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"
      )},
      require = 0
   )
   private float daamky$jumpYaw(float original) {
      if ((Object)this == Minecraft.getInstance().player && AngleConnection.INSTANCE.isMoveCorrectionActive()) {
         Angle angle = AngleConnection.INSTANCE.getMoveRotation();
         if (angle != null) {
            return angle.getYaw();
         }
      }

      return original;
   }

   @Inject(
      method = {"jumpFromGround"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$jumpEvent(CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer player) {
         JumpEvent event = Manager.postEvent(new JumpEvent(player));
         if (event.isCancelled()) {
            this.daamky$jumpMotionMultiplier = 1.0;
            ci.cancel();
            return;
         }

         this.daamky$jumpMotionMultiplier = event.getMotionMultiplier();
      }
   }

   @Inject(
      method = {"jumpFromGround"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$applyJumpMotionMultiplier(CallbackInfo ci) {
      if ((Object)this instanceof LocalPlayer player && this.daamky$jumpMotionMultiplier != 1.0) {
         Vec3 movement = player.getDeltaMovement();
         player.setDeltaMovement(movement.x, movement.y * this.daamky$jumpMotionMultiplier, movement.z);
      }

      this.daamky$jumpMotionMultiplier = 1.0;
   }

   @Inject(
      method = {"getCurrentSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$swingDuration(CallbackInfoReturnable<Integer> cir) {
      LocalPlayer player = Minecraft.getInstance().player;
      if (player != null && (Object)this == player) {
         SwingDurationEvent event = Manager.postEvent(new SwingDurationEvent());
         if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (MobEffectUtil.hasDigSpeed(player)) {
               animation *= (float)(6 - (1 + MobEffectUtil.getDigSpeedAmplification(player)));
            } else {
               animation *= player.hasEffect(MobEffects.MINING_FATIGUE)
                  ? (float)(6 + (1 + player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2)
                  : 6.0F;
            }

            cir.setReturnValue(Math.max(1, (int)animation));
         }
      }
   }

   @Inject(
      method = {"updateFallFlyingMovement"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$fallFlyingMovement(Vec3 oldVelocity, CallbackInfoReturnable<Vec3> cir) {
      if ((Object)this == Minecraft.getInstance().player) {
         AngleConnection rotationManager = AngleConnection.INSTANCE;
         Angle rotation = rotationManager.getRotation();
         AngleConstructor plan = rotationManager.getCurrentRotationPlan();
         if (rotation != null && plan != null && plan.isMoveCorrection() && !plan.isChangeLook()) {
            Vec3 look = rotation.toVector();
            float pitchRadians = rotation.getPitch() * (float) (Math.PI / 180.0);
            double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            double velocityHorizontal = oldVelocity.horizontalDistance();
            double gravity = this.getEffectiveGravity();
            double pitchCosSq = Mth.square(Math.cos((double)pitchRadians));
            oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + pitchCosSq * 0.75), 0.0);
            if (oldVelocity.y < 0.0 && lookHorizontal > 0.0) {
               double fallPull = oldVelocity.y * -0.1 * pitchCosSq;
               oldVelocity = oldVelocity.add(look.x * fallPull / lookHorizontal, fallPull, look.z * fallPull / lookHorizontal);
            }

            if (pitchRadians < 0.0F && lookHorizontal > 0.0) {
               double lift = velocityHorizontal * (double)(-Mth.sin((double)pitchRadians)) * 0.04;
               oldVelocity = oldVelocity.add(-look.x * lift / lookHorizontal, lift * 3.2, -look.z * lift / lookHorizontal);
            }

            if (lookHorizontal > 0.0) {
               oldVelocity = oldVelocity.add(
                  (look.x / lookHorizontal * velocityHorizontal - oldVelocity.x) * 0.1,
                  0.0,
                  (look.z / lookHorizontal * velocityHorizontal - oldVelocity.z) * 0.1
               );
            }

            cir.setReturnValue(oldVelocity.multiply(0.99F, 0.98F, 0.99F));
            cir.cancel();
         }
      }
   }
}

