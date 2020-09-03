package OxyEngineEditor.UI.Selector;

import OxyEngine.Core.Renderer.OxyRenderer;
import OxyEngine.Core.Renderer.Texture.OxyColor;
import OxyEngine.Core.Window.WindowHandle;
import OxyEngine.Events.OxyMouseListener;
import OxyEngineEditor.Components.TransformComponent;
import OxyEngineEditor.Scene.Model.OxyMaterial;
import OxyEngineEditor.Scene.Model.OxyModel;
import OxyEngineEditor.Scene.OxyEntity;
import OxyEngineEditor.Scene.Scene;
import imgui.flag.ImGuiMouseButton;
import org.joml.Vector2d;

import static OxyEngineEditor.UI.OxyEventSystem.keyEventDispatcher;
import static OxyEngineEditor.UI.OxyEventSystem.mouseCursorPosDispatcher;
import static OxyEngineEditor.UI.Selector.OxySelectSystem.entityContext;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;

public class OxyGizmoController implements OxyMouseListener {

    /*
     * It is a pretty mess... so TODO: REFACTOR SOMETIME IN THE FUTURE
     */

    OxyEntity hoveredGameObject = null;
    OxyColor standardColor = null;
    boolean init = false;

    static Vector2d oldMousePos = new Vector2d();

    static OxyGizmo3D gizmo;
    static Scene scene;
    static WindowHandle windowHandle;

    static boolean dragging;

    static boolean pressedXTranslation, pressedYTranslation, pressedZTranslation;
    static boolean pressedXScale, pressedYScale, pressedZScale, pressedScaleFactor;

    OxyGizmoController(WindowHandle windowHandle, Scene scene, OxyGizmo3D gizmo) {
        OxyGizmoController.gizmo = gizmo;
        OxyGizmoController.scene = scene;
        OxyGizmoController.windowHandle = windowHandle;
    }

    @Override
    public void mouseClicked(OxyEntity selectedEntity, int mouseButton) {
        if (mouseButton == ImGuiMouseButton.Right && !keyEventDispatcher.getKeys()[GLFW_KEY_LEFT_SHIFT]) {
            switch (gizmo.mode) {
                case Translation -> handleTranslationSwitch(selectedEntity);
                case Scale -> handleScalingSwitch(selectedEntity);
            }
        }
    }

    @Override
    public void mouseDown(OxyEntity selectedEntity, int mouseButton) {
        if (mouseButton == ImGuiMouseButton.Right && entityContext != null && dragging && !keyEventDispatcher.getKeys()[GLFW_KEY_LEFT_SHIFT]) {
            switch (gizmo.mode) {
                case Translation -> handleTranslation();
                case Scale -> handleScaling();
            }
            dragging = false;
        }
    }

    @Override
    public void mouseHovered(OxyEntity hoveredEntity) {
        if (hoveredEntity == null && hoveredGameObject != null) {
            OxyColor hoveredColor = hoveredGameObject.get(OxyMaterial.class).diffuseColor;
            hoveredColor.setColorRGBA(standardColor.getNumbers());
            hoveredGameObject.updateData();
        }
        if (hoveredEntity instanceof OxyModel) {
            if (!init) {
                OxyColor color = hoveredEntity.get(OxyMaterial.class).diffuseColor;
                standardColor = (OxyColor) color.clone();
                init = true;
            }
            hoveredGameObject = hoveredEntity;
            OxyColor hoveredColor = hoveredGameObject.get(OxyMaterial.class).diffuseColor;
            hoveredColor.setColorRGBA(new float[]{1.0f, 1.0f, 0.0f, 1.0f});
            hoveredGameObject.updateData();
        }
    }

    @Override
    public void mouseDragged(OxyEntity selectedEntity, int mouseButton) {
        oldMousePos = new Vector2d(mouseCursorPosDispatcher.getXPos(), mouseCursorPosDispatcher.getYPos());
        dragging = true;
    }

    @Override
    public void mouseReleased(OxyEntity selectedEntity, int mouseButton) {
    }

    private void handleTranslationSwitch(OxyEntity selectedEntity) {
        GizmoMode.Translation t = (GizmoMode.Translation) gizmo.mode.gizmoComponent;
        if (selectedEntity == t.getXModelTranslation()) {
            pressedXTranslation = true;
            pressedYTranslation = false;
            pressedZTranslation = false;
            pressedXScale = false;
            pressedYScale = false;
            pressedZScale = false;
            pressedScaleFactor = false;
        }
        if (selectedEntity == t.getYModelTranslation()) {
            pressedYTranslation = true;
            pressedXTranslation = false;
            pressedZTranslation = false;
            pressedXScale = false;
            pressedYScale = false;
            pressedZScale = false;
            pressedScaleFactor = false;
        }
        if (selectedEntity == t.getZModelTranslation()) {
            pressedZTranslation = true;
            pressedXTranslation = false;
            pressedYTranslation = false;
            pressedXScale = false;
            pressedYScale = false;
            pressedZScale = false;
            pressedScaleFactor = false;
        }
    }

    private void handleTranslation() {
        GizmoMode.Translation t = (GizmoMode.Translation) gizmo.mode.gizmoComponent;
        OxyModel xAxis = t.getXModelTranslation();
        OxyModel yAxis = t.getYModelTranslation();
        OxyModel zAxis = t.getZModelTranslation();

        TransformComponent xC = xAxis.get(TransformComponent.class);
        TransformComponent yC = yAxis.get(TransformComponent.class);
        TransformComponent zC = zAxis.get(TransformComponent.class);
        TransformComponent currC = entityContext.get(TransformComponent.class);

        Vector2d nowMousePos = new Vector2d(mouseCursorPosDispatcher.getXPos(), mouseCursorPosDispatcher.getYPos());
        Vector2d delta = nowMousePos.sub(oldMousePos);

        float mouseSpeed = OxyRenderer.currentBoundedCamera.getCameraController().getMouseSpeed();
        float deltaX = (float) ((delta.x * mouseSpeed) * xC.scale.x) / 4f;
        float deltaY = (float) ((delta.y * mouseSpeed) * yC.scale.y) / 4f;
        if (deltaX <= -1f * xC.scale.x / 4f || deltaX >= 1f * xC.scale.x / 4f) deltaX = 0; // for safety reasons
        if (deltaY <= -1f * yC.scale.y / 4f || deltaY >= 1f * yC.scale.y / 4f) deltaY = 0;

        if (pressedZTranslation) {
            xC.position.add(0, 0, -deltaX);
            yC.position.add(0, 0, -deltaX);
            zC.position.add(0, 0, -deltaX);
            currC.position.add(0, 0, -deltaX);
            for (int i = 0; i < 4; i++)
                GizmoMode.Scale.gizmoComponent.models.get(i).get(TransformComponent.class).position.add(0, 0, -deltaX);
            entityContext.updateData();
        } else if (pressedYTranslation) {
            xC.position.add(0, deltaY, 0);
            yC.position.add(0, deltaY, 0);
            zC.position.add(0, deltaY, 0);
            currC.position.add(0, deltaY, 0);
            for (int i = 0; i < 4; i++)
                GizmoMode.Scale.gizmoComponent.models.get(i).get(TransformComponent.class).position.add(0, deltaY, 0);
            entityContext.updateData();
        } else if (pressedXTranslation) {
            xC.position.add(deltaX, 0, 0);
            yC.position.add(deltaX, 0, 0);
            zC.position.add(deltaX, 0, 0);
            currC.position.add(deltaX, 0, 0);
            for (int i = 0; i < 4; i++)
                GizmoMode.Scale.gizmoComponent.models.get(i).get(TransformComponent.class).position.add(deltaX, 0, 0);
            entityContext.updateData();
        }
    }

    private void handleScalingSwitch(OxyEntity selectedEntity) {
        GizmoMode.Scaling s = (GizmoMode.Scaling) gizmo.mode.gizmoComponent;
        if (selectedEntity == s.getXModelScale()) {
            pressedXScale = true;
            pressedYScale = false;
            pressedZScale = false;
            pressedScaleFactor = false;
            pressedXTranslation = false;
            pressedYTranslation = false;
            pressedZTranslation = false;
        }
        if (selectedEntity == s.getYModelScale()) {
            pressedYScale = true;
            pressedXScale = false;
            pressedZScale = false;
            pressedScaleFactor = false;
            pressedXTranslation = false;
            pressedYTranslation = false;
            pressedZTranslation = false;
        }
        if (selectedEntity == s.getZModelScale()) {
            pressedZScale = true;
            pressedXScale = false;
            pressedYScale = false;
            pressedScaleFactor = false;
            pressedXTranslation = false;
            pressedYTranslation = false;
            pressedZTranslation = false;
        }
        if (selectedEntity == s.getScalingCube()) {
            pressedScaleFactor = true;
            pressedZScale = false;
            pressedXScale = false;
            pressedYScale = false;
            pressedXTranslation = false;
            pressedYTranslation = false;
            pressedZTranslation = false;
        }
    }

    private void handleScaling() {
        GizmoMode.Scaling s = (GizmoMode.Scaling) gizmo.mode.gizmoComponent;

        OxyModel xAxis = s.getXModelScale();
        OxyModel yAxis = s.getYModelScale();

        TransformComponent xC = xAxis.get(TransformComponent.class);
        TransformComponent yC = yAxis.get(TransformComponent.class);

        TransformComponent currC = entityContext.get(TransformComponent.class);

        Vector2d nowMousePos = new Vector2d(mouseCursorPosDispatcher.getXPos(), mouseCursorPosDispatcher.getYPos());
        Vector2d delta = nowMousePos.sub(oldMousePos);

        float mouseSpeed = OxyRenderer.currentBoundedCamera.getCameraController().getMouseSpeed();
        float deltaX = (float) ((delta.x * mouseSpeed) * xC.scale.x) / 4f;
        float deltaY = (float) ((delta.y * mouseSpeed) * yC.scale.y) / 4f;
        if (deltaX <= -1f * xC.scale.x / 4f || deltaX >= 1f * xC.scale.x / 4f) deltaX = 0; // for safety reasons
        if (deltaY <= -1f * yC.scale.y / 4f || deltaY >= 1f * yC.scale.y / 4f) deltaY = 0;

        if (pressedZScale) {
            currC.scale.x += -deltaX;
            if (currC.scale.x <= 0) currC.scale.x = 0;
            entityContext.updateData();
        } else if (pressedYScale) {
            currC.scale.y += -deltaY;
            if (currC.scale.y <= 0) currC.scale.y = 0;
            entityContext.updateData();
        } else if (pressedXScale) {
            currC.scale.z += deltaX;
            if (currC.scale.z <= 0) currC.scale.z = 0;
            entityContext.updateData();
        } else if (pressedScaleFactor) {
            currC.scale.add(deltaX, deltaX, deltaX);
            if (currC.scale.x <= 0) currC.scale.x = 0;
            if (currC.scale.y <= 0) currC.scale.y = 0;
            if (currC.scale.z <= 0) currC.scale.z = 0;
            entityContext.updateData();
        }
    }
}