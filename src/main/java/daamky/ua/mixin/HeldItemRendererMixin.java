package daamky.ua.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
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
   value = {ItemInHandRenderer.class},
   priority = 900
)
public abstract class HeldItemRendererMixin {
   @Unique
   private static final boolean daamky$holdMyItemsLoaded = FabricLoader.getInstance().isModLoaded("hold-my-items");
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   private ItemStack offHandItem;
   @Unique
   private PoseStack daamky$customSwingMatrices;
   @Unique
   private InteractionHand daamky$customSwingHand;
   @Unique
   private float daamky$customSwingProgress;

   public HeldItemRendererMixin() {
   }

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   private void daamky$updateHeldItems(CallbackInfo ci) {
      HeldItemUpdateEvent event = Manager.postEvent(new HeldItemUpdateEvent(this.mainHandItem, this.offHandItem));
      if (event.getMainHand() != this.mainHandItem) {
         this.mainHandItem = event.getMainHand();
      }

      if (event.getOffHand() != this.offHandItem) {
         this.offHandItem = event.getOffHand();
      }
   }

   @WrapOperation(
      method = {"renderHandsWithItems(FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/player/LocalPlayer;I)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V"
      )},
      require = 0
   )
   private void daamky$itemRenderHook(
      ItemInHandRenderer instance,
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector nodeCollector,
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
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
         shift = Shift.AFTER
      )},
      require = 0
   )
   private void daamky$handOffset(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector nodeCollector,
      int light,
      CallbackInfo ci
   ) {
      if (!daamky$holdMyItemsLoaded && !stack.isEmpty() && !stack.has(DataComponents.MAP_ID)) {
         HandOffsetEvent event = Manager.postEvent(new HandOffsetEvent(matrices, stack, hand));
         float scale = event.getScale();
         if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
         }
      }
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("HEAD")},
      require = 0
   )
   private void daamky$beginShaderHandMaterial(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector nodeCollector,
      int light,
      CallbackInfo ci
   ) {
      ShaderHandMaterialRenderer.beginSubmission(player);
   }

   @WrapOperation(
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"
      )},
      require = 0
   )
   private void daamky$baseSwingAnimation(
      ItemInHandRenderer instance,
      PoseStack matrices,
      HumanoidArm arm,
      float equipProgress,
      Operation<Void> original,
      @Local(argsOnly = true) AbstractClientPlayer player,
      @Local(argsOnly = true) InteractionHand hand,
      @Local(argsOnly = true,ordinal = 2) float swingProgress
   ) {
      if (daamky$holdMyItemsLoaded) {
         original.call(new Object[]{instance, matrices, arm, equipProgress});
      } else if (player.isUsingItem() && player.getUsedItemHand() == hand) {
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
      method = {"renderArmWithItem"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"
      )},
      require = 0
   )
   private void daamky$swingAnimation(
      ItemInHandRenderer instance,
      float swingProgress,
      PoseStack matrices,
      int armX,
      HumanoidArm arm,
      Operation<Void> original,
      @Local(argsOnly = true) AbstractClientPlayer player,
      @Local(argsOnly = true) InteractionHand hand
   ) {
      if (daamky$holdMyItemsLoaded) {
         original.call(new Object[]{instance, swingProgress, matrices, armX, arm});
      } else if (player.isUsingItem() && player.getUsedItemHand() == hand) {
         original.call(new Object[]{instance, swingProgress, matrices, armX, arm});
      } else if (!this.daamky$consumeCustomSwing(matrices, hand, swingProgress)) {
         HandAnimationEvent event = Manager.postEvent(new HandAnimationEvent(matrices, hand, swingProgress));
         if (!event.isCancelled()) {
            original.call(new Object[]{instance, swingProgress, matrices, armX, arm});
         }
      }
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("TAIL")},
      require = 0
   )
   private void daamky$clearSwingAnimation(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack stack,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector nodeCollector,
      int light,
      CallbackInfo ci
   ) {
      ShaderHandMaterialRenderer.endSubmission();
      this.daamky$clearCustomSwing();
   }

   @Unique
   private void daamky$markCustomSwing(PoseStack matrices, InteractionHand hand, float swingProgress) {
      this.daamky$customSwingMatrices = matrices;
      this.daamky$customSwingHand = hand;
      this.daamky$customSwingProgress = swingProgress;
   }

   @Unique
   private boolean daamky$consumeCustomSwing(PoseStack matrices, InteractionHand hand, float swingProgress) {
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
