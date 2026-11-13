package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.events.types.EventPriority;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.repository.friend.FriendUtils;

public class Arrows extends Module {
   private static final String ARROW_TEXTURE = "daamky:textures/particles/ghost-triangle.png";
   private final NumberSetting arrowsDistance = this.register(new NumberSetting("Distance", "Distance from crosshair.", 20.0, 1.0, 20.0, 0.5));
   private final NumberSetting arrowSize = this.register(new NumberSetting("Size", "Arrow texture size.", 15.0, 10.0, 25.0, 0.5));
   private final ColorSetting arrowColor = this.register(new ColorSetting("Color", "Arrow color.", new Color(247, 133, 255)));
   private final ColorSetting friendColor = this.register(new ColorSetting("Friend Color", "Friend arrow color.", new Color(0, 255, 0, 255)));
   private final SmoothAnimation distanceAnimation = new SmoothAnimation();
   private final SmoothAnimation yawAnimation = new SmoothAnimation();
   private final SmoothAnimation pitchAnimation = new SmoothAnimation();
   private final SmoothAnimation cameraYawAnimation = new SmoothAnimation();
   private final List<Arrows.Arrow> arrows = new ArrayList<>();

   public Arrows() {
      super("Arrows", "Draws arrows for players outside the screen center.", ModuleCategory.VISUAL);
   }

   @Override
   protected void onDisable() {
      this.arrows.clear();
   }

   @SubscribeEvent(
      priority = EventPriority.MONITOR
   )
   private void onDraw(DrawEvent event) {
      if (mc.player != null && mc.world != null && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
         this.distanceAnimation.update();
         this.yawAnimation.update();
         this.pitchAnimation.update();
         this.cameraYawAnimation.update();
         float size = 45.0F + this.arrowsDistance.getFloat();
         if (mc.currentScreen instanceof InventoryScreen) {
            size += 80.0F;
         }

         if (mc.player.isSneaking()) {
            size -= 20.0F;
         }

         if (this.isMoving()) {
            size += 10.0F;
         }

         float strafeInput = mc.player.input.getMovementInput().x;
         float forwardInput = mc.player.input.getMovementInput().y;
         this.yawAnimation.run((double)(strafeInput * 5.0F), 0.75, Easings.EXPO_OUT, true);
         this.pitchAnimation.run((double)(forwardInput * 5.0F), 0.75, Easings.EXPO_OUT, true);
         this.cameraYawAnimation.run((double)mc.gameRenderer.getCamera().getYaw(), 0.75, Easings.EXPO_OUT, true);
         this.distanceAnimation.run((double)size, 1.0, Easings.EXPO_OUT, true);

         for (Arrows.Arrow arrow : this.arrows) {
            arrow.active = false;
         }

         for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (this.isValidPlayer(player)) {
               Arrows.Arrow arrow = this.findArrow(player);
               if (arrow == null) {
                  arrow = new Arrows.Arrow(player);
                  this.arrows.add(arrow);
               }

               arrow.active = true;
               arrow.fade.run(1.0, 0.2, Easings.CUBIC_OUT, true);
            }
         }

         for (int i = this.arrows.size() - 1; i >= 0; i--) {
            Arrows.Arrow arrow = this.arrows.get(i);
            if (!arrow.active) {
               arrow.fade.run(0.0, 0.2, Easings.CUBIC_OUT, true);
               if (arrow.fade.get() <= 0.01F && !arrow.fade.isAlive()) {
                  this.arrows.remove(i);
               }
            }
         }

         int screenWidth = mc.getWindow().getScaledWidth();
         int screenHeight = mc.getWindow().getScaledHeight();
         float centerX = (float)screenWidth / 2.0F;
         float centerY = (float)screenHeight / 2.0F;
         double cameraYaw = this.cameraYawAnimation.getValue();
         double cos = (double)MathHelper.cos((double)((float)(cameraYaw * (Math.PI / 180.0))));
         double sin = (double)MathHelper.sin((double)((float)(cameraYaw * (Math.PI / 180.0))));

         for (Arrows.Arrow arrow : this.arrows) {
            arrow.fade.update();
            float alpha = arrow.fade.get();
            if (!(alpha <= 0.01F)) {
               PlayerEntity playerx = arrow.player;
               double playerX = MathHelper.lerp((double)event.getPartialTicks(), playerx.lastRenderX, playerx.getX())
                  - mc.gameRenderer.getCamera().getCameraPos().x;
               double playerZ = MathHelper.lerp((double)event.getPartialTicks(), playerx.lastRenderZ, playerx.getZ())
                  - mc.gameRenderer.getCamera().getCameraPos().z;
               double rotY = -(playerZ * cos - playerX * sin);
               double rotX = -(playerX * cos + playerZ * sin);
               float angle = (float)(Math.atan2(rotY, rotX) * 180.0 / Math.PI);
               float x = (float)(
                  this.distanceAnimation.getValue() * (double)alpha * (double)MathHelper.cos((double)((float)Math.toRadians((double)angle)))
                     + (double)centerX
                     + this.yawAnimation.getValue()
               );
               float y = (float)(
                  this.distanceAnimation.getValue() * (double)alpha * (double)MathHelper.sin((double)((float)Math.toRadians((double)angle)))
                     + (double)centerY
                     + this.pitchAnimation.getValue()
               );
               int baseColor = FriendUtils.isFriend(playerx) ? this.friendColor.getValue().getRGB() : this.arrowColor.getValue().getRGB();
               int color = ColorUtil.multAlpha(baseColor, alpha);
               this.drawArrow(x, y, angle, color);
            }
         }
      }
   }

   private void drawArrow(float x, float y, float angle, int color) {
      float size = this.arrowSize.getFloat();
      float halfSize = size * 0.5F;
      Render2D.image("daamky:textures/particles/ghost-triangle.png", x - halfSize, y - halfSize, size, 0.0F, angle + 90.0F, x, y, color);
   }

   private Arrows.Arrow findArrow(PlayerEntity player) {
      for (Arrows.Arrow arrow : this.arrows) {
         if (arrow.player == player) {
            return arrow;
         }
      }

      return null;
   }

   private boolean isValidPlayer(PlayerEntity player) {
      return player != mc.player && !player.isRemoved();
   }

   private boolean isMoving() {
      return mc.player.input.getMovementInput().y != 0.0F || mc.player.input.getMovementInput().x != 0.0F;
   }

   private static final class Arrow {
      private final PlayerEntity player;
      private final SmoothAnimation fade = new SmoothAnimation();
      private boolean active;

      private Arrow(PlayerEntity player) {
         this.player = player;
         this.fade.set(0.0);
      }
   }
}
