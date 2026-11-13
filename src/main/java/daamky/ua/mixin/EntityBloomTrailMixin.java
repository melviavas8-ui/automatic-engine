package daamky.ua.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.utils.render.post.entitybloom.EntityBloomWorldTrail;

@Mixin({EntityRenderManager.class})
public abstract class EntityBloomTrailMixin {
   public EntityBloomTrailMixin() {
   }

   @Inject(
      method = {"render"},
      at = {@At("TAIL")}
   )
   private <S extends EntityRenderState> void daamky$submitWorldTrail(
      S state,
      CameraRenderState cameraState,
      double relativeX,
      double relativeY,
      double relativeZ,
      MatrixStack poseStack,
      OrderedRenderCommandQueue collector,
      CallbackInfo ci
   ) {
      EntityBloomWorldTrail.submitGhosts((EntityRenderManager)(Object)this, state, cameraState, relativeX, relativeY, relativeZ, poseStack, collector);
   }
}

