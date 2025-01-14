package OxyEngine.Scene.Objects.Importer;

import OxyEngine.Scene.OxyEntity;
import org.lwjgl.assimp.AIScene;

public sealed interface ModelImporterFactory permits AnimationImporter, MeshImporter {

    void process(AIScene scene, String scenePath, OxyEntity root);

    @SuppressWarnings("unchecked")
    static <T extends ModelImporterFactory> T getInstance(ImporterType type){
        return switch (type){
            case MeshImporter -> (T) new MeshImporter();
            case AnimationImporter -> (T) new AnimationImporter();
        };
    }
}
