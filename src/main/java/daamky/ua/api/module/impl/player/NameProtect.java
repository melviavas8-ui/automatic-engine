package daamky.ua.api.module.impl.player;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TextFactoryEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class NameProtect extends Module {
   private static NameProtect instance;
   private final StringSetting nameSetting = this.register(new StringSetting("Name", "Replacement nickname.", "daamky.fun", 32));
   private final BooleanSetting friendsSetting = this.register(
      new BooleanSetting("Friends", "Hide friend nicknames when friend repository is available.", true)
   );

   public NameProtect() {
      super("Name Protect", "Hides your nickname in rendered text.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static Text protectOwnNameTag(Entity entity, Text nameTag) {
      NameProtect protect = instance;
      if (protect != null && protect.isEnabled() && entity != null && nameTag != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.getSession() != null && entity == client.player) {
            String realName = client.getSession().getUsername();
            String replacement = protect.nameSetting.getValue();
            if (realName != null && !realName.isBlank() && replacement != null && !replacement.isBlank()) {
               String text = nameTag.getString();
               return (Text)(!text.contains(realName) ? nameTag : Text.literal(text.replace(realName, replacement)));
            } else {
               return nameTag;
            }
         } else {
            return nameTag;
         }
      } else {
         return nameTag;
      }
   }

   public static String protectPlayerName(PlayerEntity player, String name) {
      NameProtect protect = instance;
      if (protect != null && protect.isEnabled() && player != null && name != null && !name.isBlank()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.getSession() != null) {
            boolean self = player == client.player;
            boolean friend = protect.friendsSetting.getValue() && FriendUtils.isFriend(player);
            if (!self && !friend) {
               return name;
            } else {
               String realName = self ? client.getSession().getUsername() : player.getName().getString();
               String replacement = protect.nameSetting.getValue();
               return realName != null && !realName.isBlank() && replacement != null && !replacement.isBlank() ? name.replace(realName, replacement) : name;
            }
         } else {
            return name;
         }
      } else {
         return name;
      }
   }

   @SubscribeEvent
   private void onTextFactory(TextFactoryEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.getSession() != null) {
         event.replaceText(client.getSession().getUsername(), this.nameSetting.getValue());
      }
   }
}
