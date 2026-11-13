package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.color.ColorUtil;

final class TargetEspRibbon {
   private static final Vec3d UP = new Vec3d(0.0, 1.0, 0.0);

   private TargetEspRibbon() {
   }

   static void ribbon(MatrixStack stack, VertexConsumer consumer, Vec3d[] points, float[] widths, int[] colors, Vec3d cameraLocal) {
      if (points.length >= 2) {
         Entry entry = stack.peek();
         int last = points.length - 1;

         for (int i = 0; i < last; i++) {
            Vec3d sideA = sideVector(points, i, cameraLocal);
            Vec3d sideB = sideVector(points, i + 1, cameraLocal);
            float uA = (float)i / (float)last;
            float uB = (float)(i + 1) / (float)last;
            vertex(consumer, entry, points[i].add(sideA.multiply((double)widths[i])), uA, 0.0F, colors[i]);
            vertex(consumer, entry, points[i].subtract(sideA.multiply((double)widths[i])), uA, 1.0F, colors[i]);
            vertex(consumer, entry, points[i + 1].subtract(sideB.multiply((double)widths[i + 1])), uB, 1.0F, colors[i + 1]);
            vertex(consumer, entry, points[i + 1].add(sideB.multiply((double)widths[i + 1])), uB, 0.0F, colors[i + 1]);
         }
      }
   }

   static void sprites(
      MatrixStack stack, VertexConsumer consumer, Vec3d[] points, float[] widths, int[] colors, Vec3d cameraLocal, float widthScale, float alphaScale
   ) {
      Entry entry = stack.peek();

      for (int i = 0; i < points.length; i++) {
         Vec3d point = points[i];
         float size = widths[i] * widthScale;
         if (!(size <= 1.0E-4F)) {
            Vec3d toCamera = cameraLocal.subtract(point);
            if (!(toCamera.lengthSquared() < 1.0E-8)) {
               toCamera = toCamera.normalize();
               Vec3d right = UP.crossProduct(toCamera);
               if (right.lengthSquared() < 1.0E-8) {
                  right = new Vec3d(1.0, 0.0, 0.0);
               }

               right = right.normalize();
               Vec3d up = toCamera.crossProduct(right).normalize().multiply((double)size);
               right = right.multiply((double)size);
               int color = ColorUtil.multAlpha(colors[i], alphaScale);
               vertex(consumer, entry, point.subtract(right).subtract(up), 0.0F, 0.0F, color);
               vertex(consumer, entry, point.add(right).subtract(up), 1.0F, 0.0F, color);
               vertex(consumer, entry, point.add(right).add(up), 1.0F, 1.0F, color);
               vertex(consumer, entry, point.subtract(right).add(up), 0.0F, 1.0F, color);
            }
         }
      }
   }

   static Vec3d sideVector(Vec3d[] points, int index, Vec3d cameraLocal) {
      Vec3d previous = points[Math.max(0, index - 1)];
      Vec3d next = points[Math.min(points.length - 1, index + 1)];
      Vec3d direction = next.subtract(previous);
      if (direction.lengthSquared() < 1.0E-8) {
         direction = UP;
      }

      direction = direction.normalize();
      Vec3d toCamera = cameraLocal.subtract(points[index]);
      if (toCamera.lengthSquared() < 1.0E-8) {
         toCamera = UP;
      }

      Vec3d side = direction.crossProduct(toCamera.normalize());
      if (side.lengthSquared() < 1.0E-8) {
         side = direction.crossProduct(UP);
         if (side.lengthSquared() < 1.0E-8) {
            side = new Vec3d(1.0, 0.0, 0.0);
         }
      }

      return side.normalize();
   }

   static void vertex(VertexConsumer consumer, Entry entry, Vec3d position, float u, float v, int color) {
      consumer.vertex(entry, (float)position.x, (float)position.y, (float)position.z).texture(u, v).color(color);
   }
}
