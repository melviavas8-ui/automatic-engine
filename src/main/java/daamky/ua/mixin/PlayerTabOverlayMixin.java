package daamky.ua.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Animations;

@Mixin({PlayerListHud.class})
public abstract class PlayerTabOverlayMixin {
   @Unique
   private boolean daamky$posePushed;

   public PlayerTabOverlayMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void daamky$beginTabAnimation(DrawContext graphics, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      if (Animations.tabEnabled()) {
         float scale = Animations.tabScale();
         graphics.getMatrices().pushMatrix();
         graphics.getMatrices().translate((float)width * 0.5F, 0.0F);
         graphics.getMatrices().scale(scale, scale);
         graphics.getMatrices().translate((float)(-width) * 0.5F, 0.0F);
         this.daamky$posePushed = true;
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void daamky$finishTabAnimation(DrawContext graphics, int width, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci) {
      if (this.daamky$posePushed) {
         graphics.getMatrices().popMatrix();
         this.daamky$posePushed = false;
      }
   }
}
