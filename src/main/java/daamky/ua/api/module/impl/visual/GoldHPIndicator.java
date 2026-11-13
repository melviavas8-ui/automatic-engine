package daamky.ua.api.module.impl.visual;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class GoldHPIndicator extends Module {
   private static final int MAX_PARTICLES = 80;
   private static final Identifier HEART_TEXTURE = Identifier.of("daamky", "textures/features/particles/heart.png");
   private static final Identifier GLOW_TEXTURE = Identifier.of("daamky", "textures/particles/glow.png");
   private static final Random RANDOM = new Random();
   private static GoldHPIndicator instance;
   private final NumberSetting heartSize = this.register(new NumberSetting("Size", "Heart particle size.", 0.25, 0.05, 0.8, 0.05));
   private final NumberSetting spawnRate = this.register(new NumberSetting("Spawn Rate", "Hearts per second per player.", 3.0, 0.5, 10.0, 0.5));
   private final NumberSetting lifetime = this.register(new NumberSetting("Lifetime", "Heart lifetime ms.", 1500.0, 500.0, 4000.0, 100.0));
   private final NumberSetting spread = this.register(new NumberSetting("Spread", "Horizontal spread.", 0.4, 0.0, 1.0, 0.05));
   private final NumberSetting riseSpeed = this.register(new NumberSetting("Rise Speed", "Upward speed.", 0.03, 0.005, 0.1, 0.005));
   private final BooleanSetting gold = this.register(new BooleanSetting("reflect", "", false));
   private final List<GoldHPIndicator.HeartParticle> particles = new ArrayList<>();
   private long lastTickTime = System.currentTimeMillis();
   private float spawnCarry;

   public GoldHPIndicator() {
      super("GoldHPIndicator", "Golden hearts on players with absorption.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static GoldHPIndicator getInstance() {
      return instance;
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      if (mc.world != null && mc.player != null) {
         long now = System.currentTimeMillis();
         float dt = Math.min(0.1F, (float)(now - this.lastTickTime) / 1000.0F);
         this.lastTickTime = now;

         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
               PlayerEntity player = (PlayerEntity)entity;
               if (player.isAlive() && (this.gold.getValue() || player != mc.player)) {
                  float absorption = player.getAbsorptionAmount();
                  if (absorption > 0.0F) {
                     float rate = this.spawnRate.getFloat();
                     this.spawnCarry += rate * dt;
                     if (this.spawnCarry >= 1.0F || RANDOM.nextFloat() < this.spawnCarry) {
                        this.spawnCarry = Math.max(0.0F, this.spawnCarry - 1.0F);
                        this.spawnHeart(player);
                     }
                  }
               }
            }
         }

         Iterator<GoldHPIndicator.HeartParticle> it = this.particles.iterator();

         while (it.hasNext()) {
            GoldHPIndicator.HeartParticle p = it.next();
            p.prevX = p.x;
            p.prevY = p.y;
            p.prevZ = p.z;
            p.prevAge = p.age;
            p.age += (long)(dt * 1000.0F);
            p.y = p.y + (double)(this.riseSpeed.getFloat() * dt * 60.0F);
            p.x = p.x + p.motionX * (double)dt;
            p.z = p.z + p.motionZ * (double)dt;
            if (p.age >= p.lifetime) {
               it.remove();
            }
         }
      }
   }

   private void spawnHeart(LivingEntity entity) {
      if (this.particles.size() >= 80) {
         this.particles.remove(0);
      }

      double x = entity.getX() + (RANDOM.nextDouble() - 0.5) * this.spread.getValue() * 2.0;
      double y = entity.getY() + (double)entity.getHeight() * 0.8 + RANDOM.nextDouble() * 0.3;
      double z = entity.getZ() + (RANDOM.nextDouble() - 0.5) * this.spread.getValue() * 2.0;
      double motionX = (RANDOM.nextDouble() - 0.5) * 0.008;
      double motionZ = (RANDOM.nextDouble() - 0.5) * 0.008;
      long lt = this.lifetime.getValue().longValue() + RANDOM.nextLong(500L);
      this.particles.add(new GoldHPIndicator.HeartParticle(x, y, z, motionX, motionZ, lt, this.heartSize.getFloat(), RANDOM.nextLong()));
   }

   @SubscribeEvent
   private void onRender(WorldRenderEvent event) {
      if (!this.particles.isEmpty() && mc.world != null) {
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cam = camera.getCameraPos();
         Quaternionf camRot = camera.getRotation();
         MatrixStack stack = event.getStack();
         Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
         RenderLayer glowType = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE);
         RenderLayer heartType = ClientPipelines.WORLD_PARTICLES_GLOW.apply(HEART_TEXTURE);
         float partialTicks = MathHelper.clamp(event.getPartialTicks(), 0.0F, 1.0F);
         VertexConsumer glowConsumer = provider.getBuffer(glowType);
         boolean renderedGlow = false;

         for (GoldHPIndicator.HeartParticle p : this.particles) {
            renderedGlow |= renderParticle(stack, cam, camRot, p, glowConsumer, partialTicks, true);
         }

         if (renderedGlow) {
            provider.draw(glowType);
         }

         VertexConsumer heartConsumer = provider.getBuffer(heartType);
         boolean renderedHeart = false;

         for (GoldHPIndicator.HeartParticle p : this.particles) {
            renderedHeart |= renderParticle(stack, cam, camRot, p, heartConsumer, partialTicks, false);
         }

         if (renderedHeart) {
            provider.draw(heartType);
         }
      }
   }

   private static boolean renderParticle(
      MatrixStack stack, Vec3d cam, Quaternionf camRot, GoldHPIndicator.HeartParticle p, VertexConsumer consumer, float partialTicks, boolean glow
   ) {
      float renderAge = MathHelper.lerp(partialTicks, (float)p.prevAge, (float)p.age);
      float progress = Math.min(1.0F, renderAge / (float)p.lifetime);
      float fade = 1.0F - progress;
      if (fade <= 0.0F) {
         return false;
      } else {
         double renderX = MathHelper.lerp((double)partialTicks, p.prevX, p.x);
         double renderY = MathHelper.lerp((double)partialTicks, p.prevY, p.y);
         double renderZ = MathHelper.lerp((double)partialTicks, p.prevZ, p.z);
         float time = (renderAge + (float)(p.seed & 1023L)) / 1000.0F;
         float bobX = (float)Math.sin((double)(time * 2.2F + p.phase)) * 0.045F;
         float bobY = (float)Math.sin((double)(time * 3.0F + p.phase * 0.7F)) * 0.025F;
         float bobZ = (float)Math.cos((double)(time * 2.0F + p.phase)) * 0.045F;
         float pulse = 0.9F + 0.1F * (float)Math.sin((double)(time * 4.0F + p.phase));
         float scale = p.size * pulse * (1.0F - progress * 0.25F);
         int color = glow ? ColorUtil.rgba(255, 205, 70, (int)(70.0F * fade)) : ColorUtil.rgba(255, 225, 85, (int)(255.0F * fade));
         if (glow) {
            renderQuad(consumer, stack.peek(), scale * 2.25F, ColorUtil.rgba(255, 185, 45, (int)(28.0F * fade)));
            renderQuad(consumer, stack.peek(), scale * 1.55F, ColorUtil.rgba(255, 215, 75, (int)(48.0F * fade)));
         } else {
            renderQuad(consumer, stack.peek(), scale, ColorUtil.rgba(255, 235, 105, (int)(255.0F * fade)));
         }

         stack.push();
         stack.translate(renderX + (double)bobX - cam.x, renderY + (double)bobY - cam.y, renderZ + (double)bobZ - cam.z);
         stack.multiply(camRot);
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
         renderQuad(consumer, stack.peek(), glow ? scale * 1.7F : scale, color);
         stack.pop();
         return true;
      }
   }

   private static void renderQuad(VertexConsumer consumer, Entry pose, float scale, int color) {
      WorldVertex.textured(consumer, pose, -scale, -scale, 0.0F, 0.0F, 0.0F, color);
      WorldVertex.textured(consumer, pose, scale, -scale, 0.0F, 1.0F, 0.0F, color);
      WorldVertex.textured(consumer, pose, scale, scale, 0.0F, 1.0F, 1.0F, color);
      WorldVertex.textured(consumer, pose, -scale, scale, 0.0F, 0.0F, 1.0F, color);
   }

   private static final class HeartParticle {
      double x;
      double y;
      double z;
      double prevX;
      double prevY;
      double prevZ;
      double motionX;
      double motionZ;
      long age;
      long prevAge;
      long lifetime;
      long seed;
      float size;
      float phase;

      HeartParticle(double x, double y, double z, double motionX, double motionZ, long lifetime, float size, long seed) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.prevX = x;
         this.prevY = y;
         this.prevZ = z;
         this.motionX = motionX;
         this.motionZ = motionZ;
         this.lifetime = lifetime;
         this.size = size;
         this.seed = seed;
         this.phase = (float)(seed & 4095L) / 4095.0F * (float) (Math.PI * 2);
      }
   }
}
