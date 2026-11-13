package daamky.ua.utils.render.animation.modernfx;

import lombok.Generated;

public class EasyBackIn extends Animation {
   private float easeAmount = 1.25F;

   public EasyBackIn() {
   }

   public EasyBackIn(int ms, double value, float easeAmount) {
      this.ms = ms;
      this.value = value;
      this.easeAmount = easeAmount;
   }

   @Override
   public double calculation(double value) {
      double x = value / (double)this.ms;
      float shrink = this.easeAmount + 1.0F;
      return Math.max(0.0, 1.0 + (double)shrink * Math.pow(x - 1.0, 3.0) + (double)this.easeAmount * Math.pow(x - 1.0, 2.0));
   }

   @Generated
   public float getEaseAmount() {
      return this.easeAmount;
   }

   @Generated
   public EasyBackIn setEaseAmount(float easeAmount) {
      this.easeAmount = easeAmount;
      return this;
   }
}
