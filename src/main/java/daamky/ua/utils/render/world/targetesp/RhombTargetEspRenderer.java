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

public final class RhombTargetEspRenderer {
   private static final Identifier RHOMB_TEXTURE = Identifier.fromNamespaceAndPath("daamky", "textures/world/cube.png");

   private RhombTargetEspRenderer() {
   }

   public static void render(PoseStack stack, BufferSource provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(RHOMB_TEXTURE));
      Quaternionf camRot = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
      stack.pushPose();
      stack.translate(0.0F, context.target().getBbHeight() / 2.0F, 0.0F);
      stack.mulPose(camRot);
      float timeRotation = (float)(context.frameTimeMs() % 6283L) / 1000.0F;
      stack.mulPose(Axis.ZP.rotationDegrees(Mth.sin((double)timeRotation) * 360.0F));
      stack.scale(0.35F, 0.35F, 1.0F);
      int c1 = ColorUtil.withAlpha(context.primaryColor(), (int)(255.0F * context.alpha()));
      int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int)(255.0F * context.alpha()));
      Pose entry = stack.last();
      consumer.addVertex(entry, -1.0F, -1.0F, 0.0F).setUv(0.0F, 0.0F).setColor(c2);
      consumer.addVertex(entry, -1.0F, 1.0F, 0.0F).setUv(0.0F, 1.0F).setColor(c1);
      consumer.addVertex(entry, 1.0F, 1.0F, 0.0F).setUv(1.0F, 1.0F).setColor(c2);
      consumer.addVertex(entry, 1.0F, -1.0F, 0.0F).setUv(1.0F, 0.0F).setColor(c1);
      stack.popPose();
   }

   public static void endBatch(BufferSource provider) {
      provider.endBatch(ClientPipelines.ROMB_ESP.apply(RHOMB_TEXTURE));
   }
}
