package daamky.ua.mixin.accessor;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin({MultiPlayerGameMode.class})
public interface MultiPlayerGameModeAccessor {
   @Accessor("isDestroying")
   void daamky$setDestroying(boolean var1);

   @Accessor("isDestroying")
   boolean daamky$isDestroying();

   @Accessor("destroyBlockPos")
   BlockPos daamky$getDestroyBlockPos();

   @Invoker("ensureHasSentCarriedItem")
   void daamky$ensureHasSentCarriedItem();

   @Accessor("destroyDelay")
   void daamky$setDestroyDelay(int var1);

   @Accessor("destroyProgress")
   float daamky$getDestroyProgress();

   @Accessor("destroyProgress")
   void daamky$setDestroyProgress(float var1);
}
