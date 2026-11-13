package daamky.ua.api.module.impl.movement;

import java.util.List;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;

public class TPloot extends Module {
   private static final double LOOT_RADIUS = 100.0;
   private static final double KNOCKBACK_RADIUS = 3.0;
   private static final double KNOCKBACK_STRENGTH = 1111.5;
   private static final double ARRIVE_DISTANCE = 0.05;
   private final double descendSpeed = 1.8;
   private final double returnSpeed = 1.8;
   private final BooleanSetting hub = this.register(new BooleanSetting("Exit Hub", "Exit Hub", false));
   private static final Set<Item> VALUABLE_ITEMS = Set.of(
      Items.TOTEM_OF_UNDYING,
      Items.NETHERITE_HELMET,
      Items.NETHERITE_CHESTPLATE,
      Items.NETHERITE_LEGGINGS,
      Items.NETHERITE_BOOTS,
      Items.NETHERITE_SWORD,
      Items.NETHERITE_PICKAXE,
      Items.GOLDEN_APPLE,
      Items.ENCHANTED_GOLDEN_APPLE,
      Items.PLAYER_HEAD,
      Items.SHULKER_BOX,
      Items.NETHERITE_INGOT,
      Items.SPLASH_POTION,
      Items.ELYTRA,
      Items.FIREWORK_ROCKET
   );
   private Vec3d initialPosition;
   private Vec3d deathPosition;

   public TPloot() {
      super("ElytraLoot", "Лутает ресурсы.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
         if (client.player.getHealth() <= 0.0F) {
            this.deathPosition = client.player.getEntityPos();
         }

         if (this.deathPosition != null && client.player.getHealth() > 0.0F) {
            client.player.setPosition(this.deathPosition.x, this.deathPosition.y, this.deathPosition.z);
            this.deathPosition = null;
         }

         if (this.initialPosition == null) {
            this.initialPosition = client.player.getEntityPos();
         }

         this.pickUpLoot(client);
      }
   }

   @Override
   protected void onDisable() {
      this.initialPosition = null;
      this.deathPosition = null;
   }

   private void pickUpLoot(MinecraftClient client) {
      Vec3d playerPos = client.player.getEntityPos();
      Box searchBox = new Box(playerPos.x - 100.0, playerPos.y - 100.0, playerPos.z - 100.0, playerPos.x + 100.0, playerPos.y + 100.0, playerPos.z + 100.0);
      List<ItemEntity> nearbyItems = client.world.getEntitiesByClass(ItemEntity.class, searchBox, this::isLootTarget);
      if (nearbyItems.isEmpty()) {
         if (this.hub.getValue()) {
            this.returnToInitialPosition(client);
            mc.player.networkHandler.sendChatCommand("/hub");
         } else {
            this.returnToInitialPosition(client);
         }
      } else {
         ItemEntity item = nearbyItems.getFirst();
         this.moveToLoot(client, item);
         this.applyKnockbackToNearbyPlayers(client, item.getX(), item.getY(), item.getZ());
      }
   }

   private void moveToLoot(MinecraftClient client, ItemEntity item) {
      double currentY = client.player.getY();
      double targetY = item.getY();
      double speed = 1.8;
      double nextY = targetY;
      if (targetY < currentY) {
         nextY = Math.max(targetY, currentY - speed);
      } else if (targetY > currentY) {
         nextY = Math.min(targetY, currentY + speed);
      }

      client.player.setPosition(item.getX(), nextY, item.getZ());
   }

   private boolean isLootTarget(ItemEntity entity) {
      if (entity == null) {
         return false;
      } else {
         ItemStack stack = entity.getStack();
         if (stack.isEmpty()) {
            return false;
         } else {
            Item item = stack.getItem();
            return item == Items.GOLDEN_APPLE ? stack.getCount() == 12 : VALUABLE_ITEMS.contains(item);
         }
      }
   }

   private void returnToInitialPosition(MinecraftClient client) {
      if (this.initialPosition != null) {
         if (this.moveToPosition(client, this.initialPosition, 1.8)) {
            this.initialPosition = null;
         }
      }
   }

   private boolean moveToPosition(MinecraftClient client, Vec3d target, double speed) {
      Vec3d current = client.player.getEntityPos();
      Vec3d offset = target.subtract(current);
      double distance = offset.length();
      if (distance <= Math.max(0.05, speed)) {
         client.player.setPosition(target.x, target.y, target.z);
         return true;
      } else {
         Vec3d next = current.add(offset.normalize().multiply(speed));
         client.player.setPosition(next.x, next.y, next.z);
         return false;
      }
   }

   private void applyKnockbackToNearbyPlayers(MinecraftClient client, double itemX, double itemY, double itemZ) {
      Box box = new Box(itemX - 3.0, itemY - 3.0, itemZ - 3.0, itemX + 3.0, itemY + 3.0, itemZ + 3.0);

      for (PlayerEntity player : client.world.getEntitiesByClass(PlayerEntity.class, box, playerx -> playerx != client.player)) {
         Vec3d direction = player.getEntityPos().subtract(itemX, itemY, itemZ).normalize();
         player.addVelocity(direction.x * 1111.5, direction.y * 1111.5, direction.z * 1111.5);
      }
   }
}
