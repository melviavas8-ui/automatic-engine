package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.Ambience;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;

@Mixin({Biome.class})
public abstract class BiomeWeatherMixin {
   public BiomeWeatherMixin() {
   }

   @Inject(
      method = {"hasPrecipitation"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$hasPrecipitation(CallbackInfoReturnable<Boolean> cir) {
      Ambience ambience = Ambience.getInstance();
      Precipitation precipitation = ambience != null ? ambience.getForcedPrecipitation() : null;
      if (precipitation != null) {
         cir.setReturnValue(precipitation != Precipitation.NONE);
      }
   }

   @Inject(
      method = {"getPrecipitationAt"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getPrecipitationAt(BlockPos pos, int seaLevel, CallbackInfoReturnable<Precipitation> cir) {
      Ambience ambience = Ambience.getInstance();
      Precipitation precipitation = ambience != null ? ambience.getForcedPrecipitation() : null;
      if (precipitation != null) {
         cir.setReturnValue(precipitation);
      }
   }

   @Inject(
      method = {"coldEnoughToSnow"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$coldEnoughToSnow(BlockPos pos, int seaLevel, CallbackInfoReturnable<Boolean> cir) {
      Ambience ambience = Ambience.getInstance();
      Precipitation precipitation = ambience != null ? ambience.getForcedPrecipitation() : null;
      if (precipitation == Precipitation.SNOW) {
         cir.setReturnValue(true);
      } else if (precipitation == Precipitation.RAIN || precipitation == Precipitation.NONE) {
         cir.setReturnValue(false);
      }
   }

   @Inject(
      method = {"warmEnoughToRain"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$warmEnoughToRain(BlockPos pos, int seaLevel, CallbackInfoReturnable<Boolean> cir) {
      Ambience ambience = Ambience.getInstance();
      Precipitation precipitation = ambience != null ? ambience.getForcedPrecipitation() : null;
      if (precipitation == Precipitation.SNOW) {
         cir.setReturnValue(false);
      } else if (precipitation == Precipitation.RAIN) {
         cir.setReturnValue(true);
      }
   }
}
