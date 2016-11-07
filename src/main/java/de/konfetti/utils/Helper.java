package de.konfetti.utils;

import de.konfetti.data.User;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class Helper {

    private static Properties prop = null;


    // static helper function to hash password
    public static final String hashPassword(String salt, String pass) {
        try {

            // get fresh instance
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

            // hash to new string
            return Arrays.toString(md5Digest.digest((salt + pass).getBytes()));

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static <T> T nonnull(T value) {
        if (value == null) {
            throwIAE();
        }
        return value;
    }

    private static void throwIAE() {
        throw new IllegalArgumentException();
    }

    // calculate the distance between two GPS coordinates in meters
    public static double distInMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 3958.75;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;
        int meterConversion = 1609;
        double meters = Math.floor(dist * meterConversion);
        return meters;
    }

    // append a value to an array
    public static <T> T[] append(T[] arr, T element) {
        final int N = arr.length;
        arr = Arrays.copyOf(arr, N + 1);
        arr[N] = element;
        return arr;
    }

    // check if value is in array
    public static <T> boolean contains(final T[] array, final T v) {
        for (final T e : array)
            if (e == v || v != null && v.equals(e))
                return true;

        return false;
    }

    public static <T> T[] remove(T[] input, T element) {
        List<T> result = new LinkedList<T>();
        for (T item : input)
            if (!element.equals(item))
                result.add(item);
        return result.toArray(input);
    }

    public static boolean userIsAdminOnParty(User user, Long partyId) {
        return user.getAdminParties()
                .stream().filter(party -> party.getId() == partyId)
                .map(party -> true)
                .findAny().orElse(false);
    }
}
