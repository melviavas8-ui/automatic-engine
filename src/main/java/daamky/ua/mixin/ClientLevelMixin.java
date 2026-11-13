package daamky.ua.mixin;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.NoRender;

@Mixin({ClientWorld.class})
public abstract class ClientLevelMixin {
   @Shadow
   @Final
   private Properties clientWorldProperties;

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
         ClientWorld level = (ClientWorld)(Object)this;
         long time = ambience.getInternalTime();
         this.clientWorldProperties.setTimeOfDay(time);
         ambience.syncWeather(level, this.clientWorldProperties);
         ci.cancel();
      }
   }

   @Inject(
      method = {"addParticleClient(Lnet/minecraft/particle/ParticleEffect;DDDDDD)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$addParticle(
      ParticleEffect parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci
   ) {
      if (daamky$shouldCancelHitParticle(parameters)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"addParticleClient(Lnet/minecraft/particle/ParticleEffect;ZZDDDDDD)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$addParticle(
      ParticleEffect parameters,
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

   private static boolean daamky$shouldCancelHitParticle(ParticleEffect parameters) {
      return parameters != null && NoRender.isActive("Hit Particles")
         ? parameters.getType() == ParticleTypes.DAMAGE_INDICATOR
            || parameters.getType() == ParticleTypes.CRIT
            || parameters.getType() == ParticleTypes.ENCHANTED_HIT
            || parameters.getType() == ParticleTypes.SWEEP_ATTACK
         : false;
   }
}

