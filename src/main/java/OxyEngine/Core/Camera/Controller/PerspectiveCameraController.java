package OxyEngine.Core.Camera.Controller;

import OxyEngine.System.OxyTimestep;
import OxyEngine.Tools.Ref;
import OxyEngineEditor.UI.Layers.SceneLayer;
import OxyEngineEditor.UI.OxyUISystem;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.*;

public class PerspectiveCameraController extends OxyCameraController {

    public PerspectiveCameraController(Ref<Vector3f> translationRef, Ref<Vector3f> rotationRef, float mouseSpeed, float horizontalSpeed, float verticalSpeed) {
        super(translationRef, rotationRef, mouseSpeed, horizontalSpeed, verticalSpeed);
    }

    public PerspectiveCameraController(Ref<Vector3f> translationRef, Ref<Vector3f> rotationRef) {
        super(translationRef, rotationRef, 50f, 15f, 15f);
    }

    private void rotate(OxyTimestep ts) {
        float dx = (float) (OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getXPos() - oldMouseX);
        float dy = (float) (OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getYPos() - oldMouseY);

        rotationRef.obj.x += (-dy * mouseSpeed) / 16 * ts.getSeconds();
        rotationRef.obj.y += (-dx * mouseSpeed) / 16 * ts.getSeconds();
    }

    private void updateRotationFree(OxyTimestep ts) {
        if (SceneLayer.focusedWindowDragging) rotate(ts);

        oldMouseX = OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getXPos();
        oldMouseY = OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getYPos();
    }

    private void updateRotationSwipe(OxyTimestep ts) {
        if (OxyUISystem.OxyEventSystem.mouseButtonDispatcher.getButtons()[GLFW_MOUSE_BUTTON_1] && SceneLayer.focusedWindowDragging)
            rotate(ts);

        oldMouseX = OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getXPos();
        oldMouseY = OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getYPos();
    }

    private void updatePosition(OxyTimestep ts) {
        if(!SceneLayer.focusedWindow) return;
        float angle90 = (float) (rotationRef.obj.y + (Math.PI / 2));
        float angle = rotationRef.obj.y;
        if (OxyUISystem.OxyEventSystem.keyEventDispatcher.getKeys()[GLFW_KEY_W]) {
            positionRef.obj.x += Math.cos(angle90) * horizontalSpeed * ts.getSeconds();
            positionRef.obj.z += Math.sin(angle90) * horizontalSpeed * ts.getSeconds();
        }
        if (OxyUISystem.OxyEventSystem.keyEventDispatcher.getKeys()[GLFW_KEY_S]) {
            positionRef.obj.x -= Math.cos(angle90) * horizontalSpeed * ts.getSeconds();
            positionRef.obj.z -= Math.sin(angle90) * horizontalSpeed * ts.getSeconds();
        }
        if (OxyUISystem.OxyEventSystem.keyEventDispatcher.getKeys()[GLFW_KEY_D]) {
            positionRef.obj.x += Math.cos(angle) * horizontalSpeed * ts.getSeconds();
            positionRef.obj.z += Math.sin(angle) * horizontalSpeed * ts.getSeconds();
        }
        if (OxyUISystem.OxyEventSystem.keyEventDispatcher.getKeys()[GLFW_KEY_A]) {
            positionRef.obj.x -= Math.cos(angle) * horizontalSpeed * ts.getSeconds();
            positionRef.obj.z -= Math.sin(angle) * horizontalSpeed * ts.getSeconds();
        }
        if (OxyUISystem.OxyEventSystem.keyEventDispatcher.getKeys()[GLFW_KEY_SPACE]) {
            positionRef.obj.y -= verticalSpeed * ts.getSeconds();
        }
        if (OxyUISystem.OxyEventSystem.keyEventDispatcher.getKeys()[GLFW_KEY_LEFT_SHIFT]) {
            positionRef.obj.y += verticalSpeed * ts.getSeconds();
        }
    }

    @Override
    public void update(OxyTimestep ts, Mode mode) {
        if (mode == Mode.SWIPE) updateRotationSwipe(ts);
        else updateRotationFree(ts);
        updatePosition(ts);
    }
}
