package daamky.ua.api.events;

public abstract class CancellableEvent implements Event, Cancellable {
   private boolean cancelled;

   public CancellableEvent() {
   }

   @Override
   public boolean isCancelled() {
      return this.cancelled;
   }

   @Override
   public void setCancelled(boolean cancelled) {
      this.cancelled = cancelled;
   }
}
