package com.mahad.arnavigation.ar;

public class WaypointProjection {
    private final float screenX;
    private final float screenY;
    private final boolean visible;
    private final String label;

    public WaypointProjection(float screenX, float screenY, boolean visible, String label) {
        this.screenX = screenX;
        this.screenY = screenY;
        this.visible = visible;
        this.label = label;
    }

    public float getScreenX() {
        return screenX;
    }

    public float getScreenY() {
        return screenY;
    }

    public boolean isVisible() {
        return visible;
    }

    public String getLabel() {
        return label;
    }
}
