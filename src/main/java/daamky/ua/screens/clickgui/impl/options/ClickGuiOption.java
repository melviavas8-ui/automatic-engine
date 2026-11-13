package daamky.ua.screens.clickgui.impl.options;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.Setting;
import daamky.ua.screens.clickgui.impl.ClickGuiController;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public abstract class ClickGuiOption {
   protected static final float HEIGHT = 22.0F;
   private final String name;
   protected float x;
   protected float y;
   protected float width;
   protected float height;
   protected float scale;
   protected float alpha;
   protected float animationAlpha;
   private final SmoothAnimation visibilityAnimation = new SmoothAnimation();
   private final Map<String, SmoothAnimation> textScrollAnimations = new HashMap<>();
   private BooleanSupplier visibleSupplier = () -> true;
   private float fadeMultiplier = 1.0F;
   private boolean interactiveThisFrame;

   protected ClickGuiOption(String name) {
      this.name = name;
      this.visibilityAnimation.set(1.0);
   }

   public float getHeight() {
      return 22.0F;
   }

   public final void render(DrawContext graphics, float x, float y, float width, float scale, float alpha) {
      float sw = width * scale;
      float sh = 22.0F * scale;
      this.x = x;
      this.y = y;
      this.width = sw;
      this.height = sh;
      this.scale = scale;
      this.animationAlpha = alpha;
      this.alpha = alpha * this.fadeMultiplier;
      this.interactiveThisFrame = this.alpha > 0.01F && sw > 0.0F && sh > 0.0F;
      this.renderControl(graphics, x, y, sw, sh, scale, this.alpha);
   }

   public void resetInteractionFrame() {
      this.interactiveThisFrame = false;
   }

   public void setSetting(Setting<?> setting) {
      if (setting == null) {
         this.setVisibleSupplier(null);
      } else {
         this.setVisibleSupplier(setting::isVisible);
      }
   }

   public void setVisibleSupplier(BooleanSupplier visibleSupplier) {
      this.visibleSupplier = visibleSupplier == null ? () -> true : visibleSupplier;
      this.visibilityAnimation.set(this.visibleSupplier.getAsBoolean() ? 1.0 : 0.0);
   }

   public void updateVisibility() {
      boolean visible = this.visibleSupplier.getAsBoolean();
      this.visibilityAnimation.run(visible ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, true);
      this.visibilityAnimation.update();
      if (!visible) {
         this.closePopup();
      }
   }

   public float visibilityProgress() {
      return this.clamp(this.visibilityAnimation.get(), 0.0F, 1.0F);
   }

   public boolean isVisibleForRender() {
      return this.visibilityProgress() > 0.01F;
   }

   public boolean isVisibleForInteraction() {
      return this.interactiveThisFrame && this.visibleSupplier.getAsBoolean() && this.visibilityProgress() > 0.85F;
   }

   public void renderOverlay(DrawContext graphics) {
   }

   public void setFadeMultiplier(float fadeMultiplier) {
      this.fadeMultiplier = this.clamp(fadeMultiplier, 0.0F, 1.0F);
   }

   public boolean mouseClicked(Click event, boolean doubled) {
      return false;
   }

   public boolean wantsMousePriority(Click event) {
      return false;
   }

   public boolean hasOpenPopup() {
      return false;
   }

   public boolean mouseReleased(Click event) {
      return false;
   }

   public boolean mouseDragged(Click event, double dragX, double dragY) {
      return false;
   }

   public boolean keyPressed(KeyInput event) {
      return false;
   }

   public boolean charTyped(CharInput event) {
      return false;
   }

   public void closePopup() {
   }

   protected abstract void renderControl(DrawContext var1, float var2, float var3, float var4, float var5, float var6, float var7);

   protected String name() {
      return ClientLanguage.tr(this.name);
   }

   protected boolean hovered(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   protected float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   protected int mixColor(int fromRed, int fromGreen, int fromBlue, int toRed, int toGreen, int toBlue, float progress, float alphaMultiplier) {
      float clamped = this.clamp(progress, 0.0F, 1.0F);
      int red = Math.round((float)fromRed + (float)(toRed - fromRed) * clamped);
      int green = Math.round((float)fromGreen + (float)(toGreen - fromGreen) * clamped);
      int blue = Math.round((float)fromBlue + (float)(toBlue - fromBlue) * clamped);
      return this.color(red, green, blue, 255, alphaMultiplier);
   }

   protected float textWidth(String text, float size) {
      return Render2D.textWidth(FontType.SEMIBOLD, text, size);
   }

   protected int labelColor() {
      float visibleAlpha = this.alpha >= 0.98F ? 1.0F : this.alpha;
      return ColorUtil.rgba(255, 255, 255, Math.round(255.0F * visibleAlpha));
   }

   protected int iconColor() {
      float visibleAlpha = this.alpha >= 0.98F ? 1.0F : this.alpha;
      return ColorUtil.rgba(175, 175, 175, Math.round(255.0F * visibleAlpha));
   }

   protected void renderGuiIcon(String icon, float x, float y, float size, float scale) {
      Render2D.text(FontType.GUI_ICONS, icon, x, y, size * scale, this.iconColor());
   }

   protected void renderOptionLabel(DrawContext graphics, String key, float textX, float textY, float textSize, float clipRight, float rowY, float rowHeight) {
      float clipWidth = Math.max(0.0F, clipRight - textX);
      this.renderScrollingText(
         graphics, "label:" + key, this.name(), textX, textY, textSize, this.labelColor(), textX, rowY, clipWidth, rowHeight, textX, rowY, clipWidth, rowHeight
      );
   }

   protected void renderScrollingText(
      DrawContext graphics,
      String key,
      String text,
      float textX,
      float textY,
      float textSize,
      int textColor,
      float clipX,
      float clipY,
      float clipWidth,
      float clipHeight,
      float hoverX,
      float hoverY,
      float hoverWidth,
      float hoverHeight
   ) {
      if (text != null && !text.isEmpty() && !(clipWidth <= 0.25F) && !(clipHeight <= 0.25F)) {
         float textWidth = this.textWidth(text, textSize);
         float overflow = Math.max(0.0F, textWidth - clipWidth + 3.0F * this.scale);
         SmoothAnimation scrollAnimation = this.textScrollAnimations.computeIfAbsent(key, ignored -> {
            SmoothAnimation animation = new SmoothAnimation();
            animation.set(0.0);
            return animation;
         });
         boolean hover = overflow > 0.25F && this.hovered(ClickGuiController.mouseX(), ClickGuiController.mouseY(), hoverX, hoverY, hoverWidth, hoverHeight);
         float target = 0.0F;
         if (hover) {
            long segment = Math.max(950L, 1200L + (long)Math.round(overflow * 18.0F));
            target = (System.currentTimeMillis() / segment & 1L) == 0L ? -overflow : 0.0F;
         }

         scrollAnimation.run((double)target, hover ? 0.55 : 0.22, Easings.CUBIC_OUT, true);
         scrollAnimation.update();
         float scroll = scrollAnimation.get();
         float fadeWidth = Math.min(10.0F * this.scale, Math.max(2.0F, clipWidth * 0.35F));
         float leftFade = overflow > 0.25F && scroll < -0.4F ? 0.78F : 0.0F;
         float rightFade = overflow > 0.25F && scroll > -overflow + 0.4F ? 0.88F : 0.0F;
         Render2D.pushScissor(graphics, clipX, clipY, clipWidth, clipHeight);
         if (!(leftFade > 0.0F) && !(rightFade > 0.0F)) {
            Render2D.text(FontType.SEMIBOLD, text, textX + scroll, textY, textSize, textColor);
         } else {
            Render2D.textFade(FontType.SEMIBOLD, text, textX + scroll, textY, textSize, textColor, clipX, clipX + clipWidth, fadeWidth, leftFade, rightFade);
         }

         Render2D.popScissor(graphics);
      }
   }

   protected void renderGlassControl(float x, float y, float width, float height, float radius, float alphaMultiplier) {
      Render2D.glass(
         x,
         y,
         width,
         height,
         radius,
         this.color(255, 255, 255, 72, alphaMultiplier),
         0.64F * alphaMultiplier,
         42.0F,
         this.color(255, 255, 255, 150, alphaMultiplier),
         0.16F * alphaMultiplier,
         true,
         0.22F,
         0.003F,
         1.0F,
         0.0F
      );
      Render2D.glassOutline(
         x,
         y,
         width,
         height,
         radius,
         0.5F,
         this.color(255, 255, 255, 176, alphaMultiplier),
         1.0F,
         35.0F,
         this.color(168, 168, 174, 118, alphaMultiplier),
         0.42F * alphaMultiplier,
         false,
         0.45F,
         0.0015F,
         1.0F,
         0.0F
      );
   }

   protected int color(int red, int green, int blue, int alpha, float alphaMultiplier) {
      return ColorUtil.rgba(red, green, blue, (int)((float)alpha * alphaMultiplier));
   }
}
