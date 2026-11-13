package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap.Type;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.AttackEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.particles.WireMesh;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class Particles extends Module {
   private static final Identifier GLOWBOOST = Identifier.of("daamky", "textures/particles/glow.png");
   private static final Identifier DASH_BLOOM = Identifier.of("daamky", "textures/world/dashbloom.png");
   private static final int[] CUBE_EDGES = new int[]{0, 1, 1, 2, 2, 3, 3, 0, 0, 4, 1, 5, 2, 6, 3, 7, 4, 5, 5, 6, 6, 7, 7, 4};
   private static Particles instance;
   private final MultiModeSetting effects = this.register(new MultiModeSetting("Effects", "", effectOptions(), "Meteor", "Cubes", "Stars"));
   private final NumberSetting count = this.register(new NumberSetting("Count", "", 14.0, 1.0, 80.0, 1.0));
   private final NumberSetting lifetime = this.register(new NumberSetting("Lifetime", "", 1600.0, 500.0, 5000.0, 100.0));
   private final NumberSetting size = this.register(new NumberSetting("Size", "", 0.45, 0.1, 1.5, 0.05));
   private final NumberSetting radius = this.register(new NumberSetting("Radius", "", 8.0, 0.5, 40.0, 0.5));
   private final NumberSetting tailLength = this.register(new NumberSetting("Tail Length", "", 0.3, 0.05, 0.7, 0.05));
   private final NumberSetting glow = this.register(new NumberSetting("Glow", "", 1.0, 0.0, 3.0, 0.1));
   private final BooleanSetting hit = this.register(new BooleanSetting("Hit Particles", "", true));
   private final MultiModeSetting hitTypes = this.register(
      new MultiModeSetting("Hit Types", "", new String[]{"Cubes", "Diamond", "Triangle"}, "Cubes", "Diamond")
   );
   private final ModeSetting hitPhysics = this.register(new ModeSetting("Hit Physics", "", "Both", "Drop", "Fly", "Both"));
   private final NumberSetting hitCount = this.register(new NumberSetting("Hit Count", "", 10.0, 1.0, 20.0, 1.0));
   private final NumberSetting hitLifetime = this.register(new NumberSetting("Hit Lifetime", "", 1000.0, 100.0, 3000.0, 50.0));
   private final NumberSetting hitSpeed = this.register(new NumberSetting("Hit Speed", "", 1.0, 0.1, 3.0, 0.1));
   private final NumberSetting hitScale = this.register(new NumberSetting("Hit Scale", "", 1.0, 0.5, 1.5, 0.1));
   private final BooleanSetting hitOnlyCrit = this.register(new BooleanSetting("Hit Only Crit", "", false));
   private final BooleanSetting hitBounce = this.register(new BooleanSetting("Hit Bounce", "", false));
   private final ModeSetting colorMode = this.register(new ModeSetting("Color", "", "Rainbow", "Rainbow", "Custom"));
   private final ColorSetting customColor = this.register(new ColorSetting("Custom Color", "", new Color(120, 200, 255, 255)));
   private final List<Particles.MeteorP> meteors = new ArrayList<>();
   private final Map<Particles.MeshEffect, List<Particles.OrbitP>> orbits = new EnumMap<>(Particles.MeshEffect.class);
   private final Map<Particles.MeshEffect, WireMesh> meshes = new EnumMap<>(Particles.MeshEffect.class);
   private final List<Particles.HitP> hits = new ArrayList<>();
   private final Set<RenderLayer> used = new HashSet<>();
   private final Random rng = new Random();
   private final Matrix3f rot = new Matrix3f();
   private final Vector3f tmpA = new Vector3f();
   private final Vector3f tmpB = new Vector3f();
   private final Vector3f[] cubeVerts = new Vector3f[8];
   private boolean meshesLoaded;

   public Particles() {
      super("Particles", "", ModuleCategory.VISUAL);
      this.hitTypes.visibleWhen(this.hit::getValue);
      this.hitPhysics.visibleWhen(this.hit::getValue);
      this.hitCount.visibleWhen(this.hit::getValue);
      this.hitLifetime.visibleWhen(this.hit::getValue);
      this.hitSpeed.visibleWhen(this.hit::getValue);
      this.hitScale.visibleWhen(this.hit::getValue);
      this.hitOnlyCrit.visibleWhen(this.hit::getValue);
      this.hitBounce.visibleWhen(() -> this.hit.getValue() && !this.hitPhysics.is("Fly"));
      this.tailLength.visibleWhen(() -> this.effects.isSelected("Meteor"));
      this.customColor.visibleWhen(() -> this.colorMode.is("Custom"));

      for (int i = 0; i < 8; i++) {
         this.cubeVerts[i] = new Vector3f();
      }

      instance = this;
   }

   public static Particles getInstance() {
      return instance;
   }

   private static String[] effectOptions() {
      String[] meshNames = Particles.MeshEffect.displayNames();
      String[] all = new String[meshNames.length + 1];
      all[0] = "Meteor";
      System.arraycopy(meshNames, 0, all, 1, meshNames.length);
      return all;
   }

   private WireMesh meshFor(Particles.MeshEffect effect) {
      return this.meshes.computeIfAbsent(effect, e -> {
         WireMesh mesh = e.loader.get();
         return mesh == null ? WireMesh.unitCube() : mesh;
      });
   }

   private List<Particles.OrbitP> orbitsFor(Particles.MeshEffect effect) {
      return this.orbits.computeIfAbsent(effect, e -> new ArrayList<>());
   }

   private boolean isSelected(Particles.MeshEffect effect) {
      return this.effects.isSelected(effect.displayName);
   }

   private void ensureMeshes() {
      if (!this.meshesLoaded) {
         this.meshesLoaded = true;

         for (Particles.MeshEffect effect : Particles.MeshEffect.VALUES) {
            if (this.isSelected(effect)) {
               this.meshFor(effect);
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.clearAll();
   }

   private void clearAll() {
      this.meteors.clear();
      this.orbits.clear();
      this.hits.clear();
   }

   @SubscribeEvent
   private void onAttack(AttackEvent e) {
      if (this.hit.getValue() && mc.player != null) {
         if (!this.hitOnlyCrit.getValue() || !(mc.player.fallDistance <= 0.0) && !mc.player.isOnGround()) {
            Entity target = e.getTarget();
            if (target instanceof LivingEntity && target != mc.player) {
               Vec3d hitPos = target.getBoundingBox().getCenter();
               List<Particles.HitKind> kinds = new ArrayList<>();
               if (this.hitTypes.isSelected("Cubes")) {
                  kinds.add(Particles.HitKind.CUBE);
               }

               if (this.hitTypes.isSelected("Diamond")) {
                  kinds.add(Particles.HitKind.DIAMOND);
               }

               if (this.hitTypes.isSelected("Triangle")) {
                  kinds.add(Particles.HitKind.TRIANGLE);
               }

               if (!kinds.isEmpty()) {
                  int n = this.hitCount.getValue().intValue();
                  long life = this.hitLifetime.getValue().longValue();
                  float speed = this.hitSpeed.getFloat() * 2.0F;
                  float scale = 0.1F + this.size.getFloat() * 0.2F * this.hitScale.getFloat();
                  long now = System.currentTimeMillis();

                  for (int i = 0; i < n; i++) {
                     Particles.HitKind kind = kinds.get(i % kinds.size());
                     boolean drop = this.hitPhysics.is("Drop") || this.hitPhysics.is("Both") && this.rng.nextBoolean();
                     Vec3d vel;
                     if (drop) {
                        vel = new Vec3d(this.rand(-1.0, 1.0), 0.2 + this.rng.nextDouble() * 0.8, this.rand(-1.0, 1.0))
                           .normalize()
                           .multiply((double)speed * 0.08);
                     } else {
                        vel = new Vec3d(this.rand(-1.0, 1.0), this.rand(-0.75, 0.75), this.rand(-1.0, 1.0)).normalize().multiply((double)speed * 0.08);
                     }

                     this.hits
                        .add(
                           new Particles.HitP(
                              hitPos,
                              vel,
                              kind,
                              this.color(i * 17),
                              scale,
                              now,
                              life,
                              drop && this.hitBounce.getValue(),
                              this.rng.nextFloat() * 2.0F - 1.0F,
                              this.rng.nextFloat() * 2.0F - 1.0F,
                              this.rng.nextFloat() * 2.0F - 1.0F
                           )
                        );
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent e) {
      if (e.isReceive() && e.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
         mc.execute(() -> {
            if (this.isEnabled() && mc.world != null && this.isSelected(Particles.MeshEffect.TOTEM)) {
               if (packet.getEntity(mc.world) instanceof LivingEntity living) {
                  long now = System.currentTimeMillis();
                  Vec3d c = living.getEntityPos().add(0.0, (double)living.getHeight() * 0.5, 0.0);
                  List list = this.orbitsFor(Particles.MeshEffect.TOTEM);

                  for (int i = 0; i < 12; i++) {
                     list.add(this.spawnOrbit(c, now, Particles.MeshEffect.TOTEM.rx, Particles.MeshEffect.TOTEM.ry, Particles.MeshEffect.TOTEM.rz));
                  }
               }
            }
         });
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent e) {
      if (this.isEnabled() && mc.player != null && mc.world != null) {
         this.ensureMeshes();
         long now = System.currentTimeMillis();
         this.maintainAmbient(now);
         this.updateHits(now);
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cam = Render3D.lastCameraPos;
         MatrixStack stack = e.getStack();
         Immediate buf = mc.getBufferBuilders().getEntityVertexConsumers();
         if (this.glow.getFloat() > 0.01F) {
            this.renderWireframeGlows(stack, buf, camera, cam, now);
         }

         if (this.effects.isSelected("Meteor") && !this.meteors.isEmpty()) {
            this.renderMeteors(stack, buf, camera, cam, now);
         }

         for (Particles.MeshEffect effect : Particles.MeshEffect.VALUES) {
            if (this.isSelected(effect)) {
               WireMesh mesh = this.meshFor(effect);
               if (!mesh.isEmpty()) {
                  this.renderMesh(this.orbitsFor(effect), mesh, now, this.size.getFloat() * effect.base, effect.growBase, effect.growAge);
               }
            }
         }

         if (this.hit.getValue() && !this.hits.isEmpty()) {
            this.renderHits(stack, buf, camera, cam, now);
         }
      }
   }

   private void renderWireframeGlows(MatrixStack stack, Immediate buf, Camera camera, Vec3d cam, long now) {
      if (!(this.glow.getFloat() <= 0.01F)) {
         float s = this.size.getFloat();
         float intensity = MathHelper.clamp(this.glow.getFloat(), 0.0F, 3.0F);
         RenderLayer bloomType = ClientPipelines.BLOOM_ESP.apply(DASH_BLOOM);
         VertexConsumer bloom = buf.getBuffer(bloomType);

         for (Particles.MeshEffect effect : Particles.MeshEffect.VALUES) {
            if (this.isSelected(effect)) {
               this.glowOrbitList(stack, bloom, camera, cam, this.orbitsFor(effect), now, s * effect.base, intensity);
            }
         }

         buf.draw(bloomType);
      }
   }

   private void glowOrbitList(
      MatrixStack stack, VertexConsumer vc, Camera camera, Vec3d cam, List<Particles.OrbitP> list, long now, float baseScale, float intensity
   ) {
      for (Particles.OrbitP p : list) {
         float age = (float)(now - p.born) / (float)p.life;
         if (!(age >= 1.0F)) {
            float fade = ageFade(age);
            if (!(fade <= 0.01F)) {
               Vec3d pos = this.aboveGround(p.posAt(age), 0.25);
               float particleSize = baseScale * (0.55F + age * 0.15F);
               float bloomSize = 4.0F * particleSize;
               int alphaI = clampByte(Math.round(255.0F * fade * 0.4F * Math.min(1.0F, intensity)));
               if (alphaI != 0) {
                  int bloomCol = ColorUtil.withAlpha(p.color, alphaI);
                  drawCamBillboard(stack, vc, camera, cam, pos, bloomSize, bloomCol);
               }
            }
         }
      }
   }

   private static void drawCamBillboard(MatrixStack stack, VertexConsumer vc, Camera camera, Vec3d cam, Vec3d world, float size, int color) {
      if (ColorUtil.getAlpha(color) > 0 && !(size <= 0.001F)) {
         stack.push();
         stack.translate((float)(world.x - cam.x), (float)(world.y - cam.y), (float)(world.z - cam.z));
         stack.multiply(camera.getRotation());
         Entry pose = stack.peek();
         float h = size * 0.5F;
         vc.vertex(pose, -h, -h, 0.0F).texture(0.0F, 0.0F).color(color);
         vc.vertex(pose, h, -h, 0.0F).texture(1.0F, 0.0F).color(color);
         vc.vertex(pose, h, h, 0.0F).texture(1.0F, 1.0F).color(color);
         vc.vertex(pose, -h, h, 0.0F).texture(0.0F, 1.0F).color(color);
         stack.pop();
      }
   }

   private static int clampByte(int v) {
      return Math.max(0, Math.min(255, v));
   }

   private void maintainAmbient(long now) {
      Vec3d player = mc.player.getEntityPos();
      double r = Math.max(1.5, (double)this.radius.getFloat());
      int n = Math.max(1, this.count.getValue().intValue());
      long lifeBase = this.lifetime.getValue().longValue();
      if (this.effects.isSelected("Meteor")) {
         this.cullMeteors(now);

         while (this.meteors.size() < n) {
            this.meteors.add(this.spawnMeteor(player, r, now, lifeBase));
         }
      } else {
         this.meteors.clear();
      }

      for (Particles.MeshEffect effect : Particles.MeshEffect.VALUES) {
         this.maintainOrbit(this.orbitsFor(effect), this.isSelected(effect), player, r, n, now, effect.rx, effect.ry, effect.rz);
      }
   }

   private void maintainOrbit(List<Particles.OrbitP> list, boolean on, Vec3d center, double r, int n, long now, float rx, float ry, float rz) {
      if (!on) {
         list.clear();
      } else {
         list.removeIf(p -> now - p.born > p.life);

         while (list.size() > n) {
            list.remove(0);
         }

         while (list.size() < n) {
            list.add(this.spawnOrbit(center, now, rx, ry, rz));
         }
      }
   }

   private void cullMeteors(long now) {
      this.meteors.removeIf(m -> now - m.born > m.life);

      while (this.meteors.size() > this.count.getValue().intValue()) {
         this.meteors.remove(0);
      }
   }

   private Particles.MeteorP spawnMeteor(Vec3d center, double rMax, long now, long lifeBase) {
      double a = this.rng.nextDouble() * Math.PI * 2.0;
      double p = Math.acos(2.0 * this.rng.nextDouble() - 1.0);
      double rr = 1.5 + this.rng.nextDouble() * Math.max(0.5, rMax - 1.5);
      double sp = Math.sin(p);
      Vec3d start = this.aboveGround(new Vec3d(center.x + sp * Math.cos(a) * rr, center.y + 1.0 + Math.cos(p) * rr, center.z + sp * Math.sin(a) * rr), 0.35);
      double va = this.rng.nextDouble() * Math.PI * 2.0;
      double vp = Math.acos(2.0 * this.rng.nextDouble() - 1.0);
      double speed = 4.0 + this.rng.nextDouble() * 7.0;
      double sv = Math.sin(vp);
      Vec3d vel = new Vec3d(sv * Math.cos(va) * speed, Math.cos(vp) * speed * 0.55 - 1.2, sv * Math.sin(va) * speed);
      long life = Math.max(80L, Math.round((double)lifeBase * (0.75 + (double)this.rng.nextFloat() * 0.5)));
      return new Particles.MeteorP(start, vel, now, life, this.color(this.rng.nextInt(180)));
   }

   private Particles.OrbitP spawnOrbit(Vec3d center, long now, float rx, float ry, float rz) {
      double a = this.rng.nextDouble() * Math.PI * 2.0;
      double p = Math.acos(2.0 * this.rng.nextDouble() - 1.0);
      double rMin = 1.5;
      double rMax = Math.max(1.6, (double)this.radius.getFloat());
      double rr = rMin + this.rng.nextDouble() * (rMax - rMin);
      double sp = Math.sin(p);
      Vec3d start = this.aboveGround(new Vec3d(center.x + sp * Math.cos(a) * rr, center.y + 1.0 + Math.cos(p) * rr, center.z + sp * Math.sin(a) * rr), 0.35);
      double va = this.rng.nextDouble() * Math.PI * 2.0;
      double vp = Math.acos(2.0 * this.rng.nextDouble() - 1.0);
      double speed = 1.6 + this.rng.nextDouble() * 1.8;
      double sv = Math.sin(vp);
      double vy = Math.abs(Math.cos(vp) * speed) * 0.85 + 0.4;
      Vec3d vel = new Vec3d(sv * Math.cos(va) * speed, vy, sv * Math.sin(va) * speed);
      long life = Math.max(50L, Math.round((double)this.lifetime.getFloat() * (0.75 + (double)this.rng.nextFloat() * 0.5)));
      return new Particles.OrbitP(
         start,
         vel,
         now,
         life,
         this.color(this.rng.nextInt(180)),
         (this.rng.nextFloat() - 0.5F) * rx,
         (this.rng.nextFloat() - 0.5F) * ry,
         (this.rng.nextFloat() - 0.5F) * rz
      );
   }

   private double groundY(double x, double z) {
      if (mc.world != null && mc.player != null) {
         try {
            BlockPos top = mc.world.getTopPosition(Type.MOTION_BLOCKING, BlockPos.ofFloored(x, 0.0, z));
            return (double)top.getY();
         } catch (Throwable var6) {
            return mc.player.getY();
         }
      } else {
         return 0.0;
      }
   }

   private Vec3d aboveGround(Vec3d p, double pad) {
      double minY = this.groundY(p.x, p.z) + pad;
      return p.y < minY ? new Vec3d(p.x, minY, p.z) : p;
   }

   private void renderMeteors(MatrixStack stack, Immediate buf, Camera camera, Vec3d cam, long now) {
      float baseSize = this.size.getFloat();
      float step = Math.max(0.025F, baseSize * 0.24F);
      float tailFactor = Math.max(0.05F, this.tailLength.getFloat());
      RenderLayer type = ClientPipelines.BLOOM_ESP.apply(GLOWBOOST);
      VertexConsumer vc = buf.getBuffer(type);

      for (Particles.MeteorP m : this.meteors) {
         float age = (float)(now - m.born) / (float)m.life;
         if (!(age >= 1.0F)) {
            Vec3d pos = m.start.add(m.vel.multiply((double)age));
            m.pushHistory(pos, now);
            float fade = ageFade(age);
            long trailMs = Math.max(1L, (long)((float)m.life * tailFactor));
            int last = m.histCount - 1;

            while (last > 0 && now - m.histTime[last] > trailMs) {
               last--;
            }

            for (int i = last; i > 0; i--) {
               double x0 = m.histX[i];
               double y0 = m.histY[i];
               double z0 = m.histZ[i];
               double x1 = m.histX[i - 1];
               double y1 = m.histY[i - 1];
               double z1 = m.histZ[i - 1];
               double dx = x1 - x0;
               double dy = y1 - y0;
               double dz = z1 - z0;
               double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
               int steps = Math.min(20, Math.max(1, (int)Math.ceil(dist / (double)step)));

               for (int s = 0; s < steps; s++) {
                  float t = (float)s / (float)steps;
                  long ts = m.histTime[i] + (long)((double)(m.histTime[i - 1] - m.histTime[i]) * (double)t);
                  float trailAge = MathHelper.clamp((float)(now - ts) / (float)trailMs, 0.0F, 1.0F);
                  float ta = (1.0F - trailAge) * (1.0F - trailAge);
                  if (!(ta <= 0.0F)) {
                     float sz = baseSize * (0.34F + (float)Math.sqrt((double)ta) * 0.86F);
                     int col = ColorUtil.withAlpha(m.color, clampByte(Math.round(255.0F * fade * ta * 0.5F)));
                     drawMeteorQuad(stack, vc, camera, cam, new Vec3d(x0 + dx * (double)t, y0 + dy * (double)t, z0 + dz * (double)t), sz, col, false);
                  }
               }
            }

            if (m.histCount > 0) {
               Vec3d head = new Vec3d(m.histX[0], m.histY[0], m.histZ[0]);
               int col = ColorUtil.withAlpha(m.color, clampByte(Math.round(255.0F * fade)));
               drawMeteorQuad(stack, vc, camera, cam, head, baseSize, col, true);
            }
         }
      }

      buf.draw(type);
   }

   private static void drawMeteorQuad(MatrixStack stack, VertexConsumer vc, Camera camera, Vec3d cam, Vec3d world, float size, int color, boolean head) {
      int outerColor = head ? ColorUtil.multAlpha(color, 0.45F) : color;
      drawCamBillboard(stack, vc, camera, cam, world, size * 1.9F, outerColor);
      if (head) {
         drawCamBillboard(stack, vc, camera, cam, world, size, color);
      }
   }

   private void renderMesh(List<Particles.OrbitP> list, WireMesh mesh, long now, float baseScale, float growBase, float growAge) {
      float[] edges = mesh.edges();
      if (edges.length >= 6) {
         for (Particles.OrbitP p : list) {
            float age = (float)(now - p.born) / (float)p.life;
            if (!(age >= 1.0F)) {
               Vec3d pos = this.aboveGround(p.posAt(age), 0.25);
               float fade = ageFade(age);
               float s = baseScale * (growBase + age * growAge);
               this.rot.identity().rotateY(p.ry * age).rotateX(p.rx * age).rotateZ(p.rz * age);
               int col = ColorUtil.multAlpha(p.color, fade);
               float g = Math.max(0.5F, this.glow.getFloat());
               float width = 1.2F + g * 0.4F;

               for (int i = 0; i + 5 < edges.length; i += 6) {
                  this.tmpA.set(edges[i] * s, edges[i + 1] * s, edges[i + 2] * s);
                  this.tmpB.set(edges[i + 3] * s, edges[i + 4] * s, edges[i + 5] * s);
                  this.rot.transform(this.tmpA);
                  this.rot.transform(this.tmpB);
                  Render3D.drawLine(
                     new Vec3d(pos.x + (double)this.tmpA.x, pos.y + (double)this.tmpA.y, pos.z + (double)this.tmpA.z),
                     new Vec3d(pos.x + (double)this.tmpB.x, pos.y + (double)this.tmpB.y, pos.z + (double)this.tmpB.z),
                     col,
                     width,
                     true
                  );
               }
            }
         }
      }
   }

   private void line(Vector3f a, Vector3f b, Vec3d origin, int color) {
      Render3D.drawLine(
         new Vec3d(origin.x + (double)a.x, origin.y + (double)a.y, origin.z + (double)a.z),
         new Vec3d(origin.x + (double)b.x, origin.y + (double)b.y, origin.z + (double)b.z),
         color,
         1.6F,
         true
      );
   }

   private void updateHits(long now) {
      Iterator<Particles.HitP> it = this.hits.iterator();

      while (it.hasNext()) {
         Particles.HitP h = it.next();
         if (now - h.born >= h.life) {
            it.remove();
         } else {
            h.prev = h.pos;
            if (h.bounce) {
               if (this.solid(h.pos.x, h.pos.y, h.pos.z + h.vel.z)) {
                  h.vel = new Vec3d(h.vel.x, h.vel.y, -h.vel.z * 0.7);
               }

               if (this.solid(h.pos.x, h.pos.y + h.vel.y, h.pos.z)) {
                  h.vel = new Vec3d(h.vel.x * 0.95, -h.vel.y * 0.55, h.vel.z * 0.95);
               }

               if (this.solid(h.pos.x + h.vel.x, h.pos.y, h.pos.z)) {
                  h.vel = new Vec3d(-h.vel.x * 0.7, h.vel.y, h.vel.z);
               }

               h.vel = h.vel.multiply(0.98).add(0.0, -0.003, 0.0);
            } else {
               h.vel = h.vel.multiply(0.985).add(0.0, -0.0015, 0.0);
            }

            h.pos = h.pos.add(h.vel);
            double minY = this.groundY(h.pos.x, h.pos.z) + 0.2;
            if (h.pos.y < minY) {
               h.pos = new Vec3d(h.pos.x, minY, h.pos.z);
               if (h.vel.y < 0.0) {
                  h.vel = new Vec3d(h.vel.x * 0.9, h.bounce ? -h.vel.y * 0.45 : 0.0, h.vel.z * 0.9);
               }
            }

            h.spin += 0.12F;
         }
      }
   }

   private void renderHits(MatrixStack stack, Immediate buf, Camera camera, Vec3d cam, long now) {
      float g = this.glow.getFloat();
      if (g > 0.01F) {
         RenderLayer bloomType = ClientPipelines.BLOOM_ESP.apply(DASH_BLOOM);
         VertexConsumer bloom = buf.getBuffer(bloomType);

         for (Particles.HitP h : this.hits) {
            float age = (float)(now - h.born) / (float)h.life;
            float fade = ageFade(age);
            if (!(fade <= 0.01F)) {
               float s = h.scale * (0.8F + age * 0.2F);
               int col = ColorUtil.withAlpha(h.color, clampByte(Math.round(255.0F * fade * 0.4F * Math.min(1.0F, g))));
               drawCamBillboard(stack, bloom, camera, cam, h.pos, 4.0F * s, col);
            }
         }

         buf.draw(bloomType);
      }

      for (Particles.HitP hx : this.hits) {
         float age = (float)(now - hx.born) / (float)hx.life;
         float fade = ageFade(age);
         int col = ColorUtil.multAlpha(hx.color, fade);
         float s = hx.scale * (0.8F + age * 0.4F);
         this.rot.identity().rotateX(hx.rx * hx.spin).rotateY(hx.ry * hx.spin).rotateZ(hx.rz * hx.spin);
         switch (hx.kind) {
            case CUBE:
               float half = s * 0.5F;

               for (int j = 0; j < 8; j++) {
                  this.cubeVerts[j].set((j & 1) == 0 ? -half : half, (j & 2) == 0 ? -half : half, (j & 4) == 0 ? -half : half);
                  this.rot.transform(this.cubeVerts[j]);
               }

               for (int e = 0; e < CUBE_EDGES.length; e += 2) {
                  this.line(this.cubeVerts[CUBE_EDGES[e]], this.cubeVerts[CUBE_EDGES[e + 1]], hx.pos, col);
               }
               break;
            case DIAMOND:
               Vector3f top = new Vector3f(0.0F, s * 0.56F, 0.0F);
               this.rot.transform(top);
               Vector3f bot = new Vector3f(0.0F, -s * 0.56F, 0.0F);
               this.rot.transform(bot);
               Vector3f px = new Vector3f(s * 0.4F, 0.0F, 0.0F);
               this.rot.transform(px);
               Vector3f nx = new Vector3f(-s * 0.4F, 0.0F, 0.0F);
               this.rot.transform(nx);
               Vector3f pz = new Vector3f(0.0F, 0.0F, s * 0.4F);
               this.rot.transform(pz);
               Vector3f nz = new Vector3f(0.0F, 0.0F, -s * 0.4F);
               this.rot.transform(nz);
               this.line(top, px, hx.pos, col);
               this.line(top, nx, hx.pos, col);
               this.line(top, pz, hx.pos, col);
               this.line(top, nz, hx.pos, col);
               this.line(bot, px, hx.pos, col);
               this.line(bot, nx, hx.pos, col);
               this.line(bot, pz, hx.pos, col);
               this.line(bot, nz, hx.pos, col);
               this.line(px, nz, hx.pos, col);
               this.line(nz, nx, hx.pos, col);
               this.line(nx, pz, hx.pos, col);
               this.line(pz, px, hx.pos, col);
               break;
            case TRIANGLE:
               Vector3f a = new Vector3f(0.0F, s * 0.62F, 0.0F);
               this.rot.transform(a);
               Vector3f b = new Vector3f(s * 0.56F, -s * 0.24F, 0.0F);
               this.rot.transform(b);
               Vector3f c = new Vector3f(-s * 0.28F, -s * 0.24F, s * 0.4816F);
               this.rot.transform(c);
               Vector3f d = new Vector3f(-s * 0.28F, -s * 0.24F, -s * 0.4816F);
               this.rot.transform(d);
               this.line(a, b, hx.pos, col);
               this.line(a, c, hx.pos, col);
               this.line(a, d, hx.pos, col);
               this.line(b, c, hx.pos, col);
               this.line(c, d, hx.pos, col);
               this.line(d, b, hx.pos, col);
         }
      }
   }

   private boolean solid(double x, double y, double z) {
      if (mc.world == null) {
         return false;
      } else {
         BlockPos p = BlockPos.ofFloored(x, y, z);
         return !mc.world.getBlockState(p).getCollisionShape(mc.world, p).isEmpty();
      }
   }

   private static float ageFade(float age) {
      if (age < 0.15F) {
         return age / 0.15F;
      } else {
         return age > 0.75F ? Math.max(0.0F, (1.0F - age) / 0.25F) : 1.0F;
      }
   }

   private int color(int offset) {
      if (this.colorMode.is("Custom")) {
         return this.customColor.getValue().getRGB() | 0xFF000000;
      } else {
         float hue = (float)((System.currentTimeMillis() + (long)offset * 20L) % 3000L) / 3000.0F;
         return Color.HSBtoRGB(hue, 0.75F, 1.0F) | 0xFF000000;
      }
   }

   private double rand(double a, double b) {
      return a + (b - a) * this.rng.nextDouble();
   }

   private static enum HitKind {
      CUBE,
      DIAMOND,
      TRIANGLE;

      private HitKind() {
      }
   }

   private static final class HitP {
      Vec3d pos;
      Vec3d prev;
      Vec3d vel;
      final Particles.HitKind kind;
      final int color;
      final float scale;
      final long born;
      final long life;
      final boolean bounce;
      final float rx;
      final float ry;
      final float rz;
      float spin;

      HitP(Vec3d pos, Vec3d vel, Particles.HitKind kind, int color, float scale, long born, long life, boolean bounce, float rx, float ry, float rz) {
         this.pos = pos;
         this.prev = pos;
         this.vel = vel;
         this.kind = kind;
         this.color = color;
         this.scale = scale;
         this.born = born;
         this.life = life;
         this.bounce = bounce;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
      }
   }

   private static enum MeshEffect {
      CUBES("Cubes", 0.6F, 0.6F, 0.6F, 6.0F, 6.0F, 6.0F, WireMesh::unitCube),
      PYRAMIDS("Pyramids", 0.75F, 0.65F, 0.55F, 7.0F, 7.0F, 7.0F, WireMesh::unitPyramid),
      TOTEM("Totem", 0.78F, 0.75F, 0.35F, 5.0F, 8.0F, 5.0F, () -> WireMesh.loadClasspath("/assets/daamky/models/particles/totem_undying.obj", false, -0.75F)),
      CRESCENT("Crescent", 0.72F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, WireMesh::proceduralCrescent),
      HEART("Heart", 0.7F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, Particles.MeshEffect::loadHeart),
      STARS("Stars", 0.9F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, () -> WireMesh.loadClasspath("/assets/daamky/models/particles/fire_nether_star.obj", true, 0.0F)),
      ROCK("Rock", 0.85F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, () -> WireMesh.loadClasspath("/assets/daamky/models/particles/meteor.obj", true, 0.0F)),
      OCTAHEDRON("Octahedron", 0.7F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, WireMesh::proceduralOctahedron),
      TETRAHEDRON("Tetrahedron", 0.72F, 0.65F, 0.55F, 7.0F, 7.0F, 7.0F, WireMesh::proceduralTetrahedron),
      ICOSAHEDRON("Icosahedron", 0.72F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, WireMesh::proceduralIcosahedron),
      TORUS("Torus", 0.8F, 0.7F, 0.45F, 6.0F, 6.0F, 6.0F, () -> WireMesh.proceduralTorus(16, 6)),
      HELIX("Helix", 0.8F, 0.7F, 0.45F, 3.0F, 8.0F, 3.0F, WireMesh::proceduralHelix),
      GYRO("Gyro", 0.8F, 0.7F, 0.45F, 8.0F, 8.0F, 8.0F, WireMesh::proceduralGyro),
      GEM("Gem", 0.75F, 0.7F, 0.45F, 4.0F, 9.0F, 4.0F, WireMesh::proceduralGem);

      private static final Particles.MeshEffect[] VALUES = values();
      private final String displayName;
      private final float base;
      private final float growBase;
      private final float growAge;
      private final float rx;
      private final float ry;
      private final float rz;
      private final Supplier<WireMesh> loader;

      private MeshEffect(String displayName, float base, float growBase, float growAge, float rx, float ry, float rz, Supplier<WireMesh> loader) {
         this.displayName = displayName;
         this.base = base;
         this.growBase = growBase;
         this.growAge = growAge;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
         this.loader = loader;
      }

      private static WireMesh loadHeart() {
         WireMesh mesh = WireMesh.loadClasspath("/assets/daamky/models/particles/heart.gltf", true, 0.0F);
         return mesh != null && !mesh.isEmpty() ? mesh : WireMesh.proceduralHeart();
      }

      static String[] displayNames() {
         String[] names = new String[VALUES.length];

         for (int i = 0; i < VALUES.length; i++) {
            names[i] = VALUES[i].displayName;
         }

         return names;
      }
   }

   private static final class MeteorP {
      final Vec3d start;
      final Vec3d vel;
      final long born;
      final long life;
      final int color;
      static final int MAX = 48;
      final double[] histX = new double[48];
      final double[] histY = new double[48];
      final double[] histZ = new double[48];
      final long[] histTime = new long[48];
      int histCount;

      MeteorP(Vec3d start, Vec3d vel, long born, long life, int color) {
         this.start = start;
         this.vel = vel;
         this.born = born;
         this.life = life;
         this.color = color;
      }

      void pushHistory(Vec3d p, long t) {
         for (int i = Math.min(this.histCount, 47); i > 0; i--) {
            this.histX[i] = this.histX[i - 1];
            this.histY[i] = this.histY[i - 1];
            this.histZ[i] = this.histZ[i - 1];
            this.histTime[i] = this.histTime[i - 1];
         }

         this.histX[0] = p.x;
         this.histY[0] = p.y;
         this.histZ[0] = p.z;
         this.histTime[0] = t;
         if (this.histCount < 48) {
            this.histCount++;
         }
      }
   }

   private static final class OrbitP {
      final Vec3d start;
      final Vec3d vel;
      final long born;
      final long life;
      final int color;
      final float rx;
      final float ry;
      final float rz;

      OrbitP(Vec3d start, Vec3d vel, long born, long life, int color, float rx, float ry, float rz) {
         this.start = start;
         this.vel = vel;
         this.born = born;
         this.life = life;
         this.color = color;
         this.rx = rx;
         this.ry = ry;
         this.rz = rz;
      }

      Vec3d posAt(float age) {
         return this.start.add(this.vel.multiply((double)age));
      }
   }
}
