package xyz.becvar.websiteinspector.utils;

import java.security.SecureRandom;

/**
 * Class StringUtils
 *
 * This class contains string utils methods
 *
 * @package xyz.becvar.websiteinspector.utils
 */
public class StringUtils {

    /**
     * Random string generator
     *
     * @param length The length of string to be generated
     *
     * @return The generated string
     */
    public static String generateRandomString(int length) {
        final String ALPHANUMERIC = "abcdefghijklmnopqrstuvwxyz0123456789";
        final SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(random.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }
}
