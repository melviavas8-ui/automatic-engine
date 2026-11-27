package daamky.ua.api.module.impl.combat.maxdamage;

import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemAttributeModifiers.Entry;
import daamky.ua.IMinecraft;

public final class MaxDamageAttribute implements IMinecraft {
   private MaxDamageAttribute() {
   }

   public static boolean isAresSphere(ItemStack stack) {
      return !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING)
         ? hasAttributeWithValue(stack, Attributes.ATTACK_DAMAGE, 6.0)
            && hasAttributeWithValue(stack, Attributes.MAX_HEALTH, -2.0)
            && hasAttributeWithValue(stack, Attributes.ARMOR, -2.0)
         : false;
   }

   public static boolean isKaratelTalisman(ItemStack stack) {
      if (!stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING)) {
         String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
         boolean sphere = name.contains("сфера") || name.contains("sphere");
         boolean talisman = name.contains("талисман") || name.contains("talisman");
         return !sphere && (talisman || stack.hasFoil());
      } else {
         return false;
      }
   }

   private static boolean hasAttributeWithValue(ItemStack stack, Holder<Attribute> attribute, double value) {
      ItemAttributeModifiers component = (ItemAttributeModifiers)stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
      if (component == null) {
         return false;
      } else {
         double total = 0.0;
         boolean found = false;

         for (Entry entry : component.modifiers()) {
            if (entry.attribute().value() == attribute.value()) {
               total += entry.modifier().amount();
               found = true;
            }
         }

         return found && Math.abs(total - value) <= 0.001;
      }
   }

   public static int findAresSlot() {
      return findSlot(MaxDamageAttribute::isAresSphere);
   }

   public static int findKaratelSlot() {
      return findSlot(MaxDamageAttribute::isKaratelTalisman);
   }

   public static int findTotemSlot() {
      return findSlot(stack -> !stack.isEmpty() && stack.is(Items.TOTEM_OF_UNDYING));
   }

   private static int findSlot(Predicate<ItemStack> predicate) {
      if (mc.player != null && mc.level != null) {
         for (int i = 0; i < 36; i++) {
            if (predicate.test(mc.player.getInventory().getItem(i))) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }
}
