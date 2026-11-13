package daamky.ua.mixin;

import java.util.HashMap;
import java.util.WeakHashMap;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
      method = {"updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
      daamky$groundStateMap.put(state, daamky$resolveGroundState(entity, entity.isOnGround()));
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V",
         ordinal = 0
      ),
      require = 0
   )
   private void daamky$translate(
      MatrixStack matrices,
      float x,
      float y,
      float z,
      ItemEntityRenderState state,
      MatrixStack matricesArg,
      OrderedRenderCommandQueue submitNodeCollector,
      CameraRenderState cameraRenderState
   ) {
      this.daamky$currentState = state;
      boolean enabled = daamky$isItemPhysicsEnabled();
      boolean stableGround = daamky$groundStateMap.getOrDefault(state, Boolean.valueOf(false));
      if (!enabled && !stableGround) {
         matrices.translate(x, y, z);
      } else {
         Box box = state.itemRenderState.getModelBoundingBox();
         matrices.translate(x, -((float)box.minY) + 0.0625F, z);
      }
   }

   @Redirect(
      method = {"render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/entity/ItemEntityRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/ItemStackEntityRenderState;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/Box;)V"
      ),
      require = 0
   )
   private void daamky$renderPhysics(
      MatrixStack matrices, OrderedRenderCommandQueue submitNodeCollector, int light, ItemStackEntityRenderState stackState, Random random, Box box
   ) {
      if (daamky$isItemPhysicsEnabled() && this.daamky$currentState != null) {
         float age = this.daamky$currentState.age;
         float offset = this.daamky$currentState.uniqueOffset;
         boolean onGround = daamky$groundStateMap.getOrDefault(this.daamky$currentState, Boolean.valueOf(false));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-ItemEntity.getRotation(age, offset)));
         if (onGround) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
            matrices.translate(0.0F, -((float)box.getLengthY() / 2.0F) + 0.0625F, 0.0F);
         } else {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((age * 15.0F + offset * 360.0F) % 360.0F));
         }
      }

      ItemEntityRenderer.render(matrices, submitNodeCollector, light, stackState, random, box);
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
            Vec3d velocity = entity.getVelocity();
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
