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
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;
import daamky.ua.utils.repository.friend.FriendUtils;

public class DashTrail extends Module {
   private static final String ROOT = "textures/features/dashtrail/";
   private static final Identifier BLOOM_TEXTURE = texture("dashbloomsample.png");
   private static final Identifier DOT_TEXTURE = texture("dashbloom.png");
   private static final float BASE_PIXEL_SCALE = 0.0033F;
   private static final double MIN_DASH_SPEED = 0.08;
   private static final int MAX_CUBICS = 420;
   private static final int CLIENT_COLOR_FIRST = ColorUtil.rgba(247, 133, 255, 255);
   private static final int CLIENT_COLOR_SECOND = ColorUtil.rgba(247, 133, 255, 255);
   private static final DashTrail.TextureInfo[] STATIC_TEXTURES = new DashTrail.TextureInfo[]{
      cubic(1, 48, 48),
      cubic(2, 48, 48),
      cubic(3, 48, 48),
      cubic(4, 96, 48),
      cubic(5, 96, 48),
      cubic(6, 96, 48),
      cubic(7, 96, 48),
      cubic(8, 96, 48),
      cubic(9, 96, 48),
      cubic(10, 96, 48),
      cubic(11, 96, 48),
      cubic(12, 96, 48),
      cubic(13, 96, 96),
      cubic(14, 96, 96),
      cubic(15, 96, 96),
      cubic(16, 96, 96),
      cubic(17, 44, 48),
      cubic(18, 44, 48),
      cubic(19, 44, 48),
      cubic(20, 44, 48),
      cubic(21, 44, 48)
   };
   private static final DashTrail.TextureInfo[][] ANIMATED_TEXTURES = new DashTrail.TextureInfo[][]{
      group(1, 11, 144, 48), group(2, 23, 48, 48), group(3, 32, 96, 48), group(4, 16, 48, 48), group(5, 32, 48, 48)
   };
   private final Random random = new Random(1234567891L);
   private final List<DashTrail.DashCubic> dashCubics = new ArrayList<>();
   private final Map<Integer, Vec3d> lastPositions = new HashMap<>();
   private final Set<RenderLayer> usedRenderTypes = new HashSet<>();
   private final Set<Integer> seenPlayerIds = new HashSet<>();
   private final BooleanSetting self = this.register(new BooleanSetting("Self", "Draw dash trail for you.", true));
   private final BooleanSetting players = this.register(new BooleanSetting("Players", "Draw dash trail for other players.", false));
   private final BooleanSetting friends = this.register(new BooleanSetting("Friends", "Draw dash trail for friends.", true));
   private final ModeSetting colorMode = this.register(
      new ModeSetting("Color Mode", "Dash trail color mode.", "Random Palette", "Random Palette", "Custom", "Client", "Rainbow")
   );
   private final ColorSetting pickColor = this.register(new ColorSetting("Pick Color", "Custom dash trail color.", new Color(170, 40, 255, 255)));
   private final NumberSetting maxDistance = this.register(
      new NumberSetting("Max Distance", "Maximum distance to render other players.", 25.0, 15.0, 100.0, 1.0)
   );
   private final BooleanSetting motionSmoothing = this.register(new BooleanSetting("Motion Smoothing", "Smooths dash pieces through the chain.", false));
   private final BooleanSetting dashSegments = this.register(new BooleanSetting("Dash Segments", "Adds small glowing line segments.", false));
   private final BooleanSetting dashDots = this.register(new BooleanSetting("Dash Dots", "Adds spark dots around dash pieces.", true));
   private final BooleanSetting lighting = this.register(new BooleanSetting("Lighting", "Adds bloom lighting around dash pieces.", true));
   private final NumberSetting dashLength = this.register(new NumberSetting("Dash Length", "Dash piece lifetime multiplier.", 0.75, 0.5, 1.5, 0.05));

   public DashTrail() {
      super("Dash Trail", "Draws Vega-style dash trail particles behind moving players.", ModuleCategory.VISUAL);
      this.colorMode.visibleWhen(this::hasAnyTargetEnabled);
      this.pickColor.visibleWhen(() -> this.colorMode.is("Custom") && this.hasAnyTargetEnabled());
      this.maxDistance.visibleWhen(() -> this.players.getValue() || this.friends.getValue());
      this.motionSmoothing.visibleWhen(this::hasAnyTargetEnabled);
      this.dashSegments.visibleWhen(this::hasAnyTargetEnabled);
      this.dashDots.visibleWhen(this::hasAnyTargetEnabled);
      this.lighting.visibleWhen(this::hasAnyTargetEnabled);
      this.dashLength.visibleWhen(this::hasAnyTargetEnabled);
   }

   @Override
   protected void onDisable() {
      this.dashCubics.clear();
      this.lastPositions.clear();
      this.usedRenderTypes.clear();
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client != null && client.world != null && client.player != null) {
         if (this.hasAnyTargetEnabled()) {
            this.seenPlayerIds.clear();

            for (PlayerEntity player : client.world.getPlayers()) {
               if (this.isTarget(player, client)) {
                  this.seenPlayerIds.add(player.getId());
                  this.trackPlayer(player);
               }
            }

            this.lastPositions.keySet().removeIf(id -> !this.seenPlayerIds.contains(id));
         } else {
            this.lastPositions.clear();
         }

         this.tickCubics();
      } else {
         this.dashCubics.clear();
         this.lastPositions.clear();
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      long now = System.currentTimeMillis();
      this.dashCubics.removeIf(cubic -> cubic.isDead(now));
      if (!this.dashCubics.isEmpty() && mc.gameRenderer != null) {
         MatrixStack stack = event.getStack();
         Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
         Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
         Quaternionf cameraRotation = mc.gameRenderer.getCamera().getRotation();
         float tickDelta = event.getPartialTicks();
         boolean renderDots = this.dashDots.getValue();
         boolean renderSegments = this.dashSegments.getValue();
         this.usedRenderTypes.clear();

         for (DashTrail.DashCubic cubic : this.dashCubics) {
            float progress = cubic.progress(now);
            float alpha = dashAlpha(progress);
            if (!(alpha <= 0.02F)) {
               Vec3d pos = cubic.renderPos(tickDelta);
               DashTrail.TextureInfo texture = cubic.texture.get(now);
               float sizeAlpha = easeOutQuad(alpha);
               float width = (float)texture.width() * 0.0033F * cubic.scale * sizeAlpha;
               float height = (float)texture.height() * 0.0033F * cubic.scale * sizeAlpha;
               int mainColor = ColorUtil.multAlpha(ColorUtil.lerpColor(cubic.color, ColorUtil.WHITE, 0.34F), alpha * 0.92F);
               this.renderBillboard(
                  stack, provider, ClientPipelines.GHOSTS_ESP.apply(texture.id()), pos, cameraPos, cameraRotation, width, height, cubic.rotation, mainColor
               );
               this.renderBloom(stack, provider, pos, cameraPos, cameraRotation, width, height, cubic.color, alpha, progress);
               if (renderDots || renderSegments) {
                  this.renderSparks(stack, provider, cubic, pos, cameraPos, cameraRotation, tickDelta, alpha, renderDots, renderSegments);
               }
            }
         }

         for (RenderLayer type : this.usedRenderTypes) {
            provider.draw(type);
         }

         this.usedRenderTypes.clear();
      }
   }

   private void trackPlayer(PlayerEntity player) {
      Vec3d current = player.getEntityPos();
      Vec3d previous = this.lastPositions.put(player.getId(), current);
      if (previous != null) {
         Vec3d delta = current.subtract(previous);
         double speedXZ = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
         if (!(speedXZ < 0.08) && !(delta.lengthSquared() > 36.0)) {
            int count = MathHelper.clamp((int)(delta.length() / 0.08), 1, 16);
            boolean addSparks = this.dashSegments.getValue() || this.dashDots.getValue();

            for (int i = 0; i < count; i++) {
               float offset = count == 1 ? 0.0F : (float)i / (float)count;
               this.spawnCubic(player, current, delta, offset, addSparks);
            }

            this.trimCubics();
         }
      }
   }

   private void spawnCubic(PlayerEntity player, Vec3d current, Vec3d delta, float offset, boolean addSparks) {
      double height = (double)player.getHeight();
      double x = current.x - delta.x * (double)offset + this.randomRange(0.175) - 0.0875;
      double y = current.y - delta.y * (double)offset + height / 3.0 + height / 4.0 * this.random.nextDouble() * 0.7;
      double z = current.z - delta.z * (double)offset + this.randomRange(0.175) - 0.0875;
      Vec3d position = new Vec3d(x, y, z);
      Vec3d motion = delta.multiply(0.04);
      DashTrail.DashTexture texture = DashTrail.DashTexture.random(this.random, (long)this.getRandomTimeAnimation());
      int lifetime = this.getRandomTimeAnimation();
      int color = this.nextDashColor();
      float scale = 0.86F + this.random.nextFloat() * 0.34F;
      this.dashCubics.add(new DashTrail.DashCubic(player.getId(), position, motion, texture, lifetime, color, scale, addSparks, this.random));
   }

   private void tickCubics() {
      long now = System.currentTimeMillis();
      boolean addSegments = this.dashSegments.getValue();
      boolean addDots = this.dashDots.getValue();

      for (int i = 0; i < this.dashCubics.size(); i++) {
         DashTrail.DashCubic current = this.dashCubics.get(i);
         DashTrail.DashCubic next = this.motionSmoothing.getValue()
               && i + 1 < this.dashCubics.size()
               && this.dashCubics.get(i + 1).entityId == current.entityId
            ? this.dashCubics.get(i + 1)
            : null;
         current.tick(next, this.random, addSegments, addDots, now);
      }

      this.dashCubics.removeIf(cubic -> cubic.isDead(now));
      this.trimCubics();
   }

   private void renderBloom(
      MatrixStack stack,
      Immediate provider,
      Vec3d pos,
      Vec3d cameraPos,
      Quaternionf cameraRotation,
      float width,
      float height,
      int color,
      float alpha,
      float progress
   ) {
      float diagonal = (float)Math.sqrt((double)(width * width + height * height));
      int softColor = ColorUtil.multAlpha(ColorUtil.lerpColor(color, ColorUtil.WHITE, 0.18F), alpha * 0.13F);
      this.renderBillboard(
         stack, provider, ClientPipelines.BLOOM_ESP.apply(BLOOM_TEXTURE), pos, cameraPos, cameraRotation, diagonal * 0.58F, diagonal * 0.58F, 0.0F, softColor
      );
      if (this.lighting.getValue()) {
         float fadeOut = 1.0F - progress;
         float glowScale = 1.05F + 1.85F * fadeOut * alpha;
         int glowColor = ColorUtil.multAlpha(ColorUtil.lerpColor(color, ColorUtil.WHITE, 0.1F), alpha * 0.18F);
         this.renderBillboard(
            stack,
            provider,
            ClientPipelines.BLOOM_ESP.apply(BLOOM_TEXTURE),
            pos,
            cameraPos,
            cameraRotation,
            diagonal * glowScale,
            diagonal * glowScale,
            0.0F,
            glowColor
         );
      }
   }

   private void renderSparks(
      MatrixStack stack,
      Immediate provider,
      DashTrail.DashCubic cubic,
      Vec3d center,
      Vec3d cameraPos,
      Quaternionf cameraRotation,
      float tickDelta,
      float cubicAlpha,
      boolean renderDots,
      boolean renderSegments
   ) {
      for (DashTrail.DashSpark spark : cubic.sparks) {
         float sparkAlpha = easeInOutWave((float)spark.alpha(nowMs()) * cubicAlpha);
         if (!(sparkAlpha <= 0.01F)) {
            Vec3d offset = spark.renderPos(tickDelta);
            Vec3d first = center.add(offset);
            Vec3d second = center.subtract(offset);
            int sparkColor = ColorUtil.multAlpha(ColorUtil.lerpColor(cubic.color, ColorUtil.WHITE, 0.45F), sparkAlpha * 0.72F);
            if (renderDots) {
               float size = 0.052F + 0.07F * sparkAlpha;
               this.renderBillboard(
                  stack, provider, ClientPipelines.WORLD_PARTICLES_GLOW.apply(DOT_TEXTURE), first, cameraPos, cameraRotation, size, size, 0.0F, sparkColor
               );
               this.renderBillboard(
                  stack, provider, ClientPipelines.WORLD_PARTICLES_GLOW.apply(DOT_TEXTURE), second, cameraPos, cameraRotation, size, size, 0.0F, sparkColor
               );
            }

            if (renderSegments) {
               int endColor = ColorUtil.withAlpha(sparkColor, 0);
               Render3D.drawLineGradient(first, second, sparkColor, endColor, 2.0F, false);
            }
         }
      }
   }

   private void renderBillboard(
      MatrixStack stack,
      Immediate provider,
      RenderLayer renderType,
      Vec3d pos,
      Vec3d cameraPos,
      Quaternionf cameraRotation,
      float width,
      float height,
      float rotation,
      int color
   ) {
      if (!(width <= 0.001F) && !(height <= 0.001F) && ColorUtil.getAlpha(color) > 0) {
         this.usedRenderTypes.add(renderType);
         VertexConsumer consumer = provider.getBuffer(renderType);
         stack.push();
         stack.translate(pos.x - cameraPos.x, pos.y - cameraPos.y, pos.z - cameraPos.z);
         stack.multiply(cameraRotation);
         stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
         Entry pose = stack.peek();
         float halfWidth = width / 2.0F;
         float halfHeight = height / 2.0F;
         WorldVertex.textured(consumer, pose, -halfWidth, -halfHeight, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, halfWidth, -halfHeight, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(consumer, pose, halfWidth, halfHeight, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(consumer, pose, -halfWidth, halfHeight, 0.0F, 0.0F, 1.0F, color);
         stack.pop();
      }
   }

   private boolean isTarget(PlayerEntity player, MinecraftClient client) {
      if (player == null || !player.isAlive()) {
         return false;
      } else if (player == client.player) {
         return this.self.getValue();
      } else {
         double maxDistanceValue = this.maxDistance.getValue();
         if (client.player.squaredDistanceTo(player) > maxDistanceValue * maxDistanceValue) {
            return false;
         } else {
            boolean friend = FriendUtils.isFriend(player);
            return friend ? this.friends.getValue() : this.players.getValue();
         }
      }
   }

   private boolean hasAnyTargetEnabled() {
      return this.self.getValue() || this.players.getValue() || this.friends.getValue();
   }

   private int getRandomTimeAnimation() {
      return Math.round((float)(550 + this.random.nextInt(300)) * this.dashLength.getFloat());
   }

   private int nextDashColor() {
      if (this.colorMode.is("Random Palette")) {
         return Color.getHSBColor(this.random.nextFloat(), 0.95F, 1.0F).getRGB();
      } else if (this.colorMode.is("Custom")) {
         return this.pickColor.getValue().getRGB();
      } else if (this.colorMode.is("Client")) {
         float wave = (MathHelper.sin((double)((float)System.currentTimeMillis() / 520.0F)) + 1.0F) / 2.0F;
         return ColorUtil.lerpColor(CLIENT_COLOR_FIRST, CLIENT_COLOR_SECOND, wave);
      } else {
         float hue = (float)(System.currentTimeMillis() % 1600L) / 1600.0F;
         return Color.getHSBColor(hue, 0.82F, 1.0F).getRGB();
      }
   }

   private void trimCubics() {
      int overflow = this.dashCubics.size() - 420;
      if (overflow > 0) {
         this.dashCubics.subList(0, overflow).clear();
      }
   }

   private double randomRange(double range) {
      return this.random.nextDouble() * range;
   }

   private static float dashAlpha(float progress) {
      float clamped = MathHelper.clamp(progress, 0.0F, 1.0F);
      if (clamped < 0.08F) {
         return clamped / 0.08F;
      } else {
         return clamped > 0.72F ? 1.0F - (clamped - 0.72F) / 0.28F : 1.0F;
      }
   }

   private static float easeOutQuad(float value) {
      float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
      return 1.0F - (1.0F - clamped) * (1.0F - clamped);
   }

   private static float easeInOutWave(float value) {
      float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
      return clamped <= 0.5F ? 2.0F * clamped * clamped : 1.0F - (float)Math.pow((double)(-2.0F * clamped + 2.0F), 2.0) / 2.0F;
   }

   private static long nowMs() {
      return System.currentTimeMillis();
   }

   private static Identifier texture(String path) {
      return Identifier.of("daamky", "textures/features/dashtrail/" + path);
   }

   private static DashTrail.TextureInfo cubic(int number, int width, int height) {
      return new DashTrail.TextureInfo(texture("dashcubics/dashcubic" + number + ".png"), width, height);
   }

   private static DashTrail.TextureInfo[] group(int group, int count, int width, int height) {
      DashTrail.TextureInfo[] textures = new DashTrail.TextureInfo[count];

      for (int i = 0; i < count; i++) {
         textures[i] = new DashTrail.TextureInfo(texture("dashcubics/group_dashs/group" + group + "/dashcubic" + (i + 1) + ".png"), width, height);
      }

      return textures;
   }

   private static final class DashCubic {
      private final int entityId;
      private final DashTrail.DashTexture texture;
      private final long startTime = System.currentTimeMillis();
      private final int lifetime;
      private final int color;
      private final float scale;
      private final boolean addSparks;
      private final List<DashTrail.DashSpark> sparks = new ArrayList<>();
      private Vec3d pos;
      private Vec3d prevPos;
      private Vec3d motion;
      private float rotation;

      private DashCubic(
         int entityId, Vec3d pos, Vec3d motion, DashTrail.DashTexture texture, int lifetime, int color, float scale, boolean addSparks, Random random
      ) {
         this.entityId = entityId;
         this.pos = pos;
         this.prevPos = pos;
         this.motion = motion;
         this.texture = texture;
         this.lifetime = Math.max(1, lifetime);
         this.color = color;
         this.scale = scale;
         this.addSparks = addSparks;
         this.updateRotation(random);
      }

      private void tick(DashTrail.DashCubic next, Random random, boolean addSegments, boolean addDots, long now) {
         this.prevPos = this.pos;
         Vec3d sourceMotion = next != null ? next.motion : this.motion;
         this.motion = sourceMotion.multiply(0.9523809523809523);
         double yDivider = this.motion.y < 0.0 ? 1.0 : 3.5;
         this.pos = this.pos.add(this.motion.x * 5.0, this.motion.y * 5.0 / yDivider, this.motion.z * 5.0);
         this.updateRotation(random);
         if (this.addSparks && (addSegments || addDots) && this.progress(now) < 0.3F && random.nextInt(12) > 5) {
            int amount = addSegments ? 1 : 2;

            for (int i = 0; i < amount; i++) {
               this.sparks.add(new DashTrail.DashSpark(random));
            }
         }

         if (this.addSparks && (addSegments || addDots)) {
            for (DashTrail.DashSpark spark : this.sparks) {
               spark.tick();
            }

            this.sparks.removeIf(DashTrail.DashSpark::isDead);
         } else {
            this.sparks.clear();
         }
      }

      private Vec3d renderPos(float tickDelta) {
         return this.prevPos.lerp(this.pos, (double)MathHelper.clamp(tickDelta, 0.0F, 1.0F));
      }

      private float progress(long now) {
         return MathHelper.clamp((float)(now - this.startTime) / (float)this.lifetime, 0.0F, 1.0F);
      }

      private boolean isDead(long now) {
         if (this.progress(now) < 1.0F) {
            return false;
         } else {
            for (DashTrail.DashSpark spark : this.sparks) {
               if (!spark.isDead()) {
                  return false;
               }
            }

            return true;
         }
      }

      private void updateRotation(Random random) {
         double horizontal = Math.sqrt(this.motion.x * this.motion.x + this.motion.z * this.motion.z);
         if (horizontal < 5.0E-4) {
            this.rotation = random.nextFloat() * 360.0F;
         } else {
            float motionYaw = (float)Math.toDegrees(Math.atan2(this.motion.z, this.motion.x) - (Math.PI / 2));
            if (motionYaw < 0.0F) {
               motionYaw += 360.0F;
            }

            this.rotation = motionYaw - 60.0F;
         }
      }
   }

   private static final class DashSpark {
      private final double speed;
      private final double yaw;
      private final double pitch;
      private final long startTime = System.currentTimeMillis();
      private Vec3d pos = Vec3d.ZERO;
      private Vec3d prevPos = Vec3d.ZERO;

      private DashSpark(Random random) {
         this.speed = random.nextDouble() / 50.0;
         this.yaw = Math.toRadians(random.nextDouble() * 360.0);
         this.pitch = Math.toRadians(-180.0 + random.nextDouble() * 180.0);
      }

      private void tick() {
         this.prevPos = this.pos;
         this.pos = this.pos.add(Math.sin(this.yaw) * this.speed, Math.cos(this.pitch) * this.speed, Math.cos(this.yaw) * this.speed);
      }

      private Vec3d renderPos(float tickDelta) {
         return this.prevPos.lerp(this.pos, (double)MathHelper.clamp(tickDelta, 0.0F, 1.0F));
      }

      private double alpha(long now) {
         return 1.0 - MathHelper.clamp((double)(now - this.startTime) / 1000.0, 0.0, 1.0);
      }

      private boolean isDead() {
         return this.alpha(System.currentTimeMillis()) <= 0.0;
      }
   }

   private static final class DashTexture {
      private final DashTrail.TextureInfo[] textures;
      private final long startTime;
      private final long animationTime;

      private DashTexture(DashTrail.TextureInfo[] textures, long startTime, long animationTime) {
         this.textures = textures;
         this.startTime = startTime;
         this.animationTime = Math.max(1L, animationTime);
      }

      private static DashTrail.DashTexture random(Random random, long animationTime) {
         return random.nextInt(100) > 40
            ? new DashTrail.DashTexture(
               DashTrail.ANIMATED_TEXTURES[random.nextInt(DashTrail.ANIMATED_TEXTURES.length)], System.currentTimeMillis(), animationTime
            )
            : new DashTrail.DashTexture(
               new DashTrail.TextureInfo[]{DashTrail.STATIC_TEXTURES[random.nextInt(DashTrail.STATIC_TEXTURES.length)]},
               System.currentTimeMillis(),
               animationTime
            );
      }

      private DashTrail.TextureInfo get(long now) {
         if (this.textures.length <= 1) {
            return this.textures[0];
         } else {
            long elapsed = Math.floorMod(now - this.startTime, this.animationTime);
            int index = MathHelper.clamp((int)((float)elapsed / (float)this.animationTime * (float)this.textures.length), 0, this.textures.length - 1);
            return this.textures[index];
         }
      }
   }

   private static record TextureInfo(Identifier id, int width, int height) {
   }
}
