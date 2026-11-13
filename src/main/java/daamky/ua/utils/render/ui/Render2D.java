package daamky.ua.utils.render.ui;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
import org.joml.Matrix4f;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.arc.ArcOutlineRenderer;
import daamky.ua.utils.render.ui.arc.ArcRenderer;
import daamky.ua.utils.render.ui.arc.BuiltArc;
import daamky.ua.utils.render.ui.arc.BuiltArcOutline;
import daamky.ua.utils.render.ui.blur.BlurBuilder;
import daamky.ua.utils.render.ui.blur.BlurFramebuffer;
import daamky.ua.utils.render.ui.blur.BuiltBlur;
import daamky.ua.utils.render.ui.effecticon.BuiltEffectIcon;
import daamky.ua.utils.render.ui.effecticon.EffectIconRenderer;
import daamky.ua.utils.render.ui.font.BuiltText;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.render.ui.font.TextRenderer;
import daamky.ua.utils.render.ui.glass.BuiltGlass;
import daamky.ua.utils.render.ui.glass.GlassRenderer;
import daamky.ua.utils.render.ui.image.BuiltImage;
import daamky.ua.utils.render.ui.image.ImageRenderer;
import daamky.ua.utils.render.ui.msdf.MsdfIconRenderer;
import daamky.ua.utils.render.ui.outline.outline360.BuiltOutline360;
import daamky.ua.utils.render.ui.outline.outline360.Outline360Range;
import daamky.ua.utils.render.ui.outline.outline360.Outline360Renderer;
import daamky.ua.utils.render.ui.outline.outlinedefault.BuiltOutline;
import daamky.ua.utils.render.ui.outline.outlinedefault.DefaultOutlineRenderer;
import daamky.ua.utils.render.ui.outline.outlineglass.BuiltGlassOutline;
import daamky.ua.utils.render.ui.outline.outlineglass.GlassOutlineRenderer;
import daamky.ua.utils.render.ui.rectangle.rectdefault.BuiltRectangle;
import daamky.ua.utils.render.ui.rectangle.rectdefault.DefaultRectangleRenderer;
import daamky.ua.utils.render.ui.rectangle.recthalficon.BuiltHalfIconRectangle;
import daamky.ua.utils.render.ui.rectangle.recthalficon.HalfIconRectangleRenderer;
import daamky.ua.utils.render.ui.rectangle.recthalftone.BuiltHalftoneRectangle;
import daamky.ua.utils.render.ui.rectangle.recthalftone.HalftoneRectangleRenderer;
import daamky.ua.utils.render.ui.ripple.BuiltRipple;
import daamky.ua.utils.render.ui.ripple.RippleRenderer;
import daamky.ua.utils.render.ui.zippy.BuiltZippy;
import daamky.ua.utils.render.ui.zippy.ZippyRenderer;

public final class Render2D {
   private static final Render2D.PointProjector IDENTITY_PROJECTOR = Render2D.ProjectedPoint::new;
   private static final Deque<Matrix4f> PROJECTION_OVERRIDES = new ArrayDeque<>();
   private static final Deque<Render2D.PointProjector> POINT_PROJECTORS = new ArrayDeque<>();

   private Render2D() {
   }

   public static void init() {
   }

   public static int getFixedScaledWidth() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      return minecraft != null && minecraft.getWindow() != null
         ? Math.max(1, (int)Math.ceil((double)((float)minecraft.getWindow().getFramebufferWidth() / Render2DCoordinateSpace.designGuiScale())))
         : 1;
   }

   public static int getFixedScaledHeight() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      return minecraft != null && minecraft.getWindow() != null
         ? Math.max(1, (int)Math.ceil((double)((float)minecraft.getWindow().getFramebufferHeight() / Render2DCoordinateSpace.designGuiScale())))
         : 1;
   }

   public static float guiToFixed(float value) {
      float scale = Render2DCoordinateSpace.guiIndependentScale();
      return Math.abs(scale) <= 1.0E-4F ? value : value / scale;
   }

   public static double guiToFixed(double value) {
      float scale = Render2DCoordinateSpace.guiIndependentScale();
      return Math.abs(scale) <= 1.0E-4F ? value : value / (double)scale;
   }

   public static void withProjection(Matrix4f projection, Render2D.PointProjector projector, Runnable block) {
      PROJECTION_OVERRIDES.addLast(new Matrix4f(projection));
      POINT_PROJECTORS.addLast(projector == null ? IDENTITY_PROJECTOR : projector);

      try {
         block.run();
      } finally {
         PROJECTION_OVERRIDES.removeLast();
         POINT_PROJECTORS.removeLast();
      }
   }

   public static Render2D.PointProjector getCurrentProjector() {
      return POINT_PROJECTORS.peekLast();
   }

   public static boolean hasProjectionOverride() {
      return !PROJECTION_OVERRIDES.isEmpty();
   }

   public static Render2D.ProjectedRect projectRect(float x, float y, float width, float height) {
      Render2D.PointProjector projector = getCurrentProjector();
      if (projector == null) {
         return new Render2D.ProjectedRect(x, y, width, height);
      } else {
         Render2D.ProjectedPoint[] points = new Render2D.ProjectedPoint[]{
            projector.project(x, y), projector.project(x + width, y), projector.project(x + width, y + height), projector.project(x, y + height)
         };
         float minX = Float.POSITIVE_INFINITY;
         float minY = Float.POSITIVE_INFINITY;
         float maxX = Float.NEGATIVE_INFINITY;
         float maxY = Float.NEGATIVE_INFINITY;
         boolean hasPoint = false;

         for (Render2D.ProjectedPoint point : points) {
            if (point != null) {
               hasPoint = true;
               minX = Math.min(minX, point.x());
               minY = Math.min(minY, point.y());
               maxX = Math.max(maxX, point.x());
               maxY = Math.max(maxY, point.y());
            }
         }

         return !hasPoint ? null : new Render2D.ProjectedRect(minX, minY, Math.max(0.0F, maxX - minX), Math.max(0.0F, maxY - minY));
      }
   }

   public static void beginFrame(DrawContext graphics) {
      blur().beginFrame(graphics);
      glass().beginFrame(graphics);
      glassOutline().beginFrame(graphics);
      rectangle().beginFrame(graphics);
      halfIconRectangle().beginFrame(graphics);
      halftoneRectangle().beginFrame(graphics);
      zippy().beginFrame(graphics);
      arc().beginFrame(graphics);
      arcOutline().beginFrame(graphics);
      outline().beginFrame(graphics);
      outline360().beginFrame(graphics);
      text().beginFrame(graphics);
      msdfIcon().beginFrame(graphics);
      image().beginFrame(graphics);
      effectIcon().beginFrame(graphics);
      ripple().beginFrame(graphics);
   }

   public static void flush() {
      blur().flush();
      glass().flush();
      glassOutline().flush();
      rectangle().flush();
      halfIconRectangle().flush();
      halftoneRectangle().flush();
      zippy().flush();
      arc().flush();
      arcOutline().flush();
      outline().flush();
      outline360().flush();
      text().flush();
      msdfIcon().flush();
      image().flush();
      effectIcon().flush();
      ripple().flush();
   }

   public static void blur(float x, float y, float width, float height, float radius) {
      blur(x, y, width, height, radius, 16.0F, 1.0F, ColorUtil.WHITE);
   }

   public static void blur(float x, float y, float width, float height, float radius, float blurRadius) {
      blur(x, y, width, height, radius, blurRadius, 1.0F, ColorUtil.WHITE);
   }

   public static void blur(float x, float y, float width, float height, float radius, float blurRadius, float smoothness, int color) {
      imageBarrier();
      blur().enqueue(new BuiltBlur(x, y, width, height, radius, smoothness, blurRadius).withColor(color));
   }

   public static void blur(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float blurRadius,
      float smoothness,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      imageBarrier();
      blur()
         .enqueue(new BuiltBlur(x, y, width, height, radius, smoothness, blurRadius).withColors(colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft));
   }

   public static void blur(DrawContext graphics, float x, float y, float width, float height, float radius, float blurRadius) {
      beginFrame(graphics);
      blur(x, y, width, height, radius, blurRadius);
      flush();
   }

   public static void blur(BuiltBlur blur) {
      imageBarrier();
      blur().enqueue(blur);
   }

   public static BlurBuilder blurBuilder() {
      return new BlurBuilder();
   }

   public static void glass(
      float x,
      float y,
      float width,
      float height,
      float[] radius,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      imageBarrier();
      glass()
         .enqueue(
            new BuiltGlass(
               x, y, width, height, radius, color, globalAlpha, fresnelPower, fresnelColor, baseAlpha, fresnelInvert, fresnelMix, distortStrength, squirt, z
            )
         );
   }

   public static void glass(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      imageBarrier();
      glass()
         .enqueue(
            new BuiltGlass(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               z
            )
         );
   }

   public static void glass(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      imageBarrier();
      glass()
         .enqueue(
            new BuiltGlass(
               x,
               y,
               width,
               height,
               radius,
               radius,
               radius,
               radius,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               z
            )
         );
   }

   public static void glass(BuiltGlass glass) {
      imageBarrier();
      glass().enqueue(glass);
   }

   public static void liquidGlass(float x, float y, float width, float height, float squirt, float power, float radius, int color) {
      glass(
         x,
         y,
         width,
         height,
         radius * squirt / 2.0F,
         radius * squirt / 2.0F,
         radius * squirt / 2.0F,
         radius * squirt / 2.0F,
         color,
         (float)(color >>> 24 & 0xFF) / 255.0F,
         height == 240.0F ? 100.0F : 50.0F,
         color | 0xFF000000,
         1.0F,
         true,
         0.0F,
         power,
         squirt,
         0.0F
      );
   }

   public static void glassOutline(
      float x,
      float y,
      float width,
      float height,
      float[] radius,
      float thickness,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      imageBarrier();
      glassOutline()
         .enqueue(
            new BuiltGlassOutline(
               x,
               y,
               width,
               height,
               radius,
               thickness,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               z
            )
         );
   }

   public static void glassOutline(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      imageBarrier();
      glassOutline()
         .enqueue(
            new BuiltGlassOutline(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               thickness,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               1.0F,
               z
            )
         );
   }

   public static void glassOutline(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int color,
      float globalAlpha,
      float fresnelPower,
      int fresnelColor,
      float baseAlpha,
      boolean fresnelInvert,
      float fresnelMix,
      float distortStrength,
      float squirt,
      float z
   ) {
      imageBarrier();
      glassOutline()
         .enqueue(
            new BuiltGlassOutline(
               x,
               y,
               width,
               height,
               radius,
               radius,
               radius,
               radius,
               thickness,
               color,
               globalAlpha,
               fresnelPower,
               fresnelColor,
               baseAlpha,
               fresnelInvert,
               fresnelMix,
               distortStrength,
               squirt,
               1.0F,
               z
            )
         );
   }

   public static void glassOutline(BuiltGlassOutline outline) {
      imageBarrier();
      glassOutline().enqueue(outline);
   }

   public static void liquidGlassOutline(float x, float y, float width, float height, float squirt, float power, float radius, float thickness, int color) {
      glassOutline(
         x,
         y,
         width,
         height,
         radius * squirt / 2.0F,
         radius * squirt / 2.0F,
         radius * squirt / 2.0F,
         radius * squirt / 2.0F,
         thickness,
         color,
         (float)(color >>> 24 & 0xFF) / 255.0F,
         height == 240.0F ? 100.0F : 50.0F,
         color | 0xFF000000,
         1.0F,
         true,
         0.0F,
         power,
         squirt,
         0.0F
      );
   }

   public static void rect(float x, float y, float width, float height, int color) {
      rect(x, y, width, height, 0.0F, color);
   }

   public static void rect(float x, float y, float width, float height, float radius, int color) {
      imageBarrier();
      rectangle().enqueue(new BuiltRectangle(x, y, width, height, radius, color));
   }

   public static void rect(
      float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int color
   ) {
      imageBarrier();
      rectangle().enqueue(new BuiltRectangle(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color));
   }

   public static void rect(
      float x, float y, float width, float height, float radius, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft
   ) {
      rect(x, y, width, height, radius, radius, radius, radius, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
   }

   public static void rect(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      imageBarrier();
      rectangle()
         .enqueue(
            new BuiltRectangle(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               1.0F
            )
         );
   }

   public static void rect(DrawContext graphics, float x, float y, float width, float height, float radius, int color) {
      beginFrame(graphics);
      rect(x, y, width, height, radius, color);
      flush();
   }

   public static void rect(BuiltRectangle rectangle) {
      rectangle().enqueue(rectangle);
   }

   public static void arc(float x, float y, float size, float thickness, float degree, float rotation, int color) {
      imageBarrier();
      arc().enqueue(new BuiltArc(x, y, size, thickness, degree, rotation, color));
   }

   public static void arc(float x, float y, float size, float thickness, float degree, float rotation, int... colors) {
      imageBarrier();
      arc().enqueue(new BuiltArc(x, y, size, thickness, degree, rotation, colors));
   }

   public static void arc(BuiltArc arc) {
      imageBarrier();
      arc().enqueue(arc);
   }

   public static void arcOutline(
      float x, float y, float size, float arcThickness, float degree, float rotation, float outlineThickness, int fillColor, int outlineColor
   ) {
      imageBarrier();
      arcOutline().enqueue(new BuiltArcOutline(x, y, size, arcThickness, degree, rotation, outlineThickness, fillColor, outlineColor));
   }

   public static void arcOutline(BuiltArcOutline outline) {
      imageBarrier();
      arcOutline().enqueue(outline);
   }

   public static void halftoneRect(float x, float y, float width, float height, float radius, int color, int dotColor, float dotSize, float dotSpacing) {
      imageBarrier();
      halftoneRectangle().enqueue(new BuiltHalftoneRectangle(x, y, width, height, radius, color, dotColor, dotSize, dotSpacing));
   }

   public static void halftoneRect(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color,
      int dotColor,
      float dotSize,
      float dotSpacing
   ) {
      imageBarrier();
      halftoneRectangle()
         .enqueue(
            new BuiltHalftoneRectangle(
               x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color, dotColor, dotSize, dotSpacing
            )
         );
   }

   public static void halftoneRect(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      int dotColor,
      float dotSize,
      float dotSpacing
   ) {
      halftoneRect(
         x, y, width, height, radius, radius, radius, radius, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft, dotColor, dotSize, dotSpacing
      );
   }

   public static void halftoneRect(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      int dotColor,
      float dotSize,
      float dotSpacing
   ) {
      imageBarrier();
      halftoneRectangle()
         .enqueue(
            new BuiltHalftoneRectangle(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               1.0F,
               dotColor,
               dotSize,
               dotSpacing
            )
         );
   }

   public static void halftoneRect(BuiltHalftoneRectangle rectangle) {
      imageBarrier();
      halftoneRectangle().enqueue(rectangle);
   }

   public static void halfIconRect(
      float x,
      float y,
      float width,
      float height,
      float radius,
      int color,
      int iconColor,
      String fontName,
      String icon,
      float iconSize,
      float spacingX,
      float spacingY,
      float rotationMaxDegrees
   ) {
      halfIconRect(new BuiltHalfIconRectangle(x, y, width, height, radius, color, fontName, icon, iconSize, iconColor, spacingX, spacingY, rotationMaxDegrees));
   }

   public static void halfIconRect(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      int color,
      int iconColor,
      String fontName,
      String icon,
      float iconSize,
      float spacingX,
      float spacingY,
      float paddingX,
      float paddingY,
      float jitterX,
      float jitterY,
      float rotationMaxDegrees,
      long seed
   ) {
      halfIconRect(
         new BuiltHalfIconRectangle(
            x,
            y,
            width,
            height,
            radiusTopLeft,
            radiusTopRight,
            radiusBottomRight,
            radiusBottomLeft,
            color,
            color,
            color,
            color,
            1.0F,
            fontName,
            icon,
            iconSize,
            iconColor,
            spacingX,
            spacingY,
            paddingX,
            paddingY,
            jitterX,
            jitterY,
            rotationMaxDegrees,
            seed
         )
      );
   }

   public static void halfIconRect(BuiltHalfIconRectangle rectangle) {
      imageBarrier();
      if (rectangle != null && rectangle.backgroundVisible()) {
         rect(
            rectangle.x(),
            rectangle.y(),
            rectangle.width(),
            rectangle.height(),
            rectangle.radiusTopLeft(),
            rectangle.radiusTopRight(),
            rectangle.radiusBottomRight(),
            rectangle.radiusBottomLeft(),
            rectangle.colorTopLeft(),
            rectangle.colorTopRight(),
            rectangle.colorBottomRight(),
            rectangle.colorBottomLeft()
         );
      }

      if (rectangle != null && rectangle.iconsVisible()) {
         halfIconRectangle().enqueue(rectangle);
      }
   }

   public static void zippy(float x, float y, float width, float height, float radius, int color) {
      imageBarrier();
      zippy().enqueue(new BuiltZippy(x, y, width, height, radius, color));
   }

   public static void zippy(
      float x, float y, float width, float height, float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft, int color
   ) {
      imageBarrier();
      zippy().enqueue(new BuiltZippy(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, color));
   }

   public static void zippy(BuiltZippy zippy) {
      imageBarrier();
      zippy().enqueue(zippy);
   }

   public static void rectHalftone(float x, float y, float width, float height, float radius, int color, int dotColor, float dotSize, float dotSpacing) {
      halftoneRect(x, y, width, height, radius, color, dotColor, dotSize, dotSpacing);
   }

   public static void outline(float x, float y, float width, float height, float radius, float thickness, int color) {
      imageBarrier();
      outline().enqueue(new BuiltOutline(x, y, width, height, radius, thickness, color));
   }

   public static void outline(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int color
   ) {
      imageBarrier();
      outline().enqueue(new BuiltOutline(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, color));
   }

   public static void outline(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float thickness,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      outline(x, y, width, height, radius, radius, radius, radius, thickness, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
   }

   public static void outline(
      float x,
      float y,
      float width,
      float height,
      float radiusTopLeft,
      float radiusTopRight,
      float radiusBottomRight,
      float radiusBottomLeft,
      float thickness,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft
   ) {
      imageBarrier();
      outline()
         .enqueue(
            new BuiltOutline(
               x,
               y,
               width,
               height,
               radiusTopLeft,
               radiusTopRight,
               radiusBottomRight,
               radiusBottomLeft,
               thickness,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               1.0F
            )
         );
   }

   public static void outline(DrawContext graphics, float x, float y, float width, float height, float radius, float thickness, int color) {
      beginFrame(graphics);
      outline(x, y, width, height, radius, thickness, color);
      flush();
   }

   public static void outline(BuiltOutline outline) {
      imageBarrier();
      outline().enqueue(outline);
   }

   public static Outline360Range outline360Range(float startDegrees, float endDegrees, int color) {
      return Outline360Range.of(startDegrees, endDegrees, color);
   }

   public static void outline360(float x, float y, float width, float height, float radius, float thickness, int defaultColor, Outline360Range... ranges) {
      imageBarrier();
      outline360(x, y, width, height, radius, thickness, defaultColor, Arrays.asList(ranges));
   }

   public static void outline360(float x, float y, float width, float height, float radius, float thickness, int defaultColor, List<Outline360Range> ranges) {
      imageBarrier();
      outline360().enqueue(new BuiltOutline360(x, y, width, height, radius, thickness, defaultColor, ranges));
   }

   public static void outline360(
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
      Outline360Range... ranges
   ) {
      imageBarrier();
      outline360(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, defaultColor, Arrays.asList(ranges));
   }

   public static void outline360(
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
      imageBarrier();
      outline360()
         .enqueue(new BuiltOutline360(x, y, width, height, radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft, thickness, defaultColor, ranges));
   }

   public static void outline360(BuiltOutline360 outline) {
      imageBarrier();
      outline360().enqueue(outline);
   }

   public static void text(String nameFont, String text, float x, float y, float size, int color) {
      imageBarrier();
      text().enqueue(new BuiltText(nameFont, text, x, y, size, color));
   }

   public static void text(FontType font, String text, float x, float y, float size, int color) {
      if (canUseMsdf(font, text)) {
         imageBarrier();
         msdfIcon().draw(font, text, x, y, size, color);
      } else {
         text(fontName(font), text, x, y, size, color);
         if (font == FontType.BOLD) {
            text(fontName(font), text, x + boldOffset(size), y, size, color);
         }
      }
   }

   public static void textFade(
      String nameFont,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      boolean fadeLeft,
      boolean fadeRight
   ) {
      imageBarrier();
      text().enqueue(new BuiltText(nameFont, text, x, y, size, color).withHorizontalFade(fadeLeftX, fadeRightX, fadeWidth, fadeLeft, fadeRight));
   }

   public static void textFade(
      FontType font,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      boolean fadeLeft,
      boolean fadeRight
   ) {
      if (canUseMsdf(font, text)) {
         imageBarrier();
         msdfIcon()
            .draw(
               font,
               text,
               x,
               y,
               size,
               color,
               color,
               color,
               color,
               0.0F,
               x,
               y,
               fadeLeft,
               fadeRight,
               fadeLeftX,
               fadeRightX,
               fadeWidth,
               fadeLeft ? 1.0F : 0.0F,
               fadeRight ? 1.0F : 0.0F
            );
      } else {
         textFade(fontName(font), text, x, y, size, color, fadeLeftX, fadeRightX, fadeWidth, fadeLeft, fadeRight);
         if (font == FontType.BOLD) {
            textFade(fontName(font), text, x + boldOffset(size), y, size, color, fadeLeftX, fadeRightX, fadeWidth, fadeLeft, fadeRight);
         }
      }
   }

   public static void textFade(
      String nameFont,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      imageBarrier();
      text()
         .enqueue(new BuiltText(nameFont, text, x, y, size, color).withHorizontalFade(fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength));
   }

   public static void textFade(
      FontType font,
      String text,
      float x,
      float y,
      float size,
      int color,
      float fadeLeftX,
      float fadeRightX,
      float fadeWidth,
      float fadeLeftStrength,
      float fadeRightStrength
   ) {
      if (canUseMsdf(font, text)) {
         imageBarrier();
         msdfIcon()
            .draw(
               font,
               text,
               x,
               y,
               size,
               color,
               color,
               color,
               color,
               0.0F,
               x,
               y,
               fadeLeftStrength > 0.001F,
               fadeRightStrength > 0.001F,
               fadeLeftX,
               fadeRightX,
               fadeWidth,
               fadeLeftStrength,
               fadeRightStrength
            );
      } else {
         textFade(fontName(font), text, x, y, size, color, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength);
         if (font == FontType.BOLD) {
            textFade(fontName(font), text, x + boldOffset(size), y, size, color, fadeLeftX, fadeRightX, fadeWidth, fadeLeftStrength, fadeRightStrength);
         }
      }
   }

   public static void text(
      String nameFont, String text, float x, float y, float size, int color, float rotationDegrees, float rotationOriginX, float rotationOriginY
   ) {
      imageBarrier();
      text()
         .enqueue(
            new BuiltText(
               nameFont,
               text,
               x,
               y,
               size,
               color,
               color,
               color,
               color,
               rotationDegrees,
               rotationOriginX,
               rotationOriginY,
               false,
               false,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F
            )
         );
   }

   public static void text(
      FontType font, String text, float x, float y, float size, int color, float rotationDegrees, float rotationOriginX, float rotationOriginY
   ) {
      if (canUseMsdf(font, text)) {
         imageBarrier();
         msdfIcon()
            .draw(
               font,
               text,
               x,
               y,
               size,
               color,
               color,
               color,
               color,
               rotationDegrees,
               rotationOriginX,
               rotationOriginY,
               false,
               false,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F
            );
      } else {
         text(fontName(font), text, x, y, size, color, rotationDegrees, rotationOriginX, rotationOriginY);
         if (font == FontType.BOLD) {
            text(fontName(font), text, x + boldOffset(size), y, size, color, rotationDegrees, rotationOriginX, rotationOriginY);
         }
      }
   }

   public static void text(
      String nameFont, String text, float x, float y, float size, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft
   ) {
      imageBarrier();
      text().enqueue(new BuiltText(nameFont, text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft));
   }

   public static void text(
      FontType font, String text, float x, float y, float size, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft
   ) {
      if (canUseMsdf(font, text)) {
         imageBarrier();
         msdfIcon()
            .draw(
               font, text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft, 0.0F, x, y, false, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F
            );
      } else {
         text(fontName(font), text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
         if (font == FontType.BOLD) {
            text(fontName(font), text, x + boldOffset(size), y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
         }
      }
   }

   public static void ripple(
      float x, float y, float width, float height, float radius, float centerX, float centerY, float progress, int sourceColor, int targetColor
   ) {
      ripple(x, y, width, height, radius, 0.5F, centerX, centerY, progress, 25.0F, sourceColor, targetColor);
   }

   public static void ripple(
      float x,
      float y,
      float width,
      float height,
      float radius,
      float smoothness,
      float centerX,
      float centerY,
      float progress,
      float rippleSmoothness,
      int sourceColor,
      int targetColor
   ) {
      ripple()
         .enqueue(
            new BuiltRipple(
               x, y, width, height, radius, radius, radius, radius, smoothness, centerX, centerY, progress, rippleSmoothness, sourceColor, targetColor, false
            )
         );
   }

   public static void rippleIcon(
      float x, float y, float width, float height, float radius, float centerX, float centerY, float progress, int sourceColor, int targetColor
   ) {
      ripple()
         .enqueue(new BuiltRipple(x, y, width, height, radius, radius, radius, radius, 0.5F, centerX, centerY, progress, 25.0F, sourceColor, targetColor, true));
   }

   public static void text(
      String nameFont,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY
   ) {
      imageBarrier();
      text()
         .enqueue(
            new BuiltText(
               nameFont,
               text,
               x,
               y,
               size,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               rotationDegrees,
               rotationOriginX,
               rotationOriginY,
               false,
               false,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F
            )
         );
   }

   public static void text(
      FontType font,
      String text,
      float x,
      float y,
      float size,
      int colorTopLeft,
      int colorTopRight,
      int colorBottomRight,
      int colorBottomLeft,
      float rotationDegrees,
      float rotationOriginX,
      float rotationOriginY
   ) {
      if (canUseMsdf(font, text)) {
         imageBarrier();
         msdfIcon()
            .draw(
               font,
               text,
               x,
               y,
               size,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               rotationDegrees,
               rotationOriginX,
               rotationOriginY,
               false,
               false,
               0.0F,
               0.0F,
               0.0F,
               0.0F,
               0.0F
            );
      } else {
         text(
            fontName(font), text, x, y, size, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft, rotationDegrees, rotationOriginX, rotationOriginY
         );
         if (font == FontType.BOLD) {
            text(
               fontName(font),
               text,
               x + boldOffset(size),
               y,
               size,
               colorTopLeft,
               colorTopRight,
               colorBottomRight,
               colorBottomLeft,
               rotationDegrees,
               rotationOriginX,
               rotationOriginY
            );
         }
      }
   }

   public static void text(DrawContext graphics, String nameFont, String text, float x, float y, float size, int color) {
      beginFrame(graphics);
      text(nameFont, text, x, y, size, color);
      flush();
   }

   public static void text(DrawContext graphics, FontType font, String text, float x, float y, float size, int color) {
      beginFrame(graphics);
      text(font, text, x, y, size, color);
      flush();
   }

   public static void text(BuiltText text) {
      imageBarrier();
      text().enqueue(text);
   }

   public static void image(String texture, float x, float y, float size, float radius) {
      image().enqueue(new BuiltImage(texture, x, y, size, radius));
   }

   public static void image(String texture, float x, float y, float size, float radius, int color) {
      image().enqueue(new BuiltImage(texture, x, y, size, radius, color));
   }

   public static void image(String texture, float x, float y, float size, float radius, int... colors) {
      image().enqueue(new BuiltImage(texture, x, y, size, radius).withColors(colors));
   }

   public static void image(String texture, float x, float y, float width, float height, float radius, int color) {
      image().enqueue(new BuiltImage(texture, x, y, width, height, radius, color));
   }

   public static void image(String texture, float x, float y, float width, float height, float radius, int... colors) {
      image().enqueue(new BuiltImage(texture, x, y, width, height, radius).withColors(colors));
   }

   public static void imageUv(String texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, int color) {
      image().enqueue(new BuiltImage(texture, x, y, width, height, radius, color).withUv(u0, v0, u1, v1));
   }

   public static void imageUv(String texture, float x, float y, float width, float height, float radius, float u0, float v0, float u1, float v1, int... colors) {
      image().enqueue(new BuiltImage(texture, x, y, width, height, radius).withUv(u0, v0, u1, v1).withColors(colors));
   }

   public static void imageUvNearest(
      String texture, float x, float y, float width, float height, float radius, float smoothness, float u0, float v0, float u1, float v1, int color
   ) {
      image().enqueue(new BuiltImage(texture, x, y, width, height, radius, color).withSmoothness(smoothness).withNearestFilter().withUv(u0, v0, u1, v1));
   }

   public static void imageUvNearest(
      String texture, float x, float y, float width, float height, float radius, float smoothness, float u0, float v0, float u1, float v1, int... colors
   ) {
      image()
         .enqueue(new BuiltImage(texture, x, y, width, height, radius).withSmoothness(smoothness).withNearestFilter().withUv(u0, v0, u1, v1).withColors(colors));
   }

   public static void image(String texture, float x, float y, float size, float radius, float rotationDegrees, float originX, float originY, int color) {
      image().enqueue(new BuiltImage(texture, x, y, size, radius, color).withRotation(rotationDegrees, originX, originY));
   }

   public static void image(String texture, float x, float y, float size, float radius, float rotationDegrees, float originX, float originY, int... colors) {
      image().enqueue(new BuiltImage(texture, x, y, size, radius).withColors(colors).withRotation(rotationDegrees, originX, originY));
   }

   public static void effectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size) {
      effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size));
   }

   public static void effectIcon(RegistryEntry<StatusEffect> effect, float x, float y, float size, int color) {
      effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size, color));
   }

   public static void effectIcon(StatusEffectInstance effect, float x, float y, float size) {
      if (effect != null && effect.shouldShowIcon()) {
         effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size));
      }
   }

   public static void effectIcon(StatusEffectInstance effect, float x, float y, float size, int color) {
      if (effect != null && effect.shouldShowIcon()) {
         effectIcon().enqueue(new BuiltEffectIcon(effect, x, y, size, color));
      }
   }

   public static float textWidth(String nameFont, String text, float size) {
      return text().width(nameFont, text, size);
   }

   public static float textWidth(FontType font, String text, float size) {
      return canUseMsdf(font, text)
         ? msdfIcon().width(font, text, size)
         : textWidth(fontName(font), text, size) + (font == FontType.BOLD ? boldOffset(size) : 0.0F);
   }

   public static float textVisualLeftInset(FontType font, String text, float size) {
      if (text != null && !text.isEmpty() && !(size <= 0.0F) && !canUseMsdf(font, text)) {
         TextRenderer.GlyphLayout layout = text().glyphLayout(fontName(font), text, size);
         float left = Float.POSITIVE_INFINITY;

         for (TextRenderer.GlyphPage page : layout.pages()) {
            for (TextRenderer.Glyph glyph : page.glyphs()) {
               left = Math.min(left, glyph.x0());
            }
         }

         return Float.isFinite(left) ? left : 0.0F;
      } else {
         return 0.0F;
      }
   }

   public static void warmupText(String nameFont, String text, float size) {
      text().glyphLayout(nameFont, text, size);
   }

   public static void warmupText(FontType font, String text, float size) {
      if (canUseMsdf(font, text)) {
         msdfIcon().width(font, text, size);
      } else {
         warmupText(fontName(font), text, size);
      }
   }

   public static void pushScissor(DrawContext graphics, float x, float y, float width, float height) {
      if (graphics != null) {
         float scissorX = x;
         float scissorY = y;
         float scissorWidth = width;
         float scissorHeight = height;
         if (hasProjectionOverride()) {
            Render2D.ProjectedRect projected = projectRect(x, y, width, height);
            if (projected == null) {
               projected = new Render2D.ProjectedRect(0.0F, 0.0F, 0.0F, 0.0F);
            }

            scissorX = projected.x();
            scissorY = projected.y();
            scissorWidth = projected.width();
            scissorHeight = projected.height();
         }

         int left = Render2DCoordinateSpace.toGuiInt(scissorX);
         int top = Render2DCoordinateSpace.toGuiInt(scissorY);
         int right = Render2DCoordinateSpace.toGuiInt(scissorX + scissorWidth);
         int bottom = Render2DCoordinateSpace.toGuiInt(scissorY + scissorHeight);
         ScissorUtil.push(left, top, right, bottom);
         graphics.enableScissor(left, top, right, bottom);
      }
   }

   public static void popScissor(DrawContext graphics) {
      if (graphics != null) {
         graphics.disableScissor();
         ScissorUtil.pop();
      }
   }

   public static void close() {
      BlurFramebuffer.closeInstance();
      GlassRenderer.closeInstance();
      GlassOutlineRenderer.closeInstance();
      DefaultRectangleRenderer.closeInstance();
      HalfIconRectangleRenderer.closeInstance();
      HalftoneRectangleRenderer.closeInstance();
      ZippyRenderer.closeInstance();
      ArcRenderer.closeInstance();
      ArcOutlineRenderer.closeInstance();
      DefaultOutlineRenderer.closeInstance();
      Outline360Renderer.closeInstance();
      TextRenderer.closeInstance();
      MsdfIconRenderer.closeInstance();
      ImageRenderer.closeInstance();
      EffectIconRenderer.closeInstance();
      RippleRenderer.closeInstance();
   }

   private static BlurFramebuffer blur() {
      return BlurFramebuffer.getInstance();
   }

   private static GlassRenderer glass() {
      return GlassRenderer.getInstance();
   }

   private static GlassOutlineRenderer glassOutline() {
      return GlassOutlineRenderer.getInstance();
   }

   private static DefaultRectangleRenderer rectangle() {
      return DefaultRectangleRenderer.getInstance();
   }

   private static HalfIconRectangleRenderer halfIconRectangle() {
      return HalfIconRectangleRenderer.getInstance();
   }

   private static HalftoneRectangleRenderer halftoneRectangle() {
      return HalftoneRectangleRenderer.getInstance();
   }

   private static ZippyRenderer zippy() {
      return ZippyRenderer.getInstance();
   }

   private static ArcRenderer arc() {
      return ArcRenderer.getInstance();
   }

   private static ArcOutlineRenderer arcOutline() {
      return ArcOutlineRenderer.getInstance();
   }

   private static DefaultOutlineRenderer outline() {
      return DefaultOutlineRenderer.getInstance();
   }

   private static Outline360Renderer outline360() {
      return Outline360Renderer.getInstance();
   }

   private static TextRenderer text() {
      return TextRenderer.getInstance();
   }

   private static MsdfIconRenderer msdfIcon() {
      return MsdfIconRenderer.getInstance();
   }

   private static ImageRenderer image() {
      return ImageRenderer.getInstance();
   }

   private static EffectIconRenderer effectIcon() {
      return EffectIconRenderer.getInstance();
   }

   private static RippleRenderer ripple() {
      return RippleRenderer.getInstance();
   }

   private static void imageBarrier() {
      ImageRenderer.getInstance().barrier();
      EffectIconRenderer.getInstance().barrier();
   }

   private static String fontName(FontType font) {
      return font == null ? FontType.DEFAULT.fontName() : font.fontName();
   }

   private static boolean canUseMsdf(FontType font, String text) {
      return font != null && font.msdf() && msdfIcon().canRender(font, text);
   }

   private static float boldOffset(float size) {
      return Math.max(0.16F, Math.min(0.42F, size * 0.035F));
   }

   @FunctionalInterface
   public interface PointProjector {
      Render2D.ProjectedPoint project(float var1, float var2);
   }

   public static record ProjectedPoint(float x, float y) {
   }

   public static record ProjectedRect(float x, float y, float width, float height) {
   }
}
