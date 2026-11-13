package daamky.ua.api.module.impl.misc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.consume.UseAction;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Box;
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
   public void onTick(MinecraftClient client) {
      if (client.player != null && client.world != null) {
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
      if (event.isReceive() && mc.world != null && mc.player != null) {
         if (this.trackTotem.getValue() && event.getPacket() instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            if (packet.getEntity(mc.world) instanceof PlayerEntity player && !player.getUuid().equals(mc.player.getUuid())) {
               ItemStack totemStack = player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)
                  ? player.getOffHandStack()
                  : (player.getMainHandStack().isOf(Items.TOTEM_OF_UNDYING) ? player.getMainHandStack() : player.getOffHandStack());
               Text totemName = (Text)(totemStack.isEmpty() ? Text.literal("Тотем бессмертия") : totemStack.getName());
               boolean enchanted = player.getOffHandStack().hasEnchantments() || player.getMainHandStack().hasEnchantments();
               MutableText enchantPart = Text.literal(enchanted ? "зачарованный" : "незачарованный").formatted(enchanted ? Formatting.GREEN : Formatting.RED);
               this.sendPrefixed(
                  Text.empty()
                     .append(Text.literal(player.getName().getString()).formatted(Formatting.WHITE))
                     .append(Text.literal(" потерял ").formatted(Formatting.GRAY))
                     .append(totemName)
                     .append(Text.literal(" (").formatted(Formatting.GRAY))
                     .append(enchantPart)
                     .append(Text.literal(")").formatted(Formatting.GRAY))
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

   private void trackConsumedItems(MinecraftClient client) {
      for (PlayerEntity player : client.world.getPlayers()) {
         if (!player.getUuid().equals(client.player.getUuid())) {
            UUID id = player.getUuid();
            if (player.isUsingItem()) {
               this.activeUseItem.computeIfAbsent(id, ignored -> player.getActiveItem().copy());
               this.useStartTick.putIfAbsent(id, player.age);
            } else {
               ItemStack used = this.activeUseItem.remove(id);
               Integer startTick = this.useStartTick.remove(id);
               if (used != null && !used.isEmpty() && startTick != null && player.age - startTick >= 31) {
                  UseAction action = used.getUseAction();

                  String verb = switch (action) {
                     case DRINK -> "выпил";
                     case EAT -> "съел";
                     default -> null;
                  };
                  if (verb != null) {
                     String itemName = stripFormatting(used.getName().getString());
                     this.chat(Formatting.WHITE + player.getName().getString() + Formatting.GRAY + " " + verb + " " + Formatting.WHITE + itemName);
                  }
               }
            }
         }
      }
   }

   private void trackThrownPotions(MinecraftClient client) {
      Set<Integer> current = this.currentPotionIds;
      current.clear();
      float maxDistance = this.radius.getFloat();

      for (Entity entity : client.world.getEntities()) {
         if (entity instanceof PotionEntity) {
            PotionEntity potion = (PotionEntity)entity;
            if (!(client.player.distanceTo(potion) > maxDistance)) {
               int entityId = potion.getId();
               current.add(entityId);
               UseTracker.PotionData data = this.trackedPotions.get(entityId);
               if (data == null) {
                  this.trackedPotions.put(entityId, new UseTracker.PotionData(potion.getStack().copy(), potion.getX(), potion.getY(), potion.getZ()));
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

   private void notifyPotionHits(MinecraftClient client, UseTracker.PotionData data) {
      Box hitBox = new Box(data.lastX - 4.0, data.lastY - 2.0, data.lastZ - 4.0, data.lastX + 4.0, data.lastY + 2.0, data.lastZ + 4.0);

      for (LivingEntity hit : client.world.getEntitiesByClass(LivingEntity.class, hitBox, ignored -> true)) {
         if (hit instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity)hit;
            double dx = player.getX() - data.lastX;
            double dz = player.getZ() - data.lastZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (!(distance > 4.0)) {
               double hitChance = Math.max(0.0, 1.0 - distance / 4.0) * 100.0;
               Formatting hitColor = hitChance >= 65.0 ? Formatting.GREEN : (hitChance >= 35.0 ? Formatting.YELLOW : Formatting.RED);
               this.sendPrefixed(
                  Text.empty()
                     .append(Text.literal(player.getName().getString()).formatted(Formatting.WHITE))
                     .append(Text.literal(" получил ").formatted(Formatting.GRAY))
                     .append(data.stack.getName())
                     .append(Text.literal(" (").formatted(Formatting.GRAY))
                     .append(Text.literal(String.format("%.0f%%", hitChance)).formatted(hitColor))
                     .append(Text.literal(")").formatted(Formatting.GRAY))
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

   private void sendPrefixed(Text message) {
      if (mc.player != null) {
         MutableText text = Text.empty()
            .append(Text.literal("daamky").styled(style -> style.withBold(true).withColor(8385279)))
            .append(Text.literal(" » ").formatted(Formatting.DARK_GRAY))
            .append(message);
         mc.execute(() -> {
            if (mc.player != null) {
               mc.player.sendMessage(text, false);
            }
         });
      }
   }

   private static String stripFormatting(String text) {
      String stripped = Formatting.strip(text);
      return stripped == null ? text : stripped;
   }

   private static MutableText parseLegacy(String message) {
      MutableText result = Text.empty();
      Formatting currentColor = null;
      boolean bold = false;
      StringBuilder current = new StringBuilder();

      for (int i = 0; i < message.length(); i++) {
         char c = message.charAt(i);
         if (c == 167 && i + 1 < message.length()) {
            appendLegacyPart(result, current, currentColor, bold);
            current.setLength(0);
            Formatting formatting = Formatting.byCode(message.charAt(++i));
            if (formatting != null) {
               if (formatting == Formatting.BOLD) {
                  bold = true;
               } else if (formatting == Formatting.RESET) {
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

   private static void appendLegacyPart(MutableText result, StringBuilder current, Formatting color, boolean bold) {
      if (!current.isEmpty()) {
         MutableText part = Text.literal(current.toString());
         if (color != null) {
            part.formatted(color);
         }

         if (bold) {
            part.formatted(Formatting.BOLD);
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
