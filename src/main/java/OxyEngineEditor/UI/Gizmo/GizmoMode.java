package OxyEngineEditor.UI.Gizmo;

import OxyEngine.Components.*;
import OxyEngine.Core.Renderer.Mesh.ModelMeshOpenGL;
import OxyEngine.Core.Renderer.Shader.OxyShader;
import OxyEngine.System.OxySystem;
import OxyEngineEditor.Scene.Objects.Model.OxyMaterial;
import OxyEngineEditor.Scene.Objects.Model.OxyModel;
import OxyEngineEditor.Scene.OxyEntity;
import OxyEngineEditor.Scene.Scene;
import OxyEngineEditor.Scene.SceneRuntime;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static OxyEngine.Components.PerspectiveCamera.zoom;

enum GizmoMode {
    None(), Translation(), Rotation(), Scale();

    GizmoComponent gizmoComponent;

    void init(Scene scene, OxyShader shader, OxyGizmo3D gizmo3D) {
        switch (this) {
            case Translation -> gizmoComponent = new Translation(scene, shader, gizmo3D);
            case Scale -> gizmoComponent = new Scaling(scene, shader, gizmo3D);
        }
    }

    abstract static class GizmoComponent {

        final List<ComponentModel> models = new ArrayList<>();
        final Scene scene;

        private float oldZoom;

        /*
         * Helper class for the gizmo models
         * Since i dont want to have the gizmos as part of the scene
         * I need to have all the gizmo models as a separate class
         */
        public static class ComponentModel extends OxyModel {

            /*
             * To hold all of the components... normally this would be the registry in the scene
             */
            public final List<EntityComponent> components = new ArrayList<>();

            public ComponentModel(Scene scene) {
                super(scene, ThreadLocalRandom.current().nextInt(100, 1000)); //TODO: change it
            }

            @Override
            public void addComponent(EntityComponent... components) {
                this.components.addAll(Arrays.asList(components));
            }

            @Override
            public <T extends EntityComponent> T get(Class<T> destClass) {
                for (EntityComponent c : components) {
                    if (c.getClass().equals(destClass)) {
                        return (T) c;
                    }
                    if (destClass.isInstance(c)) {
                        return (T) c;
                    }
                }
                return null;
            }

            @Override
            public boolean has(Class<? extends EntityComponent> destClass) {
                for (EntityComponent c : components) {
                    if (destClass.equals(c.getClass()))
                        return true;
                    if (destClass.isInstance(c))
                        return true;
                }
                return false;
            }
        }

        public GizmoComponent(String path, Scene scene, OxyShader shader, OxyGizmo3D gizmo3D) {
            List<OxyModel> models = scene.createModelEntities(OxySystem.FileSystem.getResourceByPath(path), shader);
            for (OxyModel m : models) {
                ComponentModel cM = new ComponentModel(scene);
                cM.vertices = m.vertices;
                cM.indices = m.indices;
                cM.tcs = m.tcs;
                cM.normals = m.normals;
                cM.biTangents = m.biTangents;
                cM.tangents = m.tangents;
                cM.factory = m.factory;
                cM.addComponent(m.get(UUIDComponent.class),
                        m.get(MeshPosition.class),
                        m.get(TransformComponent.class),
                        new SelectedComponent(false, true),
                        new RenderableComponent(RenderingMode.None),
                        m.get(OxyShader.class),
                        m.get(OxyMaterial.class),
                        m.get(TagComponent.class),
                        m.get(BoundingBoxComponent.class)
                );
//                cM.addEventListener(new OxyGizmoController(gizmo3D));
                cM.initData(path);
                this.models.add(cM);
                scene.removeEntity(m);
            }
            this.scene = scene;
            oldZoom = zoom; //default
        }

        public void switchRenderableState(RenderingMode value) {
            for (OxyModel model : models) {
                model.get(RenderableComponent.class).mode = value;
            }
        }

        public void scaleIt(OxyEntity e) {
            if (zoom != oldZoom) {
                for (OxyModel gizmoT : models) {
                    scale(gizmoT, e);
                }
                oldZoom = zoom;
            }
        }

        private void scale(OxyModel model, OxyEntity e) {
            if (model.get(SelectedComponent.class).fixedValue) {
                model.get(TransformComponent.class).scale.set(zoom * 0.06f);
                update(model, e);
            } else {
                if (zoom < 150 && model.get(TransformComponent.class).scale.x != 3) { //does not matter if x or y or z
                    model.get(TransformComponent.class).scale.set(3);
                    update(model, e);
                }
            }
        }

        public void render(float ts) {
            for (OxyEntity e : models) {
                RenderableComponent r = e.get(RenderableComponent.class);
                if (r.mode != RenderingMode.Normal) continue;
                ModelMeshOpenGL modelMesh = e.get(ModelMeshOpenGL.class);
                OxyMaterial material = e.get(OxyMaterial.class);
                material.push(modelMesh.getShader());
                scene.getRenderer().render(ts, modelMesh, SceneRuntime.currentBoundedCamera);
            }
        }

        abstract void update(OxyModel model, OxyEntity e);
    }

    static class Translation extends GizmoComponent {

        Translation(Scene scene, OxyShader shader, OxyGizmo3D gizmo3D) {
            super("/models/native/oxygizmo.obj", scene, shader, gizmo3D);
        }

        @Override
        void update(OxyModel model, OxyEntity e) {
            TransformComponent c = model.get(TransformComponent.class);
            c.position.add(new Vector3f(c.position).mul(c.scale));
            model.updateData();
        }

        public OxyModel getXModelTranslation() {
            return models.get(2);
        }

        public OxyModel getYModelTranslation() {
            return models.get(0);
        }

        public OxyModel getZModelTranslation() {
            return models.get(1);
        }
    }

    static class Scaling extends GizmoComponent {

        Scaling(Scene scene, OxyShader shader, OxyGizmo3D gizmo3D) {
            super("/models/native/oxygizmoScale2.obj", scene, shader, gizmo3D);
        }

        @Override
        void update(OxyModel model, OxyEntity e) {
            TransformComponent c = model.get(TransformComponent.class);
            c.position.set(new Vector3f(c.position).mul(c.scale));
            model.updateData();
        }

        public OxyModel getXModelScale() {
            return models.get(0);
        }

        public OxyModel getYModelScale() {
            return models.get(1);
        }

        public OxyModel getZModelScale() {
            return models.get(2);
        }

        public OxyModel getScalingCube() {
            return models.get(3);
        }
    }
}