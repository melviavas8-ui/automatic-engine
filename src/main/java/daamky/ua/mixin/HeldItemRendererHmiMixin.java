package daamky.ua.mixin;

import java.util.Random;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.visual.HolMyItems;

@Mixin({HeldItemRenderer.class})
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
   private ItemStack mainHand;
   @Shadow
   @Final
   private MinecraftClient client;

   public HeldItemRendererHmiMixin() {
   }

   @Shadow
   protected abstract void renderArm(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Arm var4);

   @Shadow
   protected abstract void renderArmHoldingItem(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, Arm var6);

   @Shadow
   protected abstract void renderMapInOneHand(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, Arm var5, float var6, ItemStack var7);

   @Shadow
   protected abstract void renderMapInBothHands(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, float var6);

   @Shadow
   protected abstract void applyEquipOffset(MatrixStack var1, Arm var2, float var3);

   @Shadow
   protected abstract void swingArm(float var1, MatrixStack var2, int var3, Arm var4);

   @Shadow
   public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, MatrixStack var4, OrderedRenderCommandQueue var5, int var6);

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
      AttributeModifiersComponent modifiers = (AttributeModifiersComponent)stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (modifiers == null) {
         return 0.0F;
      } else {
         float totalDamage = 0.0F;

         for (Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
               totalDamage += (float)entry.modifier().value();
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
   private void hmiUpdateUseCounters(AbstractClientPlayerEntity player, ItemStack item, Hand hand, double tt) {
      int h = hand == Hand.MAIN_HAND ? 0 : 1;
      float step = (float)tt;
      boolean using = player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand;
      UseAction use = item.getUseAction();
      boolean drawingBow = using && use == UseAction.BOW;
      this.hmi$bowDraw[h] = MathHelper.clamp(this.hmi$bowDraw[h] + (drawingBow ? 0.075F : -0.07F) * step, 0.0F, 1.0F);
      if (drawingBow && this.hmi$bowDraw[h] >= 1.0F) {
         this.hmi$bowHold[h] = MathHelper.clamp(this.hmi$bowHold[h] + 0.025F * step, 0.0F, 1.0F);
         this.hmi$bowWiggle[h] = this.hmi$bowWiggle[h] + 0.07F * step;
      } else {
         this.hmi$bowHold[h] = MathHelper.clamp(this.hmi$bowHold[h] - 0.11F * step, 0.0F, 1.0F);
         if (this.hmi$bowHold[h] <= 0.0F) {
            this.hmi$bowWiggle[h] = 0.0F;
         }
      }

      boolean chargingCrossbow = using && use == UseAction.CROSSBOW;
      this.hmi$crossbowDraw[h] = MathHelper.clamp(this.hmi$crossbowDraw[h] + (chargingCrossbow ? 0.1F : -0.1F) * step, 0.0F, 1.0F);
      this.hmi$crossbowHold[h] = MathHelper.clamp(
         this.hmi$crossbowHold[h] + (chargingCrossbow && this.hmi$crossbowDraw[h] >= 1.0F ? 0.02F : -0.1F) * step, 0.0F, 1.0F
      );
      boolean chargingSpear = using && use == UseAction.SPEAR;
      if (chargingSpear) {
         this.hmi$tridentDraw[h] = MathHelper.clamp(this.hmi$tridentDraw[h] + 0.07F * step, 0.0F, 1.0F);
         this.hmi$tridentLift[h] = MathHelper.clamp(this.hmi$tridentLift[h] + 0.05F * step, 0.0F, 1.0F);
         this.hmi$tridentJiggle[h] = this.hmi$tridentJiggle[h] + 0.1F * step;
      } else {
         float release = player.isUsingRiptide() ? 0.57F : 0.1F;
         float liftRelease = player.isUsingRiptide() ? 0.53F : 0.07F;
         this.hmi$tridentDraw[h] = MathHelper.clamp(this.hmi$tridentDraw[h] - release * step, 0.0F, 1.0F);
         this.hmi$tridentLift[h] = MathHelper.clamp(this.hmi$tridentLift[h] - liftRelease * step, 0.0F, 1.0F);
         this.hmi$tridentJiggle[h] = Math.max(0.0F, this.hmi$tridentJiggle[h] - 0.1F * step);
      }

      this.hmi$tridentLift[h] = this.hmi$tridentLift[h] * (float)Math.pow(0.95, tt);
      float shieldRate = item.isIn(ItemTags.SWORDS) ? 0.12F : 0.07F;
      this.hmi$shieldRaise[h] = MathHelper.clamp(this.hmi$shieldRaise[h] + (using && use == UseAction.BLOCK ? shieldRate : -shieldRate) * step, 0.0F, 1.0F);
      boolean consuming = using && (use == UseAction.EAT || use == UseAction.DRINK || use == UseAction.TOOT_HORN);
      this.hmi$foodDraw[h] = MathHelper.clamp(this.hmi$foodDraw[h] + (consuming ? 0.1F : -0.1F) * step, 0.0F, 1.0F);
      this.hmi$brushDraw[h] = MathHelper.clamp(this.hmi$brushDraw[h] + (using && use == UseAction.BRUSH ? 0.1F : -0.1F) * step, 0.0F, 1.0F);
      if ((!consuming || !(this.hmi$foodDraw[h] >= 1.0F)) && !(this.hmi$brushDraw[h] > 0.4F)) {
         if (this.hmi$foodDraw[h] <= 0.0F && this.hmi$brushDraw[h] <= 0.0F) {
            this.hmi$foodChew[h] = 0.0F;
         }
      } else {
         this.hmi$foodChew[h] = this.hmi$foodChew[h] + 0.1F * step;
      }

      this.hmi$drinkDraw[h] = MathHelper.clamp(
         this.hmi$drinkDraw[h] + (using && use == UseAction.DRINK && this.hmi$foodDraw[h] >= 1.0F ? 0.04F : -0.1F) * step, 0.0F, 1.0F
      );
      boolean bobberOut = item.isOf(Items.FISHING_ROD) && player.fishHook != null;
      this.hmi$rodCast[h] = MathHelper.clamp(this.hmi$rodCast[h] + (bobberOut ? 0.12F : -0.12F) * step, 0.0F, 1.0F);
   }

   @Unique
   private boolean hmiHasResidualUseMotion(int h, ItemStack item) {
      return switch (item.getUseAction()) {
         case BOW -> this.hmi$bowDraw[h] > 0.002F;
         case SPEAR -> this.hmi$tridentDraw[h] > 0.002F;
         case BLOCK -> this.hmi$shieldRaise[h] > 0.002F;
         case EAT, DRINK, TOOT_HORN -> this.hmi$foodDraw[h] > 0.002F;
         case BRUSH -> this.hmi$brushDraw[h] > 0.002F;
         default -> false;
      };
   }

   @Unique
   private float hmiVisualYaw(AbstractClientPlayerEntity player) {
      Angle angle = AngleConnection.INSTANCE.getCurrentAngle();
      return angle == null ? player.getYaw() : angle.getYaw();
   }

   @Unique
   private float hmiVisualPitch(AbstractClientPlayerEntity player) {
      Angle angle = AngleConnection.INSTANCE.getCurrentAngle();
      return angle == null ? player.getPitch() : angle.getPitch();
   }

   @Unique
   private void hmiAltSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
      int i = arm == Arm.RIGHT ? 1 : -1;
      float f = MathHelper.sin((double)(swingProgress * 3.14F));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * (45.0F + f * 0.0F)));
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * -45.0F));
   }

   @Unique
   private boolean hmiIsCompactThrowable(ItemStack item) {
      return item.isOf(Items.EGG)
         || item.isOf(Items.SNOWBALL)
         || item.isOf(Items.ENDER_EYE)
         || item.isOf(Items.EXPERIENCE_BOTTLE)
         || item.isOf(Items.WIND_CHARGE)
         || item.getItem() instanceof SplashPotionItem
         || item.getItem() instanceof LingeringPotionItem;
   }

   @Unique
   private float hmiResolveSwordProgress(AbstractClientPlayerEntity player, HolMyItems config, float tickDelta, Hand hand, float vanillaProgress) {
      float current = MathHelper.clamp(vanillaProgress, 0.0F, 1.0F);
      float previous = hand == Hand.MAIN_HAND ? this.hmi$lastMainSwordSwing : this.hmi$lastOffSwordSwing;
      boolean active = hand == Hand.MAIN_HAND ? this.hmi$mainSwordSwingActive : this.hmi$offSwordSwingActive;
      double start = hand == Hand.MAIN_HAND ? this.hmi$mainSwordSwingStart : this.hmi$offSwordSwingStart;
      double now = (double)player.age + (double)tickDelta;
      boolean newSwing = current > 0.001F && (previous <= 0.001F || current + 0.15F < previous);
      if (newSwing) {
         start = now;
         active = true;
         if (hand == Hand.MAIN_HAND) {
            this.hmi$reallyVariant = 1 - this.hmi$reallyVariant;
         }
      }

      float result = 0.0F;
      if (active) {
         result = MathHelper.clamp((float)((now - start) / this.hmiSwingDurationTicks(config)), 0.0F, 1.0F);
         if (result >= 1.0F) {
            active = false;
         }
      }

      if (hand == Hand.MAIN_HAND) {
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
      float t = MathHelper.clamp(value, 0.0F, 1.0F);
      float t2 = t * t;
      float t3 = t2 * t;
      float inverse = 1.0F - t;
      return 3.0F * t * inverse * inverse * 0.44F + 3.0F * t2 * inverse * 0.94F + t3;
   }

   @Unique
   private void hmiRotateXAroundHand(MatrixStack matrices, float degrees, float sideFactor) {
      this.hmiRotateXAt(matrices, degrees, 0.3F * sideFactor, -0.4F, 0.0F);
   }

   @Unique
   private void hmiRotateZAroundHand(MatrixStack matrices, float degrees, float sideFactor) {
      this.hmiRotateZAt(matrices, degrees, 0.3F * sideFactor, -0.4F, 0.0F);
   }

   @Unique
   private void hmiRotateXAt(MatrixStack matrices, float degrees, float pivotX, float pivotY, float pivotZ) {
      matrices.translate(pivotX, pivotY, pivotZ);
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(degrees));
      matrices.translate(-pivotX, -pivotY, -pivotZ);
   }

   @Unique
   private void hmiRotateYAt(MatrixStack matrices, float degrees, float pivotX, float pivotY, float pivotZ) {
      matrices.translate(pivotX, pivotY, pivotZ);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(degrees));
      matrices.translate(-pivotX, -pivotY, -pivotZ);
   }

   @Unique
   private void hmiRotateZAt(MatrixStack matrices, float degrees, float pivotX, float pivotY, float pivotZ) {
      matrices.translate(pivotX, pivotY, pivotZ);
      matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(degrees));
      matrices.translate(-pivotX, -pivotY, -pivotZ);
   }

   @Unique
   private float hmiSwordSwingRot(float progress) {
      float value = progress < 0.70016F
         ? MathHelper.sin((double)(MathHelper.clamp(progress, 0.0F, 0.308F) * 5.1F))
         : MathHelper.sin((double)(MathHelper.clamp(progress, 0.70016F, 1.0F) * 5.1F - 2.0F));
      return value * value * value;
   }

   @Unique
   private float hmiSwordTilt(float progress) {
      return progress < 0.65245F
         ? MathHelper.sin((double)(MathHelper.clamp(progress, 0.0F, 0.16675F) * 3.14F * 3.0F))
         : MathHelper.sin((double)(MathHelper.clamp(progress, 0.65245F, 1.0F) * 4.4F - 1.2584F));
   }

   @Unique
   private float hmiSwordHit(float progress) {
      return MathHelper.sin((double)(MathHelper.clamp(progress, 0.16561F, 0.49422F) * 4.78F * 2.0F + 4.7F));
   }

   @Unique
   private float hmiSwordSecondHit(float progress) {
      return progress < 0.65594F
         ? MathHelper.sin((double)(MathHelper.clamp(progress, 0.16561F, 0.32991F) * 4.78F * 2.0F + 4.7F))
         : MathHelper.sin((double)(MathHelper.clamp(progress, 0.65594F, 0.82025F) * 4.78F * 2.0F - 4.7F));
   }

   @Unique
   private float hmiEaseOutBack(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float inv = MathHelper.clamp(x, 0.0F, 1.0F) - 1.0F;
      return 1.0F + c3 * inv * inv * inv + c1 * inv * inv;
   }

   @Unique
   private float hmiEaseInOutSine(float x) {
      return -(MathHelper.cos((double)((float) Math.PI * MathHelper.clamp(x, 0.0F, 1.0F))) - 1.0F) / 2.0F;
   }

   @Unique
   private float hmiWindow(float progress, float start, float length) {
      return MathHelper.clamp((progress - start) / length, 0.0F, 1.0F);
   }

   @Unique
   private float hmiPulse(float progress, float start, float length) {
      return MathHelper.sin((double)(this.hmiWindow(progress, start, length) * (float) Math.PI));
   }

   @Unique
   private void hmiSharpHand(MatrixStack matrices, float progress, float sideFactor) {
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
   private void hmiSpinHand(MatrixStack matrices, float progress, float sideFactor) {
      float arc = MathHelper.sin((double)(progress * (float) Math.PI));
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
   private void hmiHeavyHand(MatrixStack matrices, float progress, float sideFactor) {
      float raise = this.hmiEaseInOutSine(this.hmiWindow(progress, 0.0F, 0.36F)) * (1.0F - this.hmiWindow(progress, 0.4F, 0.2F));
      float chop = this.hmiEaseInOutBack(this.hmiWindow(progress, 0.38F, 0.24F));
      float impact = this.hmiPulse(progress, 0.58F, 0.42F);
      float shake = MathHelper.sin((double)(progress * 78.5F)) * this.hmiWindow(progress, 0.58F, 0.06F) * (1.0F - this.hmiWindow(progress, 0.64F, 0.28F));
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
   private void hmiStabHand(MatrixStack matrices, float progress, float sideFactor) {
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
   private void hmiCrossHand(MatrixStack matrices, float progress, float sideFactor) {
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
   private void hmiApplySwordAnimation(MatrixStack matrices, HolMyItems config, float rawProgress, float sideFactor) {
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
            float overall = MathHelper.sin((double)(eased * 3.14F));
            float rise = MathHelper.clamp(MathHelper.sin((double)(eased * 6.28F)), 0.0F, 1.0F);
            float signedRise = MathHelper.sin((double)(eased * 6.28F));
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
   private void hmiApplySwordItemMotion(MatrixStack matrices, HolMyItems config, float rawProgress, float sideFactor) {
      float eased = this.hmiEaseSwordSwing(rawProgress);
      String progress = config.hmiAnimationType.getValue();
      switch (progress) {
         case "Sharp":
            float lead = this.hmiPulse(eased, 0.16F, 0.36F);
            float lag = this.hmiPulse(eased, 0.1F, 0.5F);
            matrices.translate(0.0F, -0.06F * lag, -0.1F * lead);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(26.0F * lag));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-72.0F * lead));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18.0F * sideFactor * lead));
            return;
         case "Spin":
            float spin = this.hmiEaseInOutSine(this.hmiWindow(eased, 0.06F, 0.78F));
            float arc = MathHelper.sin((double)(eased * (float) Math.PI));
            matrices.translate(0.0F, -0.08F * arc, -0.12F * arc);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-360.0F * sideFactor * spin));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * arc));
            return;
         case "Heavy":
            float raise = this.hmiEaseInOutSine(this.hmiWindow(eased, 0.0F, 0.36F)) * (1.0F - this.hmiWindow(eased, 0.4F, 0.2F));
            float chop = this.hmiEaseInOutBack(this.hmiWindow(eased, 0.38F, 0.24F));
            float ring = MathHelper.sin((double)(eased * 94.2F)) * this.hmiWindow(eased, 0.58F, 0.05F) * (1.0F - this.hmiWindow(eased, 0.63F, 0.3F));
            matrices.translate(0.0F, -0.1F * chop, -0.05F * raise);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(38.0F * raise));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-84.0F * chop));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(3.5F * sideFactor * ring));
            return;
         case "Stab":
            float pull = this.hmiEaseOutBack(this.hmiWindow(eased, 0.0F, 0.26F)) * (1.0F - this.hmiWindow(eased, 0.26F, 0.16F));
            float lunge = this.hmiEaseOutBack(this.hmiWindow(eased, 0.24F, 0.2F)) * (1.0F - this.hmiWindow(eased, 0.52F, 0.4F));
            matrices.translate(0.0F, -0.05F * lunge, -0.2F * lunge);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * pull));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-58.0F * lunge));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-26.0F * sideFactor * lunge));
            return;
         case "Cross":
            float mirror = this.hmi$reallyVariant == 1 ? -1.0F : 1.0F;
            float cut = this.hmiEaseInOutBack(this.hmiWindow(eased, 0.2F, 0.34F)) * (1.0F - this.hmiWindow(eased, 0.62F, 0.38F));
            float swipe = this.hmiPulse(eased, 0.2F, 0.46F);
            matrices.translate(0.0F, -0.07F * swipe, -0.12F * swipe);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * sideFactor * mirror * cut));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-64.0F * swipe));
            return;
         default:
            float swingRot = this.hmiSwordSwingRot(eased);
            float hit = this.hmiSwordHit(eased);
            float swordSwing = MathHelper.sin((double)(eased * 3.14F));
            float straightening = this.hmiEaseInOutBack(swordSwing);
            matrices.translate(0.0F, -0.05F * swingRot, -0.05F * swingRot);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * hit));
            matrices.translate(0.0F, -0.1F * straightening, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * straightening));
      }
   }

   @Unique
   private void hmiRenderOriginalSword(
      AbstractClientPlayerEntity player,
      HolMyItems config,
      Arm arm,
      float sideFactor,
      ItemStack item,
      float progress,
      MatrixStack matrices,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int light
   ) {
      matrices.translate(0.0F, -0.35F, 0.2F);
      matrices.push();
      matrices.translate(1.5F * sideFactor, -0.3F, -0.6F);
      float armPivotX = 0.5F * sideFactor;
      this.hmiRotateXAt(matrices, 15.0F, armPivotX, 0.5F, 0.5F);
      this.hmiRotateYAt(matrices, 35.0F * sideFactor, armPivotX, 0.5F, 0.5F);
      this.hmiRotateZAt(matrices, -65.0F * sideFactor, armPivotX, 0.5F, 0.5F);
      matrices.scale(0.9F, 0.9F, 0.9F);
      this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
      matrices.pop();
      matrices.push();
      matrices.translate(0.5F * sideFactor, -0.15F, -0.85F);
      this.hmiRotateXAt(matrices, 15.0F, 0.5F, 0.5F, 0.5F);
      matrices.scale(0.9F, 0.9F, 0.9F);
      this.hmiApplySwordItemMotion(matrices, config, progress, sideFactor);
      this.renderItem(
         player,
         item,
         arm == Arm.RIGHT ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
         matrices,
         orderedRenderCommandQueue,
         light
      );
      matrices.pop();
   }

   @Inject(
      method = {"renderFirstPersonItem"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hmiOnRenderFirstPersonItem(
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int light,
      CallbackInfo ci
   ) {
      HolMyItems swings = HolMyItems.getInstance();
      if (swings != null && swings.isEnabled()) {
         if (!player.isUsingItem() || !player.getActiveItem().isOf(Items.SPYGLASS)) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
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
      AbstractClientPlayerEntity player,
      float tickDelta,
      float pitch,
      Hand hand,
      Arm arm,
      float sideFactor,
      float swingProgress,
      ItemStack item,
      float equipProgress,
      MatrixStack matrices,
      OrderedRenderCommandQueue orderedRenderCommandQueue,
      int light
   ) {
      HolMyItems swings = HolMyItems.getInstance();
      if (swings != null && swings.isEnabled()) {
         boolean isUsingSpyglass = player.isUsingItem() && player.getActiveItem().isOf(Items.SPYGLASS);
         if (!isUsingSpyglass) {
            HolMyItems config = HolMyItems.getInstance();
            float yaw = this.hmiVisualYaw(player);
            double radians = Math.toRadians((double)yaw);
            double forwardX = -Math.sin(radians);
            double forwardZ = Math.cos(radians);
            Vec3d horizontalVelocity = player.getVelocity();
            double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
            double crossProduct = player.getVelocity().x * forwardZ - horizontalVelocity.z * forwardX;
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

            boolean bl = hand == Hand.MAIN_HAND;
            matrices.push();
            matrices.push();
            boolean rightArm = arm == Arm.RIGHT;
            float renderedSide = rightArm ? 1.0F : -1.0F;
            matrices.translate(
               rightArm ? config.rightHandX.getFloat() : config.leftHandX.getFloat(),
               0.0F,
               rightArm ? config.rightHandZ.getFloat() : config.leftHandZ.getFloat()
            );
            double tt = 1.0 / (double)Math.max(MinecraftClient.getInstance().getCurrentFps(), 1) * 30.0;
            int handIndex = hand == Hand.MAIN_HAND ? 0 : 1;
            boolean useMotions = config.itemAnimations.getValue();
            this.hmiUpdateUseCounters(player, item, hand, tt);
            float smoothness = MathHelper.clamp(config.hmiSmoothness.getFloat(), 0.35F, 2.5F);
            float sourceProgress = item.isIn(ItemTags.SWORDS)
               ? this.hmiResolveSwordProgress(player, config, tickDelta, hand, swingProgress)
               : MathHelper.clamp(swingProgress, 0.0F, 1.0F);
            float hmiProgress = (float)Math.pow((double)sourceProgress, (double)smoothness);
            float swing_rot = MathHelper.sin((double)((float)Math.sqrt((double)hmiProgress) * (float) Math.PI));
            float swing = MathHelper.sin((double)(hmiProgress * 3.14F));
            if ((
                  item.isOf(Items.EXPERIENCE_BOTTLE)
                     || item.isOf(Items.WIND_CHARGE)
                     || item.isOf(Items.EGG)
                     || item.isOf(Items.ENDER_EYE)
                     || item.isOf(Items.SNOWBALL)
                     || item.getItem() instanceof SplashPotionItem
                     || item.getItem() instanceof LingeringPotionItem
               )
               && player.getOffHandStack().isEmpty()
               && item.getUseAction() != UseAction.SPEAR
               && !item.isOf(Items.FIRE_CHARGE)
               && !player.isSwimming()
               && !player.isCrawling()
               && !player.isClimbing()) {
               if (player.getMainArm() == Arm.LEFT) {
                  bl = !bl;
               }

               matrices.push();
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * sideFactor * swing));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
               matrices.translate(-0.15 * (double)sideFactor, 0.1, 0.1);
               matrices.translate(0.0, -0.55 * (double)swing, 0.4 * (double)swing * 3.14F);
               this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, 0.0F, arm.getOpposite());
               matrices.pop();
            }

            if (this.client.options.attackKey.isPressed() && !this.hmi$isAttacking && (double)swingProgress == 0.0) {
               this.hmi$left = !this.hmi$left;
            }

            if (!item.isEmpty()) {
               if (player.getMainArm() == Arm.LEFT) {
                  bl = !bl;
               }

               if (item.isIn(ItemTags.SWORDS)) {
                  matrices.translate(0.2F * renderedSide, 0.0F, 0.0F);
               } else if ((this.hmi$left || item.isIn(ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK)
                  && !item.isIn(ItemTags.SHOVELS)) {
                  if (item.isIn(ItemTags.SWORDS) || item.isIn(ItemTags.AXES)) {
                     matrices.translate(0.8 * (double)sideFactor * (double)swing_rot, 0.3 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * sideFactor));
                     if (item.isIn(ItemTags.SWORDS)) {
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                     } else {
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
                     }
                  } else if (item.getUseAction() == UseAction.SPEAR) {
                     matrices.translate(0.0, 0.0, 0.45 * (double)swing_rot);
                     matrices.translate(-0.25 * (double)sideFactor * (double)swing, -0.35 * (double)swing_rot, -0.6 * (double)swing);
                     matrices.translate(0.0, 0.1 * (double)swing, 0.0);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * sideFactor));
                  } else if (item.isIn(ConventionalItemTags.TOOLS) && item.getUseAction() != UseAction.BLOCK && !item.isIn(ItemTags.SHOVELS)) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  } else if (item.getUseAction() != UseAction.BLOCK) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.1 * (double)swing);
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * sideFactor));
                  } else {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.2 * (double)swing);
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
                  }
               } else if (!item.isIn(ItemTags.SHOVELS)) {
                  if (item.isIn(ItemTags.SWORDS)) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  } else if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.SHOVELS)) {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.5 * (double)swing);
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                  } else {
                     matrices.translate(0.1 * (double)sideFactor * (double)swing_rot, 0.1 * (double)swing_rot, -0.1 * (double)swing);
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * sideFactor));
                  }
               } else if (item.isIn(ItemTags.SHOVELS)) {
                  matrices.translate(0.0, 0.15 * (double)swing_rot, -0.25 * (double)swing_rot);
                  matrices.translate(0.0, 0.0, -0.2 * (double)swing);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
               }
            } else if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR
               && (
                  !item.isIn(ConventionalItemTags.TOOLS)
                     || item.isIn(ItemTags.TRIMMABLE_ARMOR)
                     || item.isIn(ItemTags.BOOKSHELF_BOOKS)
                     || item.getUseAction() == UseAction.EAT
                     || !item.isEnchantable()
               )
               && item.getUseAction() != UseAction.BOW
               && item.getUseAction() != UseAction.SPYGLASS
               && this.hmiGetAttackDamage(item) == 0.0F
               && item.getUseAction() != UseAction.BLOCK
               && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
               && !item.isOf(Items.CARROT_ON_A_STICK)
               && !item.isOf(Items.FISHING_ROD)
               && !item.isOf(Items.SHEARS)) {
               swingProgress = (float)((double)swingProgress * 1.2);
               if (swingProgress > 1.0F) {
                  swingProgress = 0.0F;
               }
            } else if (!item.isIn(ItemTags.SHOVELS)) {
               swingProgress = (float)((double)swingProgress * 1.5);
               if (swingProgress > 1.0F) {
                  swingProgress = 0.0F;
               }
            }

            if (player.getVelocity().length() >= 0.08) {
               this.hmi$crawlCount = (float)((double)(Object)this.hmi$crawlCount + 0.1 * player.getVelocity().length() * 2.0 * tt);
               this.hmi$directionalCrawlCount = (float)((double)(Object)this.hmi$directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
               this.hmi$directionalCrawlCount = (float)(
                  (double)(Object)this.hmi$directionalCrawlCount
                     + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt)
               );
            }

            if (player.getVelocity().y > 0.0) {
               this.hmi$climbCount = (float)((double)(Object)this.hmi$climbCount + 0.1 * tt);
            }

            if (player.getVelocity().y < 0.0) {
               this.hmi$climbCount = (float)((double)(Object)this.hmi$climbCount - 0.1 * tt);
            }

            if ((
                  player.isCrawling() && config.climbAndCrawl.getValue()
                     || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().y) > 0.0 && config.climbAndCrawl.getValue()
               )
               && !player.isUsingItem()
               && swingProgress == 0.0F) {
               this.hmi$clCount = (float)((double)(Object)this.hmi$clCount + 0.1 * tt);
               if (this.hmi$clCount > 1.0F) {
                  this.hmi$clCount = 1.0F;
               }

               if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * this.hmi$clCount));
               }
            } else {
               this.hmi$clCount = (float)((double)(Object)this.hmi$clCount * Math.pow(0.88F, tt));
            }

            if (swingProgress == 0.0F) {
               matrices.translate(bl ? visualPitch / 650.0F * this.hmi$clCount * -1.0F : visualPitch / 650.0F * this.hmi$clCount, 0.0F, 0.0F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(visualPitch * this.hmi$clCount));
            }

            if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
               matrices.translate(0.0F, 0.0F, visualPitch / 120.0F * this.hmi$clCount);
            } else if (swingProgress == 0.0F) {
               matrices.translate(0.0F, 0.0F, visualPitch / 80.0F * this.hmi$clCount);
            }

            if (player.isClimbing()
               && config.climbAndCrawl.getValue()
               && !player.isOnGround()
               && !item.isOf(Items.LANTERN)
               && !item.isOf(Items.SOUL_LANTERN)
               && !player.isUsingItem()) {
               matrices.translate(0.0, 0.1, -0.2);
            }

            if (player.isTouchingWater() && !player.isSwimming() && !player.isSubmergedInWater()) {
               this.hmi$inWaterCounter = (float)((double)(Object)this.hmi$inWaterCounter + 0.1 * tt);
               if (this.hmi$inWaterCounter >= 1.0F) {
                  this.hmi$inWaterCounter = 1.0F;
               }
            } else {
               this.hmi$inWaterCounter = (float)((double)(Object)this.hmi$inWaterCounter * Math.pow(0.88F, tt));
            }

            this.hmi$freezeCounter = (float)((double)(Object)this.hmi$freezeCounter * Math.pow(0.88F, tt));
            matrices.translate(0.0, 0.02 * (double)(Object)this.hmi$inWaterCounter, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * sideFactor * this.hmi$inWaterCounter));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin((double)(this.hmi$freezeCounter * 5.0F))));
            if (player.getVelocity().y < -0.85 && item.isOf(Items.MACE) && player.getMainHandStack() == item) {
               this.hmi$fallCounter = (float)((double)(Object)this.hmi$fallCounter + 0.1 * tt);
               if (this.hmi$fallCounter >= 1.0F) {
                  this.hmi$fallCounter = 1.0F;
               }
            } else {
               this.hmi$fallCounter = (float)((double)(Object)this.hmi$fallCounter * Math.pow(0.88F, tt));
            }

            if (bl) {
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * this.hmi$fallCounter));
               matrices.translate(0.0, -0.2 * (double)(Object)this.hmi$fallCounter, 0.0);
            }

            if (useMotions && item.isOf(Items.FISHING_ROD) && this.hmi$rodCast[handIndex] > 0.002F) {
               float cast = this.hmiEaseOutBack(this.hmi$rodCast[handIndex]);
               float flick = MathHelper.sin((double)(MathHelper.clamp(this.hmi$rodCast[handIndex] * 2.0F, 0.0F, 1.0F) * (float) Math.PI));
               float idleSway = MathHelper.sin((double)(((float)player.age + tickDelta) * 0.09F)) * this.hmi$rodCast[handIndex];
               matrices.translate(0.07F * renderedSide * cast, 0.06F * cast, 0.12F * cast);
               this.hmiRotateXAroundHand(matrices, -22.0F * cast - 14.0F * flick + 1.6F * idleSway, renderedSide);
               this.hmiRotateZAroundHand(matrices, 12.0F * renderedSide * cast + 1.2F * renderedSide * idleSway, renderedSide);
               this.hmiRotateYAt(matrices, -10.0F * renderedSide * cast, 0.3F * renderedSide, -0.4F, 0.0F);
            }

            this.hmi$vertAngleY = (float)((double)(Object)this.hmi$vertAngleY + player.getVelocity().y * 0.015F * tt);
            this.hmi$vertAngleY = (float)((double)(Object)this.hmi$vertAngleY - (double)(0.1F * this.hmi$vertAngleY) * tt);
            this.hmi$vertAngleY = (float)((double)(Object)this.hmi$vertAngleY * Math.pow(0.88F, tt));
            this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + player.getVelocity().y * 0.015F * tt);
            this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime - (double)(0.1F * this.hmi$vertAngleYSlime) * tt);
            this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime * Math.pow(0.88F, tt));
            this.hmi$vertAngleYSlime = (float)((double)(Object)this.hmi$vertAngleYSlime + (double)(Object)this.hmi$vertVelocityYSlime * tt);
            matrices.translate(0.0F, this.hmi$vertAngleY * -1.0F, 0.0F);
            matrices.translate(0.0, Math.sin((double)player.age * 0.1) * 0.007 * (double)sideFactor, 0.0);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin((double)((float)player.age * 0.15F)) * sideFactor));
            if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
               if (player.getMainArm() == Arm.LEFT) {
                  bl = !bl;
               }

               if (item.getUseAction() == UseAction.BLOCK) {
                  matrices.translate(0.0F, 0.0F, 0.0F);
               } else {
                  matrices.translate(0.0, -0.1, 0.1);
               }
            }

            if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS)) {
               matrices.translate(0.0, 0.1, 0.0);
               if (player.isSwimming()) {
                  matrices.translate(0.0, -0.1, 0.1);
               }
            }

            boolean hmiSwimmingPose = player.isSwimming() || config.swimmingStyle.is("Wild") && player.isGliding();
            if (hmiSwimmingPose && swingProgress == 0.0F && config.swimmingAnimation.getValue()) {
               if (config.swimmingStyle.is("Wild")) {
                  float time = ((float)player.age + tickDelta) * 0.38F;
                  float sway = MathHelper.sin((double)time);
                  float glide = MathHelper.cos((double)(time * 0.5F + 0.45F));
                  float lift = MathHelper.sin((double)(time * 2.0F + (bl ? 0.0F : (float) Math.PI))) * 0.35F + MathHelper.sin((double)(time + 0.8F)) * 0.65F;
                  float reach = 0.5F + 0.5F * MathHelper.sin((double)(time - 0.65F));
                  matrices.translate(-0.075F * sideFactor * sway, -0.025F + 0.045F * lift, -0.1F - 0.09F * reach);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sideFactor * (8.0F + 12.0F * sway + 2.5F * lift)));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-7.0F + 6.0F * lift - 3.0F * reach));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sideFactor * (-5.0F - 8.5F * glide + 2.0F * sway)));
               } else {
                  double distance = (double)(Object)this.hmi$crawlCount;
                  double swingAmplitude = 1.5;
                  double frequency = 2.0;
                  double s = distance * frequency;
                  double handRotation = Math.sin(s) * swingAmplitude;
                  double smoothRotation = handRotation * 0.8 + this.hmi$previousRotation * 0.2;
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(bl ? smoothRotation : -smoothRotation)));
                  matrices.translate(0.0, 0.0, smoothRotation * 0.2F);
                  double k = (double)(this.hmi$crawlCount * 2.0F);
                  double a = Math.cos(k);
                  double b = a;
                  if (a <= 0.0) {
                     b = a * 0.5;
                  }

                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(bl ? b * 30.0 : b * 30.0 * -1.0)));
                  matrices.translate(0.0, 0.0, a * 0.2F);
                  this.hmi$previousRotation = smoothRotation;
               }

               if (item.isEmpty() && !bl && !player.isInvisible()) {
                  matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                  this.hmiAltSwing(matrices, arm, swingProgress, item);
                  float c = MathHelper.sin((double)(equipProgress * 3.14F));
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
               }
            }

            if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
               double s = (double)(Object)this.hmi$climbCount;
               float v = (float)player.getVelocity().y;
               float a = MathHelper.cos((double)((float)s * 2.0F));
               if (player.isClimbing()) {
                  if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * a * sideFactor));
                  } else {
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * a * sideFactor));
                  }
               }

               if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                  float crawlProgress = MathHelper.sin((double)(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding));
                  float upAndDown = MathHelper.cos((double)(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding));
                  if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN)) {
                     crawlProgress *= 0.14F;
                     upAndDown *= 0.14F;
                  }

                  matrices.translate(
                     0.2 * (double)crawlProgress,
                     0.3 * (double)crawlProgress * (double)sideFactor,
                     -0.2 * (double)crawlProgress * (double)sideFactor * (double)al
                  );
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * crawlProgress));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
               }

               if (item.isEmpty() && !bl && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
                  matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                  this.hmiAltSwing(matrices, arm, swingProgress, item);
                  matrices.scale(0.9F, 0.9F, 0.9F);
                  this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
               }
            }

            if (item.isEmpty()) {
               if (bl && !player.isInvisible()) {
                  if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
                     if (player.getMainArm() == Arm.LEFT) {
                        bl = !bl;
                     }

                     matrices.translate(0.0, 0.2 * (double)swing_rot, 0.15 * (double)swing_rot);
                     matrices.translate(0.1 * (double)sideFactor * (double)swing, 0.15 * (double)swing, -0.45 * (double)swing);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * sideFactor));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * swing_rot * sideFactor));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
                     this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  } else {
                     matrices.translate((double)(1.0F * sideFactor), 0.0 - (double)equipProgress * 0.3, 0.3);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.hmiAltSwing(matrices, arm, swingProgress, item);
                     float c = MathHelper.sin((double)(equipProgress * 3.14F));
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  }
               }
            } else if (item.contains(DataComponentTypes.MAP_ID)) {
               if (bl && this.mainHand.isEmpty()) {
                  matrices.translate(0.0, 0.1, 0.0);
                  this.renderMapInBothHands(matrices, orderedRenderCommandQueue, light, pitch, equipProgress, swingProgress);
               } else {
                  matrices.translate(bl ? -0.1 : 0.1, 0.1, 0.0);
                  this.renderMapInOneHand(matrices, orderedRenderCommandQueue, light, equipProgress, arm, swingProgress, item);
               }
            } else if (item.getUseAction() == UseAction.CROSSBOW) {
               matrices.push();
               boolean bl2 = CrossbowItem.isCharged(item);
               boolean bl3 = arm == Arm.RIGHT;
               int i = bl3 ? 1 : -1;
               if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                  this.applyEquipOffset(matrices, arm, equipProgress);
                  matrices.translate((float)i * -0.4785682F, -0.24387F, 0.05731531F);
                  matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                  matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 65.3F));
                  matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)i * 9.785F));
                  float f = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                  float g = f / (float)CrossbowItem.getPullTime(item, player);
                  if (g > 1.0F) {
                     g = 1.0F;
                  }

                  if (g > 0.1F) {
                     float h = MathHelper.sin((double)((f - 0.1F) * 1.3F));
                     float j = g - 0.1F;
                     float k = h * j;
                     matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                  }

                  matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                  matrices.scale(1.0F, 1.0F, 1.0F);
                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)i * 45.0F));
               } else {
                  this.swingArm(swingProgress, matrices, i, arm);
                  if (bl2 && swingProgress < 0.001F && bl) {
                     matrices.translate((float)i * -0.341864F, 0.0F, 0.0F);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)i * 10.0F));
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
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
               this.hmiAltSwing(matrices, arm, swingProgress, item);
               float c = MathHelper.sin((double)(equipProgress * 3.14F));
               matrices.scale(0.9F, 0.9F, 0.9F);
               this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
               matrices.translate(-0.25 * (double)i, 1.25, 0.05);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * i)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
               matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(85 * i)));
               matrices.scale(1.2F, 1.2F, 1.2F);
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
               matrices.translate(0.0, -0.15, 0.15);
               this.renderItem(
                  player,
                  item,
                  bl3 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                  matrices,
                  orderedRenderCommandQueue,
                  light
               );
               matrices.pop();
               boolean loadingCrossbow = player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand;
               if (loadingCrossbow || useMotions && this.hmi$crossbowDraw[handIndex] > 0.002F) {
                  float fx = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                  float gx;
                  if (useMotions) {
                     gx = MathHelper.clamp(this.hmiEaseOutBack(this.hmi$crossbowDraw[handIndex]), 0.0F, 1.0F);
                  } else {
                     gx = Math.min(fx / (float)CrossbowItem.getPullTime(item, player), 1.0F);
                     if (gx > 0.1F) {
                        float h = MathHelper.sin((double)((fx - 0.1F) * 1.3F));
                        float j = gx - 0.1F;
                        float k = h * j;
                        matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                     }
                  }

                  matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((double)gx <= 0.2 ? 75.0F * gx * 5.0F * (float)i : (float)(75 * i)));
                  matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * gx * 1.5F));
                  matrices.translate(-0.37 * (double)i, 0.0, 0.6);
                  matrices.translate(0.15 * (double)gx * (double)i, 0.0, 0.0);
                  this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
               }
            } else {
               boolean bl2x = arm == Arm.RIGHT;
               int l = bl2x ? 1 : -1;
               boolean activelyUsingThisHand = player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand;
               if (item.isIn(ItemTags.SWORDS) && !activelyUsingThisHand) {
                  this.hmiApplySwordAnimation(matrices, config, hmiProgress, renderedSide);
                  this.hmiRenderOriginalSword(player, config, arm, renderedSide, item, hmiProgress, matrices, orderedRenderCommandQueue, light);
                  matrices.pop();
                  matrices.pop();
                  this.hmi$isAttacking = this.client.options.attackKey.isPressed();
                  return;
               }

               boolean residualUseMotion = useMotions && this.hmiHasResidualUseMotion(handIndex, item);
               if (!activelyUsingThisHand && !residualUseMotion) {
                  if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
                     this.hmi$riptideCounter = (float)((double)(Object)this.hmi$riptideCounter + 0.15 * tt);
                     float m = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                     float fx = m / 10.0F;
                     if (fx > 1.0F) {
                        fx = 1.0F;
                     }

                     if (fx > 0.1F) {
                        float gx = MathHelper.sin((double)((m - 0.1F) * 1.3F));
                        float h = fx - 0.1F;
                        float j = gx * h;
                        matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                     }

                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - this.hmi$riptideCounter * 2.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l)));
                     matrices.translate(0.2 * (double)l, 0.0, 0.75);
                     matrices.translate(0.0, 0.0, 0.01 * (double)MathHelper.sin((double)(this.hmi$riptideCounter * 6.28F)));
                     this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l)));
                     matrices.translate((double)(0.65F * (float)l), -1.0, -0.6);
                  } else {
                     this.hmi$riptideCounter = 0.0F;
                     if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                        if (item.getUseAction() == UseAction.BLOCK) {
                           matrices.translate(0.0, -0.2, 0.0);
                        }
                     } else {
                        matrices.translate(0.1 * (double)l, 0.0, -0.1);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                     }

                     matrices.translate((double)(1 * l), 0.0 - (double)equipProgress * 0.3, 0.3);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                     this.hmiAltSwing(matrices, arm, swingProgress, item);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                     this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  }
               } else {
                  switch (item.getUseAction()) {
                     case BOW:
                        matrices.push();
                        if (player.getMainArm() == Arm.LEFT) {
                           bl = !bl;
                        }

                        float m1 = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float f1 = useMotions ? MathHelper.clamp(this.hmiEaseInOutBack(this.hmi$bowDraw[handIndex]), 0.0F, 1.0F) : Math.min(m1 / 20.0F, 1.0F);
                        float hold = useMotions ? this.hmi$bowHold[handIndex] : 0.0F;
                        float holdEase = this.hmiEaseOutBack(hold);
                        float tremble = useMotions ? MathHelper.sin((double)(this.hmi$bowWiggle[handIndex] * 15.14F)) * hold : 0.0F;
                        float pullArc = MathHelper.sin((double)(f1 * (float) Math.PI));
                        if (!useMotions && f1 > 0.1F) {
                           float g1 = MathHelper.sin((double)((m1 - 0.1F) * 1.3F));
                           float j1 = g1 * f1;
                           matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                        }

                        matrices.translate(bl ? -0.1 : 0.1, 0.0, (double)f1 * 0.15);
                        if (useMotions) {
                           matrices.translate(0.0F, 0.06F * f1, 0.0015F * tremble);
                           this.hmiRotateXAroundHand(matrices, 5.0F * pullArc, renderedSide);
                        }

                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        matrices.pop();
                        matrices.translate(bl ? -0.5 : 0.5, -0.45, 0.1);
                        if (useMotions) {
                           matrices.translate(0.0F, -0.1F * pullArc, -0.18F * f1);
                        }

                        matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
                        if (bl) {
                           matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                           matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
                        } else {
                           matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                           matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
                        }

                        if (useMotions) {
                           float pullBack = (bl ? -1.0F : 1.0F) * (14.0F * holdEase + 0.25F * tremble);
                           matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(pullBack));
                           matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-6.0F * holdEase));
                        }

                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                        if (bl) {
                           matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
                        } else {
                           matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
                        }

                        matrices.translate(bl ? -0.65 : 0.65, -0.35, 0.27);
                        matrices.pop();
                        if (config.mb3DCompat.getValue()) {
                           matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(10 * l)));
                        }

                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
                        matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-15 * l)));
                        matrices.translate(0.8 * (double)l, (double)(0.0F - equipProgress * 0.3F), -0.1);
                        if (useMotions) {
                           matrices.translate(0.0F, 0.0015F * tremble, -0.07F * f1);
                           matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(6.0F * (float)l * f1 + 0.35F * tremble));
                        } else {
                           float fxx = (f1 * f1 + f1 * 2.0F) / 3.0F;
                           if (fxx > 0.1F) {
                              float g1 = MathHelper.sin((double)((m1 - 0.1F) * 1.3F));
                              float h1 = f1 - 0.1F;
                              float j1 = g1 * h1;
                              matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                           }
                        }

                        matrices.push();
                        break;
                     case SPEAR:
                        if (player.getOffHandStack().isEmpty() && !player.isCrawling() && !player.isSwimming() && !player.isClimbing()) {
                           matrices.push();
                           matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-25 * l)));
                           matrices.translate(-0.15 * (double)l, 0.1, 0.1);
                           this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                           matrices.pop();
                        }

                        float mx = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        if (useMotions) {
                           float draw = this.hmiEaseOutBack(this.hmi$tridentDraw[handIndex]);
                           float lift = this.hmiEaseInOutBack(this.hmi$tridentLift[handIndex]);
                           float quiver = MathHelper.sin((double)(this.hmi$tridentJiggle[handIndex] * draw * 9.14F));
                           matrices.translate(0.0F, -0.15F * draw, -0.3F * draw);
                           this.hmiRotateXAroundHand(matrices, 65.0F * draw, renderedSide);
                           this.hmiRotateXAroundHand(matrices, 55.0F * lift, renderedSide);
                           this.hmiRotateZAroundHand(matrices, 10.0F * renderedSide * draw, renderedSide);
                           this.hmiRotateXAroundHand(matrices, 0.3F * quiver, renderedSide);
                        } else {
                           float fxx = Math.min(mx / 10.0F, 1.0F);
                           if (fxx > 0.1F) {
                              float gx = MathHelper.sin((double)((mx - 0.1F) * 1.3F));
                              float h = fxx - 0.1F;
                              float j = gx * h;
                              matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                           }
                        }

                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l)));
                        matrices.translate(0.2 * (double)l, 0.0, 0.8);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-65 * l)));
                        matrices.translate((double)(0.65F * (float)l), -1.0, -0.6);
                        break;
                     case BLOCK:
                        float k = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
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
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(10.0 * Math.sin((double)s2 * 3.14))));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * sx * (float)l));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(5 * l) * sx));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * sx));
                        matrices.translate(0.0, 0.0, -0.2 * (double)sx);
                        if (useMotions) {
                           float raise = this.hmi$shieldRaise[handIndex];
                           this.hmiRotateXAroundHand(matrices, 13.0F * MathHelper.clamp(MathHelper.sin((double)(raise * 4.14F)), 0.0F, 1.0F), renderedSide);
                           this.hmiRotateXAroundHand(matrices, -15.0F * sx, renderedSide);
                        }

                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, swingProgress, arm);
                        matrices.translate(0.35 * (double)l, -0.13, -0.12);
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F * (float)l));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * (float)l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                        matrices.translate(-0.2 * (double)l, -0.04, 0.15);
                        matrices.scale(1.0F, 1.0F, 1.0F);
                        break;
                     case EAT:
                     case DRINK:
                     case TOOT_HORN:
                        float u = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float y;
                        float q;
                        if (useMotions) {
                           float draw = this.hmi$foodDraw[handIndex];
                           y = draw * draw;
                           q = MathHelper.sin((double)(this.hmi$foodChew[handIndex] * 2.0F)) / 10.0F;
                        } else {
                           y = Math.min(u / 5.0F, 1.0F);
                           q = MathHelper.sin((double)(u / 2.0F * 3.14F)) / 10.0F;
                        }

                        matrices.translate((double)(1 * l), 0.1, 0.3);
                        matrices.translate(0.2 * (double)l * (double)y, -0.7 * (double)y, -0.2 * (double)y);
                        matrices.translate(0.0, -0.2 * (double)q, -0.2 * (double)q);
                        matrices.translate(0.0, 0.1 * (double)(Object)this.hmiEaseInOutBack(MathHelper.sin((double)(y * 3.14F))), 0.0);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * y * (float)l));
                        if (useMotions) {
                           float chew = this.hmi$foodChew[handIndex];
                           float drink = this.hmi$drinkDraw[handIndex];
                           boolean sipping = item.getUseAction() == UseAction.DRINK;
                           matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * drink));
                           if (sipping) {
                              matrices.multiply(
                                 RotationAxis.POSITIVE_X.rotationDegrees(2.0F * this.hmiEaseInOutSine(MathHelper.sin((double)(chew * 2.0F))) * y)
                              );
                              matrices.multiply(
                                 RotationAxis.POSITIVE_Y.rotationDegrees(3.0F * (float)l * this.hmiEaseInOutSine(MathHelper.sin((double)chew)) * y)
                              );
                              matrices.multiply(
                                 RotationAxis.POSITIVE_Z.rotationDegrees(5.0F * (float)l * this.hmiEaseInOutSine(MathHelper.sin((double)chew)) * y)
                              );
                           } else {
                              matrices.multiply(
                                 RotationAxis.POSITIVE_X.rotationDegrees(3.0F * this.hmiEaseInOutBack(Math.abs(MathHelper.sin((double)(chew * 3.0F)))) * y)
                              );
                              matrices.multiply(
                                 RotationAxis.POSITIVE_Y
                                    .rotationDegrees(4.0F * (float)l * this.hmiEaseInOutBack(Math.abs(MathHelper.sin((double)(chew * 2.0F)))) * y)
                              );
                              matrices.multiply(
                                 RotationAxis.POSITIVE_Z
                                    .rotationDegrees(6.0F * (float)l * this.hmiEaseInOutBack(Math.abs(MathHelper.sin((double)(chew * 2.0F)))) * y)
                              );
                           }
                        }

                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, swingProgress, arm);
                        break;
                     case BRUSH:
                        float f5 = (float)(player.getItemUseTimeLeft() % 10);
                        float g5 = f5 - tickDelta + 1.0F;
                        float h5 = 1.0F - g5 / 10.0F;
                        float n = -15.0F + 75.0F * MathHelper.cos((double)(h5 * 2.0F * (float) Math.PI));
                        float z = (float)item.getMaxUseTime(player) - ((float)player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float x = useMotions ? this.hmiEaseInOutSine(this.hmi$brushDraw[handIndex]) : Math.min(z / 4.0F, 1.0F);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * l) * x));
                        matrices.translate((double)(0.3F * (float)l * x), 0.3 * (double)x, 0.1 * (double)x);
                        if (x > 0.99F) {
                           matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n / 20.0F));
                        }

                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        break;
                     case NONE:
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        break;
                     case BUNDLE:
                        matrices.translate((double)(1 * l), 0.0 - (double)equipProgress * 0.3, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(45 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(-40 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                  }
               }

               matrices.translate(-0.3 * (double)l, item.isIn(ItemTags.SWORDS) ? 0.55 : 0.65, -0.1);
               matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-65 * l)));
               matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
               if (item.isIn(ItemTags.WOOL_CARPETS)) {
                  matrices.translate(0.2 * (double)l, -0.1, 0.0);
               }

               if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR && item.getUseAction() != UseAction.EAT && !item.isIn(ConventionalItemTags.BUCKETS)) {
                  if (item.getName().toString().toLowerCase().contains("TORCH".toLowerCase())) {
                     matrices.scale(1.5F, 1.5F, 1.5F);
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                     matrices.translate(0.2 * (double)l, 0.2, 0.05);
                  } else if ((
                        item.isOf(Items.STRING)
                           || item.isOf(Items.REDSTONE)
                           || item.isOf(Items.LEVER)
                           || item.isOf(Items.TRIPWIRE_HOOK)
                           || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                           || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS)
                           || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE)
                           || item.isIn(ItemTags.DOORS)
                     )
                     && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                     && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
                     && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                     matrices.translate(0.0, 0.0, -0.1);
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                  } else if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(25 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                     matrices.translate(0.2 * (double)l, 0.2, 0.05);
                     if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                        matrices.translate(-0.2 * (double)l, 0.0, 0.0);
                        matrices.scale(1.1F, 1.1F, 1.1F);
                     }
                  } else {
                     float yawDelta = player.getYaw() - player.getHeadYaw();
                     float pitchDelta = player.getPitch() - player.getPitch();
                     this.hmi$swingVelocityY += yawDelta * 0.015F * (float)tt;
                     this.hmi$swingVelocityY += swingProgress * 2.0F * (float)tt;
                     this.hmi$swingVelocityX += pitchDelta * 0.015F * (float)tt;
                     this.hmi$swingVelocityY = this.hmi$swingVelocityY - 0.1F * this.hmi$swingAngleY * (float)tt;
                     this.hmi$swingVelocityX = this.hmi$swingVelocityX - 0.1F * this.hmi$swingAngleX * (float)tt;
                     this.hmi$swingVelocityY = (float)((double)(Object)this.hmi$swingVelocityY * Math.pow(0.88F, tt));
                     this.hmi$swingVelocityX = (float)((double)(Object)this.hmi$swingVelocityX * Math.pow(0.88F, tt));
                     this.hmi$swingAngleY = this.hmi$swingAngleY + this.hmi$swingVelocityY * (float)tt;
                     this.hmi$swingAngleX = this.hmi$swingAngleX + this.hmi$swingVelocityX * (float)tt;
                     double currentSpeed = player.getVelocity().length();
                     this.hmi$swingVelocityZ = (float)(
                        (double)(Object)this.hmi$swingVelocityZ
                           + (
                              bl
                                 ? (currentSpeed * -1.0 * 15.0 - (double)(Object)this.hmi$swingVelocityZ) * 0.1F * tt
                                 : (currentSpeed * 15.0 - (double)(Object)this.hmi$swingVelocityZ) * 0.1F * tt
                           )
                     );
                     if ((currentSpeed > 0.09 && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround())
                        && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.hmi$swingVelocityY += (float)(randomBoolean ? -5.5 * currentSpeed * tt : 5.5 * currentSpeed * tt);
                     }

                     matrices.translate(0.0, 0.0, -0.1);
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(35 * l) + this.hmi$swingAngleY));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + this.hmi$swingAngleX));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l) + this.hmi$swingVelocityZ));
                     if (item.isIn(ItemTags.HANGING_SIGNS)) {
                        matrices.translate(0.0, -0.1, 0.0);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-45 * l)));
                     }

                     matrices.translate(0.3 * (double)l, -0.35, 0.0);
                     matrices.translate(0.0, 0.0, 0.1);
                     matrices.scale(1.5F, 1.5F, 1.5F);
                  }
               } else {
                  if (item.isIn(ItemTags.SWORDS)) {
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
                     matrices.scale(0.9F, 0.9F, 0.9F);
                  } else if ((
                        !item.isIn(ConventionalItemTags.TOOLS)
                           || item.isIn(ItemTags.TRIMMABLE_ARMOR)
                           || item.isIn(ItemTags.BOOKSHELF_BOOKS)
                           || item.getUseAction() == UseAction.EAT
                           || !item.isEnchantable()
                     )
                     && item.getUseAction() != UseAction.BOW
                     && item.getUseAction() != UseAction.SPYGLASS
                     && this.hmiGetAttackDamage(item) == 0.0F
                     && item.getUseAction() != UseAction.BLOCK
                     && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                     && !item.isOf(Items.CARROT_ON_A_STICK)
                     && !item.isOf(Items.FISHING_ROD)
                     && !item.isOf(Items.SHEARS)
                     && !item.isIn(ItemTags.HOES)
                     && !config.mb3DCompat.getValue()) {
                     if (item.getUseAction() == UseAction.BRUSH) {
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0F));
                        matrices.translate(bl ? 0.0 : 0.35, bl ? 0.0 : 0.25, bl ? 0.0 : 0.37);
                        if (!bl) {
                           matrices.scale(0.75F, 0.75F, 0.75F);
                        }

                        matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees((float)(-75 * l)));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F));
                        matrices.translate(bl ? -0.05 : 0.85, bl ? 0.0 : 0.05, bl ? 0.08 : -0.2);
                     } else {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(5 * l)));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(75 * l)));
                        matrices.translate(0.0, -0.05, -0.1);
                        matrices.scale(0.7F, 0.7F, 0.7F);
                     }

                     if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                        this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + (double)swingProgress * 0.03 * (double)((float)tt));
                        if ((
                              player.getVelocity().length() > 0.09 && player.isOnGround()
                                 || player.isSwimming()
                                 || player.isCrawling()
                                 || player.isClimbing() && !player.isOnGround()
                           )
                           && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                           Random random = new Random();
                           boolean randomBoolean = random.nextBoolean();
                           this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * (double)((float)tt));
                        }

                        matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                     }
                  } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(160 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-60 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                     matrices.scale(0.75F, 0.75F, 0.75F);
                     matrices.translate(0.15 * (double)l, bl ? 0.35 : 0.45, bl ? -0.15 : -0.1);
                     matrices.translate(0.17 * (double)l, 0.0, 0.3);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(-90 * l)));
                  } else if (item.getUseAction() == UseAction.SPEAR) {
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
                     matrices.translate(-0.3F * (float)l, 0.0F, 0.0F);
                  } else {
                     matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(75 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                     matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(45 * l)));
                  }

                  if (item.getUseAction() != UseAction.BLOCK) {
                     matrices.scale(1.2F, 1.2F, 1.2F);
                  }

                  if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
                     matrices.translate(-0.1 * (double)l, -0.2, 0.0);
                  }

                  if (item.isOf(Items.MACE)) {
                     if (config.mb3DCompat.getValue()) {
                        matrices.translate(-0.08, 0.17, 0.0);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F));
                     }

                     matrices.translate(0.1 * (double)l, 0.0, 0.0);
                     matrices.scale(0.9F, 0.9F, 0.9F);
                  }
               }

               if (item.getItem() instanceof BlockItem
                  && (
                     !item.isIn(ConventionalItemTags.BUCKETS)
                           && item.getUseAction() != UseAction.EAT
                           && !item.isIn(ItemTags.BANNERS)
                           && !item.isOf(Items.STRING)
                           && !item.isOf(Items.REDSTONE)
                           && !item.isOf(Items.LEVER)
                           && !item.isOf(Items.TRIPWIRE_HOOK)
                           && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                           && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS)
                           && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE)
                           && !item.isIn(ItemTags.DOORS)
                        || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                  )
                  && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                  BlockItem blockItem = (BlockItem)item.getItem();
                  BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
                  blockRenderManager.getModel(blockItem.getBlock().getDefaultState());
                  matrices.push();
                  if (!bl2x) {
                     matrices.translate(-0.4F, 0.0F, 0.0F);
                  }

                  matrices.scale(0.4F, 0.4F, 0.4F);
                  matrices.translate(-0.9 * (double)l, -0.45, -0.5);
                  if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BUTTONS)) {
                     matrices.translate(0.2 * (double)l, -0.15, -0.2);
                  }

                  if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.PRESSURE_PLATES)) {
                     matrices.translate(0.0, 0.1, 0.0);
                  }

                  if (item.isOf(Items.SLIME_BLOCK)
                     || item.isOf(Items.HONEY_BLOCK)
                     || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.FLOWERS)
                     || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                     || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SAPLINGS)
                     || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SWORD_EFFICIENT)) {
                     this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + (double)swingProgress * 0.03 * (double)((float)tt));
                     if ((
                           player.getVelocity().length() > 0.09 && player.isOnGround()
                              || player.isSwimming()
                              || player.isCrawling()
                              || player.isClimbing() && !player.isOnGround()
                        )
                        && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * (double)((float)tt));
                     }

                     matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                  }

                  BlockState blockState = blockItem.getBlock().getDefaultState();
                  if ((float)player.age - this.hmi$prevAge >= 100.0F) {
                     this.hmi$repPower = !this.hmi$repPower;
                     this.hmi$prevAge = (float)player.age;
                  }

                  if (blockItem.getBlock() == Blocks.REPEATER && this.hmi$repPower) {
                     blockState = (BlockState)blockState.with(RepeaterBlock.POWERED, true);
                  }

                  if (blockItem.getBlock() == Blocks.COMPARATOR && this.hmi$repPower) {
                     blockState = (BlockState)blockState.with(ComparatorBlock.POWERED, true);
                  }

                  if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) {
                     blockState = (BlockState)blockState.with(RedstoneTorchBlock.LIT, false);
                  }

                  if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) {
                     blockState = (BlockState)blockState.with(CampfireBlock.LIT, false);
                  }

                  if (item.isIn(ItemTags.BEDS)) {
                     if (bl) {
                        matrices.translate(0.9, 0.0, 0.8);
                     }

                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l)));
                  }

                  blockRenderManager.renderBlockAsEntity(
                     blockState, matrices, MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), light, OverlayTexture.DEFAULT_UV
                  );
                  matrices.pop();
               } else {
                  if (item.isIn(ConventionalItemTags.TOOLS)
                        && !item.isIn(ItemTags.TRIMMABLE_ARMOR)
                        && !item.isIn(ItemTags.BOOKSHELF_BOOKS)
                        && item.getUseAction() != UseAction.EAT
                        && item.isEnchantable()
                     || item.getUseAction() == UseAction.BOW
                     || item.getUseAction() == UseAction.SPYGLASS
                     || this.hmiGetAttackDamage(item) != 0.0F
                     || item.getUseAction() == UseAction.BLOCK
                     || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                     || item.isOf(Items.CARROT_ON_A_STICK)
                     || item.isOf(Items.FISHING_ROD)
                     || item.isOf(Items.SHEARS)) {
                     if (item.isIn(ItemTags.SHOVELS)) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                     } else if (item.getUseAction() == UseAction.SPEAR) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * swing_rot));
                        matrices.translate(0.0, 0.1 * (double)swing_rot, -0.1 * (double)swing_rot);
                     } else if (item.getUseAction() != UseAction.BLOCK) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                        matrices.translate(0.0, 0.05 * (double)swing, -0.05 * (double)swing);
                     }
                  }

                  if (item.isOf(Items.NETHER_STAR) || item.isOf(Items.END_CRYSTAL) && config.mb3DCompat.getValue()) {
                     this.hmi$netherCounter = (float)((double)(Object)this.hmi$netherCounter + 0.9 * tt);
                     matrices.translate(0.0, 0.25 + 0.02 * (double)MathHelper.sin((double)(this.hmi$netherCounter * 0.1F)), 0.0);
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin((double)(this.hmi$netherCounter * 0.2F))));
                     matrices.scale(
                        1.0F + 0.01F * MathHelper.sin((double)(Object)this.hmi$netherCounter),
                        1.0F + 0.01F * MathHelper.sin((double)(Object)this.hmi$netherCounter),
                        1.0F + 0.01F * MathHelper.sin((double)(Object)this.hmi$netherCounter)
                     );
                  } else {
                     this.hmi$netherCounter = 0.0F;
                  }

                  if (config.mb3DCompat.getValue() && (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH))) {
                     this.hmi$vertVelocityYSlime = (float)((double)(Object)this.hmi$vertVelocityYSlime + (double)swingProgress * 0.03 * (double)((float)tt));
                     if ((
                           player.getVelocity().length() > 0.09 && player.isOnGround()
                              || player.isSwimming()
                              || player.isCrawling()
                              || player.isClimbing() && !player.isOnGround()
                        )
                        && (Boolean)(Object)this.client.options.getBobView().getValue()) {
                        Random random = new Random();
                        boolean randomBoolean = random.nextBoolean();
                        this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * (double)((float)tt));
                     }

                     matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                  }

                  if (item.isIn(ItemTags.SHOVELS)) {
                     matrices.translate(0.07 * (double)l, 0.0, 0.05);
                     matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(90 * l)));
                     matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
                  }

                  if (item.isOf(Items.TORCH)) {
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

            matrices.pop();
            matrices.pop();
            this.hmi$isAttacking = this.client.options.attackKey.isPressed();
         }
      }
   }

   @Shadow
   protected abstract void renderFirstPersonItem(
      AbstractClientPlayerEntity var1,
      float var2,
      float var3,
      Hand var4,
      float var5,
      ItemStack var6,
      float var7,
      MatrixStack var8,
      OrderedRenderCommandQueue var9,
      int var10
   );
}

