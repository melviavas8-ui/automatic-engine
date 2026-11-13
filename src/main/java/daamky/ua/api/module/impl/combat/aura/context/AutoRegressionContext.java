package daamky.ua.api.module.impl.combat.aura.context;

import daamky.ua.api.module.impl.combat.aura.util.StopWatch;

public final class AutoRegressionContext {
   private static final AutoRegressionContext INSTANCE = new AutoRegressionContext();
   private final StopWatch attackWatch = new StopWatch();
   private long cdMinecraftMs = 83L;
   private int queuedHits;

   private AutoRegressionContext() {
   }

   public static AutoRegressionContext getInstance() {
      return INSTANCE;
   }

   public synchronized void setCdMinecraft(long cdMinecraftMs) {
      this.cdMinecraftMs = Math.max(1L, cdMinecraftMs);
   }

   public synchronized void updateLastAttackTime() {
      this.attackWatch.reset();
   }

   public synchronized void hitContentQueue() {
      if (this.queuedHits < 256) {
         this.queuedHits++;
      }
   }

   public synchronized void hitContentClear() {
      this.queuedHits = 0;
      this.attackWatch.reset();
   }

   public synchronized boolean isMinecraftCooldownPassed() {
      return this.attackWatch.finished((double)this.cdMinecraftMs);
   }

   public synchronized int getQueuedHits() {
      return this.queuedHits;
   }
}
