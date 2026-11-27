package daamky.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

public final class FontAtlas {
    public final int cell, pad, base, cols, w, h;
    public final float scale, range, ascent, lineHeight;
    public final Map<Integer, float[]> glyphs = new HashMap<>();
    public GpuTextureView msdfView;
    public DynamicTexture msdfTexObj, segTexObj;

    public GpuTexture sdfTex;
    public GpuTextureView sdfView;

    private FontAtlas(String name, JsonObject o) throws Exception {
        cell = o.get("cell").getAsInt();
        pad = o.get("pad").getAsInt();
        base = o.get("base").getAsInt();
        cols = o.get("cols").getAsInt();
        w = o.get("w").getAsInt();
        h = o.get("h").getAsInt();
        scale = o.get("scale").getAsFloat();
        range = o.get("range").getAsFloat();
        ascent = o.get("ascent").getAsFloat();
        lineHeight = o.get("lineHeight").getAsFloat();
        for (var e : o.getAsJsonObject("glyphs").entrySet()) {
            var arr = e.getValue().getAsJsonArray();
            glyphs.put(Integer.parseInt(e.getKey()), new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(),
                    arr.get(2).getAsFloat(), arr.get(3).getAsFloat()});
        }
        tex(name + "_msdf");
        try {
            SdfGpu.generate(this, name, glyphs);
        } catch (Throwable t) {
            System.err.println("[daamky] GPU SDF generation failed, SDF mode falls back to MSDF");
            t.printStackTrace();
        }
    }

    private void tex(String file) throws Exception {
        var mc = Minecraft.getInstance();
        try (InputStream in = mc.getResourceManager().open(Identifier.fromNamespaceAndPath("daamky", "fonts/" + file + ".png"))) {
            msdfTexObj = new DynamicTexture(() -> "daamky " + file, NativeImage.read(in));
            msdfView = msdfTexObj.getTextureView();
        }
    }

    static FontAtlas load(String name) {
        try (var in = Minecraft.getInstance().getResourceManager()
                .open(Identifier.fromNamespaceAndPath("daamky", "fonts/" + name + ".json"))) {
            JsonObject o = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
            return new FontAtlas(name, o);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
