package daamky.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.OptionalInt;


public final class SdfGpu {
    private SdfGpu() {}


    static void generate(FontAtlas f, String name, Map<Integer, float[]> glyphs) throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();

        byte[] data;
        try (InputStream in = mc.getResourceManager().open(Identifier.of("daamky", "fonts/" + name + "_seg.bin"))) {
            data = in.readAllBytes();
        }
        int texels = data.length / 4, tw = 1024, th = Math.max(1, (texels + tw - 1) / tw);
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, tw, th, true);
        for (int i = 0; i < texels; i++) {
            int r = data[4 * i] & 255, g = data[4 * i + 1] & 255, b = data[4 * i + 2] & 255, a = data[4 * i + 3] & 255;
            img.setColorArgb(i % tw, i / tw, a << 24 | r << 16 | g << 8 | b);
        }
        NativeImageBackedTexture segTex = new NativeImageBackedTexture(() -> "daamky " + name + " seg", img);
        f.segTexObj = segTex;
        GpuTextureView segView = segTex.getGlTextureView();

        GpuDevice dev = RenderSystem.getDevice();
        GpuTexture tex = dev.createTexture(() -> "daamky " + name + " sdf", 15, TextureFormat.RGBA8, f.w, f.h, 1, 1);
        GpuTextureView view = dev.createTextureView(tex);

        int quads = 0;
        for (float[] g : glyphs.values()) if ((int) g[3] != 0) quads++;
        if (quads == 0) return;

        GpuBuffer vb, ib;
        try (BufferAllocator alloc = new BufferAllocator(quads * 4 * Gfx.FORMAT.getVertexSize() + 64)) {
            BufferBuilder bb = new BufferBuilder(alloc, VertexFormat.DrawMode.QUADS, Gfx.FORMAT);
            int rng = Math.round(f.range * 16f);
            for (float[] g : glyphs.values()) {
                int idx = (int) g[0], start = (int) g[2], count = (int) g[3];
                if (count == 0) continue;
                int ox = (idx % f.cols) * f.cell, oy = (idx / f.cols) * f.cell;
                float x0 = ox / (float) f.w * 2f - 1f, x1 = (ox + f.cell) / (float) f.w * 2f - 1f;
                float y0 = oy / (float) f.h * 2f - 1f, y1 = (oy + f.cell) / (float) f.h * 2f - 1f;
                float[][] c = {{x0, y0}, {x1, y0}, {x1, y1}, {x0, y1}};
                for (float[] p : c) {
                    bb.vertex(p[0], p[1], (float) start).color(-1).texture((float) count, (float) ox)
                            .overlay(oy, f.pad).light(f.base, rng).normal(0f, 0f, 0f);
                }
            }
            try (BuiltBuffer built = bb.end()) {
                vb = Gfx.FORMAT.uploadImmediateVertexBuffer(built.getBuffer());
                ByteBuffer ibuf = MemoryUtil.memAlloc(quads * 6 * 4);
                try {
                    for (int q = 0; q < quads; q++) {
                        int b = q * 4;
                        ibuf.putInt(b).putInt(b + 1).putInt(b + 2).putInt(b + 2).putInt(b + 3).putInt(b);
                    }
                    ibuf.flip();
                    ib = Gfx.FORMAT.uploadImmediateIndexBuffer(ibuf);
                } finally { MemoryUtil.memFree(ibuf); }

                CommandEncoder enc = dev.createCommandEncoder();
                try (RenderPass pass = enc.createRenderPass(() -> "sdfgen", view, OptionalInt.of(0xFF000000))) {
                    pass.setPipeline(Gfx.SDFGEN);
                    pass.bindTexture("Sampler0", segView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                    pass.setVertexBuffer(0, vb);
                    pass.setIndexBuffer(ib, VertexFormat.IndexType.INT);
                    pass.drawIndexed(0, 0, quads * 6, 1);
                }
            }
        }
        f.sdfTex = tex;
        f.sdfView = view;
    }
}
