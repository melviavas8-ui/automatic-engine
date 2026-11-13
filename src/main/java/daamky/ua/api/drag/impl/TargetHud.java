package daamky.ua.api.drag.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.visual.ESP;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;
import daamky.ua.utils.render.ui.font.FontType;

public final class TargetHud extends HudPanel {
   private LivingEntity lastTarget;
   private static final int MAX_HIT_PARTICLES = 1148;
   private static final long HIT_PARTICLE_LIFETIME_MS = 420L;
   private final Random random = new Random();
   private final List<TargetHud.HitParticle> hitParticles = new ArrayList<>();
   private int hitTargetId = Integer.MIN_VALUE;
   private int lastHurtTime;
   private final SmoothAnimation panelAnimation = new SmoothAnimation();
   private final SmoothAnimation healthAnimation = new SmoothAnimation();
   private final SmoothAnimation barAnimation = new SmoothAnimation();
   private final SmoothAnimation absorptionAnimation = new SmoothAnimation();
   private final SmoothAnimation healthIconXAnimation = new SmoothAnimation();
   private int animatedTargetId = Integer.MIN_VALUE;
   private boolean healthIconXReady;

   public TargetHud() {
      super("targethud", "TargetHud", 140.0F, 130.0F, 110.0F, 31.0F);
   }

   @Override
   public void render() {
      TargetHud.TargetHudState state = this.logics();
      if (state != null) {
         this.renderTargetHud(state);
      }
   }

   private TargetHud.TargetHudState logics() {
      LivingEntity target = this.target();
      boolean preview = target == null && this.editPreview() && this.mc.player != null;
      boolean visible = target != null || preview;
      this.panelAnimation.update();
      this.panelAnimation.run(visible ? 1.0 : 0.0, 0.24F, visible ? Easings.EXPO_OUT : Easings.EXPO_IN, true);
      float alpha = this.panelAnimation.get();
      this.contentVisible(visible || alpha > 0.01F || this.panelAnimation.isAlive());
      if (alpha <= 0.0F && !this.panelAnimation.isAlive()) {
         this.lastTarget = null;
         return null;
      } else {
         if (preview) {
            target = this.mc.player;
         } else if (target != null) {
            this.lastTarget = target;
         } else {
            if (this.lastTarget == null || !this.lastTarget.isAlive() || !this.panelAnimation.isAlive()) {
               this.lastTarget = null;
               return null;
            }

            target = this.lastTarget;
         }

         if (target == null) {
            return null;
         } else {
            this.size(110.0F, 31.0F);
            return new TargetHud.TargetHudState(target, alpha, this.drag.x(), this.drag.y(), this.drag.width(), this.drag.height());
         }
      }
   }

   private void renderTargetHud(TargetHud.TargetHudState state) {
      Hud hud = Hud.getInstance();
      int hudColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);
      int fadedHudColor = ColorUtil.multAlpha(hudColor, state.alpha);
      int glowColor = ColorUtil.multAlpha(hudColor, 0.45F * state.alpha);
      int barBackgroundColor = ColorUtil.multAlpha(hudColor, 0.24F * state.alpha);
      int absorptionColor = ColorUtil.rgba(255, 190, 48, Math.round(255.0F * state.alpha));
      String name = state.target.getName().getString();
      float targetHealth = this.displayHealth(state.target);
      float targetAbsorption = state.target.getAbsorptionAmount();
      float hurtFlash = clamp((float)state.target.hurtTime / 10.0F, 0.0F, 1.0F);
      float maxHealth = Math.max(1.0F, state.target.getMaxHealth());
      maxHealth = Math.max(maxHealth, targetHealth);
      float targetProgress = clamp(targetHealth / maxHealth, 0.0F, 1.0F);
      float targetAbsorptionProgress = clamp(targetAbsorption / maxHealth, 0.0F, 1.0F);
      TargetHud.HealthState healthState = this.animatedHealth(state.target, targetHealth, targetProgress, targetAbsorptionProgress);
      String hp = this.formatWholeHealth(targetHealth) + "Hp";
      float absorptionProgress = healthState.absorptionProgress;
      float headX = state.x + 5.0F;
      float headY = state.y + 5.0F;
      float headSize = 21.0F;
      float barX = state.x + 32.0F;
      float barY = state.y + 21.0F;
      float barWidth = state.width - 39.0F;
      float iconSize = 7.5F;
      float iconX = state.x + 33.0F;
      float iconY = state.y + 5.3F;
      float nameX = iconX - 1.0F;
      float hpRight = state.x + state.width - 7.0F;
      float hpCellWidth = 3.35F;
      float targetHealthIconLocalX = state.width - 7.0F - (float)hp.length() * hpCellWidth - iconSize - 1.5F;
      this.healthIconXAnimation.update();
      if (this.healthIconXReady && !(Math.abs(this.healthIconXAnimation.get() - targetHealthIconLocalX) > 18.0F)) {
         this.healthIconXAnimation.run((double)targetHealthIconLocalX, 0.24F, Easings.EXPO_OUT, true);
      } else {
         this.healthIconXAnimation.set((double)targetHealthIconLocalX);
         this.healthIconXReady = true;
      }

      float healthIconX = state.x + this.healthIconXAnimation.get();
      float glowSize = 20.0F;
      int glowAlpha = Math.round(105.0F * state.alpha);
      float nameRight = healthIconX - 1.5F;
      float nameSize = 7.0F;
      float nameY = state.y + 6.0F;
      float nameClipWidth = Math.max(0.0F, nameRight - nameX);
      float nameFadeWidth = Math.min(8.0F, Math.max(2.0F, nameClipWidth * 0.35F));
      float nameFadeStrength = Render2D.textWidth(TEXT_FONT, name, nameSize) > nameClipWidth + 0.5F ? 0.88F : 0.0F;
      HudRenderCompat.background(state.x, state.y, state.width, state.height, 6.0F, 15.0F, 1.2F, ColorUtil.rgba(0, 0, 0, Math.round(255.0F * state.alpha)));
      if (state.target instanceof AbstractClientPlayerEntity player) {
         String texture = player.getSkin().body().texturePath().toString();
         int imageAlpha = Math.round(255.0F * state.alpha);
         if (imageAlpha > 3) {
            int baseHeadColor = ColorUtil.rgba(255, 255, 255, imageAlpha);
            int hurtHeadColor = ColorUtil.rgba(255, 96, 96, imageAlpha);
            int color = ColorUtil.lerpColor(baseHeadColor, hurtHeadColor, hurtFlash);
            boolean base = this.renderSkinPart(texture, headX, headY, headSize, 0.125F, 0.125F, 0.25F, 0.25F, color);
            boolean overlay = this.renderSkinPart(texture, headX, headY, headSize, 0.625F, 0.125F, 0.75F, 0.25F, color);
            if (!base && !overlay) {
               Render2D.image(texture, headX, headY, headSize, 6.0F, color);
            }
         }
      } else {
         String targetName = state.target.getName().getString();
         String letter = state.target instanceof PlayerEntity && !targetName.isEmpty() ? targetName.substring(0, 1).toUpperCase() : "?";
         float tw = Render2D.textWidth(TITLE_FONT, letter, 10.0F);
         int baseHeadBackColor = ColorUtil.rgba(128, 128, 128, Math.round(24.0F * state.alpha));
         int hurtHeadBackColor = ColorUtil.rgba(255, 70, 70, Math.round(58.0F * state.alpha));
         int headBackColor = ColorUtil.lerpColor(baseHeadBackColor, hurtHeadBackColor, hurtFlash);
         int letterColor = ColorUtil.lerpColor(
            ColorUtil.multAlpha(TEXT_COLOR, state.alpha), ColorUtil.rgba(255, 225, 225, Math.round(255.0F * state.alpha)), hurtFlash
         );
         Render2D.rect(headX, headY, headSize, headSize, 4.0F, headBackColor);
         Render2D.text(TITLE_FONT, letter, headX + (headSize - tw) * 0.5F + 0.5F, headY + 4.5F, 10.0F, letterColor);
      }

      if (glowAlpha > 3) {
         HudRenderCompat.glow("daamky:textures/particles/ghost-glow.png", healthIconX - 6.5F, iconY - 4.0F, glowSize, glowSize, 0.0F, glowColor);
      }

      Render2D.text(FontType.MAINMENUSCREEN, "e", healthIconX, iconY + 2.0F, iconSize, fadedHudColor);
      if (nameClipWidth > 0.5F) {
         ScissorUtil.push(
            Render2DCoordinateSpace.toGuiInt(nameX),
            Render2DCoordinateSpace.toGuiInt(nameY - 4.0F),
            Render2DCoordinateSpace.toGuiInt(nameRight),
            Render2DCoordinateSpace.toGuiInt(nameY + nameSize + 5.0F)
         );
         if (nameFadeStrength > 0.0F) {
            Render2D.textFade(
               TEXT_FONT, name, nameX, nameY, nameSize, ColorUtil.multAlpha(TEXT_COLOR, state.alpha), nameX, nameRight, nameFadeWidth, 0.0F, nameFadeStrength
            );
         } else {
            Render2D.text(TEXT_FONT, name, nameX, nameY, nameSize, ColorUtil.multAlpha(TEXT_COLOR, state.alpha));
         }

         ScissorUtil.pop();
      }

      this.renderStableTextRight(hp, hpRight, state.y + 6.8F, 5.5F, hpCellWidth, fadedHudColor);
      Render2D.rect(barX, barY, barWidth, 4.0F, 1.0F, barBackgroundColor, barBackgroundColor, barBackgroundColor, barBackgroundColor);
      Render2D.rect(barX, barY, barWidth * healthState.progress, 4.0F, 1.0F, fadedHudColor, fadedHudColor, fadedHudColor, fadedHudColor);
      if (absorptionProgress > 0.001F) {
         float healthWidth = barWidth * healthState.progress;
         float absorptionWidth = Math.min(barWidth * absorptionProgress, healthWidth);
         float absorptionX = barX + healthWidth - absorptionWidth;
         Render2D.rect(absorptionX, barY, absorptionWidth, 4.0F, 1.0F, absorptionColor, absorptionColor, absorptionColor, absorptionColor);
      }

      long now = System.currentTimeMillis();
      this.updateHitParticles(now);
      this.spawnHitParticlesOnDamage(state.target, barX + barWidth * healthState.progress, barY + 2.0F, now);
      this.drawHitParticles(now, state.alpha);
   }

   private void renderStableTextRight(String text, float rightX, float y, float size, float cellWidth, int color) {
      float x = rightX - (float)text.length() * cellWidth;

      for (int i = 0; i < text.length(); i++) {
         String character = charText(text.charAt(i));
         float characterWidth = Render2D.textWidth(TEXT_FONT, character, size);
         float characterX = x + (float)i * cellWidth + (cellWidth - characterWidth) * 0.5F;
         Render2D.text(TEXT_FONT, character, characterX, y, size, color);
      }
   }

   private TargetHud.HealthState animatedHealth(LivingEntity target, float health, float progress, float absorptionProgress) {
      boolean newTarget = target.getId() != this.animatedTargetId;
      boolean fullHealthCorrection = progress >= 0.995F && this.barAnimation.get() < 0.95F;
      if (!newTarget && !fullHealthCorrection) {
         this.healthAnimation.update();
         this.barAnimation.update();
         this.absorptionAnimation.update();
         this.healthAnimation.run((double)health, 0.34F, Easings.EXPO_OUT, true);
         this.barAnimation.run((double)progress, 0.34F, Easings.EXPO_OUT, true);
         this.absorptionAnimation.run((double)absorptionProgress, 0.34F, Easings.EXPO_OUT, true);
         return new TargetHud.HealthState(
            Math.max(0.0F, this.healthAnimation.get()), clamp(this.barAnimation.get(), 0.0F, 1.0F), clamp(this.absorptionAnimation.get(), 0.0F, 1.0F)
         );
      } else {
         this.animatedTargetId = target.getId();
         this.healthAnimation.set((double)health);
         this.barAnimation.set((double)progress);
         this.absorptionAnimation.set((double)absorptionProgress);
         return new TargetHud.HealthState(health, progress, absorptionProgress);
      }
   }

   private boolean renderSkinPart(String texture, float x, float y, float size, float u0, float v0, float u1, float v1, int color) {
      if (texture != null && !texture.isBlank() && color >>> 24 != 0) {
         Render2D.imageUvNearest(texture, x, y, size, size, 4.0F, 1.0F, u0, v0, u1, v1, color);
         return true;
      } else {
         return false;
      }
   }

   private float displayHealth(LivingEntity entity) {
      Float espHealth = ESP.resolveHudHealth(entity);
      return espHealth != null ? Math.max(0.0F, espHealth) : Math.max(0.0F, entity.getHealth());
   }

   private void spawnHitParticlesOnDamage(LivingEntity target, float x, float y, long now) {
      if (target.getId() != this.hitTargetId) {
         this.hitTargetId = target.getId();
         this.lastHurtTime = target.hurtTime;
      } else if (target.hurtTime <= this.lastHurtTime) {
         this.lastHurtTime = target.hurtTime;
      } else {
         this.lastHurtTime = target.hurtTime;
         Hud hud = Hud.getInstance();
         if (hud != null && hud.particles()) {
            for (int i = 0; i < hud.hitParticleCount(); i++) {
               float angle = (float)(this.random.nextDouble() * Math.PI * 2.0);
               float speed = 0.045F + this.random.nextFloat() * 0.08F;
               float velocityX = (float)Math.cos((double)angle) * speed;
               float velocityY = (float)Math.sin((double)angle) * speed;
               this.hitParticles.add(new TargetHud.HitParticle(x, y, velocityX, velocityY, now, 2.0F + this.random.nextFloat() * 2.0F));
            }

            if (this.hitParticles.size() > 1148) {
               this.hitParticles.subList(0, this.hitParticles.size() - 1148).clear();
            }
         }
      }
   }

   private void updateHitParticles(long now) {
      this.hitParticles.removeIf(particle -> particle.dead(now));
   }

   private void drawHitParticles(long now, float panelAlpha) {
      Hud hud = Hud.getInstance();
      int baseColor = hud != null ? hud.iconColor() : ColorUtil.rgba(247, 133, 255, 255);

      for (TargetHud.HitParticle particle : this.hitParticles) {
         float age = Math.min(1.0F, (float)(now - particle.spawnMs) / 420.0F);
         float alpha = (1.0F - age) * panelAlpha;
         float x = particle.x + particle.velocityX * (float)(now - particle.spawnMs);
         float y = particle.y + particle.velocityY * (float)(now - particle.spawnMs) + age * age * 7.0F;
         int color = ColorUtil.multAlpha(baseColor, alpha);
         Render2D.rect(x, y, particle.size * (1.0F - age * 0.4F), particle.size * (1.0F - age * 0.4F), particle.size, color);
      }
   }

   private String formatWholeHealth(float health) {
      return !Float.isFinite(health) ? "0" : Integer.toString(Math.max(0, Math.round(health)));
   }

   private LivingEntity target() {
      if (AuraModule.target != null && AuraModule.target.isAlive()) {
         return AuraModule.target;
      } else {
         if (this.mc.targetedEntity instanceof LivingEntity living && living.isAlive()) {
            return living;
         }

         return null;
      }
   }

   private static record HealthState(float health, float progress, float absorptionProgress) {
   }

   private static final class HitParticle {
      private final float x;
      private final float y;
      private final float velocityX;
      private final float velocityY;
      private final long spawnMs;
      private final float size;

      private HitParticle(float x, float y, float velocityX, float velocityY, long spawnMs, float size) {
         this.x = x;
         this.y = y;
         this.velocityX = velocityX;
         this.velocityY = velocityY;
         this.spawnMs = spawnMs;
         this.size = size;
      }

      private boolean dead(long now) {
         return now - this.spawnMs >= 420L;
      }
   }

   private static record TargetHudState(LivingEntity target, float alpha, float x, float y, float width, float height) {
   }
}
