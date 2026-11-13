package daamky.ua.api.module.impl.misc;

import java.awt.Color;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.ItemCooldownManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import daamky.ua.api.drag.impl.HudRenderCompat;
import daamky.ua.api.drag.impl.Notifications;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.util.StopWatch;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.mixin.accessor.ItemCooldownInstanceAccessor;
import daamky.ua.mixin.accessor.ItemCooldownsAccessor;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.interaction.PlayerInteractionHelper;
import daamky.ua.utils.inventory.lookup.InventoryUtils;
import daamky.ua.utils.inventory.movement.MovementController;
import daamky.ua.utils.inventory.script.Script;
import daamky.ua.utils.math.MathUtils;
import daamky.ua.utils.network.Network;
import daamky.ua.utils.render.Render3D;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;
import daamky.ua.utils.repository.friend.FriendUtils;
import daamky.ua.utils.sounds.SoundManager;

public final class ServerHelper extends Module {
   private static final long PRE_USE_STOP_DELAY_MS = 0L;
   private static final int FRIEND_COLOR = ColorUtil.rgba(85, 255, 85, 255);
   private static final int FRIEND_FILL_COLOR = ColorUtil.rgba(85, 255, 85, 55);
   private static final float STRUCTURE_ICON_SCALE = 0.5F;
   private static final float STRUCTURE_MARKER_FONT_SIZE = 6.0F;
   private static final long TRAP_MARKER_TIME_MS = 15000L;
   private static final long BIG_TRAP_MARKER_TIME_MS = 30000L;
   private static final long PLAST_MARKER_TIME_MS = 60000L;
   private static final long STRUCTURE_VALIDATE_DELAY_MS = 75L;
   private static final long STRUCTURE_VALIDATE_TIMEOUT_MS = 1000L;
   private static final double STRUCTURE_DEDUPE_DISTANCE = 3.0;
   private static final double PLAST_TRAP_SUPPRESS_DISTANCE = 7.0;
   private static final int PLAST_MIN_SOLID_BLOCKS = 22;
   private static final int PLAST_MAX_ADJACENT_SOLID_BLOCKS = 4;
   private final ModeSetting mode = this.register(
      new ModeSetting("Тип сервера", "Позволяет выбрать тип сервера.", "FunTime", "ReallyWorld", "HolyWorld", "FunTime")
   );
   private final ModeSetting swapMode = this.register(new ModeSetting("Метод свапа", "Способ свапа предметов.", "SpookyTime", "SpookyTime", "FunTime"));
   private final BooleanSetting autoLootSetting = this.register(new BooleanSetting("Авто лут", "Кража лута с ботов на ивенте.", true));
   private final BooleanSetting autoShulkerSetting = this.register(new BooleanSetting("Авто шалкер", "Автоматически кладет лут в шалкер.", true));
   private final BooleanSetting autoRepairSetting = this.register(new BooleanSetting("Авто ремонт", "Авто ремонтирует броню пузырем опыта.", true));
   private final BooleanSetting consumablesSetting = this.register(new BooleanSetting("Таймер расходников", "Отображает время до окончания расходников.", true));
   private final BindSetting funtimeAhSearchBind = this.register(
      new BindSetting("Поиск на аукционе", "Клавиша для /ah search по предмету в руке.", KeyBind.NONE)
   );
   private final BooleanSetting autoPointSetting = this.register(new BooleanSetting("Авто поинт", "Отображает информацию об ивенте.", true));
   private final ColorSetting boxFillColor = this.register(new ColorSetting("Цвет заливки", "Цвет заливки бокса.", new Color(130, 32, 16, 40)));
   private final ColorSetting boxLineColor = this.register(new ColorSetting("Цвет линий", "Цвет линий бокса.", new Color(130, 32, 16, 255)));
   private final List<ServerHelper.KeyBindingEntry> keyBindings = new ArrayList<>();
   private final Map<String, ServerHelper.ItemInfo> itemConfig = new HashMap<>();
   private final Map<String, Boolean> lastKeyStates = new HashMap<>();
   private final Map<String, Boolean> bindingItemAvailable = new HashMap<>();
   private final ArrayDeque<String> itemQueue = new ArrayDeque<>();
   private final Map<Item, Integer> pendingLootItems = new HashMap<>();
   private final Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
   private final List<ServerHelper.PendingStructure> pendingStructures = new CopyOnWriteArrayList<>();
   private final List<ServerHelper.Structure> structures = new CopyOnWriteArrayList<>();
   private final List<ServerHelper.ServerEventData> serverEvents = new CopyOnWriteArrayList<>();
   private final StopWatch itemTimer = new StopWatch();
   private final StopWatch ahSearchTimer = new StopWatch();
   private final StopWatch itemsWatch = new StopWatch();
   private final StopWatch shulkerWatch = new StopWatch();
   private final StopWatch repairWatch = new StopWatch();
   private final Script script = new Script();
   private final Script shulkerScript = new Script();
   private final MovementController movement = new MovementController();
   private ServerHelper.ActionState actionState = ServerHelper.ActionState.IDLE;
   private int originalSlot = -1;
   private int originalSourceSlot = -1;
   private Slot pendingInventorySlot;
   private int pendingHotbarSlot = -1;
   private long actionTimer;
   private long stopMovementUntil;
   private boolean keysOverridden;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private boolean funTimeRecovery;
   private int recoveryInventorySlot = -1;
   private int recoveryHotbarSlot = -1;
   private Item recoveryHotbarItem;
   private int recoveryAttempts;
   private long recoveryStartedAt;
   private long recoveryLastAttemptAt;
   private boolean ahSearchBindWasPressed;
   private UUID entityUUID;

   public ServerHelper() {
      super("Server Helper", "Помощник для серверов.", ModuleCategory.MISC);
      this.autoLootSetting.visibleWhen(() -> this.mode.is("HolyWorld"));
      this.autoShulkerSetting.visibleWhen(() -> this.mode.is("HolyWorld"));
      this.autoRepairSetting.visibleWhen(() -> this.mode.is("HolyWorld"));
      this.consumablesSetting.visibleWhen(() -> this.mode.is("FunTime"));
      this.funtimeAhSearchBind.visibleWhen(() -> this.mode.is("FunTime"));
      this.autoPointSetting.visibleWhen(() -> this.mode.is("FunTime"));
      this.boxFillColor.visibleWhen(() -> this.mode.is("FunTime"));
      this.boxLineColor.visibleWhen(() -> this.mode.is("FunTime"));
      this.addBind("antiflight", Items.FIREWORK_STAR, "Анти полет", "Клавиша анти полета.", () -> this.mode.is("ReallyWorld"), 0.0F);
      this.addBind("expscroll", Items.FLOWER_BANNER_PATTERN, "Свиток опыта", "Клавиша свитка опыта.", () -> this.mode.is("ReallyWorld"), 0.0F);
      this.addBind("dtrap", Items.PRISMARINE_SHARD, "Взрывная трапка", "Клавиша взрывной трапки.", () -> this.mode.is("HolyWorld"), 5.0F);
      this.addBind("trap_holy", Items.POPPED_CHORUS_FRUIT, "Обычная трапка", "Клавиша обычной трапки.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("stan", Items.NETHER_STAR, "Стан", "Клавиша стана.", () -> this.mode.is("HolyWorld"), 30.0F);
      this.addBind("ditem", Items.FIRE_CHARGE, "Взрывная штучка", "Клавиша взрывной штучки.", () -> this.mode.is("HolyWorld"), 5.0F);
      this.addBind("snow", Items.SNOWBALL, "Снежок заморозка", "Клавиша снежка.", () -> this.mode.is("HolyWorld") || this.mode.is("FunTime"), 7.0F);
      this.addBind("bojaura", Items.PHANTOM_MEMBRANE, "Божья аура", "Клавиша божьей ауры.", () -> this.mode.is("FunTime"), 2.0F);
      this.addBind("trap", Items.NETHERITE_SCRAP, "Трапка", "Клавиша трапки.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("plast", Items.DRIED_KELP, "Пласт", "Клавиша пласта.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("sugar", Items.SUGAR, "Явная пыль", "Клавиша явной пыли.", () -> this.mode.is("FunTime"), 10.0F);
      this.addBind("fireSwirl", Items.FIRE_CHARGE, "Огненный смерч", "Клавиша огненного смерча.", () -> this.mode.is("FunTime"), 10.0F);
      this.addBind("disorientation", Items.ENDER_EYE, "Дезориентация", "Клавиша дезориентации.", () -> this.mode.is("FunTime"), 10.0F);
      this.addBind("windcharge", Items.WIND_CHARGE, "Заряд ветра", "Клавиша заряда ветра.", () -> this.mode.is("FunTime"), 10.0F);
      this.addBind("tikva", Items.JACK_O_LANTERN, "Светильник Джека", "Клавиша светильника Джека.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("exp", Items.EXPERIENCE_BOTTLE, "Пузырь опыта", "Клавиша пузыря опыта.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("shulker1", Items.PINK_SHULKER_BOX, "Рюкзак 1 уровня", "Клавиша рюкзака 1 уровня.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("shulker2", Items.BLUE_SHULKER_BOX, "Рюкзак 2 уровня", "Клавиша рюкзака 2 уровня.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("shulker3", Items.RED_SHULKER_BOX, "Рюкзак 3 уровня", "Клавиша рюкзака 3 уровня.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("shulker4", Items.PINK_SHULKER_BOX, "Рюкзак 4 уровня", "Клавиша рюкзака 4 уровня.", () -> this.mode.is("HolyWorld"), 0.0F);
      this.addBind("hlopushka", Items.SPLASH_POTION, "Хлопушка", "Клавиша хлопушки.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("holywater", Items.SPLASH_POTION, "Святая вода", "Клавиша святой воды.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("gnev", Items.SPLASH_POTION, "Зелье Гнева", "Клавиша зелья гнева.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("paladin", Items.SPLASH_POTION, "Зелье Палладина", "Клавиша зелья палладина.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("assassin", Items.SPLASH_POTION, "Зелье Ассасина", "Клавиша зелья ассасина.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("radiation", Items.SPLASH_POTION, "Зелье Радиации", "Клавиша зелья радиации.", () -> this.mode.is("FunTime"), 0.0F);
      this.addBind("snotvornoe", Items.SPLASH_POTION, "Снотворное", "Клавиша снотворного.", () -> this.mode.is("FunTime"), 0.0F);
      this.addLoreItem("sugar", Items.SUGAR, "Явная пыль", "явная пыль", List.of("световая вспышка", "радиус: 10 блоков", "свечение", "слепота"));
      this.addLoreItem("disorientation", Items.ENDER_EYE, "Дезориентация", "дезориентация", List.of("чем ближе цель, тем дольше длительность эффектов"));
      this.addLoreItem("trap", Items.NETHERITE_SCRAP, "Трапка", "трапка", List.of("нерушимая клетка", "длительность: 15 секунд"));
      this.addLoreItem("plast", Items.DRIED_KELP, "Пласт", "пласт", List.of("нерушимая стена", "вертикальный:", "горизонтальный:"));
      this.addLoreItem("fireSwirl", Items.FIRE_CHARGE, "Огненный смерч", "огненный смерч", List.of("огненная волна", "радиус: 10 блоков", "поджог"));
      this.addSimpleItem("windcharge", Items.WIND_CHARGE, "Заряд ветра", "заряд ветра");
      this.addLoreItem("snow", Items.SNOWBALL, "Снежок заморозка", "снежок заморозка", List.of("ледяная сфера", "радиус: 7 блоков", "заморозка", "слабость"));
      this.addLoreItem(
         "bojaura", Items.PHANTOM_MEMBRANE, "Божья аура", "божья аура", List.of("божественная аура", "радиус: 2 блока", "снятие всех эффектов", "невидимость")
      );
      this.addPotionItem(
         "hlopushka",
         "Хлопушка",
         "хлопушка",
         new ServerHelper.EffectRequirement(StatusEffects.SLOWNESS, 9),
         new ServerHelper.EffectRequirement(StatusEffects.SPEED, 4),
         new ServerHelper.EffectRequirement(StatusEffects.BLINDNESS, 9),
         new ServerHelper.EffectRequirement(StatusEffects.GLOWING, 0)
      );
      this.addPotionItem(
         "holywater",
         "Святая вода",
         "святая вода",
         new ServerHelper.EffectRequirement(StatusEffects.REGENERATION, 2),
         new ServerHelper.EffectRequirement(StatusEffects.INVISIBILITY, 1),
         new ServerHelper.EffectRequirement(StatusEffects.INSTANT_HEALTH, 1)
      );
      this.addPotionItem(
         "gnev",
         "Зелье Гнева",
         "зелье гнева",
         new ServerHelper.EffectRequirement(StatusEffects.STRENGTH, 4),
         new ServerHelper.EffectRequirement(StatusEffects.SLOWNESS, 3)
      );
      this.addPotionItem(
         "paladin",
         "Зелье Палладина",
         "зелье палладина",
         new ServerHelper.EffectRequirement(StatusEffects.RESISTANCE, 0),
         new ServerHelper.EffectRequirement(StatusEffects.FIRE_RESISTANCE, 0),
         new ServerHelper.EffectRequirement(StatusEffects.INVISIBILITY, 0),
         new ServerHelper.EffectRequirement(StatusEffects.HEALTH_BOOST, 2)
      );
      this.addPotionItem(
         "assassin",
         "Зелье Ассасина",
         "зелье ассасина",
         new ServerHelper.EffectRequirement(StatusEffects.STRENGTH, 3),
         new ServerHelper.EffectRequirement(StatusEffects.SPEED, 2),
         new ServerHelper.EffectRequirement(StatusEffects.HASTE, 0),
         new ServerHelper.EffectRequirement(StatusEffects.INSTANT_DAMAGE, 1)
      );
      this.addPotionItem(
         "radiation",
         "Зелье Радиации",
         "зелье радиации",
         new ServerHelper.EffectRequirement(StatusEffects.POISON, 1),
         new ServerHelper.EffectRequirement(StatusEffects.WITHER, 1),
         new ServerHelper.EffectRequirement(StatusEffects.SLOWNESS, 2),
         new ServerHelper.EffectRequirement(StatusEffects.HUNGER, 4),
         new ServerHelper.EffectRequirement(StatusEffects.GLOWING, 0)
      );
      this.addPotionItem(
         "snotvornoe",
         "Снотворное",
         "снотворное",
         new ServerHelper.EffectRequirement(StatusEffects.WEAKNESS, 1),
         new ServerHelper.EffectRequirement(StatusEffects.MINING_FATIGUE, 1),
         new ServerHelper.EffectRequirement(StatusEffects.WITHER, 2),
         new ServerHelper.EffectRequirement(StatusEffects.BLINDNESS, 0)
      );
      this.addSimpleItem("antiflight", Items.FIREWORK_STAR, "Анти полет", "анти полет");
      this.addSimpleItem("expscroll", Items.FLOWER_BANNER_PATTERN, "Свиток опыта", "свиток опыта");
      this.addSimpleItem("dtrap", Items.PRISMARINE_SHARD, "Взрывная трапка", "взрывная трапка");
      this.addSimpleItem("trap_holy", Items.POPPED_CHORUS_FRUIT, "Обычная трапка", "трапка");
      this.addSimpleItem("stan", Items.NETHER_STAR, "Стан", "стан");
      this.addSimpleItem("ditem", Items.FIRE_CHARGE, "Взрывная штучка", "взрывная");
      this.addSimpleItem("tikva", Items.JACK_O_LANTERN, "Светильник Джека", "светильник джека");
      this.addSimpleItem("exp", Items.EXPERIENCE_BOTTLE, "Пузырь опыта", "пузырь опыта");
      this.addSimpleItem("shulker1", Items.PINK_SHULKER_BOX, "Рюкзак 1 уровня", "рюкзак (i уровень)");
      this.addSimpleItem("shulker2", Items.BLUE_SHULKER_BOX, "Рюкзак 2 уровня", "рюкзак (ii уровень)");
      this.addSimpleItem("shulker3", Items.RED_SHULKER_BOX, "Рюкзак 3 уровня", "рюкзак (iii уровень)");
      this.addSimpleItem("shulker4", Items.PINK_SHULKER_BOX, "Рюкзак 4 уровня", "рюкзак (iv уровень)");
   }

   public List<ServerHelper.HudBind> getHudBinds(float tickDelta) {
      if (mc.player != null && mc.world != null) {
         List<ServerHelper.HudBind> binds = new ArrayList<>();

         for (ServerHelper.KeyBindingEntry bind : this.keyBindings) {
            if (bind.setting.isVisible() && bind.setting.getValue().isBound()) {
               ServerHelper.ItemInfo info = this.itemConfig.get(bind.key);
               Slot slot = this.findSlotByBinding(bind);
               ItemStack stack = slot == null ? bind.item.getDefaultStack() : slot.getStack().copy();
               int count = this.countMatchingBinding(bind, info);
               float cooldown = !stack.isEmpty() && mc.player.getItemCooldownManager().isCoolingDown(stack)
                  ? mc.player.getItemCooldownManager().getCooldownProgress(stack, tickDelta)
                  : 0.0F;
               String displayName = info == null ? stack.getName().getString() : info.displayName;
               binds.add(new ServerHelper.HudBind(bind.key, bind.item, bind.setting, stack, count, cooldown, displayName));
            }
         }

         return binds;
      } else {
         return List.of();
      }
   }

   public List<ServerHelper.HudEvent> getHudEvents() {
      if (mc.player != null && mc.world != null) {
         List<ServerHelper.HudEvent> events = new ArrayList<>();

         for (ServerHelper.ServerEventData event : this.serverEvents) {
            if (event.anarchy == Network.getAnarchyMode() && this.matchesWorld(event.world)) {
               double open = (event.timeOpen - (double)System.currentTimeMillis()) / 1000.0;
               double end = (event.timeEnd - (double)System.currentTimeMillis()) / 1000.0;
               String timer = open > 0.0 ? "Start " + this.formatSeconds(open) : (end > 0.0 ? "End " + this.formatSeconds(end) : "Done");
               double distance = Math.sqrt(mc.player.squaredDistanceTo(event.vec.x, event.vec.y, event.vec.z));
               events.add(new ServerHelper.HudEvent(event.name, event.lvl, event.owner, timer, distance));
            }
         }

         return events;
      } else {
         return List.of();
      }
   }

   @Override
   protected void onEnable() {
      this.resetState();
   }

   @Override
   protected void onDisable() {
      if (this.keysOverridden) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
      }

      this.resetState();
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && mc.player != null && mc.world != null) {
         if (event.getPacket() instanceof ItemPickupAnimationS2CPacket packet
            && this.autoShulkerSetting.isVisible()
            && this.autoShulkerSetting.getValue()
            && packet.getCollectorEntityId() == mc.player.getId()
            && mc.world.getEntityById(packet.getEntityId()) instanceof ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getStack();
            if (stack.get(DataComponentTypes.CONTAINER) == null) {
               this.pendingLootItems.merge(stack.getItem(), Math.max(1, stack.getCount()), Integer::sum);
               this.shulkerWatch.reset();
            }
         }

         if (event.getPacket() instanceof ChunkDeltaUpdateS2CPacket packetx && this.consumablesSetting.isVisible() && this.consumablesSetting.getValue()) {
            List<BlockPos> changedPositions = new ArrayList<>();
            packetx.visitUpdates((posx, state) -> {
               BlockPos immutable = posx.toImmutable();
               this.blockStateMap.put(immutable, state);
               changedPositions.add(immutable);
            });
            if (this.blockStateMap.size() < 600) {
               boolean trapDetected = false;

               for (BlockPos pos : changedPositions) {
                  if (this.isTrap(pos.up(2))) {
                     this.addStructure(Items.NETHERITE_SCRAP, Vec3d.ofCenter(pos), (double)(System.currentTimeMillis() + 15000L));
                     trapDetected = true;
                  } else if (this.isBigTrap(pos.up(3))) {
                     this.addStructure(Items.NETHERITE_SCRAP, Vec3d.ofCenter(pos), (double)(System.currentTimeMillis() + 30000L));
                     trapDetected = true;
                  }
               }

               if (!trapDetected) {
                  for (BlockPos posx : changedPositions) {
                     BlockPos plastCenter = this.findPlastCenter(posx);
                     Vec3d plastVec = plastCenter == null ? null : Vec3d.ofCenter(plastCenter);
                     if (plastVec != null && !this.hasStructureNear(Items.NETHERITE_SCRAP, plastVec, 7.0)) {
                        this.addPendingStructure(Items.DRIED_KELP, plastCenter, (double)(System.currentTimeMillis() + 60000L));
                     }
                  }
               }
            }
         }

         if (event.getPacket() instanceof GameMessageS2CPacket packetxx) {
            if (this.autoPointSetting.isVisible() && this.autoPointSetting.getValue()) {
               this.processServerPoint(packetxx.content());
            }

            this.processExperienceCooldown(packetxx.content().getString());
         }

         if (event.getPacket() instanceof OpenScreenS2CPacket packetxx && packetxx.getName().getString().contains("Рюкзак")) {
            this.shulkerWatch.reset();
         }
      }
   }

   @SubscribeEvent
   private void onRotationUpdate(RotationUpdateEvent event) {
      if (event.isPre() && mc.player != null && mc.world != null) {
         this.processFunTimeRecovery();
         if (mc.currentScreen != null) {
            this.clearPressedBindState();
         } else {
            long now = System.currentTimeMillis();
            if (now < this.stopMovementUntil || this.keysOverridden && this.actionState != ServerHelper.ActionState.IDLE) {
               this.blockMovement();
            }

            this.processKeyBindings();
            this.processFuntimeAhSearchCombo();
            if (this.actionState != ServerHelper.ActionState.IDLE) {
               this.processItemAction();
            }

            this.processItemQueue();
            this.processServerAutomation();
         }
      }
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (mc.player != null && mc.world != null && mc.getWindow() != null && mc.currentScreen == null) {
         long handle = mc.getWindow().getHandle();
         int lineColor = this.mode.is("FunTime") ? this.boxLineColor.getValue().getRGB() : -8249328;
         int fillColor = this.mode.is("FunTime") ? this.boxFillColor.getValue().getRGB() : 679616528;
         BlockPos playerPos = mc.player.getBlockPos();
         Vec3d smooth = MathUtils.interpolate(new Vec3d(mc.player.lastX, mc.player.lastY, mc.player.lastZ), mc.player.getEntityPos())
            .subtract(Vec3d.of(playerPos));

         for (ServerHelper.KeyBindingEntry bind : this.keyBindings) {
            if (bind.setting.isVisible() && bind.setting.getValue().isDown(handle) && this.bindingItemAvailable.getOrDefault(bind.key, false)) {
               String var10 = bind.setting.getName();
               switch (var10) {
                  case "Трапка":
                  case "Обычная трапка":
                     this.drawItemCube(playerPos, smooth, 111.9921F, lineColor, fillColor);
                     break;
                  case "Дезориентация":
                  case "Огненный смерч":
                  case "Явная пыль":
                     Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), bind.distance, this.validDistance(bind.distance) ? FRIEND_COLOR : lineColor);
                     break;
                  case "Взрывная штучка":
                     Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), 5.0F, this.validDistance(5.0F) ? FRIEND_COLOR : lineColor);
                     break;
                  case "Пласт":
                     Render3D.drawPlastShape(playerPos, smooth, lineColor, fillColor);
                     break;
                  case "Взрывная трапка":
                     this.drawItemCube(playerPos, smooth, 3.99F, lineColor, fillColor);
                     break;
                  case "Стан":
                     this.drawItemCube(playerPos, smooth, 15.01F, lineColor, fillColor);
                     break;
                  case "Снежок заморозка":
                     Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), 7.0F, this.validDistance(7.0F) ? FRIEND_COLOR : lineColor);
                     break;
                  case "Божья аура":
                     Render3D.drawRadiusCircle(MathUtils.interpolate(mc.player), 2.0F, this.validDistance(2.0F) ? FRIEND_COLOR : lineColor);
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onDraw(DrawEvent event) {
      if (mc.player != null && mc.world != null) {
         DrawContext graphics = event.getGraphics();
         this.structures
            .removeIf(structure -> structure.item == Items.DRIED_KELP && this.sameStructureContext(structure) && !this.isStructureStillValid(structure));

         for (ServerHelper.Structure structure : this.structures) {
            if (structure.anarchy == Network.getAnarchyMode() && this.matchesWorld(structure.world)) {
               Vec3d screen = this.projectWorldToScreen(structure.vec);
               if (screen != null) {
                  this.renderStructureMarker(graphics, structure, screen);
               }
            }
         }

         for (ServerHelper.ServerEventData serverEvent : this.serverEvents) {
            if (serverEvent.anarchy == Network.getAnarchyMode() && this.matchesWorld(serverEvent.world)) {
               Vec3d screen = this.projectWorldToScreen(serverEvent.vec);
               if (screen != null) {
                  double open = (serverEvent.timeOpen - (double)System.currentTimeMillis()) / 1000.0;
                  double end = (serverEvent.timeEnd - (double)System.currentTimeMillis()) / 1000.0;
                  List<String> lines = new ArrayList<>();
                  lines.add(
                     serverEvent.name
                        + " ["
                        + this.formatDecimal(
                           mc.player.squaredDistanceTo(serverEvent.vec.x, serverEvent.vec.y, serverEvent.vec.z) <= 0.0
                              ? 0.0
                              : Math.sqrt(mc.player.squaredDistanceTo(serverEvent.vec.x, serverEvent.vec.y, serverEvent.vec.z))
                        )
                        + "m]"
                  );
                  if (serverEvent.owner != null) {
                     lines.add("Призван: " + serverEvent.owner);
                  }

                  lines.add(open > 0.0 ? "До начала: " + this.formatSeconds(open) : (end > 0.0 ? "До конца: " + this.formatSeconds(end) : "Конец ивента"));
                  if (serverEvent.lvl != null) {
                     lines.add(serverEvent.lvl);
                  }

                  this.drawLines(graphics, lines, screen);
               }
            }
         }

         if (this.entityUUID != null) {
            PlayerInteractionHelper.streamEntities()
               .filter(entity -> entity.getUuid().equals(this.entityUUID))
               .findFirst()
               .ifPresent(
                  entity -> {
                     Vec3d screenx = this.projectWorldToScreen(Vec3d.ofCenter(entity.getBlockPos().down()));
                     if (screenx != null) {
                        String text = !this.itemsWatch.finished(200.0)
                           ? "Можно забрать"
                           : (!this.itemsWatch.finished(20000.0) ? this.formatSeconds(20.0 - (double)this.itemsWatch.elapsedTime() / 1000.0) : "Скоро");
                        int color = mc.player.distanceTo(entity) < 5.0F ? FRIEND_COLOR : -43691;
                        graphics.drawText(mc.textRenderer, text, (int)screenx.x - mc.textRenderer.getWidth(text) / 2, (int)screenx.y, color, true);
                     }
                  }
               );
         }
      }
   }

   private void addBind(String key, Item item, String name, String description, BooleanSupplier visible, float distance) {
      BindSetting setting = this.register(new BindSetting(name, description, KeyBind.NONE));
      setting.visibleWhen(visible);
      this.keyBindings.add(new ServerHelper.KeyBindingEntry(key, item, setting, distance));
      this.lastKeyStates.put(key, false);
      this.bindingItemAvailable.put(key, false);
   }

   private void addSimpleItem(String key, Item item, String displayName, String fallbackName) {
      this.itemConfig.put(key, new ServerHelper.ItemInfo(item, displayName, fallbackName, false, null, null));
   }

   private void addLoreItem(String key, Item item, String displayName, String fallbackName, List<String> loreKeywords) {
      this.itemConfig.put(key, new ServerHelper.ItemInfo(item, displayName, fallbackName, true, loreKeywords, null));
   }

   private void addPotionItem(String key, String displayName, String fallbackName, ServerHelper.EffectRequirement... requirements) {
      this.itemConfig.put(key, new ServerHelper.ItemInfo(Items.SPLASH_POTION, displayName, fallbackName, true, null, List.of(requirements)));
   }

   private void resetState() {
      this.itemQueue.clear();
      this.pendingLootItems.clear();
      this.lastKeyStates.replaceAll((k, v) -> false);
      this.bindingItemAvailable.replaceAll((k, v) -> false);
      this.blockStateMap.clear();
      this.structures.clear();
      this.serverEvents.clear();
      this.script.cleanup();
      this.shulkerScript.cleanup();
      this.itemTimer.reset();
      this.ahSearchTimer.reset();
      this.itemsWatch.reset();
      this.shulkerWatch.reset();
      this.repairWatch.reset();
      this.clearActionState();
      this.clearFunTimeRecovery();
      this.ahSearchBindWasPressed = false;
      this.entityUUID = null;
      this.movement.reset();
   }

   private void clearActionState() {
      this.actionState = ServerHelper.ActionState.IDLE;
      this.originalSlot = -1;
      this.originalSourceSlot = -1;
      this.pendingInventorySlot = null;
      this.pendingHotbarSlot = -1;
      this.actionTimer = 0L;
      this.stopMovementUntil = 0L;
      this.keysOverridden = false;
   }

   private void clearPressedBindState() {
      this.lastKeyStates.replaceAll((k, v) -> false);
      this.bindingItemAvailable.replaceAll((k, v) -> false);
      this.itemQueue.clear();
      this.ahSearchBindWasPressed = false;
   }

   private void blockMovement() {
      mc.options.forwardKey.setPressed(false);
      mc.options.backKey.setPressed(false);
      mc.options.leftKey.setPressed(false);
      mc.options.rightKey.setPressed(false);
      mc.options.jumpKey.setPressed(false);
      if (mc.player.isSprinting()) {
         mc.player.setSprinting(false);
      }

      mc.options.sprintKey.setPressed(false);
   }

   private void processKeyBindings() {
      if (mc.getWindow() != null) {
         long handle = mc.getWindow().getHandle();

         for (ServerHelper.KeyBindingEntry bind : this.keyBindings) {
            if (bind.setting.isVisible()) {
               boolean current = bind.setting.getValue().isDown(handle);
               boolean previous = this.lastKeyStates.getOrDefault(bind.key, false);
               Slot slot = current ? this.findSlotByBinding(bind) : null;
               this.bindingItemAvailable.put(bind.key, slot != null);
               if (current && !previous) {
                  ServerHelper.ItemInfo info = this.itemConfig.get(bind.key);
                  this.handleBoundItemPress(bind.key, info, slot);
               }

               this.lastKeyStates.put(bind.key, current);
            }
         }
      }
   }

   private void handleBoundItemPress(String key, ServerHelper.ItemInfo info, Slot slot) {
      if (info != null && mc.player != null) {
         if (slot == null) {
            this.notifyItemMissing(info);
         } else {
            ItemStack stack = slot.getStack();
            if (mc.player.getItemCooldownManager().isCoolingDown(stack)) {
               this.notifyItemCooldown(info, stack);
            } else {
               if (!this.itemQueue.contains(key)) {
                  this.itemQueue.add(key);
               }
            }
         }
      }
   }

   private void processItemQueue() {
      if (this.actionState == ServerHelper.ActionState.IDLE && !this.itemQueue.isEmpty() && this.itemTimer.finished(0.0)) {
         String itemKey = this.itemQueue.poll();
         ServerHelper.ItemInfo info = this.itemConfig.get(itemKey);
         if (info != null) {
            Slot slot = this.findSlotByItem(info);
            if (slot == null) {
               this.notifyItemMissing(info);
               this.itemTimer.reset();
            } else if (mc.player.getItemCooldownManager().isCoolingDown(slot.getStack())) {
               this.notifyItemCooldown(info, slot.getStack());
               this.itemTimer.reset();
            } else {
               this.notifyItemUsed(info);
               this.startItemUse(slot);
               this.itemTimer.reset();
            }
         }
      }
   }

   private void startItemUse(Slot slot) {
      if (slot != null) {
         this.originalSlot = mc.player.getInventory().getSelectedSlot();
         this.originalSourceSlot = InventoryTask.getMenuSlotId(slot);
         if (this.swapMode.is("FunTime") && !this.isHotbarSourceSlot(this.originalSourceSlot)) {
            this.recoveryHotbarItem = mc.player.getInventory().getStack(this.originalSlot).getItem();
         }

         this.wasForwardPressed = mc.options.forwardKey.isPressed();
         this.wasBackPressed = mc.options.backKey.isPressed();
         this.wasLeftPressed = mc.options.leftKey.isPressed();
         this.wasRightPressed = mc.options.rightKey.isPressed();
         this.wasJumpPressed = mc.options.jumpKey.isPressed();
         this.keysOverridden = true;
         this.movement.saveState();
         this.movement.block();
         this.blockMovement();
         this.actionState = this.swapMode.is("FunTime") ? ServerHelper.ActionState.WAIT_BEFORE_SWAP : ServerHelper.ActionState.WAIT_BEFORE_USE;
         this.actionTimer = System.currentTimeMillis();
         this.stopMovementUntil = this.actionTimer + 0L;
      }
   }

   private void processItemAction() {
      int safety = 0;
      if (this.actionState != ServerHelper.ActionState.IDLE && safety++ < 4) {
         long now = System.currentTimeMillis();
         if (now - this.actionTimer >= 0L) {
            switch (this.actionState) {
               case WAIT_BEFORE_SWAP:
                  if (this.isHotbarSourceSlot(this.originalSourceSlot)) {
                     this.pendingHotbarSlot = this.toHotbarSlot(this.originalSourceSlot);
                     InventoryUtils.syncSelectedHotbarSlot(this.pendingHotbarSlot);
                  } else {
                     this.pendingInventorySlot = this.getSourceSlot(this.originalSourceSlot);
                     this.prepareInventoryUseSilent(this.pendingInventorySlot);
                  }

                  this.actionState = ServerHelper.ActionState.WAIT_BEFORE_USE;
                  this.actionTimer = now;
                  this.stopMovementUntil = this.actionTimer + 0L;
                  break;
               case WAIT_BEFORE_USE:
                  if (!this.swapMode.is("FunTime")) {
                     if (this.isHotbarSourceSlot(this.originalSourceSlot)) {
                        this.pendingHotbarSlot = this.toHotbarSlot(this.originalSourceSlot);
                        InventoryUtils.syncSelectedHotbarSlot(this.pendingHotbarSlot);
                     } else {
                        this.pendingInventorySlot = this.getSourceSlot(this.originalSourceSlot);
                        if (this.pendingInventorySlot == null) {
                           this.restoreKeyStates();
                           this.clearActionState();
                           return;
                        }

                        this.prepareInventoryUseSilent(this.pendingInventorySlot);
                     }
                  }

                  this.performUseItem();
                  this.actionState = ServerHelper.ActionState.WAIT_BEFORE_RESTORE;
                  this.actionTimer = now;
                  this.stopMovementUntil = this.actionTimer + 0L;
                  break;
               case WAIT_BEFORE_RESTORE:
                  if (this.pendingInventorySlot != null) {
                     this.finishInventoryUseSilent(this.pendingInventorySlot);
                     if (this.swapMode.is("FunTime")) {
                        this.actionState = ServerHelper.ActionState.WAIT_BEFORE_CLOSE;
                        this.actionTimer = now;
                        return;
                     }
                  } else if (this.pendingHotbarSlot != -1) {
                     this.finishHotbarUseSilent(this.pendingHotbarSlot);
                  }

                  this.restoreKeyStates();
                  this.clearActionState();
                  break;
               case WAIT_BEFORE_CLOSE:
                  this.startFunTimeRecovery();
                  InventoryUtils.closeScreen();
                  this.restoreKeyStates();
                  this.clearActionState();
                  break;
               default:
                  return;
            }
         }
      }
   }

   private void startFunTimeRecovery() {
      if (this.pendingInventorySlot != null && this.originalSlot != -1 && this.recoveryHotbarItem != null) {
         this.funTimeRecovery = true;
         this.recoveryInventorySlot = InventoryTask.getMenuSlotId(this.pendingInventorySlot);
         this.recoveryHotbarSlot = this.originalSlot;
         this.recoveryAttempts = 3;
         this.recoveryStartedAt = System.currentTimeMillis();
         this.recoveryLastAttemptAt = this.recoveryStartedAt;
      }
   }

   private void processFunTimeRecovery() {
      if (this.funTimeRecovery && this.actionState == ServerHelper.ActionState.IDLE && mc.player != null && mc.interactionManager != null) {
         long now = System.currentTimeMillis();
         if (this.recoveryAttempts > 0
            && now - this.recoveryStartedAt < 800L
            && this.recoveryInventorySlot >= 0
            && this.recoveryHotbarSlot >= 0
            && this.recoveryHotbarSlot < 9
            && this.recoveryInventorySlot < mc.player.currentScreenHandler.slots.size()) {
            Item hotbarItem = mc.player.getInventory().getStack(this.recoveryHotbarSlot).getItem();
            Item inventoryItem = mc.player.currentScreenHandler.getSlot(this.recoveryInventorySlot).getStack().getItem();
            if (hotbarItem == this.recoveryHotbarItem) {
               this.clearFunTimeRecovery();
            } else if (inventoryItem == this.recoveryHotbarItem && now - this.recoveryLastAttemptAt >= 150L) {
               InventoryUtils.click(this.recoveryInventorySlot, this.recoveryHotbarSlot, SlotActionType.SWAP);
               this.recoveryAttempts--;
               this.recoveryLastAttemptAt = now;
            }
         } else {
            this.clearFunTimeRecovery();
         }
      }
   }

   private void clearFunTimeRecovery() {
      this.funTimeRecovery = false;
      this.recoveryInventorySlot = -1;
      this.recoveryHotbarSlot = -1;
      this.recoveryHotbarItem = null;
      this.recoveryAttempts = 0;
      this.recoveryStartedAt = 0L;
      this.recoveryLastAttemptAt = 0L;
   }

   private boolean isHotbarSourceSlot(int slot) {
      return slot >= 0 && slot < 9 || slot >= 36 && slot < 45;
   }

   private int toHotbarSlot(int slot) {
      return slot >= 36 ? slot - 36 : slot;
   }

   private Slot getSourceSlot(int slot) {
      return slot >= 0 && mc.player != null && mc.player.currentScreenHandler != null && slot < mc.player.currentScreenHandler.slots.size()
         ? mc.player.currentScreenHandler.getSlot(slot)
         : null;
   }

   private void finishHotbarUseSilent(int hotbarSlot) {
      if (hotbarSlot != this.originalSlot) {
         InventoryUtils.syncSelectedHotbarSlot(this.originalSlot);
      }
   }

   private void prepareInventoryUseSilent(Slot slot) {
      if (slot != null && this.originalSlot != -1) {
         InventoryUtils.click(InventoryTask.getMenuSlotId(slot), this.originalSlot, SlotActionType.SWAP);
      }
   }

   private void finishInventoryUseSilent(Slot slot) {
      if (slot != null && this.originalSlot != -1) {
         InventoryUtils.click(InventoryTask.getMenuSlotId(slot), this.originalSlot, SlotActionType.SWAP);
         InventoryUtils.syncSelectedHotbarSlot(this.originalSlot);
      }
   }

   private void performUseItem() {
      if (mc.player != null && mc.interactionManager != null) {
         Hand hand = Hand.MAIN_HAND;
         mc.interactionManager.interactItem(mc.player, hand);
         mc.player.swingHand(hand);
      }
   }

   private void notifyItemUsed(ServerHelper.ItemInfo info) {
      this.notifyServerHelper1(this.itemDisplayName(info) + " использована");
   }

   private void notifyItemMissing(ServerHelper.ItemInfo info) {
      this.notifyServerHelper(this.itemDisplayName(info) + " не найден");
   }

   private void notifyItemCooldown(ServerHelper.ItemInfo info, ItemStack stack) {
      this.notifyServerHelper(this.itemDisplayName(info) + " будет доступен через " + this.formatCooldownDurationTicks(this.cooldownRemainingTicks(stack)));
   }

   private void notifyServerHelper(String text) {
      Notifications.push("Server Helper", text, 4000L, SoundManager.NOTIFICATION);
   }

   private void notifyServerHelper1(String text) {
      Notifications.push("Server Helper", text, 4000L);
   }

   private String itemDisplayName(ServerHelper.ItemInfo info) {
      return info != null && info.displayName != null && !info.displayName.isBlank() ? info.displayName : "Item";
   }

   private int cooldownRemainingTicks(ItemStack stack) {
      if (mc.player != null && stack != null && !stack.isEmpty()) {
         ItemCooldownManager cooldowns = mc.player.getItemCooldownManager();

         try {
            Identifier group = cooldowns.getGroup(stack);
            Map<Identifier, ?> activeCooldowns = ((ItemCooldownsAccessor)cooldowns).daamky$getCooldowns();
            if (activeCooldowns.get(group) instanceof ItemCooldownInstanceAccessor accessor) {
               return Math.max(0, accessor.daamky$getEndTime() - ((ItemCooldownsAccessor)cooldowns).daamky$getTickCount());
            }
         } catch (RuntimeException var7) {
         }

         float progress = cooldowns.getCooldownProgress(stack, 0.0F);
         return progress <= 0.0F ? -1 : Math.max(1, Math.round(progress * 20.0F));
      } else {
         return -1;
      }
   }

   private String formatCooldownDurationTicks(int ticks) {
      if (ticks < 0) {
         return "?";
      } else {
         int totalSeconds = Math.max(1, (int)Math.ceil((double)ticks / 20.0));
         int minutes = totalSeconds / 60;
         int seconds = totalSeconds % 60;
         return minutes > 0 ? minutes + ":" + String.format(Locale.US, "%02d", seconds) : totalSeconds + "s";
      }
   }

   private void restoreKeyStates() {
      if (this.keysOverridden) {
         mc.options.forwardKey.setPressed(this.wasForwardPressed);
         mc.options.backKey.setPressed(this.wasBackPressed);
         mc.options.leftKey.setPressed(this.wasLeftPressed);
         mc.options.rightKey.setPressed(this.wasRightPressed);
         mc.options.jumpKey.setPressed(this.wasJumpPressed);
         this.keysOverridden = false;
         this.movement.reset();
      }
   }

   private void processServerAutomation() {
      if (this.autoRepairSetting.isVisible()
         && this.autoRepairSetting.getValue()
         && this.actionState == ServerHelper.ActionState.IDLE
         && this.repairWatch.every(5000.0)
         && List.of(
               mc.player.getEquippedStack(EquipmentSlot.FEET),
               mc.player.getEquippedStack(EquipmentSlot.LEGS),
               mc.player.getEquippedStack(EquipmentSlot.CHEST),
               mc.player.getEquippedStack(EquipmentSlot.HEAD)
            )
            .stream()
            .anyMatch(this::needsMendingRepair)) {
         ServerHelper.ItemInfo exp = this.itemConfig.get("exp");
         Slot slot = exp == null ? null : this.findSlotByItem(exp);
         if (slot != null && !mc.player.getItemCooldownManager().isCoolingDown(slot.getStack())) {
            this.startItemUse(slot);
         }
      }

      if (this.autoShulkerSetting.isVisible()
         && this.autoShulkerSetting.getValue()
         && mc.currentScreen == null
         && !this.pendingLootItems.isEmpty()
         && this.shulkerScript.isFinished()
         && this.shulkerWatch.finished(300.0)) {
         Slot shulker = InventoryUtils.findSlot((Predicate<Slot>)(slotx -> slotx.hasStack() && slotx.getStack().get(DataComponentTypes.CONTAINER) != null));
         if (shulker != null) {
            this.openLootShulker(shulker);
         }
      }

      if (this.autoLootSetting.isVisible() && this.autoLootSetting.getValue()) {
         PlayerInteractionHelper.streamEntities()
            .filter(MerchantEntity.class::isInstance)
            .map(MerchantEntity.class::cast)
            .filter(villager -> !villager.getMainHandStack().isEmpty() || !villager.getOffHandStack().isEmpty())
            .filter(villager -> mc.player.distanceTo(villager) <= 6.0F)
            .findFirst()
            .ifPresent(villager -> {
               this.itemsWatch.reset();
               this.entityUUID = villager.getUuid();
               PlayerInteractionHelper.interactEntity(villager);
            });
      }

      this.script.cleanupIfFinished().update();
      this.shulkerScript.cleanupIfFinished().update();
      this.validatePendingStructures();
      this.blockStateMap.clear();
      this.structures.removeIf(structure -> structure.time <= (double)System.currentTimeMillis() || !this.isStructureStillValid(structure));
      this.serverEvents.removeIf(serverEvent -> serverEvent.timeEnd + 90000.0 <= (double)System.currentTimeMillis());
   }

   private boolean needsMendingRepair(ItemStack stack) {
      if (stack != null && !stack.isEmpty() && stack.isDamageable()) {
         if ((double)stack.getDamage() / (double)stack.getMaxDamage() < 0.94) {
            return false;
         } else {
            try {
               RegistryEntry<Enchantment> mending = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
               return EnchantmentHelper.getLevel(mending, stack) > 0;
            } catch (Throwable var3) {
               return true;
            }
         }
      } else {
         return false;
      }
   }

   private void openLootShulker(Slot shulker) {
      int selected = mc.player.getInventory().getSelectedSlot();
      int sourceSlot = InventoryTask.getMenuSlotId(shulker);
      boolean sourceInHotbar = sourceSlot >= 36 && sourceSlot < 45;
      if (sourceInHotbar) {
         InventoryUtils.syncSelectedHotbarSlot(sourceSlot - 36);
      } else {
         InventoryUtils.click(sourceSlot, selected, SlotActionType.SWAP);
      }

      PlayerInteractionHelper.interactItem(Hand.MAIN_HAND);
      this.shulkerScript
         .cleanup()
         .addTickStep(
            0,
            () -> this.moveLootIntoOpenedBackpack(sourceSlot, selected, sourceInHotbar),
            () -> mc.player.currentScreenHandler instanceof GenericContainerScreenHandler && mc.currentScreen != null
         );
   }

   private void moveLootIntoOpenedBackpack(int sourceSlot, int selectedSlot, boolean sourceInHotbar) {
      if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler chestMenu) {
         HashMap<Item, Integer> var12 = new HashMap<>();
         int start = chestMenu.getRows() * 9;

         for (int slotId = start; slotId < chestMenu.slots.size(); slotId++) {
            Slot slot = (Slot)chestMenu.slots.get(slotId);
            if (slot.hasStack()) {
               Item item = slot.getStack().getItem();
               int remaining = this.pendingLootItems.getOrDefault(item, 0) - var12.getOrDefault(item, 0);
               if (remaining > 0) {
                  int movedCount = Math.min(slot.getStack().getCount(), remaining);
                  InventoryUtils.click(slotId, 0, SlotActionType.QUICK_MOVE);
                  var12.merge(item, movedCount, Integer::sum);
               }
            }
         }

         for (Entry<Item, Integer> entry : var12.entrySet()) {
            int remaining = this.pendingLootItems.getOrDefault(entry.getKey(), 0) - entry.getValue();
            if (remaining <= 0) {
               this.pendingLootItems.remove(entry.getKey());
            } else {
               this.pendingLootItems.put(entry.getKey(), remaining);
            }
         }

         InventoryUtils.closeScreen();
         if (!sourceInHotbar) {
            InventoryUtils.click(sourceSlot, selectedSlot, SlotActionType.SWAP);
         }

         InventoryUtils.syncSelectedHotbarSlot(selectedSlot);
         this.shulkerWatch.reset();
      }
   }

   private List<String> getLore(ItemStack stack) {
      List<String> lore = new ArrayList<>();
      if (stack != null && !stack.isEmpty()) {
         try {
            LoreComponent itemLore = (LoreComponent)stack.get(DataComponentTypes.LORE);
            if (itemLore != null) {
               for (Text line : itemLore.lines()) {
                  String clean = this.cleanText(line);
                  if (!clean.isEmpty()) {
                     lore.add(clean);
                  }
               }
            }
         } catch (Throwable var7) {
         }

         return lore;
      } else {
         return lore;
      }
   }

   private boolean matchesLore(ItemStack stack, List<String> keywords) {
      if (keywords != null && !keywords.isEmpty()) {
         String lore = String.join(" ", this.getLore(stack));
         if (lore.isEmpty()) {
            return false;
         } else {
            int matches = 0;

            for (String keyword : keywords) {
               if (lore.contains(keyword.toLowerCase(Locale.ROOT))) {
                  matches++;
               }
            }

            return matches >= Math.min(2, keywords.size());
         }
      } else {
         return false;
      }
   }

   private boolean matchesPotionEffects(ItemStack stack, List<ServerHelper.EffectRequirement> requirements) {
      if (requirements != null && !requirements.isEmpty()) {
         if (stack.getItem() != Items.SPLASH_POTION && stack.getItem() != Items.LINGERING_POTION) {
            return false;
         } else {
            PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents == null) {
               return false;
            } else {
               int matches = 0;

               for (ServerHelper.EffectRequirement requirement : requirements) {
                  for (StatusEffectInstance effect : contents.customEffects()) {
                     if (effect.getEffectType().equals(requirement.effect) && effect.getAmplifier() >= requirement.minAmplifier) {
                        matches++;
                        break;
                     }
                  }

                  if (matches < requirements.size()) {
                     for (StatusEffectInstance effectx : contents.getEffects()) {
                        if (effectx.getEffectType().equals(requirement.effect) && effectx.getAmplifier() >= requirement.minAmplifier) {
                           matches++;
                           break;
                        }
                     }
                  }
               }

               return matches >= Math.min(2, requirements.size());
            }
         }
      } else {
         return false;
      }
   }

   private int countMatchingBinding(ServerHelper.KeyBindingEntry bind, ServerHelper.ItemInfo info) {
      if (bind != null && mc.player != null) {
         int count = 0;

         for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && this.matchesBindingStack(stack, bind, info)) {
               count += stack.getCount();
            }
         }

         return count;
      } else {
         return 0;
      }
   }

   private boolean matchesBindingStack(ItemStack stack, ServerHelper.KeyBindingEntry bind, ServerHelper.ItemInfo info) {
      if (stack == null || stack.isEmpty() || bind == null) {
         return false;
      } else if (info == null) {
         return stack.getItem() == bind.item;
      } else if (stack.getItem() != info.item) {
         return false;
      } else {
         if (this.mode.is("FunTime") && info.funTimeOnly) {
            if (info.effectRequirements != null && !info.effectRequirements.isEmpty() && this.matchesPotionEffects(stack, info.effectRequirements)) {
               return true;
            }

            if (info.loreKeywords != null && !info.loreKeywords.isEmpty() && this.matchesLore(stack, info.loreKeywords)) {
               return true;
            }
         }

         return this.matchesFallbackName(stack, info.nameFallback);
      }
   }

   private Slot findSlotByItem(ServerHelper.ItemInfo info) {
      if (info == null) {
         return null;
      } else {
         if (this.mode.is("FunTime") && info.funTimeOnly) {
            if (info.effectRequirements != null && !info.effectRequirements.isEmpty()) {
               Slot slot = InventoryUtils.findSlot(
                  (Predicate<Slot>)(candidate -> candidate.hasStack()
                        && candidate.getStack().getItem() == info.item
                        && this.matchesPotionEffects(candidate.getStack(), info.effectRequirements))
               );
               if (slot != null) {
                  return slot;
               }
            }

            if (info.loreKeywords != null && !info.loreKeywords.isEmpty()) {
               Slot slot = InventoryUtils.findSlot(
                  (Predicate<Slot>)(candidate -> candidate.hasStack()
                        && candidate.getStack().getItem() == info.item
                        && this.matchesLore(candidate.getStack(), info.loreKeywords))
               );
               if (slot != null) {
                  return slot;
               }
            }
         }

         return InventoryUtils.findSlot(
            (Predicate<Slot>)(candidate -> candidate.hasStack()
                  && candidate.getStack().getItem() == info.item
                  && this.matchesFallbackName(candidate.getStack(), info.nameFallback))
         );
      }
   }

   private boolean matchesFallbackName(ItemStack stack, String fallbackName) {
      if (fallbackName != null && !fallbackName.isEmpty()) {
         String cleanName = this.cleanText(stack.getName());
         if (cleanName.contains(fallbackName.toLowerCase(Locale.ROOT))) {
            return true;
         } else {
            List<String> lore = this.getLore(stack);
            return lore.isEmpty() ? false : String.join(" ", lore).contains(fallbackName.toLowerCase(Locale.ROOT));
         }
      } else {
         return false;
      }
   }

   private Slot findSlotByBinding(ServerHelper.KeyBindingEntry bind) {
      ServerHelper.ItemInfo info = this.itemConfig.get(bind.key);
      Slot slot = info == null ? null : this.findSlotByItem(info);
      return slot != null ? slot : InventoryUtils.findSlot((Predicate<Slot>)(candidate -> candidate.hasStack() && candidate.getStack().getItem() == bind.item));
   }

   private void processFuntimeAhSearchCombo() {
      if (mc.player != null && mc.currentScreen == null && this.mode.is("FunTime")) {
         KeyBind bind = this.funtimeAhSearchBind.getValue();
         boolean pressed = bind != null && mc.getWindow() != null && bind.isDown(mc.getWindow().getHandle());
         if (!pressed) {
            this.ahSearchBindWasPressed = false;
         } else if (!this.ahSearchBindWasPressed && this.ahSearchTimer.finished(700.0)) {
            ItemStack stack = mc.player.getMainHandStack();
            if (!stack.isEmpty() && mc.player.networkHandler != null) {
               String query = this.cleanText(stack.getName()).replaceAll("[^\\p{L}\\p{N}\\s]", " ").replaceAll("\\s+", " ").trim();
               if (!query.isEmpty()) {
                  mc.player.networkHandler.sendChatCommand("ah search " + query);
               }
            }

            this.ahSearchBindWasPressed = true;
            this.ahSearchTimer.reset();
         }
      }
   }

   private void renderStructureMarker(DrawContext graphics, ServerHelper.Structure structure, Vec3d screen) {
      String name = this.structureName(structure.item);
      String separator = "|";
      String timer = this.formatMarkerSeconds((structure.time - (double)System.currentTimeMillis()) / 1000.0);
      float fontSize = 6.0F;
      float nameWidth = Render2D.textWidth(FontType.SEMIBOLD, name, fontSize);
      float separatorWidth = Render2D.textWidth(FontType.SEMIBOLD, separator, fontSize);
      float timerWidth = Render2D.textWidth(FontType.SEMIBOLD, timer, fontSize);
      float iconSize = 8.0F;
      float paddingX = 5.0F;
      float gap = 4.0F;
      float width = paddingX * 2.0F + iconSize + gap + nameWidth + gap + separatorWidth + gap + timerWidth;
      float height = 17.0F;
      float panelX = (float)screen.x - width * 0.5F;
      float panelY = (float)screen.y - height * 0.5F;
      float iconX = panelX + paddingX;
      float iconY = panelY + (height - iconSize) * 0.5F;
      float textY = panelY + (height - fontSize) * 0.5F + 0.5F;
      float nameX = iconX + iconSize + gap;
      float separatorX = nameX + nameWidth + gap;
      float timerX = separatorX + separatorWidth + gap;
      HudRenderCompat.background(
         Render2D.blurBuilder()
            .rectangle(panelX, panelY, width - 3.0F, height)
            .radius(4.0F)
            .blurRadius(24.0F)
            .smoothness(1.8F)
            .color(ColorUtil.rgba(0, 0, 0, 255))
            .build()
      );
      graphics.getMatrices().pushMatrix();

      try {
         graphics.getMatrices().translate(iconX, iconY);
         graphics.getMatrices().scale(0.5F);
         graphics.drawItem(structure.item.getDefaultStack(), 0, 0);
      } finally {
         graphics.getMatrices().popMatrix();
      }

      Render2D.text(FontType.SEMIBOLD, name, nameX, textY, fontSize, ColorUtil.rgba(245, 245, 245, 255));
      Render2D.text(FontType.SEMIBOLD, separator, separatorX - 2.5F, textY - 1.5F, fontSize, ColorUtil.rgba(150, 150, 150, 255));
      Render2D.text(FontType.SEMIBOLD, timer, timerX - 5.0F, textY, fontSize, ColorUtil.rgba(245, 245, 245, 255));
   }

   private String structureName(Item item) {
      return item == Items.DRIED_KELP ? "Обычный пласт" : "Трапка";
   }

   private String formatMarkerSeconds(double seconds) {
      return Math.max(0L, Math.round(seconds)) + "s";
   }

   private boolean validDistance(float distance) {
      if (distance == 0.0F) {
         return true;
      } else {
         for (PlayerEntity player : mc.world.getPlayers()) {
            if (player != mc.player && !FriendUtils.isFriend(player) && mc.player.distanceTo(player) <= distance) {
               return true;
            }
         }

         return false;
      }
   }

   private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int lineColor, int fillColor) {
      Box box = new Box(playerPos.up()).offset(smooth).expand((double)size);
      boolean playerInside = mc.world
         .getPlayers()
         .stream()
         .anyMatch(player -> player != mc.player && !FriendUtils.isFriend(player) && box.intersects(player.getBoundingBox()));
      if (playerInside) {
         Render3D.drawBoxWithCrossFull(box, FRIEND_COLOR, FRIEND_FILL_COLOR, 2.0F);
      } else {
         Render3D.drawBoxWithCrossFull(box, lineColor, fillColor, 2.0F);
      }
   }

   private void processServerPoint(Text content) {
      String contentString = content.toString();
      String message = content.getString();
      String name = this.substringBetween(message, "||| [", "] ");
      if (name != null) {
         String position = this.substringBetween(contentString, "value='/gps ", "'");
         String lvl = this.substringBetween(message, "Уровень лута: ", "\n ║");
         String owner = this.substringBetween(message, "Призван игроком: ", "\n ║");
         if (position != null) {
            String[] parts = position.split(" ");
            if (parts.length >= 3) {
               try {
                  Vec3d center = Vec3d.ofCenter(new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
                  switch (name) {
                     case "Мистический сундук":
                        this.addEvent(name, lvl, owner, center, "overworld", 300, 0);
                        break;
                     case "Вулкан":
                        this.addEvent(name, lvl, owner, center, "overworld", 300, 120);
                        break;
                     case "Метеоритный дождь":
                     case "Маяк убийца":
                     case "Мистический Алтарь":
                        this.addEvent(name, lvl, owner, center, "overworld", 360, 0);
                        break;
                     case "Загадочный маяк":
                        this.addEvent(name, lvl, owner, center, "overworld", 60, 180);
                  }
               } catch (NumberFormatException var12) {
               }
            }
         } else {
            switch (name) {
               case "Сундук смерти":
                  this.addEvent(name, lvl, owner, Vec3d.ofCenter(new BlockPos(-155, 64, 205)), "lobby", 300, 0);
                  break;
               case "Адская резня":
                  this.addEvent(name, lvl, owner, Vec3d.ofCenter(new BlockPos(48, 87, 73)), "lobby", 180, 120);
            }
         }
      }
   }

   private void processExperienceCooldown(String message) {
      if (message != null && message.contains("Повторно активировать") && message.contains("Пузырь опыта")) {
         String seconds = this.substringBetween(message, "через ", " секунд");
         if (seconds != null && !seconds.isEmpty()) {
            try {
               int duration = Integer.parseInt(seconds) * 20;
               mc.player.getItemCooldownManager().set(Items.EXPERIENCE_BOTTLE.getDefaultStack(), duration);
            } catch (NumberFormatException var4) {
            }
         }
      }
   }

   private void addEvent(String name, String lvl, String owner, Vec3d vec, String world, int timeOpen, int timeLoot) {
      for (ServerHelper.ServerEventData event : this.serverEvents) {
         if (event.vec.equals(vec)) {
            return;
         }
      }

      long open = System.currentTimeMillis() + (long)timeOpen * 1000L;
      long end = open + (long)timeLoot * 1000L;
      this.serverEvents.add(new ServerHelper.ServerEventData(name, lvl, owner, vec, world, Network.getAnarchyMode(), (double)open, (double)end));
   }

   private void addStructure(Item item, Vec3d vec, double time) {
      if (item != Items.DRIED_KELP || !this.hasStructureNear(Items.NETHERITE_SCRAP, vec, 7.0)) {
         if (item == Items.NETHERITE_SCRAP) {
            this.structures
               .removeIf(structurex -> structurex.item == Items.DRIED_KELP && this.sameStructureContext(structurex) && structurex.vec.distanceTo(vec) < 7.0);
         }

         for (ServerHelper.Structure structure : this.structures) {
            if (structure.item == item && this.sameStructureContext(structure) && structure.vec.distanceTo(vec) < 3.0) {
               return;
            }
         }

         this.structures.add(new ServerHelper.Structure(item, vec, this.currentWorldToken(), Network.getAnarchyMode(), time));
      }
   }

   private void addPendingStructure(Item item, BlockPos center, double time) {
      if (center != null) {
         Vec3d vec = Vec3d.ofCenter(center);
         if (!this.hasStructureNear(item, vec, 3.0) && !this.hasPendingStructureNear(item, vec, 3.0)) {
            long now = System.currentTimeMillis();
            this.pendingStructures
               .add(
                  new ServerHelper.PendingStructure(
                     item, center.toImmutable(), this.currentWorldToken(), Network.getAnarchyMode(), time, now + 75L, now + 1000L
                  )
               );
         }
      }
   }

   private boolean hasPendingStructureNear(Item item, Vec3d vec, double distance) {
      for (ServerHelper.PendingStructure structure : this.pendingStructures) {
         if (structure.item == item
            && structure.anarchy == Network.getAnarchyMode()
            && this.matchesWorld(structure.world)
            && Vec3d.ofCenter(structure.center).distanceTo(vec) < distance) {
            return true;
         }
      }

      return false;
   }

   private void validatePendingStructures() {
      if (!this.pendingStructures.isEmpty()) {
         long now = System.currentTimeMillis();
         this.pendingStructures.removeIf(structure -> {
            if (structure.anarchy != Network.getAnarchyMode() || !this.matchesWorld(structure.world) || now > structure.timeoutAt) {
               return true;
            } else if (now < structure.validateAt) {
               return false;
            } else {
               if (this.isRealStructure(structure)) {
                  this.addStructure(structure.item, Vec3d.ofCenter(structure.center), structure.time);
               }

               return true;
            }
         });
      }
   }

   private boolean isRealStructure(ServerHelper.PendingStructure structure) {
      return structure.item == Items.DRIED_KELP ? this.isRealPlast(structure.center) : true;
   }

   private boolean isStructureStillValid(ServerHelper.Structure structure) {
      return structure.item == Items.DRIED_KELP && this.sameStructureContext(structure) ? this.isRealPlast(BlockPos.ofFloored(structure.vec)) : true;
   }

   private boolean hasStructureNear(Item item, Vec3d vec, double distance) {
      for (ServerHelper.Structure structure : this.structures) {
         if (structure.item == item && this.sameStructureContext(structure) && structure.vec.distanceTo(vec) < distance) {
            return true;
         }
      }

      return false;
   }

   private boolean sameStructureContext(ServerHelper.Structure structure) {
      return structure.anarchy == Network.getAnarchyMode() && this.matchesWorld(structure.world);
   }

   private BlockPos findPlastCenter(BlockPos changedPos) {
      ServerHelper.PlastCandidate best = null;

      for (int x = -2; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
               BlockPos center = changedPos.add(x, y, z);
               best = this.bestPlastCandidate(best, new ServerHelper.PlastCandidate(center, this.countSolidPlane(center, ServerHelper.Plane.XY)));
               best = this.bestPlastCandidate(best, new ServerHelper.PlastCandidate(center, this.countSolidPlane(center, ServerHelper.Plane.XZ)));
               best = this.bestPlastCandidate(best, new ServerHelper.PlastCandidate(center, this.countSolidPlane(center, ServerHelper.Plane.YZ)));
            }
         }
      }

      return best != null && best.solidBlocks >= 22 ? best.center : null;
   }

   private ServerHelper.PlastCandidate bestPlastCandidate(ServerHelper.PlastCandidate current, ServerHelper.PlastCandidate candidate) {
      if (candidate != null && candidate.solidBlocks >= 22) {
         return current != null && candidate.solidBlocks <= current.solidBlocks ? current : candidate;
      } else {
         return current;
      }
   }

   private int countSolidPlane(BlockPos center, ServerHelper.Plane plane) {
      int solid = 0;

      for (int a = -2; a <= 2; a++) {
         for (int b = -2; b <= 2; b++) {
            BlockPos pos = switch (plane) {
               case XY -> center.add(a, b, 0);
               case XZ -> center.add(a, 0, b);
               case YZ -> center.add(0, a, b);
            };
            BlockState state = this.blockStateMap.get(pos);
            if (state != null && !state.isAir()) {
               solid++;
            }
         }
      }

      return solid;
   }

   private boolean isRealPlast(BlockPos center) {
      return mc.world != null && center != null
         ? this.isIsolatedWorldPlane(center, ServerHelper.Plane.XY)
            || this.isIsolatedWorldPlane(center, ServerHelper.Plane.XZ)
            || this.isIsolatedWorldPlane(center, ServerHelper.Plane.YZ)
         : false;
   }

   private boolean isIsolatedWorldPlane(BlockPos center, ServerHelper.Plane plane) {
      return this.countWorldSolidPlane(center, plane, 0) >= 22
         && this.countWorldSolidPlane(center, plane, -1) <= 4
         && this.countWorldSolidPlane(center, plane, 1) <= 4;
   }

   private int countWorldSolidPlane(BlockPos center, ServerHelper.Plane plane, int normalOffset) {
      int solid = 0;

      for (int a = -2; a <= 2; a++) {
         for (int b = -2; b <= 2; b++) {
            BlockPos pos = switch (plane) {
               case XY -> center.add(a, b, normalOffset);
               case XZ -> center.add(a, normalOffset, b);
               case YZ -> center.add(normalOffset, a, b);
            };
            if (!mc.world.isChunkLoaded(pos)) {
               return 0;
            }

            BlockState state = mc.world.getBlockState(pos);
            if (state != null && !state.isAir()) {
               solid++;
            }
         }
      }

      return solid;
   }

   private boolean isTrap(BlockPos center) {
      int inconsistencies = 0;

      for (BlockPos pos : PlayerInteractionHelper.getCube(center, 2.0F)) {
         if (Vec3d.ofCenter(pos).distanceTo(Vec3d.ofCenter(center)) < 2.0) {
            BlockState state = this.blockStateMap.get(pos);
            if (state != null && !state.isAir()) {
               inconsistencies++;
            }
         } else if (!pos.equals(center.up(2).north().east())
            && !pos.equals(center.up(2).north().west())
            && !pos.equals(center.up(2).south().east())
            && !pos.equals(center.up(2).south().west())) {
            BlockState state = this.blockStateMap.get(pos);
            if (state == null || state.isAir()) {
               inconsistencies++;
            }
         }

         if (inconsistencies > 1) {
            return false;
         }
      }

      return true;
   }

   private boolean isBigTrap(BlockPos center) {
      int inconsistencies = 0;

      for (BlockPos pos : PlayerInteractionHelper.getCube(center, 3.0F)) {
         if (Math.abs(pos.getX() - center.getX()) <= 2 && Math.abs(pos.getY() - center.getY()) <= 2 && Math.abs(pos.getZ() - center.getZ()) <= 2) {
            BlockState state = this.blockStateMap.get(pos);
            if (state != null && !state.isAir()) {
               inconsistencies++;
            }
         } else if (!pos.equals(center.up(3))) {
            BlockState state = this.blockStateMap.get(pos);
            if (state == null || state.isAir()) {
               inconsistencies++;
            }
         }

         if (inconsistencies > 1) {
            return false;
         }
      }

      return true;
   }

   private void drawLines(DrawContext graphics, List<String> lines, Vec3d screen) {
      int y = (int)screen.y;

      for (String line : lines) {
         graphics.drawText(mc.textRenderer, line, (int)screen.x - mc.textRenderer.getWidth(line) / 2, y, -1, true);
         y += 10;
      }
   }

   private Vec3d projectWorldToScreen(Vec3d worldPos) {
      if (mc.getWindow() == null) {
         return null;
      } else {
         Vec3d relativePos = worldPos.subtract(Render3D.lastCameraPos);
         Matrix4f combined = new Matrix4f(Render3D.lastProjMat).mul(Render3D.lastModMat);
         Vector4f clip = combined.transform(new Vector4f((float)relativePos.x, (float)relativePos.y, (float)relativePos.z, 1.0F));
         if (Float.isFinite(clip.w) && !(clip.w <= 0.01F)) {
            float ndcX = clip.x / clip.w;
            float ndcY = clip.y / clip.w;
            float ndcZ = clip.z / clip.w;
            if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY) || !Float.isFinite(ndcZ)) {
               return null;
            } else if (!(ndcZ < -1.0F) && !(ndcZ > 1.0F)) {
               int width = mc.getWindow().getScaledWidth();
               int height = mc.getWindow().getScaledHeight();
               double x = ((double)ndcX * 0.5 + 0.5) * (double)width;
               double y = (1.0 - ((double)ndcY * 0.5 + 0.5)) * (double)height;
               return !(x < -32.0) && !(x > (double)width + 32.0) && !(y < -32.0) && !(y > (double)height + 32.0) ? new Vec3d(x, y, (double)ndcZ) : null;
            } else {
               return null;
            }
         } else {
            return null;
         }
      }
   }

   private boolean matchesWorld(String world) {
      String current = this.currentWorldToken();
      return world.equals(current) || "lobby".equals(world) && "overworld".equals(current);
   }

   private String currentWorldToken() {
      return mc.world == null ? "unknown" : mc.world.getRegistryKey().getValue().getPath();
   }

   private String cleanText(Text component) {
      return component == null ? "" : this.cleanText(component.getString());
   }

   private String cleanText(String value) {
      if (value != null && !value.isEmpty()) {
         StringBuilder out = new StringBuilder(value.length());
         boolean skip = false;

         for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (skip) {
               skip = false;
            } else if (c == 167) {
               skip = true;
            } else if (c != 194) {
               out.append(c == 160 ? ' ' : Character.toLowerCase(c));
            }
         }

         return out.toString().trim();
      } else {
         return "";
      }
   }

   private String substringBetween(String value, String open, String close) {
      if (value != null && open != null && close != null) {
         int start = value.indexOf(open);
         if (start == -1) {
            return null;
         } else {
            start += open.length();
            int end = value.indexOf(close, start);
            return end == -1 ? null : value.substring(start, end);
         }
      } else {
         return null;
      }
   }

   private String formatSeconds(double seconds) {
      return this.formatDecimal(Math.max(0.0, seconds)) + "с";
   }

   private String formatDecimal(double value) {
      return String.format(Locale.US, "%.1f", value).replace(".0", "");
   }

   private static enum ActionState {
      IDLE,
      WAIT_BEFORE_SWAP,
      WAIT_BEFORE_USE,
      WAIT_BEFORE_RESTORE,
      WAIT_BEFORE_CLOSE;

      private ActionState() {
      }
   }

   private static record EffectRequirement(RegistryEntry<StatusEffect> effect, int minAmplifier) {
   }

   public static record HudBind(String key, Item item, BindSetting setting, ItemStack stack, int inventoryCount, float cooldownProgress, String displayName) {
   }

   public static record HudEvent(String name, String level, String owner, String timer, double distance) {
   }

   private static final class ItemInfo {
      private final Item item;
      private final String displayName;
      private final String nameFallback;
      private final boolean funTimeOnly;
      private final List<String> loreKeywords;
      private final List<ServerHelper.EffectRequirement> effectRequirements;

      private ItemInfo(
         Item item,
         String displayName,
         String nameFallback,
         boolean funTimeOnly,
         List<String> loreKeywords,
         List<ServerHelper.EffectRequirement> effectRequirements
      ) {
         this.item = item;
         this.displayName = displayName;
         this.nameFallback = nameFallback == null ? "" : nameFallback.toLowerCase(Locale.ROOT);
         this.funTimeOnly = funTimeOnly;
         this.loreKeywords = loreKeywords;
         this.effectRequirements = effectRequirements;
      }
   }

   private static record KeyBindingEntry(String key, Item item, BindSetting setting, float distance) {
   }

   private static record PendingStructure(Item item, BlockPos center, String world, int anarchy, double time, long validateAt, long timeoutAt) {
   }

   private static enum Plane {
      XY,
      XZ,
      YZ;

      private Plane() {
      }
   }

   private static record PlastCandidate(BlockPos center, int solidBlocks) {
   }

   private static record ServerEventData(String name, String lvl, String owner, Vec3d vec, String world, int anarchy, double timeOpen, double timeEnd) {
   }

   private static record Structure(Item item, Vec3d vec, String world, int anarchy, double time) {
   }
}
