package daamky.ua.api.module.impl.visual;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.NumberSetting;

public class AspectRatio extends Module {
   private static AspectRatio instance;
   private final NumberSetting ratio = this.register(new NumberSetting("Ratio", "Screen aspect ratio.", 1.5, 1.0, 2.0, 0.01));

   public AspectRatio() {
      super("Aspect Ratio", "Changes the world projection aspect ratio.", ModuleCategory.VISUAL);
      instance = this;
   }

   public static float resolveRatio(float currentWidth, float currentHeight) {
      AspectRatio module = instance;
      return module != null && module.isEnabled() && !(currentHeight <= 0.0F) ? currentHeight * module.ratio.getFloat() : currentWidth;
   }
}
