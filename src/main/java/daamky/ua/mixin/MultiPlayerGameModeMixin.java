package daamky.ua.mixin;

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
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

@Mixin({MultiPlayerGameMode.class})
public abstract class MultiPlayerGameModeMixin {
   public MultiPlayerGameModeMixin() {
   }

   @Inject(
      method = {"handleInventoryMouseClick"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$clickSlotHook(int containerId, int slotId, int button, ClickType clickType, Player player, CallbackInfo ci) {
      ItemStack droppedStack = ItemStack.EMPTY;
      if (clickType == ClickType.THROW && slotId >= 0 && slotId < player.containerMenu.slots.size()) {
         droppedStack = player.containerMenu.getSlot(slotId).getItem();
      }

      if (clickType == ClickType.PICKUP && slotId == -999) {
         droppedStack = player.containerMenu.getCarried();
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
      method = {"handleCreativeModeItemDrop"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$preventCreativeDrop(ItemStack stack, CallbackInfo ci) {
      if (AntiDrop.shouldBlock(stack)) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"startDestroyBlock"},
      at = {@At("HEAD")}
   )
   private void daamky$startDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      Manager.postEvent(new BlockBreakingEvent(pos, direction));
   }

   @Inject(
      method = {"continueDestroyBlock"},
      at = {@At("HEAD")}
   )
   private void daamky$continueDestroyBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      Manager.postEvent(new BlockBreakingEvent(pos, direction));
   }

   @Inject(
      method = {"attack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$onAttack(Player player, Entity target, CallbackInfo ci) {
      AttackEvent attackEvent = Manager.postEvent(new AttackEvent(target));
      InteractEntityEvent interactEvent = Manager.postEvent(new InteractEntityEvent(target));
      if (attackEvent.isCancelled() || interactEvent.isCancelled()) {
         ci.cancel();
      }
   }

   @Inject(
      method = {"useItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$cancelShieldUse(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      AuraModule aura = AuraModule.getInstance();
      if (aura != null && aura.shouldCancelInteractItem(hand)) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$cancelUseItemOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
      AuraModule aura = AuraModule.getInstance();
      ModuleManager modules = Manager.getModules();
      boolean noInteract = modules != null && modules.isNoInteractEnabled();
      if (noInteract || aura != null && aura.shouldCancelInteractBlock()) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }

   @Inject(
      method = {"interact"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void daamky$cancelInteract(Player player, Entity target, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
      AuraModule aura = AuraModule.getInstance();
      InteractEntityEvent event = Manager.postEvent(new InteractEntityEvent(target));
      ModuleManager modules = Manager.getModules();
      boolean noInteract = modules != null && modules.isNoInteractEnabled();
      if (event.isCancelled() || noInteract || aura != null && aura.shouldCancelEntityInteraction()) {
         cir.setReturnValue(InteractionResult.PASS);
      }
   }
}
