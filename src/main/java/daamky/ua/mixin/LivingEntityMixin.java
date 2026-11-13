package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
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

   @Redirect(
      method = {"travel"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;isGliding()Z"
      ),
      require = 0
   )
   private boolean daamky$elytraSpeedTravel(LivingEntity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && entity == client.player) {
         ModuleManager modules = Manager.getModules();
         if (modules != null) {
            Fly fly = modules.getByType(Fly.class).orElse(null);
            if (fly != null && fly.isEnabled() && fly.isElytraSpeedMode()) {
               return true;
            }
         }

         return entity.isGliding();
      } else {
         return entity.isGliding();
      }
   }

   @ModifyExpressionValue(
      method = {"jump"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getYaw()F"
      )},
      require = 0
   )
   private float daamky$jumpYaw(float original) {
      if ((Object)this == MinecraftClient.getInstance().player && AngleConnection.INSTANCE.isMoveCorrectionActive()) {
         Angle angle = AngleConnection.INSTANCE.getMoveRotation();
         if (angle != null) {
            return angle.getYaw();
         }
      }

      return original;
   }

   @Inject(
      method = {"jump"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$jumpEvent(CallbackInfo ci) {
      if ((Object)this instanceof ClientPlayerEntity player) {
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
      method = {"jump"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$applyJumpMotionMultiplier(CallbackInfo ci) {
      if ((Object)this instanceof ClientPlayerEntity player && this.daamky$jumpMotionMultiplier != 1.0) {
         Vec3d movement = player.getVelocity();
         player.setVelocity(movement.x, movement.y * this.daamky$jumpMotionMultiplier, movement.z);
      }

      this.daamky$jumpMotionMultiplier = 1.0;
   }

   @Inject(
      method = {"getHandSwingDuration"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$swingDuration(CallbackInfoReturnable<Integer> cir) {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      if (player != null && (Object)this == player) {
         SwingDurationEvent event = Manager.postEvent(new SwingDurationEvent());
         if (event.isCancelled()) {
            float animation = event.getAnimation();
            if (StatusEffectUtil.hasHaste(player)) {
               animation *= (float)(6 - (1 + StatusEffectUtil.getHasteAmplifier(player)));
            } else {
               animation *= player.hasStatusEffect(StatusEffects.MINING_FATIGUE)
                  ? (float)(6 + (1 + player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) * 2)
                  : 6.0F;
            }

            cir.setReturnValue(Math.max(1, (int)animation));
         }
      }
   }

   @Inject(
      method = {"calcGlidingVelocity"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$fallFlyingMovement(Vec3d oldVelocity, CallbackInfoReturnable<Vec3d> cir) {
      if ((Object)this == MinecraftClient.getInstance().player) {
         AngleConnection rotationManager = AngleConnection.INSTANCE;
         Angle rotation = rotationManager.getRotation();
         AngleConstructor plan = rotationManager.getCurrentRotationPlan();
         if (rotation != null && plan != null && plan.isMoveCorrection() && !plan.isChangeLook()) {
            Vec3d look = rotation.toVector();
            float pitchRadians = rotation.getPitch() * (float) (Math.PI / 180.0);
            double lookHorizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            double velocityHorizontal = oldVelocity.horizontalLength();
            double gravity = this.getEffectiveGravity();
            double pitchCosSq = MathHelper.square(Math.cos((double)pitchRadians));
            oldVelocity = oldVelocity.add(0.0, gravity * (-1.0 + pitchCosSq * 0.75), 0.0);
            if (oldVelocity.y < 0.0 && lookHorizontal > 0.0) {
               double fallPull = oldVelocity.y * -0.1 * pitchCosSq;
               oldVelocity = oldVelocity.add(look.x * fallPull / lookHorizontal, fallPull, look.z * fallPull / lookHorizontal);
            }

            if (pitchRadians < 0.0F && lookHorizontal > 0.0) {
               double lift = velocityHorizontal * (double)(-MathHelper.sin((double)pitchRadians)) * 0.04;
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

