package daamky.ua.utils.render.world.targetesp;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Quaternionf;
import daamky.ua.api.module.impl.visual.TargetESP;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class CircleRenderer {
   private static final Identifier GLOW_TEXTURE = Identifier.of("daamky", "textures/particles/glow.png");
   private static final int ANGLE_STEP = 2;
   private static final int LAYERS = 15;
   private static final float SPRITE_SIZE = 0.2F;
   private static float jelloMoving;

   private CircleRenderer() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context, CircleTargetEspRenderer.Settings settings) {
      TargetESP targetESP = TargetESP.getInstance();
      jelloMoving = jelloMoving + (float)targetESP.speed1();
      float alpha = context.alpha();
      if (!(alpha <= 0.0F)) {
         LivingEntity target = context.target();
         float entityWidth = target.getWidth() * 1.65F * settings.size();
         float entityHeight = target.getHeight() - 0.15F;
         float alphaAnim = TargetEspMath.easeOutCubic(alpha);
         float scale = Math.max(0.5F, 0.7F - 0.2F * alphaAnim);
         int color = damageColor(context);
         int r = color >> 16 & 0xFF;
         int g = color >> 8 & 0xFF;
         int b = color & 0xFF;
         Quaternionf cameraRotation = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
         VertexConsumer buffer = provider.getBuffer(ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE));

         for (int i = 0; i < 360; i += 2) {
            double rad = Math.toRadians((double)((float)i + jelloMoving));
            float xOffset = (float)(Math.cos(rad) * (double)entityWidth * (double)scale);
            float zOffset = (float)(Math.sin(rad) * (double)entityWidth * (double)scale);

            for (int j = 0; j < 15; j++) {
               float yLayer = entityHeight / 1.7F + entityHeight / 2.0F * (float)Math.cos(Math.toRadians((double)(jelloMoving / 1.5F + (float)j * 2.0F)));
               int a = (int)(255.0F * alphaAnim * ((float)j / 15.0F) * 0.05F);
               appendSprite(stack, buffer, cameraRotation, xOffset, yLayer, zOffset, 0.2F * settings.size(), r, g, b, a);
            }
         }

         for (int i = 0; i < 360; i += 2) {
            double rad = Math.toRadians((double)((float)i + jelloMoving));
            float xOffset = (float)(Math.cos(rad) * (double)entityWidth * (double)scale);
            float zOffset = (float)(Math.sin(rad) * (double)entityWidth * (double)scale);
            float yOffset = entityHeight / 1.75F + entityHeight / 2.0F * (float)Math.cos(Math.toRadians((double)(jelloMoving / 1.5F + 30.0F)));
            int a = (int)(255.0F * alphaAnim * 0.2F);
            appendSprite(stack, buffer, cameraRotation, xOffset, yOffset, zOffset, 0.2F, r, g, b, a);
         }
      }
   }

   private static void appendSprite(
      MatrixStack stack, VertexConsumer buffer, Quaternionf cameraRotation, float x, float y, float z, float size, int r, int g, int b, int alpha
   ) {
      if (alpha > 0) {
         int color = ColorUtil.rgba(r, g, b, MathHelper.clamp(alpha, 0, 255));
         stack.push();
         stack.translate(x, y, z);
         stack.multiply(cameraRotation);
         Entry pose = stack.peek();
         float h = size / 2.0F;
         WorldVertex.textured(buffer, pose, -h, -h, 0.0F, 0.0F, 0.0F, color);
         WorldVertex.textured(buffer, pose, h, -h, 0.0F, 1.0F, 0.0F, color);
         WorldVertex.textured(buffer, pose, h, h, 0.0F, 1.0F, 1.0F, color);
         WorldVertex.textured(buffer, pose, -h, h, 0.0F, 0.0F, 1.0F, color);
         stack.pop();
      }
   }

   private static int damageColor(TargetEspRenderContext context) {
      int base = context.primaryColor() | 0xFF000000;
      float hurt = context.hurtProgress();
      TargetESP targetESP = TargetESP.getInstance();
      if (hurt > 0.0F) {
         float impact = TargetEspMath.easeOutCubic(MathHelper.clamp(hurt, 0.0F, 1.0F));
         if (targetESP.redik()) {
            int red = ColorUtil.rgba(255, 50, 50, 255);
            base = ColorUtil.lerpColor(base, red, impact);
         }
      }

      return base;
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_TEXTURE));
   }

   public static void reset() {
      jelloMoving = 0.0F;
   }
}
