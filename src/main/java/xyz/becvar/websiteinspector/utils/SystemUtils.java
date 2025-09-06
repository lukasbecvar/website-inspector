package xyz.becvar.websiteinspector.utils;

public class SystemUtils
{
    public static void shutdown(String msg)
    {
        Logger.printError(msg);
        System.exit(0);
    }
}
