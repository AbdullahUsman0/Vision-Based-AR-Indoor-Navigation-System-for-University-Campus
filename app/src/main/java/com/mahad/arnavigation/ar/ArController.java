package com.mahad.arnavigation.ar;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.mahad.arnavigation.data.PoseMock;
import com.mahad.arnavigation.render.ArrowFactory;
import com.mahad.arnavigation.util.PoseSmoother;

import java.util.function.Consumer;

public class ArController {
    private final NavigationArFragment fragment;
    private final Consumer<Pose> onDevicePose;
    private final Consumer<WaypointProjection> onWaypointProjection;
    private final Consumer<String> onTrackingState;
    private final PoseSmoother smoother = new PoseSmoother(0.25f);

    private Node arrowNode;
    private Anchor arrowAnchor;
    private String currentLabel = "Mock Destination";
    private Pose lastLocalizationArrowPose;
    private Pose cameraPoseAtLastLocalization;
    private long lastLocalizationTimestampMs;

    public ArController(
            NavigationArFragment fragment,
            Consumer<Pose> onDevicePose,
            Consumer<WaypointProjection> onWaypointProjection,
            Consumer<String> onTrackingState
    ) {
        this.fragment = fragment;
        this.onDevicePose = onDevicePose;
        this.onWaypointProjection = onWaypointProjection;
        this.onTrackingState = onTrackingState;
    }

    public void start() {
        ArSceneView sceneView = fragment.getArSceneView();
        if (sceneView == null) {
            return;
        }
        sceneView.getScene().addOnUpdateListener(frameTime -> {
            ArSceneView updateSceneView = fragment.getArSceneView();
            if (updateSceneView == null || updateSceneView.getArFrame() == null) {
                return;
            }
            Pose cameraPose = updateSceneView.getArFrame().getCamera().getPose();
            onDevicePose.accept(cameraPose);
            applyTrackingBetweenLocalizationUpdates(cameraPose);
            updateWaypointProjection();
        });
    }

    public void placeArrowInFrontOfCamera(float distanceMeters) {
        ArSceneView sceneView = fragment.getArSceneView();
        if (sceneView == null || sceneView.getArFrame() == null) {
            return;
        }
        Pose cameraPose = sceneView.getArFrame().getCamera().getPose();
        Pose arrowPose = cameraPose.compose(Pose.makeTranslation(0f, 0f, -distanceMeters));

        if (arrowAnchor != null) {
            arrowAnchor.detach();
        }
        if (sceneView.getSession() == null) {
            return;
        }
        arrowAnchor = sceneView.getSession().createAnchor(arrowPose);

        AnchorNode anchorNode = new AnchorNode(arrowAnchor);
        anchorNode.setParent(sceneView.getScene());

        ArrowFactory.createArrowNode(fragment.requireContext(), node -> {
            arrowNode = node;
            node.setParent(anchorNode);
        });
    }

    public void applyLocalizationPose(PoseMock mockPose) {
        if (arrowNode == null) {
            return;
        }
        currentLabel = mockPose.getTargetLabel() != null ? mockPose.getTargetLabel() : "Mock Destination";
        lastLocalizationArrowPose = poseFromMock(mockPose);
        ArSceneView sceneView = fragment.getArSceneView();
        if (sceneView == null || sceneView.getArFrame() == null) {
            return;
        }
        cameraPoseAtLastLocalization = sceneView.getArFrame().getCamera().getPose();
        lastLocalizationTimestampMs = System.currentTimeMillis();
        applyPoseToArrow(lastLocalizationArrowPose);

        Log.d(
                "ArController",
                "Applied localization pose: pos=("
                        + mockPose.getPosition().getX() + ", "
                        + mockPose.getPosition().getY() + ", "
                        + mockPose.getPosition().getZ() + "), quat=("
                        + mockPose.getRotation().getQx() + ", "
                        + mockPose.getRotation().getQy() + ", "
                        + mockPose.getRotation().getQz() + ", "
                        + mockPose.getRotation().getQw() + ")"
        );
        onTrackingState.accept("Mode: LOCALIZATION_UPDATE (fresh fix)");
    }

    private void updateWaypointProjection() {
        if (arrowNode == null) {
            return;
        }
        ArSceneView sceneView = fragment.getArSceneView();
        if (sceneView == null) {
            return;
        }
        Camera sceneCamera = sceneView.getScene().getCamera();
        Vector3 worldPosition = arrowNode.getWorldPosition();
        Vector3 screenPoint = sceneCamera.worldToScreenPoint(worldPosition);

        Vector3 cameraToPoint = Vector3.subtract(worldPosition, sceneCamera.getWorldPosition());
        Vector3 normalized = normalized(cameraToPoint);
        boolean isInFront = Vector3.dot(sceneCamera.getForward(), normalized) > 0f;

        onWaypointProjection.accept(
                new WaypointProjection(screenPoint.x, screenPoint.y, isInFront, currentLabel)
        );
    }

    private void applyTrackingBetweenLocalizationUpdates(Pose currentCameraPose) {
        if (arrowNode == null || lastLocalizationArrowPose == null || cameraPoseAtLastLocalization == null) {
            return;
        }

        // Delta from camera pose at localization time -> current camera pose.
        Pose cameraDelta = cameraPoseAtLastLocalization.inverse().compose(currentCameraPose);

        // Predict arrow pose by propagating with camera motion until next localization fix.
        Pose predictedArrowPose = lastLocalizationArrowPose.compose(cameraDelta);
        applyPoseToArrow(predictedArrowPose);

        long ageMs = System.currentTimeMillis() - lastLocalizationTimestampMs;
        onTrackingState.accept("Mode: ARCORE_DELTA_TRACKING | last localization " + ageMs + "ms ago");
    }

    private Pose poseFromMock(PoseMock mockPose) {
        float[] translation = new float[] {
                mockPose.getPosition().getX(),
                mockPose.getPosition().getY(),
                mockPose.getPosition().getZ()
        };
        float[] rotation = new float[] {
                mockPose.getRotation().getQx(),
                mockPose.getRotation().getQy(),
                mockPose.getRotation().getQz(),
                mockPose.getRotation().getQw()
        };
        return new Pose(translation, rotation);
    }

    private void applyPoseToArrow(Pose pose) {
        if (arrowNode == null) {
            return;
        }
        Quaternion targetRotation = new Quaternion(
                pose.qx(),
                pose.qy(),
                pose.qz(),
                pose.qw()
        );
        Vector3 targetPosition = new Vector3(
                pose.tx(),
                pose.ty(),
                pose.tz()
        );
        arrowNode.setLocalRotation(smoother.smoothRotation(targetRotation));
        arrowNode.setLocalPosition(smoother.smoothPosition(targetPosition));
    }

    private Vector3 normalized(Vector3 vector3) {
        float length = (float) Math.sqrt(
                vector3.x * vector3.x + vector3.y * vector3.y + vector3.z * vector3.z
        );
        if (length == 0f) {
            return Vector3.zero();
        }
        return new Vector3(vector3.x / length, vector3.y / length, vector3.z / length);
    }
}
