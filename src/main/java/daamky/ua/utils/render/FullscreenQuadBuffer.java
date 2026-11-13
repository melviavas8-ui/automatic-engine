package daamky.ua.utils.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.nio.ByteBuffer;

public final class FullscreenQuadBuffer {
   private static GpuBuffer buffer;

   private FullscreenQuadBuffer() {
   }

   public static GpuBuffer getOrCreate() {
      GpuDevice device = RenderSystem.tryGetDevice();
      if (device == null) {
         return null;
      } else if (buffer != null && !buffer.isClosed()) {
         return buffer;
      } else {
         ByteBuffer data = ByteBuffer.allocateDirect(4);
         data.putInt(0);
         data.flip();
         buffer = device.createBuffer(() -> "daamky:fullscreen_quad_dummy", 32, data);
         return buffer;
      }
   }

   public static void close() {
      if (buffer != null) {
         buffer.close();
         buffer = null;
      }
   }
}
