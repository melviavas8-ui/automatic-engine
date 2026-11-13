package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public class FireFly extends Module {
   public static final FireFly INSTANCE = new FireFly();
   public final NumberSetting count = this.register(new NumberSetting("Количество", "Количество светлячков", 20.0, 5.0, 120.0, 1.0));
   public final BooleanSetting useThemeColor = this.register(new BooleanSetting("Цветовая тема", "Использовать цветовую тему клиента", true));
   public final ColorSetting customColor = this.register(new ColorSetting("Кастомный цвет", "Цвет светлячков", new Color(255, 215, 0, 255)));
   private static final float SPEED = 0.35F;
   private static final float SPAWN_RADIUS = 35.0F;
   private static final int TRAIL_LENGTH = 25;
   private static final Identifier GLOW_TEXTURE = Identifier.of("daamky", "textures/world/dashbloom.png");
   private final List<FireFly.FireFlyEntity> flies = new ArrayList<>();
   private final List<Vec3d> trailPointsScratch = new ArrayList<>(21);
   private final Random random = new Random();

   public FireFly() {
      super("FireFly", "Светлячки вокруг игрока.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onEnable() {
      this.flies.clear();
   }

   @Override
   protected void onDisable() {
      this.flies.clear();
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         Vec3d playerPos = client.player.getEntityPos();
         float maxSpeed = 0.525F;
         this.flies.forEach(f -> f.update(0.35F, maxSpeed, playerPos));
         this.flies.removeIf(f -> f.isDead(playerPos));
         int target = this.count.getValue().intValue();

         while (this.flies.size() > target) {
            this.flies.remove(this.flies.size() - 1);
         }

         while (this.flies.size() < target) {
            this.spawnFly(playerPos);
         }
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null && !this.flies.isEmpty()) {
         MatrixStack stack = event.getStack();
         Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
         Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
         Quaternionf cameraRotation = mc.gameRenderer.getCamera().getRotation();
         float tickDelta = event.getTickDelta();
         boolean useTheme = this.useThemeColor.getValue();
         long animationTime = System.currentTimeMillis();
         RenderLayer glowLayer = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE);
         VertexConsumer bufferGlow = provider.getBuffer(glowLayer);
         stack.push();
         stack.translate(-cam.x, -cam.y, -cam.z);
         Matrix4f globalMatrix = stack.peek().getPositionMatrix();
         float baseWidth = 0.05F;
         float trailAlphaMultiplier = 0.6F;
         int maxInterpolatedPerSegment = 1;

         for (int fi = 0; fi < this.flies.size(); fi++) {
            FireFly.FireFlyEntity fly = this.flies.get(fi);
            float lifeAlpha = fly.lifeAlpha();
            if (!(lifeAlpha <= 0.01F)) {
               int baseColor;
               if (useTheme) {
                  float hue = (float)(animationTime % 10000L + (long)(fi * 400)) / 10000.0F;
                  baseColor = Color.HSBtoRGB(hue, 0.75F, 1.0F);
               } else {
                  baseColor = this.customColor.getValue().getRGB();
               }

               double px = fly.interpX(tickDelta);
               double py = fly.interpY(tickDelta);
               double pz = fly.interpZ(tickDelta);
               if (fly.trail.size() >= 2) {
                  List<Vec3d> trailPoints = this.trailPointsScratch;
                  trailPoints.clear();
                  trailPoints.add(new Vec3d(px, py, pz));
                  int maxTrailSegments = Math.min(fly.trail.size(), 10);

                  for (int i = 0; i < maxTrailSegments; i++) {
                     FireFly.TrailPoint tp = fly.trail.get(i);
                     Vec3d p = new Vec3d(tp.x, tp.y, tp.z);
                     Vec3d lerped = new Vec3d((p.x + px) * 0.5, (p.y + py) * 0.5, (p.z + pz) * 0.5);
                     trailPoints.add(lerped);
                     trailPoints.add(p);
                  }

                  for (int i = 0; i < trailPoints.size() - 1; i++) {
                     Vec3d curr = trailPoints.get(i);
                     Vec3d next = trailPoints.get(i + 1);
                     Vec3d seg = next.subtract(curr);
                     if (!(seg.lengthSquared() < 1.0E-6)) {
                        Vec3d dir = seg.normalize();
                        Vec3d camDir = curr.subtract(cam).normalize();
                        Vec3d side = dir.crossProduct(camDir).normalize();
                        float t = (float)i / (float)Math.max(1, trailPoints.size() - 1);
                        float alpha = (1.0F - t) * lifeAlpha * trailAlphaMultiplier;
                        int col = ColorUtil.withAlpha(baseColor, Math.round(alpha * 255.0F));
                        float w1 = baseWidth * (1.0F - t * 0.2F);
                        float w2 = baseWidth * (1.0F - (float)(i + 1) / (float)Math.max(1, trailPoints.size() - 1) * 0.2F);
                        float u2 = (float)(i + 1) / (float)Math.max(1, trailPoints.size() - 1);
                        this.addTrailVertex(bufferGlow, globalMatrix, curr, side, w1, col, t, 0.0F);
                        this.addTrailVertex(bufferGlow, globalMatrix, curr, side, -w1, col, t, 1.0F);
                        this.addTrailVertex(bufferGlow, globalMatrix, next, side, -w2, col, u2, 1.0F);
                        this.addTrailVertex(bufferGlow, globalMatrix, next, side, w2, col, u2, 0.0F);
                     }
                  }
               }

               float pulseAlpha = fly.pulseAlpha() * lifeAlpha;
               stack.push();
               stack.translate(px, py, pz);
               stack.multiply(cameraRotation);
               Matrix4f matrix = stack.peek().getPositionMatrix();
               this.drawQuad(bufferGlow, matrix, 0.8F, ColorUtil.withAlpha(baseColor, (int)(pulseAlpha * 0.16F * 255.0F)));
               this.drawQuad(bufferGlow, matrix, 0.4F, ColorUtil.withAlpha(baseColor, (int)(pulseAlpha * 0.36F * 255.0F)));
               this.drawQuad(bufferGlow, matrix, 0.16F, ColorUtil.withAlpha(16777215, (int)(pulseAlpha * 0.95F * 255.0F)));
               stack.pop();
            }
         }

         stack.pop();
         provider.draw(glowLayer);
      }
   }

   private void addTrailVertex(VertexConsumer buffer, Matrix4f matrix, Vec3d pos, Vec3d side, float width, int color, float u, float v) {
      float x = (float)(pos.x + side.x * (double)width);
      float y = (float)(pos.y + side.y * (double)width);
      float z = (float)(pos.z + side.z * (double)width);
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >> 24 & 0xFF;
      buffer.vertex(matrix, x, y, z).color(r, g, b, a).texture(u, v);
   }

   private void drawQuad(VertexConsumer buffer, Matrix4f matrix, float size, int color) {
      float halfSize = size * 0.5F;
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >> 24 & 0xFF;
      buffer.vertex(matrix, -halfSize, -halfSize, 0.0F).color(r, g, b, a).texture(0.0F, 1.0F);
      buffer.vertex(matrix, halfSize, -halfSize, 0.0F).color(r, g, b, a).texture(1.0F, 1.0F);
      buffer.vertex(matrix, halfSize, halfSize, 0.0F).color(r, g, b, a).texture(1.0F, 0.0F);
      buffer.vertex(matrix, -halfSize, halfSize, 0.0F).color(r, g, b, a).texture(0.0F, 0.0F);
   }

   private void spawnFly(Vec3d playerPos) {
      double dist = this.random.nextDouble() * 30.0 + 5.0;
      double yaw = Math.toRadians(this.random.nextDouble() * 360.0);
      double pitch = Math.toRadians((this.random.nextDouble() - 0.5) * 60.0);
      double velX = -Math.sin(yaw) * Math.cos(pitch) * 0.35F;
      double velY = Math.sin(pitch) * 0.35F * 0.5;
      double velZ = Math.cos(yaw) * Math.cos(pitch) * 0.35F;
      int[] palette = new int[]{16766720, 16776960, 65280, 65535, 16738740, 16753920, 49151};
      int color = palette[this.random.nextInt(palette.length)];
      this.flies
         .add(
            new FireFly.FireFlyEntity(
               playerPos.x - Math.sin(yaw) * dist,
               playerPos.y + (this.random.nextDouble() - 0.3) * 8.0 + 1.0,
               playerPos.z + Math.cos(yaw) * dist,
               velX,
               velY,
               velZ,
               color,
               25
            )
         );
   }

   private static class FireFlyEntity {
      double x;
      double y;
      double z;
      double prevX;
      double prevY;
      double prevZ;
      double velX;
      double velY;
      double velZ;
      double targetVelX;
      double targetVelY;
      double targetVelZ;
      final int baseRandomColor;
      final long spawnTime = System.currentTimeMillis();
      long lastDirectionChange = System.currentTimeMillis();
      final List<FireFly.TrailPoint> trail = new ArrayList<>();
      final int maxTrailLength;
      final Random rng = new Random();

      FireFlyEntity(double x, double y, double z, double velX, double velY, double velZ, int baseRandomColor, int maxTrailLength) {
         this.x = this.prevX = x;
         this.y = this.prevY = y;
         this.z = this.prevZ = z;
         this.velX = this.targetVelX = velX;
         this.velY = this.targetVelY = velY;
         this.velZ = this.targetVelZ = velZ;
         this.baseRandomColor = baseRandomColor;
         this.maxTrailLength = maxTrailLength;
      }

      void update(float speedMult, float maxSpeed, Vec3d playerPos) {
         this.prevX = this.x;
         this.prevY = this.y;
         this.prevZ = this.z;
         if (System.currentTimeMillis() - this.lastDirectionChange > (long)(2000 + this.rng.nextInt(2000))) {
            double angle = Math.toRadians(this.rng.nextDouble() * 360.0);
            double pitch = Math.toRadians((this.rng.nextDouble() - 0.5) * 40.0);
            this.targetVelX = -Math.sin(angle) * Math.cos(pitch) * (double)speedMult;
            this.targetVelY = Math.sin(pitch) * (double)speedMult * 0.3;
            this.targetVelZ = Math.cos(angle) * Math.cos(pitch) * (double)speedMult;
            this.lastDirectionChange = System.currentTimeMillis();
         }

         double dx = playerPos.x - this.x;
         double dy = playerPos.y + 1.0 - this.y;
         double dz = playerPos.z - this.z;
         double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
         if (dist > 35.0) {
            this.targetVelX += dx / dist * (double)speedMult * 0.15;
            this.targetVelY += dy / dist * (double)speedMult * 0.15;
            this.targetVelZ += dz / dist * (double)speedMult * 0.15;
         }

         double lerpF = 0.02;
         this.velX = this.velX + (this.targetVelX - this.velX) * lerpF;
         this.velY = this.velY + (this.targetVelY - this.velY) * lerpF;
         this.velZ = this.velZ + (this.targetVelZ - this.velZ) * lerpF;
         double wobble = 0.03;
         this.velX = this.velX + (this.rng.nextDouble() - 0.5) * wobble;
         this.velY = this.velY + (this.rng.nextDouble() - 0.5) * wobble;
         this.velZ = this.velZ + (this.rng.nextDouble() - 0.5) * wobble;
         this.velX = MathHelper.clamp(this.velX, (double)(-maxSpeed), (double)maxSpeed);
         this.velY = MathHelper.clamp(this.velY, (double)(-maxSpeed), (double)maxSpeed);
         this.velZ = MathHelper.clamp(this.velZ, (double)(-maxSpeed), (double)maxSpeed);
         this.x = this.x + this.velX;
         this.y = this.y + this.velY;
         this.z = this.z + this.velZ;
         this.trail.add(0, new FireFly.TrailPoint(this.x, this.y, this.z));

         while (this.trail.size() > this.maxTrailLength) {
            this.trail.remove(this.trail.size() - 1);
         }
      }

      boolean isDead(Vec3d playerPos) {
         double dx = this.x - playerPos.x;
         double dy = this.y - playerPos.y;
         double dz = this.z - playerPos.z;
         return dx * dx + dy * dy + dz * dz > 6400.0;
      }

      double interpX(float td) {
         return MathHelper.lerp((double)td, this.prevX, this.x);
      }

      double interpY(float td) {
         return MathHelper.lerp((double)td, this.prevY, this.y);
      }

      double interpZ(float td) {
         return MathHelper.lerp((double)td, this.prevZ, this.z);
      }

      float pulseAlpha() {
         double pulse = 0.8 + 0.2 * Math.sin((double)(System.currentTimeMillis() - this.spawnTime) / 200.0);
         return (float)pulse;
      }

      float lifeAlpha() {
         long age = System.currentTimeMillis() - this.spawnTime;
         return age < 1000L ? (float)age / 1000.0F : 1.0F;
      }
   }

   private static class TrailPoint {
      double x;
      double y;
      double z;

      TrailPoint(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
   }
}
