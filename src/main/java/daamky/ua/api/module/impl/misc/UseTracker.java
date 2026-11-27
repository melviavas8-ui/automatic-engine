package daamky.ua.api.module.impl.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.PacketEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.NumberSetting;

public final class UseTracker extends Module {
   private final BooleanSetting trackTotem = this.register(new BooleanSetting("Снос тотема", "Трекать снос тотема у игроков.", true));
   private final BooleanSetting trackPotions = this.register(new BooleanSetting("Полученные зелья", "Трекать попадания брошенных зелий.", true));
   private final BooleanSetting trackConsume = this.register(new BooleanSetting("Съеденный предмет", "Трекать еду и выпитые предметы игроков.", true));
   private final NumberSetting radius = this.register(new NumberSetting("Радиус зелий", "Радиус отслеживания брошенных зелий.", 100.0, 10.0, 100.0, 1.0));
   private final Map<Integer, UseTracker.PotionData> trackedPotions = new HashMap<>();
   private final Map<UUID, ItemStack> activeUseItem = new HashMap<>();
   private final Map<UUID, Integer> useStartTick = new HashMap<>();
   private final Set<Integer> currentPotionIds = new HashSet<>();
   private final Set<Integer> removedPotionIds = new HashSet<>();

   public UseTracker() {
      super("UseTracker", "Tracks player totems, potions and consumed items.", ModuleCategory.MISC);
      this.radius.setVisible(false);
   }

   @Override
   public void onTick(Minecraft client) {
      if (client.player != null && client.level != null) {
         if (this.trackConsume.getValue()) {
            this.trackConsumedItems(client);
         }

         if (this.trackPotions.getValue()) {
            this.trackThrownPotions(client);
         }
      }
   }

   @SubscribeEvent
   private void onPacket(PacketEvent event) {
      if (event.isReceive() && mc.level != null && mc.player != null) {
         if (this.trackTotem.getValue() && event.getPacket() instanceof ClientboundEntityEventPacket packet && packet.getEventId() == 35) {
            if (packet.getEntity(mc.level) instanceof Player player && !player.getUUID().equals(mc.player.getUUID())) {
               ItemStack totemStack = player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)
                  ? player.getOffhandItem()
                  : (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING) ? player.getMainHandItem() : player.getOffhandItem());
               Component totemName = (Component)(totemStack.isEmpty() ? Component.literal("Тотем бессмертия") : totemStack.getHoverName());
               boolean enchanted = player.getOffhandItem().isEnchanted() || player.getMainHandItem().isEnchanted();
               MutableComponent enchantPart = Component.literal(enchanted ? "зачарованный" : "незачарованный").withStyle(enchanted ? ChatFormatting.GREEN : ChatFormatting.RED);
               this.sendPrefixed(
                  Component.empty()
                     .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(" потерял ").withStyle(ChatFormatting.GRAY))
                     .append(totemName)
                     .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                     .append(enchantPart)
                     .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
               );
               return;
            }
         }
      }
   }

   @Override
   protected void onDisable() {
      this.trackedPotions.clear();
      this.activeUseItem.clear();
      this.useStartTick.clear();
      this.currentPotionIds.clear();
      this.removedPotionIds.clear();
   }

   private void trackConsumedItems(Minecraft client) {
      for (Player player : client.level.players()) {
         if (!player.getUUID().equals(client.player.getUUID())) {
            UUID id = player.getUUID();
            if (player.isUsingItem()) {
               this.activeUseItem.computeIfAbsent(id, ignored -> player.getUseItem().copy());
               this.useStartTick.putIfAbsent(id, player.tickCount);
            } else {
               ItemStack used = this.activeUseItem.remove(id);
               Integer startTick = this.useStartTick.remove(id);
               if (used != null && !used.isEmpty() && startTick != null && player.tickCount - startTick >= 31) {
                  ItemUseAnimation action = used.getUseAnimation();

                  String verb = switch (action) {
                     case DRINK -> "выпил";
                     case EAT -> "съел";
                     default -> null;
                  };
                  if (verb != null) {
                     String itemName = stripFormatting(used.getHoverName().getString());
                     this.chat(ChatFormatting.WHITE + player.getName().getString() + ChatFormatting.GRAY + " " + verb + " " + ChatFormatting.WHITE + itemName);
                  }
               }
            }
         }
      }
   }

   private void trackThrownPotions(Minecraft client) {
      Set<Integer> current = this.currentPotionIds;
      current.clear();
      float maxDistance = this.radius.getFloat();

      for (Entity entity : client.level.entitiesForRendering()) {
         if (entity instanceof AbstractThrownPotion) {
            AbstractThrownPotion potion = (AbstractThrownPotion)entity;
            if (!(client.player.distanceTo(potion) > maxDistance)) {
               int entityId = potion.getId();
               current.add(entityId);
               UseTracker.PotionData data = this.trackedPotions.get(entityId);
               if (data == null) {
                  this.trackedPotions.put(entityId, new UseTracker.PotionData(potion.getItem().copy(), potion.getX(), potion.getY(), potion.getZ()));
               } else {
                  data.lastX = potion.getX();
                  data.lastY = potion.getY();
                  data.lastZ = potion.getZ();
               }
            }
         }
      }

      Set<Integer> removed = this.removedPotionIds;
      removed.clear();
      removed.addAll(this.trackedPotions.keySet());
      removed.removeAll(current);

      for (int entityId : removed) {
         UseTracker.PotionData data = this.trackedPotions.remove(entityId);
         if (data != null) {
            this.notifyPotionHits(client, data);
         }
      }
   }

   private void notifyPotionHits(Minecraft client, UseTracker.PotionData data) {
      AABB hitBox = new AABB(data.lastX - 4.0, data.lastY - 2.0, data.lastZ - 4.0, data.lastX + 4.0, data.lastY + 2.0, data.lastZ + 4.0);

      for (LivingEntity hit : client.level.getEntitiesOfClass(LivingEntity.class, hitBox, ignored -> true)) {
         if (hit instanceof Player) {
            Player player = (Player)hit;
            double dx = player.getX() - data.lastX;
            double dz = player.getZ() - data.lastZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (!(distance > 4.0)) {
               double hitChance = Math.max(0.0, 1.0 - distance / 4.0) * 100.0;
               ChatFormatting hitColor = hitChance >= 65.0 ? ChatFormatting.GREEN : (hitChance >= 35.0 ? ChatFormatting.YELLOW : ChatFormatting.RED);
               this.sendPrefixed(
                  Component.empty()
                     .append(Component.literal(player.getName().getString()).withStyle(ChatFormatting.WHITE))
                     .append(Component.literal(" получил ").withStyle(ChatFormatting.GRAY))
                     .append(data.stack.getHoverName())
                     .append(Component.literal(" (").withStyle(ChatFormatting.GRAY))
                     .append(Component.literal(String.format("%.0f%%", hitChance)).withStyle(hitColor))
                     .append(Component.literal(")").withStyle(ChatFormatting.GRAY))
               );
            }
         }
      }
   }

   private void chat(String message) {
      if (mc.player != null) {
         this.sendPrefixed(parseLegacy(message));
      }
   }

   private void sendPrefixed(Component message) {
      if (mc.player != null) {
         MutableComponent text = Component.empty()
            .append(Component.literal("daamky").withStyle(style -> style.withBold(true).withColor(8385279)))
            .append(Component.literal(" » ").withStyle(ChatFormatting.DARK_GRAY))
            .append(message);
         mc.execute(() -> {
            if (mc.player != null) {
               mc.player.displayClientMessage(text, false);
            }
         });
      }
   }

   private static String stripFormatting(String text) {
      String stripped = ChatFormatting.stripFormatting(text);
      return stripped == null ? text : stripped;
   }

   private static MutableComponent parseLegacy(String message) {
      MutableComponent result = Component.empty();
      ChatFormatting currentColor = null;
      boolean bold = false;
      StringBuilder current = new StringBuilder();

      for (int i = 0; i < message.length(); i++) {
         char c = message.charAt(i);
         if (c == 167 && i + 1 < message.length()) {
            appendLegacyPart(result, current, currentColor, bold);
            current.setLength(0);
            ChatFormatting formatting = ChatFormatting.getByCode(message.charAt(++i));
            if (formatting != null) {
               if (formatting == ChatFormatting.BOLD) {
                  bold = true;
               } else if (formatting == ChatFormatting.RESET) {
                  currentColor = null;
                  bold = false;
               } else if (formatting.isColor()) {
                  currentColor = formatting;
               }
            }
         } else {
            current.append(c);
         }
      }

      appendLegacyPart(result, current, currentColor, bold);
      return result;
   }

   private static void appendLegacyPart(MutableComponent result, StringBuilder current, ChatFormatting color, boolean bold) {
      if (!current.isEmpty()) {
         MutableComponent part = Component.literal(current.toString());
         if (color != null) {
            part.withStyle(color);
         }

         if (bold) {
            part.withStyle(ChatFormatting.BOLD);
         }

         result.append(part);
      }
   }

   private static final class PotionData {
      private final ItemStack stack;
      private double lastX;
      private double lastY;
      private double lastZ;

      private PotionData(ItemStack stack, double x, double y, double z) {
         this.stack = stack;
         this.lastX = x;
         this.lastY = y;
         this.lastZ = z;
      }
   }
}
