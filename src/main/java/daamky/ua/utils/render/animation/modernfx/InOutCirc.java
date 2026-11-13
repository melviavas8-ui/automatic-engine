package daamky.ua.utils.render.animation.modernfx;

public class InOutCirc extends Animation {
   public InOutCirc() {
   }

   @Override
   public double calculation(double value) {
      double x = value / (double)this.ms;
      return x < 0.5 ? (1.0 - Math.sqrt(1.0 - Math.pow(2.0 * x, 2.0))) / 2.0 : (Math.sqrt(1.0 - Math.pow(-2.0 * x + 2.0, 2.0)) + 1.0) / 2.0;
   }
}
