package daamky.ua.api.module.impl.visual;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public final class Animations extends Module {
   public static final String CHAT = "Chat";
   public static final String TAB = "Tab";
   public static final String INVENTORY = "Inventory";
   public static final String CONTAINERS = "Containers";
   public static final String BUTTONS = "Buttons";
   public static final String PERSPECTIVE = "Perspective";
   private static final long CHAT_OPEN_MS = 240L;
   private static final long CHAT_CLOSE_MS = 220L;
   private static final long TAB_OPEN_MS = 300L;
   private static final long TAB_CLOSE_MS = 240L;
   private static final long SCREEN_OPEN_MS = 140L;
   private static final long SCREEN_CLOSE_MS = 120L;
   private static final float CHAT_SLIDE = 30.0F;
   private static Animations instance;
   private final MultiModeSetting targets = this.register(
      new MultiModeSetting(
         "Animate",
         "Interfaces to animate.",
         new String[]{"Chat", "Tab", "Inventory", "Containers", "Buttons", "Perspective"},
         "Chat",
         "Tab",
         "Inventory",
         "Containers",
         "Perspective"
      )
   );
   private final ModeSetting easing = this.register(
      new ModeSetting(
         "Easing",
         "Animation curve.",
         "Ease Out Back",
         "Linear",
         "Ease Out Quad",
         "Ease Out Cubic",
         "Ease Out Quart",
         "Ease Out Expo",
         "Ease Out Back",
         "Ease Out Elastic",
         "Ease Out Bounce"
      )
   );
   private final NumberSetting chatSpeed = this.register(new NumberSetting("Chat Speed", "Chat animation speed.", 1.0, 0.1, 3.0, 0.1));
   private final NumberSetting tabSpeed = this.register(new NumberSetting("Tab Speed", "Tab list animation speed.", 1.0, 0.1, 3.0, 0.1));
   private final NumberSetting inventorySpeed = this.register(new NumberSetting("Inventory Speed", "Inventory animation speed.", 1.0, 0.1, 3.0, 0.1));
   private final NumberSetting containerSpeed = this.register(new NumberSetting("Container Speed", "Container animation speed.", 1.0, 0.1, 3.0, 0.1));
   private final NumberSetting buttonSpeed = this.register(new NumberSetting("Button Speed", "Widget hover speed.", 1.0, 0.1, 3.0, 0.1));
   private final NumberSetting perspectiveSpeed = this.register(new NumberSetting("F5 Speed", "Perspective switch speed.", 1.0, 0.1, 3.0, 0.1));
   private final Animations.Fade chatFade = new Animations.Fade();
   private final Animations.Fade tabFade = new Animations.Fade();
   private final Animations.Fade screenFade = new Animations.Fade();
   private final Animations.Fade perspectiveFade = new Animations.Fade();
   private boolean chatClosing;
   private long chatCloseStart;
   private boolean chatReleased;
   private Screen animatedScreen;
   private boolean screenClosing;
   private long screenCloseStart;
   private boolean screenReleased;
   private boolean perspectiveInitialized;

   public Animations() {
      super("Animations", "Open / close animations for chat, tab, inventory and widgets.", ModuleCategory.VISUAL);
      this.chatSpeed.visibleWhen(() -> this.targets.isSelected("Chat"));
      this.tabSpeed.visibleWhen(() -> this.targets.isSelected("Tab"));
      this.inventorySpeed.visibleWhen(() -> this.targets.isSelected("Inventory"));
      this.containerSpeed.visibleWhen(() -> this.targets.isSelected("Containers"));
      this.buttonSpeed.visibleWhen(() -> this.targets.isSelected("Buttons"));
      this.perspectiveSpeed.visibleWhen(() -> this.targets.isSelected("Perspective"));
      instance = this;
   }

   public static Animations getInstance() {
      return instance;
   }

   private static boolean active(String target) {
      Animations animations = instance;
      return animations != null && animations.isEnabled() && animations.targets.isSelected(target);
   }

   @Override
   protected void onDisable() {
      this.resetChat();
      this.resetScreen();
      this.tabFade.reset(0.0F);
      this.perspectiveFade.reset(0.0F);
      this.perspectiveInitialized = false;
   }

   public static boolean chatEnabled() {
      return active("Chat");
   }

   public static float chatOffset() {
      Animations animations = instance;
      if (animations != null && chatEnabled()) {
         long duration = animations.chatClosing ? scale(220L, animations.chatSpeed) : scale(240L, animations.chatSpeed);
         float raw = animations.chatFade.update(duration, animations.chatClosing);
         return (1.0F - animations.ease(raw)) * 30.0F;
      } else {
         return 0.0F;
      }
   }

   public static boolean deferChatClose() {
      Animations animations = instance;
      if (animations == null || !chatEnabled() || animations.chatReleased) {
         return false;
      } else if (animations.chatClosing) {
         animations.resetChat();
         return false;
      } else {
         animations.chatClosing = true;
         animations.chatCloseStart = System.currentTimeMillis();
         return true;
      }
   }

   public static void resetChatState() {
      Animations animations = instance;
      if (animations != null) {
         animations.resetChat();
      }
   }

   private void resetChat() {
      this.chatClosing = false;
      this.chatCloseStart = 0L;
      this.chatFade.reset(0.0F);
   }

   public static boolean tabEnabled() {
      return active("Tab");
   }

   public static boolean keepTabVisible(boolean pressed) {
      Animations animations = instance;
      if (animations != null && tabEnabled()) {
         long duration = pressed ? scale(300L, animations.tabSpeed) : scale(240L, animations.tabSpeed);
         float raw = animations.tabFade.update(duration, !pressed);
         return pressed || raw > 0.001F;
      } else {
         return pressed;
      }
   }

   public static float tabScale() {
      Animations animations = instance;
      return animations != null && tabEnabled() ? Math.max(0.0F, animations.ease(animations.tabFade.value())) : 1.0F;
   }

   public static boolean screenEnabled(Screen screen) {
      if (!(screen instanceof HandledScreen)) {
         return false;
      } else {
         return screen instanceof InventoryScreen ? active("Inventory") : active("Containers");
      }
   }

   public static float screenScale(Screen screen) {
      Animations animations = instance;
      if (animations != null && screenEnabled(screen)) {
         if (animations.animatedScreen != screen) {
            animations.animatedScreen = screen;
            animations.screenClosing = false;
            animations.screenCloseStart = 0L;
            animations.screenReleased = false;
            animations.screenFade.reset(0.0F);
         }

         long duration = animations.screenClosing ? scale(120L, animations.speedFor(screen)) : scale(140L, animations.speedFor(screen));
         float raw = animations.screenFade.update(duration, animations.screenClosing);
         return 0.75F + 0.25F * animations.ease(raw);
      } else {
         return 1.0F;
      }
   }

   public static boolean deferScreenClose(Screen screen) {
      Animations animations = instance;
      if (animations == null || !screenEnabled(screen) || animations.screenReleased) {
         return false;
      } else if (animations.screenClosing) {
         animations.resetScreen();
         return false;
      } else {
         animations.animatedScreen = screen;
         animations.screenClosing = true;
         animations.screenCloseStart = System.currentTimeMillis();
         return true;
      }
   }

   public static void resetScreenState() {
      Animations animations = instance;
      if (animations != null) {
         animations.resetScreen();
      }
   }

   private void resetScreen() {
      this.animatedScreen = null;
      this.screenClosing = false;
      this.screenCloseStart = 0L;
      this.screenReleased = false;
      this.screenFade.reset(0.0F);
   }

   private NumberSetting speedFor(Screen screen) {
      return screen instanceof InventoryScreen ? this.inventorySpeed : this.containerSpeed;
   }

   public static boolean buttonsEnabled() {
      return active("Buttons");
   }

   public static float buttonRate() {
      Animations animations = instance;
      float speed = animations == null ? 1.0F : clampSpeed(animations.buttonSpeed);
      return 12.0F * speed;
   }

   public static boolean perspectiveEnabled() {
      return active("Perspective");
   }

   public static float perspectiveRate() {
      Animations animations = instance;
      return animations == null ? 1.0F : clampSpeed(animations.perspectiveSpeed);
   }

   public static float perspectiveProgress(boolean detached) {
      Animations animations = instance;
      if (animations != null && perspectiveEnabled()) {
         if (!animations.perspectiveInitialized) {
            animations.perspectiveInitialized = true;
            animations.perspectiveFade.reset(detached ? 1.0F : 0.0F);
         }

         long duration = scale(220L, animations.perspectiveSpeed);
         float raw = animations.perspectiveFade.update(duration, !detached);
         return Math.max(0.0F, Math.min(1.0F, animations.ease(raw)));
      } else {
         return detached ? 1.0F : 0.0F;
      }
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      if (this.chatClosing && this.chatFinished()) {
         Screen screen = mc.currentScreen;
         this.resetChat();
         if (screen instanceof ChatScreen) {
            this.chatReleased = true;

            try {
               screen.close();
            } finally {
               this.chatReleased = false;
            }
         }
      }

      if (this.screenClosing && this.screenFinished()) {
         Screen screen = this.animatedScreen;
         this.resetScreen();
         if (screen != null && mc.currentScreen == screen) {
            this.screenReleased = true;

            try {
               screen.close();
            } finally {
               this.screenReleased = false;
            }
         }
      }
   }

   private boolean chatFinished() {
      long duration = scale(220L, this.chatSpeed);
      return System.currentTimeMillis() - this.chatCloseStart >= duration;
   }

   private boolean screenFinished() {
      long duration = scale(120L, this.speedFor(this.animatedScreen));
      return System.currentTimeMillis() - this.screenCloseStart >= duration;
   }

   public static boolean isScreenCloseReleased() {
      Animations animations = instance;
      return animations != null && animations.screenReleased;
   }

   private float ease(float t) {
      float x = Math.max(0.0F, Math.min(1.0F, t));
      String var3 = this.easing.getValue();

      return switch (var3) {
         case "Linear" -> x;
         case "Ease Out Quad" -> 1.0F - (1.0F - x) * (1.0F - x);
         case "Ease Out Cubic" -> 1.0F - (float)Math.pow((double)(1.0F - x), 3.0);
         case "Ease Out Quart" -> 1.0F - (float)Math.pow((double)(1.0F - x), 4.0);
         case "Ease Out Expo" -> x >= 1.0F ? 1.0F : 1.0F - (float)Math.pow(2.0, -10.0 * (double)x);
         case "Ease Out Elastic" -> elasticOut(x);
         case "Ease Out Bounce" -> bounceOut(x);
         default -> backOut(x);
      };
   }

   private static float backOut(float x) {
      float c1 = 1.70158F;
      float c3 = c1 + 1.0F;
      float inv = x - 1.0F;
      return 1.0F + c3 * inv * inv * inv + c1 * inv * inv;
   }

   private static float elasticOut(float x) {
      if (!(x <= 0.0F) && !(x >= 1.0F)) {
         double c4 = Math.PI * 2.0 / 3.0;
         return (float)(Math.pow(2.0, -10.0 * (double)x) * Math.sin(((double)x * 10.0 - 0.75) * c4) + 1.0);
      } else {
         return x;
      }
   }

   private static float bounceOut(float x) {
      float n1 = 7.5625F;
      float d1 = 2.75F;
      if (x < 1.0F / d1) {
         return n1 * x * x;
      } else if (x < 2.0F / d1) {
         float v = x - 1.5F / d1;
         return n1 * v * v + 0.75F;
      } else if (x < 2.5F / d1) {
         float v = x - 2.25F / d1;
         return n1 * v * v + 0.9375F;
      } else {
         float v = x - 2.625F / d1;
         return n1 * v * v + 0.984375F;
      }
   }

   private static long scale(long baseMs, NumberSetting speed) {
      return Math.max(1L, (long)Math.round((float)baseMs / clampSpeed(speed)));
   }

   private static float clampSpeed(NumberSetting setting) {
      if (setting == null) {
         return 1.0F;
      } else {
         float value = setting.getFloat();
         return Float.isFinite(value) && value > 0.0F ? value : 1.0F;
      }
   }

   private static final class Fade {
      private float value;
      private long last;

      private Fade() {
      }

      float update(long durationMs, boolean closing) {
         long now = System.currentTimeMillis();
         if (this.last == 0L) {
            this.last = now;
         }

         float delta = (float)(now - this.last) / 1000.0F;
         this.last = now;
         if (delta > 0.1F) {
            delta = 0.1F;
         }

         float rate = durationMs <= 0L ? 1000.0F : 1000.0F / (float)durationMs;
         this.value += (closing ? -delta : delta) * rate;
         if (this.value < 0.0F) {
            this.value = 0.0F;
         } else if (this.value > 1.0F) {
            this.value = 1.0F;
         }

         return this.value;
      }

      float value() {
         return this.value;
      }

      void reset(float to) {
         this.value = to;
         this.last = 0L;
      }
   }
}
