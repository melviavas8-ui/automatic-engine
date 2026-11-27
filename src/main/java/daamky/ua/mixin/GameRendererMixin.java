package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.CachedPerspectiveProjectionMatrixBuffer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.player.NoEntityTrace;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.AspectRatio;
import daamky.ua.api.module.impl.visual.GuiHands;
import daamky.ua.api.module.impl.visual.KillEffect;
import daamky.ua.api.module.impl.visual.NoRender;
import daamky.ua.screens.clickgui.ClickGui;
import daamky.ua.screens.modernui.impl.WorldAnimation;
import daamky.ua.utils.render.HudRenderLayer;
import daamky.ua.utils.render.post.saturation.Saturation2D;

@Mixin({GameRenderer.class})
public abstract class GameRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;
   @Shadow
   @Final
   private GuiRenderState guiRenderState;
   @Shadow
   @Final
   private Camera mainCamera;
   @Shadow
   @Final
   private CachedPerspectiveProjectionMatrixBuffer hud3dProjectionMatrixBuffer;
   @Shadow
   @Final
   private FeatureRenderDispatcher featureRenderDispatcher;
   @Shadow
   @Final
   private RenderBuffers renderBuffers;

   public GameRendererMixin() {
   }

   @Shadow
   private void renderItemInHand(float partialTicks, boolean renderBlockOutline, Matrix4f projectionMatrix) {
      throw new AssertionError();
   }

   @Shadow
   private float getFov(Camera camera, float partialTicks, boolean changingFov) {
      throw new AssertionError();
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
         shift = Shift.BEFORE
      )}
   )
   private void daamky$renderGuiOverlay(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
      this.daamky$applyWorldSaturation();
      if (HudRenderLayer.shouldRender(this.minecraft)) {
         Screen screen = this.minecraft.screen;
         if (screen == null || screen instanceof ClickGui || screen instanceof ChatScreen) {
            if (screen instanceof ChatScreen) {
               HudRenderLayer.renderChatOverlay(this.minecraft, this.daamky$newGuiGraphics(), deltaTracker.getGameTimeDeltaPartialTick(false));
            } else {
               HudRenderLayer.renderGameHud(this.minecraft, this.daamky$newGuiGraphics(), deltaTracker.getGameTimeDeltaPartialTick(false));
               if (ClickGui.shouldRenderOverlay()) {
                  GuiGraphics graphics = this.daamky$newGuiGraphics();
                  ClickGui.renderPanels(graphics, this.minecraft.getWindow().getGuiScaledWidth(), this.minecraft.getWindow().getGuiScaledHeight());
                  this.daamky$renderHandsInClickGui(deltaTracker.getGameTimeDeltaPartialTick(false));
               }
            }
         }
      }
   }

   private void daamky$renderHandsInClickGui(float partialTicks) {
      if (GuiHands.shouldRender() && this.minecraft.screen instanceof ClickGui && this.minecraft.player != null && this.mainCamera != null) {
         RenderSystem.setProjectionMatrix(
            this.hud3dProjectionMatrixBuffer
               .getBuffer(
                  this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), this.getFov(this.mainCamera, partialTicks, false)
               ),
            ProjectionType.PERSPECTIVE
         );
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(this.minecraft.getMainRenderTarget().getDepthTexture(), 1.0);
         Matrix4f cameraMatrix = new Matrix4f().rotation(this.mainCamera.rotation().conjugate(new Quaternionf()));
         this.renderItemInHand(partialTicks, false, cameraMatrix);
         this.featureRenderDispatcher.renderAllFeatures();
         this.renderBuffers.bufferSource().endBatch();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
         shift = Shift.BEFORE
      )},
      require = 0
   )
   private void daamky$renderHudBehindScreen(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
      if (HudRenderLayer.shouldRender(this.minecraft)) {
         Screen screen = this.minecraft.screen;
         if (screen != null && !(screen instanceof ChatScreen) && !(screen instanceof ClickGui)) {
            HudRenderLayer.renderScreenBackground(this.minecraft, this.daamky$newGuiGraphics(), deltaTracker.getGameTimeDeltaPartialTick(false));
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/Gui;render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
         shift = Shift.BEFORE
      )},
      require = 0
   )
   private void daamky$renderDrawLayerBelowVanillaHud(DeltaTracker deltaTracker, boolean tick, CallbackInfo ci) {
      if (HudRenderLayer.shouldRender(this.minecraft)) {
         HudRenderLayer.renderGameDrawEvents(this.daamky$newGuiGraphics(), deltaTracker.getGameTimeDeltaPartialTick(false));
      }
   }

   private GuiGraphics daamky$newGuiGraphics() {
      int mouseX = this.minecraft.mouseHandler != null && this.minecraft.getWindow() != null ? (int)Math.round(this.minecraft.mouseHandler.getScaledXPos(this.minecraft.getWindow())) : 0;
      int mouseY = this.minecraft.mouseHandler != null && this.minecraft.getWindow() != null ? (int)Math.round(this.minecraft.mouseHandler.getScaledYPos(this.minecraft.getWindow())) : 0;
      return new GuiGraphics(this.minecraft, this.guiRenderState, mouseX, mouseY);
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
      )}
   )
   private void daamky$renderModernWorldAnimation(
      GuiRenderer renderer, GpuBufferSlice fogBuffer, Operation<Void> original, DeltaTracker deltaTracker, boolean tick
   ) {
      original.call(new Object[]{renderer, fogBuffer});
      if (this.minecraft.screen == null && WorldAnimation.isActive()) {
         WorldAnimation.render(renderer, this.daamky$newGuiGraphics(), fogBuffer, deltaTracker.getGameTimeDeltaPartialTick(false));
      }
   }

   private void daamky$applyWorldSaturation() {
      if (this.minecraft != null && this.minecraft.player != null && this.minecraft.level != null) {
         float saturation = KillEffect.getWorldSaturationMultiplier();
         Ambience ambience = Ambience.getInstance();
         if (ambience != null && ambience.isEnabled()) {
            saturation *= ambience.getSaturationFactor();
         }

         if (Float.isFinite(saturation) && !(Math.abs(saturation - 1.0F) <= 5.0E-4F)) {
            Saturation2D.applyWithCopy(Math.clamp(saturation, 0.0F, 2.0F));
         }
      }
   }

   @WrapOperation(
      method = {"pick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;raycastHitResult(FLnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/phys/HitResult;"
      )},
      require = 0
   )
   private HitResult daamky$noEntityTracePick(LocalPlayer player, float tickProgress, Entity cameraEntity, Operation<HitResult> original) {
      NoEntityTrace noEntityTrace = NoEntityTrace.getInstance();
      if (noEntityTrace != null && noEntityTrace.shouldIgnoreEntityTrace() && cameraEntity != null && this.minecraft.player != null) {
         double range = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
         return cameraEntity.pick(range, tickProgress, false);
      } else {
         return (HitResult)original.call(new Object[]{player, tickProgress, cameraEntity});
      }
   }

   @Inject(
      method = {"getProjectionMatrix"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void daamky$aspectRatio(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      if (this.minecraft != null && this.minecraft.getWindow() != null) {
         int width = this.minecraft.getWindow().getWidth();
         int height = this.minecraft.getWindow().getHeight();
         if (width > 0 && height > 0) {
            float adjustedWidth = AspectRatio.resolveRatio((float)width, (float)height);
            if (!(Math.abs(adjustedWidth - (float)width) <= 1.0E-4F)) {
               float currentAspect = (float)width / (float)height;
               float targetAspect = adjustedWidth / (float)height;
               if (!(targetAspect <= 1.0E-4F)) {
                  Matrix4f matrix = new Matrix4f((Matrix4fc)cir.getReturnValue());
                  matrix.m00(matrix.m00() * (currentAspect / targetAspect));
                  cir.setReturnValue(matrix);
               }
            }
         }
      }
   }

   @Inject(
      method = {"bobHurt"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$noHurtBob(PoseStack stack, float tickDelta, CallbackInfo ci) {
      if (NoRender.isActive("Damage")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"displayItemActivation"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$noItemActivation(ItemStack stack, CallbackInfo ci) {
      if (NoRender.isActive("Totem Animation")) {
         ci.cancel();
      }
   }
}
