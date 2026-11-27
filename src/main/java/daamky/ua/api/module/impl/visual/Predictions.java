package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class Predictions extends Module {
   private static final int MAX_PARTICLES = 650;
   private static final long PARTICLE_SPAWN_INTERVAL_MS = 42L;
   private static final String TEXTURE_GLOW = "Glow";
   private static final String TEXTURE_STAR = "Star";
   private static final String TEXTURE_HEART = "Heart";
   private static final String TEXTURE_LIGHTNING = "Lightning";
   private static final String TEXTURE_TRIANGLE = "Triangle";
   private static final String TEXTURE_CROSS = "Cross";
   private static final String TEXTURE_CROWN = "Crown";
   private static final String TEXTURE_DOLLAR = "Dollar";
   private static final String TEXTURE_RHOMBUS = "Rhombus";
   private static final String TEXTURE_SNOWFLAKE = "Snowflake";
   private static final String TEXTURE_SKULL = "Skull";
   private static final String TEXTURE_CUBE = "Cube";
   private static final String TEXTURE_SQUARE = "Square";
   private static final String TEXTURE_RANDOM = "Random";
   private static final Identifier PARTICLE_GLOW = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/glow.png");
   private static final Identifier PARTICLE_STAR = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/star.png");
   private static final Identifier PARTICLE_HEART = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/heart.png");
   private static final Identifier PARTICLE_LIGHTNING = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/lightning.png");
   private static final Identifier PARTICLE_TRIANGLE = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/triangle.png");
   private static final Identifier PARTICLE_CROSS = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/cross.png");
   private static final Identifier PARTICLE_CROWN = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/crown.png");
   private static final Identifier PARTICLE_DOLLAR = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/dollar.png");
   private static final Identifier PARTICLE_SQUARE = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/square.png");
   private static final Identifier PARTICLE_SNOWFLAKE = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/snowflake.png");
   private static final Identifier PARTICLE_SKULL = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/skull.png");
   private static final Identifier PARTICLE_CUBE = Identifier.fromNamespaceAndPath("daamky", "textures/features/predictions/particles/cube.png");
   private static final Predictions.ProjectileProfile PROFILE_PEARL = new Predictions.ProjectileProfile(0.99, 0.03);
   private static final Predictions.ProjectileProfile PROFILE_ITEM = new Predictions.ProjectileProfile(0.98, 0.04);
   private static final Predictions.ProjectileProfile PROFILE_ARROW = new Predictions.ProjectileProfile(0.99, 0.05);
   private static final String[] PARTICLE_TEXTURE_MODES = new String[]{
      "Glow", "Star", "Heart", "Lightning", "Triangle", "Cross", "Crown", "Dollar", "Rhombus", "Snowflake", "Skull", "Cube", "Square", "Random"
   };
   private final MultiModeSetting targets = this.register(
      new MultiModeSetting(
         "Targets",
         "Objects rendered by predictions.",
         new String[]{"Held", "Pearls", "Arrows", "Tridents", "Items"},
         "Held",
         "Pearls",
         "Arrows",
         "Tridents",
         "Items"
      )
   );
   private final MultiModeSetting render = this.register(
      new MultiModeSetting("Render", "Prediction visuals.", new String[]{"Trajectory", "Landing", "Particles"}, "Trajectory", "Landing", "Particles")
   );
   private final NumberSetting range = this.register(new NumberSetting("Range", "Maximum prediction render distance.", 128.0, 16.0, 256.0, 1.0));
   private final NumberSetting lineWidth = this.register(new NumberSetting("Line Width", "Trajectory line width.", 2.0, 1.0, 2.5, 0.1));
   private final ModeSetting particleTexture = this.register(
      new ModeSetting("Particle Texture", "Texture used for projectile particles.", "Glow", PARTICLE_TEXTURE_MODES)
   );
   private final NumberSetting particleCount = this.register(
      new NumberSetting("Particle Count", "Particles spawned per moving projectile pulse.", 4.0, 1.0, 10.0, 1.0)
   );
   private final NumberSetting particleSize = this.register(new NumberSetting("Particle Size", "Projectile particle quad size.", 0.23, 0.05, 0.75, 0.01));
   private final NumberSetting particleSpeed = this.register(new NumberSetting("Particle Speed", "Projectile particle scatter speed.", 0.3, 0.05, 1.5, 0.01));
   private final NumberSetting particleLifetime = this.register(
      new NumberSetting("Particle Lifetime", "Projectile particle lifetime in milliseconds.", 900.0, 250.0, 2500.0, 50.0)
   );
   private final BooleanSetting randomParticleColor = this.register(new BooleanSetting("Random Particle Color", "Uses random particle colors.", false));
   private final BooleanSetting particleWhiteCenter = this.register(
      new BooleanSetting("White Center", "Draws a bright center inside projectile particles.", true)
   );
   private final BooleanSetting particleSpin = this.register(new BooleanSetting("Particle Spin", "Rotates projectile particles.", true));
   private final ColorSetting lineColor = this.register(new ColorSetting("Line Color", "Prediction line color.", new Color(247, 133, 255)));
   private final ColorSetting particleColor = this.register(new ColorSetting("Particle Color", "Thrown object trail color.", new Color(185, 125, 255, 215)));
   private final Random random = new Random();
   private final List<Predictions.PredictionParticle> particles = new ArrayList<>();
   private final List<Vec3> simulationPoints = new ArrayList<>(181);
   private final Map<Integer, Long> lastParticleSpawnMs = new HashMap<>();
   private final Set<RenderType> usedParticleRenderTypes = new HashSet<>();
   private final Matrix4f inverseProjection = new Matrix4f();
   private final Matrix4f inverseView = new Matrix4f();
   private final Vector4f unprojectScratch = new Vector4f();
   private long lastParticleUpdateMs;

   public Predictions() {
      super("Predictions", "Draws projectile and thrown item trajectories.", ModuleCategory.VISUAL);
      this.particleTexture.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleCount.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleSize.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleSpeed.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleLifetime.visibleWhen(() -> this.render.isSelected("Particles"));
      this.randomParticleColor.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleWhiteCenter.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleSpin.visibleWhen(() -> this.render.isSelected("Particles"));
      this.particleColor.visibleWhen(() -> this.render.isSelected("Particles") && !this.randomParticleColor.getValue());
   }

   @Override
   protected void onDisable() {
      this.particles.clear();
      this.lastParticleSpawnMs.clear();
      this.usedParticleRenderTypes.clear();
      this.lastParticleUpdateMs = 0L;
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.level != null) {
         boolean trajectoryEnabled = this.render.isSelected("Trajectory");
         boolean particlesEnabled = this.render.isSelected("Particles");
         boolean landingEnabled = this.render.isSelected("Landing");
         if (!trajectoryEnabled && !particlesEnabled) {
            this.particles.clear();
            this.lastParticleSpawnMs.clear();
         } else {
            long now = System.currentTimeMillis();
            float frameDelta = this.frameDelta(now);
            double simulationLimitSqr = this.simulationLimitSqr();
            float lineWidthValue = this.lineWidth.getFloat();
            if (trajectoryEnabled) {
               this.drawHeldPrediction(event.getPartialTicks(), simulationLimitSqr, lineWidthValue, landingEnabled);
            }

            for (Entity entity : mc.level.entitiesForRendering()) {
               Predictions.ProjectileTarget target = this.targetFor(entity);
               if (target != null && this.targets.isSelected(target.settingName()) && this.isInRange(entity)) {
                  Vec3 velocity = entity.getDeltaMovement();
                  if (this.shouldRenderLive(entity, velocity)) {
                     Predictions.ProjectileProfile profile = this.profileFor(target);
                     Vec3 start = entity.getPosition(event.getPartialTicks());
                     if (trajectoryEnabled) {
                        boolean hit = this.simulate(start, velocity, profile, 180, entity, simulationLimitSqr, this.simulationPoints);
                        this.drawPath(this.simulationPoints, hit, this.lineColor.getValue().getRGB(), lineWidthValue, landingEnabled);
                     }

                     if (particlesEnabled) {
                        this.spawnProjectileParticles(entity, start, velocity, now);
                     }
                  }
               }
            }

            if (particlesEnabled) {
               this.updateParticles(frameDelta, now);
               this.drawParticles(event, now);
               this.lastParticleSpawnMs.entrySet().removeIf(entry -> now - entry.getValue() > 2000L);
            } else {
               this.particles.clear();
               this.lastParticleSpawnMs.clear();
            }
         }
      } else {
         this.particles.clear();
         this.lastParticleSpawnMs.clear();
      }
   }

   private void drawHeldPrediction(float partialTicks, double simulationLimitSqr, float lineWidthValue, boolean landingEnabled) {
      if (this.targets.isSelected("Held") && mc.player != null) {
         Predictions.HeldProjectile held = this.heldProjectile(mc.player, mc.player.getMainHandItem());
         if (held == null) {
            held = this.heldProjectile(mc.player, mc.player.getOffhandItem());
         }

         if (held != null && this.targets.isSelected(held.target().settingName())) {
            Predictions.AimRay ray = this.centerScreenRay(mc.gameRenderer.getMainCamera());
            if (ray != null && !(ray.direction().lengthSqr() < 1.0E-6)) {
               Vec3 start = ray.origin().add(ray.direction().scale(0.05));
               Vec3 velocity = ray.direction().scale(held.speed());
               boolean hit = this.simulate(start, velocity, this.profileFor(held.target()), 180, mc.player, simulationLimitSqr, this.simulationPoints);
               this.drawPath(this.simulationPoints, hit, this.lineColor.getValue().getRGB(), lineWidthValue, landingEnabled);
            }
         }
      }
   }

   private Predictions.AimRay centerScreenRay(Camera camera) {
      Vec3 cameraPos = Render3D.lastCameraPos;
      if (cameraPos == null || !this.isFinite(cameraPos)) {
         cameraPos = camera.position();
      }

      Predictions.AimRay matrixRay = this.centerScreenRayFromMatrices(cameraPos);
      if (matrixRay != null) {
         return matrixRay;
      } else {
         Vec3 direction = this.directionFromRotation(camera.xRot(), camera.yRot());
         return this.isFinite(direction) && direction.lengthSqr() > 1.0E-6 ? new Predictions.AimRay(cameraPos, direction) : null;
      }
   }

   private Predictions.AimRay centerScreenRayFromMatrices(Vec3 cameraPos) {
      try {
         this.inverseProjection.set(Render3D.lastProjMat).invert();
         this.inverseView.set(Render3D.lastModMat).invert();
         Vec3 near = this.unprojectCenter(this.inverseProjection, this.inverseView, -1.0F);
         Vec3 far = this.unprojectCenter(this.inverseProjection, this.inverseView, 1.0F);
         if (this.isFinite(near) && this.isFinite(far)) {
            Vec3 direction = far.subtract(near);
            return direction.lengthSqr() < 1.0E-8 ? null : new Predictions.AimRay(cameraPos.add(near), direction.normalize());
         } else {
            return null;
         }
      } catch (RuntimeException var5) {
         return null;
      }
   }

   private Vec3 unprojectCenter(Matrix4f inverseProjection, Matrix4f inverseView, float z) {
      Vector4f point = this.unprojectScratch.set(0.0F, 0.0F, z, 1.0F);
      inverseProjection.transform(point);
      if (Math.abs(point.w) > 1.0E-6F) {
         point.div(point.w);
      }

      inverseView.transform(point);
      if (Math.abs(point.w) > 1.0E-6F) {
         point.div(point.w);
      }

      return new Vec3((double)point.x, (double)point.y, (double)point.z);
   }

   private Vec3 directionFromRotation(float pitch, float yaw) {
      float pitchRad = pitch * (float) (Math.PI / 180.0);
      float yawRad = -yaw * (float) (Math.PI / 180.0);
      float cosPitch = Mth.cos((double)pitchRad);
      return new Vec3(
            (double)(Mth.sin((double)yawRad) * cosPitch),
            (double)(-Mth.sin((double)pitchRad)),
            (double)(Mth.cos((double)yawRad) * cosPitch)
         )
         .normalize();
   }

   private Predictions.HeldProjectile heldProjectile(Player player, ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         if (stack.getItem() == Items.ENDER_PEARL) {
            return new Predictions.HeldProjectile(Predictions.ProjectileTarget.PEARLS, 1.5);
         } else if (stack.getItem() instanceof BowItem) {
            if (!this.isUsing(player, stack)) {
               return null;
            } else {
               float power = BowItem.getPowerForTime(player.getTicksUsingItem());
               return power > 0.1F ? new Predictions.HeldProjectile(Predictions.ProjectileTarget.ARROWS, (double)power * 3.0) : null;
            }
         } else if (stack.getItem() instanceof CrossbowItem) {
            if (CrossbowItem.isCharged(stack)) {
               return new Predictions.HeldProjectile(Predictions.ProjectileTarget.ARROWS, 3.15);
            } else if (!this.isUsing(player, stack)) {
               return null;
            } else {
               float charge = (float)player.getTicksUsingItem() / (float)CrossbowItem.getChargeDuration(stack, player);
               return charge > 0.1F
                  ? new Predictions.HeldProjectile(Predictions.ProjectileTarget.ARROWS, 3.15 * (double)Mth.clamp(charge, 0.0F, 1.0F))
                  : null;
            }
         } else if (!(stack.getItem() instanceof TridentItem)) {
            return null;
         } else if (!this.isUsing(player, stack)) {
            return null;
         } else {
            float charge = Mth.clamp((float)player.getTicksUsingItem() / 10.0F, 0.0F, 1.0F);
            return charge > 0.1F ? new Predictions.HeldProjectile(Predictions.ProjectileTarget.TRIDENTS, 2.5 * (double)charge) : null;
         }
      } else {
         return null;
      }
   }

   private boolean isUsing(Player player, ItemStack stack) {
      return player.isUsingItem() && !player.getUseItem().isEmpty() && player.getUseItem().getItem() == stack.getItem();
   }

   private boolean simulate(
      Vec3 start, Vec3 velocity, Predictions.ProjectileProfile profile, int maxTicks, Entity collider, double simulationLimitSqr, List<Vec3> points
   ) {
      points.clear();
      points.add(start);
      Vec3 position = start;
      Vec3 motion = velocity;
      boolean hit = false;

      for (int i = 0; i < maxTicks; i++) {
         Vec3 next = position.add(motion);
         BlockHitResult blockHit = mc.level.clip(new ClipContext(position, next, Block.COLLIDER, Fluid.NONE, collider));
         if (blockHit != null && blockHit.getType() != Type.MISS) {
            points.add(blockHit.getLocation());
            hit = true;
            break;
         }

         points.add(next);
         position = next;
         motion = motion.scale(profile.drag()).add(0.0, -profile.gravity(), 0.0);
         if (next.y < -128.0 || next.distanceToSqr(start) > simulationLimitSqr) {
            break;
         }
      }

      return hit;
   }

   private void drawPath(List<Vec3> points, boolean hit, int color, float lineWidthValue, boolean landingEnabled) {
      if (points.size() >= 2) {
         int count = points.size() - 1;

         for (int i = 1; i < points.size(); i++) {
            float startAlpha = 1.0F - (float)(i - 1) / (float)count * 0.55F;
            float endAlpha = 1.0F - (float)i / (float)count * 0.55F;
            Render3D.drawLineGradient(
               points.get(i - 1), points.get(i), ColorUtil.multAlpha(color, startAlpha), ColorUtil.multAlpha(color, endAlpha), lineWidthValue, false
            );
         }

         if (landingEnabled && hit) {
            Vec3 end = points.get(points.size() - 1);
            double size = 0.11;
            AABB marker = new AABB(end.x - size, end.y - 0.015, end.z - size, end.x + size, end.y + 0.015, end.z + size);
            Render3D.drawBox(marker, ColorUtil.multAlpha(color, 0.65F), 1.0F, true, true, false);
            Render3D.drawBoxOverlay(marker, ColorUtil.multAlpha(color, 0.85F), 1.25F);
         }
      }
   }

   private double simulationLimitSqr() {
      double value = (double)this.range.getFloat();
      return value * value * 4.0;
   }

   private float frameDelta(long now) {
      if (this.lastParticleUpdateMs == 0L) {
         this.lastParticleUpdateMs = now;
         return 1.0F;
      } else {
         long elapsed = Math.max(1L, Math.min(50L, now - this.lastParticleUpdateMs));
         this.lastParticleUpdateMs = now;
         return Mth.clamp((float)elapsed / 16.6667F, 0.25F, 3.0F);
      }
   }

   private void spawnProjectileParticles(Entity entity, Vec3 position, Vec3 velocity, long now) {
      long last = this.lastParticleSpawnMs.getOrDefault(entity.getId(), 0L);
      if (now - last >= 42L) {
         this.lastParticleSpawnMs.put(entity.getId(), now);
         int count = Math.max(1, (int)Math.round(this.particleCount.getValue()));
         double spread = Math.max(0.12, (double)entity.getBbWidth() * 0.8);
         double height = Math.max(0.12, (double)entity.getBbHeight());
         double motionRange = (double)this.particleSpeed.getFloat() / 18.0;

         for (int i = 0; i < count; i++) {
            Vec3 spawn = position.add(this.randomRange(-spread, spread), this.random.nextDouble() * height, this.randomRange(-spread, spread));
            Vec3 motion = new Vec3(
                  this.randomRange(-motionRange, motionRange), this.randomRange(-motionRange, motionRange), this.randomRange(-motionRange, motionRange)
               )
               .add(velocity.scale(-0.018));
            this.particles
               .add(
                  new Predictions.PredictionParticle(
                     this.particleTexture(),
                     spawn,
                     motion,
                     now,
                     (long)Math.round(this.particleLifetime.getFloat()),
                     this.particleBaseColor(),
                     this.random.nextFloat() * 360.0F,
                     this.randomRange(-5.0, 5.0),
                     0.78F + this.random.nextFloat() * 0.5F
                  )
               );
         }

         int overflow = this.particles.size() - 650;
         if (overflow > 0) {
            this.particles.subList(0, overflow).clear();
         }
      }
   }

   private void updateParticles(float frameDelta, long now) {
      if (!this.particles.isEmpty()) {
         this.particles.removeIf(particlex -> particlex.dead(now));

         for (Predictions.PredictionParticle particle : this.particles) {
            particle.update(frameDelta);
         }
      }
   }

   private void drawParticles(WorldRenderEvent event, long now) {
      if (!this.particles.isEmpty() && mc.gameRenderer != null) {
         PoseStack stack = event.getStack();
         BufferSource provider = mc.renderBuffers().bufferSource();
         Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
         Quaternionf cameraRotation = mc.gameRenderer.getMainCamera().rotation();
         this.usedParticleRenderTypes.clear();

         for (Predictions.PredictionParticle particle : this.particles) {
            this.renderParticle(stack, provider, cameraPos, cameraRotation, particle, now);
         }

         for (RenderType type : this.usedParticleRenderTypes) {
            provider.endBatch(type);
         }

         this.usedParticleRenderTypes.clear();
      }
   }

   private void renderParticle(
      PoseStack stack, BufferSource provider, Vec3 cameraPos, Quaternionf cameraRotation, Predictions.PredictionParticle particle, long now
   ) {
      float alpha = particle.alpha(now);
      if (!(alpha <= 0.01F)) {
         float size = this.particleSize.getFloat() * particle.scale() * (0.55F + alpha * 0.45F);
         int color = ColorUtil.multAlpha(particle.color(), alpha);
         float rotation = this.particleSpin.getValue() ? particle.rotation() : 3.15F;
         this.drawBillboard(stack, provider, particle.texture(), particle.position(), cameraPos, cameraRotation, size, rotation, color);
         if (this.particleWhiteCenter.getValue()) {
            this.drawBillboard(
               stack,
               provider,
               particle.texture(),
               particle.position(),
               cameraPos,
               cameraRotation,
               size * 0.42F,
               rotation,
               ColorUtil.rgba(255, 255, 255, Math.round(230.0F * alpha))
            );
         }
      }
   }

   private void drawBillboard(
      PoseStack stack, BufferSource provider, Identifier texture, Vec3 pos, Vec3 cameraPos, Quaternionf cameraRotation, float size, float rotation, int color
   ) {
      if (texture != null && !(size <= 0.001F) && ColorUtil.getAlpha(color) > 0) {
         RenderType renderType = ClientPipelines.WORLD_PARTICLES_GLOW.apply(texture);
         this.usedParticleRenderTypes.add(renderType);
         VertexConsumer consumer = provider.getBuffer(renderType);
         stack.pushPose();
         stack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
         stack.mulPose(cameraRotation);
         stack.mulPose(Axis.ZP.rotationDegrees(rotation));
         Pose pose = stack.last();
         float half = size * 0.5F;
         WorldVertex.textured(consumer, pose, -half, -half, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, -half, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, half, half, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(consumer, pose, -half, half, 0.0F, 0.0F, 1.0F, color);
         stack.popPose();
      }
   }

   private Identifier particleTexture() {
      String mode = this.particleTexture.getValue();
      if ("Random".equals(mode)) {
         mode = PARTICLE_TEXTURE_MODES[this.random.nextInt(PARTICLE_TEXTURE_MODES.length - 1)];
      }
      return switch (mode) {
         case "Star" -> PARTICLE_STAR;
         case "Heart" -> PARTICLE_HEART;
         case "Lightning" -> PARTICLE_LIGHTNING;
         case "Triangle" -> PARTICLE_TRIANGLE;
         case "Cross" -> PARTICLE_CROSS;
         case "Crown" -> PARTICLE_CROWN;
         case "Dollar" -> PARTICLE_DOLLAR;
         case "Rhombus", "Square" -> PARTICLE_SQUARE;
         case "Snowflake" -> PARTICLE_SNOWFLAKE;
         case "Skull" -> PARTICLE_SKULL;
         case "Cube" -> PARTICLE_CUBE;
         default -> PARTICLE_GLOW;
      };
   }

   private int particleBaseColor() {
      return this.randomParticleColor.getValue() ? Color.HSBtoRGB(this.random.nextFloat(), 0.78F, 1.0F) | 0xFF000000 : this.particleColor.getValue().getRGB();
   }

   private double randomRange(double min, double max) {
      return min + (max - min) * this.random.nextDouble();
   }

   private boolean isFinite(Vec3 pos) {
      return pos != null && Double.isFinite(pos.x) && Double.isFinite(pos.y) && Double.isFinite(pos.z);
   }

   private boolean shouldRenderLive(Entity entity, Vec3 velocity) {
      return !(entity instanceof ItemEntity item)
         ? velocity.lengthSqr() > 1.0E-5
         : !this.isGroundedItem(item, velocity) && velocity.lengthSqr() > 1.0E-5;
   }

   private boolean isGroundedItem(ItemEntity item, Vec3 velocity) {
      if (item.onGround()) {
         return true;
      } else if (mc.level == null) {
         return false;
      } else {
         AABB box = item.getBoundingBox();
         Vec3 from = new Vec3(item.getX(), box.minY + 0.08, item.getZ());
         Vec3 to = new Vec3(item.getX(), box.minY - 0.16, item.getZ());
         BlockHitResult hit = mc.level.clip(new ClipContext(from, to, Block.COLLIDER, Fluid.NONE, item));
         boolean blockUnderItem = hit != null && hit.getType() != Type.MISS;
         return blockUnderItem && velocity.y > -0.08;
      }
   }

   private boolean isInRange(Entity entity) {
      float maxRange = this.range.getFloat();
      return maxRange <= 0.0F || mc.player == null || mc.player.distanceToSqr(entity) <= (double)(maxRange * maxRange);
   }

   private Predictions.ProjectileTarget targetFor(Entity entity) {
      if (entity instanceof ThrownEnderpearl) {
         return Predictions.ProjectileTarget.PEARLS;
      } else if (entity instanceof ThrownTrident) {
         return Predictions.ProjectileTarget.TRIDENTS;
      } else if (entity instanceof AbstractArrow) {
         return Predictions.ProjectileTarget.ARROWS;
      } else {
         if (entity instanceof ItemEntity item && !item.getItem().isEmpty()) {
            return Predictions.ProjectileTarget.ITEMS;
         }

         return null;
      }
   }

   private Predictions.ProjectileProfile profileFor(Predictions.ProjectileTarget target) {
      return switch (target) {
         case PEARLS -> PROFILE_PEARL;
         case ARROWS, TRIDENTS -> PROFILE_ARROW;
         case ITEMS -> PROFILE_ITEM;
      };
   }

   private static record AimRay(Vec3 origin, Vec3 direction) {
   }

   private static record HeldProjectile(Predictions.ProjectileTarget target, double speed) {
   }

   private static final class PredictionParticle {
      private final Identifier texture;
      private Vec3 position;
      private Vec3 motion;
      private final long spawnMs;
      private final long lifetimeMs;
      private final int color;
      private float rotation;
      private final float rotationSpeed;
      private final float scale;

      private PredictionParticle(
         Identifier texture, Vec3 position, Vec3 motion, long spawnMs, long lifetimeMs, int color, float rotation, double rotationSpeed, float scale
      ) {
         this.texture = texture;
         this.position = position;
         this.motion = motion;
         this.spawnMs = spawnMs;
         this.lifetimeMs = Math.max(1L, lifetimeMs);
         this.color = color;
         this.rotation = rotation;
         this.rotationSpeed = (float)rotationSpeed;
         this.scale = scale;
      }

      private void update(float frameDelta) {
         this.position = this.position.add(this.motion.scale((double)frameDelta));
         this.motion = this.motion.scale(Math.pow(0.985, (double)frameDelta)).add(0.0, -0.0018 * (double)frameDelta, 0.0);
         this.rotation = this.rotation + this.rotationSpeed * frameDelta;
      }

      private boolean dead(long now) {
         return now - this.spawnMs >= this.lifetimeMs;
      }

      private float alpha(long now) {
         float progress = Mth.clamp((float)(now - this.spawnMs) / (float)this.lifetimeMs, 0.0F, 1.0F);
         if (progress < 0.18F) {
            return progress / 0.18F;
         } else if (progress > 0.62F) {
            float out = (progress - 0.62F) / 0.38F;
            return 1.0F - out * out * (3.0F - 2.0F * out);
         } else {
            return 1.0F;
         }
      }

      private Identifier texture() {
         return this.texture;
      }

      private Vec3 position() {
         return this.position;
      }

      private int color() {
         return this.color;
      }

      private float rotation() {
         return this.rotation;
      }

      private float scale() {
         return this.scale;
      }
   }

   private static record ProjectileProfile(double drag, double gravity) {
   }

   private static enum ProjectileTarget {
      PEARLS("Pearls"),
      ARROWS("Arrows"),
      TRIDENTS("Tridents"),
      ITEMS("Items");

      private final String settingName;

      private ProjectileTarget(String settingName) {
         this.settingName = settingName;
      }

      private String settingName() {
         return this.settingName;
      }
   }
}
