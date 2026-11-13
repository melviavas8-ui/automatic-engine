package daamky.ua.utils.render.ui.font;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.awt.image.BufferedImage;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureSetup;

final class GlyphAtlasPage implements AutoCloseable {
   private static int nextId;
   private final int size;
   private final NativeImage image;
   private final NativeImageBackedTexture texture;
   private final TextureSetup textureSetup;
   private int cursorX = 1;
   private int cursorY = 1;
   private int rowHeight;
   private boolean dirty;

   GlyphAtlasPage(int size) {
      this.size = size;
      this.image = new NativeImage(size, size, true);
      this.texture = new NativeImageBackedTexture(() -> "daamky_font_atlas_" + nextId++, this.image);
      this.textureSetup = TextureSetup.of(this.texture.getGlTextureView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
   }

   TextureSetup textureSetup() {
      return this.textureSetup;
   }

   GlyphAtlasPage.Allocation allocate(int width, int height) {
      int paddedWidth = width + 2;
      int paddedHeight = height + 2;
      if (paddedWidth <= this.size && paddedHeight <= this.size) {
         if (this.cursorX + paddedWidth > this.size) {
            this.cursorX = 1;
            this.cursorY = this.cursorY + this.rowHeight;
            this.rowHeight = 0;
         }

         if (this.cursorY + paddedHeight > this.size) {
            return null;
         } else {
            int x = this.cursorX;
            int y = this.cursorY;
            this.cursorX += paddedWidth;
            this.rowHeight = Math.max(this.rowHeight, paddedHeight);
            return new GlyphAtlasPage.Allocation(
               this,
               x,
               y,
               (float)x / (float)this.size,
               (float)y / (float)this.size,
               (float)(x + width) / (float)this.size,
               (float)(y + height) / (float)this.size
            );
         }
      } else {
         return null;
      }
   }

   void copy(BufferedImage source, int atlasX, int atlasY) {
      int width = source.getWidth();
      int height = source.getHeight();

      for (int y = 0; y < height; y++) {
         for (int x = 0; x < width; x++) {
            this.image.setColor(atlasX + x, atlasY + y, argbToAbgr(source.getRGB(x, y)));
         }
      }

      this.dirty = true;
   }

   void uploadIfDirty() {
      if (this.dirty) {
         this.texture.upload();
         this.dirty = false;
      }
   }

   private static int argbToAbgr(int argb) {
      int alpha = argb >>> 24 & 0xFF;
      int red = argb >>> 16 & 0xFF;
      int green = argb >>> 8 & 0xFF;
      int blue = argb & 0xFF;
      return alpha << 24 | blue << 16 | green << 8 | red;
   }

   @Override
   public void close() {
      this.texture.close();
   }

   static record Allocation(GlyphAtlasPage page, int x, int y, float u0, float v0, float u1, float v1) {
   }
}
