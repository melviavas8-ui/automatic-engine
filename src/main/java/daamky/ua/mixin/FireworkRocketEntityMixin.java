package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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

@Mixin({FireworkRocketEntity.class})
public abstract class FireworkRocketEntityMixin extends Entity {
   @Shadow
   @Nullable
   private LivingEntity shooter;

   private FireworkRocketEntityMixin(World level) {
      super(EntityType.FIREWORK_ROCKET, level);
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"
      )},
      require = 0
   )
   private Vec3d daamky$getRotationVectorHook(LivingEntity instance, Operation<Vec3d> original) {
      return this.daamky$shouldModifyFireworkBoost() && this.shooter == MinecraftClient.getInstance().player && this.shooter.isGliding()
         ? AngleConnection.INSTANCE.getMoveRotation().toVector()
         : (Vec3d)original.call(new Object[]{instance});
   }

   @WrapOperation(
      method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/LivingEntity;setVelocity(Lnet/minecraft/util/math/Vec3d;)V"
      )},
      require = 0
   )
   private void daamky$setVelocityHook(LivingEntity instance, Vec3d velocity, Operation<Void> original) {
      if (this.daamky$shouldModifyFireworkBoost() && this.shooter == MinecraftClient.getInstance().player && this.shooter.isGliding()) {
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
