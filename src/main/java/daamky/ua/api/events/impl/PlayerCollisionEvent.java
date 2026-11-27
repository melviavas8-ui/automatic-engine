package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.level.block.Block;

public final class PlayerCollisionEvent extends CancellableEvent {
   private final Block block;

   public PlayerCollisionEvent(Block block) {
      this.block = block;
   }

   public Block getBlock() {
      return this.block;
   }
}
