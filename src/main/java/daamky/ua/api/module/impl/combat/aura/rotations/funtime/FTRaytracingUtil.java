package daamky.ua.api.module.impl.combat.aura.rotations.funtime;

import java.util.Objects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.impl.combat.aura.AngleConnection;

public final class FTRaytracingUtil {
   private static final MinecraftClient MC = MinecraftClient.getInstance();

   private FTRaytracingUtil() {
   }

   public static boolean rayTrace(double range, Box box) {
      Vec3d camera = Objects.requireNonNull(MC.player).getEyePos();
      Vec3d direction = AngleConnection.INSTANCE.getRotation().toVector();
      return box.contains(camera) || box.raycast(camera, camera.add(direction.multiply(range))).isPresent();
   }
}
