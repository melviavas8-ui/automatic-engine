package daamky.ua.utils.render.animation.modernfx;

public class OutBack extends Animation {
   public OutBack() {
   }

   @Override
   public double calculation(double value) {
      double x = value / (double)this.ms;
      double c1 = 1.70158;
      double c3 = c1 + 1.0;
      return 1.0 + c3 * Math.pow(x - 1.0, 3.0) + c1 * Math.pow(x - 1.0, 2.0);
   }
}
