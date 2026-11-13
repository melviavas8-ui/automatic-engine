package daamky.ua.utils.render.animation.modernfx;

public class EaseInOutQuad extends Animation {
   public EaseInOutQuad() {
   }

   @Override
   public double calculation(double value) {
      double x = value / (double)this.ms;
      return x < 0.5 ? 2.0 * x * x : 1.0 - Math.pow(-2.0 * x + 2.0, 2.0) / 2.0;
   }
}
