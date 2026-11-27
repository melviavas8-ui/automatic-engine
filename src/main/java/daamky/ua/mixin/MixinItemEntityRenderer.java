package daamky.ua.mixin;

import java.util.HashMap;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import daamky.ua.api.module.impl.visual.ItemPhysics;

@Mixin({ItemEntityRenderer.class})
public abstract class MixinItemEntityRenderer {
   @Unique
   private static final WeakHashMap<ItemEntityRenderState, Boolean> daamky$groundStateMap = new WeakHashMap<>();
   @Unique
   private static final HashMap<Integer, Integer> daamky$groundHoldMap = new HashMap<>();
   @Unique
   private ItemEntityRenderState daamky$currentState;

   public MixinItemEntityRenderer() {
   }

   @Inject(
      method = {"extractRenderState(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;F)V"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
      daamky$groundStateMap.put(state, daamky$resolveGroundState(entity, entity.onGround()));
   }

   @Redirect(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V",
         ordinal = 0
      ),
      require = 0
   )
   private void daamky$translate(
      PoseStack matrices,
      float x,
      float y,
      float z,
      ItemEntityRenderState state,
      PoseStack matricesArg,
      SubmitNodeCollector submitNodeCollector,
      CameraRenderState cameraRenderState
   ) {
      this.daamky$currentState = state;
      boolean enabled = daamky$isItemPhysicsEnabled();
      boolean stableGround = daamky$groundStateMap.getOrDefault(state, Boolean.valueOf(false));
      if (!enabled && !stableGround) {
         matrices.translate(x, y, z);
      } else {
         AABB box = state.item.getModelBoundingBox();
         matrices.translate(x, -((float)box.minY) + 0.0625F, z);
      }
   }

   @Redirect(
      method = {"submit(Lnet/minecraft/client/renderer/entity/state/ItemEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/ItemEntityRenderer;submitMultipleFromCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/ItemClusterRenderState;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/phys/AABB;)V"
      ),
      require = 0
   )
   private void daamky$renderPhysics(
      PoseStack matrices, SubmitNodeCollector submitNodeCollector, int light, ItemClusterRenderState stackState, RandomSource random, AABB box
   ) {
      if (daamky$isItemPhysicsEnabled() && this.daamky$currentState != null) {
         float age = this.daamky$currentState.ageInTicks;
         float offset = this.daamky$currentState.bobOffset;
         boolean onGround = daamky$groundStateMap.getOrDefault(this.daamky$currentState, Boolean.valueOf(false));
         matrices.mulPose(Axis.YP.rotation(-ItemEntity.getSpin(age, offset)));
         if (onGround) {
            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            matrices.translate(0.0F, -((float)box.getYsize() / 2.0F) + 0.0625F, 0.0F);
         } else {
            matrices.mulPose(Axis.XP.rotationDegrees((age * 15.0F + offset * 360.0F) % 360.0F));
         }
      }

      ItemEntityRenderer.submitMultipleFromCount(matrices, submitNodeCollector, light, stackState, random, box);
   }

   @Unique
   private static boolean daamky$isItemPhysicsEnabled() {
      ItemPhysics itemPhysics = ItemPhysics.getInstance();
      return itemPhysics != null && itemPhysics.isEnabled();
   }

   @Unique
   private static boolean daamky$resolveGroundState(ItemEntity entity, boolean onGround) {
      int entityId = entity.getId();
      if (onGround) {
         daamky$groundHoldMap.put(entityId, 8);
         daamky$trimGroundMap();
         return true;
      } else {
         Integer holdTicks = daamky$groundHoldMap.get(entityId);
         if (holdTicks != null && holdTicks > 0) {
            Vec3 velocity = entity.getDeltaMovement();
            boolean lowMotion = Math.abs(velocity.x) <= 0.08 && Math.abs(velocity.y) <= 0.08 && Math.abs(velocity.z) <= 0.08;
            if (!lowMotion) {
               daamky$groundHoldMap.remove(entityId);
               return false;
            } else {
               if (holdTicks == 1) {
                  daamky$groundHoldMap.remove(entityId);
               } else {
                  daamky$groundHoldMap.put(entityId, holdTicks - 1);
               }

               return true;
            }
         } else {
            return false;
         }
      }
   }

   @Unique
   private static void daamky$trimGroundMap() {
      if (daamky$groundHoldMap.size() > 4096) {
         daamky$groundHoldMap.clear();
      }
   }
}
