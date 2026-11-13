package daamky.ua.utils.render.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2f;
import daamky.ua.screens.mainmenu.VanillaMenuTheme;

public final class Render2DCoordinateSpace {
   private static final float DESIGN_GUI_SCALE = 2.0F;
   private static final ThreadLocal<Render2DCoordinateSpace.PoseCache> POSE_CACHE = ThreadLocal.withInitial(Render2DCoordinateSpace.PoseCache::new);

   private Render2DCoordinateSpace() {
   }

   public static Matrix3x2f pose(DrawContext graphics) {
      Matrix3x2f source = graphics.getMatrices();
      float scale = guiIndependentScale();
      Render2DCoordinateSpace.PoseCache cache = POSE_CACHE.get();
      if (cache.matches(graphics, source, scale)) {
         return cache.pose;
      } else {
         Matrix3x2f pose = new Matrix3x2f(source);
         if (scale != 1.0F) {
            pose.scale(scale);
         }

         cache.set(graphics, source, scale, pose);
         return pose;
      }
   }

   public static void applyGuiScaleIndependence(Matrix3x2f pose) {
      float scale = guiIndependentScale();
      if (scale != 1.0F) {
         pose.scale(scale);
      }
   }

   public static float toGui(float value) {
      return value * guiIndependentScale();
   }

   public static int toGuiInt(float value) {
      return Math.round(toGui(value));
   }

   public static float guiIndependentScale() {
      // Themed vanilla screens (singleplayer, alt manager, direct connect, add server)
      // must follow the real GUI scale so drawing and widget hitboxes always match.
      if (usesRealGuiScale()) {
         return 1.0F;
      }

      return 2.0F / (float)guiScale();
   }

   public static float designGuiScale() {
      return usesRealGuiScale() ? (float)guiScale() : 2.0F;
   }

   private static boolean usesRealGuiScale() {
      return VanillaMenuTheme.active();
   }

   public static int guiScale() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft == null) {
         return 1;
      } else {
         Window window = minecraft.getWindow();
         return window == null ? 1 : Math.max(1, window.getScaleFactor());
      }
   }

   private static final class PoseCache {
      private DrawContext graphics;
      private Matrix3x2f pose;
      private float scale;
      private float m00;
      private float m01;
      private float m10;
      private float m11;
      private float m20;
      private float m21;

      private PoseCache() {
      }

      private boolean matches(DrawContext graphics, Matrix3x2f source, float scale) {
         return this.graphics == graphics
            && this.pose != null
            && this.scale == scale
            && this.m00 == source.m00()
            && this.m01 == source.m01()
            && this.m10 == source.m10()
            && this.m11 == source.m11()
            && this.m20 == source.m20()
            && this.m21 == source.m21();
      }

      private void set(DrawContext graphics, Matrix3x2f source, float scale, Matrix3x2f pose) {
         this.graphics = graphics;
         this.pose = pose;
         this.scale = scale;
         this.m00 = source.m00();
         this.m01 = source.m01();
         this.m10 = source.m10();
         this.m11 = source.m11();
         this.m20 = source.m20();
         this.m21 = source.m21();
      }
   }
}
