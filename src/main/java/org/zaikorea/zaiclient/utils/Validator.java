package org.zaikorea.zaiclient.utils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;


public class Validator {
    public static String validateString(String value, int min, int max, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        if (!nullable && value == null) {
            throw new IllegalArgumentException(
                    String.format("The value of %s must not be null", varName)
            );
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

        if (!nullable && value == null) {
            throw new IllegalArgumentException(
                    String.format("The value of %s must not be null", varName)
            );
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

        if (!nullable && value == null) {
            throw new IllegalArgumentException(
                    String.format("The value of %s must not be null", varName)
            );
        }

        int length = value.size();

        if ((length < listMin) || (length > listMax)) {
            throw new IllegalArgumentException(
                    String.format("The length of list(%s) must be between %d and %d", varName, listMin, listMax)
            );
        }

        for (String s : value) {
            try {
                Validator.validateString(s, 1, 500, false, null);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        String.format("The length of each element in %s must be between %d and %d", varName, 1, 500)
                );
            }
        }

        return value;
    }

    public static String validateISODateTime(String dateTimeStr, String varName) {
        try {
            OffsetDateTime.parse(dateTimeStr);
        } catch (DateTimeParseException e){
            throw new IllegalArgumentException(
                    String.format("The value of %s must be ISO 8601 format not %s", varName, dateTimeStr)
            );
        }

        return dateTimeStr;
    }

    public static <T extends Comparable<T>> T validateNumber(T value, T min, T max, boolean nullable, String varName) {
        if (nullable && value == null) {
            return null;
        }

        if (!nullable && value == null) {
            throw new IllegalArgumentException(
                    String.format("The value of %s must not be null", varName)
            );
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
