package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class TotemAngel extends Module {
   private static final int TOTEM_EVENT_ID = 35;
   private static final float UNIT = 0.0625F;
   private final NumberSetting riseHeight = this.register(new NumberSetting("Rise Height", "How high the ghost floats.", 4.0, 0.2, 5.0, 0.1));
   private final NumberSetting lifetime = this.register(new NumberSetting("Lifetime", "Ghost lifetime in seconds.", 3.0, 0.2, 6.0, 0.1));
   private final ColorSetting color = this.register(new ColorSetting("Color", "Ghost color.", new Color(127, 242, 255, 190)));
   private final List<TotemAngel.Ghost> ghosts = new CopyOnWriteArrayList<>();

   public TotemAngel() {
      super("TotemAngel", "Angel ghost on totem pop.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onDisable() {
      this.ghosts.clear();
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && mc.world != null) {
         if (event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            if (packet.getEntity(mc.world) instanceof PlayerEntity player) {
               float bodyYaw = MathHelper.lerpAngleDegrees(1.0F, player.lastBodyYaw, player.bodyYaw);
               this.ghosts.add(new TotemAngel.Ghost(player.getEntityPos(), bodyYaw, System.currentTimeMillis()));
            }

            return;
         }
      }
   }

   @SubscribeEvent
   private void onRender(WorldRenderEvent event) {
      if (!this.ghosts.isEmpty() && mc.player != null && mc.world != null) {
         Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
         MatrixStack stack = event.getStack();
         Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
         VertexConsumer vc = provider.getBuffer(ClientPipelines.CRYSTAL_FILLED);
         long now = System.currentTimeMillis();
         int baseColor = this.color.getValue().getRGB();
         float life = this.lifetime.getFloat() * 1000.0F;

         for (TotemAngel.Ghost ghost : this.ghosts) {
            float progress = (float)(now - ghost.startTime) / life;
            if (progress >= 1.0F) {
               this.ghosts.remove(ghost);
            } else {
               this.renderGhost(stack, vc, ghost, cam, baseColor, progress);
            }
         }

         provider.draw(ClientPipelines.CRYSTAL_FILLED);
      }
   }

   private void renderGhost(MatrixStack stack, VertexConsumer vc, TotemAngel.Ghost ghost, Vec3d cam, int baseColor, float progress) {
      double rise = this.riseHeight.getValue() * this.easeOutCubic((double)(Math.min(progress, 0.85F) / 0.85F));
      float alpha = this.ghostAlpha(progress);
      int color = ColorUtil.withAlpha(baseColor, (int)(alpha * 255.0F));
      double rx = ghost.position.x - cam.x;
      double ry = ghost.position.y - cam.y + rise;
      double rz = ghost.position.z - cam.z;
      stack.push();
      stack.translate(rx, ry, rz);
      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - ghost.bodyYaw));
      stack.scale(-1.0F, -1.0F, 1.0F);
      stack.translate(0.0, -1.501, 0.0);
      float split = this.splitProgress(progress);
      float headRise = -6.0F * split;
      float armLift = -2.0F * split;
      float armSpread = 4.5F * split;
      float legDrop = 3.0F * split;
      float legSpread = 2.0F * split;
      this.part(stack, vc, color, 0.0F, 0.0F, 0.0F, 0.0F, headRise, 0.0F, -4, -8, -4, 4, 0, 4);
      this.part(stack, vc, color, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -4, 0, -2, 4, 12, 2);
      this.part(stack, vc, color, -5.0F, 2.0F, 0.0F, -armSpread, armLift, 0.0F, -3, -2, -2, 1, 10, 2);
      this.part(stack, vc, color, 5.0F, 2.0F, 0.0F, armSpread, armLift, 0.0F, -1, -2, -2, 3, 10, 2);
      this.part(stack, vc, color, -1.9F, 12.0F, 0.0F, -legSpread, legDrop, 0.0F, -2, 0, -2, 2, 12, 2);
      this.part(stack, vc, color, 1.9F, 12.0F, 0.0F, legSpread, legDrop, 0.0F, -2, 0, -2, 2, 12, 2);
      stack.pop();
   }

   private void part(
      MatrixStack stack,
      VertexConsumer vc,
      int color,
      float pivotX,
      float pivotY,
      float pivotZ,
      float offX,
      float offY,
      float offZ,
      int minX,
      int minY,
      int minZ,
      int maxX,
      int maxY,
      int maxZ
   ) {
      stack.push();
      stack.translate((pivotX + offX) * 0.0625F, (pivotY + offY) * 0.0625F, (pivotZ + offZ) * 0.0625F);
      this.box(
         stack.peek(),
         vc,
         color,
         (float)minX * 0.0625F,
         (float)minY * 0.0625F,
         (float)minZ * 0.0625F,
         (float)maxX * 0.0625F,
         (float)maxY * 0.0625F,
         (float)maxZ * 0.0625F
      );
      stack.pop();
   }

   private void box(Entry pose, VertexConsumer vc, int c, float x1, float y1, float z1, float x2, float y2, float z2) {
      vc.vertex(pose, x1, y1, z2).color(c);
      vc.vertex(pose, x2, y1, z2).color(c);
      vc.vertex(pose, x2, y2, z2).color(c);
      vc.vertex(pose, x1, y2, z2).color(c);
      vc.vertex(pose, x2, y1, z1).color(c);
      vc.vertex(pose, x1, y1, z1).color(c);
      vc.vertex(pose, x1, y2, z1).color(c);
      vc.vertex(pose, x2, y2, z1).color(c);
      vc.vertex(pose, x1, y2, z1).color(c);
      vc.vertex(pose, x1, y2, z2).color(c);
      vc.vertex(pose, x2, y2, z2).color(c);
      vc.vertex(pose, x2, y2, z1).color(c);
      vc.vertex(pose, x1, y1, z2).color(c);
      vc.vertex(pose, x1, y1, z1).color(c);
      vc.vertex(pose, x2, y1, z1).color(c);
      vc.vertex(pose, x2, y1, z2).color(c);
      vc.vertex(pose, x2, y1, z2).color(c);
      vc.vertex(pose, x2, y1, z1).color(c);
      vc.vertex(pose, x2, y2, z1).color(c);
      vc.vertex(pose, x2, y2, z2).color(c);
      vc.vertex(pose, x1, y1, z1).color(c);
      vc.vertex(pose, x1, y1, z2).color(c);
      vc.vertex(pose, x1, y2, z2).color(c);
      vc.vertex(pose, x1, y2, z1).color(c);
   }

   private double easeOutCubic(double v) {
      double c = (double)MathHelper.clamp((float)v, 0.0F, 1.0F);
      return 1.0 - Math.pow(1.0 - c, 3.0);
   }

   private float ghostAlpha(float progress) {
      float c = MathHelper.clamp(progress, 0.0F, 1.0F);
      float fadeIn = this.easeOut(MathHelper.clamp(c / 0.15F, 0.0F, 1.0F));
      float fadeOut = 1.0F - this.easeOut(MathHelper.clamp((c - 0.32F) / 0.68F, 0.0F, 1.0F));
      return 0.7F * fadeIn * fadeOut;
   }

   private float splitProgress(float progress) {
      return this.easeOut(MathHelper.clamp((progress - 0.38F) / 0.62F, 0.0F, 1.0F));
   }

   private float easeOut(float v) {
      return 1.0F - (float)Math.pow((double)(1.0F - v), 3.0);
   }

   private static record Ghost(Vec3d position, float bodyYaw, long startTime) {
   }
}
