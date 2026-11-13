package daamky.ua.api.module.impl.combat;

import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket.Entry;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.ModeSetting;

public class AntiBot extends Module {
   private static final MinecraftClient MC = MinecraftClient.getInstance();
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
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
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
            case PlayerListS2CPacket packet:
               packet.getPlayerAdditionEntries().forEach(this::checkPlayerAfterSpawn);
               break;
            case PlayerRemoveS2CPacket packetx:
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
      if (MC.world != null) {
         for (UUID uuid : Set.copyOf(this.suspectSet)) {
            PlayerEntity player = MC.world.getPlayerByUuid(uuid);
            if (player != null && (this.isFullyEquipped(player) || hasForeignUuid(player) || this.isNameBot(player))) {
               BOT_SET.add(uuid);
            }

            this.suspectSet.remove(uuid);
         }

         if (MC.player != null && MC.player.age % 100 == 0) {
            BOT_SET.removeIf(uuidx -> MC.world.getPlayerByUuid(uuidx) == null);
         }
      }
   }

   private void reallyWorldMode() {
      if (MC.world != null) {
         for (PlayerEntity player : MC.world.getPlayers()) {
            if (player != MC.player && hasForeignUuid(player)) {
               BOT_SET.add(player.getUuid());
            }
         }
      }
   }

   private boolean isDuplicateProfile(GameProfile profile) {
      return MC.getNetworkHandler() != null
         && MC.getNetworkHandler()
               .getPlayerList()
               .stream()
               .filter(player -> player.getProfile().name().equals(profile.name()) && !player.getProfile().id().equals(profile.id()))
               .count()
            == 1L;
   }

   private boolean isFullyEquipped(PlayerEntity player) {
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         ItemStack stack = player.getEquippedStack(slot);
         EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
         if (stack.isEmpty() || equippable == null || stack.hasEnchantments()) {
            return false;
         }
      }

      return true;
   }

   public boolean isBot(PlayerEntity player) {
      return player != null && (BOT_SET.contains(player.getUuid()) || this.isNameBot(player) || this.isBotU(player) || hasForeignUuid(player));
   }

   public static boolean shouldIgnore(PlayerEntity player) {
      return instance != null && instance.isEnabled() && instance.isBot(player);
   }

   public static boolean isKnownBot(UUID uuid) {
      return BOT_SET.contains(uuid);
   }

   public static boolean hasForeignUuid(PlayerEntity player) {
      if (player == null) {
         return false;
      } else {
         String name = player.getName().getString();
         return !name.contains("NPC") && !name.startsWith("[ZNPC]")
            ? !player.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)))
            : false;
      }
   }

   private boolean isNameBot(PlayerEntity player) {
      String name = player.getName().getString();
      return name.startsWith("CIT-") && !name.contains("NPC") && !name.startsWith("[ZNPC]");
   }

   private boolean isBotU(Entity entity) {
      String name = entity.getName().getString();
      return entity.isInvisible()
         && !name.contains("NPC")
         && !name.startsWith("[ZNPC]")
         && !entity.getUuid().equals(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)));
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
