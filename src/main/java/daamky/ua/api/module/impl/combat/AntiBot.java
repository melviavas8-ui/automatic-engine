package daamky.ua.api.module.impl.combat;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket.Entry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;

public class AntiBot extends Module {
   private static final Minecraft MC = Minecraft.getInstance();
   private static final Set<UUID> BOT_SET = new HashSet<>();
   private static final EquipmentSlot[] ARMOR_SLOTS = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
   private static AntiBot instance;
   private final Set<UUID> suspectSet = new HashSet<>();
   private final ModeSetting mode = this.register(new ModeSetting("Mode", "Bot detection mode.", "ReallyWorld", "Matrix", "ReallyWorld"));

   public static AntiBot getInstance() {
      return instance;
   }

   public AntiBot() {
      super("Anti Bot", "Filters fake server-side entities.", ModuleCategory.COMBAT);
      instance = this;
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         if (this.mode.is("Matrix")) {
            this.matrixMode();
         } else {
            this.reallyWorldMode();
         }
      } else {
         this.reset();
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive()) {
         Packet var10000 = event.getPacket();
         Objects.requireNonNull(var10000);
         Object var2 = var10000;
         switch (var2) {
            case ClientboundPlayerInfoUpdatePacket packet:
               packet.newEntries().forEach(this::checkPlayerAfterSpawn);
               break;
            case ClientboundPlayerInfoRemovePacket packetx:
               packetx.profileIds().forEach(uuid -> {
                  this.suspectSet.remove(uuid);
                  BOT_SET.remove(uuid);
               });
               break;
            default:
               break;
         }
      }
   }

   private void checkPlayerAfterSpawn(Entry entry) {
      GameProfile profile = entry.profile();
      if (profile != null && entry.latency() >= 2 && (profile.properties() == null || profile.properties().isEmpty())) {
         if (this.isDuplicateProfile(profile)) {
            BOT_SET.add(profile.id());
         } else {
            this.suspectSet.add(profile.id());
         }
      }
   }

   private void matrixMode() {
      if (MC.level != null) {
         for (UUID uuid : Set.copyOf(this.suspectSet)) {
            Player player = MC.level.getPlayerByUUID(uuid);
            if (player != null && (this.isFullyEquipped(player) || hasForeignUuid(player) || this.isNameBot(player))) {
               BOT_SET.add(uuid);
            }

            this.suspectSet.remove(uuid);
         }

         if (MC.player != null && MC.player.tickCount % 100 == 0) {
            BOT_SET.removeIf(uuidx -> MC.level.getPlayerByUUID(uuidx) == null);
         }
      }
   }

   private void reallyWorldMode() {
      if (MC.level != null) {
         for (Player player : MC.level.players()) {
            if (player != MC.player && hasForeignUuid(player)) {
               BOT_SET.add(player.getUUID());
            }
         }
      }
   }

   private boolean isDuplicateProfile(GameProfile profile) {
      return MC.getConnection() != null
         && MC.getConnection()
               .getOnlinePlayers()
               .stream()
               .filter(player -> player.getProfile().name().equals(profile.name()) && !player.getProfile().id().equals(profile.id()))
               .count()
            == 1L;
   }

   private boolean isFullyEquipped(Player player) {
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = player.getItemBySlot(slot);
         Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
         if (stack.isEmpty() || equippable == null || stack.isEnchanted()) {
            return false;
         }
      }

      return true;
   }

   public boolean isBot(Player player) {
      return player != null && (BOT_SET.contains(player.getUUID()) || this.isNameBot(player) || this.isBotU(player) || hasForeignUuid(player));
   }

   public static boolean shouldIgnore(Player player) {
      return instance != null && instance.isEnabled() && instance.isBot(player);
   }

   public static boolean isKnownBot(UUID uuid) {
      return BOT_SET.contains(uuid);
   }

   public static boolean hasForeignUuid(Player player) {
      if (player == null) {
         return false;
      } else {
         String name = player.getName().getString();
         return !name.contains("NPC") && !name.startsWith("[ZNPC]")
            ? !player.getUUID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)))
            : false;
      }
   }

   private boolean isNameBot(Player player) {
      String name = player.getName().getString();
      return name.startsWith("CIT-") && !name.contains("NPC") && !name.startsWith("[ZNPC]");
   }

   private boolean isBotU(Entity entity) {
      String name = entity.getName().getString();
      return entity.isInvisible()
         && !name.contains("NPC")
         && !name.startsWith("[ZNPC]")
         && !entity.getUUID().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
   }

   private void reset() {
      this.suspectSet.clear();
      BOT_SET.clear();
   }

   @Override
   protected void onDisable() {
      this.reset();
   }
}
