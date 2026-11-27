package daamky.ua.api.events.impl;

import daamky.ua.api.events.CancellableEvent;
import net.minecraft.world.InteractionHand;

public final class UsingItemEvent extends CancellableEvent {
   public static final byte ON = 0;
   public static final byte POST = 1;
   private final byte type;
   private final InteractionHand hand;

   public UsingItemEvent(byte type) {
      this(type, null);
   }

   public UsingItemEvent(byte type, InteractionHand hand) {
      this.type = type;
      this.hand = hand;
   }

   public byte getType() {
      return this.type;
   }

   public InteractionHand getHand() {
      return this.hand;
   }
}
