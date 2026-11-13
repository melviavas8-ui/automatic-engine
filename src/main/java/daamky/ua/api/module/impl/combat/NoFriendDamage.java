package daamky.ua.api.module.impl.combat;

import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.InteractEntityEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.utils.repository.friend.FriendUtils;

public class NoFriendDamage extends Module {
   public NoFriendDamage() {
      super("No Friend Damage", "Blocks attacks on friends.", ModuleCategory.COMBAT);
   }

   @SubscribeEvent
   private void onInteract(InteractEntityEvent event) {
      if (FriendUtils.isFriend(event.getEntity())) {
         event.cancel();
      }
   }
}
