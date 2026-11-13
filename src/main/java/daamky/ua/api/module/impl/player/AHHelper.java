package daamky.ua.api.module.impl.player;

import java.awt.Color;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.HandledScreenEvent;
import daamky.ua.api.events.impl.KeyEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.bind.InputType;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;

public final class AHHelper extends Module {
   private static final long PULSE_MS = 900L;
   private static final float PULSE_MIN = 0.35F;
   private static final float PULSE_MAX = 1.0F;
   private static final long RESCAN_DEBOUNCE_MS = 90L;
   private static final long RESCAN_INTERVAL_MS = 650L;
   private static final long STORAGE_CLICK_DELAY_MS = 60100L;
   private static final int CONTAINER_Y_LIMIT = 168;
   private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d{1,3}(?:[,.]\\d{3})+|\\d+[,.]\\d+|\\d+)");
   private static final Pattern SECTION_COLOR_PATTERN = Pattern.compile("\\u00A7[0-9A-FK-ORa-fk-or]");
   private static final Pattern AMP_COLOR_PATTERN = Pattern.compile("&[0-9A-FK-ORa-fk-or]");
   private static final Pattern BRACKET_CONTENT_PATTERN = Pattern.compile("\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}");
   private static final Pattern AUCTION_QUERY_CLEANUP = Pattern.compile("[^\\p{L}\\p{N}\\s]+");
   private final BindSetting findFromHandBind = this.register(new BindSetting("Search Item", "Runs /ah search for the item in hand.", KeyBind.NONE));
   private final BooleanSetting itemReListingSetting = this.register(
      new BooleanSetting("Storage Relist", "Quick-moves storage slot 52 once per minute.", false)
   );
   private final ColorSetting cheapestColor = this.register(
      new ColorSetting("Cheapest Color", "Highlight color for the lowest total price.", new Color(255, 184, 35, 210))
   );
   private final ColorSetting economicColor = this.register(
      new ColorSetting("Per Item Color", "Highlight color for the best price per item.", new Color(255, 64, 64, 210))
   );
   private Slot cheapestSlot;
   private Slot bestPerItemSlot;
   private int screenSyncId = -1;
   private long lastScanAt;
   private long lastStorageClick = -1L;
   private boolean dirty;
   private Field cachedLeftField;
   private Field cachedTopField;

   public AHHelper() {
      super("AH Helper", "Auction helper: highlights cheap lots and searches held item.", ModuleCategory.PLAYER);
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null
         && client.interactionManager != null
         && client.currentScreen instanceof GenericContainerScreen screen
         && this.isAuctionScreen(screen)) {
         if (this.itemReListingSetting.getValue()) {
            this.handleStorageRelisting(screen);
         }

         long now = System.currentTimeMillis();
         int syncId = ((GenericContainerScreenHandler)screen.getScreenHandler()).syncId;
         if (syncId != this.screenSyncId) {
            this.screenSyncId = syncId;
            this.dirty = true;
            this.cheapestSlot = null;
            this.bestPerItemSlot = null;
         }

         if (this.dirty && now - this.lastScanAt >= 90L || now - this.lastScanAt >= 650L) {
            this.scanAuctionSlots(screen);
         }
      } else {
         this.resetState();
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive()
         && event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket
         && mc.currentScreen instanceof GenericContainerScreen screen
         && this.isAuctionScreen(screen)) {
         this.dirty = true;
      }
   }

   @SubscribeEvent
   private void onKey(KeyEvent event) {
      if (event.action() == 1 && this.isBindTriggered(this.findFromHandBind.getValue(), event)) {
         this.triggerSearchCommandFromHand();
      }
   }

   @SubscribeEvent
   private void onHandledScreen(HandledScreenEvent event) {
      if (mc.currentScreen instanceof GenericContainerScreen screen && this.isAuctionScreen(screen) && event.getGraphics() != null) {
         int[] origin = this.resolveScreenOrigin(screen, event);
         int cheapest = this.pulsingColor(this.cheapestColor.getValue());
         int economic = this.pulsingColor(this.economicColor.getValue());
         if (this.cheapestSlot != null && this.cheapestSlot.hasStack()) {
            this.drawSlotHighlight(event.getGraphics(), origin[0], origin[1], this.cheapestSlot, cheapest);
         }

         if (this.bestPerItemSlot != null && this.bestPerItemSlot.hasStack()) {
            this.drawSlotHighlight(event.getGraphics(), origin[0], origin[1], this.bestPerItemSlot, economic);
         }

         return;
      }
   }

   private void handleStorageRelisting(GenericContainerScreen screen) {
      if (mc.interactionManager != null && mc.player != null) {
         String title = screen.getTitle() == null ? "" : this.stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
         if (title.contains("хранилище") || title.contains("storage")) {
            long now = System.currentTimeMillis();
            if (now - this.lastStorageClick >= 60100L) {
               List<Slot> slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
               if (slots.size() > 52) {
                  Slot storageSlot = slots.get(52);
                  if (storageSlot != null && storageSlot.hasStack()) {
                     mc.interactionManager
                        .clickSlot(((GenericContainerScreenHandler)screen.getScreenHandler()).syncId, 52, 0, SlotActionType.QUICK_MOVE, mc.player);
                     this.lastStorageClick = now;
                  }
               }
            }
         }
      }
   }

   private void scanAuctionSlots(GenericContainerScreen screen) {
      if (mc.player == null) {
         this.resetState();
      } else {
         List<Slot> slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots;
         int[] totalPrice = new int[slots.size()];
         int[] stackCount = new int[slots.size()];
         Slot minTotalSlot = null;
         int minTotal = Integer.MAX_VALUE;

         for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty() && slot.inventory != mc.player.getInventory() && slot.y < 168) {
               int price = this.extractPrice(stack);
               int count = Math.max(1, stack.getCount());
               totalPrice[i] = price;
               stackCount[i] = count;
               if (price >= 0 && price < minTotal) {
                  minTotal = price;
                  minTotalSlot = slot;
               }
            } else {
               totalPrice[i] = -1;
               stackCount[i] = 0;
            }
         }

         Slot bestPerItem = null;
         double bestRatio = Double.POSITIVE_INFINITY;
         int tiePrice = Integer.MAX_VALUE;

         for (int ix = 0; ix < slots.size(); ix++) {
            int price = totalPrice[ix];
            if (price >= 0) {
               Slot slot = slots.get(ix);
               if (slot != minTotalSlot) {
                  int count = Math.max(1, stackCount[ix]);
                  double ratio = (double)price / (double)count;
                  if (ratio < bestRatio - 1.0E-9 || Math.abs(ratio - bestRatio) <= 1.0E-9 && price < tiePrice) {
                     bestRatio = ratio;
                     tiePrice = price;
                     bestPerItem = slot;
                  }
               }
            }
         }

         this.cheapestSlot = minTotalSlot;
         this.bestPerItemSlot = bestPerItem;
         this.dirty = false;
         this.lastScanAt = System.currentTimeMillis();
      }
   }

   private int extractPrice(ItemStack stack) {
      try {
         int count = Math.max(1, stack.getCount());
         TooltipContext context = mc.world == null ? TooltipContext.DEFAULT : TooltipContext.create(mc.world);
         List<Text> tooltip = stack.getTooltip(context, mc.player, TooltipType.BASIC);
         StringBuilder combined = new StringBuilder();
         if (tooltip != null && !tooltip.isEmpty()) {
            for (Text line : tooltip) {
               if (line != null) {
                  combined.append(this.stripFormatting(line.getString())).append(' ');
               }
            }
         } else {
            combined.append(this.stripFormatting(stack.getName().getString()));
         }

         return this.parsePriceFromText(combined.toString(), count);
      } catch (Throwable var8) {
         return -1;
      }
   }

   private int parsePriceFromText(String text, int stackCount) {
      if (text != null && !text.isEmpty()) {
         String lower = text.toLowerCase(Locale.ROOT);
         Matcher matcher = NUMBER_PATTERN.matcher(text);
         int bestWeight = -1;
         long bestPrice = -1L;
         long fallbackPrice = -1L;

         while (matcher.find()) {
            int startIdx = matcher.start(1);
            int endIdx = matcher.end(1);
            int suffixIndex = this.skipSuffixSeparators(lower, endIdx);
            long multiplier = this.suffixMultiplier(lower, suffixIndex);
            long value = this.parseFlexibleNumber(matcher.group(1), multiplier);
            if (value > 0L) {
               int contextStart = Math.max(0, startIdx - 28);
               int contextEnd = Math.min(lower.length(), Math.max(endIdx, suffixIndex + 2) + 28);
               String context = lower.substring(contextStart, contextEnd);
               boolean priceContext = this.looksLikePriceContext(context);
               boolean totalHint = context.contains("total") || context.contains("итог") || context.contains("всего") || context.contains("сумм");
               boolean perItem = context.contains("за шт") || context.contains("за 1") || context.contains("per item") || context.contains("each");
               int weight = priceContext ? 3 : 1;
               if (totalHint) {
                  weight += 3;
               }

               if (perItem) {
                  weight++;
               }

               long normalized = perItem ? value * (long)Math.max(1, stackCount) : value;
               if (normalized > 0L) {
                  if (normalized > fallbackPrice) {
                     fallbackPrice = normalized;
                  }

                  if (weight > bestWeight || weight == bestWeight && normalized > bestPrice) {
                     bestWeight = weight;
                     bestPrice = normalized;
                  }
               }
            }
         }

         long resolved = bestPrice > 0L ? bestPrice : fallbackPrice;
         if (resolved <= 0L) {
            return -1;
         } else {
            return resolved > 2147483647L ? Integer.MAX_VALUE : (int)resolved;
         }
      } else {
         return -1;
      }
   }

   private boolean looksLikePriceContext(String context) {
      return context.contains("price")
         || context.contains("total")
         || context.contains("buy")
         || context.contains("sell")
         || context.contains("coin")
         || context.contains("auction")
         || context.contains("ah")
         || context.contains("цена")
         || context.contains("стоим")
         || context.contains("куп")
         || context.contains("прод")
         || context.contains("монет")
         || context.contains("аукц")
         || context.contains("лот")
         || context.contains("руб")
         || context.contains("$")
         || context.contains("₽");
   }

   private long suffixMultiplier(String text, int indexAfterNumber) {
      if (indexAfterNumber >= text.length()) {
         return 1L;
      } else {
         char c0 = Character.toLowerCase(text.charAt(indexAfterNumber));
         char c1 = indexAfterNumber + 1 < text.length() ? Character.toLowerCase(text.charAt(indexAfterNumber + 1)) : 0;
         if (c0 != 'k' && c0 != 1082) {
            return c0 != 'm' && c0 != 1084 ? 1L : 1000000L;
         } else {
            return c1 != 'k' && c1 != 1082 ? 1000L : 1000000L;
         }
      }
   }

   private int skipSuffixSeparators(String text, int index) {
      int i;
      for (i = index; i < text.length(); i++) {
         char c = text.charAt(i);
         if (!Character.isWhitespace(c) && c != ':' && c != '=' && c != '-') {
            break;
         }
      }

      return i;
   }

   private long parseFlexibleNumber(String source, long multiplier) {
      if (source != null && !source.isBlank()) {
         String compact = source.trim().replace(" ", "").replace("_", "");
         if (compact.matches("\\d{1,3}([,.]\\d{3})+")) {
            long base = this.parseNumber(compact.replace(",", "").replace(".", ""));
            return this.multiplySafe(base, multiplier);
         } else if (compact.indexOf(46) < 0 && compact.indexOf(44) < 0) {
            return this.multiplySafe(this.parseNumber(compact), multiplier);
         } else {
            try {
               double parsed = Double.parseDouble(compact.replace(',', '.'));
               if (!(parsed > 0.0)) {
                  return -1L;
               } else {
                  double scaled = parsed * (double)multiplier;
                  return scaled >= 9.223372E18F ? Long.MAX_VALUE : Math.round(scaled);
               }
            } catch (NumberFormatException var9) {
               return -1L;
            }
         }
      } else {
         return -1L;
      }
   }

   private long parseNumber(String source) {
      long value = 0L;

      for (int i = 0; i < source.length(); i++) {
         char c = source.charAt(i);
         if (c >= '0' && c <= '9') {
            value = value * 10L + (long)(c - '0');
         }
      }

      return value;
   }

   private long multiplySafe(long base, long multiplier) {
      if (base <= 0L) {
         return -1L;
      } else {
         return multiplier > 1L && base >= Long.MAX_VALUE / multiplier ? Long.MAX_VALUE : base * multiplier;
      }
   }

   private int pulsingColor(Color base) {
      long now = System.currentTimeMillis();
      float phase = (float)(now % 900L) / 900.0F;
      float wave = 0.5F - 0.5F * MathHelper.cos((double)(phase * (float) (Math.PI * 2)));
      float alphaMul = MathHelper.clamp(0.35F + 0.65F * wave, 0.0F, 1.0F);
      int alpha = MathHelper.clamp((int)((float)Math.max(120, base.getAlpha()) * alphaMul), 40, 220);
      return alpha << 24 | base.getRed() << 16 | base.getGreen() << 8 | base.getBlue();
   }

   private void drawSlotHighlight(DrawContext graphics, int originX, int originY, Slot slot, int color) {
      int x = originX + slot.x;
      int y = originY + slot.y;
      graphics.fill(x, y, x + 16, y + 16, color);
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int borderColor = -436207616 | r << 16 | g << 8 | b;
      graphics.fill(x, y, x + 16, y + 1, borderColor);
      graphics.fill(x, y + 15, x + 16, y + 16, borderColor);
      graphics.fill(x, y, x + 1, y + 16, borderColor);
      graphics.fill(x + 15, y, x + 16, y + 16, borderColor);
   }

   private boolean isAuctionScreen(GenericContainerScreen screen) {
      if (mc.player != null && screen != null) {
         String title = this.stripFormatting(screen.getTitle().getString()).toLowerCase(Locale.ROOT);
         if (!title.contains("auction") && !title.contains("аукц") && !title.contains("ah")) {
            int checked = 0;

            for (Slot slot : ((GenericContainerScreenHandler)screen.getScreenHandler()).slots) {
               if (slot.inventory != mc.player.getInventory() && slot.hasStack()) {
                  if (checked++ >= 8) {
                     break;
                  }

                  try {
                     TooltipContext context = mc.world == null ? TooltipContext.DEFAULT : TooltipContext.create(mc.world);
                     List<Text> tooltip = slot.getStack().getTooltip(context, mc.player, TooltipType.BASIC);
                     boolean hasPrice = false;
                     boolean hasSeller = false;

                     for (Text line : tooltip) {
                        String text = this.stripFormatting(line.getString()).toLowerCase(Locale.ROOT);
                        if (this.looksLikePriceContext(text)) {
                           hasPrice = true;
                        }

                        if (text.contains("seller") || text.contains("продавец")) {
                           hasSeller = true;
                        }
                     }

                     if (hasPrice && hasSeller) {
                        return true;
                     }
                  } catch (Throwable var13) {
                  }
               }
            }

            return false;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private int[] resolveScreenOrigin(HandledScreen<?> screen, HandledScreenEvent event) {
      int fallbackX = (screen.width - event.getImageWidth()) / 2;
      int fallbackY = (screen.height - event.getImageHeight()) / 2;

      try {
         if (this.cachedLeftField == null || this.cachedTopField == null) {
            this.cachedLeftField = HandledScreen.class.getDeclaredField("leftPos");
            this.cachedTopField = HandledScreen.class.getDeclaredField("topPos");
            this.cachedLeftField.setAccessible(true);
            this.cachedTopField.setAccessible(true);
         }

         return new int[]{this.cachedLeftField.getInt(screen), this.cachedTopField.getInt(screen)};
      } catch (Throwable var6) {
         return new int[]{fallbackX, fallbackY};
      }
   }

   private String stripFormatting(String text) {
      if (text != null && !text.isEmpty()) {
         String stripped = Formatting.strip(text);
         return stripped == null ? text : stripped;
      } else {
         return "";
      }
   }

   private void triggerSearchCommandFromHand() {
      if (mc.player != null && mc.player.networkHandler != null) {
         ItemStack hand = mc.player.getMainHandStack();
         if (hand.isEmpty()) {
            mc.player.sendMessage(Text.literal("AH Helper: no item in hand"), true);
         } else {
            String query = this.sanitizeAuctionQuery(hand.getName().getString());
            if (query.isBlank()) {
               query = this.stripFormatting(hand.getName().getString());
            }

            if (query != null && !query.isBlank()) {
               mc.player.networkHandler.sendChatCommand("ah search " + query);
               mc.player.sendMessage(Text.literal("AH Helper: /ah search " + query), true);
               this.dirty = true;
            } else {
               mc.player.sendMessage(Text.literal("AH Helper: empty item name"), true);
            }
         }
      }
   }

   private String sanitizeAuctionQuery(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String normalized = this.stripFormatting(rawName);
         normalized = SECTION_COLOR_PATTERN.matcher(normalized).replaceAll(" ");
         normalized = AMP_COLOR_PATTERN.matcher(normalized).replaceAll(" ");
         normalized = BRACKET_CONTENT_PATTERN.matcher(normalized).replaceAll(" ");
         normalized = AUCTION_QUERY_CLEANUP.matcher(normalized).replaceAll(" ");
         normalized = normalized.replace(' ', ' ');
         return normalized.trim().replaceAll("\\s{2,}", " ");
      } else {
         return "";
      }
   }

   private boolean isBindTriggered(KeyBind bind, KeyEvent event) {
      if (bind != null && bind.isBound()) {
         if (bind.getType() == InputType.KEYBOARD) {
            return event.type() == Type.KEYSYM && event.key() == bind.getCode();
         } else {
            return bind.getType() != InputType.MOUSE ? false : event.type() == Type.MOUSE && event.key() == bind.getCode();
         }
      } else {
         return false;
      }
   }

   private void resetState() {
      this.cheapestSlot = null;
      this.bestPerItemSlot = null;
      this.screenSyncId = -1;
      this.lastScanAt = 0L;
      this.dirty = false;
      this.cachedLeftField = null;
      this.cachedTopField = null;
   }
}
