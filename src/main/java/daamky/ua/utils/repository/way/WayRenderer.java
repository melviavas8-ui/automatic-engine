package daamky.ua.utils.repository.way;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import daamky.ua.api.module.impl.visual.esp.EspGeometry;
import daamky.ua.utils.render.animation.Easings;
import daamky.ua.utils.render.animation.SmoothAnimation;
import daamky.ua.utils.render.color.ColorUtil;
import daamky.ua.utils.render.ui.Render2D;
import daamky.ua.utils.render.ui.font.FontType;

public final class WayRenderer {
   private final WayRenderer.Style style = new WayRenderer.Style();
   private final SmoothAnimation bobAnimation = new SmoothAnimation();
   private final List<WayRenderer.RenderEntry> entries = new ArrayList<>();
   private final Map<WayRenderer.WayKey, WayRenderer.DistanceState> distanceStates = new HashMap<>();
   private boolean bobDown;
   private boolean bobStarted;
   private float frameBobOffset;
   private long frameTimeMillis;

   public WayRenderer() {
   }

   public WayRenderer.Style style() {
      return this.style;
   }

   public void beginFrame() {
      this.entries.clear();
      this.frameTimeMillis = System.currentTimeMillis();
      this.pruneDistanceStates();
      this.frameBobOffset = this.bobOffset();
   }

   public void queue(MinecraftClient mc, Way way, EspGeometry.ScreenPoint screen, double distance) {
      if (mc != null && mc.getWindow() != null && way != null && screen != null) {
         if (Double.isFinite(screen.x()) && Double.isFinite(screen.y()) && Double.isFinite(screen.z()) && !(screen.z() < 0.0)) {
            WayRenderer.DistanceState distanceState = this.distanceStates.computeIfAbsent(WayRenderer.WayKey.of(way), key -> new WayRenderer.DistanceState());
            int displayDistance = distanceState.update(distance, this.frameTimeMillis, this.style);
            String nameText = way.name();
            String separatorText = " - ";
            String distanceText = this.formatDistance(displayDistance);
            float nameWidth = Render2D.textWidth(FontType.SEMIBOLD, nameText, this.style.textSize);
            float separatorWidth = Render2D.textWidth(FontType.SEMIBOLD, separatorText, this.style.textSize);
            float distanceWidth = Render2D.textWidth(FontType.SEMIBOLD, distanceText, this.style.textSize);
            float distanceSlotWidth = distanceState.stableSlotWidth(Math.max(distanceWidth, this.minDistanceSlotWidth()));
            float width = nameWidth + separatorWidth + distanceSlotWidth + this.style.paddingX * 2.0F;
            float height = this.style.height;
            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();
            float rawCenterX = (float)screen.x();
            float rawCenterY = (float)screen.y();
            float rawX = rawCenterX - width * 0.5F;
            float rawY = rawCenterY - height * 0.5F;
            if (!this.isOutsideScreen(rawX, rawY, width, height, screenWidth, screenHeight)) {
               float centerX = rawCenterX;
               float x = rawCenterX - width * 0.5F;
               float y = rawCenterY - height * 0.5F;
               if (this.style.snapLabelToPixel) {
                  x = (float)Math.round(x);
                  y = (float)Math.round(y);
                  centerX = x + width * 0.5F;
               }

               float radius = this.safeRadius(width, height, this.style.radius);
               float accentRadius = this.safeRadius(this.style.accentSize, this.style.accentSize, this.style.accentRadius);
               float accentY = y + height + this.style.accentOffsetY + this.frameBobOffset;
               this.entries
                  .add(
                     new WayRenderer.RenderEntry(
                        nameText,
                        separatorText,
                        distanceText,
                        x,
                        y,
                        centerX,
                        width,
                        height,
                        radius,
                        accentRadius,
                        accentY,
                        nameWidth,
                        separatorWidth,
                        distanceWidth,
                        distanceSlotWidth
                     )
                  );
            }
         }
      }
   }

   public void renderQueued(DrawContext graphics) {
      if (graphics != null && !this.entries.isEmpty()) {
         for (WayRenderer.RenderEntry entry : this.entries) {
            this.renderBackground(entry);
         }

         graphics.createNewRootLayer();
         Render2D.beginFrame(graphics);

         for (WayRenderer.RenderEntry entry : this.entries) {
            this.renderText(entry);
         }

         graphics.createNewRootLayer();
         Render2D.beginFrame(graphics);
      }
   }

   private void renderBackground(WayRenderer.RenderEntry entry) {
      float x = entry.x();
      float y = entry.y();
      float width = entry.width();
      float height = entry.height();
      float radius = entry.radius();
      Render2D.blur(x, y, width, height, radius, this.style.blurRadius, 1.0F, this.style.blurColor);
      Render2D.rect(x, y, width, height, radius, this.style.backgroundColor);
      Render2D.outline(x, y, width, height, radius, this.style.outlineWidth, this.style.outlineColor);
   }

   private void renderText(WayRenderer.RenderEntry entry) {
      float textX = entry.x() + this.style.paddingX;
      float textY = entry.y() + this.style.textOffsetY;
      if (this.style.snapTextToPixel) {
         textX = (float)Math.round(textX);
         textY = (float)Math.round(textY);
      }

      float separatorX = textX + entry.nameWidth();
      float distanceX = textX + entry.nameWidth() + entry.separatorWidth();
      if (this.style.snapTextToPixel) {
         separatorX = (float)Math.round(separatorX);
         distanceX = (float)Math.round(distanceX);
      }

      int passes = Math.max(1, this.style.textStrengthPasses);

      for (int i = 0; i < passes; i++) {
         Render2D.text(FontType.SEMIBOLD, entry.nameText(), textX, textY, this.style.textSize, this.style.textColor);
         Render2D.text(FontType.SEMIBOLD, entry.separatorText(), separatorX, textY, this.style.textSize, this.style.textColor);
         Render2D.text(FontType.SEMIBOLD, entry.distanceText(), distanceX, textY, this.style.textSize, this.style.textColor);
      }
   }

   private String formatDistance(int distance) {
      return String.format(Locale.ROOT, "%dm", Math.max(0, distance));
   }

   private float minDistanceSlotWidth() {
      int digits = Math.max(1, this.style.minDistanceIntegerDigits);
      return Render2D.textWidth(FontType.SEMIBOLD, "8".repeat(digits) + "m", this.style.textSize);
   }

   private float bobOffset() {
      if (this.style.bobEnabled && !(this.style.bobAmplitude <= 0.0F)) {
         if (!this.bobStarted) {
            this.bobStarted = true;
            this.bobDown = true;
            this.bobAnimation.set(-1.0);
            this.bobAnimation.run(1.0, this.style.bobDuration, Easings.EXPO_IN_OUT);
         } else if (!this.bobAnimation.isAlive()) {
            this.bobDown = !this.bobDown;
            this.bobAnimation.run(this.bobDown ? 1.0 : -1.0, this.style.bobDuration, Easings.EXPO_IN_OUT);
         }

         this.bobAnimation.update();
         return this.bobAnimation.get() * this.style.bobAmplitude;
      } else {
         this.bobStarted = false;
         this.bobAnimation.set(0.0);
         return 0.0F;
      }
   }

   private boolean isOutsideScreen(float x, float y, float width, float height, int screenWidth, int screenHeight) {
      float maxY = y + height + this.style.accentOffsetY + this.style.accentSize + Math.abs(this.style.bobAmplitude);
      float margin = this.style.offscreenMargin;
      return x + width < -margin || x > (float)screenWidth + margin || maxY < -margin || y > (float)screenHeight + margin;
   }

   private float safeRadius(float width, float height, float radius) {
      return MathHelper.clamp(radius, 0.0F, Math.max(0.0F, Math.min(width, height) * 0.5F));
   }

   private void pruneDistanceStates() {
      Iterator<WayRenderer.DistanceState> iterator = this.distanceStates.values().iterator();

      while (iterator.hasNext()) {
         WayRenderer.DistanceState state = iterator.next();
         if (state.lastSeenMillis > 0L && this.frameTimeMillis - state.lastSeenMillis > this.style.distanceStateLifetimeMillis) {
            iterator.remove();
         }
      }
   }

   private static final class DistanceState {
      private int displayDistance;
      private float distanceSlotWidth;
      private float screenX;
      private float screenY;
      private long lastScreenUpdateMillis;
      private long lastSeenMillis;
      private boolean initialized;
      private boolean screenInitialized;

      private DistanceState() {
      }

      private int update(double targetDistance, long now, WayRenderer.Style style) {
         this.lastSeenMillis = now;
         int target = Math.max(0, (int)Math.floor(targetDistance));
         if (!this.initialized) {
            this.initialized = true;
            this.displayDistance = target;
            return target;
         } else {
            if (Math.abs(target - this.displayDistance) >= style.distanceSnapBlocks) {
               this.displayDistance = target;
            } else if (target > this.displayDistance && targetDistance >= (double)this.displayDistance + 1.0 + style.distanceHysteresis) {
               this.displayDistance = target;
            } else if (target < this.displayDistance && targetDistance <= (double)this.displayDistance - style.distanceHysteresis) {
               this.displayDistance = target;
            }

            return this.displayDistance;
         }
      }

      private float stableSlotWidth(float wantedWidth) {
         this.distanceSlotWidth = Math.max(this.distanceSlotWidth, wantedWidth);
         return this.distanceSlotWidth;
      }

      private WayRenderer.ScreenCenter updateScreen(float targetX, float targetY, long now, WayRenderer.Style style) {
         if (this.screenInitialized && Float.isFinite(this.screenX) && Float.isFinite(this.screenY)) {
            long elapsedMillis = Math.max(0L, now - this.lastScreenUpdateMillis);
            float dx = targetX - this.screenX;
            float dy = targetY - this.screenY;
            float distance = MathHelper.sqrt(dx * dx + dy * dy);
            if (elapsedMillis <= style.screenResyncMillis && !(distance >= style.screenSnapDistance)) {
               this.lastScreenUpdateMillis = now;
               double deltaSeconds = Math.min((double)elapsedMillis / 1000.0, 0.08);
               float factor = (float)(1.0 - Math.exp(-Math.max(1.0, style.screenSmoothingSpeed) * deltaSeconds));
               this.screenX += dx * factor;
               this.screenY += dy * factor;
               return new WayRenderer.ScreenCenter(this.screenX, this.screenY);
            } else {
               return this.snapScreen(targetX, targetY, now);
            }
         } else {
            return this.snapScreen(targetX, targetY, now);
         }
      }

      private WayRenderer.ScreenCenter snapScreen(float targetX, float targetY, long now) {
         this.screenInitialized = true;
         this.screenX = targetX;
         this.screenY = targetY;
         this.lastScreenUpdateMillis = now;
         return new WayRenderer.ScreenCenter(this.screenX, this.screenY);
      }
   }

   private static record RenderEntry(
      String nameText,
      String separatorText,
      String distanceText,
      float x,
      float y,
      float centerX,
      float width,
      float height,
      float radius,
      float accentRadius,
      float accentY,
      float nameWidth,
      float separatorWidth,
      float distanceWidth,
      float distanceSlotWidth
   ) {
   }

   private static record ScreenCenter(float x, float y) {
   }

   public static final class Style {
      public float textSize = 7.0F;
      public float paddingX = 5.0F;
      public float height = 12.0F;
      public float screenPadding = 4.0F;
      public float radius = 2.0F;
      public float blurRadius = 8.0F;
      public float outlineWidth = 0.4F;
      public float textOffsetY = 1.5F;
      public float accentOffsetX = -2.0F;
      public float accentOffsetY = 3.0F;
      public float accentSize = 6.0F;
      public float accentRadius = 12.0F;
      public float offscreenMargin = 4.0F;
      public float bobAmplitude = 3.0F;
      public double bobDuration = 1.15;
      public double screenSmoothingSpeed = 32.0;
      public float screenSnapDistance = 85.0F;
      public long screenResyncMillis = 120L;
      public double distanceHysteresis = 0.12;
      public int distanceSnapBlocks = 8;
      public int minDistanceIntegerDigits = 5;
      public long distanceStateLifetimeMillis = 10000L;
      public boolean bobEnabled = true;
      public boolean snapLabelToPixel = true;
      public boolean snapTextToPixel = true;
      public int textStrengthPasses = 2;
      public int backgroundColor = ColorUtil.rgba(13, 13, 13, 150);
      public int outlineColor = ColorUtil.rgba(128, 128, 128, 90);
      public int accentColor = ColorUtil.rgba(127, 242, 255, 230);
      public int textColor = ColorUtil.rgba(255, 255, 255, 255);
      public int blurColor = ColorUtil.rgba(255, 255, 255, 255);

      public Style() {
      }
   }

   private static record WayKey(String name, BlockPos pos, String server) {
      private static WayRenderer.WayKey of(Way way) {
         return new WayRenderer.WayKey(way.name(), way.pos(), way.server());
      }
   }
}
