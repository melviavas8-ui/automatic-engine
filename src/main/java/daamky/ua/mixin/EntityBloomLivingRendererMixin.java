package daamky.ua.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.utils.render.post.entitybloom.EntityBloomWorldTrail;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

@Mixin({LivingEntityRenderer.class})
public abstract class EntityBloomLivingRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> {
   public EntityBloomLivingRendererMixin() {
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V"},
      at = {@At("TAIL")},
      require = 0
   )
   private void daamky$captureWorldTrail(LivingEntity entity, S state, float tickDelta, CallbackInfo ci) {
      EntityBloomWorldTrail.capture(entity, state);
   }
}
