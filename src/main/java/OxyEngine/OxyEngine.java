package OxyEngine;

import OxyEngine.Core.Renderer.OxyRenderer;
import OxyEngine.Core.Renderer.OxyRenderer3D;
import OxyEngine.Core.Renderer.OxyRendererType;
import OxyEngine.Core.Window.WindowBuilder;
import OxyEngine.Core.Window.WindowHandle;
import OxyEngine.Core.Window.WindowHint;
import OxyEngine.OpenGL.OpenGLContext;
import OxyEngine.System.OxyDisposable;
import OxyEngineEditor.Sandbox.Scene.Scene;
import OxyEngineEditor.UI.Layers.MainUILayer;
import OxyEngineEditor.UI.Loader.UIThemeLoader;

import java.util.Objects;
import java.util.function.Supplier;

import static OxyEngine.System.OxySystem.logger;
import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;

public class OxyEngine implements OxyDisposable {

    private final WindowHandle windowHandle;
    private final Antialiasing antialiasing;
    private final boolean vSync;

    private final Thread thread;

    private OxyRenderer renderer;

    private static MainUILayer MAIN_UI_COMPONENT;

    private static final float[][] LOADED_THEME = UIThemeLoader.getInstance().load();

    public OxyEngine(Supplier<Runnable> supplier, WindowHandle windowHandle, Antialiasing antialiasing, boolean vSync, OxyRendererType type) {
        thread = new Thread(supplier.get(), "OxyEngine - 1");
        this.windowHandle = windowHandle;
        this.antialiasing = antialiasing;
        this.vSync = vSync;

        if(type == OxyRendererType.Oxy3D)
            renderer = OxyRenderer3D.getInstance(windowHandle);
        //OxyRenderer2D is not a thing... but will be a thing (hopefully)
    }

    public enum Antialiasing {
        ON(4), OFF(0);

        private final int level;
        Antialiasing(int level){
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    public synchronized void start() { thread.start(); }

    public void init() {
        if (!glfwInit()) {
            logger.severe("Can't init GLFW");
            throw new InternalError("Can't init GLFW");
        }
        logger.info("GLFW init successful");

        WindowBuilder builder = new WindowBuilder.WindowFactory();
        WindowHint windowHint = builder.createHints()
                .antiAliasing(antialiasing)
                .resizable(GLFW_TRUE)
                .doubleBuffered(GLFW_TRUE);
        windowHint.create();
        windowHandle.setPointer(switch(windowHandle.getMode()){
            case WINDOWED -> builder.createOpenGLWindow(windowHandle.getWidth(), windowHandle.getHeight(), windowHandle.getTitle());
            case FULLSCREEN -> builder.createFullscreenOpenGLWindow(windowHandle.getTitle());
            case WINDOWEDFULLSCREEN -> builder.createWindowedFullscreenOpenGLWindow(windowHandle.getTitle());
        });
        OpenGLContext.init(windowHandle);
        glfwSwapInterval(vSync ? 1 : 0);
    }

    public void initLayers(Scene scene){
        MAIN_UI_COMPONENT = new MainUILayer(windowHandle, scene);
    }

    @Override
    public void dispose() {
        glfwFreeCallbacks(windowHandle.getPointer());
        glfwDestroyWindow(windowHandle.getPointer());

        glfwTerminate();
        Objects.requireNonNull(glfwSetErrorCallback(null)).free();
    }

    public OxyRenderer getRenderer() {
        return renderer;
    }

    public Thread getMainThread() {
        return thread;
    }

    public static MainUILayer getMainUIComponent() {
        return MAIN_UI_COMPONENT;
    }

    public static float[][] getLoadedTheme() {
        return LOADED_THEME;
    }
}
