package daamky.ua.utils.render.item;

public record RenderItemOptions(float alpha, int color, RenderItemGlintMode glintMode, boolean showCount, boolean showDurability, float glintStrength) {
   private static final RenderItemOptions DEFAULTS = new RenderItemOptions(1.0F, -1, RenderItemGlintMode.AUTO, true, true, 0.62F);
   private static final RenderItemOptions COUNT_NO_DURABILITY = new RenderItemOptions(1.0F, -1, RenderItemGlintMode.AUTO, true, false, 0.62F);
   private static final RenderItemOptions NO_DECORATIONS = new RenderItemOptions(1.0F, -1, RenderItemGlintMode.AUTO, false, false, 0.62F);

   public RenderItemOptions(float alpha, int color, RenderItemGlintMode glintMode, boolean showCount, boolean showDurability, float glintStrength) {
      alpha = clamp(alpha, 0.0F, 1.0F);
      color = normalizeColor(color);
      glintMode = glintMode == null ? RenderItemGlintMode.AUTO : glintMode;
      glintStrength = clamp(glintStrength, 0.0F, 1.0F);
      this.alpha = alpha;
      this.color = color;
      this.glintMode = glintMode;
      this.showCount = showCount;
      this.showDurability = showDurability;
      this.glintStrength = glintStrength;
   }

   public static RenderItemOptions defaults() {
      return DEFAULTS;
   }

   public static RenderItemOptions decorated(float alpha) {
      return alpha >= 0.999F ? DEFAULTS : new RenderItemOptions(alpha, -1, RenderItemGlintMode.AUTO, true, true, 0.62F);
   }

   public static RenderItemOptions countNoDurability(float alpha) {
      return alpha >= 0.999F ? COUNT_NO_DURABILITY : new RenderItemOptions(alpha, -1, RenderItemGlintMode.AUTO, true, false, 0.62F);
   }

   public static RenderItemOptions noDecorations(float alpha) {
      return alpha >= 0.999F ? NO_DECORATIONS : new RenderItemOptions(alpha, -1, RenderItemGlintMode.AUTO, false, false, 0.62F);
   }

   public RenderItemOptions alpha(float alpha) {
      return new RenderItemOptions(alpha, this.color, this.glintMode, this.showCount, this.showDurability, this.glintStrength);
   }

   public RenderItemOptions color(int color) {
      return new RenderItemOptions(this.alpha, color, this.glintMode, this.showCount, this.showDurability, this.glintStrength);
   }

   public RenderItemOptions glint(RenderItemGlintMode mode) {
      return new RenderItemOptions(this.alpha, this.color, mode, this.showCount, this.showDurability, this.glintStrength);
   }

   public RenderItemOptions count(boolean showCount) {
      return new RenderItemOptions(this.alpha, this.color, this.glintMode, showCount, this.showDurability, this.glintStrength);
   }

   public RenderItemOptions durability(boolean showDurability) {
      return new RenderItemOptions(this.alpha, this.color, this.glintMode, this.showCount, showDurability, this.glintStrength);
   }

   public RenderItemOptions glintStrength(float glintStrength) {
      return new RenderItemOptions(this.alpha, this.color, this.glintMode, this.showCount, this.showDurability, glintStrength);
   }

   private static int normalizeColor(int color) {
      return (color & 0xFF000000) == 0 && (color & 16777215) != 0 ? color | 0xFF000000 : color;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
