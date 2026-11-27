package daamky.ua.api.module.impl.movement;

import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
   private Vec3 initialPosition;
   private Vec3 deathPosition;

   public TPloot() {
      super("ElytraLoot", "Лутает ресурсы.", ModuleCategory.MOVEMENT);
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         if (client.player.getHealth() <= 0.0F) {
            this.deathPosition = client.player.position();
         }

         if (this.deathPosition != null && client.player.getHealth() > 0.0F) {
            client.player.setPos(this.deathPosition.x, this.deathPosition.y, this.deathPosition.z);
            this.deathPosition = null;
         }

         if (this.initialPosition == null) {
            this.initialPosition = client.player.position();
         }

         this.pickUpLoot(client);
      }
   }

   @Override
   protected void onDisable() {
      this.initialPosition = null;
      this.deathPosition = null;
   }

   private void pickUpLoot(Minecraft client) {
      Vec3 playerPos = client.player.position();
      AABB searchBox = new AABB(playerPos.x - 100.0, playerPos.y - 100.0, playerPos.z - 100.0, playerPos.x + 100.0, playerPos.y + 100.0, playerPos.z + 100.0);
      List<ItemEntity> nearbyItems = client.level.getEntitiesOfClass(ItemEntity.class, searchBox, this::isLootTarget);
      if (nearbyItems.isEmpty()) {
         if (this.hub.getValue()) {
            this.returnToInitialPosition(client);
            mc.player.connection.sendCommand("/hub");
         } else {
            this.returnToInitialPosition(client);
         }
      } else {
         ItemEntity item = nearbyItems.getFirst();
         this.moveToLoot(client, item);
         this.applyKnockbackToNearbyPlayers(client, item.getX(), item.getY(), item.getZ());
      }
   }

   private void moveToLoot(Minecraft client, ItemEntity item) {
      double currentY = client.player.getY();
      double targetY = item.getY();
      double speed = 1.8;
      double nextY = targetY;
      if (targetY < currentY) {
         nextY = Math.max(targetY, currentY - speed);
      } else if (targetY > currentY) {
         nextY = Math.min(targetY, currentY + speed);
      }

      client.player.setPos(item.getX(), nextY, item.getZ());
   }

   private boolean isLootTarget(ItemEntity entity) {
      if (entity == null) {
         return false;
      } else {
         ItemStack stack = entity.getItem();
         if (stack.isEmpty()) {
            return false;
         } else {
            Item item = stack.getItem();
            return item == Items.GOLDEN_APPLE ? stack.getCount() == 12 : VALUABLE_ITEMS.contains(item);
         }
      }
   }

   private void returnToInitialPosition(Minecraft client) {
      if (this.initialPosition != null) {
         if (this.moveToPosition(client, this.initialPosition, 1.8)) {
            this.initialPosition = null;
         }
      }
   }

   private boolean moveToPosition(Minecraft client, Vec3 target, double speed) {
      Vec3 current = client.player.position();
      Vec3 offset = target.subtract(current);
      double distance = offset.length();
      if (distance <= Math.max(0.05, speed)) {
         client.player.setPos(target.x, target.y, target.z);
         return true;
      } else {
         Vec3 next = current.add(offset.normalize().scale(speed));
         client.player.setPos(next.x, next.y, next.z);
         return false;
      }
   }

   private void applyKnockbackToNearbyPlayers(Minecraft client, double itemX, double itemY, double itemZ) {
      AABB box = new AABB(itemX - 3.0, itemY - 3.0, itemZ - 3.0, itemX + 3.0, itemY + 3.0, itemZ + 3.0);

      for (Player player : client.level.getEntitiesOfClass(Player.class, box, playerx -> playerx != client.player)) {
         Vec3 direction = player.position().subtract(itemX, itemY, itemZ).normalize();
         player.push(direction.x * 1111.5, direction.y * 1111.5, direction.z * 1111.5);
      }
   }
}
