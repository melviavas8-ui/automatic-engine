package daamky.ua.screens.mainmenu;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import daamky.ua.api.theme.ClientTheme;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.render.ui.outline.outline360.BuiltOutline360;
import daamky.ua.utils.render.ui.outline.outline360.Outline360Range;

public final class VanillaMenuTheme {
   private static final String OPTIONS_PACKAGE = "net.minecraft.client.gui.screens.options";
   private static final String MULTIPLAYER_PACKAGE = "net.minecraft.client.gui.screens.multiplayer";
   private static final String WORLD_PACKAGE = "net.minecraft.client.gui.screens.worldselection";

   private VanillaMenuTheme() {
   }

   public static boolean active() {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      return minecraft != null && isThemed(minecraft.currentScreen);
   }

   public static boolean isThemed(Screen screen) {
      if (screen == null) return false;
      // Yarn package names use "screen", not Mojmap's "screens".
      return screen instanceof AltManagerScreen
         || screen instanceof SelectWorldScreen
         || screen instanceof DirectConnectScreen
         || screen instanceof AddServerScreen;
   }

   public static boolean usesNeonBackground(Screen screen) {
      return screen instanceof SelectWorldScreen
         || screen instanceof DirectConnectScreen
         || screen instanceof AddServerScreen;
   }

   public static boolean usesOpeningAnimation(Screen screen) {
      return screen instanceof AltManagerScreen || isThemed(screen) && !(screen instanceof GameMenuScreen);
   }

   public static void renderBackground(DrawContext graphics, int width, int height) {
      DaamkyMenuBackground.render(graphics, width, height);
   }

   public static boolean shouldRoundListIcon(int width, int height, int textureWidth, int textureHeight) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      Screen screen = minecraft == null ? null : minecraft.currentScreen;
      return screen instanceof SelectWorldScreen
         && width == 32
         && height == 32
         && textureWidth == 32
         && textureHeight == 32;
   }

   public static void renderRoundedListIcon(DrawContext graphics, String texture, float x, float y) {
      Render2D.beginFrame(graphics);
      Render2D.imageUvNearest(texture, x + 3.0F, y + 3.0F, 26.0F, 26.0F, 5.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, -1);
      Render2D.flush();
   }

   public static boolean shouldHideServerListOverlay(Identifier texture) {
      MinecraftClient minecraft = MinecraftClient.getInstance();
      if (minecraft == null || texture == null) return false;
      if (!(minecraft.currentScreen instanceof SelectWorldScreen)) return false;
      String path = texture.getPath();
      return path.equals("world_list/join")
         || path.equals("world_list/join_highlighted")
         || path.equals("world_list/marked_join")
         || path.equals("world_list/marked_join_highlighted");
   }

   public static void renderServerMoveButton(DrawContext graphics, float buttonX, float buttonY, boolean moveUp, boolean highlighted) {
      float size = 11.0F;
      Render2D.beginFrame(graphics);
      Render2D.blur(buttonX, buttonY, size, size, 3.5F, 12.0F, 1.0F, withAlpha(-13258753, highlighted ? 105 : 58));
      Render2D.glass(
         buttonX,
         buttonY,
         size,
         size,
         3.5F,
         withAlpha(-2231297, highlighted ? 42 : 24),
         highlighted ? 0.58F : 0.4F,
         36.0F,
         withAlpha(-8071937, highlighted ? 76 : 40),
         highlighted ? 0.18F : 0.09F,
         true,
         0.18F,
         highlighted ? 0.009F : 0.004F,
         1.0F,
         0.0F
      );
      Render2D.rect(buttonX, buttonY, size, size, 3.5F, withAlpha(-15299109, highlighted ? 34 : 15));
      String arrow = moveUp ? "^" : "v";
      float textSize = 5.2F;
      float textWidth = Render2D.textWidth(FontType.REGULARNEW, arrow, textSize);
      Render2D.text(FontType.REGULARNEW, arrow, buttonX + (size - textWidth) * 0.5F, buttonY + 2.2F, textSize, highlighted ? -219547393 : -1177950475);
      Render2D.flush();
   }

   public static void renderButton(DrawContext graphics, ClickableWidget widget, float hover) {
      float x = (float)widget.getX();
      float y = (float)widget.getY();
      float width = (float)widget.getWidth();
      float height = (float)widget.getHeight();
      float radius = Math.min(5.5F, height * 0.28F);
      float enabled = widget.active ? 1.0F : 0.48F;
      float eased = hover * hover * (3.0F - 2.0F * hover);
      Render2D.beginFrame(graphics);
      Render2D.blur(x, y, width, height, radius, 16.0F, 1.0F, withAlpha(accentColor(), Math.round(enabled * 48.0F)));
      Render2D.glass(
         x,
         y,
         width,
         height,
         radius,
         withAlpha(-1838081, Math.round(enabled * 20.0F)),
         0.42F * enabled,
         42.0F,
         withAlpha(accentColor(), Math.round(enabled * 72.0F)),
         0.08F * enabled,
         true,
         0.12F,
         0.0045F,
         1.0F,
         0.0F
      );
      Render2D.rect(x, y, width, height, radius, withAlpha(-9061907, Math.round(enabled * (hover > 0.01F ? 15.0F : 8.0F))));
      if (eased > 0.01F) {
         renderAccentOrbit(x, y, width, height, radius, eased);
      }

      renderCenteredLabel(widget.getMessage(), x, y, width, height, enabled);
      Render2D.flush();
   }

   public static void renderSlider(DrawContext graphics, ClickableWidget widget, double value, float hover) {
      float x = (float)widget.getX();
      float y = (float)widget.getY();
      float width = (float)widget.getWidth();
      float height = (float)widget.getHeight();
      float radius = Math.min(5.5F, height * 0.28F);
      float enabled = widget.active ? 1.0F : 0.48F;
      Render2D.beginFrame(graphics);
      Render2D.blur(x, y, width, height, radius, 15.0F, 1.0F, withAlpha(-9258508, Math.round(enabled * 42.0F)));
      Render2D.glass(
         x,
         y,
         width,
         height,
         radius,
         withAlpha(-1772545, Math.round(enabled * 18.0F)),
         0.4F * enabled,
         40.0F,
         withAlpha(-5318148, Math.round(enabled * 28.0F)),
         0.07F * enabled,
         true,
         0.12F,
         0.004F,
         1.0F,
         0.0F
      );
      Render2D.rect(x, y, width, height, radius, withAlpha(-9459240, Math.round(enabled * 7.0F)));
      float trackX = x + 7.0F;
      float trackY = y + height - 4.0F;
      float trackWidth = Math.max(1.0F, width - 14.0F);
      float progress = (float)Math.max(0.0, Math.min(1.0, value));
      Render2D.rect(trackX, trackY, trackWidth, 1.25F, 0.65F, withAlpha(-4798249, 40));
      Render2D.rect(trackX, trackY, trackWidth * progress, 1.25F, 0.65F, withAlpha(accentColor(), 220));
      float handleX = trackX + trackWidth * progress;
      Render2D.rect(handleX - 3.0F, trackY - 2.375F, 6.0F, 6.0F, 2.0F, withAlpha(accentColor(), Math.round(enabled * (hover > 0.01F ? 245.0F : 190.0F))));
      renderCenteredLabel(widget.getMessage(), x, y - 1.3F, width, height - 3.0F, enabled);
      Render2D.flush();
   }

   public static void renderListBackground(DrawContext graphics, ClickableWidget widget) {
      float x = (float)widget.getX();
      float y = (float)widget.getY();
      float width = (float)widget.getWidth();
      float height = (float)widget.getHeight();
      Render2D.beginFrame(graphics);
      Render2D.blur(x, y, width, height, 6.0F, 18.0F, 1.0F, 612088298);
      Render2D.glass(x, y, width, height, 6.0F, 417395199, 0.32F, 42.0F, 581556215, 0.055F, true, 0.1F, 0.003F, 1.0F, 0.0F);
      Render2D.rect(x, y, width, height, 6.0F, 151942583);
      Render2D.flush();
   }

   public static void renderListCard(DrawContext graphics, float x, float y, float width, float height, boolean hovered, boolean selected) {
      renderListCard(graphics, x, y, width, height, hovered, selected, 1.0F);
   }

   public static void renderListCard(DrawContext graphics, float x, float y, float width, float height, boolean hovered, boolean selected, float visibility) {
      float shown = Math.max(0.0F, Math.min(1.0F, visibility));
      float insetX = 2.5F;
      float insetY = 1.5F;
      float drawX = x + insetX;
      float drawY = y + insetY;
      float drawWidth = Math.max(1.0F, width - insetX * 2.0F);
      float drawHeight = Math.max(1.0F, height - insetY * 2.0F);
      float radius = Math.min(6.0F, drawHeight * 0.22F);
      Render2D.beginFrame(graphics);
      Render2D.blur(
         drawX, drawY, drawWidth, drawHeight, radius, 17.0F, 1.0F, withAlpha(accentColor(), Math.round(shown * (float)(selected ? 72 : (hovered ? 50 : 30))))
      );
      Render2D.glass(
         drawX,
         drawY,
         drawWidth,
         drawHeight,
         radius,
         withAlpha(-1706753, Math.round(shown * (float)(selected ? 27 : (hovered ? 21 : 14)))),
         shown * (selected ? 0.5F : 0.38F),
         40.0F,
         withAlpha(-4989702, Math.round(shown * (float)(selected ? 42 : 27))),
         shown * (selected ? 0.11F : 0.065F),
         true,
         0.12F,
         !hovered && !selected ? 0.003F : 0.006F,
         1.0F,
         0.0F
      );
      Render2D.rect(
         drawX,
         drawY,
         drawWidth,
         drawHeight,
         radius,
         withAlpha(selected ? -14315560 : -8865818, Math.round(shown * (float)(selected ? 24 : (hovered ? 14 : 7))))
      );
      Render2D.flush();
   }

   public static void renderHeading(DrawContext graphics, ClickableWidget widget) {
      String text = widget.getMessage() == null ? "" : widget.getMessage().getString();
      if (!text.isEmpty()) {
         float size = 7.4F;
         float textWidth = Render2D.textWidth(FontType.REGULARNEW, text, size);
         Render2D.beginFrame(graphics);
         Render2D.text(
            FontType.REGULARNEW,
            text,
            (float)widget.getX() + ((float)widget.getWidth() - textWidth) * 0.5F,
            (float)widget.getY() + ((float)widget.getHeight() - size) * 0.5F - 0.5F,
            size,
            -555027204
         );
         Render2D.flush();
      }
   }

   public static void renderEditBox(DrawContext graphics, TextFieldWidget editBox) {
      float x = (float)editBox.getX();
      float y = (float)editBox.getY();
      float width = (float)editBox.getWidth();
      float height = (float)editBox.getHeight();
      float radius = Math.min(5.5F, height * 0.28F);
      Render2D.beginFrame(graphics);
      Render2D.blur(x, y, width, height, radius, 16.0F, 1.0F, 880525809);
      Render2D.glass(x, y, width, height, radius, 551941375, 0.42F, 42.0F, 783605500, 0.08F, true, 0.12F, 0.004F, 1.0F, 0.0F);
      Render2D.rect(x, y, width, height, radius, editBox.isFocused() ? 405380840 : 177194474);
      String value = editBox.getText();
      String shown = value.isEmpty() ? "Поиск..." : value;
      float size = 5.9F;
      float available = Math.max(1.0F, width - 12.0F);
      float textWidth = Render2D.textWidth(FontType.REGULARNEW, shown, size);
      float offset = value.isEmpty() ? 0.0F : Math.min(0.0F, available - textWidth);
      Render2D.pushScissor(graphics, x + 5.0F, y, available + 2.0F, height);
      Render2D.text(FontType.REGULARNEW, shown, x + 6.0F + offset, y + (height - size) * 0.5F - 0.4F, size, value.isEmpty() ? 1904910773 : -891097353);
      if (editBox.isFocused() && System.currentTimeMillis() / 430L % 2L == 0L) {
         int cursor = Math.min(editBox.getCursor(), value.length());
         float cursorX = x + 6.0F + offset + Render2D.textWidth(FontType.REGULARNEW, value.substring(0, cursor), size);
         Render2D.rect(cursorX + 0.5F, y + 4.5F, 0.65F, Math.max(5.0F, height - 9.0F), 0.3F, -1244795905);
      }

      Render2D.popScissor(graphics);
      Render2D.flush();
   }

   public static void drawCenteredText(DrawContext graphics, String text, float centerX, float y, int color) {
      text = stripLegacyFormatting(text);
      if (text != null && !text.isEmpty()) {
         float size = 6.4F;
         int normalized = normalizeTextColor(color);
         Render2D.beginFrame(graphics);
         float width = Render2D.textWidth(FontType.REGULARNEW, text, size);
         Render2D.text(FontType.REGULARNEW, text, centerX - width * 0.5F, y + 1.0F, size, normalized);
         Render2D.flush();
      }
   }

   public static void drawText(DrawContext graphics, String text, float x, float y, int color) {
      text = stripLegacyFormatting(text);
      if (text != null && !text.isEmpty()) {
         Render2D.beginFrame(graphics);
         Render2D.text(FontType.REGULARNEW, text, x, y + 1.0F, 6.0F, normalizeTextColor(color));
         Render2D.flush();
      }
   }

   public static void drawRightAlignedText(DrawContext graphics, String text, float rightX, float y, int color) {
      text = stripLegacyFormatting(text);
      if (text != null && !text.isEmpty()) {
         float size = 6.0F;
         float width = Render2D.textWidth(FontType.REGULARNEW, text, size);
         Render2D.beginFrame(graphics);
         Render2D.text(FontType.REGULARNEW, text, rightX - width, y + 1.0F, size, normalizeTextColor(color));
         Render2D.flush();
      }
   }

   private static String stripLegacyFormatting(String text) {
      if (text != null && text.indexOf(167) >= 0) {
         StringBuilder clean = new StringBuilder(text.length());

         for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == 167 && i + 1 < text.length()) {
               i++;
            } else {
               clean.append(text.charAt(i));
            }
         }

         return clean.toString();
      } else {
         return text;
      }
   }

   private static void renderCenteredLabel(Text message, float x, float y, float width, float height, float alpha) {
      String text = message == null ? "" : message.getString();
      if (!text.isEmpty()) {
         float size = 5.85F;
         float available = Math.max(4.0F, width - 12.0F);
         float textWidth = Render2D.textWidth(FontType.REGULARNEW, text, size);
         if (textWidth > available) {
            size = Math.max(1.8F, size * available / textWidth);
            textWidth = Render2D.textWidth(FontType.REGULARNEW, text, size);
         }

         Render2D.text(
            FontType.DEFAULT, text, x + (width - textWidth) * 0.5F, y + (height - size) * 0.5F - 0.45F, size, withAlpha(-1773577, Math.round(alpha * 205.0F))
         );
      }
   }

   private static void renderAccentOrbit(float x, float y, float width, float height, float radius, float alpha) {
      float time = (float)(System.nanoTime() % 60000000000L) / 1.0E9F;
      float angle = time * 180.0F % 360.0F;
      Outline360Range tail = new Outline360Range(angle - 145.0F, angle + 14.0F, withAlpha(accentColor(), Math.round(alpha * 125.0F)));
      Outline360Range head = new Outline360Range(angle - 48.0F, angle + 14.0F, withAlpha(accentBrightColor(), Math.round(alpha * 255.0F)));
      Render2D.outline360(
         new BuiltOutline360(x - 1.2F, y - 1.2F, width + 2.4F, height + 2.4F, radius + 1.2F, 1.45F, 0, List.of(tail, head))
            .withSmoothness(1.7F)
            .withBlendDegrees(20.0F)
      );
      Render2D.outline360(
         new BuiltOutline360(x - 0.2F, y - 0.2F, width + 0.4F, height + 0.4F, radius + 0.2F, 0.5F, 0, List.of(tail, head))
            .withSmoothness(0.8F)
            .withBlendDegrees(14.0F)
      );
   }


   private static int accentColor() {
      return ThemeManager.current().accent().getRGB();
   }

   private static int accentBrightColor() {
      return ThemeManager.current().accentBright().getRGB();
   }

   private static int normalizeTextColor(int color) {
      int alpha = color >>> 24 & 0xFF;
      if (alpha == 0) {
         alpha = 255;
      }

      int red = Math.max(174, color >>> 16 & 0xFF);
      int green = Math.max(190, color >>> 8 & 0xFF);
      int blue = Math.max(204, color & 0xFF);
      return alpha << 24 | red << 16 | green << 8 | blue;
   }

   private static int withAlpha(int color, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | color & 16777215;
   }
}