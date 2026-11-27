package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import daamky.ua.api.events.impl.FireworkEvent;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.movement.SuperFireWork;
import daamky.ua.manager.Manager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin({FireworkRocketEntity.class})
public abstract class FireworkRocketEntityMixin extends Entity {
   @Shadow
   @Nullable
   private LivingEntity attachedToEntity;

   private FireworkRocketEntityMixin(Level level) {
      super(EntityType.FIREWORK_ROCKET, level);
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"
      )},
      require = 0
   )
   private Vec3 daamky$getRotationVectorHook(LivingEntity instance, Operation<Vec3> original) {
      return this.daamky$shouldModifyFireworkBoost() && this.attachedToEntity == Minecraft.getInstance().player && this.attachedToEntity.isFallFlying()
         ? AngleConnection.INSTANCE.getMoveRotation().toVector()
         : (Vec3)original.call(new Object[]{instance});
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )},
      require = 0
   )
   private void daamky$setVelocityHook(LivingEntity instance, Vec3 velocity, Operation<Void> original) {
      if (this.daamky$shouldModifyFireworkBoost() && this.attachedToEntity == Minecraft.getInstance().player && this.attachedToEntity.isFallFlying()) {
         FireworkEvent event = Manager.postEvent(new FireworkEvent(velocity));
         original.call(new Object[]{instance, event.getVector()});
      } else {
         original.call(new Object[]{instance, velocity});
      }
   }

   @Unique
   private boolean daamky$shouldModifyFireworkBoost() {
      ModuleManager modules = Manager.getModules();
      return modules != null && modules.isEnabled(SuperFireWork.class);
   }
}
