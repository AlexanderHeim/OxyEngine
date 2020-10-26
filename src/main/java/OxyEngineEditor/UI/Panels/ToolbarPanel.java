package OxyEngineEditor.UI.Panels;

import OxyEngine.Core.Layers.GizmoLayer;
import OxyEngine.Core.Layers.OverlayPanelLayer;
import OxyEngine.Core.Layers.SceneLayer;
import OxyEngine.Core.Renderer.Shader.OxyShader;
import OxyEngineEditor.Scene.Scene;
import OxyEngineEditor.Scene.SceneSerializer;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiStyleVar;

import static OxyEngine.System.OxySystem.FileSystem.openDialog;
import static OxyEngine.System.OxySystem.FileSystem.saveDialog;
import static OxyEngineEditor.Scene.SceneSerializer.extensionName;
import static OxyEngineEditor.Scene.SceneSerializer.fileExtension;

public class ToolbarPanel extends Panel {

    private static ToolbarPanel INSTANCE = null;

    public static ToolbarPanel getInstance(SceneLayer layer, GizmoLayer gizmoLayer, OverlayPanelLayer opL, OxyShader shader) {
        if (INSTANCE == null) INSTANCE = new ToolbarPanel(layer, gizmoLayer, opL, shader);
        return INSTANCE;
    }

    private final OxyShader shader;
    private final SceneLayer sceneLayer;
    private final GizmoLayer gizmoLayer;
    private final OverlayPanelLayer opL;

    public ToolbarPanel(SceneLayer sceneLayer, GizmoLayer gizmoLayer, OverlayPanelLayer opL, OxyShader shader) {
        this.shader = shader;
        this.sceneLayer = sceneLayer;
        this.gizmoLayer = gizmoLayer;
        this.opL = opL;
    }

    @Override
    public void preload() {
    }

    @Override
    public void renderPanel() {
        ImGui.pushStyleColor(ImGuiCol.MenuBarBg, bgC[0], bgC[1], bgC[2], bgC[3]);
        ImGui.pushStyleVar(ImGuiStyleVar.FramePadding, 0f, 10);

        if (ImGui.beginMainMenuBar()) {
            ImVec2 pos = new ImVec2();
            ImGui.getWindowPos(pos);
            ImGui.setCursorPosY(ImGui.getIO().hasConfigFlags(ImGuiConfigFlags.ViewportsEnable) ? pos.y - 20f : pos.y + 3);
            if (ImGui.beginMenu("File")) {
                if (ImGui.beginMenu("New")) {
                    ImGui.menuItem("New Scene");
                    ImGui.endMenu();
                }
                if (ImGui.menuItem("Open a scene", "Ctrl+O")) {
                    String openScene = openDialog(extensionName, null);
                    Scene newScene = SceneSerializer.deserializeScene(openScene, sceneLayer, shader);
                    gizmoLayer.setScene(newScene);
                    gizmoLayer.build();
                    opL.setScene(newScene);
                    sceneLayer.build();
                }
                if (ImGui.menuItem("Save the scene", "Ctrl+S")) {
                    SceneSerializer.serializeScene(sceneLayer);
                }
                if (ImGui.menuItem("Save As...")) {
                    String saveAs = saveDialog(extensionName, null);
                    if(saveAs != null) SceneSerializer.serializeScene(sceneLayer, saveAs + fileExtension);
                }
                ImGui.endMenu();
            }
            ImGui.spacing();
            if (ImGui.beginMenu("Edit")) {
                if (ImGui.menuItem("Back", "Ctrl+Z")) {
                }
                if (ImGui.menuItem("Forward", "Ctrl+Y")) {
                }
                ImGui.endMenu();
            }
            ImGui.spacing();
            ImGui.endMainMenuBar();
        }

        ImGui.popStyleColor();
        ImGui.popStyleVar();
    }
}
