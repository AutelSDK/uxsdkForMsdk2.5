package com.autel.widget.widget.temp;

/**
 * Created by  2023/10/2
 */
public class TemperaturePoint {
    private final float pointX;
    private final float pointY;

    TemperaturePoint(float pointX, float pointY) {
        this.pointX = pointX;
        this.pointY = pointY;
    }

    public float getPointX() {
        return pointX;
    }

    public float getPointY() {
        return pointY;
    }
}
