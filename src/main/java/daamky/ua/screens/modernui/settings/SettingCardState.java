package daamky.ua.screens.modernui.settings;

import java.util.HashMap;
import java.util.Map;
import daamky.ua.api.settings.Setting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.render.animation.modernfx.Decelerate;
import daamky.ua.utils.render.animation.modernfx.Direction;

final class SettingCardState {
   final Setting<?> setting;
   final Decelerate visibilityAnimation;
   final Decelerate expandAnimation = MathUtils.createAnimation(180, Direction.BACKWARDS);
   final Decelerate booleanAnimation = MathUtils.createAnimation(180, Direction.BACKWARDS);
   final Decelerate colorPickerAnimation = MathUtils.createAnimation(180, Direction.BACKWARDS);
   final Decelerate descriptionAnimation = MathUtils.createAnimation(170, Direction.BACKWARDS);
   final Map<String, Decelerate> optionAnimations = new HashMap<>();
   boolean visible;
   boolean expanded;
   boolean selectedAll;
   boolean colorSynced;
   boolean hoverDescriptionEnabled;
   String hoverDescriptionText = "";
   long hoverDescriptionStartMs;
   String text = "";
   int cursor;
   float rowX;
   float rowY;
   float rowWidth;
   float rowHeight;
   float boxX;
   float boxY;
   float boxWidth;
   float boxHeight;
   float optionX;
   float optionY;
   float optionWidth;
   float optionHeight;
   int optionCount;
   float sliderX;
   float sliderY;
   float sliderWidth;
   float sliderHeight;
   float sliderProgress;
   float colorX;
   float colorY;
   float colorWidth;
   float colorHeight;
   float popupX;
   float popupY;
   float popupWidth;
   float popupHeight;
   float paletteX;
   float paletteY;
   float paletteSize;
   float hueX;
   float hueY;
   float hueWidth;
   float hueHeight;
   float hue;
   float saturation = 1.0F;
   float brightness = 1.0F;
   float visibilityProgress;

   SettingCardState(Setting<?> setting) {
      this.setting = setting;
      this.visibilityAnimation = MathUtils.createAnimation(190, setting.isVisible() ? Direction.FORWARDS : Direction.BACKWARDS);
      this.visibilityProgress = setting.isVisible() ? 1.0F : 0.0F;
      if (setting instanceof StringSetting stringSetting) {
         this.text = stringSetting.getValue();
         this.cursor = this.text.length();
      }

      if (setting instanceof NumberSetting numberSetting) {
         double min = numberSetting.getMin();
         double max = numberSetting.getMax();
         this.sliderProgress = max == min ? 0.0F : (float)((numberSetting.getValue() - min) / (max - min));
      }

      if (setting instanceof BooleanSetting booleanSetting && booleanSetting.getValue()) {
         this.booleanAnimation.setDirection(Direction.FORWARDS);
         this.booleanAnimation.reset();
      }
   }

   void row(float x, float y, float width, float height) {
      this.rowX = x;
      this.rowY = y;
      this.rowWidth = width;
      this.rowHeight = height;
      this.visible = true;
   }

   void box(float x, float y, float width, float height) {
      this.boxX = x;
      this.boxY = y;
      this.boxWidth = width;
      this.boxHeight = height;
   }

   void slider(float x, float y, float width, float height) {
      this.sliderX = x;
      this.sliderY = y;
      this.sliderWidth = width;
      this.sliderHeight = height;
   }

   void options(float x, float y, float width, float height, int count) {
      this.optionX = x;
      this.optionY = y;
      this.optionWidth = width;
      this.optionHeight = height;
      this.optionCount = count;
   }

   Decelerate optionAnimation(String key, boolean selected) {
      return this.optionAnimations.computeIfAbsent(key, ignored -> MathUtils.createAnimation(170, selected ? Direction.FORWARDS : Direction.BACKWARDS));
   }
}
