package daamky.ua.mixin;

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
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

@Mixin({Entity.class})
public abstract class EntityMixin {
   public EntityMixin() {
   }

   @Inject(
      method = {"getBoundingBox"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void daamky$expandCombatHitBox(CallbackInfoReturnable<AABB> cir) {
      if ((Object)this instanceof LivingEntity living) {
         cir.setReturnValue(HitBoxModule.apply(living, (AABB)cir.getReturnValue()));
      }
   }

   @Inject(
      method = {"push(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$handleEntityPush(Entity entity, CallbackInfo ci) {
      LocalPlayer player = Minecraft.getInstance().player;
      Entity self = (Entity)(Object)this;
      if (player != null && (self == player || entity == player)) {
         PushEvent event = Manager.postEvent(new PushEvent(PushEvent.Type.COLLISION));
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"isPushedByFluid"},
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
      method = {"moveRelative"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"
      ),
      require = 0
   )
   private Vec3 daamky$fixMoveRelative(Vec3 movementInput, float speed, float yaw) {
      if (this.daamky$isLocalPlayer()) {
         LocalPlayer player = Minecraft.getInstance().player;
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
      method = {"calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private float daamky$packetPitch(float pitch) {
      return this.daamky$isLocalPlayer() && AngleConnection.INSTANCE.shouldApplyPacketRotation() ? AngleConnection.INSTANCE.getPacketPitch() : pitch;
   }

   @ModifyVariable(
      method = {"calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"},
      at = @At("HEAD"),
      ordinal = 1,
      argsOnly = true
   )
   private float daamky$packetYaw(float yaw) {
      return this.daamky$isLocalPlayer() && AngleConnection.INSTANCE.shouldApplyPacketRotation() ? AngleConnection.INSTANCE.getPacketYaw() : yaw;
   }

   @Unique
   private boolean daamky$isLocalPlayer() {
      LocalPlayer player = Minecraft.getInstance().player;
      return player != null && (Object)this == player;
   }

   @Unique
   private static Vec3 daamky$getInputVector(Vec3 movementInput, float speed, float yaw) {
      double length = movementInput.lengthSqr();
      if (length < 1.0E-7) {
         return Vec3.ZERO;
      } else {
         Vec3 scaled = (length > 1.0 ? movementInput.normalize() : movementInput).scale((double)speed);
         float sin = Mth.sin((double)(yaw * (float) (Math.PI / 180.0)));
         float cos = Mth.cos((double)(yaw * (float) (Math.PI / 180.0)));
         return new Vec3(scaled.x * (double)cos - scaled.z * (double)sin, scaled.y, scaled.z * (double)cos + scaled.x * (double)sin);
      }
   }
}

