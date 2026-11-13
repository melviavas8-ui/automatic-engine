package daamky.ua.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import daamky.ua.access.SeeInvisibleRenderStateAccessor;

@Mixin({LivingEntityRenderState.class})
public abstract class LivingEntityRenderStateMixin implements SeeInvisibleRenderStateAccessor {
   @Unique
   private float daamky$seeInvisibleAlpha = 1.0F;
   @Unique
   private boolean daamky$renderSeeInvisible;
   @Unique
   private int daamky$chamsColor;
   @Unique
   private boolean daamky$renderChams;

   public LivingEntityRenderStateMixin() {
   }

   @Override
   public void daamky$setSeeInvisibleAlpha(float alpha) {
      this.daamky$renderSeeInvisible = alpha < 0.999F;
      this.daamky$seeInvisibleAlpha = alpha;
   }

   @Override
   public float daamky$getSeeInvisibleAlpha() {
      return this.daamky$seeInvisibleAlpha;
   }

   @Override
   public boolean daamky$shouldRenderSeeInvisible() {
      return this.daamky$renderSeeInvisible;
   }

   @Override
   public void daamky$setChamsColor(int color) {
      this.daamky$chamsColor = color;
      this.daamky$renderChams = (color >>> 24 & 0xFF) > 0;
   }

   @Override
   public int daamky$getChamsColor() {
      return this.daamky$chamsColor;
   }

   @Override
   public boolean daamky$shouldRenderChams() {
      return this.daamky$renderChams;
   }
}
