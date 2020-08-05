package OxyEngine.Core.Renderer.Shader;

import OxyEngine.Core.Camera.OxyCamera;
import OxyEngine.System.OxyDisposable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class OxyShader implements OxyDisposable {

    public static final int VERTICES = 0;
    public static final int TEXTURE_COORDS = 1;
    public static final int TEXTURE_SLOT = 2;
    public static final int COLOR = 3;

    private final Map<String, ? super Number> uniformLocations = new HashMap<>();

    private static final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

    private final int program;

    public OxyShader(String glslPath) {
        String loadedString = ShaderUtil.loadAsString(glslPath);
        program = ShaderUtil.create(ShaderUtil.getVertex(loadedString).trim(), ShaderUtil.getFragment(loadedString).trim());
    }

    public void enable() {
        glUseProgram(program);
    }

    public void disable() {
        glUseProgram(0);
    }

    public void setUniform1i(String name, int value) {
        if (!uniformLocations.containsKey(name)) {
            uniformLocations.put(name, glGetUniformLocation(program, name));
        }
        glUniform1i((Integer) uniformLocations.get(name), value);
    }

    public void setUniform1iv(String name, int[] value) {
        if (!uniformLocations.containsKey(name)) {
            uniformLocations.put(name, glGetUniformLocation(program, name));
        }
        glUniform1iv((Integer) uniformLocations.get(name), value);
    }

    public void setUniform1f(String name, float value) {
        if (!uniformLocations.containsKey(name)) {
            uniformLocations.put(name, glGetUniformLocation(program, name));
        }
        glUniform1f((Integer) uniformLocations.get(name), value);
    }

    public void setUniformVec4(String vecName, Vector4f vec) {
        if (!uniformLocations.containsKey(vecName)) {
            uniformLocations.put(vecName, glGetUniformLocation(program, vecName));
        }
        glUniform4f((Integer) uniformLocations.get(vecName), vec.x, vec.y, vec.z, vec.w);
    }

    public void setUniformVec4(Vector4f vec, int location) {
        glUniform4f(location, vec.x, vec.y, vec.z, vec.w);
    }

    public void setUniformMatrix4fv(Matrix4f m, int location, boolean transpose) {
        m.get(buffer);
        glUniformMatrix4fv(location, transpose, buffer);
        buffer.clear();
    }

    public void setCamera(float ts, OxyCamera camera){
        camera.finalizeCamera(ts);
        setUniformMatrix4fv(camera.getViewMatrix(), camera.getLocation(), camera.isTranspose());
    }

    public Map<String, ? super Number> getUniformLocations() {
        return uniformLocations;
    }

    public int getProgram() {
        return program;
    }

    @Override
    public void dispose() {
        buffer.clear();
        glDeleteProgram(program);
    }
}
