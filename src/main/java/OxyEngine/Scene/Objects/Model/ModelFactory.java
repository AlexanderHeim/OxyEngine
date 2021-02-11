package OxyEngine.Scene.Objects.Model;

import OxyEngine.Core.Renderer.Texture.ImageTexture;
import OxyEngine.Components.TransformComponent;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static OxyEngine.Globals.toPrimitiveInteger;

public class ModelFactory {

    private final List<Vector3f> verticesNonTransformed;
    private final List<Vector3f> normals;
    private final List<Vector3f> tangents;
    private final List<Vector3f> biTangents;
    private final List<int[]> faces;
    private final List<Vector2f> textureCoords;

    public ModelFactory(List<Vector3f> verticesNonTransformed, List<Vector2f> textureCoords, List<Vector3f> normals,
                        List<int[]> faces, List<Vector3f> tangents, List<Vector3f> biTangents) {
        this.verticesNonTransformed = verticesNonTransformed;
        this.textureCoords = textureCoords;
        this.biTangents = biTangents;
        this.tangents = tangents;
        this.normals = normals;
        this.faces = faces;
    }

    public void constructData(OxyModel e) {
        e.vertices = new float[verticesNonTransformed.size() * 5];
        e.normals = new float[verticesNonTransformed.size() * 3];
        e.tcs = new float[verticesNonTransformed.size() * 2];
        e.tangents = new float[tangents.size() * 3];
        e.biTangents = new float[biTangents.size() * 3];
        List<Integer> indicesArr = new ArrayList<>();

        OxyMaterial material = OxyMaterialPool.getMaterial(e);
        TransformComponent c = e.get(TransformComponent.class);

        int slot = 0;
        if(material != null){
            ImageTexture texture = material.albedoTexture;
            if (texture != null) slot = texture.getTextureSlot();
        }

        int vertPtr = 0;
        for (Vector3f v : verticesNonTransformed) {
            Vector4f transformed = new Vector4f(v, 1.0f).mul(c.transform);
            e.vertices[vertPtr++] = transformed.x;
            e.vertices[vertPtr++] = transformed.y;
            e.vertices[vertPtr++] = transformed.z;
            e.vertices[vertPtr++] = slot;
            e.vertices[vertPtr++] = e.getObjectId();
        }

        int nPtr = 0;
        for (Vector3f n : normals) {
            e.normals[nPtr++] = n.x;
            e.normals[nPtr++] = n.y;
            e.normals[nPtr++] = n.z;
        }

        for (int[] face : faces) {
            for (int i : face) {
                indicesArr.add(i);
            }
        }

        int tcsPtr = 0;
        for (Vector2f v : textureCoords) {
            e.tcs[tcsPtr++] = v.x;
            e.tcs[tcsPtr++] = v.y;
        }
        e.indices = toPrimitiveInteger(indicesArr);

        int tangentPtr = 0;
        for(Vector3f v : tangents){
            e.tangents[tangentPtr++] = v.x;
            e.tangents[tangentPtr++] = v.y;
            e.tangents[tangentPtr++] = v.z;
        }
        int biTangentPtr = 0;
        for(Vector3f v : biTangents){
            e.biTangents[biTangentPtr++] = v.x;
            e.biTangents[biTangentPtr++] = v.y;
            e.biTangents[biTangentPtr++] = v.z;
        }
    }

    public void updateData(OxyModel e) {
        OxyMaterial material = OxyMaterialPool.getMaterial(e);
        ImageTexture texture = material.albedoTexture;
        TransformComponent c = e.get(TransformComponent.class);

        int slot = 0;
        if (texture != null)
            slot = texture.getTextureSlot();

        int vertPtr = 0;
        for (Vector3f v : verticesNonTransformed) {
            Vector4f transformed = new Vector4f(v.x, v.y, v.z, 1.0f).mul(c.transform);
            e.vertices[vertPtr++] = transformed.x;
            e.vertices[vertPtr++] = transformed.y;
            e.vertices[vertPtr++] = transformed.z;
            e.vertices[vertPtr++] = slot;
            e.vertices[vertPtr++] = e.getObjectId();
        }
    }
}