package daamky.ua.api.module.impl.visual.frageffect;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.network.OtherClientPlayerEntity;

public class LegacyGlassRenderer {
   private static final Map<Integer, LegacyGlassRenderer.GlassProperties> glassEntities = new HashMap<>();

   public LegacyGlassRenderer() {
   }

   public static void applyGlassEffect(OtherClientPlayerEntity fakePlayer, boolean glassEnabled, float alpha, Color tint) {
      if (fakePlayer != null) {
         int entityId = fakePlayer.getId();
         if (!glassEnabled) {
            fakePlayer.setInvisible(false);
            fakePlayer.setGlowing(false);
            glassEntities.remove(entityId);
         } else {
            float normalizedAlpha = Math.clamp(alpha / 100.0F, 0.0F, 1.0F);
            glassEntities.put(entityId, new LegacyGlassRenderer.GlassProperties(normalizedAlpha, tint));
            fakePlayer.setInvisible(false);
            fakePlayer.setGlowing(true);
         }
      }
   }

   public static LegacyGlassRenderer.GlassProperties getGlassProperties(int entityId) {
      return glassEntities.get(entityId);
   }

   public static boolean hasGlassEffect(int entityId) {
      return glassEntities.containsKey(entityId);
   }

   public static void removeGlassEffect(int entityId) {
      glassEntities.remove(entityId);
   }

   public static void clearAll() {
      glassEntities.clear();
   }

   public static int getGlowColor(Color tint) {
      return tint == null ? 16777215 : tint.getRed() << 16 | tint.getGreen() << 8 | tint.getBlue();
   }

   public static void applyGlassGLState(float alpha, Color tint) {
      GlStateManager._enableBlend();
      GlStateManager._blendFuncSeparate(770, 771, 1, 0);
      GlStateManager._depthMask(false);
   }

   public static void resetGlassGLState() {
      GlStateManager._depthMask(true);
   }

   public static class GlassProperties {
      private final float alpha;
      private final Color tint;

      public GlassProperties(float alpha, Color tint) {
         this.alpha = alpha;
         this.tint = tint;
      }

      public float getAlpha() {
         return this.alpha;
      }

      public Color getTint() {
         return this.tint;
      }

      public int getColorARGB() {
         int a = Math.round(this.alpha * 255.0F);
         return a << 24 | this.tint.getRed() << 16 | this.tint.getGreen() << 8 | this.tint.getBlue();
      }
   }
}
