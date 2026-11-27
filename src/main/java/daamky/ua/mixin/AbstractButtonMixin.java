package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.Animations;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;

@Mixin({AbstractButton.class})
public abstract class AbstractButtonMixin {
   @Unique
   private float daamky$hover;
   @Unique
   private long daamky$lastFrame;
   @Unique
   private boolean daamky$animationPosePushed;

   public AbstractButtonMixin() {
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$renderThemedButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (VanillaMenuTheme.active()) {
         AbstractButton button = (AbstractButton)(Object)this;
         if (button instanceof Button || button instanceof CycleButton) {
            long now = System.nanoTime();
            float delta = this.daamky$lastFrame == 0L ? 0.0F : Math.min(0.05F, (float)(now - this.daamky$lastFrame) / 1.0E9F);
            this.daamky$lastFrame = now;
            float target = button.isHoveredOrFocused() && button.active ? 1.0F : 0.0F;
            float rate = Animations.buttonsEnabled() ? Animations.buttonRate() : 7.5F;
            this.daamky$hover = approach(this.daamky$hover, target, delta * rate);
            if (Animations.buttonsEnabled()) {
               float scale = 1.0F + 0.025F * this.daamky$hover;
               float centerX = (float)button.getX() + (float)button.getWidth() * 0.5F;
               float centerY = (float)button.getY() + (float)button.getHeight() * 0.5F;
               graphics.pose().pushMatrix();
               graphics.pose().translate(centerX, centerY);
               graphics.pose().scale(scale, scale);
               graphics.pose().translate(-centerX, -centerY);
               this.daamky$animationPosePushed = true;
            }

            VanillaMenuTheme.renderButton(graphics, button, this.daamky$hover);
            this.daamky$popAnimationPose(graphics);
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("HEAD")}
   )
   private void daamky$beginButtonAnimation(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      if (!VanillaMenuTheme.active() && Animations.buttonsEnabled()) {
         AbstractButton button = (AbstractButton)(Object)this;
         long now = System.nanoTime();
         float delta = this.daamky$lastFrame == 0L ? 0.0F : Math.min(0.05F, (float)(now - this.daamky$lastFrame) / 1.0E9F);
         this.daamky$lastFrame = now;
         float target = button.isHoveredOrFocused() && button.active ? 1.0F : 0.0F;
         this.daamky$hover = approach(this.daamky$hover, target, delta * Animations.buttonRate());
         float scale = 1.0F + 0.025F * this.daamky$hover;
         float centerX = (float)button.getX() + (float)button.getWidth() * 0.5F;
         float centerY = (float)button.getY() + (float)button.getHeight() * 0.5F;
         graphics.pose().pushMatrix();
         graphics.pose().translate(centerX, centerY);
         graphics.pose().scale(scale, scale);
         graphics.pose().translate(-centerX, -centerY);
         this.daamky$animationPosePushed = true;
      }
   }

   @Inject(
      method = {"renderWidget"},
      at = {@At("RETURN")}
   )
   private void daamky$finishButtonAnimation(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
      this.daamky$popAnimationPose(graphics);
   }

   @Unique
   private void daamky$popAnimationPose(GuiGraphics graphics) {
      if (this.daamky$animationPosePushed) {
         graphics.pose().popMatrix();
         this.daamky$animationPosePushed = false;
      }
   }

   @Unique
   private static float approach(float current, float target, float amount) {
      return current < target ? Math.min(target, current + amount) : Math.max(target, current - amount);
   }
}

