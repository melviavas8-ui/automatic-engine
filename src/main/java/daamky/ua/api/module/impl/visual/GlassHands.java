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
import daamky.ua.utils.render.hands.GlassHandsRenderer;
import daamky.ua.utils.render.hands.HandsFlameRenderer;

public class GlassHands extends Module {
   private static final int CLIENT_COLOR_FIRST = ColorUtil.rgba(127, 242, 255, 230);
   private static final int CLIENT_COLOR_SECOND = ColorUtil.rgba(255, 50, 150, 230);
   private final NumberSetting blurRadius = this.register(new NumberSetting("Blur Radius", "Glass blur strength.", 2.5, 1.0, 5.0, 0.1));
   private final NumberSetting blurIterations = this.register(new NumberSetting("Blur Quality", "Glass blur pass count.", 3.0, 1.0, 5.0, 1.0));
   private final NumberSetting saturation = this.register(new NumberSetting("Saturation", "Color saturation inside glass.", 0.0, 0.0, 2.0, 0.05));
   private final BooleanSetting tint = this.register(new BooleanSetting("Tint", "Adds a colored glass tint.", false));
   private final NumberSetting tintIntensity = this.register(new NumberSetting("Tint Intensity", "Glass tint strength.", 0.2, 0.0, 0.5, 0.05));
   private final ColorSetting tintColor = this.register(new ColorSetting("Tint Color", "Glass tint color.", new Color(0, 255, 255, 255)));
   private final BooleanSetting edgeGlow = this.register(new BooleanSetting("Edge Glow", "Adds glow on glass edges.", true));
   private final NumberSetting edgeGlowIntensity = this.register(new NumberSetting("Edge Glow Intensity", "Glass edge glow strength.", 0.2, 0.0, 1.0, 0.05));
   private final BooleanSetting flame = this.register(new BooleanSetting("Flame", "Adds shader flame around first-person hands.", true));
   private final BooleanSetting onlyItems = this.register(new BooleanSetting("Only Items", "Draw hands flame only when an item is held.", false));
   private final ModeSetting colorMode = this.register(new ModeSetting("Color Mode", "Hands flame color mode.", "Item", "Item", "Custom", "Client", "Rainbow"));
   private final ColorSetting color = this.register(new ColorSetting("Color", "Custom hands flame color.", new Color(255, 255, 255, 230)));
   private final NumberSetting strength = this.register(new NumberSetting("Strength", "Hands flame intensity.", 0.85, 0.0, 2.0, 0.05));
   private final NumberSetting riseSpeed = this.register(new NumberSetting("Rise Speed", "Hands flame upward speed.", 0.0, 0.0, 2.0, 0.05));
   private final NumberSetting wobble = this.register(new NumberSetting("Wobble", "Hands flame side turbulence.", 0.65, 0.0, 2.0, 0.05));
   private final NumberSetting length = this.register(new NumberSetting("Length", "Hands flame trail length.", 0.95, 0.1, 2.5, 0.05));
   private final NumberSetting brightness = this.register(new NumberSetting("Brightness", "Hands flame brightness.", 0.9, 0.0, 2.0, 0.05));

   public GlassHands() {
      super("Glass Hands", "Makes first-person hands and items look like glass with flame effects.", ModuleCategory.VISUAL);
      this.tintIntensity.visibleWhen(this.tint::getValue);
      this.tintColor.visibleWhen(this.tint::getValue);
      this.edgeGlowIntensity.visibleWhen(this.edgeGlow::getValue);
      this.onlyItems.visibleWhen(this.flame::getValue);
      this.colorMode.visibleWhen(this.flame::getValue);
      this.color.visibleWhen(() -> this.flame.getValue() && this.colorMode.is("Custom"));
      this.strength.visibleWhen(this.flame::getValue);
      this.riseSpeed.visibleWhen(this.flame::getValue);
      this.wobble.visibleWhen(this.flame::getValue);
      this.length.visibleWhen(this.flame::getValue);
      this.brightness.visibleWhen(this.flame::getValue);
   }

   @Override
   protected void onEnable() {
      GlassHandsRenderer.setEnabled(true);
      this.syncRenderer();
   }

   @Override
   protected void onDisable() {
      GlassHandsRenderer.setEnabled(false);
      HandsFlameRenderer.setFlameEnabled(false);
   }

   @Override
   public void onTick(MinecraftClient client) {
      this.syncRenderer();
   }

   private void syncRenderer() {
      GlassHandsRenderer.configure(
         this.blurRadius.getFloat(),
         (int)Math.round(this.blurIterations.getValue()),
         this.saturation.getFloat(),
         true,
         this.tint.getValue() ? this.tintColor.getValue().getRGB() : 0,
         this.tint.getValue() ? this.tintIntensity.getFloat() : 0.0F,
         this.edgeGlow.getValue() ? this.edgeGlowIntensity.getFloat() : 0.0F
      );
      HandsFlameRenderer.configure(
         this.strength.getFloat(),
         this.riseSpeed.getFloat(),
         this.wobble.getFloat(),
         this.length.getFloat(),
         this.brightness.getFloat(),
         this.colorMode.is("Item") ? 0 : 1,
         this.resolveFlameColor(),
         this.onlyItems.getValue()
      );
      HandsFlameRenderer.setFlameEnabled(this.flame.getValue());
   }

   private int resolveFlameColor() {
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
