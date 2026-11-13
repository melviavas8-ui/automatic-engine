package daamky.ua.api.module.impl.visual;

import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public final class HolMyItems extends Module {
   private static HolMyItems instance;
   public final ModeSetting hmiAnimationType = this.register(
      new ModeSetting("Animation", "Main hand swing style.", "Flow", "Flow", "Really", "Sharp", "Spin", "Heavy", "Stab", "Cross")
   );
   public final BooleanSetting swapHands = new BooleanSetting("Swap Hands", "Mirrors the first-person hand layout.", false);
   public final NumberSetting hmiSmoothness = this.register(new NumberSetting("Smoothness", "Smoothness of custom hand attacks.", 1.0, 0.35, 2.5, 0.05));
   public final NumberSetting rightHandX = this.register(new NumberSetting("Right Hand X", "Moves the right hand left or right.", 0.0, -1.0, 1.0, 0.05));
   public final NumberSetting rightHandZ = this.register(new NumberSetting("Right Hand Z", "Moves the right hand forward or backward.", 0.0, -1.0, 1.0, 0.05));
   public final NumberSetting leftHandX = this.register(new NumberSetting("Left Hand X", "Moves the left hand left or right.", 0.0, -1.0, 1.0, 0.05));
   public final NumberSetting leftHandZ = this.register(new NumberSetting("Left Hand Z", "Moves the left hand forward or backward.", 0.0, -1.0, 1.0, 0.05));
   public final BooleanSetting climbAndCrawl = new BooleanSetting("Climb & Crawl", "Animates hands while climbing and crawling.", true);
   public final BooleanSetting swimmingAnimation = new BooleanSetting("Swimming", "Animates hands while swimming.", true);
   public final ModeSetting swimmingStyle = new ModeSetting("Swimming Style", "Selects the first-person swimming motion.", "Classic", "Classic");
   public final BooleanSetting mb3DCompat = new BooleanSetting("3D Items", "Uses the custom three-dimensional item pose.", true);
   public final BooleanSetting itemAnimations = new BooleanSetting(
      "Item Motions", "Weighted draw and release motions for bows, crossbows, tridents, shields, rods and food.", true
   );

   public HolMyItems() {
      super("HoldMyItems", "Custom first-person hands and held-item animations.", ModuleCategory.VISUAL);
      instance = this;
      this.swimmingStyle.visibleWhen(this.swimmingAnimation::getValue);
   }

   public static HolMyItems getInstance() {
      return instance;
   }
}
