package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Animations;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;

@Mixin({PlayerTabOverlay.class})
public abstract class PlayerTabOverlayMixin {
   @Unique
   private boolean daamky$posePushed;

   public PlayerTabOverlayMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void daamky$beginTabAnimation(GuiGraphics graphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      if (Animations.tabEnabled()) {
         float scale = Animations.tabScale();
         graphics.pose().pushMatrix();
         graphics.pose().translate((float)width * 0.5F, 0.0F);
         graphics.pose().scale(scale, scale);
         graphics.pose().translate((float)(-width) * 0.5F, 0.0F);
         this.daamky$posePushed = true;
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void daamky$finishTabAnimation(GuiGraphics graphics, int width, Scoreboard scoreboard, Objective objective, CallbackInfo ci) {
      if (this.daamky$posePushed) {
         graphics.pose().popMatrix();
         this.daamky$posePushed = false;
      }
   }
}
