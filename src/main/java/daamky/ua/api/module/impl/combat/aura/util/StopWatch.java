package daamky.ua.api.module.impl.combat.aura.util;

public final class StopWatch {
   private long last = System.currentTimeMillis();

   public StopWatch() {
   }

   public void reset() {
      this.last = System.currentTimeMillis();
   }

   public long elapsedTime() {
      return System.currentTimeMillis() - this.last;
   }

   public boolean finished(double delayMs) {
      return (double)this.elapsedTime() >= delayMs;
   }

   public boolean every(double delayMs) {
      if (!this.finished(delayMs)) {
         return false;
      } else {
         this.reset();
         return true;
      }
   }
}
