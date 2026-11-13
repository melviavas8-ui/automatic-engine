package daamky.ua.utils.render.ui.image;

import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;

record ImageTexture(Identifier id, TextureSetup setup, boolean nearestFilter, int width, int height) {
   float drawWidth(float size) {
      return this.width > 0 && this.height > 0 && this.width < this.height ? size * ((float)this.width / (float)this.height) : size;
   }

   float drawHeight(float size) {
      return this.width > 0 && this.height > 0 && this.height < this.width ? size * ((float)this.height / (float)this.width) : size;
   }
}
