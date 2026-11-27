package daamky.ua.mixin;

import java.util.Random;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.visual.HolMyItems;

@Mixin({ItemInHandRenderer.class})
public abstract class HeldItemRendererHmiMixin {
   @Unique
   private boolean hmi$repPower = false;
   @Unique
   private float hmi$prevAge = 0.0F;
   @Unique
   private double hmi$previousRotation = 0.0;
   @Unique
   private float hmi$swingAngleY = 0.0F;
   @Unique
   private float hmi$swingAngleX = 0.0F;
   @Unique
   private float hmi$swingVelocityY = 0.0F;
   @Unique
   private float hmi$swingVelocityX = 0.0F;
   @Unique
   private float hmi$swingVelocityZ = 0.0F;
   @Unique
   private float hmi$vertAngleY = 0.0F;
   @Unique
   private float hmi$vertVelocityY = 0.0F;
   @Unique
   private float hmi$vertVelocityYSlime = 0.0F;
   @Unique
   private float hmi$vertAngleYSlime = 0.0F;
   @Unique
   private float hmi$riptideCounter = 0.0F;
   @Unique
   private float hmi$netherCounter = 0.0F;
   @Unique
   private float hmi$fallCounter = 0.0F;
   @Unique
   private float hmi$inWaterCounter = 0.0F;
   @Unique
   private float hmi$inspect = 0.0F;
   @Unique
   private float hmi$tilt = 0.0F;
   @Unique
   private float hmi$freezeCounter = 0.0F;
   @Unique
   private float hmi$clCount = 0.0F;
   @Unique
   private float hmi$crawlCount = 0.0F;
   @Unique
   private float hmi$directionalCrawlCount = 0.0F;
   @Unique
   private float hmi$climbCount = 0.0F;
   @Unique
   private float hmi$mouseHolding = 1.0F;
   @Unique
   private boolean hmi$isSwinging = false;
   @Unique
   private float hmi$swingProgress = 0.0F;
   @Unique
   private boolean hmi$isForward = false;
   @Unique
   private boolean hmi$isAttacking = false;
   @Unique
   private boolean hmi$left = false;
   @Unique
   private int hmi$reallyVariant = 0;
   @Unique
   private float hmi$lastMainSwordSwing = 0.0F;
   @Unique
   private float hmi$lastOffSwordSwing = 0.0F;
   @Unique
   private double hmi$mainSwordSwingStart = -1.0;
   @Unique
   private double hmi$offSwordSwingStart = -1.0;
   @Unique
   private boolean hmi$mainSwordSwingActive = false;
   @Unique
   private boolean hmi$offSwordSwingActive = false;
   @Unique
   private final float[] hmi$bowDraw = new float[2];
   @Unique
   private final float[] hmi$bowHold = new float[2];
   @Unique
   private final float[] hmi$bowWiggle = new float[2];
   @Unique
   private final float[] hmi$crossbowDraw = new float[2];
   @Unique
   private final float[] hmi$crossbowHold = new float[2];
   @Unique
   private final float[] hmi$tridentDraw = new float[2];
   @Unique
   private final float[] hmi$tridentLift = new float[2];
   @Unique
   private final float[] hmi$tridentJiggle = new float[2];
   @Unique
   private final float[] hmi$shieldRaise = new float[2];
   @Unique
   private final float[] hmi$foodDraw = new float[2];
   @Unique
   private final float[] hmi$foodChew = new float[2];
   @Unique
   private final float[] hmi$drinkDraw = new float[2];
   @Unique
   private final float[] hmi$brushDraw = new float[2];
   @Unique
   private final float[] hmi$rodCast = new float[2];
   @Shadow
   private ItemStack mainHandItem;
   @Shadow
   @Final
   private Minecraft minecraft;

   public HeldItemRendererHmiMixin() {
   }

   @Shadow
   protected abstract void renderMapHand(PoseStack var1, SubmitNodeCollector var2, int var3, HumanoidArm var4);

   @Shadow
   protected abstract void renderPlayerArm(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, HumanoidArm var6);

   @Shadow
   protected abstract void renderOneHandedMap(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, HumanoidArm var5, float var6, ItemStack var7);

   @Shadow
   protected abstract void renderTwoHandedMap(PoseStack var1, SubmitNodeCollector var2, int var3, float var4, float var5, float var6);

   @Shadow
   protected abstract void applyItemArmTransform(PoseStack var1, HumanoidArm var2, float var3);

   @Shadow
   protected abstract void swingArm(float var1, PoseStack var2, int var3, HumanoidArm var4);

   @Shadow
   public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, PoseStack var4, SubmitNodeCollector var5, int var6);

   @Unique
   private float hmiEaseInOutBack(float x) {
      float c1 = 1.70158F;
      float c2 = c1 * 1.525F;
      return (float)(
         (double)x < 0.5
            ? Math.pow(2.0 * (double)x, 2.0) * (double)((c2 + 1.0F) * 2.0F * x - c2) / 2.0
            : (Math.pow(2.0 * (double)x - 2.0, 2.0) * (double)((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0
      );
   }

   @Unique
   private float hmiGetAttackDamage(ItemStack stack) {
      ItemAttributeModifiers modifiers = (ItemAttributeModifiers)stack.getComponents().get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0F;
      } else {
         float totalDamage = 0.0F;

         for (Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == Attributes.ATTACK_DAMAGE.value()) {
               totalDamage += (float)entry.modifier().amount();
            }
         }

         return totalDamage;
      }
   }

   @Unique
   private double hmiSwingDurationTicks(HolMyItems config) {
      if (config == null) {
         return 10.0;
      } else {
         String var2 = config.hmiAnimationType.getValue();

         return switch (var2) {
            case "Sharp" -> 8.0;
            case "Spin" -> 12.0;
            case "Heavy" -> 14.0;
            case "Stab" -> 7.0;
            case "Cross" -> 11.0;
            default -> 10.0;
         };
      }
   }

   @Unique
   private void hmiUpdateUseCounters(AbstractClientPlayer player, ItemStack item, InteractionHand hand, double tt) {
      int h = hand == InteractionHand.MAIN_HAND ? 0 : 1;
      float step = (float)tt;
      boolean using = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
      ItemUseAnimation use = item.getUseAnimation();
      boolean drawingBow = using && use == ItemUseAnimation.BOW;
      this.hmi$bowDraw[h] = Mth.clamp(this.hmi$bowDraw[h] + (drawingBow ? 0.075F : -0.07F) * step, 0.0F, 1.0F);
      if (drawingBow && this.hmi$bowDraw[h] >= 1.0F) {
         this.hmi$bowHold[h] = Mth.clamp(this.hmi$bowHold[h] + 0.025F * step, 0.0F, 1.0F);
         this.hmi$bowWiggle[h] = this.hmi$bowWiggle[h] + 0.07F * step;
      } else {
         this.hmi$bowHold[h] = Mth.clamp(this.hmi$bowHold[h] - 0.11F * step, 0.0F, 1.0F);
         if (this.hmi$bowHold[h] <= 0.0F) {
            this.hmi$bowWiggle[h] = 0.0F;
         }
      }

      boolean chargingCrossbow = using && use == ItemUseAnimation.CROSSBOW;
      this.hmi$crossbowDraw[h] = Mth.clamp(this.hmi$crossbowDraw[h] + (chargingCrossbow ? 0.1F : -0.1F) * step, 0.0F, 1.0F);
      this.hmi$crossbowHold[h] = Mth.clamp(
         this.hmi$crossbowHold[h] + (chargingCrossbow && this.hmi$crossbowDraw[h] >= 1.0F ? 0.02F : -0.1F) * step, 0.0F, 1.0F
      );
      boolean chargingSpear = using && use == ItemUseAnimation.SPEAR;
      if (chargingSpear) {
         this.hmi$tridentDraw[h] = Mth.clamp(this.hmi$tridentDraw[h] + 0.07F * step, 0.0F, 1.0F);
         this.hmi$tridentLift[h] = Mth.clamp(this.hmi$tridentLift[h] + 0.05F * step, 0.0F, 1.0F);
         this.hmi$tridentJiggle[h] = this.hmi$tridentJiggle[h] + 0.1F * step;
      } else {
         float release = player.isAutoSpinAttack() ? 0.57F : 0.1F;
         float liftRelease = player.isAutoSpinAttack() ? 0.53F : 0.07F;
         this.hmi$tridentDraw[h] = Mth.clamp(this.hmi$tridentDraw[h] - release * step, 0.0F, 1.0F);
         this.hmi$tridentLift[h] = Mth.clamp(this.hmi$tridentLift[h] - liftRelease * step, 0.0F, 1.0F);
         this.hmi$tridentJiggle[h] = Math.max(0.0F, this.hmi$tridentJiggle[h] - 0.1F * step);
      }

      this.hmi$tridentLift[h] = this.hmi$tridentLift[h] * (float)Math.pow(0.95, tt);
      float shieldRate = item.is(ItemTags.SWORDS) ? 0.12F : 0.07F;
      this.hmi$shieldRaise[h] = Mth.clamp(this.hmi$shieldRaise[h] + (using && use == ItemUseAnimation.BLOCK ? shieldRate : -shieldRate) * step, 0.0F, 1.0F);
      boolean consuming = using && (use == ItemUseAnimation.EAT || use == ItemUseAnimation.DRINK || use == ItemUseAnimation.TOOT_HORN);
      this.hmi$foodDraw[h] = Mth.clamp(this.hmi$foodDraw[h] + (consuming ? 0.1F : -0.1F) * step, 0.0F, 1.0F);
      this.hmi$brushDraw[h] = Mth.clamp(this.hmi$brushDraw[h] + (using && use == ItemUseAnimation.BRUSH ? 0.1F : -0.1F) * step, 0.0F, 1.0F);
      if ((!consuming || !(this.hmi$foodDraw[h] >= 1.0F)) && !(this.hmi$brushDraw[h] > 0.4F)) {
         if (this.hmi$foodDraw[h] <= 0.0F && this.hmi$brushDraw[h] <= 0.0F) {
            this.hmi$foodChew[h] = 0.0F;
         }
      } else {
         this.hmi$foodChew[h] = this.hmi$foodChew[h] + 0.1F * step;
      }

      this.hmi$drinkDraw[h] = Mth.clamp(
         this.hmi$drinkDraw[h] + (using && use == ItemUseAnimation.DRINK && this.hmi$foodDraw[h] >= 1.0F ? 0.04F : -0.1F) * step, 0.0F, 1.0F
      );
      boolean bobberOut = item.is(Items.FISHING_ROD) && player.fishing != null;
      this.hmi$rodCast[h] = Mth.clamp(this.hmi$rodCast[h] + (bobberOut ? 0.12F : -0.12F) * step, 0.0F, 1.0F);
   }

   @Unique
   private boolean hmiHasResidualUseMotion(int h, ItemStack item) {
      return switch (item.getUseAnimation()) {
         case BOW -> this.hmi$bowDraw[h] > 0.002F;
         case SPEAR -> this.hmi$tridentDraw[h] > 0.002F;
         case BLOCK -> this.hmi$shieldRaise[h] > 0.002F;
         case EAT, DRINK, TOOT_HORN -> this.hmi$foodDraw[h] > 0.002F;
         case BRUSH -> this.hmi$brushDraw[h] > 0.002F;
         default -> false;
      };
   }

   @Unique
   private float hmiVisualYaw(AbstractClientPlayer player) {
      Angle angle = AngleConnection.INSTANCE.getCurrentAngle();
      return angle == null ? player.getYRot() : angle.getYaw();
   }

   @Unique
   private float hmiVisualPitch(AbstractClientPlayer player) {
      Angle angle = AngleConnection.INSTANCE.getCurrentAngle();
      return angle == null ? player.getXRot() : angle.getPitch();
   }

   @Unique
   private void hmiAltSwing(PoseStack matrices, HumanoidArm arm, float swingProgress, ItemStack item) {
      int i = arm == HumanoidArm.RIGHT ? 1 : -1;
      float f = Mth.sin((double)(swingProgress * 3.14F));
      matrices.mulPose(Axis.YP.rotationDegrees((float)i * (45.0F + f * 0.0F)));
      matrices.mulPose(Axis.YP.rotationDegrees((float)i * -45.0F));
   }

   @Unique
   private boolean hmiIsCompactThrowable(ItemStack item) {
      return item.is(Items.EGG)
         || item.is(Items.SNOWBALL)
         || item.is(Items.ENDER_EYE)
         || item.is(Items.EXPERIENCE_BOTTLE)
         || item.is(Items.WIND_CHARGE)
         || item.getItem() instanceof SplashPotionItem
         || item.getItem() instanceof LingeringPotionItem;
   }

   @Unique
   private float hmiResolveSwordProgress(AbstractClientPlayer player, HolMyItems config, float tickDelta, InteractionHand hand, float vanillaProgress) {
      float current = Mth.clamp(vanillaProgress, 0.0F, 1.0F);
      float previous = hand == InteractionHand.MAIN_HAND ? this.hmi$lastMainSwordSwing : this.hmi$lastOffSwordSwing;
      boolean active = hand == InteractionHand.MAIN_HAND ? this.hmi$mainSwordSwingActive : this.hmi$offSwordSwingActive;
      double start = hand == InteractionHand.MAIN_HAND ? this.hmi$mainSwordSwingStart : this.hmi$offSwordSwingStart;
      double now = (double)player.tickCount + (double)tickDelta;
      boolean newSwing = current > 0.001F && (previous <= 0.001F || current + 0.15F < previous);
      if (newSwing) {
         start = now;
         active = true;
         if (hand == InteractionHand.MAIN_HAND) {
            this.hmi$reallyVariant = 1 - this.hmi$reallyVariant;
         }
      }

      float result = 0.0F;
      if (active) {
         result = Mth.clamp((float)((now - start) / this.hmiSwingDurationTicks(config)), 0.0F, 1.0F);
         if (result >= 1.0F) {
            active = false;
         }
      }

      if (hand == InteractionHand.MAIN_HAND) {
         this.hmi$lastMainSwordSwing = current;
         this.hmi$mainSwordSwingStart = start;
         this.hmi$mainSwordSwingActive = active;
      } else {
         this.hmi$lastOffSwordSwing = current;
         this.hmi$offSwordSwingStart = start;
         this.hmi$offSwordSwingActive = active;
      }

      return result;
   }

   @Unique
   private float hmiEaseSwordSwing(float value) {
      float t = Mth.clamp(value, 0.0F, 1.0F);
      float t2 = t * t;
      float t3 = t2 * t;
      float inverse = 1.0F - t;
      return 3.0F * t * inverse * inverse * 0.44F + 3.0F * t2 * inverse * 0.94F + t3;
   }

   @Unique
   private void hmiRotateXAroundHand(PoseStack matrices, float degrees, float sideFactor) {
      this.hmiRotateXAt(matrices, degrees, 0.3F * sideFactor, -0.4F, 0.0F);
   }

   @Unique
   private void hmiRotateZAroundHand(PoseStack matrices, float degrees, float sideFactor) {
      this.hmiRotateZAt(matrices, degrees, 0.3F * sideFactor, -0.4F, 0.0F);
   }

   @Unique
   private void hmiRotateXAt(PoseStack matrices, float degrees, float pivotX, float pivotY, float pivotZ) {
      matrices.translate(pivotX, pivotY, pivotZ);
      matrices.mulPose(Axis.XP.rotationDegrees(degrees));
      matrices.translate(-pivotX, -pivotY, -pivotZ);
   }

   @Unique
   private void hmiRotateYAt(PoseStack matrices, float degrees, float pivotX, float pivotY, float pivotZ) {
      matrices.translate(pivotX, pivotY, pivotZ);
      matrices.mulPose(Axis.YP.rotationDegrees(degrees));
      matrices.translate(-pivotX, -pivotY, -pivotZ);
   }

   @Unique
   private void hmiRotateZAt(PoseStack matrices, float degrees, float pivotX, float pivotY, float pivotZ) {
      matrices.translate(pivotX, pivotY, pivotZ);
      matrices.mulPose(Axis.ZP.rotationDegrees(degrees));
      matrices.translate(-pivotX, -pivotY, -pivotZ);
   }

   @Unique
   private float hmiSwordSwingRot(float progress) {
      float value = progress < 0.70016F
         ? Mth.sin((double)(Mth.clamp(progress, 0.0F, 0.308F) * 5.1F))
         : Mth.sin((double)(Mth.clamp(progress, 0.70016F, 1.0F) * 5.1F - 2.0F));
      return value * value * value;
   }

   @Unique
   private float hmiSwordTilt(float progress) {
      return progress < 0.65245F
         ? Mth.sin((double)(Mth.clamp(progress, 0.0F, 0.16675F) * 3.14F * 3.0F))
         : Mth.sin((double)(Mth.clamp(progress, 0.65245F, 1.0F) * 4.4F - 1.2584F));
   }

   @Unique
   private float hmiSwordHit(float progress) {
      return Mth.sin((double)(Mth.clamp(progress, 0.16561F, 0.49422F) * 4.78F * 2.0F + 4.7F));
   }

   @Unique
   private float hmiSwordSecondHit(float progress) {
      return progress < 0.65594F
         ? Mth.sin((double)(Mth.clamp(progress, 0.16561F, 0.32991F) * 4.78F * 2.0F + 4.7F))
         : Mth.sin((double)(Mth.clamp(progress, 0.65594F, 0.82025F) * 4.78F * 2.0F - 4.7F));
   }

   @Unique
   private float hmiEaseOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float inv = Mth.clamp(x, 0.0F, 1.0F) - 1.0F;
      return 1.0F + c3 * inv * inv * inv + c1 * inv * inv;
   }

   @Unique
   private float hmiEaseInOutSine(float x) {
      return -(Mth.cos((double)((float) Math.PI * Mth.clamp(x, 0.0F, 1.0F))) - 1.0F) / 2.0F;
   }

   @Unique
   private float hmiWindow(float progress, float start, float length) {
      return Mth.clamp((progress - start) / length, 0.0F, 1.0F);
   }

   @Unique
   private float hmiPulse(float progress, float start, float length) {
      return Mth.sin((double)(this.hmiWindow(progress, start, length) * (float) Math.PI));
   }

   @Unique
   private void hmiSharpHand(PoseStack matrices, float progress, float sideFactor) {
      float wind = this.hmiEaseOutBack(this.hmiWindow(progress, 0.0F, 0.2F)) * (1.0F - this.hmiWindow(progress, 0.2F, 0.18F));
      float strike = this.hmiPulse(progress, 0.17F, 0.34F);
      strike *= strike;
      float snap = this.hmiPulse(progress, 0.19F, 0.2F);
      float settle = this.hmiPulse(progress, 0.52F, 0.48F);
      matrices.translate(0.26F * sideFactor * wind, 0.24F * wind, 0.2F * wind);
      this.hmiRotateXAroundHand(matrices, 44.0F * wind, sideFactor);
      this.hmiRotateZAroundHand(matrices, 58.0F * sideFactor * wind, sideFactor);
      this.hmiRotateYAt(matrices, 26.0F * sideFactor * wind, 0.3F * sideFactor, -0.4F, 0.0F);
      matrices.translate(-0.44F * sideFactor * strike, 0.06F * strike, -0.52F * strike);
      this.hmiRotateZAroundHand(matrices, -88.0F * sideFactor * strike, sideFactor);
      this.hmiRotateXAroundHand(matrices, -72.0F * strike, sideFactor);
      this.hmiRotateYAt(matrices, -32.0F * sideFactor * snap, 0.3F * sideFactor, -0.4F, 0.0F);
      matrices.translate(0.0F, -0.07F * settle, 0.13F * settle);
      this.hmiRotateXAroundHand(matrices, 15.0F * settle, sideFactor);
   }

   @Unique
   private void hmiSpinHand(PoseStack matrices, float progress, float sideFactor) {
      float arc = Mth.sin((double)(progress * (float) Math.PI));
      float lift = this.hmiPulse(progress, 0.0F, 0.42F);
      float roll = this.hmiEaseInOutSine(this.hmiWindow(progress, 0.08F, 0.72F));
      matrices.translate(0.22F * sideFactor * lift, 0.3F * lift, 0.12F * lift);
      matrices.translate(-0.32F * sideFactor * arc, 0.12F * arc, -0.52F * arc);
      this.hmiRotateXAroundHand(matrices, 36.0F * lift, sideFactor);
      this.hmiRotateXAroundHand(matrices, -82.0F * arc, sideFactor);
      this.hmiRotateZAroundHand(matrices, -120.0F * sideFactor * roll, sideFactor);
      this.hmiRotateYAt(matrices, 34.0F * sideFactor * arc, 0.3F * sideFactor, -0.4F, 0.0F);
   }

   @Unique
   private void hmiHeavyHand(PoseStack matrices, float progress, float sideFactor) {
      float raise = this.hmiEaseInOutSine(this.hmiWindow(progress, 0.0F, 0.36F)) * (1.0F - this.hmiWindow(progress, 0.4F, 0.2F));
      float chop = this.hmiEaseInOutBack(this.hmiWindow(progress, 0.38F, 0.24F));
      float impact = this.hmiPulse(progress, 0.58F, 0.42F);
      float shake = Mth.sin((double)(progress * 78.5F)) * this.hmiWindow(progress, 0.58F, 0.06F) * (1.0F - this.hmiWindow(progress, 0.64F, 0.28F));
      matrices.translate(0.1F * sideFactor * raise, 0.42F * raise, 0.28F * raise);
      this.hmiRotateXAroundHand(matrices, 62.0F * raise, sideFactor);
      this.hmiRotateZAroundHand(matrices, 18.0F * sideFactor * raise, sideFactor);
      matrices.translate(-0.12F * sideFactor * chop, -0.28F * chop, -0.68F * chop);
      this.hmiRotateXAroundHand(matrices, -108.0F * chop, sideFactor);
      this.hmiRotateZAroundHand(matrices, -12.0F * sideFactor * chop, sideFactor);
      matrices.translate(0.0F, 0.05F * impact, 0.18F * impact);
      this.hmiRotateXAroundHand(matrices, 22.0F * impact + 4.0F * shake, sideFactor);
      this.hmiRotateZAroundHand(matrices, 6.0F * sideFactor * shake, sideFactor);
   }

   @Unique
   private void hmiStabHand(PoseStack matrices, float progress, float sideFactor) {
      float pull = this.hmiEaseOutBack(this.hmiWindow(progress, 0.0F, 0.26F)) * (1.0F - this.hmiWindow(progress, 0.26F, 0.16F));
      float lunge = this.hmiEaseOutBack(this.hmiWindow(progress, 0.24F, 0.2F)) * (1.0F - this.hmiWindow(progress, 0.52F, 0.4F));
      float recoil = this.hmiPulse(progress, 0.44F, 0.5F);
      matrices.translate(0.2F * sideFactor * pull, 0.06F * pull, 0.34F * pull);
      this.hmiRotateYAt(matrices, 30.0F * sideFactor * pull, 0.3F * sideFactor, -0.4F, 0.0F);
      this.hmiRotateXAroundHand(matrices, 16.0F * pull, sideFactor);
      this.hmiRotateZAroundHand(matrices, 24.0F * sideFactor * pull, sideFactor);
      matrices.translate(-0.24F * sideFactor * lunge, -0.06F * lunge, -0.95F * lunge);
      this.hmiRotateYAt(matrices, -34.0F * sideFactor * lunge, 0.3F * sideFactor, -0.4F, 0.0F);
      this.hmiRotateXAroundHand(matrices, -14.0F * lunge, sideFactor);
      matrices.translate(0.0F, 0.03F * recoil, 0.12F * recoil);
      this.hmiRotateZAroundHand(matrices, -10.0F * sideFactor * recoil, sideFactor);
   }

   @Unique
   private void hmiCrossHand(PoseStack matrices, float progress, float sideFactor) {
      float mirror = this.hmi$reallyVariant == 1 ? -1.0F : 1.0F;
      float wind = this.hmiEaseOutBack(this.hmiWindow(progress, 0.0F, 0.22F)) * (1.0F - this.hmiWindow(progress, 0.22F, 0.16F));
      float cut = this.hmiEaseInOutBack(this.hmiWindow(progress, 0.2F, 0.34F)) * (1.0F - this.hmiWindow(progress, 0.62F, 0.38F));
      float swipe = this.hmiPulse(progress, 0.2F, 0.46F);
      matrices.translate(0.3F * sideFactor * mirror * wind, 0.26F * mirror * wind, 0.16F * wind);
      this.hmiRotateZAroundHand(matrices, 64.0F * sideFactor * mirror * wind, sideFactor);
      this.hmiRotateXAroundHand(matrices, 34.0F * wind, sideFactor);
      matrices.translate(-0.55F * sideFactor * mirror * cut, -0.34F * mirror * cut, -0.5F * swipe);
      this.hmiRotateZAroundHand(matrices, -128.0F * sideFactor * mirror * cut, sideFactor);
      this.hmiRotateXAroundHand(matrices, -62.0F * swipe, sideFactor);
      this.hmiRotateYAt(matrices, 22.0F * sideFactor * mirror * swipe, 0.3F * sideFactor, -0.4F, 0.0F);
   }

   @Unique
   private void hmiApplySwordAnimation(PoseStack matrices, HolMyItems config, float rawProgress, float sideFactor) {
      float eased = this.hmiEaseSwordSwing(rawProgress);
      String progress = config.hmiAnimationType.getValue();
      switch (progress) {
         case "Sharp":
            this.hmiSharpHand(matrices, eased, sideFactor);
            return;
         case "Spin":
            this.hmiSpinHand(matrices, eased, sideFactor);
            return;
         case "Heavy":
            this.hmiHeavyHand(matrices, eased, sideFactor);
            return;
         case "Stab":
            this.hmiStabHand(matrices, eased, sideFactor);
            return;
         case "Cross":
            this.hmiCrossHand(matrices, eased, sideFactor);
            return;
         default:
            float swingRot = this.hmiSwordSwingRot(eased);
            float tilt = this.hmiSwordTilt(eased);
            float hit = this.hmiSwordHit(eased);
            float secondHit = this.hmiSwordSecondHit(eased);
            float overall = Mth.sin((double)(eased * 3.14F));
            float rise = Mth.clamp(Mth.sin((double)(eased * 6.28F)), 0.0F, 1.0F);
            float signedRise = Mth.sin((double)(eased * 6.28F));
            boolean alternateCut = config.hmiAnimationType.is("Really") && this.hmi$reallyVariant == 1;
            if (alternateCut) {
               matrices.translate(-0.5F * sideFactor * tilt, -0.5F * tilt, 0.2F * tilt);
               matrices.translate(0.0F, 0.0F, -0.2F * hit);
               matrices.translate(0.0F, -0.1F * signedRise, 0.0F);
               matrices.translate(-0.15F * sideFactor * hit, 0.0F, 0.0F);
               matrices.translate(0.15F * sideFactor * secondHit, 0.0F, 0.0F);
               matrices.translate(0.15F * sideFactor * overall, 0.2F * overall, -0.3F * overall);
               this.hmiRotateXAroundHand(matrices, 20.0F * tilt, sideFactor);
               this.hmiRotateZAroundHand(matrices, 70.0F * sideFactor * tilt, sideFactor);
               this.hmiRotateXAroundHand(matrices, 30.0F * tilt, sideFactor);
               this.hmiRotateXAroundHand(matrices, 10.0F * rise, sideFactor);
               this.hmiRotateXAroundHand(matrices, 10.0F * swingRot, sideFactor);
               this.hmiRotateXAroundHand(matrices, -25.0F * hit, sideFactor);
               this.hmiRotateXAroundHand(matrices, -10.0F * secondHit, sideFactor);
               this.hmiRotateXAroundHand(matrices, -75.0F * overall, sideFactor);
            } else {
               matrices.translate(0.0F, 0.0F, -0.2F * tilt);
               matrices.translate(0.0F, 0.0F, -0.15F * hit);
               matrices.translate(0.0F, 0.0F, -0.25F * secondHit);
               matrices.translate(0.0F, 0.2F * hit - 0.1F * signedRise, 0.0F);
               matrices.translate(-0.15F * sideFactor * hit, 0.0F, 0.0F);
               matrices.translate(0.15F * sideFactor * secondHit, 0.0F, 0.0F);
               matrices.translate(-0.15F * sideFactor * overall, 0.2F * overall, -0.45F * overall);
               this.hmiRotateXAroundHand(matrices, 20.0F * tilt, sideFactor);
               this.hmiRotateZAroundHand(matrices, -70.0F * sideFactor * tilt, sideFactor);
               this.hmiRotateXAroundHand(matrices, 30.0F * tilt, sideFactor);
               this.hmiRotateXAroundHand(matrices, 10.0F * rise, sideFactor);
               this.hmiRotateXAroundHand(matrices, 10.0F * swingRot, sideFactor);
               this.hmiRotateXAroundHand(matrices, -55.0F * hit, sideFactor);
               this.hmiRotateXAroundHand(matrices, -15.0F * secondHit, sideFactor);
               this.hmiRotateXAroundHand(matrices, -95.0F * overall, sideFactor);
            }
      }
   }

   @Unique
   private void hmiApplySwordItemMotion(PoseStack matrices, HolMyItems config, float rawProgress, float sideFactor) {
      float eased = this.hmiEaseSwordSwing(rawProgress);
      String progress = config.hmiAnimationType.getValue();
      switch (progress) {
         case "Sharp":
            float lead = this.hmiPulse(eased, 0.16F, 0.36F);
            float lag = this.hmiPulse(eased, 0.1F, 0.5F);
            matrices.translate(0.0F, -0.06F * lag, -0.1F * lead);
            matrices.mulPose(Axis.XP.rotationDegrees(26.0F * lag));
            matrices.mulPose(Axis.XP.rotationDegrees(-72.0F * lead));
            matrices.mulPose(Axis.ZP.rotationDegrees(-18.0F * sideFactor * lead));
            return;
         case "Spin":
            float spin = this.hmiEaseInOutSine(this.hmiWindow(eased, 0.06F, 0.78F));
            float arc = Mth.sin((double)(eased * (float) Math.PI));
            matrices.translate(0.0F, -0.08F * arc, -0.12F * arc);
            matrices.mulPose(Axis.ZP.rotationDegrees(-360.0F * sideFactor * spin));
            matrices.mulPose(Axis.XP.rotationDegrees(-30.0F * arc));
            return;
         case "Heavy":
            float raise = this.hmiEaseInOutSine(this.hmiWindow(eased, 0.0F, 0.36F)) * (1.0F - this.hmiWindow(eased, 0.4F, 0.2F));
            float chop = this.hmiEaseInOutBack(this.hmiWindow(eased, 0.38F, 0.24F));
            float ring = Mth.sin((double)(eased * 94.2F)) * this.hmiWindow(eased, 0.58F, 0.05F) * (1.0F - this.hmiWindow(eased, 0.63F, 0.3F));
            matrices.translate(0.0F, -0.1F * chop, -0.05F * raise);
            matrices.mulPose(Axis.XP.rotationDegrees(38.0F * raise));
            matrices.mulPose(Axis.XP.rotationDegrees(-84.0F * chop));
            matrices.mulPose(Axis.ZP.rotationDegrees(3.5F * sideFactor * ring));
            return;
         case "Stab":
            float pull = this.hmiEaseOutBack(this.hmiWindow(eased, 0.0F, 0.26F)) * (1.0F - this.hmiWindow(eased, 0.26F, 0.16F));
            float lunge = this.hmiEaseOutBack(this.hmiWindow(eased, 0.24F, 0.2F)) * (1.0F - this.hmiWindow(eased, 0.52F, 0.4F));
            matrices.translate(0.0F, -0.05F * lunge, -0.2F * lunge);
            matrices.mulPose(Axis.XP.rotationDegrees(20.0F * pull));
            matrices.mulPose(Axis.XP.rotationDegrees(-58.0F * lunge));
            matrices.mulPose(Axis.ZP.rotationDegrees(-26.0F * sideFactor * lunge));
            return;
         case "Cross":
            float mirror = this.hmi$reallyVariant == 1 ? -1.0F : 1.0F;
            float cut = this.hmiEaseInOutBack(this.hmiWindow(eased, 0.2F, 0.34F)) * (1.0F - this.hmiWindow(eased, 0.62F, 0.38F));
            float swipe = this.hmiPulse(eased, 0.2F, 0.46F);
            matrices.translate(0.0F, -0.07F * swipe, -0.12F * swipe);
            matrices.mulPose(Axis.ZP.rotationDegrees(-70.0F * sideFactor * mirror * cut));
            matrices.mulPose(Axis.XP.rotationDegrees(-64.0F * swipe));
            return;
         default:
            float swingRot = this.hmiSwordSwingRot(eased);
            float hit = this.hmiSwordHit(eased);
            float swordSwing = Mth.sin((double)(eased * 3.14F));
            float straightening = this.hmiEaseInOutBack(swordSwing);
            matrices.translate(0.0F, -0.05F * swingRot, -0.05F * swingRot);
            matrices.mulPose(Axis.XP.rotationDegrees(10.0F * swingRot));
            matrices.mulPose(Axis.XP.rotationDegrees(-30.0F * swingRot));
            matrices.mulPose(Axis.XP.rotationDegrees(-10.0F * hit));
            matrices.translate(0.0F, -0.1F * straightening, 0.0F);
            matrices.mulPose(Axis.XP.rotationDegrees(-60.0F * straightening));
      }
   }

   @Unique
   private void hmiRenderOriginalSword(
      AbstractClientPlayer player,
      HolMyItems config,
      HumanoidArm arm,
      float sideFactor,
      ItemStack item,
      float progress,
      PoseStack matrices,
      SubmitNodeCollector orderedRenderCommandQueue,
      int light
   ) {
      matrices.translate(0.0F, -0.35F, 0.2F);
      matrices.pushPose();
      matrices.translate(1.5F * sideFactor, -0.3F, -0.6F);
      float armPivotX = 0.5F * sideFactor;
      this.hmiRotateXAt(matrices, 15.0F, armPivotX, 0.5F, 0.5F);
      this.hmiRotateYAt(matrices, 35.0F * sideFactor, armPivotX, 0.5F, 0.5F);
      this.hmiRotateZAt(matrices, -65.0F * sideFactor, armPivotX, 0.5F, 0.5F);
      matrices.scale(0.9F, 0.9F, 0.9F);
      this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
      matrices.popPose();
      matrices.pushPose();
      matrices.translate(0.5F * sideFactor, -0.15F, -0.85F);
      this.hmiRotateXAt(matrices, 15.0F, 0.5F, 0.5F, 0.5F);
      matrices.scale(0.9F, 0.9F, 0.9F);
      this.hmiApplySwordItemMotion(matrices, config, progress, sideFactor);
      this.renderItem(
         player,
         item,
         arm == HumanoidArm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
         matrices,
         orderedRenderCommandQueue,
         light
      );
      matrices.popPose();
   }

   @Inject(
      method = {"renderArmWithItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hmiOnRenderFirstPersonItem(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector orderedRenderCommandQueue,
      int light,
      CallbackInfo ci
   ) {
      HolMyItems swings = HolMyItems.getInstance();
      if (swings != null && swings.isEnabled()) {
         if (!player.isUsingItem() || !player.getUseItem().is(Items.SPYGLASS)) {
            boolean isMainHand = hand == InteractionHand.MAIN_HAND;
            HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            float sideFactor = isMainHand ? 1.0F : -1.0F;
            if (swings.swapHands.getValue()) {
               arm = arm.getOpposite();
               sideFactor *= -1.0F;
            }

            this.hmiRenderCustomFirstPersonItem(
               player, tickDelta, pitch, hand, arm, sideFactor, swingProgress, item, equipProgress, matrices, orderedRenderCommandQueue, light
            );
            ci.cancel();
         }
      }
   }

   @Unique
   private void hmiRenderCustomFirstPersonItem(
      AbstractClientPlayer player,
      float tickDelta,
      float pitch,
      InteractionHand hand,
      HumanoidArm arm,
      float sideFactor,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      PoseStack matrices,
      SubmitNodeCollector orderedRenderCommandQueue,
      int light
   ) {
      HolMyItems swings = HolMyItems.getInstance();
      if (swings != null && swings.isEnabled()) {
         boolean isUsingSpyglass = player.isUsingItem() && player.getUseItem().is(Items.SPYGLASS);
         if (!isUsingSpyglass) {
            HolMyItems config = HolMyItems.getInstance();
            float yaw = this.hmiVisualYaw(player);
            double radians = Math.toRadians((double)yaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            Vec3 horizontalVelocity = player.getDeltaMovement();
            double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
            double crossProduct = player.getDeltaMovement().x * forwardZ - horizontalVelocity.z * forwardX;
            float visualPitch = this.hmiVisualPitch(player);
            float al;
            if (visualPitch != 0.0F) {
               al = 90.0F / visualPitch / 10.0F;
            } else {
               al = 1.0F;
            }

            if (al > 1.0F) {
               al = 1.0F;
            }

            if (al < 0.0F) {
               al = 1.0F;
            }

            boolean bl = hand == InteractionHand.MAIN_HAND;
            matrices.pushPose();
            matrices.pushPose();
            boolean rightArm = arm == HumanoidArm.RIGHT;
            float renderedSide = rightArm ? 1.0F : -1.0F;
            matrices.translate(
               rightArm ? config.rightHandX.getFloat() : config.leftHandX.getFloat(),
               0.0F,
               rightArm ? config.rightHandZ.getFloat() : config.leftHandZ.getFloat()
            );
            double tt = 1.0 / (double)Math.max(Minecraft.getInstance().getFps(), 1) * 30.0;
            int handIndex = hand == InteractionHand.MAIN_HAND ? 0 : 1;
            boolean useMotions = config.itemAnimations.getValue();
            this.hmiUpdateUseCounters(player, item, hand, tt);
            float smoothness = Mth.clamp(config.hmiSmoothness.getFloat(), 0.35F, 2.5F);
            float sourceProgress = item.is(ItemTags.SWORDS)
               ? this.hmiResolveSwordProgress(player, config, tickDelta, hand, swingProgress)
               : Mth.clamp(swingProgress, 0.0F, 1.0F);
            float hmiProgress = (float)Math.pow((double)sourceProgress, (double)smoothness);
            float swing_rot = Mth.sin((double)((float)Math.sqrt((double)hmiProgress) * (float) Math.PI));
            float swing = Mth.sin((double)(hmiProgress * 3.14F));
            if ((
                  item.is(Items.EXPERIENCE_BOTTLE)
                     || item.is(Items.WIND_CHARGE)
                     || item.is(Items.EGG)
                     || item.is(Items.ENDER_EYE)
                     || item.is(Items.SNOWBALL)
                     || item.getItem() instanceof SplashPotionItem
                     || item.getItem() instanceof LingeringPotionItem
               )
               && player.getOffhandItem().isEmpty()
               && item.getUseAnimation() != ItemUseAnimation.SPEAR
               && !item.is(Items.FIRE_CHARGE)
               && !player.isSwimming()
               && !player.isVisuallyCrawling()
               && !player.onClimbable()) {
               if (player.getMainArm() == HumanoidArm.LEFT) {
                  bl = !bl;
               }

               matrices.pushPose();
               matrices.mulPose(Axis.YP.rotationDegrees(-25.0F * sideFactor));
               matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
               matrices.mulPose(Axis.YP.rotationDegrees(25.0F * sideFactor * swing));
               matrices.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
               matrices.translate(-0.15 * (double)sideFactor, 0.1, 0.1);
               matrices.translate(0.0, -0.55 * (double)swing, 0.4 * (double)swing * 3.14F);
               this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, 0.0F, arm.getOpposite());
               matrices.popPose();
            }

            if (this.minecraft.options.keyAttack.isDown() && !this.hmi$isAttacking && (double)swingProgress == 0.0) {
               this.hmi$left = !this.hmi$left;
            }

            if (!item.isEmpty()) {
               if (player.getMainArm() == HumanoidArm.LEFT) {
                  bl = !bl;
               }

               if (item.is(ItemTags.SWORDS)) {
                  matrices.translate(0.2F * renderedSide, 0.0F, 0.0F);
               } else if ((this.hmi$left || item.is(ItemTags.AXES) || item.getUseAnimation() == ItemUseAnimation.SPEAR || item.getUseAnimation() == ItemUseAnimation.BLOCK)
                  && !item.is(ItemTags.SHOVELS)) {
                  if (item.is(ItemTags.SWORDS) || item.is(ItemTags.AXES)) {
                     matrices.translate(0.8 * (double)sideFactor * (double)swing_rot, 0.3 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.mulPose(Axis.YP.rotationDegrees(15.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(-20.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-70.0F * swing_rot * sideFactor));
                     if (item.is(ItemTags.SWORDS)) {
                        matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                     } else {
                        matrices.mulPose(Axis.XN.rotationDegrees(30.0F * swing));
                     }
                  } else if (item.getUseAnimation() == ItemUseAnimation.SPEAR) {
                     matrices.translate(0.0, 0.0, 0.45 * (double)swing_rot);
                     matrices.translate(-0.25 * (double)sideFactor * (double)swing, -0.35 * (double)swing_rot, -0.6 * (double)swing);
                     matrices.translate(0.0, 0.1 * (double)swing, 0.0);
                     matrices.mulPose(Axis.YP.rotationDegrees(15.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.ZP.rotationDegrees(30.0F * swing_rot * sideFactor));
                  } else if (item.is(ConventionalItemTags.TOOLS) && item.getUseAnimation() != ItemUseAnimation.BLOCK && !item.is(ItemTags.SHOVELS)) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                  } else if (item.getUseAnimation() != ItemUseAnimation.BLOCK) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.1 * (double)swing);
                     matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                     matrices.mulPose(Axis.YP.rotationDegrees(10.0F * swing * sideFactor));
                  } else {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.2 * (double)swing);
                     matrices.mulPose(Axis.XN.rotationDegrees(-10.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(20.0F * swing));
                  }
               } else if (!item.is(ItemTags.SHOVELS)) {
                  if (item.is(ItemTags.SWORDS)) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                  } else if (item.is(ConventionalItemTags.TOOLS) && !item.is(ItemTags.SHOVELS)) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-20.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                  } else {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.1 * (double)swing);
                     matrices.mulPose(Axis.XN.rotationDegrees(-30.0F * swing_rot));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XN.rotationDegrees(40.0F * swing));
                     matrices.mulPose(Axis.YP.rotationDegrees(10.0F * swing * sideFactor));
                  }
               } else if (item.is(ItemTags.SHOVELS)) {
                  matrices.translate(0.0, 0.15 * (double)swing_rot, -0.25 * (double)swing_rot);
                  matrices.translate(0.0, 0.0, -0.2 * (double)swing);
                  matrices.mulPose(Axis.YP.rotationDegrees(15.0F * swing_rot));
                  matrices.mulPose(Axis.XP.rotationDegrees(-35.0F * swing_rot));
                  matrices.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
               }
            } else if (Block.byItem(item.getItem()) != Blocks.AIR
               && (
                  !item.is(ConventionalItemTags.TOOLS)
                     || item.is(ItemTags.TRIMMABLE_ARMOR)
                     || item.is(ItemTags.BOOKSHELF_BOOKS)
                     || item.getUseAnimation() == ItemUseAnimation.EAT
                     || !item.isEnchantable()
               )
               && item.getUseAnimation() != ItemUseAnimation.BOW
               && item.getUseAnimation() != ItemUseAnimation.SPYGLASS
               && this.hmiGetAttackDamage(item) == 0.0F
               && item.getUseAnimation() != ItemUseAnimation.BLOCK
               && !item.is(Items.WARPED_FUNGUS_ON_A_STICK)
               && !item.is(Items.CARROT_ON_A_STICK)
               && !item.is(Items.FISHING_ROD)
               && !item.is(Items.SHEARS)) {
               swingProgress = (float)((double)swingProgress * 1.2);
               if (swingProgress > 1.0F) {
                  swingProgress = 0.0F;
               }
            } else if (!item.is(ItemTags.SHOVELS)) {
               swingProgress = (float)((double)swingProgress * 1.5);
               if (swingProgress > 1.0F) {
                  swingProgress = 0.0F;
               }
            }

            if (player.getDeltaMovement().length() >= 0.08) {
               this.hmi$crawlCount = (float)((double)(Object)this.hmi$crawlCount + 0.1 * player.getDeltaMovement().length() * 2.0 * tt);
               this.hmi$directionalCrawlCount = (float)((double)(Object)this.hmi$directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
               this.hmi$directionalCrawlCount = (float)(
                  (double)(Object)this.hmi$directionalCrawlCount
                     + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt)
               );
            }

            if (player.getDeltaMovement().y > 0.0) {
               this.hmi$climbCount = (float)((double)(Object)this.hmi$climbCount + 0.1 * tt);
            }

            if (player.getDeltaMovement().y < 0.0) {
               this.hmi$climbCount = (float)((double)(Object)this.hmi$climbCount - 0.1 * tt);
            }

            if ((
                  player.isVisuallyCrawling() && config.climbAndCrawl.getValue()
                     || player.onClimbable() && !player.onGround() && Math.abs(player.getDeltaMovement().y) > 0.0 && config.climbAndCrawl.getValue()
               )
               && !player.isUsingItem()
               && swingProgress == 0.0F) {
               this.hmi$clCount = (float)((double)(Object)this.hmi$clCount + 0.1 * tt);
               if (this.hmi$clCount > 1.0F) {
                  this.hmi$clCount = 1.0F;
               }

               if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                  matrices.mulPose(Axis.XP.rotationDegrees(-20.0F * this.hmi$clCount));
               }
            } else {
               this.hmi$clCount = (float)((double)(Object)this.hmi$clCount * Math.pow(0.88F, tt));
            }

            if (swingProgress == 0.0F) {
               matrices.translate(bl ? visualPitch / 650.0F * this.hmi$clCount * -1.0F : visualPitch / 650.0F * this.hmi$clCount, 0.0F, 0.0F);
               matrices.mulPose(Axis.XP.rotationDegrees(visualPitch * this.hmi$clCount));
            }

            if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
               matrices.translate(0.0F, 0.0F, visualPitch / 120.0F * this.hmi$clCount);
            } else if (swingProgress == 0.0F) {
               matrices.translate(0.0F, 0.0F, visualPitch / 80.0F * this.hmi$clCount);
            }

            if (player.onClimbable()
               && config.climbAndCrawl.getValue()
               && !player.onGround()
               && !item.is(Items.LANTERN)
               && !item.is(Items.SOUL_LANTERN)
               && !player.isUsingItem()) {
               matrices.translate(0.0, 0.1, -0.2);
            }

            if (player.isInWater() && !player.isSwimming() && !player.isUnderWater()) {
               this.hmi$inWaterCounter = (float)((double)(Object)this.hmi$inWaterCounter + 0.1 * tt);
               if (this.hmi$inWaterCounter >= 1.0F) {
                  this.hmi$inWaterCounter = 1.0F;
               }
            } else {
               this.hmi$inWaterCounter = (float)((double)(Object)this.hmi$inWaterCounter * Math.pow(0.88F, tt));
            }

            this.hmi$freezeCounter = (float)((double)(Object)this.hmi$freezeCounter * Math.pow(0.88F, tt));
            matrices.translate(0.0, 0.02 * (double)(Object)this.hmi$inWaterCounter, 0.0);
            matrices.mulPose(Axis.ZP.rotationDegrees(8.0F * sideFactor * this.hmi$inWaterCounter));
            matrices.mulPose(Axis.XP.rotationDegrees(0.3F * Mth.sin((double)(this.hmi$freezeCounter * 5.0F))));
            if (player.getDeltaMovement().y < -0.85 && item.is(Items.MACE) && player.getMainHandItem() == item) {
               this.hmi$fallCounter = (float)((double)(Object)this.hmi$fallCounter + 0.1 * tt);
               if (this.hmi$fallCounter >= 1.0F) {
                  this.hmi$fallCounter = 1.0F;
               }
            } else {
               this.hmi$fallCounter = (float)((double)(Object)this.hmi$fallCounter * Math.pow(0.88F, tt));
            }

            if (bl) {
               matrices.mulPose(Axis.XP.rotationDegrees(45.0F * this.hmi$fallCounter));
               matrices.translate(0.0, -0.2 * (double)(Object)this.hmi$fallCounter, 0.0);
            }

            if (useMotions && item.is(Items.FISHING_ROD) && this.hmi$rodCast[handIndex] > 0.002F) {
               float cast = this.hmiEaseOutBack(this.hmi$rodCast[handIndex]);
               float flick = Mth.sin((double)(Mth.clamp(this.hmi$rodCast[handIndex] * 2.0F, 0.0F, 1.0F) * (float) Math.PI));
               float idleSway = Mth.sin((double)(((float)player.tickCount + tickDelta) * 0.09F)) * this.hmi$rodCast[handIndex];
               matrices.translate(0.07F * renderedSide * cast, 0.06F * cast, 0.12F * cast);
               this.hmiRotateXAroundHand(matrices, -22.0F * cast - 14.0F * flick + 1.6F * idleSway, renderedSide);
               this.hmiRotateZAroundHand(matrices, 12.0F * renderedSide * cast + 1.2F * renderedSide * idleSway, renderedSide);
               this.hmiRotateYAt(matrices, -10.0F * renderedSide * cast, 0.3F * renderedSide, -0.4F, 0.0F);
            }

            this.hmi$vertAngleY = (float)((double)(Object)this.hmi$vertAngleY + player.getDeltaMovement().y * 0.015F * tt);
            this.hmi$vertAngleY = (float)((double)(Object)this.hmi$vertAngleY - (double)(0.1F * this.hmi$vertAngleY) * tt);
            this.hmi$vertAngleY = (float)((double)(Object)this.hmi$vertAngleY * Math.pow(0.88F, tt));
            this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + player.getDeltaMovement().y * 0.015F * tt);
            this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime - (double)(0.1F * this.hmi$vertAngleYSlime) * tt);
            this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime * Math.pow(0.88F, tt));
            this.hmi$vertAngleYSlime = (float)((double)(Object)this.hmi$vertAngleYSlime + (double)(Object)this.hmi$vertVelocityYSlime * tt);
            matrices.translate(0.0F, this.hmi$vertAngleY * -1.0F, 0.0F);
            matrices.translate(0.0, Math.sin((double)player.tickCount * 0.1) * 0.007 * (double)sideFactor, 0.0);
            matrices.mulPose(Axis.YP.rotationDegrees(0.15F * Mth.sin((double)((float)player.tickCount * 0.15F)) * sideFactor));
            if (!item.isEmpty() || player.isVisuallyCrawling() || player.onClimbable() && !player.onGround() || player.isSwimming()) {
               if (player.getMainArm() == HumanoidArm.LEFT) {
                  bl = !bl;
               }

               if (item.getUseAnimation() == ItemUseAnimation.BLOCK) {
                  matrices.translate(0.0F, 0.0F, 0.0F);
               } else {
                  matrices.translate(0.0, -0.1, 0.1);
               }
            }

            if (item.is(Items.LANTERN) || item.is(Items.SOUL_LANTERN) || item.is(ItemTags.HANGING_SIGNS)) {
               matrices.translate(0.0, 0.1, 0.0);
               if (player.isSwimming()) {
                  matrices.translate(0.0, -0.1, 0.1);
               }
            }

            boolean hmiSwimmingPose = player.isSwimming() || config.swimmingStyle.is("Wild") && player.isFallFlying();
            if (hmiSwimmingPose && swingProgress == 0.0F && config.swimmingAnimation.getValue()) {
               if (config.swimmingStyle.is("Wild")) {
                  float time = ((float)player.tickCount + tickDelta) * 0.38F;
                  float sway = Mth.sin((double)time);
                  float glide = Mth.cos((double)(time * 0.5F + 0.45F));
                  float lift = Mth.sin((double)(time * 2.0F + (bl ? 0.0F : (float) Math.PI))) * 0.35F + Mth.sin((double)(time + 0.8F)) * 0.65F;
                  float reach = 0.5F + 0.5F * Mth.sin((double)(time - 0.65F));
                  matrices.translate(-0.075F * sideFactor * sway, -0.025F + 0.045F * lift, -0.1F - 0.09F * reach);
                  matrices.mulPose(Axis.YP.rotationDegrees(sideFactor * (8.0F + 12.0F * sway + 2.5F * lift)));
                  matrices.mulPose(Axis.XP.rotationDegrees(-7.0F + 6.0F * lift - 3.0F * reach));
                  matrices.mulPose(Axis.ZP.rotationDegrees(sideFactor * (-5.0F - 8.5F * glide + 2.0F * sway)));
               } else {
                  double distance = (double)(Object)this.hmi$crawlCount;
                  double swingAmplitude = 1.5;
                  double frequency = 2.0;
                  double s = distance * frequency;
                  double handRotation = Math.sin(s) * swingAmplitude;
                  double smoothRotation = handRotation * 0.8 + this.hmi$previousRotation * 0.2;
                  matrices.mulPose(Axis.YP.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
                  matrices.translate(0.0, 0.0, smoothRotation * 0.2F);
                  double k = (double)(this.hmi$crawlCount * 2.0F);
                  double a = Math.cos(k);
                  double b = a;
                  if (a <= 0.0) {
                     b = a * 0.5;
                  }

                  matrices.mulPose(Axis.YN.rotationDegrees((float)(bl ? b * 30.0 : b * 30.0 * -1.0)));
                  matrices.translate(0.0, 0.0, a * 0.2F);
                  this.hmi$previousRotation = smoothRotation;
               }

               if (item.isEmpty() && !bl && !player.isInvisible()) {
                  matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                  matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                  matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                  matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                  this.hmiAltSwing(matrices, arm, swingProgress, item);
                  float c = Mth.sin((double)(equipProgress * 3.14F));
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
               }
            }

            if ((player.onClimbable() && !player.onGround() || player.isVisuallyCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
               double s = (double)(Object)this.hmi$climbCount;
               float v = (float)player.getDeltaMovement().y;
               float a = Mth.cos((double)((float)s * 2.0F));
               if (player.onClimbable()) {
                  if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN)) {
                     matrices.mulPose(Axis.XP.rotationDegrees(20.0F * a * sideFactor));
                  } else {
                     matrices.mulPose(Axis.XP.rotationDegrees(1.0F * a * sideFactor));
                  }
               }

               if (player.isVisuallyCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                  float crawlProgress = Mth.sin((double)(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding));
                  float upAndDown = Mth.cos((double)(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding));
                  if (item.is(Items.LANTERN) || item.is(Items.SOUL_LANTERN)) {
                     crawlProgress *= 0.14F;
                     upAndDown *= 0.14F;
                  }

                  matrices.translate(
                     0.2 * (double)crawlProgress,
                     0.3 * (double)crawlProgress * (double)sideFactor,
                     -0.2 * (double)crawlProgress * (double)sideFactor * (double)al
                  );
                  matrices.mulPose(Axis.YP.rotationDegrees(25.0F * crawlProgress));
                  matrices.mulPose(Axis.XP.rotationDegrees(Mth.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
               }

               if (item.isEmpty() && !bl && !player.isInvisible() && (!player.onGround() && player.onClimbable() || player.isVisuallyCrawling())) {
                  matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                  matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                  matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                  matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                  this.hmiAltSwing(matrices, arm, swingProgress, item);
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
               }
            }

            if (item.isEmpty()) {
               if (bl && !player.isInvisible()) {
                  if ((player.onGround() || !player.onClimbable()) && !player.isSwimming() && !player.isVisuallyCrawling()) {
                     if (player.getMainArm() == HumanoidArm.LEFT) {
                        bl = !bl;
                     }

                     matrices.translate(0.0, 0.2 * (double)swing_rot, 0.15 * (double)swing_rot);
                     matrices.translate(0.1 * (double)sideFactor * (double)swing, 0.15 * (double)swing, -0.45 * (double)swing);
                     matrices.mulPose(Axis.YP.rotationDegrees(35.0F * swing * sideFactor));
                     matrices.mulPose(Axis.XP.rotationDegrees(-30.0F * swing));
                     matrices.mulPose(Axis.YP.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.mulPose(Axis.XP.rotationDegrees(10.0F * swing_rot));
                     this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  } else {
                     matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                     matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
                     matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
                     matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                     this.hmiAltSwing(matrices, arm, swingProgress, item);
                     float c = Mth.sin((double)(equipProgress * 3.14F));
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  }
               }
            } else if (item.has(DataComponents.MAP_ID)) {
               if (bl && this.mainHandItem.isEmpty()) {
                  matrices.translate(0.0, 0.1, 0.0);
                  this.renderTwoHandedMap(matrices, orderedRenderCommandQueue, light, pitch, equipProgress, swingProgress);
               } else {
                  matrices.translate(bl ? -0.1 : 0.1, 0.1, 0.0);
                  this.renderOneHandedMap(matrices, orderedRenderCommandQueue, light, equipProgress, arm, swingProgress, item);
               }
            } else if (item.getUseAnimation() == ItemUseAnimation.CROSSBOW) {
               matrices.pushPose();
               boolean bl2 = CrossbowItem.isCharged(item);
               boolean bl3 = arm == HumanoidArm.RIGHT;
               int i = bl3 ? 1 : -1;
               if (player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand) {
                  this.applyItemArmTransform(matrices, arm, equipProgress);
                  matrices.translate((float)i * -0.4785682F, -0.24387F, 0.05731531F);
                  matrices.mulPose(Axis.XP.rotationDegrees(-11.935F));
                  matrices.mulPose(Axis.YP.rotationDegrees((float)i * 65.3F));
                  matrices.mulPose(Axis.ZP.rotationDegrees((float)i * 9.785F));
                  float f = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                  float g = f / (float)CrossbowItem.getChargeDuration(item, player);
                  if (g > 1.0F) {
                     g = 1.0F;
                  }

                  if (g > 0.1F) {
                     float h = Mth.sin((double)((f - 0.1F) * 1.3F));
                     float j = g - 0.1F;
                     float k = h * j;
                     matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                  }

                  matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                  matrices.scale(1.0F, 1.0F, 1.0F);
                  matrices.mulPose(Axis.YN.rotationDegrees((float)i * 45.0F));
               } else {
                  this.swingArm(swingProgress, matrices, i, arm);
                  if (bl2 && swingProgress < 0.001F && bl) {
                     matrices.translate((float)i * -0.341864F, 0.0F, 0.0F);
                     matrices.mulPose(Axis.YP.rotationDegrees((float)i * 10.0F));
                  }
               }

               if (useMotions) {
                  float load = this.hmiEaseOutBack(this.hmi$crossbowDraw[handIndex]);
                  float settle = this.hmiEaseOutBack(this.hmi$crossbowHold[handIndex]);
                  matrices.translate(0.0F, -0.15F * load, 0.3F * load);
                  this.hmiRotateZAt(matrices, 20.0F * renderedSide * load, -0.3F * renderedSide, -0.4F, 0.0F);
                  this.hmiRotateYAt(matrices, 15.0F * renderedSide * load, -0.3F * renderedSide, -0.4F, 0.0F);
                  this.hmiRotateXAroundHand(matrices, -7.0F * settle, renderedSide);
               }

               matrices.translate(0.0F, 0.0F, -1.0F);
               matrices.translate(-0.45 * (double)i, 0.45, 1.7);
               matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
               matrices.mulPose(Axis.YP.rotationDegrees(45.0F * sideFactor));
               matrices.mulPose(Axis.ZP.rotationDegrees(-40.0F * sideFactor));
               matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
               this.hmiAltSwing(matrices, arm, swingProgress, item);
               float c = Mth.sin((double)(equipProgress * 3.14F));
               matrices.scale(0.9F, 0.9F, 0.9F);
               this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
               matrices.translate(-0.25 * (double)i, 1.25, 0.05);
               matrices.mulPose(Axis.YP.rotationDegrees((float)(-90 * i)));
               matrices.mulPose(Axis.XP.rotationDegrees(77.0F));
               matrices.mulPose(Axis.ZP.rotationDegrees((float)(85 * i)));
               matrices.scale(1.2F, 1.2F, 1.2F);
               matrices.mulPose(Axis.XP.rotationDegrees(-10.0F));
               matrices.translate(0.0, -0.15, 0.15);
               this.renderItem(
                  player,
                  item,
                  bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                  matrices,
                  orderedRenderCommandQueue,
                  light
               );
               matrices.popPose();
               boolean loadingCrossbow = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
               if (loadingCrossbow || useMotions && this.hmi$crossbowDraw[handIndex] > 0.002F) {
                  float fx = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                  float gx;
                  if (useMotions) {
                     gx = Mth.clamp(this.hmiEaseOutBack(this.hmi$crossbowDraw[handIndex]), 0.0F, 1.0F);
                  } else {
                     gx = Math.min(fx / (float)CrossbowItem.getChargeDuration(item, player), 1.0F);
                     if (gx > 0.1F) {
                        float h = Mth.sin((double)((fx - 0.1F) * 1.3F));
                        float j = gx - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                     }
                  }

                  matrices.mulPose(Axis.YN.rotationDegrees((double)gx <= 0.2 ? 75.0F * gx * 5.0F * (float)i : (float)(75 * i)));
                  matrices.mulPose(Axis.XN.rotationDegrees(10.0F * gx * 1.5F));
                  matrices.translate(-0.37 * (double)i, 0.0, 0.6);
                  matrices.translate(0.15 * (double)gx * (double)i, 0.0, 0.0);
                  this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
               }
            } else {
               boolean bl2x = arm == HumanoidArm.RIGHT;
               int l = bl2x ? 1 : -1;
               boolean activelyUsingThisHand = player.isUsingItem() && player.getUseItemRemainingTicks() > 0 && player.getUsedItemHand() == hand;
               if (item.is(ItemTags.SWORDS) && !activelyUsingThisHand) {
                  this.hmiApplySwordAnimation(matrices, config, hmiProgress, renderedSide);
                  this.hmiRenderOriginalSword(player, config, arm, renderedSide, item, hmiProgress, matrices, orderedRenderCommandQueue, light);
                  matrices.popPose();
                  matrices.popPose();
                  this.hmi$isAttacking = this.minecraft.options.keyAttack.isDown();
                  return;
               }

               boolean residualUseMotion = useMotions && this.hmiHasResidualUseMotion(handIndex, item);
               if (!activelyUsingThisHand && !residualUseMotion) {
                  if (player.isAutoSpinAttack() && item.getUseAnimation() == ItemUseAnimation.SPEAR) {
                     this.hmi$riptideCounter = (float)((double)(Object)this.hmi$riptideCounter + 0.15 * tt);
                     float m = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                     float fx = m / 10.0F;
                     if (fx > 1.0F) {
                        fx = 1.0F;
                     }

                     if (fx > 0.1F) {
                        float gx = Mth.sin((double)((m - 0.1F) * 1.3F));
                        float h = fx - 0.1F;
                        float j = gx * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.mulPose(Axis.XP.rotationDegrees(45.0F - this.hmi$riptideCounter * 2.0F));
                     matrices.mulPose(Axis.YP.rotationDegrees((float)(25 * l)));
                     matrices.translate(0.2 * (double)l, 0.0, 0.75);
                     matrices.translate(0.0, 0.0, 0.01 * (double)Mth.sin((double)(this.hmi$riptideCounter * 6.28F)));
                     this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                     matrices.mulPose(Axis.XP.rotationDegrees(135.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(-65 * l)));
                     matrices.translate((double)(0.65F * (float)l), -1.0, -0.6);
                  } else {
                     this.hmi$riptideCounter = 0.0F;
                     if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !item.is(ItemTags.HANGING_SIGNS)) {
                        if (item.getUseAnimation() == ItemUseAnimation.BLOCK) {
                           matrices.translate(0.0, -0.2, 0.0);
                        }
                     } else {
                        matrices.translate(0.1 * (double)l, 0.0, -0.1);
                        matrices.mulPose(Axis.XP.rotationDegrees(10.0F));
                     }

                     matrices.translate((double)(1 * l), 0.0 - (double)equipProgress * 0.3, 0.3);
                     matrices.mulPose(Axis.YP.rotationDegrees((float)(45 * l)));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(-40 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                     this.hmiAltSwing(matrices, arm, swingProgress, item);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  }
               } else {
                  switch (item.getUseAnimation()) {
                     case BOW:
                        matrices.pushPose();
                        if (player.getMainArm() == HumanoidArm.LEFT) {
                           bl = !bl;
                        }

                        float m1 = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float f1 = useMotions ? Mth.clamp(this.hmiEaseInOutBack(this.hmi$bowDraw[handIndex]), 0.0F, 1.0F) : Math.min(m1 / 20.0F, 1.0F);
                        float hold = useMotions ? this.hmi$bowHold[handIndex] : 0.0F;
                        float holdEase = this.hmiEaseOutBack(hold);
                        float tremble = useMotions ? Mth.sin((double)(this.hmi$bowWiggle[handIndex] * 15.14F)) * hold : 0.0F;
                        float pullArc = Mth.sin((double)(f1 * (float) Math.PI));
                        if (!useMotions && f1 > 0.1F) {
                           float g1 = Mth.sin((double)((m1 - 0.1F) * 1.3F));
                           float j1 = g1 * f1;
                           matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                        }

                        matrices.translate(bl ? -0.1 : 0.1, 0.0, (double)f1 * 0.15);
                        if (useMotions) {
                           matrices.translate(0.0F, 0.06F * f1, 0.0015F * tremble);
                           this.hmiRotateXAroundHand(matrices, 5.0F * pullArc, renderedSide);
                        }

                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        matrices.popPose();
                        matrices.translate(bl ? -0.5 : 0.5, -0.45, 0.1);
                        if (useMotions) {
                           matrices.translate(0.0F, -0.1F * pullArc, -0.18F * f1);
                        }

                        matrices.mulPose(Axis.XP.rotation(0.3F));
                        if (bl) {
                           matrices.mulPose(Axis.ZN.rotation(-0.3F));
                           matrices.mulPose(Axis.YN.rotation(1.0F));
                        } else {
                           matrices.mulPose(Axis.ZP.rotation(-0.3F));
                           matrices.mulPose(Axis.YP.rotation(1.0F));
                        }

                        if (useMotions) {
                           float pullBack = (bl ? -1.0F : 1.0F) * (14.0F * holdEase + 0.25F * tremble);
                           matrices.mulPose(Axis.YP.rotationDegrees(pullBack));
                           matrices.mulPose(Axis.XP.rotationDegrees(-6.0F * holdEase));
                        }

                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                        if (bl) {
                           matrices.mulPose(Axis.YN.rotation(2.5F));
                        } else {
                           matrices.mulPose(Axis.YP.rotation(2.5F));
                        }

                        matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                        matrices.popPose();
                        if (config.mb3DCompat.getValue()) {
                           matrices.mulPose(Axis.YP.rotationDegrees((float)(10 * l)));
                        }

                        matrices.mulPose(Axis.XN.rotationDegrees(75.0F));
                        matrices.mulPose(Axis.ZN.rotationDegrees((float)(-15 * l)));
                        matrices.translate(0.8 * (double)l, (double)(0.0F - equipProgress * 0.3F), -0.1);
                        if (useMotions) {
                           matrices.translate(0.0F, 0.0015F * tremble, -0.07F * f1);
                           matrices.mulPose(Axis.ZN.rotationDegrees(6.0F * (float)l * f1 + 0.35F * tremble));
                        } else {
                           float fxx = (f1 * f1 + f1 * 2.0F) / 3.0F;
                           if (fxx > 0.1F) {
                              float g1 = Mth.sin((double)((m1 - 0.1F) * 1.3F));
                              float h1 = f1 - 0.1F;
                              float j1 = g1 * h1;
                              matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                           }
                        }

                        matrices.pushPose();
                        break;
                     case SPEAR:
                        if (player.getOffhandItem().isEmpty() && !player.isVisuallyCrawling() && !player.isSwimming() && !player.onClimbable()) {
                           matrices.pushPose();
                           matrices.mulPose(Axis.YP.rotationDegrees((float)(-25 * l)));
                           matrices.translate(-0.15 * (double)l, 0.1, 0.1);
                           this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                           matrices.popPose();
                        }

                        float mx = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        if (useMotions) {
                           float draw = this.hmiEaseOutBack(this.hmi$tridentDraw[handIndex]);
                           float lift = this.hmiEaseInOutBack(this.hmi$tridentLift[handIndex]);
                           float quiver = Mth.sin((double)(this.hmi$tridentJiggle[handIndex] * draw * 9.14F));
                           matrices.translate(0.0F, -0.15F * draw, -0.3F * draw);
                           this.hmiRotateXAroundHand(matrices, 65.0F * draw, renderedSide);
                           this.hmiRotateXAroundHand(matrices, 55.0F * lift, renderedSide);
                           this.hmiRotateZAroundHand(matrices, 10.0F * renderedSide * draw, renderedSide);
                           this.hmiRotateXAroundHand(matrices, 0.3F * quiver, renderedSide);
                        } else {
                           float fxx = Math.min(mx / 10.0F, 1.0F);
                           if (fxx > 0.1F) {
                              float gx = Mth.sin((double)((mx - 0.1F) * 1.3F));
                              float h = fxx - 0.1F;
                              float j = gx * h;
                              matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                           }
                        }

                        matrices.mulPose(Axis.XP.rotationDegrees(45.0F));
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(25 * l)));
                        matrices.translate(0.2 * (double)l, 0.0, 0.8);
                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        matrices.mulPose(Axis.XP.rotationDegrees(135.0F));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float)(-65 * l)));
                        matrices.translate((double)(0.65F * (float)l), -1.0, -0.6);
                        break;
                     case BLOCK:
                        float k = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float sx;
                        float s2;
                        if (useMotions) {
                           float raise = this.hmi$shieldRaise[handIndex];
                           sx = this.hmiEaseInOutBack(raise);
                           s2 = raise;
                        } else {
                           sx = Math.min(k / 4.0F, 1.0F);
                           s2 = Math.min(k / 6.0F, 1.0F);
                        }

                        matrices.translate(0.0, -0.2, 0.0);
                        matrices.translate((double)(1 * l), 0.0, 0.3);
                        matrices.translate(0.7 * (double)sx * (double)l, 0.0, -1.3 * (double)sx);
                        matrices.translate(-0.2 * (double)l * (double)s2, 0.0, 0.0);
                        matrices.mulPose(Axis.XP.rotationDegrees((float)(10.0 * Math.sin((double)s2 * 3.14))));
                        matrices.mulPose(Axis.YP.rotationDegrees(70.0F * sx * (float)l));
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(45 * l)));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float)(-40 * l)));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(5 * l) * sx));
                        matrices.mulPose(Axis.XP.rotationDegrees(-10.0F * sx));
                        matrices.translate(0.0, 0.0, -0.2 * (double)sx);
                        if (useMotions) {
                           float raise = this.hmi$shieldRaise[handIndex];
                           this.hmiRotateXAroundHand(matrices, 13.0F * Mth.clamp(Mth.sin((double)(raise * 4.14F)), 0.0F, 1.0F), renderedSide);
                           this.hmiRotateXAroundHand(matrices, -15.0F * sx, renderedSide);
                        }

                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, swingProgress, arm);
                        matrices.translate(0.35 * (double)l, -0.13, -0.12);
                        matrices.mulPose(Axis.ZP.rotationDegrees(10.0F * (float)l));
                        matrices.mulPose(Axis.YP.rotationDegrees(10.0F * (float)l));
                        matrices.mulPose(Axis.XP.rotationDegrees(0.0F));
                        matrices.translate(-0.2 * (double)l, -0.04, 0.15);
                        matrices.scale(1.0F, 1.0F, 1.0F);
                        break;
                     case EAT:
                     case DRINK:
                     case TOOT_HORN:
                        float u = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float y;
                        float q;
                        if (useMotions) {
                           float draw = this.hmi$foodDraw[handIndex];
                           y = draw * draw;
                           q = Mth.sin((double)(this.hmi$foodChew[handIndex] * 2.0F)) / 10.0F;
                        } else {
                           y = Math.min(u / 5.0F, 1.0F);
                           q = Mth.sin((double)(u / 2.0F * 3.14F)) / 10.0F;
                        }

                        matrices.translate((double)(1 * l), 0.1, 0.3);
                        matrices.translate(0.2 * (double)l * (double)y, -0.7 * (double)y, -0.2 * (double)y);
                        matrices.translate(0.0, -0.2 * (double)q, -0.2 * (double)q);
                        matrices.translate(0.0, 0.1 * (double)(Object)this.hmiEaseInOutBack(Mth.sin((double)(y * 3.14F))), 0.0);
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(45 * l)));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float)(-40 * l)));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        matrices.mulPose(Axis.YP.rotationDegrees(45.0F * y * (float)l));
                        if (useMotions) {
                           float chew = this.hmi$foodChew[handIndex];
                           float drink = this.hmi$drinkDraw[handIndex];
                           boolean sipping = item.getUseAnimation() == ItemUseAnimation.DRINK;
                           matrices.mulPose(Axis.XP.rotationDegrees(20.0F * drink));
                           if (sipping) {
                              matrices.mulPose(
                                 Axis.XP.rotationDegrees(2.0F * this.hmiEaseInOutSine(Mth.sin((double)(chew * 2.0F))) * y)
                              );
                              matrices.mulPose(
                                 Axis.YP.rotationDegrees(3.0F * (float)l * this.hmiEaseInOutSine(Mth.sin((double)chew)) * y)
                              );
                              matrices.mulPose(
                                 Axis.ZP.rotationDegrees(5.0F * (float)l * this.hmiEaseInOutSine(Mth.sin((double)chew)) * y)
                              );
                           } else {
                              matrices.mulPose(
                                 Axis.XP.rotationDegrees(3.0F * this.hmiEaseInOutBack(Math.abs(Mth.sin((double)(chew * 3.0F)))) * y)
                              );
                              matrices.mulPose(
                                 Axis.YP
                                    .rotationDegrees(4.0F * (float)l * this.hmiEaseInOutBack(Math.abs(Mth.sin((double)(chew * 2.0F)))) * y)
                              );
                              matrices.mulPose(
                                 Axis.ZP
                                    .rotationDegrees(6.0F * (float)l * this.hmiEaseInOutBack(Math.abs(Mth.sin((double)(chew * 2.0F)))) * y)
                              );
                           }
                        }

                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, swingProgress, arm);
                        break;
                     case BRUSH:
                        float f5 = (float)(player.getUseItemRemainingTicks() % 10);
                        float g5 = f5 - tickDelta + 1.0F;
                        float h5 = 1.0F - g5 / 10.0F;
                        float n = -15.0F + 75.0F * Mth.cos((double)(h5 * 2.0F * (float) Math.PI));
                        float z = (float)item.getUseDuration(player) - ((float)player.getUseItemRemainingTicks() - tickDelta + 1.0F);
                        float x = useMotions ? this.hmiEaseInOutSine(this.hmi$brushDraw[handIndex]) : Math.min(z / 4.0F, 1.0F);
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(25 * l) * x));
                        matrices.translate((double)(0.3F * (float)l * x), 0.3 * (double)x, 0.1 * (double)x);
                        if (x > 0.99F) {
                           matrices.mulPose(Axis.YP.rotationDegrees(n / 20.0F));
                        }

                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        break;
                     case NONE:
                        this.applyItemArmTransform(matrices, arm, equipProgress);
                        break;
                     case BUNDLE:
                        matrices.translate((double)(1 * l), 0.0 - (double)equipProgress * 0.3, 0.3);
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(45 * l)));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float)(-40 * l)));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderPlayerArm(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  }
               }

               matrices.translate(-0.3 * (double)l, item.is(ItemTags.SWORDS) ? 0.55 : 0.65, -0.1);
               matrices.mulPose(Axis.YP.rotationDegrees((float)(-65 * l)));
               matrices.mulPose(Axis.XP.rotationDegrees(10.0F));
               if (item.is(ItemTags.WOOL_CARPETS)) {
                  matrices.translate(0.2 * (double)l, -0.1, 0.0);
               }

               if (Block.byItem(item.getItem()) != Blocks.AIR && item.getUseAnimation() != ItemUseAnimation.EAT && !item.is(ConventionalItemTags.BUCKETS)) {
                  if (item.getHoverName().toString().toLowerCase().contains("TORCH".toLowerCase())) {
                     matrices.scale(1.5F, 1.5F, 1.5F);
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(25 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(5.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(75 * l)));
                     matrices.translate(0.2 * (double)l, 0.2, 0.05);
                  } else if ((
                        item.is(Items.STRING)
                           || item.is(Items.REDSTONE)
                           || item.is(Items.LEVER)
                           || item.is(Items.TRIPWIRE_HOOK)
                           || Block.byItem(item.getItem()).defaultBlockState().is(ConventionalBlockTags.GLASS_PANES)
                           || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.RAILS)
                           || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.CLIMBABLE)
                           || item.is(ItemTags.DOORS)
                     )
                     && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.LEAVES)
                     && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
                     && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.BANNERS)) {
                     matrices.translate(0.0, 0.0, -0.1);
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(5 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(15.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(75 * l)));
                  } else if (!item.is(Items.LANTERN) && !item.is(Items.SOUL_LANTERN) && !item.is(ItemTags.HANGING_SIGNS)) {
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(25 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(5.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(75 * l)));
                     matrices.translate(0.2 * (double)l, 0.2, 0.05);
                     if (Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.BANNERS)) {
                        matrices.translate(-0.2 * (double)l, 0.0, 0.0);
                        matrices.scale(1.1F, 1.1F, 1.1F);
                     }
                  } else {
                     float yawDelta = player.getYRot() - player.getYHeadRot();
                     float pitchDelta = player.getXRot() - player.getXRot();
                     this.hmi$swingVelocityY += yawDelta * 0.015F * (float)tt;
                     this.hmi$swingVelocityY += swingProgress * 2.0F * (float)tt;
                     this.hmi$swingVelocityX += pitchDelta * 0.015F * (float)tt;
                     this.hmi$swingVelocityY = this.hmi$swingVelocityY - 0.1F * this.hmi$swingAngleY * (float)tt;
                     this.hmi$swingVelocityX = this.hmi$swingVelocityX - 0.1F * this.hmi$swingAngleX * (float)tt;
                     this.hmi$swingVelocityY = (float)((double)(Object)this.hmi$swingVelocityY * Math.pow(0.88F, tt));
                     this.hmi$swingVelocityX = (float)((double)(Object)this.hmi$swingVelocityX * Math.pow(0.88F, tt));
                     this.hmi$swingAngleY = this.hmi$swingAngleY + this.hmi$swingVelocityY * (float)tt;
                     this.hmi$swingAngleX = this.hmi$swingAngleX + this.hmi$swingVelocityX * (float)tt;
                     double currentSpeed = player.getDeltaMovement().length();
                     this.hmi$swingVelocityZ = (float)(
                        (double)(Object)this.hmi$swingVelocityZ
                           + (
                              bl
                                 ? (currentSpeed * -1.0 * 15.0 - (double)(Object)this.hmi$swingVelocityZ) * 0.1F * tt
                                 : (currentSpeed * 15.0 - (double)(Object)this.hmi$swingVelocityZ) * 0.1F * tt
                           )
                     );
                     if ((currentSpeed > 0.09 && player.onGround() || player.isSwimming() || player.onClimbable() && !player.onGround())
                        && (Boolean)(Object)this.minecraft.options.bobView().get()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.hmi$swingVelocityY += (float)(randomBoolean ? -5.5 * currentSpeed * tt : 5.5 * currentSpeed * tt);
                     }

                     matrices.translate(0.0, 0.0, -0.1);
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(35 * l) + this.hmi$swingAngleY));
                     matrices.mulPose(Axis.XP.rotationDegrees(15.0F + this.hmi$swingAngleX));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(75 * l) + this.hmi$swingVelocityZ));
                     if (item.is(ItemTags.HANGING_SIGNS)) {
                        matrices.translate(0.0, -0.1, 0.0);
                        matrices.mulPose(Axis.YP.rotationDegrees((float)(-45 * l)));
                     }

                     matrices.translate(0.3 * (double)l, -0.35, 0.0);
                     matrices.translate(0.0, 0.0, 0.1);
                     matrices.scale(1.5F, 1.5F, 1.5F);
                  }
               } else {
                  if (item.is(ItemTags.SWORDS)) {
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(75 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(70.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(45 * l)));
                     matrices.scale(0.9F, 0.9F, 0.9F);
                  } else if ((
                        !item.is(ConventionalItemTags.TOOLS)
                           || item.is(ItemTags.TRIMMABLE_ARMOR)
                           || item.is(ItemTags.BOOKSHELF_BOOKS)
                           || item.getUseAnimation() == ItemUseAnimation.EAT
                           || !item.isEnchantable()
                     )
                     && item.getUseAnimation() != ItemUseAnimation.BOW
                     && item.getUseAnimation() != ItemUseAnimation.SPYGLASS
                     && this.hmiGetAttackDamage(item) == 0.0F
                     && item.getUseAnimation() != ItemUseAnimation.BLOCK
                     && !item.is(Items.WARPED_FUNGUS_ON_A_STICK)
                     && !item.is(Items.CARROT_ON_A_STICK)
                     && !item.is(Items.FISHING_ROD)
                     && !item.is(Items.SHEARS)
                     && !item.is(ItemTags.HOES)
                     && !config.mb3DCompat.getValue()) {
                     if (item.getUseAnimation() == ItemUseAnimation.BRUSH) {
                        matrices.mulPose(Axis.XN.rotationDegrees(25.0F));
                        matrices.translate(bl ? 0.0 : 0.35, bl ? 0.0 : 0.25, bl ? 0.0 : 0.37);
                        if (!bl) {
                           matrices.scale(0.75F, 0.75F, 0.75F);
                        }

                        matrices.mulPose(Axis.ZN.rotationDegrees((float)(-75 * l)));
                        matrices.mulPose(Axis.XN.rotationDegrees(35.0F));
                        matrices.translate(bl ? -0.05 : 0.85, bl ? 0.0 : 0.05, bl ? 0.08 : -0.2);
                     } else {
                        matrices.mulPose(Axis.YN.rotationDegrees((float)(5 * l)));
                        matrices.mulPose(Axis.XP.rotationDegrees(15.0F));
                        matrices.mulPose(Axis.ZP.rotationDegrees((float)(75 * l)));
                        matrices.translate(0.0, -0.05, -0.1);
                        matrices.scale(0.7F, 0.7F, 0.7F);
                     }

                     if (item.is(Items.FEATHER) || item.is(Items.SLIME_BALL) || item.is(Items.PUFFERFISH)) {
                        this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + (double)swingProgress * 0.03 * (double)((float)tt));
                        if ((
                              player.getDeltaMovement().length() > 0.09 && player.onGround()
                                 || player.isSwimming()
                                 || player.isVisuallyCrawling()
                                 || player.onClimbable() && !player.onGround()
                           )
                           && (Boolean)(Object)this.minecraft.options.bobView().get()) {
                           Random random = new Random();
                           boolean randomBoolean = random.nextBoolean();
                           this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getDeltaMovement().length() * (double)((float)tt));
                        }

                        matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                     }
                  } else if (item.getUseAnimation() == ItemUseAnimation.BLOCK && item.getUseAnimation() != ItemUseAnimation.SPEAR) {
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(160 * l)));
                     matrices.mulPose(Axis.YP.rotationDegrees((float)(-60 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(-70.0F));
                     matrices.scale(0.75F, 0.75F, 0.75F);
                     matrices.translate(0.15 * (double)l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                     matrices.translate(0.17 * (double)l, 0.0, 0.3);
                     matrices.mulPose(Axis.YP.rotationDegrees((float)(-90 * l)));
                  } else if (item.getUseAnimation() == ItemUseAnimation.SPEAR) {
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(75 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(45 * l)));
                     matrices.translate(-0.3F * (float)l, 0.0F, 0.0F);
                  } else {
                     matrices.mulPose(Axis.YN.rotationDegrees((float)(75 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(70.0F));
                     matrices.mulPose(Axis.ZP.rotationDegrees((float)(45 * l)));
                  }

                  if (item.getUseAnimation() != ItemUseAnimation.BLOCK) {
                     matrices.scale(1.2F, 1.2F, 1.2F);
                  }

                  if (item.getUseAnimation() == ItemUseAnimation.BOW && !player.isUsingItem()) {
                     matrices.translate(-0.1 * (double)l, -0.2, 0.0);
                  }

                  if (item.is(Items.MACE)) {
                     if (config.mb3DCompat.getValue()) {
                        matrices.translate(-0.08, 0.17, 0.0);
                        matrices.mulPose(Axis.XP.rotationDegrees(40.0F));
                     }

                     matrices.translate(0.1 * (double)l, 0.0, 0.0);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                  }
               }

               if (item.getItem() instanceof BlockItem
                  && (
                     !item.is(ConventionalItemTags.BUCKETS)
                           && item.getUseAnimation() != ItemUseAnimation.EAT
                           && !item.is(ItemTags.BANNERS)
                           && !item.is(Items.STRING)
                           && !item.is(Items.REDSTONE)
                           && !item.is(Items.LEVER)
                           && !item.is(Items.TRIPWIRE_HOOK)
                           && !Block.byItem(item.getItem()).defaultBlockState().is(ConventionalBlockTags.GLASS_PANES)
                           && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.RAILS)
                           && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.CLIMBABLE)
                           && !item.is(ItemTags.DOORS)
                        || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.LEAVES)
                  )
                  && !Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                  BlockItem blockItem = (BlockItem)item.getItem();
                  BlockRenderDispatcher blockRenderManager = Minecraft.getInstance().getBlockRenderer();
                  blockRenderManager.getBlockModel(blockItem.getBlock().defaultBlockState());
                  matrices.pushPose();
                  if (!bl2x) {
                     matrices.translate(-0.4F, 0.0F, 0.0F);
                  }

                  matrices.scale(0.4F, 0.4F, 0.4F);
                  matrices.translate(-0.9 * (double)l, -0.45, -0.5);
                  if (Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.BUTTONS)) {
                     matrices.translate(0.2 * (double)l, -0.15, -0.2);
                  }

                  if (Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.PRESSURE_PLATES)) {
                     matrices.translate(0.0, 0.1, 0.0);
                  }

                  if (item.is(Items.SLIME_BLOCK)
                     || item.is(Items.HONEY_BLOCK)
                     || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.FLOWERS)
                     || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.LEAVES)
                     || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.SAPLINGS)
                     || Block.byItem(item.getItem()).defaultBlockState().is(BlockTags.SWORD_EFFICIENT)) {
                     this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + (double)swingProgress * 0.03 * (double)((float)tt));
                     if ((
                           player.getDeltaMovement().length() > 0.09 && player.onGround()
                              || player.isSwimming()
                              || player.isVisuallyCrawling()
                              || player.onClimbable() && !player.onGround()
                        )
                        && (Boolean)(Object)this.minecraft.options.bobView().get()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getDeltaMovement().length() * (double)((float)tt));
                     }

                     matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                  }

                  BlockState blockState = blockItem.getBlock().defaultBlockState();
                  if ((float)player.tickCount - this.hmi$prevAge >= 100.0F) {
                     this.hmi$repPower = !this.hmi$repPower;
                     this.hmi$prevAge = (float)player.tickCount;
                  }

                  if (blockItem.getBlock() == Blocks.REPEATER && this.hmi$repPower) {
                     blockState = (BlockState)blockState.setValue(RepeaterBlock.POWERED, true);
                  }

                  if (blockItem.getBlock() == Blocks.COMPARATOR && this.hmi$repPower) {
                     blockState = (BlockState)blockState.setValue(ComparatorBlock.POWERED, true);
                  }

                  if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isUnderWater()) {
                     blockState = (BlockState)blockState.setValue(RedstoneTorchBlock.LIT, false);
                  }

                  if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isUnderWater()) {
                     blockState = (BlockState)blockState.setValue(CampfireBlock.LIT, false);
                  }

                  if (item.is(ItemTags.BEDS)) {
                     if (bl) {
                        matrices.translate(0.9, 0.0, 0.8);
                     }

                     matrices.mulPose(Axis.YP.rotationDegrees((float)(90 * l)));
                  }

                  blockRenderManager.renderSingleBlock(
                     blockState, matrices, Minecraft.getInstance().renderBuffers().bufferSource(), light, OverlayTexture.NO_OVERLAY
                  );
                  matrices.popPose();
               } else {
                  if (item.is(ConventionalItemTags.TOOLS)
                        && !item.is(ItemTags.TRIMMABLE_ARMOR)
                        && !item.is(ItemTags.BOOKSHELF_BOOKS)
                        && item.getUseAnimation() != ItemUseAnimation.EAT
                        && item.isEnchantable()
                     || item.getUseAnimation() == ItemUseAnimation.BOW
                     || item.getUseAnimation() == ItemUseAnimation.SPYGLASS
                     || this.hmiGetAttackDamage(item) != 0.0F
                     || item.getUseAnimation() == ItemUseAnimation.BLOCK
                     || item.is(Items.WARPED_FUNGUS_ON_A_STICK)
                     || item.is(Items.CARROT_ON_A_STICK)
                     || item.is(Items.FISHING_ROD)
                     || item.is(Items.SHEARS)) {
                     if (item.is(ItemTags.SHOVELS)) {
                        matrices.mulPose(Axis.XP.rotationDegrees(-80.0F * swing_rot));
                        matrices.mulPose(Axis.XP.rotationDegrees(30.0F * swing));
                     } else if (item.getUseAnimation() == ItemUseAnimation.SPEAR) {
                        matrices.mulPose(Axis.XP.rotationDegrees(-40.0F * swing_rot));
                        matrices.translate(0.0, 0.1 * (double)swing_rot, -0.1 * (double)swing_rot);
                     } else if (item.getUseAnimation() != ItemUseAnimation.BLOCK) {
                        matrices.mulPose(Axis.XP.rotationDegrees(-25.0F * swing));
                        matrices.translate(0.0, 0.05 * (double)swing, -0.05 * (double)swing);
                     }
                  }

                  if (item.is(Items.NETHER_STAR) || item.is(Items.END_CRYSTAL) && config.mb3DCompat.getValue()) {
                     this.hmi$netherCounter = (float)((double)(Object)this.hmi$netherCounter + 0.9 * tt);
                     matrices.translate(0.0, 0.25 + 0.02 * (double)Mth.sin((double)(this.hmi$netherCounter * 0.1F)), 0.0);
                     matrices.mulPose(Axis.XP.rotationDegrees(3.0F * Mth.sin((double)(this.hmi$netherCounter * 0.2F))));
                     matrices.scale(
                        1.0F + 0.01F * Mth.sin((double)(Object)this.hmi$netherCounter),
                        1.0F + 0.01F * Mth.sin((double)(Object)this.hmi$netherCounter),
                        1.0F + 0.01F * Mth.sin((double)(Object)this.hmi$netherCounter)
                     );
                  } else {
                     this.hmi$netherCounter = 0.0F;
                  }

                  if (config.mb3DCompat.getValue() && (item.is(Items.FEATHER) || item.is(Items.SLIME_BALL) || item.is(Items.PUFFERFISH))) {
                     this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + (double)swingProgress * 0.03 * (double)((float)tt));
                     if ((
                           player.getDeltaMovement().length() > 0.09 && player.onGround()
                              || player.isSwimming()
                              || player.isVisuallyCrawling()
                              || player.onClimbable() && !player.onGround()
                        )
                        && (Boolean)(Object)this.minecraft.options.bobView().get()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getDeltaMovement().length() * (double)((float)tt));
                     }

                     matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                  }

                  if (item.is(ItemTags.SHOVELS)) {
                     matrices.translate(0.07 * (double)l, 0.0, 0.05);
                     matrices.mulPose(Axis.YP.rotationDegrees((float)(90 * l)));
                     matrices.mulPose(Axis.XP.rotationDegrees(-15.0F));
                  }

                  if (item.is(Items.TORCH)) {
                  }

                  if (this.hmiIsCompactThrowable(item)) {
                     matrices.scale(0.62F, 0.62F, 0.62F);
                  }

                  this.renderItem(
                     player,
                     item,
                     bl2x ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                     matrices,
                     orderedRenderCommandQueue,
                     light
                  );
               }
            }

            matrices.popPose();
            matrices.popPose();
            this.hmi$isAttacking = this.minecraft.options.keyAttack.isDown();
         }
      }
   }

   @Shadow
   protected abstract void renderArmWithItem(
      AbstractClientPlayer var1,
      float var2,
      float var3,
      InteractionHand var4,
      float var5,
      ItemStack var6,
      float var7,
      PoseStack var8,
      SubmitNodeCollector var9,
      int var10
   );
}

