package com.mahad.arnavigation;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.Pose;
import com.mahad.arnavigation.ar.ArController;
import com.mahad.arnavigation.ar.NavigationArFragment;
import com.mahad.arnavigation.data.PoseMock;
import com.mahad.arnavigation.data.PoseRepository;
import com.mahad.arnavigation.databinding.ActivityMainBinding;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ArController arController;
    private PoseRepository poseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavigationArFragment arFragment = (NavigationArFragment) getSupportFragmentManager()
            .findFragmentById(R.id.arFragmentContainer);
        if (arFragment == null) {
            throw new IllegalStateException("NavigationArFragment not found in layout");
        }

        poseRepository = new PoseRepository(this);
        arController = new ArController(arFragment, this::renderDevicePoseDebug);
        arController.start();

        binding.getRoot().postDelayed(() -> {
            arController.placeArrowInFrontOfCamera(1.5f);
            PoseMock mockPose = poseRepository.loadMockPose();
            arController.applyJsonRotation(mockPose);
        }, 1000);
    }

    private void renderDevicePoseDebug(Pose pose) {
        float[] t = pose.getTranslation();
        float[] r = pose.getRotationQuaternion();

        String debug = "Device Pose (ARCore)\n"
            + "Position: x=" + format3(t[0]) + ", y=" + format3(t[1]) + ", z=" + format3(t[2]) + "\n"
            + "Rotation: qx=" + format3(r[0]) + ", qy=" + format3(r[1]) + ", qz=" + format3(r[2])
            + ", qw=" + format3(r[3]);

        binding.debugTextView.setText(debug);
    }

    private String format3(float value) {
        return String.format(Locale.US, "%.3f", value);
    }
}
