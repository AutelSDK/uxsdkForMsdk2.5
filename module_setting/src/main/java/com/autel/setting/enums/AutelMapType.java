package com.autel.setting.enums;

/**
 * Created by LJ
 * Date: 2022/12/12
 */
public enum AutelMapType {
    GAODE(0),
    GOOGLE(1),
    MAPBOX(2);

    private final int value;

    AutelMapType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static AutelMapType find(int value) {
        switch (value) {
            case 0:
                return GAODE;
            case 1:
                return GOOGLE;
            case 2:
                return MAPBOX;
            default:
                return GOOGLE;
        }
    }
}
