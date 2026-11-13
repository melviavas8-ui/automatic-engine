package daamky.ua.screens.modernui.impl;

import java.awt.Color;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.screens.modernui.settings.SettingCardController;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class PanelBackground {
   private static final float SHELL_RADIUS = 5.0F;
   private static final float SIDEBAR_RADIUS = 5.0F;
   private static final float CONTENT_RADIUS = 5.0F;
   private static final float LOGO_BOX_RADIUS = 4.5F;
   private final SettingCardController settings = new SettingCardController();

   public PanelBackground() {
   }

   public void render(
      DrawContext graphics,
      float x,
      float y,
      float panelWidth,
      float panelHeight,
      ModuleCategory category,
      boolean searchMode,
      List<Module> modules,
      int mouseX,
      int mouseY
   ) {
      this.renderShell(x, y, panelWidth, panelHeight);
      if (category != ModuleCategory.AUTOBUY) {
         this.settings.render(graphics, x, y, panelWidth, panelHeight, category, searchMode, modules, mouseX, mouseY);
      }
   }

   public boolean mouseClicked(Click event, boolean doubled) {
      return this.settings.mouseClicked(event, doubled);
   }

   public boolean mouseReleased(Click event) {
      return this.settings.mouseReleased(event);
   }

   public boolean mouseDragged(Click event, double dragX, double dragY) {
      return this.settings.mouseDragged(event, dragX, dragY);
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
      return this.settings.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
   }

   public void focusModule(Module module) {
      this.settings.focusModule(module);
   }

   public boolean keyPressed(KeyInput event) {
      return this.settings.keyPressed(event);
   }

   public boolean charTyped(CharInput event) {
      return this.settings.charTyped(event);
   }

   private void renderShell(float x, float y, float panelWidth, float panelHeight) {
      ClientTheme theme = ThemeManager.current();
      Render2D.blur(x + 7.5F, y + 7.5F, 40.0F, panelHeight - 15.0F, 5.0F, 36.0F, 1.0F, new Color(255, 255, 255, 255).getRGB());
      Render2D.rect(
         x,
         y - 0.5F,
         panelWidth,
         panelHeight,
         5.0F,
         this.alpha(theme.surfaceDark(), 225),
         this.alpha(theme.surface(), 225),
         this.alpha(theme.surfaceDeep(), 225),
         this.alpha(theme.surfaceDark(), 225)
      );
      Render2D.rect(x, y - 0.5F, panelWidth, panelHeight, 5.0F, this.alpha(theme.surfaceDeep(), 88));
      Render2D.outline(
         x,
         y - 0.5F,
         panelWidth,
         panelHeight,
         5.0F,
         0.35F,
         this.alpha(theme.outline(), 190),
         this.alpha(theme.accent(), 135),
         this.alpha(theme.accentDark(), 120),
         this.alpha(theme.outline(), 155)
      );
      Render2D.rect(
         x + 7.5F,
         y + 7.5F,
         40.0F,
         panelHeight - 15.0F,
         5.0F,
         theme.surfaceDark().getRGB(),
         theme.surface().getRGB(),
         theme.surfaceDeep().getRGB(),
         theme.surfaceDark().getRGB()
      );
      Render2D.rect(x + 15.0F, y + 15.0F, 25.0F, 25.0F, 4.5F, theme.controlBottom().getRGB());
      Render2D.outline(
         x + 15.0F,
         y + 15.0F,
         25.0F,
         25.0F,
         4.5F,
         0.15F,
         this.alpha(theme.outline(), 155),
         this.alpha(theme.accent(), 100),
         this.alpha(theme.surfaceDeep(), 255),
         this.alpha(theme.accentDark(), 120)
      );
      float logoSize = 18.0F;
      float logoWidth = Render2D.textWidth(FontType.MAINMENUSCREEN, "f", logoSize);
      Render2D.text(
         FontType.MAINMENUSCREEN,
         "f",
         x + 15.0F + (25.0F - logoWidth) * 0.5F,
         y + 16.0F + (25.0F - logoSize) * 0.5F,
         logoSize,
         new Color(255, 255, 255, 255).getRGB()
      );
      Render2D.rect(
         x + 53.0F,
         y + 41.5F,
         panelWidth - 60.0F,
         panelHeight - 49.0F,
         5.0F,
         theme.surfaceDark().getRGB(),
         theme.surface().getRGB(),
         theme.surfaceDeep().getRGB(),
         theme.surfaceDark().getRGB()
      );
      Render2D.outline(
         x + 53.0F,
         y + 41.5F,
         panelWidth - 60.0F,
         panelHeight - 49.0F,
         5.0F,
         0.25F,
         this.alpha(theme.outline(), 120),
         this.alpha(theme.accent(), 80),
         theme.surfaceDeep().getRGB(),
         this.alpha(theme.accentDark(), 90)
      );
      Render2D.rect(
         x + 53.0F,
         y + 7.5F,
         panelWidth - 60.0F,
         30.0F,
         5.0F,
         theme.surfaceDark().getRGB(),
         theme.surfaceBright().getRGB(),
         theme.surfaceDeep().getRGB(),
         theme.surfaceDark().getRGB()
      );
      Render2D.outline(
         x + 53.0F,
         y + 7.5F,
         panelWidth - 60.0F,
         30.0F,
         5.0F,
         0.25F,
         this.alpha(theme.outline(), 130),
         this.alpha(theme.accent(), 85),
         theme.surfaceDeep().getRGB(),
         this.alpha(theme.accentDark(), 100)
      );
   }

   private int alpha(Color color, int alpha) {
      return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alpha))).getRGB();
   }
}
