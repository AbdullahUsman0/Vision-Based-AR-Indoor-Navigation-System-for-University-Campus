package com.mahad.arnavigation.data;

public class PoseMock {
    private long timestamp;
    private Position position;
    private Rotation rotation;

    public PoseMock() {
        // Required by Gson reflection-based deserialization.
    }

    public PoseMock(long timestamp, Position position, Rotation rotation) {
        this.timestamp = timestamp;
        this.position = position;
        this.rotation = rotation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Position getPosition() {
        return position;
    }

    public Rotation getRotation() {
        return rotation;
    }
}
