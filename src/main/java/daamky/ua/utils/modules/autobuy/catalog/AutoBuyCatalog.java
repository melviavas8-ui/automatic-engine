package daamky.ua.utils.modules.autobuy.catalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import daamky.ua.utils.modules.autobuy.AutoBuyItem;

public final class AutoBuyCatalog {
   private static final List<AutoBuyItem> ITEMS = new ArrayList<>();
   private static final Set<String> SEEN_IDS = new HashSet<>();
   private static boolean built;

   private AutoBuyCatalog() {
   }

   public static List<AutoBuyItem> all() {
      ensureBuilt();
      return Collections.unmodifiableList(ITEMS);
   }

   public static List<AutoBuyItem> forFunTimeFamily() {
      ensureBuilt();
      List<AutoBuyItem> out = new ArrayList<>();

      for (AutoBuyItem item : ITEMS) {
         if (!item.getCategory().isHolyWorld()) {
            out.add(item);
         }
      }

      return out;
   }

   public static List<AutoBuyItem> forHolyWorld() {
      return byCategory(AutoBuyItemCategory.HOLYWORLD);
   }

   public static List<AutoBuyItem> forServer(String serverMode) {
      return serverMode != null && serverMode.equalsIgnoreCase("HolyWorld") ? forHolyWorld() : forFunTimeFamily();
   }

   public static List<AutoBuyItem> byCategory(AutoBuyItemCategory category) {
      ensureBuilt();
      List<AutoBuyItem> out = new ArrayList<>();

      for (AutoBuyItem item : ITEMS) {
         if (item.getCategory() == category) {
            out.add(item);
         }
      }

      return out;
   }

   public static List<AutoBuyItem> byCategoryForServer(AutoBuyItemCategory category, String serverMode) {
      if (category == null) {
         return forServer(serverMode);
      } else {
         return !category.matchesServer(serverMode) ? List.of() : byCategory(category);
      }
   }

   private static synchronized void ensureBuilt() {
      if (!built) {
         built = true;
         SEEN_IDS.clear();
         ITEMS.clear();
         buildKrush();
         buildSpheresNeverbuy();
         buildTalismansNeverbuy();
         buildPotions();
         buildMisc();
         buildHolyWorld();
      }
   }

   private static void buildKrush() {
      krush("Шлем Крушителя", 150000, Items.NETHERITE_HELMET);
      krush("Нагрудник Крушителя", 250000, Items.NETHERITE_CHESTPLATE);
      krush("Поножи Крушителя", 200000, Items.NETHERITE_LEGGINGS);
      krush("Ботинки Крушителя", 150000, Items.NETHERITE_BOOTS);
      krush("Меч Крушителя", 300000, Items.NETHERITE_SWORD);
      krush("Булава Крушителя", 280000, Items.MACE, "Булава");
      krush("Кирка Крушителя", 200000, Items.NETHERITE_PICKAXE);
   }

   private static void buildSpheresNeverbuy() {
      sphere("Сфера Афины", 80000);
      sphere("Сфера Хаоса", 80000);
      sphere("Сфера Сатира", 80000);
      sphere("Сфера Бестии", 80000);
      sphere("Сфера Ареса", 80000);
      sphere("Сфера Гидры", 80000);
      sphere("Сфера Икара", 80000);
      sphere("Сфера Титана", 80000);
      sphere("Сфера Эрида", 80000);
   }

   private static void buildTalismansNeverbuy() {
      talisman("Талисман Крушителя", 150000);
      talisman("Талисман Раздора", 100000);
      talisman("Талисман Тирана", 100000);
      talisman("Талисман Ярости", 100000);
      talisman("Талисман Вихря", 100000);
      talisman("Талисман Мрака", 100000);
      talisman("Талисман Демона", 100000);
      talisman("Талисман Карателя", 100000);
   }

   private static void buildPotions() {
      potion("Зелье Ассасина", 25000, 3355443);
      potion("Зелье Гнева", 25000, 10040115);
      potion("Хлопушка", 20000, 16738740);
      potion("Святая Вода", 20000, 16777215, "Святая вода");
      potion("Зелье Палладина", 30000, 65535);
      potion("Зелье Радиации", 25000, 3329330);
      potion("Снотворное", 20000, 4737096);
      potion("Мандариновый сок", 15000, 14077507);
      potion("Зелье Агента", 20000, 5635925);
      potion("Зелье Киллера", 25000, 11141120);
      potion("Зелье Медика", 20000, 5635925);
      potion("Зелье Победителя", 25000, 16755200, "Зелье победителя");
   }

   private static void buildMisc() {
      item("Отмычка к сферам", AutoBuyItemCategory.MISC, 50000, Items.TRIPWIRE_HOOK, "Отмычка к Сферам", "Отмычка");
      item("Чарка", AutoBuyItemCategory.MISC, 25000, Items.ENCHANTED_GOLDEN_APPLE, "Зачарованное золотое яблоко");
      item("Маяк", AutoBuyItemCategory.MISC, 80000, Items.BEACON, "Загадочный маяк");
      item("Модификатор полёта", AutoBuyItemCategory.MISC, 60000, Items.FEATHER, "Модификатор полета");
      item("Незеритовый слиток", AutoBuyItemCategory.MISC, 3000, Items.NETHERITE_INGOT);
      item("Шалкер", AutoBuyItemCategory.MISC, 20000, Items.SHULKER_BOX, "Шалкеровый ящик");
      item("Явная Пыль", AutoBuyItemCategory.MISC, 15000, Items.SUGAR, "Явная пыль");
      item("Дезориентация", AutoBuyItemCategory.MISC, 15000, Items.ENDER_EYE);
      item("Трапка", AutoBuyItemCategory.MISC, 40000, Items.NETHERITE_SCRAP);
      item("Пласт", AutoBuyItemCategory.MISC, 30000, Items.DRIED_KELP);
      item("Опыт 15", AutoBuyItemCategory.MISC, 5000, Items.EXPERIENCE_BOTTLE, "Пузырек опыта [15 ур]", "Пузырёк опыта [15 ур]");
      item("Опыт 30", AutoBuyItemCategory.MISC, 10000, Items.EXPERIENCE_BOTTLE, "Пузырек опыта [30 ур]", "Пузырёк опыта [30 Ур.]");
      item("Опыт 50", AutoBuyItemCategory.MISC, 20000, Items.EXPERIENCE_BOTTLE, "Пузырек опыта [50 ур]");
      item("Вайт", AutoBuyItemCategory.MISC, 25000, Items.TNT, "TNT - TIER WHITE", "TNT WHITE");
      item("Блек", AutoBuyItemCategory.MISC, 40000, Items.TNT, "TNT - TIER BLACK", "TNT BLACK");
      item("Тотем бессмертия", AutoBuyItemCategory.MISC, 5000, Items.TOTEM_OF_UNDYING);
      item("Элитры", AutoBuyItemCategory.MISC, 50000, Items.ELYTRA);
      item("Эндер жемчуг", AutoBuyItemCategory.MISC, 500, Items.ENDER_PEARL, "Эндер-жемчуг");
      item("Золотое яблоко", AutoBuyItemCategory.MISC, 1000, Items.GOLDEN_APPLE);
      item("Спавнер", AutoBuyItemCategory.MISC, 100000, Items.SPAWNER);
      item("Незеритовый блок", AutoBuyItemCategory.MISC, 25000, Items.NETHERITE_BLOCK);
      item("Заряд ветра", AutoBuyItemCategory.MISC, 5000, Items.WIND_CHARGE);
   }

   private static void buildHolyWorld() {
      hw("Шлем Infinity", 200000, Items.NETHERITE_HELMET);
      hw("Нагрудник Infinity", 300000, Items.NETHERITE_CHESTPLATE);
      hw("Поножи Infinity", 250000, Items.NETHERITE_LEGGINGS);
      hw("Ботинки Infinity", 200000, Items.NETHERITE_BOOTS);
      hw("Шлем Eternity", 120000, Items.NETHERITE_HELMET);
      hw("Нагрудник Eternity", 180000, Items.NETHERITE_CHESTPLATE);
      hw("Штаны Eternity", 150000, Items.NETHERITE_LEGGINGS, "Поножи Eternity");
      hw("Ботинки Eternity", 120000, Items.NETHERITE_BOOTS);
      hw("Шлем солнца", 100000, Items.GOLDEN_HELMET);
      hw("Броневая элитра", 250000, Items.ELYTRA);
      hw("Меч Eternity", 200000, Items.NETHERITE_SWORD);
      hw("Кирка Eternity", 150000, Items.NETHERITE_PICKAXE);
      hw("Арбалет Eternity", 100000, Items.CROSSBOW);
      hw("Громовержец", 150000, Items.TRIDENT);
      hwSphere("Сфера Цербера", 100000, "Cerber");
      hwSphere("Сфера Флеша", 100000, "Flash");
      hwSphere("Сфера Имморталити", 100000, "Сфера ɪᴍᴍᴏʀᴛᴀʟɪᴛʏ", "Immortal");
      hwSphere("Сфера Арморталити", 100000, "Сфера ᴀʀᴍᴏʀᴛᴀʟɪᴛʏ", "Armortality");
      hwSphere("Сфера на Скорость III", 80000, "Сфера на скорость 3", "Speed3");
      hwSphere("Сфера Eternity", 100000, "Eternity");
      hwSphere("Сфера Stinger", 100000, "Stinger");
      hwSphere("Сфера на броня III скорость II", 90000, "Сфера на броня 3", "Mythical3");
      hwSphere("Сфера на урон II броня III", 90000);
      hwSphere("Сфера на броня II урон III", 90000);
      hw("Талисман Stinger", 120000, Items.TOTEM_OF_UNDYING);
      hw("Талисман Infinity", 150000, Items.TOTEM_OF_UNDYING);
      hw("Талисман Eternity", 130000, Items.TOTEM_OF_UNDYING);
      hw("Легендарный талисман", 100000, Items.TOTEM_OF_UNDYING);
      hw("Пузырек с 15 уровнем", 5000, Items.EXPERIENCE_BOTTLE, "15");
      hw("Пузырек с 50 уровнем", 25000, Items.EXPERIENCE_BOTTLE, "50");
      hw("Пузырек с 100 уровнем", 80000, Items.EXPERIENCE_BOTTLE, "100");
      hw("Обычный пузырек опыта", 1000, Items.EXPERIENCE_BOTTLE, "опыт");
      hw("Рюкзак I уровень", 30000, Items.PINK_SHULKER_BOX, "рюкзак 1 уровень");
      hw("Рюкзак II уровень", 50000, Items.LIGHT_BLUE_SHULKER_BOX, "рюкзак 2 уровень");
      hw("Рюкзак III уровень", 80000, Items.RED_SHULKER_BOX, "рюкзак 3 уровень");
      hw("Рюкзак IV уровень", 120000, Items.MAGENTA_SHULKER_BOX, "рюкзак 4 уровень");
      hw("Рюкзак Infinity", 200000, Items.LIME_SHULKER_BOX, "рюкзак infinity");
      hw("Взрывная трапка", 40000, Items.PRISMARINE_SHARD);
      hw("Стан", 35000, Items.NETHER_STAR);
      hw("Взрывная штучка", 20000, Items.FIRE_CHARGE);
      hw("Ком снега", 15000, Items.SNOWBALL);
      hw("Руна «Бессмертие»", 50000, Items.ORANGE_DYE, "Бессмертие", "Руна Бессмертие");
      hwPotion("Улучшенное зелье силы", 20000, 16733440);
      hwPotion("Улучшенное зелье скорости", 20000, 3386111);
      hwPotion("Зелье исцеления", 15000, 16711680, "Зелье исцеление");
      hwPotion("Зелье черепашьей мощи", 18000, 8369336);
      hwPotion("Зелье черепашьей мощи II", 25000, 8369336);
      hw("Охотник", 40000, Items.NETHERITE_SWORD);
      hw("Снеговик", 30000, Items.SNOW_BLOCK);
      hw("Иллюминатор", 30000, Items.SEA_LANTERN);
      hw("Эндермен", 35000, Items.ENDER_PEARL);
      hw("Анти Фантом", 25000, Items.PHANTOM_MEMBRANE);
      hw("Телекинез", 30000, Items.HONEY_BLOCK);
      hw("Гравитация", 30000, Items.FEATHER);
      hw("Вампиризм", 40000, Items.WITHER_SKELETON_SKULL);
      hw("Справедливость", 35000, Items.POTION);
      hw("Универсальный ключ", 50000, Items.TRIPWIRE_HOOK);
      hw("Фармер", 40000, Items.DIAMOND_SWORD);
      hw("Золотая морковь", 500, Items.GOLDEN_CARROT);
      hw("Плод хоруса", 300, Items.CHORUS_FRUIT);
      hw("Артефакт", 80000, Items.CONDUIT);
      hw("Фейерверк", 200, Items.FIREWORK_ROCKET);
      hw("Порох", 100, Items.GUNPOWDER);
      hw("Боевой фрагмент", 15000, Items.PRISMARINE_CRYSTALS);
      hw("Взрывчатое вещество", 20000, Items.CLAY);
      hw("Динамит А", 25000, Items.TNT, "Динамит A");
      hw("Динамит B", 30000, Items.TNT, "динамит б");
      hw("Динамит B2", 35000, Items.TNT, "динамит б2");
      hw("C4 ВзРыВчАтКа", 50000, Items.TNT, "с4 взрывчатка", "C4");
      hw("Золотая кирка Джейка", 60000, Items.GOLDEN_PICKAXE);
      hw("Осколок сферы", 20000, Items.PLAYER_HEAD);
   }

   private static void krush(String name, int price, Item icon, String... aliases) {
      item(name, AutoBuyItemCategory.KRUSH, price, namedIcon(icon, name), aliases);
   }

   private static void sphere(String name, int price) {
      item(name, AutoBuyItemCategory.SPHERES, price, Items.PLAYER_HEAD, "[★] " + name);
   }

   private static void talisman(String name, int price, String... aliases) {
      item(name, AutoBuyItemCategory.TALISMANS, price, Items.TOTEM_OF_UNDYING, aliases);
   }

   private static void hw(String name, int price, Item icon, String... aliases) {
      item(name, AutoBuyItemCategory.HOLYWORLD, price, icon, aliases);
   }

   private static void hwSphere(String name, int price, String... aliases) {
      item(name, AutoBuyItemCategory.HOLYWORLD, price, Items.PLAYER_HEAD, aliases);
   }

   private static void hwPotion(String name, int price, int color, String... aliases) {
      Supplier<ItemStack> factory = () -> {
         ItemStack stack = new ItemStack(Items.POTION);
         stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.AQUA));
         stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
         return stack;
      };
      addUnique(new AutoBuyItem(name, AutoBuyItemCategory.HOLYWORLD, price, factory, aliases));
   }

   private static void potion(String name, int price, int color, String... aliases) {
      Supplier<ItemStack> factory = () -> {
         ItemStack stack = new ItemStack(Items.SPLASH_POTION);
         stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(Formatting.AQUA));
         stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
         stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(name))));
         return stack;
      };
      addUnique(new AutoBuyItem(name, AutoBuyItemCategory.POTIONS, price, factory, aliases));
   }

   private static void item(String name, AutoBuyItemCategory cat, int price, Item icon, String... aliases) {
      addUnique(new AutoBuyItem(name, cat, price, icon, aliases));
   }

   private static void item(String name, AutoBuyItemCategory cat, int price, Supplier<ItemStack> icon, String... aliases) {
      addUnique(new AutoBuyItem(name, cat, price, icon, aliases));
   }

   private static void addUnique(AutoBuyItem item) {
      if (item != null && !item.getId().isEmpty()) {
         if (SEEN_IDS.add(item.getId())) {
            ITEMS.add(item);
         }
      }
   }

   private static Supplier<ItemStack> namedIcon(Item item, String name) {
      return () -> {
         ItemStack stack = new ItemStack(item);
         stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).formatted(new Formatting[]{Formatting.BOLD, Formatting.DARK_RED}));
         stack.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal("[★] Оригинальный предмет").formatted(Formatting.GRAY))));
         return stack;
      };
   }
}
