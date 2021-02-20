package OxyEngine.Core.Layers;

import OxyEngine.Components.*;
import OxyEngine.Core.Camera.OxyCamera;
import OxyEngine.Core.Camera.PerspectiveCamera;
import OxyEngine.Core.Renderer.Buffer.OpenGLMesh;
import OxyEngine.Core.Renderer.Buffer.Platform.OpenGLFrameBuffer;
import OxyEngine.Core.Renderer.Light.Light;
import OxyEngine.Core.Renderer.Mesh.ModelMeshOpenGL;
import OxyEngine.Core.Renderer.Mesh.NativeObjectMeshOpenGL;
import OxyEngine.Core.Renderer.OxyRenderer;
import OxyEngine.Core.Renderer.Shader.OxyShader;
import OxyEngine.Core.Renderer.Texture.HDRTexture;
import OxyEngine.Core.Renderer.Texture.OxyTexture;
import OxyEngine.Scene.Objects.Model.OxyMaterial;
import OxyEngine.Scene.Objects.Model.OxyMaterialPool;
import OxyEngine.Scene.Objects.Model.OxyModel;
import OxyEngine.Scene.Objects.Native.OxyNativeObject;
import OxyEngine.Scene.OxyEntity;
import OxyEngine.Scene.Scene;
import OxyEngine.Scene.SceneRuntime;
import OxyEngine.Scene.SceneState;
import OxyEngineEditor.UI.Panels.EnvironmentPanel;
import OxyEngineEditor.UI.Panels.GUINode;
import OxyEngineEditor.UI.Panels.ScenePanel;
import org.joml.Vector2f;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static OxyEngine.Core.Renderer.Context.OxyRenderCommand.rendererAPI;
import static OxyEngine.Core.Renderer.Light.Light.LIGHT_SIZE;
import static OxyEngine.Scene.SceneRuntime.ACTIVE_SCENE;
import static OxyEngineEditor.EditorApplication.editorCameraEntity;
import static OxyEngineEditor.EditorApplication.oxyShader;
import static OxyEngineEditor.UI.Gizmo.OxySelectHandler.entityContext;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_TEXTURE_CUBE_MAP_SEAMLESS;
import static org.lwjgl.opengl.GL44.glClearTexImage;

public class SceneLayer extends Layer {

    public Set<OxyEntity> cachedLightEntities, cachedNativeMeshes;
    public Set<OxyEntity> cachedCameraComponents;
    public Set<OxyEntity> allModelEntities;

    private static final OxyShader outlineShader = new OxyShader("shaders/OxyOutline.glsl");
    private static final OxyShader cubemapShader = new OxyShader("shaders/OxySkybox.glsl");
    public static HDRTexture hdrTexture;

    private OxyCamera mainCamera;

    private static SceneLayer INSTANCE = null;

    public static SceneLayer getInstance() {
        if (INSTANCE == null) INSTANCE = new SceneLayer();
        return INSTANCE;
    }

    private SceneLayer() {
    }

    @Override
    public void build() {
        cachedNativeMeshes = ACTIVE_SCENE.view(NativeObjectMeshOpenGL.class);
        for (OxyEntity e : cachedNativeMeshes) {
            e.get(NativeObjectMeshOpenGL.class).addToQueue();
        }

        cachedCameraComponents = ACTIVE_SCENE.view(OxyCamera.class);
        updateAllEntities();

        fillPropertyEntries();
    }

    @Override
    public void rebuild() {
        updateAllEntities();

        //Prep
        {
            List<OxyEntity> cachedConverted = new ArrayList<>(allModelEntities);
            if (cachedConverted.size() == 0) return;
            ModelMeshOpenGL mesh = cachedConverted.get(cachedConverted.size() - 1).get(ModelMeshOpenGL.class);
            mesh.addToQueue();
        }
    }

    private void fillPropertyEntries() {
        for (OxyEntity entity : SceneLayer.getInstance().allModelEntities) {
            if (entity instanceof OxyNativeObject) continue;
            if (((OxyModel) entity).factory == null) continue;
            for (Class<? extends EntityComponent> component : EntityComponent.allEntityComponentChildClasses) {
                if (component == null) continue;
                if (!entity.has(component)) continue;
                //overhead, i know. getDeclaredField() method throw an exception if the given field isn't declared... => no checking for the field
                //so you have to manually do the checking by getting all the fields that the class has.
                Field[] allFields = component.getDeclaredFields();
                Field guiNodeField = null;
                for (Field f : allFields) {
                    if (f.getName().equals("guiNode")) {
                        f.setAccessible(true);
                        guiNodeField = f;
                        break;
                    }
                }
                if (guiNodeField == null) continue;
                try {
                    GUINode entry = (GUINode) guiNodeField.get(component);
                    if (!entity.getGUINodes().contains(entry)) entity.getGUINodes().add(entry);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void updateAllEntities() {
        allModelEntities = ACTIVE_SCENE.view(ModelMeshOpenGL.class);
        cachedLightEntities = ACTIVE_SCENE.view(Light.class);
    }

    public void updateCameraEntities() {
        cachedCameraComponents = ACTIVE_SCENE.view(OxyCamera.class);
    }

    @Override
    public void update(float ts) {
        if (ACTIVE_SCENE == null) return;
        SceneRuntime.onUpdate(ts);

        //Camera
        {
            for (OxyEntity e : cachedCameraComponents) {
                OxyCamera camera = e.get(OxyCamera.class);
                OxyCamera editorCamera = editorCameraEntity.get(OxyCamera.class);
                if (ACTIVE_SCENE.STATE == SceneState.RUNNING && !camera.equals(editorCamera)) {
                    editorCamera.setPrimary(false);
                    camera.setPrimary(true);
                    mainCamera = camera;
                    SceneRuntime.currentBoundedCamera = mainCamera;
                } else if (ACTIVE_SCENE.STATE != SceneState.RUNNING) {
                    if (mainCamera != null) mainCamera.setPrimary(false);
                    editorCamera.setPrimary(true);
                    mainCamera = editorCamera;
                    SceneRuntime.currentBoundedCamera = mainCamera;
                }
            }
        }

        if (mainCamera != null) {
            mainCamera.finalizeCamera(ts);
            if (mainCamera instanceof PerspectiveCamera c) c.setViewMatrixNoTranslation();
        }

        //RESET ALL THE LIGHT STATES
        for (int i = 0; i < LIGHT_SIZE; i++) {
            oxyShader.enable();
            oxyShader.setUniform1i("p_Light[" + i + "].activeState", 0);
            oxyShader.setUniform1i("d_Light[" + i + "].activeState", 0);
            oxyShader.disable();
        }

        //Lights
        int i = 0;
        for (OxyEntity e : cachedLightEntities) {
            Light l = e.get(Light.class);
            l.update(e, i++);
        }
    }


    @Override
    public void render(float ts) {
        if (ACTIVE_SCENE == null) return;

        OpenGLFrameBuffer frameBuffer = ACTIVE_SCENE.getFrameBuffer();
        OpenGLFrameBuffer destFrameBuffer = ACTIVE_SCENE.getBlittedFrameBuffer();

        OpenGLFrameBuffer.blit(frameBuffer, destFrameBuffer);
        frameBuffer.bind();
        rendererAPI.clearBuffer();
        rendererAPI.clearColor(32, 32, 32, 1.0f);

        //Rendering
        {
            for (OxyEntity e : allModelEntities) {
                if (!e.has(SelectedComponent.class)) continue;
                RenderableComponent renderableComponent = e.get(RenderableComponent.class);
                if (renderableComponent.mode != RenderingMode.Normal) continue;
                ModelMeshOpenGL modelMesh = e.get(ModelMeshOpenGL.class);
                OxyMaterial material = OxyMaterialPool.getMaterial(e);
                TransformComponent c = e.get(TransformComponent.class);
                OxyShader shader = e.get(OxyShader.class);
                shader.enable();
                shader.setUniformMatrix4fv("model", c.transform, false);
                int irradianceSlot = 0, prefilterSlot = 0, brdfLUTSlot = 0;
                if (hdrTexture != null) {
                    irradianceSlot = hdrTexture.getIrradianceSlot();
                    prefilterSlot = hdrTexture.getPrefilterSlot();
                    brdfLUTSlot = hdrTexture.getBDRFSlot();
                    hdrTexture.bindAll();
                }
                shader.setUniform1i("irradianceMap", irradianceSlot);
                shader.setUniform1i("prefilterMap", prefilterSlot);
                shader.setUniform1i("brdfLUT", brdfLUTSlot);
                shader.setUniform1f("gamma", EnvironmentPanel.gammaStrength[0]);
                shader.setUniform1f("exposure", EnvironmentPanel.exposure[0]);
                shader.disable();

                /*if (s.selected) {
                    glEnable(GL_STENCIL_TEST);
                    glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);
                    glStencilMask(0xFF);
                    glClear(GL_STENCIL_BUFFER_BIT);
                    glStencilFunc(GL_ALWAYS, 1, 0xFF);

                    render(ts, modelMesh, mainCamera);
                    outlineShader.enable();
                    e.get(TransformComponent.class).scale.mul(1.10f, 1.10f, 1.10f);
                    e.updateData();

                    glStencilFunc(GL_NOTEQUAL, 1, 0xFF);
                    glStencilMask(0x00);

                    render(ts, modelMesh, mainCamera, outlineShader); // draw with the outline shader
                    outlineShader.disable();
                    e.get(TransformComponent.class).scale.div(1.10f, 1.10f, 1.10f);
                    e.updateData();

                    glDisable(GL_STENCIL_TEST);
                } else {
                    render(ts, modelMeshglmainCamera);
                }*/
                if (material != null) material.push(shader);
                render(ts, modelMesh, mainCamera, shader);
            }

            for (OxyEntity e : cachedNativeMeshes) {
                OpenGLMesh mesh = e.get(OpenGLMesh.class);
                if (e.has(OxyMaterialIndex.class) && e.has(OxyShader.class)) {
                    OxyMaterial m = OxyMaterialPool.getMaterial(e);
                    if (m != null) {
                        m.push(e.get(OxyShader.class));
                        render(ts, mesh, mainCamera, e.get(OxyShader.class));
                    }
                }

                if (hdrTexture != null) {
                    if (mesh.equals(hdrTexture.getMesh())) {
                        hdrTexture.bindAll();
                        glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
                        cubemapShader.enable();
                        if (EnvironmentPanel.mipLevelStrength[0] > 0)
                            cubemapShader.setUniform1i("skyBoxTexture", hdrTexture.getPrefilterSlot());
                        else cubemapShader.setUniform1i("skyBoxTexture", hdrTexture.getTextureSlot());
                        cubemapShader.setUniform1f("mipLevel", EnvironmentPanel.mipLevelStrength[0]);
                        cubemapShader.setUniform1f("exposure", EnvironmentPanel.exposure[0]);
                        cubemapShader.setUniform1f("gamma", EnvironmentPanel.gammaStrength[0]);
                        cubemapShader.disable();
                        render(ts, mesh, mainCamera, cubemapShader);
                        glDisable(GL_TEXTURE_CUBE_MAP_SEAMLESS);
                    }
                }
            }
        }

        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        frameBuffer.unbind();
        OxyTexture.unbindAllTextures();

        UILayer.uiSystem.dispatchNativeEvents();
    }

    public void recompileShader() {
        oxyShader.dispose();
        oxyShader = new OxyShader("shaders/OxyPBR.glsl");
        int[] samplers = new int[32];
        for (int i = 0; i < samplers.length; i++) samplers[i] = i;
        oxyShader.enable();
        oxyShader.setUniform1iv("tex", samplers);
        oxyShader.disable();
        for (OxyEntity m : allModelEntities) m.addComponent(oxyShader);
        for (OxyEntity m : cachedLightEntities) m.addComponent(oxyShader);
    }

    public void loadHDRTextureToScene(String path, Scene s) {
        if (path == null) return;
        if (SceneLayer.hdrTexture != null) SceneLayer.hdrTexture.dispose();
        SceneLayer.hdrTexture = OxyTexture.loadHDRTexture(path, s);
        cachedNativeMeshes = ACTIVE_SCENE.view(NativeObjectMeshOpenGL.class);
    }

    private void render(float ts, OpenGLMesh mesh, OxyCamera camera, OxyShader shader) {
        ACTIVE_SCENE.getRenderer().render(ts, mesh, camera, shader);
        OxyRenderer.Stats.totalShapeCount = ACTIVE_SCENE.getShapeCount();
    }

    public void clear() {
        cachedLightEntities.clear();
        cachedCameraComponents.clear();
        allModelEntities.clear();
    }

    public void startPicking() {
        if (allModelEntities.size() == 0) return;
        glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        OpenGLFrameBuffer pickingBuffer = ACTIVE_SCENE.getPickingBuffer();
        //ID RENDER PASS
        if (pickingBuffer.getBufferId() != 0) {
            int[] clearValue = {-1};
            pickingBuffer.bind();
            rendererAPI.clearBuffer();
            rendererAPI.clearColor(32, 32, 32, 1.0f);
            glClearTexImage(pickingBuffer.getColorAttachmentTexture(1), 0, pickingBuffer.getTextureFormat(1).getStorageFormat(), GL_INT, clearValue);
            for (OxyEntity e : allModelEntities)
                render(0, e.get(ModelMeshOpenGL.class), mainCamera, e.get(OxyShader.class));
        }
        int id = getEntityID();
        if (id == -1) {
            if (entityContext != null) entityContext.get(SelectedComponent.class).selected = false;
            entityContext = null;
        } else {
            for (OxyEntity e : allModelEntities) {
                if (e.getObjectId() == id) {
                    if (entityContext != null) entityContext.get(SelectedComponent.class).selected = false;
                    entityContext = e;
                    entityContext.get(SelectedComponent.class).selected = true;
                    break;
                }
            }
        }
        pickingBuffer.unbind();
    }

    private int getEntityID() {
        Vector2f mousePos = new Vector2f(
                ScenePanel.mousePos.x - ScenePanel.windowPos.x - ScenePanel.offset.x,
                ScenePanel.mousePos.y - ScenePanel.windowPos.y - ScenePanel.offset.y);
        mousePos.y = ACTIVE_SCENE.getFrameBuffer().getHeight() - mousePos.y;
        glReadBuffer(GL_COLOR_ATTACHMENT1);
        int[] entityID = new int[1];
        glReadPixels((int) mousePos.x, (int) mousePos.y, 1, 1, GL_RED_INTEGER, GL_INT, entityID);
        return entityID[0];
    }

}
