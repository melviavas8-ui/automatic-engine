package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class MarkerTargetEspRenderer {
   private static final Identifier MARKER_TEXTURE = Identifier.of("daamky", "textures/features/targetesp/marker.png");

   private MarkerTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(MARKER_TEXTURE));
      Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
      stack.push();
      stack.translate(0.0F, context.target().getHeight() / 2.0F, 0.0F);
      stack.multiply(camRot);
      float rotation = (float)(context.frameTimeMs() % 3600L) / 3600.0F * 360.0F;
      float pulse = 1.0F + MathHelper.sin((double)((float)(context.frameTimeMs() % 1400L) / 1400.0F * (float) (Math.PI * 2))) * 0.08F;
      stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
      stack.scale(0.65F * pulse, 0.65F * pulse, 1.0F);
      int c1 = ColorUtil.withAlpha(context.primaryColor(), (int)(255.0F * context.alpha()));
      int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int)(255.0F * context.alpha()));
      int c3 = ColorUtil.withAlpha(context.primaryColor(), (int)(215.0F * context.alpha()));
      int c4 = ColorUtil.withAlpha(context.secondaryColor(), (int)(215.0F * context.alpha()));
      Entry entry = stack.peek();
      consumer.vertex(entry, -1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(c1);
      consumer.vertex(entry, -1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(c2);
      consumer.vertex(entry, 1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(c3);
      consumer.vertex(entry, 1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(c4);
      stack.pop();
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.ROMB_ESP.apply(MARKER_TEXTURE));
   }
}
