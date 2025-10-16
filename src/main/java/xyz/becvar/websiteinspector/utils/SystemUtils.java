package xyz.becvar.websiteinspector.utils;

/**
 * This class contains utility methods for the system
 */
public class SystemUtils
{
    /**
     * Shuts down the application with the given message
     * 
     * @param msg The message to print
     */
    public static void shutdown(String msg)
    {
        Logger.printError(msg);
        throw new RuntimeException(msg);
    }
}
