package daamky.ua.utils.render.animation.modernfx;

import lombok.Generated;

public class FadeAnimation {
   private final long duration;
   private long startTime;
   private boolean forwards = true;
   private double value = 0.0;
   private Easing easing = Easings.EXPO_OUT;

   public FadeAnimation(long durationMs) {
      this.duration = durationMs;
      this.startTime = System.currentTimeMillis();
   }

   public FadeAnimation(long durationMs, Easing easing) {
      this.duration = durationMs;
      this.startTime = System.currentTimeMillis();
      this.easing = easing;
   }

   public void switchDirection(boolean forwards) {
      if (this.forwards != forwards) {
         long elapsed = System.currentTimeMillis() - this.startTime;
         long remaining = this.duration - Math.min(elapsed, this.duration);
         this.startTime = System.currentTimeMillis() - remaining;
         this.forwards = forwards;
      }
   }

   public void setDirection(boolean forwards) {
      this.forwards = forwards;
   }

   public void reset() {
      this.startTime = System.currentTimeMillis();
      this.value = this.forwards ? 0.0 : 1.0;
   }

   public float get() {
      long elapsed = System.currentTimeMillis() - this.startTime;
      double progress = Math.min((double)elapsed / (double)this.duration, 1.0);
      double easedProgress = this.easing.ease(progress);
      if (this.forwards) {
         this.value = easedProgress;
      } else {
         this.value = 1.0 - easedProgress;
      }

      return (float)Math.max(0.0, Math.min(1.0, this.value));
   }

   public boolean isDone() {
      return System.currentTimeMillis() - this.startTime >= this.duration;
   }

   public boolean isFullyHidden() {
      return this.isDone() && !this.forwards;
   }

   public boolean isFullyVisible() {
      return this.isDone() && this.forwards;
   }

   @Generated
   public long getDuration() {
      return this.duration;
   }

   @Generated
   public long getStartTime() {
      return this.startTime;
   }

   @Generated
   public boolean isForwards() {
      return this.forwards;
   }

   @Generated
   public double getValue() {
      return this.value;
   }

   @Generated
   public Easing getEasing() {
      return this.easing;
   }
}
