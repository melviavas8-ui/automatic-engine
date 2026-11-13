package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix3;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.HitResult;
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
   private MinecraftClient client;
   @Shadow
   @Final
   private GuiRenderState guiState;
   @Shadow
   @Final
   private Camera camera;
   @Shadow
   @Final
   private ProjectionMatrix3 hudProjectionMatrix;
   @Shadow
   @Final
   private RenderDispatcher entityRenderDispatcher;
   @Shadow
   @Final
   private BufferBuilderStorage buffers;

   public GameRendererMixin() {
   }

   @Shadow
   private void renderHand(float partialTicks, boolean renderBlockOutline, Matrix4f projectionMatrix) {
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
   private void daamky$renderGuiOverlay(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
      this.daamky$applyWorldSaturation();
      if (HudRenderLayer.shouldRender(this.client)) {
         Screen screen = this.client.currentScreen;
         if (screen == null || screen instanceof ClickGui || screen instanceof ChatScreen) {
            if (screen instanceof ChatScreen) {
               HudRenderLayer.renderChatOverlay(this.client, this.daamky$newGuiGraphics(), deltaTracker.getTickProgress(false));
            } else {
               HudRenderLayer.renderGameHud(this.client, this.daamky$newGuiGraphics(), deltaTracker.getTickProgress(false));
               if (ClickGui.shouldRenderOverlay()) {
                  DrawContext graphics = this.daamky$newGuiGraphics();
                  ClickGui.renderPanels(graphics, this.client.getWindow().getScaledWidth(), this.client.getWindow().getScaledHeight());
                  this.daamky$renderHandsInClickGui(deltaTracker.getTickProgress(false));
               }
            }
         }
      }
   }

   private void daamky$renderHandsInClickGui(float partialTicks) {
      if (GuiHands.shouldRender() && this.client.currentScreen instanceof ClickGui && this.client.player != null && this.camera != null) {
         RenderSystem.setProjectionMatrix(
            this.hudProjectionMatrix
               .set(
                  this.client.getWindow().getFramebufferWidth(), this.client.getWindow().getFramebufferHeight(), this.getFov(this.camera, partialTicks, false)
               ),
            ProjectionType.PERSPECTIVE
         );
         RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(this.client.getFramebuffer().getDepthAttachment(), 1.0);
         Matrix4f cameraMatrix = new Matrix4f().rotation(this.camera.getRotation().conjugate(new Quaternionf()));
         this.renderHand(partialTicks, false, cameraMatrix);
         this.entityRenderDispatcher.render();
         this.buffers.getEntityVertexConsumers().draw();
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V",
         shift = Shift.BEFORE
      )},
      require = 0
   )
   private void daamky$renderHudBehindScreen(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
      if (HudRenderLayer.shouldRender(this.client)) {
         Screen screen = this.client.currentScreen;
         if (screen != null && !(screen instanceof ChatScreen) && !(screen instanceof ClickGui)) {
            HudRenderLayer.renderScreenBackground(this.client, this.daamky$newGuiGraphics(), deltaTracker.getTickProgress(false));
         }
      }
   }

   @Inject(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V",
         shift = Shift.BEFORE
      )},
      require = 0
   )
   private void daamky$renderDrawLayerBelowVanillaHud(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
      if (HudRenderLayer.shouldRender(this.client)) {
         HudRenderLayer.renderGameDrawEvents(this.daamky$newGuiGraphics(), deltaTracker.getTickProgress(false));
      }
   }

   private DrawContext daamky$newGuiGraphics() {
      int mouseX = this.client.mouse != null && this.client.getWindow() != null ? (int)Math.round(this.client.mouse.getScaledX(this.client.getWindow())) : 0;
      int mouseY = this.client.mouse != null && this.client.getWindow() != null ? (int)Math.round(this.client.mouse.getScaledY(this.client.getWindow())) : 0;
      return new DrawContext(this.client, this.guiState, mouseX, mouseY);
   }

   @WrapOperation(
      method = {"render"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V"
      )}
   )
   private void daamky$renderModernWorldAnimation(
      GuiRenderer renderer, GpuBufferSlice fogBuffer, Operation<Void> original, RenderTickCounter deltaTracker, boolean tick
   ) {
      original.call(new Object[]{renderer, fogBuffer});
      if (this.client.currentScreen == null && WorldAnimation.isActive()) {
         WorldAnimation.render(renderer, this.daamky$newGuiGraphics(), fogBuffer, deltaTracker.getTickProgress(false));
      }
   }

   private void daamky$applyWorldSaturation() {
      if (this.client != null && this.client.player != null && this.client.world != null) {
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
      method = {"updateCrosshairTarget"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/network/ClientPlayerEntity;getCrosshairTarget(FLnet/minecraft/entity/Entity;)Lnet/minecraft/util/hit/HitResult;"
      )},
      require = 0
   )
   private HitResult daamky$noEntityTracePick(ClientPlayerEntity player, float tickProgress, Entity cameraEntity, Operation<HitResult> original) {
      NoEntityTrace noEntityTrace = NoEntityTrace.getInstance();
      if (noEntityTrace != null && noEntityTrace.shouldIgnoreEntityTrace() && cameraEntity != null && this.client.player != null) {
         double range = Math.max(player.getBlockInteractionRange(), player.getEntityInteractionRange());
         return cameraEntity.raycast(range, tickProgress, false);
      } else {
         return (HitResult)original.call(new Object[]{player, tickProgress, cameraEntity});
      }
   }

   @Inject(
      method = {"getBasicProjectionMatrix"},
      at = {@At("TAIL")},
      cancellable = true
   )
   private void daamky$aspectRatio(float fovDegrees, CallbackInfoReturnable<Matrix4f> cir) {
      if (this.client != null && this.client.getWindow() != null) {
         int width = this.client.getWindow().getFramebufferWidth();
         int height = this.client.getWindow().getFramebufferHeight();
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
      method = {"tiltViewWhenHurt"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$noHurtBob(MatrixStack stack, float tickDelta, CallbackInfo ci) {
      if (NoRender.isActive("Damage")) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"showFloatingItem"},
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
