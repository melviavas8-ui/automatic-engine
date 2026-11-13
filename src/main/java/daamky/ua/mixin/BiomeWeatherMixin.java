package daamky.ua.mixin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.Precipitation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.Ambience;

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
      method = {"getPrecipitation"},
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
      method = {"isCold"},
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
      method = {"doesNotSnow"},
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
