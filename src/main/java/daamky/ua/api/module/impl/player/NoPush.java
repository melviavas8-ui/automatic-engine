package daamky.ua.api.module.impl.player;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PlayerCollisionEvent;
import daamky.ua.api.events.impl.PushEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.MultiModeSetting;

public final class NoPush extends Module {
   private final MultiModeSetting ignoreSetting = this.register(
      new MultiModeSetting(
         "Ignore",
         "Push sources to ignore.",
         new String[]{"Water", "Blocks", "Entity Collision", "Powder Snow", "Berries"},
         "Water",
         "Blocks",
         "Entity Collision"
      )
   );

   public NoPush() {
      super("No Push", "Disables selected push interactions.", ModuleCategory.PLAYER);
   }

   @SubscribeEvent
   private void onPush(PushEvent event) {
      switch (event.getType()) {
         case COLLISION:
            event.setCancelled(this.ignoreSetting.isSelected("Entity Collision"));
            break;
         case WATER:
            event.setCancelled(this.ignoreSetting.isSelected("Water"));
            break;
         case BLOCK:
            event.setCancelled(this.ignoreSetting.isSelected("Blocks"));
      }
   }

   @SubscribeEvent
   private void onPlayerCollision(PlayerCollisionEvent event) {
      Block block = event.getBlock();
      if (block == Blocks.POWDER_SNOW) {
         event.setCancelled(this.ignoreSetting.isSelected("Powder Snow"));
      } else if (block == Blocks.SWEET_BERRY_BUSH) {
         event.setCancelled(this.ignoreSetting.isSelected("Berries"));
      }
   }
}
