package OxyEngine.Core.Renderer.Texture;

import OxyEngine.Core.Renderer.Buffer.BufferTemplate;
import OxyEngine.Core.Renderer.Shader.OxyShader;
import OxyEngineEditor.Sandbox.OxyComponents.InternObjectMesh;
import OxyEngineEditor.Sandbox.Scene.InternObjects.OxyInternObject;
import OxyEngineEditor.Sandbox.Scene.Scene;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static OxyEngine.Core.Renderer.Texture.OxyTexture.allTextures;
import static OxyEngine.System.OxySystem.oxyAssert;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL12.GL_TEXTURE_WRAP_R;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP;
import static org.lwjgl.opengl.GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_set_flip_vertically_on_load;

public class CubemapTexture extends OxyTexture.Texture {

    private static final float[] skyboxVertices = {
            -1, 1, -1,
            -1, -1, -1,
            1, -1, -1,
            1, -1, -1,
            1, 1, -1,
            -1, 1, -1,

            -1, -1, 1,
            -1, -1, -1,
            -1, 1, -1,
            -1, 1, -1,
            -1, 1, 1,
            -1, -1, 1,

            1, -1, -1,
            1, -1, 1,
            1, 1, 1,
            1, 1, 1,
            1, 1, -1,
            1, -1, -1,

            -1, -1, 1,
            -1, 1, 1,
            1, 1, 1,
            1, 1, 1,
            1, -1, 1,
            -1, -1, 1,

            -1, 1, -1,
            1, 1, -1,
            1, 1, 1,
            1, 1, 1,
            -1, 1, 1,
            -1, 1, -1,

            -1, -1, -1,
            -1, -1, 1,
            1, -1, -1,
            1, -1, -1,
            -1, -1, 1,
            1, -1, 1
    };

    private final Scene scene;
    private static final List<String> fileStructure = Arrays.asList("right", "left", "top", "bottom", "back", "front");
    private static final List<String> totalFiles = new ArrayList<>();

    CubemapTexture(int slot, String path, Scene scene) {
        this.textureSlot = slot;
        this.path = path;
        this.scene = scene;

        assert slot != 0 : oxyAssert("Slot can not be 0");

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, textureId);

        File[] files = Objects.requireNonNull(new File(path).listFiles());
        for (String structureName : fileStructure) {
            for (File f : files) {
                String name = f.getName().split("\\.")[0];
                if (structureName.equals(name)) {
                    totalFiles.add(f.getPath());
                }
            }
        }

        assert totalFiles.size() == 6 : oxyAssert("Cubemap directory needs to only have the texture files. Directory length: " + files.length);
        stbi_set_flip_vertically_on_load(true);
        for (int i = 0; i < totalFiles.size(); i++) {
            int[] width = new int[1];
            int[] height = new int[1];
            int[] channels = new int[1];
            ByteBuffer buffer = loadTextureFile(totalFiles.get(i), width, height, channels);
            int internalFormat = GL_RGBA;
            if (channels[0] == 3)
                internalFormat = GL_RGB;
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, internalFormat, width[0], height[0], 0, internalFormat, GL_UNSIGNED_BYTE, buffer);
            stbi_image_free(buffer);
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

        allTextures.add(this);
    }

    public void init() {
        OxyShader shader = new OxyShader("shaders/skybox.glsl");
        shader.enable();
        shader.setUniform1i("skyBoxTexture", textureSlot);
        shader.disable();

        BufferTemplate.Attributes attributesVert = new BufferTemplate.Attributes(OxyShader.VERTICES, 3, GL_FLOAT, false, 0, 0);

        InternObjectMesh mesh = new InternObjectMesh.InternMeshBuilderImpl()
                .setShader(shader)
                .setMode(GL_TRIANGLES)
                .setUsage(BufferTemplate.Usage.STATIC)
                .setVerticesBufferAttributes(attributesVert)
                .create();

        OxyInternObject cube = scene.createInternObjectEntity();
        cube.vertices = skyboxVertices;
        int[] indices = new int[skyboxVertices.length];
        for (int i = 0; i < skyboxVertices.length; i++) {
            indices[i] = i;
        }
        cube.indices = indices;
        cube.addComponent(mesh);
        mesh.initList();
    }
}