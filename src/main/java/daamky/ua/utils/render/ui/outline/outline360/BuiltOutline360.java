package daamky.ua.utils.render.ui.outline.outline360;

import java.util.List;
import net.minecraft.client.gui.DrawContext;
import daamky.ua.utils.render.color.ColorUtil;

public record BuiltOutline360(
   float x,
   float y,
   float width,
   float height,
   float radiusTopLeft,
   float radiusTopRight,
   float radiusBottomRight,
   float radiusBottomLeft,
   float thickness,
   int defaultColor,
   float smoothness,
   float blendDegrees,
   float angleOffsetDegrees,
   List<Outline360Range> ranges
) {
   public static final float DEFAULT_SMOOTHNESS = 1.0F;
   public static final float DEFAULT_BLEND_DEGREES = 14.0F;
   public static final int DEFAULT_COLOR = ColorUtil.rgba(48, 48, 48, 255);

   public BuiltOutline360(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int defaultColor,
      float smoothness,
      float blendDegrees,
      float angleOffsetDegrees,
      List<Outline360Range> ranges
   ) {
      ranges = ranges == null ? List.of() : List.copyOf(ranges);
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
      this.radiusTopLeft = radiusTopLeft;
      this.radiusTopRight = radiusTopRight;
      this.radiusBottomRight = radiusBottomRight;
      this.radiusBottomLeft = radiusBottomLeft;
      this.thickness = thickness;
      this.defaultColor = defaultColor;
      this.smoothness = smoothness;
      this.blendDegrees = blendDegrees;
      this.angleOffsetDegrees = angleOffsetDegrees;
      this.ranges = ranges;
   }

   public BuiltOutline360(float x, float y, float width, float height, float radius, float thickness, int defaultColor, List<Outline360Range> ranges) {
      this(x, y, width, height, radius, radius, radius, radius, thickness, defaultColor, 1.0F, 14.0F, 0.0F, ranges);
   }

   public BuiltOutline360(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int defaultColor,
      List<Outline360Range> ranges
   ) {
      this(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, defaultColor, 1.0F, 14.0F, 0.0F, ranges);
   }

   public BuiltOutline360 withSmoothness(float smoothness) {
      return new BuiltOutline360(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.defaultColor,
         smoothness,
         this.blendDegrees,
         this.angleOffsetDegrees,
         this.ranges
      );
   }

   public BuiltOutline360 withBlendDegrees(float blendDegrees) {
      return new BuiltOutline360(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.defaultColor,
         this.smoothness,
         blendDegrees,
         this.angleOffsetDegrees,
         this.ranges
      );
   }

   public BuiltOutline360 withAngleOffset(float angleOffsetDegrees) {
      return new BuiltOutline360(
         this.x,
         this.y,
         this.width,
         this.height,
         this.radiusTopLeft,
         this.radiusTopRight,
         this.radiusBottomRight,
         this.radiusBottomLeft,
         this.thickness,
         this.defaultColor,
         this.smoothness,
         this.blendDegrees,
         angleOffsetDegrees,
         this.ranges
      );
   }

   public void render(DrawContext graphics) {
      Outline360Renderer.getInstance().draw(graphics, this);
   }

   public boolean visible() {
      boolean hasVisibleRange = this.ranges.stream().anyMatch(range -> range != null && range.color() >>> 24 != 0);
      return this.width > 0.0F && this.height > 0.0F && this.thickness > 0.0F && (this.defaultColor >>> 24 != 0 || hasVisibleRange);
   }
}
