package daamky.ua.utils.render.ui;

import java.io.InputStream;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class GuiResourceLoader {
   private GuiResourceLoader() {
   }

   public static InputStream open(Identifier id) {
      return id == null ? null : GuiResourceLoader.class.getResourceAsStream(resourcePath(id));
   }

   public static boolean exists(Identifier id) {
      try {
         boolean var2;
         try (InputStream input = open(id)) {
            var2 = input != null;
         }

         return var2;
      } catch (Exception var6) {
         return false;
      }
   }

   public static NativeImageBackedTexture loadTexture(Identifier id, String debugPrefix) {
      try {
         NativeImageBackedTexture exception;
         try (InputStream input = open(id)) {
            if (input == null) {
               return null;
            }

            NativeImage image = NativeImage.read(input);
            NativeImageBackedTexture texture = null;

            try {
               texture = new NativeImageBackedTexture(() -> debugPrefix + id, image);
               texture.upload();
               exception = texture;
            } catch (RuntimeException var8) {
               if (texture != null) {
                  texture.close();
               } else {
                  image.close();
               }

               return null;
            }
         }

         return exception;
      } catch (Exception var10) {
         return null;
      }
   }

   private static String resourcePath(Identifier id) {
      return "/assets/" + id.getNamespace() + "/" + id.getPath();
   }
}
