package daamky.ua.utils.render.world.targetesp;

import org.joml.Quaternionf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public final class MarkerTargetEspRenderer {
   private static final Identifier MARKER_TEXTURE = Identifier.fromNamespaceAndPath("daamky", "textures/features/targetesp/marker.png");

   private MarkerTargetEspRenderer() {
   }

   public static void render(PoseStack stack, BufferSource provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(MARKER_TEXTURE));
      Quaternionf camRot = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
      stack.pushPose();
      stack.translate(0.0F, context.target().getBbHeight() / 2.0F, 0.0F);
      stack.mulPose(camRot);
      float rotation = (float)(context.frameTimeMs() % 3600L) / 3600.0F * 360.0F;
      float pulse = 1.0F + Mth.sin((double)((float)(context.frameTimeMs() % 1400L) / 1400.0F * (float) (Math.PI * 2))) * 0.08F;
      stack.mulPose(Axis.ZP.rotationDegrees(rotation));
      stack.scale(0.65F * pulse, 0.65F * pulse, 1.0F);
      int c1 = ColorUtil.withAlpha(context.primaryColor(), (int)(255.0F * context.alpha()));
      int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int)(255.0F * context.alpha()));
      int c3 = ColorUtil.withAlpha(context.primaryColor(), (int)(215.0F * context.alpha()));
      int c4 = ColorUtil.withAlpha(context.secondaryColor(), (int)(215.0F * context.alpha()));
      Pose entry = stack.last();
      consumer.addVertex(entry, -1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F).setColor(c1);
      consumer.addVertex(entry, -1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F).setColor(c2);
      consumer.addVertex(entry, 1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F).setColor(c3);
      consumer.addVertex(entry, 1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F).setColor(c4);
      stack.popPose();
   }

   public static void endBatch(BufferSource provider) {
      provider.endBatch(ClientPipelines.ROMB_ESP.apply(MARKER_TEXTURE));
   }
}
