package com.mahad.arnavigation.data;

public class Position {
    private float x;
    private float y;
    private float z;

    public Position() {
        // Required by Gson reflection-based deserialization.
    }

    public Position(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}
