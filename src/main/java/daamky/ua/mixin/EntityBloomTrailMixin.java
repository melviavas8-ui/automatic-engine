package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import daamky.ua.utils.render.post.entitybloom.EntityBloomWorldTrail;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;

@Mixin({EntityRenderDispatcher.class})
public abstract class EntityBloomTrailMixin {
   public EntityBloomTrailMixin() {
   }

   @Inject(
      method = {"submit"},
      at = {@At("TAIL")}
   )
   private <S extends EntityRenderState> void daamky$submitWorldTrail(
      S state,
      CameraRenderState cameraState,
      double relativeX,
      double relativeY,
      double relativeZ,
      PoseStack poseStack,
      SubmitNodeCollector collector,
      CallbackInfo ci
   ) {
      EntityBloomWorldTrail.submitGhosts((EntityRenderDispatcher)(Object)this, state, cameraState, relativeX, relativeY, relativeZ, poseStack, collector);
   }
}

