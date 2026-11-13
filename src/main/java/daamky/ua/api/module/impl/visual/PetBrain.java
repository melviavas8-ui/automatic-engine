package daamky.ua.api.module.impl.visual;

import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.AuraModule;
import daamky.ua.utils.timer.TimerUtil;

public final class PetBrain {
   private final MinecraftClient mc = MinecraftClient.getInstance();
   private Vec3d pos;
   private Vec3d motion = Vec3d.ZERO;
   private float direction = randomAngle();
   private float yaw;
   private float body;
   private final PetAnimation x = new PetAnimation();
   private final PetAnimation y = new PetAnimation();
   private final PetAnimation z = new PetAnimation();
   private final PetAnimation bodyAnim = new PetAnimation();
   private final PetAnimation yawAnim = new PetAnimation();
   private final PetAnimation pitchAnim = new PetAnimation();
   private boolean lay;
   private final TimerUtil staying = new TimerUtil();
   public float prevLimbSwingAmount;
   public float limbSwingAmount;
   public float limbSwing;
   private PlayerEntity entity;

   public PetBrain() {
   }

   public void setEntity(PlayerEntity entity) {
      this.entity = entity;
   }

   public void onUpdate() {
      if (this.entity != null && this.mc.world != null) {
         Vec3d playerPos = this.entity.getEntityPos();
         if (this.pos == null || this.pos.distanceTo(playerPos) > 10.0) {
            this.pos = playerPos;
            this.x.animate((float)this.pos.x, 1);
            this.y.animate((float)this.pos.y, 1);
            this.z.animate((float)this.pos.z, 1);
         }

         this.motion = this.motion.add(0.0, -0.2, 0.0);
         Vec3d newPos = this.pos.add(this.motion);
         if (this.isBlockSolid(newPos.x, newPos.y, newPos.z)) {
            int blockY = (int)newPos.y;
            double correctedY = (double)(blockY + 1) + 0.1;
            newPos = new Vec3d(newPos.x, correctedY, newPos.z);
            this.motion = new Vec3d(this.motion.x, 0.0, this.motion.z);
         }

         if (this.collidesHorizontally(newPos)) {
            newPos = new Vec3d(this.pos.x, newPos.y, this.pos.z);
            this.motion = new Vec3d(-this.motion.x * 0.35, this.motion.y, -this.motion.z * 0.35);
            this.direction = randomAngle();
         }

         this.motion = new Vec3d(this.motion.x, 0.0, this.motion.z);
         LivingEntity target = this.auraTarget();
         if (target != null && this.entity instanceof ClientPlayerEntity) {
            if (this.isBlockSolid(newPos.x, newPos.y - 0.1, newPos.z)) {
               this.motion = new Vec3d(this.motion.x, 0.62, this.motion.z);
            }

            Box box = new Box(this.getPos().subtract(new Vec3d(0.4, 0.0, 0.4)), this.getPos().add(0.4, 0.4, 0.4));
            Box targetBox = target.getBoundingBox().expand(-0.1, 0.0, -0.1);
            this.motion = this.motion.add(target.getEntityPos().subtract(newPos).normalize());
            if (box.maxX > targetBox.minX
               && box.maxY > targetBox.minY
               && box.maxZ > targetBox.minZ
               && box.minX < targetBox.maxX
               && box.minY < targetBox.maxY
               && box.minZ < targetBox.maxZ) {
               this.motion = this.motion.multiply(-1.0, 1.0, -1.0);
            }
         } else if (newPos.distanceTo(playerPos) > 2.0) {
            this.motion = this.motion.add(playerPos.subtract(newPos).normalize());
         }

         this.handleRotation();
         this.pos = newPos;
         if (this.pos.distanceTo(playerPos) < 0.1) {
            this.direction = randomAngle();
            double xMot = -Math.sin(Math.toRadians((double)this.direction)) * 0.1;
            double zMot = Math.cos(Math.toRadians((double)this.direction)) * 0.1;
            this.motion = this.motion.add(xMot, 0.0, zMot);
         }

         this.motion = this.motion.multiply(0.5);
         int speed = 150;
         this.x.animate((float)this.pos.x, speed);
         this.y.animate((float)this.pos.y, speed);
         this.z.animate((float)this.pos.z, speed);
         this.limbTick();
         if (Math.abs(this.pos.x - (double)this.x.get()) > 0.1F || Math.abs(this.pos.z - (double)this.z.get()) > 0.1F) {
            this.staying.reset();
         }

         this.lay = this.staying.isReached(1000L);
      }
   }

   private void handleRotation() {
      if (this.motion.x != 0.0 || this.motion.z != 0.0) {
         double angle = Math.atan2(this.motion.z, this.motion.x);
         this.yaw = (float)Math.toDegrees(angle) - 90.0F;
         this.yaw %= 360.0F;
         if (this.yaw < 0.0F) {
            this.yaw += 360.0F;
         }
      }

      float[] rotation = lookAt(this.pos, this.entity.getCameraPosVec(1.0F));
      LivingEntity target = this.auraTarget();
      if (target != null && this.entity instanceof ClientPlayerEntity) {
         rotation = lookAt(this.pos, target.getEntityPos());
      }

      float gradus = this.lay ? 200.0F : 150.0F;
      float gradus1 = this.lay ? 100.0F : 50.0F;
      if (rotation[0] - this.yaw < -gradus || rotation[0] - this.yaw > gradus) {
         this.yaw = rotation[0];
      }

      float shortestYawPath = ((this.yaw - this.body) % 360.0F + 540.0F) % 360.0F - 180.0F;
      if (!this.lay) {
         this.bodyAnim.animate(this.body + shortestYawPath, 150);
      }

      this.yawAnim.animate(MathHelper.clamp(rotation[0] - this.yaw, -gradus1, gradus1), 150);
      this.pitchAnim.animate(rotation[1], 150);
      this.body += shortestYawPath;
   }

   private void limbTick() {
      this.prevLimbSwingAmount = this.limbSwingAmount;
      double d0 = (double)this.x.get() - this.pos.x;
      double d2 = (double)this.z.get() - this.pos.z;
      float f = MathHelper.sqrt((float)(d0 * d0 + d2 * d2)) * 4.0F;
      if (f > 1.0F) {
         f = 1.0F;
      }

      this.limbSwingAmount = this.limbSwingAmount + (f - this.limbSwingAmount) * 0.4F;
      this.limbSwing = this.limbSwing + this.limbSwingAmount;
   }

   public float getBody() {
      return this.bodyAnim.get();
   }

   public float getYaw() {
      return this.yawAnim.get();
   }

   public float getPitch() {
      return this.pitchAnim.get();
   }

   public boolean isLay() {
      return this.lay;
   }

   public Vec3d getPos() {
      return new Vec3d((double)this.x.get(), (double)this.y.get(), (double)this.z.get());
   }

   private LivingEntity auraTarget() {
      AuraModule aura = AuraModule.getInstance();
      return aura != null && aura.isEnabled() ? AuraModule.target : null;
   }

   private static float[] lookAt(Vec3d from, Vec3d to) {
      double dx = to.x - from.x;
      double dy = to.y - from.y;
      double dz = to.z - from.z;
      double dxz = Math.sqrt(dx * dx + dz * dz);
      float yaw = (float)Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
      float pitch = (float)Math.toDegrees(-Math.atan2(dy, dxz));
      return new float[]{yaw, pitch};
   }

   private static float randomAngle() {
      return ThreadLocalRandom.current().nextFloat() * 360.0F;
   }

   private boolean isBlockSolid(double bx, double by, double bz) {
      if (this.mc.world == null) {
         return false;
      } else {
         BlockPos bp = BlockPos.ofFloored(bx, by, bz);
         return !this.mc.world.getBlockState(bp).getCollisionShape(this.mc.world, bp).isEmpty();
      }
   }

   private boolean collidesHorizontally(Vec3d position) {
      double radius = 0.35;
      double baseY = this.horizontalBaseY(position);
      double[] heights = new double[]{0.0, 0.45};

      for (double height : heights) {
         double sy = baseY + height;
         if (this.isBlockSolid(position.x + radius, sy, position.z + radius)
            || this.isBlockSolid(position.x + radius, sy, position.z - radius)
            || this.isBlockSolid(position.x - radius, sy, position.z + radius)
            || this.isBlockSolid(position.x - radius, sy, position.z - radius)) {
            return true;
         }
      }

      return false;
   }

   private double horizontalBaseY(Vec3d position) {
      double supportRadius = 0.32;
      double probeY = position.y - 0.2;
      double highest = Double.NEGATIVE_INFINITY;
      double[][] offsets = new double[][]{
         {0.0, 0.0}, {supportRadius, supportRadius}, {supportRadius, -supportRadius}, {-supportRadius, supportRadius}, {-supportRadius, -supportRadius}
      };

      for (double[] off : offsets) {
         if (this.isBlockSolid(position.x + off[0], probeY, position.z + off[1])) {
            highest = Math.max(highest, Math.floor(probeY) + 1.0);
         }
      }

      return highest == Double.NEGATIVE_INFINITY ? position.y + 0.05 : Math.max(position.y + 0.05, highest + 0.05);
   }
}
