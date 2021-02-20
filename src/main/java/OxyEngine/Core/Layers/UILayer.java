package OxyEngine.Core.Layers;

import OxyEngine.OxyEngine;
import OxyEngine.System.OxyFontSystem;
import OxyEngine.System.OxyUISystem;
import OxyEngineEditor.UI.Panels.Panel;
import imgui.ImGui;
import imgui.ImGuiViewport;
import imgui.flag.*;

import java.util.ArrayList;
import java.util.List;

import static OxyEngine.Core.Renderer.Context.OxyRenderCommand.rendererAPI;

public class UILayer extends Layer {

    private final List<Panel> panelList = new ArrayList<>();

    public static OxyUISystem uiSystem;

    private static UILayer INSTANCE = null;

    public static UILayer getInstance(){
        if(INSTANCE == null) INSTANCE = new UILayer();
        return INSTANCE;
    }

    private UILayer(){
        uiSystem = new OxyUISystem(OxyEngine.getWindowHandle());
    }

    public void addPanel(Panel panel) {
        panelList.add(panel);
    }

    @Override
    public void build() {
        for (Panel panel : panelList) {
            panel.preload();
        }
    }

    @Override
    public void rebuild() {

    }

    @Override
    public void update(float ts) {
        OxyEngine.getWindowHandle().update();
    }

    @Override
    public void render(float ts) {
        rendererAPI.clearBuffer();
        rendererAPI.clearColor(0, 0, 0, 1.0f);

        uiSystem.newFrameGLFW();
        ImGui.newFrame();

        final ImGuiViewport viewport = ImGui.getMainViewport();
        ImGui.setNextWindowPos(viewport.getWorkPosX(), viewport.getWorkPosY(), ImGuiCond.Always);
        ImGui.setNextWindowSize(viewport.getWorkSizeX(), viewport.getWorkSizeY(), ImGuiCond.Always);

        ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 4, 8);
        ImGui.pushStyleVar(ImGuiStyleVar.FrameRounding, 3);
        ImGui.pushStyleColor(ImGuiCol.TableHeaderBg, 28f/255f, 28f/255f, 28f/255f, 1f);
        ImGui.pushFont(OxyFontSystem.getAllFonts().get(0));

        ImGui.begin("Main", ImGuiWindowFlags.NoResize |
                ImGuiWindowFlags.NoBackground |
                ImGuiWindowFlags.NoTitleBar |
                ImGuiWindowFlags.NoMove |
                ImGuiWindowFlags.NoBringToFrontOnFocus |
                ImGuiWindowFlags.NoDecoration);
        int id = ImGui.getID("MyDockSpace");
        ImGui.dockSpace(id, 0, 0, ImGuiDockNodeFlags.PassthruCentralNode);
        ImGui.end();

        for (Panel panel : panelList)
            panel.renderPanel();

        ImGui.popFont();
        ImGui.popStyleVar(2);
        ImGui.popStyleColor();

        uiSystem.updateImGuiContext(ts);
        ImGui.render();
        uiSystem.renderDrawData();
    }
}
