package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.world.targetesp.ChainTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.CircleRenderer;
import daamky.ua.utils.render.world.targetesp.CircleTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.CrystalEsp;
import daamky.ua.utils.render.world.targetesp.GhostTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.Lightning1TargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.LightningTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.MarkerTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.RhombTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.SkullTargetEspRenderer;
import daamky.ua.utils.render.world.targetesp.TargetEspMath;
import daamky.ua.utils.render.world.targetesp.TargetEspRenderContext;
import daamky.ua.utils.render.world.targetesp.VortexEsp;

public class TargetESP extends Module {
   private static TargetESP instance;
   private final SmoothAnimation alphaAnimation = new SmoothAnimation();
   private final ModeSetting mode = this.register(
      new ModeSetting("Mode", "Target highlight type.", "Ghost", "Rhomb", "Rhomb 2", "Ghost", "Chain", "Skull", "Circle", "Lightning", "Vortex", "Crystal")
   );
   private final ModeSetting lightningMode = this.register(new ModeSetting("Lightning Mode", "Lightning target ESP style.", "1", "1", "2"));
   private final ModeSetting circleMode = this.register(new ModeSetting("Circle Mode", "Circle target ESP style.", "1", "1", "2"));
   private final ColorSetting color1 = this.register(new ColorSetting("Color", "Primary target ESP color.", new Color(247, 133, 255)));
   private final NumberSetting lightningGlow = this.register(
      new NumberSetting("Lightning Glow", "Size and strength of the shader lightning halo.", 1.8, 0.0, 3.0, 0.05)
   );
   private final NumberSetting ringSize = this.register(new NumberSetting("Ring Size", "Circle target ESP ring size.", 1.0, 0.5, 2.0, 0.05));
   private final NumberSetting ringSpeed = this.register(new NumberSetting("Ring Speed", "Circle target ESP animation speed.", 4.0, 0.5, 12.0, 0.5));
   private final BooleanSetting ringParticles = this.register(new BooleanSetting("Ring Particles", "Circle target ESP particles.", true));
   private final NumberSetting ringParticleHeight = this.register(
      new NumberSetting("Ring Particle Height", "Circle target ESP particle vertical spread.", 1.0, 0.1, 1.0, 0.05)
   );
   private final NumberSetting ringParticleLife = this.register(
      new NumberSetting("Ring Particle Life", "Circle target ESP particle lifetime.", 1.0, 0.3, 3.0, 0.1)
   );
   private final BooleanSetting ringRedOnImpact = this.register(new BooleanSetting("Ring Red on Impact", "Circle target ESP turns red on impact.", true));
   private final NumberSetting ringImpactFadeIn = this.register(
      new NumberSetting("Ring Impact Fade In", "Circle target ESP impact fade in speed.", 0.3, 0.05, 1.0, 0.01)
   );
   private final NumberSetting ringImpactFadeOut = this.register(
      new NumberSetting("Ring Impact Fade Out", "Circle target ESP impact fade out speed.", 0.08, 0.01, 0.5, 0.01)
   );
   private final NumberSetting ringImpactIntensity = this.register(
      new NumberSetting("Ring Impact Intensity", "Circle target ESP impact red intensity.", 1.0, 0.1, 1.0, 0.05)
   );
   private Vec3d smoothedPos;
   private LivingEntity lastTarget;
   private float hurtProgress;
   private float chainImpactProgress;
   private long lastFrameTime = System.currentTimeMillis();

   public TargetESP() {
      super("Target ESP", "Highlights the current Aura target.", ModuleCategory.VISUAL);
      instance = this;
      this.alphaAnimation.set(0.0);
      this.circleMode.visibleWhen(() -> this.mode.is("Circle"));
      this.lightningMode.visibleWhen(() -> this.mode.is("Lightning"));
      this.lightningGlow.visibleWhen(() -> this.mode.is("Lightning") && this.lightningMode.is("2"));
      this.ringSize.visibleWhen(this::usesCommonEffectSettings);
      this.ringSpeed.visibleWhen(this::usesCommonEffectSettings);
      this.ringParticles.visibleWhen(() -> this.mode.is("Circle") && this.circleMode.is("1"));
      this.ringParticleHeight.visibleWhen(() -> this.mode.is("Circle") && this.ringParticles.getValue() && this.circleMode.is("1"));
      this.ringParticleLife.visibleWhen(() -> this.mode.is("Circle") && this.ringParticles.getValue() && this.circleMode.is("1"));
      this.ringRedOnImpact.visibleWhen(() -> this.usesCommonEffectSettings() || this.mode.is("Lightning") && this.lightningMode.is("1"));
      this.ringImpactFadeIn.visibleWhen(() -> this.mode.is("Circle") && this.ringRedOnImpact.getValue() && this.circleMode.is("1"));
      this.ringImpactFadeOut.visibleWhen(() -> this.mode.is("Circle") && this.ringRedOnImpact.getValue() && this.circleMode.is("1"));
      this.ringImpactIntensity.visibleWhen(() -> this.mode.is("Circle") && this.ringRedOnImpact.getValue() && this.circleMode.is("1"));
   }

   public static TargetESP getInstance() {
      return instance;
   }

   @Override
   protected void onDisable() {
      this.alphaAnimation.set(0.0);
      this.smoothedPos = null;
      this.lastTarget = null;
      this.hurtProgress = 0.0F;
      this.chainImpactProgress = 0.0F;
      CircleTargetEspRenderer.reset();
      CircleRenderer.reset();
      LightningTargetEspRenderer.reset();
   }

   @SubscribeEvent
   private void onRender3D(WorldRenderEvent event) {
      long frameTime = System.currentTimeMillis();
      float deltaTime = Math.max(1.0F, Math.min((float)(frameTime - this.lastFrameTime), 100.0F)) / 16.666666F;
      this.lastFrameTime = frameTime;
      LivingEntity target = AuraModule.getInstance() != null && AuraModule.getInstance().isEnabled() ? AuraModule.target : null;
      if (target != null && target.isAlive()) {
         if (this.lastTarget == target && this.smoothedPos != null) {
            this.smoothToTarget(target, event.getPartialTicks());
         } else {
            this.lastTarget = target;
            this.smoothedPos = target.getLerpedPos(event.getPartialTicks());
         }

         this.alphaAnimation.run(1.0, 0.25, Easings.CUBIC_OUT, true);
      } else {
         this.alphaAnimation.run(0.0, 0.25, Easings.CUBIC_OUT, true);
      }

      this.alphaAnimation.update();
      float alpha = MathHelper.clamp(this.alphaAnimation.get(), 0.0F, 1.0F);
      if (!(alpha <= 0.01F) && this.lastTarget != null && this.smoothedPos != null) {
         this.hurtProgress = this.lastTarget.hurtTime > 0 ? (float)this.lastTarget.hurtTime / 10.0F : Math.max(0.0F, this.hurtProgress - 0.1F * deltaTime);
         float impactTarget = this.lastTarget.hurtTime > 0 ? 1.0F : 0.0F;
         float impactStep = (impactTarget > this.chainImpactProgress ? 0.3F : 0.1F) * deltaTime;
         this.chainImpactProgress = TargetEspMath.approach(this.chainImpactProgress, impactTarget, impactStep);
         Camera camera = mc.gameRenderer.getCamera();
         Vec3d cameraPos = camera.getCameraPos();
         Quaternionf cameraRotation = new Quaternionf(camera.getRotation());
         float cameraFade = this.computeCameraFade(this.lastTarget, cameraPos);
         float renderAlpha = alpha * cameraFade;
         if (!(renderAlpha <= 0.01F)) {
            MatrixStack stack = event.getStack();
            Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();
            stack.push();
            stack.translate(this.smoothedPos.x - cameraPos.x, this.smoothedPos.y - cameraPos.y, this.smoothedPos.z - cameraPos.z);
            TargetEspRenderContext context = new TargetEspRenderContext(
               this.lastTarget,
               this.smoothedPos,
               cameraPos,
               cameraRotation,
               renderAlpha,
               event.getPartialTicks(),
               frameTime,
               this.color1.getValue().getRGB(),
               this.color1.getValue().getRGB(),
               this.hurtProgress,
               this.chainImpactProgress
            );
            if (this.mode.is("Rhomb")) {
               RhombTargetEspRenderer.render(stack, provider, context);
               RhombTargetEspRenderer.endBatch(provider);
            } else if (this.mode.is("Rhomb 2")) {
               MarkerTargetEspRenderer.render(stack, provider, context);
               MarkerTargetEspRenderer.endBatch(provider);
            } else if (this.mode.is("Ghost")) {
               GhostTargetEspRenderer.render(stack, provider, context);
               GhostTargetEspRenderer.endBatch(provider);
            } else if (this.mode.is("Chain")) {
               ChainTargetEspRenderer.render(stack, provider, context);
               ChainTargetEspRenderer.endBatch(provider);
            } else if (this.mode.is("Skull")) {
               SkullTargetEspRenderer.render(stack, provider, context);
               SkullTargetEspRenderer.endBatch(provider);
            } else if (this.mode.is("Lightning")) {
               if (this.lightningMode.is("2")) {
                  Lightning1TargetEspRenderer.render(stack, provider, context, this.lightningGlow.getFloat());
                  Lightning1TargetEspRenderer.endBatch(provider);
               } else {
                  LightningTargetEspRenderer.render(stack, provider, context, this.ringRedOnImpact.getValue());
               }
            } else if (this.mode.is("Crystal")) {
               CrystalEsp.render(stack, provider, context, this.ringSize.getFloat(), this.ringSpeed.getFloat(), this.ringRedOnImpact.getValue());
            } else if (this.mode.is("Vortex")) {
               VortexEsp.configure(this.ringSize.getFloat(), this.ringSpeed.getFloat() / 4.0F, this.ringRedOnImpact.getValue());
               VortexEsp.render(stack, provider, context);
               VortexEsp.endBatch(provider);
            } else if (this.mode.is("Circle")) {
               CircleTargetEspRenderer.Settings settings = new CircleTargetEspRenderer.Settings(
                  this.ringSize.getFloat(),
                  this.ringSpeed.getFloat(),
                  this.ringParticles.getValue(),
                  this.ringParticleHeight.getFloat(),
                  this.ringParticleLife.getFloat(),
                  this.ringRedOnImpact.getValue(),
                  this.ringImpactFadeIn.getFloat(),
                  this.ringImpactFadeOut.getFloat(),
                  this.ringImpactIntensity.getFloat()
               );
               if (this.circleMode.is("2")) {
                  CircleRenderer.render(stack, provider, context, settings);
                  CircleRenderer.endBatch(provider);
               } else {
                  CircleTargetEspRenderer.render(stack, provider, context, settings);
                  CircleTargetEspRenderer.endBatch(provider);
               }
            }

            stack.pop();
         }
      }
   }

   private void smoothToTarget(LivingEntity target, float partialTicks) {
      Vec3d targetPos = target.getLerpedPos(partialTicks);
      float smoothing = Math.min(1.0F, Math.max(0.12F, partialTicks * 1.5F));
      this.smoothedPos = new Vec3d(
         this.smoothedPos.x + (targetPos.x - this.smoothedPos.x) * (double)smoothing,
         this.smoothedPos.y + (targetPos.y - this.smoothedPos.y) * (double)smoothing,
         this.smoothedPos.z + (targetPos.z - this.smoothedPos.z) * (double)smoothing
      );
   }

   public Boolean redik() {
      return this.ringRedOnImpact.getValue();
   }

   public int speed1() {
      return (int)this.ringSpeed.getFloat();
   }

   private boolean usesCommonEffectSettings() {
      return this.mode.is("Circle") || this.mode.is("Crystal") || this.mode.is("Vortex");
   }

   private float computeCameraFade(LivingEntity target, Vec3d cameraPos) {
      Box fadeZone = target.getBoundingBox().expand(0.1F);
      double distanceToBounds = Math.sqrt(fadeZone.squaredMagnitude(cameraPos));
      float fadeRange = Math.max(0.55F, target.getWidth() * 0.85F);
      float fade = MathHelper.clamp((float)(distanceToBounds / (double)fadeRange), 0.0F, 1.0F);
      return 0.35F + fade * 0.65F;
   }
}
