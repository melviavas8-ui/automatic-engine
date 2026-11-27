package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import daamky.ua.api.module.impl.movement.Speed;
import daamky.ua.api.module.impl.movement.Timer;

@Mixin({net.minecraft.client.DeltaTracker.Timer.class})
public abstract class DeltaTrackerTimerMixin {
   public DeltaTrackerTimerMixin() {
   }

   @ModifyExpressionValue(
      method = {"advanceGameTime(J)I"},
      at = {@At(
         value = "INVOKE",
         target = "Lit/unimi/dsi/fastutil/floats/FloatUnaryOperator;apply(F)F"
      )}
   )
   private float daamky$applyTimerMultiplier(float millisecondsPerTick) {
      float multiplier = Speed.hasActiveBoostCycle() ? Speed.getReallyWorldMultiplier() : Timer.getCurrentMultiplier();
      return millisecondsPerTick / multiplier;
   }
}
