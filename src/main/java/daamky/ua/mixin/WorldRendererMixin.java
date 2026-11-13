package daamky.ua.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DefaultFramebufferSet;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.render.FramePass;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.state.WorldRenderState;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.profiler.Profiler;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.IMinecraft;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.FogBlur;
import daamky.ua.api.module.impl.visual.JumpCircles;
import daamky.ua.api.module.impl.visual.KillEffect;
import daamky.ua.api.module.impl.visual.NoRender;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.RenderCompatibility;
import daamky.ua.utils.render.post.customfog.CustomFogRenderer;
import daamky.ua.utils.render.post.fogblur.FogBlurRenderer;
import daamky.ua.utils.render.post.heavenlywrath.HeavenlyWrathWorldRenderer;
import daamky.ua.utils.render.sky.CustomSkyRenderer;
import daamky.ua.utils.render.ui.blur.BlurFramebuffer;
import daamky.ua.utils.render.world.blockblur.BlockBlur3D;
import daamky.ua.utils.render.world.blockoverlay.BlockOverlayShader3D;
import daamky.ua.utils.render.world.starsparkle.StarSparkle3D;

@Mixin({WorldRenderer.class})
public class WorldRendererMixin implements IMinecraft {
   @Shadow
   @Final
   private MinecraftClient client;
   @Shadow
   @Final
   private BufferBuilderStorage bufferBuilders;
   @Shadow
   @Final
   private DefaultFramebufferSet framebufferSet;
   @Shadow
   private ClientWorld world;

   public WorldRendererMixin() {
   }

   @Inject(
      method = {"renderSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSky(FrameGraphBuilder frameGraphBuilder, Camera camera, GpuBufferSlice fog, CallbackInfo ci) {
      Ambience ambience = Ambience.getInstance();
      if (ambience != null && ambience.shouldHideVanillaSky()) {
         ci.cancel();
      } else if (ambience != null && ambience.hasCustomSky() && this.framebufferSet != null && this.framebufferSet.mainFramebuffer != null) {
         FramePass pass = frameGraphBuilder.createPass("daamky_ambience_sky");
         Handle<Framebuffer> mainTarget = pass.transfer(this.framebufferSet.mainFramebuffer);
         this.framebufferSet.mainFramebuffer = mainTarget;
         pass.setRenderer(
            () -> CustomSkyRenderer.renderCustomSky(
                  (Framebuffer)mainTarget.get(),
                  ambience.getCustomSkyColor(),
                  ambience.getCustomSkySpeed(),
                  ambience.getCustomSkyIntensity(),
                  ambience.getCustomSkyModeId()
               )
         );
         ci.cancel();
      } else {
         NoRender noRender = NoRender.getInstance();
         if (noRender != null && noRender.shouldHideSkyBackground() && this.world != null) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("HEAD")}
   )
   private void onRenderHead(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f frustumMatrix,
      GpuBufferSlice fog,
      Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      Ambience ambience = Ambience.getInstance();
      MatrixStack captureStack = new MatrixStack();
      captureStack.multiplyPositionMatrix(new Matrix4f(positionMatrix));
      Render3D.lastProjMat.set(projectionMatrix);
      Render3D.lastModMat.set(positionMatrix);
      Render3D.lastWorldSpaceMatrix.set(captureStack.peek().getPositionMatrix());
      Render3D.setLastWorldSpaceEntry(captureStack.peek());
      Render3D.setLastTickDelta(tickCounter.getTickProgress(true));
      Render3D.setLastCameraPos(camera.getCameraPos());
      Render3D.setLastCameraRotation(new Quaternionf(camera.getRotation()));
      CustomSkyRenderer.setMatrices(projectionMatrix, positionMatrix);
      BlockBlur3D.setMatrices(projectionMatrix, positionMatrix);
      BlockOverlayShader3D.setMatrices(projectionMatrix, positionMatrix);
      StarSparkle3D.setMatrices(projectionMatrix, positionMatrix);
      if (fogColor != null) {
         float saturation = KillEffect.getWorldSaturationMultiplier();
         if (ambience != null && ambience.isEnabled()) {
            saturation *= ambience.getSaturationFactor();
         }

         if (Float.isFinite(saturation) && Math.abs(saturation - 1.0F) > 5.0E-4F) {
            saturation = Math.clamp(saturation, 0.0F, 2.0F);
            float luminance = fogColor.x * 0.2126F + fogColor.y * 0.7152F + fogColor.z * 0.0722F;
            fogColor.set(
               luminance + (fogColor.x - luminance) * saturation,
               luminance + (fogColor.y - luminance) * saturation,
               luminance + (fogColor.z - luminance) * saturation,
               fogColor.w
            );
         }

         BlurFramebuffer.setSkyFallbackColor(fogColor.x, fogColor.y, fogColor.z);
         FogBlurRenderer.setFallbackColor(fogColor.x, fogColor.y, fogColor.z);
      }
   }

   @Inject(
      method = {"hasBlindnessOrDarkness"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onHasBlindnessOrDarkness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
      NoRender noRender = NoRender.getInstance();
      if (noRender != null && noRender.isEnabled()) {
         if (camera.getFocusedEntity() instanceof LivingEntity livingEntity) {
            boolean hasBlindness = livingEntity.hasStatusEffect(StatusEffects.BLINDNESS);
            boolean hasDarkness = livingEntity.hasStatusEffect(StatusEffects.DARKNESS);
            if (NoRender.isActive("Bad Effects") && hasBlindness && !hasDarkness) {
               cir.setReturnValue(false);
            }

            if (NoRender.isActive("Darkness") && hasDarkness && !hasBlindness) {
               cir.setReturnValue(false);
            }

            if (NoRender.isActive("Bad Effects") && NoRender.isActive("Darkness")) {
               cir.setReturnValue(false);
            }
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At("RETURN")}
   )
   private void onRenderWorld(
      ObjectAllocator allocator,
      RenderTickCounter tickCounter,
      boolean renderBlockOutline,
      Camera camera,
      Matrix4f positionMatrix,
      Matrix4f projectionMatrix,
      Matrix4f frustumMatrix,
      GpuBufferSlice fog,
      Vector4f fogColor,
      boolean renderSky,
      CallbackInfo ci
   ) {
      if (mc.world != null && mc.player != null) {
         RenderCompatibility.primeFromCurrentContext();
         MatrixStack stack = new MatrixStack();
         stack.multiplyPositionMatrix(new Matrix4f(positionMatrix));
         boolean prevDepthTest = GL11.glIsEnabled(2929);
         boolean prevBlend = GL11.glIsEnabled(3042);
         boolean prevCull = GL11.glIsEnabled(2884);
         boolean prevScissor = GL11.glIsEnabled(3089);
         boolean prevDepthMask = GL11.glGetBoolean(2930);
         int prevDepthFunc = GL11.glGetInteger(2932);
         int prevBlendSrcRgb = GL11.glGetInteger(32969);
         int prevBlendDstRgb = GL11.glGetInteger(32968);
         int prevBlendSrcAlpha = GL11.glGetInteger(32971);
         int prevBlendDstAlpha = GL11.glGetInteger(32970);
         int[] prevScissorBox = new int[4];
         GL11.glGetIntegerv(3088, prevScissorBox);

         try {
            WorldRenderEvent event = new WorldRenderEvent(stack, tickCounter.getTickProgress(true));
            Manager.postEvent(event);
            Render3D.onWorldRender(event);
            JumpCircles jumpCircles = JumpCircles.getInstance();
            if (jumpCircles != null && jumpCircles.isEnabled()) {
               jumpCircles.onAfterWorldRender(this.client.getFramebuffer(), projectionMatrix, positionMatrix, camera.getCameraPos());
            }
         } finally {
            this.restoreRenderState(
               prevDepthTest,
               prevBlend,
               prevCull,
               prevScissor,
               prevDepthMask,
               prevDepthFunc,
               prevBlendSrcRgb,
               prevBlendDstRgb,
               prevBlendSrcAlpha,
               prevBlendDstAlpha,
               prevScissorBox
            );
         }
      }
   }

   @Inject(
      method = {"method_62214"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/SectionRenderState;renderSection(Lnet/minecraft/client/render/BlockRenderLayerGroup;Lnet/minecraft/client/gl/GpuSampler;)V",
         ordinal = 1,
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void daamky$fragEffectAfterTranslucent(
      GpuBufferSlice fog,
      WorldRenderState levelRenderState,
      Profiler profiler,
      Matrix4f positionMatrix,
      Handle<Framebuffer> mainTarget,
      Handle<Framebuffer> translucentTarget,
      boolean renderBlockOutline,
      Handle<Framebuffer> itemEntityTarget,
      Handle<Framebuffer> entityOutlineTarget,
      CallbackInfo ci
   ) {
      if (mc.world != null && mc.player != null) {
         Framebuffer target = mainTarget != null ? (Framebuffer)mainTarget.get() : this.client.getFramebuffer();
         FogBlur fogBlur = FogBlur.getInstance();
         if (fogBlur != null && fogBlur.isEnabled()) {
            fogBlur.onAfterTranslucent(target);
         }

         Ambience ambience = Ambience.getInstance();
         if (ambience != null && ambience.hasCustomFog()) {
            CustomFogRenderer.apply(
               target, ambience.getCustomFogColor(), ambience.getCustomFogDensity(), 0.12F, 3.0F, 0.25F, 0.12F, ambience.getWeatherModeName()
            );
         }

         if (ambience != null && ambience.hasHeavenlyWrathWorldEffects()) {
            HeavenlyWrathWorldRenderer.apply(
               target,
               Render3D.lastProjMat,
               positionMatrix,
               Render3D.lastCameraPos,
               ambience.getCustomSkyColor(),
               ambience.getCustomSkySpeed(),
               ambience.getCustomSkyIntensity(),
               ambience.getHeavenlyShadowStrength()
            );
         }

         if (ambience != null && ambience.hasBlackHoleSky()) {
            ambience.renderBlackHole(target, Render3D.lastProjMat, positionMatrix);
         }

         KillEffect killEffect = KillEffect.getInstanceIfReady();
         if (killEffect != null && killEffect.isEnabled()) {
            killEffect.onAfterTranslucent(target, Render3D.lastProjMat, positionMatrix, Render3D.lastCameraPos);
         }
      }
   }

   private void restoreRenderState(
      boolean depthTest,
      boolean blend,
      boolean cull,
      boolean scissor,
      boolean depthMask,
      int depthFunc,
      int blendSrcRgb,
      int blendDstRgb,
      int blendSrcAlpha,
      int blendDstAlpha,
      int[] scissorBox
   ) {
      if (depthTest) {
         GlStateManager._enableDepthTest();
      } else {
         GlStateManager._disableDepthTest();
      }

      GlStateManager._depthMask(depthMask);
      GlStateManager._depthFunc(depthFunc);
      GlStateManager._colorMask(true, true, true, true);
      if (blend) {
         GlStateManager._enableBlend();
         GlStateManager._blendFuncSeparate(blendSrcRgb, blendDstRgb, blendSrcAlpha, blendDstAlpha);
      } else {
         GlStateManager._disableBlend();
      }

      if (cull) {
         GlStateManager._enableCull();
      } else {
         GlStateManager._disableCull();
      }

      if (scissor) {
         GlStateManager._enableScissorTest();
         GlStateManager._scissorBox(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3]);
      } else {
         GlStateManager._disableScissorTest();
      }
   }
}
