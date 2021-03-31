package OxyEngine.Core.Renderer.Shader;

import OxyEngine.Core.Camera.OxyCamera;
import OxyEngine.System.OxyDisposable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class OxyShader implements OxyDisposable {

    public static final int VERTICES = 0;
    public static final int TEXTURE_COORDS = 1;
    public static final int NORMALS = 2;
    public static final int BITANGENT = 3;
    public static final int TANGENT = 4;
    public static final int OBJECT_ID = 5;
    public static final int BONEIDS = 6;
    public static final int WEIGHTS = 7;

    private final Map<String, ? super Number> uniformLocations = new HashMap<>();

    private static final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

    private final int program;
    private final String name;

    public static OxyShader createShader(String name, String glslPath){
        ShaderLibrary.removeShaderIfExist(name);
        OxyShader s = new OxyShader(name, glslPath);
        ShaderLibrary.addShaders(s);
        return s;
    }

    private OxyShader(String name, String glslPath) {
        this.name = name;
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

    public void setUniformVec4(String vecName, float x, float y, float z, float w) {
        if (!uniformLocations.containsKey(vecName)) {
            uniformLocations.put(vecName, glGetUniformLocation(program, vecName));
        }
        glUniform4f((Integer) uniformLocations.get(vecName), x, y, z, w);
    }

    public void setUniformVec4(Vector4f vec, int location) {
        glUniform4f(location, vec.x, vec.y, vec.z, vec.w);
    }

    public void setUniformVec3(String vecName, float x, float y, float z) {
        if (!uniformLocations.containsKey(vecName)) {
            uniformLocations.put(vecName, glGetUniformLocation(program, vecName));
        }
        glUniform3f((Integer) uniformLocations.get(vecName), x, y, z);
    }

    public void setUniformVec3(String vecName, Vector3f vec) {
        if (!uniformLocations.containsKey(vecName)) {
            uniformLocations.put(vecName, glGetUniformLocation(program, vecName));
        }
        glUniform3f((Integer) uniformLocations.get(vecName), vec.x, vec.y, vec.z);
    }

    public void setUniformVec3(Vector3f vec, int location) {
        glUniform3f(location, vec.x, vec.y, vec.z);
    }

    public void setUniformMatrix4fv(String name, Matrix4f m, boolean transpose) {
        m.get(buffer);
        if (!uniformLocations.containsKey(name)) {
            uniformLocations.put(name, glGetUniformLocation(program, name));
        }
        glUniformMatrix4fv((Integer) uniformLocations.get(name), transpose, buffer);
    }

    public void setUniformMatrix3fv(String name, Matrix3f m, boolean transpose) {
        m.get(buffer);
        if (!uniformLocations.containsKey(name)) {
            uniformLocations.put(name, glGetUniformLocation(program, name));
        }
        glUniformMatrix3fv((Integer) uniformLocations.get(name), transpose, buffer);
    }

    public void setCamera(OxyCamera camera) {
        setUniformMatrix4fv("pr_Matrix", camera.getProjectionMatrix(), camera.isTranspose());
        setUniformMatrix4fv("m_Matrix", camera.getModelMatrix(), camera.isTranspose());
        setUniformMatrix4fv("v_Matrix", camera.getViewMatrix(), camera.isTranspose());
        setUniformMatrix4fv("v_Matrix_NoTransform", camera.getViewMatrixNoTranslation(), camera.isTranspose());
    }

    public int getProgram() {
        return program;
    }

    public String getName() {
        return name;
    }

    @Override
    public void dispose() {
        ShaderLibrary.removeShaderIfExist(name);
        buffer.clear();
        glDeleteProgram(program);
    }
}