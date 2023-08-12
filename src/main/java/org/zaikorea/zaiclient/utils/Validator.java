package org.zaikorea.zaiclient.utils;

import java.util.List;
import java.util.function.Function;

public class Validator {
    public static String validateString(String value, int min, int max, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        int length = value.length();

        if ((length < min) || (length > max)) {
            throw new IllegalArgumentException(
                    String.format("The length of %s must be between %d and %d", varName, min, max)
            );
        }

        return value;
    }

    public static String validateString(String value, int min, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        int length = value.length();

        if (length < min) {
            throw new IllegalArgumentException(
                    String.format("The length of %s must be greater than %d", varName, min)
            );
        }

        return value;
    }

    public static List<String> validateStringList(List<String> value, int listMin, int listMax, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        int length = value.size();

        if ((length < listMin) || (length > listMax)) {
            throw new IllegalArgumentException(
                    String.format("The length of list(%s) must be between %d and %d", varName, listMin, listMax)
            );
        }

        for (String s : value) {
            try {
                Validator.validateString(s, 0, 500, false, null);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("The length of each element in %s must be between %d and %d", varName, 0, 500)
                );
            }
        }

        return value;
    }

    public static <T extends Comparable<T>> T validateNumber(T value, T min, T max, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        if ((value.compareTo(min) < 0) || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    String.format("The value of %s must be between %s and %s", varName, min, max)
            );
        }

        return value;
    }

    public static <T extends Comparable<T>> T validateNumber(T value, T min, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        if ((value.compareTo(min) < 0)) {
            throw new IllegalArgumentException(
                    String.format("The value of %s must be greater or equal to %s", varName, min)
            );
        }

        return value;
    }
}
