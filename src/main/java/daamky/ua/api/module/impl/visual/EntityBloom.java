package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.post.entitybloom.EntityBloomWorldTrail;

public final class EntityBloom extends Module {
   private static EntityBloom instance;
   private final BooleanSetting players = this.register(new BooleanSetting("Players", "Glow around other players.", true));
   private final BooleanSetting mobs = this.register(new BooleanSetting("Mobs", "Glow around living mobs and animals.", true));
   private final BooleanSetting self = this.register(new BooleanSetting("Self", "Glow around your own model in third person.", false));
   private final BooleanSetting invisible = this.register(new BooleanSetting("Invisible", "Include invisible living entities.", false));
   private final ColorSetting color = this.register(new ColorSetting("Color", "Bloom and outline color.", new Color(170, 92, 255, 235)));
   private final NumberSetting radius = this.register(new NumberSetting("Glow Radius", "Soft shader glow radius in screen pixels.", 8.0, 2.0, 14.0, 0.5));
   private final NumberSetting strength = this.register(new NumberSetting("Glow Strength", "Brightness and opacity of the shader glow.", 1.05, 0.15, 1.5, 0.05));
   private final NumberSetting outlineStrength = this.register(
      new NumberSetting("Outline Strength", "Density of the dark-neon edge outside the silhouette.", 0.55, 0.0, 1.0, 0.05)
   );
   private final NumberSetting flameAmount = this.register(
      new NumberSetting("Flame Shape", "Height and turbulence of the flame tongues. Zero leaves a plain soft glow.", 1.0, 0.0, 1.5, 0.05)
   );
   private final NumberSetting flameSpeed = this.register(new NumberSetting("Flame Speed", "Speed of the living flame motion.", 0.9, 0.1, 5.0, 0.1));
   private final NumberSetting flameDetail = this.register(
      new NumberSetting("Flame Detail", "Noise frequency of the flame edge. Low is broad and lazy, high is fine and wispy.", 0.85, 0.2, 1.5, 0.05)
   );
   private final NumberSetting brightness = this.register(new NumberSetting("Brightness", "Brightness of the selected color.", 1.1, 0.35, 1.6, 0.05));
   private final NumberSetting distance = this.register(new NumberSetting("Distance", "Maximum entity glow distance.", 96.0, 8.0, 192.0, 4.0));
   private final BooleanSetting hitColor = this.register(new BooleanSetting("Hit Color", "Fills a damaged entity with the bloom color.", true));
   private final BooleanSetting afterimageTrail = this.register(
      new BooleanSetting("Afterimage Trail", "Leaves soft fading bloom silhouettes behind moving entities.", true)
   );
   private final NumberSetting trailLength = this.register(
      new NumberSetting("Trail Length", "How long bloom afterimages remain visible.", 0.88, 0.55, 0.97, 0.01)
   );
   private final NumberSetting trailSoftness = this.register(
      new NumberSetting("Trail Softness", "How smoothly old silhouettes spread and dissolve.", 1.4, 0.0, 3.0, 0.1)
   );
   private final BooleanSetting pulse = this.register(new BooleanSetting("Pulse", "Makes the glow breathe smoothly.", true));
   private final NumberSetting pulseSpeed = this.register(new NumberSetting("Pulse Speed", "Speed of the glow breathing animation.", 0.8, 0.1, 3.0, 0.1));
   private final NumberSetting pulseDepth = this.register(new NumberSetting("Pulse Depth", "Amount of brightness change while pulsing.", 0.14, 0.0, 0.45, 0.01));

   public EntityBloom() {
      super("EntityBloom", "Soft shader bloom around players and mobs.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static EntityBloom getInstance() {
      return instance;
   }

   public static void apply(Entity entity, EntityRenderState state) {
      EntityBloom bloom = instance;
      if (bloom != null && bloom.isEnabled() && bloom.shouldRender(entity)) {
         state.outlineColor = bloom.currentOutlineColor(entity);
      }
   }

   public float getShaderRadius() {
      return this.isEnabled() ? this.radius.getFloat() : 2.0F;
   }

   public float getShaderStrength() {
      if (!this.isEnabled()) {
         return 1.0F;
      } else {
         Color selected = this.color.getValue();
         float alpha = (float)selected.getAlpha() / 255.0F;
         return Math.max(0.0F, this.strength.getFloat() * alpha * this.pulseMultiplier());
      }
   }

   public float getOutlineStrength() {
      return this.isEnabled() ? this.outlineStrength.getFloat() : 0.35F;
   }

   public float getFlameAmount() {
      return this.isEnabled() ? this.flameAmount.getFloat() : 0.0F;
   }

   public float getFlameSpeed() {
      return this.flameSpeed.getFloat();
   }

   public float getFlameDetail() {
      return this.flameDetail.getFloat();
   }

   public boolean isHitColorEnabled() {
      return this.isEnabled() && this.hitColor.getValue();
   }

   public boolean isAfterimageTrailEnabled() {
      return this.isEnabled() && this.afterimageTrail.getValue();
   }

   public float getTrailLength() {
      return this.trailLength.getFloat() * 1.5F;
   }

   public float getTrailSoftness() {
      return this.trailSoftness.getFloat();
   }

   public boolean shouldRenderTrail(Entity entity) {
      return this.isAfterimageTrailEnabled() && this.shouldRender(entity);
   }

   public int getWorldTrailOutlineColor(float opacity) {
      Color selected = this.color.getValue();
      float multiplier = this.brightness.getFloat() * (0.92F + this.pulseMultiplier() * 0.08F);
      int red = clamp(Math.round((float)selected.getRed() * multiplier));
      int green = clamp(Math.round((float)selected.getGreen() * multiplier));
      int blue = clamp(Math.round((float)selected.getBlue() * multiplier));
      float softnessBoost = 0.72F + Math.min(3.0F, this.trailSoftness.getFloat()) * 0.09F;
      float encoded = 0.008F + Math.clamp(opacity * softnessBoost, 0.0F, 1.0F) * 0.061F;
      int alpha = clamp(Math.round(encoded * 255.0F));
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   @Override
   protected void onDisable() {
      EntityBloomWorldTrail.clear();
   }

   private boolean shouldRender(Entity entity) {
      if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
         return false;
      }

      if (!this.invisible.getValue() && entity.isInvisible()) {
         return false;
      } else {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == null) {
            return false;
         } else {
            double maxDistance = this.distance.getValue();
            if (client.player.squaredDistanceTo(entity) > maxDistance * maxDistance) {
               return false;
            } else if (entity == client.player) {
               return this.self.getValue();
            } else {
               return entity instanceof PlayerEntity ? this.players.getValue() : this.mobs.getValue();
            }
         }
      }
   }

   private int currentOutlineColor(Entity entity) {
      int red;
      int green;
      int blue;
      float distanceFactor;
      boolean var10000;
      label19: {
         Color selected = this.color.getValue();
         float multiplier = this.brightness.getFloat() * (0.9F + this.pulseMultiplier() * 0.1F);
         red = clamp(Math.round((float)selected.getRed() * multiplier));
         green = clamp(Math.round((float)selected.getGreen() * multiplier));
         blue = clamp(Math.round((float)selected.getBlue() * multiplier));
         distanceFactor = this.distanceScale(entity);
         if (this.hitColor.getValue() && entity instanceof LivingEntity living && living.hurtTime > 0) {
            var10000 = true;
            break label19;
         }

         var10000 = false;
      }

      boolean hit = var10000;
      float encodedAlpha = hit ? 0.72F + distanceFactor * 0.24F : 0.08F + distanceFactor * 0.54F;
      int distanceAlpha = clamp(Math.round(encodedAlpha * 255.0F));
      return distanceAlpha << 24 | red << 16 | green << 8 | blue;
   }

   private float distanceScale(Entity entity) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player == null) {
         return 1.0F;
      } else {
         double blocks = Math.sqrt(client.player.squaredDistanceTo(entity));
         double scale = Math.pow(6.0 / Math.max(6.0, blocks), 0.32);
         return (float)Math.max(0.45, Math.min(1.0, scale));
      }
   }

   private float pulseMultiplier() {
      if (this.pulse.getValue() && !(this.pulseDepth.getFloat() <= 0.0F)) {
         double seconds = (double)System.nanoTime() * 1.0E-9;
         double wave = 0.5 + 0.5 * Math.sin(seconds * (double)this.pulseSpeed.getFloat() * Math.PI * 2.0);
         return 1.0F - this.pulseDepth.getFloat() + (float)wave * this.pulseDepth.getFloat();
      } else {
         return 1.0F;
      }
   }

   private static int clamp(int value) {
      return Math.max(0, Math.min(255, value));
   }
}
