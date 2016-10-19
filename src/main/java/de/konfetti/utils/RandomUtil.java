package de.konfetti.utils;


import org.apache.commons.lang3.RandomStringUtils;

import java.util.Random;

/**
 * Utility class for generating random Strings.
 */
public final class RandomUtil {

    private static final int DEF_COUNT = 20;

    private static Random randomGenerator = new Random();

    private RandomUtil() {
    }

    public static Long generadeCodeNumber() {
        long rand = randomGenerator.nextLong();
        if (rand<0) rand = -rand;
        return ( rand % 8999999999l) + 1000000000l;
    }


    /**
     * Generates a password.
     *
     * @return the generated password
     */
    public static String generatePassword() {
        return RandomStringUtils.randomAlphanumeric(DEF_COUNT);
    }

    /**
     * Generates an activation key.
     *
     * @return the generated activation key
     */
    public static String generateActivationKey() {
        return RandomStringUtils.randomNumeric(DEF_COUNT);
    }

    /**
    * Generates a reset key.
    *
    * @return the generated reset key
    */
    public static String generateResetKey() {
        return RandomStringUtils.randomNumeric(DEF_COUNT);
    }
}
