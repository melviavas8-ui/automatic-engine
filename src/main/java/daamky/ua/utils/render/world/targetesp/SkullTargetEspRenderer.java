package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class SkullTargetEspRenderer {
   private static final Identifier SKULL_TEXTURE = Identifier.of("daamky", "textures/features/targetesp/skull.png");

   private SkullTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(SKULL_TEXTURE));
      Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
      float impact = TargetEspMath.easeOutCubic(context.chainImpactProgress());
      float scale = 0.48F * (1.0F - 0.14F * impact);
      float verticalOffset = context.target().getHeight() * 0.5F;
      float alpha = Math.min(1.0F, context.alpha() * 1.85F);
      int c1 = ColorUtil.withAlpha(context.primaryColor(), (int)(255.0F * alpha));
      int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int)(255.0F * alpha));
      int c3 = ColorUtil.withAlpha(context.primaryColor(), (int)(250.0F * alpha));
      int c4 = ColorUtil.withAlpha(context.secondaryColor(), (int)(250.0F * alpha));
      stack.push();
      stack.translate(0.0F, verticalOffset, 0.0F);
      stack.multiply(camRot);
      stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
      stack.scale(scale, scale, 1.0F);
      Entry entry = stack.peek();
      consumer.vertex(entry, -0.8F, -0.8F, 0.0F).texture(0.0F, 0.0F).color(c1);
      consumer.vertex(entry, -0.8F, 0.8F, 0.0F).texture(0.0F, 1.0F).color(c2);
      consumer.vertex(entry, 0.8F, 0.8F, 0.0F).texture(1.0F, 1.0F).color(c3);
      consumer.vertex(entry, 0.8F, -0.8F, 0.0F).texture(1.0F, 0.0F).color(c4);
      stack.pop();
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.ROMB_ESP.apply(SKULL_TEXTURE));
   }
}
