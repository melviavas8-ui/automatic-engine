package daamky.ua.screens.clickgui.impl.panel;

public final class ClickGuiPanelSlot {
   private ClickGuiPanel panel;
   private float x;
   private float y;
   private float width;
   private float height;
   private float scale;
   private float alpha;

   public ClickGuiPanelSlot() {
   }

   public ClickGuiPanelSlot set(ClickGuiPanel panel, float x, float y, float width, float height, float scale, float alpha) {
      this.panel = panel;
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.scale = scale;
      this.alpha = alpha;
      return this;
   }

   public ClickGuiPanel panel() {
      return this.panel;
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public float scale() {
      return this.scale;
   }

   public float alpha() {
      return this.alpha;
   }
}
