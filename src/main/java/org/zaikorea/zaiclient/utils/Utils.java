package org.zaikorea.zaiclient.utils;

public class Utils {
    public static String getZaiClientVersion() {
        return "0.0.1";
    }

    public static double getCurrentUnixTimestamp() {
        // Have to track nanosecond because client sometimes calls api multiple times in a millisecond
        // Use nanoTime because jdk 1.8 doesn't support Instant.getNano() function.
        String time = Long.toString(System.nanoTime());
        time = time.substring(time.length()-7);

        long longTime = Long.parseLong(time);
        long currentTime = System.currentTimeMillis();

        return currentTime / 1000.d + longTime / 1e10;
    }
}
