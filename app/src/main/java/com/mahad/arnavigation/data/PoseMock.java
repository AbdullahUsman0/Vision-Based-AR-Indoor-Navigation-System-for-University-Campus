package com.mahad.arnavigation.data;

public class PoseMock {
    private long timestamp;
    private Position position;
    private Rotation rotation;
    private String targetLabel;

    public long getTimestamp() {
        return timestamp;
    }

    public Position getPosition() {
        return position;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public String getTargetLabel() {
        return targetLabel;
    }
}
