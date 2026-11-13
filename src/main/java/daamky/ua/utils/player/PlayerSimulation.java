package daamky.ua.utils.player;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.shape.VoxelShape;
import daamky.ua.IMinecraft;
import daamky.ua.mixin.accessor.EntityAccessor;
import daamky.ua.mixin.accessor.LivingEntityAccessor;
import daamky.ua.utils.move.MoveUtil;

public class PlayerSimulation implements Simulation, IMinecraft {
   public final PlayerEntity player;
   public final PlayerSimulation.SimulatedPlayerInput input;
   public Vec3d pos;
   public Vec3d velocity;
   public Box boundingBox;
   public float yaw;
   public float pitch;
   public boolean sprinting;
   public float fallDistance;
   public int jumpingCooldown;
   public boolean isJumping;
   public boolean isFallFlying;
   public boolean onGround;
   public boolean horizontalCollision;
   public boolean verticalCollision;
   public boolean touchingWater;
   public boolean isSwimming;
   public boolean submergedInWater;
   private final Object2DoubleMap<TagKey<Fluid>> fluidHeight;
   private final HashSet<TagKey<Fluid>> submergedFluidTag;
   private int simulatedTicks = 0;
   private boolean clipLedged = false;
   private static final double STEP_HEIGHT = 0.5;

   public PlayerSimulation(
      PlayerEntity player,
      PlayerSimulation.SimulatedPlayerInput input,
      Vec3d pos,
      Vec3d velocity,
      Box boundingBox,
      float yaw,
      float pitch,
      boolean sprinting,
      float fallDistance,
      int jumpingCooldown,
      boolean isJumping,
      boolean isFallFlying,
      boolean onGround,
      boolean horizontalCollision,
      boolean verticalCollision,
      boolean touchingWater,
      boolean isSwimming,
      boolean submergedInWater,
      Object2DoubleMap<TagKey<Fluid>> fluidHeight,
      HashSet<TagKey<Fluid>> submergedFluidTag
   ) {
      this.player = player;
      this.input = input;
      this.pos = pos;
      this.velocity = velocity;
      this.boundingBox = boundingBox;
      this.yaw = yaw;
      this.pitch = pitch;
      this.sprinting = sprinting;
      this.fallDistance = fallDistance;
      this.jumpingCooldown = jumpingCooldown;
      this.isJumping = isJumping;
      this.isFallFlying = isFallFlying;
      this.onGround = onGround;
      this.horizontalCollision = horizontalCollision;
      this.verticalCollision = verticalCollision;
      this.touchingWater = touchingWater;
      this.isSwimming = isSwimming;
      this.submergedInWater = submergedInWater;
      this.fluidHeight = fluidHeight;
      this.submergedFluidTag = submergedFluidTag;
   }

   public static PlayerSimulation simulateLocalPlayer(int ticks) {
      PlayerSimulation simulatedPlayer = fromClientPlayer(PlayerSimulation.SimulatedPlayerInput.fromClientPlayer(mc.player.input.playerInput));

      for (int i = 0; i < ticks; i++) {
         simulatedPlayer.tick();
      }

      return simulatedPlayer;
   }

   public static PlayerSimulation simulateOtherPlayer(PlayerEntity player, int ticks) {
      PlayerSimulation simulatedPlayer = fromOtherPlayer(player, PlayerSimulation.SimulatedPlayerInput.guessInput(player));

      for (int i = 0; i < ticks; i++) {
         simulatedPlayer.tick();
      }

      return simulatedPlayer;
   }

   public static PlayerSimulation fromClientPlayer(PlayerSimulation.SimulatedPlayerInput input) {
      ClientPlayerEntity player = mc.player;
      LivingEntityAccessor livingAccessor = (LivingEntityAccessor)player;
      EntityAccessor entityAccessor = (EntityAccessor)player;
      return new PlayerSimulation(
         player,
         input,
         player.getEntityPos(),
         player.getVelocity(),
         player.getBoundingBox(),
         player.getYaw(),
         player.getPitch(),
         player.isSprinting(),
         (float)player.fallDistance,
         livingAccessor.daamky$getJumpingCooldown(),
         livingAccessor.daamky$isJumping(),
         player.isGliding(),
         player.isOnGround(),
         entityAccessor.daamky$isHorizontalCollision(),
         entityAccessor.daamky$isVerticalCollision(),
         player.isTouchingWater(),
         player.isSwimming(),
         player.isSubmergedInWater(),
         new Object2DoubleArrayMap(entityAccessor.daamky$getFluidHeight()),
         new HashSet<>(entityAccessor.daamky$getSubmergedFluidTag())
      );
   }

   public static PlayerSimulation fromOtherPlayer(PlayerEntity player, PlayerSimulation.SimulatedPlayerInput input) {
      LivingEntityAccessor livingAccessor = (LivingEntityAccessor)player;
      EntityAccessor entityAccessor = (EntityAccessor)player;
      return new PlayerSimulation(
         player,
         input,
         player.getEntityPos(),
         player.getEntityPos().subtract(new Vec3d(player.lastX, player.lastY, player.lastZ)),
         player.getBoundingBox(),
         player.getYaw(),
         player.getPitch(),
         player.isSprinting(),
         (float)player.fallDistance,
         livingAccessor.daamky$getJumpingCooldown(),
         livingAccessor.daamky$isJumping(),
         player.isGliding(),
         player.isOnGround(),
         entityAccessor.daamky$isHorizontalCollision(),
         entityAccessor.daamky$isVerticalCollision(),
         player.isTouchingWater(),
         player.isSwimming(),
         player.isSubmergedInWater(),
         new Object2DoubleArrayMap(entityAccessor.daamky$getFluidHeight()),
         new HashSet<>(entityAccessor.daamky$getSubmergedFluidTag())
      );
   }

   @Override
   public Vec3d pos() {
      return this.player.getEntityPos();
   }

   @Override
   public void tick() {
      this.simulatedTicks++;
      this.clipLedged = false;
      if (!(this.pos.y <= -70.0)) {
         this.input.update();
         this.checkWaterState();
         this.updateSubmergedInWaterState();
         this.updateSwimming();
         if (this.jumpingCooldown > 0) {
            this.jumpingCooldown--;
         }

         this.isJumping = this.input.keyPresses.jump();
         double newX = this.velocity.x;
         double newY = this.velocity.y;
         double newZ = this.velocity.z;
         if (Math.abs(this.velocity.x) < 0.003) {
            newX = 0.0;
         }

         if (Math.abs(this.velocity.y) < 0.003) {
            newY = 0.0;
         }

         if (Math.abs(this.velocity.z) < 0.003) {
            newZ = 0.0;
         }

         if (this.onGround) {
            this.isFallFlying = false;
         }

         this.velocity = new Vec3d(newX, newY, newZ);
         if (this.isJumping) {
            double fluidLevel = this.isInLava() ? this.getFluidHeight(FluidTags.LAVA) : this.getFluidHeight(FluidTags.WATER);
            boolean inWater = this.isTouchingWater() && fluidLevel > 0.0;
            double swimHeight = this.getSwimHeight();
            if (!inWater || this.onGround && !(fluidLevel > swimHeight)) {
               if (!this.isInLava() || this.onGround && !(fluidLevel > swimHeight)) {
                  if ((this.onGround || inWater && fluidLevel <= swimHeight) && this.jumpingCooldown == 0) {
                     this.jump();
                     if (this.player.equals(mc.player)) {
                        this.jumpingCooldown = 10;
                     }
                  }
               } else {
                  this.swimUpward(FluidTags.LAVA);
               }
            } else {
               this.swimUpward(FluidTags.WATER);
            }
         }

         float sidewaysSpeed = this.input.movementSideways * 0.98F;
         float forwardSpeed = this.input.movementForward * 0.98F;
         float upwardsSpeed = 0.0F;
         if (this.hasStatusEffect(StatusEffects.SLOW_FALLING) || this.hasStatusEffect(StatusEffects.LEVITATION)) {
            this.onLanding();
         }

         this.travel(new Vec3d((double)sidewaysSpeed, (double)upwardsSpeed, (double)forwardSpeed));
      }
   }

   private void travel(Vec3d movementInput) {
      if (this.isSwimming && !this.player.hasVehicle()) {
         double g = this.getRotationVector().y;
         double h = g < -0.2 ? 0.085 : 0.06;
         BlockPos posAbove = new BlockPos(MathHelper.floor(this.pos.x), MathHelper.floor(this.pos.y + 1.0 - 0.1), MathHelper.floor(this.pos.z));
         if (g <= 0.0 || this.input.keyPresses.jump() || !this.player.getEntityWorld().getBlockState(posAbove).getFluidState().isEmpty()) {
            this.velocity = this.velocity.add(0.0, (g - this.velocity.y) * h, 0.0);
         }
      }

      double beforeTravelVelocityY = this.velocity.y;
      double d = 0.08;
      boolean falling = this.velocity.y <= 0.0;
      if (this.velocity.y <= 0.0 && this.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
         d = 0.01;
         this.onLanding();
      }

      if (this.isTouchingWater() && this.player.shouldSwimInFluids()) {
         double e = this.pos.y;
         float f = this.isSprinting() ? 0.9F : 0.8F;
         float g = 0.02F;
         float h = (float)this.getAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
         if (!this.onGround) {
            h *= 0.5F;
         }

         if (h > 0.0F) {
            f += (0.54600006F - f) * h / 3.0F;
            g += (this.getMovementSpeed() - g) * h / 3.0F;
         }

         if (this.hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
            f = 0.96F;
         }

         this.updateVelocity(g, movementInput);
         this.move(this.velocity);
         Vec3d tempVel = this.velocity;
         if (this.horizontalCollision && this.isClimbing()) {
            tempVel = new Vec3d(tempVel.x, 0.2, tempVel.z);
         }

         this.velocity = tempVel.multiply((double)f, 0.8, (double)f);
         Vec3d vec3d2 = this.player.applyFluidMovingSpeed(d, falling, this.velocity);
         this.velocity = vec3d2;
         if (this.horizontalCollision && this.doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - this.pos.y + e, vec3d2.z)) {
            this.velocity = new Vec3d(vec3d2.x, 0.3, vec3d2.z);
         }
      } else if (this.isInLava() && this.player.shouldSwimInFluids()) {
         double ex = this.pos.y;
         this.updateVelocity(0.02F, movementInput);
         this.move(this.velocity);
         if (this.getFluidHeight(FluidTags.LAVA) <= this.getSwimHeight()) {
            this.velocity = this.velocity.multiply(0.5, 0.8, 0.5);
            this.velocity = this.player.applyFluidMovingSpeed(d, falling, this.velocity);
         } else {
            this.velocity = this.velocity.multiply(0.5);
         }

         if (!this.player.hasNoGravity()) {
            this.velocity = this.velocity.add(0.0, -d / 4.0, 0.0);
         }

         if (this.horizontalCollision && this.doesNotCollide(this.velocity.x, this.velocity.y + 0.6 - this.pos.y + ex, this.velocity.z)) {
            this.velocity = new Vec3d(this.velocity.x, 0.3, this.velocity.z);
         }
      } else if (this.isFallFlying) {
         Vec3d exx = this.velocity;
         if (exx.y > -0.5) {
            this.fallDistance = 1.0F;
         }

         Vec3d vec3d3 = this.getRotationVector();
         float fx = this.pitch * (float) (Math.PI / 180.0);
         double gx = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
         double horizontalSpeed = this.velocity.horizontalLength();
         double i = vec3d3.length();
         float j = MathHelper.cos((double)fx);
         j = (float)((double)j * (double)j * Math.min(1.0, i / 0.4));
         exx = this.velocity.add(0.0, d * (-1.0 + (double)j * 0.75), 0.0);
         if (exx.y < 0.0 && gx > 0.0) {
            double k = exx.y * -0.1 * (double)j;
            exx = exx.add(vec3d3.x * k / gx, k, vec3d3.z * k / gx);
         }

         if (fx < 0.0F && gx > 0.0) {
            double k = horizontalSpeed * (double)(-MathHelper.sin((double)fx)) * 0.04;
            exx = exx.add(-vec3d3.x * k / gx, k * 3.2, -vec3d3.z * k / gx);
         }

         if (gx > 0.0) {
            exx = exx.add((vec3d3.x / gx * horizontalSpeed - exx.x) * 0.1, 0.0, (vec3d3.z / gx * horizontalSpeed - exx.z) * 0.1);
         }

         this.velocity = exx.multiply(0.99, 0.98, 0.99);
         this.move(this.velocity);
      } else {
         BlockPos blockPos = this.getVelocityAffectingPos();
         float p = this.player.getEntityWorld().getBlockState(blockPos).getBlock().getSlipperiness();
         float fxx = this.onGround ? p * 0.91F : 0.91F;
         Vec3d vec3d6 = this.applyMovementInput(movementInput, p);
         double q = vec3d6.y;
         if (this.hasStatusEffect(StatusEffects.LEVITATION)) {
            StatusEffectInstance levitation = this.getStatusEffect(StatusEffects.LEVITATION);
            if (levitation != null) {
               q += (0.05 * (double)(levitation.getAmplifier() + 1) - vec3d6.y) * 0.2;
            }
         } else if (this.player.getEntityWorld().isClient() && !this.player.getEntityWorld().isChunkLoaded(blockPos)) {
            q = this.pos.y > (double)this.player.getEntityWorld().getBottomY() ? -0.1 : 0.0;
         } else if (!this.player.hasNoGravity()) {
            q -= d;
         }

         if (this.player.hasNoDrag()) {
            this.velocity = new Vec3d(vec3d6.x, q, vec3d6.z);
         } else {
            this.velocity = new Vec3d(vec3d6.x * (double)fxx, q * 0.98F, vec3d6.z * (double)fxx);
         }
      }

      if (this.player.getAbilities().flying && !this.player.hasVehicle()) {
         this.velocity = new Vec3d(this.velocity.x, beforeTravelVelocityY * 0.6, this.velocity.z);
         this.onLanding();
      }
   }

   private Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
      this.updateVelocity(this.getMovementSpeed(slipperiness), movementInput);
      this.velocity = this.applyClimbingSpeed(this.velocity);
      this.move(this.velocity);
      Vec3d result = this.velocity;
      BlockPos posBlock = this.posToBlockPos(this.pos);
      BlockState state = this.getState(posBlock);
      if ((this.horizontalCollision || this.isJumping)
         && (this.isClimbing() || state != null && state.isOf(Blocks.POWDER_SNOW) && PowderSnowBlock.canWalkOnPowderSnow(this.player))) {
         result = new Vec3d(result.x, 0.2, result.z);
      }

      return result;
   }

   private void updateVelocity(float speed, Vec3d movementInput) {
      Vec3d vec = movementInputToVelocity(movementInput, speed, this.yaw);
      this.velocity = this.velocity.add(vec);
   }

   private static Vec3d movementInputToVelocity(Vec3d movementInput, float speed, float yaw) {
      double lengthSquared = movementInput.lengthSquared();
      if (lengthSquared < 1.0E-7) {
         return Vec3d.ZERO;
      } else {
         Vec3d normalized = lengthSquared > 1.0 ? movementInput.normalize() : movementInput;
         Vec3d scaled = normalized.multiply((double)speed);
         float yawRad = yaw * (float) (Math.PI / 180.0);
         float sin = MathHelper.sin((double)yawRad);
         float cos = MathHelper.cos((double)yawRad);
         return new Vec3d(scaled.x * (double)cos - scaled.z * (double)sin, scaled.y, scaled.z * (double)cos + scaled.x * (double)sin);
      }
   }

   private float getMovementSpeed(float slipperiness) {
      return this.onGround ? this.getMovementSpeed() * (0.21600002F / (slipperiness * slipperiness * slipperiness)) : this.getAirStrafingSpeed();
   }

   private float getAirStrafingSpeed() {
      float speed = 0.02F;
      return this.input.keyPresses.sprint() ? speed + 0.006F : speed;
   }

   private float getMovementSpeed() {
      return 0.1F;
   }

   private void move(Vec3d movement) {
      Vec3d modifiedMovement = this.adjustMovementForSneaking(movement);
      Vec3d adjustedMovement = this.adjustMovementForCollisions(modifiedMovement);
      if (adjustedMovement.lengthSquared() > 1.0E-7) {
         this.pos = this.pos.add(adjustedMovement);
         this.boundingBox = this.player.getDimensions(this.player.getPose()).getBoxAt(this.pos);
      }

      boolean xCollision = !MathHelper.approximatelyEquals(movement.x, adjustedMovement.x);
      boolean zCollision = !MathHelper.approximatelyEquals(movement.z, adjustedMovement.z);
      this.horizontalCollision = xCollision || zCollision;
      this.verticalCollision = movement.y != adjustedMovement.y;
      this.onGround = this.verticalCollision && movement.y < 0.0;
      if (!this.isTouchingWater()) {
         this.checkWaterState();
      }

      if (this.onGround) {
         this.onLanding();
      } else if (movement.y < 0.0) {
         this.fallDistance = this.fallDistance - (float)movement.y;
      }

      Vec3d currentVel = this.velocity;
      if (this.horizontalCollision || this.verticalCollision) {
         this.velocity = new Vec3d(xCollision ? 0.0 : currentVel.x, this.onGround ? 0.0 : currentVel.y, zCollision ? 0.0 : currentVel.z);
      }
   }

   private Vec3d adjustMovementForCollisions(Vec3d movement) {
      Box box = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(this.pos);
      List<VoxelShape> collisionShapes = Collections.emptyList();
      Vec3d adjusted;
      if (movement.lengthSquared() == 0.0) {
         adjusted = movement;
      } else {
         adjusted = Entity.adjustMovementForCollisions(this.player, movement, box, this.player.getEntityWorld(), collisionShapes);
      }

      boolean xCollide = movement.x != adjusted.x;
      boolean yCollide = movement.y != adjusted.y;
      boolean zCollide = movement.z != adjusted.z;
      boolean stepPossible = this.onGround || yCollide && movement.y < 0.0;
      if (this.player.getStepHeight() > 0.0F && stepPossible && (xCollide || zCollide)) {
         Vec3d stepAdjust = Entity.adjustMovementForCollisions(
            this.player, new Vec3d(movement.x, (double)this.player.getStepHeight(), movement.z), box, this.player.getEntityWorld(), collisionShapes
         );
         Vec3d stepOffset = Entity.adjustMovementForCollisions(
            this.player,
            new Vec3d(0.0, (double)this.player.getStepHeight(), 0.0),
            box.stretch(movement.x, 0.0, movement.z),
            this.player.getEntityWorld(),
            collisionShapes
         );
         Vec3d combined = Entity.adjustMovementForCollisions(
               this.player, new Vec3d(movement.x, 0.0, movement.z), box.offset(stepOffset), this.player.getEntityWorld(), collisionShapes
            )
            .add(stepOffset);
         if (stepOffset.y < (double)this.player.getStepHeight() && combined.horizontalLengthSquared() > stepAdjust.horizontalLengthSquared()) {
            stepAdjust = combined;
         }

         if (stepAdjust.horizontalLengthSquared() > adjusted.horizontalLengthSquared()) {
            return stepAdjust.add(
               Entity.adjustMovementForCollisions(
                  this.player, new Vec3d(0.0, -stepAdjust.y + movement.y, 0.0), box.offset(stepAdjust), this.player.getEntityWorld(), collisionShapes
               )
            );
         }
      }

      return adjusted;
   }

   private void onLanding() {
      this.fallDistance = 0.0F;
   }

   public void jump() {
      this.velocity = this.velocity.add(0.0, (double)this.getJumpVelocity() - this.velocity.y, 0.0);
      if (this.isSprinting()) {
         float rad = (float)Math.toRadians((double)this.yaw);
         this.velocity = this.velocity.add((double)(-MathHelper.sin((double)rad)) * 0.2, 0.0, (double)MathHelper.cos((double)rad) * 0.2);
      }
   }

   private Vec3d applyClimbingSpeed(Vec3d motion) {
      if (!this.isClimbing()) {
         return motion;
      } else {
         this.onLanding();
         double clampedX = MathHelper.clamp(motion.x, -0.15F, 0.15F);
         double clampedZ = MathHelper.clamp(motion.z, -0.15F, 0.15F);
         double clampedY = Math.max(motion.y, -0.15F);
         if (clampedY < 0.0 && !this.getState(this.posToBlockPos(this.pos)).isOf(Blocks.SCAFFOLDING) && this.player.isHoldingOntoLadder()) {
            clampedY = 0.0;
         }

         return new Vec3d(clampedX, clampedY, clampedZ);
      }
   }

   public boolean isClimbing() {
      BlockPos posBlock = this.posToBlockPos(this.pos);
      BlockState state = this.getState(posBlock);
      return state.isIn(BlockTags.CLIMBABLE) ? true : state.getBlock() instanceof TrapdoorBlock && this.canEnterTrapdoor(posBlock, state);
   }

   private boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
      if (!(Boolean)state.get(TrapdoorBlock.OPEN)) {
         return false;
      } else {
         BlockState below = this.player.getEntityWorld().getBlockState(pos.down());
         return below.isOf(Blocks.LADDER) && ((Direction)below.get(LadderBlock.FACING)).equals(state.get(TrapdoorBlock.FACING));
      }
   }

   private Vec3d adjustMovementForSneaking(Vec3d movement) {
      if (movement.y <= 0.0 && this.wouldClipAtLedge()) {
         double dx = movement.x;
         double dz = movement.z;

         double step;
         for (step = 0.05;
            dx != 0.0 && this.player.getEntityWorld().isSpaceEmpty(this.player, this.boundingBox.offset(dx, -0.5, 0.0));
            dx += dx > 0.0 ? -step : step
         ) {
            if (dx < step && dx >= -step) {
               dx = 0.0;
               break;
            }
         }

         while (dz != 0.0 && this.player.getEntityWorld().isSpaceEmpty(this.player, this.boundingBox.offset(0.0, -0.5, dz))) {
            if (dz < step && dz >= -step) {
               dz = 0.0;
               break;
            }

            dz += dz > 0.0 ? -step : step;
         }

         while (dx != 0.0 && dz != 0.0 && this.player.getEntityWorld().isSpaceEmpty(this.player, this.boundingBox.offset(dx, -0.5, dz))) {
            dx = dx < step && dx >= -step ? 0.0 : (dx > 0.0 ? dx - step : dx + step);
            if (dz < step && dz >= -step) {
               dz = 0.0;
               break;
            }

            dz += dz > 0.0 ? -step : step;
         }

         if (movement.x != dx || movement.z != dz) {
            this.clipLedged = true;
         }

         if (this.shouldClipAtLedge()) {
            movement = new Vec3d(dx, movement.y, dz);
         }
      }

      return movement;
   }

   protected boolean shouldClipAtLedge() {
      return this.input.keyPresses.sneak() || this.input.forceSafeWalk;
   }

   private boolean wouldClipAtLedge() {
      return this.onGround
         || (double)this.fallDistance < 0.5
            && !this.player.getEntityWorld().isSpaceEmpty(this.player, this.boundingBox.offset(0.0, (double)this.fallDistance - 0.5, 0.0));
   }

   private boolean isSprinting() {
      return this.sprinting;
   }

   private float getJumpVelocity() {
      return 0.42F * this.getJumpVelocityMultiplier() + this.getJumpBoostVelocityModifier();
   }

   private float getJumpBoostVelocityModifier() {
      if (this.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
         StatusEffectInstance boost = this.getStatusEffect(StatusEffects.JUMP_BOOST);
         return 0.1F * (float)(boost.getAmplifier() + 1);
      } else {
         return 0.0F;
      }
   }

   private float getJumpVelocityMultiplier() {
      float multiplier1 = 0.0F;
      Block block = this.getState(this.posToBlockPos(this.pos)).getBlock();
      if (block != null) {
         multiplier1 = block.getJumpVelocityMultiplier();
      }

      float multiplier2 = 0.0F;
      Block block2 = this.getState(this.getVelocityAffectingPos()).getBlock();
      if (block2 != null) {
         multiplier2 = block2.getJumpVelocityMultiplier();
      }

      return multiplier1 == 1.0F ? multiplier2 : multiplier1;
   }

   private boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
      return this.doesNotCollide(this.boundingBox.offset(offsetX, offsetY, offsetZ));
   }

   private boolean doesNotCollide(Box box) {
      return this.player.getEntityWorld().isSpaceEmpty(this.player, box) && !this.player.getEntityWorld().containsFluid(box);
   }

   private void swimUpward(TagKey<Fluid> fluidTag) {
      this.velocity = this.velocity.add(0.0, 0.04F, 0.0);
   }

   private BlockPos getVelocityAffectingPos() {
      return BlockPos.ofFloored(this.pos.x, this.boundingBox.minY - 0.5000001, this.pos.z);
   }

   private double getSwimHeight() {
      return (double)this.player.getStandingEyeHeight() < 0.4 ? 0.0 : 0.4;
   }

   private boolean isTouchingWater() {
      return this.touchingWater;
   }

   public boolean isInLava() {
      return this.fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
   }

   private void checkWaterState() {
      if (this.player.getVehicle() instanceof AbstractBoatEntity boat && !boat.isSubmergedInWater()) {
         this.touchingWater = false;
         return;
      }

      if (this.updateMovementInFluid(FluidTags.WATER, 0.014)) {
         this.onLanding();
         this.touchingWater = true;
      } else {
         this.touchingWater = false;
      }
   }

   private void updateSwimming() {
      if (this.isSwimming) {
         this.isSwimming = this.isSprinting() && this.isTouchingWater() && !this.player.hasVehicle();
      } else {
         this.isSwimming = this.isSprinting()
            && this.isSubmergedInWater()
            && !this.player.hasVehicle()
            && this.player.getEntityWorld().getFluidState(this.posToBlockPos(this.pos)).isIn(FluidTags.WATER);
      }
   }

   private void updateSubmergedInWaterState() {
      this.submergedInWater = this.submergedFluidTag.contains(FluidTags.WATER);
      this.submergedFluidTag.clear();
      double eyeLevel = this.getEyeY() - 0.11111111F;
      if (this.player.getVehicle() instanceof AbstractBoatEntity boat
         && !boat.isSubmergedInWater()
         && boat.getBoundingBox().maxY >= eyeLevel
         && boat.getBoundingBox().minY <= eyeLevel) {
         return;
      }

      BlockPos posEye = BlockPos.ofFloored(this.pos.x, eyeLevel, this.pos.z);
      FluidState fluidState = this.player.getEntityWorld().getFluidState(posEye);
      double height = (double)((float)posEye.getY() + fluidState.getHeight(this.player.getEntityWorld(), posEye));
      if (height > eyeLevel) {
         this.submergedFluidTag.addAll(fluidState.streamTags().toList());
      }
   }

   private double getEyeY() {
      return this.pos.y + (double)this.player.getStandingEyeHeight();
   }

   public boolean isSubmergedInWater() {
      return this.submergedInWater && this.isTouchingWater();
   }

   private double getFluidHeight(TagKey<Fluid> tag) {
      return this.fluidHeight.getDouble(tag);
   }

   private boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
      if (this.isRegionUnloaded()) {
         return false;
      } else {
         Box box = this.boundingBox.contract(0.001);
         int i = MathHelper.floor(box.minX);
         int j = MathHelper.ceil(box.maxX);
         int k = MathHelper.floor(box.minY);
         int l = MathHelper.ceil(box.maxY);
         int m = MathHelper.floor(box.minZ);
         int n = MathHelper.ceil(box.maxZ);
         double d = 0.0;
         boolean pushedByFluids = true;
         boolean foundFluid = false;
         Vec3d fluidVelocity = Vec3d.ZERO;
         int count = 0;
         Mutable mutable = new Mutable();

         for (int p = i; p < j; p++) {
            for (int q = k; q < l; q++) {
               for (int r = m; r < n; r++) {
                  mutable.set(p, q, r);
                  FluidState fluidState = this.player.getEntityWorld().getFluidState(mutable);
                  if (fluidState.isIn(tag)) {
                     double e = (double)((float)q + fluidState.getHeight(this.player.getEntityWorld(), mutable));
                     if (e >= box.minY) {
                        foundFluid = true;
                        d = Math.max(e - box.minY, d);
                        if (pushedByFluids) {
                           Vec3d vel = fluidState.getVelocity(this.player.getEntityWorld(), mutable);
                           if (d < 0.4) {
                              vel = vel.multiply(d);
                           }

                           fluidVelocity = fluidVelocity.add(vel);
                           count++;
                        }
                     }
                  }
               }
            }
         }

         if (fluidVelocity.length() > 0.0) {
            if (count > 0) {
               fluidVelocity = fluidVelocity.multiply(1.0 / (double)count);
            }

            fluidVelocity = fluidVelocity.multiply(speed);
            if (Math.abs(this.velocity.x) < 0.003 && Math.abs(this.velocity.z) < 0.003 && fluidVelocity.length() < 0.0045) {
               fluidVelocity = fluidVelocity.normalize().multiply(0.0045);
            }

            this.velocity = this.velocity.add(fluidVelocity);
         }

         this.fluidHeight.put(tag, d);
         return foundFluid;
      }
   }

   private boolean isRegionUnloaded() {
      Box box = this.boundingBox.expand(1.0);
      int i = MathHelper.floor(box.minX);
      int j = MathHelper.ceil(box.maxX);
      int k = MathHelper.floor(box.minZ);
      int l = MathHelper.ceil(box.maxZ);
      return !this.player.getEntityWorld().isRegionLoaded(i, k, j, l);
   }

   private Vec3d getRotationVector() {
      return this.getRotationVector(this.pitch, this.yaw);
   }

   private Vec3d getRotationVector(float pitch, float yaw) {
      float f = (float)((double)pitch * Math.PI / 180.0);
      float g = (float)((double)(-yaw) * Math.PI / 180.0);
      float h = MathHelper.cos((double)g);
      float i = MathHelper.sin((double)g);
      float j = MathHelper.cos((double)f);
      float k = MathHelper.sin((double)f);
      return new Vec3d((double)(i * j), (double)(-k), (double)(h * j));
   }

   public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance instance = this.player.getStatusEffect(effect);
      return instance != null && instance.getDuration() >= this.simulatedTicks;
   }

   private StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
      StatusEffectInstance instance = this.player.getStatusEffect(effect);
      return instance != null && instance.getDuration() >= this.simulatedTicks ? instance : null;
   }

   public double getAttributeValue(RegistryEntry<EntityAttribute> attribute) {
      return this.player.getAttributes().getValue(attribute);
   }

   public PlayerSimulation clone() {
      return new PlayerSimulation(
         this.player,
         this.input,
         this.pos,
         this.velocity,
         this.boundingBox,
         this.yaw,
         this.pitch,
         this.sprinting,
         this.fallDistance,
         this.jumpingCooldown,
         this.isJumping,
         this.isFallFlying,
         this.onGround,
         this.horizontalCollision,
         this.verticalCollision,
         this.touchingWater,
         this.isSwimming,
         this.submergedInWater,
         new Object2DoubleArrayMap(this.fluidHeight),
         new HashSet<>(this.submergedFluidTag)
      );
   }

   public BlockPos posToBlockPos(Vec3d pos) {
      return new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z));
   }

   public BlockState getState(BlockPos pos) {
      return this.player.getEntityWorld().getBlockState(pos);
   }

   public static class SimulatedPlayerInput extends Input {
      public boolean forceSafeWalk = false;
      public float movementForward;
      public float movementSideways;
      public PlayerInput keyPresses;
      public static final double MAX_WALKING_SPEED = 0.121;

      public SimulatedPlayerInput(PlayerInput input) {
         this.keyPresses = input;
      }

      public void update() {
         if (this.keyPresses.forward() != this.keyPresses.backward()) {
            this.movementForward = this.keyPresses.forward() ? 1.0F : -1.0F;
         } else {
            this.movementForward = 0.0F;
         }

         if (this.keyPresses.left() == this.keyPresses.right()) {
            this.movementSideways = 0.0F;
         } else {
            this.movementSideways = this.keyPresses.left() ? 1.0F : -1.0F;
         }

         if (this.keyPresses.sneak()) {
            this.movementSideways *= 0.3F;
            this.movementForward *= 0.3F;
         }
      }

      public String toString() {
         return "SimulatedPlayerInput(forwards={"
            + this.keyPresses.forward()
            + "}, backwards={"
            + this.keyPresses.backward()
            + "}, left={"
            + this.keyPresses.left()
            + "}, right={"
            + this.keyPresses.right()
            + "}, jumping={"
            + this.keyPresses.jump()
            + "}, sprinting="
            + this.keyPresses.sprint()
            + ", slowDown="
            + this.keyPresses.sneak()
            + ")";
      }

      public static PlayerSimulation.SimulatedPlayerInput fromClientPlayer(PlayerInput input) {
         return new PlayerSimulation.SimulatedPlayerInput(input);
      }

      public static PlayerSimulation.SimulatedPlayerInput guessInput(PlayerEntity entity) {
         Vec3d velocity = entity.getEntityPos().subtract(new Vec3d(entity.lastX, entity.lastY, entity.lastZ));
         double horizontalVelocity = velocity.horizontalLengthSquared();
         PlayerInput input = new PlayerInput(false, false, false, false, !entity.isOnGround(), entity.isSneaking(), horizontalVelocity >= 0.014641);
         if (horizontalVelocity > 0.0025000000000000005) {
            double velocityAngle = MoveUtil.getDegreesRelativeToView(velocity, entity.getYaw());
            double wrappedAngle = MathHelper.wrapDegrees(velocityAngle);
            input = MoveUtil.getDirectionalInputForDegrees(input, wrappedAngle);
         }

         return new PlayerSimulation.SimulatedPlayerInput(input);
      }
   }
}
