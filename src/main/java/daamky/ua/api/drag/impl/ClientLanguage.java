package daamky.ua.api.drag.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import daamky.ua.api.config.ConfigManager;

public final class ClientLanguage {
   private static final Gson GSON = new Gson();
   private static final String CONFIG_FILE = "language.daamky";
   private static ClientLanguage.Lang current = ClientLanguage.Lang.EN;
   private static final Map<String, String> RU = entries(
      Map.entry("Combat", "Бой"),
      Map.entry("Movement", "Движение"),
      Map.entry("Visual", "Визуалы"),
      Map.entry("Player", "Игрок"),
      Map.entry("Display", "Интерфейс"),
      Map.entry("World", "Мир"),
      Map.entry("Misc", "Разное"),
      Map.entry("Aura", "Аура"),
      Map.entry("TriggerBot", "Триггер-бот"),
      Map.entry("Auto Totem", "Авто тотем"),
      Map.entry("Auto GApple", "Авто яблоко"),
      Map.entry("Auto Crystal", "Авто кристалл"),
      Map.entry("Aim Assist", "Помощь в прицеливании"),
      Map.entry("Anti Bot", "Анти бот"),
      Map.entry("Criticals", "Криты"),
      Map.entry("Hit Box", "Хитбокс"),
      Map.entry("Hit Sound", "Кастомный звук"),
      Map.entry("No Friend Damage", "Без урона друзьям"),
      Map.entry("No Interact", "Без взаимодействия"),
      Map.entry("Mace Target", "Цель булавы"),
      Map.entry("Air Stuck", "Зависание"),
      Map.entry("Auto Sprint", "Авто спринт"),
      Map.entry("Elytra Target", "Элитра таргет"),
      Map.entry("Fly", "Полет"),
      Map.entry("Inventory Move", "Ходьба в инвентаре"),
      Map.entry("No Slow", "Без замедления"),
      Map.entry("No Web", "Без паутины"),
      Map.entry("Scaffold", "Скаффолд"),
      Map.entry("Spider", "Паук"),
      Map.entry("Strafe", "Стрейф"),
      Map.entry("AirStuck", "Заморозка в воздухе"),
      Map.entry("Super FireWork", "Супер фейерверк"),
      Map.entry("Target Strafe", "Стрейф вокруг цели"),
      Map.entry("Hud", "Худ"),
      Map.entry("Arrows", "Стрелки"),
      Map.entry("Aspect Ratio", "Соотношение сторон"),
      Map.entry("Block ESP", "Подсветка блоков"),
      Map.entry("Custom Hotbar", "Кастомный хотбар"),
      Map.entry("Dash Trail", "След рывка"),
      Map.entry("ESP", "ЕСП"),
      Map.entry("Fog Blur", "Размытие тумана"),
      Map.entry("Gui Hands", "Руки в GUI"),
      Map.entry("Kill Effect", "Эффект убийства"),
      Map.entry("Predictions", "Предикт"),
      Map.entry("Target ESP", "Подсветка цели"),
      Map.entry(" Color 1", "Цвет 1"),
      Map.entry("Color 2", "Цвет 2"),
      Map.entry("Auto Tool", "Авто инструмент"),
      Map.entry("Chest Stealer", "Кража из сундуков"),
      Map.entry("Click Pearl", "Клик перл"),
      Map.entry("Fake Ping", "Фейк пинг"),
      Map.entry("Fast Break", "Быстрое ломание"),
      Map.entry("Free Cam", "Свободная камера"),
      Map.entry("Free Look", "Свободный обзор"),
      Map.entry("Item Scroller", "Скролл предметов"),
      Map.entry("Name Protect", "Защита ника"),
      Map.entry("No Delay", "Без задержки"),
      Map.entry("No Entity Trace", "Без трассы сущностей"),
      Map.entry("No Push", "Без толчков"),
      Map.entry("Open Walls", "Открытые стены"),
      Map.entry("Sp Connect", "SP подключение"),
      Map.entry("Wind Jump", "Прыжок ветра"),
      Map.entry("Auto Accept", "Авто принятие"),
      Map.entry("Velocity", "Без отталкивания"),
      Map.entry("Auto Auth", "Авто авторизация"),
      Map.entry("Auto Duel", "Авто дуэль"),
      Map.entry("AutoEvent", "Авто ивент"),
      Map.entry("Auto Leave", "Авто выход"),
      Map.entry("Auto Les", "Авто лес"),
      Map.entry("Client Sounds", "Звуки клиента"),
      Map.entry("Death Coords", "Координаты смерти"),
      Map.entry("Elytra Helper", "Помощник элитр"),
      Map.entry("Joiner Helper", "Помощник входа"),
      Map.entry("Server Helper", "Помощник сервера"),
      Map.entry("Server RP Spoofer", "Спуфер ресурспака"),
      Map.entry("Use Tracker", "Трекер использований"),
      Map.entry("Bind", "Бинд"),
      Map.entry("Mode", "Режим"),
      Map.entry("Range", "Дистанция"),
      Map.entry("Distance", "Дистанция"),
      Map.entry("Delay", "Задержка"),
      Map.entry("Color", "Цвет"),
      Map.entry("Only Crits", "Только криты"),
      Map.entry("Return Item", "Вернуть предмет"),
      Map.entry("Server", "Сервер"),
      Map.entry("Auto Place", "Автоматическая ставка"),
      Map.entry("Near Boost", "Рядом с игроком"),
      Map.entry("Time", "Время"),
      Map.entry("Saturation", "Насыщенность"),
      Map.entry("Fog Density", "Плотность тумана"),
      Map.entry("Brightness", "Яркость"),
      Map.entry("Wave Speed", "Скорость волны"),
      Map.entry("Wave Scale", "Волновая шкала"),
      Map.entry("Wave Strength", "Сила волны"),
      Map.entry("Glow Intensity", "Интенсивность свечения"),
      Map.entry("Custom Fog", "Кастомный туман"),
      Map.entry("Fog Shader", "Шейдер тумана"),
      Map.entry("Color Mode", "Мод цвета"),
      Map.entry("Max Distance", "Максимальная дистанция"),
      Map.entry("Dash Length", "Длина следа"),
      Map.entry("Self", "Отображать на себе"),
      Map.entry("Motion Smoothing", "Сглаживание движения"),
      Map.entry("Dash Segments", "Сегменты линии"),
      Map.entry("Lighting", "Свечение"),
      Map.entry("Dash Dots", "Точки линии"),
      Map.entry("Color Fog", "Цвет тумана"),
      Map.entry("Players", "Игроки"),
      Map.entry("Mobs", "Мобы"),
      Map.entry("Friends", "Друзья"),
      Map.entry("None", "Нет"),
      Map.entry("selected", "выбрано"),
      Map.entry("Search", "Поиск"),
      Map.entry("Search...", "Поиск..."),
      Map.entry("Modules", "Модули"),
      Map.entry("No modules found", "Модули не найдены"),
      Map.entry("setting", "настройка"),
      Map.entry("Click", "Клик"),
      Map.entry("Opening", "Открытие"),
      Map.entry("Closing", "Закрытие"),
      Map.entry("Hot Keys", "Бинды"),
      Map.entry("Example", "Пример"),
      Map.entry("Volume", "Громкость"),
      Map.entry("Predict", "Предикт"),
      Map.entry("Auto Chestplate", "Автоматический нагрудник"),
      Map.entry("Return Slot", "Вернуть слот"),
      Map.entry("Return Items", "Вернуть предмет"),
      Map.entry("Smart Crits", "Умные криты"),
      Map.entry("Sync TPS", "Синхронизация ТПС"),
      Map.entry("Height", "Высота"),
      Map.entry("Golden Hearts", "Золотые сердца"),
      Map.entry("AimAssist Speed", "Скорость АимАссиста"),
      Map.entry("AimAssist Smoothness", "Плавность АимАссиста"),
      Map.entry("Place Delay", "Скорость ставки"),
      Map.entry("Attack Range", "Дистанция аттаки"),
      Map.entry("Attack Delay", "Скорость аттаки"),
      Map.entry("Priority", "Приоритет"),
      Map.entry("Look Range", "Дополнительная дистанция"),
      Map.entry("Crystal Height", "Высота кристала"),
      Map.entry("Elytra Health", "Здоровье на элитрах"),
      Map.entry("Crystal Distance", "Дистанция кристала"),
      Map.entry("Horizontal", "Горизонталь"),
      Map.entry("Vertical", "Вертикаль"),
      Map.entry("Down Speed", "Скорость "),
      Map.entry("Return Speed", "Скорость возврата"),
      Map.entry("Speed", "Скорость"),
      Map.entry("Auto Jump", "Авто прыжок"),
      Map.entry("Radius", "Радиус"),
      Map.entry("Physics", "Физика"),
      Map.entry("Blur", "Размытие"),
      Map.entry("Glow", "Свечение"),
      Map.entry("Elements", "Элементы"),
      Map.entry("Wobble", "Колебание"),
      Map.entry("Rise Speed", "Скорость подъема"),
      Map.entry("", ""),
      Map.entry("Look", "Дополнительная"),
      Map.entry("Only", "Только"),
      Map.entry("Crits", "Криты"),
      Map.entry("Smart", "Умные"),
      Map.entry("Golden", "Золотое"),
      Map.entry("Hearts", "Сердце"),
      Map.entry("Return", "Вернуть"),
      Map.entry("Slot", "Слот"),
      Map.entry("FOV", "Фов"),
      Map.entry("Expand XZ", "Ускорение по XZ"),
      Map.entry("Expand Y", "Ускорение по Y"),
      Map.entry("Sound", "Звук"),
      Map.entry("Expand XZ", "Ускорение по XZ"),
      Map.entry("Aim Height", "Высота прицела"),
      Map.entry("Draw FOV", "Рисовать Фов"),
      Map.entry("Fov", "Фов"),
      Map.entry("Ratio", "Соотношение"),
      Map.entry("Wave Intensity", "Интенсивность волны"),
      Map.entry("Blur Radius", "Радиус размытия"),
      Map.entry("Blur Quality", "Качество размытия"),
      Map.entry("Tint Intensity", "Сила тонировки"),
      Map.entry("Legacy Sound", "Звук эффекта"),
      Map.entry("Legacy Volume", "Громкость эффекта"),
      Map.entry("Skin", "Скин"),
      Map.entry("Dachshund", "Такса"),
      Map.entry("Jack Russell", "Джек-рассел"),
      Map.entry("Grief Number", "Номер грифа"),
      Map.entry("Auto Takeoff", "Авто взлет"),
      Map.entry("Firework Bind", "Бинд фейерверка"),
      Map.entry("Delay ms", "Задержка, мс"),
      Map.entry("Only Weapon", "Только с оружием"),
      Map.entry("Only Players", "Только игроки"),
      Map.entry("No Inventory Aim", "Не целиться в инвентаре"),
      Map.entry("Invisible", "Невидимые"),
      Map.entry("Only Armored", "Только в броне"),
      Map.entry("Ignore Naked", "Не бить голых"),
      Map.entry("Only Yaw", "Только по X"),
      Map.entry("Multipoint", "Мультипоинт"),
      Map.entry("Wall Check", "Проверка стен"),
      Map.entry("Disable On World", "Выкл. при смене мира"),
      Map.entry("Micro Movements", "Микродвижения"),
      Map.entry("AutoSwap", "Авто свап"),
      Map.entry("Auto Swap", "Авто свап"),
      Map.entry("First Item", "Первый предмет"),
      Map.entry("Second Item", "Второй предмет"),
      Map.entry("Third Item", "Третий предмет"),
      Map.entry("Swap Bind", "Бинд свапа"),
      Map.entry("Auto Damage", "Автоурон"),
      Map.entry("Double", "Двойной"),
      Map.entry("Totem", "Тотем"),
      Map.entry("Appearance", "Внешний вид"),
      Map.entry("Ambience", "Атмосфера"),
      Map.entry("Jump Circle", "Круг прыжка"),
      Map.entry("RichDog", "Питомец"),
      Map.entry("Richi Dog", "Питомец"),
      Map.entry("SpookyJoiner", "Спуки джойнер"),
      Map.entry("Warden Helper", "Помощник Вардена"),
      Map.entry("WardenHelper", "Помощник Вардена"),
      Map.entry("ShiftTap", "Шифт-тап"),
      Map.entry("Glass Hands", "Стеклянные руки"),
      Map.entry("Item Physics", "Физика предметов"),
      Map.entry("Particles", "Частицы"),
      Map.entry("See Invisible", "Видеть невидимых"),
      Map.entry("View Model", "Модель рук"),
      Map.entry("Block Overlay", "Оверлей блока"),
      Map.entry("AH Helper", "Помощник аукциона"),
      Map.entry("Auto Respawn", "Авто возрождение"),
      Map.entry("Auto Tp Accept", "Авто принятие TP"),
      Map.entry("Theme", "Тема"),
      Map.entry("Weather", "Погода"),
      Map.entry("Sunny", "Ясно"),
      Map.entry("Rain", "Дождь"),
      Map.entry("Thunder", "Гроза"),
      Map.entry("Snow", "Снег"),
      Map.entry("Day", "День"),
      Map.entry("Midday", "Полдень"),
      Map.entry("Night", "Ночь"),
      Map.entry("Midnight", "Полночь"),
      Map.entry("Custom", "Свой"),
      Map.entry("Client", "Клиент"),
      Map.entry("Rainbow", "Радуга"),
      Map.entry("Default", "Обычный"),
      Map.entry("Health", "Здоровье"),
      Map.entry("Size", "Размер"),
      Map.entry("Duration", "Длительность"),
      Map.entry("Length", "Длина"),
      Map.entry("Alpha", "Прозрачность"),
      Map.entry("Notify", "Уведомлять"),
      Map.entry("Safe Walk", "Безопасный шаг"),
      Map.entry("Tower", "Башня"),
      Map.entry("Extend", "Дальше"),
      Map.entry("Downward", "Вниз"),
      Map.entry("Triggers", "Триггеры"),
      Map.entry("Only Friends", "Только друзья"),
      Map.entry("Password", "Пароль"),
      Map.entry("Money", "Деньги"),
      Map.entry("Amount", "Сумма"),
      Map.entry("Name", "Ник"),
      Map.entry("Restart", "Перезапуск"),
      Map.entry("Textures", "Текстуры"),
      Map.entry("Events", "События"),
      Map.entry("Tint", "Тонировка"),
      Map.entry("Tint Intensity", "Сила тонировки"),
      Map.entry("Tint Color", "Цвет тонировки"),
      Map.entry("Edge Glow", "Свечение краев"),
      Map.entry("Edge Glow Intensity", "Сила свечения краев"),
      Map.entry("Flame", "Пламя"),
      Map.entry("Walk Point", "Точка ходьбы"),
      Map.entry("Bypass Point", "Точка обхода"),
      Map.entry("Walk Radius", "Радиус ходьбы"),
      Map.entry("Options", "Опции"),
      Map.entry("Only While Pressing", "Только при нажатии"),
      Map.entry("Front", "Спереди"),
      Map.entry("Direction Mode", "Режим направления"),
      Map.entry("Direction", "Направление"),
      Map.entry("Cube", "Куб"),
      Map.entry("Center", "Центр"),
      Map.entry("Circle", "Круг"),
      Map.entry("Clockwise", "По часовой"),
      Map.entry("Counter Clockwise", "Против часовой"),
      Map.entry("Random", "Случайно"),
      Map.entry("Icon HudColor", "Цвет HUD-иконок"),
      Map.entry("TargetHud Particle Count", "Частиц TargetHud"),
      Map.entry("TargetHud Hit Particles", "Частицы удара TargetHud"),
      Map.entry("Line Width", "Толщина линии"),
      Map.entry("Particle Texture", "Текстура частиц"),
      Map.entry("Particle Count", "Кол-во частиц"),
      Map.entry("Particle Size", "Размер частиц"),
      Map.entry("Particle Speed", "Скорость частиц"),
      Map.entry("Particle Lifetime", "Время жизни частиц"),
      Map.entry("Random Particle Color", "Случайный цвет частиц"),
      Map.entry("White Center", "Белый центр"),
      Map.entry("Particle Spin", "Вращение частиц"),
      Map.entry("Line Color", "Цвет линии"),
      Map.entry("Particle Color", "Цвет частиц"),
      Map.entry("Whitelist", "Белый список"),
      Map.entry("Freeze", "Заморозка"),
      Map.entry("Multiplier", "Множитель"),
      Map.entry("Ping Delay", "Задержка пинга"),
      Map.entry("Scroll Delay", "Задержка скролла"),
      Map.entry("Free Look", "Свободный обзор"),
      Map.entry("Disable With Sword", "Отключать с мечом"),
      Map.entry("Ignore", "Игнор"),
      Map.entry("Warden An", "Анархия Warden"),
      Map.entry("Wardennhouse", "Дом Warden"),
      Map.entry("Go invisible", "Уйти в инвиз"),
      Map.entry("Use speed potion", "Использовать зелье скорости"),
      Map.entry("An WareHouse", "Анархия склада")
   );
   private static final Map<String, String> DESCRIPTION_RU = Map.ofEntries(
      Map.entry("Module key bind.", "Клавиша привязки модуля."),
      Map.entry("Target strafe mode.", "Режим обхода цели."),
      Map.entry("Target strafe walk point.", "Точка движения вокруг цели."),
      Map.entry("Target strafe bypass point.", "Точка обхода для байпаса."),
      Map.entry("Target strafe walk radius.", "Радиус движения вокруг цели."),
      Map.entry("Matrix strafe radius.", "Радиус стрейфа Matrix."),
      Map.entry("Matrix strafe speed.", "Скорость стрейфа Matrix."),
      Map.entry("Target strafe options.", "Опции стрейфа вокруг цели."),
      Map.entry("Strafe direction.", "Направление стрейфа."),
      Map.entry("Firework boost mode.", "Режим ускорения фейерверком."),
      Map.entry("Custom firework speed.", "Своя скорость фейерверка."),
      Map.entry("Boosts when a player is close.", "Ускоряет, когда игрок рядом."),
      Map.entry("Fov", "Фов"),
      Map.entry("", ""),
      Map.entry("Strafe mode.", "Режим стрейфа."),
      Map.entry("Strafe speed.", "Скорость стрейфа."),
      Map.entry("Target server.", "Целевой сервер."),
      Map.entry("ReallyWorld grief number.", "Номер грифа ReallyWorld."),
      Map.entry("Delay between actions in seconds.", "Задержка между действиями в секундах."),
      Map.entry("Spider mode.", "Режим паука."),
      Map.entry("Knockback reduce mode.", "Режим уменьшения отбрасывания."),
      Map.entry("Wind charge key.", "Клавиша ветряного заряда."),
      Map.entry("Speed mode.", "Режим скорости."),
      Map.entry("Vanilla speed.", "Скорость Vanilla."),
      Map.entry("Swap elytra/chestplate bind.", "Клавиша свапа элитры и нагрудника."),
      Map.entry("Use firework bind.", "Клавиша использования фейерверка."),
      Map.entry("Automatically toggles jump for elytra takeoff.", "Автоматически включает прыжок для взлёта на элитрах."),
      Map.entry("Automatically uses firework after takeoff.", "Автоматически использует фейерверк после взлёта."),
      Map.entry("Attack distance to the target.", "Дистанция атаки до цели."),
      Map.entry("Minimum delay between attacks.", "Минимальная задержка между атаками."),
      Map.entry("Target type filter.", "Фильтр типов целей."),
      Map.entry("Attack options.", "Опции атаки."),
      Map.entry("Chance to attack the target.", "Шанс атаковать цель."),
      Map.entry("Allow smart ground hits while waiting for critical timing.", "Разрешает умные удары с земли при ожидании тайминга крита."),
      Map.entry("Adjust attack timing to current server TPS.", "Подстраивает тайминг атаки под текущий TPS сервера."),
      Map.entry("Sprint reset mode before attack.", "Режим сброса спринта перед атакой."),
      Map.entry("How many blocks to place forward.", "Сколько блоков ставить вперёд."),
      Map.entry("Jump tower.", "Прыжковая башня."),
      Map.entry("Automatically select blocks.", "Автоматически выбирает блоки."),
      Map.entry("Hold sneak at edges.", "Зажимает шифт у краёв."),
      Map.entry("Place lower while sneaking.", "Ставит блоки ниже при шифте."),
      Map.entry("Cobweb bypass mode.", "Режим обхода паутины."),
      Map.entry("Target highlight type.", "Тип подсветки цели."),
      Map.entry("Primary target ESP color.", "Основной цвет Target ESP."),
      Map.entry("Secondary target ESP color.", "Вторичный цвет Target ESP."),
      Map.entry("Client sound type.", "Тип звука клиента."),
      Map.entry("Client sound volume.", "Громкость звуков клиента."),
      Map.entry("Client sound pitch.", "Тон звуков клиента."),
      Map.entry("Search typing sound.", "Звук набора в поиске."),
      Map.entry("Slider move sound.", "Звук движения слайдера."),
      Map.entry("Category switch sound.", "Звук смены категории."),
      Map.entry("No settings sound.", "Звук отсутствия настроек."),
      Map.entry("Button click sound.", "Звук нажатия кнопки."),
      Map.entry("ClickGUI opening sound.", "Звук открытия ClickGUI."),
      Map.entry("ClickGUI closing sound.", "Звук закрытия ClickGUI."),
      Map.entry("Unknown command sound.", "Звук неизвестной команды."),
      Map.entry("Item slowdown bypass mode.", "Режим обхода замедления предметов."),
      Map.entry("Server mode.", "Режим сервера."),
      Map.entry("Flight height above target.", "Высота полёта над целью."),
      Map.entry("Equip chestplate on disable when possible.", "Надевает нагрудник при выключении, если возможно."),
      Map.entry("Predict target movement.", "Предсказывает движение цели."),
      Map.entry("Accept teleport requests only from friends.", "Принимает запросы телепорта только от друзей."),
      Map.entry("Swing animation type.", "Тип анимации взмаха."),
      Map.entry("Swing animation strength.", "Сила анимации взмаха."),
      Map.entry("Swing animation duration multiplier.", "Множитель длительности анимации взмаха."),
      Map.entry("Animate only while the player swings.", "Анимирует только во время взмаха игрока."),
      Map.entry("Animate only while Aura has a target.", "Анимирует только когда у Aura есть цель."),
      Map.entry("Pay target nickname.", "Ник цели для оплаты."),
      Map.entry("Pay amount.", "Сумма оплаты."),
      Map.entry("Delay in seconds.", "Задержка в секундах."),
      Map.entry("Restart module by timer.", "Перезапускает модуль по таймеру."),
      Map.entry("Restart delay in minutes.", "Задержка перезапуска в минутах."),
      Map.entry("Invisible entity alpha.", "Прозрачность невидимых сущностей."),
      Map.entry("Flight mode.", "Режим полёта."),
      Map.entry("Horizontal speed.", "Горизонтальная скорость."),
      Map.entry("Vertical speed.", "Вертикальная скорость."),
      Map.entry("Health threshold.", "Порог здоровья."),
      Map.entry("Elytra health threshold.", "Порог здоровья для элитры."),
      Map.entry("Totem equip triggers.", "Триггеры экипировки тотема."),
      Map.entry("Return offhand item after danger.", "Возвращает предмет во вторую руку после опасности."),
      Map.entry("Danger crystal distance.", "Опасная дистанция до кристалла."),
      Map.entry("Danger crystal vertical distance.", "Опасная вертикальная дистанция до кристалла."),
      Map.entry("Danger fall height.", "Опасная высота падения.")
   );

   private ClientLanguage() {
   }

   @SafeVarargs
   private static Map<String, String> entries(Entry<String, String>... entries) {
      Map<String, String> values = new LinkedHashMap<>();

      for (Entry<String, String> entry : entries) {
         values.put(entry.getKey(), entry.getValue());
      }

      return Map.copyOf(values);
   }

   public static String tr(String text) {
      if (text == null || text.isBlank()) {
         return text == null ? "" : text;
      } else if (current == ClientLanguage.Lang.EN) {
         return text;
      } else {
         String translated = RU.get(text);
         return translated != null ? translated : DESCRIPTION_RU.getOrDefault(text, fallbackNameRu(text));
      }
   }

   private static String fallbackNameRu(String text) {
      String value = replaceWords(text);
      return value.equals(text) ? text : value;
   }

   private static String replaceWords(String text) {
      String value = text.replace("Main Hand", "Основная рука");
      value = value.replace("Off Hand", "Вторая рука");
      value = value.replace("Screen Overlay", "Оверлей экрана");
      value = value.replace("Change Saturation", "Изменить насыщенность");
      value = value.replace("Legacy Preview", "Превью Legacy");
      value = value.replace("Legacy Test", "Тест Legacy");
      value = value.replace("Legacy Sound", "Звук Legacy");
      value = value.replace("Legacy Volume", "Громкость Legacy");
      value = value.replace("Legacy Type", "Тип Legacy");
      value = value.replace("Tint Intensity", "Сила тонировки");
      value = value.replace("Tint Color", "Цвет тонировки");
      value = value.replace("Edge Glow Intensity", "Сила свечения краев");
      value = value.replace("Edge Glow", "Свечение краев");
      value = value.replace("Tint", "Тонировка");
      value = value.replace("Flame", "Пламя");
      value = value.replace("Particle Collisions", "Столкновения частиц");
      value = value.replace("Render As", "Рендерить как");
      value = value.replace("On Collision", "При столкновении");
      value = value.replace("No Clouds", "Без облаков");
      value = value.replace("Custom Sky", "Свое небо");
      value = value.replace("Sky Mode", "Режим неба");
      value = value.replace("Sky Color", "Цвет неба");
      value = value.replace("Sky Speed", "Скорость неба");
      value = value.replace("Sky Intensity", "Интенсивность неба");
      value = value.replace("Fog Color", "Цвет тумана");
      value = value.replace("Friend Color", "Цвет друзей");
      value = value.replace("Fill Color", "Цвет заливки");
      value = value.replace("Wave Color", "Цвет волны");
      value = value.replace("Shader Mode", "Режим шейдера");
      value = value.replace("Follow Smoothness", "Плавность слежения");
      value = value.replace("Item Mode", "Режим предмета");
      value = value.replace("Leave Type", "Тип выхода");
      value = value.replace("Minimum HP", "Минимум HP");
      value = value.replace("Send Speed", "Скорость отправки");
      value = value.replace("Play For Money", "Игра на деньги");
      value = value.replace("Search Item", "Поиск предмета");
      value = value.replace("Storage Relist", "Перевыставление склада");
      value = value.replace("Cheapest Color", "Цвет самого дешевого");
      value = value.replace("Per Item Color", "Цвет цены за штуку");
      value = value.replace("Default Loot", "Обычный лут");
      value = value.replace("FT Warden Loot", "FT Warden лут");
      value = value.replace("Max Scale", "Макс. масштаб");
      value = value.replace("Dynamic Scale", "Динамический масштаб");
      value = value.replace("Smooth Textures", "Сглаживать текстуры");
      value = value.replace("Bright Mode", "Яркий режим");
      value = value.replace("Rainbow Colors", "Радужные цвета");
      value = value.replace("World Count", "Частиц в мире");
      value = value.replace("World Spread", "Разброс в мире");
      value = value.replace("World Spawn Direction", "Направление спавна в мире");
      value = value.replace("World Force", "Сила в мире");
      value = value.replace("World Lifetime", "Время жизни в мире");
      value = value.replace("Attack Count", "Частиц при атаке");
      value = value.replace("Attack Spread", "Разброс при атаке");
      value = value.replace("Attack Spawn Direction", "Направление спавна при атаке");
      value = value.replace("Attack Force", "Сила при атаке");
      value = value.replace("Attack Lifetime", "Время жизни при атаке");
      value = value.replace("Start Speed", "Стартовая скорость");
      value = value.replace("Motion Influence", "Влияние движения");
      value = value.replace("Turn Sharpness", "Резкость поворота");
      value = value.replace("Hands", "Руки");
      value = value.replace("Hand", "Рука");
      value = value.replace("Only", "Только");
      value = value.replace("Items", "Предметы");
      value = value.replace("Item", "Предмет");
      value = value.replace("Targets", "Цели");
      value = value.replace("Entities", "Сущности");
      value = value.replace("Players", "Игроки");
      value = value.replace("Mobs", "Мобы");
      value = value.replace("Friends", "Друзья");
      value = value.replace("Color", "Цвет");
      value = value.replace("Mode", "Режим");
      value = value.replace("Speed", "Скорость");
      value = value.replace("Smoothness", "Плавность");
      value = value.replace("Strength", "Сила");
      value = value.replace("Intensity", "Интенсивность");
      value = value.replace("Count", "Кол-во");
      value = value.replace("Range", "Дальность");
      value = value.replace("Distance", "Дистанция");
      value = value.replace("Lifetime", "Время жизни");
      value = value.replace("Gravity", "Гравитация");
      value = value.replace("Textures", "Текстуры");
      value = value.replace("Texture", "Текстура");
      value = value.replace("Render", "Рендер");
      value = value.replace("Events", "События");
      value = value.replace("Sound", "Звук");
      value = value.replace("Volume", "Громкость");
      value = value.replace("Type", "Тип");
      value = value.replace("Style", "Стиль");
      value = value.replace("Key", "Клавиша");
      value = value.replace("X", "X");
      value = value.replace("Y", "Y");
      return value.replace("Z", "Z");
   }

   public static String trDescription(String text) {
      if (text == null || text.isBlank()) {
         return text == null ? "" : text;
      } else if (current == ClientLanguage.Lang.EN) {
         return text;
      } else {
         String translated = DESCRIPTION_RU.get(text);
         if (translated != null) {
            return translated;
         } else {
            translated = RU.get(text);
            return translated != null ? translated : fallbackDescriptionRu(text);
         }
      }
   }

   private static String fallbackDescriptionRu(String text) {
      String value = replaceWords(text);
      value = value.replace("Automatically", "Автоматически");
      value = value.replace("automatically", "автоматически");
      value = value.replace("Adds", "Добавляет");
      value = value.replace("Draws", "Рисует");
      value = value.replace("Draw", "Рисовать");
      value = value.replace("Shows", "Показывает");
      value = value.replace("Show", "Показывать");
      value = value.replace("Uses", "Использует");
      value = value.replace("Use", "Использовать");
      value = value.replace("Only", "Только");
      value = value.replace("Ignore", "Игнорировать");
      value = value.replace("Skip", "Пропускать");
      value = value.replace("Targets", "Цели");
      value = value.replace("targets", "цели");
      value = value.replace("Target", "Цель");
      value = value.replace("target", "цель");
      value = value.replace("Players", "Игроки");
      value = value.replace("players", "игроки");
      value = value.replace("Player", "Игрок");
      value = value.replace("player", "игрок");
      value = value.replace("Friends", "Друзья");
      value = value.replace("friends", "друзья");
      value = value.replace("Friend", "Друг");
      value = value.replace("friend", "друг");
      value = value.replace("Module", "Модуль");
      value = value.replace("module", "модуль");
      value = value.replace("Mode", "Режим");
      value = value.replace("mode", "режим");
      value = value.replace("Distance", "Дистанция");
      value = value.replace("distance", "дистанция");
      value = value.replace("Range", "Дальность");
      value = value.replace("range", "дальность");
      value = value.replace("Speed", "Скорость");
      value = value.replace("speed", "скорость");
      value = value.replace("Delay", "Задержка");
      value = value.replace("delay", "задержка");
      value = value.replace("Color", "Цвет");
      value = value.replace("color", "цвет");
      value = value.replace("Sound", "Звук");
      value = value.replace("sound", "звук");
      value = value.replace("Volume", "Громкость");
      value = value.replace("volume", "громкость");
      value = value.replace("Key", "Клавиша");
      value = value.replace("key", "клавиша");
      value = value.replace("Bind", "Бинд");
      value = value.replace("bind", "бинд");
      value = value.replace("Attack", "Атака");
      value = value.replace("attack", "атака");
      value = value.replace("Aim", "Наводка");
      value = value.replace("aim", "наводка");
      value = value.replace("Rotation", "Ротация");
      value = value.replace("rotation", "ротация");
      value = value.replace("Particles", "Частицы");
      value = value.replace("particles", "частицы");
      value = value.replace("Particle", "Частица");
      value = value.replace("particle", "частица");
      value = value.replace("Render", "Рендер");
      value = value.replace("render", "рендер");
      value = value.replace("ESP", "ЕСП");
      return value.replace("HUD", "Дисплей элементы клиента");
   }

   public static void toggle() {
      current = current == ClientLanguage.Lang.EN ? ClientLanguage.Lang.RU : ClientLanguage.Lang.EN;
      saveLanguage();
   }

   public static void set(ClientLanguage.Lang lang) {
      if (lang != null && lang != current) {
         current = lang;
         saveLanguage();
      }
   }

   public static ClientLanguage.Lang current() {
      return current;
   }

   public static String buttonText() {
      return current == ClientLanguage.Lang.EN ? "EN" : "RU";
   }

   private static void loadLanguage() {
      try {
         Path file = ConfigManager.systemDirectory().resolve("language.daamky");
         if (!Files.exists(file)) {
            return;
         }

         try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject json = (JsonObject)GSON.fromJson(reader, JsonObject.class);
            if (json != null && json.has("language")) {
               String langStr = json.get("language").getAsString();

               try {
                  current = ClientLanguage.Lang.valueOf(langStr);
               } catch (IllegalArgumentException var6) {
                  current = ClientLanguage.Lang.EN;
               }
            }
         }
      } catch (Exception var8) {
      }
   }

   private static void saveLanguage() {
      try {
         Files.createDirectories(ConfigManager.systemDirectory());
         Path file = ConfigManager.systemDirectory().resolve("language.daamky");
         JsonObject json = new JsonObject();
         json.addProperty("language", current.name());

         try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(json, writer);
         }
      } catch (Exception var7) {
      }
   }

   static {
      loadLanguage();
   }

   public static enum Lang {
      EN,
      RU;

      private Lang() {
      }
   }
}
