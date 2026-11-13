package daamky.ua.api.module.impl.visual;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity.TextDisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import daamky.ua.api.events.annotation.SubscribeEvent;
import daamky.ua.api.events.impl.DrawEvent;
import daamky.ua.api.events.impl.WorldRenderEvent;
import daamky.ua.api.module.Module;
import daamky.ua.api.module.ModuleCategory;
import daamky.ua.api.module.impl.combat.AntiBot;
import daamky.ua.api.module.impl.visual.esp.EspColors;
import daamky.ua.api.module.impl.visual.esp.EspGeometry;
import daamky.ua.api.module.impl.visual.esp.EspHealthTracker;
import daamky.ua.api.module.impl.visual.esp.EspTagRenderer;
import daamky.ua.api.module.impl.visual.esp.EspVisualRenderer;
import daamky.ua.api.module.impl.visual.esp.PlayerTagResolver;
import daamky.ua.api.settings.impl.BooleanSetting;
import daamky.ua.api.settings.impl.ColorSetting;
import daamky.ua.api.settings.impl.ModeSetting;
import daamky.ua.api.settings.impl.MultiModeSetting;
import daamky.ua.api.settings.impl.NumberSetting;
import daamky.ua.utils.render.item.RenderItem;
import daamky.ua.utils.repository.friend.FriendUtils;

public final class ESP extends Module {
   private static final Comparator<ESP.RenderEntry> RENDER_DISTANCE_COMPARATOR = Comparator.comparingDouble(ESP.RenderEntry::distanceSqr).reversed();
   private static final Comparator<ESP.CompactItemRowAccumulator> COMPACT_ITEM_ROW_COMPARATOR = Comparator.comparing(ESP.CompactItemRowAccumulator::id)
      .thenComparing(ESP.CompactItemRowAccumulator::name);
   private final MultiModeSetting targets = this.register(
      new MultiModeSetting("Targets", "Entities rendered by ESP.", new String[]{"Players", "Items"}, "Players", "Items")
   );
   private final MultiModeSetting elements = this.register(
      new MultiModeSetting("Render", "ESP parts to draw.", new String[]{"Name", "Potions", "Equipment", "2D Box", "3D Box"}, "Name", "Potions", "Equipment")
   );
   private final MultiModeSetting tagElements = this.register(
      new MultiModeSetting(
         "Tag Elements",
         "Parts shown inside player name tags.",
         new String[]{"Head", "Friend Prefix", "Donation", "Prefix", "Suffix", "Health"},
         "Head",
         "Friend Prefix",
         "Donation",
         "Prefix",
         "Suffix",
         "Health"
      )
   );
   private final ModeSetting itemMode = this.register(new ModeSetting("Items Mode", "Ground item ESP label style.", "Normal", "Normal", "Compact"));
   private final BooleanSetting shaderBox = this.register(new BooleanSetting("Shader Box", "Use animated shader fill for 3D boxes.", true));
   private final ModeSetting shaderMode = this.register(
      new ModeSetting("Shader Mode", "3D box shader style.", "Crystal", "Crystal", "Nebula", "Pulse", "Static")
   );
   private final ColorSetting shaderWaveColor = this.register(
      new ColorSetting("Shader Wave Color", "Animated shader highlight color.", new Color(255, 102, 255, 255))
   );
   private final NumberSetting shaderAlpha = this.register(new NumberSetting("Shader Alpha", "3D box shader fill opacity.", 0.55, 0.0, 1.0, 0.01));
   private final NumberSetting shaderSpeed = this.register(new NumberSetting("Shader Speed", "3D box shader animation speed.", 1.0, 0.1, 5.0, 0.1));
   private final NumberSetting shaderIntensity = this.register(
      new NumberSetting("Shader Intensity", "3D box shader brightness and contrast.", 2.0, 0.2, 2.5, 0.05)
   );
   private final NumberSetting range = this.register(new NumberSetting("Range", "Maximum ESP render distance.", 128.0, 16.0, 256.0, 1.0));
   private final ColorSetting playerColor = this.register(new ColorSetting("Player Color", "Player ESP color.", new Color(127, 242, 255, 210)));
   private final ColorSetting friendColor = this.register(new ColorSetting("Friend Color", "Friend ESP color.", new Color(70, 255, 120, 220)));
   private static ESP instance;
   private final List<Entity> trackedEntities = new ArrayList<>();
   private final Set<Entity> seenEntities = Collections.newSetFromMap(new IdentityHashMap<>());
   private final List<ESP.RenderEntry> renderEntries = new ArrayList<>();
   private final List<ESP.RenderEntry> compactItemEntries = new ArrayList<>();
   private final List<ESP.CompactItemCluster> compactItemClusters = new ArrayList<>();
   private final EspHealthTracker healthTracker = new EspHealthTracker();
   private final PlayerTagResolver playerTagResolver = new PlayerTagResolver();
   private final EspTagRenderer tagRenderer = new EspTagRenderer(this.playerTagResolver, this.healthTracker);

   public ESP() {
      super("ESP", "Highlights players and items through walls.", ModuleCategory.VISUAL);
      instance = this;
      this.shaderBox.visibleWhen(() -> this.elements.isSelected("3D Box"));
      this.shaderMode.visibleWhen(() -> this.elements.isSelected("3D Box") && this.shaderBox.getValue());
      this.shaderWaveColor.visibleWhen(() -> this.elements.isSelected("3D Box") && this.shaderBox.getValue() && !this.shaderMode.is("Static"));
      this.shaderAlpha.visibleWhen(() -> this.elements.isSelected("3D Box") && this.shaderBox.getValue());
      this.shaderSpeed.visibleWhen(() -> this.elements.isSelected("3D Box") && this.shaderBox.getValue() && !this.shaderMode.is("Static"));
      this.shaderIntensity.visibleWhen(() -> this.elements.isSelected("3D Box") && this.shaderBox.getValue() && !this.shaderMode.is("Static"));
   }

   public static boolean shouldHideVanillaName(Entity entity) {
      return instance != null && instance.isEnabled() && instance.shouldHideVanillaNameInternal(entity);
   }

   public static boolean shouldSuppressServerTagEntity(Entity entity) {
      return instance != null && instance.isEnabled() && instance.shouldSuppressServerTagEntityInternal(entity);
   }

   public static void captureServerHealth(Entity entity, Text scoreText) {
      if (instance != null && instance.isEnabled()) {
         instance.healthTracker.capture(entity, scoreText);
      }
   }

   public static Float resolveHudHealth(LivingEntity entity) {
      return instance != null && instance.isEnabled() && entity != null ? instance.healthTracker.resolveDisplayHealth(entity) : null;
   }

   @Override
   public void onTick(MinecraftClient client) {
      this.healthTracker.prune();
      this.rebuildTargets(client);
   }

   @Override
   protected void onDisable() {
      this.trackedEntities.clear();
      this.healthTracker.clear();
   }

   @SubscribeEvent
   private void onWorldRender(WorldRenderEvent event) {
      if (this.elements.isSelected("3D Box") && mc.player != null && mc.world != null) {
         for (Entity entity : this.trackedEntities) {
            if (this.isValidTarget(entity)) {
               Box box = EspGeometry.interpolatedBox(entity, event.getPartialTicks()).expand(0.02);
               int color = this.colorFor(entity);
               EspVisualRenderer.draw3DCornerBox(
                  box,
                  color,
                  EspVisualRenderer.resolve3DLineWidth(mc, box),
                  this.shaderBox.getValue(),
                  this.shaderMode.is("Static") ? color : this.shaderWaveColor.getValue().getRGB(),
                  this.shaderAlpha(),
                  (float)(System.currentTimeMillis() % 1000000L) / 1000.0F,
                  this.shaderMode.is("Static") ? 0.1F : this.shaderSpeed.getFloat(),
                  this.shaderMode.is("Static") ? 0.2F : this.shaderIntensity.getFloat(),
                  this.shaderModeId(this.shaderMode.getValue())
               );
            }
         }
      }
   }

   @SubscribeEvent
   private void onDraw(DrawEvent event) {
      if (mc.player != null && mc.world != null && mc.getWindow() != null) {
         EspGeometry.ProjectionContext context = EspGeometry.createProjectionContext(mc, event.getPartialTicks());
         if (context != null) {
            boolean draw2DBox = this.elements.isSelected("2D Box");
            boolean drawName = this.elements.isSelected("Name");
            boolean drawHealth = this.tagElements.isSelected("Health");
            boolean drawPotions = this.elements.isSelected("Potions");
            boolean drawEquipment = this.elements.isSelected("Equipment");
            if (draw2DBox || drawName || drawPotions || drawEquipment) {
               List<ESP.RenderEntry> entries = this.collectRenderEntries(context);
               if (!entries.isEmpty()) {
                  if (entries.size() > 1) {
                     entries.sort(RENDER_DISTANCE_COMPARATOR);
                  }

                  DrawContext graphics = event.getGraphics();
                  if (drawEquipment || drawName) {
                     RenderItem.beginFrame(graphics);
                  }

                  boolean compactItems = this.itemMode.is("Compact");
                  this.compactItemEntries.clear();

                  for (ESP.RenderEntry entry : entries) {
                     Entity entity = entry.entity();
                     EspGeometry.ScreenBox box = entry.screenBox();
                     float alpha = this.alphaForDistance(entry.distanceSqr());
                     if (draw2DBox) {
                        EspVisualRenderer.draw2DBox(box, entry.color(), alpha);
                     }

                     if (drawEquipment && entity instanceof PlayerEntity player) {
                        this.tagRenderer.drawEquipment(player, box, entry.topAnchor(), alpha);
                     }

                     if (drawPotions && entity instanceof LivingEntity living) {
                        this.tagRenderer.drawPotions(living, box, alpha);
                     }

                     if (compactItems && drawName && entity instanceof ItemEntity) {
                        this.compactItemEntries.add(entry);
                     } else if (drawName) {
                        this.tagRenderer.drawTag(entity, box, entry.topAnchor(), drawHealth, alpha, this.tagElements, mc, graphics);
                     }
                  }

                  if (!this.compactItemEntries.isEmpty()) {
                     this.drawCompactItemTags(this.compactItemEntries, graphics, context);
                  }

                  if (drawEquipment || drawName) {
                     RenderItem.flush();
                  }
               }
            }
         }
      }
   }

   private void drawCompactItemTags(List<ESP.RenderEntry> itemEntries, DrawContext graphics, EspGeometry.ProjectionContext context) {
      this.compactItemClusters.clear();

      for (ESP.RenderEntry entry : itemEntries) {
         Entity screen = entry.entity();
         if (screen instanceof ItemEntity) {
            ItemEntity item = (ItemEntity)screen;
            Vec3d position = item.getLerpedPos(context.tickDelta());
            if (EspGeometry.isFinite(position)) {
               ESP.CompactItemCluster cluster = null;

               for (ESP.CompactItemCluster candidate : this.compactItemClusters) {
                  if (candidate.accepts(position)) {
                     cluster = candidate;
                     break;
                  }
               }

               if (cluster == null) {
                  cluster = new ESP.CompactItemCluster(position);
                  this.compactItemClusters.add(cluster);
               }

               cluster.add(item, position, this.alphaForDistance(entry.distanceSqr()));
            }
         }
      }

      for (ESP.CompactItemCluster cluster : this.compactItemClusters) {
         Vec3d anchor = cluster.anchor();
         EspGeometry.ScreenPoint screen = EspGeometry.projectWorldToScreenSpace(anchor, context);
         if (screen != null && !(screen.z() < 0.0) && !(screen.z() > 1.0)) {
            double marginX = (double)context.screenWidth() * 0.35;
            double marginY = (double)context.screenHeight() * 0.35;
            if (!(screen.x() < -marginX)
               && !(screen.x() > (double)context.screenWidth() + marginX)
               && !(screen.y() < -marginY)
               && !(screen.y() > (double)context.screenHeight() + marginY)) {
               EspVisualRenderer.drawCompactItemTag(cluster.rows(), screen.x(), screen.y(), cluster.alpha(), graphics);
            }
         }
      }
   }

   private List<ESP.RenderEntry> collectRenderEntries(EspGeometry.ProjectionContext context) {
      this.renderEntries.clear();

      for (Entity entity : this.trackedEntities) {
         if (this.isValidTarget(entity)) {
            EspGeometry.ScreenBox screenBox = EspGeometry.projectEntityBox(entity, context);
            if (screenBox != null && EspGeometry.isOnScreen(screenBox, context)) {
               EspGeometry.ScreenAnchor topAnchor = EspGeometry.resolveTopAnchor(entity, context, screenBox);
               Vec3d interpolated = entity.getLerpedPos(context.tickDelta());
               this.renderEntries
                  .add(new ESP.RenderEntry(entity, screenBox, topAnchor, context.cameraPos().squaredDistanceTo(interpolated), this.colorFor(entity)));
            }
         }
      }

      return this.renderEntries;
   }

   private void rebuildTargets(MinecraftClient client) {
      this.trackedEntities.clear();
      if (client != null && client.world != null && client.player != null) {
         this.seenEntities.clear();
         if (this.targets.isSelected("Players")) {
            for (PlayerEntity player : client.world.getPlayers()) {
               this.addIfValid(this.seenEntities, player);
            }
         }

         if (this.targets.isSelected("Items")) {
            for (Entity entity : client.world.getEntities()) {
               if (!(entity instanceof PlayerEntity)) {
                  this.addIfValid(this.seenEntities, entity);
               }
            }
         }
      }
   }

   private void addIfValid(Set<Entity> seen, Entity entity) {
      if (this.isValidTarget(entity) && seen.add(entity)) {
         this.trackedEntities.add(entity);
      }
   }

   private boolean isValidTarget(Entity entity) {
      if (entity != null && !entity.isRemoved() && mc.player != null && mc.world != null) {
         if (entity == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return false;
         } else if (this.range.getFloat() > 0.0F
            && entity != mc.player
            && mc.player.squaredDistanceTo(entity) > (double)(this.range.getFloat() * this.range.getFloat())) {
            return false;
         } else if (!EspGeometry.isFinite(entity.getEntityPos())) {
            return false;
         } else if (entity instanceof PlayerEntity player) {
            return this.targets.isSelected("Players") && !player.isSpectator() && !AntiBot.shouldIgnore(player);
         } else {
            return !(entity instanceof ItemEntity item) ? false : this.targets.isSelected("Items") && !item.getStack().isEmpty();
         }
      } else {
         return false;
      }
   }

   private boolean shouldHideVanillaNameInternal(Entity entity) {
      if (!this.shouldHidePlayerLabels()) {
         return false;
      } else {
         return entity instanceof PlayerEntity player
            ? this.isPlayerLabelTarget(player)
            : this.isServerTagEntity(entity) && this.isPlayerServerTagEntity(entity);
      }
   }

   private boolean shouldSuppressServerTagEntityInternal(Entity entity) {
      return this.shouldHidePlayerLabels() && this.isServerTagEntity(entity) ? this.isPlayerServerTagEntity(entity) : false;
   }

   private boolean shouldHidePlayerLabels() {
      return this.elements.isSelected("Name") && this.targets.isSelected("Players") && mc.player != null && mc.world != null;
   }

   private boolean isPlayerLabelTarget(PlayerEntity player) {
      if (player != null && !player.isRemoved() && !player.isSpectator() && !AntiBot.shouldIgnore(player)) {
         if (player == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) {
            return false;
         } else {
            return this.range.getFloat() > 0.0F
                  && player != mc.player
                  && mc.player.squaredDistanceTo(player) > (double)(this.range.getFloat() * this.range.getFloat())
               ? false
               : EspGeometry.isFinite(player.getEntityPos());
         }
      } else {
         return false;
      }
   }

   private boolean isServerTagEntity(Entity entity) {
      if (entity == null || entity instanceof PlayerEntity) {
         return false;
      } else if (entity instanceof TextDisplayEntity) {
         return true;
      } else {
         return entity instanceof ArmorStandEntity armorStand
            ? armorStand.hasCustomName() || armorStand.shouldRenderName() || armorStand.isMarker()
            : entity.hasCustomName() && entity.shouldRenderName();
      }
   }

   private boolean isPlayerServerTagEntity(Entity tagEntity) {
      if (tagEntity != null && EspGeometry.isFinite(tagEntity.getEntityPos())) {
         String tagText = this.serverTagText(tagEntity);
         if (tagText.isBlank()) {
            return false;
         } else {
            for (PlayerEntity player : mc.world.getPlayers()) {
               if (this.isPlayerLabelTarget(player) && this.isAttachedPlayerServerTag(tagEntity, player, tagText)) {
                  return true;
               }
            }

            return false;
         }
      } else {
         return false;
      }
   }

   private boolean isAttachedPlayerServerTag(Entity tagEntity, PlayerEntity player, String tagText) {
      return this.containsPlayerName(tagText, player)
         ? this.isNearPlayerNameLabel(tagEntity, player)
         : this.looksLikeHealthTag(tagText) && this.isNearPlayerHealthLabel(tagEntity, player);
   }

   private boolean isNearPlayerNameLabel(Entity tagEntity, PlayerEntity player) {
      double dx = tagEntity.getX() - player.getX();
      double dz = tagEntity.getZ() - player.getZ();
      double horizontalLimit = 1.1;
      if (dx * dx + dz * dz > horizontalLimit * horizontalLimit) {
         return false;
      } else {
         double aboveFeet = tagEntity.getY() - player.getY();
         return aboveFeet >= 1.2 && aboveFeet <= 4.4;
      }
   }

   private boolean isNearPlayerHealthLabel(Entity tagEntity, PlayerEntity player) {
      double dx = tagEntity.getX() - player.getX();
      double dz = tagEntity.getZ() - player.getZ();
      double horizontalLimit = 0.95;
      if (dx * dx + dz * dz > horizontalLimit * horizontalLimit) {
         return false;
      } else {
         double aboveFeet = tagEntity.getY() - player.getY();
         return aboveFeet >= 1.25 && aboveFeet <= 3.35;
      }
   }

   private String serverTagText(Entity entity) {
      Text text = null;
      if (entity instanceof TextDisplayEntity textDisplay) {
         text = textDisplay.getText();
      }

      if (text == null) {
         text = entity.getCustomName();
      }

      return this.cleanServerTagText(text == null ? "" : text.getString());
   }

   private boolean containsPlayerName(String tagText, PlayerEntity player) {
      String compactTag = this.compactSearchText(tagText);
      String scoreboardName = this.compactSearchText(player.getNameForScoreboard());
      String displayName = this.compactSearchText(player.getName().getString());
      return !scoreboardName.isBlank() && compactTag.contains(scoreboardName) || !displayName.isBlank() && compactTag.contains(displayName);
   }

   private boolean looksLikeHealthTag(String tagText) {
      String cleaned = this.cleanServerTagText(tagText).toLowerCase(Locale.ROOT);
      if (cleaned.isBlank()) {
         return false;
      } else {
         boolean hasHealthMarker = cleaned.contains("hp") || cleaned.contains("хп") || cleaned.indexOf(10084) >= 0 || cleaned.indexOf(9829) >= 0;
         int digits = 0;
         int invalid = 0;

         for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isDigit(c)) {
               digits++;
            } else if (!Character.isWhitespace(c)
               && c != '.'
               && c != ','
               && c != '/'
               && c != 10084
               && c != 9829
               && c != '['
               && c != ']'
               && c != '('
               && c != ')'
               && c != 'h'
               && c != 'p'
               && c != 1093
               && c != 1087) {
               invalid++;
            }
         }

         return digits > 0 && invalid == 0 && (hasHealthMarker || cleaned.trim().length() <= 4);
      }
   }

   private String cleanServerTagText(String text) {
      if (text != null && !text.isBlank()) {
         StringBuilder builder = new StringBuilder(text.length());
         boolean skipFormatting = false;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (skipFormatting) {
               skipFormatting = false;
            } else if (c == 167) {
               skipFormatting = true;
            } else if (!Character.isISOControl(c)) {
               builder.append(c);
            }
         }

         return builder.toString().trim();
      } else {
         return "";
      }
   }

   private String compactSearchText(String text) {
      String cleaned = this.cleanServerTagText(text).toLowerCase(Locale.ROOT);
      if (cleaned.isBlank()) {
         return "";
      } else {
         StringBuilder builder = new StringBuilder(cleaned.length());

         for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
               builder.append(c);
            }
         }

         return builder.toString();
      }
   }

   private int colorFor(Entity entity) {
      if (entity instanceof PlayerEntity player) {
         return FriendUtils.isFriend(player) ? this.friendColor.getValue().getRGB() : this.playerColor.getValue().getRGB();
      } else {
         return EspColors.ITEM;
      }
   }

   private float shaderAlpha() {
      return Math.clamp(this.shaderAlpha.getFloat(), 0.0F, 1.0F);
   }

   private int shaderModeId(String mode) {
      return switch (mode) {
         case "Nebula" -> 1;
         case "Hologram" -> 2;
         case "Pulse", "Static" -> 3;
         default -> 0;
      };
   }

   private float alphaForDistance(double distanceSqr) {
      double distance = Math.sqrt(Math.max(0.0, distanceSqr));
      return (float)MathHelper.clamp(1.0 - (distance - 96.0) / 80.0, 0.48, 1.0);
   }

   private static String cleanItemName(String text) {
      if (text != null && !text.isBlank()) {
         StringBuilder builder = new StringBuilder(text.length());
         boolean skipFormatting = false;

         for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (skipFormatting) {
               skipFormatting = false;
            } else if (c == 167) {
               skipFormatting = true;
            } else if (!Character.isISOControl(c)) {
               builder.append(c);
            }
         }

         return builder.toString().trim();
      } else {
         return "";
      }
   }

   private static final class CompactItemCluster {
      private double worldX;
      private double worldY;
      private double worldZ;
      private double anchorY;
      private float alpha;
      private int entries;
      private final Map<ESP.CompactItemKey, ESP.CompactItemRowAccumulator> rows = new LinkedHashMap<>();
      private final List<ESP.CompactItemRowAccumulator> sortedRows = new ArrayList<>();
      private final List<EspVisualRenderer.ItemTagRow> renderRows = new ArrayList<>();

      private CompactItemCluster(Vec3d first) {
         this.worldX = first.x;
         this.worldY = first.y;
         this.worldZ = first.z;
         this.anchorY = first.y + 0.72;
      }

      private boolean accepts(Vec3d position) {
         double dx = position.x - this.worldX;
         double dy = Math.abs(position.y - this.worldY);
         double dz = position.z - this.worldZ;
         return dx * dx + dz * dz <= 7.29 && dy <= 1.75;
      }

      private void add(ItemEntity item, Vec3d position, float entryAlpha) {
         double nextWeight = (double)this.entries + 1.0;
         this.worldX = (this.worldX * (double)this.entries + position.x) / nextWeight;
         this.worldY = (this.worldY * (double)this.entries + position.y) / nextWeight;
         this.worldZ = (this.worldZ * (double)this.entries + position.z) / nextWeight;
         this.anchorY = Math.max(this.anchorY, position.y + 0.72);
         this.alpha = Math.max(this.alpha, entryAlpha);
         this.entries++;
         ItemStack stack = item.getStack();
         String name = ESP.cleanItemName(stack.getName().getString());
         String id = Registries.ITEM.getId(stack.getItem()).toString();
         ESP.CompactItemKey key = new ESP.CompactItemKey(id, name);
         this.rows.computeIfAbsent(key, ignored -> new ESP.CompactItemRowAccumulator(stack.copy(), id, name)).add(stack.getCount());
      }

      private List<EspVisualRenderer.ItemTagRow> rows() {
         this.sortedRows.clear();
         this.sortedRows.addAll(this.rows.values());
         if (this.sortedRows.size() > 1) {
            this.sortedRows.sort(ESP.COMPACT_ITEM_ROW_COMPARATOR);
         }

         this.renderRows.clear();

         for (ESP.CompactItemRowAccumulator row : this.sortedRows) {
            this.renderRows.add(new EspVisualRenderer.ItemTagRow(row.stack(), row.name(), row.count()));
         }

         return this.renderRows;
      }

      private Vec3d anchor() {
         return new Vec3d(this.worldX, this.anchorY, this.worldZ);
      }

      private float alpha() {
         return this.alpha;
      }
   }

   private static record CompactItemKey(String id, String name) {
   }

   private static final class CompactItemRowAccumulator {
      private final ItemStack stack;
      private final String id;
      private final String name;
      private int count;

      private CompactItemRowAccumulator(ItemStack stack, String id, String name) {
         this.stack = stack;
         this.id = id;
         this.name = name;
         this.stack.setCount(1);
      }

      private void add(int amount) {
         this.count = Math.max(1, Math.min(Integer.MAX_VALUE, this.count + Math.max(1, amount)));
      }

      private String id() {
         return this.id;
      }

      private ItemStack stack() {
         return this.stack;
      }

      private String name() {
         return this.name;
      }

      private int count() {
         return this.count;
      }
   }

   private static record RenderEntry(Entity entity, EspGeometry.ScreenBox screenBox, EspGeometry.ScreenAnchor topAnchor, double distanceSqr, int color) {
   }
}
