package daamky.ua.api.drag.core;

public record ElementBounds(float x, float y, float width, float height) {
   public float right() {
      return this.x + this.width;
   }

   public float bottom() {
      return this.y + this.height;
   }

   public boolean contains(float pointX, float pointY) {
      return this.contains(pointX, pointY, 0.0F);
   }

   public boolean contains(float pointX, float pointY, float padding) {
      return pointX >= this.x - padding && pointX <= this.x + this.width + padding && pointY >= this.y - padding && pointY <= this.y + this.height + padding;
   }
}
