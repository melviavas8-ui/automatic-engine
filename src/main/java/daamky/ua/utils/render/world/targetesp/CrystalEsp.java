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
import org.joml.Vector3f;
import daamky.ua.utils.render.WorldVertex;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.pipeline.ClientPipelines;

public final class CrystalEsp {
   private static final Identifier BLOOM = Identifier.of("daamky", "textures/particles/glow.png");
   private static final Vector3f[] VERTICES = new Vector3f[]{
      new Vector3f(0.0F, 1.5F, 0.0F),
      new Vector3f(0.0F, -1.5F, 0.0F),
      new Vector3f(1.0F, 0.0F, 0.0F),
      new Vector3f(-1.0F, 0.0F, 0.0F),
      new Vector3f(0.0F, 0.0F, 1.0F),
      new Vector3f(0.0F, 0.0F, -1.0F)
   };
   private static final int[][] FACES = new int[][]{{0, 2, 4}, {0, 4, 3}, {0, 3, 5}, {0, 5, 2}, {1, 4, 2}, {1, 3, 4}, {1, 5, 3}, {1, 2, 5}};
   private static final float[] FACE_BRIGHTNESS = new float[]{1.0F, 0.8F, 0.6F, 0.9F, 0.7F, 0.5F, 0.4F, 0.6F};
   private static final float CRYSTAL_SIZE = 0.1F;
   private static final float BLOOM_SIZE = 1.0F;

   private CrystalEsp() {
   }

   public static void render(MatrixStack stack, Immediate provider, TargetEspRenderContext context, float size, float speed, boolean redOnImpact) {
      LivingEntity target = context.target();
      float anim = MathHelper.clamp(context.alpha(), 0.0F, 1.0F);
      if (!(anim <= 0.01F) && target != null) {
         float sizeMultiplier = MathHelper.clamp(size, 0.5F, 2.0F);
         float speedMultiplier = MathHelper.clamp(speed / 4.0F, 0.125F, 3.0F);
         float moving = (float)(context.frameTimeMs() % 100000L) * 0.2F * speedMultiplier;
         float width = target.getWidth() * 1.5F * sizeMultiplier;
         float height = target.getHeight();
         float hurt = redOnImpact ? MathHelper.clamp(context.hurtProgress(), 0.0F, 1.0F) : 0.0F;
         int rgb = ColorUtil.lerpColor(context.primaryColor(), -50630, hurt) | 0xFF000000;
         int baseColor = ColorUtil.withAlpha(rgb, Math.round(255.0F * anim));
         int bloomColor = ColorUtil.withAlpha(rgb, Math.round(255.0F * anim * 0.2F));
         VertexConsumer crystal = provider.getBuffer(ClientPipelines.CRYSTAL_FILLED_TRIANGLES);

         for (int i = 0; i < 360; i += 20) {
            float val = 1.2F - 0.5F * anim;
            float angle = (float)Math.toRadians((double)((float)i + moving * 0.3F));
            float sin = (float)(Math.sin((double)angle) * (double)width * (double)val);
            float cos = (float)(Math.cos((double)angle) * (double)width * (double)val);
            float yOff = 0.1F + height * Math.abs((float)Math.sin((double)i));
            stack.push();
            stack.translate(sin, yOff, cos);
            Vector3f dir = new Vector3f(-sin, height * 0.5F - 1.0F, -cos);
            if (dir.lengthSquared() < 1.0E-8F) {
               dir.set(0.0F, 1.0F, 0.0F);
            } else {
               dir.normalize();
            }

            stack.multiply(new Quaternionf().rotationTo(new Vector3f(0.0F, 1.0F, 0.0F), dir));
            renderCrystalMesh(stack, crystal, 0.1F * sizeMultiplier, baseColor);
            stack.pop();
         }

         provider.draw(ClientPipelines.CRYSTAL_FILLED_TRIANGLES);
         VertexConsumer bloom = provider.getBuffer(ClientPipelines.BLOOM_ESP.apply(BLOOM));
         Quaternionf camRot = MinecraftClient.getInstance().gameRenderer.getCamera().getRotation();
         float half = 1.0F * sizeMultiplier * 0.5F;

         for (int i = 0; i < 360; i += 20) {
            float val = 1.2F - 0.5F * anim;
            float angle = (float)Math.toRadians((double)((float)i + moving * 0.3F));
            float sin = (float)(Math.sin((double)angle) * (double)width * (double)val);
            float cos = (float)(Math.cos((double)angle) * (double)width * (double)val);
            float yOff = 0.1F + height * Math.abs((float)Math.sin((double)i));
            stack.push();
            stack.translate(sin, yOff, cos);
            stack.multiply(camRot);
            Entry pose = stack.peek();
            WorldVertex.textured(bloom, pose, -half, -half, 0.0F, 0.0F, 0.0F, bloomColor);
            WorldVertex.textured(bloom, pose, half, -half, 0.0F, 1.0F, 0.0F, bloomColor);
            WorldVertex.textured(bloom, pose, half, half, 0.0F, 1.0F, 1.0F, bloomColor);
            WorldVertex.textured(bloom, pose, -half, half, 0.0F, 0.0F, 1.0F, bloomColor);
            stack.pop();
         }

         provider.draw(ClientPipelines.BLOOM_ESP.apply(BLOOM));
      }
   }

   public static void endBatch(Immediate provider) {
   }

   private static void renderCrystalMesh(MatrixStack matrices, VertexConsumer buffer, float size, int color) {
      matrices.push();
      matrices.scale(size, size, size);
      Entry pose = matrices.peek();

      for (int i = 0; i < FACES.length; i++) {
         int[] face = FACES[i];
         int shaded = applyBrightness(color, FACE_BRIGHTNESS[i]);
         Vector3f v1 = VERTICES[face[0]];
         Vector3f v2 = VERTICES[face[1]];
         Vector3f v3 = VERTICES[face[2]];
         buffer.vertex(pose, v1.x, v1.y, v1.z).color(shaded);
         buffer.vertex(pose, v2.x, v2.y, v2.z).color(shaded);
         buffer.vertex(pose, v3.x, v3.y, v3.z).color(shaded);
      }

      matrices.pop();
   }

   private static int applyBrightness(int color, float brightness) {
      int alpha = color >> 24 & 0xFF;
      int red = Math.min(255, Math.max(0, (int)((float)(color >> 16 & 0xFF) * brightness)));
      int green = Math.min(255, Math.max(0, (int)((float)(color >> 8 & 0xFF) * brightness)));
      int blue = Math.min(255, Math.max(0, (int)((float)(color & 0xFF) * brightness)));
      return alpha << 24 | red << 16 | green << 8 | blue;
   }
}
