package daamky.ua.api.module.impl.visual.particles;

import java.util.Random;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

final class ParticleWorld {
   private ParticleWorld() {
   }

   static Vec3d entityVecForParticle(MinecraftClient client, Entity entityFor, float yMultiplier) {
      Entity entity = (Entity)(entityFor == null ? client.player : entityFor);
      if (entity == null) {
         return Vec3d.ZERO;
      } else {
         float eyeHeight = client.player == null ? entity.getStandingEyeHeight() : client.player.getStandingEyeHeight();
         return entity.getEyePos().add(0.0, (double)(-eyeHeight * yMultiplier), 0.0);
      }
   }

   static Vec3d randomSpawnPosition(MinecraftClient client, Random random, ParticleSettings settings, Vec3d at, Entity entityFor) {
      if (client.world == null) {
         return null;
      } else {
         boolean worldPoint = entityFor == null || entityFor == client.player;
         float maxDistance = worldPoint ? settings.maxSpawnDistanceInWorld.getFloat() : settings.maxSpawnDistanceOnAttack.getFloat();

         for (int attempt = 0; attempt < 10; attempt++) {
            Vec3d vec = ParticleMath.randomVec(random, at, maxDistance / 5.0F, maxDistance);
            if (!isSolidCollisionBlock(client, vec.x, vec.y, vec.z)) {
               return vec;
            }
         }

         return null;
      }
   }

   static boolean isSolidCollisionBlock(MinecraftClient client, double x, double y, double z) {
      if (client.world == null) {
         return false;
      } else {
         BlockPos pos = BlockPos.ofFloored(x, y, z);
         BlockState state = client.world.getBlockState(pos);
         return !state.isAir() && !state.getCollisionShape(client.world, pos).isEmpty();
      }
   }
}
