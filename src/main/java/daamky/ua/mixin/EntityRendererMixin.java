package daamky.ua.mixin;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.module.impl.visual.ESP;
import daamky.ua.api.module.impl.visual.EntityBloom;
import daamky.ua.api.module.impl.visual.WardenHelper;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin<S extends EntityRenderState> {
   public EntityRendererMixin() {
   }

   @Inject(
      method = {"updateRenderState"},
      at = {@At("TAIL")},
      require = 0
   )
   private void daamky$hideEspNameTags(Entity entity, S state, float tickDelta, CallbackInfo ci) {
      EntityBloom.apply(entity, state);
      if (ESP.shouldHideVanillaName(entity)) {
         state.displayName = null;
      }
   }

   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true,
      require = 0
   )
   private void daamky$hideEspServerTagEntities(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
      if (ESP.shouldSuppressServerTagEntity(entity)) {
         cir.setReturnValue(false);
      }

      if (WardenHelper.shouldHideVanillaTimer(entity)) {
         cir.setReturnValue(false);
      }
   }
}
