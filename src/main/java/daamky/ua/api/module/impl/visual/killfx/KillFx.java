package daamky.ua.api.module.impl.visual.killfx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.world.glow.Glow3D;

/**
 * Лёгкий движок килл- и тотем-эффектов.
 * Никаких сущностей, текстур и фреймбуферов: только линии Render3D и glow-спрайты Glow3D.
 * Позиции частиц считаются формулой от времени, поэтому нет тиков, списков частиц и мусора для GC.
 */
public final class KillFx {
   public enum Kind {
      NOVA(1500L), HALO(2400L), HELIX(2200L), SHATTER(1400L), THUNDER(1100L), VORTEX(2000L),
      T_CROSS(2300L), T_WINGS(2100L), T_RINGS(1800L), T_SPIRAL(2000L);

      final long baseMs;

      Kind(long baseMs) {
         this.baseMs = baseMs;
      }
   }

   private static final int MAX_ACTIVE = 8;
   private static final float TAU = (float)(Math.PI * 2.0);
   private final List<Fx> active = new ArrayList<>();
   private int seedCounter = 1;
   private static double OX, OY, OZ;
   private static float SC = 1.0F;

   public void spawn(Kind kind, Vec3d pos, float height, float bodyYawDeg, int color, float speed, float scale) {
      if (kind == null || pos == null) {
         return;
      }
      while (this.active.size() >= MAX_ACTIVE) {
         this.active.remove(0);
      }
      long dur = Math.max(300L, (long)(kind.baseMs / Math.clamp(speed, 0.25F, 2.0F)));
      this.active.add(new Fx(kind, pos.x, pos.y, pos.z, Math.max(height, 0.6F), bodyYawDeg, color | 0xFF000000, System.currentTimeMillis(), dur, this.seedCounter++ * 7919, Math.clamp(scale, 0.3F, 5.0F)));
   }

   public boolean isEmpty() {
      return this.active.isEmpty();
   }

   public void clear() {
      this.active.clear();
   }

   public void render() {
      if (this.active.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      Glow3D.setMatrices(Render3D.lastProjMat, Render3D.lastModMat);
      Glow3D.begin();
      Iterator<Fx> it = this.active.iterator();
      while (it.hasNext()) {
         Fx f = it.next();
         long el = now - f.start;
         if (el >= f.dur) {
            it.remove();
            continue;
         }
         float u = (float)el / (float)f.dur;
         float t = (float)el / 1000.0F * ((float)f.kind.baseMs / (float)f.dur);
         boolean body = f.kind == Kind.T_WINGS || f.kind == Kind.T_CROSS;
         OX = f.x;
         OY = body ? f.y + f.h * 0.6 : f.y;
         OZ = f.z;
         SC = body ? 1.0F + (f.sc - 1.0F) * 0.6F : f.sc;
         switch (f.kind) {
            case NOVA -> nova(f, u, t);
            case HALO -> halo(f, u, t);
            case HELIX -> helix(f, u, t);
            case SHATTER -> shatter(f, u, t);
            case THUNDER -> thunder(f, u, t);
            case VORTEX -> vortex(f, u, t);
            case T_CROSS -> cross(f, u, t);
            case T_WINGS -> wings(f, u, t);
            case T_RINGS -> rings(f, u, t);
            case T_SPIRAL -> spiral(f, u, t);
         }
      }
      Glow3D.end();
   }

   // ───────────────────────── KILL EFFECTS ─────────────────────────

   /** Кольца по земле + световой столб + искры. */
   private static void nova(Fx f, float u, float t) {
      double cy = f.y + f.h * 0.5;
      for (int r = 0; r < 2; r++) {
         float ur = clamp01((u - r * 0.12F) / (1.0F - r * 0.12F));
         if (ur <= 0.0F) continue;
         float rad = (r == 0 ? 3.2F : 2.1F) * easeOut(ur);
         ring(f.x, f.y + 0.05, f.z, rad, 40, f.color, (int)(230.0F * (float)Math.pow(1.0F - ur, 1.6F)), r == 0 ? 3.0F : 2.0F, 0.0F);
      }
      float beamH = 7.0F * easeOut(Math.min(1.0F, u * 2.5F));
      float ba = (float)Math.pow(1.0F - u, 1.2F);
      line(new Vec3d(f.x, f.y, f.z), new Vec3d(f.x, f.y + beamH, f.z), ColorUtil.withAlpha(ColorUtil.WHITE, (int)(255 * ba)), ColorUtil.withAlpha(f.color, 0), 3.0F, true);
      line(new Vec3d(f.x, f.y, f.z), new Vec3d(f.x, f.y + beamH, f.z), ColorUtil.withAlpha(f.color, (int)(140 * ba)), ColorUtil.withAlpha(f.color, 0), 8.0F, true);
      for (int k = 0; k < 8; k++) {
         float s = k / 8.0F;
         glow(f.x, f.y + s * beamH, f.z, 0.45F - 0.3F * s, f.color, 0.55F * ba);
      }
      for (int i = 0; i < 28; i++) {
         float a = rnd(f.seed, i, 0) * TAU;
         float spd = 1.5F + rnd(f.seed, i, 1) * 3.2F;
         float up = 2.0F + rnd(f.seed, i, 2) * 3.0F;
         float d = 1.0F - (float)Math.exp(-2.4F * t);
         glow(f.x + Math.cos(a) * spd * d, cy + up * t - 4.0F * t * t, f.z + Math.sin(a) * spd * d, 0.09F + rnd(f.seed, i, 3) * 0.06F, mix(f.color, ColorUtil.WHITE, 0.4F), (1.0F - u) * 0.95F);
      }
   }

   /** Душа поднимается вверх с нимбом, вокруг летят «перья». */
   private static void halo(Fx f, float u, float t) {
      float rise = 3.6F * easeOut(Math.min(1.0F, u / 0.85F));
      float env = env(u, 0.12F, 0.4F);
      double ox = f.x, oy = f.y + f.h * 0.6 + rise, oz = f.z;
      int light = mix(f.color, ColorUtil.WHITE, 0.55F);
      glow(ox, oy, oz, 0.7F, f.color, 0.8F * env);
      glow(ox, oy, oz, 0.3F, light, 0.9F * env);
      ring(ox, oy + 0.6, oz, 0.34F, 22, light, (int)(235 * env), 2.4F, t * 2.0F);
      glow(ox, oy + 0.6, oz, 0.18F, light, 0.5F * env);
      line(new Vec3d(f.x, f.y + f.h * 0.6, f.z), new Vec3d(ox, oy, oz), ColorUtil.withAlpha(f.color, 0), ColorUtil.withAlpha(light, (int)(110 * env)), 2.0F, true);
      for (int i = 0; i < 26; i++) {
         float p = rnd(f.seed, i, 0);
         float age = (t * 0.55F + p) % 1.0F;
         float a = rnd(f.seed, i, 1) * TAU + age * 3.0F;
         float r = 0.15F + 0.55F * age;
         glow(f.x + Math.cos(a) * r, f.y + f.h * 0.5 + age * (rise + 1.2F), f.z + Math.sin(a) * r, 0.06F + rnd(f.seed, i, 2) * 0.05F, light, (float)Math.sin(Math.PI * age) * env);
      }
   }

   /** Двойная спираль (ДНК) поднимается вокруг точки убийства. */
   private static void helix(Fx f, float u, float t) {
      float top = easeOut(Math.min(1.0F, u * 1.5F));
      float env = env(u, 0.08F, 0.35F);
      float shrink = 1.0F - Math.max(0.0F, (u - 0.75F) / 0.25F) * 0.8F;
      int light = mix(f.color, ColorUtil.WHITE, 0.45F);
      int n = 24;
      for (int j = 0; j < n; j++) {
         float s = j / (float)(n - 1);
         if (s > top) break;
         float rad = 0.85F * (1.0F - s * 0.35F) * shrink;
         float hy = s * 3.8F;
         double ax = 0, az = 0, bx = 0, bz = 0;
         for (int k = 0; k < 2; k++) {
            float ang = s * (float)Math.PI * 5.0F + t * 4.0F + k * (float)Math.PI;
            double px = f.x + Math.cos(ang) * rad, pz = f.z + Math.sin(ang) * rad;
            glow(px, f.y + 0.1 + hy, pz, 0.17F - 0.08F * s, k == 0 ? f.color : light, 0.9F * env);
            if (k == 0) { ax = px; az = pz; } else { bx = px; bz = pz; }
         }
         if (j % 3 == 0) {
            line(new Vec3d(ax, f.y + 0.1 + hy, az), new Vec3d(bx, f.y + 0.1 + hy, bz), ColorUtil.withAlpha(f.color, (int)(110 * env)), ColorUtil.withAlpha(light, (int)(110 * env)), 1.6F, true);
         }
      }
      if (u > 0.8F) {
         float fl = (u - 0.8F) / 0.2F;
         glow(f.x, f.y + 3.9, f.z, 1.1F * (1.0F - fl), light, 0.9F * (1.0F - fl));
      }
   }

   /** Осколки с хвостами + трещины по земле. */
   private static void shatter(Fx f, float u, float t) {
      double cy = f.y + f.h * 0.5;
      float fade = (float)Math.pow(1.0F - u, 1.3F);
      int light = mix(f.color, ColorUtil.WHITE, 0.5F);
      for (int i = 0; i < 52; i++) {
         float th = rnd(f.seed, i, 0) * TAU;
         float ph = -0.3F + rnd(f.seed, i, 1) * 1.3F;
         float cp = (float)Math.cos(ph);
         float dx = (float)Math.cos(th) * cp, dy = (float)Math.sin(ph) + 0.2F, dz = (float)Math.sin(th) * cp;
         float spd = 2.5F + rnd(f.seed, i, 2) * 5.0F;
         double[] p1 = shard(f.x, cy, f.z, dx, dy, dz, spd, t);
         double[] p0 = shard(f.x, cy, f.z, dx, dy, dz, spd, Math.max(0.0F, t - 0.06F));
         int c = rnd(f.seed, i, 3) > 0.5F ? f.color : light;
         glow(p1[0], p1[1], p1[2], 0.08F + rnd(f.seed, i, 4) * 0.06F, c, fade);
         line(new Vec3d(p0[0], p0[1], p0[2]), new Vec3d(p1[0], p1[1], p1[2]), ColorUtil.withAlpha(c, 0), ColorUtil.withAlpha(c, (int)(200 * fade)), 1.5F, true);
      }
      float fl = 1.0F - easeOut(Math.min(1.0F, u * 3.0F));
      if (fl > 0.0F) glow(f.x, cy, f.z, 1.5F * fl + 0.2F, light, 0.9F * fl);
      float len = 2.4F * easeOut(Math.min(1.0F, u * 2.5F));
      for (int i = 0; i < 8; i++) {
         float a = i * TAU / 8.0F + rnd(f.seed, i, 5) * 0.3F;
         double mx = f.x + Math.cos(a) * len * 0.5 + (rnd(f.seed, i, 6) - 0.5F) * 0.3F;
         double mz = f.z + Math.sin(a) * len * 0.5 + (rnd(f.seed, i, 7) - 0.5F) * 0.3F;
         line(new Vec3d(f.x, f.y + 0.04, f.z), new Vec3d(mx, f.y + 0.04, mz), ColorUtil.withAlpha(f.color, (int)(220 * fade)), ColorUtil.withAlpha(f.color, (int)(160 * fade)), 2.2F, true);
         line(new Vec3d(mx, f.y + 0.04, mz), new Vec3d(f.x + Math.cos(a) * len, f.y + 0.04, f.z + Math.sin(a) * len), ColorUtil.withAlpha(f.color, (int)(160 * fade)), ColorUtil.withAlpha(f.color, 0), 1.6F, true);
      }
   }

   private static double[] shard(double x, double y, double z, float dx, float dy, float dz, float spd, float t) {
      float d = (1.0F - (float)Math.exp(-3.0F * t)) / 3.0F;
      return new double[]{x + dx * spd * d, y + dy * spd * d - 4.5F * t * t, z + dz * spd * d};
   }

   /** Молния с неба с миганием, веткой и вспышкой. */
   private static void thunder(Fx f, float u, float t) {
      float fl = u < 0.1F ? 1.0F : u < 0.18F ? 0.25F : u < 0.28F ? 1.0F : Math.max(0.0F, 1.0F - (u - 0.28F) / 0.72F);
      int light = mix(f.color, ColorUtil.WHITE, 0.7F);
      int n = 14;
      Vec3d prev = null;
      Vec3d[] branches = new Vec3d[3];
      int nb = 0;
      for (int k = 0; k <= n; k++) {
         float s = k / (float)n;
         float amp = (1.0F - s) * 3.2F;
         Vec3d p = new Vec3d(f.x + (rnd(f.seed, k, 0) - 0.5F) * amp, f.y + 42.0F * (1.0F - s), f.z + (rnd(f.seed, k, 1) - 0.5F) * amp);
         if (k == 4 || k == 7 || k == 10) branches[nb++] = p;
         if (prev != null) {
            line(prev, p, ColorUtil.withAlpha(f.color, (int)(110 * fl)), ColorUtil.withAlpha(f.color, (int)(110 * fl)), 16.0F, true);
            line(prev, p, ColorUtil.withAlpha(light, (int)(255 * fl)), ColorUtil.withAlpha(light, (int)(255 * fl)), 5.5F, true);
            glow(p.x, p.y, p.z, 0.55F, f.color, 0.45F * fl);
         }
         prev = p;
      }
      for (int bi = 0; bi < nb; bi++) {
         Vec3d b = branches[bi];
         float dirx = rnd(f.seed, bi, 8) - 0.5F, dirz = rnd(f.seed, bi, 9) - 0.5F;
         for (int k = 0; k < 6; k++) {
            Vec3d nbp = b.add(dirx * 3.2F + (rnd(f.seed, k + bi * 7, 2) - 0.5F) * 1.6F, -2.6F, dirz * 3.2F + (rnd(f.seed, k + bi * 7, 3) - 0.5F) * 1.6F);
            line(b, nbp, ColorUtil.withAlpha(light, (int)(210 * fl)), ColorUtil.withAlpha(light, (int)(210 * fl)), 3.0F, true);
            b = nbp;
         }
      }
      glow(f.x, f.y + 0.3, f.z, 3.2F * fl + 0.2F, f.color, 0.85F * fl);
      glow(f.x, f.y + 0.3, f.z, 0.7F * fl + 0.1F, light, 0.9F * fl);
      if (u < 0.5F) ring(f.x, f.y + 0.05, f.z, 4.5F * easeOut(u * 2.0F), 36, light, (int)(220 * (1.0F - u * 2.0F)), 2.4F, 0.0F);
      for (int i = 0; i < 18; i++) {
         float a = rnd(f.seed, i, 4) * TAU;
         float spd = 1.0F + rnd(f.seed, i, 5) * 2.5F;
         float d = 1.0F - (float)Math.exp(-3.0F * t);
         glow(f.x + Math.cos(a) * spd * d, f.y + 0.2 + (1.5F + rnd(f.seed, i, 6) * 2.0F) * t - 5.0F * t * t, f.z + Math.sin(a) * spd * d, 0.07F, light, fl);
      }
   }

   /** Частицы затягивает в сингулярность, затем взрыв. */
   private static void vortex(Fx f, float u, float t) {
      double cy = f.y + f.h * 0.5;
      float P = 0.6F;
      int light = mix(f.color, ColorUtil.WHITE, 0.5F);
      if (u < P) {
         float k = u / P, k2 = k * k;
         float a = Math.min(1.0F, u * 5.0F);
         for (int i = 0; i < 44; i++) {
            float r0 = 1.6F + rnd(f.seed, i, 0) * 1.2F;
            float a0 = rnd(f.seed, i, 1) * TAU;
            float h0 = (rnd(f.seed, i, 2) - 0.5F) * 1.4F;
            float r = r0 * (1.0F - k2);
            float ang = a0 + k2 * 9.0F * (1.0F + rnd(f.seed, i, 3));
            glow(f.x + Math.cos(ang) * r, cy + h0 * (1.0F - k), f.z + Math.sin(ang) * r, 0.08F + 0.05F * rnd(f.seed, i, 4), i % 3 == 0 ? light : f.color, 0.9F * a);
         }
         glow(f.x, cy, f.z, 0.15F + 0.5F * k2, f.color, 0.7F * a);
         glow(f.x, cy, f.z, 0.08F + 0.2F * k2, ColorUtil.WHITE, 0.9F * a);
      } else {
         float b = (u - P) / (1.0F - P), e = easeOut(b), fade = (float)Math.pow(1.0F - b, 1.2F);
         for (int i = 0; i < 44; i++) {
            float a = rnd(f.seed, i, 1) * TAU;
            float r = 3.2F * e * (0.5F + rnd(f.seed, i, 0) * 0.5F);
            glow(f.x + Math.cos(a) * r, cy + (rnd(f.seed, i, 2) - 0.5F) * e * 2.5F, f.z + Math.sin(a) * r, 0.1F + 0.05F * rnd(f.seed, i, 4), i % 3 == 0 ? light : f.color, fade);
         }
         ring(f.x, f.y + 0.05, f.z, 3.4F * e, 40, light, (int)(230 * fade), 3.0F, 0.0F);
         if (b < 0.4F) glow(f.x, cy, f.z, 1.6F * (1.0F - b / 0.4F), light, 0.9F * (1.0F - b / 0.4F));
      }
   }

   // ───────────────────────── TOTEM EFFECTS ─────────────────────────

   /** Крест вылетает из игрока, разворачивается к камере, сияет и гаснет со вспышкой. */
   private static void cross(Fx f, float u, float t) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float camYaw = mc.gameRenderer != null && mc.gameRenderer.getCamera() != null ? (float)Math.toRadians(mc.gameRenderer.getCamera().getYaw()) : 0.0F;
      float swing = (float)Math.sin(t * 2.2F) * 0.5F;
      double hx = Math.cos(camYaw + swing), hz = Math.sin(camYaw + swing);
      float env = env(u, 0.12F, 0.35F);
      float s = 0.2F + 0.8F * easeOut(Math.min(1.0F, u / 0.3F));
      double cx = f.x, cy = f.y + f.h * 0.6 + 2.8F * easeOut(Math.min(1.0F, u / 0.75F)), cz = f.z;
      int gold = f.color, light = mix(f.color, ColorUtil.WHITE, 0.65F);
      Vec3d v0 = new Vec3d(cx, cy - 0.95F * s, cz), v1 = new Vec3d(cx, cy + 0.95F * s, cz);
      double hy = cy + 0.35F * s;
      Vec3d h0 = new Vec3d(cx - hx * 0.55F * s, hy, cz - hz * 0.55F * s), h1 = new Vec3d(cx + hx * 0.55F * s, hy, cz + hz * 0.55F * s);
      line(v0, v1, ColorUtil.withAlpha(gold, (int)(120 * env)), ColorUtil.withAlpha(gold, (int)(120 * env)), 10.0F, false);
      line(h0, h1, ColorUtil.withAlpha(gold, (int)(120 * env)), ColorUtil.withAlpha(gold, (int)(120 * env)), 10.0F, false);
      line(v0, v1, ColorUtil.withAlpha(light, (int)(255 * env)), ColorUtil.withAlpha(light, (int)(255 * env)), 3.5F, false);
      line(h0, h1, ColorUtil.withAlpha(light, (int)(255 * env)), ColorUtil.withAlpha(light, (int)(255 * env)), 3.5F, false);
      for (int i = 0; i < 9; i++) {
         glow(cx, cy - 0.95F * s + i * 0.2375F * s, cz, 0.2F * s, gold, 0.5F * env);
      }
      for (int i = 0; i < 5; i++) {
         double o = (i - 2) * 0.275F * s;
         glow(cx + hx * o, hy, cz + hz * o, 0.2F * s, gold, 0.5F * env);
      }
      glow(cx, hy, cz, 0.95F * s, gold, 0.55F * env);
      glow(cx, hy, cz, 0.4F * s, light, 0.85F * env);
      for (int i = 0; i < 18; i++) {
         float a = rnd(f.seed, i, 0) * TAU + t * 1.5F;
         float r = 0.5F + 0.6F * rnd(f.seed, i, 1);
         float tw = (float)(Math.sin(t * 8.0F + rnd(f.seed, i, 2) * 6.0F) * 0.5 + 0.5);
         glow(cx + Math.cos(a) * r, cy + (rnd(f.seed, i, 3) - 0.5F) * 1.6F, cz + Math.sin(a) * r, 0.05F + 0.04F * rnd(f.seed, i, 4), light, tw * env);
      }
      for (int i = 0; i < 14; i++) {
         float age = (t * 1.3F + rnd(f.seed, i, 5)) % 1.0F;
         glow(cx + (rnd(f.seed, i, 6) - 0.5F) * 0.5F, cy - 0.3F - age * 2.4F, cz + (rnd(f.seed, i, 7) - 0.5F) * 0.5F, 0.06F, gold, (1.0F - age) * 0.8F * env);
      }
      if (u < 0.1F) {
         float k = 1.0F - u / 0.1F;
         glow(f.x, f.y + f.h * 0.6, f.z, 1.6F * k, light, 0.9F * k);
      }
   }

   /** Крылья из «перьев» раскрываются за спиной и машут, над головой нимб. */
   private static void wings(Fx f, float u, float t) {
      float yaw = (float)Math.toRadians(f.yaw);
      double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = Math.cos(yaw), rz = Math.sin(yaw);
      float env = env(u, 0.1F, 0.4F);
      float open = easeOut(Math.min(1.0F, u / 0.25F));
      float lift = 0.6F * easeOut(u);
      float flap = (float)Math.sin(u * Math.PI * 3.2F) * 0.35F;
      double ax = f.x - fx * 0.18, ay = f.y + f.h * 0.72 + lift, az = f.z - fz * 0.18;
      int light = mix(f.color, ColorUtil.WHITE, 0.6F);
      for (int side = -1; side <= 1; side += 2) {
         Vec3d prevTip = null;
         for (int i = 0; i <= 6; i++) {
            float q = i / 6.0F;
            float theta = (0.35F + q * 1.05F) * open + flap * (0.5F + q * 0.5F);
            float len = (1.0F + 1.1F * (1.0F - q * 0.55F)) * open;
            double dx = side * rx * Math.cos(theta) - fx * 0.45;
            double dy = Math.sin(theta);
            double dz = side * rz * Math.cos(theta) - fz * 0.45;
            double inv = 1.0 / Math.sqrt(dx * dx + dy * dy + dz * dz);
            Vec3d tip = new Vec3d(ax + dx * inv * len, ay + dy * inv * len, az + dz * inv * len);
            line(new Vec3d(ax, ay, az), tip, ColorUtil.withAlpha(light, (int)(230 * env)), ColorUtil.withAlpha(f.color, (int)(120 * env)), 3.0F, false);
            glow(tip.x, tip.y, tip.z, 0.14F, light, 0.75F * env);
            glow((ax + tip.x) * 0.5, (ay + tip.y) * 0.5, (az + tip.z) * 0.5, 0.08F, f.color, 0.5F * env);
            if (prevTip != null) {
               line(prevTip, tip, ColorUtil.withAlpha(light, (int)(150 * env)), ColorUtil.withAlpha(light, (int)(150 * env)), 1.8F, false);
            }
            prevTip = tip;
         }
      }
      ring(f.x, f.y + f.h + 0.35 + lift, f.z, 0.3F, 18, light, (int)(235 * env), 2.4F, t * 2.0F);
      glow(f.x, ay, f.z, 0.55F * open, f.color, 0.5F * env);
      for (int i = 0; i < 16; i++) {
         float age = (t * 0.8F + rnd(f.seed, i, 0)) % 1.0F;
         float side = i % 2 == 0 ? 1.0F : -1.0F;
         double d = (0.8 + rnd(f.seed, i, 1) * 1.6) * open;
         glow(ax + rx * side * d, ay + (rnd(f.seed, i, 2) - 0.3F) * 1.0 - age * 0.8, az + rz * side * d, 0.05F + 0.03F * rnd(f.seed, i, 3), light, (float)Math.sin(Math.PI * age) * env);
      }
   }

   /** Три расходящихся кольца разных цветов + искры. */
   private static void rings(Fx f, float u, float t) {
      int green = mix(f.color, ColorUtil.rgba(120, 255, 140, 255), 0.65F);
      int light = mix(f.color, ColorUtil.WHITE, 0.55F);
      for (int i = 0; i < 3; i++) {
         float delay = i * 0.16F;
         float ur = clamp01((u - delay) / (1.0F - delay));
         if (ur <= 0.0F) continue;
         float rad = 0.4F + 2.4F * easeOut(ur);
         float ry = 0.1F + ur * (1.6F + i * 0.4F);
         float a = (float)Math.pow(1.0F - ur, 1.4F);
         int c = i == 1 ? green : f.color;
         ring(f.x, f.y + ry, f.z, rad, 36, c, (int)(235 * a), 3.0F, i * 0.7F + t);
         glow(f.x + Math.cos(t * 3 + i) * rad, f.y + ry, f.z + Math.sin(t * 3 + i) * rad, 0.16F, light, a * 0.8F);
      }
      for (int i = 0; i < 30; i++) {
         float age = (t * 0.9F + rnd(f.seed, i, 0)) % 1.0F;
         float a = rnd(f.seed, i, 1) * TAU;
         float r = 0.5F + 1.6F * age;
         glow(f.x + Math.cos(a) * r, f.y + 0.1 + age * 2.4F, f.z + Math.sin(a) * r, 0.06F + 0.04F * rnd(f.seed, i, 2), i % 2 == 0 ? green : light, (1.0F - age) * (1.0F - u));
      }
      if (u < 0.15F) {
         float k = 1.0F - u / 0.15F;
         glow(f.x, f.y + f.h * 0.5, f.z, 1.5F * k, light, 0.9F * k);
      }
   }

   /** Тройная золотая спираль поднимается и заканчивается вспышкой. */
   private static void spiral(Fx f, float u, float t) {
      float top = easeOut(Math.min(1.0F, u * 1.6F));
      float env = env(u, 0.08F, 0.35F);
      float shrink = 1.0F - Math.max(0.0F, (u - 0.7F) / 0.3F) * 0.9F;
      int green = mix(f.color, ColorUtil.rgba(120, 255, 140, 255), 0.6F);
      int light = mix(f.color, ColorUtil.WHITE, 0.5F);
      int n = 18;
      for (int k = 0; k < 3; k++) {
         for (int j = 0; j < n; j++) {
            float s = j / (float)(n - 1);
            if (s > top) break;
            float rad = 0.9F * (1.0F - s * 0.5F) * shrink;
            float ang = s * (float)Math.PI * 4.0F + t * 5.0F + k * TAU / 3.0F;
            glow(f.x + Math.cos(ang) * rad, f.y + 0.1 + s * 2.9F, f.z + Math.sin(ang) * rad, 0.15F - 0.07F * s, k == 1 ? green : f.color, 0.9F * env);
         }
      }
      if (u > 0.78F) {
         float k = (u - 0.78F) / 0.22F;
         glow(f.x, f.y + 3.0, f.z, 1.2F * (1.0F - k), light, 0.9F * (1.0F - k));
         ring(f.x, f.y + 3.0, f.z, 1.6F * easeOut(k), 28, light, (int)(220 * (1.0F - k)), 2.4F, 0.0F);
      }
   }

   // ───────────────────────── helpers ─────────────────────────

   private static void glow(double x, double y, double z, float size, int color, float alpha) {
      if (alpha <= 0.01F) return;
      double tx = OX + (x - OX) * SC, ty = OY + (y - OY) * SC, tz = OZ + (z - OZ) * SC;
      Glow3D.glow(tx, ty, tz, size * SC, color, alpha * 0.4F, 0.95F);
      Glow3D.core(tx, ty, tz, size * SC * 0.25F, color, Math.min(1.0F, alpha));
   }

   private static void line(Vec3d a, Vec3d b, int c1, int c2, float w, boolean depth) {
      Vec3d ta = new Vec3d(OX + (a.x - OX) * SC, OY + (a.y - OY) * SC, OZ + (a.z - OZ) * SC);
      Vec3d tb = new Vec3d(OX + (b.x - OX) * SC, OY + (b.y - OY) * SC, OZ + (b.z - OZ) * SC);
      Render3D.drawLineGradient(ta, tb, c1, c2, w * (float)Math.sqrt(SC), depth);
   }

   private static void ring(double cx, double cy, double cz, float rad, int segs, int color, int alpha, float width, float rot) {
      if (alpha <= 3) return;
      int c = ColorUtil.withAlpha(color, Math.min(255, alpha));
      double px = cx + Math.cos(rot) * rad, pz = cz + Math.sin(rot) * rad;
      for (int i = 1; i <= segs; i++) {
         float a = rot + i * TAU / segs;
         double nx = cx + Math.cos(a) * rad, nz = cz + Math.sin(a) * rad;
         line(new Vec3d(px, cy, pz), new Vec3d(nx, cy, nz), c, c, width, true);
         px = nx;
         pz = nz;
      }
   }

   private static int mix(int a, int b, float k) {
      return ColorUtil.interpolateColor(a, b, k) | 0xFF000000;
   }

   private static float rnd(int seed, int i, int k) {
      int h = seed * 374761393 + i * 668265263 + k * 1274126177;
      h = (h ^ (h >>> 13)) * 1274126177;
      h ^= h >>> 16;
      return (h & 0xFFFFFF) / (float)0x1000000;
   }

   private static float clamp01(float v) {
      return v < 0.0F ? 0.0F : Math.min(v, 1.0F);
   }

   private static float easeOut(float v) {
      float c = 1.0F - clamp01(v);
      return 1.0F - c * c * c;
   }

   private static float env(float u, float in, float out) {
      return Math.min(clamp01(u / in), clamp01((1.0F - u) / out));
   }

   private record Fx(Kind kind, double x, double y, double z, float h, float yaw, int color, long start, long dur, int seed, float sc) {
   }
}
