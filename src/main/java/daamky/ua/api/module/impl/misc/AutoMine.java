package daamky.ua.api.module.impl.misc;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import daamky.ua.api.drag.impl.Notifications;
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
import daamky.ua.api.module.impl.combat.aura.target.RaycastAngle;
import daamky.ua.api.module.impl.combat.aura.util.TaskPriority;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.mixin.accessor.MultiPlayerGameModeAccessor;
import daamky.ua.utils.inventory.InventoryTask;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public final class AutoMine extends Module {
   private static final double REACH = 4.5;
   private static final int BREAK_COOLDOWN_TICKS = 0;
   private static final Set<AutoMine.Stage> REPAIR_STAGES = Set.of(
      AutoMine.Stage.FIND_EXPERIENCE_CHEST,
      AutoMine.Stage.OPEN_EXPERIENCE_CHEST,
      AutoMine.Stage.TAKE_EXPERIENCE,
      AutoMine.Stage.PREPARE_REPAIR,
      AutoMine.Stage.REPAIR_TOOL
   );
   private final Set<BlockPos> checkedChests = new HashSet<>();
   private final Set<BlockPos> checkedBoneChests = new HashSet<>();
   private final Set<BlockPos> checkedExperienceChests = new HashSet<>();
   private final NumberSetting minToolDurability = this.register(
      new NumberSetting("Min Durability", "Minimum remaining tool durability.", 10.0, 1.0, 100.0, 1.0)
   );
   private final NumberSetting breakSpeed = this.register(new NumberSetting("Break Speed", "Block breaking speed multiplier.", 5.0, 1.0, 10.0, 0.25));
   private AutoMine.Stage stage = AutoMine.Stage.PLANT;
   private BlockPos plantPos;
   private BlockPos chestPos;
   private BlockPos experienceChestPos;
   private BlockPos target;
   private BlockPos breaking;
   private int delay;
   private int craftStep;
   private int craftBoneSlot;
   private int boneStacksTaken;
   private int repairToolSlot;
   private int repairPreviousSelectedSlot;
   private int repairBottleSourceSlot;
   private int repairBottleHotbarSlot;
   private boolean plantRotationPrepared;
   private boolean boneMealStackTaken;
   private boolean toolDurabilityWarningShown;
   private boolean repairToolInOffhand;
   private boolean repairRotationPrepared;
   private boolean experienceStackTaken;
   private boolean experienceChestWarningShown;
   private AutoMine.Stage repairReturnStage;
   private float previousYaw;
   private float previousPitch;
   private boolean previousRotationCaptured;

   public AutoMine() {
      super("Apple Farm", "", ModuleCategory.PLAYER);
   }

   @Override
   protected void onEnable() {
      if (mc.player != null) {
         this.previousYaw = mc.player.getYaw();
         this.previousPitch = mc.player.getPitch();
         this.previousRotationCaptured = true;
      } else {
         this.previousRotationCaptured = false;
      }

      this.stage = AutoMine.Stage.PLANT;
      this.plantPos = null;
      this.chestPos = null;
      this.experienceChestPos = null;
      this.target = null;
      this.breaking = null;
      this.delay = 0;
      this.craftStep = 0;
      this.craftBoneSlot = -1;
      this.boneStacksTaken = 0;
      this.repairToolSlot = -1;
      this.repairPreviousSelectedSlot = -1;
      this.repairBottleSourceSlot = -1;
      this.repairBottleHotbarSlot = -1;
      this.plantRotationPrepared = false;
      this.boneMealStackTaken = false;
      this.toolDurabilityWarningShown = false;
      this.repairToolInOffhand = false;
      this.repairRotationPrepared = false;
      this.experienceStackTaken = false;
      this.experienceChestWarningShown = false;
      this.repairReturnStage = AutoMine.Stage.BREAK_LOGS;
      this.checkedChests.clear();
      this.checkedBoneChests.clear();
      this.checkedExperienceChests.clear();
   }

   @Override
   protected void onDisable() {
      this.restoreRepairInventory(true);
      this.resetBreaking(true);
      if (this.previousRotationCaptured && mc.player != null) {
         mc.player.setYaw(this.previousYaw);
         mc.player.setPitch(this.previousPitch);
      }

      this.previousRotationCaptured = false;
   }

   @SubscribeEvent
   private void onTick(TickEvent.Post event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null) {
         if (this.delay > 0) {
            this.delay--;
         }

         if (this.plantPos == null) {
            this.plantPos = mc.player.getBlockPos().offset(mc.player.getHorizontalFacing(), 2);
         }

         BlockPos ground = this.plantPos.down();
         Vec3d groundTop = new Vec3d((double)ground.getX() + 0.5, (double)ground.getY() + 1.0, (double)ground.getZ() + 0.5);
         Angle groundAngle = MathAngle.fromTo(mc.player.getEyePos(), groundTop);
         mc.player.setYaw(groundAngle.getYaw());
         mc.player.setPitch(groundAngle.getPitch());
         if (this.stage == AutoMine.Stage.OPEN_CHEST && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            this.stage = AutoMine.Stage.TAKE_BONE_MEAL;
         }

         if (this.stage == AutoMine.Stage.OPEN_BONE_CHEST && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            this.stage = AutoMine.Stage.TAKE_BONES;
         }

         if (this.stage == AutoMine.Stage.OPEN_EXPERIENCE_CHEST && mc.player.currentScreenHandler instanceof GenericContainerScreenHandler) {
            this.stage = AutoMine.Stage.TAKE_EXPERIENCE;
         }

         if (this.stage == AutoMine.Stage.TAKE_BONE_MEAL) {
            if (!(mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menu) || this.delay > 0) {
               return;
            }

            if (!this.boneMealStackTaken) {
               int chestSlots = menu.getRows() * 9;

               for (int slot = 0; slot < chestSlots; slot++) {
                  if (((Slot)menu.slots.get(slot)).getStack().isOf(Items.BONE_MEAL)) {
                     InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE);
                     this.boneMealStackTaken = true;
                     this.delay = 2;
                     return;
                  }
               }

               mc.player.closeHandledScreen();
               if (this.chestPos != null) {
                  this.checkedChests.add(this.chestPos);
               }

               this.chestPos = null;
               this.stage = AutoMine.Stage.FIND_CHEST;
               this.delay = 3;
            } else {
               mc.player.closeHandledScreen();
               this.boneMealStackTaken = false;
               boolean receivedBoneMeal = InventoryTask.findHotbarSlot(Items.BONE_MEAL) != -1 || InventoryTask.findInventorySlot(Items.BONE_MEAL) != -1;
               if (!receivedBoneMeal && this.chestPos != null) {
                  this.checkedChests.add(this.chestPos);
               }

               this.chestPos = null;
               this.stage = receivedBoneMeal ? AutoMine.Stage.GROW : AutoMine.Stage.FIND_CHEST;
               this.delay = 3;
            }
         } else if (this.stage == AutoMine.Stage.TAKE_BONES) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menu && this.delay <= 0) {
               if (this.boneStacksTaken < 2) {
                  int chestSlots = menu.getRows() * 9;

                  for (int slotx = 0; slotx < chestSlots; slotx++) {
                     if (((Slot)menu.slots.get(slotx)).getStack().isOf(Items.BONE)) {
                        InventoryTask.clickSlot(slotx, 0, SlotActionType.QUICK_MOVE);
                        this.boneStacksTaken++;
                        this.delay = 2;
                        return;
                     }
                  }
               }

               mc.player.closeHandledScreen();
               if (this.boneStacksTaken < 2) {
                  if (this.chestPos != null) {
                     this.checkedBoneChests.add(this.chestPos);
                  }

                  this.chestPos = null;
                  this.stage = AutoMine.Stage.FIND_BONE_CHEST;
                  this.delay = 3;
                  return;
               }

               boolean hasBone = InventoryTask.findHotbarSlot(Items.BONE) != -1 || InventoryTask.findInventorySlot(Items.BONE) != -1;
               if (hasBone) {
                  this.stage = AutoMine.Stage.CRAFT_BONE_MEAL;
                  this.craftStep = 0;
                  this.craftBoneSlot = -1;
               } else {
                  this.chestPos = null;
                  this.stage = AutoMine.Stage.FIND_BONE_CHEST;
               }

               this.boneStacksTaken = 0;
               this.delay = 3;
               return;
            }
         } else if (this.stage == AutoMine.Stage.TAKE_EXPERIENCE) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler menu) {
               if (this.delay <= 0) {
                  if (this.experienceStackTaken) {
                     BlockPos openedChest = this.experienceChestPos;
                     mc.player.closeHandledScreen();
                     this.experienceStackTaken = false;
                     this.experienceChestPos = null;
                     boolean receivedExperience = this.hasExperienceBottles();
                     if (!receivedExperience && openedChest != null) {
                        this.checkedExperienceChests.add(openedChest);
                     }

                     this.stage = receivedExperience ? AutoMine.Stage.PREPARE_REPAIR : AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                     this.delay = 3;
                  } else {
                     int chestSlots = menu.getRows() * 9;

                     for (int slotxx = 0; slotxx < chestSlots; slotxx++) {
                        if (((Slot)menu.slots.get(slotxx)).getStack().isOf(Items.EXPERIENCE_BOTTLE)) {
                           InventoryTask.clickSlot(slotxx, 0, SlotActionType.QUICK_MOVE);
                           this.experienceStackTaken = true;
                           this.delay = 2;
                           return;
                        }
                     }

                     mc.player.closeHandledScreen();
                     if (this.experienceChestPos != null) {
                        this.checkedExperienceChests.add(this.experienceChestPos);
                     }

                     this.experienceChestPos = null;
                     this.stage = AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                     this.delay = 3;
                     Notifications.push("Auto Mine", "В сундуке «опыт» нет пузырьков опыта.");
                     this.experienceChestWarningShown = true;
                  }
               }
            } else {
               if (this.delay <= 0) {
                  this.experienceStackTaken = false;
                  this.experienceChestPos = null;
                  this.stage = this.hasExperienceBottles() ? AutoMine.Stage.PREPARE_REPAIR : AutoMine.Stage.FIND_EXPERIENCE_CHEST;
               }
            }
         } else if (this.stage == AutoMine.Stage.PREPARE_REPAIR) {
            if (this.delay <= 0 && mc.currentScreen == null) {
               this.prepareRepair();
            }
         } else if (this.stage == AutoMine.Stage.CRAFT_BONE_MEAL) {
            if (this.delay <= 0) {
               boolean hasBoneMeal = InventoryTask.findHotbarSlot(Items.BONE_MEAL) != -1 || InventoryTask.findInventorySlot(Items.BONE_MEAL) != -1;
               if (hasBoneMeal) {
                  this.craftStep = 0;
                  this.craftBoneSlot = -1;
                  this.stage = AutoMine.Stage.GROW;
               } else {
                  if (this.craftStep == 0) {
                     this.craftBoneSlot = InventoryTask.findInventorySlot(Items.BONE);
                     if (this.craftBoneSlot == -1) {
                        int hotbarSlot = InventoryTask.findHotbarSlot(Items.BONE);
                        if (hotbarSlot != -1) {
                           this.craftBoneSlot = 36 + hotbarSlot;
                        }
                     }
                  }

                  if (this.craftBoneSlot == -1) {
                     this.craftStep = 0;
                     this.chestPos = null;
                     this.stage = AutoMine.Stage.FIND_BONE_CHEST;
                  } else {
                     switch (this.craftStep) {
                        case 0:
                           InventoryTask.clickSlot(this.craftBoneSlot, 0, SlotActionType.PICKUP);
                           this.craftStep = 1;
                           break;
                        case 1:
                           InventoryTask.clickSlot(1, 0, SlotActionType.PICKUP);
                           this.craftStep = 2;
                           break;
                        case 2:
                           InventoryTask.clickSlot(0, 0, SlotActionType.QUICK_MOVE);
                           this.craftStep = 3;
                           break;
                        case 3:
                           this.craftStep = 0;
                           this.craftBoneSlot = -1;
                           this.stage = AutoMine.Stage.GROW;
                     }

                     this.delay = 2;
                  }
               }
            }
         } else if (mc.currentScreen == null) {
            if (this.stage != AutoMine.Stage.FIND_EXPERIENCE_CHEST) {
               BlockState plantState = mc.world.getBlockState(this.plantPos);
               if (plantState.isIn(BlockTags.LOGS)
                  && this.stage != AutoMine.Stage.BREAK_LEAVES
                  && this.stage != AutoMine.Stage.BREAK_LOGS
                  && !REPAIR_STAGES.contains(this.stage)) {
                  this.stage = AutoMine.Stage.BREAK_LEAVES;
                  this.target = null;
                  this.breaking = null;
                  this.chestPos = null;
               } else if (this.stage == AutoMine.Stage.PLANT && plantState.isIn(BlockTags.SAPLINGS)) {
                  this.stage = AutoMine.Stage.GROW;
               } else if (this.stage == AutoMine.Stage.GROW && !plantState.isIn(BlockTags.SAPLINGS)) {
                  this.stage = AutoMine.Stage.PLANT;
                  this.plantRotationPrepared = false;
               }

               if (REPAIR_STAGES.contains(this.stage)) {
                  this.target = null;
                  this.resetBreaking(true);
               } else if (this.stage == AutoMine.Stage.GROW && !this.hasBoneMeal()) {
                  this.beginSupplyRun();
               } else if (this.stage == AutoMine.Stage.FIND_CHEST) {
                  this.chestPos = this.findLabeledChest("мука", this.checkedChests);
                  if (this.chestPos != null) {
                     this.boneMealStackTaken = false;
                     this.stage = AutoMine.Stage.OPEN_CHEST;
                     this.delay = 1;
                  } else {
                     this.boneStacksTaken = 0;
                     this.stage = AutoMine.Stage.FIND_BONE_CHEST;
                  }
               } else if (this.stage == AutoMine.Stage.FIND_BONE_CHEST) {
                  if (this.delay <= 0) {
                     this.chestPos = this.findLabeledChest("кости", this.checkedBoneChests);
                     if (this.chestPos == null) {
                        this.chestPos = this.findAnyChest(this.checkedBoneChests);
                     }

                     if (this.chestPos != null) {
                        this.stage = AutoMine.Stage.OPEN_BONE_CHEST;
                        this.delay = 1;
                     } else if (!this.checkedBoneChests.isEmpty()) {
                        this.checkedBoneChests.clear();
                        this.delay = 20;
                     }
                  }
               } else if (this.stage != AutoMine.Stage.OPEN_CHEST
                  && this.stage != AutoMine.Stage.OPEN_BONE_CHEST
                  && this.stage != AutoMine.Stage.PLANT
                  && this.stage != AutoMine.Stage.GROW) {
                  TagKey<Block> wanted = this.stage == AutoMine.Stage.BREAK_LEAVES ? BlockTags.LEAVES : BlockTags.LOGS;
                  if (this.target != null) {
                     Vec3d center = Vec3d.ofCenter(this.target);
                     BlockHitResult hit = mc.world.raycast(new RaycastContext(mc.player.getEyePos(), center, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
                     BlockState targetState = mc.world.getBlockState(this.target);
                     if (!targetState.isIn(wanted)
                        || mc.player.getEyePos().distanceTo(center) > 4.5
                        || hit.getType() != Type.BLOCK
                        || !hit.getBlockPos().equals(this.target)) {
                        if (targetState.isAir() && this.target.equals(this.breaking)) {
                        }

                        this.target = null;
                        this.resetBreaking(!targetState.isAir());
                     }
                  }

                  if (this.target == null) {
                     for (int pass = 0; pass < 2 && this.target == null; pass++) {
                        TagKey<Block> searchTag = pass == 0 ? BlockTags.LEAVES : BlockTags.LOGS;
                        double bestDistance = Double.MAX_VALUE;

                        for (int x = -5; x <= 5; x++) {
                           for (int y = 0; y <= 16; y++) {
                              for (int z = -5; z <= 5; z++) {
                                 BlockPos pos = this.plantPos.add(x, y, z);
                                 if (mc.world.getBlockState(pos).isIn(searchTag)) {
                                    Vec3d center = Vec3d.ofCenter(pos);
                                    double distance = mc.player.getEyePos().distanceTo(center);
                                    if (!(distance > 4.5) && !(distance >= bestDistance)) {
                                       BlockHitResult hit = mc.world
                                          .raycast(new RaycastContext(mc.player.getEyePos(), center, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
                                       if (hit.getType() == Type.BLOCK && hit.getBlockPos().equals(pos)) {
                                          this.target = pos;
                                          bestDistance = distance;
                                       }
                                    }
                                 }
                              }
                           }
                        }

                        if (this.target != null) {
                           this.stage = pass == 0 ? AutoMine.Stage.BREAK_LEAVES : AutoMine.Stage.BREAK_LOGS;
                        }
                     }
                  }

                  if (this.target == null) {
                     this.stage = AutoMine.Stage.PLANT;
                     this.plantRotationPrepared = false;
                     this.resetBreaking(true);
                     this.delay = 1;
                  }
               } else {
                  this.target = null;
                  this.resetBreaking(true);
               }
            } else if (this.repairToolInOffhand && mc.player.getOffHandStack().getDamage() <= 0) {
               this.endRepair(true, "Инструмент починен до 100%.");
            } else if (this.hasExperienceBottles()) {
               this.stage = AutoMine.Stage.PREPARE_REPAIR;
            } else {
               this.experienceChestPos = this.findExperienceChest();
               if (this.experienceChestPos != null) {
                  this.experienceChestWarningShown = false;
                  this.experienceStackTaken = false;
                  this.stage = AutoMine.Stage.OPEN_EXPERIENCE_CHEST;
                  this.delay = 1;
               } else if (!this.experienceChestWarningShown) {
                  Notifications.push("Auto Mine", "Сундук с табличкой «опыт» не найден рядом.");
                  this.experienceChestWarningShown = true;
               }
            }
         }
      }
   }

   @SubscribeEvent
   private void onRotationUpdate(RotationUpdateEvent event) {
      if (mc.player != null && mc.world != null && mc.interactionManager != null && mc.currentScreen == null && this.plantPos != null) {
         Vec3d hitVec;
         if (this.stage == AutoMine.Stage.PLANT) {
            BlockPos ground = this.plantPos.down();
            hitVec = new Vec3d((double)ground.getX() + 0.5, (double)ground.getY() + 1.0, (double)ground.getZ() + 0.5);
         } else if (this.stage == AutoMine.Stage.GROW) {
            if (!this.hasBoneMeal()) {
               this.beginSupplyRun();
               return;
            }

            hitVec = Vec3d.ofCenter(this.plantPos);
         } else if (this.stage != AutoMine.Stage.OPEN_CHEST
            && this.stage != AutoMine.Stage.OPEN_BONE_CHEST
            && this.stage != AutoMine.Stage.OPEN_EXPERIENCE_CHEST) {
            if (this.stage == AutoMine.Stage.REPAIR_TOOL) {
               hitVec = mc.player.getEyePos().add(0.0, -1.0, 0.0);
            } else {
               if (this.target == null) {
                  return;
               }

               hitVec = Vec3d.ofCenter(this.target);
            }
         } else {
            BlockPos openPos = this.stage == AutoMine.Stage.OPEN_EXPERIENCE_CHEST ? this.experienceChestPos : this.chestPos;
            if (openPos == null) {
               this.stage = this.stage == AutoMine.Stage.OPEN_EXPERIENCE_CHEST
                  ? AutoMine.Stage.FIND_EXPERIENCE_CHEST
                  : (this.stage == AutoMine.Stage.OPEN_BONE_CHEST ? AutoMine.Stage.FIND_BONE_CHEST : AutoMine.Stage.FIND_CHEST);
               return;
            }

            BlockHitResult chestHit = this.findChestHit(openPos);
            if (chestHit == null) {
               this.markChestUnreachable(openPos);
               return;
            }

            hitVec = chestHit.getPos();
         }

         if (!(mc.player.getEyePos().distanceTo(hitVec) > 4.5)) {
            if (event.isPre()) {
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
            } else if (event.isPost() && this.delay <= 0) {
               if (this.stage == AutoMine.Stage.PLANT) {
                  if (!mc.world.getBlockState(this.plantPos).isAir()) {
                     this.plantRotationPrepared = false;
                  } else {
                     BlockPos ground = this.plantPos.down();
                     BlockHitResult groundHit = RaycastAngle.raycast(mc.player.getEyePos(), 4.5, AngleConnection.INSTANCE.getRotation(), false);
                     if (groundHit == null
                        || groundHit.getType() != Type.BLOCK
                        || !groundHit.getBlockPos().equals(ground)
                        || groundHit.getSide() != Direction.UP) {
                        this.plantRotationPrepared = false;
                     } else if (!this.plantRotationPrepared) {
                        this.plantRotationPrepared = true;
                     } else {
                        boolean used = this.runWithItem(stack -> stack.isIn(ItemTags.SAPLINGS), null, () -> {
                           mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, groundHit);
                           mc.player.swingHand(Hand.MAIN_HAND);
                        });
                        if (used) {
                           this.stage = AutoMine.Stage.GROW;
                           this.plantRotationPrepared = false;
                           this.delay = 2;
                        }
                     }
                  }
               } else if (this.stage == AutoMine.Stage.GROW) {
                  if (mc.world.getBlockState(this.plantPos).isIn(BlockTags.SAPLINGS)) {
                     if (this.runWithItem(stack -> stack.isOf(Items.BONE_MEAL), null, () -> {
                        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(hitVec, Direction.UP, this.plantPos, false));
                        mc.player.swingHand(Hand.MAIN_HAND);
                     })) {
                        this.delay = 1;
                     }
                  }
               } else if (this.stage == AutoMine.Stage.OPEN_CHEST) {
                  BlockHitResult hit = this.findChestHit(this.chestPos);
                  if (hit == null) {
                     this.checkedChests.add(this.chestPos);
                     this.chestPos = null;
                     this.stage = AutoMine.Stage.FIND_CHEST;
                  } else {
                     mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.delay = 5;
                  }
               } else if (this.stage == AutoMine.Stage.OPEN_BONE_CHEST) {
                  BlockHitResult hit = this.findChestHit(this.chestPos);
                  if (hit == null) {
                     this.checkedBoneChests.add(this.chestPos);
                     this.chestPos = null;
                     this.stage = AutoMine.Stage.FIND_BONE_CHEST;
                  } else {
                     mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.delay = 5;
                  }
               } else if (this.stage == AutoMine.Stage.OPEN_EXPERIENCE_CHEST) {
                  BlockHitResult hit = this.findChestHit(this.experienceChestPos);
                  if (hit == null) {
                     if (this.experienceChestPos != null) {
                        this.checkedExperienceChests.add(this.experienceChestPos);
                     }

                     this.experienceChestPos = null;
                     this.stage = AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                  } else {
                     mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                     mc.player.swingHand(Hand.MAIN_HAND);
                     this.delay = 5;
                  }
               } else if (this.stage == AutoMine.Stage.REPAIR_TOOL) {
                  ItemStack repairTool = mc.player.getOffHandStack();
                  if (!this.repairToolInOffhand || repairTool.isEmpty() || !repairTool.isDamageable()) {
                     this.endRepair(false, "Инструмент для ремонта потерян.");
                  } else if (repairTool.getDamage() <= 0) {
                     this.endRepair(true, "Инструмент починен до 100%.");
                  } else if (!this.repairRotationPrepared) {
                     this.repairRotationPrepared = true;
                  } else {
                     ItemStack bottle = mc.player.getInventory().getStack(this.repairBottleHotbarSlot);
                     if (!bottle.isOf(Items.EXPERIENCE_BOTTLE)) {
                        this.restoreRepairInventory(false);
                        this.stage = this.hasExperienceBottles() ? AutoMine.Stage.PREPARE_REPAIR : AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                     } else {
                        InventoryUtils.syncSelectedHotbarSlot(this.repairBottleHotbarSlot);
                        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        this.delay = 4;
                     }
                  }
               } else if (this.target != null) {
                  BlockState state = mc.world.getBlockState(this.target);
                  TagKey<Block> wanted = this.stage == AutoMine.Stage.BREAK_LEAVES ? BlockTags.LEAVES : BlockTags.LOGS;
                  if (!state.isIn(wanted)) {
                     if (state.isAir() && this.target.equals(this.breaking)) {
                     }

                     this.target = null;
                     this.resetBreaking(!state.isAir());
                  } else {
                     BlockHitResult hit = RaycastAngle.raycast(mc.player.getEyePos(), 4.5, AngleConnection.INSTANCE.getRotation(), false);
                     if (hit != null && hit.getType() == Type.BLOCK && hit.getBlockPos().equals(this.target)) {
                        BlockPos current = this.target;
                        Predicate<ItemStack> toolType = this.stage == AutoMine.Stage.BREAK_LEAVES
                           ? stack -> stack.isIn(ItemTags.HOES)
                           : stack -> stack.isIn(ItemTags.AXES);
                        int minimumDurability = this.minToolDurability.getValue().intValue();
                        Predicate<ItemStack> safeTool = stack -> toolType.test(stack)
                              && (!stack.isDamageable() || this.remainingDurability(stack) > minimumDurability);
                        boolean used = this.runWithItem(safeTool, state, () -> {
                           if (this.breaking != null && this.breaking.equals(current)) {
                              mc.interactionManager.updateBlockBreakingProgress(current, hit.getSide());
                           } else {
                              mc.interactionManager.attackBlock(current, hit.getSide());
                              this.breaking = current;
                           }

                           this.accelerateBreaking(current, state);
                           mc.player.swingHand(Hand.MAIN_HAND);
                        });
                        if (!used) {
                           this.resetBreaking(true);
                           int repairSlot = this.findRepairToolSlot(toolType);
                           if (repairSlot != -1) {
                              this.repairReturnStage = this.stage == AutoMine.Stage.BREAK_LEAVES ? AutoMine.Stage.BREAK_LEAVES : AutoMine.Stage.BREAK_LOGS;
                              this.repairToolSlot = repairSlot;
                              this.repairRotationPrepared = false;
                              this.experienceChestWarningShown = false;
                              this.checkedExperienceChests.clear();
                              this.target = null;
                              this.stage = this.hasExperienceBottles() ? AutoMine.Stage.PREPARE_REPAIR : AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                              if (!this.toolDurabilityWarningShown) {
                                 Notifications.push("Auto Mine", "Прочность низкая.");
                                 this.toolDurabilityWarningShown = true;
                              }
                           } else if (!this.toolDurabilityWarningShown) {
                              String message = this.hasInventoryItem(toolType)
                                 ? "На повреждённом инструменте нет Починки."
                                 : "Топор или мотыга уже сломаны или отсутствуют в инвентаре.";
                              Notifications.push("Auto Mine", message);
                              this.toolDurabilityWarningShown = true;
                           }
                        } else {
                           this.toolDurabilityWarningShown = false;
                           if (used && mc.world.getBlockState(current).isAir()) {
                              this.target = null;
                              this.resetBreaking(false);
                           }
                        }
                     } else {
                        this.target = null;
                        this.resetBreaking(true);
                     }
                  }
               }
            }
         }
      }
   }

   private void resetBreaking(boolean abort) {
      if (abort && this.breaking != null && mc.interactionManager != null && mc.world != null && !mc.world.getBlockState(this.breaking).isAir()) {
         mc.interactionManager.cancelBlockBreaking();
      }

      this.breaking = null;
   }

   private void accelerateBreaking(BlockPos pos, BlockState state) {
      float multiplier = this.breakSpeed.getFloat();
      if (!(multiplier <= 1.0F) && !state.isAir()) {
         MultiPlayerGameModeAccessor controller = (MultiPlayerGameModeAccessor)mc.interactionManager;
         controller.daamky$setDestroyDelay(0);
         float vanillaProgress = state.calcBlockBreakingDelta(mc.player, mc.player.getEntityWorld(), pos);
         float acceleratedProgress = controller.daamky$getDestroyProgress() + vanillaProgress * (multiplier - 1.0F);
         controller.daamky$setDestroyProgress(Math.min(1.0F, acceleratedProgress));
      }
   }

   private int remainingDurability(ItemStack stack) {
      return stack.isDamageable() ? Math.max(0, stack.getMaxDamage() - stack.getDamage()) : Integer.MAX_VALUE;
   }

   private boolean hasInventoryItem(Predicate<ItemStack> filter) {
      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && filter.test(stack)) {
            return true;
         }
      }

      return false;
   }

   private int findRepairToolSlot(Predicate<ItemStack> toolType) {
      int bestSlot = -1;
      int highestDamage = -1;

      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty()
            && toolType.test(stack)
            && stack.isDamageable()
            && stack.getDamage() > 0
            && this.hasMending(stack)
            && stack.getDamage() > highestDamage) {
            highestDamage = stack.getDamage();
            bestSlot = slot;
         }
      }

      return bestSlot;
   }

   private boolean hasMending(ItemStack stack) {
      if (mc.world == null) {
         return false;
      } else {
         try {
            RegistryEntry<Enchantment> mending = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).getOrThrow(Enchantments.MENDING);
            return EnchantmentHelper.getLevel(mending, stack) > 0;
         } catch (Throwable var3) {
            return false;
         }
      }
   }

   private void prepareRepair() {
      ItemStack tool = this.repairToolInOffhand
         ? mc.player.getOffHandStack()
         : (this.repairToolSlot >= 0 && this.repairToolSlot < 36 ? mc.player.getInventory().getStack(this.repairToolSlot) : ItemStack.EMPTY);
      if (!tool.isEmpty() && tool.isDamageable() && this.hasMending(tool)) {
         if (!this.repairToolInOffhand) {
            this.repairPreviousSelectedSlot = mc.player.getInventory().getSelectedSlot();
            InventoryTask.swap(this.repairToolSlot < 9 ? 36 + this.repairToolSlot : this.repairToolSlot, 40, false);
            tool = mc.player.getOffHandStack();
            this.repairToolInOffhand = !tool.isEmpty() && tool.isDamageable() && this.hasMending(tool);
            if (!this.repairToolInOffhand) {
               this.endRepair(false, "Не удалось перенести инструмент во вторую руку.");
               return;
            }
         }

         if (tool.getDamage() <= 0) {
            this.endRepair(true, "Инструмент починен до 100%.");
         } else {
            if (this.repairBottleHotbarSlot >= 0
               && this.repairBottleHotbarSlot < 9
               && mc.player.getInventory().getStack(this.repairBottleHotbarSlot).isOf(Items.EXPERIENCE_BOTTLE)) {
               InventoryUtils.syncSelectedHotbarSlot(this.repairBottleHotbarSlot);
            } else {
               int bottleSlot = InventoryTask.findHotbarSlot(Items.EXPERIENCE_BOTTLE);
               if (bottleSlot == -1) {
                  int inventoryBottle = InventoryTask.findInventorySlot(Items.EXPERIENCE_BOTTLE);
                  if (inventoryBottle == -1) {
                     this.stage = AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                     return;
                  }

                  int slot = 0;

                  while (true) {
                     if (slot < 9) {
                        if (!mc.player.getInventory().getStack(slot).isEmpty()) {
                           slot++;
                           continue;
                        }

                        bottleSlot = slot;
                     }

                     if (bottleSlot == -1) {
                        bottleSlot = this.repairPreviousSelectedSlot;
                     }

                     this.repairBottleSourceSlot = inventoryBottle;
                     InventoryTask.swap(inventoryBottle, bottleSlot, false);
                     break;
                  }
               } else {
                  this.repairBottleSourceSlot = -1;
               }

               this.repairBottleHotbarSlot = bottleSlot;
               if (bottleSlot < 0 || !mc.player.getInventory().getStack(bottleSlot).isOf(Items.EXPERIENCE_BOTTLE)) {
                  this.repairBottleSourceSlot = -1;
                  this.repairBottleHotbarSlot = -1;
                  this.stage = AutoMine.Stage.FIND_EXPERIENCE_CHEST;
                  return;
               }

               InventoryUtils.syncSelectedHotbarSlot(bottleSlot);
            }

            this.repairRotationPrepared = false;
            this.stage = AutoMine.Stage.REPAIR_TOOL;
            this.delay = 2;
         }
      } else {
         this.endRepair(false, "Инструмент с Починкой не найден.");
      }
   }

   private void restoreRepairInventory(boolean restoreTool) {
      if (mc.player != null && mc.interactionManager != null) {
         if (restoreTool && mc.currentScreen != null) {
            mc.player.closeHandledScreen();
         }

         if (this.repairBottleSourceSlot >= 9 && this.repairBottleHotbarSlot >= 0) {
            InventoryTask.swap(this.repairBottleSourceSlot, this.repairBottleHotbarSlot, false);
         }

         this.repairBottleSourceSlot = -1;
         this.repairBottleHotbarSlot = -1;
         if (restoreTool) {
            if (this.repairToolInOffhand && this.repairToolSlot >= 0 && this.repairToolSlot < 36) {
               InventoryTask.swap(this.repairToolSlot < 9 ? 36 + this.repairToolSlot : this.repairToolSlot, 40, false);
            }

            if (this.repairPreviousSelectedSlot >= 0 && this.repairPreviousSelectedSlot < 9) {
               InventoryUtils.syncSelectedHotbarSlot(this.repairPreviousSelectedSlot);
            }

            this.repairToolInOffhand = false;
            this.repairToolSlot = -1;
            this.repairPreviousSelectedSlot = -1;
            this.repairRotationPrepared = false;
         }
      } else {
         this.repairBottleSourceSlot = -1;
         this.repairBottleHotbarSlot = -1;
         if (restoreTool) {
            this.repairToolInOffhand = false;
         }
      }
   }

   private void endRepair(boolean success, String message) {
      this.restoreRepairInventory(true);
      this.stage = this.repairReturnStage;
      this.target = null;
      this.delay = success ? 2 : 5;
      this.toolDurabilityWarningShown = !success;
      this.checkedExperienceChests.clear();
      Notifications.push("Auto Mine", message);
   }

   private boolean hasExperienceBottles() {
      return InventoryTask.findHotbarSlot(Items.EXPERIENCE_BOTTLE) != -1 || InventoryTask.findInventorySlot(Items.EXPERIENCE_BOTTLE) != -1;
   }

   private boolean hasBoneMeal() {
      return InventoryTask.findHotbarSlot(Items.BONE_MEAL) != -1 || InventoryTask.findInventorySlot(Items.BONE_MEAL) != -1;
   }

   private void beginSupplyRun() {
      this.checkedChests.clear();
      this.checkedBoneChests.clear();
      this.boneStacksTaken = 0;
      this.craftStep = 0;
      this.craftBoneSlot = -1;
      this.chestPos = null;
      this.target = null;
      this.resetBreaking(true);
      this.stage = AutoMine.Stage.FIND_CHEST;
   }

   private BlockPos findExperienceChest() {
      return this.findLabeledChest("опыт", this.checkedExperienceChests);
   }

   private BlockPos findLabeledChest(String label, Set<BlockPos> checked) {
      return this.findChest(label, checked);
   }

   private BlockPos findAnyChest(Set<BlockPos> checked) {
      return this.findChest(null, checked);
   }

   private BlockPos findChest(String label, Set<BlockPos> checked) {
      BlockPos best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int x = -4; x <= 4; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -4; z <= 4; z++) {
               BlockPos pos = mc.player.getBlockPos().add(x, y, z);
               if (!checked.contains(pos) && mc.world.getBlockState(pos).getBlock() instanceof ChestBlock && (label == null || this.isLabeledChest(pos, label))
                  )
                {
                  Vec3d center = Vec3d.ofCenter(pos);
                  double distance = mc.player.getEyePos().distanceTo(center);
                  if (!(distance > 4.5) && !(distance >= bestDistance) && this.findChestHit(pos) != null) {
                     best = pos;
                     bestDistance = distance;
                  }
               }
            }
         }
      }

      return best;
   }

   private BlockHitResult findChestHit(BlockPos pos) {
      if (pos == null) {
         return null;
      } else {
         Vec3d eye = mc.player.getEyePos();
         double x = (double)pos.getX();
         double y = (double)pos.getY();
         double z = (double)pos.getZ();
         Vec3d[] samples = new Vec3d[]{
            new Vec3d(x + 0.5, y + 0.5, z + 0.5),
            new Vec3d(x + 0.5, y + 0.86, z + 0.5),
            new Vec3d(x + 0.15, y + 0.5, z + 0.5),
            new Vec3d(x + 0.85, y + 0.5, z + 0.5),
            new Vec3d(x + 0.5, y + 0.5, z + 0.15),
            new Vec3d(x + 0.5, y + 0.5, z + 0.85)
         };

         for (Vec3d sample : samples) {
            if (!(eye.distanceTo(sample) > 4.5)) {
               BlockHitResult hit = mc.world.raycast(new RaycastContext(eye, sample, ShapeType.OUTLINE, FluidHandling.NONE, mc.player));
               if (hit.getType() == Type.BLOCK && hit.getBlockPos().equals(pos)) {
                  return hit;
               }
            }
         }

         return null;
      }
   }

   private void markChestUnreachable(BlockPos pos) {
      if (this.stage == AutoMine.Stage.OPEN_EXPERIENCE_CHEST) {
         this.checkedExperienceChests.add(pos);
         this.experienceChestPos = null;
         this.stage = AutoMine.Stage.FIND_EXPERIENCE_CHEST;
      } else if (this.stage == AutoMine.Stage.OPEN_BONE_CHEST) {
         this.checkedBoneChests.add(pos);
         this.chestPos = null;
         this.stage = AutoMine.Stage.FIND_BONE_CHEST;
      } else {
         this.checkedChests.add(pos);
         this.chestPos = null;
         this.stage = AutoMine.Stage.FIND_CHEST;
      }
   }

   private boolean isLabeledChest(BlockPos chest, String label) {
      String normalizedLabel = this.normalizeLabel(label);
      if (mc.world.getBlockEntity(chest) instanceof ChestBlockEntity chestEntity
         && this.labelMatches(this.normalizeLabel(chestEntity.getDisplayName().getString()), normalizedLabel)) {
         return true;
      }

      for (BlockPos signPos : BlockPos.iterate(chest.add(-2, -2, -2), chest.add(2, 2, 2))) {
         BlockEntity matchingLabel = mc.world.getBlockEntity(signPos);
         if (matchingLabel instanceof SignBlockEntity) {
            SignBlockEntity sign = (SignBlockEntity)matchingLabel;
            boolean matchingLabelx = false;

            for (boolean front : new boolean[]{true, false}) {
               for (Text line : sign.getText(front).getMessages(false)) {
                  matchingLabelx |= this.labelMatches(this.normalizeLabel(line.getString()), normalizedLabel);
               }
            }

            if (matchingLabelx) {
               boolean closest = true;

               for (BlockPos other : BlockPos.iterate(signPos.add(-2, -2, -2), signPos.add(2, 2, 2))) {
                  if (!other.equals(chest)
                     && mc.world.getBlockState(other).getBlock() instanceof ChestBlock
                     && signPos.getSquaredDistance(other) < signPos.getSquaredDistance(chest)) {
                     closest = false;
                     break;
                  }
               }

               if (closest) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private String normalizeLabel(String text) {
      return text.trim().toLowerCase(Locale.ROOT).replace('ё', 'е').replaceAll("[^а-яa-z0-9]+", "");
   }

   private boolean labelMatches(String actual, String expected) {
      return !actual.equals(expected) && !actual.contains(expected) ? expected.equals("кости") && (actual.equals("кость") || actual.contains("кост")) : true;
   }

   private boolean runWithItem(Predicate<ItemStack> filter, BlockState state, Runnable action) {
      int source = -1;
      double bestSpeed = -1.0;
      int bestDurability = -1;

      for (int slot = 0; slot < 36; slot++) {
         ItemStack stack = mc.player.getInventory().getStack(slot);
         if (!stack.isEmpty() && filter.test(stack)) {
            double speed = state == null ? (slot < 9 ? 2.0 : 1.0) : (double)stack.getMiningSpeedMultiplier(state);
            int durability = this.remainingDurability(stack);
            if (speed > bestSpeed || speed == bestSpeed && durability > bestDurability) {
               bestSpeed = speed;
               bestDurability = durability;
               source = slot;
            }
         }
      }

      if (source == -1) {
         return false;
      } else {
         int useSlot = source;
         if (source >= 9) {
            useSlot = -1;

            for (int slotx = 0; slotx < 9; slotx++) {
               if (mc.player.getInventory().getStack(slotx).isEmpty()) {
                  useSlot = slotx;
                  break;
               }
            }

            if (useSlot == -1) {
               useSlot = mc.player.getInventory().getSelectedSlot();
            }

            InventoryTask.swap(source, useSlot, false);
            if (!filter.test(mc.player.getInventory().getStack(useSlot))) {
               return false;
            }
         }

         InventoryUtils.syncSelectedHotbarSlot(useSlot);
         action.run();
         return true;
      }
   }

   private static enum Stage {
      PLANT,
      GROW,
      FIND_CHEST,
      OPEN_CHEST,
      TAKE_BONE_MEAL,
      FIND_BONE_CHEST,
      OPEN_BONE_CHEST,
      TAKE_BONES,
      CRAFT_BONE_MEAL,
      FIND_EXPERIENCE_CHEST,
      OPEN_EXPERIENCE_CHEST,
      TAKE_EXPERIENCE,
      PREPARE_REPAIR,
      REPAIR_TOOL,
      BREAK_LEAVES,
      BREAK_LOGS;

      private Stage() {
      }
   }
}
