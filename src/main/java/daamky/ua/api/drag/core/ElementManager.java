package daamky.ua.api.drag.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import daamky.ua.api.config.ConfigManager;
import daamky.ua.utils.render.LoadingVisualGuard;

public final class ElementManager {
   private static final ElementManager INSTANCE = new ElementManager();
   private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
   private static final String FILE_NAME = "drags.daamky";
   private static final float HIT_PADDING = 3.0F;
   private static final float COLLISION_GAP = 2.0F;
   private static final int COLLISION_PASSES = 4;
   private static final float POSITION_EPSILON = 0.05F;
   private final Map<String, ElementComponent> components = new LinkedHashMap<>();
   private final Map<String, ElementManager.SavedElement> pendingStates = new LinkedHashMap<>();
   private final List<ElementComponent> sortedScratch = new ArrayList<>();
   private final List<ElementComponent> visibleScratch = new ArrayList<>();
   private final List<ElementComponent> obstacleScratch = new ArrayList<>();
   private ElementComponent active;
   private ElementScreen screen = new ElementScreen(1.0F, 1.0F, 1.0F);
   private int nextOrder;
   private boolean loaded;

   private ElementManager() {
   }

   public static ElementManager getInstance() {
      return INSTANCE;
   }

   public ElementComponent register(String id, String title, float defaultX, float defaultY) {
      if (id != null && !id.isBlank()) {
         String key = normalize(id);
         ElementComponent existing = this.components.get(key);
         if (existing != null) {
            return existing;
         } else {
            ElementComponent component = new ElementComponent(key, title != null && !title.isBlank() ? title : key, defaultX, defaultY, this.nextOrder++);
            ElementManager.SavedElement saved = this.pendingStates.get(key);
            if (saved != null) {
               component.applyState(saved.x, saved.y, saved.width, saved.height, saved.visible, saved.order);
               this.nextOrder = Math.max(this.nextOrder, saved.order + 1);
            }

            this.components.put(key, component);
            return component;
         }
      } else {
         throw new IllegalArgumentException("Element id cannot be empty");
      }
   }

   public List<ElementComponent> components() {
      return List.copyOf(this.components.values());
   }

   public void frame(ElementScreen screen) {
      if (screen != null && screen.valid()) {
         this.screen = screen;
      }

      for (ElementComponent component : this.components.values()) {
         component.clamp(this.screen);
      }

      if (this.canEdit()) {
         this.resolveCollisions();
      }
   }

   public boolean handleMouseClicked(Click event) {
      if (event != null && event.button() == 0 && this.canEdit()) {
         this.frame(ElementScreen.current());
         ElementComponent hovered = this.topmostAt((float)event.x(), (float)event.y());
         if (hovered == null) {
            return false;
         } else {
            this.active = hovered;
            this.active.order(this.nextOrder++);
            this.active.beginMove((float)event.x(), (float)event.y());
            return true;
         }
      } else {
         return false;
      }
   }

   public boolean handleMouseDragged(Click event) {
      if (event != null && this.active != null && this.canEdit()) {
         this.active.moveTo((float)event.x(), (float)event.y(), this.screen);
         this.resolveActiveCollision();
         return true;
      } else {
         return false;
      }
   }

   public void updateActiveElementFromMouse() {
      if (this.active != null && this.canEdit()) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.mouse != null && client.getWindow() != null) {
            ElementScreen currentScreen = ElementScreen.current();
            this.frame(currentScreen);
            float scale = Math.max(1.0E-4F, currentScreen.coordinateScale());
            float mouseX = (float)client.mouse.getScaledX(client.getWindow()) / scale;
            float mouseY = (float)client.mouse.getScaledY(client.getWindow()) / scale;
            this.active.moveTo(mouseX, mouseY, this.screen);
            this.resolveActiveCollision();
         }
      }
   }

   public boolean handleMouseReleased(Click event) {
      if (this.active == null) {
         return false;
      } else {
         boolean editing = this.canEdit();
         this.resolveActiveCollision();
         this.active.endMove();
         this.active.commitClamp(this.screen);
         this.active = null;
         if (editing) {
            this.resolveCollisions();
         }

         this.save();
         return editing;
      }
   }

   public void cancelActiveElement() {
      if (this.active != null) {
         this.resolveActiveCollision();
         this.active.endMove();
         this.active.commitClamp(this.screen);
         this.active = null;
         if (this.canEdit()) {
            this.resolveCollisions();
         }

         this.save();
      }
   }

   public boolean canEditCurrentScreen() {
      return this.canEdit();
   }

   public void renderEditorOverlay(DrawContext graphics, ElementScreen screen) {
      if (this.canEdit()) {
         this.frame(screen);

         for (ElementComponent component : this.sortedComponents()) {
            if (component.visible()) {
               this.renderComponentOverlay(component);
            }
         }
      }
   }

   public void load() {
      this.loaded = true;
      Path file = this.configFile();
      if (Files.exists(file)) {
         try {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
               JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
               if (root != null && root.has("components") && root.get("components").isJsonObject()) {
                  JsonObject componentsObject = root.getAsJsonObject("components");

                  for (String id : componentsObject.keySet()) {
                     ElementManager.SavedElement saved = (ElementManager.SavedElement)GSON.fromJson(componentsObject.get(id), ElementManager.SavedElement.class);
                     if (saved != null && saved.valid()) {
                        String key = normalize(id);
                        this.pendingStates.put(key, saved);
                        ElementComponent component = this.components.get(key);
                        if (component != null) {
                           component.applyState(saved.x, saved.y, saved.width, saved.height, saved.visible, saved.order);
                           this.nextOrder = Math.max(this.nextOrder, saved.order + 1);
                        }
                     }
                  }

                  return;
               }
            }
         } catch (Exception var12) {
            System.err.println("Failed to load element config: " + file + " (" + var12.getMessage() + ")");
         }
      }
   }

   public void save() {
      if (this.loaded) {
         Path file = this.configFile();

         try {
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
               JsonObject root = new JsonObject();
               root.addProperty("version", 1);
               JsonObject componentsObject = new JsonObject();

               for (ElementComponent component : this.components.values()) {
                  componentsObject.add(component.id(), GSON.toJsonTree(ElementManager.SavedElement.from(component)));
               }

               root.add("components", componentsObject);
               GSON.toJson(root, writer);
            }
         } catch (IOException var9) {
            System.err.println("Failed to save element config: " + file + " (" + var9.getMessage() + ")");
         }
      }
   }

   private ElementComponent topmostAt(float mouseX, float mouseY) {
      ElementComponent topmost = null;
      int topmostOrder = Integer.MIN_VALUE;

      for (ElementComponent component : this.components.values()) {
         int order = component.order();
         if (order > topmostOrder && component.hit(mouseX, mouseY, 3.0F)) {
            topmost = component;
            topmostOrder = order;
         }
      }

      return topmost;
   }

   private List<ElementComponent> sortedComponents() {
      this.sortedScratch.clear();
      this.sortedScratch.addAll(this.components.values());
      this.sortedScratch.sort(Comparator.comparingInt(ElementComponent::order));
      return this.sortedScratch;
   }

   private void resolveCollisions() {
      if (this.active != null) {
         this.resolveActiveCollision();
      } else {
         for (int pass = 0; pass < 4; pass++) {
            boolean changed = false;
            this.obstacleScratch.clear();

            for (ElementComponent component : this.visibleComponentsByOrder()) {
               if (component.locked()) {
                  this.obstacleScratch.add(component);
               } else {
                  ElementManager.Placement placement = this.findPlacement(component, this.obstacleScratch);
                  if (placement.changed(component)) {
                     component.settleTo(placement.x, placement.y, this.screen);
                     component.clamp(this.screen);
                     changed = true;
                  }

                  this.obstacleScratch.add(component);
               }
            }

            if (!changed) {
               return;
            }
         }
      }
   }

   private void resolveActiveCollision() {
      if (this.active != null && this.active.visible()) {
         this.obstacleScratch.clear();

         for (ElementComponent component : this.components.values()) {
            if (component != this.active && component.visible()) {
               this.obstacleScratch.add(component);
            }
         }

         ElementManager.Placement placement = this.findPlacement(this.active, this.obstacleScratch);
         if (placement.changed(this.active)) {
            this.active.resolveMoveTo(placement.x, placement.y, this.screen);
         }
      }
   }

   private List<ElementComponent> visibleComponentsByOrder() {
      this.visibleScratch.clear();
      this.visibleScratch.addAll(this.sortedComponents());
      this.visibleScratch.removeIf(component -> !component.visible());
      return this.visibleScratch;
   }

   private ElementManager.Placement findPlacement(ElementComponent component, List<ElementComponent> obstacles) {
      float desiredX = component.clampTargetX(this.screen, component.targetX());
      float desiredY = component.clampTargetY(this.screen, component.targetY());
      ElementBounds desiredBounds = component.dragBoundsAt(desiredX, desiredY);
      if (!this.overlapsAny(desiredBounds, obstacles)) {
         return new ElementManager.Placement(desiredX, desiredY);
      } else {
         float boundsOffsetX = desiredX - desiredBounds.x();
         float boundsOffsetY = desiredY - desiredBounds.y();
         float minX = component.clampTargetX(this.screen, Float.NEGATIVE_INFINITY);
         float maxX = component.clampTargetX(this.screen, Float.POSITIVE_INFINITY);
         float minY = component.clampTargetY(this.screen, Float.NEGATIVE_INFINITY);
         float maxY = component.clampTargetY(this.screen, Float.POSITIVE_INFINITY);
         List<Float> xCandidates = new ArrayList<>();
         List<Float> yCandidates = new ArrayList<>();
         addCandidate(xCandidates, desiredX);
         addCandidate(xCandidates, minX);
         addCandidate(xCandidates, maxX);
         addCandidate(yCandidates, desiredY);
         addCandidate(yCandidates, minY);
         addCandidate(yCandidates, maxY);

         for (ElementComponent obstacle : obstacles) {
            ElementBounds obstacleBounds = obstacle.targetDragBounds();
            addCandidate(xCandidates, obstacleBounds.x() - desiredBounds.width() - 2.0F + boundsOffsetX);
            addCandidate(xCandidates, obstacleBounds.right() + 2.0F + boundsOffsetX);
            addCandidate(yCandidates, obstacleBounds.y() - desiredBounds.height() - 2.0F + boundsOffsetY);
            addCandidate(yCandidates, obstacleBounds.bottom() + 2.0F + boundsOffsetY);
         }

         ElementManager.Placement best = new ElementManager.Placement(desiredX, desiredY);
         float bestScore = Float.POSITIVE_INFINITY;

         for (float candidateX : xCandidates) {
            float x = component.clampTargetX(this.screen, candidateX);

            for (float candidateY : yCandidates) {
               float y = component.clampTargetY(this.screen, candidateY);
               ElementBounds bounds = component.dragBoundsAt(x, y);
               float overlap = this.overlapPenalty(bounds, obstacles);
               float dx = x - desiredX;
               float dy = y - desiredY;
               float distance = dx * dx + dy * dy;
               float score = overlap <= 0.0F ? distance : 1000000.0F + overlap * 10000.0F + distance;
               if (score < bestScore) {
                  bestScore = score;
                  best = new ElementManager.Placement(x, y);
               }
            }
         }

         return best;
      }
   }

   private boolean overlapsAny(ElementBounds bounds, List<ElementComponent> obstacles) {
      for (ElementComponent obstacle : obstacles) {
         if (overlaps(bounds, obstacle.targetDragBounds())) {
            return true;
         }
      }

      return false;
   }

   private float overlapPenalty(ElementBounds bounds, List<ElementComponent> obstacles) {
      float penalty = 0.0F;

      for (ElementComponent obstacle : obstacles) {
         ElementBounds obstacleBounds = obstacle.targetDragBounds();
         float left = Math.max(bounds.x(), obstacleBounds.x() - 2.0F);
         float right = Math.min(bounds.right(), obstacleBounds.right() + 2.0F);
         float top = Math.max(bounds.y(), obstacleBounds.y() - 2.0F);
         float bottom = Math.min(bounds.bottom(), obstacleBounds.bottom() + 2.0F);
         if (right > left && bottom > top) {
            penalty += (right - left) * (bottom - top);
         }
      }

      return penalty;
   }

   private static boolean overlaps(ElementBounds first, ElementBounds second) {
      return first.right() + 2.0F > second.x() && second.right() + 2.0F > first.x() && first.bottom() + 2.0F > second.y() && second.bottom() + 2.0F > first.y();
   }

   private static void addCandidate(List<Float> candidates, float value) {
      if (Float.isFinite(value)) {
         for (float candidate : candidates) {
            if (Math.abs(candidate - value) <= 0.05F) {
               return;
            }
         }

         candidates.add(value);
      }
   }

   private boolean canEdit() {
      MinecraftClient client = MinecraftClient.getInstance();
      return client != null
         && client.player != null
         && client.world != null
         && client.currentScreen instanceof ChatScreen
         && !LoadingVisualGuard.shouldSuppressHud(client);
   }

   private void renderComponentOverlay(ElementComponent component) {
   }

   private Path configFile() {
      return ConfigManager.systemDirectory().resolve("drags.daamky");
   }

   private static String normalize(String id) {
      return id.trim().toLowerCase().replace(' ', '_');
   }

   private static record Placement(float x, float y) {
      private boolean changed(ElementComponent component) {
         return Math.abs(component.targetX() - this.x) > 0.05F || Math.abs(component.targetY() - this.y) > 0.05F;
      }
   }

   private static final class SavedElement {
      private float x;
      private float y;
      private float width;
      private float height;
      private boolean visible = true;
      private int order;

      private SavedElement() {
      }

      private static ElementManager.SavedElement from(ElementComponent component) {
         ElementManager.SavedElement saved = new ElementManager.SavedElement();
         saved.x = component.targetX();
         saved.y = component.targetY();
         saved.width = component.width();
         saved.height = component.height();
         saved.visible = component.visible();
         saved.order = component.order();
         return saved;
      }

      private boolean valid() {
         return Float.isFinite(this.x)
            && Float.isFinite(this.y)
            && Float.isFinite(this.width)
            && Float.isFinite(this.height)
            && this.width > 0.0F
            && this.height > 0.0F;
      }
   }
}
