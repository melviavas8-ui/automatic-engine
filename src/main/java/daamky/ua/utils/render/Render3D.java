package daamky.ua.utils.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.mixin.accessor.MixinRenderPipelines;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.color.ColorUtil;

public final class Render3D implements IMinecraft {
   private static final double MAX_SAFE_COORD = 3.0E7;
   private static final int TARGET_ESP_CIRCLE_SEGMENTS = 40;
   private static final Map<VoxelShape, Pair<List<Box>, List<Render3D.Line>>> SHAPE_OUTLINES = new HashMap<>();
   private static final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();
   public static final List<Render3D.Line> LINE_DEPTH = new ArrayList<>();
   public static final List<Render3D.Line> LINE = new ArrayList<>();
   public static final List<Render3D.Line> LINE_OVERLAY = new ArrayList<>();
   public static final List<Render3D.Quad> QUAD_DEPTH = new ArrayList<>();
   public static final List<Render3D.Quad> QUAD = new ArrayList<>();
   public static final List<Render3D.GradientQuad> GRADIENT_QUAD = new ArrayList<>();
   public static final List<Render3D.GradientQuad> GRADIENT_QUAD_DEPTH = new ArrayList<>();
   public static final Matrix4f lastProjMat = new Matrix4f();
   public static final Matrix4f lastModMat = new Matrix4f();
   public static final Matrix4f lastWorldSpaceMatrix = new Matrix4f();
   public static Entry lastWorldSpaceEntry = new MatrixStack().peek();
   public static float lastTickDelta = 1.0F;
   public static Vec3d lastCameraPos = Vec3d.ZERO;
   public static Quaternionf lastCameraRotation = new Quaternionf();
   private static final Vector3f LINE_NORMAL = new Vector3f();
   private static final BlendFunction STANDARD_BLEND = new BlendFunction(
      SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ZERO
   );
   private static final Snippet daamky_LINES_SNIPPET = RenderPipeline.builder(new Snippet[]{MixinRenderPipelines.daamky$getLinesSnippet()})
      .withBlend(STANDARD_BLEND)
      .withDepthWrite(false)
      .withCull(false)
      .buildSnippet();
   private static final RenderLayer daamky_LINES_OVERLAY = RenderLayerFactory.create(
      "rendertype/daamky_lines_overlay",
      256,
      RenderPipeline.builder(new Snippet[]{daamky_LINES_SNIPPET})
         .withLocation("pipelines/daamky_lines_overlay")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   private static final RenderLayer daamky_LINES_NO_DEPTH = RenderLayerFactory.create(
      "rendertype/daamky_lines_no_depth",
      256,
      RenderPipeline.builder(new Snippet[]{daamky_LINES_SNIPPET})
         .withLocation("pipelines/daamky_lines_no_depth")
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .build()
   );
   private static final RenderLayer daamky_FILLED_BOX_NO_DEPTH = RenderLayerFactory.create(
      "rendertype/daamky_filled_box_no_depth",
      256,
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation("pipelines/daamky_filled_box_no_depth")
         .withBlend(STANDARD_BLEND)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build()
   );
   private static float espValue = 1.0F;
   private static float espSpeed = 1.0F;
   private static float prevEspValue;
   private static float circleStep;
   private static boolean flipSpeed;
   private static double smoothY = 0.0;
   private static double smoothY2 = 0.0;

   private Render3D() {
   }

   public static void capture(Matrix4fc projection, Matrix4fc view, Vec3d cameraPos) {
      if (projection != null) {
         lastProjMat.set(projection);
      }

      if (view != null) {
         lastModMat.set(view);
         lastWorldSpaceMatrix.set(view);
      }

      if (cameraPos != null && isFinite(cameraPos)) {
         lastCameraPos = cameraPos;
      }
   }

   public static void setLastWorldSpaceEntry(Entry entry) {
      if (entry != null) {
         lastWorldSpaceEntry = entry;
      }
   }

   public static void setLastTickDelta(float tickDelta) {
      lastTickDelta = Float.isFinite(tickDelta) ? tickDelta : 1.0F;
   }

   public static void setLastCameraPos(Vec3d cameraPos) {
      if (cameraPos != null && isFinite(cameraPos)) {
         lastCameraPos = cameraPos;
      }
   }

   public static void setLastCameraRotation(Quaternionf rotation) {
      if (rotation != null) {
         lastCameraRotation = rotation;
      }
   }

   public static void updateTargetEsp(float deltaTime) {
      prevEspValue = espValue;
      espValue = espValue + espSpeed * deltaTime;
      if (espSpeed > 25.0F) {
         flipSpeed = true;
      }

      if (espSpeed < -25.0F) {
         flipSpeed = false;
      }

      espSpeed = flipSpeed ? espSpeed - 0.5F * deltaTime : espSpeed + 0.5F * deltaTime;
      circleStep += 0.06F * deltaTime;
   }

   public static void updateTargetEsp() {
      updateTargetEsp(1.0F);
   }

   public static float getEspValue() {
      return espValue;
   }

   public static float getPrevEspValue() {
      return prevEspValue;
   }

   public static float getCircleStep() {
      return circleStep;
   }

   private static double easeInOutSine(double t) {
      return -(Math.cos(Math.PI * t) - 1.0) / 2.0;
   }

   private static double smoothSinAnimation(double input) {
      double sin = (Math.sin(input) + 1.0) / 2.0;
      return easeInOutSine(sin);
   }

   public static void onWorldRender(WorldRenderEvent e) {
      if (mc.world != null && mc.player != null) {
         MatrixStack matrices = e.getStack();
         Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
         Vec3d cameraPos = lastCameraPos;
         if (!isFinite(cameraPos)) {
            clearQueues();
         } else {
            renderGradientQuads(matrices, immediate, cameraPos);
            renderQuads(matrices, immediate, cameraPos);
            renderLines(matrices, immediate, cameraPos);
            immediate.draw();
         }
      } else {
         clearQueues();
      }
   }

   public static void render(WorldRenderEvent e) {
      onWorldRender(e);
   }

   private static void clearQueues() {
      LINE_DEPTH.clear();
      LINE.clear();
      LINE_OVERLAY.clear();
      QUAD_DEPTH.clear();
      QUAD.clear();
      GRADIENT_QUAD.clear();
      GRADIENT_QUAD_DEPTH.clear();
   }

   private static void renderLines(MatrixStack matrices, Immediate immediate, Vec3d cameraPos) {
      if (!LINE.isEmpty() || !LINE_DEPTH.isEmpty() || !LINE_OVERLAY.isEmpty()) {
         try {
            renderLineBatch(matrices, immediate, cameraPos, LINE_DEPTH, RenderLayers.lines());
            renderLineBatch(matrices, immediate, cameraPos, LINE, daamky_LINES_NO_DEPTH);
            renderLineBatch(matrices, immediate, cameraPos, LINE_OVERLAY, daamky_LINES_OVERLAY);
         } finally {
            LINE.clear();
            LINE_DEPTH.clear();
            LINE_OVERLAY.clear();
         }
      }
   }

   private static void renderLineBatch(MatrixStack matrices, Immediate immediate, Vec3d cameraPos, List<Render3D.Line> lines, RenderLayer layer) {
      if (!lines.isEmpty()) {
         VertexConsumer buffer = immediate.getBuffer(layer);

         for (Render3D.Line line : lines) {
            drawLineVertex(matrices, buffer, line, cameraPos);
         }

         immediate.draw(layer);
      }
   }

   private static void drawLineVertex(MatrixStack matrices, VertexConsumer buffer, Render3D.Line line, Vec3d cameraPos) {
      if (line != null && isFinite(cameraPos) && isFinite(line.start) && isFinite(line.end)) {
         Entry entry = line.entry != null ? line.entry : matrices.peek();
         Vector3f normal = lineNormal(line.start, line.end);
         float width = sanitizeLineWidth(line.width);
         float x1 = (float)(line.start.x - cameraPos.x);
         float y1 = (float)(line.start.y - cameraPos.y);
         float z1 = (float)(line.start.z - cameraPos.z);
         float x2 = (float)(line.end.x - cameraPos.x);
         float y2 = (float)(line.end.y - cameraPos.y);
         float z2 = (float)(line.end.z - cameraPos.z);
         buffer.vertex(entry, x1, y1, z1).color(line.colorStart).normal(entry, normal).lineWidth(width);
         buffer.vertex(entry, x2, y2, z2).color(line.colorEnd).normal(entry, normal).lineWidth(width);
      }
   }

   private static void renderQuads(MatrixStack matrices, Immediate immediate, Vec3d cameraPos) {
      if (!QUAD.isEmpty() || !QUAD_DEPTH.isEmpty()) {
         if (!QUAD_DEPTH.isEmpty()) {
            RenderLayer layer = RenderLayers.debugFilledBox();
            VertexConsumer buffer = immediate.getBuffer(layer);

            for (Render3D.Quad quad : QUAD_DEPTH) {
               drawQuadVertex(matrices, buffer, quad, cameraPos);
            }

            immediate.draw(layer);
         }

         if (!QUAD.isEmpty()) {
            VertexConsumer buffer = immediate.getBuffer(daamky_FILLED_BOX_NO_DEPTH);

            for (Render3D.Quad quad : QUAD) {
               drawQuadVertex(matrices, buffer, quad, cameraPos);
            }

            immediate.draw(daamky_FILLED_BOX_NO_DEPTH);
         }

         QUAD.clear();
         QUAD_DEPTH.clear();
      }
   }

   private static void drawQuadVertex(MatrixStack matrices, VertexConsumer buffer, Render3D.Quad quad, Vec3d cameraPos) {
      if (quad != null && isFinite(cameraPos) && isFinite(quad.x) && isFinite(quad.y) && isFinite(quad.w) && isFinite(quad.z)) {
         Entry entry = quad.entry != null ? quad.entry : matrices.peek();
         float x1 = (float)(quad.x.x - cameraPos.x);
         float y1 = (float)(quad.x.y - cameraPos.y);
         float z1 = (float)(quad.x.z - cameraPos.z);
         float x2 = (float)(quad.y.x - cameraPos.x);
         float y2 = (float)(quad.y.y - cameraPos.y);
         float z2 = (float)(quad.y.z - cameraPos.z);
         float x3 = (float)(quad.w.x - cameraPos.x);
         float y3 = (float)(quad.w.y - cameraPos.y);
         float z3 = (float)(quad.w.z - cameraPos.z);
         float x4 = (float)(quad.z.x - cameraPos.x);
         float y4 = (float)(quad.z.y - cameraPos.y);
         float z4 = (float)(quad.z.z - cameraPos.z);
         buffer.vertex(entry, x1, y1, z1).color(quad.color);
         buffer.vertex(entry, x2, y2, z2).color(quad.color);
         buffer.vertex(entry, x3, y3, z3).color(quad.color);
         buffer.vertex(entry, x4, y4, z4).color(quad.color);
      }
   }

   private static void renderGradientQuads(MatrixStack matrices, Immediate immediate, Vec3d cameraPos) {
      if (!GRADIENT_QUAD.isEmpty() || !GRADIENT_QUAD_DEPTH.isEmpty()) {
         if (!GRADIENT_QUAD_DEPTH.isEmpty()) {
            RenderLayer layer = RenderLayers.debugFilledBox();
            VertexConsumer buffer = immediate.getBuffer(layer);

            for (Render3D.GradientQuad quad : GRADIENT_QUAD_DEPTH) {
               drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
            }

            immediate.draw(layer);
         }

         if (!GRADIENT_QUAD.isEmpty()) {
            VertexConsumer buffer = immediate.getBuffer(daamky_FILLED_BOX_NO_DEPTH);

            for (Render3D.GradientQuad quad : GRADIENT_QUAD) {
               drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
            }

            immediate.draw(daamky_FILLED_BOX_NO_DEPTH);
         }

         GRADIENT_QUAD.clear();
         GRADIENT_QUAD_DEPTH.clear();
      }
   }

   private static void drawGradientQuadVertex(MatrixStack matrices, VertexConsumer buffer, Render3D.GradientQuad quad, Vec3d cameraPos) {
      if (quad != null && isFinite(cameraPos) && isFinite(quad.p1) && isFinite(quad.p2) && isFinite(quad.p3) && isFinite(quad.p4)) {
         Entry entry = matrices.peek();
         float x1 = (float)(quad.p1.x - cameraPos.x);
         float y1 = (float)(quad.p1.y - cameraPos.y);
         float z1 = (float)(quad.p1.z - cameraPos.z);
         float x2 = (float)(quad.p2.x - cameraPos.x);
         float y2 = (float)(quad.p2.y - cameraPos.y);
         float z2 = (float)(quad.p2.z - cameraPos.z);
         float x3 = (float)(quad.p3.x - cameraPos.x);
         float y3 = (float)(quad.p3.y - cameraPos.y);
         float z3 = (float)(quad.p3.z - cameraPos.z);
         float x4 = (float)(quad.p4.x - cameraPos.x);
         float y4 = (float)(quad.p4.y - cameraPos.y);
         float z4 = (float)(quad.p4.z - cameraPos.z);
         buffer.vertex(entry, x1, y1, z1).color(quad.c1);
         buffer.vertex(entry, x2, y2, z2).color(quad.c2);
         buffer.vertex(entry, x3, y3, z3).color(quad.c3);
         buffer.vertex(entry, x4, y4, z4).color(quad.c4);
      }
   }

   public static void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red, int baseColor1, int baseColor2) {
      double cs = MathUtils.interpolate((double)circleStep - 0.17, (double)circleStep);
      Vec3d target = MathUtils.interpolate(lastTarget);
      boolean canSee = mc.player != null && mc.player.canSee(lastTarget);
      float hitEffect = Math.min(red * 2.0F, 1.0F);
      float distanceMultiplier = 1.0F + (float)Math.sin((double)hitEffect * Math.PI) * 0.18F;
      int size = 40;
      float entityWidth = lastTarget.getWidth() * distanceMultiplier;
      float entityHeight = lastTarget.getHeight();
      double targetY = smoothSinAnimation(cs) * (double)entityHeight;
      double targetY2 = smoothSinAnimation(cs - 0.35) * (double)entityHeight;
      smoothY = lerp(smoothY, targetY, 0.12);
      smoothY2 = lerp(smoothY2, targetY2, 0.1);
      int color1 = ColorUtil.multRed(baseColor1, 1.0F + red * 125.0F);
      int color2 = ColorUtil.multRed(baseColor2, 1.0F + red * 125.0F);
      float step = (float)((Math.PI * 2) / (double)size);
      float sinStep = MathHelper.sin((double)step);
      float cosStep = MathHelper.cos((double)step);
      float currentSin = 0.0F;
      float currentCos = 1.0F;

      for (int i = 0; i < size; i++) {
         float nextSin = currentSin * cosStep + currentCos * sinStep;
         float nextCos = currentCos * cosStep - currentSin * sinStep;
         float gradientT = 0.5F - 0.5F * currentCos;
         float gradientTNext = 0.5F - 0.5F * nextCos;
         int currentColor = ColorUtil.lerpColor(color1, color2, gradientT);
         int nextColor = ColorUtil.lerpColor(color1, color2, gradientTNext);
         int brightColor = ColorUtil.multAlpha(currentColor, 0.8F * anim);
         int brightColorNext = ColorUtil.multAlpha(nextColor, 0.8F * anim);
         int fadeColor = ColorUtil.multAlpha(currentColor, 0.0F);
         int fadeColorNext = ColorUtil.multAlpha(nextColor, 0.0F);
         Vec3d circlePoint = target.add((double)(currentCos * entityWidth), smoothY, (double)(-currentSin * entityWidth));
         Vec3d trailPoint = target.add((double)(currentCos * entityWidth), smoothY2, (double)(-currentSin * entityWidth));
         Vec3d nextCirclePoint = target.add((double)(nextCos * entityWidth), smoothY, (double)(-nextSin * entityWidth));
         Vec3d nextTrailPoint = target.add((double)(nextCos * entityWidth), smoothY2, (double)(-nextSin * entityWidth));
         drawGradientQuad(circlePoint, nextCirclePoint, nextTrailPoint, trailPoint, brightColor, brightColorNext, fadeColorNext, fadeColor, canSee);
         drawGradientQuad(trailPoint, nextTrailPoint, nextCirclePoint, circlePoint, fadeColor, fadeColorNext, brightColorNext, brightColor, canSee);
         int trailColorTop = ColorUtil.multAlpha(currentColor, 0.15F * anim);
         int trailColorBottom = ColorUtil.multAlpha(currentColor, 0.0F);
         drawLineGradient(circlePoint, trailPoint, trailColorTop, trailColorBottom, 6.0F, canSee);
         int circleColor = ColorUtil.multAlpha(currentColor, 1.0F * anim);
         int circleColorNext = ColorUtil.multAlpha(nextColor, 1.0F * anim);
         drawLineGradient(circlePoint, nextCirclePoint, circleColor, circleColorNext, 2.0F, canSee);
         currentSin = nextSin;
         currentCos = nextCos;
      }
   }

   public static void drawRadiusCircle(Vec3d center, float radius, int color) {
      if (mc.player != null) {
         double baseY = center.y;
         int fillColor = ColorUtil.multAlpha(color, 0.25F);
         int radiusInt = (int)Math.ceil((double)radius) + 1;

         for (int dx = -radiusInt; dx <= radiusInt; dx++) {
            for (int dz = -radiusInt; dz <= radiusInt; dz++) {
               boolean hasCornerInside = false;
               boolean hasCornerOutside = false;

               for (double ox = -0.5; ox <= 0.5; ox++) {
                  for (double oz = -0.5; oz <= 0.5; oz++) {
                     double cornerDist = Math.sqrt(((double)dx + ox) * ((double)dx + ox) + ((double)dz + oz) * ((double)dz + oz));
                     if (cornerDist <= (double)radius) {
                        hasCornerInside = true;
                     } else {
                        hasCornerOutside = true;
                     }
                  }
               }

               if (hasCornerInside && hasCornerOutside) {
                  double x = center.x + (double)dx;
                  double z = center.z + (double)dz;
                  Box box = new Box(x - 0.5, baseY, z - 0.5, x + 0.5, baseY + 1.0, z + 0.5);
                  drawBoxWithCross(box, color, fillColor, 2.0F);
               }
            }
         }
      }
   }

   public static void drawBoxWithCross(Box box, int lineColor, int fillColor, float lineWidth) {
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
      drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
      drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
      int crossColor = ColorUtil.multAlpha(lineColor, 0.6F);
      float crossWidth = lineWidth * 0.8F;
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
   }

   public static void drawBoxWithCrossFull(Box box, int lineColor, int fillColor, float lineWidth) {
      double x1 = box.minX;
      double y1 = box.minY;
      double z1 = box.minZ;
      double x2 = box.maxX;
      double y2 = box.maxY;
      double z2 = box.maxZ;
      drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
      drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
      drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, false);
      drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z1), fillColor, false);
      drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z1), new Vec3d(x1, y1, z1), fillColor, false);
      drawQuad(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
      drawQuad(new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
      drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), fillColor, false);
      drawQuad(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
      drawLine(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
      int crossColor = ColorUtil.multAlpha(lineColor, 0.6F);
      float crossWidth = lineWidth * 0.8F;
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
      drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
   }

   public static void drawPlastShape(BlockPos playerPos, Vec3d smooth, int lineColor, int fillColor) {
      if (mc.player != null) {
         float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
         if (Math.abs(mc.player.getPitch()) > 60.0F) {
            BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
            Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3.0F);
         } else if (yaw <= -157.5F || yaw >= 157.5F) {
            BlockPos blockPos = playerPos.north(3).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3.0F);
         } else if (yaw <= -112.5F) {
            drawSidePlast(playerPos.east(5).south().down(), smooth, lineColor, fillColor, -1, true);
         } else if (yaw <= -67.5F) {
            BlockPos blockPos = playerPos.east(2).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3.0F);
         } else if (yaw <= -22.5F) {
            drawSidePlast(playerPos.east(5).down(), smooth, lineColor, fillColor, 1, false);
         } else if ((double)yaw >= -22.5 && (double)yaw <= 22.5) {
            BlockPos blockPos = playerPos.south(2).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3.0F);
         } else if (yaw <= 67.5F) {
            drawSidePlast(playerPos.west(4).down(), smooth, lineColor, fillColor, 1, true);
         } else if (yaw <= 112.5F) {
            BlockPos blockPos = playerPos.west(3).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3.0F);
         } else if (yaw <= 157.5F) {
            drawSidePlast(playerPos.west(4).south().down(), smooth, lineColor, fillColor, -1, false);
         }
      }
   }

   private static void drawSidePlast(BlockPos blockPos, Vec3d smooth, int lineColor, int fillColor, int i, boolean ff) {
      Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
      int crossColor = ColorUtil.multAlpha(lineColor, 0.6F);
      List<Vec3d> horizontalPoints = new ArrayList<>();
      float x = ff ? (float)i : (float)(-i);
      horizontalPoints.add(vec3d);
      Vec3d current = vec3d.add((double)x, 0.0, 0.0);
      horizontalPoints.add(current);

      for (int f = 0; f < 4; f++) {
         Vec3d var16 = current.add(0.0, 0.0, (double)i);
         horizontalPoints.add(var16);
         current = var16.add((double)x, 0.0, 0.0);
         horizontalPoints.add(current);
      }

      current = current.add(0.0, 0.0, (double)i);
      horizontalPoints.add(current);
      current = current.add((double)(x * -2.0F), 0.0, 0.0);
      horizontalPoints.add(current);

      for (int f = 0; f < 3; f++) {
         Vec3d var19 = current.add(0.0, 0.0, (double)(i * -1));
         horizontalPoints.add(var19);
         current = var19.add((double)(x * -1.0F), 0.0, 0.0);
         horizontalPoints.add(current);
      }

      current = current.add(0.0, 0.0, (double)(i * -2));
      horizontalPoints.add(current);

      for (int p = 0; p < horizontalPoints.size() - 1; p++) {
         Vec3d p1 = horizontalPoints.get(p);
         Vec3d p2 = horizontalPoints.get(p + 1);
         drawLine(p1, p2, lineColor, 2.0F, false);
         drawLine(p1.add(0.0, 5.0, 0.0), p2.add(0.0, 5.0, 0.0), lineColor, 2.0F, false);
      }

      for (Vec3d point : horizontalPoints) {
         drawLine(point, point.add(0.0, 5.0, 0.0), lineColor, 2.0F, false);
      }

      for (int p = 0; p < horizontalPoints.size() - 1; p++) {
         Vec3d p1 = horizontalPoints.get(p);
         Vec3d p2 = horizontalPoints.get(p + 1);
         Vec3d p1Top = p1.add(0.0, 5.0, 0.0);
         Vec3d p2Top = p2.add(0.0, 5.0, 0.0);
         drawQuad(p1, p2, p2Top, p1Top, fillColor, false);
         drawQuad(p1Top, p2Top, p2, p1, fillColor, false);
         drawLine(p1, p2Top, crossColor, 1.6F, false);
         drawLine(p2, p1Top, crossColor, 1.6F, false);
      }

      current = vec3d;
      drawQuad(vec3d, vec3d.add((double)x, 0.0, 0.0), vec3d.add((double)x, 0.0, (double)(i * 2)), vec3d.add(0.0, 0.0, (double)(i * 2)), fillColor, false);
      drawQuad(vec3d.add(0.0, 0.0, (double)(i * 2)), vec3d.add((double)x, 0.0, (double)(i * 2)), vec3d.add((double)x, 0.0, 0.0), vec3d, fillColor, false);
      drawLine(vec3d, vec3d.add((double)x, 0.0, (double)(i * 2)), crossColor, 1.6F, false);
      drawLine(vec3d.add((double)x, 0.0, 0.0), vec3d.add(0.0, 0.0, (double)(i * 2)), crossColor, 1.6F, false);

      for (int f = 0; f < 3; f++) {
         current = current.add((double)x, 0.0, (double)i);
         drawQuad(
            current, current.add((double)x, 0.0, 0.0), current.add((double)x, 0.0, (double)(i * 2)), current.add(0.0, 0.0, (double)(i * 2)), fillColor, false
         );
         drawQuad(
            current.add(0.0, 0.0, (double)(i * 2)), current.add((double)x, 0.0, (double)(i * 2)), current.add((double)x, 0.0, 0.0), current, fillColor, false
         );
         drawLine(current, current.add((double)x, 0.0, (double)(i * 2)), crossColor, 1.6F, false);
         drawLine(current.add((double)x, 0.0, 0.0), current.add(0.0, 0.0, (double)(i * 2)), crossColor, 1.6F, false);
      }

      current = current.add((double)x, 0.0, (double)i);
      drawQuad(current, current.add((double)x, 0.0, 0.0), current.add((double)x, 0.0, (double)i), current.add(0.0, 0.0, (double)i), fillColor, false);
      drawQuad(current.add(0.0, 0.0, (double)i), current.add((double)x, 0.0, (double)i), current.add((double)x, 0.0, 0.0), current, fillColor, false);
      drawLine(current, current.add((double)x, 0.0, (double)i), crossColor, 1.6F, false);
      drawLine(current.add((double)x, 0.0, 0.0), current.add(0.0, 0.0, (double)i), crossColor, 1.6F, false);
      current = vec3d.add(0.0, 5.0, 0.0);
      drawQuad(
         current, current.add(0.0, 0.0, (double)(i * 2)), current.add((double)x, 0.0, (double)(i * 2)), current.add((double)x, 0.0, 0.0), fillColor, false
      );
      drawQuad(
         current.add((double)x, 0.0, 0.0), current.add((double)x, 0.0, (double)(i * 2)), current.add(0.0, 0.0, (double)(i * 2)), current, fillColor, false
      );
      drawLine(current, current.add((double)x, 0.0, (double)(i * 2)), crossColor, 1.6F, false);
      drawLine(current.add((double)x, 0.0, 0.0), current.add(0.0, 0.0, (double)(i * 2)), crossColor, 1.6F, false);

      for (int f = 0; f < 3; f++) {
         current = current.add((double)x, 0.0, (double)i);
         drawQuad(
            current, current.add(0.0, 0.0, (double)(i * 2)), current.add((double)x, 0.0, (double)(i * 2)), current.add((double)x, 0.0, 0.0), fillColor, false
         );
         drawQuad(
            current.add((double)x, 0.0, 0.0), current.add((double)x, 0.0, (double)(i * 2)), current.add(0.0, 0.0, (double)(i * 2)), current, fillColor, false
         );
         drawLine(current, current.add((double)x, 0.0, (double)(i * 2)), crossColor, 1.6F, false);
         drawLine(current.add((double)x, 0.0, 0.0), current.add(0.0, 0.0, (double)(i * 2)), crossColor, 1.6F, false);
      }

      current = current.add((double)x, 0.0, (double)i);
      drawQuad(current, current.add(0.0, 0.0, (double)i), current.add((double)x, 0.0, (double)i), current.add((double)x, 0.0, 0.0), fillColor, false);
      drawQuad(current.add((double)x, 0.0, 0.0), current.add((double)x, 0.0, (double)i), current.add(0.0, 0.0, (double)i), current, fillColor, false);
      drawLine(current, current.add((double)x, 0.0, (double)i), crossColor, 1.6F, false);
      drawLine(current.add((double)x, 0.0, 0.0), current.add(0.0, 0.0, (double)i), crossColor, 1.6F, false);
   }

   private static double lerp(double start, double end, double delta) {
      return start + (end - start) * delta;
   }

   public static void drawGradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4, boolean depth) {
      if (isFinite(p1) && isFinite(p2) && isFinite(p3) && isFinite(p4)) {
         Render3D.GradientQuad quad = new Render3D.GradientQuad(p1, p2, p3, p4, c1, c2, c3, c4);
         if (depth) {
            GRADIENT_QUAD_DEPTH.add(quad);
         } else {
            GRADIENT_QUAD.add(quad);
         }
      }
   }

   public static void drawLineGradient(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
      if (isFinite(start) && isFinite(end)) {
         Render3D.Line line = new Render3D.Line(null, start, end, colorStart, colorEnd, sanitizeLineWidth(width));
         if (depth) {
            LINE_DEPTH.add(line);
         } else {
            LINE.add(line);
         }
      }
   }

   public static Vector3f getNormal(Vector3f start, Vector3f end) {
      Vector3f normal = new Vector3f(start).sub(end);
      float sqrt = MathHelper.sqrt(normal.lengthSquared());
      return sqrt < 1.0E-4F ? new Vector3f(0.0F, 1.0F, 0.0F) : normal.div(sqrt);
   }

   private static Vector3f lineNormal(Vec3d start, Vec3d end) {
      float x = (float)(start.x - end.x);
      float y = (float)(start.y - end.y);
      float z = (float)(start.z - end.z);
      float length = MathHelper.sqrt(x * x + y * y + z * z);
      return length < 1.0E-4F ? LINE_NORMAL.set(0.0F, 1.0F, 0.0F) : LINE_NORMAL.set(x / length, y / length, z / length);
   }

   public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
      drawShape(blockPos, voxelShape, color, width, true, false);
   }

   public static void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
      List<Box> boxes = SHAPE_BOXES.computeIfAbsent(voxelShape, VoxelShape::getBoundingBoxes);
      boxes.forEach(box -> {
         Box offsetBox = box.offset(blockPos);
         drawBox(offsetBox, color, width, true, fill, depth);
      });
   }

   public static void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
      Vec3d vec3d = Vec3d.of(blockPos);
      Pair<List<Box>, List<Render3D.Line>> pair = SHAPE_OUTLINES.computeIfAbsent(
         voxelShape,
         shape -> {
            List<Render3D.Line> lines = new ArrayList<>();
            shape.forEachEdge(
               (minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Render3D.Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0.0F))
            );
            return new Pair(shape.getBoundingBoxes(), lines);
         }
      );
      if (fill) {
         ((List<Box>)pair.getLeft()).forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
      }

      ((List<Render3D.Line>)pair.getRight()).forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
   }

   public static void drawShapeOverlay(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
      Vec3d vec3d = Vec3d.of(blockPos);
      Pair<List<Box>, List<Render3D.Line>> pair = SHAPE_OUTLINES.computeIfAbsent(
         voxelShape,
         shape -> {
            List<Render3D.Line> lines = new ArrayList<>();
            shape.forEachEdge(
               (minX, minY, minZ, maxX, maxY, maxZ) -> lines.add(new Render3D.Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0.0F))
            );
            return new Pair(shape.getBoundingBoxes(), lines);
         }
      );
      ((List<Render3D.Line>)pair.getRight()).forEach(line -> drawLineOverlay(line.start.add(vec3d), line.end.add(vec3d), color, width));
   }

   public static void drawBox(Box box, int color, float width) {
      drawBox(box, color, width, true, true, false);
   }

   public static void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
      drawBox(null, box, color, width, line, fill, depth);
   }

   public static void drawBox(Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
      if (isFinite(box)) {
         double x1 = box.minX;
         double y1 = box.minY;
         double z1 = box.minZ;
         double x2 = box.maxX;
         double y2 = box.maxY;
         double z2 = box.maxZ;
         if (fill) {
            int fillColor = ColorUtil.multAlpha(color, 0.3F);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
         }

         if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
         }
      }
   }

   public static void drawBoxOverlay(Box box, int color, float width) {
      if (isFinite(box)) {
         double x1 = box.minX;
         double y1 = box.minY;
         double z1 = box.minZ;
         double x2 = box.maxX;
         double y2 = box.maxY;
         double z2 = box.maxZ;
         drawLineOverlay(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), color, width);
         drawLineOverlay(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), color, width);
         drawLineOverlay(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), color, width);
         drawLineOverlay(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), color, width);
         drawLineOverlay(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), color, width);
         drawLineOverlay(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), color, width);
         drawLineOverlay(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), color, width);
         drawLineOverlay(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), color, width);
         drawLineOverlay(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), color, width);
         drawLineOverlay(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), color, width);
         drawLineOverlay(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), color, width);
         drawLineOverlay(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), color, width);
      }
   }

   public static void drawLine(Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
      drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
   }

   public static void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
      drawLine(null, start, end, color, color, width, depth);
   }

   public static void drawLine(Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
      if (isFinite(start) && isFinite(end)) {
         Render3D.Line line = new Render3D.Line(entry, start, end, colorStart, colorEnd, sanitizeLineWidth(width));
         if (depth) {
            LINE_DEPTH.add(line);
         } else {
            LINE.add(line);
         }
      }
   }

   public static void drawLineOverlay(Vec3d start, Vec3d end, int color, float width) {
      drawLineOverlay(null, start, end, color, color, width);
   }

   public static void drawLineOverlay(Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
      if (isFinite(start) && isFinite(end)) {
         LINE_OVERLAY.add(new Render3D.Line(entry, start, end, colorStart, colorEnd, sanitizeLineWidth(width)));
      }
   }

   public static void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
      drawQuad(null, x, y, w, z, color, depth);
   }

   public static void drawQuad(Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
      if (isFinite(x) && isFinite(y) && isFinite(w) && isFinite(z)) {
         Render3D.Quad quad = new Render3D.Quad(entry, x, y, w, z, color);
         if (depth) {
            QUAD_DEPTH.add(quad);
         } else {
            QUAD.add(quad);
         }
      }
   }

   private static float sanitizeLineWidth(float width) {
      return !Float.isFinite(width) ? 1.0F : Math.clamp(width, 0.1F, 16.0F);
   }

   private static boolean isFinite(Vec3d vec) {
      return vec != null && isFinite(vec.x) && isFinite(vec.y) && isFinite(vec.z);
   }

   private static boolean isFinite(Box box) {
      return box != null && isFinite(box.minX) && isFinite(box.minY) && isFinite(box.minZ) && isFinite(box.maxX) && isFinite(box.maxY) && isFinite(box.maxZ);
   }

   private static boolean isFinite(double value) {
      return Double.isFinite(value) && Math.abs(value) <= 3.0E7;
   }

   public static void resetCircleSmoothing() {
      smoothY = 0.0;
      smoothY2 = 0.0;
   }

   public static record GradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4) {
   }

   public static record Line(Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
   }

   public static record Quad(Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
   }
}
