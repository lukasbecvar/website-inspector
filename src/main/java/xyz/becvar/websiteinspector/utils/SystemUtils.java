package xyz.becvar.websiteinspector.utils;

/**
 * Class SystemUtils
 *
 * This class contains utility methods for the system
 *
 * @package xyz.becvar.websiteinspector.utils
 */
public class SystemUtils {

    /**
     * Shuts down the application with the given message
     * 
     * @param msg The message to print
     */
    public static void shutdown(String msg) {
        Logger.printError(msg);
        throw new RuntimeException(msg);
    }
}
