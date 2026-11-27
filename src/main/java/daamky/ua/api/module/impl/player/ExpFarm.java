package daamky.ua.api.module.impl.player;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display.TextDisplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.inventory.InventoryTask;

public final class ExpFarm extends Module {
   private static final int MW = 80;
   private static final int MR = 3;
   private static final int AW = 120;
   private static final int RW = 600;
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "", "Опыт", "Опыт", "Монеты"));
   private final BooleanSetting invis = this.register(new BooleanSetting("Использовать инвиз", "", false));
   private final NumberSetting an = this.register(new NumberSetting("Анархия", "", 2.0, 1.0, 4.0, 1.0));
   private final NumberSetting an11 = this.register(new NumberSetting("Номер Анархии", "", 1.0, 1.0, 64.0, 1.0));
   private final NumberSetting expi = this.register(new NumberSetting("Количество опыта при котором уходить", "", 50.0, 1.0, 200.0, 1.0));
   private int s;
   private int a1;
   private int a2;
   private int q;
   private int w;
   private int ex;
   private int eb;
   private int d;
   private int stuckTicks;
   private int routeIndex;
   private int activeGotoX;
   private int activeGotoY;
   private int activeGotoZ;
   private int storageWaitTicks;
   private int mw;
   private int hr;
   private int os;
   private int st;
   private double lastPathX;
   private double lastPathY;
   private double lastPathZ;
   private String txt = "";
   private boolean drink;
   private boolean looked;
   private boolean rnd;
   private boolean picked;
   private boolean take;
   private boolean xp;
   private boolean rip;
   private boolean svc;
   private boolean oldp;
   private boolean oldps;
   private boolean riprun;

   public ExpFarm() {
      super("AnchorFarm", ".", ModuleCategory.PLAYER);
      this.expi.visibleWhen(() -> this.mode.is("Опыт"));
   }

   @Override
   protected void onEnable() {
      this.s = this.d = this.a1 = this.a2 = this.q = this.w = this.ex = this.eb = this.stuckTicks = this.routeIndex = this.storageWaitTicks = this.mw = this.hr = this.os = this.st = 0;
      this.activeGotoX = this.activeGotoY = this.activeGotoZ = 0;
      this.lastPathX = this.lastPathY = this.lastPathZ = 0.0;
      this.txt = "";
      this.drink = this.looked = this.rnd = this.picked = this.take = this.xp = this.rip = this.svc = this.oldps = this.riprun = false;
      if (mc.options != null) {
         this.oldp = mc.options.pauseOnLostFocus;
         this.oldps = true;
         mc.options.pauseOnLostFocus = false;
      }
   }

   @Override
   protected void onDisable() {
      this.s = this.d = this.a1 = this.a2 = this.q = this.w = this.ex = this.eb = this.stuckTicks = this.routeIndex = this.storageWaitTicks = this.mw = this.hr = this.os = this.st = 0;
      this.activeGotoX = this.activeGotoY = this.activeGotoZ = 0;
      this.lastPathX = this.lastPathY = this.lastPathZ = 0.0;
      this.txt = "";
      this.drink = this.looked = this.rnd = this.picked = this.take = this.xp = this.rip = this.svc = this.riprun = false;
      if (this.oldps && mc.options != null) {
         mc.options.pauseOnLostFocus = this.oldp;
      }

      this.oldps = false;
      this.bruh();
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      if (mc.player != null && mc.level != null && mc.gameMode != null) {
         if (mc.options != null) {
            mc.options.pauseOnLostFocus = false;
         }

         if (!(mc.player.getHealth() <= 0.0F) && !mc.player.isDeadOrDying()) {
            this.rip = false;
            if (mc.screen instanceof PauseScreen) {
               mc.setScreen(null);
            }

            if (this.d > 0) {
               this.d--;
            } else {
               this.kek();
               if (!this.stuck()) {
                  int expii = (int)this.expi.getFloat();
                  if (this.s == 23) {
                     this.bruh();
                     mc.player.connection.sendCommand("hub");
                     this.roll(true);
                     this.xp = false;
                     this.take = false;
                     this.svc = false;
                     this.picked = true;
                     this.d = 40;
                     this.s = 4;
                  } else {
                     if (this.s == 0 && this.riprun) {
                        this.riprun = false;
                     } else if (this.s == 0 && mc.player.experienceLevel >= expii) {
                        this.bruh();
                        mc.player.connection.sendCommand("hub");
                        this.roll(false);
                        this.picked = true;
                        this.xp = true;
                        this.ex = mc.player.experienceLevel;
                        this.eb = 0;
                        this.storageWaitTicks = 0;
                        this.take = this.invis.getValue() && !mc.player.hasEffect(MobEffects.INVISIBILITY) && this.pis() == -1;
                        this.svc = true;
                        this.d = 40;
                        this.s = 4;
                        return;
                     }

                     if (this.s == 0 && this.invis.getValue() && mc.player.hasEffect(MobEffects.INVISIBILITY)) {
                        this.drink = false;
                     }

                     if (this.s == 0 && this.invis.getValue() && !mc.player.hasEffect(MobEffects.INVISIBILITY)) {
                        int ii = this.pis();
                        if (ii == -1) {
                           if (!this.rnd && (this.picked || this.looked)) {
                              if (this.take && this.picked) {
                                 this.s = 11;
                                 return;
                              }

                              this.d = 20;
                              return;
                           }

                           this.bruh();
                           mc.player.connection.sendCommand("hub");
                           this.looked = true;
                           this.take = true;
                           this.svc = true;
                           this.roll(false);
                           this.picked = true;
                           this.d = 40;
                           this.s = 4;
                           return;
                        }

                        this.drink = true;
                        if (!this.picked) {
                           this.bruh();
                           mc.player.connection.sendCommand("hub");
                           this.roll(true);
                           this.picked = true;
                           this.d = 40;
                           this.s = 4;
                           return;
                        }
                     }

                     if (this.s == 14) {
                        if (this.xp && mc.player.experienceLevel < 15) {
                           this.finishXpStorageRun();
                        } else {
                           int bot = -1;

                           for (int i = 0; i < 36; i++) {
                              if (mc.player.getInventory().getItem(i).is(Items.GLASS_BOTTLE)) {
                                 bot = i;
                                 break;
                              }
                           }

                           if (bot != -1) {
                              int bar = mc.player.getInventory().getSelectedSlot();
                              if (bot >= 9) {
                                 for (int ix = 0; ix < 9; ix++) {
                                    if (mc.player.getInventory().getItem(ix).isEmpty()) {
                                       bar = ix;
                                       break;
                                    }
                                 }

                                 mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, bot, bar, ClickType.SWAP, mc.player);
                              } else {
                                 bar = bot;
                              }

                              mc.player.getInventory().setSelectedSlot(bar);
                              this.d = 10;
                              this.s = 16;
                           } else {
                              BlockPos box = this.boxx();
                              if (box == null) {
                                 this.storageWaitTicks++;
                                 if (this.storageWaitTicks >= 120) {
                                    this.go(true, false);
                                 } else {
                                    this.d = 20;
                                 }
                              } else {
                                 this.storageWaitTicks = 0;
                                 Direction side = Direction.UP;
                                 Vec3 vec = Vec3.atCenterOf(box).add(0.0, 0.5, 0.0);

                                 for (int ixx = 0; ixx < 9; ixx++) {
                                    ItemStack stack = mc.player.getInventory().getItem(ixx);
                                    if (!stack.is(Items.EXPERIENCE_BOTTLE) && !stack.is(Items.PAPER)) {
                                       mc.player.getInventory().setSelectedSlot(ixx);
                                       break;
                                    }
                                 }

                                 mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(vec, side, box, false));
                                 this.d = 10;
                                 this.s = 15;
                              }
                           }
                        }
                     } else if (this.s == 15) {
                        if (this.xp && mc.player.experienceLevel < 15) {
                           this.finishXpStorageRun();
                        } else if (!(mc.player.containerMenu instanceof ChestMenu menu)) {
                           this.d = 5;
                        } else {
                           int var38 = menu.getRowCount() * 9;
                           int var51 = -1;

                           for (int var61 = 0; var61 < var38; var61++) {
                              ItemStack stack = ((Slot)menu.slots.get(var61)).getItem();
                              if (stack.is(Items.GLASS_BOTTLE)) {
                                 var51 = var61;
                                 break;
                              }
                           }

                           if (var51 == -1) {
                              this.d = 20;
                           } else {
                              int bar = -1;

                              for (int ixxx = 0; ixxx < 9; ixxx++) {
                                 if (mc.player.getInventory().getItem(ixxx).isEmpty()) {
                                    bar = ixxx;
                                    break;
                                 }
                              }

                              if (bar == -1) {
                                 this.d = 20;
                              } else {
                                 this.q = var51;
                                 this.w = var38 + 27 + bar;
                                 mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, this.q, 0, ClickType.PICKUP, mc.player);
                                 this.d = 2;
                                 this.s = 21;
                              }
                           }
                        }
                     } else if (this.s == 21) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, this.w, 1, ClickType.PICKUP, mc.player);
                        this.d = 2;
                        this.s = 22;
                     } else if (this.s == 22) {
                        mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, this.q, 0, ClickType.PICKUP, mc.player);
                        this.q = this.w = 0;
                        mc.player.closeContainer();
                        this.d = 10;
                        this.s = 16;
                     } else if (this.s == 16) {
                        mc.player.connection.sendCommand("exp");
                        this.d = 20;
                        this.s = 17;
                     } else if (this.s == 17) {
                        int real = mc.player.experienceLevel;
                        this.ex = Math.max(0, real);
                        this.eb = 0;
                        if (real > 0 && real >= 15) {
                           int need = real >= 50 ? 50 : (real >= 45 ? 45 : (real >= 30 ? 30 : (real >= 15 ? 15 : 0)));
                           if (need <= 0) {
                              InventoryTask.closeScreen(false);
                              mc.player.closeContainer();
                              this.xp = false;
                              this.take = false;
                              this.svc = false;
                              this.looked = false;
                              this.bruh();
                              mc.player.connection.sendCommand("hub");
                              this.roll(true);
                              this.picked = true;
                              this.d = 40;
                              this.s = 4;
                           } else {
                              int[] ss = new int[]{-1, -1, -1, -1};
                              int[] cc = new int[]{50, 45, 30, 15};

                              for (int ixxxx = 0; ixxxx < mc.player.containerMenu.slots.size(); ixxxx++) {
                                 ItemStack stack = mc.player.containerMenu.getSlot(ixxxx).getItem();

                                 for (int j = 0; j < cc.length; j++) {
                                    if (stack.is(Items.EXPERIENCE_BOTTLE) && stack.getCount() == cc[j]) {
                                       ss[j] = ixxxx;
                                    }
                                 }
                              }

                              int ost = need;

                              for (int ixxxx = 0; ixxxx < cc.length; ixxxx++) {
                                 while (ss[ixxxx] != -1 && ost >= cc[ixxxx]) {
                                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, ss[ixxxx], 0, ClickType.PICKUP, mc.player);
                                    ost -= cc[ixxxx];
                                    this.eb = this.eb + cc[ixxxx];
                                 }
                              }

                              if (this.eb > 0) {
                                 this.d = 10;
                                 this.s = 18;
                              }
                           }
                        } else {
                           InventoryTask.closeScreen(false);
                           mc.player.closeContainer();
                           this.xp = false;
                           this.take = false;
                           this.svc = false;
                           this.looked = false;
                           this.bruh();
                           mc.player.connection.sendCommand("hub");
                           this.roll(true);
                           this.picked = true;
                           this.d = 40;
                           this.s = 4;
                        }
                     } else if (this.s == 18) {
                        InventoryTask.closeScreen(false);
                        mc.player.closeContainer();
                        this.d = 10;
                        this.s = 19;
                     } else if (this.s == 19) {
                        BlockPos box = this.boxx();
                        if (box == null) {
                           if (++this.storageWaitTicks >= 120) {
                              this.go(true, false);
                           } else {
                              this.d = 20;
                           }
                        } else {
                           this.storageWaitTicks = 0;

                           for (int ixxxx = 0; ixxxx < 9; ixxxx++) {
                              ItemStack stack = mc.player.getInventory().getItem(ixxxx);
                              if (!stack.is(Items.EXPERIENCE_BOTTLE) && !stack.is(Items.PAPER)) {
                                 mc.player.getInventory().setSelectedSlot(ixxxx);
                                 break;
                              }
                           }

                           Direction side = Direction.UP;
                           Vec3 vec = Vec3.atCenterOf(box).add(0.0, 0.5, 0.0);
                           mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(vec, side, box, false));
                           this.d = 10;
                           this.s = 20;
                        }
                     } else if (this.s == 20) {
                        if (!(mc.player.containerMenu instanceof ChestMenu menu)) {
                           this.d = 5;
                        } else {
                           int var33 = menu.getRowCount() * 9;
                           int var47 = -1;
                           int var59 = -1;

                           for (int var67 = 0; var67 < var33; var67++) {
                              if (((Slot)menu.slots.get(var67)).getItem().isEmpty()) {
                                 var47 = var67;
                                 break;
                              }
                           }

                           for (int ixxxxx = var33; ixxxxx < mc.player.containerMenu.slots.size(); ixxxxx++) {
                              ItemStack stack = mc.player.containerMenu.getSlot(ixxxxx).getItem();
                              if (stack.is(Items.EXPERIENCE_BOTTLE)) {
                                 var59 = ixxxxx;
                                 break;
                              }
                           }

                           if (var47 != -1 && var59 != -1) {
                              mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, var59, 0, ClickType.PICKUP, mc.player);
                              mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, var47, 0, ClickType.PICKUP, mc.player);
                              this.ex = Math.max(0, mc.player.experienceLevel);
                              this.eb = 0;
                              if (this.ex >= 15) {
                                 mc.player.closeContainer();
                                 this.d = 10;
                                 this.s = 14;
                              } else {
                                 boolean tookInvis = false;
                                 if (this.take) {
                                    int pot = -1;
                                    int bar = mc.player.getInventory().getSelectedSlot();

                                    for (int ixxxxxx = 0; ixxxxxx < var33; ixxxxxx++) {
                                       ItemStack stack = ((Slot)menu.slots.get(ixxxxxx)).getItem();
                                       if (this.sip(stack)) {
                                          pot = ixxxxxx;
                                          break;
                                       }
                                    }

                                    for (int ixxxxxxx = 0; ixxxxxxx < 9; ixxxxxxx++) {
                                       if (mc.player.getInventory().getItem(ixxxxxxx).isEmpty()) {
                                          bar = ixxxxxxx;
                                          break;
                                       }
                                    }

                                    if (pot == -1) {
                                       this.d = 20;
                                       return;
                                    }

                                    mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, pot, bar, ClickType.SWAP, mc.player);
                                    tookInvis = true;
                                 }

                                 mc.player.closeContainer();
                                 this.xp = false;
                                 this.take = false;
                                 this.svc = false;
                                 this.looked = false;
                                 this.bruh();
                                 this.drink = tookInvis;
                                 mc.player.connection.sendCommand("hub");
                                 this.roll(true);
                                 this.picked = true;
                                 this.d = 40;
                                 this.s = 4;
                              }
                           } else {
                              this.d = 20;
                           }
                        }
                     } else if (this.s == 0) {
                        for (int ixxxxxxxx = 0; ixxxxxxxx < 9; ixxxxxxxx++) {
                           if (mc.player.getInventory().getItem(ixxxxxxxx).is(Items.COMPASS)) {
                              this.bruh();
                              this.d = 20;
                              this.s = 4;
                              return;
                           }
                        }

                        mc.player.connection.sendCommand("warp pvp");
                        this.configureBaritoneMovement();
                        this.picked = false;
                        this.d = 40;
                        this.s = 9;
                     } else if (this.s == 9) {
                        if (this.invis.getValue() && !mc.player.hasEffect(MobEffects.INVISIBILITY) && this.drink) {
                           if (this.glug()) {
                              this.drink = false;
                              this.d = 35;
                              this.s = 10;
                              return;
                           }

                           this.drink = false;
                        }

                        this.startRoute(false);
                        this.d = 0;
                        this.s = 1;
                     } else if (this.s == 10) {
                        if (mc.options != null) {
                           mc.options.keyUse.setDown(false);
                        }

                        for (int ixxxxxxxxx = 0; ixxxxxxxxx < 9; ixxxxxxxxx++) {
                           ItemStack stack = mc.player.getInventory().getItem(ixxxxxxxxx);
                           if (stack.isEmpty()) {
                              mc.player.getInventory().setSelectedSlot(ixxxxxxxxx);
                              break;
                           }
                        }

                        this.startRoute(true);
                        this.d = 0;
                        this.s = 1;
                     } else if (this.s == 11) {
                        BlockPos box = this.boxx();
                        if (box == null) {
                           if (++this.storageWaitTicks >= 120) {
                              this.go(true, false);
                           } else {
                              this.d = 20;
                           }
                        } else {
                           this.storageWaitTicks = 0;
                           Direction side = Direction.UP;
                           Vec3 vec = Vec3.atCenterOf(box).add(0.0, 0.5, 0.0);

                           for (int ixxxxxxxxxx = 0; ixxxxxxxxxx < 9; ixxxxxxxxxx++) {
                              ItemStack stack = mc.player.getInventory().getItem(ixxxxxxxxxx);
                              if (!stack.is(Items.EXPERIENCE_BOTTLE) && !stack.is(Items.PAPER)) {
                                 mc.player.getInventory().setSelectedSlot(ixxxxxxxxxx);
                                 break;
                              }
                           }

                           mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(vec, side, box, false));
                           this.d = 10;
                           this.s = 12;
                        }
                     } else if (this.s == 12) {
                        if (!(mc.player.containerMenu instanceof ChestMenu menu)) {
                           this.d = 5;
                        } else {
                           int var29 = menu.getRowCount() * 9;
                           int var45 = -1;

                           for (int var56 = 0; var56 < var29; var56++) {
                              ItemStack stack = ((Slot)menu.slots.get(var56)).getItem();
                              if (this.sip(stack)) {
                                 var45 = var56;
                                 break;
                              }
                           }

                           if (var45 == -1) {
                              this.d = 20;
                           } else {
                              int bar = mc.player.getInventory().getSelectedSlot();

                              for (int ixxxxxxxxxxx = 0; ixxxxxxxxxxx < 9; ixxxxxxxxxxx++) {
                                 if (mc.player.getInventory().getItem(ixxxxxxxxxxx).isEmpty()) {
                                    bar = ixxxxxxxxxxx;
                                    break;
                                 }
                              }

                              mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, var45, bar, ClickType.SWAP, mc.player);
                              mc.player.closeContainer();
                              this.d = 10;
                              this.s = 13;
                           }
                        }
                     } else if (this.s == 13) {
                        if (this.pis() == -1) {
                           this.d = 10;
                           this.s = 12;
                        } else {
                           InventoryTask.closeScreen(false);
                           this.take = false;
                           this.looked = false;
                           this.bruh();
                           mc.player.connection.sendCommand("hub");
                           this.roll(true);
                           this.picked = true;
                           this.d = 40;
                           this.s = 4;
                        }
                     } else if (this.s == 1) {
                        if (mc.player.distanceToSqr(-320.0, 59.0, 25.0) > 9.0) {
                           this.manualWalkToActiveRoutePoint();
                           if (this.distanceToActiveGotoSqr() <= 6.25) {
                              this.sendNextRoutePoint();
                           }
                        } else {
                           this.txt = "";
                           this.bruh();
                           this.d = 20;
                           this.s = 2;
                        }
                     } else if (this.s == 2) {
                        if (!this.txt.isEmpty() && this.txt.contains(this.mode.getValue())) {
                           this.d = 40;
                           this.bruh();
                           this.s = 3;
                        } else {
                           this.bruh();
                           mc.player.connection.sendCommand("hub");
                           this.roll(true);
                           this.picked = true;
                           this.d = 40;
                           this.s = 4;
                        }
                     } else if (this.s == 3) {
                        if (mc.player.experienceLevel >= expii) {
                           this.bruh();
                           mc.player.connection.sendCommand("hub");
                           this.roll(false);
                           this.picked = true;
                           this.xp = true;
                           this.ex = mc.player.experienceLevel;
                           this.eb = 0;
                           this.storageWaitTicks = 0;
                           this.take = this.invis.getValue() && !mc.player.hasEffect(MobEffects.INVISIBILITY) && this.pis() == -1;
                           this.svc = true;
                           this.d = 40;
                           this.s = 4;
                        }
                     } else if (this.s == 4) {
                        int comp = -1;

                        for (int ixxxxxxxxxxxx = 0; ixxxxxxxxxxxx < 9; ixxxxxxxxxxxx++) {
                           ItemStack stack = mc.player.getInventory().getItem(ixxxxxxxxxxxx);
                           if (stack.is(Items.COMPASS)) {
                              comp = ixxxxxxxxxxxx;
                              break;
                           }
                        }

                        if (comp == -1) {
                           if (++this.mw >= 80) {
                              this.mw = 0;
                              this.bruh();
                              InventoryTask.closeScreen(false);
                              mc.player.closeContainer();
                              mc.player.connection.sendCommand("hub");
                              this.d = 40;
                           }
                        } else {
                           this.mw = 0;
                           mc.player.getInventory().setSelectedSlot(comp);
                           mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
                           this.d = 25;
                           this.s = 5;
                        }
                     } else if (this.s == 5) {
                        int head = -1;
                        int heads = 0;

                        for (int ixxxxxxxxxxxxx = 0; ixxxxxxxxxxxxx < mc.player.containerMenu.slots.size(); ixxxxxxxxxxxxx++) {
                           ItemStack stack = mc.player.containerMenu.getSlot(ixxxxxxxxxxxxx).getItem();
                           if (stack.is(Items.PLAYER_HEAD)) {
                              if (++heads == 2) {
                                 head = ixxxxxxxxxxxxx;
                                 break;
                              }
                           }
                        }

                        if (head == -1) {
                           this.retryAnarchyMenu(false);
                        } else {
                           this.mw = 0;
                           mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, head, 0, ClickType.PICKUP, mc.player);
                           this.s = 6;
                        }
                     } else if (this.s == 6) {
                        int stand = -1;
                        int stands = 0;
                        int ns = this.rnd ? this.a1 : (int)this.an.getFloat();

                        for (int ixxxxxxxxxxxxxx = 0; ixxxxxxxxxxxxxx < mc.player.containerMenu.slots.size(); ixxxxxxxxxxxxxx++) {
                           ItemStack stack = mc.player.containerMenu.getSlot(ixxxxxxxxxxxxxx).getItem();
                           if (stack.is(Items.ARMOR_STAND)) {
                              if (++stands == ns) {
                                 stand = ixxxxxxxxxxxxxx;
                                 break;
                              }
                           }
                        }

                        if (stand == -1) {
                           this.retryAnarchyMenu(false);
                        } else {
                           this.mw = 0;
                           mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, stand, 0, ClickType.PICKUP, mc.player);
                           this.d = 1;
                           this.s = 7;
                        }
                     } else if (this.s != 7) {
                        if (this.s == 8) {
                           this.txt = "";
                           if (this.xp) {
                              if (mc.player.experienceLevel < 15) {
                                 this.finishXpStorageRun();
                                 return;
                              }

                              if (this.hasCompassInHotbar() && this.boxx() == null && this.storageWaitTicks < 160) {
                                 this.storageWaitTicks++;
                                 this.d = 5;
                                 return;
                              }

                              this.storageWaitTicks = 0;
                              this.d = 20;
                              this.s = 14;
                              return;
                           }

                           if (this.take) {
                              this.d = 20;
                              this.s = 11;
                              return;
                           }

                           if (this.hasCompassInHotbar()) {
                              this.roll(true);
                              this.d = 20;
                              this.s = 4;
                              return;
                           }

                           if (this.svc) {
                              this.svc = false;
                              this.bruh();
                              mc.player.connection.sendCommand("hub");
                              this.roll(true);
                              this.picked = true;
                              this.d = 40;
                              this.s = 4;
                              return;
                           }

                           this.d = 40;
                           this.s = 0;
                        }
                     } else {
                        int head = -1;
                        int nh = this.rnd ? this.a2 : (int)this.an11.getFloat();

                        for (int ixxxxxxxxxxxxxxx = 0; ixxxxxxxxxxxxxxx < mc.player.containerMenu.slots.size(); ixxxxxxxxxxxxxxx++) {
                           ItemStack stack = mc.player.containerMenu.getSlot(ixxxxxxxxxxxxxxx).getItem();
                           if (stack.is(Items.PLAYER_HEAD) && stack.getCount() == nh) {
                              head = ixxxxxxxxxxxxxxx;
                              break;
                           }
                        }

                        if (head == -1) {
                           this.hr++;
                           this.retryAnarchyMenu(this.hr >= 3);
                        } else {
                           this.mw = 0;
                           this.hr = 0;
                           mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, head, 0, ClickType.PICKUP, mc.player);
                           this.d = 40;
                           this.s = 8;
                        }
                     }
                  }
               }
            }
         } else {
            if (!this.rip) {
               this.s = 23;
               this.d = this.a1 = this.a2 = this.q = this.w = this.ex = this.eb = 0;
               this.txt = "";
               this.drink = this.looked = this.rnd = this.picked = this.take = this.xp = this.svc = false;
               this.riprun = true;
               if (mc.options != null) {
                  mc.options.keyUse.setDown(false);
               }

               this.rip = true;
            }
         }
      }
   }

   private void retryAnarchyMenu(boolean randomize) {
      if (++this.mw >= 80) {
         this.go(false, randomize);
      }
   }

   private boolean stuck() {
      if (this.s != this.os) {
         this.os = this.s;
         this.st = 0;
         return false;
      } else {
         this.st++;
         if (this.s == 0 || this.s == 3 || this.s == 23) {
            return false;
         } else if (this.s == 1) {
            if (!this.isStuckOnGoto() && this.st < 600) {
               return false;
            } else {
               this.go(true, true);
               return true;
            }
         } else if (this.s >= 4 && this.s <= 8) {
            if (this.st >= 120) {
               if (this.s == 7) {
                  this.hr++;
               }

               this.go(false, this.s == 7 && this.hr >= 3);
               return true;
            } else {
               return false;
            }
         } else if (this.s >= 11 && this.s <= 22 && this.st >= 120) {
            this.go(true, false);
            return true;
         } else {
            return false;
         }
      }
   }

   private void go(boolean hub, boolean random) {
      this.mw = 0;
      this.storageWaitTicks = 0;
      this.st = 0;
      InventoryTask.closeScreen(false);
      mc.player.closeContainer();
      this.bruh();
      if (random) {
         this.roll(true);
      }

      if (hub) {
         this.picked = true;
         mc.player.connection.sendCommand("hub");
         this.d = 40;
      } else {
         this.d = 25;
      }

      this.s = 4;
   }

   private void configureBaritoneMovement() {
      if (mc.player != null) {
         mc.player.connection.sendChat("#set allowJumpAt256 true");
         mc.player.connection.sendChat("#set allowParkour true");
         mc.player.connection.sendChat("#set allowParkourAscend true");
         mc.player.connection.sendChat("#set allowDiagonalAscend true");
         mc.player.connection.sendChat("#set allowSprint true");
         mc.player.connection.sendChat("#set allowVines true");
         mc.player.connection.sendChat("#set assumeStep true");
         mc.player.connection.sendChat("#set maxFallHeightNoWater 55555");
      }
   }

   private void resetPathWatch() {
      this.stuckTicks = 0;
      if (mc.player == null) {
         this.lastPathX = this.lastPathY = this.lastPathZ = 0.0;
      } else {
         this.lastPathX = mc.player.getX();
         this.lastPathY = mc.player.getY();
         this.lastPathZ = mc.player.getZ();
      }
   }

   private boolean isStuckOnGoto() {
      double dx = mc.player.getX() - this.lastPathX;
      double dy = mc.player.getY() - this.lastPathY;
      double dz = mc.player.getZ() - this.lastPathZ;
      double movedSqr = dx * dx + dy * dy + dz * dz;
      this.lastPathX = mc.player.getX();
      this.lastPathY = mc.player.getY();
      this.lastPathZ = mc.player.getZ();
      if (movedSqr < 9.0E-4) {
         this.stuckTicks++;
      } else {
         this.stuckTicks = 0;
      }

      return this.stuckTicks >= 30 && mc.player.horizontalCollision;
   }

   private boolean hasCompassInHotbar() {
      for (int i = 0; i < 9; i++) {
         if (mc.player.getInventory().getItem(i).is(Items.COMPASS)) {
            return true;
         }
      }

      return false;
   }

   private void startRoute(boolean afterInvis) {
      this.bruh();
      this.routeIndex = afterInvis ? 1 : 0;
      this.sendRoutePoint(this.routeIndex);
   }

   private void sendNextRoutePoint() {
      this.routeIndex++;
      if (this.routeIndex >= 6) {
         this.routeIndex = 5;
      }

      this.sendRoutePoint(this.routeIndex);
   }

   private void sendRoutePoint(int index) {
      switch (index) {
         case 0:
            this.setGotoTarget(-323, 59, 21);
            break;
         case 1:
            this.setGotoTarget(-317, 59, 21);
            break;
         case 2:
            this.setGotoTarget(-323, 59, 29);
            break;
         case 3:
            this.setGotoTarget(-317, 59, 29);
            break;
         case 4:
            this.setGotoTarget(-319, 59, 24);
            break;
         default:
            this.setGotoTarget(-320, 59, 25);
      }

      this.resetPathWatch();
   }

   private void setGotoTarget(int x, int y, int z) {
      this.activeGotoX = x;
      this.activeGotoY = y;
      this.activeGotoZ = z;
   }

   private double distanceToActiveGotoSqr() {
      double dx = mc.player.getX() - (double)this.activeGotoX;
      double dy = mc.player.getY() - (double)this.activeGotoY;
      double dz = mc.player.getZ() - (double)this.activeGotoZ;
      return dx * dx + dy * dy + dz * dz;
   }

   private void manualWalkToActiveRoutePoint() {
      double dx = (double)this.activeGotoX + 0.5 - mc.player.getX();
      double dz = (double)this.activeGotoZ + 0.5 - mc.player.getZ();
      if (!(Math.abs(dx) < 0.05) || !(Math.abs(dz) < 0.05)) {
         BlockPos anchor = this.findNearbyRespawnAnchor();
         if (anchor != null) {
            this.lookAt(anchor);
         } else {
            this.lookAt((double)this.activeGotoX + 0.5, (double)this.activeGotoY, (double)this.activeGotoZ + 0.5);
         }

         if (mc.options != null) {
            mc.options.keyUp.setDown(true);
            mc.options.keySprint.setDown(true);
            mc.options.keyJump.setDown(true);
         }

         mc.player.setSprinting(true);
         if (mc.player.horizontalCollision && mc.player.onGround()) {
            mc.player.jumpFromGround();
         }
      }
   }

   private BlockPos findNearbyRespawnAnchor() {
      BlockPos me = mc.player.blockPosition();

      for (int x = -5; x <= 5; x++) {
         for (int y = -3; y <= 3; y++) {
            for (int z = -5; z <= 5; z++) {
               BlockPos pos = me.offset(x, y, z);
               if (mc.level.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                  return pos;
               }
            }
         }
      }

      return null;
   }

   private void lookAt(BlockPos pos) {
      this.lookAt((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5);
   }

   private void lookAt(double x, double y, double z) {
      double dx = x - mc.player.getX();
      double dz = z - mc.player.getZ();
      float yaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
      mc.player.setYRot(yaw);
      mc.player.setYHeadRot(yaw);
      mc.player.setYBodyRot(yaw);
   }

   private void kek() {
      double max = 576.0;
      String near = "";
      double nearD = Double.MAX_VALUE;

      for (Entity entity : mc.level.entitiesForRendering()) {
         if (entity != null && entity != mc.player) {
            double dst = mc.player.distanceToSqr(entity);
            if (!(dst > max) && !(dst >= nearD)) {
               Component c = entity instanceof TextDisplay textDisplay ? textDisplay.getText() : null;
               if (c == null) {
                  c = entity.getCustomName();
               }

               String text = c == null ? "" : c.getString().replace('\n', ' ');
               if (text.contains("Монеты") || text.contains("Опыт")) {
                  near = text;
                  nearD = dst;
               }
            }
         }
      }

      if (!near.isEmpty()) {
         this.txt = near;
      }
   }

   private int pis() {
      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getItem(i);
         if (this.sip(stack)) {
            return i;
         }
      }

      return -1;
   }

   private boolean sip(ItemStack stack) {
      if (stack != null && (stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION))) {
         PotionContents pc = (PotionContents)stack.get(DataComponents.POTION_CONTENTS);
         if (pc == null) {
            return false;
         } else {
            for (MobEffectInstance effect : pc.getAllEffects()) {
               if (effect.getEffect().is(MobEffects.INVISIBILITY)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private BlockPos boxx() {
      BlockPos me = mc.player.blockPosition();
      BlockPos best = null;
      double bd = Double.MAX_VALUE;

      for (int x = -5; x <= 5; x++) {
         for (int y = -3; y <= 3; y++) {
            for (int z = -5; z <= 5; z++) {
               BlockPos pos = me.offset(x, y, z);
               Block block = mc.level.getBlockState(pos).getBlock();
               if (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST || block == Blocks.BARREL) {
                  double distance = mc.player.distanceToSqr(Vec3.atCenterOf(pos));
                  if (distance < bd) {
                     bd = distance;
                     best = pos;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean glug() {
      int slot = this.pis();
      if (slot == -1) {
         return false;
      } else {
         int old = mc.player.getInventory().getSelectedSlot();
         if (slot >= 9) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, old, ClickType.SWAP, mc.player);
            slot = old;
         }

         mc.player.getInventory().setSelectedSlot(slot);
         mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
         if (mc.options != null) {
            mc.options.keyUse.setDown(true);
         }

         return true;
      }
   }

   private void roll(boolean random) {
      this.rnd = random;
      if (random) {
         this.svc = false;
      }

      if (!random) {
         this.a1 = (int)this.an.getFloat();
         this.a2 = (int)this.an11.getFloat();
      } else {
         int last = this.a2;
         int no = (int)this.an11.getFloat();
         int pg = (int)this.an.getFloat();

         do {
            this.a2 = ThreadLocalRandom.current().nextInt(1, 65);
            this.a1 = (this.a2 - 1) / 16 + 1;
         } while (this.a2 == last || this.a2 == no || this.a1 == pg && this.a2 == no);
      }
   }

   private void finishXpStorageRun() {
      InventoryTask.closeScreen(false);
      mc.player.closeContainer();
      this.xp = false;
      this.take = false;
      this.svc = false;
      this.looked = false;
      this.storageWaitTicks = 0;
      this.eb = 0;
      this.bruh();
      mc.player.connection.sendCommand("hub");
      this.roll(true);
      this.picked = true;
      this.d = 40;
      this.s = 4;
   }

   private void bruh() {
      if (mc.player != null) {
         mc.player.connection.sendChat("#stop");
         mc.player.connection.sendChat("#cancel");
      }

      if (mc.options != null) {
         mc.options.keyUp.setDown(false);
         mc.options.keyDown.setDown(false);
         mc.options.keyLeft.setDown(false);
         mc.options.keyRight.setDown(false);
         mc.options.keyJump.setDown(false);
         mc.options.keyShift.setDown(false);
         mc.options.keySprint.setDown(false);
         mc.options.keyUse.setDown(false);
         mc.options.keyAttack.setDown(false);
      }

      if (mc.player != null) {
         mc.player.setSprinting(false);
      }
   }
}
