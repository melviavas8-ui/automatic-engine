package daamky.ua.api.module.impl.misc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.Item.TooltipContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.timer.StopWatch;

public final class ExpCrafter extends Module {
   private final StringSetting sellPrice = this.register(new StringSetting("Цена продажи", "Цена выставления опыта на аукцион.", "70000", 32));
   private final StringSetting diamondBuyPrice = this.register(new StringSetting("Цена покупки алмаза", "Максимальная цена стака алмазов.", "235000", 32));
   private final StringSetting goldBuyPrice = this.register(new StringSetting("Цена покупки золота", "Максимальная цена стака золота.", "100000", 32));
   private final NumberSetting minBuyCount = this.register(new NumberSetting("Мин. кол-во для покупки", "Минимальный размер лота.", 64.0, 1.0, 64.0, 1.0));
   private final BooleanSetting unitPriceBuyEnabled = this.register(new BooleanSetting("Покупка по цене за 1", "Сравнивать лоты по цене за единицу.", true));
   private final NumberSetting sellDelay = this.register(new NumberSetting("Задержка продажи (мс)", "Пауза между выставлениями.", 600.0, 100.0, 3000.0, 50.0));
   private final NumberSetting buyDelay = this.register(
      new NumberSetting("Задержка покупки (мс)", "Пауза между кликами на аукционе.", 100.0, 50.0, 500.0, 10.0)
   );
   private final NumberSetting craftSpeed = this.register(new NumberSetting("Скорость крафта (мс)", "Пауза между шагами крафта.", 70.0, 0.0, 500.0, 10.0));
   private final BooleanSetting autoAimTable = this.register(new BooleanSetting("Автонаводка на верстак", "Сам наводится и возвращается к верстаку.", true));
   private final BooleanSetting antiAfkEnabled = this.register(new BooleanSetting("Анти-AFK", "Ходит туда-сюда и возвращается на анархию.", true));
   private final NumberSetting antiAfkDelay = this.register(new NumberSetting("Интервал анти-AFK (сек)", "Как часто делать проходку.", 25.0, 10.0, 120.0, 5.0));
   private final StringSetting anarchyNumber = this.register(new StringSetting("Номер анархии", "Куда заходить после кика в хаб.", "903", 8));
   private final BooleanSetting clanInvestEnabled = this.register(new BooleanSetting("Клан инвест", "Периодически сливать баланс в казну клана.", true));
   private final StringSetting clanInvestThreshold = this.register(new StringSetting("Оставлять на балансе", "Сколько не трогать.", "1000000", 32));
   private final StringSetting clanInvestAmount = this.register(new StringSetting("Мин. инвест", "Ниже этой суммы не инвестировать.", "1", 32));
   private final NumberSetting clanInvestInterval = this.register(
      new NumberSetting("Интервал инвеста (сек)", "Как часто запрашивать баланс.", 60.0, 10.0, 3600.0, 10.0)
   );
   private ExpCrafter.State state = ExpCrafter.State.OPEN_TABLE;
   private ExpCrafter.BuyPhase buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
   private ExpCrafter.ChestPhase chestPhase = ExpCrafter.ChestPhase.IDLE;
   private final StopWatch timer = new StopWatch();
   private final StopWatch buyTimer = new StopWatch();
   private final StopWatch antiAfkTimer = new StopWatch();
   private final StopWatch antiAfkRecoveryTimer = new StopWatch();
   private static final long DELAY = 50L;
   private int buyTargetSlot = -1;
   private long refreshDelay = 2000L;
   private long postRefreshDelay = 150L;
   private int pendingSellHotbarSlot = -1;
   private boolean sellSelectionConfirmed = false;
   private boolean sellPaused = false;
   private volatile long sessionEarned = 0L;
   private volatile long clanTreasuryBalance = -1L;
   private long currentBalance = -1L;
   private boolean balancePending = false;
   private final StopWatch clanInvestTimer = new StopWatch();
   private ExpCrafter.RepostPhase repostPhase = ExpCrafter.RepostPhase.IDLE;
   private final StopWatch repostTimer = new StopWatch();
   private final StopWatch repostStepTimer = new StopWatch();
   private boolean repostPending = false;
   private boolean repostSucceeded = false;
   private int repostAttempt = 0;
   private long repostWaitMs = 3000L;
   private long repostActionDelayMs = 700L;
   private float targetYaw;
   private float targetPitch;
   private float startYaw;
   private float startPitch;
   private long rotateStartTime;
   private long rotateDuration;
   private boolean rotating = false;
   private BlockPos chestPos = null;
   private BlockPos tablePos = null;
   private BlockPos homeTablePos = null;
   private final StopWatch chestStepTimer = new StopWatch();
   private boolean waitingTableRotation = false;
   private int antiAfkStage = 0;
   private long antiAfkIntervalMs = 25000L;
   private ExpCrafter.AntiAfkRecoveryPhase antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.NONE;
   private Object antiAfkTrackedWorld = null;
   private Object antiAfkTrackedPlayer = null;
   private boolean antiAfkJoinDetected = false;
   private long antiAfkLastTableProbeAt = 0L;
   private long antiAfkHomeRetryAt = 0L;
   private final List<BlockPos> antiAfkPath = new ArrayList<>();
   private int antiAfkPathIndex = 0;
   private int antiAfkStuckTicks = 0;
   private int antiAfkRepathAttempts = 0;
   private Vec3d antiAfkLastPosition = Vec3d.ZERO;
   private Vec3d antiAfkPreviousPosition = Vec3d.ZERO;
   private boolean antiAfkPositionInitialized = false;
   private BlockPos antiAfkBackTarget = null;
   private BlockPos antiAfkTableTarget = null;
   private int antiAfkPauseTicks = 0;
   private int antiAfkTurnPathIndex = -1;
   private float antiAfkTurnStartYaw;
   private float antiAfkTurnTargetYaw;
   private long antiAfkTurnStartTime;
   private long antiAfkTurnDuration;
   private int antiAfkOffsetPathIndex = -1;
   private double antiAfkWaypointOffsetX;
   private double antiAfkWaypointOffsetZ;
   private int expCraftStep = -1;
   private int expCraftAmount = 0;
   private int nuggetCraftStep = 0;
   private int nuggetSourceSlot = -1;
   private int nuggetResultWaitTicks = 0;
   private int placementTargetSlot = -1;
   private int placementSourceSlot = -1;
   private Item placementItem = null;
   private int quickCraftStage = 0;
   private int quickCraftSlotIndex = 0;
   private int expRecipeWaitTicks = 0;
   private int chestTransferStep = 0;
   private boolean chestTookGold = false;
   private boolean chestTookDiamond = false;

   public ExpCrafter() {
      super("ExpCrafter", "Крафтит кусочки золота, пузырьки опыта и продаёт", ModuleCategory.MISC);
      this.antiAfkDelay.visibleWhen(this.antiAfkEnabled::getValue);
      this.anarchyNumber.visibleWhen(this.antiAfkEnabled::getValue);
      this.clanInvestThreshold.visibleWhen(this.clanInvestEnabled::getValue);
      this.clanInvestAmount.visibleWhen(this.clanInvestEnabled::getValue);
      this.clanInvestInterval.visibleWhen(this.clanInvestEnabled::getValue);
   }

   @SubscribeEvent
   private void onChat(PacketEvent event) {
      if (this.isEnabled()) {
         if (event.isReceive() && event.getPacket() instanceof GameMessageS2CPacket packet) {
            if (packet.content() != null) {
               String msg = packet.content().getString().toLowerCase(Locale.ROOT);
               if (msg.contains("после входа на режим") && msg.contains("аукцион")) {
                  this.startPostJoinStabilization();
               } else if (this.isHubChatMessage(msg) && mc.player != null) {
                  this.startAntiAfkRecovery(this.getPlayerPosition());
               } else {
                  this.updateEconomyStats(msg);
                  if (!this.handlePurchaseConfirmation(msg)) {
                     if (msg.contains("не удалось выставить") && msg.contains("опыт")) {
                        if (!this.sellPaused && this.repostPhase == ExpCrafter.RepostPhase.IDLE) {
                           this.repostAttempt = 0;
                        }

                        this.sellPaused = true;
                        this.repostWaitMs = this.randomDelay(800L, 1000L);
                        this.repostPending = true;
                        this.repostTimer.reset();
                        this.sendMsg("Хранилище заполнено, плавно запускаю перевыставление...");
                     } else if ((this.sellPaused || this.repostPending) && msg.contains("подождите") && msg.contains("сек")) {
                        try {
                           Matcher m = Pattern.compile("(\\d+)\\s*сек").matcher(msg);
                           if (m.find()) {
                              long secs = Long.parseLong(m.group(1));
                              this.repostWaitMs = secs * 1000L + this.randomDelay(500L, 1000L);
                              this.repostPending = true;
                              this.repostTimer.reset();
                              this.sendMsg("Перевыставлю примерно через " + (this.repostWaitMs + 999L) / 1000L + " сек...");
                           }
                        } catch (Exception var13) {
                        }
                     } else if ((this.sellPaused || this.repostPhase != ExpCrafter.RepostPhase.IDLE) && msg.contains("предметы успешно перевыставлены")) {
                        this.repostSucceeded = true;
                        this.repostPending = false;
                     } else if (!msg.contains("у вас купили опыт") && (!msg.contains("купили") || !msg.contains("опыт"))) {
                        if (this.balancePending && msg.contains("ваш баланс")) {
                           try {
                              Matcher m = Pattern.compile("\\$([\\d\\s,\\.]+)").matcher(msg);
                              if (m.find()) {
                                 String numStr = m.group(1).replaceAll("[^\\d]", "");
                                 this.currentBalance = Long.parseLong(numStr);
                                 this.balancePending = false;
                                 long reserve = (long)this.parsePriceOrZero(this.clanInvestThreshold.getValue());
                                 long minInvest = (long)this.parsePriceOrZero(this.clanInvestAmount.getValue());
                                 long investAmount = this.currentBalance - reserve;
                                 if (investAmount >= minInvest && investAmount > 0L) {
                                    this.sendCommand("/clan invest " + investAmount);
                                    this.sendMsg("Инвестировал " + investAmount + " в клан (баланс: " + this.currentBalance + ", оставил: " + reserve + ")");
                                 }
                              }
                           } catch (Exception var12) {
                           }
                        }
                     } else {
                        this.sellPaused = false;
                        this.repostPending = false;
                        this.repostSucceeded = false;
                        this.repostAttempt = 0;
                        this.repostPhase = ExpCrafter.RepostPhase.IDLE;
                        this.sendMsg("Куплено, продолжаю продажу.");
                     }
                  }
               }
            }
         }
      }
   }

   private void updateEconomyStats(String msg) {
      try {
         if (msg.contains("баланс казны")) {
            Matcher treasuryMatcher = Pattern.compile("баланс казны\\s*:\\s*([\\d\\s,.]+)").matcher(msg);
            if (treasuryMatcher.find()) {
               String digits = treasuryMatcher.group(1).replaceAll("[^\\d]", "");
               if (!digits.isEmpty()) {
                  this.clanTreasuryBalance = Long.parseLong(digits);
               }
            }
         }

         Matcher saleMatcher = Pattern.compile("у вас купили\\s+опыт обычный\\s+x(\\d+)\\s+за\\s+\\$?([\\d\\s,.]+)").matcher(msg);
         if (!saleMatcher.find()) {
            return;
         }

         String digits = saleMatcher.group(2).replaceAll("[^\\d]", "");
         if (digits.isEmpty()) {
            return;
         }

         long price = Long.parseLong(digits);
         this.sessionEarned = Long.MAX_VALUE - this.sessionEarned < price ? Long.MAX_VALUE : this.sessionEarned + price;
      } catch (NumberFormatException var6) {
      }
   }

   public long getSessionEarned() {
      return this.sessionEarned;
   }

   public long getClanTreasuryBalance() {
      return this.clanTreasuryBalance;
   }

   private boolean handlePurchaseConfirmation(String msg) {
      if (!msg.contains("вы успешно купили")) {
         return false;
      } else {
         ExpCrafter.State nextState;
         String itemName;
         if (this.state == ExpCrafter.State.BUY_DIAMONDS && msg.contains("алмаз")) {
            nextState = ExpCrafter.State.CHECK_GOLD;
            itemName = "алмазы";
         } else {
            if (this.state != ExpCrafter.State.BUY_GOLD || !msg.contains("золот")) {
               return false;
            }

            nextState = ExpCrafter.State.OPEN_TABLE;
            itemName = "золото";
         }

         this.forceCloseAuctionScreen();
         this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
         this.buyTargetSlot = -1;
         this.state = nextState;
         this.buyTimer.reset();
         this.timer.reset();
         this.sendMsg("Покупка подтверждена: " + itemName + ".");
         return true;
      }
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (this.isEnabled()) {
         if (mc.player != null && mc.world != null && mc.interactionManager != null) {
            if (!this.tickAntiAfk()) {
               this.tickRepost();
               this.tickClanInvest();
               if (this.repostPhase == ExpCrafter.RepostPhase.IDLE) {
                  if (this.rotating) {
                     this.tickRotation();
                  } else {
                     if (this.waitingTableRotation) {
                        this.waitingTableRotation = false;
                        this.timer.reset();
                     }

                     if (this.timer.finished(50.0)) {
                        this.tickAuto();
                     }
                  }
               }
            }
         }
      }
   }

   private void tickAuto() {
      switch (this.state) {
         case BUY_DIAMONDS:
            this.tickBuy(Items.DIAMOND, this.diamondBuyPrice.getValue(), "Алмаз", ExpCrafter.State.CHECK_GOLD);
            break;
         case CHECK_GOLD:
            this.tickCheckGold();
            break;
         case BUY_GOLD:
            this.tickBuy(Items.GOLD_INGOT, this.goldBuyPrice.getValue(), "Золотой слиток", ExpCrafter.State.OPEN_TABLE);
            break;
         case OPEN_TABLE:
            this.openTable();
            break;
         case CRAFT_NUGGETS:
            if (this.timer.finished((double)this.getCraftDelay())) {
               this.craftNuggets();
            }
            break;
         case CRAFT_NUGGETS_TAKE:
            this.craftNuggetsTake();
            break;
         case CRAFT_EXP:
            if (this.timer.finished((double)this.getCraftDelay())) {
               this.craftExp();
            }
            break;
         case CRAFT_EXP_TAKE:
            if (this.timer.finished((double)this.getCraftDelay())) {
               this.craftExpTake();
            }
            break;
         case CLOSE_TABLE:
            this.closeTable();
            break;
         case SELL:
            this.sellExpBottles();
            break;
         case SELL_VERIFY:
            this.verifyAndSellExpBottle();
            break;
         case DONE:
            this.tickDoneAuto();
            break;
         default:
            this.state = ExpCrafter.State.OPEN_TABLE;
      }
   }

   private void tickChest() {
      switch (this.state) {
         case OPEN_TABLE:
            this.openTableChest();
            break;
         case CRAFT_NUGGETS:
            if (this.timer.finished((double)this.getCraftDelay())) {
               this.craftNuggets();
            }
            break;
         case CRAFT_NUGGETS_TAKE:
            this.craftNuggetsTake();
            break;
         case CRAFT_EXP:
            if (this.timer.finished((double)this.getCraftDelay())) {
               this.craftExp();
            }
            break;
         case CRAFT_EXP_TAKE:
            if (this.timer.finished((double)this.getCraftDelay())) {
               this.craftExpTake();
            }
            break;
         case CLOSE_TABLE:
            this.closeTableChest();
            break;
         case SELL:
         case SELL_VERIFY:
         default:
            this.state = ExpCrafter.State.CHEST_TAKE_ITEMS;
            break;
         case DONE:
            this.tickDoneChest();
            break;
         case CHEST_TAKE_ITEMS:
            this.tickChestTakeItems();
            break;
         case CHEST_PUT_EXP:
            this.tickChestPutExp();
      }
   }

   private void tickChestTakeItems() {
      switch (this.chestPhase) {
         case IDLE:
            this.chestTransferStep = 0;
            this.chestTookGold = false;
            this.chestTookDiamond = false;
            this.chestPos = this.findNearbyChest();
            if (this.chestPos == null) {
               this.sendMsg("[Chest] Сундук не найден рядом!");
               this.setEnabled(false);
               return;
            }

            this.startSmoothRotateTo(this.chestPos);
            this.chestPhase = ExpCrafter.ChestPhase.ROTATE_TO_CHEST;
            break;
         case ROTATE_TO_CHEST:
            if (this.rotating) {
               return;
            }

            this.chestPhase = ExpCrafter.ChestPhase.OPEN_CHEST;
            this.chestStepTimer.reset();
            break;
         case OPEN_CHEST:
            if (!this.chestStepTimer.finished(300.0)) {
               return;
            }

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(this.chestPos.toCenterPos(), Direction.UP, this.chestPos, false));
            this.chestStepTimer.reset();
            this.chestPhase = ExpCrafter.ChestPhase.TAKE_ITEMS;
            break;
         case TAKE_ITEMS:
            if (!this.chestStepTimer.finished(400.0)) {
               return;
            }

            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
               this.chestTransferStep = 0;
               this.chestTookGold = false;
               this.chestTookDiamond = false;
               this.chestPhase = ExpCrafter.ChestPhase.IDLE;
               return;
            }

            if (this.chestTransferStep == 0) {
               this.chestTookGold = this.takeOneStackFromChest(handler, Items.GOLD_INGOT);
               this.chestTransferStep = 1;
               this.chestStepTimer.reset();
               return;
            }

            if (this.chestTransferStep == 1) {
               this.chestTookDiamond = this.takeOneStackFromChest(handler, Items.DIAMOND);
               this.chestTransferStep = 0;
               if (!this.chestTookGold && !this.chestTookDiamond) {
                  this.sendMsg("[Chest] В сундуке нет золота/алмазов!");
               }

               this.chestTookGold = false;
               this.chestTookDiamond = false;
            }

            this.chestStepTimer.reset();
            this.chestPhase = ExpCrafter.ChestPhase.CLOSE_CHEST;
            break;
         case CLOSE_CHEST:
            if (!this.chestStepTimer.finished(300.0)) {
               return;
            }

            if (mc.currentScreen != null) {
               mc.player.closeHandledScreen();
            }

            if (this.tablePos != null) {
               this.startSmoothRotateTo(this.tablePos);
               this.chestPhase = ExpCrafter.ChestPhase.ROTATE_TO_TABLE;
            } else {
               this.chestPhase = ExpCrafter.ChestPhase.IDLE;
               this.state = ExpCrafter.State.OPEN_TABLE;
            }
            break;
         case ROTATE_TO_TABLE:
            if (this.rotating) {
               return;
            }

            this.chestPhase = ExpCrafter.ChestPhase.IDLE;
            this.state = ExpCrafter.State.OPEN_TABLE;
            break;
         default:
            this.chestPhase = ExpCrafter.ChestPhase.IDLE;
      }
   }

   private void openTableChest() {
      if (this.tablePos == null) {
         this.tablePos = this.findNearbyTable();
      }

      if (this.tablePos == null) {
         this.sendMsg("[Chest] Верстак не найден рядом!");
         this.setEnabled(false);
      } else {
         mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(this.tablePos.toCenterPos(), Direction.UP, this.tablePos, false));
         this.timer.reset();
         this.state = ExpCrafter.State.CRAFT_NUGGETS;
      }
   }

   private void closeTableChest() {
      if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
         mc.player.closeHandledScreen();
      }

      this.chestPhase = ExpCrafter.ChestPhase.ROTATE_TO_CHEST_PUT;
      this.state = ExpCrafter.State.CHEST_PUT_EXP;
      this.timer.reset();
   }

   private void tickChestPutExp() {
      switch (this.chestPhase) {
         case ROTATE_TO_CHEST_PUT:
            if (this.chestPos == null) {
               this.chestPos = this.findNearbyChest();
            }

            if (this.chestPos == null) {
               this.state = ExpCrafter.State.DONE;
               this.chestPhase = ExpCrafter.ChestPhase.IDLE;
               return;
            }

            this.startSmoothRotateTo(this.chestPos);
            this.chestPhase = ExpCrafter.ChestPhase.OPEN_CHEST_PUT;
            break;
         case OPEN_CHEST_PUT:
            if (this.rotating) {
               return;
            }

            if (!this.chestStepTimer.finished(300.0)) {
               return;
            }

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(this.chestPos.toCenterPos(), Direction.UP, this.chestPos, false));
            this.chestStepTimer.reset();
            this.chestPhase = ExpCrafter.ChestPhase.PUT_EXP;
            break;
         case PUT_EXP:
            if (!this.chestStepTimer.finished(400.0)) {
               return;
            }

            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
               this.chestPhase = ExpCrafter.ChestPhase.IDLE;
               this.state = ExpCrafter.State.DONE;
               return;
            }

            if (this.putOneExpStackToChest(handler)) {
               this.chestStepTimer.reset();
               return;
            }

            this.chestStepTimer.reset();
            this.chestPhase = ExpCrafter.ChestPhase.CLOSE_CHEST_PUT;
            break;
         case CLOSE_CHEST_PUT:
            if (!this.chestStepTimer.finished(300.0)) {
               return;
            }

            if (mc.currentScreen != null) {
               mc.player.closeHandledScreen();
            }

            this.chestPhase = ExpCrafter.ChestPhase.IDLE;
            this.state = ExpCrafter.State.DONE;
            break;
         default:
            this.chestPhase = ExpCrafter.ChestPhase.IDLE;
            this.state = ExpCrafter.State.DONE;
      }
   }

   private void tickDoneChest() {
      boolean hasGold = this.hasEnoughGoldMaterial();
      boolean hasDiamond = this.countInInventory(Items.DIAMOND) > 0;
      if (hasGold && hasDiamond) {
         this.state = ExpCrafter.State.OPEN_TABLE;
      } else {
         this.chestPhase = ExpCrafter.ChestPhase.IDLE;
         this.state = ExpCrafter.State.CHEST_TAKE_ITEMS;
      }
   }

   private void startSmoothRotateTo(BlockPos pos) {
      if (mc.player != null) {
         Vec3d center = pos.toCenterPos();
         Vec3d eyes = mc.player.getEyePos();
         double dx = center.x - eyes.x;
         double dy = center.y - eyes.y;
         double dz = center.z - eyes.z;
         double dist = Math.sqrt(dx * dx + dz * dz);
         this.startYaw = mc.player.getYaw();
         this.startPitch = mc.player.getPitch();
         this.targetYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
         this.targetPitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
         float yawDiff = this.targetYaw - this.startYaw;

         while (yawDiff > 180.0F) {
            yawDiff -= 360.0F;
         }

         while (yawDiff < -180.0F) {
            yawDiff += 360.0F;
         }

         this.targetYaw = this.startYaw + yawDiff;
         this.rotateDuration = 1000L + (long)(Math.random() * 1000.0);
         this.rotateStartTime = System.currentTimeMillis();
         this.rotating = true;
      }
   }

   private void startStrongRotateTo(BlockPos pos) {
      if (mc.player != null) {
         Vec3d aimPoint = pos.toCenterPos().add((Math.random() - 0.5) * 0.22, (Math.random() - 0.5) * 0.18, (Math.random() - 0.5) * 0.22);
         float[] rotations = this.calculateRotations(aimPoint);
         this.startYaw = mc.player.getYaw();
         this.startPitch = mc.player.getPitch();
         this.targetYaw = rotations[0];
         this.targetPitch = rotations[1];
         float yawDiff = this.targetYaw - this.startYaw;

         while (yawDiff > 180.0F) {
            yawDiff -= 360.0F;
         }

         while (yawDiff < -180.0F) {
            yawDiff += 360.0F;
         }

         this.targetYaw = this.startYaw + yawDiff;
         this.rotateDuration = 420L + (long)(Math.random() * 281.0);
         this.rotateStartTime = System.currentTimeMillis();
         this.rotating = true;
      }
   }

   private void tickRotation() {
      if (this.rotating) {
         long elapsed = System.currentTimeMillis() - this.rotateStartTime;
         float t = Math.min(1.0F, (float)elapsed / (float)this.rotateDuration);
         float smooth = t < 0.5F ? 2.0F * t * t : 1.0F - (float)Math.pow((double)(-2.0F * t + 2.0F), 2.0) / 2.0F;
         this.applyAim(this.startYaw + (this.targetYaw - this.startYaw) * smooth, this.startPitch + (this.targetPitch - this.startPitch) * smooth);
         if (t >= 1.0F) {
            this.rotating = false;
         }
      }
   }

   private BlockPos findNearbyChest() {
      if (mc.player != null && mc.world != null) {
         BlockPos origin = mc.player.getBlockPos();

         for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
               for (int dz = -4; dz <= 4; dz++) {
                  BlockPos p = origin.add(dx, dy, dz);
                  Block block = mc.world.getBlockState(p).getBlock();
                  if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST) {
                     return p;
                  }
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private BlockPos findNearbyTable() {
      if (mc.player != null && mc.world != null) {
         BlockPos origin = mc.player.getBlockPos();
         BlockPos nearest = null;
         int nearestDistance = Integer.MAX_VALUE;

         for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
               for (int dz = -4; dz <= 4; dz++) {
                  BlockPos p = origin.add(dx, dy, dz);
                  if (mc.world.getBlockState(p).getBlock() == Blocks.CRAFTING_TABLE) {
                     int distance = dx * dx + dy * dy + dz * dz;
                     if (distance < nearestDistance) {
                        nearest = p;
                        nearestDistance = distance;
                     }
                  }
               }
            }
         }

         return nearest;
      } else {
         return null;
      }
   }

   private BlockHitResult getLookedAtCraftingTable() {
      if (mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK) {
         return mc.world.getBlockState(bhr.getBlockPos()).getBlock() == Blocks.CRAFTING_TABLE ? bhr : null;
      }

      return null;
   }

   private float[] calculateRotations(Vec3d target) {
      Vec3d eyes = mc.player.getEyePos();
      double dx = target.x - eyes.x;
      double dy = target.y - eyes.y;
      double dz = target.z - eyes.z;
      double dist = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
      float pitch = (float)(-Math.toDegrees(Math.atan2(dy, dist)));
      return new float[]{yaw, pitch};
   }

   private void applyAim(float yaw, float pitch) {
      mc.player.setYaw(yaw);
      mc.player.setPitch(pitch);
      mc.player.setHeadYaw(yaw);
      mc.player.setBodyYaw(yaw);
   }

   private boolean takeOneStackFromChest(GenericContainerScreenHandler handler, Item item) {
      int containerSize = handler.slots.size() - 36;

      for (int i = 0; i < containerSize; i++) {
         ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
         if (stack.getItem() == item && !stack.isEmpty()) {
            this.clickSlot(handler, i, 0, SlotActionType.QUICK_MOVE);
            return true;
         }
      }

      return false;
   }

   private boolean putOneExpStackToChest(GenericContainerScreenHandler handler) {
      int containerSize = handler.slots.size() - 36;

      for (int i = containerSize; i < handler.slots.size(); i++) {
         ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
         if (stack.getItem() == Items.EXPERIENCE_BOTTLE && !stack.isEmpty()) {
            this.clickSlot(handler, i, 0, SlotActionType.QUICK_MOVE);
            return true;
         }
      }

      return false;
   }

   @Override
   protected void onEnable() {
      this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
      this.buyTargetSlot = -1;
      this.postRefreshDelay = 150L;
      this.pendingSellHotbarSlot = -1;
      this.sellSelectionConfirmed = false;
      this.sellPaused = false;
      this.repostPhase = ExpCrafter.RepostPhase.IDLE;
      this.repostTimer.reset();
      this.repostPending = false;
      this.repostSucceeded = false;
      this.repostAttempt = 0;
      this.repostWaitMs = 3000L;
      this.repostActionDelayMs = 700L;
      this.currentBalance = -1L;
      this.balancePending = false;
      this.clanInvestTimer.reset();
      this.timer.reset();
      this.buyTimer.reset();
      this.antiAfkTimer.reset();
      this.chestPhase = ExpCrafter.ChestPhase.IDLE;
      this.rotating = false;
      this.chestPos = null;
      this.tablePos = null;
      this.homeTablePos = null;
      this.waitingTableRotation = false;
      this.antiAfkStage = 0;
      this.antiAfkIntervalMs = this.getAntiAfkDelay();
      this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.WAIT_STABILIZE;
      this.antiAfkRecoveryTimer.reset();
      this.antiAfkTrackedWorld = mc.world;
      this.antiAfkTrackedPlayer = mc.player;
      this.antiAfkJoinDetected = false;
      this.antiAfkLastTableProbeAt = 0L;
      this.antiAfkHomeRetryAt = 0L;
      if (mc.player != null) {
         this.antiAfkPreviousPosition = this.getPlayerPosition();
      }

      this.antiAfkPositionInitialized = false;
      this.expCraftStep = -1;
      this.expCraftAmount = 0;
      this.resetCraftClickState();
      this.chestTransferStep = 0;
      this.chestTookGold = false;
      this.chestTookDiamond = false;
      this.resetAntiAfkMovement();
      this.sendMsg("ExpCrafter запустится через 15 сек...");
      if (mc.player == null) {
         this.state = ExpCrafter.State.OPEN_TABLE;
      } else {
         boolean hasDiamonds = this.countInInventory(Items.DIAMOND) > 0;
         boolean hasGold = this.hasEnoughGoldMaterial();
         int dp = this.parsePriceOrZero(this.diamondBuyPrice.getValue());
         int gp = this.parsePriceOrZero(this.goldBuyPrice.getValue());
         if (hasDiamonds && hasGold) {
            this.state = ExpCrafter.State.OPEN_TABLE;
         } else if (!hasDiamonds && dp > 0) {
            this.state = ExpCrafter.State.BUY_DIAMONDS;
         } else if (!hasGold && gp > 0) {
            this.state = ExpCrafter.State.BUY_GOLD;
         } else {
            this.state = ExpCrafter.State.OPEN_TABLE;
         }
      }
   }

   @Override
   protected void onDisable() {
      if (mc.player != null && mc.currentScreen != null) {
         mc.player.closeHandledScreen();
      }

      this.state = ExpCrafter.State.OPEN_TABLE;
      this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
      this.repostPhase = ExpCrafter.RepostPhase.IDLE;
      this.chestPhase = ExpCrafter.ChestPhase.IDLE;
      this.buyTargetSlot = -1;
      this.postRefreshDelay = 150L;
      this.pendingSellHotbarSlot = -1;
      this.sellSelectionConfirmed = false;
      this.sellPaused = false;
      this.repostPending = false;
      this.repostSucceeded = false;
      this.repostAttempt = 0;
      this.repostWaitMs = 3000L;
      this.repostActionDelayMs = 700L;
      this.rotating = false;
      this.chestPos = null;
      this.tablePos = null;
      this.homeTablePos = null;
      this.waitingTableRotation = false;
      this.antiAfkStage = 0;
      this.antiAfkIntervalMs = this.getAntiAfkDelay();
      this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.NONE;
      this.antiAfkTrackedWorld = null;
      this.antiAfkTrackedPlayer = null;
      this.antiAfkJoinDetected = false;
      this.antiAfkLastTableProbeAt = 0L;
      this.antiAfkHomeRetryAt = 0L;
      this.antiAfkRecoveryTimer.reset();
      this.antiAfkPositionInitialized = false;
      this.expCraftStep = -1;
      this.expCraftAmount = 0;
      this.resetCraftClickState();
      this.chestTransferStep = 0;
      this.chestTookGold = false;
      this.chestTookDiamond = false;
      this.resetAntiAfkMovement();
      this.timer.reset();
      this.buyTimer.reset();
      this.repostTimer.reset();
      this.repostStepTimer.reset();
      this.chestStepTimer.reset();
      this.antiAfkTimer.reset();
   }

   private void tickBuy(Item targetItem, String maxPriceStr, String searchQuery, ExpCrafter.State nextState) {
      if (this.hasRequiredItemForState(targetItem)) {
         if (mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
         this.buyTargetSlot = -1;
         this.decideNextStateAfterBuy();
         this.buyTimer.reset();
      } else {
         int maxPrice = this.parsePriceOrZero(maxPriceStr);
         if (maxPrice <= 0) {
            this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
            this.state = nextState;
         } else {
            switch (this.buyPhase) {
               case OPEN_AH:
                  if (mc.currentScreen != null) {
                     mc.player.closeHandledScreen();
                     this.buyTimer.reset();
                     return;
                  }

                  this.sendCommand("/ah search " + searchQuery);
                  this.buyPhase = ExpCrafter.BuyPhase.WAIT_AH;
                  this.buyTimer.reset();
                  break;
               case WAIT_AH:
                  if (!this.buyTimer.finished(650.0)) {
                     return;
                  }

                  if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                     this.buyPhase = ExpCrafter.BuyPhase.SEARCH;
                     this.buyTimer.reset();
                  } else if (this.buyTimer.finished(3000.0)) {
                     this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
                     this.buyTimer.reset();
                  }
                  break;
               case SEARCH:
                  if (!this.buyTimer.finished(220.0)) {
                     return;
                  }

                  if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler)) {
                     this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
                     this.buyTimer.reset();
                     return;
                  }

                  this.buyTargetSlot = this.findCheapSlot(targetItem, maxPrice);
                  if (this.buyTargetSlot != -1) {
                     this.buyPhase = ExpCrafter.BuyPhase.BUY_SLOT;
                     this.buyTimer.reset();
                  } else {
                     this.refreshDelay = this.getRandomRefreshDelay();
                     this.postRefreshDelay = this.getRandomBuyDelayAfterRefresh();
                     this.buyPhase = ExpCrafter.BuyPhase.REFRESH;
                     this.buyTimer.reset();
                  }
                  break;
               case REFRESH:
                  if (!this.buyTimer.finished((double)this.refreshDelay)) {
                     return;
                  }

                  if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
                     this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
                     this.buyTimer.reset();
                     return;
                  }

                  int refreshSlot = this.findAuctionRefreshSlot(handler);
                  if (refreshSlot == -1) {
                     this.refreshDelay = this.getRandomRefreshDelay();
                     this.buyTimer.reset();
                     return;
                  }

                  this.clickSlot(handler, refreshSlot, 0, SlotActionType.PICKUP);
                  this.buyPhase = ExpCrafter.BuyPhase.WAIT_REFRESH;
                  this.buyTimer.reset();
                  break;
               case WAIT_REFRESH:
                  if (!this.buyTimer.finished((double)this.postRefreshDelay)) {
                     return;
                  }

                  this.buyPhase = ExpCrafter.BuyPhase.SEARCH;
                  this.buyTimer.setMs(220L);
                  break;
               case BUY_SLOT:
                  if (!this.buyTimer.finished((double)this.getAuctionBuyClickDelay())) {
                     return;
                  }

                  if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
                     this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
                     this.buyTargetSlot = -1;
                     this.buyTimer.reset();
                     return;
                  }

                  this.clickSlot(handler, this.buyTargetSlot, 0, SlotActionType.QUICK_MOVE);
                  this.buyPhase = ExpCrafter.BuyPhase.WAIT_CLOSE;
                  this.buyTimer.reset();
                  break;
               case WAIT_CLOSE:
                  if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
                     if (!this.buyTimer.finished((double)this.getAuctionCloseDelay())) {
                        return;
                     }

                     mc.player.closeHandledScreen();
                  }

                  if (this.hasRequiredItemForState(targetItem)) {
                     this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
                     this.buyTargetSlot = -1;
                     this.decideNextStateAfterBuy();
                     this.timer.reset();
                     this.buyTimer.reset();
                     return;
                  }

                  if (!this.buyTimer.finished((double)this.getAuctionPostBuyResolveDelay())) {
                     return;
                  }

                  this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
                  this.buyTargetSlot = -1;
                  this.decideNextStateAfterBuy();
                  this.timer.reset();
                  this.buyTimer.reset();
            }
         }
      }
   }

   private int findCheapSlot(Item targetItem, int maxPrice) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
         int var13 = handler.slots.size() - 36;
         int minCount = this.unitPriceBuyEnabled.getValue() ? 1 : (int)this.minBuyCount.getValue().doubleValue();
         if (this.unitPriceBuyEnabled.getValue()) {
            int fullStackSlot = this.findBestSlotByUnitPrice(handler, var13, targetItem, maxPrice, 64, true);
            return fullStackSlot != -1 ? fullStackSlot : this.findBestSlotByUnitPrice(handler, var13, targetItem, maxPrice, minCount, false);
         } else {
            int bestSlot = -1;
            long bestPrice = Long.MAX_VALUE;

            for (int i = 0; i < var13; i++) {
               ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
               if (stack.getItem() == targetItem && !stack.isEmpty() && stack.getCount() >= minCount) {
                  long price = this.extractPrice(stack);
                  if (price > 0L && price <= (long)maxPrice && price < bestPrice) {
                     bestPrice = price;
                     bestSlot = i;
                  }
               }
            }

            return bestSlot;
         }
      } else {
         return -1;
      }
   }

   private int findBestSlotByUnitPrice(
      GenericContainerScreenHandler handler, int containerSize, Item targetItem, int maxPrice, int minCount, boolean requireFullStack
   ) {
      int bestSlot = -1;
      long bestPrice = Long.MAX_VALUE;
      double bestUnitPrice = Double.MAX_VALUE;
      double maxUnitPrice = (double)maxPrice / 64.0;

      for (int i = 0; i < containerSize; i++) {
         ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
         if (stack.getItem() == targetItem && !stack.isEmpty() && stack.getCount() >= minCount && (!requireFullStack || stack.getCount() >= 64)) {
            long price = this.extractPrice(stack);
            if (price > 0L) {
               double unitPrice = (double)price / (double)stack.getCount();
               if (!(unitPrice > maxUnitPrice) && (unitPrice < bestUnitPrice || Math.abs(unitPrice - bestUnitPrice) < 1.0E-4 && price < bestPrice)) {
                  bestUnitPrice = unitPrice;
                  bestPrice = price;
                  bestSlot = i;
               }
            }
         }
      }

      return bestSlot;
   }

   private long extractPrice(ItemStack stack) {
      try {
         TooltipContext context = mc.world == null ? TooltipContext.DEFAULT : TooltipContext.create(mc.world);

         for (Text lineText : stack.getTooltip(context, mc.player, mc.options.advancedItemTooltips ? TooltipType.ADVANCED : TooltipType.BASIC)) {
            String line = lineText.getString();
            if (line.contains("Цена") || line.contains("Цeна") || line.contains("Ценa") || line.contains("Цeнa") || line.contains("Курс")) {
               String priceString = line.replaceAll("[^\\d]", "");
               if (!priceString.isEmpty()) {
                  return Long.parseLong(priceString);
               }
            }
         }
      } catch (Exception var8) {
      }

      return 0L;
   }

   private void tickRepost() {
      switch (this.repostPhase) {
         case IDLE:
            if (this.repostSucceeded) {
               this.finishRepost();
               return;
            }

            if (this.repostPending && this.repostTimer.finished((double)this.repostWaitMs)) {
               this.repostPending = false;
               this.repostSucceeded = false;
               this.repostAttempt++;
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }

               this.repostPhase = ExpCrafter.RepostPhase.OPEN_AH;
               this.repostActionDelayMs = this.randomDelay(300L, 550L);
               this.repostStepTimer.reset();
            }
            break;
         case OPEN_AH:
            if (!this.repostStepTimer.finished((double)this.repostActionDelayMs)) {
               return;
            }

            this.sendCommand("/ah");
            this.repostPhase = ExpCrafter.RepostPhase.CLICK_STORAGE;
            this.repostActionDelayMs = this.randomDelay(650L, 1000L);
            this.repostStepTimer.reset();
            break;
         case CLICK_STORAGE:
            if (!this.repostStepTimer.finished((double)this.repostActionDelayMs)) {
               return;
            }

            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
               this.repostPhase = ExpCrafter.RepostPhase.OPEN_AH;
               this.repostActionDelayMs = this.randomDelay(700L, 1000L);
               this.repostStepTimer.reset();
               return;
            }

            int slot = this.findSlotByName(handler, "Хранилище");
            if (slot == -1) {
               mc.player.closeHandledScreen();
               this.repostPhase = ExpCrafter.RepostPhase.OPEN_AH;
               this.repostActionDelayMs = this.randomDelay(700L, 1000L);
               this.repostStepTimer.reset();
               return;
            }

            this.clickSlot(handler, slot, 0, SlotActionType.PICKUP);
            this.repostPhase = ExpCrafter.RepostPhase.CLICK_REPOST;
            this.repostActionDelayMs = this.randomDelay(550L, 900L);
            this.repostStepTimer.reset();
            break;
         case CLICK_REPOST:
            if (!this.repostStepTimer.finished((double)this.repostActionDelayMs)) {
               return;
            }

            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler)) {
               this.repostPhase = ExpCrafter.RepostPhase.OPEN_AH;
               this.repostActionDelayMs = this.randomDelay(700L, 1000L);
               this.repostStepTimer.reset();
               return;
            }

            int repostSlot = this.findSlotByName(handler, "Перевыставить предметы");
            if (repostSlot == -1) {
               mc.player.closeHandledScreen();
               this.repostPhase = ExpCrafter.RepostPhase.OPEN_AH;
               this.repostActionDelayMs = this.randomDelay(700L, 1000L);
               this.repostStepTimer.reset();
               return;
            }

            this.clickSlot(handler, repostSlot, 0, SlotActionType.PICKUP);
            this.repostPhase = ExpCrafter.RepostPhase.CLOSE;
            this.repostActionDelayMs = this.randomDelay(700L, 1000L);
            this.repostStepTimer.reset();
            break;
         case CLOSE:
            if (!this.repostStepTimer.finished((double)this.repostActionDelayMs)) {
               return;
            }

            if (!this.repostSucceeded && !this.repostPending && !this.repostStepTimer.finished(4000.0)) {
               return;
            }

            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
               mc.player.closeHandledScreen();
            }

            this.repostPhase = ExpCrafter.RepostPhase.IDLE;
            if (this.repostPending) {
               this.sellPaused = true;
               return;
            }

            if (!this.repostSucceeded) {
               this.sellPaused = true;
               this.repostPending = true;
               this.repostWaitMs = this.randomDelay(1200L, 1800L);
               this.repostTimer.reset();
               this.sendMsg("Нет подтверждения перевыставления. Повторяю попытку №" + (this.repostAttempt + 1) + "...");
               return;
            }

            this.finishRepost();
      }
   }

   private void finishRepost() {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
         mc.player.closeHandledScreen();
      }

      this.repostPhase = ExpCrafter.RepostPhase.IDLE;
      this.repostPending = false;
      this.repostSucceeded = false;
      this.repostAttempt = 0;
      this.sellPaused = false;
      this.pendingSellHotbarSlot = -1;
      this.sellSelectionConfirmed = false;
      this.state = ExpCrafter.State.SELL;
      this.timer.reset();
      this.sendMsg("Перевыставлено, продолжаю продажу.");
   }

   private int findSlotByName(GenericContainerScreenHandler handler, String name) {
      int containerSize = handler.slots.size() - 36;

      for (int i = 0; i < containerSize; i++) {
         ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
         if (!stack.isEmpty() && stack.getName().getString().contains(name)) {
            return i;
         }
      }

      return -1;
   }

   private int findSlotByItem(GenericContainerScreenHandler handler, Item item) {
      int containerSize = handler.slots.size() - 36;

      for (int i = 0; i < containerSize; i++) {
         ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
         if (!stack.isEmpty() && stack.getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int findAuctionRefreshSlot(GenericContainerScreenHandler handler) {
      int refreshSlot = this.findSlotByItem(handler, Items.NETHER_STAR);
      if (refreshSlot != -1) {
         return refreshSlot;
      } else {
         int containerSize = handler.slots.size() - 36;

         for (int i = 0; i < containerSize; i++) {
            ItemStack stack = ((Slot)handler.slots.get(i)).getStack();
            if (!stack.isEmpty()) {
               String name = stack.getName().getString().toLowerCase(Locale.ROOT);
               if (name.contains("обнов") || name.contains("refresh")) {
                  return i;
               }
            }
         }

         int fallbackSlot = containerSize - 5;
         return fallbackSlot >= 0 && fallbackSlot < containerSize && !handler.getSlot(fallbackSlot).getStack().isEmpty() ? fallbackSlot : -1;
      }
   }

   private boolean forceCloseAuctionScreen() {
      if (mc.player == null) {
         return false;
      } else if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
         mc.player.closeHandledScreen();
         return true;
      } else {
         return false;
      }
   }

   private void tickClanInvest() {
      if (this.clanInvestEnabled.getValue()) {
         if (!this.balancePending) {
            if (mc.currentScreen == null && !this.rotating && this.antiAfkStage == 0) {
               if (!this.repostPending && this.repostPhase == ExpCrafter.RepostPhase.IDLE) {
                  if (this.state == ExpCrafter.State.SELL || this.state == ExpCrafter.State.SELL_VERIFY || this.state == ExpCrafter.State.DONE) {
                     long intervalMs = (long)(this.clanInvestInterval.getValue() * 1000.0);
                     if (this.clanInvestTimer.finished((double)intervalMs)) {
                        this.sendCommand("/balance");
                        this.balancePending = true;
                        this.clanInvestTimer.reset();
                     }
                  }
               }
            }
         }
      }
   }

   private void tickCheckGold() {
      boolean canCraftNow = this.canCraftExpNow();
      boolean hasGold = this.hasEnoughGoldMaterial();
      int gp = this.parsePriceOrZero(this.goldBuyPrice.getValue());
      if (canCraftNow) {
         this.state = ExpCrafter.State.OPEN_TABLE;
      } else {
         if (!hasGold && gp > 0) {
            this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
            this.state = ExpCrafter.State.BUY_GOLD;
         } else {
            this.state = ExpCrafter.State.OPEN_TABLE;
         }
      }
   }

   private void tickDoneAuto() {
      boolean hasDiamonds = this.countInInventory(Items.DIAMOND) > 0;
      boolean hasGold = this.hasEnoughGoldMaterial();
      boolean canCraftNow = this.canCraftExpNow();
      int dp = this.parsePriceOrZero(this.diamondBuyPrice.getValue());
      int gp = this.parsePriceOrZero(this.goldBuyPrice.getValue());
      if (!canCraftNow && (!hasDiamonds || !hasGold)) {
         if (!hasDiamonds && dp > 0) {
            this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
            this.state = ExpCrafter.State.BUY_DIAMONDS;
         } else if (!hasGold && gp > 0) {
            this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
            this.state = ExpCrafter.State.BUY_GOLD;
         } else if (hasDiamonds) {
            this.state = ExpCrafter.State.OPEN_TABLE;
         } else {
            this.setEnabled(false);
         }
      } else {
         this.state = ExpCrafter.State.OPEN_TABLE;
      }
   }

   private void decideNextStateAfterBuy() {
      boolean hasDiamonds = this.countInInventory(Items.DIAMOND) > 0;
      boolean hasGold = this.hasEnoughGoldMaterial();
      boolean canCraftNow = this.canCraftExpNow();
      int dp = this.parsePriceOrZero(this.diamondBuyPrice.getValue());
      int gp = this.parsePriceOrZero(this.goldBuyPrice.getValue());
      if (!canCraftNow && (!hasDiamonds || !hasGold)) {
         if (!hasDiamonds && dp > 0) {
            this.state = ExpCrafter.State.BUY_DIAMONDS;
         } else if (!hasGold && gp > 0) {
            this.state = ExpCrafter.State.BUY_GOLD;
         } else {
            this.state = hasDiamonds ? ExpCrafter.State.OPEN_TABLE : ExpCrafter.State.CHECK_GOLD;
         }
      } else {
         this.forceCloseAuctionScreen();
         this.state = ExpCrafter.State.OPEN_TABLE;
      }
   }

   private void openTable() {
      if (this.forceCloseAuctionScreen()) {
         this.timer.reset();
      } else if (this.autoAimTable.getValue()) {
         BlockHitResult visibleTable = this.getLookedAtCraftingTable();
         if (visibleTable != null) {
            this.tablePos = visibleTable.getBlockPos();
            if (this.homeTablePos == null) {
               this.homeTablePos = this.tablePos;
            }

            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, visibleTable);
            this.timer.reset();
            this.state = ExpCrafter.State.CRAFT_NUGGETS;
         } else {
            if (this.tablePos == null || mc.world.getBlockState(this.tablePos).getBlock() != Blocks.CRAFTING_TABLE) {
               this.tablePos = this.getValidHomeTable();
               if (this.tablePos == null) {
                  this.tablePos = this.findNearbyTable();
               }
            }

            if (this.tablePos == null) {
               if (this.startReturnToNearestTable()) {
                  this.sendMsg("Иду к ближайшему верстаку...");
               } else {
                  this.sendMsg("Верстак не найден в радиусе 24 блоков.");
                  this.setEnabled(false);
               }
            } else {
               this.startStrongRotateTo(this.tablePos);
               this.waitingTableRotation = true;
            }
         }
      } else {
         if (mc.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos();
            if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE) {
               if (this.homeTablePos == null) {
                  this.homeTablePos = pos;
               }

               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false));
               this.timer.reset();
               this.state = ExpCrafter.State.CRAFT_NUGGETS;
               return;
            }
         }

         this.sendMsg("Смотри на верстак и включи снова.");
         this.setEnabled(false);
      }
   }

   private void craftNuggets() {
      if (mc.player.currentScreenHandler instanceof CraftingScreenHandler handler) {
         if (this.nuggetCraftStep == 0 && this.countInInventory(Items.GOLD_INGOT) == 0) {
            if (!this.clearCraftingGridStep(handler)) {
               this.timer.reset();
            } else {
               this.resetNuggetCraftState();
               this.state = ExpCrafter.State.CRAFT_EXP;
               this.timer.reset();
            }
         } else if (this.nuggetCraftStep == 0) {
            this.nuggetSourceSlot = this.findInHandler(handler, Items.GOLD_INGOT);
            if (this.nuggetSourceSlot == -1) {
               this.state = ExpCrafter.State.CRAFT_EXP;
               this.timer.reset();
            } else {
               this.clickSlot(handler, this.nuggetSourceSlot, 0, SlotActionType.PICKUP);
               this.nuggetCraftStep = 1;
               this.timer.reset();
            }
         } else if (this.nuggetCraftStep == 1) {
            if (handler.getCursorStack().getItem() != Items.GOLD_INGOT) {
               this.resetNuggetCraftState();
               this.timer.reset();
            } else {
               this.clickSlot(handler, 1, 0, SlotActionType.PICKUP);
               this.nuggetCraftStep = 2;
               this.timer.reset();
            }
         } else if (handler.getSlot(0).getStack().isEmpty()) {
            this.nuggetResultWaitTicks++;
            if (this.nuggetResultWaitTicks > 20) {
               this.sendMsg("Результат крафта золота не появился, модуль остановлен.");
               this.setEnabled(false);
            } else {
               this.timer.reset();
            }
         } else {
            this.clickSlot(handler, 0, 0, SlotActionType.QUICK_MOVE);
            this.resetNuggetCraftState();
            this.timer.reset();
         }
      } else if (this.timer.finished(1200.0)) {
         this.resetCraftClickState();
         this.state = ExpCrafter.State.OPEN_TABLE;
         this.timer.reset();
      }
   }

   private void craftNuggetsTake() {
      this.state = ExpCrafter.State.CRAFT_EXP;
   }

   private void craftExp() {
      if (mc.player.currentScreenHandler instanceof CraftingScreenHandler handler) {
         if (this.expCraftStep == -1) {
            if (!this.clearCraftingGridStep(handler)) {
               this.timer.reset();
               return;
            }

            this.expCraftAmount = Math.min(this.countInInventory(Items.GOLD_NUGGET) / 4, this.countInInventory(Items.DIAMOND));
            this.expCraftAmount = Math.min(this.expCraftAmount, 64);
            if (this.expCraftAmount <= 0) {
               this.resetExpCraftState();
               this.state = ExpCrafter.State.CLOSE_TABLE;
               this.timer.reset();
               return;
            }

            this.expCraftStep = 0;
         }

         if (this.expCraftAmount <= 0) {
            if (!this.clearCraftingGridStep(handler)) {
               this.timer.reset();
            } else {
               this.resetExpCraftState();
               this.state = ExpCrafter.State.CLOSE_TABLE;
               this.timer.reset();
            }
         } else if (this.expCraftStep == 2) {
            if (this.countExpCraftsInGrid(handler) > 0 && !handler.getSlot(0).getStack().isEmpty()) {
               this.resetExpCraftState();
               this.state = ExpCrafter.State.CRAFT_EXP_TAKE;
               this.timer.reset();
            } else if (++this.expRecipeWaitTicks <= 12) {
               this.timer.reset();
            } else {
               this.resetExpCraftState();
               this.timer.reset();
            }
         } else {
            ExpCrafter.PlacementResult placement;
            if (this.expCraftStep == 0) {
               placement = this.spreadWholeStackAcrossSlotsStep(handler, Items.GOLD_NUGGET, new int[]{2, 4, 6, 8});
            } else {
               placement = this.placeWholeStackIntoSlotStep(handler, Items.DIAMOND, 5);
            }

            if (placement == ExpCrafter.PlacementResult.IN_PROGRESS) {
               this.timer.reset();
            } else if (placement == ExpCrafter.PlacementResult.FAILED) {
               this.resetExpCraftState();
               this.timer.reset();
            } else {
               this.expCraftStep++;
               this.timer.reset();
            }
         }
      } else {
         this.resetCraftClickState();
         this.state = ExpCrafter.State.OPEN_TABLE;
         this.timer.reset();
      }
   }

   private void craftExpTake() {
      if (mc.player.currentScreenHandler instanceof CraftingScreenHandler handler) {
         ItemStack result = handler.getSlot(0).getStack();
         if (!result.isEmpty()) {
            this.clickSlot(handler, 0, 0, SlotActionType.QUICK_MOVE);
            this.timer.reset();
         } else {
            int craftsInGrid = this.countExpCraftsInGrid(handler);
            if (craftsInGrid > 0) {
               this.state = ExpCrafter.State.CRAFT_EXP_TAKE;
               this.timer.reset();
            } else {
               int nuggetsInInv = this.countInInventory(Items.GOLD_NUGGET);
               int diamondsInInv = this.countInInventory(Items.DIAMOND);
               if (nuggetsInInv >= 4 && diamondsInInv >= 1) {
                  this.state = ExpCrafter.State.CRAFT_EXP;
                  this.timer.reset();
               } else if (!this.clearCraftingGridStep(handler)) {
                  this.timer.reset();
               } else {
                  this.resetExpCraftState();
                  this.state = this.canCraftExpNow() ? ExpCrafter.State.CRAFT_EXP : ExpCrafter.State.CLOSE_TABLE;
                  this.timer.reset();
               }
            }
         }
      } else {
         this.resetCraftClickState();
         this.state = ExpCrafter.State.OPEN_TABLE;
         this.timer.reset();
      }
   }

   private void closeTable() {
      if (mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
         mc.player.closeHandledScreen();
      }

      this.resetCraftClickState();
      this.state = ExpCrafter.State.SELL;
      this.timer.reset();
   }

   private void sellExpBottles() {
      if (!this.sellPaused) {
         int total = this.countInInventory(Items.EXPERIENCE_BOTTLE);
         if (total == 0) {
            this.sendMsg("Продажа завершена.");
            this.state = ExpCrafter.State.DONE;
         } else if (total < 64) {
            this.state = ExpCrafter.State.DONE;
         } else {
            int hotbarSlot = -1;

            for (int i = 0; i < 9; i++) {
               ItemStack s = mc.player.getInventory().getStack(i);
               if (s.getItem() == Items.EXPERIENCE_BOTTLE && s.getCount() >= 64) {
                  hotbarSlot = i;
                  break;
               }
            }

            if (hotbarSlot == -1) {
               for (int ix = 9; ix < 36; ix++) {
                  ItemStack s = mc.player.getInventory().getStack(ix);
                  if (s.getItem() == Items.EXPERIENCE_BOTTLE && s.getCount() >= 64) {
                     int freeHotbar = this.findFreeHotbarSlot();
                     if (freeHotbar == -1) {
                        freeHotbar = 0;
                     }

                     mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, ix, freeHotbar, SlotActionType.SWAP, mc.player);
                     this.pendingSellHotbarSlot = freeHotbar;
                     this.sellSelectionConfirmed = false;
                     this.state = ExpCrafter.State.SELL_VERIFY;
                     this.timer.reset();
                     this.timer.setMs(0L);
                     return;
                  }
               }

               this.state = ExpCrafter.State.DONE;
            } else {
               InventoryTask.switchTo(hotbarSlot);
               this.pendingSellHotbarSlot = hotbarSlot;
               this.sellSelectionConfirmed = true;
               this.state = ExpCrafter.State.SELL_VERIFY;
               this.timer.reset();
               this.timer.setMs(0L);
            }
         }
      }
   }

   private void verifyAndSellExpBottle() {
      if (!this.sellPaused && mc.player != null) {
         if (this.timer.finished(120.0)) {
            int selectedSlot = this.pendingSellHotbarSlot;
            if (selectedSlot < 0 || selectedSlot > 8) {
               this.sellSelectionConfirmed = false;
               this.state = ExpCrafter.State.SELL;
               this.timer.reset();
            } else if (!this.sellSelectionConfirmed) {
               InventoryTask.switchTo(selectedSlot);
               this.sellSelectionConfirmed = true;
               this.timer.reset();
            } else {
               ItemStack handStack = mc.player.getInventory().getStack(selectedSlot);
               if (handStack.getItem() == Items.EXPERIENCE_BOTTLE && handStack.getCount() >= 64) {
                  this.sendCommand("/ah sell " + this.sellPrice.getValue());
                  this.pendingSellHotbarSlot = -1;
                  this.sellSelectionConfirmed = false;
                  this.state = ExpCrafter.State.SELL;
                  this.timer.setMs(50L - (long)this.sellDelay.getValue().doubleValue());
               } else {
                  this.pendingSellHotbarSlot = -1;
                  this.sellSelectionConfirmed = false;
                  this.state = ExpCrafter.State.SELL;
                  this.timer.reset();
               }
            }
         }
      }
   }

   private void clickSlot(ScreenHandler handler, int slotId, int button, SlotActionType type) {
      if (mc.interactionManager != null && mc.player != null) {
         mc.interactionManager.clickSlot(handler.syncId, slotId, button, type, mc.player);
      }
   }

   private boolean clearCraftingGridStep(CraftingScreenHandler handler) {
      if (handler.getCursorStack().isEmpty()) {
         for (int i = 1; i <= 9; i++) {
            if (!handler.getSlot(i).getStack().isEmpty()) {
               this.clickSlot(handler, i, 0, SlotActionType.QUICK_MOVE);
               return false;
            }
         }

         return true;
      } else {
         for (int ix = 10; ix < handler.slots.size(); ix++) {
            if (handler.getSlot(ix).getStack().isEmpty()) {
               this.clickSlot(handler, ix, 0, SlotActionType.PICKUP);
               break;
            }
         }

         return false;
      }
   }

   private int findInHandler(CraftingScreenHandler handler, Item item) {
      for (int i = 10; i < handler.slots.size(); i++) {
         if (handler.getSlot(i).getStack().getItem() == item) {
            return i;
         }
      }

      return -1;
   }

   private int findLargestInHandler(CraftingScreenHandler handler, Item item) {
      int bestSlot = -1;
      int bestCount = 0;

      for (int i = 10; i < handler.slots.size(); i++) {
         ItemStack stack = handler.getSlot(i).getStack();
         if (stack.getItem() == item && stack.getCount() > bestCount) {
            bestSlot = i;
            bestCount = stack.getCount();
         }
      }

      return bestSlot;
   }

   private ExpCrafter.PlacementResult spreadWholeStackAcrossSlotsStep(CraftingScreenHandler handler, Item item, int[] targetSlots) {
      int stateSlot = targetSlots[0];
      if (this.placementTargetSlot != stateSlot || this.placementItem != item) {
         this.resetPlacementState();
         this.placementTargetSlot = stateSlot;
         this.placementItem = item;
      }

      if (this.placementSourceSlot == -1) {
         this.placementSourceSlot = this.findLargestInHandler(handler, item);
         if (this.placementSourceSlot == -1) {
            this.resetPlacementState();
            return ExpCrafter.PlacementResult.FAILED;
         } else {
            this.clickSlot(handler, this.placementSourceSlot, 0, SlotActionType.PICKUP);
            return ExpCrafter.PlacementResult.IN_PROGRESS;
         }
      } else {
         ItemStack cursor = handler.getCursorStack();
         if (this.quickCraftStage > 1 || !cursor.isEmpty() && cursor.getItem() == item) {
            if (this.quickCraftStage == 0) {
               this.clickSlot(handler, -999, ScreenHandler.packQuickCraftData(0, 0), SlotActionType.QUICK_CRAFT);
               this.quickCraftStage = 1;
               return ExpCrafter.PlacementResult.IN_PROGRESS;
            } else if (this.quickCraftStage == 1 && this.quickCraftSlotIndex < targetSlots.length) {
               this.clickSlot(handler, targetSlots[this.quickCraftSlotIndex], ScreenHandler.packQuickCraftData(1, 0), SlotActionType.QUICK_CRAFT);
               this.quickCraftSlotIndex++;
               return ExpCrafter.PlacementResult.IN_PROGRESS;
            } else if (this.quickCraftStage == 1) {
               this.clickSlot(handler, -999, ScreenHandler.packQuickCraftData(2, 0), SlotActionType.QUICK_CRAFT);
               this.quickCraftStage = 2;
               return ExpCrafter.PlacementResult.IN_PROGRESS;
            } else if (this.quickCraftStage != 2 || handler.getCursorStack().isEmpty()) {
               this.quickCraftStage = 3;

               for (int targetSlot : targetSlots) {
                  ItemStack stack = handler.getSlot(targetSlot).getStack();
                  if (stack.getItem() != item || stack.isEmpty()) {
                     this.resetPlacementState();
                     return ExpCrafter.PlacementResult.FAILED;
                  }
               }

               this.resetPlacementState();
               return ExpCrafter.PlacementResult.DONE;
            } else if (handler.getCursorStack().getItem() != item) {
               this.resetPlacementState();
               return ExpCrafter.PlacementResult.FAILED;
            } else {
               this.clickSlot(handler, this.placementSourceSlot, 0, SlotActionType.PICKUP);
               this.quickCraftStage = 3;
               return ExpCrafter.PlacementResult.IN_PROGRESS;
            }
         } else {
            if (this.quickCraftStage == 1) {
               this.clickSlot(handler, -999, ScreenHandler.packQuickCraftData(2, 0), SlotActionType.QUICK_CRAFT);
            }

            this.resetPlacementState();
            return ExpCrafter.PlacementResult.FAILED;
         }
      }
   }

   private ExpCrafter.PlacementResult placeWholeStackIntoSlotStep(CraftingScreenHandler handler, Item item, int targetSlot) {
      if (this.placementTargetSlot != targetSlot || this.placementItem != item) {
         this.resetPlacementState();
         this.placementTargetSlot = targetSlot;
         this.placementItem = item;
      }

      if (this.placementSourceSlot == -1) {
         this.placementSourceSlot = this.findInHandler(handler, item);
         if (this.placementSourceSlot == -1) {
            this.resetPlacementState();
            return ExpCrafter.PlacementResult.FAILED;
         } else {
            this.clickSlot(handler, this.placementSourceSlot, 0, SlotActionType.PICKUP);
            return ExpCrafter.PlacementResult.IN_PROGRESS;
         }
      } else {
         ItemStack cursor = handler.getCursorStack();
         if (!cursor.isEmpty() && cursor.getItem() == item) {
            this.clickSlot(handler, targetSlot, 0, SlotActionType.PICKUP);
            this.resetPlacementState();
            return ExpCrafter.PlacementResult.DONE;
         } else {
            this.resetPlacementState();
            return ExpCrafter.PlacementResult.FAILED;
         }
      }
   }

   private void resetPlacementState() {
      this.placementTargetSlot = -1;
      this.placementSourceSlot = -1;
      this.placementItem = null;
      this.quickCraftStage = 0;
      this.quickCraftSlotIndex = 0;
   }

   private void resetNuggetCraftState() {
      this.nuggetCraftStep = 0;
      this.nuggetSourceSlot = -1;
      this.nuggetResultWaitTicks = 0;
   }

   private void resetExpCraftState() {
      this.expCraftStep = -1;
      this.expCraftAmount = 0;
      this.expRecipeWaitTicks = 0;
      this.resetPlacementState();
   }

   private void resetCraftClickState() {
      this.resetNuggetCraftState();
      this.resetExpCraftState();
   }

   private int countExpCraftsInGrid(CraftingScreenHandler handler) {
      ItemStack top = handler.getSlot(2).getStack();
      ItemStack left = handler.getSlot(4).getStack();
      ItemStack center = handler.getSlot(5).getStack();
      ItemStack right = handler.getSlot(6).getStack();
      ItemStack bottom = handler.getSlot(8).getStack();
      return top.getItem() == Items.GOLD_NUGGET
            && left.getItem() == Items.GOLD_NUGGET
            && right.getItem() == Items.GOLD_NUGGET
            && bottom.getItem() == Items.GOLD_NUGGET
            && center.getItem() == Items.DIAMOND
         ? Math.min(Math.min(Math.min(top.getCount(), left.getCount()), Math.min(right.getCount(), bottom.getCount())), center.getCount())
         : 0;
   }

   private int countInInventory(Item item) {
      if (mc.player == null) {
         return 0;
      } else {
         int count = 0;

         for (int i = 0; i < 36; i++) {
            ItemStack s = mc.player.getInventory().getStack(i);
            if (s.getItem() == item) {
               count += s.getCount();
            }
         }

         return count;
      }
   }

   private boolean hasEnoughGoldMaterial() {
      return this.countInInventory(Items.GOLD_INGOT) > 0 || this.countInInventory(Items.GOLD_NUGGET) >= 4;
   }

   private boolean canCraftExpNow() {
      return this.countInInventory(Items.DIAMOND) > 0 && this.countInInventory(Items.GOLD_NUGGET) >= 4;
   }

   private boolean hasRequiredItemForState(Item item) {
      if (item == Items.DIAMOND) {
         return this.countInInventory(Items.DIAMOND) > 0;
      } else {
         return item == Items.GOLD_INGOT ? this.hasEnoughGoldMaterial() : this.countInInventory(item) > 0;
      }
   }

   private int findFreeHotbarSlot() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return i;
         }
      }

      return -1;
   }

   private void resetAntiAfkMovement() {
      this.antiAfkStage = 0;
      this.antiAfkTimer.reset();
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
      }
   }

   private long getCraftDelay() {
      return (long)this.craftSpeed.getValue().doubleValue();
   }

   private long getRandomRefreshDelay() {
      return 180L + (long)(Math.random() * 181.0);
   }

   private long getRandomBuyDelayAfterRefresh() {
      return 200L + (long)(Math.random() * 221.0);
   }

   private long randomDelay(long minimum, long maximum) {
      return maximum <= minimum ? minimum : minimum + (long)(Math.random() * (double)(maximum - minimum + 1L));
   }

   private long getAuctionBuyClickDelay() {
      long configured = Math.max(50L, Math.min(120L, (long)this.buyDelay.getValue().doubleValue()));
      long minimum = Math.max(40L, configured / 2L);
      return minimum + (long)(Math.random() * (double)(configured - minimum + 1L));
   }

   private long getAuctionCloseDelay() {
      return Math.max(450L, (long)(this.buyDelay.getValue() * 0.85));
   }

   private long getAuctionPostBuyResolveDelay() {
      return 1200L;
   }

   private long getAntiAfkDelay() {
      return (long)this.antiAfkDelay.getValue().doubleValue() * 1000L;
   }

   private int parsePriceOrZero(String s) {
      if (s != null && !s.isBlank()) {
         try {
            return Integer.parseInt(s.trim().replace(",", "").replace(".", ""));
         } catch (NumberFormatException var3) {
            return 0;
         }
      } else {
         return 0;
      }
   }

   private void sendCommand(String cmd) {
      if (mc.player != null && mc.player.networkHandler != null) {
         String command = cmd.startsWith("/") ? cmd.substring(1) : cmd;
         mc.player.networkHandler.sendChatCommand(command);
      }
   }

   private boolean tickAntiAfk() {
      if (mc.options == null || mc.player == null || mc.world == null) {
         return false;
      } else if (!this.antiAfkEnabled.getValue()) {
         this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.NONE;
         this.antiAfkTrackedWorld = mc.world;
         this.antiAfkTrackedPlayer = mc.player;
         this.resetAntiAfkMovement();
         return false;
      } else {
         Vec3d currentPosition = this.getPlayerPosition();
         boolean sessionChanged = this.updateAntiAfkSessionTracking();
         if (this.antiAfkRecoveryPhase != ExpCrafter.AntiAfkRecoveryPhase.NONE) {
            return this.tickAntiAfkRecovery(currentPosition, sessionChanged);
         } else if (sessionChanged) {
            this.startAntiAfkRecovery(currentPosition);
            return true;
         } else {
            if (!this.antiAfkPositionInitialized) {
               this.antiAfkPreviousPosition = currentPosition;
               this.antiAfkPositionInitialized = true;
            } else if (currentPosition.squaredDistanceTo(this.antiAfkPreviousPosition) > 25.0) {
               this.startAntiAfkRecovery(currentPosition);
               return true;
            }

            this.antiAfkPreviousPosition = currentPosition;
            if (mc.currentScreen != null) {
               if (this.antiAfkStage != 0) {
                  this.cancelAntiAfkRoute();
               }

               return false;
            } else {
               BlockPos validHome = this.getValidHomeTable();
               if (this.antiAfkStage == 0 && validHome != null && this.horizontalDistanceSquared(mc.player.getBlockPos(), validHome) > 16) {
                  long now = System.currentTimeMillis();
                  if (now >= this.antiAfkHomeRetryAt) {
                     this.antiAfkHomeRetryAt = now + 1000L;
                     if (this.startReturnToTable(validHome)) {
                        this.sendMsg("Возвращаюсь к домашнему верстаку...");
                     }
                  }

                  return true;
               } else {
                  if (this.antiAfkStage == 0) {
                     if (!this.antiAfkTimer.finished((double)this.antiAfkIntervalMs)) {
                        return false;
                     }

                     if (this.rotating || !this.isAntiAfkSafeState()) {
                        return false;
                     }

                     if (!this.startAntiAfkRoute()) {
                        this.antiAfkIntervalMs = this.getAntiAfkDelay();
                        this.antiAfkTimer.reset();
                        return false;
                     }
                  }

                  if (this.antiAfkPathIndex < this.antiAfkPath.size()) {
                     if (this.antiAfkPauseTicks > 0) {
                        this.antiAfkPauseTicks--;
                        this.setMovement(false, false);
                        return true;
                     } else {
                        BlockPos waypoint = this.antiAfkPath.get(this.antiAfkPathIndex);
                        if (this.antiAfkOffsetPathIndex != this.antiAfkPathIndex) {
                           this.antiAfkOffsetPathIndex = this.antiAfkPathIndex;
                           this.antiAfkWaypointOffsetX = (Math.random() - 0.5) * 0.24;
                           this.antiAfkWaypointOffsetZ = (Math.random() - 0.5) * 0.24;
                        }

                        double dx = (double)waypoint.getX() + 0.5 + this.antiAfkWaypointOffsetX - mc.player.getX();
                        double dz = (double)waypoint.getZ() + 0.5 + this.antiAfkWaypointOffsetZ - mc.player.getZ();
                        double distance = Math.sqrt(dx * dx + dz * dz);
                        if (distance < 0.28) {
                           this.antiAfkPathIndex++;
                           this.antiAfkStuckTicks = 0;
                           this.antiAfkLastPosition = currentPosition;
                           this.antiAfkTurnPathIndex = -1;
                           this.antiAfkOffsetPathIndex = -1;
                           this.antiAfkPauseTicks = 2 + (int)(Math.random() * 4.0);
                           this.setMovement(false, false);
                           return true;
                        } else {
                           float movementYaw = (float)Math.toDegrees(Math.atan2(-dx, dz));
                           float desiredYaw = this.antiAfkStage == 1 ? movementYaw + 180.0F : movementYaw;
                           if (this.antiAfkTurnPathIndex != this.antiAfkPathIndex) {
                              this.startAntiAfkTurn(desiredYaw);
                           }

                           if (this.tickAntiAfkTurn()) {
                              this.setMovement(false, false);
                              return true;
                           } else {
                              if (this.antiAfkStage == 1) {
                                 this.setMovement(false, true);
                              } else {
                                 this.setMovement(true, false);
                              }

                              if (currentPosition.squaredDistanceTo(this.antiAfkLastPosition) < 4.0E-4) {
                                 this.antiAfkStuckTicks++;
                              } else {
                                 this.antiAfkStuckTicks = 0;
                                 this.antiAfkLastPosition = currentPosition;
                              }

                              if (this.antiAfkStuckTicks > 30) {
                                 if (this.antiAfkRepathAttempts++ < 2) {
                                    BlockPos target = this.antiAfkStage == 1 ? this.antiAfkBackTarget : this.antiAfkTableTarget;
                                    if (target != null && this.setAntiAfkPath(target, this.antiAfkStage == 2)) {
                                       this.antiAfkStuckTicks = 0;
                                       return true;
                                    }
                                 }

                                 this.finishAntiAfkRoute();
                              }

                              return this.antiAfkStage != 0;
                           }
                        }
                     }
                  } else {
                     if (this.antiAfkStage == 1) {
                        this.antiAfkStage = 2;
                        this.antiAfkRepathAttempts = 0;
                        if (this.antiAfkTableTarget == null || !this.setAntiAfkPathToTable(this.antiAfkTableTarget)) {
                           this.finishAntiAfkRoute();
                        }
                     } else {
                        this.finishAntiAfkRoute();
                     }

                     return this.antiAfkStage != 0;
                  }
               }
            }
         }
      }
   }

   private void startAntiAfkRecovery(Vec3d currentPosition) {
      if (this.antiAfkRecoveryPhase != ExpCrafter.AntiAfkRecoveryPhase.WAIT_REJOIN) {
         this.cancelAntiAfkRoute();
         this.rotating = false;
         this.waitingTableRotation = false;
         this.tablePos = this.homeTablePos;
         this.chestPos = null;
         this.chestPhase = ExpCrafter.ChestPhase.IDLE;
         this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
         this.buyTargetSlot = -1;
         this.pendingSellHotbarSlot = -1;
         this.sellSelectionConfirmed = false;
         if (this.state == ExpCrafter.State.SELL_VERIFY) {
            this.state = ExpCrafter.State.SELL;
         }

         if (this.repostPhase != ExpCrafter.RepostPhase.IDLE) {
            this.repostPhase = ExpCrafter.RepostPhase.IDLE;
            this.repostPending = true;
            this.repostWaitMs = this.randomDelay(800L, 1000L);
            this.repostTimer.reset();
            this.sellPaused = true;
         }

         if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler || mc.player.currentScreenHandler instanceof CraftingScreenHandler) {
            mc.player.closeHandledScreen();
         }

         this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.WAIT_REJOIN;
         this.antiAfkRecoveryTimer.reset();
         this.antiAfkPreviousPosition = currentPosition;
         this.antiAfkPositionInitialized = true;
         this.antiAfkJoinDetected = false;
         this.sendMsg("Обнаружен перенос в hub. Вход на анархию через 5 сек.");
      }
   }

   private void startPostJoinStabilization() {
      if (this.antiAfkEnabled.getValue() && mc.player != null && mc.world != null) {
         this.cancelAntiAfkRoute();
         this.rotating = false;
         this.waitingTableRotation = false;
         this.forceCloseAuctionScreen();
         this.buyPhase = ExpCrafter.BuyPhase.OPEN_AH;
         this.buyTargetSlot = -1;
         this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.WAIT_STABILIZE;
         this.antiAfkRecoveryTimer.reset();
         this.antiAfkPreviousPosition = this.getPlayerPosition();
         this.antiAfkPositionInitialized = true;
         this.antiAfkJoinDetected = true;
         this.antiAfkTrackedWorld = mc.world;
         this.antiAfkTrackedPlayer = mc.player;
         this.sendMsg("Сервер ещё загружает режим. Жду 15 сек.");
      }
   }

   private boolean isHubChatMessage(String msg) {
      return msg.contains("перемещены в хаб")
         || msg.contains("перемещены в hub")
         || msg.contains("отправлены в лобби")
         || msg.contains("возвращены в лобби")
         || msg.contains("вы были кикнуты");
   }

   private boolean updateAntiAfkSessionTracking() {
      if (this.antiAfkTrackedWorld != null && this.antiAfkTrackedPlayer != null) {
         boolean changed = this.antiAfkTrackedWorld != mc.world || this.antiAfkTrackedPlayer != mc.player;
         this.antiAfkTrackedWorld = mc.world;
         this.antiAfkTrackedPlayer = mc.player;
         return changed;
      } else {
         this.antiAfkTrackedWorld = mc.world;
         this.antiAfkTrackedPlayer = mc.player;
         return false;
      }
   }

   private boolean tickAntiAfkRecovery(Vec3d currentPosition, boolean sessionChanged) {
      this.setMovement(false, false);
      if (this.antiAfkRecoveryPhase == ExpCrafter.AntiAfkRecoveryPhase.WAIT_REJOIN) {
         if (!this.antiAfkRecoveryTimer.finished(5000.0)) {
            return true;
         } else {
            String number = this.anarchyNumber.getValue() == null ? "" : this.anarchyNumber.getValue().replaceAll("[^0-9]", "");
            if (number.isBlank()) {
               number = "903";
            }

            this.sendCommand("/an" + number);
            this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.WAIT_TELEPORT;
            this.antiAfkRecoveryTimer.reset();
            this.antiAfkPreviousPosition = currentPosition;
            this.antiAfkJoinDetected = false;
            this.antiAfkLastTableProbeAt = 0L;
            this.sendMsg("Вхожу на анархию " + number + "...");
            return true;
         }
      } else if (this.antiAfkRecoveryPhase == ExpCrafter.AntiAfkRecoveryPhase.WAIT_TELEPORT) {
         long now = System.currentTimeMillis();
         boolean tableNearby = false;
         if (this.antiAfkRecoveryTimer.finished(2000.0) && now - this.antiAfkLastTableProbeAt >= 750L) {
            this.antiAfkLastTableProbeAt = now;
            tableNearby = this.findNearestTable() != null;
         }

         boolean joined = sessionChanged || currentPosition.squaredDistanceTo(this.antiAfkPreviousPosition) > 25.0 || tableNearby;
         if (!joined) {
            if (!this.antiAfkRecoveryTimer.finished(10000.0)) {
               return true;
            } else {
               this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.WAIT_REJOIN;
               this.antiAfkRecoveryTimer.reset();
               this.sendMsg("Вход не подтверждён. Повторяю через 5 сек...");
               return true;
            }
         } else {
            this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.WAIT_STABILIZE;
            this.antiAfkRecoveryTimer.reset();
            this.antiAfkPreviousPosition = currentPosition;
            this.antiAfkJoinDetected = true;
            this.sendMsg("Анархия загружена. Жду 15 сек. перед запуском...");
            return true;
         }
      } else {
         boolean movedAbruptly = currentPosition.squaredDistanceTo(this.antiAfkPreviousPosition) > 25.0;
         if (sessionChanged || movedAbruptly) {
            if (this.antiAfkJoinDetected) {
               this.startAntiAfkRecovery(currentPosition);
               return true;
            }

            this.antiAfkJoinDetected = true;
            this.antiAfkPreviousPosition = currentPosition;
            this.antiAfkRecoveryTimer.reset();
         }

         if (!this.antiAfkRecoveryTimer.finished(15000.0)) {
            return true;
         } else {
            this.cancelAntiAfkRoute();
            this.antiAfkRecoveryPhase = ExpCrafter.AntiAfkRecoveryPhase.NONE;
            this.antiAfkJoinDetected = false;
            this.antiAfkIntervalMs = this.getAntiAfkDelay();
            this.antiAfkPositionInitialized = false;
            this.antiAfkTimer.reset();
            this.timer.reset();
            this.sendMsg("Задержка завершена, ExpCrafter и анти-AFK запущены.");
            return true;
         }
      }
   }

   private boolean startAntiAfkRoute() {
      if (!mc.player.isOnGround()) {
         return false;
      } else {
         BlockPos start = mc.player.getBlockPos();
         this.antiAfkTableTarget = this.getValidHomeTable();
         if (this.antiAfkTableTarget == null) {
            this.antiAfkTableTarget = this.findNearestTable();
         }

         if (this.antiAfkTableTarget == null) {
            return false;
         } else {
            if (this.homeTablePos == null) {
               this.homeTablePos = this.antiAfkTableTarget;
            }

            float yaw = mc.player.getYaw();
            int backX = (int)Math.round(Math.sin(Math.toRadians((double)yaw)));
            int backZ = (int)Math.round(-Math.cos(Math.toRadians((double)yaw)));
            if (backX == 0 && backZ == 0) {
               backZ = -1;
            }

            this.antiAfkBackTarget = start.add(backX * 2, 0, backZ * 2);
            this.antiAfkRepathAttempts = 0;
            if (!this.setAntiAfkPath(this.antiAfkBackTarget, false)) {
               return false;
            } else {
               this.antiAfkStage = 1;
               this.antiAfkLastPosition = this.getPlayerPosition();
               this.antiAfkPreviousPosition = this.antiAfkLastPosition;
               this.antiAfkStuckTicks = 0;
               return true;
            }
         }
      }
   }

   private boolean startReturnToNearestTable() {
      if (!mc.player.isOnGround()) {
         return false;
      } else {
         BlockPos nearest = this.getValidHomeTable();
         if (nearest == null) {
            nearest = this.findNearestTable();
         }

         if (nearest == null) {
            return false;
         } else {
            if (this.homeTablePos == null) {
               this.homeTablePos = nearest;
            }

            return this.startReturnToTable(nearest);
         }
      }
   }

   private boolean startReturnToTable(BlockPos target) {
      if (mc.player.isOnGround() && target != null && this.setAntiAfkPathToTable(target)) {
         this.tablePos = target;
         this.antiAfkTableTarget = target;
         this.antiAfkStage = 2;
         this.antiAfkRepathAttempts = 0;
         this.antiAfkStuckTicks = 0;
         this.antiAfkLastPosition = this.getPlayerPosition();
         this.antiAfkPreviousPosition = this.antiAfkLastPosition;
         this.antiAfkTurnPathIndex = -1;
         return true;
      } else {
         return false;
      }
   }

   private BlockPos getValidHomeTable() {
      if (this.homeTablePos != null && mc.world != null) {
         return mc.world.getBlockState(this.homeTablePos).getBlock() == Blocks.CRAFTING_TABLE ? this.homeTablePos : null;
      } else {
         return null;
      }
   }

   private boolean isAntiAfkSafeState() {
      return this.state == ExpCrafter.State.BUY_DIAMONDS
         || this.state == ExpCrafter.State.CHECK_GOLD
         || this.state == ExpCrafter.State.BUY_GOLD
         || this.state == ExpCrafter.State.SELL
         || this.state == ExpCrafter.State.SELL_VERIFY
         || this.state == ExpCrafter.State.DONE;
   }

   private boolean setAntiAfkPathToTable(BlockPos table) {
      return this.setAntiAfkPath(table, true);
   }

   private boolean setAntiAfkPath(BlockPos target, boolean tableTarget) {
      BlockPos start = mc.player.getBlockPos();
      List<BlockPos> path = this.findPath(start, target, tableTarget);
      if (path.isEmpty() && !start.equals(target)) {
         return false;
      } else {
         this.antiAfkPath.clear();
         this.antiAfkPath.addAll(path);
         this.antiAfkPathIndex = 0;
         return true;
      }
   }

   private List<BlockPos> findPath(BlockPos start, BlockPos target, boolean tableTarget) {
      if (start.equals(target)) {
         return new ArrayList<>();
      } else {
         ArrayDeque<BlockPos> queue = new ArrayDeque<>();
         Map<BlockPos, BlockPos> previous = new HashMap<>();
         queue.add(start);
         previous.put(start, null);
         BlockPos best = null;
         int bestDistance = Integer.MAX_VALUE;
         int explored = 0;

         while (!queue.isEmpty() && explored++ < 2500) {
            BlockPos current = queue.removeFirst();
            if (!tableTarget && current.equals(target) || tableTarget && this.isAdjacentToTable(current, target)) {
               best = current;
               break;
            }

            int targetDistance = this.horizontalDistanceSquared(current, target);
            if (!tableTarget && targetDistance < bestDistance) {
               bestDistance = targetDistance;
               best = current;
            }

            for (Direction direction : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
               BlockPos next = current.offset(direction);
               if (!previous.containsKey(next) && this.isSafeStandPosition(next)) {
                  previous.put(next, current);
                  queue.addLast(next);
               }
            }
         }

         if (best != null && !best.equals(start)) {
            ArrayList<BlockPos> path = new ArrayList<>();

            for (BlockPos currentx = best; currentx != null && !currentx.equals(start); currentx = previous.get(currentx)) {
               path.add(currentx);
            }

            Collections.reverse(path);
            return path;
         } else {
            return Collections.emptyList();
         }
      }
   }

   private boolean isAdjacentToTable(BlockPos stand, BlockPos table) {
      return Math.abs(stand.getX() - table.getX()) + Math.abs(stand.getZ() - table.getZ()) == 1
         && stand.getY() == table.getY()
         && this.isSafeStandPosition(stand);
   }

   private boolean isSafeStandPosition(BlockPos pos) {
      return mc.world.getBlockState(pos).getFluidState().isEmpty() && mc.world.getBlockState(pos.up()).getFluidState().isEmpty()
         ? mc.world.getBlockState(pos).getCollisionShape(mc.world, pos).isEmpty()
            && mc.world.getBlockState(pos.up()).getCollisionShape(mc.world, pos.up()).isEmpty()
            && !mc.world.getBlockState(pos.down()).getCollisionShape(mc.world, pos.down()).isEmpty()
         : false;
   }

   private BlockPos findNearestTable() {
      if (mc.player != null && mc.world != null) {
         BlockPos origin = mc.player.getBlockPos();
         BlockPos nearest = null;
         int nearestDistance = Integer.MAX_VALUE;

         for (int dx = -24; dx <= 24; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
               for (int dz = -24; dz <= 24; dz++) {
                  BlockPos pos = origin.add(dx, dy, dz);
                  if (mc.world.getBlockState(pos).getBlock() == Blocks.CRAFTING_TABLE) {
                     int distance = dx * dx + dy * dy + dz * dz;
                     if (distance < nearestDistance) {
                        nearestDistance = distance;
                        nearest = pos;
                     }
                  }
               }
            }
         }

         return nearest;
      } else {
         return null;
      }
   }

   private int horizontalDistanceSquared(BlockPos first, BlockPos second) {
      int dx = first.getX() - second.getX();
      int dz = first.getZ() - second.getZ();
      return dx * dx + dz * dz;
   }

   private Vec3d getPlayerPosition() {
      return new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ());
   }

   private void startAntiAfkTurn(float targetYaw) {
      this.antiAfkTurnPathIndex = this.antiAfkPathIndex;
      this.antiAfkTurnStartYaw = mc.player.getYaw();
      this.antiAfkTurnTargetYaw = this.normalizeYawAround(targetYaw, this.antiAfkTurnStartYaw);
      this.antiAfkTurnStartTime = System.currentTimeMillis();
      float difference = Math.abs(this.antiAfkTurnTargetYaw - this.antiAfkTurnStartYaw);
      this.antiAfkTurnDuration = Math.max(160L, Math.min(650L, 160L + (long)(difference * 2.8F) + (long)(Math.random() * 90.0)));
   }

   private boolean tickAntiAfkTurn() {
      long elapsed = System.currentTimeMillis() - this.antiAfkTurnStartTime;
      float progress = Math.min(1.0F, (float)elapsed / (float)this.antiAfkTurnDuration);
      float smooth = progress < 0.5F ? 2.0F * progress * progress : 1.0F - (float)Math.pow((double)(-2.0F * progress + 2.0F), 2.0) / 2.0F;
      this.applyAim(this.antiAfkTurnStartYaw + (this.antiAfkTurnTargetYaw - this.antiAfkTurnStartYaw) * smooth, mc.player.getPitch());
      return progress < 1.0F;
   }

   private float normalizeYawAround(float yaw, float reference) {
      float difference = yaw - reference;

      while (difference > 180.0F) {
         difference -= 360.0F;
      }

      while (difference < -180.0F) {
         difference += 360.0F;
      }

      return reference + difference;
   }

   private void setMovement(boolean forward, boolean back) {
      mc.options.forwardKey.setPressed(forward);
      mc.options.backKey.setPressed(back);
      mc.options.leftKey.setPressed(false);
      mc.options.rightKey.setPressed(false);
      mc.options.sprintKey.setPressed(false);
      mc.options.jumpKey.setPressed(false);
      mc.player.setSprinting(false);
   }

   private void cancelAntiAfkRoute() {
      this.resetAntiAfkMovement();
      this.antiAfkPath.clear();
      this.antiAfkPathIndex = 0;
      this.antiAfkStage = 0;
      this.antiAfkPauseTicks = 0;
      this.antiAfkTurnPathIndex = -1;
      this.antiAfkOffsetPathIndex = -1;
      this.antiAfkBackTarget = null;
      this.antiAfkTableTarget = null;
   }

   private void finishAntiAfkRoute() {
      this.cancelAntiAfkRoute();
      this.antiAfkIntervalMs = this.getAntiAfkDelay();
      this.antiAfkTimer.reset();
   }

   private void sendMsg(String msg) {
      mc.execute(() -> Notifications.push("ExpCrafter", msg, 3500L));
   }

   private static enum AntiAfkRecoveryPhase {
      NONE,
      WAIT_REJOIN,
      WAIT_TELEPORT,
      WAIT_STABILIZE;

      private AntiAfkRecoveryPhase() {
      }
   }

   private static enum BuyPhase {
      OPEN_AH,
      WAIT_AH,
      SEARCH,
      REFRESH,
      WAIT_REFRESH,
      BUY_SLOT,
      WAIT_CLOSE;

      private BuyPhase() {
      }
   }

   private static enum ChestPhase {
      IDLE,
      ROTATE_TO_CHEST,
      OPEN_CHEST,
      TAKE_ITEMS,
      CLOSE_CHEST,
      ROTATE_TO_TABLE,
      ROTATE_TO_CHEST_PUT,
      OPEN_CHEST_PUT,
      PUT_EXP,
      CLOSE_CHEST_PUT;

      private ChestPhase() {
      }
   }

   private static enum PlacementResult {
      IN_PROGRESS,
      DONE,
      FAILED;

      private PlacementResult() {
      }
   }

   private static enum RepostPhase {
      IDLE,
      OPEN_AH,
      CLICK_STORAGE,
      CLICK_REPOST,
      CLOSE;

      private RepostPhase() {
      }
   }

   private static enum State {
      BUY_DIAMONDS,
      CHECK_GOLD,
      BUY_GOLD,
      OPEN_TABLE,
      CRAFT_NUGGETS,
      CRAFT_NUGGETS_TAKE,
      CRAFT_EXP,
      CRAFT_EXP_TAKE,
      CLOSE_TABLE,
      SELL,
      SELL_VERIFY,
      DONE,
      CHEST_TAKE_ITEMS,
      CHEST_PUT_EXP;

      private State() {
      }
   }
}
