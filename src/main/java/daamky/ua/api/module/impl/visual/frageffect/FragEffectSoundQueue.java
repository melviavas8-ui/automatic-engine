package daamky.ua.api.module.impl.visual.frageffect;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.sound.SoundEvent;
import daamky.ua.utils.sounds.SoundManager;

public final class FragEffectSoundQueue {
   private final List<FragEffectSoundQueue.ScheduledSound> scheduledSounds = new ArrayList<>();

   public FragEffectSoundQueue() {
   }

   public void schedule(SoundEvent sound, long delayMillis, float volume) {
      if (sound != null) {
         if (delayMillis <= 0L) {
            SoundManager.playSoundDirect(sound, volume, 1.0F);
         } else {
            this.scheduledSounds.add(new FragEffectSoundQueue.ScheduledSound(sound, System.currentTimeMillis() + delayMillis, volume));
         }
      }
   }

   public void tick() {
      if (!this.scheduledSounds.isEmpty()) {
         long now = System.currentTimeMillis();
         Iterator<FragEffectSoundQueue.ScheduledSound> iterator = this.scheduledSounds.iterator();

         while (iterator.hasNext()) {
            FragEffectSoundQueue.ScheduledSound scheduledSound = iterator.next();
            if (scheduledSound.playAtMillis <= now) {
               SoundManager.playSoundDirect(scheduledSound.sound, scheduledSound.volume, 1.0F);
               iterator.remove();
            }
         }
      }
   }

   public void clear() {
      this.scheduledSounds.clear();
   }

   private static record ScheduledSound(SoundEvent sound, long playAtMillis, float volume) {
   }
}
