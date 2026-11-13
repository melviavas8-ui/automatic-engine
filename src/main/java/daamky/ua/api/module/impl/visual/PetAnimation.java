package daamky.ua.api.module.impl.visual;

public final class PetAnimation {
   private float current = 0.0F;
   private float target = 0.0F;
   private float origin = 0.0F;
   private long startMs = 0L;
   private int durationMs = 1;

   public PetAnimation() {
   }

   public float animate(float destination, int ms) {
      if (destination == this.target) {
         return this.get();
      } else {
         this.origin = this.get();
         this.target = destination;
         this.durationMs = Math.max(1, ms);
         this.startMs = System.currentTimeMillis();
         return this.get();
      }
   }

   public float get() {
      long elapsed = System.currentTimeMillis() - this.startMs;
      if (elapsed >= (long)this.durationMs) {
         this.current = this.target;
         return this.current;
      } else {
         float t = (float)elapsed / (float)this.durationMs;
         t = t * t * (3.0F - 2.0F * t);
         this.current = this.origin + (this.target - this.origin) * t;
         return this.current;
      }
   }

   public boolean finished() {
      return System.currentTimeMillis() - this.startMs >= (long)this.durationMs;
   }
}
