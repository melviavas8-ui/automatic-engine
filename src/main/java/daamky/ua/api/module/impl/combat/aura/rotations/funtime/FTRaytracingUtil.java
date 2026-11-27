package daamky.ua.api.module.impl.combat.aura.rotations.funtime;

import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;

public final class FTRaytracingUtil {
   private static final Minecraft MC = Minecraft.getInstance();

   private FTRaytracingUtil() {
   }

   public static boolean rayTrace(double range, AABB box) {
      Vec3 camera = Objects.requireNonNull(MC.player).getEyePosition();
      Vec3 direction = AngleConnection.INSTANCE.getRotation().toVector();
      return box.contains(camera) || box.clip(camera, camera.add(direction.scale(range))).isPresent();
   }
}
