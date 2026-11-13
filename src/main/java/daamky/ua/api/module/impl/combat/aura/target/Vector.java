package daamky.ua.api.module.impl.combat.aura.target;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class Vector {
   private Vector() {
   }

   public static Vec3d hitbox(Entity entity, float x, float y, float z, float width) {
      MinecraftClient client = MinecraftClient.getInstance();
      double wHalf = (double)(entity.getWidth() / width);
      double yExpand = MathHelper.clamp(entity.getEyeY() - entity.getY(), 0.0, (double)entity.getHeight());
      double xExpand = MathHelper.clamp(client.player.getX() - entity.getX(), -wHalf, wHalf);
      double zExpand = MathHelper.clamp(client.player.getZ() - entity.getZ(), -wHalf, wHalf);
      return new Vec3d(entity.getX() + xExpand / (double)x, entity.getY() + yExpand / (double)y, entity.getZ() + zExpand / (double)z);
   }
}
