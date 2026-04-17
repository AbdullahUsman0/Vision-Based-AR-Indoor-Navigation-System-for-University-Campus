package com.mahad.arnavigation;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Pose;
import com.mahad.arnavigation.ar.ArController;
import com.mahad.arnavigation.ar.NavigationArFragment;
import com.mahad.arnavigation.ar.WaypointProjection;
import com.mahad.arnavigation.data.PoseMock;
import com.mahad.arnavigation.data.PoseRepository;
import com.mahad.arnavigation.databinding.ActivityMainBinding;
import com.mahad.arnavigation.network.LocalizationHttpClient;
import com.google.android.material.snackbar.Snackbar;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final long LOCALIZATION_REFRESH_MS = 1000L;
    private static final String DEFAULT_LOCALIZATION_ENDPOINT = "http://10.0.2.2:8000/pose";
    private static final String PREFS_NAME = "ar_navigation_prefs";
    private static final String KEY_USE_SERVER = "use_server_localization";
    private static final String KEY_ENDPOINT = "localization_endpoint";

    private ActivityMainBinding binding;
    private ArController arController;
    private PoseRepository poseRepository;
    private LocalizationHttpClient localizationHttpClient;
    private ExecutorService localizationExecutor;
    private volatile String trackingStateText = "Mode: INITIALIZING";
    private volatile boolean useServerLocalization = true;
    private volatile String localizationEndpoint = DEFAULT_LOCALIZATION_ENDPOINT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavigationArFragment arFragment =
                (NavigationArFragment) getSupportFragmentManager().findFragmentById(R.id.arFragmentContainer);
        poseRepository = new PoseRepository(this);
        localizationHttpClient = new LocalizationHttpClient();
        localizationExecutor = Executors.newSingleThreadExecutor();

        arController = new ArController(
                arFragment,
                this::renderDevicePoseDebug,
                this::renderWaypointProjection,
                this::renderTrackingState
        );
        arFragment.getViewLifecycleOwnerLiveData().observe(this, owner -> {
            if (owner != null) {
                arController.start();
            }
        });
        setupControlPanel();

        binding.getRoot().postDelayed(() -> {
            arController.placeArrowInFrontOfCamera(1.5f);
            scheduleLocalizationLoop();
        }, 1000);
    }

    private void renderDevicePoseDebug(Pose pose) {
        float[] t = pose.getTranslation();
        float[] r = pose.getRotationQuaternion();

        String text = "Device Pose (ARCore)\n" + String.format(
                Locale.US,
                "Position: x=%.3f, y=%.3f, z=%.3f\nRotation: qx=%.3f, qy=%.3f, qz=%.3f, qw=%.3f\n%s",
                t[0], t[1], t[2], r[0], r[1], r[2], r[3], trackingStateText
        );
        binding.debugTextView.setText(text);
    }

    private void renderWaypointProjection(WaypointProjection projection) {
        runOnUiThread(() -> {
            if (!projection.isVisible()) {
                binding.roomLabelTextView.setText("");
                return;
            }

            binding.roomLabelTextView.setText(projection.getLabel());
            binding.roomLabelTextView.setX(projection.getScreenX() - (binding.roomLabelTextView.getWidth() / 2f));
            binding.roomLabelTextView.setY(projection.getScreenY() - binding.roomLabelTextView.getHeight() - 12f);
        });
    }

    private void renderTrackingState(String stateText) {
        trackingStateText = stateText;
    }

    private void scheduleLocalizationLoop() {
        binding.getRoot().postDelayed(new Runnable() {
            @Override
            public void run() {
                localizationExecutor.submit(() -> {
                    try {
                        if (useServerLocalization) {
                            PoseMock remotePose = localizationHttpClient.fetchPose(localizationEndpoint);
                            arController.applyLocalizationPose(remotePose);
                            renderTrackingState("Mode: SERVER_LOCALIZATION (HTTP)");
                        } else {
                            PoseMock mockPose = poseRepository.loadMockPose();
                            arController.applyLocalizationPose(mockPose);
                            renderTrackingState("Mode: MOCK_ONLY (switch disabled server)");
                        }
                    } catch (Exception ex) {
                        // Safe fallback ensures AR navigation stays testable offline.
                        PoseMock mockPose = poseRepository.loadMockPose();
                        arController.applyLocalizationPose(mockPose);
                        renderTrackingState("Mode: MOCK_FALLBACK (" + ex.getClass().getSimpleName() + ")");
                    }
                });
                binding.getRoot().postDelayed(this, LOCALIZATION_REFRESH_MS);
            }
        }, 200);
    }

    private void setupControlPanel() {
        loadLocalizationSettings();
        binding.serverEndpointInput.setText(localizationEndpoint);
        binding.useServerSwitch.setChecked(useServerLocalization);

        binding.applySettingsButton.setOnClickListener(v -> {
            useServerLocalization = binding.useServerSwitch.isChecked();
            String endpointInput = binding.serverEndpointInput.getText() == null
                    ? ""
                    : binding.serverEndpointInput.getText().toString().trim();

            if (endpointInput.isEmpty()) {
                localizationEndpoint = DEFAULT_LOCALIZATION_ENDPOINT;
                binding.serverEndpointInput.setText(DEFAULT_LOCALIZATION_ENDPOINT);
            } else {
                localizationEndpoint = endpointInput;
            }

            saveLocalizationSettings();
            String mode = useServerLocalization ? "SERVER mode enabled" : "MOCK mode enabled";
            Snackbar.make(binding.getRoot(), mode, Snackbar.LENGTH_SHORT).show();
        });
    }

    private void loadLocalizationSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        useServerLocalization = prefs.getBoolean(KEY_USE_SERVER, true);
        localizationEndpoint = prefs.getString(KEY_ENDPOINT, DEFAULT_LOCALIZATION_ENDPOINT);
        if (localizationEndpoint == null || localizationEndpoint.trim().isEmpty()) {
            localizationEndpoint = DEFAULT_LOCALIZATION_ENDPOINT;
        }
    }

    private void saveLocalizationSettings() {
        SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_USE_SERVER, useServerLocalization);
        editor.putString(KEY_ENDPOINT, localizationEndpoint);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (localizationExecutor != null) {
            localizationExecutor.shutdownNow();
        }
    }
}
