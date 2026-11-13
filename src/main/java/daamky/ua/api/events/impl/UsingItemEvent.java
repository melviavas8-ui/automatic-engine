package daamky.ua.api.events.impl;

import net.minecraft.util.Hand;
import daamky.ua.api.events.CancellableEvent;

public final class UsingItemEvent extends CancellableEvent {
   public static final byte ON = 0;
   public static final byte POST = 1;
   private final byte type;
   private final Hand hand;

   public UsingItemEvent(byte type) {
      this(type, null);
   }

   public UsingItemEvent(byte type, Hand hand) {
      this.type = type;
      this.hand = hand;
   }

   public byte getType() {
      return this.type;
   }

   public Hand getHand() {
      return this.hand;
   }
}
