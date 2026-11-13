package daamky.ua.api.module.impl.combat;

import daamky.gui.GuiStyle;
import java.util.HashMap;
import daamky.ua.api.module.impl.combat.aura.rotations.SpookyTime3Angle;
import java.util.List;
import java.util.Map;
import daamky.ua.api.module.impl.combat.aura.AuraConfig;
import daamky.ua.api.settings.impl.ButtonSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.RaycastContext;
import daamky.ua.api.module.impl.combat.aura.rotations.SpookyTimeAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.SpookyTime2Angle;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.events.impl.InputEvent;
import daamky.ua.api.events.impl.MouseAppliedRotationEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.events.types.EventPhase;
import daamky.ua.api.events.types.EventPriority;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.attack.StrikeManager;
import daamky.ua.api.module.impl.combat.aura.attack.StrikerConstructor;
import daamky.ua.api.module.impl.combat.aura.back.TimedBackAngle;
import daamky.ua.api.module.impl.combat.aura.context.AutoRegressionContext;
import daamky.ua.api.module.impl.combat.aura.impl.LinearConstructor;
import daamky.ua.api.module.impl.combat.aura.impl.RotateConstructor;
import daamky.ua.api.module.impl.combat.aura.rotations.Ai;
import daamky.ua.api.module.impl.combat.aura.rotations.AiNew;
import daamky.ua.api.module.impl.combat.aura.rotations.AimAssistAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.FTAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.FTProfileAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.FunTimeSnapAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.HWangle;
import daamky.ua.api.module.impl.combat.aura.rotations.LegitAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.LegitSnap;
import daamky.ua.api.module.impl.combat.aura.rotations.MLSAC;
import daamky.ua.api.module.impl.combat.aura.rotations.Matrix;
import daamky.ua.api.module.impl.combat.aura.rotations.Neuro;
import daamky.ua.api.module.impl.combat.aura.rotations.NeuroAimPointFeatures;
import daamky.ua.api.module.impl.combat.aura.rotations.NeuroHumanDatasetLogger;
import daamky.ua.api.module.impl.combat.aura.rotations.NeuroTrainingCollector;
import daamky.ua.api.module.impl.combat.aura.rotations.ReallyWorld;
import daamky.ua.api.module.impl.combat.aura.rotations.SPAngle;
import daamky.ua.api.module.impl.combat.aura.rotations.Sloth;
import daamky.ua.api.module.impl.combat.aura.rotations.SnapAngle;
import daamky.ua.api.module.impl.combat.aura.target.MultiPoint;
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.target.TargetFinder;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.module.impl.movement.ElytraTarget;
import daamky.ua.api.module.impl.movement.InventoryMove;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.screens.modernui.ClickGuiScreen;
import daamky.ua.utils.modules.warden.rotation.RotationController;
import daamky.ua.utils.player.BaritoneMovementHelper;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.string.chat.ChatMessage;

public class AuraModule extends Module {
   private static final int HUMAN_INPUT_ATTACK = 1;
   private static final int HUMAN_INPUT_USE = 2;
   private static final int HUMAN_INPUT_PICK = 4;
   private static final int HUMAN_INPUT_FORWARD = 8;
   private static final int HUMAN_INPUT_BACK = 16;
   private static final int HUMAN_INPUT_LEFT = 32;
   private static final int HUMAN_INPUT_RIGHT = 64;
   private static final int HUMAN_INPUT_JUMP = 128;
   private static final int HUMAN_INPUT_SNEAK = 256;
   private static final int HUMAN_INPUT_SPRINT = 512;
   private static final int HUMAN_INPUT_MOUSE_LEFT = 1024;
   private static final int HUMAN_INPUT_MOUSE_RIGHT = 2048;
   private static final int HUMAN_INPUT_MOUSE_MIDDLE = 4096;
   private static AuraModule instance;
   public static LivingEntity target;
   private final TargetFinder targetSelector = new TargetFinder();
   private final MultiPoint pointFinder = new MultiPoint();
   private final StrikerConstructor attackPerpetrator = new StrikerConstructor();

   public final ModeSetting presetSelector = this.register(
      new ModeSetting(
         "Preset", "Ready-made Aura configs", "Not Selected",
         "Not Selected", "SpookyTime", "FunTime", "ReallyWorld", "HypeMC"
      )
   );
   public final ButtonSetting applyPresetButton = this.register(
      new ButtonSetting("Load Config", "Applies the selected preset to Aura only", this::applySelectedPreset)
   );

   public final ModeSetting mode = this.register(
      new ModeSetting(
         "Mode", "Aura rotation mode.", "MatrixNew", "MatrixNew", "Snap", "Legit", "Legit Snap", "FunTime", "SpookyTime", "SpookyTime2", "SpookyTime3", "ReallyWorld", "Byram", "HolyWorld"
      )
   );
   public final NumberSetting timeOnTick = this.register(new NumberSetting("Snap Hold", "", 50.0, 1.0, 300.0, 1.0));
   public final NumberSetting speedValue = this.register(new NumberSetting("Snap Speed", "", 50.0, 15.0, 180.0, 1.0));
   public final NumberSetting legitSpeed = this.register(new NumberSetting("Legit Speed", "Legit yaw blend speed.", 0.08, 0.02, 0.4, 0.01));
   public final NumberSetting aimAssistSpeed = new NumberSetting("AimAssist Speed", "", 14.0, 0.5, 20.0, 0.1);
   public final NumberSetting aimAssistSmoothness = new NumberSetting("AimAssist Smoothness", "", 6.0, 1.0, 10.0, 0.1);
   public final BooleanSetting neuroDatasetCapture = this.register(new BooleanSetting("Neuro Dataset", "", false));
   public final ModeSetting neuroDatasetFormat = this.register(new ModeSetting("Neuro Format", "", "JSONL", "JSONL", "CSV"));
   public final BooleanSetting neuroHumanDatasetCapture = this.register(new BooleanSetting("Neuro Human Dataset", "", false));
   public final ModeSetting neuroHumanDatasetFormat = this.register(new ModeSetting("Neuro Human Format", "", "JSONL", "JSONL", "CSV"));
   public final BooleanSetting neuroModelInference = this.register(new BooleanSetting("Neuro Inference", "", false));
   public final StringSetting neuroModelPath = this.register(new StringSetting("Neuro Model", "", "daamky/models/neuro_reward_model.json", 256));
   public final NumberSetting neuroModelBlend = this.register(new NumberSetting("Neuro Blend", "", 0.65, 0.0, 1.0, 0.05));
   public final BooleanSetting neuroHumanInference = this.register(new BooleanSetting("Neuro Human Inference", "", false));
   public final StringSetting neuroHumanModelPath = this.register(new StringSetting("Neuro Human Model", "", "daamky/models/neuro_human_model.json", 256));
   public final NumberSetting neuroHumanBlend = this.register(new NumberSetting("Neuro Human Blend", "", 0.75, 0.0, 1.0, 0.05));
   public final NumberSetting attackRange = this.register(new NumberSetting("Range", "", 3.0, 2.5, 6.0, 0.1));
   public final NumberSetting lookRange = this.register(new NumberSetting("Look Range", "", 1.5, 0.5, 10.0, 0.1));
   public final NumberSetting keepTargetRange = this.register(new NumberSetting("Keep Range", "", 6.0, 2.5, 12.0, 0.1));
   public final MultiModeSetting targetType = this.register(
      new MultiModeSetting("Targets", "", new String[]{"Players", "Friends", "Mobs", "Animals", "Armor Stands", "Naked Players"}, "Players", "Invisible")
   );
   public final ModeSetting targetPriority = this.register(new ModeSetting("Priority", "", "Distance", "Distance", "Health", "Armor", "FOV", "Combined"));
   public final MultiModeSetting options = this.register(
      new MultiModeSetting(
         "Options",
         "",
         new String[]{"Pause While Using", "Ignore Walls", "Release Shield", "Sync TPS", "Hit in container"},
         "Pause While Using",
         "Multi Points"
      )
   );
   public final ModeSetting moveFix = this.register(new ModeSetting("Move Fix", "", "Focused", "Focused", "Free", "Chase", "Target"));
   public final BooleanSetting onlyCriticals = this.register(new BooleanSetting("Only Crits", "", true));
   public final BooleanSetting smartCriticals = this.register(new BooleanSetting("Smart Crits", "", false));
   public final BooleanSetting criticalBalance = this.register(
      new BooleanSetting("Critical Balance", "Uses a stricter randomized attack cooldown for critical hits.", false)
   );
   public final ModeSetting hitMoment = this.register(new ModeSetting("Hit Moment", "Critical fall-distance timing.", "Legit", "Static", "Legit", "Random"));
   public final NumberSetting randomFallMin = this.register(new NumberSetting("Random Fall Min", "", 0.0, 0.0, 1.5, 0.01));
   public final NumberSetting randomFallMax = this.register(new NumberSetting("Random Fall Max", "", 0.25, 0.0, 1.5, 0.01));
   public final NumberSetting staticFall = this.register(new NumberSetting("Static Fall", "", 0.0, 0.0, 1.5, 0.01));
   public final BooleanSetting maxDamage = this.register(new BooleanSetting("Max Damage", "Temporarily equips a damage talisman for Aura attacks.", false));
   private final ModeSetting firstItem = this.register(new ModeSetting("First Item", "First offhand item.", "Totem", "Totem", "Head", "GApple", "Shield"));
   private final ModeSetting secondItem = this.register(new ModeSetting("Second Item", "Second offhand item.", "Totem", "Totem", "Head", "GApple", "Shield"));
   public final BooleanSetting legitfov = this.register(new BooleanSetting("Draw Circle", "", true));
   private final LegitAngle legitAngle = new LegitAngle();
   private float cachedDynamicFov = 33.0F;
   private LivingEntity lastTarget;
   private long activationTimeMs;
   private long humanDatasetSessionStartMs;
   private float tps = 20.0F;
   private float adjustTicks;
   private long timestamp;
   private int reducedHitboxAttackCounter;
   private boolean wasForwardPressed;
   private boolean wasBackPressed;
   private boolean wasLeftPressed;
   private boolean wasRightPressed;
   private boolean wasJumpPressed;
   private boolean keysOverridden;
   private boolean inventoryOpened;
   private boolean packetsHeld;
   private final Map<Integer, Integer> observedTargetHurtTime = new HashMap<>();
   private boolean hasHumanRotationBaseline;
   private float previousHumanYaw;
   private float previousHumanPitch;
   private double accumulatedHumanMouseDx;
   private double accumulatedHumanMouseDy;
   private boolean humanMouseMovedThisTick;
   private long lastHumanDatasetNoticeSamples;
   private boolean hasHumanInputBaseline;
   private int previousHumanInputMask;

   public AuraModule() {
      super("Aura", "Automatically attacks nearby targets.", ModuleCategory.COMBAT);
      this.timeOnTick.visibleWhen(() -> this.mode.is("Snap"));
      this.speedValue.visibleWhen(() -> this.mode.is("Snap"));
      this.legitSpeed.visibleWhen(() -> this.mode.is("Legit"));
      this.neuroDatasetCapture.visibleWhen(() -> this.mode.is("Neuro"));
      this.neuroDatasetFormat.visibleWhen(() -> this.mode.is("Neuro") && this.neuroDatasetCapture.getValue());
      this.neuroHumanDatasetCapture.visibleWhen(() -> this.mode.is("Neuro"));
      this.neuroHumanDatasetFormat.visibleWhen(() -> this.mode.is("Neuro") && this.neuroHumanDatasetCapture.getValue());
      this.neuroModelInference.visibleWhen(() -> this.mode.is("Neuro"));
      this.neuroModelPath.visibleWhen(() -> this.mode.is("Neuro") && this.neuroModelInference.getValue());
      this.neuroModelBlend.visibleWhen(() -> this.mode.is("Neuro") && this.neuroModelInference.getValue());
      this.neuroHumanInference.visibleWhen(() -> this.mode.is("Neuro"));
      this.neuroHumanModelPath.visibleWhen(() -> this.mode.is("Neuro") && this.neuroHumanInference.getValue());
      this.neuroHumanBlend.visibleWhen(() -> this.mode.is("Neuro") && this.neuroHumanInference.getValue());
      this.mode.addListener((setting, oldValue, newValue) -> this.syncSpookyMoveFix());
      this.moveFix.addListener((setting, oldValue, newValue) -> this.syncSpookyMoveFix());
      this.syncSpookyMoveFix();
      this.smartCriticals.visibleWhen(this.onlyCriticals::getValue);
      this.criticalBalance.visibleWhen(this.onlyCriticals::getValue);
      this.hitMoment.visibleWhen(this.onlyCriticals::getValue);
      this.randomFallMin.visibleWhen(() -> this.onlyCriticals.getValue() && this.hitMoment.is("Random"));
      this.randomFallMax.visibleWhen(() -> this.onlyCriticals.getValue() && this.hitMoment.is("Random"));
      this.staticFall.visibleWhen(() -> this.onlyCriticals.getValue() && this.hitMoment.is("Static"));
      this.maxDamage.visibleWhen(this::supportsMaxDamage);
      this.firstItem.visibleWhen(() -> this.maxDamage.getValue());
      this.secondItem.visibleWhen(() -> this.maxDamage.getValue());
      this.legitfov.visibleWhen(() -> this.mode.is("Legit Snap"));
      instance = this;
   }

   private void applySelectedPreset() {
      String selected = this.presetSelector.getValue();
      switch (selected) {
         case "SpookyTime" -> AuraConfig.applySpookyTime(this);
         case "FunTime" -> AuraConfig.applyFunTime(this);
         case "ReallyWorld" -> AuraConfig.applyReallyWorld(this);
         case "HypeMC" -> AuraConfig.applyHypeMC(this);
         default -> ChatMessage.brandmessage("Aura preset: Not Selected");
      }
   }

   public boolean shouldUseMaxDamage() {
      return this.maxDamage.getValue() && this.supportsMaxDamage();
   }

   public String getMaxDamageFirstItem() {
      return this.firstItem.getValue();
   }

   public String getMaxDamageSecondItem() {
      return this.secondItem.getValue();
   }

   private boolean supportsMaxDamage() {
      return this.mode.is("SpookyTime")
         || this.mode.is("FunTime")
         || this.mode.is("Byram")
         || this.mode.is("ReallyWorld")
         || this.mode.is("Legit Snap")
         || this.mode.is("Snap")
         || this.mode.is("MatrixNew");
   }

   private void syncSpookyMoveFix() {
   }

   public static AuraModule getInstance() {
      return instance;
   }

   public static float activeTps() {
      return instance == null ? 20.0F : instance.tps;
   }

   @Override
   protected void onEnable() {
      this.activationTimeMs = System.currentTimeMillis();
      this.humanDatasetSessionStartMs = 0L;
      this.timestamp = System.nanoTime();
      this.tps = 20.0F;
      this.adjustTicks = 0.0F;
      this.reducedHitboxAttackCounter = 0;
      this.observedTargetHurtTime.clear();
      this.resetHumanDatasetTracking();
      NeuroTrainingCollector.getInstance().reset();
      AutoRegressionContext context = AutoRegressionContext.getInstance();
      context.setCdMinecraft(83L);
      context.hitContentClear();
   }

   @Override
   protected void onDisable() {
      if (RotationController.isRotating()) {
         RotationController.hardReset();
      }

      if (this.mode.is("AimAssist")) {
         AimAssistAngle.INSTANCE.reset();
         AngleConnection.INSTANCE.restoreVanillaLook();
      } else {
         this.activationTimeMs = 0L;
         this.humanDatasetSessionStartMs = 0L;
         this.timestamp = 0L;
         this.tps = 20.0F;
         this.adjustTicks = 0.0F;
         this.reducedHitboxAttackCounter = 0;
         this.observedTargetHurtTime.clear();
         this.resetHumanDatasetTracking();
         this.targetSelector.releaseTarget();
         target = null;
         this.lastTarget = null;
         this.attackPerpetrator.getAttackHandler().resetPendingState();
         NeuroTrainingCollector.getInstance().reset();
         NeuroHumanDatasetLogger.getInstance().flush();
         AutoRegressionContext.getInstance().hitContentClear();
         this.lastTarget = null;
         this.legitAngle.reset();
         this.finishRotationOnRelease(MinecraftClient.getInstance());
      }
   }

   @SubscribeEvent(
      priority = EventPriority.MONITOR
   )
   public void onDraw(DrawEvent e) {
      if (e.getLayer() == DrawEvent.Layer.GAME && this.isEnabled() && mc.player != null && mc.world != null) {
         if (this.mode.is("Legit") && target != null && target.isAlive()) {
            Angle next = this.legitAngle.stepFrame(target);
            mc.player.setYaw(next.getYaw());
            mc.player.headYaw = next.getYaw();
         }

         if (this.usesFovCircle()) {
            if (mc.options.getPerspective() == Perspective.FIRST_PERSON && mc.currentScreen == null && mc.getWindow() != null) {
               int width = mc.getWindow().getScaledWidth();
               int height = mc.getWindow().getScaledHeight();
               float radius = this.calculateFovRadius(width, height);
               if (!(radius < 1.0F)) {
                  float diameter = radius * 2.0F;
                  int color = target != null ? ColorUtil.rgba(127, 242, 255, 165) : ColorUtil.rgba(255, 255, 255, 95);
                  if (this.legitfov.getValue()) {
                     Render2D.outline360(
                        (float)width * 0.5F - radius,
                        (float)height * 0.5F - radius,
                        diameter,
                        diameter,
                        radius,
                        1.2F,
                        color,
                        Render2D.outline360Range(0.0F, 360.0F, color)
                     );
                  }
               }
            }
         }
      }
   }

   private float calculateFovRadius(int width, int height) {
      float maxRadius = Math.max(0.0F, (float)Math.min(width, height) * 0.5F - 2.0F);
      if (maxRadius <= 0.0F) {
         return 0.0F;
      } else {
         double aimFov = (double)this.cachedDynamicFov;
         double gameFov = MathHelper.clamp((double)((Integer)mc.options.getFov().getValue()).intValue(), 40.0, 170.0);
         double gameHalf = MathHelper.clamp(gameFov * 0.5, 1.0, 89.9);
         if (aimFov >= gameHalf) {
            return maxRadius;
         } else {
            double aimHalf = MathHelper.clamp(aimFov, 0.1, 89.9);
            double radius = Math.tan(Math.toRadians(aimHalf)) / Math.tan(Math.toRadians(gameHalf)) * (double)Math.min(width, height) * 0.5;
            return (float)MathHelper.clamp(radius, 0.0, (double)maxRadius);
         }
      }
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client == null || client.player == null || client.world == null) {
         this.targetSelector.releaseTarget();
         target = null;
         RotationController.hardReset();
         this.legitAngle.reset();
         AngleConnection.INSTANCE.restoreVanillaLook();
      }
   }

   @SubscribeEvent(
      priority = EventPriority.HIGHEST
   )
   private void onPreTick(TickEvent.Pre event) {
      MinecraftClient client = event.getClient();
      if (client != null && client.player != null && client.world != null) {
         if (this.mode.is("Neuro") && this.neuroDatasetCapture.getValue()) {
            NeuroTrainingCollector.getInstance().tick(client);
         } else {
            NeuroTrainingCollector.getInstance().reset();
         }

         if (!this.isHumanDatasetMode()) {
            this.humanDatasetSessionStartMs = 0L;
            this.resetHumanDatasetTracking();
         }

         this.attackPerpetrator.tick();
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      this.attackPerpetrator.onPacket(event);
      if (this.isSyncWithTpsEnabled() && event.isReceive() && event.getPacket() instanceof WorldTimeUpdateS2CPacket) {
         long currentTime = System.nanoTime();
         if (this.timestamp == 0L) {
            this.timestamp = currentTime;
         } else {
            long delay = currentTime - this.timestamp;
            if (delay <= 0L) {
               this.timestamp = currentTime;
            } else {
               float boundedTPS = MathHelper.clamp(20.0F * (1.0E9F / (float)delay), 0.0F, 20.0F);
               this.tps = (float)this.limitDecimals((double)boundedTPS, 2);
               this.adjustTicks = boundedTPS - 20.0F;
               this.timestamp = currentTime;
            }
         }
      }
   }

   @SubscribeEvent
   private void onInput(InputEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.isEnabled() && client.player != null && client.world != null && event.getInput() != null) {
         if (!this.isHumanDatasetMode()) {
            if (!BaritoneMovementHelper.isBaritoneActive(client.player)) {
               InventoryMove inventoryMove = InventoryMove.getInstance();
               if (inventoryMove == null || !inventoryMove.shouldSuppressSprintInput()) {
                  PlayerInput input = event.getInput();
                  if (target != null && target.isAlive()) {
                     boolean inWater = client.player.isTouchingWater() || client.player.isSubmergedInWater();
                     StrikeManager attackHandler = this.getAttackHandlerRaw();
                     attackHandler.syncShieldState();
                     boolean targetFix = this.moveFix.is("Target");
                     boolean chaseFix = this.moveFix.is("Chase");
                     boolean focusedFixSelected = this.moveFix.is("Focused");
                     StrikerConstructor.AttackPerpetratorConfigurable config = this.getConfig();
                     float targetDistance = client.player.distanceTo(target);
                     boolean shouldPrepareAttack = attackHandler.canAttack(config, 1) && targetDistance <= this.attackDistance() && !inWater;
                     boolean hitInContainer = this.options.isSelected("Hit in container");
                     boolean containerScreen = client.currentScreen instanceof InventoryScreen || client.currentScreen instanceof ClickGuiScreen || GuiStyle.isClientGui(client.currentScreen);
                     if (client.currentScreen == null || !containerScreen || hitInContainer) {
                        if (focusedFixSelected && shouldPrepareAttack) {
                           event.setDirectionalLow(false, false, false, false);
                        }

                        if (!focusedFixSelected && shouldPrepareAttack) {
                           event.setSprinting(false);
                           client.player.setSprinting(false);
                        }

                        if (target != null
                           && attackHandler.canAttack(config, 1)
                           && client.player.distanceTo(target) <= this.attackDistance()
                           && !client.player.isSwimming()) {
                           event.setDirectionalLow(false, false, false, false);
                        }

                        if (!focusedFixSelected && !targetFix && !chaseFix) {
                           PlayerInput patchedInput = event.getInput();
                           event.setInput(
                              new PlayerInput(
                                 input.forward(), input.backward(), input.left(), input.right(), input.jump(), input.sneak(), patchedInput.sprint()
                              )
                           );
                        }

                        if (focusedFixSelected && shouldPrepareAttack) {
                           event.setDirectionalLow(false, false, false, false);
                        }

                        boolean w = client.options.forwardKey.isPressed();
                        boolean s = client.options.backKey.isPressed();
                        boolean a = client.options.leftKey.isPressed();
                        boolean d = client.options.rightKey.isPressed();
                        if (!inWater) {
                           if (!targetFix
                              || !client.options.forwardKey.isPressed()
                                 && !client.options.backKey.isPressed()
                                 && !client.options.leftKey.isPressed()
                                 && !client.options.rightKey.isPressed()) {
                              if (chaseFix) {
                                 if (w || s || a || d) {
                                    Vec3d playerPos = client.player.getEntityPos();
                                    Box targetBox = target.getBoundingBox();
                                    Vec3d center = targetBox.getCenter();
                                    float targetYaw = target.getYaw();
                                    double rad = Math.toRadians((double)targetYaw);
                                    Vec3d forwardDir = new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad)).normalize();
                                    Vec3d rightDir = new Vec3d(-forwardDir.z, 0.0, forwardDir.x).normalize();
                                    Vec3d leftDir = rightDir.multiply(-1.0);
                                    double offset = (double)target.getWidth() / 2.0 + 0.1;
                                    Vec3d offsetVec = Vec3d.ZERO;
                                    if (w) {
                                       offsetVec = offsetVec.add(forwardDir);
                                    }

                                    if (s) {
                                       offsetVec = offsetVec.add(forwardDir.multiply(-1.0));
                                    }

                                    if (a) {
                                       offsetVec = offsetVec.add(leftDir);
                                    }

                                    if (d) {
                                       offsetVec = offsetVec.add(rightDir);
                                    }

                                    Vec3d moveTargetVec = center;
                                    if (offsetVec.lengthSquared() > 0.0) {
                                       moveTargetVec = center.add(offsetVec.normalize().multiply(offset));
                                    }

                                    this.moveToward(event, playerPos, moveTargetVec, AngleConnection.INSTANCE.getRotation().getYaw());
                                 }
                              }
                           } else {
                              this.moveToward(event, client.player.getEntityPos(), target.getEntityPos(), AngleConnection.INSTANCE.getRotation().getYaw());
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onRotationUpdate(RotationUpdateEvent event) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (this.isEnabled() && client.player != null && client.world != null) {
         if (event.getPhase() == EventPhase.PRE) {
            if (this.mode.is("HolyWorld")) {
               RotationController.tick();
            } else if (RotationController.isRotating()) {
               RotationController.hardReset();
            }
         }

         if (BaritoneMovementHelper.isBaritoneActive(client.player)) {
            this.targetSelector.releaseTarget();
            target = null;
            this.lastTarget = null;
            RotationController.hardReset();
         } else if (event.getPhase() == EventPhase.PRE) {
            target = this.updateTarget();
            if (this.isHumanDatasetMode()) {
               this.captureHumanRotationSample(client, target);
               this.lastTarget = target;
            } else {
               if (target != null && this.isTargetInsideFov(target)) {
                  this.rotateToTarget(this.getConfig());
                  this.lastTarget = target;
               } else {
                  if (target != null) {
                     this.targetSelector.releaseTarget();
                     target = null;
                  }

                  if (this.lastTarget == null) {
                     return;
                  }

                  this.finishRotationOnRelease(client);
                  this.lastTarget = null;
               }
            }
         } else {
            if (event.getPhase() == EventPhase.POST && target != null && this.isTargetInsideFov(target)) {
               if (this.mode.is("")) {
                  AngleConnection.INSTANCE.applyPlayerRotation(client);
               }

               this.performAuraAttack(this.getConfig());
            }
         }
      }
   }

   @SubscribeEvent
   private void onMouseAppliedRotation(MouseAppliedRotationEvent event) {
      if (this.isHumanDatasetMode()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.world != null && client.currentScreen == null) {
            if (!(Math.abs(event.getCursorDeltaX()) < 1.0E-5F)
               || !(Math.abs(event.getCursorDeltaY()) < 1.0E-5F)
               || !(Math.abs(event.getAfterYaw() - event.getBeforeYaw()) < 1.0E-5F)
               || !(Math.abs(event.getAfterPitch() - event.getBeforePitch()) < 1.0E-5F)) {
               this.accumulatedHumanMouseDx = this.accumulatedHumanMouseDx + (double)event.getCursorDeltaX();
               this.accumulatedHumanMouseDy = this.accumulatedHumanMouseDy + (double)event.getCursorDeltaY();
               this.humanMouseMovedThisTick = true;
            }
         }
      }
   }

   private void captureHumanRotationSample(MinecraftClient client, LivingEntity activeTarget) {
      if (client == null || client.player == null || client.world == null || client.currentScreen != null) {
         this.resetHumanDatasetTracking();
      } else if (activeTarget != null && activeTarget.isAlive()) {
         float currentYaw = client.player.getYaw();
         float currentPitch = client.player.getPitch();
         if (this.hasHumanRotationBaseline && this.lastTarget == activeTarget) {
            double appliedYawStep = (double)MathHelper.wrapDegrees(currentYaw - this.previousHumanYaw);
            double appliedPitchStep = (double)(currentPitch - this.previousHumanPitch);
            boolean rotationChanged = Math.abs(appliedYawStep) >= 1.0E-5 || Math.abs(appliedPitchStep) >= 1.0E-5;
            int inputMask = this.computeHumanInputMask(client);
            boolean inputChanged = !this.hasHumanInputBaseline || inputMask != this.previousHumanInputMask;
            boolean inputActive = inputMask != 0;
            if (!rotationChanged && !this.humanMouseMovedThisTick && !inputChanged && !inputActive) {
               this.previousHumanYaw = currentYaw;
               this.previousHumanPitch = currentPitch;
            } else {
               StrikerConstructor.AttackPerpetratorConfigurable config = this.getConfig();
               Angle targetAngle = config != null ? config.getAngle() : null;
               if (targetAngle == null) {
                  this.previousHumanYaw = currentYaw;
                  this.previousHumanPitch = currentPitch;
                  this.accumulatedHumanMouseDx = 0.0;
                  this.accumulatedHumanMouseDy = 0.0;
                  this.humanMouseMovedThisTick = false;
               } else {
                  Angle currentAngle = new Angle(this.previousHumanYaw, this.previousHumanPitch);
                  Angle delta = MathAngle.calculateDelta(currentAngle, targetAngle);
                  double yawDelta = (double)delta.getYaw();
                  double pitchDelta = (double)delta.getPitch();
                  double deltaLength = Math.hypot(yawDelta, pitchDelta);
                  double distance = (double)client.player.distanceTo(activeTarget);
                  double holdDistance = Math.max(0.001, (double)this.finalDistance());
                  double distanceRatio = MathHelper.clamp(distance / holdDistance, 0.0, 1.0);
                  double targetMotion = activeTarget.getVelocity().length();
                  double targetSpeed = activeTarget.getVelocity().horizontalLength();
                  StrikeManager attackHandler = this.getAttackHandlerRaw();
                  boolean canAttackNow = attackHandler != null && attackHandler.canAttack(config, 0);
                  boolean canAttackSoon = attackHandler != null && attackHandler.canAttack(config, 1);
                  boolean raytraceSuccess = config.getBox() != null && RaycastAngle.rayTrace((double)this.attackDistance(), config.getBox());
                  double attackCooldown = (double)client.player.getAttackCooldownProgress(0.0F);
                  int currentHurtTime = activeTarget.hurtTime;
                  int previousObservedHurtTime = this.observedTargetHurtTime.getOrDefault(activeTarget.getId(), currentHurtTime);
                  boolean registeredHit = currentHurtTime > previousObservedHurtTime;
                  this.observedTargetHurtTime.put(activeTarget.getId(), currentHurtTime);
                  double hitChance = this.computeHumanHitChance(
                     deltaLength, distanceRatio, targetSpeed, canAttackNow, canAttackSoon, raytraceSuccess, attackCooldown
                  );
                  long sampleTimeMs = System.currentTimeMillis();
                  if (this.humanDatasetSessionStartMs == 0L) {
                     this.humanDatasetSessionStartMs = sampleTimeMs;
                  }

                  long sessionElapsedMs = Math.max(0L, sampleTimeMs - this.humanDatasetSessionStartMs);
                  NeuroAimPointFeatures aimPointFeatures = NeuroAimPointFeatures.from(
                     activeTarget, activeTarget.getBoundingBox(), config.getAimPoint(), config.isMultiPoints()
                  );
                  long savedSamples = NeuroHumanDatasetLogger.getInstance()
                     .logSample(
                        new NeuroHumanDatasetLogger.Sample(
                           sampleTimeMs,
                           sessionElapsedMs,
                           this.formatSessionClock(sessionElapsedMs),
                           activeTarget.getId(),
                           activeTarget.getUuid().toString(),
                           activeTarget.getType().toString(),
                           (double)this.previousHumanYaw,
                           (double)this.previousHumanPitch,
                           (double)currentYaw,
                           (double)currentPitch,
                           (double)targetAngle.getYaw(),
                           (double)targetAngle.getPitch(),
                           yawDelta,
                           pitchDelta,
                           deltaLength,
                           distance,
                           distanceRatio,
                           targetMotion,
                           targetSpeed,
                           this.accumulatedHumanMouseDx,
                           this.accumulatedHumanMouseDy,
                           appliedYawStep,
                           appliedPitchStep,
                           aimPointFeatures.multiPointEnabled(),
                           aimPointFeatures.aimOffsetX(),
                           aimPointFeatures.aimOffsetY(),
                           aimPointFeatures.aimOffsetZ(),
                           aimPointFeatures.aimOffsetLength(),
                           canAttackNow,
                           canAttackSoon,
                           raytraceSuccess,
                           attackCooldown,
                           hitChance,
                           this.hasHumanInput(inputMask, 1),
                           this.hasHumanInput(inputMask, 2),
                           this.hasHumanInput(inputMask, 4),
                           this.hasHumanInput(inputMask, 8),
                           this.hasHumanInput(inputMask, 16),
                           this.hasHumanInput(inputMask, 32),
                           this.hasHumanInput(inputMask, 64),
                           this.hasHumanInput(inputMask, 128),
                           this.hasHumanInput(inputMask, 256),
                           this.hasHumanInput(inputMask, 512),
                           this.hasHumanInput(inputMask, 1024),
                           this.hasHumanInput(inputMask, 2048),
                           this.hasHumanInput(inputMask, 4096),
                           inputMask,
                           inputChanged,
                           currentHurtTime > 0,
                           currentHurtTime,
                           registeredHit
                        ),
                        this.getNeuroHumanDatasetFormat().getValue()
                     );
                  this.notifyHumanDatasetProgress(savedSamples, sessionElapsedMs);
                  this.previousHumanYaw = currentYaw;
                  this.previousHumanPitch = currentPitch;
                  this.previousHumanInputMask = inputMask;
                  this.hasHumanInputBaseline = true;
                  this.accumulatedHumanMouseDx = 0.0;
                  this.accumulatedHumanMouseDy = 0.0;
                  this.humanMouseMovedThisTick = false;
               }
            }
         } else {
            this.previousHumanYaw = currentYaw;
            this.previousHumanPitch = currentPitch;
            this.hasHumanRotationBaseline = true;
            this.accumulatedHumanMouseDx = 0.0;
            this.accumulatedHumanMouseDy = 0.0;
            this.humanMouseMovedThisTick = false;
            this.previousHumanInputMask = this.computeHumanInputMask(client);
            this.hasHumanInputBaseline = true;
         }
      } else {
         this.hasHumanRotationBaseline = false;
         this.hasHumanInputBaseline = false;
         this.previousHumanInputMask = 0;
         this.accumulatedHumanMouseDx = 0.0;
         this.accumulatedHumanMouseDy = 0.0;
         this.humanMouseMovedThisTick = false;
      }
   }

   public float finalDistance() {
      ElytraTarget elytraTarget = ElytraTarget.getInstance();
      MinecraftClient client = MinecraftClient.getInstance();
      return client.player != null && client.player.isGliding() && elytraTarget != null && elytraTarget.isEnabled()
         ? elytraTarget.getRange()
         : this.attackRange.getFloat() + this.lookRange.getFloat();
   }

   public float attackDistance() {
      return this.attackRange.getFloat();
   }

   public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (target != null && client.player != null) {
         Pair<Vec3d, Box> point = this.pointFinder
            .computeVector(
               target,
               this.attackDistance(),
               AngleConnection.INSTANCE.getRotation(),
               this.getSmoothMode().randomValue(),
               this.options.isSelected("Ignore Walls")
            );
         Vec3d computedPoint = (Vec3d)point.getLeft();
         Box box = this.adjustAttackBox((Box)point.getRight());
         
         // ИЗМЕНЕННЫЙ БЛОК ДЛЯ ПОЛЕТА НА ЭЛИТРЕ
         if (client.player.isGliding()) {
            float leadTicks = 0.0F;
            ElytraTarget elytraTarget = ElytraTarget.getInstance();
            if (ElytraTarget.shouldElytraTarget && elytraTarget != null && elytraTarget.isEnabled()) {
               leadTicks = elytraTarget.getForward();
            }

            Vec3d eye = client.player.getEyePos();
            Vec3d enemy = target.getBoundingBox().getCenter();
            double dx = enemy.x - eye.x;
            double dz = enemy.z - eye.z;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            double nx = horizontal == 0.0d ? 0.0d : dx / horizontal;
            double nz = horizontal == 0.0d ? 0.0d : dz / horizontal;

            double distanceToGround = 2.0; // Default fallback
            BlockHitResult raycast = client.world.raycast(new RaycastContext(
                client.player.getEntityPos(), client.player.getEntityPos().subtract(0, 2, 0), 
                RaycastContext.ShapeType.COLLIDER, 
                RaycastContext.FluidHandling.NONE, client.player
            ));
            if (raycast.getType() != HitResult.Type.MISS) {
                distanceToGround = client.player.getEntityPos().y - raycast.getPos().y;
            }
            double lift = Math.max(0.0d, 3.0d - distanceToGround);

            Vec3d targetVelocity = target.getVelocity();
            Vec3d predictedTargetPos = target.getEntityPos().add(targetVelocity.multiply((double)leadTicks));
            
            double forwardOffset = leadTicks > 0 ? leadTicks : 4.0d;
            
            computedPoint = new Vec3d(predictedTargetPos.x + (nx * forwardOffset), predictedTargetPos.y + lift, predictedTargetPos.z + (nz * forwardOffset));
            
            box = this.adjustAttackBox(new Box(
                predictedTargetPos.x - (double)target.getWidth() / 2.0,
                predictedTargetPos.y,
                predictedTargetPos.z - (double)target.getWidth() / 2.0,
                predictedTargetPos.x + (double)target.getWidth() / 2.0,
                predictedTargetPos.y + (double)target.getHeight(),
                predictedTargetPos.z + (double)target.getWidth() / 2.0
            ));
         }
         // КОНЕЦ ИЗМЕНЕННОГО БЛОКА

         Angle angle = MathAngle.fromVec3d(computedPoint.subtract(client.player.getEyePos()));
         return new StrikerConstructor.AttackPerpetratorConfigurable(
            target,
            angle,
            this.attackDistance(),
            this.options.getValue(),
            this.mode,
            box,
            computedPoint,
            this.onlyCriticals.getValue(),
            this.isSyncWithTpsEnabled(),
            false
         );
      } else {
         Box fallbackBox = client.player != null ? client.player.getBoundingBox() : new Box(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
         return new StrikerConstructor.AttackPerpetratorConfigurable(
            client.player,
            MathAngle.cameraAngle(),
            this.attackDistance(),
            this.options.getValue(),
            this.mode,
            fallbackBox,
            client.player.getEyePos(),
            this.onlyCriticals.getValue(),
            this.isSyncWithTpsEnabled(),
            false
         );
      }
   }

   public AngleConfig getRotationConfig() {
      return new AngleConfig(this.getSmoothMode(), true, this.moveFix.is("Free"));
   }

   public RotateConstructor getSmoothMode() {
      MinecraftClient client = MinecraftClient.getInstance();
      ElytraTarget elytraTarget = ElytraTarget.getInstance();
      if (client.player != null && client.player.isGliding() && elytraTarget != null && elytraTarget.isEnabled()) {
         return new LinearConstructor();
      } else {
         String var3 = this.mode.getValue();

         return (RotateConstructor)(switch (var3) {
            case "MatrixNew" -> new Matrix();
            case "Snap" -> new SnapAngle();
            case "FunTime" -> new FunTimeSnapAngle();
            case "FunTime New" -> new FTProfileAngle();
            case "FT", "FTAngle" -> new FTAngle();
            case "SpookyTime"  -> new SpookyTimeAngle();
            case "SpookyTime2" -> new SpookyTime2Angle();
            case "SpookyTime3" -> new SpookyTime3Angle();
            case "Legit" -> this.legitAngle;
            case "HolyWorld" -> HWangle.INSTANCE;
            case "Legit Snap" -> new LegitSnap();
            case "Neuro" -> new Neuro();
            case "Ai" -> new Ai();
            case "AimAssist" -> new AimAssistAngle();
            case "ReallyWorld" -> new ReallyWorld();
            case "AiNew" -> new AiNew();
            case "Byram" -> new Sloth();
            case "MLSAC" -> new MLSAC();
            default -> new LinearConstructor();
         });
      }
   }

   private void applyTargetOrChaseCorrection(InputEvent event, MinecraftClient client) {
      LivingEntity currentTarget = target;
      if (currentTarget != null && currentTarget.isAlive() && !client.player.isTouchingWater() && !client.player.isSubmergedInWater()) {
         if (this.moveFix.is("Target") || this.moveFix.is("Chase") || this.moveFix.is("Focused")) {
            StrikeManager attackHandler = this.getAttackHandlerRaw();
            if (attackHandler != null) {
               boolean attackReady = attackHandler.canAttack(this.getConfig(), 1) && client.player.distanceTo(currentTarget) <= this.attackDistance();
               if (this.moveFix.is("Focused")) {
                  if (attackReady) {
                     event.setDirectionalLow(false, false, false, false);
                  }
               } else if (this.moveFix.is("Target")) {
                  this.moveToward(event, client.player.getEntityPos(), currentTarget.getEntityPos(), AngleConnection.INSTANCE.getMoveRotation().getYaw());
               } else {
                  PlayerInput input = event.getInput();
                  if (input.forward() || input.backward() || input.left() || input.right()) {
                     Vec3d playerPos = client.player.getEntityPos();
                     Vec3d center = currentTarget.getBoundingBox().getCenter();
                     float targetYaw = currentTarget.getYaw();
                     double rad = Math.toRadians((double)targetYaw);
                     Vec3d forwardDir = new Vec3d(-Math.sin(rad), 0.0, Math.cos(rad)).normalize();
                     Vec3d rightDir = new Vec3d(-forwardDir.z, 0.0, forwardDir.x).normalize();
                     Vec3d offsetVec = Vec3d.ZERO;
                     if (input.forward()) {
                        offsetVec = offsetVec.add(forwardDir);
                     }

                     if (input.backward()) {
                        offsetVec = offsetVec.add(forwardDir.multiply(-1.0));
                     }

                     if (input.left()) {
                        offsetVec = offsetVec.add(rightDir.multiply(-1.0));
                     }

                     if (input.right()) {
                        offsetVec = offsetVec.add(rightDir);
                     }

                     double offset = (double)currentTarget.getWidth() / 2.0 + 0.1;
                     Vec3d moveTarget = offsetVec.lengthSquared() > 0.0 ? center.add(offsetVec.normalize().multiply(offset)) : center;
                     this.moveToward(event, playerPos, moveTarget, AngleConnection.INSTANCE.getMoveRotation().getYaw());
                  }
               }
            }
         }
      }
   }

   private void moveToward(InputEvent event, Vec3d playerPos, Vec3d targetPos, float yaw) {
      Vec3d targetFlat = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
      Vec3d dir = targetFlat.subtract(playerPos);
      if (dir.lengthSquared() < 1.0E-7) {
         event.setDirectionalLow(false, false, false, false);
      } else {
         float moveAngle = (float)Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90.0F;
         float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);
         boolean forward = false;
         boolean back = false;
         boolean left = false;
         boolean right = false;
         if (angleDiff >= -22.5F && angleDiff < 22.5F) {
            forward = true;
         } else if (angleDiff >= 22.5F && angleDiff < 67.5F) {
            forward = true;
            right = true;
         } else if (angleDiff >= 67.5F && angleDiff < 112.5F) {
            right = true;
         } else if (angleDiff >= 112.5F && angleDiff < 157.5F) {
            back = true;
            right = true;
         } else if (angleDiff >= -67.5F && angleDiff < -22.5F) {
            forward = true;
            left = true;
         } else if (angleDiff >= -112.5F && angleDiff < -67.5F) {
            left = true;
         } else if (angleDiff >= -157.5F && angleDiff < -112.5F) {
            back = true;
            left = true;
         } else {
            back = true;
         }

         event.setDirectionalLow(forward, back, left, right);
      }
   }

   public boolean shouldCancelInteractItem(Hand hand) {
      if (this.isInventoryScreenOpen()) {
         return false;
      } else if (this.shouldCancelUseInteractions()) {
         return true;
      } else {
         StrikeManager attackHandler = this.getAttackHandler();
         return attackHandler != null && attackHandler.shouldCancelShieldUse(hand);
      }
   }

   public boolean shouldSuppressAirUsePacket(Hand hand) {
      return this.shouldCancelInteractItem(hand);
   }

   public boolean shouldCancelInteractBlock() {
      if (this.isInventoryScreenOpen()) {
         return false;
      } else if (this.shouldCancelUseInteractions()) {
         return true;
      } else {
         StrikeManager attackHandler = this.getAttackHandler();
         return attackHandler != null && attackHandler.shouldCancelUseItemOn();
      }
   }

   public boolean shouldSuppressBlockUsePacket() {
      return this.shouldCancelInteractBlock();
   }

   public boolean shouldCancelEntityInteraction() {
      if (this.isInventoryScreenOpen()) {
         return false;
      } else if (this.shouldCancelUseInteractions()) {
         return true;
      } else {
         StrikeManager attackHandler = this.getAttackHandler();
         return attackHandler != null && attackHandler.shouldCancelEntityInteraction();
      }
   }

   public boolean shouldSuppressEntityUsePacket() {
      return this.shouldCancelEntityInteraction();
   }

   public boolean shouldBlockUseInteractions() {
      return false;
   }

   public boolean shouldPauseForUse() {
      return false;
   }

   public boolean shouldCancelUseInteractions() {
      return false;
   }

   private boolean isInventoryScreenOpen() {
      return MinecraftClient.getInstance().currentScreen instanceof InventoryScreen;
   }

   public boolean isSyncWithTpsEnabled() {
      return this.options.isSelected("Sync TPS");
   }

   public StrikeManager getAttackHandler() {
      return this.isEnabled() ? this.attackPerpetrator.getAttackHandler() : null;
   }

   public StrikeManager getAttackHandlerRaw() {
      return this.attackPerpetrator.getAttackHandler();
   }

   public boolean canAttack(int ticks) {
      if (this.isEnabled() && target != null) {
         StrikeManager attackHandler = this.getAttackHandler();
         StrikerConstructor.AttackPerpetratorConfigurable attackConfig = this.getConfig();
         return attackHandler != null && attackConfig != null && attackHandler.canAttack(attackConfig, ticks);
      } else {
         return false;
      }
   }

   public boolean hasQueuedAttack() {
      return false;
   }

   public void flushQueuedAttack() {
   }

   public ModeSetting getMode() {
      return this.mode;
   }

   public NumberSetting getTimeOnTick() {
      return this.timeOnTick;
   }

   public NumberSetting getSpeedValue() {
      return this.speedValue;
   }

   public NumberSetting getLegitSpeed() {
      return this.legitSpeed;
   }

   public NumberSetting getAimAssistSpeed() {
      return this.aimAssistSpeed;
   }

   public NumberSetting getAimAssistSmoothness() {
      return this.aimAssistSmoothness;
   }

   public boolean isNeuroDatasetCaptureEnabled() {
      return this.neuroDatasetCapture.getValue();
   }

   public ModeSetting getNeuroDatasetFormat() {
      return this.neuroDatasetFormat;
   }

   public boolean isNeuroHumanDatasetCaptureEnabled() {
      return this.neuroHumanDatasetCapture.getValue();
   }

   public ModeSetting getNeuroHumanDatasetFormat() {
      return this.neuroHumanDatasetFormat;
   }

   public boolean isNeuroModelInferenceEnabled() {
      return this.neuroModelInference.getValue();
   }

   public StringSetting getNeuroModelPath() {
      return this.neuroModelPath;
   }

   public NumberSetting getNeuroModelBlend() {
      return this.neuroModelBlend;
   }

   public boolean isNeuroHumanInferenceEnabled() {
      return this.neuroHumanInference.getValue();
   }

   public StringSetting getNeuroHumanModelPath() {
      return this.neuroHumanModelPath;
   }

   public NumberSetting getNeuroHumanBlend() {
      return this.neuroHumanBlend;
   }

   public NumberSetting getRange() {
      return this.attackRange;
   }

   public NumberSetting getLookRange() {
      return this.lookRange;
   }

   public MultiModeSetting getTargets() {
      return this.targetType;
   }

   public ModeSetting getTargetPriority() {
      return this.targetPriority;
   }

   public MultiModeSetting getOptions() {
      return this.options;
   }

   public ModeSetting getMoveFix() {
      return this.moveFix;
   }

   public BooleanSetting getOnlyCrits() {
      return this.onlyCriticals;
   }

   public BooleanSetting getOnlyCriticals() {
      return this.onlyCriticals;
   }

   public BooleanSetting getSmartCrits() {
      return this.smartCriticals;
   }

   public BooleanSetting getSmartCriticals() {
      return this.smartCriticals;
   }

   public boolean isCriticalBalanceEnabled() {
      return this.criticalBalance.getValue();
   }

   public boolean isHitMomentLegit() {
      return this.hitMoment.is("Legit");
   }

   public boolean isHitMomentRandom() {
      return this.hitMoment.is("Random");
   }

   public boolean isHitMomentStatic() {
      return this.hitMoment.is("Static");
   }

   public boolean isLegitMode() {
      return this.mode.is("Legit");
   }

   public boolean isMatrixMode() {
      return this.mode.is("Matrix") || this.mode.is("MatrixNew");
   }

   public boolean isLegitPitchCorrecting() {
      return false;
   }

   public long getActivationTimeMs() {
      return this.activationTimeMs;
   }

   public float getTps() {
      return this.tps;
   }

   public float getAdjustTicks() {
      return this.adjustTicks;
   }

   private LivingEntity updateTarget() {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client.player != null && client.world != null) {
         TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(this.targetType.getValue());
         float dynamicFov = this.mode.isSelected("Legit Snap") ? 35.0F : 360.0F;
         this.cachedDynamicFov = dynamicFov;
         LivingEntity lockedTarget = this.targetSelector.getCurrentTarget();
         if (this.shouldKeepLockedTarget(client, lockedTarget, filter)) {
            return lockedTarget;
         } else {
            if (lockedTarget != null) {
               this.targetSelector.releaseTarget();
            }

            this.targetSelector
               .searchTargets(client.world.getEntities(), this.finalDistance(), dynamicFov, this.options.isSelected("Ignore Walls"), filter::isValid);
            this.targetSelector.validateTarget(filter::isValid);
            LivingEntity found = this.targetSelector.getCurrentTarget();
            if (found != null && !this.isTargetInsideFov(found)) {
               this.targetSelector.releaseTarget();
               return null;
            } else {
               return found;
            }
         }
      } else {
         this.targetSelector.releaseTarget();
         return null;
      }
   }

   private boolean shouldKeepLockedTarget(MinecraftClient client, LivingEntity lockedTarget, TargetFinder.EntityFilter filter) {
      if (lockedTarget != null && filter.isValid(lockedTarget)) {
         float holdRange = Math.max(this.finalDistance(), this.keepTargetRange.getFloat());
         return client.player.squaredDistanceTo(lockedTarget) <= (double)(holdRange * holdRange) && this.isTargetInsideFov(lockedTarget);
      } else {
         return false;
      }
   }

   public boolean isTargetInsideFov(LivingEntity entity) {
      if (this.usesFovCircle() && entity != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player == null) {
            return false;
         } else {
            Angle toTarget = MathAngle.calculateAngle(entity.getBoundingBox().getCenter());
            double angleDifference = AngleConnection.computeRotationDifference(MathAngle.cameraAngle(), toTarget);
            return angleDifference <= (double)this.cachedDynamicFov;
         }
      } else {
         return true;
      }
   }

   private void finishRotationOnRelease(MinecraftClient client) {
      AngleConnection controller = AngleConnection.INSTANCE;
      if (this.mode.is("HolyWorld")) {
         HWangle.INSTANCE.reset();
      } else if (this.isLegitMode()) {
         RotationController.hardReset();
         this.legitAngle.reset();
         controller.restoreVanillaLook();
      } else if (this.mode.is("FunTime")) {
         controller.startReturning(new TimedBackAngle(800L));
      } else if (this.mode.is("SpookyTime3")) {
         SpookyTime3Angle.reset();
         controller.restoreVanillaLook();
      } else if (this.shouldApplyRotationOnRelease() && client != null && client.player != null && controller.getCurrentAngle() != null) {
         controller.applyPlayerRotation(client);
         controller.restoreVanillaLook();
      } else if (this.isMatrixMode()) {
         controller.restoreVanillaLook();
      } else {
         controller.startReturning();
      }
   }

   private boolean shouldApplyRotationOnRelease() {
      return this.mode.is("Snap")
         || this.mode.is("SpookyTime")
         || this.mode.is("FTAngle")
         || this.mode.is("HolyWorld")
         || this.mode.is("Neuro")
         || this.mode.is("AiNew")
         || this.mode.is("AimAssist")
         || this.mode.is("MatrixNew");
   }

   private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
      if (config != null && target != null) {
         if (!this.isLegitMode()) {
            if (!this.mode.is("HolyWorld")) {
               StrikeManager attackHandler = this.attackPerpetrator.getAttackHandler();
               if (!this.mode.is("Legit Snap") || attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(40.0)) {
                  AngleConnection controller = AngleConnection.INSTANCE;
                  Vec3d aimPoint = config.getAimPoint() != null ? config.getAimPoint() : config.getAngle().toVector();
                  Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), aimPoint);
                  controller.rotateTo(rotation, target, 1, this.getRotationConfig(), TaskPriority.HIGH_IMPORTANCE_1, this);
               }
            } else {
               AngleConnection controller = AngleConnection.INSTANCE;
               if (controller.getCurrentAngle() != null || controller.getCurrentRotationPlan() != null) {
                  controller.restoreVanillaLook();
               }

               Vec3d aimPoint = config.getAimPoint() != null ? config.getAimPoint() : config.getAngle().toVector();
               Angle current = new Angle(mc.player.getYaw(), mc.player.getPitch());
               HWangle.INSTANCE.limitAngleChange(current, config.getAngle(), aimPoint, target);
            }
         }
      }
   }

   private void performAuraAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
      StrikeManager attackHandler = this.attackPerpetrator.getAttackHandler();
      int before = attackHandler.getCount();
      this.attackPerpetrator.performAttack(config);
      int performed = attackHandler.getCount() - before;
      if (performed > 0) {
         this.reducedHitboxAttackCounter += performed;
      }
   }

   private Box adjustAttackBox(Box box) {
      if (box != null && this.shouldUseReducedHitbox()) {
         Vec3d center = box.getCenter();
         double halfX = Math.max(box.getLengthX() * 0.4, 0.01);
         double halfZ = Math.max(box.getLengthZ() * 0.4, 0.01);
         return new Box(center.x - halfX, box.minY, center.z - halfZ, center.x + halfX, box.maxY, center.z + halfZ);
      } else {
         return box;
      }
   }

   private boolean shouldUseReducedHitbox() {
      return this.reducedHitboxAttackCounter % 4 < 2;
   }

   private double limitDecimals(double value, int decimalPlaces) {
      return (double)Math.round(value * Math.pow(10.0, (double)decimalPlaces)) / Math.pow(10.0, (double)decimalPlaces);
   }

   private double computeHumanHitChance(
      double deltaLength, double distanceRatio, double targetSpeed, boolean canAttackNow, boolean canAttackSoon, boolean raytraceSuccess, double attackCooldown
   ) {
      double alignment = 1.0 - MathHelper.clamp(deltaLength / 35.0, 0.0, 1.0);
      double rangeFit = 1.0 - MathHelper.clamp(distanceRatio, 0.0, 1.0);
      double motionPenalty = MathHelper.clamp(targetSpeed / 0.9, 0.0, 1.0);
      double attackReadiness = canAttackNow ? 1.0 : (canAttackSoon ? 0.72 : 0.28);
      double raytraceFactor = raytraceSuccess ? 1.0 : 0.0;
      double cooldownFactor = MathHelper.clamp(attackCooldown, 0.0, 1.0);
      double chance = alignment * 0.4 + rangeFit * 0.15 + attackReadiness * 0.16 + raytraceFactor * 0.17 + cooldownFactor * 0.22 - motionPenalty * 0.1;
      return MathHelper.clamp(chance, 0.0, 1.0);
   }

   private String formatSessionClock(long sessionElapsedMs) {
      long totalSeconds = Math.max(0L, sessionElapsedMs / 1000L);
      long minutes = totalSeconds / 60L;
      long seconds = totalSeconds % 60L;
      return String.format("%02d:%02d", minutes, seconds);
   }

   private int computeHumanInputMask(MinecraftClient client) {
      if (client != null && client.options != null) {
         int mask = 0;
         mask = this.setHumanInput(mask, 1, client.options.attackKey.isPressed());
         mask = this.setHumanInput(mask, 2, client.options.useKey.isPressed());
         mask = this.setHumanInput(mask, 4, client.options.pickItemKey.isPressed());
         mask = this.setHumanInput(mask, 8, client.options.forwardKey.isPressed());
         mask = this.setHumanInput(mask, 16, client.options.backKey.isPressed());
         mask = this.setHumanInput(mask, 32, client.options.leftKey.isPressed());
         mask = this.setHumanInput(mask, 64, client.options.rightKey.isPressed());
         mask = this.setHumanInput(mask, 128, client.options.jumpKey.isPressed());
         mask = this.setHumanInput(mask, 256, client.options.sneakKey.isPressed());
         mask = this.setHumanInput(mask, 512, client.options.sprintKey.isPressed());
         if (client.getWindow() != null) {
            long handle = client.getWindow().getHandle();
            mask = this.setHumanInput(mask, 1024, GLFW.glfwGetMouseButton(handle, 0) == 1);
            mask = this.setHumanInput(mask, 2048, GLFW.glfwGetMouseButton(handle, 1) == 1);
            mask = this.setHumanInput(mask, 4096, GLFW.glfwGetMouseButton(handle, 2) == 1);
         }

         return mask;
      } else {
         return 0;
      }
   }

   private int setHumanInput(int mask, int flag, boolean enabled) {
      return enabled ? mask | flag : mask;
   }

   private boolean hasHumanInput(int mask, int flag) {
      return (mask & flag) != 0;
   }

   private void notifyHumanDatasetProgress(long savedSamples, long sessionElapsedMs) {
      if (savedSamples > 0L && savedSamples - this.lastHumanDatasetNoticeSamples >= 100L) {
         this.lastHumanDatasetNoticeSamples = savedSamples;
         ChatMessage.brandmessage("Neuro training: saved " + savedSamples + " mouse/keyboard moves (" + this.formatSessionClock(sessionElapsedMs) + ")");
      }
   }

   private void resetHumanDatasetTracking() {
      this.hasHumanRotationBaseline = false;
      this.previousHumanYaw = 0.0F;
      this.previousHumanPitch = 0.0F;
      this.accumulatedHumanMouseDx = 0.0;
      this.accumulatedHumanMouseDy = 0.0;
      this.humanMouseMovedThisTick = false;
      this.hasHumanInputBaseline = false;
      this.previousHumanInputMask = 0;
      this.lastHumanDatasetNoticeSamples = 0L;
   }

   public boolean usesFovCircle() {
      return this.mode.isSelected("Legit Snap");
   }

   private boolean isHumanDatasetMode() {
      return this.mode.is("Neuro") && this.neuroHumanDatasetCapture.getValue();
   }
}