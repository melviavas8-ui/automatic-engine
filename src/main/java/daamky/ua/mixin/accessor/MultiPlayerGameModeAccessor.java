package daamky.ua.mixin.accessor;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({ClientPlayerInteractionManager.class})
public interface MultiPlayerGameModeAccessor {
   @Accessor("breakingBlock")
   void daamky$setDestroying(boolean var1);

   @Accessor("breakingBlock")
   boolean daamky$isDestroying();

   @Accessor("currentBreakingPos")
   BlockPos daamky$getDestroyBlockPos();

   @Invoker("syncSelectedSlot")
   void daamky$ensureHasSentCarriedItem();

   @Accessor("blockBreakingCooldown")
   void daamky$setDestroyDelay(int var1);

   @Accessor("currentBreakingProgress")
   float daamky$getDestroyProgress();

   @Accessor("currentBreakingProgress")
   void daamky$setDestroyProgress(float var1);
}
