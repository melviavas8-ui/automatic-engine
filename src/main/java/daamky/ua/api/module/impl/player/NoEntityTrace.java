package daamky.ua.api.module.impl.player;

import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;

public final class NoEntityTrace extends Module {
   private static NoEntityTrace instance;
   private final BooleanSetting noSword = this.register(new BooleanSetting("Disable With Sword", "Do not ignore entities while holding sword.", true));

   public NoEntityTrace() {
      super("No Entity Trace", "Ignores entities while raycasting blocks.", ModuleCategory.PLAYER);
      instance = this;
   }

   public static NoEntityTrace getInstance() {
      return instance;
   }

   public boolean shouldIgnoreEntityTrace() {
      Minecraft client = Minecraft.getInstance();
      if (!this.isEnabled() || client.player == null) {
         return false;
      } else if (!this.noSword.getValue()) {
         return true;
      } else {
         ItemStack stack = client.player.getMainHandItem();
         String key = stack.getItem().getDescriptionId().toLowerCase(Locale.ROOT);
         return !key.contains("sword");
      }
   }
}
