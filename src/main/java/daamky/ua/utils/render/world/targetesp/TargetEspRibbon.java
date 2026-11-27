package daamky.ua.utils.render.world.targetesp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import daamky.ua.utils.render.color.ColorUtil;
import net.minecraft.world.phys.Vec3;

final class TargetEspRibbon {
   private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

   private TargetEspRibbon() {
   }

   static void ribbon(PoseStack stack, VertexConsumer consumer, Vec3[] points, float[] widths, int[] colors, Vec3 cameraLocal) {
      if (points.length >= 2) {
         Pose entry = stack.last();
         int last = points.length - 1;

         for (int i = 0; i < last; i++) {
            Vec3 sideA = sideVector(points, i, cameraLocal);
            Vec3 sideB = sideVector(points, i + 1, cameraLocal);
            float uA = (float)i / (float)last;
            float uB = (float)(i + 1) / (float)last;
            vertex(consumer, entry, points[i].add(sideA.scale((double)widths[i])), uA, 0.0F, colors[i]);
            vertex(consumer, entry, points[i].subtract(sideA.scale((double)widths[i])), uA, 1.0F, colors[i]);
            vertex(consumer, entry, points[i + 1].subtract(sideB.scale((double)widths[i + 1])), uB, 1.0F, colors[i + 1]);
            vertex(consumer, entry, points[i + 1].add(sideB.scale((double)widths[i + 1])), uB, 0.0F, colors[i + 1]);
         }
      }
   }

   static void sprites(
      PoseStack stack, VertexConsumer consumer, Vec3[] points, float[] widths, int[] colors, Vec3 cameraLocal, float widthScale, float alphaScale
   ) {
      Pose entry = stack.last();

      for (int i = 0; i < points.length; i++) {
         Vec3 point = points[i];
         float size = widths[i] * widthScale;
         if (!(size <= 1.0E-4F)) {
            Vec3 toCamera = cameraLocal.subtract(point);
            if (!(toCamera.lengthSqr() < 1.0E-8)) {
               toCamera = toCamera.normalize();
               Vec3 right = UP.cross(toCamera);
               if (right.lengthSqr() < 1.0E-8) {
                  right = new Vec3(1.0, 0.0, 0.0);
               }

               right = right.normalize();
               Vec3 up = toCamera.cross(right).normalize().scale((double)size);
               right = right.scale((double)size);
               int color = ColorUtil.multAlpha(colors[i], alphaScale);
               vertex(consumer, entry, point.subtract(right).subtract(up), 0.0F, 0.0F, color);
               vertex(consumer, entry, point.add(right).subtract(up), 1.0F, 0.0F, color);
               vertex(consumer, entry, point.add(right).add(up), 1.0F, 1.0F, color);
               vertex(consumer, entry, point.subtract(right).add(up), 0.0F, 1.0F, color);
            }
         }
      }
   }

   static Vec3 sideVector(Vec3[] points, int index, Vec3 cameraLocal) {
      Vec3 previous = points[Math.max(0, index - 1)];
      Vec3 next = points[Math.min(points.length - 1, index + 1)];
      Vec3 direction = next.subtract(previous);
      if (direction.lengthSqr() < 1.0E-8) {
         direction = UP;
      }

      direction = direction.normalize();
      Vec3 toCamera = cameraLocal.subtract(points[index]);
      if (toCamera.lengthSqr() < 1.0E-8) {
         toCamera = UP;
      }

      Vec3 side = direction.cross(toCamera.normalize());
      if (side.lengthSqr() < 1.0E-8) {
         side = direction.cross(UP);
         if (side.lengthSqr() < 1.0E-8) {
            side = new Vec3(1.0, 0.0, 0.0);
         }
      }

      return side.normalize();
   }

   static void vertex(VertexConsumer consumer, Pose entry, Vec3 position, float u, float v, int color) {
      consumer.addVertex(entry, (float)position.x, (float)position.y, (float)position.z).setUv(u, v).setColor(color);
   }
}
