package daamky.ua.screens.modernui.settings;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.drag.impl.ClientLanguage;
import daamky.ua.api.settings.Setting;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

abstract class SettingCardComponent<T extends Setting<?>> {
   protected final T setting;
   protected final SettingCardState state;

   SettingCardComponent(T setting) {
      this.setting = setting;
      this.state = new SettingCardState(setting);
   }

   T setting() {
      return this.setting;
   }

   SettingCardState state() {
      return this.state;
   }

   boolean visible() {
      return this.setting.isVisible();
   }

   void hide() {
      this.state.visible = false;
   }

   void updateVisibility() {
      this.state.visibilityAnimation.setDirection(this.setting.isVisible() ? Direction.FORWARDS : Direction.BACKWARDS);
      this.state.visibilityProgress = Math.max(0.0F, Math.min(1.0F, this.state.visibilityAnimation.getOutput().floatValue()));
   }

   boolean alive() {
      return this.setting.isVisible() || this.state.visibilityProgress > 0.01F;
   }

   abstract int order();

   abstract float height(SettingCardContext var1);

   float layoutHeight(SettingCardContext context) {
      return this.height(context);
   }

   float animatedHeight(SettingCardContext context) {
      return this.height(context) * this.state.visibilityProgress;
   }

   float animatedLayoutHeight(SettingCardContext context) {
      return this.layoutHeight(context) * this.state.visibilityProgress;
   }

   void renderAnimated(DrawContext graphics, SettingCardContext context, float cardX, float y, int mouseX, int mouseY) {
      float progress = this.state.visibilityProgress;
      if (!(progress <= 0.01F)) {
         float height = this.height(context);
         float visibleHeight = Math.max(0.0F, height * progress);
         float offsetY = (1.0F - progress) * -5.0F;
         Render2D.pushScissor(graphics, cardX, y - 4.0F, context.cardWidth, visibleHeight + 8.0F);
         float previousAlpha = context.visualAlpha();
         context.setVisualAlpha(previousAlpha * progress);
         this.state.hoverDescriptionEnabled = false;
         this.render(graphics, context, cardX, y + offsetY, mouseX, mouseY);
         context.setVisualAlpha(previousAlpha);
         Render2D.popScissor(graphics);
         this.state.visible = this.setting.isVisible() && progress > 0.55F;
      }
   }

   abstract void render(DrawContext var1, SettingCardContext var2, float var3, float var4, int var5, int var6);

   void renderOverlay(DrawContext graphics, SettingCardContext context) {
   }

   void renderFloatingOverlay(DrawContext graphics, SettingCardContext context) {
   }

   boolean mouseClickedOverlay(Click event, SettingCardContext context) {
      return false;
   }

   boolean mouseClicked(Click event, boolean doubled, SettingCardContext context) {
      return false;
   }

   boolean mouseReleased(Click event, SettingCardContext context) {
      return false;
   }

   boolean mouseDragged(Click event, double dragX, double dragY, SettingCardContext context) {
      return false;
   }

   boolean mouseScrolled(double mouseX, double mouseY, double scrollY, SettingCardContext context) {
      return false;
   }

   boolean keyPressed(KeyInput event, SettingCardContext context) {
      return false;
   }

   boolean charTyped(CharInput event, SettingCardContext context) {
      return false;
   }

   protected void markRow(float cardX, float y, float height) {
      this.state.row(cardX + 8.0F, y, 132.0F, height);
   }

   protected boolean hasDescription() {
      return !ClientLanguage.trDescription(this.setting.getDescription()).isBlank();
   }

   protected List<String> descriptionLines(SettingCardContext context, float width) {
      return this.hasDescription()
         ? context.wrapText(FontType.SEMIBOLD, ClientLanguage.trDescription(this.setting.getDescription()), 5.0F, width, 2)
         : List.of();
   }

   protected float descriptionOffset(SettingCardContext context, float width) {
      return this.descriptionOffset(this.descriptionLines(context, width).size());
   }

   protected float descriptionOffset(int lineCount) {
      return lineCount <= 0 ? 0.0F : 7.0F + (float)(lineCount - 1) * 5.5F;
   }

   protected void renderNameAndDescription(DrawContext graphics, SettingCardContext context, float x, float y, float titleWidth, float descriptionWidth) {
      context.renderClippedText(
         graphics, FontType.SEMIBOLD, ClientLanguage.tr(this.setting.getName()), x, y + 2.5F, titleWidth, 6.0F, new Color(225, 225, 225, 255).getRGB()
      );
      this.state.hoverDescriptionEnabled = false;
   }

   private void renderHoverDescription(DrawContext graphics, SettingCardContext context) {
      String description = ClientLanguage.trDescription(this.setting.getDescription());
      boolean hasDescription = this.state.hoverDescriptionEnabled && description != null && !description.isBlank();
      boolean hovered = hasDescription
         && this.state.visible
         && this.state.visibilityProgress > 0.55F
         && context.inside(context.mouseX, context.mouseY, this.state.rowX, this.state.rowY, this.state.rowWidth, this.state.rowHeight);
      if (hovered && (!description.equals(this.state.hoverDescriptionText) || this.state.descriptionAnimation.getDirection() != Direction.FORWARDS)) {
         this.state.hoverDescriptionText = description;
         this.state.hoverDescriptionStartMs = System.currentTimeMillis();
      }

      this.state.descriptionAnimation.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
      float progress = context.clamp(this.state.descriptionAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      if (!(progress <= 0.01F) && !this.state.hoverDescriptionText.isBlank()) {
         int typedCharacters = this.typedCharacters(this.state.hoverDescriptionText);
         String visibleText = this.state.hoverDescriptionText.substring(0, Math.min(typedCharacters, this.state.hoverDescriptionText.length()));
         if (hovered && typedCharacters < this.state.hoverDescriptionText.length()) {
            visibleText = visibleText + "_";
         }

         float tooltipWidth = Math.min(132.0F, Math.max(72.0F, this.state.rowWidth));
         List<String> lines = context.wrapText(FontType.SEMIBOLD, visibleText, 5.0F, tooltipWidth - 10.0F, 3);
         if (!lines.isEmpty()) {
            float lineHeight = 5.5F;
            float tooltipHeight = 8.0F + (float)lines.size() * lineHeight;
            float tooltipX = this.state.rowX;
            float tooltipY = this.state.rowY + this.state.rowHeight + 2.0F;
            if (tooltipY + tooltipHeight > context.moduleClipY + context.moduleClipHeight) {
               tooltipY = this.state.rowY - tooltipHeight - 2.0F;
            }

            tooltipY = context.clamp(tooltipY, context.moduleClipY + 2.0F, context.moduleClipY + context.moduleClipHeight - tooltipHeight - 2.0F);
            int backgroundAlpha = Math.round(205.0F * progress);
            int outlineAlpha = Math.round(56.0F * progress);
            Render2D.rect(tooltipX, tooltipY, tooltipWidth, tooltipHeight, 4.0F, context.color(10, 10, 12, backgroundAlpha));
            Render2D.outline(
               tooltipX,
               tooltipY,
               tooltipWidth,
               tooltipHeight,
               4.0F,
               0.25F,
               context.color(255, 255, 255, outlineAlpha),
               context.color(255, 255, 255, outlineAlpha),
               context.color(255, 255, 255, outlineAlpha),
               context.color(255, 255, 255, outlineAlpha)
            );
         }
      }
   }

   private int typedCharacters(String text) {
      if (text != null && !text.isEmpty()) {
         long elapsed = Math.max(0L, System.currentTimeMillis() - this.state.hoverDescriptionStartMs);
         int typed = (int)(elapsed / 18L);
         return Math.min(text.length(), typed);
      } else {
         return 0;
      }
   }
}
