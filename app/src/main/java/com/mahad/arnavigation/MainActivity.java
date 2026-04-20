package com.mahad.arnavigation;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.ar.core.ArCoreApk;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final long SPLASH_DELAY_MS = 1000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable delayedModeDecision = this::launchModeAutomatically;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ArCoreApk.Availability firstCheck = ArCoreApk.getInstance().checkAvailability(this);
        Log.i(TAG, "Splash first availability check: " + firstCheck.name());

        // Wait briefly for ARCore availability to resolve, then decide mode.
        handler.postDelayed(delayedModeDecision, SPLASH_DELAY_MS);
    }

    private void launchModeAutomatically() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        boolean arSupported = availability.isSupported();

        // Transient/unknown states are treated as unsupported for seamless fallback.
        if (availability == ArCoreApk.Availability.UNKNOWN_CHECKING) {
            arSupported = false;
        }

        Class<?> targetActivity = arSupported ? ARActivity.class : FallbackActivity.class;
        Log.i(TAG, "Auto mode selected: " + (arSupported ? "AR_MODE" : "FALLBACK_MODE")
                + " availability=" + availability.name());

        startActivity(new Intent(this, targetActivity));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(delayedModeDecision);
    }
}
