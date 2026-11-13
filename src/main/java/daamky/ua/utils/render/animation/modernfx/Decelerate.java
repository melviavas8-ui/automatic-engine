package daamky.ua.utils.render.animation.modernfx;

public class Decelerate extends Animation {
   public Decelerate() {
   }

   @Override
   public double calculation(double value) {
      double x = value / (double)this.ms;
      return 1.0 - (x - 1.0) * (x - 1.0);
   }
}
