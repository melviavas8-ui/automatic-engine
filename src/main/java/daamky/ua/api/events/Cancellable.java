package daamky.ua.api.events;

public interface Cancellable {
   boolean isCancelled();

   void setCancelled(boolean var1);

   default void cancel() {
      this.setCancelled(true);
   }
}
