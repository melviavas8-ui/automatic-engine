package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.MathUtils;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class World extends Module {
   private static final int MAX_PARTICLES = 200;
   private static final long FADE_MS = 1000L;
   private static final Identifier BLOOM_TEX = Identifier.of("cataclysm", "textures/world/dashbloom.png");
   private final ColorSetting color = this.register(new ColorSetting("Color", "Particle color.", new Color(255, 101, 57)));
   private final NumberSetting density = this.register(new NumberSetting("Density", "Maximum particles.", 100.0, 10.0, 200.0, 1.0));
   private final NumberSetting radius = this.register(new NumberSetting("Radius", "Particle spawn radius.", 20.0, 5.0, 40.0, 1.0));
   private final List<World.Particle> particles = new ArrayList<>();

   public World() {
      super("World", "Renders floating cube particles around the world.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onDisable() {
      this.particles.clear();
   }

   @SubscribeEvent
   public void onTick(TickEvent e) {
      if (mc.player != null && mc.world != null) {
         this.particles.removeIf(p -> p.timer.elapsed() >= p.liveMs + 1000L);

         for (World.Particle p : this.particles) {
            p.tick();
         }

         int max = Math.min(this.density.getValue().intValue(), 200);
         int toSpawn = Math.min(4, max - this.particles.size());
         float r = this.radius.getValue().floatValue();
         Vec3d playerPos = mc.player.getEntityPos();

         for (int i = 0; i < toSpawn; i++) {
            this.particles
               .add(
                  new World.Particle(
                     playerPos.add((double)MathUtils.getRandom(-r, r), (double)MathUtils.getRandom(0.0F, 5.0F), (double)MathUtils.getRandom(-r, r)),
                     Vec3d.ZERO,
                     new Vec3d((double)MathUtils.getRandom(-1.0F, 1.0F), (double)MathUtils.getRandom(0.0F, 2.0F), (double)MathUtils.getRandom(-1.0F, 1.0F)),
                     new Vec3d((double)MathUtils.getRandom(-1.0F, 1.0F), (double)MathUtils.getRandom(-1.0F, 1.0F), (double)MathUtils.getRandom(-1.0F, 1.0F)),
                     (long)MathUtils.getRandom(1500.0F, 4500.0F),
                     MathUtils.getRandom(0.1F, 0.3F)
                  )
               );
         }
      }
   }

   @SubscribeEvent
   public void onWorldRender(WorldRenderEvent e) {
      if (!this.particles.isEmpty()) {
         if (mc.player != null && mc.world != null) {
            MatrixStack ms = e.getStack();
            Camera camera = mc.gameRenderer.getCamera();
            Vec3d cameraPos = Render3D.lastCameraPos;
            int baseColor = this.color.getValue().getRGB();
            Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            VertexConsumer bloom = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(BLOOM_TEX));

            for (World.Particle particle : this.particles) {
               float a = particle.computeAlpha();
               if (!(a <= 0.0F)) {
                  Vec3d pos = daamky.ua.utils.math.MathUtils.interpolate(particle.prev, particle.pos);
                  float bigSize = 4.0F * particle.size;
                  int alphaI = clampByte(Math.round(255.0F * a * 0.4F));
                  if (alphaI != 0) {
                     int col = ColorUtil.withAlpha(baseColor, alphaI);
                     ms.push();
                     ms.translate((float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y), (float)(pos.z - cameraPos.z));
                     ms.multiply(camera.getRotation());
                     Entry pose = ms.peek();
                     float h = bigSize / 2.0F;
                     bloom.vertex(pose, -h, -h, 0.0F).texture(0.0F, 0.0F).color(col);
                     bloom.vertex(pose, h, -h, 0.0F).texture(1.0F, 0.0F).color(col);
                     bloom.vertex(pose, h, h, 0.0F).texture(1.0F, 1.0F).color(col);
                     bloom.vertex(pose, -h, h, 0.0F).texture(0.0F, 1.0F).color(col);
                     ms.pop();
                  }
               }
            }

            immediate.draw(ClientPipelines.BLOOM_ESP.apply(BLOOM_TEX));
            VertexConsumer lines = immediate.getBuffer(ClientPipelines.WORLD_PARTICLES_LINES);

            for (World.Particle particlex : this.particles) {
               float a = particlex.computeAlpha();
               if (!(a <= 0.0F)) {
                  Vec3d pos = daamky.ua.utils.math.MathUtils.interpolate(particlex.prev, particlex.pos);
                  Vec3d rot = daamky.ua.utils.math.MathUtils.interpolate(particlex.prevRot, particlex.rotate);
                  int outlineCol = ColorUtil.withAlpha(baseColor, clampByte(Math.round(205.0F * a)));
                  int diagCol = ColorUtil.withAlpha(baseColor, clampByte(Math.round(102.0F * a)));
                  ms.push();
                  ms.translate((float)(pos.x - cameraPos.x), (float)(pos.y - cameraPos.y), (float)(pos.z - cameraPos.z));
                  ms.multiply(new Quaternionf().rotationXYZ((float)rot.x, (float)rot.y, (float)rot.z));
                  ms.scale(particlex.size, particlex.size, particlex.size);
                  Entry pose = ms.peek();
                  drawCubeOutline(lines, pose, outlineCol);
                  drawCubeDiagonals(lines, pose, diagCol);
                  ms.pop();
               }
            }

            immediate.draw(ClientPipelines.WORLD_PARTICLES_LINES);
         }
      }
   }

   private static void drawCubeOutline(VertexConsumer buf, Entry pose, int col) {
      float a = -0.5F;
      float b = 0.5F;
      line(buf, pose, a, a, a, b, a, a, col);
      line(buf, pose, b, a, a, b, a, b, col);
      line(buf, pose, b, a, b, a, a, b, col);
      line(buf, pose, a, a, b, a, a, a, col);
      line(buf, pose, a, b, a, b, b, a, col);
      line(buf, pose, b, b, a, b, b, b, col);
      line(buf, pose, b, b, b, a, b, b, col);
      line(buf, pose, a, b, b, a, b, a, col);
      line(buf, pose, a, a, a, a, b, a, col);
      line(buf, pose, b, a, a, b, b, a, col);
      line(buf, pose, b, a, b, b, b, b, col);
      line(buf, pose, a, a, b, a, b, b, col);
   }

   private static void drawCubeDiagonals(VertexConsumer buf, Entry pose, int col) {
      float a = -0.5F;
      float b = 0.5F;
      line(buf, pose, a, a, a, b, b, b, col);
      line(buf, pose, b, a, a, a, b, b, col);
      line(buf, pose, a, a, b, b, b, a, col);
      line(buf, pose, b, a, b, a, b, a, col);
   }

   private static void line(VertexConsumer buf, Entry pose, float x1, float y1, float z1, float x2, float y2, float z2, int col) {
      buf.vertex(pose, x1, y1, z1).color(col);
      buf.vertex(pose, x2, y2, z2).color(col);
   }

   private static int clampByte(int v) {
      return v < 0 ? 0 : Math.min(v, 255);
   }

   private static final class Particle {
      Vec3d prev;
      Vec3d prevRot;
      Vec3d pos;
      Vec3d rotate;
      Vec3d motion;
      Vec3d rotateMotion;
      final long liveMs;
      final float size;
      final World.Timer timer = new World.Timer();

      Particle(Vec3d pos, Vec3d rotate, Vec3d motion, Vec3d rotateMotion, long liveMs, float size) {
         this.pos = pos;
         this.prev = pos;
         this.rotate = rotate;
         this.prevRot = rotate;
         this.motion = motion.multiply(0.04);
         this.rotateMotion = rotateMotion.multiply(0.04);
         this.liveMs = liveMs;
         this.size = size;
      }

      void tick() {
         this.prev = this.pos;
         this.prevRot = this.rotate;
         this.pos = this.pos.add(this.motion);
         this.rotate = this.rotate.add(this.rotateMotion);
         this.motion = this.motion.multiply(0.98);
         this.rotateMotion = this.rotateMotion.multiply(0.98);
      }

      float computeAlpha() {
         long t = this.timer.elapsed();
         if (t < 1000L) {
            float p = (float)t / 1000.0F;
            return easeOutCubic(p);
         } else if (t < this.liveMs) {
            return 1.0F;
         } else {
            long fade = t - this.liveMs;
            if (fade >= 1000L) {
               return 0.0F;
            } else {
               float p = (float)fade / 1000.0F;
               return 1.0F - easeOutCubic(p);
            }
         }
      }

      private static float easeOutCubic(float x) {
         float inv = 1.0F - x;
         return 1.0F - inv * inv * inv;
      }
   }

   private static final class Timer {
      final long start = System.currentTimeMillis();

      private Timer() {
      }

      long elapsed() {
         return System.currentTimeMillis() - this.start;
      }
   }
}
