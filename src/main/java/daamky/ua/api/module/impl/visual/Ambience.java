package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.client.world.ClientWorld.Properties;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameStateChangeS2CPacket.Reason;
import net.minecraft.world.biome.Biome.Precipitation;
import org.joml.Matrix4fc;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.post.heavenlywrath.HeavenlyWrathWorldRenderer;
import daamky.ua.utils.render.world.blackhole.BlackHoleSettings;
import daamky.ua.utils.render.world.blackhole.BlackHoleSkyRenderer;

public class Ambience extends Module {
   private static Ambience instance;
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "World time mode.", "Day", "Day", "Midday", "Night", "Midnight", "Custom"));
   private final NumberSetting customTime = this.register(new NumberSetting("Time", "Custom day time.", 1000.0, 0.0, 24000.0, 100.0));
   private final ModeSetting weather = this.register(new ModeSetting("Weather", "Client weather override.", "Sunny", "Sunny", "Rain", "Thunder", "Snow"));
   private final NumberSetting saturation = this.register(new NumberSetting("Saturation", "World saturation multiplier offset.", 0.0, -1.0, 1.0, 0.05));
   private final NumberSetting brightness = this.register(new NumberSetting("Brightness", "World brightness offset.", 0.0, -1.0, 1.0, 0.05));
   private final BooleanSetting customFog = this.register(new BooleanSetting("Custom Fog", "Use custom fog color.", false));
   private final ColorSetting customFogColor = this.register(new ColorSetting("Fog Color", "Custom fog color.", new Color(247, 133, 255)));
   private final NumberSetting customFogDensity = this.register(new NumberSetting("Fog Density", "Custom fog thickness.", 2.0, 0.1, 8.0, 0.1));
   private final BooleanSetting hideClouds = this.register(new BooleanSetting("No Clouds", "Hide vanilla clouds.", false));
   private final BooleanSetting customSky = this.register(new BooleanSetting("Custom Sky", "Use shader sky waves.", true));
   private final ModeSetting customSkyMode = this.register(
      new ModeSetting("Sky Mode", "Custom sky shader style.", "Aurora", "Aurora", "Nebula", "Heavenly Wrath", "Black Hole")
   );
   private final ColorSetting customSkyColor = this.register(new ColorSetting("Sky Color", "Base color for shader sky waves.", new Color(174, 55, 62, 255)));
   private final NumberSetting customSkySpeed = this.register(new NumberSetting("Sky Speed", "Shader sky animation speed.", 0.45, 0.05, 2.0, 0.05));
   private final NumberSetting customSkyIntensity = this.register(
      new NumberSetting("Sky Intensity", "Shader sky brightness and contrast.", 1.0, 0.2, 2.5, 0.05)
   );
   private final BooleanSetting heavenlyWorldEffects = this.register(
      new BooleanSetting("World Lighting", "Tint terrain and cast moving cloud shadows in Heavenly Wrath mode.", true)
   );
   private final NumberSetting heavenlyShadowStrength = this.register(
      new NumberSetting("Cloud Shadows", "Strength of moving shadows on terrain.", 0.62, 0.0, 1.0, 0.02)
   );
   private final NumberSetting blackHoleOpacity = this.register(
      new NumberSetting("Black Hole Opacity", "How much of the vanilla sky is replaced.", 1.0, 0.0, 1.0, 0.02)
   );
   private final NumberSetting blackHoleSize = this.register(new NumberSetting("Black Hole Size", "Apparent size of the black hole.", 1.35, 0.35, 2.5, 0.05));
   private final NumberSetting blackHoleBearing = this.register(
      new NumberSetting("Black Hole Bearing", "Compass direction of the black hole.", 0.0, 0.0, 360.0, 1.0)
   );
   private final NumberSetting blackHoleElevation = this.register(
      new NumberSetting("Black Hole Elevation", "Height above the horizon.", 12.0, -20.0, 70.0, 1.0)
   );
   private final NumberSetting blackHoleSpin = this.register(new NumberSetting("Black Hole Spin", "Accretion disk animation speed.", 1.0, 0.0, 4.0, 0.05));
   private final NumberSetting blackHoleBrightness = this.register(
      new NumberSetting("Black Hole Brightness", "Accretion disk brightness.", 1.0, 0.2, 3.0, 0.05)
   );
   private final ColorSetting blackHoleInnerColor = this.register(new ColorSetting("Black Hole Inner", "Inner accretion disk color.", new Color(255, 246, 214)));
   private final ColorSetting blackHoleOuterColor = this.register(new ColorSetting("Black Hole Outer", "Outer accretion disk color.", new Color(255, 92, 16)));
   private final ColorSetting blackHoleSpaceColor = this.register(new ColorSetting("Black Hole Space", "Space background color.", new Color(26, 10, 24)));
   private boolean weatherOverrideActive;
   private boolean cachedServerRaining;
   private float cachedServerRainLevel;
   private float cachedServerThunderLevel;
   private ClientWorld weatherSnapshotLevel;

   public Ambience() {
      super("Ambience", "Changes time, weather and world atmosphere.", ModuleCategory.VISUAL);
      this.customTime.visibleWhen(() -> this.mode.is("Custom"));
      this.customFogColor.visibleWhen(this.customFog::getValue);
      this.customFogDensity.visibleWhen(this.customFog::getValue);
      this.customSkyMode.visibleWhen(this.customSky::getValue);
      this.customSkyColor.visibleWhen(() -> this.customSky.getValue() && !this.customSkyMode.is("Black Hole"));
      this.customSkySpeed.visibleWhen(() -> this.customSky.getValue() && !this.customSkyMode.is("Black Hole"));
      this.customSkyIntensity.visibleWhen(() -> this.customSky.getValue() && !this.customSkyMode.is("Black Hole"));
      this.heavenlyWorldEffects.visibleWhen(this::isHeavenlyWrathModeSelected);
      this.heavenlyShadowStrength.visibleWhen(() -> this.isHeavenlyWrathModeSelected() && this.heavenlyWorldEffects.getValue());
      this.blackHoleOpacity.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleSize.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleBearing.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleElevation.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleSpin.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleBrightness.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleInnerColor.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleOuterColor.visibleWhen(this::isBlackHoleModeSelected);
      this.blackHoleSpaceColor.visibleWhen(this::isBlackHoleModeSelected);
      instance = this;
   }

   public static Ambience getInstance() {
      return instance;
   }

   public float getBrightnessValue() {
      return this.brightness.getFloat();
   }

   public float getSaturationFactor() {
      return Math.clamp(1.0F + this.saturation.getFloat(), 0.0F, 2.0F);
   }

   public boolean hasCustomFog() {
      return this.isEnabled() && this.customFog.getValue();
   }

   public int getCustomFogColor() {
      return this.customFogColor.getValue().getRGB();
   }

   public float getCustomFogDensity() {
      return this.customFogDensity.getFloat();
   }

   public boolean shouldHideClouds() {
      return this.isEnabled() && (this.hideClouds.getValue() || this.hasBlackHoleSky());
   }

   public boolean hasCustomSky() {
      return this.isEnabled() && this.customSky.getValue() && !this.customSkyMode.is("Black Hole");
   }

   public boolean hasBlackHoleSky() {
      return this.isEnabled() && this.customSky.getValue() && this.customSkyMode.is("Black Hole") && !BlackHoleSkyRenderer.isDisabledAfterError();
   }

   public boolean shouldHideVanillaSky() {
      return this.hasBlackHoleSky() && this.blackHoleOpacity.getFloat() >= 0.999F;
   }

   public void renderBlackHole(Framebuffer target, Matrix4fc projectionMatrix, Matrix4fc viewMatrix) {
      if (this.hasBlackHoleSky() && target != null && !(this.blackHoleOpacity.getFloat() <= 0.0F)) {
         BlackHoleSkyRenderer.render(target, projectionMatrix, viewMatrix, this.buildBlackHoleSettings());
      }
   }

   public int getCustomSkyColor() {
      return this.customSkyColor.getValue().getRGB();
   }

   public float getCustomSkySpeed() {
      return this.customSkySpeed.getFloat();
   }

   public float getCustomSkyIntensity() {
      return this.customSkyIntensity.getFloat();
   }

   public boolean hasHeavenlyWrathWorldEffects() {
      return this.isEnabled()
         && this.customSky.getValue()
         && this.customSkyMode.is("Heavenly Wrath")
         && this.heavenlyWorldEffects.getValue()
         && !HeavenlyWrathWorldRenderer.isDisabledAfterError();
   }

   public float getHeavenlyShadowStrength() {
      return this.heavenlyShadowStrength.getFloat();
   }

   public int getCustomSkyModeId() {
      String var1 = this.customSkyMode.getValue();

      return switch (var1) {
         case "Nebula" -> 1;
         case "Void" -> 2;
         case "Prism" -> 3;
         case "Storm" -> 4;
         case "Heavenly Wrath" -> 5;
         default -> 0;
      };
   }

   public String getWeatherModeName() {
      return this.weather.getValue();
   }

   public long getInternalTime() {
      if (this.mode.is("Day")) {
         return 1000L;
      } else if (this.mode.is("Midday")) {
         return 6000L;
      } else if (this.mode.is("Night")) {
         return 13000L;
      } else {
         return this.mode.is("Midnight") ? 18000L : (long)this.customTime.getValue().doubleValue();
      }
   }

   public void syncWeather(ClientWorld level, Properties levelData) {
      if (level != null && levelData != null) {
         if (this.weatherSnapshotLevel != level) {
            this.clearWeatherSnapshot();
         }

         if (!this.weatherOverrideActive) {
            this.cachedServerRaining = levelData.isRaining();
            this.cachedServerRainLevel = level.getRainGradient(1.0F);
            this.cachedServerThunderLevel = level.getThunderGradient(1.0F);
            this.weatherOverrideActive = true;
            this.weatherSnapshotLevel = level;
         }

         boolean raining = this.shouldForcePrecipitation();
         levelData.setRaining(raining);
         level.setRainGradient(raining ? 1.0F : 0.0F);
         level.setThunderGradient(this.weather.is("Thunder") ? 1.0F : 0.0F);
      } else {
         this.clearWeatherSnapshot();
      }
   }

   public Precipitation getForcedPrecipitation() {
      if (!this.isEnabled()) {
         return null;
      } else if (this.weather.is("Snow")) {
         return Precipitation.SNOW;
      } else {
         return !this.weather.is("Rain") && !this.weather.is("Thunder") ? Precipitation.NONE : Precipitation.RAIN;
      }
   }

   public boolean shouldForceSnow() {
      return this.getForcedPrecipitation() == Precipitation.SNOW;
   }

   @Override
   protected void onDisable() {
      BlackHoleSkyRenderer.clear();
      HeavenlyWrathWorldRenderer.clear();
      ClientWorld var2 = mc.world;
      if (var2 instanceof ClientWorld) {
         this.restoreWeather(var2, var2.getLevelProperties());
      } else {
         this.clearWeatherSnapshot();
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive()) {
         if (event.getPacket() instanceof WorldTimeUpdateS2CPacket && this.isEnabled()) {
            event.cancel();
         } else if (event.getPacket() instanceof GameStateChangeS2CPacket packet) {
            Reason type = packet.getReason();
            if (isWeatherPacket(type)) {
               this.updateCachedWeather(packet);
               if (this.isEnabled()) {
                  event.cancel();
               }
            }
         }
      }
   }

   private boolean shouldForcePrecipitation() {
      return this.weather.is("Rain") || this.weather.is("Thunder") || this.weather.is("Snow");
   }

   private boolean isBlackHoleModeSelected() {
      return this.customSky.getValue() && this.customSkyMode.is("Black Hole");
   }

   private boolean isHeavenlyWrathModeSelected() {
      return this.customSky.getValue() && this.customSkyMode.is("Heavenly Wrath");
   }

   private BlackHoleSettings buildBlackHoleSettings() {
      double yaw = Math.toRadians(this.blackHoleBearing.getValue());
      double pitch = Math.toRadians(this.blackHoleElevation.getValue());
      float directionX = (float)(-Math.sin(yaw) * Math.cos(pitch));
      float directionY = (float)Math.sin(pitch);
      float directionZ = (float)(Math.cos(yaw) * Math.cos(pitch));
      float cameraDistance = (float)(22.0 / Math.max(0.2, this.blackHoleSize.getValue()));
      Color hot = this.blackHoleInnerColor.getValue();
      Color cold = this.blackHoleOuterColor.getValue();
      Color space = this.blackHoleSpaceColor.getValue();
      return new BlackHoleSettings(
         directionX,
         directionY,
         directionZ,
         cameraDistance,
         (float)hot.getRed() / 255.0F,
         (float)hot.getGreen() / 255.0F,
         (float)hot.getBlue() / 255.0F,
         2.7F,
         (float)cold.getRed() / 255.0F,
         (float)cold.getGreen() / 255.0F,
         (float)cold.getBlue() / 255.0F,
         13.0F,
         1.0F,
         0.69F,
         0.29F,
         1.0F,
         (float)space.getRed() / 255.0F,
         (float)space.getGreen() / 255.0F,
         (float)space.getBlue() / 255.0F,
         0.55F,
         this.blackHoleSpin.getFloat(),
         0.6F,
         (float)Math.toRadians(1.0),
         this.blackHoleOpacity.getFloat(),
         120.0F,
         this.blackHoleBrightness.getFloat(),
         0.35F,
         0.14F,
         1.0F,
         1.15F
      );
   }

   private void restoreWeather(ClientWorld level, Properties levelData) {
      if (this.weatherOverrideActive) {
         levelData.setRaining(this.cachedServerRaining);
         level.setRainGradient(this.cachedServerRainLevel);
         level.setThunderGradient(this.cachedServerThunderLevel);
         this.clearWeatherSnapshot();
      }
   }

   private void clearWeatherSnapshot() {
      this.weatherOverrideActive = false;
      this.weatherSnapshotLevel = null;
   }

   private void updateCachedWeather(GameStateChangeS2CPacket packet) {
      Reason type = packet.getReason();
      if (type == GameStateChangeS2CPacket.RAIN_STARTED) {
         this.cachedServerRaining = true;
         this.cachedServerRainLevel = Math.max(this.cachedServerRainLevel, 1.0F);
      } else if (type == GameStateChangeS2CPacket.RAIN_STOPPED) {
         this.cachedServerRaining = false;
         this.cachedServerRainLevel = 0.0F;
         this.cachedServerThunderLevel = 0.0F;
      } else if (type == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED) {
         this.cachedServerRainLevel = Math.clamp(packet.getValue(), 0.0F, 1.0F);
         this.cachedServerRaining = this.cachedServerRainLevel > 1.0E-4F;
      } else {
         if (type == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED) {
            this.cachedServerThunderLevel = Math.clamp(packet.getValue(), 0.0F, 1.0F);
         }
      }
   }

   private static boolean isWeatherPacket(Reason type) {
      return type == GameStateChangeS2CPacket.RAIN_STARTED
         || type == GameStateChangeS2CPacket.RAIN_STOPPED
         || type == GameStateChangeS2CPacket.RAIN_GRADIENT_CHANGED
         || type == GameStateChangeS2CPacket.THUNDER_GRADIENT_CHANGED;
   }
}
