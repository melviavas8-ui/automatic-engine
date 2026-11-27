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

public final class SkullTargetEspRenderer {
   private static final Identifier SKULL_TEXTURE = Identifier.fromNamespaceAndPath("daamky", "textures/features/targetesp/skull.png");

   private SkullTargetEspRenderer() {
   }

   public static void render(PoseStack stack, BufferSource provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(SKULL_TEXTURE));
      Quaternionf camRot = Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
      float impact = TargetEspMath.easeOutCubic(context.chainImpactProgress());
      float scale = 0.48F * (1.0F - 0.14F * impact);
      float verticalOffset = context.target().getBbHeight() * 0.5F;
      float alpha = Math.min(1.0F, context.alpha() * 1.85F);
      int c1 = ColorUtil.withAlpha(context.primaryColor(), (int)(255.0F * alpha));
      int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int)(255.0F * alpha));
      int c3 = ColorUtil.withAlpha(context.primaryColor(), (int)(250.0F * alpha));
      int c4 = ColorUtil.withAlpha(context.secondaryColor(), (int)(250.0F * alpha));
      stack.pushPose();
      stack.translate(0.0F, verticalOffset, 0.0F);
      stack.mulPose(camRot);
      stack.mulPose(Axis.ZP.rotationDegrees(180.0F));
      stack.scale(scale, scale, 1.0F);
      Pose entry = stack.last();
      consumer.addVertex(entry, -0.8F, -0.8F, 0.0F).setUv(0.0F, 0.0F).setColor(c1);
      consumer.addVertex(entry, -0.8F, 0.8F, 0.0F).setUv(0.0F, 1.0F).setColor(c2);
      consumer.addVertex(entry, 0.8F, 0.8F, 0.0F).setUv(1.0F, 1.0F).setColor(c3);
      consumer.addVertex(entry, 0.8F, -0.8F, 0.0F).setUv(1.0F, 0.0F).setColor(c4);
      stack.popPose();
   }

   public static void endBatch(BufferSource provider) {
      provider.endBatch(ClientPipelines.ROMB_ESP.apply(SKULL_TEXTURE));
   }
}
