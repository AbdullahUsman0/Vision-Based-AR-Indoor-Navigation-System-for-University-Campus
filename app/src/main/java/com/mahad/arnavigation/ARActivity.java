package com.mahad.arnavigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.mahad.arnavigation.ar.NavigationArFragment;
import com.mahad.arnavigation.render.ArrowFactory;

import java.util.Locale;

public class ARActivity extends AppCompatActivity {
    private static final String TAG = "ARActivity";
    private static final float ARROW_DISTANCE_METERS = 1.5f;
    public static final String EXTRA_TARGET_ROOM = "target_room";
    public static final String EXTRA_TARGET_BEARING = "target_bearing_degrees";
    private static final String DEFAULT_TARGET_ROOM = "F20";
    private static final float MOCK_TARGET_BEARING_DEGREES = 65f;

    private NavigationArFragment arFragment;
    private TextView debugTextView;
    private TextView targetTextView;

    private Node arrowNode;
    private float smoothedArrowYawDegrees = 0f;
    private boolean isSceneListenerAttached = false;
    private float currentHeadingDegrees = 0f;
    private SensorHelper sensorHelper;

    private String targetRoom = DEFAULT_TARGET_ROOM;
    // Replace with backend route bearing for the selected room.
    private float mockTargetBearingDegrees = MOCK_TARGET_BEARING_DEGREES;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startArMode();
                } else {
                    Log.w(TAG, "Camera permission denied in AR mode. Finishing activity.");
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);

        targetRoom = getIntent().getStringExtra(EXTRA_TARGET_ROOM);
        if (targetRoom == null || targetRoom.trim().isEmpty()) {
            targetRoom = DEFAULT_TARGET_ROOM;
        }
        mockTargetBearingDegrees = getIntent().getFloatExtra(
            EXTRA_TARGET_BEARING,
            MOCK_TARGET_BEARING_DEGREES
        );
        mockTargetBearingDegrees = normalizeDegrees(mockTargetBearingDegrees);

        debugTextView = findViewById(R.id.debugTextView);
        targetTextView = findViewById(R.id.targetTextView);
        arFragment = (NavigationArFragment) getSupportFragmentManager()
                .findFragmentById(R.id.arFragmentContainer);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorHelper = new SensorHelper(sensorManager, this::onHeadingUpdated);

        targetTextView.setText("Target: " + targetRoom + "  |  Computing direction...");

        if (arFragment == null) {
            Log.e(TAG, "NavigationArFragment not found.");
            finish();
            return;
        }

        checkCameraPermissionAndStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorHelper.hasRequiredSensors()) {
            sensorHelper.start();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startArMode();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.stop();
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startArMode();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startArMode() {
        if (isSceneListenerAttached) {
            return;
        }

        ArCoreApk.InstallStatus installStatus;
        try {
            installStatus = ArCoreApk.getInstance().requestInstall(this, true);
        } catch (Exception e) {
            Log.e(TAG, "ARCore install/request failed", e);
            finish();
            return;
        }

        if (installStatus == ArCoreApk.InstallStatus.INSTALL_REQUESTED) {
            Log.i(TAG, "Waiting for ARCore installation flow.");
            return;
        }

        // Keep mock input deterministic for this demo.
        mockTargetBearingDegrees = MOCK_TARGET_BEARING_DEGREES;

        arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
            Frame frame = arFragment.getArSceneView().getArFrame();
            if (frame == null) {
                return;
            }

            Pose cameraPose = frame.getCamera().getPose();
            updateDebugText(cameraPose);

            if (arrowNode == null) {
                placeArrowInFrontOfCamera(cameraPose);
            }
            updateArrowDirection();
        });
        isSceneListenerAttached = true;

        Log.i(TAG, "AR mode active.");
    }

    private void placeArrowInFrontOfCamera(@NonNull Pose cameraPose) {
        if (arFragment.getArSceneView().getSession() == null) {
            return;
        }

        Pose arrowPose = cameraPose.compose(Pose.makeTranslation(0f, 0f, -ARROW_DISTANCE_METERS));
        AnchorNode anchorNode = new AnchorNode(arFragment.getArSceneView().getSession().createAnchor(arrowPose));
        anchorNode.setParent(arFragment.getArSceneView().getScene());

        ArrowFactory.createArrowNode(this, node -> {
            arrowNode = node;
            node.setParent(anchorNode);
            Log.d(TAG, "3D arrow created in AR scene");
        });
    }

    private void updateArrowDirection() {
        if (arrowNode == null) {
            return;
        }

        float relativeTurn = shortestSignedAngleDegrees(
                normalizeDegrees(currentHeadingDegrees),
                normalizeDegrees(mockTargetBearingDegrees)
        );

        // Low-pass filter for smoother visual rotation.
        smoothedArrowYawDegrees = smoothAngle(smoothedArrowYawDegrees, relativeTurn, 0.15f);
        arrowNode.setLocalRotation(Quaternion.axisAngle(Vector3.up(), smoothedArrowYawDegrees));
    }

    private float smoothAngle(float current, float target, float alpha) {
        float delta = ((target - current + 540f) % 360f) - 180f;
        return (current + alpha * delta + 360f) % 360f;
    }

    private void updateDebugText(@NonNull Pose pose) {
        float[] t = pose.getTranslation();
        float[] r = pose.getRotationQuaternion();
        float normalizedHeading = normalizeDegrees(currentHeadingDegrees);
        float normalizedBearing = normalizeDegrees(mockTargetBearingDegrees);
        float relativeTurn = shortestSignedAngleDegrees(normalizedHeading, normalizedBearing);
        String instruction = buildInstruction(relativeTurn);

        String text = String.format(
                Locale.US,
                "Mode: AR\nTarget: %s\nHeading: %.1f°\nTarget bearing: %.1f°\nInstruction: %s\nPosition: x=%.2f y=%.2f z=%.2f\nRotation: qx=%.2f qy=%.2f qz=%.2f qw=%.2f\nArrowYaw: %.1f°",
                targetRoom,
                normalizedHeading,
                normalizedBearing,
                instruction,
                t[0], t[1], t[2],
                r[0], r[1], r[2], r[3],
                smoothedArrowYawDegrees
        );
        debugTextView.setText(text);
        targetTextView.setText(String.format(Locale.US, "Target: %s  |  %s", targetRoom, instruction));
    }

    private void onHeadingUpdated(float headingDegrees) {
        currentHeadingDegrees = normalizeDegrees(headingDegrees);
    }

    private float shortestSignedAngleDegrees(float from, float to) {
        return ((to - from + 540f) % 360f) - 180f;
    }

    private float normalizeDegrees(float degrees) {
        float normalized = degrees % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }

    private String buildInstruction(float relativeTurnDegrees) {
        float absTurn = Math.abs(relativeTurnDegrees);
        if (absTurn < 10f) {
            return "Go straight";
        }
        if (relativeTurnDegrees > 0f) {
            return String.format(Locale.US, "Turn right %.0f°", absTurn);
        }
        return String.format(Locale.US, "Turn left %.0f°", absTurn);
    }
}
