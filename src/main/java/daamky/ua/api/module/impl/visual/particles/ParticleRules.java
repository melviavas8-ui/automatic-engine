package daamky.ua.api.module.impl.visual.particles;

import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

final class ParticleRules {
   final Entity entityFor;
   final boolean ignoreCollisionsWithBlocks;
   final boolean instantDeathOnCollide;
   final boolean ricochetOnCollide;
   final boolean stopMotionOnCollide;
   final boolean collisionAtOtherParticles;
   private final ParticleSettings settings;
   private final MinecraftClient client;
   private final Random random;
   private final boolean renderModeIsRandom;
   boolean renderBox;
   boolean renderTexture;
   final float motionMulFactorAlways;
   final float motionMulOnCollideJump;
   final float motionMulOnCollideSlide;
   final float gravityPowers;
   float minScale;
   float maxScale;
   float rotationSpeedMul;
   String motionSideChangeRuleMode;

   ParticleRules(MinecraftClient client, ParticleSettings settings, Random random, Entity entityFor) {
      this.client = client;
      this.settings = settings;
      this.random = random;
      this.entityFor = entityFor;
      this.ignoreCollisionsWithBlocks = settings.jumpFromCollideMode.is("Ignore");
      this.instantDeathOnCollide = settings.jumpFromCollideMode.is("Fatal");
      this.ricochetOnCollide = this.isRicochetMode(settings.jumpFromCollideMode.getValue());
      this.stopMotionOnCollide = settings.jumpFromCollideMode.is("Stuck");
      this.collisionAtOtherParticles = settings.jumpFromAnotherParticle.getValue();
      String renderMode = settings.renderModeAs.getValue();
      this.renderModeIsRandom = "Random".equals(renderMode);
      this.applyRenderMode(this.renderModeIsRandom ? this.randomRenderMode() : renderMode);
      float motionInfluence = settings.motionInfluenceMul.getFloat();
      this.motionMulFactorAlways = motionInfluence > 0.0F
         ? 1.0F + ParticleMath.cubicOut(motionInfluence) * 0.05F
         : 1.0F + ParticleMath.sineOut(motionInfluence) * 0.1F;
      this.motionMulOnCollideJump = this.collisionJumpMultiplier(settings.jumpFromCollideMode.getValue());
      this.motionMulOnCollideSlide = settings.jumpFromCollideMode.is("Slide Slow") ? 0.875F : 1.0F;
      this.gravityPowers = settings.gravityPower.getFloat();
      this.updateScalePreUpdate();
      this.motionSideChangeRuleMode = this.sourceIsMe() ? settings.motionSideChangeRuleInWorld.getValue() : settings.motionSideChangeRuleOnAttack.getValue();
      if ("Alternate by Source".equalsIgnoreCase(this.motionSideChangeRuleMode)) {
         this.motionSideChangeRuleMode = "To Source";
      }

      this.rotationSpeedMul = settings.magnetismPowerBase.getFloat();
   }

   void updateScalePreUpdate() {
      this.maxScale = this.settings.maxBaseScale.getFloat();
      this.minScale = this.maxScale / 5.0F;
   }

   void updateMotionRulePreUpdate(int ticksExisted, int timeAliveTicks) {
      String currentRule = this.sourceIsMe() ? this.settings.motionSideChangeRuleInWorld.getValue() : this.settings.motionSideChangeRuleOnAttack.getValue();
      if ("Alternate by Source".equalsIgnoreCase(currentRule)) {
         this.motionSideChangeRuleMode = (float)ticksExisted < (float)timeAliveTicks / 2.0F ? "To Source" : "From Source";
      }
   }

   void updateRenderRulesPreRender() {
      if (!this.renderModeIsRandom) {
         this.applyRenderMode(this.settings.renderModeAs.getValue());
      }
   }

   void updateRotationSpeedMul() {
      String var2 = this.motionSideChangeRuleMode;

      float multiplier = switch (var2) {
         case "To Source", "From Source" -> 2.0F;
         case "Twist Left", "Twist Right", "Twist Random" -> 1.0F;
         case "Camera Left", "Camera Right", "Camera Forward", "Camera Back" -> 0.5F;
         default -> 1.0F;
      };
      this.rotationSpeedMul = multiplier * this.settings.magnetismPowerBase.getFloat();
   }

   private boolean sourceIsMe() {
      return this.entityFor == null || this.client.player == null || this.entityFor.getId() == this.client.player.getId();
   }

   private void applyRenderMode(String renderMode) {
      this.renderBox = "Cubes".equals(renderMode) || "Textures and Cubes".equals(renderMode);
      this.renderTexture = "Textures".equals(renderMode) || "Textures and Cubes".equals(renderMode);
   }

   private boolean isRicochetMode(String mode) {
      return "Ricochet".equals(mode)
         || "Strong Ricochet".equals(mode)
         || "Very Strong Ricochet".equals(mode)
         || "Weak Ricochet".equals(mode)
         || "Very Weak Ricochet".equals(mode);
   }

   private float collisionJumpMultiplier(String mode) {
      return switch (mode) {
         case "Very Weak Ricochet" -> 0.5F;
         case "Weak Ricochet" -> 0.75F;
         case "Strong Ricochet" -> 1.5F;
         case "Very Strong Ricochet" -> 2.0F;
         default -> 1.0F;
      };
   }

   private String randomRenderMode() {
      return switch (this.random.nextInt(3)) {
         case 0 -> "Textures";
         case 1 -> "Cubes";
         default -> "Textures and Cubes";
      };
   }
}
