package daamky.ua.api.module.impl.combat.maxdamage;

import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import daamky.ua.IMinecraft;

public final class MaxDamageAttribute implements IMinecraft {
   private MaxDamageAttribute() {
   }

   public static boolean isAresSphere(ItemStack stack) {
      return !stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING)
         ? hasAttributeWithValue(stack, EntityAttributes.ATTACK_DAMAGE, 6.0)
            && hasAttributeWithValue(stack, EntityAttributes.MAX_HEALTH, -2.0)
            && hasAttributeWithValue(stack, EntityAttributes.ARMOR, -2.0)
         : false;
   }

   public static boolean isKaratelTalisman(ItemStack stack) {
      if (!stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING)) {
         String name = stack.getName().getString().toLowerCase(Locale.ROOT);
         boolean sphere = name.contains("сфера") || name.contains("sphere");
         boolean talisman = name.contains("талисман") || name.contains("talisman");
         return !sphere && (talisman || stack.hasGlint());
      } else {
         return false;
      }
   }

   private static boolean hasAttributeWithValue(ItemStack stack, RegistryEntry<EntityAttribute> attribute, double value) {
      AttributeModifiersComponent component = (AttributeModifiersComponent)stack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
      if (component == null) {
         return false;
      } else {
         double total = 0.0;
         boolean found = false;

         for (Entry entry : component.modifiers()) {
            if (entry.attribute().value() == attribute.value()) {
               total += entry.modifier().value();
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
      return findSlot(stack -> !stack.isEmpty() && stack.isOf(Items.TOTEM_OF_UNDYING));
   }

   private static int findSlot(Predicate<ItemStack> predicate) {
      if (mc.player != null && mc.world != null) {
         for (int i = 0; i < 36; i++) {
            if (predicate.test(mc.player.getInventory().getStack(i))) {
               return i;
            }
         }

         return -1;
      } else {
         return -1;
      }
   }
}
