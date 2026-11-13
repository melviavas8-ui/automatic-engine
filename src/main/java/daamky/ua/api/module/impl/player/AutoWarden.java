package daamky.ua.api.module.impl.player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Type;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.RotationUpdateEvent;
import daamky.ua.api.events.impl.TickEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.AngleConfig;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.module.impl.combat.aura.impl.LinearConstructor;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.module.impl.visual.WardenHelper;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.StringSetting;
import daamky.ua.utils.network.Network;

public final class AutoWarden extends Module {
   private final Set<BlockPos> ig = new HashSet<>();
   private final Set<BlockPos> dt = new HashSet<>();
   private boolean bm;
   private BlockPos bt;
   private int iv;
   private BlockPos ib;
   private boolean ni;
   private boolean ns;
   private boolean nc;
   private boolean mv;
   private boolean so;
   private int d;
   private int rt;
   private BlockPos ob;
   private BlockPos tb;
   private BlockPos pos;
   private boolean wr;
   private int rd;
   private int ut;
   private int um;
   private int ss;
   private boolean ds;
   private int p0 = -1;
   private int p1 = -1;
   private int p2 = -1;
   private int p3;
   private int sr;
   private boolean pI;
   private boolean pS;
   private boolean pC;
   private boolean pR;
   private boolean init;
   private boolean dead;
   private int fs;
   private int fd;
   private int fw;
   private boolean ep;
   private BlockPos et;
   private int pr;
   private double eh;
   private final StringSetting an = this.register(new StringSetting("Warden An", "", "211", 15));
   private final StringSetting home = this.register(new StringSetting("Wardennhouse", "", "home", 15));
   private final BooleanSetting invis = this.register(new BooleanSetting("use invisible", "", true));
   private final BooleanSetting speed = this.register(new BooleanSetting("Use speed", "", true));
   private final BooleanSetting carrot = new BooleanSetting("Use carrot", "", true);
   private final StringSetting sk = this.register(new StringSetting("An WareHouse", "", "308", 15));

   public AutoWarden() {
      super("AutoWarden", "", ModuleCategory.PLAYER);
   }

   @Override
   protected void onEnable() {
      this.rst();
      this.wr = false;
      this.rd = 0;
      this.init = true;
      this.dead = false;
      this.ig.clear();
      this.dt.clear();
      this.barCfg();
      this.keys();
   }

   @Override
   protected void onDisable() {
      this.rst();
      this.wr = false;
      this.rd = 0;
      this.init = false;
      this.dead = false;
      this.stop();
      this.keys();
   }

   @SubscribeEvent
   private void r(RotationUpdateEvent event) {
      if (event.isPre() && mc.player != null && this.tb != null) {
         Vec3d hitVec = Vec3d.ofCenter(this.tb).add(0.0, 0.25, 0.0);
         Angle angle = MathAngle.fromTo(mc.player.getEyePos(), hitVec);
         AngleConnection.INSTANCE
            .rotateTo(
               new Angle.VecRotation(angle, hitVec),
               mc.player,
               1,
               new AngleConfig(LinearConstructor.INSTANCE, true, false),
               TaskPriority.HIGH_IMPORTANCE_1,
               this
            );
      }
   }

   @SubscribeEvent
   private void t(TickEvent.Post event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         WardenHelper.updateTimers();
         this.barJump();
         BlockPos playerPos = mc.player.getBlockPos();
         boolean atDeathSpawn = playerPos.getX() == 0 && playerPos.getY() == 90 && playerPos.getZ() == 0;
         if (atDeathSpawn) {
            if (!this.dead) {
               this.dead = true;
               this.rst();
               this.stop();
               if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
                  mc.player.closeHandledScreen();
               }

               String warehouseAnarchy = this.sk.getValue().trim();
               mc.player.networkHandler.sendChatCommand("an" + warehouseAnarchy);
               this.init = true;
               this.wr = true;
               this.rd = 40;
            }
         } else {
            if (this.dead) {
               this.dead = false;
            }

            if (this.ut > 0) {
               if (this.um != 11 && this.um != 22 && this.um != 33) {
                  if (mc.options != null) {
                     mc.options.useKey.setPressed(true);
                  }

                  this.ut--;
                  boolean usedInvis = this.um == 1 && mc.player.hasStatusEffect(StatusEffects.INVISIBILITY);
                  boolean usedFood = this.um == 2 && !mc.player.getHungerManager().isNotFull();
                  boolean usedSpeed = this.um == 3 && mc.player.hasStatusEffect(StatusEffects.SPEED);
                  if (usedInvis || usedFood || usedSpeed || this.ut <= 0) {
                     if (mc.options != null) {
                        mc.options.useKey.setPressed(false);
                     }

                     if ((this.um == 1 || this.um == 3)
                        && mc.player.currentScreenHandler == mc.player.playerScreenHandler
                        && !mc.player.getMainHandStack().isEmpty()) {
                        mc.interactionManager
                           .clickSlot(
                              mc.player.playerScreenHandler.syncId, 36 + mc.player.getInventory().getSelectedSlot(), 0, SlotActionType.QUICK_MOVE, mc.player
                           );
                     }

                     this.um = 0;
                     this.d = 3;
                  }
               } else {
                  this.ut--;
                  if (this.ut <= 0) {
                     mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                     if (mc.options != null) {
                        mc.options.useKey.setPressed(true);
                     }

                     this.um = this.um == 11 ? 1 : (this.um == 22 ? 2 : 3);
                     this.ut = 40;
                  }
               }
            } else {
               if (this.um == 0 && this.iv == 0 && this.p3 == 0 && !mc.player.getMainHandStack().isEmpty()) {
                  ScreenHandler menu = mc.player.currentScreenHandler;
                  int selected = mc.player.getInventory().getSelectedSlot();
                  int hotbarSlot = 36 + selected;
                  if (menu != mc.player.playerScreenHandler) {
                     int containerSlots = 0;

                     for (int i = 0; i < menu.slots.size(); i++) {
                        Slot slot = (Slot)menu.slots.get(i);
                        if (slot.inventory == mc.player.getInventory()) {
                           break;
                        }

                        containerSlots++;
                     }

                     hotbarSlot = containerSlots + 27 + selected;
                  }

                  if (hotbarSlot >= 0 && hotbarSlot < menu.slots.size()) {
                     mc.interactionManager.clickSlot(menu.syncId, hotbarSlot, 0, SlotActionType.QUICK_MOVE, mc.player);
                     this.d = 2;
                     return;
                  }
               }

               if (this.wr) {
                  if (this.rd > 0) {
                     this.rd--;
                  } else {
                     this.wr = false;
                     this.rst();
                  }
               } else if (this.fs != 0) {
                  this.farmTick();
               } else if (!this.eat()) {
                  boolean pvpActive = Network.isPvp();
                  if (!this.ep && pvpActive && this.danger()) {
                     this.esc();
                  }

                  if (this.ep) {
                     if (pvpActive) {
                        this.escTick();
                        return;
                     }

                     this.escEnd();
                  }

                  if (this.init && this.iv == 0 && this.ss == 0 && this.um == 0) {
                     this.init = false;
                     this.needs();
                     if (!this.ni && !this.ns && !this.nc) {
                        this.farm();
                     } else {
                        this.iv = this.onSk() ? 8 : 7;
                        this.ib = null;
                        this.clr();
                        this.d = 1;
                     }
                  } else if (this.ss == 0 && this.iv == 0 && this.um == 0 && !Network.isPvp() && this.full() && this.hasLoot()) {
                     if (!this.onSk()) {
                        String anName = this.sk.getValue().trim();
                        mc.player.networkHandler.sendChatCommand("an" + anName);
                     }

                     this.all();
                     this.ds = true;
                     this.ss = 1;
                     this.d = 40;
                  } else if (this.d > 0) {
                     this.d--;
                  } else if (this.ss != 0) {
                     this.depTick();
                  } else {
                     boolean hasCarrot = false;
                     int carrotSlot = -1;

                     for (int i = 0; i < 36; i++) {
                        if (mc.player.getInventory().getStack(i).isOf(Items.GOLDEN_CARROT)) {
                           hasCarrot = true;
                           carrotSlot = i;
                           break;
                        }
                     }

                     if (this.invis.getValue() && !mc.player.hasStatusEffect(StatusEffects.INVISIBILITY)
                        || this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED)
                        || this.carrot.getValue() && !hasCarrot
                        || this.carrot.getValue() && hasCarrot && mc.player.getHungerManager().isNotFull()
                        || this.iv != 0) {
                        int invisPotionSlot = -1;
                        int speedPotionSlot = -1;

                        for (int ix = 0; ix < 36; ix++) {
                           ItemStack stack = mc.player.getInventory().getStack(ix);
                           if (invisPotionSlot == -1 && this.invPot(stack)) {
                              invisPotionSlot = ix;
                           }

                           if (speedPotionSlot == -1 && this.spdPot(stack)) {
                              speedPotionSlot = ix;
                           }

                           if (invisPotionSlot != -1 && speedPotionSlot != -1) {
                              break;
                           }
                        }

                        if (mc.player.currentScreenHandler == mc.player.playerScreenHandler
                           && this.iv == 0
                           && invisPotionSlot != -1
                           && this.invis.getValue()
                           && !mc.player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
                           int old = mc.player.getInventory().getSelectedSlot();
                           if (invisPotionSlot >= 9) {
                              mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, invisPotionSlot, old, SlotActionType.SWAP, mc.player);
                              invisPotionSlot = old;
                           }

                           mc.player.getInventory().setSelectedSlot(invisPotionSlot);
                           this.ut = 2;
                           this.um = 11;
                           this.d = 0;
                           return;
                        }

                        if (mc.player.currentScreenHandler == mc.player.playerScreenHandler
                           && this.iv == 0
                           && speedPotionSlot != -1
                           && this.speed.getValue()
                           && !mc.player.hasStatusEffect(StatusEffects.SPEED)) {
                           int old = mc.player.getInventory().getSelectedSlot();
                           if (speedPotionSlot >= 9) {
                              mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, speedPotionSlot, old, SlotActionType.SWAP, mc.player);
                              speedPotionSlot = old;
                           }

                           mc.player.getInventory().setSelectedSlot(speedPotionSlot);
                           this.ut = 2;
                           this.um = 33;
                           this.d = 0;
                           return;
                        }

                        if (this.carrot.getValue()
                           && mc.player.currentScreenHandler == mc.player.playerScreenHandler
                           && this.iv == 0
                           && carrotSlot != -1
                           && mc.player.getHungerManager().isNotFull()) {
                           int old = mc.player.getInventory().getSelectedSlot();
                           if (carrotSlot >= 9) {
                              mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, carrotSlot, old, SlotActionType.SWAP, mc.player);
                              carrotSlot = old;
                           }

                           mc.player.getInventory().setSelectedSlot(carrotSlot);
                           this.ut = 2;
                           this.um = 22;
                           this.d = 0;
                           return;
                        }

                        if (this.iv == 0) {
                           this.ni = this.invis.getValue() && !mc.player.hasStatusEffect(StatusEffects.INVISIBILITY) && invisPotionSlot == -1;
                           this.ns = this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED) && speedPotionSlot == -1;
                           this.nc = this.carrot.getValue() && !hasCarrot;
                           if (!this.ni && !this.ns && !this.nc) {
                              this.d = 5;
                              return;
                           }

                           if (Network.isPvp()) {
                              this.d = 20;
                              return;
                           }

                           this.iv = 7;
                           this.ib = null;
                           this.clr();
                           this.d = 1;
                           return;
                        }

                        if (this.iv == 7) {
                           if (!this.onSk()) {
                              String anName = this.sk.getValue().trim();
                              mc.player.networkHandler.sendChatCommand("an" + anName);
                           }

                           this.ib = null;
                           this.clr();
                           this.iv = 8;
                           this.d = 40;
                           return;
                        }

                        if (this.iv == 8) {
                           if (this.ib == null || !this.box(this.ib)) {
                              this.ib = this.labelBox("еда");
                              this.pos = null;
                              this.mv = false;
                              if (this.ib == null) {
                                 this.d = 10;
                                 return;
                              }
                           }

                           this.tb = this.ib;
                           if (this.pos == null) {
                              this.pos = this.stand(this.ib);
                              if (this.pos == null) {
                                 this.d = 10;
                                 return;
                              }
                           }

                           if (mc.player.squaredDistanceTo(Vec3d.ofCenter(this.pos)) > 2.25) {
                              if (!this.mv) {
                                 this.go(this.pos);
                                 this.mv = true;
                              }

                              this.d = 10;
                              return;
                           }

                           if (this.mv) {
                              this.stop();
                           }

                           this.so = true;
                           this.mv = false;
                           this.iv = 2;
                           this.d = 3;
                           return;
                        }

                        if (this.iv == 1) {
                           this.ib = this.tb;
                           if (this.ib == null) {
                              this.d = 10;
                              return;
                           }

                           this.tb = this.ib;
                           this.so = true;
                           this.mv = false;
                           this.d = 3;
                           this.iv = 2;
                           return;
                        }

                        if (this.iv == 2) {
                           if (this.ib == null) {
                              this.iv = 0;
                              this.d = 10;
                              return;
                           }

                           this.tb = this.ib;
                           Vec3d vec = Vec3d.ofCenter(this.ib).add(0.0, 0.5, 0.0);
                           mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(vec, Direction.UP, this.ib, false));
                           this.d = 3;
                           this.iv = 3;
                           return;
                        }

                        if (this.iv == 3) {
                           if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menux)) {
                              if (++this.sr >= 4) {
                                 this.sr = 0;
                                 this.iv = 2;
                                 this.d = 2;
                              } else {
                                 this.d = 3;
                              }

                              return;
                           }

                           this.sr = 0;
                           int var41 = menux.getRows() * 9;
                           if (this.take(menux)) {
                              return;
                           }

                           if (!this.ni && !this.ns && !this.nc) {
                              mc.player.closeHandledScreen();
                              this.iv = 4;
                              this.d = 3;
                              return;
                           }

                           int itemSlot = -1;
                           boolean foundInvisPotion = false;
                           boolean foundSpeedPotion = false;
                           boolean foundCarrot = false;

                           for (int ix = 0; ix < var41; ix++) {
                              ItemStack stackx = ((Slot)menux.slots.get(ix)).getStack();
                              if (this.ni && this.invPot(stackx)) {
                                 itemSlot = ix;
                                 foundInvisPotion = true;
                                 break;
                              }

                              if (this.ns && this.spdPot(stackx)) {
                                 itemSlot = ix;
                                 foundSpeedPotion = true;
                                 break;
                              }

                              if (this.nc && stackx.isOf(Items.GOLDEN_CARROT)) {
                                 itemSlot = ix;
                                 foundCarrot = true;
                                 break;
                              }
                           }

                           if (itemSlot == -1) {
                              mc.player.closeHandledScreen();
                              this.iv = 4;
                              this.d = 20;
                              return;
                           }

                           int bar = -1;

                           for (int ix = 0; ix < 9; ix++) {
                              if (mc.player.getInventory().getStack(ix).isEmpty()) {
                                 bar = ix;
                                 break;
                              }
                           }

                           if (bar == -1) {
                              bar = mc.player.getInventory().getSelectedSlot();
                              int emptyInventorySlot = this.empty(menux, var41);
                              if (emptyInventorySlot == -1) {
                                 mc.player.closeHandledScreen();
                                 this.d = 10;
                                 return;
                              }

                              this.mvSlot(menux.syncId, var41 + 27 + bar, emptyInventorySlot);
                              this.d = 3;
                              return;
                           }

                           int targetSlot = var41 + 27 + bar;
                           this.sch(mc.player.currentScreenHandler.syncId, itemSlot, targetSlot, foundInvisPotion, foundSpeedPotion, foundCarrot);
                           this.d = 0;
                           return;
                        }

                        if (this.iv == 4) {
                           this.iv = 0;
                           this.farm();
                           return;
                        }

                        if (this.iv == 5) {
                           this.iv = 0;
                           this.d = 20;
                           return;
                        }

                        if (this.iv == 6) {
                           this.iv = 0;
                           this.d = 20;
                           return;
                        }
                     }

                     ScreenHandler menux = mc.player.currentScreenHandler;
                     if (menux != mc.player.playerScreenHandler) {
                        if (this.ob != null) {
                           this.loot(menux);
                        } else {
                           mc.player.closeHandledScreen();
                           this.d = 5;
                        }
                     } else {
                        boolean needRetarget = this.tb == null || this.skip(this.tb) || !this.box(this.tb);
                        if (needRetarget) {
                           this.rt = 0;
                           BlockPos bestBox = this.bestBox();
                           if (bestBox == null) {
                              this.clr();
                              this.d = 20;
                              return;
                           }

                           if (this.tb == null || !this.tb.equals(bestBox)) {
                              if (this.mv) {
                                 this.stop();
                              }

                              this.tb = bestBox;
                              this.pos = null;
                              this.mv = false;
                              this.so = false;
                              this.d = 1;
                              return;
                           }
                        }

                        if (this.pos == null) {
                           this.pos = this.stand(this.tb);
                           this.mv = false;
                           this.so = false;
                           if (this.pos == null) {
                              this.ig.add(this.tb);
                              this.tb = null;
                              this.d = 5;
                              return;
                           }
                        }

                        int targetTimerSeconds = this.timer(this.tb);
                        if (targetTimerSeconds > 20) {
                           this.stop();
                           this.clr();
                           this.d = 1;
                        } else if (mc.player.squaredDistanceTo(Vec3d.ofCenter(this.pos)) > 2.25) {
                           if (!this.mv) {
                              this.go(this.pos);
                              this.mv = true;
                              this.so = false;
                           }

                           this.d = 10;
                        } else if (!this.so) {
                           this.stop();
                           this.keys();
                           this.mv = false;
                           this.so = true;
                           this.d = 3;
                        } else if (this.timer(this.tb) > 0) {
                           this.dt.add(this.tb);
                           this.clr();
                           this.d = 1;
                        } else {
                           this.ob = this.tb;
                           Vec3d hit = Vec3d.ofCenter(this.ob).add(0.0, 0.5, 0.0);
                           mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, this.ob, false));
                           this.d = 3;
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private void depTick() {
      ScreenHandler menu = mc.player.currentScreenHandler;
      if (menu != mc.player.playerScreenHandler) {
         this.dep(menu);
      } else {
         boolean needRetarget = this.tb == null || this.skip(this.tb) || !this.box(this.tb);
         if (needRetarget) {
            BlockPos farBox = this.labelBox("ресы");
            if (farBox == null) {
               this.d = 10;
            } else {
               this.tb = farBox;
               this.pos = null;
               this.mv = false;
               this.so = false;
               this.d = 1;
            }
         } else {
            if (this.pos == null) {
               this.pos = this.stand(this.tb);
               this.mv = false;
               this.so = false;
               if (this.pos == null) {
                  this.ig.add(this.tb);
                  this.tb = null;
                  this.d = 5;
                  return;
               }
            }

            if (mc.player.squaredDistanceTo(Vec3d.ofCenter(this.pos)) > 2.25) {
               if (!this.mv) {
                  this.go(this.pos);
                  this.mv = true;
                  this.so = false;
               }

               this.d = 10;
            } else if (!this.so) {
               this.stop();
               this.keys();
               this.mv = false;
               this.so = true;
               this.d = 3;
            } else {
               this.ob = this.tb;
               Vec3d hit = Vec3d.ofCenter(this.ob).add(0.0, 0.5, 0.0);
               mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, this.ob, false));
               this.d = 3;
            }
         }
      }
   }

   private void dep(ScreenHandler menu) {
      int containerSlots = 0;

      for (int i = 0; i < menu.slots.size(); i++) {
         Slot slot = (Slot)menu.slots.get(i);
         if (slot.inventory == mc.player.getInventory()) {
            break;
         }

         containerSlots++;
      }

      int moved = 0;

      for (int i = containerSlots; i < menu.slots.size(); i++) {
         Slot slot = (Slot)menu.slots.get(i);
         if (slot.hasStack() && this.Ite(slot.getStack())) {
            mc.interactionManager.clickSlot(menu.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            if (++moved >= 6) {
               this.d = 0;
               return;
            }
         }
      }

      if (moved > 0) {
         this.d = 0;
      } else {
         mc.player.closeHandledScreen();
         this.all();
         this.ds = false;
         this.ss = 0;
         this.rt = 0;
         this.needs();
         if (!this.ni && !this.ns && !this.nc) {
            this.farm();
         } else {
            this.iv = 8;
            this.ib = null;
            this.d = 5;
         }
      }
   }

   private void mvSlot(int containerId, int sourceSlot, int targetSlot) {
      mc.interactionManager.clickSlot(containerId, sourceSlot, 0, SlotActionType.PICKUP, mc.player);
      mc.interactionManager.clickSlot(containerId, targetSlot, 0, SlotActionType.PICKUP, mc.player);
   }

   private void sch(int containerId, int sourceSlot, int targetSlot, boolean invisPotion, boolean speedPotion, boolean goldenCarrot) {
      this.p0 = containerId;
      this.p1 = sourceSlot;
      this.p2 = targetSlot;
      this.p3 = 1;
      this.pI = invisPotion;
      this.pS = speedPotion;
      this.pC = goldenCarrot;
   }

   private boolean take(GenericContainerScreenHandler menu) {
      if (this.p3 == 0) {
         return false;
      } else if (menu.syncId != this.p0) {
         this.takeClr();
         return false;
      } else {
         if (this.p3 == 1) {
            ItemStack sourceStack = ((Slot)menu.slots.get(this.p1)).getStack();
            this.pR = (this.pI || this.pS) && sourceStack.getCount() > 1;
            mc.interactionManager.clickSlot(menu.syncId, this.p1, 0, SlotActionType.PICKUP, mc.player);
            this.p3 = 2;
         } else if (this.p3 == 2) {
            mc.interactionManager.clickSlot(menu.syncId, this.p2, !this.pC && !this.pR ? 0 : 1, SlotActionType.PICKUP, mc.player);
            if (this.pC) {
               ItemStack targetStack = ((Slot)menu.slots.get(this.p2)).getStack();
               if (menu.getCursorStack().isEmpty()) {
                  this.takeEnd();
               } else if (!targetStack.isEmpty() && targetStack.getCount() >= targetStack.getMaxCount()) {
                  this.p3 = 3;
               }
            } else if (this.pR) {
               this.p3 = 3;
            } else {
               this.takeEnd();
            }
         } else {
            mc.interactionManager.clickSlot(menu.syncId, this.p1, 0, SlotActionType.PICKUP, mc.player);
            this.takeEnd();
         }

         this.d = 2;
         return true;
      }
   }

   private void takeEnd() {
      if (this.pI) {
         this.ni = false;
      }

      if (this.pS) {
         this.ns = false;
      }

      if (this.pC) {
         this.nc = false;
      }

      this.takeClr();
   }

   private void takeClr() {
      this.p0 = -1;
      this.p1 = -1;
      this.p2 = -1;
      this.p3 = 0;
      this.pI = false;
      this.pS = false;
      this.pC = false;
      this.pR = false;
   }

   private int empty(ScreenHandler menu, int containerSlots) {
      for (int slot = containerSlots; slot < containerSlots + 27 && slot < menu.slots.size(); slot++) {
         if (!((Slot)menu.slots.get(slot)).hasStack()) {
            return slot;
         }
      }

      return -1;
   }

   private void loot(ScreenHandler menu) {
      int containerSlots = 0;

      for (int i = 0; i < menu.slots.size(); i++) {
         Slot slot = (Slot)menu.slots.get(i);
         if (slot.inventory == mc.player.getInventory()) {
            break;
         }

         containerSlots++;
      }

      int moved = 0;

      for (int i = 0; i < containerSlots; i++) {
         Slot slot = (Slot)menu.slots.get(i);
         if (slot.hasStack() && this.Ite(slot.getStack()) && this.room(slot.getStack())) {
            mc.interactionManager.clickSlot(menu.syncId, i, 0, SlotActionType.QUICK_MOVE, mc.player);
            if (++moved >= 6) {
               this.d = 0;
               return;
            }
         }
      }

      if (moved > 0) {
         this.d = 0;
      } else {
         if (this.ob != null) {
            this.ig.add(this.ob);
         }

         mc.player.closeHandledScreen();
         this.all();
         this.d = 5;
         this.rt = 0;
      }
   }

   private boolean invPot(ItemStack stack) {
      if (stack != null && (stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION))) {
         PotionContentsComponent potion = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (potion == null) {
            return false;
         } else {
            for (StatusEffectInstance effect : potion.getEffects()) {
               if (effect.getEffectType().matches(StatusEffects.INVISIBILITY)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean hasLoot() {
      for (int i = 0; i < 36; i++) {
         if (this.Ite(mc.player.getInventory().getStack(i))) {
            return true;
         }
      }

      return false;
   }

   private boolean spdPot(ItemStack stack) {
      if (stack != null && (stack.isOf(Items.POTION) || stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION))) {
         PotionContentsComponent potion = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (potion == null) {
            return false;
         } else {
            for (StatusEffectInstance effect : potion.getEffects()) {
               if (effect.getEffectType().matches(StatusEffects.SPEED)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean eat() {
      if (this.carrot.getValue() && mc.player.getHungerManager().isNotFull() && mc.player.currentScreenHandler == mc.player.playerScreenHandler) {
         int carrotSlot = -1;

         for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.GOLDEN_CARROT)) {
               carrotSlot = i;
               break;
            }
         }

         if (carrotSlot == -1) {
            return false;
         } else {
            int selected = mc.player.getInventory().getSelectedSlot();
            if (carrotSlot >= 9) {
               mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, carrotSlot, selected, SlotActionType.SWAP, mc.player);
               carrotSlot = selected;
            }

            mc.player.getInventory().setSelectedSlot(carrotSlot);
            this.ut = 2;
            this.um = 22;
            this.d = 0;
            return true;
         }
      } else {
         return false;
      }
   }

   private int count() {
      int count = 0;

      for (int i = 0; i < 36; i++) {
         if (this.Ite(mc.player.getInventory().getStack(i))) {
            count++;
         }
      }

      return count;
   }

   private boolean full() {
      for (int i = 0; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).isEmpty()) {
            return false;
         }
      }

      return true;
   }

   private boolean room(ItemStack incoming) {
      for (int i = 0; i < 36; i++) {
         ItemStack current = mc.player.getInventory().getStack(i);
         if (current.isEmpty()) {
            return true;
         }

         if (ItemStack.areItemsAndComponentsEqual(current, incoming) && current.getCount() < current.getMaxCount()) {
            return true;
         }
      }

      return false;
   }

   private boolean danger() {
      if (this.count() >= 5) {
         return true;
      } else {
         boolean missingInvis = this.invis.getValue() && !mc.player.hasStatusEffect(StatusEffects.INVISIBILITY) && !this.hasInv(this::invPot);
         boolean missingSpeed = this.speed.getValue() && !mc.player.hasStatusEffect(StatusEffects.SPEED) && !this.hasInv(this::spdPot);
         return missingInvis || missingSpeed;
      }
   }

   private boolean hasInv(Predicate<ItemStack> predicate) {
      for (int i = 0; i < 36; i++) {
         if (predicate.test(mc.player.getInventory().getStack(i))) {
            return true;
         }
      }

      return false;
   }

   private void esc() {
      this.ep = true;
      this.et = null;
      this.pr = 0;
      this.eh = Math.toRadians((double)mc.player.getYaw());
      this.all();
      this.ss = 0;
      this.ds = false;
      this.takeClr();
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
         mc.player.closeHandledScreen();
      }

      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      this.ut = 0;
      this.um = 0;
   }

   private void escTick() {
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
         mc.player.closeHandledScreen();
      }

      int tick = mc.player.age;
      if (this.et == null || tick >= this.pr || mc.player.squaredDistanceTo(Vec3d.ofCenter(this.et)) < 6.25) {
         int distance = 32;
         this.et = this.escPos(this.eh, distance);
         if (this.et == null) {
            this.eh += Math.PI;
            this.et = this.escPos(this.eh, distance);
         }

         if (this.et == null) {
            this.et = mc.player
               .getBlockPos()
               .add((int)Math.round(-Math.sin(this.eh) * (double)distance), 0, (int)Math.round(Math.cos(this.eh) * (double)distance));
         }

         this.pr = tick + 200;
         this.go(this.et);
         this.mv = true;
      }
   }

   private BlockPos escPos(double heading, int distance) {
      BlockPos current = mc.player.getBlockPos();
      int targetX = current.getX() + (int)Math.round(-Math.sin(heading) * (double)distance);
      int targetZ = current.getZ() + (int)Math.round(Math.cos(heading) * (double)distance);

      for (int radius = 0; radius <= 4; radius++) {
         for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
               if (radius <= 0 || Math.abs(x) == radius || Math.abs(z) == radius) {
                  for (int y = 3; y >= -3; y--) {
                     BlockPos candidate = new BlockPos(targetX + x, current.getY() + y, targetZ + z);
                     if (this.walk(candidate)) {
                        return candidate;
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   private boolean walk(BlockPos pos) {
      return mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.up()).isAir() && !mc.world.getBlockState(pos.down()).isAir();
   }

   private void escEnd() {
      this.stop();
      this.ep = false;
      this.et = null;
      this.pr = 0;
      this.eh = 0.0;
      this.mv = false;
      this.so = false;
      this.keys();
      this.d = 1;
   }

   private boolean onSk() {
      try {
         return Network.getAnarchyMode() == Integer.parseInt(this.sk.getValue().trim());
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   private void needs() {
      boolean hasInvisPotion = false;
      boolean hasSpeedPotion = false;
      boolean hasCarrot = false;

      for (int i = 0; i < 36; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         hasInvisPotion |= this.invPot(stack);
         hasSpeedPotion |= this.spdPot(stack);
         hasCarrot |= stack.isOf(Items.GOLDEN_CARROT);
      }

      this.ni = this.invis.getValue() && !hasInvisPotion;
      this.ns = this.speed.getValue() && !hasSpeedPotion;
      this.nc = this.carrot.getValue() && !hasCarrot;
   }

   private void anGo() {
      String anName = this.an.getValue().trim();
      mc.player.networkHandler.sendChatCommand("an" + anName);
   }

   private void barCfg() {
      if (mc.player != null) {
         mc.player.networkHandler.sendChatMessage("#set allowSprint true");
         mc.player.networkHandler.sendChatMessage("#set allowParkour true");
         mc.player.networkHandler.sendChatMessage("#set allowParkourAscend true");
         mc.player.networkHandler.sendChatMessage("#set allowDiagonalAscend true");
         mc.player.networkHandler.sendChatMessage("#set allowJumpAt256 true");
         mc.player.networkHandler.sendChatMessage("#set assumeStep true");
      }
   }

   private void barJump() {
      if (this.bm && mc.player.horizontalCollision && mc.player.isOnGround()) {
         mc.player.jump();
      }
   }

   private void go(BlockPos destination) {
      if (destination == null || mc.player == null) {
         this.stop();
      } else if (!this.bm || !destination.equals(this.bt)) {
         this.stop();
         this.bt = destination.toImmutable();
         this.bm = true;
         mc.player.networkHandler.sendChatMessage("#goto " + destination.getX() + " " + destination.getY() + " " + destination.getZ());
      }
   }

   private void stop() {
      if (this.bm && mc.player != null) {
         mc.player.networkHandler.sendChatMessage("#stop");
         mc.player.networkHandler.sendChatMessage("#cancel");
      }

      this.bm = false;
      this.bt = null;
   }

   private void farm() {
      this.stop();
      this.keys();
      if (mc.player.currentScreenHandler != mc.player.playerScreenHandler) {
         mc.player.closeHandledScreen();
      }

      this.all();
      this.anGo();
      this.fs = 1;
      this.fd = 40;
      this.fw = 0;
   }

   private void farmTick() {
      this.stop();
      this.keys();
      if (this.fd > 0) {
         this.fd--;
      } else if (this.fs == 1) {
         if (!this.onAn() && this.fw < 100) {
            this.fw += 5;
            this.fd = 5;
         } else {
            String homeName = this.home.getValue().trim();
            mc.player.networkHandler.sendChatCommand("home " + homeName);
            this.fs = 2;
            this.fd = 60;
            this.fw = 0;
         }
      } else {
         this.fs = 0;
         this.fd = 0;
         this.ig.clear();
         this.dt.clear();
         this.all();
         this.d = 5;
      }
   }

   private boolean onAn() {
      try {
         return Network.getAnarchyMode() == Integer.parseInt(this.an.getValue().trim());
      } catch (NumberFormatException var2) {
         return false;
      }
   }

   private BlockPos labelBox(String label) {
      BlockPos me = mc.player.getBlockPos();
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;
      int radius = 64;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos chest = me.add(x, y, z);
               if (this.box(chest) && this.stand(chest) != null && this.label(chest, label)) {
                  double distance = mc.player.squaredDistanceTo(Vec3d.ofCenter(chest));
                  if (distance < bestDistance) {
                     best = chest;
                     bestDistance = distance;
                  }
               }
            }
         }
      }

      return best;
   }

   private boolean label(BlockPos chest, String label) {
      String expected = this.norm(label);

      for (int x = -2; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
               BlockPos signPos = chest.add(x, y, z);
               if (mc.world.getBlockEntity(signPos) instanceof SignBlockEntity sign && this.sign(sign, expected) && this.nearSign(chest, signPos)) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private boolean sign(SignBlockEntity sign, String expected) {
      for (boolean front : new boolean[]{true, false}) {
         for (Text line : sign.getText(front).getMessages(false)) {
            if (this.norm(line.getString()).equals(expected)) {
               return true;
            }
         }
      }

      return false;
   }

   private boolean nearSign(BlockPos chest, BlockPos signPos) {
      double chestDistance = signPos.getSquaredDistance(chest);

      for (int x = -2; x <= 2; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -2; z <= 2; z++) {
               BlockPos other = signPos.add(x, y, z);
               if (!other.equals(chest) && this.box(other) && signPos.getSquaredDistance(other) < chestDistance) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private String norm(String text) {
      return text.toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("[^а-яa-z0-9]+", "");
   }

   private BlockPos bestBox() {
      BlockPos me = mc.player.getBlockPos();
      BlockPos best = null;
      int bestScore = Integer.MAX_VALUE;
      double bestDistance = Double.MAX_VALUE;
      int radius = 64;

      for (int x = -radius; x <= radius; x++) {
         for (int y = -radius; y <= radius; y++) {
            for (int z = -radius; z <= radius; z++) {
               BlockPos pos = me.add(x, y, z);
               if (this.box(pos) && this.stand(pos) != null) {
                  int timerSeconds = this.timer(pos);
                  if (this.dt.contains(pos)) {
                     if (timerSeconds > 0) {
                        continue;
                     }

                     this.dt.remove(pos);
                  }

                  if ((!this.ig.contains(pos) || timerSeconds > 0) && timerSeconds <= 20) {
                     int score = timerSeconds == 0 ? 0 : timerSeconds;
                     double distance = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
                     if (score < bestScore || score == bestScore && distance < bestDistance) {
                        best = pos;
                        bestScore = score;
                        bestDistance = distance;
                     }
                  }
               }
            }
         }
      }

      return best;
   }

   private BlockPos stand(BlockPos box) {
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (Direction direction : Type.HORIZONTAL) {
         BlockPos pos = box.offset(direction);
         if (mc.world.getBlockState(pos).isAir() && mc.world.getBlockState(pos.up()).isAir() && !mc.world.getBlockState(pos.down()).isAir()) {
            double distance = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos));
            if (distance < bestDistance) {
               best = pos;
               bestDistance = distance;
            }
         }
      }

      return best;
   }

   private boolean Ite(ItemStack state) {
      return state.getItem() == Items.TOTEM_OF_UNDYING
         || state.getItem() == Items.SPLASH_POTION
         || state.getItem() == Items.TRIPWIRE_HOOK
         || state.getItem() == Items.ENDER_PEARL
         || state.getItem() == Items.GOLDEN_APPLE;
   }

   private boolean box(BlockPos pos) {
      BlockState state = mc.world.getBlockState(pos);
      return state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST);
   }

   private boolean skip(BlockPos pos) {
      return this.ig.contains(pos) && this.timer(pos) <= 0;
   }

   private int timer(BlockPos pos) {
      int closestTimer = 0;

      for (WardenHelper.SavedTimer timer : WardenHelper.getTimers().values()) {
         if (!(timer.pos().squaredDistanceTo(Vec3d.ofCenter(pos)) > 100.0) && this.nearTimer(pos, timer.pos())) {
            int left = timer.leftSeconds();
            if (left > 0 && (closestTimer == 0 || left < closestTimer)) {
               closestTimer = left;
            }
         }
      }

      return closestTimer;
   }

   private boolean nearTimer(BlockPos candidate, Vec3d timerPos) {
      double candidateDistance = timerPos.squaredDistanceTo(Vec3d.ofCenter(candidate));
      BlockPos center = BlockPos.ofFloored(timerPos);

      for (int x = -10; x <= 10; x++) {
         for (int y = -10; y <= 10; y++) {
            for (int z = -10; z <= 10; z++) {
               BlockPos other = center.add(x, y, z);
               if (!other.equals(candidate) && this.box(other) && timerPos.squaredDistanceTo(Vec3d.ofCenter(other)) < candidateDistance) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private void clr() {
      this.tb = this.pos = null;
      this.mv = this.so = false;
   }

   private void all() {
      this.clr();
      this.ob = null;
   }

   private void keys() {
      if (mc.options != null) {
         mc.options.forwardKey.setPressed(false);
         mc.options.backKey.setPressed(false);
         mc.options.jumpKey.setPressed(false);
         mc.options.leftKey.setPressed(false);
         mc.options.rightKey.setPressed(false);
         mc.options.sprintKey.setPressed(false);
         if (mc.player != null) {
            mc.player.setSprinting(false);
         }
      }
   }

   private void rst() {
      this.stop();
      this.mv = false;
      this.so = false;
      this.d = 0;
      this.rt = 0;
      this.ob = null;
      this.tb = null;
      this.pos = null;
      this.iv = 0;
      this.ib = null;
      this.ni = false;
      this.ns = false;
      this.nc = false;
      this.sr = 0;
      this.ut = 0;
      this.um = 0;
      this.ss = 0;
      this.ds = false;
      this.fs = 0;
      this.fd = 0;
      this.fw = 0;
      this.ep = false;
      this.et = null;
      this.pr = 0;
      this.eh = 0.0;
      this.takeClr();
      this.dt.clear();
      if (mc.options != null) {
         mc.options.useKey.setPressed(false);
      }

      this.keys();
   }
}
