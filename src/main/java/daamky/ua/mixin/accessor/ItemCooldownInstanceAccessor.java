package daamky.ua.mixin.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(
   targets = {"net/minecraft/entity/player/ItemCooldownManager$Entry"}
)
public interface ItemCooldownInstanceAccessor {
   @Accessor("endTick")
   int daamky$getEndTime();
}
