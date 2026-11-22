package daamky.ua.newmenu.render.liqvid;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class ShaderBuilder {
    private final ShaderProgram program = new ShaderProgram();

    private ShaderBuilder() {
    }

    public static ShaderBuilder create() {
        return new ShaderBuilder();
    }

    public ShaderBuilder attach(String path, int type) {
        InputStream stream = ShaderBuilder.class.getResourceAsStream("/assets/lunaware/shaders/" + path);
        if (stream == null) {
            return this;
        }
        try (stream) {
            this.program.attachSource(new String(stream.readAllBytes(), StandardCharsets.UTF_8), type);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read shader: " + path, exception);
        }
        return this;
    }

    public ShaderBuilder link() {
        this.program.link();
        return this;
    }

    public ShaderProgram build() {
        return this.program;
    }
}

