package daamky.ua.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.events.impl.PlayerVelocityStrafeEvent;
import daamky.ua.api.events.impl.PushEvent;
import daamky.ua.api.module.impl.combat.HitBoxModule;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.manager.Manager;
import daamky.ua.utils.player.BaritoneMovementHelper;

@Mixin({Entity.class})
public abstract class EntityMixin {
   public EntityMixin() {
   }

   @Inject(
      method = {"getBoundingBox"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void daamky$expandCombatHitBox(CallbackInfoReturnable<Box> cir) {
      if ((Object)this instanceof LivingEntity living) {
         cir.setReturnValue(HitBoxModule.apply(living, (Box)cir.getReturnValue()));
      }
   }

   @Inject(
      method = {"pushAwayFrom"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$handleEntityPush(Entity entity, CallbackInfo ci) {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      Entity self = (Entity)(Object)this;
      if (player != null && (self == player || entity == player)) {
         PushEvent event = Manager.postEvent(new PushEvent(PushEvent.Type.COLLISION));
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"isPushedByFluids"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$handleFluidPush(CallbackInfoReturnable<Boolean> cir) {
      if (this.daamky$isLocalPlayer()) {
         PushEvent event = Manager.postEvent(new PushEvent(PushEvent.Type.WATER));
         if (event.isCancelled()) {
            cir.setReturnValue(false);
         }
      }
   }

   @Redirect(
      method = {"updateVelocity"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"
      ),
      require = 0
   )
   private Vec3d daamky$fixMoveRelative(Vec3d movementInput, float speed, float yaw) {
      if (this.daamky$isLocalPlayer()) {
         ClientPlayerEntity player = MinecraftClient.getInstance().player;
         if (BaritoneMovementHelper.isMovementControlled(player)) {
            return daamky$getInputVector(movementInput, speed, yaw);
         } else {
            PlayerVelocityStrafeEvent event = Manager.postEvent(
               new PlayerVelocityStrafeEvent(movementInput, speed, yaw, daamky$getInputVector(movementInput, speed, yaw))
            );
            return event.getVelocity();
         }
      } else {
         return daamky$getInputVector(movementInput, speed, yaw);
      }
   }

   @ModifyVariable(
      method = {"getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private float daamky$packetPitch(float pitch) {
      return this.daamky$isLocalPlayer() && AngleConnection.INSTANCE.shouldApplyPacketRotation() ? AngleConnection.INSTANCE.getPacketPitch() : pitch;
   }

   @ModifyVariable(
      method = {"getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"},
      at = @At("HEAD"),
      ordinal = 1,
      argsOnly = true
   )
   private float daamky$packetYaw(float yaw) {
      return this.daamky$isLocalPlayer() && AngleConnection.INSTANCE.shouldApplyPacketRotation() ? AngleConnection.INSTANCE.getPacketYaw() : yaw;
   }

   @Unique
   private boolean daamky$isLocalPlayer() {
      ClientPlayerEntity player = MinecraftClient.getInstance().player;
      return player != null && (Object)this == player;
   }

   @Unique
   private static Vec3d daamky$getInputVector(Vec3d movementInput, float speed, float yaw) {
      double length = movementInput.lengthSquared();
      if (length < 1.0E-7) {
         return Vec3d.ZERO;
      } else {
         Vec3d scaled = (length > 1.0 ? movementInput.normalize() : movementInput).multiply((double)speed);
         float sin = MathHelper.sin((double)(yaw * (float) (Math.PI / 180.0)));
         float cos = MathHelper.cos((double)(yaw * (float) (Math.PI / 180.0)));
         return new Vec3d(scaled.x * (double)cos - scaled.z * (double)sin, scaled.y, scaled.z * (double)cos + scaled.x * (double)sin);
      }
   }
}

