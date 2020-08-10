package OxyEngineEditor.UI;

import OxyEngine.Core.Camera.OxyCamera;
import OxyEngine.Core.Renderer.Shader.OxyShader;
import OxyEngine.Core.Window.WindowHandle;
import OxyEngine.Events.GLFW.GLFWEventDispatcher;
import OxyEngine.Events.GLFW.GLFWEventType;
import OxyEngine.Events.OxyEventDispatcherThread;
import OxyEngine.OxyEngine;
import OxyEngineEditor.Sandbox.Scene.OxyEntity;
import OxyEngineEditor.Sandbox.Scene.Scene;
import OxyEngineEditor.UI.Font.FontLoader;
import OxyEngineEditor.UI.Font.OxyFontSystem;
import OxyEngineEditor.UI.Selector.OxySelectSystem;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import imgui.gl3.ImGuiImplGl3;

import java.io.File;
import java.util.Objects;
import java.util.Set;

import static OxyEngine.System.OxySystem.gl_Version;
import static OxyEngineEditor.UI.OxyUISystem.OxyEventSystem.*;
import static org.lwjgl.glfw.GLFW.*;

public class OxyUISystem {

    private ImGuiIO io;

    private final ImGuiImplGl3 imGuiRenderer;
    private final WindowHandle windowHandle;

    private final OxySelectSystem selectSystem;

    private final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];

    public OxyUISystem(Scene scene, WindowHandle windowHandle, OxyShader shader) {
        this.windowHandle = windowHandle;
        imGuiRenderer = new ImGuiImplGl3();
        dispatcherThread = new OxyEventDispatcherThread();
        dispatcherThread.startThread();
        selectSystem = OxySelectSystem.getInstance(scene, shader);
        init();
    }

    public static record OxyEventSystem() {
        public static GLFWEventDispatcher.MouseEvent mouseButtonDispatcher;
        public static GLFWEventDispatcher.MouseCursorPosEvent mouseCursorPosDispatcher;
        public static GLFWEventDispatcher.KeyEvent keyEventDispatcher;
        public static GLFWEventDispatcher.KeyCharEvent keyCharDispatcher;
        public static GLFWEventDispatcher.MouseScrollEvent mouseScrollDispatcher;

        public static OxyEventDispatcherThread dispatcherThread;
    }

    private void init() {
        ImGui.createContext();
        io = ImGui.getIO();

        ImGui.getStyle().setColors(OxyEngine.getLoadedTheme());

        io.setIniFilename(null);
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard | ImGuiConfigFlags.DockingEnable);
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors);
        io.setBackendPlatformName("imgui_java_impl_glfw");

        setIndices();

        mouseButtonDispatcher = (GLFWEventDispatcher.MouseEvent) GLFWEventDispatcher.getInstance(GLFWEventType.MouseEvent, io);
        mouseCursorPosDispatcher = (GLFWEventDispatcher.MouseCursorPosEvent) GLFWEventDispatcher.getInstance(GLFWEventType.MouseCursorPosEvent, io);
        keyEventDispatcher = (GLFWEventDispatcher.KeyEvent) GLFWEventDispatcher.getInstance(GLFWEventType.KeyEvent, io);
        keyCharDispatcher = (GLFWEventDispatcher.KeyCharEvent) GLFWEventDispatcher.getInstance(GLFWEventType.KeyCharEvent, io);
        mouseScrollDispatcher = (GLFWEventDispatcher.MouseScrollEvent) GLFWEventDispatcher.getInstance(GLFWEventType.MouseScrollEvent, io);


        final long winPtr = windowHandle.getPointer();
        glfwSetMouseButtonCallback(winPtr, mouseButtonDispatcher);
        glfwSetKeyCallback(winPtr, keyEventDispatcher);
        glfwSetCharCallback(winPtr, keyCharDispatcher);
        glfwSetScrollCallback(winPtr, mouseScrollDispatcher);
        glfwSetCursorPosCallback(winPtr, mouseCursorPosDispatcher);

        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(final String s) {
                glfwSetClipboardString(winPtr, s);
            }
        });

        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                final String clipboardString = glfwGetClipboardString(winPtr);
                return Objects.requireNonNullElse(clipboardString, "");
            }
        });

        File[] file = FontLoader.getInstance().load();
        for (File f : file) OxyFontSystem.load(io, f.getPath(), 15, f.getName().split("\\.")[0]);

        imGuiRenderer.init(gl_Version);
    }

    public void updateImGuiContext(float deltaTime) {
        int[] fbWidth = new int[1];
        int[] fbHeight = new int[1];
        glfwGetFramebufferSize(windowHandle.getPointer(), fbWidth, fbHeight);

        io.setDisplaySize(windowHandle.getWidth(), windowHandle.getHeight());
        io.setDisplayFramebufferScale((float) fbWidth[0] / windowHandle.getWidth(), (float) fbHeight[0] / windowHandle.getHeight());
        io.setMousePos((float) OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getXPos(), (float) OxyUISystem.OxyEventSystem.mouseCursorPosDispatcher.getYPos());
        io.setDeltaTime(deltaTime);

        final int imguiCursor = ImGui.getMouseCursor();
        glfwSetCursor(windowHandle.getPointer(), mouseCursors[imguiCursor]);
        glfwSetInputMode(windowHandle.getPointer(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    public void start(Set<OxyEntity> entityList, OxyCamera camera) {
        selectSystem.start(entityList, camera);
    }

    public void updateImGuiRenderer() {
        imGuiRenderer.render(ImGui.getDrawData());
    }

    public void dispose() {
        for (long mouseCursor : mouseCursors) {
            glfwDestroyCursor(mouseCursor);
        }
        imGuiRenderer.dispose();
        ImGui.destroyContext();
    }

    private void setIndices() {
        final int[] keyMap = new int[ImGuiKey.COUNT];
        //unfortunately can't use a for loop here
        keyMap[ImGuiKey.Tab] = GLFW_KEY_TAB;
        keyMap[ImGuiKey.LeftArrow] = GLFW_KEY_LEFT;
        keyMap[ImGuiKey.RightArrow] = GLFW_KEY_RIGHT;
        keyMap[ImGuiKey.UpArrow] = GLFW_KEY_UP;
        keyMap[ImGuiKey.DownArrow] = GLFW_KEY_DOWN;
        keyMap[ImGuiKey.PageUp] = GLFW_KEY_PAGE_UP;
        keyMap[ImGuiKey.PageDown] = GLFW_KEY_PAGE_DOWN;
        keyMap[ImGuiKey.Home] = GLFW_KEY_HOME;
        keyMap[ImGuiKey.End] = GLFW_KEY_END;
        keyMap[ImGuiKey.Insert] = GLFW_KEY_INSERT;
        keyMap[ImGuiKey.Delete] = GLFW_KEY_DELETE;
        keyMap[ImGuiKey.Backspace] = GLFW_KEY_BACKSPACE;
        keyMap[ImGuiKey.Space] = GLFW_KEY_SPACE;
        keyMap[ImGuiKey.Enter] = GLFW_KEY_ENTER;
        keyMap[ImGuiKey.Escape] = GLFW_KEY_ESCAPE;
        keyMap[ImGuiKey.KeyPadEnter] = GLFW_KEY_KP_ENTER;
        keyMap[ImGuiKey.A] = GLFW_KEY_A;
        keyMap[ImGuiKey.C] = GLFW_KEY_C;
        keyMap[ImGuiKey.V] = GLFW_KEY_V;
        keyMap[ImGuiKey.X] = GLFW_KEY_X;
        keyMap[ImGuiKey.Y] = GLFW_KEY_Y;
        keyMap[ImGuiKey.Z] = GLFW_KEY_Z;
        io.setKeyMap(keyMap);

        //unfortunately can't use a for loop here
        mouseCursors[ImGuiMouseCursor.Arrow] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.TextInput] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNS] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeEW] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.Hand] = glfwCreateStandardCursor(GLFW_HAND_CURSOR);
        mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
    }
}
