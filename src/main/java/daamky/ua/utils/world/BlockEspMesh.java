package daamky.ua.utils.world;

import daamky.ua.utils.render.Render3D;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Builds translucent faces and silhouette edges for clusters of found blocks using only the standard
 * Render3D queues (no custom shaders, so it behaves the same on every renderer).
 * Direction index = axis * 2 + (positive ? 1 : 0): 0 -x, 1 +x, 2 -y, 3 +y, 4 -z, 5 +z.
 */
public final class BlockEspMesh {
   private static final int MAX_QUADS = 12000;
   private static final int MAX_LINES = 20000;
   private static final double EPS = 0.003;
   private static final float LINE_WIDTH = 1.5F;
   private static final int[] DX = new int[]{-1, 1, 0, 0, 0, 0};
   private static final int[] DY = new int[]{0, 0, -1, 1, 0, 0};
   private static final int[] DZ = new int[]{0, 0, 0, 0, -1, 1};

   private BlockEspMesh() {
   }

   private static float smooth(float edge0, float edge1, float x) {
      if (edge1 <= edge0) {
         return x < edge0 ? 0.0F : 1.0F;
      }

      float t = Math.max(0.0F, Math.min(1.0F, (x - edge0) / (edge1 - edge0)));
      return t * t * (3.0F - 2.0F * t);
   }

   public static void build(
      long[] pos, byte[] type, int count, Vec3d camera, float fadeStart, float fadeEnd, List<Render3D.Quad> quads, List<Render3D.Line> lines
   ) {
      Long2ByteOpenHashMap map = new Long2ByteOpenHashMap(Math.max(16, count * 2));
      map.defaultReturnValue((byte)-1);
      for (int i = 0; i < count; i++) {
         map.put(pos[i], type[i]);
      }

      int[] base = new int[3];
      int[] cell = new int[3];
      for (int i = 0; i < count; i++) {
         if (quads.size() >= MAX_QUADS && lines.size() >= MAX_LINES) {
            return;
         }

         int x = BlockPos.unpackLongX(pos[i]);
         int y = BlockPos.unpackLongY(pos[i]);
         int z = BlockPos.unpackLongZ(pos[i]);
         byte t = type[i];
         double dx = x + 0.5 - camera.x;
         double dy = y + 0.5 - camera.y;
         double dz = z + 0.5 - camera.z;
         float dist = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
         float fade = 1.0F - smooth(fadeStart, fadeEnd, dist);
         if (fade <= 0.02F) {
            continue;
         }

         float near = smooth(0.0F, 1.5F, dist);
         int rgb = BlockEspTypes.COLORS[t] & 0xFFFFFF;
         int fillColor = (int)(46.0F * fade * near) << 24 | rgb;
         int lineColor = (int)(235.0F * fade) << 24 | rgb;
         base[0] = x;
         base[1] = y;
         base[2] = z;
         for (int face = 0; face < 6; face++) {
            if (map.get(BlockPos.asLong(x + DX[face], y + DY[face], z + DZ[face])) == t) {
               continue;
            }

            if (quads.size() < MAX_QUADS) {
               addFace(quads, face, x, y, z, fillColor);
            }

            if (lines.size() >= MAX_LINES) {
               continue;
            }

            int axis = face >> 1;
            for (int edge = 0; edge < 6; edge++) {
               int edgeAxis = edge >> 1;
               if (edgeAxis == axis) {
                  continue;
               }

               boolean sideSolid = map.get(BlockPos.asLong(x + DX[edge], y + DY[edge], z + DZ[edge])) == t;
               boolean cornerSolid = map.get(BlockPos.asLong(x + DX[edge] + DX[face], y + DY[edge] + DY[face], z + DZ[edge] + DZ[face])) == t;
               if (!cornerSolid && sideSolid) {
                  continue;
               }

               int other = cornerSolid ? edge ^ 1 : edge;
               if (face > other) {
                  continue;
               }

               int third = 3 - axis - edgeAxis;
               boolean facePositive = (face & 1) == 1;
               boolean edgePositive = (edge & 1) == 1;
               cell[axis] = base[axis] + (facePositive ? 1 : 0);
               cell[edgeAxis] = base[edgeAxis] + (edgePositive ? 1 : 0);
               double[] a = new double[3];
               double[] b = new double[3];
               a[axis] = cell[axis] + (facePositive ? EPS : -EPS);
               b[axis] = a[axis];
               a[edgeAxis] = cell[edgeAxis];
               b[edgeAxis] = cell[edgeAxis];
               a[third] = base[third];
               b[third] = base[third] + 1;
               lines.add(new Render3D.Line(null, new Vec3d(a[0], a[1], a[2]), new Vec3d(b[0], b[1], b[2]), lineColor, lineColor, LINE_WIDTH));
            }
         }
      }
   }

   private static void addFace(List<Render3D.Quad> quads, int face, int bx, int by, int bz, int color) {
      double x1 = bx;
      double y1 = by;
      double z1 = bz;
      double x2 = bx + 1;
      double y2 = by + 1;
      double z2 = bz + 1;
      Vec3d a;
      Vec3d b;
      Vec3d c;
      Vec3d d;
      switch (face) {
         case 0 -> {
            double px = x1 - EPS;
            a = new Vec3d(px, y1, z1);
            b = new Vec3d(px, y1, z2);
            c = new Vec3d(px, y2, z2);
            d = new Vec3d(px, y2, z1);
         }
         case 1 -> {
            double px = x2 + EPS;
            a = new Vec3d(px, y1, z1);
            b = new Vec3d(px, y2, z1);
            c = new Vec3d(px, y2, z2);
            d = new Vec3d(px, y1, z2);
         }
         case 2 -> {
            double py = y1 - EPS;
            a = new Vec3d(x1, py, z1);
            b = new Vec3d(x2, py, z1);
            c = new Vec3d(x2, py, z2);
            d = new Vec3d(x1, py, z2);
         }
         case 3 -> {
            double py = y2 + EPS;
            a = new Vec3d(x1, py, z1);
            b = new Vec3d(x1, py, z2);
            c = new Vec3d(x2, py, z2);
            d = new Vec3d(x2, py, z1);
         }
         case 4 -> {
            double pz = z1 - EPS;
            a = new Vec3d(x1, y1, pz);
            b = new Vec3d(x1, y2, pz);
            c = new Vec3d(x2, y2, pz);
            d = new Vec3d(x2, y1, pz);
         }
         default -> {
            double pz = z2 + EPS;
            a = new Vec3d(x1, y1, pz);
            b = new Vec3d(x2, y1, pz);
            c = new Vec3d(x2, y2, pz);
            d = new Vec3d(x1, y2, pz);
         }
      }

      quads.add(new Render3D.Quad(null, a, b, c, d, color));
   }
}
