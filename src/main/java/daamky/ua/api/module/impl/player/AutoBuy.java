package daamky.ua.api.module.impl.player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.modules.autobuy.AuctionUtils;
import daamky.ua.utils.modules.autobuy.AutoBuyItem;
import daamky.ua.utils.modules.autobuy.AutoBuyManager;
import daamky.ua.utils.string.chat.ChatMessage;
import daamky.ua.utils.timer.TimerUtil;

public final class AutoBuy extends Module {
   private static AutoBuy instance;
   private static final boolean TEMPORARILY_UNAVAILABLE = false;
   private boolean suppressingDisableMessage;
   private final ModeSetting serverMode = this.register(
      new ModeSetting("Режим сервера", "FunTime / SpookyTime / HolyWorld", "FunTime", "FunTime", "SpookyTime", "HolyWorld")
   );
   private final BooleanSetting autoParse = this.register(new BooleanSetting("Auto Parse", "Периодический парсинг цен через /ah search.", false));
   private final NumberSetting parseDiscount = this.register(new NumberSetting("Парс скидка %", "Скидка от мин. цены при парсе.", 20.0, 1.0, 100.0, 1.0));
   private final NumberSetting reparseMinutes = this.register(
      new NumberSetting("ReParse (мин)", "Повторный полный парсинг (0 = только вручную).", 0.0, 0.0, 240.0, 5.0)
   );
   private final NumberSetting updateDelay = this.register(new NumberSetting("Кд обновления (мс)", "Задержка обновления аукциона.", 350.0, 100.0, 5000.0, 50.0));
   private final NumberSetting buyDelay = this.register(new NumberSetting("Кд покупки (мс)", "Задержка между покупками.", 120.0, 50.0, 5000.0, 10.0));
   private final NumberSetting buyHoldMs = this.register(
      new NumberSetting("Пауза перед покупкой (мс)", "Сколько держать цель перед shift-кликом (меньше ложных переносов в инвентарь).", 100.0, 40.0, 400.0, 10.0)
   );
   private final NumberSetting postBuyPauseMs = this.register(
      new NumberSetting("Пауза после покупки (мс)", "Не кликать/не обновлять AH сразу после shift-покупки.", 250.0, 50.0, 1500.0, 25.0)
   );
   private final NumberSetting confirmDelay = this.register(
      new NumberSetting("Кд подтверждения (мс)", "Задержка клика подтверждения.", 50.0, 0.0, 1000.0, 10.0)
   );
   private final BooleanSetting anarchySwap = this.register(new BooleanSetting("Свап анархии", "Случайный /anN (FT 90–120с · SP/HW 5–10 мин).", true));
   private final NumberSetting anarchyMinSec = this.register(new NumberSetting("Анархия мин (с)", "Мин. интервал /an (FunTime).", 90.0, 30.0, 600.0, 5.0));
   private final NumberSetting anarchyMaxSec = this.register(new NumberSetting("Анархия макс (с)", "Макс. интервал /an (FunTime).", 120.0, 40.0, 900.0, 5.0));
   private final BooleanSetting spWalk = this.register(new BooleanSetting("SP ходьба", "SpookyTime: отойти ~N блоков и снова /ah.", true));
   private final NumberSetting spWalkBlocks = this.register(new NumberSetting("SP блоков", "Дистанция ухода перед скупкой.", 5.0, 2.0, 15.0, 0.5));
   private final NumberSetting spWalkIntervalSec = this.register(new NumberSetting("SP интервал (с)", "Как часто отходить (сек).", 60.0, 15.0, 300.0, 5.0));
   private final BooleanSetting autoOpenAh = this.register(new BooleanSetting("Авто /ah", "Открывать аукцион если закрыт.", true));
   private final NumberSetting ahReopenMs = this.register(new NumberSetting("Кд /ah (мс)", "Как часто пытаться открыть /ah.", 1500.0, 500.0, 30000.0, 100.0));
   private final NumberSetting shulkerProfitPct = this.register(
      new NumberSetting("Shulker Profit %", "Мин. профит % для шалкера (зарезерв.).", 18.0, 0.0, 200.0, 1.0)
   );
   private final NumberSetting shulkerProfitAbs = this.register(
      new NumberSetting("Shulker Profit $", "Мин. профит $ для шалкера (зарезерв.).", 50000.0, 0.0, 1.0E9, 10000.0)
   );
   private final NumberSetting shulkerValue = this.register(
      new NumberSetting("Shulker Value $", "Мин. ценность содержимого (зарезерв.).", 100000.0, 0.0, 1.0E9, 10000.0)
   );
   private final BooleanSetting notifications = this.register(new BooleanSetting("Уведомления", "Сообщения о покупках в чат.", true));
   private final BindSetting menuBind = this.register(new BindSetting("Бинд меню", "Открыть ClickGUI (категория AutoBuy).", KeyBind.NONE));
   private final TimerUtil updateTimer = TimerUtil.create();
   private final TimerUtil buyTimer = TimerUtil.create();
   private final TimerUtil confirmTimer = TimerUtil.create();
   private final TimerUtil ahTimer = TimerUtil.create();
   private final TimerUtil anarchyTimer = TimerUtil.create();
   private final TimerUtil parseCmdTimer = TimerUtil.create();
   private final TimerUtil parseWaitTimer = TimerUtil.create();
   private final TimerUtil reparseTimer = TimerUtil.create();
   private final Set<String> boughtKeys = new HashSet<>();
   private long nextAnarchyMs = 0L;
   private boolean inAuction;
   private AutoBuy.WalkState walkState = AutoBuy.WalkState.IDLE;
   private long nextWalkAtMs;
   private long walkCloseAtMs;
   private boolean walkForward = true;
   private double walkStartX;
   private double walkStartZ;
   private AutoBuy.FtSwitch ftSwitch = AutoBuy.FtSwitch.IDLE;
   private long ftPhaseAtMs;
   private int pendingAnarchy = -1;
   private static final int[] FT_ANARCHIES = new int[]{101, 102, 103, 104, 105, 201, 202, 203, 204, 205, 301, 302, 303};
   private int pendingSlot = -1;
   private String pendingKey = "";
   private String pendingName = "";
   private int pendingPrice = -1;
   private int pendingCount = -1;
   private long pendingSinceMs = 0L;
   private long postBuyUntilMs = 0L;
   private long lastBuyClickMs = 0L;
   private boolean parseRunning;
   private boolean parseWaitingResult;
   private int parseIndex;
   private int parseRetries;
   private String parseCurrentName = "";
   private final List<String> parseQueue = new ArrayList<>();
   private int parseUpdatedCount;

   public AutoBuy() {
      super("AutoBuy", "Автопокупка предметов на аукционе", ModuleCategory.AUTOBUY);
      instance = this;
      this.parseDiscount.visibleWhen(this.autoParse::getValue);
      this.reparseMinutes.visibleWhen(this.autoParse::getValue);
      this.anarchyMinSec.visibleWhen(() -> this.anarchySwap.getValue() && this.serverMode.is("FunTime"));
      this.anarchyMaxSec.visibleWhen(() -> this.anarchySwap.getValue() && this.serverMode.is("FunTime"));
      this.spWalk.visibleWhen(() -> this.serverMode.is("SpookyTime"));
      this.spWalkBlocks.visibleWhen(() -> this.serverMode.is("SpookyTime") && this.spWalk.getValue());
      this.spWalkIntervalSec.visibleWhen(() -> this.serverMode.is("SpookyTime") && this.spWalk.getValue());
      this.ahReopenMs.visibleWhen(this.autoOpenAh::getValue);
   }

   public NumberSetting getUpdateDelaySetting() {
      return this.updateDelay;
   }

   public NumberSetting getBuyDelaySetting() {
      return this.buyDelay;
   }

   public BooleanSetting getAnarchySwapSetting() {
      return this.anarchySwap;
   }

   public NumberSetting getAnarchyMinSecSetting() {
      return this.anarchyMinSec;
   }

   public NumberSetting getAnarchyMaxSecSetting() {
      return this.anarchyMaxSec;
   }

   public BooleanSetting getSpWalkSetting() {
      return this.spWalk;
   }

   public NumberSetting getSpWalkBlocksSetting() {
      return this.spWalkBlocks;
   }

   public NumberSetting getSpWalkIntervalSetting() {
      return this.spWalkIntervalSec;
   }

   public NumberSetting getAhReopenSetting() {
      return this.ahReopenMs;
   }

   public BooleanSetting getAutoOpenAhSetting() {
      return this.autoOpenAh;
   }

   public NumberSetting getReparseMinutesSetting() {
      return this.reparseMinutes;
   }

   public void cycleServerMode() {
      String[] modes = new String[]{"FunTime", "SpookyTime", "HolyWorld"};
      String cur = this.serverMode.getValue();
      int idx = 0;

      for (int i = 0; i < modes.length; i++) {
         if (modes[i].equalsIgnoreCase(cur)) {
            idx = i;
            break;
         }
      }

      this.serverMode.setValue(modes[(idx + 1) % modes.length]);
      this.msg("§eРежим: §f" + this.serverMode.getValue());
   }

   public void adjustParseDiscount(int delta) {
      int v = this.parseDiscount.getValue().intValue() + delta;
      this.parseDiscount.setValue(Double.valueOf((double)Math.max(1, Math.min(100, v))));
   }

   public void adjustUpdateDelay(int deltaMs) {
      int v = this.updateDelay.getValue().intValue() + deltaMs;
      this.updateDelay.setValue(Double.valueOf((double)Math.max(100, Math.min(5000, v))));
   }

   public static AutoBuy getInstance() {
      return instance;
   }

   public ModeSetting getServerMode() {
      return this.serverMode;
   }

   public BooleanSetting getAutoParseSetting() {
      return this.autoParse;
   }

   public NumberSetting getParseDiscountSetting() {
      return this.parseDiscount;
   }

   public boolean isAutoParseEnabled() {
      return this.autoParse.getValue();
   }

   public boolean isParseRunning() {
      return this.parseRunning;
   }

   public int getParseIndex() {
      return this.parseIndex;
   }

   public int getParseQueueSize() {
      return this.parseQueue.size();
   }

   public String getParseCurrentName() {
      return this.parseCurrentName == null ? "" : this.parseCurrentName;
   }

   public void toggleAutoParse() {
      if (!this.isEnabled() && !this.autoParse.getValue()) {
         this.msg("§cСначала включите AutoBuy вручную");
      } else {
         this.autoParse.setValue(Boolean.valueOf(!this.autoParse.getValue()));
         if (this.autoParse.getValue()) {
            this.msg("§d[AutoParse] §aвключён (скидка " + this.parseDiscount.getValue().intValue() + "%)");
            this.startParse(false);
         } else {
            this.stopParse(false);
            this.msg("§d[AutoParse] §cвыключен");
         }
      }
   }

   public void startParseNow() {
      if (!this.isEnabled()) {
         this.msg("§cСначала включите AutoBuy вручную");
      } else {
         this.startParse(false);
      }
   }

   @Override
   public boolean shouldRestoreEnabledState() {
      return false;
   }

   @Override
   protected void onEnable() {
      AutoBuyManager.get().ensureLoaded();
      if (!this.parseRunning) {
         this.resetBuyRuntime();
      }

      this.nextAnarchyMs = this.randomAnarchyDelay();
      this.anarchyTimer.resetCounter();
      this.scheduleNextWalk();
      this.walkState = AutoBuy.WalkState.IDLE;
      this.ftSwitch = AutoBuy.FtSwitch.IDLE;
      this.pendingAnarchy = -1;
      this.stopWalkKeys();
      this.reparseTimer.resetCounter();
      this.msg("§aAutoBuy включён §7(" + this.serverMode.getValue() + ")");
   }

   @Override
   protected void onDisable() {
      if (!this.suppressingDisableMessage) {
         this.stopParse(false);
         this.resetBuyRuntime();
         this.stopWalkKeys();
         this.walkState = AutoBuy.WalkState.IDLE;
         this.ftSwitch = AutoBuy.FtSwitch.IDLE;
         this.msg("§cAutoBuy выключен");
      }
   }

   private void resetBuyRuntime() {
      this.boughtKeys.clear();
      this.inAuction = false;
      this.clearPendingBuy();
      this.postBuyUntilMs = 0L;
      this.lastBuyClickMs = 0L;
      this.updateTimer.resetCounter();
      this.buyTimer.resetCounter();
      this.confirmTimer.resetCounter();
      this.ahTimer.resetCounter();
      this.anarchyTimer.resetCounter();
   }

   private void scheduleNextWalk() {
      long sec = Math.max(5L, this.spWalkIntervalSec.getValue().longValue());
      this.nextWalkAtMs = System.currentTimeMillis() + sec * 1000L;
   }

   private void stopWalkKeys() {
      if (mc != null && mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
      }
   }

   private void clearPendingBuy() {
      this.pendingSlot = -1;
      this.pendingKey = "";
      this.pendingName = "";
      this.pendingPrice = -1;
      this.pendingCount = -1;
      this.pendingSinceMs = 0L;
   }

   private void stopParse(boolean completedMsg) {
      this.parseRunning = false;
      this.parseWaitingResult = false;
      this.parseIndex = 0;
      this.parseRetries = 0;
      this.parseCurrentName = "";
      this.parseQueue.clear();
      this.parseUpdatedCount = 0;
      this.parseCmdTimer.resetCounter();
      this.parseWaitTimer.resetCounter();
      if (completedMsg) {
         this.msg("§d[AutoParse] §aГотово · обновлено " + this.parseUpdatedCount + " поз.");
      }
   }

   @SubscribeEvent
   public void onTick(TickEvent.Pre event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.isEnabled()) {
            this.handleMenuBind();
            if (this.parseRunning) {
               this.tickParse();
            } else if (!this.handleFunTimeAnarchy()) {
               if (!this.handleSpookyWalk()) {
                  if (this.autoParse.getValue()) {
                     long reparseMs = this.reparseMinutes.getValue().longValue() * 60000L;
                     if (reparseMs > 0L && this.reparseTimer.hasTimeElapsed(reparseMs)) {
                        this.startParse(true);
                        return;
                     }
                  }

                  if (!this.serverMode.is("FunTime")) {
                     this.handleAnarchySwap();
                  }

                  if (mc.currentScreen instanceof GenericContainerScreen screen) {
                     String var10 = screen.getTitle().getString();
                     int slotCount = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size();
                     if (AuctionUtils.isConfirmTitle(var10) || this.isLikelyConfirm(var10, slotCount)) {
                        this.clearPendingBuy();
                        if (this.confirmTimer.hasTimeElapsed(this.confirmDelay.getValue().longValue())) {
                           this.clickConfirm(screen);
                           this.confirmTimer.resetCounter();
                           this.postBuyUntilMs = System.currentTimeMillis() + 120L;
                        }
                     } else if (!AuctionUtils.isAuctionTitle(var10) && !AuctionUtils.isSearchTitle(var10)) {
                        this.inAuction = false;
                        this.clearPendingBuy();
                     } else {
                        if (!this.inAuction) {
                           this.inAuction = true;
                           this.boughtKeys.clear();
                           this.clearPendingBuy();
                           this.msg("§aАукцион. Включено: §b" + AutoBuyManager.get().enabledCount(this.serverMode.getValue()));
                        }

                        long now = System.currentTimeMillis();
                        boolean postBuyFreeze = now < this.postBuyUntilMs;
                        boolean aiming = this.pendingSlot >= 0;
                        if (!postBuyFreeze && !aiming && this.updateTimer.hasTimeElapsed(this.effectiveUpdateDelay())) {
                           this.refreshAuction(screen);
                           this.updateTimer.resetCounter();
                           if (this.boughtKeys.size() > 200) {
                              this.boughtKeys.clear();
                           }
                        }

                        if (!postBuyFreeze) {
                           if (this.buyTimer.hasTimeElapsed(this.effectiveBuyDelay()) && this.scanAndBuy(screen)) {
                              this.buyTimer.resetCounter();
                           }
                        }
                     }
                  } else {
                     this.inAuction = false;
                     this.boughtKeys.clear();
                     this.clearPendingBuy();
                     if (this.autoOpenAh.getValue() && this.ahTimer.hasTimeElapsed(this.ahReopenMs.getValue().longValue())) {
                        mc.player.networkHandler.sendChatCommand("ah");
                        this.ahTimer.resetCounter();
                     }
                  }
               }
            }
         }
      }
   }

   private void startParse(boolean isReparse) {
      AutoBuyManager.get().ensureLoaded();
      this.parseQueue.clear();
      List<AutoBuyItem> source = AutoBuyManager.get().enabledItems(this.serverMode.getValue());
      if (source.isEmpty()) {
         this.msg("§c[AutoParse] Нет включённых предметов для " + this.serverMode.getValue() + " — кликни иконки в AutoBuy GUI");
         this.parseRunning = false;
      } else {
         for (AutoBuyItem item : source) {
            this.parseQueue.add(item.getName());
         }

         this.parseRunning = true;
         this.parseWaitingResult = false;
         this.parseIndex = 0;
         this.parseRetries = 0;
         this.parseUpdatedCount = 0;
         this.parseCurrentName = this.parseQueue.get(0);
         this.parseCmdTimer.resetCounter();
         this.parseWaitTimer.resetCounter();
         this.reparseTimer.resetCounter();
         if (mc.player != null && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         this.msg(
            (isReparse ? "§e[AutoParse] ReParse · " : "§d[AutoParse] §fСтарт · ")
               + this.parseQueue.size()
               + " предм. · скидка "
               + this.parseDiscount.getValue().intValue()
               + "%"
         );
      }
   }

   private void tickParse() {
      if (this.parseQueue.isEmpty()) {
         this.finishParse();
      } else if (this.parseIndex >= this.parseQueue.size()) {
         this.finishParse();
      } else {
         this.parseCurrentName = this.parseQueue.get(this.parseIndex);
         if (!this.parseWaitingResult) {
            if (this.parseCmdTimer.hasTimeElapsed(1100L)) {
               if (mc.player != null) {
                  if (mc.currentScreen != null) {
                     mc.player.closeHandledScreen();
                     this.parseCmdTimer.resetCounter();
                  } else {
                     String search = toSearchQuery(this.parseCurrentName);
                     if (this.serverMode.is("HolyWorld")) {
                        mc.player.networkHandler.sendChatCommand("ah " + search);
                     } else {
                        mc.player.networkHandler.sendChatCommand("ah search " + search);
                     }

                     this.parseWaitingResult = true;
                     this.parseWaitTimer.resetCounter();
                  }
               }
            }
         } else if (!(mc.currentScreen instanceof GenericContainerScreen screen)) {
            if (this.parseWaitTimer.hasTimeElapsed(4500L)) {
               if (++this.parseRetries >= 3) {
                  this.msg("§c[AutoParse] §f" + shortName(this.parseCurrentName) + " §7пропущен: GUI не открылся (3 попытки)");
                  this.advanceParse();
               } else {
                  this.msg("§e[AutoParse] §f" + shortName(this.parseCurrentName) + " §7GUI не открылся, повтор " + this.parseRetries + "/3");
                  this.parseWaitingResult = false;
                  this.parseCmdTimer.resetCounter();
               }
            }
         } else if (this.parseWaitTimer.hasTimeElapsed(700L)) {
            String title = screen.getTitle().getString();
            if (AuctionUtils.isConfirmTitle(title)) {
               this.clickConfirm(screen);
               this.parseWaitTimer.resetCounter();
            } else {
               boolean okGui = AuctionUtils.isAuctionTitle(title)
                  || AuctionUtils.isSearchTitle(title)
                  || this.titleLooksLikeSearchFor(title, this.parseCurrentName)
                  || this.hasPricedLots(screen);
               if (!okGui) {
                  if (this.parseWaitTimer.hasTimeElapsed(5000L)) {
                     if (++this.parseRetries >= 3) {
                        this.msg("§c[AutoParse] §f" + shortName(this.parseCurrentName) + " §7пропущен: неверный GUI");
                        this.advanceParse();
                     } else {
                        this.parseWaitingResult = false;
                        this.parseCmdTimer.resetCounter();
                        if (mc.player != null) {
                           mc.player.closeHandledScreen();
                        }
                     }
                  }
               } else {
                  AutoBuy.ParsePrice result = this.findLowestUnitPrice(screen, this.parseCurrentName);
                  if (result == null) {
                     if (this.parseWaitTimer.hasTimeElapsed(2500L)) {
                        if (++this.parseRetries >= 3) {
                           this.msg("§e[AutoParse] §f" + shortName(this.parseCurrentName) + " §7не найден на AH");
                           this.advanceParse();
                        } else {
                           this.msg("§e[AutoParse] §f" + shortName(this.parseCurrentName) + " §7не найден, повтор " + this.parseRetries + "/3");
                           this.parseWaitingResult = false;
                           this.parseCmdTimer.resetCounter();
                           if (mc.player != null) {
                              mc.player.closeHandledScreen();
                           }
                        }
                     }
                  } else {
                     int discount = this.parseDiscount.getValue().intValue();
                     long discounted = Math.max(1L, (long)result.unitPrice * (100L - (long)discount) / 100L);
                     AutoBuyItem item = AutoBuyManager.get().findByName(this.parseCurrentName, this.serverMode.getValue());
                     if (item != null) {
                        item.setBuyPrice((int)Math.min(2147483647L, discounted));
                        AutoBuyManager.get().scheduleSave();
                        this.parseUpdatedCount++;
                        this.msg(
                           "§d[AutoParse] §f"
                              + shortName(this.parseCurrentName)
                              + " §7→ §a"
                              + discounted
                              + "$ §8(-"
                              + discount
                              + "% от "
                              + result.unitPrice
                              + "$/шт, лот "
                              + result.lotPrice
                              + "$ x"
                              + result.count
                              + ")"
                        );
                     }

                     this.advanceParse();
                  }
               }
            }
         }
      }
   }

   private void advanceParse() {
      this.parseIndex++;
      this.parseRetries = 0;
      this.parseWaitingResult = false;
      this.parseCmdTimer.resetCounter();
      this.parseWaitTimer.resetCounter();
      if (mc.player != null && mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      if (this.parseIndex < this.parseQueue.size()) {
         this.parseCurrentName = this.parseQueue.get(this.parseIndex);
      } else {
         this.finishParse();
      }
   }

   private void finishParse() {
      int updated = this.parseUpdatedCount;
      this.parseRunning = false;
      this.parseWaitingResult = false;
      this.parseIndex = 0;
      this.parseRetries = 0;
      this.parseCurrentName = "";
      this.parseQueue.clear();
      this.parseUpdatedCount = 0;
      this.reparseTimer.resetCounter();
      if (this.reparseMinutes.getValue().intValue() <= 0) {
         this.autoParse.setValue(Boolean.valueOf(false));
      }

      this.msg("§d[AutoParse] §aГотово · обновлено " + updated + " поз.");
   }

   private AutoBuy.ParsePrice findLowestUnitPrice(GenericContainerScreen screen, String targetName) {
      int lowestUnit = -1;
      int bestLot = -1;
      int bestCount = 1;
      int limit = Math.min(45, ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size());
      int matched = 0;

      for (int i = 0; i < limit; i++) {
         Slot slot = (Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(i);
         ItemStack stack = slot.getStack();
         if (!stack.isEmpty()) {
            int price = AuctionUtils.getPrice(stack);
            if (price > 0) {
               boolean nameOk = this.matchName(stack, targetName);
               if ((nameOk || this.titleLooksLikeSearchFor(screen.getTitle().getString(), targetName)) && (nameOk || this.looseMatch(stack, targetName))) {
                  matched++;
                  int count = Math.max(1, stack.getCount());
                  int unit = AuctionUtils.unitPrice(price, count);
                  if (lowestUnit < 0 || unit < lowestUnit || unit == lowestUnit && price < bestLot) {
                     lowestUnit = unit;
                     bestLot = price;
                     bestCount = count;
                  }
               }
            }
         }
      }

      if (lowestUnit < 0 && this.titleLooksLikeSearchFor(screen.getTitle().getString(), targetName)) {
         for (int ix = 0; ix < limit; ix++) {
            Slot slot = (Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(ix);
            ItemStack stack = slot.getStack();
            if (!stack.isEmpty()) {
               int price = AuctionUtils.getPrice(stack);
               if (price > 0) {
                  int count = Math.max(1, stack.getCount());
                  int unit = AuctionUtils.unitPrice(price, count);
                  if (lowestUnit < 0 || unit < lowestUnit) {
                     lowestUnit = unit;
                     bestLot = price;
                     bestCount = count;
                     matched++;
                  }
               }
            }
         }
      }

      return lowestUnit > 0 && (matched != 0 || bestLot > 0) ? new AutoBuy.ParsePrice(lowestUnit, bestLot, bestCount) : null;
   }

   private boolean hasPricedLots(GenericContainerScreen screen) {
      int limit = Math.min(45, ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size());

      for (int i = 0; i < limit; i++) {
         ItemStack stack = ((Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(i)).getStack();
         if (!stack.isEmpty() && AuctionUtils.getPrice(stack) > 0) {
            return true;
         }
      }

      return false;
   }

   private boolean titleLooksLikeSearchFor(String title, String itemName) {
      if (title != null && itemName != null) {
         String t = AuctionUtils.cleanName(title);
         String n = AuctionUtils.cleanName(itemName);
         if (t.isEmpty() || n.isEmpty()) {
            return false;
         } else if (!t.contains(n) && !n.contains(t)) {
            String[] parts = n.split(" ");
            if (parts.length > 0 && parts[0].length() >= 4 && t.contains(parts[0])) {
               return true;
            } else {
               String search = AuctionUtils.cleanName(toSearchQuery(itemName));
               return !search.isEmpty() && t.contains(search);
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   private static String toSearchQuery(String name) {
      if (name == null) {
         return "";
      } else {
         return switch (name) {
            case "Опыт 15" -> "Опыт с уровнем 15";
            case "Опыт 30" -> "Опыт с уровнем 30";
            case "Опыт 45" -> "Опыт с уровнем 45";
            case "Опыт 50" -> "Опыт с уровнем 50";
            default -> name.replaceAll("[★\\[\\]⚒❄\ud83c\udf79]", "").trim();
         };
      }
   }

   private static String shortName(String name) {
      if (name == null) {
         return "?";
      } else {
         return name.length() > 28 ? name.substring(0, 28) + "…" : name;
      }
   }

   private boolean matchName(ItemStack stack, String target) {
      if (stack != null && !stack.isEmpty() && target != null) {
         AutoBuyItem item = AutoBuyManager.get().findByName(target, this.serverMode.getValue());
         if (item != null) {
            return item.matchesName(stack.getName().getString());
         } else {
            String a = AuctionUtils.cleanName(stack.getName().getString());
            String b = AuctionUtils.cleanName(target);
            return !a.isEmpty() && !b.isEmpty() && (a.contains(b) || b.contains(a));
         }
      } else {
         return false;
      }
   }

   private boolean looseMatch(ItemStack stack, String target) {
      String a = AuctionUtils.cleanName(stack.getName().getString());
      String b = AuctionUtils.cleanName(target);
      if (!a.isEmpty() && !b.isEmpty()) {
         if (!a.contains(b) && !b.contains(a)) {
            for (String token : b.split(" ")) {
               if (token.length() >= 4 && a.contains(token)) {
                  return true;
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

   private void handleMenuBind() {
   }

   private void handleAnarchySwap() {
      if (this.anarchySwap.getValue() && !this.parseRunning) {
         if (this.nextAnarchyMs <= 0L) {
            this.nextAnarchyMs = this.randomAnarchyDelay();
         }

         if (this.anarchyTimer.hasTimeElapsed(this.nextAnarchyMs)) {
            int n = ThreadLocalRandom.current().nextInt(1, 101);
            if (mc.currentScreen != null && mc.player != null) {
               mc.player.closeHandledScreen();
            }

            mc.player.networkHandler.sendChatCommand("an" + n);
            this.msg("§eСвап анархии → /an" + n);
            this.nextAnarchyMs = this.randomAnarchyDelay();
            this.anarchyTimer.resetCounter();
            this.inAuction = false;
            this.boughtKeys.clear();
            this.ahTimer.resetCounter();
         }
      }
   }

   private boolean handleFunTimeAnarchy() {
      if (this.anarchySwap.getValue() && this.serverMode.is("FunTime") && !this.parseRunning && mc.player != null) {
         long now = System.currentTimeMillis();
         switch (this.ftSwitch) {
            case IDLE:
               if (this.nextAnarchyMs <= 0L) {
                  this.nextAnarchyMs = this.randomAnarchyDelay();
                  this.anarchyTimer.resetCounter();
                  return false;
               } else {
                  if (!this.anarchyTimer.hasTimeElapsed(this.nextAnarchyMs)) {
                     return false;
                  }

                  this.pendingAnarchy = FT_ANARCHIES[ThreadLocalRandom.current().nextInt(FT_ANARCHIES.length)];
                  this.stopWalkKeys();
                  if (mc.currentScreen != null) {
                     mc.player.closeHandledScreen();
                  }

                  this.ftSwitch = AutoBuy.FtSwitch.SEND;
                  this.ftPhaseAtMs = now;
                  return true;
               }
            case SEND:
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
                  return true;
               }

               mc.player.networkHandler.sendChatCommand("an" + this.pendingAnarchy);
               this.msg("§e[FT] Анархия → /an" + this.pendingAnarchy);
               this.ftPhaseAtMs = now;
               this.ftSwitch = AutoBuy.FtSwitch.WAIT;
               return true;
            case WAIT:
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               if (now - this.ftPhaseAtMs < 12000L) {
                  return true;
               }

               this.ftSwitch = AutoBuy.FtSwitch.OPEN_AH;
               return true;
            case OPEN_AH:
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
                  return true;
               }

               if (this.autoOpenAh.getValue()) {
                  mc.player.networkHandler.sendChatCommand("ah");
               }

               this.nextAnarchyMs = this.randomAnarchyDelay();
               this.anarchyTimer.resetCounter();
               this.ftSwitch = AutoBuy.FtSwitch.IDLE;
               this.pendingAnarchy = -1;
               this.inAuction = false;
               this.boughtKeys.clear();
               this.clearPendingBuy();
               return true;
            default:
               this.ftSwitch = AutoBuy.FtSwitch.IDLE;
               return false;
         }
      } else {
         return false;
      }
   }

   private boolean handleSpookyWalk() {
      if (this.spWalk.getValue() && this.serverMode.is("SpookyTime") && !this.parseRunning && mc.player != null) {
         long now = System.currentTimeMillis();
         double need = Math.max(1.0, this.spWalkBlocks.getValue());
         double needSq = need * need;
         switch (this.walkState) {
            case IDLE:
               if (now < this.nextWalkAtMs) {
                  return false;
               } else {
                  if (!(mc.currentScreen instanceof GenericContainerScreen)) {
                     this.scheduleNextWalk();
                     return false;
                  }

                  this.walkForward = !this.walkForward;
                  mc.player.closeHandledScreen();
                  this.walkCloseAtMs = now;
                  this.walkState = AutoBuy.WalkState.CLOSING;
                  return true;
               }
            case CLOSING:
               if (mc.currentScreen != null) {
                  if (now - this.walkCloseAtMs > 2000L) {
                     this.stopWalkKeys();
                     this.walkState = AutoBuy.WalkState.IDLE;
                     this.scheduleNextWalk();
                  } else {
                     mc.player.closeHandledScreen();
                  }

                  return true;
               }

               this.walkStartX = mc.player.getX();
               this.walkStartZ = mc.player.getZ();
               if (this.walkForward) {
                  mc.options.forwardKey.setPressed(true);
               } else {
                  mc.options.backKey.setPressed(true);
               }

               this.walkState = AutoBuy.WalkState.MOVING;
               return true;
            case MOVING:
               double dx = mc.player.getX() - this.walkStartX;
               double dz = mc.player.getZ() - this.walkStartZ;
               if (dx * dx + dz * dz < needSq) {
                  if (this.walkForward) {
                     mc.options.forwardKey.setPressed(true);
                  } else {
                     mc.options.backKey.setPressed(true);
                  }

                  return true;
               }

               this.stopWalkKeys();
               this.walkState = AutoBuy.WalkState.OPENING;
               return true;
            case OPENING:
               if (mc.currentScreen != null) {
                  return true;
               }

               if (this.autoOpenAh.getValue()) {
                  mc.player.networkHandler.sendChatCommand("ah");
               }

               this.scheduleNextWalk();
               this.walkState = AutoBuy.WalkState.IDLE;
               return true;
            default:
               this.walkState = AutoBuy.WalkState.IDLE;
               return false;
         }
      } else {
         if (this.walkState != AutoBuy.WalkState.IDLE) {
            this.stopWalkKeys();
            this.walkState = AutoBuy.WalkState.IDLE;
         }

         return false;
      }
   }

   private long randomAnarchyDelay() {
      if (this.serverMode.is("FunTime")) {
         long min = Math.max(10L, this.anarchyMinSec.getValue().longValue()) * 1000L;
         long max = Math.max(min + 1000L, this.anarchyMaxSec.getValue().longValue() * 1000L);
         return ThreadLocalRandom.current().nextLong(min, max + 1L);
      } else {
         return ThreadLocalRandom.current().nextLong(300000L, 600001L);
      }
   }

   private boolean isLikelyConfirm(String title, int slots) {
      if (!AuctionUtils.isAuctionTitle(title) && !AuctionUtils.isSearchTitle(title)) {
         String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
         return !t.contains("инвентарь") && !t.contains("inventory") ? slots == 27 || slots == 36 || slots == 45 || slots == 54 || slots == 63 : false;
      } else {
         return false;
      }
   }

   private void clickConfirm(GenericContainerScreen screen) {
      int syncId = ((GenericContainerScreenHandler)screen.getScreenHandler()).syncId;
      int confirmSlot = this.findConfirmSlot(screen);
      if (confirmSlot >= 0) {
         mc.interactionManager.clickSlot(syncId, confirmSlot, 0, SlotActionType.PICKUP, mc.player);
         this.msg("§a✓ Подтверждение покупки");
      } else {
         int[] candidates = new int[]{11, 13, 15, 1, 2, 3, 20, 21, 22, 24};

         for (int slot : candidates) {
            if (slot < ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size()) {
               ItemStack stack = ((Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(slot)).getStack();
               if (!stack.isEmpty()) {
                  mc.interactionManager.clickSlot(syncId, slot, 0, SlotActionType.PICKUP, mc.player);
                  this.msg("§a✓ Подтверждение покупки");
                  return;
               }
            }
         }

         if (((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size() > 1) {
            mc.interactionManager.clickSlot(syncId, 1, 0, SlotActionType.PICKUP, mc.player);
            this.msg("§a✓ Подтверждение (fallback)");
         }
      }
   }

   private int findConfirmSlot(GenericContainerScreen screen) {
      int limit = Math.min(((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size(), 54);

      for (int i = 0; i < limit; i++) {
         ItemStack stack = ((Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(i)).getStack();
         if (!stack.isEmpty()) {
            String n = AuctionUtils.cleanName(stack.getName().getString());
            if (n.contains("купить") || n.contains("подтверд") || n.contains("accept") || n.contains("buy")) {
               return i;
            }

            if (stack.getItem() == Items.LIME_STAINED_GLASS_PANE
               || stack.getItem() == Items.GREEN_STAINED_GLASS_PANE
               || stack.getItem() == Items.LIME_CONCRETE
               || stack.getItem() == Items.GREEN_CONCRETE) {
               return i;
            }
         }
      }

      return -1;
   }

   private void refreshAuction(GenericContainerScreen screen) {
      int syncId = ((GenericContainerScreenHandler)screen.getScreenHandler()).syncId;
      int slots = ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size();
      int refreshSlot = 49;
      if (this.serverMode.is("HolyWorld")) {
         int containerSize = Math.max(9, slots - 36);
         refreshSlot = Math.max(0, containerSize - 7);
         this.boughtKeys.clear();
      }

      if (refreshSlot < slots) {
         mc.interactionManager.clickSlot(syncId, refreshSlot, 0, SlotActionType.PICKUP, mc.player);
      }
   }

   private boolean scanAndBuy(GenericContainerScreen screen) {
      List<AutoBuyItem> enabled = AutoBuyManager.get().enabledItems(this.serverMode.getValue());
      if (enabled.isEmpty()) {
         this.clearPendingBuy();
         return false;
      } else if (mc.player != null && !mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
         this.clearPendingBuy();
         return false;
      } else {
         int syncId = ((GenericContainerScreenHandler)screen.getScreenHandler()).syncId;
         int containerSlots = Math.max(0, ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size() - 36);
         int limit = Math.min(45, containerSlots > 0 ? containerSlots : 45);
         limit = Math.min(limit, ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size());
         AutoBuy.BuyCandidate best = null;

         for (int i = 0; i < limit; i++) {
            Slot slot = (Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(i);
            if (slot.inventory != mc.player.getInventory()) {
               ItemStack stack = slot.getStack();
               if (!stack.isEmpty()) {
                  int price = AuctionUtils.getPrice(stack);
                  if (price > 0) {
                     int count = Math.max(1, stack.getCount());
                     int unit = AuctionUtils.unitPrice(price, count);
                     String name = stack.getName().getString();
                     String key = i + "|" + price + "|" + count + "|" + name.hashCode();
                     if (!this.boughtKeys.contains(key)) {
                        AutoBuyItem bestItem = null;
                        int bestScore = 0;

                        for (AutoBuyItem item : enabled) {
                           int score = item.matchScore(name);
                           if (score > 0 && count >= item.getMinQty()) {
                              int maxUnit = item.getBuyPrice();
                              if (unit <= maxUnit && score > bestScore) {
                                 bestScore = score;
                                 bestItem = item;
                              }
                           }
                        }

                        if (bestItem != null
                           && (
                              best == null
                                 || unit < best.unit
                                 || unit == best.unit && bestScore > best.matchScore
                                 || unit == best.unit && bestScore == best.matchScore && price < best.price
                           )) {
                           best = new AutoBuy.BuyCandidate(i, key, bestItem.getName(), price, count, unit, name, bestScore);
                        }
                     }
                  }
               }
            }
         }

         long now = System.currentTimeMillis();
         if (best == null) {
            this.clearPendingBuy();
            return false;
         } else if (this.pendingSlot != best.slot || !this.pendingKey.equals(best.key)) {
            this.pendingSlot = best.slot;
            this.pendingKey = best.key;
            this.pendingName = best.ruleName;
            this.pendingPrice = best.price;
            this.pendingCount = best.count;
            this.pendingSinceMs = now;
            return false;
         } else if (this.pendingSlot >= 0 && this.pendingSlot < ((GenericContainerScreenHandler)screen.getScreenHandler()).slots.size()) {
            Slot live = (Slot)((GenericContainerScreenHandler)screen.getScreenHandler()).slots.get(this.pendingSlot);
            ItemStack liveStack = live.getStack();
            if (liveStack.isEmpty()) {
               this.clearPendingBuy();
               return false;
            } else {
               int livePrice = AuctionUtils.getPrice(liveStack);
               int liveCount = Math.max(1, liveStack.getCount());
               if (livePrice != this.pendingPrice || liveCount != this.pendingCount) {
                  this.pendingKey = this.pendingSlot + "|" + livePrice + "|" + liveCount + "|" + liveStack.getName().getString().hashCode();
                  this.pendingPrice = livePrice;
                  this.pendingCount = liveCount;
                  this.pendingSinceMs = now;
                  return false;
               } else if (livePrice <= 0) {
                  this.clearPendingBuy();
                  return false;
               } else {
                  long hold = this.serverMode.is("HolyWorld") ? Math.min(60L, this.buyHoldMs.getValue().longValue()) : this.buyHoldMs.getValue().longValue();
                  if (now - this.pendingSinceMs < hold) {
                     return false;
                  } else if (now - this.lastBuyClickMs < 40L) {
                     return false;
                  } else {
                     boolean holy = this.serverMode.is("HolyWorld");
                     SlotActionType click = holy ? SlotActionType.PICKUP : SlotActionType.QUICK_MOVE;
                     int clickSlot = this.pendingSlot;
                     mc.interactionManager.clickSlot(syncId, clickSlot, 0, click, mc.player);
                     this.boughtKeys.add(this.pendingKey);
                     this.lastBuyClickMs = now;
                     long pause = holy ? Math.max(this.postBuyPauseMs.getValue().longValue(), 650L) : this.postBuyPauseMs.getValue().longValue();
                     this.postBuyUntilMs = now + pause;
                     this.msg(
                        "§a⚡ Покупка: §f"
                           + this.pendingName
                           + " §7x"
                           + this.pendingCount
                           + " §aза §e"
                           + this.pendingPrice
                           + "$ §8("
                           + best.unit
                           + "$/шт · "
                           + (holy ? "click" : "shift")
                           + ")"
                     );
                     this.clearPendingBuy();
                     this.updateTimer.resetCounter();
                     return true;
                  }
               }
            }
         } else {
            this.clearPendingBuy();
            return false;
         }
      }
   }

   private long effectiveUpdateDelay() {
      long base = this.updateDelay.getValue().longValue();
      if (this.serverMode.is("HolyWorld")) {
         return Math.max(50L, Math.min(base, 450L));
      } else if (this.serverMode.is("FunTime")) {
         return Math.max(base, ThreadLocalRandom.current().nextLong(180L, 320L));
      } else {
         return this.serverMode.is("SpookyTime") ? Math.max(base, ThreadLocalRandom.current().nextLong(220L, 400L)) : base;
      }
   }

   private long effectiveBuyDelay() {
      long base = this.buyDelay.getValue().longValue();
      if (this.serverMode.is("FunTime")) {
         return Math.max(50L, base);
      } else if (this.serverMode.is("SpookyTime")) {
         return Math.max(50L, base);
      } else {
         return this.serverMode.is("HolyWorld") ? Math.max(40L, base) : base;
      }
   }

   private void msg(String message) {
      if (this.notifications.getValue()) {
         ChatMessage.autobuymessage(message);
      }
   }

   private static record BuyCandidate(int slot, String key, String ruleName, int price, int count, int unit, String rawName, int matchScore) {
   }

   private static enum FtSwitch {
      IDLE,
      SEND,
      WAIT,
      OPEN_AH;

      private FtSwitch() {
      }
   }

   private static record ParsePrice(int unitPrice, int lotPrice, int count) {
   }

   private static enum WalkState {
      IDLE,
      CLOSING,
      MOVING,
      OPENING;

      private WalkState() {
      }
   }
}
