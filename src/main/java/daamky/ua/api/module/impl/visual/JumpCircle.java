package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.JumpEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.world.jumpcircle.GroundGlassRenderer;

public final class JumpCircle extends Module {
   private static final int MAX_CIRCLES = 8;
   private final List<JumpCircle.Circle> circles = new ArrayList<>();
   private final NumberSetting size = this.register(new NumberSetting("Size", "Maximum jump circle size.", 0.65, 0.15, 2.0, 0.05));
   private final NumberSetting duration = this.register(new NumberSetting("Duration", "Jump circle lifetime in milliseconds.", 900.0, 200.0, 2500.0, 25.0));
   private final ColorSetting color1 = this.register(new ColorSetting("Color", "Primary jump circle color.", new Color(247, 133, 255)));
   private long lastSpawnTime;
   private Vec3d lastSpawnPos;

   public JumpCircle() {
      super("Jump Circle", "Draws a fading circle when you jump.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onDisable() {
      this.circles.clear();
      this.lastSpawnTime = 0L;
      this.lastSpawnPos = null;
   }

   @SubscribeEvent
   private void onJump(JumpEvent event) {
      if (mc.player != null && event.getPlayer() == mc.player) {
         Vec3d playerPos = mc.player.getEntityPos();
         if (this.isFiniteAndSafe(playerPos)) {
            Vec3d spawnPos = new Vec3d(playerPos.x, Math.floor(playerPos.y) + 0.01, playerPos.z);
            long now = System.currentTimeMillis();
            if (this.lastSpawnPos == null || now - this.lastSpawnTime >= 120L || !(this.lastSpawnPos.squaredDistanceTo(spawnPos) <= 0.1225)) {
               if (this.circles.size() >= 8) {
                  this.circles.remove(0);
               }

               this.circles.add(new JumpCircle.Circle(spawnPos, new StopWatch()));
               this.lastSpawnTime = now;
               this.lastSpawnPos = spawnPos;
            }
         }
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (mc.gameRenderer != null && mc.getBufferBuilders() != null) {
         this.purgeExpired();
         if (!this.circles.isEmpty()) {
            if (!this.isFiniteAndSafe(mc.gameRenderer.getCamera().getCameraPos())) {
               this.circles.clear();
            } else {
               for (JumpCircle.Circle circle : this.circles) {
                  this.renderRefraction(circle);
               }
            }
         }
      }
   }

   private void renderRefraction(JumpCircle.Circle circle) {
      float durationMs = Math.max(1.0F, this.duration.getFloat());
      float progress = MathHelper.clamp((float)circle.timer().elapsedTime() / durationMs, 0.0F, 1.0F);
      float radius = this.size.getFloat() * 1.35F * this.easeOutQuad(progress);
      float alpha = this.alphaFor(progress);
      if (!(progress >= 1.0F) && !(radius <= 0.01F) && !(alpha <= 0.01F)) {
         GroundGlassRenderer.render(circle.pos(), radius, this.color1.getValue().getRGB(), alpha, progress);
      }
   }

   private void purgeExpired() {
      long maxTime = Math.max(1L, Math.round(this.duration.getValue()));
      Iterator<JumpCircle.Circle> iterator = this.circles.iterator();

      while (iterator.hasNext()) {
         JumpCircle.Circle circle = iterator.next();
         if (circle.timer().elapsedTime() > maxTime || !this.isFiniteAndSafe(circle.pos())) {
            iterator.remove();
         }
      }
   }

   private float alphaFor(float progress) {
      if (progress < 0.14F) {
         return progress / 0.14F;
      } else if (progress > 0.62F) {
         float fade = (progress - 0.62F) / 0.38F;
         return 1.0F - fade * fade * (3.0F - 2.0F * fade);
      } else {
         return 1.0F;
      }
   }

   private float easeOutQuad(float value) {
      return 1.0F - (1.0F - value) * (1.0F - value);
   }

   private boolean isFiniteAndSafe(Vec3d value) {
      return value != null && this.isFiniteAndSafe(value.x) && this.isFiniteAndSafe(value.y) && this.isFiniteAndSafe(value.z);
   }

   private boolean isFiniteAndSafe(double value) {
      return Double.isFinite(value) && Math.abs(value) <= 3.0E7;
   }

   private static record Circle(Vec3d pos, StopWatch timer) {
   }
}
