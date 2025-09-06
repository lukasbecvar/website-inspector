package xyz.becvar.websiteinspector.utils;

import xyz.becvar.websiteinspector.Main;

public class Logger {

    // ANSI color codes for formatting
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_CYAN = "\u001B[36m";

    public static String APP_HEADING = ANSI_YELLOW + "[" + ANSI_GREEN + Main.APP_PREFIX + ANSI_YELLOW + "]" + ANSI_CYAN;

    private static String lastProgressMessage = "";

    private static void clearProgressLine() {
        System.out.print("\r\u001B[K"); // Erase current line
    }

    private static void reprintProgressLine() {
        System.out.print(lastProgressMessage);
    }

    public static void printSpacer() {
        clearProgressLine();
        System.out.println(ANSI_CYAN + "========================================================================================");
        reprintProgressLine();
    }

    public static void prompt(String msg) {
        System.out.print(APP_HEADING + ": " + msg + ": " + ANSI_RESET);
    }

    public static void log(String msg) {
        clearProgressLine();
        System.out.println(APP_HEADING + ": " + ANSI_CYAN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    public static void rawLog(String msg) {
        clearProgressLine();
        System.out.println(ANSI_GREEN + msg + ANSI_RESET);
        reprintProgressLine();
    }

    public static void printColoredKeyValue(String key, String value) {
        clearProgressLine();
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            value = "Not specified";
        }
        System.out.println(ANSI_BLUE + key + ANSI_RESET + ": " + ANSI_CYAN + value + ANSI_RESET);
        reprintProgressLine();
    }

    public static void printSuccess(String key, String value) {
        clearProgressLine();
        System.out.println(ANSI_GREEN + "[+] " + key + ANSI_RESET + ": " + value);
        reprintProgressLine();
    }

    public static void printWarning(String key, String value) {
        clearProgressLine();
        System.out.println(ANSI_YELLOW + "[!] " + key + ANSI_RESET + ": " + value);
        reprintProgressLine();
    }

    public static void printError(String message) {
        clearProgressLine();
        System.out.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
        reprintProgressLine();
    }

    public static void printProgress(String msg) {
        lastProgressMessage = "\r" + APP_HEADING + ": " + msg;
        System.out.print(lastProgressMessage);
    }

    public static void clearProgress() {
        clearProgressLine();
        lastProgressMessage = "";
    }
}