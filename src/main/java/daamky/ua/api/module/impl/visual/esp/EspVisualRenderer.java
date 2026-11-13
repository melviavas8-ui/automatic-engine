package daamky.ua.api.module.impl.visual.esp;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.render.item.RenderItemOptions;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.render.world.blockoverlay.BlockOverlayShader3D;

public final class EspVisualRenderer {
   private EspVisualRenderer() {
   }

   public static void draw2DBox(EspGeometry.ScreenBox box, int color, float alpha) {
      float x = roundHalf((float)box.minX());
      float y = roundHalf((float)box.minY());
      float width = Math.max(2.0F, roundHalf((float)box.width()));
      float height = Math.max(2.0F, roundHalf((float)box.height()));
      float length = Math.max(5.0F, Math.min(Math.min(width * 0.34F, height * 0.22F), 18.0F));
      drawCornerSegments(x, y, width, height, length, 3.1F, ColorUtil.rgba(0, 0, 0, Math.round(155.0F * alpha)));
      drawCornerSegments(x, y, width, height, length, 1.35F, ColorUtil.multAlpha(color, alpha));
   }

   public static void draw3DCornerBox(
      Box box,
      int color,
      float width,
      boolean shaderFill,
      int shaderColor,
      float shaderAlpha,
      float shaderTime,
      float shaderSpeed,
      float shaderIntensity,
      int shaderMode
   ) {
      if (shaderFill && shaderAlpha > 0.0F) {
         BlockOverlayShader3D.setMatrices(Render3D.lastProjMat, Render3D.lastModMat);
         BlockOverlayShader3D.begin();
         BlockOverlayShader3D.box(box);
         BlockOverlayShader3D.end(color, shaderColor, Math.clamp(shaderAlpha, 0.0F, 1.0F), shaderTime, shaderSpeed, shaderIntensity, shaderMode);
      }

      double lenX = Math.max(0.08, Math.min(box.getLengthX() * 0.38, 0.34));
      double lenY = Math.max(0.18, Math.min(box.getLengthY() * 0.22, 0.48));
      double lenZ = Math.max(0.08, Math.min(box.getLengthZ() * 0.38, 0.34));
      int renderColor = ColorUtil.multAlpha(color, 0.92F);

      for (int xi = 0; xi < 2; xi++) {
         for (int yi = 0; yi < 2; yi++) {
            for (int zi = 0; zi < 2; zi++) {
               double x = xi == 0 ? box.minX : box.maxX;
               double y = yi == 0 ? box.minY : box.maxY;
               double z = zi == 0 ? box.minZ : box.maxZ;
               double dx = xi == 0 ? lenX : -lenX;
               double dy = yi == 0 ? lenY : -lenY;
               double dz = zi == 0 ? lenZ : -lenZ;
               Vec3d corner = new Vec3d(x, y, z);
               Render3D.drawLineOverlay(corner, new Vec3d(x + dx, y, z), renderColor, width);
               Render3D.drawLineOverlay(corner, new Vec3d(x, y + dy, z), renderColor, width);
               Render3D.drawLineOverlay(corner, new Vec3d(x, y, z + dz), renderColor, width);
            }
         }
      }
   }

   public static float resolve3DLineWidth(MinecraftClient mc, Box box) {
      double distance = mc.gameRenderer.getCamera().getCameraPos().distanceTo(box.getCenter());
      return (float)MathHelper.clamp(2.4 - distance * 0.012, 1.05, 2.4);
   }

   public static void drawTagFrame(
      PlayerEntity player,
      List<TagPart> parts,
      ItemStack iconStack,
      boolean roundIcon,
      EspGeometry.ScreenBox box,
      EspGeometry.ScreenAnchor anchor,
      float alpha,
      DrawContext graphics
   ) {
      boolean playerTag = player != null;
      float textSize = 6.0F;
      float textWidth = 0.0F;

      for (TagPart part : parts) {
         textWidth += Render2D.textWidth(part.font(), part.text(), textSize);
      }

      boolean hasIcon = !iconStack.isEmpty();
      float headSize = playerTag ? 8.0F : 0.0F;
      float headGap = playerTag && !parts.isEmpty() ? 3.2F : 0.0F;
      float iconSize = hasIcon ? 9.0F : 0.0F;
      float iconBgSize = hasIcon ? iconSize + 3.0F : 0.0F;
      float iconGap = hasIcon && !parts.isEmpty() ? 2.8F : 0.0F;
      float paddingX = 3.2F;
      float paddingY = 1.8F;
      float height = Math.max(textSize + paddingY * 2.0F + 1.0F, Math.max(hasIcon ? iconBgSize + 2.0F : 0.0F, playerTag ? headSize + 2.0F : 0.0F));
      float totalWidth = textWidth + headSize + headGap + iconBgSize + iconGap;
      double anchorX = anchor != null ? anchor.x() : box.centerX();
      double anchorY = anchor != null ? anchor.y() : box.minY();
      float x = (float)(anchorX - (double)(totalWidth * 0.5F));
      float y = (float)anchorY - height - 1.0F + 0.0F;
      float backgroundX = x - (hasIcon && parts.isEmpty() ? 0.0F : paddingX);
      float backgroundWidth = totalWidth + (hasIcon && parts.isEmpty() ? 0.0F : paddingX * 2.0F);
      Render2D.rect(backgroundX, y - 1.0F, backgroundWidth, height + 2.0F, 3.0F, ColorUtil.rgba(0, 0, 0, Math.round(185.0F * alpha)));
      float drawX = x;
      if (playerTag) {
         drawPlayerHead(graphics, player, x, y + (height - headSize) * 0.5F + 0.0F, headSize, alpha);
         drawX = x + headSize + headGap;
      }

      if (hasIcon) {
         float iconBgY = y + (height - iconBgSize) * 0.5F + 0.0F;
         if (roundIcon) {
            Render2D.rect(drawX, iconBgY, iconBgSize, iconBgSize, iconBgSize * 0.5F, ColorUtil.rgba(22, 22, 22, Math.round(170.0F * alpha)));
         }

         RenderItem.item(iconStack, drawX + 1.5F, iconBgY + 1.5F, iconSize, RenderItemOptions.noDecorations(alpha));
         drawX += iconBgSize + iconGap;
      }

      float textY = y + (height - textSize) * 0.5F - 0.75F;

      for (TagPart part : parts) {
         Render2D.text(part.font(), part.text(), drawX, textY + 0.0F + part.yOffset(), textSize, ColorUtil.multAlpha(part.color(), alpha));
         drawX += Render2D.textWidth(part.font(), part.text(), textSize);
      }
   }

   public static void drawCompactItemTag(List<EspVisualRenderer.ItemTagRow> rows, double anchorX, double anchorY, float alpha, DrawContext graphics) {
      if (rows != null && !rows.isEmpty()) {
         float textSize = 6.2F;
         float rowHeight = 11.0F;
         float iconSize = 8.5F;
         float iconGap = 3.0F;
         float paddingX = 4.2F;
         float paddingY = 3.0F;
         float countGap = 4.0F;
         float nameWidth = 0.0F;
         float countWidth = 0.0F;

         for (EspVisualRenderer.ItemTagRow row : rows) {
            nameWidth = Math.max(nameWidth, Render2D.textWidth(FontType.SEMIBOLD, row.name(), textSize));
            countWidth = Math.max(countWidth, Render2D.textWidth(FontType.SEMIBOLD, compactCount(row.count()), textSize));
         }

         float width = paddingX * 2.0F + iconSize + iconGap + nameWidth + countGap + countWidth;
         float height = paddingY * 2.0F + (float)rows.size() * rowHeight;
         float x = (float)anchorX - width * 0.5F;
         float y = (float)anchorY - height - 2.0F + 0.0F;
         Render2D.rect(x, y, width, height, 3.0F, ColorUtil.rgba(0, 0, 0, Math.round(185.0F * alpha)));
         RenderItemOptions itemOptions = RenderItemOptions.noDecorations(alpha);
         float rowY = y + paddingY;

         for (EspVisualRenderer.ItemTagRow row : rows) {
            float iconY = rowY + (rowHeight - iconSize) * 0.5F;
            RenderItem.item(row.stack(), x + paddingX, iconY, iconSize, itemOptions);
            float textY = rowY + (rowHeight - textSize) * 0.5F - 0.65F;
            float textX = x + paddingX + iconSize + iconGap;
            Render2D.text(FontType.SEMIBOLD, row.name(), textX, textY, textSize, ColorUtil.rgba(245, 245, 245, Math.round(255.0F * alpha)));
            String count = compactCount(row.count());
            float rowCountWidth = Render2D.textWidth(FontType.SEMIBOLD, count, textSize);
            Render2D.text(
               FontType.SEMIBOLD, count, x + width - paddingX - rowCountWidth, textY, textSize, ColorUtil.rgba(165, 165, 165, Math.round(245.0F * alpha))
            );
            rowY += rowHeight;
         }
      }
   }

   public static void drawHealthBar(EspGeometry.ScreenBox box, LivingEntity entity, float alpha, EspHealthTracker healthTracker) {
      float health = healthTracker.resolveDisplayHealth(entity);
      float maxHealth = Math.max(1.0F, entity.getMaxHealth() + entity.getAbsorptionAmount());
      maxHealth = Math.max(maxHealth, health);
      float progress = Math.clamp(health / maxHealth, 0.0F, 1.0F);
      float x = (float)box.minX() - 4.6F;
      float y = (float)box.minY();
      float height = (float)box.height();
      float fillHeight = Math.max(1.0F, height * progress);
      int healthColor = ColorUtil.lerpColor(ColorUtil.rgba(255, 70, 70, 240), ColorUtil.rgba(80, 255, 120, 240), progress);
      Render2D.rect(x, y, 2.1F, height, 1.0F, ColorUtil.rgba(0, 0, 0, Math.round(135.0F * alpha)));
      Render2D.rect(x, y + height - fillHeight, 2.1F, fillHeight, 1.0F, ColorUtil.multAlpha(healthColor, alpha));
   }

   public static int healthTextColor(LivingEntity entity, EspHealthTracker healthTracker) {
      float health = healthTracker.resolveDisplayHealth(entity);
      float maxHealth = Math.max(1.0F, entity.getMaxHealth() + entity.getAbsorptionAmount());
      maxHealth = Math.max(maxHealth, health);
      return ColorUtil.lerpColor(ColorUtil.rgba(255, 70, 70, 255), ColorUtil.rgba(70, 255, 115, 255), Math.clamp(health / maxHealth, 0.0F, 1.0F));
   }

   public static void drawEquipment(PlayerEntity player, EspGeometry.ScreenBox box, EspGeometry.ScreenAnchor anchor, float alpha) {
      ItemStack offhand = player.getOffHandStack();
      ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
      ItemStack chestplate = player.getEquippedStack(EquipmentSlot.CHEST);
      ItemStack leggings = player.getEquippedStack(EquipmentSlot.LEGS);
      ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
      ItemStack mainHand = player.getMainHandStack();
      int itemCount = equipmentCount(offhand, helmet, chestplate, leggings, boots, mainHand);
      if (itemCount != 0) {
         float totalWidth = (float)itemCount * 10.0F + (float)(itemCount - 1) * 1.4F;
         double anchorX = anchor != null ? anchor.x() : box.centerX();
         double anchorY = anchor != null ? anchor.y() : box.minY();
         float x = (float)(anchorX - (double)(totalWidth * 0.5F));
         float y = (float)anchorY - 27.0F;
         RenderItemOptions options = RenderItemOptions.decorated(alpha);
         x = drawEquipmentItem(offhand, x, y, options);
         x = drawEquipmentItem(helmet, x, y, options);
         x = drawEquipmentItem(chestplate, x, y, options);
         x = drawEquipmentItem(leggings, x, y, options);
         x = drawEquipmentItem(boots, x, y, options);
         drawEquipmentItem(mainHand, x, y, options);
      }
   }

   public static void drawPotions(LivingEntity entity, EspGeometry.ScreenBox box, float alpha) {
      if (!entity.getStatusEffects().isEmpty()) {
         float x = (float)box.maxX() + 4.0F;
         float y = (float)box.minY() + 1.0F;
         int shown = 0;

         for (StatusEffectInstance effect : entity.getStatusEffects()) {
            if (shouldRenderEffect(effect)) {
               if (shown >= 5) {
                  break;
               }

               Render2D.effectIcon(effect, x, y - 0.4F, 7.0F, ColorUtil.rgba(255, 255, 255, Math.round(235.0F * alpha)));
               Render2D.text(FontType.SEMIBOLD, effectText(effect), x + 8.2F, y + 0.1F, 5.8F, ColorUtil.rgba(235, 235, 235, Math.round(230.0F * alpha)));
               y += 8.5F;
               shown++;
            }
         }
      }
   }

   private static boolean shouldRenderEffect(StatusEffectInstance effect) {
      return effect != null && effect.getEffectType() != null && ((StatusEffect)effect.getEffectType().value()).getCategory() != StatusEffectCategory.NEUTRAL;
   }

   private static void drawPlayerHead(DrawContext graphics, PlayerEntity player, float x, float y, float size, float alpha) {
      if (graphics != null && player instanceof AbstractClientPlayerEntity clientPlayer && !(size <= 0.0F) && !(alpha <= 0.0F)) {
         Identifier skinTexture = clientPlayer.getSkin().body().texturePath();
         String texture = skinTexture.toString();
         int color = ColorUtil.rgba(255, 255, 255, Math.round(255.0F * Math.clamp(alpha, 0.0F, 1.0F)));
         Render2D.imageUvNearest(texture, x, y, size, size, 2.0F, 1.0F, 0.125F, 0.125F, 0.25F, 0.25F, color);
         Render2D.imageUvNearest(texture, x, y, size, size, 2.0F, 1.0F, 0.625F, 0.125F, 0.75F, 0.25F, color);
         return;
      }
   }

   private static String compactCount(int count) {
      return count > 1 ? "x" + count : "x1";
   }

   private static String effectText(StatusEffectInstance effect) {
      String name = ((StatusEffect)effect.getEffectType().value()).getName().getString();
      int level = effect.getAmplifier() + 1;
      return name + (level > 1 ? " " + level : "") + " " + formatDuration(effect.getDuration());
   }

   private static String formatDuration(int ticks) {
      if (ticks >= 0 && ticks < 72000) {
         int seconds = Math.max(0, ticks / 20);
         return seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + seconds % 60;
      } else {
         return "**:**";
      }
   }

   private static int equipmentCount(ItemStack offhand, ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots, ItemStack mainHand) {
      int count = 0;
      if (!offhand.isEmpty()) {
         count++;
      }

      if (!helmet.isEmpty()) {
         count++;
      }

      if (!chestplate.isEmpty()) {
         count++;
      }

      if (!leggings.isEmpty()) {
         count++;
      }

      if (!boots.isEmpty()) {
         count++;
      }

      if (!mainHand.isEmpty()) {
         count++;
      }

      return count;
   }

   private static float drawEquipmentItem(ItemStack stack, float x, float y, RenderItemOptions options) {
      if (!stack.isEmpty()) {
         RenderItem.item(stack, x, y, 8.0F, options);
         return x + 11.4F;
      } else {
         return x;
      }
   }

   private static void drawCornerSegments(float x, float y, float width, float height, float length, float thickness, int color) {
      float right = x + width;
      float bottom = y + height;
      float half = thickness * 0.5F;
      Render2D.rect(x - half, y - half, length, thickness, 0.0F, color);
      Render2D.rect(x - half, y - half, thickness, length, 0.0F, color);
      Render2D.rect(right - length + half, y - half, length, thickness, 0.0F, color);
      Render2D.rect(right - half, y - half, thickness, length, 0.0F, color);
      Render2D.rect(x - half, bottom - half, length, thickness, 0.0F, color);
      Render2D.rect(x - half, bottom - length + half, thickness, length, 0.0F, color);
      Render2D.rect(right - length + half, bottom - half, length, thickness, 0.0F, color);
      Render2D.rect(right - half, bottom - length + half, thickness, length, 0.0F, color);
   }

   private static float roundHalf(float value) {
      return (float)Math.round(value * 2.0F) / 2.0F;
   }

   public static record ItemTagRow(ItemStack stack, String name, int count) {
   }
}
