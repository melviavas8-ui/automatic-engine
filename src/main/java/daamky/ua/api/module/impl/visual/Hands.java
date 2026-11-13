package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.hands.HandsFlameRenderer;

public class Hands extends Module {
   private static final int CLIENT_COLOR_FIRST = ColorUtil.rgba(127, 242, 255, 230);
   private static final int CLIENT_COLOR_SECOND = ColorUtil.rgba(255, 50, 150, 230);
   private final BooleanSetting onlyItems = this.register(new BooleanSetting("Only Items", "Draw hands flame only when an item is held.", false));
   private final ModeSetting colorMode = this.register(new ModeSetting("Color Mode", "Hands flame color mode.", "Item", "Item", "Custom", "Client", "Rainbow"));
   private final ColorSetting color = this.register(new ColorSetting("Color", "Custom hands flame color.", new Color(247, 133, 255)));
   private final NumberSetting strength = this.register(new NumberSetting("Strength", "Hands flame intensity.", 0.85, 0.0, 2.0, 0.05));
   private final NumberSetting riseSpeed = this.register(new NumberSetting("Rise Speed", "Hands flame upward speed.", 0.0, 0.0, 2.0, 0.05));
   private final NumberSetting wobble = this.register(new NumberSetting("Wobble", "Hands flame side turbulence.", 0.65, 0.0, 2.0, 0.05));
   private final NumberSetting length = this.register(new NumberSetting("Length", "Hands flame trail length.", 0.95, 0.1, 2.5, 0.05));
   private final NumberSetting brightness = this.register(new NumberSetting("Brightness", "Hands flame brightness.", 0.9, 0.0, 2.0, 0.05));

   public Hands() {
      super("Hands", "Draws a shader flame effect around first-person hands.", ModuleCategory.VISUAL);
      this.color.visibleWhen(() -> this.colorMode.is("Custom"));
   }

   @Override
   protected void onEnable() {
      this.syncRenderer();
      HandsFlameRenderer.setFlameEnabled(true);
   }

   @Override
   protected void onDisable() {
      HandsFlameRenderer.setFlameEnabled(false);
   }

   @Override
   public void onTick(MinecraftClient client) {
      this.syncRenderer();
   }

   private void syncRenderer() {
      int colorModeId = this.colorMode.is("Item") ? 0 : 1;
      HandsFlameRenderer.configure(
         this.strength.getFloat(),
         this.riseSpeed.getFloat(),
         this.wobble.getFloat(),
         this.length.getFloat(),
         this.brightness.getFloat(),
         colorModeId,
         this.resolveColor(),
         this.onlyItems.getValue()
      );
   }

   private int resolveColor() {
      if (this.colorMode.is("Custom")) {
         return this.color.getValue().getRGB();
      } else if (this.colorMode.is("Client")) {
         float pulse = (float)((Math.sin((double)System.currentTimeMillis() / 520.0) + 1.0) * 0.5);
         return ColorUtil.lerpColor(CLIENT_COLOR_FIRST, CLIENT_COLOR_SECOND, pulse);
      } else if (this.colorMode.is("Rainbow")) {
         float hue = (float)(System.currentTimeMillis() % 4000L) / 4000.0F;
         return -436207616 | Color.HSBtoRGB(hue, 0.72F, 1.0F) & 16777215;
      } else {
         return this.color.getValue().getRGB();
      }
   }
}
