package daamky.ua.utils.modules.autobuy;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;

public final class AuctionUtils {
   private static final Pattern PRICE_DOLLAR = Pattern.compile("\\$\\s*([\\d][\\d\\s,.]*)");
   private static final Pattern PRICE_LABELED = Pattern.compile("(?iu)(?:цена|price|стоимость|купить\\s+за)\\s*[:：]?\\s*\\$?\\s*([\\d][\\d\\s,.]*)");
   private static final Pattern PRICE_CURRENCY = Pattern.compile("([\\d][\\d\\s,.]*)\\s*[¤$]");
   private static final Pattern DIGITS = Pattern.compile("([\\d][\\d\\s,.]{1,})");

   private AuctionUtils() {
   }

   public static boolean isAuctionTitle(String title) {
      if (title != null && !title.isEmpty()) {
         String t = stripColors(title).toLowerCase(Locale.ROOT);
         return t.contains("аукцион")
            || t.contains("auction")
            || t.contains("поиск")
            || t.contains("search")
            || t.contains("ah ")
            || t.startsWith("ah")
            || t.contains("ah-")
            || t.contains("ah:")
            || t.contains("market")
            || t.contains("рынок")
            || t.contains("лот")
            || t.contains("listings");
      } else {
         return false;
      }
   }

   public static boolean isSearchTitle(String title) {
      if (title != null && !title.isEmpty()) {
         String t = stripColors(title).toLowerCase(Locale.ROOT);
         return t.contains("поиск") || t.contains("search") || t.contains("результат") || t.contains("result") || t.contains("найти") || t.contains("filter");
      } else {
         return false;
      }
   }

   public static boolean isConfirmTitle(String title) {
      if (title != null && !title.isEmpty()) {
         String t = stripColors(title).toLowerCase(Locale.ROOT);
         return t.contains("подозрительн") || t.contains("подтвер") || t.contains("suspicious") || t.contains("confirm") || t.contains("покупк");
      } else {
         return false;
      }
   }

   public static int getPrice(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         LoreComponent lore = (LoreComponent)stack.get(DataComponentTypes.LORE);
         if (lore != null) {
            for (Text line : lore.lines()) {
               String s = line.getString();
               if (s != null && !s.isEmpty() && (s.contains("$") || s.contains("¤") || s.indexOf(164) >= 0)) {
                  int p = parseDigitsOnly(s);
                  if (p > 0) {
                     return p;
                  }

                  String found = extractPriceString(s);
                  int v = parsePrice(found);
                  if (v > 0) {
                     return v;
                  }
               }
            }

            for (Text linex : lore.lines()) {
               String s = linex.getString();
               if (s != null && !s.isEmpty()) {
                  String lower = s.toLowerCase(Locale.ROOT);
                  if (lower.contains("цена") || lower.contains("price") || lower.contains("стоимость") || lower.contains("купить")) {
                     String found = extractPriceString(s);
                     int v = parsePrice(found);
                     if (v > 0) {
                        return v;
                     }

                     int px = parseDigitsOnly(s);
                     if (px > 0) {
                        return px;
                     }
                  }
               }
            }
         }

         String name = stack.getName().getString();
         if (name != null && (name.contains("$") || name.contains("¤"))) {
            int px = parseDigitsOnly(name);
            return px > 0 ? px : parsePrice(extractPriceString(name));
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static int parseDigitsOnly(String text) {
      if (text != null && !text.isEmpty()) {
         String digits = text.replaceAll("[^0-9]", "");
         if (!digits.isEmpty() && digits.length() <= 12) {
            try {
               long v = Long.parseLong(digits);
               if (v > 0L && v <= 2147483647L) {
                  return (int)v;
               } else {
                  return v > 2147483647L ? Integer.MAX_VALUE : -1;
               }
            } catch (NumberFormatException var4) {
               return -1;
            }
         } else {
            return -1;
         }
      } else {
         return -1;
      }
   }

   private static String extractPriceString(String text) {
      if (text != null && !text.isEmpty()) {
         Matcher m = PRICE_DOLLAR.matcher(text);
         if (m.find()) {
            return m.group(1);
         } else {
            m = PRICE_LABELED.matcher(text);
            if (m.find()) {
               return m.group(1);
            } else {
               m = PRICE_CURRENCY.matcher(text);
               if (m.find()) {
                  return m.group(1);
               } else {
                  if (text.contains("$") || text.contains("¤") || text.toLowerCase(Locale.ROOT).contains("цена")) {
                     m = DIGITS.matcher(text);
                     if (m.find()) {
                        String d = m.group(1).replaceAll("[\\s,.]", "");
                        if (d.length() >= 2) {
                           return m.group(1);
                        }
                     }
                  }

                  return null;
               }
            }
         }
      } else {
         return null;
      }
   }

   public static int parsePrice(String raw) {
      if (raw != null && !raw.isEmpty()) {
         try {
            String clean = raw.replaceAll("[\\s,.$¤]", "").trim();
            if (clean.isEmpty()) {
               return -1;
            } else {
               long v = Long.parseLong(clean);
               if (v > 2147483647L) {
                  return Integer.MAX_VALUE;
               } else {
                  return v <= 0L ? -1 : (int)v;
               }
            }
         } catch (NumberFormatException var4) {
            return -1;
         }
      } else {
         return -1;
      }
   }

   public static String cleanName(String raw) {
      if (raw == null) {
         return "";
      } else {
         String s = stripColors(raw)
            .replace(' ', ' ')
            .replaceAll("\\$[\\d\\s,.]+", "")
            .replaceAll("[★⚒❄\ud83c\udf79\\[\\]]", "")
            .trim()
            .toLowerCase(Locale.ROOT)
            .replaceAll("\\s+", " ");
         if (s.startsWith("+")) {
            s = s.substring(1).trim();
         }

         return s;
      }
   }

   public static String stripColors(String raw) {
      return raw == null ? "" : raw.replaceAll("§.", "").replaceAll("\\u00a7.", "");
   }

   public static int unitPrice(int lotPrice, int count) {
      if (lotPrice <= 0) {
         return -1;
      } else {
         int c = Math.max(1, count);
         return lotPrice / c;
      }
   }

   public static boolean isArmorItem(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return false;
      } else if (stack.isOf(Items.ELYTRA)) {
         return false;
      } else if (!stack.isIn(ItemTags.HEAD_ARMOR) && !stack.isIn(ItemTags.CHEST_ARMOR) && !stack.isIn(ItemTags.LEG_ARMOR) && !stack.isIn(ItemTags.FOOT_ARMOR)) {
         EquippableComponent equippable = (EquippableComponent)stack.get(DataComponentTypes.EQUIPPABLE);
         return equippable != null
            && stack.getItem() != Items.ELYTRA
            && (
               stack.isOf(Items.NETHERITE_HELMET)
                  || stack.isOf(Items.NETHERITE_CHESTPLATE)
                  || stack.isOf(Items.NETHERITE_LEGGINGS)
                  || stack.isOf(Items.NETHERITE_BOOTS)
                  || stack.isOf(Items.DIAMOND_HELMET)
                  || stack.isOf(Items.DIAMOND_CHESTPLATE)
                  || stack.isOf(Items.DIAMOND_LEGGINGS)
                  || stack.isOf(Items.DIAMOND_BOOTS)
                  || stack.isOf(Items.IRON_HELMET)
                  || stack.isOf(Items.IRON_CHESTPLATE)
                  || stack.isOf(Items.IRON_LEGGINGS)
                  || stack.isOf(Items.IRON_BOOTS)
                  || stack.isOf(Items.GOLDEN_HELMET)
                  || stack.isOf(Items.GOLDEN_CHESTPLATE)
                  || stack.isOf(Items.GOLDEN_LEGGINGS)
                  || stack.isOf(Items.GOLDEN_BOOTS)
                  || stack.isOf(Items.CHAINMAIL_HELMET)
                  || stack.isOf(Items.CHAINMAIL_CHESTPLATE)
                  || stack.isOf(Items.CHAINMAIL_LEGGINGS)
                  || stack.isOf(Items.CHAINMAIL_BOOTS)
                  || stack.isOf(Items.LEATHER_HELMET)
                  || stack.isOf(Items.LEATHER_CHESTPLATE)
                  || stack.isOf(Items.LEATHER_LEGGINGS)
                  || stack.isOf(Items.LEATHER_BOOTS)
                  || stack.isOf(Items.TURTLE_HELMET)
            );
      } else {
         return true;
      }
   }
}
