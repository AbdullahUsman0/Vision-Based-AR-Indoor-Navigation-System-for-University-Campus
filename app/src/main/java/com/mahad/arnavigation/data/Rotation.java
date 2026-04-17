package com.mahad.arnavigation.data;

public class Rotation {
    private float qx;
    private float qy;
    private float qz;
    private float qw;

    public Rotation() {
        // Required by Gson reflection-based deserialization.
    }

    public Rotation(float qx, float qy, float qz, float qw) {
        this.qx = qx;
        this.qy = qy;
        this.qz = qz;
        this.qw = qw;
    }

    public float getQx() {
        return qx;
    }

    public float getQy() {
        return qy;
    }

    public float getQz() {
        return qz;
    }

    public float getQw() {
        return qw;
    }
}
