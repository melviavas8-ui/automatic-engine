package daamky.ua.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import net.minecraft.client.gl.PostEffectProcessor.FramebufferSet;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.visual.EntityBloom;
import daamky.ua.mixin.accessor.PostPassAccessor;

@Mixin({PostEffectProcessor.class})
public abstract class PostChainMixin {
   private static final Identifier ENTITY_OUTLINE = Identifier.ofVanilla("entity_outline");
   @Shadow
   @Final
   private List<PostEffectPass> passes;
   @Shadow
   @Final
   private Set<Identifier> externalTargets;

   public PostChainMixin() {
   }

   @Inject(
      method = {"render(Lnet/minecraft/client/render/FrameGraphBuilder;IILnet/minecraft/client/gl/PostEffectProcessor$FramebufferSet;)V"},
      at = {@At("HEAD")}
   )
   private void daamky$configureEntityBloom(FrameGraphBuilder frameGraph, int width, int height, FramebufferSet targets, CallbackInfo ci) {
      if (this.externalTargets.contains(ENTITY_OUTLINE)) {
         EntityBloom bloom = EntityBloom.getInstance();
         float radius = bloom != null ? bloom.getShaderRadius() : 2.0F;
         float strength = bloom != null ? bloom.getShaderStrength() : 1.0F;
         float outlineStrength = bloom != null ? bloom.getOutlineStrength() : 0.35F;
         float hitFill = bloom != null && bloom.isHitColorEnabled() ? 1.0F : 0.0F;
         float flameAmount = bloom != null ? bloom.getFlameAmount() : 0.0F;
         float flameSpeed = bloom != null ? bloom.getFlameSpeed() : 0.75F;
         float flameDetail = bloom != null ? bloom.getFlameDetail() : 0.8F;
         float viewOffsetX = daamky$viewAnchorX();
         float viewOffsetY = daamky$viewAnchorY();
         CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
         int blurPass = 0;

         for (PostEffectPass pass : this.passes) {
            Map<String, GpuBuffer> uniforms = ((PostPassAccessor)pass).daamky$getCustomUniforms();
            if (uniforms.containsKey("BloomConfig")) {
               writeHitFill(encoder, uniforms, hitFill);
            }

            GpuBuffer blur = uniforms.get("BlurConfig");
            if (blur != null) {
               int passIndex = blurPass++;
               boolean horizontal = (passIndex & 1) == 0;
               float coreRadius = Math.max(2.6F, radius * 0.42F);
               float wideRadius = Math.max(2.4F, radius);
               float hazeRadius = Math.min(20.0F, Math.max(6.0F, radius * 2.0F));

               float layerRadius = switch (passIndex >> 1) {
                  case 0 -> coreRadius;
                  case 1 -> remainingRadius(wideRadius, coreRadius);
                  default -> remainingRadius(hazeRadius, wideRadius);
               };
               writeBlur(encoder, uniforms, horizontal ? 1.0F : 0.0F, horizontal ? 0.0F : 1.0F, layerRadius, outlineStrength);
            }

            GpuBuffer blit = uniforms.get("BlitConfig");
            if (blit != null) {
               writeBlit(encoder, uniforms, strength, outlineStrength, radius, flameAmount, flameSpeed, flameDetail, viewOffsetX, viewOffsetY);
            }
         }
      }
   }

   private static float daamky$viewAnchorX() {
      return (float)daamky$wrapAnchor(Math.toRadians(daamky$cameraYaw()) * daamky$focalLength());
   }

   private static float daamky$viewAnchorY() {
      return (float)daamky$wrapAnchor(-Math.toRadians(daamky$cameraPitch()) * daamky$focalLength());
   }

   private static double daamky$focalLength() {
      double fieldOfView = (double)((Integer)MinecraftClient.getInstance().options.getFov().getValue()).intValue();
      return 0.5 / Math.tan(Math.toRadians(Math.max(1.0, fieldOfView)) * 0.5);
   }

   private static double daamky$cameraYaw() {
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      return camera.isReady() ? (double)camera.getYaw() : 0.0;
   }

   private static double daamky$cameraPitch() {
      Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
      return camera.isReady() ? (double)camera.getPitch() : 0.0;
   }

   private static double daamky$wrapAnchor(double value) {
      double period = 8192.0;
      return value - period * Math.floor((value + period * 0.5) / period);
   }

   private static float remainingRadius(float target, float alreadyApplied) {
      float remaining = target * target - alreadyApplied * alreadyApplied;
      return remaining <= 1.0F ? 1.0F : (float)Math.sqrt((double)remaining);
   }

   private static void writeHitFill(CommandEncoder encoder, Map<String, GpuBuffer> uniforms, float enabled) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.malloc(16);
         data.putFloat(enabled);
         data.putFloat(0.0F);
         data.putFloat(0.0F);
         data.putFloat(0.0F);
         data.flip();
         writeUniform(encoder, uniforms, "BloomConfig", data);
      } catch (Throwable var7) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var6) {
               var7.addSuppressed(var6);
            }
         }

         throw var7;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void writeBlur(
      CommandEncoder encoder, Map<String, GpuBuffer> uniforms, float directionX, float directionY, float radius, float outlineStrength
   ) {
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.malloc(16);
         data.putFloat(directionX);
         data.putFloat(directionY);
         data.putFloat(radius);
         data.putFloat(outlineStrength);
         data.flip();
         writeUniform(encoder, uniforms, "BlurConfig", data);
      } catch (Throwable var10) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var9) {
               var10.addSuppressed(var9);
            }
         }

         throw var10;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void writeBlit(
      CommandEncoder encoder,
      Map<String, GpuBuffer> uniforms,
      float strength,
      float outlineStrength,
      float radius,
      float flameAmount,
      float flameSpeed,
      float flameDetail,
      float viewOffsetX,
      float viewOffsetY
   ) {
      float normalizedStrength = Math.clamp(strength, 0.0F, 1.5F);
      float alpha = Math.min(0.98F, normalizedStrength * 0.88F);
      float light = 1.0F + normalizedStrength * 0.18F;
      float corePower = 0.85F + Math.clamp(outlineStrength, 0.0F, 1.0F) * 1.25F;
      float bloomPower = 0.9F + normalizedStrength * 0.55F;
      float haloPower = 0.7F + Math.clamp(radius / 14.0F, 0.0F, 1.0F) * 0.65F;
      float hitPower = 1.38F;
      float animationTime = (float)(System.nanoTime() % 120000000000L) * 1.0E-9F;
      MemoryStack stack = MemoryStack.stackPush();

      try {
         ByteBuffer data = stack.malloc(64);
         data.putFloat(light);
         data.putFloat(light);
         data.putFloat(light);
         data.putFloat(alpha);
         data.putFloat(corePower);
         data.putFloat(bloomPower);
         data.putFloat(haloPower);
         data.putFloat(hitPower);
         data.putFloat(animationTime);
         data.putFloat(Math.clamp(flameAmount, 0.0F, 1.5F));
         data.putFloat(Math.clamp(flameSpeed, 0.1F, 5.0F));
         data.putFloat(Math.clamp(flameDetail, 0.2F, 1.5F));
         data.putFloat(viewOffsetX);
         data.putFloat(viewOffsetY);
         data.putFloat(0.0F);
         data.putFloat(0.0F);
         data.flip();
         writeUniform(encoder, uniforms, "BlitConfig", data);
      } catch (Throwable var22) {
         if (stack != null) {
            try {
               stack.close();
            } catch (Throwable var21) {
               var22.addSuppressed(var21);
            }
         }

         throw var22;
      }

      if (stack != null) {
         stack.close();
      }
   }

   private static void writeUniform(CommandEncoder encoder, Map<String, GpuBuffer> uniforms, String name, ByteBuffer data) {
      GpuBuffer buffer = uniforms.get(name);
      if (buffer != null) {
         if ((buffer.usage() & 8) == 0) {
            GpuBuffer writable = RenderSystem.getDevice().createBuffer(() -> "EntityBloom " + name, 136, data);
            uniforms.put(name, writable);
            buffer.close();
         } else {
            encoder.writeToBuffer(buffer.slice(), data);
         }
      }
   }
}
