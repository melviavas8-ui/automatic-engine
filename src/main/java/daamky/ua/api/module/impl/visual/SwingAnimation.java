package daamky.ua.api.module.impl.visual;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.HandAnimationEvent;
import daamky.ua.api.events.impl.SwingDurationEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public class SwingAnimation extends Module {
   private final ModeSetting swingType = this.register(
      new ModeSetting(
         "Type", "", "Chop", "Chop", "Swipe", "Down", "Smooth", "Smooth 2", "Power", "Feast", "Twist", "Stab", "Helix", "Default", "Carrot", "SwipeD"
      )
   );
   private final NumberSetting hitStrength = this.register(new NumberSetting("Strength", "", 1.0, 0.5, 3.0, 0.05));
   private final NumberSetting swingSpeed = this.register(new NumberSetting("Duration", "", 1.0, 0.5, 4.0, 0.05));
   private final BooleanSetting onlySwing = this.register(new BooleanSetting("Only Swing", "", false));
   private final BooleanSetting onlyAura = this.register(new BooleanSetting("Only Aura", "", false));
   private final NumberSetting sizehand = this.register(new NumberSetting("Size Hand", "", 1.0, 0.1, 2.0, 0.1F));

   public SwingAnimation() {
      super("Swing Animation", "Custom first-person swing animations.", ModuleCategory.VISUAL);
   }

   @SubscribeEvent
   private void onSwingDuration(SwingDurationEvent event) {
      if (!this.isBlockedForSwingAnimation(Hand.MAIN_HAND) && !this.isBlockedForSwingAnimation(Hand.OFF_HAND)) {
         if (this.passesAuraFilter()) {
            event.setAnimation(this.swingSpeed.getFloat());
            event.cancel();
         }
      }
   }

   @SubscribeEvent
   private void onHandAnimation(HandAnimationEvent event) {
      if (mc.player != null && event.getHand() == Hand.MAIN_HAND && !this.isBlockedForSwingAnimation(event.getHand()) && this.passesAuraFilter()) {
         MatrixStack matrices = event.getMatrices();
         float swingProgress = event.getSwingProgress();
         int armSide = mc.player.getMainArm() == Arm.RIGHT ? 1 : -1;
         float sin1 = MathHelper.sin((double)(swingProgress * swingProgress * (float) Math.PI));
         float sin2 = MathHelper.sin((double)(MathHelper.sqrt(swingProgress) * (float) Math.PI));
         float sinSmooth = (float)(Math.sin((double)swingProgress * Math.PI) * 0.5);
         float strength = this.hitStrength.getFloat();
         if (this.onlySwing.getValue() && mc.player.handSwingTicks == 0) {
            matrices.translate((float)armSide * 0.56F, -0.52F, -0.72F);
         } else if (this.swingType.is("Chop")) {
            matrices.translate(0.56F * (float)armSide, -0.44F, -0.72F);
            matrices.translate(0.0F, -0.19800001F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * -20.0F * (float)armSide * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
            matrices.translate(0.4F, 0.2F, 0.2F);
            matrices.translate(-0.5F, 0.08F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(20.0F));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Twist")) {
            matrices.translate((float)armSide * 0.56F, -0.36F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(80 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60.0F * (float)armSide * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
            matrices.translate(0.0F, -0.1F, 0.05F);
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("SwipeD")) {
            float wildSize = 3.7F * strength;
            matrices.translate((float)armSide * 0.67F, -0.32F, -1.0F);
            matrices.translate(sin2 * -wildSize / 35.0F, 0.0F, sin2 * -wildSize / 35.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -wildSize * 5.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(50.0F));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Swipe")) {
            matrices.translate(0.56F * (float)armSide, -0.32F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -120.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Carrot")) {
            float vanillaSin1 = MathHelper.sin((double)(swingProgress * swingProgress * (float) Math.PI));
            float vanillaSin2 = MathHelper.sin((double)(MathHelper.sqrt(swingProgress) * (float) Math.PI));
            matrices.translate((float)armSide * 0.56F, -0.52F, -0.72F);
            float scale = this.sizehand.getFloat();
            matrices.scale(scale, scale, scale);
            matrices.translate((float)armSide * -0.4F * vanillaSin1 * strength, 0.2F * vanillaSin2 * strength, -0.2F * vanillaSin2 * strength);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)armSide * 45.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)armSide * vanillaSin1 * -20.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)armSide * vanillaSin2 * -20.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(vanillaSin2 * -80.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)armSide * -45.0F));
         } else if (this.swingType.is("Default")) {
            matrices.translate((float)armSide * 0.56F, -0.52F - sin2 * 0.5F * strength, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F * (float)armSide));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Down")) {
            matrices.translate((float)armSide * 0.56F, -0.32F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Smooth")) {
            matrices.translate((float)armSide * 0.56F, -0.42F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)armSide * (45.0F + sin1 * -20.0F * strength)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)armSide * sin2 * -20.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)armSide * -45.0F));
            matrices.translate(0.0F, -0.1F, 0.0F);
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Smooth 2")) {
            matrices.translate((float)armSide * 0.56F, -0.42F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength));
            matrices.translate(0.0F, -0.1F, 0.0F);
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Power")) {
            matrices.translate((float)armSide * 0.56F, -0.32F, -0.72F);
            matrices.translate(-sinSmooth * sinSmooth * sin1 * (float)armSide * strength, 0.0F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -30.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F * strength));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Feast")) {
            matrices.translate((float)armSide * 0.56F, -0.32F, -0.72F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * (float)armSide * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * (float)armSide));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * (float)armSide));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Stab")) {
            matrices.translate((float)armSide * 0.66F, -0.32F, -0.92F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(25 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 90.0F * (float)armSide * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -15.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(50 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(70 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(2 * armSide)));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         } else if (this.swingType.is("Helix")) {
            matrices.translate((float)armSide * 0.66F, -0.32F, -0.92F);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(35 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 15.0F * (float)armSide * strength));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -55.0F * strength));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(50 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(70 * armSide)));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float)(2 * armSide)));
            matrices.scale(this.sizehand.getFloat(), this.sizehand.getFloat(), this.sizehand.getFloat());
         }

         event.cancel();
      }
   }

   private boolean passesAuraFilter() {
      AuraModule aura = AuraModule.getInstance();
      return !this.onlyAura.getValue() || aura != null && aura.isEnabled() && AuraModule.target != null;
   }

   private boolean isBlockedForSwingAnimation(Hand hand) {
      return mc.player != null && this.isExcludedSwingItem(mc.player.getStackInHand(hand));
   }

   private boolean isExcludedSwingItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (!(stack.getItem() instanceof CrossbowItem) && !(stack.getItem() instanceof TridentItem)) {
         String path = Registries.ITEM.getId(stack.getItem()).getPath();
         return path.contains("spear");
      } else {
         return true;
      }
   }
}
