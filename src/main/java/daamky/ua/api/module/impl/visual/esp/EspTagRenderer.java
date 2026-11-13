package daamky.ua.api.module.impl.visual.esp;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class EspTagRenderer {
   private final PlayerTagResolver playerTags;
   private final EspHealthTracker healthTracker;
   private final List<TagPart> parts = new ArrayList<>(12);

   public EspTagRenderer(PlayerTagResolver playerTags, EspHealthTracker healthTracker) {
      this.playerTags = playerTags;
      this.healthTracker = healthTracker;
   }

   public void drawTag(
      Entity entity,
      EspGeometry.ScreenBox box,
      EspGeometry.ScreenAnchor anchor,
      boolean includeHealth,
      float alpha,
      MultiModeSetting tagElements,
      MinecraftClient mc,
      DrawContext graphics
   ) {
      this.parts.clear();
      ItemStack iconStack = ItemStack.EMPTY;
      boolean roundIcon = false;
      if (entity instanceof ItemEntity item) {
         ItemStack stack = item.getStack();
         iconStack = stack;
         TagParts.addTagPart(this.parts, FontType.SEMIBOLD, TagParts.clean(stack.getName().getString()), EspColors.TAG_TEXT, 0.0F);
         if (stack.getCount() > 1) {
            TagParts.addTagPart(this.parts, FontType.SEMIBOLD, " x" + stack.getCount(), EspColors.TAG_MUTED, 0.0F);
         }
      } else if (entity instanceof PlayerEntity player) {
         PlayerTag tag = this.playerTags.resolve(player, mc);
         if (tagElements.isSelected("Friend Prefix") && FriendUtils.isFriend(player)) {
            TagParts.addTagPart(this.parts, FontType.SEMIBOLD, "(F)", ColorUtil.rgba(42, 255, 118, 255), -0.25F);
         }

         TagParts.addTagParts(this.parts, TagParts.withYOffset(tag.voice(), 0.0F));
         if (tagElements.isSelected("Donation")) {
            TagParts.addTagParts(this.parts, TagParts.withYOffset(TagParts.compactToSingleTextPart(tag.donation(), FontType.BOLD, EspColors.TAG_MUTED), 0.0F));
         }

         if (tagElements.isSelected("Prefix")) {
            TagParts.addTagParts(this.parts, TagParts.withYOffset(TagParts.compactToSingleTextPart(tag.prefix(), FontType.SEMIBOLD, EspColors.TAG_MUTED), 0.0F));
         }

         TagParts.addTagParts(this.parts, TagParts.withYOffset(tag.name(), 0.0F));
         if (tagElements.isSelected("Suffix")) {
            TagParts.addTagParts(this.parts, TagParts.withYOffset(tag.suffix(), 0.0F));
         }

         if (includeHealth && tagElements.isSelected("Health")) {
            TagParts.addTagPart(
               this.parts,
               FontType.BOLD,
               this.healthTracker.formatHealth(this.healthTracker.resolveDisplayHealth(player)),
               EspVisualRenderer.healthTextColor(player, this.healthTracker),
               0.0F
            );
         }
      } else {
         TagParts.addTagPart(this.parts, FontType.SEMIBOLD, this.labelFor(entity, false), EspColors.TAG_TEXT, 0.0F);
      }

      if (includeHealth && entity instanceof LivingEntity living && !(entity instanceof PlayerEntity)) {
         TagParts.addTagPart(
            this.parts,
            FontType.SEMIBOLD,
            " [" + this.healthTracker.formatHealth(this.healthTracker.resolveDisplayHealth(living)) + "HP]",
            EspColors.TAG_HEALTH,
            0.0F
         );
      }

      if (!this.parts.isEmpty() || !iconStack.isEmpty()) {
         PlayerEntity var10000;
         label56: {
            if (entity instanceof PlayerEntity player && tagElements.isSelected("Head")) {
               var10000 = player;
               break label56;
            }

            var10000 = null;
         }

         PlayerEntity headPlayer = var10000;
         EspVisualRenderer.drawTagFrame(headPlayer, this.parts, iconStack, roundIcon, box, anchor, alpha, graphics);
      }
   }

   public void drawHealthBar(EspGeometry.ScreenBox box, LivingEntity entity, float alpha) {
      EspVisualRenderer.drawHealthBar(box, entity, alpha, this.healthTracker);
   }

   public void drawEquipment(PlayerEntity player, EspGeometry.ScreenBox box, EspGeometry.ScreenAnchor anchor, float alpha) {
      EspVisualRenderer.drawEquipment(player, box, anchor, alpha);
   }

   public void drawPotions(LivingEntity entity, EspGeometry.ScreenBox box, float alpha) {
      EspVisualRenderer.drawPotions(entity, box, alpha);
   }

   private String labelFor(Entity entity, boolean includeHealth) {
      if (entity instanceof ItemEntity item) {
         ItemStack stack = item.getStack();
         String name = TagParts.clean(stack.getName().getString());
         return stack.getCount() > 1 ? name + " x" + stack.getCount() : name;
      } else {
         String name = TagParts.clean(entity.getName().getString());
         if (entity instanceof PlayerEntity player && FriendUtils.isFriend(player)) {
            name = "Friend " + name;
         }

         if (includeHealth && entity instanceof LivingEntity living) {
            name = name + " [" + this.healthTracker.formatHealth(this.healthTracker.resolveDisplayHealth(living)) + "HP]";
         }

         return name;
      }
   }
}
