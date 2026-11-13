package daamky.ua.api.events.types;

public enum EventPriority {
   HIGHEST(0),
   HIGH(100),
   NORMAL(200),
   LOW(300),
   LOWEST(400),
   MONITOR(500);

   private final int order;

   private EventPriority(int order) {
      this.order = order;
   }

   public int getOrder() {
      return this.order;
   }
}
