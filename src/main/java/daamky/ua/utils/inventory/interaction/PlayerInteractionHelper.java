package daamky.ua.utils.inventory.interaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil.Type;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import daamky.ua.IMinecraft;
import daamky.ua.api.module.impl.combat.aura.Angle;
import daamky.ua.api.module.impl.combat.aura.MathAngle;
import daamky.ua.api.settings.bind.KeyBind;
import daamky.ua.api.settings.impl.BindSetting;
import daamky.ua.utils.inventory.lookup.InventoryUtils;

public final class PlayerInteractionHelper implements IMinecraft {
   private PlayerInteractionHelper() {
   }

   public static void sendSequencedPacket(SequencedPacketCreator packetCreator) {
      if (mc.world != null && mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendPacket(packetCreator.predict(0));
      }
   }

   public static void interactItem(Hand hand) {
      interactItem(hand, MathAngle.cameraAngle());
   }

   public static void interactItem(Hand hand, Angle angle) {
      if (angle == null) {
         interactItem(hand);
      } else {
         InventoryUtils.sendUsePacket(hand, angle.getYaw(), angle.getPitch());
      }
   }

   public static void interactEntity(Entity entity) {
      if (mc.interactionManager != null && mc.player != null && entity != null) {
         mc.interactionManager.interactEntity(mc.player, entity, Hand.MAIN_HAND);
      }
   }

   public static void sendPacketWithOutEvent(Packet<?> packet) {
      mc.getNetworkHandler().getConnection().send(packet, null);
   }

   public static void grimSuperBypass$$$(double y, Angle angle) {
      mc.player
         .networkHandler
         .sendPacket(
            new Full(
               mc.player.getX(),
               mc.player.getY() + y,
               mc.player.getZ(),
               angle.getYaw(),
               angle.getPitch(),
               mc.player.isOnGround(),
               mc.player.horizontalCollision
            )
         );
   }

   public static String getHealthString(float hp) {
      return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
   }

   public static void jump() {
      if (mc.player.isSprinting()) {
         float g = mc.player.getYaw() * (float) (Math.PI / 180.0);
         mc.player.addVelocityInternal(new Vec3d((double)(-MathHelper.sin((double)g) * 0.2F), 0.0, (double)(MathHelper.cos((double)g) * 0.2F)));
      }

      mc.player.knockedBack = true;
   }

   public static List<BlockPos> getCube(BlockPos center, float radius) {
      return getCube(center, radius, radius, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY) {
      return getCube(center, radiusXZ, radiusY, true);
   }

   public static List<BlockPos> getCube(BlockPos center, float radiusXZ, float radiusY, boolean down) {
      List<BlockPos> positions = new ArrayList<>();
      int centerX = center.getX();
      int centerY = center.getY();
      int centerZ = center.getZ();
      int posY = down ? centerY - (int)radiusY : centerY;

      for (int x = centerX - (int)radiusXZ; (float)x <= (float)centerX + radiusXZ; x++) {
         for (int z = centerZ - (int)radiusXZ; (float)z <= (float)centerZ + radiusXZ; z++) {
            for (int y = posY; (float)y <= (float)centerY + radiusY; y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static List<BlockPos> getCube(BlockPos start, BlockPos end) {
      List<BlockPos> positions = new ArrayList<>();

      for (int x = start.getX(); x <= end.getX(); x++) {
         for (int z = start.getZ(); z <= end.getZ(); z++) {
            for (int y = start.getY(); y <= end.getY(); y++) {
               positions.add(new BlockPos(x, y, z));
            }
         }
      }

      return positions;
   }

   public static Type getKeyType(int key) {
      return key < 8 ? Type.MOUSE : Type.KEYSYM;
   }

   public static Stream<Entity> streamEntities() {
      return StreamSupport.stream(mc.world.getEntities().spliterator(), false);
   }

   public static boolean canChangeIntoPose(EntityPose pose, Vec3d pos) {
      return mc.player.getEntityWorld().isSpaceEmpty(mc.player, mc.player.getDimensions(pose).getBoxAt(pos).contract(1.0E-7));
   }

   public static boolean isPotionActive(RegistryEntry<StatusEffect> statusEffect) {
      return mc.player.getActiveStatusEffects().containsKey(statusEffect);
   }

   public static boolean isPlayerInBlock(Block block) {
      return isBoxInBlock(mc.player.getBoundingBox().expand(-0.001), block);
   }

   public static boolean isBoxInBlock(Box box, Block block) {
      return isBox(box, pos -> mc.world.getBlockState(pos).getBlock().equals(block));
   }

   public static boolean isBoxInBlocks(Box box, List<Block> blocks) {
      return isBox(box, pos -> blocks.contains(mc.world.getBlockState(pos).getBlock()));
   }

   public static boolean isBox(Box box, Predicate<BlockPos> pos) {
      return BlockPos.stream(box).anyMatch(pos);
   }

   public static boolean isKey(BindSetting setting) {
      KeyBind bind = setting.getValue();
      return mc.currentScreen == null && setting.isVisible() && bind != null && mc.getWindow() != null && bind.isDown(mc.getWindow().getHandle());
   }

   public static boolean isKey(KeyBinding key) {
      return isKey(key.getDefaultKey().getCategory(), key.getDefaultKey().getCode());
   }

   public static boolean isKey(Type type, int keyCode) {
      if (keyCode != -1) {
         switch (type) {
            case KEYSYM:
               return GLFW.glfwGetKey(mc.getWindow().getHandle(), keyCode) == 1;
            case MOUSE:
               return GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), keyCode) == 1;
         }
      }

      return false;
   }

   public static boolean isAir(BlockPos blockPos) {
      return isAir(mc.world.getBlockState(blockPos));
   }

   public static boolean isAir(BlockState state) {
      return state.isAir() || state.getBlock().equals(Blocks.CAVE_AIR) || state.getBlock().equals(Blocks.VOID_AIR);
   }

   public static boolean isChat(Screen screen) {
      return screen instanceof ChatScreen;
   }

   public static boolean nullCheck() {
      return mc.player == null || mc.world == null;
   }
}
