package com.karson.dadcalc;

public final class Convert {
    public static final String INCHES = "in";
    public static final String FEET = "ft";
    public static final String MILLIMETERS = "mm";
    private static final double INCHES_IN_FEET = 12;
    private static final double MILLIMETERS_IN_INCHES = 25.4;

    private Convert() {}

    public static boolean isUnit(String token) {
        return INCHES.equals(token) || FEET.equals(token) || MILLIMETERS.equals(token);
    }

    public static double x_y(double value, String x, String y) {
        if (x.equals(y)) return value;
        return fromInches(toInches(value, x), y);
    }

    private static double toInches(double value, String unit) {
        switch (unit) {
            case INCHES:
                return value;
            case FEET:
                return value * INCHES_IN_FEET;
            case MILLIMETERS:
                return value / MILLIMETERS_IN_INCHES;
            default:
                throw new IllegalArgumentException("Invalid convert type: " + unit);
        }
    }

    private static double fromInches(double inches, String unit) {
        switch (unit) {
            case INCHES:
                return inches;
            case FEET:
                return inches / INCHES_IN_FEET;
            case MILLIMETERS:
                return inches * MILLIMETERS_IN_INCHES;
            default:
                throw new IllegalArgumentException("Invalid convert type: " + unit);
        }
    }
}
