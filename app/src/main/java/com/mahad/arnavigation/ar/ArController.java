package com.mahad.arnavigation.ar;

import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.mahad.arnavigation.data.PoseMock;
import com.mahad.arnavigation.render.ArrowFactory;
import com.mahad.arnavigation.util.PoseSmoother;

import java.util.function.Consumer;

public class ArController {
    private static final String TAG = "ArController";

    private final NavigationArFragment fragment;
    private final Consumer<Pose> onDevicePose;
    private final PoseSmoother smoother = new PoseSmoother(0.25f);

    private Node arrowNode;
    private Anchor arrowAnchor;

    public ArController(NavigationArFragment fragment, Consumer<Pose> onDevicePose) {
        this.fragment = fragment;
        this.onDevicePose = onDevicePose;
    }

    public void start() {
        fragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            if (fragment.getArSceneView().getArFrame() == null) {
                return;
            }
            Pose cameraPose = fragment.getArSceneView().getArFrame().getCamera().getPose();
            onDevicePose.accept(cameraPose);
        });
    }

    public void placeArrowInFrontOfCamera() {
        placeArrowInFrontOfCamera(1.5f);
    }

    public void placeArrowInFrontOfCamera(float distanceMeters) {
        if (fragment.getArSceneView().getArFrame() == null) {
            return;
        }

        Pose cameraPose = fragment.getArSceneView().getArFrame().getCamera().getPose();
        Pose arrowPose = cameraPose.compose(Pose.makeTranslation(0f, 0f, -distanceMeters));

        if (arrowAnchor != null) {
            arrowAnchor.detach();
        }

        if (fragment.getArSceneView().getSession() == null) {
            return;
        }
        arrowAnchor = fragment.getArSceneView().getSession().createAnchor(arrowPose);

        AnchorNode anchorNode = new AnchorNode(arrowAnchor);
        anchorNode.setParent(fragment.getArSceneView().getScene());

        ArrowFactory.createArrowNode(fragment.requireContext(), node -> {
            arrowNode = node;
            node.setParent(anchorNode);
        });
    }

    public void applyJsonRotation(PoseMock mockPose) {
        if (arrowNode == null || mockPose == null || mockPose.getRotation() == null || mockPose.getPosition() == null) {
            return;
        }

        Quaternion targetRotation = new Quaternion(
            mockPose.getRotation().getQx(),
            mockPose.getRotation().getQy(),
            mockPose.getRotation().getQz(),
            mockPose.getRotation().getQw()
        );
        arrowNode.setLocalRotation(smoother.smoothRotation(targetRotation));

        Vector3 targetPosition = new Vector3(
            mockPose.getPosition().getX(),
            mockPose.getPosition().getY(),
            mockPose.getPosition().getZ()
        );
        arrowNode.setLocalPosition(smoother.smoothPosition(targetPosition));

        Log.d(
            TAG,
            "Applied JSON pose: pos=(" + mockPose.getPosition().getX() + ", "
                + mockPose.getPosition().getY() + ", " + mockPose.getPosition().getZ() + ")"
                + ", quat=(" + mockPose.getRotation().getQx() + ", " + mockPose.getRotation().getQy()
                + ", " + mockPose.getRotation().getQz() + ", " + mockPose.getRotation().getQw() + ")"
        );
    }
}
