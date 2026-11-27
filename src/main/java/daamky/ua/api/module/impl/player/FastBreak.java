package daamky.ua.api.module.impl.player;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.BlockBreakingEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.mixin.accessor.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class FastBreak extends Module {
   private final NumberSetting multiplier = this.register(new NumberSetting("Multiplier", "Block breaking speed multiplier.", 1.5, 1.0, 3.0, 0.05));

   public FastBreak() {
      super("Fast Break", "Speeds up block breaking.", ModuleCategory.PLAYER);
   }

   @SubscribeEvent
   private void onBlockBreaking(BlockBreakingEvent event) {
      Minecraft client = Minecraft.getInstance();
      if (client.gameMode != null && client.player != null && client.level != null) {
         float multiplierValue = this.multiplier.getFloat();
         if (!(multiplierValue <= 1.0F)) {
            BlockPos blockPos = event.blockPos();
            BlockState state = client.level.getBlockState(blockPos);
            if (!state.isAir()) {
               MultiPlayerGameModeAccessor controller = (MultiPlayerGameModeAccessor)client.gameMode;
               controller.daamky$setDestroyDelay(0);
               float delta = state.getDestroyProgress(client.player, client.player.level(), blockPos);
               float extraProgress = delta * (multiplierValue - 1.0F);
               float breakingProgress = controller.daamky$getDestroyProgress() + extraProgress;
               controller.daamky$setDestroyProgress(Math.min(1.0F, breakingProgress));
            }
         }
      }
   }
}
