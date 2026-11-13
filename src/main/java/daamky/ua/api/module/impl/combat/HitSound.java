package daamky.ua.api.module.impl.combat;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.entity.LivingEntity;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.AttackEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.sounds.SoundManager;

public class HitSound extends Module {
   private final ModeSetting soundType = this.register(new ModeSetting("Sound", "Hit sound type.", "Moans", "Moans", "Metallic", "Crime"));
   private final NumberSetting volume = this.register(new NumberSetting("Volume", "Sound volume.", 1.0, 0.1, 2.0, 0.1));

   public HitSound() {
      super("Hit Sound", "Plays a sound on hit.", ModuleCategory.COMBAT);
   }

   @SubscribeEvent
   private void onAttack(AttackEvent event) {
      if (event.getTarget() instanceof LivingEntity) {
         this.playSelectedSound();
      }
   }

   private void playSelectedSound() {
      float vol = this.volume.getFloat();
      if (this.soundType.is("Metallic")) {
         SoundManager.playSound(SoundManager.METALLIC, vol, 1.0F);
      } else if (this.soundType.is("Crime")) {
         SoundManager.playSound(SoundManager.CRIME, vol, 1.0F);
      } else {
         switch (ThreadLocalRandom.current().nextInt(4)) {
            case 0:
               SoundManager.playSound(SoundManager.MOAN1, vol, 1.0F);
               break;
            case 1:
               SoundManager.playSound(SoundManager.MOAN2, vol, 1.0F);
               break;
            case 2:
               SoundManager.playSound(SoundManager.MOAN3, vol, 1.0F);
               break;
            default:
               SoundManager.playSound(SoundManager.MOAN4, vol, 1.0F);
         }
      }
   }
}
