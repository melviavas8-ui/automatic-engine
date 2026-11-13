package daamky.ua.utils.render.ui.arc;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import daamky.ua.mixin.accessor.GuiGraphicsExtractorAccessor;
import daamky.ua.utils.render.ScissorUtil;
import daamky.ua.utils.render.ui.Render2DCoordinateSpace;

public final class ArcRenderer implements AutoCloseable {
   private static final int MAX_ARCS = 256;
   private static final int PARAMS_PER_ARC = 10;
   private static final int FLOATS_PER_PARAM = 4;
   private static final int UNIFORM_BYTES = 40960;
   private static volatile ArcRenderer instance;
   public static final RenderPipeline ARC_PIPELINE = RenderPipeline.builder(new Snippet[0])
      .withLocation(id("pipeline/arc"))
      .withVertexShader(id("ui/shared/outline_quad"))
      .withFragmentShader(id("ui/arc/arc"))
      .withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, DrawMode.QUADS)
      .withBlend(BlendFunction.TRANSLUCENT)
      .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
      .withCull(false)
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .withUniform("ArcParamsArray", UniformType.UNIFORM_BUFFER)
      .build();
   private final List<BuiltArc> preparedArcs = new ArrayList<>(64);
   private DrawContext activeGraphics;
   private GpuBuffer paramsBuffer;
   private boolean paramsDirty = true;

   private ArcRenderer() {
   }

   public static ArcRenderer getInstance() {
      ArcRenderer local = instance;
      if (local == null) {
         synchronized (ArcRenderer.class) {
            local = instance;
            if (local == null) {
               local = new ArcRenderer();
               instance = local;
            }
         }
      }

      return local;
   }

   public static void closeInstance() {
      ArcRenderer local = instance;
      if (local != null) {
         local.close();
         instance = null;
      }
   }

   public void beginFrame(DrawContext graphics) {
      this.activeGraphics = graphics;
   }

   public void draw(DrawContext graphics, BuiltArc arc) {
      this.beginFrame(graphics);
      this.enqueue(arc);
      this.flush();
   }

   public void enqueue(BuiltArc arc) {
      this.submit(this.activeGraphics, arc);
   }

   public void flush() {
      this.activeGraphics = null;
   }

   public void beginGuiFrame() {
      this.preparedArcs.clear();
      this.paramsDirty = false;
   }

   public boolean isArcPipeline(RenderPipeline pipeline) {
      return pipeline == ARC_PIPELINE;
   }

   public void bindParams(RenderPass renderPass) {
      if (renderPass != null && !this.preparedArcs.isEmpty()) {
         GpuBuffer buffer = this.ensureParamsBuffer();
         if (buffer != null) {
            renderPass.setUniform("ArcParamsArray", buffer);
         }
      }
   }

   public void prepareBuffers() {
      if (!this.preparedArcs.isEmpty() && this.paramsDirty) {
         GpuBuffer buffer = this.ensureWritableParamsBuffer();
         if (buffer != null) {
            try {
               MemoryStack stack = MemoryStack.stackPush();

               try {
                  ByteBuffer uniformData = this.buildUniformData(stack, this.preparedArcs);
                  RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(0L, (long)uniformData.remaining()), uniformData);
                  this.paramsDirty = false;
               } catch (Throwable var6) {
                  if (stack != null) {
                     try {
                        stack.close();
                     } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                     }
                  }

                  throw var6;
               }

               if (stack != null) {
                  stack.close();
               }
            } catch (RuntimeException var7) {
               this.paramsDirty = true;
            }
         }
      }
   }

   int reserve(BuiltArc arc) {
      int index = this.preparedArcs.size();
      if (index == 256) {
         return -1;
      } else {
         this.preparedArcs.add(arc);
         this.paramsDirty = true;
         return index;
      }
   }

   private void submit(DrawContext graphics, BuiltArc arc) {
      if (graphics != null && arc != null && arc.visible()) {
         try {
            BuiltArc normalized = this.normalize(arc);
            Matrix3x2f pose = Render2DCoordinateSpace.pose(graphics);
            ((GuiGraphicsExtractorAccessor)graphics).daamky$getGuiRenderState().addSimpleElement(new ArcRenderState(pose, normalized, ScissorUtil.current()));
         } catch (RuntimeException var5) {
         }
      }
   }

   private GpuBuffer ensureParamsBuffer() {
      if (!this.paramsDirty && this.paramsBuffer != null) {
         return this.paramsBuffer;
      } else {
         this.prepareBuffers();
         if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
         } else {
            this.closeParamsBuffer();
            MemoryStack stack = MemoryStack.stackPush();

            GpuBuffer var3;
            try {
               ByteBuffer uniformData = this.buildUniformData(stack, this.preparedArcs);
               this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_arc_params", 128, uniformData);
               this.paramsDirty = false;
               var3 = this.paramsBuffer;
            } catch (Throwable var5) {
               if (stack != null) {
                  try {
                     stack.close();
                  } catch (Throwable var4) {
                     var5.addSuppressed(var4);
                  }
               }

               throw var5;
            }

            if (stack != null) {
               stack.close();
            }

            return var3;
         }
      }
   }

   private GpuBuffer ensureWritableParamsBuffer() {
      if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 40960L) {
         return this.paramsBuffer;
      } else {
         this.closeParamsBuffer();

         try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "daamky_arc_params", 136, 40960L);
            return this.paramsBuffer;
         } catch (RuntimeException var2) {
            return null;
         }
      }
   }

   private ByteBuffer buildUniformData(MemoryStack stack, List<BuiltArc> batch) {
      int usedBytes = Math.max(1, batch.size()) * 10 * 4 * 4;
      ByteBuffer data = stack.calloc(usedBytes);

      for (int i = 0; i < batch.size(); i++) {
         BuiltArc arc = batch.get(i);
         int offset = i * 10 * 4 * 4;
         data.putFloat(offset, arc.size());
         data.putFloat(offset + 4, arc.thickness());
         data.putFloat(offset + 8, arc.degree());
         data.putFloat(offset + 12, arc.rotation());
         int[] colors = arc.colors();

         for (int colorIndex = 0; colorIndex < 9; colorIndex++) {
            this.putColor(data, offset + (colorIndex + 1) * 16, colors[colorIndex]);
         }
      }

      data.position(0);
      return data;
   }

   private BuiltArc normalize(BuiltArc arc) {
      float size = Math.max(0.0F, arc.size());
      float thickness = Math.max(0.0F, Math.min(arc.thickness(), size));
      float degree = Math.max(0.0F, Math.min(arc.degree(), 360.0F));
      return new BuiltArc(arc.x(), arc.y(), size, thickness, degree, arc.rotation(), arc.colors());
   }

   private void putColor(ByteBuffer data, int offset, int color) {
      data.putFloat(offset, (float)(color >>> 16 & 0xFF) / 255.0F);
      data.putFloat(offset + 4, (float)(color >>> 8 & 0xFF) / 255.0F);
      data.putFloat(offset + 8, (float)(color & 0xFF) / 255.0F);
      data.putFloat(offset + 12, (float)(color >>> 24 & 0xFF) / 255.0F);
   }

   private static Identifier id(String path) {
      return Identifier.of("daamky", path);
   }

   @Override
   public void close() {
      this.closeParamsBuffer();
   }

   private void closeParamsBuffer() {
      if (this.paramsBuffer != null) {
         this.paramsBuffer.close();
         this.paramsBuffer = null;
      }
   }
}
