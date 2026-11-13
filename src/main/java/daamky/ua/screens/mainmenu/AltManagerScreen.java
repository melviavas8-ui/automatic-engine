package daamky.ua.screens.mainmenu;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.session.Session;
import net.minecraft.text.Text;
import daamky.ua.api.theme.ThemeManager;
import daamky.ua.mixin.accessor.MinecraftAccessor;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.render.ui.outline.outline360.BuiltOutline360;
import daamky.ua.utils.render.ui.outline.outline360.Outline360Range;

public final class AltManagerScreen extends Screen {
   private static final float CARD_WIDTH = 174.0F;
   private static final float CARD_HEIGHT = 23.0F;
   private static final float CARD_GAP = 3.0F;
   private static final float CONTROL_HEIGHT = 18.0F;
   private static final String[] PREFIXES = new String[]{
      "Nova",
      "Void",
      "Lunar",
      "Frost",
      "Aero",
      "Night",
      "Ghost",
      "Pixel",
      "Zero",
      "Dark",
      "Neon",
      "Silent",
      "Cloud",
      "Cyber",
      "Vortex",
      "Echo",
      "Blue",
      "Hyper",
      "Astro",
      "Lost"
   };
   private static final String[] CORES = new String[]{
      "Wolf",
      "Raven",
      "Blade",
      "Flux",
      "Storm",
      "Shade",
      "Spark",
      "Dream",
      "Fox",
      "Byte",
      "Pulse",
      "Drift",
      "Comet",
      "Venom",
      "Soul",
      "Wave",
      "Glitch",
      "Crow",
      "Ninja",
      "Orbit"
   };
   private static final String[] SUFFIXES = new String[]{"", "X", "EX", "TV", "YT", "GG", "Pro", "One", "Prime", "Core", "Live", "XD"};
   private final List<AltManagerScreen.AccountCard> accounts = new ArrayList<>();
   private final List<AltManagerScreen.CardBounds> cardBounds = new ArrayList<>();
   private final Random random = new Random();
   private String input = "";
   private int cursor;
   private boolean inputFocused = true;
   private float scroll;
   private long lastFrameNanos;
   private float inputX;
   private float inputY;
   private float inputWidth;
   private float plusX;
   private float backX;
   private float backWidth;
   private AltManagerScreen.AccountCard draggedCard;
   private boolean draggingCard;
   private double dragPressX;
   private double dragPressY;
   private double dragGrabX;
   private double dragGrabY;
   private double dragCursorX;
   private double dragCursorY;

   public AltManagerScreen() {
      super(Text.literal("Accounts"));
      List<AltAccountStore.Account> loaded = AltAccountStore.load();
      loaded.stream().filter(AltAccountStore.Account::selected).findFirst().ifPresent(account -> this.applyAccount(account.name()));
      String currentName = this.currentName();
      boolean migrated = AltAccountStore.migrateLegacyDefaultPin(loaded, currentName);
      if (loaded.stream().noneMatch(account -> account.name().equalsIgnoreCase(currentName))) {
         loaded.add(new AltAccountStore.Account(currentName, false));
         AltAccountStore.save(loaded);
      } else if (migrated) {
         AltAccountStore.save(loaded);
      }

      long now = System.nanoTime();

      for (int i = 0; i < loaded.size(); i++) {
         this.accounts.add(new AltManagerScreen.AccountCard(loaded.get(i), now + (long)i * 45000000L));
      }

      this.sortAccounts();
   }

   protected void init() {
      this.lastFrameNanos = System.nanoTime();
      this.inputFocused = true;
   }

   public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      int fixedMouseX = Math.round(Render2D.guiToFixed((float)mouseX));
      int fixedMouseY = Math.round(Render2D.guiToFixed((float)mouseY));
      float delta = this.frameDeltaSeconds();
      long now = System.nanoTime();
      this.renderBackground(context, mouseX, mouseY, deltaTicks);
      context.createNewRootLayer();
      Render2D.beginFrame(context);
      this.renderHeader();
      this.renderAccounts(context, fixedMouseX, fixedMouseY, delta, now);
      this.renderControls(fixedMouseX, fixedMouseY, now);
      Render2D.flush();
   }

   private void renderHeader() {
      float designWidth = (float)Render2D.getFixedScaledWidth();
      String title = "АККАУНТЫ";
      float titleSize = 9.0F;
      float titleWidth = Render2D.textWidth(FontType.REGULARNEW, title, titleSize);
      Render2D.text(FontType.REGULARNEW, title, (designWidth - titleWidth) * 0.5F, 21.0F, titleSize, -571935489);
      String help = "ЛКМ: ВОЙТИ   /   СКМ: ЗАКРЕПИТЬ   /   ПКМ: УДАЛИТЬ";
      float helpSize = 4.6F;
      float helpWidth = Render2D.textWidth(FontType.REGULARNEW, help, helpSize);
      Render2D.text(FontType.REGULARNEW, help, (designWidth - helpWidth) * 0.5F, 33.5F, helpSize, 1769969064);
   }

   private void renderAccounts(DrawContext graphics, int mouseX, int mouseY, float delta, long now) {
      float designWidth = (float)Render2D.getFixedScaledWidth();
      float designHeight = (float)Render2D.getFixedScaledHeight();
      float listTop = 40.0F;
      float listBottom = designHeight - 51.0F;
      float cardX = (designWidth - 174.0F) * 0.5F;
      float contentHeight = (float)this.accounts.size() * 26.0F - 3.0F;
      float viewportHeight = Math.max(1.0F, listBottom - listTop);
      float minScroll = Math.min(0.0F, viewportHeight - contentHeight);
      this.scroll = clamp(this.scroll, minScroll, 0.0F);
      this.cardBounds.clear();
      Render2D.pushScissor(graphics, 0.0F, listTop, designWidth, viewportHeight);
      List<AltManagerScreen.AccountCard> finishedRemoving = new ArrayList<>();

      for (int i = 0; i < this.accounts.size(); i++) {
         AltManagerScreen.AccountCard card = this.accounts.get(i);
         float target = listTop + this.scroll + (float)i * 26.0F;
         if (Float.isNaN(card.y)) {
            card.y = target + 8.0F;
         }

         float drawX = cardX;
         if (card == this.draggedCard && this.draggingCard) {
            drawX = (float)(this.dragCursorX - this.dragGrabX);
            card.y = (float)(this.dragCursorY - this.dragGrabY);
         } else {
            card.y = approach(card.y, target, delta * 105.0F);
         }

         float targetVisibility = card.removing ? 0.0F : (now >= card.revealAt ? 1.0F : 0.0F);
         card.visibility = approach(card.visibility, targetVisibility, delta * (card.removing ? 5.5F : 4.2F));
         if (card.removing && card.visibility <= 0.01F) {
            finishedRemoving.add(card);
         } else if (card.y + 23.0F >= listTop && card.y <= listBottom) {
            this.renderAccountCard(card, drawX, card.y, mouseX, mouseY, now);
            this.cardBounds.add(new AltManagerScreen.CardBounds(card, drawX, card.y, 174.0F, 23.0F));
         }
      }

      Render2D.popScissor(graphics);
      if (!finishedRemoving.isEmpty()) {
         this.accounts.removeAll(finishedRemoving);
         this.persist();
      }
   }

   private void renderAccountCard(AltManagerScreen.AccountCard card, float x, float y, int mouseX, int mouseY, long now) {
      float visibility = smooth(card.visibility);
      float scale = 0.88F + visibility * 0.12F;
      float cardWidth = 174.0F * scale;
      float cardHeight = 23.0F * scale;
      float drawX = x + (174.0F - cardWidth) * 0.5F;
      float drawY = y + (23.0F - cardHeight) * 0.5F;
      float radius = 5.0F;
      boolean hovered = contains((double)mouseX, (double)mouseY, x, y, 174.0F, 23.0F);
      boolean active = card.account.name().equalsIgnoreCase(this.currentName());
      int alpha = Math.round(visibility * 255.0F);
      Render2D.blur(drawX, drawY, cardWidth, cardHeight, radius, 16.0F, 1.0F, withAlpha(accentColor(), Math.round((float)alpha * 0.24F)));
      Render2D.glass(
         drawX,
         drawY,
         cardWidth,
         cardHeight,
         radius,
         withAlpha(-1575681, Math.round((float)alpha * 0.11F)),
         visibility * 0.42F,
         40.0F,
         withAlpha(-4728328, Math.round((float)alpha * 0.18F)),
         visibility * 0.08F,
         true,
         0.12F,
         0.004F,
         1.0F,
         0.0F
      );
      Render2D.rect(
         drawX, drawY, cardWidth, cardHeight, radius, withAlpha(active ? accentColor() : -4268033, Math.round((float)alpha * (hovered ? 0.20F : 0.08F)))
      );
      if (card.account.pinned()) {
         this.renderPinnedOrbit(drawX, drawY, cardWidth, cardHeight, radius, visibility, now);
      }

      float nameSize = 6.4F;
      Render2D.text(
         FontType.REGULARNEW,
         card.account.name(),
         drawX + 9.0F,
         drawY + (cardHeight - nameSize) * 0.5F - 0.4F,
         nameSize,
         withAlpha(-1576456, Math.round((float)alpha * 0.82F))
      );
      if (active) {
         String activeText = "АКТИВЕН";
         float activeSize = 4.3F;
         float activeWidth = Render2D.textWidth(FontType.REGULARNEW, activeText, activeSize);
         Render2D.text(
            FontType.REGULARNEW,
            activeText,
            drawX + cardWidth - activeWidth - 8.0F,
            drawY + (cardHeight - activeSize) * 0.5F - 0.2F,
            activeSize,
            withAlpha(-9123841, Math.round((float)alpha * 0.78F))
         );
      }
   }

   private void renderPinnedOrbit(float x, float y, float width, float height, float radius, float visibility, long now) {
      float time = (float)(now % 60000000000L) / 1.0E9F;
      float angle = time * 135.0F % 360.0F;
      Outline360Range tail = new Outline360Range(angle - 155.0F, angle + 14.0F, withAlpha(accentColor(), Math.round(visibility * 105.0F)));
      Outline360Range head = new Outline360Range(angle - 48.0F, angle + 14.0F, withAlpha(accentBrightColor(), Math.round(visibility * 255.0F)));
      Render2D.outline360(
         new BuiltOutline360(x - 1.3F, y - 1.3F, width + 2.6F, height + 2.6F, radius + 1.3F, 1.5F, 0, List.of(tail, head))
            .withSmoothness(1.8F)
            .withBlendDegrees(20.0F)
      );
      Render2D.outline360(
         new BuiltOutline360(x - 0.25F, y - 0.25F, width + 0.5F, height + 0.5F, radius + 0.25F, 0.55F, 0, List.of(tail, head))
            .withSmoothness(0.8F)
            .withBlendDegrees(14.0F)
      );
   }

   private void renderControls(int mouseX, int mouseY, long now) {
      float designWidth = (float)Render2D.getFixedScaledWidth();
      float designHeight = (float)Render2D.getFixedScaledHeight();
      this.inputWidth = 124.0F;
      float plusWidth = 18.0F;
      this.backWidth = 42.0F;
      float totalWidth = this.inputWidth + 4.0F + plusWidth + 4.0F + this.backWidth;
      this.inputX = (designWidth - totalWidth) * 0.5F;
      this.inputY = designHeight - 46.0F;
      this.plusX = this.inputX + this.inputWidth + 4.0F;
      this.backX = this.plusX + plusWidth + 4.0F;
      this.renderControl(
         this.inputX,
         this.inputY,
         this.inputWidth,
         18.0F,
         contains((double)mouseX, (double)mouseY, this.inputX, this.inputY, this.inputWidth, 18.0F),
         this.inputFocused
      );
      this.renderControl(this.plusX, this.inputY, plusWidth, 18.0F, contains((double)mouseX, (double)mouseY, this.plusX, this.inputY, plusWidth, 18.0F), false);
      this.renderControl(
         this.backX, this.inputY, this.backWidth, 18.0F, contains((double)mouseX, (double)mouseY, this.backX, this.inputY, this.backWidth, 18.0F), false
      );
      float textSize = 5.8F;
      String shown = this.input.isEmpty() ? "Введите ник..." : this.input;
      int textColor = this.input.isEmpty() ? 1904647085 : -807079944;
      Render2D.text(FontType.REGULARNEW, shown, this.inputX + 7.0F, this.inputY + 5.9F, textSize, textColor);
      if (this.inputFocused && now / 430000000L % 2L == 0L) {
         float cursorX = this.inputX
            + 7.0F
            + Render2D.textWidth(FontType.REGULARNEW, this.input.substring(0, Math.min(this.cursor, this.input.length())), textSize);
         Render2D.rect(cursorX + 0.7F, this.inputY + 4.5F, 0.65F, 8.5F, 0.3F, accentAlpha(230));
      }

      float plusSize = 7.5F;
      float plusTextWidth = Render2D.textWidth(FontType.GUI_ICONS, "N", plusSize);
      Render2D.text(FontType.GUI_ICONS, "N", this.plusX + (plusWidth - plusTextWidth) * 0.5F, this.inputY + 5.0F, plusSize, accentAlpha(240));
      this.drawCentered("ВЫХОД", this.backX, this.inputY, this.backWidth, 18.0F, 5.0F, -1345857822);
   }

   private void renderControl(float x, float y, float width, float height, boolean hovered, boolean focused) {
      float radius = width == height ? 5.0F : 5.5F;
      Render2D.blur(x, y, width, height, radius, 15.0F, 1.0F, 947370474);
      Render2D.glass(x, y, width, height, radius, 585495551, 0.42F, 40.0F, 817422335, 0.08F, true, 0.12F, 0.004F, 1.0F, 0.0F);
      Render2D.rect(x, y, width, height, radius, !hovered && !focused ? 213704703 : accentAlpha(focused ? 62 : 38));
   }

   private void drawCentered(String text, float x, float y, float width, float height, float size, int color) {
      float textWidth = Render2D.textWidth(FontType.REGULARNEW, text, size);
      Render2D.text(FontType.REGULARNEW, text, x + (width - textWidth) * 0.5F, y + (height - size) * 0.5F - 0.4F, size, color);
   }

   public boolean mouseClicked(Click click, boolean doubled) {
      double fixedMouseX = Render2D.guiToFixed(click.x());
      double fixedMouseY = Render2D.guiToFixed(click.y());
      if (click.button() == 0) {
         if (contains(fixedMouseX, fixedMouseY, this.inputX, this.inputY, this.inputWidth, 18.0F)) {
            this.inputFocused = true;
            this.cursor = this.input.length();
            return true;
         }

         if (contains(fixedMouseX, fixedMouseY, this.plusX, this.inputY, 18.0F, 18.0F)) {
            this.addAccount(this.randomName());
            return true;
         }

         if (contains(fixedMouseX, fixedMouseY, this.backX, this.inputY, this.backWidth, 18.0F)) {
            this.close();
            return true;
         }
      }

      for (AltManagerScreen.CardBounds bounds : this.cardBounds) {
         if (contains(fixedMouseX, fixedMouseY, bounds.x(), bounds.y(), bounds.width(), bounds.height())) {
            AltManagerScreen.AccountCard card = bounds.card();
            if (click.button() == 0) {
               this.draggedCard = card;
               this.draggingCard = false;
               this.dragPressX = fixedMouseX;
               this.dragPressY = fixedMouseY;
               this.dragGrabX = fixedMouseX - (double)bounds.x();
               this.dragGrabY = fixedMouseY - (double)bounds.y();
               this.dragCursorX = fixedMouseX;
               this.dragCursorY = fixedMouseY;
               return true;
            }

            if (click.button() == 1) {
               if (!card.account.pinned()) {
                  card.removing = true;
               }

               return true;
            }

            if (click.button() == 2) {
               card.account.setPinned(!card.account.pinned());
               this.sortAccounts();
               this.persist();
               return true;
            }
         }
      }

      this.inputFocused = false;
      return super.mouseClicked(click, doubled);
   }

   public boolean mouseDragged(Click click, double offsetX, double offsetY) {
      if (this.draggedCard != null && click.button() == 0) {
         double fixedMouseX = Render2D.guiToFixed(click.x());
         double fixedMouseY = Render2D.guiToFixed(click.y());
         if (!this.draggingCard && Math.hypot(fixedMouseX - this.dragPressX, fixedMouseY - this.dragPressY) < 3.0) {
            return true;
         } else {
            this.draggingCard = true;
            this.dragCursorX = fixedMouseX;
            this.dragCursorY = fixedMouseY;
            this.reorderDraggedAccount();
            return true;
         }
      } else {
         return super.mouseDragged(click, offsetX, offsetY);
      }
   }

   public boolean mouseReleased(Click click) {
      if (this.draggedCard != null && click.button() == 0) {
         AltManagerScreen.AccountCard released = this.draggedCard;
         boolean moved = this.draggingCard;
         this.draggedCard = null;
         this.draggingCard = false;
         if (moved) {
            this.persist();
         } else {
            this.switchAccount(released.account.name());
         }

         return true;
      } else {
         return super.mouseReleased(click);
      }
   }

   public boolean keyPressed(KeyInput input) {
      if (!this.inputFocused) {
         if (input.key() == 256) {
            this.close();
            return true;
         } else {
            return super.keyPressed(input);
         }
      } else if (input.key() == 257 || input.key() == 335) {
         this.addAccount(this.input);
         return true;
      } else if (input.key() == 256) {
         this.close();
         return true;
      } else if (input.key() == 259 && this.cursor > 0) {
         this.input = this.input.substring(0, this.cursor - 1) + this.input.substring(this.cursor);
         this.cursor--;
         return true;
      } else if (input.key() == 261 && this.cursor < this.input.length()) {
         this.input = this.input.substring(0, this.cursor) + this.input.substring(this.cursor + 1);
         return true;
      } else if (input.key() == 263) {
         this.cursor = Math.max(0, this.cursor - 1);
         return true;
      } else if (input.key() == 262) {
         this.cursor = Math.min(this.input.length(), this.cursor + 1);
         return true;
      } else {
         return true;
      }
   }

   public boolean charTyped(CharInput input) {
      if (this.inputFocused && input.isValidChar() && this.input.length() < 16) {
         String value = input.asString();
         if (!value.matches("[A-Za-z0-9_]")) {
            return false;
         } else {
            this.input = this.input.substring(0, this.cursor) + value + this.input.substring(this.cursor);
            this.cursor = this.cursor + value.length();
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      this.scroll += (float)verticalAmount * 15.0F;
      return true;
   }

   public void close() {
      this.client.setScreen(new DaamkyMainMenuScreen());
   }

   public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
      DaamkyMenuBackground.render(context, this.width, this.height);
   }

   public boolean shouldCloseOnEsc() {
      return false;
   }

   private void addAccount(String rawName) {
      String name = sanitize(rawName);
      if (name.length() >= 3 && !this.accounts.stream().anyMatch(card -> card.account.name().equalsIgnoreCase(name))) {
         this.accounts.add(new AltManagerScreen.AccountCard(new AltAccountStore.Account(name, false), System.nanoTime()));
         this.input = "";
         this.cursor = 0;
         this.sortAccounts();
         this.persist();
      }
   }

   private String randomName() {
      String name = PREFIXES[this.random.nextInt(PREFIXES.length)] + CORES[this.random.nextInt(CORES.length)];
      if (this.random.nextBoolean()) {
         name = name + SUFFIXES[this.random.nextInt(SUFFIXES.length)];
      }

      if (this.random.nextInt(100) < 72) {
         name = name + this.random.nextInt(10, 9999);
      }

      if (name.length() > 16) {
         name = name.substring(0, 16);
      }

      return name;
   }

   private void switchAccount(String name) {
      this.applyAccount(name);
      this.accounts.forEach(card -> card.account.setSelected(card.account.name().equalsIgnoreCase(name)));
      this.persist();
   }

   private void applyAccount(String name) {
      MinecraftClient client = MinecraftClient.getInstance();
      UUID profileId = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
      Session user = new Session(name, profileId, "", Optional.empty(), Optional.empty());
      ((MinecraftAccessor)client).daamky$setUser(user);
      client.updateWindowTitle();
   }

   private void sortAccounts() {
      this.accounts.sort(Comparator.comparing(card -> !card.account.pinned()));
   }

   private void reorderDraggedAccount() {
      int index = this.accounts.indexOf(this.draggedCard);
      if (index >= 0) {
         float listTop = 40.0F;
         float step = 26.0F;
         float draggedCenter = (float)(this.dragCursorY - this.dragGrabY + 11.5);
         int target = index;
         if (draggedCenter < listTop + this.scroll + (float)index * step
            && index > 0
            && this.accounts.get(index - 1).account.pinned() == this.draggedCard.account.pinned()) {
            target = index - 1;
         } else if (draggedCenter > listTop + this.scroll + (float)index * step + 23.0F
            && index + 1 < this.accounts.size()
            && this.accounts.get(index + 1).account.pinned() == this.draggedCard.account.pinned()) {
            target = index + 1;
         }

         if (target != index) {
            Collections.swap(this.accounts, index, target);
         }
      }
   }

   private void persist() {
      AltAccountStore.save(this.accounts.stream().filter(card -> !card.removing).map(card -> card.account).toList());
   }

   private String currentName() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client.getSession() != null ? client.getSession().getUsername() : "Player";
   }

   private static String sanitize(String value) {
      if (value == null) {
         return "";
      } else {
         String cleaned = value.replaceAll("[^A-Za-z0-9_]", "");
         return cleaned.length() > 16 ? cleaned.substring(0, 16) : cleaned;
      }
   }

   private float frameDeltaSeconds() {
      long now = System.nanoTime();
      float delta = this.lastFrameNanos == 0L ? 0.0F : (float)(now - this.lastFrameNanos) / 1.0E9F;
      this.lastFrameNanos = now;
      return Math.min(delta, 0.05F);
   }

   private static float approach(float current, float target, float amount) {
      return current < target ? Math.min(target, current + amount) : Math.max(target, current - amount);
   }

   private static float smooth(float value) {
      float clamped = clamp(value, 0.0F, 1.0F);
      return clamped * clamped * (3.0F - 2.0F * clamped);
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static boolean contains(double mouseX, double mouseY, float x, float y, float width, float height) {
      return mouseX >= (double)x && mouseX <= (double)(x + width) && mouseY >= (double)y && mouseY <= (double)(y + height);
   }

   private static int accentColor() {
      return ThemeManager.current().accent().getRGB();
   }

   private static int accentBrightColor() {
      return ThemeManager.current().accentBright().getRGB();
   }

   private static int accentAlpha(int alpha) {
      return withAlpha(accentColor(), alpha);
   }

   private static int withAlpha(int color, int alpha) {
      return Math.max(0, Math.min(255, alpha)) << 24 | color & 16777215;
   }

   private static final class AccountCard {
      private final AltAccountStore.Account account;
      private final long revealAt;
      private float visibility;
      private float y = Float.NaN;
      private boolean removing;

      private AccountCard(AltAccountStore.Account account, long revealAt) {
         this.account = account;
         this.revealAt = revealAt;
      }
   }

   private static record CardBounds(AltManagerScreen.AccountCard card, float x, float y, float width, float height) {
   }
}
