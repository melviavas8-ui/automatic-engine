package daamky.ua.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import daamky.ua.api.events.impl.AttackEvent;
import daamky.ua.api.events.impl.BlockBreakingEvent;
import daamky.ua.api.events.impl.ClickSlotEvent;
import daamky.ua.api.events.impl.InteractEntityEvent;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.player.AntiDrop;
import daamky.ua.manager.Manager;

@Mixin({ClientPlayerInteractionManager.class})
public abstract class MultiPlayerGameModeMixin {
   public MultiPlayerGameModeMixin() {
   }

   @Inject(
      method = {"clickSlot"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$clickSlotHook(int containerId, int slotId, int button, SlotActionType clickType, PlayerEntity player, CallbackInfo ci) {
      ItemStack droppedStack = ItemStack.EMPTY;
      if (clickType == SlotActionType.THROW && slotId >= 0 && slotId < player.currentScreenHandler.slots.size()) {
         droppedStack = player.currentScreenHandler.getSlot(slotId).getStack();
      }

      if (clickType == SlotActionType.PICKUP && slotId == -999) {
         droppedStack = player.currentScreenHandler.getCursorStack();
      }

      if (AntiDrop.shouldBlock(droppedStack)) {
         ci.cancel();
      } else {
         ClickSlotEvent event = Manager.postEvent(new ClickSlotEvent(containerId, slotId, button, clickType));
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }

   @Inject(
      method = {"dropCreativeStack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$preventCreativeDrop(ItemStack stack, CallbackInfo ci) {
      if (AntiDrop.shouldBlock(stack)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"attackBlock"},
      at = {@At("HEAD")}
   )
   private void daamky$startDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      Manager.postEvent(new BlockBreakingEvent(pos, direction));
   }

   @Inject(
      method = {"updateBlockBreakingProgress"},
      at = {@At("HEAD")}
   )
   private void daamky$continueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      Manager.postEvent(new BlockBreakingEvent(pos, direction));
   }

   @Inject(
      method = {"attackEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
      AttackEvent attackEvent = Manager.postEvent(new AttackEvent(target));
      InteractEntityEvent interactEvent = Manager.postEvent(new InteractEntityEvent(target));
      if (attackEvent.isCancelled() || interactEvent.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"interactItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$cancelShieldUse(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      AuraModule aura = AuraModule.getInstance();
      if (aura != null && aura.shouldCancelInteractItem(hand)) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"interactBlock"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$cancelUseItemOn(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
      AuraModule aura = AuraModule.getInstance();
      ModuleManager modules = Manager.getModules();
      boolean noInteract = modules != null && modules.isNoInteractEnabled();
      if (noInteract || aura != null && aura.shouldCancelInteractBlock()) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }

   @Inject(
      method = {"interactEntity"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$cancelInteract(PlayerEntity player, Entity target, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
      AuraModule aura = AuraModule.getInstance();
      InteractEntityEvent event = Manager.postEvent(new InteractEntityEvent(target));
      ModuleManager modules = Manager.getModules();
      boolean noInteract = modules != null && modules.isNoInteractEnabled();
      if (event.isCancelled() || noInteract || aura != null && aura.shouldCancelEntityInteraction()) {
         cir.setReturnValue(ActionResult.PASS);
      }
   }
}
