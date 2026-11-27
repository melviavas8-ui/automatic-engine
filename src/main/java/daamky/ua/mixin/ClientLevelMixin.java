package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.NoRender;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;

@Mixin({ClientLevel.class})
public abstract class ClientLevelMixin {
   @Shadow
   @Final
   private ClientLevelData clientLevelData;

   public ClientLevelMixin() {
   }

   @Inject(
      method = {"tickTime"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$tickTime(CallbackInfo ci) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.isEnabled()) {
         ClientLevel level = (ClientLevel)(Object)this;
         long time = ambience.getInternalTime();
         this.clientLevelData.setDayTime(time);
         ambience.syncWeather(level, this.clientLevelData);
         ci.cancel();
      }
   }

   @Inject(
      method = {"addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$addParticle(
      ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci
   ) {
      if (daamky$shouldCancelHitParticle(parameters)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$addParticle(
      ParticleOptions parameters,
      boolean force,
      boolean canSpawnOnMinimal,
      double x,
      double y,
      double z,
      double velocityX,
      double velocityY,
      double velocityZ,
      CallbackInfo ci
   ) {
      if (daamky$shouldCancelHitParticle(parameters)) {
         ci.cancel();
      }
   }

   private static boolean daamky$shouldCancelHitParticle(ParticleOptions parameters) {
      return parameters != null && NoRender.isActive("Hit Particles")
         ? parameters.getType() == ParticleTypes.DAMAGE_INDICATOR
            || parameters.getType() == ParticleTypes.CRIT
            || parameters.getType() == ParticleTypes.ENCHANTED_HIT
            || parameters.getType() == ParticleTypes.SWEEP_ATTACK
         : false;
   }
}

