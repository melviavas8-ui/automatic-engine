package daamky.ua.api.module.impl.combat.autoswaputil;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.module.impl.combat.AutoSwap;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public class AutoSwapWheelScreen extends Screen {
   private static final int SECTORS = 3;
   private static final float SECTOR_SIZE = 120.2F;
   private static final float START_ANGLE = -150.0F;
   private static final float SECTOR_GAP = 8.0F;
   private static final long HOVER_SWAP_DELAY_MS = 120L;
   private static final float ICON_SIZE = 18.0F;
   private static final int OPEN_ANIMATION_MS = 180;
   private final Decelerate openAnimation = MathUtils.createAnimation(180, Direction.FORWARDS);
   private final AutoSwap autoSwap;
   private int hoveredSlot = -1;
   private int lastHoveredSlot = -1;
   private long hoverStartMs;
   private boolean hoverSwapTriggered;

   public AutoSwapWheelScreen(AutoSwap autoSwap) {
      super(Text.empty());
      this.autoSwap = autoSwap;
   }

   protected void init() {
      super.init();
      this.hoveredSlot = -1;
      this.lastHoveredSlot = -1;
      this.hoverSwapTriggered = false;
      this.openAnimation.setDirection(Direction.FORWARDS);
      this.openAnimation.reset();
   }

   public boolean shouldPause() {
      return false;
   }

   public boolean shouldCloseOnEsc() {
      return true;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      super.render(context, mouseX, mouseY, deltaTicks);
      Render2D.beginFrame(context);
      RenderItem.beginFrame(context);
      float progress = MathUtils.clamp(this.openAnimation.getOutput().floatValue(), 0.0F, 1.0F);
      Render2D.rect(0.0F, 0.0F, (float)this.width, (float)this.height, this.withAlphaScale(335939080, progress));
      float centerX = (float)this.width * 0.5F;
      float centerY = (float)this.height * 0.5F;
      float baseSize = (float)Math.min(this.width, this.height);
      float baseOuterRadius = baseSize * 0.2F;
      float baseThickness = Math.max(24.0F, baseOuterRadius * 0.11F);
      float outerRadius = baseOuterRadius * (0.84F + progress * 0.16F);
      float thickness = baseThickness * (0.55F + progress * 0.45F);
      float innerRadius = outerRadius - thickness;
      float outerSize = outerRadius * 2.0F;
      float innerSize = innerRadius * 2.0F;
      float iconRadius = (innerRadius + outerRadius) * 0.49F;
      float iconSize = 18.0F * (0.72F + progress * 0.28F);
      float hoverInnerRadius = innerRadius * 0.55F;
      float constrainedMouseX = (float)mouseX;
      float constrainedMouseY = (float)mouseY;
      float dx = constrainedMouseX - centerX;
      float dy = constrainedMouseY - centerY;
      float distance = MathHelper.sqrt(dx * dx + dy * dy);
      if (distance > outerRadius && distance > 0.001F) {
         float scale = outerRadius / distance;
         constrainedMouseX = centerX + dx * scale;
         constrainedMouseY = centerY + dy * scale;
         this.confineCursorToCircle(constrainedMouseX, constrainedMouseY);
      }

      this.hoveredSlot = this.computeHoveredSlot((double)constrainedMouseX, (double)constrainedMouseY, centerX, centerY, hoverInnerRadius, outerRadius);
      this.handleHoverSwap();
      Render2D.blur(centerX - outerRadius, centerY - outerRadius, outerSize, outerSize, outerRadius, 20.0F, 1.0F, this.withAlphaScale(2063597567, progress));
      Render2D.rect(centerX - outerRadius, centerY - outerRadius, outerSize, outerSize, outerRadius, this.withAlphaScale(403378196, progress));
      Render2D.blur(centerX - innerRadius, centerY - innerRadius, innerSize, innerSize, innerRadius, 18.0F, 1.0F, this.withAlphaScale(1929379839, progress));
      Render2D.rect(centerX - innerRadius, centerY - innerRadius, innerSize, innerSize, innerRadius, this.withAlphaScale(604704788, progress));

      for (int i = 0; i < 3; i++) {
         float rotation = -150.0F + (float)i * 120.2F + 4.0F;
         boolean hovered = i == this.hoveredSlot;
         Render2D.arc(
            centerX - outerRadius,
            centerY - outerRadius,
            outerSize,
            thickness,
            112.2F,
            rotation,
            this.withAlphaScale(hovered ? 2147445354 : 1392508927, progress)
         );
         ItemStack stack = this.autoSwap.getWheelSlotStack(i);
         float midAngle = rotation + 56.1F;
         double radians = Math.toRadians((double)midAngle);
         float iconX = centerX + MathHelper.cos((double)((float)radians)) * iconRadius - iconSize * 0.5F;
         float iconY = centerY + MathHelper.sin((double)((float)radians)) * iconRadius - iconSize * 0.6F;
         if (!stack.isEmpty()) {
            RenderItem.item(stack, iconX, iconY, iconSize);
         } else {
            String plus = "+";
            float textSize = 12.5F * (0.75F + progress * 0.25F);
            float textWidth = Render2D.textWidth(FontType.SEMIBOLD, plus, textSize);
            Render2D.text(
               FontType.SEMIBOLD,
               plus,
               iconX + (iconSize - textWidth) * 0.6F,
               iconY + 3.0F * progress,
               textSize,
               this.withAlphaScale(hovered ? -1 : -654311425, progress)
            );
         }
      }

      for (int ix = 0; ix < 3; ix++) {
         float dividerAngle = -150.0F + (float)ix * 120.2F - 1.5F;
         if (ix == 0) {
            dividerAngle -= 8.0F;
         }

         if (ix == 1) {
            dividerAngle += 8.0F;
         }

         this.renderDividerLine(context, centerX, centerY, dividerAngle, innerRadius, outerRadius, progress);
      }

      Render2D.outline360(
         centerX - outerRadius,
         centerY - outerRadius,
         outerSize,
         outerSize,
         outerRadius,
         1.2F,
         this.withAlphaScale(-1694498817, progress),
         Render2D.outline360Range(0.0F, 360.0F, this.withAlphaScale(-1694498817, progress))
      );
      Render2D.outline360(
         centerX - innerRadius,
         centerY - innerRadius,
         innerSize,
         innerSize,
         innerRadius,
         1.0F,
         this.withAlphaScale(1929379839, progress),
         Render2D.outline360Range(0.0F, 360.0F, this.withAlphaScale(1929379839, progress))
      );
      RenderItem.flush();
      Render2D.flush();
   }

   private void renderDividerLine(DrawContext graphics, float centerX, float centerY, float angleDegrees, float innerRadius, float outerRadius, float progress) {
      float lineWidth = 3.0F;
      float ringThickness = outerRadius - innerRadius;
      float lineLength = Math.max(24.0F, ringThickness * 0.42F) * progress;
      float lineRadius = innerRadius + ringThickness * 0.43F;
      float angleRadians = (float)Math.toRadians((double)angleDegrees);
      float lineCenterX = centerX + MathHelper.cos((double)angleRadians) * lineRadius;
      float lineCenterY = centerY + MathHelper.sin((double)angleRadians) * lineRadius;
      float rotationRadians = (float)Math.toRadians((double)(angleDegrees + 90.0F));
      graphics.getMatrices().pushMatrix();
      graphics.getMatrices().translate(lineCenterX, lineCenterY);
      graphics.getMatrices().rotate(rotationRadians);
      Render2D.rect(-lineWidth * 0.5F, -lineLength * 0.5F, lineWidth, lineLength, 0.0F, this.withAlphaScale(-855638017, progress));
      graphics.getMatrices().popMatrix();
   }

   private int withAlphaScale(int color, float scale) {
      int alpha = color >>> 24;
      int scaledAlpha = Math.max(0, Math.min(255, Math.round((float)alpha * MathUtils.clamp(scale, 0.0F, 1.0F))));
      return scaledAlpha << 24 | color & 16777215;
   }

   private void confineCursorToCircle(float guiX, float guiY) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft != null && minecraft.getWindow() != null) {
         double windowWidth = Math.max(1.0, (double)minecraft.getWindow().getFramebufferWidth());
         double windowHeight = Math.max(1.0, (double)minecraft.getWindow().getFramebufferHeight());
         double guiWidth = Math.max(1.0, (double)minecraft.getWindow().getScaledWidth());
         double guiHeight = Math.max(1.0, (double)minecraft.getWindow().getScaledHeight());
         double windowX = (double)guiX * (windowWidth / guiWidth);
         double windowY = (double)guiY * (windowHeight / guiHeight);
         GLFW.glfwSetCursorPos(minecraft.getWindow().getHandle(), windowX, windowY);
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (input.key() == 256) {
         this.close();
         return true;
      } else {
         return super.keyPressed(input);
      }
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      if (this.hoveredSlot < 0 || this.hoveredSlot >= 3) {
         return super.mouseClicked(click, doubled);
      } else if (click.button() == 0) {
         ItemStack stack = this.autoSwap.getWheelSlotStack(this.hoveredSlot);
         this.close();
         if (!stack.isEmpty()) {
            this.autoSwap.startSwapToItemStack(stack);
         }

         return true;
      } else if (click.button() == 1) {
         this.close();
         this.autoSwap.startSelectingItem(this.hoveredSlot);
         return true;
      } else {
         return super.mouseClicked(click, doubled);
      }
   }

   private void handleHoverSwap() {
      if (!this.hoverSwapTriggered) {
         long now = System.currentTimeMillis();
         if (this.hoveredSlot != this.lastHoveredSlot) {
            this.lastHoveredSlot = this.hoveredSlot;
            this.hoverStartMs = now;
         } else if (this.hoveredSlot >= 0 && this.hoveredSlot < 3 && now - this.hoverStartMs >= 120L) {
            ItemStack stack = this.autoSwap.getWheelSlotStack(this.hoveredSlot);
            if (!stack.isEmpty()) {
               this.hoverSwapTriggered = true;
               this.close();
               this.autoSwap.startSwapToItemStack(stack);
            }
         }
      }
   }

   private int computeHoveredSlot(double mouseX, double mouseY, float centerX, float centerY, float innerRadius, float outerRadius) {
      float dx = (float)mouseX - centerX;
      float dy = (float)mouseY - centerY;
      float distance = MathHelper.sqrt(dx * dx + dy * dy);
      if (!(distance < innerRadius) && !(distance > outerRadius)) {
         float angle = (float)Math.toDegrees(Math.atan2((double)dy, (double)dx));
         if (angle < 0.0F) {
            angle += 360.0F;
         }

         float normalizedStart = -150.0F;

         while (normalizedStart < 0.0F) {
            normalizedStart += 360.0F;
         }

         while (normalizedStart >= 360.0F) {
            normalizedStart -= 360.0F;
         }

         float relativeAngle = angle - normalizedStart;
         if (relativeAngle < 0.0F) {
            relativeAngle += 360.0F;
         }

         int index = (int)(relativeAngle / 120.2F);
         return index >= 0 && index < 3 ? index : -1;
      } else {
         return -1;
      }
   }
}
