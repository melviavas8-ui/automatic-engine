package daamky.ua.api.drag.impl;

import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class WatermarkIcons extends HudPanel {
   private static final WatermarkIcons.IconSection[] SECTIONS = new WatermarkIcons.IconSection[]{
      new WatermarkIcons.IconSection("Main menu / mainmenuicons", FontType.MAINMENUSCREEN, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0"),
      new WatermarkIcons.IconSection("Watermark / watermarkicons", FontType.WATERMARKICONS, "abcdefghijklmnopqrstuvwxyzAB"),
      new WatermarkIcons.IconSection("GUI / guiicons", FontType.GUI_ICONS, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
      new WatermarkIcons.IconSection("HUD / hudicons", FontType.HUD_ICONS, " abcdefghijklmnopqrstuvwx"),
      new WatermarkIcons.IconSection("Category / categoryicons", FontType.CATEGORY_ICONS, "abcdefgjlmnuv"),
      new WatermarkIcons.IconSection("Icons / icons", FontType.ICONS, "ABCDEFGHIJKLMNOPQRSTUVWX"),
      new WatermarkIcons.IconSection("TypeTho / iconstypetho", FontType.ICONSTYPETHO, "abcdefghijklmnopqrstuvwxyz"),
      new WatermarkIcons.IconSection(
         "Media / icons3", FontType.MEDIA_PLAYER_ICONS, "JBETCQGRSWDNHV!\"#$%&'()*+,-./0123456789:;<=>?@FIYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~AKMLXOPU"
      ),
      new WatermarkIcons.IconSection("Swords / swords", FontType.SWORDS, "s"),
      new WatermarkIcons.IconSection("Speed / speed", FontType.SPEED, "s"),
      new WatermarkIcons.IconSection("Glass / glas", FontType.GLAS, "g"),
      new WatermarkIcons.IconSection("Misc / misc_icon", FontType.MISC_ICON, "m")
   };
   private static final int COLUMNS = 14;
   private static final float CELL_WIDTH = 27.0F;
   private static final float CELL_HEIGHT = 34.0F;
   private static final float SECTION_HEADER_HEIGHT = 17.0F;
   private static final float SECTION_GAP = 7.0F;

   public WatermarkIcons() {
      super("watermarkicons", "Watermark Icons", 300.0F, 180.0F, 390.0F, 900.0F);
   }

   @Override
   public void render() {
      WatermarkIcons.WatermarkIconsState state = this.logics();
      this.renderWatermarkIcons(state);
   }

   private WatermarkIcons.WatermarkIconsState logics() {
      this.contentVisible(this.selected());
      int rows = 0;

      for (WatermarkIcons.IconSection section : SECTIONS) {
         rows += rows(section.icons());
      }

      float width = 390.0F;
      float height = 14.0F + (float)rows * 34.0F + (float)SECTIONS.length * 24.0F;
      this.size(width, height);
      return new WatermarkIcons.WatermarkIconsState(this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height());
   }

   private void renderWatermarkIcons(WatermarkIcons.WatermarkIconsState state) {
      HudRenderCompat.background(state.x, state.y, state.width, state.height, 4.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, 255));
      float sectionY = state.y + 8.0F;

      for (WatermarkIcons.IconSection section : SECTIONS) {
         this.renderSectionHeader(state.x + 8.0F, sectionY, section);
         sectionY += 17.0F;

         for (int i = 0; i < section.icons().length(); i++) {
            String icon = String.valueOf(section.icons().charAt(i));
            int column = i % 14;
            int row = i / 14;
            float cellX = state.x + 8.0F + (float)column * 27.0F;
            float cellY = sectionY + (float)row * 34.0F;
            this.renderIconCell(section.font(), icon, cellX, cellY);
         }

         sectionY += (float)rows(section.icons()) * 34.0F + 7.0F;
      }
   }

   private void renderSectionHeader(float x, float y, WatermarkIcons.IconSection section) {
      Render2D.text(TEXT_FONT, section.title(), x, y + 2.5F, 6.0F, TEXT_COLOR);
      Render2D.text(TEXT_FONT, String.valueOf(section.icons().length()), x + 315.0F, y + 2.5F, 6.0F, MUTED_COLOR);
      Render2D.rect(x, y + 14.0F, 365.0F, 0.5F, 0.0F, ColorUtil.rgba(255, 255, 255, 26));
   }

   private void renderIconCell(FontType font, String icon, float cellX, float cellY) {
      float iconSize = 16.0F;
      String label = label(icon);
      float iconWidth = Render2D.textWidth(font, icon, iconSize);
      float labelWidth = Render2D.textWidth(TEXT_FONT, label, 6.0F);
      Render2D.rect(cellX, cellY, 22.0F, 27.0F, 5.0F, ColorUtil.rgba(255, 255, 255, 16));
      Render2D.outline(cellX, cellY, 22.0F, 27.0F, 5.0F, 0.6F, ColorUtil.rgba(255, 255, 255, 34));
      Render2D.text(font, icon, cellX + (22.0F - iconWidth) * 0.5F, cellY + 3.2F, iconSize, ColorUtil.rgba(255, 255, 255, 245));
      Render2D.text(TEXT_FONT, label, cellX + (22.0F - labelWidth) * 0.5F, cellY + 19.5F, 6.0F, MUTED_COLOR);
   }

   private static int rows(String icons) {
      return (int)Math.ceil((double)icons.length() / 14.0);
   }

   private static String label(String icon) {
      return " ".equals(icon) ? "SP" : icon;
   }

   private static record IconSection(String title, FontType font, String icons) {
   }

   private static record WatermarkIconsState(float x, float y, float width, float height) {
   }
}
