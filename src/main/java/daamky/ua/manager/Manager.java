package daamky.ua.manager;

import lightning.sdk.LightningSDK;
import lightning.sdk.annotation.LgVM;
import lightning.sdk.annotation.LgVM.Level;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.StartTick;
import net.minecraft.client.MinecraftClient;
import daamky.ua.api.command.CommandManager;
import daamky.ua.api.config.ConfigManager;
import daamky.ua.api.drag.core.ElementManager;
import daamky.ua.api.events.Event;
import daamky.ua.api.events.bus.EventBus;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.ModuleManager;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.rotations.HWangle;
import daamky.ua.api.module.impl.misc.ClientSounds;
import daamky.ua.common.proxy.Config;
import daamky.ua.screens.clickgui.ClickGui;
import daamky.ua.screens.modernui.ClickGuiScreen;
import daamky.ua.screens.modernui.impl.WorldAnimation;
import daamky.ua.utils.inventory.item.ItemToolkit;
import daamky.ua.utils.repository.friend.FriendUtils;
import daamky.ua.utils.repository.macro.MacroRepository;
import daamky.ua.utils.repository.staff.StaffUtils;
import daamky.ua.utils.repository.way.WayRepository;

public class Manager {
   private static Manager instance;
   private EventBus eventBus;
   private ModuleManager moduleManager;
   private ConfigManager configManager;
   private CommandManager commandManager;
   private boolean clickGuiTextWarmed;

   public Manager() {
   }

   @LgVM(Level.FULL)
   public void initClient() {
      if (LightningSDK.checkAuthAlive()) {
         instance = this;
         this.eventBus = new EventBus();
         Config.loadConfig();
         FriendUtils.load();
         StaffUtils.load();
         MacroRepository.getInstance().load();
         WayRepository.getInstance().load();
         this.moduleManager = new ModuleManager(this.eventBus);
         this.moduleManager.init();
         this.commandManager = new CommandManager();
         this.commandManager.init();
         this.eventBus.register(AngleConnection.INSTANCE);
         this.eventBus.register(HWangle.INSTANCE);
         this.configManager = new ConfigManager(this.moduleManager);
         this.moduleManager.setDirtyListener(this.configManager::markDirty);
         this.configManager.init();
         ElementManager.getInstance().load();
         this.configManager.loadAll();
         // Bind the replacement GUI to the real module instances after persisted values are loaded.
         daamky.gui.Modules.bindToTarget(this.moduleManager);
         this.eventBus.register(this.configManager);
         this.eventBus.register(this.commandManager);
         this.eventBus.register(MacroRepository.getInstance());
         this.eventBus.register(WayRepository.getInstance());
         this.eventBus.register(ItemToolkit.INSTANCE);
         ClientTickEvents.START_CLIENT_TICK.register((StartTick)client -> {
            this.warmClickGuiText();
            WorldAnimation.tick();
            ClickGui.tickMovementKeys();
            postEvent(new TickEvent.Pre(client));
         });
         ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> postEvent(new TickEvent.Post(client)));
      }
   }

   public static Manager getInstance() {
      return instance;
   }

   public EventBus getEventBus() {
      return this.eventBus;
   }

   public ModuleManager getModuleManager() {
      return this.moduleManager;
   }

   public ConfigManager getConfigManager() {
      return this.configManager;
   }

   public CommandManager getCommandManager() {
      return this.commandManager;
   }

   public static ModuleManager getModules() {
      return instance == null ? null : instance.moduleManager;
   }

   public static <T extends Event> T postEvent(T event) {
      return instance != null && instance.eventBus != null ? instance.eventBus.post(event) : event;
   }

   public static void toggleClickGui(MinecraftClient client) {
      if (daamky.gui.GuiStyle.isClientGui(client.currentScreen)) {
         ClientSounds.playClickGuiClosingSound();
         client.currentScreen.close();
      } else if (!WorldAnimation.isActive()) {
         ClientSounds.playClickGuiOpeningSound();
         client.setScreen(daamky.gui.GuiStyle.create());
      }
   }

   private void warmClickGuiText() {
      if (!this.clickGuiTextWarmed) {
         this.clickGuiTextWarmed = true;
         ClickGui.warmupText();
      }
   }
}
