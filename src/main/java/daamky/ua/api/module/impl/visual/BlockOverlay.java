package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.world.blockoverlay.BlockOverlayShader3D;

public final class BlockOverlay extends Module {
   private static final int MAX_RENDER_BOXES = 256;
   private static final double BOX_RENDER_INSET = 0.01;
   private static final int LINE_GLOW_LAYERS = 4;
   private static final double LINE_GLOW_SPREAD = 0.007;
   private static final List<Box> FULL_BLOCK_BOXES = List.of(new Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0));
   private final ColorSetting fillColor = this.register(new ColorSetting("Fill Color", "Overlay fill color.", new Color(247, 133, 255)));
   private final ColorSetting patternColor = this.register(new ColorSetting("Wave Color", "Animated shine pattern color.", new Color(0, 255, 110, 255)));
   private final ModeSetting shaderMode = this.register(new ModeSetting("Shader Mode", "Block overlay shader style.", "Crystal", "Crystal", "Nebula", "Pulse"));
   private final NumberSetting alpha = this.register(new NumberSetting("Alpha", "Pattern opacity.", 0.62, 0.1, 1.0, 0.05));
   private final NumberSetting waveIntensity = this.register(new NumberSetting("Wave Intensity", "Wave brightness and contrast.", 1.0, 0.2, 2.5, 0.05));
   private final NumberSetting speed = this.register(new NumberSetting("Speed", "Pattern animation speed.", 1.0, 0.1, 5.0, 0.1));
   private final NumberSetting followSmoothness = this.register(
      new NumberSetting("Follow Smoothness", "How softly the overlay moves between blocks.", 3.5, 1.0, 8.0, 0.25)
   );
   private Vec3d smoothedBlockPos;
   private List<Box> cachedBoxes = List.of();
   private final SmoothAnimation visibilityAnimation = new SmoothAnimation();
   private long lastUpdateNanos;

   public BlockOverlay() {
      super("Block Overlay", "Custom overlay for the selected block.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onEnable() {
      this.resetSmoothing();
      this.visibilityAnimation.set(0.0);
   }

   @Override
   protected void onDisable() {
      this.resetSmoothing();
      this.visibilityAnimation.set(0.0);
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null) {
         boolean hasTarget = false;
         if (mc.crosshairTarget instanceof BlockHitResult blockHit && blockHit.getType() == Type.BLOCK) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            if (!this.shouldSkip(state)) {
               List<Box> boxes = this.resolveBoxes(state, pos);
               if (!boxes.isEmpty()) {
                  this.cachedBoxes = boxes;
                  this.updateSmoothing(Vec3d.of(pos));
                  hasTarget = this.smoothedBlockPos != null;
               }
            }
         }

         this.visibilityAnimation.run(hasTarget ? 1.0 : 0.0, hasTarget ? 0.14 : 0.24, Easings.CUBIC_OUT, true);
         this.visibilityAnimation.update();
         float visibility = this.visibilityAnimation.get();
         if (!(visibility <= 0.01F) && this.smoothedBlockPos != null && !this.cachedBoxes.isEmpty()) {
            float pulse = 1.0F + Math.max(0.0F, (float)Math.sin((double)((float)System.currentTimeMillis() / 310.0F))) * 0.018F * visibility;
            int outline = ColorUtil.multAlpha(this.patternColor.getValue().getRGB(), visibility);
            BlockOverlayShader3D.begin();

            for (Box box : this.cachedBoxes) {
               if (this.isSafeBox(box)) {
                  Box renderBox = this.prepareRenderBox(box, pulse);
                  BlockOverlayShader3D.box(renderBox);
                  Render3D.drawBox(renderBox, outline, 0.7F, true, false, false);
               }
            }

            float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
            BlockOverlayShader3D.end(
               this.fillColor.getValue().getRGB(),
               this.patternColor.getValue().getRGB(),
               Math.clamp(this.alpha.getFloat() * visibility, 0.05F, 0.75F),
               time,
               this.speed.getFloat(),
               this.waveIntensity.getFloat(),
               this.shaderModeId(this.shaderMode.getValue())
            );
         }
      } else {
         this.resetSmoothing();
         this.visibilityAnimation.set(0.0);
      }
   }

   private int shaderModeId(String mode) {
      return switch (mode) {
         case "Nebula" -> 1;
         case "Hologram" -> 2;
         case "Pulse" -> 3;
         default -> 0;
      };
   }

   private boolean shouldSkip(BlockState state) {
      return state.isAir()
         || state.isOf(Blocks.SHORT_GRASS)
         || state.isOf(Blocks.TALL_GRASS)
         || state.isOf(Blocks.FERN)
         || state.isOf(Blocks.LARGE_FERN)
         || state.isOf(Blocks.SEAGRASS)
         || state.isOf(Blocks.TALL_SEAGRASS);
   }

   private List<Box> resolveBoxes(BlockState state, BlockPos pos) {
      VoxelShape shape = state.getOutlineShape(mc.world, pos);
      if (shape.isEmpty()) {
         shape = state.getCollisionShape(mc.world, pos);
      }

      if (!shape.isEmpty() && shape != VoxelShapes.fullCube()) {
         List<Box> boxes = shape.getBoundingBoxes();
         return !boxes.isEmpty() && boxes.size() <= 256 ? List.copyOf(boxes) : FULL_BLOCK_BOXES;
      } else {
         return FULL_BLOCK_BOXES;
      }
   }

   private void updateSmoothing(Vec3d targetPos) {
      long nowNanos = System.nanoTime();
      float deltaSeconds;
      if (this.lastUpdateNanos == 0L) {
         deltaSeconds = 0.016666668F;
      } else {
         deltaSeconds = (float)((double)(nowNanos - this.lastUpdateNanos) / 1.0E9);
         deltaSeconds = Math.clamp(deltaSeconds, 0.0F, 0.25F);
      }

      this.lastUpdateNanos = nowNanos;
      if (this.smoothedBlockPos != null && !(this.smoothedBlockPos.distanceTo(targetPos) > 12.0)) {
         float response = 28.0F / this.followSmoothness.getFloat();
         float smoothingFactor = 1.0F - (float)Math.exp((double)(-response * deltaSeconds));
         this.smoothedBlockPos = new Vec3d(
            this.smoothedBlockPos.x + (targetPos.x - this.smoothedBlockPos.x) * (double)smoothingFactor,
            this.smoothedBlockPos.y + (targetPos.y - this.smoothedBlockPos.y) * (double)smoothingFactor,
            this.smoothedBlockPos.z + (targetPos.z - this.smoothedBlockPos.z) * (double)smoothingFactor
         );
      } else {
         this.smoothedBlockPos = targetPos;
      }
   }

   private void resetSmoothing() {
      this.smoothedBlockPos = null;
      this.cachedBoxes = List.of();
      this.lastUpdateNanos = 0L;
   }

   private Box prepareRenderBox(Box box, float visualScale) {
      return this.scaleAroundCenter(box.offset(this.smoothedBlockPos.x, this.smoothedBlockPos.y, this.smoothedBlockPos.z), visualScale).expand(0.01);
   }

   private void drawGlowOutline(Box box, int color, float visibility) {
      for (int i = 4; i >= 1; i--) {
         float progress = (float)i / 4.0F;
         int glowAlpha = Math.round(120.0F * visibility * (1.0F - progress * 0.68F));
         if (glowAlpha > 0) {
            int glowColor = ColorUtil.rgba(ColorUtil.getRed(color), ColorUtil.getGreen(color), ColorUtil.getBlue(color), glowAlpha);
            Render3D.drawBox(box.expand(0.007 * (double)i), glowColor, 2.0F + (float)i * 1.35F, true, false, false);
         }
      }
   }

   private Box scaleAroundCenter(Box box, float scaleFactor) {
      if (box != null && Float.isFinite(scaleFactor) && !(Math.abs(scaleFactor - 1.0F) <= 1.0E-4F)) {
         double centerX = (box.minX + box.maxX) * 0.5;
         double centerY = (box.minY + box.maxY) * 0.5;
         double centerZ = (box.minZ + box.maxZ) * 0.5;
         double halfX = box.getLengthX() * 0.5 * (double)scaleFactor;
         double halfY = box.getLengthY() * 0.5 * (double)scaleFactor;
         double halfZ = box.getLengthZ() * 0.5 * (double)scaleFactor;
         return new Box(centerX - halfX, centerY - halfY, centerZ - halfZ, centerX + halfX, centerY + halfY, centerZ + halfZ);
      } else {
         return box;
      }
   }

   private boolean isSafeBox(Box box) {
      return box != null
         && Double.isFinite(box.minX)
         && Double.isFinite(box.minY)
         && Double.isFinite(box.minZ)
         && Double.isFinite(box.maxX)
         && Double.isFinite(box.maxY)
         && Double.isFinite(box.maxZ)
         && box.getLengthX() <= 2.0
         && box.getLengthY() <= 2.0
         && box.getLengthZ() <= 2.0;
   }
}
