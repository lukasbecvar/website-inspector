package xyz.becvar.websiteinspector.utils;

import xyz.becvar.websiteinspector.Main;

public class Logger
{
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    public static String Prefix = ANSI_YELLOW + "[" + ANSI_GREEN + Main.APP_PREFIX + ANSI_YELLOW + "]" + ANSI_YELLOW + ": " + ANSI_CYAN;

    public static void printSpacer()
    {
        System.out.println(ANSI_CYAN + "========================================================================================");
    }

    public static void prompt(String msg)
    {
        System.out.print(Prefix + msg + ": " + ANSI_RESET);
    }

    private static String lastProgressMessage = "";

    public static void log(String msg)
    {
        // Clear the current progress line before printing a new log message
        System.out.print("\r\u001B[K"); // \u001B[K clears from cursor to end of line
        System.out.println(Prefix + msg);
        // Re-print the last progress message to keep it on the last line
        System.out.print(lastProgressMessage);
    }

    public static void rawLog(String msg)
    {
        // Clear the current progress line before printing a new log message
        System.out.print("\r\u001B[K");
        System.out.println(ANSI_GREEN + msg);
        // Re-print the last progress message
        System.out.print(lastProgressMessage);
    }

    public static void printColoredKeyValue(String key, String value)
    {
        // Clear the current progress line before printing a new log message
        System.out.print("\r\u001B[K");
        System.out.println(ANSI_YELLOW + key + ANSI_RESET + ": " + ANSI_GREEN + value + ANSI_RESET);
        // Re-print the last progress message
        System.out.print(lastProgressMessage);
    }

    public static void error(String msg)
    {
        // Clear the current progress line before printing a new log message
        System.out.print("\r\u001B[K");
        System.out.println(Prefix + ANSI_RED + msg);
        // Re-print the last progress message
        System.out.print(lastProgressMessage);
    }

    public static void printProgress(String msg)
    {
        lastProgressMessage = "\r" + Prefix + msg;
        System.out.print(lastProgressMessage);
    }

    public static void clearProgress()
    {
        System.out.print("\r\u001B[K"); // Clear the last progress line
        lastProgressMessage = ""; // Reset the stored message
    }
}
