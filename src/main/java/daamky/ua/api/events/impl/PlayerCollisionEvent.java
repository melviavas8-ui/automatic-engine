package daamky.ua.api.events.impl;

import net.minecraft.block.Block;
import daamky.ua.api.events.CancellableEvent;

public final class PlayerCollisionEvent extends CancellableEvent {
   private final Block block;

   public PlayerCollisionEvent(Block block) {
      this.block = block;
   }

   public Block getBlock() {
      return this.block;
   }
}
