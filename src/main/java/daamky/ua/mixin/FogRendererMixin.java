package daamky.ua.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
import daamky.ua.utils.render.FogEffectSuppression;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.fog.FogRenderer.FogMode;
import net.minecraft.world.level.material.FogType;

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
      method = {"setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$beginApplyFog(
      Camera camera, int viewDistance, DeltaTracker tickCounter, float skyDarkness, ClientLevel world, CallbackInfoReturnable<Vector4f> cir
   ) {
      FogEffectSuppression.begin();
   }

   @Inject(
      method = {"setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$endApplyFog(
      Camera camera, int viewDistance, DeltaTracker tickCounter, float skyDarkness, ClientLevel world, CallbackInfoReturnable<Vector4f> cir
   ) {
      FogEffectSuppression.end();
   }

   @Inject(
      method = {"computeFogColor"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$beginFogColor(
      Camera camera, float tickProgress, ClientLevel world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      FogEffectSuppression.begin();
   }

   @Inject(
      method = {"computeFogColor"},
      at = {@At("RETURN")},
      require = 0
   )
   private void daamky$endFogColor(
      Camera camera, float tickProgress, ClientLevel world, int viewDistance, float skyDarkness, CallbackInfoReturnable<Vector4f> cir
   ) {
      FogEffectSuppression.end();
   }

   @Inject(
      method = {"getBuffer"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getFogBuffer(FogMode fogType, CallbackInfoReturnable<GpuBufferSlice> cir) {
      if (NoRender.isActive("Fog")) {
         cir.setReturnValue(this.emptyBuffer.slice(0L, (long)FOG_UBO_SIZE));
      }
   }

   @Inject(
      method = {"getFogType"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$getFogType(Camera camera, CallbackInfoReturnable<FogType> cir) {
      if (camera != null && camera.getFluidInCamera() == FogType.LAVA && NoRender.isActive("Lava")) {
         cir.setReturnValue(FogType.ATMOSPHERIC);
      }
   }

   @ModifyArg(
      method = {"setupFog(Lnet/minecraft/client/Camera;ILnet/minecraft/client/DeltaTracker;FLnet/minecraft/client/multiplayer/ClientLevel;)Lorg/joml/Vector4f;"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/fog/FogRenderer;updateBuffer(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
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
