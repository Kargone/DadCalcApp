package com.example.dadcalc;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Convert {
    static final private double INCHES_IN_FEET = 12;
    static final private double MILLIMETERS_IN_INCHES = 25.4;
    public double ft_in(double feet) {
        return feet * INCHES_IN_FEET;
    }
    public double in_mm(double inches) {
        return inches * MILLIMETERS_IN_INCHES;
    }
    public double mm_ft(double millimeters) {
        return millimeters / INCHES_IN_FEET / MILLIMETERS_IN_INCHES;
    }
    public double in_ft(double inches) {
        return inches / INCHES_IN_FEET;
    }
    public double mm_in(double millimeters) {
        return millimeters / MILLIMETERS_IN_INCHES;
    }
    public double ft_mm(double feet) {
        return feet * INCHES_IN_FEET * MILLIMETERS_IN_INCHES;
    }
    public Double x_y(double value, String x, String y) throws Exception {
        final String[] convertTypes = {"mm", "ft", "in"};
        List<String> convertTypesList = Arrays.asList(convertTypes);

        if (Objects.equals(x, y)) return value;

        if (convertTypesList.contains(x) && convertTypesList.contains(y)) {
            String convertMethodName = x + "_" + y;
            Method convertMethod = this.getClass().getMethod(convertMethodName, double.class);
            String convertedValue = Objects.requireNonNull(convertMethod.invoke(this, value)).toString();
            return Double.parseDouble(convertedValue);
        }

        throw new Exception("Invalid convert types: " + x + " " + y);
    }
}
