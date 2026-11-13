package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.events.impl.HandAnimationEvent;
import daamky.ua.api.events.impl.HandOffsetEvent;
import daamky.ua.api.events.impl.HeldItemUpdateEvent;
import daamky.ua.api.events.impl.ItemRendererEvent;
import daamky.ua.manager.Manager;
import daamky.ua.utils.render.hands.ShaderHandMaterialRenderer;

@Mixin(
   value = {HeldItemRenderer.class},
   priority = 900
)
public abstract class HeldItemRendererMixin {
   @Unique
   private static final boolean daamky$holdMyItemsLoaded = FabricLoader.getInstance().isModLoaded("hold-my-items");
   @Shadow
   private ItemStack mainHand;
   @Shadow
   private ItemStack offHand;
   @Unique
   private MatrixStack daamky$customSwingMatrices;
   @Unique
   private Hand daamky$customSwingHand;
   @Unique
   private float daamky$customSwingProgress;

   public HeldItemRendererMixin() {
   }

   @Inject(
      method = {"updateHeldItems"},
      at = {@At("TAIL")}
   )
   private void daamky$updateHeldItems(CallbackInfo ci) {
      HeldItemUpdateEvent event = Manager.postEvent(new HeldItemUpdateEvent(this.mainHand, this.offHand));
      if (event.getMainHand() != this.mainHand) {
         this.mainHand = event.getMainHand();
      }

      if (event.getOffHand() != this.offHand) {
         this.offHand = event.getOffHand();
      }
   }

   @WrapOperation(
      method = {"renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/network/ClientPlayerEntity;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;I)V"
      )},
      require = 0
   )
   private void daamky$itemRenderHook(
      HeldItemRenderer instance,
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue nodeCollector,
      int light,
      Operation<Void> original
   ) {
      if (daamky$holdMyItemsLoaded) {
         original.call(new Object[]{instance, player, tickDelta, pitch, hand, swingProgress, item, equipProgress, matrices, nodeCollector, light});
      } else {
         ItemRendererEvent event = Manager.postEvent(new ItemRendererEvent(player, item, hand));
         original.call(
            new Object[]{
               instance, event.getPlayer(), tickDelta, pitch, event.getHand(), swingProgress, event.getStack(), equipProgress, matrices, nodeCollector, light
            }
         );
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/util/math/MatrixStack;push()V",
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void daamky$handOffset(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue nodeCollector,
      int light,
      CallbackInfo ci
   ) {
      if (!daamky$holdMyItemsLoaded && !stack.isEmpty() && !stack.contains(DataComponentTypes.MAP_ID)) {
         HandOffsetEvent event = Manager.postEvent(new HandOffsetEvent(matrices, stack, hand));
         float scale = event.getScale();
         if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
         }
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$beginShaderHandMaterial(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue nodeCollector,
      int light,
      CallbackInfo ci
   ) {
      ShaderHandMaterialRenderer.beginSubmission(player);
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V"
      )},
      require = 0
   )
   private void daamky$baseSwingAnimation(
      HeldItemRenderer instance,
      MatrixStack matrices,
      Arm arm,
      float equipProgress,
      Operation<Void> original,
      @Local(argsOnly = true) AbstractClientPlayerEntity player,
      @Local(argsOnly = true) Hand hand,
      @Local(argsOnly = true,ordinal = 2) float swingProgress
   ) {
      if (daamky$holdMyItemsLoaded) {
         original.call(new Object[]{instance, matrices, arm, equipProgress});
      } else if (player.isUsingItem() && player.getActiveHand() == hand) {
         original.call(new Object[]{instance, matrices, arm, equipProgress});
      } else {
         HandAnimationEvent event = Manager.postEvent(new HandAnimationEvent(matrices, hand, swingProgress));
         if (event.isCancelled()) {
            this.daamky$markCustomSwing(matrices, hand, swingProgress);
         } else {
            original.call(new Object[]{instance, matrices, arm, equipProgress});
         }
      }
   }

   @WrapOperation(
      method = {"renderFirstPersonItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/render/item/HeldItemRenderer;swingArm(FLnet/minecraft/client/util/math/MatrixStack;ILnet/minecraft/util/Arm;)V"
      )},
      require = 0
   )
   private void daamky$swingAnimation(
      HeldItemRenderer instance,
      float swingProgress,
      MatrixStack matrices,
      int armX,
      Arm arm,
      Operation<Void> original,
      @Local(argsOnly = true) AbstractClientPlayerEntity player,
      @Local(argsOnly = true) Hand hand
   ) {
      if (daamky$holdMyItemsLoaded) {
         original.call(new Object[]{instance, swingProgress, matrices, armX, arm});
      } else if (player.isUsingItem() && player.getActiveHand() == hand) {
         original.call(new Object[]{instance, swingProgress, matrices, armX, arm});
      } else if (!this.daamky$consumeCustomSwing(matrices, hand, swingProgress)) {
         HandAnimationEvent event = Manager.postEvent(new HandAnimationEvent(matrices, hand, swingProgress));
         if (!event.isCancelled()) {
            original.call(new Object[]{instance, swingProgress, matrices, armX, arm});
         }
      }
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("TAIL")},
      require = 0
   )
   private void daamky$clearSwingAnimation(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue nodeCollector,
      int light,
      CallbackInfo ci
   ) {
      ShaderHandMaterialRenderer.endSubmission();
      this.daamky$clearCustomSwing();
   }

   @Unique
   private void daamky$markCustomSwing(MatrixStack matrices, Hand hand, float swingProgress) {
      this.daamky$customSwingMatrices = matrices;
      this.daamky$customSwingHand = hand;
      this.daamky$customSwingProgress = swingProgress;
   }

   @Unique
   private boolean daamky$consumeCustomSwing(MatrixStack matrices, Hand hand, float swingProgress) {
      boolean matches = this.daamky$customSwingMatrices == matrices
         && this.daamky$customSwingHand == hand
         && Float.compare(this.daamky$customSwingProgress, swingProgress) == 0;
      if (matches) {
         this.daamky$clearCustomSwing();
      }

      return matches;
   }

   @Unique
   private void daamky$clearCustomSwing() {
      this.daamky$customSwingMatrices = null;
      this.daamky$customSwingHand = null;
      this.daamky$customSwingProgress = 0.0F;
   }
}
