package daamky.ua.api.module.impl.visual.esp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import daamky.ua.utils.render.Render3D;

public final class EspGeometry {
   private static final double NEAR_PLANE = 0.05;
   private static final double MAX_SAFE_COORD = 3.0E7;
   private static final int[][] AABB_EDGES = new int[][]{{0, 1}, {0, 2}, {0, 4}, {1, 3}, {1, 5}, {2, 3}, {2, 6}, {3, 7}, {4, 5}, {4, 6}, {5, 7}, {6, 7}};

   private EspGeometry() {
   }

   public static EspGeometry.ProjectionContext createProjectionContext(MinecraftClient mc, float tickDelta) {
      if (mc.gameRenderer != null && mc.getWindow() != null) {
         Camera camera = mc.gameRenderer.getCamera();
         if (camera != null && camera.isReady()) {
            Vec3d cameraPos = isFinite(Render3D.lastCameraPos) ? Render3D.lastCameraPos : camera.getCameraPos();
            float resolvedTickDelta = Float.isFinite(tickDelta) ? tickDelta : Render3D.lastTickDelta;
            MatrixStack stack = new MatrixStack();
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            return new EspGeometry.ProjectionContext(
               cameraPos,
               new Matrix4f(Render3D.lastProjMat),
               new Matrix3f(stack.peek().getPositionMatrix()),
               (float)mc.getWindow().getScaledWidth(),
               (float)mc.getWindow().getScaledHeight(),
               resolvedTickDelta,
               createCorners(),
               new Vector3f(),
               new Vector3f(),
               new Vector4f(),
               new double[4]
            );
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   public static Box interpolatedBox(Entity entity, float tickDelta) {
      Vec3d pos = entity.getLerpedPos(tickDelta);
      return entity.getDimensions(entity.getPose()).getBoxAt(pos.x, pos.y, pos.z);
   }

   public static EspGeometry.ScreenBox projectEntityBox(Entity entity, EspGeometry.ProjectionContext context) {
      Box box = interpolatedBox(entity, context.tickDelta()).expand(0.025, 0.0, 0.025);
      box = new Box(box.minX, box.minY, box.minZ, box.maxX, box.maxY + 0.05, box.maxZ);
      return projectBox(box, context);
   }

   public static EspGeometry.ScreenBox projectBox(Box box, EspGeometry.ProjectionContext context) {
      if (box != null && context != null) {
         fillViewSpaceCorners(box, context);
         double[] bounds = context.bounds();
         bounds[0] = Double.POSITIVE_INFINITY;
         bounds[1] = Double.POSITIVE_INFINITY;
         bounds[2] = Double.NEGATIVE_INFINITY;
         bounds[3] = Double.NEGATIVE_INFINITY;
         int projected = 0;

         for (Vector3f corner : context.corners()) {
            projected += accumulateProjectedBounds(bounds, corner, context);
         }

         for (int[] edge : AABB_EDGES) {
            projected += accumulateClippedEdgeBounds(bounds, context.corners()[edge[0]], context.corners()[edge[1]], context);
         }

         if (projected <= 0) {
            return null;
         } else {
            double minX = bounds[0];
            double minY = bounds[1];
            double maxX = bounds[2];
            double maxY = bounds[3];
            if (Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(maxX) && Double.isFinite(maxY)) {
               return !(maxX <= minX) && !(maxY <= minY) && !(maxX - minX < 1.0) && !(maxY - minY < 1.0)
                  ? new EspGeometry.ScreenBox(minX, minY, maxX, maxY)
                  : null;
            } else {
               return null;
            }
         }
      } else {
         return null;
      }
   }

   public static EspGeometry.ScreenAnchor resolveTopAnchor(Entity entity, EspGeometry.ProjectionContext context, EspGeometry.ScreenBox fallbackBox) {
      Vec3d interpolated = entity.getLerpedPos(context.tickDelta());
      double height = (double)entity.getDimensions(entity.getPose()).height();
      double topOffset = entity instanceof PlayerEntity ? 0.38 : 0.06;
      return resolveTopAnchor(interpolated, height, topOffset, context, fallbackBox);
   }

   public static EspGeometry.ScreenAnchor resolveTopAnchor(
      Vec3d interpolated, double height, double topOffset, EspGeometry.ProjectionContext context, EspGeometry.ScreenBox fallbackBox
   ) {
      if (!isFinite(interpolated)) {
         return fallbackBox != null ? new EspGeometry.ScreenAnchor(fallbackBox.centerX(), fallbackBox.minY()) : null;
      } else {
         EspGeometry.ScreenPoint projected = worldSpaceToScreenSpace(interpolated.x, interpolated.y + height + topOffset, interpolated.z, context);
         if (isUsableAnchorPoint(projected, context)) {
            double marginX = (double)context.screenWidth() * 0.45;
            double marginY = (double)context.screenHeight() * 0.45;
            return new EspGeometry.ScreenAnchor(
               MathHelper.clamp(projected.x(), -marginX, (double)context.screenWidth() + marginX),
               MathHelper.clamp(projected.y(), -marginY, (double)context.screenHeight() + marginY)
            );
         } else {
            return fallbackBox != null ? new EspGeometry.ScreenAnchor(fallbackBox.centerX(), fallbackBox.minY()) : null;
         }
      }
   }

   public static boolean isOnScreen(EspGeometry.ScreenBox box, EspGeometry.ProjectionContext context) {
      double marginX = (double)context.screenWidth() * 0.35;
      double marginY = (double)context.screenHeight() * 0.35;
      return box.maxX() >= -marginX
         && box.minX() <= (double)context.screenWidth() + marginX
         && box.maxY() >= -marginY
         && box.minY() <= (double)context.screenHeight() + marginY;
   }

   public static boolean isFinite(Vec3d pos) {
      return pos != null
         && Double.isFinite(pos.x)
         && Double.isFinite(pos.y)
         && Double.isFinite(pos.z)
         && Math.abs(pos.x) < 3.0E7
         && Math.abs(pos.y) < 3.0E7
         && Math.abs(pos.z) < 3.0E7;
   }

   public static EspGeometry.ScreenPoint projectWorldToScreenSpace(Vec3d worldPos, EspGeometry.ProjectionContext context) {
      return !isFinite(worldPos) ? null : projectWorldToScreenSpace(worldPos.x, worldPos.y, worldPos.z, context);
   }

   public static EspGeometry.ScreenPoint projectWorldToScreenSpace(double worldX, double worldY, double worldZ, EspGeometry.ProjectionContext context) {
      return context == null ? null : worldSpaceToScreenSpace(worldX, worldY, worldZ, context);
   }

   private static EspGeometry.ScreenPoint worldSpaceToScreenSpace(double worldX, double worldY, double worldZ, EspGeometry.ProjectionContext context) {
      Vector3f view = toCameraViewSpace(worldX, worldY, worldZ, context, context.pointScratch());
      if (!isVisibleViewPoint(view)) {
         return null;
      } else {
         Vector4f clip = context.projection().transform(context.clipScratch4().set(view.x, view.y, view.z, 1.0F));
         if (clip.w <= 1.0E-6F) {
            return null;
         } else {
            float invW = 1.0F / clip.w;
            float ndcX = clip.x * invW;
            float ndcY = clip.y * invW;
            float ndcZ = clip.z * invW;
            if (Float.isFinite(ndcX) && Float.isFinite(ndcY) && Float.isFinite(ndcZ)) {
               double screenX = ((double)ndcX * 0.5 + 0.5) * (double)context.screenWidth();
               double screenY = (1.0 - ((double)ndcY * 0.5 + 0.5)) * (double)context.screenHeight();
               double screenZ = (double)ndcZ * 0.5 + 0.5;
               return Double.isFinite(screenX) && Double.isFinite(screenY) && Double.isFinite(screenZ)
                  ? new EspGeometry.ScreenPoint(screenX, screenY, screenZ)
                  : null;
            } else {
               return null;
            }
         }
      }
   }

   private static boolean isUsableAnchorPoint(EspGeometry.ScreenPoint point, EspGeometry.ProjectionContext context) {
      if (point != null && !(point.z() < 0.0) && !(point.z() > 1.0)) {
         double marginX = (double)context.screenWidth() * 0.5;
         double marginY = (double)context.screenHeight() * 0.5;
         return point.x() >= -marginX
            && point.x() <= (double)context.screenWidth() + marginX
            && point.y() >= -marginY
            && point.y() <= (double)context.screenHeight() + marginY;
      } else {
         return false;
      }
   }

   private static void fillViewSpaceCorners(Box box, EspGeometry.ProjectionContext context) {
      toCameraViewSpace(box.minX, box.minY, box.minZ, context, context.corners()[0]);
      toCameraViewSpace(box.maxX, box.minY, box.minZ, context, context.corners()[1]);
      toCameraViewSpace(box.minX, box.maxY, box.minZ, context, context.corners()[2]);
      toCameraViewSpace(box.maxX, box.maxY, box.minZ, context, context.corners()[3]);
      toCameraViewSpace(box.minX, box.minY, box.maxZ, context, context.corners()[4]);
      toCameraViewSpace(box.maxX, box.minY, box.maxZ, context, context.corners()[5]);
      toCameraViewSpace(box.minX, box.maxY, box.maxZ, context, context.corners()[6]);
      toCameraViewSpace(box.maxX, box.maxY, box.maxZ, context, context.corners()[7]);
   }

   private static Vector3f toCameraViewSpace(double worldX, double worldY, double worldZ, EspGeometry.ProjectionContext context, Vector3f dest) {
      dest.set((float)(worldX - context.cameraPos().x), (float)(worldY - context.cameraPos().y), (float)(worldZ - context.cameraPos().z));
      context.viewRotation().transform(dest);
      return dest;
   }

   private static int accumulateProjectedBounds(double[] bounds, Vector3f viewPos, EspGeometry.ProjectionContext context) {
      if (!isVisibleViewPoint(viewPos)) {
         return 0;
      } else {
         return projectViewPointToBounds(viewPos.x, viewPos.y, viewPos.z, context, bounds) ? 1 : 0;
      }
   }

   private static int accumulateClippedEdgeBounds(double[] bounds, Vector3f start, Vector3f end, EspGeometry.ProjectionContext context) {
      boolean startVisible = isVisibleViewPoint(start);
      boolean endVisible = isVisibleViewPoint(end);
      if (startVisible == endVisible) {
         return 0;
      } else {
         Vector3f clippedPoint = clipEdgeToNearPlane(start, end, context.clipScratch());
         if (clippedPoint == null) {
            return 0;
         } else {
            return projectViewPointToBounds(clippedPoint.x, clippedPoint.y, clippedPoint.z, context, bounds) ? 1 : 0;
         }
      }
   }

   private static Vector3f clipEdgeToNearPlane(Vector3f start, Vector3f end, Vector3f dest) {
      float deltaZ = end.z - start.z;
      if (Math.abs(deltaZ) <= 1.0E-6F) {
         return null;
      } else {
         float planeZ = -0.05F;
         float t = (planeZ - start.z) / deltaZ;
         return Float.isFinite(t) && !(t < 0.0F) && !(t > 1.0F)
            ? dest.set(MathHelper.lerp(t, start.x, end.x), MathHelper.lerp(t, start.y, end.y), planeZ - 0.001F)
            : null;
      }
   }

   private static boolean projectViewPointToBounds(float viewX, float viewY, float viewZ, EspGeometry.ProjectionContext context, double[] bounds) {
      Vector4f clip = context.projection().transform(context.clipScratch4().set(viewX, viewY, viewZ, 1.0F));
      if (clip.w <= 1.0E-6F) {
         return false;
      } else {
         float invW = 1.0F / clip.w;
         float ndcX = clip.x * invW;
         float ndcY = clip.y * invW;
         float ndcZ = clip.z * invW;
         if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY) || !Float.isFinite(ndcZ)) {
            return false;
         } else if (!(ndcZ < -1.0F) && !(ndcZ > 1.0F)) {
            double screenX = ((double)ndcX * 0.5 + 0.5) * (double)context.screenWidth();
            double screenY = (1.0 - ((double)ndcY * 0.5 + 0.5)) * (double)context.screenHeight();
            if (Double.isFinite(screenX) && Double.isFinite(screenY)) {
               expandBounds(bounds, screenX, screenY, context.screenWidth(), context.screenHeight());
               return true;
            } else {
               return false;
            }
         } else {
            return false;
         }
      }
   }

   private static void expandBounds(double[] bounds, double x, double y, float screenWidth, float screenHeight) {
      double clampedX = MathHelper.clamp(x, (double)(-screenWidth), (double)screenWidth * 2.0);
      double clampedY = MathHelper.clamp(y, (double)(-screenHeight), (double)screenHeight * 2.0);
      bounds[0] = Math.min(bounds[0], clampedX);
      bounds[1] = Math.min(bounds[1], clampedY);
      bounds[2] = Math.max(bounds[2], clampedX);
      bounds[3] = Math.max(bounds[3], clampedY);
   }

   private static boolean isVisibleViewPoint(Vector3f viewPos) {
      return viewPos != null && Float.isFinite(viewPos.x) && Float.isFinite(viewPos.y) && Float.isFinite(viewPos.z) && (double)(-viewPos.z) > 0.05;
   }

   private static Vector3f[] createCorners() {
      Vector3f[] result = new Vector3f[8];

      for (int i = 0; i < result.length; i++) {
         result[i] = new Vector3f();
      }

      return result;
   }

   public static record ProjectionContext(
      Vec3d cameraPos,
      Matrix4f projection,
      Matrix3f viewRotation,
      float screenWidth,
      float screenHeight,
      float tickDelta,
      Vector3f[] corners,
      Vector3f pointScratch,
      Vector3f clipScratch,
      Vector4f clipScratch4,
      double[] bounds
   ) {
   }

   public static record ScreenAnchor(double x, double y) {
   }

   public static record ScreenBox(double minX, double minY, double maxX, double maxY) {
      public double width() {
         return this.maxX - this.minX;
      }

      public double height() {
         return this.maxY - this.minY;
      }

      public double centerX() {
         return (this.minX + this.maxX) * 0.5;
      }
   }

   public static record ScreenPoint(double x, double y, double z) {
   }
}
