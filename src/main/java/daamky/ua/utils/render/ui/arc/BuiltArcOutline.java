package daamky.ua.utils.render.ui.arc;

import net.minecraft.client.gui.DrawContext;

public record BuiltArcOutline(
   float x, float y, float size, float arcThickness, float degree, float rotation, float outlineThickness, int fillColor, int outlineColor
) {
   public void render(DrawContext graphics) {
      ArcOutlineRenderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      return this.size > 0.0F
         && this.arcThickness > 0.0F
         && this.degree > 0.0F
         && this.outlineThickness > 0.0F
         && (this.fillColor >>> 24 != 0 || this.outlineColor >>> 24 != 0);
   }
}
