package daamky.ua.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.api.module.impl.visual.NoRender;

@Mixin({InGameHud.class})
public abstract class GuiNoRenderMixin {
   public GuiNoRenderMixin() {
   }

   @Inject(
      method = {"renderNauseaOverlay"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$confusion(DrawContext graphics, float nauseaStrength, CallbackInfo ci) {
      if (NoRender.isActive("Nausea")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$scoreboard(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
      if (NoRender.isActive("Scoreboard")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderBossBarHud"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$bossOverlay(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
      if (NoRender.isActive("Bossbar")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"renderHotbar"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$hotbar(DrawContext graphics, RenderTickCounter deltaTracker, CallbackInfo ci) {
      if (Hud.shouldRenderCustomHotbar()) {
         ci.cancel();
      }
   }
}
