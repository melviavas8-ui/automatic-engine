package daamky.ua.api.module.impl.visual;

import com.mojang.authlib.GameProfile;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.AttackEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.visual.frageffect.FragEffectDeathMemoryTracker;
import daamky.ua.api.module.impl.visual.frageffect.FragEffectEasing;
import daamky.ua.api.module.impl.visual.frageffect.FragEffectParticleSystem;
import daamky.ua.api.module.impl.visual.frageffect.FragEffectScanRenderer;
import daamky.ua.api.module.impl.visual.frageffect.FragEffectSoundQueue;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ButtonSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.RenderCompatibility;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.repository.friend.FriendUtils;
import daamky.ua.utils.sounds.SoundManager;

public class KillEffect extends Module {
   private static final long BASE_EFFECT_DURATION_MS = 5200L;
   private static final long LEGACY_EFFECT_DURATION_MS = 3000L;
   private static final String VIGNETTE_TEXTURE = "daamky:textures/effects/frag/lightarroundscreen_alpha.png";
   private static final float LEGACY_CROSS_HEIGHT = 3.0F;
   private static final float LEGACY_CROSS_WIDTH = 5.0F;
   private static final float LEGACY_CROSS_ARM_LENGTH = 1.0F;
   private static final float LEGACY_CROSS_Y_OFFSET = 2.3F;
   private static final float LEGACY_SOUL_RISE = 3.0F;
   private static final float LEGACY_SOUL_START_PITCH = -30.0F;
   private static KillEffect instance;
   private final FragEffectSoundQueue soundQueue = new FragEffectSoundQueue();
   private final FragEffectDeathMemoryTracker deathMemoryTracker = new FragEffectDeathMemoryTracker();
   private final FragEffectParticleSystem particleSystem = new FragEffectParticleSystem();
   private final List<KillEffect.LegacyEffectEntry> legacyEffects = new ArrayList<>();
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Kill effect mode.", "Frag", "Frag", "Legacy"));
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Controls effect playback speed.", 100.0, 25.0, 200.0, 1.0));
   private final ColorSetting effectColor = this.register(new ColorSetting("Color", "Primary kill effect color.", new Color(-50116, true)));
   private final MultiModeSetting effectTargets = this.register(
      new MultiModeSetting(
         "Targets",
         "Entities that can trigger the kill effect.",
         new String[]{"Players", "Friends", "Mobs", "Animals"},
         "Players",
         "Friends",
         "Mobs",
         "Animals"
      )
   );
   private final BooleanSetting screenOverlay = this.register(new BooleanSetting("Screen Overlay", "Shows the screen vignette during the kill effect.", true));
   private final BooleanSetting scanlines = this.register(new BooleanSetting("Scanlines", "Shows scan stripes inside the wave.", true));
   private final BooleanSetting changeSaturation = this.register(
      new BooleanSetting("Change Saturation", "Temporarily lowers world saturation during the effect.", false)
   );
   private final ModeSetting legacyType = this.register(new ModeSetting("Legacy Type", "Legacy effect style.", "Soul", "Cross", "Soul"));
   private final BooleanSetting legacyPreview = new BooleanSetting("Legacy Preview", "Continuously shows the legacy effect for testing.", false);
   private final ButtonSetting legacyTest = new ButtonSetting("Legacy Test", "Spawns one legacy effect for testing.", this::spawnLegacyPreviewEffect);
   private final BooleanSetting legacySound = this.register(new BooleanSetting("Legacy Sound", "Plays a sound for the legacy effect.", true));
   private final NumberSetting legacyVolume = this.register(new NumberSetting("Legacy Volume", "Legacy effect sound volume.", 100.0, 0.0, 100.0, 1.0));
   private boolean animate;
   private long effectStartMillis = System.currentTimeMillis();
   private float activeEffectSpeedMultiplier = 1.0F;
   private int nextLegacyFakeEntityId = -2200000;

   public KillEffect() {
      super("Kill Effect", "Kill effect with vignette, scan wave, particles and sounds.", ModuleCategory.VISUAL);
      instance = this;
      this.speed.visibleWhen(this::isFragMode);
      this.effectColor.visibleWhen(this::isFragMode);
      this.screenOverlay.visibleWhen(this::isFragMode);
      this.scanlines.visibleWhen(this::isFragMode);
      this.changeSaturation.visibleWhen(this::isFragMode);
      this.legacyType.visibleWhen(this::isLegacyMode);
      this.legacyPreview.visibleWhen(this::isLegacyMode);
      this.legacyTest.visibleWhen(this::isLegacyMode);
      this.legacySound.visibleWhen(this::isLegacyMode);
      this.legacyVolume.visibleWhen(this::isLegacyMode);
      this.mode.addListener((setting, oldValue, newValue) -> this.resetState());
   }

   public static KillEffect getInstance() {
      return instance;
   }

   public static KillEffect getInstanceIfReady() {
      ModuleManager modules = Manager.getModules();
      return modules == null ? null : modules.getByType(KillEffect.class).orElse(instance);
   }

   public static boolean isLegacySoulFake(Entity entity) {
      KillEffect module = getInstanceIfReady();
      return module != null && module.isTrackedLegacySoulFake(entity);
   }

   public static boolean isLegacySoulFake(int entityId) {
      KillEffect module = getInstanceIfReady();
      return module != null && module.isTrackedLegacySoulFake(entityId);
   }

   public static float getLegacySoulProgress(int entityId) {
      KillEffect module = getInstanceIfReady();
      return module != null && entityId != 0 ? module.getTrackedLegacySoulProgress(entityId) : 1.0F;
   }

   @SubscribeEvent
   private void onAttack(AttackEvent event) {
      if (event.getTarget() instanceof LivingEntity target) {
         if (target.isAlive() && !(target.getHealth() <= 0.0F) && this.shouldTriggerFor(target)) {
            this.deathMemoryTracker.remember(target, true);
         }
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      if (mc.player != null && mc.world != null) {
         LivingEntity auraTarget = AuraModule.target;
         if (auraTarget != null && auraTarget.isAlive() && auraTarget.getHealth() > 0.0F && this.shouldTriggerFor(auraTarget)) {
            this.deathMemoryTracker.remember(auraTarget, true);
         }

         if (this.isLegacyMode() && this.legacyPreview.getValue() && this.legacyEffects.isEmpty()) {
            this.spawnLegacyPreviewEffect();
         }

         this.deathMemoryTracker.tick(this::onTrackedKill);
         this.updateLegacyEffects();
         this.pruneLegacyEffects();
         if (this.isFragMode()) {
            this.particleSystem.tick();
            this.soundQueue.tick();
         } else {
            this.soundQueue.clear();
         }
      } else {
         this.resetState();
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (this.isFragMode()) {
         this.particleSystem.render(event.getPartialTicks());
      } else {
         this.renderLegacyEffects(event);
      }
   }

   @SubscribeEvent
   private void onDraw(DrawEvent event) {
      if (this.isFragMode()) {
         this.drawVignette();
      }
   }

   @Override
   protected void onDisable() {
      this.resetState();
   }

   public void onAfterTranslucent(Framebuffer renderTarget, Matrix4f projectionMatrix, Matrix4f positionMatrix, Vec3d cameraPos) {
      if (this.isEnabled() && this.isFragMode()) {
         if (!RenderCompatibility.shouldDisableFragEffectScanShader() && !FragEffectScanRenderer.isDisabledAfterError()) {
            FragEffectScanRenderer.render(
               renderTarget,
               projectionMatrix,
               positionMatrix,
               cameraPos,
               this.activeEffectSpeedMultiplier,
               this.c1(),
               this.c2(),
               this.c3(),
               this.c4(),
               this.c5(),
               this.c6(),
               this.c7(),
               this.c8()
            );
         }
      }
   }

   public static float getWorldSaturationMultiplier() {
      KillEffect module = getInstanceIfReady();
      if (module != null && module.isEnabled() && module.isFragMode() && module.changeSaturation.getValue()) {
         float pulse = module.transientEnvelope(1950L, 260L, 760L);
         return pulse <= 0.0F ? 1.0F : Math.clamp(1.0F - pulse, 0.0F, 1.0F);
      } else {
         return 1.0F;
      }
   }

   private void drawVignette() {
      if (this.screenOverlay.getValue()) {
         float effectProgress = this.effectProgress();
         if (!(effectProgress <= 0.0F)) {
            float alphaProgress = this.effectEnvelope(380L, 1100L);
            if (!(alphaProgress <= 0.0F)) {
               float scaleAnimation = FragEffectEasing.quintOut(effectProgress);
               float scaleProgress = Math.min((scaleAnimation > 0.5F ? 1.0F - scaleAnimation : scaleAnimation) * 4.25F, 1.0F);
               float width = (float)mc.getWindow().getScaledWidth();
               float height = (float)mc.getWindow().getScaledHeight();
               float extendX = width * 0.5F * (1.0F - scaleProgress);
               float extendY = height * 0.5F * (1.0F - scaleProgress);
               int color = ColorUtil.withAlpha(
                  ColorUtil.interpolateColor(lighten(this.accent(), 0.22F), ColorUtil.WHITE, effectProgress), Math.round(80.0F * alphaProgress)
               );
               Render2D.image(
                  "daamky:textures/effects/frag/lightarroundscreen_alpha.png", -extendX, -extendY, width + extendX * 2.0F, height + extendY * 2.0F, 0.0F, color
               );
            }
         }
      }
   }

   private void onTrackedKill(LivingEntity entity) {
      if (this.matchesEffectTarget(entity)) {
         if (this.isFragMode()) {
            this.triggerFragEffect(entity);
         } else {
            this.spawnLegacyEffect(entity, true);
         }
      }
   }

   private void triggerFragEffect(LivingEntity entity) {
      Vec3d killPosition = entity.getEyePos();
      float effectSpeed = this.speedMultiplier();
      this.restartEffect(effectSpeed);
      FragEffectScanRenderer.ping(killPosition, effectSpeed);
      this.particleSystem.spawnBurst(killPosition, 480, 900, 1.9F, 22, 0.012F);
      this.scheduleCharmSounds();
   }

   private void spawnLegacyPreviewEffect() {
      if (mc.player != null && mc.world != null) {
         this.spawnLegacyEffect(mc.player, false);
      }
   }

   private void spawnLegacyEffect(LivingEntity entity, boolean playSound) {
      if (mc.world != null && entity != null) {
         KillEffect.LegacyStyle style = this.legacyType.is("Soul") && entity instanceof PlayerEntity
            ? KillEffect.LegacyStyle.SOUL
            : KillEffect.LegacyStyle.CROSS;
         OtherClientPlayerEntity fakePlayer = style == KillEffect.LegacyStyle.SOUL ? this.createLegacyFakePlayer((PlayerEntity)entity) : null;
         this.legacyEffects.add(new KillEffect.LegacyEffectEntry(System.currentTimeMillis(), entity.getYaw(), entity.getEntityPos(), style, fakePlayer));
         if (playSound && this.legacySound.getValue()) {
            float volume = (float)(this.legacyVolume.getValue() / 100.0);
            mc.world.playSound(mc.player, BlockPos.ofFloored(entity.getEntityPos()), SoundManager.FRAG_EFFECT_ECHO_MAIN, SoundCategory.BLOCKS, volume, 1.0F);
         }
      }
   }

   private OtherClientPlayerEntity createLegacyFakePlayer(PlayerEntity player) {
      if (mc.world != null && mc.player != null) {
         GameProfile profile = new GameProfile(UUID.randomUUID(), player.getGameProfile().name());
         OtherClientPlayerEntity fake = new OtherClientPlayerEntity(mc.world, profile);
         fake.setId(this.nextLegacyFakeEntityId--);
         fake.copyPositionAndRotation(player);
         fake.setYaw(player.getYaw());
         fake.setPitch(-30.0F);
         fake.setHeadYaw(player.getHeadYaw());
         fake.setBodyYaw(player.getYaw());
         fake.setPose(player.getPose());
         fake.setSprinting(player.isSprinting());
         fake.setSneaking(player.isSneaking());
         fake.setNoGravity(true);
         fake.setSilent(true);
         fake.setInvisible(true);
         fake.setHealth(Math.max(player.getHealth(), 1.0F));
         fake.setAbsorptionAmount(player.getAbsorptionAmount());
         mc.world.addEntity(fake);
         return fake;
      } else {
         return null;
      }
   }

   private boolean isTrackedLegacySoulFake(Entity entity) {
      return entity != null && this.isTrackedLegacySoulFake(entity.getId());
   }

   private boolean isTrackedLegacySoulFake(int entityId) {
      if (entityId == 0) {
         return false;
      } else {
         for (KillEffect.LegacyEffectEntry effect : this.legacyEffects) {
            if (effect.style() == KillEffect.LegacyStyle.SOUL && effect.fakePlayer() != null && effect.fakePlayer().getId() == entityId) {
               return true;
            }
         }

         return false;
      }
   }

   private float getTrackedLegacySoulProgress(int entityId) {
      for (KillEffect.LegacyEffectEntry effect : this.legacyEffects) {
         if (effect.style() == KillEffect.LegacyStyle.SOUL && effect.fakePlayer() != null && effect.fakePlayer().getId() == entityId) {
            return effect.progress();
         }
      }

      return 1.0F;
   }

   private void renderLegacyEffects(WorldRenderEvent event) {
      if (!this.legacyEffects.isEmpty()) {
         for (KillEffect.LegacyEffectEntry effect : this.legacyEffects) {
            float progress = effect.progress();
            if (!(progress >= 1.0F) && (effect.style() == KillEffect.LegacyStyle.CROSS || effect.fakePlayer() == null)) {
               this.renderLegacyCross(effect, progress);
            }
         }
      }
   }

   private void renderLegacyCross(KillEffect.LegacyEffectEntry effect, float progress) {
      int alpha = Math.max(0, Math.min(255, Math.round(150.0F * (1.0F - progress))));
      int color = new Color(255, 255, 255, alpha).getRGB();
      float yaw = (float)Math.toRadians((double)(effect.yaw() + 95.0F));
      Vec3d pos = effect.startPos();
      Render3D.drawLine(pos, pos.add(0.0, 3.0, 0.0), color, 5.0F, true);
      Vec3d start = pos.add(-1.0 * Math.sin((double)yaw), 2.3F, 1.0 * Math.cos((double)yaw));
      Vec3d end = pos.add(1.0 * Math.sin((double)yaw), 2.3F, -1.0 * Math.cos((double)yaw));
      Render3D.drawLine(start, end, color, 5.0F, true);
   }

   private void updateLegacyEffects() {
      if (!this.legacyEffects.isEmpty()) {
         for (KillEffect.LegacyEffectEntry effect : this.legacyEffects) {
            OtherClientPlayerEntity fakePlayer = effect.fakePlayer();
            if (effect.style() == KillEffect.LegacyStyle.SOUL && fakePlayer != null) {
               float progress = effect.progress();
               Vec3d soulPos = effect.startPos().add(0.0, (double)(progress * 3.0F), 0.0);
               fakePlayer.setPosition(soulPos.x, soulPos.y, soulPos.z);
               fakePlayer.setYaw(effect.yaw());
               fakePlayer.setHeadYaw(effect.yaw());
               fakePlayer.setBodyYaw(effect.yaw());
               fakePlayer.age = mc.player != null ? mc.player.age : fakePlayer.age;
            }
         }
      }
   }

   private void pruneLegacyEffects() {
      if (!this.legacyEffects.isEmpty()) {
         Iterator<KillEffect.LegacyEffectEntry> iterator = this.legacyEffects.iterator();

         while (iterator.hasNext()) {
            KillEffect.LegacyEffectEntry effect = iterator.next();
            if (effect.isExpired()) {
               this.disposeLegacyEffect(effect);
               iterator.remove();
            }
         }
      }
   }

   private void disposeLegacyEffect(KillEffect.LegacyEffectEntry effect) {
      if (mc.world != null && effect != null && effect.fakePlayer() != null) {
         mc.world.removeEntity(effect.fakePlayer().getId(), RemovalReason.DISCARDED);
         effect.fakePlayer().discard();
      }
   }

   private void clearLegacyEffects() {
      if (!this.legacyEffects.isEmpty()) {
         for (KillEffect.LegacyEffectEntry effect : this.legacyEffects) {
            this.disposeLegacyEffect(effect);
         }

         this.legacyEffects.clear();
      }
   }

   private void scheduleCharmSounds() {
      long offset = this.scaleDuration(150L);
      this.soundQueue.schedule(SoundManager.FRAG_EFFECT_PULSE, 0L, 1.0F);
      this.soundQueue.schedule(SoundManager.FRAG_EFFECT_KNOCK_MAIN, offset, 1.0F);
      this.soundQueue.schedule(SoundManager.FRAG_EFFECT_SPARKS_COLLISION, offset * 2L, 0.2F);
      this.soundQueue.schedule(SoundManager.FRAG_EFFECT_ECHO_MAIN, offset * 3L, 0.6F);
   }

   private void restartEffect(float effectSpeedMultiplier) {
      this.animate = true;
      this.activeEffectSpeedMultiplier = Math.clamp(effectSpeedMultiplier, 0.25F, 2.0F);
      this.effectStartMillis = System.currentTimeMillis();
   }

   private boolean shouldTriggerFor(LivingEntity entity) {
      return entity != null && entity != mc.player && entity.isAlive() && entity.getHealth() > 0.0F && this.matchesEffectTarget(entity);
   }

   private boolean matchesEffectTarget(LivingEntity entity) {
      if (entity == null || entity == mc.player) {
         return false;
      } else if (entity instanceof PlayerEntity player) {
         return FriendUtils.isFriend(player) ? this.effectTargets.isSelected("Friends") : this.effectTargets.isSelected("Players");
      } else if (entity instanceof AnimalEntity) {
         return this.effectTargets.isSelected("Animals");
      } else {
         return entity instanceof MobEntity ? this.effectTargets.isSelected("Mobs") : false;
      }
   }

   private boolean isFragMode() {
      return this.mode.is("Frag");
   }

   private boolean isLegacyMode() {
      return this.mode.is("Legacy");
   }

   private float effectProgress() {
      long duration = this.effectDurationMs();
      float progress = this.animate ? Math.min((float)this.elapsedMillis() / (float)duration, 1.0F) : 0.0F;
      if (progress >= 1.0F) {
         this.animate = false;
      }

      return progress;
   }

   private float effectEnvelope(long attackMs, long releaseMs) {
      if (!this.animate) {
         return 0.0F;
      } else {
         long duration = this.effectDurationMs();
         long elapsed = Math.min(this.elapsedMillis(), duration);
         if (elapsed <= 0L) {
            return 0.0F;
         } else {
            long attack = Math.clamp(this.scaleDuration(attackMs), 1L, duration);
            long release = Math.clamp(this.scaleDuration(releaseMs), 1L, duration);
            long releaseStart = Math.max(duration - release, attack);
            if (elapsed < attack) {
               return FragEffectEasing.sineOut((float)elapsed / (float)attack);
            } else if (elapsed >= releaseStart) {
               float releaseProgress = Math.clamp((float)(elapsed - releaseStart) / (float)release, 0.0F, 1.0F);
               return 1.0F - FragEffectEasing.sineInOut(releaseProgress);
            } else {
               return 1.0F;
            }
         }
      }
   }

   private float transientEnvelope(long totalMs, long attackMs, long releaseMs) {
      if (!this.animate) {
         return 0.0F;
      } else {
         long duration = this.effectDurationMs();
         long total = Math.clamp(this.scaleDuration(totalMs), 1L, duration);
         long elapsed = Math.min(this.elapsedMillis(), total);
         if (elapsed <= 0L) {
            return 0.0F;
         } else {
            long attack = Math.clamp(this.scaleDuration(attackMs), 1L, total);
            long release = Math.clamp(this.scaleDuration(releaseMs), 1L, total);
            long releaseStart = Math.max(total - release, attack);
            if (elapsed < attack) {
               return FragEffectEasing.sineOut((float)elapsed / (float)attack);
            } else if (elapsed >= releaseStart) {
               float releaseProgress = Math.clamp((float)(elapsed - releaseStart) / (float)release, 0.0F, 1.0F);
               return 1.0F - FragEffectEasing.sineInOut(releaseProgress);
            } else {
               return 1.0F;
            }
         }
      }
   }

   private void resetState() {
      this.animate = false;
      this.activeEffectSpeedMultiplier = this.speedMultiplier();
      this.soundQueue.clear();
      this.deathMemoryTracker.clear();
      this.particleSystem.clear();
      this.clearLegacyEffects();
      FragEffectScanRenderer.clear();
   }

   private long elapsedMillis() {
      return System.currentTimeMillis() - this.effectStartMillis;
   }

   private int accent() {
      Color color = this.effectColor.getValue();
      return ColorUtil.rgba(color.getRed(), color.getGreen(), color.getBlue(), 255);
   }

   private float speedMultiplier() {
      return Math.clamp(this.speed.getFloat() / 100.0F, 0.25F, 2.0F);
   }

   private long effectDurationMs() {
      return this.scaleDuration(5200L);
   }

   private long scaleDuration(long durationMs) {
      return this.scaleDuration(durationMs, this.activeEffectSpeedMultiplier);
   }

   private long scaleDuration(long durationMs, float speedMultiplier) {
      return durationMs <= 0L ? 0L : Math.max(1L, (long)Math.round((float)durationMs / Math.clamp(speedMultiplier, 0.25F, 2.0F)));
   }

   private int c1() {
      return lighten(this.accent(), 0.45F);
   }

   private int c2() {
      return lighten(this.accent(), 0.18F);
   }

   private int c3() {
      return darken(this.accent(), 0.18F);
   }

   private int c4() {
      return this.scanlines.getValue() ? lighten(this.accent(), 0.6F) : 0;
   }

   private int c5() {
      return lighten(this.accent(), 0.7F);
   }

   private int c6() {
      return ColorUtil.interpolateColor(this.accent(), ColorUtil.WHITE, 0.28F);
   }

   private int c7() {
      return darken(this.accent(), 0.08F);
   }

   private int c8() {
      return this.scanlines.getValue() ? darken(this.accent(), 0.35F) : 0;
   }

   private static int lighten(int color, float amount) {
      float clamped = Math.clamp(amount, 0.0F, 1.0F);
      int red = ColorUtil.getRed(color);
      int green = ColorUtil.getGreen(color);
      int blue = ColorUtil.getBlue(color);
      int alpha = ColorUtil.getAlpha(color);
      return ColorUtil.rgba(
         Math.clamp((long)Math.round((float)red + (float)(255 - red) * clamped), 0, 255),
         Math.clamp((long)Math.round((float)green + (float)(255 - green) * clamped), 0, 255),
         Math.clamp((long)Math.round((float)blue + (float)(255 - blue) * clamped), 0, 255),
         alpha
      );
   }

   private static int darken(int color, float amount) {
      float factor = 1.0F - Math.clamp(amount, 0.0F, 1.0F);
      return ColorUtil.rgba(
         Math.clamp((long)Math.round((float)ColorUtil.getRed(color) * factor), 0, 255),
         Math.clamp((long)Math.round((float)ColorUtil.getGreen(color) * factor), 0, 255),
         Math.clamp((long)Math.round((float)ColorUtil.getBlue(color) * factor), 0, 255),
         ColorUtil.getAlpha(color)
      );
   }

   private static record LegacyEffectEntry(long startedAt, float yaw, Vec3d startPos, KillEffect.LegacyStyle style, OtherClientPlayerEntity fakePlayer) {
      private float progress() {
         return Math.clamp((float)(System.currentTimeMillis() - this.startedAt) / 3000.0F, 0.0F, 1.0F);
      }

      private boolean isExpired() {
         return System.currentTimeMillis() - this.startedAt >= 3000L;
      }
   }

   private static enum LegacyStyle {
      CROSS,
      SOUL;

      private LegacyStyle() {
      }
   }
}
