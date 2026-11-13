package daamky.ua.api.module.impl.misc;

import net.minecraft.sound.SoundEvent;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.ModuleToggleEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.sounds.SoundManager;

public final class ClientSounds extends Module {
   private static final long CLICK_GUI_SOUND_DEBOUNCE_MS = 120L;
   private static final float CLICK_GUI_SOUND_VOLUME_MULTIPLIER = 2.5F;
   private static ClientSounds instance;
   private static long lastClickGuiOpeningSoundMs;
   private static long lastClickGuiClosingSoundMs;
   private static long lastSwitchCategorySoundMs;
   private final ModeSetting soundType = this.register(new ModeSetting("Sound Type", "Client sound type.", "New", "New", "Old", "Alternative"));
   private final NumberSetting volume = this.register(new NumberSetting("Volume", "Client sound volume.", 1.0, 0.0, 1.0, 0.05));
   private final NumberSetting pitch = this.register(new NumberSetting("Pitch", "Client sound pitch.", 1.0, 0.5, 2.0, 0.05));
   private final BooleanSetting searchTypingSound = this.register(new BooleanSetting("Search Typing", "Search typing sound.", true));
   private final BooleanSetting sliderSound = this.register(new BooleanSetting("Slider", "Slider move sound.", true));
   private final BooleanSetting switchCategorySound = this.register(new BooleanSetting("Switch Category", "Category switch sound.", true));
   private final BooleanSetting noSettingsSound = this.register(new BooleanSetting("No Settings", "No settings sound.", true));
   private final BooleanSetting buttonClickSound = this.register(new BooleanSetting("Button", "Button click sound.", true));
   private final BooleanSetting clickGuiOpeningSound = this.register(new BooleanSetting("Opening", "ClickGUI opening sound.", true));
   private final BooleanSetting clickGuiClosingSound = this.register(new BooleanSetting("Closing", "ClickGUI closing sound.", true));
   private final BooleanSetting unknownCommandSound = this.register(new BooleanSetting("Unknown Command", "Unknown command sound.", true));

   public ClientSounds() {
      super("Client Sounds", "Client sounds for module and GUI actions.", ModuleCategory.MISC);
      instance = this;
   }

   public static ClientSounds getInstance() {
      return instance;
   }

   @SubscribeEvent
   private void onModuleToggle(ModuleToggleEvent event) {
      if (event.getModule() != this) {
         this.playToggleSound(event.isEnabled());
      }
   }

   @Override
   protected void onEnable() {
      this.playToggleSound(true);
   }

   @Override
   protected void onDisable() {
      this.playToggleSound(false);
   }

   private void playToggleSound(boolean enabled) {
      float vol = this.volume.getFloat();
      float tone = this.pitch.getFloat();
      if (this.soundType.is("Old")) {
         SoundManager.playSoundDirect(enabled ? SoundManager.ON : SoundManager.OFF, vol, tone);
      } else if (this.soundType.is("Alternative")) {
         SoundManager.playSoundDirect(enabled ? SoundManager.MODULE_ENABLE_TYPE2 : SoundManager.MODULE_DISABLE_TYPE2, vol, tone);
      } else {
         SoundManager.playSoundDirect(enabled ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, vol, tone);
      }
   }

   public float getVolume() {
      return this.volume.getFloat();
   }

   public float getPitch() {
      return this.pitch.getFloat();
   }

   public static boolean allowSearchTypingSound() {
      ClientSounds sounds = instance;
      return sounds != null && sounds.isEnabled() && sounds.searchTypingSound.getValue();
   }

   public static boolean allowSliderSound() {
      ClientSounds sounds = instance;
      return sounds != null && sounds.isEnabled() && sounds.sliderSound.getValue();
   }

   public static boolean allowSwitchCategorySound() {
      ClientSounds sounds = instance;
      return sounds != null && sounds.isEnabled() && sounds.switchCategorySound.getValue();
   }

   public static void playSwitchCategorySound() {
      ClientSounds sounds = instance;
      if (sounds == null || sounds.switchCategorySound.getValue()) {
         long now = System.currentTimeMillis();
         if (now - lastSwitchCategorySoundMs >= 120L) {
            lastSwitchCategorySoundMs = now;
            float volume = (sounds == null ? 1.0F : sounds.getVolume()) * 2.5F;
            SoundManager.playSoundDirect(SoundManager.SWITCH_CATEGORY, volume, sounds == null ? 1.0F : sounds.getPitch());
         }
      }
   }

   public static boolean allowNoSettingsSound() {
      ClientSounds sounds = instance;
      return sounds != null && sounds.isEnabled() && sounds.noSettingsSound.getValue();
   }

   public static boolean allowButtonClickSound() {
      ClientSounds sounds = instance;
      return sounds != null && sounds.isEnabled() && sounds.buttonClickSound.getValue();
   }

   public static void playClickGuiOpeningSound() {
      playClickGuiSound(SoundManager.MENU_OPEN, true);
   }

   public static void playClickGuiClosingSound() {
      playClickGuiSound(SoundManager.MENU_CLOSE, false);
   }

   private static void playClickGuiSound(SoundEvent sound, boolean opening) {
      ClientSounds sounds = instance;
      if (sound != null) {
         if (sounds == null || opening ? sounds.clickGuiOpeningSound.getValue() : sounds.clickGuiClosingSound.getValue()) {
            long now = System.currentTimeMillis();
            if (opening) {
               if (now - lastClickGuiOpeningSoundMs < 120L) {
                  return;
               }

               lastClickGuiOpeningSoundMs = now;
            } else {
               if (now - lastClickGuiClosingSoundMs < 120L) {
                  return;
               }

               lastClickGuiClosingSoundMs = now;
            }

            float volume = (sounds == null ? 1.0F : sounds.getVolume()) * 2.5F;
            SoundManager.playSoundDirect(sound, volume, sounds == null ? 1.0F : sounds.getPitch());
         }
      }
   }

   public static boolean allowUnknownCommandSound() {
      ClientSounds sounds = instance;
      return sounds != null && sounds.isEnabled() && sounds.unknownCommandSound.getValue();
   }
}
