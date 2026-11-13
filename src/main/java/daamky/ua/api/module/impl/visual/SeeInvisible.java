package daamky.ua.api.module.impl.visual;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.NumberSetting;

public class SeeInvisible extends Module {
   private static SeeInvisible instance;
   private final NumberSetting alpha = this.register(new NumberSetting("Alpha", "Invisible entity alpha.", 0.5, 0.1, 1.0, 0.05));

   public SeeInvisible() {
      super("See Invisible", "Allows invisible entities to be rendered.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static boolean shouldRenderInvisible() {
      return instance != null && instance.isEnabled();
   }

   public static float getAlpha() {
      return instance == null ? 0.5F : instance.alpha.getFloat() * 5.0F;
   }
}
