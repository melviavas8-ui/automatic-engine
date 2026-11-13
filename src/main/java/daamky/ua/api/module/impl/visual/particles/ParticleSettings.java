package daamky.ua.api.module.impl.visual.particles;

import java.awt.Color;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public final class ParticleSettings {
   public final MultiModeSetting spawnIf;
   public final MultiModeSetting texturesEnabled;
   public final ModeSetting renderModeAs;
   public final NumberSetting maxBaseScale;
   public final BooleanSetting scaleAsAlphaEnabled;
   public final BooleanSetting smoothTextureEnabled;
   public final BooleanSetting bloomTextureEnabled;
   public final BooleanSetting rainbowColorEnabled;
   public final ColorSetting color;
   public final NumberSetting spawnCountPerTickInWorld;
   public final NumberSetting maxSpawnDistanceInWorld;
   public final ModeSetting spawnPulseMotionSideRuleInWorld;
   public final ModeSetting motionSideChangeRuleInWorld;
   public final NumberSetting aliveTimeBaseInWorld;
   public final NumberSetting spawnCountOnAttack;
   public final NumberSetting maxSpawnDistanceOnAttack;
   public final ModeSetting spawnPulseMotionSideRuleOnAttack;
   public final ModeSetting motionSideChangeRuleOnAttack;
   public final NumberSetting aliveTimeBaseOnAttack;
   public final NumberSetting speedBase;
   public final NumberSetting gravityPower;
   public final NumberSetting motionInfluenceMul;
   public final NumberSetting magnetismPowerBase;
   public final ModeSetting jumpFromCollideMode;
   public final BooleanSetting jumpFromAnotherParticle;
   private final ParticleSettings.SettingRegistrar registrar;

   public ParticleSettings(ParticleSettings.SettingRegistrar registrar) {
      this.registrar = registrar;
      this.spawnIf = this.register(new MultiModeSetting("Events", "Particle spawn events.", ParticleConstants.EVENT_MODES, "World", "Attacks"));
      this.texturesEnabled = this.register(
         new MultiModeSetting("Textures", "Particle textures.", ParticleConstants.TEXTURE_MODES, ParticleConstants.TEXTURE_MODES)
      );
      this.renderModeAs = this.register(new ModeSetting("Render As", "Particle render mode.", "Textures", ParticleConstants.RENDER_MODES));
      this.maxBaseScale = this.register(new NumberSetting("Max Scale", "Maximum particle scale.", 0.5, 0.1, 1.0, 0.05));
      this.scaleAsAlphaEnabled = this.register(new BooleanSetting("Dynamic Scale", "Scales particles by current alpha.", true));
      this.smoothTextureEnabled = this.register(new BooleanSetting("Smooth Textures", "Keeps parity with the source texture smoothing option.", false));
      this.bloomTextureEnabled = this.register(new BooleanSetting("Bright Mode", "Uses a glowing blend for texture particles.", true));
      this.rainbowColorEnabled = this.register(new BooleanSetting("Rainbow Colors", "Uses random rainbow particle colors.", false));
      this.color = this.register(new ColorSetting("Color", "Custom particle color.", new Color(127, 242, 255, 255)));
      this.spawnCountPerTickInWorld = this.register(new NumberSetting("World Count", "Particles added each tick around you.", 4.0, 1.0, 15.0, 1.0));
      this.maxSpawnDistanceInWorld = this.register(new NumberSetting("World Spread", "Maximum world particle spread.", 10.0, 1.0, 24.0, 1.0));
      this.spawnPulseMotionSideRuleInWorld = this.register(
         new ModeSetting("World Spawn Direction", "Initial world particle direction.", "Random", ParticleConstants.SPAWN_RULES)
      );
      this.motionSideChangeRuleInWorld = this.register(
         new ModeSetting("World Force", "Continuous world particle force.", "None", ParticleConstants.FORCE_RULES)
      );
      this.aliveTimeBaseInWorld = this.register(new NumberSetting("World Lifetime", "World particle lifetime in milliseconds.", 2500.0, 500.0, 4000.0, 50.0));
      this.spawnCountOnAttack = this.register(new NumberSetting("Attack Count", "Particles added per hit.", 40.0, 5.0, 150.0, 5.0));
      this.maxSpawnDistanceOnAttack = this.register(new NumberSetting("Attack Spread", "Maximum hit particle spread.", 2.0, 0.5, 12.0, 0.5));
      this.spawnPulseMotionSideRuleOnAttack = this.register(
         new ModeSetting("Attack Spawn Direction", "Initial hit particle direction.", "From Source", ParticleConstants.SPAWN_RULES)
      );
      this.motionSideChangeRuleOnAttack = this.register(
         new ModeSetting("Attack Force", "Continuous hit particle force.", "None", ParticleConstants.FORCE_RULES)
      );
      this.aliveTimeBaseOnAttack = this.register(new NumberSetting("Attack Lifetime", "Hit particle lifetime in milliseconds.", 1250.0, 500.0, 4000.0, 50.0));
      this.speedBase = this.register(new NumberSetting("Start Speed", "Initial particle speed.", 0.3, 0.05, 1.0, 0.05));
      this.gravityPower = this.register(new NumberSetting("Gravity", "Particle gravity factor.", 0.2, -0.5, 1.0, 0.05));
      this.motionInfluenceMul = this.register(new NumberSetting("Motion Influence", "Particle slowdown or acceleration.", 0.0, -1.0, 1.0, 0.05));
      this.magnetismPowerBase = this.register(new NumberSetting("Turn Sharpness", "Particle movement turn sharpness.", 1.0, 0.1, 2.0, 0.05));
      this.jumpFromCollideMode = this.register(new ModeSetting("On Collision", "Block collision behavior.", "Ricochet", ParticleConstants.COLLISION_MODES));
      this.jumpFromAnotherParticle = this.register(new BooleanSetting("Particle Collisions", "Particles steer away from each other.", false));
      this.configureVisibility();
   }

   public boolean spawnWorld() {
      return this.spawnIf.isSelected("World");
   }

   public boolean spawnAttacks() {
      return this.spawnIf.isSelected("Attacks");
   }

   public boolean hasEnabledTextures() {
      return this.texturesEnabled.selectedCount() > 0;
   }

   private boolean hasAnySpawnEvent() {
      return this.spawnIf.selectedCount() > 0;
   }

   private boolean canSpawnParticles() {
      return this.hasAnySpawnEvent() && this.hasEnabledTextures();
   }

   private boolean hasAnyForceRule() {
      return this.spawnWorld() && !this.motionSideChangeRuleInWorld.is("None") || this.spawnAttacks() && !this.motionSideChangeRuleOnAttack.is("None");
   }

   private <S extends Setting<?>> S register(S setting) {
      return (S)this.registrar.register(setting);
   }

   private void configureVisibility() {
      this.texturesEnabled.visibleWhen(this::hasAnySpawnEvent);
      this.renderModeAs.visibleWhen(this::canSpawnParticles);
      this.maxBaseScale.visibleWhen(this::canSpawnParticles);
      this.scaleAsAlphaEnabled.visibleWhen(this::canSpawnParticles);
      this.smoothTextureEnabled.visibleWhen(this::canSpawnParticles);
      this.bloomTextureEnabled.visibleWhen(this::canSpawnParticles);
      this.rainbowColorEnabled.visibleWhen(this::canSpawnParticles);
      this.color.visibleWhen(() -> this.canSpawnParticles() && !this.rainbowColorEnabled.getValue());
      this.spawnCountPerTickInWorld.visibleWhen(() -> this.spawnWorld() && this.hasEnabledTextures());
      this.maxSpawnDistanceInWorld.visibleWhen(() -> this.spawnWorld() && this.hasEnabledTextures());
      this.spawnPulseMotionSideRuleInWorld.visibleWhen(() -> this.spawnWorld() && this.hasEnabledTextures());
      this.motionSideChangeRuleInWorld.visibleWhen(() -> this.spawnWorld() && this.hasEnabledTextures());
      this.aliveTimeBaseInWorld.visibleWhen(() -> this.spawnWorld() && this.hasEnabledTextures());
      this.spawnCountOnAttack.visibleWhen(() -> this.spawnAttacks() && this.hasEnabledTextures());
      this.maxSpawnDistanceOnAttack.visibleWhen(() -> this.spawnAttacks() && this.hasEnabledTextures());
      this.spawnPulseMotionSideRuleOnAttack.visibleWhen(() -> this.spawnAttacks() && this.hasEnabledTextures());
      this.motionSideChangeRuleOnAttack.visibleWhen(() -> this.spawnAttacks() && this.hasEnabledTextures());
      this.aliveTimeBaseOnAttack.visibleWhen(() -> this.spawnAttacks() && this.hasEnabledTextures());
      this.speedBase.visibleWhen(this::canSpawnParticles);
      this.gravityPower.visibleWhen(this::canSpawnParticles);
      this.motionInfluenceMul.visibleWhen(this::canSpawnParticles);
      this.magnetismPowerBase.visibleWhen(() -> this.canSpawnParticles() && this.hasAnyForceRule());
      this.jumpFromCollideMode.visibleWhen(this::canSpawnParticles);
      this.jumpFromAnotherParticle.visibleWhen(this::canSpawnParticles);
   }

   @FunctionalInterface
   public interface SettingRegistrar {
      Setting<?> register(Setting<?> var1);
   }
}
