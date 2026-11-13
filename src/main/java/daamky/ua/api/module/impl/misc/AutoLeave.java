package daamky.ua.api.module.impl.misc;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.network.Network;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class AutoLeave extends Module {
   private final ModeSetting leaveType = this.register(new ModeSetting("Leave Type", "Leave action.", "Disconnect", "Hub", "Disconnect"));
   private final MultiModeSetting triggers = this.register(
      new MultiModeSetting("Triggers", "Auto leave triggers.", new String[]{"Players", "Staff", "HP"}, "Players", "Staff")
   );
   private final NumberSetting distance = this.register(new NumberSetting("Max Distance", "Player trigger distance.", 10.0, 5.0, 40.0, 1.0));
   private final NumberSetting hp = this.register(new NumberSetting("Minimum HP", "Health trigger threshold.", 8.0, 1.0, 40.0, 1.0));

   public AutoLeave() {
      super("Auto Leave", "Leaves the server when selected danger triggers fire.", ModuleCategory.MISC);
      this.distance.visibleWhen(() -> this.triggers.isSelected("Players"));
      this.hp.visibleWhen(() -> this.triggers.isSelected("HP"));
   }

   @SubscribeEvent
   private void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         if (this.triggers.isSelected("HP")) {
            float health = Network.getResolvedHealth(mc.player, true);
            if (health > 0.0F && health <= this.hp.getFloat()) {
               this.leave(Text.literal(" - HP dropped to " + Network.formatHealthValue(health)));
               return;
            }
         }

         if (!Network.isPvp()) {
            if (this.triggers.isSelected("Players")) {
               for (PlayerEntity player : mc.world.getPlayers()) {
                  if (player != mc.player && !FriendUtils.isFriend(player) && !(mc.player.distanceTo(player) >= this.distance.getFloat())) {
                     this.leave(player.getName().copy().append(" - appeared nearby " + Math.round(mc.player.distanceTo(player)) + "m"));
                     return;
                  }
               }
            }
         }
      }
   }

   private void leave(Text text) {
      if (mc.getNetworkHandler() != null) {
         if (this.leaveType.is("Hub")) {
            mc.getNetworkHandler().sendChatCommand("hub");
         } else {
            mc.getNetworkHandler().getConnection().disconnect(Text.of("[Auto Leave]\n").copy().append(text));
         }

         this.setEnabled(false);
      }
   }
}
