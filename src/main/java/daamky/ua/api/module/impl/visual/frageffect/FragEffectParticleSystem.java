package daamky.ua.api.module.impl.visual.frageffect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.world.glow.Glow3D;

public final class FragEffectParticleSystem {
   private final List<FragEffectParticleSystem.Particle> particles = new ArrayList<>();

   public FragEffectParticleSystem() {
   }

   public void spawnBurst(Vec3d center, int lifetimeMin, int lifetimeMax, float maxFlight, int count, float gravity) {
      if (center != null) {
         int safeLifetimeMin = Math.max(1, lifetimeMin);
         float lifetimeTicks = (float)safeLifetimeMin / 50.0F;
         float maxXzMotion = maxFlight / lifetimeTicks * 1.12F;
         float maxYMotion = maxFlight / lifetimeTicks * 0.31F;

         for (int i = 0; i < count; i++) {
            int lifetime = Math.max(1, MathUtils.getRandom(lifetimeMin, lifetimeMax));
            this.particles.add(new FragEffectParticleSystem.Particle(center, lifetime, maxXzMotion, maxYMotion, gravity));
         }
      }
   }

   public void tick() {
      if (!this.particles.isEmpty()) {
         Iterator<FragEffectParticleSystem.Particle> iterator = this.particles.iterator();

         while (iterator.hasNext()) {
            FragEffectParticleSystem.Particle particle = iterator.next();
            if (particle.update()) {
               iterator.remove();
            }
         }
      }
   }

   public void render(float partialTicks) {
      if (!this.particles.isEmpty()) {
         Glow3D.setMatrices(Render3D.lastProjMat, Render3D.lastModMat);
         Glow3D.begin();

         for (FragEffectParticleSystem.Particle particle : this.particles) {
            double x = particle.renderX(partialTicks);
            double y = particle.renderY(partialTicks);
            double z = particle.renderZ(partialTicks);
            int color = particle.color();
            Glow3D.glow(x, y, z, particle.size(), color, particle.alpha() * 0.38F, 0.92F);
            Glow3D.core(x, y, z, particle.size() * 0.22F, color, particle.alpha() * 0.9F);
         }

         Glow3D.end();
      }
   }

   public void clear() {
      this.particles.clear();
   }

   private static final class Particle {
      private double prevX;
      private double prevY;
      private double prevZ;
      private double posX;
      private double posY;
      private double posZ;
      private double motionX;
      private double motionY;
      private double motionZ;
      private final int maxTime;
      private final float gravity;
      private final int baseColor;
      private final float baseSize;
      private final long createdMillis = System.currentTimeMillis();

      private Particle(Vec3d spawnPos, int maxTime, float motionXzSpeedMax, float motionYSpeedMax, float gravity) {
         this.posX = spawnPos.x;
         this.posY = spawnPos.y;
         this.posZ = spawnPos.z;
         this.prevX = spawnPos.x;
         this.prevY = spawnPos.y;
         this.prevZ = spawnPos.z;
         this.motionX = (double)MathUtils.getRandom(-motionXzSpeedMax, motionXzSpeedMax);
         this.motionY = (double)MathUtils.getRandom(-motionYSpeedMax / 4.0F, motionYSpeedMax);
         this.motionZ = (double)MathUtils.getRandom(-motionXzSpeedMax, motionXzSpeedMax);
         this.maxTime = maxTime;
         this.gravity = gravity;
         this.baseColor = Color.HSBtoRGB(0.0F, 0.0F, FragEffectEasing.expoInOut(ThreadLocalRandom.current().nextFloat()));
         this.baseSize = (float)MathUtils.getRandom(0.035, 0.08);
      }

      private boolean update() {
         this.prevX = this.posX;
         this.prevY = this.posY;
         this.prevZ = this.posZ;
         this.posX = this.posX + this.motionX;
         this.posY = this.posY + this.motionY;
         this.posZ = this.posZ + this.motionZ;
         this.motionX *= 0.99;
         this.motionY = (this.motionY - (double)this.gravity) * 0.994;
         this.motionZ *= 0.99;
         return System.currentTimeMillis() - this.createdMillis >= (long)this.maxTime;
      }

      private double renderX(float partialTicks) {
         float tickDelta = Math.clamp(partialTicks, 0.0F, 1.0F);
         return MathHelper.lerp((double)tickDelta, this.prevX, this.posX);
      }

      private double renderY(float partialTicks) {
         float tickDelta = Math.clamp(partialTicks, 0.0F, 1.0F);
         return MathHelper.lerp((double)tickDelta, this.prevY, this.posY);
      }

      private double renderZ(float partialTicks) {
         float tickDelta = Math.clamp(partialTicks, 0.0F, 1.0F);
         return MathHelper.lerp((double)tickDelta, this.prevZ, this.posZ);
      }

      private float alpha() {
         return 1.0F - Math.min((float)(System.currentTimeMillis() - this.createdMillis) / (float)this.maxTime, 1.0F);
      }

      private int color() {
         float factor = this.alpha();
         return ColorUtil.rgba(
            Math.round((float)ColorUtil.getRed(this.baseColor) * factor),
            Math.round((float)ColorUtil.getGreen(this.baseColor) * factor),
            Math.round((float)ColorUtil.getBlue(this.baseColor) * factor),
            Math.round(255.0F * factor)
         );
      }

      private float size() {
         return this.baseSize;
      }
   }
}
