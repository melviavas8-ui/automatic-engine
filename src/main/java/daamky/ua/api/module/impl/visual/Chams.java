package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class Chams extends Module {
   private static Chams instance;
   private static final String TARGET_ALL = "All";
   private static final String TARGET_PLAYERS = "Players";
   private static final String TARGET_ENEMIES = "Enemies";
   private static final String TARGET_FRIENDS = "Friends";
   private static final String TARGET_MOBS = "Mobs";
   private static final String TARGET_ANIMALS = "Animals";
   private static final String TARGET_OTHER = "Other";
   private final MultiModeSetting targets = this.register(
      new MultiModeSetting(
         "Targets", "Entities that should be highlighted.", new String[]{"All", "Players", "Enemies", "Friends", "Mobs", "Animals", "Other"}, "All"
      )
   );
   private final BooleanSetting useThemeColor = this.register(new BooleanSetting("Theme Color", "Use the client accent color.", true));
   private final ModeSetting style = this.register(new ModeSetting("Style", "Chams render style.", "Default", "Default", "Glass"));
   private final ColorSetting customColor = this.register(new ColorSetting("Color", "Chams color.", new Color(255, 100, 100, 255)));
   private final NumberSetting fillAlpha = this.register(new NumberSetting("Fill Alpha", "Model fill transparency.", 135.0, 5.0, 255.0, 1.0));
   private final BooleanSetting throughWalls = this.register(new BooleanSetting("Through Walls", "Render chams model fill through walls.", true));

   public Chams() {
      super("Chams", "Highlights players with translucent chams.", ModuleCategory.VISUAL);
      this.customColor.visibleWhen(() -> !this.useThemeColor.getValue());
      instance = this;
   }

   public static Chams getInstance() {
      return instance;
   }

   public boolean shouldRender(Entity entity) {
      if (this.isEnabled() && entity != null && mc.player != null) {
         if (entity == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return false;
         } else if (this.targets.isSelected("All")) {
            return true;
         } else if (entity instanceof PlayerEntity) {
            boolean friend = FriendUtils.isFriend(entity);
            return friend
               ? this.targets.isSelected("Friends") || this.targets.isSelected("Players")
               : this.targets.isSelected("Enemies") || this.targets.isSelected("Players");
         } else if (entity instanceof AnimalEntity) {
            return this.targets.isSelected("Animals");
         } else {
            return entity instanceof MobEntity ? this.targets.isSelected("Mobs") : this.targets.isSelected("Other");
         }
      } else {
         return false;
      }
   }

   public int getColor(Entity entity) {
      return this.resolveColor(entity, this.getFillAlpha());
   }

   public int getFillAlpha() {
      return Math.clamp((long)((int)Math.round(this.fillAlpha.getValue())), 5, 255);
   }

   public boolean isThroughWalls() {
      return this.throughWalls.getValue();
   }

   public boolean isGlassStyle() {
      return this.style.is("Glass");
   }

   private int resolveColor(Entity entity, int alphaValue) {
      int safeAlpha = Math.clamp((long)alphaValue, 0, 255);
      if (FriendUtils.isFriend(entity)) {
         if (this.isGlassStyle()) {
            int time = (int)(System.currentTimeMillis() / 8L & 255L);
            return ColorUtil.rgba(time, 220, 120, safeAlpha);
         } else {
            return this.encodeChamsTime(ColorUtil.rgba(0, 220, 80, safeAlpha));
         }
      } else {
         int baseColor = this.useThemeColor.getValue() ? ColorUtil.rgba(247, 133, 255, 255) : this.customColor.getValue().getRGB();
         if (this.isGlassStyle()) {
            int time = (int)(System.currentTimeMillis() / 8L & 255L);
            return ColorUtil.rgba(time, ColorUtil.getGreen(baseColor), ColorUtil.getBlue(baseColor), safeAlpha);
         } else {
            return this.encodeChamsTime(ColorUtil.withAlpha(baseColor, safeAlpha));
         }
      }
   }

   private int encodeChamsTime(int color) {
      int time = (int)(System.currentTimeMillis() / 8L & 255L);
      int red = ColorUtil.getRed(color) & 240 | time & 15;
      int green = ColorUtil.getGreen(color) & 240 | time >> 4 & 15;
      return ColorUtil.rgba(red, green, ColorUtil.getBlue(color), ColorUtil.getAlpha(color));
   }
}
