package com.mahad.arnavigation;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Arrays;
import java.util.Locale;

public class FallbackActivity extends AppCompatActivity {
    private static final String TAG = "FallbackActivity";
    public static final String EXTRA_TARGET_ROOM = "target_room";
    public static final String EXTRA_TARGET_BEARING = "target_bearing_degrees";
    private static final String DEFAULT_TARGET_ROOM = "F20";
    private static final float MOCK_TARGET_BEARING_DEGREES = 65f;

    private TextureView previewView;
    private ArrowView arrowView;
    private TextView debugTextView;
    private TextView targetTextView;

    private CameraManager cameraManager;
    private String backCameraId;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Size previewSize;
    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private SensorHelper sensorHelper;
    private String targetRoom = DEFAULT_TARGET_ROOM;
    private float mockTargetBearingDegrees = MOCK_TARGET_BEARING_DEGREES;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startCameraPreview();
                } else {
                    Log.w(TAG, "Camera permission denied in fallback mode. Finishing activity.");
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fallback);

        targetRoom = getIntent().getStringExtra(EXTRA_TARGET_ROOM);
        if (targetRoom == null || targetRoom.trim().isEmpty()) {
            targetRoom = DEFAULT_TARGET_ROOM;
        }
        mockTargetBearingDegrees = getIntent().getFloatExtra(
            EXTRA_TARGET_BEARING,
            MOCK_TARGET_BEARING_DEGREES
        );
        mockTargetBearingDegrees = normalizeDegrees(mockTargetBearingDegrees);

        previewView = findViewById(R.id.previewView);
        arrowView = findViewById(R.id.arrowView);
        debugTextView = findViewById(R.id.debugTextView);
        targetTextView = findViewById(R.id.targetTextView);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorHelper = new SensorHelper(sensorManager, this::onHeadingUpdated);

        Log.i(TAG, "Fallback mode active (CameraX + sensors).");
        debugTextView.setText("Mode: FALLBACK\\nInitializing camera and sensors...");
        targetTextView.setText("Target: " + targetRoom + "  |  Computing direction...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startCameraThread();
        checkCameraPermissionAndStart();

        if (sensorHelper.hasRequiredSensors()) {
            sensorHelper.start();
        } else {
            debugTextView.setText("Mode: FALLBACK\\nRequired sensors not available.");
            Log.e(TAG, "Accelerometer or magnetometer missing.");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorHelper.stop();
        closeCamera();
        stopCameraThread();
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCameraPreview();
            return;
        }
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
    }

    private void startCameraPreview() {
        if (previewView.isAvailable()) {
            openCameraAndStartPreview();
            return;
        }

        previewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCameraAndStartPreview();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                // No-op for this basic preview pipeline.
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                // No-op.
            }
        });
    }

    private void openCameraAndStartPreview() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            backCameraId = findBackCameraId();
            if (backCameraId == null) {
                debugTextView.setText("Mode: FALLBACK\\nNo back camera found.");
                return;
            }

            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(backCameraId);
            StreamConfigurationMap configMap = characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
            );
            if (configMap != null) {
                Size[] outputSizes = configMap.getOutputSizes(SurfaceTexture.class);
                if (outputSizes != null && outputSizes.length > 0) {
                    previewSize = outputSizes[0];
                }
            }

            cameraManager.openCamera(backCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraDevice = camera;
                    createPreviewSession();
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    camera.close();
                    cameraDevice = null;
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "Camera open error: " + error);
                    camera.close();
                    cameraDevice = null;
                    debugTextView.setText("Mode: FALLBACK\\nCamera failed to start.");
                }
            }, cameraHandler);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start fallback camera", e);
            debugTextView.setText("Mode: FALLBACK\\nCamera failed to start.");
        }
    }

    private void createPreviewSession() {
        if (cameraDevice == null || !previewView.isAvailable()) {
            return;
        }
        try {
            SurfaceTexture texture = previewView.getSurfaceTexture();
            if (texture == null) {
                return;
            }
            if (previewSize != null) {
                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            }
            Surface previewSurface = new Surface(texture);

            CaptureRequest.Builder requestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            requestBuilder.addTarget(previewSurface);
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            captureSession = session;
                            try {
                                session.setRepeatingRequest(requestBuilder.build(), null,
                                        cameraHandler);
                            } catch (Exception e) {
                                Log.e(TAG, "Failed to start repeating preview request", e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            debugTextView.setText("Mode: FALLBACK\\nCamera configuration failed.");
                        }
                    },
                    cameraHandler
            );
        } catch (Exception e) {
            Log.e(TAG, "createPreviewSession failed", e);
            debugTextView.setText("Mode: FALLBACK\\nCamera failed to start.");
        }
    }

    private String findBackCameraId() throws Exception {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        return null;
    }

    private void closeCamera() {
        if (captureSession != null) {
            captureSession.close();
            captureSession = null;
        }
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
    }

    private void startCameraThread() {
        if (cameraThread != null) {
            return;
        }
        cameraThread = new HandlerThread("FallbackCameraThread");
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
    }

    private void stopCameraThread() {
        if (cameraThread == null) {
            return;
        }
        cameraThread.quitSafely();
        try {
            cameraThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        cameraThread = null;
        cameraHandler = null;
    }

    private void onHeadingUpdated(float headingDegrees) {
        float normalizedHeading = normalizeDegrees(headingDegrees);
        float relativeTurn = shortestSignedAngleDegrees(normalizedHeading, mockTargetBearingDegrees);
        String instruction = buildInstruction(relativeTurn);

        // Rotate arrow to indicate turn angle needed to reach target bearing.
        arrowView.setRotation(relativeTurn);
        targetTextView.setText(String.format(Locale.US, "Target: %s  |  %s", targetRoom, instruction));
        debugTextView.setText(String.format(
                Locale.US,
                "Mode: FALLBACK\\nTarget: %s\\nHeading: %.1f°\\nTarget bearing: %.1f°\\nInstruction: %s",
                targetRoom,
                normalizedHeading,
                mockTargetBearingDegrees,
                instruction
        ));
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
