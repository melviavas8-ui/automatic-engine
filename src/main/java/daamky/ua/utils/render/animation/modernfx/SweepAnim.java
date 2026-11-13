package daamky.ua.utils.render.animation.modernfx;

public class SweepAnim {
   private float progress = 0.0F;
   private final float duration;
   private boolean completed;
   private boolean active;
   private long startTime;

   public SweepAnim(float durationSeconds) {
      this.duration = durationSeconds * 1000.0F;
      this.completed = false;
      this.active = false;
      this.startTime = 0L;
   }

   public void start() {
      if (!this.active && !this.completed) {
         this.progress = 0.0F;
         this.completed = false;
         this.active = true;
         this.startTime = System.currentTimeMillis();
      }
   }

   public void reset() {
      this.progress = 0.0F;
      this.completed = false;
      this.active = false;
      this.startTime = 0L;
   }

   public void update() {
      if (this.active) {
         long elapsed = System.currentTimeMillis() - this.startTime;
         this.progress = Math.min((float)elapsed / this.duration, 1.0F);
         if (this.progress >= 1.0F) {
            this.progress = 1.0F;
            this.completed = true;
            this.active = false;
         }
      }
   }

   public float getProgress() {
      return this.progress;
   }

   public boolean isCompleted() {
      return this.completed;
   }

   public boolean isActive() {
      return this.active;
   }

   public boolean isFinished() {
      return this.completed && !this.active;
   }
}
