package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.api.module.impl.visual.NoRender;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;

@Mixin({Gui.class})
public abstract class GuiNoRenderMixin {
   public GuiNoRenderMixin() {
   }

   @Inject(
      method = {"renderConfusionOverlay"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$confusion(GuiGraphics graphics, float nauseaStrength, CallbackInfo ci) {
      if (NoRender.isActive("Nausea")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$scoreboard(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (NoRender.isActive("Scoreboard")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderBossOverlay"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$bossOverlay(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (NoRender.isActive("Bossbar")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderItemHotbar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$hotbar(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
      if (Hud.shouldRenderCustomHotbar()) {
         ci.cancel();
      }
   }
}
