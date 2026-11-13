package daamky.ua.screens.modernui.impl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RawProjectionMatrix;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import daamky.ua.screens.modernui.ClickGuiScreen;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.ui.Render2D;

public final class WorldAnimation {
   private static final double DISTANCE = 7.5;
   private static final float NEAR_PLANE = 0.05F;
   private static final float CLOSE_STEP = 0.08F;
   private static final float WORLD_SCALE = 0.016F;
   private static final float GUI_RENDERER_Z_OFFSET = 11000.0F;
   private static WorldAnimation.State state;
   private static RawProjectionMatrix projectionBuffer;
   private static boolean renderingDetached;
   private static GpuBufferSlice projectionOverride;

   private WorldAnimation() {
   }

   public static void start(ClickGuiScreen screen, float previousValue, float value) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.world != null && client.player != null && screen != null) {
         Camera camera = client.gameRenderer.getCamera();
         float yaw = camera.getYaw();
         float pitch = camera.getPitch();
         Vec3d look = lookVector(yaw, pitch);
         double offsetDistance = 7.5 * (double)client.getWindow().getFramebufferHeight() / 1080.0;
         Vec3d anchor = camera.getCameraPos().add(look.x * offsetDistance, look.y * offsetDistance, look.z * offsetDistance);
         float fixedWidth = (float)Render2D.getFixedScaledWidth();
         float fixedHeight = (float)Render2D.getFixedScaledHeight();
         state = new WorldAnimation.State(
            screen, anchor, yaw, pitch, resolveWorldScale(anchor, yaw, pitch, fixedWidth, fixedHeight), clamp01(previousValue), clamp01(value), true
         );
      } else {
         stop();
      }
   }

   public static void stop() {
      state = null;
      renderingDetached = false;
      projectionOverride = null;
   }

   public static boolean isActive() {
      return state != null;
   }

   public static boolean isRenderingDetached() {
      return renderingDetached;
   }

   public static void tick() {
      WorldAnimation.State current = state;
      if (current != null) {
         current.previousValue = current.value;
         if (current.skipNextTick) {
            current.skipNextTick = false;
         } else {
            current.value = Math.max(0.0F, current.value - 0.08F);
            if (current.value == 0.0F && current.previousValue == 0.0F) {
               stop();
            }
         }
      }
   }

   public static GpuBufferSlice projectionOverride() {
      return projectionOverride;
   }

   public static void render(GuiRenderer renderer, DrawContext graphics, GpuBufferSlice fogBuffer, float partialTicks) {
      WorldAnimation.State current = state;
      if (current != null && renderer != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client == null || client.world == null || client.player == null || client.options.hudHidden) {
            stop();
         } else if (client.currentScreen != null) {
            stop();
         } else {
            float fixedWidth = (float)Render2D.getFixedScaledWidth();
            float fixedHeight = (float)Render2D.getFixedScaledHeight();
            float progress = interpolate(current.previousValue, current.value, partialTicks);
            Matrix4f projection = createDetachedProjection(current, progress, fixedWidth, fixedHeight);
            if (projection != null) {
               WorldAnimation.MatrixProjector projector = new WorldAnimation.MatrixProjector(projection, fixedWidth, fixedHeight);
               if (isPanelVisible(projector, fixedWidth, fixedHeight)) {
                  current.screen.renderDetachedBackground(graphics, progress, fixedWidth, fixedHeight);
                  renderer.render(fogBuffer);
                  renderingDetached = true;
                  projectionOverride = projectionBuffer(new Matrix4f(projection).translate(0.0F, 0.0F, 11000.0F));
                  RenderSystem.backupProjectionMatrix();

                  try {
                     Render2D.withProjection(projection, projector, () -> current.screen.renderDetached(graphics, partialTicks));
                     renderer.render(fogBuffer);
                  } finally {
                     RenderSystem.restoreProjectionMatrix();
                     projectionOverride = null;
                     renderingDetached = false;
                  }
               }
            }
         }
      }
   }

   private static GpuBufferSlice projectionBuffer(Matrix4f projection) {
      if (projectionBuffer == null) {
         projectionBuffer = new RawProjectionMatrix("daamky_modern_ui_world");
      }

      return projectionBuffer.set(projection);
   }

   private static Matrix4f createDetachedProjection(WorldAnimation.State state, float progress, float fixedWidth, float fixedHeight) {
      float animationScale = scale(progress);
      if (animationScale <= 0.001F) {
         return null;
      } else {
         Vec3d cameraPos = Render3D.lastCameraPos;
         float centerX = fixedWidth * 0.5F;
         float centerY = fixedHeight * 0.5F;
         float relativeX = (float)(state.anchor.x - cameraPos.x);
         float relativeY = (float)(state.anchor.y - cameraPos.y);
         float relativeZ = (float)(state.anchor.z - cameraPos.z);
         Matrix4f model = new Matrix4f()
            .translate(relativeX, relativeY, relativeZ)
            .rotateY((float)Math.toRadians((double)(-state.yaw + 180.0F)))
            .rotateX((float)Math.toRadians((double)(-state.pitch + 180.0F)))
            .scale(state.worldScale, state.worldScale, state.worldScale)
            .scale(animationScale, animationScale, 1.0F)
            .translate(-centerX, -centerY, 0.0F);
         return new Matrix4f(Render3D.lastProjMat).mul(Render3D.lastModMat).mul(model);
      }
   }

   private static float resolveWorldScale(Vec3d anchor, float yaw, float pitch, float fixedWidth, float fixedHeight) {
      float centerX = fixedWidth * 0.5F;
      float centerY = fixedHeight * 0.5F;
      Vec3d cameraPos = Render3D.lastCameraPos;
      float relativeX = (float)(anchor.x - cameraPos.x);
      float relativeY = (float)(anchor.y - cameraPos.y);
      float relativeZ = (float)(anchor.z - cameraPos.z);
      Matrix4f unitProjection = new Matrix4f(Render3D.lastProjMat)
         .mul(Render3D.lastModMat)
         .mul(
            new Matrix4f()
               .translate(relativeX, relativeY, relativeZ)
               .rotateY((float)Math.toRadians((double)(-yaw + 180.0F)))
               .rotateX((float)Math.toRadians((double)(-pitch + 180.0F)))
               .translate(-centerX, -centerY, 0.0F)
         );
      WorldAnimation.MatrixProjector projector = new WorldAnimation.MatrixProjector(unitProjection, fixedWidth, fixedHeight);
      Render2D.ProjectedPoint left = projector.project(0.0F, centerY);
      Render2D.ProjectedPoint right = projector.project(fixedWidth, centerY);
      Render2D.ProjectedPoint top = projector.project(centerX, 0.0F);
      Render2D.ProjectedPoint bottom = projector.project(centerX, fixedHeight);
      float widthScale = Float.NaN;
      if (left != null && right != null) {
         widthScale = fixedWidth / Math.max(1.0E-4F, Math.abs(right.x() - left.x()));
      }

      float heightScale = Float.NaN;
      if (top != null && bottom != null) {
         heightScale = fixedHeight / Math.max(1.0E-4F, Math.abs(bottom.y() - top.y()));
      }

      float scaleSum = 0.0F;
      int scaleCount = 0;
      if (Float.isFinite(widthScale) && widthScale > 0.0F) {
         scaleSum += widthScale;
         scaleCount++;
      }

      if (Float.isFinite(heightScale) && heightScale > 0.0F) {
         scaleSum += heightScale;
         scaleCount++;
      }

      return scaleCount == 0 ? 0.016F : scaleSum / (float)scaleCount;
   }

   private static boolean isPanelVisible(WorldAnimation.MatrixProjector projector, float fixedWidth, float fixedHeight) {
      return projector.project(0.0F, 0.0F) != null
         || projector.project(fixedWidth, 0.0F) != null
         || projector.project(fixedWidth, fixedHeight) != null
         || projector.project(0.0F, fixedHeight) != null;
   }

   private static Vec3d lookVector(float yaw, float pitch) {
      double yawRadians = Math.toRadians((double)yaw);
      double pitchRadians = Math.toRadians((double)pitch);
      double pitchCos = Math.cos(pitchRadians);
      return new Vec3d(-Math.sin(yawRadians) * pitchCos, -Math.sin(pitchRadians), Math.cos(yawRadians) * pitchCos);
   }

   private static float scale(float progress) {
      return Math.max(0.001F, easeOutBack(clamp01(progress)));
   }

   private static float interpolate(float previous, float current, float partialTicks) {
      float clampedPartial = clamp01(partialTicks);
      return previous + (current - previous) * clampedPartial;
   }

   private static float clamp01(float value) {
      return Math.max(0.0F, Math.min(1.0F, value));
   }

   private static float easeOutBack(float value) {
      float overshoot = 1.70158F;
      float shifted = value - 1.0F;
      return 1.0F + (overshoot + 1.0F) * shifted * shifted * shifted + overshoot * shifted * shifted;
   }

   private static final class MatrixProjector implements Render2D.PointProjector {
      private final Matrix4f matrix;
      private final float fixedWidth;
      private final float fixedHeight;

      private MatrixProjector(Matrix4f matrix, float fixedWidth, float fixedHeight) {
         this.matrix = matrix;
         this.fixedWidth = fixedWidth;
         this.fixedHeight = fixedHeight;
      }

      @Override
      public Render2D.ProjectedPoint project(float x, float y) {
         float clipX = this.matrix.m00() * x + this.matrix.m10() * y + this.matrix.m30();
         float clipY = this.matrix.m01() * x + this.matrix.m11() * y + this.matrix.m31();
         float clipW = this.matrix.m03() * x + this.matrix.m13() * y + this.matrix.m33();
         if (clipW <= 0.05F) {
            return null;
         } else {
            float inverseW = 1.0F / clipW;
            float ndcX = clipX * inverseW;
            float ndcY = clipY * inverseW;
            return new Render2D.ProjectedPoint((ndcX * 0.5F + 0.5F) * this.fixedWidth, (1.0F - (ndcY * 0.5F + 0.5F)) * this.fixedHeight);
         }
      }
   }

   private static final class State {
      private final ClickGuiScreen screen;
      private final Vec3d anchor;
      private final float yaw;
      private final float pitch;
      private final float worldScale;
      private float previousValue;
      private float value;
      private boolean skipNextTick;

      private State(ClickGuiScreen screen, Vec3d anchor, float yaw, float pitch, float worldScale, float previousValue, float value, boolean skipNextTick) {
         this.screen = screen;
         this.anchor = anchor;
         this.yaw = yaw;
         this.pitch = pitch;
         this.worldScale = worldScale;
         this.previousValue = previousValue;
         this.value = value;
         this.skipNextTick = skipNextTick;
      }
   }
}
