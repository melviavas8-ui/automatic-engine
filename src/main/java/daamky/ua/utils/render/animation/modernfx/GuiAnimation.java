package daamky.ua.utils.render.animation.modernfx;

import lombok.Generated;
import daamky.ua.utils.timer.TimerUtil;

public class GuiAnimation {
   public final TimerUtil counter = new TimerUtil();
   protected int ms = 250;
   protected double value = 1.0;
   protected Direction direction = Direction.FORWARDS;

   public GuiAnimation() {
   }

   public GuiAnimation reset() {
      this.counter.resetCounter();
      return this;
   }

   public boolean isDone() {
      return this.counter.isReached((long)this.ms);
   }

   public boolean isFinished(Direction direction) {
      return this.direction == direction && this.isDone();
   }

   public Direction getDirection() {
      return this.direction;
   }

   public GuiAnimation setDirection(Direction direction) {
      if (this.direction != direction) {
         this.direction = direction;
      }

      return this;
   }

   public Double getOutput() {
      double progress = Math.min(1.0, (double)this.counter.getTime() / (double)this.ms);
      double eased = this.easeOutQuart(progress);
      return this.direction == Direction.FORWARDS ? eased * this.value : (1.0 - eased) * this.value;
   }

   private double easeOutQuart(double x) {
      return 1.0 - Math.pow(1.0 - x, 4.0);
   }

   @Generated
   public GuiAnimation setMs(int ms) {
      this.ms = ms;
      return this;
   }

   @Generated
   public GuiAnimation setValue(double value) {
      this.value = value;
      return this;
   }
}
