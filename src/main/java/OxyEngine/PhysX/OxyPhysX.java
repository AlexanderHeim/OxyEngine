package OxyEngine.PhysX;

import OxyEngine.Components.TransformComponent;
import OxyEngine.Scene.OxyEntity;
import OxyEngine.Scene.SceneRuntime;
import OxyEngine.Scene.SceneState;
import OxyEngine.System.OxyDisposable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import physx.PxTopLevelFunctions;
import physx.common.*;
import physx.physics.PxFilterData;
import physx.physics.PxSceneFlagEnum;

import java.util.HashMap;
import java.util.Map;

import static OxyEngine.Scene.SceneRuntime.ACTIVE_SCENE;
import static OxyEngine.System.OxySystem.logger;

public final class OxyPhysX implements OxyDisposable {

    private OxyPhysXEnvironment physXEnv = null;
    private static PxFilterData DEFAULT_FILTER_DATA;

    private static OxyPhysX INSTANCE = null;

    public static OxyPhysX getInstance() {
        if (INSTANCE == null) INSTANCE = new OxyPhysX();
        return INSTANCE;
    }

    private OxyPhysX() {

    }

    //From the PhysX example
    static final class CustomErrorCallback extends JavaErrorCallback {

        private final Map<Integer, String> codeNames = new HashMap<>() {{
            put(PxErrorCodeEnum.eDEBUG_INFO, "DEBUG_INFO");
            put(PxErrorCodeEnum.eDEBUG_WARNING, "DEBUG_WARNING");
            put(PxErrorCodeEnum.eINVALID_PARAMETER, "INVALID_PARAMETER");
            put(PxErrorCodeEnum.eINVALID_OPERATION, "INVALID_OPERATION");
            put(PxErrorCodeEnum.eOUT_OF_MEMORY, "OUT_OF_MEMORY");
            put(PxErrorCodeEnum.eINTERNAL_ERROR, "INTERNAL_ERROR");
            put(PxErrorCodeEnum.eABORT, "ABORT");
            put(PxErrorCodeEnum.ePERF_WARNING, "PERF_WARNING");
        }};

        @Override
        public void reportError(int code, String message, String file, int line) {
            String codeName = codeNames.getOrDefault(code, "code: " + code);
            logger.severe(String.format("Nvidia PhysX: [%s] %s (%s:%d)\n", codeName, message, file, line));
        }
    }

    public void init() {
        physXEnv = OxyPhysXEnvironment.create(OxyPhysXEnvironment.createSpecification()
                .setFilterShader(PxTopLevelFunctions.DefaultFilterShader())
                .setFlags(PxSceneFlagEnum.eENABLE_CCD)
                .setGravity(0f, -9.81f, 0f)
                .setCallback(CustomErrorCallback.class));
        DEFAULT_FILTER_DATA = physXEnv.createFilterData(1, 1, 0, 0);
    }

    public void simulate() {

        if (ACTIVE_SCENE.STATE != SceneState.RUNNING) return;

        physXEnv.simulatePhysics(SceneRuntime.TS);
        for (OxyEntity physXEntities : ACTIVE_SCENE.view(OxyPhysXComponent.class)) {
            OxyPhysXActor actor = physXEntities.get(OxyPhysXComponent.class).getActor();
            OxyPhysXGeometry geometry = physXEntities.get(OxyPhysXComponent.class).getGeometry();
            if (actor == null || geometry == null) continue;

            Vector3f scale = new Vector3f();
            physXEntities.get(TransformComponent.class).transform.getScale(scale);

            PxTransform globalPose = physXEntities.get(OxyPhysXComponent.class).getActor().getGlobalPose();
            Vector3f pos = pxVec3ToJOMLVector3f(globalPose.getP());
            Quaternionf rot = pxVec3ToJOMLQuaternionf(globalPose.getQ());

            //physXMatrix4f is here because we are giving to the nvidia physx the "end" transformation (with root transformation)
            //in order to reset the transformation to the world space, we need to get the root transformation and invert it and finally multiply it

            Matrix4f physXMatrix4f = new Matrix4f()
                    .translate(pos)
                    .rotate(rot)
                    .scale(scale);

            Matrix4f actualEntityMatrix = physXMatrix4f.mulLocal(new Matrix4f(physXEntities.getRoot().get(TransformComponent.class).transform).invert());

            Vector3f posDest = new Vector3f();
            Quaternionf rotDest = new Quaternionf();
            actualEntityMatrix.getTranslation(posDest);
            actualEntityMatrix.getUnnormalizedRotation(rotDest);

            Vector3f scaleDest = new Vector3f();
            actualEntityMatrix.getScale(scaleDest);

            physXEntities.get(TransformComponent.class).set(posDest, rotDest, scaleDest);
            physXEntities.transformLocally();
        }
    }

    public void resetSimulation() {

        for (OxyEntity e : ACTIVE_SCENE.view(OxyPhysXComponent.class)) {
            physXEnv.removeActor(e.get(OxyPhysXComponent.class));
            e.get(OxyPhysXComponent.class).dispose();
        }

        physXEnv.resetScene();

        for (OxyEntity e : ACTIVE_SCENE.view(OxyPhysXComponent.class)) {
            e.get(OxyPhysXComponent.class).getGeometry().build();
            e.get(OxyPhysXComponent.class).getActor().build();
        }
    }

    @Override
    public void dispose() {
        DEFAULT_FILTER_DATA.destroy();
        physXEnv.dispose();
        physXEnv = null;
    }

    public OxyPhysXEnvironment getPhysXEnv() {
        return physXEnv;
    }

    public PxFilterData getDefaultFilterData() {
        return DEFAULT_FILTER_DATA;
    }

    public static Vector3f pxVec3ToJOMLVector3f(PxVec3 vec3) {
        return new Vector3f(vec3.getX(), vec3.getY(), vec3.getZ());
    }

    public static Quaternionf pxVec3ToJOMLQuaternionf(PxQuat quat) {
        return new Quaternionf(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    public static PxTransform matrix4fToPxTransform(Matrix4f transform) {
        Vector3f pos = new Vector3f();
        Quaternionf rot = new Quaternionf();
        transform.getTranslation(pos);
        transform.getUnnormalizedRotation(rot);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PxVec3 vec3 = PxVec3.createAt(stack, MemoryStack::nmalloc, pos.x, pos.y, pos.z);
            PxQuat quat = PxQuat.createAt(stack, MemoryStack::nmalloc, rot.x, rot.y, rot.z, rot.w);
            return PxTransform.createAt(stack, MemoryStack::nmalloc, vec3, quat);
        }
    }
}
