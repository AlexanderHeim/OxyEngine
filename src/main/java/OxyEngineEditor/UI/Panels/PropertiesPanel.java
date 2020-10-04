package OxyEngineEditor.UI.Panels;

import OxyEngine.Core.Layers.SceneLayer;
import OxyEngine.Core.Renderer.Buffer.Mesh;
import OxyEngine.Core.Renderer.Shader.OxyShader;
import OxyEngine.Core.Renderer.Texture.OxyTexture;
import OxyEngine.Scripting.OxyScriptItem;
import OxyEngineEditor.Components.*;
import OxyEngineEditor.Scene.Objects.Model.OxyMaterial;
import OxyEngineEditor.Scene.Objects.Model.OxyModel;
import imgui.ImGui;
import imgui.flag.ImGuiColorEditFlags;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static OxyEngine.System.OxySystem.BASE_PATH;
import static OxyEngine.System.OxySystem.FileSystem.openDialog;
import static OxyEngineEditor.UI.Selector.OxySelectHandler.entityContext;
import static OxyEngineEditor.UI.Selector.OxySelectHandler.gizmoEntityContextControl;

public class PropertiesPanel extends Panel {

    private static PropertiesPanel INSTANCE = null;

    public static PropertiesPanel getInstance(SceneLayer sceneLayer) {
        if (INSTANCE == null) INSTANCE = new PropertiesPanel(sceneLayer);
        return INSTANCE;
    }

    private final SceneLayer sceneLayer;

    private static final List<PropertyTree> propertyTree = new ArrayList<>();

    public PropertiesPanel(SceneLayer sceneLayer) {
        this.sceneLayer = sceneLayer;
    }

    static boolean init = false;
    public static boolean focusedWindow = false;
    private static final ImBoolean helpWindowBool = new ImBoolean();

    ImString name = new ImString(0);
    ImString meshPath = new ImString(0);
    float[] albedo = {0, 0, 0, 0};

    final String[] componentNames = UIEditable.allUIEditableNames();
    final String[] componentFullName = UIEditable.allUIEditableFullNames();

    protected static final ImInt defaultValue = new ImInt(-1);

    @Override
    public void preload() {

    }

    @Override
    public void renderPanel() {
        gizmoEntityContextControl(entityContext);

        if (entityContext != null) {
            name = new ImString(entityContext.get(TagComponent.class).tag(), 100);
            meshPath = new ImString(new File(BASE_PATH).toURI().relativize(new File(entityContext.get(Mesh.class).getPath()).toURI()).getPath());
            albedo = entityContext.get(OxyMaterial.class).albedoColor.getNumbers();
        }

        ImGui.begin("Properties");

        if (entityContext == null) {
            ImGui.end();
            return;
        }

        ImGui.alignTextToFramePadding();
        ImGui.text("Name: ");
        ImGui.sameLine();
        if (ImGui.inputText("##hidelabel", name, ImGuiInputTextFlags.EnterReturnsTrue)) {
            if (name.get().length() == 0) name.set("Unnamed");
            entityContext.get(TagComponent.class).setTag(name.get());
        }

        focusedWindow = ImGui.isWindowFocused();

        if (!init) ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Transform")) {
            ImGui.columns(2, "myColumns");
            if (!init) ImGui.setColumnOffset(0, -90f);
            ImGui.alignTextToFramePadding();
            ImGui.text("Translation:");
            ImGui.alignTextToFramePadding();
            ImGui.text("Rotation:");
            ImGui.alignTextToFramePadding();
            ImGui.text("Scale:");
            ImGui.nextColumn();
            ImGui.pushItemWidth(ImGui.getContentRegionAvailWidth());

            TransformComponent t = entityContext.get(TransformComponent.class);
            float[] translationArr = new float[]{t.position.x, t.position.y, t.position.z};
            float[] rotationArr = new float[]{t.rotation.x, t.rotation.y, t.rotation.z};
            float[] scaleArr = new float[]{t.scale.x, t.scale.y, t.scale.z};
            ImGui.dragFloat3("##hidelabel T", translationArr, 0.1f);
            ImGui.dragFloat3("##hidelabel R", rotationArr, 0.1f);
            ImGui.dragFloat3("##hidelabel S", scaleArr, 0.1f, 0, Float.MAX_VALUE);
            t.position.set(translationArr);
            t.rotation.set(rotationArr);
            t.scale.set(scaleArr);
            entityContext.updateData();

            ImGui.popItemWidth();
            ImGui.columns(1);
            ImGui.separator();
            ImGui.treePop();
        }

        if (!init) ImGui.setNextItemOpen(true);
        if (ImGui.treeNode("Mesh")) {
            ImGui.columns(2, "myColumns");
            if (!init) {
                ImGui.setColumnOffset(0, -100f);
                init = true;
            }
            ImGui.alignTextToFramePadding();
            ImGui.text("Mesh:");
            ImGui.nextColumn();
            ImGui.pushItemWidth(ImGui.getContentRegionAvailWidth() - 30f);
            ImGui.inputText("##hidelabel", meshPath);
            ImGui.popItemWidth();
            ImGui.sameLine();
            if (ImGui.button("...")) {
                String path = openDialog("", null);
                if (path != null) {
                    if (entityContext != null) {
                        List<OxyModel> eList = sceneLayer.getScene().createModelEntities(path, entityContext.get(OxyShader.class));
                        for (OxyModel e : eList) {
                            TransformComponent t = new TransformComponent(entityContext.get(TransformComponent.class));
                            e.addComponent(t, new SelectedComponent(true, false));
                            e.constructData();
                        }
                        sceneLayer.getScene().removeEntity(entityContext);
                        sceneLayer.updateAllModelEntities();
                        meshPath = new ImString(path);
                        entityContext = null;
                    }
                }
            }
            ImGui.columns(1);
            ImGui.separator();
            ImGui.treePop();
        }

        if (entityContext == null) {
            ImGui.end();
            return;
        }

        if (ImGui.treeNodeEx("Base", ImGuiTreeNodeFlags.DefaultOpen)) {
            ImGui.alignTextToFramePadding();
            { // BASE COLOR
                ImGui.text("Base color: ");
                ImGui.sameLine();
                if (ImGui.colorEdit4("Base color", albedo,
                        ImGuiColorEditFlags.AlphaBar |
                                ImGuiColorEditFlags.AlphaPreview |
                                ImGuiColorEditFlags.NoBorder |
                                ImGuiColorEditFlags.NoDragDrop |
                                ImGuiColorEditFlags.DisplayRGB |
                                ImGuiColorEditFlags.NoLabel
                ) && entityContext != null) {
                    entityContext.get(OxyMaterial.class).albedoColor.setColorRGBA(albedo);
                }
            }

            {
                ImGui.spacing();
                ImGui.alignTextToFramePadding();
                ImGui.text("Albedo: (Base Texture): ");
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 130);
                boolean nullT = entityContext.get(OxyMaterial.class).albedoTexture == null;
                if (ImGui.imageButton(nullT ? -1 : entityContext.get(OxyMaterial.class).albedoTexture.getTextureId(), 80, 60)) {
                    String path = openDialog("", null);
                    if (path != null) {
                        if (!nullT) entityContext.get(OxyMaterial.class).albedoTexture.dispose();
                        entityContext.get(OxyMaterial.class).albedoTexture = OxyTexture.loadImage(path);
                    }
                }
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 30);
                if (ImGui.button("Remove A")) {
                    entityContext.get(OxyMaterial.class).albedoTexture.dispose();
                    entityContext.get(OxyMaterial.class).albedoTexture = null;
                }

                ImGui.alignTextToFramePadding();
                ImGui.text("Normal Map: ");
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 130);
                boolean nullN = entityContext.get(OxyMaterial.class).normalTexture == null;
                if (ImGui.imageButton(nullN ? -2 : entityContext.get(OxyMaterial.class).normalTexture.getTextureId(), 80, 60)) {
                    String path = openDialog("", null);
                    if (path != null) {
                        if (!nullN) entityContext.get(OxyMaterial.class).normalTexture.dispose();
                        entityContext.get(OxyMaterial.class).normalTexture = OxyTexture.loadImage(path);
                    }
                }
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 30);
                if (ImGui.button("Remove N")) {
                    entityContext.get(OxyMaterial.class).normalTexture.dispose();
                    entityContext.get(OxyMaterial.class).normalTexture = null;
                }

                ImGui.alignTextToFramePadding();
                ImGui.text("Roughness Map: ");
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 130);
                boolean nullR = entityContext.get(OxyMaterial.class).roughnessTexture == null;
                if (ImGui.imageButton(nullR ? -3 : entityContext.get(OxyMaterial.class).roughnessTexture.getTextureId(), 80, 60)) {
                    String path = openDialog("", null);
                    if (path != null) {
                        if (!nullR) entityContext.get(OxyMaterial.class).roughnessTexture.dispose();
                        entityContext.get(OxyMaterial.class).roughnessTexture = OxyTexture.loadImage(path);
                    }
                }
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 30);
                if (ImGui.button("Remove R")) {
                    entityContext.get(OxyMaterial.class).roughnessTexture.dispose();
                    entityContext.get(OxyMaterial.class).roughnessTexture = null;
                }

                ImGui.alignTextToFramePadding();
                ImGui.text("Metallic Map: ");
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 130);
                boolean nullM = entityContext.get(OxyMaterial.class).metallicTexture == null;
                if (ImGui.imageButton(nullM ? -4 : entityContext.get(OxyMaterial.class).metallicTexture.getTextureId(), 80, 60)) {
                    String path = openDialog("", null);
                    if (path != null) {
                        if (!nullM) entityContext.get(OxyMaterial.class).metallicTexture.dispose();
                        entityContext.get(OxyMaterial.class).metallicTexture = OxyTexture.loadImage(path);
                    }
                }
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 30);
                if (ImGui.button("Remove M")) {
                    entityContext.get(OxyMaterial.class).metallicTexture.dispose();
                    entityContext.get(OxyMaterial.class).metallicTexture = null;
                }

                ImGui.alignTextToFramePadding();
                ImGui.text("AO Map: ");
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 130);
                boolean nullAO = entityContext.get(OxyMaterial.class).aoTexture == null;
                if (ImGui.imageButton(nullAO ? -5 : entityContext.get(OxyMaterial.class).aoTexture.getTextureId(), 80, 60)) {
                    String path = openDialog("", null);
                    if (path != null) {
                        if (!nullAO) entityContext.get(OxyMaterial.class).aoTexture.dispose();
                        entityContext.get(OxyMaterial.class).aoTexture = OxyTexture.loadImage(path);
                    }
                }
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 30);
                if (ImGui.button("Remove AO")) {
                    entityContext.get(OxyMaterial.class).aoTexture.dispose();
                    entityContext.get(OxyMaterial.class).aoTexture = null;
                }

                ImGui.alignTextToFramePadding();
                ImGui.text("Height Map: ");
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 130);
                boolean nullHeight = entityContext.get(OxyMaterial.class).heightTexture == null;
                if (ImGui.imageButton(nullHeight ? -6 : entityContext.get(OxyMaterial.class).heightTexture.getTextureId(), 80, 60)) {
                    String path = openDialog("", null);
                    if (path != null) {
                        if (!nullHeight) entityContext.get(OxyMaterial.class).heightTexture.dispose();
                        entityContext.get(OxyMaterial.class).heightTexture = OxyTexture.loadImage(path);
                    }
                }
                ImGui.sameLine(ImGui.getContentRegionAvailWidth() - 30);
                if (ImGui.button("Remove H")) {
                    entityContext.get(OxyMaterial.class).heightTexture.dispose();
                    entityContext.get(OxyMaterial.class).heightTexture = null;
                }
            }
            ImGui.treePop();
        }

        { // Scripting
            Set<ScriptingComponent> set = sceneLayer.cachedScriptComponents;
            for (EntityComponent e : set) {
                OxyScriptItem item = ((ScriptingComponent) e).getScriptItem();
                for (var f : item.getFieldsAsObject()) {
                    if (f.getObject() instanceof Float m) {
                        ImGui.alignTextToFramePadding();
                        ImGui.text(f.getName());
                        ImGui.sameLine();
                        ImGui.sliderFloat("##hideLabel item1" + f.hashCode(), new float[]{m}, 0, 1000);
                    }
                }
            }
        }

        OxyMaterial m = entityContext.get(OxyMaterial.class);

        ImGui.alignTextToFramePadding();
        ImGui.text("Metallic Strength:");
        ImGui.sameLine();
        ImGui.sliderFloat("###hidelabel metallic", m.metalness, 0, 1);

        ImGui.alignTextToFramePadding();
        ImGui.text("Roughness strength:");
        ImGui.sameLine();
        ImGui.sliderFloat("###hidelabel roughness", m.roughness, 0, 1);

        ImGui.alignTextToFramePadding();
        ImGui.text("AO strength:");
        ImGui.sameLine();
        ImGui.sliderFloat("###hidelabel ao", m.aoStrength, 0, 1);

        ImGui.alignTextToFramePadding();
        ImGui.text("Normal map strength:");
        ImGui.sameLine();
        ImGui.sliderFloat("###hidelabel n", m.normalStrength, 0, 100);

        final float windowWidth = ImGui.getWindowWidth();
        ImGui.spacing();
        ImGui.spacing();
        ImGui.setCursorPosX(windowWidth / 2 - 150);
        ImGui.pushItemWidth(-1);
        if (ImGui.button("Add Component", 300, 30)) {
            propertyTree.add(new PropertyTree() {
                @Override
                public void designTree() {
                    ImGui.text("Type: ");
                    ImGui.sameLine();
                    ImGui.combo("###hidelabel " + hashCode(), defaultValue, componentNames, componentNames.length);
                    if (defaultValue.get() != -1) {
                        try {
                            @SuppressWarnings("unchecked")
                            EntityComponent destClass = entityContext.get((Class<? extends EntityComponent>) Class.forName(componentFullName[defaultValue.get()]));
                            node = (PropertyNode) destClass.getClass().getField("node").get(destClass);
                        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    if (node != null) {
                        name = componentNames[defaultValue.get()];
                        node.designNode();
                    }
                }
            });
        }
        for (PropertyTree tree : propertyTree) {

            if (tree.name == null) tree.name = "Node " + tree.hashCode();
            if (ImGui.treeNodeEx(tree.name, ImGuiTreeNodeFlags.DefaultOpen)) {
                tree.designTree();
                ImGui.treePop();
            }
        }
        ImGui.popItemWidth();

        ImGui.checkbox("Demo", helpWindowBool);
        if (helpWindowBool.get()) ImGui.showDemoWindow();

        ImGui.end();
    }
}
