package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4fc;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;
import daamky.ua.utils.render.post.jumpcircle.JumpCircleDistortionRenderer;

public final class JumpCircles extends Module {
   private static final int SEGMENTS = 40;
   private static final int TUBE_SIDES = 6;
   private static final int SOFT_EFFECT_LIMIT = 6;
   private static final int HARD_EFFECT_LIMIT = 12;
   private static final long RETIRE_FADE_MS = 280L;
   private static JumpCircles instance;
   private final BooleanSetting self = this.register(new BooleanSetting("Self", "Show jump waves beneath your player.", true));
   private final BooleanSetting players = this.register(new BooleanSetting("Players", "Show jump waves beneath other players.", true));
   private final ModeSetting style = this.register(
      new ModeSetting("Style", "Procedural shape of the volumetric jump circle.", "Waves", "Waves", "Crown", "Bloom", "Rift", "Refraction")
   );
   private final ColorSetting primaryColor = this.register(new ColorSetting("Primary Color", "Main color of the energy ribbons.", new Color(255, 84, 28, 255)));
   private final ColorSetting secondaryColor = this.register(
      new ColorSetting("Secondary Color", "Color flowing through the ribbon highlights.", new Color(255, 218, 112, 255))
   );
   private final NumberSetting size = this.register(new NumberSetting("Size", "Maximum radius of the jump wave.", 1.65, 0.75, 3.0, 0.05));
   private final NumberSetting lifetime = this.register(new NumberSetting("Lifetime", "How long a jump wave remains in the world.", 1.35, 0.55, 3.0, 0.05));
   private final NumberSetting strands = this.register(new NumberSetting("Ribbons", "Number of intertwined energy ribbons.", 3.0, 2.0, 5.0, 1.0));
   private final NumberSetting ribbonWidth = this.register(new NumberSetting("Ribbon Width", "Width of every translucent ribbon.", 0.14, 0.04, 0.3, 0.005));
   private final NumberSetting volume = this.register(
      new NumberSetting("Volume", "Height of sharp crests and depth of the energy ribbons.", 0.52, 0.08, 1.1, 0.01)
   );
   private final NumberSetting glow = this.register(new NumberSetting("Glow", "Strength of the soft light around the ribbons.", 1.0, 0.0, 2.0, 0.05));
   private final NumberSetting animationSpeed = this.register(
      new NumberSetting("Animation Speed", "Speed of the flowing waves and intertwined motion.", 1.0, 0.25, 2.5, 0.05)
   );
   private final NumberSetting distortion = this.register(
      new NumberSetting("Distortion", "World-space refraction inside and around every jump circle.", 1.0, 0.0, 2.5, 0.05)
   );
   private final NumberSetting groundLight = this.register(
      new NumberSetting("Ground Light", "Colored light projected by the circle onto nearby surfaces.", 0.85, 0.0, 2.0, 0.05)
   );
   private final Map<Integer, JumpCircles.PlayerJumpState> playerStates = new HashMap<>();
   private final List<JumpCircles.JumpWave> waves = new ArrayList<>();
   private final Set<Integer> seenPlayers = new HashSet<>();
   private final Random random = new Random();

   public JumpCircles() {
      super("Jump Circles", "Creates volumetric intertwined energy ribbons whenever a player jumps.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static JumpCircles getInstance() {
      return instance;
   }

   @Override
   protected void onEnable() {
      this.playerStates.clear();
      this.waves.clear();
   }

   @Override
   protected void onDisable() {
      this.playerStates.clear();
      this.waves.clear();
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client != null && client.world != null && client.player != null) {
         this.seenPlayers.clear();

         for (PlayerEntity player : client.world.getPlayers()) {
            if (this.shouldTrack(player, client)) {
               this.seenPlayers.add(player.getId());
               this.trackJump(player);
            }
         }

         this.playerStates.keySet().removeIf(id -> !this.seenPlayers.contains(id));
         long now = System.currentTimeMillis();
         this.waves.removeIf(wave -> wave.expired(now, this.lifetimeMillis()));
      } else {
         this.playerStates.clear();
         this.waves.clear();
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (!this.waves.isEmpty() && mc.gameRenderer != null) {
         long now = System.currentTimeMillis();
         long duration = this.lifetimeMillis();
         this.waves.removeIf(wavex -> wavex.expired(now, duration));
         if (!this.waves.isEmpty()) {
            MatrixStack stack = event.getStack();
            Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
            Vec3d camera = mc.gameRenderer.getCamera().getCameraPos();
            VertexConsumer bodyConsumer = provider.getBuffer(ClientPipelines.TRAILS_ALPHA);

            for (JumpCircles.JumpWave wave : this.waves) {
               float progress = MathHelper.clamp((float)(now - wave.spawnTime) / (float)duration, 0.0F, 1.0F);
               float alpha = envelope(progress) * wave.retirementAlpha(now);
               if (!(alpha <= 0.006F)) {
                  stack.push();
                  stack.translate(wave.origin.x - camera.x, wave.origin.y - camera.y, wave.origin.z - camera.z);
                  this.renderWave(stack, bodyConsumer, wave, progress, alpha, JumpCircles.RenderPass.BODY);
                  stack.pop();
               }
            }

            provider.draw(ClientPipelines.TRAILS_ALPHA);
            if (!(this.glow.getFloat() <= 0.01F)) {
               VertexConsumer glowConsumer = provider.getBuffer(ClientPipelines.CRYSTAL_GLOW);

               for (JumpCircles.JumpWave wavex : this.waves) {
                  float progress = MathHelper.clamp((float)(now - wavex.spawnTime) / (float)duration, 0.0F, 1.0F);
                  float alpha = envelope(progress) * wavex.retirementAlpha(now);
                  if (!(alpha <= 0.006F)) {
                     stack.push();
                     stack.translate(wavex.origin.x - camera.x, wavex.origin.y - camera.y, wavex.origin.z - camera.z);
                     this.renderWave(stack, glowConsumer, wavex, progress, alpha, JumpCircles.RenderPass.GLOW);
                     stack.pop();
                  }
               }

               provider.draw(ClientPipelines.CRYSTAL_GLOW);
            }
         }
      }
   }

   public void onAfterWorldRender(Framebuffer target, Matrix4fc projectionMatrix, Matrix4fc viewMatrix, Vec3d cameraPosition) {
      if (this.isEnabled() && !this.waves.isEmpty()) {
         long now = System.currentTimeMillis();
         long duration = this.lifetimeMillis();
         this.waves.removeIf(wavex -> wavex.expired(now, duration));
         if (!this.waves.isEmpty()) {
            int first = Math.max(0, this.waves.size() - 8);
            List<JumpCircleDistortionRenderer.Circle> activeCircles = new ArrayList<>(this.waves.size() - first);
            Color firstColor = this.primaryColor.getValue();
            Color secondColor = this.secondaryColor.getValue();
            float red = ((float)firstColor.getRed() * 0.58F + (float)secondColor.getRed() * 0.42F) / 255.0F;
            float green = ((float)firstColor.getGreen() * 0.58F + (float)secondColor.getGreen() * 0.42F) / 255.0F;
            float blue = ((float)firstColor.getBlue() * 0.58F + (float)secondColor.getBlue() * 0.42F) / 255.0F;
            float styleCode = this.styleCode();

            for (int index = first; index < this.waves.size(); index++) {
               JumpCircles.JumpWave wave = this.waves.get(index);
               float progress = MathHelper.clamp((float)(now - wave.spawnTime) / (float)duration, 0.0F, 1.0F);
               float alpha = envelope(progress) * wave.retirementAlpha(now);
               if (!(alpha <= 0.006F)) {
                  float thickness = this.ribbonWidth.getFloat() * (this.style.is("Refraction") ? 0.78F : 1.28F);
                  activeCircles.add(
                     new JumpCircleDistortionRenderer.Circle(
                        wave.origin, this.animatedRadius(progress), progress, alpha, Math.max(0.035F, thickness), wave.seedPhase, red, green, blue, styleCode
                     )
                  );
               }
            }

            JumpCircleDistortionRenderer.apply(
               target, projectionMatrix, viewMatrix, cameraPosition, activeCircles, this.distortion.getFloat(), this.groundLight.getFloat()
            );
         }
      }
   }

   private void trackJump(PlayerEntity player) {
      int id = player.getId();
      JumpCircles.PlayerJumpState state = this.playerStates.get(id);
      boolean grounded = player.isOnGround();
      double y = player.getY();
      if (state == null) {
         this.playerStates.put(id, new JumpCircles.PlayerJumpState(grounded, y, y));
      } else {
         if (grounded) {
            state.lastGroundY = y;
         } else if (state.grounded) {
            double rise = Math.max(player.getVelocity().y, y - state.lastY);
            if (rise > 0.055) {
               this.spawnWave(new Vec3d(player.getX(), state.lastGroundY + 0.035, player.getZ()));
            }
         }

         state.grounded = grounded;
         state.lastY = y;
      }
   }

   private void spawnWave(Vec3d position) {
      long now = System.currentTimeMillis();
      this.waves.removeIf(wavex -> wavex.expired(now, this.lifetimeMillis()));
      if (this.waves.size() >= 6) {
         for (JumpCircles.JumpWave wave : this.waves) {
            if (!wave.isRetiring()) {
               wave.beginRetirement(now);
               break;
            }
         }
      }

      while (this.waves.size() >= 12) {
         this.waves.remove(0);
      }

      this.waves.add(JumpCircles.JumpWave.create(position, now, this.random));
   }

   private void renderWave(MatrixStack stack, VertexConsumer consumer, JumpCircles.JumpWave wave, float progress, float alpha, JumpCircles.RenderPass pass) {
      int ribbonCount = this.style.is("Refraction")
         ? Math.clamp((long)((int)Math.round(this.strands.getValue())), 2, 3)
         : Math.clamp((long)((int)Math.round(this.strands.getValue())), 2, 5);
      float radius = this.animatedRadius(progress);
      float volumeReveal = smoothStep(0.015F, 0.2F, progress);
      float halfWidth = this.ribbonWidth.getFloat() * (0.28F + 0.72F * volumeReveal) * (0.78F + 0.22F * alpha) * (this.style.is("Refraction") ? 0.58F : 1.0F);
      float waveHeight = this.style.is("Refraction") ? Math.min(0.1F, this.volume.getFloat() * 0.16F) * volumeReveal : this.volume.getFloat() * volumeReveal;
      float speed = this.animationSpeed.getFloat();
      float time = progress * (float) (Math.PI * 2) * speed;
      float glowStrength = this.glow.getFloat();

      for (int ribbon = 0; ribbon < ribbonCount; ribbon++) {
         float strandPhase = wave.seedPhase + (float) (Math.PI * 2) * (float)ribbon / (float)ribbonCount;
         float centeredRibbon = (float)ribbon - (float)(ribbonCount - 1) * 0.5F;
         float orbitOffset = radius * 0.018F;
         float offsetX = MathHelper.cos((double)strandPhase) * orbitOffset;
         float offsetZ = MathHelper.sin((double)strandPhase) * orbitOffset;
         float strandWidth = halfWidth * (0.88F + 0.18F * MathHelper.sin((double)(strandPhase + time * 0.65F)));
         float radialLane = centeredRibbon * halfWidth * (this.style.is("Refraction") ? 2.35F : 1.28F);

         for (int segment = 0; segment < 40; segment++) {
            float thetaA = (float) (Math.PI * 2) * (float)segment / 40.0F;
            float thetaB = (float) (Math.PI * 2) * (float)(segment + 1) / 40.0F;
            float pulseA = flowingPulse(thetaA, strandPhase, time);
            float pulseB = flowingPulse(thetaB, strandPhase, time);
            float opacityA = this.organicOpacity(wave, ribbon, thetaA, time);
            float opacityB = this.organicOpacity(wave, ribbon, thetaB, time);
            float tubeRadius = Math.max(0.018F, (strandWidth * 0.56F + waveHeight * 0.045F) * (0.32F + 0.68F * volumeReveal));
            if (pass == JumpCircles.RenderPass.BODY) {
               int bodyA = this.ribbonColor(ribbon, thetaA, time, alpha * opacityA * (0.34F + pulseA * 0.16F));
               int bodyB = this.ribbonColor(ribbon, thetaB, time, alpha * opacityB * (0.34F + pulseB * 0.16F));
               JumpCircles.RibbonPoint bodyPointA = this.point(
                  thetaA, radius, radialLane, strandWidth, waveHeight, strandPhase, time, offsetX, offsetZ, wave, ribbon
               );
               JumpCircles.RibbonPoint bodyPointB = this.point(
                  thetaB, radius, radialLane, strandWidth, waveHeight, strandPhase, time, offsetX, offsetZ, wave, ribbon
               );
               this.emitTube(stack, consumer, bodyPointA, bodyPointB, tubeRadius, bodyA, bodyB, false);
               this.emitCurtains(stack, consumer, bodyPointA, bodyPointB, tubeRadius, bodyA, bodyB);
            } else {
               float glowWidth = strandWidth * (1.28F + glowStrength * 0.32F);
               float glowHeight = tubeRadius * (1.2F + glowStrength * 0.16F);
               JumpCircles.RibbonPoint glowPointA = this.point(
                  thetaA, radius, radialLane, glowWidth, waveHeight, strandPhase, time, offsetX, offsetZ, wave, ribbon
               );
               JumpCircles.RibbonPoint glowPointB = this.point(
                  thetaB, radius, radialLane, glowWidth, waveHeight, strandPhase, time, offsetX, offsetZ, wave, ribbon
               );
               int haloA = this.ribbonColor(ribbon, thetaA, time, alpha * opacityA * glowStrength * 0.052F);
               int haloB = this.ribbonColor(ribbon, thetaB, time, alpha * opacityB * glowStrength * 0.052F);
               this.emitTube(stack, consumer, glowPointA, glowPointB, glowHeight, haloA, haloB, true);
               float coreWidth = Math.max(0.008F, strandWidth * 0.17F);
               JumpCircles.RibbonPoint coreA = this.point(thetaA, radius, radialLane, coreWidth, waveHeight, strandPhase, time, offsetX, offsetZ, wave, ribbon);
               JumpCircles.RibbonPoint coreB = this.point(thetaB, radius, radialLane, coreWidth, waveHeight, strandPhase, time, offsetX, offsetZ, wave, ribbon);
               int coreAColor = this.ribbonColor(ribbon, thetaA, time, alpha * opacityA * (0.5F + pulseA * 0.25F));
               int coreBColor = this.ribbonColor(ribbon, thetaB, time, alpha * opacityB * (0.5F + pulseB * 0.25F));
               this.emitTopAtHeight(stack, consumer, coreA, coreB, tubeRadius * 0.88F, coreAColor, coreBColor);
            }
         }
      }
   }

   private JumpCircles.RibbonPoint point(
      float theta,
      float radius,
      float radiusOffset,
      float halfWidth,
      float waveHeight,
      float strandPhase,
      float time,
      float offsetX,
      float offsetZ,
      JumpCircles.JumpWave wave,
      int ribbon
   ) {
      float laneMotion = 0.0F;
      float radiusNoise;
      if (this.style.is("Refraction")) {
         radiusNoise = 0.019F * MathHelper.sin((double)(theta * 3.0F + wave.seedPhase + time * 0.18F))
            + 0.008F * MathHelper.sin((double)(theta * 8.0F - wave.opacityPhase - time * 0.11F));
      } else if (this.style.is("Crown")) {
         radiusNoise = 0.04F * MathHelper.sin((double)(theta * 4.0F + wave.seedPhase)) + 0.018F * MathHelper.sin((double)(theta * 9.0F - time * 0.18F));
      } else if (this.style.is("Bloom")) {
         radiusNoise = 0.125F * MathHelper.sin((double)(theta * 3.0F + wave.seedPhase + time * 0.16F))
            + 0.032F * MathHelper.sin((double)(theta * 6.0F - strandPhase));
         laneMotion = MathHelper.sin((double)(theta * 3.0F + strandPhase - time * 0.42F)) * radius * 0.028F;
      } else if (this.style.is("Rift")) {
         radiusNoise = 0.058F * MathHelper.sin((double)(theta * 7.0F + strandPhase - time * 0.35F))
            + 0.026F * MathHelper.sin((double)(theta * 13.0F + wave.seedPhase));
      } else {
         radiusNoise = 0.072F * MathHelper.sin((double)(theta * 2.0F + strandPhase + time * 0.34F))
            + 0.034F * MathHelper.sin((double)(theta * 5.0F - strandPhase * 1.7F - time * 0.24F))
            + 0.016F * MathHelper.sin((double)(theta * 11.0F + wave.seedPhase * 2.3F));
      }

      float centerRadius = radius * (1.0F + radiusNoise) + radiusOffset + laneMotion;
      float cos = MathHelper.cos((double)theta);
      float sin = MathHelper.sin((double)theta);
      float rollingWave;
      float crestMultiplier;
      if (this.style.is("Refraction")) {
         rollingWave = waveHeight
            * (
               0.2F
                  + 0.08F * MathHelper.sin((double)(theta * 2.0F + strandPhase + time * 0.32F))
                  + 0.035F * MathHelper.sin((double)(theta * 6.0F - time * 0.2F))
            );
         crestMultiplier = 0.0F;
      } else if (this.style.is("Crown")) {
         rollingWave = waveHeight
            * (0.16F * MathHelper.sin((double)(theta * 4.0F + time * 0.32F)) + 0.07F * MathHelper.sin((double)(theta * 8.0F - time * 0.21F)));
         crestMultiplier = 1.82F;
      } else if (this.style.is("Bloom")) {
         rollingWave = waveHeight
            * (
               0.38F
                  + 0.34F * MathHelper.sin((double)(theta * 3.0F + wave.seedPhase + time * 0.38F))
                  + 0.1F * MathHelper.sin((double)(theta * 6.0F - time * 0.17F))
            );
         crestMultiplier = 0.52F;
      } else if (this.style.is("Rift")) {
         float jagged = (float)Math.pow((double)Math.max(0.0F, MathHelper.sin((double)(theta * 5.0F - time * 0.48F + strandPhase))), 4.0);
         rollingWave = waveHeight * (0.18F * MathHelper.sin((double)(theta * 3.0F + strandPhase)) + jagged * 0.62F);
         crestMultiplier = 1.15F;
      } else {
         rollingWave = waveHeight
            * (
               0.3F * MathHelper.sin((double)(theta * 2.0F + strandPhase + time * 0.6F))
                  + 0.14F * MathHelper.sin((double)(theta * 6.0F - strandPhase + time * 0.27F))
                  + wave.bend * 0.08F * MathHelper.cos((double)(theta + time * 0.19F))
            );
         crestMultiplier = 1.42F;
      }

      float sharpCrests = waveHeight * crestMultiplier * this.spikeProfile(wave, ribbon, theta, time);
      float y = Math.max(-0.025F, rollingWave + sharpCrests);
      float centerX = offsetX + cos * centerRadius;
      float centerY = 0.085F + y;
      float centerZ = offsetZ + sin * centerRadius;
      float radialX = cos * halfWidth;
      float radialZ = sin * halfWidth;
      return new JumpCircles.RibbonPoint(centerX + radialX, centerY, centerZ + radialZ, centerX - radialX, centerY, centerZ - radialZ);
   }

   private void emitTube(
      MatrixStack stack,
      VertexConsumer consumer,
      JumpCircles.RibbonPoint a,
      JumpCircles.RibbonPoint b,
      float verticalRadius,
      int colorA,
      int colorB,
      boolean halo
   ) {
      Entry pose = stack.peek();
      float centerAX = (a.outerX + a.innerX) * 0.5F;
      float centerAZ = (a.outerZ + a.innerZ) * 0.5F;
      float centerBX = (b.outerX + b.innerX) * 0.5F;
      float centerBZ = (b.outerZ + b.innerZ) * 0.5F;
      float widthAX = (a.outerX - a.innerX) * 0.5F;
      float widthAZ = (a.outerZ - a.innerZ) * 0.5F;
      float widthBX = (b.outerX - b.innerX) * 0.5F;
      float widthBZ = (b.outerZ - b.innerZ) * 0.5F;

      for (int side = 0; side < 6; side++) {
         float angle0 = (float) (Math.PI * 2) * (float)side / 6.0F;
         float angle1 = (float) (Math.PI * 2) * (float)(side + 1) / 6.0F;
         float radial0 = MathHelper.cos((double)angle0);
         float radial1 = MathHelper.cos((double)angle1);
         float vertical0 = MathHelper.sin((double)angle0);
         float vertical1 = MathHelper.sin((double)angle1);
         int a0 = halo ? colorA : shadeTubeColor(colorA, vertical0);
         int a1 = halo ? colorA : shadeTubeColor(colorA, vertical1);
         int b0 = halo ? colorB : shadeTubeColor(colorB, vertical0);
         int b1 = halo ? colorB : shadeTubeColor(colorB, vertical1);
         vertex(consumer, pose, centerAX + widthAX * radial0, a.outerY + verticalRadius * vertical0, centerAZ + widthAZ * radial0, a0);
         vertex(consumer, pose, centerBX + widthBX * radial0, b.outerY + verticalRadius * vertical0, centerBZ + widthBZ * radial0, b0);
         vertex(consumer, pose, centerBX + widthBX * radial1, b.outerY + verticalRadius * vertical1, centerBZ + widthBZ * radial1, b1);
         vertex(consumer, pose, centerAX + widthAX * radial1, a.outerY + verticalRadius * vertical1, centerAZ + widthAZ * radial1, a1);
      }
   }

   private void emitTopAtHeight(
      MatrixStack stack, VertexConsumer consumer, JumpCircles.RibbonPoint a, JumpCircles.RibbonPoint b, float yOffset, int colorA, int colorB
   ) {
      Entry pose = stack.peek();
      vertex(consumer, pose, a.outerX, a.outerY + yOffset, a.outerZ, colorA);
      vertex(consumer, pose, b.outerX, b.outerY + yOffset, b.outerZ, colorB);
      vertex(consumer, pose, b.innerX, b.innerY + yOffset, b.innerZ, colorB);
      vertex(consumer, pose, a.innerX, a.innerY + yOffset, a.innerZ, colorA);
   }

   private void emitCurtains(
      MatrixStack stack, VertexConsumer consumer, JumpCircles.RibbonPoint a, JumpCircles.RibbonPoint b, float tubeRadius, int colorA, int colorB
   ) {
      float floorY = 0.018F;
      float topAY = Math.max(floorY, a.outerY - tubeRadius * 0.55F);
      float topBY = Math.max(floorY, b.outerY - tubeRadius * 0.55F);
      if (!(topAY <= floorY + 0.015F) || !(topBY <= floorY + 0.015F)) {
         Entry pose = stack.peek();
         int topA = ColorUtil.multAlpha(colorA, 0.48F);
         int topB = ColorUtil.multAlpha(colorB, 0.48F);
         int floorA = ColorUtil.multAlpha(colorA, 0.23F);
         int floorB = ColorUtil.multAlpha(colorB, 0.23F);
         vertex(consumer, pose, a.outerX, topAY, a.outerZ, topA);
         vertex(consumer, pose, a.outerX, floorY, a.outerZ, floorA);
         vertex(consumer, pose, b.outerX, floorY, b.outerZ, floorB);
         vertex(consumer, pose, b.outerX, topBY, b.outerZ, topB);
         vertex(consumer, pose, a.innerX, topAY, a.innerZ, topA);
         vertex(consumer, pose, b.innerX, topBY, b.innerZ, topB);
         vertex(consumer, pose, b.innerX, floorY, b.innerZ, floorB);
         vertex(consumer, pose, a.innerX, floorY, a.innerZ, floorA);
         int floorFillA = ColorUtil.multAlpha(floorA, 0.62F);
         int floorFillB = ColorUtil.multAlpha(floorB, 0.62F);
         vertex(consumer, pose, a.outerX, floorY, a.outerZ, floorFillA);
         vertex(consumer, pose, b.outerX, floorY, b.outerZ, floorFillB);
         vertex(consumer, pose, b.innerX, floorY, b.innerZ, floorFillB);
         vertex(consumer, pose, a.innerX, floorY, a.innerZ, floorFillA);
      }
   }

   private static int shadeTubeColor(int color, float vertical) {
      float light = 0.58F + (vertical + 1.0F) * 0.21F;
      float whiteMix = Math.max(0.0F, vertical) * 0.18F;
      int red = Math.round((float)ColorUtil.getRed(color) * light);
      int green = Math.round((float)ColorUtil.getGreen(color) * light);
      int blue = Math.round((float)ColorUtil.getBlue(color) * light);
      red = Math.round(MathHelper.lerp(whiteMix, (float)red, 255.0F));
      green = Math.round(MathHelper.lerp(whiteMix, (float)green, 255.0F));
      blue = Math.round(MathHelper.lerp(whiteMix, (float)blue, 255.0F));
      float alphaScale = 0.72F + (vertical + 1.0F) * 0.14F;
      return ColorUtil.rgba(
         Math.clamp((long)red, 0, 255),
         Math.clamp((long)green, 0, 255),
         Math.clamp((long)blue, 0, 255),
         Math.clamp((long)Math.round((float)ColorUtil.getAlpha(color) * alphaScale), 0, 255)
      );
   }

   private static void vertex(VertexConsumer consumer, Entry pose, float x, float y, float z, int color) {
      consumer.vertex(pose, x, y, z).color(color);
   }

   private int ribbonColor(int ribbon, float theta, float time, float alpha) {
      float flow = (MathHelper.sin((double)(theta * 1.35F - time * 1.3F + (float)ribbon * 1.7F)) + 1.0F) * 0.5F;
      flow = 0.12F + flow * 0.82F;
      Color first = this.primaryColor.getValue();
      Color second = this.secondaryColor.getValue();
      int red = Math.round(MathHelper.lerp(flow, (float)first.getRed(), (float)second.getRed()));
      int green = Math.round(MathHelper.lerp(flow, (float)first.getGreen(), (float)second.getGreen()));
      int blue = Math.round(MathHelper.lerp(flow, (float)first.getBlue(), (float)second.getBlue()));
      if (this.style.is("Refraction")) {
         float pearl = 0.69F + 0.12F * flowingPulse(theta, (float)ribbon * 1.7F, time);
         red = Math.round(MathHelper.lerp(pearl, (float)red, 255.0F));
         green = Math.round(MathHelper.lerp(pearl, (float)green, 255.0F));
         blue = Math.round(MathHelper.lerp(pearl, (float)blue, 255.0F));
      }

      int baseAlpha = Math.min(first.getAlpha(), second.getAlpha());
      return ColorUtil.rgba(
         Math.clamp((long)red, 0, 255),
         Math.clamp((long)green, 0, 255),
         Math.clamp((long)blue, 0, 255),
         Math.clamp((long)Math.round((float)baseAlpha * MathHelper.clamp(alpha, 0.0F, 1.0F)), 0, 255)
      );
   }

   private float animatedRadius(float progress) {
      float exponent = this.style.is("Refraction") ? 3.7F : 3.0F;
      float eased = 1.0F - (float)Math.pow((double)(1.0F - progress), (double)exponent);
      float settle = 1.0F + 0.035F * MathHelper.sin((double)(progress * (float) Math.PI));
      return (0.035F + this.size.getFloat() * eased) * settle;
   }

   private static float envelope(float progress) {
      float appear = smoothStep(0.0F, 0.11F, progress);
      float disappear = 1.0F - smoothStep(0.58F, 1.0F, progress);
      return MathHelper.clamp(appear * disappear, 0.0F, 1.0F);
   }

   private static float flowingPulse(float theta, float phase, float time) {
      return (MathHelper.sin((double)(theta * 3.0F - time * 2.0F + phase)) + 1.0F) * 0.5F;
   }

   private float organicOpacity(JumpCircles.JumpWave wave, int ribbon, float theta, float time) {
      float slow = MathHelper.sin((double)(theta * 3.0F + wave.opacityPhase + (float)ribbon * 1.41F - time * 0.52F));
      float detail = MathHelper.sin((double)(theta * 8.0F - wave.opacityPhase * 0.67F + (float)ribbon * 2.07F + time * 0.21F));
      float drifting = MathHelper.sin((double)(theta * 1.35F + wave.seedPhase - time * 0.88F));
      float noise = slow * 0.5F + detail * 0.27F + drifting * 0.23F;
      if (this.style.is("Refraction")) {
         return MathHelper.clamp(0.88F + noise * 0.12F, 0.7F, 1.0F);
      } else if (this.style.is("Crown")) {
         return MathHelper.clamp(0.82F + noise * 0.18F, 0.58F, 1.0F);
      } else if (this.style.is("Bloom")) {
         return MathHelper.clamp(0.74F + noise * 0.22F, 0.46F, 1.0F);
      } else if (this.style.is("Rift")) {
         float fracture = (float)Math.pow((double)Math.abs(MathHelper.sin((double)(theta * 4.5F + wave.opacityPhase - time * 0.34F))), 2.6);
         return MathHelper.clamp(0.24F + fracture * 0.76F + noise * 0.16F, 0.12F, 1.0F);
      } else {
         return MathHelper.clamp(0.7F + noise * 0.3F, 0.3F, 1.0F);
      }
   }

   private float spikeProfile(JumpCircles.JumpWave wave, int ribbon, float theta, float time) {
      if (this.style.is("Refraction")) {
         return 0.0F;
      } else {
         float total = 0.0F;

         for (int i = 0; i < wave.spikeAngles.length; i++) {
            float direction = (i & 1) == 0 ? 1.0F : -1.0F;
            float ribbonShift = this.style.is("Crown") ? (float)ribbon * 0.018F : (float)ribbon * 0.11F;
            float movingAngle = wave.spikeAngles[i] + direction * time * (0.018F + (float)i * 0.003F) + ribbonShift;
            float distance = angularDistance(theta, movingAngle);
            float normalized = distance / wave.spikeWidths[i];
            float crest = (float)Math.exp((double)(-normalized * normalized * 2.35F));
            total += crest * wave.spikeHeights[i];
         }

         return Math.min(total, 1.32F);
      }
   }

   private float styleCode() {
      if (this.style.is("Crown")) {
         return 1.0F;
      } else if (this.style.is("Bloom")) {
         return 2.0F;
      } else if (this.style.is("Rift")) {
         return 3.0F;
      } else {
         return this.style.is("Refraction") ? 4.0F : 0.0F;
      }
   }

   private static float angularDistance(float first, float second) {
      float delta = Math.abs(MathHelper.wrapDegrees((first - second) * (180.0F / (float)Math.PI))) * (float) (Math.PI / 180.0);
      return Math.min(delta, (float) Math.PI);
   }

   private static float smoothStep(float edge0, float edge1, float value) {
      float t = MathHelper.clamp((value - edge0) / Math.max(1.0E-4F, edge1 - edge0), 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private boolean shouldTrack(PlayerEntity player, MinecraftClient client) {
      return player != null && player.isAlive() ? player == client.player ? this.self.getValue() : this.players.getValue() : false;
   }

   private long lifetimeMillis() {
      return Math.max(100L, Math.round(this.lifetime.getValue() * 1000.0));
   }

   private static final class JumpWave {
      private static final int SPIKE_COUNT = 4;
      private final Vec3d origin;
      private final long spawnTime;
      private final float seedPhase;
      private final float bend;
      private final float opacityPhase;
      private final float[] spikeAngles;
      private final float[] spikeHeights;
      private final float[] spikeWidths;
      private long retirementStarted = -1L;

      private JumpWave(
         Vec3d origin, long spawnTime, float seedPhase, float bend, float opacityPhase, float[] spikeAngles, float[] spikeHeights, float[] spikeWidths
      ) {
         this.origin = origin;
         this.spawnTime = spawnTime;
         this.seedPhase = seedPhase;
         this.bend = bend;
         this.opacityPhase = opacityPhase;
         this.spikeAngles = spikeAngles;
         this.spikeHeights = spikeHeights;
         this.spikeWidths = spikeWidths;
      }

      private static JumpCircles.JumpWave create(Vec3d origin, long spawnTime, Random random) {
         float[] spikeAngles = new float[4];
         float[] spikeHeights = new float[4];
         float[] spikeWidths = new float[4];
         float baseAngle = random.nextFloat() * (float) (Math.PI * 2);

         for (int i = 0; i < 4; i++) {
            float distributed = baseAngle + (float) (Math.PI * 2) * (float)i / 4.0F;
            spikeAngles[i] = distributed + (random.nextFloat() - 0.5F) * 0.78F;
            spikeHeights[i] = 0.58F + random.nextFloat() * 0.54F;
            spikeWidths[i] = 0.115F + random.nextFloat() * 0.105F;
         }

         return new JumpCircles.JumpWave(
            origin,
            spawnTime,
            random.nextFloat() * (float) (Math.PI * 2),
            random.nextFloat() * 2.0F - 1.0F,
            random.nextFloat() * (float) (Math.PI * 2),
            spikeAngles,
            spikeHeights,
            spikeWidths
         );
      }

      private boolean expired(long now, long lifetime) {
         return now - this.spawnTime >= lifetime || this.retirementStarted >= 0L && now - this.retirementStarted >= 280L;
      }

      private boolean isRetiring() {
         return this.retirementStarted >= 0L;
      }

      private void beginRetirement(long now) {
         if (this.retirementStarted < 0L) {
            this.retirementStarted = now;
         }
      }

      private float retirementAlpha(long now) {
         if (this.retirementStarted < 0L) {
            return 1.0F;
         } else {
            float progress = MathHelper.clamp((float)(now - this.retirementStarted) / 280.0F, 0.0F, 1.0F);
            return 1.0F - JumpCircles.smoothStep(0.0F, 1.0F, progress);
         }
      }
   }

   private static final class PlayerJumpState {
      private boolean grounded;
      private double lastGroundY;
      private double lastY;

      private PlayerJumpState(boolean grounded, double lastGroundY, double lastY) {
         this.grounded = grounded;
         this.lastGroundY = lastGroundY;
         this.lastY = lastY;
      }
   }

   private static enum RenderPass {
      BODY,
      GLOW;

      private RenderPass() {
      }
   }

   private static record RibbonPoint(float outerX, float outerY, float outerZ, float innerX, float innerY, float innerZ) {
   }
}
