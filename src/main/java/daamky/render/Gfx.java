package daamky.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;


public final class Gfx {
    public static final VertexFormat FORMAT = DefaultVertexFormat.NEW_ENTITY;

    public static GuiGraphics ctx;

    public static ScreenRectangle scissor;

    public static final RenderPipeline RECT = gui("rect", false);
    public static final RenderPipeline MSDF = gui("msdf", true);
    public static final RenderPipeline SDF = gui("sdf", true);
    public static final RenderPipeline SDFGEN = RenderPipeline.builder()
            .withLocation(Identifier.fromNamespaceAndPath("daamky", "pipeline/sdfgen"))
            .withVertexShader(Identifier.fromNamespaceAndPath("daamky", "core/sdfgen"))
            .withFragmentShader(Identifier.fromNamespaceAndPath("daamky", "core/sdfgen"))
            .withSampler("Sampler0")
            .withVertexFormat(FORMAT, VertexFormat.Mode.QUADS)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .build();

    private Gfx() {}

    private static RenderPipeline gui(String n, boolean sampler) {
        var b = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
                .withLocation(Identifier.fromNamespaceAndPath("daamky", "pipeline/" + n))
                .withVertexShader(Identifier.fromNamespaceAndPath("daamky", "core/" + n))
                .withFragmentShader(Identifier.fromNamespaceAndPath("daamky", "core/" + n))
                .withVertexFormat(FORMAT, VertexFormat.Mode.QUADS)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false);
        if (sampler) b = b.withSampler("Sampler0");
        return b.build();
    }


    public static final class Batch {
        private float[] d = new float[11 * 64];
        private int n;
        private float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        private final Matrix3x2f pose;
        private final Vector2f t = new Vector2f();

        public Batch(Matrix3x2f pose) { this.pose = pose; }

        public boolean isEmpty() { return n == 0; }

        public void v(float x, float y, float z, int color, float u, float v, int o1x, int o1y, int o2x, int o2y, float nx) {
            pose.transformPosition(x, y, t);
            if (d.length < (n + 1) * 11) d = java.util.Arrays.copyOf(d, d.length * 2);
            int i = n++ * 11;
            d[i] = t.x; d[i + 1] = t.y; d[i + 2] = z;
            d[i + 3] = Float.intBitsToFloat(color);
            d[i + 4] = u; d[i + 5] = v;
            d[i + 6] = o1x; d[i + 7] = o1y; d[i + 8] = o2x; d[i + 9] = o2y;
            d[i + 10] = nx;
            minX = Math.min(minX, t.x); maxX = Math.max(maxX, t.x);
            minY = Math.min(minY, t.y); maxY = Math.max(maxY, t.y);
        }

        public void submit(RenderPipeline pl, TextureSetup ts) {
            if (n == 0 || ctx == null) return;
            int x = (int) Math.floor(minX), y = (int) Math.floor(minY);
            ScreenRectangle b = new ScreenRectangle(x, y, (int) Math.ceil(maxX) - x + 1, (int) Math.ceil(maxY) - y + 1);
            ScreenRectangle sc = scissor;
            if (sc != null) {
                b = b.intersection(sc);
                if (b == null) return;
            }
            ctx.guiRenderState.submitGuiElement(new State(pl, ts, sc, b, java.util.Arrays.copyOf(d, n * 11), n));
        }
    }

    private record State(RenderPipeline pipeline, TextureSetup textureSetup, ScreenRectangle scissorArea, ScreenRectangle bounds,
                         float[] d, int n) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer vc) {
            for (int k = 0; k < n; k++) {
                int i = k * 11;
                vc.addVertex(d[i], d[i + 1], d[i + 2])
                        .setColor(Float.floatToRawIntBits(d[i + 3]))
                        .setUv(d[i + 4], d[i + 5])
                        .setUv1((int) d[i + 6], (int) d[i + 7])
                        .setUv2((int) d[i + 8], (int) d[i + 9])
                        .setNormal(d[i + 10], 0f, 0f);
            }
        }
    }
}
