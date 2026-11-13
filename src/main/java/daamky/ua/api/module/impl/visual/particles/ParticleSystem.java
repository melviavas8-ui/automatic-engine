package daamky.ua.api.module.impl.visual.particles;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import daamky.ua.api.events.impl.WorldRenderEvent;

public final class ParticleSystem {
   private final ParticleSettings settings;
   private final Random random = new Random();
   private final List<TexturedParticle3D> particles = new ArrayList<>();
   private final List<ParticleTexture> enabledTextures = new ArrayList<>(ParticleConstants.TEXTURES.length);
   private final ParticleMotion motion;
   private final ParticleRenderer renderer = new ParticleRenderer();

   public ParticleSystem(ParticleSettings settings) {
      this.settings = settings;
      this.motion = new ParticleMotion(settings, this.random);
   }

   public void clear() {
      this.particles.clear();
      this.renderer.clear();
   }

   public void tick(MinecraftClient client) {
      if (client != null && client.world != null && client.player != null) {
         this.updateParticlesList(false, client);
         if (this.settings.spawnWorld()) {
            this.addParticlesToWorld(client, client.player);
         }
      } else {
         this.clear();
      }
   }

   public void spawnAttack(MinecraftClient client, Entity target) {
      if (target != null && client != null && client.world != null && client.player != null && this.settings.spawnAttacks()) {
         this.addParticlesToList(
            client, target, Math.round(this.settings.spawnCountOnAttack.getFloat()), (long)Math.round(this.settings.aliveTimeBaseOnAttack.getFloat())
         );
      }
   }

   public void render(MinecraftClient client, WorldRenderEvent event) {
      if (client != null && client.world != null && client.player != null && !this.particles.isEmpty() && client.gameRenderer != null) {
         MatrixStack stack = event.getStack();
         Immediate provider = client.getBufferBuilders().getEntityVertexConsumers();
         Vec3d cameraPos = client.gameRenderer.getCamera().getCameraPos();
         Quaternionf cameraRotation = client.gameRenderer.getCamera().getRotation();
         float partialTicks = 1.0F - MathHelper.clamp(event.getPartialTicks(), 0.0F, 1.0F);
         boolean scaleOfAlpha = this.settings.scaleAsAlphaEnabled.getValue();
         boolean bloom = this.settings.bloomTextureEnabled.getValue();
         this.renderer.clear();

         for (TexturedParticle3D particle : this.particles) {
            particle.render(this.renderer, stack, provider, partialTicks, 1.0F, scaleOfAlpha, bloom, cameraPos, cameraRotation);
         }

         this.renderer.flush(provider);
      }
   }

   private void addParticlesToWorld(MinecraftClient client, Entity entityIn) {
      this.addParticlesToList(
         client, entityIn, Math.round(this.settings.spawnCountPerTickInWorld.getFloat()), (long)Math.round(this.settings.aliveTimeBaseInWorld.getFloat())
      );
   }

   private void addParticlesToList(MinecraftClient client, Entity entityIn, int countAdd, long aliveTime) {
      Entity entity = (Entity)(entityIn == null ? client.player : entityIn);
      if (entity != null && countAdd > 0 && client.world != null && client.player != null && this.settings.hasEnabledTextures()) {
         Vec3d targetVelocityPos = ParticleWorld.entityVecForParticle(client, entity, 0.5F);
         Vec3d spawnCenter = entity.getEyePos().add(0.0, (double)(-entity.getStandingEyeHeight() / 2.0F), 0.0);
         this.refreshEnabledTextures();
         if (!this.enabledTextures.isEmpty()) {
            for (int i = 0; i < countAdd; i++) {
               Vec3d pos = ParticleWorld.randomSpawnPosition(client, this.random, this.settings, spawnCenter, entity);
               if (pos != null) {
                  ParticleTexture texture = this.randomParticleTexture();
                  if (texture != null) {
                     this.particles
                        .add(
                           new TexturedParticle3D(
                              this.random,
                              this.motion,
                              texture.id(),
                              pos,
                              null,
                              this.settings.speedBase.getFloat(),
                              this.motion.startMotionRotations(client, targetVelocityPos, pos, entity),
                              (float)aliveTime,
                              null,
                              30.0F,
                              this.particleColor(),
                              new ParticleRules(client, this.settings, this.random, entity)
                           )
                        );
                  }
               }
            }

            this.trimParticles();
         }
      }
   }

   private void updateParticlesList(boolean clear, MinecraftClient client) {
      if (clear) {
         this.particles.clear();
      } else if (!this.particles.isEmpty()) {
         this.particles.removeIf(TexturedParticle3D::removeIfDeadInWorld);
         if (!this.particles.isEmpty()) {
            for (TexturedParticle3D particle : this.particles) {
               particle.updateOnTickWorld(client, this.motion, this.particles);
            }

            this.trimParticles();
         }
      }
   }

   private void trimParticles() {
      int overflow = this.particles.size() - 1800;
      if (overflow > 0) {
         this.particles.subList(0, overflow).clear();
      }
   }

   private ParticleTexture randomParticleTexture() {
      return this.enabledTextures.isEmpty() ? null : this.enabledTextures.get(this.random.nextInt(this.enabledTextures.size()));
   }

   private void refreshEnabledTextures() {
      this.enabledTextures.clear();

      for (ParticleTexture texture : ParticleConstants.TEXTURES) {
         if (this.settings.texturesEnabled.isSelected(texture.mode())) {
            this.enabledTextures.add(texture);
         }
      }
   }

   private int particleColor() {
      return this.settings.rainbowColorEnabled.getValue() ? Color.HSBtoRGB(this.random.nextFloat(), 0.75F, 1.0F) : this.settings.color.getValue().getRGB();
   }
}
