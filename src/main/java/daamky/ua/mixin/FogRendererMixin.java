package daamky.ua.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.NoRender;

@Mixin({FogRenderer.class})
public abstract class FogRendererMixin {
   @Shadow
   @Final
   private GpuBuffer emptyBuffer;
   @Shadow
   @Final
   private static int FOG_UBO_SIZE;

   public FogRendererMixin() {
   }

   @Inject(
      method = {"getFogBuffer"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getFogBuffer(FogType fogType, CallbackInfoReturnable<GpuBufferSlice> cir) {
      if (NoRender.isActive("Fog")) {
         cir.setReturnValue(this.emptyBuffer.slice(0L, (long)FOG_UBO_SIZE));
      }
   }

   @Inject(
      method = {"getCameraSubmersionType"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getFogType(Camera camera, CallbackInfoReturnable<CameraSubmersionType> cir) {
      if (camera != null && camera.getSubmersionType() == CameraSubmersionType.LAVA && NoRender.isActive("Lava")) {
         cir.setReturnValue(CameraSubmersionType.ATMOSPHERIC);
      }
   }

   @ModifyArg(
      method = {"applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
      ),
      index = 2,
      require = 0
   )
   private Vector4f daamky$modifyCustomFogColor(Vector4f color) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.hasCustomFog() && color != null) {
         int customColor = ambience.getCustomFogColor();
         return new Vector4f(
            (float)(customColor >> 16 & 0xFF) / 255.0F, (float)(customColor >> 8 & 0xFF) / 255.0F, (float)(customColor & 0xFF) / 255.0F, color.w
         );
      } else {
         return color;
      }
   }
}
