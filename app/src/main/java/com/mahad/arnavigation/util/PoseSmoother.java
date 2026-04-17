package com.mahad.arnavigation.util;

import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;

public class PoseSmoother {
    private final float alpha;
    private Vector3 smoothedPosition = Vector3.zero();
    private Quaternion smoothedRotation = Quaternion.identity();
    private boolean initialized = false;

    public PoseSmoother() {
        this(0.2f);
    }

    public PoseSmoother(float alpha) {
        this.alpha = alpha;
    }

    public Vector3 smoothPosition(Vector3 target) {
        if (!initialized) {
            smoothedPosition = target;
            initialized = true;
            return target;
        }

        smoothedPosition = new Vector3(
            lerp(smoothedPosition.x, target.x),
            lerp(smoothedPosition.y, target.y),
            lerp(smoothedPosition.z, target.z)
        );
        return smoothedPosition;
    }

    public Quaternion smoothRotation(Quaternion target) {
        if (!initialized) {
            smoothedRotation = target;
        } else {
            smoothedRotation = Quaternion.slerp(smoothedRotation, target, alpha);
        }
        return smoothedRotation;
    }

    private float lerp(float start, float end) {
        return start + alpha * (end - start);
    }
}
