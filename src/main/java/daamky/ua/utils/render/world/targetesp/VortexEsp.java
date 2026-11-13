package daamky.ua.utils.render.world.targetesp;

import java.util.Random;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class VortexEsp {
   public static final Identifier TEXTURE_VORTEX = Identifier.of("daamky", "textures/new/vortex.png");
   public static final Identifier TEXTURE_BRACKETS = Identifier.of("daamky", "textures/new/brackets.png");
   private static final int LAYERS = 3;
   private static final long MODULE_START_MS = System.currentTimeMillis();
   private static final float[] rotSpeed = new float[3];
   private static final float[] pitchSpeed = new float[3];
   private static final float[] pitchPhase = new float[3];
   private static final float[] yawBase = new float[3];
   private static LivingEntity seededFor;
   private static final Random RANDOM = new Random();
   private static float sizeMul = 1.2F;
   private static float speedMul = 1.0F;
   private static boolean redOnHit = true;
   private static Identifier activeTexture = TEXTURE_VORTEX;

   private VortexEsp() {
   }

   public static void configure(float size, float speed, boolean redHit) {
      configure(size, speed, redHit, TEXTURE_VORTEX);
   }

   public static void configure(float size, float speed, boolean redHit, Identifier texture) {
      sizeMul = MathHelper.clamp(size, 0.3F, 3.0F);
      speedMul = MathHelper.clamp(speed, 0.0F, 4.0F);
      redOnHit = redHit;
      activeTexture = texture != null ? texture : TEXTURE_VORTEX;
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context) {
      LivingEntity target = context.target();
      if (target != null) {
         if (seededFor != target) {
            reseed(target);
         }

         float time = (float)(System.currentTimeMillis() - MODULE_START_MS) / 1000.0F;
         float speed = speedMul;
         float height = target.getHeight();
         float half = Math.max(target.getWidth(), target.getWidth()) * 2.5F * sizeMul;
         float baseY = height * 0.62F - 0.006F;
         float hurt = redOnHit ? MathHelper.clamp(context.hurtProgress(), 0.0F, 1.0F) : 0.0F;
         int baseColor = ColorUtil.lerpColor(context.primaryColor(), -50630, hurt);
         float[] layerAlpha = new float[3];
         float[] yawDeg = new float[3];
         float[] pitchDeg = new float[3];

         for (int j = 0; j < 3; j++) {
            float pulse = (MathHelper.sin((double)(time * speed * 3.4F + (float)j * (float) (Math.PI * 2.0 / 3.0))) + 1.0F) * 0.5F;
            layerAlpha[j] = MathHelper.lerp(pulse, 0.35F, 1.0F);
            yawDeg[j] = -time * 140.0F * speed * rotSpeed[j] + yawBase[j];
            pitchDeg[j] = MathHelper.sin((double)(time * pitchSpeed[j] + pitchPhase[j])) * 30.0F;
         }

         Identifier tex = activeTexture;
         VertexConsumer consumer = provider.getBuffer(ClientPipelines.ROMB_ESP.apply(tex));

         for (int k = 0; k < 3; k++) {
            int color = ColorUtil.withAlpha(baseColor, (int)(255.0F * context.alpha() * layerAlpha[k]));
            drawLayer(stack, consumer, half, baseY + 0.006F * (float)k, yawDeg[k], pitchDeg[k], color);
         }

         for (int l = 0; l < 3; l++) {
            int color = ColorUtil.withAlpha(baseColor, (int)(255.0F * context.alpha() * layerAlpha[l] * 0.85F));
            drawLayer(stack, consumer, half, baseY + 0.006F * (float)l, yawDeg[l], pitchDeg[l], color);
         }
      }
   }

   private static void drawLayer(MatrixStack stack, VertexConsumer consumer, float half, float y, float yawDeg, float pitchDeg, int color) {
      stack.push();
      stack.translate(0.0F, y, 0.0F);
      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F + yawDeg));
      stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitchDeg));
      Entry pose = stack.peek();
      consumer.vertex(pose, -half, 0.0F, -half).texture(0.0F, 0.0F).color(color);
      consumer.vertex(pose, -half, 0.0F, half).texture(0.0F, 1.0F).color(color);
      consumer.vertex(pose, half, 0.0F, half).texture(1.0F, 1.0F).color(color);
      consumer.vertex(pose, half, 0.0F, -half).texture(1.0F, 0.0F).color(color);
      stack.pop();
   }

   public static void endBatch(Immediate provider) {
      provider.draw(ClientPipelines.ROMB_ESP.apply(activeTexture));
   }

   public static void reset() {
      seededFor = null;
   }

   private static void reseed(LivingEntity target) {
      seededFor = target;

      for (int i = 0; i < 3; i++) {
         rotSpeed[i] = 0.75F + RANDOM.nextFloat() * 0.5F;
         pitchSpeed[i] = 0.8F + RANDOM.nextFloat() * 0.8F;
         pitchPhase[i] = RANDOM.nextFloat() * (float) (Math.PI * 2);
      }

      for (int attempt = 0; attempt < 64; attempt++) {
         for (int j = 0; j < 3; j++) {
            yawBase[j] = -30.0F + RANDOM.nextFloat() * 60.0F;
         }

         boolean ok = true;

         for (int a = 0; a < 3 && ok; a++) {
            for (int b = a + 1; b < 3; b++) {
               if (Math.abs(yawBase[a] - yawBase[b]) < 22.0F) {
                  ok = false;
                  break;
               }
            }
         }

         if (ok) {
            return;
         }
      }

      yawBase[0] = -30.0F + RANDOM.nextFloat() * 4.0F;
      yawBase[1] = -2.0F + RANDOM.nextFloat() * 4.0F;
      yawBase[2] = 30.0F - RANDOM.nextFloat() * 4.0F;
   }
}
