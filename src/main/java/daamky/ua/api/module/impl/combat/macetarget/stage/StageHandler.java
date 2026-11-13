package daamky.ua.api.module.impl.combat.macetarget.stage;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.module.impl.combat.macetarget.armor.ArmorSwapHandler;
import daamky.ua.api.module.impl.combat.macetarget.armor.FireworkHandler;
import daamky.ua.api.module.impl.combat.macetarget.attack.AttackHandler;
import daamky.ua.api.module.impl.combat.macetarget.state.MaceState;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public class StageHandler {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
   private final ArmorSwapHandler armorSwapHandler;
   private final FireworkHandler fireworkHandler;
   private final AttackHandler attackHandler;
   private final StopWatch fireworkTimer;
   private MaceState.Stage stage = MaceState.Stage.PREPARE;
   private boolean silentMode = true;
   private boolean reallyWorldMode;
   private float height = 30.0F;
   private float minAttackFallDistance = 3.0F;
   private float maxAttackFallDistance = 4.5F;
   private boolean attackedThisFall;

   public StageHandler(ArmorSwapHandler armorSwapHandler, FireworkHandler fireworkHandler, AttackHandler attackHandler, StopWatch fireworkTimer) {
      this.armorSwapHandler = armorSwapHandler;
      this.fireworkHandler = fireworkHandler;
      this.attackHandler = attackHandler;
      this.fireworkTimer = fireworkTimer;
   }

   public MaceState.Stage getStage() {
      return this.stage;
   }

   public void setSilentMode(boolean silentMode) {
      this.silentMode = silentMode;
   }

   public void setReallyWorldMode(boolean reallyWorldMode) {
      this.reallyWorldMode = reallyWorldMode;
   }

   public void setHeight(float height) {
      this.height = height;
   }

   public void setAttackFallDistance(float minAttackFallDistance, float maxAttackFallDistance) {
      this.minAttackFallDistance = minAttackFallDistance;
      this.maxAttackFallDistance = maxAttackFallDistance;
   }

   public void handlePrepare(boolean hasElytra) {
      if (!hasElytra) {
         int slot = InventoryUtils.findElytraSlot();
         if (slot != -1) {
            this.armorSwapHandler.startSwap(slot, this.silentMode);
         }
      } else {
         this.stage = MaceState.Stage.FLYING_UP;
         this.fireworkTimer.reset();
      }
   }

   public void handleFlyingUp(LivingEntity target, boolean hasElytra) {
      if (MC.player != null) {
         if (!hasElytra) {
            this.stage = MaceState.Stage.PREPARE;
         } else {
            if (MC.player.isGliding() && this.fireworkTimer.finished(300.0)) {
               this.fireworkHandler.useFirework(this.silentMode);
               this.fireworkTimer.reset();
            }

            if (MC.player.getY() - target.getY() >= (double)this.height) {
               this.stage = MaceState.Stage.TARGETTING;
            }
         }
      }
   }

   public void handleTargetting(LivingEntity target) {
      if (MC.player != null) {
         if (InventoryUtils.hasElytra() && MC.player.distanceTo(target) < 12.0F && !this.armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
               this.armorSwapHandler.startSwap(slot, this.silentMode);
            }
         }

         if (MC.player.distanceTo(target) < 16.0F) {
            this.stage = MaceState.Stage.ATTACKING;
         }
      }
   }

   public void handleAttacking(LivingEntity target, boolean hasElytra) {
      if (MC.player != null) {
         if (hasElytra && !this.armorSwapHandler.isActive()) {
            int slot = InventoryUtils.findChestArmorSlot();
            if (slot != -1) {
               this.armorSwapHandler.startSwap(slot, this.silentMode);
            }
         } else {
            if (!hasElytra
               && !this.armorSwapHandler.isActive()
               && this.attackHandler.hasMaceInHotbar()
               && MC.player.distanceTo(target) < 5.0F
               && this.isLandingMaceWindow()) {
               this.attackHandler.setPendingAttack(true);
               if (this.reallyWorldMode) {
                  this.attackHandler.setShouldDisableAfterAttack(true);
               } else {
                  this.stage = MaceState.Stage.FLYING_UP;
                  this.fireworkTimer.reset();
               }
            }
         }
      }
   }

   public void handleFallAttack(LivingEntity target) {
      if (MC.player != null) {
         if (!MC.player.isOnGround()) {
            if (this.attackHandler.hasMaceInHotbar() && this.isLandingMaceWindow()) {
               this.attackHandler.prepareMaceSwap();
            }
         }
      }
   }

   private boolean isLandingMaceWindow() {
      if (MC.player != null && !MC.player.isOnGround()) {
         double fallDistance = MC.player.fallDistance;
         return fallDistance >= (double)this.minAttackFallDistance && MC.player.getVelocity().y < -0.08;
      } else {
         return false;
      }
   }

   public void reset() {
      this.stage = MaceState.Stage.PREPARE;
      this.resetFallAttack();
   }

   public void resetFallAttack() {
      this.attackedThisFall = false;
   }

   public void markFallAttackConsumed() {
      this.attackedThisFall = true;
   }
}
