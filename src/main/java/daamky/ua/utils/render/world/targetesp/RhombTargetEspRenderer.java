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

public final class RhombTargetEspRenderer {
   private static final Identifier RHOMB_TEXTURE = Identifier.of("daamky", "textures/world/cube.png");

   private RhombTargetEspRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context) {
      VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(RHOMB_TEXTURE));
      Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
      stack.push();
      stack.translate(0.0F, context.target().getHeight() / 2.0F, 0.0F);
      stack.multiply(camRot);
      float timeRotation = (float)(context.frameTimeMs() % 6283L) / 1000.0F;
      stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin((double)timeRotation) * 360.0F));
      stack.scale(0.35F, 0.35F, 1.0F);
      int c1 = ColorUtil.withAlpha(context.primaryColor(), (int)(255.0F * context.alpha()));
      int c2 = ColorUtil.withAlpha(context.secondaryColor(), (int)(255.0F * context.alpha()));
      Entry entry = stack.peek();
      consumer.vertex(entry, -1.0F, -1.0F, 0.0F).texture(0.0F, 0.0F).color(c2);
      consumer.vertex(entry, -1.0F, 1.0F, 0.0F).texture(0.0F, 1.0F).color(c1);
      consumer.vertex(entry, 1.0F, 1.0F, 0.0F).texture(1.0F, 1.0F).color(c2);
      consumer.vertex(entry, 1.0F, -1.0F, 0.0F).texture(1.0F, 0.0F).color(c1);
      stack.pop();
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.ROMB_ESP.apply(RHOMB_TEXTURE));
   }
}
