package daamky.ua.api.drag.core;

public final class ElementComponent {
   private static final float DEFAULT_MIN_WIDTH = 12.0F;
   private static final float DEFAULT_MIN_HEIGHT = 12.0F;
   private static final float SCREEN_MARGIN = 4.0F;
   private static final float SNAP_DISTANCE = 6.0F;
   private static final float POSITION_SMOOTHING = 18.0F;
   private final String id;
   private final String title;
   private final float defaultX;
   private final float defaultY;
   private float targetX;
   private float targetY;
   private float x;
   private float y;
   private float width = 12.0F;
   private float height = 12.0F;
   private float minWidth = 12.0F;
   private float minHeight = 12.0F;
   private float hitExpandLeft;
   private float hitExpandTop;
   private float hitExpandRight;
   private float hitExpandBottom;
   private boolean visible = true;
   private boolean locked;
   private boolean moving;
   private float pointerOffsetX;
   private float pointerOffsetY;
   private float pointerX;
   private float pointerY;
   private boolean pointerTracked;
   private long lastPositionFrameMs = System.currentTimeMillis();
   private int order;

   ElementComponent(String id, String title, float defaultX, float defaultY, int order) {
      this.id = id;
      this.title = title;
      this.defaultX = defaultX;
      this.defaultY = defaultY;
      this.targetX = defaultX;
      this.targetY = defaultY;
      this.x = defaultX;
      this.y = defaultY;
      this.order = order;
   }

   public String id() {
      return this.id;
   }

   public String title() {
      return this.title;
   }

   public float x() {
      return this.x;
   }

   public float y() {
      return this.y;
   }

   public float targetX() {
      return this.targetX;
   }

   public float targetY() {
      return this.targetY;
   }

   public float width() {
      return this.width;
   }

   public float height() {
      return this.height;
   }

   public boolean visible() {
      return this.visible;
   }

   public boolean locked() {
      return this.locked;
   }

   public boolean moving() {
      return this.moving;
   }

   public int order() {
      return this.order;
   }

   public ElementBounds bounds() {
      return new ElementBounds(this.x, this.y, this.width, this.height);
   }

   public ElementBounds dragBounds() {
      return new ElementBounds(
         this.x - this.hitExpandLeft,
         this.y - this.hitExpandTop,
         this.width + this.hitExpandLeft + this.hitExpandRight,
         this.height + this.hitExpandTop + this.hitExpandBottom
      );
   }

   ElementBounds targetDragBounds() {
      return this.dragBoundsAt(this.targetX, this.targetY);
   }

   ElementBounds dragBoundsAt(float x, float y) {
      return new ElementBounds(
         x - this.hitExpandLeft,
         y - this.hitExpandTop,
         this.width + this.hitExpandLeft + this.hitExpandRight,
         this.height + this.hitExpandTop + this.hitExpandBottom
      );
   }

   public ElementComponent size(float width, float height) {
      if (Float.isFinite(width)) {
         this.width = Math.max(this.minWidth, width);
      }

      if (Float.isFinite(height)) {
         this.height = Math.max(this.minHeight, height);
      }

      return this;
   }

   public ElementComponent minimumSize(float minWidth, float minHeight) {
      if (Float.isFinite(minWidth)) {
         this.minWidth = Math.max(1.0F, minWidth);
      }

      if (Float.isFinite(minHeight)) {
         this.minHeight = Math.max(1.0F, minHeight);
      }

      return this.size(this.width, this.height);
   }

   public ElementComponent hitExpansion(float left, float top, float right, float bottom) {
      this.hitExpandLeft = safeExpansion(left);
      this.hitExpandTop = safeExpansion(top);
      this.hitExpandRight = safeExpansion(right);
      this.hitExpandBottom = safeExpansion(bottom);
      return this;
   }

   public ElementComponent position(float x, float y) {
      if (Float.isFinite(x)) {
         this.targetX = x;
         this.x = x;
      }

      if (Float.isFinite(y)) {
         this.targetY = y;
         this.y = y;
      }

      return this;
   }

   public ElementComponent visible(boolean visible) {
      this.visible = visible;
      return this;
   }

   public ElementComponent locked(boolean locked) {
      this.locked = locked;
      if (locked) {
         this.moving = false;
      }

      return this;
   }

   public ElementComponent resetPosition() {
      return this.position(this.defaultX, this.defaultY);
   }

   void order(int order) {
      this.order = order;
   }

   boolean hit(float mouseX, float mouseY, float padding) {
      return this.visible && !this.locked && this.dragBounds().contains(mouseX, mouseY, padding);
   }

   void beginMove(float mouseX, float mouseY) {
      this.moving = true;
      this.pointerTracked = true;
      this.pointerX = mouseX;
      this.pointerY = mouseY;
      this.pointerOffsetX = mouseX - this.x;
      this.pointerOffsetY = mouseY - this.y;
   }

   void moveTo(float mouseX, float mouseY, ElementScreen screen) {
      if (this.moving) {
         this.targetX = mouseX - this.pointerOffsetX;
         this.targetY = mouseY - this.pointerOffsetY;
         this.pointerTracked = true;
         this.pointerX = mouseX;
         this.pointerY = mouseY;
         this.x = this.targetX;
         this.y = this.targetY;
         this.snap(screen);
         this.commitClamp(screen);
      }
   }

   void endMove() {
      this.moving = false;
      this.pointerTracked = false;
   }

   void settleTo(float x, float y, ElementScreen screen) {
      if (Float.isFinite(x) && Float.isFinite(y)) {
         this.targetX = this.clampedX(screen, x);
         this.targetY = this.clampedY(screen, y);
      }
   }

   void resolveMoveTo(float x, float y, ElementScreen screen) {
      if (Float.isFinite(x) && Float.isFinite(y)) {
         this.targetX = this.clampedX(screen, x);
         this.targetY = this.clampedY(screen, y);
         this.x = this.targetX;
         this.y = this.targetY;
         if (this.moving && this.pointerTracked) {
            this.pointerOffsetX = this.pointerX - this.targetX;
            this.pointerOffsetY = this.pointerY - this.targetY;
         }
      }
   }

   void applyState(float x, float y, float width, float height, boolean visible, int order) {
      this.position(x, y);
      this.size(width, height);
      this.visible = visible;
      this.order = order;
   }

   public void clamp(ElementScreen screen) {
      if (screen != null && screen.valid()) {
         this.targetX = this.clampedX(screen, this.targetX);
         this.targetY = this.clampedY(screen, this.targetY);
         if (this.moving) {
            this.x = this.targetX;
            this.y = this.targetY;
            this.lastPositionFrameMs = System.currentTimeMillis();
         } else {
            float delta = this.deltaSeconds();
            this.x = smooth(this.x, this.targetX, delta);
            this.y = smooth(this.y, this.targetY, delta);
            if (Math.abs(this.x - this.targetX) < 0.05F) {
               this.x = this.targetX;
            }

            if (Math.abs(this.y - this.targetY) < 0.05F) {
               this.y = this.targetY;
            }
         }
      }
   }

   public void commitClamp(ElementScreen screen) {
      if (screen != null && screen.valid()) {
         this.targetX = this.clampedX(screen, this.targetX);
         this.targetY = this.clampedY(screen, this.targetY);
         this.x = this.targetX;
         this.y = this.targetY;
      }
   }

   private void snap(ElementScreen screen) {
      if (screen != null && screen.valid()) {
         float right = screen.width() - this.width - this.hitExpandRight - 4.0F;
         float bottom = screen.height() - this.height - this.hitExpandBottom - 4.0F;
         float centerX = (screen.width() - this.width - this.hitExpandRight + this.hitExpandLeft) * 0.5F;
         float centerY = (screen.height() - this.height - this.hitExpandBottom + this.hitExpandTop) * 0.5F;
         if (Math.abs(this.targetX - (4.0F + this.hitExpandLeft)) <= 6.0F) {
            this.targetX = 4.0F + this.hitExpandLeft;
         } else if (Math.abs(this.targetX - right) <= 6.0F) {
            this.targetX = right;
         } else if (Math.abs(this.targetX - centerX) <= 6.0F) {
            this.targetX = centerX;
         }

         if (Math.abs(this.targetY - (4.0F + this.hitExpandTop)) <= 6.0F) {
            this.targetY = 4.0F + this.hitExpandTop;
         } else if (Math.abs(this.targetY - bottom) <= 6.0F) {
            this.targetY = bottom;
         } else if (Math.abs(this.targetY - centerY) <= 6.0F) {
            this.targetY = centerY;
         }
      }
   }

   private float clampedX(ElementScreen screen, float value) {
      return clamp(value, 4.0F + this.hitExpandLeft, Math.max(4.0F + this.hitExpandLeft, screen.width() - this.width - this.hitExpandRight - 4.0F));
   }

   private float clampedY(ElementScreen screen, float value) {
      return clamp(value, 4.0F + this.hitExpandTop, Math.max(4.0F + this.hitExpandTop, screen.height() - this.height - this.hitExpandBottom - 4.0F));
   }

   float clampTargetX(ElementScreen screen, float value) {
      return this.clampedX(screen, value);
   }

   float clampTargetY(ElementScreen screen, float value) {
      return this.clampedY(screen, value);
   }

   private float deltaSeconds() {
      long now = System.currentTimeMillis();
      float delta = Math.min(0.05F, Math.max(0.001F, (float)(now - this.lastPositionFrameMs) / 1000.0F));
      this.lastPositionFrameMs = now;
      return delta;
   }

   private static float smooth(float current, float target, float delta) {
      float factor = 1.0F - (float)Math.exp((double)(-18.0F * delta));
      return current + (target - current) * factor;
   }

   private static float safeExpansion(float value) {
      return Float.isFinite(value) ? Math.max(0.0F, value) : 0.0F;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }
}
