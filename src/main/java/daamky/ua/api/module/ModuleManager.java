package daamky.ua.api.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import lightning.sdk.LightningSDK;
import lightning.sdk.annotation.LgVM;
import lightning.sdk.annotation.LgVM.Level;
import net.minecraft.client.MinecraftClient;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.bus.EventBus;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.impl.combat.AimAssist;
import daamky.ua.api.module.impl.combat.AntiBot;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.api.module.impl.combat.AutoCrystal;
import daamky.ua.api.module.impl.combat.AutoGApple;
import daamky.ua.api.module.impl.combat.AutoMaceSwap;
import daamky.ua.api.module.impl.combat.AutoSwap;
import daamky.ua.api.module.impl.combat.AutoTotem;
import daamky.ua.api.module.impl.combat.BackTracks;
import daamky.ua.api.module.impl.combat.Criticals;
import daamky.ua.api.module.impl.combat.HitBoxModule;
import daamky.ua.api.module.impl.combat.HitSound;
import daamky.ua.api.module.impl.combat.NoFriendDamage;
import daamky.ua.api.module.impl.combat.NoInteract;
import daamky.ua.api.module.impl.combat.ShiftTap;
import daamky.ua.api.module.impl.combat.TriggerBot;
import daamky.ua.api.module.impl.combat.Velocity;
import daamky.ua.api.module.impl.display.Appearance;
import daamky.ua.api.module.impl.misc.AutoAuth;
import daamky.ua.api.module.impl.misc.AutoDuel;
import daamky.ua.api.module.impl.misc.AutoEvent;
import daamky.ua.api.module.impl.misc.AutoLeave;
import daamky.ua.api.module.impl.misc.AutoLes;
import daamky.ua.api.module.impl.combat.AutoExplosion;
import daamky.ua.api.module.impl.misc.AutoMine;
import daamky.ua.api.module.impl.misc.AutoTpAccept;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.api.module.impl.misc.DeathCoords;
import daamky.ua.api.module.impl.misc.ElytraHelper;
import daamky.ua.api.module.impl.misc.ExpCrafter;
import daamky.ua.api.module.impl.misc.JoinerHelper;
import daamky.ua.api.module.impl.misc.ServerHelper;
import daamky.ua.api.module.impl.misc.ServerRPSpoofer;
import daamky.ua.api.module.impl.misc.UseTracker;
import daamky.ua.api.module.impl.movement.AirStuck;
import daamky.ua.api.module.impl.movement.AutoSprint;
import daamky.ua.api.module.impl.movement.ElytraTarget;
import daamky.ua.api.module.impl.movement.Fly;
import daamky.ua.api.module.impl.movement.InventoryMove;
import daamky.ua.api.module.impl.movement.NoSlow;
import daamky.ua.api.module.impl.movement.NoWeb;
import daamky.ua.api.module.impl.movement.Scaffold;
import daamky.ua.api.module.impl.movement.Speed;
import daamky.ua.api.module.impl.movement.Spider;
import daamky.ua.api.module.impl.movement.Strafe;
import daamky.ua.api.module.impl.movement.SuperFireWork;
import daamky.ua.api.module.impl.movement.TargetStrafe;
import daamky.ua.api.module.impl.movement.Timer;
import daamky.ua.api.module.impl.player.AHHelper;
import daamky.ua.api.module.impl.player.AntiDrop;
import daamky.ua.api.module.impl.player.AutoBuy;
import daamky.ua.api.module.impl.player.AutoRespawn;
import daamky.ua.api.module.impl.player.AutoTool;
import daamky.ua.api.module.impl.player.AutoUse;
import daamky.ua.api.module.impl.player.AutoWarden;
import daamky.ua.api.module.impl.player.ChestStealer;
import daamky.ua.api.module.impl.player.ClanUpgrade;
import daamky.ua.api.module.impl.player.ClickPearl;
import daamky.ua.api.module.impl.player.ExpFarm;
import daamky.ua.api.module.impl.player.FastBreak;
import daamky.ua.api.module.impl.player.FreeCam;
import daamky.ua.api.module.impl.player.FreeLook;
import daamky.ua.api.module.impl.player.NameProtect;
import daamky.ua.api.module.impl.player.NoDelay;
import daamky.ua.api.module.impl.player.NoEntityTrace;
import daamky.ua.api.module.impl.player.NoPush;
import daamky.ua.api.module.impl.player.OpenWalls;
import daamky.ua.api.module.impl.player.SpConnect;
import daamky.ua.api.module.impl.player.WindJump;
import daamky.ua.api.module.impl.visual.Ambience;
import daamky.ua.api.module.impl.visual.Animations;
import daamky.ua.api.module.impl.visual.Arrows;
import daamky.ua.api.module.impl.visual.AspectRatio;
import daamky.ua.api.module.impl.visual.BlockESP;
import daamky.ua.api.module.impl.visual.BlockOverlay;
import daamky.ua.api.module.impl.visual.DashTrail;
import daamky.ua.api.module.impl.visual.ESP;
import daamky.ua.api.module.impl.visual.FogBlur;
import daamky.ua.api.module.impl.visual.GlassHands;
import daamky.ua.api.module.impl.visual.GoldHPIndicator;
import daamky.ua.api.module.impl.visual.HolMyItems;
import daamky.ua.api.module.impl.visual.Hud;
import daamky.ua.api.module.impl.visual.ItemPhysics;
import daamky.ua.api.module.impl.visual.JumpCircle;
import daamky.ua.api.module.impl.visual.KillEffect;
import daamky.ua.api.module.impl.visual.MusicLyrics;
import daamky.ua.api.module.impl.visual.NoRender;
import daamky.ua.api.module.impl.visual.Particles;
import daamky.ua.api.module.impl.visual.Predictions;
import daamky.ua.api.module.impl.visual.RichDog;
import daamky.ua.api.module.impl.visual.SeeInvisible;
import daamky.ua.api.module.impl.visual.ShaderHand;
import daamky.ua.api.module.impl.visual.SwingAnimation;
import daamky.ua.api.module.impl.visual.TargetESP;
import daamky.ua.api.module.impl.visual.ViewModel;
import daamky.ua.api.module.impl.visual.WardenHelper;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.utils.inventory.InventoryFlowManager;
import daamky.ua.utils.network.Network;

public final class ModuleManager {
   private final EventBus eventBus;
   private final List<Module> modules = new ArrayList<>();
   private final Map<String, Module> byName = new HashMap<>();
   private final Map<Class<? extends Module>, Module> byType = new HashMap<>();
   private final Map<ModuleCategory, List<Module>> byCategory = new EnumMap<>(ModuleCategory.class);
   private final Map<ModuleCategory, List<Module>> byCategoryViews = new EnumMap<>(ModuleCategory.class);
   private Collection<Module> modulesView = List.of();
   private final Map<Module, Boolean> bindStates = new IdentityHashMap<>();
   private Runnable dirtyListener = () -> {
   };

   public ModuleManager(EventBus eventBus) {
      this.eventBus = eventBus;
   }

   @LgVM(Level.FULL)
   public void init() {
      if (LightningSDK.checkAuthAlive()) {
         this.registerDefaults();
         this.eventBus.register(this);
      }
   }

   public void setDirtyListener(Runnable dirtyListener) {
      this.dirtyListener = dirtyListener == null ? () -> {
      } : dirtyListener;
   }

   public void register(Module module) {
      String key = this.normalize(module.getName());
      if (!this.byName.containsKey(key)) {
         this.modules.add(module);
         this.byName.put(key, module);
         this.byType.putIfAbsent((Class<? extends Module>)module.getClass(), module);
         this.byCategory.computeIfAbsent(module.getCategory(), ignored -> new ArrayList<>()).add(module);
         module.addStateListener(changedModule -> this.dirtyListener.run());
         module.initialize(this.eventBus);
         this.sortViews();
      }
   }

   public Collection<Module> getModules() {
      return this.modulesView;
   }

   public List<Module> getByCategory(ModuleCategory category) {
      return this.byCategoryViews.getOrDefault(category, List.of());
   }

   public Optional<Module> getByName(String name) {
      return Optional.ofNullable(this.byName.get(this.normalize(name)));
   }

   public <T extends Module> Optional<T> getByType(Class<T> type) {
      Module module = this.byType.get(type);
      if (type.isInstance(module)) {
         return Optional.of(type.cast(module));
      } else {
         for (Module candidate : this.modules) {
            if (type.isInstance(candidate)) {
               return Optional.of(type.cast(candidate));
            }
         }

         return Optional.empty();
      }
   }

   public boolean isEnabled(Class<? extends Module> type) {
      return this.getByType(type).filter(Module::isEnabled).isPresent();
   }

   public Optional<AuraModule> getAura() {
      return this.getByType(AuraModule.class);
   }

   public Optional<AuraModule> getKillAura() {
      return this.getAura();
   }

   public Optional<HitBoxModule> getHitBox() {
      return this.getByType(HitBoxModule.class);
   }

   public boolean isHitBoxEnabled() {
      return this.isEnabled(HitBoxModule.class);
   }

   public Optional<NoInteract> getNoInteract() {
      return this.getByType(NoInteract.class);
   }

   public boolean isNoInteractEnabled() {
      return this.isEnabled(NoInteract.class);
   }

   public Optional<ServerRPSpoofer> getServerRPSpoofer() {
      return this.getByType(ServerRPSpoofer.class);
   }

   public boolean isServerRPSpooferEnabled() {
      return this.isEnabled(ServerRPSpoofer.class);
   }

   @SubscribeEvent
   private void onPreTick(TickEvent.Pre event) {
      InventoryFlowManager.update();
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      MinecraftClient client = event.getClient();
      Network.tick();
      this.handleBinds(client);

      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.onTick(client);
         }
      }
   }

   @LgVM(Level.FULL)
   private void registerDefaults() {
      if (LightningSDK.checkAuthAlive()) {
         this.register(new Appearance());
         this.register(new AntiBot());
         this.register(new AimAssist());
         this.register(new AuraModule());
         this.register(new BackTracks());
         this.register(new AutoCrystal());
         this.register(new AutoExplosion());
         this.register(new AutoGApple());
         this.register(new AutoSwap());
         this.register(new Criticals());
         this.register(new Animations());
         this.register(new AntiDrop());
         this.register(new ExpCrafter());
         this.register(new ShaderHand());
         this.register(new ExpFarm());
         this.register(new AutoMine());
         this.register(new WindJump());
         this.register(new AutoWarden());
         this.register(new AutoBuy());
         this.register(new GoldHPIndicator());
         this.register(new AutoMaceSwap());
         this.register(new RichDog());
         this.register(new JumpCircle());
         this.register(new AutoTotem());
         this.register(new HitBoxModule());
         this.register(new WardenHelper());
         this.register(new HitSound());
         this.register(new NoFriendDamage());
         this.register(new NoInteract());
         this.register(new TriggerBot());
         this.register(new Velocity());
         this.register(new Scaffold());
         this.register(new ShiftTap());
         this.register(new AutoSprint());
         this.register(new Fly());
         this.register(new ElytraTarget());
         this.register(new NoWeb());
         this.register(new NoSlow());
         this.register(new InventoryMove());
         this.register(new Spider());
         this.register(new Speed());
         this.register(new Strafe());
         this.register(new SuperFireWork());
         this.register(new TargetStrafe());
         this.register(new AirStuck());
         this.register(new Timer());
         this.register(new Ambience());
         this.register(new HolMyItems());
         this.register(new Arrows());
         this.register(new AspectRatio());
         this.register(new DashTrail());
         this.register(new BlockESP());
         this.register(new ESP());
         this.register(new FogBlur());
         this.register(new GlassHands());
         this.register(new ShaderHand());
         this.register(new Hud());
         this.register(new ItemPhysics());
         this.register(new KillEffect());
         this.register(new MusicLyrics());
         this.register(new Particles());
         this.register(new Predictions());
         this.register(new SeeInvisible());
         this.register(new SwingAnimation());
         this.register(new ViewModel());
         this.register(new NoRender());
         this.register(new TargetESP());
         this.register(new BlockOverlay());
         this.register(new OpenWalls());
         this.register(new NoPush());
         this.register(new NoEntityTrace());
         this.register(new NoDelay());
         this.register(new FastBreak());
         this.register(new NameProtect());
         this.register(new FreeLook());
         this.register(new FreeCam());
         this.register(new AHHelper());
         this.register(new AutoRespawn());
         this.register(new AutoTool());
         this.register(new AutoUse());
         this.register(new ChestStealer());
         this.register(new ClanUpgrade());
         this.register(new SpConnect());
         this.register(new JoinerHelper());
         this.register(new ServerRPSpoofer());
         this.register(new AutoAuth());
         this.register(new AutoDuel());
         this.register(new AutoEvent());
         this.register(new AutoLeave());
         this.register(new AutoTpAccept());
         this.register(new AutoLes());
         this.register(new ClientSounds());
         this.register(new ClickPearl());
         this.register(new DeathCoords());
         this.register(new ElytraHelper());
         this.register(new ServerHelper());
         this.register(new UseTracker());
      }
   }

   private void handleBinds(MinecraftClient client) {
      if (client != null && client.getWindow() != null && client.currentScreen == null) {
         long handle = client.getWindow().getHandle();

         for (Module module : this.modules) {
            KeyBind bind = module.getBind();
            boolean down = bind.isDown(handle);
            boolean wasDown = this.bindStates.getOrDefault(module, false);
            if (down && !wasDown) {
               module.toggle();
            }

            this.bindStates.put(module, down);
         }
      } else {
         this.bindStates.clear();
      }
   }

   private void register(Module module, String name, String description, ModuleCategory category) {
      module.configure(name, description, category);
      this.register(module);
   }

   private void sortViews() {
      this.modules.sort(Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT)));

      for (List<Module> categoryModules : this.byCategory.values()) {
         categoryModules.sort(Comparator.comparing(module -> module.getName().toLowerCase(Locale.ROOT)));
      }

      this.modulesView = Collections.unmodifiableList(this.modules);
      this.byCategoryViews.clear();

      for (Entry<ModuleCategory, List<Module>> entry : this.byCategory.entrySet()) {
         this.byCategoryViews.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
      }
   }

   private String normalize(String name) {
      return name == null ? "" : name.toLowerCase(Locale.ROOT).replace(" ", "");
   }
}
